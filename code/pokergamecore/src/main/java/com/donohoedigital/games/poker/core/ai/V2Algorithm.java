/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.*;

import java.util.function.Consumer;

/**
 * V2 algorithm extracted from poker module V2Player. Advanced AI with bluffing,
 * opponent modeling, hand range estimation, and adaptive bet sizing.
 *
 * <p>
 * Stateful across hands:
 * <ul>
 * <li>Steam (tilt factor from bad beats)</li>
 * <li>Pot raise frequency tracking (10-hand moving average)</li>
 * </ul>
 *
 * <p>
 * Stateful per hand:
 * <ul>
 * <li>Steal suspicion (estimated steal probability)</li>
 * <li>Hand strength caching (fingerprint-based)</li>
 * <li>Biased hand strength (opponent-weighted)</li>
 * <li>Biased potentials (improvement odds weighted by opponent ranges)</li>
 * </ul>
 */
@SuppressWarnings({"FieldCanBeLocal", "unused", "DuplicatedCode"})
public class V2Algorithm implements PurePokerAI, V2PlayerState, AIConstants {

    // === Core engine ===
    private final PureRuleEngine ruleEngine;

    // === Configuration ===
    private final Consumer<String> debugOutput;
    private final boolean debug;

    // === Persistent state (across hands) ===
    private float steam = 0.0f; // tilt factor from bad beats
    private final boolean[] potRaised = new boolean[10]; // 10-hand moving average
    private int maPotRaised = 0; // count of raised pots in window

    // === Per-hand state ===
    private int lastPotStatus; // for pot raise tracking
    private float stealSuspicion = 0.0f; // estimated steal probability

    // === Current action context (set during getAction(), used by lazy computation)
    // ===
    private GamePlayerInfo currentPlayer;
    private V2AIContext currentContext;

    // === Hand strength cache (per action, fingerprint-keyed) ===
    private long fpPocket = 0;
    private long fpCommunity = 0;
    private int myHandScore;
    private int[][] otherHandScore = null; // [52][52]
    private float rawHandStrength = -1;
    private float biasedHandStrength = -1;

    // === Potential cache (per action) ===
    private float[][] positivePotential = null; // [52][52]
    private float[][] negativePotential = null; // [52][52]
    private float rawPositivePotential = -1; // unbiased ppot
    private float rawNegativePotential = -1; // unbiased npot
    private float biasedPositivePotential = -1;
    private float biasedNegativePotential = -1;

    // === Biased effective hand strength cache (per action) ===
    private float cachedBEHS = -1;

    // === Player count for multi-player scaling ===
    private int numPlayersWithCards = 0;

    // === Opponent hand range matrix ===
    private PocketMatrixFloat fieldMatrix = null;

    public V2Algorithm() {
        this(null, false);
    }

    public V2Algorithm(Consumer<String> debugOutput, boolean enableDebug) {
        this.ruleEngine = new PureRuleEngine();
        this.debugOutput = debugOutput != null ? debugOutput : (s -> {
        });
        this.debug = enableDebug;
    }

    // === PurePokerAI interface ===

    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options, AIContext context) {
        if (!(context instanceof V2AIContext)) {
            throw new IllegalArgumentException("V2Algorithm requires V2AIContext");
        }
        V2AIContext v2Context = (V2AIContext) context;
        // Store current context for lazy computation methods
        this.currentPlayer = player;
        this.currentContext = v2Context;
        detectStateChanges(player, v2Context);
        computeOdds(player, v2Context);
        ruleEngine.execute(v2Context, this, player, debugOutput);
        return ruleEngine.getAction();
    }

    @Override
    public boolean wantsRebuy(GamePlayerInfo player, AIContext context) {
        // TODO: Implement rebuy logic (delegate to base V1 logic)
        return false;
    }

    @Override
    public boolean wantsAddon(GamePlayerInfo player, AIContext context) {
        // TODO: Implement addon logic (delegate to base V1 logic)
        return false;
    }

    // === V2PlayerState interface ===

    @Override
    public float getSteam() {
        return steam;
    }

    @Override
    public float getStealSuspicion() {
        return stealSuspicion;
    }

    @Override
    public float getHandStrength() {
        // Pre-flop hand selection strength (from StrategyProvider)
        // This is called by PureRuleEngine for pre-flop factors
        return -1; // Handled via V2AIContext.getStrategy().getHandStrength()
    }

    @Override
    public float getRawHandStrength() {
        // Apply multi-player scaling (matches V2Player.getRawHandStrength line 695-701)
        int exponent = Math.max(1, numPlayersWithCards - 1);
        return (float) Math.pow(rawHandStrength, exponent);
    }

    @Override
    public float getBiasedHandStrength() {
        if (biasedHandStrength >= 0.0f) {
            return biasedHandStrength;
        }
        // Compute on demand using stored context (matches V2Player lazy computation)
        return computeBiasedHandStrength(null, null);
    }

    @Override
    public float getBiasedPositivePotential() {
        return biasedPositivePotential;
    }

    @Override
    public float getBiasedNegativePotential() {
        return biasedNegativePotential;
    }

    @Override
    public float getPositiveHandPotential() {
        return rawPositivePotential;
    }

    @Override
    public float getNegativeHandPotential() {
        return rawNegativePotential;
    }

    @Override
    public float getBiasedEffectiveHandStrength(float scaledPotOdds) {
        if (cachedBEHS >= 0) {
            return cachedBEHS;
        }

        float rhs = getRawHandStrength();
        float bhs = computeBiasedHandStrength(null, null); // Will use cached values

        // Use numPlayersWithCards for multi-player scaling (matches
        // V2Player.getBiasedEffectiveHandStrength)
        int exponent = Math.max(1, numPlayersWithCards - 1);

        cachedBEHS = (float) Math.pow(Math.min(bhs - rhs * getBiasedNegativePotential()
                + Math.min((1 - bhs), getBiasedPositivePotential() * (scaledPotOdds + 1)), 1.0f), exponent);

        return cachedBEHS;
    }

    @Override
    public boolean debugEnabled() {
        return debug;
    }

    // === Lifecycle detection ===

    /**
     * Detect state changes via fingerprinting and update lifecycle state. Called at
     * the start of every action decision.
     */
    private void detectStateChanges(GamePlayerInfo player, V2AIContext context) {
        Hand pocket = context.getPocketCards(player);
        Hand community = context.getCommunity();

        // Detect new hand (pocket change or no community on first action)
        if (pocket != null && pocket.fingerprint() != fpPocket) {
            onNewHand(player, context);
        }

        // Detect new round (community change)
        if (community != null && community.fingerprint() != fpCommunity) {
            int round = context.getBettingRound();
            if (round == BettingRound.FLOP.toLegacy()) {
                onDealtFlop(context);
            } else if (round == BettingRound.TURN.toLegacy()) {
                onDealtTurn(context);
            } else if (round == BettingRound.RIVER.toLegacy()) {
                onDealtRiver(context);
            }
        }

        // Player acted events are not detectable via fingerprinting alone
        // (would need action history tracking). For now, we update steal
        // suspicion inline in getAction if needed.
    }

    private void onNewHand(GamePlayerInfo player, V2AIContext context) {
        stealSuspicion = 0.0f;
        // TODO: loadReputation() if needed
    }

    private void onDealtFlop(V2AIContext context) {
        int maOld = maPotRaised;

        maPotRaised -= (potRaised[0] ? 1 : 0);
        System.arraycopy(potRaised, 1, potRaised, 0, 9);

        switch (lastPotStatus) {
            case PokerConstants.RAISED_POT :
            case PokerConstants.RERAISED_POT :
                potRaised[9] = true;
                maPotRaised += 1;
                break;
            default :
                potRaised[9] = false;
                break;
        }
    }

    private void onDealtTurn(V2AIContext context) {
        // Intentionally empty in original
    }

    private void onDealtRiver(V2AIContext context) {
        // Intentionally empty in original
    }

    /**
     * Called at end of hand to update persistent state (steam). NOTE: This is not
     * automatically called - desktop/server wrappers must call it.
     */
    public void onEndHand(float badBeatScore) {
        if (badBeatScore > 0) {
            steam += Math.log(badBeatScore);
        } else {
            steam *= 0.5f;
        }
        // TODO: saveReputation() if needed
    }

    /**
     * Update steal suspicion and opponent model state when a player acts. NOTE:
     * This is not automatically called - desktop/server wrappers must call it.
     */
    public void onPlayerActed(GamePlayerInfo actingPlayer, int action, int amount, V2AIContext context) {
        cachedBEHS = -1;
        lastPotStatus = context.getPotStatus();

        if (context.getBettingRound() == BettingRound.PRE_FLOP.toLegacy()) {
            if (context.getPotStatus() == PokerConstants.NO_POT_ACTION) {
                // Player opened the pot
                if (action == AIContext.ACTION_RAISE) {
                    // Suspect steal from late position
                    switch (context.getStartingPositionCategory(actingPlayer)) {
                        case POSITION_MIDDLE :
                            stealSuspicion = 0.03f;
                            break;
                        case POSITION_LATE :
                            // Higher steal suspicion for button (0.10) vs non-button late (0.08)
                            stealSuspicion = context.isButton(actingPlayer) ? 0.10f : 0.08f;
                            break;
                        case POSITION_SMALL :
                            stealSuspicion = 0.12f;
                            break;
                    }
                }
            } else {
                switch (action) {
                    case AIContext.ACTION_RAISE :
                        // Raiser has been re-raised
                        stealSuspicion = 0.0f;
                        break;
                    case AIContext.ACTION_CALL :
                        if ((float) context.getTotalPotChipCount() / (float) amount < 3.0) {
                            // Raiser called with weak pot odds
                            stealSuspicion = 0.0f;
                        } else {
                            // Reduce with successive callers
                            stealSuspicion /= 2.0f;
                        }
                        break;
                }
            }
        } else if ((action == AIContext.ACTION_BET) || (action == AIContext.ACTION_RAISE)) {
            if (amount > context.getTotalPotChipCount() / 2) {
                // Mark overbet in opponent model
                V2OpponentModel model = context.getOpponentModel(actingPlayer);
                model.setOverbetPotPostFlop(true);
            }
        }
    }

    // === Hand strength computation ===

    /**
     * Compute or retrieve cached hand strength values. Fingerprint-based caching
     * prevents redundant 52x52 enumeration.
     */
    private void computeOdds(GamePlayerInfo player, V2AIContext context) {
        Hand pocket = context.getPocketCards(player);
        Hand community = context.getCommunity();

        if (pocket == null || community == null || community.size() == 0) {
            return;
        }

        // Store player count for multi-player scaling
        numPlayersWithCards = context.getNumPlayersWithCards();

        // Check fingerprint cache
        if (fpPocket == pocket.fingerprint() && fpCommunity == community.fingerprint()) {
            return; // Already computed for this state
        }

        fpPocket = pocket.fingerprint();
        fpCommunity = community.fingerprint();

        _computeOdds(pocket, community, player, context);
    }

    /**
     * Core 52x52 enumeration for raw hand strength and potential. Extracted from
     * V2Player._computeOdds().
     */
    private void _computeOdds(Hand pocket, Hand community, GamePlayerInfo player, V2AIContext context) {
        float[][] posPot = getPositivePotentialArray();
        float[][] negPot = getNegativePotentialArray();
        int[][] otherScore = getOtherHandScoreArray();

        Deck deck = new Deck(false);
        deck.removeCards(pocket);
        deck.removeCards(community);

        Hand opponentHand = new Hand(Card.BLANK, Card.BLANK);
        HandInfoFaster info = new HandInfoFaster();
        PocketRanks ranks = PocketRanks.getInstance(community);

        myHandScore = info.getScore(pocket, community);

        int totalHands = 0;
        int winCount = 0;

        float ppotSum = 0.0f;
        float ppotDiv = 0.0f;
        float npotSum = 0.0f;
        float npotDiv = 0.0f;

        for (int i = 51; i >= 0; --i) {
            opponentHand.setCard(0, Card.getCard(i % 4, i / 4 + 2));

            for (int j = 51; j > i; --j) {
                opponentHand.setCard(1, Card.getCard(j % 4, j / 4 + 2));

                if (pocket.containsAny(opponentHand) || community.containsAny(opponentHand)) {
                    otherScore[i][j] = 0;
                    posPot[i][j] = 0;
                    negPot[i][j] = 0;
                    continue;
                }

                int ohs = otherScore[i][j] = info.getScore(opponentHand, community);

                if (ohs <= myHandScore) {
                    ++winCount;
                }

                ++totalHands;

                // Compute potential if not river
                if (community.size() < 5) {
                    deck.removeCards(opponentHand);

                    int deckSize = deck.size();
                    float pdiv = 0.0f;
                    float ndiv = 0.0f;
                    float ppot = 0.0f;
                    float npot = 0.0f;

                    for (int k = 0; k < deckSize; ++k) {
                        Card nextCard = deck.getCard(k);
                        community.add(nextCard);

                        float phs = (float) Math.pow(ranks.getRawHandStrength(opponentHand), 2);

                        int myNewScore = info.getScore(pocket, community);
                        int otherNewScore = info.getScore(opponentHand, community);

                        // Currently behind
                        pdiv += phs;
                        ndiv += 1.0;

                        if (ohs > myHandScore) {
                            if (myNewScore == otherNewScore) {
                                ppot += phs * 0.5;
                                pdiv -= phs * 0.5;
                            } else if (myNewScore > otherNewScore) {
                                ppot += phs * 1.0;
                            }
                        }
                        // Currently tied
                        else if (ohs == myHandScore) {
                            if (myNewScore > otherNewScore) {
                                ppot += phs * 0.5;
                                pdiv -= phs * 0.5;
                            } else if (myNewScore < otherNewScore) {
                                npot += 1.0f;
                            } else {
                                npot += 0.5f;
                                ndiv -= 0.5f;
                            }
                        }
                        // Currently ahead
                        else {
                            if (myNewScore == otherNewScore) {
                                npot += 0.5f;
                                ndiv -= 0.5;
                            } else if (myNewScore < otherNewScore) {
                                npot += 1.0f;
                            }
                        }

                        community.remove(nextCard);
                    }

                    ppot = pdiv > 0 ? ppot / pdiv : 0;
                    npot = ndiv > 0 ? npot / ndiv : 0;

                    posPot[i][j] = ppot;
                    negPot[i][j] = npot;

                    if (otherScore[i][j] > myHandScore) {
                        ppotSum += ppot;
                        ppotDiv += 1.0;
                    } else if (otherScore[i][j] == myHandScore) {
                        ppotSum += ppot * 0.5;
                        ppotDiv += 0.5;
                        npotSum += npot * 0.5f;
                        npotDiv += 0.5f;
                    } else {
                        npotSum += npot;
                        npotDiv += 1.0f;
                    }

                    deck.addAll(opponentHand);
                }
            }
        }

        rawHandStrength = (float) winCount / totalHands;
        rawPositivePotential = ppotDiv != 0.0f ? ppotSum / ppotDiv : 0.0f;
        rawNegativePotential = npotDiv != 0.0f ? npotSum / npotDiv : 0.0f;

        // Reset cached values
        biasedHandStrength = -1;
        cachedBEHS = -1;

        if (community.size() == 5) {
            biasedPositivePotential = 0.0f;
            biasedNegativePotential = 0.0f;
            return;
        }

        computeBiasedPotential(player, context);
    }

    /**
     * Compute opponent-weighted positive and negative potential.
     */
    private void computeBiasedPotential(GamePlayerInfo player, V2AIContext context) {
        PocketMatrixFloat fieldMatrix = updateFieldMatrix(player, context);

        float[][] posPot = getPositivePotentialArray();
        float[][] negPot = getNegativePotentialArray();

        Hand community = context.getCommunity();
        PocketRanks ranks = PocketRanks.getInstance(community);

        Hand hand = new Hand(Card.BLANK, Card.BLANK);

        float bpp = 0.0f;
        float bnp = 0.0f;
        float total = 0.0f;

        int skip = context.getSeat(player);

        for (int i = 51; i >= 0; --i) {
            hand.setCard(0, Card.getCard(i % 4, i / 4 + 2));

            for (int j = 51; j > i; --j) {
                hand.setCard(1, Card.getCard(j % 4, j / 4 + 2));

                for (int seat = 0; seat < 10; ++seat) {
                    if (seat == skip)
                        continue;

                    float weight = fieldMatrix.get(i, j);
                    float rhs = ranks.getRawHandStrength(hand);

                    bpp += posPot[i][j] * weight * rhs;
                    bnp += negPot[i][j] * weight * rhs;

                    total += weight * rhs;
                }
            }
        }

        biasedPositivePotential = (total > 0) ? bpp / total : 0.0f;
        biasedNegativePotential = (total > 0) ? bnp / total : 0.0f;
    }

    /**
     * Compute opponent-weighted hand strength using SimpleBias tables. Can be
     * called with null parameters to use current action context.
     */
    private float computeBiasedHandStrength(GamePlayerInfo player, V2AIContext context) {
        if (biasedHandStrength >= 0.0f) {
            return biasedHandStrength;
        }

        // Use stored context if parameters are null (lazy computation from getters)
        if (player == null) {
            player = currentPlayer;
        }
        if (context == null) {
            context = currentContext;
        }

        // If still null (called before getAction()), return cached value
        if (player == null || context == null) {
            return biasedHandStrength; // -1
        }

        int[][] otherScore = getOtherHandScoreArray();
        Hand pocket = context.getPocketCards(player);
        Hand community = context.getCommunity();
        PocketRanks ranks = PocketRanks.getInstance(community);

        Hand hand = new Hand(Card.BLANK, Card.BLANK);

        float[] bhs = new float[10];
        float[] total = new float[10];

        int skip = context.getSeat(player);

        boolean[] paid = new boolean[context.getNumPlayersAtTable()];

        for (int p = 0; p < context.getNumPlayersAtTable(); ++p) {
            GamePlayerInfo opponent = context.getPlayerAt(p);
            if (opponent != null) {
                paid[p] = context.paidToPlay(opponent);
            }
        }

        for (int i = 50; i >= 0; --i) {
            hand.setCard(0, Card.getCard(i % 4, i / 4 + 2));

            for (int j = 51; j > i; --j) {
                hand.setCard(1, Card.getCard(j % 4, j / 4 + 2));

                if (pocket.containsAny(hand) || community.containsAny(hand))
                    continue;

                for (int p = 0; p < context.getNumPlayersAtTable(); ++p) {
                    GamePlayerInfo opponent = context.getPlayerAt(p);

                    if ((opponent == null) || opponent.isFolded())
                        continue;

                    int seat = context.getSeat(opponent);

                    if (seat == skip)
                        continue;

                    int x = SimpleBias.getX(hand);
                    int y = SimpleBias.getY(hand);

                    V2OpponentModel model = context.getOpponentModel(opponent);
                    float percentPaid = model.getHandsPaidPercent(0.30f);

                    int biasIndex = (int) (percentPaid * 100.0f) / 10;

                    float weight;
                    if (paid[p]) {
                        weight = SimpleBias.simpleBias_[biasIndex][x][y] / 1000.0f;
                    } else {
                        weight = 1.0f;
                    }

                    float rhs = ranks.getRawHandStrength(hand);

                    if (otherScore[i][j] <= myHandScore) {
                        bhs[seat] += weight * rhs;
                    }

                    total[seat] += weight * rhs;
                }
            }
        }

        float bhsSum = 0.0f;
        int count = 0;

        for (int p = 0; p < context.getNumPlayersAtTable(); ++p) {
            GamePlayerInfo opponent = context.getPlayerAt(p);
            if (opponent == null)
                continue;

            int seat = context.getSeat(opponent);

            if (seat == skip)
                continue;

            if (opponent.isFolded())
                continue;

            if (total[seat] > 0) {
                bhsSum += bhs[seat] / total[seat];
            }

            ++count;
        }

        biasedHandStrength = count > 0 ? bhsSum / count : 0.0f;

        return biasedHandStrength;
    }

    /**
     * Update field matrix (opponent hand range weights) using SimpleBias.
     */
    private PocketMatrixFloat updateFieldMatrix(GamePlayerInfo player, V2AIContext context) {
        if (fieldMatrix == null) {
            fieldMatrix = new PocketMatrixFloat(0.0f);
        }

        int skip = context.getSeat(player);

        boolean[] couldLimp = new boolean[context.getNumPlayersAtTable()];
        boolean[] paid = new boolean[context.getNumPlayersAtTable()];
        boolean[] limped = new boolean[context.getNumPlayersAtTable()];

        for (int p = 0; p < context.getNumPlayersAtTable(); ++p) {
            GamePlayerInfo opponent = context.getPlayerAt(p);
            if (opponent != null) {
                couldLimp[p] = context.couldLimp(opponent);
                paid[p] = context.paidToPlay(opponent);
                limped[p] = couldLimp[p] && context.limped(opponent);
            }
        }

        for (int i = 51; i >= 0; --i) {
            Card card1 = Card.getCard(i % 4, i / 4 + 2);

            for (int j = 51; j > i; --j) {
                Card card2 = Card.getCard(j % 4, j / 4 + 2);

                float max = 0.0f;

                for (int p = 0; p < context.getNumPlayersAtTable(); ++p) {
                    GamePlayerInfo opponent = context.getPlayerAt(p);
                    if (opponent == null)
                        continue;

                    int seat = context.getSeat(opponent);

                    if (seat == skip)
                        continue;

                    if (opponent.isFolded())
                        continue;

                    int x = SimpleBias.getX(card1, card2);
                    int y = SimpleBias.getY(card1, card2);

                    V2OpponentModel model = context.getOpponentModel(opponent);
                    float percentPaid = model.getHandsPaidPercent(0.30f);

                    boolean wasRaisedPreFlop = context.wasRaisedPreFlop();

                    float weight;

                    if (couldLimp[p]) {
                        float percentLimped = model.getHandsLimpedPercent(percentPaid);
                        float percentRaised = Math
                                .max(1.0f - percentLimped - model.getHandsFoldedUnraisedPercent(0.60f), 0.0f);

                        int limpIndex = (int) ((percentLimped + percentRaised) * 100.0f + 4) / 10;
                        int raiseIndex = (int) (percentRaised * 100.0f + 4) / 10;

                        if (limped[p]) {
                            if (wasRaisedPreFlop) {
                                raiseIndex = Math.max(raiseIndex - 1, 0);
                            }
                            weight = (SimpleBias.simpleBias_[limpIndex][x][y]
                                    - SimpleBias.simpleBias_[raiseIndex][x][y]) / 1000.0f;
                        } else {
                            weight = SimpleBias.simpleBias_[raiseIndex][x][y] / 1000.0f;
                        }
                    } else if (paid[p]) {
                        int bhsIndex = (int) (percentPaid * 100.0f + 4) / 10;

                        if (wasRaisedPreFlop) {
                            bhsIndex = Math.max(bhsIndex - 2, 0);
                        }

                        weight = SimpleBias.simpleBias_[bhsIndex][x][y] / 1000.0f;
                    } else {
                        weight = 1.0f;
                    }

                    max = Math.max(max, weight);
                }

                fieldMatrix.set(i, j, max);
            }
        }

        return fieldMatrix;
    }

    // === Array getters (lazy init) ===

    private float[][] getPositivePotentialArray() {
        if (positivePotential == null) {
            positivePotential = new float[52][52];
        }
        return positivePotential;
    }

    private float[][] getNegativePotentialArray() {
        if (negativePotential == null) {
            negativePotential = new float[52][52];
        }
        return negativePotential;
    }

    private int[][] getOtherHandScoreArray() {
        if (otherHandScore == null) {
            otherHandScore = new int[52][52];
        }
        return otherHandScore;
    }
}
