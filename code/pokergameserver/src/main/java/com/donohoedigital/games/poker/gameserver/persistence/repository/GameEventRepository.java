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

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donohoedigital.games.poker.gameserver.persistence.entity.GameEventEntity;

/**
 * Spring Data JPA repository for {@link GameEventEntity}.
 */
public interface GameEventRepository extends JpaRepository<GameEventEntity, Long> {

    /**
     * Find all events for a game, ordered by sequence number.
     *
     * @param gameId
     *            the game ID
     * @return list of events in order
     */
    List<GameEventEntity> findByGameIdOrderBySequenceNumberAsc(String gameId);

    /**
     * Find events for a game after a specific sequence number.
     *
     * @param gameId
     *            the game ID
     * @param afterSequence
     *            the sequence number to start after
     * @return list of events with sequence > afterSequence, in order
     */
    List<GameEventEntity> findByGameIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(String gameId,
            long afterSequence);

    /**
     * Count events for a game.
     *
     * @param gameId
     *            the game ID
     * @return number of events
     */
    long countByGameId(String gameId);
}
