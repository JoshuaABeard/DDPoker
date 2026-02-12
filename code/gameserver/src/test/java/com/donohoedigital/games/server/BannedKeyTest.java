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
package com.donohoedigital.games.server;

import com.donohoedigital.games.server.dao.BannedKeyDao;
import com.donohoedigital.games.server.model.BannedKey;
import org.apache.logging.log4j.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Created by IntelliJ IDEA. User: donohoe Date: Mar 13, 2008 Time: 2:44:16 PM
 * To change this template use File | Settings | File Templates.
 */

@SpringJUnitConfig(locations = {"/app-context-jpatests.xml"})
@Transactional
class BannedKeyTest {
    private final Logger logger = LogManager.getLogger(BannedKeyTest.class);

    @Autowired
    private BannedKeyDao dao;

    @Test
    @Rollback
    void should_PersistAndUpdateBannedKey_When_SavedAndUpdated() {
        BannedKey newKey = ServerTestData.createBannedKey("0000-0000-1111-2222");
        dao.save(newKey);
        assertThat(newKey.getId()).isNotNull();

        BannedKey nullComment = ServerTestData.createBannedKey("0000-0000-1111-3332");
        nullComment.setComment(null);
        dao.save(nullComment);
        assertThat(nullComment.getId()).isNotNull();

        BannedKey fetch = dao.get(newKey.getId());
        assertThat(fetch.getKey()).as("name should match").isEqualTo(newKey.getKey());

        String key = "1111-1111-1111-1111";
        newKey.setKey(key);
        dao.update(newKey);

        BannedKey updated = dao.get(newKey.getId());
        assertThat(updated.getKey()).as("key should match").isEqualTo(key);
    }

    @Test
    @Rollback
    void should_DeleteBannedKey_When_SavedAndThenDeleted() {
        BannedKey bannedKey = ServerTestData.createBannedKey("9999-8888-7777-6666");
        dao.save(bannedKey);
        assertThat(bannedKey.getId()).isNotNull();
        logger.info(bannedKey.getKey() + " saved with id " + bannedKey.getId());

        BannedKey lookup = dao.get(bannedKey.getId());
        dao.delete(lookup);
        logger.info("Should have deleted profile with id " + lookup.getId());

        BannedKey delete = dao.get(lookup.getId());
        assertThat(delete).isNull();
    }

    @Test
    @Rollback
    void should_LoadAllBannedKeys_When_MultipleSaved() {
        BannedKey key1 = ServerTestData.createBannedKey("0000-0000-1111-2222");
        BannedKey key2 = ServerTestData.createBannedKey("0000-0000-1111-3333");

        dao.save(key1);
        dao.save(key2);

        List<BannedKey> list = dao.getAll();
        for (BannedKey key : list) {
            logger.info("Loaded: " + key);
        }

        assertThat(list).contains(key1, key2);
    }

    @Test
    @Rollback
    void should_FindByKey_When_SingleKeyProvided() {
        String sKey = "0000-0000-1111-2222";
        BannedKey newKey = ServerTestData.createBannedKey(sKey);
        dao.save(newKey);
        assertThat(newKey.getId()).isNotNull();

        List<BannedKey> fetch = dao.getByKeys(sKey);
        assertThat(fetch).hasSize(1);
        assertThat(fetch.get(0).getKey()).isEqualTo(sKey);
    }

    @Test
    @Rollback
    void should_FindByMultipleKeys_When_MultipleKeysProvided() {
        String sKey = "0000-0000-1111-2222";
        BannedKey newKey = ServerTestData.createBannedKey(sKey);
        newKey.setUntil(new Date());
        dao.save(newKey);
        assertThat(newKey.getId()).isNotNull();

        String key2 = "dexter@example.com";
        BannedKey newKey2 = ServerTestData.createBannedKey(key2);
        dao.save(newKey2);
        assertThat(newKey2.getId()).isNotNull();

        List<BannedKey> fetch = dao.getByKeys(sKey, key2);
        assertThat(fetch).hasSize(2);
        assertThat(fetch.get(0).getKey()).isEqualTo(key2); // default date is later, should get returned first
        assertThat(fetch.get(1).getKey()).isEqualTo(sKey);
    }
}
