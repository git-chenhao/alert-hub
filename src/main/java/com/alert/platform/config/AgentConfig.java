package com.alert.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Sub-Agent配置 (A2A协议)
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alert.agent")
public class AgentConfig {

    /**
     * Sub-Agent端点
     */
    private String endpoint = "http://localhost:9001/api/agent/analyze";

    /**
     * 请求超时 (秒)
     */
    private Integer timeoutSeconds = 60;

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;
}
