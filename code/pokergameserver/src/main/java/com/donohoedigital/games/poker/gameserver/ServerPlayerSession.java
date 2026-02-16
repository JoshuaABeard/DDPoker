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

import java.time.Instant;
import java.util.function.Consumer;

/**
 * Represents one player's connection state within a game instance. Tracks
 * connection status, WebSocket message sender, and timeout information.
 *
 * <p>
 * Used by GameInstance to manage player sessions. WebSocket handlers set the
 * message sender callback when a player connects.
 */
public class ServerPlayerSession {
    private final long profileId;
    private final String playerName;
    private final boolean isAI;
    private final int skillLevel;

    private volatile boolean connected;
    private volatile boolean disconnected;
    private volatile Instant disconnectedAt;
    private volatile Consumer<Object> messageSender; // WebSocket message callback
    private volatile int consecutiveTimeouts;

    /**
     * Create a new player session.
     *
     * @param profileId
     *            unique profile ID
     * @param playerName
     *            player's display name
     * @param isAI
     *            true if this is an AI player
     * @param skillLevel
     *            AI skill level (1-10), or 0 for human players
     */
    public ServerPlayerSession(long profileId, String playerName, boolean isAI, int skillLevel) {
        this.profileId = profileId;
        this.playerName = playerName;
        this.isAI = isAI;
        this.skillLevel = skillLevel;
        this.connected = false;
        this.disconnected = false;
        this.consecutiveTimeouts = 0;
    }

    /**
     * Mark this session as connected. Clears disconnected state.
     */
    public void connect() {
        this.connected = true;
        this.disconnected = false;
        this.disconnectedAt = null;
    }

    /**
     * Mark this session as disconnected. Records the disconnection timestamp and
     * clears the message sender.
     */
    public void disconnect() {
        this.connected = false;
        if (!this.disconnected) {
            this.disconnected = true;
            this.disconnectedAt = Instant.now();
        }
        this.messageSender = null;
    }

    /**
     * Increment the consecutive timeout counter.
     */
    public void incrementConsecutiveTimeouts() {
        this.consecutiveTimeouts++;
    }

    /**
     * Reset the consecutive timeout counter (called when player acts successfully).
     */
    public void resetConsecutiveTimeouts() {
        this.consecutiveTimeouts = 0;
    }

    // === Getters ===

    public long getProfileId() {
        return profileId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isAI() {
        return isAI;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public Instant getDisconnectedAt() {
        return disconnectedAt;
    }

    public Consumer<Object> getMessageSender() {
        return messageSender;
    }

    public void setMessageSender(Consumer<Object> messageSender) {
        this.messageSender = messageSender;
    }

    public int getConsecutiveTimeouts() {
        return consecutiveTimeouts;
    }

    @Override
    public String toString() {
        return String.format("ServerPlayerSession[id=%d, name=%s, AI=%b, connected=%b]", profileId, playerName, isAI,
                connected);
    }
}
