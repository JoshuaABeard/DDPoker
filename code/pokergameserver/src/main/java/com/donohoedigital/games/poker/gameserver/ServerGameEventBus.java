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
import com.donohoedigital.games.poker.core.event.GameEventBus;

import java.util.function.Consumer;

/**
 * Server-side event bus that broadcasts game events to listeners, persists to
 * event store, and optionally broadcasts to connected clients via WebSocket.
 *
 * <p>
 * Event flow: Event published → Persisted to event store → In-process listeners
 * notified → Broadcast to WebSocket clients
 */
public class ServerGameEventBus extends GameEventBus {
    private final IGameEventStore eventStore;
    private volatile Consumer<GameEvent> broadcastCallback;

    /**
     * Create a new server event bus.
     *
     * @param eventStore
     *            the event store for persistence (required)
     */
    public ServerGameEventBus(IGameEventStore eventStore) {
        if (eventStore == null) {
            throw new IllegalArgumentException("Event store cannot be null");
        }
        this.eventStore = eventStore;
    }

    /**
     * Set the callback for broadcasting events to connected clients.
     *
     * <p>
     * GameInstance sets this to forward events to WebSocket connections.
     *
     * @param broadcastCallback
     *            the callback to invoke for each event, or null to disable
     *            broadcasting
     */
    public void setBroadcastCallback(Consumer<GameEvent> broadcastCallback) {
        this.broadcastCallback = broadcastCallback;
    }

    /**
     * Publish an event. The event is persisted to the event store, published to
     * in-process listeners, and broadcasted to connected clients.
     *
     * @param event
     *            the event to publish
     */
    @Override
    public void publish(GameEvent event) {
        // 1. Persist to event store (always — event store is the authoritative
        // log)
        eventStore.append(event);

        // 2. Notify in-process listeners (same as base GameEventBus)
        super.publish(event);

        // 3. Broadcast to connected clients
        if (broadcastCallback != null) {
            broadcastCallback.accept(event);
        }
    }

    /**
     * Broadcast a table state change event.
     *
     * <p>
     * This is a convenience method for publishing table state transitions, which
     * are the most common events during game execution.
     *
     * @param table
     *            the table whose state changed
     */
    public void broadcastTableState(ServerGameTable table) {
        publish(new GameEvent.TableStateChanged(table.getNumber(), table.getPreviousTableState(),
                table.getTableState()));
    }

    /**
     * Get the event store used by this bus.
     *
     * @return the event store
     */
    public IGameEventStore getEventStore() {
        return eventStore;
    }
}
