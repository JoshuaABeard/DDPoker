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
  })

  test('games page loads for authenticated user', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games')
    await expect(page).toHaveURL(/\/games/)
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
  })

  test('create game page loads with form', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    await expect(page.getByRole('heading', { name: /Create Game/i })).toBeVisible()
  })

  test('create game page has basic settings', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    await expect(page.getByText('Basic Settings')).toBeVisible()
    await expect(page.getByLabel(/Game Name/i)).toBeVisible()
    await expect(page.getByLabel(/Max Players/i)).toBeVisible()
  })

  test('create game page has blind structure section', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    await expect(page.getByText('Blind Structure')).toBeVisible()
  })

  test('create game page has AI opponents section', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    await expect(page.getByText('AI Opponents')).toBeVisible()
  })

  test('create game page has quick practice button', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    await expect(page.getByRole('button', { name: /quick practice/i })).toBeVisible()
  })

  test('practice mode page shows start practice button', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create?practice=true')
    await expect(page.getByRole('heading', { name: /Quick Practice Game/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /start practice game/i })).toBeVisible()
  })

  test('create game form requires game name', async ({ page }) => {
    await ui.login(page, 'gamehost', 'password123')
    await page.goto('/games/create')
    // Clear the name field and try to submit
    const nameInput = page.getByLabel(/Game Name/i)
    await nameInput.clear()
    await page.getByRole('button', { name: /^Create Game$/i }).click()
    // Should stay on the create page (form validation prevents navigation)
    await expect(page).toHaveURL(/\/games\/create/)
  })
})
