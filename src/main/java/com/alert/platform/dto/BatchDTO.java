package com.alert.platform.dto;

import com.alert.platform.entity.AlertBatch;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警批次数据传输对象
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchDTO {

    private Long id;
    private String batchKey;
    private String name;
    private Integer alertCount;
    private String status;
    private Map<String, String> groupLabels;
    private String agentResult;
    private Long agentDurationMs;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime aggregatedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime completedAt;

    /**
     * 从实体转换为DTO
     */
    public static BatchDTO fromEntity(AlertBatch batch) {
        return BatchDTO.builder()
                .id(batch.getId())
                .batchKey(batch.getBatchKey())
                .name(batch.getName())
                .alertCount(batch.getAlertCount())
                .status(batch.getStatus())
                .groupLabels(parseLabels(batch.getGroupLabels()))
                .agentResult(batch.getAgentResult())
                .agentDurationMs(batch.getAgentDurationMs())
                .errorMessage(batch.getErrorMessage())
                .createdAt(batch.getCreatedAt())
                .aggregatedAt(batch.getAggregatedAt())
                .dispatchedAt(batch.getDispatchedAt())
                .completedAt(batch.getCompletedAt())
                .build();
    }

    /**
     * 解析标签JSON字符串
     */
    private static Map<String, String> parseLabels(String labelsJson) {
        // 简化处理，实际应使用ObjectMapper
        return null;
    }
}
