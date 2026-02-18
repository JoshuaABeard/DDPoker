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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.GameServerProperties;
import com.donohoedigital.games.poker.gameserver.persistence.entity.GameInstanceEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameInstanceRepository;
import com.donohoedigital.games.poker.gameserver.websocket.LobbyBroadcaster;

/**
 * Scheduled job that cancels stale games and deletes expired records.
 *
 * <p>
 * Runs every 60 seconds. Handles three scenarios:
 * <ol>
 * <li>COMMUNITY games whose last heartbeat is older than
 * {@link GameServerProperties#communityHeartbeatTimeoutMinutes()} →
 * CANCELLED</li>
 * <li>SERVER lobbies stuck in WAITING_FOR_PLAYERS longer than
 * {@link GameServerProperties#lobbyTimeoutHours()} → CANCELLED</li>
 * <li>COMPLETED/CANCELLED games older than
 * {@link GameServerProperties#completedGameRetentionDays()} → deleted</li>
 * </ol>
 *
 * <p>
 * {@code @EnableScheduling} is on
 * {@link com.donohoedigital.games.poker.gameserver.GameServerAutoConfiguration}.
 */
@Component
public class StaleGameCleanupJob {

    private static final Logger logger = LogManager.getLogger(StaleGameCleanupJob.class);

    private final GameInstanceRepository repo;
    private final GameServerProperties properties;

    /** Null in non-web/test contexts where LobbyBroadcaster is not configured. */
    @Autowired(required = false)
    private LobbyBroadcaster lobbyBroadcaster;

    public StaleGameCleanupJob(GameInstanceRepository repo, GameServerProperties properties) {
        this.repo = repo;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void cleanup() {
        cancelStaleCommunityGames();
        cancelAbandonedServerLobbies();
        deleteExpiredGames();
    }

    private void cancelStaleCommunityGames() {
        Instant cutoff = Instant.now().minus(properties.communityHeartbeatTimeoutMinutes(), ChronoUnit.MINUTES);
        List<GameInstanceEntity> stale = repo.findStaleCommunityGames(cutoff);
        for (GameInstanceEntity game : stale) {
            repo.updateStatusWithCompletionTime(game.getGameId(), GameInstanceState.CANCELLED, Instant.now());
            if (lobbyBroadcaster != null) {
                lobbyBroadcaster.broadcastGameCancelled(game.getGameId(), "Community host disconnected");
            }
            logger.info("Cancelled stale community game: {}", game.getGameId());
        }
    }

    private void cancelAbandonedServerLobbies() {
        Instant cutoff = Instant.now().minus(properties.lobbyTimeoutHours(), ChronoUnit.HOURS);
        List<GameInstanceEntity> abandoned = repo.findAbandonedServerLobbies(cutoff);
        for (GameInstanceEntity game : abandoned) {
            repo.updateStatusWithCompletionTime(game.getGameId(), GameInstanceState.CANCELLED, Instant.now());
            if (lobbyBroadcaster != null) {
                lobbyBroadcaster.broadcastGameCancelled(game.getGameId(), "Lobby expired — game never started");
            }
            logger.info("Cancelled abandoned server lobby: {}", game.getGameId());
        }
    }

    private void deleteExpiredGames() {
        Instant cutoff = Instant.now().minus(properties.completedGameRetentionDays(), ChronoUnit.DAYS);
        List<GameInstanceEntity> expired = repo.findExpiredGames(cutoff);
        if (!expired.isEmpty()) {
            logger.info("Deleting {} expired game records", expired.size());
            repo.deleteAll(expired);
        }
    }
}
