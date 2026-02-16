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

import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.PokerConstants;

import java.util.ArrayList;

/**
 * Represents the AI's decision outcome with weighted probabilities. Adapted
 * from the original poker module version to remove Swing and PokerPlayer
 * dependencies.
 */
public class AIOutcome {

    public static final int FOLD = 0;
    public static final int CHECK = 0;
    public static final int CALL = 1;
    public static final int BET = 2;
    public static final int RAISE = 2;
    public static final int RERAISE = 2;

    // Outcome type constants (matches PureRuleEngine)
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

    private final ArrayList<Tuple> tuples_ = new ArrayList<>();
    private float checkFold;
    private float call;
    private float betRaise;
    private boolean computed_ = false;
    private BetRange betRange_;
    private final int potStatus_;
    private final int round_;
    private final boolean isLimit_;
    private String allInReason_;

    private static class Tuple {

        int outcome;
        String tactic;
        float checkFold;
        float call;
        float betRaise;

        Tuple(int outcome, String tactic, float checkFold, float call, float betRaise) {
            this.outcome = outcome;
            this.tactic = tactic;
            this.checkFold = checkFold;
            this.call = call;
            this.betRaise = betRaise;
        }
    }

    public AIOutcome(int potStatus, int round, boolean isLimit) {
        potStatus_ = potStatus;
        round_ = round;
        isLimit_ = isLimit;
    }

    public void addTuple(int outcome, String tactic, float checkFold, float call, float betRaise) {
        tuples_.add(new Tuple(outcome, tactic, checkFold, call, betRaise));
        computed_ = false;
    }

    public void setBetRange(BetRange betRange, String allInReason) {
        betRange_ = betRange;
        allInReason_ = allInReason;
    }

    public String toHTML() {
        return toHTML(0);
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public String toHTML(int brevity) {
        StringBuilder buf = new StringBuilder();
        Tuple tuple;
        boolean found;

        if (!computed_)
            computeAverageTuple();

        if (checkFold > 0f) {
            if (brevity < 1)
                buf.append("Recommend ");
            if (potStatus_ == PokerConstants.NO_POT_ACTION) {
                buf.append("Check");
            } else {
                buf.append("Fold");
            }
            if (brevity < 5) {
                buf.append(" ");
                buf.append(PokerConstants.formatPercent(checkFold * 100f));
                buf.append("%");
            }
            if (brevity < 2) {
                found = false;
                for (int i = 0; i < tuples_.size(); ++i) {
                    tuple = tuples_.get(i);
                    if (tuple.outcome == AIOutcome.CHECK || tuple.outcome == AIOutcome.FOLD) {
                        if (!found)
                            buf.append(" (");
                        else
                            buf.append(", ");
                        buf.append(tuple.tactic);
                        found = true;
                    }
                }
                if (found)
                    buf.append(")");
            }
        }

        if (call > 0f) {
            if (buf.length() > 0)
                buf.append(" or ");
            else if (brevity < 1)
                buf.append("Recommend ");
            buf.append("Call");
            if (brevity < 5) {
                buf.append(" ");
                buf.append(PokerConstants.formatPercent(call * 100f));
                buf.append("%");
            }
            if (brevity < 2) {
                found = false;
                for (int i = 0; i < tuples_.size(); ++i) {
                    tuple = tuples_.get(i);
                    if (tuple.outcome == AIOutcome.CALL) {
                        if (!found)
                            buf.append(" (");
                        else
                            buf.append(", ");
                        buf.append(tuple.tactic);
                        found = true;
                    }
                }
                if (found)
                    buf.append(")");
            }
        }

        if (betRaise > 0f) {
            if (buf.length() > 0)
                buf.append(" or ");
            else if (brevity < 1)
                buf.append("Recommend ");

            switch (potStatus_) {
                case PokerConstants.NO_POT_ACTION :
                    if (round_ == BettingRound.PRE_FLOP.toLegacy())
                        buf.append("Raise");
                    else
                        buf.append("Bet");
                    break;
                case PokerConstants.RAISED_POT :
                    buf.append("Raise");
                    break;
                default :
                    buf.append("Re-Raise");
                    break;
            }

            if (brevity < 5) {
                buf.append(" ");
                buf.append(PokerConstants.formatPercent(betRaise * 100f));
                buf.append("%");
            }

            if ((brevity < 3) && !isLimit_ && (betRange_ != null)) {
                buf.append(" ");
                buf.append(betRange_.toDescription());
            }

            if (allInReason_ != null) {
                buf.append(" All In");
                if (brevity < 2) {
                    buf.append(" (");
                    buf.append(allInReason_);
                    buf.append(")");
                }
            }

            if (brevity < 2) {
                found = false;
                for (int i = 0; i < tuples_.size(); ++i) {
                    tuple = tuples_.get(i);
                    if (tuple.outcome == AIOutcome.BET || tuple.outcome == AIOutcome.RAISE) {
                        if (!found)
                            buf.append(" (");
                        else
                            buf.append(", ");
                        buf.append(tuple.tactic);
                        found = true;
                    }
                }
                if (found)
                    buf.append(")");
            }
        }

        return buf.toString();
    }

    public float getCheckFold() {
        if (!computed_)
            computeAverageTuple();
        return checkFold;
    }

    public float getCall() {
        if (!computed_)
            computeAverageTuple();
        return call;
    }

    public float getBetRaise() {
        if (!computed_)
            computeAverageTuple();
        return betRaise;
    }

    public int getStrongestOutcome(int potStatus) {
        if (!computed_)
            computeAverageTuple();
        if (checkFold > call) {
            if (checkFold > betRaise) {
                return (potStatus == PokerConstants.NO_POT_ACTION) ? OUTCOME_CHECK : OUTCOME_FOLD;
            }
        } else {
            if (call > betRaise)
                return OUTCOME_CALL;
        }
        if (potStatus == PokerConstants.NO_POT_ACTION) {
            return (round_ == BettingRound.PRE_FLOP.toLegacy()) ? OUTCOME_OPEN_POT : OUTCOME_BET;
        } else {
            return OUTCOME_RAISE;
        }
    }

    public int selectOutcome(int potStatus) {
        if (!computed_)
            computeAverageTuple();
        float v = (float) Math.random();
        if (v < checkFold) {
            return (potStatus == PokerConstants.NO_POT_ACTION) ? OUTCOME_CHECK : OUTCOME_FOLD;
        } else if (v < checkFold + call) {
            return OUTCOME_CALL;
        } else {
            return (potStatus == PokerConstants.NO_POT_ACTION) ? OUTCOME_BET : OUTCOME_RAISE;
        }
    }

    private void computeAverageTuple() {
        checkFold = 0f;
        call = 0f;
        betRaise = 0f;
        for (int i = 0; i < tuples_.size(); ++i) {
            Tuple tuple = tuples_.get(i);
            checkFold += tuple.checkFold;
            call += tuple.call;
            betRaise += tuple.betRaise;
        }
        checkFold /= tuples_.size();
        call /= tuples_.size();
        betRaise /= tuples_.size();
        computed_ = true;
    }
}
