/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OnlineServerUrlTest {

    @Test
    void normalizeBaseUrl_addsHttpSchemeForHostPortPreference() {
        assertThat(OnlineServerUrl.normalizeBaseUrl("example.com:8877")).isEqualTo("http://example.com:8877");
    }

    @Test
    void normalizeBaseUrl_keepsHttpsSchemeWhenAlreadyProvided() {
        assertThat(OnlineServerUrl.normalizeBaseUrl("https://example.com:8877")).isEqualTo("https://example.com:8877");
    }

    @Test
    void normalizeBaseUrl_stripsPathQueryAndFragment() {
        assertThat(OnlineServerUrl.normalizeBaseUrl("https://example.com:8877/foo?x=1#frag"))
                .isEqualTo("https://example.com:8877");
    }

    @Test
    void normalizeBaseUrl_returnsNullForBlankInput() {
        assertThat(OnlineServerUrl.normalizeBaseUrl("   ")).isNull();
    }

    @Test
    void normalizeBaseUrl_returnsNullForUnsupportedScheme() {
        assertThat(OnlineServerUrl.normalizeBaseUrl("ftp://example.com:21")).isNull();
    }

    @Test
    void buildApiUri_returnsNullWhenBaseUrlInvalid() {
        assertThat(OnlineServerUrl.buildApiUri("http://", "/api/v1/games")).isNull();
    }

    @Test
    void buildApiUri_buildsPathAgainstNormalizedBaseUrl() {
        assertThat(OnlineServerUrl.buildApiUri("example.com:8877", "/api/v1/games?pageSize=1").toString())
                .isEqualTo("http://example.com:8877/api/v1/games?pageSize=1");
    }
}
