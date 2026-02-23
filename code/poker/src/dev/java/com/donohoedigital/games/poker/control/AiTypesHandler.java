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
import com.donohoedigital.games.poker.ai.PlayerType;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/**
 * {@code GET /ai-types} — list all AI player type profiles.
 */
class AiTypesHandler extends BaseHandler {

    AiTypesHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        List<BaseProfile> profiles = PlayerType.getProfileList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (BaseProfile bp : profiles) {
            if (bp instanceof PlayerType pt) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", pt.getName());
                m.put("fileName", pt.getFile() != null ? pt.getFile().getName() : null);
                m.put("canDelete", pt.canDelete());
                m.put("canEdit", pt.canEdit());
                m.put("description", pt.getDescription());
                m.put("skillLevel", PlayerType.toSkillLevel(pt));
                result.add(m);
            }
        }
        sendJson(exchange, 200, Map.of("aiTypes", result));
    }
}
