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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link GameServletResponse}.
 */
@ExtendWith(MockitoExtension.class)
class GameServletResponseTest {

    @Mock
    private SocketChannel mockChannel;

    private GameServletResponse response;

    @BeforeEach
    void setUp() {
        response = new GameServletResponse(mockChannel);
    }

    // -----------------------------------------------------------------------
    // Content type
    // -----------------------------------------------------------------------

    @Test
    void should_StoreContentType_When_SetContentTypeCalled() {
        response.setContentType("application/json");

        assertThat(response.sContentType_).isEqualTo("application/json");
    }

    @Test
    void should_ReturnNullContentType_When_GetContentTypeCalled() {
        // getContentType() logs a warning and returns null (it reads from a different
        // path)
        assertThat(response.getContentType()).isNull();
    }

    @Test
    void should_HaveNullContentType_When_NotSet() {
        assertThat(response.sContentType_).isNull();
    }

    // -----------------------------------------------------------------------
    // Unsupported methods throw IOException
    // -----------------------------------------------------------------------

    @Test
    void should_ThrowIOException_When_FlushBufferCalled() {
        assertThatThrownBy(() -> response.flushBuffer()).isInstanceOf(IOException.class).hasMessage("Unsupported");
    }

    @Test
    void should_ThrowIOException_When_GetOutputStreamCalled() {
        assertThatThrownBy(() -> response.getOutputStream()).isInstanceOf(IOException.class).hasMessage("Unsupported");
    }

    // -----------------------------------------------------------------------
    // Default return values
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnZero_When_GetBufferSizeCalled() {
        assertThat(response.getBufferSize()).isEqualTo(0);
    }

    @Test
    void should_ReturnNull_When_GetCharacterEncodingCalled() {
        assertThat(response.getCharacterEncoding()).isNull();
    }

    @Test
    void should_ReturnLocaleUS_When_GetLocaleCalled() {
        assertThat(response.getLocale()).isEqualTo(Locale.US);
    }

    @Test
    void should_ReturnNull_When_GetWriterCalled() throws IOException {
        assertThat(response.getWriter()).isNull();
    }

    @Test
    void should_ReturnFalse_When_IsCommittedCalled() {
        assertThat(response.isCommitted()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_ContainsHeaderCalled() {
        assertThat(response.containsHeader("X-Test")).isFalse();
    }

    @Test
    void should_ReturnZero_When_GetStatusCalled() {
        assertThat(response.getStatus()).isEqualTo(0);
    }

    @Test
    void should_ReturnEmptyString_When_GetHeaderCalled() {
        assertThat(response.getHeader("X-Test")).isEmpty();
    }

    @Test
    void should_ReturnEmptyCollection_When_GetHeadersCalled() {
        assertThat(response.getHeaders("X-Test")).isEmpty();
    }

    @Test
    void should_ReturnEmptyCollection_When_GetHeaderNamesCalled() {
        assertThat(response.getHeaderNames()).isEqualTo(Collections.emptyList());
    }

    // -----------------------------------------------------------------------
    // Encode methods return input
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnInput_When_EncodeRedirectURLCalled() {
        assertThat(response.encodeRedirectURL("http://test")).isEqualTo("http://test");
    }

    @Test
    void should_ReturnInput_When_EncodeRedirectUrlCalled() {
        assertThat(response.encodeRedirectUrl("http://test")).isEqualTo("http://test");
    }

    @Test
    void should_ReturnInput_When_EncodeURLCalled() {
        assertThat(response.encodeURL("http://test")).isEqualTo("http://test");
    }

    @Test
    void should_ReturnInput_When_EncodeUrlCalled() {
        assertThat(response.encodeUrl("http://test")).isEqualTo("http://test");
    }

    // -----------------------------------------------------------------------
    // Void methods should not throw
    // -----------------------------------------------------------------------

    @Test
    void should_NotThrow_When_ResetCalled() {
        response.reset();
    }

    @Test
    void should_NotThrow_When_ResetBufferCalled() {
        response.resetBuffer();
    }

    @Test
    void should_NotThrow_When_SetBufferSizeCalled() {
        response.setBufferSize(1024);
    }

    @Test
    void should_NotThrow_When_SetContentLengthCalled() {
        response.setContentLength(100);
    }

    @Test
    void should_NotThrow_When_SetLocaleCalled() {
        response.setLocale(Locale.US);
    }

    @Test
    void should_NotThrow_When_AddCookieCalled() {
        response.addCookie(null);
    }

    @Test
    void should_NotThrow_When_AddDateHeaderCalled() {
        response.addDateHeader("Date", System.currentTimeMillis());
    }

    @Test
    void should_NotThrow_When_AddHeaderCalled() {
        response.addHeader("X-Test", "value");
    }

    @Test
    void should_NotThrow_When_AddIntHeaderCalled() {
        response.addIntHeader("X-Count", 5);
    }

    @Test
    void should_NotThrow_When_SendRedirectCalled() throws IOException {
        response.sendRedirect("http://redirect");
    }

    @Test
    void should_NotThrow_When_SetDateHeaderCalled() {
        response.setDateHeader("Date", System.currentTimeMillis());
    }

    @Test
    void should_NotThrow_When_SetHeaderCalled() {
        response.setHeader("X-Test", "value");
    }

    @Test
    void should_NotThrow_When_SetIntHeaderCalled() {
        response.setIntHeader("X-Count", 5);
    }

    @Test
    void should_NotThrow_When_SetStatusCalled() {
        response.setStatus(200);
    }

    @Test
    void should_NotThrow_When_SetStatusWithMessageCalled() {
        response.setStatus(200, "OK");
    }

    @Test
    void should_NotThrow_When_SetCharacterEncodingCalled() {
        response.setCharacterEncoding("UTF-8");
    }

    @Test
    void should_NotThrow_When_SetContentLengthLongCalled() {
        response.setContentLengthLong(100L);
    }

    @Test
    void should_NotThrow_When_SendRedirectWithClearBufferCalled() throws IOException {
        response.sendRedirect("http://test", 302, false);
    }
}
