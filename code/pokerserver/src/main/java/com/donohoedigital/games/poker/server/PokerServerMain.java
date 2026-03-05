/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.engine.PokerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Spring Boot entry point for the standalone poker server.
 *
 * <p>
 * Component scanning covers the legacy service/DAO packages under
 * {@code com.donohoedigital} as well as the server config classes. The
 * pokergameserver and api packages are excluded to avoid bean conflicts (same
 * exclusions as the original XML config). Auto-configurations from
 * pokergameserver are also excluded.
 */
@SpringBootApplication(exclude = {com.donohoedigital.games.poker.gameserver.GameServerAutoConfiguration.class,
        com.donohoedigital.games.poker.gameserver.persistence.GameServerPersistenceAutoConfiguration.class})
@ComponentScan(basePackages = "com.donohoedigital", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.donohoedigital\\.poker\\.api\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.donohoedigital\\.games\\.poker\\.gameserver\\..*")})
public class PokerServerMain {
    private static final Logger logger = LogManager.getLogger(PokerServerMain.class);

    public static void main(String[] argv) {
        // Log version at startup
        logger.info("========================================");
        logger.info("DD Poker Server Starting");
        logger.info("Version: {}", PokerConstants.VERSION);
        logger.info("========================================");

        SpringApplication.run(PokerServerMain.class, argv);
    }
}
