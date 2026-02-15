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
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.TournamentContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simple tournament-aware poker AI using M-ratio strategy.
 * <p>
 * This AI uses <strong>M-ratio</strong> (stack / cost per orbit) to determine
 * playing style. It adjusts aggression based on stack pressure but does NOT
 * consider hand strength, position, or pot odds.
 * <p>
 * <strong>Performance:</strong> 10-50x faster than random play in tests.
 * <p>
 * <strong>Intended Use:</strong>
 * <ul>
 * <li>Development/testing - Fast game completion for testing server
 * infrastructure</li>
 * <li>Beginner difficulty - Labeled honestly as "plays fast, not smart"</li>
 * <li>Placeholder - While V1/V2 AI extraction is in progress</li>
 * </ul>
 * <p>
 * <strong>NOT recommended for:</strong> Player-facing production games
 * expecting realistic poker AI. Use V1Algorithm or V2Algorithm instead.
 * <p>
 * <strong>Strategy Zones:</strong>
 * <ul>
 * <li><strong>Critical (M &lt; 5):</strong> Push-or-fold - 70% all-in, 30%
 * fold</li>
 * <li><strong>Danger (5 ≤ M &lt; 10):</strong> Aggressive - 50% bet/raise</li>
 * <li><strong>Comfortable (M ≥ 10):</strong> Balanced - weighted random
 * actions</li>
 * </ul>
 *
 * @see PurePokerAI
 * @see AIContext
 */
public class TournamentAI implements PurePokerAI {

    private final Random random;

    /**
     * Create tournament AI with random seed.
     */
    public TournamentAI() {
        this(System.nanoTime());
    }

    /**
     * Create tournament AI with specific seed (for deterministic testing).
     *
     * @param seed
     *            Random seed
     */
    public TournamentAI(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options, AIContext context) {
        // Calculate M-ratio: stack / cost per orbit
        TournamentContext tournament = context.getTournament();
        int stack = player.getChipCount();
        int level = tournament.getLevel();
        int smallBlind = tournament.getSmallBlind(level);
        int bigBlind = tournament.getBigBlind(level);
        int ante = tournament.getAnte(level);

        // Standard 10-handed cost per orbit: SB + BB + (10 * ante)
        int costPerOrbit = smallBlind + bigBlind + (10 * ante);
        double mRatio = stack / (double) Math.max(1, costPerOrbit);

        // Choose strategy based on M-ratio zone
        if (mRatio < 5.0) {
            return criticalZoneStrategy(player, options);
        } else if (mRatio < 10.0) {
            return dangerZoneStrategy(player, options);
        } else {
            return comfortableZoneStrategy(player, options);
        }
    }

    @Override
    public boolean wantsRebuy(GamePlayerInfo player, AIContext context) {
        // Simple rebuy logic: 50% chance to rebuy if allowed
        // Could be made more sophisticated based on tournament phase, player position,
        // etc.
        return random.nextBoolean();
    }

    @Override
    public boolean wantsAddon(GamePlayerInfo player, AIContext context) {
        // Simple addon logic: 75% chance to take addon
        // Addons are usually good value, so bias toward taking them
        return random.nextInt(100) < 75;
    }

    /**
     * CRITICAL ZONE (M &lt; 5): Push or fold strategy.
     * <p>
     * Stack pressure is severe - can't afford to wait. Go all-in or fold.
     */
    private PlayerAction criticalZoneStrategy(GamePlayerInfo player, ActionOptions options) {
        int stack = player.getChipCount();

        // Try to go all-in (70% of the time)
        if (options.canBet() || options.canRaise()) {
            if (random.nextInt(100) < 70) {
                int allIn = Math.min(stack, options.maxBet() > 0 ? options.maxBet() : options.maxRaise());
                return options.canBet() ? PlayerAction.bet(allIn) : PlayerAction.raise(allIn);
            } else {
                return PlayerAction.fold();
            }
        }

        // Call if we can (pot odds might be good) - 60% of the time
        if (options.canCall()) {
            return random.nextInt(100) < 60 ? PlayerAction.call() : PlayerAction.fold();
        }

        // Free check
        if (options.canCheck()) {
            return PlayerAction.check();
        }

        return PlayerAction.fold();
    }

    /**
     * DANGER ZONE (5 ≤ M &lt; 10): Aggressive play to accumulate chips.
     * <p>
     * Moderate pressure - need to build stack before blinds consume it.
     */
    private PlayerAction dangerZoneStrategy(GamePlayerInfo player, ActionOptions options) {
        int stack = player.getChipCount();

        // Raise aggressively (50% of the time)
        if (options.canRaise() && random.nextInt(100) < 50) {
            int raiseRange = Math.max(1, (options.maxRaise() - options.minRaise()) / 3 + 1);
            int raiseAmount = options.minRaise() + random.nextInt(raiseRange);
            return PlayerAction.raise(Math.min(raiseAmount, stack));
        }

        // Bet aggressively (50% of the time)
        if (options.canBet() && random.nextInt(100) < 50) {
            int betRange = Math.max(1, (options.maxBet() - options.minBet()) / 3 + 1);
            int betAmount = options.minBet() + random.nextInt(betRange);
            return PlayerAction.bet(Math.min(betAmount, stack));
        }

        // Call moderately (60% of the time)
        if (options.canCall() && random.nextInt(100) < 60) {
            return PlayerAction.call();
        }

        // Free check
        if (options.canCheck()) {
            return PlayerAction.check();
        }

        return PlayerAction.fold();
    }

    /**
     * COMFORTABLE ZONE (M ≥ 10): Balanced play with moderate aggression.
     * <p>
     * Deep stack allows patience - use weighted random selection.
     */
    private PlayerAction comfortableZoneStrategy(GamePlayerInfo player, ActionOptions options) {
        int stack = player.getChipCount();
        List<PlayerAction> availableActions = new ArrayList<>();

        // Build weighted action list (some actions added multiple times for
        // probability)

        // Check is safe - 2x weight
        if (options.canCheck()) {
            availableActions.add(PlayerAction.check());
            availableActions.add(PlayerAction.check());
        }

        // Call is moderate - 1x weight
        if (options.canCall()) {
            availableActions.add(PlayerAction.call());
        }

        // Bet for value - 1x weight
        if (options.canBet()) {
            int betRange = Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1);
            int betAmount = options.minBet() + random.nextInt(betRange);
            availableActions.add(PlayerAction.bet(Math.min(betAmount, stack)));
        }

        // Raise for value - 1x weight
        if (options.canRaise()) {
            int raiseRange = Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1);
            int raiseAmount = options.minRaise() + random.nextInt(raiseRange);
            availableActions.add(PlayerAction.raise(Math.min(raiseAmount, stack)));
        }

        // Fold is always available - 1x weight
        if (options.canFold()) {
            availableActions.add(PlayerAction.fold());
        }

        // Choose random action from weighted list
        return availableActions.isEmpty()
                ? PlayerAction.fold()
                : availableActions.get(random.nextInt(availableActions.size()));
    }
}
