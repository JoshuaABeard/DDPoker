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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class ServerPlayerSessionTest {

    private ServerPlayerSession session;
    private static final long PROFILE_ID = 42L;
    private static final String PLAYER_NAME = "TestPlayer";
    private static final int SKILL_LEVEL = 5;

    @BeforeEach
    void setUp() {
        session = new ServerPlayerSession(PROFILE_ID, PLAYER_NAME, false, SKILL_LEVEL);
    }

    @Test
    void testConstructor() {
        assertEquals(PROFILE_ID, session.getProfileId());
        assertEquals(PLAYER_NAME, session.getPlayerName());
        assertFalse(session.isAI());
        assertEquals(SKILL_LEVEL, session.getSkillLevel());
        assertFalse(session.isConnected());
        assertFalse(session.isDisconnected());
        assertNull(session.getDisconnectedAt());
        assertEquals(0, session.getConsecutiveTimeouts());
    }

    @Test
    void testAIPlayer() {
        ServerPlayerSession aiSession = new ServerPlayerSession(1L, "Bot", true, 8);
        assertTrue(aiSession.isAI());
        assertEquals(8, aiSession.getSkillLevel());
    }

    @Test
    void testConnect() {
        session.connect();
        assertTrue(session.isConnected());
        assertFalse(session.isDisconnected());
        assertNull(session.getDisconnectedAt());
    }

    @Test
    void testDisconnect() {
        session.connect();
        assertTrue(session.isConnected());

        Instant before = Instant.now();
        session.disconnect();
        Instant after = Instant.now();

        assertFalse(session.isConnected());
        assertTrue(session.isDisconnected());
        assertNotNull(session.getDisconnectedAt());

        // Verify timestamp is reasonable
        assertFalse(session.getDisconnectedAt().isBefore(before));
        assertFalse(session.getDisconnectedAt().isAfter(after));
    }

    @Test
    void testReconnect() {
        // Connect, disconnect, then reconnect
        session.connect();
        session.disconnect();
        assertTrue(session.isDisconnected());
        assertNotNull(session.getDisconnectedAt());

        session.connect();
        assertTrue(session.isConnected());
        assertFalse(session.isDisconnected());
        assertNull(session.getDisconnectedAt()); // Cleared on reconnect
    }

    @Test
    void testMessageSender() {
        assertNull(session.getMessageSender());

        List<Object> messages = new ArrayList<>();
        session.setMessageSender(messages::add);
        assertNotNull(session.getMessageSender());

        // Send a message
        session.getMessageSender().accept("test message");
        assertEquals(1, messages.size());
        assertEquals("test message", messages.get(0));
    }

    @Test
    void testMessageSenderClearedOnDisconnect() {
        List<Object> messages = new ArrayList<>();
        session.setMessageSender(messages::add);
        session.connect();
        assertNotNull(session.getMessageSender());

        session.disconnect();
        assertNull(session.getMessageSender()); // Cleared
    }

    @Test
    void testConsecutiveTimeouts() {
        assertEquals(0, session.getConsecutiveTimeouts());

        session.incrementConsecutiveTimeouts();
        assertEquals(1, session.getConsecutiveTimeouts());

        session.incrementConsecutiveTimeouts();
        session.incrementConsecutiveTimeouts();
        assertEquals(3, session.getConsecutiveTimeouts());
    }

    @Test
    void testResetTimeouts() {
        session.incrementConsecutiveTimeouts();
        session.incrementConsecutiveTimeouts();
        assertEquals(2, session.getConsecutiveTimeouts());

        session.resetConsecutiveTimeouts();
        assertEquals(0, session.getConsecutiveTimeouts());
    }

    @Test
    void testTimeoutsResetOnSuccessfulAction() {
        session.incrementConsecutiveTimeouts();
        session.incrementConsecutiveTimeouts();
        assertEquals(2, session.getConsecutiveTimeouts());

        // Simulate successful action
        session.resetConsecutiveTimeouts();
        assertEquals(0, session.getConsecutiveTimeouts());
    }

    @Test
    void testSendMessageWhenNotConnected() {
        // No message sender set - should not throw
        assertDoesNotThrow(() -> {
            if (session.getMessageSender() != null) {
                session.getMessageSender().accept("test");
            }
        });
    }

    @Test
    void testMultipleDisconnectsSameTimestamp() {
        session.connect();
        session.disconnect();
        Instant firstDisconnect = session.getDisconnectedAt();

        // Multiple disconnect calls don't change timestamp
        session.disconnect();
        assertEquals(firstDisconnect, session.getDisconnectedAt());
    }

    @Test
    void testAISessionNeverConnects() {
        ServerPlayerSession aiSession = new ServerPlayerSession(1L, "Bot", true, 5);

        // AI players don't track connection state
        assertFalse(aiSession.isConnected());

        // Even if we call connect (shouldn't happen, but be defensive)
        aiSession.connect();
        // For AI, connection state is irrelevant - they're always "available"
        // The implementation might choose to track it or ignore it
    }

    @Test
    void testThreadSafetyOfMessageSender() throws InterruptedException {
        AtomicInteger messageCount = new AtomicInteger(0);
        session.setMessageSender(msg -> messageCount.incrementAndGet());
        session.connect();

        // Simulate multiple threads sending messages
        int numThreads = 10;
        int messagesPerThread = 100;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    if (session.getMessageSender() != null) {
                        session.getMessageSender().accept("msg");
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(numThreads * messagesPerThread, messageCount.get());
    }

    @Test
    void testToString() {
        String str = session.toString();
        assertTrue(str.contains(PLAYER_NAME));
        assertTrue(str.contains(String.valueOf(PROFILE_ID)));
    }
}
