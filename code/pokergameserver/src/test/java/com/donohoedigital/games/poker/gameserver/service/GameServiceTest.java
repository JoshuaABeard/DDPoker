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
package com.donohoedigital.games.poker.gameserver.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.GameServerException;
import com.donohoedigital.games.poker.gameserver.dto.CommunityGameRegisterRequest;
import com.donohoedigital.games.poker.gameserver.dto.GameJoinResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameListResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.entity.GameInstanceEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameEventRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameInstanceRepository;

@DataJpaTest
@ContextConfiguration(classes = {TestJpaConfiguration.class, GameServiceTest.TestConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameInstanceRepository gameInstanceRepository;

    @org.springframework.context.annotation.Configuration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public GameService gameService(GameInstanceRepository gameInstanceRepository,
                GameEventRepository gameEventRepository) {
            return new GameService(gameInstanceRepository, gameEventRepository);
        }
    }

    // =========================================================================
    // createGame
    // =========================================================================

    @Test
    void testCreateGame() {
        GameConfig config = createTestConfig();

        String gameId = gameService.createGame(config, 1L, "testowner");

        assertThat(gameId).isNotNull();
        GameInstanceEntity instance = gameInstanceRepository.findById(gameId).orElse(null);
        assertThat(instance).isNotNull();
        assertThat(instance.getName()).isEqualTo("Test Game");
        assertThat(instance.getOwnerName()).isEqualTo("testowner");
        assertThat(instance.getOwnerProfileId()).isEqualTo(1L);
        assertThat(instance.getMaxPlayers()).isEqualTo(9);
        assertThat(instance.getHostingType()).isEqualTo("SERVER");
        assertThat(instance.getWsUrl()).isNotNull().contains("/ws/games/");
    }

    // =========================================================================
    // registerCommunityGame
    // =========================================================================

    @Test
    void testRegisterCommunityGame_createsEntityWithCommunityType() {
        CommunityGameRegisterRequest req = new CommunityGameRegisterRequest("Alice's Game",
                "ws://203.0.113.42:8765/ws/games/local-uuid", null, null);

        GameSummary summary = gameService.registerCommunityGame(1L, "alice", req);

        assertThat(summary).isNotNull();
        assertThat(summary.gameId()).isNotNull();
        assertThat(summary.hostingType()).isEqualTo("COMMUNITY");
        assertThat(summary.wsUrl()).isEqualTo("ws://203.0.113.42:8765/ws/games/local-uuid");
        assertThat(summary.ownerName()).isEqualTo("alice");

        GameInstanceEntity entity = gameInstanceRepository.findById(summary.gameId()).orElse(null);
        assertThat(entity).isNotNull();
        assertThat(entity.getHostingType()).isEqualTo("COMMUNITY");
        assertThat(entity.getWsUrl()).isEqualTo("ws://203.0.113.42:8765/ws/games/local-uuid");
        assertThat(entity.getStatus()).isEqualTo(GameInstanceState.WAITING_FOR_PLAYERS);
    }

    @Test
    void testRegisterCommunityGame_withPassword_hashesPasswordAndHidesWsUrl() {
        CommunityGameRegisterRequest req = new CommunityGameRegisterRequest("Private Game",
                "ws://203.0.113.42:8765/ws/games/abc", null, "secret");

        GameSummary summary = gameService.registerCommunityGame(1L, "alice", req);

        assertThat(summary.isPrivate()).isTrue();
        assertThat(summary.wsUrl()).isNull(); // URL hidden for private games in list view
        GameInstanceEntity entity = gameInstanceRepository.findById(summary.gameId()).orElse(null);
        assertThat(entity).isNotNull();
        assertThat(entity.getPasswordHash()).isNotNull().isNotEqualTo("secret");
    }

    // =========================================================================
    // listGames
    // =========================================================================

    @Test
    void testListGames_defaultFiltersReturnOnlyActiveGames() {
        gameService.createGame(createTestConfig("Game 1"), 1L, "owner1"); // WAITING_FOR_PLAYERS
        gameService.createGame(createTestConfig("Game 2"), 2L, "owner2"); // WAITING_FOR_PLAYERS

        GameListResponse response = gameService.listGames(null, null, null, 0, 50);

        assertThat(response.games()).hasSize(2);
        assertThat(response.total()).isEqualTo(2);
    }

    @Test
    void testListGames_completedGamesExcludedByDefault() {
        String gameId = gameService.createGame(createTestConfig(), 1L, "owner");
        // Manually mark completed
        gameInstanceRepository.updateStatusWithCompletionTime(gameId, GameInstanceState.COMPLETED,
                java.time.Instant.now());

        GameListResponse response = gameService.listGames(null, null, null, 0, 50);

        assertThat(response.games()).isEmpty();
    }

    @Test
    void testListGames_filterByHostingType() {
        gameService.createGame(createTestConfig("Server Game"), 1L, "owner");
        gameService.registerCommunityGame(2L, "host",
                new CommunityGameRegisterRequest("Community Game", "ws://1.2.3.4:8765/ws/games/x", null, null));

        GameListResponse serverOnly = gameService.listGames(null, "SERVER", null, 0, 50);
        GameListResponse communityOnly = gameService.listGames(null, "COMMUNITY", null, 0, 50);

        assertThat(serverOnly.games()).hasSize(1);
        assertThat(serverOnly.games().get(0).hostingType()).isEqualTo("SERVER");
        assertThat(communityOnly.games()).hasSize(1);
        assertThat(communityOnly.games().get(0).hostingType()).isEqualTo("COMMUNITY");
    }

    @Test
    void testListGames_searchByName() {
        gameService.createGame(createTestConfig("Friday Night Poker"), 1L, "alice");
        gameService.createGame(createTestConfig("Weekend Warriors"), 2L, "bob");

        GameListResponse response = gameService.listGames(null, null, "friday", 0, 50);

        assertThat(response.games()).hasSize(1);
        assertThat(response.games().get(0).name()).isEqualTo("Friday Night Poker");
    }

    @Test
    void testListGames_includesHostingTypeAndWsUrlInSummary() {
        gameService.createGame(createTestConfig("Server Game"), 1L, "owner");

        GameListResponse response = gameService.listGames(null, null, null, 0, 50);

        assertThat(response.games()).hasSize(1);
        GameSummary summary = response.games().get(0);
        assertThat(summary.hostingType()).isEqualTo("SERVER");
        assertThat(summary.wsUrl()).isNotNull().contains("/ws/games/");
    }

    // =========================================================================
    // getGameSummary
    // =========================================================================

    @Test
    void testGetGameSummary() {
        String gameId = gameService.createGame(createTestConfig(), 1L, "owner");

        GameSummary summary = gameService.getGameSummary(gameId);

        assertThat(summary).isNotNull();
        assertThat(summary.gameId()).isEqualTo(gameId);
        assertThat(summary.status()).isEqualTo("WAITING_FOR_PLAYERS");
        assertThat(summary.hostingType()).isEqualTo("SERVER");
    }

    @Test
    void testGetGameSummaryNotFound() {
        GameSummary summary = gameService.getGameSummary("nonexistent");
        assertThat(summary).isNull();
    }

    // =========================================================================
    // joinGame
    // =========================================================================

    @Test
    void testJoinGame_publicGame_returnsWsUrl() {
        String gameId = gameService.createGame(createTestConfig(), 1L, "owner");

        GameJoinResponse response = gameService.joinGame(gameId, null);

        assertThat(response).isNotNull();
        assertThat(response.wsUrl()).isNotNull().contains("/ws/games/");
        assertThat(response.gameId()).isEqualTo(gameId);
    }

    @Test
    void testJoinGame_notFound_throws() {
        assertThatThrownBy(() -> gameService.joinGame("nonexistent", null)).isInstanceOf(GameServerException.class);
    }

    @Test
    void testJoinGame_wrongPassword_throws() {
        CommunityGameRegisterRequest req = new CommunityGameRegisterRequest("Private", "ws://1.2.3.4/x", null,
                "correctpassword");
        GameSummary summary = gameService.registerCommunityGame(1L, "host", req);

        assertThatThrownBy(() -> gameService.joinGame(summary.gameId(), "wrongpassword"))
                .isInstanceOf(GameServerException.class);
    }

    // =========================================================================
    // startGame (with ownership check)
    // =========================================================================

    @Test
    void testStartGame_ownerCanStart() {
        String gameId = gameService.createGame(createTestConfig(), 1L, "owner");

        GameSummary result = gameService.startGame(gameId, 1L);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void testStartGame_nonOwner_throws403() {
        String gameId = gameService.createGame(createTestConfig(), 1L, "owner");

        assertThatThrownBy(() -> gameService.startGame(gameId, 99L)).isInstanceOf(GameServerException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void testStartGame_alreadyStarted_throwsConflict() {
        String gameId = gameService.createGame(createTestConfig(), 1L, "owner");
        gameService.startGame(gameId, 1L);

        assertThatThrownBy(() -> gameService.startGame(gameId, 1L)).isInstanceOf(GameServerException.class);
    }

    // =========================================================================
    // cancelGame
    // =========================================================================

    @Test
    void testCancelGame_ownerCanCancel() {
        String gameId = gameService.createGame(createTestConfig(), 1L, "owner");

        gameService.cancelGame(gameId, 1L);

        GameInstanceEntity entity = gameInstanceRepository.findById(gameId).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(GameInstanceState.CANCELLED);
    }

    @Test
    void testCancelGame_nonOwner_throws() {
        String gameId = gameService.createGame(createTestConfig(), 1L, "owner");

        assertThatThrownBy(() -> gameService.cancelGame(gameId, 99L)).isInstanceOf(GameServerException.class);
    }

    // =========================================================================
    // heartbeat
    // =========================================================================

    @Test
    void testHeartbeat_communityOwnerCanSendHeartbeat() {
        CommunityGameRegisterRequest req = new CommunityGameRegisterRequest("Community Game",
                "ws://1.2.3.4:8765/ws/games/x", null, null);
        GameSummary summary = gameService.registerCommunityGame(1L, "host", req);

        // Should not throw
        gameService.heartbeat(summary.gameId(), 1L);
    }

    @Test
    void testHeartbeat_nonCommunityGame_throws() {
        String gameId = gameService.createGame(createTestConfig(), 1L, "owner");

        assertThatThrownBy(() -> gameService.heartbeat(gameId, 1L)).isInstanceOf(GameServerException.class);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private GameConfig createTestConfig(String name) {
        return new GameConfig(name, "Test description", "Welcome!", 9, 90, true, 0, 1000,
                List.of(new GameConfig.BlindLevel(10, 20, 0, 15, false, "NOLIMIT_HOLDEM")), true, "NOLIMIT_HOLDEM",
                GameConfig.LevelAdvanceMode.TIME, 10, 15, null, null, null, null, null, null, null, null, null, null,
                null, true, false, false, null);
    }

    private GameConfig createTestConfig() {
        return createTestConfig("Test Game");
    }
}
