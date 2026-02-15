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
     * Get action from human player using existing Swing UI. NOTE: In Phase 2, we
     * don't actually invoke UI here - the existing TournamentDirector.doBetting()
     * handles that. This is a placeholder for future full delegation.
     */
    private PlayerAction getHumanAction(PokerPlayer player, ActionOptions options) {
        // TODO Phase 3: Full implementation with Swing UI integration
        // For Phase 2, return null to indicate we should use existing code path
        return null;
    }

    /**
     * Get action from AI player using existing AI strategy.
     */
    private PlayerAction getAIAction(PokerPlayer player, ActionOptions options) {
        // Use existing AI strategy system
        // For Phase 2, return null to use existing code path
        // TODO Phase 3: Integrate AI action retrieval
        return null;
    }
}
