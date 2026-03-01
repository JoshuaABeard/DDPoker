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

import com.donohoedigital.games.poker.model.TournamentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link TournamentTemplate}.
 *
 * <p>
 * Provides CRUD operations and a query to find templates by profile ID.
 * </p>
 */
public interface TournamentTemplateRepository extends JpaRepository<TournamentTemplate, Long> {

    /**
     * Find all templates belonging to a profile, ordered by most recently modified
     * first.
     *
     * @param profileId
     *            the profile ID
     * @return list of templates for the profile
     */
    List<TournamentTemplate> findByProfileIdOrderByModifyDateDesc(Long profileId);
}
