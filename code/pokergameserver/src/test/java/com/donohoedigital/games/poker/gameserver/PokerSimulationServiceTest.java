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
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for PokerSimulationService Monte Carlo equity calculator and multi-hand
 * showdown simulation.
 */
class PokerSimulationServiceTest {

    private PokerSimulationService service;

    @BeforeEach
    void setUp() {
        service = new PokerSimulationService();
    }

    // -------------------------------------------------------------------------
    // Monte Carlo simulate() tests
    // -------------------------------------------------------------------------

    @Test
    void aaVsOneRandomOpponent_winPercentAbove80() {
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of(), 1, 10000, null, null);

        assertTrue(result.win() > 80, "AA should win > 80% vs 1 random opponent, got " + result.win());
        assertPercentagesSumTo100(result);
        assertEquals(10000, result.iterations());
        assertNull(result.opponentResults());
    }

    @Test
    void aaVsKkKnown_winPercentAround80() {
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of(), 1, 10000,
                List.of(List.of("Kh", "Ks")), null);

        assertTrue(result.win() > 75, "AA vs KK should win > 75%, got " + result.win());
        assertTrue(result.win() < 90, "AA vs KK should win < 90%, got " + result.win());
        assertPercentagesSumTo100(result);

        assertNotNull(result.opponentResults());
        assertEquals(1, result.opponentResults().size());

        SimulationResult.OpponentResult oppResult = result.opponentResults().get(0);
        assertPercentagesSumTo100(oppResult);
        assertEquals(result.loss(), oppResult.win(), 0.01);
    }

    @Test
    void preFlopWithTwoOpponents_percentagesSumTo100() {
        SimulationResult result = service.simulate(List.of("Th", "Ts"), List.of(), 2, 5000, null, null);

        assertPercentagesSumTo100(result);
        assertTrue(result.win() > 0);
        assertTrue(result.loss() > 0);
    }

    @Test
    void fullBoard_deterministicResult() {
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of("Qh", "Jd", "9c", "5s", "2d"), 1, 1000,
                List.of(List.of("Kh", "Ks")), null);

        assertEquals(100.0, result.win(), 0.01);
        assertEquals(0.0, result.loss(), 0.01);
        assertEquals(0.0, result.tie(), 0.01);
    }

    @Test
    void fullBoard_tieScenario() {
        SimulationResult result = service.simulate(List.of("2s", "3c"), List.of("Ts", "Jc", "Qd", "Kh", "Ah"), 1, 1000,
                List.of(List.of("4s", "5c")), null);

        assertEquals(0.0, result.loss(), 0.01);
        assertEquals(100.0, result.tie(), 0.01);
        assertEquals(0.0, result.win(), 0.01);
    }

    @Test
    void zeroCommunityCards_preFlopSimulation() {
        SimulationResult result = service.simulate(List.of("Ah", "Kh"), List.of(), 1, 5000, null, null);

        assertPercentagesSumTo100(result);
        assertTrue(result.win() > 0);
        assertTrue(result.loss() > 0);
    }

    @Test
    void fiveCommunityCards_fullBoard() {
        SimulationResult result = service.simulate(List.of("Ah", "Kh"), List.of("Qh", "Jh", "Th", "2c", "3d"), 1, 1000,
                List.of(List.of("As", "Ks")), null);

        assertEquals(100.0, result.win(), 0.01);
    }

    @Test
    void duplicateCards_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Ah", "Ah"), List.of(), 1, 1000, null, null));
    }

    @Test
    void duplicateCardAcrossHoleAndCommunity_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Ah", "Kh"), List.of("Ah"), 1, 1000, null, null));
    }

    @Test
    void duplicateCardAcrossHoleAndOpponent_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Ah", "Kh"), List.of(), 1, 1000, List.of(List.of("Ah", "Ks")), null));
    }

    @Test
    void invalidCardFormat_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Xx", "Yy"), List.of(), 1, 1000, null, null));
    }

    @Test
    void invalidCardSingleChar_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("A", "K"), List.of(), 1, 1000, null, null));
    }

    @Test
    void knownOpponentWithRandomOpponents_mixedResults() {
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of(), 2, 5000,
                List.of(List.of("Kh", "Ks")), null);

        assertPercentagesSumTo100(result);
        assertNotNull(result.opponentResults());
        assertEquals(2, result.opponentResults().size());
    }

    @Test
    void knownOpponentHandsExceedNumOpponents_rejected() {
        assertThrows(IllegalArgumentException.class, () -> service.simulate(List.of("Ah", "As"), List.of(), 1, 1000,
                List.of(List.of("Kh", "Ks"), List.of("Qh", "Qs")), null));
    }

    @Test
    void knownOpponentHandWithWrongCardCount_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Ah", "As"), List.of(), 1, 1000, List.of(List.of("Kh")), null));

        assertThrows(IllegalArgumentException.class, () -> service.simulate(List.of("Ah", "As"), List.of(), 1, 1000,
                List.of(List.of("Kh", "Ks", "Qh")), null));
    }

    @Test
    void parseCards_validCards() {
        List<com.donohoedigital.games.poker.engine.Card> cards = service.parseCards(List.of("Ah", "Kd", "Qs", "Jc"));
        assertEquals(4, cards.size());
    }

    @Test
    void parseCards_caseInsensitive() {
        List<com.donohoedigital.games.poker.engine.Card> lower = service.parseCards(List.of("ah", "kd"));
        List<com.donohoedigital.games.poker.engine.Card> upper = service.parseCards(List.of("Ah", "Kd"));
        assertEquals(lower.get(0), upper.get(0));
        assertEquals(lower.get(1), upper.get(1));
    }

    // -------------------------------------------------------------------------
    // Hand type breakdown tests
    // -------------------------------------------------------------------------

    @Test
    void monteCarlo_handTypeBreakdownPopulated() {
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of(), 1, 5000, null, null);

        Map<String, Double> breakdown = result.playerHandTypeBreakdown();
        assertNotNull(breakdown);
        assertFalse(breakdown.isEmpty(), "Hand type breakdown should not be empty");

        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            assertTrue(entry.getValue() > 0, "All breakdown entries should have positive percentage");
            assertTrue(entry.getValue() <= 100, "Breakdown percentage should not exceed 100");
        }

        double sum = breakdown.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(100.0, sum, 1.0, "Breakdown percentages should sum to ~100%, got " + sum);
    }

    @Test
    void monteCarlo_fullBoard_handTypeBreakdownIsSingleEntry() {
        // AA vs board with no help - result is one pair (AA) every time
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of("Qh", "Jd", "9c", "5s", "2d"), 1, 100,
                List.of(List.of("Kh", "Ks")), null);

        Map<String, Double> breakdown = result.playerHandTypeBreakdown();
        assertNotNull(breakdown);
        assertEquals(1, breakdown.size(), "Full board with AA should have exactly one hand type");
        assertTrue(breakdown.containsKey("ONE_PAIR"), "AA on this board makes one pair");
        assertEquals(100.0, breakdown.get("ONE_PAIR"), 0.01);
    }

    // -------------------------------------------------------------------------
    // Exhaustive mode tests
    // -------------------------------------------------------------------------

    @Test
    void exhaustive_riverBoard_trivialCase() {
        // River: 5 community cards already dealt - only 1 board to evaluate
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of("Qh", "Jd", "9c", "5s", "2d"), 1, null,
                List.of(List.of("Kh", "Ks")), true);

        assertEquals(1, result.iterations(), "River exhaustive should evaluate exactly 1 board");
        assertEquals(100.0, result.win(), 0.01);
        assertPercentagesSumTo100(result);

        Map<String, Double> breakdown = result.playerHandTypeBreakdown();
        assertNotNull(breakdown);
        assertFalse(breakdown.isEmpty());
    }

    @Test
    void exhaustive_turnBoard_44Boards() {
        // Turn: 4 community cards, 1 known opponent. Remaining deck: 52 - 2 - 4 - 2 =
        // 44 cards. Exhaustive should evaluate exactly 44 boards.
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of("Qh", "Jd", "9c", "5s"), 1, null,
                List.of(List.of("Kh", "Ks")), true);

        assertEquals(44, result.iterations(), "Turn exhaustive with known opponent should evaluate 44 boards");
        assertPercentagesSumTo100(result);
        assertTrue(result.win() > 50, "AA should win majority on this board vs KK");

        Map<String, Double> breakdown = result.playerHandTypeBreakdown();
        assertNotNull(breakdown);
        assertFalse(breakdown.isEmpty());
        double sum = breakdown.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(100.0, sum, 0.5);
    }

    @Test
    void exhaustive_tieScenario_turnBoard() {
        // Board straight scenario on the turn: Ts Jc Qd Kh, player 2s 3c, opp 4s 5c.
        // Most rivers produce a tie (board dominates).
        SimulationResult result = service.simulate(List.of("2s", "3c"), List.of("Ts", "Jc", "Qd", "Kh"), 1, null,
                List.of(List.of("4s", "5c")), true);

        assertPercentagesSumTo100(result);
        // Exhaustive mode: iterations == number of river cards evaluated (44 boards)
        assertEquals(44, result.iterations());
        // The vast majority of outcomes should be ties (board high cards dominate)
        assertTrue(result.tie() > 50.0, "Most boards should be ties, got tie=" + result.tie());
    }

    @Test
    void exhaustive_flopWithRandomOpponent_rejectsOverLimit() {
        // Flop (3 community cards) + 1 random opponent produces too many combinations.
        assertThrows(IllegalArgumentException.class,
                () -> service.simulate(List.of("Ah", "As"), List.of("Qh", "Jd", "9c"), 1, null, null, true),
                "Exhaustive on flop with random opponent should throw due to combo limit");
    }

    @Test
    void exhaustive_flopWithAllKnownOpponents_acceptsUnder10000() {
        // Flop (3 community cards) + 1 known opponent: no random opp hands to
        // enumerate. Remaining deck = 52 - 2 - 3 - 2 = 45 cards. C(45,2) = 990.
        SimulationResult result = service.simulate(List.of("Ah", "As"), List.of("Qh", "Jd", "9c"), 1, null,
                List.of(List.of("Kh", "Ks")), true);

        assertEquals(990, result.iterations(),
                "Flop exhaustive with known opponent should evaluate C(45,2)=990 boards");
        assertPercentagesSumTo100(result);
    }

    // -------------------------------------------------------------------------
    // countExhaustiveCombos() unit tests
    // -------------------------------------------------------------------------

    @Test
    void countExhaustiveCombos_riverNoRandomOpps() {
        // River board: 0 community needed, 0 random opponents -> 1 combo
        assertEquals(1, PokerSimulationService.countExhaustiveCombos(44, 0, 0));
    }

    @Test
    void countExhaustiveCombos_turnKnownOpponent() {
        // Turn, 1 known opponent: 44 remaining, need 1 community card, 0 random opps
        assertEquals(44, PokerSimulationService.countExhaustiveCombos(44, 1, 0));
    }

    @Test
    void countExhaustiveCombos_flopNoRandomOpps() {
        // Flop, 1 known opponent: 45 remaining, need 2 community, 0 random opps
        // C(45,2) = 990
        assertEquals(990, PokerSimulationService.countExhaustiveCombos(45, 2, 0));
    }

    @Test
    void countExhaustiveCombos_turnOneRandomOpp() {
        // Turn, 1 random opp: 44 remaining, 1 community needed, then C(43,2)=903 opp
        // hands. Total = 44 * 903 = 39,732
        assertEquals(44 * 903L, PokerSimulationService.countExhaustiveCombos(44, 1, 1));
    }

    // -------------------------------------------------------------------------
    // Multi-hand showdown tests
    // -------------------------------------------------------------------------

    @Test
    void multiHand_AAvsKK_returnsPerHandEquity() {
        List<List<String>> hands = List.of(List.of("Ah", "Ad"), List.of("Kh", "Kd"));
        SimulationResult result = service.simulateMultiHand(hands, List.of(), 10000);
        assertNotNull(result.handResults());
        assertEquals(2, result.handResults().size());
        assertTrue(result.handResults().get(0).win() > 70.0,
                "AA should win > 70% vs KK, got " + result.handResults().get(0).win());
        assertTrue(result.handResults().get(1).win() > 10.0,
                "KK should win > 10% vs AA, got " + result.handResults().get(1).win());
    }

    @Test
    void multiHand_withCommunity_deterministicOnRiver() {
        List<List<String>> hands = List.of(List.of("Ah", "Kh"), List.of("2c", "7d"));
        List<String> community = List.of("Ac", "Kc", "3s", "8d", "Jh");
        SimulationResult result = service.simulateMultiHand(hands, community, 1000);
        assertEquals(100.0, result.handResults().get(0).win(), 0.01, "AK with two pair on full board should win 100%");
    }

    @Test
    void multiHand_perHandWinTieLossSumTo100() {
        List<List<String>> hands = List.of(List.of("Ah", "As"), List.of("Kh", "Ks"), List.of("Qh", "Qs"));
        SimulationResult result = service.simulateMultiHand(hands, List.of(), 5000);
        assertNotNull(result.handResults());
        assertEquals(3, result.handResults().size());
        for (SimulationResult.HandResult hr : result.handResults()) {
            double sum = hr.win() + hr.tie() + hr.loss();
            assertEquals(100.0, sum, 0.5, "Each hand's win+tie+loss should sum to ~100, got " + sum);
        }
    }

    @Test
    void multiHand_tieScenario_boardStraightDominates() {
        List<List<String>> hands = List.of(List.of("2s", "3c"), List.of("4s", "5c"));
        List<String> community = List.of("Ts", "Jc", "Qd", "Kh", "Ah");
        SimulationResult result = service.simulateMultiHand(hands, community, 100);
        assertEquals(100.0, result.handResults().get(0).tie(), 0.01);
        assertEquals(100.0, result.handResults().get(1).tie(), 0.01);
        assertEquals(0.0, result.handResults().get(0).win(), 0.01);
        assertEquals(0.0, result.handResults().get(1).win(), 0.01);
    }

    @Test
    void multiHand_duplicateCardAcrossHands_rejected() {
        List<List<String>> hands = List.of(List.of("Ah", "Kh"), List.of("Ah", "Qs"));
        assertThrows(IllegalArgumentException.class, () -> service.simulateMultiHand(hands, List.of(), 1000));
    }

    @Test
    void multiHand_duplicateCardInCommunity_rejected() {
        List<List<String>> hands = List.of(List.of("Ah", "Kh"), List.of("Qh", "Qs"));
        assertThrows(IllegalArgumentException.class, () -> service.simulateMultiHand(hands, List.of("Ah"), 1000));
    }

    @Test
    void multiHand_singleHand_returnsOneResult() {
        List<List<String>> hands = List.of(List.of("Ah", "As"));
        SimulationResult result = service.simulateMultiHand(hands, List.of(), 1000);
        assertNotNull(result.handResults());
        assertEquals(1, result.handResults().size());
        assertEquals(100.0, result.handResults().get(0).win(), 0.01);
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
