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
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.PlayerAction;

/**
 * Pure poker AI interface with no Swing/AWT dependencies. Suitable for both
 * server-hosted games and desktop client.
 * <p>
 * This interface provides a clean abstraction for poker AI decision making that
 * works in any environment (desktop client, headless server, tests).
 * <p>
 * Implementations include:
 * <ul>
 * <li>{@code TournamentAI} - Simple M-ratio based tournament strategy</li>
 * <li>{@code V1Algorithm} - Moderate skill AI (Sklansky groups, position, pot
 * odds)</li>
 * <li>{@code V2Algorithm} - Advanced AI (bluffing, bet sizing, c-betting)</li>
 * </ul>
 *
 * @see AIContext
 * @see com.donohoedigital.games.poker.ai.PokerAI
 */
public interface PurePokerAI {

    /**
     * Decide what action to take when it's the player's turn.
     * <p>
     * The AI should analyze the current game state (via {@link AIContext}),
     * consider available options, and return the chosen action.
     *
     * @param player
     *            The player making the decision (read-only info)
     * @param options
     *            Available actions and constraints (fold, check, call, bet, raise)
     * @param context
     *            Game state for decision making (table, hand, tournament, etc.)
     * @return The action to take (fold, check, call, bet, or raise with amount)
     */
    PlayerAction getAction(GamePlayerInfo player, ActionOptions options, AIContext context);

    /**
     * Decide whether to rebuy when eliminated.
     * <p>
     * Called when a player is eliminated and rebuys are allowed. The AI should
     * consider:
     * <ul>
     * <li>Number of rebuys already used</li>
     * <li>Tournament phase (early/late)</li>
     * <li>Cost vs stack size benefit</li>
     * <li>AI personality (aggressive/conservative)</li>
     * </ul>
     *
     * @param player
     *            The player considering rebuy
     * @param context
     *            Game state for decision making
     * @return {@code true} if player wants to rebuy, {@code false} otherwise
     */
    boolean wantsRebuy(GamePlayerInfo player, AIContext context);

    /**
     * Decide whether to take addon chips.
     * <p>
     * Called when addon period is active (typically at end of rebuy period). The AI
     * should consider:
     * <ul>
     * <li>Current stack size</li>
     * <li>Addon chip value</li>
     * <li>Cost vs benefit</li>
     * <li>Tournament strategy</li>
     * </ul>
     *
     * @param player
     *            The player considering addon
     * @param context
     *            Game state for decision making
     * @return {@code true} if player wants addon, {@code false} otherwise
     */
    boolean wantsAddon(GamePlayerInfo player, AIContext context);
}
