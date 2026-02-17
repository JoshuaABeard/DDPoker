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
package com.donohoedigital.games.poker.gameserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security test: verifies hole card privacy guarantees in GameStateProjection.
 *
 * <p>
 * CRITICAL: Hole cards must NEVER appear in the wrong player's view. All game
 * state sent to a player must go through GameStateProjection.
 */
class CardPrivacyTest {

    private ServerGameTable table;
    private ServerPlayer alice;
    private ServerPlayer bob;
    private ServerPlayer charlie;
    private ServerHand hand;

    @BeforeEach
    void setUp() {
        List<ServerPlayer> players = new ArrayList<>();
        alice = new ServerPlayer(1, "Alice", true, 0, 1000);
        bob = new ServerPlayer(2, "Bob", true, 0, 1000);
        charlie = new ServerPlayer(3, "Charlie", true, 0, 1000);
        players.add(alice);
        players.add(bob);
        players.add(charlie);

        ServerTournamentContext tournament = new ServerTournamentContext(players, 1, 1000, new int[]{10}, new int[]{20},
                new int[]{0}, new int[]{10}, new boolean[]{false}, false, 0, 0, false, 30);

        table = (ServerGameTable) tournament.getTable(0);
        hand = new ServerHand(table, 1, 10, 20, 0, 0, 1, 2);
        hand.deal();
    }

    // ============================================================
    // forPlayer() privacy tests — normal game (not showdown)
    // ============================================================

    @Test
    void forPlayer_requestingPlayerSeesOwnCards() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, alice.getID());

        assertNotNull(snapshot.myHoleCards());
        assertEquals(2, snapshot.myHoleCards().length);
    }

    @Test
    void forPlayer_requestingPlayerStateHasCards() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, alice.getID());

        GameStateSnapshot.PlayerState aliceState = findPlayer(snapshot, alice.getID());
        assertNotNull(aliceState.holeCards());
        assertEquals(2, aliceState.holeCards().length);
    }

    @Test
    void forPlayer_otherPlayersCardsHidden() {
        GameStateSnapshot snapshot = GameStateProjection.forPlayer(table, hand, alice.getID());

        assertNull(findPlayer(snapshot, bob.getID()).holeCards(),
                "SECURITY VIOLATION: Bob's cards must be hidden from Alice");
        assertNull(findPlayer(snapshot, charlie.getID()).holeCards(),
                "SECURITY VIOLATION: Charlie's cards must be hidden from Alice");
    }

    @Test
    void forPlayer_eachPlayerSeesOnlyOwnCards() {
        GameStateSnapshot aliceView = GameStateProjection.forPlayer(table, hand, alice.getID());
        GameStateSnapshot bobView = GameStateProjection.forPlayer(table, hand, bob.getID());
        GameStateSnapshot charlieView = GameStateProjection.forPlayer(table, hand, charlie.getID());

        // Alice sees only her own cards
        assertNotNull(findPlayer(aliceView, alice.getID()).holeCards());
        assertNull(findPlayer(aliceView, bob.getID()).holeCards());
        assertNull(findPlayer(aliceView, charlie.getID()).holeCards());

        // Bob sees only his own cards
        assertNull(findPlayer(bobView, alice.getID()).holeCards());
        assertNotNull(findPlayer(bobView, bob.getID()).holeCards());
        assertNull(findPlayer(bobView, charlie.getID()).holeCards());

        // Charlie sees only his own cards
        assertNull(findPlayer(charlieView, alice.getID()).holeCards());
        assertNull(findPlayer(charlieView, bob.getID()).holeCards());
        assertNotNull(findPlayer(charlieView, charlie.getID()).holeCards());
    }

    // ============================================================
    // forShowdown() card reveal tests
    // ============================================================

    @Test
    void forShowdown_nonFoldedPlayersCardsRevealed() {
        // Nobody is folded — all cards should be visible at showdown
        GameStateSnapshot snapshot = GameStateProjection.forShowdown(table, hand, alice.getID());

        assertNotNull(findPlayer(snapshot, alice.getID()).holeCards());
        assertNotNull(findPlayer(snapshot, bob.getID()).holeCards());
        assertNotNull(findPlayer(snapshot, charlie.getID()).holeCards());
    }

    @Test
    void forShowdown_foldedPlayerCardsHidden() {
        bob.setFolded(true);

        GameStateSnapshot snapshot = GameStateProjection.forShowdown(table, hand, alice.getID());

        // Non-folded players' cards are revealed
        assertNotNull(findPlayer(snapshot, alice.getID()).holeCards());
        assertNotNull(findPlayer(snapshot, charlie.getID()).holeCards());

        // Folded player's cards stay hidden at showdown
        assertNull(findPlayer(snapshot, bob.getID()).holeCards(),
                "SECURITY VIOLATION: Folded player's cards must not be revealed at showdown");
    }

    @Test
    void forShowdown_myHoleCardsAlwaysVisible() {
        // Even for a folded player, their own myHoleCards are returned
        bob.setFolded(true);

        GameStateSnapshot snapshot = GameStateProjection.forShowdown(table, hand, bob.getID());

        assertNotNull(snapshot.myHoleCards());
    }

    @Test
    void forShowdown_differentFromForPlayer_whenOthersActive() {
        GameStateSnapshot normalView = GameStateProjection.forPlayer(table, hand, alice.getID());
        GameStateSnapshot showdownView = GameStateProjection.forShowdown(table, hand, alice.getID());

        // Normal view hides other players' cards
        assertNull(findPlayer(normalView, bob.getID()).holeCards());
        assertNull(findPlayer(normalView, charlie.getID()).holeCards());

        // Showdown view reveals non-folded players' cards
        assertNotNull(findPlayer(showdownView, bob.getID()).holeCards());
        assertNotNull(findPlayer(showdownView, charlie.getID()).holeCards());
    }

    // ============================================================
    // Helper
    // ============================================================

    private GameStateSnapshot.PlayerState findPlayer(GameStateSnapshot snapshot, int playerId) {
        return snapshot.players().stream().filter(p -> p.playerId() == playerId).findFirst()
                .orElseThrow(() -> new AssertionError("Player " + playerId + " not found in snapshot"));
    }
}
