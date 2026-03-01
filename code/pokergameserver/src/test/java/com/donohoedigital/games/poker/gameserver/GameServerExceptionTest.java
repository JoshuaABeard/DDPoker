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
package com.donohoedigital.games.poker.gameserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.gameserver.GameServerException.ErrorCode;

/**
 * Unit tests for {@link GameServerException} construction variants.
 */
class GameServerExceptionTest {

    @Test
    void should_storeErrorCode_when_constructedWithErrorCode() {
        GameServerException ex = new GameServerException(ErrorCode.GAME_NOT_FOUND, "not found");
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.GAME_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("not found");
    }

    @Test
    void should_haveNullErrorCode_when_constructedWithMessageOnly() {
        GameServerException ex = new GameServerException("something went wrong");
        assertThat(ex.getErrorCode()).isNull();
        assertThat(ex.getMessage()).isEqualTo("something went wrong");
    }

    @Test
    void should_haveNullErrorCode_when_constructedWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        GameServerException ex = new GameServerException("wrapped error", cause);
        assertThat(ex.getErrorCode()).isNull();
        assertThat(ex.getMessage()).isEqualTo("wrapped error");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void should_coverAllErrorCodes() {
        // Verify all error codes are valid enum values
        ErrorCode[] codes = ErrorCode.values();
        assertThat(codes).hasSizeGreaterThan(0);
        assertThat(codes).contains(ErrorCode.GAME_NOT_FOUND, ErrorCode.GAME_FULL, ErrorCode.WRONG_PASSWORD,
                ErrorCode.GAME_NOT_JOINABLE, ErrorCode.GAME_NOT_OBSERVABLE, ErrorCode.NOT_GAME_OWNER,
                ErrorCode.WRONG_HOSTING_TYPE, ErrorCode.GAME_ALREADY_STARTED, ErrorCode.GAME_NOT_IN_LOBBY,
                ErrorCode.PLAYER_NOT_FOUND, ErrorCode.GAME_COMPLETED, ErrorCode.NOT_APPLICABLE,
                ErrorCode.INVALID_GAME_STATE);
    }

    @Test
    void should_supportErrorCodeValueOf() {
        assertThat(ErrorCode.valueOf("GAME_NOT_FOUND")).isEqualTo(ErrorCode.GAME_NOT_FOUND);
        assertThat(ErrorCode.valueOf("INVALID_GAME_STATE")).isEqualTo(ErrorCode.INVALID_GAME_STATE);
    }
}
