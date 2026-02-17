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
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.dto.GameStateResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.persistence.entity.GameInstanceEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameEventRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameInstanceRepository;

/**
 * Service for game management operations.
 */
@Service
@Transactional
public class GameService {

    private final GameInstanceRepository gameInstanceRepository;
    private final GameEventRepository gameEventRepository;

    public GameService(GameInstanceRepository gameInstanceRepository, GameEventRepository gameEventRepository) {
        this.gameInstanceRepository = gameInstanceRepository;
        this.gameEventRepository = gameEventRepository;
    }

    /**
     * Create a new game.
     *
     * @param config
     *            game configuration
     * @param ownerProfileId
     *            owner's profile ID
     * @param ownerName
     *            owner's username
     * @return game ID
     */
    public String createGame(GameConfig config, Long ownerProfileId, String ownerName) {
        String gameId = UUID.randomUUID().toString();

        // Create database entity
        GameInstanceEntity entity = new GameInstanceEntity();
        entity.setGameId(gameId);
        entity.setName(config.name());
        entity.setOwnerProfileId(ownerProfileId);
        entity.setOwnerName(ownerName);
        entity.setMaxPlayers(config.maxPlayers());
        entity.setPlayerCount(1);
        entity.setStatus(GameInstanceState.WAITING_FOR_PLAYERS);
        entity.setHostingType("SERVER");
        entity.setProfileData("{}");
        entity.setCreatedAt(Instant.now());

        gameInstanceRepository.save(entity);

        return gameId;
    }

    /**
     * Join an existing game.
     *
     * @param gameId
     *            game ID
     * @param profileId
     *            player's profile ID
     * @param playerName
     *            player's username
     * @return true if joined successfully
     */
    public boolean joinGame(String gameId, Long profileId, String playerName) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId).orElse(null);
        if (entity == null) {
            return false;
        }

        entity.setPlayerCount(entity.getPlayerCount() + 1);
        gameInstanceRepository.save(entity);

        return true;
    }

    /**
     * Start a game.
     *
     * @param gameId
     *            game ID
     * @return true if started successfully
     */
    public boolean startGame(String gameId) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId).orElse(null);
        if (entity == null) {
            return false;
        }

        entity.setStatus(GameInstanceState.IN_PROGRESS);
        entity.setStartedAt(Instant.now());
        gameInstanceRepository.save(entity);

        return true;
    }

    /**
     * Get game state.
     *
     * @param gameId
     *            game ID
     * @return game state or null if not found
     */
    public GameStateResponse getGameState(String gameId) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId).orElse(null);
        if (entity == null) {
            return null;
        }

        return new GameStateResponse(gameId, entity.getName(), entity.getStatus().name(), entity.getPlayerCount(),
                entity.getMaxPlayers());
    }

    /**
     * List all games.
     *
     * @return list of game summaries
     */
    public List<GameSummary> listGames() {
        return gameInstanceRepository.findAll().stream().map(e -> new GameSummary(e.getGameId(), e.getName(),
                e.getOwnerName(), e.getPlayerCount(), e.getMaxPlayers(), e.getStatus().name())).toList();
    }
}
