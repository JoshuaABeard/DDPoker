/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver.persistence.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.persistence.entity.GameInstanceEntity;

/**
 * Spring Data JPA repository for {@link GameInstanceEntity}.
 */
public interface GameInstanceRepository extends JpaRepository<GameInstanceEntity, String> {

    /**
     * Find all games with a specific status.
     *
     * @param status
     *            the game status
     * @return list of matching games
     */
    List<GameInstanceEntity> findByStatus(GameInstanceState status);

    /**
     * Find all games with a specific hosting type.
     *
     * @param hostingType
     *            the hosting type ("SERVER" or "COMMUNITY")
     * @return list of matching games
     */
    List<GameInstanceEntity> findByHostingType(String hostingType);

    /**
     * Find all games owned by a specific profile.
     *
     * @param ownerProfileId
     *            the owner's profile ID
     * @return list of games owned by this profile
     */
    List<GameInstanceEntity> findByOwnerProfileId(Long ownerProfileId);

    /**
     * Update game status and started time.
     *
     * @param gameId
     *            the game ID
     * @param status
     *            the new status
     * @param startedAt
     *            the start time
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE GameInstanceEntity g SET g.status = :status, g.startedAt = :startedAt WHERE g.gameId = :gameId")
    void updateStatusWithStartTime(@Param("gameId") String gameId, @Param("status") GameInstanceState status,
            @Param("startedAt") Instant startedAt);

    /**
     * Update game status and completion time.
     *
     * @param gameId
     *            the game ID
     * @param status
     *            the new status
     * @param completedAt
     *            the completion time
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE GameInstanceEntity g SET g.status = :status, g.completedAt = :completedAt WHERE g.gameId = :gameId")
    void updateStatusWithCompletionTime(@Param("gameId") String gameId, @Param("status") GameInstanceState status,
            @Param("completedAt") Instant completedAt);

    /**
     * Update game status only.
     *
     * @param gameId
     *            the game ID
     * @param status
     *            the new status
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE GameInstanceEntity g SET g.status = :status WHERE g.gameId = :gameId")
    void updateStatus(@Param("gameId") String gameId, @Param("status") GameInstanceState status);

    /**
     * Update player count (absolute value). Prefer the atomic increment/decrement
     * methods for concurrent updates.
     *
     * @param gameId
     *            the game ID
     * @param count
     *            the new player count
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE GameInstanceEntity g SET g.playerCount = :count WHERE g.gameId = :gameId")
    void updatePlayerCount(@Param("gameId") String gameId, @Param("count") int count);

    /**
     * Atomically increment player count by 1.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE GameInstanceEntity g SET g.playerCount = g.playerCount + 1 WHERE g.gameId = :gameId")
    void incrementPlayerCount(@Param("gameId") String gameId);

    /**
     * Atomically decrement player count by 1, clamped to 0.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE GameInstanceEntity g "
            + "SET g.playerCount = CASE WHEN g.playerCount > 0 THEN g.playerCount - 1 ELSE 0 END "
            + "WHERE g.gameId = :gameId")
    void decrementPlayerCount(@Param("gameId") String gameId);

    /**
     * Find games filtered by status, hosting type, and name/owner search term.
     * Status filter is required; hosting type and search pattern are optional (pass
     * {@code null} to skip). Search pattern should use SQL LIKE syntax (e.g.
     * {@code "%term%"}) and is matched case-insensitively.
     */
    @Query("SELECT g FROM GameInstanceEntity g WHERE g.status IN :statuses "
            + "AND (:hostingType IS NULL OR g.hostingType = :hostingType) "
            + "AND (:searchPattern IS NULL OR LOWER(g.name) LIKE :searchPattern "
            + "     OR LOWER(g.ownerName) LIKE :searchPattern) " + "ORDER BY g.createdAt DESC")
    List<GameInstanceEntity> findGamesFiltered(@Param("statuses") List<GameInstanceState> statuses,
            @Param("hostingType") String hostingType, @Param("searchPattern") String searchPattern);

    /**
     * Find COMMUNITY games whose last heartbeat is older than the given cutoff, or
     * that have never sent a heartbeat (null). Used for stale-game cleanup.
     */
    @Query("SELECT g FROM GameInstanceEntity g WHERE g.hostingType = 'COMMUNITY' "
            + "AND g.status IN ('WAITING_FOR_PLAYERS', 'IN_PROGRESS') "
            + "AND (g.lastHeartbeat IS NULL OR g.lastHeartbeat < :cutoff)")
    List<GameInstanceEntity> findStaleCommunityGames(@Param("cutoff") Instant cutoff);

    /**
     * Find SERVER games that have been in WAITING_FOR_PLAYERS longer than the given
     * cutoff (abandoned lobbies).
     */
    @Query("SELECT g FROM GameInstanceEntity g WHERE g.hostingType = 'SERVER' "
            + "AND g.status = 'WAITING_FOR_PLAYERS' AND g.createdAt < :cutoff")
    List<GameInstanceEntity> findAbandonedServerLobbies(@Param("cutoff") Instant cutoff);

    /**
     * Find games in terminal states (COMPLETED, CANCELLED) older than the retention
     * cutoff.
     */
    @Query("SELECT g FROM GameInstanceEntity g WHERE g.status IN ('COMPLETED', 'CANCELLED') "
            + "AND g.completedAt < :cutoff")
    List<GameInstanceEntity> findExpiredGames(@Param("cutoff") Instant cutoff);

    /**
     * Update the last_heartbeat timestamp for a community-hosted game.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE GameInstanceEntity g SET g.lastHeartbeat = :now WHERE g.gameId = :gameId")
    void updateHeartbeat(@Param("gameId") String gameId, @Param("now") Instant now);
}
