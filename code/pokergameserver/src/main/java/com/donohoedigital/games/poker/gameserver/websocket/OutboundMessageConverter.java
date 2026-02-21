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

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.gameserver.GameStateSnapshot;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts internal game state objects to server-to-client WebSocket messages.
 *
 * All card serialization uses getRankDisplaySingle() + getCardSuit().getAbbr()
 * to produce two-character notation (e.g. "Ah", "Ts") compatible with
 * Card.getCard() parsing.
 */
public class OutboundMessageConverter {

    /**
     * Converts a Card to its string representation.
     *
     * @param card
     *            Card to convert (may be null)
     * @return Card display string (e.g., "Ah", "Kd"), or null if card is null
     */
    public static String cardToString(Card card) {
        if (card == null)
            return null;
        return card.getRankDisplaySingle() + card.getCardSuit().getAbbr();
    }

    /**
     * Converts an array of cards to a list of card strings.
     *
     * @param cards
     *            Array of cards (may be null or empty)
     * @return List of card display strings, or empty list
     */
    static List<String> cardsToList(Card[] cards) {
        if (cards == null || cards.length == 0)
            return Collections.emptyList();
        return Arrays.stream(cards).map(OutboundMessageConverter::cardToString).collect(Collectors.toList());
    }

    /**
     * Creates a CONNECTED message for when a player connects or reconnects.
     *
     * @param gameId
     *            Game ID
     * @param profileId
     *            Player's profile ID
     * @param snapshot
     *            Game state snapshot (may be null if game not started)
     * @param reconnectToken
     *            Game-scoped reconnect JWT (24h TTL) the client uses for
     *            reconnection
     * @return CONNECTED message
     */
    public ServerMessage createConnectedMessage(String gameId, long profileId, GameStateSnapshot snapshot,
            String reconnectToken) {
        ServerMessageData.GameStateData gameStateData = snapshot != null ? convertSnapshot(snapshot) : null;
        return ServerMessage.of(ServerMessageType.CONNECTED, gameId,
                new ServerMessageData.ConnectedData(profileId, gameStateData, reconnectToken));
    }

    /**
     * Creates a GAME_STATE message from a game state snapshot.
     *
     * @param gameId
     *            Game ID
     * @param snapshot
     *            Game state snapshot
     * @return GAME_STATE message
     */
    public ServerMessage createGameStateMessage(String gameId, GameStateSnapshot snapshot) {
        return ServerMessage.of(ServerMessageType.GAME_STATE, gameId, convertSnapshot(snapshot));
    }

    /**
     * Creates a HOLE_CARDS_DEALT message for the card owner.
     *
     * @param gameId
     *            Game ID
     * @param cards
     *            Hole cards
     * @return HOLE_CARDS_DEALT message
     */
    public ServerMessage createHoleCardsMessage(String gameId, Card[] cards) {
        return ServerMessage.of(ServerMessageType.HOLE_CARDS_DEALT, gameId,
                new ServerMessageData.HoleCardsDealtData(cardsToList(cards)));
    }

    /**
     * Creates an ACTION_REQUIRED message for the current player.
     *
     * @param gameId
     *            Game ID
     * @param options
     *            Available actions
     * @param timeoutSeconds
     *            Seconds before auto-action
     * @return ACTION_REQUIRED message
     */
    public ServerMessage createActionRequiredMessage(String gameId, ActionOptions options, int timeoutSeconds) {
        // canAllIn: true when the player can put all their chips in via bet or raise.
        // allInAmount: the player's remaining chip count, which equals maxBet when
        // betting or maxRaise when raising (both represent the stack in no-limit).
        boolean canAllIn = options.canBet() || options.canRaise();
        int allInAmount = options.canBet() ? options.maxBet() : options.maxRaise();
        ServerMessageData.ActionOptionsData optionsData = new ServerMessageData.ActionOptionsData(options.canFold(),
                options.canCheck(), options.canCall(), options.callAmount(), options.canBet(), options.minBet(),
                options.maxBet(), options.canRaise(), options.minRaise(), options.maxRaise(), canAllIn, allInAmount);
        return ServerMessage.of(ServerMessageType.ACTION_REQUIRED, gameId,
                new ServerMessageData.ActionRequiredData(timeoutSeconds, optionsData));
    }

    /**
     * Creates an ERROR message.
     *
     * @param gameId
     *            Game ID
     * @param code
     *            Error code
     * @param message
     *            Human-readable error message
     * @return ERROR message
     */
    public ServerMessage createErrorMessage(String gameId, String code, String message) {
        return ServerMessage.of(ServerMessageType.ERROR, gameId, new ServerMessageData.ErrorData(code, message));
    }

    /**
     * Creates a PLAYER_JOINED message.
     *
     * @param gameId
     *            Game ID
     * @param profileId
     *            Player's profile ID
     * @param playerName
     *            Player's name
     * @param seatIndex
     *            Seat index (-1 if unknown)
     * @param tableId
     *            Table ID (-1 if unknown)
     * @return PLAYER_JOINED message
     */
    public ServerMessage createPlayerJoinedMessage(String gameId, long profileId, String playerName, int seatIndex,
            int tableId) {
        return ServerMessage.of(ServerMessageType.PLAYER_JOINED, gameId,
                new ServerMessageData.PlayerJoinedData(profileId, playerName, seatIndex, tableId));
    }

    /**
     * Creates a PLAYER_LEFT message.
     *
     * @param gameId
     *            Game ID
     * @param profileId
     *            Player's profile ID
     * @param playerName
     *            Player's name
     * @return PLAYER_LEFT message
     */
    public ServerMessage createPlayerLeftMessage(String gameId, long profileId, String playerName) {
        return ServerMessage.of(ServerMessageType.PLAYER_LEFT, gameId,
                new ServerMessageData.PlayerLeftData(profileId, playerName));
    }

    /**
     * Creates a PLAYER_DISCONNECTED message (player lost connection; may
     * reconnect).
     *
     * @param gameId
     *            Game ID
     * @param profileId
     *            Player's profile ID
     * @param playerName
     *            Player's name
     * @return PLAYER_DISCONNECTED message
     */
    public ServerMessage createPlayerDisconnectedMessage(String gameId, long profileId, String playerName) {
        return ServerMessage.of(ServerMessageType.PLAYER_DISCONNECTED, gameId,
                new ServerMessageData.PlayerDisconnectedData(profileId, playerName));
    }

    /**
     * Creates a PLAYER_KICKED message.
     *
     * @param gameId
     *            Game ID
     * @param profileId
     *            Profile ID of kicked player
     * @param playerName
     *            Name of kicked player
     * @param reason
     *            Reason for kick
     * @return PLAYER_KICKED message
     */
    public ServerMessage createPlayerKickedMessage(String gameId, long profileId, String playerName, String reason) {
        return ServerMessage.of(ServerMessageType.PLAYER_KICKED, gameId,
                new ServerMessageData.PlayerKickedData(profileId, playerName, reason));
    }

    /**
     * Creates a GAME_PAUSED message.
     *
     * @param gameId
     *            Game ID
     * @param reason
     *            Pause reason
     * @param pausedBy
     *            Name of player who paused
     * @return GAME_PAUSED message
     */
    public ServerMessage createGamePausedMessage(String gameId, String reason, String pausedBy) {
        return ServerMessage.of(ServerMessageType.GAME_PAUSED, gameId,
                new ServerMessageData.GamePausedData(reason, pausedBy));
    }

    /**
     * Creates a GAME_RESUMED message.
     *
     * @param gameId
     *            Game ID
     * @param resumedBy
     *            Name of player who resumed
     * @return GAME_RESUMED message
     */
    public ServerMessage createGameResumedMessage(String gameId, String resumedBy) {
        return ServerMessage.of(ServerMessageType.GAME_RESUMED, gameId,
                new ServerMessageData.GameResumedData(resumedBy));
    }

    /**
     * Creates a CHAT_MESSAGE message.
     *
     * @param gameId
     *            Game ID
     * @param profileId
     *            Sender's profile ID
     * @param playerName
     *            Sender's name
     * @param message
     *            Chat message text
     * @param tableChat
     *            true for table chat, false for other channels
     * @return CHAT_MESSAGE message
     */
    public ServerMessage createChatMessage(String gameId, long profileId, String playerName, String message,
            boolean tableChat) {
        return ServerMessage.of(ServerMessageType.CHAT_MESSAGE, gameId,
                new ServerMessageData.ChatMessageData(profileId, playerName, message, tableChat));
    }

    /**
     * Converts a GameStateSnapshot to a GameStateData payload.
     *
     * This is the primary conversion that builds the full game state view including
     * table layout, player states, and (for the requesting player) hole cards.
     */
    private ServerMessageData.GameStateData convertSnapshot(GameStateSnapshot snapshot) {
        int dealerSeat = snapshot.dealerSeat();
        int sbSeat = snapshot.smallBlindSeat();
        int bbSeat = snapshot.bigBlindSeat();
        int actorSeat = snapshot.currentActorSeat();

        // Build seats from player states
        List<ServerMessageData.SeatData> seats = snapshot.players().stream()
                .map(p -> new ServerMessageData.SeatData(p.seat(), p.playerId(), p.playerName(), p.chipCount(),
                        p.folded() ? "FOLDED" : (p.allIn() ? "ALL_IN" : "ACTIVE"), p.seat() == dealerSeat,
                        p.seat() == sbSeat, p.seat() == bbSeat, p.currentBet(), cardsToList(p.holeCards()),
                        p.seat() == actorSeat))
                .collect(Collectors.toList());

        // Build pots
        List<ServerMessageData.PotData> pots = snapshot.pots().stream()
                .map(p -> new ServerMessageData.PotData(p.amount(),
                        p.eligiblePlayerIds().stream().map(Integer::longValue).collect(Collectors.toList())))
                .collect(Collectors.toList());

        // Build table data
        String round = snapshot.bettingRound() != null ? snapshot.bettingRound() : "PRE_FLOP";
        ServerMessageData.TableData tableData = new ServerMessageData.TableData(snapshot.tableId(), seats,
                cardsToList(snapshot.communityCards()), pots, round, snapshot.handNumber());

        // Build summary player list
        List<ServerMessageData.PlayerSummaryData> playerSummaries = snapshot.players().stream()
                .map(p -> new ServerMessageData.PlayerSummaryData(p.playerId(), p.playerName(), p.chipCount(),
                        snapshot.tableId(), p.seat(), null))
                .collect(Collectors.toList());

        return new ServerMessageData.GameStateData("IN_PROGRESS", snapshot.level(),
                new ServerMessageData.BlindsData(snapshot.smallBlind(), snapshot.bigBlind(), snapshot.ante()), null,
                List.of(tableData), playerSummaries);
    }
}
