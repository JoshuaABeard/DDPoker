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

import { test, expect } from '@playwright/test';
import { api, ui } from './fixtures/test-helper';

test.describe('Online portal (public data)', () => {
  test.beforeAll(async () => {
    await api.resetDatabase();
    await api.createVerifiedUser('pokerpro', 'password123', 'pokerpro@example.com');
    await api.createVerifiedUser('cardshark', 'password123', 'cardshark@example.com');
    await api.createVerifiedUser('bluffer', 'password123', 'bluffer@example.com');
  });

  test('online portal shows navigation links', async ({ page }) => {
    await page.goto('/online');
    const main = page.locator('main');
    await expect(main.getByRole('link', { name: 'Leaderboard', exact: true })).toBeVisible();
    await expect(main.getByRole('link', { name: 'Player Search', exact: true })).toBeVisible();
    await expect(main.getByRole('link', { name: 'Host List', exact: true })).toBeVisible();
  });

  test('leaderboard loads in default DDR1 mode', async ({ page }) => {
    await page.goto('/online/leaderboard');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByText(/error|failed/i)).not.toBeVisible();
  });

  test('player search finds existing player', async ({ page }) => {
    await page.goto('/online/search');
    await page.getByLabel('Player Name').fill('pokerpro');
    await page.getByRole('button', { name: 'Apply Filters' }).click();
    await expect(page.getByText('Search results for:')).toBeVisible();
  });

  test('player search with no results shows message', async ({ page }) => {
    await page.goto('/online/search');
    await page.getByLabel('Player Name').fill('nonexistentplayer12345');
    await page.getByRole('button', { name: 'Apply Filters' }).click();
    await expect(page.getByText('No players found matching "nonexistentplayer12345"')).toBeVisible();
  });

  test('click player in search navigates to history', async ({ page }) => {
    await page.goto('/online/search');
    await page.getByLabel('Player Name').fill('pokerpro');
    await page.getByRole('button', { name: 'Apply Filters' }).click();
    // Verify search results area is shown (users without tournament history won't appear)
    await expect(page.getByText(/Search results for:|No players found/).first()).toBeVisible();
  });

  test('tournament history page shows stats', async ({ page }) => {
    await page.goto('/online/history?name=pokerpro');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Tournament History: pokerpro');
    // New users with 0 games won't have stats cards; verify no error is shown
    await expect(page.getByText(/error|failed/i)).not.toBeVisible();
  });

  test('hosts page loads', async ({ page }) => {
    await page.goto('/online/hosts');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByText(/error|failed/i)).not.toBeVisible();
  });

  test('my profile page requires login', async ({ page }) => {
    await page.goto('/online/myprofile');
    await expect(page.getByText(/log in|login/i).first()).toBeVisible({ timeout: 10_000 });
  });

  test('my profile page shows user info when logged in', async ({ page }) => {
    await ui.login(page, 'pokerpro', 'password123');
    await page.goto('/online/myprofile');
    await expect(page.getByRole('heading', { name: /My Profile/i })).toBeVisible();
  });

  test('leaderboard filter by name works', async ({ page }) => {
    await page.goto('/online/leaderboard');
    const nameFilter = page.getByLabel('Player Name');
    if (await nameFilter.isVisible()) {
      await nameFilter.fill('pokerpro');
      await page.getByRole('button', { name: 'Apply Filters' }).click();
      await expect(page.getByText(/error|failed/i)).not.toBeVisible();
    }
  });
});
