/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.display.ClientCard;
import com.donohoedigital.games.poker.display.ClientHand;
import com.donohoedigital.games.poker.display.ClientHandScoreConstants;

import com.donohoedigital.config.PropertyConfig;

/**
 * Display utilities for hand types. Provides localized hand type descriptions
 * and best-hand rank strings derived from hand scores.
 */
public final class HandTypeDisplay implements ClientHandScoreConstants {

    private static final String[] desc_ = new String[ROYAL_FLUSH + 1];
    private static volatile boolean init_ = false;

    private HandTypeDisplay() {
    }

    /**
     * Returns the localized hand type description for the given hand type constant
     * (e.g. {@link ClientHandScoreConstants#FLUSH} -> "Flush").
     */
    public static String getHandTypeDesc(int nType) {
        ensureInit();
        if (nType < HIGH_CARD || nType > ROYAL_FLUSH)
            return "";
        return desc_[nType];
    }

    /**
     * Returns the hand type constant for the given score.
     */
    public static int getTypeFromScore(int score) {
        return ClientHandEval.typeFromScore(score);
    }

    /**
     * Returns a rank-only string for the best 5 cards implied by {@code score},
     * e.g. "A K Q J T".
     */
    public static String getBestRankString(int score) {
        int[] ranks = new int[5];
        ClientHandEval.getCardsFromScore(score, ranks);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ranks.length; i++) {
            if (ranks[i] == 0)
                break;
            if (i > 0)
                sb.append(' ');
            sb.append(ClientCard.getRank(ranks[i]));
        }
        return sb.toString();
    }

    /**
     * Computes the hand score for {@code pocket} + {@code community} using hand
     * evaluation, returning it as an int for comparison.
     */
    public static int getHandScore(ClientHand pocket, ClientHand community) {
        ClientHandEval eval = new ClientHandEval();
        return eval.score(pocket, community);
    }

    /**
     * Returns a localized display string for the hand described by the given
     * {@code eval} after a {@link ClientHandEval#score} call.
     *
     * <p>
     * Example: {@code toDisplayString(eval, ", ")} -> {@code "Pair,  As"}.
     */
    public static String toDisplayString(ClientHandEval eval, String divider) {
        ensureInit();
        int type = eval.getHandType();
        StringBuilder buf = new StringBuilder();
        buf.append(desc_[type]);
        switch (type) {
            case HIGH_CARD :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortSingular(eval.getHighCardRank())));
                break;
            case PAIR :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.getBigPairRank())));
                break;
            case TWO_PAIR :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.getBigPairRank()),
                        rankShortPlural(eval.getSmallPairRank())));
                break;
            case TRIPS :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.getTripsRank())));
                break;
            case STRAIGHT :
            case STRAIGHT_FLUSH :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        rankShortSingular(eval.getStraightLowRank()), rankShortSingular(eval.getStraightHighRank())));
                break;
            case FLUSH :
                buf.append(divider);
                buf.append(
                        PropertyConfig.getMessage("msg.handfmt." + type, rankShortSingular(eval.getFlushHighRank())));
                break;
            case FULL_HOUSE :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.getTripsRank()),
                        rankShortPlural(eval.getBigPairRank())));
                break;
            case QUADS :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.getQuadsRank())));
                break;
            case ROYAL_FLUSH :
                break;
            default :
                break;
        }
        return buf.toString();
    }

    /**
     * Short singular rank label using {@code msg.cardrank.singular} (short form).
     */
    private static String rankShortSingular(int rank) {
        return PropertyConfig.getMessage("msg.cardrank.singular", ClientCard.getRank(rank));
    }

    /** Short plural rank label using {@code msg.cardrank.plural} (short form). */
    private static String rankShortPlural(int rank) {
        return PropertyConfig.getMessage("msg.cardrank.plural", ClientCard.getRank(rank));
    }

    private static synchronized void ensureInit() {
        if (init_)
            return;
        init_ = true;
        for (int i = HIGH_CARD; i <= ROYAL_FLUSH; i++) {
            desc_[i] = PropertyConfig.getMessage("msg.hand." + i);
        }
    }
}
