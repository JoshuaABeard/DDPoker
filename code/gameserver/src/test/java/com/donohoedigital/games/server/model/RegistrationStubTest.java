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
package com.donohoedigital.games.server.model;

import com.donohoedigital.comms.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Registration stub class.
 * Verifies backward compatibility after license tracking removal.
 */
class RegistrationStubTest {

    private Registration registration;

    @BeforeEach
    void setUp() {
        registration = new Registration();
    }

    // ========== Type Enum Tests ==========

    @Test
    void should_HaveAllTypeValues_When_EnumAccessed() {
        // All enum values should exist for DAO compatibility
        assertThat(Registration.Type.UNKNOWN).isNotNull();
        assertThat(Registration.Type.RETAIL).isNotNull();
        assertThat(Registration.Type.ONLINE).isNotNull();
        assertThat(Registration.Type.DEMO).isNotNull();
        assertThat(Registration.Type.REGISTRATION).isNotNull();
        assertThat(Registration.Type.PATCH).isNotNull();
        assertThat(Registration.Type.ACTIVATION).isNotNull();
    }

    @Test
    void should_SetAndGetType_When_TypeSet() {
        registration.setType(Registration.Type.REGISTRATION);
        assertThat(registration.getType()).isEqualTo(Registration.Type.REGISTRATION);

        registration.setType(Registration.Type.ACTIVATION);
        assertThat(registration.getType()).isEqualTo(Registration.Type.ACTIVATION);
    }

    // ========== Basic Field Tests ==========

    @Test
    void should_SetAndGetName_When_NameSet() {
        registration.setName("John Doe");
        assertThat(registration.getName()).isEqualTo("John Doe");
    }

    @Test
    void should_SetAndGetEmail_When_EmailSet() {
        registration.setEmail("john@example.com");
        assertThat(registration.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void should_SetAndGetAddress_When_AddressSet() {
        registration.setAddress("123 Main St");
        assertThat(registration.getAddress()).isEqualTo("123 Main St");
    }

    @Test
    void should_SetAndGetCity_When_CitySet() {
        registration.setCity("Springfield");
        assertThat(registration.getCity()).isEqualTo("Springfield");
    }

    @Test
    void should_SetAndGetState_When_StateSet() {
        registration.setState("CA");
        assertThat(registration.getState()).isEqualTo("CA");
    }

    @Test
    void should_SetAndGetCountry_When_CountrySet() {
        registration.setCountry("USA");
        assertThat(registration.getCountry()).isEqualTo("USA");
    }

    @Test
    void should_SetAndGetPostal_When_PostalSet() {
        registration.setPostal("12345");
        assertThat(registration.getPostal()).isEqualTo("12345");
    }

    // ========== OS Detection Tests ==========

    @Test
    void should_ReturnTrue_When_WindowsOSSet() {
        registration.setOperatingSystem("Windows 10");
        assertThat(registration.isWin()).isTrue();
        assertThat(registration.isLinux()).isFalse();
        assertThat(registration.isMac()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_LinuxOSSet() {
        registration.setOperatingSystem("Linux");
        assertThat(registration.isLinux()).isTrue();
        assertThat(registration.isWin()).isFalse();
        assertThat(registration.isMac()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_MacOSSet() {
        registration.setOperatingSystem("Mac OS X");
        assertThat(registration.isMac()).isTrue();
        assertThat(registration.isWin()).isFalse();
        assertThat(registration.isLinux()).isFalse();
    }

    @Test
    void should_BeCaseInsensitive_When_DetectingOS() {
        registration.setOperatingSystem("WINDOWS");
        assertThat(registration.isWin()).isTrue();

        registration.setOperatingSystem("linux");
        assertThat(registration.isLinux()).isTrue();

        registration.setOperatingSystem("mac");
        assertThat(registration.isMac()).isTrue();
    }

    // ========== License Key Stub Tests ==========

    @Test
    void should_SetAndGetLicenseKey_When_LicenseKeySet() {
        registration.setLicenseKey("fake-license-key");
        assertThat(registration.getLicenseKey()).isEqualTo("fake-license-key");
    }

    @Test
    void should_AcceptNullLicenseKey_When_NullSet() {
        registration.setLicenseKey(null);
        assertThat(registration.getLicenseKey()).isNull();
    }

    // ========== Version Tests ==========

    @Test
    void should_SetVersionString_When_StringVersionSet() {
        registration.setVersion("3.2.1");
        assertThat(registration.getVersion()).isEqualTo("3.2.1");
    }

    @Test
    void should_ConvertVersionObject_When_VersionObjectSet() {
        Version version = new Version("3.2.1");
        registration.setVersion(version);
        assertThat(registration.getVersion()).isEqualTo("3.2.1");
    }

    @Test
    void should_HandleNullVersion_When_NullVersionSet() {
        registration.setVersion((Version) null);
        assertThat(registration.getVersion()).isNull();
    }

    // ========== Date/Time Tests ==========

    @Test
    void should_SetAndGetServerTime_When_DateSet() {
        Date now = new Date();
        registration.setServerTime(now);
        assertThat(registration.getServerTime()).isEqualTo(now);
    }

    @Test
    void should_ReturnMillis_When_ServerTimeMillisAccessed() {
        Date now = new Date();
        registration.setServerTime(now);
        assertThat(registration.getServerTimeMillis()).isEqualTo(now.getTime());
    }

    @Test
    void should_ReturnZero_When_NoServerTimeSet() {
        assertThat(registration.getServerTimeMillis()).isZero();
    }

    // ========== Network Info Tests ==========

    @Test
    void should_SetAndGetIP_When_IPSet() {
        registration.setIp("192.168.1.100");
        assertThat(registration.getIp()).isEqualTo("192.168.1.100");
    }

    @Test
    void should_SetAndGetPort_When_PortSet() {
        registration.setPort(8080);
        assertThat(registration.getPort()).isEqualTo(8080);
    }

    @Test
    void should_SetAndGetHostName_When_HostNameSet() {
        registration.setHostName("example.com");
        assertThat(registration.getHostName()).isEqualTo("example.com");
    }

    @Test
    void should_SetAndGetHostNameModified_When_ModifiedHostNameSet() {
        registration.setHostNameModified("modified.example.com");
        assertThat(registration.getHostNameModified()).isEqualTo("modified.example.com");
    }

    // ========== Type Check Tests ==========

    @Test
    void should_ReturnTrue_When_TypeIsRegistration() {
        registration.setType(Registration.Type.REGISTRATION);
        assertThat(registration.isRegistration()).isTrue();
        assertThat(registration.isActivation()).isFalse();
        assertThat(registration.isPatch()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_TypeIsActivation() {
        registration.setType(Registration.Type.ACTIVATION);
        assertThat(registration.isActivation()).isTrue();
        assertThat(registration.isRegistration()).isFalse();
        assertThat(registration.isPatch()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_TypeIsPatch() {
        registration.setType(Registration.Type.PATCH);
        assertThat(registration.isPatch()).isTrue();
        assertThat(registration.isRegistration()).isFalse();
        assertThat(registration.isActivation()).isFalse();
    }

    // ========== Ban and Duplicate Tests ==========

    @Test
    void should_SetAndCheckBanAttempt_When_BanFlagSet() {
        registration.setBanAttempt(true);
        assertThat(registration.isBanAttempt()).isTrue();

        registration.setBanAttempt(false);
        assertThat(registration.isBanAttempt()).isFalse();
    }

    @Test
    void should_SetAndCheckDuplicate_When_DuplicateFlagSet() {
        registration.setDuplicate(true);
        assertThat(registration.isDuplicate()).isTrue();

        registration.setDuplicate(false);
        assertThat(registration.isDuplicate()).isFalse();
    }

    // ========== ID Tests (BaseModel compatibility) ==========

    @Test
    void should_SetAndGetId_When_IdSet() {
        registration.setId(42L);
        assertThat(registration.getId()).isEqualTo(42L);
    }

    @Test
    void should_AcceptNullId_When_NullIdSet() {
        registration.setId(null);
        assertThat(registration.getId()).isNull();
    }

    // ========== Utility Method Tests ==========

    @Test
    void should_ReturnHostName_When_GenerifyHostNameCalled() {
        String result = Registration.generifyHostName("example.com");
        assertThat(result).isEqualTo("example.com");
    }

    @Test
    void should_HandleNullHostName_When_NullPassedToGenerify() {
        String result = Registration.generifyHostName(null);
        assertThat(result).isNull();
    }

    // ========== Integration Tests ==========

    @Test
    void should_StoreAllFields_When_CompleteRegistrationCreated() {
        // Set all fields
        registration.setId(1L);
        registration.setType(Registration.Type.REGISTRATION);
        registration.setName("Test User");
        registration.setEmail("test@example.com");
        registration.setAddress("123 Test St");
        registration.setCity("Test City");
        registration.setState("TS");
        registration.setCountry("Testland");
        registration.setPostal("12345");
        registration.setOperatingSystem("Windows 10");
        registration.setJavaVersion("17.0.1");
        registration.setLicenseKey("test-key");
        registration.setVersion("3.2.1");
        registration.setServerTime(new Date());
        registration.setIp("192.168.1.1");
        registration.setPort(8080);
        registration.setHostName("test.local");
        registration.setHostNameModified("modified.test.local");
        registration.setBanAttempt(false);
        registration.setDuplicate(false);

        // Verify all fields
        assertThat(registration.getId()).isEqualTo(1L);
        assertThat(registration.getType()).isEqualTo(Registration.Type.REGISTRATION);
        assertThat(registration.getName()).isEqualTo("Test User");
        assertThat(registration.getEmail()).isEqualTo("test@example.com");
        assertThat(registration.getAddress()).isEqualTo("123 Test St");
        assertThat(registration.getCity()).isEqualTo("Test City");
        assertThat(registration.getState()).isEqualTo("TS");
        assertThat(registration.getCountry()).isEqualTo("Testland");
        assertThat(registration.getPostal()).isEqualTo("12345");
        assertThat(registration.getOperatingSystem()).isEqualTo("Windows 10");
        assertThat(registration.getJavaVersion()).isEqualTo("17.0.1");
        assertThat(registration.getLicenseKey()).isEqualTo("test-key");
        assertThat(registration.getVersion()).isEqualTo("3.2.1");
        assertThat(registration.getServerTime()).isNotNull();
        assertThat(registration.getIp()).isEqualTo("192.168.1.1");
        assertThat(registration.getPort()).isEqualTo(8080);
        assertThat(registration.getHostName()).isEqualTo("test.local");
        assertThat(registration.getHostNameModified()).isEqualTo("modified.test.local");
        assertThat(registration.isBanAttempt()).isFalse();
        assertThat(registration.isDuplicate()).isFalse();
        assertThat(registration.isWin()).isTrue();
    }
}
