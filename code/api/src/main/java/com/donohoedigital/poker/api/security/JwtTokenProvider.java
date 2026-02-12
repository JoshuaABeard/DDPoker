/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.poker.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token generation and validation.
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours default
    private long jwtExpiration;

    @Value("${jwt.remember-me-expiration:2592000000}") // 30 days default
    private long jwtRememberMeExpiration;

    @PostConstruct
    public void validateConfig() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret must be configured. Set JWT_SECRET environment variable.");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters (256 bits) for HMAC-SHA256 security.");
        }
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
