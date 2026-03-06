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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link GameServletRequest}.
 */
class GameServletRequestTest {

    private GameServletRequest request;

    @BeforeEach
    void setUp() {
        request = new GameServletRequest();
    }

    // -----------------------------------------------------------------------
    // initRequest and field getters
    // -----------------------------------------------------------------------

    @Test
    void should_StoreAllFields_When_InitRequestCalled() {
        request.initRequest("POST", "/game/servlet", "HTTP/1.1", 42, "application/octet-stream", "10.0.0.1", 8080);

        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getRequestURI()).isEqualTo("/game/servlet");
        assertThat(request.getHTTPVersion()).isEqualTo("HTTP/1.1");
        assertThat(request.getContentLength()).isEqualTo(42);
        assertThat(request.getContentType()).isEqualTo("application/octet-stream");
        assertThat(request.getRemoteAddr()).isEqualTo("10.0.0.1");
        assertThat(request.getServerPort()).isEqualTo(8080);
    }

    @Test
    void should_ReturnNullFields_When_NotInitialized() {
        assertThat(request.getMethod()).isNull();
        assertThat(request.getRequestURI()).isNull();
        assertThat(request.getHTTPVersion()).isNull();
        assertThat(request.getContentType()).isNull();
        assertThat(request.getRemoteAddr()).isNull();
        assertThat(request.getContentLength()).isEqualTo(0);
        assertThat(request.getServerPort()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Header management
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnHeaderValue_When_HeaderIsSet() {
        request.setHeader("Content-Type", "text/html");

        assertThat(request.getHeader("Content-Type")).isEqualTo("text/html");
    }

    @Test
    void should_ReturnNull_When_HeaderNotSet() {
        assertThat(request.getHeader("X-Missing")).isNull();
    }

    @Test
    void should_BeCaseInsensitive_When_LookingUpHeaders() {
        request.setHeader("Content-Type", "text/html");

        assertThat(request.getHeader("content-type")).isEqualTo("text/html");
        assertThat(request.getHeader("CONTENT-TYPE")).isEqualTo("text/html");
    }

    @Test
    void should_ReturnAllHeaders_When_GetHeaderNamesCalled() {
        request.setHeader("Host", "localhost");
        request.setHeader("Accept", "text/html");

        Enumeration names = request.getHeaderNames();
        assertThat(names).isNotNull();
        assertThat(names.hasMoreElements()).isTrue();
    }

    @Test
    void should_OverwriteHeader_When_SameHeaderSetTwice() {
        request.setHeader("Host", "first");
        request.setHeader("Host", "second");

        assertThat(request.getHeader("Host")).isEqualTo("second");
    }

    // -----------------------------------------------------------------------
    // Input stream management
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnInputStream_When_SetViaSetInputStream2() {
        byte[] data = "test data".getBytes();
        InputStream in = new ByteArrayInputStream(data);

        request.setInputStream2(in);

        assertThat(request.getInputStream2()).isSameAs(in);
    }

    @Test
    void should_ReturnNull_When_InputStreamNotSet() {
        assertThat(request.getInputStream2()).isNull();
    }

    @Test
    void should_ThrowIOException_When_GetInputStreamCalled() {
        assertThatThrownBy(() -> request.getInputStream()).isInstanceOf(IOException.class).hasMessage("unsupported");
    }

    // -----------------------------------------------------------------------
    // Unimplemented methods return sensible defaults
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnNullDefaults_When_UnimplementedStringMethodsCalled() {
        assertThat(request.getAttribute("key")).isNull();
        assertThat(request.getAttributeNames()).isNull();
        assertThat(request.getAuthType()).isNull();
        assertThat(request.getCharacterEncoding()).isNull();
        assertThat(request.getContextPath()).isNull();
        assertThat(request.getPathInfo()).isNull();
        assertThat(request.getPathTranslated()).isNull();
        assertThat(request.getProtocol()).isNull();
        assertThat(request.getQueryString()).isNull();
        assertThat(request.getRealPath("test")).isNull();
        assertThat(request.getRemoteHost()).isNull();
        assertThat(request.getRemoteUser()).isNull();
        assertThat(request.getRequestDispatcher("test")).isNull();
        assertThat(request.getRequestURL()).isNull();
        assertThat(request.getRequestedSessionId()).isNull();
        assertThat(request.getScheme()).isNull();
        assertThat(request.getServerName()).isNull();
        assertThat(request.getServletPath()).isNull();
        assertThat(request.getSession()).isNull();
        assertThat(request.getSession(false)).isNull();
        assertThat(request.getUserPrincipal()).isNull();
        assertThat(request.getParameter("test")).isNull();
        assertThat(request.getParameterMap()).isNull();
        assertThat(request.getParameterNames()).isNull();
        assertThat(request.getParameterValues("test")).isNull();
        assertThat(request.getLocales()).isNull();
    }

    @Test
    void should_ReturnFalseDefaults_When_UnimplementedBooleanMethodsCalled() {
        assertThat(request.isRequestedSessionIdFromCookie()).isFalse();
        assertThat(request.isRequestedSessionIdFromURL()).isFalse();
        assertThat(request.isRequestedSessionIdFromUrl()).isFalse();
        assertThat(request.isRequestedSessionIdValid()).isFalse();
        assertThat(request.isSecure()).isFalse();
        assertThat(request.isUserInRole("admin")).isFalse();
    }

    @Test
    void should_ReturnZeroDefaults_When_UnimplementedIntMethodsCalled() {
        assertThat(request.getDateHeader("date")).isEqualTo(0);
        assertThat(request.getIntHeader("num")).isEqualTo(0);
        assertThat(request.getLocalPort()).isEqualTo(0);
        assertThat(request.getRemotePort()).isEqualTo(0);
    }

    @Test
    void should_ReturnLocaleUS_When_GetLocaleCalled() {
        assertThat(request.getLocale()).isEqualTo(Locale.US);
    }

    @Test
    void should_ReturnNull_When_GetCookiesCalled() {
        assertThat(request.getCookies()).isNull();
    }

    @Test
    void should_ThrowIOException_When_GetReaderCalled() {
        assertThatThrownBy(() -> request.getReader()).isInstanceOf(IOException.class).hasMessage("unsupported");
    }

    // -----------------------------------------------------------------------
    // Servlet API 3.1+ additions
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnEmptyString_When_ChangeSessionIdCalled() {
        assertThat(request.changeSessionId()).isEmpty();
    }

    @Test
    void should_ReturnFalse_When_AuthenticateCalled() throws Exception {
        assertThat(request.authenticate(null)).isFalse();
    }

    @Test
    void should_ReturnEmptyParts_When_GetPartsCalled() throws Exception {
        assertThat(request.getParts()).isEmpty();
    }

    @Test
    void should_ReturnNull_When_GetPartCalled() throws Exception {
        assertThat(request.getPart("name")).isNull();
    }

    @Test
    void should_ReturnNull_When_UpgradeCalled() throws Exception {
        Object result = request.upgrade(null);
        assertThat(result).isNull();
    }

    @Test
    void should_ReturnZero_When_GetContentLengthLongCalled() {
        assertThat(request.getContentLengthLong()).isEqualTo(0);
    }

    @Test
    void should_ReturnNull_When_GetServletContextCalled() {
        assertThat(request.getServletContext()).isNull();
    }

    @Test
    void should_ReturnFalse_When_IsAsyncStartedCalled() {
        assertThat(request.isAsyncStarted()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_IsAsyncSupportedCalled() {
        assertThat(request.isAsyncSupported()).isFalse();
    }

    @Test
    void should_ReturnNull_When_GetAsyncContextCalled() {
        assertThat(request.getAsyncContext()).isNull();
    }

    @Test
    void should_ReturnNull_When_GetDispatcherTypeCalled() {
        assertThat(request.getDispatcherType()).isNull();
    }

    @Test
    void should_ReturnEmptyString_When_GetRequestIdCalled() {
        assertThat(request.getRequestId()).isEmpty();
    }

    @Test
    void should_ReturnEmptyString_When_GetProtocolRequestIdCalled() {
        assertThat(request.getProtocolRequestId()).isEmpty();
    }

    @Test
    void should_ReturnNull_When_GetServletConnectionCalled() {
        assertThat(request.getServletConnection()).isNull();
    }

    // -----------------------------------------------------------------------
    // Void methods do not throw
    // -----------------------------------------------------------------------

    @Test
    void should_NotThrow_When_RemoveAttributeCalled() {
        request.removeAttribute("key");
    }

    @Test
    void should_NotThrow_When_SetAttributeCalled() {
        request.setAttribute("key", "value");
    }

    @Test
    void should_NotThrow_When_SetCharacterEncodingCalled() throws Exception {
        request.setCharacterEncoding("UTF-8");
    }

    @Test
    void should_NotThrow_When_LoginCalled() throws Exception {
        request.login("user", "pass");
    }

    @Test
    void should_NotThrow_When_LogoutCalled() throws Exception {
        request.logout();
    }

    @Test
    void should_ReturnNull_When_StartAsyncCalled() {
        assertThat(request.startAsync()).isNull();
    }

    @Test
    void should_ReturnNull_When_StartAsyncWithArgsCalled() {
        assertThat(request.startAsync(null, null)).isNull();
    }
}
