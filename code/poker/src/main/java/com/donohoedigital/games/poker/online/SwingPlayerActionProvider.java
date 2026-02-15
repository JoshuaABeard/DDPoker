/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.logic.BetValidator;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridge between pokergamecore's PlayerActionProvider interface and existing
 * Swing betting UI / AI strategy. Delegates to existing systems for getting
 * player actions.
 *
 * Phase 2 Step 10: Full implementation.
 */
public class SwingPlayerActionProvider implements PlayerActionProvider {

    private final TournamentDirector td;

    public SwingPlayerActionProvider(TournamentDirector td) {
        this.td = td;
    }

    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
        // Cast to concrete types (safe because we know the implementation)
        PokerPlayer pokerPlayer = (PokerPlayer) player;

        // For human players: use existing Swing UI (BetPhase)
        // For AI players: use existing AI strategy
        if (pokerPlayer.isHuman()) {
            return getHumanAction(pokerPlayer, options);
        } else {
            return getAIAction(pokerPlayer, options);
        }
    }

    /**
     * Get action from human player using existing Swing UI. This method blocks
     * until the user makes a decision (or timeout occurs).
     *
     * Uses CountDownLatch to bridge the synchronous API with asynchronous Swing UI.
     */
    private PlayerAction getHumanAction(PokerPlayer player, ActionOptions options) {
        // Get game components
        PokerTable table = player.getTable();
        if (table == null) {
            return PlayerAction.fold(); // Safety: player not at table
        }

        PokerGame game = table.getGame();
        if (game == null) {
            return PlayerAction.fold(); // Safety: no game instance
        }

        HoldemHand hand = table.getHoldemHand();
        if (hand == null) {
            return PlayerAction.fold(); // Safety: no active hand
        }

        // Create synchronization primitives
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HandAction> actionRef = new AtomicReference<>();

        // Create one-time listener for this specific player action
        PlayerActionListener listener = (action, amount) -> {
            // Convert UI action to HandAction
            HandAction handAction = createHandActionFromUI(player, hand, action, amount);
            actionRef.set(handAction);
            latch.countDown();
        };

        // Store existing listener (to restore later)
        // NOTE: Should be null since Bet phase is no longer used (Phase 3 removed it).
        // If non-null, setPlayerActionListener will assert. Clear it defensively.
        PlayerActionListener existingListener = game.getPlayerActionListener();
        if (existingListener != null) {
            game.setPlayerActionListener(null); // Clear existing listener first
        }

        try {
            // Register our temporary listener
            game.setPlayerActionListener(listener);

            // Show betting UI on Swing EDT
            SwingUtilities.invokeLater(() -> showBettingUI(game, hand, player, options));

            // Wait for user input (with timeout)
            int timeoutSeconds = options.timeoutSeconds();
            if (timeoutSeconds <= 0) {
                timeoutSeconds = 300; // Default 5 minutes if no timeout specified
            }

            boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);

            if (!completed) {
                // Timeout - auto-fold
                return PlayerAction.fold();
            }

            // Get result and convert to PlayerAction
            HandAction result = actionRef.get();
            if (result == null) {
                return PlayerAction.fold(); // Safety fallback
            }

            return convertHandActionToPlayerAction(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PlayerAction.fold();
        } finally {
            // Cleanup: restore previous listener and reset UI
            // Clear current listener first to avoid assertion if restoring non-null
            if (existingListener != null) {
                game.setPlayerActionListener(null);
            }
            game.setPlayerActionListener(existingListener);
            SwingUtilities.invokeLater(() -> game.setInputMode(PokerTableInput.MODE_QUITSAVE));
        }
    }

    /**
     * Get action from AI player using existing AI strategy.
     */
    private PlayerAction getAIAction(PokerPlayer player, ActionOptions options) {
        // Get AI instance
        PokerAI ai = player.getPokerAI();
        if (ai == null) {
            // Safety fallback: if no AI configured, fold
            return PlayerAction.fold();
        }

        // Get AI decision using existing strategy system
        // false = not quick mode (AI takes time to think)
        HandAction handAction = ai.getHandAction(false);

        if (handAction == null) {
            // Safety fallback: if AI returns null, fold
            return PlayerAction.fold();
        }

        // Convert HandAction to PlayerAction
        return convertHandActionToPlayerAction(handAction);
    }

    /**
     * Convert legacy HandAction to pokergamecore PlayerAction.
     *
     * Note: CALL action uses amount=0 in PlayerAction because the call amount is
     * re-derived from game state in HoldemHand.applyPlayerAction() via getCall().
     * This asymmetry (BET/RAISE preserve amount, CALL does not) is intentional - it
     * ensures the call amount is always accurate even if game state changes between
     * conversion points.
     *
     * @param handAction
     *            the legacy HandAction from AI or UI
     * @return equivalent PlayerAction for pokergamecore
     */
    private PlayerAction convertHandActionToPlayerAction(HandAction handAction) {
        int action = handAction.getAction();
        int amount = handAction.getAmount();

        return switch (action) {
            case HandAction.ACTION_FOLD -> PlayerAction.fold();
            case HandAction.ACTION_CHECK -> PlayerAction.check();
            case HandAction.ACTION_CALL -> PlayerAction.call(); // amount=0, re-derived in applyPlayerAction
            case HandAction.ACTION_BET -> PlayerAction.bet(amount);
            case HandAction.ACTION_RAISE -> PlayerAction.raise(amount);
            default -> {
                // For other actions (CHECK_RAISE, blinds, etc.), map to closest equivalent
                // CHECK_RAISE becomes RAISE, others fold as safety
                if (action == HandAction.ACTION_CHECK_RAISE) {
                    yield PlayerAction.raise(amount);
                }
                yield PlayerAction.fold();
            }
        };
    }

    /**
     * Show betting UI on Swing EDT. Sets up the table input mode to display
     * appropriate betting buttons.
     */
    private void showBettingUI(PokerGame game, HoldemHand hand, PokerPlayer player, ActionOptions options) {
        // Determine input mode based on betting situation
        int toCall = hand.getCall(player);
        int currentBet = hand.getBet();
        int inputMode = BetValidator.determineInputMode(toCall, currentBet);

        // Set input mode - this enables the betting buttons
        game.setInputMode(inputMode, hand, player);
    }

    /**
     * Create HandAction from UI button callback. Converts PokerGame.ACTION_*
     * constants to HandAction with appropriate amounts.
     */
    private HandAction createHandActionFromUI(PokerPlayer player, HoldemHand hand, int uiAction, int amount) {
        int round = hand.getRound().toLegacy();

        return switch (uiAction) {
            case PokerGame.ACTION_FOLD -> new HandAction(player, round, HandAction.ACTION_FOLD);
            case PokerGame.ACTION_CHECK -> new HandAction(player, round, HandAction.ACTION_CHECK);
            case PokerGame.ACTION_CALL -> {
                int callAmount = hand.getCall(player);
                yield new HandAction(player, round, HandAction.ACTION_CALL, callAmount);
            }
            case PokerGame.ACTION_BET -> new HandAction(player, round, HandAction.ACTION_BET, amount);
            case PokerGame.ACTION_RAISE -> new HandAction(player, round, HandAction.ACTION_RAISE, amount);
            case PokerGame.ACTION_ALL_IN -> {
                // All-in: player bets/raises all chips
                int chipCount = player.getChipCount();
                int toCall = hand.getCall(player);
                if (toCall == 0) {
                    yield new HandAction(player, round, HandAction.ACTION_BET, chipCount);
                } else {
                    yield new HandAction(player, round, HandAction.ACTION_RAISE, chipCount);
                }
            }
            default -> new HandAction(player, round, HandAction.ACTION_FOLD); // Safety fallback
        };
    }
}
