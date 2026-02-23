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

import com.sun.net.httpserver.HttpExchange;

import java.net.URL;
import java.util.*;

/**
 * {@code GET /help/topics} — list available help topics with content verification.
 */
class HelpTopicsHandler extends BaseHandler {

    private static final String[] KNOWN_TOPICS = {
        "practice", "pokerclock", "calculator", "rankings",
        "texasholdem", "glossary", "shortcuts", "whatsnew", "credits"
    };

    HelpTopicsHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        List<Map<String, Object>> topics = new ArrayList<>();
        for (String topic : KNOWN_TOPICS) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", topic);
            // Check if help resource exists
            String resourcePath = "/config/poker/help/" + topic + ".html";
            URL url = getClass().getResource(resourcePath);
            t.put("exists", url != null);
            topics.add(t);
        }

        sendJson(exchange, 200, Map.of("topics", topics));
    }
}
