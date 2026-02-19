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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.GameServerException;
import com.donohoedigital.games.poker.gameserver.GameServerException.ErrorCode;
import com.donohoedigital.games.poker.gameserver.dto.GameJoinResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameListResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.service.GameService;

@WebMvcTest
@Import({TestSecurityConfiguration.class, GameController.class, GameServerExceptionHandler.class})
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    // =========================================================================
    // POST /api/v1/games — createGame
    // =========================================================================

    @Test
    void testCreateGame() throws Exception {
        when(gameService.createGame(any(), anyLong(), anyString())).thenReturn("game-123");

        mockMvc.perform(post("/api/v1/games").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Game\",\"description\":\"Test\",\"greeting\":\"Hi\",\"maxPlayers\":9,"
                        + "\"maxOnlinePlayers\":90,\"fillComputer\":true,\"buyIn\":0,\"startingChips\":1000,"
                        + "\"blindStructure\":[{\"smallBlind\":10,\"bigBlind\":20,\"ante\":0,\"minutes\":15,\"isBreak\":false,\"gameType\":\"NOLIMIT_HOLDEM\"}],"
                        + "\"doubleAfterLastLevel\":true,\"defaultGameType\":\"NOLIMIT_HOLDEM\",\"levelAdvanceMode\":\"TIME\","
                        + "\"handsPerLevel\":10,\"defaultMinutesPerLevel\":15,\"allowDash\":false,\"allowAdvisor\":false}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.gameId").value("game-123"));
    }

    // =========================================================================
    // POST /api/v1/games/community — registerCommunityGame
    // =========================================================================

    @Test
    void testRegisterCommunityGame() throws Exception {
        when(gameService.registerCommunityGame(anyLong(), anyString(), any()))
                .thenReturn(buildSummary("game-456", "Community Game", "COMMUNITY"));

        mockMvc.perform(post("/api/v1/games/community").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Community Game\",\"wsUrl\":\"ws://1.2.3.4:8765/ws/games/local\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.gameId").value("game-456"))
                .andExpect(jsonPath("$.hostingType").value("COMMUNITY"));
    }

    // =========================================================================
    // POST /api/v1/games/{id}/join — joinGame
    // =========================================================================

    @Test
    void testJoinGame() throws Exception {
        when(gameService.joinGame(eq("game-123"), any()))
                .thenReturn(new GameJoinResponse("ws://localhost/ws/games/game-123", "game-123"));

        mockMvc.perform(post("/api/v1/games/game-123/join")).andExpect(status().isOk())
                .andExpect(jsonPath("$.wsUrl").value("ws://localhost/ws/games/game-123"));
    }

    @Test
    void testJoinGame_withPassword() throws Exception {
        when(gameService.joinGame(eq("game-123"), eq("secret")))
                .thenReturn(new GameJoinResponse("ws://localhost/ws/games/game-123", "game-123"));

        mockMvc.perform(post("/api/v1/games/game-123/join").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"secret\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.wsUrl").value("ws://localhost/ws/games/game-123"));
    }

    @Test
    void testJoinGameNotFound() throws Exception {
        when(gameService.joinGame(eq("nonexistent"), any()))
                .thenThrow(new GameServerException(ErrorCode.GAME_NOT_FOUND, "Game not found"));

        mockMvc.perform(post("/api/v1/games/nonexistent/join")).andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /api/v1/games/{id}/start — startGame
    // =========================================================================

    @Test
    void testStartGame() throws Exception {
        when(gameService.startGame(eq("game-123"), anyLong()))
                .thenReturn(buildSummary("game-123", "Test Game", "SERVER"));

        mockMvc.perform(post("/api/v1/games/game-123/start")).andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("game-123"));
    }

    @Test
    void testStartGameNotFound() throws Exception {
        when(gameService.startGame(eq("nonexistent"), anyLong()))
                .thenThrow(new GameServerException(ErrorCode.GAME_NOT_FOUND, "Game not found"));

        mockMvc.perform(post("/api/v1/games/nonexistent/start")).andExpect(status().isNotFound());
    }

    @Test
    void testStartGame_nonOwner_returns403() throws Exception {
        when(gameService.startGame(eq("game-123"), anyLong()))
                .thenThrow(new GameServerException(ErrorCode.NOT_GAME_OWNER, "Not owner"));

        mockMvc.perform(post("/api/v1/games/game-123/start")).andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/games/{id} — getGame
    // =========================================================================

    @Test
    void testGetGame() throws Exception {
        when(gameService.getGameSummary("game-123")).thenReturn(buildSummary("game-123", "Test Game", "SERVER"));

        mockMvc.perform(get("/api/v1/games/game-123")).andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("game-123")).andExpect(jsonPath("$.name").value("Test Game"))
                .andExpect(jsonPath("$.hostingType").value("SERVER"));
    }

    @Test
    void testGetGameNotFound() throws Exception {
        when(gameService.getGameSummary("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/api/v1/games/nonexistent")).andExpect(status().isNotFound());
    }

    // =========================================================================
    // GET /api/v1/games — listGames
    // =========================================================================

    @Test
    void testListGames() throws Exception {
        when(gameService.listGames(any(), any(), any(), anyInt(), anyInt())).thenReturn(new GameListResponse(
                List.of(buildSummary("game-1", "Game 1", "SERVER"), buildSummary("game-2", "Game 2", "COMMUNITY")), 2,
                0, 50));

        mockMvc.perform(get("/api/v1/games")).andExpect(status().isOk())
                .andExpect(jsonPath("$.games.length()").value(2))
                .andExpect(jsonPath("$.games[0].gameId").value("game-1"))
                .andExpect(jsonPath("$.games[1].hostingType").value("COMMUNITY"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void testListGames_publicEndpoint_noAuthRequired() throws Exception {
        when(gameService.listGames(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new GameListResponse(List.of(), 0, 0, 50));

        // GET /api/v1/games should succeed even without valid JWT
        mockMvc.perform(get("/api/v1/games")).andExpect(status().isOk());
    }

    // =========================================================================
    // DELETE /api/v1/games/{id} — cancelGame
    // =========================================================================

    @Test
    void testCancelGame() throws Exception {
        doNothing().when(gameService).cancelGame(eq("game-123"), anyLong());

        mockMvc.perform(delete("/api/v1/games/game-123")).andExpect(status().isNoContent());
    }

    @Test
    void testCancelGame_notOwner_returns403() throws Exception {
        doThrow(new GameServerException(ErrorCode.NOT_GAME_OWNER, "Not owner")).when(gameService)
                .cancelGame(eq("game-123"), anyLong());

        mockMvc.perform(delete("/api/v1/games/game-123")).andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /api/v1/games/{id}/heartbeat
    // =========================================================================

    @Test
    void testHeartbeat() throws Exception {
        doNothing().when(gameService).heartbeat(eq("game-123"), anyLong());

        mockMvc.perform(post("/api/v1/games/game-123/heartbeat")).andExpect(status().isOk());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private GameSummary buildSummary(String gameId, String name, String hostingType) {
        return new GameSummary(gameId, name, hostingType, "WAITING_FOR_PLAYERS", "owner", 1, 9, false,
                "ws://localhost/ws/games/" + gameId, new GameSummary.BlindsSummary(10, 20, 0), Instant.now(), null);
    }
}
