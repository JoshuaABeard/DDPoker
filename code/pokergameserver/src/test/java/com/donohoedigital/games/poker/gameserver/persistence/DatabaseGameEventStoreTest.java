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
package com.donohoedigital.games.poker.gameserver.persistence;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.gameserver.StoredEvent;
import com.donohoedigital.games.poker.gameserver.persistence.repository.GameEventRepository;

/**
 * Tests for {@link DatabaseGameEventStore}.
 */
@DataJpaTest
@ContextConfiguration(classes = TestJpaConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseGameEventStoreTest {

    @Autowired
    private GameEventRepository repository;

    @Test
    void testAppendAndRetrieve() {
        DatabaseGameEventStore eventStore = new DatabaseGameEventStore("game-1", repository);

        GameEvent event = new GameEvent.HandStarted(0, 1);
        eventStore.append(event);

        List<StoredEvent> events = eventStore.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).gameId()).isEqualTo("game-1");
        assertThat(events.get(0).sequenceNumber()).isEqualTo(1L);
        assertThat(events.get(0).eventType()).isEqualTo("HandStarted");
    }

    @Test
    void testAppendMultipleEvents() {
        DatabaseGameEventStore eventStore = new DatabaseGameEventStore("game-1", repository);

        eventStore.append(new GameEvent.HandStarted(0, 1));
        eventStore.append(new GameEvent.PlayerActed(0, 1, ActionType.CALL, 100));
        eventStore.append(new GameEvent.HandCompleted(0));

        List<StoredEvent> events = eventStore.getEvents();
        assertThat(events).hasSize(3);
        assertThat(events).extracting(StoredEvent::sequenceNumber).containsExactly(1L, 2L, 3L);
    }

    @Test
    void testGetEventsSince() {
        DatabaseGameEventStore eventStore = new DatabaseGameEventStore("game-1", repository);

        for (int i = 1; i <= 5; i++) {
            eventStore.append(new GameEvent.HandStarted(0, i));
        }

        List<StoredEvent> recent = eventStore.getEventsSince(2L);
        assertThat(recent).hasSize(3);
        assertThat(recent).extracting(StoredEvent::sequenceNumber).containsExactly(3L, 4L, 5L);
    }

    @Test
    void testGetCurrentSequenceNumber() {
        DatabaseGameEventStore eventStore = new DatabaseGameEventStore("game-1", repository);

        assertThat(eventStore.getCurrentSequenceNumber()).isEqualTo(0L);

        eventStore.append(new GameEvent.HandStarted(0, 1));
        assertThat(eventStore.getCurrentSequenceNumber()).isEqualTo(1L);

        eventStore.append(new GameEvent.HandStarted(0, 2));
        assertThat(eventStore.getCurrentSequenceNumber()).isEqualTo(2L);
    }

    @Test
    void testClear() {
        DatabaseGameEventStore eventStore = new DatabaseGameEventStore("game-1", repository);

        eventStore.append(new GameEvent.HandStarted(0, 1));
        eventStore.append(new GameEvent.HandStarted(0, 2));
        assertThat(eventStore.getEvents()).hasSize(2);

        eventStore.clear();
        assertThat(eventStore.getEvents()).isEmpty();
        assertThat(eventStore.getCurrentSequenceNumber()).isEqualTo(0L);
    }

    @Test
    void testMultipleGamesIndependent() {
        DatabaseGameEventStore store1 = new DatabaseGameEventStore("game-1", repository);
        DatabaseGameEventStore store2 = new DatabaseGameEventStore("game-2", repository);

        store1.append(new GameEvent.HandStarted(0, 1));
        store1.append(new GameEvent.HandStarted(0, 2));

        store2.append(new GameEvent.HandStarted(0, 1));

        assertThat(store1.getEvents()).hasSize(2);
        assertThat(store2.getEvents()).hasSize(1);
        assertThat(store1.getCurrentSequenceNumber()).isEqualTo(2L);
        assertThat(store2.getCurrentSequenceNumber()).isEqualTo(1L);
    }

    @Test
    void testReloadFromDatabase() {
        // First store appends events
        DatabaseGameEventStore store1 = new DatabaseGameEventStore("game-1", repository);
        store1.append(new GameEvent.HandStarted(0, 1));
        store1.append(new GameEvent.HandStarted(0, 2));
        store1.append(new GameEvent.HandStarted(0, 3));

        // Second store for same game should see same events
        DatabaseGameEventStore store2 = new DatabaseGameEventStore("game-1", repository);
        assertThat(store2.getEvents()).hasSize(3);
        assertThat(store2.getCurrentSequenceNumber()).isEqualTo(3L);

        // Appending to second store continues from sequence
        store2.append(new GameEvent.HandStarted(0, 4));
        assertThat(store2.getCurrentSequenceNumber()).isEqualTo(4L);

        // First store should see new event too
        assertThat(store1.getEvents()).hasSize(4);
    }
}
