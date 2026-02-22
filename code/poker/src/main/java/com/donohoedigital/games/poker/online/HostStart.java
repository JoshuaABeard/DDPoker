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
import com.donohoedigital.games.poker.engine.*;
import org.apache.logging.log4j.*;

import java.awt.event.*;

/**
 * Phase that runs after the host clicks "Start!" in the Lobby.
 *
 * <p>
 * Performs a countdown (driven by {@link PokerConstants#OPTION_ONLINESTART}),
 * then transitions to the {@code InitializeOnlineGame} phase.
 */
public class HostStart extends ChainPhase implements ActionListener {

    static Logger logger = LogManager.getLogger(HostStart.class);

    private static final int ONE_SEC = 1000;

    private PokerGame game_;
    private javax.swing.Timer timer_;
    private int DELAY;
    private int DELAY_SECS;
    private int elapsed_;

    @Override
    public void start() {
        process();
        // do NOT call nextPhase() here — the timer callback does it
    }

    @Override
    public void process() {
        game_ = (PokerGame) context_.getGame();

        // countdown delay from options
        DELAY = PokerUtils.getIntOption(PokerConstants.OPTION_ONLINESTART);
        DELAY_SECS = ONE_SEC * DELAY;
        elapsed_ = 0;

        logger.info("Starting online game, countdown: {}s", DELAY);

        timer_ = new javax.swing.Timer(ONE_SEC, this);
        timer_.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        elapsed_ += ONE_SEC;
        int remaining = DELAY - (elapsed_ / ONE_SEC);

        if (remaining > 0) {
            logger.debug("Game starting in {}s...", remaining);
        } else {
            beginGame();
        }
    }

    private void beginGame() {
        timer_.stop();
        timer_ = null;
        nextPhase();
    }
}
