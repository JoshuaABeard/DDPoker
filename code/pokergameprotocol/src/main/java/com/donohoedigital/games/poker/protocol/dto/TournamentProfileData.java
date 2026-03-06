/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.poker.protocol.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Client-safe tournament profile data, replacing the pokerengine
 * TournamentProfile JPA entity for client display.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TournamentProfileData(
        // Basic info
        String name, String description, String greeting, int maxPlayers, int maxOnlinePlayers, int seats,
        boolean fillComputer, int buyin, int buyinChips,
        // Blind structure
        List<BlindLevelData> blindLevels, boolean doubleAfterLastLevel, String defaultGameType, String levelAdvanceMode,
        int handsPerLevel, int defaultMinutesPerLevel,
        // Rebuys
        boolean rebuysEnabled, int rebuyCost, int rebuyChips, int rebuyChipCount, int maxRebuys, int lastRebuyLevel,
        String rebuyExpressionType,
        // Addons
        boolean addonsEnabled, int addonCost, int addonChips, int addonLevel,
        // Payout
        String payoutType, double payoutPercent, int prizePool, int numSpots, List<Double> spotAllocations,
        String allocationType,
        // House
        String houseCutType, double housePercent, int houseAmount,
        // Bounty
        boolean bountyEnabled, int bountyAmount,
        // Timeouts
        int timeoutSeconds, int timeoutPreflop, int timeoutFlop, int timeoutTurn, int timeoutRiver,
        int thinkBankSeconds,
        // Boot
        boolean bootSitout, int bootSitoutCount, boolean bootDisconnect, int bootDisconnectCount,
        // Online
        boolean allowDash, boolean allowAdvisor, int maxObservers,
        // Betting
        int maxRaises, boolean raiseCapIgnoredHeadsUp,
        // Late registration
        boolean lateRegEnabled, int lateRegUntilLevel, String lateRegChipMode,
        // Scheduled start
        boolean scheduledStartEnabled, Instant startTime, int minPlayersForStart,
        // Invite
        boolean inviteOnly, List<String> invitees, boolean observersPublic) {
}
