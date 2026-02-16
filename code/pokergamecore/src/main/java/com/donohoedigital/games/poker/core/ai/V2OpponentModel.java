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
package com.donohoedigital.games.poker.core.ai;

/**
 * Read-only interface for opponent modeling data used by V2 AI. The
 * write/update side stays in the poker module (OpponentModel); the read side is
 * exposed here for PureRuleEngine and V2Algorithm.
 *
 * Round parameters use legacy integer values: FLOP=1, TURN=2, RIVER=3.
 */
public interface V2OpponentModel {

    // Pre-flop statistics
    float getPreFlopTightness(int position, float defVal);

    float getPreFlopAggression(int position, float defVal);

    // Post-flop per-round statistics
    float getActPostFlop(int round, float defVal);

    float getCheckFoldPostFlop(int round, float defVal);

    float getOpenPostFlop(int round, float defVal);

    float getRaisePostFlop(int round, float defVal);

    // Overall statistics
    int getHandsPlayed();

    float getHandsPaidPercent(float defVal);

    float getHandsLimpedPercent(float defVal);

    float getHandsFoldedUnraisedPercent(float defVal);

    float getOverbetFrequency(float defVal);

    float getBetFoldFrequency(float defVal);

    float getHandsRaisedPreFlopPercent(float defVal);

    // Transient per-hand flag
    boolean isOverbetPotPostFlop();

    void setOverbetPotPostFlop(boolean value);
}
