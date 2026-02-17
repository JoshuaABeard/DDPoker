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

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.BanRepository;

@DataJpaTest
@ContextConfiguration(classes = TestJpaConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BanService.class)
class BanServiceTest {

    @Autowired
    private BanService banService;

    @Autowired
    private BanRepository banRepository;

    @Test
    void testIsProfileBanned_NotBanned() {
        assertThat(banService.isProfileBanned(999L)).isFalse();
    }

    @Test
    void testIsProfileBanned_ActiveBan() {
        BanEntity ban = new BanEntity();
        ban.setBanType(BanEntity.BanType.PROFILE);
        ban.setProfileId(123L);
        ban.setUntil(LocalDate.now().plusDays(30));
        ban.setReason("Test ban");
        banRepository.save(ban);

        assertThat(banService.isProfileBanned(123L)).isTrue();
    }

    @Test
    void testIsProfileBanned_ExpiredBan() {
        BanEntity ban = new BanEntity();
        ban.setBanType(BanEntity.BanType.PROFILE);
        ban.setProfileId(456L);
        ban.setUntil(LocalDate.now().minusDays(1));
        ban.setReason("Expired ban");
        banRepository.save(ban);

        assertThat(banService.isProfileBanned(456L)).isFalse();
    }

    @Test
    void testIsEmailBanned_NotBanned() {
        assertThat(banService.isEmailBanned("clean@example.com")).isFalse();
    }

    @Test
    void testIsEmailBanned_ActiveBan() {
        BanEntity ban = new BanEntity();
        ban.setBanType(BanEntity.BanType.EMAIL);
        ban.setEmail("banned@example.com");
        ban.setUntil(LocalDate.now().plusDays(30));
        ban.setReason("Test ban");
        banRepository.save(ban);

        assertThat(banService.isEmailBanned("banned@example.com")).isTrue();
    }

    @Test
    void testIsEmailBanned_ExpiredBan() {
        BanEntity ban = new BanEntity();
        ban.setBanType(BanEntity.BanType.EMAIL);
        ban.setEmail("expired@example.com");
        ban.setUntil(LocalDate.now().minusDays(1));
        ban.setReason("Expired ban");
        banRepository.save(ban);

        assertThat(banService.isEmailBanned("expired@example.com")).isFalse();
    }

    @Test
    void testIsProfileBanned_MultipleActiveBans() {
        // Create two active bans for same profile
        BanEntity ban1 = new BanEntity();
        ban1.setBanType(BanEntity.BanType.PROFILE);
        ban1.setProfileId(789L);
        ban1.setUntil(LocalDate.now().plusDays(10));
        ban1.setReason("First ban");
        banRepository.save(ban1);

        BanEntity ban2 = new BanEntity();
        ban2.setBanType(BanEntity.BanType.PROFILE);
        ban2.setProfileId(789L);
        ban2.setUntil(LocalDate.now().plusDays(20));
        ban2.setReason("Second ban");
        banRepository.save(ban2);

        assertThat(banService.isProfileBanned(789L)).isTrue();
    }
}
