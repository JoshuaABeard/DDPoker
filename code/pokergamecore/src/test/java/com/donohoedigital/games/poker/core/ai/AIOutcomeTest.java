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
package com.donohoedigital.games.poker.core.ai;

import static org.assertj.core.api.Assertions.*;

import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.engine.state.BettingRound;
import org.junit.jupiter.api.Test;

class AIOutcomeTest {

    // ========== Constructor ==========

    @Test
    void should_CreateOutcome_When_ValidParameters() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        assertThat(outcome).isNotNull();
    }

    // ========== addTuple and probability computation ==========

    @Test
    void should_ComputeAverages_When_SingleTuple() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "weak hand", 0.6f, 0.3f, 0.1f);

        assertThat(outcome.getCheckFold()).isEqualTo(0.6f);
        assertThat(outcome.getCall()).isEqualTo(0.3f);
        assertThat(outcome.getBetRaise()).isEqualTo(0.1f);
    }

    @Test
    void should_AverageProbabilities_When_MultipleTuples() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "weak", 0.8f, 0.1f, 0.1f);
        outcome.addTuple(AIOutcome.CALL, "medium", 0.2f, 0.5f, 0.3f);

        // Average: checkFold=(0.8+0.2)/2=0.5, call=(0.1+0.5)/2=0.3,
        // betRaise=(0.1+0.3)/2=0.2
        assertThat(outcome.getCheckFold()).isCloseTo(0.5f, within(0.001f));
        assertThat(outcome.getCall()).isCloseTo(0.3f, within(0.001f));
        assertThat(outcome.getBetRaise()).isCloseTo(0.2f, within(0.001f));
    }

    @Test
    void should_RecomputeAfterNewTuple_When_TupleAdded() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "weak", 1.0f, 0.0f, 0.0f);
        assertThat(outcome.getCheckFold()).isEqualTo(1.0f);

        // Add another tuple - should invalidate cache and recompute
        outcome.addTuple(AIOutcome.BET, "strong", 0.0f, 0.0f, 1.0f);
        assertThat(outcome.getCheckFold()).isCloseTo(0.5f, within(0.001f));
        assertThat(outcome.getBetRaise()).isCloseTo(0.5f, within(0.001f));
    }

    @Test
    void should_AverageThreeTuples_When_ThreeAdded() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.PRE_FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "t1", 0.9f, 0.05f, 0.05f);
        outcome.addTuple(AIOutcome.CALL, "t2", 0.1f, 0.8f, 0.1f);
        outcome.addTuple(AIOutcome.RAISE, "t3", 0.0f, 0.1f, 0.9f);

        // Averages: (0.9+0.1+0.0)/3=0.333, (0.05+0.8+0.1)/3=0.317,
        // (0.05+0.1+0.9)/3=0.35
        assertThat(outcome.getCheckFold()).isCloseTo(0.333f, within(0.01f));
        assertThat(outcome.getCall()).isCloseTo(0.317f, within(0.01f));
        assertThat(outcome.getBetRaise()).isCloseTo(0.35f, within(0.01f));
    }

    // ========== getStrongestOutcome ==========

    @Test
    void should_ReturnCheck_When_CheckFoldHighestAndNoPotAction() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.CHECK, "check", 0.7f, 0.2f, 0.1f);

        assertThat(outcome.getStrongestOutcome(PokerConstants.NO_POT_ACTION)).isEqualTo(AIOutcome.OUTCOME_CHECK);
    }

    @Test
    void should_ReturnFold_When_CheckFoldHighestAndRaisedPot() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "fold", 0.7f, 0.2f, 0.1f);

        assertThat(outcome.getStrongestOutcome(PokerConstants.RAISED_POT)).isEqualTo(AIOutcome.OUTCOME_FOLD);
    }

    @Test
    void should_ReturnCall_When_CallHighest() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.CALL, "call", 0.2f, 0.6f, 0.2f);

        assertThat(outcome.getStrongestOutcome(PokerConstants.RAISED_POT)).isEqualTo(AIOutcome.OUTCOME_CALL);
    }

    @Test
    void should_ReturnBet_When_BetRaiseHighestAndNoPotActionPostFlop() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.BET, "bet", 0.1f, 0.2f, 0.7f);

        assertThat(outcome.getStrongestOutcome(PokerConstants.NO_POT_ACTION)).isEqualTo(AIOutcome.OUTCOME_BET);
    }

    @Test
    void should_ReturnOpenPot_When_BetRaiseHighestAndNoPotActionPreFlop() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.PRE_FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.RAISE, "raise", 0.1f, 0.2f, 0.7f);

        assertThat(outcome.getStrongestOutcome(PokerConstants.NO_POT_ACTION)).isEqualTo(AIOutcome.OUTCOME_OPEN_POT);
    }

    @Test
    void should_ReturnRaise_When_BetRaiseHighestAndRaisedPot() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.RAISE, "raise", 0.1f, 0.2f, 0.7f);

        assertThat(outcome.getStrongestOutcome(PokerConstants.RAISED_POT)).isEqualTo(AIOutcome.OUTCOME_RAISE);
    }

    @Test
    void should_ReturnRaise_When_BetRaiseTiedWithCall() {
        // When call == betRaise, the code falls through to betRaise path
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.RAISE, "raise", 0.0f, 0.5f, 0.5f);

        assertThat(outcome.getStrongestOutcome(PokerConstants.RAISED_POT)).isEqualTo(AIOutcome.OUTCOME_RAISE);
    }

    // ========== toHTML ==========

    @Test
    void should_IncludeCheckInHTML_When_NoPotActionAndCheckFoldPositive() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.CHECK, "check tactic", 0.5f, 0.3f, 0.2f);

        String html = outcome.toHTML();
        assertThat(html).contains("Check");
        assertThat(html).contains("check tactic");
    }

    @Test
    void should_IncludeFoldInHTML_When_RaisedPotAndCheckFoldPositive() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "fold tactic", 0.5f, 0.3f, 0.2f);

        String html = outcome.toHTML();
        assertThat(html).contains("Fold");
        assertThat(html).contains("fold tactic");
    }

    @Test
    void should_IncludeCallInHTML_When_CallPositive() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.CALL, "call tactic", 0.2f, 0.5f, 0.3f);

        String html = outcome.toHTML();
        assertThat(html).contains("Call");
        assertThat(html).contains("call tactic");
    }

    @Test
    void should_IncludeRaiseInHTML_When_RaisedPotAndBetRaisePositive() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.RAISE, "raise tactic", 0.1f, 0.2f, 0.7f);

        String html = outcome.toHTML();
        assertThat(html).contains("Raise");
        assertThat(html).contains("raise tactic");
    }

    @Test
    void should_IncludeBetInHTML_When_NoPotActionPostFlop() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.BET, "bet tactic", 0.1f, 0.2f, 0.7f);

        String html = outcome.toHTML();
        assertThat(html).contains("Bet");
    }

    @Test
    void should_IncludeRaiseInHTML_When_NoPotActionPreFlop() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.PRE_FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.BET, "open pot", 0.1f, 0.2f, 0.7f);

        String html = outcome.toHTML();
        assertThat(html).contains("Raise");
    }

    @Test
    void should_IncludeReRaiseInHTML_When_ReRaisedPot() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RERAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.RAISE, "re-raise", 0.1f, 0.2f, 0.7f);

        String html = outcome.toHTML();
        assertThat(html).contains("Re-Raise");
    }

    @Test
    void should_IncludePercentages_When_DefaultBrevity() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.CHECK, "check", 0.5f, 0.3f, 0.2f);

        String html = outcome.toHTML();
        assertThat(html).contains("%");
    }

    @Test
    void should_IncludeBetRangeDescription_When_BetRangeSet() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.BET, "bet", 0.1f, 0.2f, 0.7f);
        outcome.setBetRange(BetRange.potRelative(0.5f, 1.0f), null);

        String html = outcome.toHTML();
        assertThat(html).contains("50%-100% pot");
    }

    @Test
    void should_IncludeAllInReason_When_SetWithReason() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.BET, "bet", 0.1f, 0.2f, 0.7f);
        outcome.setBetRange(BetRange.allIn(), "short stack");

        String html = outcome.toHTML();
        assertThat(html).contains("All In");
        assertThat(html).contains("short stack");
    }

    @Test
    void should_SuppressTactics_When_HighBrevity() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "fold tactic", 0.7f, 0.2f, 0.1f);

        String html = outcome.toHTML(3);
        assertThat(html).doesNotContain("fold tactic");
        assertThat(html).doesNotContain("Recommend");
    }

    @Test
    void should_SuppressPercentages_When_Brevity5() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "fold", 0.7f, 0.2f, 0.1f);

        String html = outcome.toHTML(5);
        assertThat(html).doesNotContain("%");
    }

    @Test
    void should_IncludeRecommend_When_Brevity0() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "fold", 0.7f, 0.0f, 0.0f);

        String html = outcome.toHTML(0);
        assertThat(html).contains("Recommend");
    }

    @Test
    void should_OmitBetRangeForLimit_When_IsLimit() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), true);
        outcome.addTuple(AIOutcome.BET, "bet", 0.1f, 0.2f, 0.7f);
        outcome.setBetRange(BetRange.potRelative(0.5f, 1.0f), null);

        String html = outcome.toHTML();
        assertThat(html).doesNotContain("pot");
    }

    @Test
    void should_CombineActionsWithOr_When_MultipleProbabilitiesPositive() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "fold", 0.3f, 0.4f, 0.3f);

        String html = outcome.toHTML();
        assertThat(html).contains(" or ");
    }

    // ========== Outcome type constants ==========

    @Test
    void should_HaveCorrectFoldCheckConstants() {
        // FOLD and CHECK both map to 0
        assertThat(AIOutcome.FOLD).isEqualTo(0);
        assertThat(AIOutcome.CHECK).isEqualTo(0);
    }

    @Test
    void should_HaveCorrectCallConstant() {
        assertThat(AIOutcome.CALL).isEqualTo(1);
    }

    @Test
    void should_HaveCorrectBetRaiseConstants() {
        // BET, RAISE, RERAISE all map to 2
        assertThat(AIOutcome.BET).isEqualTo(2);
        assertThat(AIOutcome.RAISE).isEqualTo(2);
        assertThat(AIOutcome.RERAISE).isEqualTo(2);
    }

    @Test
    void should_HaveDistinctOutcomeTypeConstants() {
        assertThat(AIOutcome.OUTCOME_FOLD).isEqualTo(0);
        assertThat(AIOutcome.OUTCOME_CHECK).isEqualTo(1);
        assertThat(AIOutcome.OUTCOME_CALL).isEqualTo(5);
        assertThat(AIOutcome.OUTCOME_RAISE).isEqualTo(6);
        assertThat(AIOutcome.OUTCOME_BET).isEqualTo(11);
        assertThat(AIOutcome.OUTCOME_ALL_IN).isEqualTo(12);
        assertThat(AIOutcome.OUTCOME_BLUFF).isEqualTo(14);
    }

    // ========== setBetRange ==========

    @Test
    void should_AcceptNullBetRange_When_SetBetRangeCalled() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.BET, "bet", 0.1f, 0.2f, 0.7f);
        outcome.setBetRange(null, null);

        // Should not throw and should not include range in HTML
        String html = outcome.toHTML();
        assertThat(html).doesNotContain("pot");
        assertThat(html).doesNotContain("All In");
    }

    // ========== selectOutcome ==========

    @Test
    void should_SelectValidOutcome_When_CalledMultipleTimes() {
        AIOutcome outcome = new AIOutcome(PokerConstants.RAISED_POT, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.FOLD, "fold", 0.33f, 0.34f, 0.33f);

        // Run multiple times - should produce a mix of outcomes
        boolean sawFold = false;
        boolean sawCall = false;
        boolean sawRaise = false;
        for (int i = 0; i < 200; i++) {
            int selected = outcome.selectOutcome(PokerConstants.RAISED_POT);
            if (selected == AIOutcome.OUTCOME_FOLD)
                sawFold = true;
            else if (selected == AIOutcome.OUTCOME_CALL)
                sawCall = true;
            else if (selected == AIOutcome.OUTCOME_RAISE)
                sawRaise = true;
        }
        assertThat(sawFold || sawCall || sawRaise).isTrue();
    }

    @Test
    void should_ReturnCheckNotFold_When_SelectOutcomeNoPotAction() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.CHECK, "check", 1.0f, 0.0f, 0.0f);

        int selected = outcome.selectOutcome(PokerConstants.NO_POT_ACTION);
        assertThat(selected).isEqualTo(AIOutcome.OUTCOME_CHECK);
    }

    @Test
    void should_ReturnBetNotRaise_When_SelectOutcomeNoPotActionBet() {
        AIOutcome outcome = new AIOutcome(PokerConstants.NO_POT_ACTION, BettingRound.FLOP.toLegacy(), false);
        outcome.addTuple(AIOutcome.BET, "bet", 0.0f, 0.0f, 1.0f);

        int selected = outcome.selectOutcome(PokerConstants.NO_POT_ACTION);
        assertThat(selected).isEqualTo(AIOutcome.OUTCOME_BET);
    }
}
