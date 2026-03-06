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
import com.donohoedigital.games.poker.event.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Swing EDT event dispatcher for PokerTableEvents. Dispatches legacy
 * PokerTableEvents to registered listeners on the Swing EDT thread.
 *
 * <p>
 * Standalone event bus — manages its own listener list and publish method.
 * Accepts PokerGame for table resolution.
 * </p>
 */
public class SwingEventBus {

    private static final Logger logger = LogManager.getLogger(SwingEventBus.class);

    private final PokerGame game;
    private final CopyOnWriteArrayList<PokerTableListener> legacyListeners = new CopyOnWriteArrayList<>();

    /**
     * Create event bus for the given game.
     *
     * @param game
     *            the poker game (manages all tables)
     */
    public SwingEventBus(PokerGame game) {
        this.game = game;
    }

    /**
     * Publishes a PokerTableEvent to all registered listeners on the Swing EDT.
     */
    public void publish(PokerTableEvent event) {
        logger.debug("[SwingEventBus] publish event={} listeners={}", event.getTypeAsString(), legacyListeners.size());
        if (!legacyListeners.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                for (PokerTableListener listener : legacyListeners) {
                    listener.tableEventOccurred(event);
                }
            });
        }
    }

    /**
     * Add a legacy PokerTableListener.
     */
    public void addLegacyListener(PokerTableListener listener) {
        legacyListeners.add(listener);
    }

    /**
     * Remove a legacy PokerTableListener.
     */
    public void removeLegacyListener(PokerTableListener listener) {
        legacyListeners.remove(listener);
    }
}
