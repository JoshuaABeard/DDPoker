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
package com.donohoedigital.games.poker.gameserver.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query dataQuery;

    @Mock
    private Query countQuery;

    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(entityManager);
    }

    // =========================================================================
    // getLeaderboard
    // =========================================================================

    @Test
    void getLeaderboard_returnsFormattedEntries() {
        when(entityManager.createNativeQuery(contains("count(*)"))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(2L);

        when(entityManager.createNativeQuery(contains("ORDER BY"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);

        // Row: gamesplayed, profile_id, name, rank1, roi, addons, rebuys, buyin, prize
        Object[] row1 = {10, 1L, "Alice", 1500.0, 25.0, 100, 200, 1000, 1500};
        Object[] row2 = {8, 2L, "Bob", 1200.0, 10.0, 50, 100, 800, 900};
        List<Object[]> rows = List.of(row1, row2);
        when(dataQuery.getResultList()).thenReturn(rows);

        Map<String, Object> result = service.getLeaderboard(false, 5, null, null, null, 0, 20);

        assertThat(result.get("total")).isEqualTo(2);
        assertThat(result.get("page")).isEqualTo(0);
        assertThat(result.get("pageSize")).isEqualTo(20);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).get("playerName")).isEqualTo("Alice");
        assertThat(entries.get(0).get("rank")).isEqualTo(1);
        assertThat(entries.get(0).get("gamesPlayed")).isEqualTo(10);
        assertThat(entries.get(0).get("ddr1")).isEqualTo(1500);
        assertThat(entries.get(0).get("totalBuyin")).isEqualTo(1000);
        assertThat(entries.get(0).get("totalPrizes")).isEqualTo(1500);
        assertThat(entries.get(1).get("playerName")).isEqualTo("Bob");
        assertThat(entries.get(1).get("rank")).isEqualTo(2);
    }

    @Test
    void getLeaderboard_withNameSearch_setsNameParameter() {
        when(entityManager.createNativeQuery(contains("count(*)"))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        when(entityManager.createNativeQuery(contains("ORDER BY"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());

        service.getLeaderboard(true, 1, "Ali", null, null, 0, 10);

        // Verify name parameter was set with wildcard wrapping
        verify(countQuery).setParameter("name", "%Ali%");
        verify(dataQuery).setParameter("name", "%Ali%");
    }

    @Test
    void getLeaderboard_withNullDates_usesDefaults() {
        when(entityManager.createNativeQuery(contains("count(*)"))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        when(entityManager.createNativeQuery(contains("ORDER BY"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());

        Map<String, Object> result = service.getLeaderboard(false, 1, null, null, null, 0, 10);

        assertThat(result.get("total")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
        assertThat(entries).isEmpty();
    }

    @Test
    void getLeaderboard_pagination_setsCorrectOffset() {
        when(entityManager.createNativeQuery(contains("count(*)"))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(30L);

        when(entityManager.createNativeQuery(contains("ORDER BY"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);

        Object[] row = {5, 3L, "Charlie", 1000.0, 5.0, 10, 20, 500, 525};
        when(dataQuery.getResultList()).thenReturn(List.<Object[]>of(row));

        Map<String, Object> result = service.getLeaderboard(false, 1, null, null, null, 2, 10);

        verify(dataQuery).setFirstResult(20); // page 2 * pageSize 10
        verify(dataQuery).setMaxResults(10);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
        assertThat(entries.get(0).get("rank")).isEqualTo(21); // offset 20 + row 0 + 1
    }

    @Test
    void getLeaderboard_percentileCalculation() {
        when(entityManager.createNativeQuery(contains("count(*)"))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(100L);

        when(entityManager.createNativeQuery(contains("ORDER BY"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);

        Object[] row = {10, 1L, "TopPlayer", 2000.0, 50.0, 0, 0, 1000, 1500};
        when(dataQuery.getResultList()).thenReturn(List.<Object[]>of(row));

        Map<String, Object> result = service.getLeaderboard(false, 1, null, null, null, 0, 10);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) result.get("entries");
        // rank=1, total=100: percentile = 100 - (1*100/100) = 99
        assertThat(entries.get(0).get("percentile")).isEqualTo(99);
    }

    @Test
    void getLeaderboard_sortByRoi_usesRoiOrdering() {
        when(entityManager.createNativeQuery(contains("count(*)"))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        when(entityManager.createNativeQuery(contains("ORDER BY roi DESC"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());

        service.getLeaderboard(true, 1, null, null, null, 0, 10);

        // Verify it used ROI ordering by matching the query with "ORDER BY roi DESC"
        verify(entityManager).createNativeQuery(contains("ORDER BY roi DESC"));
    }

    @Test
    void getLeaderboard_withExplicitDates_passesThem() {
        Date from = new Date(1000000);
        Date to = new Date(2000000);

        when(entityManager.createNativeQuery(contains("count(*)"))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        when(entityManager.createNativeQuery(contains("ORDER BY"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());

        service.getLeaderboard(false, 1, null, from, to, 0, 10);

        verify(dataQuery).setParameter("begin", from);
        verify(dataQuery).setParameter("end", to);
    }

    // =========================================================================
    // getPlayerRank
    // =========================================================================

    @Test
    void getPlayerRank_returnsPlayerEntry() {
        when(entityManager.createNativeQuery(contains("wpr_name = :name"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);

        Object[] row = {15, 1L, "Alice", 1800.0, 30.0, 50, 100, 1500, 2000};
        when(dataQuery.getResultList()).thenReturn(List.<Object[]>of(row));

        List<Map<String, Object>> result = service.getPlayerRank("Alice", 5, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("playerName")).isEqualTo("Alice");
        assertThat(result.get(0).get("gamesPlayed")).isEqualTo(15);
        assertThat(result.get(0).get("ddr1")).isEqualTo(1800);
        assertThat(result.get(0).get("totalBuyin")).isEqualTo(1500);
        assertThat(result.get(0).get("totalPrizes")).isEqualTo(2000);
    }

    @Test
    void getPlayerRank_returnsEmptyWhenNotFound() {
        when(entityManager.createNativeQuery(contains("wpr_name = :name"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());

        List<Map<String, Object>> result = service.getPlayerRank("Nobody", 1, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getPlayerRank_withExplicitDates_passesThem() {
        Date from = new Date(1000000);
        Date to = new Date(2000000);

        when(entityManager.createNativeQuery(contains("wpr_name = :name"))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());

        service.getPlayerRank("Alice", 1, from, to);

        verify(dataQuery).setParameter("begin", from);
        verify(dataQuery).setParameter("end", to);
        verify(dataQuery).setParameter("name", "Alice");
    }
}
