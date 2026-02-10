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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.integration.IntegrationTestBase;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for V2Player Harrington M/Q/Zone calculations.
 * Tests the "M" (chip stack relative to blinds) and "Q" (chip stack relative to average)
 * metrics from Dan Harrington's poker strategy books.
 * Extends IntegrationTestBase for full game infrastructure.
 */
@Tag("integration")
class V2PlayerHarringtonTest extends IntegrationTestBase
{
    private PokerGame game;
    private PokerTable table;
    private HoldemHand hand;
    private PokerPlayer player;
    private V2Player ai;

    @BeforeEach
    void setUp()
    {
        // Create game infrastructure
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);

        table = new PokerTable(game, 1);
        table.setMinChip(1);

        // Create 3 players with initial chip counts
        player = new PokerPlayer(1, "TestPlayer", true);
        player.setChipCount(1000); // Set chips BEFORE newHand()

        PokerPlayer opponent1 = new PokerPlayer(2, "Opponent1", true);
        opponent1.setChipCount(1000);

        PokerPlayer opponent2 = new PokerPlayer(3, "Opponent2", true);
        opponent2.setChipCount(1000);

        game.addPlayer(player);
        game.addPlayer(opponent1);
        game.addPlayer(opponent2);

        table.setPlayer(player, 0);
        table.setPlayer(opponent1, 1);
        table.setPlayer(opponent2, 2);

        // Update total chips in play for Q calculation
        game.computeTotalChipsInPlay();

        // Set button
        table.setButton(1);

        // Give players pocket cards for proper hand initialization
        // This captures chip counts as "chips at start"
        player.newHand('p');
        opponent1.newHand('p');
        opponent2.newHand('p');

        hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setBigBlind(20);
        hand.setSmallBlind(10);
        hand.setPlayerOrder(false);

        // Attach V2Player AI
        ai = new V2Player();
        ai.setPokerPlayer(player);
        ai.init();
    }

    /**
     * Helper to set chip count and reinitialize hand (captures chips at start)
     */
    private void setPlayerChips(PokerPlayer p, int chips)
    {
        p.setChipCount(chips);
        p.newHand('p'); // Recapture chip count at start
    }

    // ========================================
    // M Calculation Tests
    // ========================================

    @Test
    void should_ReturnHighM_When_PlayerHasBigStack()
    {
        // Set player to have 3000 chips (150 big blinds)
        setPlayerChips(player, 3000);
        game.computeTotalChipsInPlay();

        float m = ai.getHohM();

        // M = chips / (ante * players + SB + BB)
        // M = 3000 / (0 * 3 + 10 + 20) = 3000 / 30 = 100
        // Effective M = 100 * (2/3 + 2/27) = 100 * 0.741 = 74.1
        assertThat(m).isGreaterThan(70.0f);
        assertThat(m).isLessThan(80.0f);
    }

    @Test
    void should_ReturnMediumM_When_PlayerHasAverageStack()
    {
        // Set player to have 600 chips (30 big blinds)
        setPlayerChips(player, 600);
        game.computeTotalChipsInPlay();

        float m = ai.getHohM();

        // M = 600 / 30 = 20
        // Effective M = 20 * 0.741 = 14.8
        assertThat(m).isGreaterThan(13.0f);
        assertThat(m).isLessThan(16.0f);
    }

    @Test
    void should_ReturnLowM_When_PlayerHasShortStack()
    {
        // Set player to have 150 chips (7.5 big blinds)
        setPlayerChips(player, 150);
        game.computeTotalChipsInPlay();

        float m = ai.getHohM();

        // M = 150 / 30 = 5
        // Effective M = 5 * 0.741 = 3.7
        assertThat(m).isGreaterThan(3.0f);
        assertThat(m).isLessThan(5.0f);
    }

    @Test
    void should_CalculateTableAverageM_When_MultiplePlayers()
    {
        // Set all players to 1000 chips
        setPlayerChips(player, 1000);
        setPlayerChips(table.getPlayer(1), 1000);
        setPlayerChips(table.getPlayer(2), 1000);
        game.computeTotalChipsInPlay();

        float avgM = ai.getTableAverageHohM();

        // M for each player = 1000 / 30 = 33.33
        // Effective M = 33.33 * 0.741 = 24.7
        // Average should be the same since all equal
        assertThat(avgM).isGreaterThan(23.0f);
        assertThat(avgM).isLessThan(26.0f);
    }

    // ========================================
    // Q Calculation Tests
    // ========================================

    @Test
    void should_ReturnQAbove1_When_PlayerAboveAverage()
    {
        // Set player to 2000, others to 500
        // Average stack is based on buyin (1500), not actual chips
        // Total chips in play = 3 * 1500 buyin = 4500, average = 1500
        setPlayerChips(player, 2000);
        setPlayerChips(table.getPlayer(1), 500);
        setPlayerChips(table.getPlayer(2), 500);
        game.computeTotalChipsInPlay();

        float q = ai.getHohQ();

        // Q = chips / average = 2000 / 1500 = 1.33
        assertThat(q).isGreaterThan(1.0f);
        assertThat(q).isLessThan(1.5f);
    }

    @Test
    void should_ReturnQBelow1_When_PlayerBelowAverage()
    {
        // Set player to 500, others to 2000 (average = 1500)
        setPlayerChips(player, 500);
        setPlayerChips(table.getPlayer(1), 2000);
        setPlayerChips(table.getPlayer(2), 2000);
        game.computeTotalChipsInPlay();

        float q = ai.getHohQ();

        // Q = chips / average = 500 / 1500 = 0.33
        assertThat(q).isGreaterThan(0.2f);
        assertThat(q).isLessThan(0.5f);
    }

    // ========================================
    // Zone Categorization Tests
    // ========================================

    @Test
    void should_ReturnDeadZone_When_MLessThan1()
    {
        // M <= 1 is Dead zone
        // Need chips < 30 * (1 / 0.741) = 40 chips
        setPlayerChips(player, 20);
        game.computeTotalChipsInPlay();

        int zone = ai.getHohZone();
        String zoneName = ai.getHohZoneName();

        assertThat(zone).isEqualTo(AIConstants.HOH_DEAD);
        assertThat(zoneName).isEqualTo("Dead");
    }

    @Test
    void should_ReturnRedZone_When_MBetween1And5()
    {
        // 1 < M <= 5 is Red zone
        // Need chips between 40 and 200 (approx)
        setPlayerChips(player, 100);
        game.computeTotalChipsInPlay();

        int zone = ai.getHohZone();
        String zoneName = ai.getHohZoneName();

        assertThat(zone).isEqualTo(AIConstants.HOH_RED);
        assertThat(zoneName).isEqualTo("Red");
    }

    @Test
    void should_ReturnOrangeZone_When_MBetween5And10()
    {
        // 5 < M <= 10 is Orange zone
        // Need chips between 200 and 400 (approx)
        setPlayerChips(player, 250);
        game.computeTotalChipsInPlay();

        int zone = ai.getHohZone();
        String zoneName = ai.getHohZoneName();

        assertThat(zone).isEqualTo(AIConstants.HOH_ORANGE);
        assertThat(zoneName).isEqualTo("Orange");
    }

    @Test
    void should_ReturnYellowZone_When_MBetween10And20()
    {
        // 10 < M <= 20 is Yellow zone
        // Need chips between 400 and 800 (approx)
        setPlayerChips(player, 500);
        game.computeTotalChipsInPlay();

        int zone = ai.getHohZone();
        String zoneName = ai.getHohZoneName();

        assertThat(zone).isEqualTo(AIConstants.HOH_YELLOW);
        assertThat(zoneName).isEqualTo("Yellow");
    }

    @Test
    void should_ReturnGreenZone_When_MGreaterThan20()
    {
        // M > 20 is Green zone
        // Need chips > 800 (approx)
        setPlayerChips(player, 1000);
        game.computeTotalChipsInPlay();

        int zone = ai.getHohZone();
        String zoneName = ai.getHohZoneName();

        assertThat(zone).isEqualTo(AIConstants.HOH_GREEN);
        assertThat(zoneName).isEqualTo("Green");
    }
}
