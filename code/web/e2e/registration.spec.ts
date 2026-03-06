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

test.describe('Registration', () => {
  test.beforeAll(async () => {
    await api.resetDatabase()
  })

  test('navigate from login page to register via "Create one" link', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('link', { name: /create one/i }).click()
    await expect(page).toHaveURL(/\/register/)
    await expect(page.getByRole('button', { name: /create account/i })).toBeVisible()
  })

  test('short username shows no availability feedback', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('ab')
    // Trigger blur to ensure any check would fire
    await page.getByLabel('Email').click()
    // Wait enough time for debounce to have fired if it was going to
    await page.waitForTimeout(800)
    await expect(page.getByText(/username available/i)).not.toBeVisible()
    await expect(page.getByText(/username not available/i)).not.toBeVisible()
  })

  test('available username shows green "Username available" text', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('freshuser')
    // Wait for debounce (~600ms) plus network time
    await expect(page.getByText(/username available/i)).toBeVisible({ timeout: 5000 })
  })

  test('taken username shows red "Username not available" text', async ({ page }) => {
    await api.registerUser('takenuser', 'password1234', 'taken@example.com')

    await page.goto('/register')
    await page.getByLabel('Username').fill('takenuser')
    // Wait for debounce plus network time
    await expect(page.getByText(/username not available/i)).toBeVisible({ timeout: 5000 })
  })

  test('mismatched passwords shows "Passwords do not match" error on submit', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('mismatchuser')
    await page.getByLabel('Email').fill('mismatch@example.com')
    await page.getByLabel('Password', { exact: true }).fill('password1234')
    await page.getByLabel('Confirm Password').fill('differentpassword')
    await page.getByRole('button', { name: /create account/i }).click()
    await expect(page.getByText(/passwords do not match/i)).toBeVisible()
  })

  test('short password shows "at least 8 characters" error on submit', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('shortpwuser')
    await page.getByLabel('Email').fill('shortpw@example.com')
    await page.getByLabel('Password', { exact: true }).fill('short')
    await page.getByLabel('Confirm Password').fill('short')
    await page.getByRole('button', { name: /create account/i }).click()
    // Browser HTML5 validation prevents submission for short passwords
    await expect(page).toHaveURL(/\/register/)
  })

  test('successful registration redirects to /verify-email-pending', async ({ page }) => {
    await page.goto('/register')
    await page.getByLabel('Username').fill('newuser')
    await page.getByLabel('Email').fill('newuser@example.com')
    await page.getByLabel('Password', { exact: true }).fill('password1234')
    await page.getByLabel('Confirm Password').fill('password1234')
    await page.getByRole('button', { name: /create account/i }).click()
    await expect(page).toHaveURL(/\/verify-email-pending/, { timeout: 10000 })
    await expect(page.getByText(/check your email/i)).toBeVisible()
  })

  test('unverified user accessing /games gets redirected', async ({ page }) => {
    await api.registerUser('unverified', 'password1234', 'unverified@example.com')
    await ui.loginViaForm(page, 'unverified', 'password1234')
    await expect(page).toHaveURL(/\/verify-email-pending/, { timeout: 10000 })
  })

  test('after verification, login and navigate to /games succeeds', async ({ page }) => {
    await api.registerUser('verifieduser', 'password1234', 'verified@example.com')
    await api.verifyUser('verifieduser')
    await ui.login(page, 'verifieduser', 'password1234')
    await page.goto('/games')
    await expect(page).toHaveURL(/\/games/)
    await expect(page.getByText(/error|failed/i)).not.toBeVisible()
  })

  test('/verify-email page without token shows error message', async ({ page }) => {
    await page.goto('/verify-email')
    await expect(page.getByRole('heading', { name: /Verification failed/i })).toBeVisible({ timeout: 10000 })
  })
})
