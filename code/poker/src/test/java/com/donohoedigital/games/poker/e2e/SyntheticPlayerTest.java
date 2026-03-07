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
package com.donohoedigital.games.poker.e2e;

import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.online.WebSocketGameClient;
import com.donohoedigital.games.poker.protocol.message.ServerMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SyntheticPlayerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    class Construction {
        @Test
        void createForTest_setsFieldsCorrectly() {
            RestAuthClient authClient = mock(RestAuthClient.class);
            WebSocketGameClient wsClient = mock(WebSocketGameClient.class);

            SyntheticPlayer player = SyntheticPlayer.createForTest("http://localhost:9999", "testuser", authClient,
                    wsClient);

            assertThat(player.getUsername()).isEqualTo("testuser");
        }

        @Test
        void isConnected_delegatesToWsClient() {
            WebSocketGameClient wsClient = mock(WebSocketGameClient.class);
            when(wsClient.isConnected()).thenReturn(true);

            SyntheticPlayer player = SyntheticPlayer.createForTest("http://localhost:9999", "testuser",
                    mock(RestAuthClient.class), wsClient);

            assertThat(player.isConnected()).isTrue();
        }

        @Test
        void close_disconnectsWsClient() {
            WebSocketGameClient wsClient = mock(WebSocketGameClient.class);
            SyntheticPlayer player = SyntheticPlayer.createForTest("http://localhost:9999", "testuser",
                    mock(RestAuthClient.class), wsClient);

            player.close();

            verify(wsClient).disconnect();
        }
    }

    @Nested
    class ActionStrategy {
        @Test
        void choosesCheck_whenCanCheck() {
            ObjectNode options = MAPPER.createObjectNode().put("canCheck", true).put("canCall", true).put("canFold",
                    true);

            assertThat(SyntheticPlayer.chooseAction(options)).isEqualTo("CHECK");
        }

        @Test
        void choosesCall_whenCannotCheckButCanCall() {
            ObjectNode options = MAPPER.createObjectNode().put("canCheck", false).put("canCall", true)
                    .put("callAmount", 200).put("canFold", true);

            assertThat(SyntheticPlayer.chooseAction(options)).isEqualTo("CALL");
        }

        @Test
        void choosesFold_whenCannotCheckOrCall() {
            ObjectNode options = MAPPER.createObjectNode().put("canCheck", false).put("canCall", false).put("canFold",
                    true);

            assertThat(SyntheticPlayer.chooseAction(options)).isEqualTo("FOLD");
        }

        @Test
        void choosesFold_asDefaultFallback() {
            ObjectNode options = MAPPER.createObjectNode();

            assertThat(SyntheticPlayer.chooseAction(options)).isEqualTo("FOLD");
        }

        @Test
        void checkAmount_isZero() {
            ObjectNode options = MAPPER.createObjectNode().put("canCheck", true);

            assertThat(SyntheticPlayer.chooseAmount("CHECK", options)).isEqualTo(0);
        }

        @Test
        void callAmount_readsFromOptions() {
            ObjectNode options = MAPPER.createObjectNode().put("callAmount", 150);

            assertThat(SyntheticPlayer.chooseAmount("CALL", options)).isEqualTo(150);
        }

        @Test
        void foldAmount_isZero() {
            ObjectNode options = MAPPER.createObjectNode();

            assertThat(SyntheticPlayer.chooseAmount("FOLD", options)).isEqualTo(0);
        }
    }

    @Nested
    class MessageHandling {
        private SyntheticPlayer player;
        private WebSocketGameClient wsClient;

        @BeforeEach
        void setUp() {
            wsClient = mock(WebSocketGameClient.class);
            player = SyntheticPlayer.createForTest("http://localhost:9999", "testuser", mock(RestAuthClient.class),
                    wsClient);
        }

        @Test
        void actionRequired_sendsAutoAction() {
            ObjectNode options = MAPPER.createObjectNode().put("canCheck", true).put("canCall", false).put("canFold",
                    true);
            ObjectNode data = MAPPER.createObjectNode().put("timeoutSeconds", 30);
            data.set("options", options);

            player.handleActionRequired(data);

            verify(wsClient).sendAction("CHECK", 0);
        }

        @Test
        void actionRequired_callWithAmount() {
            ObjectNode options = MAPPER.createObjectNode().put("canCheck", false).put("canCall", true)
                    .put("callAmount", 100).put("canFold", true);
            ObjectNode data = MAPPER.createObjectNode().put("timeoutSeconds", 30);
            data.set("options", options);

            player.handleActionRequired(data);

            verify(wsClient).sendAction("CALL", 100);
        }

        @Test
        void actionRequired_noOptions_doesNotSend() {
            ObjectNode data = MAPPER.createObjectNode().put("timeoutSeconds", 30);

            player.handleActionRequired(data);

            verify(wsClient, never()).sendAction(anyString(), anyInt());
        }

        @Test
        void connectedMessage_storesReconnectToken() {
            ObjectNode data = MAPPER.createObjectNode().put("reconnectToken", "tok123");
            var msg = new WebSocketGameClient.InboundMessage(ServerMessageType.CONNECTED, "game1", data, null);

            player.handleMessage(msg);

            verify(wsClient).setReconnectToken("tok123");
        }

        @Test
        void connectedMessage_noToken_doesNotSetReconnectToken() {
            ObjectNode data = MAPPER.createObjectNode();
            var msg = new WebSocketGameClient.InboundMessage(ServerMessageType.CONNECTED, "game1", data, null);

            player.handleMessage(msg);

            verify(wsClient, never()).setReconnectToken(anyString());
        }

        @Test
        void nonActionMessage_ignored() {
            ObjectNode data = MAPPER.createObjectNode();
            var msg = new WebSocketGameClient.InboundMessage(ServerMessageType.PLAYER_ACTED, "game1", data, 1L);

            player.handleMessage(msg);

            verify(wsClient, never()).sendAction(anyString(), anyInt());
        }
    }
}
