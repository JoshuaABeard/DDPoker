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

import com.donohoedigital.games.config.BaseProfile;
import com.donohoedigital.games.poker.ClientTournamentProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/**
 * {@code GET/POST/DELETE /tournament-profiles} — CRUD for tournament profiles.
 *
 * <p>{@code GET} lists all saved tournament profiles with their settings.
 * <p>{@code POST} creates a new profile from JSON.
 * <p>{@code DELETE} removes a user-created profile by name.
 */
class TournamentProfilesHandler extends BaseHandler {

    TournamentProfilesHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "GET" -> handleGet(exchange);
            case "POST" -> handlePost(exchange);
            case "DELETE" -> handleDelete(exchange);
            default -> sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
        }
    }

    private void handleGet(HttpExchange exchange) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<BaseProfile> profiles = ClientTournamentProfile.getProfileList();
            for (BaseProfile bp : profiles) {
                if (bp instanceof ClientTournamentProfile tp) {
                    result.add(profileToMap(tp));
                }
            }
        } catch (Exception e) {
            logger.debug("Could not load tournament profile list", e);
        }
        sendJson(exchange, 200, Map.of("profiles", result));
    }

    private void handlePost(HttpExchange exchange) throws Exception {
        String body = readRequestBodyAsString(exchange);
        if (body.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body required"));
            return;
        }

        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON"));
            return;
        }

        if (!json.has("name") || json.get("name").asText().isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "name is required"));
            return;
        }

        String name = json.get("name").asText().trim();

        // Check for duplicate name
        for (BaseProfile bp : ClientTournamentProfile.getProfileList()) {
            if (bp.getName().equalsIgnoreCase(name)) {
                sendJson(exchange, 409, Map.of("error", "Conflict", "message", "Profile already exists: " + name));
                return;
            }
        }

        ClientTournamentProfile tp = new ClientTournamentProfile(name);
        applyJsonToProfile(tp, json);
        tp.fixAll();
        tp.initFile();
        tp.save();

        sendJson(exchange, 201, Map.of("created", true, "profile", profileToMap(tp)));
    }

    private void handleDelete(HttpExchange exchange) throws Exception {
        String body = readRequestBodyAsString(exchange);
        if (body.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body required with 'name'"));
            return;
        }

        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON"));
            return;
        }

        String name = json.has("name") ? json.get("name").asText().trim() : "";
        if (name.isEmpty()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "name is required"));
            return;
        }

        for (BaseProfile bp : ClientTournamentProfile.getProfileList()) {
            if (bp.getName().equalsIgnoreCase(name)) {
                if (!bp.canDelete()) {
                    sendJson(exchange, 403, Map.of("error", "Forbidden", "message", "Built-in profile cannot be deleted"));
                    return;
                }
                bp.getFile().delete();
                sendJson(exchange, 200, Map.of("deleted", true, "name", name));
                return;
            }
        }

        sendJson(exchange, 404, Map.of("error", "NotFound", "message", "No profile named: " + name));
    }

    private void applyJsonToProfile(ClientTournamentProfile tp, JsonNode json) {
        if (json.has("numPlayers")) tp.setNumPlayers(json.get("numPlayers").asInt());
        if (json.has("buyinChips")) tp.setBuyinChips(json.get("buyinChips").asInt());
        if (json.has("buyinCost")) tp.setBuyin(json.get("buyinCost").asInt());
        if (json.has("rebuys")) tp.setRebuys(json.get("rebuys").asBoolean());
        if (json.has("addons")) tp.setAddons(json.get("addons").asBoolean());

        if (json.has("blindLevels") && json.get("blindLevels").isArray()) {
            JsonNode levels = json.get("blindLevels");
            for (int i = 0; i < levels.size(); i++) {
                JsonNode level = levels.get(i);
                int small = level.has("small") ? level.get("small").asInt() : 25 * (1 << i);
                int big = level.has("big") ? level.get("big").asInt() : 50 * (1 << i);
                int ante = level.has("ante") ? level.get("ante").asInt() : 0;
                int minutes = level.has("minutes") ? level.get("minutes").asInt() : 15;
                tp.setLevel(i + 1, ante, small, big, minutes);
            }
        }
    }

    private Map<String, Object> profileToMap(ClientTournamentProfile tp) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", tp.getName());
        m.put("fileName", tp.getFile() != null ? tp.getFile().getName() : null);
        m.put("canDelete", tp.canDelete());
        m.put("numPlayers", tp.getNumPlayers());
        m.put("buyinChips", tp.getBuyinChips());
        m.put("buyinCost", tp.getBuyinCost());
        m.put("rebuys", tp.isRebuys());
        m.put("addons", tp.isAddons());
        m.put("payoutType", tp.getPayoutType());
        m.put("levelCount", tp.getLastLevel());

        // Blind levels
        List<Map<String, Object>> levels = new ArrayList<>();
        for (int i = 1; i <= tp.getLastLevel(); i++) {
            Map<String, Object> level = new LinkedHashMap<>();
            level.put("level", i);
            boolean isBreak = tp.isBreak(i);
            level.put("isBreak", isBreak);
            // Break levels have no blind/ante values — calling getSmallBlind() on a break level throws.
            level.put("smallBlind", isBreak ? null : tp.getSmallBlind(i));
            level.put("bigBlind", isBreak ? null : tp.getBigBlind(i));
            level.put("ante", isBreak ? null : tp.getAnte(i));
            level.put("minutes", tp.getMinutes(i));
            levels.add(level);
        }
        m.put("blindLevels", levels);
        return m;
    }
}
