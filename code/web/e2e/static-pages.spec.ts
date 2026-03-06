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

test.describe('Static Pages', () => {
  test('home page loads with branding and download link', async ({ page }) => {
    await page.goto('/')

    await expect(page.getByRole('heading', { name: /DD Poker Community Edition/i })).toBeVisible()
    await expect(page.getByRole('link', { name: /Download DD Poker Community Edition/i })).toBeVisible()
  })

  test('navigate to About via nav link', async ({ page }) => {
    await page.goto('/')

    // About has a dropdown toggle — click it to navigate
    await page.locator('.nav-link', { hasText: 'About' }).click()
    await expect(page).toHaveURL('/about')
    await expect(
      page.getByRole('heading', { name: /About DD Poker Community Edition/i }),
    ).toBeVisible()

    // Nav link should be active
    await expect(page.locator('.nav-link.active', { hasText: 'About' })).toBeVisible()
  })

  test('About FAQ sub-page loads via sidebar', async ({ page }) => {
    await page.goto('/about')

    // Click FAQ in the sidebar
    await page.getByRole('link', { name: /FAQ/ }).click()
    await expect(page).toHaveURL('/about/faq')
    await expect(page.getByRole('heading', { name: /Poker FAQ/i })).toBeVisible()
  })

  test('Download page shows platform installers', async ({ page }) => {
    await page.goto('/')
    await page.locator('.nav-link', { hasText: 'Download' }).click()
    await expect(page).toHaveURL('/download')

    await expect(page.getByRole('heading', { name: /Download DD Poker Community Edition/i })).toBeVisible()

    // Verify all three platform sections are present
    await expect(page.getByRole('heading', { name: /Windows Installer/i })).toBeVisible()
    await expect(page.getByRole('heading', { name: /macOS Installer/i })).toBeVisible()
    await expect(page.getByRole('heading', { name: /Linux Installers/i })).toBeVisible()
  })

  test('Support page loads via nav', async ({ page }) => {
    await page.goto('/')
    await page.locator('.nav-link', { hasText: 'Support' }).click()
    await expect(page).toHaveURL('/support')
    await expect(
      page.getByRole('heading', { name: /DD Poker Community Edition Support/i }),
    ).toBeVisible()
  })

  test('Terms page loads at /terms', async ({ page }) => {
    await page.goto('/terms')
    await expect(
      page.getByRole('heading', { name: /DD Poker Terms of Use/i }),
    ).toBeVisible()
  })

  test('Online portal loads without login', async ({ page }) => {
    await page.goto('/online')
    await expect(page.getByRole('heading', { name: /Online Games Portal/i })).toBeVisible()

    // Verify leaderboard and search links are present (scoped to main to avoid sidebar duplicates)
    const main = page.locator('main')
    await expect(main.getByRole('link', { name: 'Leaderboard' })).toBeVisible()
    await expect(main.getByRole('link', { name: 'Player Search' })).toBeVisible()
  })

  test('Leaderboard loads without login at /online/leaderboard', async ({ page }) => {
    await page.goto('/online/leaderboard')
    await expect(page.getByRole('heading', { name: /Leaderboard/i })).toBeVisible()
  })

  test('footer is visible on every page', async ({ page }) => {
    await page.goto('/')
    const footer = page.locator('footer')
    await expect(footer).toBeVisible()
    await expect(footer.getByText('DD Poker Community Edition')).toBeVisible()
    await expect(footer.getByText('Doug Donohoe')).toBeVisible()
  })

  test('navigation highlights active page', async ({ page }) => {
    // Navigate to About and verify active class
    await page.goto('/about')
    const aboutLink = page.locator('.nav-link', { hasText: 'About' })
    await expect(aboutLink).toHaveClass(/active/)

    // Home link should NOT be active
    const homeLink = page.locator('.nav-link', { hasText: 'Home' })
    await expect(homeLink).not.toHaveClass(/active/)

    // Navigate to Download and verify it becomes active
    await page.locator('.nav-link', { hasText: 'Download' }).click()
    await expect(page).toHaveURL('/download')
    await expect(page.locator('.nav-link', { hasText: 'Download' })).toHaveClass(/active/)
    await expect(page.locator('.nav-link', { hasText: 'About' })).not.toHaveClass(/active/)
  })
})
