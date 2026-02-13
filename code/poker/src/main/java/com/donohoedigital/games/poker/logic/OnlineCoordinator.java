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
package com.donohoedigital.games.poker.logic;

/**
 * Online game coordination logic extracted from TournamentDirector.java.
 * Contains pure business logic for network message routing, chat distribution,
 * and online/practice mode decisions with no UI dependencies. Part of Wave 3
 * testability refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>Chat message routing (online vs practice)</li>
 * <li>WAN game server interaction decisions</li>
 * <li>Remote vs local processing decisions</li>
 * </ul>
 */
public class OnlineCoordinator {

    // Utility class - no instantiation
    private OnlineCoordinator() {
    }

    /**
     * Message routing destination.
     */
    public enum MessageDestination {
        /** Send via online manager to remote clients */
        ONLINE_MANAGER,
        /** Deliver locally (practice mode or local echo) */
        LOCAL,
        /** No delivery needed */
        NONE
    }

    /**
     * Determine where to route dealer chat message.
     *
     * <p>
     * Extracted from TournamentDirector.sendDealerChat() lines 2658-2664.
     *
     * <p>
     * In online games with manager, route through manager. In practice or for
     * current table, deliver locally.
     *
     * @param hasOnlineManager
     *            true if online manager exists
     * @param isCurrentTable
     *            true if message is for current table
     * @return message routing destination
     */
    public static MessageDestination routeDealerChat(boolean hasOnlineManager, boolean isCurrentTable) {
        if (hasOnlineManager) {
            return MessageDestination.ONLINE_MANAGER;
        } else if (isCurrentTable) {
            return MessageDestination.LOCAL;
        } else {
            return MessageDestination.NONE;
        }
    }

    /**
     * Determine where to route director chat message.
     *
     * <p>
     * Extracted from TournamentDirector.sendDirectorChat() lines 2671-2675.
     *
     * <p>
     * Director messages always deliver (to all players), either via manager or
     * locally.
     *
     * @param hasOnlineManager
     *            true if online manager exists
     * @return message routing destination
     */
    public static MessageDestination routeDirectorChat(boolean hasOnlineManager) {
        return hasOnlineManager ? MessageDestination.ONLINE_MANAGER : MessageDestination.LOCAL;
    }

    /**
     * Determine if WAN server should be notified of game start.
     *
     * <p>
     * Extracted from TournamentDirector.startWanGame() lines 2704-2707.
     *
     * <p>
     * Only public online games hosted by the server notify WAN for
     * matchmaking/tracking.
     *
     * @param isOnline
     *            true if online game
     * @param isHost
     *            true if this is the host
     * @param isPublic
     *            true if public game (vs private)
     * @return true if should notify WAN server of game start
     */
    public static boolean shouldNotifyWanGameStart(boolean isOnline, boolean isHost, boolean isPublic) {
        return isOnline && isHost && isPublic;
    }

    /**
     * Determine if WAN server should be notified of game end.
     *
     * <p>
     * Extracted from TournamentDirector.endWanGame() lines 2718-2721.
     *
     * <p>
     * Same criteria as start - only public online games notify WAN.
     *
     * @param isOnline
     *            true if online game
     * @param isHost
     *            true if this is the host
     * @param isPublic
     *            true if public game (vs private)
     * @return true if should notify WAN server of game end
     */
    public static boolean shouldNotifyWanGameEnd(boolean isOnline, boolean isHost, boolean isPublic) {
        return isOnline && isHost && isPublic;
    }

    /**
     * Determine if action should be sent to online clients.
     *
     * <p>
     * Helper method for online action distribution decisions.
     *
     * @param isOnline
     *            true if online game
     * @param hasOnlineManager
     *            true if online manager exists
     * @return true if should send to online clients
     */
    public static boolean shouldSendToClients(boolean isOnline, boolean hasOnlineManager) {
        return isOnline && hasOnlineManager;
    }

    /**
     * Determine if should wait for remote client action.
     *
     * <p>
     * Helper method for remote player coordination.
     *
     * @param isHost
     *            true if this is the host
     * @param isRemotePlayer
     *            true if player is remote (not local)
     * @return true if should wait for client to send action
     */
    public static boolean shouldWaitForClient(boolean isHost, boolean isRemotePlayer) {
        // Only host waits for remote players
        return isHost && isRemotePlayer;
    }

    /**
     * Determine if message should only be sent to waitlisted players.
     *
     * <p>
     * Helper method for targeted message delivery.
     *
     * @param isOnline
     *            true if online game
     * @param isHost
     *            true if this is the host
     * @return true if should only send to waitlist (optimization)
     */
    public static boolean shouldSendOnlyToWaitlist(boolean isOnline, boolean isHost) {
        // In online mode, host can optimize by sending only to waiting players
        return isOnline && isHost;
    }
}
