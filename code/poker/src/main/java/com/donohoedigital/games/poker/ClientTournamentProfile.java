/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * Copyright (c) 2026 DD Poker Community
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
package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.poker.protocol.constants.ProtocolConstants;
import com.donohoedigital.games.poker.protocol.dto.*;
import com.donohoedigital.xml.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static com.donohoedigital.config.DebugConfig.*;

/**
 * Client-side tournament profile. Replicates the API of
 * {@code com.donohoedigital.games.poker.model.TournamentProfile} without
 * depending on the pokerengine module. Uses the same
 * {@link DMTypedHashMap}-based storage and {@link BaseProfile} file I/O for
 * backward compatibility.
 */
public class ClientTournamentProfile extends BaseProfile implements DataMarshal, SimpleXMLEncodable {
    static Logger logger = LogManager.getLogger(ClientTournamentProfile.class);

    // defines
    public static final String PROFILE_BEGIN = "tourney";
    public static final String TOURNAMENT_DIR = "tournaments";

    // MAX values
    public static final int MAX_LEVELS = 40;
    public static final int MAX_SPOTS = 560;
    public static final int MIN_SPOTS = 10;
    public static final double MAX_SPOTS_PERCENT = .3333333d;
    public static final int MAX_PLAYERS = 5625;
    public static final int MAX_ONLINE_PLAYERS = 120;
    public static final int MAX_OBSERVERS = 30;

    public static final int MAX_CHIPS = TESTING(PokerClientConstants.TESTING_LEVELS) ? 10000000 : 1000000;
    public static final int MAX_REBUY_CHIPS = TESTING(PokerClientConstants.TESTING_LEVELS) ? 10000000 : 1000000;
    public static final int MAX_BUY = TESTING(PokerClientConstants.TESTING_LEVELS) ? 10000000 : 1000000;
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
    public static final int MAX_TIMEOUT = 120;
    public static final int MAX_THINKBANK = 120;
    public static final int MAX_BOOT_HANDS = 100;
    public static final int MIN_BOOT_HANDS = 5;
    public static final int ROUND_MULT = 1000;

    private static final MessageFormat FORMAT_PERC = new MessageFormat("{0}%");
    private static final MessageFormat FORMAT_AMOUNT = new MessageFormat("${0}");

    // Hands per level constraints
    public static final int MIN_HANDS_PER_LEVEL = 1;
    public static final int MAX_HANDS_PER_LEVEL = 100;
    public static final int DEFAULT_HANDS_PER_LEVEL = 10;

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
    public static final String PARAM_THINKBANK = "thinkbank";
    public static final String PARAM_MAX_OBSERVERS = "maxobservers";
    public static final String PARAM_MAX_ONLINE_PLAYERS = "maxonlineplayers";
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

    /**
     * Empty constructor for loading from data
     */
    public ClientTournamentProfile() {
        this("");
    }

    /**
     * Load profile from string file
     */
    public ClientTournamentProfile(String sFile, boolean bFull) {
        super(sFile, bFull);
    }

    /**
     * Load profile from file
     */
    public ClientTournamentProfile(File file, boolean bFull) {
        super(file, bFull);
    }

    /**
     * New profile with given name
     */
    public ClientTournamentProfile(String sName) {
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
        setPayout(PokerClientConstants.PAYOUT_SPOTS);
        setPayoutSpots(3);
        setMaxOnlinePlayers(60);
        fixAll();
    }

    /**
     * New profile copied from given profile, using new name
     */
    public ClientTournamentProfile(ClientTournamentProfile tp, String sName) {
        super(sName);
        map_ = new DMTypedHashMap();
        map_.putAll(tp.map_);
    }

    @Override
    protected String getBegin() {
        return PROFILE_BEGIN;
    }

    @Override
    protected String getProfileDirName() {
        return TOURNAMENT_DIR;
    }

    @Override
    protected List<BaseProfile> getProfileFileList() {
        return getProfileList();
    }

    public DMTypedHashMap getMap() {
        return map_;
    }

    public void setUpdateDate() {
        map_.setLong(PARAM_UPDATE, System.currentTimeMillis());
    }

    public long getUpdateDate() {
        return map_.getLong(PARAM_UPDATE, getCreateDate());
    }

    public String getDescription() {
        return map_.getString(PARAM_DESC, "");
    }

    public void setDescription(String s) {
        map_.setString(PARAM_DESC, s);
    }

    public String getGreeting(String sName) {
        String sGreeting = map_.getString(PARAM_GREETING, "").trim();
        if (sGreeting.length() == 0)
            return null;
        sGreeting = Utils.replace(sGreeting, "\\$name", sName);
        return sGreeting;
    }

    public int getNumPlayers() {
        return map_.getInteger(PARAM_NUM_PLAYERS, 0, 2, MAX_PLAYERS);
    }

    public void setNumPlayers(int n) {
        map_.setInteger(PARAM_NUM_PLAYERS, n);
    }

    public int getConfiguredMaxOnlinePlayers() {
        return map_.getInteger(PARAM_MAX_ONLINE_PLAYERS, 90, 2, MAX_ONLINE_PLAYERS);
    }

    public void setMaxOnlinePlayers(int max) {
        map_.setInteger(PARAM_MAX_ONLINE_PLAYERS, max);
    }

    public int getMaxOnlinePlayers() {
        return Math.min(getNumPlayers(), getConfiguredMaxOnlinePlayers());
    }

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
        setUpdateDate();
    }

    public List<String> getPlayers() {
        DMArrayList<String> players = (DMArrayList<String>) map_.getList(PARAM_PLAYERS);
        if (players == null)
            players = new DMArrayList<String>();
        return players;
    }

    public boolean isInviteOnly() {
        return map_.getBoolean(PARAM_INVITE_ONLY, false);
    }

    public void setInviteOnly(boolean b) {
        map_.setBoolean(PARAM_INVITE_ONLY, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isInviteObserversPublic() {
        return map_.getBoolean(PARAM_INVITE_OBS, false);
    }

    public void setInviteObserversPublic(boolean b) {
        map_.setBoolean(PARAM_INVITE_OBS, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public AbstractPlayerList getInvitees() {
        return new InviteePlayerList(this);
    }

    private static class InviteePlayerList extends AbstractPlayerList {
        ClientTournamentProfile profile;

        private InviteePlayerList(ClientTournamentProfile profile) {
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
        }

        @Override
        protected String fetchKeys() {
            return null;
        }
    }

    public int getSeats() {
        return map_.getInteger(PARAM_TABLE_SEATS, ProtocolConstants.SEATS, 2, ProtocolConstants.SEATS);
    }

    public int getBuyinCost() {
        return map_.getInteger(PARAM_BUYIN, 0, 1, MAX_BUY);
    }

    public void setBuyin(int n) {
        map_.setInteger(PARAM_BUYIN, n);
    }

    public int getBuyinChips() {
        return map_.getInteger(PARAM_BUYINCHIPS, 0, 1, MAX_CHIPS);
    }

    public void setBuyinChips(int n) {
        map_.setInteger(PARAM_BUYINCHIPS, n);
    }

    public int getStartingDepthBBs() {
        int bigBlind = getBigBlind(1);
        if (bigBlind <= 0)
            return -1;
        return getBuyinChips() / bigBlind;
    }

    public int getRebuyChipCount() {
        return map_.getInteger(PARAM_REBUYCHIPCNT, getBuyinChips(), 0, MAX_REBUY_CHIPS);
    }

    public void setRebuyChipCount(int n) {
        map_.setInteger(PARAM_REBUYCHIPCNT, n);
    }

    public int getRebuyExpressionType() {
        return map_.getInteger(PARAM_REBUYEXPR, PokerClientConstants.REBUY_LTE, PokerClientConstants.REBUY_LT,
                PokerClientConstants.REBUY_LTE);
    }

    public void setRebuyExpression(int n) {
        map_.setInteger(PARAM_REBUYEXPR, n);
    }

    // -------------------------------------------------------------------------
    // Blind structure
    // -------------------------------------------------------------------------

    public void clearLevel(int nLevel) {
        map_.remove(PARAM_ANTE + nLevel);
        map_.remove(PARAM_SMALL + nLevel);
        map_.remove(PARAM_BIG + nLevel);
        map_.remove(PARAM_MINUTES + nLevel);
        map_.remove(PARAM_GAMETYPE + nLevel);
    }

    public void clearAllLevels() {
        for (int i = 1; i <= MAX_LEVELS; i++) {
            clearLevel(i);
        }
    }

    public void setLevel(int nLevel, int ante, int small, int big, int minutes) {
        map_.setString(PARAM_ANTE + nLevel, String.valueOf(ante));
        map_.setString(PARAM_SMALL + nLevel, String.valueOf(small));
        map_.setString(PARAM_BIG + nLevel, String.valueOf(big));
        map_.setString(PARAM_MINUTES + nLevel, String.valueOf(minutes));
    }

    public int getSmallBlind(int nLevel) {
        return getBlindAmount(PARAM_SMALL, nLevel);
    }

    public int getBigBlind(int nLevel) {
        return getBlindAmount(PARAM_BIG, nLevel);
    }

    public int getAnte(int nLevel) {
        return getBlindAmount(PARAM_ANTE, nLevel);
    }

    public int getLastSmallBlind(int nLevel) {
        while (isBreak(nLevel) && nLevel > 0) {
            nLevel--;
        }
        return getSmallBlind(nLevel);
    }

    public int getLastBigBlind(int nLevel) {
        while (isBreak(nLevel) && nLevel > 0) {
            nLevel--;
        }
        return getBigBlind(nLevel);
    }

    public int getLastAnte(int nLevel) {
        while (isBreak(nLevel) && nLevel > 0) {
            nLevel--;
        }
        return getAnte(nLevel);
    }

    public boolean isBreak(int nLevel) {
        return getAmountFromString(PARAM_ANTE + nLevel, true) == BREAK_ANTE_VALUE;
    }

    public void setBreak(int nLevel, int nMinutes) {
        map_.setString(PARAM_ANTE + nLevel, Integer.toString(BREAK_ANTE_VALUE));
        map_.setString(PARAM_MINUTES + nLevel, Integer.toString(nMinutes));
        map_.remove(PARAM_SMALL + nLevel);
        map_.remove(PARAM_BIG + nLevel);
        map_.remove(PARAM_GAMETYPE + nLevel);
    }

    /**
     * Inline blind amount calculation with doubling logic.
     */
    private int getBlindAmount(String paramName, int level) {
        ApplicationError.assertTrue(!isBreak(level), "Attempting to get value for a break level", paramName);

        int lastLevel = getLastLevel();
        int effectiveLastLevel = lastLevel;
        int amount;

        if (level > lastLevel) {
            while (isBreak(effectiveLastLevel) && effectiveLastLevel > 0) {
                effectiveLastLevel--;
                level--;
            }
            amount = getAmountFromString(paramName + effectiveLastLevel, false);

            if (isDoubleAfterLastLevel()) {
                long longAmount = amount;
                for (int i = 0; i < (level - effectiveLastLevel); i++) {
                    longAmount *= 2;
                    if (longAmount >= MAX_BLINDANTE) {
                        longAmount /= 2;
                        break;
                    }
                }
                amount = (int) longAmount;
            }
        } else {
            amount = getAmountFromString(paramName + level, false);
        }

        return amount;
    }

    public int getDefaultMinutesPerLevel() {
        return map_.getInteger(PARAM_MINPERLEVEL_DEFAULT, 0, 1, MAX_MINUTES);
    }

    public void setMinutesPerLevel(int n) {
        map_.setInteger(PARAM_MINPERLEVEL_DEFAULT, n);
    }

    public int getMinutes(int nLevel) {
        int nAmount = getAmountFromString(PARAM_MINUTES + nLevel, false);
        if (nAmount == 0) {
            nAmount = getDefaultMinutesPerLevel();
        }
        if (nAmount > MAX_MINUTES)
            nAmount = MAX_MINUTES;
        return nAmount;
    }

    public void setDefaultGameType(String gameType) {
        map_.setString(PARAM_GAMETYPE_DEFAULT, gameType);
    }

    public void setGameType(int level, String gameType) {
        map_.setString(PARAM_GAMETYPE + level, gameType);
    }

    public String getDefaultGameTypeString() {
        return map_.getString(PARAM_GAMETYPE_DEFAULT, ProtocolConstants.DE_NO_LIMIT_HOLDEM);
    }

    public String getGameTypeString(int nLevel) {
        String sType = map_.getString(PARAM_GAMETYPE + nLevel);
        if (sType == null || sType.length() == 0) {
            sType = getDefaultGameTypeString();
        }
        return sType;
    }

    public String getGameTypeDisplay(int i) {
        String sGame = getGameTypeString(i);
        if (sGame.equals(getDefaultGameTypeString())) {
            return "";
        } else {
            return DataElement.getDisplayValue(DATA_ELEMENT_GAMETYPE, sGame);
        }
    }

    public int getGameType(int nLevel) {
        String sType = getGameTypeString(nLevel);
        if (sType.equals(ProtocolConstants.DE_NO_LIMIT_HOLDEM)) {
            return ProtocolConstants.TYPE_NO_LIMIT_HOLDEM;
        } else if (sType.equals(ProtocolConstants.DE_POT_LIMIT_HOLDEM)) {
            return ProtocolConstants.TYPE_POT_LIMIT_HOLDEM;
        }
        if (sType.equals(ProtocolConstants.DE_LIMIT_HOLDEM)) {
            return ProtocolConstants.TYPE_LIMIT_HOLDEM;
        }
        throw new ApplicationError(ErrorCodes.ERROR_INVALID, "Unknown poker game type", sType, null);
    }

    // -------------------------------------------------------------------------
    // Raises
    // -------------------------------------------------------------------------

    public int getMaxRaises(int nNumWithCards, boolean isComputer) {
        int maxRaises = map_.getInteger(PARAM_MAXRAISES, MAX_MAX_RAISES, 0, MAX_MAX_RAISES);
        if (isComputer) {
            maxRaises = Math.min(maxRaises, MAX_AI_RAISES);
        }
        if (maxRaises == 0) {
            return Integer.MAX_VALUE;
        }
        if (isRaiseCapIgnoredHeadsUp() && nNumWithCards <= 2) {
            return Integer.MAX_VALUE;
        }
        return maxRaises;
    }

    public boolean isRaiseCapIgnoredHeadsUp() {
        return map_.getBoolean(PARAM_MAXRAISES_NONE_HEADSUP, true);
    }

    public boolean hasLimitLevels() {
        if (getDefaultGameTypeString().equals(ProtocolConstants.DE_LIMIT_HOLDEM))
            return true;
        for (int i = getLastLevel(); i > 0; i--) {
            if (getGameTypeString(i).equals(ProtocolConstants.DE_LIMIT_HOLDEM))
                return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Payout
    // -------------------------------------------------------------------------

    public void setPayout(int nType) {
        map_.setInteger(PARAM_PAYOUT, nType);
    }

    public int getPayoutType() {
        return map_.getInteger(PARAM_PAYOUT, PokerClientConstants.PAYOUT_PERC, PokerClientConstants.PAYOUT_SPOTS,
                PokerClientConstants.PAYOUT_SATELLITE);
    }

    public void setPayoutSpots(int n) {
        map_.setInteger(PARAM_PAYOUTNUM, n);
    }

    public void setPayoutPercent(int n) {
        map_.setInteger(PARAM_PAYOUTPERC, n);
    }

    private int getPayoutSpots() {
        return map_.getInteger(PARAM_PAYOUTNUM, 3, 1, MAX_SPOTS);
    }

    public int getPayoutPercent() {
        return map_.getInteger(PARAM_PAYOUTPERC, 5, 1, MAX_PERC);
    }

    public int getNumSpots() {
        int numPlayers = getNumPlayers();
        switch (getPayoutType()) {
            case PokerClientConstants.PAYOUT_SPOTS :
                return Math.min(getPayoutSpots(), numPlayers);
            case PokerClientConstants.PAYOUT_PERC :
                int spots = (int) (numPlayers * (getPayoutPercent() / 100.0d));
                return Math.max(1, Math.min(spots, numPlayers));
            case PokerClientConstants.PAYOUT_SATELLITE :
                int pool = getPrizePool();
                double spot = getSpot(1);
                return (spot <= 0) ? 1 : Math.max(1, (int) (pool / spot));
            default :
                return 1;
        }
    }

    public void setAlloc(int nType) {
        map_.setInteger(PARAM_ALLOC, nType);
    }

    public boolean isAllocAuto() {
        return !isAllocSatellite() && map_.getInteger(PARAM_ALLOC, PokerClientConstants.ALLOC_AUTO,
                PokerClientConstants.ALLOC_AUTO, PokerClientConstants.ALLOC_AMOUNT) == PokerClientConstants.ALLOC_AUTO;
    }

    public boolean isAllocPercent() {
        return !isAllocSatellite() && map_.getInteger(PARAM_ALLOC, PokerClientConstants.ALLOC_AUTO,
                PokerClientConstants.ALLOC_AUTO, PokerClientConstants.ALLOC_AMOUNT) == PokerClientConstants.ALLOC_PERC;
    }

    public boolean isAllocFixed() {
        return !isAllocSatellite()
                && map_.getInteger(PARAM_ALLOC, PokerClientConstants.ALLOC_AUTO, PokerClientConstants.ALLOC_AUTO,
                        PokerClientConstants.ALLOC_AMOUNT) == PokerClientConstants.ALLOC_AMOUNT;
    }

    public boolean isAllocSatellite() {
        return getPayoutType() == PokerClientConstants.PAYOUT_SATELLITE;
    }

    public int getPrizePool() {
        int nPool = map_.getInteger(PARAM_PRIZEPOOL, -1);
        if (nPool != -1) {
            return nPool;
        }
        int nTotalPool = getNumPlayers() * getBuyinCost();
        return getPoolAfterHouseTake(nTotalPool);
    }

    public void setPrizePool(int nPool, boolean bAdjustForHouseTake) {
        if (bAdjustForHouseTake)
            nPool = getPoolAfterHouseTake(nPool);
        map_.setInteger(PARAM_PRIZEPOOL, nPool);
        if (isAllocAuto())
            setAutoSpots();
    }

    public int getPoolAfterHouseTake(int nPool) {
        int nType = getHouseCutType();
        if (nType == PokerClientConstants.HOUSE_PERC) {
            int perc = getHousePercent();
            if (perc > 0) {
                nPool = nPool - (int) (nPool * (perc / 100.0d));
            }
        } else {
            int amount = getHouseAmount();
            nPool = Math.max(0, nPool - (getNumPlayers() * amount));
        }
        return nPool;
    }

    public int getHouseCutType() {
        return map_.getInteger(PARAM_HOUSE, PokerClientConstants.HOUSE_PERC, PokerClientConstants.HOUSE_AMOUNT,
                PokerClientConstants.HOUSE_PERC);
    }

    public int getHousePercent() {
        return map_.getInteger(PARAM_HOUSEPERC, 0, 0, MAX_HOUSE_PERC);
    }

    public int getHouseAmount() {
        return map_.getInteger(PARAM_HOUSEAMOUNT, 0, 0, MAX_HOUSE_AMOUNT);
    }

    public int getTrueBuyin() {
        int nType = getHouseCutType();
        int buy = getBuyinCost();
        if (nType == PokerClientConstants.HOUSE_AMOUNT) {
            buy -= getHouseAmount();
        }
        return buy;
    }

    public int getMaxPayoutSpots(int nNumPlayers) {
        return Math.max(1, (int) (nNumPlayers * MAX_SPOTS_PERCENT));
    }

    public int getMaxPayoutPercent(int nNumPlayers) {
        return (int) (MAX_SPOTS_PERCENT * 100);
    }

    public double getSpot(int nNum) {
        return getSpotFromString(PARAM_SPOTAMOUNT + nNum);
    }

    public void setSpot(int nNum, double percentage) {
        map_.setString(PARAM_SPOTAMOUNT + nNum, String.valueOf(percentage));
    }

    public String getSpotAsString(int nNum) {
        return map_.getString(PARAM_SPOTAMOUNT + nNum, "");
    }

    public int getPayout(int nNum) {
        int numSpots = getNumSpots();
        int pool = getPrizePool();
        if (nNum > numSpots || numSpots == 0)
            return 0;

        if (isAllocSatellite()) {
            return (int) getSpot(1);
        }

        if (isAllocFixed()) {
            return (int) getSpot(nNum);
        }

        // percent or auto
        double spot = getSpot(nNum);
        int payout = (int) (pool * (spot / 100.0d));
        return Math.max(0, payout);
    }

    public void setAutoSpots() {
        // Simplified auto payout distribution
        int numSpots = getNumSpots();
        int pool = getPrizePool();
        if (numSpots <= 0 || pool <= 0)
            return;

        // Use a Fibonacci-based distribution
        double[] weights = new double[numSpots];
        double totalWeight = 0;
        for (int i = 0; i < numSpots; i++) {
            weights[i] = Math.pow(1.618, numSpots - i);
            totalWeight += weights[i];
        }

        int remaining = pool;
        for (int i = 0; i < numSpots; i++) {
            int amount;
            if (i == numSpots - 1) {
                amount = remaining; // last place gets remainder
            } else {
                amount = round((int) (pool * weights[i] / totalWeight));
                remaining -= amount;
            }
            map_.setString(PARAM_SPOTAMOUNT + (i + 1), FORMAT_AMOUNT.format(new Object[]{amount}));
        }
    }

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

    // -------------------------------------------------------------------------
    // Rebuys/Addons
    // -------------------------------------------------------------------------

    public boolean isDoubleAfterLastLevel() {
        return map_.getBoolean(PARAM_DOUBLE, true);
    }

    public boolean isRebuys() {
        return map_.getBoolean(PARAM_REBUYS, false);
    }

    public void setRebuys(boolean b) {
        map_.setBoolean(PARAM_REBUYS, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public int getRebuyCost() {
        return map_.getInteger(PARAM_REBUYCOST, 0, 1, MAX_BUY);
    }

    public int getRebuyChips() {
        return map_.getInteger(PARAM_REBUYCHIPS, 0, 1, MAX_CHIPS);
    }

    public int getLastRebuyLevel() {
        return map_.getInteger(PARAM_REBUY_UNTIL, 0, 1, MAX_LEVELS);
    }

    public void setLastRebuyLevel(int n) {
        map_.setInteger(PARAM_REBUY_UNTIL, n);
    }

    public int getMaxRebuys() {
        return map_.getInteger(PARAM_MAXREBUYS, 0, 0, MAX_REBUYS);
    }

    public boolean isAddons() {
        return map_.getBoolean(PARAM_ADDONS, false);
    }

    public void setAddons(boolean b) {
        map_.setBoolean(PARAM_ADDONS, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public int getAddonCost() {
        return map_.getInteger(PARAM_ADDONCOST, 0, 1, MAX_BUY);
    }

    public int getAddonChips() {
        return map_.getInteger(PARAM_ADDONCHIPS, 0, 1, MAX_CHIPS);
    }

    public int getAddonLevel() {
        return map_.getInteger(PARAM_ADDONLEVEL, 0, 1, MAX_LEVELS);
    }

    // -------------------------------------------------------------------------
    // Bounty
    // -------------------------------------------------------------------------

    public boolean isBountyEnabled() {
        return map_.getBoolean(PARAM_BOUNTY, false);
    }

    public void setBountyEnabled(boolean b) {
        map_.setBoolean(PARAM_BOUNTY, b ? Boolean.TRUE : Boolean.FALSE);
    }

    public int getBountyAmount() {
        return map_.getInteger(PARAM_BOUNTY_AMOUNT, 0, 0, MAX_BOUNTY);
    }

    public void setBountyAmount(int amount) {
        int clamped = Math.max(0, Math.min(amount, MAX_BOUNTY));
        map_.setInteger(PARAM_BOUNTY_AMOUNT, clamped);
    }

    // -------------------------------------------------------------------------
    // Level advance mode
    // -------------------------------------------------------------------------

    public ClientLevelAdvanceMode getLevelAdvanceMode() {
        String mode = map_.getString(PARAM_LEVEL_ADVANCE_MODE, ClientLevelAdvanceMode.TIME.name());
        return ClientLevelAdvanceMode.fromString(mode);
    }

    public void setLevelAdvanceMode(ClientLevelAdvanceMode mode) {
        if (mode == null) {
            mode = ClientLevelAdvanceMode.TIME;
        }
        map_.setString(PARAM_LEVEL_ADVANCE_MODE, mode.name());
    }

    public int getHandsPerLevel() {
        return map_.getInteger(PARAM_HANDS_PER_LEVEL, DEFAULT_HANDS_PER_LEVEL, MIN_HANDS_PER_LEVEL,
                MAX_HANDS_PER_LEVEL);
    }

    public void setHandsPerLevel(int hands) {
        int clamped = Math.max(MIN_HANDS_PER_LEVEL, Math.min(hands, MAX_HANDS_PER_LEVEL));
        map_.setInteger(PARAM_HANDS_PER_LEVEL, clamped);
    }

    // -------------------------------------------------------------------------
    // Online options
    // -------------------------------------------------------------------------

    public int getTimeoutSeconds() {
        return map_.getInteger(PARAM_TIMEOUT, 30, MIN_TIMEOUT, MAX_TIMEOUT);
    }

    public void setTimeoutSeconds(int seconds) {
        map_.setInteger(PARAM_TIMEOUT, seconds);
    }

    public int getTimeoutForRound(int round) {
        String param = getTimeoutParamForRound(round);
        if (param == null) {
            return getTimeoutSeconds();
        }
        int roundTimeout = map_.getInteger(param, 0, 0, MAX_TIMEOUT);
        if (roundTimeout > 0 && roundTimeout < MIN_TIMEOUT) {
            roundTimeout = MIN_TIMEOUT;
        }
        return (roundTimeout > 0) ? roundTimeout : getTimeoutSeconds();
    }

    private String getTimeoutParamForRound(int round) {
        switch (round) {
            case 0 :
                return PARAM_TIMEOUT_PREFLOP;
            case 1 :
                return PARAM_TIMEOUT_FLOP;
            case 2 :
                return PARAM_TIMEOUT_TURN;
            case 3 :
                return PARAM_TIMEOUT_RIVER;
            default :
                return null;
        }
    }

    public int getTimeoutPreflop() {
        return map_.getInteger(PARAM_TIMEOUT_PREFLOP, 0, 0, MAX_TIMEOUT);
    }

    public void setTimeoutPreflop(int seconds) {
        map_.setInteger(PARAM_TIMEOUT_PREFLOP, seconds);
    }

    public int getTimeoutFlop() {
        return map_.getInteger(PARAM_TIMEOUT_FLOP, 0, 0, MAX_TIMEOUT);
    }

    public void setTimeoutFlop(int seconds) {
        map_.setInteger(PARAM_TIMEOUT_FLOP, seconds);
    }

    public int getTimeoutTurn() {
        return map_.getInteger(PARAM_TIMEOUT_TURN, 0, 0, MAX_TIMEOUT);
    }

    public void setTimeoutTurn(int seconds) {
        map_.setInteger(PARAM_TIMEOUT_TURN, seconds);
    }

    public int getTimeoutRiver() {
        return map_.getInteger(PARAM_TIMEOUT_RIVER, 0, 0, MAX_TIMEOUT);
    }

    public void setTimeoutRiver(int seconds) {
        map_.setInteger(PARAM_TIMEOUT_RIVER, seconds);
    }

    public int getThinkBankSeconds() {
        return map_.getInteger(PARAM_THINKBANK, 15, 0, MAX_THINKBANK);
    }

    public int getMaxObservers() {
        int rawMax = map_.getInteger(PARAM_MAX_OBSERVERS, MAX_OBSERVERS, 0, MAX_OBSERVERS);
        return Math.min(rawMax, MAX_OBSERVERS);
    }

    public boolean isFillComputer() {
        return map_.getBoolean(PARAM_FILL_COMPUTER, true);
    }

    public boolean isAllowDash() {
        return map_.getBoolean(PARAM_ALLOW_DASH, false);
    }

    public boolean isAllowAdvisor() {
        return map_.getBoolean(PARAM_ALLOW_ADVISOR, false);
    }

    public boolean isBootSitout() {
        return map_.getBoolean(PARAM_BOOT_SITOUT, false);
    }

    public boolean isBootDisconnect() {
        return map_.getBoolean(PARAM_BOOT_DISCONNECT, true);
    }

    public int getBootSitoutCount() {
        return map_.getInteger(PARAM_BOOT_SITOUT_COUNT, 25, MIN_BOOT_HANDS, MAX_BOOT_HANDS);
    }

    public int getBootDisconnectCount() {
        return map_.getInteger(PARAM_BOOT_DISCONNECT_COUNT, 10, MIN_BOOT_HANDS, MAX_BOOT_HANDS);
    }

    // -------------------------------------------------------------------------
    // Late registration / scheduled start
    // -------------------------------------------------------------------------

    public boolean isLateRegEnabled() {
        return map_.getBoolean(PARAM_LATE_REG, false);
    }

    public void setLateRegEnabled(boolean enabled) {
        map_.setBoolean(PARAM_LATE_REG, enabled);
    }

    public int getLateRegUntilLevel() {
        return map_.getInteger(PARAM_LATE_REG_UNTIL, 1, 1, MAX_LEVELS);
    }

    public void setLateRegUntilLevel(int level) {
        map_.setInteger(PARAM_LATE_REG_UNTIL, level);
    }

    public int getLateRegChips() {
        return map_.getInteger(PARAM_LATE_REG_CHIPS, PokerClientConstants.LATE_REG_CHIPS_STARTING);
    }

    public void setLateRegChips(int mode) {
        map_.setInteger(PARAM_LATE_REG_CHIPS, mode);
    }

    public boolean isScheduledStartEnabled() {
        return map_.getBoolean(PARAM_SCHEDULED_START, false);
    }

    public void setScheduledStartEnabled(boolean enabled) {
        map_.setBoolean(PARAM_SCHEDULED_START, enabled);
    }

    public long getStartTime() {
        return map_.getLong(PARAM_START_TIME, 0L);
    }

    public void setStartTime(long timeMillis) {
        map_.setLong(PARAM_START_TIME, timeMillis);
    }

    public int getMinPlayersForStart() {
        return map_.getInteger(PARAM_MIN_PLAYERS_START, ProtocolConstants.MIN_SCHEDULED_START_PLAYERS, 2,
                MAX_ONLINE_PLAYERS);
    }

    public void setMinPlayersForStart(int minPlayers) {
        map_.setInteger(PARAM_MIN_PLAYERS_START, minPlayers);
    }

    // -------------------------------------------------------------------------
    // Player type mix
    // -------------------------------------------------------------------------

    public void setPlayerTypePercent(String sPlayerTypeUniqueId, int pct) {
        if (sPlayerTypeUniqueId == null)
            return;
        if (pct <= 0) {
            map_.remove(PARAM_MIX + sPlayerTypeUniqueId);
        } else {
            map_.setInteger(PARAM_MIX + sPlayerTypeUniqueId, pct);
        }
    }

    public int getPlayerTypePercent(String sPlayerTypeUniqueId) {
        Integer pct = map_.getInteger(PARAM_MIX + sPlayerTypeUniqueId);
        if (pct == null) {
            return 0;
        } else {
            return pct;
        }
    }

    // -------------------------------------------------------------------------
    // Level info
    // -------------------------------------------------------------------------

    public int getLastLevel() {
        return map_.getInteger(PARAM_LASTLEVEL, 0);
    }

    public void updateNumPlayers(int nNumPlayers) {
        setNumPlayers(nNumPlayers);
    }

    public int getSatellitePayout() {
        return (int) getSpot(1);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

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

    private double getSpotFromString(String sName) {
        String s = map_.getString(sName);
        double ret = Utils.parseStringToDouble(s, ROUND_MULT);
        if (ret < 0)
            ret = 0;
        if (ret > MAX_BLINDANTE)
            ret = MAX_BLINDANTE;
        return ret;
    }

    // -------------------------------------------------------------------------
    // Validation / fixAll
    // -------------------------------------------------------------------------

    public void fixLevels() {
        // Consolidate levels: remove gaps, enforce valid structure
        int dest = 1;
        for (int src = 1; src <= MAX_LEVELS; src++) {
            String ante = map_.getString(PARAM_ANTE + src);
            String small = map_.getString(PARAM_SMALL + src);
            String big = map_.getString(PARAM_BIG + src);
            if (ante == null && small == null && big == null) {
                continue; // skip empty levels
            }
            if (dest != src) {
                map_.setString(PARAM_ANTE + dest, ante);
                map_.setString(PARAM_SMALL + dest, small);
                map_.setString(PARAM_BIG + dest, big);
                String min = map_.getString(PARAM_MINUTES + src);
                if (min != null)
                    map_.setString(PARAM_MINUTES + dest, min);
                String gt = map_.getString(PARAM_GAMETYPE + src);
                if (gt != null)
                    map_.setString(PARAM_GAMETYPE + dest, gt);
                clearLevel(src);
            }
            dest++;
        }
        map_.setInteger(PARAM_LASTLEVEL, dest - 1);
    }

    public void fixAll() {
        fixLevels();
    }

    /**
     * Validate tournament profile settings and return warnings.
     */
    public ClientValidationResult validateProfile() {
        ClientValidationResult result = new ClientValidationResult();

        // Check unreachable levels
        boolean rebuysEnabled = map_.getBoolean(PARAM_REBUYS, false);
        if (rebuysEnabled) {
            int lastLevel = map_.getInteger(PARAM_LASTLEVEL, 0);
            int rebuyUntilLevel = map_.getInteger(PARAM_REBUY_UNTIL, 0);
            if (lastLevel > 0 && rebuyUntilLevel > 0) {
                double rebuyPortion = (double) rebuyUntilLevel / lastLevel;
                if (rebuyPortion < 0.25) {
                    result.addWarning(ClientValidationWarning.UNREACHABLE_LEVELS,
                            "Rebuy period ends very early at level " + rebuyUntilLevel + " ("
                                    + String.format("%.0f", rebuyPortion * 100) + "% of " + lastLevel
                                    + " total levels)");
                }
            }
        }

        // Check payout spots
        int numPlayers = getNumPlayers();
        int numSpots = getNumSpots();
        if (numPlayers > 0 && numSpots > numPlayers) {
            result.addWarning(ClientValidationWarning.TOO_MANY_PAYOUT_SPOTS,
                    numSpots + " payout spots configured but only " + numPlayers + " players");
        }

        // Check starting depth
        int buyinChips = getBuyinChips();
        String bigBlindStr = map_.getString(PARAM_BIG + "1");
        if (buyinChips > 0 && bigBlindStr != null && !bigBlindStr.isEmpty()) {
            try {
                int bigBlind = Integer.parseInt(bigBlindStr.trim());
                if (bigBlind > 0) {
                    int depth = buyinChips / bigBlind;
                    if (depth < 10) {
                        result.addWarning(ClientValidationWarning.SHALLOW_STARTING_DEPTH,
                                "Starting depth is " + depth + " big blinds (< 10)");
                    }
                }
            } catch (NumberFormatException e) {
                // Invalid big blind value, skip check
            }
        }

        // Check house take
        int houseType = getHouseCutType();
        if (houseType == PokerClientConstants.HOUSE_PERC) {
            int housePercent = getHousePercent();
            if (housePercent > 20) {
                result.addWarning(ClientValidationWarning.EXCESSIVE_HOUSE_TAKE,
                        "House take is " + housePercent + "% (> 20%)");
            }
        } else if (houseType == PokerClientConstants.HOUSE_AMOUNT) {
            int buyin = getBuyinCost();
            int houseAmount = getHouseAmount();
            if (buyin > 0) {
                double housePercent = (double) houseAmount / buyin * 100;
                if (housePercent > 20) {
                    result.addWarning(ClientValidationWarning.EXCESSIVE_HOUSE_TAKE, "House take is $" + houseAmount
                            + " (" + String.format("%.1f", housePercent) + "% of $" + buyin + " buy-in)");
                }
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    @Override
    public boolean canEdit() {
        return true;
    }

    @Override
    public void save() {
        fixAll();
        super.save();
    }

    @Override
    public void read(Reader reader, boolean bFull) throws IOException {
        BufferedReader buf = new BufferedReader(reader);
        super.read(buf, bFull);
        map_ = new DMTypedHashMap();
        map_.demarshal(null, buf.readLine());
    }

    @Override
    public void write(Writer writer) throws IOException {
        super.write(writer);
        writer.write(map_.marshal(null));
        writeEndEntry(writer);
    }

    public static List<BaseProfile> getProfileList() {
        return BaseProfile.getProfileList(TOURNAMENT_DIR,
                Utils.getFilenameFilter(SaveFile.DELIM + PROFILE_EXT, PROFILE_BEGIN), ClientTournamentProfile.class,
                false);
    }

    // -------------------------------------------------------------------------
    // DataMarshal
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // XML Encoding
    // -------------------------------------------------------------------------

    public void encodeXML(SimpleXMLEncoder encoder) {
        encoder.setCurrentObject(this, "tournament");
        encoder.addAllTagsExcept("profileFileList", "profileDirName", "begin", "map", "updateDate");
        encoder.finishCurrentObject();
    }

    @Override
    public String toString() {
        return getName();
    }

    // -------------------------------------------------------------------------
    // Protocol conversion
    // -------------------------------------------------------------------------

    /**
     * Convert to a protocol DTO for sending to the server.
     */
    public TournamentProfileData toProfileData() {
        List<BlindLevelData> blindLevels = new ArrayList<>();
        for (int i = 1; i <= getLastLevel(); i++) {
            if (isBreak(i)) {
                blindLevels.add(new BlindLevelData(i, 0, 0, 0, getMinutes(i), true, null));
            } else {
                blindLevels.add(new BlindLevelData(i, getSmallBlind(i), getBigBlind(i), getAnte(i), getMinutes(i),
                        false, getGameTypeString(i)));
            }
        }

        List<Double> spotAllocations = new ArrayList<>();
        for (int i = 1; i <= getNumSpots(); i++) {
            spotAllocations.add(getSpot(i));
        }

        String allocationType;
        if (isAllocAuto())
            allocationType = "AUTO";
        else if (isAllocPercent())
            allocationType = "PERCENT";
        else if (isAllocFixed())
            allocationType = "FIXED";
        else
            allocationType = "AUTO";

        String payoutType;
        switch (getPayoutType()) {
            case PokerClientConstants.PAYOUT_SPOTS :
                payoutType = "SPOTS";
                break;
            case PokerClientConstants.PAYOUT_PERC :
                payoutType = "PERCENT";
                break;
            case PokerClientConstants.PAYOUT_SATELLITE :
                payoutType = "SATELLITE";
                break;
            default :
                payoutType = "SPOTS";
        }

        String houseCutType;
        if (getHouseCutType() == PokerClientConstants.HOUSE_AMOUNT) {
            houseCutType = "AMOUNT";
        } else {
            houseCutType = "PERCENT";
        }

        String rebuyExpr;
        if (getRebuyExpressionType() == PokerClientConstants.REBUY_LT) {
            rebuyExpr = "LT";
        } else {
            rebuyExpr = "LTE";
        }

        String lateRegChipMode;
        if (getLateRegChips() == PokerClientConstants.LATE_REG_CHIPS_AVERAGE) {
            lateRegChipMode = "AVERAGE";
        } else {
            lateRegChipMode = "STARTING";
        }

        return new TournamentProfileData(getName(), getDescription(),
                map_.getString(PARAM_GREETING, "").trim().isEmpty() ? null : map_.getString(PARAM_GREETING, "").trim(),
                getNumPlayers(), getMaxOnlinePlayers(), getSeats(), isFillComputer(), getBuyinCost(), getBuyinChips(),
                blindLevels, isDoubleAfterLastLevel(), getDefaultGameTypeString(), getLevelAdvanceMode().name(),
                getHandsPerLevel(), getDefaultMinutesPerLevel(), isRebuys(), getRebuyCost(), getRebuyChips(),
                getRebuyChipCount(), getMaxRebuys(), getLastRebuyLevel(), rebuyExpr, isAddons(), getAddonCost(),
                getAddonChips(), getAddonLevel(), payoutType, getPayoutPercent(), getPrizePool(), getNumSpots(),
                spotAllocations, allocationType, houseCutType, getHousePercent(), getHouseAmount(), isBountyEnabled(),
                getBountyAmount(), getTimeoutSeconds(), getTimeoutPreflop(), getTimeoutFlop(), getTimeoutTurn(),
                getTimeoutRiver(), getThinkBankSeconds(), isBootSitout(), getBootSitoutCount(), isBootDisconnect(),
                getBootDisconnectCount(), isAllowDash(), isAllowAdvisor(), getMaxObservers(),
                map_.getInteger(PARAM_MAXRAISES, MAX_MAX_RAISES, 0, MAX_MAX_RAISES), isRaiseCapIgnoredHeadsUp(),
                isLateRegEnabled(), getLateRegUntilLevel(), lateRegChipMode, isScheduledStartEnabled(),
                getStartTime() > 0 ? java.time.Instant.ofEpochMilli(getStartTime()) : null, getMinPlayersForStart(),
                isInviteOnly(),
                getInvitees().size() > 0
                        ? getInvitees().stream().map(AbstractPlayerList.PlayerInfo::getName)
                                .collect(java.util.stream.Collectors.toList())
                        : null,
                isInviteObserversPublic());
    }
}
