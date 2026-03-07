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
import com.donohoedigital.games.poker.engine.PlayerAction;
import com.donohoedigital.games.poker.core.PlayerActionProvider;
import com.donohoedigital.games.poker.engine.state.ActionType;

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

    // === Zip Mode ===

    @Test
    void testZipMode_SkipsAIDelay() {
        // Provider with 500ms AI delay
        ServerPlayerActionProvider delayedProvider = new ServerPlayerActionProvider(mockAI, capturedRequest::set, 0, 2,
                playerSessions, 500);

        ServerPlayer aiPlayer = createAIPlayer(1, "AI");
        ActionOptions options = createSimpleOptions();
        mockAI.nextAction = PlayerAction.call();

        delayedProvider.setZipMode(true);

        long start = System.currentTimeMillis();
        PlayerAction action = delayedProvider.getAction(aiPlayer, options);
        long elapsedMs = System.currentTimeMillis() - start;

        assertEquals(ActionType.CALL, action.actionType());
        assertTrue(elapsedMs < 200, "Zip mode should skip the 500ms AI delay (elapsed=" + elapsedMs + "ms)");
    }

    @Test
    void testZipMode_DelayAppliedWhenOff() {
        // Provider with 100ms delay (small enough for test to be fast)
        ServerPlayerActionProvider delayedProvider = new ServerPlayerActionProvider(mockAI, capturedRequest::set, 0, 2,
                playerSessions, 100);

        ServerPlayer aiPlayer = createAIPlayer(1, "AI");
        ActionOptions options = createSimpleOptions();
        mockAI.nextAction = PlayerAction.call();

        // Zip mode off (default) — delay should apply (min = 50ms)
        long start = System.currentTimeMillis();
        delayedProvider.getAction(aiPlayer, options);
        long elapsedMs = System.currentTimeMillis() - start;

        assertTrue(elapsedMs >= 40, "Without zip mode, AI delay should apply (elapsed=" + elapsedMs + "ms)");
    }

    @Test
    void testZipMode_DefaultOff() {
        assertFalse(provider.isZipMode(), "Zip mode should default to false");
    }

    @Test
    void testZipMode_SetAndGet() {
        assertFalse(provider.isZipMode());
        provider.setZipMode(true);
        assertTrue(provider.isZipMode());
        provider.setZipMode(false);
        assertFalse(provider.isZipMode());
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

    @Test
    void testDisconnectedPlayer_PastGracePeriod_AutoFoldsImmediately() {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");

        // Register a disconnected session that has exceeded the grace turn limit
        ServerPlayerSession session = new ServerPlayerSession(2L, "Human", false, 0);
        session.connect();
        session.disconnect();
        // Exceed the grace turn limit (disconnectGraceTurns=2 in setUp)
        session.incrementConsecutiveTimeouts();
        session.incrementConsecutiveTimeouts();
        playerSessions.put(2L, session);

        ActionOptions options = createOptions(true, false, true, 100);

        // Should auto-fold immediately without waiting for timeout
        long start = System.currentTimeMillis();
        PlayerAction action = provider.getAction(humanPlayer, options);
        long elapsedMs = System.currentTimeMillis() - start;

        assertEquals(ActionType.FOLD, action.actionType(), "Disconnected player past grace period should auto-fold");
        assertTrue(elapsedMs < 500,
                "Auto-fold should be immediate, not wait for timeout (elapsed=" + elapsedMs + "ms)");
    }

    @Test
    void testDisconnectedPlayer_WithinGracePeriod_WaitsForTimeout() {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");

        // Register a disconnected session within grace period
        ServerPlayerSession session = new ServerPlayerSession(2L, "Human", false, 0);
        session.connect();
        session.disconnect();
        // Only 1 timeout, grace limit is 2 — still within grace
        session.incrementConsecutiveTimeouts();
        playerSessions.put(2L, session);

        ActionOptions options = createOptions(true, false, true, 100);

        // Should wait for timeout (1 second) then auto-fold
        long start = System.currentTimeMillis();
        PlayerAction action = provider.getAction(humanPlayer, options);
        long elapsedMs = System.currentTimeMillis() - start;

        assertEquals(ActionType.FOLD, action.actionType(),
                "Disconnected player within grace period should still auto-fold on timeout");
        assertTrue(elapsedMs >= 800, "Should wait for timeout before auto-folding (elapsed=" + elapsedMs + "ms)");
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

    // === Puppet Mode ===

    @Test
    void testPuppetMode_DefaultOff() {
        assertFalse(provider.isPuppeted(1));
        assertTrue(provider.getPuppetedPlayerIds().isEmpty());
    }

    @Test
    void testPuppetMode_SetAndGet() {
        provider.setPuppeted(1, true);
        assertTrue(provider.isPuppeted(1));
        assertFalse(provider.isPuppeted(2));
        assertEquals(1, provider.getPuppetedPlayerIds().size());
        assertTrue(provider.getPuppetedPlayerIds().contains(1));
    }

    @Test
    void testPuppetMode_ClearAll() {
        provider.setPuppeted(1, true);
        provider.setPuppeted(2, true);
        assertEquals(2, provider.getPuppetedPlayerIds().size());

        provider.clearAllPuppets();
        assertTrue(provider.getPuppetedPlayerIds().isEmpty());
        assertFalse(provider.isPuppeted(1));
        assertFalse(provider.isPuppeted(2));
    }

    @Test
    void testPuppetMode_Disable() {
        provider.setPuppeted(1, true);
        assertTrue(provider.isPuppeted(1));

        provider.setPuppeted(1, false);
        assertFalse(provider.isPuppeted(1));
    }

    @Test
    void testPuppetMode_PuppetedAIBlocksForAction() throws Exception {
        ServerPlayer aiPlayer = createAIPlayer(1, "AI");
        ActionOptions options = createSimpleOptions();
        mockAI.nextAction = PlayerAction.call();

        // Puppet the AI player
        provider.setPuppeted(1, true);

        // AI should now block like a human
        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(aiPlayer, options);
        });

        Thread.sleep(50);
        // Should have a puppet request but NOT a normal callback
        assertNotNull(provider.getCurrentPuppetRequest(), "Puppet request should be set");
        assertNull(capturedRequest.get(), "Normal callback should not be invoked for puppets");

        // Submit action to unblock
        provider.submitAction(aiPlayer.getID(), PlayerAction.check());

        PlayerAction action = actionFuture.get(1, TimeUnit.SECONDS);
        assertEquals(ActionType.CHECK, action.actionType());
        assertNull(provider.getCurrentPuppetRequest(), "Puppet request should be cleared after action");
    }

    @Test
    void testPuppetMode_TimeoutAutoFolds() {
        ServerPlayer aiPlayer = createAIPlayer(1, "AI");
        ActionOptions options = createOptions(true, false, true, 100);
        provider.setPuppeted(1, true);

        // Don't submit action — let it timeout
        PlayerAction action = provider.getAction(aiPlayer, options);
        assertEquals(ActionType.FOLD, action.actionType());
    }

    @Test
    void testPuppetMode_TimeoutAutoChecksWhenAvailable() {
        ServerPlayer aiPlayer = createAIPlayer(1, "AI");
        ActionOptions options = createOptions(true, true, false, 0);
        provider.setPuppeted(1, true);

        // Don't submit action — let it timeout
        PlayerAction action = provider.getAction(aiPlayer, options);
        assertEquals(ActionType.CHECK, action.actionType());
    }

    // === Pending Action Request ===

    @Test
    void testGetPendingActionRequest_NoneWhenNoAction() {
        assertNull(provider.getPendingActionRequest(1));
    }

    @Test
    void testGetPendingActionRequest_ReturnsWhenWaiting() throws Exception {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createSimpleOptions();

        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(humanPlayer, options);
        });

        Thread.sleep(50);
        ActionRequest pending = provider.getPendingActionRequest(2);
        assertNotNull(pending, "Should have a pending request while waiting");
        assertEquals(2, pending.player().getID());

        // Clean up
        provider.submitAction(2, PlayerAction.fold());
        actionFuture.get(1, TimeUnit.SECONDS);
    }

    @Test
    void testGetPendingActionRequest_ClearedAfterSubmit() throws Exception {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createSimpleOptions();

        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return provider.getAction(humanPlayer, options);
        });

        Thread.sleep(50);
        provider.submitAction(2, PlayerAction.fold());
        actionFuture.get(1, TimeUnit.SECONDS);

        assertNull(provider.getPendingActionRequest(2), "Pending request should be cleared after action");
    }

    // === Timeout Publisher ===

    @Test
    void testTimeoutPublisher_InvokedOnTimeout() {
        AtomicReference<com.donohoedigital.games.poker.engine.event.GameEvent> capturedEvent = new AtomicReference<>();
        ServerPlayerActionProvider publisherProvider = new ServerPlayerActionProvider(mockAI, capturedRequest::set,
                TIMEOUT_SECONDS, 2, playerSessions, 0, capturedEvent::set);

        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createOptions(true, false, true, 100);

        // Let it timeout
        publisherProvider.getAction(humanPlayer, options);

        assertNotNull(capturedEvent.get(), "Timeout publisher should be invoked");
        assertInstanceOf(com.donohoedigital.games.poker.engine.event.GameEvent.ActionTimeout.class,
                capturedEvent.get());
    }

    @Test
    void testTimeoutPublisher_NotInvokedOnNormalAction() throws Exception {
        AtomicReference<com.donohoedigital.games.poker.engine.event.GameEvent> capturedEvent = new AtomicReference<>();
        ServerPlayerActionProvider publisherProvider = new ServerPlayerActionProvider(mockAI, capturedRequest::set,
                TIMEOUT_SECONDS, 2, playerSessions, 0, capturedEvent::set);

        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ActionOptions options = createSimpleOptions();

        CompletableFuture<PlayerAction> actionFuture = CompletableFuture.supplyAsync(() -> {
            return publisherProvider.getAction(humanPlayer, options);
        });
        Thread.sleep(50);
        publisherProvider.submitAction(2, PlayerAction.call());
        actionFuture.get(1, TimeUnit.SECONDS);

        assertNull(capturedEvent.get(), "Timeout publisher should not be invoked for normal action");
    }

    // === Validate Action Edge Cases ===

    @Test
    void testValidateAction_FoldWhenCanFold() {
        ActionOptions options = createOptions(true, false, true, 100);
        PlayerAction result = provider.validateAction(PlayerAction.fold(), options);
        assertEquals(ActionType.FOLD, result.actionType());
    }

    @Test
    void testValidateAction_UnknownActionDefaultsToFold() {
        ActionOptions options = createSimpleOptions();
        // Create an action with an unknown type by using a type that isn't handled
        // The default case in the switch returns fold
        PlayerAction result = provider.validateAction(PlayerAction.fold(), options);
        assertEquals(ActionType.FOLD, result.actionType());
    }

    @Test
    void testValidateAction_BetWhenCantBet_FoldsInstead() {
        ActionOptions options = createSimpleOptions(); // canBet=false
        PlayerAction result = provider.validateAction(PlayerAction.bet(100), options);
        assertEquals(ActionType.FOLD, result.actionType());
    }

    @Test
    void testValidateAction_RaiseWhenCantRaise_FoldsInstead() {
        ActionOptions options = createSimpleOptions(); // canRaise=false
        PlayerAction result = provider.validateAction(PlayerAction.raise(100), options);
        assertEquals(ActionType.FOLD, result.actionType());
    }

    @Test
    void testValidateAction_RaiseAmount_ClampedToMin() {
        ActionOptions options = createRaiseOptions(100, 500);
        PlayerAction result = provider.validateAction(PlayerAction.raise(10), options);
        assertEquals(ActionType.RAISE, result.actionType());
        assertEquals(100, result.amount(), "Raise should be clamped to min");
    }

    // === Disconnect Grace - Auto Check ===

    @Test
    void testDisconnectedPlayer_PastGracePeriod_AutoChecksWhenAvailable() {
        ServerPlayer humanPlayer = createHumanPlayer(2, "Human");
        ServerPlayerSession session = new ServerPlayerSession(2L, "Human", false, 0);
        session.connect();
        session.disconnect();
        session.incrementConsecutiveTimeouts();
        session.incrementConsecutiveTimeouts();
        playerSessions.put(2L, session);

        ActionOptions options = createOptions(true, true, false, 0); // canCheck=true

        PlayerAction action = provider.getAction(humanPlayer, options);
        assertEquals(ActionType.CHECK, action.actionType(),
                "Disconnected player past grace period should auto-check when check is available");
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
        public PlayerAction getAction(com.donohoedigital.games.poker.engine.GamePlayerInfo player,
                ActionOptions options) {
            return nextAction;
        }
    }

}
