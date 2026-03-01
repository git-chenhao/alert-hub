package com.alert.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 告警聚合配置
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alert.deduplication")
public class AlertConfig {

    /**
     * 是否启用去重
     */
    private Boolean enabled = true;

    /**
     * 去重窗口时间 (分钟)
     */
    private Integer windowMinutes = 10;
}
