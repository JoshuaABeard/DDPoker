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
package com.donohoedigital.games.poker;

import com.donohoedigital.comms.*;

import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Swing-based game clock that extends javax.swing.Timer for UI updates. Tracks
 * time internally using System.currentTimeMillis() without depending on any
 * engine classes.
 */
@DataCoder('c')
public class GameClock extends Timer implements ActionListener, DataMarshal {
    private static final int ACTION_TICK = 0;
    private static final int ACTION_START = 1;
    private static final int ACTION_STOP = 2;
    private static final int ACTION_SET = 3;

    // Time tracking state (inlined from PureTournamentClock)
    private long millisRemaining_;
    private boolean clockRunning_;
    private long tickBeginTime_;

    // UI state (transient)
    private boolean bFlash_;
    private boolean bPaused_;

    public GameClock() {
        super(1000, null);
        addActionListener(this);
        this.millisRemaining_ = 0;
        this.clockRunning_ = false;
        this.tickBeginTime_ = 0;
    }

    public void setFlash(boolean b) {
        bFlash_ = b;
    }

    public boolean isFlash() {
        return bFlash_;
    }

    public int getSecondsRemaining() {
        return (int) (millisRemaining_ / 1000);
    }

    public boolean isExpired() {
        return millisRemaining_ <= 0;
    }

    public synchronized void setSecondsRemaining(int n) {
        tickBeginTime_ = System.currentTimeMillis();
        millisRemaining_ = n * 1000L;
        this.fireActionPerformed(new ActionEvent(this, ACTION_SET, null, System.currentTimeMillis(), 0));
    }

    public void pause() {
        bPaused_ = true;
        stop();
    }

    public void unpause() {
        bPaused_ = false;
        start();
    }

    public boolean isPaused() {
        return bPaused_;
    }

    public void start() {
        if (!isRunning()) {
            if (!clockRunning_) {
                tickBeginTime_ = System.currentTimeMillis();
                clockRunning_ = true;
            }
            super.start(); // Start Swing Timer for UI ticks
            this.fireActionPerformed(new ActionEvent(this, ACTION_START, null, System.currentTimeMillis(), 0));
        }
    }

    public void stop() {
        if (isRunning()) {
            tickClock(); // Update time before stopping
            clockRunning_ = false;
            super.stop(); // Stop Swing Timer
            this.fireActionPerformed(new ActionEvent(this, ACTION_STOP, null, System.currentTimeMillis(), 0));
        }
    }

    public void toggle() {
        if (isRunning()) {
            stop();
        } else {
            start();
        }
    }

    public synchronized void actionPerformed(ActionEvent e) {
        if (e.getID() == ACTION_TICK) {
            tickClock();

            // Stop clock if expired (fires ACTION_STOP event to listeners)
            if (millisRemaining_ <= 0) {
                stop();
            }
        }

        Object[] listeners = listenerList.getListenerList();

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == GameClockListener.class) {
                switch (e.getID()) {
                    case ACTION_SET :
                        ((GameClockListener) listeners[i + 1]).gameClockSet(this);
                        break;
                    case ACTION_START :
                        ((GameClockListener) listeners[i + 1]).gameClockStarted(this);
                        break;
                    case ACTION_STOP :
                        ((GameClockListener) listeners[i + 1]).gameClockStopped(this);
                        break;
                    case ACTION_TICK :
                        ((GameClockListener) listeners[i + 1]).gameClockTicked(this);
                        break;
                }
            }
        }

    }

    public void addGameClockListener(GameClockListener listener) {
        listenerList.add(GameClockListener.class, listener);
    }

    public void removeGameClockListener(GameClockListener listener) {
        listenerList.remove(GameClockListener.class, listener);
    }

    public void demarshal(MsgState state, String sData) {
        TokenizedList list = new TokenizedList();
        list.demarshal(state, sData);
        long millisRemaining = list.removeLongToken();
        boolean wasRunning = list.removeBooleanToken();

        // Set time (preserves millisecond precision)
        tickBeginTime_ = System.currentTimeMillis();
        millisRemaining_ = millisRemaining;

        // Restore running state
        if (wasRunning) {
            start();
        }
    }

    public String marshal(MsgState state) {
        TokenizedList list = new TokenizedList();
        list.addToken(millisRemaining_);
        list.addToken(isRunning());
        return list.marshal(state);
    }

    /** Update remaining time based on wall clock elapsed since last tick. */
    private void tickClock() {
        if (!clockRunning_)
            return;
        long now = System.currentTimeMillis();
        long elapsed = now - tickBeginTime_;
        if (elapsed >= millisRemaining_) {
            millisRemaining_ = 0;
            clockRunning_ = false;
        } else {
            millisRemaining_ -= elapsed;
        }
        tickBeginTime_ = now;
    }

}
