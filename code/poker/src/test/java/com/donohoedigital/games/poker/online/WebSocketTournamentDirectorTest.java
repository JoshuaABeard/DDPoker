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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.GameClock;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.protocol.message.ServerMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WebSocketTournamentDirector} message dispatch.
 *
 * <p>
 * Uses {@link #dispatch} helper to send JSON message payloads and then runs
 * pending EDT events so the handler's SwingUtilities.invokeLater completes.
 *
 * <p>
 * Tests that required {@link RemotePokerTable} construction (which needs
 * {@code PropertyConfig} to be initialized) have been removed — they always
 * skipped outside a full game engine context.
 */
class WebSocketTournamentDirectorTest {

    private WebSocketTournamentDirector wsTD;
    private PokerGame mockGame;
    private GameClock mockClock;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        mockGame = Mockito.mock(PokerGame.class);
        mockClock = Mockito.mock(GameClock.class);
        Mockito.when(mockGame.getGameClock()).thenReturn(mockClock);
        wsTD = new WebSocketTournamentDirector();
        wsTD.setGameForTest(mockGame);
        wsTD.setLocalPlayerIdForTest(1L);
    }

    // -------------------------------------------------------------------------
    // GAME_STATE
    // -------------------------------------------------------------------------

    @Test
    void gameStateUpdatesGameLevel() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 3));

        // setLevel is called regardless of PropertyConfig (game_ is a mock)
        Mockito.verify(mockGame).setLevel(3);
    }

    // -------------------------------------------------------------------------
    // LEVEL_CHANGED
    // -------------------------------------------------------------------------

    @Test
    void levelChangedUpdatesGameLevel() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("level", 5).put("smallBlind", 200).put("bigBlind", 400).put("ante", 50);
        payload.putNull("nextLevelIn");
        dispatch(ServerMessageType.LEVEL_CHANGED, payload);

        Mockito.verify(mockGame).setLevel(5);
    }

    // -------------------------------------------------------------------------
    // PLAYER_ELIMINATED / GAME_COMPLETE — client ID mapping
    // -------------------------------------------------------------------------

    /**
     * PLAYER_ELIMINATED must call game_.applyPlayerResult with the CLIENT player ID
     * (from game_.players_), not the server-assigned player ID. In practice games
     * the server uses the JWT profileId for the human and -1, -2, ... for AI, while
     * game_.players_ uses sequential IDs 0, 1, 2, ...
     */
    @Test
    void playerEliminatedCallsApplyPlayerResultWithClientId() throws Exception {
        // Set localPlayerId to a non-zero server value (simulating a JWT profileId).
        long humanServerIdl = 42L;
        ObjectNode connected = mapper.createObjectNode();
        connected.put("playerId", humanServerIdl);
        dispatch(ServerMessageType.CONNECTED, connected);

        // Configure mock game: human at client ID 0, one AI at client ID 1.
        ClientPlayer humanGamePlayer = Mockito.mock(ClientPlayer.class);
        Mockito.when(humanGamePlayer.getID()).thenReturn(0);
        ClientPlayer aiGamePlayer = Mockito.mock(ClientPlayer.class);
        Mockito.when(aiGamePlayer.getID()).thenReturn(1);

        Mockito.when(mockGame.getPokerPlayerFromID(0)).thenReturn(humanGamePlayer);
        Mockito.when(mockGame.getPokerPlayerAt(1)).thenReturn(aiGamePlayer);
        Mockito.when(mockGame.getNumPlayers()).thenReturn(2);

        // AI eliminated at server ID -1 (finish position 2).
        ObjectNode aiElim = mapper.createObjectNode();
        aiElim.put("playerId", -1L).put("playerName", "Bot").put("finishPosition", 2).put("handsPlayed", 5);
        dispatch(ServerMessageType.PLAYER_ELIMINATED, aiElim);

        // Must call applyPlayerResult with CLIENT id 1, not server id -1.
        Mockito.verify(mockGame).applyPlayerResult(1, 2);
    }

    /**
     * GAME_COMPLETE must call game_.applyPlayerResult for the winner using the
     * CLIENT player ID, not the server-assigned profileId.
     */
    @Test
    void gameCompleteCallsApplyPlayerResultWithClientIdForWinner() throws Exception {
        // Set localPlayerId to a non-zero server value.
        long humanServerId = 42L;
        ObjectNode connected = mapper.createObjectNode();
        connected.put("playerId", humanServerId);
        dispatch(ServerMessageType.CONNECTED, connected);

        // Configure mock game: human at client ID 0, one AI at client ID 1.
        ClientPlayer humanGamePlayer = Mockito.mock(ClientPlayer.class);
        Mockito.when(humanGamePlayer.getID()).thenReturn(0);
        ClientPlayer aiGamePlayer = Mockito.mock(ClientPlayer.class);
        Mockito.when(aiGamePlayer.getID()).thenReturn(1);

        Mockito.when(mockGame.getPokerPlayerFromID(0)).thenReturn(humanGamePlayer);
        Mockito.when(mockGame.getPokerPlayerAt(1)).thenReturn(aiGamePlayer);
        Mockito.when(mockGame.getNumPlayers()).thenReturn(2);

        // Human wins (server ID 42 = winner).
        ObjectNode standing = mapper.createObjectNode();
        standing.put("playerId", humanServerId).put("position", 1).put("playerName", "Alice").put("prize", 0);
        ObjectNode gameComplete = mapper.createObjectNode();
        gameComplete.putArray("standings").add(standing);
        gameComplete.put("totalHands", 10).put("duration", 120000L);
        dispatch(ServerMessageType.GAME_COMPLETE, gameComplete);

        // Must call applyPlayerResult with CLIENT id 0, not server id 42.
        Mockito.verify(mockGame, Mockito.times(1)).applyPlayerResult(0, 1);
    }

    // -------------------------------------------------------------------------
    // CHAT_MESSAGE
    // -------------------------------------------------------------------------

    @Test
    void chatMessageDoesNotThrow() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 2L).put("playerName", "Bob").put("message", "Hello!").put("tableChat", true);
        dispatch(ServerMessageType.CHAT_MESSAGE, payload);
        // No exception — chatHandler_ is null in M4 practice mode
    }

    // -------------------------------------------------------------------------
    // ERROR
    // -------------------------------------------------------------------------

    @Test
    void errorMessageDoesNotThrow() throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("code", "INVALID_ACTION").put("message", "Cannot raise now");
        dispatch(ServerMessageType.ERROR, payload);
        // Logged as error — no exception thrown
    }

    // -------------------------------------------------------------------------
    // PokerDirector implementation
    // -------------------------------------------------------------------------

    @Test
    void setPausedTrueDoesNotThrow() {
        // In unit test, wsClient is not connected — just verify no NPE
        wsTD.setPaused(true);
    }

    @Test
    void setPausedFalseDoesNotThrow() {
        wsTD.setPaused(false);
    }

    @Test
    void playerUpdateNullPlayerIsNoOp() {
        wsTD.playerUpdate(null, null); // null player is safe
    }

    @Test
    void getPhaseName() {
        assertThat(wsTD.getPhaseName()).isEqualTo("WebSocketTournamentDirector");
    }

    @Test
    void getSaveLockObjectReturnsNonNull() {
        assertThat(wsTD.getSaveLockObject()).isNotNull();
    }

    @Test
    void setChatHandlerStores() {
        ChatHandler handler = (fromPlayerID, chatType, message) -> {
        };
        wsTD.setChatHandler(handler);
        // No exception, handler stored internally
    }

    // -------------------------------------------------------------------------
    // GAME_PAUSED / GAME_RESUMED — GameClock pause/unpause
    // -------------------------------------------------------------------------

    @Test
    void gamePausedCallsClockPause() throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("reason", "user request").put("pausedBy", "Alice");
        dispatch(ServerMessageType.GAME_PAUSED, payload);

        Mockito.verify(mockClock).pause();
    }

    @Test
    void gameResumedCallsClockUnpause() throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("resumedBy", "Alice");
        dispatch(ServerMessageType.GAME_RESUMED, payload);

        Mockito.verify(mockClock).unpause();
    }

    // -------------------------------------------------------------------------
    // GAME_CANCELLED
    // -------------------------------------------------------------------------

    @Test
    void gameCancelledDoesNotThrow() throws Exception {
        // context_ is null in unit tests so processPhase is not called,
        // but the null guard must prevent any exception.
        ObjectNode payload = mapper.createObjectNode();
        payload.put("reason", "host left");
        dispatch(ServerMessageType.GAME_CANCELLED, payload);
        // No exception — null-guarded processPhase is the key assertion here.
    }

    // -------------------------------------------------------------------------
    // deliverChatLocal — routes to chatHandler
    // -------------------------------------------------------------------------

    @Test
    void deliverChatLocalWithHandlerCallsChatReceived() {
        List<int[]> received = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        ChatHandler handler = (fromPlayerID, chatType, message) -> {
            received.add(new int[]{fromPlayerID, chatType});
            messages.add(message);
        };
        wsTD.setChatHandler(handler);

        wsTD.deliverChatLocal(1, "Hello!", 42);

        assertThat(received).hasSize(1);
        assertThat(received.get(0)[0]).isEqualTo(42); // fromPlayerID = id param
        assertThat(received.get(0)[1]).isEqualTo(1); // chatType = nType param
        assertThat(messages.get(0)).isEqualTo("Hello!");
    }

    @Test
    void deliverChatLocalWithNullHandlerDoesNotThrow() {
        // chatHandler_ is null — must not throw NPE
        wsTD.deliverChatLocal(0, "ignored", 99);
    }

    @Test
    void chatMessageDeliveredToHandlerViaOnChatMessage() throws Exception {
        List<String> received = new ArrayList<>();
        wsTD.setChatHandler((fromPlayerID, chatType, message) -> received.add(message));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 2L).put("playerName", "Bob").put("message", "Hello!").put("tableChat", true);
        dispatch(ServerMessageType.CHAT_MESSAGE, payload);

        assertThat(received).hasSize(1);
        assertThat(received.get(0)).contains("Bob").contains("Hello!");
    }

    // -------------------------------------------------------------------------
    // resolveActionAmount — ALL_IN sends 0 (server resolves), others pass through
    // -------------------------------------------------------------------------

    @Test
    void resolveActionAmountAllInReturnsZero() throws Exception {
        // ALL_IN amount is always 0 because the server resolves it from pending
        // options.
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        ObjectNode options = mapper.createObjectNode();
        options.put("canFold", true).put("canCheck", false).put("canCall", true).put("callAmount", 50)
                .put("canBet", false).put("minBet", 0).put("maxBet", 0).put("canRaise", true).put("minRaise", 100)
                .put("maxRaise", 1500).put("canAllIn", true).put("allInAmount", 1500);
        ObjectNode payload = mapper.createObjectNode();
        payload.put("timeoutSeconds", 30);
        payload.set("options", options);
        dispatch(ServerMessageType.ACTION_REQUIRED, payload);

        // Server resolves ALL_IN amount, client sends 0.
        assertThat(wsTD.resolveActionAmount(PokerGame.ACTION_ALL_IN, 100)).isEqualTo(0);
    }

    @Test
    void resolveActionAmountNonAllInUsesUiAmount() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        ObjectNode options = mapper.createObjectNode();
        options.put("canFold", true).put("canCheck", false).put("canCall", true).put("callAmount", 50)
                .put("canBet", false).put("minBet", 0).put("maxBet", 0).put("canRaise", true).put("minRaise", 100)
                .put("maxRaise", 1500).put("canAllIn", true).put("allInAmount", 1500);
        ObjectNode payload = mapper.createObjectNode();
        payload.put("timeoutSeconds", 30);
        payload.set("options", options);
        dispatch(ServerMessageType.ACTION_REQUIRED, payload);

        // RAISE, BET, CALL, FOLD must pass through the uiAmount unchanged.
        assertThat(wsTD.resolveActionAmount(PokerGame.ACTION_RAISE, 400)).isEqualTo(400);
        assertThat(wsTD.resolveActionAmount(PokerGame.ACTION_BET, 200)).isEqualTo(200);
        assertThat(wsTD.resolveActionAmount(PokerGame.ACTION_CALL, 50)).isEqualTo(50);
        assertThat(wsTD.resolveActionAmount(PokerGame.ACTION_FOLD, 0)).isEqualTo(0);
    }

    @Test
    void resolveActionAmountAllInWithNoOptionsReturnsZero() {
        // currentOptions_ is null — ALL_IN always returns 0 regardless.
        assertThat(wsTD.resolveActionAmount(PokerGame.ACTION_ALL_IN, 500)).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // allInWsAction — always sends "ALL_IN" (server resolves to BET/RAISE/CALL)
    // -------------------------------------------------------------------------

    @Test
    void allInWsActionAlwaysSendsAllIn() {
        // ALL_IN is now sent directly to the server which resolves it.
        assertThat(wsTD.allInWsActionForTest()).isEqualTo("ALL_IN");
    }

    // -------------------------------------------------------------------------
    // finish() lifecycle — declineScheduler_ shutdown
    // -------------------------------------------------------------------------

    @Test
    void finishShutsDownDeclineScheduler() {
        // After finish(), the declineScheduler_ must be shut down.
        // Verified via the test accessor which runs the same shutdown logic.
        wsTD.finishSchedulersForTest();
        assertThat(wsTD.isDeclineSchedulerShutdownForTest()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Dispatches a message and drains the Swing EDT. */
    private void dispatch(ServerMessageType type, ObjectNode data) throws Exception {
        WebSocketGameClient.InboundMessage msg = new WebSocketGameClient.InboundMessage(type, "test-game-id", data,
                null);
        wsTD.onMessage(msg);
        drainEdt();
    }

    /** Waits for all pending EDT tasks to complete. */
    private static void drainEdt() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(latch::countDown);
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    /**
     * Builds a GAME_STATE JSON with N tables at level {@code level}, no players.
     */
    private ObjectNode buildGameState(int numTables, int level) {
        return buildGameState(numTables, level, -1L);
    }

    /**
     * Builds a GAME_STATE JSON with N tables at level {@code level}. If
     * {@code seatPlayerId >= 0}, places that player at seat 0 of each table.
     */
    private ObjectNode buildGameState(int numTables, int level, long seatPlayerId) {
        ObjectNode gs = mapper.createObjectNode();
        gs.put("status", "IN_PROGRESS");
        gs.put("level", level);

        ObjectNode blinds = mapper.createObjectNode();
        blinds.put("small", 50).put("big", 100).put("ante", 0);
        gs.set("blinds", blinds);
        gs.putNull("nextLevelIn");

        var tables = gs.putArray("tables");
        for (int i = 1; i <= numTables; i++) {
            ObjectNode t = mapper.createObjectNode();
            t.put("tableId", i);
            t.put("currentRound", "PRE_FLOP");
            t.put("handNumber", 0);
            t.putArray("communityCards");
            t.putArray("pots");

            var seats = t.putArray("seats");
            if (seatPlayerId > 0) {
                ObjectNode seat = mapper.createObjectNode();
                seat.put("seatIndex", 0).put("playerId", seatPlayerId).put("playerName", "Alice").put("chipCount", 1000)
                        .put("status", "ACTIVE").put("isDealer", true).put("isSmallBlind", false)
                        .put("isBigBlind", false).put("currentBet", 0).put("isCurrentActor", false);
                seat.putArray("holeCards");
                seats.add(seat);
            }
            tables.add(t);
        }

        gs.putArray("players");
        return gs;
    }

    /** Builds a HAND_STARTED JSON. */
    private ObjectNode handStarted(int dealerSeat, int smallBlindSeat, int bigBlindSeat) {
        ObjectNode node = mapper.createObjectNode();
        node.put("handNumber", 1).put("dealerSeat", dealerSeat).put("smallBlindSeat", smallBlindSeat)
                .put("bigBlindSeat", bigBlindSeat);
        node.putArray("blindsPosted");
        return node;
    }
}
