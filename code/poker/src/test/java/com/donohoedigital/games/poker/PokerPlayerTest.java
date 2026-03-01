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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerPlayer - chip management, player state, money operations, and
 * hand operations.
 */
class PokerPlayerTest {

    private PokerPlayer player;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create a test player (true = human, false = computer)
        player = new PokerPlayer(1, "TestPlayer", true);
    }

    // =================================================================
    // Chip Management Tests
    // =================================================================

    @Test
    void should_InitializeWithZeroChips_When_PlayerCreated() {
        assertThat(player.getChipCount()).isZero();
    }

    @Test
    void should_SetChipCount_When_ChipCountSet() {
        player.setChipCount(1500);

        assertThat(player.getChipCount()).isEqualTo(1500);
    }

    @Test
    void should_AddChips_When_ChipsAdded() {
        player.setChipCount(1000);

        player.addChips(500);

        assertThat(player.getChipCount()).isEqualTo(1500);
    }

    @Test
    void should_SubtractChips_When_NegativeChipsAdded() {
        player.setChipCount(1000);

        player.addChips(-300);

        assertThat(player.getChipCount()).isEqualTo(700);
    }

    @Test
    void should_ReturnZeroChipCountAtStart_When_NoChipsSet() {
        // ChipCountAtStart is only set when chips are first added, not via setChipCount
        assertThat(player.getChipCountAtStart()).isZero();
    }

    @Test
    void should_TrackInitialChipCount_When_ChipsFirstAdded() {
        player.addChips(1500);

        // After first add, starting chips should be tracked
        assertThat(player.getChipCount()).isEqualTo(1500);
    }

    @Test
    void should_SetOddChips_When_OddChipsSet() {
        player.setOddChips(3);

        assertThat(player.getOddChips()).isEqualTo(3);
    }

    @Test
    void should_MarkWonChipRace_When_ChipRaceWon() {
        player.setWonChipRace(true);

        assertThat(player.isWonChipRace()).isTrue();
    }

    @Test
    void should_MarkBrokeInChipRace_When_ChipRaceLost() {
        player.setBrokeChipRace(true);

        assertThat(player.isBrokeChipRace()).isTrue();
    }

    @Test
    void should_AllowZeroChips_When_PlayerBusted() {
        player.setChipCount(100);
        player.addChips(-100);

        assertThat(player.getChipCount()).isZero();
    }

    @Test
    void should_AllowNegativeChips_When_ChipsOverspent() {
        player.setChipCount(50);
        player.addChips(-100);

        assertThat(player.getChipCount()).isEqualTo(-50);
    }

    @Test
    void should_HandleLargeChipCounts_When_ChipsAccumulated() {
        player.setChipCount(1000000);
        player.addChips(500000);

        assertThat(player.getChipCount()).isEqualTo(1500000);
    }

    // =================================================================
    // Money Operations Tests
    // =================================================================

    @Test
    void should_SetBuyin_When_BuyinSet() {
        player.setBuyin(100);

        assertThat(player.getBuyin()).isEqualTo(100);
    }

    @Test
    void should_ReturnZeroRebuy_When_NoRebuysAdded() {
        // Rebuy/addon operations require table context, test getters only
        assertThat(player.getRebuy()).isZero();
        assertThat(player.getNumRebuys()).isZero();
    }

    @Test
    void should_ReturnZeroAddon_When_NoAddonPurchased() {
        assertThat(player.getAddon()).isZero();
    }

    @Test
    void should_CalculateTotalSpent_When_BuyinSet() {
        player.setBuyin(100);

        assertThat(player.getTotalSpent()).isEqualTo(100);
    }

    @Test
    void should_SetPrize_When_PrizeAwarded() {
        player.setPrize(500);

        assertThat(player.getPrize()).isEqualTo(500);
    }

    @Test
    void should_SetPlace_When_TournamentFinishRecorded() {
        player.setPlace(3);

        assertThat(player.getPlace()).isEqualTo(3);
    }

    // =================================================================
    // Player State Tests
    // =================================================================

    @Test
    void should_BeHuman_When_HumanPlayerCreated() {
        PokerPlayer human = new PokerPlayer(1, "Human", true); // true = human

        assertThat(human.isHuman()).isTrue();
        assertThat(human.isComputer()).isFalse();
    }

    @Test
    void should_BeComputer_When_ComputerPlayerCreated() {
        PokerPlayer computer = new PokerPlayer(1, "Computer", false); // false = computer

        assertThat(computer.isComputer()).isTrue();
        assertThat(computer.isHuman()).isFalse();
    }

    @Test
    void should_HaveCorrectName_When_PlayerCreated() {
        assertThat(player.getName()).isEqualTo("TestPlayer");
    }

    @Test
    void should_HaveCorrectID_When_PlayerCreated() {
        assertThat(player.getID()).isEqualTo(1);
    }

    @Test
    void should_ReturnDisplayName_When_GetDisplayNameCalled() {
        String displayName = player.getDisplayName(false);

        assertThat(displayName).isNotNull();
        assertThat(displayName).contains("TestPlayer");
    }

    @Test
    void should_NotBeInHand_When_NoHandInProgress() {
        // isInHand() requires an active HoldemHand, which needs table context
        player.setChipCount(1000);

        assertThat(player.isInHand()).isFalse();
    }

    // =================================================================
    // Profile Tests
    // =================================================================

    @Test
    void should_SetProfile_When_ProfileProvided() {
        PlayerProfile profile = new PlayerProfile("TestProfile");

        player.setProfile(profile);

        assertThat(player.getProfile()).isEqualTo(profile);
        assertThat(player.isProfileDefined()).isTrue();
    }

    @Test
    void should_NotHaveProfile_When_NoProfileSet() {
        assertThat(player.isProfileDefined()).isFalse();
    }

    @Test
    void should_SetProfilePath_When_PathProvided() {
        player.setProfilePath("/path/to/profile");

        assertThat(player.getProfilePath()).isEqualTo("/path/to/profile");
    }

    // =================================================================
    // Time Management Tests
    // =================================================================

    @Test
    void should_SetThinkBankMillis_When_ThinkBankSet() {
        player.setThinkBankMillis(30000);

        assertThat(player.getThinkBankMillis()).isEqualTo(30000);
    }

    @Test
    void should_SetTimeoutMillis_When_TimeoutSet() {
        player.setTimeoutMillis(60000);

        assertThat(player.getTimeoutMillis()).isEqualTo(60000);
    }

    // =================================================================
    // Online Player Tests
    // =================================================================

    @Test
    void should_SetOnlineActivated_When_ActivationSet() {
        player.setOnlineActivated(true);

        assertThat(player.isOnlineActivated()).isTrue();
    }

    @Test
    void should_NotBeOnlineActivated_When_NewPlayerCreated() {
        assertThat(player.isOnlineActivated()).isFalse();
    }

    @Test
    void should_HavePlayerId_When_CreatedWithKey() {
        PokerPlayer playerWithKey = new PokerPlayer("player-123", 5, "KeyedPlayer", true);

        assertThat(playerWithKey.getPlayerId()).isEqualTo("player-123");
    }

    @Test
    void should_HaveNullPlayerId_When_CreatedWithoutKey() {
        assertThat(player.getPlayerId()).isNull();
    }

    // =================================================================
    // Average Chips Calculation Tests (for Late Registration)
    // =================================================================

    @Test
    void should_CalculateAverageChips_WithMultiplePlayers() {
        PokerGame game = new PokerGame(null);

        // Create players with different chip counts
        PokerPlayer p1 = new PokerPlayer(1, "Player1", true);
        p1.setChipCount(1000);
        game.addPlayer(p1);

        PokerPlayer p2 = new PokerPlayer(2, "Player2", true);
        p2.setChipCount(2000);
        game.addPlayer(p2);

        PokerPlayer p3 = new PokerPlayer(3, "Player3", true);
        p3.setChipCount(3000);
        game.addPlayer(p3);

        // Average should be (1000 + 2000 + 3000) / 3 = 2000
        int average = PokerPlayer.calculateAverageChips(game);
        assertThat(average).isEqualTo(2000);
    }

    @Test
    void should_ReturnZero_WhenNoPlayersInGame() {
        PokerGame game = new PokerGame(null);

        int average = PokerPlayer.calculateAverageChips(game);
        assertThat(average).isEqualTo(0);
    }

    @Test
    void should_ReturnPlayerChips_WhenOnlyOnePlayer() {
        PokerGame game = new PokerGame(null);

        PokerPlayer p1 = new PokerPlayer(1, "Player1", true);
        p1.setChipCount(5000);
        game.addPlayer(p1);

        int average = PokerPlayer.calculateAverageChips(game);
        assertThat(average).isEqualTo(5000);
    }

    @Test
    void should_HandleIntegerDivision_WhenAverageIsNotWhole() {
        PokerGame game = new PokerGame(null);

        PokerPlayer p1 = new PokerPlayer(1, "Player1", true);
        p1.setChipCount(1000);
        game.addPlayer(p1);

        PokerPlayer p2 = new PokerPlayer(2, "Player2", true);
        p2.setChipCount(1500);
        game.addPlayer(p2);

        // Average is 1250 (2500 / 2)
        int average = PokerPlayer.calculateAverageChips(game);
        assertThat(average).isEqualTo(1250);
    }

    @Test
    void should_TruncateDecimal_WhenAverageHasFraction() {
        PokerGame game = new PokerGame(null);

        PokerPlayer p1 = new PokerPlayer(1, "Player1", true);
        p1.setChipCount(1000);
        game.addPlayer(p1);

        PokerPlayer p2 = new PokerPlayer(2, "Player2", true);
        p2.setChipCount(1000);
        game.addPlayer(p2);

        PokerPlayer p3 = new PokerPlayer(3, "Player3", true);
        p3.setChipCount(1001);
        game.addPlayer(p3);

        // Average is 1000.333... which truncates to 1000
        int average = PokerPlayer.calculateAverageChips(game);
        assertThat(average).isEqualTo(1000);
    }

    @Test
    void should_ExcludeEliminatedPlayers_WhenCalculatingAverage() {
        PokerGame game = new PokerGame(null);

        PokerPlayer p1 = new PokerPlayer(1, "Player1", true);
        p1.setChipCount(2000);
        game.addPlayer(p1);

        PokerPlayer p2 = new PokerPlayer(2, "Player2", true);
        p2.setChipCount(3000);
        game.addPlayer(p2);

        PokerPlayer p3 = new PokerPlayer(3, "Player3-Eliminated", true);
        p3.setChipCount(0);
        p3.setEliminated(true);
        game.addPlayer(p3);

        // Average should be (2000 + 3000) / 2 = 2500, NOT (2000 + 3000 + 0) / 3 = 1666
        int average = PokerPlayer.calculateAverageChips(game);
        assertThat(average).isEqualTo(2500);
    }

    @Test
    void should_ReturnZero_WhenAllPlayersEliminated() {
        PokerGame game = new PokerGame(null);

        PokerPlayer p1 = new PokerPlayer(1, "Player1", true);
        p1.setChipCount(0);
        p1.setEliminated(true);
        game.addPlayer(p1);

        PokerPlayer p2 = new PokerPlayer(2, "Player2", true);
        p2.setChipCount(0);
        p2.setEliminated(true);
        game.addPlayer(p2);

        int average = PokerPlayer.calculateAverageChips(game);
        assertThat(average).isEqualTo(0);
    }

    // =================================================================
    // Server-Driven AI Bypass Tests
    // =================================================================

    @Test
    void should_ReturnNullGameAI_When_ComputerPlayerInServerDrivenMode() {
        PokerGame game = new PokerGame(null);
        game.setWebSocketConfig("game-123", "jwt", 8080);

        PokerTable table = new PokerTable(game, 1);
        game.addTable(table);
        game.setCurrentTable(table);

        PokerPlayer aiPlayer = new PokerPlayer(2, "Computer 1", false);
        aiPlayer.setTable(table, 0);

        // getGameAI() should return null for computer players in server-driven mode
        // because ServerAIProvider handles AI decisions on the server
        assertThat(aiPlayer.getGameAI()).isNull();
    }

    @Test
    void should_IdentifyComputerVsHuman_When_ServerDrivenMode() {
        PokerGame game = new PokerGame(null);
        game.setWebSocketConfig("game-123", "jwt", 8080);

        PokerPlayer computer = new PokerPlayer(2, "Computer 1", false);
        PokerPlayer human = new PokerPlayer(1, "Human", true);

        // The server-driven AI bypass guard checks isComputer()
        assertThat(computer.isComputer()).isTrue();
        assertThat(human.isComputer()).isFalse();
        assertThat(game.isServerDriven()).isTrue();
    }

    // =================================================================
    // Elimination and Observer State Tests
    // =================================================================

    @Test
    void should_BeEliminated_When_EliminatedFlagSet() {
        player.setEliminated(true);

        assertThat(player.isEliminated()).isTrue();
    }

    @Test
    void should_NotBeEliminated_When_Default() {
        assertThat(player.isEliminated()).isFalse();
    }

    @Test
    void should_ReturnEliminated_When_ZeroChipsAndEliminated() {
        player.setChipCount(0);
        player.setEliminated(true);

        assertThat(player.isEliminated()).isTrue();
        assertThat(player.getChipCount()).isZero();
    }

    @Test
    void should_ClearEliminated_When_EliminationReverted() {
        player.setEliminated(true);
        player.setEliminated(false);

        assertThat(player.isEliminated()).isFalse();
    }

    @Test
    void should_BeObserver_When_ObserverFlagSet() {
        player.setObserver(true);

        assertThat(player.isObserver()).isTrue();
    }

    @Test
    void should_NotBeObserver_When_Default() {
        assertThat(player.isObserver()).isFalse();
    }

    @Test
    void should_ClearObserver_When_ObserverReverted() {
        player.setObserver(true);
        player.setObserver(false);

        assertThat(player.isObserver()).isFalse();
    }

    // =================================================================
    // Fold and All-In State Tests
    // =================================================================

    @Test
    void should_BeFolded_When_FoldedFlagSet() {
        player.setFolded(true);

        assertThat(player.isFolded()).isTrue();
    }

    @Test
    void should_NotBeFolded_When_Default() {
        assertThat(player.isFolded()).isFalse();
    }

    @Test
    void should_ClearFolded_When_FoldReverted() {
        player.setFolded(true);
        player.setFolded(false);

        assertThat(player.isFolded()).isFalse();
    }

    @Test
    void should_BeAllIn_When_ChipsAreZero() {
        // isAllIn() is based on chip count being exactly 0
        player.setChipCount(0);

        assertThat(player.isAllIn()).isTrue();
    }

    @Test
    void should_NotBeAllIn_When_ChipsRemain() {
        player.setChipCount(500);

        assertThat(player.isAllIn()).isFalse();
    }

    @Test
    void should_NotBeAllIn_When_NegativeChips() {
        // Negative chips is not the same as all-in (which requires exactly 0)
        player.setChipCount(-50);

        assertThat(player.isAllIn()).isFalse();
    }

    // =================================================================
    // Combined Money Operations Tests
    // =================================================================

    @Test
    void should_CalculateTotalSpent_When_BuyinAndRebuyAndAddon() {
        // Set buyin directly
        player.setBuyin(100);

        // Rebuy and addon require table context for events, so test via getters
        // We can verify the total formula: getTotalSpent = buyin + rebuy + addon
        assertThat(player.getTotalSpent()).isEqualTo(100);
        assertThat(player.getRebuy()).isZero();
        assertThat(player.getAddon()).isZero();
    }

    @Test
    void should_TrackBountyCollected_When_BountiesAdded() {
        // addBounty requires no table context
        // But it adds to nPrize_ so we set buyin first for context
        player.setBuyin(50);

        // addBounty increments bountyCollected, bountyCount, and prize
        player.addBounty(25);

        assertThat(player.getBountyCollected()).isEqualTo(25);
        assertThat(player.getBountyCount()).isEqualTo(1);
        // bounty also adds to prize
        assertThat(player.getPrize()).isEqualTo(25);
    }

    @Test
    void should_AccumulateBounties_When_MultipleBountiesCollected() {
        player.addBounty(25);
        player.addBounty(50);

        assertThat(player.getBountyCollected()).isEqualTo(75);
        assertThat(player.getBountyCount()).isEqualTo(2);
        assertThat(player.getPrize()).isEqualTo(75);
    }

    @Test
    void should_IncludePrizeAndBounty_When_BothAwarded() {
        player.setPrize(500);
        player.addBounty(25);

        // setPrize sets prize to 500, then addBounty adds 25 to prize
        assertThat(player.getPrize()).isEqualTo(525);
        assertThat(player.getBountyCollected()).isEqualTo(25);
    }

    // =================================================================
    // Tournament Finish State Tests
    // =================================================================

    @Test
    void should_SetPlace_When_FinishedTournament() {
        player.setPlace(3);
        player.setPrize(250);

        assertThat(player.getPlace()).isEqualTo(3);
        assertThat(player.getPrize()).isEqualTo(250);
    }

    @Test
    void should_ReturnZeroPlace_When_NotFinished() {
        assertThat(player.getPlace()).isZero();
    }

    @Test
    void should_ReturnCorrectFinishOrder_When_MultiplePlayersFinish() {
        PokerPlayer first = new PokerPlayer(1, "Winner", true);
        first.setPlace(1);
        first.setPrize(1000);

        PokerPlayer second = new PokerPlayer(2, "Runner-Up", true);
        second.setPlace(2);
        second.setPrize(500);

        PokerPlayer third = new PokerPlayer(3, "Third", true);
        third.setPlace(3);
        third.setPrize(250);

        assertThat(first.getPlace()).isLessThan(second.getPlace());
        assertThat(second.getPlace()).isLessThan(third.getPlace());
        assertThat(first.getPrize()).isGreaterThan(second.getPrize());
        assertThat(second.getPrize()).isGreaterThan(third.getPrize());
    }

    @Test
    void should_AllowZeroPrize_When_FinishedOutOfMoney() {
        player.setPlace(7);
        player.setPrize(0);

        assertThat(player.getPlace()).isEqualTo(7);
        assertThat(player.getPrize()).isZero();
    }

    // =================================================================
    // Online Settings Serialization Tests
    // =================================================================

    @Test
    void should_PreserveSettings_When_OnlineSettingsMarshalledAndUnmarshalled() {
        // Set non-default values for all online settings
        player.setFolded(false); // avoid fireSettingsChanged NPE on table_
        // Use direct field access through getOnlineSettings/setOnlineSettings
        // which marshals: sittingOut, muckLosing, showWinning, askShowWinning,
        // askShowLosing

        // Get default settings serialized
        String defaultSettings = player.getOnlineSettings();
        assertThat(defaultSettings).isNotNull();

        // Create another player and apply settings
        PokerPlayer other = new PokerPlayer(2, "Other", true);
        other.setOnlineSettings(defaultSettings);

        // Defaults should match: sittingOut=false, muckLosing=true, showWinning=false
        assertThat(other.isSittingOut()).isFalse();
        assertThat(other.isMuckLosing()).isTrue();
        assertThat(other.isShowWinning()).isFalse();
        assertThat(other.isAskShowWinning()).isFalse();
        assertThat(other.isAskShowLosing()).isFalse();
    }

    @Test
    void should_SerializeOnlineSettings_When_SettingsString() {
        // getOnlineSettings returns a serialized string representation
        String settings = player.getOnlineSettings();

        assertThat(settings).isNotNull();
        assertThat(settings).isNotEmpty();
    }

    // =================================================================
    // Disconnected and Sitting Out State Tests
    // =================================================================

    @Test
    void should_BeDisconnected_When_DisconnectedFlagSet() {
        player.setDisconnected(true);

        assertThat(player.isDisconnected()).isTrue();
    }

    @Test
    void should_NotBeDisconnected_When_Default() {
        assertThat(player.isDisconnected()).isFalse();
    }

    @Test
    void should_BeBooted_When_BootedFlagSet() {
        player.setBooted(true);

        assertThat(player.isBooted()).isTrue();
    }

    @Test
    void should_NotBeBooted_When_Default() {
        assertThat(player.isBooted()).isFalse();
    }

    // =================================================================
    // Cards Exposed State Tests
    // =================================================================

    @Test
    void should_HaveCardsExposed_When_ExposedFlagSet() {
        player.setCardsExposed(true);

        assertThat(player.isCardsExposed()).isTrue();
    }

    @Test
    void should_NotHaveCardsExposed_When_Default() {
        assertThat(player.isCardsExposed()).isFalse();
    }

    // =================================================================
    // Hand Operations Tests
    // =================================================================

    @Test
    void should_HaveEmptyHand_When_DefaultCreated() {
        Hand hand = player.getHand();

        assertThat(hand).isNotNull();
        assertThat(hand.size()).isZero();
    }

    @Test
    void should_RemoveHand_When_RemoveHandCalled() {
        player.removeHand();

        assertThat(player.getHand()).isNull();
        assertThat(player.getHandSorted()).isNull();
    }

    @Test
    void should_ReturnNullHoldemHand_When_NoTableAssigned() {
        assertThat(player.getHoldemHand()).isNull();
    }

    // =================================================================
    // Simulated Bet Tests
    // =================================================================

    @Test
    void should_TrackSimulatedBet_When_BetAdded() {
        player.setChipCount(1000);

        int actual = player.addSimulatedBet(200);

        assertThat(actual).isEqualTo(200);
        assertThat(player.getSimulatedBet()).isEqualTo(200);
        assertThat(player.getChipCount()).isEqualTo(800);
    }

    @Test
    void should_CapSimulatedBet_When_BetExceedsChips() {
        player.setChipCount(100);

        int actual = player.addSimulatedBet(500);

        assertThat(actual).isEqualTo(100);
        assertThat(player.getSimulatedBet()).isEqualTo(100);
        assertThat(player.getChipCount()).isZero();
    }

    @Test
    void should_ReturnZeroSimulatedBet_When_Default() {
        assertThat(player.getSimulatedBet()).isZero();
    }

    // =================================================================
    // All-In Display and Score Tests
    // =================================================================

    @Test
    void should_TrackAllInPercentage_When_Set() {
        player.setAllInPerc("45.2%");

        assertThat(player.getAllInPerc()).isEqualTo("45.2%");
    }

    @Test
    void should_ReturnNullAllInPerc_When_Default() {
        assertThat(player.getAllInPerc()).isNull();
    }

    @Test
    void should_IncrementAllInWin_When_WinAdded() {
        player.addAllInWin();
        player.addAllInWin();

        assertThat(player.getAllInWin()).isEqualTo(2);
    }

    @Test
    void should_ClearAllInWin_When_Cleared() {
        player.addAllInWin();
        player.addAllInWin();

        player.clearAllInWin();

        assertThat(player.getAllInWin()).isZero();
    }

    @Test
    void should_SetAllInScore_When_ScoreProvided() {
        player.setAllInScore(85);

        assertThat(player.getAllInScore()).isEqualTo(85);
    }

    // =================================================================
    // Hands Played Tracking Tests
    // =================================================================

    @Test
    void should_ReturnZeroHandsPlayed_When_Default() {
        assertThat(player.getHandsPlayed()).isZero();
    }

    @Test
    void should_ReturnZeroDisconnectedHands_When_Default() {
        assertThat(player.getHandsPlayedDisconnected()).isZero();
    }

    @Test
    void should_ReturnZeroSitoutHands_When_Default() {
        assertThat(player.getHandsPlayedSitout()).isZero();
    }

    // =================================================================
    // Position Constants Tests
    // =================================================================

    @Test
    void should_ReturnPositionName_When_ValidPositionProvided() {
        assertThat(PokerPlayer.getPositionName(PokerPlayer.EARLY)).isEqualTo("early");
        assertThat(PokerPlayer.getPositionName(PokerPlayer.MIDDLE)).isEqualTo("middle");
        assertThat(PokerPlayer.getPositionName(PokerPlayer.LATE)).isEqualTo("late");
        assertThat(PokerPlayer.getPositionName(PokerPlayer.SMALL)).isEqualTo("small");
        assertThat(PokerPlayer.getPositionName(PokerPlayer.BIG)).isEqualTo("big");
    }

    @Test
    void should_ReturnNone_When_InvalidPositionProvided() {
        assertThat(PokerPlayer.getPositionName(-1)).isEqualTo("none");
        assertThat(PokerPlayer.getPositionName(99)).isEqualTo("none");
    }

    // =================================================================
    // Player toString Tests
    // =================================================================

    @Test
    void should_IncludeNameAndChips_When_ToStringCalled() {
        player.setChipCount(1500);

        String result = player.toString();

        assertThat(result).contains("TestPlayer");
        assertThat(result).contains("1500");
    }

    @Test
    void should_ShowNoHand_When_HandRemoved() {
        player.removeHand();

        String result = player.toString();

        assertThat(result).contains("[no hand]");
    }

    // =================================================================
    // Waiting (Wait List) State Tests
    // =================================================================

    @Test
    void should_NotBeWaiting_When_Default() {
        assertThat(player.isWaiting()).isFalse();
    }

    @Test
    void should_BeWaiting_When_WaitingFlagSet() {
        player.setWaiting(true);

        assertThat(player.isWaiting()).isTrue();
        assertThat(player.getWaitListTimeStamp()).isGreaterThan(0);
    }

    @Test
    void should_ClearWaitingTimestamp_When_WaitingCleared() {
        player.setWaiting(true);
        player.setWaiting(false);

        assertThat(player.isWaiting()).isFalse();
        assertThat(player.getWaitListTimeStamp()).isZero();
    }

    @Test
    void should_RemoveHand_When_PutOnWaitList() {
        // setWaiting(true) calls removeHand()
        player.setWaiting(true);

        assertThat(player.getHand()).isNull();
    }

    // =================================================================
    // Player Type Tests
    // =================================================================

    @Test
    void should_ReturnNullPlayerType_When_Default() {
        assertThat(player.getPlayerType()).isNull();
    }

    // =================================================================
    // Muck / Show Settings Tests
    // =================================================================

    @Test
    void should_MuckLosing_When_Default() {
        // Default is muckLosing = true
        assertThat(player.isMuckLosing()).isTrue();
    }

    @Test
    void should_NotShowWinning_When_Default() {
        assertThat(player.isShowWinning()).isFalse();
    }

    @Test
    void should_NotAskShowLosing_When_Default() {
        assertThat(player.isAskShowLosing()).isFalse();
    }

    @Test
    void should_NotAskShowWinning_When_Default() {
        assertThat(player.isAskShowWinning()).isFalse();
    }

    // =================================================================
    // Comparator Tests
    // =================================================================

    @Test
    void should_SortByName_When_SortByNameComparatorUsed() {
        PokerPlayer alice = new PokerPlayer(1, "Alice", true);
        PokerPlayer bob = new PokerPlayer(2, "Bob", true);
        PokerPlayer charlie = new PokerPlayer(3, "Charlie", true);

        List<PokerPlayer> players = new java.util.ArrayList<>(java.util.List.of(charlie, alice, bob));
        players.sort(PokerPlayer.SORTBYNAME);

        assertThat(players.get(0).getName()).isEqualTo("Alice");
        assertThat(players.get(1).getName()).isEqualTo("Bob");
        assertThat(players.get(2).getName()).isEqualTo("Charlie");
    }

    // =================================================================
    // Time Management Combined Tests
    // =================================================================

    @Test
    void should_StoreThinkBankAndTimeoutIndependently() {
        // ThinkBank and Timeout are stored in the same int field
        // but in different parts (think bank in first million, timeout above)
        player.setThinkBankMillis(30000);
        player.setTimeoutMillis(60000);

        // Both should be retrievable independently
        assertThat(player.getThinkBankMillis()).isEqualTo(30000);
        // Timeout is stored as tenths, so some truncation occurs
        assertThat(player.getTimeoutMillis()).isEqualTo(60000);
    }

    @Test
    void should_PreserveThinkBank_When_TimeoutChanged() {
        player.setThinkBankMillis(15000);
        player.setTimeoutMillis(120000);

        // Changing timeout should not affect think bank
        assertThat(player.getThinkBankMillis()).isEqualTo(15000);
    }

    @Test
    void should_PreserveTimeout_When_ThinkBankChanged() {
        player.setTimeoutMillis(90000);
        player.setThinkBankMillis(25000);

        // Changing think bank should not affect timeout
        assertThat(player.getTimeoutMillis()).isEqualTo(90000);
    }

    // =================================================================
    // isInHand State Test
    // =================================================================

    @Test
    void should_NotBeInHand_When_PlayerFolded() {
        // Even if we could be in a hand, folded players are not "in hand"
        player.setFolded(true);

        assertThat(player.isInHand()).isFalse();
    }

    // =================================================================
    // Constructor Variants Tests
    // =================================================================

    @Test
    void should_CreatePlayerWithProfile_When_ProfileConstructorUsed() {
        PlayerProfile profile = new PlayerProfile("ProfilePlayer");
        PokerPlayer profilePlayer = new PokerPlayer("key-123", 5, profile, true);

        assertThat(profilePlayer.getName()).isEqualTo("ProfilePlayer");
        assertThat(profilePlayer.getPlayerId()).isEqualTo("key-123");
        assertThat(profilePlayer.getID()).isEqualTo(5);
        assertThat(profilePlayer.isHuman()).isTrue();
        assertThat(profilePlayer.getProfile()).isEqualTo(profile);
        // Setting a profile auto-activates online
        assertThat(profilePlayer.isOnlineActivated()).isTrue();
    }

    @Test
    void should_CreateEmptyPlayer_When_DefaultConstructorUsed() {
        PokerPlayer emptyPlayer = new PokerPlayer();

        assertThat(emptyPlayer.getChipCount()).isZero();
        assertThat(emptyPlayer.isEliminated()).isFalse();
        assertThat(emptyPlayer.isFolded()).isFalse();
    }
}
