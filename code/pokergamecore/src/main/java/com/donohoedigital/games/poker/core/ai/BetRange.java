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

/**
 * Represents a range of bet sizes, either in terms of the pot, a player's
 * stack, or the big blind amount. Adapted from the original poker module
 * version to remove PokerPlayer and PropertyConfig dependencies.
 */
public class BetRange {

    public static final int POT_SIZE = 1;
    public static final int STACK_SIZE = 2;
    public static final int BIG_BLIND = 3;
    public static final int ALL_IN = 4;

    private final int type_;
    private final float min_;
    private final float max_;
    private final int stackChipCount_;

    /**
     * Create an ALL_IN bet range.
     */
    public BetRange(int type) {
        this(type, 0, 0.0f, 0.0f);
        if (type != ALL_IN) {
            throw new IllegalArgumentException("Single-arg constructor only valid for ALL_IN, got " + type);
        }
    }

    /**
     * Create a POT_SIZE or BIG_BLIND relative bet range.
     */
    public BetRange(int type, float min, float max) {
        this(type, 0, min, max);
        if (type != POT_SIZE && type != BIG_BLIND) {
            throw new IllegalArgumentException(
                    "Two-arg type constructor only valid for POT_SIZE or BIG_BLIND, got " + type);
        }
    }

    private BetRange(int type, int stackChipCount, float min, float max) {
        type_ = type;
        stackChipCount_ = stackChipCount;
        min_ = min;
        max_ = max;

        switch (type_) {
            case STACK_SIZE :
                if (stackChipCount_ <= 0)
                    throw new IllegalArgumentException("Stack relative BetRange but no stack size.");
                break;
            case POT_SIZE :
            case BIG_BLIND :
                if (min_ == max_ && min_ == 0.0f)
                    throw new IllegalArgumentException("BetRange min/max both zero.");
                break;
            case ALL_IN :
                break;
            default :
                throw new IllegalArgumentException("Unrecognized BetRange type " + type_ + '.');
        }

        if (min_ > max_)
            throw new IllegalArgumentException("BetRange min greater than max.");
    }

    /**
     * Create an ALL_IN bet range.
     */
    public static BetRange allIn() {
        return new BetRange(ALL_IN, 0, 0.0f, 0.0f);
    }

    /**
     * Create a pot-relative bet range.
     */
    public static BetRange potRelative(float min, float max) {
        return new BetRange(POT_SIZE, 0, min, max);
    }

    /**
     * Create a stack-relative bet range. Replaces the original PokerPlayer
     * constructor since we cannot depend on PokerPlayer in pokergamecore.
     */
    public static BetRange stackRelative(int stackChipCount, float min, float max) {
        return new BetRange(STACK_SIZE, stackChipCount, min, max);
    }

    /**
     * Create a big-blind-relative bet range.
     */
    public static BetRange bigBlindRelative(float min, float max) {
        return new BetRange(BIG_BLIND, 0, min, max);
    }

    public int getType() {
        return type_;
    }

    public float getMin() {
        return min_;
    }

    public float getMax() {
        return max_;
    }

    public int getStackChipCount() {
        return stackChipCount_;
    }

    public int getMinBet(int chipCount, int potSize, int bigBlind, int toCall, int minRaise, int minChip) {
        return chooseBetAmount(chipCount, potSize, bigBlind, toCall, minRaise, minChip, 0.0f);
    }

    public int getMaxBet(int chipCount, int potSize, int bigBlind, int toCall, int minRaise, int minChip) {
        return chooseBetAmount(chipCount, potSize, bigBlind, toCall, minRaise, minChip, 1.0f);
    }

    public int chooseBetAmount(int chipCount, int potSize, int bigBlind, int toCall, int minRaise, int minChip) {
        return chooseBetAmount(chipCount, potSize, bigBlind, toCall, minRaise, minChip, (float) Math.random());
    }

    private int chooseBetAmount(int chipCount, int potSize, int bigBlind, int toCall, int minRaise, int minChip,
            float v) {
        if (type_ == ALL_IN)
            return chipCount;

        int betSize = 0;

        switch (type_) {
            case POT_SIZE :
                betSize = (int) (((potSize + toCall) * (min_ + v * (max_ - min_))));
                break;
            case STACK_SIZE :
                betSize = (int) ((stackChipCount_ - toCall) * (min_ + v * (max_ - min_)));
                break;
            case BIG_BLIND :
                betSize = (int) (bigBlind * (min_ + v * (max_ - min_)));
                break;
        }

        int oddChips = betSize % minChip;
        if (oddChips > 0)
            betSize += minChip - oddChips;

        if (betSize < minRaise) {
            if (min_ > 0.0f)
                betSize = minRaise;
            else
                betSize = 0;
        }

        if (betSize > chipCount - toCall)
            betSize = chipCount - toCall;

        return betSize;
    }

    /**
     * Returns a human-readable description of this bet range.
     */
    public String toDescription() {
        StringBuilder buf = new StringBuilder();
        switch (type_) {
            case POT_SIZE :
                if (min_ == max_) {
                    buf.append((int) (min_ * 100.0f)).append("% pot");
                } else {
                    buf.append((int) (min_ * 100.0f)).append("%-").append((int) (max_ * 100.0f)).append("% pot");
                }
                break;
            case STACK_SIZE :
                if (min_ == max_) {
                    buf.append((int) (min_ * 100.0f)).append("% stack");
                } else {
                    buf.append((int) (min_ * 100.0f)).append("%-").append((int) (max_ * 100.0f)).append("% stack");
                }
                break;
            case BIG_BLIND :
                if (min_ == max_) {
                    buf.append(min_).append("x BB");
                } else {
                    buf.append(min_).append('-').append(max_).append("x BB");
                }
                break;
            case ALL_IN :
                buf.append("All In");
                break;
        }
        return buf.toString();
    }
}
