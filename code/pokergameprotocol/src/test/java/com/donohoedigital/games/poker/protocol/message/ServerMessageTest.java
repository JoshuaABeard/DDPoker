/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
package com.donohoedigital.games.poker.protocol.message;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ServerMessageTest {

    @Test
    void of_createsMessageWithCurrentTimestamp() {
        ServerMessage msg = ServerMessage.of(ServerMessageType.GAME_STATE, "game-1", "payload");

        assertEquals(ServerMessageType.GAME_STATE, msg.type());
        assertEquals("game-1", msg.gameId());
        assertEquals("payload", msg.data());
        assertNotNull(msg.timestamp());
        assertNull(msg.sequenceNumber());
    }

    @Test
    void backwardCompatibleConstructor_nullSequenceNumber() {
        Instant ts = Instant.parse("2026-01-01T00:00:00Z");
        ServerMessage msg = new ServerMessage(ServerMessageType.CONNECTED, "game-2", ts, "data");

        assertNull(msg.sequenceNumber());
        assertEquals(ts, msg.timestamp());
    }

    @Test
    void fullConstructor_includesSequenceNumber() {
        Instant ts = Instant.now();
        ServerMessage msg = new ServerMessage(ServerMessageType.HAND_STARTED, "game-3", ts, "data", 42L);

        assertEquals(42L, msg.sequenceNumber());
    }

    @Test
    void withSequence_stampsSequenceNumber() {
        ServerMessage original = ServerMessage.of(ServerMessageType.PLAYER_ACTED, "game-4", "act");
        ServerMessage stamped = original.withSequence(99);

        assertEquals(99L, stamped.sequenceNumber());
        assertEquals(original.type(), stamped.type());
        assertEquals(original.gameId(), stamped.gameId());
        assertEquals(original.data(), stamped.data());
        assertEquals(original.timestamp(), stamped.timestamp());
    }

    @Test
    void withSequence_doesNotMutateOriginal() {
        ServerMessage original = ServerMessage.of(ServerMessageType.ERROR, "game-5", "err");
        original.withSequence(10);

        assertNull(original.sequenceNumber());
    }

    @Test
    void equality_sameFieldsAreEqual() {
        Instant ts = Instant.parse("2026-06-01T12:00:00Z");
        ServerMessage msg1 = new ServerMessage(ServerMessageType.CHAT_MESSAGE, "g", ts, "hi", 1L);
        ServerMessage msg2 = new ServerMessage(ServerMessageType.CHAT_MESSAGE, "g", ts, "hi", 1L);

        assertEquals(msg1, msg2);
        assertEquals(msg1.hashCode(), msg2.hashCode());
    }
}
