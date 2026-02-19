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
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTableInput;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.games.poker.online.PokerDirector;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code POST /action} — submits a player action into the running game.
 * <p>
 * Supported action types:
 * <pre>
 * Betting (modes CHECK_BET, CHECK_RAISE, CALL_RAISE):
 *   {"type": "FOLD"}
 *   {"type": "CHECK"}
 *   {"type": "CALL"}
 *   {"type": "BET",   "amount": 200}
 *   {"type": "RAISE", "amount": 400}
 *   {"type": "ALL_IN"}
 *
 * Between-hand (mode DEAL):
 *   {"type": "DEAL"}
 *
 * Continue/advance (mode CONTINUE or CONTINUE_LOWER):
 *   {"type": "CONTINUE"}
 *   {"type": "CONTINUE_LOWER"}
 *
 * Rebuy/addon (mode REBUY_CHECK):
 *   {"type": "REBUY"}
 *   {"type": "ADDON"}
 *   {"type": "DECLINE_REBUY"}   // no-op; let rebuy timer expire naturally
 * </pre>
 */
class ActionHandler extends BaseHandler {

    ActionHandler(String apiKey) {
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
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body is required"));
            return;
        }

        JsonNode json = MAPPER.readTree(body);
        if (!json.has("type")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'type' field"));
            return;
        }

        String type = json.get("type").asText("").toUpperCase();
        int amount = json.has("amount") ? json.get("amount").asInt(0) : 0;
        int inputMode = getInputMode();

        switch (type) {
            case "FOLD", "CHECK", "CALL", "BET", "RAISE", "ALL_IN" ->
                    handleBettingAction(exchange, type, amount, inputMode);
            case "DEAL" ->
                    handleContinueAction(exchange, type, PokerGame.ACTION_CONTINUE, PokerTableInput.MODE_DEAL, inputMode);
            case "CONTINUE" ->
                    handleContinueAction(exchange, type, PokerGame.ACTION_CONTINUE, PokerTableInput.MODE_CONTINUE, inputMode);
            case "CONTINUE_LOWER" ->
                    handleContinueAction(exchange, type, PokerGame.ACTION_CONTINUE_LOWER, PokerTableInput.MODE_CONTINUE_LOWER, inputMode);
            case "REBUY" -> handleRebuy(exchange, inputMode);
            case "ADDON" -> handleAddon(exchange, inputMode);
            case "DECLINE_REBUY" -> handleDeclineRebuy(exchange, inputMode);
            default -> sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "Unknown action type: " + type +
                               ". Valid: FOLD, CHECK, CALL, BET, RAISE, ALL_IN, DEAL, CONTINUE, CONTINUE_LOWER, REBUY, ADDON, DECLINE_REBUY"));
        }
    }

    // -------------------------------------------------------------------------
    // Action dispatchers
    // -------------------------------------------------------------------------

    private void handleBettingAction(HttpExchange exchange, String type, int amount, int inputMode) throws Exception {
        if (!isBettingInputMode(inputMode)) {
            sendConflict(exchange, inputMode);
            return;
        }
        int actionId = switch (type) {
            case "FOLD"   -> PokerGame.ACTION_FOLD;
            case "CHECK"  -> PokerGame.ACTION_CHECK;
            case "CALL"   -> PokerGame.ACTION_CALL;
            case "BET"    -> PokerGame.ACTION_BET;
            case "RAISE"  -> PokerGame.ACTION_RAISE;
            case "ALL_IN" -> PokerGame.ACTION_ALL_IN;
            default       -> -1;
        };
        dispatchPlayerAction(actionId, amount);
        sendJson(exchange, 200, Map.of("accepted", true, "type", type, "amount", amount));
    }

    private void handleContinueAction(HttpExchange exchange, String type, int actionId,
                                      int expectedMode, int inputMode) throws Exception {
        if (inputMode != expectedMode) {
            sendConflict(exchange, inputMode);
            return;
        }
        dispatchPlayerAction(actionId, 0);
        sendJson(exchange, 200, Map.of("accepted", true, "type", type));
    }

    private void handleRebuy(HttpExchange exchange, int inputMode) throws Exception {
        if (inputMode != PokerTableInput.MODE_REBUY_CHECK) {
            sendConflict(exchange, inputMode);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            PokerGame game = getGame();
            PokerDirector director = getDirector();
            if (game == null || director == null) return;
            PokerPlayer human = game.getHumanPlayer();
            TournamentProfile prof = game.getProfile();
            if (human == null || prof == null) return;
            boolean pending = human.isInHand();
            director.doRebuy(human, game.getLevel(), prof.getRebuyCost(), prof.getRebuyChips(), pending);
        });
        sendJson(exchange, 200, Map.of("accepted", true, "type", "REBUY"));
    }

    private void handleAddon(HttpExchange exchange, int inputMode) throws Exception {
        if (inputMode != PokerTableInput.MODE_REBUY_CHECK) {
            sendConflict(exchange, inputMode);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            PokerGame game = getGame();
            PokerDirector director = getDirector();
            if (game == null || director == null) return;
            PokerPlayer human = game.getHumanPlayer();
            TournamentProfile prof = game.getProfile();
            if (human == null || prof == null) return;
            director.doAddon(human, prof.getAddonCost(), prof.getAddonChips());
        });
        sendJson(exchange, 200, Map.of("accepted", true, "type", "ADDON"));
    }

    private void handleDeclineRebuy(HttpExchange exchange, int inputMode) throws Exception {
        if (inputMode != PokerTableInput.MODE_REBUY_CHECK) {
            sendConflict(exchange, inputMode);
            return;
        }
        // Declining a rebuy is a no-op at the API level — the game's rebuy timer
        // will expire naturally. Use tournament profiles with rebuys=false to avoid
        // this mode entirely in automated tests.
        sendJson(exchange, 200, Map.of("accepted", true, "type", "DECLINE_REBUY",
                "note", "Rebuy timer will expire naturally"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void dispatchPlayerAction(int actionId, int amount) {
        SwingUtilities.invokeLater(() -> {
            PokerGame game = getGame();
            if (game != null) game.playerActionPerformed(actionId, amount);
        });
    }

    private void sendConflict(HttpExchange exchange, int inputMode) throws Exception {
        String modeName = inputModeToString(inputMode);
        List<String> available = availableActionsForMode(inputMode);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Conflict");
        body.put("message", "Cannot perform this action in current input mode");
        body.put("inputMode", modeName);
        body.put("availableActions", available);
        sendJson(exchange, 409, body);
    }

    private int getInputMode() {
        PokerGame game = getGame();
        return game == null ? PokerTableInput.MODE_NONE : game.getInputMode();
    }

    private PokerGame getGame() {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) return null;
        GameContext context = main.getDefaultContext();
        if (context == null) return null;
        return (PokerGame) context.getGame();
    }

    private PokerDirector getDirector() {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) return null;
        GameContext context = main.getDefaultContext();
        if (context == null) return null;
        return context.getGameManager() instanceof PokerDirector pd ? pd : null;
    }

    private boolean isBettingInputMode(int mode) {
        return mode == PokerTableInput.MODE_CHECK_BET
                || mode == PokerTableInput.MODE_CHECK_RAISE
                || mode == PokerTableInput.MODE_CALL_RAISE;
    }

    static String inputModeToString(int mode) {
        return switch (mode) {
            case PokerTableInput.MODE_NONE          -> "NONE";
            case PokerTableInput.MODE_DEAL          -> "DEAL";
            case PokerTableInput.MODE_CHECK_BET     -> "CHECK_BET";
            case PokerTableInput.MODE_CHECK_RAISE   -> "CHECK_RAISE";
            case PokerTableInput.MODE_CALL_RAISE    -> "CALL_RAISE";
            case PokerTableInput.MODE_QUITSAVE      -> "QUITSAVE";
            case PokerTableInput.MODE_CONTINUE_LOWER-> "CONTINUE_LOWER";
            case PokerTableInput.MODE_CONTINUE      -> "CONTINUE";
            case PokerTableInput.MODE_REBUY_CHECK   -> "REBUY_CHECK";
            default                                 -> "UNKNOWN_" + mode;
        };
    }

    static List<String> availableActionsForMode(int mode) {
        return switch (mode) {
            case PokerTableInput.MODE_CHECK_BET     -> List.of("FOLD", "CHECK", "BET", "ALL_IN");
            case PokerTableInput.MODE_CHECK_RAISE   -> List.of("FOLD", "CHECK", "RAISE", "ALL_IN");
            case PokerTableInput.MODE_CALL_RAISE    -> List.of("FOLD", "CALL", "RAISE", "ALL_IN");
            case PokerTableInput.MODE_DEAL          -> List.of("DEAL");
            case PokerTableInput.MODE_CONTINUE      -> List.of("CONTINUE");
            case PokerTableInput.MODE_CONTINUE_LOWER-> List.of("CONTINUE_LOWER");
            case PokerTableInput.MODE_REBUY_CHECK   -> List.of("REBUY", "ADDON", "DECLINE_REBUY");
            default                                 -> List.of();
        };
    }
}
