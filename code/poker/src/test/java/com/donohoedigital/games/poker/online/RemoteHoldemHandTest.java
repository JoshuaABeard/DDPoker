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

import com.donohoedigital.games.poker.HoldemHand;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(hand.getRound()).isEqualTo(BettingRound.PRE_FLOP);
        assertThat(hand.getNumPlayers()).isEqualTo(0);
        assertThat(hand.getCurrentPlayerIndex()).isEqualTo(HoldemHand.NO_CURRENT_PLAYER);
        assertThat(hand.getCurrentPlayer()).isNull();
        assertThat(hand.getTotalPotChipCount()).isEqualTo(0);
    }

    @Test
    void updateRoundChangesRound() {
        hand.updateRound(BettingRound.FLOP);
        assertThat(hand.getRound()).isEqualTo(BettingRound.FLOP);

        hand.updateRound(BettingRound.TURN);
        assertThat(hand.getRound()).isEqualTo(BettingRound.TURN);

        hand.updateRound(BettingRound.RIVER);
        assertThat(hand.getRound()).isEqualTo(BettingRound.RIVER);
    }

    @Test
    void updateCommunityStoresCommunity() {
        Hand community = new Hand();
        // Cards as strings like "Ah" = Ace of Hearts; use index-based constructor for
        // portability
        community.addCard(Card.getCard("Ah"));
        community.addCard(Card.getCard("Kd"));
        community.addCard(Card.getCard("2c"));

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
        PokerPlayer alice = new PokerPlayer(1, "Alice", true);
        PokerPlayer bob = new PokerPlayer(2, "Bob", false);
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
        PokerPlayer alice = new PokerPlayer(1, "Alice", true);
        PokerPlayer bob = new PokerPlayer(2, "Bob", false);
        hand.updatePlayerOrder(List.of(alice, bob));
        hand.updateCurrentPlayer(1); // valid index

        // Replace with smaller list — index 1 is now out of bounds
        hand.updatePlayerOrder(List.of(alice));

        assertThat(hand.getCurrentPlayerIndex()).isEqualTo(HoldemHand.NO_CURRENT_PLAYER);
    }
}
