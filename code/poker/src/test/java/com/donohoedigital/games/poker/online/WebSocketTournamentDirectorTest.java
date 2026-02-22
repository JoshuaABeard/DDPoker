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
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assumptions;
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
 * Tests that require {@link RemotePokerTable} construction are skipped when
 * {@code PropertyConfig} is not initialized (outside a full game engine
 * context), using JUnit 5 assumptions — same pattern as
 * {@link RemotePokerTableTest}.
 */
class WebSocketTournamentDirectorTest {

    private WebSocketTournamentDirector wsTD;
    private PokerGame mockGame;
    private GameClock mockClock;
    private ObjectMapper mapper;
    private boolean tablesAvailable;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // Probe mock — clock must be configured so onGameState doesn't NPE inside
        // SwingUtilities.invokeLater when it calls game_.getGameClock().isRunning().
        mockGame = Mockito.mock(PokerGame.class);
        mockClock = Mockito.mock(GameClock.class);
        Mockito.when(mockGame.getGameClock()).thenReturn(mockClock);
        wsTD = new WebSocketTournamentDirector();
        wsTD.setGameForTest(mockGame);
        wsTD.setLocalPlayerIdForTest(1L);

        // Probe whether RemotePokerTable can be created (requires PropertyConfig).
        // If the table map stays empty after GAME_STATE dispatch, table-dependent
        // tests are skipped via requireTable().
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        tablesAvailable = wsTD.getTableCount() > 0;

        // Reset for individual tests
        mockGame = Mockito.mock(PokerGame.class);
        mockClock = Mockito.mock(GameClock.class);
        Mockito.when(mockGame.getGameClock()).thenReturn(mockClock);
        wsTD = new WebSocketTournamentDirector();
        wsTD.setGameForTest(mockGame);
        wsTD.setLocalPlayerIdForTest(1L);
    }

    /**
     * Returns the first table, or skips the current test if table construction
     * failed due to missing PropertyConfig.
     */
    private RemotePokerTable requireTable() {
        Assumptions.assumeTrue(tablesAvailable, "PropertyConfig not initialized; skipping table-dependent test");
        RemotePokerTable table = wsTD.getFirstTable();
        Assumptions.assumeTrue(table != null, "No table after GAME_STATE; skipping");
        return table;
    }

    // -------------------------------------------------------------------------
    // CONNECTED / GAME_STATE
    // -------------------------------------------------------------------------

    @Test
    void connectedSetsLocalPlayerIdAndBuildsTable() throws Exception {
        Assumptions.assumeTrue(tablesAvailable, "PropertyConfig not initialized; skipping");
        ObjectNode gameState = buildGameState(1, 0);
        ObjectNode root = mapper.createObjectNode();
        root.put("playerId", 1L);
        root.set("gameState", gameState);

        dispatch(ServerMessageType.CONNECTED, root);

        assertThat(wsTD.getTableCount()).isEqualTo(1);
    }

    @Test
    void gameStateSingleTableCreatesRemotePokerTable() throws Exception {
        Assumptions.assumeTrue(tablesAvailable, "PropertyConfig not initialized; skipping");
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));

        assertThat(wsTD.getTableCount()).isEqualTo(1);
        Mockito.verify(mockGame, Mockito.atLeastOnce()).addTable(Mockito.any());
    }

    @Test
    void gameStateSetsMinChipOnRemoteTable() throws Exception {
        // RemotePokerTable.nMinChip_ defaults to 0 which causes ArithmeticException
        // (/ by zero) in PokerUtils.roundAmountMinChip when the bet slider fires
        // ChangeEvent. The fix calls table.setMinChip(game_.getMinChip()) on creation.
        Assumptions.assumeTrue(tablesAvailable, "PropertyConfig not initialized; skipping");
        Mockito.when(mockGame.getMinChip()).thenReturn(25);
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));

        RemotePokerTable table = requireTable();
        assertThat(table.getMinChip()).isEqualTo(25);
    }

    @Test
    void gameStateMultipleTablesCreatesAllTables() throws Exception {
        Assumptions.assumeTrue(tablesAvailable, "PropertyConfig not initialized; skipping");
        ObjectNode gs = buildGameState(3, 0); // 3 tables
        dispatch(ServerMessageType.GAME_STATE, gs);

        assertThat(wsTD.getTableCount()).isEqualTo(3);
    }

    @Test
    void gameStateUpdatesGameLevel() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 3));

        // setLevel is called regardless of PropertyConfig (game_ is a mock)
        Mockito.verify(mockGame).setLevel(3);
    }

    @Test
    void gameStateRemovesTableNoLongerPresent() throws Exception {
        Assumptions.assumeTrue(tablesAvailable, "PropertyConfig not initialized; skipping");
        // First: 2 tables
        dispatch(ServerMessageType.GAME_STATE, buildGameState(2, 0));
        assertThat(wsTD.getTableCount()).isEqualTo(2);

        // Then: 1 table
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        assertThat(wsTD.getTableCount()).isEqualTo(1);
        Mockito.verify(mockGame, Mockito.atLeastOnce()).removeTable(Mockito.any());
    }

    // -------------------------------------------------------------------------
    // HAND_STARTED
    // -------------------------------------------------------------------------

    @Test
    void handStartedCreatesNewRemoteHoldemHand() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        RemotePokerTable table = requireTable();

        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        assertThat(table.getRemoteHand()).isNotNull();
        assertThat(table.getRemoteHand().getRound()).isEqualTo(BettingRound.PRE_FLOP);
        assertThat(table.getButton()).isEqualTo(0);
    }

    @Test
    void handStartedFiresNewHandAndButtonMovedEvents() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        RemotePokerTable table = requireTable();

        List<Integer> events = collectEvents(table);
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        assertThat(events).contains(PokerTableEvent.TYPE_NEW_HAND);
        assertThat(events).contains(PokerTableEvent.TYPE_BUTTON_MOVED);
    }

    @Test
    void handStartedFiresCleaningDoneToResetVisualState() throws Exception {
        // CLEANING_DONE must fire before each new hand so that visual elements from
        // eliminated players (hole cards, all-in indicators) are cleared by
        // ShowTournamentTable's existing TYPE_CLEANING_DONE handler.
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        RemotePokerTable table = requireTable();

        List<Integer> events = collectEvents(table);
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        assertThat(events).contains(PokerTableEvent.TYPE_CLEANING_DONE);
    }

    // -------------------------------------------------------------------------
    // COMMUNITY_CARDS_DEALT
    // -------------------------------------------------------------------------

    @Test
    void communityCardsDealtUpdatesRoundAndCards() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("round", "FLOP");
        payload.putArray("cards").add("As").add("Kd").add("Qc");
        payload.putArray("allCommunityCards").add("As").add("Kd").add("Qc");
        dispatch(ServerMessageType.COMMUNITY_CARDS_DEALT, payload);

        RemoteHoldemHand hand = requireTable().getRemoteHand();
        assertThat(hand.getRound()).isEqualTo(BettingRound.FLOP);
        assertThat(hand.getCommunity().size()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // ACTION_REQUIRED
    // -------------------------------------------------------------------------

    @Test
    void actionRequiredSetsCurrentPlayerToLocalPlayer() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        ObjectNode options = mapper.createObjectNode();
        options.put("canFold", true).put("canCheck", false).put("canCall", true).put("callAmount", 50)
                .put("canBet", false).put("minBet", 0).put("maxBet", 0).put("canRaise", false).put("minRaise", 0)
                .put("maxRaise", 0).put("canAllIn", true).put("allInAmount", 1000);
        ObjectNode payload = mapper.createObjectNode();
        payload.put("timeoutSeconds", 30);
        payload.set("options", options);
        dispatch(ServerMessageType.ACTION_REQUIRED, payload);

        RemoteHoldemHand hand = requireTable().getRemoteHand();
        assertThat(hand.getCurrentPlayerIndex())
                .isNotEqualTo(com.donohoedigital.games.poker.HoldemHand.NO_CURRENT_PLAYER);
        assertThat(wsTD.getCurrentOptions()).isNotNull();
        assertThat(wsTD.getCurrentOptions().canFold()).isTrue();
        assertThat(wsTD.getCurrentOptions().canCall()).isTrue();
    }

    @Test
    void actionRequiredFiresCurrentPlayerChangedWithCorrectIndices() throws Exception {
        // Local player (id=1) is at seat 1 (hand index 1), behind an AI at seat 0.
        // Before the fix, fireEvent(TYPE_CURRENT_PLAYER_CHANGED) produced getNew()=0,
        // so Odds panels would only update when the human happened to be at index 0.
        dispatch(ServerMessageType.GAME_STATE, buildGameStateTwoPlayers(2L, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        RemotePokerTable table = requireTable();

        // Capture full PokerTableEvent objects (not just types)
        List<PokerTableEvent> captured = new ArrayList<>();
        table.addPokerTableListener(e -> {
            if (e.getType() == PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED)
                captured.add(e);
        }, PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED);

        ObjectNode options = mapper.createObjectNode();
        options.put("canFold", true).put("canCheck", false).put("canCall", true).put("callAmount", 50)
                .put("canBet", false).put("minBet", 0).put("maxBet", 0).put("canRaise", false).put("minRaise", 0)
                .put("maxRaise", 0).put("canAllIn", true).put("allInAmount", 1000);
        ObjectNode payload = mapper.createObjectNode();
        payload.put("timeoutSeconds", 30);
        payload.set("options", options);
        dispatch(ServerMessageType.ACTION_REQUIRED, payload);

        assertThat(captured).isNotEmpty();
        PokerTableEvent evt = captured.get(captured.size() - 1);
        // Local player is at hand index 1, not 0. getNew() must reflect that.
        assertThat(evt.getNew()).isEqualTo(1);
        // Old index was NO_CURRENT_PLAYER (-1) set by onHandStarted.
        assertThat(evt.getOld()).isEqualTo(com.donohoedigital.games.poker.HoldemHand.NO_CURRENT_PLAYER);
    }

    @Test
    void actionRequiredSetsTimeoutOnLocalPlayer() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        ObjectNode options = mapper.createObjectNode();
        options.put("canFold", true).put("canCheck", true).put("canCall", false).put("callAmount", 0)
                .put("canBet", false).put("minBet", 0).put("maxBet", 0).put("canRaise", false).put("minRaise", 0)
                .put("maxRaise", 0).put("canAllIn", false).put("allInAmount", 0);
        ObjectNode payload = mapper.createObjectNode();
        payload.put("timeoutSeconds", 30);
        payload.set("options", options);
        dispatch(ServerMessageType.ACTION_REQUIRED, payload);

        PokerPlayer localPlayer = requireTable().getRemoteHand().getCurrentPlayer();
        assertThat(localPlayer).isNotNull();
        // 30s * 1000ms = 30000ms (stored internally in 100ms increments, so readable as
        // 30000)
        assertThat(localPlayer.getTimeoutMillis()).isEqualTo(30000);
        assertThat(localPlayer.getThinkBankMillis()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // PLAYER_ACTED
    // -------------------------------------------------------------------------

    @Test
    void playerActedUpdatesChipsAndPot() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("action", "CALL").put("amount", 50)
                .put("totalBet", 50).put("chipCount", 950).put("potTotal", 100);
        dispatch(ServerMessageType.PLAYER_ACTED, payload);

        RemoteHoldemHand hand = requireTable().getRemoteHand();
        assertThat(hand.getTotalPotChipCount()).isEqualTo(100);
        assertThat(hand.getCurrentPlayerIndex()).isEqualTo(com.donohoedigital.games.poker.HoldemHand.NO_CURRENT_PLAYER);
    }

    // -------------------------------------------------------------------------
    // ACTION_TIMEOUT
    // -------------------------------------------------------------------------

    @Test
    void actionTimeoutFiresPlayerActionEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("autoAction", "FOLD");
        dispatch(ServerMessageType.ACTION_TIMEOUT, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_PLAYER_ACTION);
    }

    // -------------------------------------------------------------------------
    // HAND_COMPLETE
    // -------------------------------------------------------------------------

    @Test
    void handCompleteFiresEndHandEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("handNumber", 1);
        payload.putArray("winners");
        payload.putArray("showdownPlayers");
        dispatch(ServerMessageType.HAND_COMPLETE, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_END_HAND);
    }

    @Test
    void handCompleteCreditsWinnerChips() throws Exception {
        wsTD.setLocalPlayerIdForTest(1L);
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        RemotePokerTable table = requireTable();

        // Player 1 starts with 1000 chips (from GAME_STATE); they win 300 from the pot.
        ObjectNode payload = mapper.createObjectNode();
        payload.put("handNumber", 1);
        var winners = payload.putArray("winners");
        ObjectNode w = mapper.createObjectNode();
        w.put("playerId", 1L).put("amount", 300).put("hand", "").put("potIndex", 0);
        w.putArray("cards");
        winners.add(w);
        payload.putArray("showdownPlayers");
        dispatch(ServerMessageType.HAND_COMPLETE, payload);

        assertThat(table.getPlayer(0).getChipCount()).isEqualTo(1300);
    }

    @Test
    void handCompleteClearsPotDisplay() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        RemotePokerTable table = requireTable();
        RemoteHoldemHand hand = table.getRemoteHand();
        hand.updatePot(500);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("handNumber", 1);
        payload.putArray("winners");
        payload.putArray("showdownPlayers");
        dispatch(ServerMessageType.HAND_COMPLETE, payload);

        assertThat(hand.getTotalPotChipCount()).isZero();
    }

    @Test
    void handStartedStoresSmallAndBigBlindSeats() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        RemoteHoldemHand hand = requireTable().getRemoteHand();
        assertThat(hand.getRemoteSmallBlindSeat()).isEqualTo(1);
        assertThat(hand.getRemoteBigBlindSeat()).isEqualTo(2);
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

    @Test
    void gameStateSetsBlindAmountsOnHand() throws Exception {
        // buildGameState includes blinds: {small:50, big:100, ante:0}
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        assertThat(table.getRemoteHand().getSmallBlind()).isEqualTo(50);
        assertThat(table.getRemoteHand().getBigBlind()).isEqualTo(100);
        assertThat(table.getRemoteHand().getAnte()).isEqualTo(0);
    }

    @Test
    void levelChangedUpdatesBlindAmountsOnHand() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("level", 3).put("smallBlind", 200).put("bigBlind", 400).put("ante", 50);
        payload.putNull("nextLevelIn");
        dispatch(ServerMessageType.LEVEL_CHANGED, payload);

        assertThat(table.getRemoteHand().getSmallBlind()).isEqualTo(200);
        assertThat(table.getRemoteHand().getBigBlind()).isEqualTo(400);
        assertThat(table.getRemoteHand().getAnte()).isEqualTo(50);
    }

    // -------------------------------------------------------------------------
    // PLAYER_JOINED / PLAYER_LEFT
    // -------------------------------------------------------------------------

    @Test
    void playerJoinedAddsPlayerToTable() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        RemotePokerTable table = requireTable();

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 99L).put("playerName", "NewPlayer").put("seatIndex", 5);
        dispatch(ServerMessageType.PLAYER_JOINED, payload);

        assertThat(table.getPlayer(5)).isNotNull();
        assertThat(table.getPlayer(5).getName()).isEqualTo("NewPlayer");
    }

    @Test
    void playerLeftRemovesPlayerFromTable() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();

        // Verify player 1 is at seat 0
        assertThat(table.getPlayer(0)).isNotNull();

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice");
        dispatch(ServerMessageType.PLAYER_LEFT, payload);

        assertThat(table.getPlayer(0)).isNull();
    }

    // -------------------------------------------------------------------------
    // PLAYER_REBUY / PLAYER_ADDON
    // -------------------------------------------------------------------------

    @Test
    void playerRebuyFiresRebuyEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("addedChips", 1500);
        dispatch(ServerMessageType.PLAYER_REBUY, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_PLAYER_REBUY);
    }

    @Test
    void playerAddonFiresAddonEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("addedChips", 1000);
        dispatch(ServerMessageType.PLAYER_ADDON, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_PLAYER_ADDON);
    }

    @Test
    void blindAllInUpdatesChipCountToZero() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        RemotePokerTable table = requireTable();

        // Player posts big blind but is all-in: chipCount=0 after posting.
        // The old guard (chipCount > 0) would have skipped this update.
        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("action", "BLIND_BIG").put("amount", 1000)
                .put("totalBet", 1000).put("chipCount", 0).put("potTotal", 1000);
        dispatch(ServerMessageType.PLAYER_ACTED, payload);

        assertThat(table.getPlayer(0).getChipCount()).isZero();
    }

    // -------------------------------------------------------------------------
    // GAME_COMPLETE
    // -------------------------------------------------------------------------

    @Test
    void gameCompleteSetWinnerPlaceToFirst() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();

        ObjectNode payload = mapper.createObjectNode();
        payload.putArray("standings");
        payload.put("totalHands", 5);
        payload.put("duration", 60000);
        dispatch(ServerMessageType.GAME_COMPLETE, payload);

        assertThat(table.getPlayer(0).getPlace()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // GAME_PAUSED / GAME_RESUMED
    // -------------------------------------------------------------------------

    @Test
    void gamePausedFiresStateChangedEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("reason", "user request").put("pausedBy", "Alice");
        dispatch(ServerMessageType.GAME_PAUSED, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_STATE_CHANGED);
    }

    @Test
    void gameResumedFiresStateChangedEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("resumedBy", "Alice");
        dispatch(ServerMessageType.GAME_RESUMED, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_STATE_CHANGED);
    }

    // -------------------------------------------------------------------------
    // PLAYER_KICKED
    // -------------------------------------------------------------------------

    @Test
    void playerKickedRemovesPlayerFromTable() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();
        assertThat(table.getPlayer(0)).isNotNull();

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("reason", "cheating");
        dispatch(ServerMessageType.PLAYER_KICKED, payload);

        assertThat(table.getPlayer(0)).isNull();
    }

    // -------------------------------------------------------------------------
    // SHOWDOWN_STARTED
    // -------------------------------------------------------------------------

    @Test
    void showdownStartedUpdatesRoundToShowdown() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("tableId", 1);
        dispatch(ServerMessageType.SHOWDOWN_STARTED, payload);

        RemoteHoldemHand hand = requireTable().getRemoteHand();
        assertThat(hand.getRound()).isEqualTo(BettingRound.SHOWDOWN);
    }

    // -------------------------------------------------------------------------
    // POT_AWARDED
    // -------------------------------------------------------------------------

    @Test
    void potAwardedFiresChipsChangedEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("potIndex", 0).put("amount", 300);
        payload.putArray("winnerIds").add(1L);
        dispatch(ServerMessageType.POT_AWARDED, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED);
    }

    // -------------------------------------------------------------------------
    // PLAYER_DISCONNECTED
    // -------------------------------------------------------------------------

    @Test
    void playerDisconnectedFiresPrefsChangedEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice");
        dispatch(ServerMessageType.PLAYER_DISCONNECTED, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_PREFS_CHANGED);
    }

    // -------------------------------------------------------------------------
    // PLAYER_ELIMINATED
    // -------------------------------------------------------------------------

    @Test
    void playerEliminatedFiresPlayerRemovedEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("finishPosition", 5).put("handsPlayed", 42);
        dispatch(ServerMessageType.PLAYER_ELIMINATED, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_PLAYER_REMOVED);
    }

    // -------------------------------------------------------------------------
    // REBUY_OFFERED / ADDON_OFFERED
    // -------------------------------------------------------------------------

    @Test
    void rebuyOfferedFiresRebuyPendingEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("cost", 100).put("chips", 1500).put("timeoutSeconds", 60);
        dispatch(ServerMessageType.REBUY_OFFERED, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_PLAYER_REBUY);
    }

    @Test
    void addonOfferedFiresAddonPendingEvent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("cost", 100).put("chips", 1000).put("timeoutSeconds", 60);
        dispatch(ServerMessageType.ADDON_OFFERED, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_PLAYER_ADDON);
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
    // TIMER_UPDATE
    // -------------------------------------------------------------------------

    @Test
    void timerUpdateFiresStateChangedOnCurrentTable() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("secondsRemaining", 25);
        dispatch(ServerMessageType.TIMER_UPDATE, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_STATE_CHANGED);
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
    void playerUpdateIsNoOp() {
        wsTD.playerUpdate(null, null); // no-op in M4
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
    // PLAYER_ACTED — all-in chip count (fix 1)
    // -------------------------------------------------------------------------

    @Test
    void betAllInUpdatesChipCountToZero() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        RemotePokerTable table = requireTable();

        // Player goes all-in via BET: server returns chipCount=0.
        // The old guard (chipCount > 0 || isFold || isBlindAnte) skipped this update.
        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("action", "BET").put("amount", 1000)
                .put("totalBet", 1000).put("chipCount", 0).put("potTotal", 1000);
        dispatch(ServerMessageType.PLAYER_ACTED, payload);

        assertThat(table.getPlayer(0).getChipCount()).isZero();
    }

    @Test
    void raiseAllInUpdatesChipCountToZero() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        RemotePokerTable table = requireTable();

        // Player goes all-in via RAISE: server returns chipCount=0.
        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("action", "RAISE").put("amount", 1000)
                .put("totalBet", 1000).put("chipCount", 0).put("potTotal", 1000);
        dispatch(ServerMessageType.PLAYER_ACTED, payload);

        assertThat(table.getPlayer(0).getChipCount()).isZero();
    }

    // -------------------------------------------------------------------------
    // GAME_PAUSED / GAME_RESUMED — GameClock pause/unpause (fix 2)
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
    // GAME_CANCELLED — navigates away from table (fix 4)
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

    @Test
    void gameCancelledFiresStateChangedEventWhenTablePresent() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0));
        RemotePokerTable table = requireTable();
        List<Integer> events = collectEvents(table);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("reason", "host left");
        dispatch(ServerMessageType.GAME_CANCELLED, payload);

        assertThat(events).contains(PokerTableEvent.TYPE_STATE_CHANGED);
    }

    // -------------------------------------------------------------------------
    // deliverChatLocal — routes to chatHandler (fix 5)
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
    // PLAYER_JOINED — clears old seat on consolidation (fix 6 client)
    // -------------------------------------------------------------------------

    @Test
    void playerJoinedClearsOldSeatBeforeAddingToNew() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        RemotePokerTable table = requireTable();

        // Player 1 is at seat 0 (from GAME_STATE). Simulate table consolidation:
        // PLAYER_JOINED arrives for player 1 at seat 3 of the same table (PLAYER_LEFT
        // was suppressed server-side for active players during consolidation).
        assertThat(table.getPlayer(0)).isNotNull();

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("seatIndex", 3).put("tableId", 1);
        dispatch(ServerMessageType.PLAYER_JOINED, payload);

        assertThat(table.getPlayer(0)).isNull(); // old seat cleared
        assertThat(table.getPlayer(3)).isNotNull(); // new seat filled
        assertThat(table.getPlayer(3).getName()).isEqualTo("Alice");
    }

    @Test
    void playerJoinedCrossTableConsolidationClearsOldTableSeat() throws Exception {
        // Build GAME_STATE with 2 tables: player 1 at table 1 seat 0; table 2 is empty.
        ObjectNode gs = mapper.createObjectNode();
        gs.put("status", "IN_PROGRESS");
        gs.put("level", 0);
        ObjectNode blinds = mapper.createObjectNode();
        blinds.put("small", 50).put("big", 100).put("ante", 0);
        gs.set("blinds", blinds);
        gs.putNull("nextLevelIn");
        var tables = gs.putArray("tables");

        ObjectNode t1 = mapper.createObjectNode();
        t1.put("tableId", 1);
        t1.putNull("currentRound");
        t1.put("handNumber", 0);
        t1.putArray("communityCards");
        t1.putArray("pots");
        var seats1 = t1.putArray("seats");
        ObjectNode seat1 = mapper.createObjectNode();
        seat1.put("seatIndex", 0).put("playerId", 1L).put("playerName", "Alice").put("chipCount", 1000)
                .put("status", "ACTIVE").put("isDealer", false).put("isSmallBlind", false).put("isBigBlind", false)
                .put("currentBet", 0).put("isCurrentActor", false);
        seat1.putArray("holeCards");
        seats1.add(seat1);
        tables.add(t1);

        ObjectNode t2 = mapper.createObjectNode();
        t2.put("tableId", 2);
        t2.putNull("currentRound");
        t2.put("handNumber", 0);
        t2.putArray("communityCards");
        t2.putArray("pots");
        t2.putArray("seats");
        tables.add(t2);

        gs.putArray("players");
        dispatch(ServerMessageType.GAME_STATE, gs);

        Assumptions.assumeTrue(tablesAvailable, "PropertyConfig not initialized; skipping table-dependent test");
        assertThat(wsTD.getTableCount()).isEqualTo(2);

        RemotePokerTable table1 = wsTD.getTableForTest(1);
        RemotePokerTable table2 = wsTD.getTableForTest(2);
        Assumptions.assumeTrue(table1 != null && table2 != null, "Tables not created; skipping");
        assertThat(table1.getPlayer(0)).isNotNull();

        // Consolidation: player 1 moves from table 1 seat 0 to table 2 seat 3.
        // PLAYER_LEFT was suppressed server-side for active players.
        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("seatIndex", 3).put("tableId", 2);
        dispatch(ServerMessageType.PLAYER_JOINED, payload);

        assertThat(table1.getPlayer(0)).isNull(); // old seat on table 1 cleared
        assertThat(table2.getPlayer(3)).isNotNull(); // new seat on table 2 filled
        assertThat(table2.getPlayer(3).getName()).isEqualTo("Alice");
    }

    // -------------------------------------------------------------------------
    // PLAYER_ACTED / ACTION_TIMEOUT — dealer chat (fix: action chat in practice
    // mode)
    // -------------------------------------------------------------------------

    @Test
    void playerActedSendsDealerChatToHandler() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        requireTable(); // skip if PropertyConfig not initialized

        List<int[]> received = new ArrayList<>();
        wsTD.setChatHandler((fromPlayerID, chatType, message) -> received.add(new int[]{fromPlayerID, chatType}));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("action", "CALL").put("amount", 100)
                .put("totalBet", 100).put("chipCount", 900).put("potTotal", 200).put("tableId", 1);
        dispatch(ServerMessageType.PLAYER_ACTED, payload);

        assertThat(received).hasSize(1);
        assertThat(received.get(0)[0]).isEqualTo(PokerConstants.CHAT_DEALER_MSG_ID);
        assertThat(received.get(0)[1]).isEqualTo(PokerConstants.CHAT_2);
    }

    @Test
    void actionTimeoutSendsDealerChatToHandler() throws Exception {
        dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
        dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
        requireTable(); // skip if PropertyConfig not initialized

        List<int[]> received = new ArrayList<>();
        wsTD.setChatHandler((fromPlayerID, chatType, message) -> received.add(new int[]{fromPlayerID, chatType}));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("playerId", 1L).put("playerName", "Alice").put("autoAction", "FOLD").put("tableId", 1);
        dispatch(ServerMessageType.ACTION_TIMEOUT, payload);

        assertThat(received).hasSize(1);
        assertThat(received.get(0)[0]).isEqualTo(PokerConstants.CHAT_DEALER_MSG_ID);
        assertThat(received.get(0)[1]).isEqualTo(PokerConstants.CHAT_2);
    }

    // -------------------------------------------------------------------------
    // resolveActionAmount — all-in button sends correct amount (fix)
    // -------------------------------------------------------------------------

    @Test
    void resolveActionAmountAllInUsesAllInAmountFromOptions() throws Exception {
        // Simulate server sending ACTION_REQUIRED with allInAmount=1500.
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

        // uiAmount=100 (spinner default/min), but ACTION_ALL_IN must use
        // allInAmount=1500.
        assertThat(wsTD.resolveActionAmount(PokerGame.ACTION_ALL_IN, 100)).isEqualTo(1500);
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
    void resolveActionAmountAllInWithNoOptionsUsesUiAmount() {
        // currentOptions_ is null (no ACTION_REQUIRED received yet) — must not throw,
        // must fall back to the uiAmount so the action is still submitted.
        assertThat(wsTD.resolveActionAmount(PokerGame.ACTION_ALL_IN, 500)).isEqualTo(500);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Dispatches a message and drains the Swing EDT. */
    private void dispatch(ServerMessageType type, ObjectNode data) throws Exception {
        WebSocketGameClient.InboundMessage msg = new WebSocketGameClient.InboundMessage(type, "test-game-id", data);
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

    /** Collects event types fired on a table while the test runs. */
    private static List<Integer> collectEvents(RemotePokerTable table) {
        List<Integer> events = new ArrayList<>();
        table.addPokerTableListener(e -> events.add(e.getType()), PokerTableEvent.TYPES_ALL);
        return events;
    }

    /**
     * Builds a single-table GAME_STATE with two players: {@code aiPlayerId} at seat
     * 0 and {@code localPlayerId} at seat 1. Used to test that ACTION_REQUIRED
     * fires TYPE_CURRENT_PLAYER_CHANGED with the correct non-zero hand index for
     * the local player.
     */
    private ObjectNode buildGameStateTwoPlayers(long aiPlayerId, long localPlayerId) {
        ObjectNode gs = mapper.createObjectNode();
        gs.put("status", "IN_PROGRESS");
        gs.put("level", 0);
        ObjectNode blinds = mapper.createObjectNode();
        blinds.put("small", 50).put("big", 100).put("ante", 0);
        gs.set("blinds", blinds);
        gs.putNull("nextLevelIn");

        ObjectNode t = mapper.createObjectNode();
        t.put("tableId", 1);
        t.put("currentRound", "PRE_FLOP");
        t.put("handNumber", 0);
        t.putArray("communityCards");
        t.putArray("pots");
        var seats = t.putArray("seats");

        ObjectNode seatAI = mapper.createObjectNode();
        seatAI.put("seatIndex", 0).put("playerId", aiPlayerId).put("playerName", "Bot").put("chipCount", 1000)
                .put("status", "ACTIVE").put("isDealer", true).put("isSmallBlind", false).put("isBigBlind", false)
                .put("currentBet", 0).put("isCurrentActor", false);
        seatAI.putArray("holeCards");
        seats.add(seatAI);

        ObjectNode seatLocal = mapper.createObjectNode();
        seatLocal.put("seatIndex", 1).put("playerId", localPlayerId).put("playerName", "Alice").put("chipCount", 1000)
                .put("status", "ACTIVE").put("isDealer", false).put("isSmallBlind", false).put("isBigBlind", false)
                .put("currentBet", 0).put("isCurrentActor", false);
        seatLocal.putArray("holeCards");
        seats.add(seatLocal);

        gs.putArray("tables").add(t);
        gs.putArray("players");
        return gs;
    }
}
