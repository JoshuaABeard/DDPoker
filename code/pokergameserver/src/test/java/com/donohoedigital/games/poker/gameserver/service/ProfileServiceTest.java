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
package com.donohoedigital.games.poker.gameserver.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

@DataJpaTest
@ContextConfiguration(classes = TestJpaConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProfileService.class)
class ProfileServiceTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private OnlineProfileRepository profileRepository;

    @Test
    void testGetProfile() {
        OnlineProfile profile = createProfile("testuser", "test@example.com");

        OnlineProfile result = profileService.getProfile(profile.getId());

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void testGetProfileNotFound() {
        OnlineProfile result = profileService.getProfile(999L);
        assertThat(result).isNull();
    }

    @Test
    void testDeleteProfile() {
        OnlineProfile profile = createProfile("testuser", "test@example.com");

        boolean success = profileService.deleteProfile(profile.getId());

        assertThat(success).isTrue();
        OnlineProfile deleted = profileRepository.findById(profile.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.isRetired()).isTrue();
    }

    @Test
    void testDeleteProfileNotFound() {
        boolean success = profileService.deleteProfile(999L);
        assertThat(success).isFalse();
    }

    // =========================================================================
    // changePassword
    // =========================================================================

    @Test
    void changePassword_withCorrectOldPassword_updatesHash() {
        OnlineProfile profile = createProfile("pwduser", "pwd@example.com");

        profileService.changePassword(profile.getId(), "password", "newpassword");

        OnlineProfile updated = profileRepository.findById(profile.getId()).orElseThrow();
        assertThat(org.mindrot.jbcrypt.BCrypt.checkpw("newpassword", updated.getPasswordHash())).isTrue();
        assertThat(org.mindrot.jbcrypt.BCrypt.checkpw("password", updated.getPasswordHash())).isFalse();
    }

    @Test
    void changePassword_withWrongOldPassword_throwsInvalidPasswordException() {
        OnlineProfile profile = createProfile("wrongpwduser", "wrong@example.com");

        assertThatThrownBy(() -> profileService.changePassword(profile.getId(), "wrongpass", "newpass"))
                .isInstanceOf(ProfileService.InvalidPasswordException.class);
    }

    @Test
    void changePassword_withNonexistentProfile_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> profileService.changePassword(999L, "old", "new"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private OnlineProfile createProfile(String name, String email) {
        OnlineProfile profile = new OnlineProfile();
        profile.setName(name);
        profile.setEmail(email);
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("password", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        return profileRepository.save(profile);
    }
}
