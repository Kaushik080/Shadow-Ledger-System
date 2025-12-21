package com.example.drift_correction_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a correction event to be published to Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorrectionEvent {
    private String eventId;
    private String accountId;
    private String type; // credit or debit
    private BigDecimal amount;
    private Long timestamp;
    private String reason; // explanation for the correction
}

