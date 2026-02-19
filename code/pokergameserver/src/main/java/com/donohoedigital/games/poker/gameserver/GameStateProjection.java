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

import java.util.ArrayList;
import java.util.List;

/**
 * Builds per-player views of game state that hide other players' hole cards.
 *
 * <p>
 * <b>SECURITY CRITICAL:</b> This class enforces the core security rule: never
 * leak hole cards to the wrong player. All WebSocket state broadcasts must go
 * through this projection.
 */
public class GameStateProjection {

    /**
     * Create a state snapshot for a specific player.
     *
     * <p>
     * Includes: all public information + this player's hole cards.
     * <p>
     * Excludes: other players' hole cards (except at showdown).
     *
     * @param table
     *            the game table
     * @param hand
     *            the current hand (null if no hand in progress)
     * @param playerId
     *            the player ID to create the projection for
     * @return the game state snapshot visible to this player
     */
    public static GameStateSnapshot forPlayer(ServerGameTable table, ServerHand hand, int playerId) {
        int tableId = table.getNumber();
        int handNumber = table.getHandNum();

        // Player's hole cards (only if they're in the hand)
        Card[] myHoleCards = null;
        if (hand != null) {
            ServerPlayer player = findPlayer(table, playerId);
            if (player != null) {
                myHoleCards = hand.getPlayerCards(player);
            }
        }

        // Community cards (visible to all)
        Card[] communityCards = null;
        if (hand != null) {
            communityCards = hand.getCommunityCards();
        }

        // Build player states
        List<GameStateSnapshot.PlayerState> playerStates = new ArrayList<>();
        for (int seat = 0; seat < table.getSeats(); seat++) {
            ServerPlayer player = table.getPlayer(seat);
            if (player != null) {
                // Include hole cards ONLY for the requesting player
                Card[] holeCards = null;
                if (player.getID() == playerId && hand != null) {
                    holeCards = hand.getPlayerCards(player);
                }

                int currentBet = (hand != null) ? hand.getPlayerBet(player.getID()) : 0;
                playerStates.add(new GameStateSnapshot.PlayerState(player.getID(), player.getName(),
                        player.getChipCount(), seat, player.isFolded(), player.isAllIn(), holeCards, currentBet));
            }
        }

        // Build pot states
        List<GameStateSnapshot.PotState> potStates = new ArrayList<>();
        if (hand != null) {
            List<ServerPot> pots = hand.getPots();
            for (ServerPot pot : pots) {
                List<Integer> eligiblePlayerIds = new ArrayList<>();
                for (ServerPlayer eligible : pot.getEligiblePlayers()) {
                    eligiblePlayerIds.add(eligible.getID());
                }
                potStates.add(new GameStateSnapshot.PotState(pot.getChips(), eligiblePlayerIds));
            }
            // Include pending bets (blinds/antes not yet moved to a pot via calcPots)
            int pendingBets = hand.getPendingBetTotal();
            if (pendingBets > 0) {
                potStates.add(new GameStateSnapshot.PotState(pendingBets, List.of()));
            }
        }

        return new GameStateSnapshot(tableId, handNumber, myHoleCards, communityCards, playerStates, potStates);
    }

    /**
     * Create a showdown snapshot where all active players' cards are revealed.
     *
     * <p>
     * Used when the hand reaches showdown - all non-folded players' cards become
     * visible.
     *
     * @param table
     *            the game table
     * @param hand
     *            the current hand
     * @param playerId
     *            the player ID to create the projection for
     * @return the game state snapshot with showdown cards revealed
     */
    public static GameStateSnapshot forShowdown(ServerGameTable table, ServerHand hand, int playerId) {
        int tableId = table.getNumber();
        int handNumber = table.getHandNum();

        // Requesting player's own hole cards (for the myHoleCards field)
        Card[] myHoleCards = null;
        if (hand != null) {
            ServerPlayer player = findPlayer(table, playerId);
            if (player != null) {
                myHoleCards = hand.getPlayerCards(player);
            }
        }

        // Community cards (visible to all)
        Card[] communityCards = null;
        if (hand != null) {
            communityCards = hand.getCommunityCards();
        }

        // Build player states — at showdown, reveal cards for all non-folded players
        List<GameStateSnapshot.PlayerState> playerStates = new ArrayList<>();
        for (int seat = 0; seat < table.getSeats(); seat++) {
            ServerPlayer player = table.getPlayer(seat);
            if (player != null) {
                Card[] holeCards = null;
                if (hand != null && !player.isFolded()) {
                    holeCards = hand.getPlayerCards(player);
                }
                // At showdown all bets have been committed to pots
                playerStates.add(new GameStateSnapshot.PlayerState(player.getID(), player.getName(),
                        player.getChipCount(), seat, player.isFolded(), player.isAllIn(), holeCards, 0));
            }
        }

        // Build pot states
        List<GameStateSnapshot.PotState> potStates = new ArrayList<>();
        if (hand != null) {
            List<ServerPot> pots = hand.getPots();
            for (ServerPot pot : pots) {
                List<Integer> eligiblePlayerIds = new ArrayList<>();
                for (ServerPlayer eligible : pot.getEligiblePlayers()) {
                    eligiblePlayerIds.add(eligible.getID());
                }
                potStates.add(new GameStateSnapshot.PotState(pot.getChips(), eligiblePlayerIds));
            }
        }

        return new GameStateSnapshot(tableId, handNumber, myHoleCards, communityCards, playerStates, potStates);
    }

    private static ServerPlayer findPlayer(ServerGameTable table, int playerId) {
        for (int seat = 0; seat < table.getSeats(); seat++) {
            ServerPlayer player = table.getPlayer(seat);
            if (player != null && player.getID() == playerId) {
                return player;
            }
        }
        return null;
    }
}
