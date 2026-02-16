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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.core.ai.StrategyProvider;
import com.donohoedigital.games.poker.engine.Hand;

import java.util.HashMap;
import java.util.Map;

/**
 * Desktop implementation of StrategyProvider that wraps PlayerType.
 * <p>
 * Delegates strategy factor queries to the PlayerType system and adds
 * per-player random modifiers to simulate personality variance.
 */
public class ClientStrategyProvider implements StrategyProvider {

    private final PlayerType playerType;
    private final Map<String, Float> randomModifiers = new HashMap<>();
    private final HandSelectionScheme handSelection;

    /**
     * Create strategy provider for given player type.
     *
     * @param playerType
     *            the player type (AI profile)
     */
    public ClientStrategyProvider(PlayerType playerType) {
        this.playerType = playerType;
        this.handSelection = playerType.getHandSelectionFull();
    }

    @Override
    public float getStratFactor(String name, float min, float max) {
        // Get base value from player type
        float base = playerType.getStratFactor(name, min, max);

        // Apply per-player random modifier (consistent for this player/factor)
        Float modifier = randomModifiers.get(name);
        if (modifier == null) {
            // Generate random modifier once per factor
            modifier = (float) (Math.random() * 0.1f - 0.05f); // ±5% variance
            randomModifiers.put(name, modifier);
        }

        return Math.min(Math.max(base + modifier, min), max);
    }

    @Override
    public float getStratFactor(String name, Hand hand, float min, float max) {
        // PlayerType's getStratFactor needs an int mod parameter
        // Use 0 for no modification
        return playerType.getStratFactor(name, hand, min, max, 0);
    }

    @Override
    public float getHandStrength(Hand pocket) {
        // Use hand selection scheme for pre-flop hand strength
        return handSelection != null ? handSelection.getHandStrength(pocket) : 0.5f;
    }

    @Override
    public float getHandStrength(Hand pocket, int numPlayers) {
        // Select appropriate scheme based on table size
        HandSelectionScheme scheme;
        if (numPlayers <= 2) {
            scheme = playerType.getHandSelectionHup();
        } else if (numPlayers <= 4) {
            scheme = playerType.getHandSelectionVeryShort();
        } else if (numPlayers <= 6) {
            scheme = playerType.getHandSelectionShort();
        } else {
            scheme = playerType.getHandSelectionFull();
        }
        return scheme != null ? scheme.getHandStrength(pocket) : 0.5f;
    }
}
