package com.alert.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 聚合配置
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alert.aggregation")
public class AggregationConfig {

    /**
     * 分组等待时间 (秒)
     */
    private Integer groupWaitSeconds = 30;

    /**
     * 最大聚合数量
     */
    private Integer maxCount = 50;

    /**
     * 最大等待时间 (秒)
     */
    private Integer maxWaitSeconds = 300;

    /**
     * 分组标签 (逗号分隔)
     */
    private String groupByLabels = "severity,alertname";
}
