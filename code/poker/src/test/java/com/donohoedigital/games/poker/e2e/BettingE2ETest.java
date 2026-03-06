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
package com.donohoedigital.games.poker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E tests for betting scenarios not covered by existing tests: RAISE with an
 * amount, CHECK in CHECK_RAISE mode, BET in CHECK_BET mode, game advancement
 * after FOLD, and playing a complete hand with active betting (raise/call).
 *
 * <p>
 * Existing coverage in other E2E test classes:
 * <ul>
 * <li>{@code ActionValidationE2ETest}: FOLD acceptance, CHECK in CHECK_BET,
 * CALL in CALL_RAISE</li>
 * <li>{@code GameLifecycleE2ETest}: FOLD, ALL_IN, hand advancement, complete
 * hand via fold</li>
 * <li>{@code ChipConservationE2ETest}: complete hand via check/call, complete
 * hand via fold, ALL_IN</li>
 * </ul>
 *
 * <p>
 * Tests are tagged {@code e2e} and {@code slow} (inherited from
 * {@link ControlServerTestBase}) and require a live DDPoker subprocess.
 */
class BettingE2ETest extends ControlServerTestBase {

    /**
     * Starts a fresh 3-player game before each test and waits for DEAL mode.
     */
    @BeforeEach
    void startFreshGame() throws Exception {
        client().startGame(3);
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
    }

    // -------------------------------------------------------------------------
    // Test 1: RAISE with an amount is accepted in CALL_RAISE mode
    // -------------------------------------------------------------------------

    @Test
    void should_AcceptRaise_When_ModeIsCallRaise() throws Exception {
        playUntilMode(Duration.ofSeconds(60), "CALL_RAISE");

        // Get state to determine a valid raise amount (must be at least the big blind)
        JsonNode state = client().getState();
        int raiseAmount = state.path("currentAction").path("minRaise").asInt(0);
        if (raiseAmount <= 0) {
            // Fallback: use a reasonable raise amount if minRaise is not exposed
            raiseAmount = 40;
        }

        JsonNode result = client().submitAction("RAISE", raiseAmount);
        assertTrue(result.path("accepted").asBoolean(false),
                "RAISE with amount should be accepted in CALL_RAISE mode; response: " + result);
    }

    // -------------------------------------------------------------------------
    // Test 2: BET with an amount is accepted in CHECK_BET mode
    // -------------------------------------------------------------------------

    @Test
    void should_AcceptBet_When_ModeIsCheckBet() throws Exception {
        playUntilMode(Duration.ofSeconds(60), "CHECK_BET");

        // Get state to determine a valid bet amount
        JsonNode state = client().getState();
        int betAmount = state.path("currentAction").path("minRaise").asInt(0);
        if (betAmount <= 0) {
            // Fallback: use a reasonable bet amount
            betAmount = 20;
        }

        JsonNode result = client().submitAction("BET", betAmount);
        assertTrue(result.path("accepted").asBoolean(false),
                "BET with amount should be accepted in CHECK_BET mode; response: " + result);
    }

    // -------------------------------------------------------------------------
    // Test 3: CHECK is accepted in CHECK_RAISE mode
    // -------------------------------------------------------------------------

    @Test
    void should_AcceptCheck_When_ModeIsCheckRaise() throws Exception {
        playUntilMode(Duration.ofSeconds(60), "CHECK_RAISE");

        JsonNode result = client().submitAction("CHECK");
        assertTrue(result.path("accepted").asBoolean(false),
                "CHECK should be accepted in CHECK_RAISE mode; response: " + result);
    }

    // -------------------------------------------------------------------------
    // Test 4: Game advances to next hand after FOLD
    // -------------------------------------------------------------------------

    @Test
    void should_AdvanceToNextHand_When_HumanFoldsEveryStreet() throws Exception {
        JsonNode stateBefore = client().getState();
        int handBefore = stateBefore.path("handNumber").asInt(0);

        // Deal, fold on human's turn, wait for hand to complete
        client().submitAction("DEAL");
        playHandFolding(Duration.ofSeconds(60));
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");

        JsonNode stateAfter = client().getState();
        int handAfter = stateAfter.path("handNumber").asInt(0);

        assertTrue(handAfter > handBefore,
                "Hand number should advance after folding: before=" + handBefore + " after=" + handAfter);
    }

    // -------------------------------------------------------------------------
    // Test 5: Playing a complete hand with raise/call through all streets
    // -------------------------------------------------------------------------

    @Test
    void should_CompleteHand_When_RaisingAndCallingThroughStreets() throws Exception {
        JsonNode stateBefore = client().getState();
        int handBefore = stateBefore.path("handNumber").asInt(0);

        // Deal and play a hand using raise/call strategy
        client().submitAction("DEAL");
        playHandRaiseCall(Duration.ofSeconds(60));

        // Wait for next DEAL mode (hand complete)
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");

        // Verify the hand advanced
        JsonNode stateAfter = client().getState();
        int handAfter = stateAfter.path("handNumber").asInt(0);
        assertTrue(handAfter > handBefore,
                "Hand number should advance after raise/call hand: before=" + handBefore + " after=" + handAfter);

        // Verify chip conservation
        JsonNode validation = client().validate();
        assertTrue(validation.path("chipConservation").path("valid").asBoolean(false),
                "chipConservation.valid must be true after a raise/call hand; response: " + validation);
    }

    // -------------------------------------------------------------------------
    // Test 6: RAISE is rejected in CHECK_BET mode (RAISE requires a prior bet)
    // -------------------------------------------------------------------------

    @Test
    void should_RejectRaise_When_ModeIsCheckBet() throws Exception {
        playUntilMode(Duration.ofSeconds(60), "CHECK_BET");

        // RAISE should be invalid in CHECK_BET — only CHECK or BET are valid
        assertThrows(AssertionError.class, () -> client().submitAction("RAISE", 40),
                "RAISE should be rejected in CHECK_BET mode (no prior bet to raise)");

        // Game should still be in CHECK_BET mode after the rejection
        JsonNode stateAfter = client().getState();
        assertEquals("CHECK_BET", stateAfter.path("inputMode").asText(),
                "After a rejected RAISE the game should remain in CHECK_BET mode");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Drives the game loop until the human's turn arrives in the specified target
     * mode. Submits DEAL, CONTINUE, and folds on non-target human betting turns.
     *
     * @param timeout
     *            maximum time to wait
     * @param targetMode
     *            the exact input mode to wait for (e.g. "CALL_RAISE",
     *            "CHECK_RAISE")
     * @throws AssertionError
     *             if the deadline elapses without reaching the target mode
     */
    private void playUntilMode(Duration timeout, String targetMode) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            JsonNode state = client().getState();
            String mode = state.path("inputMode").asText("");
            boolean isHumanTurn = state.path("currentAction").path("isHumanTurn").asBoolean(false);

            if (targetMode.equals(mode) && isHumanTurn) {
                return;
            }

            switch (mode) {
                case "DEAL" -> client().submitAction("DEAL");
                case "CONTINUE", "CONTINUE_LOWER" -> client().submitAction(mode);
                case "CHECK_BET", "CHECK_RAISE", "CALL_RAISE" -> {
                    if (isHumanTurn) {
                        // Not our target mode — fold and try again next hand
                        client().submitAction("FOLD");
                    } else {
                        Thread.sleep(200);
                    }
                }
                case "REBUY_CHECK" -> client().submitAction("DECLINE_REBUY");
                default -> Thread.sleep(200);
            }
        }
        throw new AssertionError("Timed out after " + timeout + " waiting for human turn in " + targetMode + " mode");
    }

    /**
     * Plays through the current hand by folding on every human betting turn and
     * advancing past CONTINUE/CONTINUE_LOWER pauses, until DEAL mode is reached
     * (hand complete) or the deadline elapses.
     */
    private void playHandFolding(Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            JsonNode state = client().getState();
            String mode = state.path("inputMode").asText("");
            switch (mode) {
                case "DEAL" -> {
                    return;
                }
                case "CONTINUE", "CONTINUE_LOWER" -> client().submitAction(mode);
                case "CHECK_BET", "CHECK_RAISE", "CALL_RAISE" -> client().submitAction("FOLD");
                case "REBUY_CHECK" -> client().submitAction("DECLINE_REBUY");
                default -> Thread.sleep(200);
            }
        }
        throw new AssertionError("Timed out after " + timeout + " waiting for hand to complete");
    }

    /**
     * Plays through the current hand by raising (or betting) on the first human
     * turn encountered, then calling on subsequent turns. This exercises the
     * raise/call code path through multiple streets. Falls back to check/call if
     * raise/bet is rejected.
     */
    private void playHandRaiseCall(Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        boolean hasRaised = false;
        while (System.currentTimeMillis() < deadline) {
            JsonNode state = client().getState();
            String mode = state.path("inputMode").asText("");
            switch (mode) {
                case "DEAL" -> {
                    return;
                }
                case "CONTINUE", "CONTINUE_LOWER" -> client().submitAction(mode);
                case "CHECK_BET" -> {
                    if (!hasRaised) {
                        // Try to bet; fall back to check if rejected
                        try {
                            int betAmount = state.path("currentAction").path("minRaise").asInt(20);
                            if (betAmount <= 0)
                                betAmount = 20;
                            client().submitAction("BET", betAmount);
                            hasRaised = true;
                        } catch (AssertionError e) {
                            client().submitAction("CHECK");
                        }
                    } else {
                        client().submitAction("CHECK");
                    }
                }
                case "CALL_RAISE" -> {
                    if (!hasRaised) {
                        // Try to raise; fall back to call if rejected
                        try {
                            int raiseAmount = state.path("currentAction").path("minRaise").asInt(40);
                            if (raiseAmount <= 0)
                                raiseAmount = 40;
                            client().submitAction("RAISE", raiseAmount);
                            hasRaised = true;
                        } catch (AssertionError e) {
                            client().submitAction("CALL");
                        }
                    } else {
                        client().submitAction("CALL");
                    }
                }
                case "CHECK_RAISE" -> client().submitAction("CHECK");
                case "REBUY_CHECK" -> client().submitAction("DECLINE_REBUY");
                default -> Thread.sleep(200);
            }
        }
        throw new AssertionError("Timed out after " + timeout + " waiting for hand to complete");
    }
}
