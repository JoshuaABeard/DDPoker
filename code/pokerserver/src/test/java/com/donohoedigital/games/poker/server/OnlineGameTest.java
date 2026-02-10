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
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.db.PagedList;
import com.donohoedigital.games.poker.dao.OnlineGameDao;
import com.donohoedigital.games.poker.dao.OnlineProfileDao;
import com.donohoedigital.games.poker.model.HostSummary;
import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.model.util.OnlineGameList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;


/**
 * Tests for OnlineGame persistence and DAO operations.
 */
@Tag("slow")
@SpringJUnitConfig(locations = {"/app-context-pokerservertests.xml"})
@Transactional
class OnlineGameTest
{
    private final Logger logger = LogManager.getLogger(OnlineGameTest.class);

    @Autowired
    private OnlineGameDao dao;

    @Autowired
    private OnlineProfileDao profileDao;

    @Test
    @Rollback
    void should_PersistAndUpdate_When_GameSaved()
    {
        OnlineGame newGame = PokerTestData.createOnlineGame("shouldPersist", 1, "XXX-333");
        dao.save(newGame);

        assertThat(newGame.getId()).isNotNull();

        OnlineGame fetch = dao.get(newGame.getId());
        assertThat(fetch.getLicenseKey()).isEqualTo(newGame.getLicenseKey());

        String key = "1111-1111-1111-1111";
        newGame.setLicenseKey(key);
        dao.update(newGame);

        OnlineGame updated = dao.get(newGame.getId());
        assertThat(updated.getLicenseKey()).isEqualTo(key);
    }

    @Test
    @Rollback
    void should_FindGame_When_SearchingByKeyAndUrl()
    {
        OnlineGame newGame = PokerTestData.createOnlineGame("shouldPersist", 1, "XXX-333");
        dao.save(newGame);

        OnlineGame fetch = dao.getByKeyAndUrl(newGame.getLicenseKey(), newGame.getUrl());

        assertThat(fetch).isNotNull();
        assertThat(fetch.getId()).isEqualTo(newGame.getId());

        fetch = dao.getByKeyAndUrl(newGame.getLicenseKey(), newGame.getUrl() + "/EXTRA");
        assertThat(fetch).isNull();
    }

    @Test
    @Rollback
    void should_DeleteGame_When_DeleteCalled()
    {
        OnlineGame game = PokerTestData.createOnlineGame("saveBeforeDelete", 2, "YYY-444");
        dao.save(game);
        assertThat(game.getId()).isNotNull();
        logger.info(game.getHostPlayer() + " saved with id " + game.getId());

        dao.delete(game); // test delete of unattached entity
        logger.info("Should have deleted profile with id " + game.getId());

        OnlineGame deleted = dao.get(game.getId());
        assertThat(deleted).isNull();
    }

    @Test
    @Rollback
    void should_PageCorrectly_When_FetchingGamesByMode()
    {
        new ConfigManager("poker", ApplicationType.COMMAND_LINE);
        final int gameCount = 12;
        assertThat(gameCount % 4).isEqualTo(0); // must be divisible by 4
        long now = System.currentTimeMillis();
        int reverse;
        int day = 1000 * 60 * 24;

        // create some games
        for (int i = 1; i <= gameCount; i++)
        {
            reverse = gameCount - i + 1;
            // create so host name is a number which sort in reverse of order created
            OnlineGame og = PokerTestData.createOnlineGame("" + (reverse + 100), i, "XXX-" + (100 + i));
            int mode = (i % 4) + 1; // alternate over OnlineGame mode
            og.setMode(mode);
            og.setStartDate(new Date(now + ((i % 4 + 1) * day)));
            og.setEndDate(new Date(now + (reverse * day * 3)));

            Utils.sleepMillis(1000); // ensure different dates but TODO: review as this makes this test slow
            dao.save(og);
        }

        // test fetch of all and sort by mode
        int pagesize = 7;
        int fetched = 0;
        int index = 0;
        int expectedCount = gameCount;
        Integer[] modes = {OnlineGame.MODE_REG, OnlineGame.MODE_PLAY, OnlineGame.MODE_STOP, OnlineGame.MODE_END};
        int count = dao.getByModeCount(modes, null, null, null);
        while (fetched < gameCount)
        {
            OnlineGameList fetch = dao.getByMode(count, fetched, pagesize, modes, null, null, null, true);
            fetched += fetch.size();

            assertThat(fetch.getTotalSize()).isEqualTo(expectedCount);
            assertThat(fetch.size()).isLessThanOrEqualTo(pagesize);
            if (fetch.isEmpty()) break;

            // sorting by mode first, verify that first half are REG
            for (OnlineGame og : fetch)
            {
                if (index < (gameCount / 4))
                {
                    assertThat(og.getMode()).isEqualTo(OnlineGame.MODE_REG);
                }
                else if (index < (gameCount / 2))
                {
                    assertThat(og.getMode()).isEqualTo(OnlineGame.MODE_PLAY);
                }
                else if (index < (gameCount * 3 / 4))
                {
                    assertThat(og.getMode()).isEqualTo(OnlineGame.MODE_STOP);
                }
                else
                {
                    assertThat(og.getMode()).isEqualTo(OnlineGame.MODE_END);
                }

                index++;
            }
        }

        assertThat(fetched).isEqualTo(gameCount);

        // test fetch of just end/stop and sort normally
        pagesize = 4;
        fetched = 0;
        Integer[] modes2 = {OnlineGame.MODE_END, OnlineGame.MODE_STOP};
        List<Integer> modes2x = Arrays.asList(modes2);
        int lastName = 0;
        long lastDate = Long.MAX_VALUE;
        expectedCount = gameCount / 2;
        count = dao.getByModeCount(modes2, null, null, null);
        while (fetched < expectedCount)
        {
            OnlineGameList fetch = dao.getByMode(count, fetched, pagesize, modes2, null, null, null, false);
            fetched += fetch.size();

            assertThat(fetch.getTotalSize()).isEqualTo(expectedCount);
            assertThat(fetch.size()).isLessThanOrEqualTo(pagesize);
            if (fetch.isEmpty()) break;

            // sorting by mode first, verify that first half are REG
            for (OnlineGame og : fetch)
            {
                assertThat(modes2x).contains(og.getMode());

                int name = Integer.parseInt(og.getHostPlayer());
                long date = og.getEndDate().getTime();

                assertThat(date).isLessThanOrEqualTo(lastDate);
                if (date == lastDate) assertThat(name).isGreaterThan(lastName);

                lastName = name;
                lastDate = date;
            }
        }

        assertThat(fetched).isEqualTo(expectedCount);

        // test fetch of just each type
        for (int i = OnlineGame.MODE_REG; i <= OnlineGame.MODE_END; i++)
        {
            pagesize = 3;
            fetched = 0;
            Integer[] modes3 = {i};
            List<Integer> modes3x = Arrays.asList(modes3);
            lastDate = Long.MAX_VALUE;
            expectedCount = gameCount / 4;
            count = dao.getByModeCount(modes3, null, null, null);
            while (fetched < expectedCount)
            {
                OnlineGameList fetch = dao.getByMode(count, fetched, pagesize, modes3, null, null, null, false);
                fetched += fetch.size();

                assertThat(fetch.getTotalSize()).isEqualTo(expectedCount);
                assertThat(fetch.size()).isLessThanOrEqualTo(pagesize);
                if (fetch.isEmpty()) break;

                // verify ordering correct
                for (OnlineGame og : fetch)
                {
                    assertThat(modes3x).contains(og.getMode());

                    long date;

                    switch (modes3[0])
                    {
                        case OnlineGame.MODE_PLAY:
                            date = og.getStartDate().getTime();
                            break;

                        case OnlineGame.MODE_END:
                        case OnlineGame.MODE_STOP:
                            date = og.getEndDate().getTime();
                            break;

                        case OnlineGame.MODE_REG:
                        default:
                            date = og.getCreateDate().getTime();
                    }

                    assertThat(date).isLessThanOrEqualTo(lastDate);

                    lastDate = date;
                }
            }

            assertThat(fetched).isEqualTo(expectedCount);
        }
    }

    @Test
    @Rollback
    void should_GenerateHostSummary_When_MultipleGamesHosted()
    {
        Date now = new Date(System.currentTimeMillis() - 1000);
        Date later = new Date(System.currentTimeMillis() + 30000);
        int games = 20;
        for (int i = 0; i < games; i++)
        {
            OnlineGame sample = PokerTestData.createOnlineGame(i % 4 == 0 ? "Dexter" : "Zorro", i, "blah");
            dao.save(sample);
        }

        profileDao.save(PokerTestData.createOnlineProfile("Dexter"));
        profileDao.save(PokerTestData.createOnlineProfile("Zorro"));

        int count = dao.getHostSummaryCount(null, now, later);
        assertThat(count).isEqualTo(2);

        PagedList<HostSummary> list = dao.getHostSummary(count, 0, games, null, now, later);
        assertThat(list).hasSize(count);

        HostSummary one = list.get(0);
        HostSummary two = list.get(1);

        assertThat(one.getHostName()).isEqualTo("Zorro");
        assertThat(one.getGamesHosted()).isEqualTo(15);

        assertThat(two.getHostName()).isEqualTo("Dexter");
        assertThat(two.getGamesHosted()).isEqualTo(5);
    }

}
