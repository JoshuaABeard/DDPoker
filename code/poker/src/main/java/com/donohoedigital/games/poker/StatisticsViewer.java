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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.display.ClientBettingRound;
import com.donohoedigital.games.poker.protocol.dto.HandRoundStatsData;
import com.donohoedigital.games.poker.protocol.dto.HandStatsData;
import com.donohoedigital.games.poker.server.GameServerRestClient;

import com.donohoedigital.gui.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class StatisticsViewer extends BasePhase implements ActionListener {
    public static final String COL_FINISH = "finish";
    public static final String COL_NAME = "name";
    public static final String COL_ENDDATE = "enddate";
    public static final String COL_TOTALBUYIN = "totalbuyin";
    public static final String COL_REBUY = "rebuy";
    public static final String COL_ADDON = "addon";
    public static final String COL_PRIZE = "prize";
    public static final String COL_PROFIT = "profit";

    private static final int RW = 120; // rank width
    private static final int CW = 100; // chip width
    private static final int NW = 188; // name width
    private static final int DW = 140; // name width

    private DDTable finishTable_;

    private static final String[] RESULTS_NAMES = new String[]{COL_FINISH, COL_NAME, COL_ENDDATE, COL_TOTALBUYIN,
            COL_PRIZE, COL_PROFIT};
    // client table info
    private static final int[] RESULTS_WIDTHS = new int[]{RW, NW, DW, CW, CW, CW};

    private ResultsModel resultsModel_;
    private PlayerProfile profile_;
    private MenuBackground menu_;
    private ButtonBox buttonbox_;
    private DDTabbedPane tabs_;
    private GlassButton delete_;

    public StatisticsViewer() {
    }

    @Override
    public void init(GameEngine engine, GameContext context, GamePhase gamephase) {
        super.init(engine, context, gamephase);

        DDPanel top = new DDPanel();
        top.setBorderLayoutGap(5, 10);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        DDPanel topinfo = new DDPanel();
        top.add(topinfo, BorderLayout.NORTH);

        DDLabel clabel = new DDLabel(GuiManager.DEFAULT, "Analysis");
        topinfo.add(clabel, BorderLayout.CENTER);
        topinfo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 17));
        profile_ = PlayerProfileOptions.getDefaultProfile();
        clabel.setText(PropertyConfig.getMessage("msg.statsfor", profile_.getName()));

        GlassButton change = new GlassButton("changeprofile", "Glass");
        topinfo.add(change, BorderLayout.EAST);
        change.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                context_.processPhase("PlayerProfileOptions");
            }
        });

        DDScrollTable scrollOut = new DDScrollTable(GuiManager.DEFAULT, "PokerPrefsPlayerList", "BrushedMetal",
                RESULTS_NAMES, RESULTS_WIDTHS);
        scrollOut.setPreferredSize(new Dimension(scrollOut.getPreferredWidth(), 104));
        top.add(GuiUtils.WEST(scrollOut), BorderLayout.CENTER);

        finishTable_ = scrollOut.getDDTable();

        resultsModel_ = new ResultsModel(getFinishes(), RESULTS_NAMES, RESULTS_WIDTHS);
        finishTable_.setModel(resultsModel_);
        finishTable_.setExporter(new TableExporter(context_, "tournaments"));
        finishTable_.setRowSelectionInterval(0, 0);
        finishTable_.setShowHorizontalLines(true);
        finishTable_.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        finishTable_.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 3; i <= 5; i++) {
            finishTable_.setAlign(i, SwingConstants.RIGHT);
        }

        menu_ = new MenuBackground(gamephase);
        buttonbox_ = new ButtonBox(context_, gamephase, this, "empty", false, false);
        menu_.getMenuBox().add(buttonbox_, BorderLayout.SOUTH);

        String style = gamephase_.getString("menubox-style", "StartMenu");

        DDPanel base = new DDPanel(GuiManager.DEFAULT, style);
        menu_.getMenuBox().setBorderLayoutGap(5, 0);
        menu_.getMenuBox().add(base, BorderLayout.CENTER);

        tabs_ = new DDTabbedPane(style, "BrushedMetal", JTabbedPane.TOP);
        tabs_.setOpaque(false);
        tabs_.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                checkDetailsButton();
            }
        });

        base.add(GuiUtils.CENTER(top), BorderLayout.NORTH);
        DDPanel overlay = new DDPanel();
        base.add(overlay, BorderLayout.CENTER);
        overlay.setLayout(new SVLayout());

        DDPanel deletebase = new DDPanel();
        deletebase.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        delete_ = new GlassButton("delete", "Glass");
        delete_.addActionListener(this);
        checkButtons();
        deletebase.add(delete_, BorderLayout.CENTER);
        overlay.add(deletebase, new ScaleConstraintsFixed(SwingConstants.TOP, SwingConstants.RIGHT));
        overlay.add(tabs_, BorderLayout.CENTER);

        ImageComponent ic = new ImageComponent("ddlogo20", 1.0d);

        ic.setScaleToFit(false);
        ic.setIconWidth(GamePrefsPanel.ICWIDTH);
        ic.setIconHeight(GamePrefsPanel.ICHEIGHT);

        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.overall"), ic, new OverallPanel(), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.byhand"), ic, new ByHandPanel(), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.preflop"), ic,
                new ByRoundPanel(ClientBettingRound.PRE_FLOP.toLegacy()), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.flop"), ic,
                new ByRoundPanel(ClientBettingRound.FLOP.toLegacy()), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.turn"), ic,
                new ByRoundPanel(ClientBettingRound.TURN.toLegacy()), null);
        tabs_.addTab(PropertyConfig.getMessage("msg.handhistory.river"), ic,
                new ByRoundPanel(ClientBettingRound.RIVER.toLegacy()), null);

        finishTable_.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;

                if (finishTable_.getSelectedRow() < 0) {
                    finishTable_.setRowSelectionInterval(0, 0);
                    finishTable_.repaint();
                } else {
                    Component c = tabs_.getSelectedComponent();

                    if (c instanceof OverallPanel)
                        ((OverallPanel) c).refresh();
                    if (c instanceof ByHandPanel)
                        ((ByHandPanel) c).refresh();
                    if (c instanceof ByRoundPanel)
                        ((ByRoundPanel) c).refresh();
                }
            }
        });
    }

    /**
     * get array of finishes
     */
    private List<ClientTournamentHistory> getFinishes() {
        List<ClientTournamentHistory> finishes = profile_.getHistory();
        finishes.add(0, profile_.getOverallHistory());
        return finishes;
    }

    /**
     * set delete button enabled
     */
    private void checkButtons() {
        delete_.setEnabled(finishTable_.getRowCount() > 1);
    }

    /**
     * delete button
     */
    public void actionPerformed(ActionEvent e) {
        boolean bDelete = false;
        if (finishTable_.getSelectedRow() == 0) {
            bDelete = PlayerProfileOptions.deleteAllHistory(context_, profile_);
        } else {
            bDelete = PlayerProfileOptions.deleteHistory(context_, profile_, finishTable_.getSelectedRow() - 1);
        }
        if (bDelete) {
            resultsModel_.updateFinishes(getFinishes());
            checkButtons();
        }
    }

    /**
     * Set component
     */
    @Override
    public void start() {
        context_.setMainUIComponent(this, menu_, false, tabs_);
    }

    /**
     * interface to indicate if details can be shown
     */
    private interface ShowDetails {
        public boolean canShowDetails();
        public void showDetails();
        public void exportHistory();
    }

    private static final String[] byHandColNames_ = {"stats.cards", "stats.numhands", "stats.winpct", "stats.losepct",
            "stats.passpct", "stats.winbets", "stats.seeflop", "stats.seeturn", "stats.seeriver", "stats.seeshowdown"};

    /**
     * Table model backed by HandStatsData from REST API.
     */
    private static class ByHandModel extends DefaultTableModel {
        private final List<HandStatsData> stats_;

        public ByHandModel(List<HandStatsData> stats) {
            stats_ = stats;
            for (String name : byHandColNames_) {
                addColumn(name);
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public int getRowCount() {
            return stats_ == null ? 0 : stats_.size();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return Object.class;
            return Number.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            HandStatsData s = stats_.get(row);
            Object v = switch (column) {
                case 0 -> s.handClass();
                case 1 -> s.count();
                case 2 -> formatPct(s.winPct());
                case 3 -> formatPct(s.losePct());
                case 4 -> formatPct(s.passPct());
                case 5 -> String.format("%.2f", s.avgBet());
                case 6 -> formatPct(s.flopPct());
                case 7 -> formatPct(s.turnPct());
                case 8 -> formatPct(s.riverPct());
                case 9 -> formatPct(s.showdownPct());
                default -> null;
            };
            if ("0%".equals(v))
                return null;
            return v;
        }
    }

    private class OverallModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    private class OverallPanel extends DDTabPanel implements ShowDetails {
        DDTable table_;

        public OverallPanel() {
            super();
            createUI(); // first panel, so create at outset
        }

        public void refresh() {
            OverallModel tmodel = new OverallModel();
            tmodel.setColumnCount(2);

            ClientTournamentHistory hist = getSelectedTournament();

            // Get hand stats from REST (for overall stats like win%, etc.)
            List<HandStatsData> handStats = List.of();
            PokerGame.WebSocketConfig cfg = getWebSocketConfig(hist);
            if (cfg != null) {
                try {
                    GameServerRestClient restClient = new GameServerRestClient(cfg.port());
                    handStats = restClient.getHandStats(cfg.gameId(), cfg.jwt());
                } catch (Exception ignored) {
                    // stats unavailable
                }
            }

            // Aggregate the hand stats to get overall numbers
            int totalHands = 0;
            int totalWins = 0;
            int totalLosses = 0;
            int totalPasses = 0;
            double totalBet = 0;
            int sawFlop = 0;
            int sawTurn = 0;
            int sawRiver = 0;
            int sawShowdown = 0;
            for (HandStatsData s : handStats) {
                totalHands += s.count();
                totalWins += (int) Math.round(s.winPct() * s.count() / 100.0);
                totalLosses += (int) Math.round(s.losePct() * s.count() / 100.0);
                totalPasses += (int) Math.round(s.passPct() * s.count() / 100.0);
                totalBet += s.avgBet() * s.count();
                sawFlop += (int) Math.round(s.flopPct() * s.count() / 100.0);
                sawTurn += (int) Math.round(s.turnPct() * s.count() / 100.0);
                sawRiver += (int) Math.round(s.riverPct() * s.count() / 100.0);
                sawShowdown += (int) Math.round(s.showdownPct() * s.count() / 100.0);
            }

            GameEngine engine = GameEngine.getGameEngine();
            String sLocale = null;

            if (engine != null) {
                sLocale = engine.getLocale();
            }

            int nPlace = hist.getPlace();

            if (hist.getGameId() != 0) {
                tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.tournamentname"),
                        hist.getTournamentName() + " (" + PropertyConfig
                                .getMessage("msg.handhistory.tournamenttype." + hist.getTournamentType()) + ')'});
                tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.startdate"),
                        PropertyConfig.getDateFormat(sLocale).format(hist.getStartDate())});

                if (nPlace == 0) {
                    tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.enddate"),
                            PropertyConfig.getMessage("msg.incomplete")});
                    tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.lastdate"),
                            PropertyConfig.getDateFormat(sLocale).format(hist.getEndDate())});
                } else {
                    tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.enddate"),
                            PropertyConfig.getDateFormat(sLocale).format(hist.getEndDate())});
                }
            } else {
                tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.tournamentname"),
                        hist.getTournamentName()});
            }
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.buyin"),
                    getNumber(hist.getBuyin(), false)});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.totalrebuys"),
                    getNumber(hist.getRebuy(), false)});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.totaladdons"),
                    getNumber(hist.getAddon(), false)});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.totalbuyin"),
                    getNumber(hist.getTotalSpent(), false)});
            if (nPlace > 0) {
                tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.prize"),
                        getNumber(hist.getPrize(), false)});
                tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.profit"),
                        getNumber(hist.getPrize() - hist.getTotalSpent(), false)});
            }
            if (hist.getGameId() != 0) {
                if (nPlace > 0) {
                    tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.finishplace"),
                            PropertyConfig.getMessage("msg.finishoutof", PropertyConfig.getPlace(nPlace),
                                    hist.getNumPlayers())});
                } else {
                    tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.playersremaining"),
                            PropertyConfig.getMessage("msg.finishoutof", hist.getNumRemaining(),
                                    hist.getNumPlayers())});
                }
            }
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.numhands"), totalHands});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.winpct"),
                    totalHands > 0 ? formatPct(totalWins * 100.0 / totalHands) : null});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.losepct"),
                    totalHands > 0 ? formatPct(totalLosses * 100.0 / totalHands) : null});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.passpct"),
                    totalHands > 0 ? formatPct(totalPasses * 100.0 / totalHands) : null});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.winbets"),
                    totalHands > 0 ? String.format("%.2f", totalBet / totalHands) : null});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.seeflop"),
                    totalHands > 0 ? formatPct(sawFlop * 100.0 / totalHands) : null});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.seeturn"),
                    totalHands > 0 ? formatPct(sawTurn * 100.0 / totalHands) : null});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.seeriver"),
                    totalHands > 0 ? formatPct(sawRiver * 100.0 / totalHands) : null});
            tmodel.addRow(new Object[]{PropertyConfig.getMessage("msg.handhistory.overall.seeshowdown"),
                    totalHands > 0 ? formatPct(sawShowdown * 100.0 / totalHands) : null});
            table_.setModel(tmodel);
            table_.setExporter(new TableExporter(context_, "overall"));
        }

        @Override
        public void ancestorAdded(AncestorEvent event) {
            super.ancestorAdded(event);
            refresh();
        }

        public boolean canShowDetails() {
            return true;
        }

        public void exportHistory() {
            context_.processPhaseNow("HistoryExportDialog", getDetailsParams());
        }

        public void showDetails() {
            context_.processPhaseNow("HandHistoryDialog", getDetailsParams());
        }

        private TypedHashMap getDetailsParams() {
            TypedHashMap params = new TypedHashMap();
            ClientTournamentHistory hist = getSelectedTournament();
            PokerGame.WebSocketConfig cfg = getWebSocketConfig(hist);
            if (cfg != null) {
                params.setString("gameId", cfg.gameId());
                params.setString("jwt", cfg.jwt());
                params.setInteger("port", cfg.port());
            }
            return params;
        }

        @Override
        public void createUI() {
            DDScrollTable scrollTable = new DDScrollTable("stats", "PokerPrefsPlayerList", "BrushedMetal",
                    new String[]{"stats.statname", "stats.statvalue"}, new int[]{160, 200});
            table_ = scrollTable.getDDTable();
            table_.setTableHeader(null);
            table_.setRowSelectionAllowed(false);
            scrollTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            add(GuiUtils.CENTER(scrollTable), BorderLayout.CENTER);
            refresh();
            checkDetailsButton();
        }
    }

    private class ByHandPanel extends DDTabPanel implements ShowDetails {
        DDTable table_;

        public ByHandPanel() {
            super();
        }

        public void refresh() {
            ClientTournamentHistory hist = getSelectedTournament();
            List<HandStatsData> stats = List.of();
            PokerGame.WebSocketConfig cfg = getWebSocketConfig(hist);
            if (cfg != null) {
                try {
                    GameServerRestClient restClient = new GameServerRestClient(cfg.port());
                    stats = restClient.getHandStats(cfg.gameId(), cfg.jwt());
                } catch (Exception ignored) {
                }
            }
            table_.setModel(new ByHandModel(stats));
            table_.setExporter(new TableExporter(context_, "byhand"));
        }

        @Override
        public void ancestorAdded(AncestorEvent event) {
            super.ancestorAdded(event);
            refresh();
        }

        public boolean canShowDetails() {
            return (table_ != null) && table_.getSelectedRow() >= 0;
        }

        public void exportHistory() {
            context_.processPhaseNow("HistoryExportDialog", getDetailsParams());
        }

        public void showDetails() {
            context_.processPhaseNow("HandHistoryDialog", getDetailsParams());
        }

        private TypedHashMap getDetailsParams() {
            TypedHashMap params = new TypedHashMap();
            ClientTournamentHistory hist = getSelectedTournament();
            PokerGame.WebSocketConfig cfg = getWebSocketConfig(hist);
            if (cfg != null) {
                params.setString("gameId", cfg.gameId());
                params.setString("jwt", cfg.jwt());
                params.setInteger("port", cfg.port());
            }
            return params;
        }

        @Override
        public void createUI() {
            DDScrollTable scrollTable = new DDScrollTable("stats", "PokerPrefsPlayerList", "BrushedMetal",
                    byHandColNames_, new int[]{40, 40, 40, 40, 40, 40, 40, 40, 40, 40});

            scrollTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            add(scrollTable, BorderLayout.CENTER);

            table_ = scrollTable.getDDTable();

            table_.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    checkDetailsButton();
                }
            });
            table_.addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        showDetails();
                    }
                }

                public void mousePressed(MouseEvent e) {
                }

                public void mouseReleased(MouseEvent e) {
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }
            });
            refresh();
            checkDetailsButton();
        }
    }

    private static final String[] byRoundPreFlopColNames_ = {"stats.cards", "stats.numhands", "stats.roundchecked",
            "stats.roundcalled", "stats.roundbet", "stats.roundraised", "stats.roundreraised", "stats.roundfolded",
            "stats.roundwon"};

    private static final String[] byRoundColNames_ = {"stats.cards", "stats.numhands", "stats.roundchecked",
            "stats.roundcheckraised", "stats.roundcalled", "stats.roundbet", "stats.roundraised", "stats.roundreraised",
            "stats.roundfolded", "stats.roundwon"};

    /**
     * Table model backed by HandRoundStatsData from REST API.
     */
    private static class ByRoundModel extends DefaultTableModel {
        private final List<HandRoundStatsData> stats_;
        private final boolean preFlop_;

        public ByRoundModel(List<HandRoundStatsData> stats, boolean preFlop) {
            stats_ = stats;
            preFlop_ = preFlop;
            String[] names = preFlop ? byRoundPreFlopColNames_ : byRoundColNames_;
            for (String name : names) {
                addColumn(name);
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public int getRowCount() {
            return stats_ == null ? 0 : stats_.size();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return Object.class;
            return Number.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            HandRoundStatsData s = stats_.get(row);
            Object v;
            if (preFlop_) {
                v = switch (column) {
                    case 0 -> s.handClass();
                    case 1 -> s.count();
                    case 2 -> formatPct(s.checkedPct());
                    case 3 -> formatPct(s.calledPct());
                    case 4 -> formatPct(s.betPct());
                    case 5 -> formatPct(s.raisedPct());
                    case 6 -> formatPct(s.reraisedPct());
                    case 7 -> formatPct(s.foldedPct());
                    case 8 -> formatPct(s.wonPct());
                    default -> null;
                };
            } else {
                v = switch (column) {
                    case 0 -> s.handClass();
                    case 1 -> s.count();
                    case 2 -> formatPct(s.checkedPct());
                    case 3 -> formatPct(s.checkRaisedPct());
                    case 4 -> formatPct(s.calledPct());
                    case 5 -> formatPct(s.betPct());
                    case 6 -> formatPct(s.raisedPct());
                    case 7 -> formatPct(s.reraisedPct());
                    case 8 -> formatPct(s.foldedPct());
                    case 9 -> formatPct(s.wonPct());
                    default -> null;
                };
            }
            if ("0%".equals(v))
                return null;
            return v;
        }
    }

    @Override
    public boolean processButton(GameButton button) {
        if ("export".equals(button.getName())) {
            Component c = tabs_.getSelectedComponent();

            if (c instanceof ShowDetails) {
                ((ShowDetails) c).exportHistory();
            }
        } else if ("handdetails".equals(button.getName())) {
            Component c = tabs_.getSelectedComponent();

            if (c instanceof ShowDetails) {
                ((ShowDetails) c).showDetails();
            }
        }
        return super.processButton(button);
    }

    private void checkDetailsButton() {
        Component c = tabs_.getSelectedComponent();

        if (c instanceof ShowDetails) {
            buttonbox_.getButton("export").setEnabled(((ShowDetails) c).canShowDetails());
            buttonbox_.getButton("handdetails").setEnabled(((ShowDetails) c).canShowDetails());
        } else {
            buttonbox_.getButton("export").setEnabled(false);
            buttonbox_.getButton("handdetails").setEnabled(false);
        }
    }

    private class ByRoundPanel extends DDTabPanel implements ShowDetails {
        DDTable table_;
        int nRound_;

        public ByRoundPanel(int nRound) {
            super();
            nRound_ = nRound;
        }

        public void refresh() {
            ClientTournamentHistory hist = getSelectedTournament();
            boolean preFlop = (nRound_ == ClientBettingRound.PRE_FLOP.toLegacy());
            List<HandRoundStatsData> stats = List.of();
            PokerGame.WebSocketConfig cfg = getWebSocketConfig(hist);
            if (cfg != null) {
                try {
                    GameServerRestClient restClient = new GameServerRestClient(cfg.port());
                    stats = restClient.getRoundStats(cfg.gameId(), cfg.jwt(), nRound_);
                } catch (Exception ignored) {
                }
            }
            table_.setModel(new ByRoundModel(stats, preFlop));
            table_.setExporter(new TableExporter(context_, ClientBettingRound.getRoundName(nRound_)));
        }

        @Override
        public void ancestorAdded(AncestorEvent event) {
            super.ancestorAdded(event);
            refresh();
        }

        public boolean canShowDetails() {
            return (table_ != null) && table_.getSelectedRow() >= 0;
        }

        public void exportHistory() {
            context_.processPhaseNow("HistoryExportDialog", getDetailsParams());
        }

        public void showDetails() {
            context_.processPhaseNow("HandHistoryDialog", getDetailsParams());
        }

        private TypedHashMap getDetailsParams() {
            TypedHashMap params = new TypedHashMap();
            ClientTournamentHistory hist = getSelectedTournament();
            PokerGame.WebSocketConfig cfg = getWebSocketConfig(hist);
            if (cfg != null) {
                params.setString("gameId", cfg.gameId());
                params.setString("jwt", cfg.jwt());
                params.setInteger("port", cfg.port());
            }
            return params;
        }

        @Override
        public void createUI() {
            DDScrollTable scrollTable = new DDScrollTable("stats", "PokerPrefsPlayerList", "BrushedMetal",
                    (nRound_ == ClientBettingRound.PRE_FLOP.toLegacy()) ? byRoundPreFlopColNames_ : byRoundColNames_,
                    (nRound_ == ClientBettingRound.PRE_FLOP.toLegacy())
                            ? new int[]{30, 30, 40, 40, 40, 40, 40, 40, 40}
                            : new int[]{30, 30, 40, 40, 40, 40, 40, 40, 40, 40});

            scrollTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            add(scrollTable, BorderLayout.CENTER);

            table_ = scrollTable.getDDTable();

            table_.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    checkDetailsButton();
                }
            });
            table_.addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        showDetails();
                    }
                }

                public void mousePressed(MouseEvent e) {
                }

                public void mouseReleased(MouseEvent e) {
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }
            });
            checkDetailsButton();
        }
    }

    /**
     * Used by table to display tournament finishes.
     */
    static class ResultsModel extends DefaultTableModel {
        List<ClientTournamentHistory> finishes;
        String[] names;
        int[] widths;

        public ResultsModel(List<ClientTournamentHistory> finishes, String names[], int[] widths) {
            this.finishes = finishes;
            this.names = names;
            this.widths = widths;
        }

        public void updateFinishes(List<ClientTournamentHistory> f) {
            this.finishes = f;
            fireTableDataChanged();
        }

        public ClientTournamentHistory getTournamentHistory(int r) {
            return finishes.get(r);
        }

        public int getRank(int r) {
            return getTournamentHistory(r).getPlace();
        }

        @Override
        public String getColumnName(int c) {
            return names[c];
        }

        @Override
        public int getColumnCount() {
            return widths.length;
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public int getRowCount() {
            if (finishes == null)
                return 0;
            return finishes.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            ClientTournamentHistory h = getTournamentHistory(rowIndex);

            if (h == null)
                return null;

            String sValue = "[bad column]";

            if (names[colIndex].equals(COL_FINISH)) {
                int finish = getRank(rowIndex);
                if (finish > 0) {
                    sValue = PropertyConfig.getMessage("msg.finishoutof", PropertyConfig.getPlace(finish),
                            h.getNumPlayers());
                } else if (rowIndex == 0) {
                    sValue = PropertyConfig.getMessage("msg.summary");
                } else {
                    sValue = null;
                }
            } else if (names[colIndex].equals(COL_NAME)) {
                sValue = h.getTournamentName();
            } else if (names[colIndex].equals(COL_ENDDATE)) {
                if (h.getPlace() > 0) {
                    GameEngine engine = GameEngine.getGameEngine();
                    String sLocale = null;

                    if (engine != null) {
                        sLocale = engine.getLocale();
                    }

                    sValue = PropertyConfig.getDateFormat("msg.format.shortdatetime", sLocale).format(h.getEndDate());
                } else {
                    sValue = (rowIndex == 0) ? null : PropertyConfig.getMessage("msg.incomplete");
                }
            } else if (names[colIndex].equals(COL_TOTALBUYIN)) {
                sValue = getNumber(h.getTotalSpent(), rowIndex == 0);
            } else if (names[colIndex].equals(COL_PRIZE)) {
                sValue = getNumber(h.getPrize(), rowIndex == 0);
            } else if (names[colIndex].equals(COL_PROFIT)) {
                sValue = getNumber(h.getPrize() - h.getTotalSpent(), rowIndex == 0);
            }

            return sValue;
        }
    }

    private ClientTournamentHistory getSelectedTournament() {
        int selectedTournament = finishTable_.getSelectedRow();

        return ((ResultsModel) finishTable_.getModel()).getTournamentHistory(selectedTournament);
    }

    /**
     * Get the WebSocket config for the current game. Returns null if no active game
     * connection exists.
     */
    private PokerGame.WebSocketConfig getWebSocketConfig(ClientTournamentHistory hist) {
        GameEngine engine = GameEngine.getGameEngine();
        if (engine == null)
            return null;
        GameContext ctx = engine.getDefaultContext();
        if (ctx == null)
            return null;
        PokerGame game = (PokerGame) ctx.getGame();
        if (game == null)
            return null;
        return game.getWebSocketConfig();
    }

    static String formatPct(double pct) {
        if (pct == 0.0)
            return "0%";
        return String.format("%.0f%%", pct);
    }

    private static String getNumber(int nNum, boolean bBold) {
        if (nNum < 0) {
            return "<HTML>" + (bBold ? "<B>" : "") + PropertyConfig.getMessage("msg.chip.net.neg", -1 * nNum)
                    + (bBold ? "</B>" : "");
        } else {
            return "<HTML>" + (bBold ? "<B>" : "") + PropertyConfig.getMessage("msg.chip.net.pos", nNum)
                    + (bBold ? "</B>" : "");
        }
    }

    private class SVLayout extends BorderLayout {
        Component extra;
        ScaleConstraintsFixed scf;

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
            if (constraints instanceof ScaleConstraintsFixed) {
                extra = comp;
                scf = (ScaleConstraintsFixed) constraints;
                return;
            }
            super.addLayoutComponent(comp, constraints);
        }

        @Override
        public void layoutContainer(Container target) {
            if (extra != null) {
                ScaleLayout.layoutScaleConstraintsFixed(scf, extra, target, target.getInsets());
            }
            super.layoutContainer(target);
        }
    }
}
