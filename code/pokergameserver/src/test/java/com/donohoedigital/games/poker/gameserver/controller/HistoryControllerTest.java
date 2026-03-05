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
}
