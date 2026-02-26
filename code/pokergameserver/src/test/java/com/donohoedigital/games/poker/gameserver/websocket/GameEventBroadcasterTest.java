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
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameStateSnapshot;
import com.donohoedigital.games.poker.gameserver.ServerGameTable;
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
}
