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
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.engine.Hand;

import java.util.List;

/**
 * Extended AI context for V2 algorithm. Provides strategy factors, opponent
 * models, hand evaluation, and table state queries beyond what base AIContext
 * offers.
 */
public interface V2AIContext extends AIContext {

    // === Strategy ===

    StrategyProvider getStrategy();

    // === Tournament Metrics ===

    float getHohM(GamePlayerInfo player);

    float getHohQ(GamePlayerInfo player);

    int getHohZone(GamePlayerInfo player);

    float getTableAverageHohM();

    float getRemainingAverageHohM();

    // === Opponent Models ===

    V2OpponentModel getOpponentModel(GamePlayerInfo player);

    V2OpponentModel getSelfModel();

    // === Hand Evaluation (V2-specific) ===

    int getHandScore(Hand pocket, Hand community);

    float getRawHandStrength(Hand pocket, Hand community);

    float getBiasedRawHandStrength(int seat, Hand community);

    float getBiasedEffectiveHandStrength(int seat, Hand community);

    float getApparentStrength(int seat, Hand community);

    // === Draw Detection ===

    int getNutFlushCount(Hand pocket, Hand community);

    int getNonNutFlushCount(Hand pocket, Hand community);

    int getNutStraightCount(Hand pocket, Hand community);

    int getNonNutStraightCount(Hand pocket, Hand community);

    // === Table State (V2-specific) ===

    int getStartingPositionCategory(GamePlayerInfo player);

    int getPostFlopPositionCategory(GamePlayerInfo player);

    int getStartingOrder(GamePlayerInfo player);

    boolean wasRaisedPreFlop();

    boolean wasFirstRaiserPreFlop(GamePlayerInfo player);

    boolean wasLastRaiserPreFlop(GamePlayerInfo player);

    boolean wasOnlyRaiserPreFlop(GamePlayerInfo player);

    GamePlayerInfo getFirstBettor(int round, boolean includeRaises);

    int getFirstVoluntaryAction(GamePlayerInfo player, int round);

    boolean wasPotAction(int round);

    int getPotStatus();

    int getLastActionThisRound(GamePlayerInfo player);

    int getSeat(GamePlayerInfo player);

    int getChipCountAtStart(GamePlayerInfo player);

    int getHandsBeforeBigBlind(GamePlayerInfo player);

    int getConsecutiveHandsUnpaid(GamePlayerInfo player);

    int getMinRaise();

    float getPotOdds(GamePlayerInfo player);

    boolean paidToPlay(GamePlayerInfo player);

    boolean couldLimp(GamePlayerInfo player);

    boolean limped(GamePlayerInfo player);

    boolean isLimit();

    int getBigBlind();

    int getMinChip();

    int getCall(GamePlayerInfo player);

    int getTotalPotChipCount();

    // === Cards ===

    Hand getCommunity();

    Hand getPocketCards(GamePlayerInfo player);

    // === Player State ===

    int getNumLimpers();

    boolean hasActedThisRound(GamePlayerInfo player);

    GamePlayerInfo getLastBettor(int round, boolean includeRaises);

    int getNumFoldsSinceLastBet();

    boolean isBlind(GamePlayerInfo player);

    boolean isButton(GamePlayerInfo player);

    boolean isSmallBlind(GamePlayerInfo player);

    boolean isBigBlind(GamePlayerInfo player);

    int getNumPlayersWithCards();

    // === Player Iteration ===

    int getNumPlayersAtTable();

    GamePlayerInfo getPlayerAt(int index);

    List<GamePlayerInfo> getPlayersLeft(GamePlayerInfo excludePlayer);
}
