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

import com.donohoedigital.mail.DDPostalService;
import com.donohoedigital.mail.DDPostalServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the legacy {@link DDPostalService} as a Spring bean.
 *
 * <p>
 * The {@code false} constructor argument means the mail queue thread will NOT
 * loop at end during shutdown (matching the original XML config).
 */
@Configuration
public class PostalServiceConfig {

    @Bean
    public DDPostalService postalService() {
        return new DDPostalServiceImpl(false);
    }
}
