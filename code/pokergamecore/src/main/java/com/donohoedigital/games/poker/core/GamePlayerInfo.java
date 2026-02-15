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
package com.donohoedigital.games.poker.core;

/**
 * Interface for player information that the core game engine needs. Implemented
 * by PokerPlayer in Phase 2.
 */
public interface GamePlayerInfo {
    /** @return unique player ID */
    int getID();

    /** @return player's display name */
    String getName();

    /** @return true if this is a human player, false for AI */
    boolean isHuman();

    /** @return current chip count */
    int getChipCount();

    /** @return true if player has folded this hand */
    boolean isFolded();

    /** @return true if player is all-in */
    boolean isAllIn();

    /** @return player's seat number at the table */
    int getSeat();

    /** @return true if player wants to be asked before showing winning cards */
    boolean isAskShowWinning();

    /** @return true if player wants to be asked before showing losing cards */
    boolean isAskShowLosing();

    /** @return true if this player is an observer (not playing) */
    boolean isObserver();

    /** @return true if this player is currently controlled by a human */
    boolean isHumanControlled();

    /** @return think bank time remaining in milliseconds */
    int getThinkBankMillis();

    /** @return true if player is sitting out */
    boolean isSittingOut();

    /**
     * @param sittingOut
     *            true if player should sit out
     */
    void setSittingOut(boolean sittingOut);

    /** @return true if this player is locally controlled (local human or AI) */
    boolean isLocallyControlled();

    /** @return true if this is a computer/AI player */
    boolean isComputer();

    /**
     * Set timeout milliseconds for this player.
     *
     * @param millis
     *            timeout in milliseconds
     */
    void setTimeoutMillis(int millis);

    /**
     * Set timeout message warning countdown.
     *
     * @param seconds
     *            seconds left before warning
     */
    void setTimeoutMessageSecondsLeft(int seconds);
}
