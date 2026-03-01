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
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.HandScoreConstants;

/**
 * Stateless advisor service that computes hand evaluation, Monte Carlo equity,
 * pot odds, starting hand category, and recommendation text.
 */
public class AdvisorService implements HandScoreConstants {

    private static final String[] RANK_NAMES = {"", "", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Jack", "Queen", "King", "Ace"};

    private static final String[] RANK_NAMES_PLURAL = {"", "", "Twos", "Threes", "Fours", "Fives", "Sixes", "Sevens",
            "Eights", "Nines", "Tens", "Jacks", "Queens", "Kings", "Aces"};

    private static final String[] RANK_LABELS = {"", "", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K",
            "A"};

    /**
     * 13x13 grid of starting hand categories. Row = first rank (A=0, K=1, ...,
     * 2=12), Col = second rank. Upper-right triangle (col > row) = suited.
     * Lower-left triangle (row > col) = offsuit. Diagonal = pocket pairs.
     */
    // @formatter:off
    private static final String[][] STARTING_HAND_CATEGORIES = {
        //         A          K          Q          J          T          9          8          7          6          5          4          3          2
        /* A */ {"premium", "premium", "strong",  "strong",  "playable","playable","playable","playable","playable","playable","playable","playable","playable"},
        /* K */ {"strong",  "premium", "strong",  "playable","playable","marginal","fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold"    },
        /* Q */ {"playable","playable","premium", "playable","playable","marginal","fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold"    },
        /* J */ {"playable","marginal","marginal","strong",  "playable","marginal","fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold"    },
        /* T */ {"marginal","marginal","marginal","marginal","strong",  "playable","marginal","fold",    "fold",    "fold",    "fold",    "fold",    "fold"    },
        /* 9 */ {"fold",    "fold",    "fold",    "fold",    "fold",    "playable","playable","marginal","fold",    "fold",    "fold",    "fold",    "fold"    },
        /* 8 */ {"fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "playable","playable","marginal","fold",    "fold",    "fold",    "fold"    },
        /* 7 */ {"fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "playable","playable","marginal","fold",    "fold",    "fold"    },
        /* 6 */ {"fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "marginal","marginal","fold",    "fold",    "fold"    },
        /* 5 */ {"fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "marginal","marginal","fold",    "fold"    },
        /* 4 */ {"fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "marginal","fold",    "fold"    },
        /* 3 */ {"fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "marginal","fold"    },
        /* 2 */ {"fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "fold",    "marginal"},
    };
    // @formatter:on

    /**
     * Compute advisor data for a player's hand.
     *
     * @param holeCards
     *            player's 2 hole cards
     * @param communityCards
     *            community cards (0-5), may be empty
     * @param potSize
     *            current pot size
     * @param callAmount
     *            amount needed to call
     * @param numOpponents
     *            number of active opponents
     * @param iterations
     *            Monte Carlo iterations for equity calculation
     * @return advisor result
     */
    public AdvisorResult compute(Card[] holeCards, Card[] communityCards, int potSize, int callAmount, int numOpponents,
            int iterations) {
        return compute(holeCards, communityCards, potSize, callAmount, numOpponents, iterations, new Random());
    }

    /**
     * Compute advisor data with explicit Random for deterministic testing.
     */
    AdvisorResult compute(Card[] holeCards, Card[] communityCards, int potSize, int callAmount, int numOpponents,
            int iterations, Random random) {

        // Hand evaluation
        List<Card> holeList = List.of(holeCards);
        List<Card> communityList = communityCards.length > 0 ? List.of(communityCards) : List.of();

        ServerHandEvaluator evaluator = new ServerHandEvaluator();
        int score = evaluator.getScore(holeList, communityList);
        int handType = score / SCORE_BASE;
        // Map from server constants (1-10) to 0-9 range
        int handRank = handType - 1;

        // Hand description (only when 5+ cards available)
        int totalCards = holeCards.length + communityCards.length;
        String handDescription = totalCards >= 5 ? describeHand(handType, score, holeCards, communityCards) : null;

        // Equity via Monte Carlo
        double equity = calculateEquity(holeCards, communityCards, numOpponents, iterations, random);

        // Pot odds
        double potOdds = callAmount == 0 ? 0.0 : (double) callAmount / (potSize + callAmount) * 100.0;

        // Recommendation
        String recommendation = computeRecommendation(equity, potOdds, callAmount);

        // Starting hand category (pre-flop only)
        String startingHandCategory = null;
        String startingHandNotation = null;
        if (communityCards.length == 0 && holeCards.length == 2) {
            int rank1 = holeCards[0].getRank();
            int rank2 = holeCards[1].getRank();
            boolean suited = holeCards[0].getSuit() == holeCards[1].getSuit();

            int[] gridPos = holeCardsToGrid(rank1, rank2, suited);
            startingHandCategory = STARTING_HAND_CATEGORIES[gridPos[0]][gridPos[1]];
            startingHandNotation = getHandNotation(gridPos[0], gridPos[1]);
        }

        // Improvement odds (flop/turn only)
        Map<String, Double> improvementOdds = computeImprovementOdds(holeCards, communityCards, handType);

        // Hand potential (flop/turn only)
        double[] handPotential = computeHandPotential(holeCards, communityCards, random);
        Double positivePotential = handPotential == null ? null : handPotential[0];
        Double negativePotential = handPotential == null ? null : handPotential[1];

        return new AdvisorResult(handRank, handDescription, equity, potOdds, recommendation, startingHandCategory,
                startingHandNotation, improvementOdds, positivePotential, negativePotential);
    }

    /**
     * Map from HandScoreConstants hand type integer to hand type name. Index 0 is
     * unused (no hand type 0). Index 1 = HIGH_CARD. Used by improvementOdds (skips
     * null/HIGH_CARD entries since it is not an improvement target).
     */
    private static final String[] HAND_TYPE_NAMES = {null, // 0 - unused
            "HIGH_CARD", // 1 - HIGH_CARD
            "ONE_PAIR", // 2 - PAIR
            "TWO_PAIR", // 3 - TWO_PAIR
            "TRIPS", // 4 - TRIPS
            "STRAIGHT", // 5 - STRAIGHT
            "FLUSH", // 6 - FLUSH
            "FULL_HOUSE", // 7 - FULL_HOUSE
            "FOUR_OF_A_KIND", // 8 - QUADS
            "STRAIGHT_FLUSH", // 9 - STRAIGHT_FLUSH
            "ROYAL_FLUSH", // 10 - ROYAL_FLUSH
    };

    /**
     * Compute improvement odds by enumerating remaining deck cards. Only meaningful
     * on flop (3 community cards) or turn (4 community cards). Returns null
     * otherwise.
     *
     * @param holeCards
     *            player's hole cards
     * @param communityCards
     *            current community cards
     * @param currentHandType
     *            current hand type integer (from HandScoreConstants)
     * @return map of hand type name to improvement percentage, or null
     */
    private Map<String, Double> computeImprovementOdds(Card[] holeCards, Card[] communityCards, int currentHandType) {
        int numCommunity = communityCards.length;
        if (numCommunity != 3 && numCommunity != 4) {
            return null;
        }

        // Build fingerprint of known cards
        long knownFingerprint = 0L;
        for (Card c : holeCards) {
            knownFingerprint |= c.fingerprint();
        }
        for (Card c : communityCards) {
            knownFingerprint |= c.fingerprint();
        }

        // Build remaining deck
        List<Card> remainingDeck = new ArrayList<>(52);
        for (int suit = CardSuit.CLUBS_RANK; suit <= CardSuit.SPADES_RANK; suit++) {
            for (int rank = Card.TWO; rank <= Card.ACE; rank++) {
                Card card = Card.getCard(suit, rank);
                if ((card.fingerprint() & knownFingerprint) == 0L) {
                    remainingDeck.add(card);
                }
            }
        }

        // Count improvements for each hand type
        int[] improvementCounts = new int[HAND_TYPE_NAMES.length];
        List<Card> holeList = List.of(holeCards);
        List<Card> trialCommunity = new ArrayList<>(numCommunity + 1);
        ServerHandEvaluator eval = new ServerHandEvaluator();

        for (Card drawCard : remainingDeck) {
            trialCommunity.clear();
            for (Card c : communityCards) {
                trialCommunity.add(c);
            }
            trialCommunity.add(drawCard);

            int newScore = eval.getScore(holeList, trialCommunity);
            int newHandType = newScore / SCORE_BASE;

            if (newHandType > currentHandType && newHandType < HAND_TYPE_NAMES.length) {
                improvementCounts[newHandType]++;
            }
        }

        // Convert counts to percentages
        int total = remainingDeck.size();
        Map<String, Double> result = new HashMap<>();
        for (int i = 2; i < HAND_TYPE_NAMES.length; i++) {
            if (improvementCounts[i] > 0 && HAND_TYPE_NAMES[i] != null) {
                result.put(HAND_TYPE_NAMES[i], (double) improvementCounts[i] / total * 100.0);
            }
        }
        return result;
    }

    private static final int AHEAD = 0;
    private static final int TIED = 1;
    private static final int BEHIND = 2;
    private static final int NUM_HAND_POTENTIAL_SAMPLES = 200;

    /**
     * Compute hand potential using opponent-sampling (Sklansky-Malmuth algorithm).
     * Only meaningful on flop (3 community cards) or turn (4 community cards).
     * Returns null otherwise.
     *
     * <p>
     * Samples 200 random opponent hands, tracks behind-&gt;ahead transitions
     * (positive potential) and ahead-&gt;behind transitions (negative potential)
     * after the next board card.
     *
     * @param holeCards
     *            player's hole cards
     * @param communityCards
     *            current community cards
     * @param random
     *            random source for opponent hand sampling
     * @return double[2] with {positivePotential%, negativePotential%}, or null
     */
    private double[] computeHandPotential(Card[] holeCards, Card[] communityCards, Random random) {
        int numCommunity = communityCards.length;
        if (numCommunity != 3 && numCommunity != 4) {
            return null;
        }

        // Build fingerprint of known cards
        long knownFingerprint = 0L;
        for (Card c : holeCards) {
            knownFingerprint |= c.fingerprint();
        }
        for (Card c : communityCards) {
            knownFingerprint |= c.fingerprint();
        }

        // Build remaining deck
        List<Card> remainingDeck = new ArrayList<>(52);
        for (int suit = CardSuit.CLUBS_RANK; suit <= CardSuit.SPADES_RANK; suit++) {
            for (int rank = Card.TWO; rank <= Card.ACE; rank++) {
                Card card = Card.getCard(suit, rank);
                if ((card.fingerprint() & knownFingerprint) == 0L) {
                    remainingDeck.add(card);
                }
            }
        }

        List<Card> shuffled = new ArrayList<>(remainingDeck);

        // hp[currentRelation][futureRelation]: transition counts
        int[][] hp = new int[3][3];
        int[] hpTotal = new int[3];

        ServerHandEvaluator eval = new ServerHandEvaluator();
        List<Card> holeList = List.of(holeCards);
        List<Card> currentCommunity = List.of(communityCards);

        // Evaluate our current score with current community
        int ourCurrentScore = eval.getScore(holeList, currentCommunity);

        for (int sample = 0; sample < NUM_HAND_POTENTIAL_SAMPLES; sample++) {
            Collections.shuffle(shuffled, random);

            // Pick 2 cards as opponent hole cards
            Card oppCard1 = shuffled.get(0);
            Card oppCard2 = shuffled.get(1);
            List<Card> oppHole = List.of(oppCard1, oppCard2);

            // Evaluate opponent's current score
            int oppCurrentScore = eval.getScore(oppHole, currentCommunity);

            // Determine current relation
            int relation;
            if (ourCurrentScore > oppCurrentScore) {
                relation = AHEAD;
            } else if (ourCurrentScore == oppCurrentScore) {
                relation = TIED;
            } else {
                relation = BEHIND;
            }

            // Build fingerprint of opponent cards to exclude from future cards
            long oppFingerprint = oppCard1.fingerprint() | oppCard2.fingerprint();

            // Enumerate future board cards (cards not in hole, community, or opp hand)
            List<Card> futureDeck = new ArrayList<>(remainingDeck.size() - 2);
            for (Card c : remainingDeck) {
                if ((c.fingerprint() & oppFingerprint) == 0L) {
                    futureDeck.add(c);
                }
            }

            List<Card> futureComm = new ArrayList<>(numCommunity + 1);
            for (Card c : communityCards) {
                futureComm.add(c);
            }

            for (Card futureCard : futureDeck) {
                futureComm.add(futureCard);

                int ourFutureScore = eval.getScore(holeList, futureComm);
                int oppFutureScore = eval.getScore(oppHole, futureComm);

                int futureRelation;
                if (ourFutureScore > oppFutureScore) {
                    futureRelation = AHEAD;
                } else if (ourFutureScore == oppFutureScore) {
                    futureRelation = TIED;
                } else {
                    futureRelation = BEHIND;
                }

                hp[relation][futureRelation]++;
                hpTotal[relation]++;

                futureComm.remove(futureComm.size() - 1);
            }
        }

        // Positive potential: were behind, ended up ahead
        double positivePotential;
        int totalBehind = hpTotal[BEHIND];
        if (totalBehind == 0) {
            positivePotential = 0.0;
        } else {
            positivePotential = (double) hp[BEHIND][AHEAD] / totalBehind * 100.0;
        }

        // Negative potential: were ahead, ended up behind
        double negativePotential;
        int totalAhead = hpTotal[AHEAD];
        if (totalAhead == 0) {
            negativePotential = 0.0;
        } else {
            negativePotential = (double) hp[AHEAD][BEHIND] / totalAhead * 100.0;
        }

        return new double[]{positivePotential, negativePotential};
    }

    /**
     * Calculate equity via Monte Carlo simulation.
     */
    private double calculateEquity(Card[] holeCards, Card[] communityCards, int numOpponents, int iterations,
            Random random) {
        // Build set of known card indices for fast lookup
        long knownFingerprint = 0L;
        for (Card c : holeCards) {
            knownFingerprint |= c.fingerprint();
        }
        for (Card c : communityCards) {
            knownFingerprint |= c.fingerprint();
        }

        // Build remaining deck
        List<Card> remainingDeck = new ArrayList<>(52);
        for (int suit = CardSuit.CLUBS_RANK; suit <= CardSuit.SPADES_RANK; suit++) {
            for (int rank = Card.TWO; rank <= Card.ACE; rank++) {
                Card card = Card.getCard(suit, rank);
                if ((card.fingerprint() & knownFingerprint) == 0L) {
                    remainingDeck.add(card);
                }
            }
        }

        int communityNeeded = 5 - communityCards.length;

        int wins = 0;
        int ties = 0;

        ServerHandEvaluator playerEval = new ServerHandEvaluator();
        ServerHandEvaluator oppEval = new ServerHandEvaluator();

        List<Card> shuffled = new ArrayList<>(remainingDeck);

        for (int i = 0; i < iterations; i++) {
            Collections.shuffle(shuffled, random);

            int dealIndex = 0;

            // Deal remaining community cards
            List<Card> fullCommunity = new ArrayList<>(5);
            for (Card c : communityCards) {
                fullCommunity.add(c);
            }
            for (int j = 0; j < communityNeeded; j++) {
                fullCommunity.add(shuffled.get(dealIndex++));
            }

            // Evaluate player hand
            List<Card> playerHole = List.of(holeCards);
            int playerScore = playerEval.getScore(playerHole, fullCommunity);

            // Evaluate opponent hands and compare
            boolean playerWins = true;
            boolean playerTies = false;
            for (int o = 0; o < numOpponents; o++) {
                List<Card> oppHole = List.of(shuffled.get(dealIndex++), shuffled.get(dealIndex++));
                int oppScore = oppEval.getScore(oppHole, fullCommunity);
                if (oppScore > playerScore) {
                    playerWins = false;
                    break;
                } else if (oppScore == playerScore) {
                    playerTies = true;
                }
            }

            if (playerWins && !playerTies) {
                wins++;
            } else if (playerWins) {
                ties++;
            }
        }

        // Win + tie/2 for equity (ties split the pot)
        return ((double) wins + (double) ties / 2.0) / iterations * 100.0;
    }

    /**
     * Compute recommendation text based on equity and pot odds.
     */
    private String computeRecommendation(double equity, double potOdds, int callAmount) {
        if (callAmount == 0) {
            return "Check";
        }
        double edge = equity - potOdds;
        if (edge > 10) {
            return "Raise or Call";
        } else if (edge > 0) {
            return "Consider calling";
        } else {
            return "Consider folding";
        }
    }

    /**
     * Describe the hand type with specifics (e.g. "One Pair, Aces").
     */
    private String describeHand(int handType, int score, Card[] holeCards, Card[] communityCards) {
        // Gather all cards for flush detection
        int[] rankCounts = new int[Card.ACE + 1];
        int[] suitCounts = new int[4];
        for (Card c : holeCards) {
            rankCounts[c.getRank()]++;
            suitCounts[c.getSuit()]++;
        }
        for (Card c : communityCards) {
            rankCounts[c.getRank()]++;
            suitCounts[c.getSuit()]++;
        }

        switch (handType) {
            case ROYAL_FLUSH :
                return "Royal Flush";
            case STRAIGHT_FLUSH : {
                int high = score % SCORE_BASE;
                return "Straight Flush, " + RANK_NAMES[high] + " High";
            }
            case QUADS : {
                int quadsRank = findRankWithCount(rankCounts, 4);
                return "Four of a Kind, " + RANK_NAMES_PLURAL[quadsRank];
            }
            case FULL_HOUSE : {
                int tripsRank = findRankWithCount(rankCounts, 3);
                // For full house with two trips, find the lower trips as the "pair"
                int pairRank = findSecondaryRank(rankCounts, tripsRank);
                return "Full House, " + RANK_NAMES_PLURAL[tripsRank] + " over " + RANK_NAMES_PLURAL[pairRank];
            }
            case FLUSH : {
                int flushSuit = findFlushSuit(suitCounts);
                int highRank = findHighestRankInSuit(holeCards, communityCards, flushSuit);
                return "Flush, " + RANK_NAMES[highRank] + " High";
            }
            case STRAIGHT : {
                int high = score % SCORE_BASE;
                return "Straight, " + RANK_NAMES[high] + " High";
            }
            case TRIPS : {
                int tripsRank = findRankWithCount(rankCounts, 3);
                return "Three of a Kind, " + RANK_NAMES_PLURAL[tripsRank];
            }
            case TWO_PAIR : {
                int highPair = findHighestRankWithCount(rankCounts, 2);
                int lowPair = findSecondHighestRankWithCount(rankCounts, 2, highPair);
                return "Two Pair, " + RANK_NAMES_PLURAL[highPair] + " and " + RANK_NAMES_PLURAL[lowPair];
            }
            case PAIR : {
                int pairRank = findRankWithCount(rankCounts, 2);
                return "One Pair, " + RANK_NAMES_PLURAL[pairRank];
            }
            case HIGH_CARD : {
                int highRank = findHighestRankWithCount(rankCounts, 1);
                return "High Card, " + RANK_NAMES[highRank];
            }
            default :
                return null;
        }
    }

    private int findRankWithCount(int[] rankCounts, int count) {
        for (int r = Card.ACE; r >= Card.TWO; r--) {
            if (rankCounts[r] == count) {
                return r;
            }
        }
        return 0;
    }

    private int findHighestRankWithCount(int[] rankCounts, int minCount) {
        for (int r = Card.ACE; r >= Card.TWO; r--) {
            if (rankCounts[r] >= minCount) {
                return r;
            }
        }
        return 0;
    }

    private int findSecondHighestRankWithCount(int[] rankCounts, int count, int excludeRank) {
        for (int r = Card.ACE; r >= Card.TWO; r--) {
            if (r != excludeRank && rankCounts[r] >= count) {
                return r;
            }
        }
        return 0;
    }

    /**
     * For full house, find the secondary rank (pair rank). If two sets of trips
     * exist (7-card hand), the lower trips acts as the pair.
     */
    private int findSecondaryRank(int[] rankCounts, int tripsRank) {
        // Look for another trips first (in case of two trips)
        for (int r = Card.ACE; r >= Card.TWO; r--) {
            if (r != tripsRank && rankCounts[r] >= 3) {
                return r;
            }
        }
        // Then look for a pair
        for (int r = Card.ACE; r >= Card.TWO; r--) {
            if (r != tripsRank && rankCounts[r] >= 2) {
                return r;
            }
        }
        return 0;
    }

    private int findFlushSuit(int[] suitCounts) {
        for (int s = 0; s < 4; s++) {
            if (suitCounts[s] >= 5) {
                return s;
            }
        }
        return 0;
    }

    private int findHighestRankInSuit(Card[] holeCards, Card[] communityCards, int suit) {
        int highest = 0;
        for (Card c : holeCards) {
            if (c.getSuit() == suit && c.getRank() > highest) {
                highest = c.getRank();
            }
        }
        for (Card c : communityCards) {
            if (c.getSuit() == suit && c.getRank() > highest) {
                highest = c.getRank();
            }
        }
        return highest;
    }

    /**
     * Convert hole card ranks and suited status to grid position. Returns {row,
     * col}.
     */
    private int[] holeCardsToGrid(int rank1, int rank2, boolean suited) {
        int idx1 = rankToGridIndex(rank1);
        int idx2 = rankToGridIndex(rank2);

        int highIdx = Math.min(idx1, idx2);
        int lowIdx = Math.max(idx1, idx2);

        if (highIdx == lowIdx) {
            // Pocket pair - on the diagonal
            return new int[]{highIdx, highIdx};
        }

        if (suited) {
            // Suited - upper-right triangle (row < col)
            return new int[]{highIdx, lowIdx};
        }

        // Offsuit - lower-left triangle (row > col)
        return new int[]{lowIdx, highIdx};
    }

    /**
     * Convert card rank (2-14) to grid index (0=A, 1=K, ..., 12=2).
     */
    private int rankToGridIndex(int rank) {
        return Card.ACE - rank;
    }

    /**
     * Get notation for a grid position (e.g. "AKs", "AA", "72o").
     */
    private String getHandNotation(int row, int col) {
        String r1 = RANK_LABELS[Card.ACE - row];
        String r2 = RANK_LABELS[Card.ACE - col];
        if (row == col) {
            return r1 + r2;
        }
        if (col > row) {
            return r1 + r2 + "s";
        }
        return r2 + r1 + "o";
    }
}
