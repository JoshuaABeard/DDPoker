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
package com.donohoedigital.games.poker.model;

import java.util.*;

/**
 * Validates and normalizes tournament blind level structure.
 *
 * <p>
 * Performs critical validations and transformations:
 * <ol>
 * <li><strong>Gap consolidation:</strong> Levels 1,3,5 become 1,2,3
 * <li><strong>Missing blind fill-in:</strong> If big=0, uses small*2 or
 * previous level
 * <li><strong>Monotonic enforcement:</strong> Blinds must increase (or stay
 * same) each level
 * <li><strong>Ante bounds:</strong> 5% minimum, 100% maximum of small blind
 * <li><strong>Rounding:</strong> Amounts rounded to clean increments (5, 25,
 * 100, etc.)
 * <li><strong>Break level handling:</strong> Special ante=-1 indicates break
 * <li><strong>Default propagation:</strong> Missing game types and minutes use
 * defaults
 * <li><strong>Safety net:</strong> If no non-break levels exist, creates
 * default level 1
 * </ol>
 *
 * <p>
 * Extracted from TournamentProfile.fixLevels() to improve testability.
 *
 * @see TournamentProfile#fixLevels()
 */
public class LevelValidator {

    /**
     * Represents a single blind level's data.
     */
    public static class LevelData {
        public int levelNum;
        public int ante;
        public int smallBlind;
        public int bigBlind;
        public int minutes;
        public String gameType;
        public boolean isBreak;

        public LevelData(int levelNum) {
            this.levelNum = levelNum;
        }

        @Override
        public String toString() {
            if (isBreak) {
                return String.format("Level %d: BREAK (%d min)", levelNum, minutes);
            }
            return String.format("Level %d: Ante=%d, Small=%d, Big=%d, Minutes=%d, Type=%s", levelNum, ante, smallBlind,
                    bigBlind, minutes, gameType);
        }
    }

    /**
     * Validate and normalize level data from raw string map.
     *
     * @param rawLevelData
     *            Map of parameter keys to string values (e.g., "small1" -> "10")
     * @param defaultMinutes
     *            Default minutes per level (used when not specified)
     * @param defaultGameType
     *            Default game type string (used when not specified)
     * @return Normalized list of LevelData, sorted by level number
     */
    public List<LevelData> validateAndNormalize(Map<String, String> rawLevelData, int defaultMinutes,
            String defaultGameType) {

        // Step 1: Parse raw data into LevelData objects
        List<LevelData> rawLevels = parseRawLevels(rawLevelData);

        // Step 2: If no non-break levels exist, create default level 1
        if (!hasNonBreakLevel(rawLevels)) {
            rawLevels.add(createDefaultLevel());
        }

        // Step 3: Sort by level number
        rawLevels.sort(Comparator.comparingInt(level -> level.levelNum));

        // Step 4: Consolidate gaps (renumber to sequential)
        List<LevelData> consolidated = consolidateGaps(rawLevels);

        // Step 5: Fill missing blinds from previous level
        fillMissingBlinds(consolidated);

        // Step 6: Validate and normalize each level
        normalizeLevels(consolidated, defaultMinutes, defaultGameType);

        return consolidated;
    }

    /**
     * Parse raw level data from string map into LevelData objects.
     */
    private List<LevelData> parseRawLevels(Map<String, String> rawData) {
        Map<Integer, LevelData> levelMap = new HashMap<>();

        // Scan for all level numbers that have data
        for (String key : rawData.keySet()) {
            String value = rawData.get(key);
            if (value == null || value.trim().isEmpty()) {
                continue;
            }

            // Extract level number from parameter key (e.g., "small15" -> 15)
            Integer levelNum = extractLevelNumber(key);
            if (levelNum == null || levelNum < 1 || levelNum > TournamentProfile.MAX_LEVELS) {
                continue;
            }

            // Get or create LevelData for this level number
            LevelData level = levelMap.computeIfAbsent(levelNum, LevelData::new);

            // Parse the value based on parameter type
            try {
                if (key.startsWith("ante")) {
                    level.ante = Integer.parseInt(value);
                    if (level.ante == TournamentProfile.BREAK_ANTE_VALUE) {
                        level.isBreak = true;
                    }
                } else if (key.startsWith("small")) {
                    level.smallBlind = Integer.parseInt(value);
                } else if (key.startsWith("big")) {
                    level.bigBlind = Integer.parseInt(value);
                } else if (key.startsWith("minutes")) {
                    level.minutes = Integer.parseInt(value);
                } else if (key.startsWith("gametype")) {
                    level.gameType = value;
                }
            } catch (NumberFormatException e) {
                // Invalid number - skip this parameter
            }
        }

        return new ArrayList<>(levelMap.values());
    }

    /**
     * Extract level number from parameter key (e.g., "small15" -> 15).
     */
    private Integer extractLevelNumber(String paramKey) {
        // Remove prefix (ante, small, big, minutes, gametype)
        String numberPart = paramKey.replaceAll("^(ante|small|big|minutes|gametype)", "");
        try {
            return Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Check if there's at least one non-break level.
     */
    private boolean hasNonBreakLevel(List<LevelData> levels) {
        for (LevelData level : levels) {
            if (!level.isBreak) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a default level 1 with standard blinds.
     */
    private LevelData createDefaultLevel() {
        LevelData level = new LevelData(1);
        level.smallBlind = 1;
        level.bigBlind = 2;
        level.ante = 0;
        level.minutes = 0; // Use default
        level.gameType = null; // Use default
        level.isBreak = false;
        return level;
    }

    /**
     * Consolidate gaps in level numbers (1,3,5 becomes 1,2,3).
     */
    private List<LevelData> consolidateGaps(List<LevelData> levels) {
        List<LevelData> consolidated = new ArrayList<>();
        int newLevelNum = 1;

        for (LevelData level : levels) {
            level.levelNum = newLevelNum++;
            consolidated.add(level);
        }

        return consolidated;
    }

    /**
     * Fill missing blinds from previous level or calculate from existing blind.
     */
    private void fillMissingBlinds(List<LevelData> levels) {
        LevelData previous = null;

        for (LevelData level : levels) {
            if (level.isBreak) {
                // Breaks don't have blinds
                level.smallBlind = 0;
                level.bigBlind = 0;
            } else {
                // Fill missing small blind
                if (level.smallBlind == 0 && previous != null && !previous.isBreak) {
                    level.smallBlind = previous.smallBlind;
                }

                // Fill missing big blind
                if (level.bigBlind == 0 && previous != null && !previous.isBreak) {
                    level.bigBlind = previous.bigBlind;
                }

                // For level 1, ensure we have both blinds
                if (level.levelNum == 1) {
                    if (level.smallBlind == 0 && level.bigBlind > 0) {
                        level.smallBlind = (level.bigBlind > 2) ? level.bigBlind / 2 : 1;
                    } else if (level.bigBlind == 0 && level.smallBlind > 0) {
                        level.bigBlind = level.smallBlind * 2;
                    }
                }
            }

            if (!level.isBreak) {
                previous = level;
            }
        }
    }

    /**
     * Validate and normalize all levels: monotonic blinds, ante bounds, rounding,
     * defaults.
     */
    private void normalizeLevels(List<LevelData> levels, int defaultMinutes, String defaultGameType) {
        LevelData previous = null;

        for (LevelData level : levels) {
            if (level.isBreak) {
                // Breaks only need minutes validation
                if (level.minutes > TournamentProfile.MAX_MINUTES) {
                    level.minutes = TournamentProfile.MAX_MINUTES;
                }
                continue;
            }

            // Enforce big >= small
            if (level.bigBlind < level.smallBlind) {
                level.bigBlind = level.smallBlind;
            }

            // Enforce monotonic increasing (except ante can go to 0)
            if (previous != null && !previous.isBreak) {
                if (level.ante < previous.ante && level.ante != 0) {
                    level.ante = previous.ante;
                }
                if (level.smallBlind < previous.smallBlind) {
                    level.smallBlind = previous.smallBlind;
                }
                if (level.bigBlind < previous.bigBlind) {
                    level.bigBlind = previous.bigBlind;
                }
            }

            // Enforce ante bounds (5% to 100% of small blind)
            if (level.ante != 0) {
                int minAnte = (int) (level.smallBlind * 0.05f);
                if (level.ante < minAnte) {
                    level.ante = minAnte;
                }
                if (level.ante > level.smallBlind) {
                    level.ante = level.smallBlind;
                }
            }

            // Round amounts to clean increments
            level.ante = round(level.ante);
            level.smallBlind = round(level.smallBlind);
            level.bigBlind = round(level.bigBlind);

            // Enforce max minutes
            if (level.minutes > TournamentProfile.MAX_MINUTES) {
                level.minutes = TournamentProfile.MAX_MINUTES;
            }

            // Normalize defaults: if matches default, store as 0/null to indicate "use
            // default"
            if (level.minutes == defaultMinutes) {
                level.minutes = 0;
            }
            if (defaultGameType != null && defaultGameType.equals(level.gameType)) {
                level.gameType = null;
            }

            previous = level;
        }
    }

    /**
     * Round ante/blind amounts to appropriate increments based on size.
     *
     * <p>
     * Rounding increments:
     * <ul>
     * <li>$1-100: Round to $1
     * <li>$101-500: Round to $5
     * <li>$501-1,000: Round to $25
     * <li>$1,001-10,000: Round to $100
     * <li>$10,001-100,000: Round to $1,000
     * <li>$100,001-1,000,000: Round to $10,000
     * <li>$1,000,000+: Round to $100,000
     * </ul>
     */
    private int round(int amount) {
        if (amount == 0) {
            return 0;
        }

        int increment;
        if (amount <= 100) {
            increment = 1;
        } else if (amount <= 500) {
            increment = 5;
        } else if (amount <= 1000) {
            increment = 25;
        } else if (amount <= 10000) {
            increment = 100;
        } else if (amount <= 100000) {
            increment = 1000;
        } else if (amount <= 1000000) {
            increment = 10000;
        } else {
            increment = 100000;
        }

        // Round up to nearest increment
        int remainder = amount % increment;
        if (remainder == 0) {
            return amount;
        } else {
            return amount - remainder + increment;
        }
    }
}
