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
import com.donohoedigital.games.poker.display.ClientHandScoreConstants;
import com.donohoedigital.games.poker.protocol.dto.HandEvaluationData;

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
        return score / SCORE_BASE;
    }

    /**
     * Returns a rank-only string for the best 5 cards implied by {@code score},
     * e.g. "A K Q J T".
     */
    public static String getBestRankString(int score) {
        int[] ranks = new int[5];
        getCardsFromScore(score, ranks);
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
     * Extract the best card ranks from a score into the given array.
     */
    private static void getCardsFromScore(int score, int[] cards) {
        int s = score % SCORE_BASE;
        int cnt = 0;
        for (int i = 16; i >= 0; i -= 4) {
            int n = (s >> i) % 16;
            if (n == 0)
                continue;
            if (cnt < cards.length) {
                cards[cnt++] = n;
            }
        }
    }

    /**
     * Returns a localized display string for the hand described by the given
     * {@code HandEvaluationData}.
     *
     * <p>
     * Example: {@code toDisplayString(eval, ", ")} -> {@code "Pair,  As"}.
     */
    public static String toDisplayString(HandEvaluationData eval, String divider) {
        ensureInit();
        int type = eval.handType();
        StringBuilder buf = new StringBuilder();
        buf.append(desc_[type]);
        switch (type) {
            case HIGH_CARD :
                if (eval.highCardRank() != null) {
                    buf.append(divider);
                    buf.append(
                            PropertyConfig.getMessage("msg.handfmt." + type, rankShortSingular(eval.highCardRank())));
                }
                break;
            case PAIR :
                if (eval.bigPairRank() != null) {
                    buf.append(divider);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.bigPairRank())));
                }
                break;
            case TWO_PAIR :
                if (eval.bigPairRank() != null && eval.smallPairRank() != null) {
                    buf.append(divider);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.bigPairRank()),
                            rankShortPlural(eval.smallPairRank())));
                }
                break;
            case TRIPS :
                if (eval.tripsRank() != null) {
                    buf.append(divider);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.tripsRank())));
                }
                break;
            case STRAIGHT :
            case STRAIGHT_FLUSH :
                if (eval.straightLowRank() != null && eval.straightHighRank() != null) {
                    buf.append(divider);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                            rankShortSingular(eval.straightLowRank()), rankShortSingular(eval.straightHighRank())));
                }
                break;
            case FLUSH :
                if (eval.flushHighRank() != null) {
                    buf.append(divider);
                    buf.append(
                            PropertyConfig.getMessage("msg.handfmt." + type, rankShortSingular(eval.flushHighRank())));
                }
                break;
            case FULL_HOUSE :
                if (eval.tripsRank() != null && eval.bigPairRank() != null) {
                    buf.append(divider);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.tripsRank()),
                            rankShortPlural(eval.bigPairRank())));
                }
                break;
            case QUADS :
                if (eval.quadsRank() != null) {
                    buf.append(divider);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(eval.quadsRank())));
                }
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
