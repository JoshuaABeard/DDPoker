/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
import com.donohoedigital.games.poker.core.GameHand;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.HandSorted;

import java.util.Random;

/**
 * V1 poker AI algorithm - Moderate skill level.
 * <p>
 * Strategy based on:
 * <ul>
 * <li>Sklansky starting hand groups for pre-flop decisions</li>
 * <li>Position-based aggression (early/middle/late/blind)</li>
 * <li>Pot odds calculations for post-flop play</li>
 * <li>Hand strength evaluation and improvement odds</li>
 * <li>Personality traits (tight/loose, bluff propensity)</li>
 * </ul>
 * <p>
 * Extracted from {@code com.donohoedigital.games.poker.ai.V1Player} to be
 * Swing-free for use in server-hosted games.
 *
 * @see TournamentAI
 */
public class V1Algorithm implements PurePokerAI {

    // AI Skill Levels
    public static final int AI_EASY = 1;
    public static final int AI_MEDIUM = 2;
    public static final int AI_HARD = 3;

    // Minimum improvement odds (accounts for bluff possibility)
    private static final double MIN_IMPROVE_ODDS = 0.07; // 7% minimum improvement odds

    // Immutable configuration
    private final Random random;
    private final int skillLevel;

    // Personality traits (immutable after construction)
    private final int baseTightFactor; // 0-100, 100 = very tight
    private final int baseBluffFactor; // 0-100, 100 = lots of bluffing
    private final int rebuyPropensity; // 0-100
    private final int addonPropensity; // 0-100

    // State tracking across rounds and hands
    private boolean checkRaiseIntent = false; // Intent to check-raise this hand
    private int lastRoundBetAmount = 0; // Amount bet in previous round
    private int lastRoundPotSize = 0; // Pot size at end of last round
    private int lastBettingRound = -1; // Last round we made a decision
    private int currentHandId = -1; // Track current hand for state reset
    private PlayerAction lastAction = null; // Our last action this round

    // Limper detection state (for B10 - V1Player lines 1293-1349)
    private int raiserPreAction = AIContext.ACTION_NONE; // Raiser's action in pre-flop
    private int raiserFlopAction = AIContext.ACTION_NONE; // Raiser's action in flop
    private int raiserTurnAction = AIContext.ACTION_NONE; // Raiser's action in turn
    private int bettorPreAction = AIContext.ACTION_NONE; // Bettor's action in pre-flop
    private int bettorFlopAction = AIContext.ACTION_NONE; // Bettor's action in flop
    private int bettorTurnAction = AIContext.ACTION_NONE; // Bettor's action in turn

    /**
     * Opponent statistics are now queried from AIContext instead of being tracked
     * locally. See getOpponentRaiseFrequency() and getOpponentBetFrequency()
     * methods.
     */

    /**
     * Create V1 algorithm with random personality traits.
     *
     * @param seed
     *            Random seed for reproducible behavior
     * @param skillLevel
     *            AI difficulty (AI_EASY, AI_MEDIUM, AI_HARD)
     */
    public V1Algorithm(long seed, int skillLevel) {
        this.random = new Random(seed);
        this.skillLevel = skillLevel;

        // Generate random personality traits
        this.baseTightFactor = random.nextInt(100);
        this.baseBluffFactor = random.nextInt(100);
        this.addonPropensity = random.nextInt(100);
        this.rebuyPropensity = random.nextInt(100);
    }

    /**
     * Create V1 algorithm with explicit personality traits.
     *
     * @param seed
     *            Random seed for reproducible behavior
     * @param skillLevel
     *            AI difficulty (AI_EASY, AI_MEDIUM, AI_HARD)
     * @param tightFactor
     *            Tight/loose factor (0-100, 100 = very tight)
     * @param bluffFactor
     *            Bluff propensity (0-100, 100 = lots of bluffing)
     * @param rebuyPropensity
     *            Rebuy propensity (0-100)
     * @param addonPropensity
     *            Addon propensity (0-100)
     */
    public V1Algorithm(long seed, int skillLevel, int tightFactor, int bluffFactor, int rebuyPropensity,
            int addonPropensity) {
        this.random = new Random(seed);
        this.skillLevel = skillLevel;
        this.baseTightFactor = tightFactor;
        this.baseBluffFactor = bluffFactor;
        this.rebuyPropensity = rebuyPropensity;
        this.addonPropensity = addonPropensity;
    }

    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options, AIContext context) {
        PlayerAction action;

        // Populate limper detection state for B10 (V1Player lines 1293-1349)
        int bettingRound = context.getBettingRound();
        GamePlayerInfo raiser = context.getLastRaiser();
        GamePlayerInfo bettor = context.getLastBettor();

        if (bettingRound >= 1) { // Flop or later
            if (raiser != null) {
                raiserPreAction = context.getLastActionInRound(raiser, 0); // Pre-flop action
            }
            if (bettor != null) {
                bettorPreAction = context.getLastActionInRound(bettor, 0); // Pre-flop action
            }
        }
        if (bettingRound >= 2) { // Turn or later
            if (raiser != null) {
                raiserFlopAction = context.getLastActionInRound(raiser, 1); // Flop action
            }
            if (bettor != null) {
                bettorFlopAction = context.getLastActionInRound(bettor, 1); // Flop action
            }
        }
        if (bettingRound >= 3) { // River
            if (raiser != null) {
                raiserTurnAction = context.getLastActionInRound(raiser, 2); // Turn action
            }
            if (bettor != null) {
                bettorTurnAction = context.getLastActionInRound(bettor, 2); // Turn action
            }
        }

        // Pre-flop decisions
        if (context.getBettingRound() == 0) {
            action = getPreFlop(player, options, context);
        } else {
            // Post-flop decisions (flop/turn/river)
            action = getPostFlop(player, options, context);
        }

        // Track our action for re-raise detection
        lastAction = action;

        return action;
    }

    /**
     * Pre-flop decision logic based on Sklansky's system.
     */
    private PlayerAction getPreFlop(GamePlayerInfo player, ActionOptions options, AIContext context) {
        Card[] holeCards = context.getHoleCards(player);
        if (holeCards == null || holeCards.length != 2) {
            return PlayerAction.fold();
        }

        // Create sorted hand for Sklansky ranking
        HandSorted hole = new HandSorted(holeCards[0], holeCards[1]);

        // Get context information
        int numActivePlayers = context.getNumActivePlayers();
        int amountToCall = context.getAmountToCall(player);
        boolean potRaised = context.hasBeenRaised();

        // Hard mode: 5% of players use Sklansky's "all-in system" if more than 4
        // players
        if (skillLevel == AI_HARD && baseTightFactor <= 5 && numActivePlayers > 4) {
            return getSklankskySystem(hole, player, options, context);
        }

        // Get Sklansky rank for starting hand
        int sklanskyRank = SklankskyRanking.getRank(hole);

        // Random number for decision variance (1-100)
        int randomPct = random.nextInt(100) + 1;

        // Determine loose play threshold based on tight factor
        int tightFactor = getTightFactor(context, player);
        int looseThreshold;
        if (tightFactor <= 25) {
            looseThreshold = 20;
        } else if (tightFactor <= 50) {
            looseThreshold = 15;
        } else if (tightFactor <= 75) {
            looseThreshold = 10;
        } else {
            looseThreshold = 5;
        }

        // Get position
        int position = context.getPosition(player);

        // Position-based strategy
        if (isEarlyPosition(position)) {
            return getPreFlopEarly(hole, sklanskyRank, randomPct, looseThreshold, potRaised, options, context, player);
        } else if (isMiddlePosition(position)) {
            return getPreFlopMiddle(hole, sklanskyRank, randomPct, looseThreshold, potRaised, options, context, player);
        } else if (isLatePosition(position)) {
            return getPreFlopLate(hole, sklanskyRank, randomPct, looseThreshold, potRaised, numActivePlayers, options,
                    context, player);
        } else {
            // Blinds
            return getPreFlopBlind(hole, sklanskyRank, randomPct, tightFactor, potRaised, options, context, player);
        }
    }

    /**
     * Post-flop decision logic for flop/turn/river.
     */
    private PlayerAction getPostFlop(GamePlayerInfo player, ActionOptions options, AIContext context) {
        // Reset state for new hand if needed
        resetHandState(context);

        // Random for decision variance
        int randomPct = random.nextInt(100) + 1;

        // Execute check-raise if intended (matches V1Player lines 754-766)
        int amountToCall = context.getAmountToCall(player);
        if (lastAction != null && lastAction.actionType() == com.donohoedigital.games.poker.core.state.ActionType.CHECK
                && checkRaiseIntent && amountToCall > 0) {
            // Reset check-raise intent now that we're executing it
            checkRaiseIntent = false;

            if (randomPct < 5) {
                // 5% all-in
                return createAllIn(context, player, "V1:CheckRaise");
            } else if (randomPct < 50) {
                // 45% raise 2x
                return PlayerAction.raise(amountToCall * 2 + amountToCall);
            } else {
                // 50% raise 3x
                return PlayerAction.raise(amountToCall * 3 + amountToCall);
            }
        }

        Card[] holeCards = context.getHoleCards(player);
        if (holeCards == null || holeCards.length != 2) {
            return PlayerAction.fold();
        }

        // Get community cards
        Card[] communityCards = getCommunityCards(context);
        if (communityCards == null || communityCards.length < 3) {
            return PlayerAction.fold();
        }

        // Evaluate hand
        int handRank = context.evaluateHandRank(holeCards, communityCards);
        long handScore = context.evaluateHandScore(holeCards, communityCards);

        // Analyze board texture
        BoardTexture board = analyzeBoardTexture(communityCards, context);

        // Get hand strength (0.0 to 1.0)
        float handStrength = calculateHandStrength(holeCards, communityCards, context);

        // Calculate improvement odds (if not river)
        double improveOdds = MIN_IMPROVE_ODDS;
        if (context.getBettingRound() < 3) { // Not river
            improveOdds = context.calculateImprovementOdds(holeCards, communityCards);
            if (improveOdds < MIN_IMPROVE_ODDS) {
                improveOdds = MIN_IMPROVE_ODDS;
            }
        }

        // Get pot odds
        int potSize = context.getPotSize();
        // amountToCall already declared above for check-raise logic
        float potOdds = 0;
        if (amountToCall > 0 && potSize > 0) {
            potOdds = (100.0f * amountToCall) / (potSize + amountToCall);
        }

        // Check for check-raise opportunity (tracked across rounds)
        boolean intendCheckRaise = checkRaiseIntent;

        // Detect if our hole cards are involved in the hand
        boolean holeInvolved = isHoleInvolved(holeCards, communityCards, handScore, context);

        // Dispatch based on hand type
        return evaluateHandType(handRank, handScore, holeInvolved, handStrength, improveOdds, potOdds, board, player,
                options, context, randomPct, holeCards, communityCards);
    }

    /**
     * Evaluate action based on hand type (Royal Flush down to High Card).
     */
    private PlayerAction evaluateHandType(int handRank, long handScore, boolean holeInvolved, float handStrength,
            double improveOdds, float potOdds, BoardTexture board, GamePlayerInfo player, ActionOptions options,
            AIContext context, int randomPct, Card[] holeCards, Card[] communityCards) {

        int amountToCall = context.getAmountToCall(player);
        int playersAfter = context.getNumPlayersYetToAct(player);
        int numActivePlayers = context.getNumActivePlayers();

        PlayerAction action = null;

        switch (handRank) {
            case 9 : // Royal Flush
            case 8 : // Straight Flush
            case 7 : // Quads
            case 6 : // Full House
                if (holeInvolved) {
                    return doRaiseCheckRaiseBet(25, 25, context, player, "V1:StrFlush/FH");
                } else {
                    // Full house/straight flush on board
                    if (amountToCall > 0) {
                        return PlayerAction.call();
                    } else {
                        return PlayerAction.check();
                    }
                }

            case 5 : // Flush
                return evaluateFlush(holeInvolved, player, context, randomPct, board, holeCards, communityCards);

            case 4 : // Straight
                return evaluateStraight(holeInvolved, player, context, randomPct, board, holeCards, communityCards);

            case 3 : // Trips
                return evaluateTrips(holeInvolved, player, context, randomPct, board, handScore, holeCards,
                        communityCards);

            case 2 : // Two Pair
                return evaluateTwoPair(holeInvolved, player, context, randomPct, board, holeCards, communityCards);

            case 1 : // Pair
                action = evaluatePair(holeInvolved, player, context, randomPct, board, handRank, handScore, holeCards,
                        communityCards);
                if (action != null)
                    return action;
                break; // Fall through to generic strength logic

            case 0 : // High Card
                action = evaluateHighCard(player, context, randomPct, improveOdds, potOdds, holeCards, communityCards);
                if (action != null)
                    return action;
                break; // Fall through to generic strength logic

            default :
                return PlayerAction.fold();
        }

        // Generic hand strength fallthrough (matches V1Player lines 1091-1127)
        boolean better = (board.threeFlush || board.numOppStraights > 0 || board.boardPair);

        if (handStrength > 90 && !better) {
            return createRaise(context, player, "V1:90+ strength");
        } else if (handStrength > 75) {
            if (amountToCall == 0) {
                return createBet(context, player, "V1:75+ strength");
            } else {
                return PlayerAction.call();
            }
        } else if (amountToCall == 0) {
            // Checked around previous round
            if (lastRoundBetAmount == 0) {
                if (playersAfter == 0) {
                    if (!better) {
                        return createBet(context, player, "V1:check around, last to act");
                    } else if (getBluffFactor(context) > 85) {
                        return createBet(context, player, "V1:check around, last to act bet");
                    }
                } else {
                    if (getBluffFactor(context) > 50) {
                        return createBet(context, player, "V1:check around bluff");
                    }
                }
            } else if (improveOdds > 0.30) {
                return createBet(context, player, "V1:improve odds good (semi-bluff)");
            } else {
                if (playersAfter == 0 && numActivePlayers <= 3 && improveOdds > 0.17) {
                    return createBet(context, player, "V1:none after, improve odds decent, <3 left");
                }
            }
            return PlayerAction.check();
        }

        return foldIfPotOdds(context, player, holeCards, communityCards, "V1:default");
    }

    /**
     * Get community cards from context.
     */
    private Card[] getCommunityCards(AIContext context) {
        Card[] cards = context.getCommunityCards();
        return cards != null ? cards : new Card[0];
    }

    /**
     * Calculate hand strength as a percentage (0-100).
     * <p>
     * Uses Monte Carlo simulation to calculate win probability against N opponents.
     * Matches V1Player line 712: {@code player.getHandStrength() * 100.0f}
     */
    private float calculateHandStrength(Card[] holeCards, Card[] communityCards, AIContext context) {
        int numOpponents = context.getNumActivePlayers() - 1; // Exclude ourselves
        double strength = context.calculateHandStrength(holeCards, communityCards, Math.max(1, numOpponents));
        return (float) (strength * 100.0); // Convert to percentage (0-100)
    }

    /**
     * Check if hole cards are involved in the made hand.
     */
    private boolean isHoleInvolved(Card[] holeCards, Card[] communityCards, long handScore, AIContext context) {
        // Check if at least one hole card is part of the best 5-card hand
        return context.isHoleCardInvolved(holeCards, communityCards);
    }

    // ========== Pre-Flop Position Methods ==========

    private boolean isEarlyPosition(int position) {
        return position == 0; // Early
    }

    private boolean isMiddlePosition(int position) {
        return position == 1; // Middle
    }

    private boolean isLatePosition(int position) {
        return position == 2 || position == 3; // Late or Button
    }

    @Override
    public boolean wantsRebuy(GamePlayerInfo player, AIContext context) {
        return wantsRebuy(player, rebuyPropensity);
    }

    @Override
    public boolean wantsAddon(GamePlayerInfo player, AIContext context) {
        TournamentContext tournament = context.getTournament();
        int buyinChips = tournament.getStartingChips();
        return wantsAddon(player, addonPropensity, buyinChips);
    }

    /**
     * Sklansky's all-in system from "Tournament Poker for Advanced Players" (pp
     * 128-133). Used by tight players in hard mode when 5+ players at table.
     */
    private PlayerAction getSklankskySystem(HandSorted hole, GamePlayerInfo player, ActionOptions options,
            AIContext context) {
        boolean potRaised = context.hasBeenRaised();

        // If pot is raised, only continue with premium hands
        if (potRaised) {
            if (hole.isEquivalent(SklankskyRanking.AA) || hole.isEquivalent(SklankskyRanking.KK)
                    || hole.isEquivalent(SklankskyRanking.AKs)) {
                return createAllIn(context, player, "V1:system raised");
            } else {
                return PlayerAction.fold();
            }
        }

        // Calculate "the number" = (stack / pot) * playersAfter * (playersBefore + 1)
        // This determines hand strength needed
        int potSize = context.getPotSize();
        int playerStack = player.getChipCount();
        int playersAfter = context.getNumPlayersYetToAct(player);
        int playersBefore = context.getNumPlayersWhoActed(player);

        float theNumber = (float) playerStack / (float) potSize;
        if (playersAfter > 0) {
            theNumber *= playersAfter;
        }
        theNumber *= (playersBefore + 1);

        // Determine if we should go all-in based on "the number" and hand strength
        boolean shouldAllIn = false;

        if (theNumber >= 400) {
            if (hole.isEquivalent(SklankskyRanking.AA)) {
                shouldAllIn = true;
            }
        } else if (theNumber >= 200) {
            if (hole.isEquivalent(SklankskyRanking.AA) || hole.isEquivalent(SklankskyRanking.KK)) {
                shouldAllIn = true;
            }
        } else if (theNumber >= 150) {
            if (hole.isEquivalent(SklankskyRanking.AA) || hole.isEquivalent(SklankskyRanking.KK)
                    || hole.isEquivalent(SklankskyRanking.QQ) || hole.isEquivalent(SklankskyRanking.AKs)
                    || hole.isEquivalent(SklankskyRanking.AKo)) {
                shouldAllIn = true;
            }
        } else if (theNumber >= 100) {
            if (hole.isEquivalent(SklankskyRanking.AA) || hole.isEquivalent(SklankskyRanking.KK)
                    || hole.isEquivalent(SklankskyRanking.QQ) || hole.isEquivalent(SklankskyRanking.JJ)
                    || hole.isEquivalent(SklankskyRanking.TT) || hole.isEquivalent(SklankskyRanking.AKs)
                    || hole.isEquivalent(SklankskyRanking.AKo) || hole.isEquivalent(SklankskyRanking.AQs)
                    || hole.isEquivalent(SklankskyRanking.AQo) || hole.isEquivalent(SklankskyRanking.KQs)
                    || hole.isEquivalent(SklankskyRanking.KQo)) {
                shouldAllIn = true;
            }
        } else if (theNumber >= 80) {
            if (hole.isPair() || hole.isEquivalent(SklankskyRanking.AKs) || hole.isEquivalent(SklankskyRanking.AKo)
                    || hole.isEquivalent(SklankskyRanking.AQs) || hole.isEquivalent(SklankskyRanking.AQo)
                    || hole.isEquivalent(SklankskyRanking.KQs) || hole.isEquivalent(SklankskyRanking.KQo)
                    || (hole.isSuited() && hole.isInHand(Card.ACE)) || (hole.isSuited() && hole.hasConnector(0, 4))) {
                shouldAllIn = true;
            }
        } else if (theNumber >= 20) {
            // 20-40: same as below plus any suited cards
            if (theNumber < 40 && hole.isSuited()) {
                shouldAllIn = true;
            }

            // 40-60: same as 60-80 plus any king
            if (!shouldAllIn && theNumber < 60 && hole.isInHand(Card.KING)) {
                shouldAllIn = true;
            }

            if (!shouldAllIn && (hole.isPair() || hole.isInHand(Card.ACE) || hole.isEquivalent(SklankskyRanking.KQo)
                    || (hole.isSuited() && hole.isInHand(Card.KING)) || (hole.isSuited() && hole.hasConnector(1)))) {
                shouldAllIn = true;
            }
        } else {
            // theNumber < 20: always go all-in
            shouldAllIn = true;
        }

        if (shouldAllIn) {
            return createAllIn(context, player, "V1:system key num=" + theNumber);
        } else {
            return PlayerAction.fold();
        }
    }

    /**
     * Pre-flop decisions from early position.
     */
    private PlayerAction getPreFlopEarly(HandSorted hole, int sklanskyRank, int randomPct, int looseThreshold,
            boolean potRaised, ActionOptions options, AIContext context, GamePlayerInfo player) {

        if (potRaised) {
            // Premium hands: AA, KK, QQ, AKs
            if (hole.isEquivalent(SklankskyRanking.AA) || hole.isEquivalent(SklankskyRanking.KK)
                    || hole.isEquivalent(SklankskyRanking.QQ) || hole.isEquivalent(SklankskyRanking.AKs)) {
                return createRaise(context, player, "V1:early raised AA-AKs");
            } else if (sklanskyRank <= SklankskyRanking.MAXGROUP2) {
                return PlayerAction.call();
            } else {
                return foldOrLooseCheck(hole, randomPct, getTightFactor(context, player), context, player,
                        "V1:early raised");
            }
        } else {
            // No callers yet
            int numCallers = getNumCallers(context);
            if (numCallers == 0) {
                // Premium hands: almost always raise (97%)
                if (hole.isEquivalent(SklankskyRanking.AA) || hole.isEquivalent(SklankskyRanking.KK)
                        || hole.isEquivalent(SklankskyRanking.QQ) || hole.isEquivalent(SklankskyRanking.AKo)
                        || hole.isEquivalent(SklankskyRanking.AQo)) {
                    if (randomPct <= 97) {
                        return createRaise(context, player, "V1:early 0 caller, 97%");
                    } else {
                        return PlayerAction.call();
                    }
                }
                // Good hands: raise 2/3 of the time
                else if (hole.isEquivalent(SklankskyRanking.AKs) || hole.isEquivalent(SklankskyRanking.KQs)
                        || hole.isEquivalent(SklankskyRanking.AJs)) {
                    if (randomPct <= 66) {
                        return createRaise(context, player, "V1:early 0 caller 66%");
                    } else {
                        return PlayerAction.call();
                    }
                }
                // Suited group 4: occasionally raise (33%)
                else if (randomPct <= 33 && sklanskyRank <= SklankskyRanking.MAXGROUP4 && hole.isSuited()) {
                    return createRaise(context, player, "V1:early suited G4 33%");
                }
            }

            // Play groups 1-3 from early position
            if (sklanskyRank <= SklankskyRanking.MAXGROUP3) {
                return PlayerAction.call();
            }
            // Occasionally play suited/paired lesser hands (2 groups down)
            else if (sklanskyRank <= (SklankskyRanking.MAXGROUP3 + (2 * SklankskyRanking.MULT))
                    && randomPct <= looseThreshold && (hole.isSuited() || hole.isPair())) {
                return PlayerAction.call();
            }

            return foldOrLooseCheck(hole, randomPct, getTightFactor(context, player), context, player, "V1:early");
        }
    }

    /**
     * Pre-flop decisions from middle position.
     */
    private PlayerAction getPreFlopMiddle(HandSorted hole, int sklanskyRank, int randomPct, int looseThreshold,
            boolean potRaised, ActionOptions options, AIContext context, GamePlayerInfo player) {

        if (potRaised) {
            // Almost always re-raise with premium hands (97%)
            if (hole.isEquivalent(SklankskyRanking.AA) || hole.isEquivalent(SklankskyRanking.KK)
                    || hole.isEquivalent(SklankskyRanking.QQ) || hole.isEquivalent(SklankskyRanking.AKs)
                    || hole.isEquivalent(SklankskyRanking.AKo)) {
                if (randomPct <= 97) {
                    return createRaise(context, player, "V1:middle raised 97%");
                } else {
                    return PlayerAction.call();
                }
            }
            // Occasionally re-raise with group 4 (5%)
            else if (randomPct <= 5 && sklanskyRank <= SklankskyRanking.MAXGROUP4) {
                return createRaise(context, player, "V1:middle raised 5% G4");
            } else if (sklanskyRank <= SklankskyRanking.MAXGROUP3) {
                return PlayerAction.call();
            } else {
                return foldOrLooseCheck(hole, randomPct, getTightFactor(context, player), context, player,
                        "V1:middle raised");
            }
        } else {
            int numCallers = getNumCallers(context);

            // No callers: raise groups 1-3
            if (numCallers == 0) {
                if (sklanskyRank <= SklankskyRanking.MAXGROUP3) {
                    return createRaise(context, player, "V1:middle 0 callers G3");
                } else if (sklanskyRank <= SklankskyRanking.MAXGROUP6) {
                    return PlayerAction.call();
                }
            } else {
                // With callers: raise groups 1-2 only
                if (sklanskyRank <= SklankskyRanking.MAXGROUP2) {
                    return createRaise(context, player, "V1:middle G2");
                }
            }

            // Call with groups 1-5
            if (sklanskyRank <= SklankskyRanking.MAXGROUP5) {
                return PlayerAction.call();
            }
            // Occasionally play suited/paired lesser hands
            else if (sklanskyRank <= (SklankskyRanking.MAXGROUP4 + (2 * SklankskyRanking.MULT))
                    && randomPct <= looseThreshold && (hole.isSuited() || hole.isPair())) {
                return PlayerAction.call();
            }

            return foldOrLooseCheck(hole, randomPct, getTightFactor(context, player), context, player, "V1:middle");
        }
    }

    /**
     * Pre-flop decisions from late position.
     */
    private PlayerAction getPreFlopLate(HandSorted hole, int sklanskyRank, int randomPct, int looseThreshold,
            boolean potRaised, int numActivePlayers, ActionOptions options, AIContext context, GamePlayerInfo player) {

        if (potRaised) {
            // Almost always re-raise with premium hands (97%)
            if (hole.isEquivalent(SklankskyRanking.AA) || hole.isEquivalent(SklankskyRanking.KK)
                    || hole.isEquivalent(SklankskyRanking.QQ) || hole.isEquivalent(SklankskyRanking.AKs)
                    || hole.isEquivalent(SklankskyRanking.AKo)) {
                if (randomPct <= 97) {
                    return createRaise(context, player, "V1:late raised 97%");
                } else {
                    return PlayerAction.call();
                }
            }
            // Occasionally re-raise with group 3 (5%)
            else if (randomPct <= 5 && sklanskyRank <= SklankskyRanking.MAXGROUP3) {
                return createRaise(context, player, "V1:late raised 5% G3");
            }
            // Call with group 2
            else if (sklanskyRank <= SklankskyRanking.MAXGROUP2) {
                return PlayerAction.call();
            } else {
                // Short-handed (3 or fewer): looser requirements
                if (numActivePlayers <= 3) {
                    if ((hole.isSuited() && hole.getCard(0).getRank() > 7) || hole.hasConnector(0, 9)
                            || hole.isInHand(Card.ACE) || hole.isPair()
                            || ((hole.isInHand(Card.KING) || hole.isInHand(Card.QUEEN) || hole.isInHand(Card.JACK))
                                    && hole.hasConnector(2, 9))) {
                        if (randomPct < 25) {
                            return createRaise(context, player, "V1:late <= 3 players");
                        } else {
                            return PlayerAction.call();
                        }
                    }
                }
                return foldOrLooseCheck(hole, randomPct, getTightFactor(context, player), context, player,
                        "V1:late raised");
            }
        } else {
            int numCallers = getNumCallers(context);

            // No callers: raise with group 8 and better (steal blinds)
            if (numCallers == 0) {
                if (sklanskyRank <= SklankskyRanking.MAXGROUP8) {
                    return createRaise(context, player, "V1:late 0 callers G8");
                }
            } else {
                // With callers: raise groups 1-3
                if (sklanskyRank <= SklankskyRanking.MAXGROUP3) {
                    return createRaise(context, player, "V1:late G3");
                } else if (sklanskyRank <= SklankskyRanking.MAXGROUP4 && randomPct < 15) {
                    return createRaise(context, player, "V1:late G4 15%");
                }
            }

            // Call with groups 1-6
            if (sklanskyRank <= SklankskyRanking.MAXGROUP6) {
                return PlayerAction.call();
            }
            // Occasionally play suited/paired lesser hands
            else if (sklanskyRank <= (SklankskyRanking.MAXGROUP6 + (2 * SklankskyRanking.MULT))
                    && randomPct <= looseThreshold && (hole.isSuited() || hole.isPair())) {
                return PlayerAction.call();
            }

            // Short-handed: play looser
            if (numActivePlayers <= 3) {
                if ((hole.isSuited() && hole.getCard(0).getRank() > 7) || hole.hasConnector(0, 9)
                        || hole.isInHand(Card.ACE) || hole.isPair()
                        || ((hole.isInHand(Card.KING) || hole.isInHand(Card.QUEEN)) && hole.hasConnector(2, 9))) {
                    int amountToCall = context.getAmountToCall(player);
                    if (randomPct < 25) {
                        if (amountToCall == 0) {
                            return createBet(context, player, "V1:late <= 3 players");
                        } else {
                            return createRaise(context, player, "V1:late <= 3 players");
                        }
                    } else {
                        if (amountToCall == 0) {
                            return createBet(context, player, "V1:late <= 3 players");
                        } else {
                            return PlayerAction.call();
                        }
                    }
                }
            } else {
                // Loose players play more hands
                randomPct = random.nextInt(100) + 1;
                int tightFactor = getTightFactor(context, player);
                if (randomPct > tightFactor) {
                    int connectorGap = (tightFactor < 10) ? 1 : 0;
                    randomPct = random.nextInt(100) + 1;
                    if (hole.isSuited() || hole.hasConnector(connectorGap) || hole.isInHand(Card.ACE)
                            || hole.isPair()) {
                        if (randomPct < 25 && skillLevel != AI_EASY) {
                            return createRaise(context, player, "V1:loose suited/connector raise");
                        } else {
                            return PlayerAction.call();
                        }
                    } else {
                        if (randomPct < 15 && skillLevel != AI_EASY) {
                            return createRaise(context, player, "V1:loose garbage raise");
                        } else if (randomPct < 50) {
                            return PlayerAction.call();
                        }
                    }
                }
            }

            return foldOrLooseCheck(hole, randomPct, getTightFactor(context, player), context, player, "V1:late");
        }
    }

    /**
     * Pre-flop decisions from blind positions.
     */
    private PlayerAction getPreFlopBlind(HandSorted hole, int sklanskyRank, int randomPct, int tightFactor,
            boolean potRaised, ActionOptions options, AIContext context, GamePlayerInfo player) {

        int amountToCall = context.getAmountToCall(player);
        boolean isBigBlind = context.isBigBlind(player);

        if (potRaised) {
            // Re-raise with group 1
            if (sklanskyRank <= SklankskyRanking.MAXGROUP1) {
                return createRaise(context, player, "V1:blind raised G1");
            }
            // Big blind with group 2 and 2 or fewer callers: re-raise
            else if (isBigBlind && sklanskyRank <= SklankskyRanking.MAXGROUP2 && getNumCallers(context) <= 2) {
                return createRaise(context, player, "V1:bigblind raised G2, <= 2 callers");
            } else if (sklanskyRank <= SklankskyRanking.MAXGROUP2) {
                return PlayerAction.call();
            }
        } else {
            // No raise: raise with group 2, call with group 3
            if (sklanskyRank <= SklankskyRanking.MAXGROUP2) {
                return createRaise(context, player, "V1:blind G2");
            } else if (sklanskyRank <= SklankskyRanking.MAXGROUP3) {
                return PlayerAction.call();
            }
        }

        // Loose play from blinds
        randomPct = random.nextInt(100) + 1;
        if (randomPct > (tightFactor + 10)) { // Tighten up a bit in blinds
            randomPct = random.nextInt(100) + 1;
            if (hole.isSuited() || hole.hasConnector(0) || hole.isInHand(Card.ACE) || hole.isPair()) {
                if (randomPct < 15 && skillLevel != AI_EASY) {
                    return createRaise(context, player, "V1:blind loose suited/connector raise");
                } else {
                    return PlayerAction.call();
                }
            } else {
                if (randomPct < 10 && skillLevel != AI_EASY) {
                    return createRaise(context, player, "V1:blind loose garbage raise");
                } else if (randomPct < 40) {
                    return PlayerAction.call();
                }
            }
        }

        // No money to call: check
        if (amountToCall == 0) {
            return PlayerAction.check();
        }

        return foldOrLooseCheck(hole, randomPct, tightFactor, context, player, "V1:blind");
    }

    /**
     * Board texture information for post-flop decisions.
     */
    private static class BoardTexture {
        boolean flushDraw; // 2 suited cards on board
        boolean threeFlush; // 3+ suited cards on board
        boolean boardPair; // Pair on board
        boolean straightDraw; // Possible straight draw
        int numOppStraights; // Number of possible opponent straights

        BoardTexture(boolean flushDraw, boolean threeFlush, boolean boardPair, boolean straightDraw,
                int numOppStraights) {
            this.flushDraw = flushDraw;
            this.threeFlush = threeFlush;
            this.boardPair = boardPair;
            this.straightDraw = straightDraw;
            this.numOppStraights = numOppStraights;
        }
    }

    /**
     * Analyze board texture for draws and dangerous cards.
     */
    private BoardTexture analyzeBoardTexture(Card[] communityCards, AIContext context) {
        // Use AIContext methods for board analysis (matches original V1Player logic)
        boolean flushDraw = context.hasFlushDraw(communityCards);
        boolean straightDraw = context.hasStraightDraw(communityCards);
        int numOppStraights = context.getNumOpponentStraights(communityCards);

        // Count suits for 3-flush detection
        boolean threeFlush = false;
        if (communityCards != null) {
            int[] suitCounts = new int[4];
            for (Card card : communityCards) {
                if (card != null) {
                    suitCounts[card.getSuit()]++;
                }
            }
            for (int count : suitCounts) {
                if (count >= 3) {
                    threeFlush = true;
                    break;
                }
            }
        }

        // Detect board pair
        boolean boardPair = false;
        if (communityCards != null) {
            int[] rankCounts = new int[Card.ACE + 1];
            for (Card card : communityCards) {
                if (card != null) {
                    rankCounts[card.getRank()]++;
                }
            }
            for (int count : rankCounts) {
                if (count >= 2) {
                    boardPair = true;
                    break;
                }
            }
        }

        // On river, no more draws possible
        if (context.getBettingRound() == 3) {
            flushDraw = false;
            straightDraw = false;
        }

        return new BoardTexture(flushDraw, threeFlush, boardPair, straightDraw, numOppStraights);
    }

    /**
     * Evaluate flush hand.
     */
    private PlayerAction evaluateFlush(boolean holeInvolved, GamePlayerInfo player, AIContext context, int randomPct,
            BoardTexture board, Card[] holeCards, Card[] communityCards) {

        int amountToCall = context.getAmountToCall(player);
        int bettingRound = context.getBettingRound();

        if (holeInvolved) {
            // We have a flush with our hole cards
            int majorSuit = context.getMajorSuit(holeCards, communityCards);
            // Use HandInfo.isNutFlush() via context (matches V1Player lines 803, 807)
            boolean isNutFlush = context.isNutFlush(holeCards, communityCards, majorSuit, 1);
            boolean isTopFlush = context.isNutFlush(holeCards, communityCards, majorSuit, 3) && !isNutFlush;

            if (bettingRound == 1) { // Flop
                if (isNutFlush) {
                    return doRaiseCheckRaiseBet(5, 25, context, player, "V1:Nut Flush");
                } else if (isTopFlush) {
                    if (isReRaised(context, player)) {
                        if (randomPct < 50) {
                            return foldIfPotOdds(context, player, holeCards, communityCards,
                                    "V1:re-raised 2nd nut flush");
                        } else {
                            return PlayerAction.call();
                        }
                    }
                    return doRaiseCheckRaiseBet(5, 0, context, player, "V1:2nd Nut Flush");
                } else {
                    // Regular flush
                    if (amountToCall == 0) {
                        return createBet(context, player, "V1:flush");
                    } else {
                        if (isReRaised(context, player)) {
                            if (randomPct < 50) {
                                return PlayerAction.fold();
                            } else {
                                return PlayerAction.call();
                            }
                        }
                        return createRaise(context, player, "V1:flush");
                    }
                }
            } else { // Turn or River
                if (isNutFlush) {
                    return doRaiseCheckRaiseBet(60, 5, context, player, "V1:Nut Flush");
                } else if (isTopFlush) {
                    if (isReRaised(context, player)) {
                        if (randomPct < 50) {
                            return PlayerAction.fold();
                        } else {
                            return PlayerAction.call();
                        }
                    }
                    return doRaiseCheckRaiseBet(60, 0, context, player, "V1:2nd Nut Flush");
                } else {
                    // Regular flush
                    if (amountToCall == 0) {
                        return createBet(context, player, "V1:flush");
                    } else {
                        if (isReRaised(context, player)) {
                            if (randomPct < 50) {
                                return PlayerAction.fold();
                            } else {
                                return PlayerAction.call();
                            }
                        }
                        return createRaise(context, player, "V1:flush");
                    }
                }
            }
        } else {
            // Flush on board
            if (amountToCall == 0) {
                return PlayerAction.check();
            } else {
                return foldIfPotOdds(context, player, holeCards, communityCards, "V1:Flush, no hole");
            }
        }
    }

    /**
     * Evaluate straight hand.
     */
    private PlayerAction evaluateStraight(boolean holeInvolved, GamePlayerInfo player, AIContext context, int randomPct,
            BoardTexture board, Card[] holeCards, Card[] communityCards) {

        int amountToCall = context.getAmountToCall(player);
        int bettingRound = context.getBettingRound();

        if (holeInvolved) {
            // We have a straight
            if (board.threeFlush) {
                return doBiggerHandPossibleBets("V1:str, pos flush", 50, 15, 20, 4, context, player, holeCards,
                        communityCards);
            } else {
                if (isRaised(context, player)) {
                    if (board.boardPair) {
                        return foldIfPotOdds(context, player, holeCards, communityCards, "V1:re-raised, paired board");
                    }
                }
                // Don't check-raise on flop or with flush draw (allow drawing out)
                int checkRaisePct = (bettingRound == 1 || board.flushDraw) ? 0 : 60;
                return doRaiseCheckRaiseBet(checkRaisePct, 2, context, player, "V1:Straight");
            }
        } else {
            // Straight on board
            if (amountToCall == 0) {
                // Bluff we have higher straight (only when no bet last round, matches V1Player
                // line 900)
                if (lastRoundBetAmount == 0 && randomPct < 25) {
                    return createBet(context, player, "V1:Straight, no hole");
                }
                return PlayerAction.check();
            } else {
                return foldIfPotOdds(context, player, holeCards, communityCards, "V1:Straight, no hole");
            }
        }
    }

    /**
     * Evaluate trips (three of a kind).
     */
    private PlayerAction evaluateTrips(boolean holeInvolved, GamePlayerInfo player, AIContext context, int randomPct,
            BoardTexture board, long handScore, Card[] holeCards, Card[] communityCards) {

        int amountToCall = context.getAmountToCall(player);
        int bettingRound = context.getBettingRound();

        if (holeInvolved && holeCards != null) {
            // Hidden trips (pocket pair matched) or pair on board
            boolean pocketPair = (holeCards[0].getRank() == holeCards[1].getRank());

            if (pocketPair) {
                // Hidden trips - very strong
                if (board.threeFlush || board.numOppStraights > 0) {
                    return doBiggerHandPossibleBets("V1:trips(hole), pos flush/str", 60, 25, 30, 3, context, player,
                            holeCards, communityCards);
                } else {
                    boolean isTopTrip = isTopTrip(holeCards, communityCards, context);
                    int checkRaisePct = (bettingRound == 1) ? 5 : 60;
                    int allInPct = 5;
                    if (isTopTrip) {
                        return doRaiseCheckRaiseBet(checkRaisePct, allInPct, context, player, "V1:Top Trips");
                    } else {
                        checkRaisePct = (bettingRound == 1) ? 0 : 20;
                        return doRaiseCheckRaiseBet(checkRaisePct, 0, context, player, "V1:Middle/low Trips");
                    }
                }
            } else {
                // Pair on board, one matching in hand
                if (board.threeFlush || board.numOppStraights > 0) {
                    return doBiggerHandPossibleBets("V1:trips(2 board), pos flush/str", 60, 25, 30, 3, context, player,
                            holeCards, communityCards);
                } else {
                    if (isReRaised(context, player)) {
                        if (randomPct < 50) {
                            return PlayerAction.call();
                        } else {
                            return PlayerAction.fold();
                        }
                    }

                    boolean isTopTrip = isTopTrip(holeCards, communityCards, context);
                    int checkRaisePct = (bettingRound == 1) ? 5 : 60;
                    if (isTopTrip) {
                        return doRaiseCheckRaiseBet(checkRaisePct, 0, context, player, "V1:Top Trips,Pair on board");
                    } else {
                        checkRaisePct = (bettingRound == 1) ? 0 : 20;
                        return doRaiseCheckRaiseBet(checkRaisePct, 0, context, player,
                                "V1:Middle/low Trips,Pair on board");
                    }
                }
            }
        } else {
            // Trips on board - check for ace kicker (matches V1Player line 960-962)
            int[] best = context.getBest5CardRanks(holeCards, communityCards);
            if (best[1] == Card.ACE && hasCard(holeCards, Card.ACE)) {
                // Always call with ace kicker (original line 962)
                return PlayerAction.call();
            }
            // Non-ace kicker
            if (amountToCall == 0) {
                return PlayerAction.check();
            } else {
                return foldIfPotOdds(context, player, holeCards, communityCards, "V1:trips, non ace kicker");
            }
        }
    }

    /**
     * Evaluate two pair.
     */
    private PlayerAction evaluateTwoPair(boolean holeInvolved, GamePlayerInfo player, AIContext context, int randomPct,
            BoardTexture board, Card[] holeCards, Card[] communityCards) {

        int amountToCall = context.getAmountToCall(player);
        int playersAfter = context.getNumPlayersYetToAct(player);

        if (holeInvolved) {
            if (board.threeFlush || board.numOppStraights > 0 || board.boardPair) {
                return doBiggerHandPossibleBets("V1:2 pair, pos flush/str/board paired", 60, 25, 35, 2, context, player,
                        holeCards, communityCards);
            } else {
                int checkRaisePct = (context.getBettingRound() == 1) ? 0 : 20;
                return doRaiseCheckRaiseBet(checkRaisePct, 0, context, player, "V1:2 pair, no bigger pos");
            }
        } else {
            // Two pair on board
            if (amountToCall == 0) {
                if (playersAfter == 0 && randomPct < 50) {
                    return createBet(context, player, "V1:two pair on board bluff");
                }
                return PlayerAction.check();
            } else {
                if (isRaised(context, player) || board.flushDraw || board.numOppStraights > 0) {
                    return foldIfPotOdds(context, player, holeCards, communityCards, "V1:raised two pair on board");
                } else {
                    if (playersAfter == 0 || (holeCards != null && hasCard(holeCards, Card.ACE))) {
                        return PlayerAction.call();
                    } else {
                        return foldIfPotOdds(context, player, holeCards, communityCards, "V1:two pair on board");
                    }
                }
            }
        }
    }

    /**
     * Evaluate pair.
     */
    private PlayerAction evaluatePair(boolean holeInvolved, GamePlayerInfo player, AIContext context, int randomPct,
            BoardTexture board, int handRank, long handScore, Card[] holeCards, Card[] communityCards) {

        int amountToCall = context.getAmountToCall(player);
        int playersAfter = context.getNumPlayersYetToAct(player);

        if (holeInvolved && holeCards != null) {
            boolean pocketPair = (holeCards[0].getRank() == holeCards[1].getRank());

            if (pocketPair) {
                boolean isOverpair = isOverpair(holeCards, communityCards);

                if (isOverpair) {
                    if (board.threeFlush || board.numOppStraights > 0) {
                        return doBiggerHandPossibleBets("V1:overpair, pos flush/str", 70, 30, 35, 1, context, player,
                                holeCards, communityCards);
                    } else {
                        if (isReRaised(context, player)) {
                            return foldIfPotOdds(context, player, holeCards, communityCards, "V1:re-raised overpair");
                        }

                        if (amountToCall == 0) {
                            return createBet(context, player, "V1:overpair");
                        } else {
                            return createRaise(context, player, "V1:overpair");
                        }
                    }
                }
            } else {
                // Paired one card from board
                boolean isTopPair = isTopPair(holeCards, communityCards, context);

                if (isTopPair) {
                    if (board.threeFlush || board.numOppStraights > 0) {
                        return doBiggerHandPossibleBets("V1:top pair, pos flush/str", 70, 30, 35, 1, context, player,
                                holeCards, communityCards);
                    } else {
                        if (amountToCall == 0) {
                            return createBet(context, player, "V1:top pair");
                        } else {
                            boolean goodKicker = isGoodKicker(handRank, holeCards, context, communityCards);
                            if (goodKicker) {
                                return createRaise(context, player, "V1:toppair, good kicker");
                            } else {
                                return PlayerAction.call();
                            }
                        }
                    }
                } else {
                    // Middle or low pair
                    if (amountToCall == 0 && playersAfter == 0) {
                        return createBet(context, player, "V1:pair, none after");
                    } else if (amountToCall == 0 && playersAfter == 1) {
                        if (randomPct < 50) {
                            return createBet(context, player, "V1:pair, one after");
                        }
                    } else if (amountToCall > 0) {
                        if (!isRaised(context, player) && playersAfter == 0) {
                            if (randomPct < 50) {
                                return PlayerAction.call();
                            }
                        }
                    }
                }
            }
        }

        // Fall through to pot odds check
        PlayerAction action = checkPotOdds(context, player, holeCards, communityCards, "V1:pair");
        if (action != null) {
            return action;
        }

        // Return null to fall through to generic strength logic (matches original)
        return null;
    }

    /**
     * Evaluate high card (no pair).
     */
    private PlayerAction evaluateHighCard(GamePlayerInfo player, AIContext context, int randomPct, double improveOdds,
            float potOdds, Card[] holeCards, Card[] communityCards) {

        int amountToCall = context.getAmountToCall(player);
        int playersAfter = context.getNumPlayersYetToAct(player);
        int playersBefore = context.getNumPlayersWhoActed(player);

        // Ace high - some bluff value
        if (holeCards != null && hasCard(holeCards, Card.ACE)) {
            if (playersAfter == 0 && amountToCall == 0) {
                return createBet(context, player, "V1:high card, last to act, no bet");
            }
            // Original V1Player line 1077-1078 has a TODO comment with no implementation
            // Do NOT add heads-up bluff logic here - it was never in the original
        }

        // Check pot odds
        PlayerAction action = checkPotOdds(context, player, holeCards, communityCards, "V1:high");
        if (action != null) {
            return action;
        }

        // Return null to fall through to generic strength logic (matches original)
        return null;
    }

    /**
     * Helper to check if hole cards contain a specific rank.
     */
    private boolean hasCard(Card[] holeCards, int rank) {
        if (holeCards == null)
            return false;
        for (Card card : holeCards) {
            if (card.getRank() == rank)
                return true;
        }
        return false;
    }

    /**
     * Check if player was re-raised this round.
     * <p>
     * A re-raise occurs when:
     * <ul>
     * <li>Our last action was a raise</li>
     * <li>AND there's now been another raise (we have amount to call)</li>
     * </ul>
     * <p>
     * Matches original V1Player logic (_nLast == HandAction.ACTION_RAISE).
     */
    private boolean isReRaised(AIContext context, GamePlayerInfo player) {
        // Check if our last action was a raise and there's been another raise since
        if (lastAction != null
                && lastAction.actionType() == com.donohoedigital.games.poker.core.state.ActionType.RAISE) {
            int amountToCall = context.getAmountToCall(player);
            return amountToCall > 0;
        }
        return false;
    }

    /**
     * Check if there's been any raise this round.
     * <p>
     * Matches original V1Player logic (line 1465): checks if player's last action
     * was a call, bet, or raise, meaning the player already acted and someone has
     * raised since.
     */
    private boolean isRaised(AIContext context, GamePlayerInfo player) {
        // Check if our last action was call/bet/raise (meaning someone raised after us)
        if (lastAction != null) {
            com.donohoedigital.games.poker.core.state.ActionType actionType = lastAction.actionType();
            return actionType == com.donohoedigital.games.poker.core.state.ActionType.CALL
                    || actionType == com.donohoedigital.games.poker.core.state.ActionType.BET
                    || actionType == com.donohoedigital.games.poker.core.state.ActionType.RAISE;
        }
        return false;
    }

    /**
     * Raise, check-raise, or bet based on situation.
     */
    private PlayerAction doRaiseCheckRaiseBet(int checkRaisePct, int allInPct, AIContext context, GamePlayerInfo player,
            String reason) {

        int amountToCall = context.getAmountToCall(player);
        int playersAfter = context.getNumPlayersYetToAct(player);
        int randomPct = random.nextInt(100) + 1;

        if (amountToCall > 0) {
            // Raise 1-3x current bet (matches original V1Player logic)
            int multiplier = (random.nextInt(3) + 1); // 1, 2, or 3
            int raiseAmount = amountToCall * multiplier;

            // Avoid re-raises unless 3 or fewer players - then go all-in
            if (isReRaised(context, player)) {
                if (skillLevel != AI_EASY && context.getNumActivePlayers() <= 3) {
                    return PlayerAction.bet(player.getChipCount()); // All-in
                }
                return PlayerAction.call(); // Force call, no re-raise
            }

            GameHand hand = context.getCurrentHand();
            int biggestBetRaise = hand != null ? hand.getMinRaise() : raiseAmount;
            int playerStack = player.getChipCount();

            // If >95% of stack, go all-in
            if (raiseAmount > (playerStack * 0.95)) {
                raiseAmount = playerStack;
            }

            // Ensure meets minimum
            if (raiseAmount < biggestBetRaise) {
                raiseAmount = biggestBetRaise;
            }

            return PlayerAction.raise(raiseAmount + amountToCall);
        } else {
            // No bet yet
            // Check-raise if hard mode, players after us, and random allows
            if (skillLevel == AI_HARD && playersAfter > 0 && randomPct <= checkRaisePct) {
                // Set check-raise intent for next round
                checkRaiseIntent = true;
                return PlayerAction.check();
            } else if (skillLevel != AI_EASY && randomPct > checkRaisePct && randomPct < (checkRaisePct + allInPct)) {
                return createAllIn(context, player, reason);
            } else {
                return createBet(context, player, reason);
            }
        }
    }

    /**
     * Betting logic when bigger hands are possible on board.
     */
    private PlayerAction doBiggerHandPossibleBets(String reason, int reRaiseFoldPct, int raiseFoldPct, int foldPct,
            int handType, AIContext context, GamePlayerInfo player, Card[] holeCards, Card[] communityCards) {

        int amountToCall = context.getAmountToCall(player);
        int numActivePlayers = context.getNumActivePlayers();
        int randomPct = random.nextInt(100) + 1;

        // Adjust fold percentages based on number of players
        if (numActivePlayers >= 4) {
            reRaiseFoldPct += 10;
            raiseFoldPct += 10;
            foldPct += 10;
        } else if (numActivePlayers <= 2) {
            reRaiseFoldPct -= 10;
            raiseFoldPct -= 10;
            foldPct -= 10;
        }

        // Limper detection logic (B10 - matches V1Player lines 1293-1349)
        int currentRound = context.getBettingRound();

        // Check if raiser limped in previous round
        if (currentRound == 1
                && (raiserPreAction == AIContext.ACTION_CALL || raiserPreAction == AIContext.ACTION_CHECK)) {
            reRaiseFoldPct += 20;
            raiseFoldPct += 20;
        } else if (currentRound == 2
                && (raiserFlopAction == AIContext.ACTION_CALL || raiserFlopAction == AIContext.ACTION_CHECK)) {
            reRaiseFoldPct += 25;
            raiseFoldPct += 25;
        } else if (currentRound == 3
                && (raiserTurnAction == AIContext.ACTION_CALL || raiserTurnAction == AIContext.ACTION_CHECK)) {
            reRaiseFoldPct += 30;
            raiseFoldPct += 30;
        }

        // Reduce fold percentages if raiser raised pre-flop and we have trips+
        if (currentRound == 1 && raiserPreAction == AIContext.ACTION_RAISE && handType >= 3) {
            reRaiseFoldPct -= 20;
            raiseFoldPct -= 20;
        }

        // Check if bettor limped in previous round (affects fold percentage only)
        if (currentRound == 1
                && (bettorPreAction == AIContext.ACTION_CALL || bettorPreAction == AIContext.ACTION_CHECK)) {
            foldPct += 20;
        } else if (currentRound == 2
                && (bettorFlopAction == AIContext.ACTION_CALL || bettorFlopAction == AIContext.ACTION_CHECK)) {
            foldPct += 25;
        } else if (currentRound == 3
                && (bettorTurnAction == AIContext.ACTION_CALL || bettorTurnAction == AIContext.ACTION_CHECK)) {
            foldPct += 30;
        }

        // Reduce fold percentage if bettor raised pre-flop and we have trips+
        if (currentRound == 1 && bettorPreAction == AIContext.ACTION_RAISE && handType >= 3) {
            foldPct -= 20;
        }

        // No bet yet
        if (amountToCall == 0) {
            int playersBefore = context.getNumPlayersWhoActed(player);
            int bluffFactor = getBluffFactor(context);

            if (playersBefore == 0) {
                // First to act - always bet when no last round bet (matches V1Player line 1279)
                if (lastRoundBetAmount == 0 || randomPct < 25 || bluffFactor > 75) {
                    return createBet(context, player, reason + ":no bet, 1st to act");
                }
                return PlayerAction.check();
            } else {
                // Always bet when no last round bet (matches V1Player line 1285)
                if (lastRoundBetAmount == 0 || randomPct < 45 || bluffFactor > 50) {
                    return createBet(context, player, reason + ":no bet");
                } else {
                    return PlayerAction.check();
                }
            }
        } else {
            // Facing a bet/raise
            if (isReRaised(context, player)) {
                if (randomPct > reRaiseFoldPct) {
                    return PlayerAction.call();
                } else {
                    return foldIfPotOdds(context, player, holeCards, communityCards, reason + ":rereraised");
                }
            }

            if (isRaised(context, player)) {
                if (randomPct > raiseFoldPct) {
                    return PlayerAction.call();
                } else {
                    return foldIfPotOdds(context, player, holeCards, communityCards, reason + ":raised");
                }
            }

            // Just call or fold
            if (randomPct > foldPct) {
                return PlayerAction.call();
            } else {
                return foldIfPotOdds(context, player, holeCards, communityCards, reason);
            }
        }
    }

    /**
     * Check pot odds and call if favorable, otherwise return null.
     */
    private PlayerAction checkPotOdds(AIContext context, GamePlayerInfo player, Card[] holeCards, Card[] communityCards,
            String reason) {
        int amountToCall = context.getAmountToCall(player);
        if (amountToCall == 0) {
            return null;
        }

        int potSize = context.getPotSize();
        float potOdds = (100.0f * amountToCall) / (potSize + amountToCall);

        // Calculate improvement odds using context
        double improveOdds = context.calculateImprovementOdds(holeCards, communityCards) * 100.0;

        // Fudge factor for small bets
        float fudgeFactor = 0.0f;
        float betPercOfStack = (100.0f * amountToCall) / player.getChipCount();
        if (betPercOfStack < 10) {
            fudgeFactor = 3.0f;
        }

        if ((improveOdds + fudgeFactor) >= potOdds) {
            return PlayerAction.call();
        }

        return null;
    }

    /**
     * Fold unless pot odds favor calling.
     */
    private PlayerAction foldIfPotOdds(AIContext context, GamePlayerInfo player, Card[] holeCards,
            Card[] communityCards, String reason) {
        PlayerAction action = checkPotOdds(context, player, holeCards, communityCards, reason);
        if (action != null) {
            return action;
        }

        int randomPct = random.nextInt(100) + 1;
        int numActivePlayers = context.getNumActivePlayers();
        int tightFactor = getTightFactor(context, player);

        // Loose players sometimes call anyway
        if (numActivePlayers > 4 && randomPct > tightFactor) {
            int threshold = 10;
            if (randomPct < threshold) {
                return createRaise(context, player, "V1:loose potodds " + reason);
            } else if (randomPct < (threshold + 15)) {
                return PlayerAction.call();
            }
        }

        return PlayerAction.fold();
    }

    // ========== Helper Methods ==========

    /**
     * Rebuy logic for computer players. Package-private to allow V2Algorithm to
     * reuse.
     *
     * @param player
     *            Player considering rebuy
     * @param rebuyPropensity
     *            Rebuy propensity (0-100)
     * @return true if player should rebuy
     */
    static boolean wantsRebuy(GamePlayerInfo player, int rebuyPropensity) {
        int numRebuys = player.getNumRebuys();

        // BUG 395: Protect players from going broke
        if (numRebuys >= 5) {
            return false;
        }

        if (rebuyPropensity <= 25) {
            return true; // 25% always rebuy
        }
        if (rebuyPropensity <= 50) {
            return numRebuys < 3; // 25% rebuy up to 3 times
        }
        if (rebuyPropensity <= 75) {
            return numRebuys < 2; // 25% rebuy up to 2 times
        }
        if (rebuyPropensity <= 90) {
            return numRebuys < 1; // 15% rebuy once
        }

        // 10% never rebuy
        return false;
    }

    /**
     * Addon logic for computer players. Package-private to allow V2Algorithm to
     * reuse.
     * <p>
     * Extracted from V1Player.WantsAddon() - static logic with no Swing
     * dependencies.
     *
     * @param player
     *            Player considering addon
     * @param addonPropensity
     *            Addon propensity (0-100)
     * @param buyinChips
     *            Starting chip count (for comparison)
     * @return true if player should take addon
     */
    static boolean wantsAddon(GamePlayerInfo player, int addonPropensity, int buyinChips) {
        if (addonPropensity < 25) {
            return true; // 25% always addon
        }
        if (addonPropensity < 50) {
            return player.getChipCount() < (3 * buyinChips); // 25% if chips < 3x buyin
        }
        if (addonPropensity < 75) {
            return player.getChipCount() < (2 * buyinChips); // 25% if chips < 2x buyin
        }

        // 25% never addon
        return false;
    }

    /**
     * Get tight factor adjusted for game context.
     * <p>
     * Adjustments:
     * <ul>
     * <li>-20 if rebuy period still active (play looser)</li>
     * <li>+20 for easy AI (play tighter)</li>
     * <li>+10 for medium AI (play slightly tighter)</li>
     * <li>+20 on river for hard AI (play tighter on river)</li>
     * </ul>
     */
    private int getTightFactor(AIContext context, GamePlayerInfo player) {
        int adjusted = baseTightFactor;

        // Play looser during rebuy period (matches original V1Player logic)
        if (context.isRebuyPeriodActive()) {
            adjusted -= 20;
            if (adjusted < 0)
                adjusted = 0; // Clamp AFTER subtraction, BEFORE skill adjustment
        }

        // Skill level adjustments
        if (skillLevel == AI_EASY) {
            adjusted += 20;
        } else if (skillLevel == AI_MEDIUM) {
            adjusted += 10;
        }

        // Hard AI plays tighter on river
        if (skillLevel == AI_HARD && context.getBettingRound() == 3) { // River
            adjusted += 20;
        }

        return adjusted; // Remove Math.max(0, Math.min(100, ...)) to match original
    }

    /**
     * Get bluff factor adjusted for game context.
     * <p>
     * Adjustments:
     * <ul>
     * <li>-20 for easy AI (less bluffing)</li>
     * <li>-10 for medium AI (slightly less bluffing)</li>
     * <li>-20 on river for hard AI (less bluffing on river)</li>
     * </ul>
     */
    private int getBluffFactor(AIContext context) {
        int adjusted = baseBluffFactor;

        // Skill level adjustments
        if (skillLevel == AI_EASY) {
            adjusted -= 20;
        } else if (skillLevel == AI_MEDIUM) {
            adjusted -= 10;
        }

        // Hard AI bluffs less on river
        if (skillLevel == AI_HARD && context.getBettingRound() == 3) { // River
            adjusted -= 20;
        }

        return Math.max(0, adjusted);
    }

    /**
     * Get number of callers in the current round.
     * <p>
     * This counts players who have called but not raised.
     */
    private int getNumCallers(AIContext context) {
        return context.getNumCallers();
    }

    /**
     * Create a raise action with standard sizing.
     * <p>
     * Original V1Player logic: raises 3-4x big blind (75% / 25% distribution).
     * TODO: Original had note to use pot size for post-flop, not implemented yet.
     */
    private PlayerAction createRaise(AIContext context, GamePlayerInfo player, String reason) {
        GameHand hand = context.getCurrentHand();
        if (hand == null) {
            // Fallback if no hand context
            return PlayerAction.raise(100);
        }

        int minBet = hand.getMinBet();
        int raiseAmount = getRaiseAmount(minBet);

        // Avoid re-raises unless 3 or fewer players - then go all-in
        if (isReRaised(context, player)) {
            if (skillLevel != AI_EASY && context.getNumActivePlayers() <= 3) {
                return PlayerAction.bet(player.getChipCount()); // All-in
            }
            return PlayerAction.call(); // Force call, no re-raise
        }

        int biggestBetRaise = hand.getMinRaise();
        int playerStack = player.getChipCount();

        // If >95% of stack, go all-in
        if (raiseAmount > (playerStack * 0.95)) {
            raiseAmount = playerStack;
        }

        // Ensure meets minimum
        if (raiseAmount < biggestBetRaise) {
            raiseAmount = biggestBetRaise;
        }

        int amountToCall = context.getAmountToCall(player);
        return PlayerAction.raise(raiseAmount + amountToCall);
    }

    /**
     * Calculate raise amount (matches original V1Player.getRaise).
     * <p>
     * Raises 3-4 times the big blind (75% / 25% distribution).
     */
    private int getRaiseAmount(int bigBlind) {
        int randomPct = random.nextInt(100);
        // 75% chance: 3x BB, 25% chance: 4x BB
        if (randomPct < 75) {
            return bigBlind * 3;
        } else {
            return bigBlind * 4;
        }
    }

    /**
     * Create a bet action.
     * <p>
     * Original V1Player logic: Random bet from 1/4 to 4/4 of pot with weighted
     * distribution (30% at 1/4, 30% at 1/2, 30% at 3/4, 10% at full pot).
     */
    private PlayerAction createBet(AIContext context, GamePlayerInfo player, String reason) {
        GameHand hand = context.getCurrentHand();
        if (hand == null) {
            // Fallback if no hand context
            return PlayerAction.bet(100);
        }

        int potSize = context.getPotSize();
        int minBet = hand.getMinBet();

        // Match original weighted distribution
        int randomRoll = random.nextInt(10) + 1; // 1-10
        int fraction;
        if (randomRoll <= 2) {
            fraction = 1; // 20%: 1/4 pot
        } else if (randomRoll <= 5) {
            fraction = 2; // 30%: 2/4 pot (50%)
        } else if (randomRoll <= 8) {
            fraction = 3; // 30%: 3/4 pot (75%)
        } else {
            fraction = 4; // 20%: 4/4 pot (100%)
        }

        int betAmount = (int) ((double) potSize * (fraction / 4.0));

        // Round up to minimum bet
        int remainder = betAmount % minBet;
        if (remainder > 0) {
            betAmount += (minBet - remainder);
        }
        if (betAmount < minBet) {
            betAmount = minBet;
        }

        // If >95% of stack, go all-in
        int playerStack = player.getChipCount();
        if (betAmount > (playerStack * 0.95)) {
            betAmount = playerStack;
        }

        return PlayerAction.bet(betAmount);
    }

    /**
     * Create an all-in action.
     */
    private PlayerAction createAllIn(AIContext context, GamePlayerInfo player, String reason) {
        int allInAmount = player.getChipCount();
        return PlayerAction.bet(allInAmount);
    }

    /**
     * Fold unless pot odds or loose opponent suggest otherwise.
     * <p>
     * Extracted from V1Player._foldLooseCheck()
     */
    private PlayerAction foldOrLooseCheck(HandSorted hole, int randomPct, int tightFactor, AIContext context,
            GamePlayerInfo player, String debugMsg) {

        int amountToCall = context.getAmountToCall(player);

        // Check if raiser/bettor is loose (high raise/bet frequency)
        int looseLevel = 0;
        int threshold = 0;

        if (skillLevel != AI_EASY) {
            GamePlayerInfo raiser = context.getLastRaiser();
            GamePlayerInfo bettor = context.getLastBettor();

            if (raiser != null) {
                // Use opponent's raise frequency to adjust bluffing
                float raiseFreq = getOpponentRaiseFrequency(raiser, context);
                looseLevel = (int) (raiseFreq * 100);
                threshold = 15;
            } else if (bettor != null) {
                // Use opponent's bet frequency to adjust bluffing
                float betFreq = getOpponentBetFrequency(bettor, context);
                looseLevel = (int) (betFreq * 100);
                threshold = 40;
            }

            // Adjust threshold for short-handed games
            if (context.getNumActivePlayers() <= 3) {
                threshold += 20;
            }
        }

        // If opponent is loose, call/raise with decent hands
        if (looseLevel > threshold
                && (hole.isSuited() || hole.hasConnector(1, 7) || hole.isInHand(Card.ACE) || hole.isPair())) {
            randomPct = random.nextInt(100) + 1;
            int bluffFactor = getBluffFactor(context);
            if (bluffFactor > 75 && randomPct < 15) {
                return createRaise(context, player, "V1:loose check - " + debugMsg);
            } else {
                return PlayerAction.call();
            }
        }

        // Easy AI plays more hands
        if (skillLevel == AI_EASY) {
            int numCallers = getNumCallers(context);
            if (numCallers == 0
                    && (hole.isSuited() || hole.hasConnector(1, 6) || hole.isPair() || hole.isInHand(Card.ACE))) {
                randomPct = random.nextInt(100) + 1;
                int position = context.getPosition(player);
                if ((position == 0 && randomPct <= 15) || // Early
                        (position == 1 && randomPct <= 35) || // Middle
                        (position >= 2 && randomPct <= 60) || // Late/Button
                        (position >= 4 && randomPct <= 80)) { // Blinds
                    return PlayerAction.call();
                }
            }
        }

        // Final pot odds check before folding (matches V1Player line 1201-1203)
        Card[] holeCards = context.getHoleCards(player);
        Card[] communityCards = getCommunityCards(context);
        if (holeCards != null && communityCards != null && amountToCall > 0) {
            double improveOdds = context.calculateImprovementOdds(holeCards, communityCards);
            int potSize = context.getPotSize();
            float potOdds = (float) amountToCall / (potSize + amountToCall);
            if (improveOdds >= potOdds) {
                return PlayerAction.call();
            }
        }

        // Fold
        return PlayerAction.fold();
    }

    // ========== State Management ==========

    /**
     * Reset state for new hand and track round changes.
     */
    private void resetHandState(AIContext context) {
        GameHand currentHand = context.getCurrentHand();
        int handId = currentHand != null ? System.identityHashCode(currentHand) : -1;

        // New hand - reset all state
        if (handId != currentHandId) {
            currentHandId = handId;
            checkRaiseIntent = false;
            lastRoundBetAmount = 0;
            lastRoundPotSize = 0;
            lastBettingRound = -1;
            lastAction = null;

            // Reset limper detection state (B10)
            raiserPreAction = AIContext.ACTION_NONE;
            raiserFlopAction = AIContext.ACTION_NONE;
            raiserTurnAction = AIContext.ACTION_NONE;
            bettorPreAction = AIContext.ACTION_NONE;
            bettorFlopAction = AIContext.ACTION_NONE;
            bettorTurnAction = AIContext.ACTION_NONE;
        }

        // Track round changes to calculate last round betting
        int currentRound = context.getBettingRound();
        if (currentRound != lastBettingRound && lastBettingRound >= 0) {
            // New round started - calculate betting from last round
            int currentPot = context.getPotSize();
            lastRoundBetAmount = currentPot - lastRoundPotSize;
            lastRoundPotSize = currentPot;

            // Reset action tracking for new round
            lastAction = null;
        }
        lastBettingRound = currentRound;
    }

    /**
     * Update opponent statistics based on observed action.
     */
    /**
     * Get opponent's raise frequency (0.0 to 1.0).
     * <p>
     * Queries AIContext instead of tracking stats locally (matches V1Player line
     * 1479).
     */
    private float getOpponentRaiseFrequency(GamePlayerInfo opponent, AIContext context) {
        if (opponent == null) {
            return 0.5f; // Default assumption
        }
        // Query from context (matches V1Player line 1479)
        int freq = context.getOpponentRaiseFrequency(opponent, context.getBettingRound());
        return freq / 100.0f; // Convert to 0.0-1.0
    }

    /**
     * Get opponent's bet frequency (0.0 to 1.0).
     */
    private float getOpponentBetFrequency(GamePlayerInfo opponent, AIContext context) {
        if (opponent == null) {
            return 0.5f; // Default assumption
        }
        // Query from context (matches V1Player line 1488)
        int freq = context.getOpponentBetFrequency(opponent, context.getBettingRound());
        return freq / 100.0f; // Convert to 0.0-1.0
    }

    // ========== Helper Methods for Hand Analysis ==========

    /**
     * Get the highest rank from a set of cards.
     */
    private int getHighestRank(Card[] cards) {
        if (cards == null || cards.length == 0) {
            return 0;
        }
        int highestRank = 0;
        for (Card card : cards) {
            if (card != null && card.getRank() > highestRank) {
                highestRank = card.getRank();
            }
        }
        return highestRank;
    }

    /**
     * Check if hole cards are a pocket pair.
     */
    private boolean isPocketPair(Card[] holeCards) {
        return holeCards != null && holeCards.length == 2 && holeCards[0].getRank() == holeCards[1].getRank();
    }

    /**
     * Check if trips are the top rank on the board. Used to determine strength of
     * trips - top trips are stronger.
     */
    private boolean isTopTrip(Card[] holeCards, Card[] communityCards, AIContext context) {
        int[] best = context.getBest5CardRanks(holeCards, communityCards);
        int highestCommunityRank = getHighestRank(communityCards);
        return best[0] == highestCommunityRank;
    }

    /**
     * Check if pocket pair is an overpair (higher than all community cards).
     */
    private boolean isOverpair(Card[] holeCards, Card[] communityCards) {
        if (!isPocketPair(holeCards)) {
            return false;
        }
        int pairRank = holeCards[0].getRank();
        int highestCommunityRank = getHighestRank(communityCards);
        return pairRank > highestCommunityRank;
    }

    /**
     * Check if pair matches the top card on the board (top pair).
     */
    private boolean isTopPair(Card[] holeCards, Card[] communityCards, AIContext context) {
        int[] best = context.getBest5CardRanks(holeCards, communityCards);
        int highestCommunityRank = getHighestRank(communityCards);
        return best[0] == highestCommunityRank && hasCard(holeCards, highestCommunityRank);
    }

    /**
     * Check if kicker is good (Ace or King and in our hole cards). Based on hand
     * type (pair, trips, two pair).
     */
    private boolean isGoodKicker(int handRank, Card[] holeCards, AIContext context, Card[] communityCards) {
        int[] best = context.getBest5CardRanks(holeCards, communityCards);

        // For pairs: kicker is the highest non-pair card
        // For trips: kicker is the highest non-trip card
        // For two pair: no kicker in best 5 (pair-pair-kicker)

        // Simplified: Check if Ace or King is in our hole cards and in the best hand
        boolean hasAceKicker = hasCard(holeCards, Card.ACE)
                && (best[1] == Card.ACE || best[2] == Card.ACE || best[3] == Card.ACE || best[4] == Card.ACE);
        boolean hasKingKicker = hasCard(holeCards, Card.KING)
                && (best[1] == Card.KING || best[2] == Card.KING || best[3] == Card.KING || best[4] == Card.KING);

        return hasAceKicker || hasKingKicker;
    }
}
