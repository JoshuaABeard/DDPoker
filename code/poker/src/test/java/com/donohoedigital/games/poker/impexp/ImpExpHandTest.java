/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.impexp;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.engine.PokerConstants;

/**
 * Tests for {@link ImpExpHand} data class.
 */
class ImpExpHandTest {

    @Test
    void constructor_initializesDefaultValues() {
        ImpExpHand hand = new ImpExpHand();

        assertThat(hand.community).isNotNull();
        assertThat(hand.community.size()).isZero();
        assertThat(hand.startDate).isNotNull();
        assertThat(hand.endDate).isNotNull();
        assertThat(hand.tournamentStartDate).isNotNull();
        assertThat(hand.tournamentEndDate).isNotNull();
        assertThat(hand.hist).isNotNull();
        assertThat(hand.hist).isEmpty();
    }

    @Test
    void constructor_initializesArraysWithCorrectLength() {
        ImpExpHand hand = new ImpExpHand();

        assertThat(hand.players).hasSize(PokerConstants.SEATS);
        assertThat(hand.betChips).hasSize(PokerConstants.SEATS);
        assertThat(hand.overbetChips).hasSize(PokerConstants.SEATS);
        assertThat(hand.winChips).hasSize(PokerConstants.SEATS);
        assertThat(hand.startChips).hasSize(PokerConstants.SEATS);
        assertThat(hand.endChips).hasSize(PokerConstants.SEATS);
    }

    @Test
    void constructor_initializesLocalHumanPlayerSeatToNegativeOne() {
        ImpExpHand hand = new ImpExpHand();

        assertThat(hand.localHumanPlayerSeat).isEqualTo(-1);
    }

    @Test
    void constructor_initializesNumericFieldsToZero() {
        ImpExpHand hand = new ImpExpHand();

        assertThat(hand.profileNumber).isZero();
        assertThat(hand.handID).isZero();
        assertThat(hand.tournamentID).isZero();
        assertThat(hand.ante).isZero();
        assertThat(hand.smallBlind).isZero();
        assertThat(hand.bigBlind).isZero();
        assertThat(hand.buttonSeat).isZero();
    }

    @Test
    void fields_areReadableAndWritable() {
        ImpExpHand hand = new ImpExpHand();

        hand.profileNumber = 5;
        hand.handID = 42;
        hand.tournamentID = 100;
        hand.tournamentName = "Main Event";
        hand.hndTable = "Table 1";
        hand.hndNumber = "Hand #42";
        hand.gameStyle = "NL";
        hand.gameType = "Hold'em";
        hand.ante = 25;
        hand.smallBlind = 50;
        hand.bigBlind = 100;
        hand.buttonSeat = 3;

        assertThat(hand.profileNumber).isEqualTo(5);
        assertThat(hand.handID).isEqualTo(42);
        assertThat(hand.tournamentID).isEqualTo(100);
        assertThat(hand.tournamentName).isEqualTo("Main Event");
        assertThat(hand.hndTable).isEqualTo("Table 1");
        assertThat(hand.hndNumber).isEqualTo("Hand #42");
        assertThat(hand.gameStyle).isEqualTo("NL");
        assertThat(hand.gameType).isEqualTo("Hold'em");
        assertThat(hand.ante).isEqualTo(25);
        assertThat(hand.smallBlind).isEqualTo(50);
        assertThat(hand.bigBlind).isEqualTo(100);
        assertThat(hand.buttonSeat).isEqualTo(3);
    }
}
