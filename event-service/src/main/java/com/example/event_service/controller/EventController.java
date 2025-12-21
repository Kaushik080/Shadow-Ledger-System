package com.example.event_service.controller;

import com.example.event_service.model.Event;
import com.example.event_service.repository.EventRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/events")
@Validated
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final EventRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.transactions-raw:transactions.raw}")
    private String transactionsRawTopic;

    public EventController(EventRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Event event,
                                    @RequestHeader(name = "X-Trace-Id", required = false) String traceId) {
        // Set trace ID in MDC for logging
        if (traceId != null) {
            MDC.put("X-Trace-Id", traceId);
        }

        try {
            log.info("Received event submission: eventId={}, accountId={}, type={}, amount={}",
                    event.getEventId(), event.getAccountId(), event.getType(), event.getAmount());

            // Additional validation for type (already validated by @Pattern but good to be explicit)
            String type = event.getType().toLowerCase();
            if (!type.equals("debit") && !type.equals("credit")) {
                log.warn("Invalid event type: {}", event.getType());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "type must be 'debit' or 'credit'"));
            }
            event.setType(type); // normalize to lowercase

            // Idempotency check
            Optional<Event> existing = repository.findByEventId(event.getEventId());
            if (existing.isPresent()) {
                log.warn("Duplicate eventId detected: {}", event.getEventId());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "duplicate eventId", "eventId", event.getEventId()));
            }

            // Persist for traceability (timestamp auto-generated via @PrePersist)
            Event saved = repository.save(event);
            log.info("Event persisted: eventId={}", saved.getEventId());

            // Produce to Kafka
            kafkaTemplate.send(transactionsRawTopic, event.getAccountId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Event published to Kafka: eventId={}, topic={}",
                                    event.getEventId(), transactionsRawTopic);
                        } else {
                            log.error("Failed to publish event to Kafka: eventId={}",
                                    event.getEventId(), ex);
                        }
                    });

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of(
                            "status", "queued",
                            "eventId", event.getEventId(),
                            "traceId", traceId != null ? traceId : "none"
                    ));

        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity violation for eventId={}", event.getEventId(), ex);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "duplicate eventId or invalid data"));
        } catch (Exception ex) {
            log.error("Unexpected error processing event: eventId={}", event.getEventId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "internal server error"));
        } finally {
            MDC.clear();
        }
    }
}

