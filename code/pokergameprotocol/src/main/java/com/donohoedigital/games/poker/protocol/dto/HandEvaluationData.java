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
package com.donohoedigital.games.poker.protocol.dto;

import java.util.List;

/**
 * Pre-computed hand evaluation data sent by the server. Eliminates need for
 * client-side hand evaluation.
 */
public record HandEvaluationData(int score, // Opaque hand score for comparison
        int handType, // Hand type (0=HIGH_CARD through 8=ROYAL_FLUSH)
        String handDescription, // Localized description ("Pair of Kings")
        List<String> bestFiveCards, // Best 5-card hand as card strings ("Ah", "Kd", ...)
        Integer bigPairRank, // Rank of big pair (null if N/A)
        Integer smallPairRank, // Rank of small pair (null if N/A)
        Integer tripsRank, // Rank of trips (null if N/A)
        Integer quadsRank, // Rank of quads (null if N/A)
        Integer highCardRank, // High card rank (null if N/A)
        Integer straightHighRank, // High rank of straight (null if N/A)
        Integer straightLowRank, // Low rank of straight (null if N/A)
        Integer flushHighRank // High rank of flush (null if N/A)
) {
    public static final HandEvaluationData NONE = new HandEvaluationData(0, 0, "", List.of(), null, null, null, null,
            null, null, null, null);
}
