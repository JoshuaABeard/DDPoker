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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.dto.GameStateResponse;
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
    }

    @Test
    void testJoinGame() {
        GameConfig config = createTestConfig();
        String gameId = gameService.createGame(config, 1L, "owner");

        boolean success = gameService.joinGame(gameId, 2L, "player2");

        assertThat(success).isTrue();
        GameInstanceEntity instance = gameInstanceRepository.findById(gameId).orElse(null);
        assertThat(instance).isNotNull();
        assertThat(instance.getPlayerCount()).isEqualTo(2);
    }

    @Test
    void testJoinGameNotFound() {
        boolean success = gameService.joinGame("nonexistent", 2L, "player");
        assertThat(success).isFalse();
    }

    @Test
    void testStartGame() {
        GameConfig config = createTestConfig();
        String gameId = gameService.createGame(config, 1L, "owner");
        gameService.joinGame(gameId, 2L, "player2");

        boolean success = gameService.startGame(gameId);

        assertThat(success).isTrue();
        GameInstanceEntity instance = gameInstanceRepository.findById(gameId).orElse(null);
        assertThat(instance).isNotNull();
        assertThat(instance.getStatus()).isEqualTo(GameInstanceState.IN_PROGRESS);
    }

    @Test
    void testStartGameNotFound() {
        boolean success = gameService.startGame("nonexistent");
        assertThat(success).isFalse();
    }

    @Test
    void testGetGameState() {
        GameConfig config = createTestConfig();
        String gameId = gameService.createGame(config, 1L, "owner");

        GameStateResponse state = gameService.getGameState(gameId);

        assertThat(state).isNotNull();
        assertThat(state.gameId()).isEqualTo(gameId);
        assertThat(state.status()).isEqualTo("WAITING_FOR_PLAYERS");
    }

    @Test
    void testGetGameStateNotFound() {
        GameStateResponse state = gameService.getGameState("nonexistent");
        assertThat(state).isNull();
    }

    @Test
    void testListGames() {
        GameConfig config1 = createTestConfig("Game 1");
        GameConfig config2 = createTestConfig("Game 2");

        gameService.createGame(config1, 1L, "owner1");
        gameService.createGame(config2, 2L, "owner2");

        var games = gameService.listGames();

        assertThat(games).hasSize(2);
    }

    private GameConfig createTestConfig(String name) {
        return new GameConfig(name, "Test description", "Welcome!", 9, 90, true, 0, 1000,
                java.util.List.of(new GameConfig.BlindLevel(10, 20, 0, 15, false, "NOLIMIT_HOLDEM")), true,
                "NOLIMIT_HOLDEM", GameConfig.LevelAdvanceMode.TIME, 10, 15, null, null, null, null, null, null, null,
                null, null, null, null, true, false, false, null);
    }

    private GameConfig createTestConfig() {
        return createTestConfig("Test Game");
    }
}
