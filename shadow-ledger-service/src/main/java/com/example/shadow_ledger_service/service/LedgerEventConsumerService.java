package com.example.shadow_ledger_service.service;

import com.example.shadow_ledger_service.model.LedgerEvent;
import com.example.shadow_ledger_service.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service that consumes events from Kafka and maintains the shadow ledger
 */
@Service
public class LedgerEventConsumerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerEventConsumerService.class);

    private final LedgerRepository ledgerRepository;

    public LedgerEventConsumerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Consume events from transactions.raw topic
     */
    @KafkaListener(topics = "${kafka.topics.transactions-raw}", groupId = "shadow-ledger-consumer-group")
    @Transactional
    public void consumeRawTransaction(@Payload LedgerEvent event,
                                     @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                     Acknowledgment acknowledgment) {
        try {
            log.info("Received raw transaction: key={}, eventId={}, accountId={}, type={}, amount={}",
                    key, event.getEventId(), event.getAccountId(), event.getType(), event.getAmount());

            processEvent(event);

            // Acknowledge the message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Error processing raw transaction: key={}", key, e);
            // In production, implement dead letter queue handling
            throw new RuntimeException("Failed to process event", e);
        }
    }

    /**
     * Consume correction events from transactions.corrections topic
     */
    @KafkaListener(topics = "${kafka.topics.transactions-corrections}", groupId = "shadow-ledger-consumer-group")
    @Transactional
    public void consumeCorrectionTransaction(@Payload LedgerEvent event,
                                            @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                            Acknowledgment acknowledgment) {
        try {
            log.info("Received correction transaction: key={}, eventId={}, accountId={}, type={}, amount={}",
                    key, event.getEventId(), event.getAccountId(), event.getType(), event.getAmount());

            processEvent(event);

            // Acknowledge the message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Error processing correction transaction: key={}", key, e);
            throw new RuntimeException("Failed to process correction event", e);
        }
    }

    /**
     * Process and persist event to ledger with deduplication
     */
    private void processEvent(LedgerEvent event) {
        // Deduplication check
        if (ledgerRepository.findByEventId(event.getEventId()).isPresent()) {
            log.warn("Duplicate event detected, skipping: eventId={}", event.getEventId());
            return;
        }

        // Validate that balance won't go negative
        BigDecimal currentBalance = ledgerRepository.getFinalBalance(event.getAccountId());
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }

        BigDecimal newBalance = currentBalance;
        if ("credit".equalsIgnoreCase(event.getType())) {
            newBalance = currentBalance.add(event.getAmount());
        } else if ("debit".equalsIgnoreCase(event.getType())) {
            newBalance = currentBalance.subtract(event.getAmount());
        }

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Transaction would result in negative balance: accountId={}, currentBalance={}, newBalance={}",
                    event.getAccountId(), currentBalance, newBalance);
            throw new IllegalStateException("Insufficient balance - transaction would result in negative balance");
        }

        // Persist to immutable ledger
        ledgerRepository.save(event);
        log.info("Event persisted to ledger: eventId={}, accountId={}, type={}, amount={}, newBalance={}",
                event.getEventId(), event.getAccountId(), event.getType(), event.getAmount(), newBalance);
    }
}

