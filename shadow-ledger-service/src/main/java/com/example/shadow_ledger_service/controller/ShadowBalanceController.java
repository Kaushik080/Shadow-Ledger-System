package com.example.shadow_ledger_service.controller;

import com.example.shadow_ledger_service.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller for querying shadow balance
 */
@RestController
@RequestMapping("/accounts")
public class ShadowBalanceController {

    private static final Logger log = LoggerFactory.getLogger(ShadowBalanceController.class);

    private final LedgerRepository ledgerRepository;

    public ShadowBalanceController(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * GET /accounts/{accountId}/shadow-balance
     * Returns the computed shadow balance for an account
     */
    @GetMapping("/{accountId}/shadow-balance")
    public ResponseEntity<Map<String, Object>> getShadowBalance(
            @PathVariable String accountId,
            @RequestHeader(name = "X-Trace-Id", required = false) String traceId) {

        // Set trace ID in MDC for logging
        if (traceId != null) {
            MDC.put("X-Trace-Id", traceId);
        }

        try {
            log.info("Fetching shadow balance for accountId={}", accountId);

            BigDecimal balance = ledgerRepository.getFinalBalance(accountId);
            if (balance == null) {
                balance = BigDecimal.ZERO;
            }

            String lastEventId = ledgerRepository.getLastEventId(accountId);

            log.info("Shadow balance retrieved: accountId={}, balance={}, lastEvent={}",
                    accountId, balance, lastEventId);

            return ResponseEntity.ok(Map.of(
                    "accountId", accountId,
                    "balance", balance,
                    "lastEvent", lastEventId != null ? lastEventId : "none"
            ));

        } catch (Exception e) {
            log.error("Error fetching shadow balance for accountId={}", accountId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve shadow balance"));
        } finally {
            MDC.clear();
        }
    }

    /**
     * GET /accounts/service-status
     * Simple endpoint for API Gateway to verify service is available
     */
    @GetMapping("/service-status")
    public ResponseEntity<Map<String, String>> getServiceStatus() {
        return ResponseEntity.ok(Map.of(
                "service", "shadow-ledger-service",
                "status", "UP",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}

