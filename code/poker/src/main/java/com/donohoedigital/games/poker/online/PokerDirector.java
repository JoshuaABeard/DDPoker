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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.engine.GameManager;
import com.donohoedigital.games.poker.PokerPlayer;

/**
 * Combined interface for the tournament director as seen by the desktop UI.
 *
 * <p>
 * {@link com.donohoedigital.games.poker.ShowTournamentTable} calls
 * {@code TD()}, which previously returned a concrete
 * {@link TournamentDirector}. This interface lets
 * {@link WebSocketTournamentDirector} satisfy both the {@link GameManager} and
 * {@link ChatManager} contracts while adding the two additional methods that
 * {@code ShowTournamentTable} calls directly:
 *
 * <ul>
 * <li>{@link #setPaused(boolean)} — called from the table's right-click
 * menu</li>
 * <li>{@link #playerUpdate(PokerPlayer, String)} — called for player settings
 * changes</li>
 * </ul>
 */
public interface PokerDirector extends GameManager, ChatManager {

    /**
     * Pauses or resumes the game.
     *
     * <p>
     * Sent as an {@code ADMIN_PAUSE} or {@code ADMIN_RESUME} message to the server.
     *
     * @param paused
     *            {@code true} to pause, {@code false} to resume
     */
    void setPaused(boolean paused);

    /**
     * Notifies the director of a player settings update.
     *
     * <p>
     * No-op in practice mode (AI-only games have no settings to propagate). In M6
     * multiplayer, routes the update to the server.
     *
     * @param player
     *            the player whose settings changed
     * @param settings
     *            serialized settings string
     */
    void playerUpdate(PokerPlayer player, String settings);
}
