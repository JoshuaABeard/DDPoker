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
 * Tests for Token - game token with hidden state and action history.
 */
class TokenTest {

    // ========== Constructor Tests ==========

    @Test
    void should_NotBeHidden_When_DefaultConstructorUsed() {
        Token token = new Token();

        assertThat(token.isHidden()).isFalse();
    }

    @Test
    void should_BeHidden_When_ConstructedWithHiddenTrue() {
        Token token = new Token(true);

        assertThat(token.isHidden()).isTrue();
    }

    @Test
    void should_NotBeHidden_When_ConstructedWithHiddenFalse() {
        Token token = new Token(false);

        assertThat(token.isHidden()).isFalse();
    }

    // ========== Hidden State Tests ==========

    @Test
    void should_UpdateHiddenState_When_SetHiddenCalled() {
        Token token = new Token();

        token.setHidden(true);
        assertThat(token.isHidden()).isTrue();

        token.setHidden(false);
        assertThat(token.isHidden()).isFalse();
    }

    // ========== Action History Tests ==========

    @Test
    void should_HaveNoActions_When_NewlyCreated() {
        Token token = new Token();

        assertThat(token.getNumActions()).isZero();
        assertThat(token.getLastAction()).isNull();
    }

    @Test
    void should_AddAction_When_AddActionCalled() {
        Token token = new Token();
        TokenAction action = new TokenAction();

        token.addAction(action);

        assertThat(token.getNumActions()).isEqualTo(1);
        assertThat(token.getLastAction()).isSameAs(action);
        assertThat(action.getToken()).isSameAs(token);
    }

    @Test
    void should_ReturnLastAction_When_MultipleActionsAdded() {
        Token token = new Token();
        TokenAction action1 = new TokenAction();
        TokenAction action2 = new TokenAction();
        TokenAction action3 = new TokenAction();

        token.addAction(action1);
        token.addAction(action2);
        token.addAction(action3);

        assertThat(token.getLastAction()).isSameAs(action3);
        assertThat(token.getNumActions()).isEqualTo(3);
    }

    @Test
    void should_ReturnSecondToLastAction_When_MultipleActionsExist() {
        Token token = new Token();
        TokenAction action1 = new TokenAction();
        TokenAction action2 = new TokenAction();

        token.addAction(action1);
        token.addAction(action2);

        assertThat(token.getSecondToLastAction()).isSameAs(action1);
    }

    @Test
    void should_ReturnNull_When_SecondToLastActionWithLessThanTwoActions() {
        Token token = new Token();

        assertThat(token.getSecondToLastAction()).isNull();

        token.addAction(new TokenAction());
        assertThat(token.getSecondToLastAction()).isNull();
    }

    @Test
    void should_ReturnActionAtIndex_When_GetActionCalled() {
        Token token = new Token();
        TokenAction action0 = new TokenAction();
        TokenAction action1 = new TokenAction();

        token.addAction(action0);
        token.addAction(action1);

        assertThat(token.getAction(0)).isSameAs(action0);
        assertThat(token.getAction(1)).isSameAs(action1);
    }

    @Test
    void should_RemoveAndReturnLastAction_When_RemoveLastActionCalled() {
        Token token = new Token();
        TokenAction action1 = new TokenAction();
        TokenAction action2 = new TokenAction();

        token.addAction(action1);
        token.addAction(action2);

        TokenAction removed = token.removeLastAction();

        assertThat(removed).isSameAs(action2);
        assertThat(token.getNumActions()).isEqualTo(1);
        assertThat(token.getLastAction()).isSameAs(action1);
    }

    @Test
    void should_ReturnNull_When_RemoveLastActionOnEmptyHistory() {
        Token token = new Token();

        TokenAction removed = token.removeLastAction();

        assertThat(removed).isNull();
    }

    @Test
    void should_ClearAllActions_When_ClearActionsCalled() {
        Token token = new Token();
        token.addAction(new TokenAction());
        token.addAction(new TokenAction());

        token.clearActions();

        assertThat(token.getNumActions()).isZero();
    }

    // ========== GamePiece Association Tests ==========

    @Test
    void should_ReturnNull_When_NoGamePieceSet() {
        Token token = new Token();

        assertThat(token.getGamePiece()).isNull();
    }

    @Test
    void should_ReturnGamePiece_When_SetGamePieceCalled() {
        Token token = new Token();
        TestGamePiece piece = new TestGamePiece();

        token.setGamePiece(piece);

        assertThat(token.getGamePiece()).isSameAs(piece);
    }

    // ========== TotalMoves Tests ==========

    @Test
    void should_ReturnZero_When_NoActions() {
        Token token = new Token();

        assertThat(token.getTotalMoves()).isZero();
    }

    @Test
    void should_SumMoves_When_MultipleActions() {
        Token token = new Token();
        TokenAction a1 = new TokenAction(null, 3, null);
        TokenAction a2 = new TokenAction(null, 5, null);

        token.addAction(a1);
        token.addAction(a2);

        assertThat(token.getTotalMoves()).isEqualTo(8);
    }

    // ========== Test Helpers ==========

    /**
     * Minimal GamePiece subclass for testing Token's piece association.
     */
    private static class TestGamePiece extends GamePiece {
        @Override
        public GamePiece duplicate() {
            return new TestGamePiece();
        }
    }
}
