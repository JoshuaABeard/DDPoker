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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.BanRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.service.EmailService;
import com.donohoedigital.games.poker.model.OnlineProfile;

@WebMvcTest
@Import({TestSecurityConfiguration.class, AdminController.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnlineProfileRepository profileRepository;

    @MockitoBean
    private BanRepository banRepository;

    @MockitoBean
    private EmailService emailService;

    @Test
    void searchProfiles_returnsResults() throws Exception {
        OnlineProfile p = new OnlineProfile();
        p.setId(1L);
        p.setName("player1");

        when(profileRepository.searchProfiles(anyString(), anyString(), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(p)));

        mockMvc.perform(get("/api/v1/admin/profiles").param("name", "player")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("player1"));
    }

    @Test
    void manuallyVerify_profileFound() throws Exception {
        OnlineProfile p = new OnlineProfile();
        p.setId(1L);
        p.setEmailVerified(false);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(p));
        when(profileRepository.save(any())).thenReturn(p);

        mockMvc.perform(post("/api/v1/admin/profiles/1/verify")).andExpect(status().isOk());
    }

    @Test
    void manuallyVerify_notFound() throws Exception {
        when(profileRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/admin/profiles/999/verify")).andExpect(status().isNotFound());
    }

    @Test
    void unlockAccount_found() throws Exception {
        OnlineProfile p = new OnlineProfile();
        p.setId(1L);
        p.setFailedLoginAttempts(5);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(p));
        when(profileRepository.save(any())).thenReturn(p);

        mockMvc.perform(post("/api/v1/admin/profiles/1/unlock")).andExpect(status().isOk());
    }

    @Test
    void unlockAccount_notFound() throws Exception {
        when(profileRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/admin/profiles/999/unlock")).andExpect(status().isNotFound());
    }

    @Test
    void resendVerification_unverifiedProfile() throws Exception {
        OnlineProfile p = new OnlineProfile();
        p.setId(1L);
        p.setName("player1");
        p.setEmail("player1@example.com");
        p.setEmailVerified(false);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(p));
        when(profileRepository.save(any())).thenReturn(p);

        mockMvc.perform(post("/api/v1/admin/profiles/1/resend-verification")).andExpect(status().isOk());
        verify(emailService).sendVerificationEmail(eq("player1@example.com"), eq("player1"), anyString());
    }

    @Test
    void resendVerification_alreadyVerified_returns400() throws Exception {
        OnlineProfile p = new OnlineProfile();
        p.setId(1L);
        p.setEmailVerified(true);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(p));

        mockMvc.perform(post("/api/v1/admin/profiles/1/resend-verification")).andExpect(status().isBadRequest());
    }

    @Test
    void listBans_returnsAll() throws Exception {
        BanEntity ban = new BanEntity();
        ban.setId(1L);
        ban.setUntil(LocalDate.of(2099, 12, 31));

        when(banRepository.findAll()).thenReturn(List.of(ban));

        mockMvc.perform(get("/api/v1/admin/bans")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void addBan_returnsCreated() throws Exception {
        BanEntity saved = new BanEntity();
        saved.setId(1L);
        saved.setUntil(LocalDate.of(2099, 12, 31));

        when(banRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/admin/bans").contentType(MediaType.APPLICATION_JSON)
                .content("{\"banType\":\"PROFILE\",\"profileId\":1}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void removeBan_exists() throws Exception {
        when(banRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/admin/bans/1")).andExpect(status().isNoContent());
    }

    @Test
    void removeBan_notFound() throws Exception {
        when(banRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/admin/bans/999")).andExpect(status().isNotFound());
    }
}
