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
package com.donohoedigital.games.poker.model;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.Test;

/**
 * Tests for PasswordResetToken.
 */
public class PasswordResetTokenTest {

    @Test
    public void testDefaultConstructor() {
        PasswordResetToken token = new PasswordResetToken();

        assertNotNull(token);
        assertFalse(token.isUsed());
        assertNull(token.getToken());
        assertNull(token.getProfileId());
        assertNull(token.getCreateDate());
        assertNull(token.getExpiryDate());
    }

    @Test
    public void testConstructorWithProfileAndExpiry() {
        Long profileId = 123L;
        long expiryMs = PasswordResetToken.DEFAULT_EXPIRY_MS;

        PasswordResetToken token = new PasswordResetToken(profileId, expiryMs);

        assertNotNull(token.getToken());
        assertEquals(profileId, token.getProfileId());
        assertNotNull(token.getCreateDate());
        assertNotNull(token.getExpiryDate());
        assertFalse(token.isUsed());
    }

    @Test
    public void testTokenIsUUIDFormat() {
        PasswordResetToken token = new PasswordResetToken(1L, 3600000L);

        String tokenStr = token.getToken();
        assertNotNull(tokenStr);
        // UUID format: 8-4-4-4-12 characters
        assertTrue(tokenStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    public void testExpiryDateCalculation() {
        long expiryMs = 1000L; // 1 second
        PasswordResetToken token = new PasswordResetToken(1L, expiryMs);

        Instant createDate = token.getCreateDate();
        Instant expiryDate = token.getExpiryDate();

        assertTrue(expiryDate.isAfter(createDate));
        assertEquals(expiryMs, expiryDate.toEpochMilli() - createDate.toEpochMilli());
    }

    @Test
    public void testGettersAndSetters() {
        PasswordResetToken token = new PasswordResetToken();

        Long id = 100L;
        token.setId(id);
        assertEquals(id, token.getId());

        String tokenStr = "test-token-123";
        token.setToken(tokenStr);
        assertEquals(tokenStr, token.getToken());

        Long profileId = 456L;
        token.setProfileId(profileId);
        assertEquals(profileId, token.getProfileId());

        Instant createDate = Instant.now();
        token.setCreateDate(createDate);
        assertEquals(createDate, token.getCreateDate());

        Instant expiryDate = Instant.now().plusSeconds(3600);
        token.setExpiryDate(expiryDate);
        assertEquals(expiryDate, token.getExpiryDate());

        token.setUsed(true);
        assertTrue(token.isUsed());
    }

    @Test
    public void testIsValidForUnusedNonExpiredToken() {
        PasswordResetToken token = new PasswordResetToken(1L, 3600000L); // 1 hour from now

        assertTrue(token.isValid());
    }

    @Test
    public void testIsValidForUsedToken() {
        PasswordResetToken token = new PasswordResetToken(1L, 3600000L);
        token.setUsed(true);

        assertFalse(token.isValid());
    }

    @Test
    public void testIsValidForExpiredToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsed(false);
        token.setCreateDate(Instant.now().minusSeconds(7200));
        token.setExpiryDate(Instant.now().minusSeconds(3600)); // Expired 1 hour ago

        assertFalse(token.isValid());
    }

    @Test
    public void testMarkAsUsed() {
        PasswordResetToken token = new PasswordResetToken(1L, 3600000L);

        assertTrue(token.isValid());

        token.markAsUsed();

        assertTrue(token.isUsed());
        assertFalse(token.isValid());
    }

    @Test
    public void testDefaultExpiryConstant() {
        assertEquals(3600000L, PasswordResetToken.DEFAULT_EXPIRY_MS);
    }
}
