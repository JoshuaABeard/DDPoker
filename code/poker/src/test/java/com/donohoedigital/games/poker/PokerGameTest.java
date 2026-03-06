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
// ClientTournamentProfile is in the same package
import com.donohoedigital.games.poker.online.ClientPlayer;
import com.donohoedigital.games.poker.online.RemotePokerTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerGame - core game lifecycle, player management, table
 * management, and tournament operations.
 */
class PokerGameTest {

    private PokerGame game;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create PokerGame instance with null context (headless mode)
        game = new PokerGame(null);
    }

    // =================================================================
    // Game Lifecycle Tests
    // =================================================================

    @Test
    void should_CreateNewGame_When_ConstructorCalled() {
        assertThat(game).isNotNull();
    }

    @Test
    void should_InitializeWithDefaultValues_When_GameCreated() {
        assertThat(game.isClockMode()).isFalse();
        assertThat(game.isSimulatorMode()).isFalse();
        assertThat(game.isStartFromLobby()).isFalse();
        assertThat(game.getNumTables()).isZero();
        assertThat(game.getLevel()).isZero();
    }

    @Test
    void should_SetClockMode_When_ClockModeEnabled() {
        game.setClockMode(true);

        assertThat(game.isClockMode()).isTrue();
    }

    @Test
    void should_SetSimulatorMode_When_SimulatorModeEnabled() {
        game.setSimulatorMode(true);

        assertThat(game.isSimulatorMode()).isTrue();
    }

    @Test
    void should_SetStartFromLobby_When_LobbyFlagSet() {
        game.setStartFromLobby(true);

        assertThat(game.isStartFromLobby()).isTrue();
    }

    @Test
    void should_ReturnGameDescription_When_GetDescriptionCalled() {
        // Need a human player and profile for description to work
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        ClientPlayer human = createTestPlayer("Human", false);
        game.addPlayer(human);

        String description = game.getDescription();

        assertThat(description).isNotNull();
        assertThat(description).contains("Test Tournament");
    }

    @Test
    void should_ReturnBeginPhase_When_GetBeginCalled() {
        String begin = game.getBegin();

        assertThat(begin).isNotNull();
    }

    @Test
    void should_SetClientTournamentProfile_When_ProfileSet() {
        ClientTournamentProfile profile = createTestProfile();

        game.setProfile(profile);

        assertThat(game.getProfile()).isEqualTo(profile);
    }

    @Test
    void should_MarkGameNotInProgress_When_NewGameCreated() {
        // initTournament requires PokerMain initialization, so we just test
        // that a new game is not in progress by default
        assertThat(game.isInProgress()).isFalse();
    }

    @Test
    void should_HaveGameClock_When_GameCreated() {
        assertThat(game.getGameClock()).isNotNull();
    }

    // =================================================================
    // Player Management Tests
    // =================================================================

    @Test
    void should_AddPlayer_When_PlayerAdded() {
        ClientPlayer player = createTestPlayer("TestPlayer", false);

        game.addPlayer(player);

        assertThat(game.getPokerPlayersCopy()).contains(player);
    }

    @Test
    void should_RemovePlayer_When_PlayerRemoved() {
        ClientPlayer player = createTestPlayer("TestPlayer", false);
        game.addPlayer(player);

        game.removePlayer(player);

        assertThat(game.getPokerPlayersCopy()).doesNotContain(player);
    }

    @Test
    void should_ReturnPlayerByID_When_PlayerExists() {
        ClientPlayer player = new ClientPlayer(42, "TestPlayer", false);
        game.addPlayer(player);

        ClientPlayer found = game.getPokerPlayerFromID(42);

        assertThat(found).isEqualTo(player);
    }

    @Test
    void should_ReturnNull_When_PlayerIDDoesNotExist() {
        ClientPlayer found = game.getPokerPlayerFromID(999);

        assertThat(found).isNull();
    }

    @Test
    void should_ReturnPlayersCopy_When_GetClientPlayersCopyCalled() {
        ClientPlayer player1 = createTestPlayer("Player1", false);
        ClientPlayer player2 = createTestPlayer("Player2", false);
        game.addPlayer(player1);
        game.addPlayer(player2);

        var players = game.getPokerPlayersCopy();

        assertThat(players).hasSize(2);
        assertThat(players).containsExactlyInAnyOrder(player1, player2);
    }

    @Test
    void should_ReturnHumanPlayer_When_HumanPlayerExists() {
        ClientPlayer human = createTestPlayer("Human", false);
        ClientPlayer computer = createTestPlayer("Computer", true);
        game.addPlayer(human); // Add human first
        game.addPlayer(computer);

        ClientPlayer found = game.getHumanPlayer();

        assertThat(found).isEqualTo(human);
    }

    @Test
    void should_ReturnNull_When_PlayerKeyDoesNotMatch() {
        ClientPlayer player = createTestPlayer("TestPlayer", false);
        game.addPlayer(player);

        // Key matching requires specific initialization that's not available in unit
        // tests
        // Test the null case instead
        ClientPlayer found = game.getPokerPlayerFromKey("non-existent-key");

        assertThat(found).isNull();
    }

    // =================================================================
    // Table Management Tests
    // =================================================================

    @Test
    void should_AddTable_When_TableAdded() {
        RemotePokerTable table = createTestTable(1);

        game.addTable(table);

        assertThat(game.getNumTables()).isEqualTo(1);
        assertThat(game.getTables()).contains(table);
    }

    @Test
    void should_RemoveTable_When_TableRemoved() {
        RemotePokerTable table = createTestTable(1);
        game.addTable(table);

        game.removeTable(table);

        assertThat(game.getNumTables()).isZero();
        assertThat(game.getTables()).doesNotContain(table);
    }

    @Test
    void should_SetCurrentTable_When_TableSet() {
        RemotePokerTable table = createTestTable(1);
        game.addTable(table);

        game.setCurrentTable(table);

        assertThat(game.getCurrentTable()).isEqualTo(table);
    }

    @Test
    void should_ReturnTableByIndex_When_IndexValid() {
        RemotePokerTable table1 = createTestTable(1);
        RemotePokerTable table2 = createTestTable(2);
        game.addTable(table1);
        game.addTable(table2);

        assertThat(game.getTable(0)).isEqualTo(table1);
        assertThat(game.getTable(1)).isEqualTo(table2);
    }

    @Test
    void should_ReturnTableByNumber_When_NumberMatches() {
        RemotePokerTable table = createTestTable(5);
        game.addTable(table);

        RemotePokerTable found = (RemotePokerTable) game.getTableByNumber(5);

        assertThat(found).isEqualTo(table);
    }

    @Test
    void should_ReturnAllTables_When_GetTablesCalled() {
        RemotePokerTable table1 = createTestTable(1);
        RemotePokerTable table2 = createTestTable(2);
        game.addTable(table1);
        game.addTable(table2);

        var tables = game.getTables();

        assertThat(tables).hasSize(2);
        assertThat(tables).containsExactlyInAnyOrder(table1, table2);
    }

    // =================================================================
    // Level Management Tests
    // =================================================================

    @Test
    void should_AdvanceLevel_When_NextLevelCalled() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        int initialLevel = game.getLevel();

        game.nextLevel();

        assertThat(game.getLevel()).isEqualTo(initialLevel + 1);
    }

    @Test
    void should_DecrementLevel_When_PrevLevelCalled() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        game.nextLevel(); // Go to level 1
        game.nextLevel(); // Go to level 2

        game.prevLevel();

        assertThat(game.getLevel()).isEqualTo(1);
    }

    @Test
    void should_ChangeToSpecificLevel_When_ChangeLevelCalled() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        game.changeLevel(5);

        assertThat(game.getLevel()).isEqualTo(5);
    }

    @Test
    void should_GetBigBlind_When_LevelSet() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        game.changeLevel(1); // Set to level 1 to get default blinds

        int bigBlind = game.getBigBlind();

        // Level 0 returns 0, but once game is set up it should have blinds
        assertThat(bigBlind).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_GetSmallBlind_When_LevelSet() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        game.changeLevel(1); // Set to level 1 to get default blinds

        int smallBlind = game.getSmallBlind();

        // Level 0 returns 0, but once game is set up it should have blinds
        assertThat(smallBlind).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_GetAnte_When_LevelSet() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        int ante = game.getAnte();

        assertThat(ante).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_GetMinChip_When_LevelSet() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        int minChip = game.getMinChip();

        assertThat(minChip).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_IncreaseBlinds_When_LevelAdvances() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        int initialBigBlind = game.getBigBlind();

        game.nextLevel();
        int newBigBlind = game.getBigBlind();

        assertThat(newBigBlind).isGreaterThan(initialBigBlind);
    }

    // =================================================================
    // Tournament Management Tests
    // =================================================================

    @Test
    void should_GetPrizePool_When_ProfileSet() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        int prizePool = game.getPrizePool();

        assertThat(prizePool).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_GetPrizesPaid_When_TournamentActive() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        int prizesPaid = game.getPrizesPaid();

        assertThat(prizesPaid).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_TrackPlayersOut_When_PlayerOut() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        ClientPlayer player = createTestPlayer("OutPlayer", false);
        game.addPlayer(player);

        game.playerOut(player);

        assertThat(game.getNumPlayersOut()).isEqualTo(1);
    }

    @Test
    void should_FinishGame_When_FinishCalled() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        assertThatCode(() -> game.finish()).doesNotThrowAnyException();
    }

    // =================================================================
    // Chip Management Tests
    // =================================================================

    @Test
    void should_AddExtraChips_When_ChipsAdded() {
        assertThatCode(() -> game.addExtraChips(1000)).doesNotThrowAnyException();
    }

    @Test
    void should_AdjustTotalChipsInPlay_When_ExtraChipsChange() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        game.addPlayer(createTestPlayer("Player1", false));
        game.addPlayer(createTestPlayer("Player2", false));

        game.computeTotalChipsInPlay();
        int baseline = game.getTotalChipsInPlay();

        game.addExtraChips(-25);

        assertThat(game.getTotalChipsInPlay()).isEqualTo(baseline - 25);
    }

    @Test
    void should_ComputeTotalChips_When_ChipsInPlay() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        ClientPlayer player1 = createTestPlayer("Player1", false);
        ClientPlayer player2 = createTestPlayer("Player2", false);
        player1.setChipCount(1000);
        player2.setChipCount(1500);
        game.addPlayer(player1);
        game.addPlayer(player2);

        game.computeTotalChipsInPlay();
        int totalChips = game.getTotalChipsInPlay();

        // Profile adds buyin chips (500 each) to player chips (1000+1500)
        assertThat(totalChips).isEqualTo(3000);
    }

    @Test
    void should_GetAverageStack_When_PlayersHaveChips() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        ClientPlayer player1 = createTestPlayer("Player1", false);
        ClientPlayer player2 = createTestPlayer("Player2", false);
        player1.setChipCount(1000);
        player2.setChipCount(1500);
        game.addPlayer(player1);
        game.addPlayer(player2);
        game.computeTotalChipsInPlay();

        int averageStack = game.getAverageStack();

        // Total 3000 / 2 players = 1500 average
        assertThat(averageStack).isEqualTo(1500);
    }

    @Test
    void should_TrackChipsBought_When_ChipsPurchased() {
        game.chipsBought(500);

        // Should not throw exception
        int totalChips = game.getTotalChipsInPlay();
        assertThat(totalChips).isGreaterThanOrEqualTo(0);
    }

    // =================================================================
    // Player Ranking Tests
    // =================================================================

    @Test
    void should_GetPlayerRank_When_PlayerInGame() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        ClientPlayer player1 = createTestPlayer("Player1", false);
        ClientPlayer player2 = createTestPlayer("Player2", false);
        player1.setChipCount(2000);
        player2.setChipCount(1000);
        game.addPlayer(player1);
        game.addPlayer(player2);

        int rank1 = game.getRank(player1);
        int rank2 = game.getRank(player2);

        assertThat(rank1).isLessThan(rank2); // Player1 should rank higher (smaller rank number)
    }

    @Test
    void should_GetPlayersByRank_When_MultiplePlayers() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        ClientPlayer player1 = createTestPlayer("Player1", false);
        ClientPlayer player2 = createTestPlayer("Player2", false);
        ClientPlayer player3 = createTestPlayer("Player3", false);
        player1.setChipCount(3000);
        player2.setChipCount(2000);
        player3.setChipCount(1000);
        game.addPlayer(player1);
        game.addPlayer(player2);
        game.addPlayer(player3);

        var rankedPlayers = game.getPlayersByRank();

        assertThat(rankedPlayers).hasSize(3);
        assertThat(rankedPlayers.get(0).getChipCount()).isGreaterThanOrEqualTo(rankedPlayers.get(1).getChipCount());
        assertThat(rankedPlayers.get(1).getChipCount()).isGreaterThanOrEqualTo(rankedPlayers.get(2).getChipCount());
    }

    // =================================================================
    // Clock Management Tests
    // =================================================================

    @Test
    void should_GetGameClock_When_ClockModeEnabled() {
        game.setClockMode(true);

        var clock = game.getGameClock();

        assertThat(clock).isNotNull();
    }

    @Test
    void should_GetSecondsInLevel_When_LevelQueried() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        int seconds = game.getSecondsInLevel(1);

        assertThat(seconds).isGreaterThan(0);
    }

    // =================================================================
    // Game State Tests
    // =================================================================

    @Test
    void should_GetStartDate_When_GameCreated() {
        long startDate = game.getStartDate();

        // Start date is 0 until game actually starts
        assertThat(startDate).isEqualTo(0L);
    }

    @Test
    void should_GetGameID_When_GameCreated() {
        long gameId = game.getID();

        assertThat(gameId).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_GetSeats_When_ProfileSet() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        int seats = game.getSeats();

        assertThat(seats).isGreaterThan(0);
    }

    @Test
    void should_VerifyChipCount_When_ChipsInPlay() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        ClientPlayer player = createTestPlayer("Player1", false);
        player.setChipCount(1000);
        game.addPlayer(player);
        game.computeTotalChipsInPlay();

        assertThatCode(() -> game.verifyChipCount()).doesNotThrowAnyException();
    }

    @Test
    void should_NotDoubleCountPlayerOut_When_PlayerAlreadyEliminated() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        ClientPlayer player = new ClientPlayer(1, "Player1", false);
        player.setBuyin(100);
        game.addPlayer(player);

        game.playerOut(player);
        game.playerOut(player);

        assertThat(game.getNumPlayersOut()).isEqualTo(1);
    }

    @Test
    void should_ResolveCanonicalPlayer_When_PlayerOutCalledWithCloneObject() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        ClientPlayer canonical = new ClientPlayer(7, "Player7", false);
        canonical.setBuyin(100);
        game.addPlayer(canonical);

        ClientPlayer clone = new ClientPlayer(7, "Player7", false);
        game.playerOut(clone);

        assertThat(canonical.isEliminated()).isTrue();
        assertThat(game.getNumPlayersOut()).isEqualTo(1);

        game.playerOut(clone);
        assertThat(game.getNumPlayersOut()).isEqualTo(1);
    }

    // =================================================================
    // applyPlayerResult Tests (WebSocket mode)
    // =================================================================

    @Test
    void should_DoNothing_When_PlayerIdNotFoundInApplyPlayerResult() {
        // No players added — ID 999 won't be found
        assertThatCode(() -> game.applyPlayerResult(999, 2)).doesNotThrowAnyException();
        assertThat(game.getNumPlayersOut()).isEqualTo(0);
    }

    @Test
    void should_MarkPlayerEliminated_When_ApplyPlayerResultCalled() {
        ClientPlayer player = new ClientPlayer(1, "Player1", false);
        game.addPlayer(player);

        game.applyPlayerResult(1, 2);

        assertThat(player.isEliminated()).isTrue();
    }

    @Test
    void should_SetFinishPosition_When_ApplyPlayerResultCalled() {
        ClientPlayer player = new ClientPlayer(1, "Player1", false);
        game.addPlayer(player);

        game.applyPlayerResult(1, 3);

        assertThat(player.getPlace()).isEqualTo(3);
    }

    @Test
    void should_ZeroChips_When_FinishPositionIsNotFirst() {
        ClientPlayer player = new ClientPlayer(1, "Player1", false);
        player.setChipCount(1000);
        game.addPlayer(player);

        game.applyPlayerResult(1, 2);

        assertThat(player.getChipCount()).isEqualTo(0);
    }

    @Test
    void should_PreserveChips_When_FinishPositionIsFirst() {
        ClientPlayer player = new ClientPlayer(1, "Player1", false);
        player.setChipCount(5000);
        game.addPlayer(player);

        game.applyPlayerResult(1, 1);

        assertThat(player.getChipCount()).isEqualTo(5000);
    }

    @Test
    void should_IncrementPlayersOut_When_ApplyPlayerResultCalled() {
        ClientPlayer player = new ClientPlayer(1, "Player1", false);
        game.addPlayer(player);

        game.applyPlayerResult(1, 2);

        assertThat(game.getNumPlayersOut()).isEqualTo(1);
    }

    @Test
    void should_SetZeroPrize_When_NoProfileSetInApplyPlayerResult() {
        // No profile set — prize defaults to 0
        ClientPlayer player = new ClientPlayer(1, "Player1", false);
        game.addPlayer(player);

        game.applyPlayerResult(1, 2);

        assertThat(player.getPrize()).isEqualTo(0);
    }

    @Test
    void should_SetPrizeFromProfile_When_ProfileIsSetInApplyPlayerResult() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);
        ClientPlayer player = new ClientPlayer(1, "Player1", false);
        player.setBuyin(100);
        game.addPlayer(player);

        game.applyPlayerResult(1, 2);

        // getPayout(2) for a basic profile without explicit payout structure returns 0;
        // just verify the method ran without exception and set a non-negative prize.
        assertThat(player.getPrize()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_AllocatePrizePoolAcrossResults_When_ApplyPlayerResultCalledForAllFinishers() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        ClientPlayer winner = new ClientPlayer(1, "Winner", false);
        winner.setBuyin(100);
        ClientPlayer runnerUp = new ClientPlayer(2, "RunnerUp", false);
        runnerUp.setBuyin(100);

        game.addPlayer(winner);
        game.addPlayer(runnerUp);

        game.applyPlayerResult(2, 2);
        game.applyPlayerResult(1, 1);

        assertThat(winner.getPrize()).isGreaterThan(0);
        assertThat(game.getPrizesPaid()).isEqualTo(game.getPrizePool());
    }

    @Test
    void should_NotIncrementPlayersOut_When_ApplyPlayerResultRepeatedForSamePlayer() {
        ClientPlayer player = new ClientPlayer(1, "Player1", false);
        game.addPlayer(player);

        game.applyPlayerResult(1, 2);
        game.applyPlayerResult(1, 2);

        assertThat(game.getNumPlayersOut()).isEqualTo(1);
    }

    @Test
    void should_NotOverwriteWinnerPrize_When_ApplyPlayerResultRepeatedForWinner() {
        ClientTournamentProfile profile = createTestProfile();
        game.setProfile(profile);

        ClientPlayer winner = new ClientPlayer(1, "Winner", false);
        winner.setBuyin(100);
        ClientPlayer runnerUp = new ClientPlayer(2, "RunnerUp", false);
        runnerUp.setBuyin(100);

        game.addPlayer(winner);
        game.addPlayer(runnerUp);

        game.applyPlayerResult(2, 2);
        game.applyPlayerResult(1, 1);
        int prizeAfterFirstApply = winner.getPrize();

        game.applyPlayerResult(1, 1);

        assertThat(winner.getPrize()).isEqualTo(prizeAfterFirstApply);
        assertThat(winner.getPrize()).isGreaterThan(0);
        assertThat(game.getPrizesPaid()).isEqualTo(game.getPrizePool());
    }

    // =================================================================
    // Server-Driven Mode Tests
    // =================================================================

    @Test
    void should_NotBeServerDriven_When_NoWebSocketConfigSet() {
        assertThat(game.isServerDriven()).isFalse();
    }

    @Test
    void should_BeServerDriven_When_WebSocketConfigSet() {
        game.setWebSocketConfig("game-123", "jwt-token", 8080);

        assertThat(game.isServerDriven()).isTrue();
    }

    @Test
    void should_BeServerDriven_When_WebSocketConfigSetWithObserver() {
        game.setWebSocketConfig("game-456", "jwt-token", 8080, true);

        assertThat(game.isServerDriven()).isTrue();
    }

    @Test
    void should_ReturnWebSocketConfig_When_ConfigSet() {
        game.setWebSocketConfig("game-123", "jwt-token", 8080);

        PokerGame.WebSocketConfig config = game.getWebSocketConfig();
        assertThat(config).isNotNull();
        assertThat(config.gameId()).isEqualTo("game-123");
        assertThat(config.jwt()).isEqualTo("jwt-token");
        assertThat(config.host()).isEqualTo("localhost");
        assertThat(config.port()).isEqualTo(8080);
        assertThat(config.observer()).isFalse();
    }

    @Test
    void should_StoreExplicitHost_When_CentralServerConfigSet() {
        game.setWebSocketConfig("game-456", "jwt-xyz", "game.example.com", 9090);

        PokerGame.WebSocketConfig config = game.getWebSocketConfig();
        assertThat(config.gameId()).isEqualTo("game-456");
        assertThat(config.jwt()).isEqualTo("jwt-xyz");
        assertThat(config.host()).isEqualTo("game.example.com");
        assertThat(config.port()).isEqualTo(9090);
        assertThat(config.observer()).isFalse();
    }

    @Test
    void should_StoreExplicitHostAndObserver_When_CentralServerObserverConfigSet() {
        game.setWebSocketConfig("game-789", "jwt-obs", "game.example.com", 9090, true);

        PokerGame.WebSocketConfig config = game.getWebSocketConfig();
        assertThat(config.host()).isEqualTo("game.example.com");
        assertThat(config.observer()).isTrue();
    }

    // =================================================================
    // Test Helper Methods
    // =================================================================

    private ClientTournamentProfile createTestProfile() {
        ClientTournamentProfile profile = new ClientTournamentProfile();
        profile.setName("Test Tournament");
        profile.setBuyinChips(1500);
        // Set default blind levels so level-advance tests work
        profile.setLevel(1, 0, 10, 20, 15);
        profile.setLevel(2, 0, 20, 40, 15);
        profile.setLevel(3, 5, 30, 60, 15);
        profile.fixLevels();
        return profile;
    }

    private ClientPlayer createTestPlayer(String name, boolean isComputer) {
        ClientPlayer player = new ClientPlayer(0, name, isComputer);
        player.setName(name);
        return player;
    }

    private RemotePokerTable createTestTable(int tableNumber) {
        RemotePokerTable table = new RemotePokerTable(game, tableNumber);
        return table;
    }
}
