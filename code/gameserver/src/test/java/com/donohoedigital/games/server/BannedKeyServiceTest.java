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

import com.donohoedigital.games.server.model.BannedKey;
import com.donohoedigital.games.server.service.BannedKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;

import static org.assertj.core.api.Assertions.*;

/**
 * Created by IntelliJ IDEA. User: donohoe Date: Mar 18, 2008 Time: 2:52:25 PM
 * To change this template use File | Settings | File Templates.
 */
@SpringJUnitConfig(locations = {"/app-context-jpatests.xml"})
@Transactional
class BannedKeyServiceTest {
    @Autowired
    private BannedKeyService service;

    @Test
    @Rollback
    void should_LookupAndDeleteBannedKey_When_SavedAndThenDeleted() {
        String key = "0000-0000-1111-2222";
        BannedKey key1 = ServerTestData.createBannedKey(key);

        service.saveBannedKey(key1);
        assertThat(service.isBanned(key)).isTrue();
        assertThat(service.getIfBanned(key).getKey()).isEqualTo(key);
        assertThat(service.isBanned("blah", key)).isTrue();

        service.deleteBannedKey(key);
        assertThat(service.isBanned(key)).isFalse();
    }

    @Test
    @Rollback
    void should_NotBeBanned_When_BanDateIsInPast() {
        String key = "0000-0000-1111-2222";
        BannedKey key1 = ServerTestData.createBannedKey(key);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        key1.setUntil(cal.getTime());
        service.saveBannedKey(key1);
        assertThat(service.isBanned(key)).isFalse();
        service.deleteBannedKey(key);
        assertThat(service.isBanned(key)).isFalse();
    }

    @Test
    @Rollback
    void should_BeBanned_When_BanDateIsTomorrow() {
        String key = "0000-0000-1111-4444";
        BannedKey key1 = ServerTestData.createBannedKey(key);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        key1.setUntil(cal.getTime());
        service.saveBannedKey(key1);
        assertThat(service.isBanned(key)).isTrue();
        service.deleteBannedKey(key);
        assertThat(service.isBanned(key)).isFalse();
    }

    @Test
    @Rollback
    void should_BeBanned_When_BanDateIsInFuture() {
        String key = "0000-0000-1111-3333";
        BannedKey key1 = ServerTestData.createBannedKey(key);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 100);
        key1.setUntil(cal.getTime());
        service.saveBannedKey(key1);
        assertThat(service.isBanned(key)).isTrue();
        assertThat(service.isBanned(key, null)).isTrue(); // test null in params
        service.deleteBannedKey(key);
        assertThat(service.isBanned(key)).isFalse();
    }

    @Test
    @Rollback
    void should_NotBeBanned_When_KeyIsNull() {
        assertThat(service.isBanned((String) null)).isFalse();
        assertThat(service.isBanned(null, null)).isFalse();
        assertThat(service.isBanned((String[]) null)).isFalse();
        assertThat(service.getIfBanned((String) null)).isNull();
    }
}
