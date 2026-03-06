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
package com.donohoedigital.games.poker.gameserver.controller;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donohoedigital.games.poker.gameserver.persistence.repository.OnlineProfileRepository;
import com.donohoedigital.games.poker.gameserver.persistence.repository.TournamentHistoryRepository;
import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.model.TournamentHistory;
import com.donohoedigital.games.poker.protocol.dto.TournamentStatsDto;

/**
 * Tournament history endpoints - player statistics and tournament details.
 */
@RestController
@RequestMapping("/api/v1")
public class HistoryController {

    private final TournamentHistoryRepository historyRepository;
    private final OnlineProfileRepository profileRepository;

    public HistoryController(TournamentHistoryRepository historyRepository, OnlineProfileRepository profileRepository) {
        this.historyRepository = historyRepository;
        this.profileRepository = profileRepository;
    }

    /**
     * Get tournament history for a player by name, including aggregate stats.
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(@RequestParam("name") String name,
            @RequestParam(name = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "50") int pageSize) {

        OnlineProfile profile = profileRepository.findByName(name).orElse(null);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        Page<TournamentHistory> history = historyRepository.findByProfileId(profile.getId(), from, to,
                PageRequest.of(page, pageSize));

        Object[] agg = historyRepository.aggregateStats(profile.getId(), from, to);
        long totalGames = ((Number) agg[0]).longValue();
        long totalWins = ((Number) agg[1]).longValue();
        int totalPrize = ((Number) agg[2]).intValue();
        int totalBuyIn = ((Number) agg[3]).intValue();
        int bestFinish = ((Number) agg[4]).intValue();
        double avgPlacement = ((Number) agg[5]).doubleValue();
        double winRate = totalGames > 0 ? (totalWins * 100.0 / totalGames) : 0;

        TournamentStatsDto stats = new TournamentStatsDto((int) totalGames, (int) totalWins, totalPrize, totalBuyIn,
                totalPrize - totalBuyIn, bestFinish, avgPlacement, winRate);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", history.getContent());
        response.put("totalElements", history.getTotalElements());
        response.put("totalPages", history.getTotalPages());
        response.put("number", history.getNumber());
        response.put("size", history.getSize());
        response.put("stats", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Get tournament details (history entries) for a specific game.
     */
    @GetMapping("/tournaments/{id}")
    public ResponseEntity<Page<TournamentHistory>> getTournament(@PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "50") int pageSize) {

        Page<TournamentHistory> history = historyRepository.findByGameId(id, PageRequest.of(page, pageSize));
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(history);
    }
}
