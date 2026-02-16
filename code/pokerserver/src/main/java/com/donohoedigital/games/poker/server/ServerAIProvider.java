/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.ai.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI provider for server-hosted games with skill-based routing.
 * <p>
 * Manages PurePokerAI instances and per-player AIContext for computer players.
 * Routes to different AI algorithms based on skill level:
 * <ul>
 * <li>Skill 1-2: TournamentAI (beginner)</li>
 * <li>Skill 3-4: V1Algorithm (moderate)</li>
 * <li>Skill 5-7: V2Algorithm (advanced)</li>
 * </ul>
 *
 * @see PurePokerAI
 * @see TournamentAI
 * @see V1Algorithm
 * @see V2Algorithm
 * @see PlayerActionProvider
 */
public class ServerAIProvider implements PlayerActionProvider {

    private final Map<Integer, PurePokerAI> playerAIs = new ConcurrentHashMap<>();
    private final Map<Integer, AIContext> playerContexts = new ConcurrentHashMap<>();
    private final GameTable table;
    private final TournamentContext tournament;
    private final ServerOpponentTracker opponentTracker;
    private GameHand currentHand;

    /**
     * Create AI provider for server-hosted game with skill-based routing.
     *
     * @param players
     *            List of all players in the game
     * @param skillLevels
     *            Map of player ID to skill level (1-7)
     * @param table
     *            Game table
     * @param tournament
     *            Tournament context for blinds and structure
     */
    public ServerAIProvider(List<GamePlayerInfo> players, Map<Integer, Integer> skillLevels, GameTable table,
            TournamentContext tournament) {
        this.table = table;
        this.tournament = tournament;
        this.currentHand = null;
        this.opponentTracker = new ServerOpponentTracker();
        initializeAIs(players, skillLevels);
    }

    /**
     * Initialize AI instances and contexts for computer players based on skill
     * level.
     */
    private void initializeAIs(List<GamePlayerInfo> players, Map<Integer, Integer> skillLevels) {
        for (GamePlayerInfo player : players) {
            if (!player.isHuman()) {
                int skill = skillLevels.getOrDefault(player.getID(), 3); // Default: V1Algorithm
                PurePokerAI ai = createAI(skill, player);
                AIContext ctx = createContext(skill, player);
                playerAIs.put(player.getID(), ai);
                playerContexts.put(player.getID(), ctx);
            }
        }
    }

    /**
     * Create AI instance based on skill level.
     *
     * @param skillLevel
     *            Skill level (1-7)
     * @param player
     *            Player info
     * @return AI instance
     */
    private PurePokerAI createAI(int skillLevel, GamePlayerInfo player) {
        long seed = player.getID() * 31L + System.nanoTime();
        return switch (skillLevel) {
            case 1, 2 -> new TournamentAI();
            case 3 -> new V1Algorithm(seed, V1Algorithm.AI_EASY);
            case 4 -> new V1Algorithm(seed, V1Algorithm.AI_MEDIUM);
            case 5, 6, 7 -> new V2Algorithm();
            default -> new V1Algorithm(seed, V1Algorithm.AI_MEDIUM);
        };
    }

    /**
     * Create context based on skill level. V2 players get V2AIContext with strategy
     * provider, others get base ServerAIContext.
     *
     * @param skillLevel
     *            Skill level (1-7)
     * @param player
     *            Player info
     * @return AI context
     */
    private AIContext createContext(int skillLevel, GamePlayerInfo player) {
        if (skillLevel >= 5) {
            ServerStrategyProvider strategy = new ServerStrategyProvider(String.valueOf(player.getID()));
            return new ServerV2AIContext(table, currentHand, tournament, player, strategy, opponentTracker);
        }
        return new ServerAIContext(table, currentHand, tournament, player, opponentTracker);
    }

    /**
     * Update current hand reference and propagate to all contexts. Called at the
     * start of each hand.
     *
     * @param hand
     *            New hand
     */
    public void onNewHand(GameHand hand) {
        this.currentHand = hand;
        for (AIContext ctx : playerContexts.values()) {
            if (ctx instanceof ServerAIContext sac) {
                sac.setCurrentHand(hand);
            }
        }
    }

    /**
     * Get player action from appropriate AI using per-player context.
     *
     * @param player
     *            Player who must act
     * @param options
     *            Available actions and constraints
     * @return AI's decision, or null for human players
     */
    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
        if (player.isHuman()) {
            return null;
        }

        PurePokerAI ai = playerAIs.get(player.getID());
        AIContext ctx = playerContexts.get(player.getID());
        if (ai == null || ctx == null) {
            return PlayerAction.fold();
        }

        try {
            return ai.getAction(player, options, ctx);
        } catch (Exception e) {
            System.err.println("AI error for player " + player.getName() + ": " + e.getMessage() + " - folding");
            return PlayerAction.fold();
        }
    }

    /**
     * Check if player wants to rebuy.
     *
     * @param player
     *            Player considering rebuy
     * @return true if player wants to rebuy
     */
    public boolean wantsRebuy(GamePlayerInfo player) {
        PurePokerAI ai = playerAIs.get(player.getID());
        AIContext ctx = playerContexts.get(player.getID());
        if (ai == null || ctx == null) {
            return false;
        }
        return ai.wantsRebuy(player, ctx);
    }

    /**
     * Check if player wants addon.
     *
     * @param player
     *            Player considering addon
     * @return true if player wants addon
     */
    public boolean wantsAddon(GamePlayerInfo player) {
        PurePokerAI ai = playerAIs.get(player.getID());
        AIContext ctx = playerContexts.get(player.getID());
        if (ai == null || ctx == null) {
            return false;
        }
        return ai.wantsAddon(player, ctx);
    }

    /**
     * Get AI instance for a player (for testing/debugging).
     *
     * @param playerId
     *            Player ID
     * @return AI instance, or null if player is human or not found
     */
    public PurePokerAI getAI(int playerId) {
        return playerAIs.get(playerId);
    }

    /**
     * Get count of AI players being managed.
     *
     * @return Number of computer players with AI
     */
    public int getAICount() {
        return playerAIs.size();
    }
}
