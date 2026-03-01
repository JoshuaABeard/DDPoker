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
package com.donohoedigital.games.poker.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TournamentTemplate JPA entity POJO.
 */
class TournamentTemplateTest {

    // ===== Default Construction =====

    @Test
    void should_HaveNullDefaults_WhenNewlyConstructed() {
        TournamentTemplate template = new TournamentTemplate();

        assertThat(template.getId()).isNull();
        assertThat(template.getProfileId()).isNull();
        assertThat(template.getName()).isNull();
        assertThat(template.getConfig()).isNull();
        assertThat(template.getCreateDate()).isNull();
        assertThat(template.getModifyDate()).isNull();
    }

    // ===== Getters and Setters =====

    @Test
    void should_ReturnId_WhenSet() {
        TournamentTemplate template = new TournamentTemplate();
        template.setId(42L);

        assertThat(template.getId()).isEqualTo(42L);
    }

    @Test
    void should_ReturnProfileId_WhenSet() {
        TournamentTemplate template = new TournamentTemplate();
        template.setProfileId(99L);

        assertThat(template.getProfileId()).isEqualTo(99L);
    }

    @Test
    void should_ReturnName_WhenSet() {
        TournamentTemplate template = new TournamentTemplate();
        template.setName("Friday Night Poker");

        assertThat(template.getName()).isEqualTo("Friday Night Poker");
    }

    @Test
    void should_ReturnConfig_WhenSet() {
        TournamentTemplate template = new TournamentTemplate();
        String config = "{\"buyIn\":100,\"blinds\":\"10/20\"}";
        template.setConfig(config);

        assertThat(template.getConfig()).isEqualTo(config);
    }

    @Test
    void should_ReturnCreateDate_WhenSet() {
        TournamentTemplate template = new TournamentTemplate();
        Date now = new Date();
        template.setCreateDate(now);

        assertThat(template.getCreateDate()).isEqualTo(now);
    }

    @Test
    void should_ReturnModifyDate_WhenSet() {
        TournamentTemplate template = new TournamentTemplate();
        Date now = new Date();
        template.setModifyDate(now);

        assertThat(template.getModifyDate()).isEqualTo(now);
    }

    // ===== Setters Accept Null =====

    @Test
    void should_AcceptNull_WhenSettingIdToNull() {
        TournamentTemplate template = new TournamentTemplate();
        template.setId(42L);
        template.setId(null);

        assertThat(template.getId()).isNull();
    }

    @Test
    void should_AcceptNull_WhenSettingNameToNull() {
        TournamentTemplate template = new TournamentTemplate();
        template.setName("Test");
        template.setName(null);

        assertThat(template.getName()).isNull();
    }

    // ===== toString =====

    @Test
    void should_IncludeAllKeyFields_WhenToStringCalled() {
        TournamentTemplate template = new TournamentTemplate();
        template.setId(7L);
        template.setProfileId(123L);
        template.setName("Weekend Tournament");

        String result = template.toString();

        assertThat(result).contains("7");
        assertThat(result).contains("123");
        assertThat(result).contains("Weekend Tournament");
    }

    @Test
    void should_ContainClassName_WhenToStringCalled() {
        TournamentTemplate template = new TournamentTemplate();

        assertThat(template.toString()).startsWith("TournamentTemplate{");
    }

    @Test
    void should_HandleNullFields_WhenToStringCalled() {
        TournamentTemplate template = new TournamentTemplate();

        String result = template.toString();

        assertThat(result).contains("null");
        assertThat(result).doesNotContain("Exception");
    }

    // ===== BaseModel Contract =====

    @Test
    void should_ImplementBaseModel_WhenGetIdCalled() {
        TournamentTemplate template = new TournamentTemplate();
        template.setId(55L);

        // getId() satisfies the BaseModel<Long> contract
        Long id = template.getId();
        assertThat(id).isEqualTo(55L);
    }
}
