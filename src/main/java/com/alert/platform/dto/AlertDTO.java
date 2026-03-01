package com.alert.platform.dto;

import com.alert.platform.entity.Alert;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 告警数据传输对象
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertDTO {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Long id;
    private String fingerprint;
    private String source;
    private String severity;
    private String alertname;
    private String instance;
    private Map<String, String> labels;
    private String message;
    private String status;
    private Long batchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换为DTO
     */
    public static AlertDTO fromEntity(Alert alert) {
        return AlertDTO.builder()
                .id(alert.getId())
                .fingerprint(alert.getFingerprint())
                .source(alert.getSource())
                .severity(alert.getSeverity())
                .alertname(alert.getAlertname())
                .instance(alert.getInstance())
                .labels(parseLabels(alert.getLabels()))
                .message(alert.getMessage())
                .status(alert.getStatus())
                .batchId(alert.getBatchId())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }

    /**
     * 解析标签JSON字符串
     */
    private static Map<String, String> parseLabels(String labelsJson) {
        try {
            if (labelsJson == null || labelsJson.isEmpty()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(labelsJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
