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

import com.donohoedigital.games.poker.core.event.GameEvent;

import java.util.List;

/**
 * Append-only event log for game events. The event store is the authoritative
 * record of every game action, enabling crash recovery, game replay, and
 * simulation analysis.
 *
 * <p>
 * Implementations must be thread-safe for concurrent event appending.
 */
public interface IGameEventStore {
    /**
     * Append an event to the store.
     *
     * @param event
     *            the event to append (must not be null)
     * @throws IllegalArgumentException
     *             if event is null
     */
    void append(GameEvent event);

    /**
     * Get all events in this store.
     *
     * @return unmodifiable list of all stored events in sequence order
     */
    List<StoredEvent> getEvents();

    /**
     * Get events after a specific sequence number.
     *
     * @param afterSequence
     *            the sequence number to start after
     * @return unmodifiable list of events with sequence numbers greater than the
     *         specified value
     */
    List<StoredEvent> getEventsSince(long afterSequence);

    /**
     * Get the game ID for this event store.
     *
     * @return the game ID
     */
    String getGameId();

    /**
     * Clear all events from the store and reset the sequence number.
     * <p>
     * Used primarily for testing or game reset scenarios. Implementation must be
     * thread-safe.
     */
    void clear();

    /**
     * Get the current sequence number (number of events stored).
     *
     * @return the current sequence number
     */
    long getCurrentSequenceNumber();
}
