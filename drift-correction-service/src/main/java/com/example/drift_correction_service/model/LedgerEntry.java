package com.example.drift_correction_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Read-only entity to query ledger data from Shadow Ledger Service database
 */
@Entity
@Table(name = "ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {
    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "type")
    private String type;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "timestamp")
    private Long timestamp;
}

