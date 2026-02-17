/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.websocket;

import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for InboundMessageRouter — verifies routing, validation, and error
 * handling of inbound WebSocket messages.
 */
class InboundMessageRouterTest {

    private GameInstanceManager gameInstanceManager;
    private GameInstance gameInstance;
    private GameConnectionManager connectionManager;
    private ObjectMapper objectMapper;
    private InboundMessageRouter router;
    private WebSocketSession session;
    private PlayerConnection connection;

    private static final String GAME_ID = "game-1";
    private static final long OWNER_PROFILE_ID = 1L;
    private static final long PLAYER_PROFILE_ID = 42L;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        gameInstanceManager = mock(GameInstanceManager.class);
        gameInstance = mock(GameInstance.class);
        connectionManager = new GameConnectionManager();

        when(gameInstanceManager.getGame(GAME_ID)).thenReturn(gameInstance);
        when(gameInstance.getOwnerProfileId()).thenReturn(OWNER_PROFILE_ID);
        when(gameInstance.getState()).thenReturn(GameInstanceState.IN_PROGRESS);
        when(gameInstance.getPlayerSessions()).thenReturn(Collections.emptyMap());

        // Use a 500ms action rate limiter (permissive for most tests)
        RateLimiter actionRateLimiter = new RateLimiter(500);
        RateLimiter chatRateLimiter = new RateLimiter(500);

        router = new InboundMessageRouter(gameInstanceManager, connectionManager, actionRateLimiter, chatRateLimiter,
                objectMapper);

        // Mock WebSocket session
        session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("sess-1");

        // Create a player connection
        connection = new PlayerConnection(session, PLAYER_PROFILE_ID, "testuser", GAME_ID, objectMapper);

        // Add to connection manager
        connectionManager.addConnection(GAME_ID, PLAYER_PROFILE_ID, connection);
    }

    @Test
    void handleMessage_dispatchesPlayerAction() throws Exception {
        String json = "{\"type\":\"PLAYER_ACTION\",\"sequenceNumber\":1,\"data\":{\"action\":\"FOLD\",\"amount\":0}}";

        router.handleMessage(connection, json);

        verify(gameInstance).onPlayerAction(eq(PLAYER_PROFILE_ID), any(PlayerAction.class));
        verify(session, never()).sendMessage(argThat(msg -> isErrorMessage(msg)));
    }

    @Test
    void handleMessage_rejectsOutOfOrderSequenceNumber() throws Exception {
        // First message with seq=5
        connection.setLastSequenceNumber(5);

        // Send a message with seq=3 (out of order)
        String json = "{\"type\":\"PLAYER_ACTION\",\"sequenceNumber\":3,\"data\":{\"action\":\"FOLD\",\"amount\":0}}";

        router.handleMessage(connection, json);

        // Should send error
        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "ERROR")));
        // Should not dispatch the action
        verify(gameInstance, never()).onPlayerAction(anyLong(), any(PlayerAction.class));
    }

    @Test
    void handleMessage_rejectsRateLimitedActions() throws Exception {
        // Use a very long interval rate limiter (effectively blocks second action)
        RateLimiter strictActionRateLimiter = new RateLimiter(60_000); // 60 second interval
        RateLimiter chatRateLimiter = new RateLimiter(500);
        InboundMessageRouter strictRouter = new InboundMessageRouter(gameInstanceManager, connectionManager,
                strictActionRateLimiter, chatRateLimiter, objectMapper);

        WebSocketSession strictSession = mock(WebSocketSession.class);
        when(strictSession.isOpen()).thenReturn(true);
        when(strictSession.getId()).thenReturn("strict-sess");
        PlayerConnection strictConnection = new PlayerConnection(strictSession, 99L, "strictuser", GAME_ID,
                objectMapper);
        connectionManager.addConnection(GAME_ID, 99L, strictConnection);

        // First action should pass
        String json1 = "{\"type\":\"PLAYER_ACTION\",\"sequenceNumber\":1,\"data\":{\"action\":\"FOLD\",\"amount\":0}}";
        strictRouter.handleMessage(strictConnection, json1);
        verify(strictSession, never()).sendMessage(argThat(msg -> isErrorMessage(msg)));

        // Second action immediately after should be rate-limited
        String json2 = "{\"type\":\"PLAYER_ACTION\",\"sequenceNumber\":2,\"data\":{\"action\":\"CHECK\",\"amount\":0}}";
        strictRouter.handleMessage(strictConnection, json2);
        verify(strictSession).sendMessage(argThat(msg -> isMessageWithType(msg, "ERROR")));
    }

    @Test
    void handleMessage_dispatchesChat() throws Exception {
        // Add a second player to verify broadcast
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.isOpen()).thenReturn(true);
        when(session2.getId()).thenReturn("sess-2");
        PlayerConnection connection2 = new PlayerConnection(session2, 2L, "player2", GAME_ID, objectMapper);
        connectionManager.addConnection(GAME_ID, 2L, connection2);

        String json = "{\"type\":\"CHAT\",\"sequenceNumber\":1,\"data\":{\"message\":\"Hello!\",\"tableChat\":true}}";

        router.handleMessage(connection, json);

        // Both players should receive the chat message
        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "CHAT_MESSAGE")));
        verify(session2).sendMessage(argThat(msg -> isMessageWithType(msg, "CHAT_MESSAGE")));
    }

    @Test
    void handleMessage_adminKick_requiresOwner() throws Exception {
        // Player 42 is not the owner (owner is 1)
        String json = "{\"type\":\"ADMIN_KICK\",\"sequenceNumber\":1,\"data\":{\"playerId\":2}}";

        router.handleMessage(connection, json);

        // Should get forbidden error
        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "ERROR")));
        // Game should not remove player
        verify(gameInstance, never()).removePlayer(anyLong());
    }

    @Test
    void handleMessage_adminKick_ownerSucceeds() throws Exception {
        // Create owner connection
        WebSocketSession ownerSession = mock(WebSocketSession.class);
        when(ownerSession.isOpen()).thenReturn(true);
        when(ownerSession.getId()).thenReturn("owner-sess");
        PlayerConnection ownerConnection = new PlayerConnection(ownerSession, OWNER_PROFILE_ID, "owner", GAME_ID,
                objectMapper);
        connectionManager.addConnection(GAME_ID, OWNER_PROFILE_ID, ownerConnection);

        // Target player to kick
        WebSocketSession targetSession = mock(WebSocketSession.class);
        when(targetSession.isOpen()).thenReturn(true);
        when(targetSession.getId()).thenReturn("target-sess");
        PlayerConnection targetConnection = new PlayerConnection(targetSession, 99L, "targetplayer", GAME_ID,
                objectMapper);
        connectionManager.addConnection(GAME_ID, 99L, targetConnection);

        String json = "{\"type\":\"ADMIN_KICK\",\"sequenceNumber\":1,\"data\":{\"playerId\":99}}";

        router.handleMessage(ownerConnection, json);

        // Game should remove the player
        verify(gameInstance).removePlayer(99L);
        // No error message for owner
        verify(ownerSession, never()).sendMessage(argThat(msg -> isErrorMessage(msg)));
    }

    @Test
    void handleMessage_adminPause_ownerOnly() throws Exception {
        // Player 42 is not the owner
        String json = "{\"type\":\"ADMIN_PAUSE\",\"sequenceNumber\":1,\"data\":{}}";

        router.handleMessage(connection, json);

        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "ERROR")));
        verify(gameInstance, never()).pauseAsUser(anyLong());
    }

    @Test
    void handleMessage_adminResume_ownerOnly() throws Exception {
        // Player 42 is not the owner
        String json = "{\"type\":\"ADMIN_RESUME\",\"sequenceNumber\":1,\"data\":{}}";

        router.handleMessage(connection, json);

        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "ERROR")));
        verify(gameInstance, never()).resumeAsUser(anyLong());
    }

    @Test
    void handleMessage_unknownActionString_sendsError() throws Exception {
        String json = "{\"type\":\"PLAYER_ACTION\",\"sequenceNumber\":1,\"data\":{\"action\":\"INVALID\",\"amount\":0}}";

        router.handleMessage(connection, json);

        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "ERROR")));
        verify(gameInstance, never()).onPlayerAction(anyLong(), any(PlayerAction.class));
    }

    @Test
    void handleMessage_malformedJson_sendsError() throws Exception {
        String badJson = "not-valid-json{{{";

        router.handleMessage(connection, badJson);

        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "ERROR")));
        verify(gameInstance, never()).onPlayerAction(anyLong(), any(PlayerAction.class));
    }

    @Test
    void handleMessage_playerActionValidation_ownerIsAlsoPlayer() throws Exception {
        // Owner (profile 1) can also submit player actions
        WebSocketSession ownerSession = mock(WebSocketSession.class);
        when(ownerSession.isOpen()).thenReturn(true);
        when(ownerSession.getId()).thenReturn("owner-sess");
        PlayerConnection ownerConnection = new PlayerConnection(ownerSession, OWNER_PROFILE_ID, "owner", GAME_ID,
                objectMapper);
        connectionManager.addConnection(GAME_ID, OWNER_PROFILE_ID, ownerConnection);

        String json = "{\"type\":\"PLAYER_ACTION\",\"sequenceNumber\":1,\"data\":{\"action\":\"CHECK\",\"amount\":0}}";

        router.handleMessage(ownerConnection, json);

        verify(gameInstance).onPlayerAction(eq(OWNER_PROFILE_ID), any(PlayerAction.class));
        verify(ownerSession, never()).sendMessage(argThat(msg -> isErrorMessage(msg)));
    }

    @Test
    void allIn_action_returnsInvalidAction() throws Exception {
        // ALL_IN is not a valid client action; clients must use RAISE with the all-in
        // amount
        String json = "{\"type\":\"PLAYER_ACTION\",\"sequenceNumber\":1,\"data\":{\"action\":\"ALL_IN\",\"amount\":0}}";

        router.handleMessage(connection, json);

        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "ERROR")));
        verify(gameInstance, never()).onPlayerAction(anyLong(), any(PlayerAction.class));
    }

    // ============================================================================================
    // Helpers
    // ============================================================================================

    private boolean isErrorMessage(Object msg) {
        return isMessageWithType(msg, "ERROR");
    }

    private boolean isMessageWithType(Object msg, String expectedType) {
        if (!(msg instanceof TextMessage textMessage)) {
            return false;
        }
        try {
            ServerMessage serverMessage = objectMapper.readValue(textMessage.getPayload(), ServerMessage.class);
            return serverMessage.type().name().equals(expectedType);
        } catch (Exception e) {
            // Try parsing just the type field
            try {
                String payload = textMessage.getPayload();
                return payload.contains("\"" + expectedType + "\"");
            } catch (Exception ex) {
                return false;
            }
        }
    }
}
