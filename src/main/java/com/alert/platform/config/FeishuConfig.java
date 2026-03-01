package com.alert.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 飞书通知配置
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alert.feishu")
public class FeishuConfig {

    /**
     * 是否启用飞书通知
     */
    private Boolean enabled = true;

    /**
     * 飞书Webhook URL
     */
    private String webhookUrl;

    /**
     * 是否发送详细分析结果
     */
    private Boolean sendDetail = true;
}
