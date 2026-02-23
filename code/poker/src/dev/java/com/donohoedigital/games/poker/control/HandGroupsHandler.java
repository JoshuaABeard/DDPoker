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
import com.donohoedigital.games.poker.HandGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/**
 * {@code GET/POST/DELETE /hand-groups} — CRUD for starting hand groups.
 */
class HandGroupsHandler extends BaseHandler {

    HandGroupsHandler(String apiKey) {
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
        List<BaseProfile> profiles = HandGroup.getProfileList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (BaseProfile bp : profiles) {
            if (bp instanceof HandGroup hg) {
                result.add(groupToMap(hg));
            }
        }
        sendJson(exchange, 200, Map.of("handGroups", result));
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
        for (BaseProfile bp : HandGroup.getProfileList()) {
            if (bp.getName().equalsIgnoreCase(name)) {
                sendJson(exchange, 409, Map.of("error", "Conflict", "message", "Hand group already exists: " + name));
                return;
            }
        }

        HandGroup hg = new HandGroup(name);
        if (json.has("description")) {
            hg.setDescription(json.get("description").asText());
        }
        hg.initFile();
        hg.save();

        sendJson(exchange, 201, Map.of("created", true, "handGroup", groupToMap(hg)));
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

        for (BaseProfile bp : HandGroup.getProfileList()) {
            if (bp.getName().equalsIgnoreCase(name)) {
                if (!bp.canDelete()) {
                    sendJson(exchange, 403, Map.of("error", "Forbidden", "message", "Built-in hand group cannot be deleted"));
                    return;
                }
                bp.getFile().delete();
                sendJson(exchange, 200, Map.of("deleted", true, "name", name));
                return;
            }
        }

        sendJson(exchange, 404, Map.of("error", "NotFound", "message", "No hand group named: " + name));
    }

    private Map<String, Object> groupToMap(HandGroup hg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", hg.getName());
        m.put("fileName", hg.getFile() != null ? hg.getFile().getName() : null);
        m.put("canDelete", hg.canDelete());
        m.put("description", hg.getDescription());
        m.put("handCount", hg.getHandCount());
        m.put("classCount", hg.getClassCount());
        m.put("percent", hg.getPercent());
        m.put("summary", hg.getSummary());
        return m;
    }
}
