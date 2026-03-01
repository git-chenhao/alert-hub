package com.alert.platform.entity;

/**
 * 告警严重级别枚举
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
public enum AlertSeverity {

    /**
     * 信息
     */
    INFO("info", "信息", 1),

    /**
     * 警告
     */
    WARNING("warning", "警告", 2),

    /**
     * 严重
     */
    CRITICAL("critical", "严重", 3);

    private final String code;
    private final String description;
    private final int level;

    AlertSeverity(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    public static AlertSeverity fromCode(String code) {
        if (code == null) {
            return WARNING;
        }
        for (AlertSeverity severity : values()) {
            if (severity.code.equalsIgnoreCase(code)) {
                return severity;
            }
        }
        return WARNING;
    }
}
