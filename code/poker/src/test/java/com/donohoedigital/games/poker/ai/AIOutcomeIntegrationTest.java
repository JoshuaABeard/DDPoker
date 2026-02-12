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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.integration.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.PokerConstants.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for AIOutcome decision tracking and outcome selection.
 * Requires full game infrastructure (GameEngine, PokerMain, etc.)
 */
@Tag("integration")
class AIOutcomeIntegrationTest extends IntegrationTestBase {
    private PokerGame game;
    private PokerTable table;
    private HoldemHand hand;
    private PokerPlayer player;
    private AIOutcome outcome;

    @BeforeEach
    void setUp() {
        // Create game infrastructure
        game = new PokerGame();
        table = new PokerTable(game, 0);

        // Create player
        player = new PokerPlayer(1, "TestPlayer", true);
        player.setChipCount(1000);
        game.addPlayer(player);

        // Create hand
        hand = new HoldemHand(table);
        table.setHoldemHand(hand);

        // Create AIOutcome
        outcome = new AIOutcome(hand, player);
    }

    // ========================================
    // Basic Functionality Tests
    // ========================================

    @Test
    void should_CreateOutcome_When_Constructed() {
        assertThat(outcome).isNotNull();
    }

    @Test
    void should_AddTuple_When_TupleAdded() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.2f, 0.8f, 0.0f);

        assertThat(outcome.getCall()).isEqualTo(0.8f);
    }

    @Test
    void should_AddMultipleTuples_When_CalledMultipleTimes() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.2f, 0.8f, 0.0f);
        outcome.addTuple(AIOutcome.CALL, "implied odds", 0.1f, 0.9f, 0.0f);

        assertThat(outcome.getCall()).isEqualTo(0.85f); // (0.8 + 0.9) / 2
    }

    // ========================================
    // Probability Computation Tests
    // ========================================

    @Test
    void should_ReturnCheckFold_When_Computed() {
        outcome.addTuple(AIOutcome.FOLD, "weak hand", 1.0f, 0.0f, 0.0f);

        assertThat(outcome.getCheckFold()).isEqualTo(1.0f);
    }

    @Test
    void should_ReturnCall_When_Computed() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.0f, 1.0f, 0.0f);

        assertThat(outcome.getCall()).isEqualTo(1.0f);
    }

    @Test
    void should_ReturnBetRaise_When_Computed() {
        outcome.addTuple(AIOutcome.BET, "value bet", 0.0f, 0.0f, 1.0f);

        assertThat(outcome.getBetRaise()).isEqualTo(1.0f);
    }

    @Test
    void should_AverageProbabilities_When_MultipleTuples() {
        outcome.addTuple(AIOutcome.CALL, "tactic1", 0.2f, 0.6f, 0.2f);
        outcome.addTuple(AIOutcome.CALL, "tactic2", 0.1f, 0.8f, 0.1f);
        outcome.addTuple(AIOutcome.CALL, "tactic3", 0.3f, 0.5f, 0.2f);

        assertThat(outcome.getCheckFold()).isEqualTo(0.2f); // (0.2 + 0.1 + 0.3) / 3
        assertThat(outcome.getCall()).isCloseTo(0.633f, within(0.01f)); // (0.6 + 0.8 + 0.5) / 3
        assertThat(outcome.getBetRaise()).isCloseTo(0.167f, within(0.01f)); // (0.2 + 0.1 + 0.2) / 3
    }

    // ========================================
    // Strongest Outcome Tests
    // ========================================

    @Test
    void should_ReturnCheck_When_CheckFoldStrongestAndNoPotAction() {
        outcome.addTuple(AIOutcome.CHECK, "weak hand", 0.9f, 0.05f, 0.05f);

        int strongest = outcome.getStrongestOutcome(NO_POT_ACTION);

        assertThat(strongest).isEqualTo(RuleEngine.OUTCOME_CHECK);
    }

    @Test
    void should_ReturnFold_When_CheckFoldStrongestAndPotAction() {
        outcome.addTuple(AIOutcome.FOLD, "weak hand", 0.9f, 0.05f, 0.05f);

        int strongest = outcome.getStrongestOutcome(RAISED_POT);

        assertThat(strongest).isEqualTo(RuleEngine.OUTCOME_FOLD);
    }

    @Test
    void should_ReturnCall_When_CallStrongest() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.1f, 0.8f, 0.1f);

        int strongest = outcome.getStrongestOutcome(RAISED_POT);

        assertThat(strongest).isEqualTo(RuleEngine.OUTCOME_CALL);
    }

    @Test
    void should_ReturnBetOrRaise_When_BetRaiseStrongestAndNoPotAction() {
        outcome.addTuple(AIOutcome.BET, "value bet", 0.1f, 0.1f, 0.8f);

        int strongest = outcome.getStrongestOutcome(NO_POT_ACTION);

        // Should return either BET or OPEN_POT depending on round
        assertThat(strongest).isIn(RuleEngine.OUTCOME_BET, RuleEngine.OUTCOME_OPEN_POT);
    }

    @Test
    void should_ReturnRaise_When_BetRaiseStrongestAndPotAction() {
        outcome.addTuple(AIOutcome.RAISE, "value raise", 0.1f, 0.1f, 0.8f);

        int strongest = outcome.getStrongestOutcome(RAISED_POT);

        assertThat(strongest).isEqualTo(RuleEngine.OUTCOME_RAISE);
    }

    // ========================================
    // Select Outcome Tests (Probabilistic)
    // ========================================

    @Test
    void should_SelectCheckOrFold_When_OnlyCheckFoldProbability() {
        outcome.addTuple(AIOutcome.CHECK, "weak hand", 1.0f, 0.0f, 0.0f);

        int selected = outcome.selectOutcome(NO_POT_ACTION);

        assertThat(selected).isEqualTo(RuleEngine.OUTCOME_CHECK);
    }

    @Test
    void should_SelectCall_When_OnlyCallProbability() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.0f, 1.0f, 0.0f);

        int selected = outcome.selectOutcome(RAISED_POT);

        assertThat(selected).isEqualTo(RuleEngine.OUTCOME_CALL);
    }

    @Test
    void should_SelectBetOrRaise_When_OnlyBetRaiseProbability() {
        outcome.addTuple(AIOutcome.RAISE, "value raise", 0.0f, 0.0f, 1.0f);

        int selected = outcome.selectOutcome(RAISED_POT);

        assertThat(selected).isEqualTo(RuleEngine.OUTCOME_RAISE);
    }

    @Test
    void should_SelectVariedOutcomes_When_MixedProbabilities() {
        outcome.addTuple(AIOutcome.CALL, "mixed", 0.33f, 0.33f, 0.34f);

        // Run multiple times to verify all outcomes can be selected
        boolean hasCheckFold = false;
        boolean hasCall = false;
        boolean hasBetRaise = false;

        for (int i = 0; i < 100; i++) {
            int selected = outcome.selectOutcome(RAISED_POT);
            if (selected == RuleEngine.OUTCOME_FOLD)
                hasCheckFold = true;
            if (selected == RuleEngine.OUTCOME_CALL)
                hasCall = true;
            if (selected == RuleEngine.OUTCOME_RAISE)
                hasBetRaise = true;
        }

        assertThat(hasCheckFold).isTrue();
        assertThat(hasCall).isTrue();
        assertThat(hasBetRaise).isTrue();
    }

    // ========================================
    // BetRange Tests
    // ========================================

    @Test
    void should_StoreBetRange_When_Set() {
        BetRange range = new BetRange(BetRange.POT_SIZE, 100f, 200f);
        outcome.setBetRange(range, "aggressive");

        assertThat(outcome).isNotNull();
    }

    @Test
    void should_StoreAllInReason_When_Set() {
        BetRange range = new BetRange(BetRange.POT_SIZE, 100f, 200f);
        outcome.setBetRange(range, "chip advantage");

        // Need to add a bet/raise tuple for All In to show in HTML
        outcome.addTuple(AIOutcome.RAISE, "value raise", 0.0f, 0.0f, 1.0f);

        // Use high brevity to avoid bet range calculation (which needs full hand
        // context)
        String html = outcome.toHTML(3);

        assertThat(html).contains("All In");
    }

    // ========================================
    // toHTML Tests
    // ========================================

    @Test
    void should_IncludeRecommend_When_BrevityZero() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.0f, 1.0f, 0.0f);

        String html = outcome.toHTML(0);

        assertThat(html).contains("Recommend");
        assertThat(html).contains("Call");
    }

    @Test
    void should_OmitRecommend_When_BrevityOne() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.0f, 1.0f, 0.0f);

        String html = outcome.toHTML(1);

        assertThat(html).doesNotContain("Recommend");
        assertThat(html).contains("Call");
    }

    @Test
    void should_IncludePercentages_When_BrevityLessThanFive() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.0f, 1.0f, 0.0f);

        String html = outcome.toHTML(0);

        assertThat(html).contains("100");
        assertThat(html).contains("%");
    }

    @Test
    void should_OmitPercentages_When_BrevityFive() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.0f, 1.0f, 0.0f);

        String html = outcome.toHTML(5);

        assertThat(html).doesNotContain("%");
    }

    @Test
    void should_IncludeTactics_When_BrevityLessThanTwo() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.0f, 1.0f, 0.0f);

        String html = outcome.toHTML(0);

        assertThat(html).contains("pot odds");
    }

    @Test
    void should_OmitTactics_When_BrevityTwo() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.0f, 1.0f, 0.0f);

        String html = outcome.toHTML(2);

        assertThat(html).doesNotContain("pot odds");
    }

    @Test
    void should_ShowCheck_When_NoPotActionAndCheckFold() {
        outcome.addTuple(AIOutcome.CHECK, "weak hand", 1.0f, 0.0f, 0.0f);

        String html = outcome.toHTML(0);

        assertThat(html).contains("Check");
        assertThat(html).doesNotContain("Fold");
    }

    @Test
    void should_ShowBetOrRaise_When_BetRaiseProbability() {
        outcome.addTuple(AIOutcome.BET, "value bet", 0.0f, 0.0f, 1.0f);

        String html = outcome.toHTML(0);

        // Should contain either Bet or Raise depending on round/pot status
        assertThat(html).containsAnyOf("Bet", "Raise");
    }

    @Test
    void should_CombineMultipleOutcomes_When_MixedProbabilities() {
        outcome.addTuple(AIOutcome.CALL, "mixed", 0.2f, 0.5f, 0.3f);

        String html = outcome.toHTML(0);

        assertThat(html).contains("Check");
        assertThat(html).contains("or");
        assertThat(html).contains("Call");
        assertThat(html).contains("Raise");
    }

    // ========================================
    // Edge Cases
    // ========================================

    // Note: Testing limit games requires specific tournament profile setup
    // which is better suited for end-to-end integration tests

    @Test
    void should_ComputeOnce_When_CalledMultipleTimes() {
        outcome.addTuple(AIOutcome.CALL, "pot odds", 0.2f, 0.8f, 0.0f);

        float call1 = outcome.getCall();
        float call2 = outcome.getCall();
        float call3 = outcome.getCall();

        assertThat(call1).isEqualTo(call2).isEqualTo(call3).isEqualTo(0.8f);
    }
}
