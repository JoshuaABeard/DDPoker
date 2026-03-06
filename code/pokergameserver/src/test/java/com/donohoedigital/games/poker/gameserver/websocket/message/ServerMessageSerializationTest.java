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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.protocol.message.ClientMessage;
import com.donohoedigital.games.poker.protocol.message.ClientMessageData;
import com.donohoedigital.games.poker.protocol.message.ClientMessageType;
import com.donohoedigital.games.poker.protocol.message.ServerMessage;
import com.donohoedigital.games.poker.protocol.message.ServerMessageData;
import com.donohoedigital.games.poker.protocol.message.ServerMessageType;
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
                new ServerMessageData.ConnectedData(42L, null, null));

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
    void showdownStartedData_includesShowdownPlayers() throws Exception {
        // ShowdownStartedData now carries showdown player cards so the client
        // can reveal hole cards before displayShowdown() runs.
        var showdownPlayers = java.util.List
                .of(new ServerMessageData.ShowdownPlayerData(7L, java.util.List.of("Ah", "Kd"), "", null));
        ServerMessageData.ShowdownStartedData data = new ServerMessageData.ShowdownStartedData(1, showdownPlayers);
        ServerMessage message = ServerMessage.of(ServerMessageType.SHOWDOWN_STARTED, "game-xyz", data);

        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("SHOWDOWN_STARTED", node.get("type").asText());
        JsonNode dataNode = node.get("data");
        assertNotNull(dataNode);
        assertEquals(1, dataNode.get("tableId").asInt());
        JsonNode players = dataNode.get("showdownPlayers");
        assertNotNull(players, "showdownPlayers must be present");
        assertEquals(1, players.size());
        assertEquals(7L, players.get(0).get("playerId").asLong());
        assertEquals("Ah", players.get(0).get("cards").get(0).asText());
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

    @Test
    void lobbyStateData_serializesPlayerList() throws Exception {
        ServerMessageData.LobbyPlayerData p1 = new ServerMessageData.LobbyPlayerData(10L, "Alice", true, false, null);
        ServerMessageData.LobbyPlayerData p2 = new ServerMessageData.LobbyPlayerData(11L, "Bot", false, true, "MEDIUM");
        ServerMessageData.LobbyStateData lobby = new ServerMessageData.LobbyStateData("game-abc", "Friday Game",
                "SERVER", "Alice", 10L, 6, false, List.of(p1, p2), null);

        ServerMessage message = ServerMessage.of(ServerMessageType.LOBBY_STATE, "game-abc", lobby);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("LOBBY_STATE", node.get("type").asText());
        JsonNode data = node.get("data");
        assertEquals("game-abc", data.get("gameId").asText());
        assertEquals(2, data.get("players").size());
        assertTrue(data.get("players").get(0).get("isOwner").asBoolean());
    }

    @Test
    void chipsTransferredData_serializesAllFields() throws Exception {
        ServerMessageData.ChipsTransferredData data = new ServerMessageData.ChipsTransferredData(1L, "Alice", 2L, "Bob",
                500, 4500, 1500);

        ServerMessage message = ServerMessage.of(ServerMessageType.CHIPS_TRANSFERRED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(1L, node.get("fromPlayerId").asLong());
        assertEquals("Alice", node.get("fromPlayerName").asText());
        assertEquals(500, node.get("amount").asInt());
    }

    @Test
    void winnerData_serializesCorrectly() throws Exception {
        ServerMessageData.WinnerData winner = new ServerMessageData.WinnerData(7L, 1200, "Pair of Aces",
                List.of("Ah", "As"), 0, 6200, null);

        ServerMessage message = ServerMessage.of(ServerMessageType.HAND_COMPLETE, "g",
                new ServerMessageData.HandCompleteData(5, List.of(winner), List.of(), 1));
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        JsonNode winners = node.get("winners");
        assertEquals(1, winners.size());
        assertEquals(7L, winners.get(0).get("playerId").asLong());
        assertEquals("Pair of Aces", winners.get(0).get("hand").asText());
    }

    @Test
    void colorUpStartedData_serializesPlayerList() throws Exception {
        ServerMessageData.ColorUpPlayerData cupPlayer = new ServerMessageData.ColorUpPlayerData(3L, List.of("5c", "5h"),
                true, false, 1000);
        ServerMessageData.ColorUpStartedData data = new ServerMessageData.ColorUpStartedData(List.of(cupPlayer), 25, 1);

        ServerMessage message = ServerMessage.of(ServerMessageType.COLOR_UP_STARTED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(25, node.get("newMinChip").asInt());
        assertEquals(1, node.get("players").size());
    }

    @Test
    void gamePausedData_serializesBreakFields() throws Exception {
        ServerMessageData.GamePausedData data = new ServerMessageData.GamePausedData("Break", "system", true, 15);

        ServerMessage message = ServerMessage.of(ServerMessageType.GAME_PAUSED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals("Break", node.get("reason").asText());
        assertTrue(node.get("isBreak").asBoolean());
        assertEquals(15, node.get("breakDurationMinutes").asInt());
    }

    @Test
    void playerAddonData_serializesFields() throws Exception {
        ServerMessageData.PlayerAddonData data = new ServerMessageData.PlayerAddonData(5L, "Dave", 3000, 8000);

        ServerMessage message = ServerMessage.of(ServerMessageType.PLAYER_ADDON, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(5L, node.get("playerId").asLong());
        assertEquals(3000, node.get("addedChips").asInt());
        assertEquals(8000, node.get("chipCount").asInt());
    }

    @Test
    void standingData_serializesPositionAndPrize() throws Exception {
        ServerMessageData.StandingData s = new ServerMessageData.StandingData(1, 42L, "Alice", 5000);
        ServerMessageData.GameCompleteData data = new ServerMessageData.GameCompleteData(List.of(s), 45, 3600L);

        ServerMessage message = ServerMessage.of(ServerMessageType.GAME_COMPLETE, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode standings = objectMapper.readTree(json).get("data").get("standings");

        assertEquals(1, standings.size());
        assertEquals(1, standings.get(0).get("position").asInt());
        assertEquals(5000, standings.get(0).get("prize").asInt());
    }

    @Test
    void playerRebuyData_serializesFields() throws Exception {
        ServerMessageData.PlayerRebuyData data = new ServerMessageData.PlayerRebuyData(9L, "Eve", 5000, 5000);

        ServerMessage message = ServerMessage.of(ServerMessageType.PLAYER_REBUY, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(9L, node.get("playerId").asLong());
        assertEquals(5000, node.get("addedChips").asInt());
    }

    @Test
    void observerJoinedData_serializesFields() throws Exception {
        ServerMessageData.ObserverJoinedData data = new ServerMessageData.ObserverJoinedData(99L, "Watcher", 1);

        ServerMessage message = ServerMessage.of(ServerMessageType.OBSERVER_JOINED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(99L, node.get("observerId").asLong());
        assertEquals("Watcher", node.get("observerName").asText());
    }

    @Test
    void observerLeftData_serializesFields() throws Exception {
        ServerMessageData.ObserverLeftData data = new ServerMessageData.ObserverLeftData(99L, "Watcher", 1);

        ServerMessage message = ServerMessage.of(ServerMessageType.OBSERVER_LEFT, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(99L, node.get("observerId").asLong());
    }

    @Test
    void tableStateChangedData_serializesStateTransition() throws Exception {
        ServerMessageData.TableStateChangedData data = new ServerMessageData.TableStateChangedData(1, "IDLE",
                "PLAYING");

        ServerMessage message = ServerMessage.of(ServerMessageType.TABLE_STATE_CHANGED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals("IDLE", node.get("oldState").asText());
        assertEquals("PLAYING", node.get("newState").asText());
    }

    @Test
    void currentPlayerChangedData_serializesFields() throws Exception {
        ServerMessageData.CurrentPlayerChangedData data = new ServerMessageData.CurrentPlayerChangedData(1, 7L, "Bob");

        ServerMessage message = ServerMessage.of(ServerMessageType.CURRENT_PLAYER_CHANGED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(7L, node.get("playerId").asLong());
        assertEquals("Bob", node.get("playerName").asText());
    }

    @Test
    void buttonMovedData_serializesNewSeat() throws Exception {
        ServerMessageData.ButtonMovedData data = new ServerMessageData.ButtonMovedData(1, 3);

        ServerMessage message = ServerMessage.of(ServerMessageType.BUTTON_MOVED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(3, node.get("newSeat").asInt());
    }

    @Test
    void playerSatOutData_serializesFields() throws Exception {
        ServerMessageData.PlayerSatOutData data = new ServerMessageData.PlayerSatOutData(4L, "Charlie");

        ServerMessage message = ServerMessage.of(ServerMessageType.PLAYER_SAT_OUT, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(4L, node.get("playerId").asLong());
        assertEquals("Charlie", node.get("playerName").asText());
    }

    @Test
    void playerCameBackData_serializesFields() throws Exception {
        ServerMessageData.PlayerCameBackData data = new ServerMessageData.PlayerCameBackData(4L, "Charlie");

        ServerMessage message = ServerMessage.of(ServerMessageType.PLAYER_CAME_BACK, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(4L, node.get("playerId").asLong());
    }

    @Test
    void timerUpdateData_serializesSecondsRemaining() throws Exception {
        ServerMessageData.TimerUpdateData data = new ServerMessageData.TimerUpdateData(6L, 20);

        ServerMessage message = ServerMessage.of(ServerMessageType.TIMER_UPDATE, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(6L, node.get("playerId").asLong());
        assertEquals(20, node.get("secondsRemaining").asInt());
    }

    @Test
    void aiHoleCardsData_serializesPlayerCards() throws Exception {
        ServerMessageData.AiPlayerCards aiCards = new ServerMessageData.AiPlayerCards(2L, List.of("Kh", "Qh"));
        ServerMessageData.AiHoleCardsData data = new ServerMessageData.AiHoleCardsData(List.of(aiCards));

        ServerMessage message = ServerMessage.of(ServerMessageType.AI_HOLE_CARDS, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        JsonNode players = node.get("players");
        assertEquals(1, players.size());
        assertEquals(2L, players.get(0).get("playerId").asLong());
    }

    @Test
    void gameResumedData_serializesResumedBy() throws Exception {
        ServerMessageData.GameResumedData data = new ServerMessageData.GameResumedData("admin");

        ServerMessage message = ServerMessage.of(ServerMessageType.GAME_RESUMED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals("admin", node.get("resumedBy").asText());
    }

    @Test
    void neverBrokeOfferedData_serializesTimeout() throws Exception {
        ServerMessageData.NeverBrokeOfferedData data = new ServerMessageData.NeverBrokeOfferedData(30);

        ServerMessage message = ServerMessage.of(ServerMessageType.NEVER_BROKE_OFFERED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(30, node.get("timeoutSeconds").asInt());
    }

    @Test
    void gameCancelledData_serializesReason() throws Exception {
        ServerMessageData.GameCancelledData data = new ServerMessageData.GameCancelledData("Owner left");

        ServerMessage message = ServerMessage.of(ServerMessageType.GAME_CANCELLED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals("Owner left", node.get("reason").asText());
    }

    @Test
    void lobbyPlayerKickedData_serializesPlayer() throws Exception {
        ServerMessageData.LobbyPlayerData kicked = new ServerMessageData.LobbyPlayerData(15L, "Bad Actor", false, false,
                null);
        ServerMessageData.LobbyPlayerKickedData data = new ServerMessageData.LobbyPlayerKickedData(kicked);

        ServerMessage message = ServerMessage.of(ServerMessageType.LOBBY_PLAYER_KICKED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data").get("player");

        assertEquals(15L, node.get("profileId").asLong());
        assertEquals("Bad Actor", node.get("name").asText());
    }

    @Test
    void clientMessage_record_holdsTypeAndData() {
        ClientMessageData.PlayerActionData actionData = new ClientMessageData.PlayerActionData("FOLD", 0);
        ClientMessage msg = new ClientMessage(ClientMessageType.PLAYER_ACTION, 42L, actionData);

        assertEquals(ClientMessageType.PLAYER_ACTION, msg.type());
        assertEquals(42L, msg.sequenceNumber());
        assertEquals(actionData, msg.data());
    }

    @Test
    void clientMessageData_rebuyDecision_holdsAccept() {
        ClientMessageData.RebuyDecisionData accept = new ClientMessageData.RebuyDecisionData(true);
        ClientMessageData.RebuyDecisionData decline = new ClientMessageData.RebuyDecisionData(false);

        assertTrue(accept.accept());
        assertTrue(!decline.accept());
    }

    @Test
    void clientMessageData_addonDecision_holdsAccept() {
        ClientMessageData.AddonDecisionData data = new ClientMessageData.AddonDecisionData(true);
        assertTrue(data.accept());
    }

    @Test
    void clientMessageData_neverBrokeDecision_holdsAccept() {
        ClientMessageData.NeverBrokeDecisionData data = new ClientMessageData.NeverBrokeDecisionData(false);
        assertTrue(!data.accept());
    }

    @Test
    void clientMessageData_noDataRecords_instantiate() {
        ClientMessageData.SitOutData sitOut = new ClientMessageData.SitOutData();
        ClientMessageData.ComeBackData comeBack = new ClientMessageData.ComeBackData();
        ClientMessageData.AdminPauseData pause = new ClientMessageData.AdminPauseData();
        ClientMessageData.AdminResumeData resume = new ClientMessageData.AdminResumeData();

        assertNotNull(sitOut);
        assertNotNull(comeBack);
        assertNotNull(pause);
        assertNotNull(resume);
    }

    @Test
    void blindPostedData_serializesFields() throws Exception {
        ServerMessageData.BlindPostedData data = new ServerMessageData.BlindPostedData(3L, 50, "BIG_BLIND");

        ServerMessage message = ServerMessage.of(ServerMessageType.HAND_STARTED, "g",
                new ServerMessageData.HandStartedData(1, 0, 1, 2, List.of(data)));
        String json = objectMapper.writeValueAsString(message);
        JsonNode blindsPosted = objectMapper.readTree(json).get("data").get("blindsPosted");

        assertEquals(1, blindsPosted.size());
        assertEquals(3L, blindsPosted.get(0).get("playerId").asLong());
        assertEquals(50, blindsPosted.get(0).get("amount").asInt());
        assertEquals("BIG_BLIND", blindsPosted.get(0).get("type").asText());
    }

    @Test
    void rebuyOfferedData_serializesFields() throws Exception {
        ServerMessageData.RebuyOfferedData data = new ServerMessageData.RebuyOfferedData(500, 5000, 30);

        ServerMessage message = ServerMessage.of(ServerMessageType.REBUY_OFFERED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(500, node.get("cost").asInt());
        assertEquals(5000, node.get("chips").asInt());
        assertEquals(30, node.get("timeoutSeconds").asInt());
    }

    @Test
    void addonOfferedData_serializesFields() throws Exception {
        ServerMessageData.AddonOfferedData data = new ServerMessageData.AddonOfferedData(1000, 3000, 30);

        ServerMessage message = ServerMessage.of(ServerMessageType.ADDON_OFFERED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json).get("data");

        assertEquals(1000, node.get("cost").asInt());
        assertEquals(3000, node.get("chips").asInt());
    }

    @Test
    void lobbySettingsChangedData_serializesWithNullSettings() throws Exception {
        // LobbySettingsChangedData wraps a GameSummary; null is acceptable for tests
        // that just verify the message type serializes without error.
        ServerMessageData.LobbySettingsChangedData data = new ServerMessageData.LobbySettingsChangedData(null);

        ServerMessage message = ServerMessage.of(ServerMessageType.LOBBY_SETTINGS_CHANGED, "g", data);
        String json = objectMapper.writeValueAsString(message);
        JsonNode node = objectMapper.readTree(json);

        assertEquals("LOBBY_SETTINGS_CHANGED", node.get("type").asText());
    }
}
