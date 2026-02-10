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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.config.ConfigUtils;
import com.donohoedigital.config.MatchingResources;
import com.donohoedigital.games.poker.dao.OnlineProfileDao;
import com.donohoedigital.games.poker.model.OnlineProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.*;


/**
 * Tests for OnlineProfile persistence and DAO operations.
 */
@SpringJUnitConfig(locations = {"/app-context-pokerservertests.xml"})
@Transactional
class OnlineProfileTest
{
    private final Logger logger = LogManager.getLogger(OnlineProfileTest.class);

    @Autowired
    private OnlineProfileDao dao;

    @Test
    @Rollback
    void should_PersistAndUpdate_When_ProfileSaved()
    {
        String password = "foobar";
        OnlineProfile newProfile = PokerTestData.createOnlineProfile("TEST shouldPersist");
        newProfile.setPassword(password);
        dao.save(newProfile);

        assertThat(newProfile.getId()).isNotNull();

        OnlineProfile fetch = dao.get(newProfile.getId());
        assertThat(fetch.getName()).isEqualTo(newProfile.getName());
        assertThat(fetch.getPassword()).isEqualTo(password);

        // Update and verify persistence
        String newName = "Updated Name";
        newProfile.setName(newName);
        dao.update(newProfile);

        OnlineProfile updated = dao.get(newProfile.getId());
        assertThat(updated.getName()).isEqualTo(newName);
    }

    @Test
    @Rollback
    void should_DeleteProfile_When_DeleteCalled()
    {
        OnlineProfile profile = PokerTestData.createOnlineProfile("TEST saveBeforeDelete");
        dao.save(profile);
        assertThat(profile.getId()).isNotNull();
        logger.info(profile.getName() + " saved with id " + profile.getId());

        OnlineProfile lookup = dao.get(profile.getId());
        dao.delete(lookup);
        logger.info("Should have deleted profile with id " + lookup.getId());

        OnlineProfile deleted = dao.get(lookup.getId());
        assertThat(deleted).isNull();
    }

    @Test
    @Rollback
    void should_FindProfile_When_SearchingByName()
    {
        String name = "TEST getByName";
        OnlineProfile newProfile = PokerTestData.createOnlineProfile(name);
        dao.save(newProfile);

        OnlineProfile fetch = dao.getByName(name);
        assertThat(fetch).isNotNull();
        assertThat(fetch.getName()).isEqualTo(name);

        fetch = dao.getByName("no such name dude");
        assertThat(fetch).isNull();
    }

    @Test
    @Rollback
    void should_ReturnMatchingProfiles_When_SearchingByEmail()
    {
        String email1 = "dexter@example.com";
        String email2 = "zorro@example.com";
        String name = "Test all ";
        int total = 10;
        for (int i = 1; i <= total; i++)
        {
            OnlineProfile newProfile = PokerTestData.createOnlineProfile(name + i);
            newProfile.setEmail(i % 2 == 0 ? email1 : email2);
            dao.save(newProfile);
        }

        List<OnlineProfile> list1 = dao.getAllForEmail(email1, null);
        assertThat(list1).hasSize(total / 2);

        for (OnlineProfile p : list1)
        {
            assertThat(p.getEmail()).isEqualTo(email1);
        }

        List<OnlineProfile> list2 = dao.getAllForEmail(email2, null);
        assertThat(list2).hasSize(total / 2);

        for (OnlineProfile p : list2)
        {
            assertThat(p.getEmail()).isEqualTo(email2);
        }

        // test exclude behavior
        String nameFetch = name + 1;
        List<OnlineProfile> list3 = dao.getAllForEmail(email2, nameFetch);
        assertThat(list3).hasSize((total / 2) - 1);

        for (OnlineProfile p : list3)
        {
            assertThat(p.getName()).isNotEqualTo(nameFetch);
        }

        // test none found returns empty list
        List<OnlineProfile> list4 = dao.getAllForEmail("xxxx", null);
        assertThat(list4).isNotNull().isEmpty();
    }

    @Test
    @Rollback
    void should_FindMatchingProfiles_When_SearchingWithWildcards()
    {
        String email = "dexter@example.com";
        String name = "Find Me Special_Chars 100% \\/";
        int total = 10;
        for (int i = 1; i <= total; i++)
        {
            OnlineProfile newProfile = PokerTestData.createOnlineProfile(name + i);
            newProfile.setEmail(email);
            dao.save(newProfile);
        }

        // add extra to test non-matching
        int nonmatchtotal = 5;
        for (int i = 1; i <= nonmatchtotal; i++)
        {
            OnlineProfile newProfile = PokerTestData.createOnlineProfile("TOTALLY DIFFERENT NAME" + i);
            newProfile.setEmail("foo@blah.com");
            dao.save(newProfile);
        }
        int max = total + nonmatchtotal;

        List<OnlineProfile> list;

        list = dao.getMatching(null, 0, max, "Find", null, null, false);
        assertThat(list).hasSize(total);

        list = dao.getMatching(null, 0, max, null, "example", null, false);
        assertThat(list).hasSize(total);

        list = dao.getMatching(null, 0, max, "%", null, null, false);
        assertThat(list).hasSize(total);

        list = dao.getMatching(null, 0, max, "_", null, null, false);
        assertThat(list).hasSize(total);

        list = dao.getMatching(null, 0, max, "\\", null, null, false);
        assertThat(list).hasSize(total);

        list = dao.getMatching(null, 0, max, "blah", "noone", "3333", false);
        assertThat(list).isEmpty();
    }

    @Test
    @Rollback
    void should_HandleUTF8Characters_When_ProfileNameIsNonASCII()
    {
        verifyFile("greek.utf8.txt");
        verifyFile("russian.utf8.txt");
        verifyFile("japanese.utf8.txt");
        verifyFile("chinese.utf8.txt");
        verifyFile("arabic.utf8.txt");
        verifyFile("swedish.utf8.txt");
    }

    private void verifyFile(String filename)
    {
        URL url = new MatchingResources("classpath:" + filename).getSingleRequiredResourceURL();
        String utf8 = ConfigUtils.readURL(url).trim();
        logger.debug(filename + ": " + utf8);
        OnlineProfile newProfile = PokerTestData.createOnlineProfile(utf8);
        dao.save(newProfile);
        assertThat(newProfile.getId()).isNotNull();

        dao.flush();
        dao.clear();

        OnlineProfile fetch = dao.get(newProfile.getId());
        assertThat(fetch).isNotSameAs(newProfile);
        assertThat(fetch.getName()).isEqualTo(newProfile.getName());
        assertThat(fetch.getName()).isEqualTo(utf8);
        logger.debug("Name: " + utf8);
    }

}
