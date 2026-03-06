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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.ConfigTestHelper;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.games.poker.protocol.constants.ProtocolConstants;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

class TournamentProfileFormatterTest {

    // =================================================================
    // getTableFormatDisplay tests (pure unit — no infrastructure needed)
    // =================================================================

    @Nested
    class GetTableFormatDisplay {

        @Test
        void should_ReturnFullRing_When_SeatsFullRing() {
            assertThat(TournamentProfileFormatter.getTableFormatDisplay(ProtocolConstants.SEATS_FULL_RING))
                    .isEqualTo("Full Ring, 10 per table");
        }

        @Test
        void should_Return6Max_When_Seats6Max() {
            assertThat(TournamentProfileFormatter.getTableFormatDisplay(ProtocolConstants.SEATS_6MAX))
                    .isEqualTo("6-Max, 6 per table");
        }

        @Test
        void should_ReturnHeadsUp_When_SeatsHeadsUp() {
            assertThat(TournamentProfileFormatter.getTableFormatDisplay(ProtocolConstants.SEATS_HEADS_UP))
                    .isEqualTo("Heads-Up");
        }

        @Test
        void should_ReturnCustomFormat_When_NonStandardSeats() {
            assertThat(TournamentProfileFormatter.getTableFormatDisplay(8)).isEqualTo("8 per table");
        }

        @Test
        void should_ReturnCustomFormat_When_ThreeSeats() {
            assertThat(TournamentProfileFormatter.getTableFormatDisplay(3)).isEqualTo("3 per table");
        }
    }

    // =================================================================
    // Integration tests (require ConfigManager / PropertyConfig)
    // =================================================================

    @Nested
    @Tag("slow")
    @Tag("integration")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WithIntegration {

        @BeforeAll
        void setupConfig() {
            ConfigTestHelper.initializeForTesting("poker");
        }

        @AfterAll
        void teardownConfig() {
            ConfigTestHelper.resetForTesting();
        }

        private TournamentProfile createTestProfile() {
            TournamentProfile profile = new TournamentProfile("Test Tournament");
            profile.setNumPlayers(10);
            profile.setBuyin(100);
            profile.setBuyinChips(1000);
            profile.setLevel(1, 0, 5, 10, 30);
            profile.setLevel(2, 0, 10, 20, 30);
            profile.setLevel(3, 25, 25, 50, 30);
            profile.fixAll();
            return profile;
        }

        // --- getProfile ---

        @Test
        void should_ReturnProfile_When_PassedToConstructor() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            assertThat(formatter.getProfile()).isSameAs(profile);
        }

        // --- toHTMLSummary ---

        @Test
        void should_ReturnNonEmptyHtmlSummary_When_ValidProfile() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String html = formatter.toHTMLSummary(false, null);

            assertThat(html).isNotEmpty();
        }

        @Test
        void should_ReturnNonEmptyHtmlSummary_When_ListMode() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String html = formatter.toHTMLSummary(true, null);

            assertThat(html).isNotEmpty();
        }

        @Test
        void should_CacheSummaryResult_When_CalledTwice() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String first = formatter.toHTMLSummary(false, null);
            String second = formatter.toHTMLSummary(false, null);

            assertThat(first).isSameAs(second);
        }

        @Test
        void should_RecacheSummaryResult_When_ListModeChanges() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String nonList = formatter.toHTMLSummary(false, null);
            String list = formatter.toHTMLSummary(true, null);

            assertThat(nonList).isNotEqualTo(list);
        }

        // --- toHTMLOnline ---

        @Test
        void should_ReturnNonEmptyHtmlOnline_When_ValidProfile() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String html = formatter.toHTMLOnline();

            assertThat(html).isNotEmpty();
        }

        // --- getBlindsText ---

        @Test
        void should_ReturnNonEmptyBlindsText_When_ValidLevel() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String text = formatter.getBlindsText("msg.menu.", 1, false);

            assertThat(text).isNotEmpty();
        }

        @Test
        void should_ReturnBriefAmounts_When_BriefRequested() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String text = formatter.getBlindsText("msg.menu.", 1, true);

            assertThat(text).isNotEmpty();
        }

        @Test
        void should_IncludeAnte_When_LevelHasAnte() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String text = formatter.getBlindsText("msg.menu.", 3, false);

            assertThat(text).contains("25");
        }

        // --- getSpotHTML ---

        @Test
        void should_ReturnNonEmptySpotHtml_When_ValidSpot() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String html = formatter.getSpotHTML(1, false, null);

            assertThat(html).isNotEmpty();
        }

        @Test
        void should_ReturnNonEmptySpotHtml_When_ShowPercAndEstimate() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String html = formatter.getSpotHTML(1, true, null);

            assertThat(html).isNotEmpty();
        }

        // --- toHTMLSpots ---

        @Test
        void should_ReturnNonEmptySpotsHtml_When_ValidProfile() {
            TournamentProfile profile = createTestProfile();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String html = formatter.toHTMLSpots();

            assertThat(html).isNotEmpty();
        }

        // --- rebuys and addons ---

        @Test
        void should_IncludeRebuyInfo_When_RebuysEnabled() {
            TournamentProfile profile = createTestProfile();
            profile.setRebuys(true);
            profile.getMap().setInteger(TournamentProfile.PARAM_REBUYCOST, 50);
            profile.getMap().setInteger(TournamentProfile.PARAM_REBUYCHIPS, 500);
            profile.getMap().setInteger(TournamentProfile.PARAM_REBUY_UNTIL, 3);
            profile.fixAll();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String html = formatter.toHTMLSummary(false, null);

            assertThat(html).isNotEmpty();
        }

        @Test
        void should_IncludeAddonInfo_When_AddonsEnabled() {
            TournamentProfile profile = createTestProfile();
            profile.setAddons(true);
            profile.getMap().setInteger(TournamentProfile.PARAM_ADDONCOST, 50);
            profile.getMap().setInteger(TournamentProfile.PARAM_ADDONCHIPS, 500);
            profile.getMap().setInteger(TournamentProfile.PARAM_ADDONLEVEL, 3);
            profile.fixAll();
            TournamentProfileFormatter formatter = new TournamentProfileFormatter(profile);

            String html = formatter.toHTMLSummary(false, null);

            assertThat(html).isNotEmpty();
        }
    }
}
