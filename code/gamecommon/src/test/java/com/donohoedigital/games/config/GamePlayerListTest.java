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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the GamePlayerList interface contract, verified via a minimal
 * concrete implementation. Also exercises the GamePlayer objects stored in the
 * list.
 */
class GamePlayerListTest {

    private SimplePlayerList list;

    @BeforeEach
    void setUp() {
        list = new SimplePlayerList();
    }

    // ========== Empty list state ==========

    @Test
    void should_HaveZeroPlayers_When_ListIsEmpty() {
        assertThat(list.getNumPlayers()).isZero();
    }

    @Test
    void should_ReturnFalse_When_ListIsEmptyAndContainsPlayerChecked() {
        GamePlayer player = new GamePlayer(1, "Alice");

        assertThat(list.containsPlayer(player)).isFalse();
    }

    // ========== addPlayer and getPlayerAt ==========

    @Test
    void should_ReturnOne_When_OnePlayerAdded() {
        list.addPlayer(new GamePlayer(1, "Alice"));

        assertThat(list.getNumPlayers()).isEqualTo(1);
    }

    @Test
    void should_ReturnPlayer_When_GetPlayerAtCalledWithValidIndex() {
        GamePlayer alice = new GamePlayer(1, "Alice");
        list.addPlayer(alice);

        assertThat(list.getPlayerAt(0)).isSameAs(alice);
    }

    @Test
    void should_ThrowException_When_GetPlayerAtCalledWithInvalidIndex() {
        assertThatThrownBy(() -> list.getPlayerAt(0)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void should_ReturnPlayers_When_MultiplePlayersAdded() {
        GamePlayer alice = new GamePlayer(1, "Alice");
        GamePlayer bob = new GamePlayer(2, "Bob");
        GamePlayer carol = new GamePlayer(3, "Carol");

        list.addPlayer(alice);
        list.addPlayer(bob);
        list.addPlayer(carol);

        assertThat(list.getNumPlayers()).isEqualTo(3);
        assertThat(list.getPlayerAt(0)).isSameAs(alice);
        assertThat(list.getPlayerAt(1)).isSameAs(bob);
        assertThat(list.getPlayerAt(2)).isSameAs(carol);
    }

    // ========== containsPlayer ==========

    @Test
    void should_ReturnTrue_When_PlayerWasAdded() {
        GamePlayer player = new GamePlayer(5, "Player5");
        list.addPlayer(player);

        assertThat(list.containsPlayer(player)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_DifferentPlayerInstanceChecked() {
        list.addPlayer(new GamePlayer(1, "Alice"));
        GamePlayer other = new GamePlayer(1, "Alice"); // same id/name, different instance

        assertThat(list.containsPlayer(other)).isFalse();
    }

    @Test
    void should_UseReferenceEquality_When_CheckingContainsPlayer() {
        GamePlayer alice = new GamePlayer(1, "Alice");
        GamePlayer alsoAlice = new GamePlayer(1, "Alice");
        list.addPlayer(alice);

        assertThat(list.containsPlayer(alice)).isTrue();
        assertThat(list.containsPlayer(alsoAlice)).isFalse();
    }

    // ========== clearPlayerList ==========

    @Test
    void should_HaveZeroPlayers_When_ClearCalledAfterAdding() {
        list.addPlayer(new GamePlayer(1, "Alice"));
        list.addPlayer(new GamePlayer(2, "Bob"));

        list.clearPlayerList(0);

        assertThat(list.getNumPlayers()).isZero();
    }

    @Test
    void should_AllowAddingPlayers_When_ClearCalledThenNewPlayersAdded() {
        list.addPlayer(new GamePlayer(1, "Alice"));
        list.clearPlayerList(0);

        GamePlayer newPlayer = new GamePlayer(10, "NewPlayer");
        list.addPlayer(newPlayer);

        assertThat(list.getNumPlayers()).isEqualTo(1);
        assertThat(list.getPlayerAt(0)).isSameAs(newPlayer);
    }

    // ========== Iteration order ==========

    @Test
    void should_PreserveInsertionOrder_When_IteratingByIndex() {
        GamePlayer p0 = new GamePlayer(GamePlayer.HOST_ID, "Host");
        GamePlayer p1 = new GamePlayer(1, "Player1");
        GamePlayer p2 = new GamePlayer(2, "Player2");

        list.addPlayer(p0);
        list.addPlayer(p1);
        list.addPlayer(p2);

        List<GamePlayer> collected = new ArrayList<>();
        for (int i = 0; i < list.getNumPlayers(); i++) {
            collected.add(list.getPlayerAt(i));
        }

        assertThat(collected).containsExactly(p0, p1, p2);
    }

    // ========== HOST_ID convention ==========

    @Test
    void should_IdentifyHostPlayer_When_AddedWithHostId() {
        GamePlayer host = new GamePlayer(GamePlayer.HOST_ID, "Host");
        list.addPlayer(host);

        GamePlayer retrieved = list.getPlayerAt(0);

        assertThat(retrieved.isHost()).isTrue();
        assertThat(retrieved.getID()).isEqualTo(GamePlayer.HOST_ID);
    }

    // ========== Minimal concrete implementation used in tests ==========

    /**
     * Minimal ArrayList-backed implementation of GamePlayerList for testing the
     * interface contract without pulling in the Game class from gameengine.
     */
    private static class SimplePlayerList implements GamePlayerList {
        private final List<GamePlayer> players = new ArrayList<>();

        @Override
        public GamePlayer getPlayerAt(int i) {
            return players.get(i);
        }

        @Override
        public int getNumPlayers() {
            return players.size();
        }

        @Override
        public void addPlayer(GamePlayer p) {
            players.add(p);
        }

        @Override
        public boolean containsPlayer(GamePlayer player) {
            return players.contains(player);
        }

        @Override
        public void clearPlayerList(int nNewCount) {
            players.clear();
        }
    }
}
