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
package com.donohoedigital.games.poker.dashboard;

import com.donohoedigital.config.*;
import com.donohoedigital.games.engine.*;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import com.donohoedigital.games.poker.event.*;
import com.donohoedigital.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DashboardAdvisor extends DashboardItem {
    JScrollPane scroll_;
    DDHtmlArea htmlAdvice_;
    String title_;
    String advice_;

    public static final String NOADVICE = PropertyConfig.getMessage("msg.advisor.noadvice");
    public static final String NOADVICETITLE = PropertyConfig.getMessage("msg.advisor.noadvice.title");

    // Static state readable by the dev control server (StateHandler) without
    // navigating the UI tree.
    private static volatile String currentAdvice_ = null;
    private static volatile String currentTitle_ = null;

    /**
     * Returns the most recently computed advice text, or null if not yet available.
     */
    public static String getCurrentAdvice() {
        return currentAdvice_;
    }

    /**
     * Returns the most recently computed advice title, or null if not yet
     * available.
     */
    public static String getCurrentTitle() {
        return currentTitle_;
    }

    /**
     * Sets the current advice from an external source (e.g. server ADVISOR_UPDATE).
     */
    public static void setCurrentAdvice(String advice, String title) {
        currentAdvice_ = advice;
        currentTitle_ = title;
    }

    private DDPanel buttons_;

    public DashboardAdvisor(GameContext context) {
        super(context, "advisor");
        setDynamicTitle(true);
        trackTableEvents(PokerTableEvent.TYPE_NEW_HAND | PokerTableEvent.TYPE_CURRENT_PLAYER_CHANGED
                | PokerTableEvent.TYPE_DEALER_ACTION | PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED | // could change pot
                                                                                                    // odds, for example
                PokerTableEvent.TYPE_BUTTON_MOVED | PokerTableEvent.TYPE_PLAYER_AI_CHANGED);
    }

    @Override
    protected Object getDynamicTitleParam() {
        return title_;
    }

    @Override
    protected JComponent createBody() {
        DDPanel base = new DDPanel();

        DDButton actButton_ = new GlassButton("aidoit", "Glass");
        actButton_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Phase phase = context_.getCurrentPhase();

                if (phase instanceof Bet) {
                    // Practice mode: Bet phase handles AI action directly
                    ((Bet) phase).doAI();
                }
            }
        });

        DDButton whyButton_ = new GlassButton("tellmewhy", "Glass");
        whyButton_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                context_.processPhaseNow("AdvisorInfoDialog", null);
            }
        });

        htmlAdvice_ = new DDHtmlArea(GuiManager.DEFAULT, STYLE);
        htmlAdvice_.setPreferredSize(new Dimension(100, 100));
        htmlAdvice_.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        scroll_ = new DDScrollPane(htmlAdvice_, "ChatInGame", null, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll_.setOpaque(false);

        base.add(scroll_, BorderLayout.CENTER);

        buttons_ = new DDPanel();
        buttons_.setVisible(false);
        buttons_.setLayout(new GridLayout(1, 2, 8, 0));
        buttons_.add(whyButton_);
        buttons_.add(actButton_);
        base.add(GuiUtils.CENTER(buttons_), BorderLayout.SOUTH);

        return base;
    }

    @Override
    protected void updateInfo() {
        advice_ = NOADVICE;
        title_ = NOADVICETITLE;

        // Check if server-pushed advice is available
        String serverAdvice = currentAdvice_;
        String serverTitle = currentTitle_;
        if (serverAdvice != null && !NOADVICE.equals(serverAdvice)) {
            advice_ = serverAdvice;
            title_ = serverTitle;
        }

        htmlAdvice_.setText(advice_);
        buttons_.setVisible(!NOADVICE.equals(advice_));
    }

    @Override
    public int getPreferredBodyHeight() {
        return 100;
    }
}
