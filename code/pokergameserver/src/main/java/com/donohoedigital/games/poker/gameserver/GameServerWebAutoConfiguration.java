/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration for poker game server web features (REST API,
 * controllers). Only activates when running in a web application context.
 *
 * <p>
 * Provides:
 * </p>
 * <ul>
 * <li>REST controllers for game management</li>
 * <li>REST controllers for authentication</li>
 * <li>REST controllers for profile management</li>
 * <li>JWT authentication filter and security configuration</li>
 * </ul>
 *
 * <p>
 * This configuration is conditional on the presence of a web application
 * context, meaning it won't activate in non-web scenarios (e.g., embedded game
 * server in desktop client).
 * </p>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ComponentScan(basePackages = {"com.donohoedigital.games.poker.gameserver.controller",
        "com.donohoedigital.games.poker.gameserver.service"})
public class GameServerWebAutoConfiguration {
    // Bean definitions for web-specific components will be added here
    // (JWT provider, auth filter, security config, etc.)
}
