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
package com.donohoedigital.games.engine;

import com.donohoedigital.games.config.GamePlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Game - Core game state container extending TypedHashMap.
 */
@ExtendWith(MockitoExtension.class)
class GameTest {

    @Mock
    private GameContext context;

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game(context);
    }

    // ========== Constructor Tests ==========

    @Test
    void should_StoreContext_When_Constructed() {
        assertThat(game.getGameContext()).isSameAs(context);
    }

    @Test
    void should_InitializeGameOverFalse_When_Constructed() {
        assertThat(game.isGameOver()).isFalse();
    }

    @Test
    void should_InitializeNotFinished_When_Constructed() {
        assertThat(game.isFinished()).isFalse();
    }

    @Test
    void should_InitializeGoodLoad_When_Constructed() {
        assertThat(game.isGoodLoad()).isTrue();
    }

    @Test
    void should_InitializeWithNoPlayers_When_Constructed() {
        assertThat(game.getNumPlayers()).isZero();
    }

    @Test
    void should_InitializeWithNoObservers_When_Constructed() {
        assertThat(game.getNumObservers()).isZero();
    }

    @Test
    void should_InitializeWithDefaultTurn_When_Constructed() {
        assertThat(game.getTurn()).isEqualTo(1);
    }

    // ========== Player Management Tests ==========

    @Test
    void should_IncrementPlayerCount_When_PlayerAdded() {
        GamePlayer player = new GamePlayer(0, "Alice");

        game.addPlayer(player);

        assertThat(game.getNumPlayers()).isEqualTo(1);
    }

    @Test
    void should_ReturnPlayer_When_GetPlayerAtCalled() {
        GamePlayer player = new GamePlayer(0, "Alice");
        game.addPlayer(player);

        assertThat(game.getPlayerAt(0)).isSameAs(player);
    }

    @Test
    void should_DecrementPlayerCount_When_PlayerRemoved() {
        GamePlayer player = new GamePlayer(0, "Alice");
        game.addPlayer(player);

        game.removePlayer(player);

        assertThat(game.getNumPlayers()).isZero();
    }

    @Test
    void should_ReturnTrue_When_ContainsAddedPlayer() {
        GamePlayer player = new GamePlayer(0, "Alice");
        game.addPlayer(player);

        assertThat(game.containsPlayer(player)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_ContainsRemovedPlayer() {
        GamePlayer player = new GamePlayer(0, "Alice");
        game.addPlayer(player);
        game.removePlayer(player);

        assertThat(game.containsPlayer(player)).isFalse();
    }

    @Test
    void should_ClearAllPlayers_When_ClearPlayerListCalled() {
        game.addPlayer(new GamePlayer(0, "Alice"));
        game.addPlayer(new GamePlayer(1, "Bob"));

        game.clearPlayerList(0);

        assertThat(game.getNumPlayers()).isZero();
    }

    @Test
    void should_ReturnPlayerById_When_GetPlayerFromIDCalled() {
        GamePlayer alice = new GamePlayer(0, "Alice");
        GamePlayer bob = new GamePlayer(1, "Bob");
        game.addPlayer(alice);
        game.addPlayer(bob);

        assertThat(game.getPlayerFromID(1)).isSameAs(bob);
    }

    @Test
    void should_ReturnNull_When_PlayerIDNotFound() {
        assertThat(game.getPlayerFromID(999)).isNull();
    }

    @Test
    void should_ReturnPlayersCopy_When_GetPlayersCopyCalled() {
        GamePlayer alice = new GamePlayer(0, "Alice");
        game.addPlayer(alice);

        List<GamePlayer> copy = game.getPlayersCopy();
        copy.clear();

        assertThat(game.getNumPlayers()).isEqualTo(1);
    }

    // ========== Observer Management Tests ==========

    @Test
    void should_IncrementObserverCount_When_ObserverAdded() {
        GamePlayer observer = new GamePlayer(10, "Observer1");

        game.addObserver(observer);

        assertThat(game.getNumObservers()).isEqualTo(1);
    }

    @Test
    void should_SetObserverFlag_When_ObserverAdded() {
        GamePlayer observer = new GamePlayer(10, "Observer1");

        game.addObserver(observer);

        assertThat(observer.isObserver()).isTrue();
    }

    @Test
    void should_DecrementObserverCount_When_ObserverRemoved() {
        GamePlayer observer = new GamePlayer(10, "Observer1");
        game.addObserver(observer);

        game.removeObserver(observer);

        assertThat(game.getNumObservers()).isZero();
    }

    @Test
    void should_ClearObserverFlag_When_ObserverRemoved() {
        GamePlayer observer = new GamePlayer(10, "Observer1");
        game.addObserver(observer);

        game.removeObserver(observer);

        assertThat(observer.isObserver()).isFalse();
    }

    @Test
    void should_NotAddDuplicate_When_ObserverAlreadyExists() {
        GamePlayer observer = new GamePlayer(10, "Observer1");
        game.addObserver(observer);
        game.addObserver(observer);

        assertThat(game.getNumObservers()).isEqualTo(1);
    }

    @Test
    void should_ReturnTrue_When_ContainsAddedObserver() {
        GamePlayer observer = new GamePlayer(10, "Observer1");
        game.addObserver(observer);

        assertThat(game.containsObserver(observer)).isTrue();
    }

    @Test
    void should_ClearAllObservers_When_ClearObserverListCalled() {
        game.addObserver(new GamePlayer(10, "Obs1"));
        game.addObserver(new GamePlayer(11, "Obs2"));

        game.clearObserverList(0);

        assertThat(game.getNumObservers()).isZero();
    }

    // ========== Turn Tracking Tests ==========

    @Test
    void should_ReturnUpdatedTurn_When_SetTurnCalled() {
        game.setTurn(5);

        assertThat(game.getTurn()).isEqualTo(5);
    }

    // ========== Online Game ID Tests ==========

    @Test
    void should_ReturnFalse_When_NoOnlineGameIDSet() {
        assertThat(game.isOnlineGame()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_OnlineGameIDSet() {
        game.setOnlineGameID("game-123");

        assertThat(game.isOnlineGame()).isTrue();
        assertThat(game.getOnlineGameID()).isEqualTo("game-123");
    }

    @Test
    void should_ReturnTrue_When_TempOnlineGameIDSet() {
        game.setTempOnlineGameID();

        assertThat(game.isOnlineGame()).isTrue();
    }

    // ========== Game Over State Tests ==========

    @Test
    void should_ReturnTrue_When_GameOverSet() {
        game.setGameOver(true);

        assertThat(game.isGameOver()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_GameOverCleared() {
        game.setGameOver(true);
        game.setGameOver(false);

        assertThat(game.isGameOver()).isFalse();
    }

    // ========== Finished State Tests ==========

    @Test
    void should_ReturnTrue_When_FinishCalled() {
        game.finish();

        assertThat(game.isFinished()).isTrue();
    }

    @Test
    void should_ClearPlayersAndObservers_When_FinishCalled() {
        game.addPlayer(new GamePlayer(0, "Alice"));
        game.addObserver(new GamePlayer(10, "Obs1"));

        game.finish();

        assertThat(game.getNumPlayers()).isZero();
        assertThat(game.getNumObservers()).isZero();
    }

    // ========== Current Player Tests ==========

    @Test
    void should_ReturnNull_When_NoCurrentPlayerSet() {
        assertThat(game.getCurrentPlayer()).isNull();
    }

    @Test
    void should_SetCurrentPlayer_When_SetCurrentPlayerByIndex() {
        GamePlayer alice = new GamePlayer(0, "Alice");
        GamePlayer bob = new GamePlayer(1, "Bob");
        game.addPlayer(alice);
        game.addPlayer(bob);

        game.setCurrentPlayer(1);

        assertThat(game.getCurrentPlayer()).isSameAs(bob);
        assertThat(game.getCurrentPlayerIndex()).isEqualTo(1);
    }

    @Test
    void should_ClearCurrentPlayer_When_SetToNoCurrentPlayer() {
        GamePlayer alice = new GamePlayer(0, "Alice");
        game.addPlayer(alice);
        game.setCurrentPlayer(0);

        game.setCurrentPlayer(Game.NO_CURRENT_PLAYER);

        assertThat(game.getCurrentPlayer()).isNull();
    }

    @Test
    void should_SetCurrentPlayer_When_SetCurrentPlayerByReference() {
        GamePlayer alice = new GamePlayer(0, "Alice");
        GamePlayer bob = new GamePlayer(1, "Bob");
        game.addPlayer(alice);
        game.addPlayer(bob);

        game.setCurrentPlayer(bob);

        assertThat(game.getCurrentPlayer()).isSameAs(bob);
    }

    @Test
    void should_ClearCurrentPlayer_When_SetCurrentPlayerWithUnknownPlayer() {
        GamePlayer alice = new GamePlayer(0, "Alice");
        game.addPlayer(alice);
        game.setCurrentPlayer(0);

        GamePlayer unknown = new GamePlayer(99, "Unknown");
        game.setCurrentPlayer(unknown);

        assertThat(game.getCurrentPlayer()).isNull();
    }

    // ========== Password Tests ==========

    @Test
    void should_ReturnNull_When_NoPasswordSet() {
        assertThat(game.getOnlinePassword()).isNull();
    }

    @Test
    void should_ReturnPassword_When_PasswordSet() {
        game.setOnlinePassword("secret");

        assertThat(game.getOnlinePassword()).isEqualTo("secret");
    }

    // ========== Server URL Tests ==========

    @Test
    void should_ReturnNull_When_NoServerURLSet() {
        assertThat(game.getServerURL()).isNull();
    }

    @Test
    void should_ReturnURL_When_ServerURLSet() {
        game.setServerURL("http://localhost:8080");

        assertThat(game.getServerURL()).isEqualTo("http://localhost:8080");
    }

    // ========== Sequence ID Tests ==========

    @Test
    void should_ReturnOne_When_GetNextSeqIDCalledFirstTime() {
        assertThat(game.getNextSeqID()).isEqualTo(1);
    }

    @Test
    void should_IncrementSequentially_When_GetNextSeqIDCalledMultipleTimes() {
        assertThat(game.getNextSeqID()).isEqualTo(1);
        assertThat(game.getNextSeqID()).isEqualTo(2);
        assertThat(game.getNextSeqID()).isEqualTo(3);
    }

    // ========== Player Order Tests ==========

    @Test
    void should_ReturnTrue_When_PlayerAIsBeforePlayerB() {
        GamePlayer alice = new GamePlayer(0, "Alice");
        GamePlayer bob = new GamePlayer(1, "Bob");
        game.addPlayer(alice);
        game.addPlayer(bob);

        assertThat(game.isPlayerBefore(alice, bob)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_PlayerBIsBeforePlayerA() {
        GamePlayer alice = new GamePlayer(0, "Alice");
        GamePlayer bob = new GamePlayer(1, "Bob");
        game.addPlayer(alice);
        game.addPlayer(bob);

        assertThat(game.isPlayerBefore(bob, alice)).isFalse();
    }

    @Test
    void should_MovePlayerToFirst_When_MakePlayerFirstCalled() {
        GamePlayer alice = new GamePlayer(0, "Alice");
        GamePlayer bob = new GamePlayer(1, "Bob");
        GamePlayer carol = new GamePlayer(2, "Carol");
        game.addPlayer(alice);
        game.addPlayer(bob);
        game.addPlayer(carol);

        game.makePlayerFirst(bob);

        assertThat(game.getPlayerAt(0)).isSameAs(bob);
    }

    // ========== Computer Player Tests ==========

    @Test
    void should_ReturnFalse_When_NoComputerPlayers() {
        game.addPlayer(new GamePlayer(0, "Human"));

        assertThat(game.hasComputerPlayers()).isFalse();
    }

    // ========== PropertyChangeListener Tests ==========

    @Test
    void should_ReturnEmptyArray_When_NoListenersRegistered() {
        assertThat(game.getPropertyChangeListeners()).isEmpty();
    }

    @Test
    void should_NotifyListener_When_PlayerAdded() {
        TestPropertyListener listener = new TestPropertyListener();
        game.addPropertyChangeListener(Game.PROP_PLAYERS, listener);
        GamePlayer player = new GamePlayer(0, "Alice");

        game.addPlayer(player);

        assertThat(listener.events).hasSize(1);
        assertThat(listener.events.get(0).getPropertyName()).isEqualTo(Game.PROP_PLAYERS);
        assertThat(listener.events.get(0).getNewValue()).isSameAs(player);
    }

    @Test
    void should_NotifyListener_When_PlayerRemoved() {
        TestPropertyListener listener = new TestPropertyListener();
        GamePlayer player = new GamePlayer(0, "Alice");
        game.addPlayer(player);
        game.addPropertyChangeListener(Game.PROP_PLAYERS, listener);

        game.removePlayer(player);

        assertThat(listener.events).hasSize(1);
        assertThat(listener.events.get(0).getOldValue()).isSameAs(player);
    }

    @Test
    void should_NotifyListener_When_TurnChanges() {
        TestPropertyListener listener = new TestPropertyListener();
        game.addPropertyChangeListener(listener);

        game.setTurn(3);

        assertThat(listener.events).anySatisfy(event -> {
            assertThat(event.getNewValue()).isEqualTo(3);
        });
    }

    @Test
    void should_NotNotify_When_ListenerRemoved() {
        TestPropertyListener listener = new TestPropertyListener();
        game.addPropertyChangeListener(listener);
        game.removePropertyChangeListener(listener);

        game.setTurn(5);

        assertThat(listener.events).isEmpty();
    }

    @Test
    void should_ReturnRegisteredListeners_When_GetPropertyChangeListenersCalled() {
        TestPropertyListener listener = new TestPropertyListener();
        game.addPropertyChangeListener(listener);

        assertThat(game.getPropertyChangeListeners()).contains(listener);
    }

    @Test
    void should_ReturnNamedListeners_When_GetPropertyChangeListenersWithNameCalled() {
        TestPropertyListener listener = new TestPropertyListener();
        game.addPropertyChangeListener(Game.PROP_PLAYERS, listener);

        assertThat(game.getPropertyChangeListeners(Game.PROP_PLAYERS)).contains(listener);
    }

    // ========== toString Tests ==========

    @Test
    void should_ContainPlayersInfo_When_ToStringCalled() {
        game.addPlayer(new GamePlayer(0, "Alice"));

        String result = game.toString();

        assertThat(result).contains("Game players=");
        assertThat(result).contains("Alice");
    }

    // ========== Game Options Tests ==========

    @Test
    void should_ReturnNonNullOptions_When_GetGameOptionsCalled() {
        assertThat(game.getGameOptions()).isNotNull();
    }

    // ========== canSave Tests ==========

    @Test
    void should_ReturnFalse_When_NoLastGameState() {
        assertThat(game.canSave()).isFalse();
    }

    // ========== Test Helper ==========

    private static class TestPropertyListener implements PropertyChangeListener {
        final List<PropertyChangeEvent> events = new ArrayList<>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            events.add(evt);
        }
    }
}
