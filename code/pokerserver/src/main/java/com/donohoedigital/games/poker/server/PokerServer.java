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
 * PokerServer.java
 *
 * Created on August 13, 2003, 3:47 PM
 */

package com.donohoedigital.games.poker.server;

import com.donohoedigital.base.Utils;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.server.EngineServer;
import com.donohoedigital.udp.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.donohoedigital.config.DebugConfig.TESTING;

/**
 *
 * @author  donohoe
 */
public class PokerServer extends EngineServer implements UDPLinkHandler, UDPManagerMonitor
{
    // udp server for test connections and chat
    private UDPServer udp_;
    private ChatServer chat_;

    @Autowired
    private OnlineProfileService onlineProfileService;

    /**
     * Initialize, start UDP and run
     */
    @Override
    public void init()
    {
        super.init();
        initializeAdminProfile();
        udp_.manager().addMonitor(this);
        udp_.start();
        start();
    }

    /**
     * Initialize or update admin profile based on environment variables
     */
    void initializeAdminProfile()
    {
        String adminUsername = PropertyConfig.getStringProperty("settings.admin.user", null, false);
        String adminPassword = PropertyConfig.getStringProperty("settings.admin.password", null, false);
        boolean passwordExplicitlySet = adminPassword != null;

        // If no admin username provided, default to "admin"
        if (adminUsername == null)
        {
            adminUsername = "admin";
        }

        OnlineProfile profile = onlineProfileService.getOnlineProfileByName(adminUsername);

        if (profile == null)
        {
            // Create new admin profile
            // Generate password only if not explicitly set
            if (adminPassword == null)
            {
                adminPassword = onlineProfileService.generatePassword();
                logger.warn("Admin credentials not configured, using defaults:");
                logger.warn("  Username: {}", adminUsername);
                logger.warn("  Password: {}", adminPassword);
                logger.warn("  Set ADMIN_USERNAME and ADMIN_PASSWORD environment variables to customize");
            }

            profile = new OnlineProfile();
            profile.setName(adminUsername);
            profile.setPassword(adminPassword);
            profile.setEmail("admin@localhost");
            profile.setUuid(UUID.randomUUID().toString());
            profile.setActivated(true);
            profile.setRetired(false);
            profile.setLicenseKey("0000-0000-0000-0000");

            if (onlineProfileService.saveOnlineProfile(profile))
            {
                logger.info("Admin profile created: {}", adminUsername);
            }
            else
            {
                logger.error("Failed to create admin profile: {}", adminUsername);
            }
        }
        else
        {
            // Update existing admin profile only if password was explicitly set
            if (passwordExplicitlySet)
            {
                profile.setPassword(adminPassword);
                profile.setActivated(true);
                profile.setRetired(false);

                onlineProfileService.updateOnlineProfile(profile);
                logger.info("Admin profile password updated: {}", adminUsername);
            }
            else
            {
                logger.info("Admin profile exists, keeping existing password: {}", adminUsername);
            }
        }
    }

    /**
     * Set UDP
     */
    public void setUDPServer(UDPServer udp)
    {
        udp_ = udp;
    }

    /**
     * Get UDP
     */
    public UDPServer getUDPServer()
    {
        return udp_;
    }

    /**
     * Set chat server
     */
    public void setChatServer(ChatServer chat)
    {
        chat_ = chat;
    }

    ////
    //// UDPManagerMonitor
    ////

    public void monitorEvent(UDPManagerEvent event)
    {
        UDPLink link = event.getLink();
        if (chat_.isChat(link)) chat_.monitorEvent(event);
        else switch(event.getType())
        {
            case CREATED:
                if (TESTING(UDPServer.TESTING_UDP))
                    logger.debug("PublicTest Created: {}", Utils.getAddressPort(link.getRemoteIP()));
                break;

            case DESTROYED:
                if (TESTING(UDPServer.TESTING_UDP))
                    logger.debug("PublicTest Destroyed: {}", Utils.getAddressPort(link.getRemoteIP()));
                break;
        }
        //logger.debug("Event: "+ event + " on " + Utils.getAddressPort(event.getLink().getLocalIP()));
    }

    ////
    //// UDPLinkHandler interface
    ////

    public int getTimeout(UDPLink link)
    {
        if (chat_.isChat(link)) return chat_.getTimeout(link);
        return UDPLink.DEFAULT_TIMEOUT; // Timeout for TestConnection
    }

    public int getPossibleTimeoutNotificationInterval(UDPLink link)
    {
        if (chat_.isChat(link)) return chat_.getPossibleTimeoutNotificationInterval(link);
        return getTimeout(link);
    }

    public int getPossibleTimeoutNotificationStart(UDPLink link)
    {
        if (chat_.isChat(link)) return chat_.getPossibleTimeoutNotificationStart(link);
        return getTimeout(link);
    }
}
