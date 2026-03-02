/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.dto.LobbyPlayerInfo;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData;
import com.donohoedigital.games.poker.server.*;
import com.donohoedigital.gui.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pre-game waiting room for online host games.
 *
 * <p>
 * Connects to the embedded server via WebSocket for real-time lobby updates
 * (player joins/leaves/kicks, settings changes, game start). Falls back to REST
 * polling if WebSocket connection fails. REST is still used for one-shot
 * operations like startGame() and cancelGame().
 */
public class Lobby extends BasePhase {

    static Logger logger = LogManager.getLogger(Lobby.class);

    private final ObjectMapper objectMapper_ = new ObjectMapper().registerModule(new JavaTimeModule());

    private String STYLE;
    private MenuBackground menu_;
    private ButtonBox buttonbox_;
    private DDButton start_;
    private DDButton cancel_;
    private DDButton edit_;

    // game state
    private PokerGame game_;
    private boolean bHost_;
    private String gameId_;
    private boolean bStarting_;

    // REST client for one-shot operations (startGame, cancelGame)
    private RestGameClient restClient_;

    // WebSocket connection for real-time lobby updates
    private WebSocketGameClient wsClient_;

    // REST polling fallback (used if WebSocket connection fails)
    private javax.swing.Timer pollTimer_;
    private final AtomicBoolean pollInFlight_ = new AtomicBoolean(false);

    // player table
    private PlayerModel playerModel_;
    private DDTable playerTable_;

    // URL panel fields
    private DDTextField lanUrlText_;
    private DDTextField publicUrlText_;

    // chat panel (no ChatManager in lobby — display only)
    private ChatPanel chat_;

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase) {
        super.init(engine, context, gamephase);

        STYLE = gamephase_.getString("style", "default");
        game_ = (PokerGame) context_.getGame();
        bHost_ = gamephase_.getBoolean("host", false);

        // get game ID from WebSocketConfig stored by OnlineConfiguration
        if (game_ != null && game_.getWebSocketConfig() != null) {
            gameId_ = game_.getWebSocketConfig().gameId();
        }

        // setup REST client for one-shot operations (startGame, cancelGame) and polling
        // fallback
        String jwt = RestAuthClient.getInstance().getCachedJwt();
        String baseUrl = getCentralServerUrl();
        if (baseUrl != null && jwt != null) {
            restClient_ = new RestGameClient(baseUrl, jwt);
        } else {
            logger.warn("Lobby: no central server URL or JWT — lobby REST operations will fail");
        }

        // create main panel
        menu_ = new MenuBackground(gamephase);
        DDPanel menubox = menu_.getMenuBox();

        // buttons
        buttonbox_ = new ButtonBox(context_, gamephase_, this, "empty", false, false);
        menubox.add(buttonbox_, BorderLayout.SOUTH);
        if (bHost_) {
            start_ = buttonbox_.getDefaultButton();
            edit_ = buttonbox_.getButton("edit");
        }
        cancel_ = buttonbox_.getButtonStartsWith("cancel");

        // data area
        DDPanel data = new DDPanel();
        data.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        data.setBorderLayoutGap(5, 5);
        menubox.add(data, BorderLayout.CENTER);

        // title
        String titleKey = bHost_ ? "msg.title.Lobby.Host" : "msg.title.Lobby.Player";
        DDLabel title = new DDLabel(GuiManager.DEFAULT, STYLE);
        title.setText(PropertyConfig.getMessage(titleKey));
        data.add(title, BorderLayout.NORTH);

        // player list (center)
        DDPanel center = new DDPanel();
        center.setBorderLayoutGap(5, 5);
        data.add(center, BorderLayout.CENTER);

        DDLabelBorder playersBorder = new DDLabelBorder("lobby.players", STYLE);
        playersBorder.setLayout(new BorderLayout());
        center.add(playersBorder, BorderLayout.CENTER);

        playerModel_ = new PlayerModel();
        String[] cols = {PropertyConfig.getMessage("msg.lobby.col.num"),
                PropertyConfig.getMessage("msg.lobby.col.name"), PropertyConfig.getMessage("msg.lobby.col.role")};
        int[] colWidths = {40, 200, 80};

        DDScrollTable scroll = new DDScrollTable(GuiManager.DEFAULT, STYLE, "BrushedMetal", cols, colWidths);
        playerTable_ = scroll.getDDTable();
        playerTable_.setModel(playerModel_);
        playerTable_.setShowHorizontalLines(true);
        playersBorder.add(scroll, BorderLayout.CENTER);

        // URL panel (south of center)
        DDPanel urlPanel = createURLPanel();
        if (urlPanel != null) {
            center.add(urlPanel, BorderLayout.SOUTH);
        }

        // chat panel (east)
        chat_ = new ChatPanel(game_, context_, null, "ChatLobby", "BrushedMetal", false);
        data.add(chat_, BorderLayout.EAST);
    }

    @Override
    public void start() {
        context_.setMainUIComponent(this, menu_, false, null);
        connectWebSocket();
    }

    @Override
    public void finish() {
        disconnectWebSocket();
        finishPolling();
        super.finish();
    }

    // =========================================================================
    // WebSocket connection for real-time lobby updates
    // =========================================================================

    private void connectWebSocket() {
        PokerGame.WebSocketConfig config = game_ != null ? game_.getWebSocketConfig() : null;
        if (config == null || gameId_ == null) {
            startPolling();
            return;
        }

        wsClient_ = new WebSocketGameClient();
        wsClient_.setMessageHandler(this::onLobbyMessage);
        wsClient_.connect(config.host(), config.port(), gameId_, config.jwt()).whenComplete((v, ex) -> {
            if (ex != null) {
                logger.warn("WebSocket lobby connection failed, falling back to REST polling: {}", ex.getMessage());
                SwingUtilities.invokeLater(this::startPolling);
            } else {
                logger.debug("WebSocket lobby connected for game {}", gameId_);
            }
        });
    }

    private void disconnectWebSocket() {
        if (wsClient_ != null) {
            wsClient_.disconnect();
            wsClient_ = null;
        }
    }

    private void onLobbyMessage(WebSocketGameClient.InboundMessage msg) {
        try {
            switch (msg.type()) {
                case LOBBY_STATE -> {
                    ServerMessageData.LobbyStateData d = objectMapper_.treeToValue(msg.data(),
                            ServerMessageData.LobbyStateData.class);
                    SwingUtilities.invokeLater(() -> updateFromLobbyState(d));
                }
                case LOBBY_PLAYER_JOINED -> {
                    ServerMessageData.LobbyPlayerJoinedData d = objectMapper_.treeToValue(msg.data(),
                            ServerMessageData.LobbyPlayerJoinedData.class);
                    SwingUtilities.invokeLater(() -> addLobbyPlayer(d.player()));
                }
                case LOBBY_PLAYER_LEFT -> {
                    ServerMessageData.LobbyPlayerLeftData d = objectMapper_.treeToValue(msg.data(),
                            ServerMessageData.LobbyPlayerLeftData.class);
                    SwingUtilities.invokeLater(() -> removeLobbyPlayer(d.player()));
                }
                case LOBBY_PLAYER_KICKED -> {
                    ServerMessageData.LobbyPlayerKickedData d = objectMapper_.treeToValue(msg.data(),
                            ServerMessageData.LobbyPlayerKickedData.class);
                    SwingUtilities.invokeLater(() -> removeLobbyPlayer(d.player()));
                }
                case LOBBY_SETTINGS_CHANGED -> {
                    ServerMessageData.LobbySettingsChangedData d = objectMapper_.treeToValue(msg.data(),
                            ServerMessageData.LobbySettingsChangedData.class);
                    SwingUtilities.invokeLater(() -> updateFromLobbySettings(d));
                }
                case LOBBY_GAME_STARTING -> {
                    ServerMessageData.LobbyGameStartingData d = objectMapper_.treeToValue(msg.data(),
                            ServerMessageData.LobbyGameStartingData.class);
                    SwingUtilities.invokeLater(() -> onGameStarting(d.startingInSeconds()));
                }
                case GAME_CANCELLED -> {
                    ServerMessageData.GameCancelledData d = objectMapper_.treeToValue(msg.data(),
                            ServerMessageData.GameCancelledData.class);
                    SwingUtilities.invokeLater(() -> onGameCancelled(d.reason()));
                }
                case CONNECTED -> {
                    // Initial connection — server sends CONNECTED then LOBBY_STATE
                    logger.debug("[LOBBY] WebSocket connected");
                }
                default -> logger.debug("[LOBBY] Ignoring message type: {}", msg.type());
            }
        } catch (Exception ex) {
            logger.warn("Failed to process lobby message {}: {}", msg.type(), ex.getMessage());
        }
    }

    private void updateFromLobbyState(ServerMessageData.LobbyStateData d) {
        if (d.players() != null) {
            List<LobbyPlayerInfo> players = d.players().stream()
                    .map(lp -> new LobbyPlayerInfo(lp.name(), lp.isOwner() ? "Host" : (lp.isAI() ? "AI" : "Player")))
                    .toList();
            playerModel_.update(players);
        }
    }

    private void updateFromLobbySettings(ServerMessageData.LobbySettingsChangedData d) {
        if (d.updatedSettings() == null) {
            return;
        }
        GameSummary s = d.updatedSettings();
        logger.debug("[LOBBY] settings changed: name={} maxPlayers={} blinds={}", s.name(), s.maxPlayers(), s.blinds());
        // Update the window title if the game name changed.
        String titleKey = bHost_ ? "msg.title.Lobby.Host" : "msg.title.Lobby.Player";
        String title = PropertyConfig.getMessage(titleKey);
        if (s.name() != null && !s.name().isBlank()) {
            title = s.name() + " - " + title;
        }
        if (menu_ != null) {
            menu_.repaint();
        }
    }

    private void addLobbyPlayer(ServerMessageData.LobbyPlayerData player) {
        if (player == null) {
            return;
        }
        String role = player.isOwner() ? "Host" : (player.isAI() ? "AI" : "Player");
        playerModel_.addPlayer(new LobbyPlayerInfo(player.name(), role));
    }

    private void removeLobbyPlayer(ServerMessageData.LobbyPlayerData player) {
        if (player == null) {
            return;
        }
        playerModel_.removePlayer(player.name());
    }

    private void onGameStarting(int startingInSeconds) {
        logger.info("[LOBBY] Game starting in {} seconds", startingInSeconds);
        disconnectWebSocket();
        finishPolling();
        context_.processPhase("InitializeOnlineGame", null);
    }

    private void onGameCancelled(String reason) {
        logger.info("[LOBBY] Game cancelled: {}", reason);
        disconnectWebSocket();
        finishPolling();
        EngineUtils.displayInformationDialog(context_,
                PropertyConfig.getMessage("msg.lobby.cancelled", reason != null ? reason : "Game cancelled"));
        context_.restart();
    }

    // =========================================================================
    // REST polling fallback
    // =========================================================================

    private void startPolling() {
        if (restClient_ == null || gameId_ == null) {
            return;
        }
        pollTimer_ = new javax.swing.Timer(2000, e -> pollServer());
        pollTimer_.start();
    }

    private void finishPolling() {
        if (pollTimer_ != null) {
            pollTimer_.stop();
            pollTimer_ = null;
        }
        pollInFlight_.set(false);
    }

    private void pollServer() {
        if (!pollInFlight_.compareAndSet(false, true)) {
            return;
        }

        // run off EDT to avoid blocking UI
        Thread t = new Thread(() -> {
            try {
                GameSummary summary = restClient_.getGameSummary(gameId_);
                List<LobbyPlayerInfo> players = summary.players() != null ? summary.players() : Collections.emptyList();
                SwingUtilities.invokeLater(() -> {
                    if (pollTimer_ == null) {
                        return;
                    }

                    playerModel_.update(players);

                    // if game has started and we're the host waiting to transition
                    if ("IN_PROGRESS".equals(summary.status()) && bStarting_) {
                        finishPolling();
                        context_.processPhase("InitializeOnlineGame", null);
                    }
                });
            } catch (Exception ex) {
                logger.warn("Poll failed: {}", ex.getMessage());
            } finally {
                pollInFlight_.set(false);
            }
        }, "LobbyPoll");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // Button handling
    // =========================================================================

    @Override
    public boolean processButton(GameButton button) {
        if (start_ != null && button.getName().equals(start_.getName())) {
            String sMsg = PropertyConfig.getMessage("msg.lobby.start.confirm");
            if (!EngineUtils.displayConfirmationDialog(context_, sMsg, "lobby.start")) {
                return false;
            }
            start_.setEnabled(false);
            cancel_.setEnabled(false);
            if (edit_ != null) {
                edit_.setEnabled(false);
            }
            bStarting_ = true;
            // call startGame on server in background
            Thread t = new Thread(() -> {
                try {
                    restClient_.startGame(gameId_);
                } catch (Exception ex) {
                    logger.error("Failed to start game: {}", ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        bStarting_ = false;
                        start_.setEnabled(true);
                        cancel_.setEnabled(true);
                        if (edit_ != null)
                            edit_.setEnabled(true);
                        EngineUtils.displayInformationDialog(context_,
                                PropertyConfig.getMessage("msg.lobby.startfail", ex.getMessage()));
                    });
                }
            }, "LobbyStart");
            t.setDaemon(true);
            t.start();
            return false;
        }

        if (cancel_ != null && button.getName().startsWith("cancel")) {
            String msgKey = bHost_ ? "msg.confirm.lobby.host" : "msg.confirm.lobby.client";
            String sMsg = PropertyConfig.getMessage(msgKey);
            if (!EngineUtils.displayConfirmationDialog(context_, sMsg)) {
                return false;
            }
            disconnectWebSocket();
            finishPolling();

            // perform blocking cleanup (deregister, cancel) off-EDT then navigate
            boolean host = bHost_;
            EmbeddedGameServer embeddedServer = host ? ((PokerMain) engine_).getEmbeddedServer() : null;
            String gameId = gameId_;
            Thread t = new Thread(() -> {
                CommunityGameRegistration reg = OnlineConfiguration.getActiveRegistration();
                if (reg != null) {
                    reg.deregister();
                    OnlineConfiguration.clearActiveRegistration();
                }
                if (host && embeddedServer != null && gameId != null) {
                    try {
                        new RestGameClient("http://localhost:" + embeddedServer.getPort(),
                                embeddedServer.getLocalUserJwt()).cancelGame(gameId);
                    } catch (Exception ex) {
                        logger.warn("Failed to cancel game on server: {}", ex.getMessage());
                    }
                }
                SwingUtilities.invokeLater(context_::restart);
            }, "LobbyCancel");
            t.setDaemon(true);
            t.start();
            return false;
        }

        return true;
    }

    // =========================================================================
    // URL panel
    // =========================================================================

    private DDPanel createURLPanel() {
        if (game_ == null || game_.getWebSocketConfig() == null) {
            return null;
        }
        String wsUrl = "ws://" + game_.getWebSocketConfig().host() + ":" + game_.getWebSocketConfig().port()
                + "/ws/games/" + gameId_;

        DDLabelBorder urlBorder = new DDLabelBorder("lobby.url", STYLE);
        urlBorder.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 5, 3));

        OnlineConfiguration.Widgets lanW = OnlineConfiguration.addIPText(urlBorder, "lobby.lanurl", STYLE, true);
        lanUrlText_ = lanW.text;
        lanUrlText_.setText(wsUrl);

        OnlineConfiguration.Widgets pubW = OnlineConfiguration.addIPText(urlBorder, "lobby.publicurl", STYLE, true);
        publicUrlText_ = pubW.text;

        // wrap in a DDPanel so it can be added to a BorderLayout slot
        DDPanel wrapper = new DDPanel();
        wrapper.setLayout(new BorderLayout());
        wrapper.add(urlBorder, BorderLayout.CENTER);
        return wrapper;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Returns the configured central server HTTP base URL, or null if not set. */
    private String getCentralServerUrl() {
        try {
            String node = Prefs.NODE_OPTIONS + engine_.getPrefsNodeName();
            String server = Prefs.getUserPrefs(node).get(EngineConstants.OPTION_ONLINE_SERVER, "");
            return OnlineServerUrl.normalizeBaseUrl(server);
        } catch (Exception e) {
            logger.warn("Could not read central server URL from preferences", e);
            return null;
        }
    }

    // =========================================================================
    // PlayerModel — updated by WebSocket events or REST poll fallback
    // =========================================================================

    private class PlayerModel extends AbstractTableModel {

        private List<LobbyPlayerInfo> players_ = Collections.emptyList();

        void update(List<LobbyPlayerInfo> newPlayers) {
            players_ = new ArrayList<>(newPlayers);
            fireTableDataChanged();
        }

        void addPlayer(LobbyPlayerInfo player) {
            players_ = new ArrayList<>(players_);
            players_.add(player);
            fireTableDataChanged();
        }

        void removePlayer(String name) {
            players_ = new ArrayList<>(players_);
            players_.removeIf(p -> p.name().equals(name));
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return players_.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            LobbyPlayerInfo info = players_.get(rowIndex);
            switch (colIndex) {
                case 0 :
                    return rowIndex + 1;
                case 1 :
                    return info.name();
                case 2 :
                    return info.role();
                default :
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }
}
