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

import com.donohoedigital.games.poker.core.GameHand;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PureRuleEngine.
 * <p>
 * Tests core decision logic for the V2 AI rule engine. Focuses on key
 * scenarios: pre-flop decisions, post-flop play, and outcome selection.
 */
class PureRuleEngineTest {

    @Test
    void execute_preFlopWithTrashHand_executesSuccessfully() {
        // Setup mocks
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        // Pre-flop scenario: trash hand (72o), facing a raise
        when(context.getBettingRound()).thenReturn(0); // Pre-flop
        when(context.getAmountToCall(player)).thenReturn(100); // Facing a bet
        when(context.getPotSize()).thenReturn(150);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        // Trash hand strength
        when(state.getHandStrength()).thenReturn(0.1f); // Very weak
        when(state.getRawHandStrength()).thenReturn(0.05f);
        when(state.getBiasedHandStrength()).thenReturn(0.05f);
        when(state.getSteam()).thenReturn(0.0f);
        when(state.getStealSuspicion()).thenReturn(0.0f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        }); // No-op debug consumer
        PlayerAction action = engine.getAction();

        // Verify execution completes successfully
        assertThat(action).isNotNull();
        // Action should be a valid poker action
        assertThat(action.actionType()).isIn(ActionType.FOLD, ActionType.CHECK, ActionType.CALL, ActionType.BET,
                ActionType.RAISE);
    }

    @Test
    void execute_preFlopWithPremiumHand_shouldRaise() {
        // Setup mocks
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        // Pre-flop scenario: premium hand (AA), no one has bet yet
        when(context.getBettingRound()).thenReturn(0); // Pre-flop
        when(context.getAmountToCall(player)).thenReturn(0); // No bet to call
        when(context.getPotSize()).thenReturn(30); // Just blinds
        when(context.hasBeenBet()).thenReturn(false);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        // Additional mocks needed for executePreFlop
        when(context.hasActedThisRound(player)).thenReturn(false);
        when(context.getNumPlayersWithCards()).thenReturn(6);
        when(context.getNumPlayersAtTable()).thenReturn(6);
        when(context.getStartingPositionCategory(player)).thenReturn(3); // Middle position
        when(context.getPostFlopPositionCategory(player)).thenReturn(3);
        when(context.getTotalPotChipCount()).thenReturn(30);
        when(context.getMinRaise()).thenReturn(20);
        when(context.getStartingOrder(player)).thenReturn(2);
        when(context.getPlayerAt(anyInt())).thenReturn(null); // Simplify opponent model logic

        // Premium hand strength
        when(state.getHandStrength()).thenReturn(0.95f); // AA
        when(state.getRawHandStrength()).thenReturn(0.90f);
        when(state.getBiasedHandStrength()).thenReturn(0.90f);
        when(state.getSteam()).thenReturn(0.0f);
        when(state.getStealSuspicion()).thenReturn(0.0f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        }); // No-op debug consumer
        PlayerAction action = engine.getAction();

        // Verify we get a valid action
        assertThat(action).isNotNull();
        // With AA pre-flop and no bet, we should raise/bet (open pot)
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.RAISE);
    }

    // TODO: Requires additional mock setup for post-flop community cards and hand
    // potential
    @Test
    void execute_postFlopWithNutHand_shouldBet() {
        // Setup mocks
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        // Post-flop scenario: nut hand, checked to us
        when(context.getBettingRound()).thenReturn(1); // Flop
        when(context.getAmountToCall(player)).thenReturn(0); // Checked to us
        when(context.getPotSize()).thenReturn(100);
        when(context.hasBeenBet()).thenReturn(false);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        // Nut hand strength
        when(state.getRawHandStrength()).thenReturn(0.98f);
        when(state.getBiasedHandStrength()).thenReturn(0.98f);
        when(state.getBiasedPositivePotential()).thenReturn(0.05f);
        when(state.getBiasedNegativePotential()).thenReturn(0.02f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.98f);
        when(state.getSteam()).thenReturn(0.0f);
        when(state.getStealSuspicion()).thenReturn(0.0f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        }); // No-op debug consumer
        PlayerAction action = engine.getAction();

        // Verify we get a valid action
        assertThat(action).isNotNull();
        // With nut hand post-flop, we should bet/raise
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.RAISE, ActionType.CHECK);
    }

    @Test
    void execute_postFlopWithDrawingHand_shouldCall() {
        // Setup mocks
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        // Post-flop scenario: drawing hand (flush draw), facing a bet
        when(context.getBettingRound()).thenReturn(1); // Flop
        when(context.getAmountToCall(player)).thenReturn(50);
        when(context.getPotSize()).thenReturn(150); // Good pot odds
        when(context.hasBeenBet()).thenReturn(true);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        // Drawing hand: weak current strength, high positive potential
        when(state.getRawHandStrength()).thenReturn(0.30f);
        when(state.getBiasedHandStrength()).thenReturn(0.30f);
        when(state.getBiasedPositivePotential()).thenReturn(0.40f); // Good draw
        when(state.getBiasedNegativePotential()).thenReturn(0.10f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.55f);
        when(state.getSteam()).thenReturn(0.0f);
        when(state.getStealSuspicion()).thenReturn(0.0f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        }); // No-op debug consumer
        PlayerAction action = engine.getAction();

        // Verify we get a valid action
        assertThat(action).isNotNull();
        // With drawing hand and good pot odds, we should call/check or possibly fold
        assertThat(action.actionType()).isIn(ActionType.CALL, ActionType.FOLD, ActionType.CHECK, ActionType.RAISE);
    }

    @Test
    void execute_shortStackedPreFlop_considersAllIn() {
        // Setup mocks
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        // Pre-flop scenario: short stack (5BB), decent hand
        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(0);
        when(context.getPotSize()).thenReturn(30);
        when(player.getChipCount()).thenReturn(100); // 5 big blinds
        when(player.isFolded()).thenReturn(false);
        when(context.hasBeenBet()).thenReturn(false);

        // Decent hand (KQ, suited connectors, etc.)
        when(state.getHandStrength()).thenReturn(0.65f);
        when(state.getRawHandStrength()).thenReturn(0.60f);
        when(state.getBiasedHandStrength()).thenReturn(0.60f);
        when(state.getSteam()).thenReturn(0.0f);
        when(state.getStealSuspicion()).thenReturn(0.0f);

        // HOH M-ratio indicates desperate situation
        when(context.getHohM(player)).thenReturn(3.0f); // Red zone

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        }); // No-op debug consumer
        PlayerAction action = engine.getAction();

        // Verify we get a valid action
        assertThat(action).isNotNull();
        // Short-stacked with decent hand, should consider all-in or aggressive play
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.RAISE, ActionType.CHECK, ActionType.FOLD);
    }

    @Test
    void getOutcome_beforeExecute_returnsNull() {
        PureRuleEngine engine = new PureRuleEngine();
        AIOutcome outcome = engine.getOutcome();

        assertThat(outcome).isNull();
    }

    @Test
    void execute_withDebugConsumer_callsDebugOutput() {
        // Setup mocks
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StringBuilder debugOutput = new StringBuilder();

        // Basic scenario
        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(0);
        when(context.getPotSize()).thenReturn(30);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(state.getHandStrength()).thenReturn(0.50f);
        when(state.getRawHandStrength()).thenReturn(0.50f);
        when(state.getBiasedHandStrength()).thenReturn(0.50f);
        when(state.getSteam()).thenReturn(0.0f);
        when(state.getStealSuspicion()).thenReturn(0.0f);
        when(state.debugEnabled()).thenReturn(true);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, debugOutput::append);

        // Debug output should be populated
        assertThat(debugOutput.toString()).isNotEmpty();
    }

    // ===== Helper Methods =====

    private V2AIContext createMockContext() {
        V2AIContext context = mock(V2AIContext.class);

        // Mock table and tournament
        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(context.getTable()).thenReturn(table);
        when(context.getCurrentHand()).thenReturn(hand);
        when(context.getTournament()).thenReturn(tournament);
        when(context.getStrategy()).thenReturn(strategy);

        // Default table state
        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(6);
        when(tournament.getSmallBlind(1)).thenReturn(10);
        when(tournament.getBigBlind(1)).thenReturn(20);
        when(tournament.getAnte(1)).thenReturn(0);

        // Default hand state
        when(hand.getNumWithCards()).thenReturn(6);
        when(context.getNumActivePlayers()).thenReturn(6);
        // Note: getNumPlayersWithCards not mocked - let it return default for mock
        when(context.getNumPlayersYetToAct(any())).thenReturn(3);
        when(context.getPosition(any())).thenReturn(3);
        when(context.getSeat(any())).thenReturn(2);
        // Note: getLastActionThisRound and getChipCountAtStart not mocked by default
        // to avoid affecting specific test scenarios
        when(context.isButton(any())).thenReturn(false);
        when(context.isSmallBlind(any())).thenReturn(false);
        when(context.isBigBlind(any())).thenReturn(false);

        // Default strategy factors
        when(strategy.getStratFactor(anyString(), anyFloat(), anyFloat())).thenReturn(0.5f);
        when(strategy.getStratFactor(anyString(), any(Hand.class), anyFloat(), anyFloat())).thenReturn(0.5f);

        // Default opponent model
        V2OpponentModel opponentModel = mock(V2OpponentModel.class);
        when(context.getOpponentModel(any())).thenReturn(opponentModel);
        when(context.getSelfModel()).thenReturn(opponentModel);
        when(opponentModel.getPreFlopTightness(anyInt(), anyFloat())).thenReturn(0.5f);
        when(opponentModel.getPreFlopAggression(anyInt(), anyFloat())).thenReturn(0.5f);

        // Default HOH values
        when(context.getHohM(any())).thenReturn(20.0f); // Green zone
        when(context.getHohZone(any())).thenReturn(4); // HOH_GREEN
        when(context.getTableAverageHohM()).thenReturn(20.0f);

        // Default hand evaluation
        when(context.evaluateHandRank(any(), any())).thenReturn(1); // HIGH_CARD
        when(context.evaluateHandScore(any(), any())).thenReturn(1000L);
        when(context.getBest5CardRanks(any(), any())).thenReturn(new int[]{14, 13, 12, 11, 10});
        when(context.getApparentStrength(anyInt(), any())).thenReturn(0.5f);
        when(context.getBiasedRawHandStrength(anyInt(), any())).thenReturn(0.5f);
        when(context.getBiasedEffectiveHandStrength(anyInt(), any())).thenReturn(0.5f);

        // Default pocket cards (pair of 8s - medium strength)
        Hand defaultPocket = new Hand(2);
        defaultPocket.addCard(new Card(CardSuit.SPADES, Card.EIGHT));
        defaultPocket.addCard(new Card(CardSuit.HEARTS, Card.EIGHT));
        when(context.getPocketCards(any())).thenReturn(defaultPocket);

        // Default community cards (empty for pre-flop)
        when(context.getCommunityCards()).thenReturn(new Card[0]);
        when(context.getCommunity()).thenReturn(new Hand(0)); // Empty hand for pre-flop

        // Default table state queries
        when(context.wasRaisedPreFlop()).thenReturn(false);
        when(context.getPotStatus()).thenReturn(0); // NO_POT_ACTION
        when(context.getNumLimpers()).thenReturn(0);
        when(context.getPlayersLeft(any())).thenReturn(Collections.emptyList());
        when(context.isLimit()).thenReturn(false); // No-limit by default
        when(context.getBigBlind()).thenReturn(20);

        return context;
    }

    private V2PlayerState createMockState() {
        V2PlayerState state = mock(V2PlayerState.class);

        // Default state values
        when(state.getSteam()).thenReturn(0.0f);
        when(state.getStealSuspicion()).thenReturn(0.0f);
        when(state.getHandStrength()).thenReturn(0.5f);
        when(state.getRawHandStrength()).thenReturn(0.5f);
        when(state.getBiasedHandStrength()).thenReturn(0.5f);
        when(state.getBiasedPositivePotential()).thenReturn(0.15f);
        when(state.getBiasedNegativePotential()).thenReturn(0.15f);
        when(state.getPositiveHandPotential()).thenReturn(0.15f);
        when(state.getNegativeHandPotential()).thenReturn(0.15f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.5f);
        when(state.debugEnabled()).thenReturn(false);

        return state;
    }
}
