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
package com.donohoedigital.p2p;

import com.donohoedigital.base.ApplicationError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for P2PURL - Custom P2P URL parsing.
 * Format: protocol://host:port/uri
 * Example: poker://192.211.1.110:11885/n-2/WXZ-741
 */
class P2PURLTest {

    private static final String VALID_URL = "poker://192.211.1.110:11885/n-2/WXZ-741";

    // =================================================================
    // Valid URL Parsing Tests
    // =================================================================

    @Test
    void should_ParseProtocol_When_ValidURLProvided() {
        P2PURL url = new P2PURL(VALID_URL);

        assertThat(url.toString()).startsWith("poker://");
    }

    @Test
    void should_ParseHost_When_ValidURLProvided() {
        P2PURL url = new P2PURL(VALID_URL);

        assertThat(url.getHost()).isEqualTo("192.211.1.110");
    }

    @Test
    void should_ParsePort_When_ValidURLProvided() {
        P2PURL url = new P2PURL(VALID_URL);

        assertThat(url.getPort()).isEqualTo(11885);
    }

    @Test
    void should_ParseURI_When_ValidURLProvided() {
        P2PURL url = new P2PURL(VALID_URL);

        assertThat(url.getURI()).isEqualTo("n-2/WXZ-741");
    }

    @Test
    void should_RoundTripCorrectly_When_ToStringCalled() {
        P2PURL url = new P2PURL(VALID_URL);

        assertThat(url.toString()).isEqualTo(VALID_URL);
    }

    // =================================================================
    // Different Protocol Tests
    // =================================================================

    @Test
    void should_ParseHTTP_When_HTTPProtocolUsed() {
        String httpUrl = "http://example.com:8080/path";
        P2PURL url = new P2PURL(httpUrl);

        assertThat(url.getHost()).isEqualTo("example.com");
        assertThat(url.getPort()).isEqualTo(8080);
        assertThat(url.getURI()).isEqualTo("path");
        assertThat(url.toString()).isEqualTo(httpUrl);
    }

    @Test
    void should_ParseCustomProtocol_When_CustomProtocolUsed() {
        String customUrl = "myapp://localhost:9999/resource";
        P2PURL url = new P2PURL(customUrl);

        assertThat(url.getHost()).isEqualTo("localhost");
        assertThat(url.getPort()).isEqualTo(9999);
        assertThat(url.getURI()).isEqualTo("resource");
    }

    @Test
    void should_ParseSingleLetterProtocol_When_ShortProtocolUsed() {
        String shortUrl = "a://host:123/uri";
        P2PURL url = new P2PURL(shortUrl);

        assertThat(url.getHost()).isEqualTo("host");
        assertThat(url.getPort()).isEqualTo(123);
    }

    // =================================================================
    // Different Host Tests
    // =================================================================

    @Test
    void should_ParseLocalhost_When_LocalhostUsed() {
        String localhostUrl = "poker://localhost:8080/game";
        P2PURL url = new P2PURL(localhostUrl);

        assertThat(url.getHost()).isEqualTo("localhost");
    }

    @Test
    void should_ParseDomainName_When_DomainNameUsed() {
        String domainUrl = "poker://example.com:9000/game/123";
        P2PURL url = new P2PURL(domainUrl);

        assertThat(url.getHost()).isEqualTo("example.com");
    }

    @Test
    void should_ParseSubdomain_When_SubdomainUsed() {
        String subdomainUrl = "poker://game.example.com:9000/room";
        P2PURL url = new P2PURL(subdomainUrl);

        assertThat(url.getHost()).isEqualTo("game.example.com");
    }

    @Test
    void should_Parse127Address_When_LoopbackUsed() {
        String loopbackUrl = "poker://127.0.0.1:8080/game";
        P2PURL url = new P2PURL(loopbackUrl);

        assertThat(url.getHost()).isEqualTo("127.0.0.1");
    }

    // =================================================================
    // Different Port Tests
    // =================================================================

    @Test
    void should_ParseStandardPort_When_Port80Used() {
        String port80Url = "http://example.com:80/path";
        P2PURL url = new P2PURL(port80Url);

        assertThat(url.getPort()).isEqualTo(80);
    }

    @Test
    void should_ParseHighPort_When_HighPortUsed() {
        String highPortUrl = "poker://host:65535/uri";
        P2PURL url = new P2PURL(highPortUrl);

        assertThat(url.getPort()).isEqualTo(65535);
    }

    @Test
    void should_ParseLowPort_When_LowPortUsed() {
        String lowPortUrl = "poker://host:1/uri";
        P2PURL url = new P2PURL(lowPortUrl);

        assertThat(url.getPort()).isEqualTo(1);
    }

    @Test
    void should_ParseFourDigitPort_When_CommonPortUsed() {
        String commonPortUrl = "poker://host:1234/uri";
        P2PURL url = new P2PURL(commonPortUrl);

        assertThat(url.getPort()).isEqualTo(1234);
    }

    // =================================================================
    // Different URI Tests
    // =================================================================

    @Test
    void should_ParseSimpleURI_When_SimplePathUsed() {
        String simpleUrl = "poker://host:123/game";
        P2PURL url = new P2PURL(simpleUrl);

        assertThat(url.getURI()).isEqualTo("game");
    }

    @Test
    void should_ParseNestedURI_When_NestedPathUsed() {
        String nestedUrl = "poker://host:123/game/room/table";
        P2PURL url = new P2PURL(nestedUrl);

        assertThat(url.getURI()).isEqualTo("game/room/table");
    }

    @Test
    void should_ParseURIWithHyphen_When_HyphenUsed() {
        String hyphenUrl = "poker://host:123/game-room-123";
        P2PURL url = new P2PURL(hyphenUrl);

        assertThat(url.getURI()).isEqualTo("game-room-123");
    }

    @Test
    void should_ParseURIWithNumbers_When_NumbersUsed() {
        String numberUrl = "poker://host:123/123456";
        P2PURL url = new P2PURL(numberUrl);

        assertThat(url.getURI()).isEqualTo("123456");
    }

    @Test
    void should_ParseEmptyURI_When_EmptyPathProvided() {
        String emptyUriUrl = "poker://host:123/";
        P2PURL url = new P2PURL(emptyUriUrl);

        assertThat(url.getURI()).isEmpty();
    }

    @Test
    void should_ParseLongURI_When_LongPathProvided() {
        String longUriUrl = "poker://host:123/very/long/path/to/resource/with/many/segments";
        P2PURL url = new P2PURL(longUriUrl);

        assertThat(url.getURI()).isEqualTo("very/long/path/to/resource/with/many/segments");
    }

    // =================================================================
    // Invalid URL Tests - Missing Delimiters
    // =================================================================

    @Test
    void should_ThrowException_When_MissingProtocolDelimiter() {
        String invalidUrl = "poker:192.211.1.110:11885/path"; // Missing ://

        assertThatThrownBy(() -> new P2PURL(invalidUrl))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_ThrowException_When_MissingPortDelimiter() {
        String invalidUrl = "poker://192.211.1.110/11885/path"; // Missing : before port

        assertThatThrownBy(() -> new P2PURL(invalidUrl))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_ThrowException_When_MissingURIDelimiter() {
        String invalidUrl = "poker://192.211.1.110:11885"; // Missing / before URI

        assertThatThrownBy(() -> new P2PURL(invalidUrl))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_AllowEmptyProtocol_When_ProtocolDelimiterPresent() {
        // P2PURL doesn't validate for empty protocol, only checks delimiter presence
        String urlWithEmptyProtocol = "://host:123/path";
        P2PURL url = new P2PURL(urlWithEmptyProtocol);

        assertThat(url.getHost()).isEqualTo("host");
        assertThat(url.getPort()).isEqualTo(123);
    }

    // =================================================================
    // Invalid URL Tests - Invalid Port
    // =================================================================

    @Test
    void should_ThrowException_When_PortNotNumeric() {
        String invalidUrl = "poker://host:abc/path";

        assertThatThrownBy(() -> new P2PURL(invalidUrl))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_ThrowException_When_PortEmpty() {
        String invalidUrl = "poker://host:/path";

        assertThatThrownBy(() -> new P2PURL(invalidUrl))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_ThrowException_When_PortHasSpaces() {
        String invalidUrl = "poker://host:12 34/path";

        assertThatThrownBy(() -> new P2PURL(invalidUrl))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_ThrowException_When_PortMixedAlphanumeric() {
        String invalidUrl = "poker://host:123abc/path";

        assertThatThrownBy(() -> new P2PURL(invalidUrl))
                .isInstanceOf(ApplicationError.class);
    }

    // =================================================================
    // Invalid URL Tests - Malformed
    // =================================================================

    @Test
    void should_ThrowException_When_EmptyString() {
        assertThatThrownBy(() -> new P2PURL(""))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_ThrowException_When_OnlyProtocol() {
        String invalidUrl = "poker://";

        assertThatThrownBy(() -> new P2PURL(invalidUrl))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_AllowEmptyHost_When_HostDelimiterPresent() {
        // P2PURL doesn't validate for empty host, only checks delimiter presence
        String urlWithEmptyHost = "poker://:123/path";
        P2PURL url = new P2PURL(urlWithEmptyHost);

        assertThat(url.getHost()).isEmpty();
        assertThat(url.getPort()).isEqualTo(123);
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ReconstructURL_When_ToStringCalled() {
        String original = "poker://192.211.1.110:11885/n-2/WXZ-741";
        P2PURL url = new P2PURL(original);

        assertThat(url.toString()).isEqualTo(original);
    }

    @Test
    void should_PreserveAllParts_When_ToStringCalled() {
        String original = "myprotocol://myhost:9999/my/uri/path";
        P2PURL url = new P2PURL(original);

        String result = url.toString();
        assertThat(result).contains("myprotocol://");
        assertThat(result).contains("myhost");
        assertThat(result).contains(":9999");
        assertThat(result).contains("/my/uri/path");
    }

    @Test
    void should_HandleEmptyURI_When_ToStringCalled() {
        String original = "poker://host:123/";
        P2PURL url = new P2PURL(original);

        assertThat(url.toString()).isEqualTo(original);
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_ParseCorrectly_When_HostHasOnlyLetters() {
        String letterUrl = "poker://abcdefg:123/uri";
        P2PURL url = new P2PURL(letterUrl);

        assertThat(url.getHost()).isEqualTo("abcdefg");
    }

    @Test
    void should_ParseCorrectly_When_HostHasOnlyNumbers() {
        String numberUrl = "poker://123456:789/uri";
        P2PURL url = new P2PURL(numberUrl);

        assertThat(url.getHost()).isEqualTo("123456");
    }

    @Test
    void should_ParseCorrectly_When_URIHasSpecialCharacters() {
        String specialUrl = "poker://host:123/path?query=value&key=value#fragment";
        P2PURL url = new P2PURL(specialUrl);

        assertThat(url.getURI()).isEqualTo("path?query=value&key=value#fragment");
    }

    @Test
    void should_ParseCorrectly_When_ProtocolHasNumbers() {
        String protocolUrl = "http2://host:123/path";
        P2PURL url = new P2PURL(protocolUrl);

        assertThat(url.toString()).startsWith("http2://");
    }

    // =================================================================
    // Constant Tests
    // =================================================================

    @Test
    void should_HaveCorrectDelimiters_When_ConstantsChecked() {
        assertThat(P2PURL.PROTOCOL_DELIM).isEqualTo("://");
        assertThat(P2PURL.PORT_DELIM).isEqualTo(":");
        assertThat(P2PURL.URI_DELIM).isEqualTo("/");
    }
}
