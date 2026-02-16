/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds strategy factor data loaded from PlayerType .dat files. Provides
 * Swing-free access to AI personality configuration.
 */
public class StrategyData {

    private final String name;
    private final String description;
    private final Map<String, Integer> strategyFactors = new HashMap<>();
    private final Map<String, String> handSelectionSchemes = new HashMap<>();

    public StrategyData(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Set a strategy factor value.
     *
     * @param key
     *            Factor name (e.g., "basics.aggression")
     * @param value
     *            Integer value 0-100
     */
    public void setStrategyFactor(String key, int value) {
        strategyFactors.put(key, value);
    }

    /**
     * Get a strategy factor value with hierarchical fallback. If specific key not
     * found, tries parent keys by stripping rightmost segment.
     *
     * @param key
     *            Factor name (e.g., "basics.aggression.suited_ace")
     * @param defaultValue
     *            Default if not found
     * @return Strategy value 0-100
     */
    public int getStrategyFactor(String key, int defaultValue) {
        Integer value = strategyFactors.get(key);
        if (value != null) {
            return value;
        }

        // Try parent key by stripping last segment
        int lastDot = key.lastIndexOf('.');
        if (lastDot > 0) {
            String parentKey = key.substring(0, lastDot);
            return getStrategyFactor(parentKey, defaultValue);
        }

        return defaultValue;
    }

    /**
     * Set hand selection scheme reference (resource path to scheme data).
     *
     * @param tableSize
     *            "full", "short", "veryshort", or "hup"
     * @param schemePath
     *            Resource path to scheme data
     */
    public void setHandSelectionScheme(String tableSize, String schemePath) {
        handSelectionSchemes.put(tableSize, schemePath);
    }

    /**
     * Get hand selection scheme path for table size.
     *
     * @param tableSize
     *            "full", "short", "veryshort", or "hup"
     * @return Resource path or null if not set
     */
    public String getHandSelectionScheme(String tableSize) {
        return handSelectionSchemes.get(tableSize);
    }

    /**
     * Check if this strategy has all required data loaded.
     */
    public boolean isComplete() {
        return !strategyFactors.isEmpty();
    }
}
