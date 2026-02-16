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
        assertEquals(0, snapshot.tableId());
        assertNull(snapshot.myHoleCards());
        assertNull(snapshot.communityCards());
        assertEquals(3, snapshot.players().size());
    }

    @Test
    void testProjectionIncludesTableInfo() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, null, 1);

        assertEquals(0, snapshot.tableId());
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
}
