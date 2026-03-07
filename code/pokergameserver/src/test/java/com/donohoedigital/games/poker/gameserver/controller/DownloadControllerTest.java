/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({TestSecurityConfiguration.class, DownloadController.class})
class DownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void downloadFile_nonexistentFile_returns404() throws Exception {
        // The default downloads.dir doesn't exist in test env
        mockMvc.perform(get("/api/v1/downloads/installer.exe")).andExpect(status().isNotFound());
    }

    @Test
    void downloadFile_directoryTraversal_returns400(@TempDir Path tempDir) throws Exception {
        // Set downloads.dir to a temp directory
        System.setProperty("downloads.dir", tempDir.toString());
        try {
            // Attempt directory traversal — the controller normalizes the path
            // and checks it stays within the downloads directory.
            // This should fail with 400 or 404 depending on platform path resolution.
            mockMvc.perform(get("/api/v1/downloads/..%2F..%2Fetc%2Fpasswd")).andExpect(status().isBadRequest());
        } finally {
            System.clearProperty("downloads.dir");
        }
    }
}
