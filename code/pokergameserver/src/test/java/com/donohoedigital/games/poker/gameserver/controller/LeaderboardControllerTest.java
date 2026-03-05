/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.service.LeaderboardService;

@WebMvcTest
@Import({TestSecurityConfiguration.class, LeaderboardController.class})
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaderboardService leaderboardService;

    @Test
    void getLeaderboard_defaultMode() throws Exception {
        Map<String, Object> result = Map.of("entries", List.of(), "total", 0, "page", 0, "pageSize", 50);
        when(leaderboardService.getLeaderboard(eq(false), eq(10), isNull(), isNull(), isNull(), eq(0), eq(50)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/leaderboard")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void getLeaderboard_roiMode() throws Exception {
        Map<String, Object> result = Map.of("entries", List.of(), "total", 5, "page", 0, "pageSize", 50);
        when(leaderboardService.getLeaderboard(eq(true), eq(10), isNull(), isNull(), isNull(), eq(0), eq(50)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/leaderboard").param("mode", "roi")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    void getPlayerRank_found() throws Exception {
        Map<String, Object> entry = Map.of("playerName", "player1", "gamesPlayed", 20);
        when(leaderboardService.getPlayerRank(eq("player1"), eq(10), isNull(), isNull())).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/leaderboard/player/player1")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerName").value("player1"));
    }

    @Test
    void getPlayerRank_notFound() throws Exception {
        when(leaderboardService.getPlayerRank(eq("nonexistent"), eq(10), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/leaderboard/player/nonexistent")).andExpect(status().isNotFound());
    }
}
