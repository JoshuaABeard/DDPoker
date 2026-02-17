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

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Path;
import java.security.KeyPair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JwtTokenProviderTest {

    @TempDir
    Path tempDir;

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        // Generate test keys
        KeyPair keyPair = JwtKeyManager.generateKeyPair();
        Path privateKeyPath = tempDir.resolve("jwt-private.pem");
        Path publicKeyPath = tempDir.resolve("jwt-public.pem");

        JwtKeyManager.savePrivateKey(keyPair.getPrivate(), privateKeyPath);
        JwtKeyManager.savePublicKey(keyPair.getPublic(), publicKeyPath);

        // Create provider in issuing mode (has both keys)
        provider = new JwtTokenProvider(privateKeyPath, publicKeyPath, 3600000L, 604800000L);
    }

    @Test
    void testGenerateTokenWithoutRememberMe() {
        String token = provider.generateToken("testuser", 123L, false);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void testGenerateTokenWithRememberMe() {
        String token = provider.generateToken("testuser", 123L, true);

        assertThat(token).isNotNull();
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void testGetUsernameFromToken() {
        String token = provider.generateToken("testuser", 123L, false);

        String username = provider.getUsernameFromToken(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void testGetProfileIdFromToken() {
        String token = provider.generateToken("testuser", 123L, false);

        Long profileId = provider.getProfileIdFromToken(token);
        assertThat(profileId).isEqualTo(123L);
    }

    @Test
    void testInvalidTokenReturnsFalse() {
        assertThat(provider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    void testValidationOnlyMode() throws Exception {
        // Create validation-only provider (public key only)
        Path publicKeyPath = tempDir.resolve("jwt-public.pem");
        JwtTokenProvider validationProvider = new JwtTokenProvider(null, publicKeyPath, 3600000L, 604800000L);

        // Generate token with full provider
        String token = provider.generateToken("testuser", 123L, false);

        // Validation-only provider can validate but not generate
        assertThat(validationProvider.validateToken(token)).isTrue();
        assertThat(validationProvider.getUsernameFromToken(token)).isEqualTo("testuser");
        assertThat(validationProvider.getProfileIdFromToken(token)).isEqualTo(123L);

        assertThatThrownBy(() -> validationProvider.generateToken("user", 1L, false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("validation-only mode");
    }

    @Test
    void testExpiredTokenInvalidates() throws Exception {
        // Create provider with very short expiration (1ms)
        KeyPair keyPair = JwtKeyManager.generateKeyPair();
        Path privateKeyPath = tempDir.resolve("jwt-private-short.pem");
        Path publicKeyPath = tempDir.resolve("jwt-public-short.pem");

        JwtKeyManager.savePrivateKey(keyPair.getPrivate(), privateKeyPath);
        JwtKeyManager.savePublicKey(keyPair.getPublic(), publicKeyPath);

        JwtTokenProvider shortProvider = new JwtTokenProvider(privateKeyPath, publicKeyPath, 1L, 1L);

        String token = shortProvider.generateToken("testuser", 123L, false);

        // Wait for token to expire
        Thread.sleep(10);

        assertThat(shortProvider.validateToken(token)).isFalse();
    }
}
