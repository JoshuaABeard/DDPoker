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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads PlayerType strategy data from .dat files without Swing dependencies.
 * Parses the DMTypedHashMap serialization format used by DD Poker.
 */
public class StrategyDataLoader {

    private static final Logger logger = Logger.getLogger(StrategyDataLoader.class.getName());
    private static final String RESOURCE_PATH = "/save/poker/playertypes/";

    // Cache of loaded strategies
    private static final Map<String, StrategyData> cache = new HashMap<>();
    private static List<String> availableStrategyFiles;

    /**
     * Get list of available strategy file names from resources.
     */
    public static synchronized List<String> getAvailableStrategies() {
        if (availableStrategyFiles == null) {
            availableStrategyFiles = new ArrayList<>();

            // DD Poker has playertype files numbered 0991-1000 (and potentially more)
            // Try to load known ranges
            for (int i = 991; i <= 1000; i++) {
                String filename = "playertype." + i + ".dat";
                String resourcePath = RESOURCE_PATH + filename;

                if (StrategyDataLoader.class.getResource(resourcePath) != null) {
                    availableStrategyFiles.add(filename);
                }
            }

            logger.info("Found " + availableStrategyFiles.size() + " strategy files");
        }

        return availableStrategyFiles;
    }

    /**
     * Load strategy data from .dat file.
     *
     * @param filename
     *            Filename (e.g., "playertype.0991.dat")
     * @return Loaded strategy data, or null if file not found/parseable
     */
    public static synchronized StrategyData loadStrategy(String filename) {
        // Check cache first
        if (cache.containsKey(filename)) {
            return cache.get(filename);
        }

        String resourcePath = RESOURCE_PATH + filename;
        InputStream is = StrategyDataLoader.class.getResourceAsStream(resourcePath);

        if (is == null) {
            logger.warning("Strategy file not found: " + resourcePath);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StrategyData data = parseStrategyFile(reader);
            if (data != null && data.isComplete()) {
                cache.put(filename, data);
                logger.info("Loaded strategy: " + data.getName() + " from " + filename);
                return data;
            } else {
                logger.warning("Failed to parse strategy file: " + filename);
                return null;
            }
        } catch (IOException e) {
            logger.warning("Error reading strategy file " + filename + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse .dat file format. Format is two lines: Line 1: name:id:flags:filename
     * Line 2: $key\=value:$key\=value:...
     */
    private static StrategyData parseStrategyFile(BufferedReader reader) throws IOException {
        // Line 1: metadata
        String metadataLine = reader.readLine();
        if (metadataLine == null) {
            return null;
        }

        String[] metadata = metadataLine.split(":");
        String name = metadata.length > 0 ? metadata[0].substring(1) : "Unknown"; // Strip leading 's'

        // Line 2: key-value pairs
        String dataLine = reader.readLine();
        if (dataLine == null) {
            return null;
        }

        StrategyData data = new StrategyData(name, "");

        // Parse key-value pairs separated by ':'
        // Each pair is: $key\=typeValue where type is i/s/l/b
        String[] pairs = dataLine.split(":");

        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }

            // Split on \= (escaped equals)
            String[] parts = pair.split("\\\\=", 2);
            if (parts.length != 2) {
                continue;
            }

            String key = parts[0];
            String value = parts[1];

            // Remove $ prefix from key
            if (key.startsWith("$")) {
                key = key.substring(1);
            }

            // Parse value based on type prefix
            if (value.isEmpty()) {
                continue;
            }

            char type = value.charAt(0);
            String valueStr = value.substring(1);

            try {
                if (key.startsWith("strat.")) {
                    // Strategy factor
                    if (type == 'i') {
                        // Integer value
                        int intValue = Integer.parseInt(valueStr);
                        // Strip "strat." prefix for cleaner keys
                        String factorKey = key.substring(6);
                        data.setStrategyFactor(factorKey, intValue);
                    }
                } else if (key.equals("desc") && type == 's') {
                    // Description (stored in constructor, but we could update it here)
                } else if (key.startsWith("hs")) {
                    // Hand selection scheme reference
                    String tableSize = key.substring(2); // "full", "short", "vshort", "hup"
                    data.setHandSelectionScheme(tableSize, valueStr);
                }
            } catch (NumberFormatException e) {
                logger.warning("Failed to parse value for key " + key + ": " + valueStr);
            }
        }

        return data;
    }

    /**
     * Load default strategy (first available).
     */
    public static StrategyData loadDefaultStrategy() {
        List<String> available = getAvailableStrategies();
        if (available.isEmpty()) {
            logger.warning("No strategy files available!");
            return createFallbackStrategy();
        }

        StrategyData data = loadStrategy(available.get(0));
        return data != null ? data : createFallbackStrategy();
    }

    /**
     * Create fallback strategy with default values if no files can be loaded.
     */
    private static StrategyData createFallbackStrategy() {
        StrategyData data = new StrategyData("Default (Fallback)", "Medium-skilled balanced player");

        // Set reasonable defaults for common factors
        data.setStrategyFactor("basics.aggression", 50);
        data.setStrategyFactor("basics.tightness", 50);
        data.setStrategyFactor("basics.position", 50);
        data.setStrategyFactor("deception.bluff", 40);
        data.setStrategyFactor("handselection", 50);

        logger.info("Using fallback strategy with default values");
        return data;
    }
}
