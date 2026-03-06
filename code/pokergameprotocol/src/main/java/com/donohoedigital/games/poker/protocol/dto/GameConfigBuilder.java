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

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link GameConfig} from a {@link TournamentProfileData} record.
 *
 * <p>
 * This is the client-side equivalent of {@code TournamentProfileConverter} in
 * pokergameserver, but sources from the flat {@link TournamentProfileData}
 * record instead of the pokerengine {@code TournamentProfile} entity.
 */
public final class GameConfigBuilder {

    private GameConfigBuilder() {
        // utility class
    }

    /**
     * Convert a {@link TournamentProfileData} to a {@link GameConfig}.
     *
     * <p>
     * The returned config has {@code aiPlayers = null},
     * {@code humanDisplayName = null}, and {@code practiceConfig = null}. Callers
     * should chain the appropriate {@code with*} methods on {@link GameConfig} to
     * set those.
     *
     * @param profile
     *            the tournament profile data
     * @return game configuration
     */
    public static GameConfig fromProfile(TournamentProfileData profile) {
        return new GameConfig(profile.name(), profile.description(), normalizeGreeting(profile.greeting()),
                profile.maxPlayers(), profile.maxOnlinePlayers(), profile.fillComputer(), profile.buyin(),
                profile.buyinChips(), convertBlindStructure(profile.blindLevels()), profile.doubleAfterLastLevel(),
                profile.defaultGameType(), convertLevelAdvanceMode(profile.levelAdvanceMode()), profile.handsPerLevel(),
                profile.defaultMinutesPerLevel(), convertRebuys(profile), convertAddons(profile),
                convertPayout(profile), convertHouse(profile), convertBounty(profile), convertTimeouts(profile),
                convertBoot(profile), convertLateRegistration(profile), convertScheduledStart(profile),
                convertInvite(profile), convertBetting(profile), profile.allowDash(), profile.allowAdvisor(), null, // aiPlayers
                                                                                                                    // set
                                                                                                                    // separately
                null, // humanDisplayName set separately
                null // practiceConfig set separately
        );
    }

    /**
     * Build a list of {@link GameConfig.AIPlayerConfig} records.
     *
     * @param profile
     *            source profile (reserved for future use)
     * @param aiNames
     *            ordered list of AI player display names
     * @param defaultSkillLevel
     *            skill level to assign (1-7)
     * @return list of AI player configs, one per entry in {@code aiNames}
     */
    public static List<GameConfig.AIPlayerConfig> buildAiPlayers(TournamentProfileData profile, List<String> aiNames,
            int defaultSkillLevel) {
        List<GameConfig.AIPlayerConfig> result = new ArrayList<>();
        for (String name : aiNames) {
            result.add(new GameConfig.AIPlayerConfig(name, defaultSkillLevel));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private conversion helpers
    // -------------------------------------------------------------------------

    private static String normalizeGreeting(String greeting) {
        return (greeting == null || greeting.trim().isEmpty()) ? null : greeting.trim();
    }

    private static List<GameConfig.BlindLevel> convertBlindStructure(List<BlindLevelData> blindLevels) {
        List<GameConfig.BlindLevel> levels = new ArrayList<>();
        if (blindLevels == null) {
            return levels;
        }
        for (BlindLevelData bl : blindLevels) {
            if (bl.isBreak()) {
                levels.add(new GameConfig.BlindLevel(0, 0, 0, bl.minutes(), true, null));
            } else {
                levels.add(new GameConfig.BlindLevel(bl.smallBlind(), bl.bigBlind(), bl.ante(), bl.minutes(), false,
                        bl.gameType()));
            }
        }
        return levels;
    }

    private static GameConfig.LevelAdvanceMode convertLevelAdvanceMode(String mode) {
        return "HANDS".equals(mode) ? GameConfig.LevelAdvanceMode.HANDS : GameConfig.LevelAdvanceMode.TIME;
    }

    private static GameConfig.RebuyConfig convertRebuys(TournamentProfileData profile) {
        if (!profile.rebuysEnabled()) {
            return null;
        }
        return new GameConfig.RebuyConfig(true, profile.rebuyCost(), profile.rebuyChips(), profile.rebuyChipCount(),
                profile.maxRebuys(), profile.lastRebuyLevel(), profile.rebuyExpressionType());
    }

    private static GameConfig.AddonConfig convertAddons(TournamentProfileData profile) {
        if (!profile.addonsEnabled()) {
            return null;
        }
        return new GameConfig.AddonConfig(true, profile.addonCost(), profile.addonChips(), profile.addonLevel());
    }

    private static GameConfig.PayoutConfig convertPayout(TournamentProfileData profile) {
        return new GameConfig.PayoutConfig(profile.payoutType(), profile.numSpots(), (int) profile.payoutPercent(),
                profile.prizePool(), profile.allocationType(), profile.spotAllocations());
    }

    private static GameConfig.HouseConfig convertHouse(TournamentProfileData profile) {
        int perc = (int) profile.housePercent();
        int amount = profile.houseAmount();
        if (perc == 0 && amount == 0) {
            return null;
        }
        return new GameConfig.HouseConfig(profile.houseCutType(), perc, amount);
    }

    private static GameConfig.BountyConfig convertBounty(TournamentProfileData profile) {
        if (!profile.bountyEnabled()) {
            return null;
        }
        return new GameConfig.BountyConfig(true, profile.bountyAmount());
    }

    private static GameConfig.TimeoutConfig convertTimeouts(TournamentProfileData profile) {
        return new GameConfig.TimeoutConfig(profile.timeoutSeconds(), profile.timeoutPreflop(), profile.timeoutFlop(),
                profile.timeoutTurn(), profile.timeoutRiver(), profile.thinkBankSeconds());
    }

    private static GameConfig.BootConfig convertBoot(TournamentProfileData profile) {
        return new GameConfig.BootConfig(profile.bootSitout(), profile.bootSitoutCount(), profile.bootDisconnect(),
                profile.bootDisconnectCount());
    }

    private static GameConfig.LateRegistrationConfig convertLateRegistration(TournamentProfileData profile) {
        if (!profile.lateRegEnabled()) {
            return null;
        }
        return new GameConfig.LateRegistrationConfig(true, profile.lateRegUntilLevel(), profile.lateRegChipMode());
    }

    private static GameConfig.ScheduledStartConfig convertScheduledStart(TournamentProfileData profile) {
        if (!profile.scheduledStartEnabled()) {
            return null;
        }
        return new GameConfig.ScheduledStartConfig(true, profile.startTime(), profile.minPlayersForStart());
    }

    private static GameConfig.InviteConfig convertInvite(TournamentProfileData profile) {
        if (!profile.inviteOnly()) {
            return null;
        }
        return new GameConfig.InviteConfig(true, profile.invitees(), profile.observersPublic());
    }

    private static GameConfig.BettingConfig convertBetting(TournamentProfileData profile) {
        return new GameConfig.BettingConfig(profile.maxRaises(), profile.raiseCapIgnoredHeadsUp());
    }
}
