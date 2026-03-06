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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.ClientTournamentProfile;
import com.donohoedigital.games.poker.protocol.dto.GameSummary;

import java.util.List;

/**
 * Converts server-side {@link GameSummary} DTOs into desktop-side
 * {@link ClientOnlineGame} objects for display in the game list table.
 *
 * <p>
 * The WebSocket URL is constructed as:
 * <ul>
 * <li>SERVER games: {@code ws://{serverHost}/ws/games/{gameId}} (server host
 * injected at construction)</li>
 * <li>COMMUNITY games: the {@code wsUrl} registered by the host, passed through
 * as-is</li>
 * </ul>
 */
public class GameSummaryConverter {

    /** Host and port of the WAN/embedded server, e.g., {@code localhost:54321}. */
    private final String serverHost;

    /**
     * @param serverHost
     *            host:port of the server (e.g., {@code localhost:54321})
     */
    public GameSummaryConverter(String serverHost) {
        this.serverHost = serverHost;
    }

    /**
     * Convert a single {@link GameSummary} to an {@link ClientOnlineGame} for the
     * list model.
     */
    public ClientOnlineGame convert(GameSummary summary) {
        ClientOnlineGame game = new ClientOnlineGame();

        // Game name via ClientTournamentProfile (display only — no full profile data
        // available)
        ClientTournamentProfile profile = new ClientTournamentProfile(summary.name());
        game.setTournament(profile);

        // Host player
        game.setHostPlayer(summary.ownerName());

        // Mode based on status
        int mode;
        switch (summary.status()) {
            case "WAITING_FOR_PLAYERS" :
                mode = ClientOnlineGame.MODE_REG;
                break;
            case "IN_PROGRESS" :
                mode = ClientOnlineGame.MODE_PLAY;
                break;
            default :
                mode = ClientOnlineGame.MODE_STOP;
        }
        game.setMode(mode);

        // WebSocket URL for joining
        String wsUrl;
        if ("COMMUNITY".equals(summary.hostingType()) && summary.wsUrl() != null) {
            wsUrl = summary.wsUrl();
        } else {
            wsUrl = "ws://" + serverHost + "/ws/games/" + summary.gameId();
        }
        game.setUrl(wsUrl);

        // Transient field for hosting-type column
        game.setHostingType(summary.hostingType());

        return game;
    }

    /**
     * Convert a list of summaries to an {@link ClientOnlineGameList}.
     */
    public ClientOnlineGameList convertAll(List<GameSummary> summaries) {
        ClientOnlineGameList list = new ClientOnlineGameList();
        for (GameSummary s : summaries) {
            list.add(convert(s));
        }
        list.setTotalSize(summaries.size());
        return list;
    }
}
