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
package com.donohoedigital.games.poker.gameserver.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BanEntity} — verifies getters/setters and the
 * {@code isActive()} business rule without requiring a database.
 */
class BanEntityTest {

    @Test
    void should_returnTrue_when_banUntilDateIsInFuture() {
        BanEntity ban = new BanEntity();
        ban.setUntil(LocalDate.now().plusDays(30));
        assertThat(ban.isActive()).isTrue();
    }

    @Test
    void should_returnTrue_when_banUntilDateIsToday() {
        BanEntity ban = new BanEntity();
        ban.setUntil(LocalDate.now());
        assertThat(ban.isActive()).isTrue();
    }

    @Test
    void should_returnFalse_when_banUntilDateIsInPast() {
        BanEntity ban = new BanEntity();
        ban.setUntil(LocalDate.now().minusDays(1));
        assertThat(ban.isActive()).isFalse();
    }

    @Test
    void should_storeBanType() {
        BanEntity ban = new BanEntity();
        ban.setBanType(BanEntity.BanType.PROFILE);
        assertThat(ban.getBanType()).isEqualTo(BanEntity.BanType.PROFILE);
    }

    @Test
    void should_storeEmailBanType() {
        BanEntity ban = new BanEntity();
        ban.setBanType(BanEntity.BanType.EMAIL);
        assertThat(ban.getBanType()).isEqualTo(BanEntity.BanType.EMAIL);
    }

    @Test
    void should_storeProfileId() {
        BanEntity ban = new BanEntity();
        ban.setProfileId(123L);
        assertThat(ban.getProfileId()).isEqualTo(123L);
    }

    @Test
    void should_storeEmail() {
        BanEntity ban = new BanEntity();
        ban.setEmail("banned@example.com");
        assertThat(ban.getEmail()).isEqualTo("banned@example.com");
    }

    @Test
    void should_storeReason() {
        BanEntity ban = new BanEntity();
        ban.setReason("cheating");
        assertThat(ban.getReason()).isEqualTo("cheating");
    }

    @Test
    void should_storeCreatedAt() {
        BanEntity ban = new BanEntity();
        Instant now = Instant.now();
        ban.setCreatedAt(now);
        assertThat(ban.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void should_storeId() {
        BanEntity ban = new BanEntity();
        ban.setId(99L);
        assertThat(ban.getId()).isEqualTo(99L);
    }

    @Test
    void banType_should_haveProfileAndEmailValues() {
        assertThat(BanEntity.BanType.values()).containsExactlyInAnyOrder(BanEntity.BanType.PROFILE,
                BanEntity.BanType.EMAIL);
    }

    @Test
    void banType_should_supportValueOf() {
        assertThat(BanEntity.BanType.valueOf("PROFILE")).isEqualTo(BanEntity.BanType.PROFILE);
        assertThat(BanEntity.BanType.valueOf("EMAIL")).isEqualTo(BanEntity.BanType.EMAIL);
    }
}
