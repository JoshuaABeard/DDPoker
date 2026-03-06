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
package com.donohoedigital.games.poker.gameserver.persistence.repository;

import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.donohoedigital.games.poker.model.TournamentHistory;

/**
 * Spring Data JPA repository for {@link TournamentHistory}.
 */
public interface TournamentHistoryRepository extends JpaRepository<TournamentHistory, Long> {

    /**
     * Find tournament histories for a profile, ordered by end date descending.
     */
    @Query("SELECT t FROM TournamentHistory t WHERE t.profile.id = :profileId"
            + " AND (:from IS NULL OR t.endDate >= :from)" + " AND (:to IS NULL OR t.endDate <= :to)"
            + " ORDER BY t.endDate DESC")
    Page<TournamentHistory> findByProfileId(@Param("profileId") Long profileId, @Param("from") Date from,
            @Param("to") Date to, Pageable pageable);

    /**
     * Aggregate tournament statistics for a player profile.
     */
    @Query("SELECT COUNT(t), " + "SUM(CASE WHEN t.place = 1 THEN 1 ELSE 0 END), " + "COALESCE(SUM(t.prize), 0), "
            + "COALESCE(SUM(t.buyin), 0), " + "COALESCE(MIN(t.place), 0), "
            + "COALESCE(AVG(CAST(t.place AS double)), 0) " + "FROM TournamentHistory t WHERE t.profile.id = :profileId"
            + " AND (:from IS NULL OR t.endDate >= :from)" + " AND (:to IS NULL OR t.endDate <= :to)")
    Object[] aggregateStats(@Param("profileId") Long profileId, @Param("from") Date from, @Param("to") Date to);

    /**
     * Find all tournament histories for a specific game, ordered by place.
     */
    @Query("SELECT t FROM TournamentHistory t WHERE t.onlineGame.id = :gameId ORDER BY t.place")
    Page<TournamentHistory> findByGameId(@Param("gameId") Long gameId, Pageable pageable);
}
