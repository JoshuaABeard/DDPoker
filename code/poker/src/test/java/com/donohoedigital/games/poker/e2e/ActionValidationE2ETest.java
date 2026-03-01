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
 * E2E tests for action validation: the server accepts valid actions and rejects
 * invalid ones based on the current input mode.
 *
 * <p>
 * Key design note: {@link ControlServerClient#submitAction(String)} calls
 * {@code requireSuccess()} internally and throws {@link AssertionError} on any
 * non-2xx response. To test rejection (HTTP 409), we assert that an
 * {@link AssertionError} is thrown when the wrong action is submitted for the
 * current mode. For acceptance tests we simply assert {@code accepted: true}.
 *
 * <p>
 * Tests are tagged {@code e2e} and {@code slow} (inherited from
 * {@link ControlServerTestBase}) and require a live DDPoker subprocess.
 */
class ActionValidationE2ETest extends ControlServerTestBase {

    /**
     * Starts a fresh 3-player game before each test and waits for DEAL mode.
     */
    @BeforeEach
    void startFreshGame() throws Exception {
        client().startGame(3);
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
    }

    // -------------------------------------------------------------------------
    // Test 1: Submitting FOLD in DEAL mode is rejected (HTTP 409)
    // -------------------------------------------------------------------------

    @Test
    void should_RejectFold_When_ModeIsDeal() throws Exception {
        // Confirm we are in DEAL mode
        JsonNode state = client().getState();
        assertEquals("DEAL", state.path("inputMode").asText(),
                "Expected DEAL mode before the test; got: " + state.path("inputMode").asText());

        // submitAction() calls requireSuccess() and throws AssertionError on 4xx
        assertThrows(AssertionError.class, () -> client().submitAction("FOLD"),
                "Submitting FOLD in DEAL mode should be rejected (HTTP 409) and throw AssertionError");

        // Game must still be in DEAL mode — the rejection must not advance the state
        JsonNode stateAfter = client().getState();
        assertEquals("DEAL", stateAfter.path("inputMode").asText(),
                "After a rejected FOLD the game should remain in DEAL mode");
    }

    // -------------------------------------------------------------------------
    // Test 2: Submitting DEAL in DEAL mode is accepted
    // -------------------------------------------------------------------------

    @Test
    void should_AcceptDeal_When_ModeIsDeal() throws Exception {
        JsonNode result = client().submitAction("DEAL");
        assertTrue(result.path("accepted").asBoolean(false),
                "DEAL action should be accepted in DEAL mode; response: " + result);
    }

    // -------------------------------------------------------------------------
    // Test 3: FOLD is accepted when it is the human's betting turn
    // -------------------------------------------------------------------------

    @Test
    void should_AcceptFold_When_HumanBettingTurnStarts() throws Exception {
        playUntilHumanBettingTurn(Duration.ofSeconds(30));

        JsonNode result = client().submitAction("FOLD");
        assertTrue(result.path("accepted").asBoolean(false),
                "FOLD should be accepted when it is the human's betting turn; response: " + result);
    }

    // -------------------------------------------------------------------------
    // Test 4: CALL is accepted in CALL_RAISE mode
    // -------------------------------------------------------------------------

    @Test
    void should_AcceptCall_When_ModeIsCallRaise() throws Exception {
        // Drive the game until we hit CALL_RAISE mode on the human's turn
        playUntilMode(Duration.ofSeconds(60), "CALL_RAISE");

        JsonNode result = client().submitAction("CALL");
        assertTrue(result.path("accepted").asBoolean(false),
                "CALL should be accepted in CALL_RAISE mode; response: " + result);
    }

    // -------------------------------------------------------------------------
    // Test 5: CHECK is accepted in CHECK_BET mode
    // -------------------------------------------------------------------------

    @Test
    void should_AcceptCheck_When_ModeIsCheckBet() throws Exception {
        // Drive the game until we hit CHECK_BET mode on the human's turn
        playUntilMode(Duration.ofSeconds(60), "CHECK_BET");

        JsonNode result = client().submitAction("CHECK");
        assertTrue(result.path("accepted").asBoolean(false),
                "CHECK should be accepted in CHECK_BET mode; response: " + result);
    }

    // -------------------------------------------------------------------------
    // Test 6: availableActions is non-empty when it is the human's betting turn
    // -------------------------------------------------------------------------

    @Test
    void should_MatchStateToAvailableActions_When_InBettingMode() throws Exception {
        playUntilHumanBettingTurn(Duration.ofSeconds(30));

        JsonNode state = client().getState();
        String mode = state.path("inputMode").asText("");
        assertTrue(isBettingMode(mode), "Expected a betting input mode but got: " + mode);

        JsonNode availableActions = state.path("currentAction").path("availableActions");
        assertFalse(availableActions.isEmpty(),
                "availableActions must be non-empty in a betting mode; state: " + state);

        // FOLD must always be in the available actions for every betting mode
        boolean foldPresent = false;
        for (JsonNode action : availableActions) {
            if ("FOLD".equals(action.asText())) {
                foldPresent = true;
                break;
            }
        }
        assertTrue(foldPresent,
                "FOLD should always be available in a betting mode; availableActions: " + availableActions);
    }

    // -------------------------------------------------------------------------
    // Test 7: CONTINUE is accepted in CONTINUE mode
    // -------------------------------------------------------------------------

    @Test
    void should_AcceptContinue_When_ModeIsContinue() throws Exception {
        // Deal the hand first, then wait for CONTINUE mode (end of a betting street)
        client().submitAction("DEAL");
        JsonNode state = client().waitForInputMode(Duration.ofSeconds(30), "CONTINUE", "CONTINUE_LOWER");
        String mode = state.path("inputMode").asText("");

        JsonNode result = client().submitAction(mode);
        assertTrue(result.path("accepted").asBoolean(false),
                mode + " action should be accepted in " + mode + " mode; response: " + result);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Drives the game loop — submitting DEAL, CONTINUE, and waiting out AI turns —
     * until the input mode is the specified target mode and
     * {@code currentAction.isHumanTurn} is {@code true}. Folds on any other human
     * betting mode encountered along the way.
     *
     * @param timeout
     *            maximum time to wait
     * @param targetMode
     *            the exact input mode to wait for (e.g. "CALL_RAISE", "CHECK_BET")
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
     * Drives the game loop until the human's betting turn arrives (any betting mode
     * with isHumanTurn == true).
     */
    private void playUntilHumanBettingTurn(Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            JsonNode state = client().getState();
            String mode = state.path("inputMode").asText("");
            boolean isHumanTurn = state.path("currentAction").path("isHumanTurn").asBoolean(false);

            if (isBettingMode(mode) && isHumanTurn) {
                return;
            }

            switch (mode) {
                case "DEAL" -> client().submitAction("DEAL");
                case "CONTINUE", "CONTINUE_LOWER" -> client().submitAction(mode);
                case "REBUY_CHECK" -> client().submitAction("DECLINE_REBUY");
                default -> Thread.sleep(200);
            }
        }
        throw new AssertionError("Timed out after " + timeout + " waiting for human betting turn");
    }

    /**
     * Returns {@code true} if {@code mode} is one of the three human betting modes.
     */
    private boolean isBettingMode(String mode) {
        return "CHECK_BET".equals(mode) || "CHECK_RAISE".equals(mode) || "CALL_RAISE".equals(mode);
    }
}
