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
package com.donohoedigital.games.poker.gameserver.auth;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * JWT token provider using RS256 asymmetric keys.
 *
 * <p>
 * Supports two operating modes:
 * <ul>
 * <li><b>Issuing mode</b>: Has private key. Can generate and validate
 * tokens.</li>
 * <li><b>Validation-only mode</b>: Has public key only. Can validate tokens but
 * not generate them.</li>
 * </ul>
 */
public class JwtTokenProvider {
    private final PrivateKey privateKey; // Null in validation-only mode
    private final PublicKey publicKey; // Always present
    private final long expiration;
    private final long rememberMeExpiration;

    /**
     * Create a JWT token provider.
     *
     * @param privateKeyPath
     *            path to private key PEM file (null for validation-only mode)
     * @param publicKeyPath
     *            path to public key PEM file (required)
     * @param expiration
     *            token expiration in milliseconds (regular)
     * @param rememberMeExpiration
     *            token expiration in milliseconds (remember me)
     */
    public JwtTokenProvider(Path privateKeyPath, Path publicKeyPath, long expiration, long rememberMeExpiration) {
        try {
            this.privateKey = (privateKeyPath != null) ? JwtKeyManager.loadPrivateKey(privateKeyPath) : null;
            this.publicKey = JwtKeyManager.loadPublicKey(publicKeyPath);
            this.expiration = expiration;
            this.rememberMeExpiration = rememberMeExpiration;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT keys", e);
        }
    }

    /**
     * Generate a JWT token (issuing mode only).
     *
     * @param username
     *            the username (subject)
     * @param profileId
     *            the profile ID
     * @param rememberMe
     *            whether to use extended expiration
     * @return the JWT token
     * @throws IllegalStateException
     *             if in validation-only mode
     */
    public String generateToken(String username, Long profileId, boolean rememberMe) {
        if (privateKey == null) {
            throw new IllegalStateException("Cannot generate tokens in validation-only mode");
        }

        long expirationTime = rememberMe ? rememberMeExpiration : expiration;

        return Jwts.builder().subject(username).claim("profileId", profileId).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime)).signWith(privateKey).compact();
    }

    /**
     * Validate a JWT token.
     *
     * @param token
     *            the token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extract username from token.
     *
     * @param token
     *            the JWT token
     * @return the username
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract profile ID from token.
     *
     * @param token
     *            the JWT token
     * @return the profile ID
     */
    public Long getProfileIdFromToken(String token) {
        return getClaims(token).get("profileId", Long.class);
    }

    /**
     * Parse and validate claims from token.
     */
    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
    }
}
