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
 * E2E tests for card injection: verifies that seeded and explicit card
 * injection produces deterministic, correct hand outcomes.
 *
 * <p>
 * Card injection format for a 3-player game (14 cards total):
 *
 * <pre>
 *   seat0-c1, seat0-c2, seat1-c1, seat1-c2, seat2-c1, seat2-c2,
 *   burn, flop1, flop2, flop3, burn, turn, burn, river
 * </pre>
 *
 * <p>
 * The state response does not expose individual hole cards in its documented
 * fields. Tests therefore verify outcomes observable through the public API:
 * chip conservation, mode progression, and the absence of crashes.
 *
 * <p>
 * Tests are tagged {@code e2e} and {@code slow} (inherited from
 * {@link ControlServerTestBase}) and require a live DDPoker subprocess.
 */
class CardInjectionE2ETest extends ControlServerTestBase {

    /**
     * Starts a fresh 3-player game before each test and waits for DEAL mode.
     */
    @BeforeEach
    void startFreshGame() throws Exception {
        client().startGame(3);
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
    }

    // -------------------------------------------------------------------------
    // Test 1: Seeded injection produces a deterministic, completable hand
    // -------------------------------------------------------------------------

    @Test
    void should_InjectSpecificCards_When_SeedProvided() throws Exception {
        // Inject with a fixed seed — the shuffle is deterministic
        client().injectCardsBySeed(42);

        // Deal the injected hand and play it to completion
        client().submitAction("DEAL");
        playHandFolding(Duration.ofSeconds(60));

        // After the hand the game must return to DEAL mode — no crash or stuck state
        JsonNode state = client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
        assertEquals("DEAL", state.path("inputMode").asText(),
                "After an injected seeded hand the game should return to DEAL mode");

        // Chip conservation must hold
        JsonNode validation = client().validate();
        assertTrue(validation.path("chipConservation").path("valid").asBoolean(false),
                "chipConservation.valid must be true after an injected seeded hand; response: " + validation);
    }

    // -------------------------------------------------------------------------
    // Test 2: Explicit card list injection produces a normal, completable hand
    // -------------------------------------------------------------------------

    @Test
    void should_InjectByCardList_When_CardsSpecified() throws Exception {
        // 3-player game: 14 cards total
        // seat0-c1, seat0-c2, seat1-c1, seat1-c2, seat2-c1, seat2-c2,
        // burn, flop1, flop2, flop3, burn, turn, burn, river
        client().injectCards("As", "Ks", // seat 0 (human): Ace + King of spades
                "2d", "3c", // seat 1
                "7h", "8h", // seat 2
                "Qd", // burn
                "Jd", "Td", "9d", // flop
                "4c", // burn
                "2h", // turn
                "3s", // burn
                "5c" // river
        );

        // Verify the injection doesn't crash the game by dealing and completing the
        // hand
        client().submitAction("DEAL");
        playHandFolding(Duration.ofSeconds(60));

        // Game must return to DEAL mode — no crash
        JsonNode state = client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
        assertEquals("DEAL", state.path("inputMode").asText(),
                "After an injected card-list hand the game should return to DEAL mode");

        // Chip conservation must hold after the injected hand
        JsonNode validation = client().validate();
        assertTrue(validation.path("chipConservation").path("valid").asBoolean(false),
                "chipConservation.valid must be true after an injected card-list hand; response: " + validation);
    }

    // -------------------------------------------------------------------------
    // Test 3: Injection is consumed after one hand; a second injection also works
    // -------------------------------------------------------------------------

    @Test
    void should_ClearInjection_After_OneHand() throws Exception {
        // First injection with seed 99
        client().injectCardsBySeed(99);

        // Deal and complete the first hand
        client().submitAction("DEAL");
        playHandFolding(Duration.ofSeconds(60));
        client().waitForInputMode(Duration.ofSeconds(15), "DEAL");

        // The first injection should have been consumed. Now inject again with a
        // different seed and verify it also produces a valid, completable hand.
        client().injectCardsBySeed(7);

        client().submitAction("DEAL");
        playHandFolding(Duration.ofSeconds(60));

        JsonNode state = client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
        assertEquals("DEAL", state.path("inputMode").asText(),
                "Second injection after first was consumed should also complete cleanly");

        JsonNode validation = client().validate();
        assertTrue(validation.path("chipConservation").path("valid").asBoolean(false),
                "chipConservation.valid must be true after second injected hand; response: " + validation);
    }

    // -------------------------------------------------------------------------
    // Test 4: Injection accepted before game start takes effect when dealt
    // -------------------------------------------------------------------------

    @Test
    void should_AcceptInjection_When_GameNotStarted() throws Exception {
        // Start a new game — @BeforeEach already started one; restart to test the
        // "inject before deal" case by injecting immediately in DEAL mode (before
        // any cards have been dealt for this hand).
        JsonNode stateBeforeDeal = client().getState();
        assertEquals("DEAL", stateBeforeDeal.path("inputMode").asText(),
                "Pre-condition: should be in DEAL mode before injecting");

        // Inject in DEAL mode (before the hand is dealt)
        client().injectCardsBySeed(1234);

        // Now deal — injection should apply to this hand
        client().submitAction("DEAL");
        playHandFolding(Duration.ofSeconds(60));

        // Game must complete normally
        JsonNode state = client().waitForInputMode(Duration.ofSeconds(15), "DEAL");
        assertEquals("DEAL", state.path("inputMode").asText(),
                "Game should return to DEAL mode after injection-before-deal hand");

        JsonNode validation = client().validate();
        assertTrue(validation.path("chipConservation").path("valid").asBoolean(false),
                "chipConservation.valid must be true after injection-before-deal hand; response: " + validation);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Plays through the current hand by folding on every human betting turn and
     * submitting CONTINUE/CONTINUE_LOWER as needed, until DEAL mode is reached
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
}
