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
import java.util.Locale;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.GameServerException;
import com.donohoedigital.games.poker.gameserver.GameServerProperties;
import com.donohoedigital.games.poker.gameserver.GameServerException.ErrorCode;
import com.donohoedigital.games.poker.gameserver.dto.CommunityGameRegisterRequest;
import com.donohoedigital.games.poker.gameserver.dto.GameJoinResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameListResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameSettingsRequest;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary.BlindsSummary;
import com.donohoedigital.games.poker.gameserver.persistence.entity.GameInstanceEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameEventRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameInstanceRepository;
import com.donohoedigital.games.poker.gameserver.websocket.LobbyBroadcaster;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData.LobbyPlayerData;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for game management and discovery operations.
 */
@Service
@Transactional
public class GameService {

    private static final Logger logger = LogManager.getLogger(GameService.class);

    private final GameInstanceRepository gameInstanceRepository;
    private final GameEventRepository gameEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Set by WebSocketAutoConfiguration post-construction; null when running in
     * test/non-web contexts.
     */
    @Autowired(required = false)
    private LobbyBroadcaster lobbyBroadcaster;

    /**
     * Set by GameServerAutoConfiguration post-construction; null when running in
     * test contexts.
     */
    @Autowired(required = false)
    private GameInstanceManager gameInstanceManager;

    /**
     * Null in non-web/test contexts; used in @PostConstruct to seed serverBaseUrl
     * from config.
     */
    @Autowired(required = false)
    private GameServerProperties gameServerProperties;

    /**
     * Server base URL used to build ws_url for SERVER-hosted games. Injected by
     * EmbeddedGameServer post-startup or by @PostConstruct in standalone mode.
     */
    private volatile String serverBaseUrl = "ws://localhost";

    public GameService(GameInstanceRepository gameInstanceRepository, GameEventRepository gameEventRepository) {
        this.gameInstanceRepository = gameInstanceRepository;
        this.gameEventRepository = gameEventRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        if (gameServerProperties != null) {
            this.serverBaseUrl = gameServerProperties.serverBaseUrl();
        }
    }

    /**
     * Sets the server base URL used to build ws_url for SERVER-hosted games. Called
     * by EmbeddedGameServer after the embedded Spring context starts (to get the
     * actual port), or by a @PostConstruct in standalone mode (from
     * GameServerProperties.serverBaseUrl).
     */
    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    // =========================================================================
    // Game creation
    // =========================================================================

    /**
     * Create a new SERVER-hosted game.
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
        String wsUrl = serverBaseUrl + "/ws/games/" + gameId;

        GameInstanceEntity entity = new GameInstanceEntity();
        entity.setGameId(gameId);
        entity.setName(config.name());
        entity.setOwnerProfileId(ownerProfileId);
        entity.setOwnerName(ownerName);
        entity.setMaxPlayers(config.maxPlayers());
        entity.setPlayerCount(0);
        entity.setStatus(GameInstanceState.WAITING_FOR_PLAYERS);
        entity.setHostingType("SERVER");
        entity.setWsUrl(wsUrl);
        entity.setProfileData(serializeConfig(config));
        entity.setCreatedAt(Instant.now());

        gameInstanceRepository.save(entity);
        return gameId;
    }

    /**
     * Register a community-hosted game.
     */
    public GameSummary registerCommunityGame(Long ownerProfileId, String ownerName,
            CommunityGameRegisterRequest request) {
        String gameId = UUID.randomUUID().toString();

        GameInstanceEntity entity = new GameInstanceEntity();
        entity.setGameId(gameId);
        entity.setName(request.name());
        entity.setOwnerProfileId(ownerProfileId);
        entity.setOwnerName(ownerName);
        entity.setMaxPlayers(request.profile() != null ? request.profile().maxPlayers() : 9);
        entity.setPlayerCount(0);
        entity.setStatus(GameInstanceState.WAITING_FOR_PLAYERS);
        entity.setHostingType("COMMUNITY");
        entity.setWsUrl(request.wsUrl());
        entity.setProfileData(serializeConfig(request.profile()));
        entity.setCreatedAt(Instant.now());
        entity.setLastHeartbeat(Instant.now());

        if (request.password() != null && !request.password().isEmpty()) {
            entity.setPasswordHash(BCrypt.hashpw(request.password(), BCrypt.gensalt()));
        }

        gameInstanceRepository.save(entity);
        return toSummary(entity);
    }

    // =========================================================================
    // Game listing
    // =========================================================================

    /**
     * Paginated game listing with optional filters.
     *
     * @param statuses
     *            status names to include (default: WAITING_FOR_PLAYERS,
     *            IN_PROGRESS)
     * @param hostingType
     *            "SERVER", "COMMUNITY", or null for all
     * @param search
     *            case-insensitive substring match against name and ownerName
     * @param page
     *            zero-based page index
     * @param pageSize
     *            items per page (max 100)
     */
    @Transactional(readOnly = true)
    public GameListResponse listGames(List<GameInstanceState> statuses, String hostingType, String search, int page,
            int pageSize) {
        if (statuses == null || statuses.isEmpty()) {
            statuses = List.of(GameInstanceState.WAITING_FOR_PLAYERS, GameInstanceState.IN_PROGRESS);
        }
        pageSize = Math.min(pageSize, 100);

        String searchPattern = search != null ? "%" + search.toLowerCase(Locale.ROOT) + "%" : null;
        List<GameInstanceEntity> all = gameInstanceRepository.findGamesFiltered(statuses, hostingType, searchPattern);

        List<GameSummary> filtered = all.stream().map(this::toSummary).toList();

        int total = filtered.size();
        int fromIndex = page * pageSize;
        if (fromIndex >= total) {
            return new GameListResponse(List.of(), total, page, pageSize);
        }
        int toIndex = Math.min(fromIndex + pageSize, total);
        return new GameListResponse(filtered.subList(fromIndex, toIndex), total, page, pageSize);
    }

    /**
     * Get a single game summary by ID.
     */
    @Transactional(readOnly = true)
    public GameSummary getGameSummary(String gameId) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId).orElse(null);
        if (entity == null) {
            return null;
        }
        return toSummary(entity);
    }

    // =========================================================================
    // Join gatekeeper
    // =========================================================================

    /**
     * Join gate: validates password (if private) and returns the WebSocket URL.
     * Does NOT increment playerCount.
     */
    public GameJoinResponse joinGame(String gameId, String password) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId)
                .orElseThrow(() -> new GameServerException(ErrorCode.GAME_NOT_FOUND, "Game not found: " + gameId));

        if (entity.getStatus() != GameInstanceState.WAITING_FOR_PLAYERS) {
            throw new GameServerException(ErrorCode.GAME_NOT_JOINABLE,
                    "Game is not accepting players (status: " + entity.getStatus() + ")");
        }

        if (entity.getPasswordHash() != null) {
            if (password == null || !BCrypt.checkpw(password, entity.getPasswordHash())) {
                throw new GameServerException(ErrorCode.WRONG_PASSWORD, "Incorrect password");
            }
        }

        return new GameJoinResponse(entity.getWsUrl(), gameId);
    }

    // =========================================================================
    // Heartbeat
    // =========================================================================

    /**
     * Record a heartbeat from the community host.
     */
    public void heartbeat(String gameId, Long requesterProfileId) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId)
                .orElseThrow(() -> new GameServerException(ErrorCode.GAME_NOT_FOUND, "Game not found: " + gameId));

        if (!"COMMUNITY".equals(entity.getHostingType())) {
            throw new GameServerException(ErrorCode.WRONG_HOSTING_TYPE, "Heartbeat is only for COMMUNITY games");
        }
        if (!entity.getOwnerProfileId().equals(requesterProfileId)) {
            throw new GameServerException(ErrorCode.NOT_GAME_OWNER, "Only the game owner can send heartbeats");
        }

        gameInstanceRepository.updateHeartbeat(gameId, Instant.now());
    }

    // =========================================================================
    // Game lifecycle
    // =========================================================================

    /**
     * Start the game (WAITING_FOR_PLAYERS → IN_PROGRESS).
     */
    public GameSummary startGame(String gameId, Long requesterProfileId) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId)
                .orElseThrow(() -> new GameServerException(ErrorCode.GAME_NOT_FOUND, "Game not found: " + gameId));

        if (!entity.getOwnerProfileId().equals(requesterProfileId)) {
            throw new GameServerException(ErrorCode.NOT_GAME_OWNER, "Only the game owner can start the game");
        }
        if (entity.getStatus() != GameInstanceState.WAITING_FOR_PLAYERS) {
            throw new GameServerException(ErrorCode.GAME_ALREADY_STARTED,
                    "Game has already started (status: " + entity.getStatus() + ")");
        }

        if ("SERVER".equals(entity.getHostingType()) && gameInstanceManager != null) {
            GameInstance game = gameInstanceManager.getGame(gameId);
            if (game != null) {
                // In-memory game director exists (created via GameInstanceManager) —
                // start it BEFORE updating DB so a failure leaves DB at WAITING_FOR_PLAYERS.
                gameInstanceManager.startGame(gameId, requesterProfileId);
            }
            // If game is null, the game was created via REST API only (no in-memory
            // director yet). DB update proceeds normally.
        }

        Instant now = Instant.now();
        gameInstanceRepository.updateStatusWithStartTime(gameId, GameInstanceState.IN_PROGRESS, now);

        if ("SERVER".equals(entity.getHostingType()) && lobbyBroadcaster != null) {
            // Broadcast countdown to lobby clients after successful start
            lobbyBroadcaster.broadcastLobbyGameStarting(gameId, 3);
        }
        // COMMUNITY: DB status update only (no WS broadcast — community clients connect
        // to host's WS)

        entity.setStatus(GameInstanceState.IN_PROGRESS);
        entity.setStartedAt(now);
        return toSummary(entity);
    }

    /**
     * Update pre-game settings. SERVER games only.
     */
    public GameSummary updateSettings(String gameId, Long requesterProfileId, GameSettingsRequest request) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId)
                .orElseThrow(() -> new GameServerException(ErrorCode.GAME_NOT_FOUND, "Game not found: " + gameId));

        if ("COMMUNITY".equals(entity.getHostingType())) {
            throw new GameServerException(ErrorCode.NOT_APPLICABLE,
                    "Settings update is not applicable for COMMUNITY games");
        }
        if (!entity.getOwnerProfileId().equals(requesterProfileId)) {
            throw new GameServerException(ErrorCode.NOT_GAME_OWNER, "Only the game owner can update settings");
        }
        if (entity.getStatus() != GameInstanceState.WAITING_FOR_PLAYERS) {
            throw new GameServerException(ErrorCode.GAME_NOT_IN_LOBBY, "Settings can only be changed in lobby");
        }

        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.maxPlayers() != null) {
            entity.setMaxPlayers(request.maxPlayers());
        }
        if (request.profile() != null) {
            entity.setProfileData(serializeConfig(request.profile()));
        }
        if (request.password() != null) {
            if (request.password().isEmpty()) {
                entity.setPasswordHash(null);
            } else {
                entity.setPasswordHash(BCrypt.hashpw(request.password(), BCrypt.gensalt()));
            }
        }

        gameInstanceRepository.save(entity);
        GameSummary updated = toSummary(entity);

        if (lobbyBroadcaster != null) {
            lobbyBroadcaster.broadcastLobbySettingsChanged(gameId, updated);
        }

        return updated;
    }

    /**
     * Kick a player from the lobby (pre-game). SERVER games only.
     */
    public void kickFromLobby(String gameId, Long requesterProfileId, Long targetProfileId) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId)
                .orElseThrow(() -> new GameServerException(ErrorCode.GAME_NOT_FOUND, "Game not found: " + gameId));

        if ("COMMUNITY".equals(entity.getHostingType())) {
            throw new GameServerException(ErrorCode.NOT_APPLICABLE, "Kick is not applicable for COMMUNITY games");
        }
        if (!entity.getOwnerProfileId().equals(requesterProfileId)) {
            throw new GameServerException(ErrorCode.NOT_GAME_OWNER, "Only the game owner can kick players");
        }
        if (entity.getStatus() != GameInstanceState.WAITING_FOR_PLAYERS) {
            throw new GameServerException(ErrorCode.GAME_NOT_IN_LOBBY, "Kick is only available in lobby");
        }

        // Remove player from in-memory game instance
        if (gameInstanceManager != null) {
            GameInstance game = gameInstanceManager.getGame(gameId);
            if (game != null) {
                if (!game.hasPlayer(targetProfileId)) {
                    throw new GameServerException(ErrorCode.PLAYER_NOT_FOUND,
                            "Player not in lobby: " + targetProfileId);
                }
                game.removePlayer(targetProfileId);
            }
        }

        // Update DB player count (atomic decrement, clamped to 0)
        gameInstanceRepository.decrementPlayerCount(gameId);

        if (lobbyBroadcaster != null) {
            lobbyBroadcaster.broadcastLobbyPlayerKicked(gameId,
                    new LobbyPlayerData(targetProfileId, "", false, false, null));
        }
    }

    /**
     * Cancel a game. Valid at any point before COMPLETED.
     */
    public void cancelGame(String gameId, Long requesterProfileId) {
        GameInstanceEntity entity = gameInstanceRepository.findById(gameId)
                .orElseThrow(() -> new GameServerException(ErrorCode.GAME_NOT_FOUND, "Game not found: " + gameId));

        if (!entity.getOwnerProfileId().equals(requesterProfileId)) {
            throw new GameServerException(ErrorCode.NOT_GAME_OWNER, "Only the game owner can cancel the game");
        }
        if (entity.getStatus() == GameInstanceState.COMPLETED || entity.getStatus() == GameInstanceState.CANCELLED) {
            throw new GameServerException(ErrorCode.GAME_COMPLETED,
                    "Game is already in terminal state: " + entity.getStatus());
        }

        gameInstanceRepository.updateStatusWithCompletionTime(gameId, GameInstanceState.CANCELLED, Instant.now());

        if ("SERVER".equals(entity.getHostingType()) && lobbyBroadcaster != null) {
            lobbyBroadcaster.broadcastGameCancelled(gameId, "Game cancelled by owner");
        }
    }

    // =========================================================================
    // Legacy / internal helpers
    // =========================================================================

    /**
     * Internal join used by WebSocketHandler when a player connects to a
     * WAITING_FOR_PLAYERS game. Increments DB player count atomically. Not exposed
     * via REST.
     */
    public void incrementPlayerCount(String gameId) {
        gameInstanceRepository.incrementPlayerCount(gameId);
    }

    /**
     * Internal decrement used by WebSocketHandler when a player disconnects from a
     * WAITING_FOR_PLAYERS game. Atomic, clamped to 0.
     */
    public void decrementPlayerCount(String gameId) {
        gameInstanceRepository.decrementPlayerCount(gameId);
    }

    // =========================================================================
    // DTO mapping
    // =========================================================================

    GameSummary toSummary(GameInstanceEntity e) {
        boolean isPrivate = e.getPasswordHash() != null;
        String wsUrl = isPrivate ? null : e.getWsUrl();
        BlindsSummary blinds = parseBlinds(e.getProfileData());
        return new GameSummary(e.getGameId(), e.getName(), e.getHostingType(), e.getStatus().name(), e.getOwnerName(),
                e.getPlayerCount(), e.getMaxPlayers(), isPrivate, wsUrl, blinds, e.getCreatedAt(), e.getStartedAt());
    }

    private BlindsSummary parseBlinds(String profileData) {
        if (profileData == null || profileData.isBlank() || "{}".equals(profileData.trim())) {
            return new BlindsSummary(0, 0, 0);
        }
        try {
            GameConfig config = objectMapper.readValue(profileData, GameConfig.class);
            if (config.blindStructure() != null && !config.blindStructure().isEmpty()) {
                GameConfig.BlindLevel level = config.blindStructure().get(0);
                return new BlindsSummary(level.smallBlind(), level.bigBlind(), level.ante());
            }
        } catch (Exception ignored) {
            // Malformed profileData — return zeros
        }
        return new BlindsSummary(0, 0, 0);
    }

    private String serializeConfig(GameConfig config) {
        if (config == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return "{}";
        }
    }
}
