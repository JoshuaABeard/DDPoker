/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Community Contributors
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
package com.donohoedigital.server;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ServletDebug}.
 */
@ExtendWith(MockitoExtension.class)
class ServletDebugTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    private boolean originalVerbose;
    private boolean originalDebug;

    @BeforeEach
    void setUp() {
        originalVerbose = ServletDebug.bVerbose_;
        originalDebug = ServletDebug.bDebug_;
    }

    @AfterEach
    void tearDown() {
        ServletDebug.bVerbose_ = originalVerbose;
        ServletDebug.bDebug_ = originalDebug;
    }

    // -----------------------------------------------------------------------
    // setVerbose
    // -----------------------------------------------------------------------

    @Test
    void should_SetVerboseTrue_When_SetVerboseCalledWithTrue() {
        ServletDebug.setVerbose(true);

        assertThat(ServletDebug.bVerbose_).isTrue();
    }

    @Test
    void should_SetVerboseFalse_When_SetVerboseCalledWithFalse() {
        ServletDebug.setVerbose(false);

        assertThat(ServletDebug.bVerbose_).isFalse();
    }

    // -----------------------------------------------------------------------
    // constructURL
    // -----------------------------------------------------------------------

    @Test
    void should_BuildFullURL_When_ConstructURLCalledWithMockedRequest() {
        when(mockRequest.getScheme()).thenReturn("http");
        when(mockRequest.getServerName()).thenReturn("localhost");
        when(mockRequest.getServerPort()).thenReturn(8080);
        when(mockRequest.getRequestURI()).thenReturn("/game/servlet");
        when(mockRequest.getParameterNames()).thenReturn(Collections.emptyEnumeration());

        String url = ServletDebug.constructURL(mockRequest);

        assertThat(url).isEqualTo("http://localhost:8080/game/servlet");
    }

    @Test
    void should_AppendParams_When_ConstructURLCalledWithParameters() {
        when(mockRequest.getScheme()).thenReturn("https");
        when(mockRequest.getServerName()).thenReturn("example.com");
        when(mockRequest.getServerPort()).thenReturn(443);
        when(mockRequest.getRequestURI()).thenReturn("/api");
        when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(Collections.singletonList("key")));
        when(mockRequest.getParameter("key")).thenReturn("value");

        String url = ServletDebug.constructURL(mockRequest);

        assertThat(url).isEqualTo("https://example.com:443/api?key=value");
    }

    @Test
    void should_ReplaceNewlines_When_URLContainsNewlines() {
        when(mockRequest.getScheme()).thenReturn("http");
        when(mockRequest.getServerName()).thenReturn("localhost");
        when(mockRequest.getServerPort()).thenReturn(80);
        when(mockRequest.getRequestURI()).thenReturn("/path\nwith\rnewlines");
        when(mockRequest.getParameterNames()).thenReturn(Collections.emptyEnumeration());

        String url = ServletDebug.constructURL(mockRequest);

        assertThat(url).doesNotContain("\n").doesNotContain("\r");
        assertThat(url).contains("/path with newlines");
    }

    // -----------------------------------------------------------------------
    // getParams
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnEmptyString_When_NoParameterNames() {
        when(mockRequest.getParameterNames()).thenReturn(Collections.emptyEnumeration());

        String params = ServletDebug.getParams(mockRequest, false);

        assertThat(params).isEmpty();
    }

    @Test
    void should_ReturnNullParameterNames_When_NullEnumeration() {
        when(mockRequest.getParameterNames()).thenReturn(null);

        String params = ServletDebug.getParams(mockRequest, false);

        assertThat(params).isEmpty();
    }

    @Test
    void should_JoinParamsWithAmpersand_When_MultipleParametersExist() {
        when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(java.util.List.of("a", "b")));
        when(mockRequest.getParameter("a")).thenReturn("1");
        when(mockRequest.getParameter("b")).thenReturn("2");

        String params = ServletDebug.getParams(mockRequest, false);

        assertThat(params).contains("a=1").contains("b=2").contains("&");
    }

    @Test
    void should_SkipSessionIds_When_IgnoreSessionIdsIsTrue() {
        when(mockRequest.getParameterNames())
                .thenReturn(Collections.enumeration(java.util.List.of("sessionID", "RequisiteSession", "key")));
        when(mockRequest.getParameter("key")).thenReturn("val");

        String params = ServletDebug.getParams(mockRequest, true);

        assertThat(params).isEqualTo("key=val");
        assertThat(params).doesNotContain("sessionID").doesNotContain("RequisiteSession");
    }

    @Test
    void should_IncludeSessionIds_When_IgnoreSessionIdsIsFalse() {
        when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(java.util.List.of("sessionID")));
        when(mockRequest.getParameter("sessionID")).thenReturn("abc");

        String params = ServletDebug.getParams(mockRequest, false);

        assertThat(params).isEqualTo("sessionID=abc");
    }

    // -----------------------------------------------------------------------
    // printHeaders / printCookies - should not throw
    // -----------------------------------------------------------------------

    @Test
    void should_NotThrow_When_PrintHeadersCalledWithNoHeaders() {
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        ServletDebug.printHeaders(mockRequest);
    }

    @Test
    void should_NotThrow_When_PrintHeadersCalledWithNullEnumeration() {
        when(mockRequest.getHeaderNames()).thenReturn(null);

        ServletDebug.printHeaders(mockRequest);
    }

    @Test
    void should_NotThrow_When_PrintHeadersCalledWithHeaders() {
        when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(java.util.List.of("Host")));
        when(mockRequest.getHeader("Host")).thenReturn("localhost");

        ServletDebug.printHeaders(mockRequest);
    }

    @Test
    void should_NotThrow_When_PrintCookiesCalledWithNullCookies() {
        when(mockRequest.getCookies()).thenReturn(null);

        ServletDebug.printCookies(mockRequest);
    }

    @Test
    void should_NotThrow_When_PrintCookiesCalledWithCookies() {
        Cookie cookie = new Cookie("name", "value");
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{cookie});

        ServletDebug.printCookies(mockRequest);
    }

    // -----------------------------------------------------------------------
    // debugGet - should not throw
    // -----------------------------------------------------------------------

    @Test
    void should_NotThrow_When_DebugGetCalledWithDebugOff() {
        ServletDebug.bDebug_ = false;

        ServletDebug.debugGet(mockRequest, mockResponse);
    }

    @Test
    void should_NotThrow_When_DebugGetCalledWithVerboseOff() {
        ServletDebug.bDebug_ = true;
        ServletDebug.bVerbose_ = false;
        when(mockRequest.getAttribute("_trh_")).thenReturn(null);
        when(mockRequest.getScheme()).thenReturn("http");
        when(mockRequest.getServerName()).thenReturn("localhost");
        when(mockRequest.getServerPort()).thenReturn(80);
        when(mockRequest.getRequestURI()).thenReturn("/test");
        when(mockRequest.getParameterNames()).thenReturn(Collections.emptyEnumeration());

        ServletDebug.debugGet(mockRequest, mockResponse);
    }

    @Test
    void should_SkipProcessing_When_RequestAlreadyHandled() {
        ServletDebug.bDebug_ = true;
        when(mockRequest.getAttribute("_trh_")).thenReturn(Boolean.TRUE);

        ServletDebug.debugGet(mockRequest, mockResponse);
    }

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    @Test
    void should_HaveExpectedDelimiterConstants() {
        assertThat(ServletDebug.POST_DELIMITER).isEqualTo("--POST--");
        assertThat(ServletDebug.CONTENT_TYPE_DELIMITER).isEqualTo("--CONTENT-TYPE--");
    }
}
