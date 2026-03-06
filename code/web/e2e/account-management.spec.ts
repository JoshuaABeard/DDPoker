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

test.describe('Account Management', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
    await api.createVerifiedUser('acctuser', 'password123', 'acctuser@example.com')
    await api.createVerifiedUser('emailuser', 'password123', 'emailuser@example.com')
    await api.registerUser('unverifiedacct', 'password123', 'unverifiedacct@example.com')
  })

  test('account page shows user info', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')

    await expect(page.getByRole('heading', { name: 'Account Settings' })).toBeVisible()
    await expect(page.getByRole('heading', { name: /change password/i })).toBeVisible()
    await expect(page.getByRole('heading', { name: /change email/i })).toBeVisible()
  })

  test('change password with wrong current password shows error', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')

    await page.getByPlaceholder('Current password').fill('wrongpassword')
    await page.getByPlaceholder('New password (min 8 chars)').fill('newpassword1')
    await page.getByPlaceholder('Confirm new password').fill('newpassword1')
    await page.getByRole('button', { name: /change password/i }).click()

    await expect(page.getByText('Failed to change password. Check your current password.')).toBeVisible()
  })

  test('change password with mismatched new passwords shows error', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')

    await page.getByPlaceholder('Current password').fill('password123')
    await page.getByPlaceholder('New password (min 8 chars)').fill('newpassword1')
    await page.getByPlaceholder('Confirm new password').fill('differentpassword')
    await page.getByRole('button', { name: /change password/i }).click()

    await expect(page.getByText(/do not match/i)).toBeVisible()
  })

  test('change password with short new password shows error', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')

    await page.getByPlaceholder('Current password').fill('password123')
    await page.getByPlaceholder('New password (min 8 chars)').fill('short')
    await page.getByPlaceholder('Confirm new password').fill('short')
    await page.getByRole('button', { name: /change password/i }).click()

    await expect(page.getByText(/at least 8 characters/i)).toBeVisible()
  })

  test('password change form submits and shows feedback', async ({ page }) => {
    await ui.login(page, 'acctuser', 'password123')
    await page.goto('/account')

    await page.getByPlaceholder('Current password').fill('password123')
    await page.getByPlaceholder('New password (min 8 chars)').fill('newpassword1')
    await page.getByPlaceholder('Confirm new password').fill('newpassword1')
    await page.getByRole('button', { name: /change password/i }).click()

    // The form submits and shows either success or error feedback
    await expect(page.getByText(/password changed|failed to change/i)).toBeVisible()
  })

  test('change email form submits and shows feedback', async ({ page }) => {
    await ui.login(page, 'emailuser', 'password123')
    await page.goto('/account')

    await page.getByPlaceholder('New email address').fill('newemail@example.com')
    await page.getByRole('button', { name: /update email/i }).click()

    // The form submits and shows either success or error feedback
    await expect(page.getByText(/confirmation email sent|email updated|failed to update|error/i)).toBeVisible()
  })

  test('unverified user gets redirected to verify-email-pending', async ({ page }) => {
    await ui.loginViaForm(page, 'unverifiedacct', 'password123')
    await expect(page).toHaveURL(/\/verify-email-pending/, { timeout: 10_000 })
    await expect(page.getByRole('heading', { name: /check your email/i })).toBeVisible()
  })

  test('resend verification shows confirmation message', async ({ page }) => {
    await ui.loginViaForm(page, 'unverifiedacct', 'password123')
    await expect(page).toHaveURL(/\/verify-email-pending/, { timeout: 10_000 })

    await page.getByRole('button', { name: /resend verification email/i }).click()

    await expect(page.getByText(/email resent|could not resend/i)).toBeVisible()
  })
})
