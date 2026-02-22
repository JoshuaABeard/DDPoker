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
 * Sealed interface hierarchy for client-to-server message payloads.
 *
 * Each record matches the master plan JSON spec exactly. Note: SIT_OUT,
 * COME_BACK, ADMIN_PAUSE, and ADMIN_RESUME have no data fields and are
 * represented by their respective no-data records.
 */
public sealed interface ClientMessageData permits ClientMessageData.PlayerActionData,ClientMessageData.RebuyDecisionData,ClientMessageData.AddonDecisionData,ClientMessageData.ChatData,ClientMessageData.SitOutData,ClientMessageData.ComeBackData,ClientMessageData.AdminKickData,ClientMessageData.AdminPauseData,ClientMessageData.AdminResumeData,ClientMessageData.NeverBrokeDecisionData {

    /**
     * Player action: FOLD, CHECK, CALL, BET, RAISE, or ALL_IN.
     *
     * @param action
     *            Action type string (case-insensitive)
     * @param amount
     *            Required for BET and RAISE, ignored for others
     */
    record PlayerActionData(String action, int amount) implements ClientMessageData {
    }

    /**
     * Rebuy decision (accept or decline).
     *
     * @param accept
     *            true to accept the rebuy, false to decline
     */
    record RebuyDecisionData(boolean accept) implements ClientMessageData {
    }

    /**
     * Add-on decision (accept or decline).
     *
     * @param accept
     *            true to accept the add-on, false to decline
     */
    record AddonDecisionData(boolean accept) implements ClientMessageData {
    }

    /**
     * Chat message.
     *
     * @param message
     *            Chat message text (max 500 chars after sanitization)
     * @param tableChat
     *            true for table chat, false for private/observer chat
     */
    record ChatData(String message, boolean tableChat) implements ClientMessageData {
    }

    /** Player opts to sit out next hand. No data fields. */
    record SitOutData() implements ClientMessageData {
    }

    /** Player opts to come back from sitting out. No data fields. */
    record ComeBackData() implements ClientMessageData {
    }

    /**
     * Admin kick: remove a player from the game (owner only).
     *
     * @param playerId
     *            Profile ID of the player to kick
     */
    record AdminKickData(long playerId) implements ClientMessageData {
    }

    /** Admin pause: pause the game (owner only). No data fields. */
    record AdminPauseData() implements ClientMessageData {
    }

    /** Admin resume: resume a paused game (owner only). No data fields. */
    record AdminResumeData() implements ClientMessageData {
    }

    /**
     * Never Broke decision (accept or decline).
     *
     * @param accept
     *            true to accept the Never Broke rescue, false to decline
     */
    record NeverBrokeDecisionData(boolean accept) implements ClientMessageData {
    }
}
