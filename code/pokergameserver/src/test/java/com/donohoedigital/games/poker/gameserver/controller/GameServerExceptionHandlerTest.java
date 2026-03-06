/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.poker.gameserver.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.donohoedigital.games.poker.gameserver.GameServerException;
import com.donohoedigital.games.poker.gameserver.GameServerException.ErrorCode;
import com.donohoedigital.games.poker.protocol.dto.ErrorResponse;

/**
 * Unit tests for {@link GameServerExceptionHandler}.
 *
 * <p>
 * Tests verify that each exception type maps to the correct HTTP status and
 * that the response body carries the expected error code and message.
 * </p>
 */
class GameServerExceptionHandlerTest {

    private GameServerExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GameServerExceptionHandler();
    }

    // -------------------------------------------------------------------------
    // GameServerException — NOT_FOUND codes
    // -------------------------------------------------------------------------

    @Test
    void should_return404_when_GameNotFound() {
        GameServerException ex = new GameServerException(ErrorCode.GAME_NOT_FOUND, "game not found");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("GAME_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("game not found");
    }

    @Test
    void should_return404_when_PlayerNotFound() {
        GameServerException ex = new GameServerException(ErrorCode.PLAYER_NOT_FOUND, "player not found");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("PLAYER_NOT_FOUND");
    }

    // -------------------------------------------------------------------------
    // GameServerException — FORBIDDEN codes
    // -------------------------------------------------------------------------

    @Test
    void should_return403_when_WrongPassword() {
        GameServerException ex = new GameServerException(ErrorCode.WRONG_PASSWORD, "wrong password");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("WRONG_PASSWORD");
    }

    @Test
    void should_return403_when_NotGameOwner() {
        GameServerException ex = new GameServerException(ErrorCode.NOT_GAME_OWNER, "not owner");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("NOT_GAME_OWNER");
    }

    // -------------------------------------------------------------------------
    // GameServerException — CONFLICT codes
    // -------------------------------------------------------------------------

    @Test
    void should_return409_when_GameFull() {
        GameServerException ex = new GameServerException(ErrorCode.GAME_FULL, "game full");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("GAME_FULL");
    }

    @Test
    void should_return409_when_GameNotJoinable() {
        GameServerException ex = new GameServerException(ErrorCode.GAME_NOT_JOINABLE, "not joinable");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_return409_when_GameNotObservable() {
        GameServerException ex = new GameServerException(ErrorCode.GAME_NOT_OBSERVABLE, "not observable");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_return409_when_GameAlreadyStarted() {
        GameServerException ex = new GameServerException(ErrorCode.GAME_ALREADY_STARTED, "already started");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_return409_when_GameNotInLobby() {
        GameServerException ex = new GameServerException(ErrorCode.GAME_NOT_IN_LOBBY, "not in lobby");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_return409_when_GameCompleted() {
        GameServerException ex = new GameServerException(ErrorCode.GAME_COMPLETED, "game completed");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_return409_when_InvalidGameState() {
        GameServerException ex = new GameServerException(ErrorCode.INVALID_GAME_STATE, "invalid state");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // -------------------------------------------------------------------------
    // GameServerException — UNPROCESSABLE_ENTITY codes
    // -------------------------------------------------------------------------

    @Test
    void should_return422_when_WrongHostingType() {
        GameServerException ex = new GameServerException(ErrorCode.WRONG_HOSTING_TYPE, "wrong hosting");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo("WRONG_HOSTING_TYPE");
    }

    @Test
    void should_return422_when_NotApplicable() {
        GameServerException ex = new GameServerException(ErrorCode.NOT_APPLICABLE, "not applicable");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // -------------------------------------------------------------------------
    // GameServerException — null error code (internal error)
    // -------------------------------------------------------------------------

    @Test
    void should_return500_when_ErrorCodeIsNull() {
        GameServerException ex = new GameServerException("internal failure");
        ResponseEntity<ErrorResponse> response = handler.handleGameServerException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("internal failure");
    }

    // -------------------------------------------------------------------------
    // IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void should_return400_when_IllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("bad argument");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("bad argument");
    }

    // -------------------------------------------------------------------------
    // IllegalStateException
    // -------------------------------------------------------------------------

    @Test
    void should_return500_when_IllegalStateException() {
        IllegalStateException ex = new IllegalStateException("bad state");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("bad state");
    }

    // -------------------------------------------------------------------------
    // Generic Exception
    // -------------------------------------------------------------------------

    @Test
    void should_return500_when_GenericException() {
        Exception ex = new RuntimeException("unexpected");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    }
}
