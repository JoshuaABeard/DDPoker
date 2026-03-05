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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.comms.DMTypedHashMap;

/**
 * Tests for OnlineProfile.
 */
public class OnlineProfileTest {

    private OnlineProfile profile;

    @BeforeEach
    public void setUp() {
        profile = new OnlineProfile();
    }

    @Test
    public void testDefaultConstructor() {
        OnlineProfile newProfile = new OnlineProfile();

        assertNotNull(newProfile);
        assertNotNull(newProfile.getData());
        assertNull(newProfile.getName());
        assertNull(newProfile.getId());
    }

    @Test
    public void testConstructorWithName() {
        OnlineProfile newProfile = new OnlineProfile("TestPlayer");

        assertNotNull(newProfile);
        assertEquals(newProfile.getName(), "TestPlayer");
        assertNotNull(newProfile.getData());
    }

    @Test
    public void testConstructorWithData() {
        DMTypedHashMap data = new DMTypedHashMap();
        data.setString(OnlineProfile.PROFILE_NAME, "DataPlayer");
        data.setLong(OnlineProfile.PROFILE_ID, 123L);

        OnlineProfile newProfile = new OnlineProfile(data);

        assertEquals(newProfile.getName(), "DataPlayer");
        assertEquals(Long.valueOf(123L), newProfile.getId());
        assertSame(data, newProfile.getData());
    }

    @Test
    public void testGetData() {
        DMTypedHashMap data = profile.getData();

        assertNotNull(data);
        assertSame(data, profile.getData());
    }

    @Test
    public void testSetAndGetId() {
        profile.setId(456L);

        assertEquals(Long.valueOf(456L), profile.getId());
    }

    @Test
    public void testSetAndGetName() {
        profile.setName("Player123");

        assertEquals(profile.getName(), "Player123");
    }

    @Test
    public void testSetAndGetUuid() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        profile.setUuid(uuid);

        assertEquals(uuid, profile.getUuid());
    }

    @Test
    public void testSetAndGetEmail() {
        profile.setEmail("test@example.com");

        assertEquals(profile.getEmail(), "test@example.com");
    }

    @Test
    public void testSetAndGetPasswordHash() {
        String hash = "$2a$10$abcdefghijklmnopqrstuvwxyz";
        profile.setPasswordHash(hash);

        assertEquals(hash, profile.getPasswordHash());
    }

    @Test
    public void testSetAndGetPassword() {
        profile.setPassword("plaintext123");

        assertEquals(profile.getPassword(), "plaintext123");
    }

    @Test
    public void testPasswordAndPasswordHashSeparate() {
        profile.setPassword("plaintext");
        profile.setPasswordHash("hashed");

        assertEquals(profile.getPassword(), "plaintext");
        assertEquals(profile.getPasswordHash(), "hashed");
    }

    @Test
    public void testIsRetiredDefaultFalse() {
        assertFalse(profile.isRetired());
    }

    @Test
    public void testSetRetiredTrue() {
        profile.setRetired(true);

        assertTrue(profile.isRetired());
    }

    @Test
    public void testSetRetiredFalse() {
        profile.setRetired(true);
        profile.setRetired(false);

        assertFalse(profile.isRetired());
    }

    @Test
    public void testSetAndGetCreateDate() {
        Date now = new Date();
        profile.setCreateDate(now);

        assertEquals(now, profile.getCreateDate());
    }

    @Test
    public void testSetAndGetModifyDate() {
        Date now = new Date();
        profile.setModifyDate(now);

        assertEquals(now, profile.getModifyDate());
    }

    @Test
    public void testEqualsSameInstance() {
        assertTrue(profile.equals(profile));
    }

    @Test
    public void testEqualsSameName() {
        profile.setName("Player1");
        OnlineProfile other = new OnlineProfile("Player1");

        assertTrue(profile.equals(other));
        assertTrue(other.equals(profile));
    }

    @Test
    public void testEqualsDifferentName() {
        profile.setName("Player1");
        OnlineProfile other = new OnlineProfile("Player2");

        assertFalse(profile.equals(other));
        assertFalse(other.equals(profile));
    }

    @Test
    public void testEqualsNull() {
        profile.setName("Player1");

        assertFalse(profile.equals(null));
    }

    @Test
    public void testEqualsDifferentType() {
        profile.setName("Player1");

        assertFalse(profile.equals("Player1"));
    }

    @Test
    public void testHashCodeSameName() {
        profile.setName("Player1");
        OnlineProfile other = new OnlineProfile("Player1");

        assertEquals(profile.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCodeDifferentName() {
        profile.setName("Player1");
        OnlineProfile other = new OnlineProfile("Player2");

        assertNotEquals(profile.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCodeNullName() {
        int hashCode1 = profile.hashCode();
        int hashCode2 = profile.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testToString() {
        profile.setName("TestPlayer");
        profile.setEmail("test@example.com");

        String result = profile.toString();

        assertNotNull(result);
        assertTrue(result.startsWith("OnlineProfile: "));
    }

    @Test
    public void testConstants() {
        assertEquals(OnlineProfile.PROFILE_ID, "profileid");
        assertEquals(OnlineProfile.PROFILE_NAME, "profilename");
        assertEquals(OnlineProfile.PROFILE_EMAIL, "profileemail");
        assertEquals(OnlineProfile.PROFILE_PASSWORD, "profilepassword");
        assertEquals(OnlineProfile.PROFILE_PASSWORD_HASH, "profilepasswordhash");
        assertEquals(OnlineProfile.PROFILE_UUID, "profileuuid");
        assertEquals(OnlineProfile.PROFILE_CREATE_DATE, "profilecreatedate");
        assertEquals(OnlineProfile.PROFILE_MODIFY_DATE, "profilemodifydate");
        assertEquals(OnlineProfile.PROFILE_RETIRED, "profileretired");
    }

    @Test
    public void testFullyPopulatedProfile() {
        Date createDate = new Date(System.currentTimeMillis() - 86400000);
        Date modifyDate = new Date();

        profile.setId(999L);
        profile.setName("FullPlayer");
        profile.setUuid("uuid-12345");
        profile.setEmail("full@example.com");
        profile.setPasswordHash("$2a$10$hash");
        profile.setPassword("plaintext");
        profile.setRetired(false);
        profile.setCreateDate(createDate);
        profile.setModifyDate(modifyDate);

        assertEquals(Long.valueOf(999L), profile.getId());
        assertEquals(profile.getName(), "FullPlayer");
        assertEquals(profile.getUuid(), "uuid-12345");
        assertEquals(profile.getEmail(), "full@example.com");
        assertEquals(profile.getPasswordHash(), "$2a$10$hash");
        assertEquals(profile.getPassword(), "plaintext");
        assertFalse(profile.isRetired());
        assertEquals(createDate, profile.getCreateDate());
        assertEquals(modifyDate, profile.getModifyDate());
    }
}
