package com.alert.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聚合规则配置实体
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Entity
@Table(name = "aggregation_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 规则名称
     */
    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    /**
     * 规则描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 匹配标签 (JSON格式, 例如: {"severity": "critical"})
     */
    @Column(name = "match_labels", columnDefinition = "TEXT")
    private String matchLabels;

    /**
     * 分组标签 (逗号分隔)
     */
    @Column(name = "group_by", length = 500)
    private String groupBy;

    /**
     * 分组等待时间 (秒)
     */
    @Column(name = "group_wait_seconds")
    @Builder.Default
    private Integer groupWaitSeconds = 30;

    /**
     * 最大聚合数量
     */
    @Column(name = "max_count")
    @Builder.Default
    private Integer maxCount = 50;

    /**
     * 最大等待时间 (秒)
     */
    @Column(name = "max_wait_seconds")
    @Builder.Default
    private Integer maxWaitSeconds = 300;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

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
