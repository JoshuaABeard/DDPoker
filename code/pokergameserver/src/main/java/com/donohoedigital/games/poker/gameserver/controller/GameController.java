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
package com.donohoedigital.games.poker.gameserver.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter.JwtAuthenticationToken;
import com.donohoedigital.games.poker.gameserver.dto.CommunityGameRegisterRequest;
import com.donohoedigital.games.poker.gameserver.dto.CreateGameResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameJoinRequest;
import com.donohoedigital.games.poker.gameserver.dto.GameJoinResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameListResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameSettingsRequest;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.dto.KickRequest;
import com.donohoedigital.games.poker.gameserver.service.GameService;

/**
 * REST controller for game discovery and management.
 *
 * <p>
 * GET /api/v1/games and GET /api/v1/games/{id} are public (no auth). All other
 * endpoints require a valid JWT.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // =========================================================================
    // Public endpoints — permitted without auth in
    // GameServerSecurityAutoConfiguration
    // =========================================================================

    /**
     * List games with optional filters.
     *
     * @param statuses
     *            comma-separated (multi-value) {@link GameInstanceState} names;
     *            default: WAITING_FOR_PLAYERS,IN_PROGRESS
     * @param hostingType
     *            "SERVER" or "COMMUNITY"; null for all
     * @param search
     *            case-insensitive substring against name and ownerName
     * @param page
     *            zero-based page index (default 0)
     * @param pageSize
     *            items per page, capped at 100 (default 50)
     */
    @GetMapping
    public ResponseEntity<GameListResponse> listGames(
            @RequestParam(name = "status", required = false) List<String> statuses,
            @RequestParam(name = "hostingType", required = false) String hostingType,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "50") int pageSize) {
        List<GameInstanceState> statusEnums = parseStatuses(statuses);
        return ResponseEntity.ok(gameService.listGames(statusEnums, hostingType, search, page, pageSize));
    }

    /** Get a single game summary by ID. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<GameSummary> getGame(@PathVariable("id") String id) {
        GameSummary summary = gameService.getGameSummary(id);
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(summary);
    }

    // =========================================================================
    // Authenticated endpoints
    // =========================================================================

    /** Create a server-hosted game. Returns the new gameId (201 Created). */
    @PostMapping
    public ResponseEntity<CreateGameResponse> createGame(@Valid @RequestBody GameConfig config) {
        AuthenticatedUser user = getAuthenticatedUser();
        String gameId = gameService.createGame(config, user.profileId(), user.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateGameResponse(gameId));
    }

    /**
     * Register a community-hosted game. Returns the new GameSummary (201 Created).
     */
    @PostMapping("/community")
    public ResponseEntity<GameSummary> registerCommunityGame(@Valid @RequestBody CommunityGameRegisterRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        GameSummary summary = gameService.registerCommunityGame(user.profileId(), user.username(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    /**
     * Join-gate: validates password (for private games) and returns the WebSocket
     * URL. Does NOT increment playerCount — that happens when the WebSocket
     * connects.
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<GameJoinResponse> joinGame(@PathVariable("id") String id,
            @RequestBody(required = false) GameJoinRequest request) {
        String password = request != null ? request.password() : null;
        return ResponseEntity.ok(gameService.joinGame(id, password));
    }

    /** Community host keepalive. COMMUNITY games only; owner only. */
    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable("id") String id) {
        AuthenticatedUser user = getAuthenticatedUser();
        gameService.heartbeat(id, user.profileId());
        return ResponseEntity.ok().build();
    }

    /** Start the game (WAITING_FOR_PLAYERS → IN_PROGRESS). Owner only. */
    @PostMapping("/{id}/start")
    public ResponseEntity<GameSummary> startGame(@PathVariable("id") String id) {
        AuthenticatedUser user = getAuthenticatedUser();
        return ResponseEntity.ok(gameService.startGame(id, user.profileId()));
    }

    /** Update pre-game settings. SERVER games only; owner only. */
    @PutMapping("/{id}/settings")
    public ResponseEntity<GameSummary> updateSettings(@PathVariable("id") String id,
            @RequestBody GameSettingsRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        return ResponseEntity.ok(gameService.updateSettings(id, user.profileId(), request));
    }

    /** Kick a player from the lobby. SERVER games only; owner only. */
    @PostMapping("/{id}/kick")
    public ResponseEntity<Void> kickPlayer(@PathVariable("id") String id, @Valid @RequestBody KickRequest request) {
        AuthenticatedUser user = getAuthenticatedUser();
        gameService.kickFromLobby(id, user.profileId(), request.profileId());
        return ResponseEntity.ok().build();
    }

    /** Cancel a game. Both game types; owner only. Returns 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelGame(@PathVariable("id") String id) {
        AuthenticatedUser user = getAuthenticatedUser();
        gameService.cancelGame(id, user.profileId());
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return new AuthenticatedUser(jwtAuth.getProfileId(), (String) jwtAuth.getPrincipal());
        }
        throw new IllegalStateException("No authenticated user found");
    }

    private List<GameInstanceState> parseStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return statuses.stream().map(s -> {
            try {
                return GameInstanceState.valueOf(s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(s -> s != null).toList();
    }

    private record AuthenticatedUser(Long profileId, String username) {
    }
}
