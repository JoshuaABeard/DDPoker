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

import com.donohoedigital.games.poker.core.GameHand;
import com.donohoedigital.games.poker.core.PlayerActionProvider;
import java.util.function.Consumer;

/**
 * Result from {@link AIProviderFactory#create}, bundling the action provider
 * with an optional new-hand callback. The callback is invoked at the start of
 * each hand so that stateful AI implementations can update their hand
 * reference.
 *
 * @param provider
 *            the AI action provider
 * @param newHandCallback
 *            optional callback invoked with the new {@link GameHand} at hand
 *            start; may be null
 */
public record AIProviderResult(PlayerActionProvider provider, Consumer<GameHand> newHandCallback) {

    /**
     * Convenience constructor for providers that do not need new-hand
     * notifications.
     */
    public AIProviderResult(PlayerActionProvider provider) {
        this(provider, null);
    }
}
