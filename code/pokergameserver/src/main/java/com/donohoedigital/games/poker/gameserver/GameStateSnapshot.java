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
package com.donohoedigital.games.poker.gameserver;

import com.donohoedigital.games.poker.engine.Card;

import java.util.List;

/**
 * A snapshot of game state for a specific player. Includes all public
 * information plus this player's hole cards, but excludes other players' hole
 * cards (except at showdown).
 *
 * <p>
 * This is the security boundary that prevents card leakage. In Milestone 3,
 * this will be serialized to JSON and sent via WebSocket.
 *
 * @param tableId
 *            table number
 * @param handNumber
 *            current hand number
 * @param myHoleCards
 *            this player's hole cards (null if not in hand)
 * @param communityCards
 *            community cards (visible to all)
 * @param players
 *            all players at the table with public state
 * @param pots
 *            current pot information
 * @param dealerSeat
 *            seat index of the dealer button (-1 if unknown)
 * @param smallBlindSeat
 *            seat index of the small blind (-1 if no hand in progress)
 * @param bigBlindSeat
 *            seat index of the big blind (-1 if no hand in progress)
 * @param currentActorSeat
 *            seat index of the player whose turn it is (-1 if nobody acting)
 * @param bettingRound
 *            current betting round name (null if no hand in progress)
 * @param level
 *            current tournament level
 * @param smallBlind
 *            current small blind amount
 * @param bigBlind
 *            current big blind amount
 * @param ante
 *            current ante amount
 */
public record GameStateSnapshot(int tableId, int handNumber, Card[] myHoleCards, Card[] communityCards,
        List<PlayerState> players, List<PotState> pots, int dealerSeat, int smallBlindSeat, int bigBlindSeat,
        int currentActorSeat, String bettingRound, int level, int smallBlind, int bigBlind, int ante) {

    /**
     * Per-player state visible in the snapshot.
     *
     * @param playerId
     *            player ID
     * @param playerName
     *            player name
     * @param chipCount
     *            current chip count
     * @param seat
     *            seat number
     * @param folded
     *            true if folded
     * @param allIn
     *            true if all-in
     * @param holeCards
     *            hole cards (only included for the requesting player or at
     *            showdown)
     * @param currentBet
     *            total amount bet by this player in the current betting round
     */
    public record PlayerState(int playerId, String playerName, int chipCount, int seat, boolean folded, boolean allIn,
            Card[] holeCards, int currentBet) {
    }

    /**
     * Pot state visible in the snapshot.
     *
     * @param amount
     *            chips in the pot
     * @param eligiblePlayerIds
     *            players eligible to win this pot
     */
    public record PotState(int amount, List<Integer> eligiblePlayerIds) {
    }
}
