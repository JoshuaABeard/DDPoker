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

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;

import java.util.List;
import java.util.function.Consumer;

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

    private final String gameId;
    private final GameConnectionManager connectionManager;
    private final OutboundMessageConverter converter;

    /**
     * Creates a game event broadcaster.
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
        this.gameId = gameId;
        this.connectionManager = connectionManager;
        this.converter = converter;
    }

    @Override
    public void accept(GameEvent event) {
        switch (event) {
            case GameEvent.HandStarted e -> broadcast(
                ServerMessage.of(ServerMessageType.HAND_STARTED, gameId,
                    new ServerMessageData.HandStartedData(e.handNumber(), -1, -1, -1, List.of()))
            );
            case GameEvent.PlayerActed e -> broadcast(
                ServerMessage.of(ServerMessageType.PLAYER_ACTED, gameId,
                    new ServerMessageData.PlayerActedData(e.playerId(), "", e.action().name(), e.amount(), 0, 0, 0))
            );
            case GameEvent.CommunityCardsDealt e -> broadcast(
                ServerMessage.of(ServerMessageType.COMMUNITY_CARDS_DEALT, gameId,
                    new ServerMessageData.CommunityCardsDealtData(e.round().name(), List.of(), List.of()))
            );
            case GameEvent.HandCompleted e -> broadcast(
                ServerMessage.of(ServerMessageType.HAND_COMPLETE, gameId,
                    new ServerMessageData.HandCompleteData(0, List.of(), List.of()))
            );
            case GameEvent.PotAwarded e -> {
                int[] ids = e.winnerIds();
                long[] winnerIds = new long[ids.length];
                for (int i = 0; i < ids.length; i++) winnerIds[i] = ids[i];
                broadcast(ServerMessage.of(ServerMessageType.POT_AWARDED, gameId,
                    new ServerMessageData.PotAwardedData(winnerIds, e.amount(), e.potIndex())));
            }
            case GameEvent.ShowdownStarted e -> broadcast(
                ServerMessage.of(ServerMessageType.SHOWDOWN_STARTED, gameId,
                    new ServerMessageData.ShowdownStartedData(e.tableId()))
            );
            case GameEvent.LevelChanged e -> broadcast(
                ServerMessage.of(ServerMessageType.LEVEL_CHANGED, gameId,
                    new ServerMessageData.LevelChangedData(e.newLevel(), 0, 0, 0, null))
            );
            case GameEvent.TournamentCompleted e -> broadcast(
                ServerMessage.of(ServerMessageType.GAME_COMPLETE, gameId,
                    new ServerMessageData.GameCompleteData(List.of(), 0, 0L))
            );
            case GameEvent.BreakStarted e -> broadcast(
                ServerMessage.of(ServerMessageType.GAME_PAUSED, gameId,
                    new ServerMessageData.GamePausedData("Break started", "system"))
            );
            case GameEvent.BreakEnded e -> broadcast(
                ServerMessage.of(ServerMessageType.GAME_RESUMED, gameId,
                    new ServerMessageData.GameResumedData("system"))
            );
            case GameEvent.PlayerAdded e -> broadcast(
                ServerMessage.of(ServerMessageType.PLAYER_JOINED, gameId,
                    new ServerMessageData.PlayerJoinedData(e.playerId(), "", e.seat()))
            );
            case GameEvent.PlayerRemoved e -> broadcast(
                ServerMessage.of(ServerMessageType.PLAYER_LEFT, gameId,
                    new ServerMessageData.PlayerLeftData(e.playerId(), ""))
            );
            case GameEvent.PlayerRebuy e -> broadcast(
                ServerMessage.of(ServerMessageType.PLAYER_REBUY, gameId,
                    new ServerMessageData.PlayerRebuyData(e.playerId(), "", e.amount()))
            );
            case GameEvent.PlayerAddon e -> broadcast(
                ServerMessage.of(ServerMessageType.PLAYER_ADDON, gameId,
                    new ServerMessageData.PlayerAddonData(e.playerId(), "", e.amount()))
            );
            // Internal housekeeping events — not broadcast to clients
            case GameEvent.ButtonMoved ignored -> {}
            case GameEvent.TableStateChanged ignored -> {}
            case GameEvent.CurrentPlayerChanged ignored -> {}
            case GameEvent.ObserverAdded ignored -> {}
            case GameEvent.ObserverRemoved ignored -> {}
            case GameEvent.ColorUpCompleted ignored -> {}
            case GameEvent.CleaningDone ignored -> {}
        }
    }

    private void broadcast(ServerMessage message) {
        connectionManager.broadcastToGame(gameId, message);
    }
}
