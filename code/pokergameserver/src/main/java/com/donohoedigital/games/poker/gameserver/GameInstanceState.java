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
 * Lifecycle states for a GameInstance.
 *
 * <p>
 * State transitions:
 *
 * <pre>
 * CREATED → WAITING_FOR_PLAYERS → STARTING → IN_PROGRESS → COMPLETED
 *                                                ↓             ↓
 *                                              PAUSED      CANCELLED
 *                                                ↓
 *                                           IN_PROGRESS
 * </pre>
 */
public enum GameInstanceState {
    /** Game instance created, no players yet */
    CREATED,

    /** Waiting for players to join before starting */
    WAITING_FOR_PLAYERS,

    /** Game is being initialized (brief transition state) */
    STARTING,

    /** Game is actively running */
    IN_PROGRESS,

    /** Game temporarily paused, can be resumed */
    PAUSED,

    /** Game finished normally */
    COMPLETED,

    /** Game cancelled before completion */
    CANCELLED
}
