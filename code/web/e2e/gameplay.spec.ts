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

import { test, expect } from '@playwright/test'
import { api, ui } from './fixtures/test-helper'

/**
 * Gameplay tests are limited because the standalone game server
 * (GameServerStandaloneApplication) does not include the WebSocket package
 * in its component scan. Real-time gameplay tests require WebSocket support.
 *
 * These tests verify the game creation UI works and the game pages exist.
 */
test.describe('Gameplay (practice)', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createVerifiedUser('player1', 'password123', 'player1@example.com')
  })

  test('practice game form loads with all sections', async ({ page }) => {
    await ui.login(page, 'player1', 'password123')
    await page.goto('/games/create?practice=true')
    await expect(page.getByRole('heading', { name: /Quick Practice Game/i })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Basic Settings' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Blind Structure' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'AI Opponents' })).toBeVisible()
  })

  test('practice form has practice-specific options', async ({ page }) => {
    await ui.login(page, 'player1', 'password123')
    await page.goto('/games/create?practice=true')
    await expect(page.getByRole('heading', { name: 'Practice Options' })).toBeVisible()
    await expect(page.getByLabel(/Show AI Cards/i)).toBeVisible()
    await expect(page.getByLabel(/Auto Deal/i)).toBeVisible()
  })

  test('AI opponents section shows player inputs', async ({ page }) => {
    await ui.login(page, 'player1', 'password123')
    await page.goto('/games/create?practice=true')
    // AI players are shown as labeled textboxes
    await expect(page.getByRole('textbox', { name: 'AI 1' })).toBeVisible()
    await expect(page.getByRole('button', { name: /add ai player/i })).toBeVisible()
  })

  test('game type selector has options', async ({ page }) => {
    await ui.login(page, 'player1', 'password123')
    await page.goto('/games/create')
    const gameType = page.getByLabel(/Game Type/i)
    await expect(gameType).toBeVisible()
    // "No Limit" is the selected option in the select element
    await expect(gameType).toHaveValue('NOLIMIT_HOLDEM')
  })

  test('blind structure has preset selector', async ({ page }) => {
    await ui.login(page, 'player1', 'password123')
    await page.goto('/games/create')
    await expect(page.getByLabel(/Preset/i)).toBeVisible()
  })

  test('advanced settings section is collapsible', async ({ page }) => {
    await ui.login(page, 'player1', 'password123')
    await page.goto('/games/create')
    const details = page.locator('details', { hasText: 'Advanced Settings' })
    await expect(details).toBeVisible()
    await details.locator('summary').click()
    await expect(page.getByLabel(/Action Timeout/i)).toBeVisible()
  })
})
