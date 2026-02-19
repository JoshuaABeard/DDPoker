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
package com.donohoedigital.games.comms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for EngineMessage - Game messaging protocol.
 */
class EngineMessageTest {

    @TempDir
    Path tempDir;

    // ========== Constructor Tests ==========

    @Test
    void should_CreateEmptyMessage_When_DefaultConstructorUsed() {
        EngineMessage msg = new EngineMessage();

        assertThat(msg).isNotNull();
    }

    @Test
    void should_InitializeBasicFields_When_BasicConstructorUsed() {
        EngineMessage msg = new EngineMessage("game123", 5, EngineMessage.CAT_POLL_UPDATES);

        assertThat(msg.getGameID()).isEqualTo("game123");
        assertThat(msg.getFromPlayerID()).isEqualTo(5);
        assertThat(msg.getCategory()).isEqualTo(EngineMessage.CAT_POLL_UPDATES);
    }

    @Test
    void should_InitializeWithStringData_When_StringConstructorUsed() {
        EngineMessage msg = new EngineMessage("game456", 3, EngineMessage.CAT_CHAT, "Hello World");

        assertThat(msg.getGameID()).isEqualTo("game456");
        assertThat(msg.getFromPlayerID()).isEqualTo(3);
        assertThat(msg.getCategory()).isEqualTo(EngineMessage.CAT_CHAT);
        assertThat(msg.getDataAsString()).isEqualTo("Hello World");
    }

    @Test
    void should_InitializeWithByteData_When_ByteArrayConstructorUsed() {
        byte[] data = {1, 2, 3, 4};
        EngineMessage msg = new EngineMessage("game789", 7, EngineMessage.CAT_ACTION_DONE, data);

        assertThat(msg.getGameID()).isEqualTo("game789");
        assertThat(msg.getFromPlayerID()).isEqualTo(7);
        assertThat(msg.getCategory()).isEqualTo(EngineMessage.CAT_ACTION_DONE);
    }

    @Test
    void should_InitializeWithFileData_When_FileConstructorUsed() throws IOException {
        File testFile = tempDir.resolve("test.dat").toFile();
        Files.writeString(testFile.toPath(), "file content");

        EngineMessage msg = new EngineMessage("gameABC", 2, EngineMessage.CAT_GAME_DATA, testFile);

        assertThat(msg.getGameID()).isEqualTo("gameABC");
        assertThat(msg.getFromPlayerID()).isEqualTo(2);
        assertThat(msg.getCategory()).isEqualTo(EngineMessage.CAT_GAME_DATA);
    }

    @Test
    void should_InitializeWithFileArray_When_FileArrayConstructorUsed() throws IOException {
        File file1 = tempDir.resolve("file1.dat").toFile();
        File file2 = tempDir.resolve("file2.dat").toFile();
        Files.writeString(file1.toPath(), "content1");
        Files.writeString(file2.toPath(), "content2");
        File[] files = {file1, file2};

        EngineMessage msg = new EngineMessage("gameDEF", 4, EngineMessage.CAT_COMPOSITE_MESSAGE, files);

        assertThat(msg.getGameID()).isEqualTo("gameDEF");
        assertThat(msg.getFromPlayerID()).isEqualTo(4);
        assertThat(msg.getCategory()).isEqualTo(EngineMessage.CAT_COMPOSITE_MESSAGE);
    }

    // ========== Game/Player ID Tests ==========

    @Test
    void should_ReturnGameID_When_GameIDSet() {
        EngineMessage msg = new EngineMessage();

        msg.setGameID("myGame");

        assertThat(msg.getGameID()).isEqualTo("myGame");
    }

    @Test
    void should_ReturnNotDefined_When_GameIDNull() {
        EngineMessage msg = new EngineMessage();

        msg.setGameID(null);

        assertThat(msg.getGameID()).isEqualTo(EngineMessage.GAME_NOTDEFINED);
    }

    @Test
    void should_ReturnPlayerID_When_PlayerIDSet() {
        EngineMessage msg = new EngineMessage();

        msg.setFromPlayerID(42);

        assertThat(msg.getFromPlayerID()).isEqualTo(42);
    }

    @Test
    void should_ReturnNotDefined_When_PlayerIDNotSet() {
        EngineMessage msg = new EngineMessage();

        assertThat(msg.getFromPlayerID()).isEqualTo(EngineMessage.PLAYER_NOTDEFINED);
    }

    @Test
    void should_HandleSpecialPlayerIDs_When_Set() {
        EngineMessage msg1 = new EngineMessage();
        msg1.setFromPlayerID(EngineMessage.PLAYER_SERVER);
        assertThat(msg1.getFromPlayerID()).isEqualTo(EngineMessage.PLAYER_SERVER);

        EngineMessage msg2 = new EngineMessage();
        msg2.setFromPlayerID(EngineMessage.PLAYER_GROUP);
        assertThat(msg2.getFromPlayerID()).isEqualTo(EngineMessage.PLAYER_GROUP);
    }

    // ========== Sequence ID Tests ==========

    @Test
    void should_ReturnMinusOne_When_SeqIDNotSet() {
        EngineMessage msg = new EngineMessage();

        assertThat(msg.getSeqID()).isEqualTo(-1);
    }

    @Test
    void should_ReturnSeqID_When_SeqIDSet() {
        EngineMessage msg = new EngineMessage();

        msg.setSeqID(12345L);

        assertThat(msg.getSeqID()).isEqualTo(12345L);
    }

    // ========== Category Debug Tests ==========

    @Test
    void should_ReturnServerQuery_When_CategoryIsServerQuery() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_SERVER_QUERY);

        assertThat(msg.getDebugCat()).isEqualTo("server query");
    }

    @Test
    void should_ReturnNewGame_When_CategoryIsNewGame() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_NEW_GAME);

        assertThat(msg.getDebugCat()).isEqualTo("new game");
    }

    @Test
    void should_ReturnJoinGame_When_CategoryIsJoinGame() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_JOIN_GAME);

        assertThat(msg.getDebugCat()).isEqualTo("join game");
    }

    @Test
    void should_ReturnPoll_When_CategoryIsPollUpdates() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_POLL_UPDATES);

        assertThat(msg.getDebugCat()).isEqualTo("poll");
    }

    @Test
    void should_ReturnChat_When_CategoryIsChat() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_CHAT);

        assertThat(msg.getDebugCat()).isEqualTo("chat");
    }

    @Test
    void should_ReturnComposite_When_CategoryIsCompositeMessage() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_COMPOSITE_MESSAGE);

        assertThat(msg.getDebugCat()).isEqualTo("composite");
    }

    @Test
    void should_ReturnOk_When_CategoryIsOk() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_OK);

        assertThat(msg.getDebugCat()).isEqualTo("ok");
    }

    @Test
    void should_ReturnEmpty_When_CategoryIsEmpty() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_EMPTY);

        assertThat(msg.getDebugCat()).isEqualTo("empty");
    }

    @Test
    void should_ReturnBadEmail_When_CategoryIsErrorBadEmail() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_ERROR_BAD_EMAIL);

        assertThat(msg.getDebugCat()).isEqualTo("bad email");
    }

    @Test
    void should_ReturnCategoryNumber_When_CategoryUnknown() {
        EngineMessage msg = new EngineMessage("game", 1, 9999);

        assertThat(msg.getDebugCat()).isEqualTo("(9999)");
    }

    // ========== Debug Info Tests ==========

    @Test
    void should_ReturnShortInfo_When_GetDebugInfoShortCalled() {
        EngineMessage msg = new EngineMessage("game", 1, EngineMessage.CAT_CHAT);

        String info = msg.getDebugInfoShort();

        assertThat(info).contains("chat");
    }

    @Test
    void should_ReturnStandardInfo_When_GetDebugInfoCalled() {
        EngineMessage msg = new EngineMessage("game", 5, EngineMessage.CAT_POLL_UPDATES);

        String info = msg.getDebugInfo();

        assertThat(info).contains("poll");
        assertThat(info).contains("player 5");
    }

    @Test
    void should_ReturnLongInfo_When_GetDebugInfoLongCalled() {
        EngineMessage msg = new EngineMessage("game", 3, EngineMessage.CAT_CHAT, "Hello");

        String info = msg.getDebugInfoLong();

        assertThat(info).contains("chat");
        assertThat(info).contains("player 3");
        assertThat(info).contains("Hello");
    }

    @Test
    void should_ShowServerInDebug_When_FromPlayerIsServer() {
        EngineMessage msg = new EngineMessage("game", EngineMessage.PLAYER_SERVER, EngineMessage.CAT_OK);

        String info = msg.getDebugInfo();

        assertThat(info).contains("(server)");
    }

    @Test
    void should_ShowGroupInDebug_When_FromPlayerIsGroup() {
        EngineMessage msg = new EngineMessage("game", EngineMessage.PLAYER_GROUP, EngineMessage.CAT_GAME_UPDATE);

        String info = msg.getDebugInfo();

        assertThat(info).contains("(group)");
    }

    @Test
    void should_ShowNotDefinedInDebug_When_FromPlayerNotDefined() {
        EngineMessage msg = new EngineMessage();
        msg.setGameID("game");
        msg.setCategory(EngineMessage.CAT_INFO);

        String info = msg.getDebugInfo();

        assertThat(info).contains("(not defined)");
    }

    // ========== Category Constants Tests ==========

    @Test
    void should_HaveCorrectValues_When_CategoryConstantsAccessed() {
        assertThat(EngineMessage.CAT_SERVER_QUERY).isEqualTo(0);
        assertThat(EngineMessage.CAT_NEW_GAME).isEqualTo(1);
        assertThat(EngineMessage.CAT_JOIN_GAME).isEqualTo(2);
        assertThat(EngineMessage.CAT_POLL_UPDATES).isEqualTo(3);
        assertThat(EngineMessage.CAT_CHAT).isEqualTo(6);
        assertThat(EngineMessage.CAT_COMPOSITE_MESSAGE).isEqualTo(100);
        assertThat(EngineMessage.CAT_OK).isEqualTo(102);
        assertThat(EngineMessage.CAT_EMPTY).isEqualTo(104);
        assertThat(EngineMessage.CAT_ERROR_BAD_EMAIL).isEqualTo(1000);
    }

    @Test
    void should_HaveCorrectValues_When_PlayerConstantsAccessed() {
        assertThat(EngineMessage.PLAYER_NOTDEFINED).isEqualTo(-1);
        assertThat(EngineMessage.PLAYER_SERVER).isEqualTo(-2);
        assertThat(EngineMessage.PLAYER_GROUP).isEqualTo(-3);
    }
}
