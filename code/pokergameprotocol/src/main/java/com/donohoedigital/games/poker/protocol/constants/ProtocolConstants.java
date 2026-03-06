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
package com.donohoedigital.games.poker.protocol.constants;

/**
 * Constants that define the client-server protocol contract.
 *
 * <p>
 * These values must be agreed upon by both client and server. They are
 * extracted from {@code PokerConstants} in the pokerengine module so that the
 * client can depend on this lightweight protocol module instead of the full
 * engine.
 * </p>
 */
public class ProtocolConstants {

    private ProtocolConstants() {
    }

    // Seat counts

    /** Default number of seats at a table. */
    public static final int SEATS = 10;

    /** Full ring table format. */
    public static final int SEATS_FULL_RING = 10;

    /** 6-max table format. */
    public static final int SEATS_6MAX = 6;

    /** Heads-up table format. */
    public static final int SEATS_HEADS_UP = 2;

    // Game types

    /** No-limit Texas Hold'em. */
    public static final int TYPE_NO_LIMIT_HOLDEM = 1;

    /** Pot-limit Texas Hold'em. */
    public static final int TYPE_POT_LIMIT_HOLDEM = 2;

    /** Limit Texas Hold'em. */
    public static final int TYPE_LIMIT_HOLDEM = 3;

    // Game type data element string values (used in profile and display)

    /** Data element value for No-Limit Hold'em. */
    public static final String DE_NO_LIMIT_HOLDEM = "nolimit";

    /** Data element value for Pot-Limit Hold'em. */
    public static final String DE_POT_LIMIT_HOLDEM = "potlimit";

    /** Data element value for Limit Hold'em. */
    public static final String DE_LIMIT_HOLDEM = "limit";

    // Scheduled start

    /** Minimum players required for scheduled start. */
    public static final int MIN_SCHEDULED_START_PLAYERS = 2;

    // Player IDs

    /** Player ID for the host of an online game. */
    public static final int PLAYER_ID_HOST = 0;

    /** Temporary player ID placeholder. */
    public static final int PLAYER_ID_TEMP = -1;
}
