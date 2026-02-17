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
package com.donohoedigital.games.poker.gameserver.integration;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test covering the full workflow: 1. User registration
 * 2. User login 3. Game creation 4. Game joining 5. Game starting 6. Game state
 * retrieval
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
class EndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCompleteGameWorkflow() throws Exception {
        // 1. Register first user
        MvcResult registerResult1 = mockMvc
                .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"username\":\"player1\",\"password\":\"password123\",\"email\":\"player1@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists()).andReturn();

        JsonNode registerResponse1 = objectMapper.readTree(registerResult1.getResponse().getContentAsString());
        String token1 = registerResponse1.get("token").asText();
        Long profileId1 = registerResponse1.get("profileId").asLong();
        assertThat(token1).isNotEmpty();
        assertThat(profileId1).isPositive();

        // 2. Register second user
        MvcResult registerResult2 = mockMvc
                .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"username\":\"player2\",\"password\":\"password456\",\"email\":\"player2@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists()).andReturn();

        JsonNode registerResponse2 = objectMapper.readTree(registerResult2.getResponse().getContentAsString());
        String token2 = registerResponse2.get("token").asText();
        Long profileId2 = registerResponse2.get("profileId").asLong();

        // 3. Login first user (verify authentication works)
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"player1\",\"password\":\"password123\",\"rememberMe\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists());

        // 4. Create a game
        String gameConfigJson = "{\"name\":\"Test Tournament\",\"description\":\"Integration test game\","
                + "\"greeting\":\"Welcome!\",\"maxPlayers\":9,\"maxOnlinePlayers\":90,\"fillComputer\":true,"
                + "\"buyIn\":0,\"startingChips\":1500,"
                + "\"blindStructure\":[{\"smallBlind\":10,\"bigBlind\":20,\"ante\":0,\"minutes\":15,\"isBreak\":false,\"gameType\":\"NOLIMIT_HOLDEM\"}],"
                + "\"doubleAfterLastLevel\":true,\"defaultGameType\":\"NOLIMIT_HOLDEM\",\"levelAdvanceMode\":\"TIME\","
                + "\"handsPerLevel\":10,\"defaultMinutesPerLevel\":15,\"onlineActivatedOnly\":true,"
                + "\"allowDash\":false,\"allowAdvisor\":false}";

        MvcResult createGameResult = mockMvc
                .perform(post("/api/v1/games").contentType(MediaType.APPLICATION_JSON).content(gameConfigJson)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.gameId").exists()).andReturn();

        JsonNode createGameResponse = objectMapper.readTree(createGameResult.getResponse().getContentAsString());
        String gameId = createGameResponse.get("gameId").asText();
        assertThat(gameId).isNotEmpty();

        // 5. Get game state (should be WAITING_FOR_PLAYERS)
        mockMvc.perform(get("/api/v1/games/" + gameId).header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gameId").value(gameId))
                .andExpect(jsonPath("$.name").value("Test Tournament"))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PLAYERS"))
                .andExpect(jsonPath("$.playerCount").value(1)).andExpect(jsonPath("$.maxPlayers").value(9));

        // 6. List games (should show our game)
        mockMvc.perform(get("/api/v1/games").header("Authorization", "Bearer " + token1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].gameId").value(gameId))
                .andExpect(jsonPath("$[0].ownerName").value("player1"));

        // 7. Second player joins the game
        mockMvc.perform(post("/api/v1/games/" + gameId + "/join").header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());

        // 8. Verify player count increased
        mockMvc.perform(get("/api/v1/games/" + gameId).header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.playerCount").value(2));

        // 9. Start the game
        mockMvc.perform(post("/api/v1/games/" + gameId + "/start").header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        // 10. Verify game status changed to IN_PROGRESS
        mockMvc.perform(get("/api/v1/games/" + gameId).header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 11. Verify profile retrieval works
        mockMvc.perform(get("/api/v1/profiles/" + profileId1).header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(profileId1))
                .andExpect(jsonPath("$.name").value("player1"));
    }
}
