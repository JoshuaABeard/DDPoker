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
 * E2E tests for chip conservation: verifies that {@code playerChips + inPot}
 * always equals {@code buyinPerPlayer * numPlayers} throughout gameplay.
 *
 * <p>
 * Chip conservation is checked via the {@code /validate} endpoint which
 * returns:
 *
 * <pre>
 * {
 *   "chipConservation": {
 *     "valid": true,
 *     "tables": [{"playerChips": N, "inPot": M, "total": T, "expectedTotal": E}]
 *   }
 * }
 * </pre>
 *
 * <p>
 * Tests are tagged {@code e2e} and {@code slow} (inherited from
 * {@link ControlServerTestBase}) and require a live DDPoker subprocess.
 */
class ChipConservationE2ETest extends ControlServerTestBase {

    /**
     * Starts a fresh 3-player game before each test and waits for DEAL mode.
     */
    @BeforeEach
    void startFreshGame() throws Exception {
        client().startGame(3);
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
    }

    // -------------------------------------------------------------------------
    // Test 1: Chips are conserved immediately after game starts (before any action)
    // -------------------------------------------------------------------------

    @Test
    void should_ConserveChips_When_GameStarts() throws Exception {
        JsonNode validation = client().validate();
        assertTrue(validation.path("chipConservation").path("valid").asBoolean(false),
                "chipConservation.valid must be true immediately after game starts; response: " + validation);
    }

    // -------------------------------------------------------------------------
    // Test 2: Chips are conserved after each of 3 complete hands
    // -------------------------------------------------------------------------

    @Test
    void should_ConserveChips_After_EachHand() throws Exception {
        for (int hand = 1; hand <= 3; hand++) {
            // Deal and play the hand
            client().submitAction("DEAL");
            playHandFolding(Duration.ofSeconds(60));

            // Wait for the next DEAL mode (hand complete)
            client().waitForInputMode(Duration.ofSeconds(15), "DEAL");

            // Validate chip conservation after this hand
            JsonNode validation = client().validate();
            assertTrue(validation.path("chipConservation").path("valid").asBoolean(false),
                    "chipConservation.valid must be true after hand " + hand + "; response: " + validation);
        }
    }

    // -------------------------------------------------------------------------
    // Test 3: Chips are conserved when the human folds every hand
    // -------------------------------------------------------------------------

    @Test
    void should_ConserveChips_When_PlayerFoldsEveryHand() throws Exception {
        for (int hand = 1; hand <= 3; hand++) {
            // Deal the hand, then fold on our first human turn
            client().submitAction("DEAL");
            playHandFolding(Duration.ofSeconds(60));

            // Wait for the next DEAL mode (hand complete)
            client().waitForInputMode(Duration.ofSeconds(15), "DEAL");

            JsonNode validation = client().validate();
            assertTrue(validation.path("chipConservation").path("valid").asBoolean(false),
                    "chipConservation.valid must be true after folding on hand " + hand + "; response: " + validation);
        }
    }

    // -------------------------------------------------------------------------
    // Test 4: Chips are conserved when the human goes all-in
    // -------------------------------------------------------------------------

    @Test
    void should_ConserveChips_When_AllInOccurs() throws Exception {
        // Deal the hand and drive to the human's first betting turn
        client().submitAction("DEAL");
        playUntilHumanBettingTurn(Duration.ofSeconds(30));

        // Go all-in
        JsonNode result = client().submitAction("ALL_IN");
        assertTrue(result.path("accepted").asBoolean(false),
                "ALL_IN should be accepted on the human's betting turn; response: " + result);

        // Play out the rest of the hand (no more human actions after ALL_IN until next
        // hand)
        playHandFolding(Duration.ofSeconds(60));
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");

        JsonNode validation = client().validate();
        assertTrue(validation.path("chipConservation").path("valid").asBoolean(false),
                "chipConservation.valid must be true after an all-in hand; response: " + validation);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Plays through the current hand by folding on every human betting turn and
     * advancing past CONTINUE/CONTINUE_LOWER pauses, until DEAL mode is reached
     * (hand complete) or the deadline elapses.
     *
     * @param timeout
     *            maximum time to wait for the hand to complete
     */
    private void playHandFolding(Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            JsonNode state = client().getState();
            String mode = state.path("inputMode").asText("");
            switch (mode) {
                case "DEAL" -> {
                    // Hand is complete — stop
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
     * Drives the game loop — dealing and advancing past CONTINUE pauses — until the
     * human's betting turn arrives (any betting mode with
     * {@code isHumanTurn == true}).
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
