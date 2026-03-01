/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.config;

import com.donohoedigital.config.PropertyConfig;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Test helper that creates Border and Territory instances without requiring the
 * XML/PropertyConfig infrastructure.
 *
 * <p>
 * {@code Territory} has static fields initialized via {@code PropertyConfig} at
 * class-load time, and constructors that require an {@code Areas} singleton. We
 * work around both constraints:
 *
 * <ol>
 * <li>Prime {@code PropertyConfig} so Territory's {@code <clinit>} succeeds by
 * injecting a stub {@code Properties} instance as {@code propConfig} and
 * enabling {@code testing} mode so missing keys return a harmless
 * placeholder.</li>
 * <li>Bypass the Territory constructor via {@link Unsafe#allocateInstance} and
 * set only the {@code sName_} field needed by
 * {@code Border.orderTerritories()}.</li>
 * </ol>
 */
class BorderTestHelper {

    private static final Unsafe UNSAFE;
    private static final Field TERRITORY_NAME_FIELD;
    private static final Field TERRITORY_BORDERS_FIELD;

    static {
        try {
            // sun.misc.Unsafe is in jdk.unsupported which opens sun.misc to unnamed modules
            // by default
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);

            primePropertyConfig();

            // Force Territory class load now that PropertyConfig is primed
            Class.forName("com.donohoedigital.games.config.Territory");

            TERRITORY_NAME_FIELD = Territory.class.getDeclaredField("sName_");
            TERRITORY_NAME_FIELD.setAccessible(true);

            // Unsafe.allocateInstance skips field initializers, so myBorders_ is null.
            // We capture the field so createTerritory() can inject a fresh BorderArrayList.
            TERRITORY_BORDERS_FIELD = Territory.class.getDeclaredField("myBorders_");
            TERRITORY_BORDERS_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Injects a minimal stub into {@code PropertyConfig} so that
     * {@code assertNotNull(propConfig)} passes and property lookups return non-null
     * values without needing any config files on the classpath.
     *
     * <p>
     * We use {@link Unsafe#putObject} to bypass the type constraint: propConfig is
     * typed as {@code PropertyConfig} but we install a plain {@code Properties}
     * instance. At the JVM level they share the same {@code getProperty()} code
     * path so this is safe for the read-only lookups made by Territory's static
     * initializer.
     */
    @SuppressWarnings("deprecation")
    private static void primePropertyConfig() throws Exception {
        if (PropertyConfig.isInitialized()) {
            return;
        }

        // Create a real Properties instance (not Unsafe-allocated) so the
        // underlying Hashtable infrastructure is properly initialized.
        // Provide the values that Territory's static initializer reads.
        java.util.Properties stub = new java.util.Properties();
        stub.setProperty("define.territoryPointType.label", "label");
        stub.setProperty("define.territoryType.edge", "edge");
        stub.setProperty("define.territoryType.decoration", "decoration");
        stub.setProperty("define.territoryType.water", "water");
        stub.setProperty("define.territoryType.land", "land");

        // Install stub as propConfig, bypassing the type check at the JVM level.
        Field propConfigField = PropertyConfig.class.getDeclaredField("propConfig");
        propConfigField.setAccessible(true);
        long fieldOffset = UNSAFE.staticFieldOffset(propConfigField);
        Object base = UNSAFE.staticFieldBase(propConfigField);
        UNSAFE.putObject(base, fieldOffset, stub);

        // Enable testing mode so any missing property returns a placeholder
        // string rather than null, avoiding secondary assertion failures.
        Field testingField = PropertyConfig.class.getDeclaredField("testing");
        testingField.setAccessible(true);
        long testingOffset = UNSAFE.staticFieldOffset(testingField);
        UNSAFE.putBoolean(base, testingOffset, true);
    }

    /**
     * Creates a minimal Territory with its name and an empty border list set.
     * Bypasses the constructor to avoid needing a configured Areas singleton.
     *
     * <p>
     * {@code Unsafe.allocateInstance} skips all field initializers, so
     * {@code myBorders_} would be {@code null} without explicit injection.
     */
    static Territory createTerritory(String name) {
        try {
            Territory t = (Territory) UNSAFE.allocateInstance(Territory.class);
            TERRITORY_NAME_FIELD.set(t, name);
            TERRITORY_BORDERS_FIELD.set(t, new BorderArrayList(2));
            return t;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test Territory: " + name, e);
        }
    }

    /**
     * Creates a Border from two named territories without requiring the static
     * Territories registry used by the string-based Border constructor.
     */
    static Border createBorder(String name1, String name2) {
        Territory t1 = createTerritory(name1);
        Territory t2 = createTerritory(name2);
        return new Border(t1, t2, false);
    }

    /**
     * Creates a Border from two named territories with the enclosed flag set.
     */
    static Border createBorder(String name1, String name2, boolean enclosed) {
        Territory t1 = createTerritory(name1);
        Territory t2 = createTerritory(name2);
        return new Border(t1, t2, enclosed);
    }

    /**
     * Creates a Border with a specific number.
     */
    static Border createBorder(String name1, String name2, boolean enclosed, int num) {
        Territory t1 = createTerritory(name1);
        Territory t2 = createTerritory(name2);
        return new Border(t1, t2, enclosed, num);
    }

    /**
     * Creates an empty Territories collection without requiring XML or Areas
     * infrastructure. Uses Unsafe to bypass the Territories(XML) constructor; the
     * underlying TreeMap state initialises lazily on first put, so this is safe for
     * add/get/size operations.
     */
    static Territories createTerritories() {
        try {
            return (Territories) UNSAFE.allocateInstance(Territories.class);
        } catch (InstantiationException e) {
            throw new RuntimeException("Failed to create test Territories", e);
        }
    }
}
