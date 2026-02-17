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

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;

import java.util.*;
import com.donohoedigital.games.poker.core.state.BettingRound;

/**
 * Static display helpers for rendering showdown results on the game board.
 * Phase lifecycle (ChainPhase) removed — game logic now lives in the server.
 *
 * @author Doug Donohoe
 */
public class Showdown {

    private Showdown() {
    }

    /**
     * Display showdown results - designed to be called multiple times, adjusting
     * for changes in options.
     */
    static void displayShowdown(GameEngine engine, GameContext context, HoldemHand hhand) {
        PokerGame game = (PokerGame) context.getGame();
        PokerPlayer player;
        ResultsPiece piece;
        Territory t;
        HandInfo info;
        boolean bUncontested = hhand.isUncontested();
        boolean bShowRiver = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_RABBITHUNT);
        boolean bShowWin = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_SHOWWINNINGHAND);
        boolean bShowMuck = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_SHOW_MUCKED);
        boolean bHumanUp = !PokerUtils.isOptionOn(PokerConstants.OPTION_HOLE_CARDS_DOWN);
        boolean bAIFaceUp = PokerUtils.isCheatOn(context, PokerConstants.OPTION_CHEAT_AIFACEUP);
        boolean bSeenRiver = hhand.isActionInRound(BettingRound.RIVER.toLegacy());
        boolean bShowCards;
        boolean bShowHandType = !bUncontested || ((bShowRiver || bSeenRiver) && bShowWin);
        boolean bShowHandTypeFold = !bUncontested || bShowRiver || bSeenRiver;
        boolean bShowHandTypeLocal;

        // display results
        int nAmount;
        int nOverbet;
        int nTotal;
        String sResult;
        int nResult;
        boolean bWon;

        for (int i = 0; i < hhand.getNumPlayers(); i++) {
            // get info
            player = hhand.getPlayerAt(i);
            if (player.getTable() == null)
                continue; // could be null due to saving after RemovePlayer cheat option used
            t = PokerUtils.getTerritoryForTableSeat(player.getTable(), player.getSeat());
            piece = PokerGameboard.getTerritoryInfo(t).resultpiece;

            // all-in showdown, just show current hand
            if (hhand.getRound().toLegacy() < BettingRound.SHOWDOWN.toLegacy()) {
                if (!player.isFolded()) {
                    if (hhand.getRound() == BettingRound.PRE_FLOP) {
                        piece.setResult(ResultsPiece.ALLIN,
                                PropertyConfig.getMessage("msg.hand.allin", player.getHand().toStringRank()));
                    } else {
                        info = new HandInfo(player, player.getHandSorted(), hhand.getCommunitySorted());
                        piece.setResult(ResultsPiece.ALLIN, PropertyConfig.getMessage("msg.hand.allin",
                                info.getHandTypeDesc(), info.getBest().toStringRank()));
                    }
                }
                continue;
            }

            // cleanup % win (all-in)
            player.setAllInPerc(null);

            ///
            /// folded players
            ///
            if (player.isFolded()) {
                if (player.showFoldedHand()) {
                    int nRound = hhand.getFoldRound(player);
                    info = new HandInfo(player, player.getHandSorted(), hhand.getCommunitySorted());
                    String sRound = PropertyConfig.getMessage("msg.round." + nRound);
                    piece.setResult(ResultsPiece.FOLD,
                            PropertyConfig.getMessage(bShowHandTypeFold ? "msg.hand.fold" : "msg.hand.fold.noshow",
                                    sRound, info.getHandTypeDesc(), info.getBest().toStringRank()));
                    PokerUtils.showCards(player, true);
                } else {
                    // online game - use disconnected logic so we don't remove
                    // a sitting out/disconnected placard
                    if (game.isOnlineGame()) {
                        PokerUtils.setConnectionStatus(context, player, true);
                    } else {
                        piece.setResult(ResultsPiece.HIDDEN, "");
                    }
                    PokerUtils.showCards(player, false);
                }
                continue;
            }

            ///
            /// players who reached showdown
            ///

            // amount won
            nAmount = hhand.getWin(player);
            nOverbet = hhand.getOverbet(player);
            nTotal = nAmount + nOverbet;
            bWon = (nAmount > 0);

            // determine whether cards and hand types are shown

            bShowCards = player.isCardsExposed() || (!bUncontested && (bShowMuck && !bWon)) || (bShowWin && bWon)
                    || (player.isHuman() && player.isLocallyControlled() && bHumanUp)
                    || (player.isComputer() && bAIFaceUp);

            bShowHandTypeLocal = bShowHandType;
            // human cards are always known to user (mouse over hole cards immaterial),
            // so show handtype if showing river
            if (player.isHuman() && bShowRiver)
                bShowHandTypeLocal = true;

            // uncontested, but winning player is showing hand
            if (bUncontested && player.isShowWinning() && (bShowRiver || bSeenRiver))
                bShowHandTypeLocal = true;

            // get hand info
            info = player.getHandInfo();

            // overbet / win text
            if (nTotal > 0) {
                sResult = PropertyConfig.getMessage(bShowHandTypeLocal ? "msg.hand.win" : "msg.hand.win.noshow",
                        info.getHandTypeDesc(), info.getBest().toStringRank(), nTotal);
            }
            // lose text
            else {
                sResult = PropertyConfig.getMessage(bShowCards ? "msg.hand.lose" : "msg.hand.lose.muck",
                        info.getHandTypeDesc(), info.getBest().toStringRank());
            }

            // placard choice
            if (nTotal == 0) {
                nResult = ResultsPiece.LOSE;
            } else {
                if (nOverbet == nTotal) {
                    nResult = ResultsPiece.OVERBET;
                } else {
                    nResult = ResultsPiece.WIN;
                }
            }
            piece.setResult(nResult, sResult);
            PokerUtils.showCards(player, bShowCards);
        }

        // update board cards too (for change in option)
        Territory flop = PokerUtils.getFlop();
        List<GamePiece> cards;
        CardPiece card;
        synchronized (flop.getMap()) {
            cards = EngineUtils.getMatchingPieces(flop, PokerConstants.PIECE_CARD);
            for (GamePiece gp : cards) {
                card = (CardPiece) gp;
                card.setNotDrawn(!card.isDrawnNormal() && !bShowRiver);
            }
        }

        PokerUtils.getPokerGameboard().repaintAll();
    }

    /**
     * Display all-in showdown percentages — called as community cards are revealed.
     */
    static void displayAllin(HoldemHand hhand, boolean bAllCardsDisplayed) {
        PokerPlayer player;
        ResultsPiece piece;
        Territory t;
        HandInfo info;

        // we know next card before we display it, so do all in
        // percentages based on community cards before current cards
        HandSorted comm = new HandSorted(bAllCardsDisplayed ? hhand.getCommunity() : hhand.getCommunityForDisplay());
        HandStrength.doAllInPercentages(hhand, comm);

        int nMax = 0;
        for (int i = 0; i < hhand.getNumPlayers(); i++) {
            player = hhand.getPlayerAt(i);
            if (player.isFolded())
                continue;
            if (player.getAllInWin() > nMax) {
                nMax = player.getAllInWin();
            }
        }

        int nResult;
        for (int i = 0; i < hhand.getNumPlayers(); i++) {
            // get info
            player = hhand.getPlayerAt(i);
            t = PokerUtils.getTerritoryForTableSeat(player.getTable(), player.getSeat());
            piece = PokerGameboard.getTerritoryInfo(t).resultpiece;

            nResult = ResultsPiece.ALLIN;
            if (player.getAllInWin() == nMax)
                nResult = ResultsPiece.WIN;

            if (!player.isFolded()) {
                // when this is called, round has advanced already
                if (hhand.getRound() == BettingRound.FLOP) {
                    piece.setResult(nResult, PropertyConfig.getMessage("msg.hand.allin.pre", player.getAllInPerc(),
                            player.getHand().toStringRank()));
                } else {
                    info = new HandInfo(player, player.getHandSorted(), comm);
                    piece.setResult(nResult, PropertyConfig.getMessage("msg.hand.allin", player.getAllInPerc(),
                            info.getHandTypeDesc(), info.getBest().toStringRank()));
                }
            }
        }

        PokerUtils.getPokerGameboard().repaintAll();
    }
}
