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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;

@WebMvcTest
@Import({TestSecurityConfiguration.class, SearchController.class})
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnlineProfileRepository profileRepository;

    @Test
    void searchPlayers_returnsResults() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("player1");

        when(profileRepository.searchByName(eq("%player%"), any())).thenReturn(new PageImpl<>(List.of(profile)));

        mockMvc.perform(get("/api/v1/search").param("name", "player")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("player1"));
    }

    @Test
    void searchPlayers_emptyResults() throws Exception {
        when(profileRepository.searchByName(anyString(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/search").param("name", "nonexistent")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
