/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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

import com.donohoedigital.games.poker.online.ClientPlayer;
import com.donohoedigital.games.poker.display.ClientHandScoreConstants;
import com.donohoedigital.games.poker.protocol.dto.HandActionDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandEvaluationData;
import com.donohoedigital.games.poker.protocol.dto.HandPlayerDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandSummaryData;
import com.donohoedigital.games.poker.server.GameServerRestClient;
import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.display.ClientBettingRound;
import com.donohoedigital.games.poker.protocol.constants.ProtocolConstants;
import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import com.donohoedigital.games.poker.online.ClientHoldemHand;

public class HandHistoryPanel extends DDPanel {

    private String gameId_;
    private String jwt_;
    private int port_;
    private ClientHoldemHand currentHand_;
    private int pageSize_;

    private int handCount_;
    private int handFirst_;

    private long selectedHandId_;
    private ClientHoldemHand hhand_;

    private List<Object> hands_;

    private GameContext context_;
    private DDTabbedPane tabs_;
    private DDHtmlArea detailsHtmlArea_;
    private DDHtmlArea summaryHtmlArea_;
    private DDCheckBox showAllCheckbox_;
    private DDCheckBox showReasonCheckbox_;
    private ListPanel handsList_;
    private JScrollPane summaryScroll_;
    private DDLabel titleLabel_;
    private DDLabel pagingLabel_;
    private DDButton pageDownButton_;
    private DDButton pageUpButton_;
    private DDButton exportButton_;
    private ImageIcon icon_ = ImageConfig.getImageIcon("ddlogo20");

    public HandHistoryPanel(GameContext context, String sStyle, String gameId, String jwt, int port,
            ClientHoldemHand currentHand, int pageSize) {
        context_ = context;
        pageSize_ = pageSize;
        gameId_ = gameId;
        jwt_ = jwt;
        port_ = port;
        currentHand_ = currentHand;

        GameServerRestClient restClient = new GameServerRestClient(port_);
        handCount_ = (int) restClient.getHandCount(gameId_, jwt_);

        if (currentHand_ != null) {
            ++handCount_;
        }

        handFirst_ = Math.max(handCount_ - pageSize_, 0);

        // widgets
        detailsHtmlArea_ = new DDHtmlArea(GuiManager.DEFAULT, sStyle);
        detailsHtmlArea_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        summaryHtmlArea_ = new DDHtmlArea(GuiManager.DEFAULT, sStyle);
        summaryHtmlArea_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        handsList_ = new ListPanel(HandListItemPanel.class, sStyle);
        handsList_.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                setHandIndex(handsList_.getSelectedIndex());
            }
        });
        handsList_.setOpaque(false);
        handsList_.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        Insets insets = handsList_.getInsets();
        handsList_.setPreferredSize(new Dimension(176, 30 * pageSize_ + insets.top + insets.bottom));

        DDPanel westPanel = new DDPanel(GuiManager.DEFAULT, sStyle);

        DDPanel handsHeader = new DDPanel(GuiManager.DEFAULT, sStyle);
        DDLabel handLabel = new DDLabel("histpocket", sStyle);
        DDLabel boardLabel = new DDLabel("histcommunity", sStyle);
        handLabel.setHorizontalAlignment(DDLabel.CENTER);
        handLabel.setPreferredWidth(55);
        boardLabel.setHorizontalAlignment(DDLabel.CENTER);
        boardLabel.setPreferredWidth(100);
        handsHeader.add(handLabel, BorderLayout.WEST);
        handsHeader.add(GuiUtils.WEST(boardLabel), BorderLayout.CENTER);
        westPanel.add(handsHeader, BorderLayout.NORTH);

        DDPanel pagingPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        pagingPanel.setBorderLayoutGap(5, 0);
        DDPanel pagingButtonPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        pagingLabel_ = new DDLabel(GuiManager.DEFAULT, sStyle);
        pagingLabel_.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        pageDownButton_ = new GlassButton("pagedown", "Glass");
        pageDownButton_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handFirst_ -= pageSize_;
                setHands();
            }
        });
        pageUpButton_ = new GlassButton("pageup", "Glass");
        pageUpButton_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handFirst_ += pageSize_;
                setHands();
            }
        });

        exportButton_ = new GlassButton("export", "Glass");
        exportButton_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                export();
            }
        });

        pagingButtonPanel.setLayout(new GridLayout(1, 3, 8, 0));
        pagingButtonPanel.add(pageUpButton_);
        pagingButtonPanel.add(exportButton_);
        pagingButtonPanel.add(pageDownButton_);

        pagingPanel.add(pagingLabel_, BorderLayout.NORTH);
        pagingPanel.add(GuiUtils.CENTER(pagingButtonPanel), BorderLayout.CENTER);

        DDPanel hsPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        hsPanel.add(handsList_, BorderLayout.CENTER);
        hsPanel.setBorder(BorderFactory.createEtchedBorder());
        westPanel.add(hsPanel, BorderLayout.CENTER);
        westPanel.add(pagingPanel, BorderLayout.SOUTH);

        // description
        // scroll (use NORTH for correct sizing)
        JScrollPane detailsScroll = new DDScrollPane(detailsHtmlArea_, sStyle, null,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        detailsScroll.setOpaque(false);
        detailsScroll.setPreferredSize(new Dimension(450, 400));
        detailsScroll.setViewportBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        detailsScroll.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        summaryScroll_ = new DDScrollPane(summaryHtmlArea_, sStyle, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        summaryScroll_.setOpaque(false);
        summaryScroll_.setPreferredSize(new Dimension(450, 400));
        summaryScroll_.setViewportBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        summaryScroll_.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        titleLabel_ = new DDLabel();

        DDPanel optionPanel = new DDPanel(GuiManager.DEFAULT, sStyle);
        optionPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        showAllCheckbox_ = new DDCheckBox("handhistaifaceup", sStyle);
        showAllCheckbox_.setEnabled(false);

        showReasonCheckbox_ = new DDCheckBox("handhistaireason", sStyle);
        showReasonCheckbox_.setEnabled(false);

        DDPanel showAllPanel = new DDPanel(GuiManager.DEFAULT, sStyle);

        showAllPanel.setLayout(new GridLayout(1, 2));

        showAllPanel.add(showAllCheckbox_);
        showAllPanel.add(showReasonCheckbox_);

        optionPanel.add(showAllPanel, BorderLayout.CENTER);

        showAllCheckbox_.setSelected(PokerUtils.isCheatOn(context_, PokerClientConstants.OPTION_CHEAT_AIFACEUP));

        showAllCheckbox_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showReasonCheckbox_.setEnabled(showAllCheckbox_.isSelected());
                setHistoryText();
            }
        });
        showReasonCheckbox_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setHistoryText();
            }
        });

        tabs_ = new DDTabbedPane(sStyle, null, DDTabbedPane.TOP);

        tabs_.addTab(PropertyConfig.getMessage("msg.histdetails"), icon_, detailsScroll);

        DDPanel eastPanel = new DDPanel(GuiManager.DEFAULT, sStyle);

        eastPanel.add(titleLabel_, BorderLayout.NORTH);
        eastPanel.add(tabs_, BorderLayout.CENTER);
        eastPanel.add(optionPanel, BorderLayout.SOUTH);
        eastPanel.setBorderLayoutGap(4, 0);

        add(westPanel, BorderLayout.WEST);
        add(eastPanel, BorderLayout.CENTER);

        setBorderLayoutGap(0, 8);

        setHands();
    }

    private void checkButtons() {
        pageDownButton_.setEnabled(handFirst_ > 0);
        pageUpButton_.setEnabled(handFirst_ < handCount_ - pageSize_);
        exportButton_.setEnabled(selectedHandId_ >= 0 && handCount_ > 0);
    }

    private void setHands() {
        GameServerRestClient restClient = new GameServerRestClient(port_);

        // Compute the page number for the REST API.
        // handFirst_ is 0-based offset; REST uses page numbers.
        // Hands come from server in descending order, but we want ascending display.
        int dbHandCount = currentHand_ != null ? handCount_ - 1 : handCount_;
        int adjustedFirst = Math.max(handFirst_, 0);

        // The server returns hands in descending order. We need to map our ascending
        // offset to a descending page. For ascending offset 'first' with page size 'n'
        // from total 'T', the descending page is: (T - first - n) / n
        // But simpler: request the right page and reverse.
        int page = adjustedFirst / pageSize_;
        List<HandSummaryData> summaries = restClient.getHandSummaries(gameId_, jwt_, page, pageSize_);

        hands_ = new ArrayList<>(summaries.size());
        // Server returns descending order; reverse for display (newest first at top)
        for (int i = summaries.size() - 1; i >= 0; --i) {
            hands_.add(summaries.get(i));
        }

        if ((currentHand_ != null) && (handFirst_ + pageSize_ >= handCount_)) {
            hands_.add(0, currentHand_);
        }

        handsList_.setItems(hands_);

        if (hands_.isEmpty()) {
            titleLabel_.setText(PropertyConfig.getMessage("msg.nohistory"));
            pagingLabel_.setText("");
            detailsHtmlArea_.setText("");
            summaryHtmlArea_.setText("");
            pageUpButton_.setEnabled(false);
            pageDownButton_.setEnabled(false);
            exportButton_.setEnabled(false);
        } else {
            handsList_.setSelectedIndex(0);
            pagingLabel_.setText(PropertyConfig.getMessage("msg.histpage", Math.max(handFirst_, 0) + 1,
                    Math.min(handFirst_ + pageSize_, handCount_), handCount_));
            setHandIndex(handsList_.getSelectedIndex());
        }
    }

    private void setHistoryText() {
        if (selectedHandId_ < 0) {
            titleLabel_.setText(PropertyConfig.getMessage("msg.currenthand"));
            detailsHtmlArea_.setText(getCurrentHistoryText());

            if (tabs_.getTabCount() > 1) {
                tabs_.removeTabAt(1);
            }
        } else {
            String[] handHTML = getHandAsHTML(selectedHandId_, showAll(), showReason());
            titleLabel_.setText(handHTML[0]);
            summaryHtmlArea_.setText(handHTML[1]);
            detailsHtmlArea_.setText(handHTML[2]);

            summaryHtmlArea_.setCaretPosition(0); // scroll to top

            if (tabs_.getTabCount() < 2) {
                tabs_.addTab(PropertyConfig.getMessage("msg.histsummary"), icon_, summaryScroll_);
            }
        }

        detailsHtmlArea_.setCaretPosition(0); // scroll to top

        detailsHtmlArea_.repaint();
    }

    private String getCurrentHistoryText() {
        List<HandAction> hist = hhand_.getHistoryCopy();
        StringBuilder sb = new StringBuilder();

        if (hhand_.getAnte() > 0) {
            sb.append(getHist(hist, ClientBettingRound.PRE_FLOP.toLegacy(), hhand_, true));
        }
        sb.append(getHist(hist, ClientBettingRound.PRE_FLOP.toLegacy(), hhand_, false));
        sb.append(getHist(hist, ClientBettingRound.FLOP.toLegacy(), hhand_, false));
        sb.append(getHist(hist, ClientBettingRound.TURN.toLegacy(), hhand_, false));
        sb.append(getHist(hist, ClientBettingRound.RIVER.toLegacy(), hhand_, false));

        if (hhand_.getRound() == ClientBettingRound.SHOWDOWN) {
            appendShowdown(sb, hhand_.getHistoryCopy(), hhand_.getCommunity(), showAll());
        }

        return sb.toString();
    }

    private String getHist(List<HandAction> hist, int nRound, ClientHoldemHand hhand, boolean bAnte) {
        StringBuilder sb = new StringBuilder();
        ClientPlayer p;
        int nNum = 0;
        int nPrior = 0;

        ClientHand community = hhand.getCommunity();

        if (nRound == ClientBettingRound.PRE_FLOP.toLegacy()) {
            community = ClientHand.empty();
        } else if ((nRound == ClientBettingRound.FLOP.toLegacy()) && (community.size() > 3)) {
            community = ClientHand.of(community.getCard(0), community.getCard(1), community.getCard(2));
        } else if ((nRound == ClientBettingRound.TURN.toLegacy()) && (community.size() > 4)) {
            community = ClientHand.of(community.getCard(0), community.getCard(1), community.getCard(2),
                    community.getCard(3));
        }

        for (HandAction action : hist) {
            p = action.getPlayer();

            // must be from this round
            if (action.getRound() != nRound || (!bAnte && action.getAction() == HandAction.ACTION_ANTE))
                continue;
            if (bAnte && action.getAction() != HandAction.ACTION_ANTE)
                continue;

            ClientHand hand = p.getHand();

            String handHTML;
            String handShown = "";
            String sReason = showReason() ? decodeReason(action.getDebug()) : null;

            if (sReason == null)
                sReason = "";
            else
                sReason = " " + PropertyConfig.getMessage("msg.hist.reason", sReason);

            if (p.isCardsExposed() || (p.isHuman() && p.isLocallyControlled()) || showAll()) {
                handHTML = hand.toHTML();

                if (community.size() > 0 && p.getHandEval() != null) {
                    handShown = "&nbsp;-&nbsp;" + handDesc(p.getHandEval());
                }
            } else {
                handHTML = "<DDCARD FACEUP=\"false\"><DDCARD FACEUP=\"false\">";
            }

            String sSnippet = action.getHTMLSnippet("msg.handhist", nPrior, null);

            // get right raise icon
            if (action.getAction() == HandAction.ACTION_RAISE) {
                nPrior++;
            }

            // count actions added
            nNum++;

            // append message
            sb.append(PropertyConfig.getMessage("msg.hist.x", Utils.encodeHTML(p.getName()), sSnippet, handHTML,
                    handShown, sReason));
            sb.append("\n");
        }

        if (nNum == 0)
            return "";

        // if doing antes, change round (match client.properties)
        if (bAnte)
            nRound = 9;

        return PropertyConfig.getMessage("msg.hand.history", PropertyConfig.getMessage("msg.round." + nRound),
                sb.toString(), community.toHTML());
    }

    private void setHandIndex(int index) {

        Object o = hands_.get(index);

        if (o instanceof HandSummaryData summary) {
            if (selectedHandId_ == summary.handId())
                return;
            selectedHandId_ = summary.handId();
            hhand_ = null;
            // Practice games always allow show-all
            showAllCheckbox_.setEnabled(true);
        } else {
            if (hhand_ == o)
                return;
            selectedHandId_ = -1;
            hhand_ = (ClientHoldemHand) o;
            showAllCheckbox_.setEnabled(!hhand_.getClientTable().getGame().isOnlineGame());
        }

        checkButtons();

        showReasonCheckbox_.setEnabled(showAll());

        setHistoryText();
    }

    private boolean showAll() {
        return showAllCheckbox_.isEnabled() && showAllCheckbox_.isSelected();
    }

    private boolean showReason() {
        return showAll() && showReasonCheckbox_.isEnabled() && showReasonCheckbox_.isSelected();
    }

    @SuppressWarnings({"PublicInnerClass"})
    public static class HandListItemPanel extends ListItemPanel {
        private DDHtmlArea display_;

        public HandListItemPanel(ListPanel panel, Object item, String sStyle) {
            super(panel, item, sStyle);

            display_ = new DDHtmlArea(GuiManager.DEFAULT, sStyle);
            display_.setBorder(BorderFactory.createEmptyBorder());

            add(display_, BorderLayout.CENTER);
        }

        @Override
        public void update() {
            Object o = getItem();

            if (o instanceof HandSummaryData summary) {
                display_.setText("<html><body>" + buildHandListHTML(summary) + "</body></html>");
            } else {
                display_.setText("<html><body>" + ((ClientHoldemHand) o).getHandListHTML() + "</body></html>");
            }
        }
    }

    /**
     * Build HTML for the hand list item from a HandSummaryData.
     */
    static String buildHandListHTML(HandSummaryData summary) {
        StringBuilder buf = new StringBuilder();
        List<String> holeCards = summary.holeCards();
        if (holeCards != null && holeCards.size() >= 2) {
            buf.append(ClientCard.getCard(holeCards.get(0)).toHTML());
            buf.append(ClientCard.getCard(holeCards.get(1)).toHTML());
        } else {
            buf.append("<DDCARD FACEUP=\"false\"><DDCARD FACEUP=\"false\">");
        }
        buf.append("&nbsp;&nbsp;");
        List<String> community = summary.communityCards();
        if (community != null) {
            for (String card : community) {
                buf.append(ClientCard.getCard(card).toHTML());
            }
        }
        return buf.toString();
    }

    /**
     * Build hand detail HTML from the REST API, returning [title, summary,
     * details].
     */
    private String[] getHandAsHTML(long handId, boolean bShowAll, boolean bShowReason) {
        GameServerRestClient restClient = new GameServerRestClient(port_);
        HandDetailData detail = restClient.getHandDetail(gameId_, jwt_, handId);
        if (detail == null) {
            return new String[]{"", "", ""};
        }

        // Build community cards
        ClientHand community = ClientHand.empty();
        if (detail.communityCards() != null) {
            int dealt = detail.communityCardsDealt();
            List<String> cards = detail.communityCards();
            for (int i = 0; i < cards.size() && i < dealt; i++) {
                community.addCard(ClientCard.getCard(cards.get(i)));
            }
        }

        // Build player data
        ClientPlayer[] players = new ClientPlayer[ProtocolConstants.SEATS];
        int[] start = new int[ProtocolConstants.SEATS];
        int[] end = new int[ProtocolConstants.SEATS];

        for (HandPlayerDetailData p : detail.players()) {
            int seat = p.seatNumber();
            players[seat] = new ClientPlayer();
            start[seat] = p.startChips();
            end[seat] = p.endChips();
            players[seat].setChipCount(end[seat]);
            List<String> holeCards = p.holeCards();
            if (holeCards != null) {
                for (String card : holeCards) {
                    players[seat].getHand().addCard(ClientCard.getCard(card));
                }
            }
            boolean folded = ((p.preflopActions() | p.flopActions() | p.turnActions() | p.riverActions()) & 32) > 0;
            players[seat].setFolded(folded);
            players[seat].setCardsExposed(p.cardsExposed());
            players[seat].setName(p.playerName());
        }

        // Build action history
        ArrayList<HandAction> hist = new ArrayList<>();
        boolean bAnte = false;

        for (HandActionDetailData a : detail.actions()) {
            // Find the player by matching playerId to seat
            ClientPlayer player = null;
            for (HandPlayerDetailData p : detail.players()) {
                if (p.playerId() == a.playerId()) {
                    player = players[p.seatNumber()];
                    break;
                }
            }
            if (player == null)
                continue;

            int actionType = HandAction.decodeActionType(a.actionType());
            HandAction action = new HandAction(player, a.round(), actionType, a.amount(), a.subAmount(), null);
            action.setAllIn(a.allIn());
            hist.add(action);

            if (actionType == HandAction.ACTION_ANTE) {
                bAnte = true;
            }
        }

        // Title
        String title = PropertyConfig.getMessage("msg.handhist.title",
                new Object[]{String.valueOf(detail.tableId()), String.valueOf(detail.handNumber())});

        // Summary
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < ProtocolConstants.SEATS; ++i) {
            int delta = end[i] - start[i];
            if (delta != 0) {
                sb2.append(PropertyConfig.getMessage("msg.handhist.chipdelta", players[i].getName(),
                        PropertyConfig.getMessage("msg.handhist." + ((delta > 0) ? "wonchips" : "lostchips")),
                        Math.abs(delta),
                        (players[i].getChipCount() == 0)
                                ? PropertyConfig.getMessage("msg.handhist.busted")
                                : "&nbsp;"));
            }
        }

        String unit = PropertyConfig.getMessage("msg.chipunit.cash");
        String gameStyle = detail.gameStyle() != null ? detail.gameStyle() : "HOLDEM";
        String gameType = detail.gameType() != null ? detail.gameType() : "NOLIMIT";

        Object[] oParams = new Object[]{sb2.toString(),
                PropertyConfig.getMessage("msg.gamestyle." + gameStyle.toLowerCase()),
                PropertyConfig.getMessage("list.gameType." + gameType.toLowerCase()), (detail.ante() > 0) ? unit : "",
                (detail.ante() > 0) ? detail.ante() : (Object) PropertyConfig.getMessage("msg.forcedbet.none"), unit,
                detail.smallBlind(), unit, detail.bigBlind(),
                (detail.startDate() != null)
                        ? java.util.Date.from(detail.startDate())
                        : (Object) PropertyConfig.getMessage("msg.value.unknown"),
                (detail.endDate() != null)
                        ? PropertyConfig.getMessage("msg.handhist.enddate", java.util.Date.from(detail.endDate()))
                        : ""};

        String summary = PropertyConfig.getMessage("msg.handhist.summary", oParams);

        // Details
        StringBuilder details = new StringBuilder();
        if (bAnte)
            appendHistory(details, community, hist, ClientBettingRound.PRE_FLOP.toLegacy(), true, bShowAll,
                    bShowReason);
        appendHistory(details, community, hist, ClientBettingRound.PRE_FLOP.toLegacy(), false, bShowAll, bShowReason);
        appendHistory(details, community, hist, ClientBettingRound.FLOP.toLegacy(), false, bShowAll, bShowReason);
        appendHistory(details, community, hist, ClientBettingRound.TURN.toLegacy(), false, bShowAll, bShowReason);
        appendHistory(details, community, hist, ClientBettingRound.RIVER.toLegacy(), false, bShowAll, bShowReason);
        appendShowdown(details, hist, community, bShowAll);

        return new String[]{title, summary, details.toString()};
    }

    private static void appendHistory(StringBuilder sb, ClientHand community, ArrayList<HandAction> hist, int nRound,
            boolean bAnte, boolean bShowAll, boolean bShowReason) {
        StringBuilder sb2 = new StringBuilder();

        ClientPlayer p;
        HandAction action;
        int nNum = 0;
        int nPrior = 0;

        if (nRound == ClientBettingRound.PRE_FLOP.toLegacy()) {
            community = ClientHand.empty();
        } else if ((nRound == ClientBettingRound.FLOP.toLegacy()) && (community.size() > 3)) {
            community = ClientHand.of(community.getCard(0), community.getCard(1), community.getCard(2));
        } else if ((nRound == ClientBettingRound.TURN.toLegacy()) && (community.size() > 4)) {
            community = ClientHand.of(community.getCard(0), community.getCard(1), community.getCard(2),
                    community.getCard(3));
        }

        for (int i = 0; i < hist.size(); i++) {
            action = hist.get(i);
            p = action.getPlayer();

            // must be from this round
            if (action.getRound() != nRound || (!bAnte && action.getAction() == HandAction.ACTION_ANTE))
                continue;
            if (bAnte && action.getAction() != HandAction.ACTION_ANTE)
                continue;

            ClientHand hand = p.getHandSorted();

            String handHTML;
            String handShown = "";
            String sReason = (bShowAll && bShowReason) ? decodeReason(action.getDebug()) : null;

            if (sReason == null)
                sReason = "";
            else
                sReason = " " + PropertyConfig.getMessage("msg.hist.reason", sReason);

            if (p.isCardsExposed() || bShowAll) {
                handHTML = hand.toHTML();

                if (!community.isEmpty() && p.getHandEval() != null) {
                    handShown = "&nbsp;-&nbsp;" + handDesc(p.getHandEval());
                }
            } else {
                handHTML = "<DDCARD FACEUP=\"false\"><DDCARD FACEUP=\"false\">";
            }

            String sSnippet = action.getHTMLSnippet("msg.handhist", nPrior, null);

            // get right raise icon
            if (action.getAction() == HandAction.ACTION_RAISE) {
                nPrior++;
            }

            // count actions added
            nNum++;

            // append message
            sb2.append(PropertyConfig.getMessage("msg.hist.x", Utils.encodeHTML(p.getName()), sSnippet, handHTML,
                    handShown, sReason));
            sb2.append("\n");
        }

        if (nNum != 0) {
            // if doing antes, change round (match client.properties)
            if (bAnte)
                nRound = 9;

            sb.append(PropertyConfig.getMessage("msg.hand.history", PropertyConfig.getMessage("msg.round." + nRound),
                    sb2.toString(), community.toHTML()));
        }
    }

    static void appendShowdown(StringBuilder sb, List<HandAction> hist, ClientHand community, boolean bShowAll) {
        StringBuilder sb2 = new StringBuilder();

        for (int i = 8; i >= 0; i--) // can have a max of 9 pots
        {
            appendShowdown(sb2, hist, community, bShowAll, i);
        }

        sb.append(PropertyConfig.getMessage("msg.hand.history",
                PropertyConfig.getMessage("msg.round." + ClientBettingRound.SHOWDOWN), sb2.toString(),
                community.toHTML()));
    }

    private static void appendShowdown(StringBuilder sb, List<HandAction> hist, ClientHand community, boolean bShowAll,
            int nPot) {
        int nNum = 0;
        int potTotal = 0;

        for (HandAction action : hist) {
            if (action.getRound() != ClientBettingRound.SHOWDOWN.toLegacy())
                continue;
            if (action.getSubAmount() != nPot)
                continue;

            if (action.getAction() == HandAction.ACTION_WIN) {
                potTotal += action.getAmount();
            }
            nNum++;
        }

        if (nNum == 0) {
            return;
        }

        String sHeaderKey = null;

        if ((sb.length() > 0) || nPot > 0) {
            if (nPot == 0) {
                sHeaderKey = "msg.handhist.pot.main";
            } else {
                sHeaderKey = "msg.handhist.pot.side";
            }
        }

        StringBuilder sb2 = new StringBuilder();

        for (HandAction action : hist) {
            if (action.getRound() != ClientBettingRound.SHOWDOWN.toLegacy())
                continue;
            if (action.getSubAmount() != nPot)
                continue;

            ClientPlayer player = action.getPlayer();

            if (action.getAction() == HandAction.ACTION_OVERBET) {
                sHeaderKey = null;
            }

            ClientHand hand = player.getHandSorted();

            String handHTML;
            String handShown = "";

            if (player.isCardsExposed() || bShowAll) {
                handHTML = hand.toHTML();

                if (!community.isEmpty() && player.getHandEval() != null) {
                    handShown = "&nbsp;-&nbsp;" + handDesc(player.getHandEval());
                }
            } else {
                handHTML = "<DDCARD FACEUP=\"false\"><DDCARD FACEUP=\"false\">";
            }

            String sSnippet = action.getHTMLSnippet("msg.handhist", 0, null);

            sb2.append(PropertyConfig.getMessage("msg.hist.x", Utils.encodeHTML(player.getName()), sSnippet, handHTML,
                    handShown, ""));
            sb2.append("\n");
        }

        if (sHeaderKey != null) {
            String sHeader = PropertyConfig.getMessage(sHeaderKey, nPot, potTotal);
            sb.append(PropertyConfig.getMessage("msg.handhist.pot", sHeader, sb2.toString()));
        } else {
            sb.append(sb2.toString());
        }
    }

    private void export() {
        TypedHashMap params = new TypedHashMap();
        params.setString("gameId", gameId_);
        params.setString("jwt", jwt_);
        params.setInteger("port", port_);
        if (selectedHandId_ > 0) {
            params.setLong("handId", selectedHandId_);
        }
        context_.processPhaseNow("HistoryExportDialog", params);
    }

    /**
     * Build a short hand description from server-provided HandEvaluationData.
     */
    public static String handDesc(HandEvaluationData eval) {
        int type = eval.handType();
        StringBuilder buf = new StringBuilder();
        buf.append(PropertyConfig.getMessage("msg.hand." + type));
        String sep = ", ";
        switch (type) {
            case ClientHandScoreConstants.HIGH_CARD :
                if (eval.highCardRank() != null) {
                    buf.append(sep);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type, PropertyConfig
                            .getMessage("msg.cardrank.singular", ClientCard.getRank(eval.highCardRank()))));
                }
                break;
            case ClientHandScoreConstants.PAIR :
                if (eval.bigPairRank() != null) {
                    buf.append(sep);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                            PropertyConfig.getMessage("msg.cardrank.plural", ClientCard.getRank(eval.bigPairRank()))));
                }
                break;
            case ClientHandScoreConstants.TWO_PAIR :
                if (eval.bigPairRank() != null && eval.smallPairRank() != null) {
                    buf.append(sep);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                            PropertyConfig.getMessage("msg.cardrank.plural", ClientCard.getRank(eval.bigPairRank())),
                            PropertyConfig.getMessage("msg.cardrank.plural",
                                    ClientCard.getRank(eval.smallPairRank()))));
                }
                break;
            case ClientHandScoreConstants.TRIPS :
                if (eval.tripsRank() != null) {
                    buf.append(sep);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                            PropertyConfig.getMessage("msg.cardrank.plural", ClientCard.getRank(eval.tripsRank()))));
                }
                break;
            case ClientHandScoreConstants.STRAIGHT :
            case ClientHandScoreConstants.STRAIGHT_FLUSH :
                if (eval.straightLowRank() != null && eval.straightHighRank() != null) {
                    buf.append(sep);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                            PropertyConfig.getMessage("msg.cardrank.singular",
                                    ClientCard.getRank(eval.straightLowRank())),
                            PropertyConfig.getMessage("msg.cardrank.singular",
                                    ClientCard.getRank(eval.straightHighRank()))));
                }
                break;
            case ClientHandScoreConstants.FLUSH :
                if (eval.flushHighRank() != null) {
                    buf.append(sep);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type, PropertyConfig
                            .getMessage("msg.cardrank.singular", ClientCard.getRank(eval.flushHighRank()))));
                }
                break;
            case ClientHandScoreConstants.FULL_HOUSE :
                if (eval.tripsRank() != null && eval.bigPairRank() != null) {
                    buf.append(sep);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                            PropertyConfig.getMessage("msg.cardrank.plural", ClientCard.getRank(eval.tripsRank())),
                            PropertyConfig.getMessage("msg.cardrank.plural", ClientCard.getRank(eval.bigPairRank()))));
                }
                break;
            case ClientHandScoreConstants.QUADS :
                if (eval.quadsRank() != null) {
                    buf.append(sep);
                    buf.append(PropertyConfig.getMessage("msg.handfmt." + type,
                            PropertyConfig.getMessage("msg.cardrank.plural", ClientCard.getRank(eval.quadsRank()))));
                }
                break;
            default :
                break;
        }
        return buf.toString();
    }

    static String decodeReason(String sReason) {
        if (sReason == null) {
            return null;
        }

        if (sReason.startsWith("V1:")) {
            return null;
        }

        try {
            return PropertyConfig.getMessage("msg.aioutcome." + sReason);
        } catch (Exception e) {
            return null;
        }
    }
}
