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
/*
 * DescriptionDialog.java
 *
 * Created on April 29, 2004, 02:18 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.Utils;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.config.GameButton;
import com.donohoedigital.games.engine.FileChooserDialog;
import com.donohoedigital.games.poker.display.ClientCard;
import com.donohoedigital.games.poker.impexp.ImpExp;
import com.donohoedigital.games.poker.impexp.ImpExpHand;
import com.donohoedigital.games.poker.impexp.ImpExpParadise;
import com.donohoedigital.games.poker.online.ClientPlayer;
import com.donohoedigital.games.poker.protocol.constants.ProtocolConstants;
import com.donohoedigital.games.poker.protocol.dto.HandActionDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandExportData;
import com.donohoedigital.games.poker.protocol.dto.HandPlayerDetailData;
import com.donohoedigital.games.poker.server.GameServerRestClient;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;

public class HistoryExportDialog extends FileChooserDialog {

    private String gameId_;
    private String jwt_;
    private int port_;
    private List<HandExportData> exportHands_;

    private JComponent base_;

    private DDCheckBox cbxHands_;
    private DDCheckBox cbxTournaments_;

    private DDProgressBar progressBar_;

    private DDTextField txtSize_;

    private boolean cancel_ = false;

    private static final javax.swing.filechooser.FileFilter filterParadise_ = new javax.swing.filechooser.FileFilter() {
        public boolean accept(File f) {
            return !f.isFile() || f.getName().endsWith(".paradise.txt");
        }
        public String getDescription() {
            return "Paradise Poker (*.paradise.txt)";
        }
    };

    public JComponent createTopPanel() {
        String sStyle = gamephase_.getString("style");

        DDLabel lblExport = new DDLabel("export", sStyle);
        DDLabel lblSize = new DDLabel("estfilesize", sStyle);

        cbxHands_ = new DDCheckBox();
        cbxTournaments_ = new DDCheckBox();

        txtSize_ = new DDTextField("estfilesize", sStyle);

        lblSize.setHorizontalAlignment(DDTextField.RIGHT);

        txtSize_.setHorizontalAlignment(DDTextField.RIGHT);
        txtSize_.setEditable(false);
        txtSize_.setOpaque(false);

        DDPanel left = new DDPanel();

        ((BorderLayout) left.getLayout()).setHgap(8);

        left.add(lblExport, BorderLayout.WEST);
        left.add(cbxHands_, BorderLayout.CENTER);

        DDPanel right = new DDPanel();

        ((BorderLayout) right.getLayout()).setHgap(8);

        right.add(lblSize, BorderLayout.WEST);
        right.add(txtSize_, BorderLayout.CENTER);

        DDPanel top = new DDPanel();

        ((BorderLayout) top.getLayout()).setHgap(32);
        top.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.CENTER);

        return top;
    }

    public JComponent createBottomPanel() {
        String sStyle = gamephase_.getString("style");

        progressBar_ = new DDProgressBar(GuiManager.DEFAULT, sStyle, false);
        progressBar_.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        progressBar_.setPreferredWidth(500);
        progressBar_.setPreferredHeight(30);
        progressBar_.setVisible(false);

        return GuiUtils.CENTER(progressBar_);
    }

    public JComponent createDialogContents() {
        String sStyle = gamephase_.getString("style");

        base_ = super.createDialogContents();

        choose_.resetChoosableFileFilters();
        choose_.addChoosableFileFilter(filterParadise_);

        final DDLabel wait = new DDLabel("waitexport", sStyle);

        wait.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        wait.setPreferredSize(choose_.getPreferredSize());

        base_.remove(choose_);
        base_.add(wait, BorderLayout.CENTER);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gameId_ = (String) gamephase_.getObject("gameId");
                jwt_ = (String) gamephase_.getObject("jwt");
                Integer portObj = gamephase_.getInteger("port");
                port_ = portObj != null ? portObj : 0;

                Long handId = gamephase_.getLong("handId");

                if (gameId_ != null && jwt_ != null && port_ > 0) {
                    GameServerRestClient restClient = new GameServerRestClient(port_);
                    if (handId != null && handId > 0) {
                        // Export a single hand
                        var detail = restClient.getHandDetail(gameId_, jwt_, handId);
                        if (detail != null) {
                            // Convert HandDetailData to HandExportData for consistency
                            exportHands_ = List.of(new HandExportData(detail.handId(), detail.handNumber(), null, null,
                                    String.valueOf(detail.tableId()), detail.gameStyle(), detail.gameType(),
                                    detail.startDate(), detail.endDate(), detail.ante(), detail.smallBlind(),
                                    detail.bigBlind(), null, detail.communityCards(), detail.players(),
                                    detail.actions()));
                        } else {
                            exportHands_ = List.of();
                        }
                    } else {
                        exportHands_ = restClient.getHandsForExport(gameId_, jwt_);
                    }
                } else {
                    exportHands_ = List.of();
                }

                ActionListener actionListener = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        checkButtons();
                    }
                };

                cbxHands_.setText(PropertyConfig.getMessage(
                        "msg.handtranscriptcount" + (exportHands_.size() != 1 ? ".plural" : ".singular"),
                        exportHands_.size()));
                cbxHands_.setSelected(true);

                cbxHands_.addActionListener(actionListener);

                // Count distinct tournaments
                long tournamentCount = exportHands_.stream().map(HandExportData::tournamentId).distinct().count();
                cbxTournaments_.setText(PropertyConfig.getMessage(
                        "msg.tourneysummarycount" + (tournamentCount != 1 ? ".plural" : ".singular"), tournamentCount));

                cbxTournaments_.addActionListener(actionListener);

                base_.remove(wait);
                base_.add(choose_, BorderLayout.CENTER);

                checkButtons();
            }
        });

        return base_;
    }

    protected void checkButtons() {
        super.checkButtons();

        if (txtSize_ != null)
            txtSize_.setText(String.valueOf(
                    Utils.formatSizeBytes((int) ((cbxHands_.isSelected() ? (1500L * exportHands_.size()) : 0)))));

        if ((okayButton_ != null) && (cbxHands_ != null) && (cbxTournaments_ != null))
            okayButton_.setEnabled(okayButton_.isEnabled() && (cbxHands_.isSelected() || cbxTournaments_.isSelected()));
    }

    /**
     * Default processButton calls closes dialog on any button press
     */
    public boolean processButton(GameButton button) {
        if (super.processButton(button, false) && (getResult() instanceof File)) {
            getMatchingButton("export").setEnabled(false);

            progressBar_.setVisible(true);

            new Thread("HistoryExportDialog") {
                public void run() {
                    ImpExp ie = new ImpExpParadise();
                    ie.setPlayerName(PlayerProfileOptions.getDefaultProfile().getName());

                    try {
                        File file = (File) getResult();
                        String path = file.getAbsolutePath();

                        Object filter = choose_.getFileFilter();

                        if (filter == filterParadise_) {
                            if (!path.endsWith(".paradise.txt")) {
                                path = path + ".paradise.txt";
                            }
                        }

                        Writer w = new FileWriter(path);

                        java.util.Set<Integer> tournamentsExported = new java.util.HashSet<>();

                        for (int i = 0; i < exportHands_.size() && !cancel_; ++i) {
                            HandExportData data = exportHands_.get(i);
                            ImpExpHand ieHand = toImpExpHand(data);

                            if (cbxTournaments_.isSelected() && data.tournamentId() != null
                                    && !tournamentsExported.contains(data.tournamentId())) {
                                w.write(ie.exportTournament(ieHand));
                                w.write("\r\n\r\n");
                                tournamentsExported.add(data.tournamentId());
                            }

                            if (cbxHands_.isSelected()) {
                                w.write(ie.exportHand(ieHand));
                                w.write("\r\n\r\n");
                            }

                            progressBar_.setPercentDone((i * 100) / exportHands_.size());
                        }

                        w.close();
                    } catch (IOException e) {
                        throw new ApplicationError(e);
                    } finally {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                removeDialog();
                            }
                        });
                    }
                }
            }.start();
        } else {
            removeDialog();
            cancel_ = true;
        }

        return true;
    }

    /**
     * Convert a HandExportData DTO to the legacy ImpExpHand for export format
     * compatibility.
     */
    private ImpExpHand toImpExpHand(HandExportData data) {
        ImpExpHand ieHand = new ImpExpHand();

        PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
        ieHand.profileNumber = profile.getFileNumber(profile.getFile());
        ieHand.handID = data.handId() != null ? data.handId().intValue() : 0;
        ieHand.hndTable = data.tableId();
        ieHand.hndNumber = String.valueOf(data.handNumber());
        ieHand.gameStyle = data.gameStyle();
        ieHand.gameType = data.gameType();
        if (data.startDate() != null) {
            ieHand.startDate.setTime(Date.from(data.startDate()));
        }
        if (data.endDate() != null) {
            ieHand.endDate.setTime(Date.from(data.endDate()));
        }
        ieHand.ante = data.ante();
        ieHand.smallBlind = data.smallBlind();
        ieHand.bigBlind = data.bigBlind();
        ieHand.tournamentID = data.tournamentId() != null ? data.tournamentId() : 0;
        ieHand.tournamentName = data.tournamentName() != null ? data.tournamentName() : "";
        if (data.buttonSeat() != null) {
            ieHand.buttonSeat = data.buttonSeat();
        }

        // Community cards
        if (data.communityCards() != null) {
            for (String card : data.communityCards()) {
                ieHand.community.addCard(ClientCard.getCard(card));
            }
        }

        // Players
        if (data.players() != null) {
            for (HandPlayerDetailData p : data.players()) {
                int seat = p.seatNumber();
                ieHand.players[seat] = new ClientPlayer();
                ieHand.players[seat].setSeat(seat);
                ieHand.startChips[seat] = p.startChips();
                ieHand.endChips[seat] = p.endChips();
                ieHand.betChips[seat] = 0;
                ieHand.players[seat].setChipCount(p.endChips());
                if (p.holeCards() != null) {
                    for (String card : p.holeCards()) {
                        ieHand.players[seat].getHand().addCard(ClientCard.getCard(card));
                    }
                }
                boolean folded = ((p.preflopActions() | p.flopActions() | p.turnActions() | p.riverActions()) & 32) > 0;
                ieHand.players[seat].setFolded(folded);
                ieHand.players[seat].setCardsExposed(p.cardsExposed());
                ieHand.players[seat].setName(p.playerName());
            }
        }

        // Actions
        if (data.actions() != null) {
            int firstBlindSeat = -1;
            for (HandActionDetailData a : data.actions()) {
                ClientPlayer player = null;
                int seat = -1;
                if (data.players() != null) {
                    for (HandPlayerDetailData p : data.players()) {
                        if (p.playerId() == a.playerId()) {
                            seat = p.seatNumber();
                            player = ieHand.players[seat];
                            break;
                        }
                    }
                }
                if (player == null)
                    continue;

                int actionType = HandAction.decodeActionType(a.actionType());
                HandAction action = new HandAction(player, a.round(), actionType, a.amount(), a.subAmount(), null);
                action.setAllIn(a.allIn());
                ieHand.hist.add(action);

                switch (actionType) {
                    case HandAction.ACTION_OVERBET :
                        ieHand.overbetChips[seat] += a.amount();
                        break;
                    case HandAction.ACTION_WIN :
                        ieHand.winChips[seat] += a.amount();
                        break;
                    case HandAction.ACTION_CALL :
                    case HandAction.ACTION_BET :
                    case HandAction.ACTION_RAISE :
                    case HandAction.ACTION_ANTE :
                        ieHand.betChips[seat] += a.amount();
                        break;
                    case HandAction.ACTION_BLIND_SM :
                    case HandAction.ACTION_BLIND_BIG :
                        ieHand.betChips[seat] += a.amount();
                        if (firstBlindSeat < 0)
                            firstBlindSeat = seat;
                        break;
                }
            }

            // Determine button seat if not provided
            if (data.buttonSeat() == null && firstBlindSeat >= 0) {
                int playerCount = 0;
                for (int i = 0; i < ProtocolConstants.SEATS; i++) {
                    if (ieHand.players[i] != null)
                        playerCount++;
                }
                if (playerCount == 2) {
                    ieHand.buttonSeat = firstBlindSeat;
                } else {
                    for (int s = firstBlindSeat - 1; s > firstBlindSeat - ProtocolConstants.SEATS; --s) {
                        if (ieHand.players[(s + ProtocolConstants.SEATS) % ProtocolConstants.SEATS] != null) {
                            ieHand.buttonSeat = (s + ProtocolConstants.SEATS) % ProtocolConstants.SEATS;
                            break;
                        }
                    }
                }
            }
        }

        return ieHand;
    }

    protected String fixName(String sName) {
        return sName;
    }
}
