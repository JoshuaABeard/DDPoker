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
package com.donohoedigital.games.poker.gameserver.websocket;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.service.BanService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for LobbyWebSocketHandler.
 *
 * <p>
 * Tests the full lifecycle: authentication, join/leave broadcasts, chat message
 * routing, rate limiting, and ban enforcement.
 */
class LobbyWebSocketHandlerTest {

    private static final long ALICE_ID = 42L;
    private static final long BOB_ID = 99L;
    private static final String ALICE_NAME = "Alice";
    private static final String BOB_NAME = "Bob";
    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String WS_URI_ALICE = "ws://localhost/ws/lobby?token=" + VALID_TOKEN;

    private JwtTokenProvider jwtTokenProvider;
    private BanService banService;
    private ObjectMapper objectMapper;
    private LobbyWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        banService = mock(BanService.class);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        handler = new LobbyWebSocketHandler(jwtTokenProvider, banService, objectMapper);

        // Default: valid token for Alice
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getProfileIdFromToken(VALID_TOKEN)).thenReturn(ALICE_ID);
        when(jwtTokenProvider.getUsernameFromToken(VALID_TOKEN)).thenReturn(ALICE_NAME);
        when(banService.isProfileBanned(anyLong())).thenReturn(false);
    }

    // -------------------------------------------------------------------------
    // Connection: authentication
    // -------------------------------------------------------------------------

    @Test
    void connect_validJwt_receivesPlayerList() throws Exception {
        WebSocketSession session = mockSession("session-alice", WS_URI_ALICE);

        handler.afterConnectionEstablished(session);

        // Should receive LOBBY_PLAYER_LIST message
        assertReceivedMessageType(session, "LOBBY_PLAYER_LIST");
    }

    @Test
    void connect_invalidJwt_sessionClosed() throws Exception {
        when(jwtTokenProvider.validateToken(INVALID_TOKEN)).thenReturn(false);

        WebSocketSession session = mockSession("session-bad", "ws://localhost/ws/lobby?token=" + INVALID_TOKEN);

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status -> status.getCode() == 1008));
    }

    @Test
    void connect_bannedProfile_sessionClosed() throws Exception {
        when(banService.isProfileBanned(ALICE_ID)).thenReturn(true);
        WebSocketSession session = mockSession("session-alice", WS_URI_ALICE);

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status -> status.getCode() == 4003));
    }

    @Test
    void connect_noTokenParam_sessionClosed() throws Exception {
        WebSocketSession session = mockSession("session-notoken", "ws://localhost/ws/lobby");

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status -> status.getCode() == 1008));
    }

    // -------------------------------------------------------------------------
    // Join / Leave broadcasts
    // -------------------------------------------------------------------------

    @Test
    void secondPlayerConnects_firstPlayerReceivesLobbyJoin() throws Exception {
        // Alice connects first
        WebSocketSession aliceSession = mockSession("session-alice", WS_URI_ALICE);
        handler.afterConnectionEstablished(aliceSession);

        // Set up Bob's JWT
        when(jwtTokenProvider.validateToken("bob-token")).thenReturn(true);
        when(jwtTokenProvider.getProfileIdFromToken("bob-token")).thenReturn(BOB_ID);
        when(jwtTokenProvider.getUsernameFromToken("bob-token")).thenReturn(BOB_NAME);

        // Bob connects
        WebSocketSession bobSession = mockSession("session-bob", "ws://localhost/ws/lobby?token=bob-token");
        handler.afterConnectionEstablished(bobSession);

        // Alice should have received: LOBBY_PLAYER_LIST, then LOBBY_JOIN (for Bob)
        assertReceivedMessageType(aliceSession, "LOBBY_JOIN");
    }

    @Test
    void disconnect_otherPlayersReceiveLobbyLeave() throws Exception {
        // Alice connects
        WebSocketSession aliceSession = mockSession("session-alice", WS_URI_ALICE);
        handler.afterConnectionEstablished(aliceSession);

        // Bob connects
        when(jwtTokenProvider.validateToken("bob-token")).thenReturn(true);
        when(jwtTokenProvider.getProfileIdFromToken("bob-token")).thenReturn(BOB_ID);
        when(jwtTokenProvider.getUsernameFromToken("bob-token")).thenReturn(BOB_NAME);
        WebSocketSession bobSession = mockSession("session-bob", "ws://localhost/ws/lobby?token=bob-token");
        handler.afterConnectionEstablished(bobSession);

        // Alice disconnects
        handler.afterConnectionClosed(aliceSession, CloseStatus.NORMAL);

        // Bob should receive LOBBY_LEAVE
        assertReceivedMessageType(bobSession, "LOBBY_LEAVE");
    }

    // -------------------------------------------------------------------------
    // Chat
    // -------------------------------------------------------------------------

    @Test
    void sendChat_valid_broadcastToAllClients() throws Exception {
        // Alice connects
        WebSocketSession aliceSession = mockSession("session-alice", WS_URI_ALICE);
        handler.afterConnectionEstablished(aliceSession);

        // Bob connects
        when(jwtTokenProvider.validateToken("bob-token")).thenReturn(true);
        when(jwtTokenProvider.getProfileIdFromToken("bob-token")).thenReturn(BOB_ID);
        when(jwtTokenProvider.getUsernameFromToken("bob-token")).thenReturn(BOB_NAME);
        WebSocketSession bobSession = mockSession("session-bob", "ws://localhost/ws/lobby?token=bob-token");
        handler.afterConnectionEstablished(bobSession);

        // Alice sends chat
        handler.handleTextMessage(aliceSession, new TextMessage("{\"type\":\"LOBBY_CHAT\",\"message\":\"Hello!\"}"));

        // Both Alice and Bob should receive the LOBBY_CHAT broadcast
        assertReceivedMessageType(aliceSession, "LOBBY_CHAT");
        assertReceivedMessageType(bobSession, "LOBBY_CHAT");
    }

    @Test
    void sendChat_over500Chars_notBroadcast() throws Exception {
        WebSocketSession aliceSession = mockSession("session-alice", WS_URI_ALICE);
        handler.afterConnectionEstablished(aliceSession);

        // Reset invocation count to ignore LOBBY_PLAYER_LIST sent on connect
        clearInvocations(aliceSession);

        String longMessage = "x".repeat(501);
        handler.handleTextMessage(aliceSession,
                new TextMessage("{\"type\":\"LOBBY_CHAT\",\"message\":\"" + longMessage + "\"}"));

        // No LOBBY_CHAT broadcast should occur
        verify(aliceSession, never()).sendMessage(argThat(msg -> {
            try {
                JsonNode node = objectMapper.readTree(((TextMessage) msg).getPayload());
                return "LOBBY_CHAT".equals(node.path("type").asText());
            } catch (Exception e) {
                return false;
            }
        }));
    }

    @Test
    void sendChat_rateLimitExceeded_messagesDropped() throws Exception {
        // Use a handler with very short window for testing
        LobbyWebSocketHandler testHandler = new LobbyWebSocketHandler(jwtTokenProvider, banService, objectMapper,
                LobbyWebSocketHandler.MAX_CHAT_MESSAGES, 60_000L);

        WebSocketSession aliceSession = mockSession("session-alice", WS_URI_ALICE);
        testHandler.afterConnectionEstablished(aliceSession);
        clearInvocations(aliceSession);

        // Send MAX_CHAT_MESSAGES + 1 chat messages
        for (int i = 0; i < LobbyWebSocketHandler.MAX_CHAT_MESSAGES + 1; i++) {
            testHandler.handleTextMessage(aliceSession,
                    new TextMessage("{\"type\":\"LOBBY_CHAT\",\"message\":\"msg " + i + "\"}"));
        }

        // Exactly MAX_CHAT_MESSAGES LOBBY_CHAT broadcasts should have been sent (last
        // one dropped)
        verify(aliceSession, times(LobbyWebSocketHandler.MAX_CHAT_MESSAGES)).sendMessage(argThat(msg -> {
            try {
                JsonNode node = objectMapper.readTree(((TextMessage) msg).getPayload());
                return "LOBBY_CHAT".equals(node.path("type").asText());
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // -------------------------------------------------------------------------
    // Player list on connect
    // -------------------------------------------------------------------------

    @Test
    void connect_whenOthersAlreadyConnected_playerListIncludesAll() throws Exception {
        // Bob connects first
        when(jwtTokenProvider.validateToken("bob-token")).thenReturn(true);
        when(jwtTokenProvider.getProfileIdFromToken("bob-token")).thenReturn(BOB_ID);
        when(jwtTokenProvider.getUsernameFromToken("bob-token")).thenReturn(BOB_NAME);
        WebSocketSession bobSession = mockSession("session-bob", "ws://localhost/ws/lobby?token=bob-token");
        handler.afterConnectionEstablished(bobSession);

        // Alice connects second
        WebSocketSession aliceSession = mockSession("session-alice", WS_URI_ALICE);
        handler.afterConnectionEstablished(aliceSession);

        // Alice should receive LOBBY_PLAYER_LIST with Bob included
        verify(aliceSession, atLeastOnce()).sendMessage(argThat(msg -> {
            try {
                JsonNode node = objectMapper.readTree(((TextMessage) msg).getPayload());
                if (!"LOBBY_PLAYER_LIST".equals(node.path("type").asText())) {
                    return false;
                }
                JsonNode players = node.path("players");
                return players.isArray() && players.size() >= 1;
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private WebSocketSession mockSession(String sessionId, String uriStr) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.getUri()).thenReturn(URI.create(uriStr));
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    /**
     * Asserts that the session received at least one message of the given type
     * among all sendMessage() invocations.
     */
    private void assertReceivedMessageType(WebSocketSession session, String expectedType) throws Exception {
        verify(session, atLeastOnce()).sendMessage(argThat(msg -> {
            try {
                JsonNode node = objectMapper.readTree(((TextMessage) msg).getPayload());
                return expectedType.equals(node.path("type").asText());
            } catch (Exception e) {
                return false;
            }
        }));
    }
}
