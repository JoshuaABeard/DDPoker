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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Base class for all dev control server HTTP handlers.
 * Provides API key validation and JSON response helpers.
 */
abstract class BaseHandler implements HttpHandler {

    protected final Logger logger = LogManager.getLogger(getClass());
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;

    BaseHandler(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            String key = exchange.getRequestHeaders().getFirst("X-Control-Key");
            if (!apiKey.equals(key)) {
                sendJson(exchange, 403, Map.of("error", "Forbidden", "message", "Missing or invalid X-Control-Key header"));
                return;
            }
            handleAuthenticated(exchange);
        } catch (Exception e) {
            logger.error("Error handling {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), e);
            try {
                sendJson(exchange, 500, Map.of("error", "InternalError", "message",
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            } catch (IOException ignored) {
            }
        }
    }

    /** Implement request handling after authentication has passed. */
    protected abstract void handleAuthenticated(HttpExchange exchange) throws Exception;

    // -------------------------------------------------------------------------
    // Response helpers
    // -------------------------------------------------------------------------

    protected void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendBytes(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected byte[] readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return is.readAllBytes();
        }
    }

    protected String readRequestBodyAsString(HttpExchange exchange) throws IOException {
        return new String(readRequestBody(exchange), StandardCharsets.UTF_8);
    }
}
