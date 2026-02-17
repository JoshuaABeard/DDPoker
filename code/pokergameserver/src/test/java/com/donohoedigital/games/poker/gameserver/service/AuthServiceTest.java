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
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.repository.BanRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

import java.nio.file.Path;
import java.security.KeyPair;

@DataJpaTest
@ContextConfiguration(classes = {TestJpaConfiguration.class, AuthServiceTest.TestConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthServiceTest {

    @Autowired
    private OnlineProfileRepository profileRepository;

    @Autowired
    private BanRepository banRepository;

    @Autowired
    private AuthService authService;

    @org.springframework.context.annotation.Configuration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public JwtTokenProvider jwtTokenProvider() throws Exception {
            KeyPair keyPair = com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager.generateKeyPair();
            String uniqueId = java.util.UUID.randomUUID().toString();
            Path privateKey = Path.of(System.getProperty("java.io.tmpdir"), "test-jwt-private-" + uniqueId + ".pem");
            Path publicKey = Path.of(System.getProperty("java.io.tmpdir"), "test-jwt-public-" + uniqueId + ".pem");

            com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager.savePrivateKey(keyPair.getPrivate(),
                    privateKey);
            com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager.savePublicKey(keyPair.getPublic(), publicKey);

            return new JwtTokenProvider(privateKey, publicKey, 3600000L, 604800000L);
        }

        @org.springframework.context.annotation.Bean
        public BanService banService(BanRepository banRepository) {
            return new BanService(banRepository);
        }

        @org.springframework.context.annotation.Bean
        public AuthService authService(OnlineProfileRepository profileRepository, BanService banService,
                JwtTokenProvider tokenProvider) {
            return new AuthService(profileRepository, banService, tokenProvider);
        }
    }

    @Test
    void testRegisterNewProfile() {
        LoginResponse response = authService.register("newuser", "password123", "new@example.com");

        assertThat(response.success()).isTrue();
        assertThat(response.token()).isNotNull();
        assertThat(response.profileId()).isNotNull();

        // Verify profile was created
        OnlineProfile profile = profileRepository.findByName("newuser").orElse(null);
        assertThat(profile).isNotNull();
        assertThat(profile.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void testRegisterDuplicateUsername() {
        // Create existing profile
        OnlineProfile existing = new OnlineProfile();
        existing.setName("existing");
        existing.setEmail("existing@example.com");
        existing.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("password", org.mindrot.jbcrypt.BCrypt.gensalt()));
        existing.setUuid(java.util.UUID.randomUUID().toString());
        profileRepository.save(existing);

        LoginResponse response = authService.register("existing", "newpass", "different@example.com");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("already exists");
    }

    @Test
    void testLoginSuccess() {
        // Create profile with BCrypt password
        OnlineProfile profile = new OnlineProfile();
        profile.setName("testuser");
        profile.setEmail("test@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("password123", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profileRepository.save(profile);

        LoginResponse response = authService.login("testuser", "password123", false);

        assertThat(response.success()).isTrue();
        assertThat(response.token()).isNotNull();
        assertThat(response.profileId()).isEqualTo(profile.getId());
        assertThat(response.username()).isEqualTo("testuser");
    }

    @Test
    void testLoginWrongPassword() {
        OnlineProfile profile = new OnlineProfile();
        profile.setName("testuser");
        profile.setEmail("test@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("correctpass", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profileRepository.save(profile);

        LoginResponse response = authService.login("testuser", "wrongpass", false);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Invalid");
    }

    @Test
    void testLoginNonexistentUser() {
        LoginResponse response = authService.login("nonexistent", "password", false);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Invalid");
    }

    @Test
    void testLoginBannedProfile() {
        // Create profile
        OnlineProfile profile = new OnlineProfile();
        profile.setName("banned");
        profile.setEmail("banned@example.com");
        profile.setPasswordHash(org.mindrot.jbcrypt.BCrypt.hashpw("password", org.mindrot.jbcrypt.BCrypt.gensalt()));
        profile.setUuid(java.util.UUID.randomUUID().toString());
        profileRepository.save(profile);

        // Ban the profile
        com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity ban = new com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity();
        ban.setBanType(com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity.BanType.PROFILE);
        ban.setProfileId(profile.getId());
        ban.setUntil(java.time.LocalDate.now().plusDays(30));
        ban.setReason("Test ban");
        banRepository.save(ban);

        LoginResponse response = authService.login("banned", "password", false);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("banned");
    }

    @Test
    void testRegisterBannedEmail() {
        // Ban an email
        com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity ban = new com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity();
        ban.setBanType(com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity.BanType.EMAIL);
        ban.setEmail("banned@example.com");
        ban.setUntil(java.time.LocalDate.now().plusDays(30));
        ban.setReason("Email ban");
        banRepository.save(ban);

        LoginResponse response = authService.register("newuser", "password", "banned@example.com");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("banned");
    }
}
