/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
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

/**
 * Unit tests for PUT /api/v1/profiles/{id}/password added to ProfileController
 * in M7 Phase 7.1.
 *
 * <p>
 * TestSecurityConfiguration injects authenticated user with profileId=1L.
 */
@WebMvcTest
@Import({TestSecurityConfiguration.class, ProfileController.class})
class ProfileControllerPasswordTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void changePassword_ownProfile_correctOldPassword_returns200() throws Exception {
        doNothing().when(profileService).changePassword(eq(1L), eq("oldPass123"), eq("newPass456"));

        mockMvc.perform(put("/api/v1/profiles/1/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"oldPass123\",\"newPassword\":\"newPass456\"}")).andExpect(status().isOk());
    }

    @Test
    void changePassword_wrongOldPassword_returns403() throws Exception {
        doThrow(new ProfileService.InvalidPasswordException()).when(profileService).changePassword(eq(1L), anyString(),
                anyString());

        mockMvc.perform(put("/api/v1/profiles/1/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"wrongPass\",\"newPassword\":\"newPass456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_differentProfile_returns403() throws Exception {
        // TestSecurityConfiguration sets profileId=1L; trying to change profile 999 →
        // forbidden
        mockMvc.perform(put("/api/v1/profiles/999/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"oldPass123\",\"newPassword\":\"newPass456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_newPasswordTooShort_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/1/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"oldPass123\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_blankOldPassword_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/1/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"\",\"newPassword\":\"newPass456\"}")).andExpect(status().isBadRequest());
    }
}
