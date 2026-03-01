/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.donohoedigital.games.poker.engine.Card;

/**
 * Monte Carlo poker equity simulation service. Runs configurable iterations to
 * estimate win/tie/loss percentages for a given hand against opponents.
 */
@Service
public class PokerSimulationService {

    /**
     * Run a Monte Carlo equity simulation.
     *
     * @param holeCards
     *            player's 2 hole cards as strings (e.g., "Ah", "Kd")
     * @param communityCards
     *            community cards (0-5), may be null or empty
     * @param numOpponents
     *            total number of opponents (1-9)
     * @param iterations
     *            number of Monte Carlo iterations (100-100000)
     * @param knownOpponentHands
     *            optional list of known opponent hole card pairs, may be null
     * @return simulation result with win/tie/loss percentages
     */
    public SimulationResult simulate(List<String> holeCards, List<String> communityCards, int numOpponents,
            int iterations, List<List<String>> knownOpponentHands) {

        // Parse and validate cards
        List<Card> hole = parseCards(holeCards);
        List<Card> community = communityCards != null ? parseCards(communityCards) : List.of();
        List<List<Card>> knownOppHands = knownOpponentHands != null
                ? knownOpponentHands.stream().map(this::parseCards).toList()
                : List.of();

        // Validate no duplicate cards
        validateNoDuplicates(hole, community, knownOppHands);

        for (List<Card> oppHand : knownOppHands) {
            if (oppHand.size() != 2) {
                throw new IllegalArgumentException(
                        "Each opponent hand must have exactly 2 cards, got " + oppHand.size());
            }
        }

        int randomOpponents = numOpponents - knownOppHands.size();
        if (randomOpponents < 0) {
            throw new IllegalArgumentException("Number of known opponent hands (" + knownOppHands.size()
                    + ") exceeds numOpponents (" + numOpponents + ")");
        }
        int communityNeeded = 5 - community.size();

        // Build remaining deck (remove all known cards)
        List<Card> remainingDeck = buildRemainingDeck(hole, community, knownOppHands);

        // Tally results
        int wins = 0;
        int ties = 0;
        int losses = 0;
        int[] oppWins = new int[numOpponents];
        int[] oppTies = new int[numOpponents];
        int[] oppLosses = new int[numOpponents];

        Random rng = new Random();
        ServerHandEvaluator evaluator = new ServerHandEvaluator();
        List<Card> shuffled = new ArrayList<>(remainingDeck);

        for (int iter = 0; iter < iterations; iter++) {
            Collections.shuffle(shuffled, rng);
            int dealIndex = 0;

            // Deal remaining community cards
            List<Card> board = new ArrayList<>(community);
            for (int j = 0; j < communityNeeded; j++) {
                board.add(shuffled.get(dealIndex++));
            }

            // Evaluate player hand
            int playerScore = evaluator.getScore(hole, board);

            // Evaluate known opponents first, then random
            boolean playerWins = true;
            boolean playerTied = false;
            int oppIdx = 0;

            for (List<Card> knownOppHand : knownOppHands) {
                int oppScore = evaluator.getScore(knownOppHand, board);
                if (oppScore > playerScore) {
                    playerWins = false;
                    oppWins[oppIdx]++;
                } else if (oppScore == playerScore) {
                    playerTied = true;
                    oppTies[oppIdx]++;
                } else {
                    oppLosses[oppIdx]++;
                }
                oppIdx++;
            }

            for (int o = 0; o < randomOpponents; o++) {
                List<Card> oppHand = List.of(shuffled.get(dealIndex++), shuffled.get(dealIndex++));
                int oppScore = evaluator.getScore(oppHand, board);
                if (oppScore > playerScore) {
                    playerWins = false;
                    oppWins[oppIdx]++;
                } else if (oppScore == playerScore) {
                    playerTied = true;
                    oppTies[oppIdx]++;
                } else {
                    oppLosses[oppIdx]++;
                }
                oppIdx++;
            }

            if (!playerWins) {
                losses++;
            } else if (playerTied) {
                ties++;
            } else {
                wins++;
            }
        }

        // Build per-opponent results only if known opponents were provided
        List<SimulationResult.OpponentResult> opponentResults = null;
        if (!knownOppHands.isEmpty()) {
            opponentResults = new ArrayList<>();
            for (int i = 0; i < numOpponents; i++) {
                opponentResults.add(new SimulationResult.OpponentResult((double) oppWins[i] / iterations * 100,
                        (double) oppTies[i] / iterations * 100, (double) oppLosses[i] / iterations * 100));
            }
        }

        return new SimulationResult((double) wins / iterations * 100, (double) ties / iterations * 100,
                (double) losses / iterations * 100, iterations, opponentResults);
    }

    /**
     * Parse string card representations to Card objects.
     *
     * @param cardStrings
     *            list of card strings (e.g., "Ah", "Kd")
     * @return list of Card objects
     * @throws IllegalArgumentException
     *             if any card string is invalid
     */
    List<Card> parseCards(List<String> cardStrings) {
        List<Card> cards = new ArrayList<>(cardStrings.size());
        for (String s : cardStrings) {
            Card card = Card.getCard(s);
            if (card == null) {
                throw new IllegalArgumentException("Invalid card: " + s);
            }
            cards.add(card);
        }
        return cards;
    }

    private void validateNoDuplicates(List<Card> hole, List<Card> community, List<List<Card>> knownOppHands) {
        Set<Integer> seen = new HashSet<>();
        for (Card c : hole) {
            if (!seen.add(c.getIndex())) {
                throw new IllegalArgumentException("Duplicate card at index: " + c.getIndex());
            }
        }
        for (Card c : community) {
            if (!seen.add(c.getIndex())) {
                throw new IllegalArgumentException("Duplicate card at index: " + c.getIndex());
            }
        }
        for (List<Card> oppHand : knownOppHands) {
            for (Card c : oppHand) {
                if (!seen.add(c.getIndex())) {
                    throw new IllegalArgumentException("Duplicate card at index: " + c.getIndex());
                }
            }
        }
    }

    private List<Card> buildRemainingDeck(List<Card> hole, List<Card> community, List<List<Card>> knownOppHands) {
        Set<Integer> usedIndices = new HashSet<>();
        for (Card c : hole) {
            usedIndices.add(c.getIndex());
        }
        for (Card c : community) {
            usedIndices.add(c.getIndex());
        }
        for (List<Card> oppHand : knownOppHands) {
            for (Card c : oppHand) {
                usedIndices.add(c.getIndex());
            }
        }

        List<Card> remaining = new ArrayList<>(52 - usedIndices.size());
        for (int i = 0; i < 52; i++) {
            if (!usedIndices.contains(i)) {
                remaining.add(Card.getCard(i));
            }
        }
        return remaining;
    }
}
