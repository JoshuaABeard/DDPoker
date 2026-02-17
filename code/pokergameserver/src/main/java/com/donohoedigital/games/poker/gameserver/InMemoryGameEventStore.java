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
 * In-memory implementation of {@link IGameEventStore}.
 *
 * <p>
 * Uses CopyOnWriteArrayList for thread-safe concurrent access. Suitable for M1
 * single-server deployments. M2 adds DatabaseGameEventStore for persistence
 * across restarts.
 */
public class InMemoryGameEventStore implements IGameEventStore {
    private final String gameId;
    private final AtomicLong sequenceNumber;
    private final List<StoredEvent> events;

    /**
     * Create a new event store for a game.
     *
     * @param gameId
     *            unique identifier for the game
     */
    public InMemoryGameEventStore(String gameId) {
        if (gameId == null || gameId.isBlank()) {
            throw new IllegalArgumentException("Game ID cannot be null or blank");
        }
        this.gameId = gameId;
        this.sequenceNumber = new AtomicLong(0);
        this.events = new CopyOnWriteArrayList<>();
    }

    @Override
    public void append(GameEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        StoredEvent stored = new StoredEvent(gameId, sequenceNumber.incrementAndGet(), event.getClass().getSimpleName(),
                event, Instant.now());

        events.add(stored);
    }

    @Override
    public List<StoredEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    @Override
    public List<StoredEvent> getEventsSince(long afterSequence) {
        return events.stream().filter(e -> e.sequenceNumber() > afterSequence).toList();
    }

    @Override
    public String getGameId() {
        return gameId;
    }

    @Override
    public void clear() {
        events.clear();
        sequenceNumber.set(0);
    }

    @Override
    public long getCurrentSequenceNumber() {
        return sequenceNumber.get();
    }
}
