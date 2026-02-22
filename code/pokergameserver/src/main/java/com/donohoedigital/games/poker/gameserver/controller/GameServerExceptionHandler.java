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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.donohoedigital.games.poker.gameserver.GameServerException;
import com.donohoedigital.games.poker.gameserver.GameServerException.ErrorCode;
import com.donohoedigital.games.poker.gameserver.dto.ErrorResponse;

/**
 * Global exception handler for consistent error responses across all REST
 * endpoints.
 */
@RestControllerAdvice
public class GameServerExceptionHandler {

    @ExceptionHandler(GameServerException.class)
    public ResponseEntity<ErrorResponse> handleGameServerException(GameServerException ex) {
        ErrorCode code = ex.getErrorCode();
        if (code == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", ex.getMessage()));
        }
        HttpStatus status = switch (code) {
            case GAME_NOT_FOUND, PLAYER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case WRONG_PASSWORD, NOT_GAME_OWNER -> HttpStatus.FORBIDDEN;
            case GAME_FULL, GAME_NOT_JOINABLE, GAME_ALREADY_STARTED, GAME_NOT_IN_LOBBY, GAME_COMPLETED, INVALID_GAME_STATE -> HttpStatus.CONFLICT;
            case WRONG_HOSTING_TYPE, NOT_APPLICABLE -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(code.name(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
