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
package com.donohoedigital.games.poker.gameserver.websocket.message;

import java.util.List;

/**
 * Sealed interface hierarchy for server-to-client message payloads.
 *
 * Each record matches the master plan JSON spec exactly, ensuring consistent
 * wire format across all client implementations (desktop, web, mobile).
 */
public sealed interface ServerMessageData permits ServerMessageData.ConnectedData,ServerMessageData.GameStateData,ServerMessageData.HandStartedData,ServerMessageData.HoleCardsDealtData,ServerMessageData.CommunityCardsDealtData,ServerMessageData.ActionRequiredData,ServerMessageData.PlayerActedData,ServerMessageData.ActionTimeoutData,ServerMessageData.HandCompleteData,ServerMessageData.LevelChangedData,ServerMessageData.PlayerEliminatedData,ServerMessageData.RebuyOfferedData,ServerMessageData.AddonOfferedData,ServerMessageData.GameCompleteData,ServerMessageData.PlayerJoinedData,ServerMessageData.PlayerLeftData,ServerMessageData.PlayerDisconnectedData,ServerMessageData.PotAwardedData,ServerMessageData.ShowdownStartedData,ServerMessageData.PlayerRebuyData,ServerMessageData.PlayerAddonData,ServerMessageData.GamePausedData,ServerMessageData.GameResumedData,ServerMessageData.PlayerKickedData,ServerMessageData.ChatMessageData,ServerMessageData.TimerUpdateData,ServerMessageData.ErrorData {

    /**
     * Sent on successful WebSocket connection, includes full game state snapshot.
     */
    record ConnectedData(long playerId, GameStateData gameState) implements ServerMessageData {
    }

    /** Full game state snapshot. */
    record GameStateData(String status, int level, BlindsData blinds, Long nextLevelIn, List<TableData> tables,
            List<PlayerSummaryData> players) implements ServerMessageData {
    }

    /** New hand started. */
    record HandStartedData(int handNumber, int dealerSeat, int smallBlindSeat, int bigBlindSeat,
            List<BlindPostedData> blindsPosted) implements ServerMessageData {
    }

    /** Hole cards dealt to player (private, sent only to card owner). */
    record HoleCardsDealtData(List<String> cards) implements ServerMessageData {
    }

    /** Community cards dealt to all players. */
    record CommunityCardsDealtData(String round, List<String> cards,
            List<String> allCommunityCards) implements ServerMessageData {
    }

    /** Action required from a specific player (private). */
    record ActionRequiredData(int timeoutSeconds, ActionOptionsData options) implements ServerMessageData {
    }

    /** Player performed an action, broadcast to all. */
    record PlayerActedData(long playerId, String playerName, String action, int amount, int totalBet, int chipCount,
            int potTotal) implements ServerMessageData {
    }

    /** Player action timed out, auto-action performed. */
    record ActionTimeoutData(long playerId, String autoAction) implements ServerMessageData {
    }

    /** Hand complete with results. */
    record HandCompleteData(int handNumber, List<WinnerData> winners,
            List<ShowdownPlayerData> showdownPlayers) implements ServerMessageData {
    }

    /** Blind level changed. */
    record LevelChangedData(int level, int smallBlind, int bigBlind, int ante,
            Long nextLevelIn) implements ServerMessageData {
    }

    /** Player eliminated from tournament. */
    record PlayerEliminatedData(long playerId, String playerName, int finishPosition,
            int handsPlayed) implements ServerMessageData {
    }

    /** Rebuy offered to eliminated player (private). */
    record RebuyOfferedData(int cost, int chips, int timeoutSeconds) implements ServerMessageData {
    }

    /** Add-on offered to eligible players. */
    record AddonOfferedData(int cost, int chips, int timeoutSeconds) implements ServerMessageData {
    }

    /** Game/tournament complete with final standings. */
    record GameCompleteData(List<StandingData> standings, int totalHands, long duration) implements ServerMessageData {
    }

    /** Player joined the game. */
    record PlayerJoinedData(long playerId, String playerName, int seatIndex) implements ServerMessageData {
    }

    /** Player left the game intentionally. */
    record PlayerLeftData(long playerId, String playerName) implements ServerMessageData {
    }

    /** Player lost connection during game; may reconnect. */
    record PlayerDisconnectedData(long playerId, String playerName) implements ServerMessageData {
    }

    /** Pot won by one or more players. */
    record PotAwardedData(long[] winnerIds, int amount, int potIndex) implements ServerMessageData {
    }

    /** Showdown phase begins (cards to be revealed). */
    record ShowdownStartedData(int tableId) implements ServerMessageData {
    }

    /** Player purchased a rebuy. */
    record PlayerRebuyData(long playerId, String playerName, int addedChips) implements ServerMessageData {
    }

    /** Player purchased an add-on. */
    record PlayerAddonData(long playerId, String playerName, int addedChips) implements ServerMessageData {
    }

    /** Game paused. */
    record GamePausedData(String reason, String pausedBy) implements ServerMessageData {
    }

    /** Game resumed. */
    record GameResumedData(String resumedBy) implements ServerMessageData {
    }

    /** Player kicked by owner. */
    record PlayerKickedData(long playerId, String playerName, String reason) implements ServerMessageData {
    }

    /** Chat message. */
    record ChatMessageData(long playerId, String playerName, String message,
            boolean tableChat) implements ServerMessageData {
    }

    /** Timer update for current actor. */
    record TimerUpdateData(long playerId, int secondsRemaining) implements ServerMessageData {
    }

    /** Error occurred. */
    record ErrorData(String code, String message) implements ServerMessageData {
    }

    // ====================================
    // Nested data types
    // ====================================

    /** Blind level amounts. */
    record BlindsData(int small, int big, int ante) {
    }

    /** Seat/player state at a table. */
    record SeatData(int seatIndex, long playerId, String playerName, int chipCount, String status, boolean isDealer,
            boolean isSmallBlind, boolean isBigBlind, int currentBet, List<String> holeCards, boolean isCurrentActor) {
    }

    /** Table state. */
    record TableData(int tableId, List<SeatData> seats, List<String> communityCards, List<PotData> pots,
            String currentRound, int handNumber) {
    }

    /** Summary player data (for tournament-level view). */
    record PlayerSummaryData(long playerId, String name, int chipCount, int tableId, int seatIndex,
            Integer finishPosition) {
    }

    /** Pot data. */
    record PotData(int amount, List<Long> eligiblePlayers) {
    }

    /** Blind posted by player. */
    record BlindPostedData(long playerId, int amount, String type) {
    }

    /** Action options available to the player. */
    record ActionOptionsData(boolean canFold, boolean canCheck, boolean canCall, int callAmount, boolean canBet,
            int minBet, int maxBet, boolean canRaise, int minRaise, int maxRaise, boolean canAllIn, int allInAmount) {
    }

    /** Winner of a pot. */
    record WinnerData(long playerId, int amount, String hand, List<String> cards, int potIndex) {
    }

    /** Showdown player (all cards revealed). */
    record ShowdownPlayerData(long playerId, List<String> cards, String handDescription) {
    }

    /** Final standings entry. */
    record StandingData(int position, long playerId, String playerName, int prize) {
    }
}
