package com.example.drift_correction_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents the result of a drift detection check
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriftResult {
    private String accountId;
    private BigDecimal shadowBalance;
    private BigDecimal reportedBalance;
    private BigDecimal difference;
    private String status; // MATCH, MISMATCH
    private String mismatchType; // missing_credit, incorrect_debit, unknown
    private String correctionEventId; // if correction was generated
    private String message;
}

