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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.model.TournamentHistory;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerDatabase - HSQLDB integration, hand history storage,
 * tournament persistence, and database lifecycle management.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PokerDatabaseTest {

    @TempDir
    static Path tempDir;

    private PlayerProfile profile;
    private File dbTempDir;

    @BeforeAll
    void setUpAll() throws IOException {
        Utils.setVersionString("-db-test");
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create player profile
        profile = new PlayerProfile("poker-database-test");
        profile.setEmail("test@test.com");
        profile.setName("test");
        profile.initFile();

        // Create database in temp directory
        dbTempDir = tempDir.resolve("poker-database-test").toFile();
        dbTempDir.mkdirs();
        PokerDatabase.init(profile, dbTempDir);
    }

    @AfterAll
    void tearDownAll() {
        PokerDatabase.shutdownDatabase();
    }

    // =================================================================
    // Database Lifecycle Tests
    // =================================================================

    @Test
    void should_InitializeDatabase_When_InitCalled() {
        assertThatCode(() -> PokerDatabase.testConnection()).doesNotThrowAnyException();
    }

    @Test
    void should_GetDatabase_When_DatabaseInitialized() {
        assertThat(PokerDatabase.getDatabase()).isNotNull();
    }

    @Test
    void should_TestConnection_When_DatabaseActive() {
        assertThatCode(() -> PokerDatabase.testConnection()).doesNotThrowAnyException();
    }

    // =================================================================
    // Hand History Storage Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveHandHistory_When_UsingHSQLDB() {
        PokerGame game = createGame();
        PokerPlayer player = createPlayer("test-player", game);
        HoldemHand hand = createHand(game, player);
        hand.setAnte(5);

        int id = hand.storeHandHistoryDB();
        String[] html = PokerDatabase.getHandAsHTML(id, true, true);

        assertThat(html).isNotNull().isNotEmpty();
        assertThat(html[0]).isEqualTo("<HTML><B>Hand 0 - Table 1</B></HTML>");
    }

    @Test
    void should_ReturnValidHandID_When_HandStored() {
        PokerGame game = createGame();
        PokerPlayer player = createPlayer("test-player2", game);
        HoldemHand hand = createHand(game, player);
        hand.setAnte(10);

        int id = hand.storeHandHistoryDB();

        assertThat(id).isGreaterThan(0);
    }

    @Test
    void should_RetrieveHandHTML_When_HandExists() {
        PokerGame game = createGame();
        PokerPlayer player = createPlayer("test-player3", game);
        HoldemHand hand = createHand(game, player);
        hand.setAnte(20);
        int id = hand.storeHandHistoryDB();

        String[] html = PokerDatabase.getHandAsHTML(id, false, false);

        assertThat(html).isNotNull();
        assertThat(html).isNotEmpty();
    }

    // =================================================================
    // Tournament Persistence Tests
    // =================================================================

    @Test
    void should_StoreTournament_When_GameProvided() {
        PokerGame game = createGame();
        int tournamentId = PokerDatabase.storeTournament(game);

        assertThat(tournamentId).isGreaterThan(0);
    }

    @Test
    void should_StoreTournamentFinish_When_PlayerFinishes() {
        PokerGame game = createGame();
        PokerPlayer player = createPlayer("finish-player", game);
        int tournamentId = PokerDatabase.storeTournament(game);
        player.setPlace(1);
        player.setPrize(1000);

        int finishId = PokerDatabase.storeTournamentFinish(game, player);

        assertThat(finishId).isGreaterThan(0);
    }

    @Test
    void should_RetrieveTournamentHistory_When_TournamentsStored() {
        PokerGame game = createGame();
        PokerDatabase.storeTournament(game);

        List<TournamentHistory> history = PokerDatabase.getTournamentHistory(profile);

        assertThat(history).isNotNull();
    }

    @Test
    void should_GetOverallHistory_When_TournamentsStored() {
        PokerGame game = createGame();
        PokerPlayer player = createPlayer("overall-player", game);
        PokerDatabase.storeTournament(game);
        player.setPlace(1);
        player.setPrize(1000);
        PokerDatabase.storeTournamentFinish(game, player);

        TournamentHistory overall = PokerDatabase.getOverallHistory(profile);

        assertThat(overall).isNotNull();
        assertThat(overall.getTournamentName()).contains("ALL");
    }

    @Test
    void should_DeleteTournament_When_TournamentExists() {
        PokerGame game = createGame();
        int tournamentId = PokerDatabase.storeTournament(game);
        TournamentHistory hist = new TournamentHistory();
        hist.setId((long) tournamentId);

        assertThatCode(() -> PokerDatabase.deleteTournament(hist)).doesNotThrowAnyException();
    }

    // =================================================================
    // Practice Hand Tests
    // =================================================================

    @Test
    void should_CheckPracticeHand_When_HandIDProvided() {
        PokerGame game = createGame();
        PokerPlayer player = createPlayer("practice-player", game);
        HoldemHand hand = createHand(game, player);
        int id = hand.storeHandHistoryDB();

        boolean isPractice = PokerDatabase.isPracticeHand(id);

        assertThat(isPractice).isIn(true, false);
    }

    // =================================================================
    // Test Helper Methods
    // =================================================================

    private PokerGame createGame() {
        PokerGame game = new PokerGame(null);
        TournamentProfile tournament = new TournamentProfile("test-" + System.currentTimeMillis());
        game.setProfile(tournament);
        return game;
    }

    private PokerPlayer createPlayer(String name, PokerGame game) {
        PokerPlayer player = new PokerPlayer(PokerPlayer.HOST_ID, name, true);
        game.addPlayer(player);
        return player;
    }

    private HoldemHand createHand(PokerGame game, PokerPlayer player) {
        PokerTable table = new PokerTable(game, 1);
        table.addPlayer(player);
        return new HoldemHand(table);
    }
}
