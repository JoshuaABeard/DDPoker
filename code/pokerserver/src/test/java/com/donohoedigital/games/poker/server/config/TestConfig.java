/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.server.config;

import com.donohoedigital.games.poker.server.PokerServer;
import com.donohoedigital.games.poker.server.PokerServerMain;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Spring Boot test configuration for pokerserver integration tests.
 *
 * <p>
 * This replaces the legacy {@code app-context-pokerservertests.xml} and
 * provides the same component scanning and data source configuration via Spring
 * Boot auto-configuration with test-specific properties from
 * {@code application-test.properties}.
 *
 * <p>
 * The {@link PokerServer}, {@link PokerServerMain}, and
 * {@link PostalServiceConfig} are excluded because the original test context
 * did not create the server or mail beans.
 */
@SpringBootApplication(exclude = {com.donohoedigital.games.poker.gameserver.GameServerAutoConfiguration.class,
        com.donohoedigital.games.poker.gameserver.persistence.GameServerPersistenceAutoConfiguration.class})
@ComponentScan(basePackages = "com.donohoedigital", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.donohoedigital\\.poker\\.api\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.donohoedigital\\.games\\.poker\\.gameserver\\..*"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {PokerServer.class,
                PostalServiceConfig.class, PokerServerMain.class})})
public class TestConfig {
}
