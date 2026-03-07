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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.donohoedigital.games.poker.gameserver.PokerSimulationService;
import com.donohoedigital.games.poker.protocol.dto.SimulationResult;

@WebMvcTest
@Import({TestSecurityConfiguration.class, SimulationController.class})
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PokerSimulationService simulationService;

    @Test
    void simulate_validRequest_returnsResult() throws Exception {
        SimulationResult result = new SimulationResult(50.0, 5.0, 45.0, 1000, null, Map.of());

        when(simulationService.simulate(anyList(), isNull(), eq(1), eq(1000), isNull(), isNull())).thenReturn(result);

        mockMvc.perform(post("/api/v1/poker/simulate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"holeCards\":[\"Ah\",\"Kd\"],\"numOpponents\":1,\"iterations\":1000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.win").value(50.0));
    }

    @Test
    void simulate_missingHoleCards_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/poker/simulate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numOpponents\":1,\"iterations\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void simulate_missingIterations_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/poker/simulate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"holeCards\":[\"Ah\",\"Kd\"],\"numOpponents\":1}")).andExpect(status().isBadRequest());
    }

    @Test
    void simulate_exhaustiveMode_noIterationsRequired() throws Exception {
        SimulationResult result = new SimulationResult(55.0, 3.0, 42.0, 0, null, Map.of());

        when(simulationService.simulate(anyList(), isNull(), eq(1), isNull(), isNull(), eq(true))).thenReturn(result);

        mockMvc.perform(post("/api/v1/poker/simulate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"holeCards\":[\"Ah\",\"Kd\"],\"numOpponents\":1,\"exhaustive\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.win").value(55.0));
    }

    @Test
    void simulate_withCommunityCards_passedToService() throws Exception {
        SimulationResult result = new SimulationResult(60.0, 5.0, 35.0, 1000, null, Map.of());

        when(simulationService.simulate(anyList(), anyList(), eq(1), eq(1000), isNull(), isNull())).thenReturn(result);

        mockMvc.perform(post("/api/v1/poker/simulate").contentType(MediaType.APPLICATION_JSON).content(
                "{\"holeCards\":[\"Ah\",\"Kd\"],\"communityCards\":[\"Td\",\"9s\",\"2c\"],\"numOpponents\":1,\"iterations\":1000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.win").value(60.0));
    }

    @Test
    void simulate_serviceThrowsIllegalArgument_returns400() throws Exception {
        when(simulationService.simulate(anyList(), isNull(), eq(1), eq(1000), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("Duplicate card"));

        mockMvc.perform(post("/api/v1/poker/simulate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"holeCards\":[\"Ah\",\"Kd\"],\"numOpponents\":1,\"iterations\":1000}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Duplicate card"));
    }
}
