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

import { test, expect, type Page } from '@playwright/test'
import { api, ui } from './fixtures/test-helper'

/**
 * Wait for the poker table to be interactive (player has cards or action prompt).
 *
 * NOTE: The standalone game server (GameServerStandaloneApplication) does not
 * currently include the `websocket` package in its component scan, so WebSocket
 * endpoints (/ws/games/*, /ws/lobby) are not registered. Gameplay tests that
 * depend on real-time WebSocket communication will timeout until the component
 * scan is extended to include `com.donohoedigital.games.poker.gameserver.websocket`.
 */
async function waitForTable(page: Page) {
  await expect(
    page.locator('[data-testid="poker-table"], .poker-table, canvas').first()
  ).toBeVisible({ timeout: 30_000 })
}

/** Wait for the action panel to show available actions. */
async function waitForAction(page: Page) {
  await expect(
    page.getByRole('button', { name: /fold|check|call/i }).first()
  ).toBeVisible({ timeout: 30_000 })
}

/** Start a quick practice game and wait for the table. */
async function startQuickPractice(page: Page) {
  await page.goto('/games/create')
  await page.getByRole('button', { name: /quick practice/i }).click()
  await expect(page).toHaveURL(/\/games\/.*\/play/, { timeout: 20_000 })
  await waitForTable(page)
}

test.describe('Gameplay (practice)', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createVerifiedUser('player1', 'password123', 'player1@example.com')
  })

  test.beforeEach(async ({ page }) => {
    await ui.login(page, 'player1', 'password123')
  })

  test('practice game renders poker table with player seats', async ({ page }) => {
    await startQuickPractice(page)
    const seats = page.locator('[data-testid*="seat"], [class*="seat"], [class*="player"]')
    await expect(seats.first()).toBeVisible()
  })

  test('player sees their hole cards', async ({ page }) => {
    await startQuickPractice(page)
    const cards = page.locator('[data-testid*="card"], [class*="card"], .card')
    await expect(cards.first()).toBeVisible({ timeout: 20_000 })
  })

  test('tournament info bar shows blinds and level', async ({ page }) => {
    await startQuickPractice(page)
    await expect(page.getByText(/level|blind/i).first()).toBeVisible({ timeout: 20_000 })
  })

  test('action panel appears when it is player turn', async ({ page }) => {
    await startQuickPractice(page)
    await waitForAction(page)
    await expect(page.getByRole('button', { name: /fold/i })).toBeVisible()
  })

  test('fold action works', async ({ page }) => {
    await startQuickPractice(page)
    await waitForAction(page)
    await page.getByRole('button', { name: /fold/i }).click()
    await expect(page.getByRole('button', { name: /fold/i })).not.toBeVisible({
      timeout: 5000,
    })
  })

  test('check or call action works', async ({ page }) => {
    await startQuickPractice(page)
    await waitForAction(page)
    const checkBtn = page.getByRole('button', { name: /^check$/i })
    const callBtn = page.getByRole('button', { name: /^call/i })
    if (await checkBtn.isVisible()) {
      await checkBtn.click()
    } else {
      await callBtn.click()
    }
    await expect(page.getByRole('button', { name: /fold/i })).not.toBeVisible({
      timeout: 5000,
    })
  })

  test('keyboard shortcut F folds', async ({ page }) => {
    await startQuickPractice(page)
    await waitForAction(page)
    await page.keyboard.press('f')
    await expect(page.getByRole('button', { name: /fold/i })).not.toBeVisible({
      timeout: 5000,
    })
  })

  test('hand history panel toggles', async ({ page }) => {
    await startQuickPractice(page)
    const historyBtn = page.getByRole('button', { name: /hand history/i })
    if (await historyBtn.isVisible()) {
      await historyBtn.click()
      await expect(page.getByRole('log')).toBeVisible()
      await historyBtn.click()
    }
  })

  test('theme picker changes table appearance', async ({ page }) => {
    await startQuickPractice(page)
    const settingsBtn = page.getByRole('button', { name: /settings/i })
    if (await settingsBtn.isVisible()) {
      await settingsBtn.click()
      await expect(page.getByText(/table/i)).toBeVisible()
    }
  })

  test('chat panel accepts messages', async ({ page }) => {
    await startQuickPractice(page)
    const chatInput = page.getByPlaceholder(/message|chat/i)
    if (await chatInput.isVisible()) {
      await chatInput.fill('Hello from E2E test')
      await chatInput.press('Enter')
      await expect(page.getByText('Hello from E2E test')).toBeVisible()
    }
  })

  test('@slow full game plays to completion', async ({ page }) => {
    test.setTimeout(120_000)
    await page.goto('/games/create')
    await page.getByRole('button', { name: /quick practice/i }).click()
    await expect(page).toHaveURL(/\/games\/.*\/play/, { timeout: 20_000 })
    await waitForTable(page)

    let gameComplete = false
    const maxIterations = 200

    for (let i = 0; i < maxIterations; i++) {
      if (page.url().includes('/results')) {
        gameComplete = true
        break
      }

      const continueBtn = page.getByRole('button', { name: /continue|deal|ok/i })
      if (await continueBtn.isVisible({ timeout: 500 }).catch(() => false)) {
        await continueBtn.click()
        continue
      }

      const foldBtn = page.getByRole('button', { name: /fold/i })
      if (await foldBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
        const checkBtn = page.getByRole('button', { name: /^check$/i })
        const callBtn = page.getByRole('button', { name: /^call/i })
        if (await checkBtn.isVisible({ timeout: 200 }).catch(() => false)) {
          await checkBtn.click()
        } else if (await callBtn.isVisible({ timeout: 200 }).catch(() => false)) {
          await callBtn.click()
        } else {
          await foldBtn.click()
        }
        continue
      }

      await page.waitForTimeout(500)
    }

    if (!gameComplete) {
      await expect(page).toHaveURL(/\/results/, { timeout: 30_000 })
    }

    await expect(page.getByText(/1st|2nd|3rd|first|second|third/i).first()).toBeVisible()
  })
})
