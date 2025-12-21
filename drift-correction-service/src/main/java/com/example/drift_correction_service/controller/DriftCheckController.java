package com.example.drift_correction_service.controller;

import com.example.drift_correction_service.model.CBSBalanceEntry;
import com.example.drift_correction_service.model.DriftResult;
import com.example.drift_correction_service.service.DriftService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for drift detection
 */
@RestController
@RequestMapping("/drift-check")
public class DriftCheckController {

    private static final Logger log = LoggerFactory.getLogger(DriftCheckController.class);

    private final DriftService driftService;

    public DriftCheckController(DriftService driftService) {
        this.driftService = driftService;
    }

    /**
     * POST /drift-check
     * Accept CBS balance file and compare with shadow ledger
     */
    @PostMapping
    public ResponseEntity<?> checkDrift(
            @Valid @RequestBody List<CBSBalanceEntry> cbsBalances,
            @RequestHeader(name = "X-Trace-Id", required = false) String traceId) {

        // Set trace ID in MDC for logging
        if (traceId != null) {
            MDC.put("X-Trace-Id", traceId);
        }

        try {
            log.info("Drift check requested for {} accounts", cbsBalances.size());

            if (cbsBalances.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "CBS balance list cannot be empty"));
            }

            List<DriftResult> results = driftService.checkDrift(cbsBalances);

            long mismatches = results.stream()
                    .filter(r -> "MISMATCH".equals(r.getStatus()))
                    .count();

            log.info("Drift check completed: {} accounts, {} mismatches found",
                    results.size(), mismatches);

            return ResponseEntity.ok(Map.of(
                    "totalAccounts", results.size(),
                    "mismatches", mismatches,
                    "results", results
            ));

        } catch (Exception e) {
            log.error("Error during drift check", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to perform drift check: " + e.getMessage()));
        } finally {
            MDC.clear();
        }
    }
}

