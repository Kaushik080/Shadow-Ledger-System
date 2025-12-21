package com.example.drift_correction_service.service;

import com.example.drift_correction_service.model.*;
import com.example.drift_correction_service.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for drift detection and correction
 */
@Service
public class DriftService {

    private static final Logger log = LoggerFactory.getLogger(DriftService.class);
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01"); // 1 cent tolerance

    private final LedgerEntryRepository ledgerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.transactions-corrections}")
    private String correctionsTopic;

    public DriftService(LedgerEntryRepository ledgerRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.ledgerRepository = ledgerRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Check drift between CBS balance and shadow balance
     */
    public List<DriftResult> checkDrift(List<CBSBalanceEntry> cbsBalances) {
        List<DriftResult> results = new ArrayList<>();

        for (CBSBalanceEntry cbs : cbsBalances) {
            DriftResult result = checkAccountDrift(cbs);
            results.add(result);
        }

        return results;
    }

    /**
     * Check drift for a single account
     */
    private DriftResult checkAccountDrift(CBSBalanceEntry cbs) {
        String accountId = cbs.getAccountId();
        BigDecimal reportedBalance = cbs.getReportedBalance();

        log.info("Checking drift for accountId={}, reportedBalance={}", accountId, reportedBalance);

        BigDecimal shadowBalance = ledgerRepository.getShadowBalance(accountId);
        if (shadowBalance == null) {
            shadowBalance = BigDecimal.ZERO;
        }

        BigDecimal difference = reportedBalance.subtract(shadowBalance);

        DriftResult result = new DriftResult();
        result.setAccountId(accountId);
        result.setShadowBalance(shadowBalance);
        result.setReportedBalance(reportedBalance);
        result.setDifference(difference);

        // Check if balances match within tolerance
        if (difference.abs().compareTo(TOLERANCE) <= 0) {
            result.setStatus("MATCH");
            result.setMessage("Balances match");
            log.info("Balance match for accountId={}", accountId);
        } else {
            result.setStatus("MISMATCH");

            // Determine mismatch type and generate correction if possible
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                // CBS balance is higher - missing credit in shadow ledger
                result.setMismatchType("missing_credit");
                result.setMessage("Shadow ledger is missing credit of " + difference.abs());

                // Generate correction event
                CorrectionEvent correction = generateCorrectionEvent(accountId, "credit", difference.abs(),
                        "Auto-correction: missing credit detected");
                result.setCorrectionEventId(correction.getEventId());

                publishCorrectionEvent(correction);

            } else {
                // CBS balance is lower - extra credit or missing debit in shadow ledger
                result.setMismatchType("incorrect_debit");
                result.setMessage("Shadow ledger has extra balance of " + difference.abs() + " - may need debit correction");

                // Generate debit correction
                CorrectionEvent correction = generateCorrectionEvent(accountId, "debit", difference.abs(),
                        "Auto-correction: incorrect debit detected");
                result.setCorrectionEventId(correction.getEventId());

                publishCorrectionEvent(correction);
            }

            log.warn("Balance mismatch for accountId={}, difference={}, type={}",
                    accountId, difference, result.getMismatchType());
        }

        return result;
    }

    /**
     * Generate a correction event
     */
    public CorrectionEvent generateCorrectionEvent(String accountId, String type, BigDecimal amount, String reason) {
        String correctionId = "CORR-" + accountId + "-" + UUID.randomUUID().toString().substring(0, 8);

        CorrectionEvent correction = new CorrectionEvent();
        correction.setEventId(correctionId);
        correction.setAccountId(accountId);
        correction.setType(type);
        correction.setAmount(amount);
        correction.setTimestamp(System.currentTimeMillis());
        correction.setReason(reason);

        log.info("Generated correction event: eventId={}, accountId={}, type={}, amount={}",
                correctionId, accountId, type, amount);

        return correction;
    }

    /**
     * Publish correction event to Kafka
     */
    public void publishCorrectionEvent(CorrectionEvent correction) {
        try {
            kafkaTemplate.send(correctionsTopic, correction.getAccountId(), correction)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Correction event published: eventId={}, topic={}",
                                    correction.getEventId(), correctionsTopic);
                        } else {
                            log.error("Failed to publish correction event: eventId={}",
                                    correction.getEventId(), ex);
                        }
                    });
            log.info("Correction event queued for publishing: eventId={}", correction.getEventId());
        } catch (Exception e) {
            log.error("Error publishing correction event: eventId={} - Kafka may be unavailable. Drift detection will continue.",
                    correction.getEventId(), e);
            // Don't throw exception - allow drift detection to continue even if Kafka is down
        }
    }
}

