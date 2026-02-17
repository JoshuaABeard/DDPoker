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
package com.donohoedigital.games.poker.gameserver.persistence;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Boot auto-configuration for poker game server persistence features
 * (JPA, database). Only activates when a DataSource is available on the
 * classpath.
 *
 * <p>
 * Provides:
 * </p>
 * <ul>
 * <li>JPA repositories for game instances, events, profiles, and bans</li>
 * <li>Database-backed event store implementation</li>
 * <li>Entity scanning for game server and shared entities</li>
 * </ul>
 *
 * <p>
 * This configuration is conditional on the presence of {@link DataSource} on
 * the classpath, meaning it won't activate in scenarios without database
 * support (e.g., pure in-memory games).
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@EnableJpaRepositories(basePackages = "com.donohoedigital.games.poker.gameserver.persistence.repository")
@EntityScan(basePackages = {"com.donohoedigital.games.poker.gameserver.persistence.entity",
        "com.donohoedigital.games.poker.model" // OnlineProfile for auth
})
public class GameServerPersistenceAutoConfiguration {
    // Bean definitions for persistence components will be added here
    // (GameEventStoreFactory, etc.)
}
