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

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Clean server-side tournament configuration. This is the canonical
 * configuration model for the poker game server, serving triple duty:
 *
 * <ol>
 * <li>REST API contract - JSON request/response body for game creation</li>
 * <li>Internal model - GameInstance and ServerTournamentContext consume it
 * directly</li>
 * <li>Database storage - serialized as JSON in
 * GameInstanceEntity.profileData</li>
 * </ol>
 *
 * <p>
 * This model replaces {@code TournamentProfile} for server use. The legacy
 * TournamentProfile uses {@code DMTypedHashMap} internally and is deeply tied
 * to DD Poker's BaseProfile/ConfigManager framework. Desktop clients convert
 * their internal TournamentProfile to GameConfig for API calls.
 * </p>
 *
 * <p>
 * Null values for optional nested configs (rebuys, addons, house, bounty,
 * lateRegistration, scheduledStart) indicate the feature is disabled.
 * </p>
 *
 * @param name
 *            Tournament name
 * @param description
 *            Tournament description
 * @param greeting
 *            Greeting message (supports ${name} variable)
 * @param maxPlayers
 *            Maximum players (2-5625)
 * @param maxOnlinePlayers
 *            Maximum online players (default: 90, max: 120)
 * @param fillComputer
 *            Fill empty seats with AI (default: true)
 * @param buyIn
 *            Cost in dollars (1-1,000,000)
 * @param startingChips
 *            Chips per buy-in (1-1,000,000)
 * @param blindStructure
 *            Ordered list of blind levels (0-indexed)
 * @param doubleAfterLastLevel
 *            Auto-double blinds beyond last level (default: true)
 * @param defaultGameType
 *            Default game type for levels: "NOLIMIT_HOLDEM" etc.
 * @param levelAdvanceMode
 *            TIME or HANDS (default: TIME)
 * @param handsPerLevel
 *            Hands per level when mode=HANDS (1-100, default: 10)
 * @param defaultMinutesPerLevel
 *            Default duration for levels
 * @param rebuys
 *            Rebuy configuration (null = disabled)
 * @param addons
 *            Add-on configuration (null = disabled)
 * @param payout
 *            Payout structure
 * @param house
 *            House take configuration (null = no take)
 * @param bounty
 *            Bounty configuration (null = disabled)
 * @param timeouts
 *            Timeout configuration
 * @param boot
 *            Boot configuration
 * @param lateRegistration
 *            Late registration config (null = disabled)
 * @param scheduledStart
 *            Scheduled start config (null = disabled)
 * @param invite
 *            Invite/observer configuration
 * @param betting
 *            Betting rules configuration
 * @param allowDash
 *            Allow dashboard during play (default: false)
 * @param allowAdvisor
 *            Allow AI advisor during play (default: false)
 * @param aiPlayers
 *            AI player configurations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GameConfig(String name, String description, String greeting, int maxPlayers, int maxOnlinePlayers,
        boolean fillComputer, int buyIn, int startingChips, List<BlindLevel> blindStructure,
        boolean doubleAfterLastLevel, String defaultGameType, LevelAdvanceMode levelAdvanceMode, int handsPerLevel,
        int defaultMinutesPerLevel, RebuyConfig rebuys, AddonConfig addons, PayoutConfig payout, HouseConfig house,
        BountyConfig bounty, TimeoutConfig timeouts, BootConfig boot, LateRegistrationConfig lateRegistration,
        ScheduledStartConfig scheduledStart, InviteConfig invite, BettingConfig betting, boolean allowDash,
        boolean allowAdvisor, List<AIPlayerConfig> aiPlayers) {

    /**
     * Validate this configuration.
     *
     * @throws IllegalArgumentException
     *             if any validation fails
     */
    public void validate() {
        if (maxPlayers < 2 || maxPlayers > 5625) {
            throw new IllegalArgumentException("maxPlayers must be between 2 and 5625, got: " + maxPlayers);
        }
        if (startingChips < 1 || startingChips > 1_000_000) {
            throw new IllegalArgumentException("startingChips must be between 1 and 1,000,000, got: " + startingChips);
        }
        if (blindStructure == null || blindStructure.isEmpty()) {
            throw new IllegalArgumentException("blind structure must have at least one non-break level");
        }
        long nonBreakLevels = blindStructure.stream().filter(bl -> !bl.isBreak()).count();
        if (nonBreakLevels == 0) {
            throw new IllegalArgumentException("blind structure must have at least one non-break level");
        }
        if (aiPlayers != null) {
            for (AIPlayerConfig ai : aiPlayers) {
                if (ai.skillLevel() < 1 || ai.skillLevel() > 7) {
                    throw new IllegalArgumentException(
                            "AI skill level must be between 1 and 7, got: " + ai.skillLevel());
                }
            }
        }
    }

    // Nested records for configuration sections

    /**
     * Blind level configuration.
     *
     * @param smallBlind
     *            Small blind amount
     * @param bigBlind
     *            Big blind amount
     * @param ante
     *            Ante amount (-1 indicates break level in legacy code, but use
     *            isBreak field)
     * @param minutes
     *            Duration in minutes (or break duration)
     * @param isBreak
     *            True if this is a break level
     * @param gameType
     *            Game type ("NOLIMIT_HOLDEM", "POTLIMIT_HOLDEM", "LIMIT_HOLDEM") -
     *            null for breaks
     */
    public record BlindLevel(int smallBlind, int bigBlind, int ante, int minutes, boolean isBreak, String gameType) {
    }

    /**
     * Level advancement mode.
     */
    public enum LevelAdvanceMode {
        TIME, HANDS
    }

    /**
     * Rebuy configuration.
     *
     * @param enabled
     *            Rebuys enabled
     * @param cost
     *            Cost per rebuy in dollars
     * @param chips
     *            Chips per rebuy
     * @param chipCount
     *            Rebuy chip count override (default: startingChips)
     * @param maxRebuys
     *            Max rebuys per player (0-99)
     * @param lastLevel
     *            Last level allowing rebuys (0-indexed)
     * @param expressionType
     *            "LESS_THAN" or "LESS_THAN_OR_EQUAL"
     */
    public record RebuyConfig(boolean enabled, int cost, int chips, int chipCount, int maxRebuys, int lastLevel,
            String expressionType) {
    }

    /**
     * Add-on configuration.
     *
     * @param enabled
     *            Add-ons enabled
     * @param cost
     *            Cost in dollars
     * @param chips
     *            Chips received
     * @param level
     *            Level at which addon is offered (0-indexed)
     */
    public record AddonConfig(boolean enabled, int cost, int chips, int level) {
    }

    /**
     * Payout configuration.
     *
     * @param type
     *            "SPOTS", "PERCENT", or "SATELLITE"
     * @param spots
     *            Number of paid positions (for SPOTS, 1-560)
     * @param percent
     *            Percent of players paid (for PERCENT, 1-100)
     * @param prizePool
     *            Total prize pool in dollars
     * @param allocationType
     *            "AUTO", "FIXED", "PERCENT", or "SATELLITE"
     * @param spotAllocations
     *            Per-position payouts (% or $ depending on allocationType)
     */
    public record PayoutConfig(String type, int spots, int percent, int prizePool, String allocationType,
            List<Double> spotAllocations) {
    }

    /**
     * House take configuration.
     *
     * @param cutType
     *            "AMOUNT" or "PERCENT"
     * @param percent
     *            Percentage (0-25%)
     * @param amount
     *            Fixed dollar amount (0-9999)
     */
    public record HouseConfig(String cutType, int percent, int amount) {
    }

    /**
     * Bounty configuration.
     *
     * @param enabled
     *            Bounties enabled
     * @param amount
     *            $ per knockout (0-10000)
     */
    public record BountyConfig(boolean enabled, int amount) {
    }

    /**
     * Timeout configuration.
     *
     * @param defaultSeconds
     *            General timeout (5-120, default: 30)
     * @param preflopSeconds
     *            Preflop timeout (0 = use default)
     * @param flopSeconds
     *            Flop timeout (0 = use default)
     * @param turnSeconds
     *            Turn timeout (0 = use default)
     * @param riverSeconds
     *            River timeout (0 = use default)
     * @param thinkBankSeconds
     *            Bank time per player (0-120, default: 15)
     */
    public record TimeoutConfig(int defaultSeconds, int preflopSeconds, int flopSeconds, int turnSeconds,
            int riverSeconds, int thinkBankSeconds) {
    }

    /**
     * Boot configuration.
     *
     * @param bootSitout
     *            Boot players who sit out
     * @param bootSitoutCount
     *            Hands before boot (5-100, default: 25)
     * @param bootDisconnect
     *            Boot disconnected players (default: true)
     * @param bootDisconnectCount
     *            Hands before boot (5-100, default: 10)
     */
    public record BootConfig(boolean bootSitout, int bootSitoutCount, boolean bootDisconnect, int bootDisconnectCount) {
    }

    /**
     * Late registration configuration.
     *
     * @param enabled
     *            Late registration enabled
     * @param untilLevel
     *            Last level for late reg (1-40)
     * @param chipMode
     *            "STARTING" or "AVERAGE"
     */
    public record LateRegistrationConfig(boolean enabled, int untilLevel, String chipMode) {
    }

    /**
     * Scheduled start configuration.
     *
     * @param enabled
     *            Scheduled start enabled
     * @param startTime
     *            Scheduled start time
     * @param minPlayers
     *            Minimum players before start (2-120)
     */
    public record ScheduledStartConfig(boolean enabled, Instant startTime, int minPlayers) {
    }

    /**
     * Invite/observer configuration.
     *
     * @param inviteOnly
     *            Restrict to invited players
     * @param invitees
     *            Invited player names
     * @param observersPublic
     *            Allow public observation
     */
    public record InviteConfig(boolean inviteOnly, List<String> invitees, boolean observersPublic) {
    }

    /**
     * Betting rules configuration.
     *
     * @param maxRaises
     *            Max raises per round (0 = unlimited)
     * @param raiseCapIgnoredHeadsUp
     *            Ignore raise cap heads-up (default: true)
     */
    public record BettingConfig(int maxRaises, boolean raiseCapIgnoredHeadsUp) {
    }

    /**
     * AI player configuration.
     *
     * @param name
     *            AI player name
     * @param skillLevel
     *            Skill level (1-7, maps to TournamentAI/V1/V2)
     */
    public record AIPlayerConfig(String name, int skillLevel) {
    }

    /**
     * Create a copy of this config with a different blind structure. Useful for
     * testing and modifications.
     */
    public GameConfig withBlindStructure(List<BlindLevel> newBlindStructure) {
        return new GameConfig(name, description, greeting, maxPlayers, maxOnlinePlayers, fillComputer, buyIn,
                startingChips, newBlindStructure, doubleAfterLastLevel, defaultGameType, levelAdvanceMode,
                handsPerLevel, defaultMinutesPerLevel, rebuys, addons, payout, house, bounty, timeouts, boot,
                lateRegistration, scheduledStart, invite, betting, allowDash, allowAdvisor, aiPlayers);
    }

    /**
     * Create a copy of this config with a different maxPlayers value. Useful for
     * testing and modifications.
     */
    public GameConfig withMaxPlayers(int newMaxPlayers) {
        return new GameConfig(name, description, greeting, newMaxPlayers, maxOnlinePlayers, fillComputer, buyIn,
                startingChips, blindStructure, doubleAfterLastLevel, defaultGameType, levelAdvanceMode, handsPerLevel,
                defaultMinutesPerLevel, rebuys, addons, payout, house, bounty, timeouts, boot, lateRegistration,
                scheduledStart, invite, betting, allowDash, allowAdvisor, aiPlayers);
    }

    /**
     * Create a copy of this config with different AI players. Useful for testing
     * and modifications.
     */
    public GameConfig withAiPlayers(List<AIPlayerConfig> newAiPlayers) {
        return new GameConfig(name, description, greeting, maxPlayers, maxOnlinePlayers, fillComputer, buyIn,
                startingChips, blindStructure, doubleAfterLastLevel, defaultGameType, levelAdvanceMode, handsPerLevel,
                defaultMinutesPerLevel, rebuys, addons, payout, house, bounty, timeouts, boot, lateRegistration,
                scheduledStart, invite, betting, allowDash, allowAdvisor, newAiPlayers);
    }
}
