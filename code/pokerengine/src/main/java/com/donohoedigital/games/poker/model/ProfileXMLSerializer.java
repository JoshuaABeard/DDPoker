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

import com.donohoedigital.games.config.*;
import com.donohoedigital.xml.*;
import java.util.*;

/**
 * Handles XML serialization of tournament profiles.
 *
 * <p>
 * Encodes tournament configuration to XML format including:
 * <ul>
 * <li>Tournament metadata (via BaseProfile)
 * <li>Blind level structure
 * <li>Payout spots
 * <li>Invitees and player lists
 * </ul>
 *
 * <p>
 * Extracted from TournamentProfile to improve testability.
 *
 * @see TournamentProfile#encodeXML(SimpleXMLEncoder)
 */
public class ProfileXMLSerializer {

    /**
     * Callbacks for accessing tournament profile data.
     */
    public interface ProfileDataProvider {
        int getLastLevel();

        int getMinutes(int level);

        boolean isBreak(int level);

        String getGameTypeString(int level);

        int getAnte(int level);

        int getBigBlind(int level);

        int getSmallBlind(int level);

        int getNumSpots();

        String getSpotAsString(int spot);

        List<AbstractPlayerList.PlayerInfo> getInvitees();

        List<String> getPlayers();
    }

    private final ProfileDataProvider provider;

    /**
     * Create an XML serializer for tournament profiles.
     *
     * @param provider
     *            Provider for accessing profile data
     */
    public ProfileXMLSerializer(ProfileDataProvider provider) {
        this.provider = provider;
    }

    /**
     * Encode tournament profile to XML.
     *
     * <p>
     * Extracted from TournamentProfile.encodeXML() (lines 1470-1522).
     *
     * @param encoder
     *            XML encoder to write to
     * @param profile
     *            Tournament profile being encoded
     */
    public void encodeXML(SimpleXMLEncoder encoder, TournamentProfile profile) {
        encoder.setCurrentObject(profile, "tournamentFormat");
        encoder.addAllTagsExcept("map", "fileNum", "file", "dir", "fileName", "lastModified", "createDate",
                "updateDate", "invitees", "players");

        // levels
        encodeLevels(encoder);

        // payouts
        encodePayouts(encoder);

        // invitees
        encodeInvitees(encoder);

        // players
        encodePlayers(encoder);

        encoder.finishCurrentObject(); // tournament
    }

    /**
     * Encode blind level structure to XML.
     */
    private void encodeLevels(SimpleXMLEncoder encoder) {
        encoder.setCurrentObject("levels");
        for (int i = 1; i <= provider.getLastLevel(); i++) {
            encoder.setCurrentObject("level");

            encoder.addTag("number", i);
            encoder.addTag("minutes", provider.getMinutes(i));

            if (provider.isBreak(i)) {
                encoder.addTag("break", true);
            } else {
                encoder.addTag("gameType", provider.getGameTypeString(i));
                encoder.addTag("ante", provider.getAnte(i));
                encoder.addTag("small", provider.getBigBlind(i));
                encoder.addTag("big", provider.getSmallBlind(i));
            }

            encoder.finishCurrentObject();
        }
        encoder.finishCurrentObject(); // levels
    }

    /**
     * Encode payout structure to XML.
     */
    private void encodePayouts(SimpleXMLEncoder encoder) {
        encoder.setCurrentObject("prizes");
        for (int i = 1; i <= provider.getNumSpots(); i++) {
            encoder.setCurrentObject("prize");

            encoder.addTag("place", i);
            encoder.addTag("amount", provider.getSpotAsString(i));

            encoder.finishCurrentObject(); // prize
        }
        encoder.finishCurrentObject(); // prizes
    }

    /**
     * Encode invitees list to XML.
     */
    private void encodeInvitees(SimpleXMLEncoder encoder) {
        encoder.setCurrentObject("invitees");
        for (AbstractPlayerList.PlayerInfo player : provider.getInvitees()) {
            encoder.addTag("player", player.getName());
        }
        encoder.finishCurrentObject(); // invitees
    }

    /**
     * Encode players list to XML.
     */
    private void encodePlayers(SimpleXMLEncoder encoder) {
        encoder.setCurrentObject("players");
        for (String player : provider.getPlayers()) {
            encoder.addTag("player", player);
        }
        encoder.finishCurrentObject(); // players
    }
}
