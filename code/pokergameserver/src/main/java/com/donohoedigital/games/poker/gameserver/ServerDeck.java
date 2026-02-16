/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.donohoedigital.games.poker.engine.Card;

/**
 * Server-side deck implementation without dependencies. Uses standard 52-card
 * deck with SecureRandom shuffling.
 */
public class ServerDeck {
    private final List<Card> cards;
    private int nextCardIndex;
    private final SecureRandom random;

    /**
     * Create and shuffle a new deck.
     */
    public ServerDeck() {
        this(true);
    }

    /**
     * Create a deck, optionally shuffled.
     *
     * @param shuffle
     *            true to shuffle the deck
     */
    public ServerDeck(boolean shuffle) {
        this.cards = new ArrayList<>(52);
        this.random = new SecureRandom();
        this.nextCardIndex = 0;

        // Initialize standard 52-card deck
        initializeDeck();

        if (shuffle) {
            shuffle();
        }
    }

    private void initializeDeck() {
        // Spades
        cards.add(Card.SPADES_2);
        cards.add(Card.SPADES_3);
        cards.add(Card.SPADES_4);
        cards.add(Card.SPADES_5);
        cards.add(Card.SPADES_6);
        cards.add(Card.SPADES_7);
        cards.add(Card.SPADES_8);
        cards.add(Card.SPADES_9);
        cards.add(Card.SPADES_T);
        cards.add(Card.SPADES_J);
        cards.add(Card.SPADES_Q);
        cards.add(Card.SPADES_K);
        cards.add(Card.SPADES_A);

        // Hearts
        cards.add(Card.HEARTS_2);
        cards.add(Card.HEARTS_3);
        cards.add(Card.HEARTS_4);
        cards.add(Card.HEARTS_5);
        cards.add(Card.HEARTS_6);
        cards.add(Card.HEARTS_7);
        cards.add(Card.HEARTS_8);
        cards.add(Card.HEARTS_9);
        cards.add(Card.HEARTS_T);
        cards.add(Card.HEARTS_J);
        cards.add(Card.HEARTS_Q);
        cards.add(Card.HEARTS_K);
        cards.add(Card.HEARTS_A);

        // Diamonds
        cards.add(Card.DIAMONDS_2);
        cards.add(Card.DIAMONDS_3);
        cards.add(Card.DIAMONDS_4);
        cards.add(Card.DIAMONDS_5);
        cards.add(Card.DIAMONDS_6);
        cards.add(Card.DIAMONDS_7);
        cards.add(Card.DIAMONDS_8);
        cards.add(Card.DIAMONDS_9);
        cards.add(Card.DIAMONDS_T);
        cards.add(Card.DIAMONDS_J);
        cards.add(Card.DIAMONDS_Q);
        cards.add(Card.DIAMONDS_K);
        cards.add(Card.DIAMONDS_A);

        // Clubs
        cards.add(Card.CLUBS_2);
        cards.add(Card.CLUBS_3);
        cards.add(Card.CLUBS_4);
        cards.add(Card.CLUBS_5);
        cards.add(Card.CLUBS_6);
        cards.add(Card.CLUBS_7);
        cards.add(Card.CLUBS_8);
        cards.add(Card.CLUBS_9);
        cards.add(Card.CLUBS_T);
        cards.add(Card.CLUBS_J);
        cards.add(Card.CLUBS_Q);
        cards.add(Card.CLUBS_K);
        cards.add(Card.CLUBS_A);
    }

    /**
     * Shuffle the deck using SecureRandom.
     */
    public void shuffle() {
        Collections.shuffle(cards, random);
        nextCardIndex = 0;
    }

    /**
     * Deal the next card from the deck.
     *
     * @return next card
     * @throws IllegalStateException
     *             if deck is empty
     */
    public Card nextCard() {
        if (nextCardIndex >= cards.size()) {
            throw new IllegalStateException("Deck is empty");
        }
        return cards.get(nextCardIndex++);
    }

    /**
     * Get number of cards remaining in deck.
     *
     * @return cards left
     */
    public int size() {
        return cards.size() - nextCardIndex;
    }

    /**
     * Check if deck is empty.
     *
     * @return true if no cards left
     */
    public boolean isEmpty() {
        return nextCardIndex >= cards.size();
    }
}
