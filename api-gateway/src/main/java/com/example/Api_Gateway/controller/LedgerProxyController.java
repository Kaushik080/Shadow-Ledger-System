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
 * Proxy controller for Shadow Ledger Service
 */
@RestController
@RequestMapping("/accounts")
public class LedgerProxyController {

    private static final Logger log = LoggerFactory.getLogger(LedgerProxyController.class);

    @Value("${services.ledger-service}")
    private String ledgerServiceUrl;

    private final RestTemplate restTemplate;

    public LedgerProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/{accountId}/shadow-balance")
    public ResponseEntity<?> getShadowBalance(@PathVariable String accountId, HttpServletRequest request) {
        String targetUrl = ledgerServiceUrl + "/accounts/" + accountId + "/shadow-balance";

        // Forward headers
        HttpHeaders headers = new HttpHeaders();
        copyHeaders(request, headers);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("Forwarding GET /accounts/{}/shadow-balance to {}", accountId, targetUrl);
            return restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            log.error("Error forwarding request to ledger service", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"Service unavailable\"}");
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
    }
}

