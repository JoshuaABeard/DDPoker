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

  /**
   * Login via API and return the JWT token. Useful for setting up auth
   * in the browser context without going through the UI.
   */
  async login(
    username: string,
    password: string,
  ): Promise<{ token: string }> {
    const res = await fetch(`${GAME_SERVER_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    if (!res.ok) throw new Error(`Login failed: ${res.status} ${await res.text()}`)
    // Extract JWT from Set-Cookie header
    const setCookie = res.headers.get('set-cookie') || ''
    const match = setCookie.match(/DDPoker-JWT=([^;]+)/)
    const token = match ? match[1] : ''
    return { token }
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
  /**
   * Log in via the browser UI form AND set extra HTTP headers so the
   * Next.js middleware sees the cookie on subsequent page.goto() calls.
   *
   * In the E2E setup, the game server (port 8877) and Next.js (port 3000) run
   * on different ports. The game server sets an HttpOnly DDPoker-JWT cookie,
   * but Chromium scopes it to port 8877 only. The Next.js middleware on port
   * 3000 never sees it on full-page navigations (page.goto). We work around
   * this by capturing the JWT from the login response and forwarding it as an
   * extra HTTP header on every subsequent request.
   */
  async login(page: Page, username: string, password: string): Promise<void> {
    await page.goto('/login')
    await page.getByLabel('Username').fill(username)
    await page.getByLabel('Password').fill(password)
    // Check "Remember me" so auth state is stored in localStorage (persists
    // across full-page navigations better than sessionStorage)
    await page.getByLabel(/remember me/i).check()
    await page.getByRole('button', { name: /log in/i }).click()

    await page.waitForResponse(
      (resp) => resp.url().includes('/api/v1/auth/login') && resp.status() === 200,
    )
    // Wait for the client-side redirect to complete
    await page.waitForURL('**/online**', { timeout: 10_000 })

    // Extract the JWT from the browser's cookie jar (set by the game server
    // on port 8877 via cross-origin fetch). Chromium doesn't send this cookie
    // on full-page navigations to port 3000 (different port = different origin
    // for cookie delivery). We use the CDP Fetch domain to inject the cookie
    // header ONLY on port-3000 requests, leaving port-8877 requests untouched
    // so the AuthProvider's /auth/me call uses the natural browser cookie.
    const cookies = await page.context().cookies()
    const jwt = cookies.find((c) => c.name === 'DDPoker-JWT')
    if (jwt) {
      // Set extra HTTP headers so the Next.js middleware on port 3000 sees
      // the cookie. The cookie is naturally available for port-8877 requests.
      await page.setExtraHTTPHeaders({ cookie: `DDPoker-JWT=${jwt.value}` })
    }
  },

  /**
   * Log in via the browser UI form without setting extra headers.
   * Useful for testing the login/error flow itself.
   */
  async loginViaForm(page: Page, username: string, password: string): Promise<void> {
    await page.goto('/login')
    await page.getByLabel('Username').fill(username)
    await page.getByLabel('Password').fill(password)
    await page.getByRole('button', { name: /log in/i }).click()

    // Wait for the login API call to complete (success or failure)
    await page.waitForResponse((resp) => resp.url().includes('/api/v1/auth/login'))
    await page.waitForTimeout(500)
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
