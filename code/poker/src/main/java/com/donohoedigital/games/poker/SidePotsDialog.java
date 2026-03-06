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
 * SidePotsDialog.java
 *
 * Created on April 28, 2004, 3:53 PM
 */

package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.online.ClientPlayer;
import com.donohoedigital.base.*;
import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.online.ClientHoldemHand;
import com.donohoedigital.games.poker.online.ClientPokerTable;
import com.donohoedigital.games.poker.online.ClientPot;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 *
 * @author donohoe
 */
public class SidePotsDialog extends DialogPhase {
    // static Logger logger = LogManager.getLogger(SidePotsDialog.class);

    private JScrollPane scroll_;
    private DDHtmlArea html_;
    private PokerGame game_;

    /**
     * Create dialog
     */
    public JComponent createDialogContents() {
        // init
        DDPanel base = new DDPanel();
        base.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // html area
        DDLabelBorder border = new DDLabelBorder("sidepots", STYLE);
        border.setPreferredSize(new Dimension(250, 350));
        base.add(border, BorderLayout.CENTER);
        html_ = new DDHtmlArea(GuiManager.DEFAULT, "SidePots");
        html_.setDisplayOnly(true);
        html_.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
        scroll_ = new DDScrollPane(html_, STYLE, null, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll_.setOpaque(false);
        border.add(scroll_, BorderLayout.CENTER);

        return base;
    }

    public void start() {
        game_ = (PokerGame) context_.getGame();
        setPots();
        super.start();
    }

    /**
     * Set pool html based on settings
     */
    private void setPots() {
        nSide = 0;
        ClientPokerTable currentTable = game_.getCurrentTable();
        ClientHoldemHand hhand = currentTable != null ? currentTable.getHoldemHand() : null;
        if (hhand == null)
            return;
        int nNum = hhand.getNumPots();
        StringBuilder sb = new StringBuilder();
        for (int i = nNum - 1; i >= 0; i--) {
            sb.append(getPot(currentTable, hhand, i));
        }

        html_.setText(PropertyConfig.getMessage("msg.sidepots", sb.toString()));
        scroll_.revalidate();
        html_.setCaretPosition(0); // scroll to top

    }

    // main
    private int nSide = 0;

    /**
     * get side pot description
     */
    private String getPot(ClientPokerTable table, ClientHoldemHand hhand, int nPot) {
        ClientPot p = hhand.getPot(nPot);
        if (p == null)
            return "";

        List<Integer> winnerIds = p.winnerPlayerIds();
        List<Integer> eligibleIds = p.eligiblePlayerIds();
        int nNum = eligibleIds.size();

        String sKey;
        if (nNum == 1) {
            sKey = "msg.sidepot.overbet";
        } else if (nPot == 0) {
            sKey = "msg.sidepot.main";
        } else {
            sKey = "msg.sidepot.side";
            nSide++;
        }

        // Resolve players from seat indices
        List<ClientPlayer> players = new ArrayList<>();
        for (int seatId : eligibleIds) {
            ClientPlayer pl = table.getPlayer(seatId);
            if (pl != null)
                players.add(pl);
        }
        players.sort(ClientPlayer.SORTBYNAME);

        StringBuilder sb = new StringBuilder();
        for (ClientPlayer pl : players) {
            if (pl.isFolded())
                continue;
            boolean bWinner = winnerIds.contains(pl.getSeat());
            if (sb.length() > 0)
                sb.append("<BR>");
            sb.append(PropertyConfig.getMessage(bWinner ? "msg.sidepot.player.win" : "msg.sidepot.player",
                    Utils.encodeHTML(pl.getName())));
        }

        return PropertyConfig.getMessage("msg.sidepot", PropertyConfig.getMessage(sKey, nSide, p.chips()),
                sb.toString());
    }
}
