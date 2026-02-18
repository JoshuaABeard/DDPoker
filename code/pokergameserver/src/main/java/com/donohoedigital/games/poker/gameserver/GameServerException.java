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
package com.donohoedigital.games.poker.gameserver;

/**
 * Runtime exception for game server errors. Carries a typed {@link ErrorCode}
 * for HTTP mapping.
 */
public class GameServerException extends RuntimeException {

    /**
     * Typed error codes mapped to HTTP status codes in GameServerExceptionHandler.
     */
    public enum ErrorCode {
        GAME_NOT_FOUND, GAME_FULL, WRONG_PASSWORD, GAME_NOT_JOINABLE, NOT_GAME_OWNER, WRONG_HOSTING_TYPE, GAME_ALREADY_STARTED, GAME_NOT_IN_LOBBY, PLAYER_NOT_FOUND, GAME_COMPLETED, NOT_APPLICABLE
    }

    private final ErrorCode errorCode;

    public GameServerException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GameServerException(String message) {
        super(message);
        this.errorCode = null;
    }

    public GameServerException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
