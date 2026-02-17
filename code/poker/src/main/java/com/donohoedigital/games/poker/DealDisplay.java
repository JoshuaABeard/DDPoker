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

import com.donohoedigital.base.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.gui.*;

/**
 * Static display helpers for dealing hole cards to player seats. Phase
 * lifecycle (ChainPhase) removed — game logic now lives in the server.
 *
 * @author Doug Donohoe
 */
public class DealDisplay {

    private DealDisplay() {
    }

    /**
     * Make sure cards in players hands in table match what is actually displayed.
     */
    public static void syncCards(PokerTable table) {
        PokerPlayer player;
        Hand hand;
        for (int i = 0; i < PokerConstants.SEATS; i++) {
            player = table.getPlayer(i);
            if (player == null)
                continue;

            hand = player.getHand();
            if (hand == null)
                continue;

            for (int c = 0; c < hand.size(); c++) {
                displayCard(table.getGame().getGameContext(), player, c, false, 0);
            }
        }
    }

    /**
     * Display card for given player.
     */
    private static void displayCard(GameContext context, PokerPlayer player, int c, boolean bRepaint, int nCardDelay) {
        int nSeat = player.getSeat();
        Hand hand = player.getHand();
        boolean bUp = hand.getType() != Hand.TYPE_NORMAL;
        boolean bDealDown = PokerUtils.isOptionOn(PokerConstants.OPTION_HOLE_CARDS_DOWN);
        boolean bAIFaceUp = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_AIFACEUP);

        // get card and create a piece around it
        Territory t;
        CardPiece piece = new CardPiece(context, player, CardPiece.POINT_HOLE1,
                bUp || (player.isHuman() && player.isLocallyControlled() && !bDealDown)
                        || (player.isComputer() && bAIFaceUp),
                c);
        if (hand.getType() == Hand.TYPE_COLOR_UP)
            piece.setThumbnailMode(true);
        t = PokerUtils.getTerritoryForTableSeat(player.getTable(), nSeat);
        t.addGamePiece(piece);

        if (bRepaint) {
            final Territory tParam = t;
            GuiUtils.invokeAndWait(new Runnable() {
                public void run() {
                    // check null for cleaner exit
                    if (PokerUtils.getGameboard() != null) {
                        PokerUtils.getGameboard().repaintTerritory(tParam, true);
                    }
                }
            });

            // sleep
            Utils.sleepMillis(nCardDelay);
        }
    }
}
