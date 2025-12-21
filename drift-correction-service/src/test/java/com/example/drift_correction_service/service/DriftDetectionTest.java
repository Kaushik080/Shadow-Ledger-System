package com.example.drift_correction_service.service;

import com.example.drift_correction_service.model.CBSBalanceEntry;
import com.example.drift_correction_service.model.CorrectionEvent;
import com.example.drift_correction_service.model.DriftResult;
import com.example.drift_correction_service.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test 3: Drift Detection Test
 * Tests that difference between CBS and shadow balance is correctly identified
 */
public class DriftDetectionTest {

    @Mock
    private LedgerEntryRepository ledgerRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private DriftService driftService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        driftService = new DriftService(ledgerRepository, kafkaTemplate);
    }

    @Test
    public void testDriftDetection_NoMismatch() {
        // Arrange
        String accountId = "A10";
        BigDecimal shadowBalance = new BigDecimal("1000.00");
        BigDecimal reportedBalance = new BigDecimal("1000.00");

        when(ledgerRepository.getShadowBalance(accountId)).thenReturn(shadowBalance);

        CBSBalanceEntry cbsEntry = new CBSBalanceEntry(accountId, reportedBalance);
        List<CBSBalanceEntry> cbsBalances = Arrays.asList(cbsEntry);

        // Act
        List<DriftResult> results = driftService.checkDrift(cbsBalances);

        // Assert
        assertThat(results).hasSize(1);
        DriftResult result = results.get(0);
        assertThat(result.getAccountId()).isEqualTo(accountId);
        assertThat(result.getStatus()).isEqualTo("MATCH");
        assertThat(result.getShadowBalance()).isEqualByComparingTo(shadowBalance);
        assertThat(result.getReportedBalance()).isEqualByComparingTo(reportedBalance);

        // Verify no correction event was published
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    public void testDriftDetection_MissingCredit() {
        // Arrange
        String accountId = "A20";
        BigDecimal shadowBalance = new BigDecimal("950.00");
        BigDecimal reportedBalance = new BigDecimal("1000.00");
        BigDecimal difference = new BigDecimal("50.00");

        when(ledgerRepository.getShadowBalance(accountId)).thenReturn(shadowBalance);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        CBSBalanceEntry cbsEntry = new CBSBalanceEntry(accountId, reportedBalance);
        List<CBSBalanceEntry> cbsBalances = Arrays.asList(cbsEntry);

        // Act
        List<DriftResult> results = driftService.checkDrift(cbsBalances);

        // Assert
        assertThat(results).hasSize(1);
        DriftResult result = results.get(0);
        assertThat(result.getAccountId()).isEqualTo(accountId);
        assertThat(result.getStatus()).isEqualTo("MISMATCH");
        assertThat(result.getMismatchType()).isEqualTo("missing_credit");
        assertThat(result.getDifference()).isEqualByComparingTo(difference);
        assertThat(result.getCorrectionEventId()).isNotNull();
        assertThat(result.getCorrectionEventId()).startsWith("CORR-" + accountId);

        // Verify correction event was published
        ArgumentCaptor<CorrectionEvent> eventCaptor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(kafkaTemplate, times(1)).send(anyString(), eq(accountId), eventCaptor.capture());

        CorrectionEvent correction = eventCaptor.getValue();
        assertThat(correction.getAccountId()).isEqualTo(accountId);
        assertThat(correction.getType()).isEqualTo("credit");
        assertThat(correction.getAmount()).isEqualByComparingTo(difference);
    }

    @Test
    public void testDriftDetection_IncorrectDebit() {
        // Arrange
        String accountId = "A30";
        BigDecimal shadowBalance = new BigDecimal("1050.00");
        BigDecimal reportedBalance = new BigDecimal("1000.00");
        BigDecimal difference = new BigDecimal("-50.00");

        when(ledgerRepository.getShadowBalance(accountId)).thenReturn(shadowBalance);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        CBSBalanceEntry cbsEntry = new CBSBalanceEntry(accountId, reportedBalance);
        List<CBSBalanceEntry> cbsBalances = Arrays.asList(cbsEntry);

        // Act
        List<DriftResult> results = driftService.checkDrift(cbsBalances);

        // Assert
        assertThat(results).hasSize(1);
        DriftResult result = results.get(0);
        assertThat(result.getAccountId()).isEqualTo(accountId);
        assertThat(result.getStatus()).isEqualTo("MISMATCH");
        assertThat(result.getMismatchType()).isEqualTo("incorrect_debit");
        assertThat(result.getDifference()).isEqualByComparingTo(difference);
        assertThat(result.getCorrectionEventId()).isNotNull();

        // Verify correction event was published
        ArgumentCaptor<CorrectionEvent> eventCaptor = ArgumentCaptor.forClass(CorrectionEvent.class);
        verify(kafkaTemplate, times(1)).send(anyString(), eq(accountId), eventCaptor.capture());

        CorrectionEvent correction = eventCaptor.getValue();
        assertThat(correction.getAccountId()).isEqualTo(accountId);
        assertThat(correction.getType()).isEqualTo("debit");
        assertThat(correction.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    public void testDriftDetection_MultipleAccounts() {
        // Arrange
        when(ledgerRepository.getShadowBalance("A10")).thenReturn(new BigDecimal("1000.00"));
        when(ledgerRepository.getShadowBalance("A20")).thenReturn(new BigDecimal("950.00"));
        when(ledgerRepository.getShadowBalance("A30")).thenReturn(new BigDecimal("1050.00"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        List<CBSBalanceEntry> cbsBalances = Arrays.asList(
            new CBSBalanceEntry("A10", new BigDecimal("1000.00")), // Match
            new CBSBalanceEntry("A20", new BigDecimal("1000.00")), // Missing credit
            new CBSBalanceEntry("A30", new BigDecimal("1000.00"))  // Incorrect debit
        );

        // Act
        List<DriftResult> results = driftService.checkDrift(cbsBalances);

        // Assert
        assertThat(results).hasSize(3);

        // Verify match
        assertThat(results.get(0).getStatus()).isEqualTo("MATCH");

        // Verify mismatches
        assertThat(results.get(1).getStatus()).isEqualTo("MISMATCH");
        assertThat(results.get(1).getMismatchType()).isEqualTo("missing_credit");

        assertThat(results.get(2).getStatus()).isEqualTo("MISMATCH");
        assertThat(results.get(2).getMismatchType()).isEqualTo("incorrect_debit");

        // Verify 2 correction events were published
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), any(CorrectionEvent.class));
    }

    @Test
    public void testDriftDetection_WithinTolerance() {
        // Arrange - difference within 0.01 tolerance
        String accountId = "A40";
        BigDecimal shadowBalance = new BigDecimal("1000.00");
        BigDecimal reportedBalance = new BigDecimal("1000.005"); // 0.005 difference

        when(ledgerRepository.getShadowBalance(accountId)).thenReturn(shadowBalance);

        CBSBalanceEntry cbsEntry = new CBSBalanceEntry(accountId, reportedBalance);
        List<CBSBalanceEntry> cbsBalances = Arrays.asList(cbsEntry);

        // Act
        List<DriftResult> results = driftService.checkDrift(cbsBalances);

        // Assert - should be considered a match within tolerance
        assertThat(results).hasSize(1);
        DriftResult result = results.get(0);
        assertThat(result.getStatus()).isEqualTo("MATCH");

        // Verify no correction event was published
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    public void testDriftDetection_EmptyAccount() {
        // Arrange - account with no transactions
        String accountId = "A50";
        BigDecimal reportedBalance = new BigDecimal("0.00");

        when(ledgerRepository.getShadowBalance(accountId)).thenReturn(null); // No ledger entries

        CBSBalanceEntry cbsEntry = new CBSBalanceEntry(accountId, reportedBalance);
        List<CBSBalanceEntry> cbsBalances = Arrays.asList(cbsEntry);

        // Act
        List<DriftResult> results = driftService.checkDrift(cbsBalances);

        // Assert
        assertThat(results).hasSize(1);
        DriftResult result = results.get(0);
        assertThat(result.getStatus()).isEqualTo("MATCH");
        assertThat(result.getShadowBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

