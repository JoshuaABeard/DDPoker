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

import static org.junit.jupiter.api.Assertions.*;

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.core.event.GameEventListener;
import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.core.state.TableState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ServerGameEventBusTest {

    private ServerGameEventBus eventBus;
    private InMemoryGameEventStore eventStore;
    private List<GameEvent> broadcastedEvents;
    private List<GameEvent> listenerReceivedEvents;

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryGameEventStore("test-game");
        eventBus = new ServerGameEventBus(eventStore);
        broadcastedEvents = new ArrayList<>();
        listenerReceivedEvents = new ArrayList<>();
    }

    @Test
    void testConstructor() {
        assertNotNull(eventBus);
        assertEquals(0, eventBus.getListenerCount());
    }

    @Test
    void testPublishStoresEventInEventStore() {
        GameEvent event = new GameEvent.HandStarted(0, 1);
        eventBus.publish(event);

        List<StoredEvent> stored = eventStore.getEvents();
        assertEquals(1, stored.size());
        assertEquals(event, stored.get(0).event());
    }

    @Test
    void testPublishNotifiesSubscribedListeners() {
        // Subscribe a listener
        GameEventListener listener = listenerReceivedEvents::add;
        eventBus.subscribe(listener);

        // Publish event
        GameEvent event = new GameEvent.HandStarted(0, 1);
        eventBus.publish(event);

        // Verify listener received event
        assertEquals(1, listenerReceivedEvents.size());
        assertEquals(event, listenerReceivedEvents.get(0));
    }

    @Test
    void testPublishBroadcastsToCallback() {
        // Set broadcast callback
        eventBus.setBroadcastCallback(broadcastedEvents::add);

        // Publish event
        GameEvent event = new GameEvent.HandStarted(0, 1);
        eventBus.publish(event);

        // Verify callback received event
        assertEquals(1, broadcastedEvents.size());
        assertEquals(event, broadcastedEvents.get(0));
    }

    @Test
    void testPublishWithoutCallbackDoesNotThrow() {
        // No broadcast callback set
        GameEvent event = new GameEvent.HandStarted(0, 1);

        // Should not throw
        assertDoesNotThrow(() -> eventBus.publish(event));

        // Event should still be stored
        assertEquals(1, eventStore.getEvents().size());
    }

    @Test
    void testPublishOrderOfOperations() {
        // Track order of operations
        List<String> operations = new ArrayList<>();

        // Set up event store wrapper that tracks appends
        InMemoryGameEventStore trackingStore = new InMemoryGameEventStore("test") {
            @Override
            public synchronized void append(GameEvent event) {
                operations.add("store");
                super.append(event);
            }
        };

        ServerGameEventBus bus = new ServerGameEventBus(trackingStore);

        // Set up listener
        bus.subscribe(event -> operations.add("listener"));

        // Set up broadcast callback
        bus.setBroadcastCallback(event -> operations.add("broadcast"));

        // Publish event
        GameEvent event = new GameEvent.HandStarted(0, 1);
        bus.publish(event);

        // Verify order: store -> listener -> broadcast
        assertEquals(3, operations.size());
        assertEquals("store", operations.get(0));
        assertEquals("listener", operations.get(1));
        assertEquals("broadcast", operations.get(2));
    }

    @Test
    void testBroadcastTableState() {
        // Create a simple tournament with 2 players
        List<ServerPlayer> players = new ArrayList<>();
        players.add(new ServerPlayer(1, "Player1", true, 0, 1000));
        players.add(new ServerPlayer(2, "Player2", false, 5, 1000));

        ServerTournamentContext tournament = new ServerTournamentContext(players, 1, 1000, new int[]{10}, new int[]{20},
                new int[]{0}, new int[]{10}, new boolean[]{false}, false, 0, 0, false, 30);

        ServerGameTable table = (ServerGameTable) tournament.getTable(0);

        // Set broadcast callback
        eventBus.setBroadcastCallback(broadcastedEvents::add);

        // Set table state
        table.setPendingTableState(TableState.BEGIN);
        table.setTableState(TableState.PENDING);

        // Broadcast table state
        eventBus.broadcastTableState(table);

        // Verify TableStateChanged event was broadcasted
        assertEquals(1, broadcastedEvents.size());
        assertTrue(broadcastedEvents.get(0) instanceof GameEvent.TableStateChanged);

        GameEvent.TableStateChanged stateEvent = (GameEvent.TableStateChanged) broadcastedEvents.get(0);
        assertEquals(0, stateEvent.tableId());
        assertEquals(TableState.PENDING, stateEvent.newState());
    }

    @Test
    void testMultipleEvents() {
        eventBus.setBroadcastCallback(broadcastedEvents::add);

        GameEvent event1 = new GameEvent.HandStarted(0, 1);
        GameEvent event2 = new GameEvent.PlayerActed(0, 1, ActionType.CALL, 100);
        GameEvent event3 = new GameEvent.HandCompleted(0);

        eventBus.publish(event1);
        eventBus.publish(event2);
        eventBus.publish(event3);

        // All events stored
        assertEquals(3, eventStore.getEvents().size());

        // All events broadcasted
        assertEquals(3, broadcastedEvents.size());
        assertEquals(event1, broadcastedEvents.get(0));
        assertEquals(event2, broadcastedEvents.get(1));
        assertEquals(event3, broadcastedEvents.get(2));
    }

    @Test
    void testClearBroadcastCallback() {
        eventBus.setBroadcastCallback(broadcastedEvents::add);

        // Publish event - should broadcast
        eventBus.publish(new GameEvent.HandStarted(0, 1));
        assertEquals(1, broadcastedEvents.size());

        // Clear callback
        eventBus.setBroadcastCallback(null);

        // Publish another event - should not broadcast
        eventBus.publish(new GameEvent.HandStarted(0, 2));
        assertEquals(1, broadcastedEvents.size()); // Still only 1

        // But event should still be stored
        assertEquals(2, eventStore.getEvents().size());
    }

    @Test
    void testListenerExceptionDoesNotStopBroadcast() {
        // Add a failing listener
        eventBus.subscribe(event -> {
            throw new RuntimeException("Listener error");
        });

        // Set broadcast callback
        eventBus.setBroadcastCallback(broadcastedEvents::add);

        // Publish event - listener throws but broadcast should still happen
        GameEvent event = new GameEvent.HandStarted(0, 1);
        assertDoesNotThrow(() -> eventBus.publish(event));

        // Broadcast should have happened despite listener exception
        assertEquals(1, broadcastedEvents.size());
        assertEquals(event, broadcastedEvents.get(0));

        // Event should be stored
        assertEquals(1, eventStore.getEvents().size());
    }

    @Test
    void testNullEventThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            eventBus.publish(null);
        });
    }
}
