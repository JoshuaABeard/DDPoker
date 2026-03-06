/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
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
package com.donohoedigital.games.poker.display;

/**
 * Hand scoring constants for use in the client display layer, mirroring the
 * values from {@code pokerengine.HandScoreConstants} without a compile-time
 * dependency on that module.
 */
public interface ClientHandScoreConstants {
    int ROYAL_FLUSH = 10;
    int STRAIGHT_FLUSH = 9;
    int QUADS = 8;
    int FULL_HOUSE = 7;
    int FLUSH = 6;
    int STRAIGHT = 5;
    int TRIPS = 4;
    int TWO_PAIR = 3;
    int PAIR = 2;
    int HIGH_CARD = 1;

    int SCORE_BASE = 1000000;
    int H0 = 1;
    int H1 = 16;
    int H2 = 256;
    int H3 = 4096;
    int H4 = 65536;
}
