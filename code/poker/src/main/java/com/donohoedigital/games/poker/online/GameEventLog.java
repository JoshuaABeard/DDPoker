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
import java.util.List;

/**
 * Ring buffer for game event logging, readable by the dev control server.
 *
 * <p>
 * Captures the last {@value #CAPACITY} key {@code PokerTableEvent} firings with
 * millisecond timestamps. Used by {@code GET /state} to expose
 * {@code recentEvents} and by {@code GET /ws-log} for full event history.
 *
 * <p>
 * Wired at key {@code table.fireEvent} / {@code table.firePokerTableEvent} call
 * sites in {@link WebSocketTournamentDirector}.
 */
public class GameEventLog {

    /** A single logged game event. */
    public record Entry(long millis, String type, int tableId) {
    }

    static final int CAPACITY = 50;

    private static final Deque<Entry> buffer_ = new ArrayDeque<>(CAPACITY);

    private GameEventLog() {
    }

    /** Log a game event for the given table (1-based table number). */
    public static synchronized void log(String type, int tableId) {
        Entry entry = new Entry(System.currentTimeMillis(), type, tableId);
        if (buffer_.size() >= CAPACITY) {
            buffer_.pollFirst();
        }
        buffer_.addLast(entry);
    }

    /** Clear all entries. */
    public static synchronized void clear() {
        buffer_.clear();
    }

    /** Returns a snapshot of all buffered entries, oldest first. */
    public static synchronized List<Entry> getEntries() {
        return new ArrayList<>(buffer_);
    }

    /** Returns a snapshot of the most recent {@code n} entries, oldest first. */
    public static synchronized List<Entry> getLastN(int n) {
        List<Entry> all = new ArrayList<>(buffer_);
        int from = Math.max(0, all.size() - n);
        return all.subList(from, all.size());
    }
}
