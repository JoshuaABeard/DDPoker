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
package com.donohoedigital.games.poker.gameserver.websocket;

import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameStateSnapshot;
import com.donohoedigital.games.poker.gameserver.ServerGameTable;
import com.donohoedigital.games.poker.gameserver.ServerHand;
import com.donohoedigital.games.poker.gameserver.ServerPlayer;
import com.donohoedigital.games.poker.gameserver.ServerTournamentContext;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.donohoedigital.games.poker.engine.Card;

/**
 * Bridges the ServerGameEventBus to WebSocket connections.
 *
 * Implements Consumer&lt;GameEvent&gt; to be set as the broadcastCallback on
 * ServerGameEventBus. Receives all game events and routes them to appropriate
 * connected clients via GameConnectionManager.
 *
 * <p>
 * Privacy routing:
 * <ul>
 * <li>Most events broadcast to all connected players</li>
 * <li>Hole card events are handled separately via messageSender on
 * ServerPlayerSession</li>
 * <li>ACTION_REQUIRED is handled via the messageSender mechanism, not this
 * broadcaster</li>
 * </ul>
 */
public class GameEventBroadcaster implements Consumer<GameEvent> {

    private static final Logger logger = LoggerFactory.getLogger(GameEventBroadcaster.class);

    private final String gameId;
    private final GameConnectionManager connectionManager;
    private final OutboundMessageConverter converter;

    /**
     * Optional game reference used to send per-player GAME_STATE snapshots before
     * HAND_STARTED when a game starts fresh (i.e. the client has no table data
     * yet). Null for reconnect-path broadcasters where the handler already sends
     * the snapshot directly.
     */
    private final GameInstance game;

    /**
     * When true, broadcast AI hole cards after HAND_STARTED (aiFaceUp practice
     * option).
     */
    private boolean aiFaceUp;

    /**
     * Enable or disable AI face-up mode. When enabled, AI hole cards are broadcast
     * to all connections after each HAND_STARTED.
     *
     * @param aiFaceUp
     *            true to reveal AI hole cards
     */
    public void setAiFaceUp(boolean aiFaceUp) {
        this.aiFaceUp = aiFaceUp;
    }

    /**
     * Creates a game event broadcaster without a game reference (reconnect path).
     *
     * @param gameId
     *            Game ID this broadcaster serves
     * @param connectionManager
     *            Connection manager for routing messages
     * @param converter
     *            Converter for game state to message payloads
     */
    public GameEventBroadcaster(String gameId, GameConnectionManager connectionManager,
            OutboundMessageConverter converter) {
        this(gameId, connectionManager, converter, null);
    }

    /**
     * Creates a game event broadcaster with a game reference (auto-start path). The
     * game reference is used to send each player a GAME_STATE snapshot immediately
     * before the HAND_STARTED broadcast so that the client can build its table view
     * before any action-related events arrive.
     *
     * @param gameId
     *            Game ID this broadcaster serves
     * @param connectionManager
     *            Connection manager for routing messages
     * @param converter
     *            Converter for game state to message payloads
     * @param game
     *            Game instance for snapshot generation (may be null)
     */
    public GameEventBroadcaster(String gameId, GameConnectionManager connectionManager,
            OutboundMessageConverter converter, GameInstance game) {
        this.gameId = gameId;
        this.connectionManager = connectionManager;
        this.converter = converter;
        this.game = game;
    }

    @Override
    public void accept(GameEvent event) {
        logger.debug("[BROADCAST] gameId={} event={}", gameId, event.getClass().getSimpleName());
        switch (event) {
            case GameEvent.HandStarted e -> {
                if (game != null) {
                    // Look up dealer/SB/BB seats from the starting table.
                    int dealerSeat = -1;
                    int sbSeat = -1;
                    int bbSeat = -1;
                    if (game.getTournament() != null && e.tableId() >= 0
                            && e.tableId() < game.getTournament().getNumTables()) {
                        Object gt = game.getTournament().getTable(e.tableId());
                        if (gt instanceof ServerGameTable sgt) {
                            dealerSeat = sgt.getButton();
                            ServerHand hand = (ServerHand) sgt.getHoldemHand();
                            if (hand != null) {
                                sbSeat = hand.getSmallBlindSeat();
                                bbSeat = hand.getBigBlindSeat();
                            }
                        }
                    }
                    ServerMessage handStartedMsg = ServerMessage.of(ServerMessageType.HAND_STARTED, gameId,
                            new ServerMessageData.HandStartedData(e.handNumber(), dealerSeat, sbSeat, bbSeat,
                                    List.of()));
                    // Send GAME_STATE + HAND_STARTED only to players seated at this table.
                    // Sending HAND_STARTED for other tables would corrupt the client's
                    // hand model (onHandStarted always applies to currentTable).
                    for (PlayerConnection conn : connectionManager.getConnections(gameId)) {
                        GameStateSnapshot snapshot = game.getGameStateSnapshot(conn.getProfileId());
                        if (snapshot != null && snapshot.tableId() == e.tableId()) {
                            logger.debug("[BROADCAST] sending GAME_STATE + HAND_STARTED to player={} for table={}",
                                    conn.getProfileId(), e.tableId());
                            conn.sendMessage(converter.createGameStateMessage(gameId, snapshot));
                            conn.sendMessage(handStartedMsg);
                            // Send hole cards privately AFTER HAND_STARTED so they arrive
                            // after the client's TYPE_NEW_HAND fires (which resets card slots).
                            Card[] holeCards = snapshot.myHoleCards();
                            if (holeCards != null && holeCards.length > 0) {
                                conn.sendMessage(converter.createHoleCardsMessage(gameId, holeCards));
                            }
                        }
                    }
                    // If aiFaceUp, broadcast AI hole cards after all players received HAND_STARTED
                    if (aiFaceUp && game.getTournament() != null && e.tableId() >= 0
                            && e.tableId() < game.getTournament().getNumTables()) {
                        Object gt = game.getTournament().getTable(e.tableId());
                        if (gt instanceof ServerGameTable sgt) {
                            ServerHand hand = (ServerHand) sgt.getHoldemHand();
                            if (hand != null) {
                                List<ServerMessageData.AiPlayerCards> aiCards = new ArrayList<>();
                                for (int s = 0; s < sgt.getNumSeats(); s++) {
                                    ServerPlayer sp = sgt.getPlayer(s);
                                    if (sp != null && !sp.isHuman() && !sp.isSittingOut()) {
                                        List<Card> cards = hand.getPlayerCards(sp.getID());
                                        if (!cards.isEmpty()) {
                                            aiCards.add(new ServerMessageData.AiPlayerCards(
                                                    sp.getID(),
                                                    OutboundMessageConverter.cardsToList(cards.toArray(new Card[0]))));
                                        }
                                    }
                                }
                                if (!aiCards.isEmpty()) {
                                    broadcast(ServerMessage.of(ServerMessageType.AI_HOLE_CARDS, gameId,
                                            new ServerMessageData.AiHoleCardsData(aiCards)));
                                }
                            }
                        }
                    }
                } else {
                    // No game reference — broadcast to all (future: filter by table).
                    broadcast(ServerMessage.of(ServerMessageType.HAND_STARTED, gameId,
                            new ServerMessageData.HandStartedData(e.handNumber(), -1, -1, -1, List.of())));
                }
            }
            case GameEvent.PlayerActed e -> {
                int chipCount = 0;
                int totalBet = 0;
                int potTotal = 0;
                String playerName = "";
                if (game != null && game.getTournament() != null
                        && e.tableId() >= 0 && e.tableId() < game.getTournament().getNumTables()) {
                    Object gt = game.getTournament().getTable(e.tableId());
                    if (gt instanceof ServerGameTable sgt) {
                        ServerHand hand = (ServerHand) sgt.getHoldemHand();
                        if (hand != null) {
                            totalBet = hand.getPlayerBet(e.playerId());
                            potTotal = hand.getPotSize();
                        }
                        for (int s = 0; s < sgt.getNumSeats(); s++) {
                            ServerPlayer sp = sgt.getPlayer(s);
                            if (sp != null && sp.getID() == e.playerId()) {
                                chipCount = sp.getChipCount();
                                playerName = sp.getName();
                                break;
                            }
                        }
                    }
                }
                broadcast(ServerMessage.of(ServerMessageType.PLAYER_ACTED, gameId,
                    new ServerMessageData.PlayerActedData(e.playerId(), playerName, e.action().name(), e.amount(),
                        totalBet, chipCount, potTotal, e.tableId())));
            }
            case GameEvent.CommunityCardsDealt e -> {
                // Look up actual community cards from the live hand at broadcast time.
                List<String> allCommunityCards = List.of();
                if (game != null && game.getTournament() != null && e.tableId() >= 0
                        && e.tableId() < game.getTournament().getNumTables()) {
                    Object gt = game.getTournament().getTable(e.tableId());
                    if (gt instanceof ServerGameTable sgt) {
                        ServerHand hand = (ServerHand) sgt.getHoldemHand();
                        if (hand != null) {
                            Card[] community = hand.getCommunityCards();
                            allCommunityCards = OutboundMessageConverter.cardsToList(community);
                        }
                    }
                }
                broadcast(ServerMessage.of(ServerMessageType.COMMUNITY_CARDS_DEALT, gameId,
                        new ServerMessageData.CommunityCardsDealtData(e.round().name(), allCommunityCards,
                                allCommunityCards, e.tableId())));
            }
            case GameEvent.HandCompleted e -> {
                int handNum = 0;
                List<ServerMessageData.WinnerData> winners = List.of();
                List<ServerMessageData.ShowdownPlayerData> showdownPlayers = List.of();
                if (game != null && game.getTournament() != null && e.tableId() >= 0
                        && e.tableId() < game.getTournament().getNumTables()) {
                    Object gt = game.getTournament().getTable(e.tableId());
                    if (gt instanceof ServerGameTable sgt) {
                        handNum = sgt.getHandNum();
                        ServerHand hand = (ServerHand) sgt.getHoldemHand();
                        if (hand != null) {
                            List<ServerHand.PotResolutionResult> results = hand.getResolutionResults();
                            if (!results.isEmpty()) {
                                List<ServerMessageData.WinnerData> w = new ArrayList<>();
                                for (ServerHand.PotResolutionResult r : results) {
                                    int share = r.winnerIds().length > 0 ? r.amount() / r.winnerIds().length : 0;
                                    for (int wid : r.winnerIds()) {
                                        w.add(new ServerMessageData.WinnerData(wid, share, "", List.of(),
                                                r.potIndex()));
                                    }
                                }
                                winners = w;
                            }
                            // Build showdown players — include all players who were in the hand
                            // (not sitting out), so folded players' cards can be revealed too.
                            List<ServerPlayer> allWithCards = new ArrayList<>();
                            for (int s = 0; s < sgt.getNumSeats(); s++) {
                                ServerPlayer sp = sgt.getPlayer(s);
                                if (sp != null && !sp.isSittingOut()) {
                                    allWithCards.add(sp);
                                }
                            }
                            if (allWithCards.size() > 1) {
                                showdownPlayers = allWithCards.stream()
                                        .map(sp -> new ServerMessageData.ShowdownPlayerData(
                                                sp.getID(),
                                                OutboundMessageConverter.cardsToList(
                                                        hand.getPlayerCards(sp.getID()).toArray(new Card[0])),
                                                sp.getName()))
                                        .toList();
                            }
                        }
                    }
                }
                broadcast(ServerMessage.of(ServerMessageType.HAND_COMPLETE, gameId,
                        new ServerMessageData.HandCompleteData(handNum, winners, showdownPlayers, e.tableId())));
            }
            case GameEvent.PotAwarded e -> {
                int[] ids = e.winnerIds();
                long[] winnerIds = new long[ids.length];
                for (int i = 0; i < ids.length; i++) winnerIds[i] = ids[i];
                broadcast(ServerMessage.of(ServerMessageType.POT_AWARDED, gameId,
                    new ServerMessageData.PotAwardedData(winnerIds, e.amount(), e.potIndex(), e.tableId())));
            }
            case GameEvent.ShowdownStarted e -> {
                List<ServerMessageData.ShowdownPlayerData> showdownPlayers = List.of();
                if (game != null && game.getTournament() != null && e.tableId() >= 0
                        && e.tableId() < game.getTournament().getNumTables()) {
                    Object gt = game.getTournament().getTable(e.tableId());
                    if (gt instanceof ServerGameTable sgt) {
                        ServerHand hand = (ServerHand) sgt.getHoldemHand();
                        if (hand != null) {
                            List<ServerMessageData.ShowdownPlayerData> sp = new ArrayList<>();
                            for (int s = 0; s < sgt.getNumSeats(); s++) {
                                ServerPlayer player = sgt.getPlayer(s);
                                if (player != null && !player.isFolded()) {
                                    List<String> cards = OutboundMessageConverter.cardsToList(
                                            hand.getPlayerCards(player));
                                    sp.add(new ServerMessageData.ShowdownPlayerData(player.getID(), cards, ""));
                                }
                            }
                            showdownPlayers = sp;
                        }
                    }
                }
                broadcast(ServerMessage.of(ServerMessageType.SHOWDOWN_STARTED, gameId,
                        new ServerMessageData.ShowdownStartedData(e.tableId(), showdownPlayers)));
            }
            case GameEvent.LevelChanged e -> {
                int sb = 0, bb = 0, ante = 0;
                if (game != null && game.getTournament() instanceof TournamentContext tc) {
                    sb = tc.getSmallBlind(e.newLevel());
                    bb = tc.getBigBlind(e.newLevel());
                    ante = tc.getAnte(e.newLevel());
                }
                broadcast(ServerMessage.of(ServerMessageType.LEVEL_CHANGED, gameId,
                        new ServerMessageData.LevelChangedData(e.newLevel(), sb, bb, ante, null)));
            }
            case GameEvent.TournamentCompleted e -> {
                List<ServerMessageData.StandingData> standings = List.of();
                if (game != null && game.getTournament() instanceof ServerTournamentContext ctx) {
                    ctx.getAllPlayers().stream()
                            .filter(p -> p.getID() == e.winnerId())
                            .findFirst()
                            .ifPresent(p -> p.setFinishPosition(1));
                    standings = ctx.getAllPlayers().stream()
                            .filter(p -> p.getFinishPosition() > 0)
                            .sorted(Comparator.comparingInt(ServerPlayer::getFinishPosition))
                            .map(p -> new ServerMessageData.StandingData(p.getFinishPosition(), p.getID(),
                                    p.getName(), 0))
                            .toList();
                }
                broadcast(ServerMessage.of(ServerMessageType.GAME_COMPLETE, gameId,
                        new ServerMessageData.GameCompleteData(standings, 0, 0L)));
            }
            case GameEvent.BreakStarted e -> broadcast(
                ServerMessage.of(ServerMessageType.GAME_PAUSED, gameId,
                    new ServerMessageData.GamePausedData("Break started", "system"))
            );
            case GameEvent.BreakEnded e -> broadcast(
                ServerMessage.of(ServerMessageType.GAME_RESUMED, gameId,
                    new ServerMessageData.GameResumedData("system"))
            );
            case GameEvent.PlayerAdded e -> {
                // Look up the player's name from the table so it arrives correctly
                // in PLAYER_JOINED (e.g. during multi-table consolidation moves).
                String playerName = "";
                if (game != null && game.getTournament() != null) {
                    Object gt = game.getTournament().getTable(e.tableId());
                    if (gt instanceof ServerGameTable sgt) {
                        ServerPlayer sp = sgt.getPlayer(e.seat());
                        if (sp != null) {
                            playerName = sp.getName();
                        }
                    }
                }
                broadcast(ServerMessage.of(ServerMessageType.PLAYER_JOINED, gameId,
                    new ServerMessageData.PlayerJoinedData(e.playerId(), playerName, e.seat(), e.tableId())));
            }
            case GameEvent.PlayerRemoved e -> {
                // During table consolidation the player is moved (PlayerRemoved then
                // PlayerAdded). Suppress PLAYER_LEFT so the client doesn't show them
                // as permanently gone; the subsequent PLAYER_JOINED will re-seat them.
                // For eliminated players (finishPosition > 0) we still send PLAYER_LEFT
                // to clear their seat (PLAYER_ELIMINATED doesn't clear it).
                boolean isConsolidation = false;
                if (game != null && game.getTournament() instanceof ServerTournamentContext ctx) {
                    isConsolidation = ctx.getAllPlayers().stream()
                            .anyMatch(p -> p.getID() == e.playerId() && p.getFinishPosition() == 0);
                }
                if (!isConsolidation) {
                    broadcast(ServerMessage.of(ServerMessageType.PLAYER_LEFT, gameId,
                        new ServerMessageData.PlayerLeftData(e.playerId(), "")));
                } else {
                    logger.debug("[BROADCAST] suppressing PLAYER_LEFT for consolidation playerId={}", e.playerId());
                }
            }
            case GameEvent.PlayerRebuy e -> broadcast(
                ServerMessage.of(ServerMessageType.PLAYER_REBUY, gameId,
                    new ServerMessageData.PlayerRebuyData(e.playerId(), "", e.amount()))
            );
            case GameEvent.PlayerAddon e -> broadcast(
                ServerMessage.of(ServerMessageType.PLAYER_ADDON, gameId,
                    new ServerMessageData.PlayerAddonData(e.playerId(), "", e.amount()))
            );
            case GameEvent.PlayerEliminated e -> broadcast(
                ServerMessage.of(ServerMessageType.PLAYER_ELIMINATED, gameId,
                    new ServerMessageData.PlayerEliminatedData(e.playerId(), "", e.finishPosition(), 0, e.tableId()))
            );
            case GameEvent.ActionTimeout e -> {
                // ActionTimeout has no tableId on the event itself; derive it by
                // finding which table the timing-out player is currently seated at.
                int timeoutTableId = -1;
                if (game != null && game.getTournament() != null) {
                    outer:
                    for (int t = 0; t < game.getTournament().getNumTables(); t++) {
                        Object gt = game.getTournament().getTable(t);
                        if (gt instanceof ServerGameTable sgt) {
                            for (int s = 0; s < sgt.getNumSeats(); s++) {
                                ServerPlayer sp = sgt.getPlayer(s);
                                if (sp != null && sp.getID() == e.playerId()) {
                                    timeoutTableId = t;
                                    break outer;
                                }
                            }
                        }
                    }
                }
                broadcast(ServerMessage.of(ServerMessageType.ACTION_TIMEOUT, gameId,
                    new ServerMessageData.ActionTimeoutData(e.playerId(), e.autoAction().name(), timeoutTableId)));
            }
            case GameEvent.RebuyOffered e -> connectionManager.sendToPlayer(gameId, (long) e.playerId(),
                ServerMessage.of(ServerMessageType.REBUY_OFFERED, gameId,
                    new ServerMessageData.RebuyOfferedData(e.cost(), e.chips(), e.timeoutSeconds())));
            case GameEvent.AddonOffered e -> connectionManager.sendToPlayer(gameId, (long) e.playerId(),
                ServerMessage.of(ServerMessageType.ADDON_OFFERED, gameId,
                    new ServerMessageData.AddonOfferedData(e.cost(), e.chips(), e.timeoutSeconds())));
            case GameEvent.ChipsTransferred e -> {
                String fromName = "";
                String toName = "";
                if (game != null && game.getTournament() != null && e.tableId() >= 0
                        && e.tableId() < game.getTournament().getNumTables()) {
                    Object gt = game.getTournament().getTable(e.tableId());
                    if (gt instanceof ServerGameTable sgt) {
                        for (int s = 0; s < sgt.getNumSeats(); s++) {
                            ServerPlayer sp = sgt.getPlayer(s);
                            if (sp == null) continue;
                            if (sp.getID() == e.fromPlayerId()) fromName = sp.getName();
                            if (sp.getID() == e.toPlayerId()) toName = sp.getName();
                        }
                    }
                }
                broadcast(ServerMessage.of(ServerMessageType.CHIPS_TRANSFERRED, gameId,
                        new ServerMessageData.ChipsTransferredData(e.fromPlayerId(), fromName,
                                e.toPlayerId(), toName, e.amount())));
            }
            case GameEvent.ColorUpStarted e -> {
                List<ServerMessageData.ColorUpPlayerData> players = e.players().stream()
                        .map(p -> new ServerMessageData.ColorUpPlayerData(
                                p.playerId(), p.cards(), p.won(), p.broke(), p.finalChips()))
                        .toList();
                broadcast(ServerMessage.of(ServerMessageType.COLOR_UP_STARTED, gameId,
                        new ServerMessageData.ColorUpStartedData(players, e.newMinChip(), e.tableId())));
            }
            // Internal housekeeping events — not broadcast to clients
            case GameEvent.ButtonMoved ignored -> {}
            case GameEvent.TableStateChanged ignored -> {}
            case GameEvent.CurrentPlayerChanged ignored -> {}
            case GameEvent.ObserverAdded ignored -> {}
            case GameEvent.ObserverRemoved ignored -> {}
            case GameEvent.ColorUpCompleted ignored -> {}
            case GameEvent.CleaningDone ignored -> {}
            default -> logger.debug("[BROADCAST] unhandled event type: {}", event.getClass().getSimpleName());
        }
    }

    private void broadcast(ServerMessage message) {
        connectionManager.broadcastToGame(gameId, message);
    }
}
