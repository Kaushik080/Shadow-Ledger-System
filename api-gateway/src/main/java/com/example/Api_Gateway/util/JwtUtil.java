package com.example.Api_Gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey key;

    public JwtUtil(@Value("${security.jwt.secret:change-me-secret-key-change}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parse JWT token and extract user info and role
     */
    public JwtPayload parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        String subject = claims.getSubject();
        String role = claims.get("role", String.class);

        logger.debug("JWT parsed - userId: {}, role: {} (will be used for RBAC)", subject, role);
        return new JwtPayload(subject, role);
    }

    /**
     * Generate JWT token with userId and role embedded for RBAC
     */
    public String generateToken(String subject, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600); // 1 hour expiry

        String token = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of("role", role))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        logger.debug("JWT generated - userId: {}, role: {} embedded for RBAC, expires in 1 hour", subject, role);
        return token;
    }

    @Getter
    public static class JwtPayload {
        private final String subject;
        private final String role;

        public JwtPayload(String subject, String role) {
            this.subject = subject;
            this.role = role;
        }
    }
}
