package com.example.Api_Gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Trace ID handling.
 * Note: The actual trace ID filter is implemented in TraceIdFilter.java
 * as a servlet filter, not a reactive Gateway filter.
 */
@Configuration
public class TraceIdFilterConfig {
    // TraceId handling is done via the servlet filter TraceIdFilter.java
    // This configuration class is kept for future trace-related configurations
}
