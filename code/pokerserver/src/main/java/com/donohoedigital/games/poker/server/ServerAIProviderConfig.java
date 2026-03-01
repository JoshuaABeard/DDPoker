/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.gameserver.AIProviderFactory;
import com.donohoedigital.games.poker.gameserver.AIProviderResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that provides an {@link AIProviderFactory} backed by
 * {@link ServerAIProvider}. This bridges the module boundary: the game engine
 * ({@code pokergameserver}) cannot depend on {@code pokerserver}, but this
 * module has access to both.
 *
 * <p>
 * When this configuration is on the classpath, all games created via
 * {@code GameInstanceManager} use the strategic AI algorithms
 * (TournamentAI/V1/V2) instead of the random fallback.
 *
 * <p>
 * For the desktop embedded server, this configuration must be imported by the
 * Spring Boot application class (e.g., {@code EmbeddedServerConfig}). For the
 * standalone server, it is discovered by Spring Boot auto-configuration if on
 * the component scan path.
 */
@Configuration
public class ServerAIProviderConfig {

    @Bean
    public AIProviderFactory aiProviderFactory() {
        return (players, skillLevels, table, tournament) -> {
            ServerAIProvider provider = new ServerAIProvider(players, skillLevels, table, tournament);
            return new AIProviderResult(provider, provider::onNewHand);
        };
    }
}
