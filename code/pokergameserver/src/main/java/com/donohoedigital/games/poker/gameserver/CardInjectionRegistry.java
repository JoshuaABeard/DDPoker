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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.donohoedigital.games.poker.engine.Card;

/**
 * Thread-safe registry for injecting specific decks into upcoming hands.
 *
 * <p>
 * The dev control server calls {@link #setCards(List)} or
 * {@link #setSeed(long)} before a hand starts.
 * {@link ServerGameTable#startNewHand()} calls {@link #takeDeck()} — which
 * atomically consumes the next pending injection and returns a pre-configured
 * {@link ServerDeck}, or {@code null} to use the normal random shuffle.
 *
 * <p>
 * Multiple injections can be queued: each {@link #setCards(List)} call enqueues
 * a deck, and each {@link #takeDeck()} call dequeues the next one. This
 * supports test scripts that stage cards for several upcoming hands at once.
 *
 * <p>
 * Card order for {@link #setCards(List)}: seat0-card1, seat0-card2,
 * seat1-card1, seat1-card2, …, burn, flop1, flop2, flop3, burn, turn, burn,
 * river (total must match the table's deal requirements — at minimum 2*numSeats
 * + 8 cards).
 */
public class CardInjectionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(CardInjectionRegistry.class);
    private static final long NO_SEED = Long.MIN_VALUE;

    private static final ConcurrentLinkedQueue<List<Card>> pendingDecks = new ConcurrentLinkedQueue<>();
    private static final AtomicLong pendingSeed = new AtomicLong(NO_SEED);

    private CardInjectionRegistry() {
    }

    /**
     * Enqueue a fixed card order for an upcoming hand. Multiple calls queue
     * multiple decks; each {@link #takeDeck()} consumes the next one in order.
     * Clears any pending seed (seed and card-list modes are mutually exclusive).
     */
    public static void setCards(List<Card> cards) {
        pendingSeed.set(NO_SEED);
        pendingDecks.add(new ArrayList<>(cards));
        logger.info("[INJECT] setCards: {} cards queued (first 4: {}, queue size: {})", cards.size(),
                cards.subList(0, Math.min(4, cards.size())), pendingDecks.size());
    }

    /**
     * Stage a seeded shuffle for the next hand. Clears any pending card queues
     * (seed and card-list modes are mutually exclusive).
     *
     * <p>
     * Note: {@code Long.MIN_VALUE} is reserved as an internal sentinel and cannot
     * be used as a seed — it is silently treated as no-injection.
     */
    public static void setSeed(long seed) {
        pendingDecks.clear();
        pendingSeed.set(seed);
    }

    /**
     * Clear all pending injections. The next hand will use normal random shuffling.
     */
    public static void clear() {
        pendingDecks.clear();
        pendingSeed.set(NO_SEED);
    }

    /**
     * Called by {@link ServerGameTable#startNewHand()} before creating a
     * {@link ServerHand}. Atomically consumes the next pending injection.
     *
     * @return a configured {@link ServerDeck}, or {@code null} if no injection is
     *         pending
     */
    public static ServerDeck takeDeck() {
        List<Card> cards = pendingDecks.poll();
        if (cards != null) {
            logger.info("[INJECT] takeDeck: consuming {} cards (first 4: {}, remaining: {})", cards.size(),
                    cards.subList(0, Math.min(4, cards.size())), pendingDecks.size());
            return new ServerDeck(cards);
        }
        long seed = pendingSeed.getAndSet(NO_SEED);
        if (seed != NO_SEED) {
            logger.info("[INJECT] takeDeck: consuming seed {}", seed);
            return new ServerDeck(seed);
        }
        logger.debug("[INJECT] takeDeck: no injection pending");
        return null;
    }
}
