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
package com.donohoedigital.games.poker.gameserver;

import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.core.TournamentContext;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating AI action providers. Allows the game engine module
 * ({@code pokergameserver}) to use strategic AI implementations from other
 * modules (e.g., {@code pokerserver}'s ServerAIProvider) without a direct
 * compile-time dependency.
 *
 * <p>
 * When no factory is provided, {@link GameInstance} falls back to a simple
 * random AI.
 *
 * @see AIProviderResult
 */
@FunctionalInterface
public interface AIProviderFactory {

    /**
     * Create an AI provider for a game, along with an optional new-hand callback
     * for stateful AI implementations.
     *
     * @param players
     *            all players in the game
     * @param skillLevels
     *            map of player ID to skill level (1-7)
     * @param table
     *            the game table
     * @param tournament
     *            the tournament context
     * @return result containing the AI action provider and optional new-hand
     *         callback
     */
    AIProviderResult create(List<GamePlayerInfo> players, Map<Integer, Integer> skillLevels, GameTable table,
            TournamentContext tournament);
}
