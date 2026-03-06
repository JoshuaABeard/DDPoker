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

import java.util.List;

/**
 * Lightweight card representation for the client display layer. Suit indices
 * match pokerengine's ClientCard constants: clubs=0, diamonds=1, hearts=2,
 * spades=3. Rank values match pokerengine: 2-14 (2=Two through 14=Ace).
 */
public record ClientCard(int rank, int suit) {

    // Rank constants (matching pokerengine ClientCard)
    public static final int ACE = 14;
    public static final int KING = 13;
    public static final int QUEEN = 12;
    public static final int JACK = 11;
    public static final int TEN = 10;
    public static final int NINE = 9;
    public static final int EIGHT = 8;
    public static final int SEVEN = 7;
    public static final int SIX = 6;
    public static final int FIVE = 5;
    public static final int FOUR = 4;
    public static final int THREE = 3;
    public static final int TWO = 2;
    public static final int UNKNOWN = 0;

    // Suit constants (matching pokerengine ClientCard)
    public static final int CLUBS = 0;
    public static final int DIAMONDS = 1;
    public static final int HEARTS = 2;
    public static final int SPADES = 3;

    /** Blank/unknown card. */
    public static final ClientCard BLANK = new ClientCard(0, -1);

    /** Well-known card constants. */
    public static final ClientCard SPADES_A = new ClientCard(ACE, SPADES);

    private static final char[] SUIT_CHARS = {'c', 'd', 'h', 's'};
    private static final String[] SUIT_NAMES = {"Clubs", "Diamonds", "Hearts", "Spades"};
    private static final String[] RANK_NAMES = {null, null, "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack",
            "Queen", "King", "Ace"};
    private static final char[] RANK_CHARS = {0, 0, '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A'};

    // Flyweight card cache for all 52 cards + blank
    private static final ClientCard[] CARDS = new ClientCard[53];

    static {
        for (int s = 0; s <= 3; s++) {
            for (int r = TWO; r <= ACE; r++) {
                CARDS[(r - 2) * 4 + s] = new ClientCard(r, s);
            }
        }
        CARDS[52] = BLANK;
    }

    /**
     * Get a cached card instance by suit and rank.
     */
    public static ClientCard getCard(int suit, int rank) {
        if (rank < TWO || rank > ACE || suit < 0 || suit > 3) {
            return BLANK;
        }
        return CARDS[(rank - 2) * 4 + suit];
    }

    /**
     * Get a card from a two-character string like "Ah", "Td", "2c". Null-safe
     * version of {@link #parse(String)} — returns {@link #BLANK} for null/empty
     * input.
     */
    public static ClientCard getCard(String s) {
        if (s == null || s.isEmpty()) {
            return BLANK;
        }
        return parse(s);
    }

    /**
     * Parse a two-character card string like "Ah", "Td", "2c".
     */
    public static ClientCard parse(String s) {
        if (s == null || s.length() < 2) {
            throw new IllegalArgumentException("Invalid card string: " + s);
        }
        int r = parseRank(s.charAt(0));
        int su = parseSuit(s.charAt(1));
        if (r == 0 || su == -1) {
            throw new IllegalArgumentException("Invalid card string: " + s);
        }
        return getCard(su, r);
    }

    /**
     * Parse a list of card strings into ClientCard instances.
     */
    public static List<ClientCard> parseAll(List<String> cards) {
        return cards.stream().map(ClientCard::parse).toList();
    }

    /**
     * Get rank display string from a rank constant. Matches engine
     * ClientCard.getRank(int).
     */
    public static String getRank(int rank) {
        if (rank < 0 || rank > ACE)
            return "?";
        if (rank == 1 || rank == ACE)
            return "A";
        if (rank == KING)
            return "K";
        if (rank == QUEEN)
            return "Q";
        if (rank == JACK)
            return "J";
        if (rank == UNKNOWN)
            return "?";
        return Integer.toString(rank);
    }

    /**
     * Get single-char rank display from a rank constant. Matches engine
     * ClientCard.getRankSingle(int).
     */
    public static String getRankSingle(int rank) {
        if (rank == 10)
            return "T";
        return getRank(rank);
    }

    /**
     * Parse a rank character to its int value. Matches engine
     * ClientCard.getRank(char).
     */
    public static int getRank(char c) {
        return parseRank(c);
    }

    /** Whether this is a blank/unknown card. */
    public boolean isBlank() {
        return rank == 0;
    }

    /**
     * Two-char string for serialization/REST (e.g. "Ah", "Tc"). Same as
     * {@link #display()} but named for engine Card compatibility.
     */
    public String toStringSingle() {
        return display();
    }

    /** Whether this card is hearts. */
    public boolean isHearts() {
        return suit == HEARTS;
    }

    /** Whether this card is spades. */
    public boolean isSpades() {
        return suit == SPADES;
    }

    /** Whether this card is clubs. */
    public boolean isClubs() {
        return suit == CLUBS;
    }

    /** Whether this card is diamonds. */
    public boolean isDiamonds() {
        return suit == DIAMONDS;
    }

    /** Two-character display string, e.g., "Ah", "Td". */
    public String display() {
        if (isBlank())
            return "??";
        return String.valueOf(rankChar()) + suitChar();
    }

    /**
     * Compatibility method matching engine Card.getDisplay(). Returns rank display
     * + suit abbreviation, e.g. "Ac", "10h", "Ks".
     */
    public String getDisplay() {
        if (isBlank())
            return "??";
        return getRank(rank) + suitChar();
    }

    /**
     * Compatibility method matching engine Card.getRankDisplay().
     */
    public String getRankDisplay() {
        return getRank(rank);
    }

    /**
     * Compatibility method matching engine Card.getRankDisplaySingle().
     */
    public String getRankDisplaySingle() {
        return getRankSingle(rank);
    }

    /**
     * Compatibility method matching engine Card.getSuitDisplay().
     */
    public String getSuitDisplay() {
        if (suit < 0 || suit > 3)
            return "?";
        return String.valueOf(SUIT_CHARS[suit]);
    }

    /**
     * Compatibility method matching engine Card.toHTML().
     */
    public String toHTML() {
        return "<DDCARD CARD=\"" + getRankDisplaySingle() + getSuitDisplay() + "\">";
    }

    /**
     * Compatibility method for engine ClientCard.getRank().
     */
    public int getRank() {
        return rank;
    }

    /**
     * Compatibility method for engine Card.getSuit().
     */
    public int getSuit() {
        return suit;
    }

    /** Single character for rank: 'A', 'K', 'Q', 'J', 'T', '9', ..., '2'. */
    public char rankChar() {
        if (rank < 2 || rank > 14)
            return '?';
        return RANK_CHARS[rank];
    }

    /** Single character for suit: 's', 'c', 'd', 'h'. */
    public char suitChar() {
        if (suit < 0 || suit > 3)
            return '?';
        return SUIT_CHARS[suit];
    }

    /**
     * Full rank name: "Ace", "King", "Queen", "Jack", "Ten", or the digit string.
     */
    public String rankDisplay() {
        if (rank < 2 || rank > 14)
            return "?";
        return RANK_NAMES[rank];
    }

    /** Full suit name: "Spades", "Clubs", "Diamonds", "Hearts". */
    public String suitDisplay() {
        if (suit < 0 || suit > 3)
            return "?";
        return SUIT_NAMES[suit];
    }

    /** Whether this is a face card (Jack, Queen, or King). */
    public boolean isFaceCard() {
        return rank >= JACK && rank <= KING;
    }

    /** Whether this card is the same suit as another. */
    public boolean isSameSuit(ClientCard c) {
        return c != null && c.suit == suit;
    }

    /** Whether this card is the same rank as another. */
    public boolean isSameRank(ClientCard c) {
        return c != null && c.rank == rank;
    }

    /** Whether this card is higher rank than another. */
    public boolean isGreaterThan(ClientCard c) {
        if (c == null)
            return true;
        return rank > c.rank || (rank == c.rank && suit > c.suit);
    }

    @Override
    public String toString() {
        return display();
    }

    private static int parseRank(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'A' -> 14;
            case 'K' -> 13;
            case 'Q' -> 12;
            case 'J' -> 11;
            case 'T' -> 10;
            case '9' -> 9;
            case '8' -> 8;
            case '7' -> 7;
            case '6' -> 6;
            case '5' -> 5;
            case '4' -> 4;
            case '3' -> 3;
            case '2' -> 2;
            default -> 0;
        };
    }

    private static int parseSuit(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'c' -> 0; // clubs
            case 'd' -> 1; // diamonds
            case 'h' -> 2; // hearts
            case 's' -> 3; // spades
            default -> -1;
        };
    }
}
