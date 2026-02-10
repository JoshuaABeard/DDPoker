/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.integration.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for RuleEngine AI decision-making.
 * Tests RuleEngine's outcome management and eligibility system.
 *
 * Note: Full AI execution tests (with V2Player) require complete game scenarios
 * and are better suited for end-to-end testing.
 */
@Tag("slow")
@Tag("integration")
class RuleEngineIntegrationTest extends IntegrationTestBase
{
    private RuleEngine engine;

    @BeforeEach
    void setUp()
    {
        engine = new RuleEngine();
    }

    // ========================================
    // Basic Functionality Tests
    // ========================================

    @Test
    void should_CreateEngine_When_Constructed()
    {
        assertThat(engine).isNotNull();
    }

    // ========================================
    // Outcome Constants Tests
    // ========================================

    @Test
    void should_HaveValidOutcomeConstants_When_Defined()
    {
        assertThat(RuleEngine.OUTCOME_NONE).isEqualTo(-1);
        assertThat(RuleEngine.OUTCOME_FOLD).isEqualTo(0);
        assertThat(RuleEngine.OUTCOME_CHECK).isEqualTo(1);
        assertThat(RuleEngine.OUTCOME_CALL).isEqualTo(5);
        assertThat(RuleEngine.OUTCOME_RAISE).isEqualTo(6);
        assertThat(RuleEngine.OUTCOME_BET).isEqualTo(11);
    }

    @Test
    void should_HaveSpecializedOutcomes_When_Defined()
    {
        assertThat(RuleEngine.OUTCOME_LIMP).isEqualTo(2);
        assertThat(RuleEngine.OUTCOME_STEAL).isEqualTo(3);
        assertThat(RuleEngine.OUTCOME_SEMI_BLUFF).isEqualTo(7);
        assertThat(RuleEngine.OUTCOME_TRAP).isEqualTo(8);
        assertThat(RuleEngine.OUTCOME_SLOW_PLAY).isEqualTo(9);
        assertThat(RuleEngine.OUTCOME_CHECK_RAISE).isEqualTo(10);
        assertThat(RuleEngine.OUTCOME_BLUFF).isEqualTo(14);
    }

    // ========================================
    // Factor Constants Tests
    // ========================================

    @Test
    void should_HaveHandStrengthFactors_When_Defined()
    {
        assertThat(RuleEngine.FACTOR_RAW_HAND_STRENGTH).isEqualTo(12);
        assertThat(RuleEngine.FACTOR_BIASED_HAND_STRENGTH).isEqualTo(13);
        assertThat(RuleEngine.FACTOR_HAND_POTENTIAL).isEqualTo(14);
    }

    @Test
    void should_HavePositionFactors_When_Defined()
    {
        assertThat(RuleEngine.FACTOR_POSITION).isEqualTo(8);
        assertThat(RuleEngine.FACTOR_PRE_FLOP_POSITION).isEqualTo(30);
        assertThat(RuleEngine.FACTOR_RAISER_POSITION).isEqualTo(33);
    }

    @Test
    void should_HaveOddsFactors_When_Defined()
    {
        assertThat(RuleEngine.FACTOR_POT_ODDS).isEqualTo(11);
        assertThat(RuleEngine.FACTOR_IMPLIED_ODDS).isEqualTo(32);
    }

    @Test
    void should_HaveDrawFactors_When_Defined()
    {
        assertThat(RuleEngine.FACTOR_STRAIGHT_DRAW).isEqualTo(22);
        assertThat(RuleEngine.FACTOR_FLUSH_DRAW).isEqualTo(23);
    }

    @Test
    void should_HaveOpponentFactors_When_Defined()
    {
        assertThat(RuleEngine.FACTOR_AGGRESSION).isEqualTo(18);
        assertThat(RuleEngine.FACTOR_OPPONENT_BET_FREQUENCY).isEqualTo(41);
        assertThat(RuleEngine.FACTOR_OPPONENT_RAISE_FREQUENCY).isEqualTo(42);
        assertThat(RuleEngine.FACTOR_OPPONENT_OVERBET_FREQUENCY).isEqualTo(43);
        assertThat(RuleEngine.FACTOR_OPPONENT_BET_FOLD_FREQUENCY).isEqualTo(44);
    }

    @Test
    void should_HavePsychologicalFactors_When_Defined()
    {
        assertThat(RuleEngine.FACTOR_BOREDOM).isEqualTo(35);
        assertThat(RuleEngine.FACTOR_STEAM).isEqualTo(36);
    }

    // ========================================
    // Outcome Eligibility Tests
    // ========================================

    @Test
    void should_SetEligible_When_SetEligibleCalled()
    {
        engine.setEligible(RuleEngine.OUTCOME_CALL, true);

        assertThat(engine.isEligible(RuleEngine.OUTCOME_CALL)).isTrue();
    }

    @Test
    void should_SetIneligible_When_SetEligibleCalledWithFalse()
    {
        engine.setEligible(RuleEngine.OUTCOME_CALL, false);

        assertThat(engine.isEligible(RuleEngine.OUTCOME_CALL)).isFalse();
    }

    @Test
    void should_SetMultipleEligible_When_CalledMultipleTimes()
    {
        engine.setEligible(RuleEngine.OUTCOME_CALL, true);
        engine.setEligible(RuleEngine.OUTCOME_RAISE, true);
        engine.setEligible(RuleEngine.OUTCOME_FOLD, true);

        assertThat(engine.isEligible(RuleEngine.OUTCOME_CALL)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_RAISE)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_FOLD)).isTrue();
    }

    @Test
    void should_ToggleEligibility_When_SetMultipleTimes()
    {
        engine.setEligible(RuleEngine.OUTCOME_CALL, true);
        assertThat(engine.isEligible(RuleEngine.OUTCOME_CALL)).isTrue();

        engine.setEligible(RuleEngine.OUTCOME_CALL, false);
        assertThat(engine.isEligible(RuleEngine.OUTCOME_CALL)).isFalse();

        engine.setEligible(RuleEngine.OUTCOME_CALL, true);
        assertThat(engine.isEligible(RuleEngine.OUTCOME_CALL)).isTrue();
    }

    @Test
    void should_ManageIndependentOutcomes_When_SettingEligibility()
    {
        engine.setEligible(RuleEngine.OUTCOME_CALL, true);
        engine.setEligible(RuleEngine.OUTCOME_RAISE, false);

        assertThat(engine.isEligible(RuleEngine.OUTCOME_CALL)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_RAISE)).isFalse();
    }

    @Test
    void should_GetEligibleOutcomes_When_MultipleSet()
    {
        engine.setEligible(RuleEngine.OUTCOME_CALL, true);
        engine.setEligible(RuleEngine.OUTCOME_RAISE, true);
        engine.setEligible(RuleEngine.OUTCOME_FOLD, true);

        assertThat(engine.getEligibleOutcomeNames()).hasSizeGreaterThanOrEqualTo(3);
    }

    // ========================================
    // Outcome Coverage Tests
    // ========================================

    @Test
    void should_SupportAllBasicOutcomes_When_CheckingEligibility()
    {
        // Test that all basic outcomes can be set
        engine.setEligible(RuleEngine.OUTCOME_FOLD, true);
        engine.setEligible(RuleEngine.OUTCOME_CHECK, true);
        engine.setEligible(RuleEngine.OUTCOME_CALL, true);
        engine.setEligible(RuleEngine.OUTCOME_BET, true);
        engine.setEligible(RuleEngine.OUTCOME_RAISE, true);

        assertThat(engine.isEligible(RuleEngine.OUTCOME_FOLD)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_CHECK)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_CALL)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_BET)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_RAISE)).isTrue();
    }

    @Test
    void should_SupportAdvancedOutcomes_When_CheckingEligibility()
    {
        // Test advanced tactical outcomes
        engine.setEligible(RuleEngine.OUTCOME_LIMP, true);
        engine.setEligible(RuleEngine.OUTCOME_STEAL, true);
        engine.setEligible(RuleEngine.OUTCOME_SEMI_BLUFF, true);
        engine.setEligible(RuleEngine.OUTCOME_CHECK_RAISE, true);
        engine.setEligible(RuleEngine.OUTCOME_BLUFF, true);

        assertThat(engine.isEligible(RuleEngine.OUTCOME_LIMP)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_STEAL)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_SEMI_BLUFF)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_CHECK_RAISE)).isTrue();
        assertThat(engine.isEligible(RuleEngine.OUTCOME_BLUFF)).isTrue();
    }

    // ========================================
    // Curve Constants Tests
    // ========================================

    @Test
    void should_HaveCurveConstants_When_Defined()
    {
        assertThat(RuleEngine.CURVE_LINEAR).isEqualTo(1);
        assertThat(RuleEngine.CURVE_SQUARE).isEqualTo(2);
        assertThat(RuleEngine.CURVE_CUBE).isEqualTo(3);
    }

    // ========================================
    // Edge Cases
    // ========================================

    // Note: OUTCOME_NONE (-1) is a special sentinel value and not a valid outcome
    // for eligibility checking

    @Test
    void should_HandleAllInOutcome_When_Checking()
    {
        engine.setEligible(RuleEngine.OUTCOME_ALL_IN, true);

        assertThat(engine.isEligible(RuleEngine.OUTCOME_ALL_IN)).isTrue();
    }
}
