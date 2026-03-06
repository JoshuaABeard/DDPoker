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
 * match pokerengine's Card constants: clubs=0, diamonds=1, hearts=2, spades=3.
 * Rank values match pokerengine: 2-14 (2=Two through 14=Ace).
 */
public record ClientCard(int rank, int suit) {

    /** Blank/unknown card. */
    public static final ClientCard BLANK = new ClientCard(0, -1);

    private static final char[] SUIT_CHARS = {'c', 'd', 'h', 's'};
    private static final String[] SUIT_NAMES = {"Clubs", "Diamonds", "Hearts", "Spades"};
    private static final String[] RANK_NAMES = {null, null, "2", "3", "4", "5", "6", "7", "8", "9", "Ten", "Jack",
            "Queen", "King", "Ace"};
    private static final char[] RANK_CHARS = {0, 0, '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A'};

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
        return new ClientCard(r, su);
    }

    /**
     * Parse a list of card strings into ClientCard instances.
     */
    public static List<ClientCard> parseAll(List<String> cards) {
        return cards.stream().map(ClientCard::parse).toList();
    }

    /** Whether this is a blank/unknown card. */
    public boolean isBlank() {
        return rank == 0;
    }

    /** Two-character display string, e.g., "Ah", "Td". */
    public String display() {
        if (isBlank())
            return "??";
        return String.valueOf(rankChar()) + suitChar();
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
