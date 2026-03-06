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

test.beforeAll(async () => {
  await api.resetDatabase()
  await api.createVerifiedUser('alice', 'password123', 'alice@example.com')
  await api.registerUser('unverified', 'password123', 'unverified@example.com')
})

test.describe('Login and Password Recovery', () => {
  test('navigate to login from nav', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('link', { name: 'Log In' }).click()
    await expect(page).toHaveURL(/\/login/)
  })

  test('wrong password shows error alert and stays on /login', async ({ page }) => {
    await ui.login(page, 'alice', 'wrongpassword')
    await expect(page.locator('[role="alert"]').first()).toBeVisible()
    await expect(page).toHaveURL(/\/login/)
  })

  test('successful login redirects to /online', async ({ page }) => {
    await ui.login(page, 'alice', 'password123')
    await expect(page).toHaveURL(/\/online/)
  })

  test('login preserves returnUrl', async ({ page }) => {
    await page.goto('/games')
    await expect(page).toHaveURL(/\/login\?returnUrl=/)
    await page.getByLabel('Username').fill('alice')
    await page.getByLabel('Password').fill('password123')
    await page.getByRole('button', { name: /log in/i }).click()
    await expect(page).toHaveURL(/\/games/)
  })

  test('login with unverified account redirects to /verify-email-pending', async ({ page }) => {
    await ui.login(page, 'unverified', 'password123')
    await expect(page).toHaveURL(/\/verify-email-pending/)
  })

  test('logged-in user sees username in navigation', async ({ page }) => {
    await ui.login(page, 'alice', 'password123')
    await expect(page).toHaveURL(/\/online/)
    await expect(page.getByText('alice')).toBeVisible()
  })

  test('logout clears access and redirects to /login', async ({ page }) => {
    await ui.login(page, 'alice', 'password123')
    await expect(page).toHaveURL(/\/online/)
    await page.getByRole('button', { name: /logout/i }).click()
    await page.goto('/games')
    await expect(page).toHaveURL(/\/login/)
  })

  test('forgot password link on login page leads to /forgot', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('link', { name: 'Forgot your password?' }).first().click()
    await expect(page).toHaveURL(/\/forgot/)
  })

  test('full forgot -> reset -> login flow', async ({ page }) => {
    await page.goto('/forgot')
    await page.getByLabel('Email address').fill('alice@example.com')
    await page.getByRole('button', { name: /send reset link/i }).click()

    const resetToken = await api.getForgotPasswordToken('alice@example.com')

    await page.goto(`/reset-password?token=${resetToken}`)
    await page.getByPlaceholder('New password (min 8 chars)').fill('newpassword456')
    await page.getByPlaceholder('Confirm password').fill('newpassword456')
    await page.getByRole('button', { name: /reset password/i }).click()
    await expect(page.getByText('Password reset. Redirecting to login')).toBeVisible()

    await expect(page).toHaveURL(/\/login/, { timeout: 10000 })

    await ui.login(page, 'alice', 'newpassword456')
    await expect(page).toHaveURL(/\/online/)
  })

  test('/reset-password without token shows invalid link message', async ({ page }) => {
    await page.goto('/reset-password')
    await expect(page.getByRole('heading', { name: 'Invalid link' })).toBeVisible()
    await expect(page.getByText('No reset token provided.')).toBeVisible()
  })
})
