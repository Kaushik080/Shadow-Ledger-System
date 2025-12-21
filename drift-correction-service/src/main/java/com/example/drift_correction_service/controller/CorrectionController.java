package com.example.drift_correction_service.controller;

import com.example.drift_correction_service.model.CorrectionEvent;
import com.example.drift_correction_service.service.DriftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller for manual corrections
 */
@RestController
@RequestMapping("/correct")
public class CorrectionController {

    private static final Logger log = LoggerFactory.getLogger(CorrectionController.class);

    private final DriftService driftService;

    public CorrectionController(DriftService driftService) {
        this.driftService = driftService;
    }

    /**
     * POST /correct/{accountId}
     * Manually trigger a correction for an account
     */
    @PostMapping("/{accountId}")
    public ResponseEntity<?> manualCorrection(
            @PathVariable String accountId,
            @RequestBody ManualCorrectionRequest request,
            @RequestHeader(name = "X-Trace-Id", required = false) String traceId) {

        // Set trace ID in MDC for logging
        if (traceId != null) {
            MDC.put("X-Trace-Id", traceId);
        }

        try {
            log.info("Manual correction requested for accountId={}, type={}, amount={}",
                    accountId, request.type(), request.amount());

            // Validate request
            if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Amount must be greater than 0"));
            }

            if (!request.type().equalsIgnoreCase("credit") && !request.type().equalsIgnoreCase("debit")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Type must be 'credit' or 'debit'"));
            }

            String reason = request.reason() != null ? request.reason() : "Manual correction";

            // Generate and publish correction event
            CorrectionEvent correction = driftService.generateCorrectionEvent(
                    accountId,
                    request.type().toLowerCase(),
                    request.amount(),
                    reason
            );

            driftService.publishCorrectionEvent(correction);

            log.info("Manual correction published: eventId={}", correction.getEventId());

            return ResponseEntity.ok(Map.of(
                    "message", "Correction event published",
                    "correctionEventId", correction.getEventId(),
                    "accountId", accountId,
                    "type", correction.getType(),
                    "amount", correction.getAmount()
            ));

        } catch (Exception e) {
            log.error("Error processing manual correction for accountId={}", accountId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process correction: " + e.getMessage()));
        } finally {
            MDC.clear();
        }
    }

    /**
     * Request for manual correction
     */
    public record ManualCorrectionRequest(
            String type,
            BigDecimal amount,
            String reason
    ) {}
}

