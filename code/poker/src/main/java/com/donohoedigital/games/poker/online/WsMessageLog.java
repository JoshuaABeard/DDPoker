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
 * Ring buffer for WebSocket message logging, readable by the dev control
 * server.
 *
 * <p>
 * Captures the last {@value #CAPACITY} inbound and outbound WebSocket messages.
 * Thread-safe via {@code synchronized}; writes are infrequent (one per WS
 * message).
 *
 * <p>
 * Wired at two sites:
 * <ul>
 * <li>Outbound: {@link WebSocketGameClient#sendAction}</li>
 * <li>Inbound: {@link WebSocketTournamentDirector#onMessage}</li>
 * </ul>
 */
public class WsMessageLog {

    /** A single logged WebSocket message. */
    public record Entry(long millis, String direction, String type, String payload) {
    }

    static final int CAPACITY = 40;
    private static final int MAX_PAYLOAD = 500;

    private static final Deque<Entry> buffer_ = new ArrayDeque<>(CAPACITY);

    private WsMessageLog() {
    }

    /** Log an outbound player action. */
    public static synchronized void logOutbound(String type, String payload) {
        addEntry(new Entry(System.currentTimeMillis(), "OUT", type, truncate(payload)));
    }

    /** Log an inbound server message. */
    public static synchronized void logInbound(String type, String payload) {
        addEntry(new Entry(System.currentTimeMillis(), "IN", type, truncate(payload)));
    }

    /** Clear all entries. */
    public static synchronized void clear() {
        buffer_.clear();
    }

    /** Returns a snapshot of all buffered entries, oldest first. */
    public static synchronized List<Entry> getEntries() {
        return new ArrayList<>(buffer_);
    }

    private static void addEntry(Entry entry) {
        if (buffer_.size() >= CAPACITY) {
            buffer_.pollFirst();
        }
        buffer_.addLast(entry);
    }

    private static String truncate(String s) {
        if (s == null)
            return "";
        return s.length() <= MAX_PAYLOAD ? s : s.substring(0, MAX_PAYLOAD) + "…";
    }
}
