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
import com.donohoedigital.games.poker.online.ClientPokerTable;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
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

        if (game == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("valid", true);
            result.put("tables", tableResults);
            return result;
        }

        List<ClientPokerTable> tables = game.getTables();
        int buyinPerPlayer = game.getStartingChips();
        // Profile holds the original configured player count (constant throughout game).
        // game.getNumPlayers() may shrink as players are eliminated and removed.
        TournamentProfile profile = game.getProfile();
        int profilePlayerCount = profile != null ? profile.getNumPlayers() : game.getNumPlayers();

        int grandTotalChips = 0;
        int grandTotalInPot = 0;
        int visiblePlayerCount = 0;

        if (tables != null) {
            for (ClientPokerTable table : tables) {
                Map<String, Object> tableResult = buildTableChipInfo(table);
                tableResults.add(tableResult);
                grandTotalChips += (int) tableResult.get("playerChips");
                grandTotalInPot += (int) tableResult.get("inPot");
                visiblePlayerCount += (int) tableResult.get("numSeated");
            }
        }

        // In multi-table tournaments, the client only sees the human's current table.
        // We cannot validate global chip conservation when tables are invisible to the
        // client. Detect this case: fewer tables visible than expected for the
        // profile's player count (assuming 10 seats per table).
        int expectedTables = (int) Math.ceil((double) profilePlayerCount / PokerConstants.SEATS);
        boolean isMultiTable = (tables == null ? 0 : tables.size()) < expectedTables && expectedTables > 1;

        int expectedTotal = game.getTotalChipsInPlay();
        if (expectedTotal <= 0) {
            expectedTotal = buyinPerPlayer * profilePlayerCount;
        }
        int grandTotal = grandTotalChips + grandTotalInPot;

        // Skip chip conservation check for multi-table — not all chips are visible.
        boolean allValid = isMultiTable || (grandTotal == expectedTotal);

        if (!allValid) {
            warnings.add(String.format(
                    "Game chip conservation violated: total=%d expected=%d"
                            + " (playerChips=%d inPot=%d visiblePlayers=%d profilePlayers=%d buyinPerPlayer=%d)",
                    grandTotal, expectedTotal,
                    grandTotalChips, grandTotalInPot, visiblePlayerCount, profilePlayerCount, buyinPerPlayer));
        }

        // Mark each table result with the game-level validity flag
        for (Map<String, Object> tableResult : tableResults) {
            tableResult.put("valid", allValid);
            tableResult.put("expectedTotal", expectedTotal);
            tableResult.put("visiblePlayerCount", visiblePlayerCount);
            tableResult.put("profilePlayerCount", profilePlayerCount);
            tableResult.put("isMultiTable", isMultiTable);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", allValid);
        result.put("tables", tableResults);
        return result;
    }

    private Map<String, Object> buildTableChipInfo(ClientPokerTable table) {
        int playerChips = 0;
        int numSeated = 0;
        for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
            PokerPlayer player = table.getPlayer(seat);
            if (player != null) {
                playerChips += player.getChipCount();
                numSeated++;
            }
        }

        HoldemHand hand = table.getHoldemHand();
        int inPot = hand != null ? hand.getTotalPotChipCount() : 0;
        int total = playerChips + inPot;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", table.getNumber());
        result.put("playerChips", playerChips);
        result.put("inPot", inPot);
        result.put("total", total);
        result.put("numSeated", numSeated);
        return result;
    }

    private boolean checkInputModeConsistency(PokerGame game, List<String> warnings) {
        if (game == null) return true;

        int inputMode = game.getInputMode();
        boolean isBettingMode = inputMode == PokerTableInput.MODE_CHECK_BET
                || inputMode == PokerTableInput.MODE_CHECK_RAISE
                || inputMode == PokerTableInput.MODE_CALL_RAISE;

        if (!isBettingMode) return true;

        ClientPokerTable table = game.getCurrentTable();
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
