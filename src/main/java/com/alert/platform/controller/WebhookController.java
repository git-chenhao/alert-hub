package com.alert.platform.controller;

import com.alert.platform.dto.ApiResponse;
import com.alert.platform.dto.WebhookAlertRequest;
import com.alert.platform.entity.Alert;
import com.alert.platform.repository.AlertRepository;
import com.alert.platform.service.AlertAggregationService;
import com.alert.platform.service.AlertDeduplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook告警接收Controller
 * 支持多种告警源格式
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final AlertDeduplicationService deduplicationService;
    private final AlertAggregationService aggregationService;
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    /**
     * Prometheus Alertmanager Webhook
     */
    @PostMapping("/alerts")
    public ApiResponse<?> receivePrometheusAlert(@RequestBody Map<String, Object> payload) {
        try {
            log.info("接收到Prometheus告警: {}", payload);

            // Alertmanager格式解析
            String status = (String) payload.get("status");
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> alerts = (java.util.List<Map<String, Object>>) payload.get("alerts");

            int received = 0;
            int deduped = 0;

            for (Map<String, Object> alertData : alerts) {
                @SuppressWarnings("unchecked")
                Map<String, String> labels = (Map<String, String>) alertData.get("labels");
                @SuppressWarnings("unchecked")
                Map<String, String> annotations = (Map<String, String>) alertData.get("annotations");

                WebhookAlertRequest request = WebhookAlertRequest.builder()
                        .alertname(labels.get("alertname"))
                        .severity(labels.get("severity"))
                        .instance(labels.get("instance"))
                        .message(annotations.get("summary"))
                        .labels(labels)
                        .annotations(annotations)
                        .status(status)
                        .source("prometheus")
                        .build();

                int result = processAlert(request);
                if (result == 1) received++;
                else deduped++;
            }

            return ApiResponse.success(String.format("接收 %d 条告警，去重 %d 条", received, deduped), null);

        } catch (Exception e) {
            log.error("处理Prometheus告警失败", e);
            return ApiResponse.error("处理告警失败: " + e.getMessage());
        }
    }

    /**
     * Grafana Webhook
     */
    @PostMapping("/grafana")
    public ApiResponse<?> receiveGrafanaAlert(@RequestBody Map<String, Object> payload) {
        try {
            log.info("接收到Grafana告警: {}", payload);

            String title = (String) payload.get("title");
            String state = (String) payload.get("state");
            String message = (String) payload.get("message");

            @SuppressWarnings("unchecked")
            Map<String, String> labels = (Map<String, String>) payload.get("labels");

            Map<String, String> allLabels = new HashMap<>();
            if (labels != null) {
                allLabels.putAll(labels);
            }
            allLabels.put("state", state);

            WebhookAlertRequest request = WebhookAlertRequest.builder()
                    .alertname(title)
                    .severity(mapGrafanaStateToSeverity(state))
                    .message(message)
                    .labels(allLabels)
                    .status(state)
                    .source("grafana")
                    .build();

            int result = processAlert(request);

            if (result == 1) {
                return ApiResponse.success("告警接收成功");
            } else {
                return ApiResponse.success("告警已去重");
            }

        } catch (Exception e) {
            log.error("处理Grafana告警失败", e);
            return ApiResponse.error("处理告警失败: " + e.getMessage());
        }
    }

    /**
     * Zabbix Webhook
     */
    @PostMapping("/zabbix")
    public ApiResponse<?> receiveZabbixAlert(@RequestBody Map<String, Object> payload) {
        try {
            log.info("接收到Zabbix告警: {}", payload);

            String host = (String) payload.get("host");
            String triggerName = (String) payload.get("trigger_name");
            String severity = (String) payload.get("severity");
            String message = (String) payload.get("message");

            Map<String, String> labels = new HashMap<>();
            labels.put("host", host);
            labels.put("trigger_name", triggerName);
            labels.put("severity", severity);

            WebhookAlertRequest request = WebhookAlertRequest.builder()
                    .alertname(triggerName)
                    .severity(mapZabbixSeverity(severity))
                    .instance(host)
                    .message(message)
                    .labels(labels)
                    .source("zabbix")
                    .build();

            int result = processAlert(request);

            if (result == 1) {
                return ApiResponse.success("告警接收成功");
            } else {
                return ApiResponse.success("告警已去重");
            }

        } catch (Exception e) {
            log.error("处理Zabbix告警失败", e);
            return ApiResponse.error("处理告警失败: " + e.getMessage());
        }
    }

    /**
     * 通用告警创建接口 (添加输入验证)
     */
    @PostMapping("/create")
    public ApiResponse<?> createAlert(@Valid @RequestBody WebhookAlertRequest request) {
        try {
            log.info("接收到通用告警: alertname={}, severity={}",
                    request.getAlertname(), request.getSeverity());

            if (request.getSource() == null || request.getSource().isEmpty()) {
                request.setSource("generic");
            }

            int result = processAlert(request);

            if (result == 1) {
                return ApiResponse.success("告警创建成功");
            } else {
                return ApiResponse.success("告警已去重");
            }

        } catch (Exception e) {
            log.error("创建告警失败", e);
            return ApiResponse.error("创建告警失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        return ApiResponse.success(health);
    }

    /**
     * 处理单个告警
     * 返回: 1-接收成功, 0-已去重
     */
    private int processAlert(WebhookAlertRequest request) throws Exception {
        // 生成指纹
        String fingerprint = deduplicationService.generateFingerprint(
                request.getAlertname(),
                request.getSeverity(),
                request.getInstance(),
                request.getLabels()
        );

        // 检查去重
        if (deduplicationService.isDuplicate(fingerprint)) {
            log.debug("告警已去重: fingerprint={}", fingerprint);
            return 0;
        }

        // 构建告警实体
        Alert alert = Alert.builder()
                .fingerprint(fingerprint)
                .source(request.getSource())
                .severity(request.getSeverity() != null ? request.getSeverity() : "warning")
                .alertname(request.getAlertname() != null ? request.getAlertname() : "unnamed")
                .instance(request.getInstance())
                .labels(objectMapper.writeValueAsString(request.getLabels()))
                .message(request.getMessage())
                .status("PENDING")
                .build();

        // 保存告警
        alert = alertRepository.save(alert);

        // 添加到聚合
        aggregationService.addToAggregation(alert);

        log.info("告警已接收并添加到聚合: id={}, fingerprint={}", alert.getId(), fingerprint);
        return 1;
    }

    /**
     * 映射Grafana状态到严重级别
     */
    private String mapGrafanaStateToSeverity(String state) {
        if (state == null) return "warning";

        return switch (state.toLowerCase()) {
            case "alerting" -> "critical";
            case "ok" -> "info";
            default -> "warning";
        };
    }

    /**
     * 映射Zabbix严重级别
     */
    private String mapZabbixSeverity(String severity) {
        if (severity == null) return "warning";

        return switch (severity.toLowerCase()) {
            case "disaster", "high" -> "critical";
            case "average" -> "warning";
            case "warning" -> "info";
            default -> "info";
        };
    }
}
