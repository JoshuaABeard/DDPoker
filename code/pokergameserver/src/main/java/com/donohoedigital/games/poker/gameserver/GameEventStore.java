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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Append-only event log for game events. The event store is the authoritative
 * record of every game action, enabling crash recovery, game replay, and
 * simulation analysis.
 *
 * <p>
 * M1 implementation uses in-memory storage. M2 will add database persistence
 * with the same interface.
 */
public class GameEventStore {
    private final String gameId;
    private final AtomicLong sequenceNumber;
    private final List<StoredEvent> events;

    /**
     * Create a new event store for a game.
     *
     * @param gameId
     *            unique identifier for the game
     */
    public GameEventStore(String gameId) {
        if (gameId == null || gameId.isBlank()) {
            throw new IllegalArgumentException("Game ID cannot be null or blank");
        }
        this.gameId = gameId;
        this.sequenceNumber = new AtomicLong(0);
        this.events = new CopyOnWriteArrayList<>();
    }

    /**
     * Append an event to the store.
     * <p>
     * Thread-safe: CopyOnWriteArrayList ensures safe concurrent access.
     *
     * @param event
     *            the event to append
     */
    public void append(GameEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        StoredEvent stored = new StoredEvent(gameId, sequenceNumber.incrementAndGet(), event.getClass().getSimpleName(),
                event, Instant.now());

        events.add(stored);
    }

    /**
     * Get all events in this store.
     *
     * @return unmodifiable list of all stored events in sequence order
     */
    public List<StoredEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Get events after a specific sequence number.
     *
     * @param afterSequence
     *            the sequence number to start after
     * @return unmodifiable list of events with sequence numbers greater than the
     *         specified value
     */
    public List<StoredEvent> getEventsSince(long afterSequence) {
        return events.stream().filter(e -> e.sequenceNumber() > afterSequence).toList();
    }

    /**
     * Get the game ID for this event store.
     *
     * @return the game ID
     */
    public String getGameId() {
        return gameId;
    }

    /**
     * Clear all events from the store and reset the sequence number.
     * <p>
     * Thread-safe: CopyOnWriteArrayList ensures safe concurrent access. Used
     * primarily for testing or game reset scenarios.
     */
    public void clear() {
        events.clear();
        sequenceNumber.set(0);
    }

    /**
     * Get the current sequence number (number of events stored).
     *
     * @return the current sequence number
     */
    public long getCurrentSequenceNumber() {
        return sequenceNumber.get();
    }
}
