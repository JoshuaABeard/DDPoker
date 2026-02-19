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
import com.donohoedigital.games.engine.ProfileList;
import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.PlayerProfileOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles player profile endpoints:
 * <ul>
 *   <li>{@code GET  /profiles}         — list all profiles on disk</li>
 *   <li>{@code POST /profiles}         — create a new profile and make it the default</li>
 *   <li>{@code GET  /profiles/default} — return the currently active default profile name</li>
 * </ul>
 */
class ProfilesHandler extends BaseHandler {

    ProfilesHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equals(method) && path.endsWith("/profiles/default")) {
            handleGetDefault(exchange);
        } else if ("GET".equals(method)) {
            handleList(exchange);
        } else if ("POST".equals(method)) {
            handleCreate(exchange);
        } else {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
        }
    }

    // -------------------------------------------------------------------------
    // GET /profiles
    // -------------------------------------------------------------------------

    private void handleList(HttpExchange exchange) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<BaseProfile> profiles = PlayerProfile.getProfileList();
            if (profiles != null) {
                for (BaseProfile p : profiles) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("name", p.getName());
                    entry.put("fileName", p.getFileName());
                    result.add(entry);
                }
            }
        } catch (Exception e) {
            // ConfigManager may not be initialized in test environments
            logger.debug("Could not load profile list (ConfigManager not available)", e);
        }
        sendJson(exchange, 200, Map.of("profiles", result));
    }

    // -------------------------------------------------------------------------
    // POST /profiles
    // -------------------------------------------------------------------------

    private void handleCreate(HttpExchange exchange) throws Exception {
        String body = readRequestBodyAsString(exchange);
        if (body.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body is required"));
            return;
        }

        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON: " + e.getMessage()));
            return;
        }

        if (!json.has("name")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'name' field"));
            return;
        }

        String name = json.get("name").asText("").strip();
        if (name.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "'name' must not be blank"));
            return;
        }

        // Check for duplicate
        try {
            List<BaseProfile> existing = PlayerProfile.getProfileList();
            if (existing != null) {
                for (BaseProfile p : existing) {
                    if (p.getName().equalsIgnoreCase(name)) {
                        sendJson(exchange, 409, Map.of("error", "Conflict",
                                "message", "Profile '" + name + "' already exists"));
                        return;
                    }
                }
            }

            PlayerProfile profile = new PlayerProfile(name);
            profile.init();
            profile.initFile();
            profile.save();

            // Make it the default (saves to prefs)
            ProfileList.setStoredProfile(profile, PlayerProfileOptions.PROFILE_NAME);

            sendJson(exchange, 200, Map.of("created", true, "name", name, "fileName", profile.getFileName()));
        } catch (Exception e) {
            logger.warn("Failed to create profile '{}'", name, e);
            sendJson(exchange, 500, Map.of("error", "InternalError",
                    "message", "Could not create profile: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /profiles/default
    // -------------------------------------------------------------------------

    private void handleGetDefault(HttpExchange exchange) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            PlayerProfile defaultProfile = PlayerProfileOptions.getDefaultProfile();
            result.put("defaultProfile", defaultProfile != null ? defaultProfile.getName() : null);
        } catch (Exception e) {
            // getDefaultProfile() can fail if no profiles exist or if PokerMain isn't fully initialized
            logger.debug("Could not get default profile", e);
            result.put("defaultProfile", null);
            result.put("note", "No profile available: " + e.getMessage());
        }
        sendJson(exchange, 200, result);
    }
}
