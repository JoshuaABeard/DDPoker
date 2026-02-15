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
 * TournamentDirector.java
 *
 * Created on January 21, 2005, 10:24 AM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import static com.donohoedigital.config.DebugConfig.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.TableProcessResult;
import com.donohoedigital.games.poker.core.event.*;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.network.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.util.*;

/**
 * Class which handles tournament.
 *
 * @author donohoe
 */
@SuppressWarnings({"PublicField"})
public class TournamentDirector extends BasePhase implements Runnable, GameManager, ChatManager {
    static Logger logger = LogManager.getLogger(TournamentDirector.class);

    // debugging stmts
    public static final boolean DEBUG = false; // general debug in this class
    public static boolean DEBUG_SLEEP = false; // debug when sleeping
    public static boolean DEBUG_SAVE = false; // save
    public static boolean DEBUG_EVENT = false; // event handling in this class
    public static boolean DEBUG_EVENT_DISPLAY = false; // event display in ShowTournamentTable, PokerGameboard
    public static boolean DEBUG_CLEANUP_TABLE = DEBUG; // display stmts w.r.t. table cleanup and consolidation
    public static boolean DEBUG_REJOIN = false; // display rejoin info

    // name of phase associated with this in gamedef.xml
    public static final String PHASE_NAME = "TournamentDirector";

    // wait time before clearing wait list for non-betting wait
    public static final int NON_BETTING_TIMEOUT_MILLIS = 10 * 1000;

    // wait time before clearing wait list for new level check timeout
    // this allows extra time for answering rebuy/addon messages
    public static final int NEWLEVEL_TIMEOUT_MILLIS = 25 * 1000;

    // time to wait before clearing a pending rejoin
    public static final int REJOIN_TIMEOUT_MILLIS = 20 * 1000;

    // TD default sleep between checking for things to do
    private static final int SLEEP_MILLIS = 250;

    // AI pause in tenths for online games
    public static final int AI_PAUSE_TENTHS = 10; // one second

    // member data
    private Thread thread_;
    private Thread threadAlive_;
    private static int nThreadSeq_ = 0;
    private boolean bDone_ = false;
    private PokerGame game_;
    private OnlineManager mgr_;
    private ChatHandler chat_;
    private boolean bOnline_;

    // two variables to make code easier to read in places
    private boolean bClient_; // online client
    private boolean bHost_; // online host (also true if practice mode)

    // Phase 2: pokergamecore engine
    private TournamentEngine engine_; // pokergamecore engine

    /**
     * Phase start
     */
    @Override
    public void start() {
        game_ = (PokerGame) context_.getGame();
        context_.setGameManager(this);
        bOnline_ = game_.isOnlineGame();
        bClient_ = gamephase_.getBoolean("client", false);
        bHost_ = !bClient_;

        if (bOnline_) {
            mgr_ = game_.getOnlineManager();
            if (mgr_ == null) {
                // if no online manager, create it. This is null
                // when loading game from save file. We create here
                // so that mgr is init'd with TD so it can properly
                // process any incoming rejoin messages
                mgr_ = game_.initOnlineManager(this);
            } else {
                mgr_.setTournamentDirector(this);
            }
        }

        if (bHost_) {
            // starting up - new and loaded games, need to
            // set the last change time to now to account for
            // lobby/startup time
            PokerTable table;
            int nNum = game_.getNumTables();
            for (int i = 0; i < nNum; i++) {
                table = game_.getTable(i);
                table.touchLastStateChangeTime();
            }

            // new game
            if (bOnline_ && game_.isStartFromLobby()) {
                // add all remote players to wait list so we don't start until they are ready
                for (int i = 0; i < nNum; i++) {
                    table = game_.getTable(i);
                    if (table.isAllComputer())
                        continue;
                    table.addWaitAllHumans();
                }

                // host is ready
                PokerPlayer host = game_.getLocalPlayer();
                host.getTable().removeWait(host);

                // have each player display the tournament table
                PokerPlayer player;
                nNum = game_.getNumPlayers();
                for (int i = 0; i < nNum; i++) {
                    player = game_.getPokerPlayerAt(i);
                    if (player.isComputer() || player.isHost())
                        continue;
                    mgr_.onlineProcessPhase(HostStart.PHASE_CLIENT_INIT, null, false, player);
                }

                // and each observer (loop from bottom in case of error, which removes observer)
                nNum = game_.getNumObservers();
                for (int i = nNum - 1; i >= 0; i--) {
                    player = game_.getPokerObserverAt(i);
                    if (player.isHost())
                        continue;
                    mgr_.onlineProcessPhase(HostStart.PHASE_CLIENT_INIT, null, false, player);
                }
            }

            // reset timeout clock - players
            if (bOnline_) {
                PokerPlayer player;
                nNum = game_.getNumPlayers();
                for (int i = 0; i < nNum; i++) {
                    player = game_.getPokerPlayerAt(i);
                    if (player.isComputer() || player.isHost())
                        continue;
                    player.clearMessageReceived();
                }

                // reset timeout clock - observers
                nNum = game_.getNumObservers();
                for (int i = nNum - 1; i >= 0; i--) {
                    player = game_.getPokerObserverAt(i);
                    if (player.isHost())
                        continue;
                    player.clearMessageReceived();
                }
            }

            startWanGame();
            setStartPause();

            // Phase 2: Initialize pokergamecore engine
            GameEventBus eventBus = new SwingEventBus(game_);
            PlayerActionProvider actionProvider = new SwingPlayerActionProvider(this);
            engine_ = new TournamentEngine(eventBus, actionProvider);

            // noinspection AssignmentToStaticFieldFromInstanceMethod
            thread_ = new Thread(this, "TournamentDirector-" + (nThreadSeq_++));
            thread_.start();
        }
        // client
        else if (bOnline_) {
            // reset timeout clock - host
            game_.getHost().clearMessageReceived();
        }

        // alive thread for online games
        if (bOnline_) {
            threadAlive_ = new TDAlive();
            threadAlive_.start();
        }
    }

    /**
     * on startup, if table is in state waiting to deal for button, force a pause in
     * online games so there is a delay between when table is displayed and when
     * deal happens
     */
    private void setStartPause() {
        if (bOnline_) {
            int tableState = game_.getHost().getTable().getTableStateInt();
            int pendingState = game_.getHost().getTable().getPendingTableStateInt();

            boolean bHostTableDealForButton = tableState == PokerTable.STATE_DEAL_FOR_BUTTON
                    || pendingState == PokerTable.STATE_DEAL_FOR_BUTTON;

            if (bHostTableDealForButton) {
                boolean bPauseAtStart = PokerUtils.isOptionOn(PokerConstants.OPTION_ONLINE_PAUSE);

                if (bPauseAtStart) {
                    TypedHashMap params = new TypedHashMap();
                    params.setString(HostPauseDialog.PARAM_MSG_KEY, "msg.host.paused.start");
                    context_.processPhaseNow("HostPauseDialog", params);
                }
            }

            // upon start see if we are disconnected
            mgr_.checkAllConnected();
        }
    }

    /**
     * Is client?
     */
    public boolean isClient() {
        return bClient_;
    }

    /**
     * alive thread
     */
    private class TDAlive extends Thread {
        TDAlive() {
            super("TDAlive");
        }

        @Override
        public void run() {
            while (!bDone_) {
                try {
                    Utils.sleepMillis(OnlineManager.ALIVE_SLEEP_MILLIS);

                    if (!bDone_) {
                        mgr_.alive(true);
                    }
                } catch (Throwable t) {
                    logger.error("TDAlive caught an unexcepted exception: " + Utils.formatExceptionText(t));
                }
            }
        }
    }

    /**
     * thread start
     */
    public void run() {
        boolean bSleep = false;
        // wait while clients display;
        int nSleep;
        while (!bDone_) {
            try {
                bSleep = process();
            } catch (Throwable t) {
                logger.error("TournamentDirector caught an unexcepted exception: " + Utils.formatExceptionText(t));
                bDone_ = true;

                // log current hand
                PokerContext.LogGameInfo(game_);

                // if online, try to save game
                if (bOnline_) {
                    try {
                        saveGame("error");
                    } catch (Throwable darnCantSave) {
                        logger.error("Attempted to save but caught an exception: "
                                + Utils.formatExceptionText(darnCantSave));
                    }
                }

                // show error dialog to user and restart
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        EngineUtils.displayInformationDialog(context_,
                                Utils.fixHtmlTextFor15(PropertyConfig.getMessage("msg.tderror")));
                        context_.restart();
                    }
                });
            }

            // if not done, sleep
            if (!bDone_) {
                nSleep = SLEEP_MILLIS;
                if (!bSleep)
                    nSleep = 5; // if not sleeping, sleep very small amount to avoid reving up CPU
                if (DEBUG_SLEEP)
                    logger.debug("Sleeping " + nSleep);
                Utils.sleepMillis(nSleep);
            }
        }
    }

    /**
     * Cleanup
     */
    public void cleanup() {
        // stop thread
        bDone_ = true;

        // wait for alive to finish
        try {
            if (threadAlive_ != null) {
                threadAlive_.interrupt();
                threadAlive_.join();
            }
        } catch (InterruptedException ie) {
            Thread.interrupted();
        }

        // wait for TD to finish
        try {
            if (thread_ != null) {
                thread_.join();
            }
        } catch (InterruptedException ie) {
            Thread.interrupted();
        }

        // nullify
        thread_ = null;
        threadAlive_ = null;

        // clear us from OnlineManager
        if (mgr_ != null)
            mgr_.setTournamentDirector(null);

        // have engine restart to menu (and clear GameManager)
        context_.setGameManager(null);
        context_.restartNormal();
    }

    /**
     * Phase name
     */
    public String getPhaseName() {
        return PHASE_NAME;
    }

    /**
     * Return so other's can sync on saves. We sync on this class so that we don't
     * save during a processTable()
     */
    public Object getSaveLockObject() {
        return this;
    }

    /**
     * Save the game (sync to prevent save twice @ same time)
     */
    void saveGame(String sDesc) {
        synchronized (getSaveLockObject()) {
            if (DEBUG_SAVE)
                logger.debug("SAVING GAME: [" + sDesc + "]");
            game_.saveWriteGame();
        }
    }

    /**
     * return value from process
     */
    private class TDreturn implements PokerTableListener {
        private boolean bSave;
        private boolean bAutoSave;
        private boolean bSleep;
        private int nState;
        private int nPendingState;
        private boolean bRunOnClient;
        private boolean bAddAllHumans;
        private String sPhase;
        private DMTypedHashMap params;
        private DMArrayList<PokerTableEvent> events;
        private PokerTable table; // table we are listening to events
        private boolean bOnlySendToWaitList_;

        public void init() {
            setPhaseToRun(null);
            setSave(false);
            setAutoSave(false);
            setSleep(true);
            setTableState(-1);
            setPendingTableState(-1);
            setRunOnClient(false);
            setAddAllHumans(true);
            setOnlySendToWaitList(false);
            events = null;
            table = null;
        }

        public void finish() {
            if (table != null)
                table.removePokerTableListener(this, PokerTableEvent.TYPES_ALL);
        }

        public void setPhaseToRun(String s) {
            setPhaseToRun(s, null);
        }

        public void setPhaseToRun(String s, DMTypedHashMap p) {
            sPhase = s;
            params = p;
        }

        public String getPhaseToRun() {
            return sPhase;
        }

        public DMTypedHashMap getPhaseToRunParams() {
            return params;
        }

        public void setSleep(boolean b) {
            bSleep = b;
        }

        public boolean isSleep() {
            return bSleep;
        }

        public void setSave(boolean b) {
            bSave = b;
        }

        public boolean isSave() {
            return bSave;
        }

        public void setAutoSave(boolean b) {
            bAutoSave = b;
        }

        public boolean isAutoSave() {
            return bAutoSave;
        }

        public void setTableState(int n) {
            nState = n;
        }

        public int getTableState() {
            return nState;
        }

        public void setPendingTableState(int n) {
            nPendingState = n;
        }

        public int getPendingTableState() {
            return nPendingState;
        }

        public void setRunOnClient(boolean b) {
            bRunOnClient = b;
        }

        public boolean isRunOnClient() {
            return bRunOnClient;
        }

        public void setOnlySendToWaitList(boolean b) {
            bOnlySendToWaitList_ = b;
        }

        public boolean isOnlySendToWaitList() {
            return bOnlySendToWaitList_;
        }

        public void setAddAllHumans(boolean b) {
            bAddAllHumans = b;
        }

        public boolean isAddAllHumans() {
            return bAddAllHumans;
        }

        public DMArrayList<PokerTableEvent> getEvents() {
            return events;
        }

        public void startListening(PokerTable t) {
            table = t;
            table.addPokerTableListener(this, PokerTableEvent.TYPES_ALL);
        }

        public void tableEventOccurred(PokerTableEvent event) {
            if (events == null)
                events = new DMArrayList<PokerTableEvent>();
            if (DEBUG_EVENT)
                logger.debug("TDReturn event: " + event);
            events.add(event);
        }
    }

    // only need one instance since process() is synchronized
    private TDreturn ret_ = new TDreturn();

    // used to pause
    private boolean bPaused_ = false;
    private int nPauseCnt_ = 0;
    private long nPauseStart_;

    /**
     * pause the td (no need to synchronize since all we do is set a flag, and the
     * timing of when the TD checks it doesn't matter)
     */
    public void setPaused(boolean b) {
        if (b) {
            if (nPauseCnt_ == 0)
                nPauseStart_ = System.currentTimeMillis();
            nPauseCnt_++;
        } else {
            nPauseCnt_--;

            // if no longer paused, adjust time stamp on each table to
            // account for time asleep
            if (nPauseCnt_ == 0) {
                long asleep = System.currentTimeMillis() - nPauseStart_;
                for (int i = 0; i < game_.getNumTables(); i++) {
                    game_.getTable(i).adjustForPause(asleep);
                }
            }
        }

        bPaused_ = nPauseCnt_ > 0;
        // logger.debug((bPaused_? "TD paused":"TD unpaused")+" cnt: "+ nPauseCnt_);
    }

    /**
     * tournament director core logic. Return true if action taken.
     */
    private synchronized boolean process() {
        // if we are paused, just return so we can take another nap
        if (bPaused_)
            return true;

        // process each table
        boolean bSave = false;
        boolean bSleep = true;
        boolean bAutoSave = false;

        // query getNumTables() each time since tables
        // can get removed when players bust out
        for (int i = 0; i < game_.getNumTables(); i++) {
            // process (init's ret_ in this call)
            processTable(game_.getTable(i));

            // handle return data
            bSave |= ret_.isSave(); // save if anybody wants to save
            bAutoSave |= ret_.isAutoSave(); // autosave if anybody wants to autosave
            bSleep &= ret_.isSleep(); // sleep only if everyone wants to sleep
        }

        // save at end if directed to
        if (bSave && bOnline_)
            saveGame("process");

        // auto save if directed to (only used in practice mode)
        if (bAutoSave && !bOnline_)
            game_.autoSave();

        return bSleep;
    }

    /**
     * process table, return true if some action taken
     */
    public synchronized void processTable(PokerTable table) {
        // check here since we can enter process table from places other than the above
        // process()
        if (bPaused_)
            return;

        // init return data
        ret_.init();

        // skip if directed to pause
        if (table.getPause() > System.currentTimeMillis())
            return;

        // skip all computer tables (handled by current table)
        // in practice mode this means that _processTable is only
        // ever called for the human table, which code run in any !bOnline_
        // blocks in the methods below are only run once where otherwise
        // it wouldn't make sense to them multiple times
        if (table.isAllComputer() && !table.isCurrent())
            return;

        synchronized (table) {
            // Phase 2: Call pokergamecore engine instead of old _processTable()
            // Set auto-deal delay before calling engine (engine needs this value)
            table.setAutoDealDelay(getAutoDealDelay(table));

            // Call engine
            TableProcessResult engineResult = engine_.processTable(table, game_, bHost_, bOnline_);

            // Copy engine result to legacy TDreturn structure
            copyEngineResultToReturn(engineResult, ret_);

            // finish after processing (remove listeners)
            ret_.finish();

            // if we are to run on the client, we basically send the table in
            // its current state so that processTable can be run on the client
            // machine and whatever happens for that state happens on the client
            if (bHost_ && bOnline_ && ret_.isRunOnClient()) {
                mgr_.sendTableUpdate(table, ret_.getEvents(), ret_.isOnlySendToWaitList(), true);
            }

            // run any phases (after send to client in case phase updates game state)
            if (table.isCurrent() && ret_.getPhaseToRun() != null) {
                if (DEBUG)
                    logger.debug("Running " + ret_.getPhaseToRun());
                context_.processPhase(ret_.getPhaseToRun(), ret_.getPhaseToRunParams());
            }

            // set next state
            if (ret_.getPendingTableState() != -1) {
                // only host cares about wait list
                if (bHost_ && ret_.isAddAllHumans()) {
                    table.addWaitAllHumans();

                    // if host is eliminated or waiting, need to add
                    // them to wait list for proper display
                    if (table.isCurrent()) {
                        PokerPlayer host = game_.getHost();
                        // isObserver() also covers isWaiting() and isEliminated() with no seat (JDD,
                        // P2)
                        if (host.isObserver()) {
                            table.addWait(host);
                        }
                    }
                }
                table.setPendingTableState(ret_.getPendingTableState());
                // store phase/params for use on re-load from save
                table.setPendingPhase(ret_.getPhaseToRun());
                table.setPendingPhaseParams(ret_.getPhaseToRunParams());
                table.setTableState(PokerTable.STATE_PENDING);
            } else if (ret_.getTableState() != -1) {
                // special case - don't unset pending state if state specifically
                // set to pending (see STATE_PENDING_LOAD)
                if (ret_.getTableState() != PokerTable.STATE_PENDING) {
                    table.setPendingTableState(PokerTable.STATE_NONE);
                    table.setPendingPhase(null);
                    table.setPendingPhaseParams(null);
                }
                table.setTableState(ret_.getTableState());
            }
        }
    }

    /**
     * Copy TableProcessResult from pokergamecore engine to legacy TDreturn
     * structure.
     *
     * <p>
     * <b>Phase 2 Integration Bridge:</b> This method translates between the new
     * pokergamecore module's clean data structures and the legacy TDreturn inner
     * class. The mapping preserves all game logic decisions while maintaining
     * backward compatibility with existing UI/network code.
     *
     * <p>
     * <b>Field Mappings:</b>
     * <ul>
     * <li><b>State changes:</b> TableState enum → int constants via toLegacy()
     * <li><b>Phase to run:</b> String + Map&lt;String,Object&gt; → String +
     * DMTypedHashMap
     * <li><b>Flags:</b> Direct copy of all boolean flags (save, autoSave, sleep,
     * etc.)
     * </ul>
     *
     * <p>
     * <b>Event Flow:</b> Events are NOT copied from engineResult.events() to
     * TDreturn. Instead, the TournamentEngine publishes events to SwingEventBus,
     * which converts new GameEvent records to legacy PokerTableEvent objects and
     * dispatches them on Swing EDT. TDreturn receives these events via
     * startListening() registration.
     *
     * <p>
     * Event flow: TournamentEngine → SwingEventBus → PokerTableEvent → TDreturn
     * (via listener)
     *
     * @param engineResult
     *            the result from TournamentEngine.processTable()
     * @param ret
     *            the legacy return object to populate
     */
    private void copyEngineResultToReturn(TableProcessResult engineResult, TDreturn ret) {
        // Copy state changes
        if (engineResult.nextState() != null) {
            ret.setTableState(engineResult.nextState().toLegacy());
        }
        if (engineResult.pendingState() != null) {
            ret.setPendingTableState(engineResult.pendingState().toLegacy());
        }

        // Copy phase to run
        if (engineResult.phaseToRun() != null) {
            DMTypedHashMap params = null;
            if (engineResult.phaseParams() != null) {
                params = new DMTypedHashMap();
                params.putAll(engineResult.phaseParams());
            }
            ret.setPhaseToRun(engineResult.phaseToRun(), params);
        }

        // Copy flags
        ret.setSave(engineResult.shouldSave());
        ret.setAutoSave(engineResult.shouldAutoSave());
        ret.setSleep(engineResult.shouldSleep());
        ret.setRunOnClient(engineResult.shouldRunOnClient());
        ret.setAddAllHumans(engineResult.shouldAddAllHumans());
        ret.setOnlySendToWaitList(engineResult.shouldOnlySendToWaitList());

        // Note: Events are handled via SwingEventBus, not via engineResult.events()
        // The SwingEventBus converts new GameEvent records to legacy PokerTableEvent
        // objects and dispatches them to listeners (including ret_ via
        // startListening())
    }

    /**
     * Handle rejoining. Return true if table is in middle of rejoin and should not
     * be processed.
     */
    private boolean checkRejoin(PokerTable table) {
        if (table.getRejoinState() == PokerTable.REJOIN_NONE)
            return false;

        // rejoin started ... need to wait for REJOIN_PROCESS to be set...
        if (table.getRejoinState() == PokerTable.REJOIN_START) {
            // ...but first see if we have waited to long for a rejoin
            if (System.currentTimeMillis() - table.getLastRejoinStateChangeTime() > REJOIN_TIMEOUT_MILLIS) {
                logger.info("Timeout waiting for rejoin on table " + table.getName() + "...");
                PokerPlayer player;
                for (int i = 0; i < PokerConstants.SEATS; i++) {
                    player = table.getPlayer(i);
                    if (player == null)
                        continue;
                    if (player.isRejoining()) {
                        logger.info("   setRejoining(false) for " + player.getName());
                        player.setRejoining(false);
                        table.removeWait(player);
                    }
                }
                table.setRejoinState(PokerTable.REJOIN_NONE);
                return false;
            }
            return true;
        }

        // handle REJOIN_PROCESS for each rejoined player
        List<PokerPlayer> wait = table.getWaitList();
        PokerPlayer player;
        for (int i = wait.size() - 1; i >= 0; i--) {
            player = wait.get(i);
            if (player.isRejoining()) {
                // when save happens, we set to use pending save logic
                // as used in getStateForSave(), which will cause the
                // correct state to be passed along and run on the client
                if (DEBUG_REJOIN)
                    logger.debug("Sending rejoin table update to " + player.getName() + ", prev state: "
                            + PokerTable.getStringForState(table.getPreviousTableStateInt()));
                player.setRejoining(false);
                mgr_.sendTableUpdate(table, player, null, table.getPreviousTableStateInt(), true, null, false, null,
                        null, null);
            }
        }

        table.setRejoinState(PokerTable.REJOIN_NONE);
        return true;
    }

    /**
     * Handle save - take given state and return state that table should be in upon
     * load (handles case where save happened in a wait state)
     */
    public static int getStateForSave(GameState state, PokerTable table) {
        PokerSaveDetails pdetails = (PokerSaveDetails) state.getSaveDetails().getCustomInfo();

        // if doing a full save, tweak the state saved to allow
        // proper reloading
        if (pdetails.getSaveTables() == SaveDetails.SAVE_ALL) {
            switch (table.getTableStateInt()) {
                case PokerTable.STATE_BEGIN_WAIT :
                    return PokerTable.STATE_BEGIN;

                case PokerTable.STATE_PENDING :
                    return PokerTable.STATE_PENDING_LOAD;
            }
        } else if (pdetails.getOverrideState() != PokerSaveDetails.NO_OVERRIDE) {
            return pdetails.getOverrideState();
        }

        return table.getTableStateInt();
    }

    /**
     * Handle table in pending state
     */

    /**
     * check to see if a pending action has been waiting to long, and if so, handle
     * it
     */

    /**
     * betting timeout handling
     */

    /**
     * Send cancel message to all players on wait list
     */
    private void sendCancel(PokerTable table) {
        PokerPlayer player;
        int nNum = table.getWaitSize();
        for (int i = 0; i < nNum; i++) {
            player = table.getWaitPlayer(i);
            mgr_.doCancelAction(player);
        }
    }

    /**
     * Get seconds in a string (plurality considered)
     */
    private String getSeconds(int n) {
        return PropertyConfig.getMessage(n == 1 ? "msg.seconds.singular" : "msg.seconds.plural", n);
    }

    /**
     * Get number of seconds represented by given millis - special use method. If
     * the number of millis is within the TD's sleep time of a round number, that
     * round number is returned. Otherwise, -1 is returned. For example, if millis
     * is 10000 to 10249, then 10 is returned (assuming 250 millis sleep).
     */
    private int getWholeSeconds(long nMillis) {
        int nRemainder = (int) (nMillis % 1000);
        if (nRemainder < SLEEP_MILLIS) {
            return (int) nMillis / 1000;
        }
        return -1;
    }

    /**
     * deal high card for button
     */

    /**
     * initialize button on all computer tables
     */
    private void doDealForButtonAllComputers() {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++) {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent()) {
                table.setButton();
            }
        }
    }

    /**
     * check end of hand
     */

    /**
     * initial all computer tables for betting
     */
    private void doCheckEndHandAllComputers() {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++) {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent()) {
                table.aiRebuy();
            }
        }
    }

    /**
     * boot player logic
     */
    private void bootPlayers(PokerTable table) {
        TournamentProfile profile = game_.getProfile();

        PokerPlayer player;
        boolean bBooted;
        String sMsg = null;
        for (int i = 0; i < PokerConstants.SEATS; i++) {
            player = table.getPlayer(i);
            if (player == null)
                continue;
            bBooted = false;

            // determine if booted
            if (profile.isBootDisconnect() && player.getHandsPlayedDisconnected() >= profile.getBootDisconnectCount()) {
                bBooted = true;
                sMsg = PropertyConfig.getMessage("msg.chat.boot.disconnect", Utils.encodeHTML(player.getName()),
                        player.getHandsPlayedDisconnected());

            } else if (profile.isBootSitout() && player.getHandsPlayedSitout() >= profile.getBootSitoutCount()) {
                bBooted = true;
                sMsg = PropertyConfig.getMessage("msg.chat.boot.sitout", Utils.encodeHTML(player.getName()),
                        player.getHandsPlayedSitout());
            }

            // process booted players
            if (bBooted) {
                player.setBooted(true);
                int nChip = player.getChipCount();
                player.setChipCount(0);
                game_.addExtraChips(-nChip);
                sendDirectorChat(sMsg, Boolean.FALSE);
            }
        }
    }

    /**
     * check end of break, return true if break over
     */

    /**
     * during clean, listen to all tables and record which tables, players and
     * observers were impacted.
     */
    private class TDClean implements PokerTableListener {
        List<PokerTable> tables;
        List<PokerTable> tablesRemoved = new ArrayList<PokerTable>(1);
        List<PokerPlayer> playersTouched = new ArrayList<PokerPlayer>(10);
        List<PokerTable> tablesTouched = new ArrayList<PokerTable>(3);
        List<PokerPlayer> playersBusted = new ArrayList<PokerPlayer>(3);
        List<PokerPlayer> playersWaiting = new ArrayList<PokerPlayer>(1);

        public TDClean(PokerTable active) {
            tables = new ArrayList<PokerTable>(game_.getTables());
            listen(true);

            // need to make sure players on current table are included
            PokerPlayer player;
            addTableTouched(active);
            for (int i = 0; i < PokerConstants.SEATS; i++) {
                player = active.getPlayer(i);
                if (player == null)
                    continue;
                addPlayerTouched(player);
            }

        }

        public void finish() {
            listen(false);
        }

        private void listen(boolean bAdd) {
            int nTypes = PokerTableEvent.TYPES_PLAYERS_CHANGED | PokerTableEvent.TYPES_OBSERVERS_CHANGED
                    | PokerTableEvent.TYPE_TABLE_REMOVED;

            PokerTable table;
            int nNum = tables.size();
            for (int i = 0; i < nNum; i++) {
                table = tables.get(i);
                if (bAdd) {
                    table.addPokerTableListener(this, nTypes);
                } else {
                    table.removePokerTableListener(this, nTypes);
                }
            }
        }

        private void addTableTouched(PokerTable table) {
            if (!tablesTouched.contains(table))
                tablesTouched.add(table);
        }

        private void addTableRemoved(PokerTable table) {
            if (!tablesRemoved.contains(table))
                tablesRemoved.add(table);
        }

        private void addPlayerTouched(PokerPlayer player) {
            if (!playersTouched.contains(player))
                playersTouched.add(player);
        }

        public void tableEventOccurred(PokerTableEvent event) {
            if (DEBUG_CLEANUP_TABLE)
                logger.debug("TDClean event: " + event);
            PokerTable table = event.getTable();
            PokerPlayer player = event.getPlayer();
            int type = event.getType();
            switch (type) {
                case PokerTableEvent.TYPE_PLAYER_ADDED :
                case PokerTableEvent.TYPE_PLAYER_REMOVED :
                case PokerTableEvent.TYPE_OBSERVER_ADDED :
                case PokerTableEvent.TYPE_OBSERVER_REMOVED :
                    addTableTouched(table);
                    addPlayerTouched(player);
                    break;

                case PokerTableEvent.TYPE_TABLE_REMOVED :
                    addTableRemoved(table);
                    break;
            }

            // if player removed, they are busted (unless they are marked as waiting)
            if (type == PokerTableEvent.TYPE_PLAYER_REMOVED) {
                if (player.isWaiting()) {
                    if (!playersWaiting.contains(player))
                        playersWaiting.add(player);
                } else {
                    if (!playersBusted.contains(player))
                        playersBusted.add(player);
                }
            }
            // however if they were added, then they were just moved
            if (type == PokerTableEvent.TYPE_PLAYER_ADDED) {
                if (playersBusted.contains(player))
                    playersBusted.remove(player);
                if (playersWaiting.contains(player))
                    playersWaiting.remove(player);
            }
        }

        public void debugPrint() {
            logger.debug("TDClean results ------------------------");
            printTableList("Tables Removed", tablesRemoved);
            printTableList("Tables touched", tablesTouched);
            printPlayerList("Touched", playersTouched);
            printPlayerList("Busted", playersBusted);
            printPlayerList("Waiting", playersWaiting);
        }

        private void printPlayerList(String sName, List<PokerPlayer> players) {
            logger.debug("  " + sName + ":");
            for (PokerPlayer player : players) {
                logger.debug("    => " + player.getName());
            }
        }

        private void printTableList(String sName, List<PokerTable> printtables) {
            logger.debug(sName + ":");
            for (PokerTable table : printtables) {
                logger.debug("    => " + table.getName());
            }
        }

    }

    /**
     * clean tables, return true if need to wait for human action
     */

    /**
     * this version is called from cleanup - it sends cleaning done event and causes
     * process table to be run for clients of given table
     */
    private void notifyPlayersCleanDone(PokerTable table, TDClean clean) {
        // clean done, fire event so UI can do appropriate cleanup
        if (!game_.isGameOver()) {
            ret_.startListening(table); // start listening here to skip player add/remove events
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_CLEANING_DONE, table));
        }

        notifyPlayers(table, clean, true);

        // cleanup since we handled it here
        ret_.init();
    }

    /**
     * Notify all players of changes due to cleanup. In the simple case, this
     * updates all players of the results of the previous hand.
     */
    private void notifyPlayers(PokerTable table, TDClean clean, boolean bCleanDoneLogic) {
        PokerPlayer player;

        // get list of all players and observers
        List<PokerPlayer> allPlayer = game_.getPokerPlayersCopy();
        int nNum = game_.getNumObservers();
        for (int i = 0; i < nNum; i++) {
            player = game_.getPokerObserverAt(i);
            if (!allPlayer.contains(player)) {
                allPlayer.add(player);
            }
        }

        // send each a message
        String sPhase;
        boolean bSetCurrentTable;
        boolean bRunProcessTable;
        nNum = allPlayer.size();
        DMArrayList<PokerTableEvent> events;
        for (int i = 0; i < nNum; i++) {
            sPhase = null;
            bSetCurrentTable = false;
            bRunProcessTable = false;
            events = null;

            player = allPlayer.get(i);
            if (player.isComputer() || player.isRejoining())
                continue;

            if (clean.playersBusted.contains(player) || game_.isGameOver()) {
                // only run phase if online or (if not online) if game over
                // we do this because if not online and game not over, the
                // only reason this code is run is if the human is watching the
                // AI players finish out the game and we have already shown
                // them the GameOver dialog
                if (bCleanDoneLogic && (bOnline_ || game_.isGameOver())) {
                    sPhase = bOnline_ ? "OnlineGameOver" : "GameOver";
                }
                bSetCurrentTable = true;
            }

            // update current if player moved or player is observer.
            // if the table is the same, this is a noop on the client.
            if (clean.playersTouched.contains(player)) {
                bSetCurrentTable = true;
            }

            // if current table is still alive, need to run process table and send events
            if (bCleanDoneLogic && player.getTable() == table) {
                bRunProcessTable = true;
                events = ret_.getEvents();
            }

            // set current table for host
            if (player.isHost() && bSetCurrentTable) {
                PokerTable current = player.getTable();
                game_.setCurrentTable(current);

                // in practice, if moved to new table, need to start deal automatically
                // so that user doesn't have to press 'deal' twice
                // TODO: do this online if allow dealer-controlled dealing
                // TODO: do this when TESTING_ONLINE_AUTO_DEAL_OFF on
                if (bCleanDoneLogic && !bOnline_ && current.getTableStateInt() == PokerTable.STATE_DONE) {
                    current.setTableState(getTableStateStartDeal());
                }
            }

            // run phase locally
            if (bCleanDoneLogic && player.isHost() && sPhase != null) {
                context_.processPhase(sPhase);
            }

            // send update
            if (bOnline_) {
                mgr_.sendTableUpdate(table, player, events, PokerSaveDetails.NO_OVERRIDE, bRunProcessTable, sPhase,
                        bSetCurrentTable, clean.tablesTouched, clean.playersTouched, clean.tablesRemoved);
            }
        }
    }

    /**
     * Clean given table, and if that table is the current table, also clean all
     * computer tables. If bRemovePlayers is false, then players are not removed
     * from the table (they are still processed for how they finish. Leaving them at
     * the table is needed so the display still shows them).
     */
    public void cleanTables(PokerTable table, boolean bRemovePlayers) {
        List<PokerPlayer> removed = new ArrayList<PokerPlayer>();

        // clean tables (storing removed players in array)
        cleanTable(table, removed, bRemovePlayers);
        if (table.isCurrent())
            doCleanAllComputers(removed);

        // record placement of all players removed
        OtherTables.recordPlayerPlacement(this, game_, removed);
    }

    /**
     * do clean of all computer tables
     */
    private void doCleanAllComputers(List<PokerPlayer> removed) {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++) {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent()) {
                cleanTable(table, removed, true);
            }
        }
    }

    /**
     * Clean the given table.
     */
    private void cleanTable(PokerTable table, List<PokerPlayer> removed, boolean bRemovePlayers) {
        List<PokerPlayer> removedThisTable = new ArrayList<PokerPlayer>();
        boolean bAllComputerPrior = table.isAllComputer() && table.getNumObservers() == 0;

        // remove any left-over ai (if table used to have humans and the human was moved
        // away, it could have some AI left)
        if (bAllComputerPrior) {
            table.verifyAllAIRemoved();
        }

        // clean table
        OtherTables.cleanTable(table, removedThisTable, bRemovePlayers);

        // send message to removed players
        if (bRemovePlayers && !bAllComputerPrior) {
            processRemovedPlayers(table, removedThisTable);
        }

        removed.addAll(removedThisTable);
    }

    /**
     * Make busted out players observers and move observers from busted table.
     */
    private void processRemovedPlayers(PokerTable table, List<PokerPlayer> removed) {
        ApplicationError.assertTrue(bHost_, "Can only run processRemovedPlayers() on host");

        PokerTable newtable = table;

        // if table is now all computer players, change
        // the table to observe to a different table
        if (table.isAllComputer()) {
            newtable = getNewTable(table);
        }

        // busted
        PokerPlayer player;
        int nNum = removed.size();
        for (int i = 0; i < nNum; i++) {
            player = removed.get(i);
            if (player.isComputer())
                continue;

            // add player as an observer
            game_.addObserver(player);
            newtable.addObserver(player);

            if (DEBUG_CLEANUP_TABLE) {
                logger.debug(player.getName() + " eliminated, added to table as observer: " + newtable.getName());
            }
        }

        // move observers if table changed
        if (table.getNumObservers() > 0 && newtable != table) {
            moveObservers(table, newtable);
        }
    }

    /**
     * Get a new table to replace this table for observing.
     */
    private PokerTable getNewTable(PokerTable table) {
        PokerTable oldtable = table;
        PokerTable newtable = game_.getHost().getTable();
        PokerTable backup = null;
        PokerTable lookat;

        // if host has no table, then host was just removed,
        // so look for a table with humans
        if (newtable == null || newtable.isRemoved()) {
            newtable = null;
            int nNum = game_.getNumTables();
            for (int i = 0; i < nNum; i++) {
                lookat = game_.getTable(i);
                if (!lookat.isAllComputer()) {
                    newtable = lookat;
                    break;
                } else if (backup == null) {
                    backup = lookat;
                }
            }
        }

        // if we found a new table to set as current,
        // use it. Otherwise default to the all-ai
        // table that was passed in (unless that
        // was removed, then go to the backup table - some
        // other all-ai table)
        if (newtable != null)
            table = newtable;
        else if (table.isRemoved())
            table = backup;

        ApplicationError.assertNotNull(table, "No new table given existing table", oldtable);
        return table;
    }

    /**
     * Move observers from the given table to the new one
     */
    private void moveObservers(PokerTable from, PokerTable to) {
        ApplicationError.assertTrue(bHost_, "Can only run moveObservers() on host");

        int nNum;
        PokerPlayer player;
        nNum = from.getNumObservers();
        for (int i = 0; i < nNum; i++) {
            player = from.getObserver(0); // get first in list since we are reducing size of list as we go
            from.removeObserver(player);
            to.addObserver(player);

            if (DEBUG_CLEANUP_TABLE) {
                logger.debug(player.getName() + " observer moved from " + from.getName() + " to " + to.getName());
            }
        }
    }

    /**
     * do new level check, return whether need to wait
     */

    /**
     * initial all computer tables for betting
     */
    private void doLevelCheckAllComputers() {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++) {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent()) {
                table.aiRebuy();
                table.aiAddOn();
            }
        }
    }

    /**
     * do colorup determination
     */

    /**
     * initial all computer tables for betting
     */
    private void doColorUpAllComputers(int nMinNext) {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++) {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent()) {
                table.setNextMinChip(nMinNext);
                table.doColorUpDetermination();
                if (table.isColoringUp()) {
                    table.colorUp();
                    table.colorUpFinish();
                }
            }
        }
    }

    /**
     * Start break at table
     */

    /**
     * start break on all computer tables
     */
    private void doBreakAllComputers() {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++) {
            table = game_.getTable(i);

            if (table.isAllComputer() && !table.isCurrent()) {
                table.startBreak();
            }
        }
    }

    /**
     * Start hand at table
     */

    /**
     * initialize all computer tables for betting
     */
    private void doStartAllComputers() {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++) {
            table = game_.getTable(i);

            if (table.isAllComputer() && !table.isCurrent()) {
                // if not required to be on hold anymore, change state
                if (table.getTableStateInt() == PokerTable.STATE_ON_HOLD && table.getNumOccupiedSeats() > 1) {
                    table.setTableState(PokerTable.STATE_DONE);
                }

                // don't process on-hold tables
                if (table.getTableStateInt() != PokerTable.STATE_ON_HOLD) {
                    table.setTableState(PokerTable.STATE_BETTING);
                }
            }
        }
    }

    /**
     * do betting
     */

    /**
     * all computer betting
     */
    private void doBettingAllComputer() {
        int nNumTables = game_.getNumTables();
        PokerTable table;
        for (int i = 0; i < nNumTables; i++) {
            table = game_.getTable(i);
            if (table.isAllComputer() && !table.isCurrent()) {
                // shortcut for subsequent calls through here after we
                // have already bet (so this isn't done over and over)
                if (table.getTableStateInt() != PokerTable.STATE_BETTING)
                    return;

                // do quick AI bet
                table.simulateHand();

                // set new state
                table.setTableState(PokerTable.STATE_DONE);
            }
        }
    }

    /**
     * figure out new state
     */
    private int nextBettingState(HoldemHand hhand) {
        boolean bDone = hhand.isDone();
        if (!bDone)
            return PokerTable.STATE_BETTING;

        int nRound = hhand.getRound().toLegacy();
        if (nRound == BettingRound.RIVER.toLegacy())
            return PokerTable.STATE_PRE_SHOWDOWN;

        return PokerTable.STATE_COMMUNITY;
    }

    /**
     * community card
     */

    /**
     * pre-showdown
     */

    /**
     * showdown
     */

    /**
     * auto deal this table?
     */
    private boolean isAutoDeal(PokerTable table) {
        return (!bOnline_ && PokerUtils.isOptionOn(PokerConstants.OPTION_AUTODEAL))
                || TESTING(PokerConstants.TESTING_AUTOPILOT)
                || (bOnline_ && !(TESTING(PokerConstants.TESTING_ONLINE_AUTO_DEAL_OFF) && table.isCurrent())) ||

                // this check does auto deal when game
                // is over or potentially over (rebuy/never go broke needed
                // to continue). This pops up either a rebuy, tournament
                // over or never go broke dialog.
                (!bOnline_ && CheckEndHand.isGameOver(game_, false, this));
    }

    /**
     * Get auto deal delay
     *
     * @param table
     */
    private int getAutoDealDelay(PokerTable table) {
        int nDelay;

        if (!bOnline_ && CheckEndHand.isGameOver(game_, false, this)) {
            nDelay = 0;
        } else if (TESTING(PokerConstants.TESTING_AUTOPILOT)) {
            nDelay = 250;
        } else if (bOnline_) {
            nDelay = PokerUtils.getIntOption(PokerConstants.OPTION_AUTODEALONLINE) * 100;
        } else {
            HoldemHand hhand = table.getHoldemHand();
            boolean bHumanFolded = false;
            if (hhand != null) {
                PokerPlayer human = game_.getLocalPlayer();
                bHumanFolded = !human.isObserver() && human.getTable() == table && human.isFolded();
            }

            if (!bHumanFolded) {
                nDelay = PokerUtils.getIntOption(PokerConstants.OPTION_AUTODEALHAND) * 100;
            } else {
                nDelay = PokerUtils.getIntOption(PokerConstants.OPTION_AUTODEALFOLD) * 100;
            }
        }

        // logger.debug("Autodeal delay: " + nDelay);
        return nDelay;
    }

    /////
    ///// updates to director from outside - must synchronize
    /////

    /**
     * Notify of rejoining player
     */
    public synchronized void notifyPlayerRejoinStart(PokerPlayer p) {
        PokerTable table = p.getTable();
        if (!p.isObserver()) {
            if (DEBUG_REJOIN)
                logger.debug(p.getName() + " rejoin start, table now REJOIN_START");
            table.setRejoinState(PokerTable.REJOIN_START);
        }
        p.setRejoining(true);
    }

    /**
     * Notify rejoin process is done. This is also called when the client is ready
     * at start of tournament.
     */
    public synchronized void notifyPlayerRejoinDone(PokerPlayer p) {
        PokerTable table = p.getTable();
        // if this is a player and we are waiting on that player to act,
        // process them
        if (!p.isObserver() && table.isWaitListMember(p)) {
            // if we are waiting on deal for button, then this is the start
            // of the tournament, so remove the player from the wait list
            if (table.getTableStateInt() == PokerTable.STATE_PENDING
                    && table.getPendingTableStateInt() == PokerTable.STATE_DEAL_FOR_BUTTON) {
                if (DEBUG_REJOIN)
                    logger.debug(p.getName() + " ready for deal for button");
                table.removeWait(p);
            }
            // otherwise this player is done rejoining (TD is created and table is
            // displayed),
            // so we can process the rejoin
            else {
                if (DEBUG_REJOIN)
                    logger.debug(p.getName() + " rejoin done, table now REJOIN_PROCESS");
                table.setRejoinState(PokerTable.REJOIN_PROCESS);
            }
        } else {
            p.setRejoining(false);
            if (!p.isObserver()) {
                if (DEBUG_REJOIN)
                    logger.debug(p.getName() + " rejoin done, table now REJOIN_NONE");
                table.setRejoinState(PokerTable.REJOIN_NONE);
            }
        }
    }

    /**
     * handle player action from Bet or remote client
     */
    public synchronized void doHandAction(HandAction action, boolean bRemote) {
        doHandAction(action, true, true, bRemote);
    }

    /**
     * handle player action - internal with option to update state, call
     * processTable()
     */
    private void doHandAction(HandAction action, boolean bRemoveWaitList, boolean bProcessTable, boolean bRemote) {
        if (bClient_) {
            mgr_.doHandAction(action);
        } else {
            // store action in hand
            PokerPlayer player = action.getPlayer();
            PokerTable table = player.getTable();
            storeHandAction(action, bRemote);

            // remove from wait list to show an action
            // occurred and the next betting round should happen
            if (bRemoveWaitList) {
                table.removeWait(player);
            }

            // send HandAction to all other players
            if (bOnline_) {
                mgr_.doHandActionCopy(action);
            }

            // process next action
            if (bProcessTable) {
                processTable(table);
            }
        }
    }

    /**
     * actual processing where we store action in hand, which updates the state of
     * the holdem hand
     */
    private void storeHandAction(HandAction action, boolean bValidateRemote) {
        PokerPlayer player = action.getPlayer();
        PokerTable table = player.getTable();

        // current state must be pending and last state must be betting
        // for it to make sense to process this action from remote client
        if (bValidateRemote) {
            int nState = table.getTableStateInt();
            int nLastState = table.getPreviousTableStateInt();
            if (nState != PokerTable.STATE_PENDING && nLastState != PokerTable.STATE_BETTING) {
                logger.warn("Current state: " + PokerTable.getStringForState(nState) + ", last state: "
                        + PokerTable.getStringForState(nLastState) + "; incorrect for handling: " + action);
                return;
            }

            // make sure we are waiting on the right person
            PokerPlayer expected = table.getWaitPlayer();
            if (expected == null || expected != player) {
                logger.warn("Waiting on: " + (expected == null ? "(nobody)" : expected.getName()) + "; ignoring: "
                        + action);
                return;
            }
        }

        // play sound b4 doing all the player processing
        if (table.isCurrent() && !table.isZipMode()) {
            int nAction = action.getAction();
            switch (nAction) {
                case HandAction.ACTION_FOLD :
                    // TODO: fold audio
                    break;

                case HandAction.ACTION_CHECK :
                    PokerUtils.checkAudio();
                    break;

                case HandAction.ACTION_CHECK_RAISE :
                    PokerUtils.checkAudio();
                    break;

                case HandAction.ACTION_BET :
                    PokerUtils.betAudio();
                    break;

                case HandAction.ACTION_CALL :
                    PokerUtils.betAudio();
                    break;

                case HandAction.ACTION_RAISE :
                    PokerUtils.raiseAudio();
                    // PokerUtils.betAudio();
                    break;

                default :
                    ApplicationError.assertTrue(false, "Unknown HandAction action: " + nAction);
            }
        }

        // have player process this action
        player.processAction(action);

        // need to get action as stored in hand history
        if (table.isCurrent() && !table.isZipMode()) {
            HoldemHand hhand = player.getHoldemHand();
            action = hhand.getLastAction();
            int nPrior = 0;
            int nAction = action.getAction();
            if (nAction == HandAction.ACTION_RAISE) {
                nPrior = hhand.getNumPriorRaises(player);
            }

            sendDealerChatLocal(PokerConstants.CHAT_2, action.getChat(nPrior, null, null));
        }
    }

    /**
     * handle CC of hand action TODO: remove this method if decide to wait for
     * observers in TD wait list
     */
    public void storeHandActionCC(HandAction action) {
        // make sure current player is set - this can be
        // not set do to timing of DealDisplay/DealCommunity
        // in observers (can receive hand action prior to
        // those finishing because host doesn't wait on
        // observers to acknowledge they finished action)
        action.getPlayer().getHoldemHand().getCurrentPlayerWithInit();

        // do normal processing
        storeHandAction(action, false);
    }

    /**
     * remove player from wait list
     */
    public synchronized void removeFromWaitList(PokerPlayer player) {
        // could be null in shutdown instance
        if (player == null)
            return;

        // client observers never added to wait list, so don't waste processing TODO:
        // may change this...
        if (bClient_ && player.isObserver())
            return;

        if (bClient_) {
            mgr_.removeFromWaitList(player);
        } else {
            PokerTable table = player.getTable();
            table.removeWait(player);
            processTable(table);
        }
    }

    /**
     * send player update
     */
    public synchronized void playerUpdate(PokerPlayer player, String sSettings) {
        mgr_.sendPlayerUpdate(player, sSettings);
    }

    /**
     * process player update
     */
    public synchronized void processPlayerUpdate(PokerPlayer player, String sSettings) {
        player.setOnlineSettings(sSettings);
    }

    /**
     * do the deal
     */
    public synchronized void doDeal(PokerTable table) {
        table.setTableState(getTableStateStartDeal());
        processTable(table);
    }

    /**
     * send player update
     */
    public synchronized void changeTable(PokerPlayer player, PokerTable table) {
        if (bHost_) {
            PokerTable old = player.getTable();
            if (old == table || table.isAllComputer()) {
                // ignore request to do to same table or (perhaps a just new) all computer table
                return;
            }
            String sMsg = PropertyConfig.getMessage("msg.chat.observerchanged", Utils.encodeHTML(player.getName()),
                    old.getName(), table.getName());
            sendDealerChat(PokerConstants.CHAT_1, old, sMsg);
            sendDealerChat(PokerConstants.CHAT_1, table, sMsg);

            // gather events
            TDClean cleanEvents = new TDClean(table);
            old.removeObserver(player);
            table.addObserver(player);
            notifyPlayers(table, cleanEvents, false);
        } else {
            mgr_.changeTable(player, table);
        }
    }

    /**
     * keep this in one place
     */
    private static int getTableStateStartDeal() {
        return PokerTable.STATE_CHECK_END_HAND;
    }

    /**
     * game over
     */
    public synchronized void setGameOver() {
        // end each table
        PokerTable table;
        int nNumTables = game_.getNumTables();
        for (int i = 0; i < nNumTables; i++) {
            table = game_.getTable(i);
            table.setTableState(PokerTable.STATE_GAME_OVER);
            table.removeWaitAll();
        }

        // stop clock
        game_.getGameClock().stop();

        // update server
        if (bOnline_ && !game_.isGameOver() && game_.isPublic()) {
            endWanGame();
        }

        // note game over after processing done
        game_.setGameOver(true);
    }

    /**
     * rebuy
     */
    public synchronized void doRebuy(PokerPlayer player, int nLevel, int nAmount, int nChips, boolean bPending) {
        PokerTable table = player.getTable();
        if (table == null)
            return; // safety

        if (!table.isRebuyAllowed(player, nLevel)) {
            logger.warn("Skipping non-allowed rebuy for " + player.getName() + " level: " + nLevel + " amount: "
                    + nAmount + " chips: " + nChips + " pending: " + bPending);
            return;
        }

        if (bClient_) {
            player.addRebuy(nAmount, nChips, bPending);
            mgr_.doRebuy(player, nLevel, nAmount, nChips, bPending);
        } else {
            player.addRebuy(nAmount, nChips, bPending);

            sendDealerChat(PokerConstants.CHAT_1, player.getTable(),
                    PokerUtils.chatInformation(PropertyConfig.getMessage(bPending ? "chat.rebuy.pending" : "chat.rebuy",
                            Utils.encodeHTML(player.getName()), nChips)));
        }
    }

    /**
     * rebuy
     */
    public synchronized void doAddon(PokerPlayer player, int nAmount, int nChips) {
        if (bClient_) {
            player.addAddon(nAmount, nChips);
            mgr_.doAddon(player, nAmount, nChips);
        } else {
            player.addAddon(nAmount, nChips);
            sendDealerChat(PokerConstants.CHAT_1, player.getTable(), PokerUtils.chatInformation(
                    PropertyConfig.getMessage("chat.addon", Utils.encodeHTML(player.getName()), nChips)));
        }
    }

    ////
    //// chat
    ////

    /**
     * chat handler
     */
    public void setChatHandler(ChatHandler chat) {
        chat_ = chat;
        if (mgr_ != null) {
            mgr_.setChatHandler(chat);
        }
    }

    /**
     * send chat from client
     */
    public void sendChat(String sMessage, PokerTable table, String sTestData) {
        if (mgr_ != null) {
            mgr_.sendChat(sMessage, table, sTestData);
        }
    }

    /**
     * send chat to given player
     */
    public void sendChat(int nPlayerID, String sMessage) {
        if (mgr_ != null) {
            mgr_.sendChat(nPlayerID, sMessage);
        }
    }

    /**
     * send message to all people at table
     */
    private void sendDealerChat(int nType, PokerTable table, String sMessage) {
        if (mgr_ != null) {
            mgr_.sendDealerChat(nType, sMessage, table);
        } else {
            if (table.isCurrent()) {
                deliverChatLocal(nType, sMessage, OnlineMessage.CHAT_DEALER_MSG_ID);
            }
        }
    }

    /**
     * send message to all players
     */
    public void sendDirectorChat(String sMessage, Boolean bPauseClock) {
        if (mgr_ != null) {
            mgr_.sendDirectorChat(sMessage, bPauseClock);
        } else {
            deliverChatLocal(PokerConstants.CHAT_ALWAYS, sMessage, OnlineMessage.CHAT_DIRECTOR_MSG_ID);
        }
    }

    /**
     * display dealer chat local
     */
    public void sendDealerChatLocal(int nType, String sMessage) {
        deliverChatLocal(nType, sMessage, OnlineMessage.CHAT_DEALER_MSG_ID);
    }

    /**
     * In practice mode, used due to no online manager, in online used to deliver
     * messages as a result of some other action (to avoid unnecessary network
     * traffic)
     */
    public void deliverChatLocal(int nType, String sMessage, int id) {
        if (chat_ != null) {
            OnlineMessage chat = new OnlineMessage(OnlineMessage.CAT_CHAT);
            chat.setChat(sMessage);
            chat.setChatType(nType);
            chat.setFromPlayerID(id);
            chat_.chatReceived(chat);
        }
    }

    /**
     * Start the WAN game.
     */
    private void startWanGame() {
        // No server processing if not the host or not a public game.
        if (!bOnline_ || !bHost_ || !game_.isPublic()) {
            return;
        }

        // Send a message requesting that the game be started.
        OnlineServer manager = OnlineServer.getWanManager();
        manager.startGame(game_);
    }

    /**
     * End the WAN game.
     */
    private void endWanGame() {
        // No server processing if not the host or not a public game.
        if (!bOnline_ || !bHost_ || !game_.isPublic()) {
            return;
        }

        // Send a message requesting that the game be ended and results stored.
        OnlineServer manager = OnlineServer.getWanManager();
        manager.endGame(game_, true);
    }
}
