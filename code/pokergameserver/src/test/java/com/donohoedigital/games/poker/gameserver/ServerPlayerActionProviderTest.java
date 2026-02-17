/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.PlayerActionProvider;
import com.donohoedigital.games.poker.core.state.ActionType;

/**
 * Test for ServerPlayerActionProvider - validates action routing, timeout
 * handling, and action validation.
 */
class ServerPlayerActionProviderTest {

    private ServerPlayerActionProvider provider;
    private MockAIProvider mockAI;
    private AtomicReference<ActionRequest> capturedRequest;
    private Map<Long, ServerPlayerSession> playerSessions;

    private static final int TIMEOUT_SECONDS = 1; // Short timeout for testing

    @BeforeEach
    void setUp() {
        mockAI = new MockAIProvider();
        capturedRequest = new AtomicReference<>();
        playerSessions = new ConcurrentHashMap<>();

        provider = new ServerPlayerActionProvider(mockAI, capturedRequest::set, TIMEOUT_SECONDS, 2, playerSessions);
    }

    // === AI Player Actions ===

    @Test
    void testAIPlayerAction() {
        ServerPlayer aiPlayer = createAIPlayer(1, "AI");
        ActionOptions options = createSimpleOptions();

        mockAI.nextAction = PlayerAction.call();

        PlayerAction action = provider.getAction(aiPlayer, options);

        assertEquals(ActionType.CALL, action.actionType());
        assertNull(capturedRequest.get(), "No callback should be invoked for AI players");
    }

    @Test
    void testAIPlayerMultipleCalls() {
        ServerPlayer aiPlayer = createAIPlayer(1, "AI");
        ActionOptions options = createSimpleOptions();

        // First call
        mockAI.nextAction = PlayerAction.check();
        PlayerAction action1 = provider.getAction(aiPlayer, options);
        assertEquals(ActionType.CHECK, action1.actionType());

        // Second call
        mockAI.nextAction = PlayerAction.fold();
        PlayerAction action2 = provider.getAction(aiPlayer, options);
        assertEquals(ActionType.FOLD, action2.actionType());
    }

    // === Human Player Actions ===

    @Test
    void testHumanPlayerAction() throws Exception {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createSimpleOptions();

        // Start action request in background
        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(humanPlayer, options);
        });

        // Wait for callback to be invoked
        Thread.sleep(50);
        assertNotNull(capturedRequest.get(), "Callback should be invoked for human player");
        assertEquals(humanPlayer.getID(), capturedRequest.get().player().getID());

        // Submit action
        provider.submitAction(humanPlayer.getID(), PlayerAction.call());

        // Action should complete
        PlayerAction action = actionFuture.get(1, TimeUnit.SECONDS);
        assertEquals(ActionType.CALL, action.actionType());
    }

    @Test
    void testHumanPlayerTimeout_AutoFold() {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createOptions(true, false, true, 100);

        // Don't submit action - let it timeout
        PlayerAction action = provider.getAction(humanPlayer, options);

        // Should auto-fold on timeout
        assertEquals(ActionType.FOLD, action.actionType());
    }

    @Test
    void testHumanPlayerTimeout_AutoCheck() {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createOptions(true, true, false, 0); // Check is available

        // Don't submit action - let it timeout
        PlayerAction action = provider.getAction(humanPlayer, options);

        // Should auto-check on timeout (check is free, prefer over fold)
        assertEquals(ActionType.CHECK, action.actionType());
    }

    // === Action Validation ===

    @Test
    void testValidateAction_ValidCall() throws Exception {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createOptions(true, false, true, 100);

        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(humanPlayer, options);
        });

        Thread.sleep(50);
        provider.submitAction(humanPlayer.getID(), PlayerAction.call(), options);

        PlayerAction action = actionFuture.get(1, TimeUnit.SECONDS);
        assertEquals(ActionType.CALL, action.actionType());
    }

    @Test
    void testValidateAction_InvalidAction_DefaultsToFold() throws Exception {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createOptions(true, false, true, 100);

        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(humanPlayer, options);
        });

        Thread.sleep(50);
        // Try to check when check is not allowed
        provider.submitAction(humanPlayer.getID(), PlayerAction.check(), options);

        PlayerAction action = actionFuture.get(1, TimeUnit.SECONDS);
        assertEquals(ActionType.FOLD, action.actionType());
    }

    @Test
    void testValidateAction_BetAmount_Clamped() throws Exception {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createBetOptions(50, 200);

        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(humanPlayer, options);
        });

        Thread.sleep(50);
        // Try to bet more than max
        provider.submitAction(humanPlayer.getID(), PlayerAction.bet(500), options);

        PlayerAction action = actionFuture.get(1, TimeUnit.SECONDS);
        assertEquals(ActionType.BET, action.actionType());
        assertEquals(200, action.amount(), "Bet amount should be clamped to max");
    }

    @Test
    void testValidateAction_BetAmount_ClampedToMin() throws Exception {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createBetOptions(50, 200);

        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(humanPlayer, options);
        });

        Thread.sleep(50);
        // Try to bet less than min
        provider.submitAction(humanPlayer.getID(), PlayerAction.bet(10), options);

        PlayerAction action = actionFuture.get(1, TimeUnit.SECONDS);
        assertEquals(ActionType.BET, action.actionType());
        assertEquals(50, action.amount(), "Bet amount should be clamped to min");
    }

    @Test
    void testValidateAction_RaiseAmount_Clamped() throws Exception {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createRaiseOptions(100, 500);

        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(humanPlayer, options);
        });

        Thread.sleep(50);
        // Try to raise too much
        provider.submitAction(humanPlayer.getID(), PlayerAction.raise(1000), options);

        PlayerAction action = actionFuture.get(1, TimeUnit.SECONDS);
        assertEquals(ActionType.RAISE, action.actionType());
        assertEquals(500, action.amount(), "Raise amount should be clamped to max");
    }

    // === Concurrent Submissions ===

    @Test
    void testConcurrentSubmissions_OnlyFirstAccepted() throws Exception {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createSimpleOptions();

        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(humanPlayer, options);
        });

        Thread.sleep(50);

        // Submit two actions concurrently
        provider.submitAction(humanPlayer.getID(), PlayerAction.call(), options);
        provider.submitAction(humanPlayer.getID(), PlayerAction.fold(), options); // Should be ignored

        PlayerAction action = actionFuture.get(1, TimeUnit.SECONDS);
        assertEquals(ActionType.CALL, action.actionType(), "First action should be accepted");
    }

    @Test
    void testSubmitAction_NoWaitingRequest() {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createSimpleOptions();

        // Submit action without calling getAction first - should be ignored silently
        provider.submitAction(humanPlayer.getID(), PlayerAction.call(), options);

        // No exception should be thrown
    }

    // === Edge Cases ===

    @Test
    void testMultipleHumanPlayersSequentially() throws Exception {
        ServerPlayer human1 = createHumanPlayer(1, "Human1");
        ServerPlayer human2 = createHumanPlayer(2, "Human2");
        ActionOptions options = createSimpleOptions();

        // First player
        CompletableFuture<PlayerAction> future1 = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(human1, options);
        });
        Thread.sleep(50);
        provider.submitAction(human1.getID(), PlayerAction.call(), options);
        assertEquals(ActionType.CALL, future1.get(1, TimeUnit.SECONDS).actionType());

        // Second player
        CompletableFuture<PlayerAction> future2 = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(human2, options);
        });
        Thread.sleep(50);
        provider.submitAction(human2.getID(), PlayerAction.check(), options);
        assertEquals(ActionType.CHECK, future2.get(1, TimeUnit.SECONDS).actionType());
    }

    // === Helpers ===

    private ServerPlayer createHumanPlayer(int id, String name) {
        return new ServerPlayer(id, name, true, 0, 1000);
    }

    private ServerPlayer createAIPlayer(int id, String name) {
        return new ServerPlayer(id, name, false, 5, 1000);
    }

    private ActionOptions createSimpleOptions() {
        // ActionOptions(canCheck, canCall, canBet, canRaise, canFold, callAmount,
        // minBet, maxBet, minRaise, maxRaise,
        // timeoutSeconds)
        return new ActionOptions(true, true, false, false, true, 50, 0, 0, 0, 0, 0);
    }

    private ActionOptions createOptions(boolean canFold, boolean canCheck, boolean canCall, int callAmount) {
        return new ActionOptions(canCheck, canCall, false, false, canFold, callAmount, 0, 0, 0, 0, 0);
    }

    private ActionOptions createBetOptions(int minBet, int maxBet) {
        return new ActionOptions(false, false, true, false, true, 0, minBet, maxBet, 0, 0, 0);
    }

    private ActionOptions createRaiseOptions(int minRaise, int maxRaise) {
        return new ActionOptions(false, false, false, true, true, 0, 0, 0, minRaise, maxRaise, 0);
    }

    // === Mock AI Provider ===

    private static class MockAIProvider implements PlayerActionProvider {
        PlayerAction nextAction = PlayerAction.fold();

        @Override
        public PlayerAction getAction(com.donohoedigital.games.poker.core.GamePlayerInfo player,
                ActionOptions options) {
            return nextAction;
        }
    }

}
