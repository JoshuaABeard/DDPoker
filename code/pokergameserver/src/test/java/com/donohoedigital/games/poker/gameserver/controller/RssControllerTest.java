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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineGameRepository;
import com.donohoedigital.games.poker.model.OnlineGame;

@WebMvcTest
@Import({TestSecurityConfiguration.class, RssController.class})
class RssControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnlineGameRepository gameRepository;

    @Test
    void rssFeed_available_returnsXml() throws Exception {
        when(gameRepository.findByModeIn(eq(List.of(OnlineGame.MODE_REG)), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/rss/available")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(xpath("/rss/channel/title").string("DD Poker - Available Games"));
    }

    @Test
    void rssFeed_ended_returnsXml() throws Exception {
        when(gameRepository.findByModeIn(eq(List.of(OnlineGame.MODE_END)), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/rss/ended")).andExpect(status().isOk())
                .andExpect(xpath("/rss/channel/title").string("DD Poker - Ended Games"));
    }

    @Test
    void rssFeed_withGames() throws Exception {
        OnlineGame game = new OnlineGame();
        game.setId(1L);
        game.setUrl("Test Game");
        game.setHostPlayer("player1");
        game.setMode(OnlineGame.MODE_REG);

        when(gameRepository.findByModeIn(anyList(), any())).thenReturn(List.of(game));

        mockMvc.perform(get("/api/v1/rss/available")).andExpect(status().isOk())
                .andExpect(xpath("/rss/channel/item/title").string("Test Game"));
    }
}
