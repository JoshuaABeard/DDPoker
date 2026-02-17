/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.controller;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.AIPlayerConfig;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;
import com.donohoedigital.games.poker.gameserver.dto.CreateGameResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint for creating single-player practice games.
 *
 * <p>
 * {@code POST /api/v1/games/practice} accepts a fully-formed {@link GameConfig}
 * (including {@code aiPlayers}), creates the game, adds all AI players,
 * auto-joins the authenticated caller as the human player, and starts the game
 * immediately. Returns a {@code { "gameId": "..." }} response that the desktop
 * client passes to
 * {@link com.donohoedigital.games.poker.online.WebSocketTournamentDirector}.
 */
@RestController
@RequestMapping("/api/v1/games/practice")
public class PracticeGameController {

    private static final Logger logger = LogManager.getLogger(PracticeGameController.class);

    private static final int DEFAULT_AI_SKILL = 4;

    private final GameInstanceManager gameInstanceManager;

    public PracticeGameController(GameInstanceManager gameInstanceManager) {
        this.gameInstanceManager = gameInstanceManager;
    }

    /**
     * Create a practice game.
     *
     * <p>
     * The caller is auto-joined as the human player and the game is started
     * immediately. AI players are added from {@link GameConfig#aiPlayers()}.
     *
     * @param config
     *            game configuration including AI players
     * @return 201 Created with {@code { "gameId": "..." }}
     */
    @PostMapping
    public ResponseEntity<CreateGameResponse> createPracticeGame(@RequestBody GameConfig config) {
        AuthenticatedUser user = getAuthenticatedUser();

        // Create game instance in CREATED state
        GameInstance instance = gameInstanceManager.createGame(user.profileId(), config);

        // Transition to accept players
        instance.transitionToWaitingForPlayers();

        // Auto-join the human caller
        instance.addPlayer(user.profileId(), user.username(), false, 0);

        // Add AI players from config
        List<AIPlayerConfig> aiPlayers = config.aiPlayers();
        if (aiPlayers != null) {
            long aiId = -1L;
            for (AIPlayerConfig ai : aiPlayers) {
                int skillLevel = (ai.skillLevel() >= 1 && ai.skillLevel() <= 7) ? ai.skillLevel() : DEFAULT_AI_SKILL;
                instance.addPlayer(aiId--, ai.name(), true, skillLevel);
            }
        }

        // Start immediately — owner is the human caller
        gameInstanceManager.startGame(instance.getGameId(), user.profileId());

        logger.info("Practice game {} started for user {} with {} AI players", instance.getGameId(), user.username(),
                aiPlayers != null ? aiPlayers.size() : 0);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateGameResponse(instance.getGameId()));
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationFilter.JwtAuthenticationToken jwtAuth) {
            return new AuthenticatedUser(jwtAuth.getProfileId(), (String) jwtAuth.getPrincipal());
        }
        throw new IllegalStateException("No authenticated user found");
    }

    private record AuthenticatedUser(Long profileId, String username) {
    }
}
