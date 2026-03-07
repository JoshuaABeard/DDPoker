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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.service.HandHistoryService;
import com.donohoedigital.games.poker.protocol.dto.HandActionDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandExportData;
import com.donohoedigital.games.poker.protocol.dto.HandPlayerDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandRoundStatsData;
import com.donohoedigital.games.poker.protocol.dto.HandStatsData;
import com.donohoedigital.games.poker.protocol.dto.HandSummaryData;

@WebMvcTest
@Import({TestSecurityConfiguration.class, HandHistoryController.class})
class HandHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HandHistoryService handHistoryService;

    @Test
    void listHands_returnsPage() throws Exception {
        HandSummaryData summary = new HandSummaryData(1L, 5, 0, List.of("Ac", "Kh"), List.of("Td", "9s", "2c"),
                Instant.parse("2026-01-01T00:00:00Z"));
        when(handHistoryService.getHandSummaries(eq("game1"), any()))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/games/game1/hands")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].handId").value(1))
                .andExpect(jsonPath("$.content[0].handNumber").value(5))
                .andExpect(jsonPath("$.content[0].holeCards[0]").value("Ac"));
    }

    @Test
    void countHands_returnsCount() throws Exception {
        when(handHistoryService.getHandCount("game1")).thenReturn(42L);

        mockMvc.perform(get("/api/v1/games/game1/hands/count")).andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void getHand_found_returnsDetail() throws Exception {
        HandPlayerDetailData player = new HandPlayerDetailData(0, "Player1", 0, 1000, 1200, List.of("Ac", "Kh"), 1, 0,
                0, 0, false);
        HandActionDetailData action = new HandActionDetailData(0, 1, 0, "CALL", 100, 0, false);
        HandDetailData detail = new HandDetailData(1L, 5, 0, "NO_LIMIT", "HOLDEM",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:01:00Z"), 0, 50, 100,
                List.of("Td", "9s", "2c"), 3, List.of(player), List.of(action));

        when(handHistoryService.getHandDetail("game1", 1L)).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/api/v1/games/game1/hands/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.handId").value(1)).andExpect(jsonPath("$.handNumber").value(5))
                .andExpect(jsonPath("$.players[0].playerName").value("Player1"))
                .andExpect(jsonPath("$.actions[0].actionType").value("CALL"));
    }

    @Test
    void getHand_notFound_returns404() throws Exception {
        when(handHistoryService.getHandDetail("game1", 999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/games/game1/hands/999")).andExpect(status().isNotFound());
    }

    @Test
    void getStats_returnsList() throws Exception {
        HandStatsData stats = new HandStatsData("AKo", 10, 60.0, 30.0, 10.0, 150.0, 1050.0, 80.0, 50.0, 30.0, 20.0);
        when(handHistoryService.getHandStats("game1")).thenReturn(List.of(stats));

        mockMvc.perform(get("/api/v1/games/game1/hands/stats")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].handClass").value("AKo")).andExpect(jsonPath("$[0].count").value(10))
                .andExpect(jsonPath("$[0].winPct").value(60.0));
    }

    @Test
    void exportHands_returnsList() throws Exception {
        HandExportData export = new HandExportData(1L, 5, null, null, "0", "NO_LIMIT", "HOLDEM",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:01:00Z"), 0, 50, 100, null,
                List.of("Td", "9s", "2c"), List.of(), List.of());
        when(handHistoryService.getHandsForExport("game1")).thenReturn(List.of(export));

        mockMvc.perform(get("/api/v1/games/game1/hands/export")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].handId").value(1)).andExpect(jsonPath("$[0].gameStyle").value("NO_LIMIT"));
    }

    @Test
    void getRoundStats_returnsList() throws Exception {
        HandRoundStatsData roundStats = new HandRoundStatsData("AKo", 5, 20.0, 10.0, 30.0, 15.0, 10.0, 5.0, 10.0, 60.0);
        when(handHistoryService.getRoundStats("game1", 1)).thenReturn(List.of(roundStats));

        mockMvc.perform(get("/api/v1/games/game1/hands/round-stats").param("round", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].handClass").value("AKo")).andExpect(jsonPath("$[0].count").value(5));
    }

    @Test
    void listHands_emptyPage_returnsEmptyContent() throws Exception {
        when(handHistoryService.getHandSummaries(eq("empty-game"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/games/empty-game/hands")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getStats_emptyGame_returnsEmptyList() throws Exception {
        when(handHistoryService.getHandStats("empty-game")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/games/empty-game/hands/stats")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
