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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.comms.DMTypedHashMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Tests for OnlineProfile license/activation stub methods. Verifies that
 * removed license functionality is properly stubbed for backward compatibility.
 */
public class OnlineProfileStubTest {

    private OnlineProfile profile;

    @Before
    public void setUp() {
        profile = new OnlineProfile("TestPlayer");
        profile.setEmail("test@example.com");
        profile.setPassword("testpass");
        profile.setUuid("test-uuid-1234");
    }

    // ========== License Key Stub Tests ==========

    @Test
    public void should_ReturnNull_When_GetLicenseKeyCalled() {
        // License keys removed in open source version
        String licenseKey = profile.getLicenseKey();

        assertNull("License key should be null in open source version", licenseKey);
    }

    @Test
    public void should_NotThrowException_When_SetLicenseKeyCalledWithValue() {
        // Should accept any value without error (no-op)
        profile.setLicenseKey("fake-license-key");

        // Still returns null after setting
        assertNull("License key should remain null", profile.getLicenseKey());
    }

    @Test
    public void should_NotThrowException_When_SetLicenseKeyCalledWithNull() {
        profile.setLicenseKey(null);

        assertNull("License key should be null", profile.getLicenseKey());
    }

    @Test
    public void should_NotPersistLicenseKey_When_SetMultipleTimes() {
        profile.setLicenseKey("key1");
        profile.setLicenseKey("key2");
        profile.setLicenseKey("key3");

        // Always returns null, nothing is stored
        assertNull("License key should not persist", profile.getLicenseKey());
    }

    // ========== Activation Stub Tests ==========

    @Test
    public void should_AlwaysReturnTrue_When_IsActivatedCalled() {
        // Always activated in open source version
        boolean activated = profile.isActivated();

        assertTrue("Profile should always be activated", activated);
    }

    @Test
    public void should_RemainActivated_When_SetActivatedCalledWithFalse() {
        profile.setActivated(false);

        // Still returns true (no-op)
        assertTrue("Profile should remain activated", profile.isActivated());
    }

    @Test
    public void should_RemainActivated_When_SetActivatedCalledWithTrue() {
        profile.setActivated(true);

        // Returns true as expected
        assertTrue("Profile should be activated", profile.isActivated());
    }

    @Test
    public void should_AlwaysBeActivated_When_NewProfileCreated() {
        OnlineProfile newProfile = new OnlineProfile("NewPlayer");

        // Activated from creation
        assertTrue("New profile should be activated", newProfile.isActivated());
    }

    // ========== Integration Tests ==========

    @Test
    public void should_NotAffectOtherFields_When_LicenseStubsCalled() {
        // Set real fields
        profile.setName("TestPlayer");
        profile.setEmail("test@example.com");
        profile.setPassword("secret");
        profile.setUuid("uuid-1234");

        // Call stub methods
        profile.setLicenseKey("fake-key");
        profile.setActivated(false);

        // Real fields should be unaffected
        assertEquals("Name should be preserved", "TestPlayer", profile.getName());
        assertEquals("Email should be preserved", "test@example.com", profile.getEmail());
        assertEquals("Password should be preserved", "secret", profile.getPassword());
        assertEquals("UUID should be preserved", "uuid-1234", profile.getUuid());
    }

    @Test
    public void should_SerializeWithoutLicenseKey_When_DataMarshalled() {
        profile.setLicenseKey("should-not-appear");
        profile.setActivated(false);

        DMTypedHashMap data = profile.getData();

        // License key should not be in serialized data
        assertFalse("Data should not contain 'license' key", data.containsKey("license"));
        assertFalse("Data should not contain 'licenseKey' key", data.containsKey("licenseKey"));
        assertFalse("Data should not contain 'activated' key", data.containsKey("activated"));
    }

    @Test
    public void should_WorkWithMultipleProfiles_When_StubMethodsCalled() {
        OnlineProfile profile1 = new OnlineProfile("Player1");
        OnlineProfile profile2 = new OnlineProfile("Player2");
        OnlineProfile profile3 = new OnlineProfile("Player3");

        // All should be activated
        assertTrue("Profile 1 should be activated", profile1.isActivated());
        assertTrue("Profile 2 should be activated", profile2.isActivated());
        assertTrue("Profile 3 should be activated", profile3.isActivated());

        // All return null for license key
        assertNull("Profile 1 license key should be null", profile1.getLicenseKey());
        assertNull("Profile 2 license key should be null", profile2.getLicenseKey());
        assertNull("Profile 3 license key should be null", profile3.getLicenseKey());
    }

    // ========== Backward Compatibility Tests ==========

    @Test
    public void should_MaintainProfileFunctionality_When_LicenseRemoved() {
        // Core profile functionality should work independently of license stubs
        profile.setName("UpdatedName");
        profile.setEmail("updated@example.com");
        profile.setPassword("newpass");
        profile.setRetired(true);
        profile.setCreateDate(new Date());
        profile.setModifyDate(new Date());

        // All core functionality works
        assertEquals("Name should be updated", "UpdatedName", profile.getName());
        assertEquals("Email should be updated", "updated@example.com", profile.getEmail());
        assertEquals("Password should be updated", "newpass", profile.getPassword());
        assertTrue("Profile should be retired", profile.isRetired());
        assertNotNull("Create date should be set", profile.getCreateDate());
        assertNotNull("Modify date should be set", profile.getModifyDate());

        // License stubs don't interfere
        assertTrue("Profile should still be activated", profile.isActivated());
        assertNull("License key should still be null", profile.getLicenseKey());
    }

    @Test
    public void should_BeDeprecated_When_LicenseMethodsCalled() {
        // These methods should be marked @Deprecated
        // This test documents the deprecation for future developers

        @SuppressWarnings("deprecation")
        String key = profile.getLicenseKey();
        assertNull("Deprecated method should still work", key);

        @SuppressWarnings("deprecation")
        boolean activated = profile.isActivated();
        assertTrue("Deprecated method should still work", activated);
    }

    // ========== Null Safety Tests ==========

    @Test
    public void should_HandleNullProfile_When_CreatedWithNullName() {
        // This tests general robustness, not just license stubs
        OnlineProfile nullNameProfile = new OnlineProfile((String) null);

        // Stub methods should still work
        assertTrue("Null-name profile should be activated", nullNameProfile.isActivated());
        assertNull("Null-name profile license key should be null", nullNameProfile.getLicenseKey());
    }

    @Test
    public void should_HandleEmptyProfile_When_NoFieldsSet() {
        OnlineProfile emptyProfile = new OnlineProfile();

        // Stub methods work even on empty profile
        assertTrue("Empty profile should be activated", emptyProfile.isActivated());
        assertNull("Empty profile license key should be null", emptyProfile.getLicenseKey());

        // Setting stubs should not cause errors
        emptyProfile.setLicenseKey("test");
        emptyProfile.setActivated(false);

        // Still works after setting stubs
        assertTrue("Empty profile should remain activated", emptyProfile.isActivated());
        assertNull("Empty profile license key should remain null", emptyProfile.getLicenseKey());
    }
}
