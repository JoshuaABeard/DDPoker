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

import { type Page } from '@playwright/test'

const GAME_SERVER_URL = 'http://localhost:8877'

export const api = {
  async resetDatabase(): Promise<void> {
    const res = await fetch(`${GAME_SERVER_URL}/api/v1/dev/reset`, { method: 'POST' })
    if (!res.ok) throw new Error(`Reset failed: ${res.status}`)
  },

  async registerUser(
    username: string,
    password: string,
    email: string,
  ): Promise<{ token: string }> {
    const res = await fetch(`${GAME_SERVER_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password, email }),
    })
    if (!res.ok) throw new Error(`Register failed: ${res.status} ${await res.text()}`)
    return res.json()
  },

  async verifyUser(username: string): Promise<void> {
    const res = await fetch(
      `${GAME_SERVER_URL}/api/v1/dev/verify-user?username=${encodeURIComponent(username)}`,
      { method: 'POST' },
    )
    if (!res.ok) throw new Error(`Verify failed: ${res.status}`)
  },

  async makeAdmin(username: string): Promise<void> {
    const res = await fetch(
      `${GAME_SERVER_URL}/api/v1/dev/make-admin?username=${encodeURIComponent(username)}`,
      { method: 'POST' },
    )
    if (!res.ok) throw new Error(`Make admin failed: ${res.status}`)
  },

  async createVerifiedUser(username: string, password: string, email: string): Promise<string> {
    const { token } = await api.registerUser(username, password, email)
    await api.verifyUser(username)
    return token
  },

  async createAdminUser(username: string, password: string, email: string): Promise<string> {
    const token = await api.createVerifiedUser(username, password, email)
    await api.makeAdmin(username)
    return token
  },

  async getForgotPasswordToken(email: string): Promise<string> {
    const res = await fetch(`${GAME_SERVER_URL}/api/v1/auth/forgot-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
    })
    if (!res.ok) throw new Error(`Forgot password failed: ${res.status}`)
    const data = await res.json()
    return data.resetToken
  },
}

export const ui = {
  async login(page: Page, username: string, password: string): Promise<void> {
    await page.goto('/login')
    await page.getByLabel('Username').fill(username)
    await page.getByLabel('Password').fill(password)
    await page.getByRole('button', { name: /log in/i }).click()
  },

  async register(page: Page, username: string, email: string, password: string): Promise<void> {
    await page.goto('/register')
    await page.getByLabel('Username').fill(username)
    await page.getByLabel('Email').fill(email)
    await page.getByLabel('Password', { exact: true }).fill(password)
    await page.getByLabel('Confirm Password').fill(password)
    await page.getByRole('button', { name: /create account/i }).click()
  },
}
