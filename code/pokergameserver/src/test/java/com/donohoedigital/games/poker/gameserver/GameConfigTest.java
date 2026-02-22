/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.gameserver.GameConfig.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Tests for {@link GameConfig} - the clean server-side tournament configuration
 * model.
 *
 * <p>
 * Tests JSON serialization, validation, and default values.
 * </p>
 */
class GameConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void testMinimalConfigSerializationRoundTrip() throws Exception {
        // Minimal valid config with only required fields
        GameConfig config = new GameConfig("Test Tournament", "A test tournament", "Welcome ${name}!", 10, // maxPlayers
                90, // maxOnlinePlayers
                true, // fillComputer
                1000, // buyIn
                5000, // startingChips
                List.of(new BlindLevel(25, 50, 0, 20, false, "NOLIMIT_HOLDEM"),
                        new BlindLevel(50, 100, 10, 20, false, "NOLIMIT_HOLDEM")),
                true, // doubleAfterLastLevel
                "NOLIMIT_HOLDEM", // defaultGameType
                LevelAdvanceMode.TIME, 10, // handsPerLevel
                20, // defaultMinutesPerLevel
                null, // rebuys (disabled)
                null, // addons (disabled)
                new PayoutConfig("SPOTS", 3, 0, 0, "AUTO", List.of()), null, // house (no take)
                null, // bounty (disabled)
                new TimeoutConfig(30, 0, 0, 0, 0, 15), new BootConfig(true, 25, true, 10), null, // late registration
                                                                                                    // (disabled)
                null, // scheduled start (disabled)
                new InviteConfig(false, List.of(), true), new BettingConfig(0, true), false, // allowDash
                false, // allowAdvisor
                List.of(), // no AI players
                null, // humanDisplayName
                null // practiceConfig (disabled)
        );

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(config);

        // Deserialize back
        GameConfig deserialized = objectMapper.readValue(json, GameConfig.class);

        // Verify round-trip
        assertThat(deserialized).isEqualTo(config);
        assertThat(deserialized.name()).isEqualTo("Test Tournament");
        assertThat(deserialized.blindStructure()).hasSize(2);
        assertThat(deserialized.blindStructure().get(0).smallBlind()).isEqualTo(25);
    }

    @Test
    void testFullConfigWithAllFeatures() throws Exception {
        GameConfig config = new GameConfig("Friday Night Poker", "Weekly tournament", "Good luck ${name}!", 10, 90,
                true, 1000, 5000,
                List.of(new BlindLevel(25, 50, 0, 20, false, "NOLIMIT_HOLDEM"),
                        new BlindLevel(50, 100, 10, 20, false, "NOLIMIT_HOLDEM"),
                        new BlindLevel(0, 0, 0, 5, true, null), // Break
                        new BlindLevel(100, 200, 25, 20, false, "NOLIMIT_HOLDEM")),
                true, "NOLIMIT_HOLDEM", LevelAdvanceMode.TIME, 10, 20,
                new RebuyConfig(true, 1000, 5000, 5000, 3, 2, "LESS_THAN"), new AddonConfig(true, 1000, 3000, 3),
                new PayoutConfig("SPOTS", 3, 0, 9000, "AUTO", List.of()), new HouseConfig("PERCENT", 10, 0),
                new BountyConfig(true, 100), new TimeoutConfig(30, 0, 0, 0, 0, 15), new BootConfig(true, 25, true, 10),
                new LateRegistrationConfig(true, 3, "STARTING"),
                new ScheduledStartConfig(true, Instant.parse("2026-02-16T20:00:00Z"), 4),
                new InviteConfig(false, List.of(), true), new BettingConfig(0, true), false, false,
                List.of(new AIPlayerConfig("Bot-Easy", 2), new AIPlayerConfig("Bot-Medium", 4),
                        new AIPlayerConfig("Bot-Hard", 6)),
                null, // humanDisplayName
                null); // practiceConfig

        String json = objectMapper.writeValueAsString(config);
        GameConfig deserialized = objectMapper.readValue(json, GameConfig.class);

        assertThat(deserialized).isEqualTo(config);
        assertThat(deserialized.rebuys().enabled()).isTrue();
        assertThat(deserialized.addons().enabled()).isTrue();
        assertThat(deserialized.bounty().enabled()).isTrue();
        assertThat(deserialized.aiPlayers()).hasSize(3);
    }

    @Test
    void testBlindStructureWithBreaks() throws Exception {
        List<BlindLevel> blinds = List.of(new BlindLevel(25, 50, 0, 20, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(0, 0, 0, 5, true, null), // Break indicated by isBreak=true
                new BlindLevel(50, 100, 10, 20, false, "NOLIMIT_HOLDEM"));

        GameConfig config = minimalConfig().withBlindStructure(blinds);
        String json = objectMapper.writeValueAsString(config);
        GameConfig deserialized = objectMapper.readValue(json, GameConfig.class);

        assertThat(deserialized.blindStructure()).hasSize(3);
        assertThat(deserialized.blindStructure().get(1).isBreak()).isTrue();
        assertThat(deserialized.blindStructure().get(1).gameType()).isNull();
    }

    @Test
    void testValidationRejectsInvalidMaxPlayers() {
        GameConfig config = minimalConfig().withMaxPlayers(1); // Too low

        assertThatThrownBy(() -> config.validate()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxPlayers must be between 2 and 5625");
    }

    @Test
    void testValidationRejectsEmptyBlindStructure() {
        GameConfig config = minimalConfig().withBlindStructure(List.of());

        assertThatThrownBy(() -> config.validate()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blind structure must have at least one non-break level");
    }

    @Test
    void testValidationRejectsInvalidAISkillLevel() {
        GameConfig config = minimalConfig().withAiPlayers(List.of(new AIPlayerConfig("Bot", 0)) // Skill level too low
        );

        assertThatThrownBy(() -> config.validate()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AI skill level must be between 1 and 7");
    }

    @Test
    void testNullOptionalConfigsHandledGracefully() throws Exception {
        GameConfig config = new GameConfig("Test", "Description", "Hi!", 10, 90, true, 1000, 5000,
                List.of(new BlindLevel(25, 50, 0, 20, false, "NOLIMIT_HOLDEM")), true, "NOLIMIT_HOLDEM",
                LevelAdvanceMode.TIME, 10, 20, null, null, // rebuys, addons
                new PayoutConfig("SPOTS", 3, 0, 0, "AUTO", List.of()), null, null, // house, bounty
                new TimeoutConfig(30, 0, 0, 0, 0, 15), new BootConfig(true, 25, true, 10), null, null, // late reg,
                                                                                                        // scheduled
                                                                                                        // start
                new InviteConfig(false, List.of(), true), new BettingConfig(0, true), false, false, List.of(), null, // humanDisplayName
                null); // practiceConfig

        String json = objectMapper.writeValueAsString(config);
        GameConfig deserialized = objectMapper.readValue(json, GameConfig.class);

        assertThat(deserialized.rebuys()).isNull();
        assertThat(deserialized.addons()).isNull();
        assertThat(deserialized.house()).isNull();
        assertThat(deserialized.bounty()).isNull();
    }

    /**
     * Helper to create a minimal valid config for testing modifications.
     */
    private GameConfig minimalConfig() {
        return new GameConfig("Test", "Description", "Welcome!", 10, 90, true, 1000, 5000,
                List.of(new BlindLevel(25, 50, 0, 20, false, "NOLIMIT_HOLDEM")), true, "NOLIMIT_HOLDEM",
                LevelAdvanceMode.TIME, 10, 20, null, null, new PayoutConfig("SPOTS", 3, 0, 0, "AUTO", List.of()), null,
                null, new TimeoutConfig(30, 0, 0, 0, 0, 15), new BootConfig(true, 25, true, 10), null, null,
                new InviteConfig(false, List.of(), true), new BettingConfig(0, true), false, false, List.of(), null, // humanDisplayName
                null); // practiceConfig
    }
}
