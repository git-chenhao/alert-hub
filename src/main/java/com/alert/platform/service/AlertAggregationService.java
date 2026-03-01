package com.alert.platform.service;

import com.alert.platform.config.AggregationConfig;
import com.alert.platform.entity.Alert;
import com.alert.platform.entity.AlertBatch;
import com.alert.platform.repository.AlertBatchRepository;
import com.alert.platform.repository.AlertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警聚合服务
 * 可配置聚合策略：group_wait(等待时间)、max_count(最大数量)、max_wait(最大等待)
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertAggregationService {

    private final AlertRepository alertRepository;
    private final AlertBatchRepository batchRepository;
    private final AggregationConfig aggregationConfig;
    private final ObjectMapper objectMapper;

    /**
     * 聚合缓存: batchKey -> List<Alert>
     */
    private final ConcurrentHashMap<String, List<Alert>> aggregationCache = new ConcurrentHashMap<>();

    /**
     * 生成批次Key (基于分组规则)
     */
    public String generateBatchKey(Map<String, String> labels) {
        try {
            String[] groupByLabels = aggregationConfig.getGroupByLabels().split(",");

            StringBuilder sb = new StringBuilder();
            for (String labelKey : groupByLabels) {
                String key = labelKey.trim();
                String value = labels.getOrDefault(key, "");
                sb.append(key).append("=").append(value).append("|");
            }

            // 计算MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("生成批次Key失败", e);
            return UUID.randomUUID().toString();
        }
    }

    /**
     * 将告警添加到聚合批次
     */
    @Async("alertTaskExecutor")
    @Transactional
    public void addToAggregation(Alert alert) {
        try {
            // 解析标签
            Map<String, String> labels = parseLabels(alert.getLabels());
            String batchKey = generateBatchKey(labels);

            // 查找或创建批次
            AlertBatch batch = batchRepository.findByBatchKey(batchKey)
                    .orElseGet(() -> createBatch(batchKey, labels));

            // 更新告警的批次ID
            alert.setBatchId(batch.getId());
            alert.setStatus("AGGREGATING");
            alertRepository.save(alert);

            // 使用原子操作增加批次告警计数，解决并发问题
            batchRepository.incrementAlertCount(batch.getId());

            // 重新获取批次以检查条件
            batch = batchRepository.findById(batch.getId()).orElse(batch);

            // 检查是否达到聚合条件
            if (shouldDispatch(batch)) {
                dispatchBatch(batch);
            }

            log.debug("告警已添加到聚合批次: batchKey={}, alertId={}", batchKey, alert.getId());
        } catch (Exception e) {
            log.error("添加到聚合批次失败", e);
        }
    }

    /**
     * 创建新的聚合批次
     */
    private AlertBatch createBatch(String batchKey, Map<String, String> labels) {
        try {
            String name = generateBatchName(labels);
            String labelsJson = objectMapper.writeValueAsString(labels);

            AlertBatch batch = AlertBatch.builder()
                    .batchKey(batchKey)
                    .name(name)
                    .alertCount(0)
                    .status("PENDING")
                    .groupLabels(labelsJson)
                    .createdAt(LocalDateTime.now())
                    .build();

            return batchRepository.save(batch);
        } catch (Exception e) {
            log.error("创建批次失败", e);
            throw new RuntimeException("创建批次失败", e);
        }
    }

    /**
     * 生成批次名称
     */
    private String generateBatchName(Map<String, String> labels) {
        return String.format("[%s] %s",
                labels.getOrDefault("severity", "unknown"),
                labels.getOrDefault("alertname", "unnamed"));
    }

    /**
     * 检查是否应该派发批次
     */
    private boolean shouldDispatch(AlertBatch batch) {
        // 达到最大数量
        if (batch.getAlertCount() >= aggregationConfig.getMaxCount()) {
            log.info("批次达到最大数量，触发派发: batchKey={}, count={}",
                    batch.getBatchKey(), batch.getAlertCount());
            return true;
        }

        // 超过最大等待时间
        LocalDateTime maxWait = batch.getCreatedAt()
                .plusSeconds(aggregationConfig.getMaxWaitSeconds());
        if (LocalDateTime.now().isAfter(maxWait)) {
            log.info("批次达到最大等待时间，触发派发: batchKey={}", batch.getBatchKey());
            return true;
        }

        return false;
    }

    /**
     * 派发批次到Agent处理
     */
    @Transactional
    public void dispatchBatch(AlertBatch batch) {
        try {
            batch.setStatus("DISPATCHED");
            batch.setDispatchedAt(LocalDateTime.now());
            batchRepository.save(batch);

            // 使用批量更新代替循环更新，解决N+1问题
            alertRepository.updateStatusByBatchId(batch.getId(), "DISPATCHED");

            log.info("批次已派发: batchKey={}, alertCount={}",
                    batch.getBatchKey(), batch.getAlertCount());

            // 异步调用Agent处理
            // 这里需要注入AgentSchedulerService
        } catch (Exception e) {
            log.error("派发批次失败", e);
            batch.setStatus("FAILED");
            batch.setErrorMessage(e.getMessage());
            batchRepository.save(batch);
        }
    }

    /**
     * 定时检查待处理的批次
     * 每秒执行一次
     */
    @Scheduled(fixedDelay = 1000)
    public void checkPendingBatches() {
        try {
            LocalDateTime threshold = LocalDateTime.now()
                    .minusSeconds(aggregationConfig.getGroupWaitSeconds());

            List<AlertBatch> readyBatches = batchRepository
                    .findBatchesReadyToAggregate(threshold);

            for (AlertBatch batch : readyBatches) {
                if (batch.getAlertCount() > 0) {
                    dispatchBatch(batch);
                }
            }
        } catch (Exception e) {
            log.error("检查待处理批次失败", e);
        }
    }

    /**
     * 解析标签JSON
     */
    private Map<String, String> parseLabels(String labelsJson) {
        try {
            if (labelsJson == null || labelsJson.isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(labelsJson, Map.class);
        } catch (Exception e) {
            log.error("解析标签失败", e);
            return new HashMap<>();
        }
    }
}
