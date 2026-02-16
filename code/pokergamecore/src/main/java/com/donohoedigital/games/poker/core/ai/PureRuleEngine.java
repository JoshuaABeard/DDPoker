/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * Copyright (c) 2026 Joshua Beard and contributors
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

import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.PokerConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pure rule-based poker AI engine extracted from RuleEngine. Evaluates 15
 * possible outcomes (fold, check, limp, steal, call, raise, etc.) using 46
 * decision factors. Swing-free version for use in pokergamecore.
 */
@SuppressWarnings({"DuplicatedCode", "CommentedOutCode"})
public class PureRuleEngine implements AIConstants {

    private static final ArrayList<String> outcomeNames_ = new ArrayList<>();
    private static final ArrayList<String> factorNames_ = new ArrayList<>();
    private static final ArrayList<String> curveNames_ = new ArrayList<>();

    public static final int OUTCOME_NONE = -1;
    public static final int OUTCOME_FOLD = 0;
    public static final int OUTCOME_CHECK = 1;
    public static final int OUTCOME_LIMP = 2;
    public static final int OUTCOME_STEAL = 3;
    public static final int OUTCOME_OPEN_POT = 4;
    public static final int OUTCOME_CALL = 5;
    public static final int OUTCOME_RAISE = 6;
    public static final int OUTCOME_SEMI_BLUFF = 7;
    public static final int OUTCOME_TRAP = 8;
    public static final int OUTCOME_SLOW_PLAY = 9;
    public static final int OUTCOME_CHECK_RAISE = 10;
    public static final int OUTCOME_BET = 11;
    public static final int OUTCOME_ALL_IN = 12;
    public static final int OUTCOME_CONTINUATION_BET = 13;
    public static final int OUTCOME_BLUFF = 14;

    public static final int FACTOR_DEFAULT = 0;
    public static final int FACTOR_HAND_SELECTION = 1;
    public static final int FACTOR_BLIND_STEALING = 2;
    public static final int FACTOR_LEFT_TO_ACT = 3;
    public static final int FACTOR_POSITION = 8;
    public static final int FACTOR_STACK_SIZE = 9;
    public static final int FACTOR_POT_ODDS = 11;
    public static final int FACTOR_RAW_HAND_STRENGTH = 12;
    public static final int FACTOR_BIASED_HAND_STRENGTH = 13;
    public static final int FACTOR_HAND_POTENTIAL = 14;
    public static final int FACTOR_BET_TO_CALL = 15;
    public static final int FACTOR_AGGRESSION = 18;
    public static final int FACTOR_STEAL_SUSPECTED = 20;
    public static final int FACTOR_STRAIGHT_DRAW = 22;
    public static final int FACTOR_FLUSH_DRAW = 23;
    public static final int FACTOR_PROBE_BET = 25;
    public static final int FACTOR_PLAYERS_LEFT = 26;
    public static final int FACTOR_FIRST_PRE_FLOP_RAISER = 27;
    public static final int FACTOR_LAST_PRE_FLOP_RAISER = 28;
    public static final int FACTOR_ONLY_PRE_FLOP_RAISER = 29;
    public static final int FACTOR_PRE_FLOP_POSITION = 30;
    public static final int FACTOR_RAISER_STACK_SIZE = 31;
    public static final int FACTOR_IMPLIED_ODDS = 32;
    public static final int FACTOR_RAISER_POSITION = 33;
    public static final int FACTOR_RERAISER_POSITION = 34;
    public static final int FACTOR_BOREDOM = 35;
    public static final int FACTOR_STEAM = 36;
    public static final int FACTOR_FIRST_ACTION = 37;
    public static final int FACTOR_OUTDRAW_RISK = 38;
    public static final int FACTOR_CHECKED_AROUND = 39;
    public static final int FACTOR_BLINDS_CLOSING = 40;
    public static final int FACTOR_OPPONENT_BET_FREQUENCY = 41;
    public static final int FACTOR_OPPONENT_RAISE_FREQUENCY = 42;
    public static final int FACTOR_OPPONENT_OVERBET_FREQUENCY = 43;
    public static final int FACTOR_OPPONENT_BET_FOLD_FREQUENCY = 44;
    public static final int FACTOR_STEAL_POTENTIAL = 45;
    public static final int CURVE_LINEAR = 1;
    public static final int CURVE_SQUARE = 2;
    public static final int CURVE_CUBE = 3;

    private V2AIContext context_;
    private V2PlayerState state_;
    private Consumer<String> debug_;
    private float[] score_;
    private boolean[] eligible_;
    private float[] weights_;
    private OutcomeAdjustment[][] adjustments_;

    private int strongestOutcome_;

    private BetRange betRange_ = null;

    private boolean probeBet_;
    private int nPlayersRemaining_;
    private int nPlayersAfter_;
    private int nAmountToCall_;
    private int nLastAction_;
    private boolean bInPosition_;
    private int round_;
    private GamePlayerInfo self_;
    private int seat_;
    private boolean bAllIn_;
    private boolean bPotCommitted_;
    private boolean bCardsToCome_;
    private float apparentStrength_;
    private float probableStrength_;
    private float drawStrength_;
    private V2OpponentModel selfModel_;

    AIOutcome outcome_ = null;

    static {
        outcomeNames_.add("fold");
        outcomeNames_.add("check");
        outcomeNames_.add("limp");
        outcomeNames_.add("steal");
        outcomeNames_.add("openpot");
        outcomeNames_.add("call");
        outcomeNames_.add("raisevalue");
        outcomeNames_.add("semibluff");
        outcomeNames_.add("trap");
        outcomeNames_.add("slowplay");
        outcomeNames_.add("checkraise");
        outcomeNames_.add("bet");
        outcomeNames_.add("allin");
        outcomeNames_.add("continuationbet");
        outcomeNames_.add("bluff");

        factorNames_.add("default");
        factorNames_.add("handselection");
        factorNames_.add("blindstealing");
        factorNames_.add("lefttoact");
        factorNames_.add("blindssmall");
        factorNames_.add("bbshortstack");
        factorNames_.add("potraised");
        factorNames_.add("earlyraiser");
        factorNames_.add("position");
        factorNames_.add("stacksize");
        factorNames_.add("limptendency");
        factorNames_.add("potodds");
        factorNames_.add("rawhandstrength");
        factorNames_.add("biasedhandstrength");
        factorNames_.add("handpotential");
        factorNames_.add("bettocall");
        factorNames_.add("potreraised");
        factorNames_.add("tightness");
        factorNames_.add("aggression");
        factorNames_.add("tableaggression");
        factorNames_.add("stealsuspected");
        factorNames_.add("lasttoact");
        factorNames_.add("straightdraw");
        factorNames_.add("flushdraw");
        factorNames_.add("playersdealt");
        factorNames_.add("probebet");
        factorNames_.add("playersleft");
        factorNames_.add("firstpreflopraiser");
        factorNames_.add("lastpreflopraiser");
        factorNames_.add("onlypreflopraiser");
        factorNames_.add("preflopposition");
        factorNames_.add("raiserstacksize");
        factorNames_.add("impliedodds");
        factorNames_.add("raiserposition");
        factorNames_.add("reraiserposition");
        factorNames_.add("boredom");
        factorNames_.add("steam");
        factorNames_.add("firstaction");
        factorNames_.add("outdrawrisk");
        factorNames_.add("checkedaround");
        factorNames_.add("blindsclosing");
        factorNames_.add("opponentbetfrequency");
        factorNames_.add("opponentraisefrequency");
        factorNames_.add("opponentoverbetfrequency");
        factorNames_.add("opponentbetfoldfrequency");
        factorNames_.add("stealpotential");

        curveNames_.add("none");
        curveNames_.add("linear");
        curveNames_.add("square");
        curveNames_.add("cube");
    }

    public void init(V2AIContext context, V2PlayerState state, GamePlayerInfo player, Consumer<String> debug) {
        for (int i = outcomeNames_.size() - 1; i >= 0; --i) {
            score_[i] = 0.0f;
            weights_[i] = 0.0f;
            eligible_[i] = false;
            for (int j = factorNames_.size() - 1; j >= 0; --j) {
                adjustments_[i][j] = null;
            }
        }

        probeBet_ = false;

        context_ = context;
        state_ = state;
        self_ = player;
        debug_ = debug;
        seat_ = context.getSeat(player);

        if (context.getCurrentHand() != null) {
            round_ = context.getBettingRound();

            nPlayersRemaining_ = context.getNumPlayersWithCards();
            nPlayersAfter_ = context.getNumPlayersYetToAct(player);
            nAmountToCall_ = context.getAmountToCall(player);
            nLastAction_ = context.getLastActionThisRound(player);

            bInPosition_ = (nPlayersAfter_ == 0);
            bAllIn_ = (player.getChipCount() == 0);
            bPotCommitted_ = (player.getChipCount() <= context.getChipCountAtStart(player) / 2);

            bCardsToCome_ = (round_ != BettingRound.RIVER.toLegacy());

            selfModel_ = context.getSelfModel();

            if (round_ >= BettingRound.FLOP.toLegacy()) {
                Hand community = context.getCommunity();
                apparentStrength_ = context.getApparentStrength(seat_, community);
                probableStrength_ = context.getBiasedRawHandStrength(seat_, community);
                drawStrength_ = (round_ == BettingRound.FLOP.toLegacy() || round_ == BettingRound.TURN.toLegacy())
                        ? context.getBiasedEffectiveHandStrength(seat_, community)
                        : probableStrength_;
            }
        }

        outcome_ = null;

        // default bet range to prevent strange bug seen recently
        betRange_ = BetRange.bigBlindRelative(2.0f, 4.0f);
    }

    public void setEligible(int outcome, boolean eligible) {
        eligible_[outcome] = eligible;
    }

    public boolean isEligible(int outcome) {
        return eligible_[outcome];
    }

    public PureRuleEngine() {
        score_ = new float[outcomeNames_.size()];
        weights_ = new float[outcomeNames_.size()];
        eligible_ = new boolean[outcomeNames_.size()];
        adjustments_ = new OutcomeAdjustment[outcomeNames_.size()][factorNames_.size()];
    }

    public void execute(V2AIContext context, V2PlayerState state, GamePlayerInfo player, Consumer<String> debug) {
        init(context, state, player, debug);

        strongestOutcome_ = eligible_[OUTCOME_CHECK] ? OUTCOME_CHECK : OUTCOME_FOLD;

        switch (round_) {
            case -1 : // BettingRound.NONE.toLegacy()
                return;
            case 0 : // BettingRound.PRE_FLOP.toLegacy()
                determineEligibleOutcomes();
                executePreFlop();
                setPreFlopBetRange();
                break;
            case 1 : // BettingRound.FLOP.toLegacy()
            case 2 : // BettingRound.TURN.toLegacy()
            case 3 : // BettingRound.RIVER.toLegacy()
                execPostFlop();
                break;
        }
    }

    private void execPostFlop() {
        // for now limit to heads up play where opponent has bet

        if (nPlayersRemaining_ != 2) {
            executeFlopTurn();
            return;
        }

        List<GamePlayerInfo> players = context_.getPlayersLeft(self_);

        GamePlayerInfo opponent = players.get(0);

        int opponentSeat = context_.getSeat(opponent);

        V2OpponentModel opponentModel = context_.getOpponentModel(opponent);

        boolean bOpponentAllIn = (opponent.getChipCount() == 0);
        boolean bOpponentPotCommitted = (opponent.getChipCount() <= context_.getChipCountAtStart(opponent) / 2);

        float opponentProbableStrength = context_.getBiasedRawHandStrength(opponentSeat, context_.getCommunity());

        int potTotal = context_.getTotalPotChipCount();

        if (bOpponentPotCommitted) {
            potTotal += Math.min(opponent.getChipCount(), self_.getChipCount() - nAmountToCall_);

            debug_.accept("Expected pot total: " + potTotal + "<br>");
        } else {
            debug_.accept("Pot total: " + potTotal + "<br>");
        }

        float potOdds = (float) potTotal / (float) nAmountToCall_;
        float breakEvenPercent = 1.0f / (potOdds + 1);

        if (nAmountToCall_ > 0) {
            debug_.accept("Pot odds " + PokerConstants.formatPercent(potOdds) + " to 1.<br>");
            debug_.accept("Break even percent: " + PokerConstants.formatPercent(breakEvenPercent * 100) + "%<br>");
        }

        debug_.accept("Probable Strength: " + PokerConstants.formatPercent(probableStrength_ * 100) + "%<br>");
        debug_.accept("Draw Strength: " + PokerConstants.formatPercent(drawStrength_ * 100) + "%<br>");

        AIOutcome outcome = new AIOutcome(context_.getPotStatus(), round_, context_.isLimit());

        debug_.accept("Heads up");

        if (bInPosition_) {
            debug_.accept(" in position");

            if (context_.hasActedThisRound(self_)) {
                if (nLastAction_ == AIContext.ACTION_BET) {
                    debug_.accept(", opponent has check-raised");
                } else {
                    debug_.accept(", opponent has re-raised");
                }

                if (bOpponentAllIn) {
                    debug_.accept(" all-in");
                }

                // raise with very likely winners
                if (!bOpponentAllIn && (probableStrength_ > 0.85f)) {
                    setEligible(OUTCOME_RAISE, true);
                    outcome.addTuple(AIOutcome.RAISE, "Very Likely Best Hand", 0f, 1f - probableStrength_,
                            probableStrength_);
                } else if (breakEvenPercent <= drawStrength_) {
                    if (probableStrength_ < .4f) {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL, "Pot Odds / Likely Worst Hand", 0f, 1f, 0f);
                    } else if (probableStrength_ > .6f) {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL, "Pot Odds / Likely Best Hand", 0f, 1f, 0f);
                    } else {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL, "Pot Odds", 0f, 1f, 0f);
                    }
                } else {
                    setEligible(OUTCOME_FOLD, true);
                    outcome.addTuple(AIOutcome.FOLD, "Pot Odds", 1f, 0f, 0f);
                }
            } else {
                if (nAmountToCall_ == 0) {
                    debug_.accept(", opponent has checked");

                    // no point continuation betting against a pot committed opponent,
                    // or if we don't have at least half the pot to bet
                    if (!bOpponentPotCommitted && (self_.getChipCount() > context_.getTotalPotChipCount() / 2)
                            && (round_ == BettingRound.FLOP.toLegacy()) && context_.wasLastRaiserPreFlop(self_)
                            && (apparentStrength_ > 0.5f)) {
                        setEligible(OUTCOME_CONTINUATION_BET, true);
                        outcome.addTuple(AIOutcome.BET, "Continuation Bet", .35f, 0f, .65f);
                    }

                    if (probableStrength_ > 0.5f) {
                        setEligible(OUTCOME_BET, true);
                        outcome.addTuple(AIOutcome.BET, "Likely Best Hand", .25f, 0f, .75f);
                    } else {
                        setEligible(OUTCOME_CHECK, true);
                        outcome.addTuple(AIOutcome.CHECK, "Likely Worst Hand", 1f, 0f, 0f);
                        // no point bluffing against a pot committed opponent,
                        // or if we don't have at least half the pot to bet
                        if (!isEligible(OUTCOME_CONTINUATION_BET) && !bOpponentPotCommitted
                                && (self_.getChipCount() > context_.getTotalPotChipCount() / 2)) {
                            setEligible(OUTCOME_BLUFF, true);
                            outcome.addTuple(AIOutcome.BET, "Bluff", .85f, 0f, .15f);
                        }
                    }

                    if (self_.getChipCount() >= context_.getTotalPotChipCount()) {
                        if (drawStrength_ > 0.85f) {
                            setEligible(OUTCOME_TRAP, true);
                            outcome.addTuple(AIOutcome.BET, "Trap", 0f, 0f, 1f);
                        }
                    }

                    if (drawStrength_ > 0.95f) {
                        setEligible(OUTCOME_SLOW_PLAY, true);
                        outcome.addTuple(AIOutcome.CHECK, "Slow-Play", .20f, .4f, .4f);
                    }
                } else

                {
                    debug_.accept(", opponent has bet");

                    if (bOpponentAllIn) {
                        debug_.accept(" all-in");
                    }

                    debug_.accept(".<br>");

                    // raise with very likely winners
                    if (!bOpponentAllIn && (probableStrength_ > 0.85f)) {
                        setEligible(OUTCOME_RAISE, true);
                        outcome.addTuple(AIOutcome.RAISE, "Very Likely Best Hand", 0f, 1f - probableStrength_,
                                probableStrength_);
                    } else if (breakEvenPercent <= drawStrength_) {
                        if (probableStrength_ < .4f) {
                            setEligible(OUTCOME_CALL, true);
                            outcome.addTuple(AIOutcome.CALL, "Pot Odds / Likely Worst Hand", 0f, 1f, 0f);
                        } else if (probableStrength_ > .6f) {
                            setEligible(OUTCOME_CALL, true);
                            outcome.addTuple(AIOutcome.CALL, "Pot Odds / Likely Best Hand", 0f, 1f, 0f);
                        } else {
                            setEligible(OUTCOME_CALL, true);
                            outcome.addTuple(AIOutcome.CALL, "Pot Odds", 0f, 1f, 0f);
                        }
                    } else {
                        setEligible(OUTCOME_FOLD, true);
                        outcome.addTuple(AIOutcome.FOLD, "Pot Odds", 1f, 0f, 0f);
                    }
                }
            }
        } else {
            debug_.accept(" out of position");

            if (context_.hasActedThisRound(self_)) {
                switch (nLastAction_) {
                    case AIContext.ACTION_CHECK :
                        debug_.accept(", opponent has bet");
                        break;
                    case AIContext.ACTION_BET :
                        debug_.accept(", opponent has raised");
                        break;
                    case AIContext.ACTION_RAISE :
                        debug_.accept("opponent has re-raised");
                        break;
                }

                if (bOpponentAllIn) {
                    debug_.accept(" all-in");
                }

                // raise with very likely winners
                if (!bOpponentAllIn && (probableStrength_ > 0.85f)) {
                    setEligible(OUTCOME_RAISE, true);
                    outcome.addTuple(AIOutcome.RAISE, "Very Likely Best Hand", 0f, 1f - probableStrength_,
                            probableStrength_);
                } else if (breakEvenPercent <= drawStrength_) {
                    if (probableStrength_ < .4f) {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL, "Pot Odds / Likely Worst Hand", 0f, 1f, 0f);
                    } else if (probableStrength_ > .6f) {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL, "Pot Odds / Likely Best Hand", 0f, 1f, 0f);
                    } else {
                        setEligible(OUTCOME_CALL, true);
                        outcome.addTuple(AIOutcome.CALL, "Pot Odds", 0f, 1f, 0f);
                    }
                } else {
                    setEligible(OUTCOME_FOLD, true);
                    outcome.addTuple(AIOutcome.FOLD, "Pot Odds", 1f, 0f, 0f);
                }
            } else {
                debug_.accept(", first to act");

                // no point continuation betting against a pot committed opponent,
                // or if we don't have at least half the pot to bet
                if (!bOpponentPotCommitted && (self_.getChipCount() > context_.getTotalPotChipCount() / 2)
                        && (round_ == BettingRound.FLOP.toLegacy()) && context_.wasLastRaiserPreFlop(self_)
                        && (apparentStrength_ > 0.5f)) {
                    setEligible(OUTCOME_CONTINUATION_BET, true);
                    outcome.addTuple(AIOutcome.BET, "Continuation Bet", .35f, 0f, .65f);
                }

                if (probableStrength_ > 0.5f) {
                    setEligible(OUTCOME_BET, true);
                    outcome.addTuple(AIOutcome.BET, "Likely Best Hand", .2f, 0f, .8f);

                    if (drawStrength_ > 0.75f) {
                        // no point in check-raise or trap if we have less than a pot worth of chips
                        if (self_.getChipCount() >= context_.getTotalPotChipCount()) {
                            setEligible(OUTCOME_CHECK_RAISE, true);
                            outcome.addTuple(AIOutcome.CHECK, "Check-Raise", .2f, 0f, .8f);

                            if (drawStrength_ > 0.85f) {
                                setEligible(OUTCOME_TRAP, true);
                                outcome.addTuple(AIOutcome.BET, "Trap", 0f, 0f, 1f);
                            }
                        }

                        if (drawStrength_ > 0.95f) {
                            setEligible(OUTCOME_SLOW_PLAY, true);
                            outcome.addTuple(AIOutcome.CHECK, "Slow-Play", .2f, 0f, .8f);
                        }
                    }
                } else {
                    if (probableStrength_ < 0.5f) {
                        setEligible(OUTCOME_CHECK, true);
                        outcome.addTuple(AIOutcome.CHECK, "Likely Worst Hand", 1f, 0f, 0f);
                    }

                    // no point bluffing against a pot committed opponent,
                    // or if we don't have at least half the pot to bet
                    if (!bOpponentPotCommitted && (self_.getChipCount() > context_.getTotalPotChipCount() / 2)) {
                        if (bCardsToCome_ && (drawStrength_ > 0.5f)) {
                            setEligible(OUTCOME_SEMI_BLUFF, true);
                            outcome.addTuple(AIOutcome.BET, "Semi-Bluff", .5f, 0f, .5f);
                        } else {
                            setEligible(OUTCOME_BLUFF, true);
                            outcome.addTuple(AIOutcome.BET, "Bluff", .85f, 0f, .15f);
                        }
                    }
                }
            }
        }

        debug_.accept(".<br>");

        if (!bOpponentAllIn && bOpponentPotCommitted) {
            debug_.accept("Opponent is pot-committed.");
        }

        float amountNumerator = 0f;
        int amountDivisor = 0;

        if (probableStrength_ > 0.85f) {
            debug_.accept("Very likely best hand.<br>");
            amountNumerator += 1 / 3f;
            ++amountDivisor;
        } else if (probableStrength_ < 0.30f) {
            debug_.accept("Very likely worst hand.<br>");
            amountNumerator += 1f;
            ++amountDivisor;
        } else {
            debug_.accept("Moderately strong hand.<br>");
            amountNumerator += 2 / 3f;
            ++amountDivisor;
        }

        float chanceToImprove = drawStrength_ - probableStrength_;

        if (chanceToImprove > 1 / 3f) {
            debug_.accept("Very likely to improve.<br>");
            amountNumerator += 1 / 2f;
            ++amountDivisor;
        } else if (chanceToImprove > 0.15f) {
            debug_.accept("Primary draw.<br>");
            amountNumerator += 2 / 3f;
            ++amountDivisor;
        } else if ((probableStrength_ > 0.5f) && (chanceToImprove < -0.10)) {
            debug_.accept("Outdraw risk.<br>");
            amountNumerator += 1f;
            ++amountDivisor;
        }

        float amountRatio = amountNumerator / amountDivisor;

        int betAmount = (int) (amountRatio * context_.getTotalPotChipCount());

        debug_.accept("Bet amount: " + betAmount + " (" + PokerConstants.formatPercent(amountRatio * 100)
                + "% of the pot).<br>");

        betRange_ = BetRange.allIn();

        String allInReason = null;

        if (bPotCommitted_) {
            allInReason = "Pot Committed";
        } else if (bOpponentPotCommitted) {
            allInReason = "Opponent is Pot Committed";
        } else if ((self_.getChipCount() - betAmount) <= context_.getChipCountAtStart(self_) / 2) {
            allInReason = "Reasonable Bet Will Commit Me to the Pot";
        } else if ((opponent.getChipCount() - betAmount) <= context_.getChipCountAtStart(opponent) / 2) {
            allInReason = "Call of Reasonable Bet Will Commit Opponent to the Pot";
        } else {
            betRange_ = BetRange.potRelative(amountRatio, amountRatio);
        }

        debug_.accept("<br>");
        debug_.accept(outcome.toHTML());

        strongestOutcome_ = outcome.selectOutcome(context_.getPotStatus());

        Hand community = context_.getCommunity();
        Hand pocket = context_.getPocketCards(self_);

        int startingPosition = context_.getStartingPositionCategory(self_);

        int numWithCards = context_.getNumPlayersWithCards();
        int amountToCall = context_.getAmountToCall(self_);
        int playerChips = self_.getChipCount();

        float xBasicsPosition = context_.getStrategy().getStratFactor("basics.position", 0.0f, 2.0f);
        float xBasicsPotOdds = context_.getStrategy().getStratFactor("basics.pot_odds_call", 0.0f, 2.0f);
        float xBasicsAggression = context_.getStrategy().getStratFactor("basics.aggression", -1.0f, +1.0f);
        float xBasicsObservation = context_.getStrategy().getStratFactor("basics.observation", 0.0f, 2.0f);
        float xTilt = context_.getStrategy().getStratFactor("discipline.tilt", 0.0f, 1.0f);

        float breakEvenPercentage = 1.0f / (potOdds + 1);
        float rhs;
        float bhs;
        float ehs;
        float drawPotential;
        float outdrawRisk;

        PocketRanks ranks = PocketRanks.getInstance(community);

        rhs = ranks.getRawHandStrength(pocket);

        bhs = context_.getBiasedRawHandStrength(seat_, community);

        if (round_ < BettingRound.RIVER.toLegacy()) {
            ehs = context_.getBiasedEffectiveHandStrength(seat_, community);
        } else {
            ehs = bhs;
        }

        drawPotential = ehs - bhs;
        outdrawRisk = 0.0f;

        if (drawPotential < 0.0f) {
            outdrawRisk = -drawPotential;
            drawPotential = 0.0f;
        }

        PureHandPotential potential = new PureHandPotential(pocket, community);

        int pNutFlush = potential.getHandCount(PureHandPotential.NUT_FLUSH, 0);
        int pNonNutFlush = potential.getHandCount(PureHandPotential.FLUSH, 0) - pNutFlush;
        int pNutStraight = potential.getHandCount(PureHandPotential.NUT_STRAIGHT, 0);
        int pNonNutStraight = potential.getHandCount(PureHandPotential.NON_NUT_STRAIGHT, 0);

        float xStraightDraw = context_.getStrategy().getStratFactor("draws.straight.nut", -1.0f, 1.0f) * pNutStraight
                + context_.getStrategy().getStratFactor("draws.straight.non_nut", -1.0f, 1.0f) * 0.5f * pNonNutStraight;

        float xFlushDraw = context_.getStrategy().getStratFactor("draws.flush.nut", -1.0f, 1.0f) * pNutFlush
                + context_.getStrategy().getStratFactor("draws.flush.non_nut", -1.0f, 1.0f) * 0.5f * pNonNutFlush;

        if (community.hasQuads() || (community.hasTrips() && community.hasPair())) {
            xStraightDraw *= 0.0f;
            xFlushDraw *= 0.0f;
        } else if (community.hasPossibleFlush()) {
            xFlushDraw *= 0.75d;

            if (community.hasFlush()) {
                xStraightDraw = 0.0f;
            } else {
                xStraightDraw *= 0.5f;
            }
        }

        int roundsNoAction = 0;

        switch (round_) {
            case 3 : // BettingRound.RIVER.toLegacy()
                if (!context_.wasPotAction(BettingRound.TURN.toLegacy())) {
                    ++roundsNoAction;
                } else {
                    break;
                }
            case 2 : // BettingRound.TURN.toLegacy()
                if (!context_.wasPotAction(BettingRound.FLOP.toLegacy())) {
                    ++roundsNoAction;
                }
                break;
        }

        int handsToBB = context_.getHandsBeforeBigBlind(self_);

        if (state_.debugEnabled()) {
            debug_.accept("Raw Tilt Factor: " + xTilt + "<br>");
            debug_.accept("Raw Hand Strength: " + rhs + "<br>");
            debug_.accept("Biased Hand Strength: " + bhs + "<br>");
            debug_.accept("Effective Hand Strength: " + ehs + "<br>");
            debug_.accept("Break Even Percentage: " + breakEvenPercentage + "<br>");
            debug_.accept("Draw Potential (bppot): " + drawPotential + "<br>");
            debug_.accept("Outdraw Risk (bnpot): " + outdrawRisk + "<br>");
            debug_.accept("Outdraw Risk: " + outdrawRisk + "<br>");
            debug_.accept("Pot Total: " + potTotal + "<br>");
            debug_.accept("Amount To Call: " + amountToCall + "<br>");
            debug_.accept("Pot Odds: " + potOdds + "<br>");
            debug_.accept("Pot Odds Factor: " + xBasicsPotOdds + "<br>");
            debug_.accept("Straight Draw Factor: " + xStraightDraw + "<br>");
            debug_.accept("Flush Draw Factor: " + xFlushDraw + "<br>");
            debug_.accept("Aggression Factor: " + xBasicsAggression + "<br>");
            debug_.accept("Preceding post-flop rounds w/ no action: " + roundsNoAction + "<br>");
            debug_.accept("Hands Before Big Blind: " + handsToBB + "<br>");
        }

        // default bet range 1/2 to whole pot
        betRange_ = BetRange.potRelative(0.5f, 1.0f);

        if ((round_ == BettingRound.RIVER.toLegacy()) && (context_.getPotStatus() != PokerConstants.NO_POT_ACTION)
                && (rhs == 1.0f)) {
            if (playerChips > amountToCall) {
                float allInPotRatio = (float) Math.ceil((float) (playerChips - amountToCall) / (float) potTotal);

                // *always* raise/reraise on the river with the pure nuts
                betRange_ = BetRange.potRelative((float) (Math.min(0.5f, allInPotRatio)), allInPotRatio);

                adjustOutcome(OUTCOME_RAISE, FACTOR_RAW_HAND_STRENGTH, 1.0f);
            } else {
                adjustOutcome(OUTCOME_CALL, FACTOR_RAW_HAND_STRENGTH, 1.0f);
            }

            return;
        }

        int potStatus = context_.getPotStatus();

        if (potStatus == PokerConstants.NO_POT_ACTION) {
            // initialize default action
            adjustOutcome(OUTCOME_CHECK, FACTOR_DEFAULT, 1.0f);

            float allWeak = 1.0f;

            // of remaining players, compute overall likelihood none has a calling hand
            for (int i = context_.getNumPlayersAtTable() - 1; i >= 0; --i) {
                GamePlayerInfo p = context_.getPlayerAt(i);
                if (context_.getSeat(p) == seat_)
                    continue;
                if (p.isFolded())
                    continue;
                if (context_.getLastActionThisRound(p) == AIContext.ACTION_CHECK)
                    continue;
                allWeak *= context_.getOpponentModel(p).getCheckFoldPostFlop(round_, 0.5f);
            }

            if (state_.debugEnabled()) {
                debug_.accept("All Weak Probability: " + allWeak + "<br>");
            }

            // CONSIDER WHETHER WE CAN OPEN THE BETTING FOR STRENGTH

            setEligible(OUTCOME_BET, true);

            // boost for first action ehs * 0.17 makes 85% hands the cutoff
            adjustOutcome(OUTCOME_BET, FACTOR_FIRST_ACTION, ehs * 0.17f);

            // base hand strength scores
            adjustOutcome(OUTCOME_BET, FACTOR_RAW_HAND_STRENGTH, rhs);
            adjustOutcome(OUTCOME_BET, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
            adjustOutcome(OUTCOME_BET, FACTOR_HAND_POTENTIAL, drawPotential);
            adjustOutcome(OUTCOME_BET, FACTOR_OUTDRAW_RISK, outdrawRisk);

            // aggression factor
            adjustOutcome(OUTCOME_BET, FACTOR_AGGRESSION, xBasicsAggression * 0.1f);

            // tilt
            if ((state_.getSteam() > 0.1) && (xTilt > 0)) {
                adjustOutcome(OUTCOME_BET, FACTOR_STEAM, state_.getSteam() * xTilt * 0.1f);
            }

            if ((getStrongestOutcome() == OUTCOME_BET) && (nPlayersAfter_ > 0)) {
                // CONSIDER CHECK-RAISE AND SLOW-PLAY

                setEligible(OUTCOME_CHECK_RAISE, true);

                // boost for players left to act
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_LEFT_TO_ACT,
                        0.06f * nPlayersAfter_ * xBasicsPosition * (1.0f - allWeak));

                // base hand strength scores
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_RAW_HAND_STRENGTH, rhs);
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_STEAL_POTENTIAL, xBasicsObservation * allWeak * -0.15f);

                // penalty for outdraw risk
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_OUTDRAW_RISK, -outdrawRisk);
            } else {
                // CONSIDER WHETHER WE SHOULD MAKE A CONTINUATION BET

                // consider continuation bet on the flop if last raiser pre-flop
                if (context_.wasLastRaiserPreFlop(self_) && (numWithCards < 4)
                        && (round_ == BettingRound.FLOP.toLegacy())) {
                    setEligible(OUTCOME_CONTINUATION_BET, true);

                    int draws = pNutFlush * 3 + pNonNutFlush + pNutStraight * 2 + pNonNutStraight;

                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_RAW_HAND_STRENGTH, rhs);
                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_HAND_POTENTIAL, drawPotential);
                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_OUTDRAW_RISK, outdrawRisk);
                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_FIRST_ACTION, ehs * 0.17f);

                    // stronger if first raiser pre-flop
                    if (context_.wasFirstRaiserPreFlop(self_)) {
                        // even stronger if raised in-between first and last raise
                        if (context_.wasOnlyRaiserPreFlop(self_)) {
                            adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_ONLY_PRE_FLOP_RAISER, 0.15f);
                        } else {
                            adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_FIRST_PRE_FLOP_RAISER, 0.17f);
                        }
                    } else {
                        adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_LAST_PRE_FLOP_RAISER, 0.05f);
                    }

                    if (context_.wasFirstRaiserPreFlop(self_)) {
                        // stronger the earlier position we opened from
                        switch (startingPosition) {
                            case POSITION_EARLY :
                                adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PRE_FLOP_POSITION, 0.1f);
                                break;
                            case POSITION_MIDDLE :
                                adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PRE_FLOP_POSITION, 0.05f);
                                break;
                        }
                    }

                    // weaker if two opponents
                    if (numWithCards == 3) {
                        adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PLAYERS_LEFT, -0.1f);
                    }

                    // stronger the larger our raises were?

                    if (strongestOutcome_ == OUTCOME_CONTINUATION_BET) {
                        betRange_ = BetRange.potRelative(0.4f, 0.7f);
                    }
                } else {
                    // consider probe bet if original raiser has checked
                    GamePlayerInfo firstRaiser = context_.getFirstBettor(BettingRound.PRE_FLOP.toLegacy(), true);

                    if (firstRaiser != null) {
                        int firstRaiserAction = context_.getFirstVoluntaryAction(firstRaiser, round_);

                        if (firstRaiserAction == AIContext.ACTION_CHECK) {
                            probeBet_ = true;

                            betRange_ = BetRange.potRelative(0.3f, 0.5f);

                            adjustOutcome(OUTCOME_BET, FACTOR_PROBE_BET, rhs * 0.1f);
                        }
                    }
                }
            }
        } else // pot action already
        {
            float xrhs;
            float xbhs;

            adjustOutcome(OUTCOME_FOLD, FACTOR_DEFAULT, 1.0f);

            GamePlayerInfo bettor = context_.getFirstBettor(round_, false);

            // CONSIDER WHETHER WE CAN CALL

            xrhs = (float) Math.sin((rhs - 0.5f) * Math.PI) * 0.5f + 0.50f;
            xbhs = (float) Math.sin((bhs - 0.5f) * Math.PI) * 0.5f + 0.50f;

            V2OpponentModel bettorModel = context_.getOpponentModel(bettor);

            float bettorActFrequency = bettorModel.getActPostFlop(round_, 0.1f);
            float bettorCheckFoldFrequency = bettorModel.getCheckFoldPostFlop(round_, 0.5f) * bettorActFrequency;
            float bettorOpenFrequency = bettorModel.getOpenPostFlop(round_, 0.5f) * bettorActFrequency;
            float bettorRaiseFrequency = bettorModel.getRaisePostFlop(round_, 0.5f) * bettorActFrequency;

            float bettorOverbetFrequency = bettorModel.getOverbetFrequency(0.5f);
            float bettorBetFoldFrequency = bettorModel.getBetFoldFrequency(0.5f);

            // penalty for players left to act
            adjustOutcome(OUTCOME_CALL, FACTOR_LEFT_TO_ACT, -0.08f * nPlayersAfter_ * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_CALL, FACTOR_RAW_HAND_STRENGTH, 0.85f - Math.abs(rhs - 0.85f));
            adjustOutcome(OUTCOME_CALL, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
            adjustOutcome(OUTCOME_CALL, FACTOR_OUTDRAW_RISK, -outdrawRisk);
            adjustOutcome(OUTCOME_CALL, FACTOR_STRAIGHT_DRAW, xStraightDraw * 0.075f);
            adjustOutcome(OUTCOME_CALL, FACTOR_FLUSH_DRAW, xFlushDraw * 0.05f);
            if (round_ < BettingRound.RIVER.toLegacy()) {
                // ehs/breakEvenPercentage = 1 at break-even
                // bhs = chance we currently have the best hand
                // ehs-bhs = additional chance the next card gives us to have the next hand
                adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, (1.0f - ehs) * ehs / breakEvenPercentage * xBasicsPotOdds);
                adjustOutcome(OUTCOME_CALL, FACTOR_HAND_POTENTIAL,
                        drawPotential * ehs / breakEvenPercentage * xBasicsPotOdds);
            } else {
                adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, (1.0f - bhs) * bhs / breakEvenPercentage * xBasicsPotOdds);
            }
            adjustOutcome(OUTCOME_CALL, FACTOR_BET_TO_CALL,
                    -0.15f * (float) Math.pow(2 * amountToCall / playerChips, 2) * (1.0f - ehs));
            adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_BET_FREQUENCY,
                    xBasicsObservation * bettorOpenFrequency * 0.20f);
            adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_OVERBET_FREQUENCY,
                    xBasicsObservation * bettorOverbetFrequency * 0.10f);

            // CONSIDER WHETHER WE CAN RAISE / RE-RAISE

            xrhs = (rhs + 0.05f) * (rhs + 0.05f);
            xbhs = (bhs + 0.05f) * (bhs + 0.05f);

            // penalty for players left to act
            adjustOutcome(OUTCOME_RAISE, FACTOR_LEFT_TO_ACT, -0.05f * nPlayersAfter_ * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_RAISE, FACTOR_RAW_HAND_STRENGTH, rhs);
            adjustOutcome(OUTCOME_RAISE, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OUTDRAW_RISK, outdrawRisk);
            adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression * 0.1f);
            if (round_ < BettingRound.RIVER.toLegacy()) {
                adjustOutcome(OUTCOME_RAISE, FACTOR_POT_ODDS,
                        (1.0f - ehs) * ehs / breakEvenPercentage * xBasicsPotOdds);
                adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_POTENTIAL, drawPotential);
            } else {
                adjustOutcome(OUTCOME_RAISE, FACTOR_POT_ODDS,
                        (1.0f - ehs) * ehs / breakEvenPercentage * xBasicsPotOdds / 2);
            }
            adjustOutcome(OUTCOME_RAISE, FACTOR_BET_TO_CALL,
                    -0.15f * (float) Math.pow(amountToCall / playerChips, 2) * (1.0f - ehs));
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_BET_FREQUENCY,
                    xBasicsObservation * bettorOpenFrequency * 0.20f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_OVERBET_FREQUENCY,
                    xBasicsObservation * bettorOverbetFrequency * 0.10f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_BET_FOLD_FREQUENCY,
                    xBasicsObservation * bettorBetFoldFrequency * 0.10f);
        }
    }

    private void determineEligibleOutcomes() {
        // Establish which outcomes are eligible in the current situation.

        boolean raised = context_.getPotStatus() >= PokerConstants.RAISED_POT;

        int amountToCall = context_.getAmountToCall(self_);
        int playerChips = self_.getChipCount();
        int totalPot = context_.getTotalPotChipCount();
        int minBet = context_.getMinRaise();
        int preFlopPosition = context_.getStartingPositionCategory(self_);

        setEligible(OUTCOME_FOLD, (amountToCall > 0));
        setEligible(OUTCOME_CHECK, (amountToCall == 0));

        if (round_ == BettingRound.PRE_FLOP.toLegacy()) {
            setEligible(OUTCOME_RAISE, (amountToCall >= 0) && (playerChips > amountToCall));

            if ((preFlopPosition == POSITION_BIG) || raised) {
                setEligible(OUTCOME_CALL, raised);
            }
        } else {
            setEligible(OUTCOME_CALL, (amountToCall > 0));
            setEligible(OUTCOME_RAISE, (amountToCall > 0) && (playerChips > amountToCall));
        }
    }

    private void executePreFlop() {
        float xTournamentStackSize = context_.getStrategy().getStratFactor("tournament.stack_size", 0.0f, 2.0f);
        float xTournamentOpponentStackSize = context_.getStrategy().getStratFactor("tournament.opponent_stack_size",
                0.0f, 2.0f);

        int potStatus = context_.getPotStatus();

        boolean hasActed = context_.hasActedThisRound(self_);

        int startingPosition = context_.getStartingPositionCategory(self_);

        int numWithCards = context_.getNumPlayersWithCards();

        int postFlopPosition = context_.getPostFlopPositionCategory(self_);

        Hand hand = context_.getPocketCards(self_);

        int numPlayers = context_.getNumPlayersWithCards();

        float handStrength = state_.getHandStrength();

        float xBasicsAggression = context_.getStrategy().getStratFactor("basics.aggression", -0.2f, +0.2f);
        float xBasicsBoldness = context_.getStrategy().getStratFactor("basics.boldness", -0.8f, -0.0f);
        float xBasicsPosition = context_.getStrategy().getStratFactor("basics.position", 0.0f, 2.0f);
        float xBasicsObservation = context_.getStrategy().getStratFactor("basics.observation", 0.0f, 2.0f);

        float xDisciplineLimp = getLimpFactor(hand);

        float dFoldExponent = 1.5f;
        float dBetExponent = 3.0f;

        int hohZone = context_.getHohZone(self_);

        int nLeftToAct = context_.getNumPlayersYetToAct(self_);

        float profileBasicsTightness = context_.getStrategy().getStratFactor("handselection", hand, 1.0f, 0.0f);

        float tableTightness = 0f;

        for (int i = 0; i < context_.getNumPlayersAtTable(); ++i) {
            GamePlayerInfo p = context_.getPlayerAt(i);

            if ((p != null) && (context_.getSeat(p) != seat_) && !p.isFolded()) {
                tableTightness += context_.getOpponentModel(p)
                        .getPreFlopTightness(context_.getStartingPositionCategory(p), .5f);
            }
        }

        tableTightness /= ((float) numWithCards - 1f);

        float xBasicsTightness = Math.min(Math.max(
                profileBasicsTightness
                        * (xBasicsObservation * ((1f + (2f - (tableTightness * 2f))) / 2) + (1f - xBasicsObservation)),
                0f), 1f);

        float xBasicsPotOdds = context_.getStrategy().getStratFactor("basics.pot_odds", 0.0f, 2.0f);
        float xTilt = context_.getStrategy().getStratFactor("discipline.tilt", 0.0f, 1.0f);
        float xStealBlinds = context_.getStrategy().getStratFactor("deception.steal_blinds", 1.0f, 0.0f);

        int tightnessBiasIndex = (int) Math
                .round((0.5f - ((xBasicsTightness > 0.5f) ? (xBasicsTightness - 0.5f) : xBasicsTightness)) / 0.05f);

        float tightnessBiasStrength = SimpleBias.getBiasValue(tightnessBiasIndex, hand);

        float foldStrengthDelta = 0.0f;

        if (((xBasicsTightness >= 0.5f) && (tightnessBiasStrength < handStrength))
                || ((xBasicsTightness < 0.5f) && (tightnessBiasStrength > handStrength))) {
            foldStrengthDelta = (tightnessBiasStrength - handStrength) * Math.abs(xBasicsTightness - 0.5f) * 2;
        }

        float tightnessAdjustment = ((float) Math.pow(1.0f - handStrength, dFoldExponent)
                - (float) Math.pow(1.0f - handStrength - foldStrengthDelta, dFoldExponent));

        float adjustedHandStrength = handStrength + foldStrengthDelta;

        int bigBlindAmount = context_.getBigBlind();
        int playerChips = self_.getChipCount();
        int amountToCall = context_.getAmountToCall(self_);
        int potTotal = context_.getTotalPotChipCount();
        float potOdds = amountToCall > 0 ? (float) potTotal / (float) amountToCall : 0;

        float xDisciplineBoredom = context_.getStrategy().getStratFactor("discipline.boredom", 1.0f, 0.0f);
        int consecutiveUnpaid = context_.getConsecutiveHandsUnpaid(self_);
        float bored = ((float) Math.min(consecutiveUnpaid, 10)) / 10.0f - xDisciplineBoredom;

        int handsToBB = context_.getHandsBeforeBigBlind(self_);

        if (state_.debugEnabled()) {
            debug_.accept("Hands Before Big Blind: " + handsToBB + "<br>");
            debug_.accept("Consecutive Hands Unpaid: " + consecutiveUnpaid + "<br>");
            debug_.accept("Boredom Factor: " + xDisciplineBoredom + "<br>");
            debug_.accept("Boredom Adjustment: " + bored + "<br>");
            debug_.accept("Steal Suspicion: " + state_.getStealSuspicion() + "<br>");
            debug_.accept("Effective M (Stack/Blinds): " + (int) context_.getHohM(self_) + "<br>");
            debug_.accept("Q (Stack/Average): " + (float) ((int) (context_.getHohQ(self_) * 100)) / 100.0f + "<br>");
            debug_.accept("Harrington Zone: " + context_.getHohZone(self_) + "<br>");
            debug_.accept("Basics Tightness (Profile): " + profileBasicsTightness + "<br>");
            debug_.accept("Table Tightness (Remaining Players): " + tableTightness + "<br>");
            debug_.accept("Basics Tightness (Adjusted): " + xBasicsTightness + "<br>");
            debug_.accept("Tightness Bias Index: " + tightnessBiasIndex + "<br>");
            debug_.accept("Hand Strength: " + handStrength + "<br>");
            debug_.accept("Tightness Bias Strength: " + tightnessBiasIndex + "<br>");
            debug_.accept("Fold Strength Delta: " + foldStrengthDelta + "<br>");
            debug_.accept("Tightness Adjustment: " + tightnessAdjustment + "<br>");
            debug_.accept("Adjusted Hand Strength: " + adjustedHandStrength + "<br>");
            debug_.accept("Steam: " + state_.getSteam() + "<br>");
        }

        adjustOutcome(OUTCOME_FOLD, FACTOR_DEFAULT, 1.0f);
        adjustOutcome(OUTCOME_CHECK, FACTOR_DEFAULT, 1.0f);

        if (!hasActed && (potStatus == PokerConstants.NO_POT_ACTION) && (amountToCall > 0)) {
            int startingOrder = context_.getStartingOrder(self_);

            float positionAdjustment = xBasicsPosition * (startingOrder + PokerConstants.SEATS - numPlayers)
                    / PokerConstants.SEATS;

            if (xBasicsPosition < 1f) {
                positionAdjustment += (1f - xBasicsPosition) * .8f;
            }

            debug_.accept("Start Order: " + (startingOrder + 1) + " of " + numPlayers + "<br>");
            debug_.accept("Position Adjust: " + positionAdjustment + "<br>");

            if (adjustedHandStrength + positionAdjustment >= 1f) {
                AIOutcome outcome = new AIOutcome(potStatus, round_, context_.isLimit());

                float aggression = context_.getStrategy().getStratFactor("basics.aggression", 0f, 2f);
                float call = (startingPosition == POSITION_SMALL)
                        ? .25f * (aggression > 1f ? (2f - aggression) : (3.6f * (1f - aggression)) + 1f)
                        : .05f * (aggression > 1f ? (2f - aggression) : (9f * (1f - aggression)) + 1f);
                float raise = 1f - call;

                outcome.addTuple(AIOutcome.RAISE, "Desirable opening hand in this position.", 0f, call, raise);

                outcome_ = outcome;
                strongestOutcome_ = outcome.getStrongestOutcome(potStatus);
            }

            return;
        }

        // TODO: CONSIDER ALL-IN CALLS

        // SPECIAL RED/DEAD ZONE STRATEGY

        if ((hohZone == HOH_RED) || (hohZone == HOH_DEAD)) {
            // TODO: consider other player zones; need stronger hand with short stacks left
            // to act
            // TODO: lower standards as blinds get closer

            setEligible(OUTCOME_ALL_IN, true);

            float allInOdds = potTotal / playerChips;

            switch (potStatus) {
                case PokerConstants.NO_POT_ACTION :
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.20f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_BLINDS_CLOSING, 0.40f * (9 - handsToBB) / 9.0f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_POT_ODDS, allInOdds / 10.0f);
                    break;
                case PokerConstants.CALLED_POT :
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.15f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_BLINDS_CLOSING, 0.25f * (9 - handsToBB) / 9.0f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_POT_ODDS, allInOdds / 15.0f);
                    break;
                case PokerConstants.RERAISED_POT :
                case PokerConstants.RAISED_POT :
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.10f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_BLINDS_CLOSING, 0.15f * (9 - handsToBB) / 9.0f);
                    adjustOutcome(OUTCOME_ALL_IN, FACTOR_POT_ODDS, allInOdds / 25.0f);
                    break;
            }

            adjustOutcome(OUTCOME_ALL_IN, FACTOR_AGGRESSION, xBasicsAggression);
        } else {

            // SPECIAL BSB STRATEGY

            // CONSIDER OPENING POT FOR STRENGTH - SINGLE LIMPER IS IGNORED

            if ((potStatus == PokerConstants.NO_POT_ACTION)
                    || ((potStatus == PokerConstants.CALLED_POT) && (context_.getNumLimpers() == 1))) {
                int outcome = (potStatus == PokerConstants.NO_POT_ACTION) ? OUTCOME_OPEN_POT : OUTCOME_RAISE;

                setEligible(outcome, true);

                adjustOutcome(outcome, FACTOR_HAND_SELECTION, 0.6f + (adjustedHandStrength / 2.0f));
                adjustOutcome(outcome, FACTOR_AGGRESSION, xBasicsAggression);

                switch (startingPosition) {
                    case POSITION_MIDDLE :
                        // in middle position, four chip hands become openers
                        adjustOutcome(outcome, FACTOR_POSITION, xBasicsPosition * 0.1f);
                        break;
                    case POSITION_LATE :
                        // in late position, three chip hands become openers
                        adjustOutcome(outcome, FACTOR_POSITION, xBasicsPosition * 0.2f);
                        break;
                }

                // TODO: adjust small pairs and low suited connectors

                switch (hohZone) {
                    case HOH_YELLOW :
                        adjustOutcome(outcome, FACTOR_STACK_SIZE, 0.025f * xTournamentStackSize);
                        break;
                    case HOH_ORANGE :
                        adjustOutcome(outcome, FACTOR_STACK_SIZE, 0.05f * xTournamentStackSize);
                        break;
                }

                if ((state_.getSteam() > 0.1) && (xTilt > 0)) {
                    adjustOutcome(outcome, FACTOR_STEAM, state_.getSteam() * xTilt * 0.1f);
                }
            }

            // CONSIDER PLAYING AGAINST MULTIPLE LIMPERS

            if ((potStatus == PokerConstants.CALLED_POT) && (context_.getNumLimpers() > 1)) {
                adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, 0.7f + (adjustedHandStrength / 2.0f));
                adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression);
                adjustOutcome(OUTCOME_CALL, FACTOR_HAND_SELECTION, 0.5f);

                switch (hasActed ? postFlopPosition : startingPosition) {
                    case POSITION_SMALL :
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, -0.075f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, -0.15f * xBasicsPosition);
                        break;
                    case POSITION_BIG :
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, -0.06f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, -0.12f * xBasicsPosition);
                        break;
                    case POSITION_EARLY :
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, -0.05f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, -0.1f * xBasicsPosition);
                        break;
                    case POSITION_MIDDLE :
                        break;
                    case POSITION_LATE :
                    case POSITION_LAST :
                        if (postFlopPosition == POSITION_LAST) {
                            adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, 0.075f * xBasicsPosition);
                            adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, 0.15f * xBasicsPosition);
                        } else {
                            adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, 0.05f * xBasicsPosition);
                            adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, 0.1f * xBasicsPosition);
                        }
                        break;
                }

                if ((state_.getSteam() > 0.1) && (xTilt > 0)) {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_STEAM, state_.getSteam() * xTilt * 0.1f);
                }
            }

            // CONSIDER WHETHER WE CAN PLAY AGAINST A RAISE

            if (potStatus == PokerConstants.RAISED_POT) {
                // TODO: factor in opponent zone and stack size
                // TODO: factor in pot odds and left to act

                switch (startingPosition) {
                    case POSITION_SMALL :
                    case POSITION_BIG :
                        // CONSIDER DEFENDING BLINDS
                        float stealSuspicion = state_.getStealSuspicion();
                        // TODO: not if someone has called the raise already? * already adjusting value
                        if (stealSuspicion > 0.0f) {
                            adjustOutcome(OUTCOME_CALL, FACTOR_STEAL_SUSPECTED, stealSuspicion);
                            adjustOutcome(OUTCOME_RAISE, FACTOR_STEAL_SUSPECTED, stealSuspicion);
                        }
                }

                adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, potOdds * xBasicsPotOdds * 0.02f);
                adjustOutcome(OUTCOME_CALL, FACTOR_BET_TO_CALL,
                        -0.15f * (float) Math.pow(2 * amountToCall / playerChips, 2) * (1.0f - handStrength));

                // devalue raises from players in dire straits
                GamePlayerInfo raiser = context_.getFirstBettor(BettingRound.PRE_FLOP.toLegacy(), false);

                int rZone = context_.getHohZone(raiser);

                switch (rZone) {
                    case HOH_DEAD :
                    case HOH_RED :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.05f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.05f);
                        break;
                    case HOH_ORANGE :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.025f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.025f);
                        break;
                    case HOH_YELLOW :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.015f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.015f);
                        break;
                }

                // devalue raises from frequent raisers
                float raiserFrequency = (float) Math
                        .max(context_.getOpponentModel(raiser).getHandsRaisedPreFlopPercent(0.1f) - 0.1, 0.0);

                adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_RAISE_FREQUENCY,
                        xBasicsObservation * raiserFrequency * 0.3f * handStrength);
                adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_RAISE_FREQUENCY,
                        xBasicsObservation * raiserFrequency * 0.3f * handStrength);

                // devalue raises if we're getting low
                switch (hohZone) {
                    case HOH_ORANGE :
                        adjustOutcome(OUTCOME_CALL, FACTOR_STACK_SIZE,
                                xTournamentStackSize * adjustedHandStrength * 0.05f);
                        break;
                    case HOH_YELLOW :
                        adjustOutcome(OUTCOME_CALL, FACTOR_STACK_SIZE,
                                xTournamentStackSize * adjustedHandStrength * 0.02f);
                        break;
                }

                adjustOutcome(OUTCOME_CALL, FACTOR_HAND_SELECTION, 1.0f + (adjustedHandStrength - 0.6f) / 4.0f);

                // boost pocket pairs
                if (hand.isPair()) {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.4f);
                } else {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.2f);
                }

                adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression);

                if (bored > 0) {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_BOREDOM, bored * 0.05f);
                }

                if ((state_.getSteam() > 0.1) && (xTilt > 0)) {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_STEAM, state_.getSteam() * xTilt * 0.1f);
                }
            }

            // CONSIDER PLAY AGAINST RE-RAISE

            if (potStatus == PokerConstants.RERAISED_POT) {
                adjustOutcome(OUTCOME_CALL, FACTOR_HAND_SELECTION, 0.9f + (adjustedHandStrength - 0.6f) / 4.0f);
                adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, potOdds * xBasicsPotOdds * 0.02f);
                adjustOutcome(OUTCOME_CALL, FACTOR_BET_TO_CALL,
                        -0.15f * (float) Math.pow(2 * amountToCall / playerChips, 2) * (1.0f - handStrength));

                // boost pocket pairs
                if (hand.isPair()) {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.1f);
                } else {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.05f);
                }

                adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression);

                // consider my post-flop position
                switch (postFlopPosition) {
                    case POSITION_EARLY :
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, -0.025f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, -0.5f * xBasicsPosition);
                        break;
                    case POSITION_MIDDLE :
                        break;
                    case POSITION_LATE :
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, 0.025f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, 0.05f * xBasicsPosition);
                        break;
                    case POSITION_LAST :
                        adjustOutcome(OUTCOME_CALL, FACTOR_POSITION, 0.04f * xBasicsPosition);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_POSITION, 0.08f * xBasicsPosition);
                        break;
                }

                GamePlayerInfo firstRaiser = context_.getFirstBettor(BettingRound.PRE_FLOP.toLegacy(), false);
                GamePlayerInfo lastRaiser = context_.getLastBettor(BettingRound.PRE_FLOP.toLegacy(), false);

                // devalue raises from players in dire straits
                int rZone = context_.getHohZone(lastRaiser);

                switch (rZone) {
                    case HOH_DEAD :
                    case HOH_RED :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.05f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.05f);
                        break;
                    case HOH_ORANGE :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.025f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.025f);
                        break;
                    case HOH_YELLOW :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.015f);
                        adjustOutcome(OUTCOME_RAISE, FACTOR_RAISER_STACK_SIZE,
                                xTournamentOpponentStackSize * adjustedHandStrength * 0.015f);
                        break;
                }

                // devalue raises if we're getting low
                switch (hohZone) {
                    case HOH_ORANGE :
                        adjustOutcome(OUTCOME_CALL, FACTOR_STACK_SIZE,
                                xTournamentStackSize * adjustedHandStrength * 0.05f);
                        break;
                    case HOH_YELLOW :
                        adjustOutcome(OUTCOME_CALL, FACTOR_STACK_SIZE,
                                xTournamentStackSize * adjustedHandStrength * 0.02f);
                        break;
                }

                // devalue possible blind defense raise
                if (context_.isBlind(lastRaiser)) {
                    switch (context_.getStartingPositionCategory(firstRaiser)) {
                        case POSITION_EARLY :
                            adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_POSITION, 0.01f);
                            break;
                        case POSITION_MIDDLE :
                            adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_POSITION, 0.03f);
                            break;
                        case POSITION_LATE :
                            adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_POSITION, 0.08f);
                            break;
                        case POSITION_SMALL :
                            adjustOutcome(OUTCOME_CALL, FACTOR_RAISER_POSITION, 0.08f);
                            break;
                    }
                }

                int numPlayersWhenRaised = numWithCards + context_.getNumFoldsSinceLastBet();

                // devalue raises against small number of players
                adjustOutcome(OUTCOME_CALL, FACTOR_PLAYERS_LEFT, (10 - numPlayersWhenRaised) * 0.01f);

                // boost raises with players left to act
                switch (context_.getStartingPositionCategory(lastRaiser)) {
                    case POSITION_EARLY :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RERAISER_POSITION, -0.07f);
                        break;
                    case POSITION_MIDDLE :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RERAISER_POSITION, -0.04f);
                        break;
                    case POSITION_LATE :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RERAISER_POSITION, -0.02f);
                        break;
                    case POSITION_SMALL :
                        adjustOutcome(OUTCOME_CALL, FACTOR_RERAISER_POSITION, -0.01f);
                        break;
                }

                if ((state_.getSteam() > 0.1) && (xTilt > 0)) {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_STEAM, state_.getSteam() * xTilt * 0.05f);
                }
            }

            // consider stealing if no pot action, not already raising,
            // and enough chips to raise at least one big blind
            if ((getStrongestOutcome() != OUTCOME_OPEN_POT) && (potStatus == PokerConstants.NO_POT_ACTION)
                    && (playerChips - amountToCall >= bigBlindAmount)) {
                setEligible(OUTCOME_STEAL, (playerChips > amountToCall));

                adjustOutcome(OUTCOME_STEAL, FACTOR_HAND_SELECTION, adjustedHandStrength + 0.65f);
                adjustOutcome(OUTCOME_STEAL, FACTOR_AGGRESSION, xBasicsAggression);

                int stealBiasIndex = (int) Math
                        .round((0.5f - ((xStealBlinds > 0.5f) ? (xStealBlinds - 0.5f) : xStealBlinds)) / 0.05d);

                float stealBiasValue = SimpleBias.getBiasValue(stealBiasIndex, hand);

                if (((xStealBlinds >= 0.5f) && (stealBiasValue < handStrength))
                        || ((xStealBlinds < 0.5f) && (stealBiasValue > handStrength))) {
                    adjustOutcome(OUTCOME_STEAL, FACTOR_BLIND_STEALING,
                            1.5f * (stealBiasValue - handStrength) * Math.abs(xStealBlinds - 0.5f));
                }

                switch (startingPosition) {
                    case POSITION_EARLY :
                        adjustOutcome(OUTCOME_STEAL, FACTOR_POSITION, -0.4f * xBasicsPosition);
                        break;
                    case POSITION_MIDDLE :
                        adjustOutcome(OUTCOME_STEAL, FACTOR_POSITION, -0.2f * xBasicsPosition);
                        break;
                    case POSITION_LATE :
                        adjustOutcome(OUTCOME_STEAL, FACTOR_POSITION,
                                (context_.isButton(self_) ? 0.2f : 0.1f) * xBasicsPosition);
                        break;
                    case POSITION_SMALL :
                        adjustOutcome(OUTCOME_STEAL, FACTOR_POSITION, -0.15f * xBasicsPosition);
                        break;
                }

                if (bored > 0) {
                    adjustOutcome(OUTCOME_STEAL, FACTOR_BOREDOM, bored * 0.1f);
                }

                float xDeceptionBluffStealBlinds = context_.getStrategy().getStratFactor("deception.bluff.steal_blinds",
                        0.0f, 2.0f);

                // make adjustment to steal score for short stacked blinds
                // make adjustment to steal score for loose/tight blinds

                if ((state_.getSteam() > 0.1) && (xTilt > 0)) {
                    adjustOutcome(OUTCOME_RAISE, FACTOR_STEAM, state_.getSteam() * xTilt * 0.1f);
                }
            }

            // CONSIDER LIMPING

            if (((potStatus == PokerConstants.NO_POT_ACTION) || (potStatus == PokerConstants.CALLED_POT))
                    && (strongestOutcome_ == OUTCOME_FOLD)) {
                setEligible(OUTCOME_LIMP, true);

                // re-apply tightness adjustment to get more limping
                adjustOutcome(OUTCOME_LIMP, FACTOR_HAND_SELECTION,
                        Math.min(1.0f + (adjustedHandStrength + foldStrengthDelta - 0.70f) / 2.0f, 1.0f));

                switch (startingPosition) {
                    case POSITION_MIDDLE :
                        adjustOutcome(OUTCOME_LIMP, FACTOR_POSITION, xBasicsPosition * 0.02f);
                        break;
                    case POSITION_LATE :
                        adjustOutcome(OUTCOME_LIMP, FACTOR_POSITION, xBasicsPosition * 0.05f);
                        break;
                }

                adjustOutcome(OUTCOME_LIMP, FACTOR_POT_ODDS, potOdds * xBasicsPotOdds * 0.02f);

                if (hand.isConnectors(Card.TWO, Card.KING)) {
                    if (hand.isSuited()) {
                        adjustOutcome(OUTCOME_LIMP, FACTOR_IMPLIED_ODDS,
                                potOdds * xBasicsPotOdds * hand.getHighestRank() * 0.0025f);
                    } else {
                        adjustOutcome(OUTCOME_LIMP, FACTOR_IMPLIED_ODDS,
                                potOdds * xBasicsPotOdds * hand.getHighestRank() * 0.00225f);
                    }
                }

                if (bored > 0) {
                    adjustOutcome(OUTCOME_LIMP, FACTOR_BOREDOM, bored * 0.2f);
                }
            }
        }
    }

    private void setPreFlopBetRange() {
        if ((strongestOutcome_ != OUTCOME_ALL_IN) && (context_.getRemainingAverageHohM() < 10.0f)) {
            betRange_ = BetRange.potRelative(0.50f, 0.75f);
        } else
            switch (strongestOutcome_) {
                case OUTCOME_OPEN_POT :
                    switch (context_.getStartingPositionCategory(self_)) {
                        case POSITION_EARLY :
                            betRange_ = BetRange.bigBlindRelative(2.5f, 3.0f);
                            break;
                        case POSITION_MIDDLE :
                            betRange_ = BetRange.bigBlindRelative(3.0f, 3.5f);
                            break;
                        case POSITION_LATE :
                            betRange_ = BetRange.bigBlindRelative(3.5f, 4.0f);
                            break;
                        case POSITION_SMALL :
                            betRange_ = BetRange.bigBlindRelative(3.0f, 3.0f);
                            break;
                        default :
                            betRange_ = BetRange.bigBlindRelative(3.0f, 5.0f);
                            break;
                    }
                    break;
                case OUTCOME_STEAL :
                    betRange_ = BetRange.bigBlindRelative(2.0f, 4.0f);
                    break;
                case OUTCOME_ALL_IN :
                    betRange_ = BetRange.allIn();
                    break;
                case OUTCOME_RAISE :
                    betRange_ = BetRange.bigBlindRelative(3.0f, 4.5f);
                    break;
            }

        if (outcome_ != null) {
            outcome_.setBetRange(betRange_, null);
        }
    }

    private float getLimpFactor(Hand hand) {
        float xDisciplineLimp = 0.0f;

        // limping with small pairs

        if (hand.isPair() && (hand.getCard(0).getRank() <= 6)) {
            xDisciplineLimp = context_.getStrategy().getStratFactor("discipline.limp.small_pair", -1.0f, 1.0f);
        }

        // this case comes before connectors because A2 and AK aren't considered
        // connectors here

        else if ((hand.getCard(0).getRank() == Card.ACE) || (hand.getCard(1).getRank() == Card.ACE)) {
            if (hand.isSuited()) {
                xDisciplineLimp = context_.getStrategy().getStratFactor("discipline.limp.suited_ace", -1.0f, 1.0f);
            } else {
                xDisciplineLimp = context_.getStrategy().getStratFactor("discipline.limp.unsuited_ace", -1.0f, 1.0f);
            }
        }

        // limping with connectors

        else if (Math.abs(hand.getCard(0).getRank() - hand.getCard(1).getRank()) == 1) {
            if (hand.isSuited()) {
                xDisciplineLimp = context_.getStrategy().getStratFactor("discipline.limp.suited_connectors", -1.0f,
                        1.0f);
            } else {
                xDisciplineLimp = context_.getStrategy().getStratFactor("discipline.limp.unsuited_connectors", -1.0f,
                        1.0f);
            }
        } else {
            xDisciplineLimp = context_.getStrategy().getStratFactor("discipline.limp.other", -1.0f, 1.0f);
        }
        return xDisciplineLimp;
    }

    private void executeFlopTurn() {
        float xBasicsPosition = context_.getStrategy().getStratFactor("basics.position", 0.0f, 2.0f);
        float xBasicsPotOdds = context_.getStrategy().getStratFactor("basics.pot_odds_call", 0.0f, 2.0f);
        float xBasicsAggression = context_.getStrategy().getStratFactor("basics.aggression", -1.0f, +1.0f);
        float xBasicsObservation = context_.getStrategy().getStratFactor("basics.observation", 0.0f, 2.0f);
        float xTilt = context_.getStrategy().getStratFactor("discipline.tilt", 0.0f, 1.0f);

        Hand community = context_.getCommunity();
        Hand pocket = context_.getPocketCards(self_);

        int startingPosition = context_.getStartingPositionCategory(self_);

        int numWithCards = context_.getNumPlayersWithCards();
        int potStatus = context_.getPotStatus();
        int amountToCall = context_.getAmountToCall(self_);
        int potTotal = context_.getTotalPotChipCount();
        int playerChips = self_.getChipCount();

        float potOdds = amountToCall > 0 ? (float) potTotal / (float) amountToCall : 0;
        float rhs = state_.getRawHandStrength();
        float bhs = state_.getBiasedHandStrength();
        float drawPotential = state_.getBiasedPositivePotential();
        float outdrawRisk = (float) Math.pow(1.0 + state_.getBiasedNegativePotential(), numWithCards - 1) - 1.0f;

        float ehs = state_.getBiasedEffectiveHandStrength(xBasicsPotOdds * potOdds);

        PureHandPotential potential = new PureHandPotential(pocket, community);

        int pNutFlush = potential.getHandCount(PureHandPotential.NUT_FLUSH, 0);
        int pNonNutFlush = potential.getHandCount(PureHandPotential.FLUSH, 0) - pNutFlush;
        int pNutStraight = potential.getHandCount(PureHandPotential.NUT_STRAIGHT, 0);
        int pNonNutStraight = potential.getHandCount(PureHandPotential.NON_NUT_STRAIGHT, 0);

        float xStraightDraw = context_.getStrategy().getStratFactor("draws.straight.nut", -1.0f, 1.0f) * pNutStraight
                + context_.getStrategy().getStratFactor("draws.straight.non_nut", -1.0f, 1.0f) * 0.5f * pNonNutStraight;

        float xFlushDraw = context_.getStrategy().getStratFactor("draws.flush.nut", -1.0f, 1.0f) * pNutFlush
                + context_.getStrategy().getStratFactor("draws.flush.non_nut", -1.0f, 1.0f) * 0.5f * pNonNutFlush;

        if (community.hasQuads() || (community.hasTrips() && community.hasPair())) {
            xStraightDraw *= 0.0f;
            xFlushDraw *= 0.0f;
        } else if (community.hasPossibleFlush()) {
            xFlushDraw *= 0.75d;

            if (community.hasFlush()) {
                xStraightDraw = 0.0f;
            } else {
                xStraightDraw *= 0.5f;
            }
        }

        int roundsNoAction = 0;

        switch (round_) {
            case 3 : // BettingRound.RIVER.toLegacy()
                if (!context_.wasPotAction(BettingRound.TURN.toLegacy())) {
                    ++roundsNoAction;
                } else {
                    break;
                }
            case 2 : // BettingRound.TURN.toLegacy()
                if (!context_.wasPotAction(BettingRound.FLOP.toLegacy())) {
                    ++roundsNoAction;
                }
                break;
        }

        int handsToBB = context_.getHandsBeforeBigBlind(self_);

        if (state_.debugEnabled()) {
            debug_.accept("Raw Tilt Factor: " + xTilt + "<br>");
            debug_.accept("Raw Hand Strength: " + rhs + "<br>");
            debug_.accept("Biased Hand Strength: " + bhs + "<br>");
            debug_.accept("Effective Hand Strength: " + ehs + "<br>");
            debug_.accept("Positive Potential: " + state_.getPositiveHandPotential() + "<br>");
            debug_.accept("Biased Positive Potential: " + state_.getBiasedPositivePotential() + "<br>");
            debug_.accept("Negative Potential: " + state_.getNegativeHandPotential() + "<br>");
            debug_.accept("Biased Negative Potential: " + state_.getBiasedNegativePotential() + "<br>");
            debug_.accept("Outdraw Risk: " + outdrawRisk + "<br>");
            debug_.accept("Pot Total: " + potTotal + "<br>");
            debug_.accept("Amount To Call: " + amountToCall + "<br>");
            debug_.accept("Pot Odds: " + potOdds + "<br>");
            debug_.accept("Pot Odds Factor: " + xBasicsPotOdds + "<br>");
            debug_.accept("Straight Draw Factor: " + xStraightDraw + "<br>");
            debug_.accept("Flush Draw Factor: " + xFlushDraw + "<br>");
            debug_.accept("Aggression Factor: " + xBasicsAggression + "<br>");
            debug_.accept("Preceding post-flop rounds w/ no action: " + roundsNoAction + "<br>");
            debug_.accept("Hands Before Big Blind: " + handsToBB + "<br>");
        }

        // default bet range 1/2 to whole pot
        betRange_ = BetRange.potRelative(0.5f, 1.0f);

        if ((round_ == BettingRound.RIVER.toLegacy()) && (potStatus != PokerConstants.NO_POT_ACTION) && (rhs == 1.0f)) {
            if (playerChips > amountToCall) {
                float allInPotRatio = (float) Math.ceil((float) (playerChips - amountToCall) / (float) potTotal);

                // *always* raise/reraise on the river with the pure nuts
                betRange_ = BetRange.potRelative((float) (Math.min(0.5f, allInPotRatio)), allInPotRatio);

                adjustOutcome(OUTCOME_RAISE, FACTOR_RAW_HAND_STRENGTH, 1.0f);
            } else {
                adjustOutcome(OUTCOME_CALL, FACTOR_RAW_HAND_STRENGTH, 1.0f);
            }

            return;
        }

        if (potStatus == PokerConstants.NO_POT_ACTION) {
            // initialize default action
            setEligible(OUTCOME_CHECK, true);
            adjustOutcome(OUTCOME_CHECK, FACTOR_DEFAULT, 1.0f);

            float allWeak = 1.0f;

            // of remaining players, compute overall likelihood none has a calling hand
            for (int i = context_.getNumPlayersAtTable() - 1; i >= 0; --i) {
                GamePlayerInfo p = context_.getPlayerAt(i);
                if (context_.getSeat(p) == seat_)
                    continue;
                if (p.isFolded())
                    continue;
                if (context_.getLastActionThisRound(p) == AIContext.ACTION_CHECK)
                    continue;
                allWeak *= context_.getOpponentModel(p).getCheckFoldPostFlop(round_, 0.5f);
            }

            if (state_.debugEnabled()) {
                debug_.accept("All Weak Probability: " + allWeak + "<br>");
            }

            // CONSIDER WHETHER WE CAN OPEN THE BETTING FOR STRENGTH

            setEligible(OUTCOME_BET, true);

            // boost for first action
            adjustOutcome(OUTCOME_BET, FACTOR_FIRST_ACTION, 0.25f);
            // penalty for players left to act
            adjustOutcome(OUTCOME_BET, FACTOR_LEFT_TO_ACT, -0.08f * nPlayersAfter_ * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_BET, FACTOR_RAW_HAND_STRENGTH, rhs * rhs + 0.05f);
            adjustOutcome(OUTCOME_BET, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
            adjustOutcome(OUTCOME_BET, FACTOR_HAND_POTENTIAL, drawPotential);
            adjustOutcome(OUTCOME_BET, FACTOR_OUTDRAW_RISK, outdrawRisk);
            adjustOutcome(OUTCOME_BET, FACTOR_AGGRESSION, xBasicsAggression * 0.1f);
            adjustOutcome(OUTCOME_BET, FACTOR_CHECKED_AROUND, roundsNoAction * 0.15f);

            // if everyone is likely to be weak, boost bet value, but less for very strong
            // hands where we might rather check/call or check-raise
            if ((round_ == BettingRound.FLOP.toLegacy()) || (roundsNoAction > 0)) {
                adjustOutcome(OUTCOME_BET, FACTOR_STEAL_POTENTIAL,
                        xBasicsObservation * (1.0f - rhs * rhs) * allWeak * 0.50f);
            }

            // adjust for tilt
            if ((state_.getSteam() > 0.1) && (xTilt > 0)) {
                adjustOutcome(OUTCOME_BET, FACTOR_STEAM, state_.getSteam() * xTilt * 0.1f);
            }

            if ((getStrongestOutcome() == OUTCOME_BET) && (nPlayersAfter_ > 0)) {
                // CONSIDER CHECK-RAISE AND SLOW-PLAY

                setEligible(OUTCOME_CHECK_RAISE, true);

                // boost for players left to act
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_LEFT_TO_ACT,
                        0.06f * nPlayersAfter_ * xBasicsPosition * (1.0f - allWeak));

                // base hand strength scores
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_RAW_HAND_STRENGTH, rhs * rhs * rhs + 0.10f);
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_BIASED_HAND_STRENGTH, bhs - rhs);
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_STEAL_POTENTIAL, xBasicsObservation * allWeak * -0.15f);

                // penalty for outdraw risk
                adjustOutcome(OUTCOME_CHECK_RAISE, FACTOR_OUTDRAW_RISK, -outdrawRisk);
            } else {
                // CONSIDER WHETHER WE SHOULD MAKE A CONTINUATION BET

                // consider continuation bet on the flop if last raiser pre-flop
                if (context_.wasLastRaiserPreFlop(self_) && (numWithCards < 4)
                        && (round_ == BettingRound.FLOP.toLegacy())) {
                    setEligible(OUTCOME_CONTINUATION_BET, true);

                    int draws = pNutFlush * 3 + pNonNutFlush + pNutStraight * 2 + pNonNutStraight;

                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_RAW_HAND_STRENGTH, rhs * rhs + 0.05f);
                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_HAND_POTENTIAL, drawPotential - 0.01f * draws);
                    adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_FIRST_ACTION, 0.35f);

                    // stronger if first raiser pre-flop
                    if (context_.wasFirstRaiserPreFlop(self_)) {
                        // even stronger if raised in-between first and last raise
                        if (context_.wasOnlyRaiserPreFlop(self_)) {
                            adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_ONLY_PRE_FLOP_RAISER, 0.15f);
                        } else {
                            adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_FIRST_PRE_FLOP_RAISER, 0.17f);
                        }
                    } else {
                        adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_LAST_PRE_FLOP_RAISER, 0.05f);
                    }

                    if (context_.wasFirstRaiserPreFlop(self_)) {
                        // stronger the earlier position we opened from
                        switch (startingPosition) {
                            case POSITION_EARLY :
                                adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PRE_FLOP_POSITION, 0.1f);
                                break;
                            case POSITION_MIDDLE :
                                adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PRE_FLOP_POSITION, 0.05f);
                                break;
                        }
                    }

                    // weaker if two opponents
                    if (numWithCards == 3) {
                        adjustOutcome(OUTCOME_CONTINUATION_BET, FACTOR_PLAYERS_LEFT, -0.1f);
                    }

                    // stronger the larger our raises were?

                    if (strongestOutcome_ == OUTCOME_CONTINUATION_BET) {
                        betRange_ = BetRange.potRelative(0.4f, 0.7f);
                    }
                } else {
                    // consider probe bet if original raiser has checked
                    GamePlayerInfo firstRaiser = context_.getFirstBettor(BettingRound.PRE_FLOP.toLegacy(), true);

                    if (firstRaiser != null) {
                        int firstRaiserAction = context_.getFirstVoluntaryAction(firstRaiser, round_);

                        if (firstRaiserAction == AIContext.ACTION_CHECK) {
                            probeBet_ = true;

                            betRange_ = BetRange.potRelative(0.3f, 0.5f);

                            adjustOutcome(OUTCOME_BET, FACTOR_PROBE_BET, rhs * 0.1f);
                        }
                    }
                }
            }
        } else // pot action already
        {
            float xrhs;
            float xbhs;

            setEligible(OUTCOME_FOLD, true);
            adjustOutcome(OUTCOME_FOLD, FACTOR_DEFAULT, 1.0f);

            setEligible(OUTCOME_CALL, true);
            setEligible(OUTCOME_RAISE, amountToCall < playerChips);

            GamePlayerInfo bettor = context_.getFirstBettor(round_, false);
            V2OpponentModel bettorModel = context_.getOpponentModel(bettor);

            // CONSIDER WHETHER WE CAN CALL

            xrhs = (float) Math.sin((rhs - 0.5f) * Math.PI) * 0.5f + 0.50f;
            xbhs = (float) Math.sin((bhs - 0.5f) * Math.PI) * 0.5f + 0.50f;

            float bettorActFrequency = bettorModel.getActPostFlop(round_, 0.1f);
            float bettorCheckFoldFrequency = bettorModel.getCheckFoldPostFlop(round_, 0.5f) * bettorActFrequency;
            float bettorOpenFrequency = bettorModel.getOpenPostFlop(round_, 0.5f) * bettorActFrequency;
            float bettorRaiseFrequency = bettorModel.getRaisePostFlop(round_, 0.5f) * bettorActFrequency;

            float bettorOverbetFrequency = bettorModel.getOverbetFrequency(0.5f);
            float bettorBetFoldFrequency = bettorModel.getBetFoldFrequency(0.5f);

            // penalty for players left to act
            adjustOutcome(OUTCOME_CALL, FACTOR_LEFT_TO_ACT, -0.08f * nPlayersAfter_ * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_CALL, FACTOR_RAW_HAND_STRENGTH, xrhs + 0.10f);
            adjustOutcome(OUTCOME_CALL, FACTOR_BIASED_HAND_STRENGTH, xbhs - xrhs);
            adjustOutcome(OUTCOME_CALL, FACTOR_OUTDRAW_RISK, -outdrawRisk);
            adjustOutcome(OUTCOME_CALL, FACTOR_CHECKED_AROUND, roundsNoAction * 0.15f);
            adjustOutcome(OUTCOME_CALL, FACTOR_STRAIGHT_DRAW, xStraightDraw * 0.075f);
            adjustOutcome(OUTCOME_CALL, FACTOR_FLUSH_DRAW, xFlushDraw * 0.05f);
            if (round_ < BettingRound.RIVER.toLegacy()) {
                adjustOutcome(OUTCOME_CALL, FACTOR_HAND_POTENTIAL,
                        drawPotential * xBasicsPotOdds * (potOdds + 1.0f) / 2);
            } else {
                adjustOutcome(OUTCOME_CALL, FACTOR_POT_ODDS, (potOdds + 1.0f) * xBasicsPotOdds * ehs / 2);
            }
            adjustOutcome(OUTCOME_CALL, FACTOR_BET_TO_CALL,
                    -0.15f * (float) Math.pow(2 * amountToCall / playerChips, 2) * (1.0f - ehs));
            adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_BET_FREQUENCY,
                    xBasicsObservation * bettorOpenFrequency * 0.20f);
            adjustOutcome(OUTCOME_CALL, FACTOR_OPPONENT_OVERBET_FREQUENCY,
                    xBasicsObservation * bettorOverbetFrequency * 0.10f);

            // CONSIDER WHETHER WE CAN RAISE / RE-RAISE

            xrhs = (rhs + 0.05f) * (rhs + 0.05f);
            xbhs = (bhs + 0.05f) * (bhs + 0.05f);

            // penalty for players left to act
            adjustOutcome(OUTCOME_RAISE, FACTOR_LEFT_TO_ACT, -0.05f * nPlayersAfter_ * xBasicsPosition);
            // base hand strength scores
            adjustOutcome(OUTCOME_RAISE, FACTOR_RAW_HAND_STRENGTH, xrhs + 0.10f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_BIASED_HAND_STRENGTH, xbhs - xrhs);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OUTDRAW_RISK, outdrawRisk);
            adjustOutcome(OUTCOME_RAISE, FACTOR_CHECKED_AROUND, roundsNoAction * 0.15f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_AGGRESSION, xBasicsAggression * 0.1f);
            if (round_ < BettingRound.RIVER.toLegacy()) {
                adjustOutcome(OUTCOME_RAISE, FACTOR_HAND_POTENTIAL,
                        drawPotential * xBasicsPotOdds * (potOdds + 1.0f) / 2);
            } else {
                adjustOutcome(OUTCOME_RAISE, FACTOR_POT_ODDS, (potOdds + 1.0f) * xBasicsPotOdds * ehs / 2);
            }
            adjustOutcome(OUTCOME_RAISE, FACTOR_BET_TO_CALL,
                    -0.15f * (float) Math.pow(amountToCall / playerChips, 2) * (1.0f - ehs));
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_BET_FREQUENCY,
                    xBasicsObservation * bettorOpenFrequency * 0.20f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_OVERBET_FREQUENCY,
                    xBasicsObservation * bettorOverbetFrequency * 0.10f);
            adjustOutcome(OUTCOME_RAISE, FACTOR_OPPONENT_BET_FOLD_FREQUENCY,
                    xBasicsObservation * bettorBetFoldFrequency * 0.10f);
        }
    }

    private void adjustOutcome(int outcome, int factor, float delta) {
        adjustOutcome(outcome, factor, CURVE_LINEAR, false, 1.0f, 0.0f, 0.0f, delta);
    }

    private void adjustOutcome(int outcome, int factor, int curve, boolean invert, float weight, float min, float max,
            float value) {
        if (Float.isNaN(value)) {
            throw new IllegalStateException("NaN in value!");
        }
        if (Float.isNaN(weight)) {
            throw new IllegalStateException("NaN in weight!");
        }
        if (eligible_[outcome]) {
            OutcomeAdjustment adjustment = new OutcomeAdjustment(outcome, factor, curve, invert, weight, min, max,
                    value);

            score_[outcome] += adjustment.fx * weight;
            weights_[outcome] += weight;
            addAdjustment(outcome, adjustment);
        }
    }

    private void determineStrongestOutcome() {
        float strongestScore = 0.0f;

        float score;

        for (int i = 0; i < score_.length; ++i) {
            if (eligible_[i] && ((int) (score_[i] * 100) > 0)) {
                score = score_[i];

                if (score >= strongestScore) {
                    strongestOutcome_ = i;
                    strongestScore = score;
                }
            }
        }
    }

    public AIOutcome getOutcome() {
        return outcome_;
    }

    public PlayerAction getAction() {
        switch (strongestOutcome_) {
            case OUTCOME_CHECK :
            case OUTCOME_CHECK_RAISE :
                return PlayerAction.check();
            case OUTCOME_FOLD :
                return PlayerAction.fold();
            case OUTCOME_LIMP :
            case OUTCOME_CALL :
            case OUTCOME_SLOW_PLAY :
                return PlayerAction.call();
            case OUTCOME_ALL_IN :
            case OUTCOME_OPEN_POT :
            case OUTCOME_BET :
            case OUTCOME_CONTINUATION_BET :
            case OUTCOME_STEAL :
            case OUTCOME_RAISE :
            case OUTCOME_SEMI_BLUFF :
            case OUTCOME_TRAP :
            case OUTCOME_BLUFF :
                return PlayerAction.raise(0);
        }

        return PlayerAction.fold();
    }

    private void addAdjustment(int outcome, OutcomeAdjustment adjustment) {
        adjustment.next = adjustments_[outcome][adjustment.factor];

        adjustments_[outcome][adjustment.factor] = adjustment;

        determineStrongestOutcome();
    }

    private class OutcomeAdjustment {
        OutcomeAdjustment next = null;

        public int outcome;
        public int factor;
        public int curve;
        public boolean invert;
        public float weight;
        public float min;
        public float max;
        public float value;
        public float x;
        public float fx;

        public OutcomeAdjustment(int outcome, int factor, int curve, boolean invert, float weight, float min, float max,
                float value) {
            this.outcome = outcome;
            this.factor = factor;
            this.curve = curve;
            this.invert = invert;
            this.weight = weight;
            this.min = min;
            this.max = max;
            this.value = value;

            if (min != max) {
                if (value > max) {
                    value = max;
                } else if (value < min) {
                    value = min;
                }

                x = (value - min) / (max - min);
            } else {
                x = value;
            }

            fx = (float) Math.pow(x, curve);

            if (invert) {
                fx = 1.0f - fx;
            }
        }
    }

    public BetRange getBetRange() {
        return betRange_;
    }

    public boolean isProbeBetAppropriate() {
        return probeBet_;
    }

    public int getStrongestOutcome() {
        return strongestOutcome_;
    }

    public String getStrongestOutcomeName() {
        return outcomeNames_.get(strongestOutcome_);
    }
}
