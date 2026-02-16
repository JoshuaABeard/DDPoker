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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.PlayerActionProvider;

/**
 * Server-side player action provider. Routes action requests to AI (via AI
 * provider) or to human players (via callback that GameInstance connects to
 * WebSocket).
 *
 * <p>
 * For human players, this class blocks the game engine thread using
 * CompletableFuture until an action is received via WebSocket or a timeout
 * occurs. This is the same pattern as SwingPlayerActionProvider (which blocks
 * on a CountDownLatch).
 */
public class ServerPlayerActionProvider implements PlayerActionProvider {
    private final PlayerActionProvider aiProvider;
    private final Map<Integer, PendingAction> pendingActions;
    private final Consumer<ActionRequest> actionRequestCallback;
    private final int timeoutSeconds;

    /**
     * Create a new server player action provider.
     *
     * @param aiProvider
     *            AI action provider for computer players
     * @param actionRequestCallback
     *            callback invoked when human player action needed (GameInstance
     *            connects this to WebSocket)
     * @param timeoutSeconds
     *            timeout in seconds for human player actions (0 = no timeout)
     */
    public ServerPlayerActionProvider(PlayerActionProvider aiProvider, Consumer<ActionRequest> actionRequestCallback,
            int timeoutSeconds) {
        this.aiProvider = aiProvider;
        this.actionRequestCallback = actionRequestCallback;
        this.timeoutSeconds = timeoutSeconds;
        this.pendingActions = new ConcurrentHashMap<>();
    }

    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
        if (player.isComputer()) {
            return aiProvider.getAction(player, options);
        }
        return getHumanAction(player, options);
    }

    /**
     * Get action from human player via CompletableFuture.
     *
     * @param player
     *            the human player
     * @param options
     *            available actions
     * @return the player's action, or auto-fold/check on timeout
     */
    private PlayerAction getHumanAction(GamePlayerInfo player, ActionOptions options) {
        CompletableFuture<PlayerAction> future = new CompletableFuture<>();
        PendingAction pending = new PendingAction(future, options);
        pendingActions.put(player.getID(), pending);

        // Notify via callback (GameInstance sends WebSocket ACTION_REQUIRED)
        actionRequestCallback.accept(new ActionRequest((ServerPlayer) player, options));

        try {
            if (timeoutSeconds > 0) {
                return future.get(timeoutSeconds, TimeUnit.SECONDS);
            }
            // No timeout (practice mode) — wait indefinitely
            return future.get();
        } catch (TimeoutException e) {
            // Auto-fold on timeout, or check if available (check is free)
            return options.canCheck() ? PlayerAction.check() : PlayerAction.fold();
        } catch (Exception e) {
            // Any other error — fold
            return PlayerAction.fold();
        } finally {
            pendingActions.remove(player.getID());
        }
    }

    /**
     * Submit an action for a human player (called by GameInstance from WebSocket
     * handler).
     *
     * @param playerId
     *            the player's ID
     * @param action
     *            the player's chosen action
     */
    public void submitAction(int playerId, PlayerAction action) {
        PendingAction pending = pendingActions.get(playerId);
        if (pending != null) {
            PlayerAction validated = validateAction(action, pending.options);
            pending.future.complete(validated);
        }
        // If no pending action, ignore silently (might be stale submission)
    }

    /**
     * Submit an action with explicit options for validation (called by GameInstance
     * from WebSocket handler).
     *
     * @param playerId
     *            the player's ID
     * @param action
     *            the player's chosen action
     * @param currentOptions
     *            current action options for validation
     */
    public void submitAction(int playerId, PlayerAction action, ActionOptions currentOptions) {
        PendingAction pending = pendingActions.get(playerId);
        if (pending != null) {
            PlayerAction validated = validateAction(action, currentOptions);
            pending.future.complete(validated);
        }
        // If no pending action, ignore silently (might be stale submission)
    }

    /**
     * Validate a player action against current options. Invalid actions default to
     * fold. Bet/raise amounts are clamped to valid range.
     *
     * @param action
     *            the player's chosen action
     * @param options
     *            current action options
     * @return validated action
     */
    private PlayerAction validateAction(PlayerAction action, ActionOptions options) {
        return switch (action.actionType()) {
            case FOLD -> options.canFold() ? action : PlayerAction.fold();
            case CHECK -> options.canCheck() ? action : PlayerAction.fold();
            case CALL -> options.canCall() ? action : PlayerAction.fold();
            case BET -> {
                if (!options.canBet())
                    yield PlayerAction.fold();
                int amount = Math.max(options.minBet(), Math.min(action.amount(), options.maxBet()));
                yield PlayerAction.bet(amount);
            }
            case RAISE -> {
                if (!options.canRaise())
                    yield PlayerAction.fold();
                int amount = Math.max(options.minRaise(), Math.min(action.amount(), options.maxRaise()));
                yield PlayerAction.raise(amount);
            }
            default -> PlayerAction.fold();
        };
    }

    /**
     * Pending action tracking.
     */
    private record PendingAction(CompletableFuture<PlayerAction> future, ActionOptions options) {
    }
}
