/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores recent completed-hand results for control-server observability.
 */
public final class HandResultLog {

    public static final int CAPACITY = 50;

    public record PotWinnerAward(long playerId, int seat, String name, int amount) {
    }

    public record PotBreakdown(int potIndex, int totalAmount, List<PotWinnerAward> winners) {
        public PotBreakdown {
            winners = List.copyOf(winners);
        }
    }

    public record WinnerResult(long playerId, int seat, String name, String handClass, String handDescription,
            int amountWon, List<String> cards) {
        public WinnerResult {
            cards = List.copyOf(cards);
        }
    }

    public record PayoutDelta(long playerId, int seat, String name, boolean isHuman, int startChips, int endChips,
            int delta) {
    }

    public record HandResult(long timestampMs, int tableId, int handNumber, List<String> communityCards,
            List<WinnerResult> winners, List<PotBreakdown> potBreakdown, List<PayoutDelta> payoutDeltas) {
        public HandResult {
            communityCards = List.copyOf(communityCards);
            winners = List.copyOf(winners);
            potBreakdown = List.copyOf(potBreakdown);
            payoutDeltas = List.copyOf(payoutDeltas);
        }
    }

    private static final Deque<HandResult> recent_ = new ArrayDeque<>(CAPACITY);
    private static final Map<Integer, HandResult> latestByTable_ = new HashMap<>();

    private HandResultLog() {
    }

    public static synchronized void record(HandResult result) {
        if (result == null) {
            return;
        }
        latestByTable_.put(result.tableId(), result);
        if (recent_.size() >= CAPACITY) {
            recent_.pollFirst();
        }
        recent_.addLast(result);
    }

    public static synchronized HandResult getForTable(int tableId) {
        return latestByTable_.get(tableId);
    }

    public static synchronized HandResult getLatest() {
        return recent_.peekLast();
    }

    public static synchronized List<HandResult> getEntries() {
        return new ArrayList<>(recent_);
    }

    public static synchronized void clear() {
        recent_.clear();
        latestByTable_.clear();
    }
}
