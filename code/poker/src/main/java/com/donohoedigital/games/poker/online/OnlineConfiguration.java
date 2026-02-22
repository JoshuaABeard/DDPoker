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

import com.donohoedigital.base.*;
import com.donohoedigital.comms.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.gameserver.*;
import com.donohoedigital.games.poker.gameserver.dto.*;
import com.donohoedigital.games.poker.model.*;
import com.donohoedigital.games.poker.server.*;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;

/**
 * Setup Online Tournament (2 of 2) — network configuration screen.
 *
 * <p>
 * Displays LAN and internet IP/URL information for the host, lets the host
 * optionally post to the public game list, and opens the Lobby when ready.
 */
public class OnlineConfiguration extends BasePhase implements ActionListener {

    static Logger logger = LogManager.getLogger(OnlineConfiguration.class);

    private String STYLE;
    private MenuBackground menu_;
    private ButtonBox buttonbox_;
    private DDButton okaybegin_;

    // IP / URL fields
    private DDTextField lanIpText_;
    private DDTextField lanUrlText_;
    private DDTextField publicIpText_;
    private DDTextField publicUrlText_;

    // Options
    private DDCheckBox cbxPublicList_;
    private DDCheckBox cbxObserver_;

    // "Get Public IP" button (stored to disable during fetch)
    private DDButton getIpBtn_;

    // Profile label
    private DDLabel profileLabel_;

    // Hosting state
    private String lanIp_;
    private int port_;
    private RestGameClient restClient_;

    /** Active WAN registration, if the host posted to the public game list. */
    private static CommunityGameRegistration activeRegistration_;

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase) {
        super.init(engine, context, gamephase);

        STYLE = gamephase_.getString("style", "default");

        // detect LAN IP
        lanIp_ = CommunityHostingConfig.detectLanIp();

        // get port from embedded server (already started for practice games)
        EmbeddedGameServer embeddedServer = ((PokerMain) engine_).getEmbeddedServer();
        port_ = (embeddedServer != null && embeddedServer.isRunning())
                ? embeddedServer.getPort()
                : CommunityHostingConfig.DEFAULT_COMMUNITY_PORT;

        // build the RestGameClient for the embedded server
        if (embeddedServer != null && embeddedServer.isRunning()) {
            restClient_ = new RestGameClient("http://localhost:" + port_, embeddedServer.getLocalUserJwt());
        }

        // create main panel
        menu_ = new MenuBackground(gamephase);
        DDPanel menubox = menu_.getMenuBox();

        // buttons
        buttonbox_ = new ButtonBox(context_, gamephase_, this, "empty", false, false);
        menubox.add(buttonbox_, BorderLayout.SOUTH);
        okaybegin_ = buttonbox_.getDefaultButton();

        // data panel — uses VerticalFlowLayout so sections stack vertically
        DDPanel data = new DDPanel();
        data.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        data.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 8));
        menubox.add(data, BorderLayout.CENTER);

        // ---- LAN Connection section ----
        DDLabelBorder lanBorder = new DDLabelBorder("hostonline.lan", STYLE);
        lanBorder.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 5, 5));
        data.add(lanBorder);

        Widgets lanIpWidgets = addIPText(lanBorder, "hostonline.lanip", STYLE, false);
        lanIpText_ = lanIpWidgets.text;
        lanIpText_.setText(lanIp_);

        Widgets lanUrlWidgets = addIPText(lanBorder, "hostonline.lanurl", STYLE, true);
        lanUrlText_ = lanUrlWidgets.text;
        lanUrlText_.setText(""); // filled in after game creation

        // ---- Internet Connection section ----
        DDLabelBorder internetBorder = new DDLabelBorder("hostonline.internet", STYLE);
        internetBorder.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 5, 5));
        data.add(internetBorder);

        Widgets publicIpWidgets = addIPText(internetBorder, "hostonline.publicip", STYLE, false);
        publicIpText_ = publicIpWidgets.text;
        publicIpText_.setEditable(true);

        // "Get Public IP" button
        getIpBtn_ = new DDButton("hostonline.getip", STYLE);
        getIpBtn_.addActionListener(this);
        internetBorder.add(GuiUtils.WEST(getIpBtn_));

        Widgets publicUrlWidgets = addIPText(internetBorder, "hostonline.publicurl", STYLE, true);
        publicUrlText_ = publicUrlWidgets.text;

        // ---- DD Poker Public Game List section ----
        DDLabelBorder publicBorder = new DDLabelBorder("hostonline.public", STYLE);
        publicBorder.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 5, 5));
        data.add(publicBorder);

        PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
        profileLabel_ = new DDLabel(GuiManager.DEFAULT, STYLE);
        if (profile != null) {
            profileLabel_
                    .setText(PropertyConfig.getMessage("msg.hostonline.profile", Utils.encodeHTML(profile.getName())));
        }
        publicBorder.add(profileLabel_);

        cbxPublicList_ = new DDCheckBox("hostonline.publiclist", STYLE);
        cbxPublicList_.setSelected(true);
        publicBorder.add(cbxPublicList_);

        // ---- Options section ----
        DDLabelBorder optionsBorder = new DDLabelBorder("hostonline.options", STYLE);
        optionsBorder.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 5, 5));
        data.add(optionsBorder);

        cbxObserver_ = new DDCheckBox("hostonline.observer", STYLE);
        optionsBorder.add(cbxObserver_);
    }

    @Override
    public void start() {
        context_.setMainUIComponent(this, menu_, false, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // "Get Public IP" button — fetch off-EDT to avoid freezing UI
        getIpBtn_.setEnabled(false);
        Thread t = new Thread(() -> {
            String ip = new PublicIPDetector().fetchPublicIP();
            SwingUtilities.invokeLater(() -> {
                getIpBtn_.setEnabled(true);
                if (ip != null && !ip.isEmpty()) {
                    publicIpText_.setText(ip);
                    publicUrlText_.setText(""); // will be set after game creation
                }
            });
        }, "GetPublicIP");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public boolean processButton(GameButton button) {
        if (okaybegin_ != null && button.getName().equals(okaybegin_.getName())) {
            return doOpenLobby();
        }
        return true;
    }

    private boolean doOpenLobby() {
        // start embedded server in external/community mode if not yet running
        EmbeddedGameServer embeddedServer = ((PokerMain) engine_).getEmbeddedServer();
        if (embeddedServer == null) {
            EngineUtils.displayInformationDialog(context_, PropertyConfig.getMessage("msg.hostonline.noserver"));
            return false;
        }
        if (!embeddedServer.isRunning()) {
            try {
                embeddedServer.start(CommunityHostingConfig.DEFAULT_COMMUNITY_PORT);
                port_ = embeddedServer.getPort();
                restClient_ = new RestGameClient("http://localhost:" + port_, embeddedServer.getLocalUserJwt());
            } catch (EmbeddedGameServer.EmbeddedServerStartupException ex) {
                logger.error("Failed to start embedded server for hosting", ex);
                EngineUtils.displayInformationDialog(context_,
                        PropertyConfig.getMessage("msg.hostonline.startfail", ex.getMessage()));
                return false;
            }
        }

        // get the TournamentProfile selected in the previous step
        PokerGame game = (PokerGame) context_.getGame();
        if (game == null) {
            logger.error("No game set in context — cannot open lobby");
            return false;
        }
        TournamentProfile profile = game.getProfile();

        // convert TournamentProfile to GameConfig and create game on server
        com.donohoedigital.games.poker.gameserver.controller.TournamentProfileConverter converter = new com.donohoedigital.games.poker.gameserver.controller.TournamentProfileConverter();
        GameConfig gameConfig = converter.convert(profile);

        GameSummary summary;
        try {
            summary = restClient_.createGame(gameConfig);
        } catch (RestGameClient.RestGameClientException ex) {
            logger.error("Failed to create game on server", ex);
            EngineUtils.displayInformationDialog(context_,
                    PropertyConfig.getMessage("msg.hostonline.createfail", ex.getMessage()));
            return false;
        }

        String gameId = summary.gameId();
        String wsUrl = summary.wsUrl();

        // store the WebSocket connection info on the PokerGame
        game.setWebSocketConfig(gameId, embeddedServer.getLocalUserJwt(), port_);

        // update URL display fields
        if (wsUrl != null) {
            lanUrlText_.setText(wsUrl);
        }
        String publicIp = publicIpText_.getText().trim();
        if (!publicIp.isEmpty()) {
            publicUrlText_.setText(CommunityHostingConfig.buildGameUrl(publicIp, port_, gameId));
        }

        // post on public game list if requested
        if (cbxPublicList_.isSelected()) {
            String publicWsUrl = publicIp.isEmpty()
                    ? wsUrl
                    : CommunityHostingConfig.buildGameUrl(publicIp, port_, gameId);
            PlayerProfile playerProfile = PlayerProfileOptions.getDefaultProfile();
            String gameName = (playerProfile != null) ? playerProfile.getName() + "'s Game" : profile.getName();
            String wanNode = Prefs.NODE_OPTIONS + context_.getGameEngine().getPrefsNodeName();
            String wanBaseUrl = Prefs.getUserPrefs(wanNode).get(EngineConstants.OPTION_ONLINE_SERVER, "");
            if (!wanBaseUrl.isEmpty()) {
                RestGameClient wanClient = new RestGameClient(wanBaseUrl, embeddedServer.getLocalUserJwt());
                CommunityGameRegistration registration = new CommunityGameRegistration(wanClient);
                try {
                    registration.register(gameName, publicWsUrl, null);
                    activeRegistration_ = registration;
                } catch (Exception ex) {
                    logger.warn("Failed to post game to public list: {}", ex.getMessage());
                    // non-fatal — continue to lobby
                }
            }
        }

        // host as observer if requested
        if (cbxObserver_.isSelected()) {
            PokerPlayer host = game.getHost();
            if (host != null) {
                game.removePlayer(host);
                game.getTable(0).addObserver(host);
            }
        }

        // navigate to Lobby (host=true)
        DMTypedHashMap params = new DMTypedHashMap();
        params.setBoolean("host", Boolean.TRUE);
        context_.processPhase("Lobby", params);
        return false;
    }

    // =========================================================================
    // Static accessors
    // =========================================================================

    /**
     * Returns the active WAN registration (if the host posted to the public game
     * list), or {@code null} if not registered.
     */
    public static CommunityGameRegistration getActiveRegistration() {
        return activeRegistration_;
    }

    /** Clears the active WAN registration (call after deregistering). */
    public static void clearActiveRegistration() {
        activeRegistration_ = null;
    }

    // =========================================================================
    // Static helpers (used by Lobby.java)
    // =========================================================================

    /**
     * Adds an IP/URL text row to the given panel.
     *
     * @param parent
     *            the panel to add to
     * @param name
     *            base name for the label and text field
     * @param style
     *            UI style name
     * @param withCopy
     *            whether to add a Copy button
     * @return the created {@link Widgets} bundle
     */
    public static Widgets addIPText(JComponent parent, String name, String style, boolean withCopy) {
        DDPanel row = new DDPanel();
        row.setLayout(new BorderLayout(5, 0));

        DDLabel label = new DDLabel(name, style);
        row.add(label, BorderLayout.WEST);

        DDTextField text = new DDTextField(GuiManager.DEFAULT, style);
        text.setEditable(false);
        row.add(text, BorderLayout.CENTER);

        DDButton copyBtn = null;
        if (withCopy) {
            copyBtn = new DDButton(name + ".copy", style);
            final DDTextField textRef = text;
            copyBtn.addActionListener(e -> {
                String val = textRef.getText();
                if (val != null && !val.isEmpty()) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(val), null);
                }
            });
            row.add(copyBtn, BorderLayout.EAST);
        }

        parent.add(row);
        return new Widgets(label, text, copyBtn);
    }

    /**
     * A bundle of widgets for an IP/URL display row.
     */
    public static class Widgets {
        public final DDLabel label;
        public final DDTextField text;
        public final DDButton copyButton;

        public Widgets(DDLabel label, DDTextField text, DDButton copyButton) {
            this.label = label;
            this.text = text;
            this.copyButton = copyButton;
        }
    }
}
