package com.example.drift_correction_service.service;

import com.example.drift_correction_service.model.CorrectionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test 4: Correction Event Generation Test
 * Tests that correction events are correctly generated and published
 */
public class CorrectionEventGenerationTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private DriftService driftService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        driftService = new DriftService(null, kafkaTemplate);
    }

    @Test
    public void testCorrectionEventGeneration_Credit() {
        // Arrange
        String accountId = "A10";
        String type = "credit";
        BigDecimal amount = new BigDecimal("100.00");
        String reason = "Test correction";

        // Act
        CorrectionEvent correction = driftService.generateCorrectionEvent(accountId, type, amount, reason);

        // Assert
        assertThat(correction).isNotNull();
        assertThat(correction.getEventId()).isNotNull();
        assertThat(correction.getEventId()).startsWith("CORR-" + accountId);
        assertThat(correction.getAccountId()).isEqualTo(accountId);
        assertThat(correction.getType()).isEqualTo(type);
        assertThat(correction.getAmount()).isEqualByComparingTo(amount);
        assertThat(correction.getReason()).isEqualTo(reason);
        assertThat(correction.getTimestamp()).isNotNull();
        assertThat(correction.getTimestamp()).isGreaterThan(0L);
    }

    @Test
    public void testCorrectionEventGeneration_Debit() {
        // Arrange
        String accountId = "A20";
        String type = "debit";
        BigDecimal amount = new BigDecimal("50.00");
        String reason = "Reverse incorrect credit";

        // Act
        CorrectionEvent correction = driftService.generateCorrectionEvent(accountId, type, amount, reason);

        // Assert
        assertThat(correction).isNotNull();
        assertThat(correction.getEventId()).startsWith("CORR-" + accountId);
        assertThat(correction.getAccountId()).isEqualTo(accountId);
        assertThat(correction.getType()).isEqualTo(type);
        assertThat(correction.getAmount()).isEqualByComparingTo(amount);
        assertThat(correction.getReason()).isEqualTo(reason);
    }

    @Test
    public void testCorrectionEventGeneration_UniqueEventIds() {
        // Arrange
        String accountId = "A30";
        String type = "credit";
        BigDecimal amount = new BigDecimal("100.00");
        String reason = "Test";

        // Act - generate two correction events
        CorrectionEvent correction1 = driftService.generateCorrectionEvent(accountId, type, amount, reason);
        CorrectionEvent correction2 = driftService.generateCorrectionEvent(accountId, type, amount, reason);

        // Assert - event IDs should be unique
        assertThat(correction1.getEventId()).isNotEqualTo(correction2.getEventId());
    }

    @Test
    public void testCorrectionEventPublishing_Success() {
        // Arrange
        String accountId = "A40";
        CorrectionEvent correction = new CorrectionEvent();
        correction.setEventId("CORR-A40-test");
        correction.setAccountId(accountId);
        correction.setType("credit");
        correction.setAmount(new BigDecimal("100.00"));
        correction.setTimestamp(System.currentTimeMillis());
        correction.setReason("Test");

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        driftService.publishCorrectionEvent(correction);

        // Assert - verify Kafka send was called
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CorrectionEvent> eventCaptor = ArgumentCaptor.forClass(CorrectionEvent.class);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo(accountId);
        assertThat(eventCaptor.getValue()).isEqualTo(correction);
    }

    @Test
    public void testCorrectionEventFormat_HasRequiredFields() {
        // Arrange
        String accountId = "A50";
        String type = "credit";
        BigDecimal amount = new BigDecimal("200.00");
        String reason = "Auto-correction: missing credit detected";

        // Act
        CorrectionEvent correction = driftService.generateCorrectionEvent(accountId, type, amount, reason);

        // Assert - verify all required fields are present
        assertThat(correction.getEventId()).isNotNull().isNotEmpty();
        assertThat(correction.getAccountId()).isNotNull().isNotEmpty();
        assertThat(correction.getType()).isNotNull().isNotEmpty();
        assertThat(correction.getAmount()).isNotNull().isGreaterThan(BigDecimal.ZERO);
        assertThat(correction.getTimestamp()).isNotNull().isGreaterThan(0L);
        assertThat(correction.getReason()).isNotNull().isNotEmpty();
    }

    @Test
    public void testCorrectionEventGeneration_LargeAmount() {
        // Arrange
        String accountId = "A60";
        String type = "credit";
        BigDecimal amount = new BigDecimal("999999.99");
        String reason = "Large correction";

        // Act
        CorrectionEvent correction = driftService.generateCorrectionEvent(accountId, type, amount, reason);

        // Assert
        assertThat(correction.getAmount()).isEqualByComparingTo(amount);
        assertThat(correction.getAmount().scale()).isEqualTo(2); // Verify decimal precision
    }

    @Test
    public void testCorrectionEventGeneration_SmallAmount() {
        // Arrange
        String accountId = "A70";
        String type = "debit";
        BigDecimal amount = new BigDecimal("0.01"); // 1 cent
        String reason = "Minimum correction";

        // Act
        CorrectionEvent correction = driftService.generateCorrectionEvent(accountId, type, amount, reason);

        // Assert
        assertThat(correction.getAmount()).isEqualByComparingTo(amount);
    }

    @Test
    public void testCorrectionEventIdFormat() {
        // Arrange
        String accountId = "TEST-ACCOUNT-123";
        String type = "credit";
        BigDecimal amount = new BigDecimal("100.00");
        String reason = "Test";

        // Act
        CorrectionEvent correction = driftService.generateCorrectionEvent(accountId, type, amount, reason);

        // Assert - verify event ID format
        String eventId = correction.getEventId();
        assertThat(eventId).matches("CORR-TEST-ACCOUNT-123-[a-f0-9]{8}");
    }
}

