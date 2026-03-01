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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JwtProperties} defaults and setters.
 */
class JwtPropertiesTest {

    @Test
    void should_haveNullPrivateKeyPath_byDefault() {
        JwtProperties props = new JwtProperties();
        assertThat(props.getPrivateKeyPath()).isNull();
    }

    @Test
    void should_haveNullPublicKeyPath_byDefault() {
        JwtProperties props = new JwtProperties();
        assertThat(props.getPublicKeyPath()).isNull();
    }

    @Test
    void should_haveOneHourExpiration_byDefault() {
        JwtProperties props = new JwtProperties();
        assertThat(props.getExpiration()).isEqualTo(3600000L); // 1 hour in ms
    }

    @Test
    void should_haveSevenDayRememberMeExpiration_byDefault() {
        JwtProperties props = new JwtProperties();
        assertThat(props.getRememberMeExpiration()).isEqualTo(604800000L); // 7 days in ms
    }

    @Test
    void should_haveDefaultCookieName() {
        JwtProperties props = new JwtProperties();
        assertThat(props.getCookieName()).isEqualTo("DDPoker-JWT");
    }

    @Test
    void should_setPrivateKeyPath() {
        JwtProperties props = new JwtProperties();
        props.setPrivateKeyPath("/etc/keys/private.pem");
        assertThat(props.getPrivateKeyPath()).isEqualTo("/etc/keys/private.pem");
    }

    @Test
    void should_setPublicKeyPath() {
        JwtProperties props = new JwtProperties();
        props.setPublicKeyPath("/etc/keys/public.pem");
        assertThat(props.getPublicKeyPath()).isEqualTo("/etc/keys/public.pem");
    }

    @Test
    void should_setExpiration() {
        JwtProperties props = new JwtProperties();
        props.setExpiration(7200000L); // 2 hours in ms
        assertThat(props.getExpiration()).isEqualTo(7200000L); // 2 hours in ms
    }

    @Test
    void should_setRememberMeExpiration() {
        JwtProperties props = new JwtProperties();
        props.setRememberMeExpiration(1209600000L); // 14 days in ms
        assertThat(props.getRememberMeExpiration()).isEqualTo(1209600000L); // 14 days in ms
    }

    @Test
    void should_setCookieName() {
        JwtProperties props = new JwtProperties();
        props.setCookieName("MyApp-Auth");
        assertThat(props.getCookieName()).isEqualTo("MyApp-Auth");
    }
}
