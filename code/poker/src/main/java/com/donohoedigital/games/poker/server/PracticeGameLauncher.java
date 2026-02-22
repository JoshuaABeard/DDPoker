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

import com.donohoedigital.games.poker.PlayerProfileOptions;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin orchestrator for starting a practice game against the embedded server.
 *
 * <p>
 * Responsibilities:
 * <ol>
 * <li>Obtain the local-user JWT from {@link EmbeddedGameServer}</li>
 * <li>Post the {@link TournamentProfile} to the embedded server via
 * {@link GameServerRestClient}</li>
 * <li>Store the returned {@code gameId} + JWT on the {@link PokerGame} so that
 * {@link com.donohoedigital.games.poker.online.WebSocketTournamentDirector} can
 * read them when the phase starts</li>
 * </ol>
 *
 * <p>
 * Call {@link #launch(TournamentProfile, PokerGame)} from the practice-game
 * setup flow (before the phase chain transitions to
 * {@code WebSocketTournamentDirector}).
 */
public class PracticeGameLauncher {

    private static final Logger logger = LogManager.getLogger(PracticeGameLauncher.class);

    private final EmbeddedGameServer embeddedServer;
    private final GameServerRestClient restClient;

    public PracticeGameLauncher(EmbeddedGameServer embeddedServer) {
        this.embeddedServer = embeddedServer;
        this.restClient = new GameServerRestClient(embeddedServer.getPort());
    }

    /**
     * Create a practice game on the embedded server and store the connection info
     * on the given {@link PokerGame}.
     *
     * <p>
     * After this call, {@link PokerGame#getWebSocketConfig()} returns a non-null
     * config that {@code WebSocketTournamentDirector} will use to connect.
     *
     * @param profile
     *            the tournament profile chosen by the user
     * @param game
     *            the PokerGame that will run the practice session
     * @throws GameServerRestClient.GameServerClientException
     *             if the server call fails
     * @throws EmbeddedGameServer.EmbeddedServerStartupException
     *             if the server is not running (should have been caught at startup)
     */
    public void launch(TournamentProfile profile, PokerGame game) {
        if (!embeddedServer.isRunning()) {
            throw new IllegalStateException("Embedded server is not running");
        }

        String jwt = embeddedServer.getLocalUserJwt();
        List<String> aiNames = buildAiNames(profile);

        PlayerProfile playerProfile = PlayerProfileOptions.getDefaultProfile();
        String humanDisplayName = (playerProfile != null) ? playerProfile.getName() : null;

        String gameId = restClient.createPracticeGame(profile, aiNames, defaultSkillLevel(profile), jwt,
                humanDisplayName);

        game.setWebSocketConfig(gameId, jwt, embeddedServer.getPort());
        logger.info("Practice game {} created on embedded server port {}", gameId, embeddedServer.getPort());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Generate AI player names to fill the table. Uses "Computer 1", "Computer 2",
     * etc. for as many AI players as the profile requests (total players minus 1
     * for the human).
     */
    private List<String> buildAiNames(TournamentProfile profile) {
        int numAi = Math.max(0, profile.getNumPlayers() - 1);
        List<String> names = new ArrayList<>(numAi);
        for (int i = 1; i <= numAi; i++) {
            names.add("Computer " + i);
        }
        return names;
    }

    /**
     * Returns medium AI difficulty (level 4). Skill-level picker is an M6 feature.
     */
    private int defaultSkillLevel(TournamentProfile profile) {
        return 4;
    }
}
