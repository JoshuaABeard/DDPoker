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

import com.donohoedigital.games.poker.core.GamePlayerInfo;

/**
 * Server-side player implementation. Implements GamePlayerInfo without Swing
 * dependencies. Replaces PokerPlayer for server-side game hosting.
 *
 * Simple data class with no GamePlayer inheritance.
 */
public class ServerPlayer implements GamePlayerInfo {
    private final int id;
    private String name;
    private final boolean human;
    private int skillLevel; // AI skill level (0 for humans)

    private int chipCount;
    private int seat;
    private boolean folded;
    private boolean allIn;
    private boolean sittingOut;
    private boolean observer;
    private int numRebuys;
    private int finishPosition; // 0 = still in tournament
    private int oddChips; // fractional chips in current color-up race

    // Think bank for timed tournaments
    private int thinkBankMillis;
    private int timeoutMillis;
    private int timeoutMessageSecondsLeft;

    // Card-showing preferences (defaults for server)
    private boolean askShowWinning = false;
    private boolean askShowLosing = false;

    /**
     * Create a new server player.
     *
     * @param id
     *            unique player ID
     * @param name
     *            player's display name
     * @param human
     *            true if human player, false for AI
     * @param skillLevel
     *            AI skill level (0 for humans, 1-10 for AI)
     * @param startingChips
     *            initial chip count
     */
    public ServerPlayer(int id, String name, boolean human, int skillLevel, int startingChips) {
        this.id = id;
        this.name = name;
        this.human = human;
        this.skillLevel = skillLevel;
        this.chipCount = startingChips;
        this.seat = -1; // Not seated initially
        this.folded = false;
        this.allIn = false;
        this.sittingOut = false;
        this.observer = false;
        this.numRebuys = 0;
        this.thinkBankMillis = 0;
        this.timeoutMillis = 0;
        this.timeoutMessageSecondsLeft = 0;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isHuman() {
        return human;
    }

    @Override
    public int getChipCount() {
        return chipCount;
    }

    @Override
    public boolean isFolded() {
        return folded;
    }

    @Override
    public boolean isAllIn() {
        return allIn;
    }

    @Override
    public int getSeat() {
        return seat;
    }

    @Override
    public boolean isAskShowWinning() {
        return askShowWinning;
    }

    @Override
    public boolean isAskShowLosing() {
        return askShowLosing;
    }

    @Override
    public boolean isObserver() {
        return observer;
    }

    @Override
    public boolean isHumanControlled() {
        return human;
    }

    @Override
    public int getThinkBankMillis() {
        return thinkBankMillis;
    }

    @Override
    public boolean isSittingOut() {
        return sittingOut;
    }

    @Override
    public void setSittingOut(boolean sittingOut) {
        this.sittingOut = sittingOut;
    }

    @Override
    public boolean isLocallyControlled() {
        // In server mode, all players are "locally controlled" by the server
        return true;
    }

    @Override
    public boolean isComputer() {
        return !human;
    }

    @Override
    public void setTimeoutMillis(int millis) {
        this.timeoutMillis = millis;
    }

    @Override
    public void setTimeoutMessageSecondsLeft(int seconds) {
        this.timeoutMessageSecondsLeft = seconds;
    }

    @Override
    public int getNumRebuys() {
        return numRebuys;
    }

    // Additional setters and methods

    public void setChipCount(int chipCount) {
        this.chipCount = chipCount;
    }

    public void addChips(int amount) {
        this.chipCount += amount;
    }

    public void subtractChips(int amount) {
        this.chipCount -= amount;
    }

    public void setSeat(int seat) {
        this.seat = seat;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public void setAllIn(boolean allIn) {
        this.allIn = allIn;
    }

    public void setObserver(boolean observer) {
        this.observer = observer;
    }

    public void setThinkBankMillis(int millis) {
        this.thinkBankMillis = millis;
    }

    public void setAskShowWinning(boolean ask) {
        this.askShowWinning = ask;
    }

    public void setAskShowLosing(boolean ask) {
        this.askShowLosing = ask;
    }

    public void incrementRebuys() {
        this.numRebuys++;
    }

    public int getFinishPosition() {
        return finishPosition;
    }

    public void setFinishPosition(int finishPosition) {
        this.finishPosition = finishPosition;
    }

    public int getOddChips() {
        return oddChips;
    }

    public void setOddChips(int oddChips) {
        this.oddChips = oddChips;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSkillLevel(int skillLevel) {
        this.skillLevel = skillLevel;
    }

    @Override
    public String toString() {
        return "ServerPlayer{id=" + id + ", name='" + name + "', chips=" + chipCount + ", seat=" + seat + "}";
    }
}
