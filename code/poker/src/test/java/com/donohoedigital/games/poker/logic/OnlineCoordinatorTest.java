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
package com.donohoedigital.games.poker.logic;

import com.donohoedigital.games.poker.logic.OnlineCoordinator.MessageDestination;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OnlineCoordinator - online game coordination logic extracted
 * from TournamentDirector.java. Tests run in headless mode with no UI
 * dependencies. Part of Wave 3 testability refactoring.
 */
@Tag("unit")
class OnlineCoordinatorTest {

    // =================================================================
    // routeDealerChat() Tests
    // =================================================================

    @Test
    void routeDealerChat_Should_ReturnOnlineManager_When_HasOnlineManager() {
        MessageDestination result = OnlineCoordinator.routeDealerChat(true, true);

        assertThat(result).isEqualTo(MessageDestination.ONLINE_MANAGER);
    }

    @Test
    void routeDealerChat_Should_ReturnOnlineManager_When_HasOnlineManagerRegardlessOfTable() {
        MessageDestination result = OnlineCoordinator.routeDealerChat(true, false);

        assertThat(result).isEqualTo(MessageDestination.ONLINE_MANAGER);
    }

    @Test
    void routeDealerChat_Should_ReturnLocal_When_NoManagerButCurrentTable() {
        MessageDestination result = OnlineCoordinator.routeDealerChat(false, true);

        assertThat(result).isEqualTo(MessageDestination.LOCAL);
    }

    @Test
    void routeDealerChat_Should_ReturnNone_When_NoManagerAndNotCurrentTable() {
        MessageDestination result = OnlineCoordinator.routeDealerChat(false, false);

        assertThat(result).isEqualTo(MessageDestination.NONE);
    }

    // =================================================================
    // routeDirectorChat() Tests
    // =================================================================

    @Test
    void routeDirectorChat_Should_ReturnOnlineManager_When_HasOnlineManager() {
        MessageDestination result = OnlineCoordinator.routeDirectorChat(true);

        assertThat(result).isEqualTo(MessageDestination.ONLINE_MANAGER);
    }

    @Test
    void routeDirectorChat_Should_ReturnLocal_When_NoOnlineManager() {
        MessageDestination result = OnlineCoordinator.routeDirectorChat(false);

        assertThat(result).isEqualTo(MessageDestination.LOCAL);
    }

    // =================================================================
    // shouldNotifyWanGameStart() Tests
    // =================================================================

    @Test
    void shouldNotifyWanGameStart_Should_ReturnTrue_When_OnlineHostPublic() {
        boolean result = OnlineCoordinator.shouldNotifyWanGameStart(true, true, true);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotifyWanGameStart_Should_ReturnFalse_When_NotOnline() {
        boolean result = OnlineCoordinator.shouldNotifyWanGameStart(false, true, true);

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotifyWanGameStart_Should_ReturnFalse_When_NotHost() {
        boolean result = OnlineCoordinator.shouldNotifyWanGameStart(true, false, true);

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotifyWanGameStart_Should_ReturnFalse_When_NotPublic() {
        boolean result = OnlineCoordinator.shouldNotifyWanGameStart(true, true, false);

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotifyWanGameStart_Should_ReturnFalse_When_PrivateGame() {
        boolean result = OnlineCoordinator.shouldNotifyWanGameStart(true, true, false);

        assertThat(result).isFalse();
    }

    // =================================================================
    // shouldNotifyWanGameEnd() Tests
    // =================================================================

    @Test
    void shouldNotifyWanGameEnd_Should_ReturnTrue_When_OnlineHostPublic() {
        boolean result = OnlineCoordinator.shouldNotifyWanGameEnd(true, true, true);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotifyWanGameEnd_Should_ReturnFalse_When_NotOnline() {
        boolean result = OnlineCoordinator.shouldNotifyWanGameEnd(false, true, true);

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotifyWanGameEnd_Should_ReturnFalse_When_NotHost() {
        boolean result = OnlineCoordinator.shouldNotifyWanGameEnd(true, false, true);

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotifyWanGameEnd_Should_ReturnFalse_When_NotPublic() {
        boolean result = OnlineCoordinator.shouldNotifyWanGameEnd(true, true, false);

        assertThat(result).isFalse();
    }

    // =================================================================
    // shouldSendToClients() Tests
    // =================================================================

    @Test
    void shouldSendToClients_Should_ReturnTrue_When_OnlineAndHasManager() {
        boolean result = OnlineCoordinator.shouldSendToClients(true, true);

        assertThat(result).isTrue();
    }

    @Test
    void shouldSendToClients_Should_ReturnFalse_When_NotOnline() {
        boolean result = OnlineCoordinator.shouldSendToClients(false, true);

        assertThat(result).isFalse();
    }

    @Test
    void shouldSendToClients_Should_ReturnFalse_When_NoManager() {
        boolean result = OnlineCoordinator.shouldSendToClients(true, false);

        assertThat(result).isFalse();
    }

    @Test
    void shouldSendToClients_Should_ReturnFalse_When_NeitherOnlineNorManager() {
        boolean result = OnlineCoordinator.shouldSendToClients(false, false);

        assertThat(result).isFalse();
    }

    // =================================================================
    // shouldWaitForClient() Tests
    // =================================================================

    @Test
    void shouldWaitForClient_Should_ReturnTrue_When_HostAndRemotePlayer() {
        boolean result = OnlineCoordinator.shouldWaitForClient(true, true);

        assertThat(result).isTrue();
    }

    @Test
    void shouldWaitForClient_Should_ReturnFalse_When_NotHost() {
        boolean result = OnlineCoordinator.shouldWaitForClient(false, true);

        assertThat(result).isFalse();
    }

    @Test
    void shouldWaitForClient_Should_ReturnFalse_When_NotRemotePlayer() {
        boolean result = OnlineCoordinator.shouldWaitForClient(true, false);

        assertThat(result).isFalse();
    }

    @Test
    void shouldWaitForClient_Should_ReturnFalse_When_LocalPlayer() {
        boolean result = OnlineCoordinator.shouldWaitForClient(true, false);

        assertThat(result).isFalse();
    }

    // =================================================================
    // shouldSendOnlyToWaitlist() Tests
    // =================================================================

    @Test
    void shouldSendOnlyToWaitlist_Should_ReturnTrue_When_OnlineAndHost() {
        boolean result = OnlineCoordinator.shouldSendOnlyToWaitlist(true, true);

        assertThat(result).isTrue();
    }

    @Test
    void shouldSendOnlyToWaitlist_Should_ReturnFalse_When_NotOnline() {
        boolean result = OnlineCoordinator.shouldSendOnlyToWaitlist(false, true);

        assertThat(result).isFalse();
    }

    @Test
    void shouldSendOnlyToWaitlist_Should_ReturnFalse_When_NotHost() {
        boolean result = OnlineCoordinator.shouldSendOnlyToWaitlist(true, false);

        assertThat(result).isFalse();
    }

    @Test
    void shouldSendOnlyToWaitlist_Should_ReturnFalse_When_PracticeMode() {
        boolean result = OnlineCoordinator.shouldSendOnlyToWaitlist(false, false);

        assertThat(result).isFalse();
    }
}
