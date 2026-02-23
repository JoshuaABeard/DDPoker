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
package com.donohoedigital.games.poker.gameserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.donohoedigital.games.poker.engine.Card;

/**
 * Thread-safe registry for injecting a specific deck into the next hand.
 *
 * <p>
 * The dev control server calls {@link #setCards(List)} or
 * {@link #setSeed(long)} before a hand starts.
 * {@link ServerGameTable#startNewHand()} calls {@link #takeDeck()} — which
 * atomically consumes any pending injection and returns a pre-configured
 * {@link ServerDeck}, or {@code null} to use the normal random shuffle.
 *
 * <p>
 * Card order for {@link #setCards(List)}: seat0-card1, seat0-card2,
 * seat1-card1, seat1-card2, …, burn, flop1, flop2, flop3, burn, turn, burn,
 * river (total must match the table's deal requirements — at minimum 2*numSeats
 * + 8 cards).
 */
public class CardInjectionRegistry {

    private static final long NO_SEED = Long.MIN_VALUE;

    private static final AtomicReference<List<Card>> pendingCards = new AtomicReference<>();
    private static final AtomicLong pendingSeed = new AtomicLong(NO_SEED);

    private CardInjectionRegistry() {
    }

    /** Stage a fixed card order for the next hand. Clears any pending seed. */
    public static void setCards(List<Card> cards) {
        pendingSeed.set(NO_SEED);
        pendingCards.set(new ArrayList<>(cards));
    }

    /**
     * Stage a seeded shuffle for the next hand. Clears any pending explicit card
     * list.
     *
     * <p>
     * Note: {@code Long.MIN_VALUE} is reserved as an internal sentinel and cannot
     * be used as a seed — it is silently treated as no-injection.
     */
    public static void setSeed(long seed) {
        pendingCards.set(null);
        pendingSeed.set(seed);
    }

    /**
     * Clear all pending injections. The next hand will use normal random shuffling.
     */
    public static void clear() {
        pendingCards.set(null);
        pendingSeed.set(NO_SEED);
    }

    /**
     * Called by {@link ServerGameTable#startNewHand()} before creating a
     * {@link ServerHand}. Atomically consumes any pending injection.
     *
     * @return a configured {@link ServerDeck}, or {@code null} if no injection is
     *         pending
     */
    public static ServerDeck takeDeck() {
        List<Card> cards = pendingCards.getAndSet(null);
        if (cards != null) {
            return new ServerDeck(cards);
        }
        long seed = pendingSeed.getAndSet(NO_SEED);
        if (seed != NO_SEED) {
            return new ServerDeck(seed);
        }
        return null;
    }
}
