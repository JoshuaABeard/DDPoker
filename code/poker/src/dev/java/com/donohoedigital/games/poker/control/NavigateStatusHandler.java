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

import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.Phase;
import com.donohoedigital.games.poker.PokerMain;
import com.sun.net.httpserver.HttpExchange;

/**
 * {@code GET /navigate/status} — durable status for the latest /navigate request.
 */
class NavigateStatusHandler extends BaseHandler {

    NavigateStatusHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, java.util.Map.of("error", "MethodNotAllowed"));
            return;
        }

        sendJson(exchange, 200, NavigationStatusRegistry.snapshot(currentLifecyclePhase()));
    }

    private String currentLifecyclePhase() {
        PokerMain main = PokerMain.getPokerMain();
        GameContext context = main == null ? null : main.getDefaultContext();
        if (context == null) return "NONE";
        Phase phase = context.getCurrentUIPhase();
        if (phase == null) return "NONE";
        return phase.getGamePhase().getName();
    }
}
