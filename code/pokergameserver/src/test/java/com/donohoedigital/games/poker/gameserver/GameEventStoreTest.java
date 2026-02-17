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
import com.donohoedigital.games.poker.core.state.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class InMemoryGameEventStoreTest {

    private InMemoryGameEventStore eventStore;
    private static final String TEST_GAME_ID = "test-game-123";

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryGameEventStore(TEST_GAME_ID);
    }

    @Test
    void testConstructor() {
        assertEquals(TEST_GAME_ID, eventStore.getGameId());
        assertTrue(eventStore.getEvents().isEmpty());
    }

    @Test
    void testAppendSingleEvent() {
        GameEvent event = new GameEvent.HandStarted(0, 1);
        eventStore.append(event);

        List<StoredEvent> events = eventStore.getEvents();
        assertEquals(1, events.size());

        StoredEvent stored = events.get(0);
        assertEquals(TEST_GAME_ID, stored.gameId());
        assertEquals(1L, stored.sequenceNumber());
        assertEquals("HandStarted", stored.eventType());
        assertEquals(event, stored.event());
        assertNotNull(stored.timestamp());
    }

    @Test
    void testAppendMultipleEvents() {
        GameEvent event1 = new GameEvent.HandStarted(0, 1);
        GameEvent event2 = new GameEvent.PlayerActed(0, 1, ActionType.CALL, 100);
        GameEvent event3 = new GameEvent.HandCompleted(0);

        eventStore.append(event1);
        eventStore.append(event2);
        eventStore.append(event3);

        List<StoredEvent> events = eventStore.getEvents();
        assertEquals(3, events.size());

        // Verify sequence numbers are monotonic
        assertEquals(1L, events.get(0).sequenceNumber());
        assertEquals(2L, events.get(1).sequenceNumber());
        assertEquals(3L, events.get(2).sequenceNumber());

        // Verify events stored correctly
        assertEquals(event1, events.get(0).event());
        assertEquals(event2, events.get(1).event());
        assertEquals(event3, events.get(2).event());
    }

    @Test
    void testGetEventsSince() {
        // Append several events
        for (int i = 1; i <= 5; i++) {
            eventStore.append(new GameEvent.HandStarted(0, i));
        }

        // Get events after sequence 2
        List<StoredEvent> recent = eventStore.getEventsSince(2L);
        assertEquals(3, recent.size());
        assertEquals(3L, recent.get(0).sequenceNumber());
        assertEquals(4L, recent.get(1).sequenceNumber());
        assertEquals(5L, recent.get(2).sequenceNumber());
    }

    @Test
    void testGetEventsSinceReturnsEmpty() {
        eventStore.append(new GameEvent.HandStarted(0, 1));
        eventStore.append(new GameEvent.HandStarted(0, 2));

        // Request events after last sequence
        List<StoredEvent> recent = eventStore.getEventsSince(2L);
        assertTrue(recent.isEmpty());
    }

    @Test
    void testGetEventsSinceReturnsAll() {
        eventStore.append(new GameEvent.HandStarted(0, 1));
        eventStore.append(new GameEvent.HandStarted(0, 2));
        eventStore.append(new GameEvent.HandStarted(0, 3));

        // Request all events (after sequence 0)
        List<StoredEvent> all = eventStore.getEventsSince(0L);
        assertEquals(3, all.size());
    }

    @Test
    void testEventsAreUnmodifiable() {
        eventStore.append(new GameEvent.HandStarted(0, 1));

        List<StoredEvent> events = eventStore.getEvents();

        // Should throw when trying to modify
        assertThrows(UnsupportedOperationException.class, () -> {
            events.add(new StoredEvent(TEST_GAME_ID, 999L, "HandStarted", new GameEvent.HandStarted(0, 999),
                    java.time.Instant.now()));
        });
    }

    @Test
    void testSequenceNumbersAreUnique() {
        // Rapidly append events and verify no duplicates
        for (int i = 0; i < 100; i++) {
            eventStore.append(new GameEvent.HandStarted(0, i));
        }

        List<StoredEvent> events = eventStore.getEvents();
        assertEquals(100, events.size());

        // Verify all sequence numbers are unique and sequential
        for (int i = 0; i < 100; i++) {
            assertEquals(i + 1L, events.get(i).sequenceNumber());
        }
    }

    @Test
    void testEventTypeExtraction() {
        eventStore.append(new GameEvent.HandStarted(0, 1));
        eventStore.append(new GameEvent.PlayerActed(0, 1, ActionType.FOLD, 0));
        eventStore.append(new GameEvent.HandCompleted(0));

        List<StoredEvent> events = eventStore.getEvents();
        assertEquals("HandStarted", events.get(0).eventType());
        assertEquals("PlayerActed", events.get(1).eventType());
        assertEquals("HandCompleted", events.get(2).eventType());
    }

    @Test
    void testTimestampIsRecorded() {
        java.time.Instant before = java.time.Instant.now();
        eventStore.append(new GameEvent.HandStarted(0, 1));
        java.time.Instant after = java.time.Instant.now();

        StoredEvent stored = eventStore.getEvents().get(0);
        assertNotNull(stored.timestamp());
        assertFalse(stored.timestamp().isBefore(before));
        assertFalse(stored.timestamp().isAfter(after));
    }

    @Test
    void testNullEventThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            eventStore.append(null);
        });
    }

    @Test
    void testClear() {
        eventStore.append(new GameEvent.HandStarted(0, 1));
        eventStore.append(new GameEvent.HandStarted(0, 2));
        assertEquals(2, eventStore.getEvents().size());

        eventStore.clear();
        assertTrue(eventStore.getEvents().isEmpty());

        // After clear, sequence should restart
        eventStore.append(new GameEvent.HandStarted(0, 3));
        assertEquals(1L, eventStore.getEvents().get(0).sequenceNumber());
    }
}
