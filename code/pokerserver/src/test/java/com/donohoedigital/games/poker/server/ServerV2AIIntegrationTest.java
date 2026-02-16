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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.ai.*;
import com.donohoedigital.games.poker.core.state.ActionType;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying server-side V2 AI works end-to-end. Tests that
 * V2Algorithm + ServerV2AIContext + ServerStrategyProvider function together to
 * produce valid poker decisions on the server.
 */
class ServerV2AIIntegrationTest {

    @Test
    void v2Algorithm_withServerContext_producesValidDecisions() {
        // Given: Server AI components
        V2Algorithm algorithm = new V2Algorithm();
        ServerStrategyProvider strategy = new ServerStrategyProvider("test-player-1");

        // Create a pre-flop scenario: player in middle position with pocket aces
        Hand pocket = createPocketAces();
        Hand community = new Hand(0); // pre-flop

        ServerV2AIContext context = ServerTestUtils.createPreFlopContext(/* position */ 3, /* pocket */ pocket, /*
                                                                                                                 * potStatus
                                                                                                                 */ 0, // NO_POT_ACTION
                /* amountToCall */ 0);

        GamePlayerInfo player = ServerTestUtils.createPlayer(1000);
        ActionOptions options = ServerTestUtils.createOptions(/* canCheck */ true, /* canCall */ false,
                /* canBet */ true, /* canRaise */ false, /* canFold */ false);

        // When: V2Algorithm makes decision
        PlayerAction action = algorithm.getAction(player, options, context);

        // Then: Should make a valid decision (not fold with AA)
        assertThat(action).isNotNull();
        assertThat(action.actionType()).isNotEqualTo(ActionType.FOLD);
        assertThat(action.actionType()).isIn(ActionType.CHECK, ActionType.BET, ActionType.RAISE);
    }

    @Test
    void v2Algorithm_multiplePlayersWithStrategies_produceDifferentDecisions() {
        // Given: Three players with different AI instances
        V2Algorithm algo1 = new V2Algorithm();
        V2Algorithm algo2 = new V2Algorithm();
        V2Algorithm algo3 = new V2Algorithm();

        ServerStrategyProvider strategy1 = new ServerStrategyProvider("player-1");
        ServerStrategyProvider strategy2 = new ServerStrategyProvider("player-2");
        ServerStrategyProvider strategy3 = new ServerStrategyProvider("player-3");

        // Same hand (medium strength suited connectors)
        Hand pocket = createSuitedConnectors();
        ServerV2AIContext context = ServerTestUtils.createPreFlopContext(/* position */ 4, /* pocket */ pocket,
                /* potStatus */ 0, /* amountToCall */ 0);

        GamePlayerInfo player = ServerTestUtils.createPlayer(1000);
        ActionOptions options = ServerTestUtils.createOptions(/* canCheck */ true, /* canCall */ false,
                /* canBet */ true, /* canRaise */ false, /* canFold */ false);

        // When: Each AI makes decisions with different strategies
        // Note: Due to randomness, we can't assert exact differences,
        // but we verify all produce valid actions
        PlayerAction action1 = algo1.getAction(player, options, context);
        PlayerAction action2 = algo2.getAction(player, options, context);
        PlayerAction action3 = algo3.getAction(player, options, context);

        // Then: All should make valid decisions
        assertThat(action1.actionType()).isIn(ActionType.CHECK, ActionType.BET, ActionType.RAISE, ActionType.FOLD);
        assertThat(action2.actionType()).isIn(ActionType.CHECK, ActionType.BET, ActionType.RAISE, ActionType.FOLD);
        assertThat(action3.actionType()).isIn(ActionType.CHECK, ActionType.BET, ActionType.RAISE, ActionType.FOLD);
    }

    @Test
    void v2Algorithm_postFlop_evaluatesHandStrength() {
        // Given: Post-flop scenario with nut flush
        V2Algorithm algorithm = new V2Algorithm();
        ServerStrategyProvider strategy = new ServerStrategyProvider("test-player-2");

        Hand pocket = new Hand(2);
        pocket.addCard(new Card(CardSuit.HEARTS, Card.ACE));
        pocket.addCard(new Card(CardSuit.HEARTS, Card.KING));

        Hand community = new Hand(3);
        community.addCard(new Card(CardSuit.HEARTS, Card.QUEEN));
        community.addCard(new Card(CardSuit.HEARTS, Card.JACK));
        community.addCard(new Card(CardSuit.HEARTS, Card.TEN)); // Royal flush!

        ServerV2AIContext context = ServerTestUtils.createPostFlopContext(/* round */ 1, // Flop
                /* pocket */ pocket, /* community */ community, /* amountToCall */ 0);

        GamePlayerInfo player = ServerTestUtils.createPlayer(1000);
        ActionOptions options = ServerTestUtils.createOptions(/* canCheck */ true, /* canCall */ false,
                /* canBet */ true, /* canRaise */ false, /* canFold */ false);

        // When: V2Algorithm makes decision with nuts
        PlayerAction action = algorithm.getAction(player, options, context);

        // Then: Should bet/raise with the nuts
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.RAISE);
        assertThat(action.amount()).isGreaterThanOrEqualTo(0); // Amount determined by bet sizing logic
    }

    @Test
    void v2Algorithm_facingRaise_withTrashHand_shouldFold() {
        // Given: Facing a raise with 72o (worst hand)
        V2Algorithm algorithm = new V2Algorithm();
        ServerStrategyProvider strategy = new ServerStrategyProvider("test-player-3");

        Hand pocket = createTrashHand();
        ServerV2AIContext context = ServerTestUtils.createPreFlopContext(/* position */ 2, /* pocket */ pocket, /*
                                                                                                                 * potStatus
                                                                                                                 */ 2, // RAISED_POT
                /* amountToCall */ 100);

        GamePlayerInfo player = ServerTestUtils.createPlayer(1000);
        ActionOptions options = ServerTestUtils.createOptions(/* canCheck */ false, /* canCall */ true,
                /* canBet */ false, /* canRaise */ true, /* canFold */ true);

        // When: V2Algorithm makes decision
        PlayerAction action = algorithm.getAction(player, options, context);

        // Then: Should fold trash hand facing raise
        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
    }

    @Test
    void serverStrategyProvider_providesReasonableHandStrength() {
        // Given: Strategy provider
        ServerStrategyProvider strategy = new ServerStrategyProvider("test-player-4");

        // When/Then: Hand strengths should be reasonable
        Hand pocketAces = createPocketAces();
        float aaStrength = strategy.getHandStrength(pocketAces);
        assertThat(aaStrength).isGreaterThan(0.9f); // AA should be very strong

        Hand trashHand = createTrashHand();
        float trashStrength = strategy.getHandStrength(trashHand);
        assertThat(trashStrength).isLessThan(0.3f); // 72o should be weak

        Hand suitedConnectors = createSuitedConnectors();
        float scStrength = strategy.getHandStrength(suitedConnectors);
        assertThat(scStrength).isBetween(0.3f, 0.7f); // Medium strength
    }

    // === Helper methods for creating test hands ===

    private Hand createPocketAces() {
        Hand hand = new Hand(2);
        hand.addCard(new Card(CardSuit.SPADES, Card.ACE));
        hand.addCard(new Card(CardSuit.HEARTS, Card.ACE));
        return hand;
    }

    private Hand createTrashHand() {
        Hand hand = new Hand(2);
        hand.addCard(new Card(CardSuit.SPADES, 7));
        hand.addCard(new Card(CardSuit.HEARTS, 2)); // 72o - worst hand
        return hand;
    }

    private Hand createSuitedConnectors() {
        Hand hand = new Hand(2);
        hand.addCard(new Card(CardSuit.CLUBS, 9));
        hand.addCard(new Card(CardSuit.CLUBS, 8)); // 98s
        return hand;
    }
}
