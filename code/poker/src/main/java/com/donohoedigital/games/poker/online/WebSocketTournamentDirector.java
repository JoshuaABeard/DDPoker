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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.BasePhase;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.engine.GameManager;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.dashboard.AdvisorState;
import com.donohoedigital.games.poker.dashboard.AdvanceAction;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.core.ai.HandInfoFast;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandScoreConstants;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData.*;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Replaces {@link TournamentDirector} in the phase chain.
 *
 * <p>
 * Connects to the embedded WebSocket server, receives all 27 server-to-client
 * message types, and translates them into view model updates
 * ({@link RemotePokerTable}, {@link RemoteHoldemHand}) plus
 * {@link PokerTableEvent} firings so the unchanged Swing UI renders game state
 * correctly.
 *
 * <p>
 * Contains zero poker logic. All game decisions are made server-side. This
 * class only maps server messages to UI state.
 *
 * <p>
 * Registered in {@code gamedef.xml} as the replacement for
 * {@code TournamentDirector}.
 */
public class WebSocketTournamentDirector extends BasePhase
        implements
            Runnable,
            GameManager,
            ChatManager,
            PokerDirector {

    private static final Logger logger = LogManager.getLogger(WebSocketTournamentDirector.class);

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // Injected via BasePhase.context_
    private PokerGame game_;

    // Filled from PokerGame.getWebSocketConfig() in start()
    private int serverPort_;
    private String gameId_;
    private String jwt_;

    // One WebSocket connection per game session
    private final WebSocketGameClient wsClient_ = new WebSocketGameClient();

    // Tracks per-player pre-flop stats from observed actions
    private final WebSocketOpponentTracker opponentTracker_ = new WebSocketOpponentTracker();
    private volatile boolean isPreFlop_ = false;

    // True if SHOWDOWN_STARTED was received for the current hand.
    // Reset to false on each HAND_STARTED.
    private volatile boolean showdownStarted_ = false;

    // One RemotePokerTable per server table ID
    private final Map<Integer, RemotePokerTable> tables_ = new HashMap<>();

    // Per-table hand-result capture state used to publish API-HR-01 in /state.
    private final Map<Integer, HandResultCapture> handResultCaptureByTable_ = new HashMap<>();

    // Current player ID for this client (set from CONNECTED message)
    private long localPlayerId_ = -1;

    // Maps server-assigned player IDs to the canonical PokerPlayer objects in
    // game_.players_. Built lazily on first use (see resolveGamePlayer).
    // Server IDs come from the JWT profileId (human) and sequential negatives
    // -1, -2, ... (AI), which differ from the client-assigned IDs 0, 1, 2, ...
    private final Map<Long, PokerPlayer> serverIdToGamePlayer_ = new HashMap<>();

    // Last ACTION_REQUIRED options (stored for UI query)
    private ActionOptionsData currentOptions_;

    // Chat handler registered by ShowTournamentTable
    private ChatHandler chatHandler_;

    // Tracks the last sit-out state sent to the server, to avoid re-sending on
    // echo.
    private boolean lastSentSittingOut_ = false;

    // Lobby player list maintained while game is in WAITING_FOR_PLAYERS state
    private final List<LobbyPlayerData> lobbyPlayers_ = new ArrayList<>();

    // Most recent full GAME_STATE payload from the server.
    private volatile GameStateData latestGameState_;

    // Scheduler for rebuy/addon decline timeouts (Gap 1: prevent server deadlock
    // when user ignores the rebuy/addon button in the Swing UI).
    private final ScheduledExecutorService declineScheduler_ = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-decline-timer");
        t.setDaemon(true);
        return t;
    });
    private volatile ScheduledFuture<?> pendingRebuyDecline_;
    private volatile ScheduledFuture<?> pendingAddonDecline_;

    // Rebuy outcome observability for control-server /state snapshots.
    private final Object rebuyObservabilityLock_ = new Object();
    private long rebuyOfferSequence_;
    private long rebuyDecisionSequence_;
    private long rebuyApplySequence_;
    private String rebuyState_ = "NONE";
    private long rebuyStateAtMs_;
    private int rebuyLastOfferCost_;
    private int rebuyLastOfferChips_;
    private int rebuyLastOfferTimeoutSeconds_;
    private long rebuyLastOfferAtMs_;
    private String rebuyLastDecision_ = "NONE";
    private long rebuyLastDecisionAtMs_;
    private boolean rebuyLastDecisionPending_;
    private boolean rebuyAwaitingServerApply_;
    private long rebuyLastAppliedPlayerId_ = -1;
    private int rebuyLastAppliedAddedChips_;
    private long rebuyLastAppliedAtMs_;

    // -------------------------------------------------------------------------
    // Phase lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called by the phase engine when this phase becomes active. Reads WebSocket
     * config from the game, connects to the embedded server, registers the
     * PlayerActionListener, and starts message dispatch.
     */
    @Override
    public void start() {
        game_ = (PokerGame) context_.getGame();
        game_.setWebSocketOpponentTracker(opponentTracker_);
        serverIdToGamePlayer_.clear();
        tables_.clear();
        handResultCaptureByTable_.clear();
        latestGameState_ = null;
        lastSentSittingOut_ = false;
        resetRebuyObservability();
        WsMessageLog.clear();
        GameEventLog.clear();
        HandResultLog.clear();

        PokerGame.WebSocketConfig config = game_.getWebSocketConfig();
        if (config == null) {
            throw new IllegalStateException("WebSocketTournamentDirector started without a WebSocketConfig — "
                    + "call game.setWebSocketConfig() before launching this phase");
        }
        serverPort_ = config.port();
        gameId_ = config.gameId();
        jwt_ = config.jwt();

        wsClient_.setMessageHandler(this::onMessage);
        wsClient_.connect(serverPort_, gameId_, jwt_);

        // Route player UI actions to WebSocket; hide buttons immediately on click.
        // NOTE: the listener receives PokerGame.ACTION_* constants (e.g.
        // ACTION_FOLD=1),
        // NOT HandAction.ACTION_* constants (ACTION_FOLD=0). Use a dedicated mapper.
        game_.setPlayerActionListener((action, amount) -> {
            // Only hide buttons if we are actually connected. If not, leave the UI
            // active so the player can retry after reconnect, and notify via chat.
            if (!wsClient_.isConnected()) {
                logger.warn("[ACTION] not connected, dropping action={} amount={}", action, amount);
                deliverChatLocal(PokerConstants.CHAT_ALWAYS, "Action not sent \u2014 reconnecting to server...",
                        PokerConstants.CHAT_DEALER_MSG_ID);
                return;
            }
            game_.setInputMode(PokerTableInput.MODE_QUITSAVE);
            if (action == PokerGame.ACTION_CONTINUE_LOWER) {
                wsClient_.sendContinueRunout();
            } else {
                String wsAction = mapPokerGameActionToWsString(action);
                wsClient_.sendAction(wsAction, resolveActionAmount(action, amount));
            }
        });

        // Register as the GameManager so ShowTournamentTable.poststart() can wire chat.
        context_.setGameManager(this);
    }

    /**
     * Main loop — the WebSocket listener dispatches messages asynchronously, so
     * run() only needs to exist to satisfy the Runnable contract for the phase
     * system. Actual message processing happens in {@link #onMessage}.
     */
    @Override
    public void run() {
        // Message processing is handled asynchronously by the WebSocket listener.
        // This method is intentionally empty.
    }

    /**
     * Called by the phase engine when this phase ends. Disconnects the WebSocket
     * and clears the PlayerActionListener.
     */
    @Override
    public void finish() {
        context_.setGameManager(null);
        cancelPendingRebuyDecline();
        cancelPendingAddonDecline();
        declineScheduler_.shutdown();
        wsClient_.disconnect();
        game_.setPlayerActionListener(null);
    }

    // -------------------------------------------------------------------------
    // Inbound message dispatch
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Test support
    // -------------------------------------------------------------------------

    /**
     * Injects a pre-built PokerGame for unit testing without a full phase context.
     * Tests must call this before dispatching messages.
     */
    void setGameForTest(PokerGame game) {
        game_ = game;
    }

    /**
     * Injects a local player ID for unit testing (normally set from CONNECTED
     * message).
     */
    void setLocalPlayerIdForTest(long id) {
        localPlayerId_ = id;
    }

    /** Returns the number of active tables (for testing). */
    int getTableCount() {
        return tables_.size();
    }

    /** Returns current lobby player list (for testing and UI query). */
    List<LobbyPlayerData> getLobbyPlayers() {
        return java.util.Collections.unmodifiableList(lobbyPlayers_);
    }

    /**
     * Returns the first table in map order (for testing single-table scenarios).
     */
    RemotePokerTable getFirstTable() {
        if (tables_.isEmpty())
            return null;
        return tables_.values().iterator().next();
    }

    /**
     * Returns the table with the given ID, or null (for testing multi-table
     * scenarios).
     */
    RemotePokerTable getTableForTest(int tableId) {
        return tables_.get(tableId);
    }

    /** Exposes the opponent tracker for unit testing. */
    WebSocketOpponentTracker getOpponentTrackerForTest() {
        return opponentTracker_;
    }

    /**
     * Clears the tables map for testing, simulating the effect of {@link #start()}
     * being called at the beginning of a new game.
     */
    void clearTablesForTest() {
        tables_.clear();
    }

    /**
     * Shuts down schedulers for testing lifecycle (simulates the effect of finish()
     * without requiring a full phase context).
     */
    void finishSchedulersForTest() {
        cancelPendingRebuyDecline();
        cancelPendingAddonDecline();
        declineScheduler_.shutdown();
    }

    /** Returns true if the decline scheduler has been shut down (for testing). */
    boolean isDeclineSchedulerShutdownForTest() {
        return declineScheduler_.isShutdown();
    }

    public Map<String, Object> getControlObservabilitySnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("source", "websocket");
        snapshot.put("localPlayerId", localPlayerId_);

        RemotePokerTable current = currentTable();
        snapshot.put("currentTableId", current != null ? current.getNumber() : -1);

        GameStateData latest = latestGameState_;
        int serverTableCount = latest != null && latest.tables() != null ? latest.tables().size() : 0;
        int serverPlayerCount = latest != null && latest.players() != null ? latest.players().size() : 0;
        snapshot.put("serverReportedTableCount", serverTableCount);
        snapshot.put("serverReportedPlayerCount", serverPlayerCount);

        List<RemotePokerTable> localTables = new ArrayList<>(tables_.values());
        snapshot.put("remoteTableCount", localTables.size());

        List<Map<String, Object>> tableSummaries = new ArrayList<>();
        int totalSeatedPlayers = 0;
        int totalPlayersWithChips = 0;
        int totalChips = 0;
        int totalPot = 0;

        for (RemotePokerTable table : localTables) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", table.getNumber());
            t.put("dealerSeat", table.getButton());

            RemoteHoldemHand hand = table.getRemoteHand();
            int pot = hand != null ? hand.getTotalPotChipCount() : 0;
            t.put("pot", pot);
            t.put("round", hand != null ? hand.getRound().name() : "NONE");
            t.put("handNumber", table.getHandNum());
            totalPot += pot;

            List<Map<String, Object>> seats = new ArrayList<>();
            int seated = 0;
            int withChips = 0;
            int tableChips = 0;
            for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
                PokerPlayer p = table.getPlayer(seat);
                if (p == null) {
                    continue;
                }

                seated++;
                int chips = p.getChipCount();
                if (chips > 0) {
                    withChips++;
                }
                tableChips += chips;

                Map<String, Object> seatMap = new LinkedHashMap<>();
                seatMap.put("seat", seat);
                seatMap.put("playerId", p.getID());
                seatMap.put("name", p.getName());
                seatMap.put("chips", chips);
                seatMap.put("isHuman", p.isHuman());
                seatMap.put("isEliminated", p.isEliminated());
                seats.add(seatMap);
            }

            t.put("seatedPlayers", seated);
            t.put("playersWithChips", withChips);
            t.put("totalChips", tableChips);
            t.put("seats", seats);

            totalSeatedPlayers += seated;
            totalPlayersWithChips += withChips;
            totalChips += tableChips;
            tableSummaries.add(t);
        }

        tableSummaries.sort((a, b) -> Integer.compare((int) a.get("id"), (int) b.get("id")));
        snapshot.put("tables", tableSummaries);
        snapshot.put("totalSeatedPlayers", totalSeatedPlayers);
        snapshot.put("totalPlayersWithChips", totalPlayersWithChips);
        snapshot.put("totalChips", totalChips);
        snapshot.put("totalPot", totalPot);
        snapshot.put("hasMultipleTables", serverTableCount > 1 || localTables.size() > 1);
        snapshot.put("rebuyOutcome", buildRebuyOutcomeSnapshot());
        return snapshot;
    }

    // -------------------------------------------------------------------------
    // Inbound message dispatch
    // -------------------------------------------------------------------------

    /**
     * Central dispatch: called for every inbound WebSocket message. Parses the JSON
     * payload into the correct {@link ServerMessageData} subtype and delegates to
     * the appropriate handler.
     *
     * <p>
     * All view model updates and event firings are marshalled to the Swing EDT so
     * the UI code need not synchronize.
     *
     * <p>
     * Package-private for unit testing.
     */
    void onMessage(WebSocketGameClient.InboundMessage msg) {
        ServerMessageType type = msg.type();
        JsonNode data = msg.data();
        logger.debug("[WS-IN] type={}", type);
        WsMessageLog.logInbound(type.name(), data != null ? data.toString() : "");
        try {
            switch (type) {
                case CONNECTED -> {
                    ConnectedData d = parse(data, ConnectedData.class);
                    onConnected(d);
                }
                case GAME_STATE -> {
                    GameStateData d = parse(data, GameStateData.class);
                    onGameState(d);
                }
                case HAND_STARTED -> {
                    HandStartedData d = parse(data, HandStartedData.class);
                    onHandStarted(d);
                }
                case HOLE_CARDS_DEALT -> {
                    HoleCardsDealtData d = parse(data, HoleCardsDealtData.class);
                    onHoleCardsDealt(d);
                }
                case COMMUNITY_CARDS_DEALT -> {
                    CommunityCardsDealtData d = parse(data, CommunityCardsDealtData.class);
                    onCommunityCardsDealt(d);
                }
                case ACTION_REQUIRED -> {
                    ActionRequiredData d = parse(data, ActionRequiredData.class);
                    onActionRequired(d);
                    logger.debug("[WS-DIRECTOR] queued {} to EDT", type);
                }
                case PLAYER_ACTED -> {
                    PlayerActedData d = parse(data, PlayerActedData.class);
                    onPlayerActed(d);
                }
                case ACTION_TIMEOUT -> {
                    ActionTimeoutData d = parse(data, ActionTimeoutData.class);
                    onActionTimeout(d);
                }
                case HAND_COMPLETE -> {
                    HandCompleteData d = parse(data, HandCompleteData.class);
                    onHandComplete(d);
                }
                case POT_AWARDED -> {
                    PotAwardedData d = parse(data, PotAwardedData.class);
                    onPotAwarded(d);
                }
                case SHOWDOWN_STARTED -> {
                    ShowdownStartedData d = parse(data, ShowdownStartedData.class);
                    onShowdownStarted(d);
                }
                case LEVEL_CHANGED -> {
                    LevelChangedData d = parse(data, LevelChangedData.class);
                    onLevelChanged(d);
                }
                case PLAYER_ELIMINATED -> {
                    PlayerEliminatedData d = parse(data, PlayerEliminatedData.class);
                    onPlayerEliminated(d);
                }
                case REBUY_OFFERED -> {
                    RebuyOfferedData d = parse(data, RebuyOfferedData.class);
                    onRebuyOffered(d);
                }
                case ADDON_OFFERED -> {
                    AddonOfferedData d = parse(data, AddonOfferedData.class);
                    onAddonOffered(d);
                }
                case GAME_COMPLETE -> {
                    GameCompleteData d = parse(data, GameCompleteData.class);
                    onGameComplete(d);
                }
                case PLAYER_JOINED -> {
                    PlayerJoinedData d = parse(data, PlayerJoinedData.class);
                    onPlayerJoined(d);
                }
                case PLAYER_LEFT -> {
                    PlayerLeftData d = parse(data, PlayerLeftData.class);
                    onPlayerLeft(d);
                }
                case PLAYER_DISCONNECTED -> {
                    PlayerDisconnectedData d = parse(data, PlayerDisconnectedData.class);
                    onPlayerDisconnected(d);
                }
                case PLAYER_REBUY -> {
                    PlayerRebuyData d = parse(data, PlayerRebuyData.class);
                    onPlayerRebuy(d);
                }
                case PLAYER_ADDON -> {
                    PlayerAddonData d = parse(data, PlayerAddonData.class);
                    onPlayerAddon(d);
                }
                case GAME_PAUSED -> {
                    GamePausedData d = parse(data, GamePausedData.class);
                    onGamePaused(d);
                }
                case GAME_RESUMED -> {
                    GameResumedData d = parse(data, GameResumedData.class);
                    onGameResumed(d);
                }
                case PLAYER_KICKED -> {
                    PlayerKickedData d = parse(data, PlayerKickedData.class);
                    onPlayerKicked(d);
                }
                case CHAT_MESSAGE -> {
                    ChatMessageData d = parse(data, ChatMessageData.class);
                    onChatMessage(d);
                }
                case TIMER_UPDATE -> {
                    TimerUpdateData d = parse(data, TimerUpdateData.class);
                    onTimerUpdate(d);
                }
                case ERROR -> {
                    ErrorData d = parse(data, ErrorData.class);
                    onError(d);
                }
                case LOBBY_STATE -> {
                    LobbyStateData d = parse(data, LobbyStateData.class);
                    onLobbyState(d);
                }
                case LOBBY_PLAYER_JOINED -> {
                    LobbyPlayerJoinedData d = parse(data, LobbyPlayerJoinedData.class);
                    onLobbyPlayerJoined(d);
                }
                case LOBBY_PLAYER_LEFT -> {
                    LobbyPlayerLeftData d = parse(data, LobbyPlayerLeftData.class);
                    onLobbyPlayerLeft(d);
                }
                case LOBBY_SETTINGS_CHANGED -> {
                    LobbySettingsChangedData d = parse(data, LobbySettingsChangedData.class);
                    onLobbySettingsChanged(d);
                }
                case LOBBY_GAME_STARTING -> {
                    LobbyGameStartingData d = parse(data, LobbyGameStartingData.class);
                    onLobbyGameStarting(d);
                }
                case LOBBY_PLAYER_KICKED -> {
                    LobbyPlayerKickedData d = parse(data, LobbyPlayerKickedData.class);
                    onLobbyPlayerKicked(d);
                }
                case GAME_CANCELLED -> {
                    GameCancelledData d = parse(data, GameCancelledData.class);
                    onGameCancelled(d);
                }
                case CHIPS_TRANSFERRED -> {
                    ServerMessageData.ChipsTransferredData d = parse(data,
                            ServerMessageData.ChipsTransferredData.class);
                    onChipsTransferred(d);
                }
                case NEVER_BROKE_OFFERED -> {
                    ServerMessageData.NeverBrokeOfferedData d = parse(data,
                            ServerMessageData.NeverBrokeOfferedData.class);
                    onNeverBrokeOffered(d);
                }
                case COLOR_UP_STARTED -> {
                    ServerMessageData.ColorUpStartedData d = parse(data, ServerMessageData.ColorUpStartedData.class);
                    onColorUpStarted(d);
                }
                case AI_HOLE_CARDS -> {
                    ServerMessageData.AiHoleCardsData d = parse(data, ServerMessageData.AiHoleCardsData.class);
                    onAiHoleCards(d);
                }
                case CONTINUE_RUNOUT -> onContinueRunout();
                case PLAYER_SAT_OUT -> {
                    ServerMessageData.PlayerSatOutData d = parse(data, ServerMessageData.PlayerSatOutData.class);
                    onPlayerSatOut(d);
                }
                case PLAYER_CAME_BACK -> {
                    ServerMessageData.PlayerCameBackData d = parse(data, ServerMessageData.PlayerCameBackData.class);
                    onPlayerCameBack(d);
                }
                case OBSERVER_JOINED -> {
                    ServerMessageData.ObserverJoinedData d = parse(data, ServerMessageData.ObserverJoinedData.class);
                    onObserverJoined(d);
                }
                case OBSERVER_LEFT -> {
                    ServerMessageData.ObserverLeftData d = parse(data, ServerMessageData.ObserverLeftData.class);
                    onObserverLeft(d);
                }
                case COLOR_UP_COMPLETED -> {
                    ServerMessageData.ColorUpCompletedData d = parse(data,
                            ServerMessageData.ColorUpCompletedData.class);
                    onColorUpCompleted(d);
                }
                case BUTTON_MOVED -> {
                    ServerMessageData.ButtonMovedData d = parse(data, ServerMessageData.ButtonMovedData.class);
                    onButtonMoved(d);
                }
                case CURRENT_PLAYER_CHANGED -> {
                    ServerMessageData.CurrentPlayerChangedData d = parse(data,
                            ServerMessageData.CurrentPlayerChangedData.class);
                    onCurrentPlayerChanged(d);
                }
                case TABLE_STATE_CHANGED -> {
                    ServerMessageData.TableStateChangedData d = parse(data,
                            ServerMessageData.TableStateChangedData.class);
                    onTableStateChanged(d);
                }
                case CLEANING_DONE -> {
                    ServerMessageData.CleaningDoneData d = parse(data, ServerMessageData.CleaningDoneData.class);
                    onCleaningDone(d);
                }
                case PLAYER_MOVED -> {
                    ServerMessageData.PlayerMovedData d = parse(data, ServerMessageData.PlayerMovedData.class);
                    onPlayerMoved(d);
                }
                case ADVISOR_UPDATE -> {
                    ServerMessageData.AdvisorData d = parse(data, ServerMessageData.AdvisorData.class);
                    onAdvisorUpdate(d);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling {} message", type, e);
            deliverChatLocal(PokerConstants.CHAT_ALWAYS, "Warning: failed to process " + type + " message",
                    PokerConstants.CHAT_DEALER_MSG_ID);
        }
    }

    // -------------------------------------------------------------------------
    // Message handlers
    // -------------------------------------------------------------------------

    private void onConnected(ConnectedData d) {
        localPlayerId_ = d.playerId();
        logger.debug("[CONNECTED] playerId={} hasGameState={} hasReconnectToken={}", d.playerId(),
                d.gameState() != null, d.reconnectToken() != null);
        // Store reconnect token for auto-reconnect (24h TTL, replaces short-lived
        // ws-connect JWT)
        if (d.reconnectToken() != null) {
            wsClient_.setReconnectToken(d.reconnectToken());
        }
        if (d.gameState() != null) {
            onGameState(d.gameState());
        }
    }

    private void onGameState(GameStateData d) {
        latestGameState_ = d;
        logger.debug("[GAME_STATE] status={} level={} tables={} players={}", d.status(), d.level(),
                d.tables() != null ? d.tables().size() : 0, d.players() != null ? d.players().size() : 0);
        SwingUtilities.invokeLater(() -> {
            logger.debug("[GAME_STATE EDT] applying {} tables", d.tables() != null ? d.tables().size() : 0);
            // Update level and tournament-wide info immediately
            game_.setLevel(d.level());
            game_.setServerTournamentInfo(d.totalPlayers(), d.playersRemaining(), d.numTables(), d.playerRank());
            // Start the clock if not already running (handles initial game-start state)
            if (!game_.getGameClock().isRunning()) {
                game_.getGameClock().setSecondsRemaining(game_.getSecondsInLevel(d.level()));
                game_.getGameClock().start();
            }

            // On the first GAME_STATE, remove all locally-created tables that pre-date the
            // WebSocket connection (setup tables from setupTournament, and any stale
            // RemotePokerTable instances left from a previous game attempt).
            if (tables_.isEmpty()) {
                for (PokerTable local : new ArrayList<>(game_.getTables())) {
                    game_.removeTable(local);
                    logger.debug("[GAME_STATE EDT] removed local setup table: {}", local.getName());
                }
            }

            // Remove tables no longer present
            List<Integer> receivedIds = new ArrayList<>();
            if (d.tables() != null) {
                for (TableData td : d.tables()) {
                    receivedIds.add(td.tableId());
                }
            }
            List<Integer> toRemove = new ArrayList<>(tables_.keySet());
            toRemove.removeAll(receivedIds);
            for (int id : toRemove) {
                RemotePokerTable t = tables_.remove(id);
                game_.removeTable(t);
            }

            // Create or update tables
            if (d.tables() == null)
                return;
            for (TableData td : d.tables()) {
                RemotePokerTable table = tables_.computeIfAbsent(td.tableId(), id -> {
                    RemotePokerTable t = new RemotePokerTable(game_, id);
                    game_.addTable(t);
                    t.setMinChip(game_.getMinChip());
                    return t;
                });
                applyTableData(table, td);
            }

            // Apply blind amounts to all table hands so getSmallBlind()/getBigBlind()
            // return correct values for UI rendering and hand history recording.
            if (d.blinds() != null) {
                for (RemotePokerTable table : tables_.values()) {
                    RemoteHoldemHand hand = table.getRemoteHand();
                    if (hand != null) {
                        hand.setSmallBlind(d.blinds().small());
                        hand.setBigBlind(d.blinds().big());
                        hand.setAnte(d.blinds().ante());
                    }
                }
            }

            // Select the table that contains the local player
            selectLocalPlayerTable();
        });
    }

    private void onHandStarted(HandStartedData d) {
        logger.debug("[HAND_STARTED] dealer={}", d.dealerSeat());
        isPreFlop_ = true;
        AdvisorState.clear();
        opponentTracker_.onHandStart();
        SwingUtilities.invokeLater(() -> {
            showdownStarted_ = false;
            PokerPlayer human = game_.getHumanPlayer();
            if (human != null) {
                human.setHandStrength(-1f);
                human.setHandPotential(-1f);
            }
            // Apply to all tables (HAND_STARTED is per-table; we apply to the current
            // table)
            RemotePokerTable table = currentTable();
            if (table == null) {
                logger.debug("[HAND_STARTED EDT] currentTable=null, skipping");
                return;
            }

            HandResultCapture capture = handResultCaptureByTable_.computeIfAbsent(table.getNumber(),
                    k -> new HandResultCapture());
            capture.resetForHand(d.handNumber(), snapshotStartPayoutBySeat(table));

            // Clear visual state from the previous hand (card pieces, result overlays such
            // as "all-in", and the pot display). TYPE_CLEANING_DONE is not fired by the
            // server in WebSocket mode, so we fire it here to ensure eliminated players'
            // cards and status indicators are removed before the new hand begins.
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_CLEANING_DONE, table));

            // Reuse the existing hand from the preceding GAME_STATE snapshot if available
            // so that bets (blinds/antes) set in applyTableData() are preserved.
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null) {
                hand = new RemoteHoldemHand();
                table.setRemoteHand(hand);
            } else {
                // Clear per-hand state that must not carry over from the previous hand.
                // Wins accumulate via remoteWins_.merge(), so without this clear the next
                // hand's WIN overlay would show the sum of all previous win amounts.
                hand.clearWins();
                hand.updateCommunity(new Hand());
            }
            hand.updateRound(BettingRound.PRE_FLOP);
            hand.updateSmallBlindSeat(d.smallBlindSeat());
            hand.updateBigBlindSeat(d.bigBlindSeat());

            // Reconstruct player order from seated players (seat order for the hand)
            List<PokerPlayer> handPlayers = seatedPlayersFrom(table);
            hand.updatePlayerOrder(handPlayers);
            hand.updateCurrentPlayer(HoldemHand.NO_CURRENT_PLAYER);

            // Move button to dealer seat
            int oldButton = table.getButton();
            table.setRemoteButton(d.dealerSeat());
            table.fireEvent(PokerTableEvent.TYPE_BUTTON_MOVED, oldButton);
            GameEventLog.log("NEW_HAND", table.getNumber());
            table.fireEvent(PokerTableEvent.TYPE_NEW_HAND);
            // Re-trigger card display: TYPE_NEW_HAND may reset card slots; this ensures
            // the local player's hole cards and opponents' face-down cards are rendered.
            table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION);
        });
    }

    private void onHoleCardsDealt(HoleCardsDealtData d) {
        logger.debug("[HOLE_CARDS_DEALT] cards={}", d.cards());
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table == null) {
                logger.debug("[HOLE_CARDS_DEALT EDT] currentTable=null, skipping");
                return;
            }
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null) {
                logger.debug("[HOLE_CARDS_DEALT EDT] remoteHand=null on table={}, skipping", table.getNumber());
                return;
            }

            // Hole cards are sent privately only to the card owner
            PokerPlayer localPlayer = findPlayer(localPlayerId_);
            if (localPlayer == null) {
                logger.debug("[HOLE_CARDS_DEALT EDT] localPlayer={} not found, skipping", localPlayerId_);
                return;
            }

            // Use newHand() to clear any cards already set from the GAME_STATE snapshot
            // (sent before HOLE_CARDS_DEALT) to avoid accumulating duplicate cards.
            Hand playerHand = localPlayer.newHand(Hand.TYPE_NORMAL);
            if (d.cards() == null)
                return;
            for (String c : d.cards()) {
                Card card = Card.getCard(c);
                if (card != null)
                    playerHand.addCard(card);
            }

            table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION);
        });
    }

    private void onCommunityCardsDealt(CommunityCardsDealtData d) {
        isPreFlop_ = false;
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = tables_.getOrDefault(d.tableId(), currentTable());
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            BettingRound round = BettingRound.valueOf(d.round());
            hand.updateRound(round);
            hand.updateCommunity(parseCards(d.allCommunityCards()));
            // New street — clear current-round bets so no stale chips are shown
            hand.clearBets();

            table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION, round.toLegacy());
        });
    }

    private void onActionRequired(ActionRequiredData d) {
        logger.debug("[ACTION_REQUIRED] timeout={}s canCheck={} canCall={} callAmt={} canBet={} canRaise={}",
                d.timeoutSeconds(), d.options() != null && d.options().canCheck(),
                d.options() != null && d.options().canCall(), d.options() != null ? d.options().callAmount() : 0,
                d.options() != null && d.options().canBet(), d.options() != null && d.options().canRaise());
        SwingUtilities.invokeLater(() -> {
            currentOptions_ = d.options();

            RemotePokerTable table = currentTable();
            if (table == null) {
                logger.debug("[ACTION_REQUIRED EDT] currentTable=null, cannot show action buttons");
                return;
            }
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null) {
                logger.debug("[ACTION_REQUIRED EDT] remoteHand=null on table={}, cannot show action buttons",
                        table.getNumber());
                return;
            }

            // The local player is the current actor
            int playerIndex = indexOfPlayer(hand, localPlayerId_);
            int oldPlayerIndex = hand.getCurrentPlayerIndex();
            logger.debug("[ACTION_REQUIRED EDT] localPlayer={} playerIndex={} in hand with {} players", localPlayerId_,
                    playerIndex, hand.getNumPlayers());
            hand.updateCurrentPlayer(playerIndex);

            // Store server-provided bet/raise amounts so the Swing UI can populate
            // the bet spinner and enable/disable the raise button correctly.
            if (d.options() != null) {
                hand.updateActionOptions(d.options());
            }

            // If the player pre-selected an advance action while another player was
            // acting, auto-submit it now instead of showing the action buttons.
            if (d.options() != null) {
                String[] advance = AdvanceAction.getAdvanceActionWS(d.options().canCheck(), d.options().canRaise(),
                        d.options().canAllIn(), d.options().allInAmount(), d.options().canBet(), d.options().maxBet(),
                        d.options().maxRaise());
                if (advance != null) {
                    logger.debug("[ACTION_REQUIRED EDT] firing pre-action advance={}", advance[0]);
                    game_.setInputMode(PokerTableInput.MODE_QUITSAVE);
                    wsClient_.sendAction(advance[0], Integer.parseInt(advance[1]));
                    return;
                }
            }

            // Determine input mode from server options and show action buttons.
            int inputMode = determineInputMode(d.options());
            PokerPlayer localPlayer = hand.getCurrentPlayer();
            // Set server-provided timeout on the player so CountdownPanel displays
            // the correct remaining time rather than reading an unset (0) value.
            if (localPlayer != null && d.timeoutSeconds() > 0) {
                localPlayer.setTimeoutMillis(d.timeoutSeconds() * 1000);
                localPlayer.setThinkBankMillis(0);
            }
            logger.debug("[ACTION_REQUIRED EDT] firing TYPE_CURRENT_PLAYER_CHANGED, then setInputMode mode={}",
                    inputMode);
            GameEventLog.log("CURRENT_PLAYER_CHANGED", table.getNumber());
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED, table,
                    oldPlayerIndex, playerIndex));
            game_.setInputMode(inputMode, hand, localPlayer);
            logger.debug("[WS-DIRECTOR] ACTION_REQUIRED applied inputMode={} options={}", inputMode, d.options());
        });
    }

    /**
     * Maps server-provided {@link ActionOptionsData} to the Swing input mode
     * constant that controls which action buttons are shown.
     */
    private int determineInputMode(ActionOptionsData opts) {
        if (opts == null)
            return PokerTableInput.MODE_QUITSAVE;
        if (opts.canCall())
            return PokerTableInput.MODE_CALL_RAISE;
        if (opts.canRaise())
            return PokerTableInput.MODE_CHECK_RAISE;
        if (opts.canBet())
            return PokerTableInput.MODE_CHECK_BET;
        return PokerTableInput.MODE_QUITSAVE;
    }

    private void onPlayerActed(PlayerActedData d) {
        final boolean preFlop = isPreFlop_;
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = tables_.getOrDefault(d.tableId(), currentTable());
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            // Record pre-flop action for opponent style tracking (skip blinds/antes)
            if (preFlop) {
                String actionStr = d.action();
                if (!"ANTE".equalsIgnoreCase(actionStr) && !"BLIND_SM".equalsIgnoreCase(actionStr)
                        && !"BLIND_BIG".equalsIgnoreCase(actionStr)) {
                    opponentTracker_.onPreFlopAction((int) d.playerId(), actionStr);
                }
            }

            // Update chip count and pot from server-provided post-action values
            PokerPlayer player = findPlayer(d.playerId());
            if (player != null) {
                // Server is authoritative — always apply the chip count from the server.
                // The previous guard (chipCount > 0 || isFold || isBlindAnte) missed
                // all-in BET/RAISE where the server correctly returns chipCount=0.
                player.setChipCount(d.chipCount());
                if ("FOLD".equalsIgnoreCase(d.action())) {
                    player.setFolded(true);
                }
            }
            hand.updatePot(d.potTotal());

            // Update the acting player's current-round bet
            if (player != null && d.totalBet() > 0) {
                hand.updatePlayerBet(player.getID(), d.totalBet());
            }

            // Advance current player (server will send ACTION_REQUIRED for next actor)
            hand.updateCurrentPlayer(HoldemHand.NO_CURRENT_PLAYER);

            if (player == null)
                return;
            int handAction = mapWsStringToAction(d.action());
            // For CALL actions, PlayerAction.call() always has amount=0 on the wire.
            // Use totalBet (player's cumulative bet this round after the call) as the
            // chat amount so the dealer log shows the correct call size.
            int chatAmount = (handAction == HandAction.ACTION_CALL) ? d.totalBet() : d.amount();
            HandAction action = new HandAction(player, hand.getRoundForDisplay(), handAction, chatAmount);
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ACTION, table, action));
            deliverChatLocal(PokerConstants.CHAT_2, action.getChat(0, null, null), PokerConstants.CHAT_DEALER_MSG_ID);
        });
    }

    private void onActionTimeout(ActionTimeoutData d) {
        SwingUtilities.invokeLater(() -> {
            // Notification-only: the subsequent PLAYER_ACTED message handles all
            // state updates (chip count, folded, pot, action chat). This handler:
            // 1. Clears the current player highlight
            // 2. Hides action buttons if the local player timed out
            // 3. Shows a "timed out" notification (not an action chat)
            RemotePokerTable table = tables_.getOrDefault(d.tableId(), currentTable());
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            hand.updateCurrentPlayer(HoldemHand.NO_CURRENT_PLAYER);

            if (d.playerId() == localPlayerId_) {
                game_.setInputMode(PokerTableInput.MODE_QUITSAVE);
            }

            PokerPlayer player = findPlayer(d.playerId());
            String name = player != null ? player.getName() : "Player " + d.playerId();
            deliverChatLocal(PokerConstants.CHAT_2, name + " timed out", PokerConstants.CHAT_DEALER_MSG_ID);
        });
    }

    private void onHandComplete(HandCompleteData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = tables_.getOrDefault(d.tableId(), currentTable());
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            // Reveal showdown cards.
            // Clear the hand first — the local player's hole cards were already set via
            // HOLE_CARDS_DEALT, so without clear() the server-provided showdown cards
            // would be added on top, producing duplicates.
            if (d.showdownPlayers() != null) {
                for (ShowdownPlayerData sp : d.showdownPlayers()) {
                    PokerPlayer player = findPlayer(sp.playerId());
                    if (player != null && !sp.cards().isEmpty()) {
                        Hand showHand = player.getHand();
                        showHand.clear();
                        for (String c : sp.cards()) {
                            Card card = Card.getCard(c);
                            if (card != null)
                                showHand.addCard(card);
                        }
                    }
                }
            }

            // Credit winners with their pot shares so chip counts are correct
            // before TYPE_END_HAND fires (without this, chip counts remain at
            // their post-betting values until the next GAME_STATE arrives).
            // Prefer absolute chip count from server when available to prevent drift.
            if (d.winners() != null) {
                for (WinnerData w : d.winners()) {
                    if (w.amount() > 0) {
                        PokerPlayer winner = findPlayer(w.playerId());
                        if (winner != null) {
                            if (w.chipCount() != null) {
                                winner.setChipCount(w.chipCount());
                            } else {
                                winner.setChipCount(winner.getChipCount() + w.amount());
                            }
                        }
                    }
                }
            }

            recordCompletedHandResult(table, hand, d);

            hand.updateCurrentPlayer(HoldemHand.NO_CURRENT_PLAYER);
            hand.updatePot(0);
            hand.clearBets();
            GameEventLog.log("END_HAND", table.getNumber());
            table.fireEvent(PokerTableEvent.TYPE_END_HAND);
            opponentTracker_.onHandComplete();
        });
    }

    private void onPotAwarded(PotAwardedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = tables_.getOrDefault(d.tableId(), currentTable());
            if (table == null)
                return;

            HandResultCapture capture = handResultCaptureByTable_.computeIfAbsent(table.getNumber(),
                    k -> new HandResultCapture());
            capture.recordPotAward(buildPotBreakdown(d));

            RemoteHoldemHand hand = table.getRemoteHand();

            // Record win(s) in hand history so displayShowdown() shows WIN/LOSE overlays
            // correctly. Without this, getWin() returns 0 for all players and everyone
            // shows as LOSE. Only advance to SHOWDOWN round when a real showdown occurred
            // (i.e. SHOWDOWN_STARTED was received); uncontested pots must not trigger it.
            if (hand != null && d.winnerIds() != null && d.winnerIds().length > 0) {
                int amountEach = d.amount() / d.winnerIds().length;
                for (long winnerId : d.winnerIds()) {
                    PokerPlayer winner = findPlayer(winnerId);
                    if (winner != null) {
                        hand.wins(winner, amountEach, d.potIndex());
                    }
                }
                if (showdownStarted_) {
                    hand.updateRound(BettingRound.SHOWDOWN);
                    table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION, BettingRound.SHOWDOWN.toLegacy());
                }
            }

            table.fireEvent(PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED);
        });
    }

    private void onShowdownStarted(ShowdownStartedData d) {
        SwingUtilities.invokeLater(() -> {
            showdownStarted_ = true;
            RemotePokerTable table = tables_.get(d.tableId());
            if (table == null)
                table = currentTable();
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            // Reveal opponent cards before firing TYPE_DEALER_ACTION so that
            // displayShowdown() sees them as exposed (isCardsExposed() == true).
            if (d.showdownPlayers() != null) {
                for (ShowdownPlayerData sp : d.showdownPlayers()) {
                    PokerPlayer player = findPlayer(sp.playerId());
                    if (player != null && !sp.cards().isEmpty()) {
                        Hand showHand = player.getHand();
                        showHand.clear();
                        for (String c : sp.cards()) {
                            Card card = Card.getCard(c);
                            if (card != null)
                                showHand.addCard(card);
                        }
                        player.setCardsExposed(true);
                    }
                }
            }

            hand.updateRound(BettingRound.SHOWDOWN);
            table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION, BettingRound.SHOWDOWN.toLegacy());
        });
    }

    private void onLevelChanged(LevelChangedData d) {
        SwingUtilities.invokeLater(() -> {
            int oldLevel = game_.getLevel();
            game_.setLevel(d.level());
            // Reset and restart the clock for the new level
            game_.getGameClock().setSecondsRemaining(game_.getSecondsInLevel(d.level()));
            if (!game_.getGameClock().isRunning()) {
                game_.getGameClock().start();
            }
            for (RemotePokerTable table : tables_.values()) {
                // Update blind amounts on the hand so UI and hand history show correct values
                RemoteHoldemHand hand = table.getRemoteHand();
                if (hand != null) {
                    hand.setSmallBlind(d.smallBlind());
                    hand.setBigBlind(d.bigBlind());
                    hand.setAnte(d.ante());
                }
                // Fire as a property change that ShowTournamentTable listens to
                GameEventLog.log("LEVEL_CHANGED", table.getNumber());
                table.fireEvent(PokerTableEvent.TYPE_LEVEL_CHANGED, oldLevel);
            }
        });
    }

    private void onPlayerEliminated(PlayerEliminatedData d) {
        SwingUtilities.invokeLater(() -> {
            for (RemotePokerTable table : tables_.values()) {
                int seat = findSeat(table, d.playerId());
                if (seat >= 0) {
                    PokerPlayer p = table.getPlayer(seat);
                    if (p != null) {
                        p.setPlace(d.finishPosition());
                        p.setChipCount(0);
                    }
                    table.clearSeat(seat);
                    GameEventLog.log("PLAYER_REMOVED", table.getNumber());
                    table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REMOVED, table, p, seat));
                    break;
                }
            }
            // Also update the canonical player in game_.players_ so GameOver and
            // ChipLeaderPanel read the correct place, prize, and chip count.
            PokerPlayer gamePlayer = resolveGamePlayer(d.playerId());
            if (gamePlayer != null) {
                game_.applyPlayerResult(gamePlayer.getID(), d.finishPosition());
            }
            // Chat notification for eliminations — more prominent for human players
            String name = d.playerName() != null && !d.playerName().isEmpty()
                    ? d.playerName()
                    : "Player " + d.playerId();
            String place = PropertyConfig.getPlace(d.finishPosition());
            String msg = name + " finished in " + place + " place";
            int chatType = d.isHuman() ? PokerConstants.CHAT_ALWAYS : PokerConstants.CHAT_2;
            deliverChatLocal(chatType, msg, PokerConstants.CHAT_DEALER_MSG_ID);
        });
    }

    private void onRebuyOffered(RebuyOfferedData d) {
        SwingUtilities.invokeLater(() -> {
            markRebuyOffer(d.cost(), d.chips(), d.timeoutSeconds());
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            PokerPlayer localPlayer = findPlayer(localPlayerId_);
            if (localPlayer == null)
                return;

            // Control-server path: bypass the Swing rebuy button and expose the
            // rebuy decision as MODE_REBUY_CHECK so the API can respond.
            NewLevelActions.RebuyDecisionProvider provider = NewLevelActions.rebuyDecisionProvider;
            if (provider != null) {
                // Set input mode synchronously on the EDT, then wait for the
                // decision on a background thread to avoid blocking the EDT.
                game_.setInputMode(PokerTableInput.MODE_REBUY_CHECK);
                declineScheduler_.execute(() -> {
                    boolean accepted = provider.waitForDecision(() -> {
                    }, 30);
                    SwingUtilities.invokeLater(() -> {
                        cancelPendingRebuyDecline();
                        markRebuyDecisionSent(accepted, d.cost(), d.chips(), false);
                        wsClient_.sendRebuyDecision(accepted);
                    });
                });
                return;
            }

            // Normal UI flow: enable the rebuy button in the table panel.
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REBUY, table, localPlayer,
                    d.cost(), d.chips(), true));

            // Schedule auto-decline if user doesn't click the rebuy button within
            // the timeout. Without this, the server's CompletableFuture blocks
            // indefinitely in practice mode (timeout=0 means 30s default here).
            int timeout = d.timeoutSeconds() > 0 ? d.timeoutSeconds() : 30;
            cancelPendingRebuyDecline();
            pendingRebuyDecline_ = declineScheduler_.schedule(() -> {
                logger.debug("[REBUY] auto-declining after {}s timeout", timeout);
                markRebuyDecisionSent(false, d.cost(), d.chips(), localPlayer.isInHand());
                wsClient_.sendRebuyDecision(false);
            }, timeout, TimeUnit.SECONDS);
        });
    }

    /**
     * Set by GameControlServer to intercept addon decisions via the control API.
     * When non-null, {@link #onAddonOffered} sets MODE_REBUY_CHECK on the EDT and
     * waits for the API caller to resolve on the declineScheduler background
     * thread.
     */
    public static volatile NewLevelActions.RebuyDecisionProvider addonDecisionProvider;

    private void onAddonOffered(AddonOfferedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            PokerPlayer localPlayer = findPlayer(localPlayerId_);
            if (localPlayer == null)
                return;

            // Control-server path: bypass the Swing addon button and expose the
            // addon decision as MODE_REBUY_CHECK so the API can respond.
            NewLevelActions.RebuyDecisionProvider provider = addonDecisionProvider;
            if (provider != null) {
                game_.setInputMode(PokerTableInput.MODE_REBUY_CHECK);
                declineScheduler_.execute(() -> {
                    boolean accepted = provider.waitForDecision(() -> {
                    }, 30);
                    SwingUtilities.invokeLater(() -> {
                        cancelPendingAddonDecline();
                        if (accepted) {
                            doAddon(localPlayer, d.cost(), d.chips());
                        } else {
                            wsClient_.sendAddonDecision(false);
                        }
                    });
                });
                return;
            }

            // Normal UI flow: enable the addon button in the table panel.
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ADDON, table, localPlayer,
                    d.cost(), d.chips(), true));

            // Schedule auto-decline if user doesn't click the addon button within
            // the timeout.
            int timeout = d.timeoutSeconds() > 0 ? d.timeoutSeconds() : 30;
            cancelPendingAddonDecline();
            pendingAddonDecline_ = declineScheduler_.schedule(() -> {
                logger.debug("[ADDON] auto-declining after {}s timeout", timeout);
                wsClient_.sendAddonDecision(false);
            }, timeout, TimeUnit.SECONDS);
        });
    }

    private void onGameComplete(GameCompleteData d) {
        SwingUtilities.invokeLater(() -> {
            PokerPlayer winnerGamePlayer = null;
            long winnerId = -1L;

            // Apply all final standings from the server, not just the winner.
            // This keeps place/prize data correct even when PLAYER_ELIMINATED is
            // delayed or the server/player ID mapping changed.
            Set<Integer> appliedResults = new HashSet<>();
            if (d.standings() != null) {
                for (ServerMessageData.StandingData standing : d.standings()) {
                    if (standing == null || standing.position() <= 0) {
                        continue;
                    }

                    PokerPlayer gamePlayer = resolveGamePlayer(standing.playerId());
                    if (gamePlayer == null && standing.playerName() != null && !standing.playerName().isBlank()) {
                        gamePlayer = findGamePlayerByName(standing.playerName());
                    }

                    if (gamePlayer != null && appliedResults.add(gamePlayer.getID())) {
                        game_.applyPlayerResult(gamePlayer.getID(), standing.position());
                    }

                    if (standing.position() == 1) {
                        winnerId = standing.playerId();
                        if (gamePlayer != null) {
                            winnerGamePlayer = gamePlayer;
                        }
                    }
                }
            }

            // Final fallback for winner mapping in case standings->player mapping failed.
            if (winnerGamePlayer == null && winnerId >= 0) {
                winnerGamePlayer = resolveGamePlayer(winnerId);
            }
            if (winnerGamePlayer == null) {
                winnerGamePlayer = findLikelyWinnerGamePlayer();
            }

            // Identify the winner from the server's standings (position=1).
            // Fall back to scanning the table for the surviving player with chips > 0.
            if (winnerId < 0) {
                outer : for (RemotePokerTable table : tables_.values()) {
                    for (int s = 0; s < PokerConstants.SEATS; s++) {
                        PokerPlayer p = table.getPlayer(s);
                        if (p != null && p.getChipCount() > 0) {
                            winnerId = p.getID();
                            break outer;
                        }
                    }
                }
            }
            if (winnerId >= 0) {
                // Update table player's place (for UI during GameOver transition).
                for (RemotePokerTable table : tables_.values()) {
                    int seat = findSeat(table, winnerId);
                    if (seat >= 0) {
                        PokerPlayer p = table.getPlayer(seat);
                        if (p != null)
                            p.setPlace(1);
                        table.clearSeat(seat);
                        break;
                    }
                }
                // Update game_.players_ so GameOver shows "You won!" and
                // ChipLeaderPanel displays the correct final standings.
                if (winnerGamePlayer != null && appliedResults.add(winnerGamePlayer.getID())) {
                    game_.applyPlayerResult(winnerGamePlayer.getID(), 1);
                }
            }
            if (context_ != null) {
                context_.processPhase("OnlineGameOver");
            }
        });
    }

    private PokerPlayer findGamePlayerByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (int i = 0; i < game_.getNumPlayers(); i++) {
            PokerPlayer p = game_.getPokerPlayerAt(i);
            if (p != null && name.equals(p.getName())) {
                return p;
            }
        }
        return null;
    }

    private PokerPlayer findLikelyWinnerGamePlayer() {
        PokerPlayer best = null;
        for (int i = 0; i < game_.getNumPlayers(); i++) {
            PokerPlayer p = game_.getPokerPlayerAt(i);
            if (p == null) {
                continue;
            }
            if (best == null || p.getChipCount() > best.getChipCount()) {
                best = p;
            }
        }
        return best;
    }

    private void onPlayerJoined(PlayerJoinedData d) {
        SwingUtilities.invokeLater(() -> {
            if (d.seatIndex() < 0 || d.seatIndex() >= PokerConstants.SEATS) {
                // Reconnect broadcast with unknown seat — nothing to seat, ignore.
                return;
            }
            // Route to the specific table if tableId is known; fall back to first table.
            RemotePokerTable table = d.tableId() >= 0 ? tables_.get(d.tableId()) : null;
            if (table == null && !tables_.isEmpty()) {
                table = tables_.values().iterator().next();
            }
            if (table == null) {
                return;
            }
            // During table consolidation, the player may still occupy their old seat
            // (if PLAYER_LEFT was suppressed on the server). Clear it from any table
            // before seating at the new location so they don't appear twice.
            for (RemotePokerTable t : tables_.values()) {
                int oldSeat = findSeat(t, d.playerId());
                if (oldSeat >= 0 && (t != table || oldSeat != d.seatIndex())) {
                    t.clearSeat(oldSeat);
                }
            }
            PokerPlayer existing = table.getPlayer(d.seatIndex());
            if (existing == null) {
                PokerPlayer p = new PokerPlayer((int) d.playerId(), d.playerName(), d.playerId() == localPlayerId_);
                table.setRemotePlayer(d.seatIndex(), p);
                table.firePokerTableEvent(
                        new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ADDED, table, p, d.seatIndex()));
            }
            // Keep currentTable in sync immediately so messages that follow
            // (before the next GAME_STATE) route to the right table.
            if (d.playerId() == localPlayerId_) {
                game_.setCurrentTable(table);
            }
            // Show a chat notification distinguishing reconnect from new join.
            if (d.playerId() != localPlayerId_ && d.playerName() != null && !d.playerName().isEmpty()) {
                String verb = d.isReconnect() ? "reconnected" : "joined";
                deliverChatLocal(PokerConstants.CHAT_2, d.playerName() + " " + verb, PokerConstants.CHAT_DEALER_MSG_ID);
            }
        });
    }

    private void onPlayerLeft(PlayerLeftData d) {
        SwingUtilities.invokeLater(() -> {
            for (RemotePokerTable table : tables_.values()) {
                int seat = findSeat(table, d.playerId());
                if (seat >= 0) {
                    PokerPlayer p = table.getPlayer(seat);
                    table.clearSeat(seat);
                    table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REMOVED, table, p, seat));
                    break;
                }
            }
        });
    }

    private void onPlayerMoved(ServerMessageData.PlayerMovedData d) {
        SwingUtilities.invokeLater(() -> {
            // Remove the player from the origin table and show a notification.
            // The subsequent PLAYER_JOINED will seat them at the destination table.
            for (RemotePokerTable table : tables_.values()) {
                int seat = findSeat(table, d.playerId());
                if (seat >= 0) {
                    PokerPlayer p = table.getPlayer(seat);
                    table.clearSeat(seat);
                    table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REMOVED, table, p, seat));
                    break;
                }
            }
            String name = d.playerName() != null && !d.playerName().isEmpty()
                    ? d.playerName()
                    : "Player " + d.playerId();
            deliverChatLocal(PokerConstants.CHAT_2, name + " moved to another table",
                    PokerConstants.CHAT_DEALER_MSG_ID);
        });
    }

    private void onPlayerDisconnected(PlayerDisconnectedData d) {
        SwingUtilities.invokeLater(() -> {
            PokerPlayer player = findPlayer(d.playerId());
            if (player != null) {
                // Mark as disconnected for UI display
                player.setWaiting(true);
            }
            RemotePokerTable table = currentTable();
            if (table != null) {
                table.fireEvent(PokerTableEvent.TYPE_PREFS_CHANGED);
            }
        });
    }

    private void onPlayerRebuy(PlayerRebuyData d) {
        SwingUtilities.invokeLater(() -> {
            markRebuyApplied(d.playerId(), d.addedChips());
            PokerPlayer player = findPlayer(d.playerId());
            if (player == null)
                return;
            // Prefer absolute chip count from server when available to prevent drift.
            if (d.chipCount() != null) {
                player.setChipCount(d.chipCount());
            } else {
                player.setChipCount(player.getChipCount() + d.addedChips());
            }
            RemotePokerTable table = currentTable();
            if (table != null) {
                table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REBUY, table, player, 0,
                        d.addedChips(), false));
            }
        });
    }

    private void onPlayerAddon(PlayerAddonData d) {
        SwingUtilities.invokeLater(() -> {
            PokerPlayer player = findPlayer(d.playerId());
            if (player == null)
                return;
            // Prefer absolute chip count from server when available to prevent drift.
            if (d.chipCount() != null) {
                player.setChipCount(d.chipCount());
            } else {
                player.setChipCount(player.getChipCount() + d.addedChips());
            }
            RemotePokerTable table = currentTable();
            if (table != null) {
                table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ADDON, table, player, 0,
                        d.addedChips(), false));
            }
        });
    }

    private void onGamePaused(GamePausedData d) {
        SwingUtilities.invokeLater(() -> {
            game_.getGameClock().pause();
            if (d.isBreak()) {
                String msg = d.breakDurationMinutes() != null && d.breakDurationMinutes() > 0
                        ? "Tournament break \u2014 resumes in " + d.breakDurationMinutes() + " minutes"
                        : "Tournament break";
                deliverChatLocal(PokerConstants.CHAT_ALWAYS, msg, PokerConstants.CHAT_DEALER_MSG_ID);
            } else {
                String who = d.pausedBy() != null && !d.pausedBy().isEmpty() ? d.pausedBy() : "Host";
                deliverChatLocal(PokerConstants.CHAT_2, who + " paused the game", PokerConstants.CHAT_DEALER_MSG_ID);
            }
            for (RemotePokerTable table : tables_.values()) {
                table.fireEvent(PokerTableEvent.TYPE_STATE_CHANGED);
            }
        });
    }

    private void onGameResumed(GameResumedData d) {
        SwingUtilities.invokeLater(() -> {
            game_.getGameClock().unpause();
            for (RemotePokerTable table : tables_.values()) {
                table.fireEvent(PokerTableEvent.TYPE_STATE_CHANGED);
            }
        });
    }

    private void onPlayerKicked(PlayerKickedData d) {
        SwingUtilities.invokeLater(() -> {
            for (RemotePokerTable table : tables_.values()) {
                int seat = findSeat(table, d.playerId());
                if (seat >= 0) {
                    PokerPlayer p = table.getPlayer(seat);
                    table.clearSeat(seat);
                    table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REMOVED, table, p, seat));
                    break;
                }
            }
        });
    }

    private void onChatMessage(ChatMessageData d) {
        if (chatHandler_ == null)
            return;
        // Chat is delivered via the existing OnlineMessage/ChatHandler pathway.
        // In M4 practice mode there are no other human players, so this is a no-op.
        // In M6 multiplayer the chatHandler_ is wired and deliverChatLocal routes to
        // the panel.
        deliverChatLocal(0, d.playerName() + ": " + d.message(), (int) d.playerId());
    }

    private void onTimerUpdate(TimerUpdateData d) {
        // Timer countdown — fire a neutral state-changed event so the UI can query
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table != null) {
                table.fireEvent(PokerTableEvent.TYPE_STATE_CHANGED);
            }
        });
    }

    private void onError(ErrorData d) {
        SwingUtilities.invokeLater(() -> {
            logger.error("Server error {}: {}", d.code(), d.message());
            deliverChatLocal(PokerConstants.CHAT_ALWAYS, "Server error: " + d.message(),
                    PokerConstants.CHAT_DEALER_MSG_ID);
        });
    }

    // -------------------------------------------------------------------------
    // Lobby message handlers
    // -------------------------------------------------------------------------

    private void onLobbyState(LobbyStateData d) {
        SwingUtilities.invokeLater(() -> {
            lobbyPlayers_.clear();
            if (d.players() != null) {
                lobbyPlayers_.addAll(d.players());
            }
            logger.debug("[LOBBY_STATE] gameId={} players={}", d.gameId(), lobbyPlayers_.size());
            fireStateChangedOnCurrentTable();
        });
    }

    private void onLobbyPlayerJoined(LobbyPlayerJoinedData d) {
        SwingUtilities.invokeLater(() -> {
            if (d.player() != null) {
                lobbyPlayers_.removeIf(p -> p.profileId() == d.player().profileId());
                lobbyPlayers_.add(d.player());
            }
            logger.debug("[LOBBY_PLAYER_JOINED] player={}", d.player() != null ? d.player().name() : null);
            fireStateChangedOnCurrentTable();
        });
    }

    private void onLobbyPlayerLeft(LobbyPlayerLeftData d) {
        SwingUtilities.invokeLater(() -> {
            if (d.player() != null) {
                lobbyPlayers_.removeIf(p -> p.profileId() == d.player().profileId());
            }
            logger.debug("[LOBBY_PLAYER_LEFT] player={}", d.player() != null ? d.player().name() : null);
            fireStateChangedOnCurrentTable();
        });
    }

    private void onLobbySettingsChanged(LobbySettingsChangedData d) {
        SwingUtilities.invokeLater(() -> {
            logger.debug("[LOBBY_SETTINGS_CHANGED] settings={}", d.updatedSettings());
            // Notify the UI so any settings display can refresh.
            fireStateChangedOnCurrentTable();
        });
    }

    private void onLobbyGameStarting(LobbyGameStartingData d) {
        SwingUtilities.invokeLater(() -> {
            logger.debug("[LOBBY_GAME_STARTING] startingInSeconds={}", d.startingInSeconds());
            String msg = d.startingInSeconds() > 0
                    ? "Game starting in " + d.startingInSeconds() + " seconds..."
                    : "Game starting...";
            deliverChatLocal(PokerConstants.CHAT_ALWAYS, msg, PokerConstants.CHAT_DEALER_MSG_ID);
            fireStateChangedOnCurrentTable();
        });
    }

    private void onLobbyPlayerKicked(LobbyPlayerKickedData d) {
        SwingUtilities.invokeLater(() -> {
            if (d.player() != null) {
                lobbyPlayers_.removeIf(p -> p.profileId() == d.player().profileId());
            }
            logger.debug("[LOBBY_PLAYER_KICKED] player={}", d.player() != null ? d.player().name() : null);
            fireStateChangedOnCurrentTable();
        });
    }

    private void onGameCancelled(GameCancelledData d) {
        SwingUtilities.invokeLater(() -> {
            logger.debug("[GAME_CANCELLED] reason={}", d.reason());
            fireStateChangedOnCurrentTable();
            if (context_ != null) {
                context_.processPhase("OnlineGameOver");
            }
        });
    }

    private void onChipsTransferred(ServerMessageData.ChipsTransferredData d) {
        SwingUtilities.invokeLater(() -> {
            PokerPlayer fromPlayer = findPlayer(d.fromPlayerId());
            PokerPlayer toPlayer = findPlayer(d.toPlayerId());
            // Prefer absolute chip counts from server when available to prevent drift.
            if (fromPlayer != null) {
                if (d.fromChipCount() != null) {
                    fromPlayer.setChipCount(d.fromChipCount());
                } else {
                    fromPlayer.setChipCount(fromPlayer.getChipCount() - d.amount());
                }
            }
            if (toPlayer != null) {
                if (d.toChipCount() != null) {
                    toPlayer.setChipCount(d.toChipCount());
                } else {
                    toPlayer.setChipCount(toPlayer.getChipCount() + d.amount());
                }
            }
            String msg = d.fromPlayerName() + " stakes " + d.amount() + " chips to keep you in the game.";
            deliverChatLocal(PokerConstants.CHAT_2, msg, PokerConstants.CHAT_DEALER_MSG_ID);
            RemotePokerTable table = currentTable();
            if (table != null) {
                table.fireEvent(PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED);
            }
        });
    }

    private void onNeverBrokeOffered(ServerMessageData.NeverBrokeOfferedData d) {
        // Read the current preference and auto-respond — no dialog needed.
        boolean accept = com.donohoedigital.games.poker.PokerUtils
                .isOptionOn(com.donohoedigital.games.poker.engine.PokerConstants.OPTION_CHEAT_NEVERBROKE);
        wsClient_.sendNeverBrokeDecision(accept);
        if (accept) {
            SwingUtilities.invokeLater(() -> deliverChatLocal(PokerConstants.CHAT_ALWAYS,
                    "Never-broke activated: chips restored", PokerConstants.CHAT_DEALER_MSG_ID));
        }
    }

    private void onColorUpStarted(ServerMessageData.ColorUpStartedData d) {
        SwingUtilities.invokeLater(() -> {
            String msg = "Chips are being colored up to " + d.newMinChip() + " chips.";
            deliverChatLocal(PokerConstants.CHAT_2, msg, PokerConstants.CHAT_DEALER_MSG_ID);
        });
    }

    private void onAiHoleCards(ServerMessageData.AiHoleCardsData d) {
        SwingUtilities.invokeLater(() -> {
            if (d.players() == null)
                return;
            for (ServerMessageData.AiPlayerCards pc : d.players()) {
                PokerPlayer player = findPlayer(pc.playerId());
                if (player == null || pc.cards() == null || pc.cards().isEmpty())
                    continue;
                Hand playerHand = player.getHand();
                if (playerHand == null) {
                    playerHand = player.newHand(Hand.TYPE_NORMAL);
                }
                playerHand.clear();
                for (String c : pc.cards()) {
                    Card card = Card.getCard(c);
                    if (card != null)
                        playerHand.addCard(card);
                }
            }
            RemotePokerTable table = currentTable();
            if (table != null) {
                table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION);
            }
        });
    }

    private void onPlayerSatOut(ServerMessageData.PlayerSatOutData d) {
        SwingUtilities.invokeLater(() -> {
            PokerPlayer player = resolveGamePlayer(d.playerId());
            if (player != null) {
                player.setSittingOut(true);
                player.fireSettingsChanged();
            }
            String name = d.playerName() != null && !d.playerName().isEmpty()
                    ? d.playerName()
                    : "Player " + d.playerId();
            deliverChatLocal(PokerConstants.CHAT_2, name + " is sitting out", PokerConstants.CHAT_DEALER_MSG_ID);
            fireStateChangedOnCurrentTable();
        });
    }

    private void onPlayerCameBack(ServerMessageData.PlayerCameBackData d) {
        SwingUtilities.invokeLater(() -> {
            PokerPlayer player = resolveGamePlayer(d.playerId());
            if (player != null) {
                player.setSittingOut(false);
                player.fireSettingsChanged();
            }
            String name = d.playerName() != null && !d.playerName().isEmpty()
                    ? d.playerName()
                    : "Player " + d.playerId();
            deliverChatLocal(PokerConstants.CHAT_2, name + " is back", PokerConstants.CHAT_DEALER_MSG_ID);
            fireStateChangedOnCurrentTable();
        });
    }

    private void onObserverJoined(ServerMessageData.ObserverJoinedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table != null) {
                String name = d.observerName() != null && !d.observerName().isEmpty()
                        ? d.observerName()
                        : "Observer " + d.observerId();
                deliverChatLocal(PokerConstants.CHAT_2, name + " is watching", PokerConstants.CHAT_DEALER_MSG_ID);
                table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_OBSERVER_ADDED, table, null, -1));
            }
        });
    }

    private void onObserverLeft(ServerMessageData.ObserverLeftData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table != null) {
                table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_OBSERVER_REMOVED, table, null, -1));
            }
        });
    }

    private void onColorUpCompleted(ServerMessageData.ColorUpCompletedData d) {
        SwingUtilities.invokeLater(() -> {
            deliverChatLocal(PokerConstants.CHAT_2, "Color-up complete.", PokerConstants.CHAT_DEALER_MSG_ID);
            fireStateChangedOnCurrentTable();
        });
    }

    private void onButtonMoved(ServerMessageData.ButtonMovedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = tables_.get(d.tableId());
            if (table == null)
                return;
            int oldButton = table.getButton();
            table.setRemoteButton(d.newSeat());
            table.firePokerTableEvent(
                    new PokerTableEvent(PokerTableEvent.TYPE_BUTTON_MOVED, table, oldButton, d.newSeat()));
        });
    }

    private void onCurrentPlayerChanged(ServerMessageData.CurrentPlayerChangedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = tables_.get(d.tableId());
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;
            // Find the index of the player in the hand's player order
            int newIndex = RemoteHoldemHand.NO_CURRENT_PLAYER;
            for (int i = 0; i < hand.getNumPlayers(); i++) {
                PokerPlayer p = hand.getPlayerAt(i);
                if (p != null && p.getID() == d.playerId()) {
                    newIndex = i;
                    break;
                }
            }
            int oldIndex = hand.getCurrentPlayerIndex();
            hand.updateCurrentPlayer(newIndex);
            table.firePokerTableEvent(
                    new PokerTableEvent(PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED, table, oldIndex, newIndex));
        });
    }

    private void onTableStateChanged(ServerMessageData.TableStateChangedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = tables_.get(d.tableId());
            if (table == null)
                return;
            table.fireEvent(PokerTableEvent.TYPE_STATE_CHANGED);
        });
    }

    private void onCleaningDone(ServerMessageData.CleaningDoneData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = tables_.get(d.tableId());
            if (table == null)
                return;
            table.fireEvent(PokerTableEvent.TYPE_CLEANING_DONE);
        });
    }

    private void onAdvisorUpdate(ServerMessageData.AdvisorData d) {
        SwingUtilities.invokeLater(() -> {
            AdvisorState.update(d.improvementOdds(), d.positivePotential(), d.negativePotential(), d.equity());
            PokerPlayer human = game_.getHumanPlayer();
            if (human != null) {
                human.setHandStrength((float) (d.equity() / 100.0));
                human.setHandPotential(d.positivePotential() != null ? d.positivePotential().floatValue() : -1f);
            }
            RemotePokerTable table = currentTable();
            if (table != null) {
                table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION);
            }
        });
    }

    private void onContinueRunout() {
        SwingUtilities.invokeLater(() -> {
            // Re-register the player action listener if it was cleared by Bet.finish() at
            // the end of the preceding betting round. Without this, ACTION_CONTINUE_LOWER
            // dispatched by the UI (or the dev control server) silently drops because
            // playerActionListener_ is null and the WebSocket never receives the
            // CONTINUE_RUNOUT signal to unblock the server's waitForContinue() future.
            //
            // This is a one-shot listener: it clears itself after firing so the next
            // Bet.process() can install its own listener without triggering the
            // "Attempt to replace existing listener" assertion.
            if (game_.getPlayerActionListener() == null) {
                game_.setPlayerActionListener((action, amount) -> {
                    game_.setPlayerActionListener(null); // one-shot — clear self after use
                    if (!wsClient_.isConnected()) {
                        logger.warn("[ACTION] not connected, dropping continue-lower action");
                        return;
                    }
                    game_.setInputMode(PokerTableInput.MODE_QUITSAVE);
                    wsClient_.sendContinueRunout();
                });
            }
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            PokerPlayer player = findPlayer(localPlayerId_);
            game_.setInputMode(PokerTableInput.MODE_CONTINUE_LOWER, hand, player);
        });
    }

    /** Fires TYPE_STATE_CHANGED on the current table if one exists. */
    private void fireStateChangedOnCurrentTable() {
        RemotePokerTable table = currentTable();
        if (table != null) {
            table.fireEvent(PokerTableEvent.TYPE_STATE_CHANGED);
        }
    }

    private static final class HandResultCapture {
        int handNumber_;
        final Map<Integer, StartPayoutSnapshot> startPayoutBySeat_ = new HashMap<>();
        final Map<Integer, HandResultLog.PotBreakdown> potBreakdownByIndex_ = new LinkedHashMap<>();

        private record StartPayoutSnapshot(long playerId, String name, boolean isHuman, int startChips) {
        }

        void resetForHand(int handNumber, Map<Integer, StartPayoutSnapshot> startPayoutBySeat) {
            handNumber_ = handNumber;
            startPayoutBySeat_.clear();
            startPayoutBySeat_.putAll(startPayoutBySeat);
            potBreakdownByIndex_.clear();
        }

        void recordPotAward(HandResultLog.PotBreakdown pot) {
            potBreakdownByIndex_.put(pot.potIndex(), pot);
        }

        List<HandResultLog.PotBreakdown> potBreakdown() {
            return new ArrayList<>(potBreakdownByIndex_.values());
        }
    }

    private Map<Integer, HandResultCapture.StartPayoutSnapshot> snapshotStartPayoutBySeat(RemotePokerTable table) {
        Map<Integer, HandResultCapture.StartPayoutSnapshot> start = new HashMap<>();
        for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
            PokerPlayer player = table.getPlayer(seat);
            if (player != null) {
                start.put(seat, new HandResultCapture.StartPayoutSnapshot(player.getID(), player.getName(),
                        player.isHuman(), player.getChipCount()));
            }
        }
        return start;
    }

    private HandResultLog.PotBreakdown buildPotBreakdown(PotAwardedData d) {
        List<HandResultLog.PotWinnerAward> winners = new ArrayList<>();
        long[] winnerIds = d.winnerIds() != null ? d.winnerIds() : new long[0];
        if (winnerIds.length > 0) {
            int baseShare = d.amount() / winnerIds.length;
            int remainder = d.amount() % winnerIds.length;
            for (int i = 0; i < winnerIds.length; i++) {
                long winnerId = winnerIds[i];
                PokerPlayer winner = findPlayer(winnerId);
                int seat = winner != null ? winner.getSeat() : -1;
                String name = winner != null ? winner.getName() : "player-" + winnerId;
                int amount = baseShare + (i == 0 ? remainder : 0);
                winners.add(new HandResultLog.PotWinnerAward(winnerId, seat, name, amount));
            }
        }
        return new HandResultLog.PotBreakdown(d.potIndex(), d.amount(), winners);
    }

    private void recordCompletedHandResult(RemotePokerTable table, RemoteHoldemHand hand, HandCompleteData d) {
        HandResultCapture capture = handResultCaptureByTable_.computeIfAbsent(table.getNumber(),
                k -> new HandResultCapture());

        int handNumber = d.handNumber() > 0 ? d.handNumber() : capture.handNumber_;
        List<HandResultLog.PotBreakdown> potBreakdown = capture.potBreakdown();

        Map<Long, Integer> amountWonByPlayer = new HashMap<>();
        for (HandResultLog.PotBreakdown pot : potBreakdown) {
            for (HandResultLog.PotWinnerAward winner : pot.winners()) {
                amountWonByPlayer.merge(winner.playerId(), winner.amount(), Integer::sum);
            }
        }
        if (amountWonByPlayer.isEmpty() && d.winners() != null) {
            for (WinnerData winnerData : d.winners()) {
                amountWonByPlayer.merge(winnerData.playerId(), winnerData.amount(), Integer::sum);
            }
        }

        LinkedHashSet<Long> winnerIds = new LinkedHashSet<>();
        if (d.winners() != null) {
            for (WinnerData winnerData : d.winners()) {
                winnerIds.add(winnerData.playerId());
            }
        }
        if (winnerIds.isEmpty()) {
            winnerIds.addAll(amountWonByPlayer.keySet());
        }

        List<HandResultLog.WinnerResult> winners = new ArrayList<>();
        for (Long winnerId : winnerIds) {
            PokerPlayer winner = findPlayer(winnerId);
            int seat = winner != null ? winner.getSeat() : -1;
            String name = winner != null ? winner.getName() : "player-" + winnerId;
            int amountWon = amountWonByPlayer.getOrDefault(winnerId, 0);

            String handClass = "UNKNOWN";
            String handDescription = "Unknown";
            if (winner != null && hand != null && winner.getHand() != null && winner.getHand().size() >= 2
                    && hand.getCommunity() != null && hand.getCommunity().size() >= 3) {
                try {
                    HandInfoFast fast = new HandInfoFast();
                    int score = fast.getScore(winner.getHandSorted(), hand.getCommunitySorted());
                    int handType = HandInfoFast.getTypeFromScore(score);
                    handClass = handClassName(handType);
                    handDescription = PropertyConfig.getMessage("msg.hand." + handType);
                } catch (Exception e) {
                    logger.debug("[HAND_RESULT] could not evaluate winner hand for {}", name, e);
                }
            }

            List<String> cards = winner != null ? cardsToStrings(winner.getHand()) : List.of();
            winners.add(
                    new HandResultLog.WinnerResult(winnerId, seat, name, handClass, handDescription, amountWon, cards));
        }

        List<HandResultLog.PayoutDelta> payoutDeltas = new ArrayList<>();
        LinkedHashSet<Integer> payoutSeats = new LinkedHashSet<>(capture.startPayoutBySeat_.keySet());
        for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
            PokerPlayer player = table.getPlayer(seat);
            if (player != null) {
                payoutSeats.add(seat);
            }
        }

        for (int seat : payoutSeats) {
            PokerPlayer player = table.getPlayer(seat);
            HandResultCapture.StartPayoutSnapshot start = capture.startPayoutBySeat_.get(seat);

            long playerId = player != null ? player.getID() : (start != null ? start.playerId() : -1L);
            String name = player != null ? player.getName() : (start != null ? start.name() : "seat-" + seat);
            boolean isHuman = player != null ? player.isHuman() : (start != null && start.isHuman());
            int startChips = start != null ? start.startChips() : (player != null ? player.getChipCount() : 0);
            int endChips = player != null ? player.getChipCount() : 0;

            payoutDeltas.add(new HandResultLog.PayoutDelta(playerId, seat, name, isHuman, startChips, endChips,
                    endChips - startChips));
        }

        HandResultLog.record(new HandResultLog.HandResult(System.currentTimeMillis(), table.getNumber(), handNumber,
                hand != null ? cardsToStrings(hand.getCommunity()) : List.of(), winners, potBreakdown, payoutDeltas));
    }

    private List<String> cardsToStrings(Hand hand) {
        if (hand == null || hand.size() == 0) {
            return List.of();
        }
        List<String> cards = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.getCard(i);
            if (card != null && !card.isBlank()) {
                cards.add(card.getDisplay());
            }
        }
        return cards;
    }

    private String handClassName(int handType) {
        return switch (handType) {
            case HandScoreConstants.ROYAL_FLUSH -> "ROYAL_FLUSH";
            case HandScoreConstants.STRAIGHT_FLUSH -> "STRAIGHT_FLUSH";
            case HandScoreConstants.QUADS -> "QUADS";
            case HandScoreConstants.FULL_HOUSE -> "FULL_HOUSE";
            case HandScoreConstants.FLUSH -> "FLUSH";
            case HandScoreConstants.STRAIGHT -> "STRAIGHT";
            case HandScoreConstants.TRIPS -> "TRIPS";
            case HandScoreConstants.TWO_PAIR -> "TWO_PAIR";
            case HandScoreConstants.PAIR -> "PAIR";
            case HandScoreConstants.HIGH_CARD -> "HIGH_CARD";
            default -> "UNKNOWN";
        };
    }

    // -------------------------------------------------------------------------
    // PokerDirector (GameManager + ChatManager + setPaused + playerUpdate)
    // -------------------------------------------------------------------------

    @Override
    public Object getSaveLockObject() {
        return new Object(); // saves handled by server-side event store
    }

    @Override
    public String getPhaseName() {
        return "WebSocketTournamentDirector";
    }

    @Override
    public void cleanup() {
        wsClient_.disconnect();
        if (context_ != null) {
            context_.restartNormal();
        }
    }

    @Override
    public void setPaused(boolean paused) {
        if (paused) {
            wsClient_.sendAdminPause();
        } else {
            wsClient_.sendAdminResume();
        }
    }

    @Override
    public void playerUpdate(PokerPlayer player, String settings) {
        if (player == null) {
            return;
        }
        // Detect sit-out state changes and send the appropriate WS message.
        if (player.isSittingOut() != lastSentSittingOut_) {
            lastSentSittingOut_ = player.isSittingOut();
            if (lastSentSittingOut_) {
                wsClient_.sendSitOut();
            } else {
                wsClient_.sendComeBack();
            }
        }
    }

    @Override
    public void kickPlayer(long playerId) {
        wsClient_.sendAdminKick(playerId);
    }

    @Override
    public void doHandAction(HandAction action, boolean bRemote) {
        String wsAction = mapActionToWsString(action.getAction());
        wsClient_.sendAction(wsAction, action.getAmount());
    }

    @Override
    public void setGameOver() {
        // Game-over is driven by the server's GAME_COMPLETE message; no client-side
        // state to update.
    }

    @Override
    public void doDeal(PokerTable table) {
        // Server controls dealing — no-op on the client side.
    }

    @Override
    public void doRebuy(PokerPlayer player, int nLevel, int nAmount, int nChips, boolean bPending) {
        cancelPendingRebuyDecline();
        markRebuyDecisionSent(true, nAmount, nChips, bPending);
        // Don't update chips locally — let the server's PLAYER_REBUY message be
        // the sole update path to avoid double-add when onPlayerRebuy() fires.
        wsClient_.sendRebuyDecision(true);
    }

    @Override
    public void doAddon(PokerPlayer player, int nAmount, int nChips) {
        cancelPendingAddonDecline();
        // Don't update chips locally — let the server's PLAYER_ADDON message be
        // the sole update path to avoid double-add when onPlayerAddon() fires.
        wsClient_.sendAddonDecision(true);
    }

    private void cancelPendingRebuyDecline() {
        ScheduledFuture<?> f = pendingRebuyDecline_;
        if (f != null) {
            f.cancel(false);
            pendingRebuyDecline_ = null;
        }
    }

    private void cancelPendingAddonDecline() {
        ScheduledFuture<?> f = pendingAddonDecline_;
        if (f != null) {
            f.cancel(false);
            pendingAddonDecline_ = null;
        }
    }

    private void resetRebuyObservability() {
        synchronized (rebuyObservabilityLock_) {
            rebuyOfferSequence_ = 0;
            rebuyDecisionSequence_ = 0;
            rebuyApplySequence_ = 0;
            rebuyState_ = "NONE";
            rebuyStateAtMs_ = 0;
            rebuyLastOfferCost_ = 0;
            rebuyLastOfferChips_ = 0;
            rebuyLastOfferTimeoutSeconds_ = 0;
            rebuyLastOfferAtMs_ = 0;
            rebuyLastDecision_ = "NONE";
            rebuyLastDecisionAtMs_ = 0;
            rebuyLastDecisionPending_ = false;
            rebuyAwaitingServerApply_ = false;
            rebuyLastAppliedPlayerId_ = -1;
            rebuyLastAppliedAddedChips_ = 0;
            rebuyLastAppliedAtMs_ = 0;
        }
    }

    private void markRebuyOffer(int cost, int chips, int timeoutSeconds) {
        synchronized (rebuyObservabilityLock_) {
            rebuyOfferSequence_++;
            rebuyState_ = "OFFERED";
            rebuyStateAtMs_ = System.currentTimeMillis();
            rebuyLastOfferCost_ = cost;
            rebuyLastOfferChips_ = chips;
            rebuyLastOfferTimeoutSeconds_ = timeoutSeconds;
            rebuyLastOfferAtMs_ = rebuyStateAtMs_;
            rebuyAwaitingServerApply_ = false;
        }
    }

    private void markRebuyDecisionSent(boolean accepted, int amount, int chips, boolean pending) {
        synchronized (rebuyObservabilityLock_) {
            rebuyDecisionSequence_++;
            rebuyLastDecision_ = accepted ? "ACCEPT" : "DECLINE";
            rebuyLastDecisionAtMs_ = System.currentTimeMillis();
            rebuyLastDecisionPending_ = pending;
            rebuyLastOfferCost_ = amount;
            rebuyLastOfferChips_ = chips;
            rebuyState_ = accepted ? "ACCEPT_SENT" : "DECLINE_SENT";
            rebuyStateAtMs_ = rebuyLastDecisionAtMs_;
            rebuyAwaitingServerApply_ = accepted;
        }
    }

    private void markRebuyApplied(long playerId, int addedChips) {
        synchronized (rebuyObservabilityLock_) {
            rebuyApplySequence_++;
            rebuyState_ = "APPLIED";
            rebuyStateAtMs_ = System.currentTimeMillis();
            rebuyAwaitingServerApply_ = false;
            rebuyLastAppliedPlayerId_ = playerId;
            rebuyLastAppliedAddedChips_ = addedChips;
            rebuyLastAppliedAtMs_ = rebuyStateAtMs_;
        }
    }

    private Map<String, Object> buildRebuyOutcomeSnapshot() {
        synchronized (rebuyObservabilityLock_) {
            Map<String, Object> rebuy = new LinkedHashMap<>();
            rebuy.put("state", rebuyState_);
            rebuy.put("stateAtMs", rebuyStateAtMs_);
            rebuy.put("offerSequence", rebuyOfferSequence_);
            rebuy.put("decisionSequence", rebuyDecisionSequence_);
            rebuy.put("applySequence", rebuyApplySequence_);
            rebuy.put("awaitingServerApply", rebuyAwaitingServerApply_);
            rebuy.put("lastOfferCost", rebuyLastOfferCost_);
            rebuy.put("lastOfferChips", rebuyLastOfferChips_);
            rebuy.put("lastOfferTimeoutSeconds", rebuyLastOfferTimeoutSeconds_);
            rebuy.put("lastOfferAtMs", rebuyLastOfferAtMs_);
            rebuy.put("lastDecision", rebuyLastDecision_);
            rebuy.put("lastDecisionAtMs", rebuyLastDecisionAtMs_);
            rebuy.put("lastDecisionPending", rebuyLastDecisionPending_);
            rebuy.put("lastAppliedPlayerId", rebuyLastAppliedPlayerId_);
            rebuy.put("lastAppliedAddedChips", rebuyLastAppliedAddedChips_);
            rebuy.put("lastAppliedAtMs", rebuyLastAppliedAtMs_);
            return rebuy;
        }
    }

    // -------------------------------------------------------------------------
    // ChatManager
    // -------------------------------------------------------------------------

    @Override
    public void sendChat(int nPlayerID, String sMessage) {
        wsClient_.sendChat(sMessage, false);
    }

    @Override
    public void sendChat(String sMessage, PokerTable table, String sTestData) {
        wsClient_.sendChat(sMessage, table != null);
    }

    @Override
    public void sendDirectorChat(String sMessage, Boolean bPauseClock) {
        wsClient_.sendChat(sMessage, false);
    }

    @Override
    public void setChatHandler(ChatHandler chat) {
        this.chatHandler_ = chat;
    }

    @Override
    public void deliverChatLocal(int nType, String sMessage, int id) {
        if (chatHandler_ != null) {
            chatHandler_.chatReceived(id, nType, sMessage);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns the table containing the local player, or the first table. */
    private RemotePokerTable currentTable() {
        if (tables_.isEmpty())
            return null;
        // Prefer the game's current table if it is one of ours
        PokerTable current = game_.getCurrentTable();
        if (current instanceof RemotePokerTable rpt && tables_.containsValue(rpt)) {
            return rpt;
        }
        return tables_.values().iterator().next();
    }

    /**
     * Selects the table containing the local player as the game's current table.
     */
    private void selectLocalPlayerTable() {
        for (RemotePokerTable table : tables_.values()) {
            int seat = findSeat(table, localPlayerId_);
            if (seat >= 0) {
                logger.debug("[selectLocalPlayerTable] localPlayer={} found at table={} seat={}", localPlayerId_,
                        table.getNumber(), seat);
                game_.setCurrentTable(table);
                return;
            }
        }
        // Fall back to first table
        if (!tables_.isEmpty()) {
            RemotePokerTable first = tables_.values().iterator().next();
            logger.debug("[selectLocalPlayerTable] localPlayer={} not found in any table, falling back to table={}",
                    localPlayerId_, first.getNumber());
            game_.setCurrentTable(first);
        } else {
            logger.debug("[selectLocalPlayerTable] no tables available");
        }
    }

    /**
     * Applies a full {@link TableData} snapshot to a {@link RemotePokerTable}.
     * Rebuilds the player array, button position, community cards, and pot total.
     */
    private void applyTableData(RemotePokerTable table, TableData td) {
        logger.debug("[applyTableData] table={} seats={} round={} community={} pots={}", td.tableId(),
                td.seats() != null ? td.seats().size() : 0, td.currentRound(), td.communityCards(), td.pots());
        PokerPlayer[] players = new PokerPlayer[PokerConstants.SEATS];
        int dealerSeat = PokerTable.NO_SEAT;
        int sbSeat = PokerTable.NO_SEAT;
        int bbSeat = PokerTable.NO_SEAT;
        boolean localPlayerHasCards = false;

        if (td.seats() != null)
            for (SeatData sd : td.seats()) {
                if (sd.seatIndex() < 0 || sd.seatIndex() >= PokerConstants.SEATS)
                    continue;
                // PokerPlayer.getID() is int; server IDs are long. Safe for embedded mode
                // where IDs are small sequential values. Will need revisiting if IDs exceed
                // Integer.MAX_VALUE (e.g., high-volume server deployment in M6+).
                // For the local player, pass the engine's player ID as the key so
                // PokerPlayer.isLocallyControlled() returns true and hole cards render face-up.
                // Guard against null GameEngine (e.g., in unit tests where GameEngine is not
                // initialized by the test harness).
                GameEngine ge = GameEngine.getGameEngine();
                String playerKey = (sd.playerId() == localPlayerId_ && ge != null) ? ge.getPlayerId() : null;
                PokerPlayer p = new PokerPlayer(playerKey, (int) sd.playerId(), sd.playerName(),
                        sd.playerId() == localPlayerId_);
                p.setChipCount(sd.chipCount());
                // Apply folded status from snapshot (all-in is derived from chipCount == 0)
                if ("FOLDED".equals(sd.status())) {
                    p.setFolded(true);
                }
                // Apply hole cards from snapshot
                if (sd.holeCards() != null && !sd.holeCards().isEmpty()) {
                    // Local player: actual cards (face-up)
                    logger.debug("[applyTableData] player={} seat={} holeCards={}", sd.playerName(), sd.seatIndex(),
                            sd.holeCards());
                    for (String c : sd.holeCards()) {
                        Card card = Card.getCard(c);
                        if (card != null)
                            p.getHand().addCard(card);
                    }
                    if (sd.playerId() == localPlayerId_) {
                        localPlayerHasCards = true;
                    }
                } else if (sd.playerId() != localPlayerId_ && !"FOLDED".equals(sd.status())
                        && !"SITTING_OUT".equals(sd.status())) {
                    // Opponent in hand: add blank cards so face-down images are rendered
                    p.getHand().addCard(Card.BLANK);
                    p.getHand().addCard(Card.BLANK);
                }
                logger.debug("[applyTableData] seat={} player={} chips={} status={} isLocal={} holeCards={}",
                        sd.seatIndex(), sd.playerName(), sd.chipCount(), sd.status(), sd.playerId() == localPlayerId_,
                        sd.holeCards() != null ? sd.holeCards().size() : 0);
                players[sd.seatIndex()] = p;
                if (sd.isDealer())
                    dealerSeat = sd.seatIndex();
                if (sd.isSmallBlind())
                    sbSeat = sd.seatIndex();
                if (sd.isBigBlind())
                    bbSeat = sd.seatIndex();
            }

        // Only update the dealer button if the snapshot explicitly identifies one.
        // If no seat has isDealer=true (e.g., between hands after a player is
        // eliminated), preserve the current button so it doesn't jump to NO_SEAT
        // and then back to the new dealer on the next HAND_STARTED.
        int buttonToApply = dealerSeat != PokerTable.NO_SEAT ? dealerSeat : table.getButton();
        table.updateFromState(players, buttonToApply);

        // Rebuild the current hand from the table snapshot
        if (td.currentRound() != null) {
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null) {
                hand = new RemoteHoldemHand();
                table.setRemoteHand(hand);
                logger.debug("[applyTableData] created new RemoteHoldemHand for table={}", td.tableId());
            }
            BettingRound round = parseBettingRound(td.currentRound());
            hand.updateRound(round);

            hand.updateCommunity(td.communityCards() != null && !td.communityCards().isEmpty()
                    ? parseCards(td.communityCards())
                    : new Hand());

            int potTotal = td.pots() == null ? 0 : td.pots().stream().mapToInt(PotData::amount).sum();
            hand.updatePot(potTotal);

            hand.updateSmallBlindSeat(sbSeat);
            hand.updateBigBlindSeat(bbSeat);

            List<PokerPlayer> handPlayers = seatedPlayersFrom(table);
            hand.updatePlayerOrder(handPlayers);

            // Apply per-player current-round bets from snapshot
            hand.clearBets();
            if (td.seats() != null)
                for (SeatData sd : td.seats()) {
                    if (sd.currentBet() > 0) {
                        hand.updatePlayerBet((int) sd.playerId(), sd.currentBet());
                    }
                }

            logger.debug("[applyTableData] hand ready: round={} potTotal={} handPlayers={}", round, potTotal,
                    handPlayers.size());
        }

        // Fire table-level events to refresh the UI
        logger.debug("[applyTableData] firing TYPE_NEW_PLAYERS_LOADED");
        table.fireEvent(PokerTableEvent.TYPE_NEW_PLAYERS_LOADED);

        if (td.currentRound() != null) {
            // Simulate HAND_STARTED: put the table into "hand in progress" mode so the
            // action-button area and card areas become visible.
            logger.debug("[applyTableData] firing TYPE_NEW_HAND (hand in progress)");
            table.fireEvent(PokerTableEvent.TYPE_NEW_HAND);

            // Simulate HOLE_CARDS_DEALT: trigger card rendering for any hole cards that
            // were loaded from the snapshot into the local player's Hand object.
            logger.debug("[applyTableData] firing TYPE_DEALER_ACTION (localPlayerHasCards={})", localPlayerHasCards);
            table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION);
        }
    }

    /** Finds a player by server-assigned ID across all seated tables. */
    private PokerPlayer findPlayer(long playerId) {
        for (RemotePokerTable table : tables_.values()) {
            for (int s = 0; s < PokerConstants.SEATS; s++) {
                PokerPlayer p = table.getPlayer(s);
                if (p != null && p.getID() == (int) playerId)
                    return p;
            }
        }
        return null;
    }

    /** Finds the seat index of a player by ID in a specific table. */
    private int findSeat(RemotePokerTable table, long playerId) {
        for (int s = 0; s < PokerConstants.SEATS; s++) {
            PokerPlayer p = table.getPlayer(s);
            if (p != null && p.getID() == (int) playerId)
                return s;
        }
        return -1;
    }

    /**
     * Resolves a server-assigned player ID to the corresponding canonical
     * {@link PokerPlayer} object in {@code game_.players_}. Builds the mapping
     * lazily on first call.
     *
     * <p>
     * In practice games the embedded server uses the JWT {@code profileId} for the
     * human player and sequential negative IDs {@code -1, -2, ...} for AI players.
     * These differ from the client-assigned sequential IDs {@code 0, 1, 2, ...} in
     * {@code game_.players_}, so a direct ID lookup fails. This method bridges the
     * two ID spaces.
     */
    private PokerPlayer resolveGamePlayer(long serverPlayerId) {
        if (serverIdToGamePlayer_.isEmpty()) {
            buildServerIdToGamePlayerMap();
        }
        return serverIdToGamePlayer_.get(serverPlayerId);
    }

    /**
     * Builds the server-ID → game-player map from the current
     * {@code game_.players_} list. The human player is always at index 0 (client ID
     * = {@code PLAYER_ID_HOST}); AI players follow at indices 1, 2, ... matching
     * the server's sequential negative IDs {@code -1, -2, ...}.
     */
    private void buildServerIdToGamePlayerMap() {
        // Human: server ID = localPlayerId_ → client ID = PLAYER_ID_HOST (0)
        if (localPlayerId_ != -1L) {
            PokerPlayer human = game_.getPokerPlayerFromID(PokerConstants.PLAYER_ID_HOST);
            if (human != null) {
                serverIdToGamePlayer_.put(localPlayerId_, human);
            }
        }
        // AI players: server ID -1 → game_.players_[1], -2 → [2], ...
        for (int i = 1; i < game_.getNumPlayers(); i++) {
            PokerPlayer ai = game_.getPokerPlayerAt(i);
            if (ai != null) {
                serverIdToGamePlayer_.put(-(long) i, ai);
            }
        }
    }

    /** Returns the index of a player (by ID) in the hand's player-order list. */
    private int indexOfPlayer(RemoteHoldemHand hand, long playerId) {
        for (int i = 0; i < hand.getNumPlayers(); i++) {
            PokerPlayer p = hand.getPlayerAt(i);
            if (p != null && p.getID() == (int) playerId)
                return i;
        }
        return HoldemHand.NO_CURRENT_PLAYER;
    }

    /** Returns all non-null seated players in seat order. */
    private List<PokerPlayer> seatedPlayersFrom(RemotePokerTable table) {
        List<PokerPlayer> list = new ArrayList<>();
        for (int s = 0; s < PokerConstants.SEATS; s++) {
            PokerPlayer p = table.getPlayer(s);
            if (p != null)
                list.add(p);
        }
        return list;
    }

    /** Parses a list of card strings ("Ah", "Kd", etc.) into a {@link Hand}. */
    private Hand parseCards(List<String> cards) {
        Hand hand = new Hand();
        if (cards == null)
            return hand;
        for (String c : cards) {
            Card card = Card.getCard(c);
            if (card != null)
                hand.addCard(card);
        }
        return hand;
    }

    /**
     * Converts a round string from the server ("PRE_FLOP", "FLOP", etc.) to a
     * BettingRound.
     */
    private BettingRound parseBettingRound(String round) {
        if (round == null)
            return BettingRound.PRE_FLOP;
        try {
            return BettingRound.valueOf(round);
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown round '{}', defaulting to PRE_FLOP", round);
            return BettingRound.PRE_FLOP;
        }
    }

    /** Maps a {@link HandAction} action integer to the wire-format string. */
    private String mapActionToWsString(int action) {
        return switch (action) {
            case HandAction.ACTION_FOLD -> "FOLD";
            case HandAction.ACTION_CHECK, HandAction.ACTION_CHECK_RAISE -> "CHECK";
            case HandAction.ACTION_CALL -> "CALL";
            case HandAction.ACTION_BET -> "BET";
            case HandAction.ACTION_RAISE -> "RAISE";
            default -> {
                logger.warn("[mapActionToWsString] unknown action={}, defaulting to FOLD", action);
                yield "FOLD";
            }
        };
    }

    /**
     * Maps a {@link PokerGame} action integer (from PlayerActionListener /
     * playerActionPerformed) to the wire-format string.
     *
     * <p>
     * PokerGame.ACTION_* constants differ from HandAction.ACTION_* constants (e.g.
     * PokerGame.ACTION_FOLD=1 vs HandAction.ACTION_FOLD=0), so a separate mapping
     * is required for actions originating from the Swing game engine.
     */
    private String mapPokerGameActionToWsString(int action) {
        return switch (action) {
            case PokerGame.ACTION_FOLD -> "FOLD";
            case PokerGame.ACTION_CHECK -> "CHECK";
            case PokerGame.ACTION_CALL -> "CALL";
            case PokerGame.ACTION_BET -> "BET";
            case PokerGame.ACTION_RAISE -> "RAISE";
            case PokerGame.ACTION_ALL_IN -> "ALL_IN";
            default -> {
                logger.warn("[mapPokerGameActionToWsString] unknown action={}, defaulting to FOLD", action);
                yield "FOLD";
            }
        };
    }

    /** Maps a wire-format action string to a {@link HandAction} action integer. */
    private int mapWsStringToAction(String action) {
        if (action == null)
            return HandAction.ACTION_FOLD;
        return switch (action.toUpperCase()) {
            case "FOLD" -> HandAction.ACTION_FOLD;
            case "CHECK" -> HandAction.ACTION_CHECK;
            case "CALL" -> HandAction.ACTION_CALL;
            case "BET" -> HandAction.ACTION_BET;
            case "RAISE" -> HandAction.ACTION_RAISE;
            case "ALL_IN" -> HandAction.ACTION_BET; // map ALL_IN to BET for display
            case "BLIND_SM" -> HandAction.ACTION_BLIND_SM;
            case "BLIND_BIG" -> HandAction.ACTION_BLIND_BIG;
            case "ANTE" -> HandAction.ACTION_ANTE;
            default -> HandAction.ACTION_FOLD;
        };
    }

    /** Returns the last {@link ActionOptionsData} received from the server. */
    public ActionOptionsData getCurrentOptions() {
        return currentOptions_;
    }

    /**
     * Resolves the wire amount for a player action. For ALL_IN, the amount is
     * ignored because the server resolves it from pending action options.
     * Package-private for testing.
     */
    int resolveActionAmount(int action, int uiAmount) {
        if (action == PokerGame.ACTION_ALL_IN) {
            return 0; // server resolves ALL_IN amount from pending options
        }
        return uiAmount;
    }

    /**
     * Returns the wire-format action string for ACTION_ALL_IN given the current
     * server options. Package-private for testing.
     */
    String allInWsActionForTest() {
        return mapPokerGameActionToWsString(PokerGame.ACTION_ALL_IN);
    }

    /** Deserializes a {@link JsonNode} into the target data class. */
    private <T extends ServerMessageData> T parse(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse " + type.getSimpleName(), e);
        }
    }
}
