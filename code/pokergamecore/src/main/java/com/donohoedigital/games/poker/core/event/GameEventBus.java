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
package com.donohoedigital.games.poker.core.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple in-process event bus for dispatching game events to registered
 * listeners. Thread-safe using copy-on-write semantics.
 */
public class GameEventBus {
    private static final Logger logger = Logger.getLogger(GameEventBus.class.getName());
    private final List<GameEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Subscribe a listener to receive all game events.
     *
     * @param listener
     *            the listener to add
     */
    public void subscribe(GameEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        listeners.add(listener);
    }

    /**
     * Unsubscribe a listener from receiving game events.
     *
     * @param listener
     *            the listener to remove
     * @return true if the listener was removed, false if it was not subscribed
     */
    public boolean unsubscribe(GameEventListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Publish an event to all subscribed listeners.
     *
     * @param event
     *            the event to publish
     */
    public void publish(GameEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        for (GameEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // Log but don't propagate listener exceptions
                logger.log(Level.WARNING, "Error in event listener for event: " + event, e);
            }
        }
    }

    /** @return the number of currently subscribed listeners */
    public int getListenerCount() {
        return listeners.size();
    }

    /** Remove all subscribed listeners. */
    public void clear() {
        listeners.clear();
    }
}
