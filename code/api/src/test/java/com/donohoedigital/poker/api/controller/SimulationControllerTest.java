/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.poker.api.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.donohoedigital.games.poker.gameserver.PokerSimulationService;
import com.donohoedigital.games.poker.gameserver.SimulationResult;
import com.donohoedigital.poker.api.dto.SimulationRequest;

/**
 * Unit tests for SimulationController. Tests the controller directly without
 * Spring context to avoid Java 25 / Spring Boot test compatibility issues.
 */
class SimulationControllerTest {

    private SimulationController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new SimulationController();
        // Inject the service via reflection since @Autowired won't run without Spring
        PokerSimulationService service = new PokerSimulationService();
        java.lang.reflect.Field field = SimulationController.class.getDeclaredField("simulationService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    @Test
    void simulate_validRequest_returnsOk() {
        SimulationRequest request = new SimulationRequest(List.of("Ah", "As"), List.of(), 1, 1000, null, null);

        ResponseEntity<?> response = controller.simulate(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(SimulationResult.class, response.getBody());

        SimulationResult result = (SimulationResult) response.getBody();
        assertEquals(1000, result.iterations());
        double sum = result.win() + result.tie() + result.loss();
        assertEquals(100.0, sum, 0.1);
    }

    @Test
    void simulate_withCommunityCards_returnsOk() {
        SimulationRequest request = new SimulationRequest(List.of("Ah", "Kh"), List.of("Qh", "Jh", "Th"), 1, 500, null,
                null);

        ResponseEntity<?> response = controller.simulate(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(SimulationResult.class, response.getBody());
    }

    @Test
    void simulate_withKnownOpponents_returnsOpponentResults() {
        SimulationRequest request = new SimulationRequest(List.of("Ah", "As"), List.of(), 1, 1000,
                List.of(List.of("Kh", "Ks")), null);

        ResponseEntity<?> response = controller.simulate(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SimulationResult result = (SimulationResult) response.getBody();
        assertNotNull(result.opponentResults());
        assertEquals(1, result.opponentResults().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void simulate_duplicateCards_returnsBadRequest() {
        SimulationRequest request = new SimulationRequest(List.of("Ah", "Ah"), List.of(), 1, 1000, null, null);

        ResponseEntity<?> response = controller.simulate(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").contains("Duplicate"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void simulate_invalidCardFormat_returnsBadRequest() {
        SimulationRequest request = new SimulationRequest(List.of("Xx", "Yy"), List.of(), 1, 1000, null, null);

        ResponseEntity<?> response = controller.simulate(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").contains("Invalid card"));
    }

    @Test
    void simulate_fullBoard_deterministicResult() {
        SimulationRequest request = new SimulationRequest(List.of("Ah", "As"), List.of("Qh", "Jd", "9c", "5s", "2d"), 1,
                100, List.of(List.of("Kh", "Ks")), null);

        ResponseEntity<?> response = controller.simulate(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SimulationResult result = (SimulationResult) response.getBody();
        assertEquals(100.0, result.win(), 0.01);
    }

    @Test
    void simulate_multipleOpponents_returnsCorrectCount() {
        SimulationRequest request = new SimulationRequest(List.of("Ah", "As"), List.of(), 3, 500,
                List.of(List.of("Kh", "Ks")), null);

        ResponseEntity<?> response = controller.simulate(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SimulationResult result = (SimulationResult) response.getBody();
        assertNotNull(result.opponentResults());
        assertEquals(3, result.opponentResults().size());
    }
}
