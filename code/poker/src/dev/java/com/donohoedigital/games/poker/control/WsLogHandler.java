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
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.poker.online.GameEventLog;
import com.donohoedigital.games.poker.online.WsMessageLog;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dev-only HTTP handler for {@code GET /ws-log}.
 *
 * <p>Returns the last {@value WsMessageLog#CAPACITY} WebSocket messages and the last
 * {@value GameEventLog#CAPACITY} game events as JSON arrays, oldest-first.
 *
 * <p>Example response:
 * <pre>
 * {
 *   "messages": [
 *     {"ms": 1740235200000, "direction": "IN",  "type": "HAND_STARTED", "payload": "..."},
 *     {"ms": 1740235200890, "direction": "OUT", "type": "PLAYER_ACTION", "payload": "CALL:0"}
 *   ],
 *   "events": [
 *     {"ms": 1740235200100, "type": "NEW_HAND",              "table": 1},
 *     {"ms": 1740235200910, "type": "CURRENT_PLAYER_CHANGED","table": 1}
 *   ]
 * }
 * </pre>
 */
class WsLogHandler extends BaseHandler {

    WsLogHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (WsMessageLog.Entry e : WsMessageLog.getEntries()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ms", e.millis());
            m.put("direction", e.direction());
            m.put("type", e.type());
            m.put("payload", e.payload());
            messages.add(m);
        }

        List<Map<String, Object>> events = new ArrayList<>();
        for (GameEventLog.Entry e : GameEventLog.getEntries()) {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("ms", e.millis());
            ev.put("type", e.type());
            ev.put("table", e.tableId());
            events.add(ev);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("messages", messages);
        response.put("events", events);
        sendJson(exchange, 200, response);
    }
}
