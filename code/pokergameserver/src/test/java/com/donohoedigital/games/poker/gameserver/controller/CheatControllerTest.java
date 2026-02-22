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
package com.donohoedigital.games.poker.gameserver.controller;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.PracticeConfig;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.ServerGameEventBus;
import com.donohoedigital.games.poker.gameserver.ServerGameTable;
import com.donohoedigital.games.poker.gameserver.ServerHand;
import com.donohoedigital.games.poker.gameserver.ServerPlayer;
import com.donohoedigital.games.poker.gameserver.ServerTournamentContext;
import com.donohoedigital.games.poker.gameserver.websocket.GameConnectionManager;
import com.donohoedigital.games.poker.gameserver.websocket.OutboundMessageConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link CheatController}.
 *
 * <p>
 * All tests use the test JWT (profileId=1L, username="testuser") injected by
 * {@link TestSecurityConfiguration}.
 */
@WebMvcTest
@Import({TestSecurityConfiguration.class, CheatController.class, GameServerExceptionHandler.class})
class CheatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameInstanceManager gameInstanceManager;

    @MockitoBean
    private GameConnectionManager connectionManager;

    @MockitoBean
    private OutboundMessageConverter converter;

    // -------------------------------------------------------------------------
    // POST /api/v1/games/{id}/cheat/chips
    // -------------------------------------------------------------------------

    @Test
    void changeChips_success() throws Exception {
        GameInstance game = stubPracticeGame("g1", 1L);
        ServerPlayer player = stubPlayer(game, -1);

        mockMvc.perform(post("/api/v1/games/g1/cheat/chips").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":-1,\"chipCount\":5000}")).andExpect(status().isOk());

        verify(player).setChipCount(5000);
    }

    @Test
    void changeChips_negativeCount_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/games/g1a/cheat/chips").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":-1,\"chipCount\":-100}")).andExpect(status().isBadRequest());
    }

    @Test
    void changeChips_notPractice_returns422() throws Exception {
        stubNonPracticeGame("g2", 1L);

        mockMvc.perform(post("/api/v1/games/g2/cheat/chips").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":-1,\"chipCount\":5000}")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changeChips_notOwner_returns403() throws Exception {
        stubPracticeGame("g3", 99L); // owner is 99, caller is 1

        mockMvc.perform(post("/api/v1/games/g3/cheat/chips").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":-1,\"chipCount\":5000}")).andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/games/{id}/cheat/name
    // -------------------------------------------------------------------------

    @Test
    void changeName_success() throws Exception {
        GameInstance game = stubPracticeGame("g4", 1L);
        ServerPlayer player = stubPlayer(game, -1);

        mockMvc.perform(post("/api/v1/games/g4/cheat/name").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":-1,\"name\":\"Crusher\"}")).andExpect(status().isOk());

        verify(player).setName("Crusher");
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/games/{id}/cheat/level
    // -------------------------------------------------------------------------

    @Test
    void changeLevel_success() throws Exception {
        GameInstance game = stubPracticeGame("g5", 1L);
        ServerTournamentContext tournament = (ServerTournamentContext) mock(ServerTournamentContext.class);
        when(game.getTournament()).thenReturn(tournament);
        ServerGameEventBus bus = mock(ServerGameEventBus.class);
        when(game.getEventBus()).thenReturn(bus);

        mockMvc.perform(
                post("/api/v1/games/g5/cheat/level").contentType(MediaType.APPLICATION_JSON).content("{\"level\":3}"))
                .andExpect(status().isOk());

        verify(tournament).setLevel(3);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/games/{id}/cheat/button
    // -------------------------------------------------------------------------

    @Test
    void moveButton_betweenHands_success() throws Exception {
        GameInstance game = stubPracticeGame("g6", 1L);
        ServerGameTable table = stubTable(game, null); // no active hand

        mockMvc.perform(
                post("/api/v1/games/g6/cheat/button").contentType(MediaType.APPLICATION_JSON).content("{\"seat\":4}"))
                .andExpect(status().isOk());

        verify(table).setButton(4);
    }

    @Test
    void moveButton_duringHand_returns409() throws Exception {
        GameInstance game = stubPracticeGame("g7", 1L);
        ServerHand hand = mock(ServerHand.class);
        stubTable(game, hand); // active hand present

        mockMvc.perform(
                post("/api/v1/games/g7/cheat/button").contentType(MediaType.APPLICATION_JSON).content("{\"seat\":4}"))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/games/{id}/cheat/remove-player
    // -------------------------------------------------------------------------

    @Test
    void removePlayer_success() throws Exception {
        GameInstance game = stubPracticeGame("g8", 1L);
        ServerPlayer player = stubPlayer(game, -1);
        when(player.isSittingOut()).thenReturn(false);
        when(player.getChipCount()).thenReturn(1000);
        ServerGameTable table = stubTable(game, null);
        ServerGameEventBus bus = mock(ServerGameEventBus.class);
        when(game.getEventBus()).thenReturn(bus);
        // No other players
        when(table.getSeats()).thenReturn(10);
        for (int s = 0; s < 10; s++) {
            when(table.getPlayer(s)).thenReturn(null);
        }

        mockMvc.perform(post("/api/v1/games/g8/cheat/remove-player").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":-1}")).andExpect(status().isOk());

        verify(player).setChipCount(0);
        verify(player).setSittingOut(true);
        verify(player).setFinishPosition(1);
    }

    @Test
    void removePlayer_humanPlayer_returns422() throws Exception {
        GameInstance game = stubPracticeGame("g8a", 1L);
        ServerPlayer player = stubPlayer(game, 1); // profile ID 1 = human owner
        when(player.isSittingOut()).thenReturn(false);
        when(player.isHuman()).thenReturn(true);

        mockMvc.perform(post("/api/v1/games/g8a/cheat/remove-player").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":1}")).andExpect(status().isUnprocessableEntity());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/games/{id}/cheat/card
    // -------------------------------------------------------------------------

    @Test
    void changeCard_noActiveHand_returns409() throws Exception {
        GameInstance game = stubPracticeGame("g9", 1L);
        stubTable(game, null); // no active hand

        mockMvc.perform(post("/api/v1/games/g9/cheat/card").contentType(MediaType.APPLICATION_JSON)
                .content("{\"location\":\"COMMUNITY:2\",\"newCard\":\"Ah\"}")).andExpect(status().isConflict());
    }

    @Test
    void changeCard_communityCard_success() throws Exception {
        GameInstance game = stubPracticeGame("g10", 1L);
        ServerHand hand = mock(ServerHand.class);
        stubTable(game, hand);

        mockMvc.perform(post("/api/v1/games/g10/cheat/card").contentType(MediaType.APPLICATION_JSON)
                .content("{\"location\":\"COMMUNITY:2\",\"newCard\":\"Ah\"}")).andExpect(status().isOk());

        verify(hand).setCommunityCard(eq(2), any());
    }

    @Test
    void changeCard_holeCard_success() throws Exception {
        GameInstance game = stubPracticeGame("g11", 1L);
        ServerHand hand = mock(ServerHand.class);
        stubTable(game, hand);

        mockMvc.perform(post("/api/v1/games/g11/cheat/card").contentType(MediaType.APPLICATION_JSON)
                .content("{\"location\":\"PLAYER:-1:0\",\"newCard\":\"Kd\"}")).andExpect(status().isOk());

        verify(hand).setPlayerCard(eq(-1), eq(0), any());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/games/{id}/cheat/ai-strategy
    // -------------------------------------------------------------------------

    @Test
    void aiStrategy_success() throws Exception {
        GameInstance game = stubPracticeGame("g12", 1L);
        ServerTournamentContext tournament = mock(ServerTournamentContext.class);
        when(game.getTournament()).thenReturn(tournament);
        java.util.Map<Integer, Integer> overrides = new java.util.concurrent.ConcurrentHashMap<>();
        when(tournament.getAiStrategyOverrides()).thenReturn(overrides);
        ServerPlayer player = mock(ServerPlayer.class);
        when(tournament.getPlayerByID(-1)).thenReturn(player);

        mockMvc.perform(post("/api/v1/games/g12/cheat/ai-strategy").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":-1,\"skillLevel\":7}")).andExpect(status().isOk());

        verify(player).setSkillLevel(7);
    }

    @Test
    void aiStrategy_invalidSkillLevel_returns400() throws Exception {
        stubPracticeGame("g13", 1L);

        mockMvc.perform(post("/api/v1/games/g13/cheat/ai-strategy").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":-1,\"skillLevel\":11}")).andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Set up a practice game owned by the given profileId. The tournament mock is
     * attached but not yet populated with players/tables unless
     * stubPlayer/stubTable is also called.
     */
    private GameInstance stubPracticeGame(String gameId, long ownerProfileId) {
        GameInstance game = mock(GameInstance.class);
        when(game.getOwnerProfileId()).thenReturn(ownerProfileId);

        GameConfig config = mock(GameConfig.class);
        PracticeConfig practiceConfig = new PracticeConfig(null, null, null, null, null, null);
        when(config.practiceConfig()).thenReturn(practiceConfig);
        when(game.getConfig()).thenReturn(config);

        when(gameInstanceManager.getGame(gameId)).thenReturn(game);

        // By default return no connections (broadcast is a no-op).
        when(connectionManager.getConnections(gameId)).thenReturn(Collections.emptyList());

        return game;
    }

    /** Set up a non-practice game (practiceConfig == null). */
    private GameInstance stubNonPracticeGame(String gameId, long ownerProfileId) {
        GameInstance game = mock(GameInstance.class);
        when(game.getOwnerProfileId()).thenReturn(ownerProfileId);

        GameConfig config = mock(GameConfig.class);
        when(config.practiceConfig()).thenReturn(null);
        when(game.getConfig()).thenReturn(config);

        when(gameInstanceManager.getGame(gameId)).thenReturn(game);
        return game;
    }

    /** Wire a player into the tournament mock attached to the game. */
    private ServerPlayer stubPlayer(GameInstance game, int playerId) {
        ServerTournamentContext tournament = mock(ServerTournamentContext.class);
        when(game.getTournament()).thenReturn(tournament);
        ServerPlayer player = mock(ServerPlayer.class);
        when(tournament.getPlayerByID(playerId)).thenReturn(player);
        return player;
    }

    /**
     * Wire a table into the tournament mock, optionally with an active hand.
     * Returns the table mock.
     */
    private ServerGameTable stubTable(GameInstance game, ServerHand hand) {
        ServerTournamentContext tournament = game.getTournament();
        if (tournament == null) {
            tournament = mock(ServerTournamentContext.class);
            when(game.getTournament()).thenReturn(tournament);
        }
        when(tournament.getNumTables()).thenReturn(1);
        ServerGameTable table = mock(ServerGameTable.class);
        when(tournament.getTable(0)).thenReturn(table);
        when(table.getHoldemHand()).thenReturn(hand);
        when(game.getGameStateSnapshot(anyLong())).thenReturn(null);
        return table;
    }
}
