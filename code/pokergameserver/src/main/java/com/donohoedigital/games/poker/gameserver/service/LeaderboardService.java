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

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.donohoedigital.games.poker.model.TournamentHistory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Service for leaderboard queries using native SQL (complex aggregation queries
 * that cannot be expressed with Spring Data method naming).
 */
@Service
public class LeaderboardService {

    private static final Date BEGINNING_OF_TIME = new Date(0);
    private static final Date END_OF_TIME = new GregorianCalendar(2099, Calendar.DECEMBER, 31, 23, 23, 59).getTime();

    private final EntityManager entityManager;

    public LeaderboardService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Get leaderboard rankings with aggregated stats.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getLeaderboard(boolean sortByRoi, int gamesLimit, String nameSearch, Date from, Date to,
            int page, int pageSize) {

        if (from == null)
            from = BEGINNING_OF_TIME;
        if (to == null)
            to = END_OF_TIME;
        boolean hasName = nameSearch != null && !nameSearch.isEmpty();

        // Count query
        int total = getLeaderboardCount(gamesLimit, nameSearch, from, to, hasName);

        // Data query
        Query query = entityManager.createNativeQuery(
                "SELECT count(whi_profile_id) AS gamesplayed, " + "whi_profile_id, wpr_name, avg(whi_rank_1) AS rank1, "
                        + "sum(whi_prize-(whi_buy_in+whi_total_rebuy+whi_total_add_on)) / "
                        + "    sum(whi_buy_in+whi_total_rebuy+whi_total_add_on)*100 AS roi, "
                        + "sum(whi_total_add_on), sum(whi_total_rebuy), sum(whi_buy_in), sum(whi_prize) "
                        + "FROM wan_history, wan_profile " + "WHERE wan_history.whi_profile_id = wan_profile.wpr_id "
                        + "  AND whi_player_type IN (:type1, :type2) " + "  AND whi_is_ended = TRUE "
                        + "  AND wpr_is_retired = FALSE " + "  AND whi_end_date >= :begin AND whi_end_date <= :end "
                        + (hasName ? "  AND wpr_name LIKE :name " : "") + "GROUP BY whi_profile_id, wpr_name "
                        + "HAVING count(whi_profile_id) >= :game_limit "
                        + (sortByRoi ? "ORDER BY roi DESC, wpr_name" : "ORDER BY rank1 DESC, wpr_name"));

        setCommonParams(query, from, to, gamesLimit, nameSearch, hasName);
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);

        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> entries = new ArrayList<>();
        int offset = page * pageSize;
        int row = 0;
        for (Object[] a : results) {
            Map<String, Object> entry = new HashMap<>();
            int rank = offset + row + 1;
            entry.put("rank", rank);
            entry.put("percentile", total > 0 ? 100 - (rank * 100 / total) : 0);
            entry.put("gamesPlayed", ((Number) a[0]).intValue());
            entry.put("profileId", a[1]);
            entry.put("playerName", a[2]);
            entry.put("ddr1", ((Number) a[3]).intValue());
            entry.put("totalAddon", ((Number) a[5]).intValue());
            entry.put("totalRebuys", ((Number) a[6]).intValue());
            entry.put("totalBuyin", ((Number) a[7]).intValue());
            entry.put("totalPrizes", ((Number) a[8]).intValue());
            entries.add(entry);
            row++;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("entries", entries);
        response.put("total", total);
        response.put("page", page);
        response.put("pageSize", pageSize);
        return response;
    }

    /**
     * Get a single player's leaderboard entry.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPlayerRank(String name, int gamesLimit, Date from, Date to) {
        if (from == null)
            from = BEGINNING_OF_TIME;
        if (to == null)
            to = END_OF_TIME;

        Query query = entityManager.createNativeQuery(
                "SELECT count(whi_profile_id) AS gamesplayed, " + "whi_profile_id, wpr_name, avg(whi_rank_1) AS rank1, "
                        + "sum(whi_prize-(whi_buy_in+whi_total_rebuy+whi_total_add_on)) / "
                        + "    sum(whi_buy_in+whi_total_rebuy+whi_total_add_on)*100 AS roi, "
                        + "sum(whi_total_add_on), sum(whi_total_rebuy), sum(whi_buy_in), sum(whi_prize) "
                        + "FROM wan_history, wan_profile " + "WHERE wan_history.whi_profile_id = wan_profile.wpr_id "
                        + "  AND whi_player_type IN (:type1, :type2) " + "  AND whi_is_ended = TRUE "
                        + "  AND wpr_is_retired = FALSE " + "  AND whi_end_date >= :begin AND whi_end_date <= :end "
                        + "  AND wpr_name = :name " + "GROUP BY whi_profile_id, wpr_name "
                        + "HAVING count(whi_profile_id) >= :game_limit");

        query.setParameter("type1", TournamentHistory.PLAYER_TYPE_ONLINE);
        query.setParameter("type2", TournamentHistory.PLAYER_TYPE_AI);
        query.setParameter("begin", from);
        query.setParameter("end", to);
        query.setParameter("game_limit", (long) gamesLimit);
        query.setParameter("name", name);

        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Object[] a : results) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("gamesPlayed", ((Number) a[0]).intValue());
            entry.put("profileId", a[1]);
            entry.put("playerName", a[2]);
            entry.put("ddr1", ((Number) a[3]).intValue());
            entry.put("totalAddon", ((Number) a[5]).intValue());
            entry.put("totalRebuys", ((Number) a[6]).intValue());
            entry.put("totalBuyin", ((Number) a[7]).intValue());
            entry.put("totalPrizes", ((Number) a[8]).intValue());
            entries.add(entry);
        }
        return entries;
    }

    private int getLeaderboardCount(int gamesLimit, String nameSearch, Date from, Date to, boolean hasName) {
        Query countQuery = entityManager
                .createNativeQuery("SELECT count(*) FROM " + "(SELECT count(1) FROM wan_history, wan_profile "
                        + " WHERE wan_history.whi_profile_id = wan_profile.wpr_id "
                        + "   AND whi_player_type IN (:type1, :type2) " + "   AND whi_is_ended = TRUE "
                        + "   AND wpr_is_retired = FALSE " + "   AND whi_end_date >= :begin AND whi_end_date <= :end "
                        + (hasName ? "   AND wpr_name LIKE :name " : "") + " GROUP BY wpr_name "
                        + " HAVING count(whi_profile_id) >= :game_limit) foo");

        setCommonParams(countQuery, from, to, gamesLimit, nameSearch, hasName);
        return ((Number) countQuery.getSingleResult()).intValue();
    }

    private void setCommonParams(Query query, Date from, Date to, int gamesLimit, String nameSearch, boolean hasName) {
        query.setParameter("type1", TournamentHistory.PLAYER_TYPE_ONLINE);
        query.setParameter("type2", TournamentHistory.PLAYER_TYPE_AI);
        query.setParameter("begin", from);
        query.setParameter("end", to);
        query.setParameter("game_limit", (long) gamesLimit);
        if (hasName) {
            query.setParameter("name", "%" + nameSearch + "%");
        }
    }
}
