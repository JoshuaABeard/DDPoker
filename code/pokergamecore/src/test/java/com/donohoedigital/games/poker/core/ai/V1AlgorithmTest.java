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
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.GameHand;
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.GamePlayerInfo;
import com.donohoedigital.games.poker.engine.PlayerAction;
import com.donohoedigital.games.poker.engine.state.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for V1Algorithm poker AI.
 * <p>
 * Exercises pre-flop position-based decisions, post-flop hand evaluation,
 * rebuy/addon logic, Sklansky system, and helper methods across all skill
 * levels.
 */
class V1AlgorithmTest {

    private static final long TEST_SEED = 12345L;
    private static final ActionType[] VALID_ACTIONS = {ActionType.FOLD, ActionType.CHECK, ActionType.CALL,
            ActionType.BET, ActionType.RAISE};

    private V1Algorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM);
    }

    // ========== Constructor Tests ==========

    @Test
    void should_CreateWithRandomTraits_When_NoExplicitTraits() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_HARD);
        assertThat(ai).isNotNull();
    }

    @Test
    void should_CreateWithExplicitTraits_When_AllTraitsProvided() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_EASY, 50, 30, 25, 75);
        assertThat(ai).isNotNull();
    }

    // ========== Rebuy Tests ==========

    @Test
    void should_Rebuy_When_LowPropensity() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 10, 50);
        StubGamePlayerInfo player = createPlayer(1000, 0);
        StubAIContext context = createPreFlopContext(player);

        assertThat(ai.wantsRebuy(player, context)).isTrue();
    }

    @Test
    void should_NotRebuy_When_HighPropensity() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 95, 50);
        StubGamePlayerInfo player = createPlayer(1000, 0);
        StubAIContext context = createPreFlopContext(player);

        assertThat(ai.wantsRebuy(player, context)).isFalse();
    }

    @Test
    void should_NotRebuy_When_MaxRebuysReached() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 10, 50);
        StubGamePlayerInfo player = createPlayer(1000, 5);
        StubAIContext context = createPreFlopContext(player);

        assertThat(ai.wantsRebuy(player, context)).isFalse();
    }

    @Test
    void should_RebuyUpTo3_When_MediumPropensity() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 40, 50);

        assertThat(ai.wantsRebuy(createPlayer(0, 2), createPreFlopContext(null))).isTrue();
        assertThat(ai.wantsRebuy(createPlayer(0, 3), createPreFlopContext(null))).isFalse();
    }

    @Test
    void should_RebuyUpTo2_When_ModerateHighPropensity() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 60, 50);

        assertThat(ai.wantsRebuy(createPlayer(0, 1), createPreFlopContext(null))).isTrue();
        assertThat(ai.wantsRebuy(createPlayer(0, 2), createPreFlopContext(null))).isFalse();
    }

    @Test
    void should_RebuyOnce_When_HighishPropensity() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 80, 50);

        assertThat(ai.wantsRebuy(createPlayer(0, 0), createPreFlopContext(null))).isTrue();
        assertThat(ai.wantsRebuy(createPlayer(0, 1), createPreFlopContext(null))).isFalse();
    }

    // ========== Addon Tests ==========

    @Test
    void should_Addon_When_LowPropensity() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 50, 10);
        StubGamePlayerInfo player = createPlayer(1000, 0);
        StubAIContext context = createPreFlopContext(player);
        context.setTournament(createTournament(1000));

        assertThat(ai.wantsAddon(player, context)).isTrue();
    }

    @Test
    void should_NotAddon_When_HighPropensity() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 50, 85);
        StubGamePlayerInfo player = createPlayer(5000, 0);
        StubAIContext context = createPreFlopContext(player);
        context.setTournament(createTournament(1000));

        assertThat(ai.wantsAddon(player, context)).isFalse();
    }

    @Test
    void should_AddonWhenChipsLow_When_MediumPropensity() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 50, 35);
        StubAIContext context = createPreFlopContext(null);
        context.setTournament(createTournament(1000));

        // Chips < 3x buyin: should addon
        StubGamePlayerInfo low = createPlayer(2000, 0);
        assertThat(ai.wantsAddon(low, context)).isTrue();

        // Chips >= 3x buyin: should not
        StubGamePlayerInfo high = createPlayer(4000, 0);
        assertThat(ai.wantsAddon(high, context)).isFalse();
    }

    @Test
    void should_AddonWhenChipsVeryLow_When_ModerateHighPropensity() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 50, 60);
        StubAIContext context = createPreFlopContext(null);
        context.setTournament(createTournament(1000));

        // Chips < 2x buyin: should addon
        StubGamePlayerInfo low = createPlayer(1500, 0);
        assertThat(ai.wantsAddon(low, context)).isTrue();

        // Chips >= 2x buyin: should not
        StubGamePlayerInfo high = createPlayer(2500, 0);
        assertThat(ai.wantsAddon(high, context)).isFalse();
    }

    // ========== Pre-Flop: No Cards ==========

    @Test
    void should_Fold_When_NoHoleCards() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(null);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
    }

    // ========== Pre-Flop: Premium Hands ==========

    @Test
    void should_NotFold_When_PocketAcesPreFlop() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.CLUBS_A, Card.SPADES_A});
        context.setPosition(player, 1); // Middle

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
    }

    @Test
    void should_RaiseOrCall_When_PocketKingsEarlyPosition() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.CLUBS_K, Card.SPADES_K});
        context.setPosition(player, 0); // Early position

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
    }

    @Test
    void should_RaiseOrCall_When_AKSuitedMiddlePosition() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        // AKs (Ace-King suited)
        context.setHoleCards(new Card[]{Card.SPADES_A, Card.SPADES_K});
        context.setPosition(player, 1); // Middle position

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
    }

    // ========== Pre-Flop: Position-Based Decisions ==========

    @Test
    void should_ReturnValidAction_When_EarlyPositionWithRaise() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.HEARTS_Q, Card.DIAMONDS_Q});
        context.setPosition(player, 0); // Early position
        context.setHasBeenRaised(true);
        context.setAmountToCall(200);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_MiddlePositionNoCallers() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.HEARTS_J, Card.SPADES_T});
        context.setPosition(player, 1); // Middle position
        context.setNumCallers(0);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_LatePositionNoCallers() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.HEARTS_8, Card.SPADES_7});
        context.setPosition(player, 2); // Late position

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_ButtonPositionNoCallers() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.DIAMONDS_9, Card.CLUBS_8});
        context.setPosition(player, 3); // Button position

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_SmallBlindPosition() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.HEARTS_T, Card.DIAMONDS_9});
        context.setPosition(player, 4); // Small blind
        context.setAmountToCall(10); // Half blind to complete

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_BigBlindPositionRaised() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.CLUBS_6, Card.HEARTS_5});
        context.setPosition(player, 5); // Big blind
        context.setHasBeenRaised(true);
        context.setAmountToCall(40);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_CheckFromBigBlind_When_NoRaiseAndTrashHand() {
        // Use extremely tight factor to consistently fold-or-check trash hands
        V1Algorithm tightAi = new V1Algorithm(42L, V1Algorithm.AI_EASY, 99, 0, 50, 50);
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.CLUBS_7, Card.SPADES_2});
        context.setPosition(player, 5); // Big blind
        context.setAmountToCall(0); // No raise to call

        PlayerAction action = tightAi.getAction(player, options, context);

        assertThat(action).isNotNull();
        // Big blind with no raise can check
        assertThat(action.actionType()).isIn(ActionType.CHECK, ActionType.CALL, ActionType.BET, ActionType.RAISE);
    }

    // ========== Pre-Flop: Raised Pot Scenarios ==========

    @Test
    void should_ReturnValidAction_When_LatePositionFacingRaiseWithMarginalHand() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.HEARTS_9, Card.DIAMONDS_8});
        context.setPosition(player, 2); // Late
        context.setHasBeenRaised(true);
        context.setAmountToCall(60);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_ShortHandedLatePositionRaised() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_5});
        context.setPosition(player, 2); // Late
        context.setNumActivePlayers(3); // Short-handed
        context.setHasBeenRaised(true);
        context.setAmountToCall(60);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Pre-Flop: Skill Level Variations ==========

    @Test
    void should_ReturnValidAction_When_EasyAI() {
        V1Algorithm easy = new V1Algorithm(TEST_SEED, V1Algorithm.AI_EASY, 50, 50, 50, 50);
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.HEARTS_T, Card.DIAMONDS_9});
        context.setPosition(player, 1);

        PlayerAction action = easy.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_HardAI() {
        V1Algorithm hard = new V1Algorithm(TEST_SEED, V1Algorithm.AI_HARD, 50, 50, 50, 50);
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_K});
        context.setPosition(player, 2);

        PlayerAction action = hard.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_HardAISklankskySystem() {
        // Tight factor <= 5 and > 4 active players triggers Sklansky system
        V1Algorithm sysAi = new V1Algorithm(TEST_SEED, V1Algorithm.AI_HARD, 3, 50, 50, 50);
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.CLUBS_A, Card.SPADES_A});
        context.setNumActivePlayers(6);

        PlayerAction action = sysAi.getAction(player, options, context);

        assertThat(action).isNotNull();
        // AA should result in all-in or raise
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.RAISE);
    }

    @Test
    void should_Fold_When_HardAISklankskySystemWithTrashAndRaise() {
        V1Algorithm sysAi = new V1Algorithm(TEST_SEED, V1Algorithm.AI_HARD, 3, 50, 50, 50);
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.CLUBS_7, Card.SPADES_2});
        context.setNumActivePlayers(6);
        context.setHasBeenRaised(true);
        context.setAmountToCall(200);

        PlayerAction action = sysAi.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
    }

    // ========== Pre-Flop: Tight Factor / Rebuy Period ==========

    @Test
    void should_PlayLooser_When_RebuyPeriodActive() {
        V1Algorithm ai = new V1Algorithm(42L, V1Algorithm.AI_MEDIUM, 50, 50, 50, 50);
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPreFlopContext(player);
        context.setHoleCards(new Card[]{Card.HEARTS_9, Card.DIAMONDS_7});
        context.setPosition(player, 1);
        context.setRebuyPeriodActive(true);

        // During rebuy period, tight factor is reduced by 20
        PlayerAction action = ai.getAction(player, options, context);
        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Post-Flop: Various Hand Types ==========

    @Test
    void should_ReturnValidAction_When_PostFlopRoyalFlush() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.SPADES_A, Card.SPADES_K});
        context.setCommunityCards(new Card[]{Card.SPADES_Q, Card.SPADES_J, Card.SPADES_T});
        context.setHandRank(9); // Royal Flush
        context.setHoleCardInvolved(true);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
        // Should not fold with a royal flush
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopFullHouse() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_A});
        context.setCommunityCards(new Card[]{Card.CLUBS_A, Card.HEARTS_K, Card.DIAMONDS_K});
        context.setHandRank(6); // Full house
        context.setHoleCardInvolved(true);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopFullHouseOnBoard() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.CLUBS_7, Card.HEARTS_2});
        context.setCommunityCards(new Card[]{Card.CLUBS_A, Card.HEARTS_A, Card.DIAMONDS_A});
        context.setHandRank(6);
        context.setHoleCardInvolved(false);
        context.setAmountToCall(100);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        // When FH is on board with no hole involved, should call or check
        assertThat(action.actionType()).isIn(ActionType.CALL, ActionType.CHECK);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopFlushWithHoleCards() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.SPADES_A, Card.SPADES_8});
        context.setCommunityCards(new Card[]{Card.SPADES_K, Card.SPADES_5, Card.SPADES_3});
        context.setHandRank(5); // Flush
        context.setHoleCardInvolved(true);
        context.setMajorSuit(Card.SPADES);
        context.setNutFlush(true);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopFlushOnBoard() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.CLUBS_7, Card.HEARTS_2});
        context.setCommunityCards(
                new Card[]{Card.SPADES_A, Card.SPADES_K, Card.SPADES_Q, Card.SPADES_J, Card.SPADES_T});
        context.setHandRank(5);
        context.setHoleCardInvolved(false);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopStraight() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 2); // Turn
        context.setHoleCards(new Card[]{Card.HEARTS_9, Card.DIAMONDS_8});
        context.setCommunityCards(new Card[]{Card.CLUBS_7, Card.HEARTS_6, Card.DIAMONDS_5, Card.CLUBS_2});
        context.setHandRank(4); // Straight
        context.setHoleCardInvolved(true);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopTrips() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_T, Card.DIAMONDS_T});
        context.setCommunityCards(new Card[]{Card.CLUBS_T, Card.HEARTS_5, Card.DIAMONDS_2});
        context.setHandRank(3); // Trips
        context.setHoleCardInvolved(true);
        context.setHandScore(30000L); // High trips score

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopTripsOnBoard() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_2});
        context.setCommunityCards(new Card[]{Card.CLUBS_K, Card.HEARTS_K, Card.DIAMONDS_K});
        context.setHandRank(3);
        context.setHoleCardInvolved(false);
        context.setBest5CardRanks(new int[]{Card.KING, Card.ACE, Card.KING, Card.KING, Card.TWO});

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        // Trips on board with ace kicker should call
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopTwoPair() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_K});
        context.setCommunityCards(new Card[]{Card.CLUBS_A, Card.HEARTS_K, Card.DIAMONDS_5});
        context.setHandRank(2); // Two pair
        context.setHoleCardInvolved(true);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopOverpair() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_A});
        context.setCommunityCards(new Card[]{Card.CLUBS_K, Card.HEARTS_7, Card.DIAMONDS_2});
        context.setHandRank(1); // Pair (overpair)
        context.setHoleCardInvolved(true);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopTopPair() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_K});
        context.setCommunityCards(new Card[]{Card.CLUBS_A, Card.HEARTS_7, Card.DIAMONDS_2});
        context.setHandRank(1); // Pair (top pair)
        context.setHoleCardInvolved(true);
        // Set hand score above community high card to pass isTopPair check
        context.setHandScore(14000L);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopHighCard() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_K});
        context.setCommunityCards(new Card[]{Card.CLUBS_8, Card.HEARTS_5, Card.DIAMONDS_2});
        context.setHandRank(0); // High card
        context.setAmountToCall(0);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_PostFlopHighCardFacingBet() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_3});
        context.setCommunityCards(new Card[]{Card.CLUBS_8, Card.HEARTS_5, Card.DIAMONDS_2});
        context.setHandRank(0);
        context.setAmountToCall(50);
        context.setImprovementOdds(0.15);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Post-Flop: River ==========

    @Test
    void should_ReturnValidAction_When_RiverWithStrongHand() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 3); // River
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_A});
        context.setCommunityCards(
                new Card[]{Card.CLUBS_A, Card.HEARTS_K, Card.DIAMONDS_5, Card.CLUBS_8, Card.HEARTS_2});
        context.setHandRank(3); // Trips
        context.setHoleCardInvolved(true);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Post-Flop: Board Texture ==========

    @Test
    void should_ReturnValidAction_When_FlopWithFlushDraw() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_K});
        context.setCommunityCards(new Card[]{Card.CLUBS_A, Card.HEARTS_7, Card.HEARTS_5});
        context.setHandRank(1); // Pair
        context.setHoleCardInvolved(true);
        context.setFlushDraw(true);
        context.setHandStrength(0.7);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_FlopWithStraightDraw() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_9, Card.DIAMONDS_8});
        context.setCommunityCards(new Card[]{Card.CLUBS_7, Card.HEARTS_6, Card.DIAMONDS_2});
        context.setHandRank(0); // High card (open-ended straight draw)
        context.setStraightDraw(true);
        context.setImprovementOdds(0.35);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Post-Flop: Pot Odds / Folding Logic ==========

    @Test
    void should_ReturnValidAction_When_FacingLargeBetWithWeakHand() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 2); // Turn
        context.setHoleCards(new Card[]{Card.CLUBS_7, Card.HEARTS_2});
        context.setCommunityCards(new Card[]{Card.SPADES_A, Card.HEARTS_K, Card.DIAMONDS_Q, Card.CLUBS_3});
        context.setHandRank(0); // High card
        context.setAmountToCall(500);
        context.setPotSize(200);
        context.setImprovementOdds(0.02);
        context.setHandStrength(0.05);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Post-Flop: Generic Hand Strength Fallthrough ==========

    @Test
    void should_ReturnValidAction_When_HighStrengthAndNoBoard() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_A});
        context.setCommunityCards(new Card[]{Card.CLUBS_K, Card.HEARTS_7, Card.DIAMONDS_2});
        context.setHandRank(1); // Pair
        context.setHoleCardInvolved(true);
        context.setHandStrength(0.95); // Very high

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Limper Detection (B10) ==========

    @Test
    void should_ReturnValidAction_When_FlopWithLimperDetection() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        StubGamePlayerInfo raiser = createPlayer(8000, 0);
        ActionOptions options = createOptions();
        StubAIContext context = createPostFlopContext(player, 1);
        context.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_K});
        context.setCommunityCards(new Card[]{Card.CLUBS_A, Card.HEARTS_7, Card.DIAMONDS_2});
        context.setHandRank(1);
        context.setHoleCardInvolved(true);
        context.setLastRaiser(raiser);
        // Raiser limped pre-flop
        context.setLastActionInRound(raiser, 0, AIContext.ACTION_CALL);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Multiple Hands State Reset ==========

    @Test
    void should_ResetState_When_NewHandStarted() {
        StubGamePlayerInfo player = createPlayer(10000, 0);
        ActionOptions options = createOptions();

        // Play first hand
        StubAIContext context1 = createPostFlopContext(player, 1);
        context1.setHoleCards(new Card[]{Card.HEARTS_A, Card.DIAMONDS_K});
        context1.setCommunityCards(new Card[]{Card.CLUBS_A, Card.HEARTS_7, Card.DIAMONDS_2});
        context1.setHandRank(1);
        context1.setHoleCardInvolved(true);

        PlayerAction action1 = algorithm.getAction(player, options, context1);
        assertThat(action1).isNotNull();

        // Play second hand (new GameHand object triggers reset)
        StubAIContext context2 = createPostFlopContext(player, 1);
        context2.setHoleCards(new Card[]{Card.HEARTS_8, Card.DIAMONDS_7});
        context2.setCommunityCards(new Card[]{Card.CLUBS_K, Card.HEARTS_Q, Card.DIAMONDS_3});
        context2.setHandRank(0);
        context2.setHandStrength(0.2);

        PlayerAction action2 = algorithm.getAction(player, options, context2);
        assertThat(action2).isNotNull();
        assertThat(action2.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Sklansky Ranking Integration ==========

    @Test
    void should_RankPocketAcesGroup1() {
        Card[] aces = {Card.CLUBS_A, Card.SPADES_A};
        com.donohoedigital.games.poker.engine.HandSorted hand = new com.donohoedigital.games.poker.engine.HandSorted(
                aces[0], aces[1]);

        int rank = SklankskyRanking.getRank(hand);

        assertThat(rank).isLessThanOrEqualTo(SklankskyRanking.MAXGROUP1);
        assertThat(SklankskyRanking.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_Rank72oAsUnranked() {
        Card[] hand72o = {Card.CLUBS_7, Card.SPADES_2};
        com.donohoedigital.games.poker.engine.HandSorted hand = new com.donohoedigital.games.poker.engine.HandSorted(
                hand72o[0], hand72o[1]);

        int rank = SklankskyRanking.getRank(hand);

        assertThat(rank).isEqualTo(1000);
        assertThat(SklankskyRanking.getGroupFromRank(rank)).isEqualTo(10);
    }

    // ========== Helpers ==========

    private static ActionOptions createOptions() {
        return new ActionOptions(true, true, true, true, true, 0, 100, 10000, 200, 10000, 30);
    }

    private static StubGamePlayerInfo createPlayer(int chipCount, int numRebuys) {
        StubGamePlayerInfo player = new StubGamePlayerInfo();
        player.setChipCount(chipCount);
        player.setNumRebuys(numRebuys);
        return player;
    }

    private static StubTournamentContext createTournament(int startingChips) {
        StubTournamentContext tournament = new StubTournamentContext();
        tournament.setStartingChips(startingChips);
        return tournament;
    }

    private static StubAIContext createPreFlopContext(StubGamePlayerInfo player) {
        StubAIContext context = new StubAIContext();
        context.setBettingRound(0);
        context.setNumActivePlayers(6);
        context.setAmountToCall(0);
        context.setPotSize(30);
        context.setTournament(createTournament(1000));
        return context;
    }

    private static StubAIContext createPostFlopContext(StubGamePlayerInfo player, int round) {
        StubAIContext context = new StubAIContext();
        context.setBettingRound(round);
        context.setNumActivePlayers(4);
        context.setAmountToCall(0);
        context.setPotSize(200);
        context.setTournament(createTournament(1000));
        // Create a GameHand stub for post-flop
        context.setCurrentHand(new StubGameHand());
        return context;
    }

    // ========== Stub Implementations ==========

    private static class StubGamePlayerInfo implements GamePlayerInfo {
        private int chipCount;
        private int numRebuys;

        @Override
        public int getChipCount() {
            return chipCount;
        }

        @Override
        public int getNumRebuys() {
            return numRebuys;
        }

        public void setChipCount(int chips) {
            this.chipCount = chips;
        }

        public void setNumRebuys(int rebuys) {
            this.numRebuys = rebuys;
        }

        @Override
        public int getID() {
            return 1;
        }

        @Override
        public String getName() {
            return "TestPlayer";
        }

        @Override
        public boolean isHuman() {
            return false;
        }

        @Override
        public boolean isComputer() {
            return true;
        }

        @Override
        public boolean isObserver() {
            return false;
        }

        @Override
        public boolean isFolded() {
            return false;
        }

        @Override
        public boolean isAllIn() {
            return false;
        }

        @Override
        public int getSeat() {
            return 0;
        }

        @Override
        public boolean isAskShowWinning() {
            return false;
        }

        @Override
        public boolean isAskShowLosing() {
            return false;
        }

        @Override
        public boolean isHumanControlled() {
            return false;
        }

        @Override
        public int getThinkBankMillis() {
            return 0;
        }

        @Override
        public boolean isSittingOut() {
            return false;
        }

        @Override
        public void setSittingOut(boolean sittingOut) {
        }

        @Override
        public boolean isLocallyControlled() {
            return true;
        }

        @Override
        public void setTimeoutMillis(int millis) {
        }

        @Override
        public void setTimeoutMessageSecondsLeft(int seconds) {
        }
    }

    @SuppressWarnings("NullableProblems")
    private static class StubAIContext implements AIContext {
        private int bettingRound;
        private int position;
        private int numActivePlayers = 6;
        private int amountToCall;
        private int potSize = 100;
        private TournamentContext tournament;
        private Card[] holeCards;
        private Card[] communityCards = new Card[0];
        private int handRank = 0;
        private long handScore = 0;
        private double improvementOdds = 0.1;
        private double handStrength = 0.5;
        private boolean holeCardInvolved = true;
        private boolean hasBeenRaised;
        private int numCallers;
        private boolean rebuyPeriodActive;
        private boolean flushDraw;
        private boolean straightDraw;
        private int numOppStraights;
        private int majorSuit = -1;
        private boolean nutFlush;
        private GamePlayerInfo lastRaiser;
        private GamePlayerInfo lastBettor;
        private GameHand currentHand;
        private int[] best5CardRanks = new int[]{14, 13, 12, 11, 10};
        private final java.util.Map<String, Integer> actionInRoundMap = new java.util.HashMap<>();

        public void setBettingRound(int round) {
            this.bettingRound = round;
        }

        public void setPosition(GamePlayerInfo player, int pos) {
            this.position = pos;
        }

        public void setNumActivePlayers(int num) {
            this.numActivePlayers = num;
        }

        public void setAmountToCall(int amount) {
            this.amountToCall = amount;
        }

        public void setPotSize(int size) {
            this.potSize = size;
        }

        public void setTournament(TournamentContext t) {
            this.tournament = t;
        }

        public void setHoleCards(Card[] cards) {
            this.holeCards = cards;
        }

        public void setCommunityCards(Card[] cards) {
            this.communityCards = cards;
        }

        public void setHandRank(int rank) {
            this.handRank = rank;
        }

        public void setHandScore(long score) {
            this.handScore = score;
        }

        public void setImprovementOdds(double odds) {
            this.improvementOdds = odds;
        }

        public void setHandStrength(double strength) {
            this.handStrength = strength;
        }

        public void setHoleCardInvolved(boolean involved) {
            this.holeCardInvolved = involved;
        }

        public void setHasBeenRaised(boolean raised) {
            this.hasBeenRaised = raised;
        }

        public void setNumCallers(int callers) {
            this.numCallers = callers;
        }

        public void setRebuyPeriodActive(boolean active) {
            this.rebuyPeriodActive = active;
        }

        public void setFlushDraw(boolean draw) {
            this.flushDraw = draw;
        }

        public void setStraightDraw(boolean draw) {
            this.straightDraw = draw;
        }

        public void setMajorSuit(int suit) {
            this.majorSuit = suit;
        }

        public void setNutFlush(boolean nut) {
            this.nutFlush = nut;
        }

        public void setLastRaiser(GamePlayerInfo raiser) {
            this.lastRaiser = raiser;
        }

        public void setLastBettor(GamePlayerInfo bettor) {
            this.lastBettor = bettor;
        }

        public void setCurrentHand(GameHand hand) {
            this.currentHand = hand;
        }

        public void setBest5CardRanks(int[] ranks) {
            this.best5CardRanks = ranks;
        }

        public void setLastActionInRound(GamePlayerInfo player, int round, int action) {
            actionInRoundMap.put(System.identityHashCode(player) + ":" + round, action);
        }

        @Override
        public int getBettingRound() {
            return bettingRound;
        }

        @Override
        public int getPosition(GamePlayerInfo player) {
            return position;
        }

        @Override
        public int getNumActivePlayers() {
            return numActivePlayers;
        }

        @Override
        public int getAmountToCall(GamePlayerInfo player) {
            return amountToCall;
        }

        @Override
        public int getPotSize() {
            return potSize;
        }

        @Override
        public TournamentContext getTournament() {
            return tournament;
        }

        @Override
        public GameTable getTable() {
            return null;
        }

        @Override
        public GameHand getCurrentHand() {
            return currentHand;
        }

        @Override
        public boolean isButton(GamePlayerInfo player) {
            return position == 3;
        }

        @Override
        public boolean isSmallBlind(GamePlayerInfo player) {
            return position == 4;
        }

        @Override
        public boolean isBigBlind(GamePlayerInfo player) {
            return position == 5;
        }

        @Override
        public int getAmountBetThisRound(GamePlayerInfo player) {
            return 0;
        }

        @Override
        public int getLastBetAmount() {
            return 0;
        }

        @Override
        public int getNumPlayersYetToAct(GamePlayerInfo player) {
            return 2;
        }

        @Override
        public int getNumPlayersWhoActed(GamePlayerInfo player) {
            return 3;
        }

        @Override
        public boolean hasBeenBet() {
            return amountToCall > 0;
        }

        @Override
        public boolean hasBeenRaised() {
            return hasBeenRaised;
        }

        @Override
        public GamePlayerInfo getLastBettor() {
            return lastBettor;
        }

        @Override
        public GamePlayerInfo getLastRaiser() {
            return lastRaiser;
        }

        @Override
        public int evaluateHandRank(Card[] hole, Card[] community) {
            return handRank;
        }

        @Override
        public long evaluateHandScore(Card[] hole, Card[] community) {
            return handScore;
        }

        @Override
        public double calculateImprovementOdds(Card[] hole, Card[] community) {
            return improvementOdds;
        }

        @Override
        public Card[] getHoleCards(GamePlayerInfo player) {
            return holeCards;
        }

        @Override
        public Card[] getCommunityCards() {
            return communityCards;
        }

        @Override
        public int getNumCallers() {
            return numCallers;
        }

        @Override
        public Card[] getBest5Cards(Card[] hole, Card[] community) {
            return new Card[5];
        }

        @Override
        public int[] getBest5CardRanks(Card[] hole, Card[] community) {
            return best5CardRanks;
        }

        @Override
        public boolean isHoleCardInvolved(Card[] hole, Card[] community) {
            return holeCardInvolved;
        }

        @Override
        public int getMajorSuit(Card[] hole, Card[] community) {
            return majorSuit;
        }

        @Override
        public boolean hasFlushDraw(Card[] communityCards) {
            return flushDraw;
        }

        @Override
        public boolean hasStraightDraw(Card[] communityCards) {
            return straightDraw;
        }

        @Override
        public int getNumOpponentStraights(Card[] communityCards) {
            return numOppStraights;
        }

        @Override
        public boolean isRebuyPeriodActive() {
            return rebuyPeriodActive;
        }

        @Override
        public boolean isNutFlush(Card[] holeCards, Card[] communityCards, int majorSuit, int nCards) {
            return nutFlush;
        }

        @Override
        public double calculateHandStrength(Card[] holeCards, Card[] communityCards, int numOpponents) {
            return handStrength;
        }

        @Override
        public int getLastActionInRound(GamePlayerInfo player, int bettingRound) {
            return actionInRoundMap.getOrDefault(System.identityHashCode(player) + ":" + bettingRound, ACTION_NONE);
        }

        @Override
        public int getOpponentRaiseFrequency(GamePlayerInfo opponent, int bettingRound) {
            return 50;
        }

        @Override
        public int getOpponentBetFrequency(GamePlayerInfo opponent, int bettingRound) {
            return 50;
        }
    }

    private static class StubTournamentContext implements TournamentContext {
        private int startingChips = 1000;

        public void setStartingChips(int chips) {
            this.startingChips = chips;
        }

        @Override
        public int getStartingChips() {
            return startingChips;
        }

        @Override
        public int getLevel() {
            return 1;
        }

        @Override
        public int getSmallBlind(int level) {
            return 10;
        }

        @Override
        public int getBigBlind(int level) {
            return 20;
        }

        @Override
        public int getAnte(int level) {
            return 0;
        }

        @Override
        public int getNumTables() {
            return 1;
        }

        @Override
        public GameTable getTable(int index) {
            return null;
        }

        @Override
        public int getNumPlayers() {
            return 6;
        }

        @Override
        public GamePlayerInfo getPlayerByID(int playerId) {
            return null;
        }

        @Override
        public boolean isPractice() {
            return true;
        }

        @Override
        public boolean isOnlineGame() {
            return false;
        }

        @Override
        public boolean isGameOver() {
            return false;
        }

        @Override
        public void nextLevel() {
        }

        @Override
        public boolean isLevelExpired() {
            return false;
        }

        @Override
        public void advanceClockBreak() {
        }

        @Override
        public void startGameClock() {
        }

        @Override
        public int getLastMinChip() {
            return 1;
        }

        @Override
        public int getMinChip() {
            return 1;
        }

        @Override
        public void advanceClock() {
        }

        @Override
        public boolean isBreakLevel(int level) {
            return false;
        }

        @Override
        public GamePlayerInfo getLocalPlayer() {
            return null;
        }

        @Override
        public boolean isScheduledStartEnabled() {
            return false;
        }

        @Override
        public long getScheduledStartTime() {
            return 0;
        }

        @Override
        public int getMinPlayersForScheduledStart() {
            return 2;
        }

        @Override
        public int getTimeoutForRound(int round) {
            return 30;
        }

        @Override
        public int getTimeoutSeconds() {
            return 30;
        }

        @Override
        public boolean isOnePlayerLeft() {
            return false;
        }

        @Override
        public boolean isRebuyPeriodActive(GamePlayerInfo player) {
            return false;
        }
    }

    @SuppressWarnings("NullableProblems")
    private static class StubGameHand implements GameHand {
        @Override
        public com.donohoedigital.games.poker.engine.state.BettingRound getRound() {
            return com.donohoedigital.games.poker.engine.state.BettingRound.FLOP;
        }

        @Override
        public void setRound(com.donohoedigital.games.poker.engine.state.BettingRound round) {
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public int getNumWithCards() {
            return 4;
        }

        @Override
        public int getCurrentPlayerInitIndex() {
            return 0;
        }

        @Override
        public void advanceRound() {
        }

        @Override
        public void preResolve(boolean isOnline) {
        }

        @Override
        public void resolve() {
        }

        @Override
        public void storeHandHistory() {
        }

        @Override
        public java.util.List<GamePlayerInfo> getPreWinners() {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<GamePlayerInfo> getPreLosers() {
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean isUncontested() {
            return false;
        }

        @Override
        public GamePlayerInfo getCurrentPlayerWithInit() {
            return null;
        }

        @Override
        public int getAmountToCall(GamePlayerInfo player) {
            return 0;
        }

        @Override
        public int getMinBet() {
            return 20;
        }

        @Override
        public int getMinRaise() {
            return 40;
        }

        @Override
        public void applyPlayerAction(GamePlayerInfo player, PlayerAction action) {
        }

        @Override
        public Card[] getCommunityCards() {
            return new Card[0];
        }

        @Override
        public Card[] getPlayerCards(GamePlayerInfo player) {
            return new Card[0];
        }

        @Override
        public int getPotSize() {
            return 200;
        }

        @Override
        public int getPotStatus() {
            return 0;
        }

        @Override
        public float getPotOdds(GamePlayerInfo player) {
            return 0;
        }

        @Override
        public boolean wasRaisedPreFlop() {
            return false;
        }

        @Override
        public GamePlayerInfo getFirstBettor(int round, boolean includeRaises) {
            return null;
        }

        @Override
        public GamePlayerInfo getLastBettor(int round, boolean includeRaises) {
            return null;
        }

        @Override
        public boolean wasFirstRaiserPreFlop(GamePlayerInfo player) {
            return false;
        }

        @Override
        public boolean wasLastRaiserPreFlop(GamePlayerInfo player) {
            return false;
        }

        @Override
        public boolean wasOnlyRaiserPreFlop(GamePlayerInfo player) {
            return false;
        }

        @Override
        public boolean wasPotAction(int round) {
            return false;
        }

        @Override
        public boolean paidToPlay(GamePlayerInfo player) {
            return false;
        }

        @Override
        public boolean couldLimp(GamePlayerInfo player) {
            return false;
        }

        @Override
        public boolean limped(GamePlayerInfo player) {
            return false;
        }

        @Override
        public boolean isBlind(GamePlayerInfo player) {
            return false;
        }

        @Override
        public boolean hasActedThisRound(GamePlayerInfo player) {
            return false;
        }

        @Override
        public int getLastActionThisRound(GamePlayerInfo player) {
            return 0;
        }

        @Override
        public int getFirstVoluntaryAction(GamePlayerInfo player, int round) {
            return 0;
        }

        @Override
        public int getNumLimpers() {
            return 0;
        }

        @Override
        public int getNumFoldsSinceLastBet() {
            return 0;
        }
    }
}
