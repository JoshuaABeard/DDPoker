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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.PlayerActionProvider;
import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.core.state.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(ServerPlayerActionProvider.class);
    private final PlayerActionProvider aiProvider;
    private final Map<Integer, PendingAction> pendingActions;
    private final Consumer<ActionRequest> actionRequestCallback;
    private final int timeoutSeconds;
    private final int disconnectGraceTurns;
    private final Map<Long, ServerPlayerSession> playerSessions;
    private final int aiActionDelayMs;
    private final Consumer<GameEvent> timeoutPublisher;

    /**
     * Create a new server player action provider with no AI delay. Used by tests
     * and contexts that don't need pacing.
     */
    public ServerPlayerActionProvider(PlayerActionProvider aiProvider, Consumer<ActionRequest> actionRequestCallback,
            int timeoutSeconds, int disconnectGraceTurns, Map<Long, ServerPlayerSession> playerSessions) {
        this(aiProvider, actionRequestCallback, timeoutSeconds, disconnectGraceTurns, playerSessions, 0);
    }

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
     * @param disconnectGraceTurns
     *            number of turns before disconnected player is auto-folded
     *            immediately
     * @param playerSessions
     *            map of profile ID to player session for disconnect checking
     * @param aiActionDelayMs
     *            milliseconds to sleep after each AI action so humans can observe
     *            the game progressing (0 = no delay)
     */
    public ServerPlayerActionProvider(PlayerActionProvider aiProvider, Consumer<ActionRequest> actionRequestCallback,
            int timeoutSeconds, int disconnectGraceTurns, Map<Long, ServerPlayerSession> playerSessions,
            int aiActionDelayMs) {
        this(aiProvider, actionRequestCallback, timeoutSeconds, disconnectGraceTurns, playerSessions, aiActionDelayMs,
                ignored -> {
                });
    }

    /**
     * Create a new server player action provider with a timeout event publisher.
     *
     * @param aiProvider
     *            AI action provider for computer players
     * @param actionRequestCallback
     *            callback invoked when human player action needed (GameInstance
     *            connects this to WebSocket)
     * @param timeoutSeconds
     *            timeout in seconds for human player actions (0 = no timeout)
     * @param disconnectGraceTurns
     *            number of turns before disconnected player is auto-folded
     *            immediately
     * @param playerSessions
     *            map of profile ID to player session for disconnect checking
     * @param aiActionDelayMs
     *            milliseconds to sleep after each AI action so humans can observe
     *            the game progressing (0 = no delay)
     * @param timeoutPublisher
     *            callback invoked when a player action times out; receives an
     *            {@link GameEvent.ActionTimeout} event for broadcast
     */
    public ServerPlayerActionProvider(PlayerActionProvider aiProvider, Consumer<ActionRequest> actionRequestCallback,
            int timeoutSeconds, int disconnectGraceTurns, Map<Long, ServerPlayerSession> playerSessions,
            int aiActionDelayMs, Consumer<GameEvent> timeoutPublisher) {
        this.aiProvider = aiProvider;
        this.actionRequestCallback = actionRequestCallback;
        this.timeoutSeconds = timeoutSeconds;
        this.disconnectGraceTurns = disconnectGraceTurns;
        this.playerSessions = playerSessions;
        this.pendingActions = new ConcurrentHashMap<>();
        this.aiActionDelayMs = aiActionDelayMs;
        this.timeoutPublisher = timeoutPublisher;
    }

    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
        if (player.isComputer()) {
            PlayerAction action = aiProvider.getAction(player, options);
            if (aiActionDelayMs > 0) {
                try {
                    long delay = ThreadLocalRandom.current().nextLong(aiActionDelayMs / 2L, aiActionDelayMs * 2L + 1);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return action;
        }
        return getHumanAction(player, options);
    }

    /**
     * Get action from human player via CompletableFuture.
     *
     * <p>
     * If the player is disconnected and has exceeded the grace turn limit, auto
     * fold/check immediately without waiting for a timeout.
     *
     * @param player
     *            the human player
     * @param options
     *            available actions
     * @return the player's action, or auto-fold/check on timeout
     */
    private PlayerAction getHumanAction(GamePlayerInfo player, ActionOptions options) {
        logger.debug("[ACTION-HUMAN] getHumanAction player={} id={} options={}", player.getName(), player.getID(),
                options);
        // Check if player is disconnected and past grace period
        ServerPlayerSession session = playerSessions.get((long) player.getID());
        if (session != null && session.isDisconnected()) {
            if (session.getConsecutiveTimeouts() >= disconnectGraceTurns) {
                // Auto-fold immediately — no timeout wait needed
                logger.debug("[ACTION-HUMAN] player={} disconnected past grace, auto-folding", player.getName());
                session.incrementConsecutiveTimeouts();
                return options.canCheck() ? PlayerAction.check() : PlayerAction.fold();
            }
            // else: fall through to normal timeout flow (grace period for reconnect)
        }

        CompletableFuture<PlayerAction> future = new CompletableFuture<>();
        PendingAction pending = new PendingAction(future, options, player);
        pendingActions.put(player.getID(), pending);
        logger.debug("[ACTION-HUMAN] stored pending action for playerId={}", player.getID());

        // Notify via callback (GameInstance sends WebSocket ACTION_REQUIRED)
        logger.debug("[ACTION-HUMAN] calling actionRequestCallback for player={} messageSender={}", player.getName(),
                session != null && session.getMessageSender() != null ? "present" : "null");
        actionRequestCallback.accept(new ActionRequest((ServerPlayer) player, options));

        try {
            if (timeoutSeconds > 0) {
                return future.get(timeoutSeconds, TimeUnit.SECONDS);
            }
            // No timeout (practice mode) — wait indefinitely
            return future.get();
        } catch (TimeoutException e) {
            // Auto-fold on timeout, or check if available (check is free)
            if (session != null) {
                session.incrementConsecutiveTimeouts();
            }
            ActionType autoActionType = options.canCheck() ? ActionType.CHECK : ActionType.FOLD;
            PlayerAction autoAction = options.canCheck() ? PlayerAction.check() : PlayerAction.fold();
            timeoutPublisher.accept(new GameEvent.ActionTimeout(player.getID(), autoActionType));
            return autoAction;
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
        logger.debug("[ACTION-HUMAN] submitAction playerId={} action={} pendingKeys={}", playerId, action,
                pendingActions.keySet());
        PendingAction pending = pendingActions.get(playerId);
        if (pending != null) {
            PlayerAction validated = validateAction(action, pending.options);
            logger.debug("[ACTION-HUMAN] completing future with validated={}", validated);
            pending.future.complete(validated);
        } else {
            logger.debug("[ACTION-HUMAN] no pending action for playerId={}", playerId);
        }
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
     * Returns the pending action request for a player, or null if none is waiting.
     * Used by the WebSocket handler to re-send ACTION_REQUIRED when a player
     * reconnects.
     */
    public ActionRequest getPendingActionRequest(int playerId) {
        PendingAction pending = pendingActions.get(playerId);
        return pending != null ? new ActionRequest((ServerPlayer) pending.player(), pending.options()) : null;
    }

    /**
     * Pending action tracking.
     */
    private record PendingAction(CompletableFuture<PlayerAction> future, ActionOptions options, GamePlayerInfo player) {
    }
}
