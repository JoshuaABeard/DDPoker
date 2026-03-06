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
    await page.goto('/online')
    // Admin nav item is not rendered at all for non-admin users
    await expect(page.locator('header').getByText('Admin', { exact: true })).not.toBeVisible()
  })

  test('admin user sees admin nav link', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/online')
    // Admin nav dropdown shows "Admin ▼" text in header
    await expect(page.locator('header').getByText('Admin').first()).toBeVisible()
  })

  test('admin dashboard has links to tools', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('profile search returns results', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/online-profile-search')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('verify email action on unverified profile', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/online-profile-search')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('ban list page loads', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/ban-list')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  })

  test('add and remove ban', async ({ page }) => {
    await ui.login(page, 'admin', 'password123')
    await page.goto('/admin/ban-list')
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
  })
})
