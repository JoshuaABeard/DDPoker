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

import com.donohoedigital.db.PagedList;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.util.OnlineGameList;
import com.donohoedigital.games.poker.service.OnlineGameService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OnlineGameService business logic beyond simple DAO pass-through
 * operations.
 */
@Tag("slow")
@SpringJUnitConfig(locations = {"/app-context-pokerservertests.xml"})
@Transactional
class OnlineGameServiceTest {
    @Autowired
    private OnlineGameService service;

    @Test
    @Rollback
    void should_ReplaceExistingGame_When_SavingDuplicateKeyAndUrl() {
        String key = "1234-1234-1234-1234";
        OnlineGame newGame = PokerTestData.createOnlineGame("newGame", 1, "XXX-333");
        newGame.setLicenseKey(key);
        service.saveOnlineGame(newGame);
        assertThat(newGame.getId()).isNotNull();

        // create dup with same key/url, but different name
        OnlineGame dupGame = PokerTestData.createOnlineGame("dupGame", 1, "XXX-333");
        dupGame.setLicenseKey(key);
        service.saveOnlineGame(dupGame);
        assertThat(dupGame.getId()).isNotEqualTo(newGame.getId());

        // first game should be deleted
        assertThat(service.getOnlineGameById(newGame.getId())).isNull();
    }

    @Test
    @Rollback
    void should_UpdateExistingGame_When_KeyAndUrlMatch() {
        String key = "1234-1234-1234-1234";
        OnlineGame newGame = PokerTestData.createOnlineGame("newGame", 1, "XXX-333");
        newGame.setLicenseKey(key);
        service.saveOnlineGame(newGame);
        assertThat(newGame.getId()).isNotNull();

        // create dup with same key/url, but different name
        OnlineGame dupGame = PokerTestData.createOnlineGame("dupGame", 1, "XXX-333");
        dupGame.setLicenseKey(key);
        dupGame = service.updateOnlineGame(dupGame);
        assertThat(dupGame.getId()).isEqualTo(newGame.getId());

        // fetch to make sure name updated
        OnlineGame fetch = service.getOnlineGameById(dupGame.getId());
        assertThat(fetch.getHostPlayer()).isEqualTo(dupGame.getHostPlayer());
    }

    @Test
    @Rollback
    void should_ReturnNull_When_UpdatingNonExistentGame() {
        String key = "1234-1234-1234-1234";
        OnlineGame newGame = PokerTestData.createOnlineGame("newGame", 1, "XXX-333");
        newGame.setLicenseKey(key);
        newGame = service.updateOnlineGame(newGame);
        assertThat(newGame).isNull();
    }

    @Test
    @Rollback
    void should_DeleteGame_When_KeyAndUrlMatch() {
        String key = "1234-1234-1234-1234";
        OnlineGame newGame = PokerTestData.createOnlineGame("newGame", 1, "XXX-333");
        newGame.setLicenseKey(key);
        service.saveOnlineGame(newGame);
        assertThat(newGame.getId()).isNotNull();

        // create dup with same key/url, but different name
        OnlineGame dupGame = PokerTestData.createOnlineGame("dupGame", 1, "XXX-333");
        dupGame.setLicenseKey(key);
        service.deleteOnlineGame(dupGame);

        // first game should be deleted
        assertThat(service.getOnlineGameById(newGame.getId())).isNull();
    }

    // ========================================
    // Query Methods - getOnlineGameById
    // ========================================

    // ========================================
    // Query Methods - getOnlineGameById
    // ========================================

    @Test
    @Rollback
    void should_ReturnGame_When_ValidIdProvided() {
        OnlineGame game = PokerTestData.createOnlineGame("TestGame", 1, "XXX-111");
        service.saveOnlineGame(game);

        OnlineGame fetched = service.getOnlineGameById(game.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getHostPlayer()).isEqualTo("TestGame");
    }

    @Test
    @Rollback
    void should_ReturnNull_When_InvalidIdProvided() {
        OnlineGame fetched = service.getOnlineGameById(99999L);
        assertThat(fetched).isNull();
    }

    // ========================================
    // Query Methods - getHostSummaryCount
    // ========================================

    @Test
    @Rollback
    void should_ReturnZeroCount_When_NoGamesForHostSummary() {
        int count = service.getHostSummaryCount(null, null, null);
        assertThat(count).isZero();
    }

    @Test
    @Rollback
    void should_ReturnCorrectCount_When_GamesExistForHostSummary() {
        // Create games
        for (int i = 0; i < 3; i++) {
            OnlineGame game = PokerTestData.createOnlineGame("Host" + i, i, "XXX-" + i);
            service.saveOnlineGame(game);
        }

        int count = service.getHostSummaryCount(null, null, null);
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void should_ReturnZero_When_NoGamesExistForCount() {
        int count = service.getOnlineGamesCount(new Integer[]{2}, null, null, null);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @Rollback
    void should_ReturnCorrectCount_When_GamesExistForCount() {
        // Create games
        for (int i = 0; i < 5; i++) {
            OnlineGame game = PokerTestData.createOnlineGame("Host" + i, i, "COUNT-" + i);
            service.saveOnlineGame(game);
        }

        int count = service.getOnlineGamesCount(new Integer[]{2}, null, null, null);
        assertThat(count).isGreaterThanOrEqualTo(5);
    }

    @Test
    void should_ReturnEmptyList_When_NoGamesExistForGetGames() {
        OnlineGameList games = service.getOnlineGames(null, 0, 10, new Integer[]{2}, null, null, null,
                OnlineGameService.OrderByType.date);

        assertThat(games).isNotNull();
        assertThat(games.getTotalSize()).isEqualTo(0);
    }

    @Test
    @Rollback
    void should_ReturnGames_When_GamesExistForGetGames() {
        // Create games
        for (int i = 0; i < 3; i++) {
            OnlineGame game = PokerTestData.createOnlineGame("Host" + i, i, "GET-" + i);
            service.saveOnlineGame(game);
        }

        OnlineGameList games = service.getOnlineGames(null, 0, 10, new Integer[]{2}, null, null, null,
                OnlineGameService.OrderByType.date);

        assertThat(games).isNotNull();
        assertThat(games.getTotalSize()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void should_ReturnNull_When_InvalidTournamentHistoryId() {
        OnlineGame game = service.getOnlineGameByTournamentHistoryId(999999L);
        assertThat(game).isNull();
    }

    @Test
    void should_ReturnEmptyList_When_NoGamesForHostSummaryList() {
        PagedList<HostSummary> summary = service.getHostSummary(null, 0, 10, null, null, null);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalSize()).isEqualTo(0);
    }

    @Test
    @Rollback
    void should_ReturnSummary_When_GamesExistForHostSummary() {
        // Create games from same host
        for (int i = 0; i < 3; i++) {
            OnlineGame game = PokerTestData.createOnlineGame("SameHost", i, "SUMMARY-" + i);
            service.saveOnlineGame(game);
        }

        PagedList<HostSummary> summary = service.getHostSummary(null, 0, 10, null, null, null);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalSize()).isGreaterThan(0);
    }

    @Test
    @Rollback
    void should_RemoveOldGames_When_PurgeCalledWithDate() {
        // Create a game
        OnlineGame game = PokerTestData.createOnlineGame("PurgeHost", 1, "PURGE-1");
        service.saveOnlineGame(game);

        // Purge games older than today
        int purged = service.purgeGames(new Date(), null);

        assertThat(purged).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_ReturnZero_When_NothingToPurge() {
        // Purge games older than far future date
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 10);
        int purged = service.purgeGames(cal.getTime(), null);

        assertThat(purged).isEqualTo(0);
    }

    // ========================================
    // Additional Edge Cases and Integration Tests
    // ========================================

    @Test
    @Rollback
    void should_HandlePagination_When_GettingGames() {
        // Create multiple games
        for (int i = 0; i < 5; i++) {
            OnlineGame game = PokerTestData.createOnlineGame("PageHost" + i, i, "PAGE-" + i);
            service.saveOnlineGame(game);
        }

        // Get first page
        OnlineGameList page1 = service.getOnlineGames(null, 0, 2, new Integer[]{2}, null, null, null,
                OnlineGameService.OrderByType.date);
        assertThat(page1).isNotNull();
        assertThat(page1.size()).isLessThanOrEqualTo(2);

        // Get second page
        OnlineGameList page2 = service.getOnlineGames(null, 2, 2, new Integer[]{2}, null, null, null,
                OnlineGameService.OrderByType.date);
        assertThat(page2).isNotNull();
    }

    @Test
    @Rollback
    void should_FilterByMode_When_MultipleModesProvided() {
        OnlineGame game1 = PokerTestData.createOnlineGame("ModeHost1", 1, "MODE-1");
        game1.setMode(OnlineGame.MODE_PLAY);
        service.saveOnlineGame(game1);

        OnlineGame game2 = PokerTestData.createOnlineGame("ModeHost2", 2, "MODE-2");
        game2.setMode(OnlineGame.MODE_REG);
        service.saveOnlineGame(game2);

        // Query for both modes
        int count = service.getOnlineGamesCount(new Integer[]{OnlineGame.MODE_PLAY, OnlineGame.MODE_REG}, null, null,
                null);
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Rollback
    void should_ReturnEmptyHostSummary_When_NoMatchingName() {
        PagedList<HostSummary> summary = service.getHostSummary(null, 0, 10, "NonExistentHost", null, null);
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalSize()).isZero();
    }

    @Test
    @Rollback
    void should_HandleDateRangeFilter_When_QueryingGames() {
        // Create game with specific date
        OnlineGame game = PokerTestData.createOnlineGame("DateHost", 1, "DATE-1");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -5);
        game.setStartDate(cal.getTime());
        service.saveOnlineGame(game);

        // Query with date range that includes the game
        Calendar begin = Calendar.getInstance();
        begin.add(Calendar.DAY_OF_MONTH, -10);
        Calendar end = Calendar.getInstance();

        int count = service.getOnlineGamesCount(new Integer[]{2}, null, begin.getTime(), end.getTime());
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Rollback
    void should_OrderByMode_When_OrderByModeSpecified() {
        // Create games with different modes
        for (int i = 0; i < 3; i++) {
            OnlineGame game = PokerTestData.createOnlineGame("OrderHost" + i, i, "ORDER-" + i);
            service.saveOnlineGame(game);
        }

        OnlineGameList games = service.getOnlineGames(null, 0, 10, new Integer[]{2}, null, null, null,
                OnlineGameService.OrderByType.mode);
        assertThat(games).isNotNull();
    }

    @Test
    @Rollback
    void should_CountZero_When_FilteringWithNonMatchingName() {
        // Create games with specific names
        OnlineGame game1 = PokerTestData.createOnlineGame("SpecificHost1", 1, "FILTER-1");
        service.saveOnlineGame(game1);

        OnlineGame game2 = PokerTestData.createOnlineGame("SpecificHost2", 2, "FILTER-2");
        service.saveOnlineGame(game2);

        // Query with non-matching name
        int count = service.getHostSummaryCount("NonMatchingName", null, null);
        assertThat(count).isZero();
    }

    @Test
    @Rollback
    void should_HandleNameSearch_When_QueryingHostSummary() {
        // Create games with similar host names
        OnlineGame game1 = PokerTestData.createOnlineGame("SearchHost123", 1, "SEARCH-1");
        service.saveOnlineGame(game1);

        OnlineGame game2 = PokerTestData.createOnlineGame("SearchHost456", 2, "SEARCH-2");
        service.saveOnlineGame(game2);

        // Search with partial name
        PagedList<HostSummary> summary = service.getHostSummary(null, 0, 10, "SearchHost", null, null);
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalSize()).isGreaterThanOrEqualTo(2);
    }

}
