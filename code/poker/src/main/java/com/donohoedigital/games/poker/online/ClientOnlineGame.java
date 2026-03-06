/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
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

/**
 * Client-side online game representation for display in game lists. Replaces
 * {@code com.donohoedigital.games.poker.model.OnlineGame} in the desktop
 * client.
 */
public class ClientOnlineGame {

    // modes of play
    public static final int MODE_REG = 1;
    public static final int MODE_PLAY = 2;
    public static final int MODE_STOP = 3;
    public static final int MODE_END = 4;
    public static final int FETCH_MODE_REG_PLAY = 102;

    // column names for table model
    public static final String WAN_TOURNAMENT_NAME = "tournamentname";
    public static final String WAN_HOST_PLAYER = "hostplayer";
    public static final String WAN_MODE = "mode";

    private String url;
    private String hostPlayer;
    private int mode;
    private ClientTournamentProfile tournament;
    private String hostingType;

    public ClientOnlineGame() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHostPlayer() {
        return hostPlayer;
    }

    public void setHostPlayer(String hostPlayer) {
        this.hostPlayer = hostPlayer;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public ClientTournamentProfile getTournament() {
        return tournament;
    }

    public void setTournament(ClientTournamentProfile tournament) {
        this.tournament = tournament;
    }

    public String getHostingType() {
        return hostingType;
    }

    public void setHostingType(String hostingType) {
        this.hostingType = hostingType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ClientOnlineGame))
            return false;
        final ClientOnlineGame other = (ClientOnlineGame) o;
        return url != null && url.equals(other.url);
    }

    @Override
    public int hashCode() {
        return url == null ? super.hashCode() : 31 * super.hashCode() + url.hashCode();
    }

    @Override
    public String toString() {
        return "ClientOnlineGame[" + url + "]";
    }
}
