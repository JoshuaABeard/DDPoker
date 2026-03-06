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

import com.donohoedigital.games.poker.HandAction;
import com.donohoedigital.games.poker.display.ClientBettingRound;
import com.donohoedigital.games.poker.display.ClientCard;
import com.donohoedigital.games.poker.display.ClientHand;
import com.donohoedigital.games.poker.protocol.message.ServerMessageData.ActionOptionsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RemoteHoldemHand}.
 */
class RemoteHoldemHandTest {

    private RemoteHoldemHand hand;

    @BeforeEach
    void setUp() {
        hand = new RemoteHoldemHand();
    }

    @Test
    void initialStateIsPreFlop() {
        assertThat(hand.getRound()).isEqualTo(ClientBettingRound.PRE_FLOP);
        assertThat(hand.getNumPlayers()).isEqualTo(0);
        assertThat(hand.getCurrentPlayerIndex()).isEqualTo(RemoteHoldemHand.NO_CURRENT_PLAYER);
        assertThat(hand.getCurrentPlayer()).isNull();
        assertThat(hand.getTotalPotChipCount()).isEqualTo(0);
    }

    @Test
    void updateRoundChangesRound() {
        hand.updateRound(ClientBettingRound.FLOP);
        assertThat(hand.getRound()).isEqualTo(ClientBettingRound.FLOP);

        hand.updateRound(ClientBettingRound.TURN);
        assertThat(hand.getRound()).isEqualTo(ClientBettingRound.TURN);

        hand.updateRound(ClientBettingRound.RIVER);
        assertThat(hand.getRound()).isEqualTo(ClientBettingRound.RIVER);
    }

    @Test
    void updateCommunityStoresCommunity() {
        ClientHand community = ClientHand.empty();
        // Cards as strings like "Ah" = Ace of Hearts
        community.addCard(ClientCard.getCard("Ah"));
        community.addCard(ClientCard.getCard("Kd"));
        community.addCard(ClientCard.getCard("2c"));

        hand.updateCommunity(community);

        assertThat(hand.getCommunity()).isSameAs(community);
        assertThat(hand.getCommunity().size()).isEqualTo(3);
    }

    @Test
    void updateCommunityWithNullCreatesEmptyHand() {
        hand.updateCommunity(null);
        assertThat(hand.getCommunity()).isNotNull();
        assertThat(hand.getCommunity().size()).isEqualTo(0);
    }

    @Test
    void updatePlayerOrderAndCurrentPlayer() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        ClientPlayer bob = new ClientPlayer(2, "Bob", false);
        hand.updatePlayerOrder(List.of(alice, bob));

        assertThat(hand.getNumPlayers()).isEqualTo(2);
        assertThat(hand.getPlayerAt(0)).isSameAs(alice);
        assertThat(hand.getPlayerAt(1)).isSameAs(bob);

        hand.updateCurrentPlayer(1);
        assertThat(hand.getCurrentPlayerIndex()).isEqualTo(1);
        assertThat(hand.getCurrentPlayer()).isSameAs(bob);
    }

    @Test
    void getPlayerAtBoundsCheck() {
        assertThat(hand.getPlayerAt(-1)).isNull();
        assertThat(hand.getPlayerAt(0)).isNull(); // empty list
    }

    @Test
    void updatePotChangesPotTotal() {
        hand.updatePot(1500);
        assertThat(hand.getTotalPotChipCount()).isEqualTo(1500);
    }

    @Test
    void currentPlayerNullWhenNoPlayers() {
        hand.updateCurrentPlayer(0);
        assertThat(hand.getCurrentPlayer()).isNull(); // player list is empty
    }

    @Test
    void currentPlayerIndexClampsOnPlayerOrderUpdate() {
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        ClientPlayer bob = new ClientPlayer(2, "Bob", false);
        hand.updatePlayerOrder(List.of(alice, bob));
        hand.updateCurrentPlayer(1); // valid index

        // Replace with smaller list — index 1 is now out of bounds
        hand.updatePlayerOrder(List.of(alice));

        assertThat(hand.getCurrentPlayerIndex()).isEqualTo(RemoteHoldemHand.NO_CURRENT_PLAYER);
    }

    @Test
    void hasPlayerActedReturnsFalseWithoutNpe() {
        // history_ is null in no-arg constructor — must not throw NPE
        ClientPlayer alice = new ClientPlayer(1, "Alice", true);
        assertThat(hand.hasPlayerActed(alice)).isFalse();
    }

    @Test
    void getHistoryCopyReturnsEmptyListWithoutNpe() {
        // history_ is null in no-arg constructor — must not throw NPE
        List<HandAction> copy = hand.getHistoryCopy();
        assertThat(copy).isNotNull().isEmpty();
    }

    @Test
    void getHistorySizeReturnsZeroWithoutNpe() {
        // history_ is null in no-arg constructor — must not throw NPE
        assertThat(hand.getHistorySize()).isEqualTo(0);
    }

    // =========================================================================
    // Bet tracking
    // =========================================================================

    @Nested
    class BetTracking {

        @Test
        void updatePlayerBetTracksPerPlayerBets() {
            hand.updatePlayerBet(1, 100);
            hand.updatePlayerBet(2, 200);

            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            ClientPlayer bob = new ClientPlayer(2, "Bob", false);

            assertThat(hand.getBet(alice)).isEqualTo(100);
            assertThat(hand.getBet(bob)).isEqualTo(200);
        }

        @Test
        void updatePlayerBetZeroRemovesEntry() {
            hand.updatePlayerBet(1, 100);
            hand.updatePlayerBet(1, 0);

            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.getBet(alice)).isEqualTo(0);
        }

        @Test
        void getBetReturnsHighestBetAcrossAllPlayers() {
            hand.updatePlayerBet(1, 100);
            hand.updatePlayerBet(2, 300);
            hand.updatePlayerBet(3, 200);

            assertThat(hand.getBet()).isEqualTo(300);
        }

        @Test
        void getBetReturnsZeroWhenNoBets() {
            assertThat(hand.getBet()).isEqualTo(0);
        }

        @Test
        void clearBetsRemovesAllBets() {
            hand.updatePlayerBet(1, 100);
            hand.updatePlayerBet(2, 200);
            hand.clearBets();

            assertThat(hand.getBet()).isEqualTo(0);

            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.getBet(alice)).isEqualTo(0);
        }

        @Test
        void getBetWithRoundIgnoresRound() {
            hand.updatePlayerBet(1, 150);
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);

            assertThat(hand.getBet(alice, ClientBettingRound.PRE_FLOP.toLegacy())).isEqualTo(150);
            assertThat(hand.getBet(alice, ClientBettingRound.FLOP.toLegacy())).isEqualTo(150);
        }
    }

    // =========================================================================
    // Call calculation
    // =========================================================================

    @Nested
    class CallCalculation {

        @Test
        void getCallFallbackComputesFromBets() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            ClientPlayer bob = new ClientPlayer(2, "Bob", false);
            hand.updatePlayerBet(1, 50);
            hand.updatePlayerBet(2, 200);

            // alice needs 200 - 50 = 150 to call
            assertThat(hand.getCall(alice)).isEqualTo(150);
        }

        @Test
        void getCallFallbackNeverNegative() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            hand.updatePlayerBet(1, 200);
            // alice has the highest bet, call should be 0 not negative
            assertThat(hand.getCall(alice)).isEqualTo(0);
        }

        @Test
        void getCallUsesActionOptionsWhenAvailable() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            hand.updatePlayerBet(1, 50);
            hand.updatePlayerBet(2, 200);

            // Set action options with callAmount = 75 (overrides computed 150)
            ActionOptionsData opts = new ActionOptionsData(false, false, true, 75, false, 0, 0, false, 0, 0, false, 0);
            hand.updateActionOptions(opts);

            assertThat(hand.getCall(alice)).isEqualTo(75);
        }
    }

    // =========================================================================
    // Min/max bet/raise from action options
    // =========================================================================

    @Nested
    class MinMaxBetRaise {

        @Test
        void returnsZeroWithoutOptions() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);

            assertThat(hand.getMinBet()).isEqualTo(0);
            assertThat(hand.getMinRaise()).isEqualTo(0);
            assertThat(hand.getMaxBet(alice)).isEqualTo(0);
            assertThat(hand.getMaxRaise(alice)).isEqualTo(0);
        }

        @Test
        void returnsOptionValuesWhenSet() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);

            ActionOptionsData opts = new ActionOptionsData(false, false, false, 0, true, 100, 500, true, 200, 1000,
                    false, 0);
            hand.updateActionOptions(opts);

            assertThat(hand.getMinBet()).isEqualTo(100);
            assertThat(hand.getMaxBet(alice)).isEqualTo(500);
            assertThat(hand.getMinRaise()).isEqualTo(200);
            assertThat(hand.getMaxRaise(alice)).isEqualTo(1000);
        }
    }

    // =========================================================================
    // Pot odds
    // =========================================================================

    @Nested
    class PotOdds {

        @Test
        void potOddsComputesCorrectly() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            hand.updatePlayerBet(2, 100); // someone bet 100
            hand.updatePot(300);

            // call = 100 (highBet - alice's 0), pot = 300
            // potOdds = 100 * 100 / (100 + 300) = 25.0
            assertThat(hand.getPotOdds(alice)).isEqualTo(25.0f);
        }

        @Test
        void potOddsReturnsZeroWhenCallIsZero() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            hand.updatePot(500);
            // no bets, so call = 0
            assertThat(hand.getPotOdds(alice)).isEqualTo(0.0f);
        }
    }

    // =========================================================================
    // Win tracking
    // =========================================================================

    @Nested
    class WinTracking {

        @Test
        void winsAccumulatesForSplitPots() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            hand.wins(alice, 200, 0);
            hand.wins(alice, 150, 1);

            assertThat(hand.getWin(alice)).isEqualTo(350);
        }

        @Test
        void getWinReturnsZeroForNoWins() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.getWin(alice)).isEqualTo(0);
        }

        @Test
        void clearWinsResets() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            hand.wins(alice, 500, 0);
            hand.clearWins();

            assertThat(hand.getWin(alice)).isEqualTo(0);
        }
    }

    // =========================================================================
    // Blind / ante accessors
    // =========================================================================

    @Nested
    class BlindAnteAccessors {

        @Test
        void smallBlindSetAndGet() {
            hand.setSmallBlind(25);
            assertThat(hand.getSmallBlind()).isEqualTo(25);
        }

        @Test
        void bigBlindSetAndGet() {
            hand.setBigBlind(50);
            assertThat(hand.getBigBlind()).isEqualTo(50);
        }

        @Test
        void anteSetAndGet() {
            hand.setAnte(10);
            assertThat(hand.getAnte()).isEqualTo(10);
        }

        @Test
        void smallBlindSeatUpdateAndGet() {
            assertThat(hand.getRemoteSmallBlindSeat()).isEqualTo(RemoteHoldemHand.NO_CURRENT_PLAYER);
            hand.updateSmallBlindSeat(3);
            assertThat(hand.getRemoteSmallBlindSeat()).isEqualTo(3);
        }

        @Test
        void bigBlindSeatUpdateAndGet() {
            assertThat(hand.getRemoteBigBlindSeat()).isEqualTo(RemoteHoldemHand.NO_CURRENT_PLAYER);
            hand.updateBigBlindSeat(5);
            assertThat(hand.getRemoteBigBlindSeat()).isEqualTo(5);
        }
    }

    // =========================================================================
    // Pots
    // =========================================================================

    @Nested
    class Pots {

        @Test
        void numPotsInitiallyZero() {
            assertThat(hand.getNumPots()).isEqualTo(0);
        }

        @Test
        void getPotBoundsCheckReturnsNull() {
            assertThat(hand.getPot(-1)).isNull();
            assertThat(hand.getPot(0)).isNull();
        }
    }

    // =========================================================================
    // State flags
    // =========================================================================

    @Nested
    class StateFlags {

        @Test
        void isAllInShowdownAlwaysFalse() {
            assertThat(hand.isAllInShowdown()).isFalse();
        }

        @Test
        void isStoredInDatabaseAlwaysFalse() {
            assertThat(hand.isStoredInDatabase()).isFalse();
        }

        @Test
        void isFoldedDelegatesToPlayer() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.isFolded(alice)).isFalse();

            alice.setFolded(true);
            assertThat(hand.isFolded(alice)).isTrue();
        }

        @Test
        void getLastActionReturnsActionNone() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.getLastAction(alice)).isEqualTo(HandAction.ACTION_NONE);
        }

        @Test
        void getLastActionThisRoundReturnsActionNone() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.getLastActionThisRound(alice)).isEqualTo(HandAction.ACTION_NONE);
        }

        @Test
        void isActionInRoundAlwaysFalse() {
            assertThat(hand.isActionInRound(0)).isFalse();
            assertThat(hand.isActionInRound(1)).isFalse();
        }

        @Test
        void getFoldRoundReturnsNegativeOne() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.getFoldRound(alice)).isEqualTo(-1);
        }

        @Test
        void getOverbetReturnsZero() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.getOverbet(alice)).isEqualTo(0);
        }

        @Test
        void getTotalBetReturnsZero() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.getTotalBet(alice)).isEqualTo(0);
        }

        @Test
        void getNumPriorRaisesReturnsZero() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            assertThat(hand.getNumPriorRaises(alice)).isEqualTo(0);
        }

        @Test
        void getStartDateReturnsZero() {
            assertThat(hand.getStartDate()).isEqualTo(0);
        }

        @Test
        void getEndDateReturnsZero() {
            assertThat(hand.getEndDate()).isEqualTo(0);
        }

        @Test
        void getLastActionNoArgReturnsNull() {
            assertThat(hand.getLastAction()).isNull();
        }
    }

    // =========================================================================
    // Other
    // =========================================================================

    @Nested
    class Other {

        @Test
        void getNumWithCardsCountsNonFoldedPlayers() {
            ClientPlayer alice = new ClientPlayer(1, "Alice", true);
            ClientPlayer bob = new ClientPlayer(2, "Bob", false);
            ClientPlayer charlie = new ClientPlayer(3, "Charlie", false);
            charlie.setFolded(true);

            hand.updatePlayerOrder(List.of(alice, bob, charlie));
            assertThat(hand.getNumWithCards()).isEqualTo(2);
        }

        @Test
        void getMinChipReturnsOneWithNoTable() {
            assertThat(hand.getMinChip()).isEqualTo(1);
        }

        @Test
        void getMuckThrowsUnsupportedOperationException() {
            assertThatThrownBy(() -> hand.getMuck()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void getRoundForDisplayReturnsLegacyInt() {
            hand.updateRound(ClientBettingRound.FLOP);
            assertThat(hand.getRoundForDisplay()).isEqualTo(ClientBettingRound.FLOP.toLegacy());
        }

        @Test
        void getCommunityForDisplaySameAsCommunity() {
            ClientHand community = ClientHand.empty();
            community.addCard(ClientCard.getCard("Ah"));
            hand.updateCommunity(community);

            assertThat(hand.getCommunityForDisplay()).isSameAs(hand.getCommunity());
        }

        @Test
        void getCommunitySortedCachedUntilCommunityChanges() {
            ClientHand community = ClientHand.empty();
            community.addCard(ClientCard.getCard("Kd"));
            community.addCard(ClientCard.getCard("2c"));
            community.addCard(ClientCard.getCard("Ah"));
            hand.updateCommunity(community);

            ClientHand sorted1 = hand.getCommunitySorted();
            ClientHand sorted2 = hand.getCommunitySorted();
            assertThat(sorted1).isSameAs(sorted2); // cached

            // Change community — should produce new sorted hand
            ClientHand newCommunity = ClientHand.empty();
            newCommunity.addCard(ClientCard.getCard("3s"));
            hand.updateCommunity(newCommunity);

            ClientHand sorted3 = hand.getCommunitySorted();
            assertThat(sorted3).isNotSameAs(sorted1);
        }
    }
}
