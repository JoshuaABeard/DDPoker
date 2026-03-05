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
package com.donohoedigital.config;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Created by IntelliJ IDEA. User: donohoe Date: Apr 6, 2008 Time: 4:43:12 PM To
 * change this template use File | Settings | File Templates.
 */
class PropertyConfigTest {
    @Test
    void testLoadClient() {
        System.getProperties().setProperty("user.name", "unit-tester");
        String[] modules = {"common", "testapp"};
        new PropertyConfig("testapp", modules, ApplicationType.CLIENT, null, true);

        assertThat(PropertyConfig.getRequiredBooleanProperty("test.common")).isTrue();
        assertThat(PropertyConfig.getRequiredBooleanProperty("test.common.override")).isTrue();

        assertThat(PropertyConfig.getRequiredBooleanProperty("test.boolean.true")).isTrue();
        assertThat(PropertyConfig.getRequiredBooleanProperty("test.boolean.false")).isFalse();
        assertThat(PropertyConfig.getRequiredBooleanProperty("test.boolean.yes")).isTrue();
        assertThat(PropertyConfig.getRequiredBooleanProperty("test.boolean.no")).isFalse();
        assertThat(PropertyConfig.getRequiredBooleanProperty("test.boolean.+")).isTrue();
        assertThat(PropertyConfig.getRequiredBooleanProperty("test.boolean.-")).isFalse();
        assertThat(PropertyConfig.getRequiredBooleanProperty("test.boolean.1")).isTrue();
        assertThat(PropertyConfig.getRequiredBooleanProperty("test.boolean.0")).isFalse();

        assertThat(PropertyConfig.getRequiredStringProperty("test.string")).isEqualTo("This is a string");
        assertThat(PropertyConfig.getRequiredIntegerProperty("test.integer")).isEqualTo(42);
        assertThat(PropertyConfig.getRequiredDoubleProperty("test.double")).isEqualTo(3.14159d, within(.0000001d));

        assertThat(PropertyConfig.getMessage("test.message")).isEqualTo("No replacement");
        assertThat(PropertyConfig.getMessage("test.message.one", "just")).isEqualTo("Replace just one.");
        assertThat(PropertyConfig.getMessage("test.message.two", "this", "that")).isEqualTo("Replace this and that.");

        // override in unit-tester.properties
        assertThat(PropertyConfig.getRequiredBooleanProperty("override.set")).isTrue();
    }
}
