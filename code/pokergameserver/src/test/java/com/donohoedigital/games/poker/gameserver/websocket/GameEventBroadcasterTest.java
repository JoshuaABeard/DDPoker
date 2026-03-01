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
 * if not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.websocket;

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameStateSnapshot;
import com.donohoedigital.games.poker.gameserver.ServerGameTable;
import com.donohoedigital.games.poker.gameserver.ServerHand;
import com.donohoedigital.games.poker.gameserver.ServerPlayer;
import com.donohoedigital.games.poker.gameserver.ServerTournamentContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GameEventBroadcaster — verifies correct event routing.
 */
class GameEventBroadcasterTest {

    private GameConnectionManager connectionManager;
    private OutboundMessageConverter converter;
    private GameEventBroadcaster broadcaster;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        connectionManager = new GameConnectionManager();
        converter = new OutboundMessageConverter();
        broadcaster = new GameEventBroadcaster("game-1", connectionManager, converter);
    }

    private PlayerConnection makeConnectedPlayer(long profileId) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        PlayerConnection conn = new PlayerConnection(session, profileId, "player" + profileId, "game-1", objectMapper);
        connectionManager.addConnection("game-1", profileId, conn);
        return conn;
    }

    // ====================================
    // Event type dispatch tests
    // ====================================

    @Test
    void handStarted_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.HandStarted(0, 5));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void playerActed_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster
                .accept(new GameEvent.PlayerActed(0, 1, com.donohoedigital.games.poker.core.state.ActionType.FOLD, 0));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void communityCardsDealt_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(
                new GameEvent.CommunityCardsDealt(0, com.donohoedigital.games.poker.core.state.BettingRound.FLOP));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void levelChanged_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.LevelChanged(0, 3));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void tournamentCompleted_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.TournamentCompleted(1));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void handCompleted_broadcastsHandCompleteMessage() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);

        broadcaster.accept(new GameEvent.HandCompleted(0));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void potAwarded_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.PotAwarded(0, 0, new int[]{1}, 500));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void showdownStarted_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.ShowdownStarted(0));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void cleaningDone_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);

        broadcaster.accept(new GameEvent.CleaningDone(0));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void noConnectedPlayers_doesNotThrow() {
        // No exception when nobody is connected
        assertDoesNotThrow(() -> broadcaster.accept(new GameEvent.HandStarted(0, 1)));
    }

    @Test
    void broadcastsCorrectMessageType_handStarted() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.HandStarted(0, 5));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"HAND_STARTED\"") || json.contains("\"type\" : \"HAND_STARTED\"");
        }));
    }

    @Test
    void broadcastsCorrectMessageType_playerActed() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster
                .accept(new GameEvent.PlayerActed(0, 1, com.donohoedigital.games.poker.core.state.ActionType.CHECK, 0));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"PLAYER_ACTED\"") || json.contains("\"type\" : \"PLAYER_ACTED\"");
        }));
    }

    @Test
    void broadcastsCorrectMessageType_levelChanged() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.LevelChanged(0, 3));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"LEVEL_CHANGED\"") || json.contains("\"type\" : \"LEVEL_CHANGED\"");
        }));
    }

    @Test
    void broadcastsCorrectMessageType_potAwarded() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.PotAwarded(0, 0, new int[]{1}, 500));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"POT_AWARDED\"") || json.contains("\"type\" : \"POT_AWARDED\"");
        }));
    }

    @Test
    void broadcastsCorrectMessageType_showdownStarted() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.ShowdownStarted(0));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"SHOWDOWN_STARTED\"") || json.contains("\"type\" : \"SHOWDOWN_STARTED\"");
        }));
    }

    @Test
    void showdownStarted_1basedTableId_looksUpTableAtIndex0() throws Exception {
        // Regression test: table.getNumber() returns 1-based numbers, so
        // ShowdownStarted(1) must look up getTable(0), not getTable(1).
        // Before the fix, the bounds check "1 < getNumTables()=1" was false,
        // so showdownPlayers was always empty and AI cards were never revealed.
        makeConnectedPlayer(1L);

        GameInstance mockGame = mock(GameInstance.class);
        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        ServerGameTable mockTable = mock(ServerGameTable.class);
        when(mockTable.getNumSeats()).thenReturn(0); // no players in hand
        when(mockCtx.getNumTables()).thenReturn(1);
        when(mockCtx.getTable(0)).thenReturn(mockTable); // 0-based index for table number 1
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster broadcastWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                mockGame);
        broadcastWithGame.accept(new GameEvent.ShowdownStarted(1)); // 1-based table number

        // Verify the lookup was attempted with the correct 0-based index
        verify(mockCtx).getTable(0);
    }

    @Test
    void playerEliminated_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.PlayerEliminated(0, 1, 4));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void playerActed_withGameReference_populatesPlayerName() throws Exception {
        // Create a real player and table so name lookup works
        ServerPlayer player = new ServerPlayer(42, "TestPlayer", true, 0, 5000);
        player.setSeat(0);
        ServerGameTable sgt = new ServerGameTable(1, 2, null, 50, 100, 0);
        sgt.addPlayer(player, 0);

        ServerTournamentContext tournament = mock(ServerTournamentContext.class);
        when(tournament.getNumTables()).thenReturn(1);
        when(tournament.getTable(0)).thenReturn(sgt); // 0-based index for table number 1

        GameInstance game = mock(GameInstance.class);
        when(game.getTournament()).thenReturn(tournament);

        GameEventBroadcaster broadcasterWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                game);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 99L,
                new PlayerConnection(session, 99L, "observer", "game-1", objectMapper));

        broadcasterWithGame
                .accept(new GameEvent.PlayerActed(1, 42, com.donohoedigital.games.poker.core.state.ActionType.FOLD, 0));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("TestPlayer");
        }));
    }

    @Test
    void broadcastsCorrectMessageType_playerEliminated() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.PlayerEliminated(0, 1, 4));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"PLAYER_ELIMINATED\"") || json.contains("\"type\" : \"PLAYER_ELIMINATED\"");
        }));
    }

    // ====================================
    // Fix 3: null-game broadcaster still broadcasts (reconnect path)
    // ====================================

    @Test
    void nullGameBroadcaster_playerRemoved_sendsPlayerLeft() throws Exception {
        // Broadcaster without game (null-game constructor) always sends PLAYER_LEFT.
        // This verifies the reconnect-path broadcaster (fix 3) doesn't break
        // PlayerRemoved.
        PlayerConnection p1 = makeConnectedPlayer(1L);

        broadcaster.accept(new GameEvent.PlayerRemoved(0, 42, 3));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
    }

    // ====================================
    // Fix 6 server: PlayerRemoved suppression during consolidation
    // ====================================

    @Test
    void playerRemoved_activePlayer_sendsPlayerMoved() throws Exception {
        // Active player (finishPosition=0) being consolidated: PLAYER_MOVED sent.
        PlayerConnection p1 = makeConnectedPlayer(1L);

        GameInstance mockGame = mock(GameInstance.class);
        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        ServerPlayer activePlayer = mock(ServerPlayer.class);
        when(activePlayer.getID()).thenReturn(42);
        when(activePlayer.getFinishPosition()).thenReturn(0);
        when(activePlayer.getName()).thenReturn("TestPlayer");
        when(mockCtx.getAllPlayers()).thenReturn(List.of(activePlayer));
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster broadcastWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                mockGame);
        broadcastWithGame.accept(new GameEvent.PlayerRemoved(0, 42, 3));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(p1.getSession()).sendMessage(captor.capture());
        assertTrue(captor.getValue().getPayload().contains("PLAYER_MOVED"));
    }

    @Test
    void playerRemoved_eliminatedPlayer_sendsPlayerLeft() throws Exception {
        // Eliminated player (finishPosition>0): PLAYER_LEFT sent to clear seat.
        PlayerConnection p1 = makeConnectedPlayer(1L);

        GameInstance mockGame = mock(GameInstance.class);
        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        ServerPlayer eliminatedPlayer = mock(ServerPlayer.class);
        when(eliminatedPlayer.getID()).thenReturn(42);
        when(eliminatedPlayer.getFinishPosition()).thenReturn(5);
        when(mockCtx.getAllPlayers()).thenReturn(List.of(eliminatedPlayer));
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster broadcastWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                mockGame);
        broadcastWithGame.accept(new GameEvent.PlayerRemoved(0, 42, 3));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
    }

    // ====================================
    // Fix 6 server: PlayerAdded includes player name
    // ====================================

    @Test
    void playerAdded_withGame_includesPlayerName() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        GameInstance mockGame = mock(GameInstance.class);
        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        ServerGameTable mockTable = mock(ServerGameTable.class);
        ServerPlayer namedPlayer = mock(ServerPlayer.class);
        when(namedPlayer.getName()).thenReturn("Alice");
        when(mockTable.getPlayer(3)).thenReturn(namedPlayer);
        when(mockCtx.getNumTables()).thenReturn(1);
        when(mockCtx.getTable(0)).thenReturn(mockTable); // 0-based index for table number 1
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster broadcastWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                mockGame);
        broadcastWithGame.accept(new GameEvent.PlayerAdded(1, 42, 3)); // 1-based table number

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("Alice");
        }));
    }

    @Test
    void playerAdded_withoutGame_sendsEmptyName() throws Exception {
        // Broadcaster without game falls back to empty player name.
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.PlayerAdded(0, 42, 3));

        verify(session).sendMessage(any(TextMessage.class));
    }

    // ====================================
    // HandStarted ordering: GAME_STATE before HAND_STARTED
    // ====================================

    /**
     * Guards the invariant that GAME_STATE is sent before HAND_STARTED in the
     * broadcaster's HandStarted handler. The GameWebSocketHandler no longer sends
     * an extra GAME_STATE after startGame() (which raced with the broadcaster); the
     * broadcaster's pre-HAND_STARTED snapshot is now the sole mechanism that
     * populates the client's tables_ map before ACTION_REQUIRED arrives.
     */
    @Test
    void handStarted_withGame_sendsGameStateBeforeHandStarted() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        long profileId = 7L;
        connectionManager.addConnection("game-1", profileId,
                new PlayerConnection(session, profileId, "alice", "game-1", objectMapper));

        // Mock game returning a snapshot with tableId=1 (matches HandStarted tableId
        // below)
        GameStateSnapshot snapshot = new GameStateSnapshot(1, 3, null, null, List.of(), List.of(), -1, -1, -1, -1,
                "PRE_FLOP", 1, 25, 50, 0, 0, 0, 0, 0);
        GameInstance mockGame = mock(GameInstance.class);
        when(mockGame.getGameStateSnapshot(profileId)).thenReturn(snapshot);

        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        when(mockCtx.getNumTables()).thenReturn(1);
        ServerGameTable mockTable = mock(ServerGameTable.class);
        when(mockTable.getNumSeats()).thenReturn(0);
        when(mockCtx.getTable(0)).thenReturn(mockTable);
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster broadcasterWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                mockGame);
        broadcasterWithGame.accept(new GameEvent.HandStarted(1, 3)); // 1-based tableId

        // Capture all messages sent, in order
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeast(2)).sendMessage(captor.capture());
        List<TextMessage> messages = captor.getAllValues();

        String first = messages.get(0).getPayload();
        String second = messages.get(1).getPayload();
        assertTrue(first.contains("GAME_STATE"),
                "First message must be GAME_STATE so tables_ is populated before hand state; got: " + first);
        assertTrue(second.contains("HAND_STARTED"), "Second message must be HAND_STARTED; got: " + second);
    }

    // ====================================
    // Bug 3: ACTION_TIMEOUT must use 1-based tableId
    // ====================================

    @Test
    void actionTimeout_sendsOneBasedTableId_forPlayerAtTable() throws Exception {
        // Player 7 is seated at table 1 (number=1, stored at index 0 in the
        // tournament).
        // The broadcaster loops with 0-based index t, so it must send tableId = t+1 =
        // 1.
        ServerPlayer player = new ServerPlayer(7, "TimedOut", true, 0, 5000);
        player.setSeat(0);
        ServerGameTable sgt = new ServerGameTable(1, 6, null, 25, 50, 0);
        sgt.addPlayer(player, 0);

        ServerTournamentContext tournament = mock(ServerTournamentContext.class);
        when(tournament.getNumTables()).thenReturn(1);
        when(tournament.getTable(0)).thenReturn(sgt);

        GameInstance game = mock(GameInstance.class);
        when(game.getTournament()).thenReturn(tournament);

        GameEventBroadcaster b = new GameEventBroadcaster("game-1", connectionManager, converter, game);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 99L,
                new PlayerConnection(session, 99L, "watcher", "game-1", objectMapper));

        b.accept(new GameEvent.ActionTimeout(7, ActionType.FOLD));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String json = captor.getValue().getPayload();
        JsonNode data = objectMapper.readTree(json).get("data");
        assertEquals(1, data.get("tableId").asInt(),
                "ACTION_TIMEOUT must use 1-based tableId matching table.getNumber(), not 0-based loop index");
    }

    // ====================================
    // Bug 4: shutdown() must stop the timerScheduler
    // ====================================

    @Test
    void shutdown_cancelsActiveTimerAndStopsScheduler() throws Exception {
        makeConnectedPlayer(1L);
        broadcaster.startActionTimer(1L, 60);
        // shutdown() must not throw, even when a timer task is in flight
        assertDoesNotThrow(() -> broadcaster.shutdown());
        // cancelActionTimer() after shutdown must also be safe (no active task)
        assertDoesNotThrow(() -> broadcaster.cancelActionTimer());
    }

    // ====================================
    // Bug 2: per-player sends during HandStarted must have null sequenceNumber
    // ====================================

    @Test
    void handStarted_perPlayerMessages_haveNullSequenceNumber() throws Exception {
        // Per-player GAME_STATE / HAND_STARTED / HOLE_CARDS_DEALT messages sent during
        // HandStarted must carry null sequenceNumber so they are excluded from the
        // client's gap-detection counter. Without this, players who receive their
        // per-player batch early see a gap when the next broadcast() arrives.
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        long profileId = 7L;
        connectionManager.addConnection("game-1", profileId,
                new PlayerConnection(session, profileId, "alice", "game-1", objectMapper));

        GameStateSnapshot snapshot = new GameStateSnapshot(1, 3, null, null, List.of(), List.of(), -1, -1, -1, -1,
                "PRE_FLOP", 1, 25, 50, 0, 0, 0, 0, 0);
        GameInstance mockGame = mock(GameInstance.class);
        when(mockGame.getGameStateSnapshot(profileId)).thenReturn(snapshot);

        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        when(mockCtx.getNumTables()).thenReturn(1);
        ServerGameTable mockTable = mock(ServerGameTable.class);
        when(mockTable.getNumSeats()).thenReturn(0);
        when(mockCtx.getTable(0)).thenReturn(mockTable);
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster broadcasterWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                mockGame);
        broadcasterWithGame.accept(new GameEvent.HandStarted(1, 3));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeast(1)).sendMessage(captor.capture());
        for (TextMessage msg : captor.getAllValues()) {
            JsonNode root = objectMapper.readTree(msg.getPayload());
            assertTrue(root.get("sequenceNumber").isNull(),
                    "Per-player message sent during HAND_STARTED must have null sequenceNumber to avoid "
                            + "false gap detection; type=" + root.get("type").asText() + " seq="
                            + root.get("sequenceNumber"));
        }
    }

    // ====================================
    // Bug A: AllInRunoutPaused NPE when game is null
    // ====================================

    @Test
    void allInRunoutPaused_withNullGame_doesNotThrowAndSendsContinueRunout() throws Exception {
        // Reconnect-path broadcaster has null game. When AllInRunoutPaused fires it
        // must NOT throw NullPointerException and must still deliver CONTINUE_RUNOUT
        // to the connected human player.
        PlayerConnection player = makeConnectedPlayer(99L);

        assertDoesNotThrow(() -> broadcaster.accept(new GameEvent.AllInRunoutPaused(1)));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(player.getSession()).sendMessage(captor.capture());
        assertTrue(captor.getValue().getPayload().contains("CONTINUE_RUNOUT"));
    }

    @Test
    void allInRunoutPaused_withGame_sendsOnlyToOwner() throws Exception {
        // When a game reference is present, CONTINUE_RUNOUT goes only to the owner.
        PlayerConnection owner = makeConnectedPlayer(7L);
        PlayerConnection other = makeConnectedPlayer(8L);

        GameInstance mockGame = mock(GameInstance.class);
        when(mockGame.getOwnerProfileId()).thenReturn(7L);

        GameEventBroadcaster b = new GameEventBroadcaster("game-1", connectionManager, converter, mockGame);
        b.accept(new GameEvent.AllInRunoutPaused(1));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(owner.getSession()).sendMessage(captor.capture());
        assertTrue(captor.getValue().getPayload().contains("CONTINUE_RUNOUT"));
        verify(other.getSession(), never()).sendMessage(any());
    }

    // ====================================
    // Bug B: ShowdownStarted must use int getPlayerCards overload
    // ====================================

    @Test
    void showdownStarted_withGame_includesPlayerCards() throws Exception {
        // Regression: ShowdownStarted must broadcast player hole cards when they
        // exist. The GamePlayerInfo overload of getPlayerCards returns null for empty
        // hands; the int overload returns an empty list. Both reach cardsToList the
        // same way, but using the int overload (matching HAND_COMPLETE) avoids the
        // null-return path and is consistent.
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L,
                new PlayerConnection(session, 1L, "watcher", "game-1", objectMapper));

        ServerPlayer player = new ServerPlayer(42, "Alice", false, 0, 5000);
        player.setSeat(0);

        ServerGameTable mockTable = mock(ServerGameTable.class);
        when(mockTable.getNumSeats()).thenReturn(1);
        when(mockTable.getPlayer(0)).thenReturn(player);

        // Set up a ServerHand with cards for player 42.
        // Mock Card objects to avoid PropertyConfig initialization.
        ServerHand mockHand = mock(ServerHand.class);
        CardSuit mockSuit = mock(CardSuit.class);
        when(mockSuit.getAbbr()).thenReturn("s");
        Card aceSpades = mock(Card.class);
        when(aceSpades.getRankDisplaySingle()).thenReturn("A");
        when(aceSpades.getCardSuit()).thenReturn(mockSuit);
        Card kingSpades = mock(Card.class);
        when(kingSpades.getRankDisplaySingle()).thenReturn("K");
        when(kingSpades.getCardSuit()).thenReturn(mockSuit);
        // int overload — returns actual cards (never null)
        when(mockHand.getPlayerCards(42)).thenReturn(List.of(aceSpades, kingSpades));
        // GamePlayerInfo overload — returns null (the old buggy path)
        when(mockHand.getPlayerCards(any(ServerPlayer.class))).thenReturn(null);

        when(mockTable.getHoldemHand()).thenReturn(mockHand);

        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        when(mockCtx.getNumTables()).thenReturn(1);
        when(mockCtx.getTable(0)).thenReturn(mockTable);

        GameInstance mockGame = mock(GameInstance.class);
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster b = new GameEventBroadcaster("game-1", connectionManager, converter, mockGame);
        b.accept(new GameEvent.ShowdownStarted(1)); // 1-based tableId

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        JsonNode data = objectMapper.readTree(captor.getValue().getPayload()).get("data");
        JsonNode players = data.get("showdownPlayers");
        assertFalse(players.isEmpty(), "showdownPlayers must not be empty");
        assertFalse(players.get(0).get("cards").isEmpty(),
                "Player cards must be non-empty in SHOWDOWN_STARTED; got: " + players.get(0));
    }

    // ====================================
    // Bug C: cancelActionTimer must be idempotent (no orphaned task on re-entry)
    // ====================================

    // ====================================
    // ADVISOR_UPDATE: sent to human players after key events
    // ====================================

    /**
     * Helper: create a mock game with one human and one AI player at table 1, both
     * with hole cards, and a ServerHand with configurable community cards. Returns
     * the broadcaster. The human player is ID=1 (profileId=1), AI is ID=2. Uses
     * real Card objects so AdvisorService hand evaluation works correctly.
     */
    private GameEventBroadcaster createAdvisorTestBroadcaster(Card[] communityCards) throws Exception {
        // Human player (ID=1), AI player (ID=2)
        ServerPlayer humanPlayer = new ServerPlayer(1, "Human", true, 0, 5000);
        humanPlayer.setSeat(0);
        ServerPlayer aiPlayer = new ServerPlayer(2, "Bot", false, 5, 5000);
        aiPlayer.setSeat(1);

        ServerHand mockHand = mock(ServerHand.class);
        when(mockHand.getPlayerCards(1)).thenReturn(List.of(Card.HEARTS_A, Card.HEARTS_K));
        when(mockHand.getPlayerCards(2)).thenReturn(List.of(Card.SPADES_A, Card.SPADES_K));
        when(mockHand.getCommunityCards()).thenReturn(communityCards);
        when(mockHand.getPotSize()).thenReturn(150);
        when(mockHand.getAmountToCall(humanPlayer)).thenReturn(50);

        ServerGameTable mockTable = mock(ServerGameTable.class);
        when(mockTable.getNumSeats()).thenReturn(2);
        when(mockTable.getPlayer(0)).thenReturn(humanPlayer);
        when(mockTable.getPlayer(1)).thenReturn(aiPlayer);
        when(mockTable.getHoldemHand()).thenReturn(mockHand);
        when(mockTable.getButton()).thenReturn(0);

        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        when(mockCtx.getNumTables()).thenReturn(1);
        when(mockCtx.getTable(0)).thenReturn(mockTable);

        GameInstance mockGame = mock(GameInstance.class);
        when(mockGame.getTournament()).thenReturn(mockCtx);

        // Human player at profileId=1, connected to game
        GameStateSnapshot snapshot = new GameStateSnapshot(1, 1, null, null, List.of(), List.of(), 0, -1, -1, -1,
                "PRE_FLOP", 1, 25, 50, 0, 0, 0, 0, 0);
        when(mockGame.getGameStateSnapshot(1L)).thenReturn(snapshot);

        return new GameEventBroadcaster("game-1", connectionManager, converter, mockGame);
    }

    @Test
    void advisorUpdate_sentToHumanPlayer_afterHandStarted() throws Exception {
        // Connect a human player
        WebSocketSession humanSession = mock(WebSocketSession.class);
        when(humanSession.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L,
                new PlayerConnection(humanSession, 1L, "Human", "game-1", objectMapper));

        GameEventBroadcaster b = createAdvisorTestBroadcaster(null);
        b.accept(new GameEvent.HandStarted(1, 1));

        // Verify ADVISOR_UPDATE was sent to the human player
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(humanSession, atLeast(1)).sendMessage(captor.capture());

        boolean foundAdvisor = captor.getAllValues().stream()
                .anyMatch(msg -> msg.getPayload().contains("ADVISOR_UPDATE"));
        assertTrue(foundAdvisor, "Human player should receive ADVISOR_UPDATE after HAND_STARTED");
    }

    @Test
    void advisorUpdate_notSentToAiPlayer() throws Exception {
        // Connect both human (profileId=1) and AI (profileId=2)
        WebSocketSession humanSession = mock(WebSocketSession.class);
        when(humanSession.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L,
                new PlayerConnection(humanSession, 1L, "Human", "game-1", objectMapper));

        WebSocketSession aiSession = mock(WebSocketSession.class);
        when(aiSession.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 2L,
                new PlayerConnection(aiSession, 2L, "Bot", "game-1", objectMapper));

        // Use PLAYER_ACTED which broadcasts to all, then sends advisor privately.
        // AI should receive the broadcast PLAYER_ACTED but NOT ADVISOR_UPDATE.
        GameEventBroadcaster b = createAdvisorTestBroadcaster(null);
        b.accept(new GameEvent.PlayerActed(1, 2, ActionType.CALL, 50));

        // AI player should receive PLAYER_ACTED broadcast but NOT ADVISOR_UPDATE
        ArgumentCaptor<TextMessage> aiCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(aiSession, atLeast(1)).sendMessage(aiCaptor.capture());
        boolean aiGotAdvisor = aiCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.getPayload().contains("ADVISOR_UPDATE"));
        assertFalse(aiGotAdvisor, "AI player must NOT receive ADVISOR_UPDATE");
    }

    @Test
    void advisorUpdate_sentAfterCommunityCardsDealt() throws Exception {
        WebSocketSession humanSession = mock(WebSocketSession.class);
        when(humanSession.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L,
                new PlayerConnection(humanSession, 1L, "Human", "game-1", objectMapper));

        // getCommunityCards returns null; the broadcaster handles null gracefully for
        // both the broadcast payload and the advisor computation.
        GameEventBroadcaster b = createAdvisorTestBroadcaster(null);
        b.accept(new GameEvent.CommunityCardsDealt(1, BettingRound.FLOP));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(humanSession, atLeast(1)).sendMessage(captor.capture());

        boolean foundAdvisor = captor.getAllValues().stream()
                .anyMatch(msg -> msg.getPayload().contains("ADVISOR_UPDATE"));
        assertTrue(foundAdvisor, "Human player should receive ADVISOR_UPDATE after COMMUNITY_CARDS_DEALT");
    }

    @Test
    void advisorUpdate_sentAfterPlayerActed() throws Exception {
        WebSocketSession humanSession = mock(WebSocketSession.class);
        when(humanSession.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L,
                new PlayerConnection(humanSession, 1L, "Human", "game-1", objectMapper));

        GameEventBroadcaster b = createAdvisorTestBroadcaster(null);
        b.accept(new GameEvent.PlayerActed(1, 2, ActionType.CALL, 50));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(humanSession, atLeast(1)).sendMessage(captor.capture());

        boolean foundAdvisor = captor.getAllValues().stream()
                .anyMatch(msg -> msg.getPayload().contains("ADVISOR_UPDATE"));
        assertTrue(foundAdvisor, "Human player should receive ADVISOR_UPDATE after PLAYER_ACTED");
    }

    @Test
    void advisorUpdate_notSentToFoldedPlayer() throws Exception {
        // Folded human player (ID=1), active AI player (ID=2)
        ServerPlayer foldedHuman = new ServerPlayer(1, "Folded", true, 0, 5000);
        foldedHuman.setSeat(0);
        foldedHuman.setFolded(true);
        ServerPlayer aiPlayer = new ServerPlayer(2, "Bot", false, 5, 5000);
        aiPlayer.setSeat(1);

        ServerHand mockHand = mock(ServerHand.class);
        when(mockHand.getPlayerCards(1)).thenReturn(List.of(Card.HEARTS_A, Card.HEARTS_K));
        when(mockHand.getPlayerCards(2)).thenReturn(List.of(Card.SPADES_A, Card.SPADES_K));
        when(mockHand.getCommunityCards()).thenReturn(null);
        when(mockHand.getPotSize()).thenReturn(100);

        ServerGameTable mockTable = mock(ServerGameTable.class);
        when(mockTable.getNumSeats()).thenReturn(2);
        when(mockTable.getPlayer(0)).thenReturn(foldedHuman);
        when(mockTable.getPlayer(1)).thenReturn(aiPlayer);
        when(mockTable.getHoldemHand()).thenReturn(mockHand);

        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        when(mockCtx.getNumTables()).thenReturn(1);
        when(mockCtx.getTable(0)).thenReturn(mockTable);

        GameInstance mockGame = mock(GameInstance.class);
        when(mockGame.getTournament()).thenReturn(mockCtx);

        WebSocketSession humanSession = mock(WebSocketSession.class);
        when(humanSession.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L,
                new PlayerConnection(humanSession, 1L, "Folded", "game-1", objectMapper));

        GameEventBroadcaster b = new GameEventBroadcaster("game-1", connectionManager, converter, mockGame);
        b.accept(new GameEvent.CommunityCardsDealt(1, BettingRound.FLOP));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(humanSession, atLeast(1)).sendMessage(captor.capture());

        boolean foundAdvisor = captor.getAllValues().stream()
                .anyMatch(msg -> msg.getPayload().contains("ADVISOR_UPDATE"));
        assertFalse(foundAdvisor, "Folded human player must NOT receive ADVISOR_UPDATE");
    }

    @Test
    void advisorUpdate_containsExpectedFields() throws Exception {
        WebSocketSession humanSession = mock(WebSocketSession.class);
        when(humanSession.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L,
                new PlayerConnection(humanSession, 1L, "Human", "game-1", objectMapper));

        GameEventBroadcaster b = createAdvisorTestBroadcaster(null);
        b.accept(new GameEvent.PlayerActed(1, 2, ActionType.CALL, 50));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(humanSession, atLeast(1)).sendMessage(captor.capture());

        String advisorJson = captor.getAllValues().stream().map(TextMessage::getPayload)
                .filter(p -> p.contains("ADVISOR_UPDATE")).findFirst()
                .orElseThrow(() -> new AssertionError("No ADVISOR_UPDATE message found"));

        JsonNode root = objectMapper.readTree(advisorJson);
        assertEquals("ADVISOR_UPDATE", root.get("type").asText());
        JsonNode data = root.get("data");
        assertNotNull(data.get("handRank"));
        assertNotNull(data.get("equity"));
        assertNotNull(data.get("potOdds"));
        assertNotNull(data.get("recommendation"));
        // Pre-flop should have starting hand data
        assertNotNull(data.get("startingHandCategory"));
        assertNotNull(data.get("startingHandNotation"));
    }

    @Test
    void sendAdvisorUpdates_onFlop_includesImprovementOddsAndPotential() throws Exception {
        // Flop scenario: 3 community cards on the board so improvement odds and
        // potential are computed (non-null). Verify that the ADVISOR_UPDATE message
        // includes the improvementOdds map as well as positivePotential and
        // negativePotential fields.
        //
        // Triggered via PLAYER_ACTED so that community cards are only passed to
        // AdvisorService.compute() and never through cardsToList(), which would
        // require PropertyConfig to be initialised (unavailable in unit tests).
        Card[] flopCards = {Card.CLUBS_2, Card.DIAMONDS_7, Card.HEARTS_J};

        WebSocketSession humanSession = mock(WebSocketSession.class);
        when(humanSession.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L,
                new PlayerConnection(humanSession, 1L, "Human", "game-1", objectMapper));

        GameEventBroadcaster b = createAdvisorTestBroadcaster(flopCards);
        b.accept(new GameEvent.PlayerActed(1, 2, ActionType.CALL, 50));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(humanSession, atLeast(1)).sendMessage(captor.capture());

        String advisorJson = captor.getAllValues().stream().map(TextMessage::getPayload)
                .filter(p -> p.contains("ADVISOR_UPDATE")).findFirst()
                .orElseThrow(() -> new AssertionError("No ADVISOR_UPDATE message found"));

        JsonNode data = objectMapper.readTree(advisorJson).get("data");
        // On the flop, improvementOdds must be a non-null object (may be empty if no
        // improvements apply, but the field must be present and not null-valued)
        assertNotNull(data.get("improvementOdds"),
                "improvementOdds field must be present in ADVISOR_UPDATE on the flop");
        assertFalse(data.get("improvementOdds").isNull(), "improvementOdds must not be JSON null on the flop");
        // positivePotential and negativePotential must be present and non-null
        assertNotNull(data.get("positivePotential"),
                "positivePotential field must be present in ADVISOR_UPDATE on the flop");
        assertFalse(data.get("positivePotential").isNull(), "positivePotential must not be JSON null on the flop");
        assertNotNull(data.get("negativePotential"),
                "negativePotential field must be present in ADVISOR_UPDATE on the flop");
        assertFalse(data.get("negativePotential").isNull(), "negativePotential must not be JSON null on the flop");
    }

    @Test
    void cancelActionTimer_isIdempotentAfterStartAndCancel() throws Exception {
        // Regression guard for the TOCTOU race: calling cancelActionTimer twice
        // (e.g. once from the timer callback and once from startActionTimer) must
        // not throw and must leave the broadcaster in a clean state.
        assertDoesNotThrow(() -> {
            broadcaster.startActionTimer(1L, 60);
            broadcaster.cancelActionTimer();
            broadcaster.cancelActionTimer(); // second call — must not NPE or corrupt state
        });
        // After double-cancel a fresh startActionTimer must work
        assertDoesNotThrow(() -> broadcaster.startActionTimer(2L, 30));
        broadcaster.shutdown();
    }
}
