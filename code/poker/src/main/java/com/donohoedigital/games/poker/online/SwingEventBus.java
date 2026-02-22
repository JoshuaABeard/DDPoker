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
import com.donohoedigital.games.poker.core.event.*;
import com.donohoedigital.games.poker.event.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bridge between pokergamecore's GameEvent records and legacy PokerTableEvent
 * bitmasks. Converts new events to legacy format and dispatches on Swing EDT.
 *
 * Phase 2: Accepts PokerGame instead of PokerTable since one event bus serves
 * all tables. Extracts table from event's tableId.
 */
public class SwingEventBus extends GameEventBus {

    private static final Logger logger = LogManager.getLogger(SwingEventBus.class);

    private final PokerGame game;
    private final List<PokerTableListener> legacyListeners = new CopyOnWriteArrayList<>();

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
     * Override publish to convert and dispatch events to legacy listeners.
     */
    @Override
    public void publish(GameEvent event) {
        logger.debug("[SwingEventBus] publish event={} listeners={}", event.getClass().getSimpleName(),
                legacyListeners.size());
        // Call parent to notify pokergamecore listeners
        super.publish(event);

        // Convert to legacy event and dispatch on EDT
        if (!legacyListeners.isEmpty() && game != null) {
            PokerTableEvent legacyEvent = convertToLegacy(event);
            logger.debug("[SwingEventBus] converted to legacy={}",
                    legacyEvent != null ? legacyEvent.getTypeAsString() : "null");
            if (legacyEvent != null) {
                SwingUtilities.invokeLater(() -> {
                    for (PokerTableListener listener : legacyListeners) {
                        listener.tableEventOccurred(legacyEvent);
                    }
                });
            }
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

    /**
     * Convert GameEvent record to legacy PokerTableEvent.
     * Extracts table from event's tableId field.
     */
    private PokerTableEvent convertToLegacy(GameEvent event) {
        // Extract table ID from event (most events have tableId)
        int tableId = getTableId(event);
        PokerTable table = tableId >= 0 && game != null ? game.getTable(tableId) : null;

        if (table == null) {
            return null; // Can't convert without table
        }

        return switch (event) {
            case GameEvent.HandStarted e ->
                new PokerTableEvent(PokerTableEvent.TYPE_NEW_HAND, table);

            case GameEvent.PlayerActed e ->
                // PlayerActed needs HandAction, but we don't have it in the event
                // For now, just emit a basic event
                new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ACTION, table);

            case GameEvent.CommunityCardsDealt e ->
                new PokerTableEvent(PokerTableEvent.TYPE_DEALER_ACTION, table);

            case GameEvent.HandCompleted e ->
                new PokerTableEvent(PokerTableEvent.TYPE_END_HAND, table);

            case GameEvent.TableStateChanged e ->
                new PokerTableEvent(PokerTableEvent.TYPE_STATE_CHANGED, table,
                    e.oldState().toLegacy(), e.newState().toLegacy());

            case GameEvent.PlayerAdded e -> {
                PokerPlayer player = game.getPokerPlayerFromID(e.playerId());
                yield new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ADDED, table, player, e.seat());
            }

            case GameEvent.PlayerRemoved e -> {
                PokerPlayer player = game.getPokerPlayerFromID(e.playerId());
                yield new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REMOVED, table, player, e.seat());
            }

            case GameEvent.LevelChanged e ->
                new PokerTableEvent(PokerTableEvent.TYPE_LEVEL_CHANGED, table, e.newLevel());

            case GameEvent.ButtonMoved e ->
                new PokerTableEvent(PokerTableEvent.TYPE_BUTTON_MOVED, table, e.newSeat());

            case GameEvent.ShowdownStarted e ->
                new PokerTableEvent(PokerTableEvent.TYPE_DEALER_ACTION, table);

            case GameEvent.PotAwarded e ->
                new PokerTableEvent(PokerTableEvent.TYPE_END_HAND, table);

            case GameEvent.TournamentCompleted e ->
                null; // No direct legacy equivalent

            case GameEvent.BreakStarted e ->
                new PokerTableEvent(PokerTableEvent.TYPE_STATE_CHANGED, table);

            case GameEvent.BreakEnded e ->
                new PokerTableEvent(PokerTableEvent.TYPE_STATE_CHANGED, table);

            case GameEvent.ColorUpCompleted e ->
                new PokerTableEvent(PokerTableEvent.TYPE_STATE_CHANGED, table);

            case GameEvent.CurrentPlayerChanged e ->
                // Current player changed needs basic event
                new PokerTableEvent(PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED, table);

            case GameEvent.PlayerRebuy e -> {
                PokerPlayer player = game.getPokerPlayerFromID(e.playerId());
                // Rebuy needs cash, chips, pending flag - but we only have amount in event
                yield new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REBUY, table, player,
                    e.amount(), e.amount(), false);
            }

            case GameEvent.PlayerAddon e -> {
                PokerPlayer player = game.getPokerPlayerFromID(e.playerId());
                // Addon needs cash, chips, pending flag - but we only have amount in event
                yield new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ADDON, table, player,
                    e.amount(), e.amount(), false);
            }

            case GameEvent.ObserverAdded e ->
                new PokerTableEvent(PokerTableEvent.TYPE_OBSERVER_ADDED, table);

            case GameEvent.ObserverRemoved e ->
                new PokerTableEvent(PokerTableEvent.TYPE_OBSERVER_REMOVED, table);

            case GameEvent.CleaningDone e ->
                new PokerTableEvent(PokerTableEvent.TYPE_CLEANING_DONE, table);

            case GameEvent.PlayerEliminated e ->
                null; // No direct legacy equivalent

            case GameEvent.ActionTimeout ignored ->
                null; // Client handles ACTION_TIMEOUT via WebSocket message, not legacy event

            case GameEvent.RebuyOffered ignored ->
                null; // Client handles REBUY_OFFERED via WebSocket message

            case GameEvent.AddonOffered ignored ->
                null; // Client handles ADDON_OFFERED via WebSocket message

case GameEvent.ChipsTransferred e ->
                new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED, table);

            case GameEvent.ColorUpStarted e ->
                new PokerTableEvent(PokerTableEvent.TYPE_STATE_CHANGED, table);
        };
    }

    /**
     * Extract table ID from event record.
     */
    private int getTableId(GameEvent event) {
        return switch (event) {
            case GameEvent.HandStarted e -> e.tableId();
            case GameEvent.PlayerActed e -> e.tableId();
            case GameEvent.CommunityCardsDealt e -> e.tableId();
            case GameEvent.HandCompleted e -> e.tableId();
            case GameEvent.TableStateChanged e -> e.tableId();
            case GameEvent.PlayerAdded e -> e.tableId();
            case GameEvent.PlayerRemoved e -> e.tableId();
            case GameEvent.ButtonMoved e -> e.tableId();
            case GameEvent.ShowdownStarted e -> e.tableId();
            case GameEvent.PotAwarded e -> e.tableId();
            case GameEvent.BreakStarted e -> e.tableId();
            case GameEvent.BreakEnded e -> e.tableId();
            case GameEvent.ColorUpCompleted e -> e.tableId();
            case GameEvent.CurrentPlayerChanged e -> e.tableId();
            case GameEvent.PlayerRebuy e -> e.tableId();
            case GameEvent.PlayerAddon e -> e.tableId();
            case GameEvent.ObserverAdded e -> e.tableId();
            case GameEvent.ObserverRemoved e -> e.tableId();
            case GameEvent.CleaningDone e -> e.tableId();
            case GameEvent.LevelChanged e -> -1; // No tableId for game-level events
            case GameEvent.TournamentCompleted e -> -1; // No tableId for game-level events
            case GameEvent.PlayerEliminated e -> e.tableId();
            case GameEvent.ActionTimeout e -> -1; // No tableId for timeout events
            case GameEvent.RebuyOffered e -> e.tableId();
            case GameEvent.AddonOffered e -> e.tableId();
case GameEvent.ChipsTransferred e -> e.tableId();
            case GameEvent.ColorUpStarted e -> e.tableId();
        };
    }
}
