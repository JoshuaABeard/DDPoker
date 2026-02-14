/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.core.event;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.state.ActionType;

/** Tests for {@link GameEventBus}. */
class GameEventBusTest {

    private GameEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new GameEventBus();
    }

    @Test
    void subscribe_shouldAddListener() {
        GameEventListener listener = event -> {
        };

        eventBus.subscribe(listener);

        assertThat(eventBus.getListenerCount()).isEqualTo(1);
    }

    @Test
    void subscribe_shouldRejectNullListener() {
        assertThatThrownBy(() -> eventBus.subscribe(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Listener cannot be null");
    }

    @Test
    void unsubscribe_shouldRemoveListener() {
        GameEventListener listener = event -> {
        };
        eventBus.subscribe(listener);

        boolean removed = eventBus.unsubscribe(listener);

        assertThat(removed).isTrue();
        assertThat(eventBus.getListenerCount()).isEqualTo(0);
    }

    @Test
    void unsubscribe_shouldReturnFalseIfNotSubscribed() {
        GameEventListener listener = event -> {
        };

        boolean removed = eventBus.unsubscribe(listener);

        assertThat(removed).isFalse();
    }

    @Test
    void publish_shouldDeliverEventToAllListeners() {
        List<GameEvent> received1 = new ArrayList<>();
        List<GameEvent> received2 = new ArrayList<>();

        eventBus.subscribe(received1::add);
        eventBus.subscribe(received2::add);

        GameEvent event = new GameEvent.HandStarted(1, 10);
        eventBus.publish(event);

        assertThat(received1).containsExactly(event);
        assertThat(received2).containsExactly(event);
    }

    @Test
    void publish_shouldRejectNullEvent() {
        assertThatThrownBy(() -> eventBus.publish(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event cannot be null");
    }

    @Test
    void publish_shouldNotPropagateListenerExceptions() {
        List<GameEvent> received = new ArrayList<>();

        // First listener throws exception
        eventBus.subscribe(event -> {
            throw new RuntimeException("Listener error");
        });

        // Second listener should still receive event
        eventBus.subscribe(received::add);

        GameEvent event = new GameEvent.HandStarted(1, 10);
        eventBus.publish(event);

        assertThat(received).containsExactly(event);
    }

    @Test
    void publish_shouldHandleMultipleEventTypes() {
        List<GameEvent> received = new ArrayList<>();
        eventBus.subscribe(received::add);

        GameEvent event1 = new GameEvent.HandStarted(1, 10);
        GameEvent event2 = new GameEvent.PlayerActed(1, 5, ActionType.CALL, 100);
        GameEvent event3 = new GameEvent.HandCompleted(1);

        eventBus.publish(event1);
        eventBus.publish(event2);
        eventBus.publish(event3);

        assertThat(received).containsExactly(event1, event2, event3);
    }

    @Test
    void clear_shouldRemoveAllListeners() {
        eventBus.subscribe(event -> {
        });
        eventBus.subscribe(event -> {
        });
        eventBus.subscribe(event -> {
        });

        eventBus.clear();

        assertThat(eventBus.getListenerCount()).isEqualTo(0);
    }

    @Test
    void concurrentPublish_shouldBeThreadSafe() throws InterruptedException {
        AtomicInteger eventCount = new AtomicInteger(0);
        eventBus.subscribe(event -> eventCount.incrementAndGet());

        int threadCount = 10;
        int eventsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        eventBus.publish(new GameEvent.HandStarted(1, j));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(eventCount.get()).isEqualTo(threadCount * eventsPerThread);
    }

    @Test
    void concurrentSubscribeUnsubscribe_shouldBeThreadSafe() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    GameEventListener listener = event -> {
                    };
                    eventBus.subscribe(listener);
                    eventBus.unsubscribe(listener);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // All listeners should be unsubscribed
        assertThat(eventBus.getListenerCount()).isEqualTo(0);
    }
}
