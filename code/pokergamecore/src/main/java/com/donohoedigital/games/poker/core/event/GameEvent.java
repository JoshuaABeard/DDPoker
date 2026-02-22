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
package com.donohoedigital.games.poker.core.event;

import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.core.state.TableState;
import java.util.List;

/**
 * Sealed interface for game events. Replaces PokerTableEvent bitmask event
 * types with type-safe records.
 */
public sealed interface GameEvent {

    /** A new hand has started. */
    record HandStarted(int tableId, int handNumber) implements GameEvent {
    }

    /** A player performed an action (check, call, bet, raise, fold). */
    record PlayerActed(int tableId, int playerId, ActionType action, int amount) implements GameEvent {
    }

    /** Community cards were dealt for a betting round. */
    record CommunityCardsDealt(int tableId, BettingRound round) implements GameEvent {
    }

    /** A hand has completed. */
    record HandCompleted(int tableId) implements GameEvent {
    }

    /** The table state changed. */
    record TableStateChanged(int tableId, TableState oldState, TableState newState) implements GameEvent {
    }

    /** A player was added to the table. */
    record PlayerAdded(int tableId, int playerId, int seat) implements GameEvent {
    }

    /** A player was removed from the table. */
    record PlayerRemoved(int tableId, int playerId, int seat) implements GameEvent {
    }

    /** The tournament level changed. */
    record LevelChanged(int tableId, int newLevel) implements GameEvent {
    }

    /** The dealer button moved to a new position. */
    record ButtonMoved(int tableId, int newSeat) implements GameEvent {
    }

    /** Showdown has started. */
    record ShowdownStarted(int tableId) implements GameEvent {
    }

    /** A pot was awarded to one or more players. */
    record PotAwarded(int tableId, int potIndex, int[] winnerIds, int amount) implements GameEvent {
    }

    /** The tournament has completed. */
    record TournamentCompleted(int winnerId) implements GameEvent {
    }

    /** A table break has started. */
    record BreakStarted(int tableId) implements GameEvent {
    }

    /** A table break has ended. */
    record BreakEnded(int tableId) implements GameEvent {
    }

    /** Color-up process has completed. */
    record ColorUpCompleted(int tableId) implements GameEvent {
    }

    /** The current player (whose turn it is) changed. */
    record CurrentPlayerChanged(int tableId, int playerId) implements GameEvent {
    }

    /** A player performed a rebuy. */
    record PlayerRebuy(int tableId, int playerId, int amount) implements GameEvent {
    }

    /** A player performed an addon. */
    record PlayerAddon(int tableId, int playerId, int amount) implements GameEvent {
    }

    /** An observer was added to the table. */
    record ObserverAdded(int tableId, int observerId) implements GameEvent {
    }

    /** An observer was removed from the table. */
    record ObserverRemoved(int tableId, int observerId) implements GameEvent {
    }

    /** Table cleaning process has completed. */
    record CleaningDone(int tableId) implements GameEvent {
    }

    /** A player was eliminated from the tournament. */
    record PlayerEliminated(int tableId, int playerId, int finishPosition) implements GameEvent {
    }

    /** A player action timed out; an auto-action was applied. */
    record ActionTimeout(int playerId, ActionType autoAction) implements GameEvent {
    }

    /** A rebuy has been offered to a player who busted. */
    record RebuyOffered(int tableId, int playerId, int cost, int chips, int timeoutSeconds) implements GameEvent {
    }

    /**
     * Never Broke has been offered to the human player who just busted (practice
     * mode).
     */
    record NeverBrokeOffered(int tableId, int playerId, int timeoutSeconds) implements GameEvent {
    }

    /** An addon has been offered to an eligible player. */
    record AddonOffered(int tableId, int playerId, int cost, int chips, int timeoutSeconds) implements GameEvent {
    }

    /** Chips transferred from one player to another (Never Broke feature). */
    record ChipsTransferred(int tableId, int fromPlayerId, int toPlayerId, int amount) implements GameEvent {
    }

    /** Color-up chip race started. */
    record ColorUpStarted(int tableId, List<ColorUpPlayerData> players, int newMinChip) implements GameEvent {
    }

    /** Per-player color-up result (used in ColorUpStarted). */
    record ColorUpPlayerData(int playerId, List<String> cards, boolean won, boolean broke, int finalChips) {
    }

    /**
     * Server is pausing during all-in runout waiting for human to click Continue.
     */
    record AllInRunoutPaused(int tableId) implements GameEvent {
    }
}
