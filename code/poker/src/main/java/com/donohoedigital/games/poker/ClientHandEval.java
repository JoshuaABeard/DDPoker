/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.display.ClientCard;
import com.donohoedigital.games.poker.display.ClientHand;
import com.donohoedigital.games.poker.display.ClientHandScoreConstants;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reflection-based bridge to {@code pokerengine.HandInfoFast} and
 * {@code pokerengine.HandUtils}. Eliminates compile-time imports from
 * {@code com.donohoedigital.games.poker.engine} while preserving hand
 * evaluation functionality. The engine classes are available on the classpath
 * transitively via pokergameserver.
 *
 * <p>
 * Each instance wraps a HandInfoFast object. After calling
 * {@link #score(ClientHand, ClientHand)}, accessor methods return the scored
 * hand details.
 * </p>
 */
public final class ClientHandEval implements ClientHandScoreConstants {

    private static final Logger logger = LogManager.getLogger(ClientHandEval.class);

    // Cached reflection handles (initialized once)
    private static volatile boolean initialized;
    private static Class<?> cardClass;
    private static Class<?> handClass;
    private static Class<?> handSortedClass;
    private static Class<?> handInfoFastClass;
    private static Class<?> handUtilsClass;
    private static Method cardGetCard; // Card.getCard(int suit, int rank)
    private static Method handAddCard; // hand.addCard(Card)
    private static Method handGetCard; // hand.getCard(int)
    private static Method handSize; // hand.size()
    private static Constructor<?> handCtor; // new Hand(int)
    private static Constructor<?> handSortedFromHand; // new HandSorted(Hand)
    private static Method getScoreMethod; // handInfoFast.getScore(Hand, Hand)
    private static Method getTypeFromScoreMethod; // HandInfoFast.getTypeFromScore(int)
    private static Method getCardsMethod; // HandInfoFast.getCards(int, int[])
    private static Method getHandType;
    private static Method getBigPairRank;
    private static Method getSmallPairRank;
    private static Method getTripsRank;
    private static Method getQuadsRank;
    private static Method getHighCardRank;
    private static Method getStraightHighRank;
    private static Method getStraightLowRank;
    private static Method getFlushHighRank;
    private static Method getBestFive; // HandUtils.getBestFive(HandSorted, HandSorted)
    private static boolean available;

    private final Object handInfoFast; // instance of HandInfoFast

    public ClientHandEval() {
        ensureInitialized();
        Object hif = null;
        if (available) {
            try {
                hif = handInfoFastClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                logger.warn("Failed to create HandInfoFast instance", e);
            }
        }
        this.handInfoFast = hif;
    }

    /**
     * Whether hand evaluation is available (engine on classpath).
     */
    public static boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    /**
     * Score the given pocket + community cards. Returns the integer score.
     */
    public int score(ClientHand pocket, ClientHand community) {
        if (!available || handInfoFast == null)
            return 0;
        try {
            Object enginePocket = toEngineHand(pocket);
            Object engineCommunity = toEngineHand(community);
            return (int) getScoreMethod.invoke(handInfoFast, enginePocket, engineCommunity);
        } catch (Exception e) {
            logger.debug("Hand scoring failed", e);
            return 0;
        }
    }

    /** Hand type constant (HIGH_CARD..ROYAL_FLUSH) after scoring. */
    public int getHandType() {
        return invokeInt(getHandType);
    }

    /** Big pair rank after scoring. */
    public int getBigPairRank() {
        return invokeInt(getBigPairRank);
    }

    /** Small pair rank after scoring. */
    public int getSmallPairRank() {
        return invokeInt(getSmallPairRank);
    }

    /** Trips rank after scoring. */
    public int getTripsRank() {
        return invokeInt(getTripsRank);
    }

    /** Quads rank after scoring. */
    public int getQuadsRank() {
        return invokeInt(getQuadsRank);
    }

    /** High card rank after scoring. */
    public int getHighCardRank() {
        return invokeInt(getHighCardRank);
    }

    /** Straight high rank after scoring. */
    public int getStraightHighRank() {
        return invokeInt(getStraightHighRank);
    }

    /** Straight low rank after scoring. */
    public int getStraightLowRank() {
        return invokeInt(getStraightLowRank);
    }

    /** Flush high rank after scoring. */
    public int getFlushHighRank() {
        return invokeInt(getFlushHighRank);
    }

    /**
     * Extract hand type from an integer score (static utility).
     */
    public static int typeFromScore(int score) {
        // This is simply score / SCORE_BASE
        return score / SCORE_BASE;
    }

    /**
     * Extract the best card ranks from a score into the given array.
     */
    public static void getCardsFromScore(int score, int[] cards) {
        // Inline the logic from HandInfoFast.getCards() to avoid reflection
        // for this simple math operation
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
     * Get the best 5-card hand from hole and community cards. Returns a ClientHand
     * with the best 5 cards, or null if unavailable.
     */
    public static ClientHand getBestFive(ClientHand hole, ClientHand community) {
        ensureInitialized();
        if (!available)
            return null;
        try {
            Object holeSorted = toEngineHandSorted(hole);
            Object commSorted = toEngineHandSorted(community);
            Object result = getBestFive.invoke(null, holeSorted, commSorted);
            return fromEngineHand(result);
        } catch (Exception e) {
            logger.debug("getBestFive failed", e);
            return null;
        }
    }

    // ---- internal helpers ----

    private int invokeInt(Method method) {
        if (!available || handInfoFast == null)
            return 0;
        try {
            return (int) method.invoke(handInfoFast);
        } catch (Exception e) {
            return 0;
        }
    }

    private static Object toEngineHand(ClientHand clientHand) throws Exception {
        if (clientHand == null) {
            return handCtor.newInstance(0);
        }
        Object h = handCtor.newInstance(clientHand.size());
        for (int i = 0; i < clientHand.size(); i++) {
            ClientCard cc = clientHand.getCard(i);
            Object card = cardGetCard.invoke(null, cc.getSuit(), cc.getRank());
            handAddCard.invoke(h, card);
        }
        return h;
    }

    private static Object toEngineHandSorted(ClientHand clientHand) throws Exception {
        Object hand = toEngineHand(clientHand);
        return handSortedFromHand.newInstance(hand);
    }

    private static ClientHand fromEngineHand(Object engineHand) throws Exception {
        if (engineHand == null)
            return ClientHand.empty();
        int size = (int) handSize.invoke(engineHand);
        ClientHand ch = ClientHand.empty();
        for (int i = 0; i < size; i++) {
            Object card = handGetCard.invoke(engineHand, i);
            // Card has getSuit() -> int and getRank() -> int
            int suit = (int) card.getClass().getMethod("getSuit").invoke(card);
            int rank = (int) card.getClass().getMethod("getRank").invoke(card);
            ch.addCard(ClientCard.getCard(suit, rank));
        }
        return ch;
    }

    private static synchronized void ensureInitialized() {
        if (initialized)
            return;
        initialized = true;
        try {
            cardClass = Class.forName("com.donohoedigital.games.poker.engine.Card");
            handClass = Class.forName("com.donohoedigital.games.poker.engine.Hand");
            handSortedClass = Class.forName("com.donohoedigital.games.poker.engine.HandSorted");
            handInfoFastClass = Class.forName("com.donohoedigital.games.poker.engine.HandInfoFast");
            handUtilsClass = Class.forName("com.donohoedigital.games.poker.engine.HandUtils");

            cardGetCard = cardClass.getMethod("getCard", int.class, int.class);
            handCtor = handClass.getConstructor(int.class);
            handAddCard = handClass.getMethod("addCard", cardClass);
            handGetCard = handClass.getMethod("getCard", int.class);
            handSize = handClass.getMethod("size");
            handSortedFromHand = handSortedClass.getConstructor(handClass);

            getScoreMethod = handInfoFastClass.getMethod("getScore", handClass, handClass);
            getTypeFromScoreMethod = handInfoFastClass.getMethod("getTypeFromScore", int.class);
            getCardsMethod = handInfoFastClass.getMethod("getCards", int.class, int[].class);
            getHandType = handInfoFastClass.getMethod("getHandType");
            getBigPairRank = handInfoFastClass.getMethod("getBigPairRank");
            getSmallPairRank = handInfoFastClass.getMethod("getSmallPairRank");
            getTripsRank = handInfoFastClass.getMethod("getTripsRank");
            getQuadsRank = handInfoFastClass.getMethod("getQuadsRank");
            getHighCardRank = handInfoFastClass.getMethod("getHighCardRank");
            getStraightHighRank = handInfoFastClass.getMethod("getStraightHighRank");
            getStraightLowRank = handInfoFastClass.getMethod("getStraightLowRank");
            getFlushHighRank = handInfoFastClass.getMethod("getFlushHighRank");

            getBestFive = handUtilsClass.getMethod("getBestFive", handSortedClass, handSortedClass);

            available = true;
        } catch (Exception e) {
            logger.warn("Hand evaluation engine not available on classpath: {}", e.getMessage());
            available = false;
        }
    }
}
