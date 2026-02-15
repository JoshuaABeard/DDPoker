/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.poker.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

/**
 * JWT token generation and validation.
 */
@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String JWT_SECRET_FILE = System.getProperty("jwt.secret.file",
            System.getenv("WORK") != null ? System.getenv("WORK") + "/jwt.secret" : "./data/jwt.secret");

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours default
    private long jwtExpiration;

    @Value("${jwt.remember-me-expiration:2592000000}") // 30 days default
    private long jwtRememberMeExpiration;

    @PostConstruct
    public void validateConfig() {
        // If JWT_SECRET env var not set, try to load from file or generate new one
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            jwtSecret = loadOrGenerateSecret();
        }

        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters (256 bits) for HMAC-SHA256 security.");
        }

        logger.info("JWT token provider initialized (secret loaded from {})",
                System.getenv("JWT_SECRET") != null ? "environment variable" : "file: " + JWT_SECRET_FILE);
    }

    private String loadOrGenerateSecret() {
        Path secretPath = Paths.get(JWT_SECRET_FILE);

        // Try to load existing secret
        if (Files.exists(secretPath)) {
            try {
                String secret = Files.readString(secretPath, StandardCharsets.UTF_8).trim();
                if (secret.length() >= 32) {
                    logger.info("Loaded JWT secret from file: {}", JWT_SECRET_FILE);
                    return secret;
                }
            } catch (IOException e) {
                logger.warn("Failed to read JWT secret file, will generate new one", e);
            }
        }

        // Generate new secure secret
        String newSecret = generateSecureSecret();

        // Persist to file for future runs
        try {
            Files.createDirectories(secretPath.getParent());
            Files.writeString(secretPath, newSecret, StandardCharsets.UTF_8);
            logger.info("Generated and saved new JWT secret to: {}", JWT_SECRET_FILE);
        } catch (IOException e) {
            logger.error("Failed to save JWT secret to file: {}", JWT_SECRET_FILE, e);
            throw new IllegalStateException("Failed to persist JWT secret", e);
        }

        return newSecret;
    }

    private String generateSecureSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT token for authenticated user.
     *
     * @param username
     *            the username
     * @param isAdmin
     *            whether user has admin role
     * @param rememberMe
     *            whether to use extended expiration
     * @return JWT token string
     */
    public String generateToken(String username, boolean isAdmin, boolean rememberMe) {
        Date now = new Date();
        long expiration = rememberMe ? jwtRememberMeExpiration : jwtExpiration;
        Date expiryDate = new Date(now.getTime() + expiration);

        JwtBuilder builder = Jwts.builder().subject(username).issuedAt(now).expiration(expiryDate)
                .signWith(getSigningKey());

        if (isAdmin) {
            builder.claim("role", "ADMIN");
        }

        return builder.compact();
    }

    /**
     * Extract username from JWT token.
     *
     * @param token
     *            JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

        return claims.getSubject();
    }

    /**
     * Check if user has admin role from token.
     *
     * @param token
     *            JWT token
     * @return true if admin
     */
    public boolean isAdmin(String token) {
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

        return "ADMIN".equals(claims.get("role"));
    }

    /**
     * Validate JWT token.
     *
     * @param token
     *            JWT token
     * @return true if valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
