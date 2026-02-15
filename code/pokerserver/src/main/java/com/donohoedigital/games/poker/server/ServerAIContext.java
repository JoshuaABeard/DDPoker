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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.ai.AIContext;
import com.donohoedigital.games.poker.engine.Card;

/**
 * Server-side implementation of AIContext for providing game state to AI.
 * <p>
 * <strong>Status:</strong> Minimal implementation for Phase 7D
 * <p>
 * Currently provides only the essential methods needed by TournamentAI
 * (tournament context for M-ratio calculation). Other methods return stub
 * values.
 * <p>
 * <strong>Future:</strong> When V1/V2 algorithms are extracted, implement
 * remaining methods (hand evaluation, position queries, pot queries, etc.).
 *
 * @see AIContext
 * @see ServerAIProvider
 */
public class ServerAIContext implements AIContext {

    private final GameTable table;
    private final GameHand currentHand;
    private final TournamentContext tournament;

    /**
     * Create AI context for server game.
     *
     * @param table
     *            Current table state
     * @param currentHand
     *            Current hand being played (or null between hands)
     * @param tournament
     *            Tournament context for blind structure
     */
    public ServerAIContext(GameTable table, GameHand currentHand, TournamentContext tournament) {
        this.table = table;
        this.currentHand = currentHand;
        this.tournament = tournament;
    }

    // ========== Implemented Methods (used by TournamentAI) ==========

    @Override
    public GameTable getTable() {
        return table;
    }

    @Override
    public GameHand getCurrentHand() {
        return currentHand;
    }

    @Override
    public TournamentContext getTournament() {
        return tournament;
    }

    // ========== Stub Methods (not yet needed by TournamentAI) ==========
    // TODO: Implement these when V1/V2 algorithms are extracted

    @Override
    public boolean isButton(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public boolean isSmallBlind(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public boolean isBigBlind(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public int getPosition(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getPotSize() {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getAmountToCall(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getAmountBetThisRound(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getLastBetAmount() {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getNumActivePlayers() {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getNumPlayersYetToAct(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getNumPlayersWhoActed(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public boolean hasBeenBet() {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public boolean hasBeenRaised() {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public GamePlayerInfo getLastBettor() {
        // TODO: Implement for V1/V2 AI
        return null;
    }

    @Override
    public GamePlayerInfo getLastRaiser() {
        // TODO: Implement for V1/V2 AI
        return null;
    }

    @Override
    public int evaluateHandRank(Card[] holeCards, Card[] communityCards) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public long evaluateHandScore(Card[] holeCards, Card[] communityCards) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public double calculateImprovementOdds(Card[] holeCards, Card[] communityCards) {
        // TODO: Implement for V1/V2 AI
        return 0.0;
    }

    @Override
    public int getBettingRound() {
        // TODO: Implement for V1/V2 AI
        return 0;
    }
}
