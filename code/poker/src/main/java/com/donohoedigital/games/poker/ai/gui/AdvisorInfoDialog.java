/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
 * GameInfoDialog.java
 *
 * Created on April 25, 2004, 6:48 PM
 */

package com.donohoedigital.games.poker.ai.gui;

import com.donohoedigital.games.poker.online.ClientPlayer;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.config.GamePhase;
import com.donohoedigital.games.engine.DialogPhase;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.ai.PlayerType;
import com.donohoedigital.games.poker.online.ClientHoldemHand;
import com.donohoedigital.games.poker.online.ClientPokerTable;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.gui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class AdvisorInfoDialog extends DialogPhase {
    static Logger logger = LogManager.getLogger(AdvisorInfoDialog.class);

    // members
    private PokerGame game_;
    private TournamentProfile profile_;
    private DDTabbedPane tab_;
    private DDHtmlArea resultHTML_;

    private ImageComponent ic_ = new ImageComponent("ddlogo20", 1.0d);

    /**
     * Returns the current player from the active hand, or null if any link in the
     * chain (table, hand, player) is missing.
     */
    private ClientPlayer getActivePlayer() {
        ClientPokerTable table = game_.getCurrentTable();
        if (table == null)
            return null;
        ClientHoldemHand hh = table.getHoldemHand();
        if (hh == null)
            return null;
        return hh.getCurrentPlayer();
    }

    /**
     * Init phase, storing engine and gamephase. Called createUI()
     */
    public void init(GameEngine engine, GameContext context, GamePhase gamephase) {
        game_ = (PokerGame) context.getGame();
        profile_ = game_.getProfile();
        if (!game_.isClockMode())
            profile_.setPrizePool(game_.getPrizePool(), true); // update to current
        ic_.setScaleToFit(false);
        ic_.setIconWidth(GamePrefsPanel.ICWIDTH);
        ic_.setIconHeight(GamePrefsPanel.ICHEIGHT); // need to be slightly higher for focus
        super.init(engine, context, gamephase);
    }

    public void finish() {
        super.finish();

        ClientPlayer player = getActivePlayer();
        if (player == null)
            return;
        ClientPokerTable table = game_.getCurrentTable();
        table.firePokerTableEvent(
                new PokerTableEvent(PokerTableEvent.TYPE_PLAYER_AI_CHANGED, table, player, player.getSeat()));
    }

    /**
     * Focus here
     */
    public Component getFocusComponent() {
        return tab_;
    }

    /**
     * create gui
     */
    public JComponent createDialogContents() {
        resultHTML_ = new DDHtmlArea(GuiManager.DEFAULT, "AdvisorSummary");
        resultHTML_.setBorder(BorderFactory.createEmptyBorder());
        DDPanel resultPanel = (DDPanel) GuiUtils.CENTER(resultHTML_);
        resultPanel.setPreferredHeight(40);

        tab_ = new DDTabbedPane(STYLE, null, JTabbedPane.TOP);
        tab_.setOpaque(false);
        tab_.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        StrategyTab strategyTab = new StrategyTab();
        strategyTab.createUI();
        tab_.addTab(PropertyConfig.getMessage("msg.advisortab.strategy"), ic_, strategyTab, null);

        DDPanel base = new DDPanel();

        base.add(resultPanel, BorderLayout.NORTH);
        base.add(tab_, BorderLayout.CENTER);

        updateResult();

        return base;
    }

    private void updateResult() {
        ClientPlayer p = getActivePlayer();
        if (p == null)
            return;
        ClientHoldemHand hhand = game_.getCurrentTable().getHoldemHand();
        resultHTML_.setText("&nbsp;" + p.getHand().toHTML() + "&nbsp;&nbsp;" + hhand.getCommunity().toHTML());
    }

    private class StrategyTab extends DDTabPanel implements ChangeListener {
        PlayerTypeSlidersPanel slidersPanel_;

        public void createUI() {
            ClientPlayer p = getActivePlayer();
            if (p == null)
                return;
            PlayerType playerType = p.getPlayerType();
            if (playerType == null)
                return;
            HandSelectionPanel handSelectionPanel = new HandSelectionPanel(playerType, STYLE);
            slidersPanel_ = new PlayerTypeSlidersPanel(STYLE);
            slidersPanel_.setItems(playerType.getSummaryNodes(false));
            slidersPanel_.setPreferredHeight(320);
            add(handSelectionPanel, BorderLayout.NORTH);
            add(slidersPanel_, BorderLayout.CENTER);

            setPreferredWidth(700);

            repaint();
        }

        public void ancestorAdded(javax.swing.event.AncestorEvent event) {
            HandSelectionPanel.changeListener = this;
            PlayerTypeSlidersPanel.changeListener = this;
        }

        public void ancestorRemoved(javax.swing.event.AncestorEvent event) {
            HandSelectionPanel.changeListener = null;
            PlayerTypeSlidersPanel.changeListener = null;
        }

        public void stateChanged(ChangeEvent e) {
            if ((e != null) && (e.getSource() instanceof DDSlider)) {
                if (((DDSlider) e.getSource()).getValueIsAdjusting())
                    return;
            }

            ClientPlayer p = getActivePlayer();
            if (p == null)
                return;
            PlayerType playerType = p.getPlayerType();
            if (playerType == null)
                return;
            playerType.setName(PropertyConfig.getMessage("msg.advisor.profilename"));
            playerType.save();

            updateResult();
        }
    }
}
