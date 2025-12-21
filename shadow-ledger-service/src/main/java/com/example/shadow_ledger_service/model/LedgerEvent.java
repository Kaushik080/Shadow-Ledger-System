package com.example.shadow_ledger_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerEvent {
    @Id
    @Column(name = "event_id", nullable = false, unique = true)
    @JsonProperty("eventId")
    private String eventId;

    @Column(name = "account_id", nullable = false)
    @JsonProperty("accountId")
    private String accountId;

    @Column(name = "type", nullable = false)
    @JsonProperty("type")
    private String type; // debit or credit

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @JsonProperty("amount")
    private BigDecimal amount;

    @Column(name = "timestamp", nullable = false)
    @JsonProperty("timestamp")
    private Long timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        // Auto-generate timestamp if not provided in the JSON message
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
    }
}

