/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a pot (main pot or side pot) in a poker hand. Tracks chips,
 * eligible players, and winners.
 */
public class ServerPot {
    private int chips;
    private final List<ServerPlayer> eligiblePlayers = new ArrayList<>();
    private final List<ServerPlayer> winners = new ArrayList<>();
    private final int round; // Round pot was created
    private final int sideBet; // Incremental all-in cap for this pot (0 = main pot, takes all remaining chips)

    /**
     * Create a new pot.
     *
     * @param round
     *            betting round when pot was created
     * @param sideBet
     *            incremental all-in cap (0 for main pot which takes all remaining chips)
     */
    public ServerPot(int round, int sideBet) {
        this.chips = 0;
        this.round = round;
        this.sideBet = sideBet;
    }

    /**
     * Add chips to this pot from a player.
     *
     * @param player
     *            player contributing chips
     * @param amount
     *            chips to add
     */
    public void addChips(ServerPlayer player, int amount) {
        this.chips += amount;
        if (!eligiblePlayers.contains(player)) {
            eligiblePlayers.add(player);
        }
    }

    /**
     * Check if a player is eligible to win this pot.
     *
     * @param player
     *            player to check
     * @return true if player contributed to this pot
     */
    public boolean isPlayerEligible(ServerPlayer player) {
        return eligiblePlayers.contains(player);
    }

    /**
     * Check if this pot is an overbet (only one eligible player).
     *
     * @return true if only one player is eligible (uncalled bet)
     */
    public boolean isOverbet() {
        return eligiblePlayers.size() == 1;
    }

    /**
     * Add a winner for this pot.
     *
     * @param player
     *            winning player
     */
    public void addWinner(ServerPlayer player) {
        winners.add(player);
    }

    /**
     * Reset pot chips and eligible players (clears for recalculation).
     */
    public void reset() {
        this.chips = 0;
        this.eligiblePlayers.clear();
        this.winners.clear();
    }

    /**
     * Get total chips in this pot.
     *
     * @return chip count
     */
    public int getChips() {
        return chips;
    }

    /**
     * Get list of players eligible to win this pot.
     *
     * @return eligible players (unmodifiable)
     */
    public List<ServerPlayer> getEligiblePlayers() {
        return List.copyOf(eligiblePlayers);
    }

    /**
     * Get list of winners for this pot.
     *
     * @return winners (may be empty if pot not yet resolved)
     */
    public List<ServerPlayer> getWinners() {
        return List.copyOf(winners);
    }

    /**
     * Get round when this pot was created.
     *
     * @return round number
     */
    public int getRound() {
        return round;
    }

    /**
     * Get incremental all-in cap for this pot.
     *
     * @return incremental cap (0 for main pot, which takes all remaining chips)
     */
    public int getSideBet() {
        return sideBet;
    }

    @Override
    public String toString() {
        return "ServerPot{chips=" + chips + ", round=" + round + ", sideBet=" + sideBet + ", eligible="
                + eligiblePlayers.size() + ", winners=" + winners.size() + "}";
    }
}
