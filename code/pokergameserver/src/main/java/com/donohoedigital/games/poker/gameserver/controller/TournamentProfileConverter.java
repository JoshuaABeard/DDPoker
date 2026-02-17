/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.controller;

import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.AddonConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.AIPlayerConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.BettingConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.BlindLevel;
import com.donohoedigital.games.poker.gameserver.GameConfig.BootConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.BountyConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.HouseConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.InviteConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.LateRegistrationConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.LevelAdvanceMode;
import com.donohoedigital.games.poker.gameserver.GameConfig.PayoutConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.RebuyConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.ScheduledStartConfig;
import com.donohoedigital.games.poker.gameserver.GameConfig.TimeoutConfig;
import com.donohoedigital.games.poker.model.TournamentProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a desktop {@link TournamentProfile} to a server {@link GameConfig}.
 *
 * <p>
 * Maps all blind structure levels, player count, starting chips, payout config,
 * rebuy/addon rules, timeout settings, boot settings, and optional features.
 * The {@code aiPlayers} field is not set here; supply them separately via
 * {@link GameConfig#withAiPlayers(List)} after calling {@link #convert}.
 */
public class TournamentProfileConverter {

    /**
     * Convert a {@link TournamentProfile} to a {@link GameConfig}.
     *
     * <p>
     * The returned config has {@code aiPlayers = null}. Callers that want AI
     * players should chain {@link GameConfig#withAiPlayers(List)}.
     *
     * @param profile
     *            the desktop tournament profile
     * @return server-side game configuration
     */
    public GameConfig convert(TournamentProfile profile) {
        return new GameConfig(profile.getName(), profile.getDescription(), rawGreeting(profile),
                profile.getNumPlayers(), profile.getMaxOnlinePlayers(), profile.isFillComputer(),
                profile.getBuyinCost(), profile.getBuyinChips(), convertBlindStructure(profile),
                profile.isDoubleAfterLastLevel(), convertGameType(profile.getDefaultGameTypeString()),
                convertLevelAdvanceMode(profile.getLevelAdvanceMode()), profile.getHandsPerLevel(),
                profile.getDefaultMinutesPerLevel(), convertRebuys(profile), convertAddons(profile),
                convertPayout(profile), convertHouse(profile), convertBounty(profile), convertTimeouts(profile),
                convertBoot(profile), convertLateRegistration(profile), convertScheduledStart(profile),
                convertInvite(profile), convertBetting(profile), profile.isOnlineActivatedPlayersOnly(),
                profile.isAllowDash(), profile.isAllowAdvisor(), null // aiPlayers set separately
        );
    }

    // -------------------------------------------------------------------------
    // Private conversion helpers
    // -------------------------------------------------------------------------

    private String rawGreeting(TournamentProfile profile) {
        String raw = profile.getMap().getString(TournamentProfile.PARAM_GREETING, "");
        return (raw == null || raw.trim().isEmpty()) ? null : raw.trim();
    }

    private List<BlindLevel> convertBlindStructure(TournamentProfile profile) {
        List<BlindLevel> levels = new ArrayList<>();
        int last = profile.getLastLevel();
        for (int i = 1; i <= last; i++) {
            if (profile.isBreak(i)) {
                levels.add(new BlindLevel(0, 0, 0, profile.getMinutes(i), true, null));
            } else {
                levels.add(new BlindLevel(profile.getSmallBlind(i), profile.getBigBlind(i), profile.getAnte(i),
                        profile.getMinutes(i), false, convertGameType(profile.getGameTypeString(i))));
            }
        }
        return levels;
    }

    private String convertGameType(String deType) {
        return switch (deType) {
            case PokerConstants.DE_POT_LIMIT_HOLDEM -> "POTLIMIT_HOLDEM";
            case PokerConstants.DE_LIMIT_HOLDEM -> "LIMIT_HOLDEM";
            default -> "NOLIMIT_HOLDEM";
        };
    }

    private LevelAdvanceMode convertLevelAdvanceMode(com.donohoedigital.games.poker.model.LevelAdvanceMode mode) {
        return mode == com.donohoedigital.games.poker.model.LevelAdvanceMode.HANDS
                ? LevelAdvanceMode.HANDS
                : LevelAdvanceMode.TIME;
    }

    private RebuyConfig convertRebuys(TournamentProfile profile) {
        if (!profile.isRebuys()) {
            return null;
        }
        int exprType = profile.getRebuyExpressionType();
        String expr = (exprType == PokerConstants.REBUY_LT) ? "LESS_THAN" : "LESS_THAN_OR_EQUAL";
        return new RebuyConfig(true, profile.getRebuyCost(), profile.getRebuyChips(), profile.getRebuyChipCount(),
                profile.getMaxRebuys(), profile.getLastRebuyLevel(), expr);
    }

    private AddonConfig convertAddons(TournamentProfile profile) {
        if (!profile.isAddons()) {
            return null;
        }
        return new AddonConfig(true, profile.getAddonCost(), profile.getAddonChips(), profile.getAddonLevel());
    }

    private PayoutConfig convertPayout(TournamentProfile profile) {
        int type = profile.getPayoutType();
        String typeName = switch (type) {
            case PokerConstants.PAYOUT_PERC -> "PERCENT";
            case PokerConstants.PAYOUT_SATELLITE -> "SATELLITE";
            default -> "SPOTS";
        };
        String allocType;
        if (profile.isAllocSatellite()) {
            allocType = "SATELLITE";
        } else if (profile.isAllocPercent()) {
            allocType = "PERCENT";
        } else if (profile.isAllocFixed()) {
            allocType = "FIXED";
        } else {
            allocType = "AUTO";
        }
        int numSpots = profile.getNumSpots();
        List<Double> spotAllocations = new ArrayList<>();
        for (int i = 1; i <= numSpots; i++) {
            spotAllocations.add(profile.getSpot(i));
        }
        return new PayoutConfig(typeName, numSpots, profile.getPayoutPercent(), profile.getPrizePool(), allocType,
                spotAllocations);
    }

    private HouseConfig convertHouse(TournamentProfile profile) {
        int perc = profile.getHousePercent();
        int amount = profile.getHouseAmount();
        if (perc == 0 && amount == 0) {
            return null;
        }
        int type = profile.getHouseCutType();
        String cutType = (type == PokerConstants.HOUSE_AMOUNT) ? "AMOUNT" : "PERCENT";
        return new HouseConfig(cutType, perc, amount);
    }

    private BountyConfig convertBounty(TournamentProfile profile) {
        if (!profile.isBountyEnabled()) {
            return null;
        }
        return new BountyConfig(true, profile.getBountyAmount());
    }

    private TimeoutConfig convertTimeouts(TournamentProfile profile) {
        return new TimeoutConfig(profile.getTimeoutSeconds(), profile.getTimeoutPreflop(), profile.getTimeoutFlop(),
                profile.getTimeoutTurn(), profile.getTimeoutRiver(), profile.getThinkBankSeconds());
    }

    private BootConfig convertBoot(TournamentProfile profile) {
        return new BootConfig(profile.isBootSitout(), profile.getBootSitoutCount(), profile.isBootDisconnect(),
                profile.getBootDisconnectCount());
    }

    private LateRegistrationConfig convertLateRegistration(TournamentProfile profile) {
        if (!profile.isLateRegEnabled()) {
            return null;
        }
        int chipMode = profile.getLateRegChips();
        String chipModeStr = (chipMode == PokerConstants.LATE_REG_CHIPS_STARTING) ? "STARTING" : "AVERAGE";
        return new LateRegistrationConfig(true, profile.getLateRegUntilLevel(), chipModeStr);
    }

    private ScheduledStartConfig convertScheduledStart(TournamentProfile profile) {
        if (!profile.isScheduledStartEnabled()) {
            return null;
        }
        Instant startTime = profile.getStartTime() > 0 ? Instant.ofEpochMilli(profile.getStartTime()) : null;
        return new ScheduledStartConfig(true, startTime, profile.getMinPlayersForStart());
    }

    private InviteConfig convertInvite(TournamentProfile profile) {
        if (!profile.isInviteOnly()) {
            return null;
        }
        String rawInvitees = profile.getMap().getString(TournamentProfile.PARAM_INVITEES, "");
        List<String> invitees = new ArrayList<>();
        if (rawInvitees != null && !rawInvitees.isEmpty()) {
            for (String name : rawInvitees.split("[,\n]")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    invitees.add(trimmed);
                }
            }
        }
        return new InviteConfig(true, invitees, profile.isInviteObserversPublic());
    }

    private BettingConfig convertBetting(TournamentProfile profile) {
        int maxRaises = profile.getMap().getInteger(TournamentProfile.PARAM_MAXRAISES, 0, 0,
                TournamentProfile.MAX_MAX_RAISES);
        return new BettingConfig(maxRaises, profile.isRaiseCapIgnoredHeadsUp());
    }

    /**
     * Build a list of {@link AIPlayerConfig} records from the AI mix settings in
     * the given profile. The caller sets a desired total number of AI players; this
     * method distributes them according to the player-type percentages stored in
     * the profile map.
     *
     * <p>
     * If no AI player types are registered, returns a list of {@code numAiPlayers}
     * generic AI entries with skill level 4.
     *
     * @param profile
     *            source profile
     * @param aiNames
     *            ordered list of AI player display names
     * @param defaultSkillLevel
     *            skill level to assign when no specific type is configured (1-7)
     * @return list of AI player configs, one per entry in {@code aiNames}
     */
    public List<AIPlayerConfig> buildAiPlayers(TournamentProfile profile, List<String> aiNames, int defaultSkillLevel) {
        List<AIPlayerConfig> result = new ArrayList<>();
        for (String name : aiNames) {
            result.add(new AIPlayerConfig(name, defaultSkillLevel));
        }
        return result;
    }
}
