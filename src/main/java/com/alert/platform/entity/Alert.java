package com.alert.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警实体
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_fingerprint", columnList = "fingerprint"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_batch_id", columnList = "batch_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 告警指纹 (用于去重)
     * 格式: MD5(alertname + severity + instance + labels)
     */
    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    /**
     * 告警来源 (prometheus, grafana, zabbix, generic)
     */
    @Column(name = "source", nullable = false, length = 50)
    private String source;

    /**
     * 告警级别 (critical, warning, info)
     */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    /**
     * 告警名称
     */
    @Column(name = "alertname", nullable = false, length = 255)
    private String alertname;

    /**
     * 实例标识
     */
    @Column(name = "instance", length = 255)
    private String instance;

    /**
     * 标签集合 (JSON格式存储)
     */
    @Column(name = "labels", columnDefinition = "TEXT")
    private String labels;

    /**
     * 告警内容
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * 告警状态 (PENDING, DEDUPED, AGGREGATING, DISPATCHED, COMPLETED)
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * 所属批次ID
     */
    @Column(name = "batch_id")
    private Long batchId;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
