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
 * E2E tests for the game lifecycle: start → deal → play through streets → hand
 * ends.
 *
 * <p>
 * These tests drive a live DDPoker subprocess via the Dev Control Server HTTP
 * API. They require a built fat JAR at
 * {@code code/poker/target/DDPokerCE-3.3.0.jar}. The base class
 * {@link ControlServerTestBase} manages process startup and teardown.
 *
 * <p>
 * Tests are tagged {@code e2e} and {@code slow} (inherited from base class) so
 * they do not run during normal {@code mvn test}. Build with {@code -P dev} and
 * run explicitly with {@code -Dgroups=e2e} when a display is available.
 */
class GameLifecycleE2ETest extends ControlServerTestBase {

    /**
     * Starts a fresh 3-player game before each test and waits for the first DEAL
     * mode so every test begins from a consistent, known-good state.
     */
    @BeforeEach
    void startFreshGame() throws Exception {
        client().startGame(3);
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
    }

    // -------------------------------------------------------------------------
    // Test 1: Game starts and reaches DEAL mode
    // -------------------------------------------------------------------------

    @Test
    void should_TransitionToDealMode_When_GameStarts() throws Exception {
        JsonNode state = client().getState();
        String mode = state.path("inputMode").asText();
        assertEquals("DEAL", mode, "After startGame(3) the game should reach DEAL mode");
    }

    // -------------------------------------------------------------------------
    // Test 2: Submitting DEAL action advances past the deal phase
    // -------------------------------------------------------------------------

    @Test
    void should_DealFirstHand_When_DealActionSubmitted() throws Exception {
        // We are already in DEAL mode from @BeforeEach
        client().submitAction("DEAL");

        // After dealing, the game must move to a mode other than DEAL
        // (betting round, CONTINUE, or back to DEAL for the next hand)
        JsonNode state = client().waitForInputMode(Duration.ofSeconds(15), "CHECK_BET", "CHECK_RAISE", "CALL_RAISE",
                "CONTINUE", "CONTINUE_LOWER", "DEAL", "QUITSAVE", "NONE");
        String mode = state.path("inputMode").asText();
        assertNotEquals("DEAL", mode,
                "After submitting DEAL the game should advance past DEAL mode, but stayed in DEAL");
    }

    // -------------------------------------------------------------------------
    // Test 3: Human can fold on their turn
    // -------------------------------------------------------------------------

    @Test
    void should_AllowHumanToFold_When_ItIsHumanTurn() throws Exception {
        // Play until the human must act
        playUntilHumanBettingTurn(Duration.ofSeconds(30));

        // Verify it really is a betting mode before folding
        JsonNode state = client().getState();
        String mode = state.path("inputMode").asText();
        assertTrue(isBettingMode(mode), "Expected a betting mode before fold but got: " + mode);

        JsonNode result = client().submitAction("FOLD");
        assertTrue(result.path("accepted").asBoolean(false), "FOLD should be accepted; response: " + result);
    }

    // -------------------------------------------------------------------------
    // Test 4: Playing one complete hand advances the hand number
    // -------------------------------------------------------------------------

    @Test
    void should_AdvanceThroughStreets_When_ContinueSubmitted() throws Exception {
        JsonNode stateBefore = client().getState();
        int handNumberBefore = stateBefore.path("handNumber").asInt(0);

        // Play one complete hand by folding every time it is our turn
        playHand(Duration.ofSeconds(60));

        // After playing one hand, we expect to be back in DEAL mode
        JsonNode stateAfter = client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
        int handNumberAfter = stateAfter.path("handNumber").asInt(0);

        assertTrue(handNumberAfter > handNumberBefore, "Hand number should advance after playing a hand: before="
                + handNumberBefore + " after=" + handNumberAfter);
    }

    // -------------------------------------------------------------------------
    // Test 5: Validation passes while game is running
    // -------------------------------------------------------------------------

    @Test
    void should_PassValidation_When_GameIsRunning() throws Exception {
        JsonNode validation = client().validate();
        boolean valid = validation.path("chipConservation").path("valid").asBoolean(false);
        assertTrue(valid, "chipConservation.valid should be true while game is running; response: " + validation);
    }

    // -------------------------------------------------------------------------
    // Test 6: Chip conservation holds after one complete hand
    // -------------------------------------------------------------------------

    @Test
    void should_ConserveChips_After_OneHand() throws Exception {
        // Play one complete hand
        playHand(Duration.ofSeconds(60));

        // Wait for DEAL mode (next hand ready)
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");

        JsonNode validation = client().validate();
        boolean valid = validation.path("chipConservation").path("valid").asBoolean(false);
        assertTrue(valid, "chipConservation.valid should be true after one complete hand; response: " + validation);
    }

    // -------------------------------------------------------------------------
    // Test 7: Available actions are present on human turn
    // -------------------------------------------------------------------------

    @Test
    void should_ShowAvailableActions_When_HumanTurnStarts() throws Exception {
        playUntilHumanBettingTurn(Duration.ofSeconds(30));

        JsonNode state = client().getState();
        JsonNode availableActions = state.path("currentAction").path("availableActions");

        assertFalse(availableActions.isEmpty(),
                "availableActions should be non-empty when it is the human's turn; state: " + state);
    }

    // -------------------------------------------------------------------------
    // Test 8: Human can go all-in
    // -------------------------------------------------------------------------

    @Test
    void should_HandleAllIn_When_HumanGoesAllIn() throws Exception {
        playUntilHumanBettingTurn(Duration.ofSeconds(30));

        JsonNode result = client().submitAction("ALL_IN");
        assertTrue(result.path("accepted").asBoolean(false),
                "ALL_IN should be accepted when it is the human's betting turn; response: " + result);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Plays through the current hand by submitting the appropriate action for each
     * input mode until the game returns to {@code DEAL} mode (hand complete) or the
     * deadline elapses.
     *
     * <p>
     * Strategy: fold whenever it is the human's turn to bet so hands complete
     * quickly. CONTINUE/CONTINUE_LOWER are submitted immediately. AI turns are
     * waited out with a short sleep.
     *
     * @param timeout
     *            maximum time to wait for the hand to complete
     */
    private void playHand(Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            JsonNode state = client().getState();
            String mode = state.path("inputMode").asText("");
            switch (mode) {
                case "DEAL" -> {
                    // Hand is complete and the next hand is ready — stop
                    return;
                }
                case "CONTINUE", "CONTINUE_LOWER" -> client().submitAction(mode);
                case "CHECK_BET", "CHECK_RAISE", "CALL_RAISE" -> client().submitAction("FOLD");
                case "REBUY_CHECK" -> client().submitAction("DECLINE_REBUY");
                case "NONE", "QUITSAVE" -> Thread.sleep(200);
                default -> Thread.sleep(200);
            }
        }
    }

    /**
     * Drives the game loop — submitting DEAL, CONTINUE, and waiting out AI turns —
     * until the input mode is one of the three betting modes and
     * {@code currentAction.isHumanTurn} is {@code true}.
     *
     * @param timeout
     *            maximum time to wait
     * @throws AssertionError
     *             if the deadline elapses without reaching a human betting turn
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
                case "NONE", "QUITSAVE" -> Thread.sleep(200);
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
