package com.example.drift_correction_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a CBS (Core Banking System) balance report entry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CBSBalanceEntry {
    private String accountId;
    private BigDecimal reportedBalance;
}

