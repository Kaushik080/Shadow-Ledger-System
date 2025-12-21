package com.example.event_service.controller;

import com.example.event_service.model.Event;
import com.example.event_service.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test 1: Event Validation Test
 * Tests that invalid events are correctly rejected
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class EventValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Test
    public void testEventValidation_MissingEventId_ShouldReturn400() throws Exception {
        Event event = new Event();
        event.setAccountId("A10");
        event.setType("credit");
        event.setAmount(new BigDecimal("100.00"));
        event.setTimestamp(System.currentTimeMillis());
        // Missing eventId

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testEventValidation_InvalidType_ShouldReturn400() throws Exception {
        Event event = new Event();
        event.setEventId("E-TEST-001");
        event.setAccountId("A10");
        event.setType("invalid_type"); // Invalid type
        event.setAmount(new BigDecimal("100.00"));
        event.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testEventValidation_NegativeAmount_ShouldReturn400() throws Exception {
        Event event = new Event();
        event.setEventId("E-TEST-002");
        event.setAccountId("A10");
        event.setType("credit");
        event.setAmount(new BigDecimal("-100.00")); // Negative amount
        event.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testEventValidation_ZeroAmount_ShouldReturn400() throws Exception {
        Event event = new Event();
        event.setEventId("E-TEST-003");
        event.setAccountId("A10");
        event.setType("credit");
        event.setAmount(BigDecimal.ZERO); // Zero amount
        event.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testEventIdempotency_DuplicateEventId_ShouldReturn409() throws Exception {
        Event event = new Event();
        event.setEventId("E-TEST-DUP-001");
        event.setAccountId("A10");
        event.setType("credit");
        event.setAmount(new BigDecimal("100.00"));
        event.setTimestamp(System.currentTimeMillis());

        // First submission should succeed
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        // Duplicate submission should fail
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("duplicate eventId"));
    }

    @Test
    public void testEventValidation_ValidEvent_ShouldReturn202() throws Exception {
        Event event = new Event();
        event.setEventId("E-TEST-VALID-001");
        event.setAccountId("A10");
        event.setType("credit");
        event.setAmount(new BigDecimal("100.00"));
        event.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"))
                .andExpect(jsonPath("$.eventId").value("E-TEST-VALID-001"));
    }
}

