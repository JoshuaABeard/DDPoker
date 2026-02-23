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
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/**
 * {@code GET /validate} — runs game-state invariant checks and returns a structured report.
 *
 * <p>Response shape:
 * <pre>
 * {
 *   "chipConservation": {
 *     "valid": true,
 *     "tables": [{
 *       "id": 1,
 *       "playerChips": 4400,
 *       "inPot": 100,
 *       "total": 4500,
 *       "buyinPerPlayer": 1500,
 *       "numPlayers": 3,
 *       "expectedTotal": 4500
 *     }]
 *   },
 *   "inputModeConsistent": true,
 *   "warnings": []
 * }
 * </pre>
 *
 * <p>{@code chipConservation.valid} is false if any table's {@code total != expectedTotal}.
 * {@code inputModeConsistent} is false if inputMode is a betting mode but no hand is active
 * on the current table.
 * {@code warnings} lists human-readable descriptions of any violations found.
 *
 * <p>When no game is running, all checks pass vacuously and the response contains
 * empty tables and no warnings.
 */
class ValidateHandler extends BaseHandler {

    ValidateHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }
        sendJson(exchange, 200, buildValidation());
    }

    private Map<String, Object> buildValidation() {
        PokerMain main = PokerMain.getPokerMain();
        GameContext context = main == null ? null : main.getDefaultContext();
        PokerGame game = context == null ? null : (PokerGame) context.getGame();

        List<String> warnings = new ArrayList<>();

        // Chip conservation
        Map<String, Object> chipConservation = buildChipConservation(game, warnings);

        // Input mode consistency
        boolean inputModeConsistent = checkInputModeConsistency(game, warnings);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chipConservation", chipConservation);
        result.put("inputModeConsistent", inputModeConsistent);
        result.put("warnings", warnings);
        return result;
    }

    private Map<String, Object> buildChipConservation(PokerGame game, List<String> warnings) {
        List<Map<String, Object>> tableResults = new ArrayList<>();
        boolean allValid = true;

        if (game != null) {
            List<PokerTable> tables = game.getTables();
            int buyinPerPlayer = game.getStartingChips();

            if (tables != null) {
                for (PokerTable table : tables) {
                    Map<String, Object> tableResult = checkTable(table, buyinPerPlayer, warnings);
                    tableResults.add(tableResult);
                    if (!(Boolean) tableResult.get("valid")) {
                        allValid = false;
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", allValid);
        result.put("tables", tableResults);
        return result;
    }

    private Map<String, Object> checkTable(PokerTable table, int buyinPerPlayer,
                                           List<String> warnings) {
        int playerChips = 0;
        int numPlayers = 0;
        for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
            PokerPlayer player = table.getPlayer(seat);
            if (player != null) {
                playerChips += player.getChipCount();
                numPlayers++;
            }
        }

        HoldemHand hand = table.getHoldemHand();
        int inPot = hand != null ? hand.getTotalPotChipCount() : 0;
        int total = playerChips + inPot;
        int expectedTotal = buyinPerPlayer * numPlayers;
        boolean valid = (total == expectedTotal);

        if (!valid) {
            warnings.add(String.format(
                    "Table %d chip conservation violated: total=%d expected=%d (playerChips=%d inPot=%d numPlayers=%d buyinPerPlayer=%d)",
                    table.getNumber(), total, expectedTotal,
                    playerChips, inPot, numPlayers, buyinPerPlayer));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", table.getNumber());
        result.put("valid", valid);
        result.put("playerChips", playerChips);
        result.put("inPot", inPot);
        result.put("total", total);
        result.put("buyinPerPlayer", buyinPerPlayer);
        result.put("numPlayers", numPlayers);
        result.put("expectedTotal", expectedTotal);
        return result;
    }

    private boolean checkInputModeConsistency(PokerGame game, List<String> warnings) {
        if (game == null) return true;

        int inputMode = game.getInputMode();
        boolean isBettingMode = inputMode == PokerTableInput.MODE_CHECK_BET
                || inputMode == PokerTableInput.MODE_CHECK_RAISE
                || inputMode == PokerTableInput.MODE_CALL_RAISE;

        if (!isBettingMode) return true;

        PokerTable table = game.getCurrentTable();
        if (table == null) {
            warnings.add("inputMode is " + ActionHandler.inputModeToString(inputMode)
                    + " but no current table is set");
            return false;
        }
        HoldemHand hand = table.getHoldemHand();
        if (hand == null) {
            warnings.add("inputMode is " + ActionHandler.inputModeToString(inputMode)
                    + " but no hand is active on table " + table.getNumber());
            return false;
        }
        return true;
    }
}
