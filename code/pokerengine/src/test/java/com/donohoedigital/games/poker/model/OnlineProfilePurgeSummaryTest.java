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
 * Tests for OnlineProfilePurgeSummary.
 */
public class OnlineProfilePurgeSummaryTest {

    @Test
    public void testDefaultConstructor() {
        OnlineProfilePurgeSummary summary = new OnlineProfilePurgeSummary();

        assertNotNull(summary);
        assertNull(summary.getOnlineProfile());
        assertEquals(0, summary.getHistoryCount());
    }

    @Test
    public void testSetHistoryCount() {
        OnlineProfilePurgeSummary summary = new OnlineProfilePurgeSummary();

        summary.setHistoryCount(50);

        assertEquals(50, summary.getHistoryCount());
    }

    @Test
    public void testSetOnlineProfile() {
        OnlineProfilePurgeSummary summary = new OnlineProfilePurgeSummary();
        OnlineProfile profile = new OnlineProfile();
        profile.setName("TestPlayer");

        summary.setOnlineProfile(profile);

        assertEquals(profile, summary.getOnlineProfile());
        assertEquals("TestPlayer", summary.getOnlineProfile().getName());
    }

    @Test
    public void testFullyPopulatedSummary() {
        OnlineProfilePurgeSummary summary = new OnlineProfilePurgeSummary();
        OnlineProfile profile = new OnlineProfile();
        profile.setName("PurgePlayer");

        summary.setOnlineProfile(profile);
        summary.setHistoryCount(75);

        assertEquals(profile, summary.getOnlineProfile());
        assertEquals(75, summary.getHistoryCount());
    }
}
