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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.model.OnlineProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PlayerProfile - profile data, name, email, password, online state,
 * JWT/profileId session fields, copy constructor, and stat initialization.
 * Covers player profile API beyond what PokerPlayerTest already exercises.
 */
class PlayerProfileTest {

    private PlayerProfile profile;

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        profile = new PlayerProfile("TestUser");
    }

    // =================================================================
    // Name Tests
    // =================================================================

    @Test
    void should_ReturnName_When_ProfileCreatedWithName() {
        assertThat(profile.getName()).isEqualTo("TestUser");
    }

    @Test
    void should_UpdateName_When_NameSet() {
        profile.setName("NewName");

        assertThat(profile.getName()).isEqualTo("NewName");
    }

    // =================================================================
    // Email Tests
    // =================================================================

    @Test
    void should_ReturnNullEmail_When_NoEmailSet() {
        assertThat(profile.getEmail()).isNull();
    }

    @Test
    void should_ReturnEmail_When_EmailSet() {
        profile.setEmail("user@example.com");

        assertThat(profile.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void should_UpdateEmail_When_EmailChangedAfterSet() {
        profile.setEmail("first@example.com");
        profile.setEmail("second@example.com");

        assertThat(profile.getEmail()).isEqualTo("second@example.com");
    }

    @Test
    void should_ClearEmail_When_NullEmailSet() {
        profile.setEmail("user@example.com");
        profile.setEmail(null);

        assertThat(profile.getEmail()).isNull();
    }

    // =================================================================
    // isOnline Tests
    // =================================================================

    @Test
    void should_NotBeOnline_When_NoEmailSet() {
        assertThat(profile.isOnline()).isFalse();
    }

    @Test
    void should_BeOnline_When_EmailSet() {
        profile.setEmail("player@example.com");

        assertThat(profile.isOnline()).isTrue();
    }

    @Test
    void should_NotBeOnline_When_EmailCleared() {
        profile.setEmail("player@example.com");
        profile.setEmail(null);

        assertThat(profile.isOnline()).isFalse();
    }

    // =================================================================
    // isActivated Tests (always true in open source version)
    // =================================================================

    @Test
    void should_AlwaysBeActivated_When_CheckedWithoutSetting() {
        // In the open source version, isActivated() always returns true
        assertThat(profile.isActivated()).isTrue();
    }

    @Test
    void should_AlwaysBeActivated_When_SetActivatedFalse() {
        // setActivated is a no-op in open source version; isActivated() always true
        profile.setActivated(false);

        assertThat(profile.isActivated()).isTrue();
    }

    @Test
    void should_AlwaysBeActivated_When_SetActivatedTrue() {
        profile.setActivated(true);

        assertThat(profile.isActivated()).isTrue();
    }

    // =================================================================
    // Password Tests
    // =================================================================

    @Test
    void should_ReturnNullPassword_When_NoPasswordSet() {
        assertThat(profile.getPassword()).isNull();
    }

    @Test
    void should_StoreAndRetrievePassword_When_PasswordSet() {
        profile.setPassword("secretpass");

        assertThat(profile.getPassword()).isEqualTo("secretpass");
    }

    @Test
    void should_MatchPassword_When_CorrectPasswordChecked() {
        profile.setPassword("mypassword");

        assertThat(profile.isMatchingPassword("mypassword")).isTrue();
    }

    @Test
    void should_NotMatchPassword_When_IncorrectPasswordChecked() {
        profile.setPassword("mypassword");

        assertThat(profile.isMatchingPassword("wrongpassword")).isFalse();
    }

    @Test
    void should_NotMatchPassword_When_NullPasswordChecked() {
        profile.setPassword("mypassword");

        assertThat(profile.isMatchingPassword(null)).isFalse();
    }

    @Test
    void should_NotMatchPassword_When_NoPasswordStoredAndNullChecked() {
        // sPassword_ is null, so isMatchingPassword always returns false
        assertThat(profile.isMatchingPassword(null)).isFalse();
        assertThat(profile.isMatchingPassword("anything")).isFalse();
    }

    // =================================================================
    // JWT Session Field Tests (transient, not persisted)
    // =================================================================

    @Test
    void should_ReturnNullJwt_When_NoJwtSet() {
        assertThat(profile.getJwt()).isNull();
    }

    @Test
    void should_StoreJwt_When_JwtSet() {
        profile.setJwt("eyJhbGciOiJIUzI1NiJ9.payload.signature");

        assertThat(profile.getJwt()).isEqualTo("eyJhbGciOiJIUzI1NiJ9.payload.signature");
    }

    @Test
    void should_UpdateJwt_When_JwtSetTwice() {
        profile.setJwt("first-token");
        profile.setJwt("second-token");

        assertThat(profile.getJwt()).isEqualTo("second-token");
    }

    @Test
    void should_ClearJwt_When_NullJwtSet() {
        profile.setJwt("some-token");
        profile.setJwt(null);

        assertThat(profile.getJwt()).isNull();
    }

    // =================================================================
    // ProfileId Session Field Tests (transient, not persisted)
    // =================================================================

    @Test
    void should_ReturnNullProfileId_When_NoProfileIdSet() {
        assertThat(profile.getProfileId()).isNull();
    }

    @Test
    void should_StoreProfileId_When_ProfileIdSet() {
        profile.setProfileId(42L);

        assertThat(profile.getProfileId()).isEqualTo(42L);
    }

    @Test
    void should_UpdateProfileId_When_ProfileIdSetTwice() {
        profile.setProfileId(1L);
        profile.setProfileId(999L);

        assertThat(profile.getProfileId()).isEqualTo(999L);
    }

    @Test
    void should_ClearProfileId_When_NullProfileIdSet() {
        profile.setProfileId(7L);
        profile.setProfileId(null);

        assertThat(profile.getProfileId()).isNull();
    }

    // =================================================================
    // Copy Constructor Tests
    // =================================================================

    @Test
    void should_CopyName_When_CopyConstructorUsed() {
        profile.setEmail("original@example.com");
        profile.setPassword("pass123");

        PlayerProfile copy = new PlayerProfile(profile, "CopyUser");

        assertThat(copy.getName()).isEqualTo("CopyUser");
    }

    @Test
    void should_CopyEmail_When_CopyConstructorUsed() {
        profile.setEmail("original@example.com");

        PlayerProfile copy = new PlayerProfile(profile, "CopyUser");

        assertThat(copy.getEmail()).isEqualTo("original@example.com");
    }

    @Test
    void should_CopyPassword_When_CopyConstructorUsedWithSameName() {
        profile.setPassword("secretpass");

        // Copy with the SAME name so the encryption key (derived from name) matches
        PlayerProfile copy = new PlayerProfile(profile, "TestUser");

        // Password encrypted with "TestUser" key; copy also has name "TestUser" →
        // matches
        assertThat(copy.isMatchingPassword("secretpass")).isTrue();
    }

    @Test
    void should_NotCopyJwt_When_CopyConstructorUsed() {
        // JWT is transient (in-memory only), not copied
        profile.setJwt("some-token");

        PlayerProfile copy = new PlayerProfile(profile, "CopyUser");

        // Copy constructor does not copy jwt_ (it is in-memory state, not profile data)
        assertThat(copy.getJwt()).isNull();
    }

    @Test
    void should_NotCopyProfileId_When_CopyConstructorUsed() {
        // profileId is transient (in-memory only), not copied
        profile.setProfileId(77L);

        PlayerProfile copy = new PlayerProfile(profile, "CopyUser");

        assertThat(copy.getProfileId()).isNull();
    }

    // =================================================================
    // Stats Initialization Tests (init())
    // =================================================================

    @Test
    void should_InitializeStats_When_InitCalled() {
        profile.init();

        // After init, arrays are not null and wins/action counts are seeded
        assertThat(profile.nWins_).isZero();
        assertThat(profile.nActionCnt_).isZero();
        assertThat(profile.rounds_).isNotNull();
        assertThat(profile.flops_).isNotNull();
        assertThat(profile.actions_).isNotNull();
        assertThat(profile.roundactions_).isNotNull();
    }

    @Test
    void should_SeedPreFlopFolds_When_InitCalled() {
        profile.init();

        // Pre-flop fold seed is 14
        assertThat(profile.roundactions_[BettingRound.PRE_FLOP.toLegacy()][HandAction.ACTION_FOLD]).isEqualTo(14);
    }

    @Test
    void should_SeedPreFlopRoundTotal_When_InitCalled() {
        profile.init();

        // Pre-flop total seeded to 20
        assertThat(profile.nRoundActionCnt_[BettingRound.PRE_FLOP.toLegacy()]).isEqualTo(20);
    }

    @Test
    void should_LazilyInit_When_InitCheckCalledWithNullArrays() {
        // Before initCheck, arrays are null (newly constructed profile)
        assertThat(profile.rounds_).isNull();

        profile.initCheck();

        assertThat(profile.rounds_).isNotNull();
    }

    @Test
    void should_NotReinitialize_When_InitCheckCalledWithExistingArrays() {
        profile.init();
        // Manually set nWins_ to verify init() is not called again
        profile.nWins_ = 42;

        profile.initCheck();

        assertThat(profile.nWins_).isEqualTo(42);
    }

    // =================================================================
    // getFrequency Tests
    // =================================================================

    @Test
    void should_ReturnExpectedPreFlopFoldFrequency_When_DefaultSeeded() {
        profile.init();

        // Pre-flop: fold=14, total=20 → 14*100/20 = 70%
        int freq = profile.getFrequency(BettingRound.PRE_FLOP.toLegacy(), HandAction.ACTION_FOLD);
        assertThat(freq).isEqualTo(70);
    }

    @Test
    void should_ReturnExpectedPreFlopCallFrequency_When_DefaultSeeded() {
        profile.init();

        // Pre-flop: call=3, total=20 → 3*100/20 = 15%
        int freq = profile.getFrequency(BettingRound.PRE_FLOP.toLegacy(), HandAction.ACTION_CALL);
        assertThat(freq).isEqualTo(15);
    }

    @Test
    void should_ReturnExpectedFlopFoldFrequency_When_DefaultSeeded() {
        profile.init();

        // Flop: fold=6, total=20 → 6*100/20 = 30%
        int freq = profile.getFrequency(BettingRound.FLOP.toLegacy(), HandAction.ACTION_FOLD);
        assertThat(freq).isEqualTo(30);
    }

    // =================================================================
    // PROFILE_BEGIN Constant Tests
    // =================================================================

    @Test
    void should_HaveCorrectProfileBegin_When_ConstantChecked() {
        assertThat(PlayerProfile.PROFILE_BEGIN).isEqualTo("profile");
    }

    // =================================================================
    // toOnlineProfile Tests
    // =================================================================

    @Test
    void should_CreateOnlineProfileWithSameName_When_ToOnlineProfileCalled() {
        profile.setEmail("user@example.com");

        OnlineProfile online = profile.toOnlineProfile();

        assertThat(online.getName()).isEqualTo("TestUser");
    }

    @Test
    void should_CreateOnlineProfileWithSameEmail_When_ToOnlineProfileCalled() {
        profile.setEmail("user@example.com");

        OnlineProfile online = profile.toOnlineProfile();

        assertThat(online.getEmail()).isEqualTo("user@example.com");
    }

    // =================================================================
    // serverUrl Field Tests
    // =================================================================

    @Test
    void should_ReturnNullServerUrl_When_NotSet() {
        assertThat(profile.getServerUrl()).isNull();
    }

    @Test
    void should_StoreServerUrl_When_Set() {
        profile.setServerUrl("http://poker.example.com:8877");

        assertThat(profile.getServerUrl()).isEqualTo("http://poker.example.com:8877");
    }

    @Test
    void should_UpdateServerUrl_When_SetTwice() {
        profile.setServerUrl("http://first.example.com");
        profile.setServerUrl("http://second.example.com");

        assertThat(profile.getServerUrl()).isEqualTo("http://second.example.com");
    }

    @Test
    void should_ClearServerUrl_When_NullSet() {
        profile.setServerUrl("http://poker.example.com:8877");
        profile.setServerUrl(null);

        assertThat(profile.getServerUrl()).isNull();
    }

    @Test
    void should_CopyServerUrl_When_CopyConstructorUsed() {
        profile.setServerUrl("http://poker.example.com:8877");

        PlayerProfile copy = new PlayerProfile(profile, "CopyUser");

        assertThat(copy.getServerUrl()).isEqualTo("http://poker.example.com:8877");
    }
}
