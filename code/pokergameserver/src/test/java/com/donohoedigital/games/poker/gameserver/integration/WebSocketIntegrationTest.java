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
package com.donohoedigital.games.poker.gameserver.integration;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.BlindLevel;
import com.donohoedigital.games.poker.gameserver.GameConfig.LevelAdvanceMode;
import com.donohoedigital.games.poker.gameserver.GameConfig.PayoutConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.TimeoutConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.BootConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.InviteConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.BettingConfig;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the WebSocket game protocol.
 *
 * <p>
 * Tests the full WebSocket lifecycle: connect to game, receive CONNECTED
 * message, disconnect, and reconnect.
 *
 * <p>
 * Note: Uses GameInstanceManager directly to create games because the REST API
 * (GameService/JPA) and the in-memory game engine (GameInstanceManager) are
 * separate systems that are not yet bridged. The WebSocket protocol layer
 * (GameWebSocketHandler) uses GameInstanceManager exclusively.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
class WebSocketIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    GameInstanceManager gameInstanceManager;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Test
    void connect_receivesConnectedMessage() throws Exception {
        long profileId = 10001L;
        String username = "ws_player1";

        // Create game directly in the in-memory engine
        GameInstance game = gameInstanceManager.createGame(profileId, createTestConfig());
        game.transitionToWaitingForPlayers();
        String gameId = game.getGameId();

        // Generate a valid JWT for this player
        String token = jwtTokenProvider.generateToken(username, profileId, false);

        // Connect via WebSocket
        List<String> messages = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        wsClient.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                messages.add(message.getPayload());
                latch.countDown();
            }
        }, "ws://localhost:" + port + "/ws/games/" + gameId + "?token=" + token).get(5, TimeUnit.SECONDS);

        // Wait for CONNECTED message
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for CONNECTED message");
        assertFalse(messages.isEmpty());

        JsonNode msg = objectMapper.readTree(messages.get(0));
        assertEquals("CONNECTED", msg.get("type").asText());
        assertEquals(gameId, msg.get("gameId").asText());

        game.cancel();
    }

    @Test
    void disconnect_thenReconnect_receivesConnectedAgain() throws Exception {
        long profileId = 10002L;
        String username = "ws_player2";

        GameInstance game = gameInstanceManager.createGame(profileId, createTestConfig());
        game.transitionToWaitingForPlayers();
        String gameId = game.getGameId();

        String token = jwtTokenProvider.generateToken(username, profileId, false);

        // First connection
        CountDownLatch firstLatch = new CountDownLatch(1);
        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        WebSocketSession firstSession = wsClient.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                firstLatch.countDown();
            }
        }, "ws://localhost:" + port + "/ws/games/" + gameId + "?token=" + token).get(5, TimeUnit.SECONDS);

        assertTrue(firstLatch.await(5, TimeUnit.SECONDS), "Timed out on first connection");

        // Disconnect
        firstSession.close();
        Thread.sleep(100); // Brief pause to let disconnect propagate

        // Reconnect
        List<String> reconnectMessages = new CopyOnWriteArrayList<>();
        CountDownLatch reconnectLatch = new CountDownLatch(1);
        wsClient.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                reconnectMessages.add(message.getPayload());
                reconnectLatch.countDown();
            }
        }, "ws://localhost:" + port + "/ws/games/" + gameId + "?token=" + token).get(5, TimeUnit.SECONDS);

        assertTrue(reconnectLatch.await(5, TimeUnit.SECONDS), "Timed out waiting for reconnect CONNECTED message");
        assertFalse(reconnectMessages.isEmpty());

        JsonNode msg = objectMapper.readTree(reconnectMessages.get(0));
        assertEquals("CONNECTED", msg.get("type").asText());

        game.cancel();
    }

    @Test
    void invalidToken_connectionIsRejected() throws Exception {
        long profileId = 10003L;

        GameInstance game = gameInstanceManager.createGame(profileId, createTestConfig());
        game.transitionToWaitingForPlayers();
        String gameId = game.getGameId();

        // Connect with invalid token
        CountDownLatch closeLatch = new CountDownLatch(1);
        List<String> received = new CopyOnWriteArrayList<>();

        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        wsClient.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                received.add(message.getPayload());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session,
                    org.springframework.web.socket.CloseStatus status) {
                closeLatch.countDown();
            }
        }, "ws://localhost:" + port + "/ws/games/" + gameId + "?token=invalid.jwt.token").get(5, TimeUnit.SECONDS);

        // Connection should be closed quickly with no game messages
        assertTrue(closeLatch.await(5, TimeUnit.SECONDS), "Timed out waiting for connection close");
        assertTrue(received.isEmpty(), "No game messages should be received with invalid token");

        game.cancel();
    }

    private GameConfig createTestConfig() {
        List<BlindLevel> blinds = List.of(new BlindLevel(0, 10, 20, 10, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(0, 25, 50, 10, false, "NOLIMIT_HOLDEM"));

        return new GameConfig("WS Integration Test", "Test", "Welcome!", 9, 90, true, 1500, 0, blinds, true,
                "NOLIMIT_HOLDEM", LevelAdvanceMode.TIME, 0, 10, null, null,
                new PayoutConfig("SPOTS", 3, 0, 0, "AUTO", List.of()), null, null,
                new TimeoutConfig(30, 0, 0, 0, 0, 15), new BootConfig(true, 25, true, 10), null, null,
                new InviteConfig(false, List.of(), true), new BettingConfig(0, true), false, false, List.of(),
                null, // humanDisplayName
                null); // practiceConfig
    }
}
