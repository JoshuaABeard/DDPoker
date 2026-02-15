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
package com.donohoedigital.games.poker.model;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for HostSummary.
 */
public class HostSummaryTest {

    @Test
    public void testDefaultConstructor() {
        HostSummary summary = new HostSummary();

        assertNotNull(summary);
        assertNull(summary.getHostName());
        assertEquals(0, summary.getGamesHosted());
        assertFalse(summary.isRetired());
    }

    @Test
    public void testSetHostName() {
        HostSummary summary = new HostSummary();
        String hostName = "TestHost";

        summary.setHostName(hostName);

        assertEquals(hostName, summary.getHostName());
    }

    @Test
    public void testSetGamesHosted() {
        HostSummary summary = new HostSummary();

        summary.setGamesHosted(42);

        assertEquals(42, summary.getGamesHosted());
    }

    @Test
    public void testSetRetired() {
        HostSummary summary = new HostSummary();

        summary.setRetired(true);

        assertTrue(summary.isRetired());
    }

    @Test
    public void testFullyPopulatedSummary() {
        HostSummary summary = new HostSummary();

        summary.setHostName("ProHost");
        summary.setGamesHosted(100);
        summary.setRetired(false);

        assertEquals("ProHost", summary.getHostName());
        assertEquals(100, summary.getGamesHosted());
        assertFalse(summary.isRetired());
    }
}
