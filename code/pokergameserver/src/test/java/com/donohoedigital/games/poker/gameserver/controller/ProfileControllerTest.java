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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.service.ProfileService;
import com.donohoedigital.games.poker.model.OnlineProfile;

@WebMvcTest
@Import({TestSecurityConfiguration.class, ProfileController.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void testGetProfile() throws Exception {
        OnlineProfile profile = new OnlineProfile();
        profile.setId(1L);
        profile.setName("testuser");
        profile.setEmail("test@example.com");

        when(profileService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/profiles/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testGetProfileNotFound() throws Exception {
        when(profileService.getProfile(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/profiles/999")).andExpect(status().isNotFound());
    }

    @Test
    void testUpdateProfile() throws Exception {
        when(profileService.updateProfile(eq(1L), anyString())).thenReturn(true);

        mockMvc.perform(put("/api/v1/profiles/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\"}")).andExpect(status().isOk());
    }

    @Test
    void testUpdateProfileForbidden() throws Exception {
        // Attempting to update a different user's profile should return 403
        mockMvc.perform(put("/api/v1/profiles/999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\"}")).andExpect(status().isForbidden());
    }

    @Test
    void testDeleteProfile() throws Exception {
        when(profileService.deleteProfile(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/profiles/1")).andExpect(status().isNoContent());
    }

    @Test
    void testDeleteProfileForbidden() throws Exception {
        // Attempting to delete a different user's profile should return 403
        mockMvc.perform(delete("/api/v1/profiles/999")).andExpect(status().isForbidden());
    }
}
