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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.event.PokerTableListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RemotePokerTable}.
 */
class RemotePokerTableTest {

    private RemotePokerTable table;
    private PokerGame mockGame;

    @BeforeAll
    static void initConfig() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    @BeforeEach
    void setUp() {
        mockGame = Mockito.mock(PokerGame.class);
        table = new RemotePokerTable(mockGame, 0);
    }

    @Test
    void initialStateIsEmpty() {
        assertThat(table.getNumOccupiedSeats()).isEqualTo(0);
        assertThat(table.getHoldemHand()).isNull();
        assertThat(table.getButton()).isEqualTo(ClientPokerTable.NO_SEAT);
    }

    @Test
    void updateFromStateSetsPlayers() {
        ClientPlayer[] players = new ClientPlayer[PokerConstants.SEATS];
        players[0] = new ClientPlayer(1, "Alice", true);
        players[1] = new ClientPlayer(2, "Bob", false);
        table.updateFromState(players, 0);

        assertThat(table.getPlayer(0)).isSameAs(players[0]);
        assertThat(table.getPlayer(1)).isSameAs(players[1]);
        assertThat(table.getPlayer(2)).isNull();
        assertThat(table.getNumOccupiedSeats()).isEqualTo(2);
        assertThat(table.getButton()).isEqualTo(0);
    }

    @Test
    void setRemotePlayerUpdatesIndividualSeat() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        table.setRemotePlayer(3, alice);

        assertThat(table.getPlayer(3)).isSameAs(alice);
        assertThat(table.getNumOccupiedSeats()).isEqualTo(1);
    }

    @Test
    void clearSeatRemovesPlayer() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        table.setRemotePlayer(2, alice);
        table.clearSeat(2);

        assertThat(table.getPlayer(2)).isNull();
        assertThat(table.getNumOccupiedSeats()).isEqualTo(0);
    }

    @Test
    void setRemoteHandIsReturnedByGetHoldemHand() {
        RemoteHoldemHand hand = new RemoteHoldemHand();
        table.setRemoteHand(hand);

        assertThat(table.getHoldemHand()).isSameAs(hand);
        assertThat(table.getRemoteHand()).isSameAs(hand);
    }

    @Test
    void fireEventNotifiesListeners() {
        List<PokerTableEvent> received = new ArrayList<>();
        table.addPokerTableListener(received::add, PokerTableEvent.TYPE_NEW_HAND);

        table.fireEvent(PokerTableEvent.TYPE_NEW_HAND);

        assertThat(received).hasSize(1);
        assertThat(received.get(0).getType()).isEqualTo(PokerTableEvent.TYPE_NEW_HAND);
    }

    @Test
    void setRemoteButtonUpdatesButton() {
        table.setRemoteButton(3);
        assertThat(table.getButton()).isEqualTo(3);
    }

    @Test
    void getPlayerBoundsCheck() {
        assertThat(table.getPlayer(-1)).isNull();
        assertThat(table.getPlayer(PokerConstants.SEATS)).isNull();
        assertThat(table.getPlayer(PokerConstants.SEATS - 1)).isNull(); // empty seat
    }

    // -------------------------------------------------------------------------
    // Seat offset / display seat
    // -------------------------------------------------------------------------

    @Test
    void getDisplaySeatIdentityWhenNoHumanPlayer() {
        // No human player → offset is 0 → identity mapping
        for (int i = 0; i < PokerConstants.SEATS; i++) {
            assertThat(table.getDisplaySeat(i)).isEqualTo(i);
        }
    }

    @Test
    void getTableSeatIsReverseOfGetDisplaySeat() {
        // Put a non-locally-controlled player in seat 2 (no offset change)
        ClientPlayer bob = new ClientPlayer(2, "Bob", false);
        table.setRemotePlayer(2, bob);

        for (int i = 0; i < PokerConstants.SEATS; i++) {
            int display = table.getDisplaySeat(i);
            assertThat(table.getTableSeat(display)).as("round-trip for seat %d", i).isEqualTo(i);
        }
    }

    // -------------------------------------------------------------------------
    // addPlayer
    // -------------------------------------------------------------------------

    @Test
    void addPlayerAddsToFirstAvailableSeat() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        table.addPlayer(alice);

        assertThat(table.getPlayer(0)).isSameAs(alice);
        assertThat(alice.getTable()).isSameAs(table);
        assertThat(alice.getSeat()).isEqualTo(0);
    }

    @Test
    void addPlayerSkipsOccupiedSeats() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        ClientPlayer bob = new ClientPlayer(2, "Bob", false);
        table.setRemotePlayer(0, alice);

        table.addPlayer(bob);

        assertThat(table.getPlayer(0)).isSameAs(alice);
        assertThat(table.getPlayer(1)).isSameAs(bob);
        assertThat(bob.getSeat()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // removePlayer
    // -------------------------------------------------------------------------

    @Test
    void removePlayerClearsSeat() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        table.setRemotePlayer(5, alice);
        table.removePlayer(5);

        assertThat(table.getPlayer(5)).isNull();
    }

    @Test
    void removePlayerOutOfBoundsIsNoOp() {
        // Should not throw
        table.removePlayer(-1);
        table.removePlayer(PokerConstants.SEATS);
        table.removePlayer(100);
    }

    // -------------------------------------------------------------------------
    // getNumOpenSeats
    // -------------------------------------------------------------------------

    @Test
    void getNumOpenSeatsAllEmpty() {
        assertThat(table.getNumOpenSeats()).isEqualTo(PokerConstants.SEATS);
    }

    @Test
    void getNumOpenSeatsSubtractsOccupied() {
        table.setRemotePlayer(0, new ClientPlayer(1, "Alice", true));
        table.setRemotePlayer(3, new ClientPlayer(2, "Bob", false));

        assertThat(table.getNumOpenSeats()).isEqualTo(PokerConstants.SEATS - 2);
    }

    // -------------------------------------------------------------------------
    // isAllComputer
    // -------------------------------------------------------------------------

    @Test
    void isAllComputerTrueWhenEmpty() {
        assertThat(table.isAllComputer()).isTrue();
    }

    @Test
    void isAllComputerTrueWhenOnlyNonHumanPlayers() {
        table.setRemotePlayer(0, new ClientPlayer(1, "Bot1", false));
        table.setRemotePlayer(1, new ClientPlayer(2, "Bot2", false));

        assertThat(table.isAllComputer()).isTrue();
    }

    @Test
    void isAllComputerFalseWhenHumanPresent() {
        table.setRemotePlayer(0, new ClientPlayer(1, "Bot1", false));
        table.setRemotePlayer(1, new ClientPlayer(2, "Human", true));

        assertThat(table.isAllComputer()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Hand number / min chip
    // -------------------------------------------------------------------------

    @Test
    void handNumDefaultsToZeroAndCanBeSet() {
        assertThat(table.getHandNum()).isEqualTo(0);
        table.setHandNum(42);
        assertThat(table.getHandNum()).isEqualTo(42);
    }

    @Test
    void minChipDefaultsToZeroAndCanBeSet() {
        assertThat(table.getMinChip()).isEqualTo(0);
        table.setMinChip(25);
        assertThat(table.getMinChip()).isEqualTo(25);
    }

    // -------------------------------------------------------------------------
    // Constant-return methods
    // -------------------------------------------------------------------------

    @Test
    void isCurrentAlwaysTrue() {
        assertThat(table.isCurrent()).isTrue();
    }

    @Test
    void isZipModeAlwaysFalse() {
        assertThat(table.isZipMode()).isFalse();
    }

    @Test
    void isRemoteTableAlwaysTrue() {
        assertThat(table.isRemoteTable()).isTrue();
    }

    @Test
    void getNumObserversAlwaysZero() {
        assertThat(table.getNumObservers()).isEqualTo(0);
    }

    @Test
    void getObserverAlwaysNull() {
        assertThat(table.getObserver(0)).isNull();
        assertThat(table.getObserver(5)).isNull();
    }

    @Test
    void isRebuyAllowedAlwaysFalse() {
        ClientPlayer player = new ClientPlayer(1, "Alice", true);
        assertThat(table.isRebuyAllowed(player)).isFalse();
        assertThat(table.isRebuyAllowed(player, 3)).isFalse();
    }

    @Test
    void isRebuyDoneAlwaysTrue() {
        ClientPlayer player = new ClientPlayer(1, "Alice", true);
        assertThat(table.isRebuyDone(player)).isTrue();
    }

    @Test
    void isAddonAllowedAlwaysFalse() {
        ClientPlayer player = new ClientPlayer(1, "Alice", true);
        assertThat(table.isAddonAllowed(player)).isFalse();
    }

    @Test
    void getRebuyListAlwaysEmpty() {
        assertThat(table.getRebuyList()).isEmpty();
    }

    @Test
    void getAddonListAlwaysEmpty() {
        assertThat(table.getAddonList()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Listener management
    // -------------------------------------------------------------------------

    @Test
    void removeListenerStopsNotifications() {
        List<PokerTableEvent> received = new ArrayList<>();
        PokerTableListener listener = received::add;
        table.addPokerTableListener(listener, PokerTableEvent.TYPE_NEW_HAND);

        table.removePokerTableListener(listener, PokerTableEvent.TYPE_NEW_HAND);
        table.fireEvent(PokerTableEvent.TYPE_NEW_HAND);

        assertThat(received).isEmpty();
    }

    @Test
    void addListenerMergesTypeFlags() {
        List<PokerTableEvent> received = new ArrayList<>();
        PokerTableListener listener = received::add;

        // Register for two types separately
        table.addPokerTableListener(listener, PokerTableEvent.TYPE_NEW_HAND);
        table.addPokerTableListener(listener, PokerTableEvent.TYPE_PLAYER_ACTION);

        table.fireEvent(PokerTableEvent.TYPE_NEW_HAND);
        table.fireEvent(PokerTableEvent.TYPE_PLAYER_ACTION);

        assertThat(received).hasSize(2);
    }

    @Test
    void listenerFilteredByType() {
        List<PokerTableEvent> received = new ArrayList<>();
        PokerTableListener listener = received::add;

        table.addPokerTableListener(listener, PokerTableEvent.TYPE_NEW_HAND);

        // Fire a different type — should NOT be received
        table.fireEvent(PokerTableEvent.TYPE_PLAYER_ACTION);

        assertThat(received).isEmpty();
    }

    // -------------------------------------------------------------------------
    // setRemoved
    // -------------------------------------------------------------------------

    @Test
    void setRemovedTrueFiresTableRemovedEvent() {
        List<PokerTableEvent> received = new ArrayList<>();
        table.addPokerTableListener(received::add, PokerTableEvent.TYPE_TABLE_REMOVED);

        table.setRemoved(true);

        assertThat(received).hasSize(1);
        assertThat(received.get(0).getType()).isEqualTo(PokerTableEvent.TYPE_TABLE_REMOVED);
    }

    @Test
    void setRemovedFalseDoesNotFireEvent() {
        List<PokerTableEvent> received = new ArrayList<>();
        table.addPokerTableListener(received::add, PokerTableEvent.TYPE_TABLE_REMOVED);

        table.setRemoved(false);

        assertThat(received).isEmpty();
    }

    // -------------------------------------------------------------------------
    // updateFromState links table to players
    // -------------------------------------------------------------------------

    @Test
    void updateFromStateSetsTableOnPlayers() {
        ClientPlayer[] players = new ClientPlayer[PokerConstants.SEATS];
        players[3] = new ClientPlayer(1, "Alice", true);

        table.updateFromState(players, 3);

        assertThat(players[3].getTable()).isSameAs(table);
        assertThat(players[3].getSeat()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // setRemoteHand links hand to table
    // -------------------------------------------------------------------------

    @Test
    void setRemoteHandLinksHandToTable() {
        RemoteHoldemHand hand = new RemoteHoldemHand();
        table.setRemoteHand(hand);

        assertThat(hand.getClientTable()).isSameAs(table);
    }

    @Test
    void setRemoteHandNullDoesNotThrow() {
        // Should not throw
        table.setRemoteHand(null);
        assertThat(table.getHoldemHand()).isNull();
    }
}
