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

import com.donohoedigital.base.Utils;
import com.donohoedigital.games.poker.dao.OnlineGameDao;
import com.donohoedigital.games.poker.dao.OnlineProfileDao;
import com.donohoedigital.games.poker.dao.TournamentHistoryDao;
import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.TournamentHistory;
import com.donohoedigital.games.poker.model.util.TournamentHistoryList;
import org.apache.logging.log4j.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TournamentHistory DAO operations.
 */
@Tag("slow")
@SpringJUnitConfig(locations = {"/app-context-pokerservertests.xml"})
@Transactional
class TournamentHistoryTest {
    private final Logger logger = LogManager.getLogger(TournamentHistoryTest.class);

    @Autowired
    private TournamentHistoryDao histDao;

    @Autowired
    private OnlineGameDao gameDao;

    @Autowired
    private OnlineProfileDao profileDao;

    @Test
    @Rollback
    void should_PersistAndUpdate_When_HistorySaved() {
        TournamentHistory newHistory = PokerTestData.createTournamentHistory("TEST shouldPersist", 5, "AAA-999");
        gameDao.save(newHistory.getGame());
        profileDao.save(newHistory.getProfile());
        histDao.save(newHistory);

        assertThat(newHistory.getId()).isNotNull();

        TournamentHistory fetch = histDao.get(newHistory.getId());
        assertThat(fetch.getPlayerName()).isEqualTo(newHistory.getPlayerName());

        int prize = 12345;
        newHistory.setPrize(prize);
        histDao.update(newHistory);

        TournamentHistory updated = histDao.get(newHistory.getId());
        assertThat(updated.getPrize()).isEqualTo(prize);
    }

    @Test
    @Rollback
    void should_CascadeDeleteHistory_When_GameDeleted() {
        TournamentHistory history = PokerTestData.createTournamentHistory("saveBeforeDelete", 10, "BBB-888");
        gameDao.save(history.getGame());
        profileDao.save(history.getProfile());
        histDao.save(history);
        gameDao.refresh(history.getGame());
        assertThat(history.getId()).isNotNull();
        logger.info(history.getPlayerName() + " saved with id " + history.getId());

        // deleting via cascade from OnlineGame
        OnlineGame game = gameDao.get(history.getGame().getId());
        List<TournamentHistory> histories = game.getHistories();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getId()).isEqualTo(history.getId());
        gameDao.delete(game);

        logger.info("Should have deleted profile with id " + history.getId());
        TournamentHistory deleted = histDao.get(history.getId());
        assertThat(deleted).isNull();

        profileDao.delete(history.getProfile());
    }

    @Test
    @Rollback
    void should_DeleteHistoryButNotProfile_When_GameDeleted() {
        TournamentHistory newHistory = PokerTestData.createTournamentHistory("TEST shouldPersist", 5, "AAA-999");
        gameDao.save(newHistory.getGame());
        profileDao.save(newHistory.getProfile());
        histDao.save(newHistory);
        assertThat(newHistory.getId()).isNotNull();
        logger.info("Saved history id=" + newHistory.getId());

        // refresh needed due to new histories saved after game saved
        OnlineGame gameFetch = newHistory.getGame();
        gameDao.refresh(gameFetch);

        // get histories
        List<TournamentHistory> histories = gameFetch.getHistories();
        logger.info("****** # Histories: " + histories.size());
        for (TournamentHistory hh : histories) {
            logger.info("History: " + hh);
        }
        assertThat(histories).contains(newHistory);

        // delete game from old game
        gameDao.delete(newHistory.getGame());

        // history should be deleted
        TournamentHistory deleted = histDao.get(newHistory.getId());
        assertThat(deleted).isNull();

        // profile should still be there
        OnlineProfile profile = profileDao.get(newHistory.getProfile().getId());
        assertThat(profile).isNotNull();
    }

    @Test
    @Rollback
    void should_PurgeOldGames_When_PurgeCalled() {
        int gameCount = 10;
        int histCount = 0;
        for (int i = 1; i <= gameCount; i++) {
            OnlineProfile profile = PokerTestData.createOnlineProfile("Dexter" + i);
            OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), i, "XXX-" + (100 + i));
            game.setMode(i % 2 == 0 ? OnlineGame.MODE_REG : OnlineGame.MODE_PLAY);

            gameDao.save(game);
            profileDao.save(profile);

            for (int j = 1; j <= 8; j++) {
                TournamentHistory hist = PokerTestData.createTournamentHistory(game, profile, "Zorro" + j);
                histDao.save(hist);
                histCount++;
            }
        }

        // count should match
        assertThat(histDao.getAll()).hasSize(histCount);

        // sleep a bit and get new date
        Utils.sleepSeconds(3);
        Date now = new Date();

        // purge half the games
        gameDao.purge(now, OnlineGame.MODE_REG);

        // verify half are gone
        assertThat(histDao.getAll()).hasSize(histCount / 2);
        List<OnlineGame> games = gameDao.getAll();
        assertThat(games).hasSize(gameCount / 2);

        // verify no MODE_REG left
        for (OnlineGame game : games) {
            assertThat(game.getMode()).isNotEqualTo(OnlineGame.MODE_REG);
        }

        // purge other half
        gameDao.purge(now, OnlineGame.MODE_PLAY);
        assertThat(histDao.getAll()).isEmpty();
        assertThat(gameDao.getAll()).isEmpty();
    }

    @Test
    @Rollback
    void should_FindGame_When_SearchingByHistoryId() {
        TournamentHistory newHistory = PokerTestData.createTournamentHistory("TEST shouldPersist", 5, "AAA-999");
        gameDao.save(newHistory.getGame());
        profileDao.save(newHistory.getProfile());
        histDao.save(newHistory);

        OnlineGame game = gameDao.getByTournamentHistoryId(newHistory.getId());
        assertThat(game.getId()).isEqualTo(newHistory.getGame().getId());

        game = gameDao.getByTournamentHistoryId(-1L);
        assertThat(game).isNull();
    }

    @Test
    @Rollback
    void should_DeleteOnlySpecificGameHistories_When_DeleteAllForGameCalled() {
        int gameCount = 2;
        int histCount = 0;
        OnlineGame game1 = null;
        OnlineGame game2 = null;
        for (int i = 1; i <= gameCount; i++) {
            OnlineProfile profile = PokerTestData.createOnlineProfile("Dexter" + i);
            OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), i, "XXX-" + (100 + i));
            game.setMode(i % 2 == 0 ? OnlineGame.MODE_REG : OnlineGame.MODE_PLAY);
            if (game1 == null)
                game1 = game;
            else
                game2 = game;

            gameDao.save(game);
            profileDao.save(profile);

            for (int j = 1; j <= 8; j++) {
                TournamentHistory hist = PokerTestData.createTournamentHistory(game, profile, "Zorro" + j);
                histDao.save(hist);
                histCount++;
            }
        }

        histDao.deleteAllForGame(game1);
        gameDao.refresh(game1);
        gameDao.refresh(game2);

        // no histories for game1
        assertThat(game1.getHistories()).isEmpty();

        // fetch histories for game2
        assert game2 != null;
        assertThat(game2.getHistories()).hasSize(histCount / 2);

        // count of remaining histories should be half history count
        List<TournamentHistory> hists = histDao.getAll();
        assertThat(hists).hasSize(histCount / 2);

        // no remaining history should match game 1
        for (TournamentHistory hist : hists) {
            assertThat(hist.getGame().getId()).isNotEqualTo(game1.getId());
        }
    }

    @Test
    @Rollback
    void should_FilterHistories_When_QueryingByProfileOrGame() {
        int gameCount = 2;
        int histCount = 0;
        OnlineProfile one = PokerTestData.createOnlineProfile("Dexter");
        OnlineProfile two = PokerTestData.createOnlineProfile("Zorro");
        profileDao.save(one);
        profileDao.save(two);
        OnlineGame game1 = null;
        OnlineGame game2 = null;
        for (int i = 1; i <= gameCount; i++) {
            OnlineProfile profile = (i % 2 == 0) ? one : two;
            OnlineGame game = PokerTestData.createOnlineGame(profile.getName(), i, "XXX-" + (100 + i));
            game.setMode(OnlineGame.MODE_END);
            if (game1 == null)
                game1 = game;
            else
                game2 = game;

            gameDao.save(game);

            for (int j = 1; j <= 8; j++) {
                TournamentHistory hist = PokerTestData.createTournamentHistory(game, profile, profile.getName());
                histDao.save(hist);
                histCount++;
            }
        }

        TournamentHistoryList list;
        int count = histDao.getAllForProfileCount(one.getId(), null, null, null);
        list = histDao.getAllForProfile(count, 0, histCount, one.getId(), null, null, null);
        assertThat(list).hasSize(histCount / 2);
        for (TournamentHistory hist : list) {
            assertThat(hist.getPlayerName()).isEqualTo(one.getName());
        }

        list = histDao.getAllForGame(null, 0, histCount, game1.getId());
        assertThat(list).hasSize(histCount / 2);
        for (TournamentHistory hist : list) {
            assertThat(hist.getGame().getId()).isEqualTo(game1.getId());
        }

        assert game2 != null;
        list = histDao.getAllForGame(null, 0, histCount, game2.getId());
        assertThat(histDao.getAllForGameCount(game2.getId())).isEqualTo(histCount / 2);
        assertThat(list).hasSize(histCount / 2);
        for (TournamentHistory hist : list) {
            assertThat(hist.getGame().getId()).isEqualTo(game2.getId());
        }
    }

    @Test
    @Rollback
    void should_ExecuteLeaderboardQuery_When_Called() {
        // just verify the query still works - kind of hard to verify data
        int games_limit = 5;
        Date begin = new Date(0);
        Date end = new Date();
        int count = histDao.getLeaderboardCount(games_limit, null, begin, end);
        histDao.getLeaderboard(count, 0, 20, true, games_limit, null, begin, end);
    }
}
