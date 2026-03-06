/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandInfoFast;
import com.donohoedigital.games.poker.engine.HandSorted;
import com.donohoedigital.games.poker.engine.HandUtils;
import com.donohoedigital.games.poker.gameserver.websocket.OutboundMessageConverter;
import com.donohoedigital.games.poker.protocol.dto.HandEvaluationData;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes {@link HandEvaluationData} on the server side using
 * {@link HandInfoFast}. Used by the broadcaster to populate hand evaluation in
 * showdown, winner, and advisor messages.
 */
public class HandEvaluationHelper {

    private HandEvaluationHelper() {
    }

    /**
     * Evaluate a hand and return pre-computed evaluation data.
     *
     * @param holeCards
     *            player's hole cards
     * @param communityCards
     *            community cards (may be empty)
     * @return hand evaluation data, or {@link HandEvaluationData#NONE} if
     *         insufficient cards
     */
    public static HandEvaluationData evaluate(List<Card> holeCards, List<Card> communityCards) {
        if (holeCards == null || holeCards.isEmpty()) {
            return HandEvaluationData.NONE;
        }

        Hand pocket = new Hand(holeCards.size());
        for (Card c : holeCards) {
            pocket.addCard(c);
        }

        Hand community = new Hand(communityCards != null ? communityCards.size() : 0);
        if (communityCards != null) {
            for (Card c : communityCards) {
                community.addCard(c);
            }
        }

        HandInfoFast info = new HandInfoFast();
        int score = info.getScore(pocket, community);
        int handType = HandInfoFast.getTypeFromScore(score);

        // Hand description (only meaningful with 5+ cards)
        int totalCards = pocket.size() + community.size();
        String description = "";
        if (totalCards >= 5) {
            description = describeHand(info, handType);
        }

        // Best five cards
        List<String> bestFiveCards = List.of();
        if (totalCards >= 5) {
            Hand bestFive = HandUtils.getBestFive(new HandSorted(pocket), new HandSorted(community));
            bestFiveCards = handToCardStrings(bestFive);
        }

        // Extract rank components (0 means N/A, convert to null)
        Integer bigPairRank = nullIfZero(info.getBigPairRank());
        Integer smallPairRank = nullIfZero(info.getSmallPairRank());
        Integer tripsRank = nullIfZero(info.getTripsRank());
        Integer quadsRank = nullIfZero(info.getQuadsRank());
        Integer highCardRank = nullIfZero(info.getHighCardRank());
        Integer straightHighRank = null;
        Integer straightLowRank = null;
        Integer flushHighRank = null;

        if (handType == HandInfoFast.STRAIGHT || handType == HandInfoFast.STRAIGHT_FLUSH
                || handType == HandInfoFast.ROYAL_FLUSH) {
            straightHighRank = info.getStraightHighRank();
            straightLowRank = info.getStraightLowRank();
        }

        if (handType == HandInfoFast.FLUSH || handType == HandInfoFast.STRAIGHT_FLUSH
                || handType == HandInfoFast.ROYAL_FLUSH) {
            flushHighRank = nullIfZero(info.getFlushHighRank());
        }

        // Map hand type from engine constants (1-10) to protocol (0-8)
        // Engine: HIGH_CARD=1, PAIR=2, ..., ROYAL_FLUSH=10
        // Protocol: HIGH_CARD=0, PAIR=1, ..., ROYAL_FLUSH=8
        int protocolHandType = handType > 0 ? handType - 1 : 0;

        return new HandEvaluationData(score, protocolHandType, description, bestFiveCards, bigPairRank, smallPairRank,
                tripsRank, quadsRank, highCardRank, straightHighRank, straightLowRank, flushHighRank);
    }

    /**
     * Evaluate a hand from Card arrays (convenience overload).
     */
    public static HandEvaluationData evaluate(Card[] holeCards, Card[] communityCards) {
        List<Card> holeList = holeCards != null ? List.of(holeCards) : List.of();
        List<Card> communityList = communityCards != null ? List.of(communityCards) : List.of();
        return evaluate(holeList, communityList);
    }

    private static Integer nullIfZero(int value) {
        return value == 0 ? null : value;
    }

    private static List<String> handToCardStrings(Hand hand) {
        List<String> result = new ArrayList<>(hand.size());
        for (int i = 0; i < hand.size(); i++) {
            result.add(OutboundMessageConverter.cardToString(hand.getCard(i)));
        }
        return result;
    }

    private static final String[] RANK_NAMES = {"", "", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Jack", "Queen", "King", "Ace"};

    private static final String[] RANK_NAMES_PLURAL = {"", "", "Twos", "Threes", "Fours", "Fives", "Sixes", "Sevens",
            "Eights", "Nines", "Tens", "Jacks", "Queens", "Kings", "Aces"};

    /**
     * Describe the hand type using data from HandInfoFast.
     */
    private static String describeHand(HandInfoFast info, int handType) {
        switch (handType) {
            case HandInfoFast.ROYAL_FLUSH :
                return "Royal Flush";
            case HandInfoFast.STRAIGHT_FLUSH :
                return "Straight Flush, " + RANK_NAMES[info.getStraightHighRank()] + " High";
            case HandInfoFast.QUADS :
                return "Four of a Kind, " + RANK_NAMES_PLURAL[info.getQuadsRank()];
            case HandInfoFast.FULL_HOUSE :
                return "Full House, " + RANK_NAMES_PLURAL[info.getTripsRank()] + " over "
                        + RANK_NAMES_PLURAL[info.getBigPairRank()];
            case HandInfoFast.FLUSH :
                return "Flush, " + RANK_NAMES[info.getFlushHighRank()] + " High";
            case HandInfoFast.STRAIGHT :
                return "Straight, " + RANK_NAMES[info.getStraightHighRank()] + " High";
            case HandInfoFast.TRIPS :
                return "Three of a Kind, " + RANK_NAMES_PLURAL[info.getTripsRank()];
            case HandInfoFast.TWO_PAIR :
                return "Two Pair, " + RANK_NAMES_PLURAL[info.getBigPairRank()] + " and "
                        + RANK_NAMES_PLURAL[info.getSmallPairRank()];
            case HandInfoFast.PAIR :
                return "One Pair, " + RANK_NAMES_PLURAL[info.getBigPairRank()];
            case HandInfoFast.HIGH_CARD :
                return "High Card, " + RANK_NAMES[info.getHighCardRank()];
            default :
                return "";
        }
    }
}
