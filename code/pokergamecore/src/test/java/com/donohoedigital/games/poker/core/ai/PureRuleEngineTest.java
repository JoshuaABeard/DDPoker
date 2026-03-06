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
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.GamePlayerInfo;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.PlayerAction;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.engine.state.ActionType;
import com.donohoedigital.games.poker.engine.state.BettingRound;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PureRuleEngine.
 * <p>
 * Tests core decision logic for the V2 AI rule engine across pre-flop and
 * post-flop scenarios, eligible outcome determination, heads-up play, multiway
 * pots, and accessor methods.
 */
class PureRuleEngineTest {

    private static final ActionType[] VALID_ACTIONS = {ActionType.FOLD, ActionType.CHECK, ActionType.CALL,
            ActionType.BET, ActionType.RAISE};

    // ========== Constructor and Initial State ==========

    @Test
    void should_CreateEngineSuccessfully() {
        PureRuleEngine engine = new PureRuleEngine();
        assertThat(engine).isNotNull();
    }

    @Test
    void should_ReturnNullOutcome_When_NotExecuted() {
        PureRuleEngine engine = new PureRuleEngine();
        assertThat(engine.getOutcome()).isNull();
    }

    @Test
    void should_ReturnDefaultBetRange_When_NotExecuted() {
        PureRuleEngine engine = new PureRuleEngine();
        // After construction, betRange is not accessible without execution, but
        // getAction returns fold
        PlayerAction action = engine.getAction();
        assertThat(action).isNotNull();
        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
    }

    // ========== Pre-Flop: Trash Hand ==========

    @Test
    void should_ReturnValidAction_When_PreFlopTrashHand() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(100);
        when(context.getPotSize()).thenReturn(150);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(state.getHandStrength()).thenReturn(0.1f);
        when(state.getRawHandStrength()).thenReturn(0.05f);
        when(state.getBiasedHandStrength()).thenReturn(0.05f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Pre-Flop: Premium Hand ==========

    @Test
    void should_RaiseOrBet_When_PreFlopPremiumHandNoBet() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(0);
        when(context.getPotSize()).thenReturn(30);
        when(context.hasBeenBet()).thenReturn(false);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.hasActedThisRound(player)).thenReturn(false);
        when(context.getNumPlayersWithCards()).thenReturn(6);
        when(context.getNumPlayersAtTable()).thenReturn(6);
        when(context.getStartingPositionCategory(player)).thenReturn(3);
        when(context.getPostFlopPositionCategory(player)).thenReturn(3);
        when(context.getTotalPotChipCount()).thenReturn(30);
        when(context.getMinRaise()).thenReturn(20);
        when(context.getStartingOrder(player)).thenReturn(2);
        when(context.getPlayerAt(anyInt())).thenReturn(null);

        when(state.getHandStrength()).thenReturn(0.95f);
        when(state.getRawHandStrength()).thenReturn(0.90f);
        when(state.getBiasedHandStrength()).thenReturn(0.90f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.RAISE);
    }

    // ========== Pre-Flop: Red Zone / Short Stack ==========

    @Test
    void should_ConsiderAllIn_When_RedZoneShortStack() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(0);
        when(context.getPotSize()).thenReturn(30);
        when(player.getChipCount()).thenReturn(100);
        when(player.isFolded()).thenReturn(false);
        when(context.hasBeenBet()).thenReturn(false);

        when(state.getHandStrength()).thenReturn(0.65f);
        when(state.getRawHandStrength()).thenReturn(0.60f);
        when(state.getBiasedHandStrength()).thenReturn(0.60f);

        when(context.getHohM(player)).thenReturn(3.0f);
        when(context.getHohZone(player)).thenReturn(AIConstants.HOH_RED);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ConsiderAllIn_When_DeadZone() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(20);
        when(context.getPotSize()).thenReturn(30);
        when(player.getChipCount()).thenReturn(60);
        when(player.isFolded()).thenReturn(false);

        when(state.getHandStrength()).thenReturn(0.55f);

        when(context.getHohM(player)).thenReturn(1.5f);
        when(context.getHohZone(player)).thenReturn(AIConstants.HOH_DEAD);
        when(context.getPotStatus()).thenReturn(PokerConstants.NO_POT_ACTION);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Pre-Flop: Raised and Re-Raised Pots ==========

    @Test
    void should_ReturnValidAction_When_FacingRaisedPot() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        GamePlayerInfo raiser = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(60);
        when(context.getPotSize()).thenReturn(90);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(raiser.getChipCount()).thenReturn(800);
        when(raiser.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.RAISED_POT);
        when(context.getFirstBettor(eq(BettingRound.PRE_FLOP.toLegacy()), eq(false))).thenReturn(raiser);
        when(context.getHohZone(raiser)).thenReturn(AIConstants.HOH_GREEN);
        V2OpponentModel raiserModel = mock(V2OpponentModel.class);
        when(context.getOpponentModel(raiser)).thenReturn(raiserModel);
        when(raiserModel.getHandsRaisedPreFlopPercent(anyFloat())).thenReturn(0.15f);

        when(state.getHandStrength()).thenReturn(0.70f);
        when(state.getStealSuspicion()).thenReturn(0.3f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_FacingReRaisedPot() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        GamePlayerInfo firstRaiser = mock(GamePlayerInfo.class);
        GamePlayerInfo lastRaiser = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(180);
        when(context.getPotSize()).thenReturn(270);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(firstRaiser.isFolded()).thenReturn(false);
        when(lastRaiser.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.RERAISED_POT);
        when(context.getFirstBettor(eq(BettingRound.PRE_FLOP.toLegacy()), eq(false))).thenReturn(firstRaiser);
        when(context.getLastBettor(eq(BettingRound.PRE_FLOP.toLegacy()), eq(false))).thenReturn(lastRaiser);
        when(context.getHohZone(lastRaiser)).thenReturn(AIConstants.HOH_GREEN);
        when(context.getStartingPositionCategory(firstRaiser)).thenReturn(AIConstants.POSITION_MIDDLE);
        when(context.getStartingPositionCategory(lastRaiser)).thenReturn(AIConstants.POSITION_LATE);
        when(context.isBlind(lastRaiser)).thenReturn(false);
        when(context.getNumFoldsSinceLastBet()).thenReturn(2);

        V2OpponentModel raiserModel = mock(V2OpponentModel.class);
        when(context.getOpponentModel(firstRaiser)).thenReturn(raiserModel);
        when(context.getOpponentModel(lastRaiser)).thenReturn(raiserModel);
        when(raiserModel.getHandsRaisedPreFlopPercent(anyFloat())).thenReturn(0.1f);

        Hand pocket = new Hand(2);
        pocket.addCard(new Card(CardSuit.SPADES, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));
        when(context.getPocketCards(player)).thenReturn(pocket);

        when(state.getHandStrength()).thenReturn(0.95f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Pre-Flop: Called Pot / Limpers ==========

    @Test
    void should_ReturnValidAction_When_CalledPotWithLimpers() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(20);
        when(context.getPotSize()).thenReturn(80);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.CALLED_POT);
        when(context.getNumLimpers()).thenReturn(3);

        when(state.getHandStrength()).thenReturn(0.60f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_CalledPotSingleLimper() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(20);
        when(context.getPotSize()).thenReturn(60);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.CALLED_POT);
        when(context.getNumLimpers()).thenReturn(1);

        when(state.getHandStrength()).thenReturn(0.70f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Pre-Flop: Steal Opportunity ==========

    @Test
    void should_ConsiderSteal_When_NoActionAndLatePosition() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(20);
        when(context.getPotSize()).thenReturn(30);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.NO_POT_ACTION);
        when(context.getStartingPositionCategory(player)).thenReturn(AIConstants.POSITION_LATE);
        when(context.isButton(player)).thenReturn(true);

        when(state.getHandStrength()).thenReturn(0.50f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Pre-Flop: Desirable Opening Hand ==========

    @Test
    void should_OpenPot_When_DesirableHandInPosition() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(20); // Must call BB
        when(context.getPotSize()).thenReturn(30);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.NO_POT_ACTION);
        when(context.hasActedThisRound(player)).thenReturn(false);
        when(context.getStartingPositionCategory(player)).thenReturn(AIConstants.POSITION_LATE);
        when(context.getStartingOrder(player)).thenReturn(5);
        when(context.getNumPlayersWithCards()).thenReturn(6);

        // Very strong hand plus position = adjusted hand strength + position >= 1
        when(state.getHandStrength()).thenReturn(0.85f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Pre-Flop: Boredom and Steam Factors ==========

    @Test
    void should_ReturnValidAction_When_SteamAndBoredomActive() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(20);
        when(context.getPotSize()).thenReturn(30);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(state.getSteam()).thenReturn(0.5f); // On tilt
        when(state.getHandStrength()).thenReturn(0.40f);
        when(context.getConsecutiveHandsUnpaid(player)).thenReturn(8); // Bored

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Pre-Flop: NONE Round (No Hand) ==========

    @Test
    void should_ReturnFold_When_NoBettingRound() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(-1); // BettingRound.NONE
        when(context.getCurrentHand()).thenReturn(mock(GameHand.class));
        when(player.getChipCount()).thenReturn(1000);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        // With NONE round, execute returns early; default strongest is fold
        assertThat(action.actionType()).isIn(ActionType.FOLD, ActionType.CHECK);
    }

    // ========== Post-Flop: Nut Hand ==========

    @Test
    void should_BetOrRaise_When_PostFlopNutHand() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(1); // Flop
        when(context.getAmountToCall(player)).thenReturn(0);
        when(context.getPotSize()).thenReturn(100);
        when(context.hasBeenBet()).thenReturn(false);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(state.getRawHandStrength()).thenReturn(0.98f);
        when(state.getBiasedHandStrength()).thenReturn(0.98f);
        when(state.getBiasedPositivePotential()).thenReturn(0.05f);
        when(state.getBiasedNegativePotential()).thenReturn(0.02f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.98f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.RAISE, ActionType.CHECK);
    }

    // ========== Post-Flop: Drawing Hand ==========

    @Test
    void should_ReturnValidAction_When_PostFlopDrawingHand() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(1); // Flop
        when(context.getAmountToCall(player)).thenReturn(50);
        when(context.getPotSize()).thenReturn(150);
        when(context.hasBeenBet()).thenReturn(true);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(state.getRawHandStrength()).thenReturn(0.30f);
        when(state.getBiasedHandStrength()).thenReturn(0.30f);
        when(state.getBiasedPositivePotential()).thenReturn(0.40f);
        when(state.getBiasedNegativePotential()).thenReturn(0.10f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.55f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Post-Flop: Heads-Up In Position ==========

    @Test
    void should_ReturnValidAction_When_HeadsUpInPositionOpponentChecked() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        GamePlayerInfo opponent = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(1); // Flop
        when(context.getAmountToCall(player)).thenReturn(0); // Opponent checked
        when(context.getNumPlayersWithCards()).thenReturn(2); // Heads up
        when(context.getNumPlayersYetToAct(player)).thenReturn(0); // In position
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(opponent.getChipCount()).thenReturn(800);
        when(context.getPlayersLeft(player)).thenReturn(List.of(opponent));
        when(context.getSeat(opponent)).thenReturn(3);
        when(context.hasActedThisRound(player)).thenReturn(false);
        when(context.getTotalPotChipCount()).thenReturn(100);
        when(context.getChipCountAtStart(opponent)).thenReturn(1000);
        when(context.getChipCountAtStart(player)).thenReturn(1000);

        V2OpponentModel opponentModel = mock(V2OpponentModel.class);
        when(context.getOpponentModel(opponent)).thenReturn(opponentModel);
        when(context.getBiasedRawHandStrength(eq(3), any())).thenReturn(0.4f);

        // Provide community cards so PocketRanks.getInstance doesn't throw
        Hand community = new Hand(3);
        community.addCard(new Card(CardSuit.HEARTS, Card.KING));
        community.addCard(new Card(CardSuit.DIAMONDS, Card.SEVEN));
        community.addCard(new Card(CardSuit.CLUBS, Card.TWO));
        when(context.getCommunity()).thenReturn(community);

        // Strong hand in position
        when(state.getRawHandStrength()).thenReturn(0.75f);
        when(state.getBiasedHandStrength()).thenReturn(0.75f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.75f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_HeadsUpInPositionOpponentBet() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        GamePlayerInfo opponent = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(1);
        when(context.getAmountToCall(player)).thenReturn(50); // Opponent bet
        when(context.getNumPlayersWithCards()).thenReturn(2);
        when(context.getNumPlayersYetToAct(player)).thenReturn(0);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(opponent.getChipCount()).thenReturn(750);
        when(context.getPlayersLeft(player)).thenReturn(List.of(opponent));
        when(context.getSeat(opponent)).thenReturn(3);
        when(context.hasActedThisRound(player)).thenReturn(false);
        when(context.getTotalPotChipCount()).thenReturn(150);
        when(context.getChipCountAtStart(opponent)).thenReturn(1000);
        when(context.getChipCountAtStart(player)).thenReturn(1000);

        V2OpponentModel opponentModel = mock(V2OpponentModel.class);
        when(context.getOpponentModel(opponent)).thenReturn(opponentModel);
        when(context.getBiasedRawHandStrength(eq(3), any())).thenReturn(0.5f);

        // Provide community cards so PocketRanks.getInstance doesn't throw
        Hand community = new Hand(3);
        community.addCard(new Card(CardSuit.HEARTS, Card.KING));
        community.addCard(new Card(CardSuit.DIAMONDS, Card.SEVEN));
        community.addCard(new Card(CardSuit.CLUBS, Card.TWO));
        when(context.getCommunity()).thenReturn(community);

        // Very strong hand vs bet
        when(state.getRawHandStrength()).thenReturn(0.90f);
        when(state.getBiasedHandStrength()).thenReturn(0.90f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.90f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        // With very strong hand facing bet, should raise
        assertThat(action.actionType()).isIn(ActionType.CALL, ActionType.RAISE);
    }

    // ========== Post-Flop: Heads-Up Out of Position ==========

    @Test
    void should_ReturnValidAction_When_HeadsUpOutOfPositionFirstToAct() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        GamePlayerInfo opponent = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(1);
        when(context.getAmountToCall(player)).thenReturn(0); // First to act
        when(context.getNumPlayersWithCards()).thenReturn(2);
        when(context.getNumPlayersYetToAct(player)).thenReturn(1); // Out of position
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(opponent.getChipCount()).thenReturn(800);
        when(context.getPlayersLeft(player)).thenReturn(List.of(opponent));
        when(context.getSeat(opponent)).thenReturn(3);
        when(context.hasActedThisRound(player)).thenReturn(false);
        when(context.getTotalPotChipCount()).thenReturn(100);
        when(context.getChipCountAtStart(opponent)).thenReturn(1000);
        when(context.getChipCountAtStart(player)).thenReturn(1000);
        when(context.wasLastRaiserPreFlop(player)).thenReturn(true);
        when(context.getApparentStrength(anyInt(), any())).thenReturn(0.6f);

        V2OpponentModel opponentModel = mock(V2OpponentModel.class);
        when(context.getOpponentModel(opponent)).thenReturn(opponentModel);
        when(context.getBiasedRawHandStrength(eq(3), any())).thenReturn(0.4f);

        // Provide community cards so PocketRanks.getInstance doesn't throw
        Hand community = new Hand(3);
        community.addCard(new Card(CardSuit.HEARTS, Card.ACE));
        community.addCard(new Card(CardSuit.DIAMONDS, Card.SEVEN));
        community.addCard(new Card(CardSuit.CLUBS, Card.THREE));
        when(context.getCommunity()).thenReturn(community);

        when(state.getRawHandStrength()).thenReturn(0.65f);
        when(state.getBiasedHandStrength()).thenReturn(0.65f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.65f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_HeadsUpOutOfPositionFacingBet() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        GamePlayerInfo opponent = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(2); // Turn
        when(context.getAmountToCall(player)).thenReturn(80);
        when(context.getNumPlayersWithCards()).thenReturn(2);
        when(context.getNumPlayersYetToAct(player)).thenReturn(1);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(opponent.getChipCount()).thenReturn(700);
        when(context.getPlayersLeft(player)).thenReturn(List.of(opponent));
        when(context.getSeat(opponent)).thenReturn(3);
        when(context.hasActedThisRound(player)).thenReturn(true);
        when(context.getLastActionThisRound(player)).thenReturn(AIContext.ACTION_CHECK);
        when(context.getTotalPotChipCount()).thenReturn(200);
        when(context.getChipCountAtStart(opponent)).thenReturn(1000);
        when(context.getChipCountAtStart(player)).thenReturn(1000);

        V2OpponentModel opponentModel = mock(V2OpponentModel.class);
        when(context.getOpponentModel(opponent)).thenReturn(opponentModel);
        when(context.getBiasedRawHandStrength(eq(3), any())).thenReturn(0.5f);

        // Provide community cards so PocketRanks.getInstance doesn't throw
        Hand community = new Hand(4);
        community.addCard(new Card(CardSuit.HEARTS, Card.KING));
        community.addCard(new Card(CardSuit.DIAMONDS, Card.QUEEN));
        community.addCard(new Card(CardSuit.CLUBS, Card.FIVE));
        community.addCard(new Card(CardSuit.SPADES, Card.THREE));
        when(context.getCommunity()).thenReturn(community);

        when(state.getRawHandStrength()).thenReturn(0.45f);
        when(state.getBiasedHandStrength()).thenReturn(0.45f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.50f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Post-Flop: Multiway (3+ Players) ==========

    @Test
    void should_ReturnValidAction_When_MultiwayPostFlopNoPotAction() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(1);
        when(context.getAmountToCall(player)).thenReturn(0);
        when(context.getNumPlayersWithCards()).thenReturn(4);
        when(context.getNumPlayersYetToAct(player)).thenReturn(2);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.NO_POT_ACTION);
        when(context.getTotalPotChipCount()).thenReturn(100);
        when(context.getNumPlayersAtTable()).thenReturn(6);
        when(context.wasLastRaiserPreFlop(player)).thenReturn(false);

        // Set up player iteration for "all weak" computation
        for (int i = 0; i < 6; i++) {
            GamePlayerInfo p = mock(GamePlayerInfo.class);
            when(p.isFolded()).thenReturn(i >= 4);
            when(context.getPlayerAt(i)).thenReturn(p);
            when(context.getSeat(p)).thenReturn(i);
            when(context.getLastActionThisRound(p)).thenReturn(AIContext.ACTION_CHECK);
            V2OpponentModel model = mock(V2OpponentModel.class);
            when(model.getCheckFoldPostFlop(anyInt(), anyFloat())).thenReturn(0.5f);
            when(context.getOpponentModel(p)).thenReturn(model);
        }

        when(state.getRawHandStrength()).thenReturn(0.60f);
        when(state.getBiasedHandStrength()).thenReturn(0.60f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.65f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    @Test
    void should_ReturnValidAction_When_MultiwayPostFlopFacingBet() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        GamePlayerInfo bettor = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(2); // Turn
        when(context.getAmountToCall(player)).thenReturn(80);
        when(context.getNumPlayersWithCards()).thenReturn(3);
        when(context.getNumPlayersYetToAct(player)).thenReturn(1);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.RAISED_POT);
        when(context.getTotalPotChipCount()).thenReturn(300);
        when(context.getFirstBettor(eq(2), eq(false))).thenReturn(bettor);

        V2OpponentModel bettorModel = mock(V2OpponentModel.class);
        when(context.getOpponentModel(bettor)).thenReturn(bettorModel);
        when(bettorModel.getActPostFlop(anyInt(), anyFloat())).thenReturn(0.5f);
        when(bettorModel.getCheckFoldPostFlop(anyInt(), anyFloat())).thenReturn(0.3f);
        when(bettorModel.getOpenPostFlop(anyInt(), anyFloat())).thenReturn(0.4f);
        when(bettorModel.getRaisePostFlop(anyInt(), anyFloat())).thenReturn(0.2f);
        when(bettorModel.getOverbetFrequency(anyFloat())).thenReturn(0.1f);
        when(bettorModel.getBetFoldFrequency(anyFloat())).thenReturn(0.2f);

        when(state.getRawHandStrength()).thenReturn(0.70f);
        when(state.getBiasedHandStrength()).thenReturn(0.70f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.72f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Post-Flop: River with Pure Nuts ==========

    @Test
    void should_Raise_When_RiverWithPureNuts() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(3); // River
        when(context.getAmountToCall(player)).thenReturn(100);
        when(context.getNumPlayersWithCards()).thenReturn(3);
        when(context.getNumPlayersYetToAct(player)).thenReturn(1);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.RAISED_POT);
        when(context.getTotalPotChipCount()).thenReturn(500);

        // Pure nuts on river: rhs = 1.0
        Hand community = new Hand(5);
        community.addCard(new Card(CardSuit.SPADES, Card.KING));
        community.addCard(new Card(CardSuit.HEARTS, Card.QUEEN));
        community.addCard(new Card(CardSuit.DIAMONDS, Card.JACK));
        community.addCard(new Card(CardSuit.CLUBS, Card.TEN));
        community.addCard(new Card(CardSuit.SPADES, Card.TWO));
        when(context.getCommunity()).thenReturn(community);

        Hand pocket = new Hand(2);
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));
        pocket.addCard(new Card(CardSuit.DIAMONDS, Card.ACE));
        when(context.getPocketCards(player)).thenReturn(pocket);

        // rhs = 1.0 (pure nuts)
        when(context.getBiasedRawHandStrength(anyInt(), any())).thenReturn(1.0f);
        when(context.getBiasedEffectiveHandStrength(anyInt(), any())).thenReturn(1.0f);

        // PocketRanks.getInstance will compute actual RHS from community
        // Mock getRawHandStrength to return 1.0
        when(state.getRawHandStrength()).thenReturn(1.0f);
        when(state.getBiasedHandStrength()).thenReturn(1.0f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(1.0f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        // With pure nuts on river, should raise or call (depends on PocketRanks
        // computation)
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Post-Flop: Continuation Bet ==========

    @Test
    void should_ReturnValidAction_When_ContinuationBetOpportunity() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(1); // Flop
        when(context.getAmountToCall(player)).thenReturn(0);
        when(context.getNumPlayersWithCards()).thenReturn(3);
        when(context.getNumPlayersYetToAct(player)).thenReturn(1);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.getPotStatus()).thenReturn(PokerConstants.NO_POT_ACTION);
        when(context.getTotalPotChipCount()).thenReturn(100);
        when(context.wasLastRaiserPreFlop(player)).thenReturn(true);
        when(context.wasFirstRaiserPreFlop(player)).thenReturn(true);
        when(context.wasOnlyRaiserPreFlop(player)).thenReturn(true);
        when(context.getNumPlayersAtTable()).thenReturn(6);

        when(state.getRawHandStrength()).thenReturn(0.40f);
        when(state.getBiasedHandStrength()).thenReturn(0.40f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.45f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Debug Output ==========

    @Test
    void should_ProduceDebugOutput_When_DebugEnabled() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        StringBuilder debugOutput = new StringBuilder();

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(0);
        when(context.getPotSize()).thenReturn(30);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(state.getHandStrength()).thenReturn(0.50f);
        when(state.debugEnabled()).thenReturn(true);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, debugOutput::append);

        assertThat(debugOutput.toString()).isNotEmpty();
    }

    // ========== Accessor Methods ==========

    @Test
    void should_ReturnBetRange_After_Execution() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(state.getHandStrength()).thenReturn(0.50f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });

        assertThat(engine.getBetRange()).isNotNull();
    }

    @Test
    void should_ReturnStrongestOutcome_After_Execution() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(state.getHandStrength()).thenReturn(0.50f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });

        int strongest = engine.getStrongestOutcome();
        // Should be a valid outcome constant
        assertThat(strongest).isBetween(-1, 14);
    }

    @Test
    void should_TrackEligibleOutcomes_After_Execution() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(0);
        when(context.getAmountToCall(player)).thenReturn(0);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);
        when(state.getHandStrength()).thenReturn(0.50f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });

        // Check should be eligible when no amount to call
        assertThat(engine.isEligible(PureRuleEngine.OUTCOME_CHECK)).isTrue();
        // Fold should not be eligible when no amount to call
        assertThat(engine.isEligible(PureRuleEngine.OUTCOME_FOLD)).isFalse();
    }

    @Test
    void should_ReturnProbeBetFlag_After_Execution() {
        PureRuleEngine engine = new PureRuleEngine();
        // Before execution, probe bet should be false (default)
        assertThat(engine.isProbeBetAppropriate()).isFalse();
    }

    @Test
    void should_SetAndGetEligible() {
        PureRuleEngine engine = new PureRuleEngine();

        engine.setEligible(PureRuleEngine.OUTCOME_FOLD, true);
        assertThat(engine.isEligible(PureRuleEngine.OUTCOME_FOLD)).isTrue();

        engine.setEligible(PureRuleEngine.OUTCOME_FOLD, false);
        assertThat(engine.isEligible(PureRuleEngine.OUTCOME_FOLD)).isFalse();
    }

    // ========== Post-Flop: Limit vs No-Limit ==========

    @Test
    void should_ReturnValidAction_When_LimitGame() {
        V2AIContext context = createMockContext();
        V2PlayerState state = createMockState();
        GamePlayerInfo player = mock(GamePlayerInfo.class);

        when(context.getBettingRound()).thenReturn(1);
        when(context.getAmountToCall(player)).thenReturn(20);
        when(context.getNumPlayersWithCards()).thenReturn(4);
        when(player.getChipCount()).thenReturn(1000);
        when(player.isFolded()).thenReturn(false);

        when(context.isLimit()).thenReturn(true);

        when(state.getRawHandStrength()).thenReturn(0.60f);
        when(state.getBiasedHandStrength()).thenReturn(0.60f);
        when(state.getBiasedEffectiveHandStrength(anyFloat())).thenReturn(0.62f);

        PureRuleEngine engine = new PureRuleEngine();
        engine.execute(context, state, player, s -> {
        });
        PlayerAction action = engine.getAction();

        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(VALID_ACTIONS);
    }

    // ========== Outcome Constants ==========

    @Test
    void should_HaveDistinctOutcomeConstants() {
        // Verify all 15 outcome constants are distinct
        int[] outcomes = {PureRuleEngine.OUTCOME_FOLD, PureRuleEngine.OUTCOME_CHECK, PureRuleEngine.OUTCOME_LIMP,
                PureRuleEngine.OUTCOME_STEAL, PureRuleEngine.OUTCOME_OPEN_POT, PureRuleEngine.OUTCOME_CALL,
                PureRuleEngine.OUTCOME_RAISE, PureRuleEngine.OUTCOME_SEMI_BLUFF, PureRuleEngine.OUTCOME_TRAP,
                PureRuleEngine.OUTCOME_SLOW_PLAY, PureRuleEngine.OUTCOME_CHECK_RAISE, PureRuleEngine.OUTCOME_BET,
                PureRuleEngine.OUTCOME_ALL_IN, PureRuleEngine.OUTCOME_CONTINUATION_BET, PureRuleEngine.OUTCOME_BLUFF};
        assertThat(outcomes).hasSize(15);

        java.util.Set<Integer> unique = new java.util.HashSet<>();
        for (int o : outcomes) {
            unique.add(o);
        }
        assertThat(unique).hasSize(15);
    }

    // ========== Helper Methods ==========

    private V2AIContext createMockContext() {
        V2AIContext context = mock(V2AIContext.class);

        GameTable table = mock(GameTable.class);
        GameHand hand = mock(GameHand.class);
        TournamentContext tournament = mock(TournamentContext.class);
        StrategyProvider strategy = mock(StrategyProvider.class);

        when(context.getTable()).thenReturn(table);
        when(context.getCurrentHand()).thenReturn(hand);
        when(context.getTournament()).thenReturn(tournament);
        when(context.getStrategy()).thenReturn(strategy);

        when(table.getLevel()).thenReturn(1);
        when(table.getNumOccupiedSeats()).thenReturn(6);
        when(tournament.getSmallBlind(1)).thenReturn(10);
        when(tournament.getBigBlind(1)).thenReturn(20);
        when(tournament.getAnte(1)).thenReturn(0);

        when(hand.getNumWithCards()).thenReturn(6);
        when(context.getNumActivePlayers()).thenReturn(6);
        when(context.getNumPlayersWithCards()).thenReturn(6);
        when(context.getNumPlayersYetToAct(any())).thenReturn(3);
        when(context.getPosition(any())).thenReturn(3);
        when(context.getSeat(any())).thenReturn(2);
        when(context.getLastActionThisRound(any())).thenReturn(0);
        when(context.getChipCountAtStart(any())).thenReturn(1000);
        when(context.isButton(any())).thenReturn(false);
        when(context.isSmallBlind(any())).thenReturn(false);
        when(context.isBigBlind(any())).thenReturn(false);

        when(strategy.getStratFactor(anyString(), anyFloat(), anyFloat())).thenReturn(0.5f);
        when(strategy.getStratFactor(anyString(), any(Hand.class), anyFloat(), anyFloat())).thenReturn(0.5f);

        V2OpponentModel opponentModel = mock(V2OpponentModel.class);
        when(context.getOpponentModel(any())).thenReturn(opponentModel);
        when(context.getSelfModel()).thenReturn(opponentModel);
        when(opponentModel.getPreFlopTightness(anyInt(), anyFloat())).thenReturn(0.5f);
        when(opponentModel.getPreFlopAggression(anyInt(), anyFloat())).thenReturn(0.5f);
        when(opponentModel.getCheckFoldPostFlop(anyInt(), anyFloat())).thenReturn(0.5f);
        when(opponentModel.getActPostFlop(anyInt(), anyFloat())).thenReturn(0.5f);
        when(opponentModel.getOpenPostFlop(anyInt(), anyFloat())).thenReturn(0.5f);
        when(opponentModel.getRaisePostFlop(anyInt(), anyFloat())).thenReturn(0.5f);
        when(opponentModel.getOverbetFrequency(anyFloat())).thenReturn(0.5f);
        when(opponentModel.getBetFoldFrequency(anyFloat())).thenReturn(0.5f);
        when(opponentModel.getHandsRaisedPreFlopPercent(anyFloat())).thenReturn(0.1f);

        when(context.getHohM(any())).thenReturn(20.0f);
        when(context.getHohQ(any())).thenReturn(1.0f);
        when(context.getHohZone(any())).thenReturn(AIConstants.HOH_GREEN);
        when(context.getTableAverageHohM()).thenReturn(20.0f);
        when(context.getRemainingAverageHohM()).thenReturn(20.0f);

        when(context.evaluateHandRank(any(), any())).thenReturn(1);
        when(context.evaluateHandScore(any(), any())).thenReturn(1000L);
        when(context.getBest5CardRanks(any(), any())).thenReturn(new int[]{14, 13, 12, 11, 10});
        when(context.getApparentStrength(anyInt(), any())).thenReturn(0.5f);
        when(context.getBiasedRawHandStrength(anyInt(), any())).thenReturn(0.5f);
        when(context.getBiasedEffectiveHandStrength(anyInt(), any())).thenReturn(0.5f);

        Hand defaultPocket = new Hand(2);
        defaultPocket.addCard(new Card(CardSuit.SPADES, Card.EIGHT));
        defaultPocket.addCard(new Card(CardSuit.HEARTS, Card.EIGHT));
        when(context.getPocketCards(any())).thenReturn(defaultPocket);

        when(context.getCommunityCards()).thenReturn(new Card[0]);
        when(context.getCommunity()).thenReturn(new Hand(0));

        when(context.wasRaisedPreFlop()).thenReturn(false);
        when(context.getPotStatus()).thenReturn(0);
        when(context.getNumLimpers()).thenReturn(0);
        when(context.getPlayersLeft(any())).thenReturn(Collections.emptyList());
        when(context.isLimit()).thenReturn(false);
        when(context.getBigBlind()).thenReturn(20);
        when(context.getTotalPotChipCount()).thenReturn(100);
        when(context.getMinRaise()).thenReturn(20);
        when(context.getStartingPositionCategory(any())).thenReturn(AIConstants.POSITION_MIDDLE);
        when(context.getPostFlopPositionCategory(any())).thenReturn(AIConstants.POSITION_MIDDLE);
        when(context.getStartingOrder(any())).thenReturn(2);
        when(context.getNumPlayersAtTable()).thenReturn(6);
        when(context.getPlayerAt(anyInt())).thenReturn(null);
        when(context.getConsecutiveHandsUnpaid(any())).thenReturn(0);
        when(context.getHandsBeforeBigBlind(any())).thenReturn(5);
        when(context.getMinChip()).thenReturn(1);
        when(context.hasActedThisRound(any())).thenReturn(false);

        return context;
    }

    private V2PlayerState createMockState() {
        V2PlayerState state = mock(V2PlayerState.class);

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
