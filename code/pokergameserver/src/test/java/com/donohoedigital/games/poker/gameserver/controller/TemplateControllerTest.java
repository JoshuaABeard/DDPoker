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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.persistence.repository.TournamentTemplateRepository;
import com.donohoedigital.games.poker.gameserver.service.ProfileService;
import com.donohoedigital.games.poker.model.TournamentTemplate;

@WebMvcTest
@Import({TestSecurityConfiguration.class, TemplateController.class})
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TournamentTemplateRepository templateRepository;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void listTemplates_returnsUserTemplates() throws Exception {
        TournamentTemplate t = new TournamentTemplate();
        t.setId(1L);
        t.setProfileId(1L);
        t.setName("My Template");
        t.setConfig("{\"blinds\":100}");

        when(templateRepository.findByProfileIdOrderByModifyDateDesc(1L)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/v1/profiles/templates")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("My Template"));
    }

    @Test
    void createTemplate_success() throws Exception {
        TournamentTemplate saved = new TournamentTemplate();
        saved.setId(1L);
        saved.setProfileId(1L);
        saved.setName("New Template");
        saved.setConfig("{\"blinds\":200}");

        when(templateRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/profiles/templates").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Template\",\"config\":\"{\\\"blinds\\\":200}\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Template"));
    }

    @Test
    void updateTemplate_ownTemplate() throws Exception {
        TournamentTemplate existing = new TournamentTemplate();
        existing.setId(1L);
        existing.setProfileId(1L);
        existing.setName("Old Name");
        existing.setConfig("{}");

        TournamentTemplate saved = new TournamentTemplate();
        saved.setId(1L);
        saved.setProfileId(1L);
        saved.setName("Updated");
        saved.setConfig("{\"blinds\":300}");

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(templateRepository.save(any())).thenReturn(saved);

        mockMvc.perform(put("/api/v1/profiles/templates/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated\",\"config\":\"{\\\"blinds\\\":300}\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void updateTemplate_differentUser_forbidden() throws Exception {
        TournamentTemplate existing = new TournamentTemplate();
        existing.setId(1L);
        existing.setProfileId(999L); // different user

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existing));

        mockMvc.perform(put("/api/v1/profiles/templates/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hack\",\"config\":\"{}\"}")).andExpect(status().isForbidden());
    }

    @Test
    void updateTemplate_notFound() throws Exception {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/profiles/templates/999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"config\":\"{}\"}")).andExpect(status().isNotFound());
    }

    @Test
    void deleteTemplate_ownTemplate() throws Exception {
        TournamentTemplate existing = new TournamentTemplate();
        existing.setId(1L);
        existing.setProfileId(1L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existing));

        mockMvc.perform(delete("/api/v1/profiles/templates/1")).andExpect(status().isNoContent());
    }

    @Test
    void deleteTemplate_differentUser_forbidden() throws Exception {
        TournamentTemplate existing = new TournamentTemplate();
        existing.setId(1L);
        existing.setProfileId(999L);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(existing));

        mockMvc.perform(delete("/api/v1/profiles/templates/1")).andExpect(status().isForbidden());
    }

    @Test
    void deleteTemplate_notFound() throws Exception {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/profiles/templates/999")).andExpect(status().isNotFound());
    }
}
