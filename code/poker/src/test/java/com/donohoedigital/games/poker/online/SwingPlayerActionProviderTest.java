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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.HandAction;
import com.donohoedigital.games.poker.HoldemHand;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.ai.PokerAI;
import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.state.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SwingPlayerActionProvider. Tests AI action conversion from
 * HandAction to PlayerAction.
 *
 * NOTE: Human action testing (getHumanAction) is not covered here because it
 * requires: - Full Swing EDT setup with PokerGame, PokerTable, HoldemHand -
 * Mock UI components (PokerTableInput) - CountDownLatch blocking behavior
 * (difficult to test in unit tests) Human actions are tested implicitly through
 * integration/E2E tests.
 */
class SwingPlayerActionProviderTest {

    private SwingPlayerActionProvider provider;
    private PokerPlayer testPlayer;
    private TestAI testAI;
    private ActionOptions testOptions;

    /**
     * Test double for PokerAI. Allows configuring what HandAction to return.
     */
    private static class TestAI extends PokerAI {
        private HandAction actionToReturn;
        private boolean wasGetHandActionCalled = false;
        private Boolean quickParamReceived;

        void setActionToReturn(HandAction action) {
            this.actionToReturn = action;
        }

        @Override
        public HandAction getHandAction(boolean bQuick) {
            wasGetHandActionCalled = true;
            quickParamReceived = bQuick;
            return actionToReturn;
        }

        boolean wasGetHandActionCalled() {
            return wasGetHandActionCalled;
        }

        Boolean getQuickParamReceived() {
            return quickParamReceived;
        }
    }

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for PokerPlayer
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        provider = new SwingPlayerActionProvider(null); // TournamentDirector not needed for AI tests

        // Create real player (mocking PokerPlayer doesn't work due to complex
        // hierarchy)
        testPlayer = new PokerPlayer(1, "TestPlayer", false); // false = AI player
        testPlayer.setChipCount(1000);

        // Create test AI (manual test double since Mockito can't mock PokerAI)
        testAI = new TestAI();
        testPlayer.setPokerAI(testAI);

        // Create test ActionOptions (values don't matter for conversion tests)
        testOptions = new ActionOptions(true, // canCheck
                true, // canCall
                true, // canBet
                true, // canRaise
                true, // canFold
                100, // callAmount
                50, // minBet
                1000, // maxBet
                100, // minRaise
                1000, // maxRaise
                30 // timeoutSeconds
        );
    }

    @Test
    void getAction_convertsFoldCorrectly() {
        // Arrange
        HandAction foldAction = new HandAction(testPlayer, HoldemHand.ROUND_FLOP, HandAction.ACTION_FOLD);
        testAI.setActionToReturn(foldAction);

        // Act
        PlayerAction result = provider.getAction(testPlayer, testOptions);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
        assertThat(result.amount()).isEqualTo(0);
    }

    @Test
    void getAction_convertsCheckCorrectly() {
        // Arrange
        HandAction checkAction = new HandAction(testPlayer, HoldemHand.ROUND_FLOP, HandAction.ACTION_CHECK);
        testAI.setActionToReturn(checkAction);

        // Act
        PlayerAction result = provider.getAction(testPlayer, testOptions);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.actionType()).isEqualTo(ActionType.CHECK);
        assertThat(result.amount()).isEqualTo(0);
    }

    @Test
    void getAction_convertsCallCorrectly() {
        // Arrange
        HandAction callAction = new HandAction(testPlayer, HoldemHand.ROUND_FLOP, HandAction.ACTION_CALL, 100 // call
                                                                                                                // amount
        );
        testAI.setActionToReturn(callAction);

        // Act
        PlayerAction result = provider.getAction(testPlayer, testOptions);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.actionType()).isEqualTo(ActionType.CALL);
        assertThat(result.amount()).isEqualTo(0); // Call uses 0 amount in PlayerAction
    }

    @Test
    void getAction_convertsBetWithAmountCorrectly() {
        // Arrange
        HandAction betAction = new HandAction(testPlayer, HoldemHand.ROUND_FLOP, HandAction.ACTION_BET, 250 // bet
                                                                                                            // amount
        );
        testAI.setActionToReturn(betAction);

        // Act
        PlayerAction result = provider.getAction(testPlayer, testOptions);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.actionType()).isEqualTo(ActionType.BET);
        assertThat(result.amount()).isEqualTo(250);
    }

    @Test
    void getAction_convertsRaiseWithAmountCorrectly() {
        // Arrange
        HandAction raiseAction = new HandAction(testPlayer, HoldemHand.ROUND_FLOP, HandAction.ACTION_RAISE, 300 // total
                                                                                                                // raise
                                                                                                                // amount
                                                                                                                // (includes
                                                                                                                // call
                                                                                                                // portion)
        );
        testAI.setActionToReturn(raiseAction);

        // Act
        PlayerAction result = provider.getAction(testPlayer, testOptions);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.actionType()).isEqualTo(ActionType.RAISE);
        assertThat(result.amount()).isEqualTo(300);
    }

    @Test
    void getAction_convertsCheckRaiseToRaise() {
        // Arrange: CHECK_RAISE should be converted to RAISE
        HandAction checkRaiseAction = new HandAction(testPlayer, HoldemHand.ROUND_FLOP, HandAction.ACTION_CHECK_RAISE,
                200);
        testAI.setActionToReturn(checkRaiseAction);

        // Act
        PlayerAction result = provider.getAction(testPlayer, testOptions);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.actionType()).isEqualTo(ActionType.RAISE);
        assertThat(result.amount()).isEqualTo(200);
    }

    @Test
    void getAction_returnsFold_whenAIReturnsNull() {
        // Arrange
        testAI.setActionToReturn(null);

        // Act
        PlayerAction result = provider.getAction(testPlayer, testOptions);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
        assertThat(result.amount()).isEqualTo(0);
    }

    @Test
    void getAction_callsAIWithCorrectParameter() {
        // Arrange
        HandAction foldAction = new HandAction(testPlayer, HoldemHand.ROUND_FLOP, HandAction.ACTION_FOLD);
        testAI.setActionToReturn(foldAction);

        // Act
        provider.getAction(testPlayer, testOptions);

        // Assert: verify AI is called with false (not quick mode)
        assertThat(testAI.wasGetHandActionCalled()).isTrue();
        assertThat(testAI.getQuickParamReceived()).isFalse();
    }

    @Test
    void getAction_returnsFold_forHumanPlayerWithoutTable() {
        // Arrange: human player without a table (safety fallback case)
        PokerPlayer humanPlayer = new PokerPlayer(2, "HumanPlayer", true); // true = human
        humanPlayer.setChipCount(1000);
        // Note: humanPlayer.getTable() returns null by default

        // Act
        PlayerAction result = provider.getAction(humanPlayer, testOptions);

        // Assert: Should return fold as safety fallback
        assertThat(result).isNotNull();
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
        assertThat(result.amount()).isEqualTo(0);
        assertThat(testAI.wasGetHandActionCalled()).isFalse(); // AI should not be called for humans
    }
}
