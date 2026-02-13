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
/*
 * TournamentProfile.java
 *
 * Created on January 27, 2004, 9:26 AM
 */

package com.donohoedigital.games.poker.model;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.xml.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static com.donohoedigital.config.DebugConfig.*;

/**
 * @author donohoe
 */
@DataCoder('X')
public class TournamentProfile extends BaseProfile implements DataMarshal, SimpleXMLEncodable {
    static Logger logger = LogManager.getLogger(TournamentProfile.class);

    // defines
    public static final String PROFILE_BEGIN = "tourney";
    public static final String TOURNAMENT_DIR = "tournaments";

    // MAX value
    public static final int MAX_LEVELS = 40;
    public static final int MAX_SPOTS = 560;
    public static final int MIN_SPOTS = 10;
    public static final double MAX_SPOTS_PERCENT = .3333333d;

    // note on max players - if this changes above 6000, need to change
    // ids for territories in gameboard.xml and adjust PokerInit starting IDs
    public static final int MAX_PLAYERS = 5625;
    public static final int MAX_ONLINE_PLAYERS = 90; // 9 tables of 10
    public static final int MAX_OBSERVERS = 30;

    public static final int MAX_CHIPS = TESTING(PokerConstants.TESTING_LEVELS) ? 10000000 : 1000000;
    public static final int MAX_REBUY_CHIPS = TESTING(PokerConstants.TESTING_LEVELS) ? 10000000 : 1000000;
    public static final int MAX_BUY = TESTING(PokerConstants.TESTING_LEVELS) ? 10000000 : 1000000;
    public static final int MAX_BLINDANTE = 100000000;
    public static final int MAX_BOUNTY = 10000;
    public static final int MAX_MINUTES = 120;
    public static final int MAX_HOUSE_PERC = 25;
    public static final int MAX_HOUSE_AMOUNT = 9999;
    public static final int MAX_REBUYS = 99;
    public static final int MAX_PERC = 100;
    public static final int MAX_MAX_RAISES = 9;
    public static final int MAX_AI_RAISES = 4;
    public static final int BREAK_ANTE_VALUE = -1;
    public static final int MIN_TIMEOUT = 5;
    public static final int MAX_TIMEOUT = 120; // stored in poker player, absolute max is 214
    public static final int MAX_THINKBANK = 120; // stored in poker player, absolute max is 999
    public static final int MAX_BOOT_HANDS = 100;
    public static final int MIN_BOOT_HANDS = 5;
    public static final int ROUND_MULT = 1000; // 3 decimal places

    // formatting TODO: need to change %/$ formatting if localizing
    private static final MessageFormat FORMAT_PERC = new MessageFormat("{0}%");
    private static final MessageFormat FORMAT_AMOUNT = new MessageFormat("${0}");

    // saved members
    private DMTypedHashMap map_;

    // param names
    public static final String PARAM_DESC = "desc";
    public static final String PARAM_GREETING = "greeting";
    public static final String PARAM_NUM_PLAYERS = "defplayers";
    public static final String PARAM_TABLE_SEATS = "tableseats";
    public static final String PARAM_MINPERLEVEL_DEFAULT = "minperlevel";
    public static final String PARAM_MINUTES = "minutes";
    public static final String PARAM_BUYIN = "buyin";
    public static final String PARAM_BUYINCHIPS = "buyinchips";
    public static final String PARAM_SMALL = "small";
    public static final String PARAM_BIG = "big";
    public static final String PARAM_ANTE = "ante";
    public static final String PARAM_PAYOUT = "payout";
    public static final String PARAM_PAYOUTPERC = "payoutperc";
    public static final String PARAM_PAYOUTNUM = "payoutnum";
    public static final String PARAM_ALLOC = "alloc";
    public static final String PARAM_PRIZEPOOL = "prizepool";
    public static final String PARAM_HOUSE = "house";
    public static final String PARAM_HOUSEPERC = "houseperc";
    public static final String PARAM_HOUSEAMOUNT = "houseamount";
    public static final String PARAM_SPOTAMOUNT = "spotamount";
    public static final String PARAM_DOUBLE = "double";
    public static final String PARAM_REBUYS = "rebuys";
    public static final String PARAM_REBUYCOST = "rebuycost";
    public static final String PARAM_REBUYCHIPS = "rebuychips";
    public static final String PARAM_REBUY_UNTIL = "rebuyuntil";
    public static final String PARAM_MAXREBUYS = "maxrebuys";
    public static final String PARAM_ADDONS = "addons";
    public static final String PARAM_ADDONCOST = "addoncost";
    public static final String PARAM_ADDONCHIPS = "addonchips";
    public static final String PARAM_ADDONLEVEL = "addonlevel";
    public static final String PARAM_BOUNTY = "bounty";
    public static final String PARAM_BOUNTY_AMOUNT = "bountyamount";
    public static final String PARAM_LASTLEVEL = "lastlevel";
    public static final String PARAM_MIX = "mix:";
    public static final String PARAM_REBUYEXPR = "rebuyexpr";
    public static final String PARAM_REBUYCHIPCNT = "rebuychipcnt";
    public static final String PARAM_MAXRAISES = "maxraises";
    public static final String PARAM_MAXRAISES_NONE_HEADSUP = "maxheadsup";
    public static final String PARAM_TIMEOUT = "timeout";
    public static final String PARAM_TIMEOUT_PREFLOP = "timeoutpreflop";
    public static final String PARAM_TIMEOUT_FLOP = "timeoutflop";
    public static final String PARAM_TIMEOUT_TURN = "timeoutturn";
    public static final String PARAM_TIMEOUT_RIVER = "timeoutriver";
    public static final String PARAM_FILL_COMPUTER = "fillai";
    public static final String PARAM_ALLOW_DASH = "allowdash";
    public static final String PARAM_ALLOW_ADVISOR = "allowadvisor";
    public static final String PARAM_ONLINE_ACTIVATED_ONLY = "onlineactonly";
    public static final String PARAM_THINKBANK = "thinkbank";
    public static final String PARAM_MAX_OBSERVERS = "maxobservers";
    public static final String PARAM_BOOT_SITOUT = "bootsitout";
    public static final String PARAM_BOOT_SITOUT_COUNT = "bootsitoutcount";
    public static final String PARAM_BOOT_DISCONNECT = "bootdisconnect";
    public static final String PARAM_BOOT_DISCONNECT_COUNT = "bootdisconnectcount";
    public static final String PARAM_PLAYERS = "players";
    public static final String PARAM_INVITE_ONLY = "inviteonly";
    public static final String PARAM_INVITEES = "invitees";
    public static final String PARAM_INVITE_OBS = "publicobs";
    public static final String PARAM_GAMETYPE = "gametype";
    public static final String PARAM_GAMETYPE_DEFAULT = "gametypedefault";
    public static final String DATA_ELEMENT_GAMETYPE = "gameType";
    public static final String PARAM_UPDATE = "update";
    public static final String PARAM_LATE_REG = "latereg";
    public static final String PARAM_LATE_REG_UNTIL = "latereguntil";
    public static final String PARAM_LATE_REG_CHIPS = "lateregchips";
    public static final String PARAM_SCHEDULED_START = "scheduledstart";
    public static final String PARAM_START_TIME = "starttime";
    public static final String PARAM_MIN_PLAYERS_START = "minplayersstart";
    public static final String PARAM_LEVEL_ADVANCE_MODE = "leveladvancemode";
    public static final String PARAM_HANDS_PER_LEVEL = "handsperlevel";

    // Hands per level constraints
    public static final int MIN_HANDS_PER_LEVEL = 1;
    public static final int MAX_HANDS_PER_LEVEL = 100;
    public static final int DEFAULT_HANDS_PER_LEVEL = 10;

    /**
     * Empty constructor for loading from data
     */
    public TournamentProfile() {
        this("");
    }

    /**
     * Load profile from string file
     */
    public TournamentProfile(String sFile, boolean bFull) {
        super(sFile, bFull);
    }

    /**
     * Load profile from file
     */
    public TournamentProfile(File file, boolean bFull) {
        super(file, bFull);
    }

    /**
     * New profile with given name
     */
    public TournamentProfile(String sName) {
        super(sName);
        map_ = new DMTypedHashMap();

        // init starting values
        setNumPlayers(10);
        setMinutesPerLevel(30);
        setBuyin(100);
        setBuyinChips(1000);
        setRebuyChipCount(1000);
        setRebuys(false);
        setAddons(false);
        setPayout(PokerConstants.PAYOUT_SPOTS);
        setPayoutSpots(3);
        setOnlineActivatedPlayersOnly(true); // default to true for new tournaments
        fixAll();
    }

    /**
     * New profile copied from given profile, using new name
     */
    public TournamentProfile(TournamentProfile tp, String sName) {
        super(sName);
        map_ = new DMTypedHashMap();
        map_.putAll(tp.map_);
    }

    /**
     * Get begin part of profile name
     */
    @Override
    protected String getBegin() {
        return PROFILE_BEGIN;
    }

    /**
     * Get name of directory to store profiles in
     */
    @Override
    protected String getProfileDirName() {
        return TOURNAMENT_DIR;
    }

    /**
     * Get profile list
     */
    @Override
    protected List<BaseProfile> getProfileFileList() {
        return getProfileList();
    }

    /**
     * Get map
     */
    public DMTypedHashMap getMap() {
        return map_;
    }

    /**
     * Set update date as now
     */
    public void setUpdateDate() {
        map_.setLong(PARAM_UPDATE, System.currentTimeMillis());
    }

    /**
     * Get create date
     */
    public long getUpdateDate() {
        return map_.getLong(PARAM_UPDATE, getCreateDate());
    }

    /**
     * Get description
     */
    public String getDescription() {
        return map_.getString(PARAM_DESC, "");
    }

    /**
     * Get greeting, replacing variable $name with given sName.
     */
    public String getGreeting(String sName) {
        String sGreeting = map_.getString(PARAM_GREETING, "").trim();
        if (sGreeting.length() == 0)
            return null;
        sGreeting = Utils.replace(sGreeting, "\\$name", sName);
        return sGreeting;
    }

    /**
     * Get num players
     */
    public int getNumPlayers() {
        return map_.getInteger(PARAM_NUM_PLAYERS, 0, 2, MAX_PLAYERS);
    }

    /**
     * Set num players
     */
    public void setNumPlayers(int n) {
        map_.setInteger(PARAM_NUM_PLAYERS, n);
    }

    /**
     * Get num online players - minimum of getNumPlayers() and MAX_ONLINE_PLAYERS
     */
    public int getMaxOnlinePlayers() {
        return constraints().getMaxOnlinePlayers(getNumPlayers());
    }

    /**
     * Set player list
     */
    public void setPlayers(List<String> players) {
        DMArrayList<String> list = (DMArrayList<String>) map_.getList(PARAM_PLAYERS);
        if (list == null) {
            list = new DMArrayList<String>();
            map_.setList(PARAM_PLAYERS, list);
        } else {
            list.clear();
        }

        for (String name : players) {
            list.add(name);
        }

        // change update date so it updates in LAN clients
        setUpdateDate();
    }

    /**
     * Get player list
     */
    public List<String> getPlayers() {
        DMArrayList<String> players = (DMArrayList<String>) map_.getList(PARAM_PLAYERS);
        if (players == null)
            players = new DMArrayList<String>();
        return players;
    }

    /**
     * is invite only?
     */
    public boolean isInviteOnly() {
        return map_.getBoolean(PARAM_INVITE_ONLY, false);
    }

    /**
     * Set invite only
     */
    public void setInviteOnly(boolean b) {
        map_.setBoolean(PARAM_INVITE_ONLY, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * is invite only?
     */
    public boolean isInviteObserversPublic() {
        return map_.getBoolean(PARAM_INVITE_OBS, false);
    }

    /**
     * Set public observers
     */
    public void setInviteObserversPublic(boolean b) {
        map_.setBoolean(PARAM_INVITE_OBS, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Get invitee player list
     */
    public AbstractPlayerList getInvitees() {
        return new InviteePlayerList(this);
    }

    /**
     * PlayerList which stores data in TournamentProfile
     */
    private static class InviteePlayerList extends AbstractPlayerList {
        TournamentProfile profile;

        private InviteePlayerList(TournamentProfile profile) {
            this.profile = profile;
            fetch();
        }

        @Override
        public String getName() {
            return "invited";
        }

        @Override
        protected void saveNames(String sNames) {
            profile.map_.setString(PARAM_INVITEES, sNames);
        }

        @Override
        protected String fetchNames() {
            return profile.map_.getString(PARAM_INVITEES, null);
        }

        @Override
        protected void saveKeys(String sKeys) {
            /* no keys */ }

        @Override
        protected String fetchKeys() {
            return null;
        }
    }

    /**
     * Get max players at a table
     */
    public int getSeats() {
        return map_.getInteger(PARAM_TABLE_SEATS, PokerConstants.SEATS, 2, PokerConstants.SEATS);
    }

    /**
     * Get buyin
     */
    public int getBuyinCost() {
        return map_.getInteger(PARAM_BUYIN, 0, 1, MAX_BUY);
    }

    /**
     * set buyin
     */
    public void setBuyin(int n) {
        map_.setInteger(PARAM_BUYIN, n);
    }

    /**
     * Get buyin chips
     */
    public int getBuyinChips() {
        return map_.getInteger(PARAM_BUYINCHIPS, 0, 1, MAX_CHIPS);
    }

    /**
     * set buyin chips
     */
    public void setBuyinChips(int n) {
        map_.setInteger(PARAM_BUYINCHIPS, n);
    }

    /**
     * Get starting depth in big blinds (buyin chips / level 1 big blind). Returns
     * -1 if big blind at level 1 is zero or no levels are defined.
     */
    public int getStartingDepthBBs() {
        int bigBlind = getBigBlind(1);
        if (bigBlind <= 0)
            return -1;
        return getBuyinChips() / bigBlind;
    }

    /**
     * Get rebuy chip cnt
     */
    public int getRebuyChipCount() {
        return map_.getInteger(PARAM_REBUYCHIPCNT, getBuyinChips(), 0, MAX_REBUY_CHIPS);
    }

    /**
     * set rebuy chip count
     */
    public void setRebuyChipCount(int n) {
        map_.setInteger(PARAM_REBUYCHIPCNT, n);
    }

    /**
     * Get rebuy expression
     */
    public int getRebuyExpressionType() {
        return map_.getInteger(PARAM_REBUYEXPR, PokerConstants.REBUY_LTE, PokerConstants.REBUY_LT,
                PokerConstants.REBUY_LTE);
    }

    /**
     * Get rebuy expression
     */
    public void setRebuyExpression(int n) {
        map_.setInteger(PARAM_REBUYEXPR, n);
    }

    /**
     * remove entries for level
     */
    public void clearLevel(int nLevel) {
        map_.remove(PARAM_ANTE + nLevel);
        map_.remove(PARAM_SMALL + nLevel);
        map_.remove(PARAM_BIG + nLevel);
        map_.remove(PARAM_MINUTES + nLevel);
        map_.remove(PARAM_GAMETYPE + nLevel);
    }

    /**
     * Clear all blind levels from the profile.
     */
    public void clearAllLevels() {
        for (int i = 1; i <= MAX_LEVELS; i++) {
            clearLevel(i);
        }
    }

    /**
     * Set blind level with ante, small blind, big blind, and minutes.
     *
     * @param nLevel
     *            Level number (1-based)
     * @param ante
     *            Ante amount
     * @param small
     *            Small blind amount
     * @param big
     *            Big blind amount
     * @param minutes
     *            Minutes for this level
     */
    public void setLevel(int nLevel, int ante, int small, int big, int minutes) {
        map_.setString(PARAM_ANTE + nLevel, String.valueOf(ante));
        map_.setString(PARAM_SMALL + nLevel, String.valueOf(small));
        map_.setString(PARAM_BIG + nLevel, String.valueOf(big));
        map_.setString(PARAM_MINUTES + nLevel, String.valueOf(minutes));
    }

    /**
     * Get BlindStructure wrapper for blind/ante access.
     */
    private BlindStructure blinds() {
        return new BlindStructure(map_);
    }

    /**
     * Get PayoutCalculator wrapper for payout calculations.
     */
    private PayoutCalculator payouts() {
        return new PayoutCalculator(map_);
    }

    /**
     * Get ProfileValidator wrapper for validation and normalization.
     */
    private ProfileValidator validator() {
        return new ProfileValidator(map_, new ProfileValidator.ValidationCallbacks() {
            @Override
            public int getMaxPayoutSpots(int numPlayers) {
                return constraints().getMaxPayoutSpots(numPlayers);
            }

            @Override
            public int getMaxPayoutPercent(int numPlayers) {
                return constraints().getMaxPayoutPercent(numPlayers);
            }

            @Override
            public boolean isAllocAuto() {
                return TournamentProfile.this.isAllocAuto();
            }

            @Override
            public boolean isAllocFixed() {
                return TournamentProfile.this.isAllocFixed();
            }

            @Override
            public boolean isAllocPercent() {
                return TournamentProfile.this.isAllocPercent();
            }

            @Override
            public boolean isAllocSatellite() {
                return TournamentProfile.this.isAllocSatellite();
            }

            @Override
            public void setAutoSpots() {
                TournamentProfile.this.setAutoSpots();
            }

            @Override
            public void fixLevels() {
                TournamentProfile.this.fixLevels();
            }

            @Override
            public int getNumSpots() {
                return TournamentProfile.this.getNumSpots();
            }

            @Override
            public double getSpot(int position) {
                return TournamentProfile.this.getSpot(position);
            }
        });
    }

    /**
     * Validate tournament profile settings and return any warnings.
     *
     * <p>
     * Checks for common configuration issues that don't prevent profile creation
     * but may lead to unexpected behavior.
     *
     * @return ValidationResult containing any warnings found
     * @see ProfileValidator#validateProfile()
     */
    public ValidationResult validateProfile() {
        return validator().validateProfile();
    }

    /**
     * Get ParameterConstraints wrapper for constraint calculations.
     */
    private ParameterConstraints constraints() {
        return new ParameterConstraints(map_);
    }

    /**
     * Get small blind
     */
    public int getSmallBlind(int nLevel) {
        return blinds().getSmallBlind(nLevel);
    }

    /**
     * Get big blind
     */
    public int getBigBlind(int nLevel) {
        return blinds().getBigBlind(nLevel);
    }

    /**
     * Get ante
     */
    public int getAnte(int nLevel) {
        return blinds().getAnte(nLevel);
    }

    /**
     * Get last small blind - if current level is a break, returns first prior
     * non-break level
     */
    public int getLastSmallBlind(int nLevel) {
        return blinds().getLastSmallBlind(nLevel);
    }

    /**
     * Get last big blind - if current level is a break, returns first prior
     * non-break level
     */
    public int getLastBigBlind(int nLevel) {
        return blinds().getLastBigBlind(nLevel);
    }

    /**
     * Get last ante - if current level is a break, returns first prior non-break
     * level
     */
    public int getLastAnte(int nLevel) {
        while (isBreak(nLevel) && nLevel > 0) {
            nLevel--;
        }
        return getAnte(nLevel);
    }

    /**
     * Get default time limit
     */
    public int getDefaultMinutesPerLevel() {
        return map_.getInteger(PARAM_MINPERLEVEL_DEFAULT, 0, 1, MAX_MINUTES);
    }

    /**
     * Set default level minutes
     */
    public void setMinutesPerLevel(int n) {
        map_.setInteger(PARAM_MINPERLEVEL_DEFAULT, n);
    }

    /**
     * Get minutes in level
     */
    public int getMinutes(int nLevel) {
        int nAmount = getAmountFromString(PARAM_MINUTES + nLevel, false);
        if (nAmount == 0) {
            nAmount = getDefaultMinutesPerLevel();
        }
        if (nAmount > MAX_MINUTES)
            nAmount = MAX_MINUTES;
        return nAmount;
    }

    /**
     * Get default game type
     */
    public String getDefaultGameTypeString() {
        return map_.getString(PARAM_GAMETYPE_DEFAULT, PokerConstants.DE_NO_LIMIT_HOLDEM);
    }

    /**
     * Get string version of game type
     */
    public String getGameTypeString(int nLevel) {
        String sType = map_.getString(PARAM_GAMETYPE + nLevel);
        if (sType == null || sType.length() == 0) {
            sType = getDefaultGameTypeString();
        }
        return sType;
    }

    /**
     * Get game type display for level, returns blank if type equals default.
     */
    public String getGameTypeDisplay(int i) {
        String sGame = getGameTypeString(i);
        if (sGame.equals(getDefaultGameTypeString())) {
            return "";
        } else {
            return DataElement.getDisplayValue(DATA_ELEMENT_GAMETYPE, sGame);
        }
    }

    /**
     * Get game type for level, returning an int (see PokerContants)
     */
    public int getGameType(int nLevel) {
        String sType = getGameTypeString(nLevel);

        if (sType.equals(PokerConstants.DE_NO_LIMIT_HOLDEM)) {
            return PokerConstants.TYPE_NO_LIMIT_HOLDEM;
        } else if (sType.equals(PokerConstants.DE_POT_LIMIT_HOLDEM)) {
            return PokerConstants.TYPE_POT_LIMIT_HOLDEM;
        }
        if (sType.equals(PokerConstants.DE_LIMIT_HOLDEM)) {
            return PokerConstants.TYPE_LIMIT_HOLDEM;
        }

        throw new ApplicationError(ErrorCodes.ERROR_INVALID, "Unknown poker game type", sType, null);
    }

    /**
     * is given level a break?
     */
    public boolean isBreak(int nLevel) {
        return blinds().isBreak(nLevel);
    }

    /**
     * Set given level as a break
     */
    public void setBreak(int nLevel, int nMinutes) {
        map_.setString(PARAM_ANTE + nLevel, Integer.toString(BREAK_ANTE_VALUE));
        map_.setString(PARAM_MINUTES + nLevel, Integer.toString(nMinutes));
        map_.remove(PARAM_SMALL + nLevel);
        map_.remove(PARAM_BIG + nLevel);
        map_.remove(PARAM_GAMETYPE + nLevel);
    }

    /**
     * Get max raises
     */
    public int getMaxRaises(int nNumWithCards, boolean isComputer) {
        return constraints().getMaxRaises(nNumWithCards, isComputer, isRaiseCapIgnoredHeadsUp());
    }

    /**
     * Observe max raises when heads-up?
     */
    public boolean isRaiseCapIgnoredHeadsUp() {
        return map_.getBoolean(PARAM_MAXRAISES_NONE_HEADSUP, true);
    }

    /**
     * set payout type
     */
    public void setPayout(int nType) {
        map_.setInteger(PARAM_PAYOUT, nType);
    }

    /**
     * Get payout type
     */
    public int getPayoutType() {
        return map_.getInteger(PARAM_PAYOUT, PokerConstants.PAYOUT_PERC, PokerConstants.PAYOUT_SPOTS,
                PokerConstants.PAYOUT_SATELLITE);
    }

    /**
     * Set payout spots
     */
    public void setPayoutSpots(int n) {
        map_.setInteger(PARAM_PAYOUTNUM, n);
    }

    /**
     * Set payout spots
     */
    public void setPayoutPercent(int n) {
        map_.setInteger(PARAM_PAYOUTPERC, n);
    }

    /**
     * Get spots to payout
     */
    private int getPayoutSpots() {
        return map_.getInteger(PARAM_PAYOUTNUM, 3, 1, MAX_SPOTS);
    }

    /**
     * get percent of spots to payout
     */
    public int getPayoutPercent() {
        return map_.getInteger(PARAM_PAYOUTPERC, 5, 1, MAX_PERC);
    }

    /**
     * Update num players, adjust payout if necessary
     */
    public void updateNumPlayers(int nNumPlayers) {
        validator().updateNumPlayers(nNumPlayers);
    }

    /**
     * Return number of payout spots
     */
    public int getNumSpots() {
        return payouts().getNumSpots();
    }

    /**
     * In isAllocSatellite() case, this returns the amount each spot gets
     */
    public int getSatellitePayout() {
        return (int) getSpot(1);
    }

    /**
     * Set alloc type
     */
    public void setAlloc(int nType) {
        map_.setInteger(PARAM_ALLOC, nType);
    }

    /**
     * Return if pool is auto allocated
     */
    public boolean isAllocAuto() {
        return !isAllocSatellite() && map_.getInteger(PARAM_ALLOC, PokerConstants.ALLOC_AUTO, PokerConstants.ALLOC_AUTO,
                PokerConstants.ALLOC_AMOUNT) == PokerConstants.ALLOC_AUTO;
    }

    /**
     * Return if pool is perc allocated
     */
    public boolean isAllocPercent() {
        return !isAllocSatellite() && map_.getInteger(PARAM_ALLOC, PokerConstants.ALLOC_AUTO, PokerConstants.ALLOC_AUTO,
                PokerConstants.ALLOC_AMOUNT) == PokerConstants.ALLOC_PERC;
    }

    /**
     * Return if pool is fixed amount allocated
     */
    public boolean isAllocFixed() {
        return !isAllocSatellite() && map_.getInteger(PARAM_ALLOC, PokerConstants.ALLOC_AUTO, PokerConstants.ALLOC_AUTO,
                PokerConstants.ALLOC_AMOUNT) == PokerConstants.ALLOC_AMOUNT;
    }

    /**
     * Return if pool is satellite allocated
     */
    public boolean isAllocSatellite() {
        return getPayoutType() == PokerConstants.PAYOUT_SATELLITE;
    }

    /**
     * Get prize pool amount
     */
    public int getPrizePool() {
        // pool - get amount set during a tournament,
        int nPool = map_.getInteger(PARAM_PRIZEPOOL, -1);
        if (nPool != -1) {
            return nPool;
        }

        // or if not set yet, estimate from number of players
        int nTotalPool = getNumPlayers() * getBuyinCost();
        return getPoolAfterHouseTake(nTotalPool);
    }

    /**
     * Set actual prize pool (updates spots if auto-allocated).
     */
    public void setPrizePool(int nPool, boolean bAdjustForHouseTake) {
        if (bAdjustForHouseTake)
            nPool = getPoolAfterHouseTake(nPool);
        map_.setInteger(PARAM_PRIZEPOOL, nPool);
        if (isAllocAuto())
            setAutoSpots();
    }

    /**
     * Get house take
     */
    public int getPoolAfterHouseTake(int nPool) {
        return payouts().getPoolAfterHouseTake(nPool);
    }

    /**
     * return type of house cut
     */
    public int getHouseCutType() {
        return map_.getInteger(PARAM_HOUSE, PokerConstants.HOUSE_PERC, PokerConstants.HOUSE_AMOUNT,
                PokerConstants.HOUSE_PERC);
    }

    /**
     * get house percent integer (0-100)
     */
    public int getHousePercent() {
        return map_.getInteger(PARAM_HOUSEPERC, 0, 0, MAX_HOUSE_PERC);
    }

    /**
     * Get house cut amount
     */
    public int getHouseAmount() {
        return map_.getInteger(PARAM_HOUSEAMOUNT, 0, 0, MAX_HOUSE_AMOUNT);
    }

    /**
     * Get true buyin (less house cost) - this is used to figure out the multiple to
     * use for minimum payouts
     */
    public int getTrueBuyin() {
        int nType = getHouseCutType();
        int buy = getBuyinCost();
        if (nType == PokerConstants.HOUSE_AMOUNT) {
            buy -= getHouseAmount();
        }
        return buy;
    }

    /**
     * Get max number of spots for given number of players
     */
    public int getMaxPayoutSpots(int nNumPlayers) {
        return constraints().getMaxPayoutSpots(nNumPlayers);
    }

    /**
     * Get max percetage of spots
     */
    public int getMaxPayoutPercent(int nNumPlayers) {
        return constraints().getMaxPayoutPercent(nNumPlayers);
    }

    /**
     * Set automatic spot percentages using Fibonacci-based payout distribution.
     *
     * <p>
     * Delegates to {@link PayoutDistributionCalculator} for the complex algorithm,
     * then writes the results back to the internal map.
     *
     * @see PayoutDistributionCalculator#calculatePayouts(int, int, int, int)
     */
    public void setAutoSpots() {
        PayoutDistributionCalculator calc = new PayoutDistributionCalculator();

        // Calculate payout amounts using extracted algorithm
        int[] amounts = calc.calculatePayouts(getNumSpots(), getPrizePool(), getTrueBuyin(), getRebuyCost(),
                getNumPlayers(), getBuyinCost(), getPoolAfterHouseTake(getBuyinCost() * getNumPlayers()));

        // Write amounts back to map (indexed from last place to first)
        for (int i = 0; i < MAX_SPOTS; i++) {
            if (i >= amounts.length) {
                map_.removeString(PARAM_SPOTAMOUNT + (i + 1));
            } else {
                String text = FORMAT_AMOUNT.format(new Object[]{amounts[amounts.length - i - 1]});
                map_.setString(PARAM_SPOTAMOUNT + (i + 1), text);
            }
        }
    }

    /**
     * Get value of payout spot
     */
    public double getSpot(int nNum) {
        return getSpotFromString(PARAM_SPOTAMOUNT + nNum);
    }

    /**
     * Set value of payout spot (as percentage)
     */
    public void setSpot(int nNum, double percentage) {
        map_.setString(PARAM_SPOTAMOUNT + nNum, String.valueOf(percentage));
    }

    /**
     * Get value of payout spot as string
     */
    public String getSpotAsString(int nNum) {
        return map_.getString(PARAM_SPOTAMOUNT + nNum, "");
    }

    /**
     * Get payout based on spot
     */
    public int getPayout(int nNum) {
        return payouts().getPayout(nNum, getNumSpots(), getPrizePool());
    }

    /**
     * Get integer from string, throws exception if not there, used for items stored
     * as strings
     */
    private int getAmountFromString(String sName, boolean allowNegative) {
        String s = map_.getString(sName);
        if (s == null || s.length() == 0)
            return 0;

        int n = Integer.parseInt(s);
        if (!allowNegative && n < 0)
            n = 0;
        if (n > MAX_BLINDANTE)
            n = MAX_BLINDANTE;

        return n;
    }

    /**
     * Get double from string, throws exception if not there, used for items stored
     * as strings
     */
    private double getSpotFromString(String sName) {
        String s = map_.getString(sName);
        double ret = Utils.parseStringToDouble(s, ROUND_MULT);

        if (ret < 0)
            ret = 0;
        if (ret > MAX_BLINDANTE)
            ret = MAX_BLINDANTE;
        return ret;
    }

    /**
     * Get whether an online game is filled with ai players
     */
    public boolean isFillComputer() {
        return map_.getBoolean(PARAM_FILL_COMPUTER, true);
    }

    /**
     * Get whether an online game only allows online activated players
     */
    public boolean isOnlineActivatedPlayersOnly() {
        // Added 3.0p3 - defaults to false since new option
        return map_.getBoolean(PARAM_ONLINE_ACTIVATED_ONLY, false);
    }

    /**
     * set online activated
     */
    public void setOnlineActivatedPlayersOnly(boolean onlineActivatedPlayersOnly) {
        map_.setBoolean(PARAM_ONLINE_ACTIVATED_ONLY, onlineActivatedPlayersOnly);
    }

    /**
     * Get whether an online game allows dashboard usage
     */
    public boolean isAllowDash() {
        return map_.getBoolean(PARAM_ALLOW_DASH, false);
    }

    /**
     * Get whether an online game allows advisor usage
     */
    public boolean isAllowAdvisor() {
        return map_.getBoolean(PARAM_ALLOW_ADVISOR, false);
    }

    /**
     * Get whether an online game boots sitout players
     */
    public boolean isBootSitout() {
        return map_.getBoolean(PARAM_BOOT_SITOUT, false);
    }

    /**
     * Get whether an online game boots disconnected players
     */
    public boolean isBootDisconnect() {
        return map_.getBoolean(PARAM_BOOT_DISCONNECT, true);
    }

    /**
     * get boot sitout count
     */
    public int getBootSitoutCount() {
        return map_.getInteger(PARAM_BOOT_SITOUT_COUNT, 25, MIN_BOOT_HANDS, MAX_BOOT_HANDS);
    }

    /**
     * get boot disconnect count
     */
    public int getBootDisconnectCount() {
        return map_.getInteger(PARAM_BOOT_DISCONNECT_COUNT, 10, MIN_BOOT_HANDS, MAX_BOOT_HANDS);
    }

    /**
     * Get whether late registration is enabled
     */
    public boolean isLateRegEnabled() {
        return map_.getBoolean(PARAM_LATE_REG, false);
    }

    /**
     * Set whether late registration is enabled
     */
    public void setLateRegEnabled(boolean enabled) {
        map_.setBoolean(PARAM_LATE_REG, enabled);
    }

    /**
     * Get late registration cutoff level
     */
    public int getLateRegUntilLevel() {
        return map_.getInteger(PARAM_LATE_REG_UNTIL, 1, 1, MAX_LEVELS);
    }

    /**
     * Set late registration cutoff level
     */
    public void setLateRegUntilLevel(int level) {
        map_.setInteger(PARAM_LATE_REG_UNTIL, level);
    }

    /**
     * Get late registration chip mode (starting or average)
     */
    public int getLateRegChips() {
        return map_.getInteger(PARAM_LATE_REG_CHIPS, PokerConstants.LATE_REG_CHIPS_STARTING);
    }

    /**
     * Set late registration chip mode
     */
    public void setLateRegChips(int mode) {
        map_.setInteger(PARAM_LATE_REG_CHIPS, mode);
    }

    /**
     * Get whether scheduled start is enabled
     */
    public boolean isScheduledStartEnabled() {
        return map_.getBoolean(PARAM_SCHEDULED_START, false);
    }

    /**
     * Set whether scheduled start is enabled
     */
    public void setScheduledStartEnabled(boolean enabled) {
        map_.setBoolean(PARAM_SCHEDULED_START, enabled);
    }

    /**
     * Get the scheduled start time (milliseconds since epoch)
     */
    public long getStartTime() {
        return map_.getLong(PARAM_START_TIME, 0L);
    }

    /**
     * Set the scheduled start time (milliseconds since epoch)
     */
    public void setStartTime(long timeMillis) {
        map_.setLong(PARAM_START_TIME, timeMillis);
    }

    /**
     * Get minimum number of players required for scheduled start
     */
    public int getMinPlayersForStart() {
        return map_.getInteger(PARAM_MIN_PLAYERS_START, PokerConstants.MIN_SCHEDULED_START_PLAYERS, 2,
                MAX_ONLINE_PLAYERS);
    }

    /**
     * Set minimum number of players required for scheduled start
     */
    public void setMinPlayersForStart(int minPlayers) {
        map_.setInteger(PARAM_MIN_PLAYERS_START, minPlayers);
    }

    /**
     * Get whether the blinds double after last level
     */
    public boolean isDoubleAfterLastLevel() {
        return map_.getBoolean(PARAM_DOUBLE, true);
    }

    /**
     * Get whether there are rebuys
     */
    public boolean isRebuys() {
        return map_.getBoolean(PARAM_REBUYS, false);
    }

    /**
     * set whether there are rebuys
     */
    public void setRebuys(boolean b) {
        map_.setBoolean(PARAM_REBUYS, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Get rebuy cost
     */
    public int getRebuyCost() {
        return map_.getInteger(PARAM_REBUYCOST, 0, 1, MAX_BUY);
    }

    /**
     * Get rebuy chips
     */
    public int getRebuyChips() {
        return map_.getInteger(PARAM_REBUYCHIPS, 0, 1, MAX_CHIPS);
    }

    /**
     * Get last rebuy level
     */
    public int getLastRebuyLevel() {
        return map_.getInteger(PARAM_REBUY_UNTIL, 0, 1, MAX_LEVELS);
    }

    /**
     * Set last rebuy level
     */
    public void setLastRebuyLevel(int n) {
        map_.setInteger(PARAM_REBUY_UNTIL, n);
    }

    /**
     * Get max rebuys
     */
    public int getMaxRebuys() {
        return constraints().getMaxRebuys();
    }

    /**
     * Get whether there are addons
     */
    public boolean isAddons() {
        return map_.getBoolean(PARAM_ADDONS, false);
    }

    /**
     * set whether there are addons
     */
    public void setAddons(boolean b) {
        map_.setBoolean(PARAM_ADDONS, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Get addon cost
     */
    public int getAddonCost() {
        return map_.getInteger(PARAM_ADDONCOST, 0, 1, MAX_BUY);
    }

    /**
     * Get rebuy chips
     */
    public int getAddonChips() {
        return map_.getInteger(PARAM_ADDONCHIPS, 0, 1, MAX_CHIPS);
    }

    /**
     * Get add on level
     */
    public int getAddonLevel() {
        return map_.getInteger(PARAM_ADDONLEVEL, 0, 1, MAX_LEVELS);
    }

    /**
     * Get whether bounties are enabled
     */
    public boolean isBountyEnabled() {
        return map_.getBoolean(PARAM_BOUNTY, false);
    }

    /**
     * Set whether bounties are enabled
     */
    public void setBountyEnabled(boolean b) {
        map_.setBoolean(PARAM_BOUNTY, b ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Get bounty amount per knockout
     */
    public int getBountyAmount() {
        return map_.getInteger(PARAM_BOUNTY_AMOUNT, 0, 0, MAX_BOUNTY);
    }

    /**
     * Set bounty amount per knockout
     *
     * @param amount
     *            Bounty amount (will be clamped to [0, MAX_BOUNTY])
     */
    public void setBountyAmount(int amount) {
        // Clamp to valid range to match getter behavior
        int clamped = Math.max(0, Math.min(amount, MAX_BOUNTY));
        map_.setInteger(PARAM_BOUNTY_AMOUNT, clamped);
    }

    /**
     * Get level advancement mode (time-based or hands-based)
     *
     * @return level advance mode
     */
    public LevelAdvanceMode getLevelAdvanceMode() {
        String mode = map_.getString(PARAM_LEVEL_ADVANCE_MODE, LevelAdvanceMode.TIME.name());
        return LevelAdvanceMode.fromString(mode);
    }

    /**
     * Set level advancement mode
     *
     * @param mode
     *            level advance mode (null defaults to TIME)
     */
    public void setLevelAdvanceMode(LevelAdvanceMode mode) {
        if (mode == null) {
            mode = LevelAdvanceMode.TIME;
        }
        map_.setString(PARAM_LEVEL_ADVANCE_MODE, mode.name());
    }

    /**
     * Get number of hands to play before advancing to next level (when in HANDS
     * mode)
     *
     * @return hands per level
     */
    public int getHandsPerLevel() {
        return map_.getInteger(PARAM_HANDS_PER_LEVEL, DEFAULT_HANDS_PER_LEVEL, MIN_HANDS_PER_LEVEL,
                MAX_HANDS_PER_LEVEL);
    }

    /**
     * Set number of hands to play before advancing to next level
     *
     * @param hands
     *            hands per level (will be clamped to [MIN_HANDS_PER_LEVEL,
     *            MAX_HANDS_PER_LEVEL])
     */
    public void setHandsPerLevel(int hands) {
        int clamped = Math.max(MIN_HANDS_PER_LEVEL, Math.min(hands, MAX_HANDS_PER_LEVEL));
        map_.setInteger(PARAM_HANDS_PER_LEVEL, clamped);
    }

    /**
     * get online player timeout for acting
     */
    public int getTimeoutSeconds() {
        return map_.getInteger(PARAM_TIMEOUT, 30, MIN_TIMEOUT, MAX_TIMEOUT);
    }

    /**
     * set online player timeout for acting
     */
    public void setTimeoutSeconds(int seconds) {
        map_.setInteger(PARAM_TIMEOUT, seconds);
    }

    /**
     * Get timeout for a specific betting round, falling back to base timeout if not
     * set
     */
    public int getTimeoutForRound(int round) {
        String param = getTimeoutParamForRound(round);
        if (param == null) {
            return getTimeoutSeconds(); // Fallback for ROUND_SHOWDOWN or invalid
        }

        int roundTimeout = map_.getInteger(param, 0, 0, MAX_TIMEOUT);
        if (roundTimeout > 0 && roundTimeout < MIN_TIMEOUT) {
            roundTimeout = MIN_TIMEOUT; // Enforce minimum when non-zero
        }
        return (roundTimeout > 0) ? roundTimeout : getTimeoutSeconds();
    }

    /**
     * Get the parameter name for a specific round
     */
    private String getTimeoutParamForRound(int round) {
        // Using raw constants to avoid module dependency on poker
        // ROUND_PRE_FLOP = 0, ROUND_FLOP = 1, ROUND_TURN = 2, ROUND_RIVER = 3
        switch (round) {
            case 0 : // ROUND_PRE_FLOP
                return PARAM_TIMEOUT_PREFLOP;
            case 1 : // ROUND_FLOP
                return PARAM_TIMEOUT_FLOP;
            case 2 : // ROUND_TURN
                return PARAM_TIMEOUT_TURN;
            case 3 : // ROUND_RIVER
                return PARAM_TIMEOUT_RIVER;
            default :
                return null;
        }
    }

    /**
     * get pre-flop timeout
     */
    public int getTimeoutPreflop() {
        return map_.getInteger(PARAM_TIMEOUT_PREFLOP, 0, 0, MAX_TIMEOUT);
    }

    /**
     * set pre-flop timeout
     */
    public void setTimeoutPreflop(int seconds) {
        map_.setInteger(PARAM_TIMEOUT_PREFLOP, seconds);
    }

    /**
     * get flop timeout
     */
    public int getTimeoutFlop() {
        return map_.getInteger(PARAM_TIMEOUT_FLOP, 0, 0, MAX_TIMEOUT);
    }

    /**
     * set flop timeout
     */
    public void setTimeoutFlop(int seconds) {
        map_.setInteger(PARAM_TIMEOUT_FLOP, seconds);
    }

    /**
     * get turn timeout
     */
    public int getTimeoutTurn() {
        return map_.getInteger(PARAM_TIMEOUT_TURN, 0, 0, MAX_TIMEOUT);
    }

    /**
     * set turn timeout
     */
    public void setTimeoutTurn(int seconds) {
        map_.setInteger(PARAM_TIMEOUT_TURN, seconds);
    }

    /**
     * get river timeout
     */
    public int getTimeoutRiver() {
        return map_.getInteger(PARAM_TIMEOUT_RIVER, 0, 0, MAX_TIMEOUT);
    }

    /**
     * set river timeout
     */
    public void setTimeoutRiver(int seconds) {
        map_.setInteger(PARAM_TIMEOUT_RIVER, seconds);
    }

    /**
     * get player thinkbank for acting
     */
    public int getThinkBankSeconds() {
        return map_.getInteger(PARAM_THINKBANK, 15, 0, MAX_THINKBANK);
    }

    /**
     * get maximum number of observers
     */
    public int getMaxObservers() {
        return constraints().getMaxObservers();
    }

    /**
     * Validate and normalize tournament blind level structure.
     *
     * <p>
     * Delegates to {@link LevelValidator} for complex validation logic: gap
     * consolidation, blind fill-in, monotonic enforcement, ante bounds, rounding,
     * break handling, and default propagation.
     *
     * @see LevelValidator#validateAndNormalize(Map, int, String)
     */
    public void fixLevels() {
        LevelValidator validator = new LevelValidator();

        // Extract raw level data from map
        Map<String, String> rawLevelData = extractLevelStrings();

        // Validate and normalize using extracted algorithm
        List<LevelValidator.LevelData> normalizedLevels = validator.validateAndNormalize(rawLevelData,
                getDefaultMinutesPerLevel(), getDefaultGameTypeString());

        // Clear all existing level data
        for (int i = 1; i <= MAX_LEVELS; i++) {
            map_.removeString(PARAM_ANTE + i);
            map_.removeString(PARAM_SMALL + i);
            map_.removeString(PARAM_BIG + i);
            map_.removeString(PARAM_MINUTES + i);
            map_.removeString(PARAM_GAMETYPE + i);
        }

        // Write normalized levels back to map
        for (LevelValidator.LevelData level : normalizedLevels) {
            int levelNum = level.levelNum;

            if (level.isBreak) {
                // Break levels only have ante (=-1) and minutes
                map_.setString(PARAM_ANTE + levelNum, "" + BREAK_ANTE_VALUE);
                if (level.minutes > 0) {
                    map_.setString(PARAM_MINUTES + levelNum, "" + level.minutes);
                }
            } else {
                // Regular levels
                if (level.ante > 0) {
                    map_.setString(PARAM_ANTE + levelNum, "" + level.ante);
                }
                if (level.smallBlind > 0) {
                    map_.setString(PARAM_SMALL + levelNum, "" + level.smallBlind);
                }
                if (level.bigBlind > 0) {
                    map_.setString(PARAM_BIG + levelNum, "" + level.bigBlind);
                }
                if (level.minutes > 0) {
                    map_.setString(PARAM_MINUTES + levelNum, "" + level.minutes);
                }
                if (level.gameType != null) {
                    map_.setString(PARAM_GAMETYPE + levelNum, level.gameType);
                }
            }
        }

        // Record last level number
        map_.setInteger(PARAM_LASTLEVEL, normalizedLevels.size());
    }

    /**
     * Extract level strings from map for validation.
     */
    private Map<String, String> extractLevelStrings() {
        Map<String, String> rawData = new HashMap<>();

        for (int i = 1; i <= MAX_LEVELS; i++) {
            String ante = map_.getString(PARAM_ANTE + i);
            String small = map_.getString(PARAM_SMALL + i);
            String big = map_.getString(PARAM_BIG + i);
            String minutes = map_.getString(PARAM_MINUTES + i);
            String gameType = map_.getString(PARAM_GAMETYPE + i);

            if (ante != null && !ante.isEmpty()) {
                rawData.put("ante" + i, ante);
            }
            if (small != null && !small.isEmpty()) {
                rawData.put("small" + i, small);
            }
            if (big != null && !big.isEmpty()) {
                rawData.put("big" + i, big);
            }
            if (minutes != null && !minutes.isEmpty()) {
                rawData.put("minutes" + i, minutes);
            }
            if (gameType != null && !gameType.isEmpty()) {
                rawData.put("gametype" + i, gameType);
            }
        }

        return rawData;
    }

    /**
     * round ante/blind
     */
    private int round(int n) {
        int nRound;
        if (n <= 100)
            nRound = 1;
        else if (n <= 500)
            nRound = 5;
        else if (n <= 1000)
            nRound = 25;
        else if (n <= 10000)
            nRound = 100;
        else if (n <= 100000)
            nRound = 1000;
        else if (n <= 1000000)
            nRound = 10000;
        else
            nRound = 100000;

        int nRemain = n % nRound;
        n -= nRemain;
        if (nRound > 1 && nRemain >= (nRound / 2))
            n += nRound;

        return n;
    }

    /**
     * Clean up alloc entries
     */
    private void fixAllocs() {
        validator().fixAllocs();
    }

    /**
     * Return last defined level (assumes fixLevels called)
     */
    public int getLastLevel() {
        return map_.getInteger(PARAM_LASTLEVEL, 0);
    }

    /**
     * set percent for given player type
     */
    public void setPlayerTypePercent(String sPlayerTypeUniqueId, int pct) {
        if (sPlayerTypeUniqueId == null)
            return;

        if (pct <= 0) {
            map_.remove(PARAM_MIX + sPlayerTypeUniqueId);
        } else {
            map_.setInteger(PARAM_MIX + sPlayerTypeUniqueId, pct);
        }
    }

    /**
     * get percent for given player type
     */
    public int getPlayerTypePercent(String sPlayerTypeUniqueId) {
        Integer pct = map_.getInteger(PARAM_MIX + sPlayerTypeUniqueId);

        if (pct == null) {
            return 0;
        } else {
            return pct;
        }
    }

    /**
     * Does game have any limit levels?
     */
    public boolean hasLimitLevels() {
        if (getDefaultGameTypeString().equals(PokerConstants.DE_LIMIT_HOLDEM))
            return true;

        for (int i = getLastLevel(); i > 0; i--) {
            if (getGameTypeString(i).equals(PokerConstants.DE_LIMIT_HOLDEM))
                return true;
        }

        return false;
    }

    /**
     * to string for logging
     */
    @Override
    public String toString() {
        return getName();
    }

    ////
    //// Saved tournaments
    ////

    /**
     * allow editing of all tournaments, even pre-shipped ones
     */
    @Override
    public boolean canEdit() {
        return true;
    }

    /**
     * save - override to consolidate levels first
     */
    @Override
    public void save() {
        fixAll();
        super.save();
    }

    /**
     * fixstuff
     */
    public void fixAll() {
        validator().fixAll();
    }

    /**
     * subclass implements to load its contents from the given reader
     */
    @Override
    public void read(Reader reader, boolean bFull) throws IOException {
        BufferedReader buf = new BufferedReader(reader);
        super.read(buf, bFull);

        map_ = new DMTypedHashMap();
        map_.demarshal(null, buf.readLine());
    }

    /**
     * subclass implements to save its contents to the given writer
     */
    @Override
    public void write(Writer writer) throws IOException {
        super.write(writer);

        writer.write(map_.marshal(null));
        writeEndEntry(writer);
    }

    /**
     * Get list of save files in save directory
     */
    public static List<BaseProfile> getProfileList() {
        return BaseProfile.getProfileList(TOURNAMENT_DIR,
                Utils.getFilenameFilter(SaveFile.DELIM + PROFILE_EXT, PROFILE_BEGIN), TournamentProfile.class, false);
    }

    ////
    //// DataMarshal
    ////

    public void demarshal(MsgState state, String sData) {
        StringReader reader = new StringReader(sData);
        try {
            read(reader, true);
        } catch (IOException io) {
            throw new ApplicationError(io);
        }
    }

    public String marshal(MsgState state) {
        StringWriter writer = new StringWriter();
        try {
            write(writer);
        } catch (IOException io) {
            throw new ApplicationError(io);
        }
        return writer.toString();
    }

    ////
    //// XML Encoding
    ////

    public void encodeXML(SimpleXMLEncoder encoder) {
        serializer().encodeXML(encoder, this);
    }

    /**
     * Helper method to create ProfileXMLSerializer with this profile as data
     * source.
     */
    private ProfileXMLSerializer serializer() {
        return new ProfileXMLSerializer(new ProfileXMLSerializer.ProfileDataProvider() {
            @Override
            public int getLastLevel() {
                return TournamentProfile.this.getLastLevel();
            }

            @Override
            public int getMinutes(int level) {
                return TournamentProfile.this.getMinutes(level);
            }

            @Override
            public boolean isBreak(int level) {
                return TournamentProfile.this.isBreak(level);
            }

            @Override
            public String getGameTypeString(int level) {
                return TournamentProfile.this.getGameTypeString(level);
            }

            @Override
            public int getAnte(int level) {
                return TournamentProfile.this.getAnte(level);
            }

            @Override
            public int getBigBlind(int level) {
                return TournamentProfile.this.getBigBlind(level);
            }

            @Override
            public int getSmallBlind(int level) {
                return TournamentProfile.this.getSmallBlind(level);
            }

            @Override
            public int getNumSpots() {
                return TournamentProfile.this.getNumSpots();
            }

            @Override
            public String getSpotAsString(int spot) {
                return TournamentProfile.this.getSpotAsString(spot);
            }

            @Override
            public List<AbstractPlayerList.PlayerInfo> getInvitees() {
                return TournamentProfile.this.getInvitees();
            }

            @Override
            public List<String> getPlayers() {
                return TournamentProfile.this.getPlayers();
            }
        });
    }
}
