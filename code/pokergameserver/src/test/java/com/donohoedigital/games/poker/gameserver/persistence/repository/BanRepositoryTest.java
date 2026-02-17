/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity;
import com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity.BanType;

/**
 * Tests for {@link BanRepository}.
 */
@DataJpaTest
@ContextConfiguration(classes = TestJpaConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BanRepositoryTest {

    @Autowired
    private BanRepository repository;

    @Test
    void testSaveAndFindProfileBan() {
        BanEntity ban = createProfileBan(100L, LocalDate.now().plusDays(30), "Cheating");

        BanEntity saved = repository.save(ban);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBanType()).isEqualTo(BanType.PROFILE);
        assertThat(saved.getProfileId()).isEqualTo(100L);
    }

    @Test
    void testSaveAndFindEmailBan() {
        BanEntity ban = createEmailBan("cheater@example.com", LocalDate.now().plusDays(30), "Multiple accounts");

        BanEntity saved = repository.save(ban);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBanType()).isEqualTo(BanType.EMAIL);
        assertThat(saved.getEmail()).isEqualTo("cheater@example.com");
    }

    @Test
    void testFindByProfileId() {
        repository.save(createProfileBan(100L, LocalDate.now().plusDays(30), "Ban 1"));
        repository.save(createProfileBan(100L, LocalDate.now().plusDays(60), "Ban 2"));
        repository.save(createProfileBan(200L, LocalDate.now().plusDays(30), "Different user"));

        List<BanEntity> bans = repository.findByProfileId(100L);

        assertThat(bans).hasSize(2);
        assertThat(bans).allMatch(b -> b.getProfileId().equals(100L));
    }

    @Test
    void testFindByEmail() {
        repository.save(createEmailBan("banned@example.com", LocalDate.now().plusDays(30), "Ban 1"));
        repository.save(createEmailBan("banned@example.com", LocalDate.now().plusDays(60), "Ban 2"));
        repository.save(createEmailBan("other@example.com", LocalDate.now().plusDays(30), "Different email"));

        List<BanEntity> bans = repository.findByEmail("banned@example.com");

        assertThat(bans).hasSize(2);
        assertThat(bans).allMatch(b -> b.getEmail().equals("banned@example.com"));
    }

    @Test
    void testIsActiveMethod() {
        // Active ban (expires tomorrow)
        BanEntity activeBan = createProfileBan(100L, LocalDate.now().plusDays(1), "Active");
        assertThat(activeBan.isActive()).isTrue();

        // Expired ban (expired yesterday)
        BanEntity expiredBan = createProfileBan(200L, LocalDate.now().minusDays(1), "Expired");
        assertThat(expiredBan.isActive()).isFalse();

        // Ban expiring today (still active)
        BanEntity todayBan = createProfileBan(300L, LocalDate.now(), "Expires today");
        assertThat(todayBan.isActive()).isTrue();
    }

    @Test
    void testPrePersistSetsDefaults() {
        BanEntity ban = new BanEntity();
        ban.setBanType(BanType.PROFILE);
        ban.setProfileId(100L);
        ban.setReason("Test");
        // Don't set until or createdAt

        BanEntity saved = repository.save(ban);

        assertThat(saved.getUntil()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        // Default until should be far future
        assertThat(saved.getUntil()).isAfter(LocalDate.now().plusYears(50));
    }

    @Test
    void testPermanentBan() {
        BanEntity permanentBan = createProfileBan(100L, LocalDate.of(2099, 12, 31), "Permanent ban");

        repository.save(permanentBan);

        List<BanEntity> bans = repository.findByProfileId(100L);
        assertThat(bans).hasSize(1);
        assertThat(bans.get(0).isActive()).isTrue();
        assertThat(bans.get(0).getUntil()).isEqualTo(LocalDate.of(2099, 12, 31));
    }

    // Helper methods
    private BanEntity createProfileBan(Long profileId, LocalDate until, String reason) {
        BanEntity ban = new BanEntity();
        ban.setBanType(BanType.PROFILE);
        ban.setProfileId(profileId);
        ban.setUntil(until);
        ban.setReason(reason);
        return ban;
    }

    private BanEntity createEmailBan(String email, LocalDate until, String reason) {
        BanEntity ban = new BanEntity();
        ban.setBanType(BanType.EMAIL);
        ban.setEmail(email);
        ban.setUntil(until);
        ban.setReason(reason);
        return ban;
    }
}
