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
package com.donohoedigital.games.poker.engine;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for TournamentProfileHtml helper methods.
 */
public class TournamentProfileHtmlTest {

    @Test
    public void should_DisplayFullRing_ForTenSeats() {
        assertEquals("10 - Full Ring", TournamentProfileHtml.getTableFormatDisplay(10));
    }

    @Test
    public void should_Display6Max_ForSixSeats() {
        assertEquals("6 - 6-Max", TournamentProfileHtml.getTableFormatDisplay(6));
    }

    @Test
    public void should_DisplayHeadsUp_ForTwoSeats() {
        assertEquals("2 - Heads-Up", TournamentProfileHtml.getTableFormatDisplay(2));
    }

    @Test
    public void should_DisplayRawNumber_ForNonStandardSeats() {
        assertEquals("8", TournamentProfileHtml.getTableFormatDisplay(8));
        assertEquals("5", TournamentProfileHtml.getTableFormatDisplay(5));
        assertEquals("9", TournamentProfileHtml.getTableFormatDisplay(9));
    }
}
