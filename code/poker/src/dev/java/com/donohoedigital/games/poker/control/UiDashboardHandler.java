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

import com.donohoedigital.games.poker.dashboard.DashboardItem;
import com.donohoedigital.games.poker.dashboard.DashboardManager;
import com.donohoedigital.games.poker.dashboard.DashboardPanel;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code GET/POST /ui/dashboard} — dashboard customization observability + control.
 */
class UiDashboardHandler extends BaseHandler {

    UiDashboardHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        switch (exchange.getRequestMethod()) {
            case "GET" -> sendJson(exchange, 200, snapshotOnEdt());
            case "POST" -> handlePost(exchange);
            default -> sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
        }
    }

    private void handlePost(HttpExchange exchange) throws Exception {
        String raw = readRequestBodyAsString(exchange);
        if (raw == null || raw.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body required"));
            return;
        }

        JsonNode body;
        try {
            body = MAPPER.readTree(raw);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON"));
            return;
        }

        String action = text(body, "action").toUpperCase();
        if (action.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "action is required"));
            return;
        }

        AtomicReference<DashboardMutationResult> ref = new AtomicReference<>();
        Runnable runner = () -> ref.set(applyMutation(action, body));
        if (SwingUtilities.isEventDispatchThread()) {
            runner.run();
        } else {
            SwingUtilities.invokeAndWait(runner);
        }

        DashboardMutationResult result = ref.get();
        sendJson(exchange, result.status(), result.body());
    }

    private DashboardMutationResult applyMutation(String action, JsonNode body) {
        DashboardContext ctx = findDashboardContext();
        if (ctx == null) {
            return new DashboardMutationResult(409,
                    Map.of("error", "Conflict", "message", "Dashboard is not visible"));
        }

        String name = text(body, "name");
        DashboardItem item = name.isBlank() ? null : findItem(ctx.manager(), name);

        switch (action) {
            case "SET_DISPLAYED" -> {
                if (item == null) {
                    return missingItem(name);
                }
                if (!body.has("displayed") || !body.get("displayed").isBoolean()) {
                    return new DashboardMutationResult(400,
                            Map.of("error", "BadRequest", "message", "displayed boolean is required"));
                }
                boolean displayed = body.get("displayed").asBoolean();
                item.setInDashboard(displayed);
                ctx.panel().refreshFromManager();
                ctx.manager().stateChanged();
                return okMutation(action, item, ctx);
            }
            case "SET_OPEN" -> {
                if (item == null) {
                    return missingItem(name);
                }
                if (!body.has("open") || !body.get("open").isBoolean()) {
                    return new DashboardMutationResult(400,
                            Map.of("error", "BadRequest", "message", "open boolean is required"));
                }
                boolean open = body.get("open").asBoolean();
                item.setOpen(open);
                ctx.manager().stateChanged();
                return okMutation(action, item, ctx);
            }
            case "MOVE" -> {
                if (item == null) {
                    return missingItem(name);
                }

                String direction = text(body, "direction").toUpperCase();
                if (!"UP".equals(direction) && !"DOWN".equals(direction)) {
                    return new DashboardMutationResult(400,
                            Map.of("error", "BadRequest", "message", "direction must be UP or DOWN"));
                }

                int steps = 1;
                if (body.has("steps")) {
                    if (!body.get("steps").canConvertToInt()) {
                        return new DashboardMutationResult(400,
                                Map.of("error", "BadRequest", "message", "steps must be an integer"));
                    }
                    steps = Math.max(1, body.get("steps").asInt());
                }

                boolean up = "UP".equals(direction);
                for (int i = 0; i < steps; i++) {
                    ctx.manager().moveItem(item, up);
                }
                ctx.panel().refreshFromManager();
                ctx.manager().stateChanged();
                return okMutation(action, item, ctx);
            }
            default -> {
                return new DashboardMutationResult(400,
                        Map.of("error", "BadRequest", "message", "Unknown action: " + action));
            }
        }
    }

    private DashboardMutationResult okMutation(String action, DashboardItem item, DashboardContext ctx) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accepted", true);
        body.put("action", action);
        body.put("name", item.getName());
        body.put("snapshot", snapshot(ctx));
        return new DashboardMutationResult(200, body);
    }

    private DashboardMutationResult missingItem(String name) {
        return new DashboardMutationResult(404,
                Map.of("error", "NotFound", "message", "Unknown dashboard item: " + name));
    }

    private Map<String, Object> snapshotOnEdt() {
        if (SwingUtilities.isEventDispatchThread()) {
            return snapshot(findDashboardContext());
        }

        AtomicReference<Map<String, Object>> ref = new AtomicReference<>(Map.of());
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(snapshot(findDashboardContext())));
        } catch (Exception e) {
            return Map.of("present", false, "itemCount", 0, "items", List.of(),
                    "error", "UiSnapshotUnavailable", "message", e.getMessage() != null ? e.getMessage() : "unknown");
        }
        return ref.get();
    }

    private Map<String, Object> snapshot(DashboardContext ctx) {
        if (ctx == null) {
            return Map.of("present", false, "itemCount", 0, "displayedCount", 0, "openCount", 0, "items", List.of());
        }

        DashboardManager manager = ctx.manager();
        manager.sort();

        List<Map<String, Object>> items = new ArrayList<>();
        int displayedCount = 0;
        int openCount = 0;

        for (int i = 0; i < manager.getNumItems(); i++) {
            DashboardItem item = manager.getItem(i);
            boolean displayed = item.isInDashboard();
            boolean open = item.isOpen();
            if (displayed) {
                displayedCount++;
                if (open) {
                    openCount++;
                }
            }

            Map<String, Object> im = new LinkedHashMap<>();
            im.put("name", item.getName());
            im.put("title", resolveTitle(item));
            im.put("position", item.getPosition());
            im.put("displayed", displayed);
            im.put("open", open);
            items.add(im);
        }

        items.sort(Comparator.comparingInt(o -> ((Number) o.getOrDefault("position", 0)).intValue()));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("present", true);
        out.put("itemCount", items.size());
        out.put("displayedCount", displayedCount);
        out.put("openCount", openCount);
        out.put("items", items);
        return out;
    }

    private static DashboardContext findDashboardContext() {
        DashboardPanel panel = null;
        for (Window window : Window.getWindows()) {
            if (window == null || !window.isDisplayable()) {
                continue;
            }
            for (DashboardPanel candidate : findComponents(window, DashboardPanel.class)) {
                if (candidate.isShowing()) {
                    return new DashboardContext(candidate, candidate.getDashboardManager());
                }
                if (panel == null) {
                    panel = candidate;
                }
            }
        }
        return panel != null ? new DashboardContext(panel, panel.getDashboardManager()) : null;
    }

    private static DashboardItem findItem(DashboardManager manager, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (int i = 0; i < manager.getNumItems(); i++) {
            DashboardItem item = manager.getItem(i);
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    private static String resolveTitle(DashboardItem item) {
        JComponent header = item.getHeader() instanceof JComponent h ? h : null;
        if (header != null) {
            List<AbstractButton> buttons = findComponents(header, AbstractButton.class);
            for (AbstractButton button : buttons) {
                String text = button.getText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return item.getName();
    }

    private static String text(JsonNode body, String key) {
        return body.has(key) && !body.get(key).isNull() ? body.get(key).asText().trim() : "";
    }

    private static <T extends Component> List<T> findComponents(Component root, Class<T> type) {
        List<T> out = new ArrayList<>();
        collectComponents(root, type, out);
        return out;
    }

    private static <T extends Component> void collectComponents(Component node, Class<T> type, List<T> out) {
        if (node == null) {
            return;
        }
        if (type.isInstance(node)) {
            out.add(type.cast(node));
        }
        if (node instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectComponents(child, type, out);
            }
        }
    }

    private record DashboardContext(DashboardPanel panel, DashboardManager manager) {
    }

    private record DashboardMutationResult(int status, Object body) {
    }
}
