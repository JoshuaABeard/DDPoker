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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.HandScoreConstants;
import com.donohoedigital.games.poker.protocol.dto.SimulationResult;

/**
 * Poker equity simulation service. Supports Monte Carlo and exhaustive
 * simulation for a single player vs opponents.
 */
@Service
public class PokerSimulationService implements HandScoreConstants {

    /** Maximum number of board+opponent combos allowed for exhaustive mode. */
    static final long EXHAUSTIVE_COMBO_LIMIT = 10_000;

    private static final String[] HAND_TYPE_NAMES = {null, // index 0 unused
            "HIGH_CARD", "ONE_PAIR", "TWO_PAIR", "TRIPS", "STRAIGHT", "FLUSH", "FULL_HOUSE", "FOUR_OF_A_KIND",
            "STRAIGHT_FLUSH", "ROYAL_FLUSH"};

    /**
     * Run a poker equity simulation for one player vs opponents.
     *
     * @param holeCards
     *            player's 2 hole cards as strings (e.g., "Ah", "Kd")
     * @param communityCards
     *            community cards (0-5), may be null or empty
     * @param numOpponents
     *            total number of opponents (1-9)
     * @param iterations
     *            number of Monte Carlo iterations (100-100000); required when
     *            exhaustive is false or null
     * @param knownOpponentHands
     *            optional list of known opponent hole card pairs, may be null
     * @param exhaustive
     *            when true, enumerate all possible board completions instead of
     *            Monte Carlo; iterations is ignored
     * @return simulation result with win/tie/loss percentages
     * @throws IllegalArgumentException
     *             if inputs are invalid or exhaustive combo count exceeds
     *             {@link #EXHAUSTIVE_COMBO_LIMIT}
     */
    public SimulationResult simulate(List<String> holeCards, List<String> communityCards, int numOpponents,
            Integer iterations, List<List<String>> knownOpponentHands, Boolean exhaustive) {

        List<Card> hole = parseCards(holeCards);
        List<Card> community = communityCards != null ? parseCards(communityCards) : List.of();
        List<List<Card>> knownOppHands = knownOpponentHands != null
                ? knownOpponentHands.stream().map(this::parseCards).toList()
                : List.of();

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

        List<Card> remainingDeck = buildRemainingDeck(hole, community, knownOppHands);
        int communityNeeded = 5 - community.size();

        if (!Boolean.TRUE.equals(exhaustive) && iterations == null) {
            throw new IllegalArgumentException("iterations must be provided for Monte Carlo mode");
        }

        if (Boolean.TRUE.equals(exhaustive)) {
            return runExhaustive(hole, community, knownOppHands, remainingDeck, numOpponents, communityNeeded,
                    randomOpponents);
        } else {
            return runMonteCarlo(hole, community, knownOppHands, remainingDeck, numOpponents, communityNeeded,
                    randomOpponents, iterations);
        }
    }

    private SimulationResult runExhaustive(List<Card> hole, List<Card> community, List<List<Card>> knownOppHands,
            List<Card> remainingDeck, int numOpponents, int communityNeeded, int randomOpponents) {

        long comboCount = countExhaustiveCombos(remainingDeck.size(), communityNeeded, randomOpponents);
        if (comboCount > EXHAUSTIVE_COMBO_LIMIT) {
            throw new IllegalArgumentException("Exhaustive mode would require " + comboCount
                    + " combinations, which exceeds the limit of " + EXHAUSTIVE_COMBO_LIMIT);
        }

        int wins = 0;
        int ties = 0;
        int losses = 0;
        int[] oppWins = new int[numOpponents];
        int[] oppTies = new int[numOpponents];
        int[] oppLosses = new int[numOpponents];
        Map<String, Integer> handTypeCounts = new HashMap<>();

        ServerHandEvaluator evaluator = new ServerHandEvaluator();

        // Generate all board completions
        List<List<Card>> boardCompletions = new ArrayList<>();
        generateCombinations(remainingDeck, communityNeeded, 0, new ArrayList<>(), boardCompletions);

        for (List<Card> boardCompletion : boardCompletions) {
            List<Card> board = new ArrayList<>(community);
            board.addAll(boardCompletion);

            // Build the deck remaining after board cards
            Set<Integer> usedOnBoard = new HashSet<>();
            for (Card c : boardCompletion) {
                usedOnBoard.add(c.getIndex());
            }
            List<Card> afterBoard = new ArrayList<>();
            for (Card c : remainingDeck) {
                if (!usedOnBoard.contains(c.getIndex())) {
                    afterBoard.add(c);
                }
            }

            // Generate all random opponent combinations
            List<List<List<Card>>> oppCombos = new ArrayList<>();
            generateRandomOpponentCombos(afterBoard, randomOpponents, new ArrayList<>(), oppCombos);

            if (oppCombos.isEmpty()) {
                oppCombos.add(List.of()); // no random opponents
            }

            for (List<List<Card>> randOppHands : oppCombos) {
                int playerScore = evaluator.getScore(hole, board);

                // Track hand type breakdown
                int handTypeIdx = playerScore / SCORE_BASE;
                if (handTypeIdx >= 1 && handTypeIdx < HAND_TYPE_NAMES.length) {
                    String handName = HAND_TYPE_NAMES[handTypeIdx];
                    handTypeCounts.merge(handName, 1, Integer::sum);
                }

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

                for (List<Card> randOppHand : randOppHands) {
                    int oppScore = evaluator.getScore(randOppHand, board);
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
        }

        int totalIters = wins + ties + losses;
        return buildResult(wins, ties, losses, totalIters, numOpponents, knownOppHands, oppWins, oppTies, oppLosses,
                handTypeCounts);
    }

    private SimulationResult runMonteCarlo(List<Card> hole, List<Card> community, List<List<Card>> knownOppHands,
            List<Card> remainingDeck, int numOpponents, int communityNeeded, int randomOpponents, int iterations) {

        int wins = 0;
        int ties = 0;
        int losses = 0;
        int[] oppWins = new int[numOpponents];
        int[] oppTies = new int[numOpponents];
        int[] oppLosses = new int[numOpponents];
        Map<String, Integer> handTypeCounts = new HashMap<>();

        Random rng = new Random();
        ServerHandEvaluator evaluator = new ServerHandEvaluator();
        List<Card> shuffled = new ArrayList<>(remainingDeck);

        for (int iter = 0; iter < iterations; iter++) {
            Collections.shuffle(shuffled, rng);
            int dealIndex = 0;

            List<Card> board = new ArrayList<>(community);
            for (int j = 0; j < communityNeeded; j++) {
                board.add(shuffled.get(dealIndex++));
            }

            int playerScore = evaluator.getScore(hole, board);

            // Track hand type breakdown
            int handTypeIdx = playerScore / SCORE_BASE;
            if (handTypeIdx >= 1 && handTypeIdx < HAND_TYPE_NAMES.length) {
                String handName = HAND_TYPE_NAMES[handTypeIdx];
                handTypeCounts.merge(handName, 1, Integer::sum);
            }

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

        return buildResult(wins, ties, losses, iterations, numOpponents, knownOppHands, oppWins, oppTies, oppLosses,
                handTypeCounts);
    }

    private SimulationResult buildResult(int wins, int ties, int losses, int iters, int numOpponents,
            List<List<Card>> knownOppHands, int[] oppWins, int[] oppTies, int[] oppLosses,
            Map<String, Integer> handTypeCounts) {

        List<SimulationResult.OpponentResult> opponentResults = null;
        if (!knownOppHands.isEmpty()) {
            opponentResults = new ArrayList<>();
            for (int i = 0; i < numOpponents; i++) {
                opponentResults.add(new SimulationResult.OpponentResult((double) oppWins[i] / iters * 100,
                        (double) oppTies[i] / iters * 100, (double) oppLosses[i] / iters * 100));
            }
        }

        Map<String, Double> breakdown = new HashMap<>();
        for (Map.Entry<String, Integer> entry : handTypeCounts.entrySet()) {
            breakdown.put(entry.getKey(), (double) entry.getValue() / iters * 100);
        }

        return new SimulationResult((double) wins / iters * 100, (double) ties / iters * 100,
                (double) losses / iters * 100, iters, opponentResults, breakdown);
    }

    /**
     * Count the total number of board+opponent combinations for exhaustive mode.
     *
     * @param deckSize
     *            number of remaining cards in the deck
     * @param communityNeeded
     *            number of community cards still to be dealt
     * @param randomOpponents
     *            number of random (unknown) opponents
     * @return total number of combinations, or {@link Long#MAX_VALUE} if it
     *         overflows
     */
    public static long countExhaustiveCombos(int deckSize, int communityNeeded, int randomOpponents) {
        long total = combinations(deckSize, communityNeeded);
        if (total == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        // For each random opponent, we pick 2 cards from the deck that remains after
        // dealing the board and any previous opponent cards.
        int remaining = deckSize - communityNeeded;
        for (int o = 0; o < randomOpponents; o++) {
            long oppCombos = combinations(remaining, 2);
            if (oppCombos == Long.MAX_VALUE || total > Long.MAX_VALUE / oppCombos) {
                return Long.MAX_VALUE;
            }
            total *= oppCombos;
            remaining -= 2;
        }
        return total;
    }

    private static long combinations(int n, int k) {
        if (k < 0 || k > n) {
            return 0;
        }
        if (k == 0 || k == n) {
            return 1;
        }
        if (k > n - k) {
            k = n - k;
        }
        long result = 1;
        for (int i = 0; i < k; i++) {
            if (result > Long.MAX_VALUE / (n - i)) {
                return Long.MAX_VALUE;
            }
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    private void generateCombinations(List<Card> deck, int k, int start, List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < deck.size(); i++) {
            current.add(deck.get(i));
            generateCombinations(deck, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private void generateRandomOpponentCombos(List<Card> deck, int randomOpponents, List<List<Card>> current,
            List<List<List<Card>>> result) {
        if (randomOpponents == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = 0; i < deck.size() - 1; i++) {
            for (int j = i + 1; j < deck.size(); j++) {
                List<Card> oppHand = List.of(deck.get(i), deck.get(j));
                List<Card> remaining = new ArrayList<>(deck);
                remaining.remove(j);
                remaining.remove(i);
                current.add(oppHand);
                generateRandomOpponentCombos(remaining, randomOpponents - 1, current, result);
                current.remove(current.size() - 1);
            }
        }
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
