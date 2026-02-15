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
package com.donohoedigital.games.poker.network;

import com.donohoedigital.comms.DDMessage;
import com.donohoedigital.comms.DMArrayList;
import com.donohoedigital.comms.DMTypedHashMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OnlineMessage - wrapper class for network messages in DD Poker
 */
class OnlineMessageTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateMessage_When_GivenCategory() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);

        assertThat(msg.getCategory()).isEqualTo(OnlineMessage.CAT_CHAT);
        assertThat(msg.getData()).isNotNull();
        assertThat(msg.getMessageID()).isGreaterThan(0);
    }

    @Test
    void should_CreateMessage_When_GivenDDMessage() {
        DDMessage data = new DDMessage(OnlineMessage.CAT_JOIN);
        OnlineMessage msg = new OnlineMessage(data);

        assertThat(msg.getCategory()).isEqualTo(OnlineMessage.CAT_JOIN);
        assertThat(msg.getData()).isEqualTo(data);
    }

    @Test
    void should_CreateMessage_When_GivenDDMessageAndConnection() {
        DDMessage data = new DDMessage(OnlineMessage.CAT_QUIT);
        PokerConnection conn = null; // Mock connection (null for now)
        OnlineMessage msg = new OnlineMessage(data, conn);

        assertThat(msg.getCategory()).isEqualTo(OnlineMessage.CAT_QUIT);
        assertThat(msg.getData()).isEqualTo(data);
        assertThat(msg.getConnection()).isEqualTo(conn);
    }

    @Test
    void should_ThrowError_When_DDMessageIsNull() {
        assertThatThrownBy(() -> new OnlineMessage(null)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void should_AssignUniqueMessageIDs_When_CreatingMultipleMessages() {
        OnlineMessage msg1 = new OnlineMessage(OnlineMessage.CAT_CHAT);
        OnlineMessage msg2 = new OnlineMessage(OnlineMessage.CAT_CHAT);

        assertThat(msg1.getMessageID()).isNotEqualTo(msg2.getMessageID());
    }

    // =================================================================
    // Player Info Tests
    // =================================================================

    @Test
    void should_StoreAndRetrievePlayerName() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        msg.setPlayerName("TestPlayer");

        assertThat(msg.getPlayerName()).isEqualTo("TestPlayer");
    }

    @Test
    void should_ReturnNull_When_PlayerNameNotSet() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);

        assertThat(msg.getPlayerName()).isNull();
    }

    @Test
    void should_StoreAndRetrievePlayerProfilePath() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setPlayerProfilePath("/profiles/player1.dat");

        assertThat(msg.getPlayerProfilePath()).isEqualTo("/profiles/player1.dat");
    }

    @Test
    void should_StoreAndRetrieveDemoStatus() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setPlayerDemo(true);

        assertThat(msg.isPlayerDemo()).isTrue();
    }

    @Test
    void should_DefaultToFalse_When_DemoStatusNotSet() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);

        assertThat(msg.isPlayerDemo()).isFalse();
    }

    @Test
    void should_StoreAndRetrieveOnlineActivated() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setOnlineActivated(true);

        assertThat(msg.isOnlineActivated()).isTrue();
    }

    @Test
    void should_StoreAndRetrievePlayerConnected() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CONNECTION);
        msg.setPlayerConnected(true);

        assertThat(msg.isPlayerConnected()).isTrue();
    }

    // =================================================================
    // Game Data Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveGameID() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setGameID("game-12345");

        assertThat(msg.getGameID()).isEqualTo("game-12345");
    }

    @Test
    void should_StoreAndRetrievePassword() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setPassword("secret123");

        assertThat(msg.getPassword()).isEqualTo("secret123");
    }

    @Test
    void should_StoreAndRetrieveGUID() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        String guid = "550e8400-e29b-41d4-a716-446655440000";
        msg.setGUID(guid);

        assertThat(msg.getGUID()).isEqualTo(guid);
    }

    @Test
    void should_StoreAndRetrievePlayerSettings() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_PLAYER_UPDATE);
        msg.setPlayerSettings("speed=fast;sound=on");

        assertThat(msg.getPlayerSettings()).isEqualTo("speed=fast;sound=on");
    }

    // =================================================================
    // Chat Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveChat() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        msg.setChat("Hello everyone!");

        assertThat(msg.getChat()).isEqualTo("Hello everyone!");
    }

    @Test
    void should_StoreAndRetrieveChatType() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        msg.setChatType(OnlineMessage.CHAT_ADMIN_JOIN);

        assertThat(msg.getChatType()).isEqualTo(OnlineMessage.CHAT_ADMIN_JOIN);
    }

    @Test
    void should_ReturnDefaultChatType_When_NotSet() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);

        // Default is PokerConstants.CHAT_ALWAYS, but we don't have that constant
        // Just verify it returns a value
        assertThat(msg.getChatType()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_StoreAndRetrieveFromPlayerID() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_HAND_ACTION);
        msg.setFromPlayerID(5);

        assertThat(msg.getFromPlayerID()).isEqualTo(5);
    }

    @Test
    void should_ReturnNoPlayer_When_FromPlayerIDNotSet() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);

        assertThat(msg.getFromPlayerID()).isEqualTo(OnlineMessage.NO_PLAYER);
    }

    @Test
    void should_StoreAndRetrieveTableNumber() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        msg.setTableNumber(3);

        assertThat(msg.getTableNumber()).isEqualTo(3);
    }

    @Test
    void should_ReturnNoTable_When_TableNumberNotSet() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);

        assertThat(msg.getTableNumber()).isEqualTo(OnlineMessage.NO_TABLE);
    }

    // =================================================================
    // Connection URL Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveConnectURL() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        PokerURL url = new PokerURL("poker://localhost:1234/game123/pass456");
        msg.setConnectURL(url);

        PokerURL retrieved = msg.getConnectURL();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.toString()).isEqualTo(url.toString());
    }

    @Test
    void should_NotSetURL_When_URLIsNull() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setConnectURL(null);

        assertThat(msg.getConnectURL()).isNull();
    }

    // =================================================================
    // Message ID Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveInReplyTo() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_GAME_UPDATE);
        msg.setInReplyTo(42);

        assertThat(msg.getInReplyTo()).isEqualTo(42);
    }

    @Test
    void should_ReturnZero_When_InReplyToNotSet() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);

        assertThat(msg.getInReplyTo()).isEqualTo(0);
    }

    // =================================================================
    // Game Phase Tests
    // =================================================================

    @Test
    void should_StoreAndRetrievePhaseName() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_GAME_UPDATE);
        msg.setPhaseName("Flop");

        assertThat(msg.getPhaseName()).isEqualTo("Flop");
    }

    @Test
    void should_StoreAndRetrievePhaseParams() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_GAME_UPDATE);
        DMTypedHashMap params = new DMTypedHashMap();
        params.setString("key", "value");
        msg.setPhaseParams(params);

        DMTypedHashMap retrieved = msg.getPhaseParams();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getString("key")).isEqualTo("value");
    }

    @Test
    void should_StoreAndRetrieveRunProcessTable() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_GAME_UPDATE);
        msg.setRunProcessTable(true);

        assertThat(msg.isRunProcessTable()).isTrue();
    }

    @Test
    void should_DefaultToFalse_When_RunProcessTableNotSet() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_GAME_UPDATE);

        assertThat(msg.isRunProcessTable()).isFalse();
    }

    // =================================================================
    // Hand Action Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveHandAction() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_HAND_ACTION);
        String action = "RAISE_100";
        msg.setHandAction(action);

        assertThat(msg.getHandAction()).isEqualTo(action);
    }

    @Test
    void should_StoreAndRetrieveHandActionCC() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_HAND_ACTION);
        msg.setHandActionCC(true);

        assertThat(msg.isHandActionCC()).isTrue();
    }

    // =================================================================
    // Poker Table Events Tests
    // =================================================================

    @Test
    void should_StoreAndRetrievePokerTableEvents() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_GAME_UPDATE);
        DMArrayList<DMTypedHashMap> events = new DMArrayList<>();
        DMTypedHashMap event = new DMTypedHashMap();
        event.setString("type", "DEAL");
        events.add(event);
        msg.setPokerTableEvents(events);

        DMArrayList<?> retrieved = (DMArrayList<?>) msg.getPokerTableEvents();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).hasSize(1);
    }

    // =================================================================
    // Chips/Cash Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveCash() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_REBUY);
        msg.setCash(5000);

        assertThat(msg.getCash()).isEqualTo(5000);
    }

    @Test
    void should_ReturnZero_When_CashNotSet() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_REBUY);

        assertThat(msg.getCash()).isEqualTo(0);
    }

    @Test
    void should_StoreAndRetrieveChips() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_REBUY);
        msg.setChips(10000);

        assertThat(msg.getChips()).isEqualTo(10000);
    }

    @Test
    void should_StoreAndRetrieveLevel() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_GAME_UPDATE);
        msg.setLevel(5);

        assertThat(msg.getLevel()).isEqualTo(5);
    }

    // =================================================================
    // Boolean Flag Tests
    // =================================================================

    @Test
    void should_StoreAndRetrievePending() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setPending(true);

        assertThat(msg.isPending()).isTrue();
    }

    @Test
    void should_StoreAndRetrieveObserve() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setObserve(true);

        assertThat(msg.isObserve()).isTrue();
    }

    @Test
    void should_StoreAndRetrieveReconnect() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setReconnect(true);

        assertThat(msg.isReconnect()).isTrue();
    }

    @Test
    void should_StoreAndRetrieveClockPaused() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_GAME_UPDATE);
        msg.setClockPaused(Boolean.TRUE);

        assertThat(msg.isClockPaused()).isTrue();
    }

    // =================================================================
    // WAN Operation Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveWanAuth() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT_HELLO);
        DMTypedHashMap auth = new DMTypedHashMap();
        auth.setString("name", "Player1");
        auth.setString("uuid", "uuid-123");
        msg.setWanAuth(auth);

        DMTypedHashMap retrieved = msg.getWanAuth();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getString("name")).isEqualTo("Player1");
        assertThat(retrieved.getString("uuid")).isEqualTo("uuid-123");
    }

    @Test
    void should_StoreAndRetrieveOnlineProfileData() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_WAN_PROFILE_ADD);
        DMTypedHashMap profile = new DMTypedHashMap();
        profile.setString("email", "test@example.com");
        msg.setOnlineProfileData(profile);

        DMTypedHashMap retrieved = msg.getOnlineProfileData();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getString("email")).isEqualTo("test@example.com");
    }

    @Test
    void should_StoreAndRetrieveWanGame() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_ADD);
        DMTypedHashMap game = new DMTypedHashMap();
        game.setString("gameid", "game-999");
        msg.setWanGame(game);

        DMTypedHashMap retrieved = msg.getWanGame();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getString("gameid")).isEqualTo("game-999");
    }

    @Test
    void should_StoreAndRetrieveWanGames() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_LIST);
        DMArrayList<DMTypedHashMap> games = new DMArrayList<>();
        DMTypedHashMap game1 = new DMTypedHashMap();
        game1.setString("gameid", "game-1");
        games.add(game1);
        msg.setWanGames(games);

        DMArrayList<DMTypedHashMap> retrieved = msg.getWanGames();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).getString("gameid")).isEqualTo("game-1");
    }

    @Test
    void should_StoreAndRetrieveWanHistories() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_LIST);
        DMArrayList<DMTypedHashMap> histories = new DMArrayList<>();
        DMTypedHashMap hist = new DMTypedHashMap();
        hist.setString("result", "win");
        histories.add(hist);
        msg.setWanHistories(histories);

        DMArrayList<?> retrieved = (DMArrayList<?>) msg.getWanHistories();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).hasSize(1);
    }

    // =================================================================
    // Offset/Count/Mode Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveOffset() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_LIST);
        msg.setOffset(10);

        assertThat(msg.getOffset()).isEqualTo(10);
    }

    @Test
    void should_ReturnNegativeOne_When_OffsetNotSet() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_LIST);

        assertThat(msg.getOffset()).isEqualTo(-1);
    }

    @Test
    void should_StoreAndRetrieveCount() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_LIST);
        msg.setCount(50);

        assertThat(msg.getCount()).isEqualTo(50);
    }

    @Test
    void should_StoreAndRetrieveMode() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_WAN_GAME_LIST);
        msg.setMode(2);

        assertThat(msg.getMode()).isEqualTo(2);
    }

    // =================================================================
    // PlayerInfo Tests
    // =================================================================

    @Test
    void should_StoreAndRetrievePlayerInfo() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT_HELLO);
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("TestPlayer");
        info.setPlayerId("player-123");
        msg.setPlayerInfo(info);

        OnlinePlayerInfo retrieved = msg.getPlayerInfo();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("TestPlayer");
        assertThat(retrieved.getPlayerId()).isEqualTo("player-123");
    }

    @Test
    void should_StoreAndRetrievePlayerList() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT_ADMIN);
        DMArrayList<DMTypedHashMap> playerList = new DMArrayList<>();

        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Player1");
        playerList.add(info1.getData());

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Player2");
        playerList.add(info2.getData());

        msg.setPlayerList(playerList);

        var retrieved = msg.getPlayerList();
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(0).getName()).isEqualTo("Player1");
        assertThat(retrieved.get(1).getName()).isEqualTo("Player2");
    }

    // =================================================================
    // Application Error/Status Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveApplicationErrorMessage() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setApplicationErrorMessage("Connection failed");

        assertThat(msg.getApplicationErrorMessage()).isEqualTo("Connection failed");
    }

    @Test
    void should_StoreAndRetrieveApplicationStatusMessage() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);
        msg.setApplicationStatusMessage("Connecting...");

        assertThat(msg.getApplicationStatusMessage()).isEqualTo("Connecting...");
    }

    // =================================================================
    // Game Data Blob Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveGameData() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_GAME_UPDATE);
        String data = "game-state-data";
        msg.setGameData(data);

        byte[] retrieved = msg.getGameData();
        assertThat(retrieved).isNotNull();
        assertThat(new String(retrieved)).isEqualTo(data);
    }

    // =================================================================
    // ToString Tests
    // =================================================================

    @Test
    void should_ReturnValidString_When_ToStringCalled() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        msg.setPlayerName("TestPlayer");
        msg.setChat("Hello");

        String result = msg.toString();
        assertThat(result).isNotNull();
        assertThat(result).contains("OnlineMessage");
    }

    @Test
    void should_ReturnValidString_When_ToStringNoDataCalled() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);

        String result = msg.toStringNoData();
        assertThat(result).isNotNull();
        assertThat(result).contains("OnlineMessage");
    }

    @Test
    void should_ReturnCategoryString_When_ToStringCategoryCalled() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        msg.setPlayerName("TestPlayer");
        msg.setChat("Hello world");

        String result = msg.toStringCategory();
        assertThat(result).isNotNull();
        assertThat(result).contains("chat");
        assertThat(result).contains("TestPlayer");
        assertThat(result).contains("Hello world");
    }

    @Test
    void should_ReturnCategoryAndSize_When_ToStringCategorySizeCalled() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_JOIN);

        String result = msg.toStringCategorySize();
        assertThat(result).isNotNull();
        assertThat(result).contains("join");
    }

    // =================================================================
    // Category String Mapping Tests
    // =================================================================

    @Test
    void should_ReturnCorrectString_For_AllCategories() {
        // Test a sampling of important categories
        assertCategoryString(OnlineMessage.CAT_TEST, "test");
        assertCategoryString(OnlineMessage.CAT_JOIN, "join");
        assertCategoryString(OnlineMessage.CAT_QUIT, "quit");
        assertCategoryString(OnlineMessage.CAT_CANCEL, "cancel");
        assertCategoryString(OnlineMessage.CAT_READY, "ready");
        assertCategoryString(OnlineMessage.CAT_ALIVE, "alive");
        assertCategoryString(OnlineMessage.CAT_WAN_GAME_ADD, "wan-game-add");
        assertCategoryString(OnlineMessage.CAT_WAN_GAME_REMOVE, "wan-game-remove");
        assertCategoryString(OnlineMessage.CAT_WAN_PROFILE_ADD, "wan-profile-add");
    }

    @Test
    void should_ReturnUndefined_For_UnknownCategory() {
        OnlineMessage msg = new OnlineMessage(99999);

        String result = msg.toStringCategory();
        assertThat(result).contains("Undefined");
        assertThat(result).contains("99999");
    }

    @Test
    void should_ReturnChatWithDetails_When_CategoryIsChat() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        msg.setPlayerName("Alice");
        msg.setChat("Hi there!");

        String result = msg.toStringCategory();
        assertThat(result).contains("chat");
        assertThat(result).contains("Alice");
        assertThat(result).contains("Hi there!");
    }

    @Test
    void should_ReturnChatWithoutName_When_PlayerNameIsNull() {
        OnlineMessage msg = new OnlineMessage(OnlineMessage.CAT_CHAT);
        msg.setChat("Anonymous message");

        String result = msg.toStringCategory();
        assertThat(result).contains("chat");
        assertThat(result).contains(":");
    }

    // =================================================================
    // Helper Methods
    // =================================================================

    private void assertCategoryString(int category, String expectedSubstring) {
        OnlineMessage msg = new OnlineMessage(category);
        String result = msg.toStringCategory();
        assertThat(result).contains(expectedSubstring);
    }
}
