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

import com.donohoedigital.games.config.GameConfigUtils;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.util.*;

/**
 * {@code GET /game/saves} — list saved game files on disk.
 */
class SaveListHandler extends BaseHandler {

    SaveListHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        File saveDir = GameConfigUtils.getSaveDir();
        List<Map<String, Object>> saves = new ArrayList<>();

        if (saveDir != null && saveDir.isDirectory()) {
            File[] files = saveDir.listFiles((dir, name) -> name.endsWith(".sav") || name.endsWith(".dat"));
            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                for (File f : files) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("name", f.getName());
                    entry.put("size", f.length());
                    entry.put("lastModified", f.lastModified());
                    saves.add(entry);
                }
            }
        }

        sendJson(exchange, 200, Map.of(
                "saveDir", saveDir != null ? saveDir.getAbsolutePath() : null,
                "saves", saves,
                "count", saves.size()));
    }
}
