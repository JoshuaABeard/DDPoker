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
 * updates. Provides compatibility methods matching engine Hand API.
 */
public final class ClientHand {

    /** Hand type for normal cards. */
    public static final char TYPE_NORMAL = 'n';

    /** Hand type for color-up display. */
    public static final char TYPE_COLOR_UP = 'c';

    /** Hand type for deal-high. */
    public static final char TYPE_DEAL_HIGH = 'h';

    /** Hand type for face-up display. */
    public static final char TYPE_FACE_UP = 'f';

    private final List<ClientCard> cards;
    private char type = TYPE_NORMAL;
    private long fingerprint;

    private ClientHand(List<ClientCard> initial) {
        this.cards = new ArrayList<>(initial);
        recalcFingerprint();
    }

    private ClientHand(char handType) {
        this.cards = new ArrayList<>();
        this.type = handType;
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

    /** Create an empty hand with a type marker. */
    public static ClientHand ofType(char handType) {
        return new ClientHand(handType);
    }

    /** Create a hand from two cards. */
    public static ClientHand of(ClientCard c1, ClientCard c2) {
        return new ClientHand(List.of(c1, c2));
    }

    /** Create a hand from three cards. */
    public static ClientHand of(ClientCard c1, ClientCard c2, ClientCard c3) {
        return new ClientHand(List.of(c1, c2, c3));
    }

    /** Create a hand from four cards. */
    public static ClientHand of(ClientCard c1, ClientCard c2, ClientCard c3, ClientCard c4) {
        return new ClientHand(List.of(c1, c2, c3, c4));
    }

    /** Create a hand from five cards. */
    public static ClientHand of(ClientCard c1, ClientCard c2, ClientCard c3, ClientCard c4, ClientCard c5) {
        return new ClientHand(List.of(c1, c2, c3, c4, c5));
    }

    /** Create a hand with initial capacity hint (ignored, for compatibility). */
    public static ClientHand withCapacity(int capacity) {
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
        recalcFingerprint();
    }

    /** Alias for {@link #addCard(ClientCard)} for engine Hand compatibility. */
    public void add(ClientCard card) {
        addCard(card);
    }

    /** Replace the card at the given index. */
    public void setCard(int index, ClientCard card) {
        cards.set(index, card);
        recalcFingerprint();
    }

    /** Returns the index of the first card matching rank and suit, or -1. */
    public int indexOf(ClientCard card) {
        return cards.indexOf(card);
    }

    /** Notify that cards may have changed externally. Recalculates fingerprint. */
    public void cardsChanged() {
        recalcFingerprint();
    }

    /** Remove all cards from the hand. */
    public void clear() {
        cards.clear();
        fingerprint = 0;
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

    /** Returns the hand type marker. */
    public char getType() {
        return type;
    }

    /** Returns a fingerprint for change detection. */
    public long fingerprint() {
        return fingerprint;
    }

    /**
     * Returns the highest rank in the hand, or 0 if empty.
     */
    public int getHighestRank() {
        int max = 0;
        for (ClientCard c : cards) {
            if (c.rank() > max)
                max = c.rank();
        }
        return max;
    }

    /**
     * Returns the lowest rank in the hand, or Integer.MAX_VALUE if empty.
     */
    public int getLowestRank() {
        int min = Integer.MAX_VALUE;
        for (ClientCard c : cards) {
            if (c.rank() < min)
                min = c.rank();
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    /**
     * Returns true if both cards have the same suit (suited hand).
     */
    public boolean isSuited() {
        if (cards.size() < 2)
            return false;
        return cards.get(0).suit() == cards.get(1).suit();
    }

    /**
     * Returns true if both cards have the same rank (pair).
     */
    public boolean isPair() {
        if (cards.size() < 2)
            return false;
        return cards.get(0).rank() == cards.get(1).rank();
    }

    /**
     * Returns true if both cards are connectors (gap <= specified).
     */
    public boolean isConnectors(int rank1, int rank2) {
        return Math.abs(rank1 - rank2) == 1;
    }

    /**
     * Returns HTML representation of all cards in the hand. Matches engine
     * Hand.toHTML().
     */
    public String toHTML() {
        StringBuilder sb = new StringBuilder();
        for (ClientCard c : cards) {
            sb.append(c.toHTML());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ClientHand other))
            return false;
        return cards.equals(other.cards);
    }

    @Override
    public int hashCode() {
        return cards.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(cards.get(i).display());
        }
        return sb.toString();
    }

    /**
     * Returns a suited-aware string like "[A K]*" for suited or "[A K]" for
     * unsuited. Matches engine Hand.toStringSuited().
     */
    public String toStringSuited() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(cards.get(i).getRankDisplaySingle());
        }
        sb.append(']');
        if (isSuited()) {
            sb.append('*');
        }
        return sb.toString();
    }

    /**
     * Returns rank-only string like "A K Q". Matches engine Hand.toStringRank().
     */
    public String toStringRank() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(cards.get(i).getRankDisplay());
        }
        return sb.toString();
    }

    /**
     * Returns rank+suit string like "Ah Ks Qd". Matches engine
     * Hand.toStringRankSuit().
     */
    public String toStringRankSuit() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(cards.get(i).getRankDisplay());
            sb.append(cards.get(i).getSuitDisplay());
        }
        return sb.toString();
    }

    private void recalcFingerprint() {
        long fp = 0;
        for (ClientCard c : cards) {
            if (c.rank() >= 2 && c.rank() <= 14 && c.suit() >= 0 && c.suit() <= 3) {
                fp |= 1L << ((c.rank() - 2) * 4 + c.suit());
            }
        }
        fingerprint = fp;
    }
}
