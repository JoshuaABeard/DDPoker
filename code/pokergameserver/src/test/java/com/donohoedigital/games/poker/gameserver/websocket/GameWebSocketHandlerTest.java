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

import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.GameServerProperties;
import com.donohoedigital.games.poker.gameserver.ServerGameEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.service.AuthService;
import com.donohoedigital.games.poker.gameserver.service.GameService;

/**
 * Unit tests for GameWebSocketHandler.
 *
 * Uses Mockito for JwtTokenProvider, GameInstanceManager, and GameInstance.
 * Uses real GameConnectionManager, OutboundMessageConverter, and ObjectMapper.
 */
class GameWebSocketHandlerTest {

    private JwtTokenProvider jwtTokenProvider;
    private GameInstanceManager gameInstanceManager;
    private GameConnectionManager connectionManager;
    private InboundMessageRouter inboundMessageRouter;
    private OutboundMessageConverter converter;
    private ObjectMapper objectMapper;
    private GameService gameService;
    private AuthService authService;
    private GameWebSocketHandler handler;

    private WebSocketSession session;
    private GameInstance gameInstance;

    private static final String GAME_ID = "game-1";
    private static final long PROFILE_ID = 42L;
    private static final String USERNAME = "testuser";
    private static final String VALID_TOKEN = "valid-token";
    private static final String WS_URI = "ws://localhost/ws/games/" + GAME_ID + "?token=" + VALID_TOKEN;

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        gameInstanceManager = mock(GameInstanceManager.class);
        connectionManager = new GameConnectionManager();
        inboundMessageRouter = mock(InboundMessageRouter.class);
        converter = new OutboundMessageConverter();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        gameService = mock(GameService.class);
        authService = mock(AuthService.class);

        handler = new GameWebSocketHandler(jwtTokenProvider, gameInstanceManager, connectionManager,
                inboundMessageRouter, converter, objectMapper, gameService, authService,
                new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0));

        // Set up JWT provider defaults: getClaims() returns a Claims mock with no scope
        // (null scope = legacy desktop client compatibility path)
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.get("profileId", Long.class)).thenReturn(PROFILE_ID);
        when(mockClaims.getSubject()).thenReturn(USERNAME);
        when(mockClaims.get("scope", String.class)).thenReturn(null);
        when(mockClaims.getId()).thenReturn(null);
        when(mockClaims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getClaims(VALID_TOKEN)).thenReturn(mockClaims);

        // generateReconnectToken returns a stub token
        when(authService.generateReconnectToken(anyLong(), anyString(), anyString()))
                .thenReturn("reconnect-token-stub");

        // Set up game instance defaults
        gameInstance = mock(GameInstance.class);
        when(gameInstanceManager.getGame(GAME_ID)).thenReturn(gameInstance);
        when(gameInstance.getOwnerProfileId()).thenReturn(1L);
        when(gameInstance.getState()).thenReturn(GameInstanceState.IN_PROGRESS);
        when(gameInstance.hasPlayer(PROFILE_ID)).thenReturn(true);
        when(gameInstance.getPlayerSessions()).thenReturn(Collections.emptyMap());
        when(gameInstance.getEventBus()).thenReturn(null);

        // Set up mock session
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sess-1");
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(new URI(WS_URI));
    }

    @Test
    void afterConnectionEstablished_validToken_connectedPlayer() throws Exception {
        handler.afterConnectionEstablished(session);

        // Should NOT have closed the session
        verify(session, never()).close(any(CloseStatus.class));
        // Should have sent CONNECTED message
        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "CONNECTED")));
    }

    @Test
    void afterConnectionEstablished_invalidToken_closesWithError() throws Exception {
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(false);

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status -> status.getCode() == 4001));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void afterConnectionEstablished_gameNotFound_closesWithError() throws Exception {
        when(gameInstanceManager.getGame(GAME_ID)).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status -> status.getCode() == 4004));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void afterConnectionEstablished_playerNotInGame_closesWithError() throws Exception {
        // Player not in an in-progress game
        when(gameInstance.getState()).thenReturn(GameInstanceState.IN_PROGRESS);
        when(gameInstance.hasPlayer(PROFILE_ID)).thenReturn(false);

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status -> status.getCode() == 4003));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void afterConnectionEstablished_waitingForPlayers_autoJoinsPlayer() throws Exception {
        when(gameInstance.getState()).thenReturn(GameInstanceState.WAITING_FOR_PLAYERS);
        when(gameInstance.hasPlayer(PROFILE_ID)).thenReturn(false);

        handler.afterConnectionEstablished(session);

        // Should auto-join (add) the player
        verify(gameInstance).addPlayer(eq(PROFILE_ID), eq(USERNAME), eq(false), eq(0));
        // Should NOT close the session
        verify(session, never()).close(any(CloseStatus.class));
        // Should send CONNECTED
        verify(session).sendMessage(argThat(msg -> isMessageWithType(msg, "CONNECTED")));
    }

    @Test
    void autoStartsGameWhenOwnerConnectsWithPreAddedEntry() throws Exception {
        // Simulate practice game: owner was pre-added by REST endpoint, connects via WS
        when(gameInstance.getState()).thenReturn(GameInstanceState.WAITING_FOR_PLAYERS);
        when(gameInstance.hasPlayer(PROFILE_ID)).thenReturn(true);
        when(gameInstance.getOwnerProfileId()).thenReturn(PROFILE_ID);
        // prepareStart() must return a non-null bus so the broadcaster can be wired
        when(gameInstance.prepareStart()).thenReturn(mock(ServerGameEventBus.class));

        handler.afterConnectionEstablished(session);

        verify(gameInstanceManager).startGame(GAME_ID, PROFILE_ID);
        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void doesNotAutoStartGameWhenNonOwnerConnects() throws Exception {
        // Non-owner already in lobby — must not trigger auto-start
        when(gameInstance.getState()).thenReturn(GameInstanceState.WAITING_FOR_PLAYERS);
        when(gameInstance.hasPlayer(PROFILE_ID)).thenReturn(true);
        when(gameInstance.getOwnerProfileId()).thenReturn(PROFILE_ID + 1); // different owner

        handler.afterConnectionEstablished(session);

        verify(gameInstanceManager, never()).startGame(anyString(), anyLong());
    }

    @Test
    void handleTextMessage_delegatesToInboundMessageRouter() throws Exception {
        // First establish connection
        handler.afterConnectionEstablished(session);

        // Then send a text message
        String json = "{\"type\":\"PLAYER_ACTION\",\"sequenceNumber\":1,\"data\":{\"action\":\"FOLD\",\"amount\":0}}";
        handler.handleTextMessage(session, new TextMessage(json));

        verify(inboundMessageRouter).handleMessage(any(PlayerConnection.class), eq(json));
    }

    @Test
    void afterConnectionClosed_unregistersConnection() throws Exception {
        // Establish connection first
        handler.afterConnectionEstablished(session);

        // Close connection
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Game instance should be notified
        verify(gameInstance).removePlayer(PROFILE_ID);
    }

    // ============================================================================================
    // Helpers
    // ============================================================================================

    private boolean isMessageWithType(Object msg, String expectedType) {
        if (!(msg instanceof TextMessage textMessage)) {
            return false;
        }
        try {
            return textMessage.getPayload().contains("\"" + expectedType + "\"");
        } catch (Exception e) {
            return false;
        }
    }
}
