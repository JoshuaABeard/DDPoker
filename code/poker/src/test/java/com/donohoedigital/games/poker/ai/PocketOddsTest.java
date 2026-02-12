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

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PocketOdds effective hand strength calculations with lookahead.
 */
class PocketOddsTest {
    @BeforeAll
    static void initializeConfig() {
        // Initialize ConfigManager for PokerUtils.nChooseK() and StylesConfig
        ConfigManager configMgr = new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        configMgr.loadGuiConfig(); // Required for StylesConfig
    }
    // ========================================
    // Validation Tests - Community Hand
    // ========================================

    @Test
    void should_ThrowError_When_CommunityIsNull() {
        Hand pocket = new Hand(SPADES_A, HEARTS_K);

        assertThatThrownBy(() -> PocketOdds.getInstance(null, pocket)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("null community");
    }

    @Test
    void should_ThrowError_When_CommunityIsEmpty() {
        Hand community = new Hand();
        Hand pocket = new Hand(SPADES_A, HEARTS_K);

        assertThatThrownBy(() -> PocketOdds.getInstance(community, pocket)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("before the flop");
    }

    @Test
    void should_ThrowError_When_CommunityHasTwoCards() {
        Hand community = new Hand(SPADES_A, HEARTS_K);
        Hand pocket = new Hand(DIAMONDS_Q, CLUBS_J);

        assertThatThrownBy(() -> PocketOdds.getInstance(community, pocket)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("before the flop");
    }

    @Test
    void should_ThrowError_When_CommunityIsRiver() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);
        Hand pocket = new Hand(HEARTS_9, DIAMONDS_8);

        assertThatThrownBy(() -> PocketOdds.getInstance(community, pocket)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("after the river");
    }

    // ========================================
    // Validation Tests - Pocket Hand
    // ========================================

    @Test
    void should_ThrowError_When_PocketIsNull() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);

        assertThatThrownBy(() -> PocketOdds.getInstance(community, null)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("null pocket");
    }

    @Test
    void should_ThrowError_When_PocketIsEmpty() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand();

        assertThatThrownBy(() -> PocketOdds.getInstance(community, pocket)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("empty pocket");
    }

    @Test
    void should_ThrowError_When_PocketHasOneCard() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand();
        pocket.addCard(CLUBS_J);

        assertThatThrownBy(() -> PocketOdds.getInstance(community, pocket)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("empty pocket");
    }

    // ========================================
    // Caching Tests
    // ========================================

    @Test
    void should_ReturnSameInstance_When_SameCommunityAndPocket() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        PocketOdds odds1 = PocketOdds.getInstance(community, pocket);
        PocketOdds odds2 = PocketOdds.getInstance(community, pocket);

        assertThat(odds1).isSameAs(odds2);
    }

    @Test
    void should_ReturnDifferentInstance_When_DifferentCommunity() {
        Hand community1 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand community2 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_J);
        Hand pocket = new Hand(CLUBS_T, SPADES_9);

        PocketOdds odds1 = PocketOdds.getInstance(community1, pocket);
        PocketOdds odds2 = PocketOdds.getInstance(community2, pocket);

        assertThat(odds1).isNotSameAs(odds2);
    }

    @Test
    void should_ReturnDifferentInstance_When_DifferentPocket() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket1 = new Hand(CLUBS_J, SPADES_T);
        Hand pocket2 = new Hand(HEARTS_9, DIAMONDS_8);

        PocketOdds odds1 = PocketOdds.getInstance(community, pocket1);
        PocketOdds odds2 = PocketOdds.getInstance(community, pocket2);

        // Should return different instances but from same cache (same community)
        assertThat(odds1).isNotSameAs(odds2);
    }

    @Test
    void should_ClearCache_When_CommunityChanges() {
        Hand community1 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        PocketOdds odds1 = PocketOdds.getInstance(community1, pocket);

        // Change community - should clear cache
        Hand community2 = new Hand(SPADES_2, HEARTS_3, DIAMONDS_4);
        PocketOdds odds2 = PocketOdds.getInstance(community2, pocket);

        // Go back to original community - should create new instance (cache was
        // cleared)
        PocketOdds odds3 = PocketOdds.getInstance(community1, pocket);

        assertThat(odds1).isNotSameAs(odds3);
    }

    // ========================================
    // Method Overload Consistency Tests
    // ========================================

    @Test
    void should_ReturnSameStrength_When_CalledWithDifferentMethods() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);

        Hand opponent = new Hand(HEARTS_9, DIAMONDS_8);
        float strengthHand = odds.getEffectiveHandStrength(opponent);
        float strengthCards = odds.getEffectiveHandStrength(HEARTS_9, DIAMONDS_8);
        float strengthIndices = odds.getEffectiveHandStrength(HEARTS_9.getIndex(), DIAMONDS_8.getIndex());

        assertThat(strengthHand).isEqualTo(strengthCards).isEqualTo(strengthIndices);
    }

    @Test
    void should_ReturnAverageStrength_When_NoOpponentSpecified() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);

        float average = odds.getEffectiveHandStrength();

        // Average should be between 0.0 and 1.0
        assertThat(average).isBetween(0.0f, 1.0f);
    }

    // ========================================
    // Flop Scenarios (3 community cards)
    // ========================================

    @Test
    void should_ReturnHighEHS_When_MadeNutsOnFlop() {
        // Flop: A♠ K♠ Q♠ (three to royal flush)
        Hand community = new Hand(SPADES_A, SPADES_K, SPADES_Q);
        // Pocket: J♠ T♠ (made royal flush)
        Hand pocket = new Hand(SPADES_J, SPADES_T);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Has nuts, very unlikely to be outdrawn
        assertThat(ehs).isGreaterThan(0.95f);
    }

    @Test
    void should_ReturnHighEHS_When_SetOnFlop() {
        // Flop: K♠ K♥ 7♦ (paired board)
        Hand community = new Hand(SPADES_K, HEARTS_K, DIAMONDS_7);
        // Pocket: K♦ K♣ (quad kings)
        Hand pocket = new Hand(DIAMONDS_K, CLUBS_K);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Quad kings is extremely strong (>95% but could lose to straight flush)
        assertThat(ehs).isGreaterThan(0.95f);
    }

    @Test
    void should_ReturnMediumEHS_When_FlushDrawOnFlop() {
        // Flop: A♠ 7♠ 4♠ (monotone spades)
        Hand community = new Hand(SPADES_A, SPADES_7, SPADES_4);
        // Pocket: K♠ Q♠ (nut flush draw + overcards)
        Hand pocket = new Hand(SPADES_K, SPADES_Q);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Strong draw with overcards - should have good equity
        assertThat(ehs).isGreaterThan(0.70f);
    }

    @Test
    void should_ReturnMediumEHS_When_OpenEndedStraightDrawOnFlop() {
        // Flop: K♠ Q♥ J♦
        Hand community = new Hand(SPADES_K, HEARTS_Q, DIAMONDS_J);
        // Pocket: T♠ 9♠ (open-ended straight draw + gutshot to higher straight)
        Hand pocket = new Hand(SPADES_T, SPADES_9);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Made straight, very strong
        assertThat(ehs).isGreaterThan(0.85f);
    }

    @Test
    void should_ReturnLowEHS_When_DrawingDeadOnFlop() {
        // Flop: A♠ A♥ A♦ (trip aces on board)
        Hand community = new Hand(SPADES_A, HEARTS_A, DIAMONDS_A);
        // Pocket: 2♠ 3♠ (essentially drawing dead - needs runner-runner for straight
        // flush)
        Hand pocket = new Hand(SPADES_2, SPADES_3);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Very low equity - essentially drawing dead
        assertThat(ehs).isLessThan(0.15f);
    }

    @Test
    void should_ReturnMediumEHS_When_BottomPairOnFlop() {
        // Flop: A♠ K♥ 7♦
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_7);
        // Pocket: 7♠ 6♠ (bottom pair, weak kicker, backdoor flush draw)
        Hand pocket = new Hand(SPADES_7, SPADES_6);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Bottom pair with backdoor potential - medium equity
        assertThat(ehs).isBetween(0.55f, 0.75f);
    }

    // ========================================
    // Turn Scenarios (4 community cards)
    // ========================================

    @Test
    void should_HandleTurnScenario_When_FourCommunityCards() {
        // Turn: A♠ K♥ Q♦ J♠ (four to straight)
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, SPADES_J);
        // Pocket: T♠ 9♠ (made straight)
        Hand pocket = new Hand(SPADES_T, SPADES_9);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Made straight on turn, strong hand
        assertThat(ehs).isGreaterThan(0.85f);
    }

    @Test
    void should_ReturnHighEHS_When_FlushOnTurn() {
        // Turn: A♠ K♠ 7♠ 4♠ (four spades on board)
        Hand community = new Hand(SPADES_A, SPADES_K, SPADES_7, SPADES_4);
        // Pocket: Q♠ J♠ (nut flush)
        Hand pocket = new Hand(SPADES_Q, SPADES_J);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Nut flush on turn - very strong, can only lose to quads or straight flush
        assertThat(ehs).isGreaterThan(0.90f);
    }

    @Test
    void should_ReturnMediumEHS_When_DrawOnTurn() {
        // Turn: A♠ K♥ Q♦ 7♣ (rainbow board)
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_7);
        // Pocket: J♠ 9♠ (gutshot + backdoor flush draw)
        Hand pocket = new Hand(SPADES_J, SPADES_9);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Gutshot + backdoor flush gives moderate equity
        assertThat(ehs).isBetween(0.30f, 0.55f);
    }

    // ========================================
    // Range Validation Tests
    // ========================================

    @Test
    void should_ReturnValidRange_When_AnyScenario() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);

        // Test average EHS
        float average = odds.getEffectiveHandStrength();
        assertThat(average).isBetween(0.0f, 1.0f);

        // Test specific opponent EHS
        Hand opponent = new Hand(HEARTS_9, DIAMONDS_8);
        float specific = odds.getEffectiveHandStrength(opponent);
        assertThat(specific).isBetween(0.0f, 1.0f);
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    void should_HandlePairedBoard_When_CalculatingOdds() {
        // Flop: K♠ K♥ 7♦ (paired board)
        Hand community = new Hand(SPADES_K, HEARTS_K, DIAMONDS_7);
        // Pocket: A♠ A♥ (overpair to board pair)
        Hand pocket = new Hand(SPADES_A, HEARTS_A);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Overpair on paired board is very strong
        assertThat(ehs).isGreaterThan(0.80f);
    }

    @Test
    void should_HandleMonotoneBoard_When_CalculatingOdds() {
        // Flop: A♠ 7♠ 2♠ (all spades)
        Hand community = new Hand(SPADES_A, SPADES_7, SPADES_2);
        // Pocket: K♦ Q♦ (no spades - drawing thin, has overcards)
        Hand pocket = new Hand(DIAMONDS_K, DIAMONDS_Q);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // No flush draw on monotone board, but has overcards and backdoor possibilities
        assertThat(ehs).isLessThan(0.50f);
    }

    @Test
    void should_ProvideConsistentResults_When_CalledMultipleTimes() {
        Hand community = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        PocketOdds odds = PocketOdds.getInstance(community, pocket);

        float ehs1 = odds.getEffectiveHandStrength();
        float ehs2 = odds.getEffectiveHandStrength();
        float ehs3 = odds.getEffectiveHandStrength();

        assertThat(ehs1).isEqualTo(ehs2).isEqualTo(ehs3);
    }
}
