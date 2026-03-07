/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.state.BettingRound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class GameStateProjectionTest {

    private ServerTournamentContext tournament;
    private ServerGameTable table;
    private List<ServerPlayer> players;

    @BeforeEach
    void setUp() {
        // Create 3 players
        players = new ArrayList<>();
        players.add(new ServerPlayer(1, "Alice", true, 0, 1000));
        players.add(new ServerPlayer(2, "Bob", false, 5, 1000));
        players.add(new ServerPlayer(3, "Charlie", false, 6, 1000));

        tournament = new ServerTournamentContext(players, 1, // 1 table
                1000, // starting chips
                new int[]{10}, // small blinds
                new int[]{20}, // big blinds
                new int[]{0}, // antes
                new int[]{10}, // level minutes
                new boolean[]{false}, // break levels
                false, // not practice
                0, // max rebuys
                0, // rebuy max level
                false, // no addons
                30 // timeout seconds
        );

        table = (ServerGameTable) tournament.getTable(0);
    }

    @Test
    void testProjectionWithoutHand() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        assertNotNull(snapshot);
        assertEquals(1, snapshot.tableId());
        assertNull(snapshot.myHoleCards());
        assertNull(snapshot.communityCards());
        assertEquals(3, snapshot.players().size());
    }

    @Test
    void testProjectionIncludesTableInfo() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        assertEquals(1, snapshot.tableId());
        assertEquals(table.getHandNum(), snapshot.handNumber());
    }

    @Test
    void testProjectionIncludesAllPlayers() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        assertEquals(3, snapshot.players().size());

        // Verify all players are included
        List<Integer> playerIds = snapshot.players().stream().map(GameStateSnapshot.PlayerState::playerId).toList();

        assertTrue(playerIds.contains(1));
        assertTrue(playerIds.contains(2));
        assertTrue(playerIds.contains(3));
    }

    @Test
    void testProjectionIncludesPlayerChipCounts() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        for (GameStateSnapshot.PlayerState playerState : snapshot.players()) {
            assertEquals(1000, playerState.chipCount());
        }
    }

    @Test
    void testProjectionIncludesPlayerNames() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        List<String> names = snapshot.players().stream().map(GameStateSnapshot.PlayerState::playerName).toList();

        assertTrue(names.contains("Alice"));
        assertTrue(names.contains("Bob"));
        assertTrue(names.contains("Charlie"));
    }

    @Test
    void testProjectionIncludesSeatNumbers() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        for (GameStateSnapshot.PlayerState playerState : snapshot.players()) {
            assertTrue(playerState.seat() >= 0);
            assertTrue(playerState.seat() < table.getSeats());
        }
    }

    @Test
    void testProjectionTracksPlayerStatus() {
        // Fold one player
        players.get(1).setFolded(true);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        GameStateSnapshot.PlayerState bobState = snapshot.players().stream().filter(p -> p.playerId() == 2).findFirst()
                .orElseThrow();

        assertTrue(bobState.folded());
    }

    @Test
    void testProjectionForDifferentPlayers() {
        // Both players should see the same public state
        GameStateSnapshot aliceSnapshot = GameStateProjection.forPlayer(table, null, 1);
        GameStateSnapshot bobSnapshot = GameStateProjection.forPlayer(table, null, 2);

        assertEquals(aliceSnapshot.tableId(), bobSnapshot.tableId());
        assertEquals(aliceSnapshot.players().size(), bobSnapshot.players().size());
    }

    @Test
    void testNoPotsWithoutHand() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);
        assertTrue(snapshot.pots().isEmpty());
    }

    @Test
    void testSecurityCriticalNoCardLeak() {
        // CRITICAL: Verify hole cards are null for other players without a hand
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        for (GameStateSnapshot.PlayerState playerState : snapshot.players()) {
            // Without a hand, all hole cards should be null
            assertNull(playerState.holeCards(), "SECURITY VIOLATION: Cards should be null without a hand!");
        }
    }

    @Test
    void testProjectionIncludesSittingOutPlayersWithFlag() {
        // Sitting-out players are included in the snapshot with sittingOut=true
        // so other clients can display them correctly (e.g. greyed-out seat).
        players.get(2).setSittingOut(true); // Charlie is sitting out

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        assertEquals(3, snapshot.players().size());
        GameStateSnapshot.PlayerState charlieState = snapshot.players().stream().filter(p -> p.playerId() == 3)
                .findFirst().orElseThrow();
        assertTrue(charlieState.sittingOut(), "Sitting-out player must have sittingOut=true");

        // Active players should not be sitting out
        GameStateSnapshot.PlayerState aliceState = snapshot.players().stream().filter(p -> p.playerId() == 1)
                .findFirst().orElseThrow();
        assertFalse(aliceState.sittingOut());
    }

    @Test
    void testForObserver_withoutHand_returnsAllPlayers() {
        GameStateSnapshot snapshot = GameStateProjection.forObserver(table, null);

        assertNotNull(snapshot);
        assertEquals(1, snapshot.tableId());
        assertEquals(3, snapshot.players().size());
        assertNull(snapshot.communityCards());
        assertTrue(snapshot.pots().isEmpty());
    }

    @Test
    void testForObserver_noHoleCardsExposed() {
        // Observers should never see any player's hole cards
        GameStateSnapshot snapshot = GameStateProjection.forObserver(table, null);

        for (GameStateSnapshot.PlayerState playerState : snapshot.players()) {
            assertNull(playerState.holeCards(), "SECURITY: Observer must never see hole cards");
        }
    }

    @Test
    void testForObserver_includesAllPlayerNames() {
        GameStateSnapshot snapshot = GameStateProjection.forObserver(table, null);

        List<String> names = snapshot.players().stream().map(GameStateSnapshot.PlayerState::playerName).toList();
        assertTrue(names.contains("Alice"));
        assertTrue(names.contains("Bob"));
        assertTrue(names.contains("Charlie"));
    }

    @Test
    void testForObserver_includesChipCounts() {
        GameStateSnapshot snapshot = GameStateProjection.forObserver(table, null);

        for (GameStateSnapshot.PlayerState playerState : snapshot.players()) {
            assertEquals(1000, playerState.chipCount());
        }
    }

    @Test
    void testForObserver_playerRankIsZero() {
        // Observer projections do not compute player rank
        GameStateSnapshot snapshot = GameStateProjection.forObserver(table, null);
        assertEquals(0, snapshot.playerRank());
    }

    @Test
    void testForShowdown_withoutHand_returnsAllPlayers() {
        GameStateSnapshot snapshot = GameStateProjection.forShowdown(table, null, 1);

        assertNotNull(snapshot);
        assertEquals(3, snapshot.players().size());
        assertNull(snapshot.myHoleCards());
        assertNull(snapshot.communityCards());
    }

    @Test
    void testForShowdown_noHoleCardsWithoutHand() {
        GameStateSnapshot snapshot = GameStateProjection.forShowdown(table, null, 1);

        for (GameStateSnapshot.PlayerState playerState : snapshot.players()) {
            assertNull(playerState.holeCards(), "Without an active hand, hole cards must be null at showdown");
        }
    }

    @Test
    void testForShowdown_currentActorSeatIsMinusOne() {
        // At showdown no player is acting
        GameStateSnapshot snapshot = GameStateProjection.forShowdown(table, null, 1);
        assertEquals(-1, snapshot.currentActorSeat());
    }

    // -------------------------------------------------------------------------
    // Tests with a mocked ServerHand (active hand in progress)
    // -------------------------------------------------------------------------

    private ServerHand createMockHand(BettingRound round, boolean done) {
        ServerHand hand = mock(ServerHand.class);
        when(hand.getRound()).thenReturn(round);
        when(hand.isDone()).thenReturn(done);
        when(hand.getSmallBlindSeat()).thenReturn(0);
        when(hand.getBigBlindSeat()).thenReturn(1);
        when(hand.getCurrentPlayerWithInit()).thenReturn(null);
        when(hand.getPots()).thenReturn(List.of());
        when(hand.getPendingBetTotal()).thenReturn(0);
        when(hand.getCommunityCards()).thenReturn(null);
        when(hand.getPlayerCards(any())).thenReturn(null);
        when(hand.getPlayerBet(anyInt())).thenReturn(0);
        return hand;
    }

    @Test
    void forPlayer_withHand_includesOwnHoleCards() {
        ServerHand hand = createMockHand(BettingRound.PRE_FLOP, false);
        Card[] aliceCards = {Card.HEARTS_A, Card.SPADES_K};
        when(hand.getPlayerCards(players.get(0))).thenReturn(aliceCards);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        assertArrayEquals(aliceCards, snapshot.myHoleCards());
    }

    @Test
    void forPlayer_withHand_hidesOtherPlayersHoleCards() {
        ServerHand hand = createMockHand(BettingRound.PRE_FLOP, false);
        Card[] aliceCards = {Card.HEARTS_A, Card.SPADES_K};
        Card[] bobCards = {Card.DIAMONDS_Q, Card.CLUBS_J};
        when(hand.getPlayerCards(players.get(0))).thenReturn(aliceCards);
        when(hand.getPlayerCards(players.get(1))).thenReturn(bobCards);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        // Alice should see her own cards in player state
        GameStateSnapshot.PlayerState aliceState = snapshot.players().stream().filter(p -> p.playerId() == 1)
                .findFirst().orElseThrow();
        assertNotNull(aliceState.holeCards());

        // Bob's hole cards must NOT be visible to Alice
        GameStateSnapshot.PlayerState bobState = snapshot.players().stream().filter(p -> p.playerId() == 2).findFirst()
                .orElseThrow();
        assertNull(bobState.holeCards(), "SECURITY: Other player's hole cards must be hidden");
    }

    @Test
    void forPlayer_withHand_includesCommunityCards() {
        ServerHand hand = createMockHand(BettingRound.FLOP, false);
        Card[] community = {Card.HEARTS_2, Card.DIAMONDS_7, Card.SPADES_T};
        when(hand.getCommunityCards()).thenReturn(community);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        assertArrayEquals(community, snapshot.communityCards());
    }

    @Test
    void forPlayer_withHand_includesBettingRound() {
        ServerHand hand = createMockHand(BettingRound.FLOP, false);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        assertEquals("FLOP", snapshot.bettingRound());
    }

    @Test
    void forPlayer_doneHand_reportsShowdownRound() {
        ServerHand hand = createMockHand(BettingRound.RIVER, true);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        assertEquals("SHOWDOWN", snapshot.bettingRound());
    }

    @Test
    void forPlayer_withHand_includesBlindSeats() {
        ServerHand hand = createMockHand(BettingRound.PRE_FLOP, false);
        when(hand.getSmallBlindSeat()).thenReturn(0);
        when(hand.getBigBlindSeat()).thenReturn(1);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        assertEquals(0, snapshot.smallBlindSeat());
        assertEquals(1, snapshot.bigBlindSeat());
    }

    @Test
    void forPlayer_withHand_includesCurrentActor() {
        ServerHand hand = createMockHand(BettingRound.PRE_FLOP, false);
        when(hand.getCurrentPlayerWithInit()).thenReturn(players.get(2)); // Charlie acts

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        assertEquals(players.get(2).getSeat(), snapshot.currentActorSeat());
    }

    @Test
    void forPlayer_withHand_includesPlayerBets() {
        ServerHand hand = createMockHand(BettingRound.PRE_FLOP, false);
        when(hand.getPlayerBet(1)).thenReturn(50);
        when(hand.getPlayerBet(2)).thenReturn(100);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        GameStateSnapshot.PlayerState aliceState = snapshot.players().stream().filter(p -> p.playerId() == 1)
                .findFirst().orElseThrow();
        assertEquals(50, aliceState.currentBet());
        GameStateSnapshot.PlayerState bobState = snapshot.players().stream().filter(p -> p.playerId() == 2).findFirst()
                .orElseThrow();
        assertEquals(100, bobState.currentBet());
    }

    @Test
    void forPlayer_withHand_includesPots() {
        ServerHand hand = createMockHand(BettingRound.FLOP, false);
        ServerPot pot = new ServerPot(0, 0);
        pot.addChips(players.get(0), 250);
        pot.addChips(players.get(1), 250);
        when(hand.getPots()).thenReturn(List.of(pot));

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        assertEquals(1, snapshot.pots().size());
        assertEquals(500, snapshot.pots().get(0).amount());
        assertTrue(snapshot.pots().get(0).eligiblePlayerIds().contains(1));
        assertTrue(snapshot.pots().get(0).eligiblePlayerIds().contains(2));
    }

    @Test
    void forPlayer_withHand_includesPendingBets() {
        ServerHand hand = createMockHand(BettingRound.PRE_FLOP, false);
        when(hand.getPots()).thenReturn(List.of());
        when(hand.getPendingBetTotal()).thenReturn(150);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        assertEquals(1, snapshot.pots().size());
        assertEquals(150, snapshot.pots().get(0).amount());
        assertTrue(snapshot.pots().get(0).eligiblePlayerIds().isEmpty());
    }

    @Test
    void forPlayer_sittingOutPlayer_noHoleCards() {
        players.get(0).setSittingOut(true); // Alice sitting out
        ServerHand hand = createMockHand(BettingRound.PRE_FLOP, false);
        Card[] aliceCards = {Card.HEARTS_A, Card.SPADES_K};
        when(hand.getPlayerCards(players.get(0))).thenReturn(aliceCards);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        // Even though cards exist, sitting out player shouldn't see them in player
        // state
        GameStateSnapshot.PlayerState aliceState = snapshot.players().stream().filter(p -> p.playerId() == 1)
                .findFirst().orElseThrow();
        assertNull(aliceState.holeCards(), "Sitting-out player should not see hole cards in seat");
    }

    @Test
    void forPlayer_sittingOutPlayer_zeroBet() {
        players.get(0).setSittingOut(true);
        ServerHand hand = createMockHand(BettingRound.PRE_FLOP, false);
        when(hand.getPlayerBet(1)).thenReturn(50); // bet exists but player sitting out

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, 1);

        GameStateSnapshot.PlayerState aliceState = snapshot.players().stream().filter(p -> p.playerId() == 1)
                .findFirst().orElseThrow();
        assertEquals(0, aliceState.currentBet(), "Sitting-out player should have 0 current bet");
    }

    @Test
    void forShowdown_withHand_revealsNonFoldedCards() {
        ServerHand hand = createMockHand(BettingRound.RIVER, false);
        Card[] aliceCards = {Card.HEARTS_A, Card.SPADES_K};
        Card[] bobCards = {Card.DIAMONDS_Q, Card.CLUBS_J};
        when(hand.getPlayerCards(players.get(0))).thenReturn(aliceCards);
        when(hand.getPlayerCards(players.get(1))).thenReturn(bobCards);
        // Charlie folded
        players.get(2).setFolded(true);
        when(hand.getPlayerCards(players.get(2))).thenReturn(new Card[]{Card.HEARTS_2, Card.CLUBS_3});

        GameStateSnapshot snapshot = GameStateProjection.forShowdown(table, hand, 1);

        // Alice's cards visible (not folded)
        GameStateSnapshot.PlayerState aliceState = snapshot.players().stream().filter(p -> p.playerId() == 1)
                .findFirst().orElseThrow();
        assertNotNull(aliceState.holeCards());

        // Bob's cards visible (not folded)
        GameStateSnapshot.PlayerState bobState = snapshot.players().stream().filter(p -> p.playerId() == 2).findFirst()
                .orElseThrow();
        assertNotNull(bobState.holeCards());

        // Charlie's cards hidden (folded)
        GameStateSnapshot.PlayerState charlieState = snapshot.players().stream().filter(p -> p.playerId() == 3)
                .findFirst().orElseThrow();
        assertNull(charlieState.holeCards(), "Folded player's cards should be hidden at showdown");
    }

    @Test
    void forShowdown_withHand_zeroCurrentBets() {
        ServerHand hand = createMockHand(BettingRound.RIVER, false);

        GameStateSnapshot snapshot = GameStateProjection.forShowdown(table, hand, 1);

        // At showdown all bets committed to pots, so current bet = 0
        for (GameStateSnapshot.PlayerState ps : snapshot.players()) {
            assertEquals(0, ps.currentBet());
        }
    }

    @Test
    void forObserver_withHand_noHoleCards() {
        ServerHand hand = createMockHand(BettingRound.FLOP, false);
        Card[] aliceCards = {Card.HEARTS_A, Card.SPADES_K};
        when(hand.getPlayerCards(players.get(0))).thenReturn(aliceCards);
        Card[] community = {Card.HEARTS_2, Card.DIAMONDS_7, Card.SPADES_T};
        when(hand.getCommunityCards()).thenReturn(community);

        GameStateSnapshot snapshot = GameStateProjection.forObserver(table, hand);

        // Observer sees community cards
        assertArrayEquals(community, snapshot.communityCards());
        // Observer sees NO hole cards
        assertNull(snapshot.myHoleCards());
        for (GameStateSnapshot.PlayerState ps : snapshot.players()) {
            assertNull(ps.holeCards(), "SECURITY: Observer must never see hole cards");
        }
    }

    @Test
    void forObserver_withHand_includesBettingRoundAndBlinds() {
        ServerHand hand = createMockHand(BettingRound.TURN, false);

        GameStateSnapshot snapshot = GameStateProjection.forObserver(table, hand);

        assertEquals("TURN", snapshot.bettingRound());
        assertEquals(0, snapshot.smallBlindSeat());
        assertEquals(1, snapshot.bigBlindSeat());
    }

    @Test
    void forObserver_doneHand_reportsShowdownRound() {
        ServerHand hand = createMockHand(BettingRound.RIVER, true);

        GameStateSnapshot snapshot = GameStateProjection.forObserver(table, hand);

        assertEquals("SHOWDOWN", snapshot.bettingRound());
    }

    @Test
    void forPlayer_tournamentStats_playerRank() {
        // Set different chip counts: Alice=500, Bob=1500, Charlie=1000
        players.get(0).setChipCount(500);
        players.get(1).setChipCount(1500);
        players.get(2).setChipCount(1000);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        // Alice has 500 chips, 2 players have more -> rank 3
        assertEquals(3, snapshot.playerRank());
    }

    @Test
    void forPlayer_tournamentStats_topPlayerRank1() {
        players.get(0).setChipCount(2000);
        players.get(1).setChipCount(500);
        players.get(2).setChipCount(500);

        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        assertEquals(1, snapshot.playerRank());
    }

    @Test
    void forPlayer_tournamentStats_totalAndRemaining() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        assertEquals(3, snapshot.totalPlayers());
        assertEquals(3, snapshot.playersRemaining());
        assertEquals(1, snapshot.numTables());
    }

    @Test
    void forPlayer_blindsFromTournament() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        assertEquals(10, snapshot.smallBlind());
        assertEquals(20, snapshot.bigBlind());
        assertEquals(0, snapshot.ante());
        assertEquals(0, snapshot.level());
    }

    @Test
    void testForObserver_tournamentStats_withContext() {
        // The projection should expose tournament-wide stats when a context is present
        GameStateSnapshot snapshot = GameStateProjection.forObserver(table, null);

        // With 3 active players (none finished, none sitting out), totalPlayers and
        // playersRemaining should be populated from the ServerTournamentContext.
        assertEquals(3, snapshot.totalPlayers());
        // numTables reflects the single table in this test setup
        assertEquals(1, snapshot.numTables());
    }
}
