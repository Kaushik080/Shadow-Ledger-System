package com.example.Api_Gateway.service;

import com.example.Api_Gateway.entity.UserAccount;
import com.example.Api_Gateway.repository.UserAccountRepository;
import com.example.Api_Gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository repository;
    private final JwtUtil jwtUtil;

    public AuthService(UserAccountRepository repository, JwtUtil jwtUtil) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Issue JWT token - supports login with userId or username
     * Fetches user role from database for RBAC
     */
    public String issueToken(String userIdOrUsername, String password) {
        logger.debug("Login attempt for: {}", userIdOrUsername);

        // Try to find by userId first, fallback to username - QUERIES DATABASE
        UserAccount user = repository.findByUserId(userIdOrUsername)
                .orElseGet(() -> repository.findByUsername(userIdOrUsername)
                        .orElseThrow(() -> {
                            logger.warn("Login failed - user not found: {}", userIdOrUsername);
                            return new RuntimeException("invalid_credentials");
                        }));

        logger.debug("User found in database: userId={}, username={}, role={}",
                    user.getUserId(), user.getUsername(), user.getRole());

        // Validate password - plain text comparison (NO HASHING)
        if (!password.equals(user.getPasswordHash())) {
            logger.warn("Login failed - invalid password for user: {}", userIdOrUsername);
            throw new RuntimeException("invalid_credentials");
        }

        // Generate JWT token with role fetched from database
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole());
        logger.info("✅ Login successful - userId: {}, username: {}, role: {} (role fetched from DB for RBAC)",
                   user.getUserId(), user.getUsername(), user.getRole());

        return token;
    }

    /**
     * Get user information from database (including role)
     */
    public UserInfo getUserInfo(String userIdOrUsername) {
        UserAccount user = repository.findByUserId(userIdOrUsername)
                .orElseGet(() -> repository.findByUsername(userIdOrUsername)
                        .orElseThrow(() -> new RuntimeException("user_not_found")));

        logger.debug("Fetched user info from DB: userId={}, role={}", user.getUserId(), user.getRole());
        return new UserInfo(user.getUserId(), user.getUsername(), user.getRole());
    }

    /**
     * User information DTO
     */
    public record UserInfo(String userId, String username, String role) {}

    /**
     * Register new user with userId, username, password and role
     */
    public UserAccount signup(String userId, String username, String password, String role) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(username) ||
            !StringUtils.hasText(password) || !StringUtils.hasText(role)) {
            throw new IllegalArgumentException("invalid_input");
        }

        // Validate role
        if (!role.equals("user") && !role.equals("auditor") && !role.equals("admin")) {
            throw new IllegalArgumentException("invalid_role");
        }

        // Check if userId or username already exists
        if (repository.findByUserId(userId).isPresent()) {
            throw new DataIntegrityViolationException("userId_taken");
        }

        if (repository.findByUsername(username).isPresent()) {
            throw new DataIntegrityViolationException("username_taken");
        }

        // Store password as plain text (NO HASHING - NOT RECOMMENDED FOR PRODUCTION)
        UserAccount user = new UserAccount(null, userId, username, password, role);
        return repository.save(user);
    }
}
