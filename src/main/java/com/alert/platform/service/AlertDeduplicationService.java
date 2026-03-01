package com.alert.platform.service;

import com.alert.platform.config.AlertConfig;
import com.alert.platform.entity.Alert;
import com.alert.platform.exception.BusinessException;
import com.alert.platform.repository.AlertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * 告警去重服务
 * 基于指纹去重，指纹生成规则：MD5(alertname + severity + instance + labels)
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertDeduplicationService {

    private final AlertRepository alertRepository;
    private final AlertConfig alertConfig;
    private final ObjectMapper objectMapper;

    /**
     * 生成告警指纹
     */
    public String generateFingerprint(String alertname, String severity,
                                      String instance, Map<String, String> labels) {
        try {
            // 构建指纹字符串
            StringBuilder sb = new StringBuilder();
            sb.append(alertname != null ? alertname : "");
            sb.append("|");
            sb.append(severity != null ? severity : "");
            sb.append("|");
            sb.append(instance != null ? instance : "");
            sb.append("|");

            // 对标签按key排序后拼接，确保一致性
            if (labels != null && !labels.isEmpty()) {
                labels.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> sb.append(entry.getKey())
                                .append("=")
                                .append(entry.getValue())
                                .append("&"));
            }

            // 计算MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("生成指纹失败", e);
            throw new BusinessException("生成指纹失败: " + e.getMessage());
        }
    }

    /**
     * 检查告警是否重复
     * 如果在去重窗口时间内存在相同指纹的告警，则认为重复
     */
    public boolean isDuplicate(String fingerprint) {
        if (!alertConfig.getEnabled()) {
            return false;
        }

        try {
            LocalDateTime since = LocalDateTime.now()
                    .minusMinutes(alertConfig.getWindowMinutes());
            Optional<Alert> existingAlert = alertRepository
                    .findByFingerprintAndCreatedAtAfter(fingerprint, since);

            if (existingAlert.isPresent()) {
                log.debug("告警已存在，跳过处理: fingerprint={}", fingerprint);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查告警去重失败", e);
            return false;
        }
    }

    /**
     * 检查并标记重复告警
     * 如果重复，则将原告警状态更新为DEDUPED
     */
    public Alert checkAndHandleDuplicate(Alert alert) {
        if (isDuplicate(alert.getFingerprint())) {
            log.info("告警重复，标记为DEDUPED: fingerprint={}", alert.getFingerprint());
            alert.setStatus("DEDUPED");
            return alertRepository.save(alert);
        }
        return alert;
    }
}
