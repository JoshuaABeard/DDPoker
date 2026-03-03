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

    @Test
    void normalizeBaseUrl_returnsNullForNullInput() {
        assertThat(OnlineServerUrl.normalizeBaseUrl(null)).isNull();
    }

    @Test
    void normalizeBaseUrl_returnsNullForMissingHost() {
        // URI with scheme but no host
        assertThat(OnlineServerUrl.normalizeBaseUrl("http://")).isNull();
    }

    @Test
    void buildApiUri_returnsNullWhenApiPathIsNull() {
        assertThat(OnlineServerUrl.buildApiUri("example.com:8877", null)).isNull();
    }

    @Test
    void buildApiUri_returnsNullWhenApiPathIsBlank() {
        assertThat(OnlineServerUrl.buildApiUri("example.com:8877", "   ")).isNull();
    }

    @Test
    void buildApiUri_prependsSlashIfMissing() {
        assertThat(OnlineServerUrl.buildApiUri("example.com:8877", "api/v1/games").toString())
                .isEqualTo("http://example.com:8877/api/v1/games");
    }

    @Test
    void normalizeBaseUrl_returnsNullForInvalidUri() {
        // URI with spaces causes IllegalArgumentException
        assertThat(OnlineServerUrl.normalizeBaseUrl("http://example .com:8877")).isNull();
    }

    @Test
    void buildApiUri_returnsNullForInvalidBaseUrl() {
        // completely invalid value for base URL
        assertThat(OnlineServerUrl.buildApiUri("ftp://not-valid", "/api")).isNull();
    }

    @Test
    void toWsBaseUrl_convertsHttpToWs() {
        assertThat(OnlineServerUrl.toWsBaseUrl("http://server.example.com:8080"))
                .isEqualTo("ws://server.example.com:8080");
    }

    @Test
    void toWsBaseUrl_convertsHttpsToWss() {
        assertThat(OnlineServerUrl.toWsBaseUrl("https://server.example.com:443"))
                .isEqualTo("wss://server.example.com:443");
    }

    @Test
    void toWsBaseUrl_returnsNullForNull() {
        assertThat(OnlineServerUrl.toWsBaseUrl(null)).isNull();
    }
}
