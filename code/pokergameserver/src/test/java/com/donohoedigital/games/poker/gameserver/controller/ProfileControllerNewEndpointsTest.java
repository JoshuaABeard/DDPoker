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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.service.ProfileService;
import com.donohoedigital.games.poker.model.OnlineProfile;

/**
 * Tests for new ProfileController endpoints: GET /name/{name}, GET /aliases,
 * POST /{id}/retire.
 */
@WebMvcTest
@Import({TestSecurityConfiguration.class, ProfileController.class})
class ProfileControllerNewEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private OnlineProfileRepository profileRepository;

    @Test
    void getProfileByName_found() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("player1");

        when(profileRepository.findByName("player1")).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/v1/profiles/name/player1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("player1"));
    }

    @Test
    void getProfileByName_notFound() throws Exception {
        when(profileRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/profiles/name/nonexistent")).andExpect(status().isNotFound());
    }

    @Test
    void getAliases_returnsAliases() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("testuser");
        profile.setEmail("test@example.com");

        OnlineProfile alias = new OnlineProfile();
        alias.setId(2L);
        alias.setName("alias1");
        alias.setEmail("test@example.com");

        when(profileService.getProfile(1L)).thenReturn(profile);
        when(profileRepository.findByEmailExcludingName("test@example.com", "testuser")).thenReturn(List.of(alias));

        mockMvc.perform(get("/api/v1/profiles/aliases")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("alias1"));
    }

    @Test
    void getAliases_profileNotFound() throws Exception {
        when(profileService.getProfile(1L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/profiles/aliases")).andExpect(status().isNotFound());
    }

    @Test
    void retireProfile_ownProfile() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("testuser");
        profile.setEmail("test@example.com");

        when(profileService.getProfile(1L)).thenReturn(profile);
        when(profileService.deleteProfile(1L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/profiles/1/retire")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void retireProfile_aliasWithSameEmail() throws Exception {
        OnlineProfile authenticatedProfile = new OnlineProfile();
        authenticatedProfile.setId(1L);
        authenticatedProfile.setName("testuser");
        authenticatedProfile.setEmail("test@example.com");

        OnlineProfile aliasProfile = new OnlineProfile();
        aliasProfile.setId(2L);
        aliasProfile.setName("alias1");
        aliasProfile.setEmail("test@example.com");

        when(profileService.getProfile(1L)).thenReturn(authenticatedProfile);
        when(profileService.getProfile(2L)).thenReturn(aliasProfile);
        when(profileService.deleteProfile(2L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/profiles/2/retire")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void retireProfile_differentProfile_forbidden() throws Exception {
        OnlineProfile authenticatedProfile = new OnlineProfile();
        authenticatedProfile.setId(1L);
        authenticatedProfile.setName("testuser");
        authenticatedProfile.setEmail("test@example.com");

        OnlineProfile otherProfile = new OnlineProfile();
        otherProfile.setId(999L);
        otherProfile.setName("other");
        otherProfile.setEmail("other@example.com");

        when(profileService.getProfile(1L)).thenReturn(authenticatedProfile);
        when(profileService.getProfile(999L)).thenReturn(otherProfile);

        mockMvc.perform(post("/api/v1/profiles/999/retire")).andExpect(status().isForbidden());
    }

    @Test
    void retireProfile_notFound() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("testuser");
        profile.setEmail("test@example.com");

        when(profileService.getProfile(1L)).thenReturn(profile);
        when(profileService.deleteProfile(1L)).thenReturn(false);

        mockMvc.perform(post("/api/v1/profiles/1/retire")).andExpect(status().isNotFound());
    }

    @Test
    void retireProfile_targetNotFound() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("testuser");
        profile.setEmail("test@example.com");

        when(profileService.getProfile(1L)).thenReturn(profile);
        when(profileService.getProfile(999L)).thenReturn(null);

        mockMvc.perform(post("/api/v1/profiles/999/retire")).andExpect(status().isNotFound());
    }
}
