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
import com.donohoedigital.games.engine.Phase;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.gui.BaseFrame;
import com.donohoedigital.gui.DDComboBox;
import com.donohoedigital.gui.DDNumberSpinner;
import com.donohoedigital.gui.DDOption;
import com.donohoedigital.gui.InternalDialog;
import com.donohoedigital.gui.OptionBoolean;
import com.donohoedigital.gui.OptionCombo;
import com.donohoedigital.gui.OptionInteger;
import com.donohoedigital.gui.OptionSlider;
import com.donohoedigital.gui.OptionText;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code GET/POST /ui/dialogs} — dialog observability and interaction controls.
 */
class UiDialogsHandler extends BaseHandler {

    UiDialogsHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        switch (exchange.getRequestMethod()) {
            case "GET" -> handleGet(exchange);
            case "POST" -> handlePost(exchange);
            default -> sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
        }
    }

    private void handleGet(HttpExchange exchange) throws Exception {
        PokerMain main = PokerMain.getPokerMain();
        GameContext context = main == null ? null : main.getDefaultContext();
        sendJson(exchange, 200, snapshotOnEdt(context));
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

        String action = text(body, "action").toUpperCase(Locale.ROOT);
        if (action.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "action is required"));
            return;
        }

        AtomicReference<DialogActionResult> ref = new AtomicReference<>();
        Runnable runner = () -> ref.set(applyOnEdt(action, body));
        if (SwingUtilities.isEventDispatchThread()) {
            runner.run();
        } else {
            SwingUtilities.invokeAndWait(runner);
        }

        DialogActionResult result = ref.get();
        sendJson(exchange, result.status(), result.body());
    }

    private DialogActionResult applyOnEdt(String action, JsonNode body) {
        if (!isSupportedAction(action)) {
            return badRequest(action, "Unknown action: " + action
                    + ". Valid: CLOSE, CLICK_BUTTON, SELECT_TAB, SET_OPTION, SET_COMPONENT");
        }

        PokerMain main = PokerMain.getPokerMain();
        GameContext context = main == null ? null : main.getDefaultContext();
        if (context == null || context.getFrame() == null) {
            return conflict(action, "Game context is not initialized");
        }

        List<DialogRef> dialogs = collectDialogs(context);
        DialogRef target = selectDialog(dialogs, text(body, "dialog"));

        return switch (action) {
            case "CLOSE", "CLOSE_DIALOG" -> doClose(action, body, target, context);
            case "CLICK_BUTTON" -> doClickButton(action, body, target);
            case "SELECT_TAB" -> doSelectTab(action, body, target);
            case "SET_OPTION" -> doSetOption(action, body, target);
            case "SET_COMPONENT" -> doSetComponent(action, body, target);
            default -> badRequest(action, "Unknown action: " + action);
        };
    }

    private static boolean isSupportedAction(String action) {
        return "CLOSE".equals(action)
                || "CLOSE_DIALOG".equals(action)
                || "CLICK_BUTTON".equals(action)
                || "SELECT_TAB".equals(action)
                || "SET_OPTION".equals(action)
                || "SET_COMPONENT".equals(action);
    }

    private DialogActionResult doClose(String action, JsonNode body, DialogRef target, GameContext context) {
        if (target == null) {
            return conflict(action, "No matching dialog");
        }

        target.dialog().removeDialog();
        String nextPhase = text(body, "nextPhase");
        if (!nextPhase.isBlank()) {
            context.processPhase(nextPhase);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accepted", true);
        out.put("action", action);
        out.put("dialogId", target.id());
        out.put("dialogTitle", target.dialog().getTitle());
        if (!nextPhase.isBlank()) {
            out.put("nextPhase", nextPhase);
        }
        out.put("snapshot", snapshotOnEdt(context));
        return new DialogActionResult(200, out);
    }

    private DialogActionResult doClickButton(String action, JsonNode body, DialogRef target) {
        if (target == null) {
            return conflict(action, "No matching dialog");
        }

        String buttonSelector = text(body, "button");
        if (buttonSelector.isBlank()) {
            return badRequest(action, "button is required");
        }

        AbstractButton button = findButton(target.dialog(), buttonSelector);
        if (button == null) {
            return new DialogActionResult(404,
                    Map.of("accepted", false, "action", action, "error", "NotFound",
                            "message", "No button matches selector: " + buttonSelector));
        }

        if (!button.isEnabled()) {
            return conflict(action, "Button is disabled: " + buttonSelector);
        }

        button.doClick(120);
        return new DialogActionResult(200,
                Map.of("accepted", true, "action", action, "dialogId", target.id(),
                        "buttonName", nullToEmpty(button.getName()),
                        "buttonText", nullToEmpty(button.getText())));
    }

    private DialogActionResult doSelectTab(String action, JsonNode body, DialogRef target) {
        if (target == null) {
            return conflict(action, "No matching dialog");
        }

        JTabbedPane tabs = findFirstComponent(target.dialog(), JTabbedPane.class);
        if (tabs == null) {
            return new DialogActionResult(404,
                    Map.of("accepted", false, "action", action, "error", "NotFound",
                            "message", "No tabbed pane found in dialog"));
        }

        int index = -1;
        if (body.has("tabIndex") && body.get("tabIndex").canConvertToInt()) {
            index = body.get("tabIndex").asInt();
        }
        String title = text(body, "tabTitle");
        if (!title.isBlank()) {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (equalsIgnoreCase(tabs.getTitleAt(i), title)) {
                    index = i;
                    break;
                }
            }
        }

        if (index < 0 || index >= tabs.getTabCount()) {
            return badRequest(action, "tabIndex/tabTitle did not match a valid tab");
        }

        tabs.setSelectedIndex(index);
        return new DialogActionResult(200,
                Map.of("accepted", true, "action", action, "dialogId", target.id(),
                        "selectedTabIndex", index,
                        "selectedTabTitle", nullToEmpty(tabs.getTitleAt(index))));
    }

    private DialogActionResult doSetOption(String action, JsonNode body, DialogRef target) {
        if (target == null) {
            return conflict(action, "No matching dialog");
        }

        String optionName = text(body, "option");
        if (optionName.isBlank()) {
            return badRequest(action, "option is required");
        }
        if (!body.has("value")) {
            return badRequest(action, "value is required");
        }

        DDOption option = findOption(target.dialog(), optionName);
        if (option == null) {
            return new DialogActionResult(404,
                    Map.of("accepted", false, "action", action, "error", "NotFound",
                            "message", "No option matches: " + optionName));
        }
        if (!option.isEnabled()) {
            return conflict(action, "Option is disabled: " + optionName);
        }

        String error = applyOptionValue(option, body.get("value"));
        if (error != null) {
            return badRequest(action, error);
        }

        return new DialogActionResult(200,
                Map.of("accepted", true, "action", action, "dialogId", target.id(),
                        "option", optionName,
                        "value", optionValue(option)));
    }

    private DialogActionResult doSetComponent(String action, JsonNode body, DialogRef target) {
        if (target == null) {
            return conflict(action, "No matching dialog");
        }

        String componentName = text(body, "componentName");
        if (componentName.isBlank()) {
            return badRequest(action, "componentName is required");
        }
        if (!body.has("value")) {
            return badRequest(action, "value is required");
        }

        Component component = findNamedComponent(target.dialog(), componentName);
        if (component == null) {
            return new DialogActionResult(404,
                    Map.of("accepted", false, "action", action, "error", "NotFound",
                            "message", "No component matches: " + componentName));
        }

        String error = applyComponentValue(component, body.get("value"));
        if (error != null) {
            return badRequest(action, error);
        }

        return new DialogActionResult(200,
                Map.of("accepted", true, "action", action, "dialogId", target.id(),
                        "componentName", componentName,
                        "componentClass", component.getClass().getSimpleName()));
    }

    private static String applyOptionValue(DDOption option, JsonNode valueNode) {
        if (option instanceof OptionBoolean booleanOption) {
            if (!valueNode.isBoolean()) {
                return "Option " + option.getName() + " expects boolean value";
            }
            boolean desired = valueNode.asBoolean();
            if (booleanOption.getCheckBox().isSelected() != desired) {
                booleanOption.getCheckBox().doClick(120);
            }
            return null;
        }

        if (option instanceof OptionCombo comboOption) {
            DDComboBox combo = comboOption.getComboBox();
            if (valueNode.isInt()) {
                int idx = valueNode.asInt();
                if (idx < 0 || idx >= combo.getItemCount()) {
                    return "Option " + option.getName() + " tab index out of range";
                }
                combo.setSelectedIndex(idx);
                return null;
            }
            String selected = valueNode.asText();
            combo.setSelectedItem(selected);
            return null;
        }

        if (option instanceof OptionInteger integerOption) {
            if (!valueNode.canConvertToInt()) {
                return "Option " + option.getName() + " expects integer value";
            }
            integerOption.getSpinner().setValue(valueNode.asInt());
            return null;
        }

        if (option instanceof OptionSlider sliderOption) {
            if (!valueNode.canConvertToInt()) {
                return "Option " + option.getName() + " expects integer value";
            }
            sliderOption.getSlider().setValue(valueNode.asInt());
            return null;
        }

        if (option instanceof OptionText textOption) {
            textOption.getTextField().setText(valueNode.asText());
            return null;
        }

        return "Unsupported option type: " + option.getClass().getSimpleName();
    }

    private static String applyComponentValue(Component component, JsonNode valueNode) {
        if (component instanceof AbstractButton button) {
            if (valueNode.isBoolean()) {
                boolean desired = valueNode.asBoolean();
                if (button.isSelected() != desired) {
                    button.doClick(120);
                }
                return null;
            }
            button.doClick(120);
            return null;
        }

        if (component instanceof JComboBox<?> combo) {
            if (valueNode.isInt()) {
                int idx = valueNode.asInt();
                if (idx < 0 || idx >= combo.getItemCount()) {
                    return "Combo index out of range";
                }
                combo.setSelectedIndex(idx);
                return null;
            }
            combo.setSelectedItem(valueNode.asText());
            return null;
        }

        if (component instanceof JTextComponent text) {
            text.setText(valueNode.asText());
            return null;
        }

        if (component instanceof DDNumberSpinner spinner) {
            if (!valueNode.canConvertToInt()) {
                return "Number spinner expects integer value";
            }
            spinner.setValue(valueNode.asInt());
            return null;
        }

        if (component instanceof JSlider slider) {
            if (!valueNode.canConvertToInt()) {
                return "Slider expects integer value";
            }
            slider.setValue(valueNode.asInt());
            return null;
        }

        return "Unsupported component type: " + component.getClass().getSimpleName();
    }

    private static Object optionValue(DDOption option) {
        if (option instanceof OptionBoolean booleanOption) {
            return booleanOption.getCheckBox().isSelected();
        }
        if (option instanceof OptionCombo comboOption) {
            return comboOption.getComboBox().getSelectedValue();
        }
        if (option instanceof OptionInteger integerOption) {
            return integerOption.getSpinner().getValue();
        }
        if (option instanceof OptionSlider sliderOption) {
            return sliderOption.getSlider().getValue();
        }
        if (option instanceof OptionText textOption) {
            return textOption.getTextField().getText();
        }
        return null;
    }

    private static Map<String, Object> snapshotOnEdt(GameContext context) {
        if (SwingUtilities.isEventDispatchThread()) {
            return snapshot(context);
        }

        AtomicReference<Map<String, Object>> ref = new AtomicReference<>(Map.of());
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(snapshot(context)));
        } catch (Exception e) {
            return Map.of(
                    "dialogCount", 0,
                    "dialogs", List.of(),
                    "error", "UiSnapshotUnavailable",
                    "message", e.getMessage() != null ? e.getMessage() : "unknown");
        }
        return ref.get();
    }

    private static Map<String, Object> snapshot(GameContext context) {
        List<DialogRef> dialogs = collectDialogs(context);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dialogCount", dialogs.size());
        out.put("dialogs", dialogs.stream().map(UiDialogsHandler::dialogSnapshot).toList());

        Phase currentUi = context == null ? null : context.getCurrentUIPhase();
        out.put("currentUIPhaseClass", currentUi != null ? currentUi.getClass().getSimpleName() : "NONE");
        out.put("currentUIPhaseName", currentUi != null && currentUi.getGamePhase() != null
                ? currentUi.getGamePhase().getName()
                : "NONE");
        return out;
    }

    private static Map<String, Object> dialogSnapshot(DialogRef ref) {
        InternalDialog dialog = ref.dialog();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", ref.id());
        out.put("name", nullToEmpty(dialog.getName()));
        out.put("title", nullToEmpty(dialog.getTitle()));
        out.put("class", dialog.getClass().getSimpleName());
        out.put("visible", dialog.isVisible());
        out.put("showing", dialog.isShowing());
        out.put("iconified", dialog.isIcon());
        out.put("selected", dialog.isSelected());
        out.put("modal", dialog.isModal());
        out.put("layer", ref.layer());
        out.put("bounds", Map.of(
                "x", dialog.getX(),
                "y", dialog.getY(),
                "width", dialog.getWidth(),
                "height", dialog.getHeight()));

        out.put("buttons", collectButtonsSnapshot(dialog));
        out.put("tabs", collectTabsSnapshot(dialog));
        out.put("options", collectOptionsSnapshot(dialog));
        out.put("namedComponents", collectNamedComponentsSnapshot(dialog));
        return out;
    }

    private static List<Map<String, Object>> collectButtonsSnapshot(Component root) {
        List<Map<String, Object>> buttons = new ArrayList<>();
        for (AbstractButton button : findComponents(root, AbstractButton.class)) {
            if (button.getName() == null && button.getText() == null) {
                continue;
            }
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("name", nullToEmpty(button.getName()));
            b.put("text", nullToEmpty(button.getText()));
            b.put("class", button.getClass().getSimpleName());
            b.put("enabled", button.isEnabled());
            b.put("visible", button.isVisible());
            b.put("selected", button.isSelected());
            buttons.add(b);
        }
        return buttons;
    }

    private static List<Map<String, Object>> collectTabsSnapshot(Component root) {
        List<Map<String, Object>> tabs = new ArrayList<>();
        int paneOrdinal = 0;
        for (JTabbedPane pane : findComponents(root, JTabbedPane.class)) {
            for (int i = 0; i < pane.getTabCount(); i++) {
                Map<String, Object> tab = new LinkedHashMap<>();
                tab.put("pane", paneOrdinal);
                tab.put("index", i);
                tab.put("title", nullToEmpty(pane.getTitleAt(i)));
                tab.put("selected", pane.getSelectedIndex() == i);
                tab.put("enabled", pane.isEnabledAt(i));
                tabs.add(tab);
            }
            paneOrdinal++;
        }
        return tabs;
    }

    private static List<Map<String, Object>> collectOptionsSnapshot(Component root) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (DDOption option : findComponents(root, DDOption.class)) {
            String name = nullToEmpty(option.getName());
            if (name.isBlank()) {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("type", option.getClass().getSimpleName());
            row.put("enabled", option.isEnabled());
            row.put("value", optionValue(option));

            if (option instanceof OptionCombo comboOption) {
                List<String> choices = new ArrayList<>();
                for (Object v : comboOption.getComboBox().getDefaultValues()) {
                    choices.add(String.valueOf(v));
                }
                row.put("choices", choices);
            } else if (option instanceof OptionInteger integerOption) {
                row.put("min", integerOption.getSpinner().getMin());
                row.put("max", integerOption.getSpinner().getMax());
            } else if (option instanceof OptionSlider sliderOption) {
                row.put("min", sliderOption.getSlider().getMinimum());
                row.put("max", sliderOption.getSlider().getMaximum());
            }

            options.add(row);
        }
        return options;
    }

    private static List<Map<String, Object>> collectNamedComponentsSnapshot(Component root) {
        List<Map<String, Object>> components = new ArrayList<>();
        for (JComponent component : findComponents(root, JComponent.class)) {
            String name = component.getName();
            if (name == null || name.isBlank()) {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("class", component.getClass().getSimpleName());
            row.put("enabled", component.isEnabled());
            row.put("visible", component.isVisible());
            components.add(row);
        }
        return components;
    }

    private static List<DialogRef> collectDialogs(GameContext context) {
        if (context == null || context.getFrame() == null) {
            return List.of();
        }

        BaseFrame frame = context.getFrame();
        Set<InternalDialog> dialogs = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        collectDialogs(frame.getLayeredPane(), dialogs);

        List<DialogRef> refs = new ArrayList<>();
        List<InternalDialog> sorted = new ArrayList<>(dialogs);
        sorted.sort(Comparator
                .comparingInt((InternalDialog d) -> frame.getLayeredPane().getLayer(d)).reversed()
                .thenComparing(InternalDialog::isSelected).reversed()
                .thenComparing(InternalDialog::getTitle, String.CASE_INSENSITIVE_ORDER));

        int idx = 1;
        for (InternalDialog dialog : sorted) {
            int layer = frame.getLayeredPane().getLayer(dialog);
            refs.add(new DialogRef("dialog-" + idx, dialog, layer));
            idx++;
        }
        return refs;
    }

    private static void collectDialogs(Component node, Set<InternalDialog> out) {
        if (node == null) {
            return;
        }
        if (node instanceof InternalDialog dialog) {
            out.add(dialog);
        }
        if (node instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectDialogs(child, out);
            }
        }
    }

    private static DialogRef selectDialog(List<DialogRef> refs, String selector) {
        if (refs.isEmpty()) {
            return null;
        }

        if (selector == null || selector.isBlank()) {
            for (DialogRef ref : refs) {
                if (ref.dialog().isVisible() && !ref.dialog().isIcon()) {
                    return ref;
                }
            }
            return refs.get(0);
        }

        for (DialogRef ref : refs) {
            if (matchesDialog(ref, selector)) {
                return ref;
            }
        }
        return null;
    }

    private static boolean matchesDialog(DialogRef ref, String selector) {
        String s = selector.toLowerCase(Locale.ROOT);
        return ref.id().equalsIgnoreCase(selector)
                || containsIgnoreCase(ref.dialog().getName(), s)
                || containsIgnoreCase(ref.dialog().getTitle(), s)
                || containsIgnoreCase(ref.dialog().getClass().getSimpleName(), s);
    }

    private static boolean containsIgnoreCase(String value, String tokenLower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(tokenLower);
    }

    private static AbstractButton findButton(Component root, String selector) {
        List<AbstractButton> buttons = findComponents(root, AbstractButton.class);

        for (AbstractButton button : buttons) {
            if (equalsIgnoreCase(button.getName(), selector)) {
                return button;
            }
        }
        for (AbstractButton button : buttons) {
            if (equalsIgnoreCase(button.getText(), selector)) {
                return button;
            }
        }
        for (AbstractButton button : buttons) {
            if (startsWithIgnoreCase(button.getName(), selector)) {
                return button;
            }
        }
        for (AbstractButton button : buttons) {
            if (containsIgnoreCase(button.getText(), selector.toLowerCase(Locale.ROOT))) {
                return button;
            }
        }
        return null;
    }

    private static DDOption findOption(Component root, String name) {
        for (DDOption option : findComponents(root, DDOption.class)) {
            if (equalsIgnoreCase(option.getName(), name)) {
                return option;
            }
        }
        return null;
    }

    private static Component findNamedComponent(Component root, String componentName) {
        for (JComponent component : findComponents(root, JComponent.class)) {
            if (equalsIgnoreCase(component.getName(), componentName)) {
                return component;
            }
        }
        return null;
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return Objects.equals(a == null ? null : a.toLowerCase(Locale.ROOT),
                b == null ? null : b.toLowerCase(Locale.ROOT));
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value != null && prefix != null
                && value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String text(JsonNode node, String key) {
        return node.has(key) && !node.get(key).isNull() ? node.get(key).asText().trim() : "";
    }

    private static <T extends Component> List<T> findComponents(Component root, Class<T> type) {
        List<T> out = new ArrayList<>();
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

    private static DialogActionResult badRequest(String action, String message) {
        return new DialogActionResult(400,
                Map.of("accepted", false, "action", action, "error", "BadRequest", "message", message));
    }

    private static DialogActionResult conflict(String action, String message) {
        return new DialogActionResult(409,
                Map.of("accepted", false, "action", action, "error", "Conflict", "message", message));
    }

    private record DialogRef(String id, InternalDialog dialog, int layer) {
    }

    private record DialogActionResult(int status, Object body) {
    }
}
