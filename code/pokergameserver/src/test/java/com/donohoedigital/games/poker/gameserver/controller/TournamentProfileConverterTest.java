/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.controller;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.AIPlayerConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.BlindLevel;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TournamentProfileConverter}.
 */
class TournamentProfileConverterTest {

    private TournamentProfileConverter converter;
    private TournamentProfile profile;

    @BeforeEach
    void setUp() {
        converter = new TournamentProfileConverter();
        profile = new TournamentProfile("Test Tournament");
        profile.setNumPlayers(9);
        profile.setBuyinChips(5000);
        profile.setBuyin(100);
        // Add a two-level blind structure
        profile.setLevel(1, 0, 10, 20, 15); // no ante, 10/20, 15 min
        profile.setLevel(2, 5, 25, 50, 15); // ante=5, 25/50, 15 min
        profile.fixLevels();
    }

    @Test
    void basicFieldsAreMapped() {
        GameConfig config = converter.convert(profile);

        assertThat(config.name()).isEqualTo("Test Tournament");
        assertThat(config.maxPlayers()).isEqualTo(9);
        assertThat(config.startingChips()).isEqualTo(5000);
        assertThat(config.buyIn()).isEqualTo(100);
        assertThat(config.fillComputer()).isTrue();
        assertThat(config.doubleAfterLastLevel()).isTrue();
    }

    @Test
    void blindStructureHasTwoLevels() {
        GameConfig config = converter.convert(profile);

        List<BlindLevel> blinds = config.blindStructure();
        assertThat(blinds).hasSize(2);

        BlindLevel level1 = blinds.get(0);
        assertThat(level1.smallBlind()).isEqualTo(10);
        assertThat(level1.bigBlind()).isEqualTo(20);
        assertThat(level1.ante()).isEqualTo(0);
        assertThat(level1.isBreak()).isFalse();
        assertThat(level1.gameType()).isEqualTo("NOLIMIT_HOLDEM");

        BlindLevel level2 = blinds.get(1);
        assertThat(level2.smallBlind()).isEqualTo(25);
        assertThat(level2.bigBlind()).isEqualTo(50);
        assertThat(level2.ante()).isEqualTo(5);
        assertThat(level2.isBreak()).isFalse();
    }

    @Test
    void breakLevelIsConvertedCorrectly() {
        profile.setBreak(3, 10); // 10-minute break at level 3
        profile.fixLevels();

        GameConfig config = converter.convert(profile);
        List<BlindLevel> blinds = config.blindStructure();
        assertThat(blinds).hasSize(3);

        BlindLevel breakLevel = blinds.get(2);
        assertThat(breakLevel.isBreak()).isTrue();
        assertThat(breakLevel.minutes()).isEqualTo(10);
        assertThat(breakLevel.gameType()).isNull();
    }

    @Test
    void rebuyConfigNullWhenDisabled() {
        profile.setRebuys(false);
        GameConfig config = converter.convert(profile);
        assertThat(config.rebuys()).isNull();
    }

    @Test
    void rebuyConfigMappedWhenEnabled() {
        profile.setRebuys(true);
        profile.getMap().setInteger(TournamentProfile.PARAM_REBUYCOST, 50);
        GameConfig config = converter.convert(profile);

        assertThat(config.rebuys()).isNotNull();
        assertThat(config.rebuys().enabled()).isTrue();
        assertThat(config.rebuys().cost()).isEqualTo(50);
    }

    @Test
    void addonConfigNullWhenDisabled() {
        profile.setAddons(false);
        GameConfig config = converter.convert(profile);
        assertThat(config.addons()).isNull();
    }

    @Test
    void addonConfigMappedWhenEnabled() {
        profile.setAddons(true);
        GameConfig config = converter.convert(profile);

        assertThat(config.addons()).isNotNull();
        assertThat(config.addons().enabled()).isTrue();
    }

    @Test
    void bountyConfigNullWhenDisabled() {
        profile.setBountyEnabled(false);
        GameConfig config = converter.convert(profile);
        assertThat(config.bounty()).isNull();
    }

    @Test
    void bountyConfigMappedWhenEnabled() {
        profile.setBountyEnabled(true);
        profile.setBountyAmount(500);
        GameConfig config = converter.convert(profile);

        assertThat(config.bounty()).isNotNull();
        assertThat(config.bounty().enabled()).isTrue();
        assertThat(config.bounty().amount()).isEqualTo(500);
    }

    @Test
    void timeoutsMapped() {
        profile.setTimeoutSeconds(45);
        GameConfig config = converter.convert(profile);

        assertThat(config.timeouts().defaultSeconds()).isEqualTo(45);
    }

    @Test
    void bootConfigMapped() {
        profile.getMap().setBoolean(TournamentProfile.PARAM_BOOT_SITOUT, Boolean.TRUE);
        GameConfig config = converter.convert(profile);

        assertThat(config.boot().bootSitout()).isTrue();
        assertThat(config.boot().bootDisconnect()).isTrue(); // default
    }

    @Test
    void lateRegistrationNullWhenDisabled() {
        profile.setLateRegEnabled(false);
        GameConfig config = converter.convert(profile);
        assertThat(config.lateRegistration()).isNull();
    }

    @Test
    void lateRegistrationMappedWhenEnabled() {
        profile.setLateRegEnabled(true);
        profile.setLateRegUntilLevel(3);
        GameConfig config = converter.convert(profile);

        assertThat(config.lateRegistration()).isNotNull();
        assertThat(config.lateRegistration().enabled()).isTrue();
        assertThat(config.lateRegistration().untilLevel()).isEqualTo(3);
    }

    @Test
    void scheduledStartNullWhenDisabled() {
        profile.setScheduledStartEnabled(false);
        GameConfig config = converter.convert(profile);
        assertThat(config.scheduledStart()).isNull();
    }

    @Test
    void inviteConfigNullWhenNotInviteOnly() {
        profile.setInviteOnly(false);
        GameConfig config = converter.convert(profile);
        assertThat(config.invite()).isNull();
    }

    @Test
    void inviteConfigMappedWhenInviteOnly() {
        profile.setInviteOnly(true);
        GameConfig config = converter.convert(profile);

        assertThat(config.invite()).isNotNull();
        assertThat(config.invite().inviteOnly()).isTrue();
    }

    @Test
    void bettingConfigMapped() {
        GameConfig config = converter.convert(profile);
        // Default: no max raises limit, heads-up cap ignored
        assertThat(config.betting()).isNotNull();
        assertThat(config.betting().raiseCapIgnoredHeadsUp()).isTrue();
    }

    @Test
    void levelAdvanceModeMappedTime() {
        profile.setLevelAdvanceMode(com.donohoedigital.games.poker.model.LevelAdvanceMode.TIME);
        GameConfig config = converter.convert(profile);
        assertThat(config.levelAdvanceMode()).isEqualTo(GameConfig.LevelAdvanceMode.TIME);
    }

    @Test
    void levelAdvanceModeMappedHands() {
        profile.setLevelAdvanceMode(com.donohoedigital.games.poker.model.LevelAdvanceMode.HANDS);
        profile.setHandsPerLevel(20);
        GameConfig config = converter.convert(profile);
        assertThat(config.levelAdvanceMode()).isEqualTo(GameConfig.LevelAdvanceMode.HANDS);
        assertThat(config.handsPerLevel()).isEqualTo(20);
    }

    @Test
    void greetingNullWhenEmpty() {
        GameConfig config = converter.convert(profile);
        assertThat(config.greeting()).isNull();
    }

    @Test
    void aiPlayersNullAfterBasicConvert() {
        GameConfig config = converter.convert(profile);
        assertThat(config.aiPlayers()).isNull();
    }

    @Test
    void buildAiPlayersCreatesCorrectCount() {
        List<AIPlayerConfig> ais = converter.buildAiPlayers(profile, List.of("Bot1", "Bot2", "Bot3"), 4);

        assertThat(ais).hasSize(3);
        assertThat(ais.get(0).name()).isEqualTo("Bot1");
        assertThat(ais.get(1).name()).isEqualTo("Bot2");
        assertThat(ais.get(2).name()).isEqualTo("Bot3");
        assertThat(ais.get(0).skillLevel()).isEqualTo(4);
    }

    @Test
    void buildAiPlayersEmptyListReturnsEmpty() {
        List<AIPlayerConfig> ais = converter.buildAiPlayers(profile, List.of(), 4);
        assertThat(ais).isEmpty();
    }

    @Test
    void withAiPlayersWiresCorrectly() {
        List<AIPlayerConfig> ais = converter.buildAiPlayers(profile, List.of("Bot1"), 3);
        GameConfig config = converter.convert(profile).withAiPlayers(ais);

        assertThat(config.aiPlayers()).hasSize(1);
        assertThat(config.aiPlayers().get(0).name()).isEqualTo("Bot1");
        assertThat(config.aiPlayers().get(0).skillLevel()).isEqualTo(3);
    }

    @Test
    void houseConfigNullWhenNoHouseTake() {
        // Default profile has no house take
        GameConfig config = converter.convert(profile);
        assertThat(config.house()).isNull();
    }

    @Test
    void potLimitGameTypeMapped() {
        // Set all levels to pot-limit
        profile.setLevel(1, 0, 10, 20, 15);
        profile.setLevel(2, 0, 25, 50, 15);
        // Directly override game type string
        profile.getMap().setString(com.donohoedigital.games.poker.engine.PokerConstants.DE_POT_LIMIT_HOLDEM,
                com.donohoedigital.games.poker.engine.PokerConstants.DE_POT_LIMIT_HOLDEM);
        // Use default game type
        profile.getMap().setString(TournamentProfile.PARAM_GAMETYPE_DEFAULT,
                com.donohoedigital.games.poker.engine.PokerConstants.DE_POT_LIMIT_HOLDEM);
        profile.fixLevels();

        GameConfig config = converter.convert(profile);
        assertThat(config.defaultGameType()).isEqualTo("POTLIMIT_HOLDEM");
    }
}
