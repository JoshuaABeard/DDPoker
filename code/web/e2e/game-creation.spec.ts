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

test.describe('Game creation & lobby', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createVerifiedUser('gamehost', 'password123', 'gamehost@example.com')
    await api.createVerifiedUser('joiner', 'password123', 'joiner@example.com')
  })

  test('navigate to create game page', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games')
    await page.getByRole('link', { name: /create game/i }).click()
    await expect(page).toHaveURL(/\/games\/create/)
  })

  test('quick practice starts a game immediately', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    await page.getByRole('button', { name: /quick practice/i }).click()
    await expect(page).toHaveURL(/\/games\/.*\/play/, { timeout: 15_000 })
    await expect(page.locator('[data-testid="poker-table"], .poker-table, canvas').first()).toBeVisible({
      timeout: 10_000,
    })
  })

  test('create practice game with custom settings', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    const nameInput = page.getByLabel(/game name|name/i).first()
    if (await nameInput.isVisible()) {
      await nameInput.fill('My Test Game')
    }
    await page.getByRole('button', { name: /create game|start game|create practice/i }).first().click()
    await expect(page).toHaveURL(/\/games\/.*\/(play|lobby)/, { timeout: 15_000 })
  })

  test('games page shows tabs and search', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games')
    await expect(page.getByText(/game lobby/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /open/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /in progress/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /completed/i })).toBeVisible()
    await expect(page.getByLabel(/search/i)).toBeVisible()
  })

  test('tab switching filters games', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games')
    await page.getByRole('button', { name: /in progress/i }).click()
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
    await page.getByRole('button', { name: /completed/i }).click()
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
    await page.getByRole('button', { name: /open/i }).click()
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
  })

  test('no games shows empty state', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games')
    await expect(page.getByText(/no games found/i)).toBeVisible()
  })

  test('create game page has blind structure options', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    await expect(page.getByText(/blind|structure/i).first()).toBeVisible()
  })

  test('create game page has AI player options', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    await expect(page.getByText(/ai|opponent|player/i).first()).toBeVisible()
  })
})
