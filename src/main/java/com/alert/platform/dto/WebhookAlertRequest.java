package com.alert.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Webhook告警请求DTO
 * 支持多种告警源格式
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookAlertRequest {

    /**
     * 告警名称
     */
    private String alertname;

    /**
     * 告警级别
     */
    private String severity;

    /**
     * 实例标识
     */
    private String instance;

    /**
     * 告警消息/内容
     */
    private String message;

    /**
     * 告警标签集合
     */
    private Map<String, String> labels;

    /**
     * 告警注解集合
     */
    private Map<String, String> annotations;

    /**
     * 告警开始时间
     */
    @JsonProperty("startsAt")
    private String startsAt;

    /**
     * 告警结束时间
     */
    @JsonProperty("endsAt")
    private String endsAt;

    /**
     * 告警状态 (firing, resolved)
     */
    private String status;

    /**
     * 告警来源 (prometheus, grafana, zabbix, generic)
     */
    private String source;
}
