package com.example.Api_Gateway.controller;

import com.example.Api_Gateway.entity.UserAccount;
import com.example.Api_Gateway.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping(path = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public record TokenRequest(@NotBlank String userId, @NotBlank String password) {}

    public record SignupRequest(
            @NotBlank String userId,
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String role) {}

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Login endpoint (HTTP Basic Auth) - accepts credentials in Authorization header
     * Header format: Authorization: Basic base64(userId:password)
     * User role is fetched from database and embedded in JWT for RBAC
     */
//    @PostMapping(path = "/login")
//    public ResponseEntity<Map<String, Object>> login(@RequestHeader("Authorization") String authHeader) {
//        logger.debug("Login request received with Authorization header");
//
//        try {
//            // Parse Basic Auth header
//            if (!authHeader.startsWith("Basic ")) {
//                logger.warn("Invalid Authorization header format");
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(Map.of("error", "invalid_auth_header"));
//            }
//
//            String base64Credentials = authHeader.substring(6);
//            String credentials = new String(java.util.Base64.getDecoder().decode(base64Credentials));
//            String[] parts = credentials.split(":", 2);
//
//            if (parts.length != 2) {
//                logger.warn("Invalid credentials format in Authorization header");
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(Map.of("error", "invalid_credentials_format"));
//            }
//
//            String userId = parts[0];
//            String password = parts[1];
//
//            logger.debug("Parsed credentials for userId: {}", userId);
//
//            // Issue token - this fetches user role from database
//            String token = authService.issueToken(userId, password);
//
//            // Get user info to include in response
//            var userInfo = authService.getUserInfo(userId);
//
//            logger.info("Login successful via Basic Auth for userId: {}", userId);
//
//            return ResponseEntity.ok(Map.of(
//                    "token", token,
//                    "userId", userInfo.userId(),
//                    "username", userInfo.username(),
//                    "role", userInfo.role()
//            ));
//        } catch (IllegalArgumentException ex) {
//            logger.warn("Login failed - invalid base64 encoding");
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error", "invalid_auth_encoding"));
//        } catch (RuntimeException ex) {
//            logger.warn("Login failed - {}", ex.getMessage());
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(Map.of("error", "invalid_credentials"));
//        }
//    }

    /**
     * Token endpoint (JSON Body Auth) - accepts userId and password in request body
     * Alternative to /login endpoint for clients that prefer JSON body over headers
     * User role is fetched from database and embedded in JWT for RBAC
     */
    @PostMapping(path = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> token(@RequestBody TokenRequest req) {
        logger.debug("Token request received for userId: {}", req.userId());
        try {
            // Issue token - this fetches user role from database
            String token = authService.issueToken(req.userId(), req.password());

            // Get user info to include in response
            var userInfo = authService.getUserInfo(req.userId());

            return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", userInfo.userId(),
                "username", userInfo.username(),
                "role", userInfo.role()
            ));
        } catch (RuntimeException ex) {
            logger.warn("Login failed for userId: {} - {}", req.userId(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_credentials"));
        }
    }

    /**
     * Signup endpoint - register new user with userId, username, password and role
     */
    @PostMapping(path = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> signup(@RequestBody SignupRequest req) {
        logger.info("Signup request received for userId: {}, username: {}, role: {}",
                    req.userId(), req.username(), req.role());
        try {
            UserAccount user = authService.signup(req.userId(), req.username(), req.password(), req.role());

            logger.info("User registered successfully: {}", user.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User registered successfully",
                    "userId", user.getUserId(),
                    "username", user.getUsername(),
                    "role", user.getRole()
            ));
        } catch (IllegalArgumentException ex) {
            logger.warn("Signup failed with bad request: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Signup failed with error: {}", ex.getMessage(), ex);
            String error = ex.getMessage().contains("userId_taken") ? "userId_taken" :
                          ex.getMessage().contains("username_taken") ? "username_taken" : "signup_failed";
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", error));
        }
    }

    /**
     * Logout endpoint - JWT is stateless, so this just returns success
     * In production, implement token blacklisting with Redis
     */
    @PostMapping(path = "/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
