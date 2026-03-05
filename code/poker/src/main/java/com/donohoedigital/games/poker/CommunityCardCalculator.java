/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.state.BettingRound;

/**
 * Pure-logic calculator for community card visibility, extracted from
 * {@link DealCommunity#syncCards(PokerTable)}. Takes only primitive parameters
 * so it can be tested without UI or game-object dependencies.
 */
public class CommunityCardCalculator {

    /**
     * Holds per-card visibility flags for the 5 community cards. Array indices map
     * to: 0=flop1, 1=flop2, 2=flop3, 3=turn, 4=river.
     *
     * @param active
     *            whether each card is within scope of the current display round
     * @param drawnNormal
     *            whether each card is displayed normally (multiple players still
     *            in, or card already dealt)
     * @param drawn
     *            whether each card is displayed at all (includes rabbit-hunt or
     *            card-dealt overrides)
     */
    public record CommunityCardVisibility(boolean[] active, boolean[] drawnNormal, boolean[] drawn) {

        public CommunityCardVisibility {
            if (active.length != 5 || drawnNormal.length != 5 || drawn.length != 5) {
                throw new IllegalArgumentException("Arrays must have exactly 5 elements");
            }
        }
    }

    private CommunityCardCalculator() {
    }

    /**
     * Calculate which community cards should be visible based on the current
     * display round and game state.
     *
     * @param displayRound
     *            current round for display purposes (HoldemHand.ROUND_* constants)
     * @param lastBettingRound
     *            last betting round that has occurred (HoldemHand.ROUND_*
     *            constants)
     * @param numWithCards
     *            number of players who still have cards
     * @param rabbitHunt
     *            whether the rabbit-hunt cheat option is enabled
     * @return visibility flags for all 5 community cards
     */
    public static CommunityCardVisibility calculateVisibility(int displayRound, int lastBettingRound, int numWithCards,
            boolean rabbitHunt) {

        boolean[] active = new boolean[5];
        boolean[] drawnNormal = new boolean[5];
        boolean[] drawn = new boolean[5];

        boolean bDrawnNormal = numWithCards > 1;
        boolean bDrawn = rabbitHunt || bDrawnNormal;

        // all cases fall through on purpose — matches DealCommunity.syncCards()
        boolean bCardDealt;
        switch (displayRound) {
            case HoldemHand.ROUND_SHOWDOWN :
            case HoldemHand.ROUND_RIVER :
                bCardDealt = lastBettingRound >= BettingRound.RIVER.toLegacy();
                active[4] = true;
                drawnNormal[4] = bDrawnNormal || bCardDealt;
                drawn[4] = bDrawn || bCardDealt;
            case HoldemHand.ROUND_TURN :
                bCardDealt = lastBettingRound >= BettingRound.TURN.toLegacy();
                active[3] = true;
                drawnNormal[3] = bDrawnNormal || bCardDealt;
                drawn[3] = bDrawn || bCardDealt;
            case HoldemHand.ROUND_FLOP :
                bCardDealt = lastBettingRound >= BettingRound.FLOP.toLegacy();
                active[2] = true;
                active[1] = true;
                active[0] = true;
                drawnNormal[2] = bDrawnNormal || bCardDealt;
                drawn[2] = bDrawn || bCardDealt;
                drawnNormal[1] = bDrawnNormal || bCardDealt;
                drawn[1] = bDrawn || bCardDealt;
                drawnNormal[0] = bDrawnNormal || bCardDealt;
                drawn[0] = bDrawn || bCardDealt;
        }

        return new CommunityCardVisibility(active, drawnNormal, drawn);
    }
}
