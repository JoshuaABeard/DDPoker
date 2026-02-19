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
 * GetPublicIP.java
 *
 * Created on November 17, 2004, 7:16 PM
 */

package com.donohoedigital.games.poker.online;

import com.donohoedigital.comms.DDMessageListener;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.comms.*;
import com.donohoedigital.games.engine.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Get public IP address for P2P game hosting. Fetches IP directly from external
 * services (ipify.org, etc.) rather than querying the game server, which
 * correctly handles NAT/router scenarios.
 *
 * @author Doug Donohoe
 */
public class GetPublicIP extends SendMessageDialog {
    private static final Logger logger = LogManager.getLogger(GetPublicIP.class);

    public static final String PARAM_TEST_SERVER = "testServer";

    /**
     * Indicates using this test to verify server connectivity for GamePrefsPanel
     */
    private boolean testServer_;

    /**
     * message to send to server
     */
    protected EngineMessage getMessage() {
        testServer_ = gamephase_.getBoolean(PARAM_TEST_SERVER, false);
        return new EngineMessage(EngineMessage.GAME_NOTDEFINED, EngineMessage.PLAYER_NOTDEFINED,
                EngineMessage.CAT_PUBLIC_IP);
    }

    /**
     * Message to display to user
     */
    protected String getMessageKey() {
        return "msg.testConnection";
    }

    /**
     * Override to change done step message
     */
    @Override
    public void updateStep(int nStep) {
        if (nStep == DDMessageListener.STEP_DONE && testServer_) {
            setStatusText(PropertyConfig.getMessage("msg.p2p.testserver.done"));
        } else
            super.updateStep(nStep);
    }

    /**
     * Don't do server redirect query
     */
    protected boolean doServerQuery() {
        return false;
    }

    protected boolean isAutoClose() {
        return !testServer_;
    }

    /**
     * Override start() to fetch public IP from external service directly instead of
     * querying the game server. This correctly handles NAT/router scenarios where
     * the server would only see the client's private IP. Falls back to server query
     * if external services fail.
     */
    @Override
    public void start() {
        // Initialize parent (sets up UI if not faceless)
        if (!isFaceless()) {
            super.start();
        }

        testServer_ = gamephase_.getBoolean(PARAM_TEST_SERVER, false);

        // Update status to show we're connecting
        updateStep(DDMessageListener.STEP_CONNECTING);

        // Try to fetch public IP from external service
        logger.info("Fetching public IP from external service");
        PublicIPDetector detector = new PublicIPDetector();
        String publicIP = detector.fetchPublicIP();

        if (publicIP != null) {
            // Success - create result message with public IP
            logger.info("Successfully detected public IP: {}", publicIP);
            updateStep(DDMessageListener.STEP_DONE);

            EngineMessage result = new EngineMessage(EngineMessage.GAME_NOTDEFINED, EngineMessage.PLAYER_SERVER,
                    EngineMessage.CAT_PUBLIC_IP);
            result.setString(EngineMessage.PARAM_IP, publicIP);

            // Simulate successful server response
            messageReceived(result);
        } else {
            // Detection failed — no server-query fallback in community edition
            logger.warn("Failed to fetch public IP from external services");
            updateStep(DDMessageListener.STEP_DONE);
        }
    }
}
