/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
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
package com.donohoedigital.games.poker.online;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes user-configured online server addresses to HTTP(S) base URLs.
 */
public final class OnlineServerUrl {

    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$");

    private OnlineServerUrl() {
    }

    /**
     * Normalizes a configured server value into a base URL.
     *
     * <p>
     * Accepts host:port values from preferences (e.g. {@code localhost:8877}) and
     * full URLs (e.g. {@code https://example.com:443}). Returns {@code null} when
     * input is blank or invalid.
     */
    public static String normalizeBaseUrl(String configuredServer) {
        if (configuredServer == null) {
            return null;
        }

        String trimmed = configuredServer.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String withScheme = SCHEME_PATTERN.matcher(trimmed).matches() ? trimmed : "http://" + trimmed;

        try {
            URI parsed = URI.create(withScheme);
            String scheme = parsed.getScheme();
            if (scheme == null) {
                return null;
            }

            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
                return null;
            }

            String host = parsed.getHost();
            if (host == null || host.isEmpty()) {
                return null;
            }

            return new URI(normalizedScheme, null, host, parsed.getPort(), null, null, null).toString();
        } catch (IllegalArgumentException | URISyntaxException e) {
            return null;
        }
    }

    /**
     * Converts an HTTP(S) base URL to its WebSocket equivalent.
     * {@code http://host:port} becomes {@code ws://host:port}.
     * {@code https://host:port} becomes {@code wss://host:port}. Returns
     * {@code null} for null input.
     */
    public static String toWsBaseUrl(String httpBaseUrl) {
        if (httpBaseUrl == null)
            return null;
        if (httpBaseUrl.startsWith("https://")) {
            return "wss://" + httpBaseUrl.substring("https://".length());
        }
        if (httpBaseUrl.startsWith("http://")) {
            return "ws://" + httpBaseUrl.substring("http://".length());
        }
        return null;
    }

    /**
     * Builds a REST API URI from a configured server value and API path.
     */
    public static URI buildApiUri(String configuredServer, String apiPath) {
        String baseUrl = normalizeBaseUrl(configuredServer);
        if (baseUrl == null || apiPath == null || apiPath.isBlank()) {
            return null;
        }

        String normalizedPath = apiPath.startsWith("/") ? apiPath : "/" + apiPath;
        try {
            return URI.create(baseUrl + normalizedPath);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
