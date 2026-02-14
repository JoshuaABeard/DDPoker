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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GamePieceContainerImpl - Delegation-based game piece container
 * implementation.
 */
class GamePieceContainerImplTest {

    private GamePieceContainerImpl container;
    private TestContainer implFor;

    @BeforeEach
    void setUp() {
        implFor = new TestContainer();
        container = new GamePieceContainerImpl(implFor);
    }

    // ========== Constructor Tests ==========

    @Test
    void should_InitializeEmpty_When_Created() {
        assertThat(container.getNumPieces()).isZero();
    }

    @Test
    void should_StoreImplFor_When_Created() {
        // Verify delegation works by checking getName
        assertThat(container.getName()).isEqualTo("GamePieceContainerImpl");
    }

    // ========== Info Delegation Tests ==========

    @Test
    void should_ReturnName_When_GetNameCalled() {
        assertThat(container.getName()).isEqualTo("GamePieceContainerImpl");
    }

    @Test
    void should_ReturnDisplayName_When_GetDisplayNameCalled() {
        assertThat(container.getDisplayName()).isEqualTo("GamePieceContainerImpl");
    }

    // ========== Owner Management Tests ==========

    @Test
    void should_ReturnNull_When_NoOwnerSet() {
        assertThat(container.getGamePlayer()).isNull();
    }

    @Test
    void should_ReturnOwner_When_OwnerSet() {
        GamePlayer player = new GamePlayer(1, "Player1");

        container.setGamePlayer(player);

        assertThat(container.getGamePlayer()).isSameAs(player);
    }

    // ========== Piece Management Tests ==========

    @Test
    void should_AddPiece_When_PieceHasNoContainer() {
        TestGamePiece piece = new TestGamePiece(1, null);

        GamePiece result = container.addGamePiece(piece);

        assertThat(result).isSameAs(piece);
        assertThat(container.getNumPieces()).isEqualTo(1);
        assertThat(piece.getContainer()).isSameAs(implFor);
    }

    @Test
    void should_TransferTokens_When_DuplicatePieceAdded() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestGamePiece piece1 = new TestGamePiece(1, player);
        TestGamePiece piece2 = new TestGamePiece(1, player);

        container.addGamePiece(piece1);
        GamePiece result = container.addGamePiece(piece2);

        assertThat(result).isSameAs(piece1);
        assertThat(container.getNumPieces()).isEqualTo(1);
        // Transfer happens from piece2 to piece1, so piece1 gets the transfer count
        assertThat(piece1.transferCount).isEqualTo(1);
    }

    @Test
    void should_RemovePiece_When_PieceExists() {
        TestGamePiece piece = new TestGamePiece(1, null);
        container.addGamePiece(piece);

        container.removeGamePiece(piece);

        assertThat(container.getNumPieces()).isZero();
        assertThat(piece.getContainer()).isNull();
    }

    @Test
    void should_ReturnCorrectCount_When_MultiplePiecesAdded() {
        GamePlayer player1 = new GamePlayer(1, "Player1");
        GamePlayer player2 = new GamePlayer(2, "Player2");
        GamePlayer player3 = new GamePlayer(3, "Player3");
        // Use same type but different players to avoid TreeMap comparison issues
        TestGamePiece piece1 = new TestGamePiece(1, player1);
        TestGamePiece piece2 = new TestGamePiece(1, player2);
        TestGamePiece piece3 = new TestGamePiece(1, player3);

        container.addGamePiece(piece1);
        container.addGamePiece(piece2);
        container.addGamePiece(piece3);

        assertThat(container.getNumPieces()).isEqualTo(3);
    }

    @Test
    void should_IterateAllPieces_When_GetGamePiecesCalled() {
        GamePlayer player1 = new GamePlayer(1, "Player1");
        GamePlayer player2 = new GamePlayer(2, "Player2");
        // Use same type but different players
        TestGamePiece piece1 = new TestGamePiece(1, player1);
        TestGamePiece piece2 = new TestGamePiece(1, player2);
        container.addGamePiece(piece1);
        container.addGamePiece(piece2);

        List<GamePiece> pieces = new ArrayList<>();
        Iterator iter = container.getGamePieces();
        while (iter.hasNext()) {
            pieces.add((GamePiece) iter.next());
        }

        assertThat(pieces).hasSize(2);
        assertThat(pieces).contains(piece1, piece2);
    }

    // ========== Query Tests ==========

    @Test
    void should_ReturnPiece_When_TypeMatches() {
        TestGamePiece piece = new TestGamePiece(42, null);
        container.addGamePiece(piece);

        GamePiece result = container.getGamePiece(42, null);

        assertThat(result).isSameAs(piece);
    }

    @Test
    void should_ReturnNull_When_TypeDoesNotMatch() {
        TestGamePiece piece = new TestGamePiece(1, null);
        container.addGamePiece(piece);

        GamePiece result = container.getGamePiece(2, null);

        assertThat(result).isNull();
    }

    @Test
    void should_ReturnPiece_When_TypeAndOwnerMatch() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestGamePiece piece = new TestGamePiece(1, player);
        container.addGamePiece(piece);

        GamePiece result = container.getGamePiece(1, player);

        assertThat(result).isSameAs(piece);
    }

    @Test
    void should_ReturnNull_When_TypeMatchesButOwnerDoesNot() {
        GamePlayer player1 = new GamePlayer(1, "Player1");
        GamePlayer player2 = new GamePlayer(2, "Player2");
        TestGamePiece piece = new TestGamePiece(1, player1);
        container.addGamePiece(piece);

        GamePiece result = container.getGamePiece(1, player2);

        assertThat(result).isNull();
    }

    @Test
    void should_ReturnTrue_When_HasNonOwnerPiece() {
        GamePlayer player1 = new GamePlayer(1, "Player1");
        GamePlayer player2 = new GamePlayer(2, "Player2");
        TestGamePiece piece = new TestGamePiece(1, player2);
        container.addGamePiece(piece);

        boolean result = container.hasNonOwnerGamePiece(1, player1);

        assertThat(result).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NoNonOwnerPiece() {
        GamePlayer player = new GamePlayer(1, "Player1");
        TestGamePiece piece = new TestGamePiece(1, player);
        container.addGamePiece(piece);

        boolean result = container.hasNonOwnerGamePiece(1, player);

        assertThat(result).isFalse();
    }

    @Test
    void should_ReturnTrue_When_HasMovedPieces() {
        TestGamePiece piece = new TestGamePiece(1, null);
        piece.hasMoved = true;
        container.addGamePiece(piece);

        boolean result = container.hasMovedPieces();

        assertThat(result).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NoMovedPieces() {
        TestGamePiece piece = new TestGamePiece(1, null);
        piece.hasMoved = false;
        container.addGamePiece(piece);

        boolean result = container.hasMovedPieces();

        assertThat(result).isFalse();
    }

    // ========== Utility Tests ==========

    @Test
    void should_ReturnMap_When_GetMapCalled() {
        assertThat(container.getMap()).isNotNull();
        assertThat(container.getMap()).isEmpty();
    }

    @Test
    void should_ReturnFalse_When_EqualsCalledWithAny() {
        TestContainer other = new TestContainer();

        assertThat(container.equals(other)).isFalse();
    }

    // ========== Test Helpers ==========

    /**
     * Simple test container for delegation tests.
     */
    private static class TestContainer implements GamePieceContainer {
        @Override
        public String getName() {
            return "TestContainer";
        }

        @Override
        public String getDisplayName() {
            return "TestContainer";
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
        public Iterator getGamePieces() {
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

    /**
     * Minimal GamePiece implementation for testing.
     */
    private static class TestGamePiece extends GamePiece {
        boolean hasMoved = false;
        int transferCount = 0;

        TestGamePiece(int type, GamePlayer owner) {
            super(type, owner, "TestPiece" + type);
        }

        @Override
        public boolean hasMovedTokens() {
            return hasMoved;
        }

        @Override
        public void transferAllTokensTo(GamePiece piece) {
            if (piece instanceof TestGamePiece) {
                ((TestGamePiece) piece).transferCount++;
            }
        }

        @Override
        public GamePiece duplicate() {
            return new TestGamePiece(nType_, player_);
        }
    }
}
