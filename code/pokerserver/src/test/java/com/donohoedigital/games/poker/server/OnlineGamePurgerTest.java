/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;

class OnlineGamePurgerTest {

    @Test
    void parseDateOption_parsesIsoDateAtStartOfDay() {
        Date parsed = OnlineGamePurger.parseDateOption("2026-02-23");

        LocalDate parsedLocalDate = parsed.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        assertThat(parsedLocalDate).isEqualTo(LocalDate.of(2026, 2, 23));
    }

    @Test
    void parseDateOption_reportsOriginalInputWhenDateInvalid() {
        String invalidDate = "2026-99-99";

        assertThatThrownBy(() -> OnlineGamePurger.parseDateOption(invalidDate))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Unable to parse date: " + invalidDate);
    }
}
