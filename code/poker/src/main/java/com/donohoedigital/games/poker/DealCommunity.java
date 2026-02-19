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
package com.donohoedigital.games.poker;

import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.dashboard.*;

import com.donohoedigital.games.poker.core.state.BettingRound;

/**
 * Static display helpers for dealing community cards to the board. Phase
 * lifecycle (ChainPhase) removed — game logic now lives in the server.
 *
 * @author Doug Donohoe
 */
public class DealCommunity {

    private DealCommunity() {
    }

    /**
     * Make sure board cards match what is actually displayed.
     */
    public static void syncCards(PokerTable table) {
        HoldemHand hhand = table.getHoldemHand();
        if (hhand == null)
            return;

        // get last betting round and current round
        HandAction last = hhand.getLastAction();
        int nLastBettingRound = last != null ? last.getRound() : HoldemHand.ROUND_PRE_FLOP;

        // these flags match above
        int nNumWithCards = hhand.getNumWithCards();
        boolean bRabbitHunt = PokerUtils.isCheatOn(table.getGame().getGameContext(),
                PokerConstants.OPTION_CHEAT_RABBITHUNT);
        boolean bDrawnNormal = nNumWithCards > 1;
        boolean bDrawn = bRabbitHunt || bDrawnNormal;

        // all-in-showdown happening, so only show cards up to previous round
        // due to the way saves are done, DealCommunity is called again which
        // will re-display the placards (this is why we don't call initial a sync()
        // from the GamePrefsDialog)
        int nRound = hhand.getRoundForDisplay();

        // all cases fall through on purpose
        boolean bCardDealt;
        switch (nRound) {
            case HoldemHand.ROUND_SHOWDOWN :
            case HoldemHand.ROUND_RIVER :
                bCardDealt = nLastBettingRound >= BettingRound.RIVER.toLegacy();
                addCard(table, CardPiece.POINT_FLOP5, 4, bDrawnNormal || bCardDealt, bDrawn || bCardDealt, false);
            case HoldemHand.ROUND_TURN :
                bCardDealt = nLastBettingRound >= BettingRound.TURN.toLegacy();
                addCard(table, CardPiece.POINT_FLOP4, 3, bDrawnNormal || bCardDealt, bDrawn || bCardDealt, false);
            case HoldemHand.ROUND_FLOP :
                bCardDealt = nLastBettingRound >= BettingRound.FLOP.toLegacy();
                addCard(table, CardPiece.POINT_FLOP3, 2, bDrawnNormal || bCardDealt, bDrawn || bCardDealt, false);
                addCard(table, CardPiece.POINT_FLOP2, 1, bDrawnNormal || bCardDealt, bDrawn || bCardDealt, false);
                addCard(table, CardPiece.POINT_FLOP1, 0, bDrawnNormal || bCardDealt, bDrawn || bCardDealt, false);
        }

        // update dash item to match
        MyHand.cardsChanged(table);
    }

    /**
     * bDrawn - is card displayed (might be yes if show river cards is on)
     * bDrawnNormal - is card displayed normally
     */
    private static void addCard(PokerTable table, String sTP, int c, boolean bDrawnNormal, boolean bDrawn,
            boolean bRepaint) {
        CommunityCardPiece piece = new CommunityCardPiece(table, sTP, c);
        piece.setNotDrawn(!bDrawn);
        piece.setDrawnNormal(bDrawnNormal);
        Territory t = PokerUtils.getFlop();
        t.addGamePiece(piece);
        if (bRepaint)
            PokerUtils.getPokerGameboard().repaintTerritory(t);
    }
}
