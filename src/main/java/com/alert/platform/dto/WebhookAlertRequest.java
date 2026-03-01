package com.alert.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "告警名称不能为空")
    @Size(max = 255, message = "告警名称长度不能超过255个字符")
    private String alertname;

    /**
     * 告警级别
     */
    @Size(max = 20, message = "告警级别长度不能超过20个字符")
    private String severity;

    /**
     * 实例标识
     */
    @Size(max = 255, message = "实例标识长度不能超过255个字符")
    private String instance;

    /**
     * 告警消息/内容
     */
    @Size(max = 65535, message = "告警消息长度不能超过65535个字符")
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
    @Size(max = 20, message = "状态长度不能超过20个字符")
    private String status;

    /**
     * 告警来源 (prometheus, grafana, zabbix, generic)
     */
    @Size(max = 50, message = "来源长度不能超过50个字符")
    private String source;
}
