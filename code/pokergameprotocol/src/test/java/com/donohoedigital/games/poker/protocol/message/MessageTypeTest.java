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

import org.junit.jupiter.api.Test;

class MessageTypeTest {

    @Test
    void serverMessageType_hasExpectedCount() {
        // 51 server message types as of current implementation
        assertTrue(ServerMessageType.values().length >= 40,
                "Expected at least 40 server message types, got: " + ServerMessageType.values().length);
    }

    @Test
    void clientMessageType_hasExpectedCount() {
        assertEquals(12, ClientMessageType.values().length);
    }

    @Test
    void serverMessageType_valueOf_roundTrip() {
        for (ServerMessageType type : ServerMessageType.values()) {
            assertEquals(type, ServerMessageType.valueOf(type.name()));
        }
    }

    @Test
    void clientMessageType_valueOf_roundTrip() {
        for (ClientMessageType type : ClientMessageType.values()) {
            assertEquals(type, ClientMessageType.valueOf(type.name()));
        }
    }

    @Test
    void serverMessageType_containsKeyTypes() {
        assertNotNull(ServerMessageType.valueOf("CONNECTED"));
        assertNotNull(ServerMessageType.valueOf("GAME_STATE"));
        assertNotNull(ServerMessageType.valueOf("HAND_STARTED"));
        assertNotNull(ServerMessageType.valueOf("ACTION_REQUIRED"));
        assertNotNull(ServerMessageType.valueOf("PLAYER_ACTED"));
        assertNotNull(ServerMessageType.valueOf("HAND_COMPLETE"));
        assertNotNull(ServerMessageType.valueOf("GAME_COMPLETE"));
        assertNotNull(ServerMessageType.valueOf("ERROR"));
        assertNotNull(ServerMessageType.valueOf("LOBBY_STATE"));
    }

    @Test
    void clientMessageType_containsKeyTypes() {
        assertNotNull(ClientMessageType.valueOf("PLAYER_ACTION"));
        assertNotNull(ClientMessageType.valueOf("CHAT"));
        assertNotNull(ClientMessageType.valueOf("SIT_OUT"));
        assertNotNull(ClientMessageType.valueOf("COME_BACK"));
        assertNotNull(ClientMessageType.valueOf("REQUEST_STATE"));
    }

    @Test
    void clientMessage_recordFields() {
        ClientMessage msg = new ClientMessage(ClientMessageType.PLAYER_ACTION, 5, "data");
        assertEquals(ClientMessageType.PLAYER_ACTION, msg.type());
        assertEquals(5, msg.sequenceNumber());
        assertEquals("data", msg.data());
    }
}
