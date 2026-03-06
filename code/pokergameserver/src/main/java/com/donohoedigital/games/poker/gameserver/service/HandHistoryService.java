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

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donohoedigital.games.poker.gameserver.persistence.entity.HandActionEntity;
import com.donohoedigital.games.poker.gameserver.persistence.entity.HandHistoryEntity;
import com.donohoedigital.games.poker.gameserver.persistence.entity.HandPlayerEntity;
import com.donohoedigital.games.poker.gameserver.persistence.repository.HandActionRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.HandHistoryRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.HandPlayerRepository;

/**
 * Service for storing and querying hand history data.
 */
@Service
@Transactional
public class HandHistoryService {

    private final HandHistoryRepository handHistoryRepository;
    private final HandPlayerRepository handPlayerRepository;
    private final HandActionRepository handActionRepository;

    public HandHistoryService(HandHistoryRepository handHistoryRepository, HandPlayerRepository handPlayerRepository,
            HandActionRepository handActionRepository) {
        this.handHistoryRepository = handHistoryRepository;
        this.handPlayerRepository = handPlayerRepository;
        this.handActionRepository = handActionRepository;
    }

    /**
     * Store a complete hand with players and actions.
     *
     * @param gameId
     *            the game ID
     * @param hand
     *            the hand history entity
     * @param players
     *            the player entities
     * @param actions
     *            the action entities
     */
    public void storeHand(String gameId, HandHistoryEntity hand, List<HandPlayerEntity> players,
            List<HandActionEntity> actions) {
        hand.setGameId(gameId);
        HandHistoryEntity saved = handHistoryRepository.save(hand);
        Long handId = saved.getId();

        for (HandPlayerEntity player : players) {
            player.setHandId(handId);
        }
        handPlayerRepository.saveAll(players);

        for (HandActionEntity action : actions) {
            action.setHandId(handId);
        }
        handActionRepository.saveAll(actions);
    }

    /**
     * Count hands for a game.
     */
    @Transactional(readOnly = true)
    public long getHandCount(String gameId) {
        return handHistoryRepository.countByGameId(gameId);
    }

    /**
     * Get paginated hand summaries for a game.
     */
    @Transactional(readOnly = true)
    public Page<HandHistoryEntity> getHandSummaries(String gameId, Pageable pageable) {
        return handHistoryRepository.findByGameIdOrderByHandNumberDesc(gameId, pageable);
    }

    /**
     * Get full hand detail including players and actions.
     *
     * @return the hand entity, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<HandDetail> getHandDetail(String gameId, Long handId) {
        return handHistoryRepository.findByGameIdAndId(gameId, handId).map(hand -> {
            List<HandPlayerEntity> players = handPlayerRepository.findByHandId(handId);
            List<HandActionEntity> actions = handActionRepository.findByHandIdOrderBySequenceAsc(handId);
            return new HandDetail(hand, players, actions);
        });
    }

    /**
     * Get all hands with full data for export.
     */
    @Transactional(readOnly = true)
    public List<HandDetail> getHandsForExport(String gameId) {
        List<HandHistoryEntity> hands = handHistoryRepository.findByGameId(gameId);
        if (hands.isEmpty()) {
            return List.of();
        }

        List<Long> handIds = hands.stream().map(HandHistoryEntity::getId).toList();
        List<HandPlayerEntity> allPlayers = handPlayerRepository.findByHandIdIn(handIds);
        List<HandActionEntity> allActions = handActionRepository.findByHandIdIn(handIds);

        return hands.stream().map(hand -> {
            Long hid = hand.getId();
            List<HandPlayerEntity> players = allPlayers.stream().filter(p -> hid.equals(p.getHandId())).toList();
            List<HandActionEntity> actions = allActions.stream().filter(a -> hid.equals(a.getHandId())).toList();
            return new HandDetail(hand, players, actions);
        }).toList();
    }

    /**
     * Complete hand data including players and actions.
     */
    public record HandDetail(HandHistoryEntity hand, List<HandPlayerEntity> players, List<HandActionEntity> actions) {
    }
}
