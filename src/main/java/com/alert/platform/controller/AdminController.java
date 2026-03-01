package com.alert.platform.controller;

import com.alert.platform.dto.AlertDTO;
import com.alert.platform.dto.ApiResponse;
import com.alert.platform.dto.BatchDTO;
import com.alert.platform.entity.Alert;
import com.alert.platform.entity.AlertBatch;
import com.alert.platform.entity.AggregationRule;
import com.alert.platform.repository.AlertBatchRepository;
import com.alert.platform.repository.AlertRepository;
import com.alert.platform.repository.AggregationRuleRepository;
import com.alert.platform.service.AgentSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台Controller
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AlertRepository alertRepository;
    private final AlertBatchRepository batchRepository;
    private final AggregationRuleRepository ruleRepository;
    private final AgentSchedulerService agentSchedulerService;

    /**
     * 获取告警列表
     */
    @GetMapping("/alerts")
    public ApiResponse<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        try {
            org.springframework.data.domain.Pageable pageable = PageRequest.of(
                    page, size,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

            Page<Alert> alertPage;
            if (status != null && !status.isEmpty()) {
                // 使用数据库层面分页 (修复 N+1 查询 + 内存分页问题)
                alertPage = alertRepository.findByStatus(status, pageable);
            } else {
                alertPage = alertRepository.findAll(pageable);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("content", alertPage.getContent().stream()
                    .map(this::convertToDTO)
                    .toList());
            result.put("totalElements", alertPage.getTotalElements());
            result.put("totalPages", alertPage.getTotalPages());
            result.put("currentPage", page);

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("获取告警列表失败", e);
            return ApiResponse.error("获取告警列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取批次列表
     */
    @GetMapping("/batches")
    public ApiResponse<List<BatchDTO>> getBatches(
            @RequestParam(required = false) String status) {

        try {
            List<AlertBatch> batches;
            if (status != null && !status.isEmpty()) {
                batches = batchRepository.findByStatusOrderByCreatedAtDesc(status);
            } else {
                batches = batchRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")
                );
            }

            List<BatchDTO> batchDTOs = batches.stream()
                    .map(this::convertToBatchDTO)
                    .toList();

            return ApiResponse.success(batchDTOs);

        } catch (Exception e) {
            log.error("获取批次列表失败", e);
            return ApiResponse.error("获取批次列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取批次详情
     */
    @GetMapping("/batches/{id}")
    public ApiResponse<Map<String, Object>> getBatchDetail(@PathVariable Long id) {
        try {
            AlertBatch batch = batchRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + id));

            List<Alert> alerts = alertRepository.findByBatchIdOrderByCreatedAtAsc(id);

            Map<String, Object> result = new HashMap<>();
            result.put("batch", convertToBatchDTO(batch));
            result.put("alerts", alerts.stream()
                    .map(this::convertToDTO)
                    .toList());

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("获取批次详情失败", e);
            return ApiResponse.error("获取批次详情失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发批次分析
     */
    @PostMapping("/batches/{id}/dispatch")
    public ApiResponse<String> dispatchBatch(@PathVariable Long id) {
        try {
            agentSchedulerService.scheduleToAgent(id);
            return ApiResponse.success("已触发分析");
        } catch (Exception e) {
            log.error("触发分析失败", e);
            return ApiResponse.error("触发分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取聚合规则列表
     */
    @GetMapping("/rules")
    public ApiResponse<List<AggregationRule>> getRules() {
        try {
            List<AggregationRule> rules = ruleRepository.findByEnabledTrueOrderByCreatedAtDesc();
            return ApiResponse.success(rules);
        } catch (Exception e) {
            log.error("获取规则列表失败", e);
            return ApiResponse.error("获取规则列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建聚合规则
     */
    @PostMapping("/rules")
    public ApiResponse<AggregationRule> createRule(@RequestBody AggregationRule rule) {
        try {
            rule.setCreatedAt(LocalDateTime.now());
            rule.setUpdatedAt(LocalDateTime.now());
            AggregationRule saved = ruleRepository.save(rule);
            return ApiResponse.success("规则创建成功", saved);
        } catch (Exception e) {
            log.error("创建规则失败", e);
            return ApiResponse.error("创建规则失败: " + e.getMessage());
        }
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        try {
            long totalAlerts = alertRepository.count();
            long totalBatches = batchRepository.count();

            // 使用 COUNT 查询替代全表扫描 (修复全表扫描问题)
            long pendingAlerts = alertRepository.countByStatus("PENDING");

            // 添加完成的批次统计查询到 Repository
            long completedBatches = batchRepository.countByStatus("COMPLETED");

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAlerts", totalAlerts);
            stats.put("totalBatches", totalBatches);
            stats.put("pendingAlerts", pendingAlerts);
            stats.put("completedBatches", completedBatches);

            return ApiResponse.success(stats);

        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ApiResponse.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 转换为DTO
     */
    private AlertDTO convertToDTO(Alert alert) {
        return AlertDTO.builder()
                .id(alert.getId())
                .fingerprint(alert.getFingerprint())
                .source(alert.getSource())
                .severity(alert.getSeverity())
                .alertname(alert.getAlertname())
                .instance(alert.getInstance())
                .message(alert.getMessage())
                .status(alert.getStatus())
                .batchId(alert.getBatchId())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }

    /**
     * 转换为BatchDTO
     */
    private BatchDTO convertToBatchDTO(AlertBatch batch) {
        return BatchDTO.builder()
                .id(batch.getId())
                .batchKey(batch.getBatchKey())
                .name(batch.getName())
                .alertCount(batch.getAlertCount())
                .status(batch.getStatus())
                .agentResult(batch.getAgentResult())
                .agentDurationMs(batch.getAgentDurationMs())
                .errorMessage(batch.getErrorMessage())
                .createdAt(batch.getCreatedAt())
                .aggregatedAt(batch.getAggregatedAt())
                .dispatchedAt(batch.getDispatchedAt())
                .completedAt(batch.getCompletedAt())
                .build();
    }
}
