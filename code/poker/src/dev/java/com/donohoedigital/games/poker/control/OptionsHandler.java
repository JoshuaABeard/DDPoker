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

import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.PokerUtils;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dev-only HTTP handler for {@code GET /options} and {@code POST /options}.
 *
 * <p>{@code GET /options} returns a snapshot of all key game options:
 * <pre>{@code
 * {
 *   "cheat": {
 *     "neverbroke": false, "aifaceup": false, "showfold": false,
 *     "showmuck": false, "showdown": true, "popups": false,
 *     "mouseover": false, "pausecards": false
 *   },
 *   "gameplay": {
 *     "pauseAllin": false, "pauseColor": false, "zipMode": false,
 *     "aiDelayMs": 0, "autodeal": true
 *   },
 *   "display": {
 *     "largeCards": false, "fourColorDeck": false, "holeCardsDown": false
 *   }
 * }
 * }</pre>
 *
 * <p>{@code POST /options} sets one or more options using dot-separated keys:
 * <pre>{@code {"cheat.neverbroke": true, "gameplay.aiDelayMs": 500}}</pre>
 *
 * <p>Supported keys:
 * <ul>
 *   <li>Boolean: {@code cheat.neverbroke}, {@code cheat.aifaceup}, {@code cheat.showfold},
 *       {@code cheat.showmuck}, {@code cheat.showdown}, {@code cheat.popups},
 *       {@code cheat.mouseover}, {@code cheat.pausecards},
 *       {@code gameplay.pauseAllin}, {@code gameplay.pauseColor}, {@code gameplay.zipMode},
 *       {@code gameplay.autodeal},
 *       {@code display.largeCards}, {@code display.fourColorDeck},
 *       {@code display.holeCardsDown}</li>
 *   <li>Integer: {@code gameplay.aiDelayMs}</li>
 * </ul>
 */
class OptionsHandler extends BaseHandler {

    // Maps logical dot-key -> prefs key for boolean options
    private static final Map<String, String> BOOL_KEYS = new LinkedHashMap<>();
    // Maps logical dot-key -> prefs key for int options
    private static final Map<String, String> INT_KEYS = new LinkedHashMap<>();

    static {
        BOOL_KEYS.put("cheat.neverbroke",      PokerConstants.OPTION_CHEAT_NEVERBROKE);
        BOOL_KEYS.put("cheat.aifaceup",        PokerConstants.OPTION_CHEAT_AIFACEUP);
        BOOL_KEYS.put("cheat.showfold",        PokerConstants.OPTION_CHEAT_SHOWFOLD);
        BOOL_KEYS.put("cheat.showmuck",        PokerConstants.OPTION_CHEAT_SHOW_MUCKED);
        BOOL_KEYS.put("cheat.showdown",        PokerConstants.OPTION_CHEAT_SHOWWINNINGHAND);
        BOOL_KEYS.put("cheat.popups",          PokerConstants.OPTION_CHEAT_POPUP);
        BOOL_KEYS.put("cheat.mouseover",       PokerConstants.OPTION_CHEAT_MOUSEOVER);
        BOOL_KEYS.put("cheat.pausecards",      PokerConstants.OPTION_CHEAT_PAUSECARDS);
        BOOL_KEYS.put("gameplay.pauseAllin",   PokerConstants.OPTION_PAUSE_ALLIN);
        BOOL_KEYS.put("gameplay.pauseColor",   PokerConstants.OPTION_PAUSE_COLOR);
        BOOL_KEYS.put("gameplay.zipMode",      PokerConstants.OPTION_ZIP_MODE);
        BOOL_KEYS.put("gameplay.autodeal",     PokerConstants.OPTION_AUTODEAL);
        BOOL_KEYS.put("gameplay.checkfold",    PokerConstants.OPTION_CHECKFOLD);
        BOOL_KEYS.put("display.largeCards",    PokerConstants.OPTION_LARGE_CARDS);
        BOOL_KEYS.put("display.fourColorDeck", PokerConstants.OPTION_FOUR_COLOR_DECK);
        BOOL_KEYS.put("display.holeCardsDown", PokerConstants.OPTION_HOLE_CARDS_DOWN);
        BOOL_KEYS.put("display.stylizedFaceCards", PokerConstants.OPTION_STYLIZED_FACE_CARDS);
        BOOL_KEYS.put("display.showPlayerType",    PokerConstants.OPTION_SHOW_PLAYER_TYPE);
        BOOL_KEYS.put("display.rightClickOnly",    PokerConstants.OPTION_RIGHT_CLICK_ONLY);
        BOOL_KEYS.put("display.disableShortcuts",  PokerConstants.OPTION_DISABLE_SHORTCUTS);
        BOOL_KEYS.put("clock.colorUpNotify",       PokerConstants.OPTION_CLOCK_COLOUP);
        BOOL_KEYS.put("clock.pauseAtLevelEnd",     PokerConstants.OPTION_CLOCK_PAUSE);
        BOOL_KEYS.put("advisor.enabled",           PokerConstants.OPTION_DEFAULT_ADVISOR);
        BOOL_KEYS.put("cheat.rabbithunt",          PokerConstants.OPTION_CHEAT_RABBITHUNT);
        BOOL_KEYS.put("cheat.manualbutton",        PokerConstants.OPTION_CHEAT_MANUAL_BUTTON);

        INT_KEYS.put("gameplay.aiDelayMs",         PokerConstants.OPTION_DELAY);
        INT_KEYS.put("gameplay.autodealHand",      PokerConstants.OPTION_AUTODEALHAND);
        INT_KEYS.put("gameplay.autodealFold",      PokerConstants.OPTION_AUTODEALFOLD);
        INT_KEYS.put("gameplay.handsPerHour",      PokerConstants.OPTION_HANDS_PER_HOUR);
        INT_KEYS.put("chat.dealer",                PokerConstants.OPTION_CHAT_DEALER);
        INT_KEYS.put("chat.display",               PokerConstants.OPTION_CHAT_DISPLAY);
        INT_KEYS.put("chat.fontSize",              PokerConstants.OPTION_CHAT_FONT_SIZE);
        INT_KEYS.put("screenshot.maxWidth",        PokerConstants.OPTION_SCREENSHOT_MAX_WIDTH);
        INT_KEYS.put("screenshot.maxHeight",       PokerConstants.OPTION_SCREENSHOT_MAX_HEIGHT);
    }

    OptionsHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "GET" -> handleGet(exchange);
            case "POST" -> handlePost(exchange);
            default -> sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
        }
    }

    private void handleGet(HttpExchange exchange) throws Exception {
        Map<String, Object> cheat = new LinkedHashMap<>();
        cheat.put("neverbroke",  PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_NEVERBROKE));
        cheat.put("aifaceup",    PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_AIFACEUP));
        cheat.put("showfold",    PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_SHOWFOLD));
        cheat.put("showmuck",    PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_SHOW_MUCKED));
        cheat.put("showdown",    PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_SHOWWINNINGHAND));
        cheat.put("popups",      PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_POPUP));
        cheat.put("mouseover",   PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_MOUSEOVER));
        cheat.put("pausecards",  PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_PAUSECARDS));

        Map<String, Object> gameplay = new LinkedHashMap<>();
        gameplay.put("pauseAllin",  PokerUtils.isOptionOn(PokerConstants.OPTION_PAUSE_ALLIN));
        gameplay.put("pauseColor",  PokerUtils.isOptionOn(PokerConstants.OPTION_PAUSE_COLOR));
        gameplay.put("zipMode",     PokerUtils.isOptionOn(PokerConstants.OPTION_ZIP_MODE));
        gameplay.put("aiDelayMs",   PokerUtils.getIntOption(PokerConstants.OPTION_DELAY));
        gameplay.put("autodeal",    PokerUtils.isOptionOn(PokerConstants.OPTION_AUTODEAL));
        gameplay.put("autodealHand", PokerUtils.getIntOption(PokerConstants.OPTION_AUTODEALHAND));
        gameplay.put("autodealFold", PokerUtils.getIntOption(PokerConstants.OPTION_AUTODEALFOLD));
        gameplay.put("checkfold",   PokerUtils.isOptionOn(PokerConstants.OPTION_CHECKFOLD));
        gameplay.put("handsPerHour", PokerUtils.getIntOption(PokerConstants.OPTION_HANDS_PER_HOUR));

        Map<String, Object> display = new LinkedHashMap<>();
        display.put("largeCards",       PokerUtils.isOptionOn(PokerConstants.OPTION_LARGE_CARDS));
        display.put("fourColorDeck",    PokerUtils.isOptionOn(PokerConstants.OPTION_FOUR_COLOR_DECK));
        display.put("holeCardsDown",    PokerUtils.isOptionOn(PokerConstants.OPTION_HOLE_CARDS_DOWN));
        display.put("stylizedFaceCards", PokerUtils.isOptionOn(PokerConstants.OPTION_STYLIZED_FACE_CARDS));
        display.put("showPlayerType",   PokerUtils.isOptionOn(PokerConstants.OPTION_SHOW_PLAYER_TYPE));
        display.put("rightClickOnly",   PokerUtils.isOptionOn(PokerConstants.OPTION_RIGHT_CLICK_ONLY));
        display.put("disableShortcuts", PokerUtils.isOptionOn(PokerConstants.OPTION_DISABLE_SHORTCUTS));

        Map<String, Object> clock = new LinkedHashMap<>();
        clock.put("colorUpNotify",   PokerUtils.isOptionOn(PokerConstants.OPTION_CLOCK_COLOUP));
        clock.put("pauseAtLevelEnd", PokerUtils.isOptionOn(PokerConstants.OPTION_CLOCK_PAUSE));

        Map<String, Object> advisor = new LinkedHashMap<>();
        advisor.put("enabled", PokerUtils.isOptionOn(PokerConstants.OPTION_DEFAULT_ADVISOR));

        Map<String, Object> chat = new LinkedHashMap<>();
        chat.put("dealer",   PokerUtils.getIntOption(PokerConstants.OPTION_CHAT_DEALER));
        chat.put("display",  PokerUtils.getIntOption(PokerConstants.OPTION_CHAT_DISPLAY));
        chat.put("fontSize", PokerUtils.getIntOption(PokerConstants.OPTION_CHAT_FONT_SIZE));

        Map<String, Object> screenshot = new LinkedHashMap<>();
        screenshot.put("maxWidth",  PokerUtils.getIntOption(PokerConstants.OPTION_SCREENSHOT_MAX_WIDTH));
        screenshot.put("maxHeight", PokerUtils.getIntOption(PokerConstants.OPTION_SCREENSHOT_MAX_HEIGHT));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cheat",      cheat);
        result.put("gameplay",   gameplay);
        result.put("display",    display);
        result.put("clock",      clock);
        result.put("advisor",    advisor);
        result.put("chat",       chat);
        result.put("screenshot", screenshot);
        sendJson(exchange, 200, result);
    }

    private void handlePost(HttpExchange exchange) throws Exception {
        String rawBody = readRequestBodyAsString(exchange);
        if (rawBody.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body is required"));
            return;
        }
        Map<String, Object> body;
        try {
            body = MAPPER.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON: " + e.getMessage()));
            return;
        }

        Map<String, Object> changed = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : body.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (BOOL_KEYS.containsKey(key)) {
                if (!(value instanceof Boolean)) {
                    errors.put(key, "expected boolean, got: " + value);
                    continue;
                }
                boolean bval = (Boolean) value;
                GameEngine.getGameEngine().getPrefsNode().putBoolean(BOOL_KEYS.get(key), bval);
                changed.put(key, bval);

            } else if (INT_KEYS.containsKey(key)) {
                if (!(value instanceof Number)) {
                    errors.put(key, "expected number, got: " + value);
                    continue;
                }
                int ival = ((Number) value).intValue();
                GameEngine.getGameEngine().getPrefsNode().putInt(INT_KEYS.get(key), ival);
                changed.put(key, ival);

            } else {
                errors.put(key, "unknown option key");
            }
        }

        if (!errors.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "InvalidOptions");
            err.put("details", errors);
            if (!changed.isEmpty()) {
                err.put("partialChanges", changed);
            }
            sendJson(exchange, 400, err);
            return;
        }

        sendJson(exchange, 200, Map.of("accepted", true, "changed", changed));
    }
}
