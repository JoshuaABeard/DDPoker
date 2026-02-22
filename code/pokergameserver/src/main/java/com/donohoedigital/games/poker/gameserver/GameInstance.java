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

import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.core.PlayerActionProvider;
import com.donohoedigital.games.poker.core.TournamentEngine;
import com.donohoedigital.games.poker.gameserver.GameConfig.BlindLevel;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import com.donohoedigital.games.poker.gameserver.dto.LobbyPlayerInfo;

/**
 * Encapsulates one running game. Owns the ServerTournamentDirector, player
 * sessions, and game state. This is the primary unit of game hosting.
 *
 * <p>
 * Thread-safe for concurrent access from WebSocket handlers and game director
 * thread.
 */
public class GameInstance {

    private static final Logger logger = LoggerFactory.getLogger(GameInstance.class);

    private final String gameId;
    private final long ownerProfileId;
    private final GameConfig config;
    private final GameServerProperties properties;

    // Game state
    private volatile GameInstanceState state;
    private ServerTournamentContext tournament;
    private ServerTournamentDirector director;
    private ServerPlayerActionProvider actionProvider;
    private ServerGameEventBus eventBus;
    private IGameEventStore eventStore;

    // Player tracking
    private final Map<Long, ServerPlayerSession> playerSessions = new ConcurrentHashMap<>();
    private final ReentrantLock stateLock = new ReentrantLock();

    // Threading
    private Future<?> directorFuture;

    // Completion tracking
    private volatile Instant completedAt;

    // Pending rebuy/addon decisions (playerId → future awaiting client response)
    private final Map<Integer, CompletableFuture<Boolean>> pendingRebuys = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<Boolean>> pendingAddons = new ConcurrentHashMap<>();

    // Private constructor - use create() factory method
    private GameInstance(String gameId, long ownerProfileId, GameConfig config, GameServerProperties properties) {
        this.gameId = gameId;
        this.ownerProfileId = ownerProfileId;
        this.config = config;
        this.properties = properties;
        this.state = GameInstanceState.CREATED;
    }

    /**
     * Create a new GameInstance.
     *
     * @param gameId
     *            unique game identifier
     * @param ownerProfileId
     *            profile ID of the game owner
     * @param config
     *            game configuration
     * @param properties
     *            server properties
     * @return new GameInstance in CREATED state
     */
    public static GameInstance create(String gameId, long ownerProfileId, GameConfig config,
            GameServerProperties properties) {
        return new GameInstance(gameId, ownerProfileId, config, properties);
    }

    // ====================================
    // State Transitions
    // ====================================

    /** Transition from CREATED to WAITING_FOR_PLAYERS */
    public void transitionToWaitingForPlayers() {
        stateLock.lock();
        try {
            if (state != GameInstanceState.CREATED) {
                throw new IllegalStateException("Can only transition to WAITING_FOR_PLAYERS from CREATED state");
            }
            state = GameInstanceState.WAITING_FOR_PLAYERS;
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Pre-creates the event bus so that callers can wire broadcast callbacks before
     * the director starts. Must be called while the game is in
     * {@link GameInstanceState#WAITING_FOR_PLAYERS}.
     *
     * <p>
     * Calling this is optional; if not called before {@link #start}, {@code start}
     * creates the event bus itself. If called, {@code start} reuses the pre-created
     * bus so that no events are missed.
     *
     * @return the newly created (or already existing) event bus
     */
    public ServerGameEventBus prepareStart() {
        stateLock.lock();
        try {
            if (state != GameInstanceState.WAITING_FOR_PLAYERS) {
                throw new IllegalStateException("Cannot prepareStart in state: " + state);
            }
            if (eventBus == null) {
                eventStore = new InMemoryGameEventStore(gameId);
                eventBus = new ServerGameEventBus(eventStore);
            }
            return eventBus;
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Start the game (WAITING_FOR_PLAYERS → IN_PROGRESS). Creates tournament
     * context and starts director thread.
     */
    public void start(ExecutorService executor) {
        stateLock.lock();
        try {
            if (state != GameInstanceState.WAITING_FOR_PLAYERS) {
                throw new IllegalStateException("Cannot start game in state: " + state);
            }
            logger.debug("[GameInstance] start() gameId={} players={} startingChips={} blindLevels={}", gameId,
                    playerSessions.size(), config.startingChips(),
                    config.blindStructure() != null ? config.blindStructure().size() : 0);

            // Create player list from sessions
            int startingChips = config.startingChips();
            List<ServerPlayer> players = new ArrayList<>();
            for (ServerPlayerSession session : playerSessions.values()) {
                ServerPlayer player = new ServerPlayer(toIntId(session.getProfileId()), session.getPlayerName(),
                        !session.isAI(), session.getSkillLevel(), startingChips);
                players.add(player);
            }

            // Reuse pre-created event bus (from prepareStart) if available, otherwise
            // create now. This avoids a race where a broadcast callback wired after
            // start() could miss events fired by the director's first hand.
            if (eventBus == null) {
                eventStore = new InMemoryGameEventStore(gameId);
                eventBus = new ServerGameEventBus(eventStore);
            }

            PlayerActionProvider aiProvider = createSimpleAI();
            int aiDelayMs = config.practiceConfig() != null && config.practiceConfig().aiActionDelayMs() != null
                    ? config.practiceConfig().aiActionDelayMs()
                    : properties.aiActionDelayMs();
            actionProvider = new ServerPlayerActionProvider(aiProvider, this::onActionRequest,
                    properties.actionTimeoutSeconds(), properties.disconnectGraceTurns(), playerSessions, aiDelayMs,
                    eventBus::publish);

            // Determine number of tables (1 table per 10 players, minimum 1)
            int numTables = Math.max(1, (players.size() + 9) / 10);

            // Extract game configuration
            List<BlindLevel> blindStructure = config.blindStructure();
            int numLevels = blindStructure.size();
            int[] smallBlinds = new int[numLevels];
            int[] bigBlinds = new int[numLevels];
            int[] antes = new int[numLevels];
            int[] levelMinutes = new int[numLevels];
            boolean[] breakLevels = new boolean[numLevels];

            for (int i = 0; i < numLevels; i++) {
                BlindLevel level = blindStructure.get(i);
                smallBlinds[i] = level.smallBlind();
                bigBlinds[i] = level.bigBlind();
                antes[i] = level.ante();
                levelMinutes[i] = level.minutes();
                breakLevels[i] = level.isBreak();
            }

            // Get level advancement configuration
            com.donohoedigital.games.poker.model.LevelAdvanceMode levelAdvanceMode = config
                    .levelAdvanceMode() == GameConfig.LevelAdvanceMode.TIME
                            ? com.donohoedigital.games.poker.model.LevelAdvanceMode.TIME
                            : com.donohoedigital.games.poker.model.LevelAdvanceMode.HANDS;
            int handsPerLevel = config.handsPerLevel();

            // Extract rebuy/addon configuration
            int maxRebuys = config.rebuys() != null && config.rebuys().enabled() ? config.rebuys().maxRebuys() : 0;
            int lastRebuyLevel = config.rebuys() != null && config.rebuys().enabled() ? config.rebuys().lastLevel() : 0;
            boolean addons = config.addons() != null && config.addons().enabled();

            tournament = new ServerTournamentContext(players, numTables, startingChips, smallBlinds, bigBlinds, antes,
                    levelMinutes, breakLevels, false, // practice mode
                    maxRebuys, lastRebuyLevel, addons, properties.actionTimeoutSeconds(), levelAdvanceMode,
                    handsPerLevel);

            // Wire rebuy/addon cost configuration into tournament context
            if (config.rebuys() != null && config.rebuys().enabled()) {
                int addonCost = config.addons() != null && config.addons().enabled() ? config.addons().cost() : 0;
                int addonChips = config.addons() != null && config.addons().enabled() ? config.addons().chips() : 0;
                int addonLevel = config.addons() != null && config.addons().enabled() ? config.addons().level() : -1;
                tournament.setRebuyAddonConfig(config.rebuys().cost(), config.rebuys().chips(), addonCost, addonChips,
                        addonLevel);
            }

            TournamentEngine engine = new TournamentEngine(eventBus, actionProvider);
            director = new ServerTournamentDirector(engine, tournament, eventBus, actionProvider, properties,
                    this::onLifecycleEvent, this::offerRebuy, this::offerAddon);

            if (config.practiceConfig() != null) {
                GameConfig.PracticeConfig pc = config.practiceConfig();
                if (pc.handResultPauseMs() != null)
                    director.setHandResultPauseMs(pc.handResultPauseMs());
                if (pc.allInRunoutPauseMs() != null)
                    director.setAllInRunoutPauseMs(pc.allInRunoutPauseMs());
                if (Boolean.FALSE.equals(pc.zipModeEnabled()))
                    director.setAutoZipEnabled(false);
                if (pc.neverBroke() != null)
                    director.setPracticeConfig(pc);
            }

            state = GameInstanceState.IN_PROGRESS;
            directorFuture = executor.submit(director);

        } finally {
            stateLock.unlock();
        }
    }

    /** Start the game with owner authorization check */
    public void startAsUser(long userId, ExecutorService executor) {
        checkOwnership(userId);
        start(executor);
    }

    /** Pause the game */
    public void pause() {
        stateLock.lock();
        try {
            if (state != GameInstanceState.IN_PROGRESS) {
                throw new IllegalStateException("Cannot pause game in state: " + state);
            }
            if (director != null) {
                director.pause();
            }
            state = GameInstanceState.PAUSED;
        } finally {
            stateLock.unlock();
        }
    }

    /** Pause the game with owner authorization check */
    public void pauseAsUser(long userId) {
        checkOwnership(userId);
        pause();
    }

    /** Resume the game from paused state */
    public void resume() {
        stateLock.lock();
        try {
            if (state != GameInstanceState.PAUSED) {
                throw new IllegalStateException("Cannot resume game in state: " + state);
            }
            if (director != null) {
                director.resume();
            }
            state = GameInstanceState.IN_PROGRESS;
        } finally {
            stateLock.unlock();
        }
    }

    /** Resume the game with owner authorization check */
    public void resumeAsUser(long userId) {
        checkOwnership(userId);
        resume();
    }

    /** Shutdown the game (triggers graceful completion) */
    public void shutdown() {
        stateLock.lock();
        try {
            if (director != null) {
                director.shutdown();
            }
        } finally {
            stateLock.unlock();
        }
    }

    /** Cancel the game */
    public void cancel() {
        stateLock.lock();
        try {
            if (state == GameInstanceState.COMPLETED || state == GameInstanceState.CANCELLED) {
                return; // Already finished
            }
            shutdown();
            state = GameInstanceState.CANCELLED;
            completedAt = Instant.now();
        } finally {
            stateLock.unlock();
        }
    }

    /** Cancel the game with owner authorization check */
    public void cancelAsUser(long userId) {
        checkOwnership(userId);
        cancel();
    }

    // ====================================
    // Player Management
    // ====================================

    /**
     * Add a player to the game. Only allowed in WAITING_FOR_PLAYERS state.
     *
     * @param profileId
     *            player's profile ID
     * @param playerName
     *            player's display name
     * @param isAI
     *            true if AI player
     * @param skillLevel
     *            AI skill level (0-100, ignored for human players)
     */
    public void addPlayer(long profileId, String playerName, boolean isAI, int skillLevel) {
        stateLock.lock();
        try {
            if (state != GameInstanceState.WAITING_FOR_PLAYERS) {
                throw new IllegalStateException("Cannot add players in state: " + state);
            }

            ServerPlayerSession session = new ServerPlayerSession(profileId, playerName, isAI, skillLevel);
            playerSessions.put(profileId, session);

        } finally {
            stateLock.unlock();
        }
    }

    /**
     * Remove a player from the game. If game is in progress, marks player as
     * disconnected instead of removing.
     */
    public void removePlayer(long profileId) {
        stateLock.lock();
        try {
            if (state == GameInstanceState.IN_PROGRESS || state == GameInstanceState.PAUSED) {
                // Mark as disconnected, don't remove
                ServerPlayerSession session = playerSessions.get(profileId);
                if (session != null) {
                    session.disconnect();
                }
            } else {
                // Actually remove if game hasn't started
                playerSessions.remove(profileId);
            }
        } finally {
            stateLock.unlock();
        }
    }

    /** Reconnect a previously disconnected player */
    public void reconnectPlayer(long profileId) {
        ServerPlayerSession session = playerSessions.get(profileId);
        if (session != null) {
            session.connect();
        }
    }

    /** Check if player is in this game */
    public boolean hasPlayer(long profileId) {
        return playerSessions.containsKey(profileId);
    }

    /**
     * Set a player's sit-out status. No-op if the tournament hasn't started or the
     * player isn't found.
     *
     * @param profileId
     *            player's profile ID
     * @param sittingOut
     *            true to sit out, false to come back
     */
    public void setSittingOut(long profileId, boolean sittingOut) {
        if (tournament == null) {
            return;
        }
        for (int t = 0; t < tournament.getNumTables(); t++) {
            ServerGameTable table = (ServerGameTable) tournament.getTable(t);
            for (int s = 0; s < table.getSeats(); s++) {
                ServerPlayer p = table.getPlayer(s);
                if (p != null && p.getID() == toIntId(profileId)) {
                    p.setSittingOut(sittingOut);
                    return;
                }
            }
        }
    }

    /** Check if player is disconnected */
    public boolean isPlayerDisconnected(long profileId) {
        ServerPlayerSession session = playerSessions.get(profileId);
        return session != null && session.isDisconnected();
    }

    /** Get current player count */
    public int getPlayerCount() {
        return playerSessions.size();
    }

    // ====================================
    // Callbacks (for ServerPlayerActionProvider and ServerTournamentDirector)
    // ====================================

    /** Called when engine needs a human player's action */
    private void onActionRequest(ActionRequest request) {
        ServerPlayerSession session = playerSessions.get((long) request.player().getID());
        if (session != null && session.getMessageSender() != null) {
            // Send action request to connected WebSocket client
            // (MessageSender is set by WebSocket layer in Milestone 3)
            session.getMessageSender().accept(request);
        } else {
            logger.debug("[GameInstance] onActionRequest profileId={} messageSender=null (session={})",
                    request.player().getID(), session != null ? "present" : "absent");
        }
    }

    /** Called when director reports lifecycle events (STARTED, COMPLETED, etc.) */
    private void onLifecycleEvent(GameLifecycleEvent event) {
        if (event == GameLifecycleEvent.COMPLETED) {
            stateLock.lock();
            try {
                if (state != GameInstanceState.CANCELLED) {
                    state = GameInstanceState.COMPLETED;
                    completedAt = Instant.now();
                }
            } finally {
                stateLock.unlock();
            }
        }
    }

    /**
     * Submit a player action (called by WebSocket handler).
     *
     * @param profileId
     *            player's profile ID
     * @param action
     *            the action to submit
     */
    public void onPlayerAction(long profileId, com.donohoedigital.games.poker.core.PlayerAction action) {
        logger.debug("[GameInstance] onPlayerAction profileId={} action={}", profileId, action);
        if (actionProvider != null) {
            actionProvider.submitAction(toIntId(profileId), action);
        }
    }

    // ====================================
    // Accessors
    // ====================================

    public String getGameId() {
        return gameId;
    }

    public long getOwnerProfileId() {
        return ownerProfileId;
    }

    public GameInstanceState getState() {
        return state;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public GameConfig getConfig() {
        return config;
    }

    public ServerTournamentContext getTournament() {
        return tournament;
    }

    /**
     * Build a game state snapshot for a specific player (for reconnect state sync).
     * Returns null if the tournament hasn't started or the player's table isn't
     * found.
     */
    public GameStateSnapshot getGameStateSnapshot(long profileId) {
        if (tournament == null)
            return null;
        for (int t = 0; t < tournament.getNumTables(); t++) {
            ServerGameTable table = (ServerGameTable) tournament.getTable(t);
            for (int s = 0; s < table.getSeats(); s++) {
                ServerPlayer p = table.getPlayer(s);
                if (p != null && p.getID() == toIntId(profileId)) {
                    ServerHand hand = (ServerHand) table.getHoldemHand();
                    return GameStateProjection.forPlayer(table, hand, toIntId(profileId));
                }
            }
        }
        return null;
    }

    /**
     * Re-sends the ACTION_REQUIRED message if the game is currently waiting for
     * this player's action. Called by the WebSocket handler when a player
     * reconnects so they receive the action prompt even if it fired before they
     * connected.
     */
    public void resendPendingActionIfAny(long profileId) {
        if (actionProvider == null)
            return;
        ActionRequest req = actionProvider.getPendingActionRequest(toIntId(profileId));
        if (req != null) {
            onActionRequest(req);
        }
    }

    public IGameEventStore getEventStore() {
        return eventStore;
    }

    public ServerGameEventBus getEventBus() {
        return eventBus;
    }

    public Map<Long, ServerPlayerSession> getPlayerSessions() {
        return java.util.Collections.unmodifiableMap(playerSessions);
    }

    /**
     * Returns display names and roles of all currently registered players, for use
     * in the pre-game lobby. Human players are role "PLAYER"; AI players are role
     * "AI".
     */
    public List<LobbyPlayerInfo> getConnectedPlayers() {
        return playerSessions.values().stream()
                .map(s -> new LobbyPlayerInfo(s.getPlayerName(), s.isAI() ? "AI" : "PLAYER"))
                .collect(Collectors.toList());
    }

    // ====================================
    // Rebuy / Addon Offer & Decision
    // ====================================

    /**
     * Offers a rebuy to a busted player. Blocks the calling thread (director) until
     * the player responds or the action timeout elapses. Returns true if accepted,
     * false if declined or timed out.
     */
    public boolean offerRebuy(int playerId, int tableId) {
        if (tournament == null || tournament.getRebuyCost() == 0) {
            return false;
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingRebuys.put(playerId, future);
        eventBus.publish(new com.donohoedigital.games.poker.core.event.GameEvent.RebuyOffered(tableId, playerId,
                tournament.getRebuyCost(), tournament.getRebuyChips(), properties.actionTimeoutSeconds()));
        try {
            return future.get(properties.actionTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            return false;
        } finally {
            pendingRebuys.remove(playerId);
        }
    }

    /**
     * Offers an addon to an active player during a break. Blocks until the player
     * responds or the action timeout elapses.
     */
    public boolean offerAddon(int playerId, int tableId) {
        if (tournament == null || tournament.getAddonChips() == 0) {
            return false;
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingAddons.put(playerId, future);
        eventBus.publish(new com.donohoedigital.games.poker.core.event.GameEvent.AddonOffered(tableId, playerId,
                tournament.getAddonCost(), tournament.getAddonChips(), properties.actionTimeoutSeconds()));
        try {
            return future.get(properties.actionTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            return false;
        } finally {
            pendingAddons.remove(playerId);
        }
    }

    /** Called by WebSocket router when a player accepts or declines a rebuy. */
    public void submitRebuyDecision(int playerId, boolean accept) {
        CompletableFuture<Boolean> f = pendingRebuys.get(playerId);
        if (f != null) {
            f.complete(accept);
        }
    }

    /** Called by WebSocket router when a player accepts or declines an addon. */
    public void submitAddonDecision(int playerId, boolean accept) {
        CompletableFuture<Boolean> f = pendingAddons.get(playerId);
        if (f != null) {
            f.complete(accept);
        }
    }

    // ====================================
    // Helper Methods
    // ====================================

    private void checkOwnership(long userId) {
        if (userId != ownerProfileId) {
            throw new GameServerException("Only the game owner can perform this action");
        }
    }

    private static int toIntId(long profileId) {
        return Math.toIntExact(profileId); // throws ArithmeticException on overflow
    }

    /**
     * Create a simple random AI provider.
     * <p>
     * This implementation provides basic AI functionality sufficient for testing
     * and validating the game engine infrastructure. It randomly selects from
     * available actions (check, call, fold, bet, raise) without strategic
     * decision-making. ServerAIProvider integration is a future milestone.
     */
    private PlayerActionProvider createSimpleAI() {
        Random random = new Random();
        return (player, options) -> {
            List<PlayerAction> availableActions = new ArrayList<>();

            if (options.canCheck())
                availableActions.add(PlayerAction.check());
            if (options.canCall())
                availableActions.add(PlayerAction.call());
            if (options.canFold())
                availableActions.add(PlayerAction.fold());

            if (options.canBet()) {
                int betAmount = options.minBet()
                        + random.nextInt(Math.max(1, (options.maxBet() - options.minBet()) / 2 + 1));
                availableActions.add(PlayerAction.bet(betAmount));
            }

            if (options.canRaise()) {
                int raiseAmount = options.minRaise()
                        + random.nextInt(Math.max(1, (options.maxRaise() - options.minRaise()) / 2 + 1));
                availableActions.add(PlayerAction.raise(raiseAmount));
            }

            return availableActions.isEmpty()
                    ? PlayerAction.fold()
                    : availableActions.get(random.nextInt(availableActions.size()));
        };
    }
}
