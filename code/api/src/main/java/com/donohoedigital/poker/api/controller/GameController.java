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

import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.model.util.OnlineGameList;
import com.donohoedigital.games.poker.service.OnlineGameService;
import com.donohoedigital.poker.api.dto.GameListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * Game endpoints - list games by mode, get game details.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private OnlineGameService gameService;

    /**
     * Get games list filtered by mode. Modes: 0=available, 1=running, 2=ended
     */
    @GetMapping
    public ResponseEntity<GameListResponse> getGames(
            @RequestParam(required = false, defaultValue = "0,1,2") String modes,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int pageSize) {

        // Parse modes
        String[] modeStrs = modes.split(",");
        Integer[] modeArray = new Integer[modeStrs.length];
        for (int i = 0; i < modeStrs.length; i++) {
            modeArray[i] = Integer.parseInt(modeStrs[i].trim());
        }

        // Get total count
        int total = gameService.getOnlineGamesCount(modeArray, search, from, to);

        // Get page of games
        int offset = page * pageSize;
        OnlineGameList gameList = gameService.getOnlineGames(total, offset, pageSize, modeArray, search, from, to,
                OnlineGameService.OrderByType.date);

        return ResponseEntity.ok(new GameListResponse(gameList, total, page, pageSize));
    }

    /**
     * Get single game details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OnlineGame> getGame(@PathVariable Long id) {
        OnlineGame game = gameService.getOnlineGameById(id);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(game);
    }

    /**
     * Get host summary statistics.
     */
    @GetMapping("/hosts")
    public ResponseEntity<?> getHosts(@RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int pageSize) {

        int total = gameService.getHostSummaryCount(search, from, to);
        int offset = page * pageSize;

        return ResponseEntity.ok(gameService.getHostSummary(total, offset, pageSize, search, from, to));
    }
}
