/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.dto.GameStateResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.service.GameService;

@WebMvcTest
@Import({TestSecurityConfiguration.class, GameController.class})
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @Test
    void testCreateGame() throws Exception {
        when(gameService.createGame(any(), anyLong(), anyString())).thenReturn("game-123");

        mockMvc.perform(post("/api/v1/games").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Game\",\"description\":\"Test\",\"greeting\":\"Hi\",\"maxPlayers\":9,"
                        + "\"maxOnlinePlayers\":90,\"fillComputer\":true,\"buyIn\":0,\"startingChips\":1000,"
                        + "\"blindStructure\":[{\"smallBlind\":10,\"bigBlind\":20,\"ante\":0,\"minutes\":15,\"isBreak\":false,\"gameType\":\"NOLIMIT_HOLDEM\"}],"
                        + "\"doubleAfterLastLevel\":true,\"defaultGameType\":\"NOLIMIT_HOLDEM\",\"levelAdvanceMode\":\"TIME\","
                        + "\"handsPerLevel\":10,\"defaultMinutesPerLevel\":15,\"onlineActivatedOnly\":true,\"allowDash\":false,\"allowAdvisor\":false}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.gameId").value("game-123"));
    }

    @Test
    void testJoinGame() throws Exception {
        when(gameService.joinGame(eq("game-123"), anyLong(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/v1/games/game-123/join")).andExpect(status().isOk());
    }

    @Test
    void testJoinGameNotFound() throws Exception {
        when(gameService.joinGame(eq("nonexistent"), anyLong(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/v1/games/nonexistent/join")).andExpect(status().isNotFound());
    }

    @Test
    void testStartGame() throws Exception {
        when(gameService.startGame("game-123")).thenReturn(true);

        mockMvc.perform(post("/api/v1/games/game-123/start")).andExpect(status().isOk());
    }

    @Test
    void testStartGameNotFound() throws Exception {
        when(gameService.startGame("nonexistent")).thenReturn(false);

        mockMvc.perform(post("/api/v1/games/nonexistent/start")).andExpect(status().isNotFound());
    }

    @Test
    void testGetGameState() throws Exception {
        when(gameService.getGameState("game-123"))
                .thenReturn(new GameStateResponse("game-123", "Test Game", "WAITING_FOR_PLAYERS", 1, 9));

        mockMvc.perform(get("/api/v1/games/game-123")).andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("game-123")).andExpect(jsonPath("$.name").value("Test Game"))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"));
    }

    @Test
    void testGetGameStateNotFound() throws Exception {
        when(gameService.getGameState("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/api/v1/games/nonexistent")).andExpect(status().isNotFound());
    }

    @Test
    void testListGames() throws Exception {
        when(gameService.listGames()).thenReturn(
                java.util.List.of(new GameSummary("game-1", "Game 1", "owner1", 1, 9, "WAITING_FOR_PLAYERS"),
                        new GameSummary("game-2", "Game 2", "owner2", 2, 9, "IN_PROGRESS")));

        mockMvc.perform(get("/api/v1/games")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].gameId").value("game-1"));
    }
}
