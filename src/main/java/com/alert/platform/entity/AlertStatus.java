package com.alert.platform.entity;

/**
 * 告警状态枚举
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
public enum AlertStatus {

    /**
     * 待处理
     */
    PENDING("PENDING", "待处理"),

    /**
     * 已去重
     */
    DEDUPED("DEDUPED", "已去重"),

    /**
     * 聚合中
     */
    AGGREGATING("AGGREGATING", "聚合中"),

    /**
     * 已派发
     */
    DISPATCHED("DISPATCHED", "已派发"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 失败
     */
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    AlertStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AlertStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AlertStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
