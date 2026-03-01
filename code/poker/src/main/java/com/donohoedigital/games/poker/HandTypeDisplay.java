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

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.core.ai.HandInfoFast;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandScoreConstants;

/**
 * Display utilities for hand types. Provides localized hand type descriptions
 * and best-hand rank strings derived from hand scores. This is a thin display
 * adapter — all hand evaluation is performed by
 * {@code pokergamecore.HandInfoFast}.
 */
public final class HandTypeDisplay implements HandScoreConstants {

    private static final String[] desc_ = new String[ROYAL_FLUSH + 1];
    private static volatile boolean init_ = false;

    private HandTypeDisplay() {
    }

    /**
     * Returns the localized hand type description for the given hand type constant
     * (e.g. {@link HandScoreConstants#FLUSH} → "Flush").
     */
    public static String getHandTypeDesc(int nType) {
        ensureInit();
        if (nType < HIGH_CARD || nType > ROYAL_FLUSH)
            return "";
        return desc_[nType];
    }

    /**
     * Returns the hand type constant for the given score, delegating to
     * {@link HandInfoFast#getTypeFromScore(int)}.
     */
    public static int getTypeFromScore(int score) {
        return HandInfoFast.getTypeFromScore(score);
    }

    /**
     * Returns a rank-only string for the best 5 cards implied by {@code score},
     * e.g. "A K Q J T".
     *
     * <p>
     * The ranks are decoded from the score using
     * {@link HandInfoFast#getCards(int, int[])} and mapped to rank display strings
     * via {@link Card#getRank(int)}.
     */
    public static String getBestRankString(int score) {
        int[] ranks = new int[5];
        HandInfoFast.getCards(score, ranks);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ranks.length; i++) {
            if (ranks[i] == 0)
                break;
            if (i > 0)
                sb.append(' ');
            sb.append(Card.getRank(ranks[i]));
        }
        return sb.toString();
    }

    /**
     * Computes the hand score for {@code pocket} + {@code community} using
     * {@link HandInfoFast}, returning it as an int for comparison.
     */
    public static int getHandScore(Hand pocket, Hand community) {
        HandInfoFast fast = new HandInfoFast();
        return fast.getScore(pocket, community);
    }

    /**
     * Returns a localized display string for the hand described by {@code info}
     * after a {@link HandInfoFast#getScore} call, equivalent to the old
     * {@code HandInfoFast.toString(divider, false)}.
     *
     * <p>
     * Example: {@code toDisplayString(info, ", ")} → {@code "Pair,  As"}.
     */
    public static String toDisplayString(HandInfoFast info, String divider) {
        ensureInit();
        int type = info.getHandType();
        StringBuilder buf = new StringBuilder();
        buf.append(desc_[type]);
        switch (type) {
            case HIGH_CARD :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortSingular(info.getHighCardRank())));
                break;
            case PAIR :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(info.getBigPairRank())));
                break;
            case TWO_PAIR :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(info.getBigPairRank()),
                        rankShortPlural(info.getSmallPairRank())));
                break;
            case TRIPS :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(info.getTripsRank())));
                break;
            case STRAIGHT :
            case STRAIGHT_FLUSH :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                        rankShortSingular(info.getStraightLowRank()), rankShortSingular(info.getStraightHighRank())));
                break;
            case FLUSH :
                buf.append(divider);
                buf.append(
                        PropertyConfig.getMessage("msg.handfmt." + type, rankShortSingular(info.getFlushHighRank())));
                break;
            case FULL_HOUSE :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(info.getTripsRank()),
                        rankShortPlural(info.getBigPairRank())));
                break;
            case QUADS :
                buf.append(divider);
                buf.append(PropertyConfig.getMessage("msg.handfmt." + type, rankShortPlural(info.getQuadsRank())));
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
        return PropertyConfig.getMessage("msg.cardrank.singular", Card.getRank(rank));
    }

    /** Short plural rank label using {@code msg.cardrank.plural} (short form). */
    private static String rankShortPlural(int rank) {
        return PropertyConfig.getMessage("msg.cardrank.plural", Card.getRank(rank));
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
