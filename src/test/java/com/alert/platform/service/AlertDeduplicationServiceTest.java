package com.alert.platform.service;

import com.alert.platform.config.AlertConfig;
import com.alert.platform.entity.Alert;
import com.alert.platform.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 告警去重服务测试
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AlertDeduplicationServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertConfig alertConfig;

    @InjectMocks
    private AlertDeduplicationService deduplicationService;

    private Alert testAlert;

    @BeforeEach
    void setUp() {
        testAlert = Alert.builder()
                .id(1L)
                .fingerprint("test-fingerprint")
                .source("test")
                .severity("critical")
                .alertname("TestAlert")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGenerateFingerprint() {
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");
        labels.put("region", "us-east");

        String fingerprint = deduplicationService.generateFingerprint(
                "TestAlert",
                "critical",
                "server-1",
                labels
        );

        assertNotNull(fingerprint);
        assertEquals(32, fingerprint.length()); // MD5 hash length
    }

    @Test
    void testIsDuplicate_WhenDuplicateExists() {
        when(alertConfig.getEnabled()).thenReturn(true);
        when(alertConfig.getWindowMinutes()).thenReturn(10);
        when(alertRepository.findByFingerprintAndCreatedAtAfter(anyString(), any()))
                .thenReturn(Optional.of(testAlert));

        boolean isDuplicate = deduplicationService.isDuplicate("test-fingerprint");

        assertTrue(isDuplicate);
        verify(alertRepository).findByFingerprintAndCreatedAtAfter(anyString(), any());
    }

    @Test
    void testIsDuplicate_WhenNoDuplicate() {
        when(alertConfig.getEnabled()).thenReturn(true);
        when(alertConfig.getWindowMinutes()).thenReturn(10);
        when(alertRepository.findByFingerprintAndCreatedAtAfter(anyString(), any()))
                .thenReturn(Optional.empty());

        boolean isDuplicate = deduplicationService.isDuplicate("test-fingerprint");

        assertFalse(isDuplicate);
    }

    @Test
    void testIsDuplicate_WhenDeduplicationDisabled() {
        when(alertConfig.getEnabled()).thenReturn(false);

        boolean isDuplicate = deduplicationService.isDuplicate("test-fingerprint");

        assertFalse(isDuplicate);
        verify(alertRepository, never()).findByFingerprintAndCreatedAtAfter(anyString(), any());
    }
}
