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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the legacy {@link ConfigManager} singleton as a Spring bean.
 *
 * <p>
 * ConfigManager loads property files, data elements, and other config from
 * classpath resources. For the server, it only loads property config (not GUI
 * assets). The bean is created early so that all PropertyConfig-dependent code
 * sees initialized state.
 */
@Configuration
public class ConfigManagerConfig {

    @Bean
    public ConfigManager configManager(@Value("${app.name:poker}") String appName) {
        return new ConfigManager(appName, ApplicationType.SERVER);
    }
}
