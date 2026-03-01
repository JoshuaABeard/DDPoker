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
package com.donohoedigital.games.poker.gameserver.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CorsProperties} defaults and setters.
 */
class CorsPropertiesTest {

    @Test
    void should_haveEmptyAllowedOrigins_byDefault() {
        CorsProperties props = new CorsProperties();
        assertThat(props.getAllowedOrigins()).isEmpty();
    }

    @Test
    void should_haveDefaultAllowedMethods() {
        CorsProperties props = new CorsProperties();
        assertThat(props.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    void should_haveWildcardAllowedHeaders_byDefault() {
        CorsProperties props = new CorsProperties();
        assertThat(props.getAllowedHeaders()).containsExactly("*");
    }

    @Test
    void should_allowCredentials_byDefault() {
        CorsProperties props = new CorsProperties();
        assertThat(props.isAllowCredentials()).isTrue();
    }

    @Test
    void should_haveMaxAge3600_byDefault() {
        CorsProperties props = new CorsProperties();
        assertThat(props.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void should_setAllowedOrigins() {
        CorsProperties props = new CorsProperties();
        props.setAllowedOrigins(List.of("https://example.com", "https://test.com"));
        assertThat(props.getAllowedOrigins()).containsExactly("https://example.com", "https://test.com");
    }

    @Test
    void should_setAllowedMethods() {
        CorsProperties props = new CorsProperties();
        props.setAllowedMethods(List.of("GET", "POST"));
        assertThat(props.getAllowedMethods()).containsExactly("GET", "POST");
    }

    @Test
    void should_setAllowedHeaders() {
        CorsProperties props = new CorsProperties();
        props.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        assertThat(props.getAllowedHeaders()).containsExactly("Content-Type", "Authorization");
    }

    @Test
    void should_setAllowCredentials() {
        CorsProperties props = new CorsProperties();
        props.setAllowCredentials(false);
        assertThat(props.isAllowCredentials()).isFalse();
    }

    @Test
    void should_setMaxAge() {
        CorsProperties props = new CorsProperties();
        props.setMaxAge(7200L);
        assertThat(props.getMaxAge()).isEqualTo(7200L);
    }
}
