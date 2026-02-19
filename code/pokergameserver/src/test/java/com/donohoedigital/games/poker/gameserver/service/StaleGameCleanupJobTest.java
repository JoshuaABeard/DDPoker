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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.GameServerProperties;
import com.donohoedigital.games.poker.gameserver.persistence.TestJpaConfiguration;
import com.donohoedigital.games.poker.gameserver.persistence.entity.GameInstanceEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameInstanceRepository;

@DataJpaTest
@ContextConfiguration(classes = {TestJpaConfiguration.class, StaleGameCleanupJobTest.TestConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StaleGameCleanupJobTest {

    /**
     * Short thresholds for testing: 1 minute heartbeat, 1 hour lobby, 1 day
     * retention.
     */
    private static final GameServerProperties PROPS = new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5,
            /* communityHeartbeatTimeoutMinutes */ 1, /* lobbyTimeoutHours */ 1, /* completedGameRetentionDays */ 1,
            "ws://localhost", 0);

    @Autowired
    private StaleGameCleanupJob cleanupJob;

    @Autowired
    private GameInstanceRepository repo;

    @org.springframework.context.annotation.Configuration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public StaleGameCleanupJob staleGameCleanupJob(GameInstanceRepository repo) {
            return new StaleGameCleanupJob(repo, PROPS);
        }
    }

    // =========================================================================
    // Community game stale-heartbeat cancellation
    // =========================================================================

    @Test
    void staleCommunityGame_cancelledWhenHeartbeatExpired() {
        String gameId = saveGame(entity -> {
            entity.setHostingType("COMMUNITY");
            entity.setStatus(GameInstanceState.WAITING_FOR_PLAYERS);
            // Heartbeat 10 minutes ago — older than 1-minute threshold
            entity.setLastHeartbeat(Instant.now().minus(10, ChronoUnit.MINUTES));
        });

        cleanupJob.cleanup();

        assertThat(repo.findById(gameId).orElseThrow().getStatus()).isEqualTo(GameInstanceState.CANCELLED);
    }

    @Test
    void staleCommunityGame_notCancelledWhenHeartbeatFresh() {
        String gameId = saveGame(entity -> {
            entity.setHostingType("COMMUNITY");
            entity.setStatus(GameInstanceState.WAITING_FOR_PLAYERS);
            // Heartbeat 10 seconds ago — within threshold
            entity.setLastHeartbeat(Instant.now().minusSeconds(10));
        });

        cleanupJob.cleanup();

        assertThat(repo.findById(gameId).orElseThrow().getStatus()).isEqualTo(GameInstanceState.WAITING_FOR_PLAYERS);
    }

    @Test
    void staleCommunityGame_nullHeartbeat_cancelled() {
        String gameId = saveGame(entity -> {
            entity.setHostingType("COMMUNITY");
            entity.setStatus(GameInstanceState.WAITING_FOR_PLAYERS);
            // No heartbeat ever sent
            entity.setLastHeartbeat(null);
        });

        cleanupJob.cleanup();

        assertThat(repo.findById(gameId).orElseThrow().getStatus()).isEqualTo(GameInstanceState.CANCELLED);
    }

    // =========================================================================
    // Abandoned server lobby cancellation
    // =========================================================================

    @Test
    void abandonedServerLobby_cancelledWhenCreatedTooLongAgo() {
        String gameId = saveGame(entity -> {
            entity.setHostingType("SERVER");
            entity.setStatus(GameInstanceState.WAITING_FOR_PLAYERS);
            // Created 2 hours ago — older than 1-hour threshold
            entity.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        });

        cleanupJob.cleanup();

        assertThat(repo.findById(gameId).orElseThrow().getStatus()).isEqualTo(GameInstanceState.CANCELLED);
    }

    @Test
    void abandonedServerLobby_notCancelledWhenRecent() {
        String gameId = saveGame(entity -> {
            entity.setHostingType("SERVER");
            entity.setStatus(GameInstanceState.WAITING_FOR_PLAYERS);
            // Created 5 minutes ago — within threshold
            entity.setCreatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        });

        cleanupJob.cleanup();

        assertThat(repo.findById(gameId).orElseThrow().getStatus()).isEqualTo(GameInstanceState.WAITING_FOR_PLAYERS);
    }

    @Test
    void inProgressServerGame_notCancelledByLobbyTimeout() {
        String gameId = saveGame(entity -> {
            entity.setHostingType("SERVER");
            entity.setStatus(GameInstanceState.IN_PROGRESS);
            // Created 2 hours ago but in-progress — lobby timeout doesn't apply
            entity.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        });

        cleanupJob.cleanup();

        assertThat(repo.findById(gameId).orElseThrow().getStatus()).isEqualTo(GameInstanceState.IN_PROGRESS);
    }

    // =========================================================================
    // Expired game deletion
    // =========================================================================

    @Test
    void expiredCompletedGame_deleted() {
        String gameId = saveGame(entity -> {
            entity.setStatus(GameInstanceState.COMPLETED);
            // Completed 2 days ago — older than 1-day retention
            entity.setCompletedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        });

        cleanupJob.cleanup();

        assertThat(repo.findById(gameId)).isEmpty();
    }

    @Test
    void expiredCancelledGame_deleted() {
        String gameId = saveGame(entity -> {
            entity.setStatus(GameInstanceState.CANCELLED);
            entity.setCompletedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        });

        cleanupJob.cleanup();

        assertThat(repo.findById(gameId)).isEmpty();
    }

    @Test
    void recentlyCompletedGame_notDeleted() {
        String gameId = saveGame(entity -> {
            entity.setStatus(GameInstanceState.COMPLETED);
            // Completed 1 hour ago — within retention period
            entity.setCompletedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        });

        cleanupJob.cleanup();

        assertThat(repo.findById(gameId)).isPresent();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    @FunctionalInterface
    private interface EntityConfigurer {
        void configure(GameInstanceEntity entity);
    }

    private String saveGame(EntityConfigurer configurer) {
        String gameId = UUID.randomUUID().toString();
        GameInstanceEntity entity = new GameInstanceEntity();
        entity.setGameId(gameId);
        entity.setName("Test Game");
        entity.setOwnerProfileId(1L);
        entity.setOwnerName("owner");
        entity.setMaxPlayers(9);
        entity.setPlayerCount(0);
        entity.setHostingType("SERVER");
        entity.setStatus(GameInstanceState.WAITING_FOR_PLAYERS);
        entity.setWsUrl("ws://localhost/ws/games/" + gameId);
        entity.setProfileData("{}");
        entity.setCreatedAt(Instant.now());
        configurer.configure(entity);
        repo.save(entity);
        return gameId;
    }
}
