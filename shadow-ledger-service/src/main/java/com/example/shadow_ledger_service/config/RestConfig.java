package com.example.shadow_ledger_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * REST configuration to ensure compatibility with API Gateway RestTemplate
 */
@Configuration
public class RestConfig {

    /**
     * Configure RestClient bean for internal communication if needed
     * RestClient is the modern replacement for RestTemplate
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}

