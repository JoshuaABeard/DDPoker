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

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.GameServerException;
import com.donohoedigital.games.poker.gameserver.GameServerException.ErrorCode;
import com.donohoedigital.games.poker.gameserver.GameStateSnapshot;
import com.donohoedigital.games.poker.gameserver.ServerGameTable;
import com.donohoedigital.games.poker.gameserver.ServerHand;
import com.donohoedigital.games.poker.gameserver.ServerPlayer;
import com.donohoedigital.games.poker.gameserver.ServerTournamentContext;
import com.donohoedigital.games.poker.gameserver.auth.JwtAuthenticationFilter;
import com.donohoedigital.games.poker.gameserver.websocket.GameConnectionManager;
import com.donohoedigital.games.poker.gameserver.websocket.OutboundMessageConverter;
import com.donohoedigital.games.poker.gameserver.websocket.PlayerConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cheat endpoints for WebSocket practice mode. All 7 actions are gated:
 * practice-only and owner-only. After each mutation a GAME_STATE broadcast is
 * sent so clients refresh without waiting for the next hand event.
 *
 * <p>
 * All endpoints: {@code POST /api/v1/games/{id}/cheat/<action>}
 */
@RestController
@RequestMapping("/api/v1/games/{id}/cheat")
public class CheatController {

    private static final Logger logger = LogManager.getLogger(CheatController.class);

    private final GameInstanceManager gameInstanceManager;
    private final GameConnectionManager connectionManager;
    private final OutboundMessageConverter converter;

    public CheatController(GameInstanceManager gameInstanceManager, GameConnectionManager connectionManager,
            OutboundMessageConverter converter) {
        this.gameInstanceManager = gameInstanceManager;
        this.connectionManager = connectionManager;
        this.converter = converter;
    }

    // -------------------------------------------------------------------------
    // Request body records
    // -------------------------------------------------------------------------

    record ChipsRequest(int playerId, int chipCount) {
    }

    record NameRequest(int playerId, String name) {
    }

    record LevelRequest(int level) {
    }

    record ButtonRequest(int seat) {
    }

    record RemovePlayerRequest(int playerId) {
    }

    record CardRequest(String location, String newCard) {
    }

    record AiStrategyRequest(int playerId, int skillLevel) {
    }

    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    /** Change a player's chip count. */
    @PostMapping("/chips")
    public ResponseEntity<Void> changeChips(@PathVariable("id") String id, @RequestBody ChipsRequest req) {
        if (req.chipCount() < 0) {
            throw new IllegalArgumentException("Chip count must be non-negative");
        }
        GameInstance game = requirePracticeOwner(id);
        ServerPlayer player = requirePlayer(game, req.playerId());
        player.setChipCount(req.chipCount());
        broadcastGameState(game, id);
        logger.info("[CHEAT] chips: game={} player={} chips={}", id, req.playerId(), req.chipCount());
        return ResponseEntity.ok().build();
    }

    /** Change a player's display name. */
    @PostMapping("/name")
    public ResponseEntity<Void> changeName(@PathVariable("id") String id, @RequestBody NameRequest req) {
        GameInstance game = requirePracticeOwner(id);
        ServerPlayer player = requirePlayer(game, req.playerId());
        player.setName(req.name());
        broadcastGameState(game, id);
        logger.info("[CHEAT] name: game={} player={} name={}", id, req.playerId(), req.name());
        return ResponseEntity.ok().build();
    }

    /** Jump to a blind level (0-based index). */
    @PostMapping("/level")
    public ResponseEntity<Void> changeLevel(@PathVariable("id") String id, @RequestBody LevelRequest req) {
        GameInstance game = requirePracticeOwner(id);
        ServerTournamentContext tournament = game.getTournament();
        if (tournament == null) {
            throw new GameServerException(ErrorCode.INVALID_GAME_STATE, "Tournament has not started");
        }
        tournament.setLevel(req.level());
        game.getEventBus().publish(new GameEvent.LevelChanged(0, req.level()));
        broadcastGameState(game, id);
        logger.info("[CHEAT] level: game={} level={}", id, req.level());
        return ResponseEntity.ok().build();
    }

    /**
     * Move the dealer button to the given seat. Only allowed between hands.
     */
    @PostMapping("/button")
    public ResponseEntity<Void> moveButton(@PathVariable("id") String id, @RequestBody ButtonRequest req) {
        GameInstance game = requirePracticeOwner(id);
        ServerGameTable table = requireTable(game);
        if (table.getHoldemHand() != null) {
            throw new GameServerException(ErrorCode.INVALID_GAME_STATE,
                    "Cannot move button while a hand is in progress");
        }
        table.setButton(req.seat());
        broadcastGameState(game, id);
        logger.info("[CHEAT] button: game={} seat={}", id, req.seat());
        return ResponseEntity.ok().build();
    }

    /**
     * Eliminate a player by zeroing their chips and marking them as sitting out.
     */
    @PostMapping("/remove-player")
    public ResponseEntity<Void> removePlayer(@PathVariable("id") String id, @RequestBody RemovePlayerRequest req) {
        GameInstance game = requirePracticeOwner(id);
        ServerTournamentContext tournament = game.getTournament();
        if (tournament == null) {
            throw new GameServerException(ErrorCode.INVALID_GAME_STATE, "Tournament has not started");
        }
        ServerPlayer player = requirePlayer(game, req.playerId());
        if (player.isSittingOut()) {
            throw new GameServerException(ErrorCode.INVALID_GAME_STATE, "Player is already eliminated");
        }
        if (player.isHuman()) {
            throw new GameServerException(ErrorCode.NOT_APPLICABLE, "Cannot remove the human player");
        }
        // Count remaining survivors (chips > 0, not sitting out) to compute finish
        // position.
        int survivors = 0;
        ServerGameTable table = requireTable(game);
        for (int s = 0; s < table.getSeats(); s++) {
            ServerPlayer p = table.getPlayer(s);
            if (p != null && !p.isSittingOut() && p.getChipCount() > 0 && p.getID() != req.playerId()) {
                survivors++;
            }
        }
        player.setChipCount(0);
        player.setSittingOut(true);
        player.setFinishPosition(survivors + 1);
        game.getEventBus().publish(new GameEvent.PlayerEliminated(table.getNumber(), req.playerId(), survivors + 1));
        broadcastGameState(game, id);
        logger.info("[CHEAT] remove-player: game={} player={} finishPos={}", id, req.playerId(), survivors + 1);
        return ResponseEntity.ok().build();
    }

    /**
     * Swap a card. {@code location} is either {@code "COMMUNITY:<index>"} or
     * {@code "PLAYER:<playerId>:<index>"}. {@code newCard} is a card string such as
     * {@code "Ah"}.
     */
    @PostMapping("/card")
    public ResponseEntity<Void> changeCard(@PathVariable("id") String id, @RequestBody CardRequest req) {
        GameInstance game = requirePracticeOwner(id);
        ServerGameTable table = requireTable(game);
        ServerHand hand = (ServerHand) table.getHoldemHand();
        if (hand == null) {
            throw new GameServerException(ErrorCode.INVALID_GAME_STATE, "No active hand — cannot change cards");
        }
        Card newCard = Card.getCard(req.newCard());
        if (newCard == null) {
            throw new IllegalArgumentException("Invalid card: " + req.newCard());
        }
        String[] parts = req.location().split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid location format: " + req.location());
        }
        if ("COMMUNITY".equalsIgnoreCase(parts[0])) {
            int index = Integer.parseInt(parts[1]);
            hand.setCommunityCard(index, newCard);
        } else if ("PLAYER".equalsIgnoreCase(parts[0])) {
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid PLAYER location format: " + req.location());
            }
            int playerId = Integer.parseInt(parts[1]);
            int index = Integer.parseInt(parts[2]);
            hand.setPlayerCard(playerId, index, newCard);
        } else {
            throw new IllegalArgumentException("Unknown location type: " + parts[0]);
        }
        broadcastGameState(game, id);
        logger.info("[CHEAT] card: game={} location={} newCard={}", id, req.location(), req.newCard());
        return ResponseEntity.ok().build();
    }

    /**
     * Override the AI skill level for a player (1–10). Takes effect on the next AI
     * decision.
     */
    @PostMapping("/ai-strategy")
    public ResponseEntity<Void> setAiStrategy(@PathVariable("id") String id, @RequestBody AiStrategyRequest req) {
        if (req.skillLevel() < 1 || req.skillLevel() > 10) {
            throw new IllegalArgumentException("Skill level must be between 1 and 10");
        }
        GameInstance game = requirePracticeOwner(id);
        ServerTournamentContext tournament = game.getTournament();
        if (tournament == null) {
            throw new GameServerException(ErrorCode.INVALID_GAME_STATE, "Tournament has not started");
        }
        tournament.getAiStrategyOverrides().put(req.playerId(), req.skillLevel());
        // Update the player's skill level so it reflects in GAME_STATE snapshots.
        // Note: the current AI provider is random and does not read skillLevel; this
        // override will take effect when a skill-aware provider is wired in.
        ServerPlayer player = (ServerPlayer) tournament.getPlayerByID(req.playerId());
        if (player != null) {
            player.setSkillLevel(req.skillLevel());
        }
        broadcastGameState(game, id);
        logger.info("[CHEAT] ai-strategy: game={} player={} skillLevel={}", id, req.playerId(), req.skillLevel());
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Verify the caller is authenticated, the game exists, is practice-mode, and
     * owned by the caller.
     */
    private GameInstance requirePracticeOwner(String gameId) {
        long profileId = getCallerProfileId();
        GameInstance game = gameInstanceManager.getGame(gameId);
        if (game == null) {
            throw new GameServerException(ErrorCode.GAME_NOT_FOUND, "Game not found: " + gameId);
        }
        if (game.getConfig().practiceConfig() == null) {
            throw new GameServerException(ErrorCode.NOT_APPLICABLE,
                    "Cheat actions are only available in practice mode");
        }
        if (profileId != game.getOwnerProfileId()) {
            throw new GameServerException(ErrorCode.NOT_GAME_OWNER, "Only the game owner may use cheat actions");
        }
        return game;
    }

    private long getCallerProfileId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationFilter.JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getProfileId();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    private ServerPlayer requirePlayer(GameInstance game, int playerId) {
        ServerTournamentContext tournament = game.getTournament();
        if (tournament == null) {
            throw new GameServerException(ErrorCode.INVALID_GAME_STATE, "Tournament has not started");
        }
        ServerPlayer player = (ServerPlayer) tournament.getPlayerByID(playerId);
        if (player == null) {
            throw new GameServerException(ErrorCode.PLAYER_NOT_FOUND, "Player not found: " + playerId);
        }
        return player;
    }

    private ServerGameTable requireTable(GameInstance game) {
        ServerTournamentContext tournament = game.getTournament();
        if (tournament == null || tournament.getNumTables() == 0) {
            throw new GameServerException(ErrorCode.INVALID_GAME_STATE, "Tournament has not started");
        }
        return (ServerGameTable) tournament.getTable(0);
    }

    private void broadcastGameState(GameInstance game, String gameId) {
        for (PlayerConnection conn : connectionManager.getConnections(gameId)) {
            GameStateSnapshot snap = game.getGameStateSnapshot(conn.getProfileId());
            if (snap != null) {
                conn.sendMessage(converter.createGameStateMessage(gameId, snap));
            }
        }
    }
}
