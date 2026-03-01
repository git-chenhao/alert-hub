package com.alert.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警批次实体
 * 用于聚合管理一组相关的告警
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Entity
@Table(name = "alert_batches", indexes = {
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 批次唯一标识
     */
    @Column(name = "batch_key", unique = true, nullable = false, length = 64)
    private String batchKey;

    /**
     * 批次名称 (基于分组规则生成)
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * 批次告警数量
     */
    @Column(name = "alert_count", nullable = false)
    @Builder.Default
    private Integer alertCount = 0;

    /**
     * 批次状态 (PENDING, AGGREGATING, DISPATCHED, COMPLETED, FAILED)
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * 分组标签 (JSON格式)
     */
    @Column(name = "group_labels", columnDefinition = "TEXT")
    private String groupLabels;

    /**
     * Sub-Agent分析结果 (JSON格式)
     */
    @Column(name = "agent_result", columnDefinition = "TEXT")
    private String agentResult;

    /**
     * Agent分析耗时 (毫秒)
     */
    @Column(name = "agent_duration_ms")
    private Long agentDurationMs;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 开始聚合时间
     */
    @Column(name = "aggregated_at")
    private LocalDateTime aggregatedAt;

    /**
     * 派发时间
     */
    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (alertCount == null) {
            alertCount = 0;
        }
    }
}
