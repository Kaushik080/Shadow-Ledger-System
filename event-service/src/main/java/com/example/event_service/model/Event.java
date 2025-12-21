package com.example.event_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @NotBlank(message = "eventId is required")
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @NotBlank(message = "accountId is required")
    @Column(name = "account_id", nullable = false)
    private String accountId;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "debit|credit", message = "type must be 'debit' or 'credit'")
    @Column(name = "type", nullable = false)
    private String type;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be greater than 0")
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @JsonIgnore
    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = System.currentTimeMillis();
        }
    }
}

