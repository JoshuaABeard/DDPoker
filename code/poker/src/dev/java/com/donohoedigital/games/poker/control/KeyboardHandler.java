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
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.engine.EngineWindow;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Map;

/**
 * {@code POST /keyboard} — inject keyboard events into the game window.
 *
 * <p>Request body: {@code {"key": "D"}} or {@code {"key": "F"}} etc.
 *
 * <p>Supported keys: single characters (A-Z, 0-9) and special names
 * (ESCAPE, ENTER, TAB, F1-F12, CTRL_T, CTRL_P).
 */
class KeyboardHandler extends BaseHandler {

    KeyboardHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

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

        String keyName = json.has("key") ? json.get("key").asText().trim().toUpperCase() : "";
        if (keyName.isEmpty()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "key is required"));
            return;
        }

        PokerMain main = PokerMain.getPokerMain();
        if (main == null) {
            sendJson(exchange, 503, Map.of("error", "ServiceUnavailable", "message", "PokerMain not initialized"));
            return;
        }

        int keyCode;
        int modifiers = 0;

        switch (keyName) {
            case "ESCAPE" -> keyCode = KeyEvent.VK_ESCAPE;
            case "ENTER" -> keyCode = KeyEvent.VK_ENTER;
            case "TAB" -> keyCode = KeyEvent.VK_TAB;
            case "SPACE" -> keyCode = KeyEvent.VK_SPACE;
            case "F1" -> keyCode = KeyEvent.VK_F1;
            case "F2" -> keyCode = KeyEvent.VK_F2;
            case "F3" -> keyCode = KeyEvent.VK_F3;
            case "F4" -> keyCode = KeyEvent.VK_F4;
            case "F5" -> keyCode = KeyEvent.VK_F5;
            case "F6" -> keyCode = KeyEvent.VK_F6;
            case "F7" -> keyCode = KeyEvent.VK_F7;
            case "F8" -> keyCode = KeyEvent.VK_F8;
            case "F9" -> keyCode = KeyEvent.VK_F9;
            case "F10" -> keyCode = KeyEvent.VK_F10;
            case "F11" -> keyCode = KeyEvent.VK_F11;
            case "F12" -> keyCode = KeyEvent.VK_F12;
            case "CTRL_T" -> { keyCode = KeyEvent.VK_T; modifiers = KeyEvent.CTRL_DOWN_MASK; }
            case "CTRL_P" -> { keyCode = KeyEvent.VK_P; modifiers = KeyEvent.CTRL_DOWN_MASK; }
            default -> {
                if (keyName.length() == 1) {
                    char c = keyName.charAt(0);
                    keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
                    if (keyCode == KeyEvent.VK_UNDEFINED) {
                        sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Unknown key: " + keyName));
                        return;
                    }
                } else {
                    sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Unknown key: " + keyName));
                    return;
                }
            }
        }

        int finalKeyCode = keyCode;
        int finalModifiers = modifiers;

        SwingUtilities.invokeLater(() -> {
            try {
                Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focused == null) {
                    GameContext ctx = main.getDefaultContext();
                    EngineWindow frame = ctx != null ? ctx.getFrame() : null;
                    focused = frame != null ? frame : main.getDefaultContext().getFrame();
                }
                long now = System.currentTimeMillis();
                KeyEvent press = new KeyEvent(focused, KeyEvent.KEY_PRESSED, now, finalModifiers, finalKeyCode, KeyEvent.CHAR_UNDEFINED);
                KeyEvent release = new KeyEvent(focused, KeyEvent.KEY_RELEASED, now + 10, finalModifiers, finalKeyCode, KeyEvent.CHAR_UNDEFINED);
                focused.dispatchEvent(press);
                focused.dispatchEvent(release);
                logger.info("Dispatched key: {} (code={}, modifiers={})", keyName, finalKeyCode, finalModifiers);
            } catch (Exception e) {
                logger.error("Error dispatching key: {}", keyName, e);
            }
        });

        sendJson(exchange, 200, Map.of("accepted", true, "key", keyName));
    }
}
