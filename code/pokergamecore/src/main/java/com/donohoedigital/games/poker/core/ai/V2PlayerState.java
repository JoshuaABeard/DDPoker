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
 * Read-only interface for V2-specific per-player computed state used by
 * PureRuleEngine. V2Algorithm implements this; the desktop V2Player wrapper
 * provides an adapter.
 *
 * <p>
 * Hand strength values are pre-computed by the algorithm's computeOdds() and
 * cached per action. Volatile state (steam, steal suspicion) persists across
 * hands.
 */
public interface V2PlayerState {

    /** Tilt factor from bad beats (0 = calm, higher = tilted). */
    float getSteam();

    /** Estimated probability the current raise is a steal attempt. */
    float getStealSuspicion();

    /** Pre-flop hand selection strength from HandSelectionScheme. */
    float getHandStrength();

    /** Raw hand strength from PocketRanks (unbiased, 0-1). */
    float getRawHandStrength();

    /** Hand strength biased by opponent modeling (0-1). */
    float getBiasedHandStrength();

    /** Biased positive potential (chance to improve). */
    float getBiasedPositivePotential();

    /** Biased negative potential (chance to be outdrawn). */
    float getBiasedNegativePotential();

    /** Unbiased positive potential (for debug display). */
    float getPositiveHandPotential();

    /** Unbiased negative potential (for debug display). */
    float getNegativeHandPotential();

    /**
     * Effective hand strength adjusted by pot odds scaling.
     *
     * @param scaledPotOdds
     *            pot odds multiplied by pot odds strategy factor
     */
    float getBiasedEffectiveHandStrength(float scaledPotOdds);

    /** Whether debug output is enabled for this player. */
    boolean debugEnabled();
}
