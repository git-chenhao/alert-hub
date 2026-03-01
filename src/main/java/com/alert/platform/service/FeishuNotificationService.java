package com.alert.platform.service;

import com.alert.platform.config.FeishuConfig;
import com.alert.platform.entity.AlertBatch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 飞书通知服务
 * 分析完成后发送飞书卡片
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuNotificationService {

    private final FeishuConfig feishuConfig;
    private final ObjectMapper objectMapper;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * 发送飞书通知
     */
    public void sendNotification(AlertBatch batch, JsonNode agentResult) {
        if (!feishuConfig.getEnabled()) {
            log.debug("飞书通知未启用，跳过发送");
            return;
        }

        if (feishuConfig.getWebhookUrl() == null || feishuConfig.getWebhookUrl().isEmpty()) {
            log.warn("飞书Webhook URL未配置，跳过发送");
            return;
        }

        try {
            Map<String, Object> card = buildCard(batch, agentResult);
            sendToFeishu(card);
            log.info("飞书通知发送成功: batchKey={}", batch.getBatchKey());
        } catch (Exception e) {
            log.error("发送飞书通知失败: batchKey={}", batch.getBatchKey(), e);
        }
    }

    /**
     * 构建飞书卡片
     */
    private Map<String, Object> buildCard(AlertBatch batch, JsonNode agentResult) {
        Map<String, Object> card = new HashMap<>();

        // 卡片标题
        Map<String, Object> title = new HashMap<>();
        title.put("tag", "plain_text");
        title.put("content", "🔔 告警分析完成");
        card.put("title", title);

        // 卡片内容
        StringBuilder content = new StringBuilder();
        content.append("**批次信息**\n");
        content.append("- 批次名称: ").append(batch.getName()).append("\n");
        content.append("- 告警数量: ").append(batch.getAlertCount()).append("\n");
        content.append("- 创建时间: ")
                .append(batch.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\n");

        if (batch.getAgentDurationMs() != null) {
            content.append("- 分析耗时: ").append(batch.getAgentDurationMs()).append("ms\n");
        }

        // 解析分析结果
        if (agentResult != null && feishuConfig.getSendDetail()) {
            content.append("\n**分析结果**\n");

            if (agentResult.has("root_cause")) {
                content.append("- 根因分析: ").append(agentResult.get("root_cause").asText()).append("\n");
            }

            if (agentResult.has("suggestion")) {
                content.append("- 处理建议: ").append(agentResult.get("suggestion").asText()).append("\n");
            }

            if (agentResult.has("impact")) {
                content.append("- 影响范围: ").append(agentResult.get("impact").asText()).append("\n");
            }
        }

        Map<String, Object> textElement = new HashMap<>();
        textElement.put("tag", "lark_md");
        textElement.put("content", content.toString());

        // 构建卡片元素
        Map<String, Object>[] elements = new Map[]{
                textElement
        };

        card.put("elements", elements);

        return card;
    }

    /**
     * 发送到飞书 (修复 WebClient.block() 阻塞问题)
     */
    private Disposable sendToFeishu(Map<String, Object> card) {
        Map<String, Object> request = new HashMap<>();
        request.put("msg_type", "interactive");
        request.put("card", card);

        return webClient.post()
                .uri(feishuConfig.getWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .subscribe(
                        response -> log.debug("飞书响应: {}", response),
                        error -> log.error("发送飞书通知失败", error)
                );
    }
}
