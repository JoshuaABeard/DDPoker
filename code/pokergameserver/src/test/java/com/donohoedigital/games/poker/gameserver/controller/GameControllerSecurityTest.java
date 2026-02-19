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

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.dto.GameListResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.service.GameService;

/**
 * Verifies the security boundary of {@link GameController}: public endpoints
 * succeed without auth; all mutating endpoints require a valid JWT.
 */
@WebMvcTest
@ActiveProfiles("restricted-security")
@Import({TestRestrictedSecurityConfiguration.class, GameController.class, GameServerExceptionHandler.class})
class GameControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    // =========================================================================
    // Public endpoints — no auth required
    // =========================================================================

    @Test
    void getGames_withoutAuth_returns200() throws Exception {
        when(gameService.listGames(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new GameListResponse(List.of(), 0, 0, 50));

        mockMvc.perform(get("/api/v1/games")).andExpect(status().isOk());
    }

    @Test
    void getGame_withoutAuth_returns200() throws Exception {
        when(gameService.getGameSummary("game-123")).thenReturn(buildSummary("game-123"));

        mockMvc.perform(get("/api/v1/games/game-123")).andExpect(status().isOk());
    }

    // =========================================================================
    // Protected endpoints — 401 without auth
    // =========================================================================

    @Test
    void createGame_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/games").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"description\":\"\",\"greeting\":\"\",\"maxPlayers\":9,"
                        + "\"maxOnlinePlayers\":90,\"fillComputer\":true,\"buyIn\":0,\"startingChips\":1000,"
                        + "\"blindStructure\":[],\"doubleAfterLastLevel\":true,\"defaultGameType\":\"NOLIMIT_HOLDEM\","
                        + "\"levelAdvanceMode\":\"TIME\",\"handsPerLevel\":10,\"defaultMinutesPerLevel\":15,"
                        + "\"allowDash\":false,\"allowAdvisor\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void joinGame_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/games/game-123/join")).andExpect(status().isUnauthorized());
    }

    @Test
    void startGame_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/games/game-123/start")).andExpect(status().isUnauthorized());
    }

    @Test
    void cancelGame_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/games/game-123")).andExpect(status().isUnauthorized());
    }

    @Test
    void heartbeat_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/games/game-123/heartbeat")).andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Protected endpoints succeed with auth header
    // =========================================================================

    @Test
    void startGame_withAuth_returns200() throws Exception {
        when(gameService.startGame(eq("game-123"), anyLong())).thenReturn(buildSummary("game-123"));

        mockMvc.perform(post("/api/v1/games/game-123/start").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    @Test
    void joinGame_withAuth_returns200() throws Exception {
        when(gameService.joinGame(eq("game-123"), any())).thenReturn(
                new com.donohoedigital.games.poker.gameserver.dto.GameJoinResponse("ws://localhost/ws/games/game-123",
                        "game-123"));

        mockMvc.perform(post("/api/v1/games/game-123/join").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private GameSummary buildSummary(String gameId) {
        return new GameSummary(gameId, "Test Game", "SERVER", "WAITING_FOR_PLAYERS", "owner", 0, 9, false,
                "ws://localhost/ws/games/" + gameId, new GameSummary.BlindsSummary(10, 20, 0), Instant.now(), null);
    }
}
