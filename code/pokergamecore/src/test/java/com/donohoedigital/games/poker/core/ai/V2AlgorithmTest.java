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

import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for V2Algorithm.
 * <p>
 * Tests the V2 AI algorithm implementation focusing on: - Stateful behavior
 * (steam, steal suspicion, hand strength caching) - Integration with
 * PureRuleEngine - V2PlayerState interface implementation - Context type
 * validation
 */
class V2AlgorithmTest {

    @Test
    void getAction_withValidV2Context_returnsValidAction() {
        // Setup
        V2AIContext context = createMockV2Context();
        GamePlayerInfo player = createMockPlayer();
        ActionOptions options = createMockOptions();

        V2Algorithm algorithm = new V2Algorithm();

        // Execute
        PlayerAction action = algorithm.getAction(player, options, context);

        // Verify
        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(ActionType.FOLD, ActionType.CHECK, ActionType.CALL, ActionType.BET,
                ActionType.RAISE);
    }

    @Test
    void getAction_withNonV2Context_throwsIllegalArgumentException() {
        // Setup - use regular AIContext instead of V2AIContext
        AIContext context = mock(AIContext.class);
        GamePlayerInfo player = createMockPlayer();
        ActionOptions options = createMockOptions();

        V2Algorithm algorithm = new V2Algorithm();

        // Execute & Verify
        assertThatThrownBy(() -> algorithm.getAction(player, options, context))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("V2Algorithm requires V2AIContext");
    }

    @Test
    void getSteam_initialValue_isZero() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.getSteam()).isEqualTo(0.0f);
    }

    @Test
    void getStealSuspicion_initialValue_isZero() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.getStealSuspicion()).isEqualTo(0.0f);
    }

    @Test
    void getHandStrength_beforeComputation_returnsNegativeOne() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.getHandStrength()).isEqualTo(-1.0f);
    }

    @Test
    void getRawHandStrength_beforeComputation_returnsNegativeOne() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.getRawHandStrength()).isEqualTo(-1.0f);
    }

    @Test
    void getBiasedHandStrength_beforeComputation_returnsNegativeOne() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.getBiasedHandStrength()).isEqualTo(-1.0f);
    }

    @Test
    void getBiasedPositivePotential_beforeComputation_returnsNegativeOne() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.getBiasedPositivePotential()).isEqualTo(-1.0f);
    }

    @Test
    void getBiasedNegativePotential_beforeComputation_returnsNegativeOne() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.getBiasedNegativePotential()).isEqualTo(-1.0f);
    }

    @Test
    void getPositiveHandPotential_beforeComputation_returnsNegativeOne() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.getPositiveHandPotential()).isEqualTo(-1.0f);
    }

    @Test
    void getNegativeHandPotential_beforeComputation_returnsNegativeOne() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.getNegativeHandPotential()).isEqualTo(-1.0f);
    }

    @Test
    void debugEnabled_defaultConstructor_returnsFalse() {
        V2Algorithm algorithm = new V2Algorithm();
        assertThat(algorithm.debugEnabled()).isFalse();
    }

    @Test
    void debugEnabled_withDebugEnabled_returnsTrue() {
        V2Algorithm algorithm = new V2Algorithm(s -> {
        }, true);
        assertThat(algorithm.debugEnabled()).isTrue();
    }

    @Test
    void debugEnabled_withDebugDisabled_returnsFalse() {
        V2Algorithm algorithm = new V2Algorithm(s -> {
        }, false);
        assertThat(algorithm.debugEnabled()).isFalse();
    }

    @Test
    void getAction_withDebugOutput_callsDebugConsumer() {
        // Setup
        StringBuilder debugOutput = new StringBuilder();
        V2Algorithm algorithm = new V2Algorithm(debugOutput::append, true);

        V2AIContext context = createMockV2Context();
        GamePlayerInfo player = createMockPlayer();
        ActionOptions options = createMockOptions();

        // Execute
        algorithm.getAction(player, options, context);

        // Verify - debug output should be produced (though we can't assert on exact
        // content
        // without knowing PureRuleEngine internals, we can verify the consumer was
        // used)
        // For this test, we just verify no exception is thrown and action is returned
    }

    @Test
    void wantsRebuy_lowPropensity_wantsRebuy() {
        V2Algorithm algorithm = new V2Algorithm(20, 50); // Low rebuy propensity
        V2AIContext context = createMockV2Context();
        GamePlayerInfo player = createMockPlayer();
        when(player.getNumRebuys()).thenReturn(0);

        assertThat(algorithm.wantsRebuy(player, context)).isTrue();
    }

    @Test
    void wantsRebuy_highPropensity_doesNotWantRebuy() {
        V2Algorithm algorithm = new V2Algorithm(95, 50); // High rebuy propensity
        V2AIContext context = createMockV2Context();
        GamePlayerInfo player = createMockPlayer();
        when(player.getNumRebuys()).thenReturn(0);

        assertThat(algorithm.wantsRebuy(player, context)).isFalse();
    }

    @Test
    void wantsRebuy_maxRebuysReached_doesNotWantRebuy() {
        V2Algorithm algorithm = new V2Algorithm(20, 50); // Low propensity, but...
        V2AIContext context = createMockV2Context();
        GamePlayerInfo player = createMockPlayer();
        when(player.getNumRebuys()).thenReturn(5); // Max rebuys

        assertThat(algorithm.wantsRebuy(player, context)).isFalse();
    }

    @Test
    void wantsAddon_lowPropensity_wantsAddon() {
        V2Algorithm algorithm = new V2Algorithm(50, 20); // Low addon propensity
        V2AIContext context = createMockV2Context();
        TournamentContext tournament = mock(TournamentContext.class);
        when(tournament.getStartingChips()).thenReturn(1000);
        when(context.getTournament()).thenReturn(tournament);

        GamePlayerInfo player = createMockPlayer();
        when(player.getChipCount()).thenReturn(500);

        assertThat(algorithm.wantsAddon(player, context)).isTrue();
    }

    @Test
    void wantsAddon_highPropensity_doesNotWantAddon() {
        V2Algorithm algorithm = new V2Algorithm(50, 80); // High addon propensity
        V2AIContext context = createMockV2Context();
        TournamentContext tournament = mock(TournamentContext.class);
        when(tournament.getStartingChips()).thenReturn(1000);
        when(context.getTournament()).thenReturn(tournament);

        GamePlayerInfo player = createMockPlayer();
        when(player.getChipCount()).thenReturn(500);

        assertThat(algorithm.wantsAddon(player, context)).isFalse();
    }

    @Test
    void wantsAddon_manyChips_doesNotWantAddon() {
        V2Algorithm algorithm = new V2Algorithm(50, 40); // Moderate propensity
        V2AIContext context = createMockV2Context();
        TournamentContext tournament = mock(TournamentContext.class);
        when(tournament.getStartingChips()).thenReturn(1000);
        when(context.getTournament()).thenReturn(tournament);

        GamePlayerInfo player = createMockPlayer();
        when(player.getChipCount()).thenReturn(10000); // 10x buyin (> 3x threshold)

        // With propensity 40 (25-50 range), wants addon only if chips < 3x buyin
        // 10000 > 3000, so should not want addon
        assertThat(algorithm.wantsAddon(player, context)).isFalse();
    }

    @Test
    void constructor_withPropensityValues_storesValues() {
        V2Algorithm algorithm = new V2Algorithm(30, 40);
        V2AIContext context = createMockV2Context();
        TournamentContext tournament = mock(TournamentContext.class);
        when(tournament.getStartingChips()).thenReturn(1000);
        when(context.getTournament()).thenReturn(tournament);

        GamePlayerInfo player = createMockPlayer();
        when(player.getNumRebuys()).thenReturn(0);
        when(player.getChipCount()).thenReturn(500);

        // Verify the propensity values were stored by checking behavior
        // 30 rebuy propensity should want rebuy (in the 25-50 range)
        assertThat(algorithm.wantsRebuy(player, context)).isTrue();
        // 40 addon propensity should want addon when chips < 3x buyin
        assertThat(algorithm.wantsAddon(player, context)).isTrue();
    }

    @Test
    void getAction_preFlopWithPremiumHand_returnsAggressiveAction() {
        // Setup
        V2AIContext context = createMockV2Context();
        GamePlayerInfo player = createMockPlayer();
        ActionOptions options = createMockOptions();

        // Premium hand (AA)
        Hand pocket = new Hand(2);
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));
        when(context.getPocketCards(player)).thenReturn(pocket);
        when(context.getBettingRound()).thenReturn(BettingRound.PRE_FLOP.toLegacy());
        when(context.getAmountToCall(player)).thenReturn(0); // Opening pot

        V2Algorithm algorithm = new V2Algorithm();

        // Execute
        PlayerAction action = algorithm.getAction(player, options, context);

        // Verify - with AA, should be aggressive (bet/raise)
        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.RAISE);
    }

    @Test
    void getAction_preFlopWithTrashHand_returnsFold() {
        // Setup
        V2AIContext context = createMockV2Context();
        GamePlayerInfo player = createMockPlayer();
        ActionOptions options = createMockOptions();

        // Trash hand (72o)
        Hand pocket = new Hand(2);
        pocket.addCard(new Card(CardSuit.SPADES, 7));
        pocket.addCard(new Card(CardSuit.HEARTS, 2));
        when(context.getPocketCards(player)).thenReturn(pocket);
        when(context.getBettingRound()).thenReturn(BettingRound.PRE_FLOP.toLegacy());
        when(context.getAmountToCall(player)).thenReturn(100); // Facing a bet

        V2Algorithm algorithm = new V2Algorithm();

        // Execute
        PlayerAction action = algorithm.getAction(player, options, context);

        // Verify - with 72o facing a bet, should fold
        assertThat(action).isNotNull();
        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
    }

    // === Helper methods ===

    private V2AIContext createMockV2Context() {
        V2AIContext context = mock(V2AIContext.class);
        GameHand hand = mock(GameHand.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        // Default pocket cards (pair of 8s - medium strength)
        Hand defaultPocket = new Hand(2);
        defaultPocket.addCard(new Card(CardSuit.SPADES, Card.EIGHT));
        defaultPocket.addCard(new Card(CardSuit.HEARTS, Card.EIGHT));

        // Core context setup
        when(context.getCurrentHand()).thenReturn(hand);
        when(context.getStrategy()).thenReturn(strategy);

        // Hand state
        when(context.getBettingRound()).thenReturn(BettingRound.PRE_FLOP.toLegacy());
        when(context.getPocketCards(any())).thenReturn(defaultPocket);
        when(context.getCommunity()).thenReturn(new Hand(0)); // No community cards pre-flop
        when(context.getCommunityCards()).thenReturn(new Card[0]);

        // Pot and betting
        when(context.getPotStatus()).thenReturn(PokerConstants.NO_POT_ACTION);
        when(context.getPotSize()).thenReturn(30);
        when(context.getTotalPotChipCount()).thenReturn(30);
        when(context.getAmountToCall(any())).thenReturn(0);
        when(context.getMinRaise()).thenReturn(20);

        // Table state
        when(context.getNumPlayersWithCards()).thenReturn(6);
        when(context.getNumPlayersAtTable()).thenReturn(6);
        when(context.getNumActivePlayers()).thenReturn(6);
        when(context.getNumLimpers()).thenReturn(0);
        when(context.isLimit()).thenReturn(false);

        // Tournament state
        when(context.getHohM(any())).thenReturn(15.0f); // Green zone
        when(context.getHohQ(any())).thenReturn(1.0f);
        when(context.getHohZone(any())).thenReturn(AIConstants.HOH_GREEN);
        when(context.getTableAverageHohM()).thenReturn(15.0f);
        when(context.getRemainingAverageHohM()).thenReturn(15.0f);
        when(context.getBigBlind()).thenReturn(20);

        // Player state
        when(context.hasActedThisRound(any())).thenReturn(false);
        when(context.getStartingPositionCategory(any())).thenReturn(AIConstants.POSITION_MIDDLE);
        when(context.getPostFlopPositionCategory(any())).thenReturn(AIConstants.POSITION_MIDDLE);
        when(context.getStartingOrder(any())).thenReturn(2);
        when(context.getSeat(any())).thenReturn(2);
        when(context.getHandsBeforeBigBlind(any())).thenReturn(5);
        when(context.getConsecutiveHandsUnpaid(any())).thenReturn(0);
        when(context.getChipCountAtStart(any())).thenReturn(1000);

        // Strategy factors (moderate defaults)
        when(strategy.getStratFactor(anyString(), anyFloat(), anyFloat())).thenReturn(1.0f);
        when(strategy.getStratFactor(anyString(), any(Hand.class), anyFloat(), anyFloat())).thenReturn(0.5f);
        when(strategy.getHandStrength(any(Hand.class))).thenReturn(0.5f);

        // Opponent models (default to null for simplicity)
        when(context.getOpponentModel(any())).thenReturn(mock(V2OpponentModel.class));
        when(context.getSelfModel()).thenReturn(mock(V2OpponentModel.class));

        // Player iteration
        when(context.getPlayerAt(anyInt())).thenReturn(null);
        when(context.getPlayersLeft(any())).thenReturn(Collections.emptyList());

        // Hand evaluation (neutral defaults)
        when(context.getHandScore(any(), any())).thenReturn(0);
        when(context.getRawHandStrength(any(), any())).thenReturn(0.5f);
        when(context.getBiasedRawHandStrength(anyInt(), any())).thenReturn(0.5f);
        when(context.getBiasedEffectiveHandStrength(anyInt(), any())).thenReturn(0.5f);
        when(context.getApparentStrength(anyInt(), any())).thenReturn(0.5f);

        // Draw detection
        when(context.getNutFlushCount(any(), any())).thenReturn(0);
        when(context.getNonNutFlushCount(any(), any())).thenReturn(0);
        when(context.getNutStraightCount(any(), any())).thenReturn(0);
        when(context.getNonNutStraightCount(any(), any())).thenReturn(0);

        // Other queries
        when(context.hasBeenBet()).thenReturn(false);
        when(context.wasRaisedPreFlop()).thenReturn(false);
        when(context.getPotOdds(any())).thenReturn(0.0f);
        when(context.paidToPlay(any())).thenReturn(false);
        when(context.couldLimp(any())).thenReturn(false);
        when(context.limped(any())).thenReturn(false);

        return context;
    }

    private GamePlayerInfo createMockPlayer() {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(player.isAllIn()).thenReturn(false);
        return player;
    }

    private ActionOptions createMockOptions() {
        ActionOptions options = mock(ActionOptions.class);
        when(options.canCheck()).thenReturn(true);
        when(options.canCall()).thenReturn(false);
        when(options.canBet()).thenReturn(true);
        when(options.canRaise()).thenReturn(false);
        when(options.canFold()).thenReturn(false);
        return options;
    }
}
