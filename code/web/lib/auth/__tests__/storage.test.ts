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

import { describe, it, expect, beforeEach } from 'vitest'
import { getAuthUser, setAuthUser, clearAuthUser, type StoredAuthUser } from '../storage'

const AUTH_KEY = 'ddpoker_auth_user'

function buildStoredAuthUser(overrides: Partial<StoredAuthUser> = {}): StoredAuthUser {
  return { username: 'testuser', ...overrides }
}

describe('storage', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  // --- getAuthUser ---

  describe('getAuthUser', () => {
    it('returns null when both storages are empty', () => {
      expect(getAuthUser()).toBeNull()
    })

    it('reads user from localStorage', () => {
      const user = buildStoredAuthUser({ username: 'alice' })
      localStorage.setItem(AUTH_KEY, JSON.stringify(user))
      expect(getAuthUser()).toEqual(user)
    })

    it('reads user from sessionStorage', () => {
      const user = buildStoredAuthUser({ username: 'bob' })
      sessionStorage.setItem(AUTH_KEY, JSON.stringify(user))
      expect(getAuthUser()).toEqual(user)
    })

    it('prefers localStorage over sessionStorage', () => {
      const localUser = buildStoredAuthUser({ username: 'local' })
      const sessionUser = buildStoredAuthUser({ username: 'session' })
      localStorage.setItem(AUTH_KEY, JSON.stringify(localUser))
      sessionStorage.setItem(AUTH_KEY, JSON.stringify(sessionUser))
      expect(getAuthUser()).toEqual(localUser)
    })

    it('self-heals corrupt localStorage JSON and returns null', () => {
      localStorage.setItem(AUTH_KEY, 'not-valid-json}}}')
      expect(getAuthUser()).toBeNull()
      expect(localStorage.getItem(AUTH_KEY)).toBeNull()
    })

    it('self-heals corrupt sessionStorage JSON and returns null', () => {
      sessionStorage.setItem(AUTH_KEY, 'not-valid-json}}}')
      expect(getAuthUser()).toBeNull()
      expect(sessionStorage.getItem(AUTH_KEY)).toBeNull()
    })
  })

  // --- setAuthUser ---

  describe('setAuthUser', () => {
    it('stores user in localStorage and clears sessionStorage when rememberMe=true', () => {
      const user = buildStoredAuthUser({ username: 'charlie' })
      sessionStorage.setItem(AUTH_KEY, JSON.stringify(buildStoredAuthUser({ username: 'old' })))

      setAuthUser(user, true)

      expect(JSON.parse(localStorage.getItem(AUTH_KEY)!)).toEqual(user)
      expect(sessionStorage.getItem(AUTH_KEY)).toBeNull()
    })

    it('stores user in sessionStorage and clears localStorage when rememberMe=false', () => {
      const user = buildStoredAuthUser({ username: 'diana' })
      localStorage.setItem(AUTH_KEY, JSON.stringify(buildStoredAuthUser({ username: 'old' })))

      setAuthUser(user, false)

      expect(JSON.parse(sessionStorage.getItem(AUTH_KEY)!)).toEqual(user)
      expect(localStorage.getItem(AUTH_KEY)).toBeNull()
    })
  })

  // --- clearAuthUser ---

  describe('clearAuthUser', () => {
    it('removes user from both localStorage and sessionStorage', () => {
      const user = buildStoredAuthUser()
      localStorage.setItem(AUTH_KEY, JSON.stringify(user))
      sessionStorage.setItem(AUTH_KEY, JSON.stringify(user))

      clearAuthUser()

      expect(localStorage.getItem(AUTH_KEY)).toBeNull()
      expect(sessionStorage.getItem(AUTH_KEY)).toBeNull()
    })
  })
})
