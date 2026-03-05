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
 * PokerGame.java
 *
 * Created on December 30, 2003, 4:34 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.model.LevelAdvanceMode;
import com.donohoedigital.games.poker.online.*;
import org.apache.logging.log4j.*;

import java.io.*;
import java.security.*;
import java.util.*;
import com.donohoedigital.games.poker.engine.GamePlayerInfo;
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.engine.state.BettingRound;

/**
 * @author donohoe
 */
public class PokerGame extends Game implements PlayerActionListener {
    static Logger logger = LogManager.getLogger(PokerGame.class);

    public static final int ACTION_FOLD = 1;
    public static final int ACTION_CHECK = 2;
    public static final int ACTION_CALL = 3;
    public static final int ACTION_BET = 4;
    public static final int ACTION_RAISE = 5;
    public static final int ACTION_ALL_IN = 6;
    public static final int ACTION_CONTINUE_LOWER = 7;
    public static final int ACTION_CONTINUE = 8;

    // home game save starts with
    public static final String HOME_BEGIN = "home";

    // denominations for chips
    private static final int nChipDenom_[] = new int[]{1, 5, 25, 100, 500, 1000, 5000, 10000, 50000, 100000};

    /**
     * Name used in PropertyChangeEvents when current table changed
     */
    public static final String PROP_CURRENT_TABLE = "_current_table_";

    /**
     * Name used in PropertyChangeEvents when current level changed
     */
    public static final String PROP_CURRENT_LEVEL = "_current_level_";

    /**
     * Name used in PropertyChangeEvents when profile changed
     */
    public static final String PROP_PROFILE = "_profile_";

    /**
     * Name used in PropertyChangeEvents when tables added/removed
     */
    public static final String PROP_TABLES = "_tables_";

    /**
     * Name used in PropertyChangeEvents when player busts out
     */
    public static final String PROP_PLAYER_FINISHED = "_busted_";

    // game info
    private DMArrayList<ClientPokerTable> tables_ = new DMArrayList<ClientPokerTable>();
    private TournamentProfile profile_;
    private int nLevel_ = 0;
    private boolean bClockMode_ = false;
    private boolean bSimulatorMode_ = false;
    private long id_;
    private int nMinChipIdx_ = 0;
    private int nLastMinChipIdx_ = 0;
    private int nExtraChips_ = 0;
    private int nClockCash_ = 0;
    // @GuardedBy("game thread") - accessed only from single-threaded game logic per
    // table
    private int nHandsInLevel_ = 0; // tracks hands played in current level for HANDS mode

    // Server-provided tournament info for online games (updated via GAME_STATE)
    private int serverTotalPlayers_;
    private int serverPlayersRemaining_;
    private int serverNumTables_;
    private int serverPlayerRank_;

    // online game and other info added for 2.0
    private String sLocalIP_;
    private String sPublicIP_;
    private boolean bPublic_;
    private int nPort_;
    private int nOnlineMode_ = MODE_NONE;
    private ClientPokerTable currentTable_;
    private int lastHandSaved_ = 0;
    private int nNumOut_ = 0;

    // clock object used to store seconds remaining, used in tournament/poker night
    // manager
    private GameClock clock_ = new GameClock();

    // input mode
    public static final int MODE_NONE = -1;
    public static final int MODE_INIT = 0;
    public static final int MODE_REG = 1;
    public static final int MODE_PLAY = 2;
    public static final int MODE_CLIENT = 3;
    public static final int MODE_CANCELLED = 4;

    ////
    //// members below are transient (not saved)
    ////

    // total chips
    private int totalChipsInPlay_;
    private boolean deleteHandsAfterSaveDate_ = false;

    // UI bridge
    private PokerTableInput input_ = null;

    // online transient
    private boolean bStartFromLobby_;

    /**
     *
     */
    public PokerGame() {
        super(GameEngine.getGameEngine().getDefaultContext()); // Future: get right game context for multi-game
    }

    /**
     * empty constructor for loading
     *
     * @param context
     */
    public PokerGame(GameContext context) {
        super(context);
    }

    /**
     * Set whether the TD should run the start logic to move clients from the lobby
     */
    public void setStartFromLobby(boolean b) {
        bStartFromLobby_ = b;
    }

    /**
     * Was this game started from the lobby
     */
    public boolean isStartFromLobby() {
        return bStartFromLobby_;
    }

    /**
     * Return default for tournaments and "home" for home games
     */
    @Override
    public String getBegin() {
        if (isClockMode())
            return HOME_BEGIN;
        else
            return super.getBegin(); // handles on-line too
    }

    /**
     * Get description for save game
     */
    @Override
    public String getDescription() {
        if (isClockMode())
            return profile_.getName();
        else if (isOnlineGame()) {
            if (profile_ == null)
                return "";
            return PropertyConfig.getMessage("msg.savegame.desc.o", profile_.getName(), getNumPlayers());
        } else {
            ClientPlayer human = getHumanPlayer();
            Integer chips = human.getChipCount();
            return PropertyConfig.getMessage("msg.savegame.desc", human.getName(), chips, profile_.getName());
        }
    }

    /**
     * Set clock mode
     */
    public void setClockMode(boolean b) {
        bClockMode_ = b;
    }

    /**
     * Is clock mode?
     */
    public boolean isClockMode() {
        return bClockMode_;
    }

    /**
     * Set simulator mode
     */
    public void setSimulatorMode(boolean b) {
        bSimulatorMode_ = b;
    }

    /**
     * Is simulator mode?
     */
    public boolean isSimulatorMode() {
        return bSimulatorMode_;
    }

    /**
     * set cash collected in home game
     */
    public void setClockCash(int n) {
        nClockCash_ = n;
    }

    /**
     * Get home cash
     */
    public int getClockCash() {
        return nClockCash_;
    }

    /**
     * Update profile for online game too (override completely so prop change event
     * happens after profile updated)
     */
    @Override
    public void addPlayer(GamePlayer player) {
        players_.add(player);
        updatePlayerList((ClientPlayer) player);
        firePropertyChange(PROP_PLAYERS, null, player);
    }

    /**
     * Update profile for online game too (override completely so prop change event
     * happens after profile updated)
     */
    @Override
    public void removePlayer(GamePlayer player) {
        players_.remove(player);
        updatePlayerList((ClientPlayer) player);
        firePropertyChange(PROP_PLAYERS, player, null);
    }

    /**
     * Return copy of player list (thus it can be changed)
     */
    public List<ClientPlayer> getPokerPlayersCopy() {
        List<ClientPlayer> copy = new ArrayList<ClientPlayer>();
        for (GamePlayer p : players_) {
            copy.add((ClientPlayer) p);
        }
        return copy;
    }

    /**
     * update list of players in tournament profile based on given player
     */
    public void updatePlayerList(ClientPlayer player) {
        if (isOnlineGame() && !player.isComputer()) {
            updatePlayerList();
        }
    }

    /**
     * Update list of players in tournament profile
     */
    private void updatePlayerList() {
        if (profile_ != null) {
            List<String> list = new ArrayList<String>();
            int nNum = getNumPlayers();
            for (int i = 0; i < nNum; i++) {
                ClientPlayer p = getPokerPlayerAt(i);
                if (p.isHuman() && !p.isEliminated()) {
                    list.add(p.getName());
                }
            }

            // save in profile
            profile_.setPlayers(list);
        }
    }

    /**
     * get poker player
     */
    public ClientPlayer getPokerPlayerAt(int n) {
        return (ClientPlayer) getPlayerAt(n);
    }

    /**
     * Get poker observer
     */
    public ClientPlayer getPokerObserverAt(int n) {
        return (ClientPlayer) getObserverAt(n);
    }

    /**
     * Get number of observers who were not players
     */
    public int getNumObserversNonPlayers() {
        int cnt = 0;
        int n = getNumObservers();
        for (int i = 0; i < n; i++) {
            // observer was a player if they have a finish
            if (getPokerObserverAt(i).getPlace() == 0) {
                cnt++;
            }
        }
        return cnt;
    }

    /**
     * Remove observer - override to make sure players is removed from their table's
     * observer list too
     *
     * @param player
     */
    @Override
    public void removeObserver(GamePlayer player) {
        ClientPlayer pokerPlayer = ((ClientPlayer) player);
        ClientPokerTable table = pokerPlayer.getTable();
        if (table != null)
            table.removeObserver(pokerPlayer);
        super.removeObserver(player);
    }

    /**
     * Get poker player by id. Player returned could be an observer.
     */
    public ClientPlayer getPokerPlayerFromID(int n) {
        return (ClientPlayer) getPlayerFromID(n, true);
    }

    /**
     * Get player by ID.
     */
    public GamePlayerInfo getPlayerByID(int playerId) {
        return getPokerPlayerFromID(playerId);
    }

    /**
     * Get poker player by key - used in online games. Player returned could be an
     * observer.
     */
    public ClientPlayer getPokerPlayerFromKey(String sKey) {
        // there should only be one (human) player
        // per key - due to logic in OnlineManager.
        // Thus, we don't check for multiple, we just
        // get the first one we find.
        ClientPlayer p;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++) {
            p = getPokerPlayerAt(i);
            if (p.isComputer())
                continue;
            if (sKey.equals(p.getPlayerId()))
                return p;
        }

        // search observers 2nd
        nNum = getNumObservers();
        for (int i = 0; i < nNum; i++) {
            p = getPokerObserverAt(i);
            if (sKey.equals(p.getPlayerId()))
                return p;
        }

        return null;
    }

    /**
     * Return the human player. In online games, returns local player. In WebSocket
     * mode, finds the human-controlled player in the current table.
     */
    public ClientPlayer getHumanPlayer() {
        if (isOnlineGame())
            return getLocalPlayer();
        // In WebSocket mode the human player lives in RemotePokerTable.remotePlayers_,
        // not in the game's own player list (which still holds the PracticeGameLauncher
        // placeholder). Scan the current table's seats to find the human player.
        if (webSocketConfig_ != null) {
            ClientPokerTable table = getCurrentTable();
            if (table != null) {
                for (int s = 0; s < PokerConstants.SEATS; s++) {
                    ClientPlayer p = table.getPlayer(s);
                    if (p != null && p.isHuman())
                        return p;
                }
            }
        }
        return getPokerPlayerFromID(PokerConstants.PLAYER_ID_HOST);
    }

    /**
     * Get locally controlled player
     */
    public ClientPlayer getLocalPlayer() {
        String sKey = getPlayerId();
        ClientPlayer local = getPokerPlayerFromKey(sKey);
        ApplicationError.assertNotNull(local, "No player matching current key", sKey);
        return local;
    }

    private String sPubKey_ = null;

    /**
     * get public use key from engine (cache locally to avoid unnecessary String
     * creation)
     */
    private String getPlayerId() {
        if (sPubKey_ == null)
            sPubKey_ = GameEngine.getGameEngine().getPlayerId();
        return sPubKey_;
    }

    /**
     * Get the host of online games
     */
    public ClientPlayer getHost() {
        return getPokerPlayerFromID(GamePlayer.HOST_ID);
    }

    /**
     * Return rank of player based on chips
     */
    public int getRank(ClientPlayer player) {
        ClientPlayer p;
        int nLastChips = 0;
        int nRank = 0;
        int nChips;
        List<ClientPlayer> rank = getPlayersByRank();
        for (int i = 0; i < rank.size(); i++) {
            p = rank.get(i);
            nChips = p.getChipCount();
            if (nChips != nLastChips) {
                nRank = (i + 1);
            }
            nLastChips = nChips;
            if (p == player)
                return nRank;
        }
        throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "No rank for player", player.toString());
    }

    /**
     * Get sorted list of players
     */
    public List<ClientPlayer> getPlayersByRank() {
        List<ClientPlayer> sort = getPokerPlayersCopy();
        Collections.sort(sort, SORTCHIPS);
        return sort;
    }

    // instances for sorting
    private static SortChips SORTCHIPS = new SortChips();

    // sort players by chips they have at start of hand
    private static class SortChips implements Comparator<ClientPlayer> {
        /**
         * Compares its two arguments for order. Returns a negative integer, zero, or a
         * positive integer as the first argument is less than, equal to, or greater
         * than the second.
         */
        public int compare(ClientPlayer p1, ClientPlayer p2) {
            // reverse comparison so highest chips at top
            int diff = p2.getChipCount() - p1.getChipCount();
            if (diff != 0)
                return diff;

            // if no diff, and chip count is zero, sort by place
            // normal comparison so best finish at top
            if (p1.getChipCount() == 0) {
                diff = p1.getPlace() - p2.getPlace();
                if (diff != 0)
                    return diff;
            }

            // if still no diff, rank by id, which puts human towards the top
            return p1.getID() - p2.getID();
        }
    }

    /**
     * Set current poker table
     */
    public void setCurrentTable(ClientPokerTable current) {
        if (current == currentTable_)
            return;

        ClientPokerTable old = currentTable_;
        currentTable_ = current;

        if (old != null)
            old.setCurrent(false);
        if (current != null)
            current.setCurrent(true);

        firePropertyChange(PROP_CURRENT_TABLE, old, current);
    }

    /**
     * Get poker table being displayed
     */
    public ClientPokerTable getCurrentTable() {
        return currentTable_;
    }

    /**
     * get num tables
     */
    public int getNumTables() {
        return tables_.size();
    }

    /**
     * Server-provided tournament info for online games.
     */
    public int getServerTotalPlayers() {
        return serverTotalPlayers_;
    }

    public int getServerPlayersRemaining() {
        return serverPlayersRemaining_;
    }

    public int getServerNumTables() {
        return serverNumTables_;
    }

    public int getServerPlayerRank() {
        return serverPlayerRank_;
    }

    public void setServerTournamentInfo(int totalPlayers, int playersRemaining, int numTables, int playerRank) {
        this.serverTotalPlayers_ = totalPlayers;
        this.serverPlayersRemaining_ = playersRemaining;
        this.serverNumTables_ = numTables;
        this.serverPlayerRank_ = playerRank;
    }

    /**
     * Add a table to the list of tables maintained by the game. Added table passed
     * as "new" value in PROP_TABLES event.
     */
    public void addTable(ClientPokerTable table) {
        tables_.add(table);
        firePropertyChange(PROP_TABLES, null, table);

    }

    /**
     * Get table at index as GameTable (for save/load compatibility).
     */
    public GameTable getTable(int i) {
        ClientPokerTable t = tables_.get(i);
        if (t instanceof GameTable)
            return (GameTable) t;
        throw new IllegalStateException(
                "getTable(" + i + ") called on a non-engine table. " + "Use getTableByNumber() in WebSocket paths.");
    }

    /**
     * Get table by number
     */
    public ClientPokerTable getTableByNumber(int nTableNum) {
        int nNum = getNumTables();
        for (int i = 0; i < nNum; i++) {
            ClientPokerTable table = tables_.get(i);
            if (table.getNumber() == nTableNum) {
                return table;
            }
        }
        return null;
    }

    /**
     * Remove table from tournament. Removed table passed as "old" value in
     * PROP_TABLES event.
     */
    public void removeTable(ClientPokerTable table) {
        tables_.remove(table);
        table.setRemoved(true);
        firePropertyChange(PROP_TABLES, table, null);
    }

    /**
     * Get array of tables
     */
    public List<ClientPokerTable> getTables() {
        return tables_;
    }

    /**
     * Get current level
     */
    public int getLevel() {
        return nLevel_;
    }

    /**
     * Sets the current blind level (called by WebSocketTournamentDirector on
     * LEVEL_CHANGED).
     */
    public void setLevel(int level) {
        nLevel_ = level;
    }

    // -------------------------------------------------------------------------
    // WebSocket configuration (set by PracticeGameLauncher, read by WsTD)
    // -------------------------------------------------------------------------

    /** Immutable connection parameters for WebSocketTournamentDirector. */
    public record WebSocketConfig(String gameId, String jwt, String host, int port, boolean observer) {
        /** Convenience constructor for embedded/localhost server. */
        public WebSocketConfig(String gameId, String jwt, int port) {
            this(gameId, jwt, "localhost", port, false);
        }
        /** Convenience constructor for embedded/localhost server with observer flag. */
        public WebSocketConfig(String gameId, String jwt, int port, boolean observer) {
            this(gameId, jwt, "localhost", port, observer);
        }
    }

    private WebSocketConfig webSocketConfig_;

    /** Set config for embedded (practice) server — host defaults to localhost. */
    public void setWebSocketConfig(String gameId, String jwt, int port) {
        webSocketConfig_ = new WebSocketConfig(gameId, jwt, port);
    }

    /** Set config for embedded (practice) server with observer flag. */
    public void setWebSocketConfig(String gameId, String jwt, int port, boolean observer) {
        webSocketConfig_ = new WebSocketConfig(gameId, jwt, port, observer);
    }

    /** Set config for central server — caller provides explicit host. */
    public void setWebSocketConfig(String gameId, String jwt, String host, int port) {
        webSocketConfig_ = new WebSocketConfig(gameId, jwt, host, port, false);
    }

    /** Set config for central server with observer flag. */
    public void setWebSocketConfig(String gameId, String jwt, String host, int port, boolean observer) {
        webSocketConfig_ = new WebSocketConfig(gameId, jwt, host, port, observer);
    }

    /** Returns the WebSocket connection parameters, or null if not yet set. */
    public WebSocketConfig getWebSocketConfig() {
        return webSocketConfig_;
    }

    /**
     * Returns {@code true} when the game loop is driven by the embedded server (via
     * {@link com.donohoedigital.games.poker.online.WebSocketTournamentDirector}).
     * In this mode, AI decisions are made server-side by {@code ServerAIProvider}
     * and should not be computed locally.
     */
    public boolean isServerDriven() {
        return webSocketConfig_ != null;
    }

    private WebSocketOpponentTracker wsOpponentTracker_;

    public WebSocketOpponentTracker getWebSocketOpponentTracker() {
        return wsOpponentTracker_;
    }

    public void setWebSocketOpponentTracker(WebSocketOpponentTracker tracker) {
        wsOpponentTracker_ = tracker;
    }

    /**
     * init chip count over all players (and specify their buyin), init think bank
     * millis and profile
     */
    private void initPlayers(int n, boolean bOnline) {
        ClientPlayer p;
        for (int i = 0; i < getNumPlayers(); i++) {
            p = getPokerPlayerAt(i);
            p.setChipCount(n);
            p.setBuyin(profile_.getBuyinCost());
            if (p.isProfileDefined())
                p.getProfile().init(); // init profile at start of tournament
            if (bOnline && !p.isComputer() && !p.isHost())
                p.setSittingOut(true); // sitout at start
        }
    }

    /**
     * Calc next min chip
     */
    private int calcMinChip(int nLevel) {
        // use current level (up to max)
        int nMin = Integer.MAX_VALUE;

        // figure out min chip amount for all players if level 1
        if (nLevel == 1) {
            if (isClockMode()) {
                nMin = profile_.getBuyinChips();
            } else {
                int nNum = getNumPlayers();
                for (int i = 0; i < nNum; i++) {
                    nMin = Math.min(nMin, getMaxDenom(getPokerPlayerAt(i).getChipCount()));
                }
            }
        }

        // if can rebuy, take into account those chips
        if (profile_.isRebuys() && nLevel <= profile_.getLastRebuyLevel()) {
            nMin = Math.min(nMin, getMaxDenom(profile_.getRebuyChips()));
        }

        // if addon, take into account those chips too
        if (profile_.isAddons() && nLevel <= profile_.getAddonLevel()) {
            nMin = Math.min(nMin, getMaxDenom(profile_.getAddonChips()));
        }

        // from this level up to remaining levels, figure out smallest max chip to
        // cover all antes/blinds
        int nAnte, nBig, nSmall;
        for (int i = nLevel; i <= Math.max(nLevel, TournamentProfile.MAX_LEVELS); i++) {
            if (profile_.isBreak(i))
                continue;
            nAnte = profile_.getAnte(i);
            nSmall = profile_.getSmallBlind(i);
            nBig = profile_.getBigBlind(i);
            if (nAnte > 0)
                nMin = Math.min(nMin, getMaxDenom(nAnte));
            nMin = Math.min(nMin, Math.min(getMaxDenom(nBig), getMaxDenom(nSmall)));
        }

        return nMin;
    }

    /**
     * Get largest chip denom which divides into value
     */
    private int getMaxDenom(int n) {
        int nChip = 1;
        // start at largest chip and look for a chip that
        // is less than or equal to min ante/blind and
        // is evenly divisble by that blind
        for (int i = nChipDenom_.length - 1; i >= 0; i--) {
            if (nChipDenom_[i] <= n && n % nChipDenom_[i] == 0) {
                nChip = nChipDenom_[i];
                break;
            }
        }
        return nChip;
    }

    /**
     * Add extra chips created during race-offs
     */
    public void addExtraChips(int n) {
        nExtraChips_ += n;
        totalChipsInPlay_ += n;
    }

    /**
     * Get min chip denomination
     */
    public int getMinChip() {
        return nChipDenom_[nMinChipIdx_];
    }

    /**
     * Get chip denom prior to given denom
     */
    public int getLastMinChip() {
        return nChipDenom_[nLastMinChipIdx_];
    }

    /**
     * Get chip denom index for next (non-break) level
     */
    private int getNextMinChipIndex() {
        // init
        int nLevel = nLevel_ + 1;

        // if next level is a break, look for next
        // non-break level
        while (profile_.isBreak(nLevel)) {
            nLevel++;
        }
        int nMinIdx = nMinChipIdx_;

        // if min chip for this level is greater
        // than last recorded min chip, increase it to
        // the next level - we increase chip level one
        // step at a time
        int nNewMinChip = calcMinChip(nLevel);
        int nMinChip = getMinChip();
        if (nNewMinChip > nMinChip) {
            if (nLevel == 1) {
                for (int i = 0; i < nChipDenom_.length; i++) {
                    if (nNewMinChip == nChipDenom_[i]) {
                        nMinIdx = i;
                        break;
                    }
                }
            } else {
                nMinIdx++;
                if (nMinIdx == nChipDenom_.length)
                    nMinIdx--;
            }
        }
        return nMinIdx;
    }

    /**
     * Is there only one player left with chips
     */
    public boolean isOnePlayerLeft() {
        // shortcut - if we have multiple tables left, we
        // still have multiple players
        if (getNumTables() > 1)
            return false;

        // check all players (could be some not at a table, waiting)
        int nNumWithChips = 0;
        ClientPlayer player;
        int nNum = getNumPlayers();
        for (int i = 0; i < nNum; i++) {
            player = getPokerPlayerAt(i);
            // safety check in case of erroneous chips being added to eliminated player
            // (like late-registering rebuy)
            if (player.isEliminated())
                continue;
            if (player.getChipCount() > 0)
                nNumWithChips++;
            if (nNumWithChips > 1)
                return false;
        }
        return (nNumWithChips == 1);
    }

    /**
     * Get big blind for current level
     */
    public int getBigBlind() {
        return profile_.getBigBlind(nLevel_);
    }

    /**
     * Get big blind for a specific level
     */
    public int getBigBlind(int level) {
        return profile_.getBigBlind(level);
    }

    /**
     * Get small blind for current level
     */
    public int getSmallBlind() {
        return profile_.getSmallBlind(nLevel_);
    }

    /**
     * Get small blind for a specific level
     */
    public int getSmallBlind(int level) {
        return profile_.getSmallBlind(level);
    }

    /**
     * Get ante for current level
     */
    public int getAnte() {
        return profile_.getAnte(nLevel_);
    }

    /**
     * Get ante for a specific level
     */
    public int getAnte(int level) {
        return profile_.getAnte(level);
    }

    /**
     * Get starting chips for the tournament.
     */
    public int getStartingChips() {
        return profile_.getBuyinChips();
    }

    public boolean isRebuyPeriodActive(GamePlayerInfo player) {
        if (player == null) {
            return false;
        }
        ClientPlayer pokerPlayer = (ClientPlayer) player;
        return !pokerPlayer.getTable().isRebuyDone(pokerPlayer);
    }

    /**
     * Get profile
     */
    public TournamentProfile getProfile() {
        return profile_;
    }

    /**
     * Get default timeout in seconds for player actions.
     */
    public int getTimeoutSeconds() {
        return profile_ != null ? profile_.getTimeoutSeconds() : 0;
    }

    /**
     * Get timeout for a specific betting round.
     */
    public int getTimeoutForRound(int round) {
        return profile_ != null ? profile_.getTimeoutForRound(round) : 0;
    }

    /**
     * Get minimum players required for scheduled start.
     */
    public int getMinPlayersForScheduledStart() {
        return profile_ != null ? profile_.getMinPlayersForStart() : 0;
    }

    /**
     * Get scheduled start time in milliseconds since epoch.
     */
    public long getScheduledStartTime() {
        return profile_ != null ? profile_.getStartTime() : 0L;
    }

    /**
     * Check if scheduled start is enabled.
     */
    public boolean isScheduledStartEnabled() {
        return profile_ != null && profile_.isScheduledStartEnabled();
    }

    /**
     * Check if this is a practice game.
     */
    public boolean isPractice() {
        return !isOnlineGame();
    }

    @Override
    public boolean isOnlineGame() {
        return super.isOnlineGame();
    }

    /**
     * Check if a level is a break period.
     */
    public boolean isBreakLevel(int level) {
        return profile_ != null && profile_.isBreak(level);
    }

    /**
     * Set profile
     */
    public void setProfile(TournamentProfile profile) {
        TournamentProfile old = profile_;
        profile_ = profile;
        nLastPool_ = -1;
        firePropertyChange(PROP_PROFILE, old, profile_);
    }

    /**
     * Change the level
     */
    public void changeLevel(int n) {
        int nOld = nLevel_;
        nLevel_ += n;
        // Reset hands counter for new level (after level incremented but before clock
        // set)
        nHandsInLevel_ = 0;
        clock_.setSecondsRemaining(getSecondsInLevel(nLevel_));
        firePropertyChange(PROP_CURRENT_LEVEL, nOld, nLevel_);
    }

    /**
     * Go to previous level (poker night use only)
     */
    public void prevLevel() {
        changeLevel(-1);
    }

    /**
     * Go to next level
     */
    public void nextLevel() {
        // get new min chip before level is changed
        int nNewMinIdx = getNextMinChipIndex();
        if (nMinChipIdx_ != nNewMinIdx) {
            nLastMinChipIdx_ = nLevel_ == 0 ? nNewMinIdx : nMinChipIdx_;
            nMinChipIdx_ = nNewMinIdx;
        }

        changeLevel(+1);
    }

    /**
     * Increment the count of hands played in the current level. Used for
     * hands-based level advancement.
     */
    public void incrementHandsInLevel() {
        nHandsInLevel_++;
    }

    /**
     * Get the number of hands played in the current level
     *
     * @return hands played in current level
     */
    public int getHandsInLevel() {
        return nHandsInLevel_;
    }

    /**
     * Check if level should advance based on hands played (when in HANDS mode)
     *
     * @return true if level should advance
     */
    public boolean shouldAdvanceLevelByHands() {
        if (profile_ == null || profile_.getLevelAdvanceMode() != LevelAdvanceMode.HANDS) {
            return false;
        }
        return nHandsInLevel_ >= profile_.getHandsPerLevel();
    }

    /**
     * Get seconds in a level
     */
    public int getSecondsInLevel(int nLevel) {
        return profile_.getMinutes(nLevel) * 60;
    }

    /**
     * Get number of seconds a portion of a hand takes. We use the number of hands
     * per hour as the basis and assume 5 parts per hand: deal, flop, turn, river,
     * showdown.
     */
    private int getSecondsPerHandAction() {
        int nHandsPerHour = PokerUtils.getIntOption(PokerConstants.OPTION_HANDS_PER_HOUR);
        int nSecondsPerHand = 3600 / nHandsPerHour;
        return nSecondsPerHand / 5;
    }

    /**
     * indicate a clock action took place - subtract appropriate amount of seconds
     */
    public void advanceClock() {
        int nAction = getSecondsPerHandAction();
        int nNew = clock_.getSecondsRemaining() - nAction;
        if (nNew < 0)
            nNew = 0;
        clock_.setSecondsRemaining(nNew);
    }

    /**
     * advance clock during a break. Advances 120 seconds, which happens every 200
     * millis, so breaks go by quickly in practice mode
     */
    public void advanceClockBreak() {
        int nNew = clock_.getSecondsRemaining() - 120;
        if (nNew < 0)
            nNew = 0;
        clock_.setSecondsRemaining(nNew);
    }

    /**
     * Get clock used for game
     */
    public GameClock getGameClock() {
        return clock_;
    }

    /**
     * Start the game clock (GameContext interface method)
     */
    public void startGameClock() {
        if (clock_ != null) {
            clock_.start();
        }
    }

    /**
     * check whether blinds need to increase. return true if they do
     */
    public boolean isLevelExpired() {
        return clock_.isExpired();
    }

    /**
     * Get start date of game (millis).
     */
    public long getStartDate() {
        return Utils.getMillisFromTimeStamp(id_);
    }

    /**
     * Get id of game
     */
    public long getID() {
        return id_;
    }

    /**
     * Setup tournament given profile and num players (called from
     * TournamentOptions). For clock, nextLevel() is called to init the level and
     * for practice mode, setupTournament is called. For online games, nothing is
     * done - setupTournament needs to be called explicitly after all players have
     * joined the game.
     */
    public void initTournament(TournamentProfile profile) {
        // id used to uniquely identify a tournament in player profiles
        id_ = Utils.getCurrentTimeStamp();
        nLevel_ = 0;
        profile_ = profile;
        nLastPool_ = -1;

        if (isClockMode()) {
            nextLevel();
        } else if (isOnlineGame()) {
            // little initial work done from TournamentOptions, rather
            // done when required start conditions are met. We do
            // init profile so current players list has host.
            updatePlayerList();
        } else {
            setupTournament(false, true, profile_.getNumPlayers());
        }
    }

    /**
     * return if game is in progress
     */
    public boolean isInProgress() {
        // game is in registration mode (online) until HostStart calls setupTournament
        return nLevel_ > 0;
    }

    /**
     * Setup the tournament by creating computer players, creating players and
     * assigning to tables, setting initial chip count for each player and setting
     * the first level
     */
    public void setupTournament(boolean bOnline, boolean bFillComputers, int nMaxPlayers) {
        // if need to fill remaining seats with computers, do so
        if (bFillComputers) {
            setupComputerPlayers(nMaxPlayers);
        }

        // assign players to tables and give them their buyin
        assignTables(bOnline);
        initPlayers(profile_.getBuyinChips(), bOnline);

        computeTotalChipsInPlay();

        // init first level - calc's min chip (need to do after initChipCount)
        nextLevel();

        // set initial min chip now that its been set
        for (int i = 0; i < getNumTables(); i++) {
            tables_.get(i).setMinChip(getMinChip());
        }

        // init DDMessage MsgState
        initMsgState(true);
    }

    /**
     * setup the players in the game
     */
    private void setupComputerPlayers(int nNumPlayers) {
        ClientPlayer player;
        List<String> names = new ArrayList<String>(PokerMain.getPokerMain().getNames());
        int nNumHumans = getNumPlayers();

        // if we don't have enough names, add more (enough
        // to account for non-used names that conflict with
        // humans)
        if (names.size() < nNumPlayers) {
            int step = names.size() / (nNumPlayers - names.size());
            int index = DiceRoller.rollDieInt(step) - 1;
            while (names.size() < nNumPlayers) {
                names.add(names.get(index) + " D.");
                index += step;
            }
        }

        Set<String> hsUsed = new HashSet<String>();

        for (int i = 0; i < nNumHumans; i++) {
            hsUsed.add(getPokerPlayerAt(i).getName());
        }

        // fill remaining players with computer players
        PlayerType playerType;
        String sName;
        String sKey = getPlayerId();
        Map<String, List<String>> hmRoster = new HashMap<String, List<String>>();
        List<String> roster;
        for (int i = getNumPlayers(); i < nNumPlayers; i++) {
            playerType = getNextPlayerType(/* i - nNumHumans, nNumPlayers - nNumHumans */);
            roster = hmRoster.get(playerType.getFileName());
            if (roster == null) {
                roster = Roster.getRosterNameList(playerType);
                hmRoster.put(playerType.getFileName(), roster);
            }
            sName = getName(names, roster, hsUsed);
            player = new ClientPlayer(sKey, getNextPlayerID(), sName, false);
            player.setPlayerType(playerType);
            addPlayer(player);
        }
    }

    private PlayerType getNextPlayerType() {
        PlayerType result = null;

        SecureRandom random = SecurityUtils.getSecureRandom();
        int percentile = random.nextInt(100);
        // SecureRandom replaced: (Math.random() * 100);
        // FIX: verify this works as expected. The index/total params
        // not used, but this code was commented out:
        // (index * 100) / total;

        List<BaseProfile> playerTypes = PlayerType.getProfileListCached();

        for (BaseProfile playerType : playerTypes) {
            PlayerType type = (PlayerType) playerType;

            int percent = profile_.getPlayerTypePercent(type.getUniqueKey());

            if (percent > percentile) {
                result = type;
                break;
            } else {
                percentile -= percent;
            }
        }

        if (result == null)
            result = PlayerType.getDefaultProfile();

        return result;
    }

    /**
     * get random name not equal to any human
     */
    private String getName(List<String> names, List<String> roster, Set<String> hsUsed) {
        String sName;

        while (true) {
            if (!roster.isEmpty()) {
                sName = roster.remove(DiceRoller.rollDieInt(roster.size()) - 1);
            } else {
                sName = names.remove(DiceRoller.rollDieInt(names.size()) - 1);
            }

            if (!hsUsed.contains(sName)) {
                hsUsed.add(sName);

                return sName;
            }
        }
    }

    /**
     * Get # seats at table (call-through to TournamentProfile)
     */
    public int getSeats() {
        return profile_.getSeats();
    }

    /**
     * Assign all players to tables
     */
    private void assignTables(boolean bOnline) {
        tables_.clear(); // clear any stale tables from a previous setup attempt
        int nNumPlayers = getNumPlayers();
        List<ClientPlayer> players = getPokerPlayersCopy(); // clone since we will remove players as we go

        // evenly distribute players across tables
        int nNumTables = nNumPlayers / getSeats();
        if (nNumPlayers % getSeats() > 0)
            nNumTables++;
        int nMinPerTable = nNumPlayers / nNumTables;
        int nExtra = nNumPlayers % nNumTables;
        int nMax;
        for (int i = 0; i < nNumTables; i++) {
            nMax = nMinPerTable;
            if (nExtra > 0) {
                nMax++;
                nExtra--;
            }
            fillSeats(players, nMax, bOnline);
        }

        // if host is an observer, assign to table 1
        ClientPlayer host = getHost();
        if (host.isObserver()) {
            tables_.get(0).addObserver(host);
        }

        // assign observers tables
        ClientPokerTable hostTable = host.getTable();
        nNumPlayers = getNumObservers();
        ClientPlayer obs;
        for (int i = 0; i < nNumPlayers; i++) {
            obs = getPokerObserverAt(i);
            if (obs.isHost())
                continue;
            hostTable.addObserver(obs);
        }
    }

    /**
     * Fill seats in a table from list of players up to max
     */
    private void fillSeats(List<ClientPlayer> players, int nMax, boolean bOnline) {
        int idx;
        RemotePokerTable table = new RemotePokerTable(this, tables_.size() + 1);
        table.setMinChip(getMinChip());

        // num seats to fill
        int nOpen = table.getNumOpenSeats();
        boolean bDemo = false;

        // randomly assign player
        ClientPlayer player;
        for (int i = 0; i < nOpen && i < nMax; i++) {
            // if demo, set seed so order is same
            // do in loop since adding player to table
            // triggers AI creation, which could change seed
            if (bDemo) {
                DiceRoller.setSeed(49469233 + i);
            }

            idx = DiceRoller.rollDieInt(players.size()) - 1;

            // testing with two players and more than 10 players - keep at diff tables
            if (TESTING(PokerConstants.TESTING_SPLIT_HUMANS) && ((getNumPlayers() > 10 && getNumHumans() == 2)
                    || (getNumPlayers() > 20 && getNumHumans() == 3))) {
                if (getNumTables() == 0 && table.getNumOccupiedSeats() == 0) {
                    logger.debug("TESTING: placing host at table 1");
                    idx = 0;
                } else {
                    boolean bDone = false;
                    while (!bDone) {
                        ClientPlayer peek = players.get(idx);
                        if (peek.isHuman() && table.getNumOccupiedSeats() > 0 && !table.isAllComputer()) {
                            idx = DiceRoller.rollDieInt(players.size()) - 1;
                            logger.debug("TESTING: skip placing " + peek.getName() + " on table " + table.getName()
                                    + " new index to check: " + idx);
                        } else
                            bDone = true;
                    }
                }
            }
            player = players.remove(idx);
            table.addPlayer(player);

            // if practice mode and we place human, mark this table
            // as current (online setCurrent handled in HostStart
            // and in load-game logic)
            if (!bOnline && player.isHuman()) {
                setCurrentTable(table);
            }
        }

        addTable(table);
    }

    private int nLastPool_ = 0;

    /**
     * handle a player getting out of tournament
     */
    public void playerOut(ClientPlayer player) {
        if (player == null) {
            return;
        }

        ClientPlayer canonical = getPokerPlayerFromID(player.getID());
        ClientPlayer finisher = canonical != null ? canonical : player;
        if (finisher.isEliminated()) {
            return;
        }

        // figure finish spot
        int nTotalOut = getNumPlayersOut();
        int nFinish = getNumPlayers() - nTotalOut;

        // update pool total
        int nPool = getPrizePool();
        if (nPool != nLastPool_) {
            nLastPool_ = nPool;
            profile_.setPrizePool(nPool, true);
        }

        // get amount paid
        int nPrize;

        // if 1st place, get rest of pool not paid. This
        // accounts for underpayments to people out before
        // rebuys/addons over yet still received prize money (rare case)
        if (nFinish == 1) {
            // get prizepool as profile defines it (to account for house cut)
            nPrize = profile_.getPrizePool() - getPrizesPaid();
        }
        // else get from profile
        else {
            nPrize = profile_.getPayout(nFinish);
        }

        // set player's prize, place and note eliminated
        finisher.setEliminated(true);
        finisher.setPlace(nFinish);
        finisher.setPrize(nPrize);
        nNumOut_++;

        // update player list (online games)
        updatePlayerList(finisher);

        // event
        firePropertyChange(PROP_PLAYER_FINISHED, null, finisher);
    }

    /**
     * Records the server-reported result for a player in WebSocket mode.
     *
     * <p>
     * {@code applyTableData()} creates new {@link ClientPlayer} objects in
     * {@link com.donohoedigital.games.poker.online.RemotePokerTable} seats, which
     * are separate from the original objects in {@code players_}. This method
     * applies the server-provided finish position to the {@code players_} object so
     * that {@link #getHumanPlayer()}, {@link #getPlayersByRank()}, and
     * {@link #getNumPlayersOut()} return correct values for the {@code GameOver}
     * dialog and {@link ChipLeaderPanel}.
     *
     * @param playerId
     *            server-assigned player ID
     * @param finishPosition
     *            1 = winner, 2 = runner-up, etc.
     */
    public void applyPlayerResult(int playerId, int finishPosition) {
        ClientPlayer player = getPokerPlayerFromID(playerId);
        if (player == null || finishPosition <= 0)
            return;

        boolean alreadyEliminated = player.isEliminated();
        int previousPlace = player.getPlace();
        if (alreadyEliminated && previousPlace == finishPosition) {
            return;
        }

        int prize = 0;
        if (profile_ != null) {
            int pool = getPrizePool();
            if (pool != nLastPool_ || profile_.getPrizePool() != pool) {
                nLastPool_ = pool;
                profile_.setPrizePool(pool, true);
            }

            int prizesPaidExcludingPlayer = getPrizesPaid() - player.getPrize();
            if (finishPosition == 1) {
                prize = Math.max(0, profile_.getPrizePool() - prizesPaidExcludingPlayer);
            } else {
                prize = profile_.getPayout(finishPosition);
            }
        }

        player.setEliminated(true);
        player.setPlace(finishPosition);
        player.setPrize(prize);
        // Zero out chips for eliminated players so getPlayersByRank() sorts them
        // after the winner (who retains a non-zero initial chip count).
        if (finishPosition > 1) {
            player.setChipCount(0);
        }
        if (!alreadyEliminated) {
            nNumOut_++;
        }
        firePropertyChange(PROP_PLAYER_FINISHED, null, player);
    }

    /**
     * Get number of players out
     */
    public int getNumPlayersOut() {
        return nNumOut_;
    }

    /**
     * Get players in wait list, sorted by time added to list (longest wait on list
     * at top)
     */
    public List<ClientPlayer> getWaitList() {
        List<ClientPlayer> wait = null;
        int nNum = getNumPlayers();
        ClientPlayer p;
        for (int i = 0; i < nNum; i++) {
            p = getPokerPlayerAt(i);
            if (p.isWaiting()) {
                if (wait == null)
                    wait = new ArrayList<ClientPlayer>();
                wait.add(p);
            }
        }

        if (wait != null && wait.size() > 1) {
            Collections.sort(wait, SORTBYWAIT);
        }

        return wait;
    }

    // instances for sorting
    private static SortByWait SORTBYWAIT = new SortByWait();

    // sort players by when they were added to wait list
    private static class SortByWait implements Comparator<ClientPlayer> {
        /**
         * Compares its two arguments for order. Returns a negative integer, zero, or a
         * positive integer as the first argument is less than, equal to, or greater
         * than the second.
         */
        public int compare(ClientPlayer p1, ClientPlayer p2) {
            return (int) (p1.getWaitListTimeStamp() - p2.getWaitListTimeStamp());
        }
    }

    /**
     * Get prize pool based on actual buy-ins and rebuys
     */
    public int getPrizePool() {
        int n = 0;
        ClientPlayer p;
        for (int i = getNumPlayers() - 1; i >= 0; i--) {
            p = getPokerPlayerAt(i);
            n += p.getTotalSpent();
        }
        return n;
    }

    /**
     * Get total paid out so far
     */
    public int getPrizesPaid() {
        int n = 0;
        ClientPlayer p;
        for (int i = getNumPlayers() - 1; i >= 0; i--) {
            p = getPokerPlayerAt(i);
            n += p.getPrize();
        }
        return n;
    }

    /**
     * Verify chips count equals sum of all rebuys,addons,buyins
     */
    public void verifyChipCount() {
        int nChips = 0;
        int nBought = 0;
        ClientPlayer p;
        for (int i = getNumPlayers() - 1; i >= 0; i--) {
            p = getPokerPlayerAt(i);
            nChips += p.getChipCount();
            nBought += profile_.getBuyinChips() + (p.getAddon() == 0 ? 0 : profile_.getAddonChips())
                    + p.getNumRebuys() * profile_.getRebuyChips();
        }

        // for online, add in chips in pots at all tables, since
        // hands aren't in sync and money could be in pots when we
        // do this calculation
        if (isOnlineGame()) {
            ClientPokerTable table;
            ClientHoldemHand hhand;
            for (int i = getNumTables() - 1; i >= 0; i--) {
                table = tables_.get(i);
                hhand = table.getHoldemHand();
                if (hhand == null)
                    continue;

                // if in showdown, pot has already been allocated to players
                // and this would result in extra chips
                if (hhand.getRound() == BettingRound.SHOWDOWN)
                    continue;

                nChips += hhand.getTotalPotChipCount();
            }
        }

        if (nChips != (nBought + nExtraChips_)) {
            logger.error("Chip count off.  Bought=" + nBought + "   chips=" + nChips + "   nExtra=" + nExtraChips_);
        }
    }

    /**
     * Debug print tables
     */
    public void debugPrintTables(boolean bShort) {
        for (int i = 0; i < getNumTables(); i++) {
            logger.debug(tables_.get(i).getName());
        }
    }

    ////
    //// 2.0 online stuff
    ////

    // transient (recreated upon load)
    private PokerGameState state_;

    private void initMsgState(boolean bInitIds) {
        if (isOnlineGame() && state_ == null) {
            // create a poker game state for this game so marshalling of
            // DDMessages containing objects like HandAction work properly
            // Only needed in online play.
            state_ = new PokerGameState(this, bInitIds);
            DDMessage.setMsgState(state_); // FIX: ick! Figure out a way to do this non-staticly (prohibits multi-games)
        }
    }

    /**
     * finish - cleanup
     */
    @Override
    public void finish() {
        clock_.stop();
        if (state_ != null) {
            state_.finish();
            state_ = null;
            DDMessage.setMsgState(null); // FIX: ick!!!!
        }

        setOnlineMode(MODE_NONE);

        super.finish();
    }

    /**
     * Get online mode
     */
    public int getOnlineMode() {
        return nOnlineMode_;
    }

    /**
     * Set online mode
     */
    public void setOnlineMode(int n) {
        nOnlineMode_ = n;
    }

    /**
     * Is this a listed public game?
     */
    public boolean isPublic() {
        return bPublic_;
    }

    /**
     * Is this a listed public game?
     */
    public void setPublic(boolean bPublic) {
        bPublic_ = bPublic;
    }

    /**
     * Is online ready?
     */
    public boolean isAcceptingRegistrations() {
        // we must have a host
        ClientPlayer p = getPokerPlayerFromID(PokerConstants.PLAYER_ID_HOST);
        if (p == null)
            return false;

        // and the host's key must equal our game
        String sKey = getPlayerId();
        if (!sKey.equals(p.getPlayerId()))
            return false;

        return nOnlineMode_ == MODE_REG || nOnlineMode_ == MODE_PLAY;
    }

    /**
     * Get local ip
     */
    public String getLocalIP() {
        return sLocalIP_;
    }

    /**
     * Set local ip
     */
    public void setLocalIP(String s) {
        sLocalIP_ = s;
    }

    /**
     * Get public ip
     */
    public String getPublicIP() {
        return sPublicIP_;
    }

    /**
     * Set public ip
     */
    public void setPublicIP(String s) {
        sPublicIP_ = s;
    }

    /**
     * Get port (used both local/public)
     */
    public int getPort() {
        return nPort_;
    }

    /**
     * Set prot
     */
    public void setPort(int n) {
        nPort_ = n;
    }

    /**
     * Get lan connect string
     */
    public String getLanConnectURL() {
        return getConnectURL(getLocalIP());
    }

    /**
     * Get public connect string
     */
    public String getPublicConnectURL() {
        String sPub = getPublicIP();
        if (sPub == null)
            return null;
        return getConnectURL(sPub);
    }

    /**
     * Get regular expression for connect URL for this game. Essentially, it is a
     * regexp which valiates a proper IP address
     */
    public String getConnectRegExp() {
        return '^' + getConnectURL(PokerConstants.REGEXP_IP_ADDRESS) + '$';
    }

    /**
     * Get connect string
     */
    private String getConnectURL(String IP) {
        // SAMPLE: poker://192.111.2.101:11885/n-1/QPF-841
        StringBuilder sb = new StringBuilder();
        sb.append(PokerConstants.URL_START);
        sb.append(IP);
        sb.append(":");
        sb.append(getPort());
        sb.append("/");
        sb.append(getOnlineGameID());
        sb.append(PokerConstants.ID_PASS_DELIM);
        sb.append(getOnlinePassword());
        return sb.toString();
    }

    ////
    //// misc overrides
    ////

    /**
     * Override - not used
     */
    @Override
    public void setOnlinePlayerIDs(DMArrayList<Integer> ids) {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setOnlinePlayerIDs() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public DMArrayList<Integer> getOnlinePlayerIDs() {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getOnlinePlayerIDs() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public boolean isOnlinePlayer(GamePlayer player) {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "isOnlinePlayer() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @SuppressWarnings({"RawUseOfParameterizedType"})
    @Override
    public DMArrayList getResendList() {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getResendList() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @SuppressWarnings({"RawUseOfParameterizedType"})
    @Override
    public DMArrayList getTimestampList() {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getTimestampList() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void addCompletedPhase(String sPhase) {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "addCompletedPhase() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public boolean isCompletedPhase(String sPhase) {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "isCompletedPhase() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void clearCompletedPhases() {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "clearCompletedPhases() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void setTurn(int num) {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setTurn() not used in PokerGame", null);
    }

    /**
     * Get turn # for game
     */
    @Override
    public int getTurn() {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "getTurn() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void setCurrentPlayer(int i) {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setCurrentPlayer() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void setCurrentPlayerByID(int id) {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setCurrentPlayerByID() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public void setCurrentPlayer(GamePlayer player) {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED, "setCurrentPlayer() not used in PokerGame", null);
    }

    /**
     * Override - not used
     */
    @Override
    public int getCurrentPlayerIndex() {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED,
                "getCurrentPlayerIndex() not used in PokerGame - use HoldemHand's instead", null);
    }

    /**
     * Get the current player
     */
    @Override
    public GamePlayer getCurrentPlayer() {
        throw new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED,
                "getCurrentPlayer() not used in PokerGame - used HoldemHand's instead", null);
    }

    ////
    //// save/load logic
    ////

    /**
     * Get save details with given init value
     */
    @Override
    public SaveDetails getSaveDetails(int nInit) {
        SaveDetails details = new SaveDetails(nInit);
        PokerSaveDetails pdetails = new PokerSaveDetails(nInit);
        details.setCustomInfo(pdetails);
        details.setSaveTerritories(SaveDetails.SAVE_NONE); // we never save territories
        details.setSaveGameHashData(SaveDetails.SAVE_ALL); // we always save game hash data
        return details;
    }

    /**
     * Override to update description
     */
    @Override
    public void saveGame(GameState state) {
        state.setDescription(getDescription());
        super.saveGame(state);
    }

    /**
     * save poker specific data
     */
    @Override
    protected void saveSubclassData(GameState state) {
        PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();

        // create entry with game info (including num of entries)
        GameStateEntry entry = new GameStateEntry(state, this, ConfigConstants.SAVE_NUM_GAMEDATA);

        // info
        entry.addToken(nLevel_);
        entry.addToken(bClockMode_);
        entry.addToken(clock_.marshal(state));
        entry.addToken(id_);
        entry.addToken(nMinChipIdx_);
        entry.addToken(nLastMinChipIdx_);
        entry.addToken(nExtraChips_);

        // profiles
        if (pdetails.getSaveProfileData() != SaveDetails.SAVE_NONE) {
            try {
                // tournament profile
                StringWriter writer = new StringWriter();
                profile_.write(writer);
                entry.addToken(writer.toString());
                writer.getBuffer().setLength(0);
            } catch (IOException ioe) {
                throw new ApplicationError(ioe);
            }
        }

        // Save/load code removed — PokerTable engine class no longer exists.
        // This will be fully cleaned up in Task 18 (Remove PokerSaveGame).
        throw new UnsupportedOperationException("Save/load not supported after engine class removal");
    }

    /**
     * load poker specific data — disabled after engine class removal. Will be fully
     * cleaned up in Task 18 (Remove PokerSaveGame).
     */
    @Override
    protected void loadSubclassData(GameState state) {
        throw new UnsupportedOperationException("Save/load not supported after engine class removal");
    }

    /**
     * allow subclass to do final setup after load — disabled after engine class
     * removal. Will be fully cleaned up in Task 18 (Remove PokerSaveGame).
     */
    @Override
    protected void gameLoaded(GameState state) {
        throw new UnsupportedOperationException("Save/load not supported after engine class removal");
    }

    /**
     * fix keys to match current game
     */
    private void fixKeys(GameState state) {
        // only fix when loading from a file
        if (state.getFile() == null || isClockMode()) {
            return;
        }

        ClientPlayer host = getPokerPlayerFromID(PokerConstants.PLAYER_ID_HOST);
        String sHostKey = host.getPlayerId();
        String sCurrentKey = getPlayerId();
        if (!sHostKey.equals(sCurrentKey)) {
            ClientPlayer player;
            for (int i = getNumPlayers() - 1; i >= 0; i--) {
                player = getPokerPlayerAt(i);
                if (player.getPlayerId().equals(sHostKey)) {
                    player.setPlayerId(sCurrentKey);

                    // info message
                    if (player.isHuman()) {
                        logger.info("Key in save file updated to current key for: " + player.getName());
                    }
                }
            }
        }
    }

    /**
     * fix disconnected flag - when reloading an online game, human players are no
     * longer connected. Also remove observers, who are no longer connected and
     * don't need to be loaded
     */
    private void fixDisconnected(GameState state) {
        // only fix when loading from a file
        if (state.getFile() == null || !isOnlineGame()) {
            return;
        }

        // set disconnected state for players
        ClientPlayer player;
        for (int i = getNumPlayers() - 1; i >= 0; i--) {
            player = getPokerPlayerAt(i);
            player.setDisconnected(!(player.isHost() || player.isComputer()));
            // I'm leaving this off - if a host exits and restarts right
            // away, there might be players waiting to rejoing right
            // away and setting them to sitting out seems wrong
            // if (player.isDisconnected()) player.setSittingOut(true);
        }

        // remove observers
        for (int i = getNumObservers() - 1; i >= 0; i--) {
            player = getPokerObserverAt(i);
            if (!player.isHost()) {
                removeObserver(player);
            }
        }
    }
    ////
    //// ui bridge methods
    ////

    public void setInput(PokerTableInput input) {
        input_ = input;
    }

    public void setInputMode(int nMode) {
        setInputMode(nMode, null, null);
    }

    public void setInputMode(int nMode, ClientHoldemHand hhand, ClientPlayer player) {
        if (input_ != null) {
            input_.setInputMode(nMode, hhand, player);
        }
    }

    public int getInputMode() {
        if (input_ != null) {
            return input_.getInputMode();
        } else {
            return PokerTableInput.MODE_NONE;
        }
    }

    private PlayerActionListener playerActionListener_ = null;

    public PlayerActionListener getPlayerActionListener() {
        return playerActionListener_;
    }

    public void setPlayerActionListener(PlayerActionListener listener) {
        ApplicationError.assertTrue(playerActionListener_ == null || listener == null,
                "Attempt to replace existing listener.");
        playerActionListener_ = listener;
    }

    public void playerActionPerformed(int action, int nAmount) {
        if (playerActionListener_ != null) {
            playerActionListener_.playerActionPerformed(action, nAmount);
        }
    }

    /**
     * total chips in play
     */
    public void computeTotalChipsInPlay() {
        int chips = nExtraChips_;

        ClientPlayer p;
        for (int i = getNumPlayers() - 1; i >= 0; i--) {
            p = getPokerPlayerAt(i);
            chips += profile_.getBuyinChips() + (p.getAddon() == 0 ? 0 : profile_.getAddonChips())
                    + p.getNumRebuys() * profile_.getRebuyChips();
        }

        totalChipsInPlay_ = chips;
    }

    /**
     * add chips
     */
    public void chipsBought(int nChips) {
        totalChipsInPlay_ += nChips;
    }

    /**
     * return total chips in play
     */
    public int getTotalChipsInPlay() {
        return totalChipsInPlay_;
    }

    /**
     * Get average stack size.
     */
    public int getAverageStack() {
        return getTotalChipsInPlay() / (getNumPlayers() - getNumPlayersOut());
    }

    /**
     * set id of last hand saved
     */
    public void setLastHandSaved(int handID) {
        lastHandSaved_ = handID;
    }

    /**
     * Get id of last hand saved
     */
    public int getLastHandSaved() {
        return lastHandSaved_;
    }

    /**
     * if set, hands are deleted upon first save
     */
    public void setDeleteHandsAfterSaveDate(boolean b) {
        deleteHandsAfterSaveDate_ = b;
    }

    /**
     * should "future" hand history be removed
     */
    public boolean isDeleteHandsAfterSaveDate() {
        return deleteHandsAfterSaveDate_;
    }
}
