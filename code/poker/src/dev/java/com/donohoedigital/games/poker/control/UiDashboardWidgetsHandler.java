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
import com.donohoedigital.games.poker.online.ClientHoldemHand;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.online.ClientPokerTable;
import com.donohoedigital.games.poker.PokerTableInput;
import com.donohoedigital.games.poker.PokerUtils;
import com.donohoedigital.games.poker.dashboard.AdvisorState;
import com.donohoedigital.games.poker.dashboard.DashboardAdvisor;
import com.donohoedigital.games.poker.dashboard.DashboardItem;
import com.donohoedigital.games.poker.dashboard.DashboardManager;
import com.donohoedigital.games.poker.dashboard.DashboardPanel;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code GET /ui/dashboard/widgets} — semantic dashboard widget snapshot.
 */
class UiDashboardWidgetsHandler extends BaseHandler {
    private static final AtomicLong STATE_SEQ = new AtomicLong(0L);
    private static final AtomicLong LAST_STATE_UPDATED_AT_MS = new AtomicLong(0L);
    private static final AtomicReference<String> LAST_STATE_FINGERPRINT = new AtomicReference<>("");

    UiDashboardWidgetsHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }
        sendJson(exchange, 200, snapshotOnEdt());
    }

    private Map<String, Object> snapshotOnEdt() {
        if (SwingUtilities.isEventDispatchThread()) {
            return buildSnapshot();
        }

        AtomicReference<Map<String, Object>> ref = new AtomicReference<>(Map.of());
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(buildSnapshot()));
        } catch (Exception e) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("present", false);
            out.put("error", "UiSnapshotUnavailable");
            out.put("message", e.getMessage() != null ? e.getMessage() : "unknown");
            out.put("source", Map.of(
                    "updatedAtMs", System.currentTimeMillis(),
                    "sourceStateSeq", 0,
                    "sourceHandNumber", 0,
                    "sourceRound", "NONE",
                    "sourceInputMode", "NONE"));
            out.put("widgets", Map.of());
            return out;
        }
        return ref.get();
    }

    private Map<String, Object> buildSnapshot() {
        DashboardContext ctx = findDashboardContext();
        PokerMain main = PokerMain.getPokerMain();
        GameContext context = main == null ? null : main.getDefaultContext();
        PokerGame game = context != null && context.getGame() instanceof PokerGame pokerGame ? pokerGame : null;

        SnapshotState state = buildState(game);
        Map<String, DashboardItem> itemsByName = new LinkedHashMap<>();
        if (ctx != null) {
            DashboardManager manager = ctx.manager();
            manager.sort();
            for (int i = 0; i < manager.getNumItems(); i++) {
                DashboardItem item = manager.getItem(i);
                itemsByName.put(item.getName().toLowerCase(), item);
            }
        }

        Map<String, Object> widgets = new LinkedHashMap<>();

        DashboardItem playerInfoItem = itemsByName.get("playerstyle");
        String playerInfoVariant = "playerstyle";
        if (playerInfoItem == null) {
            playerInfoItem = itemsByName.get("playerinfo");
            playerInfoVariant = playerInfoItem != null ? "playerinfo" : "none";
        }

        widgets.put("clock", widgetEntry("clock", itemsByName.get("clock"), clockData(state), state));
        widgets.put("playerInfo", widgetEntry("playerInfo", playerInfoItem,
                playerInfoData(state, playerInfoVariant), state));
        widgets.put("advisor", widgetEntry("advisor", itemsByName.get("advisor"), advisorData(state), state));
        widgets.put("simulator", widgetEntry("simulator", itemsByName.get("simulator"), simulatorData(), state));
        widgets.put("handStrength", widgetEntry("handStrength", itemsByName.get("handstrength"),
                handStrengthData(state), state));
        widgets.put("potOdds", widgetEntry("potOdds", itemsByName.get("potodds"), potOddsData(state), state));
        widgets.put("improveOdds", widgetEntry("improveOdds", itemsByName.get("improveodds"),
                improveOddsData(state), state));
        widgets.put("myHand", widgetEntry("myHand", itemsByName.get("myhand"), myHandData(state), state));
        widgets.put("myTable", widgetEntry("myTable", itemsByName.get("mytable"), myTableData(state), state));
        widgets.put("rank", widgetEntry("rank", itemsByName.get("rank"), rankData(state), state));
        widgets.put("upNext", widgetEntry("upNext", itemsByName.get("next"), upNextData(state), state));
        widgets.put("cheat", widgetEntry("cheat", itemsByName.get("cheat"), cheatData(), state));
        widgets.put("debug", widgetEntry("debug", itemsByName.get("debug"), debugData(), state));

        Map<String, Object> source = new LinkedHashMap<>();
        source.put("updatedAtMs", state.updatedAtMs());
        source.put("sourceStateSeq", state.stateSeq());
        source.put("sourceHandNumber", state.handNumber());
        source.put("sourceRound", state.round());
        source.put("sourceInputMode", state.inputModeName());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("present", ctx != null);
        out.put("source", source);
        out.put("widgets", widgets);
        return out;
    }

    private SnapshotState buildState(PokerGame game) {
        long nowMs = System.currentTimeMillis();

        if (game == null) {
            StateStamp stamp = resolveStateStamp("NO_GAME", nowMs);
            return new SnapshotState(null, null, null, null, null,
                    false, 0, "NONE", "NONE", stamp.updatedAtMs(), stamp.stateSeq());
        }

        ClientPokerTable table = game.getCurrentTable();
        if (table == null) {
            List<ClientPokerTable> tables = game.getTables();
            table = (tables != null && !tables.isEmpty()) ? tables.get(0) : null;
        }
        ClientHoldemHand hand = table != null ? table.getHoldemHand() : null;
        PokerPlayer human = game.getHumanPlayer();
        TournamentProfile profile = game.getProfile();
        int inputMode = game.getInputMode();
        boolean humanTurn = isHumanTurn(inputMode);
        int handNumber = table != null ? table.getHandNum() : 0;
        String round = hand != null ? hand.getRound().name() : "NONE";
        String inputModeName = inputModeName(inputMode);
        StateStamp stamp = resolveStateStamp(
                buildFingerprint(game, table, hand, human, inputMode, handNumber, round, humanTurn),
                nowMs);

        return new SnapshotState(game, table, hand, human, profile,
                humanTurn, handNumber, round, inputModeName, stamp.updatedAtMs(), stamp.stateSeq());
    }

    private static String buildFingerprint(PokerGame game, ClientPokerTable table, ClientHoldemHand hand, PokerPlayer human,
            int inputMode, int handNumber, String round, boolean humanTurn) {
        int tableId = table != null ? table.getNumber() : -1;
        int level = Math.max(1, game.getLevel());
        int pot = hand != null ? hand.getTotalPotChipCount() : 0;
        int callAmount = (hand != null && human != null) ? hand.getCall(human) : 0;
        PokerPlayer current = hand != null ? hand.getCurrentPlayer() : null;
        int currentSeat = current != null ? current.getSeat() : -1;
        int playersRemaining = game.getNumPlayers() - game.getNumPlayersOut();
        return tableId + "|" + level + "|" + handNumber + "|" + round + "|" + inputMode + "|"
                + humanTurn + "|" + pot + "|" + callAmount + "|" + currentSeat + "|" + playersRemaining;
    }

    private static StateStamp resolveStateStamp(String fingerprint, long nowMs) {
        synchronized (LAST_STATE_FINGERPRINT) {
            String previous = LAST_STATE_FINGERPRINT.get();
            if (!fingerprint.equals(previous)) {
                LAST_STATE_FINGERPRINT.set(fingerprint);
                long seq = STATE_SEQ.incrementAndGet();
                LAST_STATE_UPDATED_AT_MS.set(nowMs);
                return new StateStamp(seq, nowMs);
            }

            long seq = STATE_SEQ.get();
            long updatedAtMs = LAST_STATE_UPDATED_AT_MS.get();
            if (seq <= 0L) {
                seq = STATE_SEQ.incrementAndGet();
            }
            if (updatedAtMs <= 0L) {
                updatedAtMs = nowMs;
                LAST_STATE_UPDATED_AT_MS.set(updatedAtMs);
            }
            return new StateStamp(seq, updatedAtMs);
        }
    }

    private static boolean isHumanTurn(int inputMode) {
        return inputMode == PokerTableInput.MODE_CHECK_BET
                || inputMode == PokerTableInput.MODE_CHECK_RAISE
                || inputMode == PokerTableInput.MODE_CALL_RAISE;
    }

    private static String inputModeName(int inputMode) {
        return switch (inputMode) {
            case PokerTableInput.MODE_NONE -> "NONE";
            case PokerTableInput.MODE_QUITSAVE -> "QUITSAVE";
            case PokerTableInput.MODE_CHECK_BET -> "CHECK_BET";
            case PokerTableInput.MODE_CHECK_RAISE -> "CHECK_RAISE";
            case PokerTableInput.MODE_CALL_RAISE -> "CALL_RAISE";
            case PokerTableInput.MODE_DEAL -> "DEAL";
            case PokerTableInput.MODE_CONTINUE -> "CONTINUE";
            case PokerTableInput.MODE_REBUY_CHECK -> "REBUY_CHECK";
            case PokerTableInput.MODE_CONTINUE_LOWER -> "CONTINUE_LOWER";
            default -> "UNKNOWN";
        };
    }

    private static Map<String, Object> widgetEntry(String key, DashboardItem item, Map<String, Object> data,
            SnapshotState state) {
        boolean present = item != null;
        boolean displayed = present && item.isInDashboard();
        boolean open = present && item.isOpen();
        boolean actualVisible = present && item.isDisplayed();

        String reason;
        if (!present) {
            reason = "not-available-in-current-mode";
        } else if (!displayed) {
            reason = "hidden-by-dashboard-customization";
        } else {
            reason = "displayed";
        }

        Map<String, Object> visibility = new LinkedHashMap<>();
        visibility.put("expected", displayed);
        visibility.put("actual", actualVisible);
        visibility.put("reason", reason);

        Map<String, Object> freshness = new LinkedHashMap<>();
        freshness.put("updatedAtMs", state.updatedAtMs());
        freshness.put("sourceStateSeq", state.stateSeq());
        freshness.put("sourceHandNumber", state.handNumber());
        freshness.put("sourceRound", state.round());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("key", key);
        out.put("name", item != null ? item.getName() : key);
        out.put("present", present);
        out.put("displayed", displayed);
        out.put("open", open);
        out.put("visibility", visibility);
        out.put("freshness", freshness);
        out.put("data", data);
        return out;
    }

    private static Map<String, Object> clockData(SnapshotState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        PokerGame game = state.game();
        TournamentProfile profile = state.profile();

        int level = game != null ? Math.max(1, game.getLevel()) : 0;
        data.put("level", level);
        data.put("smallBlind", profile != null ? profile.getSmallBlind(level) : 0);
        data.put("bigBlind", profile != null ? profile.getBigBlind(level) : 0);
        data.put("ante", profile != null ? profile.getAnte(level) : 0);

        if (game != null && game.getGameClock() != null) {
            data.put("secondsRemaining", game.getGameClock().getSecondsRemaining());
            data.put("isPaused", game.getGameClock().isPaused());
            data.put("isExpired", game.getGameClock().isExpired());
        } else {
            data.put("secondsRemaining", 0);
            data.put("isPaused", false);
            data.put("isExpired", false);
        }

        if (profile != null && level > 0 && level < profile.getLastLevel()) {
            data.put("nextSmallBlind", profile.getSmallBlind(level + 1));
            data.put("nextBigBlind", profile.getBigBlind(level + 1));
        } else {
            data.put("nextSmallBlind", 0);
            data.put("nextBigBlind", 0);
        }

        return data;
    }

    private static Map<String, Object> playerInfoData(SnapshotState state, String variant) {
        Map<String, Object> data = new LinkedHashMap<>();
        PokerPlayer human = state.human();
        PokerGame game = state.game();

        data.put("variant", variant);
        data.put("hasHuman", human != null);

        if (human == null) {
            data.put("playerName", "");
            data.put("seat", -1);
            data.put("chips", 0);
            data.put("isObserver", false);
            data.put("playersRemaining", 0);
            data.put("rank", 0);
            return data;
        }

        data.put("playerName", human.getName());
        data.put("seat", human.getSeat());
        data.put("chips", human.getChipCount());
        data.put("isObserver", human.isObserver());
        data.put("playersRemaining", game != null ? game.getNumPlayers() - game.getNumPlayersOut() : 0);
        data.put("rank", safeRank(game, human));
        return data;
    }

    private static Map<String, Object> advisorData(SnapshotState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("isHumanTurn", state.humanTurn());
        data.put("enabledOption", isOptionOnSafe(PokerConstants.OPTION_DEFAULT_ADVISOR));

        String advice = DashboardAdvisor.getCurrentAdvice();
        String title = DashboardAdvisor.getCurrentTitle();
        data.put("advisorAdvice", advice != null ? advice : "");
        data.put("advisorTitle", title != null ? title : "");
        data.put("hasAdvice", advice != null && !advice.isBlank());
        return data;
    }

    private static Map<String, Object> simulatorData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("available", true);
        data.put("targetPhase", "CalcTool");
        return data;
    }

    private static Map<String, Object> handStrengthData(SnapshotState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        PokerPlayer human = state.human();
        String round = state.round();
        boolean postFlopRound = "FLOP".equals(round) || "TURN".equals(round) || "RIVER".equals(round);

        data.put("round", round);
        data.put("expectedStrengthValue", postFlopRound);

        if (human == null) {
            data.put("handStrength", 0.0d);
            data.put("effectiveHandStrength", 0.0d);
            return data;
        }

        data.put("handStrength", human.getHandStrength());
        data.put("effectiveHandStrength", human.getEffectiveHandStrength());
        return data;
    }

    private static Map<String, Object> potOddsData(SnapshotState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        ClientHoldemHand hand = state.hand();
        PokerPlayer human = state.human();

        if (hand == null || human == null) {
            data.put("pot", 0);
            data.put("callAmount", 0);
            data.put("potOddsPercent", 0.0d);
            data.put("oddsAgainstPercent", 0.0d);
            data.put("expectedOdds", false);
            return data;
        }

        int callAmount = hand.getCall(human);
        int pot = hand.getTotalPotChipCount();
        data.put("pot", pot);
        data.put("callAmount", callAmount);

        if (callAmount > 0) {
            double po = hand.getPotOdds(human);
            data.put("potOddsPercent", po);
            data.put("oddsAgainstPercent", po == 0.0d ? 0.0d : (100.0d - po) / po);
            data.put("expectedOdds", true);
        } else {
            data.put("potOddsPercent", 0.0d);
            data.put("oddsAgainstPercent", 0.0d);
            data.put("expectedOdds", false);
        }

        return data;
    }

    private static Map<String, Object> improveOddsData(SnapshotState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        ClientHoldemHand hand = state.hand();
        PokerPlayer human = state.human();

        int communityCards = hand != null ? hand.getCommunity().size() : 0;
        String round = state.round();
        boolean expectedCompute = ("FLOP".equals(round) || "TURN".equals(round)) && communityCards >= 3;

        data.put("round", round);
        data.put("communityCardCount", communityCards);
        data.put("expectedImproveOdds", expectedCompute);
        data.put("totalImprovePercent", computeImproveOdds());
        return data;
    }

    private static Double computeImproveOdds() {
        // Improvement odds are computed server-side and broadcast via ADVISOR_UPDATE.
        // Read from AdvisorState (same source used by the ImproveOdds dashboard widget).
        java.util.Map<String, Double> odds = AdvisorState.getImprovementOdds();
        if (odds == null) {
            return null;
        }
        double total = 0.0d;
        for (double d : odds.values()) {
            total += d;
        }
        return total;
    }

    private static Map<String, Object> myHandData(SnapshotState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        PokerPlayer human = state.human();

        if (human == null) {
            data.put("holeCards", List.of());
            data.put("hasCards", false);
            data.put("isFolded", false);
            data.put("round", state.round());
            return data;
        }

        List<String> holeCards = handToStrings(human.getHand());
        data.put("holeCards", holeCards);
        data.put("hasCards", holeCards.size() == 2);
        data.put("isFolded", human.isFolded());
        data.put("round", state.round());
        return data;
    }

    private static Map<String, Object> myTableData(SnapshotState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        ClientPokerTable table = state.table();
        PokerPlayer human = state.human();

        data.put("tableId", table != null ? table.getNumber() : -1);
        data.put("dealerSeat", table != null ? table.getButton() : -1);
        data.put("occupiedSeats", table != null ? table.getNumOccupiedSeats() : 0);
        data.put("handNumber", state.handNumber());
        data.put("humanSeat", human != null ? human.getSeat() : -1);
        return data;
    }

    private static Map<String, Object> rankData(SnapshotState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        PokerGame game = state.game();
        PokerPlayer human = state.human();

        int totalPlayers = game != null ? game.getNumPlayers() : 0;
        int playersRemaining = game != null ? game.getNumPlayers() - game.getNumPlayersOut() : 0;
        data.put("totalPlayers", totalPlayers);
        data.put("playersRemaining", playersRemaining);
        data.put("isGameOver", game != null && game.isGameOver());

        if (human == null) {
            data.put("rank", 0);
            data.put("place", 0);
            data.put("prize", 0);
            data.put("totalSpent", 0);
            data.put("netResult", 0);
            return data;
        }

        data.put("rank", safeRank(game, human));
        data.put("place", human.getPlace());
        data.put("prize", human.getPrize());
        data.put("totalSpent", human.getTotalSpent());
        data.put("netResult", human.getPrize() - human.getTotalSpent());
        return data;
    }

    private static Map<String, Object> upNextData(SnapshotState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        TournamentProfile profile = state.profile();
        int level = state.game() != null ? Math.max(1, state.game().getLevel()) : 0;
        int nextLevel = level + 1;
        boolean hasNextLevel = profile != null && nextLevel <= profile.getLastLevel();

        data.put("level", level);
        data.put("nextLevel", hasNextLevel ? nextLevel : 0);
        data.put("hasNextLevel", hasNextLevel);
        data.put("nextSmallBlind", hasNextLevel ? profile.getSmallBlind(nextLevel) : 0);
        data.put("nextBigBlind", hasNextLevel ? profile.getBigBlind(nextLevel) : 0);
        data.put("nextIsBreak", hasNextLevel && profile.isBreak(nextLevel));
        return data;
    }

    private static Map<String, Object> cheatData() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<String> active = new ArrayList<>();
        addCheatIfOn(active, "neverbroke", PokerConstants.OPTION_CHEAT_NEVERBROKE);
        addCheatIfOn(active, "aifaceup", PokerConstants.OPTION_CHEAT_AIFACEUP);
        addCheatIfOn(active, "showfold", PokerConstants.OPTION_CHEAT_SHOWFOLD);
        addCheatIfOn(active, "showmuck", PokerConstants.OPTION_CHEAT_SHOW_MUCKED);
        addCheatIfOn(active, "showdown", PokerConstants.OPTION_CHEAT_SHOWWINNINGHAND);
        addCheatIfOn(active, "popups", PokerConstants.OPTION_CHEAT_POPUP);
        addCheatIfOn(active, "mouseover", PokerConstants.OPTION_CHEAT_MOUSEOVER);
        addCheatIfOn(active, "pausecards", PokerConstants.OPTION_CHEAT_PAUSECARDS);
        addCheatIfOn(active, "rabbithunt", PokerConstants.OPTION_CHEAT_RABBITHUNT);
        addCheatIfOn(active, "manualbutton", PokerConstants.OPTION_CHEAT_MANUAL_BUTTON);
        data.put("activeOptions", active);
        data.put("activeCount", active.size());
        return data;
    }

    private static void addCheatIfOn(List<String> active, String logicalName, String option) {
        if (isOptionOnSafe(option)) {
            active.add(logicalName);
        }
    }

    private static boolean isOptionOnSafe(String option) {
        try {
            return PokerUtils.isOptionOn(option);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Map<String, Object> debugData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("threadDumpActionAvailable", true);
        data.put("action", "THREAD_DUMP");
        return data;
    }

    private static int safeRank(PokerGame game, PokerPlayer human) {
        if (game == null || human == null || human.isObserver()) {
            return 0;
        }
        try {
            return game.getRank(human);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static List<String> handToStrings(Hand hand) {
        if (hand == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.getCard(i);
            if (card != null && !card.isBlank()) {
                out.add(card.toString());
            }
        }
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

    private record SnapshotState(
            PokerGame game,
            ClientPokerTable table,
            ClientHoldemHand hand,
            PokerPlayer human,
            TournamentProfile profile,
            boolean humanTurn,
            int handNumber,
            String round,
            String inputModeName,
            long updatedAtMs,
            long stateSeq) {
    }

    private record StateStamp(long stateSeq, long updatedAtMs) {
    }
}
