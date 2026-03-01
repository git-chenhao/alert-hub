package com.alert.platform.service;

import com.alert.platform.config.AgentConfig;
import com.alert.platform.entity.Alert;
import com.alert.platform.entity.AlertBatch;
import com.alert.platform.repository.AlertBatchRepository;
import com.alert.platform.repository.AlertRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent调度服务
 * 实现A2A协议调用Sub-Agent
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSchedulerService {

    private final AlertBatchRepository batchRepository;
    private final AlertRepository alertRepository;
    private final AgentConfig agentConfig;
    private final FeishuNotificationService feishuNotificationService;
    private final ObjectMapper objectMapper;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * 调度批次到Sub-Agent进行分析
     */
    @Async("agentTaskExecutor")
    @Transactional
    public void scheduleToAgent(Long batchId) {
        AlertBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));

        try {
            log.info("开始调度批次到Agent: batchId={}, batchKey={}",
                    batchId, batch.getBatchKey());

            // 准备请求数据
            Map<String, Object> request = buildAgentRequest(batch);

            // 调用Sub-Agent
            long startTime = System.currentTimeMillis();
            JsonNode response = callSubAgent(request);
            long duration = System.currentTimeMillis() - startTime;

            // 处理响应
            handleAgentResponse(batch, response, duration);

        } catch (Exception e) {
            log.error("调度到Agent失败: batchId={}", batchId, e);
            handleAgentError(batch, e);
        }
    }

    /**
     * 构建Agent请求数据
     */
    private Map<String, Object> buildAgentRequest(AlertBatch batch) {
        List<Alert> alerts = alertRepository.findByBatchIdOrderByCreatedAtAsc(batch.getId());

        Map<String, Object> request = new HashMap<>();
        request.put("batch_id", batch.getId());
        request.put("batch_key", batch.getBatchKey());
        request.put("batch_name", batch.getName());
        request.put("alert_count", alerts.size());

        // 构建告警列表
        List<Map<String, Object>> alertList = alerts.stream()
                .map(alert -> {
                    Map<String, Object> alertMap = new HashMap<>();
                    alertMap.put("alertname", alert.getAlertname());
                    alertMap.put("severity", alert.getSeverity());
                    alertMap.put("instance", alert.getInstance());
                    alertMap.put("message", alert.getMessage());
                    alertMap.put("labels", parseLabels(alert.getLabels()));
                    alertMap.put("created_at", alert.getCreatedAt().toString());
                    return alertMap;
                })
                .toList();

        request.put("alerts", alertList);
        request.put("group_labels", parseLabels(batch.getGroupLabels()));

        return request;
    }

    /**
     * 调用Sub-Agent
     */
    private JsonNode callSubAgent(Map<String, Object> request) {
        return webClient.post()
                .uri(agentConfig.getEndpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(agentConfig.getTimeoutSeconds()))
                .retryWhen(Retry.backoff(
                        agentConfig.getMaxRetries(),
                        Duration.ofSeconds(2)
                ).doBeforeRetry(signal -> log.warn("重试Agent调用: attempt={}",
                        signal.totalRetries() + 1)))
                .block();
    }

    /**
     * 处理Agent响应
     */
    @Transactional
    public void handleAgentResponse(AlertBatch batch, JsonNode response, long duration) {
        try {
            // 提取分析结果
            String resultJson = objectMapper.writeValueAsString(response);

            batch.setAgentResult(resultJson);
            batch.setAgentDurationMs(duration);
            batch.setStatus("COMPLETED");
            batch.setCompletedAt(LocalDateTime.now());
            batchRepository.save(batch);

            // 更新告警状态
            List<Alert> alerts = alertRepository.findByBatchIdOrderByCreatedAtAsc(batch.getId());
            alerts.forEach(alert -> {
                alert.setStatus("COMPLETED");
                alertRepository.save(alert);
            });

            log.info("Agent分析完成: batchId={}, duration={}ms", batch.getId(), duration);

            // 发送飞书通知
            if (response != null) {
                feishuNotificationService.sendNotification(batch, response);
            }

        } catch (Exception e) {
            log.error("处理Agent响应失败: batchId={}", batch.getId(), e);
            batch.setStatus("FAILED");
            batch.setErrorMessage("处理响应失败: " + e.getMessage());
            batchRepository.save(batch);
        }
    }

    /**
     * 处理Agent错误
     */
    @Transactional
    public void handleAgentError(AlertBatch batch, Exception e) {
        batch.setStatus("FAILED");
        batch.setErrorMessage(e.getMessage());
        batch.setCompletedAt(LocalDateTime.now());
        batchRepository.save(batch);

        // 更新告警状态
        List<Alert> alerts = alertRepository.findByBatchIdOrderByCreatedAtAsc(batch.getId());
        alerts.forEach(alert -> {
            alert.setStatus("FAILED");
            alertRepository.save(alert);
        });
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
