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
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link PracticeGameController}.
 */
@WebMvcTest
@Import({TestSecurityConfiguration.class, PracticeGameController.class})
class PracticeGameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameInstanceManager gameInstanceManager;

    private static final String BLIND_STRUCTURE = "[{\"smallBlind\":10,\"bigBlind\":20,\"ante\":0,\"minutes\":15,\"isBreak\":false,\"gameType\":\"NOLIMIT_HOLDEM\"}]";

    private static final String COMMON_FIELDS = "\"maxPlayers\":2,\"maxOnlinePlayers\":90,\"fillComputer\":true,\"buyIn\":0,\"startingChips\":1000,"
            + "\"blindStructure\":" + BLIND_STRUCTURE
            + ",\"doubleAfterLastLevel\":true,\"defaultGameType\":\"NOLIMIT_HOLDEM\","
            + "\"levelAdvanceMode\":\"TIME\",\"handsPerLevel\":10,\"defaultMinutesPerLevel\":15,"
            + "\"allowDash\":false,\"allowAdvisor\":false";

    private static final String MINIMAL_GAME_CONFIG = "{\"name\":\"Practice\"," + COMMON_FIELDS
            + ",\"aiPlayers\":[{\"name\":\"Computer 1\",\"skillLevel\":4}]}";

    private static final String TWO_AI_CONFIG = "{\"name\":\"Practice\"," + COMMON_FIELDS
            + ",\"aiPlayers\":[{\"name\":\"Bot1\",\"skillLevel\":3},{\"name\":\"Bot2\",\"skillLevel\":5}]}";

    private static final String NO_AI_CONFIG = "{\"name\":\"Practice\"," + COMMON_FIELDS + ",\"aiPlayers\":null}";

    @Test
    void createsPracticeGameAndReturns201() throws Exception {
        GameInstance mockInstance = stubMockInstance("practice-abc");

        mockMvc.perform(
                post("/api/v1/games/practice").contentType(MediaType.APPLICATION_JSON).content(MINIMAL_GAME_CONFIG))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.gameId").value("practice-abc"));
    }

    @Test
    void transitionsToWaitingForPlayersBeforeAddingPlayers() throws Exception {
        GameInstance mockInstance = stubMockInstance("game-1");

        mockMvc.perform(
                post("/api/v1/games/practice").contentType(MediaType.APPLICATION_JSON).content(MINIMAL_GAME_CONFIG))
                .andExpect(status().isCreated());

        verify(mockInstance).transitionToWaitingForPlayers();
    }

    @Test
    void addsHumanPlayerFirst() throws Exception {
        GameInstance mockInstance = stubMockInstance("game-2");

        mockMvc.perform(
                post("/api/v1/games/practice").contentType(MediaType.APPLICATION_JSON).content(MINIMAL_GAME_CONFIG))
                .andExpect(status().isCreated());

        // Human player profileId = 1L (from TestSecurityConfiguration)
        verify(mockInstance).addPlayer(eq(1L), eq("testuser"), eq(false), eq(0));
    }

    @Test
    void addsAiPlayersFromConfig() throws Exception {
        GameInstance mockInstance = stubMockInstance("game-3");

        mockMvc.perform(
                post("/api/v1/games/practice").contentType(MediaType.APPLICATION_JSON).content(MINIMAL_GAME_CONFIG))
                .andExpect(status().isCreated());

        // AI player has negative ID (-1L), name "Computer 1", isAI=true, skillLevel=4
        verify(mockInstance).addPlayer(eq(-1L), eq("Computer 1"), eq(true), eq(4));
    }

    @Test
    void gameNotStartedByRestEndpoint() throws Exception {
        GameInstance mockInstance = stubMockInstance("game-4");

        mockMvc.perform(
                post("/api/v1/games/practice").contentType(MediaType.APPLICATION_JSON).content(MINIMAL_GAME_CONFIG))
                .andExpect(status().isCreated());

        // startGame is intentionally NOT called here — it is triggered when the
        // owner's WebSocket connects (GameWebSocketHandler.afterConnectionEstablished).
        verify(gameInstanceManager, never()).startGame(anyString(), anyLong());
    }

    @Test
    void multipleAiPlayersGetSequentialNegativeIds() throws Exception {
        GameInstance mockInstance = stubMockInstance("game-5");

        mockMvc.perform(post("/api/v1/games/practice").contentType(MediaType.APPLICATION_JSON).content(TWO_AI_CONFIG))
                .andExpect(status().isCreated());

        verify(mockInstance).addPlayer(eq(-1L), eq("Bot1"), eq(true), eq(3));
        verify(mockInstance).addPlayer(eq(-2L), eq("Bot2"), eq(true), eq(5));
    }

    @Test
    void configWithNoAiPlayersCreatesGameWithHumanOnly() throws Exception {
        GameInstance mockInstance = stubMockInstance("game-6");

        mockMvc.perform(post("/api/v1/games/practice").contentType(MediaType.APPLICATION_JSON).content(NO_AI_CONFIG))
                .andExpect(status().isCreated());

        // Only human player added
        verify(mockInstance).addPlayer(anyLong(), anyString(), eq(false), anyInt());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private GameInstance stubMockInstance(String gameId) {
        GameInstance mockInstance = mock(GameInstance.class);
        when(mockInstance.getGameId()).thenReturn(gameId);
        when(gameInstanceManager.createGame(anyLong(), any(GameConfig.class))).thenReturn(mockInstance);
        return mockInstance;
    }
}
