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
package com.donohoedigital.games.poker.gameserver;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for PokerSimulationService Monte Carlo equity calculator.
 */
class PokerSimulationServiceTest {

    private PokerSimulationService service;

    @BeforeEach
    void setUp() {
        service = new PokerSimulationService();
    }

    @Test
    void aaVsOneRandomOpponent_winPercentAbove80() {
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of(), 1, 10000, null);

        assertTrue(result.win() > 80, "AA should win > 80% vs 1 random opponent, got " + result.win());
        assertPercentagesSumTo100(result);
        assertEquals(10000, result.iterations());
        assertNull(result.opponentResults());
    }

    @Test
    void aaVsKkKnown_winPercentAround80() {
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of(), 1, 10000,
                List.of(List.of("Kh", "Ks")));

        assertTrue(result.win() > 75, "AA vs KK should win > 75%, got " + result.win());
        assertTrue(result.win() < 90, "AA vs KK should win < 90%, got " + result.win());
        assertPercentagesSumTo100(result);

        // Should have per-opponent results since known hands were provided
        assertNotNull(result.opponentResults());
        assertEquals(1, result.opponentResults().size());

        SimulationResult.OpponentResult oppResult = result.opponentResults().get(0);
        assertPercentagesSumTo100(oppResult);
        // Opponent's win% should be complementary to player's loss%
        assertEquals(result.loss(), oppResult.win(), 0.01);
    }

    @Test
    void preFlopWithTwoOpponents_percentagesSumTo100() {
        SimulationResult result = service.simulate(List.of("Th", "Ts"), List.of(), 2, 5000, null);

        assertPercentagesSumTo100(result);
        assertTrue(result.win() > 0);
        assertTrue(result.loss() > 0);
    }

    @Test
    void fullBoard_deterministicResult() {
        // AA vs KK with full board: Qh Jd 9c 5s 2d - no help for either
        // AA should win 100% here
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of("Qh", "Jd", "9c", "5s", "2d"), 1, 1000,
                List.of(List.of("Kh", "Ks")));

        assertEquals(100.0, result.win(), 0.01);
        assertEquals(0.0, result.loss(), 0.01);
        assertEquals(0.0, result.tie(), 0.01);
    }

    @Test
    void fullBoard_tieScenario() {
        // Same straight for both players on the board
        // Board: Ts Jc Qd Kh Ah - both have broadway straight
        // Player: 2s 3c, Opponent: 4s 5c - board straight is best for both
        SimulationResult result = service.simulate(List.of("2s", "3c"), List.of("Ts", "Jc", "Qd", "Kh", "Ah"), 1, 1000,
                List.of(List.of("4s", "5c")));

        assertEquals(0.0, result.loss(), 0.01);
        assertEquals(100.0, result.tie(), 0.01);
        assertEquals(0.0, result.win(), 0.01);
    }

    @Test
    void zeroCommunityCards_preFlopSimulation() {
        SimulationResult result = service.simulate(List.of("Ah", "Kh"), List.of(), 1, 5000, null);

        assertPercentagesSumTo100(result);
        assertTrue(result.win() > 0);
        assertTrue(result.loss() > 0);
    }

    @Test
    void fiveCommunityCards_fullBoard() {
        // Player has flush draw that completes
        SimulationResult result = service.simulate(List.of("Ah", "Kh"), List.of("Qh", "Jh", "Th", "2c", "3d"), 1, 1000,
                List.of(List.of("As", "Ks")));

        // Player has a royal flush, opponent has two pair at best
        assertEquals(100.0, result.win(), 0.01);
    }

    @Test
    void duplicateCards_rejected() {
        // Same card in hole cards
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Ah", "Ah"), List.of(), 1, 1000, null));
    }

    @Test
    void duplicateCardAcrossHoleAndCommunity_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Ah", "Kh"), List.of("Ah"), 1, 1000, null));
    }

    @Test
    void duplicateCardAcrossHoleAndOpponent_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Ah", "Kh"), List.of(), 1, 1000, List.of(List.of("Ah", "Ks"))));
    }

    @Test
    void invalidCardFormat_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Xx", "Yy"), List.of(), 1, 1000, null));
    }

    @Test
    void invalidCardSingleChar_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("A", "K"), List.of(), 1, 1000, null));
    }

    @Test
    void knownOpponentWithRandomOpponents_mixedResults() {
        // 1 known opponent + 1 random = 2 total opponents
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of(), 2, 5000,
                List.of(List.of("Kh", "Ks")));

        assertPercentagesSumTo100(result);
        assertNotNull(result.opponentResults());
        assertEquals(2, result.opponentResults().size());
    }

    @Test
    void parseCards_validCards() {
        List<com.donohoedigital.games.poker.engine.Card> cards = service.parseCards(List.of("Ah", "Kd", "Qs", "Jc"));
        assertEquals(4, cards.size());
    }

    @Test
    void parseCards_caseInsensitive() {
        // Card.getCard handles case-insensitive input
        List<com.donohoedigital.games.poker.engine.Card> lower = service.parseCards(List.of("ah", "kd"));
        List<com.donohoedigital.games.poker.engine.Card> upper = service.parseCards(List.of("Ah", "Kd"));
        assertEquals(lower.get(0), upper.get(0));
        assertEquals(lower.get(1), upper.get(1));
    }

    private void assertPercentagesSumTo100(SimulationResult result) {
        double sum = result.win() + result.tie() + result.loss();
        assertEquals(100.0, sum, 0.1, "Win + Tie + Loss should sum to ~100, got " + sum);
    }

    private void assertPercentagesSumTo100(SimulationResult.OpponentResult result) {
        double sum = result.win() + result.tie() + result.loss();
        assertEquals(100.0, sum, 0.1, "Opponent Win + Tie + Loss should sum to ~100, got " + sum);
    }
}
