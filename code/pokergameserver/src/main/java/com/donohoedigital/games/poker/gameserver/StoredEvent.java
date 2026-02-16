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
package com.donohoedigital.games.poker.gameserver;

import com.donohoedigital.games.poker.core.event.GameEvent;
import java.time.Instant;

/**
 * A stored event with metadata for persistence and replay.
 *
 * @param gameId
 *            unique identifier for the game this event belongs to
 * @param sequenceNumber
 *            monotonically increasing sequence number for this game's events
 * @param eventType
 *            simple class name of the event (e.g., "HandStarted")
 * @param event
 *            the actual game event
 * @param timestamp
 *            when the event was recorded
 */
public record StoredEvent(String gameId, long sequenceNumber, String eventType, GameEvent event, Instant timestamp) {
}
