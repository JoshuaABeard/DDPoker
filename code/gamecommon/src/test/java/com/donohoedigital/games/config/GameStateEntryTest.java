/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.config;

import com.donohoedigital.base.EscapeStringTokenizer;
import com.donohoedigital.comms.MsgState;
import com.donohoedigital.comms.ObjectID;
import com.donohoedigital.comms.TokenizedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GameStateEntry - token-based serialization entry for game state.
 */
class GameStateEntryTest {

    /**
     * Minimal MsgState subclass that initializes the class-name registry so
     * getClassId() can be called without a NullPointerException.
     */
    private static class TestMsgState extends MsgState {
        TestMsgState() {
            setClassNames(new TokenizedList());
        }
    }

    /**
     * Minimal ObjectID implementation so we can test id-based construction.
     */
    private static class TestObject implements ObjectID {
        private final int id;

        TestObject(int id) {
            this.id = id;
        }

        @Override
        public int getObjectID() {
            return id;
        }
    }

    private TestMsgState state;

    @BeforeEach
    void setUp() {
        state = new TestMsgState();
    }

    // ========== Empty Constructor Tests ==========

    @Test
    void should_ReturnNegativeOne_When_IDNotSet() {
        GameStateEntry entry = new GameStateEntry();

        assertThat(entry.getID()).isEqualTo(-1);
    }

    @Test
    void should_ReturnNullObject_When_DefaultConstructorUsed() {
        GameStateEntry entry = new GameStateEntry();

        assertThat(entry.getObject()).isNull();
    }

    @Test
    void should_ReturnDefaultType_When_DefaultConstructorUsed() {
        GameStateEntry entry = new GameStateEntry();

        // char default is '\0' (zero)
        assertThat(entry.getType()).isEqualTo('\0');
    }

    // ========== Parameterized Constructor Tests ==========

    @Test
    void should_StoreType_When_ConstructedWithNullObject() {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_DATA);

        assertThat(entry.getType()).isEqualTo(ConfigConstants.SAVE_DATA);
    }

    @Test
    void should_ReturnNullObject_When_ConstructedWithNullObject() {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_GAMENAME);

        assertThat(entry.getObject()).isNull();
    }

    @Test
    void should_ReturnNegativeOne_When_ConstructedWithNullObject() {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_DATA);

        assertThat(entry.getID()).isEqualTo(-1);
    }

    @Test
    void should_StoreObjectAndID_When_ConstructedWithObjectID() {
        TestObject obj = new TestObject(42); // arbitrary ID for ObjectID round-trip test
        GameStateEntry entry = new GameStateEntry(state, obj, ConfigConstants.SAVE_PLAYER);

        assertThat(entry.getObject()).isSameAs(obj);
        assertThat(entry.getID()).isEqualTo(42);
        assertThat(entry.getType()).isEqualTo(ConfigConstants.SAVE_PLAYER);
    }

    @Test
    void should_AssignGeneratedID_When_ConstructedWithPlainObject() {
        // A plain Object (not implementing ObjectID) gets a generated id >= 1000
        Object plainObj = new Object();
        GameStateEntry entry = new GameStateEntry(state, plainObj, ConfigConstants.SAVE_TOKEN);

        assertThat(entry.getObject()).isSameAs(plainObj);
        assertThat(entry.getID()).isGreaterThanOrEqualTo(1000);
        assertThat(entry.getType()).isEqualTo(ConfigConstants.SAVE_TOKEN);
    }

    // ========== setType / getType Tests ==========

    @Test
    void should_UpdateType_When_SetTypeCalled() {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_DATA);

        entry.setType(ConfigConstants.SAVE_TERRITORY);

        assertThat(entry.getType()).isEqualTo(ConfigConstants.SAVE_TERRITORY);
    }

    // ========== Token List Inheritance Tests ==========

    @Test
    void should_HaveNoTokens_When_CreatedEmpty() {
        GameStateEntry entry = new GameStateEntry();

        assertThat(entry.hasMoreTokens()).isFalse();
    }

    @Test
    void should_StoreAndRemoveStringToken_When_TokenAdded() {
        GameStateEntry entry = new GameStateEntry();

        entry.addToken("hello");

        assertThat(entry.hasMoreTokens()).isTrue();
        assertThat(entry.removeStringToken()).isEqualTo("hello");
        assertThat(entry.hasMoreTokens()).isFalse();
    }

    @Test
    void should_StoreAndRemoveIntToken_When_IntAdded() {
        GameStateEntry entry = new GameStateEntry();

        entry.addToken(99);

        assertThat(entry.removeIntToken()).isEqualTo(99);
    }

    @Test
    void should_PreserveTokenOrder_When_MultipleTokensAdded() {
        GameStateEntry entry = new GameStateEntry();

        entry.addToken("first");
        entry.addToken("second");
        entry.addToken("third");

        assertThat(entry.removeStringToken()).isEqualTo("first");
        assertThat(entry.removeStringToken()).isEqualTo("second");
        assertThat(entry.removeStringToken()).isEqualTo("third");
    }

    // ========== write() / marshal() Tests ==========

    @Test
    void should_WriteTypeCharFirst_When_WriteCalledWithNullObject() throws Exception {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_DATA);

        StringWriter sw = new StringWriter();
        entry.write(state, sw);
        String result = sw.toString();

        // Format: "<type>:<tokens...>"
        // With null object: type char 'd', then ':', then two null tokens (~:~)
        assertThat(result).startsWith(String.valueOf(ConfigConstants.SAVE_DATA));
        assertThat(result.charAt(1)).isEqualTo(TokenizedList.TOKEN_DELIM);
    }

    @Test
    void should_IncludeTypeCharInMarshalOutput_When_EntryHasType() throws Exception {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_PLAYER);
        // add an extra token to verify full format
        entry.addToken(77);

        StringWriter sw = new StringWriter();
        entry.write(state, sw);
        String result = sw.toString();

        assertThat(result.charAt(0)).isEqualTo(ConfigConstants.SAVE_PLAYER);
    }

    // ========== read() Tests ==========

    @Test
    void should_ParseTypeChar_When_ReadFromTokenizer() {
        GameStateEntry entry = new GameStateEntry();
        // Simulate the wire format produced by write(): "<type>:<tokens>"
        // For an entry with null object the tokens are: "~:~" (two null tokens)
        String wireData = ConfigConstants.SAVE_DATA + ":" + "~" + ":" + "~";
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer(wireData, TokenizedList.TOKEN_DELIM);

        entry.read(state, tokenizer, TokenizedList.TOKEN_READ_ALL);

        assertThat(entry.getType()).isEqualTo(ConfigConstants.SAVE_DATA);
    }

    @Test
    void should_ParseTypeAndTokens_When_ReadFromTokenizerWithExtraTokens() {
        GameStateEntry entry = new GameStateEntry();
        // type char '@', then integer token 7, then string token "world"
        // DataMarshaller encodes int as "i7" and string as "sworld"
        String wireData = ConfigConstants.SAVE_GAMENAME + ":i7:sworld";
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer(wireData, TokenizedList.TOKEN_DELIM);

        entry.read(state, tokenizer, TokenizedList.TOKEN_READ_ALL);

        assertThat(entry.getType()).isEqualTo(ConfigConstants.SAVE_GAMENAME);
        assertThat(entry.removeIntToken()).isEqualTo(7);
        assertThat(entry.removeStringToken()).isEqualTo("world");
    }
}
