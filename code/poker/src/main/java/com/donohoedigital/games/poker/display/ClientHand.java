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
package com.donohoedigital.games.poker.display;

import java.util.*;

/**
 * Lightweight mutable hand for the client display layer. Holds a list of
 * {@link ClientCard} instances. Mutable to support incremental WebSocket
 * updates.
 */
public final class ClientHand {

    private final List<ClientCard> cards;

    private ClientHand(List<ClientCard> cards) {
        this.cards = new ArrayList<>(cards);
    }

    /** Create a hand from a list of card strings (e.g., "Ah", "Ks"). */
    public static ClientHand fromStrings(List<String> cardStrings) {
        return new ClientHand(ClientCard.parseAll(cardStrings));
    }

    /** Create a hand from a list of ClientCard instances. */
    public static ClientHand fromCards(List<ClientCard> cards) {
        return new ClientHand(cards);
    }

    /** Create an empty hand. */
    public static ClientHand empty() {
        return new ClientHand(List.of());
    }

    /** Number of cards in the hand. */
    public int size() {
        return cards.size();
    }

    /** Whether the hand is empty. */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /** Get the card at the given index. */
    public ClientCard getCard(int index) {
        return cards.get(index);
    }

    /** Get an unmodifiable view of all cards. */
    public List<ClientCard> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /** Add a card to the hand. */
    public void addCard(ClientCard card) {
        cards.add(card);
    }

    /** Remove all cards from the hand. */
    public void clear() {
        cards.clear();
    }

    /** Returns a new ClientHand sorted by rank descending. */
    public ClientHand sorted() {
        List<ClientCard> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparingInt(ClientCard::rank).reversed());
        return new ClientHand(sorted);
    }

    /** Check if the hand contains a card with matching rank and suit. */
    public boolean containsCard(ClientCard card) {
        return cards.contains(card);
    }
}
