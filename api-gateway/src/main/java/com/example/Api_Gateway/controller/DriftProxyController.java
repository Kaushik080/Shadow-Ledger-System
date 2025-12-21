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
 * Proxy controller for Drift and Correction Service
 */
@RestController
public class DriftProxyController {

    private static final Logger log = LoggerFactory.getLogger(DriftProxyController.class);

    @Value("${services.drift-service}")
    private String driftServiceUrl;

    private final RestTemplate restTemplate;

    public DriftProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/drift-check")
    public ResponseEntity<?> checkDrift(@RequestBody String body, HttpServletRequest request) {
        String targetUrl = driftServiceUrl + "/drift-check";

        log.info("Received POST /drift-check request");
        log.info("Target URL: {}", targetUrl);
        log.info("Request body: {}", body);
        log.debug("Body length: {} characters", body != null ? body.length() : 0);

        // Forward headers
        HttpHeaders headers = new HttpHeaders();
        copyHeaders(request, headers);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            log.info("Forwarding POST /drift-check to {}", targetUrl);
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            log.info("Drift service responded with status: {}", response.getStatusCode());
            return response;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot connect to drift service at {}: {}", targetUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"Drift service is unavailable. Please ensure it's running on " + driftServiceUrl + "\",\"details\":\"" + e.getMessage() + "\"}");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Client error from drift service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("Server error from drift service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error forwarding request to drift service", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"Failed to forward request to drift service\",\"details\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/correct/{accountId}")
    public ResponseEntity<?> applyCorrection(@PathVariable String accountId,
                                            @RequestBody String body,
                                            HttpServletRequest request) {
        String targetUrl = driftServiceUrl + "/correct/" + accountId;

        // Forward headers
        HttpHeaders headers = new HttpHeaders();
        copyHeaders(request, headers);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            log.info("Forwarding POST /correct/{} to {}", accountId, targetUrl);
            log.debug("Request body: {}", body);
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            log.info("Drift service responded with status: {}", response.getStatusCode());
            return response;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot connect to drift service at {}: {}", targetUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"Drift service is unavailable. Please ensure it's running on " + driftServiceUrl + "\",\"details\":\"" + e.getMessage() + "\"}");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Client error from drift service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("Server error from drift service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error forwarding request to drift service", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"Failed to forward request to drift service\",\"details\":\"" + e.getMessage() + "\"}");
        }
    }

    private void copyHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!"host".equalsIgnoreCase(headerName)) {
                headers.add(headerName, request.getHeader(headerName));
            }
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
    }
}

