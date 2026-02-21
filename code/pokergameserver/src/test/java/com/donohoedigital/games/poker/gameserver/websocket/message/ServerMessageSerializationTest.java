/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver.websocket.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Tests JSON serialization of {@link ServerMessage} with various payload types.
 */
class ServerMessageSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void connectedMessage_serializesTypeCorrectly() throws Exception {
        ServerMessage message = ServerMessage.of(ServerMessageType.CONNECTED, "game-123",
                new ServerMessageData.ConnectedData(42L, null));

        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("CONNECTED", node.get("type").asText());
        assertEquals("game-123", node.get("gameId").asText());
    }

    @Test
    void errorMessage_hasCodeAndMessageFields() throws Exception {
        ServerMessage message = ServerMessage.of(ServerMessageType.ERROR, "game-456",
                new ServerMessageData.ErrorData("INVALID_ACTION", "Cannot fold out of turn"));

        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("ERROR", node.get("type").asText());
        JsonNode data = node.get("data");
        assertNotNull(data, "data field must be present");
        assertEquals("INVALID_ACTION", data.get("code").asText());
        assertEquals("Cannot fold out of turn", data.get("message").asText());
    }

    @Test
    void playerActedMessage_hasRequiredFields() throws Exception {
        ServerMessageData.PlayerActedData data = new ServerMessageData.PlayerActedData(7L, "Bob", "CALL", 100, 100, 900,
                300, 1);
        ServerMessage message = ServerMessage.of(ServerMessageType.PLAYER_ACTED, "game-789", data);

        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("PLAYER_ACTED", node.get("type").asText());
        JsonNode dataNode = node.get("data");
        assertNotNull(dataNode);
        assertEquals(7L, dataNode.get("playerId").asLong());
        assertEquals("Bob", dataNode.get("playerName").asText());
        assertEquals("CALL", dataNode.get("action").asText());
        assertEquals(100, dataNode.get("amount").asInt());
    }

    @Test
    void timestampIsIncludedInAllMessages() throws Exception {
        ServerMessage msg1 = ServerMessage.of(ServerMessageType.CONNECTED, "g1", null);
        ServerMessage msg2 = ServerMessage.of(ServerMessageType.ERROR, "g2",
                new ServerMessageData.ErrorData("ERR", "msg"));
        ServerMessage msg3 = ServerMessage.of(ServerMessageType.PLAYER_ACTED, "g3",
                new ServerMessageData.PlayerActedData(1L, "Alice", "FOLD", 0, 0, 1000, 200, 1));

        for (ServerMessage msg : new ServerMessage[]{msg1, msg2, msg3}) {
            String json = objectMapper.writeValueAsString(msg);
            JsonNode node = objectMapper.readTree(json);
            assertNotNull(node.get("timestamp"), "timestamp must be present in message: " + node.get("type").asText());
        }
    }

    @Test
    void nullData_serializesWithoutError() throws Exception {
        ServerMessage message = ServerMessage.of(ServerMessageType.GAME_RESUMED, "game-001", null);

        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("GAME_RESUMED", node.get("type").asText());
        // data should be null or absent — no exception should be thrown
        assertTrue(node.get("data") == null || node.get("data").isNull(), "null data should serialize as null");
    }

    @Test
    void chatMessageData_fieldsRoundtrip() throws Exception {
        ServerMessageData.ChatMessageData data = new ServerMessageData.ChatMessageData(3L, "Carol",
                "Good game everyone!", true);
        ServerMessage message = ServerMessage.of(ServerMessageType.CHAT_MESSAGE, "game-chat", data);

        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json);

        JsonNode dataNode = node.get("data");
        assertEquals(3L, dataNode.get("playerId").asLong());
        assertEquals("Carol", dataNode.get("playerName").asText());
        assertEquals("Good game everyone!", dataNode.get("message").asText());
        assertTrue(dataNode.get("tableChat").asBoolean());
    }
}
