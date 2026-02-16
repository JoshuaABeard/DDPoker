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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.ai.AIConstants;
import com.donohoedigital.games.poker.core.ai.StrategyProvider;
import com.donohoedigital.games.poker.core.ai.V2OpponentModel;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.PokerConstants;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test utilities for creating server-side test objects.
 */
public class ServerTestUtils {

    /**
     * Create a pre-flop context for testing.
     *
     * @param position
     *            starting position category (0-5)
     * @param pocket
     *            pocket cards
     * @param potStatus
     *            pot status (0=NO_POT_ACTION, 1=CALLED_POT, 2=RAISED_POT, etc.)
     * @param amountToCall
     *            chips needed to call
     * @return configured ServerV2AIContext
     */
    public static ServerV2AIContext createPreFlopContext(int position, Hand pocket, int potStatus, int amountToCall) {
        ServerV2AIContext context = createBasicContext();

        // Pre-flop specific setup
        when(context.getBettingRound()).thenReturn(BettingRound.PRE_FLOP.toLegacy());
        when(context.getPocketCards(Mockito.any())).thenReturn(pocket);
        when(context.getCommunity()).thenReturn(new Hand(0)); // No community cards pre-flop
        when(context.getCommunityCards()).thenReturn(new Card[0]);

        // Position and pot state
        when(context.getStartingPositionCategory(Mockito.any())).thenReturn(position);
        when(context.getPostFlopPositionCategory(Mockito.any())).thenReturn(position);
        when(context.getPotStatus()).thenReturn(potStatus);
        when(context.getAmountToCall(Mockito.any())).thenReturn(amountToCall);

        // Adjust other mocks based on pot status
        boolean hasBeenBet = (potStatus == PokerConstants.RAISED_POT || potStatus == PokerConstants.RERAISED_POT);
        when(context.hasBeenBet()).thenReturn(hasBeenBet);

        return context;
    }

    /**
     * Create a post-flop context for testing.
     *
     * @param round
     *            betting round (1=flop, 2=turn, 3=river)
     * @param pocket
     *            pocket cards
     * @param community
     *            community cards
     * @param amountToCall
     *            chips needed to call
     * @return configured ServerV2AIContext
     */
    public static ServerV2AIContext createPostFlopContext(int round, Hand pocket, Hand community, int amountToCall) {
        ServerV2AIContext context = createBasicContext();

        // Post-flop specific setup
        when(context.getBettingRound()).thenReturn(round);
        when(context.getPocketCards(Mockito.any())).thenReturn(pocket);
        when(context.getCommunity()).thenReturn(community);
        when(context.getCommunityCards()).thenReturn(handToArray(community));
        when(context.getAmountToCall(Mockito.any())).thenReturn(amountToCall);

        // Post-flop defaults
        when(context.hasBeenBet()).thenReturn(amountToCall > 0);
        when(context.getPotStatus())
                .thenReturn(amountToCall > 0 ? PokerConstants.RAISED_POT : PokerConstants.NO_POT_ACTION);

        return context;
    }

    /**
     * Create a basic context with default values.
     */
    private static ServerV2AIContext createBasicContext() {
        ServerV2AIContext context = mock(ServerV2AIContext.class);

        // Strategy provider mock with default values
        StrategyProvider strategy = mock(StrategyProvider.class);
        when(strategy.getStratFactor(any(String.class), anyFloat(), anyFloat())).thenReturn(1.0f);
        when(strategy.getStratFactor(any(String.class), any(Hand.class), anyFloat(), anyFloat())).thenReturn(0.5f);
        when(strategy.getHandStrength(any(Hand.class))).thenReturn(0.5f);
        when(context.getStrategy()).thenReturn(strategy);

        // Opponent model mocks
        V2OpponentModel opponentModel = mock(V2OpponentModel.class);
        when(opponentModel.getHandsRaisedPreFlopPercent(anyFloat())).thenReturn(0.2f);
        when(opponentModel.getPreFlopTightness(anyInt(), anyFloat())).thenReturn(0.5f);
        when(opponentModel.getPreFlopAggression(anyInt(), anyFloat())).thenReturn(0.5f);
        when(context.getOpponentModel(any())).thenReturn(opponentModel);
        when(context.getSelfModel()).thenReturn(opponentModel);

        // Game state defaults
        when(context.getNumPlayersWithCards()).thenReturn(6);
        when(context.getNumPlayersAtTable()).thenReturn(6);
        when(context.getNumActivePlayers()).thenReturn(6);
        when(context.getNumLimpers()).thenReturn(0);
        when(context.isLimit()).thenReturn(false);

        // Tournament state defaults (green zone)
        when(context.getHohM(Mockito.any())).thenReturn(15.0f);
        when(context.getHohQ(Mockito.any())).thenReturn(1.0f);
        when(context.getHohZone(Mockito.any())).thenReturn(AIConstants.HOH_GREEN);
        when(context.getTableAverageHohM()).thenReturn(15.0f);
        when(context.getRemainingAverageHohM()).thenReturn(15.0f);

        // Blinds and pot
        when(context.getBigBlind()).thenReturn(20);
        when(context.getPotSize()).thenReturn(30);
        when(context.getTotalPotChipCount()).thenReturn(30);
        when(context.getMinRaise()).thenReturn(20);

        // Player state defaults
        when(context.hasActedThisRound(Mockito.any())).thenReturn(false);
        when(context.getStartingOrder(Mockito.any())).thenReturn(2);
        when(context.getSeat(Mockito.any())).thenReturn(2);
        when(context.getHandsBeforeBigBlind(Mockito.any())).thenReturn(5);
        when(context.getConsecutiveHandsUnpaid(Mockito.any())).thenReturn(0);
        when(context.getChipCountAtStart(Mockito.any())).thenReturn(1000);

        // Other defaults
        when(context.wasRaisedPreFlop()).thenReturn(false);
        when(context.getPotOdds(Mockito.any())).thenReturn(0.0f);

        return context;
    }

    /**
     * Create a mock player with given chip count.
     */
    public static GamePlayerInfo createPlayer(int chipCount) {
        GamePlayerInfo player = mock(GamePlayerInfo.class);
        when(player.getChipCount()).thenReturn(chipCount);
        when(player.isFolded()).thenReturn(false);
        when(player.isAllIn()).thenReturn(false);
        return player;
    }

    /**
     * Create action options.
     *
     * @param canCheck
     *            true if check is available
     * @param canCall
     *            true if call is available
     * @param canBet
     *            true if bet is available
     * @param canRaise
     *            true if raise is available
     * @param canFold
     *            true if fold is available
     */
    public static ActionOptions createOptions(boolean canCheck, boolean canCall, boolean canBet, boolean canRaise,
            boolean canFold) {
        ActionOptions options = mock(ActionOptions.class);
        when(options.canCheck()).thenReturn(canCheck);
        when(options.canCall()).thenReturn(canCall);
        when(options.canBet()).thenReturn(canBet);
        when(options.canRaise()).thenReturn(canRaise);
        when(options.canFold()).thenReturn(canFold);
        return options;
    }

    /**
     * Convert Hand to Card array.
     */
    private static Card[] handToArray(Hand hand) {
        Card[] cards = new Card[hand.size()];
        for (int i = 0; i < hand.size(); i++) {
            cards[i] = hand.getCard(i);
        }
        return cards;
    }
}
