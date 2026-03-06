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
package com.donohoedigital.games.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GamePiece - abstract game piece with token management and player
 * association.
 */
class GamePieceTest {

    // ========== Constructor Tests ==========

    @Test
    void should_HaveNullFields_When_DefaultConstructorUsed() {
        TestPiece piece = new TestPiece();

        assertThat(piece.getName()).isNull();
        assertThat(piece.getType()).isNull();
        assertThat(piece.getGamePlayer()).isNull();
        assertThat(piece.getNumTokens()).isZero();
    }

    @Test
    void should_InitializeWithTypePlayerAndName_When_ParameterizedConstructorUsed() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPiece piece = new TestPiece(5, player, "Infantry");

        assertThat(piece.getType()).isEqualTo(5);
        assertThat(piece.getGamePlayer()).isSameAs(player);
        assertThat(piece.getName()).isEqualTo("Infantry");
        // Parameterized constructor adds one token
        assertThat(piece.getNumTokens()).isEqualTo(1);
    }

    // ========== Token Management Tests ==========

    @Test
    void should_AddToken_When_AddTokenCalled() {
        TestPiece piece = new TestPiece();
        Token token = new Token();

        piece.addToken(token);

        assertThat(piece.getNumTokens()).isEqualTo(1);
        assertThat(piece.getToken(0)).isSameAs(token);
        assertThat(token.getGamePiece()).isSameAs(piece);
    }

    @Test
    void should_RemoveToken_When_RemoveTokenCalled() {
        TestPiece piece = new TestPiece();
        Token token = new Token();
        piece.addToken(token);

        piece.removeToken(token);

        assertThat(piece.getNumTokens()).isZero();
        assertThat(token.getGamePiece()).isNull();
    }

    @Test
    void should_ClearAllTokens_When_ClearTokensCalled() {
        TestPiece piece = new TestPiece();
        piece.addToken(new Token());
        piece.addToken(new Token());

        piece.clearTokens();

        assertThat(piece.getNumTokens()).isZero();
    }

    @Test
    void should_AddMultipleTokens_When_AddNewTokensCalled() {
        TestPiece piece = new TestPiece();

        piece.addNewTokens(3);

        assertThat(piece.getNumTokens()).isEqualTo(3);
        // All should be non-hidden by default
        for (int i = 0; i < 3; i++) {
            assertThat(piece.getToken(i).isHidden()).isFalse();
        }
    }

    @Test
    void should_AddHiddenTokens_When_AddNewTokensCalledWithHidden() {
        TestPiece piece = new TestPiece();

        piece.addNewTokens(2, true);

        assertThat(piece.getNumTokens()).isEqualTo(2);
        assertThat(piece.getToken(0).isHidden()).isTrue();
        assertThat(piece.getToken(1).isHidden()).isTrue();
    }

    // ========== Name Tests ==========

    @Test
    void should_UpdateName_When_SetNameCalled() {
        TestPiece piece = new TestPiece();

        piece.setName("Cavalry");

        assertThat(piece.getName()).isEqualTo("Cavalry");
    }

    // ========== Type Tests ==========

    @Test
    void should_UpdateType_When_SetTypeCalled() {
        TestPiece piece = new TestPiece();

        piece.setType(42);

        assertThat(piece.getType()).isEqualTo(42);
    }

    // ========== Player Association Tests ==========

    @Test
    void should_SetPlayer_When_SetGamePlayerCalled() {
        TestPiece piece = new TestPiece();
        GamePlayer player = new GamePlayer(1, "Alice");

        piece.setGamePlayer(player);

        assertThat(piece.getGamePlayer()).isSameAs(player);
    }

    // ========== Selection State Tests ==========

    @Test
    void should_NotBeSelected_When_NewlyCreated() {
        TestPiece piece = new TestPiece();

        assertThat(piece.isSelected()).isFalse();
    }

    @Test
    void should_BeSelected_When_SetSelectedTrue() {
        TestPiece piece = new TestPiece();

        piece.setSelected(true);

        assertThat(piece.isSelected()).isTrue();
    }

    @Test
    void should_NotBeSelected_When_SetSelectedFalse() {
        TestPiece piece = new TestPiece();
        piece.setSelected(true);

        piece.setSelected(false);

        assertThat(piece.isSelected()).isFalse();
    }

    // ========== Quantity Tests ==========

    @Test
    void should_ReturnVisibleCount_When_GetQuantityCalled() {
        TestPiece piece = new TestPiece();
        piece.addNewTokens(3, false); // 3 visible
        piece.addNewTokens(2, true); // 2 hidden

        assertThat(piece.getQuantity()).isEqualTo(3);
    }

    @Test
    void should_ReturnHiddenCount_When_GetHiddenQuantityCalled() {
        TestPiece piece = new TestPiece();
        piece.addNewTokens(3, false); // 3 visible
        piece.addNewTokens(2, true); // 2 hidden

        assertThat(piece.getHiddenQuantity()).isEqualTo(2);
    }

    @Test
    void should_ReturnZero_When_NoTokensForQuantity() {
        TestPiece piece = new TestPiece();

        assertThat(piece.getQuantity()).isZero();
        assertThat(piece.getHiddenQuantity()).isZero();
    }

    // ========== Equals Tests ==========

    @Test
    void should_BeEqual_When_SameTypeContainerAndPlayer() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPiece piece1 = new TestPiece(1, player, "P1");
        TestPiece piece2 = new TestPiece(1, player, "P2");

        // Same type, same player, same container (null)
        assertThat(piece1.equals(piece2)).isTrue();
    }

    @Test
    void should_NotBeEqual_When_DifferentType() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPiece piece1 = new TestPiece(1, player, "P");
        TestPiece piece2 = new TestPiece(2, player, "P");

        assertThat(piece1.equals(piece2)).isFalse();
    }

    @Test
    void should_NotBeEqual_When_DifferentPlayer() {
        GamePlayer player1 = new GamePlayer(1, "Player1");
        GamePlayer player2 = new GamePlayer(2, "Player2");
        TestPiece piece1 = new TestPiece(1, player1, "P");
        TestPiece piece2 = new TestPiece(1, player2, "P");

        assertThat(piece1.equals(piece2)).isFalse();
    }

    @Test
    void should_NotBeEqual_When_ComparedToNonGamePiece() {
        TestPiece piece = new TestPiece(1, null, "P");

        assertThat(piece.equals("not a piece")).isFalse();
    }

    // ========== compareTo Tests ==========

    @Test
    void should_ReturnZero_When_EqualPieces() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestPiece piece1 = new TestPiece(1, player, "P1");
        TestPiece piece2 = new TestPiece(1, player, "P2");

        assertThat(piece1.compareTo(piece2)).isZero();
    }

    @Test
    void should_CompareByType_When_TypesDiffer() {
        GamePlayer player = new GamePlayer(1, "Player1");
        GamePieceContainerImpl container = createContainer();
        TestPiece piece1 = new TestPiece(1, player, "P1");
        TestPiece piece2 = new TestPiece(2, player, "P2");
        container.addGamePiece(piece1);
        container.addGamePiece(piece2);

        assertThat(piece1.compareTo(piece2)).isLessThan(0);
        assertThat(piece2.compareTo(piece1)).isGreaterThan(0);
    }

    // ========== Test Helpers ==========

    private GamePieceContainerImpl createContainer() {
        return new GamePieceContainerImpl(new SimpleContainer());
    }

    /**
     * Minimal concrete GamePiece for testing.
     */
    private static class TestPiece extends GamePiece {
        TestPiece() {
            super();
        }

        TestPiece(int type, GamePlayer player, String name) {
            super(type, player, name);
        }

        @Override
        public GamePiece duplicate() {
            TestPiece dup = new TestPiece();
            dup.setType(nType_);
            dup.setGamePlayer(player_);
            dup.setName(sName_);
            return dup;
        }
    }

    /**
     * Simple container for compareTo tests.
     */
    private static class SimpleContainer implements GamePieceContainer {
        @Override
        public String getName() {
            return "SimpleContainer";
        }

        @Override
        public String getDisplayName() {
            return "SimpleContainer";
        }

        @Override
        public void setGamePlayer(GamePlayer player) {
        }

        @Override
        public GamePlayer getGamePlayer() {
            return null;
        }

        @Override
        public GamePiece addGamePiece(GamePiece gp) {
            return null;
        }

        @Override
        public void removeGamePiece(GamePiece gp) {
        }

        @Override
        public int getNumPieces() {
            return 0;
        }

        @Override
        public java.util.Iterator getGamePieces() {
            return null;
        }

        @Override
        public GamePiece getGamePiece(int nType, GamePlayer owner) {
            return null;
        }

        @Override
        public boolean hasNonOwnerGamePiece(int nType, GamePlayer owner) {
            return false;
        }

        @Override
        public boolean hasMovedPieces() {
            return false;
        }

        @Override
        public boolean equals(GamePieceContainer c) {
            return c == this;
        }

        @Override
        public java.util.Map getMap() {
            return null;
        }
    }
}
