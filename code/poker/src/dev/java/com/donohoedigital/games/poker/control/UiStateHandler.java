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
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.Phase;
import com.donohoedigital.games.poker.GamePrefsPanel;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.dashboard.DashboardHeader;
import com.donohoedigital.games.poker.dashboard.DashboardPanel;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.AbstractButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code GET /ui/state} — Swing/UI observability snapshot for scenario tests.
 */
class UiStateHandler extends BaseHandler {

    UiStateHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        PokerMain main = PokerMain.getPokerMain();
        GameContext context = main == null ? null : main.getDefaultContext();
        sendJson(exchange, 200, buildStateSnapshotOnEdt(context));
    }

    static Map<String, Object> buildStateSnapshotOnEdt(GameContext context) {
        if (SwingUtilities.isEventDispatchThread()) {
            return buildStateSnapshot(context);
        }

        AtomicReference<Map<String, Object>> ref = new AtomicReference<>(Map.of());
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(buildStateSnapshot(context)));
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("error", "UiSnapshotUnavailable");
            fallback.put("message", e.getMessage() != null ? e.getMessage() : "unknown");
            return fallback;
        }
        return ref.get();
    }

    static Map<String, Object> buildSummarySnapshotOnEdt(GameContext context) {
        if (SwingUtilities.isEventDispatchThread()) {
            return buildSummarySnapshot(context);
        }

        AtomicReference<Map<String, Object>> ref = new AtomicReference<>(Map.of());
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(buildSummarySnapshot(context)));
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("currentPhaseClass", "NONE");
            fallback.put("currentUIPhaseClass", "NONE");
            fallback.put("frameShowing", false);
            fallback.put("frameVisible", false);
            fallback.put("frameMaximized", false);
            fallback.put("frameBounds", Map.of("x", 0, "y", 0, "width", 0, "height", 0));
            fallback.put("layout", Map.of(
                    "dashboard", Map.of("present", false, "itemCount", 0, "openItemCount", 0, "itemTitles", List.of()),
                    "preferences", Map.of("present", false, "tabCount", 0, "selectedTabTitle", "", "tabTitles", List.of())));
            fallback.put("dashboardVisible", false);
            fallback.put("dashboardItemCount", 0);
            fallback.put("dashboardOpenItemCount", 0);
            fallback.put("preferencesVisible", false);
            fallback.put("preferencesTabCount", 0);
            fallback.put("preferencesSelectedTab", "");
            fallback.put("uiSnapshotError", e.getMessage() != null ? e.getMessage() : "unknown");
            return fallback;
        }
        return ref.get();
    }

    private static Map<String, Object> buildStateSnapshot(GameContext context) {
        Map<String, Object> out = new LinkedHashMap<>();

        Phase currentPhase = context == null ? null : context.getCurrentPhase();
        Phase currentUiPhase = context == null ? null : context.getCurrentUIPhase();
        EngineWindow frame = context == null ? null : context.getFrame();

        out.put("currentPhaseClass", currentPhase != null ? currentPhase.getClass().getSimpleName() : "NONE");
        out.put("currentUIPhaseClass", currentUiPhase != null ? currentUiPhase.getClass().getSimpleName() : "NONE");
        out.put("lifecyclePhase", currentUiPhase != null ? currentUiPhase.getGamePhase().getName() : "NONE");

        Map<String, Object> frameMap = new LinkedHashMap<>();
        if (frame != null) {
            frameMap.put("title", frame.getTitle());
            frameMap.put("showing", frame.isShowing());
            frameMap.put("visible", frame.isVisible());
            frameMap.put("maximized", (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0);
            frameMap.put("extendedState", frame.getExtendedState());
            frameMap.put("bounds", boundsMap(frame.getBounds()));
        } else {
            frameMap.put("title", "");
            frameMap.put("showing", false);
            frameMap.put("visible", false);
            frameMap.put("maximized", false);
            frameMap.put("extendedState", 0);
            frameMap.put("bounds", boundsMap(new Rectangle(0, 0, 0, 0)));
        }
        out.put("frame", frameMap);

        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        out.put("activeWindowClass", active != null ? active.getClass().getSimpleName() : "NONE");
        out.put("activeWindowTitle", windowTitle(active));

        List<Window> rawWindows = new ArrayList<>();
        List<Map<String, Object>> windows = new ArrayList<>();
        int visibleDialogs = 0;
        for (Window w : Window.getWindows()) {
            if (w == null || !w.isDisplayable()) {
                continue;
            }
            rawWindows.add(w);

            Map<String, Object> wm = new LinkedHashMap<>();
            wm.put("class", w.getClass().getSimpleName());
            wm.put("title", windowTitle(w));
            wm.put("showing", w.isShowing());
            wm.put("visible", w.isVisible());
            wm.put("focused", w.isFocused());
            wm.put("active", w == active);
            wm.put("bounds", boundsMap(w.getBounds()));
            windows.add(wm);

            if (w instanceof Dialog d && d.isShowing()) {
                visibleDialogs++;
            }
        }

        out.put("windows", windows);
        out.put("windowCount", windows.size());
        out.put("visibleDialogCount", visibleDialogs);
        out.put("layout", buildLayoutSnapshot(rawWindows));
        return out;
    }

    private static Map<String, Object> buildSummarySnapshot(GameContext context) {
        Map<String, Object> ui = new LinkedHashMap<>();

        Phase currentPhase = context == null ? null : context.getCurrentPhase();
        Phase currentUiPhase = context == null ? null : context.getCurrentUIPhase();
        EngineWindow frame = context == null ? null : context.getFrame();

        ui.put("currentPhaseClass", currentPhase != null ? currentPhase.getClass().getSimpleName() : "NONE");
        ui.put("currentUIPhaseClass", currentUiPhase != null ? currentUiPhase.getClass().getSimpleName() : "NONE");

        if (frame == null) {
            ui.put("frameShowing", false);
            ui.put("frameVisible", false);
            ui.put("frameMaximized", false);
            ui.put("frameBounds", Map.of("x", 0, "y", 0, "width", 0, "height", 0));
        } else {
            ui.put("frameShowing", frame.isShowing());
            ui.put("frameVisible", frame.isVisible());
            ui.put("frameMaximized", (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0);
            ui.put("frameBounds", Map.of(
                    "x", frame.getX(),
                    "y", frame.getY(),
                    "width", frame.getWidth(),
                    "height", frame.getHeight()));
        }

        List<Window> rawWindows = new ArrayList<>();
        for (Window w : Window.getWindows()) {
            if (w != null && w.isDisplayable()) {
                rawWindows.add(w);
            }
        }

        Map<String, Object> layout = buildLayoutSummary(rawWindows);
        ui.put("layout", layout);

        Map<String, Object> dashboard = asMap(layout.get("dashboard"));
        Map<String, Object> preferences = asMap(layout.get("preferences"));
        ui.put("dashboardVisible", asBoolean(dashboard.get("present")));
        ui.put("dashboardItemCount", asInt(dashboard.get("itemCount")));
        ui.put("dashboardOpenItemCount", asInt(dashboard.get("openItemCount")));
        ui.put("preferencesVisible", asBoolean(preferences.get("present")));
        ui.put("preferencesTabCount", asInt(preferences.get("tabCount")));
        ui.put("preferencesSelectedTab", String.valueOf(preferences.getOrDefault("selectedTabTitle", "")));

        return ui;
    }

    private static Map<String, Object> buildLayoutSnapshot(List<Window> windows) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("dashboard", buildDashboardLayout(windows));
        layout.put("preferences", buildPreferencesLayout(windows));
        return layout;
    }

    private static Map<String, Object> buildLayoutSummary(List<Window> windows) {
        Map<String, Object> full = buildLayoutSnapshot(windows);

        Map<String, Object> dashboard = asMap(full.get("dashboard"));
        Map<String, Object> dashboardSummary = new LinkedHashMap<>();
        dashboardSummary.put("present", asBoolean(dashboard.get("present")));
        dashboardSummary.put("itemCount", asInt(dashboard.get("itemCount")));
        dashboardSummary.put("openItemCount", asInt(dashboard.get("openItemCount")));
        List<String> dashboardTitles = new ArrayList<>();
        for (Object itemObj : asList(dashboard.get("items"))) {
            Map<String, Object> item = asMap(itemObj);
            String title = String.valueOf(item.getOrDefault("title", ""));
            if (!title.isBlank()) {
                dashboardTitles.add(title);
            }
        }
        dashboardSummary.put("itemTitles", dashboardTitles);

        Map<String, Object> preferences = asMap(full.get("preferences"));
        Map<String, Object> preferencesSummary = new LinkedHashMap<>();
        preferencesSummary.put("present", asBoolean(preferences.get("present")));
        preferencesSummary.put("tabCount", asInt(preferences.get("tabCount")));
        preferencesSummary.put("selectedTabTitle", String.valueOf(preferences.getOrDefault("selectedTabTitle", "")));
        preferencesSummary.put("tabTitles", preferences.getOrDefault("tabTitles", List.of()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("dashboard", dashboardSummary);
        summary.put("preferences", preferencesSummary);
        return summary;
    }

    private static Map<String, Object> buildDashboardLayout(List<Window> windows) {
        List<Map<String, Object>> panels = new ArrayList<>();
        for (Window window : windows) {
            for (DashboardPanel panel : findComponents(window, DashboardPanel.class)) {
                panels.add(buildDashboardPanelSnapshot(window, panel));
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("present", !panels.isEmpty());
        out.put("panelCount", panels.size());

        Map<String, Object> primary = null;
        for (Map<String, Object> panel : panels) {
            if (asBoolean(panel.get("showing"))) {
                primary = panel;
                break;
            }
        }
        if (primary == null && !panels.isEmpty()) {
            primary = panels.get(0);
        }

        if (primary == null) {
            out.put("itemCount", 0);
            out.put("openItemCount", 0);
            out.put("items", List.of());
        } else {
            out.put("itemCount", asInt(primary.get("itemCount")));
            out.put("openItemCount", asInt(primary.get("openItemCount")));
            out.put("items", primary.getOrDefault("items", List.of()));
        }
        out.put("panels", panels);
        return out;
    }

    private static Map<String, Object> buildDashboardPanelSnapshot(Window window, DashboardPanel panel) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Map<String, Object>> items = collectDashboardItems(panel);

        int openCount = 0;
        for (Map<String, Object> item : items) {
            if (asBoolean(item.get("selected"))) {
                openCount++;
            }
        }

        map.put("windowClass", window.getClass().getSimpleName());
        map.put("windowTitle", windowTitle(window));
        map.put("windowBounds", boundsMap(window.getBounds()));
        map.put("panelBounds", boundsMap(panel.getBounds()));
        map.put("showing", panel.isShowing());
        map.put("visible", panel.isVisible());
        map.put("itemCount", items.size());
        map.put("openItemCount", openCount);
        map.put("items", items);
        return map;
    }

    private static List<Map<String, Object>> collectDashboardItems(DashboardPanel panel) {
        List<DashboardHeader> headers = findComponents(panel, DashboardHeader.class);
        headers.sort(Comparator
                .comparingInt(Component::getY)
                .thenComparingInt(Component::getX));

        List<Map<String, Object>> items = new ArrayList<>();
        for (DashboardHeader header : headers) {
            items.add(buildDashboardHeaderSnapshot(header));
        }
        return items;
    }

    private static Map<String, Object> buildDashboardHeaderSnapshot(DashboardHeader header) {
        AbstractButton toggle = null;
        List<AbstractButton> buttons = findComponents(header, AbstractButton.class);
        for (AbstractButton button : buttons) {
            if (button.getText() != null && !button.getText().isBlank()) {
                toggle = button;
                break;
            }
        }
        if (toggle == null && !buttons.isEmpty()) {
            toggle = buttons.get(0);
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", toggle != null && toggle.getText() != null ? toggle.getText() : "");
        item.put("selected", toggle != null && toggle.isSelected());
        item.put("visible", header.isVisible());
        item.put("showing", header.isShowing());
        item.put("bounds", boundsMap(header.getBounds()));
        return item;
    }

    private static Map<String, Object> buildPreferencesLayout(List<Window> windows) {
        List<Map<String, Object>> panels = new ArrayList<>();
        for (Window window : windows) {
            for (GamePrefsPanel panel : findComponents(window, GamePrefsPanel.class)) {
                panels.add(buildPreferencesPanelSnapshot(window, panel));
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("present", !panels.isEmpty());
        out.put("panelCount", panels.size());

        Map<String, Object> primary = null;
        for (Map<String, Object> panel : panels) {
            if (asBoolean(panel.get("showing"))) {
                primary = panel;
                break;
            }
        }
        if (primary == null && !panels.isEmpty()) {
            primary = panels.get(0);
        }

        if (primary == null) {
            out.put("tabCount", 0);
            out.put("selectedTabIndex", -1);
            out.put("selectedTabTitle", "");
            out.put("tabTitles", List.of());
            out.put("tabs", List.of());
        } else {
            out.put("tabCount", asInt(primary.get("tabCount")));
            out.put("selectedTabIndex", asInt(primary.get("selectedTabIndex")));
            out.put("selectedTabTitle", String.valueOf(primary.getOrDefault("selectedTabTitle", "")));
            out.put("tabTitles", primary.getOrDefault("tabTitles", List.of()));
            out.put("tabs", primary.getOrDefault("tabs", List.of()));
        }

        out.put("panels", panels);
        return out;
    }

    private static Map<String, Object> buildPreferencesPanelSnapshot(Window window, GamePrefsPanel panel) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("windowClass", window.getClass().getSimpleName());
        map.put("windowTitle", windowTitle(window));
        map.put("windowBounds", boundsMap(window.getBounds()));
        map.put("panelBounds", boundsMap(panel.getBounds()));
        map.put("showing", panel.isShowing());
        map.put("visible", panel.isVisible());

        JTabbedPane tabs = findFirstComponent(panel, JTabbedPane.class);
        if (tabs == null) {
            map.put("tabCount", 0);
            map.put("selectedTabIndex", -1);
            map.put("selectedTabTitle", "");
            map.put("tabTitles", List.of());
            map.put("tabs", List.of());
            return map;
        }

        int tabCount = tabs.getTabCount();
        int selectedIndex = tabs.getSelectedIndex();
        String selectedTitle = selectedIndex >= 0 && selectedIndex < tabCount
                ? tabs.getTitleAt(selectedIndex)
                : "";

        List<Map<String, Object>> tabSnapshots = new ArrayList<>();
        List<String> tabTitles = new ArrayList<>();
        for (int i = 0; i < tabCount; i++) {
            String title = tabs.getTitleAt(i) != null ? tabs.getTitleAt(i) : "";
            tabTitles.add(title);

            Rectangle bounds;
            try {
                bounds = tabs.getBoundsAt(i);
            } catch (RuntimeException e) {
                bounds = new Rectangle(0, 0, 0, 0);
            }

            Map<String, Object> tab = new LinkedHashMap<>();
            tab.put("index", i);
            tab.put("title", title);
            tab.put("enabled", tabs.isEnabledAt(i));
            tab.put("selected", i == selectedIndex);
            tab.put("bounds", boundsMap(bounds));
            tabSnapshots.add(tab);
        }

        map.put("tabCount", tabCount);
        map.put("selectedTabIndex", selectedIndex);
        map.put("selectedTabTitle", selectedTitle != null ? selectedTitle : "");
        map.put("tabTitles", tabTitles);
        map.put("tabs", tabSnapshots);
        return map;
    }

    private static <T extends Component> List<T> findComponents(Component root, Class<T> type) {
        List<T> out = new ArrayList<>();
        if (root == null) {
            return out;
        }
        collectComponents(root, type, out);
        return out;
    }

    private static <T extends Component> T findFirstComponent(Component root, Class<T> type) {
        if (root == null) {
            return null;
        }
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = findFirstComponent(child, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static <T extends Component> void collectComponents(Component node, Class<T> type, List<T> out) {
        if (type.isInstance(node)) {
            out.add(type.cast(node));
        }
        if (node instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectComponents(child, type, out);
            }
        }
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return copy;
        }
        return Map.of();
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return false;
    }

    private static int asInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static Map<String, Object> boundsMap(Rectangle bounds) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("x", bounds.x);
        b.put("y", bounds.y);
        b.put("width", bounds.width);
        b.put("height", bounds.height);
        return b;
    }

    private static String windowTitle(Window window) {
        if (window instanceof Frame frame) {
            return frame.getTitle() != null ? frame.getTitle() : "";
        }
        if (window instanceof Dialog dialog) {
            return dialog.getTitle() != null ? dialog.getTitle() : "";
        }
        return "";
    }
}
