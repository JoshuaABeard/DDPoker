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

import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.PlayerProfileOptions;
import com.donohoedigital.games.poker.PokerDatabase;
import com.donohoedigital.games.poker.model.TournamentHistory;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/**
 * {@code GET /history} — tournament history and hand history.
 *
 * <p>{@code GET /history} returns tournament history for the current profile.
 * <p>{@code GET /history/hand?id=N} returns a specific hand as HTML.
 */
class HistoryHandler extends BaseHandler {

    HistoryHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/history/hand")) {
            handleHandHistory(exchange);
        } else {
            handleTournamentHistory(exchange);
        }
    }

    private void handleTournamentHistory(HttpExchange exchange) throws Exception {
        PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
        if (profile == null) {
            sendJson(exchange, 503, Map.of("error", "Unavailable", "message", "No active profile"));
            return;
        }

        List<TournamentHistory> histories = PokerDatabase.getTournamentHistory(profile);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TournamentHistory th : histories) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tournamentName", th.getTournamentName());
            m.put("numPlayers", th.getNumPlayers());
            m.put("place", th.getPlace());
            m.put("prize", th.getPrize());
            m.put("buyin", th.getBuyin());
            m.put("net", th.getNet());
            m.put("endDate", th.getEndDate() != null ? th.getEndDate().getTime() : null);
            result.add(m);
        }

        sendJson(exchange, 200, Map.of(
                "profileName", profile.getName(),
                "tournaments", result,
                "count", result.size()));
    }

    private void handleHandHistory(HttpExchange exchange) throws Exception {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("id=")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "id query parameter required"));
            return;
        }

        int handId;
        try {
            String idStr = query.split("id=")[1].split("&")[0];
            handId = Integer.parseInt(idStr);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid hand id"));
            return;
        }

        String[] html = PokerDatabase.getHandAsHTML(handId, true, true);
        if (html == null) {
            sendJson(exchange, 404, Map.of("error", "NotFound", "message", "Hand not found: " + handId));
            return;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("handId", handId);
        result.put("title", html[0]);
        result.put("summary", html[1]);
        result.put("details", html[2]);
        sendJson(exchange, 200, result);
    }
}
