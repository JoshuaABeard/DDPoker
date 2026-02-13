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
package com.donohoedigital.games.poker.model;

import com.donohoedigital.games.config.AbstractPlayerList;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for ProfileXMLSerializer - XML encoding of tournament profiles.
 */
public class ProfileXMLSerializerTest {

    // ========== Integration Test with Real Profile ==========

    @Test
    public void should_EncodeXML_WithoutError() {
        // Given: a tournament profile with data
        TournamentProfile profile = new TournamentProfile("Test Tournament");
        profile.setNumPlayers(10);
        profile.setBuyinChips(1000);

        // When: encode to XML (this will exercise the full encoding path)
        // Note: We can't easily test the XML output without a full encoder setup,
        // but we can verify it doesn't throw exceptions
        try {
            // Create a mock encoder that just tracks calls
            MockEncoder encoder = new MockEncoder();
            ProfileXMLSerializer serializer = new ProfileXMLSerializer(new ProfileDataProviderImpl(profile));
            serializer.encodeXML(encoder, profile);

            // Then: should have encoded levels, prizes, invitees, players
            assertTrue("Should have encoded tournament", encoder.objectsSet.contains("tournamentFormat"));
            assertTrue("Should have encoded levels", encoder.objectsSet.contains("levels"));
            assertTrue("Should have encoded prizes", encoder.objectsSet.contains("prizes"));
            assertTrue("Should have encoded invitees", encoder.objectsSet.contains("invitees"));
            assertTrue("Should have encoded players", encoder.objectsSet.contains("players"));
        } catch (Exception e) {
            fail("Should not throw exception during encoding: " + e.getMessage());
        }
    }

    @Test
    public void should_EncodeLevels_WithBreakHandling() {
        // Given: a data provider with a break level
        ProfileXMLSerializer.ProfileDataProvider provider = new ProfileXMLSerializer.ProfileDataProvider() {
            @Override
            public int getLastLevel() {
                return 2;
            }

            @Override
            public int getMinutes(int level) {
                return level == 2 ? 10 : 20;
            }

            @Override
            public boolean isBreak(int level) {
                return level == 2;
            }

            @Override
            public String getGameTypeString(int level) {
                return "HOLDEM";
            }

            @Override
            public int getAnte(int level) {
                return 0;
            }

            @Override
            public int getBigBlind(int level) {
                return 20;
            }

            @Override
            public int getSmallBlind(int level) {
                return 10;
            }

            @Override
            public int getNumSpots() {
                return 0;
            }

            @Override
            public String getSpotAsString(int spot) {
                return "";
            }

            @Override
            public List<AbstractPlayerList.PlayerInfo> getInvitees() {
                return new ArrayList<>();
            }

            @Override
            public List<String> getPlayers() {
                return new ArrayList<>();
            }
        };

        TournamentProfile profile = new TournamentProfile("Test");
        MockEncoder encoder = new MockEncoder();
        ProfileXMLSerializer serializer = new ProfileXMLSerializer(provider);

        // When: encode XML
        serializer.encodeXML(encoder, profile);

        // Then: should have processed levels including break
        assertTrue("Should have set level object", encoder.objectsSet.contains("level"));
        assertTrue("Should have added break tag", encoder.tagsAdded.containsKey("break"));
    }

    @Test
    public void should_EncodePayouts_ForAllSpots() {
        // Given: profile with 3 payout spots
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setPayoutSpots(3);

        MockEncoder encoder = new MockEncoder();
        ProfileXMLSerializer serializer = new ProfileXMLSerializer(new ProfileDataProviderImpl(profile));

        // When: encode XML
        serializer.encodeXML(encoder, profile);

        // Then: should have encoded prize structure
        assertTrue("Should have set prizes object", encoder.objectsSet.contains("prizes"));
        assertTrue("Should have set prize object", encoder.objectsSet.contains("prize"));
    }

    @Test
    public void should_HandleEmptyInvitees() {
        // Given: profile with no invitees
        TournamentProfile profile = new TournamentProfile("Test");

        MockEncoder encoder = new MockEncoder();
        ProfileXMLSerializer serializer = new ProfileXMLSerializer(new ProfileDataProviderImpl(profile));

        // When: encode XML
        serializer.encodeXML(encoder, profile);

        // Then: should still have invitees section (empty)
        assertTrue("Should have set invitees object even when empty", encoder.objectsSet.contains("invitees"));
    }

    @Test
    public void should_HandleEmptyPlayers() {
        // Given: profile with no players
        TournamentProfile profile = new TournamentProfile("Test");

        MockEncoder encoder = new MockEncoder();
        ProfileXMLSerializer serializer = new ProfileXMLSerializer(new ProfileDataProviderImpl(profile));

        // When: encode XML
        serializer.encodeXML(encoder, profile);

        // Then: should still have players section (empty)
        assertTrue("Should have set players object even when empty", encoder.objectsSet.contains("players"));
    }

    // ========== Helper Classes ==========

    /**
     * Mock encoder that tracks method calls for testing.
     */
    private static class MockEncoder extends com.donohoedigital.xml.SimpleXMLEncoder {
        Set<String> objectsSet = new HashSet<>();
        Map<String, Object> tagsAdded = new HashMap<>();
        int finishCount = 0;

        @Override
        public com.donohoedigital.xml.SimpleXMLEncoder setCurrentObject(Object obj, String sTag) {
            objectsSet.add(sTag);
            return this;
        }

        @Override
        public com.donohoedigital.xml.SimpleXMLEncoder setCurrentObject(String sTag) {
            objectsSet.add(sTag);
            return this;
        }

        @Override
        public com.donohoedigital.xml.SimpleXMLEncoder addTag(String sTag, Object value) {
            tagsAdded.put(sTag, value);
            return this;
        }

        @Override
        public com.donohoedigital.xml.SimpleXMLEncoder addAllTagsExcept(String... excludedTags) {
            // Track that metadata was encoded
            tagsAdded.put("_metadata_", true);
            return this;
        }

        @Override
        public com.donohoedigital.xml.SimpleXMLEncoder finishCurrentObject() {
            finishCount++;
            return this;
        }
    }

    /**
     * Implementation of ProfileDataProvider that delegates to TournamentProfile.
     */
    private static class ProfileDataProviderImpl implements ProfileXMLSerializer.ProfileDataProvider {
        private final TournamentProfile profile;

        ProfileDataProviderImpl(TournamentProfile profile) {
            this.profile = profile;
        }

        @Override
        public int getLastLevel() {
            return profile.getLastLevel();
        }

        @Override
        public int getMinutes(int level) {
            return profile.getMinutes(level);
        }

        @Override
        public boolean isBreak(int level) {
            return profile.isBreak(level);
        }

        @Override
        public String getGameTypeString(int level) {
            return profile.getGameTypeString(level);
        }

        @Override
        public int getAnte(int level) {
            return profile.getAnte(level);
        }

        @Override
        public int getBigBlind(int level) {
            return profile.getBigBlind(level);
        }

        @Override
        public int getSmallBlind(int level) {
            return profile.getSmallBlind(level);
        }

        @Override
        public int getNumSpots() {
            return profile.getNumSpots();
        }

        @Override
        public String getSpotAsString(int spot) {
            return profile.getSpotAsString(spot);
        }

        @Override
        public List<AbstractPlayerList.PlayerInfo> getInvitees() {
            return profile.getInvitees();
        }

        @Override
        public List<String> getPlayers() {
            return profile.getPlayers();
        }
    }
}
