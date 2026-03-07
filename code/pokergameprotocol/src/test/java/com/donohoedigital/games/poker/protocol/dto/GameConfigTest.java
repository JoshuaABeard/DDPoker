/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
package com.donohoedigital.games.poker.protocol.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class GameConfigTest {

    private static final GameConfig.BlindLevel LEVEL = new GameConfig.BlindLevel(10, 20, 0, 15, false,
            "NOLIMIT_HOLDEM");

    private GameConfig validConfig() {
        return new GameConfig("Test", null, null, 10, 90, true, 100, 1000, List.of(LEVEL), true, "NOLIMIT_HOLDEM",
                GameConfig.LevelAdvanceMode.TIME, 10, 15, null, null, null, null, null, null, null, null, null, null,
                null, false, false, null, null, null);
    }

    @Test
    void validate_validConfig_noException() {
        assertDoesNotThrow(() -> validConfig().validate());
    }

    @Test
    void validate_maxPlayersTooLow_throws() {
        GameConfig config = validConfig().withMaxPlayers(1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(ex.getMessage().contains("maxPlayers"));
    }

    @Test
    void validate_maxPlayersTooHigh_throws() {
        GameConfig config = validConfig().withMaxPlayers(5626);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(ex.getMessage().contains("maxPlayers"));
    }

    @Test
    void validate_maxPlayersBoundary_min() {
        GameConfig config = validConfig().withMaxPlayers(2);
        assertDoesNotThrow(config::validate);
    }

    @Test
    void validate_maxPlayersBoundary_max() {
        GameConfig config = validConfig().withMaxPlayers(5625);
        assertDoesNotThrow(config::validate);
    }

    @Test
    void validate_startingChipsTooLow_throws() {
        GameConfig config = new GameConfig("Test", null, null, 10, 90, true, 100, 0, List.of(LEVEL), true,
                "NOLIMIT_HOLDEM", GameConfig.LevelAdvanceMode.TIME, 10, 15, null, null, null, null, null, null, null,
                null, null, null, null, false, false, null, null, null);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void validate_startingChipsTooHigh_throws() {
        GameConfig config = new GameConfig("Test", null, null, 10, 90, true, 100, 1_000_001, List.of(LEVEL), true,
                "NOLIMIT_HOLDEM", GameConfig.LevelAdvanceMode.TIME, 10, 15, null, null, null, null, null, null, null,
                null, null, null, null, false, false, null, null, null);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void validate_nullBlindStructure_throws() {
        GameConfig config = new GameConfig("Test", null, null, 10, 90, true, 100, 1000, null, true, "NOLIMIT_HOLDEM",
                GameConfig.LevelAdvanceMode.TIME, 10, 15, null, null, null, null, null, null, null, null, null, null,
                null, false, false, null, null, null);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void validate_emptyBlindStructure_throws() {
        GameConfig config = validConfig().withBlindStructure(List.of());
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void validate_onlyBreakLevels_throws() {
        GameConfig.BlindLevel breakLevel = new GameConfig.BlindLevel(0, 0, 0, 10, true, null);
        GameConfig config = validConfig().withBlindStructure(List.of(breakLevel));
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void validate_aiSkillTooLow_throws() {
        GameConfig config = validConfig().withAiPlayers(List.of(new GameConfig.AIPlayerConfig("Bot", 0)));
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void validate_aiSkillTooHigh_throws() {
        GameConfig config = validConfig().withAiPlayers(List.of(new GameConfig.AIPlayerConfig("Bot", 8)));
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void validate_nullAiPlayers_noException() {
        GameConfig config = validConfig().withAiPlayers(null);
        assertDoesNotThrow(config::validate);
    }

    @Test
    void validate_validAiSkillBoundaries() {
        GameConfig config1 = validConfig().withAiPlayers(List.of(new GameConfig.AIPlayerConfig("Bot", 1)));
        assertDoesNotThrow(config1::validate);

        GameConfig config7 = validConfig().withAiPlayers(List.of(new GameConfig.AIPlayerConfig("Bot", 7)));
        assertDoesNotThrow(config7::validate);
    }

    // with* method tests

    @Test
    void withBlindStructure_replacesBlindStructure() {
        GameConfig original = validConfig();
        GameConfig.BlindLevel newLevel = new GameConfig.BlindLevel(50, 100, 10, 20, false, "POTLIMIT_HOLDEM");
        GameConfig modified = original.withBlindStructure(List.of(newLevel));

        assertEquals(List.of(newLevel), modified.blindStructure());
        assertEquals(original.name(), modified.name());
        assertEquals(original.maxPlayers(), modified.maxPlayers());
    }

    @Test
    void withMaxPlayers_replacesMaxPlayers() {
        GameConfig modified = validConfig().withMaxPlayers(50);
        assertEquals(50, modified.maxPlayers());
    }

    @Test
    void withAiPlayers_replacesAiPlayers() {
        List<GameConfig.AIPlayerConfig> ais = List.of(new GameConfig.AIPlayerConfig("Bot1", 3));
        GameConfig modified = validConfig().withAiPlayers(ais);
        assertEquals(ais, modified.aiPlayers());
    }

    @Test
    void withHumanDisplayName_replacesName() {
        GameConfig modified = validConfig().withHumanDisplayName("CustomName");
        assertEquals("CustomName", modified.humanDisplayName());
    }

    @Test
    void withPracticeConfig_replacesPracticeConfig() {
        GameConfig.PracticeConfig pc = new GameConfig.PracticeConfig(500, 2000, 1000, true, false, false, true);
        GameConfig modified = validConfig().withPracticeConfig(pc);
        assertEquals(pc, modified.practiceConfig());
    }
}
