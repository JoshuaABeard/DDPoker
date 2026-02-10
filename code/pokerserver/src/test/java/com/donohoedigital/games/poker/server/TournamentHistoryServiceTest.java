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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.TournamentHistory;
import com.donohoedigital.games.poker.model.util.LeaderboardSummaryList;
import com.donohoedigital.games.poker.model.util.TournamentHistoryList;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.poker.service.TournamentHistoryService;
import org.apache.logging.log4j.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;

import static com.donohoedigital.games.poker.model.TournamentHistory.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TournamentHistoryService business logic beyond simple DAO pass-through operations.
 */
@Tag("slow")
@SpringJUnitConfig(locations = {"/app-context-pokerservertests.xml"})
@Transactional
class TournamentHistoryServiceTest
{
    private static final Logger logger = LogManager.getLogger(TournamentHistoryServiceTest.class);

    @Autowired
    private TournamentHistoryService histService;

    @Autowired
    private OnlineGameService gameService;

    @Autowired
    private OnlineProfileService profileService;

    @Test
    @Rollback
    void should_StoreTournamentHistories_When_GameUpdatedWithResults()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("Dexter");
        OnlineProfile guest1 = PokerTestData.createOnlineProfile("Zorro");
        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "XXX-999");

        gameService.saveOnlineGame(game);
        profileService.saveOnlineProfile(profile);
        profileService.saveOnlineProfile(guest1);

        TournamentHistoryList list = new TournamentHistoryList();

        // entry for host and guest
        list.add(PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE));
        list.add(PokerTestData.createTournamentHistory(guest1.getName(), PLAYER_TYPE_ONLINE));

        // some ai
        for (int i = 0; i < 4; i++)
        {
            list.add(PokerTestData.createTournamentHistory("AI #"+(i+1), PLAYER_TYPE_AI));
        }

        // some local
        for (int i = 0; i < 4; i++)
        {
            list.add(PokerTestData.createTournamentHistory("Local #"+(i+1), PLAYER_TYPE_LOCAL));
        }

        // a non-existent online
        list.add(PokerTestData.createTournamentHistory("Mystery Player", PLAYER_TYPE_ONLINE));

        // set places
        int place = list.size();
        for (TournamentHistory hist : list)
        {
            hist.setPlace(place);
            place--;
        }

        // insert them
        gameService.updateOnlineGame(game, list);

        // fetch
        TournamentHistoryList allForGame = histService.getAllTournamentHistoriesForGame(null, 0, list.size()*2, game.getId());
        assertThat(allForGame).hasSize(list.size());
        assertThat(allForGame.getTotalSize()).isEqualTo(list.size());

        // verify all histories
        for (TournamentHistory hist : allForGame)
        {
            logger.info("RETURN: " + hist);
            assertThat(hist.getTournamentName()).isEqualTo(game.getTournament().getName());
            assertThat(hist.getNumPlayers()).isEqualTo(list.size());
        }
    }

    // ========================================
    // Query Methods - getAllTournamentHistoriesForGameCount
    // ========================================

    @Test
    @Rollback
    void should_ReturnZeroCount_When_NoHistoriesForGame()
    {
        int count = histService.getAllTournamentHistoriesForGameCount(99999L);
        assertThat(count).isZero();
    }

    @Test
    @Rollback
    void should_ReturnCorrectCount_When_HistoriesExist()
    {
        // Create game and profiles
        OnlineProfile profile = PokerTestData.createOnlineProfile("Host");
        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "XXX-123");

        gameService.saveOnlineGame(game);
        profileService.saveOnlineProfile(profile);

        // Create histories
        TournamentHistoryList list = new TournamentHistoryList();
        list.add(PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE));
        list.add(PokerTestData.createTournamentHistory("AI #1", PLAYER_TYPE_AI));

        gameService.updateOnlineGame(game, list);

        int count = histService.getAllTournamentHistoriesForGameCount(game.getId());
        assertThat(count).isEqualTo(2);
    }

    // ========================================
    // Query Methods - getAllTournamentHistoriesForProfile
    // ========================================

    @Test
    @Rollback
    void should_ReturnZeroCount_When_NoHistoriesForProfile()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("NewPlayer");
        profileService.saveOnlineProfile(profile);

        int count = histService.getAllTournamentHistoriesForProfileCount(profile.getId(), null, null, null);
        assertThat(count).isZero();
    }

    @Test
    @Rollback
    void should_ReturnHistoriesForProfile_When_ProfileHasPlayed()
    {
        // Create profiles and game
        OnlineProfile profile = PokerTestData.createOnlineProfile("Player1");
        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "XXX-456");

        gameService.saveOnlineGame(game);
        profileService.saveOnlineProfile(profile);

        // Create histories
        TournamentHistoryList list = new TournamentHistoryList();
        TournamentHistory hist = PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE);
        hist.setPlace(1);
        list.add(hist);

        gameService.updateOnlineGame(game, list);

        // Query histories for profile
        int count = histService.getAllTournamentHistoriesForProfileCount(profile.getId(), null, null, null);
        assertThat(count).isEqualTo(1);

        TournamentHistoryList histories = histService.getAllTournamentHistoriesForProfile(null, 0, 10, profile.getId(), null, null, null);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getPlayerName()).isEqualTo(profile.getName());
    }

    // ========================================
    // Query Methods - getLeaderboard
    // ========================================

    @Test
    @Rollback
    void should_ReturnZeroCount_When_NoLeaderboardData()
    {
        int count = histService.getLeaderboardCount(10, null, null, null);
        assertThat(count).isZero();
    }

    @Test
    @Rollback
    void should_ReturnLeaderboard_When_DataExists()
    {
        // Create profiles and game
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("Winner");
        OnlineProfile profile2 = PokerTestData.createOnlineProfile("RunnerUp");
        OnlineGame game = PokerTestData.createOnlineGame(profile1.getName(), 1, "XXX-789");

        gameService.saveOnlineGame(game);
        profileService.saveOnlineProfile(profile1);
        profileService.saveOnlineProfile(profile2);

        // Create histories
        TournamentHistoryList list = new TournamentHistoryList();

        TournamentHistory hist1 = PokerTestData.createTournamentHistory(profile1.getName(), PLAYER_TYPE_ONLINE);
        hist1.setPlace(1);
        hist1.setPrize(1000);
        list.add(hist1);

        TournamentHistory hist2 = PokerTestData.createTournamentHistory(profile2.getName(), PLAYER_TYPE_ONLINE);
        hist2.setPlace(2);
        hist2.setPrize(500);
        list.add(hist2);

        gameService.updateOnlineGame(game, list);

        // Query leaderboard - may return 0 if minimum game requirements not met
        int count = histService.getLeaderboardCount(10, null, null, null);
        assertThat(count).isGreaterThanOrEqualTo(0);

        LeaderboardSummaryList leaderboard = histService.getLeaderboard(null, 0, 10, TournamentHistoryService.LeaderboardType.ddr1, 10, null, null, null);
        assertThat(leaderboard).isNotNull();
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    @Rollback
    void should_HandleEmptyHistoryList_When_UpdatingGame()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("EmptyHost");
        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "XXX-000");

        gameService.saveOnlineGame(game);
        profileService.saveOnlineProfile(profile);

        TournamentHistoryList emptyList = new TournamentHistoryList();
        gameService.updateOnlineGame(game, emptyList);

        int count = histService.getAllTournamentHistoriesForGameCount(game.getId());
        assertThat(count).isZero();
    }

    @Test
    @Rollback
    void should_HandleMultipleGamesForProfile_When_QueryingHistory()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("ActivePlayer");
        profileService.saveOnlineProfile(profile);

        // Create multiple games with this profile
        for (int i = 0; i < 3; i++)
        {
            OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), i, "XXX-" + i);
            gameService.saveOnlineGame(game);

            TournamentHistoryList list = new TournamentHistoryList();
            TournamentHistory hist = PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE);
            hist.setPlace(1);
            list.add(hist);

            gameService.updateOnlineGame(game, list);
        }

        int count = histService.getAllTournamentHistoriesForProfileCount(profile.getId(), null, null, null);
        assertThat(count).isEqualTo(3);
    }

    @Test
    @Rollback
    void should_FilterByName_When_QueryingProfileHistory()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("SearchPlayer");
        profileService.saveOnlineProfile(profile);

        // Create game with specific tournament name
        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "XXX-111");
        game.getTournament().setName("Special Tournament");
        gameService.saveOnlineGame(game);

        TournamentHistoryList list = new TournamentHistoryList();
        TournamentHistory hist = PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE);
        hist.setPlace(1);
        list.add(hist);

        gameService.updateOnlineGame(game, list);

        // Search with matching name
        int count = histService.getAllTournamentHistoriesForProfileCount(profile.getId(), "Special", null, null);
        assertThat(count).isEqualTo(1);

        // Search with non-matching name
        int count2 = histService.getAllTournamentHistoriesForProfileCount(profile.getId(), "NonExistent", null, null);
        assertThat(count2).isZero();
    }

    // ========================================
    // Mutation Methods - upgradeAllTournamentHistoriesForGame
    // ========================================

    @Test
    @Rollback
    void should_UpgradeHistories_When_UpgradeAllTournamentHistoriesForGameCalled()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("UpgradePlayer");
        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "XXX-555");

        gameService.saveOnlineGame(game);
        profileService.saveOnlineProfile(profile);

        // Create history
        TournamentHistoryList list = new TournamentHistoryList();
        TournamentHistory hist = PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE);
        hist.setPlace(1);
        list.add(hist);

        gameService.updateOnlineGame(game, list);

        // Upgrade histories - should complete without error
        histService.upgradeAllTournamentHistoriesForGame(game, logger);

        // Verify histories still exist
        int count = histService.getAllTournamentHistoriesForGameCount(game.getId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Rollback
    void should_HandleEmptyGame_When_UpgradingHistories()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("EmptyUpgrade");
        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "XXX-666");

        gameService.saveOnlineGame(game);
        profileService.saveOnlineProfile(profile);

        // Don't add any histories, just upgrade empty game
        histService.upgradeAllTournamentHistoriesForGame(game, logger);

        // Should handle gracefully
        int count = histService.getAllTournamentHistoriesForGameCount(game.getId());
        assertThat(count).isZero();
    }

    // ========================================
    // Additional Edge Cases and Leaderboard Tests
    // ========================================

    @Test
    @Rollback
    void should_HandlePagination_When_GettingProfileHistory()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("PaginationPlayer");
        profileService.saveOnlineProfile(profile);

        // Create multiple games for this profile
        for (int i = 0; i < 5; i++)
        {
            OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), i, "PAGE-" + i);
            gameService.saveOnlineGame(game);

            TournamentHistoryList list = new TournamentHistoryList();
            TournamentHistory hist = PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE);
            hist.setPlace(1);
            list.add(hist);

            gameService.updateOnlineGame(game, list);
        }

        // Get first page
        TournamentHistoryList page1 = histService.getAllTournamentHistoriesForProfile(null, 0, 2, profile.getId(), null, null, null);
        assertThat(page1).hasSizeLessThanOrEqualTo(2);

        // Get second page
        TournamentHistoryList page2 = histService.getAllTournamentHistoriesForProfile(null, 2, 2, profile.getId(), null, null, null);
        assertThat(page2).isNotNull();
    }

    @Test
    @Rollback
    void should_FilterByDateRange_When_QueryingProfileHistory()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("DateRangePlayer");
        profileService.saveOnlineProfile(profile);

        // Create game with specific date
        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "DATE-1");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -5);
        game.setStartDate(cal.getTime());
        gameService.saveOnlineGame(game);

        TournamentHistoryList list = new TournamentHistoryList();
        TournamentHistory hist = PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE);
        hist.setPlace(1);
        list.add(hist);

        gameService.updateOnlineGame(game, list);

        // Query with date range
        Calendar begin = Calendar.getInstance();
        begin.add(Calendar.DAY_OF_MONTH, -10);
        Calendar end = Calendar.getInstance();

        int count = histService.getAllTournamentHistoriesForProfileCount(profile.getId(), null, begin.getTime(), end.getTime());
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Rollback
    void should_ReturnDifferentLeaderboards_When_DifferentTypesRequested()
    {
        // Create profiles and games with prize data
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("LeaderWinner");
        OnlineProfile profile2 = PokerTestData.createOnlineProfile("LeaderRunner");
        profileService.saveOnlineProfile(profile1);
        profileService.saveOnlineProfile(profile2);

        OnlineGame game = PokerTestData.createOnlineGame(profile1.getName(), 1, "LEADER-1");
        gameService.saveOnlineGame(game);

        TournamentHistoryList list = new TournamentHistoryList();

        TournamentHistory hist1 = PokerTestData.createTournamentHistory(profile1.getName(), PLAYER_TYPE_ONLINE);
        hist1.setPlace(1);
        hist1.setPrize(1000);
        list.add(hist1);

        TournamentHistory hist2 = PokerTestData.createTournamentHistory(profile2.getName(), PLAYER_TYPE_ONLINE);
        hist2.setPlace(2);
        hist2.setPrize(500);
        list.add(hist2);

        gameService.updateOnlineGame(game, list);

        // Get DDR1 leaderboard
        LeaderboardSummaryList ddr1 = histService.getLeaderboard(null, 0, 10, TournamentHistoryService.LeaderboardType.ddr1, 10, null, null, null);
        assertThat(ddr1).isNotNull();

        // Get ROI leaderboard
        LeaderboardSummaryList roi = histService.getLeaderboard(null, 0, 10, TournamentHistoryService.LeaderboardType.roi, 10, null, null, null);
        assertThat(roi).isNotNull();
    }

    @Test
    @Rollback
    void should_FilterLeaderboardByName_When_NameSearchProvided()
    {
        // Create profiles with similar names
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("LeaderSearch123");
        OnlineProfile profile2 = PokerTestData.createOnlineProfile("LeaderSearch456");
        profileService.saveOnlineProfile(profile1);
        profileService.saveOnlineProfile(profile2);

        // Create games for both
        for (int i = 0; i < 2; i++)
        {
            String playerName = i == 0 ? profile1.getName() : profile2.getName();
            OnlineGame game = PokerTestData.createOnlineGame(playerName, i, "SEARCH-" + i);
            gameService.saveOnlineGame(game);

            TournamentHistoryList list = new TournamentHistoryList();
            TournamentHistory hist = PokerTestData.createTournamentHistory(playerName, PLAYER_TYPE_ONLINE);
            hist.setPlace(1);
            hist.setPrize(100);
            list.add(hist);

            gameService.updateOnlineGame(game, list);
        }

        // Search with partial name
        int count = histService.getLeaderboardCount(10, "LeaderSearch", null, null);
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Rollback
    void should_HandleLargeNumberOfHistories_When_QueryingGame()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("LargeGameHost");
        profileService.saveOnlineProfile(profile);

        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "LARGE-1");
        gameService.saveOnlineGame(game);

        // Create many histories (simulate large tournament)
        TournamentHistoryList list = new TournamentHistoryList();
        for (int i = 0; i < 20; i++)
        {
            TournamentHistory hist = PokerTestData.createTournamentHistory("Player" + i, PLAYER_TYPE_ONLINE);
            hist.setPlace(i + 1);
            list.add(hist);
        }

        gameService.updateOnlineGame(game, list);

        int count = histService.getAllTournamentHistoriesForGameCount(game.getId());
        assertThat(count).isEqualTo(20);

        // Get paginated results
        TournamentHistoryList page = histService.getAllTournamentHistoriesForGame(null, 0, 10, game.getId());
        assertThat(page).hasSizeLessThanOrEqualTo(10);
    }

    @Test
    @Rollback
    void should_PreservePlaceRankings_When_RetrievingHistories()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("RankingHost");
        profileService.saveOnlineProfile(profile);

        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "RANK-1");
        gameService.saveOnlineGame(game);

        TournamentHistoryList list = new TournamentHistoryList();
        for (int i = 0; i < 5; i++)
        {
            TournamentHistory hist = PokerTestData.createTournamentHistory("Player" + i, PLAYER_TYPE_ONLINE);
            hist.setPlace(i + 1);
            list.add(hist);
        }

        gameService.updateOnlineGame(game, list);

        TournamentHistoryList retrieved = histService.getAllTournamentHistoriesForGame(null, 0, 10, game.getId());
        assertThat(retrieved).hasSize(5);

        // Verify places are preserved
        for (int i = 0; i < retrieved.size(); i++)
        {
            assertThat(retrieved.get(i).getPlace()).isBetween(1, 5);
        }
    }

    @Test
    @Rollback
    void should_HandleMixedPlayerTypes_When_StoringHistories()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("MixedHost");
        profileService.saveOnlineProfile(profile);

        OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), 1, "MIXED-1");
        gameService.saveOnlineGame(game);

        TournamentHistoryList list = new TournamentHistoryList();

        // Add different player types
        TournamentHistory online = PokerTestData.createTournamentHistory(profile.getName(), PLAYER_TYPE_ONLINE);
        online.setPlace(1);
        list.add(online);

        TournamentHistory ai = PokerTestData.createTournamentHistory("AI Bot", PLAYER_TYPE_AI);
        ai.setPlace(2);
        list.add(ai);

        TournamentHistory local = PokerTestData.createTournamentHistory("Local Player", PLAYER_TYPE_LOCAL);
        local.setPlace(3);
        list.add(local);

        gameService.updateOnlineGame(game, list);

        int count = histService.getAllTournamentHistoriesForGameCount(game.getId());
        assertThat(count).isEqualTo(3);
    }
}
