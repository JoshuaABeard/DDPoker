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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donohoedigital.games.poker.gameserver.PokerSimulationService;
import com.donohoedigital.games.poker.gameserver.SimulationResult;
import com.donohoedigital.poker.api.dto.SimulationRequest;

import jakarta.validation.Valid;

/**
 * REST endpoint for poker equity simulation.
 */
@RestController
@RequestMapping("/api/v1/poker")
public class SimulationController {

    @Autowired
    private PokerSimulationService simulationService;

    /**
     * Run a poker equity simulation.
     *
     * <p>
     * Provide either {@code iterations} (100-100000) for Monte Carlo mode, or
     * {@code exhaustive=true} to enumerate all possible board completions.
     */
    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@Valid @RequestBody SimulationRequest request) {
        if (request.holeCards() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "holeCards must be provided"));
        }
        if (!Boolean.TRUE.equals(request.exhaustive()) && request.iterations() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "iterations (100-100000) must be provided"));
        }

        try {
            SimulationResult result = simulationService.simulate(request.holeCards(), request.communityCards(),
                    request.numOpponents(), request.iterations(), request.knownOpponentHands(), request.exhaustive());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
