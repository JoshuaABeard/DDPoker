/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.poker.gameserver.persistence.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Tests for {@link OnlineProfileRepository}.
 */
@DataJpaTest
@ContextConfiguration(classes = TestJpaConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OnlineProfileRepositoryTest {

    @Autowired
    private OnlineProfileRepository repository;

    @Test
    void testFindByEmailVerificationToken_found() {
        OnlineProfile profile = newProfile("user1", "user1@example.com");
        profile.setEmailVerificationToken("tok123");
        repository.save(profile);

        Optional<OnlineProfile> result = repository.findByEmailVerificationToken("tok123");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("user1");
    }

    @Test
    void testFindByEmailVerificationToken_notFound() {
        Optional<OnlineProfile> result = repository.findByEmailVerificationToken("no-such-token");

        assertThat(result).isEmpty();
    }

    @Test
    void testFindByPendingEmail_found() {
        OnlineProfile profile = newProfile("user2", "user2@example.com");
        profile.setPendingEmail("new@example.com");
        repository.save(profile);

        Optional<OnlineProfile> result = repository.findByPendingEmail("new@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("user2");
    }

    @Test
    void testFindByEmail_found() {
        OnlineProfile profile = newProfile("user3", "email@example.com");
        repository.save(profile);

        Optional<OnlineProfile> result = repository.findByEmail("email@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("user3");
    }

    @Test
    void testFindByEmail_notFound() {
        Optional<OnlineProfile> result = repository.findByEmail("nobody@example.com");

        assertThat(result).isEmpty();
    }

    // Helper: create a minimal valid OnlineProfile
    private OnlineProfile newProfile(String name, String email) {
        OnlineProfile p = new OnlineProfile();
        p.setName(name);
        p.setEmail(email);
        p.setPasswordHash("hash");
        p.setUuid(UUID.randomUUID().toString());
        return p;
    }
}
