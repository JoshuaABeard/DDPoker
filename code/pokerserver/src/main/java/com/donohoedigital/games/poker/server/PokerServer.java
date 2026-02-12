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

import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import com.donohoedigital.games.server.EngineServer;
import com.donohoedigital.games.server.service.BannedKeyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 *
 * @author donohoe
 */
public class PokerServer extends EngineServer {
    private static final Logger logger = LogManager.getLogger(PokerServer.class);
    private static final String ADMIN_PASSWORD_FILE = "admin-password.txt";

    // TCP chat server
    private TcpChatServer tcpChatServer_;

    @Autowired
    private OnlineProfileService onlineProfileService;

    @Autowired
    private BannedKeyService bannedKeyService;

    /**
     * Initialize, start TCP chat server and run
     */
    @Override
    public void init() {
        super.init();
        initializeAdminProfile();
        if (tcpChatServer_ != null) {
            tcpChatServer_.init();
            tcpChatServer_.start();
        }
        start();
    }

    /**
     * Initialize or update admin profile based on environment variables
     */
    void initializeAdminProfile() {
        String adminUsername = PropertyConfig.getStringProperty("settings.admin.user", null, false);
        String adminPassword = PropertyConfig.getStringProperty("settings.admin.password", null, false);
        boolean passwordExplicitlySet = adminPassword != null;

        // If no admin username provided, default to "admin"
        if (adminUsername == null) {
            adminUsername = "admin";
        }

        OnlineProfile profile = onlineProfileService.getOnlineProfileByName(adminUsername);

        if (profile == null) {
            // Create new admin profile
            // Generate password only if not explicitly set
            if (adminPassword == null) {
                adminPassword = onlineProfileService.generatePassword();
                logger.warn("Admin credentials not configured, using defaults:");
                logger.warn("  Username: {}", adminUsername);
                logger.warn("  Password: {}", adminPassword);
                logger.warn("  Set ADMIN_USERNAME and ADMIN_PASSWORD environment variables to customize");
            }

            profile = new OnlineProfile();
            profile.setName(adminUsername);
            profile.setEmail("admin@localhost");
            profile.setUuid(UUID.randomUUID().toString());
            profile.setActivated(true);
            profile.setRetired(false);
            profile.setLicenseKey("0000-0000-0000-0000");
            onlineProfileService.hashAndSetPassword(profile, adminPassword);

            if (onlineProfileService.saveOnlineProfile(profile)) {
                writeAdminPasswordFile(adminPassword);
                logger.info("Admin profile created: {}", adminUsername);
            } else {
                logger.error("Failed to create admin profile: {}", adminUsername);
            }
        } else {
            // Update existing admin profile only if password was explicitly set
            if (passwordExplicitlySet) {
                onlineProfileService.hashAndSetPassword(profile, adminPassword);
                profile.setActivated(true);
                profile.setRetired(false);

                onlineProfileService.updateOnlineProfile(profile);
                writeAdminPasswordFile(adminPassword);
                logger.info("Admin profile password updated: {}", adminUsername);
            } else {
                // Profile exists, no explicit password - read from persisted file
                String savedPassword = readAdminPasswordFile();
                if (savedPassword != null) {
                    logger.info("Admin profile exists, keeping existing password: {}", adminUsername);
                    logger.warn("Admin credentials (from {}):", ADMIN_PASSWORD_FILE);
                    logger.warn("  Username: {}", adminUsername);
                    logger.warn("  Password: {}", savedPassword);
                    logger.warn("  Set ADMIN_PASSWORD environment variable to customize");
                } else {
                    // File missing (e.g., volume was wiped) - regenerate
                    adminPassword = onlineProfileService.generatePassword();
                    onlineProfileService.hashAndSetPassword(profile, adminPassword);
                    onlineProfileService.updateOnlineProfile(profile);
                    writeAdminPasswordFile(adminPassword);
                    logger.warn("Admin credentials (regenerated, {} file was missing):", ADMIN_PASSWORD_FILE);
                    logger.warn("  Username: {}", adminUsername);
                    logger.warn("  Password: {}", adminPassword);
                }
            }
        }
    }

    /**
     * Write admin password to file for persistence across restarts
     */
    private void writeAdminPasswordFile(String password) {
        try {
            String workDir = System.getenv("WORK");
            if (workDir == null)
                workDir = "/data";
            Path dirPath = Paths.get(workDir);
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(ADMIN_PASSWORD_FILE);
            Files.writeString(filePath, password, StandardCharsets.UTF_8);
            logger.debug("Admin password saved to: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to write admin password file", e);
        }
    }

    /**
     * Read admin password from file
     */
    private String readAdminPasswordFile() {
        try {
            String workDir = System.getenv("WORK");
            if (workDir == null)
                workDir = "/data";
            Path filePath = Paths.get(workDir, ADMIN_PASSWORD_FILE);
            if (Files.exists(filePath)) {
                String password = Files.readString(filePath, StandardCharsets.UTF_8).trim();
                logger.debug("Admin password read from: {}", filePath);
                return password;
            }
        } catch (IOException e) {
            logger.error("Failed to read admin password file", e);
        }
        return null;
    }

    /**
     * Set TCP chat server
     */
    public void setTcpChatServer(TcpChatServer tcpChatServer) {
        tcpChatServer_ = tcpChatServer;
    }

    /**
     * Shutdown TCP chat server
     */
    @Override
    protected void shutdown(boolean immediate) {
        if (tcpChatServer_ != null) {
            tcpChatServer_.shutdown();
        }
        super.shutdown(immediate);
    }
}
