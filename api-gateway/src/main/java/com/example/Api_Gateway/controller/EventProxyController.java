package com.example.Api_Gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;

/**
 * Proxy controller for Event Service
 */
@RestController
@RequestMapping("/events")
public class EventProxyController {

    private static final Logger log = LoggerFactory.getLogger(EventProxyController.class);

    @Value("${services.event-service}")
    private String eventServiceUrl;

    private final RestTemplate restTemplate;

    public EventProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping
    public ResponseEntity<?> submitEvent(@RequestBody String body, HttpServletRequest request) {
        String targetUrl = eventServiceUrl + "/events";

        // Forward headers
        HttpHeaders headers = new HttpHeaders();
        copyHeaders(request, headers);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            log.info("Forwarding POST /events to {} with body: {}", targetUrl, body);
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            log.info("Event service responded with status: {}", response.getStatusCode());
            return response;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Event service returned 4xx error - pass it through to client
            log.error("Event service returned error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // Event service returned 5xx error
            log.error("Event service internal error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            // Connection error or other issue
            log.error("Error connecting to event service", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"Cannot connect to event service: " + e.getMessage() + "\"}");
        }
    }

    private void copyHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Skip host header
            if (!"host".equalsIgnoreCase(headerName)) {
                headers.add(headerName, request.getHeader(headerName));
            }
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
    }
}

