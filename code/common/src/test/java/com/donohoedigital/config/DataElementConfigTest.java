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

import org.apache.logging.log4j.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Created by IntelliJ IDEA. User: donohoe Date: Apr 7, 2008 Time: 8:28:44 AM To
 * change this template use File | Settings | File Templates.
 */
class DataElementConfigTest {
    private static Logger logger = LogManager.getLogger(DataElementConfigTest.class);

    @SuppressWarnings({"SuspiciousMethodCalls"})
    @Test
    void testLoad() {
        String[] modules = {"common", "testapp"};
        new PropertyConfig("testapp", modules, ApplicationType.CLIENT, null, true);
        DataElementConfig dec = new DataElementConfig("testapp", null);

        DataElement dogs = dec.get("dogs");
        assertThat(dogs).isNotNull();

        List<Object> values = new ArrayList<>(dogs.getListValues());
        for (Object o : values) {
            logger.info("Value: " + o);
        }
        assertThat(values).contains("tahoe");
        assertThat(values).contains("dexter");
        assertThat(values).contains("zorro");
        assertThat(values).doesNotContain("rugby");
    }
}
