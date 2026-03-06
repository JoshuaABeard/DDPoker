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

import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.server.*;
import com.donohoedigital.games.poker.protocol.dto.SimulationResult;
import com.donohoedigital.games.poker.online.ClientHoldemHand;
import com.donohoedigital.games.poker.online.ClientPokerTable;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class PokerShowdownPanel extends DDTabPanel implements DDProgressFeedback, ChangeListener {
    static Logger logger = LogManager.getLogger(PokerShowdownPanel.class);

    private static Dimension resultsSize = new Dimension(90, 50);

    private GameContext context_;
    private ClientPokerTable table_;
    private DDProgressBar progress_;
    private String STYLE;
    private SimulatorDialog sim_;
    private OptionInteger numOpponents_, numSims_;
    private DDRadioButton allcombo_, simcombo_;
    private List<DDLabelBorder> opponents_ = new ArrayList<DDLabelBorder>();
    private boolean bStopRequested_ = false;
    private GlassButton run_, stop_;

    /**
     * Create showdown panel
     */
    public PokerShowdownPanel(GameContext context, SimulatorDialog sim, ClientPokerTable table, String sStyle) {
        super();
        context_ = context;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
        sim_ = sim;
        table_ = table;
        STYLE = sStyle;
        setPreferredSize(new Dimension(420, 355));
    }

    /**
     * create UI upon demand
     */
    @Override
    protected void createUI() {
        setBorderLayoutGap(5, 0);

        // top
        DDPanel top = new DDPanel();
        top.setBorderLayoutGap(5, 0);
        add(top, BorderLayout.NORTH);

        // html
        DDPanel toptop = new DDPanel();
        toptop.setBorderLayoutGap(7, 0);
        top.add(toptop, BorderLayout.NORTH);

        DDHtmlArea test = new DDHtmlArea(GuiManager.DEFAULT, "PokerStatsHeader");
        toptop.add(test, BorderLayout.NORTH);
        test.setBorder(BorderFactory.createEmptyBorder());
        test.setText(PropertyConfig.getMessage("msg.sim.showdown"));

        // control parts
        DDPanel controlbase = new DDPanel();
        toptop.add(controlbase, BorderLayout.CENTER);
        controlbase.setLayout(new HorizontalFlowLayout(HorizontalFlowLayout.LEFT, 5, 0, HorizontalFlowLayout.CENTER));

        TypedHashMap dummy = new TypedHashMap();
        numOpponents_ = new OptionInteger(null, "numopp", STYLE, dummy, null, 1, 9, -1, true);
        numOpponents_.addChangeListener(this);
        numOpponents_.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (numOpponents_.getSpinner().isValidData()) {
                    updateNumOpponents();
                }
            }
        });
        controlbase.add(numOpponents_);

        DDPanel radios = new DDPanel();
        radios.setLayout(new GridLayout(2, 1, 0, -3));
        controlbase.add(radios);

        DDPanel simbase = new DDPanel();
        ButtonGroup group = new ButtonGroup();
        radios.add(simbase);
        simcombo_ = new DDRadioButton("simulate", STYLE);
        simcombo_.setSelected(true);
        group.add(simcombo_);
        simbase.add(simcombo_, BorderLayout.WEST);
        // listener to control # sims
        ActionListener comboListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                numSims_.setEnabled(simcombo_.isSelected());
            }
        };

        allcombo_ = new DDRadioButton("allcombos", STYLE);
        group.add(allcombo_);
        radios.add(allcombo_);

        simcombo_.addActionListener(comboListener);
        allcombo_.addActionListener(comboListener);

        int nMax = 10000000;
        numSims_ = new OptionInteger(null, "hands", STYLE, dummy, null, 25000, nMax, -1, true);
        numSims_.getSpinner().setValue(nMax);
        numSims_.getSpinner().setUseBigStep(true);
        numSims_.getSpinner().resetPreferredSize();
        numSims_.resetToDefault();
        numSims_.setEnabled(true);
        numSims_.addChangeListener(this);
        simbase.add(numSims_, BorderLayout.CENTER);

        // progress bar
        progress_ = new DDProgressBar(GuiManager.DEFAULT, "PokerStats", false);
        progress_.setProgressFeedback(this);

        DDPanel pb = new DDPanel();
        pb.setBorderLayoutGap(0, 5);

        run_ = new GlassButton("run", "Glass");
        run_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sim_.bSimRunning_ = true;
                bStopRequested_ = false;
                clearResults();
                run_.setEnabled(false);
                numSims_.setEnabled(false);
                numOpponents_.setEnabled(false);
                allcombo_.setEnabled(false);
                simcombo_.setEnabled(false);
                stop_.setEnabled(true);
                if (simcombo_.isSelected()) {
                    runSimulator();
                } else {
                    runIterator();
                }
            }
        });
        stop_ = new GlassButton("stop", "Glass");
        stop_.setEnabled(false);
        stop_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStopRequested();
            }
        });

        pb.add(run_, BorderLayout.WEST);
        pb.add(progress_, BorderLayout.CENTER);
        pb.add(stop_, BorderLayout.EAST);

        top.add(pb, BorderLayout.CENTER);

        // opponent grid
        DDPanel opponents = new DDPanel();
        add(GuiUtils.CENTER(opponents), BorderLayout.CENTER);
        opponents.setLayout(new GridLayout(2, 5, 10, 10));
        SimulatorDialog.SimHandPanel cards;

        for (int i = 0; i < PokerConstants.SEATS; i++) {
            cards = new SimulatorDialog.SimHandPanel(sim_, table_, table_.getPlayer(i).getHand());
            DDLabelBorder boardcards = new ShowBorder(i == 0 ? "myhand" : "opponent", cards, i);
            opponents.add(boardcards);
            opponents_.add(boardcards);
        }

        // update enabled
        updateNumOpponents();
    }

    /**
     * num opponents or num sims changed
     */
    public void stateChanged(ChangeEvent e) {
        if (!stop_.isEnabled()) {
            run_.setEnabled(numSims_.getSpinner().isValidData() && numOpponents_.getSpinner().isValidData());
        }
    }

    /**
     * labelborder for each hand
     */
    private class ShowBorder extends DDLabelBorder {
        SimulatorDialog.SimHandPanel cards;
        DDHtmlArea results;

        ShowBorder(String sName, SimulatorDialog.SimHandPanel cards, int i) {
            super(sName, STYLE);
            this.cards = cards;
            setBorderLayoutGap(5, 0);
            if (i > 0)
                setText(PropertyConfig.formatMessage(getText(), i));
            add(GuiUtils.CENTER(cards), BorderLayout.NORTH);

            results = new DDHtmlArea(GuiManager.DEFAULT, "PokerStats");
            results.setPreferredSize(resultsSize);
            results.setBorder(BorderFactory.createEmptyBorder());
            add(results, BorderLayout.CENTER);
        }

        @Override
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            cards.setEnabled(b);
        }

        public void setResults(String s) {
            results.setText(s);
        }
    }

    /**
     * set num players
     */
    public void setNumOpponents(int nNum) {
        numOpponents_.getSpinner().setValue(nNum);
    }

    /**
     * update number of opponents by enabling cards
     */
    private void updateNumOpponents() {
        int nNum = numOpponents_.getSpinner().getValue();
        ShowBorder show;
        for (int i = 0; i < PokerConstants.SEATS; i++) {
            show = (ShowBorder) opponents_.get(i);
            show.setEnabled(i <= nNum);
        }
        updateDisplay(true);
    }

    /**
     * update display - results clearing if requested
     */
    public void updateDisplay(boolean bClearResults) {
        if (allcombo_ == null)
            return; // UI not yet created
        if (bClearResults)
            clearResults();
        allcombo_.setText(PropertyConfig.getMessage("msg.allcombos.rest"));
        simcombo_.setSelected(true);
        numSims_.setEnabled(true);
    }

    /**
     * clear results in all hands
     */
    private void clearResults() {
        progress_.setPercentDone(0);
        for (int i = 0; i < PokerConstants.SEATS; i++) {
            setResults(i, "");
        }
    }

    /**
     * set results for hand i
     */
    private void setResults(int i, String s) {
        ShowBorder show = (ShowBorder) opponents_.get(i);
        show.setResults(s);
    }

    /**
     * run simulator
     */
    private void runSimulator() {
        new UpdateThread(true).start();
    }

    /**
     * run exhaustive enumeration via server
     */
    private void runIterator() {
        new UpdateThread(false).start();
    }

    /**
     * stop requested on simulator panel
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
        allcombo_.setEnabled(true);
        simcombo_.setEnabled(true);
        numSims_.setEnabled(simcombo_.isSelected());
        numOpponents_.setEnabled(true);

        setIntermediateResult(oResult);
    }

    /**
     * set results for what we have so far
     */
    public void setIntermediateResult(Object oResult) {
        if (oResult == null)
            return;

        final StatResult[] stats = (StatResult[]) oResult;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                StatResult stat;
                for (int i = 0; i < stats.length; i++) {
                    stat = stats[i];
                    if (stat == null)
                        continue;
                    setResults(i, stat.toHTML("msg.showdown.results"));
                }
            }
        });
    }

    /**
     * thread to run simulator via server REST API
     */
    private class UpdateThread extends Thread {
        private boolean bSimulate;

        public UpdateThread(boolean bSim) {
            super("UpdateThread");
            bSimulate = bSim;
        }

        @Override
        public void run() {
            ClientHoldemHand hhand = sim_.hhand_;
            ClientPokerTable table = sim_.table_;

            int numPlayers = numOpponents_.getSpinner().getValue() + 1;

            // Player 0 hole cards (non-blank)
            List<String> holeCards = new ArrayList<>();
            Hand playerHand = table.getPlayer(0).getHand();
            for (int i = 0; i < playerHand.size(); i++) {
                Card c = playerHand.getCard(i);
                if (!c.isBlank())
                    holeCards.add(c.toStringSingle());
            }

            // Community cards (non-blank)
            List<String> communityCards = new ArrayList<>();
            Hand community = hhand.getCommunity();
            for (int i = 0; i < community.size(); i++) {
                Card c = community.getCard(i);
                if (!c.isBlank())
                    communityCards.add(c.toStringSingle());
            }

            // Known opponent hands (non-blank pairs only)
            List<List<String>> knownOpponentHands = new ArrayList<>();
            for (int i = 1; i < numPlayers; i++) {
                Hand oppHand = table.getPlayer(i).getHand();
                List<String> oppCards = new ArrayList<>();
                for (int j = 0; j < oppHand.size(); j++) {
                    Card c = oppHand.getCard(j);
                    if (!c.isBlank())
                        oppCards.add(c.toStringSingle());
                }
                // Only include as known if both cards are specified
                if (oppCards.size() == 2) {
                    knownOpponentHands.add(oppCards);
                }
            }

            if (holeCards.size() < 2) {
                progress_.setFinalResult(null);
                return;
            }

            // Set indeterminate progress while server runs
            SwingUtilities.invokeLater(() -> progress_.getProgressBar().setIndeterminate(true));
            SwingUtilities.invokeLater(() -> stop_.setEnabled(false));

            try {
                PokerMain pokerMain = (PokerMain) GameEngine.getGameEngine();
                GameServerRestClient restClient = new GameServerRestClient(pokerMain.getEmbeddedServer().getPort());

                int numOpponents = numPlayers - 1;
                Integer iterations = bSimulate ? numSims_.getSpinner().getValue() : null;
                Boolean exhaustive = bSimulate ? null : Boolean.TRUE;

                SimulationResult result = restClient.simulate(holeCards, communityCards, numOpponents, iterations,
                        knownOpponentHands, exhaustive);

                // Build StatResult[] from server response
                // result.win/tie/loss = player 0's equity
                // result.opponentResults().get(i) = head-to-head vs opponent i
                StatResult[] stats = new StatResult[numPlayers];
                stats[0] = toStatResult(result.win(), result.tie(), result.loss());

                List<SimulationResult.OpponentResult> oppResults = result.opponentResults();
                for (int i = 1; i < numPlayers; i++) {
                    int oppIdx = i - 1;
                    if (oppResults != null && oppIdx < oppResults.size()) {
                        SimulationResult.OpponentResult opp = oppResults.get(oppIdx);
                        // opp.win() = opponent beats hero, opp.loss() = hero beats opponent
                        stats[i] = toStatResult(opp.win(), opp.tie(), opp.loss());
                    } else {
                        stats[i] = null;
                    }
                }

                progress_.setFinalResult(stats);
            } catch (GameServerRestClient.GameServerClientException e) {
                logger.warn("Simulation REST call failed: {}", e.getMessage());
                progress_.setFinalResult(null);
            } finally {
                SwingUtilities.invokeLater(() -> progress_.getProgressBar().setIndeterminate(false));
                SwingUtilities.invokeLater(() -> stop_.setEnabled(true));
            }
        }
    }

    /**
     * Build a StatResult from win/tie/loss percentages (0-100). Scale to integer
     * counts out of 10000 so calculated percentages match.
     */
    private static StatResult toStatResult(double winPct, double tiePct, double lossPct) {
        int win = (int) Math.round(winPct * 100);
        int tie = (int) Math.round(tiePct * 100);
        int lose = (int) Math.round(lossPct * 100);
        return new StatResult("", win, lose, tie);
    }
}
