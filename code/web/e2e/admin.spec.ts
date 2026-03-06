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

test.describe('Admin', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createAdminUser('admin', 'password123', 'admin@example.com')
    await api.createVerifiedUser('regular', 'password123', 'regular@example.com')
    await api.registerUser('unverifieduser', 'password123', 'unverified@example.com')
    // Do NOT verify 'unverifieduser'
  })

  test('non-admin user cannot see admin nav link', async ({ page }) => {
    await ui.login(page, 'regular', 'password123')
    await expect(page).toHaveURL(/\/online/, { timeout: 10_000 })
    await expect(page.getByRole('navigation').getByRole('link', { name: /admin/i })).not.toBeVisible()
  })

  test('admin user sees admin nav link', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await expect(page).toHaveURL(/\/online/, { timeout: 10_000 })
    await expect(page.getByRole('navigation').getByRole('link', { name: /admin/i })).toBeVisible()
  })

  test('admin dashboard has links to tools', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin')
    await expect(page.getByRole('link', { name: /profile search/i })).toBeVisible()
    await expect(page.getByRole('link', { name: /ban list/i })).toBeVisible()
  })

  test('profile search returns results', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/online-profile-search')
    await page.getByPlaceholder(/name/i).first().fill('regular')
    await page.getByRole('button', { name: /search/i }).click()
    await expect(page.getByText('regular')).toBeVisible()
  })

  test('verify email action on unverified profile', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/online-profile-search')
    await page.getByPlaceholder(/name/i).first().fill('unverifieduser')
    await page.getByRole('button', { name: /search/i }).click()
    await expect(page.getByText('unverifieduser')).toBeVisible()
    const verifyBtn = page.getByRole('button', { name: /verify/i }).first()
    if (await verifyBtn.isVisible()) {
      await verifyBtn.click()
      await expect(page.getByText(/verified|success/i).first()).toBeVisible({ timeout: 5000 })
    }
  })

  test('ban list page loads', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/ban-list')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('add and remove ban', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/ban-list')

    const keyInput = page.getByPlaceholder(/key/i).first()
    const commentInput = page.getByPlaceholder(/comment|reason/i).first()
    if (await keyInput.isVisible()) {
      await keyInput.fill('test-ban-key-123')
      if (await commentInput.isVisible()) {
        await commentInput.fill('E2E test ban')
      }
      await page.getByRole('button', { name: /add|ban|submit/i }).first().click()
      await expect(page.getByText('test-ban-key-123')).toBeVisible({ timeout: 5000 })
      await page.getByRole('button', { name: /remove|delete/i }).first().click()
      await expect(page.getByText('test-ban-key-123')).not.toBeVisible({ timeout: 5000 })
    }
  })
})
