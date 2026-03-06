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

import com.donohoedigital.comms.DDMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BaseServlet}.
 */
class BaseServletTest {

    // -----------------------------------------------------------------------
    // Minimal concrete subclass
    // -----------------------------------------------------------------------

    private static class TestServlet extends BaseServlet {
        @Override
        public DDMessage processMessage(HttpServletRequest request, HttpServletResponse response, DDMessage received)
                throws IOException {
            return null;
        }
    }

    private TestServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new TestServlet();
    }

    // -----------------------------------------------------------------------
    // Server getter/setter
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnNull_When_ServerNotSet() {
        assertThat(servlet.getServer()).isNull();
    }

    @Test
    void should_ReturnServer_When_ServerIsSet() {
        GameServer server = new GameServer() {
        };
        server.setAppName("test");

        servlet.setServer(server);

        assertThat(servlet.getServer()).isSameAs(server);
    }

    @Test
    void should_ThrowError_When_ServerSetTwice() {
        GameServer server1 = new GameServer() {
        };
        server1.setAppName("test1");
        GameServer server2 = new GameServer() {
        };
        server2.setAppName("test2");

        servlet.setServer(server1);

        assertThatThrownBy(() -> servlet.setServer(server2))
                .isInstanceOf(com.donohoedigital.base.ApplicationError.class);
    }

    // -----------------------------------------------------------------------
    // DDMessageHandler flag
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnFalse_When_DDMessageHandlerNotSet() {
        assertThat(servlet.isDDMessageHandler()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_DDMessageHandlerSetToTrue() {
        servlet.setDDMessageHandler(true);

        assertThat(servlet.isDDMessageHandler()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_DDMessageHandlerSetToFalse() {
        servlet.setDDMessageHandler(true);
        servlet.setDDMessageHandler(false);

        assertThat(servlet.isDDMessageHandler()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Debug flag
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnFalse_When_IsDebugOnCalled() {
        assertThat(servlet.isDebugOn()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Message creation
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnNewDDMessage_When_CreateNewMessageCalled() {
        DDMessage message = servlet.createNewMessage();

        assertThat(message).isNotNull();
        assertThat(message).isInstanceOf(DDMessage.class);
    }

    @Test
    void should_ReturnDistinctInstances_When_CreateNewMessageCalledMultipleTimes() {
        DDMessage message1 = servlet.createNewMessage();
        DDMessage message2 = servlet.createNewMessage();

        assertThat(message1).isNotSameAs(message2);
    }

    // -----------------------------------------------------------------------
    // ThreadLocal servlet context
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnNull_When_ThreadServletContextNotSet() {
        assertThat(BaseServlet.getThreadServletContext()).isNull();
    }

    @Test
    void should_ReturnContext_When_ThreadServletContextIsSet() {
        try {
            BaseServlet.setThreadServletContext(null);
            assertThat(BaseServlet.getThreadServletContext()).isNull();
        } finally {
            BaseServlet.setThreadServletContext(null);
        }
    }

    // -----------------------------------------------------------------------
    // afterConfigInit is a no-op
    // -----------------------------------------------------------------------

    @Test
    void should_NotThrow_When_AfterConfigInitCalled() {
        servlet.afterConfigInit();
    }
}
