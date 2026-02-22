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
 * Client-to-server WebSocket message types.
 *
 * Defines all message types that can be sent from clients to the server during
 * a poker game session.
 */
public enum ClientMessageType {
    /** Player action (fold, check, call, bet, raise, all-in) */
    PLAYER_ACTION,

    /** Rebuy decision (accept/decline) */
    REBUY_DECISION,

    /** Add-on decision (accept/decline) */
    ADDON_DECISION,

    /** Chat message */
    CHAT,

    /** Player sits out */
    SIT_OUT,

    /** Player comes back from sitting out */
    COME_BACK,

    /** Admin kicks a player (owner only) */
    ADMIN_KICK,

    /** Admin pauses the game (owner only) */
    ADMIN_PAUSE,

    /** Admin resumes the game (owner only) */
    ADMIN_RESUME,

    /** Never Broke decision (accept/decline) */
    NEVER_BROKE_DECISION
}
