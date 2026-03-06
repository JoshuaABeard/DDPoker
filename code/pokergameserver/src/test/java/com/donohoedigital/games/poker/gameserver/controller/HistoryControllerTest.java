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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.TournamentHistoryRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.TournamentHistory;

@WebMvcTest
@Import({TestSecurityConfiguration.class, HistoryController.class})
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TournamentHistoryRepository historyRepository;

    @MockitoBean
    private OnlineProfileRepository profileRepository;

    @Test
    void getHistory_playerFound_returnsPage() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("player1");

        TournamentHistory h = new TournamentHistory();
        h.setId(10L);
        h.setPlace(1);

        when(profileRepository.findByName("player1")).thenReturn(Optional.of(profile));
        when(historyRepository.findByProfileId(eq(1L), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(h)));

        mockMvc.perform(get("/api/v1/history").param("name", "player1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void getHistory_playerNotFound_returns404() throws Exception {
        when(profileRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/history").param("name", "nonexistent")).andExpect(status().isNotFound());
    }

    @Test
    void getTournament_found() throws Exception {
        TournamentHistory h = new TournamentHistory();
        h.setId(10L);

        when(historyRepository.findByGameId(eq(5L), any())).thenReturn(new PageImpl<>(List.of(h)));

        mockMvc.perform(get("/api/v1/tournaments/5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void getTournament_notFound() throws Exception {
        when(historyRepository.findByGameId(eq(999L), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/tournaments/999")).andExpect(status().isNotFound());
    }

    @Test
    void getOverallStats_returnsAggregatedData() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("player1");

        TournamentHistory h1 = new TournamentHistory();
        h1.setId(10L);
        h1.setPlace(1);
        h1.setPrize(500);
        h1.setBuyin(100);
        h1.setRebuy(0);
        h1.setAddon(0);

        TournamentHistory h2 = new TournamentHistory();
        h2.setId(11L);
        h2.setPlace(3);
        h2.setPrize(100);
        h2.setBuyin(100);
        h2.setRebuy(50);
        h2.setAddon(25);

        when(profileRepository.findByName("player1")).thenReturn(Optional.of(profile));
        when(historyRepository.findAllByProfileId(1L)).thenReturn(List.of(h1, h2));

        mockMvc.perform(get("/api/v1/history/stats").param("name", "player1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTournaments").value(2)).andExpect(jsonPath("$.totalWins").value(1))
                .andExpect(jsonPath("$.totalPrize").value(600)).andExpect(jsonPath("$.totalSpent").value(275))
                .andExpect(jsonPath("$.netProfit").value(325)).andExpect(jsonPath("$.avgFinish").value(2.0))
                .andExpect(jsonPath("$.avgROI").value(118.18181818181819));
    }

    @Test
    void getOverallStats_playerNotFound_returns404() throws Exception {
        when(profileRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/history/stats").param("name", "nonexistent")).andExpect(status().isNotFound());
    }

    @Test
    void getOverallStats_noEntries_returnsZeros() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("player1");

        when(profileRepository.findByName("player1")).thenReturn(Optional.of(profile));
        when(historyRepository.findAllByProfileId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/history/stats").param("name", "player1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTournaments").value(0)).andExpect(jsonPath("$.avgFinish").value(0.0))
                .andExpect(jsonPath("$.avgROI").value(0.0));
    }

    @Test
    void deleteHistory_exists_returns204() throws Exception {
        when(historyRepository.existsById(10L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/history/10")).andExpect(status().isNoContent());

        verify(historyRepository).deleteById(10L);
    }

    @Test
    void deleteHistory_notFound_returns404() throws Exception {
        when(historyRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/history/999")).andExpect(status().isNotFound());

        verify(historyRepository, never()).deleteById(any());
    }

    @Test
    void deleteAllHistory_playerFound_returns204() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("player1");

        when(profileRepository.findByName("player1")).thenReturn(Optional.of(profile));

        mockMvc.perform(delete("/api/v1/history").param("name", "player1")).andExpect(status().isNoContent());

        verify(historyRepository).deleteByProfileId(1L);
    }

    @Test
    void deleteAllHistory_playerNotFound_returns404() throws Exception {
        when(profileRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/history").param("name", "nonexistent")).andExpect(status().isNotFound());

        verify(historyRepository, never()).deleteByProfileId(any());
    }
}
