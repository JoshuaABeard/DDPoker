/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * Copyright (c) 2026 Joshua Beard and contributors
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
package com.donohoedigital.games.poker;
import com.donohoedigital.games.poker.display.ClientHand;
import com.donohoedigital.games.poker.display.ClientCard;

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.protocol.dto.SimulationResult;
import com.donohoedigital.games.poker.server.GameServerRestClient;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PokerSimulatorPanel extends DDTabPanel implements DDProgressFeedback {
    static Logger logger = LogManager.getLogger(PokerSimulatorPanel.class);

    // ClientHand type display order: best to worst, matching server HAND_TYPE_NAMES
    private static final String[] HAND_TYPE_ORDER = {"ROYAL_FLUSH", "STRAIGHT_FLUSH", "FOUR_OF_A_KIND", "FULL_HOUSE",
            "FLUSH", "STRAIGHT", "TRIPS", "TWO_PAIR", "ONE_PAIR", "HIGH_CARD"};

    // Human-readable names for each hand type key
    private static final Map<String, String> HAND_TYPE_LABELS = Map.of("ROYAL_FLUSH", "Royal Flush", "STRAIGHT_FLUSH",
            "Straight Flush", "FOUR_OF_A_KIND", "Four of a Kind", "FULL_HOUSE", "Full House", "FLUSH", "Flush",
            "STRAIGHT", "Straight", "TRIPS", "Three of a Kind", "TWO_PAIR", "Two Pair", "ONE_PAIR", "One Pair",
            "HIGH_CARD", "High Card");

    private ClientHand pocket_;
    private ClientHand community_;

    private DDScrollPane scroll_;
    private DDHtmlArea htmlArea_;
    private DDHtmlArea header_;
    private DDProgressBar progress_;
    private boolean bStopRequested_ = false;
    private GlassButton stop_, run_;
    private SimulatorDialog sim_;

    public PokerSimulatorPanel(SimulatorDialog sim) {
        super();
        sim_ = sim;
        setPreferredSize(new Dimension(460, 350));
    }

    protected void createUI() {
        setBorderLayoutGap(0, 0);

        // top
        DDPanel top = new DDPanel();
        top.setBorderLayoutGap(5, 0);
        add(top, BorderLayout.NORTH);

        // info
        header_ = new DDHtmlArea(GuiManager.DEFAULT, "PokerStatsHeader");
        header_.setBorder(BorderFactory.createEmptyBorder());
        top.add(header_, BorderLayout.NORTH);

        DDPanel pb = new DDPanel();
        pb.setBorderLayoutGap(0, 5);
        top.add(pb, BorderLayout.CENTER);

        // progress
        progress_ = new DDProgressBar(GuiManager.DEFAULT, "PokerStats");
        progress_.setProgressFeedback(this);
        pb.add(progress_, BorderLayout.CENTER);

        run_ = new GlassButton("run", "Glass");
        run_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateStats();
            }
        });

        stop_ = new GlassButton("stop", "Glass");
        stop_.setEnabled(false);
        stop_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStopRequested();
            }
        });

        pb.add(GuiUtils.NORTH(run_), BorderLayout.WEST);
        pb.add(progress_, BorderLayout.CENTER);
        pb.add(GuiUtils.NORTH(stop_), BorderLayout.EAST);
        Dimension size = progress_.getProgressBar().getPreferredSize();
        size.height = run_.getPreferredSize().height;
        progress_.getProgressBar().setPreferredSize(size);

        // html results
        htmlArea_ = new DDHtmlArea("PokerStats", "PokerStats");
        htmlArea_.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        scroll_ = new DDScrollPane(htmlArea_, "PokerStandardDialog", null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll_.setOpaque(false);

        add(scroll_, BorderLayout.CENTER);

        // update stats
        if (pocket_ != null) {
            updateHeader();
            updateStats();
        }
    }

    /**
     * header
     */
    private void updateHeader() {
        ClientHand pocket = ClientHand.fromCards(pocket_.getCards());
        while (pocket.size() < 2)
            pocket.addCard(ClientCard.BLANK);

        ClientHand community = ClientHand.fromCards(community_.getCards());
        int nNumComm = community.size();
        String sKey;
        if (nNumComm < 3) {
            sKey = "msg.sim.header.deal";
        } else {
            sKey = nNumComm == 5 ? "msg.sim.header.show" : "msg.sim.header.post";
        }
        while (community.size() < 5)
            community.addCard(ClientCard.BLANK);
        nNumComm = community.size();
        header_.setText(PropertyConfig.getMessage(sKey, pocket.toHTML(), community.toHTML(), (2 + nNumComm) * 23 + 5));
    }

    /**
     * run update thread
     */
    public void updateStats() {
        if (checkRequiredCards()) {
            htmlArea_.setText("");
            sim_.bSimRunning_ = true;
            bStopRequested_ = false;
            stop_.setEnabled(true);
            run_.setEnabled(false);
            new UpdateThread().start();
        } else {
            run_.setEnabled(false);
        }
    }

    /**
     * check prereqs
     */
    private boolean checkRequiredCards() {
        String sText = null;
        if (pocket_.size() != 2) {
            sText = PropertyConfig.getMessage("msg.sim.needboth");
        }

        if (sText != null) {
            htmlArea_.setText(sText);
            return false;
        }

        return true;
    }

    /**
     * update thread with given pocket/community
     */
    public void updateStats(ClientHand pocket, ClientHand community) {
        // same hand, skip update
        if (pocket_ != null && pocket.fingerprint() == pocket_.fingerprint() && community_ != null
                && community.fingerprint() == community_.fingerprint()) {
            return;
        }

        // store new values
        pocket_ = pocket;
        community_ = community;

        // UI not created, so stop here
        if (header_ == null)
            return;

        // update
        updateHeader();
        updateStats();
    }

    /**
     * no stop on simulator panel
     */
    public boolean isStopRequested() {
        return bStopRequested_;
    }

    /**
     * set stop requested
     */
    void setStopRequested() {
        bStopRequested_ = true;
    }

    /**
     * progres bar handles this
     */
    public void setMessage(String sMessage) {
    }

    /**
     * progress bar handles this
     */
    public void setPercentDone(int n) {
    }

    /**
     * progress bar passes this onto us when done
     */
    public void setFinalResult(Object oResult) {
        sim_.bSimRunning_ = false;
        stop_.setEnabled(false);
        run_.setEnabled(true);

        setIntermediateResult(oResult);
    }

    public void setIntermediateResult(Object oResult) {
        if (oResult == null)
            return;
        final String html = (String) oResult;
        SwingUtilities.invokeLater(() -> {
            htmlArea_.setText(html);
            htmlArea_.setCaretPosition(0); // scroll to top
        });
    }

    private class UpdateThread extends Thread {
        public UpdateThread() {
            super("UpdateThread");
        }

        @Override
        public void run() {
            List<String> holeCards = new ArrayList<>();
            for (int i = 0; i < pocket_.size(); i++) {
                ClientCard c = pocket_.getCard(i);
                if (!c.isBlank())
                    holeCards.add(c.toStringSingle());
            }

            List<String> communityCards = new ArrayList<>();
            if (community_ != null) {
                for (int i = 0; i < community_.size(); i++) {
                    ClientCard c = community_.getCard(i);
                    if (!c.isBlank())
                        communityCards.add(c.toStringSingle());
                }
            }

            SwingUtilities.invokeLater(() -> progress_.getProgressBar().setIndeterminate(true));
            try {
                PokerMain pokerMain = (PokerMain) GameEngine.getGameEngine();
                GameServerRestClient restClient = new GameServerRestClient(pokerMain.getEmbeddedServer().getPort());

                SimulationResult result = restClient.simulate(holeCards, communityCards, 1, 100000, null, null);
                String html = buildHtml(result);
                progress_.setFinalResult(html);
            } catch (GameServerRestClient.GameServerClientException e) {
                logger.warn("Simulation REST call failed: {}", e.getMessage());
                progress_.setFinalResult(null);
            } finally {
                SwingUtilities.invokeLater(() -> progress_.getProgressBar().setIndeterminate(false));
            }
        }

        private String buildHtml(SimulationResult result) {
            StringBuilder sb = new StringBuilder();

            // Win/tie/loss summary
            sb.append(PropertyConfig.getMessage("msg.simresult.header"));
            sb.append(PropertyConfig.getMessage("msg.simresult.row", PropertyConfig.getMessage("msg.sim.random"),
                    pocket_.toStringRankSuit(), PokerClientConstants.formatPercent(result.win()),
                    PokerClientConstants.formatPercent(result.loss()), PokerClientConstants.formatPercent(result.tie()),
                    result.iterations()));
            sb.append(PropertyConfig.getMessage("msg.simresult.footer"));

            // ClientHand type distribution
            Map<String, Double> breakdown = result.playerHandTypeBreakdown();
            if (breakdown != null && !breakdown.isEmpty()) {
                sb.append(PropertyConfig.getMessage("msg.simresult.handtype.header"));
                for (String key : HAND_TYPE_ORDER) {
                    Double pct = breakdown.get(key);
                    if (pct != null && pct > 0) {
                        String label = HAND_TYPE_LABELS.getOrDefault(key, key);
                        sb.append(PropertyConfig.getMessage("msg.simresult.handtype.row", label,
                                PokerClientConstants.formatPercent(pct)));
                    }
                }
                sb.append(PropertyConfig.getMessage("msg.simresult.handtype.footer"));
            }

            return sb.toString();
        }
    }
}
