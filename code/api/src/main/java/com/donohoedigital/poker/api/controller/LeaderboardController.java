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
package com.donohoedigital.poker.api.controller;

import com.donohoedigital.games.poker.model.util.LeaderboardSummaryList;
import com.donohoedigital.games.poker.service.TournamentHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * Leaderboard endpoints - DDR1 and ROI rankings.
 */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    @Autowired
    private TournamentHistoryService historyService;

    /**
     * Get leaderboard rankings.
     */
    @GetMapping
    public ResponseEntity<LeaderboardSummaryList> getLeaderboard(@RequestParam(defaultValue = "ddr1") String mode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
            @RequestParam(required = false) String name, @RequestParam(defaultValue = "10") int gamesLimit,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int pageSize) {

        TournamentHistoryService.LeaderboardType type = "roi".equalsIgnoreCase(mode)
                ? TournamentHistoryService.LeaderboardType.roi
                : TournamentHistoryService.LeaderboardType.ddr1;

        int offset = page * pageSize;
        int total = historyService.getLeaderboardCount(gamesLimit, name, from, to);

        LeaderboardSummaryList leaderboard = historyService.getLeaderboard(total, offset, pageSize, type, gamesLimit,
                name, from, to);

        return ResponseEntity.ok(leaderboard);
    }
}
