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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donohoedigital.games.poker.gameserver.service.HandHistoryService;
import com.donohoedigital.games.poker.protocol.dto.HandDetailData;
import com.donohoedigital.games.poker.protocol.dto.HandExportData;
import com.donohoedigital.games.poker.protocol.dto.HandStatsData;
import com.donohoedigital.games.poker.protocol.dto.HandSummaryData;

/**
 * REST controller for hand history data within a game.
 */
@RestController
@RequestMapping("/api/v1/games/{gameId}/hands")
public class HandHistoryController {

    private final HandHistoryService handHistoryService;

    public HandHistoryController(HandHistoryService handHistoryService) {
        this.handHistoryService = handHistoryService;
    }

    @GetMapping
    public Page<HandSummaryData> listHands(@PathVariable("gameId") String gameId, Pageable pageable) {
        return handHistoryService.getHandSummaries(gameId, pageable);
    }

    @GetMapping("/count")
    public long countHands(@PathVariable("gameId") String gameId) {
        return handHistoryService.getHandCount(gameId);
    }

    @GetMapping("/{handId}")
    public ResponseEntity<HandDetailData> getHand(@PathVariable("gameId") String gameId,
            @PathVariable("handId") Long handId) {
        return handHistoryService.getHandDetail(gameId, handId).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public List<HandStatsData> getStats(@PathVariable("gameId") String gameId) {
        return handHistoryService.getHandStats(gameId);
    }

    @GetMapping("/export")
    public List<HandExportData> exportHands(@PathVariable("gameId") String gameId) {
        return handHistoryService.getHandsForExport(gameId);
    }
}
