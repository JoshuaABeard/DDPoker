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
package com.donohoedigital.games.poker.network;

import com.donohoedigital.comms.DMArrayList;
import com.donohoedigital.comms.DMTypedHashMap;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OnlinePlayerInfo - player information data class
 */
class OnlinePlayerInfoTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateEmptyInstance_When_DefaultConstructorUsed() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();

        assertThat(info.getData()).isNotNull();
    }

    @Test
    void should_CreateInstance_When_GivenDMTypedHashMap() {
        DMTypedHashMap data = new DMTypedHashMap();
        data.setString(OnlinePlayerInfo.ONLINE_NAME, "TestPlayer");

        OnlinePlayerInfo info = new OnlinePlayerInfo(data);

        assertThat(info.getData()).isEqualTo(data);
        assertThat(info.getName()).isEqualTo("TestPlayer");
    }

    // =================================================================
    // Name Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveName() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("Alice");

        assertThat(info.getName()).isEqualTo("Alice");
    }

    @Test
    void should_ReturnLowercaseName_When_GetNameLowerCalled() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("AlIcE");

        assertThat(info.getNameLower()).isEqualTo("alice");
    }

    @Test
    void should_ReturnConsistentLowercaseName_When_NameHasMixedCase() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("TEST_Player_123");

        assertThat(info.getNameLower()).isEqualTo("test_player_123");
    }

    // =================================================================
    // Player ID Tests
    // =================================================================

    @Test
    void should_StoreAndRetrievePlayerId() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setPlayerId("player-uuid-123");

        assertThat(info.getPlayerId()).isEqualTo("player-uuid-123");
    }

    @Test
    void should_SetPlayerId_When_UsingDeprecatedMethod() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setPublicUseKey("deprecated-key");

        assertThat(info.getPlayerId()).isEqualTo("deprecated-key");
    }

    // =================================================================
    // Create Date Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveCreateDate_When_GivenLong() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        Long timestamp = 1609459200000L; // 2021-01-01 00:00:00 UTC
        info.setCreateDate(timestamp);

        assertThat(info.getCreateDate()).isEqualTo(timestamp);
    }

    @Test
    void should_StoreAndRetrieveCreateDate_When_GivenDate() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        Date date = new Date(1609459200000L);
        info.setCreateDate(date);

        assertThat(info.getCreateDate()).isEqualTo(date.getTime());
    }

    @Test
    void should_ReturnZero_When_CreateDateNotSet() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();

        assertThat(info.getCreateDate()).isEqualTo(0L);
    }

    // =================================================================
    // Aliases Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveAliases() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setPlayerId("player-123");

        DMArrayList<DMTypedHashMap> aliases = new DMArrayList<>();

        DMTypedHashMap alias1 = new DMTypedHashMap();
        alias1.setString(OnlinePlayerInfo.ONLINE_NAME, "Alias1");
        aliases.add(alias1);

        DMTypedHashMap alias2 = new DMTypedHashMap();
        alias2.setString(OnlinePlayerInfo.ONLINE_NAME, "Alias2");
        aliases.add(alias2);

        info.setAliases(aliases);

        List<OnlinePlayerInfo> retrieved = info.getAliases();
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(0).getName()).isEqualTo("Alias1");
        assertThat(retrieved.get(1).getName()).isEqualTo("Alias2");
        // Aliases should inherit the player ID
        assertThat(retrieved.get(0).getPlayerId()).isEqualTo("player-123");
        assertThat(retrieved.get(1).getPlayerId()).isEqualTo("player-123");
    }

    @Test
    void should_ReturnNull_When_AliasesNotSet() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();

        assertThat(info.getAliases()).isNull();
    }

    @Test
    void should_ReturnEmptyList_When_AliasesSetToEmptyList() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setAliases(new DMArrayList<>());

        List<OnlinePlayerInfo> retrieved = info.getAliases();
        assertThat(retrieved).isEmpty();
    }

    // =================================================================
    // Equality Tests
    // =================================================================

    @Test
    void should_BeEqual_When_SameNameAndPlayerId() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");
        info1.setPlayerId("player-123");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Alice");
        info2.setPlayerId("player-123");

        assertThat(info1).isEqualTo(info2);
    }

    @Test
    void should_BeEqual_When_SameNameDifferentCase() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");
        info1.setPlayerId("player-123");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("ALICE");
        info2.setPlayerId("player-123");

        assertThat(info1).isEqualTo(info2);
    }

    @Test
    void should_NotBeEqual_When_DifferentName() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");
        info1.setPlayerId("player-123");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Bob");
        info2.setPlayerId("player-123");

        assertThat(info1).isNotEqualTo(info2);
    }

    @Test
    void should_NotBeEqual_When_DifferentPlayerId() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");
        info1.setPlayerId("player-123");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Alice");
        info2.setPlayerId("player-456");

        assertThat(info1).isNotEqualTo(info2);
    }

    @Test
    void should_BeEqualToItself_When_SameInstance() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("Alice");
        info.setPlayerId("player-123");

        assertThat(info).isEqualTo(info);
    }

    @Test
    void should_NotBeEqual_When_ComparedToNull() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("Alice");
        info.setPlayerId("player-123");

        assertThat(info).isNotEqualTo(null);
    }

    @Test
    void should_NotBeEqual_When_ComparedToDifferentType() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("Alice");
        info.setPlayerId("player-123");

        assertThat(info).isNotEqualTo("not a player info");
    }

    @Test
    void should_SatisfyEqualsReflexive() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("Alice");
        info.setPlayerId("player-123");

        assertThat(info.equals(info)).isTrue();
    }

    @Test
    void should_SatisfyEqualsSymmetric() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");
        info1.setPlayerId("player-123");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Alice");
        info2.setPlayerId("player-123");

        assertThat(info1.equals(info2)).isTrue();
        assertThat(info2.equals(info1)).isTrue();
    }

    @Test
    void should_SatisfyEqualsTransitive() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");
        info1.setPlayerId("player-123");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Alice");
        info2.setPlayerId("player-123");

        OnlinePlayerInfo info3 = new OnlinePlayerInfo();
        info3.setName("Alice");
        info3.setPlayerId("player-123");

        assertThat(info1.equals(info2)).isTrue();
        assertThat(info2.equals(info3)).isTrue();
        assertThat(info1.equals(info3)).isTrue();
    }

    // =================================================================
    // HashCode Tests
    // =================================================================

    @Test
    void should_HaveConsistentHashCode_When_CalledMultipleTimes() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("Alice");
        info.setPlayerId("player-123");

        int hash1 = info.hashCode();
        int hash2 = info.hashCode();

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void should_HaveSameHashCode_When_ObjectsAreEqual() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");
        info1.setPlayerId("player-123");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("ALICE"); // Different case, but equals() treats as equal
        info2.setPlayerId("player-123");

        assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
    }

    @Test
    void should_LikelyHaveDifferentHashCode_When_ObjectsNotEqual() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");
        info1.setPlayerId("player-123");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Bob");
        info2.setPlayerId("player-456");

        // Not required by contract, but likely for good hash function
        assertThat(info1.hashCode()).isNotEqualTo(info2.hashCode());
    }

    // =================================================================
    // CompareTo Tests
    // =================================================================

    @Test
    void should_ReturnZero_When_ComparingToSameName() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Alice");

        assertThat(info1.compareTo(info2)).isEqualTo(0);
    }

    @Test
    void should_ReturnZero_When_ComparingToSameNameDifferentCase() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("ALICE");

        assertThat(info1.compareTo(info2)).isEqualTo(0);
    }

    @Test
    void should_ReturnNegative_When_NameComesBeforeAlphabetically() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Alice");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Bob");

        assertThat(info1.compareTo(info2)).isLessThan(0);
    }

    @Test
    void should_ReturnPositive_When_NameComesAfterAlphabetically() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Zoe");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Alice");

        assertThat(info1.compareTo(info2)).isGreaterThan(0);
    }

    @Test
    void should_SortCorrectly_When_UsedInList() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("Charlie");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("Alice");

        OnlinePlayerInfo info3 = new OnlinePlayerInfo();
        info3.setName("Bob");

        List<OnlinePlayerInfo> list = new java.util.ArrayList<>();
        list.add(info1);
        list.add(info2);
        list.add(info3);

        list.sort(null); // Uses natural ordering (Comparable)

        assertThat(list.get(0).getName()).isEqualTo("Alice");
        assertThat(list.get(1).getName()).isEqualTo("Bob");
        assertThat(list.get(2).getName()).isEqualTo("Charlie");
    }

    @Test
    void should_BeCaseInsensitive_When_Sorting() {
        OnlinePlayerInfo info1 = new OnlinePlayerInfo();
        info1.setName("bob");

        OnlinePlayerInfo info2 = new OnlinePlayerInfo();
        info2.setName("ALICE");

        OnlinePlayerInfo info3 = new OnlinePlayerInfo();
        info3.setName("Charlie");

        List<OnlinePlayerInfo> list = new java.util.ArrayList<>();
        list.add(info1);
        list.add(info2);
        list.add(info3);

        list.sort(null);

        assertThat(list.get(0).getName()).isEqualTo("ALICE");
        assertThat(list.get(1).getName()).isEqualTo("bob");
        assertThat(list.get(2).getName()).isEqualTo("Charlie");
    }

    // =================================================================
    // Data Accessor Tests
    // =================================================================

    @Test
    void should_ReturnUnderlyingData_When_GetDataCalled() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        info.setName("TestPlayer");

        DMTypedHashMap data = info.getData();
        assertThat(data).isNotNull();
        assertThat(data.getString(OnlinePlayerInfo.ONLINE_NAME)).isEqualTo("TestPlayer");
    }

    @Test
    void should_ShareSameDataInstance_When_ModifyingData() {
        OnlinePlayerInfo info = new OnlinePlayerInfo();
        DMTypedHashMap data = info.getData();
        data.setString(OnlinePlayerInfo.ONLINE_NAME, "DirectlySet");

        assertThat(info.getName()).isEqualTo("DirectlySet");
    }
}
