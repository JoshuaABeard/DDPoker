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
package com.donohoedigital.games.poker.service.impl;

import com.donohoedigital.games.poker.dao.OnlineGameDao;
import com.donohoedigital.games.poker.dao.OnlineProfileDao;
import com.donohoedigital.games.poker.dao.TournamentHistoryDao;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.util.OnlineGameList;
import com.donohoedigital.games.poker.model.util.TournamentHistoryList;
import com.donohoedigital.games.poker.service.OnlineGameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OnlineGameServiceImpl with mocked DAOs.
 */
@ExtendWith(MockitoExtension.class)
class OnlineGameServiceImplUnitTest {

    @Mock
    private OnlineGameDao gameDao;

    @Mock
    private OnlineProfileDao profileDao;

    @Mock
    private TournamentHistoryDao histDao;

    private OnlineGameServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OnlineGameServiceImpl();
        service.setOnlineGameDao(gameDao);
        service.setOnlineProfileDao(profileDao);
        service.setTournamentHistoryDao(histDao);
    }

    // ========================================
    // saveOnlineGame
    // ========================================

    @Test
    void should_SaveGame_When_NoExistingGame() {
        OnlineGame game = createGame("poker://host/1/pass");
        when(gameDao.getByUrl("poker://host/1/pass")).thenReturn(null);

        service.saveOnlineGame(game);

        verify(gameDao).save(game);
        verify(gameDao, never()).delete(any());
    }

    @Test
    void should_DeleteExistingAndSave_When_GameAlreadyExists() {
        OnlineGame existing = createGame("poker://host/1/pass");
        OnlineGame newGame = createGame("poker://host/1/pass");
        when(gameDao.getByUrl("poker://host/1/pass")).thenReturn(existing);

        service.saveOnlineGame(newGame);

        verify(gameDao).delete(existing);
        verify(gameDao).flush();
        verify(gameDao).save(newGame);
    }

    // ========================================
    // deleteOnlineGame
    // ========================================

    @Test
    void should_DeleteGame_When_GameExists() {
        OnlineGame game = createGame("poker://host/2/pass");
        OnlineGame existing = createGame("poker://host/2/pass");
        when(gameDao.getByUrl("poker://host/2/pass")).thenReturn(existing);

        service.deleteOnlineGame(game);

        verify(gameDao).delete(existing);
    }

    @Test
    void should_DoNothing_When_DeletingNonexistentGame() {
        OnlineGame game = createGame("poker://host/99/pass");
        when(gameDao.getByUrl("poker://host/99/pass")).thenReturn(null);

        service.deleteOnlineGame(game);

        verify(gameDao, never()).delete(any());
    }

    // ========================================
    // purgeGames
    // ========================================

    @Test
    void should_DelegateToDao_When_PurgingGames() {
        Date cutoff = new Date();
        when(gameDao.purge(cutoff, 1)).thenReturn(5);

        int purged = service.purgeGames(cutoff, 1);

        assertThat(purged).isEqualTo(5);
        verify(gameDao).purge(cutoff, 1);
    }

    // ========================================
    // getOnlineGameById / getOnlineGameByUrl
    // ========================================

    @Test
    void should_DelegateToDao_When_GetById() {
        OnlineGame expected = createGame("poker://host/3/pass");
        when(gameDao.get(7L)).thenReturn(expected);

        OnlineGame result = service.getOnlineGameById(7L);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void should_DelegateToDao_When_GetByUrl() {
        OnlineGame expected = createGame("poker://host/4/pass");
        when(gameDao.getByUrl("poker://host/4/pass")).thenReturn(expected);

        OnlineGame result = service.getOnlineGameByUrl("poker://host/4/pass");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void should_DelegateToDao_When_GetByTournamentHistoryId() {
        OnlineGame expected = createGame("poker://host/5/pass");
        when(gameDao.getByTournamentHistoryId(11L)).thenReturn(expected);

        OnlineGame result = service.getOnlineGameByTournamentHistoryId(11L);

        assertThat(result).isSameAs(expected);
    }

    // ========================================
    // getOnlineGamesCount / getOnlineGames
    // ========================================

    @Test
    void should_DelegateToDao_When_GetOnlineGamesCount() {
        Integer[] modes = {1, 2};
        Date begin = new Date();
        Date end = new Date();
        when(gameDao.getByModeCount(modes, "host", begin, end)).thenReturn(3);

        int count = service.getOnlineGamesCount(modes, "host", begin, end);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void should_DelegateToDao_When_GetOnlineGames() {
        Integer[] modes = {1};
        Date begin = new Date();
        Date end = new Date();
        OnlineGameList expected = new OnlineGameList();
        when(gameDao.getByMode(null, 0, 10, modes, null, begin, end, false)).thenReturn(expected);

        OnlineGameList result = service.getOnlineGames(null, 0, 10, modes, null, begin, end,
                OnlineGameService.OrderByType.date);

        assertThat(result).isSameAs(expected);
    }

    // ========================================
    // updateOnlineGame (simple)
    // ========================================

    @Test
    void should_MergeAndUpdate_When_GameExists() {
        OnlineGame existing = createGame("poker://host/6/pass");
        existing.setId(10L);
        OnlineGame incoming = createGame("poker://host/6/pass");

        when(gameDao.getByUrl("poker://host/6/pass")).thenReturn(existing);
        when(gameDao.update(existing)).thenReturn(existing);

        OnlineGame result = service.updateOnlineGame(incoming);

        assertThat(result).isSameAs(existing);
        verify(gameDao).update(existing);
    }

    @Test
    void should_ReturnNull_When_UpdatingNonexistentGame() {
        OnlineGame incoming = createGame("poker://host/7/pass");
        when(gameDao.getByUrl("poker://host/7/pass")).thenReturn(null);

        OnlineGame result = service.updateOnlineGame(incoming);

        assertThat(result).isNull();
        verify(gameDao, never()).update(any());
    }

    // ========================================
    // updateOnlineGame with TournamentHistoryList
    // ========================================

    @Test
    void should_InsertHistories_When_UpdatingGameWithHistoryList() {
        OnlineGame existing = createGame("poker://host/8/pass");
        existing.setId(20L);
        existing.setMode(OnlineGame.MODE_END);

        OnlineGame incoming = createGame("poker://host/8/pass");

        when(gameDao.getByUrl("poker://host/8/pass")).thenReturn(existing);
        when(gameDao.update(existing)).thenReturn(existing);

        // Build history list with an online player and an AI player
        TournamentHistoryList histList = new TournamentHistoryList();

        TournamentHistory onlineHist = new TournamentHistory();
        onlineHist.setPlayerName("OnlinePlayer");
        onlineHist.setPlayerType(TournamentHistory.PLAYER_TYPE_ONLINE);
        onlineHist.setId(100L); // client-side id should be cleared
        histList.add(onlineHist);

        TournamentHistory aiHist = new TournamentHistory();
        aiHist.setPlayerName("AIBot");
        aiHist.setPlayerType(TournamentHistory.PLAYER_TYPE_AI);
        aiHist.setId(101L);
        histList.add(aiHist);

        OnlineProfile onlineProfile = new OnlineProfile();
        onlineProfile.setName("OnlinePlayer");
        when(profileDao.getByName("OnlinePlayer")).thenReturn(onlineProfile);

        OnlineProfile aiBest = new OnlineProfile();
        when(profileDao.getDummy(OnlineProfile.Dummy.AI_BEST)).thenReturn(aiBest);

        OnlineGame result = service.updateOnlineGame(incoming, histList);

        assertThat(result).isSameAs(existing);
        verify(histDao).deleteAllForGame(existing);
        verify(histDao, times(2)).save(any(TournamentHistory.class));
    }

    @Test
    void should_AssignDummyProfiles_When_AIAndLocalPlayers() {
        OnlineGame existing = createGame("poker://host/9/pass");
        existing.setId(30L);
        existing.setMode(OnlineGame.MODE_PLAY);

        OnlineGame incoming = createGame("poker://host/9/pass");

        when(gameDao.getByUrl("poker://host/9/pass")).thenReturn(existing);
        when(gameDao.update(existing)).thenReturn(existing);

        TournamentHistoryList histList = new TournamentHistoryList();

        // First AI gets AI_BEST
        TournamentHistory ai1 = new TournamentHistory();
        ai1.setPlayerName("AI1");
        ai1.setPlayerType(TournamentHistory.PLAYER_TYPE_AI);
        histList.add(ai1);

        // Second AI gets AI_REST
        TournamentHistory ai2 = new TournamentHistory();
        ai2.setPlayerName("AI2");
        ai2.setPlayerType(TournamentHistory.PLAYER_TYPE_AI);
        histList.add(ai2);

        // Local player gets HUMAN dummy
        TournamentHistory local = new TournamentHistory();
        local.setPlayerName("Local");
        local.setPlayerType(TournamentHistory.PLAYER_TYPE_LOCAL);
        histList.add(local);

        OnlineProfile aiBest = new OnlineProfile();
        OnlineProfile aiRest = new OnlineProfile();
        OnlineProfile human = new OnlineProfile();
        when(profileDao.getDummy(OnlineProfile.Dummy.AI_BEST)).thenReturn(aiBest);
        when(profileDao.getDummy(OnlineProfile.Dummy.AI_REST)).thenReturn(aiRest);
        when(profileDao.getDummy(OnlineProfile.Dummy.HUMAN)).thenReturn(human);

        service.updateOnlineGame(incoming, histList);

        verify(histDao, times(3)).save(any(TournamentHistory.class));
        verify(profileDao).getDummy(OnlineProfile.Dummy.AI_BEST);
        verify(profileDao).getDummy(OnlineProfile.Dummy.AI_REST);
        verify(profileDao).getDummy(OnlineProfile.Dummy.HUMAN);
    }

    @Test
    void should_ForceLocalType_When_OnlinePlayerProfileNotFound() {
        OnlineGame existing = createGame("poker://host/10/pass");
        existing.setId(40L);
        existing.setMode(OnlineGame.MODE_END);

        OnlineGame incoming = createGame("poker://host/10/pass");

        when(gameDao.getByUrl("poker://host/10/pass")).thenReturn(existing);
        when(gameDao.update(existing)).thenReturn(existing);

        TournamentHistoryList histList = new TournamentHistoryList();

        TournamentHistory orphan = new TournamentHistory();
        orphan.setPlayerName("OrphanPlayer");
        orphan.setPlayerType(TournamentHistory.PLAYER_TYPE_ONLINE);
        histList.add(orphan);

        when(profileDao.getByName("OrphanPlayer")).thenReturn(null);
        OnlineProfile human = new OnlineProfile();
        when(profileDao.getDummy(OnlineProfile.Dummy.HUMAN)).thenReturn(human);

        service.updateOnlineGame(incoming, histList);

        // Orphan online player should be forced to LOCAL and get HUMAN dummy
        verify(profileDao).getDummy(OnlineProfile.Dummy.HUMAN);
        verify(histDao).save(any(TournamentHistory.class));
    }

    @Test
    void should_SkipHistories_When_ListIsNull() {
        OnlineGame existing = createGame("poker://host/11/pass");
        OnlineGame incoming = createGame("poker://host/11/pass");

        when(gameDao.getByUrl("poker://host/11/pass")).thenReturn(existing);
        when(gameDao.update(existing)).thenReturn(existing);

        OnlineGame result = service.updateOnlineGame(incoming, null);

        assertThat(result).isSameAs(existing);
        verify(histDao, never()).deleteAllForGame(any());
        verify(histDao, never()).save(any());
    }

    @Test
    void should_SkipHistories_When_ListIsEmpty() {
        OnlineGame existing = createGame("poker://host/12/pass");
        OnlineGame incoming = createGame("poker://host/12/pass");

        when(gameDao.getByUrl("poker://host/12/pass")).thenReturn(existing);
        when(gameDao.update(existing)).thenReturn(existing);

        service.updateOnlineGame(incoming, new TournamentHistoryList());

        verify(histDao, never()).deleteAllForGame(any());
        verify(histDao, never()).save(any());
    }

    @Test
    void should_ReturnNull_When_UpdatingWithHistoriesButGameNotFound() {
        OnlineGame incoming = createGame("poker://host/13/pass");
        when(gameDao.getByUrl("poker://host/13/pass")).thenReturn(null);

        TournamentHistoryList histList = new TournamentHistoryList();
        histList.add(new TournamentHistory());

        OnlineGame result = service.updateOnlineGame(incoming, histList);

        assertThat(result).isNull();
        verify(histDao, never()).save(any());
    }

    // ========================================
    // Host summary delegation
    // ========================================

    @Test
    void should_DelegateToDao_When_GetHostSummaryCount() {
        Date begin = new Date();
        Date end = new Date();
        when(gameDao.getHostSummaryCount("host", begin, end)).thenReturn(7);

        int count = service.getHostSummaryCount("host", begin, end);

        assertThat(count).isEqualTo(7);
    }

    // ========================================
    // Helper
    // ========================================

    private OnlineGame createGame(String url) {
        TournamentProfile tp = new TournamentProfile("Test Tournament");
        OnlineGame game = new OnlineGame();
        game.setHostPlayer("Host");
        game.setMode(OnlineGame.MODE_PLAY);
        game.setUrl(url);
        game.setTournament(tp);
        game.setStartDate(new Date());
        game.setEndDate(new Date());
        return game;
    }
}
