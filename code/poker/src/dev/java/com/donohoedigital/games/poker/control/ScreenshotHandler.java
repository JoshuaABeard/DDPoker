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

import com.donohoedigital.games.engine.EngineWindow;
import com.donohoedigital.games.poker.PokerMain;
import com.sun.net.httpserver.HttpExchange;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code GET /screenshot} — captures the game window and returns a PNG image.
 */
class ScreenshotHandler extends BaseHandler {

    ScreenshotHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        // Obtain window bounds on the EDT (safe for Swing state access)
        AtomicReference<Rectangle> boundsRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            PokerMain main = PokerMain.getPokerMain();
            if (main != null && main.getDefaultContext() != null) {
                EngineWindow frame = main.getDefaultContext().getFrame();
                if (frame != null && frame.isShowing()) {
                    boundsRef.set(frame.getBounds());
                }
            }
        });

        Rectangle bounds = boundsRef.get();
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            sendJson(exchange, 503, Map.of("error", "WindowUnavailable",
                    "message", "Game window is not visible or not yet initialized"));
            return;
        }

        // Robot.createScreenCapture does NOT require the EDT
        Robot robot = new Robot();
        BufferedImage image = robot.createScreenCapture(bounds);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        sendBytes(exchange, 200, "image/png", pngBytes);
    }
}
