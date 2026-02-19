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

import com.donohoedigital.games.engine.BasePhase;
import com.donohoedigital.games.engine.GameManager;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
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
import java.util.List;
import java.util.Map;

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

    // One RemotePokerTable per server table ID
    private final Map<Integer, RemotePokerTable> tables_ = new HashMap<>();

    // Current player ID for this client (set from CONNECTED message)
    private long localPlayerId_ = -1;

    // Last ACTION_REQUIRED options (stored for UI query)
    private ActionOptionsData currentOptions_;

    // Chat handler registered by ShowTournamentTable
    private ChatHandler chatHandler_;

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

        PokerGame.WebSocketConfig config = game_.getWebSocketConfig();
        serverPort_ = config.port();
        gameId_ = config.gameId();
        jwt_ = config.jwt();

        wsClient_.setMessageHandler(this::onMessage);
        wsClient_.connect(serverPort_, gameId_, jwt_);

        // Route player UI actions to WebSocket
        game_.setPlayerActionListener((action, amount) -> {
            String wsAction = mapActionToWsString(action);
            wsClient_.sendAction(wsAction, amount);
        });
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

    /**
     * Returns the first table in map order (for testing single-table scenarios).
     */
    RemotePokerTable getFirstTable() {
        if (tables_.isEmpty())
            return null;
        return tables_.values().iterator().next();
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
            }
        } catch (Exception e) {
            logger.error("Error handling {} message", type, e);
        }
    }

    // -------------------------------------------------------------------------
    // Message handlers
    // -------------------------------------------------------------------------

    private void onConnected(ConnectedData d) {
        localPlayerId_ = d.playerId();
        if (d.gameState() != null) {
            onGameState(d.gameState());
        }
    }

    private void onGameState(GameStateData d) {
        SwingUtilities.invokeLater(() -> {
            // Update level immediately — independent of table creation
            game_.setLevel(d.level());

            // Remove tables no longer present
            List<Integer> receivedIds = new ArrayList<>();
            for (TableData td : d.tables()) {
                receivedIds.add(td.tableId());
            }
            List<Integer> toRemove = new ArrayList<>(tables_.keySet());
            toRemove.removeAll(receivedIds);
            for (int id : toRemove) {
                RemotePokerTable t = tables_.remove(id);
                game_.removeTable(t);
            }

            // Create or update tables
            for (TableData td : d.tables()) {
                RemotePokerTable table = tables_.computeIfAbsent(td.tableId(), id -> {
                    RemotePokerTable t = new RemotePokerTable(game_, id);
                    game_.addTable(t);
                    return t;
                });
                applyTableData(table, td);
            }

            // Select the table that contains the local player
            selectLocalPlayerTable();
        });
    }

    private void onHandStarted(HandStartedData d) {
        SwingUtilities.invokeLater(() -> {
            // Apply to all tables (HAND_STARTED is per-table; we apply to the current
            // table)
            RemotePokerTable table = currentTable();
            if (table == null)
                return;

            RemoteHoldemHand hand = new RemoteHoldemHand();
            hand.updateRound(BettingRound.PRE_FLOP);

            // Reconstruct player order from seated players (seat order for the hand)
            List<PokerPlayer> handPlayers = seatedPlayersFrom(table);
            hand.updatePlayerOrder(handPlayers);
            hand.updateCurrentPlayer(HoldemHand.NO_CURRENT_PLAYER);

            table.setRemoteHand(hand);

            // Move button to dealer seat
            int oldButton = table.getButton();
            table.setRemoteButton(d.dealerSeat());
            table.fireEvent(PokerTableEvent.TYPE_BUTTON_MOVED, oldButton);
            table.fireEvent(PokerTableEvent.TYPE_NEW_HAND);
        });
    }

    private void onHoleCardsDealt(HoleCardsDealtData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            // Hole cards are sent privately only to the card owner
            PokerPlayer localPlayer = findPlayer(localPlayerId_);
            if (localPlayer == null)
                return;

            Hand playerHand = localPlayer.getHand();
            for (String c : d.cards()) {
                Card card = Card.getCard(c);
                if (card != null)
                    playerHand.addCard(card);
            }

            table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION);
        });
    }

    private void onCommunityCardsDealt(CommunityCardsDealtData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            BettingRound round = BettingRound.valueOf(d.round());
            hand.updateRound(round);
            hand.updateCommunity(parseCards(d.allCommunityCards()));

            table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION, round.toLegacy());
        });
    }

    private void onActionRequired(ActionRequiredData d) {
        SwingUtilities.invokeLater(() -> {
            currentOptions_ = d.options();

            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            // The local player is the current actor
            int playerIndex = indexOfPlayer(hand, localPlayerId_);
            hand.updateCurrentPlayer(playerIndex);

            table.fireEvent(PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED);
        });
    }

    private void onPlayerActed(PlayerActedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            // Update chip counts and pot
            PokerPlayer player = findPlayer(d.playerId());
            if (player != null) {
                player.setChipCount(d.chipCount());
            }
            hand.updatePot(d.potTotal());

            // Advance current player (server will send ACTION_REQUIRED for next actor)
            hand.updateCurrentPlayer(HoldemHand.NO_CURRENT_PLAYER);

            int handAction = mapWsStringToAction(d.action());
            HandAction action = new HandAction(player, handAction, d.amount());
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ACTION, table, action));
        });
    }

    private void onActionTimeout(ActionTimeoutData d) {
        SwingUtilities.invokeLater(() -> {
            // Auto-action already applied server-side; treat like PLAYER_ACTED
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            hand.updateCurrentPlayer(HoldemHand.NO_CURRENT_PLAYER);

            PokerPlayer player = findPlayer(d.playerId());
            int handAction = mapWsStringToAction(d.autoAction());
            HandAction action = new HandAction(player, handAction, 0);
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ACTION, table, action));
        });
    }

    private void onHandComplete(HandCompleteData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
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

            hand.updateCurrentPlayer(HoldemHand.NO_CURRENT_PLAYER);
            table.fireEvent(PokerTableEvent.TYPE_END_HAND);
        });
    }

    private void onPotAwarded(PotAwardedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            // Pot display updated — fire a player-chips-changed event
            table.fireEvent(PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED);
        });
    }

    private void onShowdownStarted(ShowdownStartedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = tables_.get(d.tableId());
            if (table == null)
                table = currentTable();
            if (table == null)
                return;
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null)
                return;

            hand.updateRound(BettingRound.SHOWDOWN);
            table.fireEvent(PokerTableEvent.TYPE_DEALER_ACTION, BettingRound.SHOWDOWN.toLegacy());
        });
    }

    private void onLevelChanged(LevelChangedData d) {
        SwingUtilities.invokeLater(() -> {
            int oldLevel = game_.getLevel();
            game_.setLevel(d.level());
            // Fire as a property change that ShowTournamentTable listens to
            for (RemotePokerTable table : tables_.values()) {
                table.fireEvent(PokerTableEvent.TYPE_LEVEL_CHANGED, oldLevel);
            }
        });
    }

    private void onPlayerEliminated(PlayerEliminatedData d) {
        SwingUtilities.invokeLater(() -> {
            PokerPlayer player = findPlayer(d.playerId());
            if (player != null) {
                player.setPlace(d.finishPosition());
                player.setChipCount(0);
            }
            RemotePokerTable table = currentTable();
            if (table != null) {
                table.fireEvent(PokerTableEvent.TYPE_PLAYER_REMOVED);
            }
        });
    }

    private void onRebuyOffered(RebuyOfferedData d) {
        SwingUtilities.invokeLater(() -> {
            // Show rebuy dialog — existing UI flow
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_REBUY, table,
                    findPlayer(localPlayerId_), d.cost(), d.chips(), true));
        });
    }

    private void onAddonOffered(AddonOfferedData d) {
        SwingUtilities.invokeLater(() -> {
            RemotePokerTable table = currentTable();
            if (table == null)
                return;
            table.firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ADDON, table,
                    findPlayer(localPlayerId_), d.cost(), d.chips(), true));
        });
    }

    private void onGameComplete(GameCompleteData d) {
        SwingUtilities.invokeLater(() -> {
            // Trigger the results phase via the context's phase transition
            context_.processPhase("PracticeGameOver");
        });
    }

    private void onPlayerJoined(PlayerJoinedData d) {
        SwingUtilities.invokeLater(() -> {
            // Find or create the player and seat them at the matching table
            for (RemotePokerTable table : tables_.values()) {
                if (d.seatIndex() < PokerConstants.SEATS) {
                    PokerPlayer existing = table.getPlayer(d.seatIndex());
                    if (existing == null) {
                        PokerPlayer p = new PokerPlayer((int) d.playerId(), d.playerName(),
                                d.playerId() == localPlayerId_);
                        table.setRemotePlayer(d.seatIndex(), p);
                        table.firePokerTableEvent(
                                new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_ADDED, table, p, d.seatIndex()));
                    }
                    break;
                }
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
            PokerPlayer player = findPlayer(d.playerId());
            if (player != null) {
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
            if (player != null) {
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
            for (RemotePokerTable table : tables_.values()) {
                table.fireEvent(PokerTableEvent.TYPE_STATE_CHANGED);
            }
        });
    }

    private void onGameResumed(GameResumedData d) {
        SwingUtilities.invokeLater(() -> {
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
        SwingUtilities.invokeLater(() -> logger.error("Server error {}: {}", d.code(), d.message()));
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
        // No-op in M4 practice mode (AI-only games have no settings to propagate).
        // In M6 multiplayer, this sends a player-settings update to the server.
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
        player.addRebuy(nAmount, nChips, bPending);
        wsClient_.sendRebuyDecision(true);
    }

    @Override
    public void doAddon(PokerPlayer player, int nAmount, int nChips) {
        player.addAddon(nAmount, nChips);
        wsClient_.sendAddonDecision(true);
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
        // In M4 practice mode this is never called with a live chatHandler_.
        // In M6 multiplayer, the chatHandler_ will route this to the chat panel.
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
            if (findSeat(table, localPlayerId_) >= 0) {
                game_.setCurrentTable(table);
                return;
            }
        }
        // Fall back to first table
        if (!tables_.isEmpty()) {
            game_.setCurrentTable(tables_.values().iterator().next());
        }
    }

    /**
     * Applies a full {@link TableData} snapshot to a {@link RemotePokerTable}.
     * Rebuilds the player array, button position, community cards, and pot total.
     */
    private void applyTableData(RemotePokerTable table, TableData td) {
        PokerPlayer[] players = new PokerPlayer[PokerConstants.SEATS];
        int dealerSeat = PokerTable.NO_SEAT;

        for (SeatData sd : td.seats()) {
            if (sd.seatIndex() < 0 || sd.seatIndex() >= PokerConstants.SEATS)
                continue;
            // PokerPlayer.getID() is int; server IDs are long. Safe for embedded mode
            // where IDs are small sequential values. Will need revisiting if IDs exceed
            // Integer.MAX_VALUE (e.g., high-volume server deployment in M6+).
            PokerPlayer p = new PokerPlayer((int) sd.playerId(), sd.playerName(), sd.playerId() == localPlayerId_);
            p.setChipCount(sd.chipCount());
            // Apply hole cards from snapshot (only provided for the local player)
            if (sd.cards() != null && !sd.cards().isEmpty()) {
                for (String c : sd.cards()) {
                    Card card = Card.getCard(c);
                    if (card != null)
                        p.getHand().addCard(card);
                }
            }
            players[sd.seatIndex()] = p;
            if (sd.isDealer())
                dealerSeat = sd.seatIndex();
        }

        table.updateFromState(players, dealerSeat);

        // Rebuild the current hand from the table snapshot
        if (td.currentRound() != null) {
            RemoteHoldemHand hand = table.getRemoteHand();
            if (hand == null) {
                hand = new RemoteHoldemHand();
                table.setRemoteHand(hand);
            }
            BettingRound round = parseBettingRound(td.currentRound());
            hand.updateRound(round);

            if (td.communityCards() != null && !td.communityCards().isEmpty()) {
                hand.updateCommunity(parseCards(td.communityCards()));
            }

            int potTotal = td.pots() == null ? 0 : td.pots().stream().mapToInt(PotData::amount).sum();
            hand.updatePot(potTotal);

            hand.updatePlayerOrder(seatedPlayersFrom(table));
        }

        // Fire table-level events to refresh the UI
        table.fireEvent(PokerTableEvent.TYPE_NEW_PLAYERS_LOADED);
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
            default -> "FOLD"; // safe fallback
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

    /** Deserializes a {@link JsonNode} into the target data class. */
    private <T extends ServerMessageData> T parse(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse " + type.getSimpleName(), e);
        }
    }
}
