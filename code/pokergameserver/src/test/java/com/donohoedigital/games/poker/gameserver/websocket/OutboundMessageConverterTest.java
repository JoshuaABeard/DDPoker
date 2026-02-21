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

import com.donohoedigital.config.ConfigTestHelper;
import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.gameserver.GameStateSnapshot;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OutboundMessageConverter.
 */
class OutboundMessageConverterTest {

    private OutboundMessageConverter converter;

    @BeforeAll
    static void initConfig() {
        ConfigTestHelper.initializeForTesting("poker");
    }

    @BeforeEach
    void setUp() {
        converter = new OutboundMessageConverter();
    }

    @Test
    void cardToString_returnsCardDisplay() {
        assertEquals("As", OutboundMessageConverter.cardToString(Card.SPADES_A));
        // Ten must serialize as "T" not "10" so Card.getCard() can round-trip it
        assertEquals("Ts", OutboundMessageConverter.cardToString(Card.SPADES_T));
    }

    @Test
    void cardToString_nullReturnsEmpty() {
        assertNull(OutboundMessageConverter.cardToString(null));
    }

    @Test
    void createConnectedMessage_hasCorrectType() {
        GameStateSnapshot snapshot = new GameStateSnapshot(1, 5, null, new Card[0], List.of(), List.of(), -1, -1, -1,
                -1, null, 0, 0, 0, 0);

        ServerMessage message = converter.createConnectedMessage("game-1", 42L, snapshot, null);

        assertEquals(ServerMessageType.CONNECTED, message.type());
        assertEquals("game-1", message.gameId());
        assertNotNull(message.timestamp());
    }

    @Test
    void createConnectedMessage_includesPlayerId() {
        GameStateSnapshot snapshot = new GameStateSnapshot(1, 5, null, new Card[0], List.of(), List.of(), -1, -1, -1,
                -1, null, 0, 0, 0, 0);

        ServerMessage message = converter.createConnectedMessage("game-1", 42L, snapshot, null);

        ServerMessageData.ConnectedData data = (ServerMessageData.ConnectedData) message.data();
        assertEquals(42L, data.playerId());
    }

    @Test
    void createConnectedMessage_nullSnapshotYieldsNullGameState() {
        ServerMessage message = converter.createConnectedMessage("game-1", 42L, null, null);

        ServerMessageData.ConnectedData data = (ServerMessageData.ConnectedData) message.data();
        assertEquals(42L, data.playerId());
        assertNull(data.gameState());
    }

    @Test
    void createHoleCardsMessage_returnsPrivateMessage() {
        Card[] cards = {Card.HEARTS_A, Card.DIAMONDS_K};

        ServerMessage message = converter.createHoleCardsMessage("game-1", cards);

        assertEquals(ServerMessageType.HOLE_CARDS_DEALT, message.type());
        ServerMessageData.HoleCardsDealtData data = (ServerMessageData.HoleCardsDealtData) message.data();
        assertEquals(2, data.cards().size());
        assertEquals("Ah", data.cards().get(0));
        assertEquals("Kd", data.cards().get(1));
    }

    @Test
    void createActionRequiredMessage_mapsAllOptions() {
        ActionOptions options = new ActionOptions(true, false, true, false, true, 0, 200, 1000, 0, 0, 30);

        ServerMessage message = converter.createActionRequiredMessage("game-1", options, 30);

        assertEquals(ServerMessageType.ACTION_REQUIRED, message.type());
        ServerMessageData.ActionRequiredData data = (ServerMessageData.ActionRequiredData) message.data();
        assertEquals(30, data.timeoutSeconds());
        assertTrue(data.options().canCheck());
        assertFalse(data.options().canCall());
        assertTrue(data.options().canBet());
        assertFalse(data.options().canRaise());
        assertTrue(data.options().canFold());
        assertEquals(200, data.options().minBet());
        assertEquals(1000, data.options().maxBet());
        // canBet=true: canAllIn must be true and allInAmount must equal maxBet
        assertTrue(data.options().canAllIn());
        assertEquals(1000, data.options().allInAmount());
    }

    @Test
    void createActionRequiredMessage_allInAmountUsesMaxRaiseWhenOnlyRaisePossible() {
        // canBet=false, canRaise=true: all-in via raise with maxRaise amount
        ActionOptions options = new ActionOptions(false, true, false, true, true, 50, 0, 0, 100, 800, 30);

        ServerMessage message = converter.createActionRequiredMessage("game-1", options, 30);

        ServerMessageData.ActionRequiredData data = (ServerMessageData.ActionRequiredData) message.data();
        assertTrue(data.options().canAllIn());
        assertEquals(800, data.options().allInAmount());
    }

    @Test
    void createActionRequiredMessage_canAllInFalseWhenNoBetOrRaise() {
        // canBet=false, canRaise=false: player can only check/call/fold
        ActionOptions options = new ActionOptions(true, true, false, false, true, 50, 0, 0, 0, 0, 30);

        ServerMessage message = converter.createActionRequiredMessage("game-1", options, 30);

        ServerMessageData.ActionRequiredData data = (ServerMessageData.ActionRequiredData) message.data();
        assertFalse(data.options().canAllIn());
        assertEquals(0, data.options().allInAmount());
    }

    @Test
    void createErrorMessage_hasCorrectStructure() {
        ServerMessage message = converter.createErrorMessage("game-1", "INVALID_ACTION", "It is not your turn");

        assertEquals(ServerMessageType.ERROR, message.type());
        ServerMessageData.ErrorData data = (ServerMessageData.ErrorData) message.data();
        assertEquals("INVALID_ACTION", data.code());
        assertEquals("It is not your turn", data.message());
    }

    @Test
    void createPlayerJoinedMessage_hasCorrectStructure() {
        ServerMessage message = converter.createPlayerJoinedMessage("game-1", 99L, "TestPlayer", 3, 0);

        assertEquals(ServerMessageType.PLAYER_JOINED, message.type());
        ServerMessageData.PlayerJoinedData data = (ServerMessageData.PlayerJoinedData) message.data();
        assertEquals(99L, data.playerId());
        assertEquals("TestPlayer", data.playerName());
        assertEquals(3, data.seatIndex());
        assertEquals(0, data.tableId());
    }

    @Test
    void createPlayerLeftMessage_hasCorrectStructure() {
        ServerMessage message = converter.createPlayerLeftMessage("game-1", 99L, "TestPlayer");

        assertEquals(ServerMessageType.PLAYER_LEFT, message.type());
        ServerMessageData.PlayerLeftData data = (ServerMessageData.PlayerLeftData) message.data();
        assertEquals(99L, data.playerId());
        assertEquals("TestPlayer", data.playerName());
    }

    @Test
    void createGameStateMessage_convertsSnapshotCorrectly() {
        Card[] holeCards = {Card.HEARTS_A, Card.DIAMONDS_K};
        Card[] communityCards = {Card.SPADES_J, Card.CLUBS_T};

        List<GameStateSnapshot.PlayerState> players = List.of(
                new GameStateSnapshot.PlayerState(1, "Player1", 1000, 0, false, false, holeCards, 100),
                new GameStateSnapshot.PlayerState(2, "Player2", 2000, 1, false, false, null, 0));
        List<GameStateSnapshot.PotState> pots = List.of(new GameStateSnapshot.PotState(500, List.of(1, 2)));
        // dealerSeat=0, sbSeat=1, bbSeat=-1, actorSeat=-1, round="FLOP", level=2,
        // sb=50, bb=100, ante=0
        GameStateSnapshot snapshot = new GameStateSnapshot(0, 3, holeCards, communityCards, players, pots, 0, 1, -1, -1,
                "FLOP", 2, 50, 100, 0);

        ServerMessage message = converter.createGameStateMessage("game-1", snapshot);

        assertEquals(ServerMessageType.GAME_STATE, message.type());
        ServerMessageData.GameStateData data = (ServerMessageData.GameStateData) message.data();
        assertNotNull(data.tables());
        assertEquals(1, data.tables().size());

        // Community cards should be in the table data
        ServerMessageData.TableData table = data.tables().get(0);
        assertEquals(2, table.communityCards().size());
        assertEquals("Js", table.communityCards().get(0));
        assertEquals("Tc", table.communityCards().get(1));

        // Player's own hole cards should be included
        ServerMessageData.SeatData seat0 = table.seats().get(0);
        assertNotNull(seat0.holeCards());
        assertEquals(2, seat0.holeCards().size());
        assertEquals("Ah", seat0.holeCards().get(0));

        // Other player's hole cards should NOT be included
        ServerMessageData.SeatData seat1 = table.seats().get(1);
        assertTrue(seat1.holeCards() == null || seat1.holeCards().isEmpty());

        // Current bet should be passed through from the snapshot
        assertEquals(100, seat0.currentBet());
        assertEquals(0, seat1.currentBet());

        // Dealer/blind flags populated from snapshot (dealerSeat=0, sbSeat=1,
        // bbSeat=-1)
        assertTrue(seat0.isDealer());
        assertFalse(seat0.isSmallBlind());
        assertFalse(seat0.isBigBlind());
        assertFalse(seat1.isDealer());
        assertTrue(seat1.isSmallBlind());
        assertFalse(seat1.isBigBlind());

        // Betting round, level, and blinds from snapshot
        assertEquals("FLOP", table.currentRound());
        assertEquals(2, data.level());
        assertEquals(50, data.blinds().small());
        assertEquals(100, data.blinds().big());
        assertEquals(0, data.blinds().ante());
    }
}
