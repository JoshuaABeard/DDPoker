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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.ai.AIContext;
import com.donohoedigital.games.poker.core.ai.PurePokerAI;
import com.donohoedigital.games.poker.core.ai.TournamentAI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI provider for server-hosted games.
 * <p>
 * Manages PurePokerAI instances for computer players and provides actions when
 * requested by the game engine. Implements PlayerActionProvider to integrate
 * with pokergamecore.
 * <p>
 * <strong>AI Selection Strategy:</strong>
 * <ul>
 * <li>Currently uses TournamentAI for all computer players</li>
 * <li>Future: Select V1Algorithm or V2Algorithm based on skill level</li>
 * </ul>
 * <p>
 * <strong>Usage:</strong>
 *
 * <pre>
 * ServerAIProvider aiProvider = new ServerAIProvider(players, context);
 * TournamentEngine engine = new TournamentEngine(eventBus, aiProvider);
 * </pre>
 *
 * @see PurePokerAI
 * @see TournamentAI
 * @see PlayerActionProvider
 */
public class ServerAIProvider implements PlayerActionProvider {

    private final Map<Integer, PurePokerAI> playerAIs = new ConcurrentHashMap<>();
    private final AIContext context;

    /**
     * Create AI provider for server-hosted game.
     *
     * @param players
     *            List of all players in the game
     * @param context
     *            AI context for game state queries
     */
    public ServerAIProvider(List<GamePlayerInfo> players, AIContext context) {
        this.context = context;
        initializeAIs(players);
    }

    /**
     * Initialize AI instances for computer players.
     * <p>
     * Currently creates TournamentAI for all computer players. Future enhancement
     * will select AI based on skill level:
     * <ul>
     * <li>Skill 1-2: TournamentAI (beginner)</li>
     * <li>Skill 3-4: V1Algorithm (moderate)</li>
     * <li>Skill 5-7: V2Algorithm (advanced)</li>
     * </ul>
     */
    private void initializeAIs(List<GamePlayerInfo> players) {
        for (GamePlayerInfo player : players) {
            if (!player.isHuman()) {
                // TODO: Select AI based on skill level when V1/V2 algorithms are available
                // For now, use TournamentAI for all computer players
                PurePokerAI ai = createAI(player);
                playerAIs.put(player.getID(), ai);
            }
        }
    }

    /**
     * Create AI instance for a player.
     * <p>
     * <strong>Current Implementation:</strong> Returns TournamentAI for all players
     * <p>
     * <strong>Future Implementation:</strong>
     *
     * <pre>
     * private PurePokerAI createAI(GamePlayerInfo player) {
     * 	int skillLevel = getSkillLevel(player);
     * 	return switch (skillLevel) {
     * 		case 1, 2 -> new TournamentAI(); // Beginner
     * 		case 3, 4 -> new V1Algorithm(); // Moderate
     * 		case 5, 6, 7 -> new V2Algorithm(); // Advanced
     * 		default -> new V1Algorithm();
     * 	};
     * }
     * </pre>
     *
     * @param player
     *            Player to create AI for
     * @return AI instance
     */
    private PurePokerAI createAI(GamePlayerInfo player) {
        // TODO: When V1/V2 algorithms are available, select based on skill level
        // For now, TournamentAI provides:
        // - Fast game completion for testing
        // - Reasonable tournament-aware strategy
        // - Zero Swing dependencies
        return new TournamentAI();
    }

    /**
     * Get player action from appropriate AI.
     * <p>
     * Called by TournamentEngine when a player must act. Routes the request to the
     * player's AI instance (for computer players) or returns null for human players
     * (server will handle separately via network).
     *
     * @param player
     *            Player who must act
     * @param options
     *            Available actions and constraints
     * @return AI's decision, or null for human players
     */
    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
        // Human players handled separately (via network in server-hosted games)
        if (player.isHuman()) {
            return null; // Server will wait for network message
        }

        // Get AI for this player
        PurePokerAI ai = playerAIs.get(player.getID());
        if (ai == null) {
            // Shouldn't happen - all computer players should have AI
            // Fall back to fold for safety
            return PlayerAction.fold();
        }

        // Delegate to AI with error handling
        try {
            return ai.getAction(player, options, context);
        } catch (Exception e) {
            // If AI throws exception, fold for safety
            // Log error for debugging but don't crash the game
            System.err.println("AI error for player " + player.getName() + ": " + e.getMessage() + " - folding");
            return PlayerAction.fold();
        }
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
