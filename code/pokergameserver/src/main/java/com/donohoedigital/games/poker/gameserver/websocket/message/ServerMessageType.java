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
package com.donohoedigital.games.poker.gameserver.websocket.message;

/**
 * Server-to-client WebSocket message types.
 *
 * Defines all message types that can be sent from the server to connected
 * clients during a poker game session.
 */
public enum ServerMessageType {
    /** Connection established, includes full game state snapshot */
    CONNECTED,

    /** Full game state update */
    GAME_STATE,

    /** New hand started */
    HAND_STARTED,

    /** Hole cards dealt to player (private) */
    HOLE_CARDS_DEALT,

    /** Community cards dealt */
    COMMUNITY_CARDS_DEALT,

    /** Action required from player (private) */
    ACTION_REQUIRED,

    /** Player performed an action */
    PLAYER_ACTED,

    /** Player action timed out */
    ACTION_TIMEOUT,

    /** Hand complete with results */
    HAND_COMPLETE,

    /** Blind level changed */
    LEVEL_CHANGED,

    /** Player eliminated from tournament */
    PLAYER_ELIMINATED,

    /** Rebuy offered to player */
    REBUY_OFFERED,

    /** Add-on offered to player */
    ADDON_OFFERED,

    /** Game/tournament complete */
    GAME_COMPLETE,

    /** Player joined game */
    PLAYER_JOINED,

    /** Player left the game intentionally */
    PLAYER_LEFT,

    /** Player lost connection during game; may reconnect */
    PLAYER_DISCONNECTED,

    /** Pot won by one or more players */
    POT_AWARDED,

    /** Showdown phase begins (cards to be revealed) */
    SHOWDOWN_STARTED,

    /** Player purchased a rebuy */
    PLAYER_REBUY,

    /** Player purchased an add-on */
    PLAYER_ADDON,

    /** Game paused */
    GAME_PAUSED,

    /** Game resumed */
    GAME_RESUMED,

    /** Player kicked by owner */
    PLAYER_KICKED,

    /** Chat message */
    CHAT_MESSAGE,

    /** Timer update */
    TIMER_UPDATE,

    /** Error occurred */
    ERROR
}
