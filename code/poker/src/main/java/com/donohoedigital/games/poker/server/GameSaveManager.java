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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Queries the embedded game server at startup for any in-progress or paused
 * games that can be resumed by the local user.
 *
 * <p>
 * Practice games auto-persist via the server's event store. When the desktop
 * client restarts, this class discovers incomplete games so the UI can offer to
 * resume them.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * gameSaveManager_.loadResumableGames();
 * if (gameSaveManager_.hasResumableGames()) {
 * 	// offer resume dialog
 * 	GameSummary game = gameSaveManager_.getResumableGames().get(0);
 * }
 * </pre>
 */
public class GameSaveManager {

    private static final Logger logger = LogManager.getLogger(GameSaveManager.class);

    private final EmbeddedGameServer embeddedServer;
    private List<GameSummary> resumableGames = Collections.emptyList();

    public GameSaveManager(EmbeddedGameServer embeddedServer) {
        this.embeddedServer = embeddedServer;
    }

    /**
     * Queries the embedded server for games with status {@code IN_PROGRESS} or
     * {@code PAUSED}. Stores the results for later retrieval via
     * {@link #getResumableGames()}.
     *
     * <p>
     * Errors are logged and swallowed — a failure to load resumable games is
     * non-fatal; the user simply starts a new game.
     */
    public void loadResumableGames() {
        try {
            GameServerRestClient client = new GameServerRestClient(embeddedServer.getPort());
            String jwt = embeddedServer.getLocalUserJwt();
            List<GameSummary> all = client.listGames(jwt);
            resumableGames = all.stream().filter(g -> "IN_PROGRESS".equals(g.status()) || "PAUSED".equals(g.status()))
                    .collect(Collectors.toList());
            if (!resumableGames.isEmpty()) {
                logger.info("Found {} resumable game(s) from previous session", resumableGames.size());
            }
        } catch (Exception e) {
            logger.warn("Could not load resumable games from embedded server", e);
        }
    }

    /**
     * Returns an unmodifiable snapshot of the in-progress or paused games found at
     * last {@link #loadResumableGames()} call.
     */
    public List<GameSummary> getResumableGames() {
        return Collections.unmodifiableList(resumableGames);
    }

    /**
     * Returns {@code true} if at least one resumable game is available.
     */
    public boolean hasResumableGames() {
        return !resumableGames.isEmpty();
    }
}
