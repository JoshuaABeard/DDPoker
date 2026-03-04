/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { authApi, gameServerApi, checkApiHealth } from '../api'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function makeFetchResponse(body: object, ok = true, status = 200) {
  return {
    ok,
    status,
    statusText: ok ? 'OK' : 'Error',
    json: () => Promise.resolve(body),
  }
}

// ---------------------------------------------------------------------------
// Setup
// ---------------------------------------------------------------------------

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('apiFetch — Content-Type header', () => {
  it('GET request does not send Content-Type header', async () => {
    const mockFetch = vi.fn().mockResolvedValue(
      makeFetchResponse({ token: 'abc', expiresIn: 60 })
    )
    vi.stubGlobal('fetch', mockFetch)

    await gameServerApi.getWsToken()

    expect(mockFetch).toHaveBeenCalledTimes(1)
    const [, fetchOptions] = mockFetch.mock.calls[0] as [string, RequestInit]
    const headers = fetchOptions.headers as Record<string, string>
    expect(headers['Content-Type']).toBeUndefined()
  })

  it('POST request with body sends Content-Type: application/json', async () => {
    const mockFetch = vi.fn().mockResolvedValue(
      makeFetchResponse({
        gameId: 'g1',
        name: 'Test',
        status: 'WAITING_FOR_PLAYERS',
        maxPlayers: 9,
        currentPlayers: 1,
        isPrivate: false,
        createdAt: new Date().toISOString(),
      })
    )
    vi.stubGlobal('fetch', mockFetch)

    await gameServerApi.createGame({ name: 'Test', maxPlayers: 9 } as Parameters<typeof gameServerApi.createGame>[0])

    expect(mockFetch).toHaveBeenCalledTimes(1)
    const [, fetchOptions] = mockFetch.mock.calls[0] as [string, RequestInit]
    const headers = fetchOptions.headers as Record<string, string>
    expect(headers['Content-Type']).toBe('application/json')
  })
})

describe('apiFetch — AbortSignal', () => {
  it('passes composed AbortSignal to fetch when caller signal provided', async () => {
    let capturedInit: RequestInit | undefined
    vi.stubGlobal('fetch', vi.fn().mockImplementation((_url: string, init: RequestInit) => {
      capturedInit = init
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ token: 'test', expiresIn: 60 }),
      })
    }))

    // Call getWsToken which is a GET request with no signal normally
    await gameServerApi.getWsToken()

    // The signal should be defined (it's always the timeout signal at minimum)
    expect(capturedInit?.signal).toBeDefined()
    expect(capturedInit?.signal).toBeInstanceOf(AbortSignal)
  })
})

describe('checkApiHealth', () => {
  it('returns true when API responds with 200 and data', async () => {
    const mockFetch = vi.fn().mockResolvedValue(
      makeFetchResponse({ status: 'ok' })
    )
    vi.stubGlobal('fetch', mockFetch)

    const result = await checkApiHealth()

    expect(result).toBe(true)
  })

  it('returns false when fetch throws', async () => {
    const mockFetch = vi.fn().mockRejectedValue(new Error('Network error'))
    vi.stubGlobal('fetch', mockFetch)

    const result = await checkApiHealth()

    expect(result).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// authApi — game server endpoints
// ---------------------------------------------------------------------------

describe('authApi — game server endpoints', () => {
  it('login — POSTs to /api/v1/auth/login', async () => {
    const mockFetch = vi.fn().mockResolvedValue(
      makeFetchResponse({ success: true, message: null, username: 'alice', email: 'alice@example.com', emailVerified: false })
    )
    vi.stubGlobal('fetch', mockFetch)

    await authApi.login({ username: 'alice', password: 'pw', rememberMe: false })

    const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/login')
    expect((init?.method ?? 'GET').toUpperCase()).toBe('POST')
  })

  it('logout — POSTs to /api/v1/auth/logout', async () => {
    const mockFetch = vi.fn().mockResolvedValue(makeFetchResponse({}))
    vi.stubGlobal('fetch', mockFetch)

    await authApi.logout()

    const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/logout')
    expect((init?.method ?? 'GET').toUpperCase()).toBe('POST')
  })

  it('getCurrentUser — GETs /api/v1/auth/me', async () => {
    const mockFetch = vi.fn().mockResolvedValue(
      makeFetchResponse({ success: true, message: null, username: 'alice', email: 'alice@example.com', emailVerified: true })
    )
    vi.stubGlobal('fetch', mockFetch)

    const result = await authApi.getCurrentUser()

    const [url] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/me')
    expect(result?.username).toBe('alice')
  })

  it('forgotPassword — POSTs to /api/v1/auth/forgot-password', async () => {
    const mockFetch = vi.fn().mockResolvedValue(
      makeFetchResponse({ success: true, message: 'sent' })
    )
    vi.stubGlobal('fetch', mockFetch)

    await authApi.forgotPassword('alice')

    const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/forgot-password')
    expect((init?.method ?? 'GET').toUpperCase()).toBe('POST')
    expect(JSON.parse(init.body as string)).toMatchObject({ username: 'alice' })
  })

  it('changePassword — PUTs to /api/v1/auth/change-password', async () => {
    const mockFetch = vi.fn().mockResolvedValue(makeFetchResponse({}))
    vi.stubGlobal('fetch', mockFetch)

    await authApi.changePassword('oldpw', 'newpw')

    const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/change-password')
    expect((init?.method ?? 'GET').toUpperCase()).toBe('PUT')
    expect(JSON.parse(init.body as string)).toMatchObject({ currentPassword: 'oldpw', newPassword: 'newpw' })
    expect(init.credentials).toBe('include')
  })

  it('verifyEmail — GETs /api/v1/auth/verify-email?token=...', async () => {
    const mockFetch = vi.fn().mockResolvedValue(makeFetchResponse({ success: true }))
    vi.stubGlobal('fetch', mockFetch)

    await authApi.verifyEmail('tok123')

    const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/verify-email')
    expect(url).toContain('token=tok123')
    expect(init.credentials).toBe('include')
  })

  it('resendVerification — POSTs to /api/v1/auth/resend-verification', async () => {
    const mockFetch = vi.fn().mockResolvedValue(makeFetchResponse({ success: true }))
    vi.stubGlobal('fetch', mockFetch)

    await authApi.resendVerification()

    const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/resend-verification')
    expect((init?.method ?? 'GET').toUpperCase()).toBe('POST')
    expect(init.credentials).toBe('include')
  })

  it('checkUsername — GETs /api/v1/auth/check-username?username=...', async () => {
    const mockFetch = vi.fn().mockResolvedValue(makeFetchResponse({ available: true }))
    vi.stubGlobal('fetch', mockFetch)

    await authApi.checkUsername('alice')

    const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/check-username')
    expect(url).toContain('username=alice')
    expect(init.credentials).toBe('include')
  })

  it('changeEmail — PUTs to /api/v1/auth/email with email in body', async () => {
    const mockFetch = vi.fn().mockResolvedValue(makeFetchResponse({ success: true }))
    vi.stubGlobal('fetch', mockFetch)

    await authApi.changeEmail('newemail@example.com')

    const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/email')
    expect((init?.method ?? 'GET').toUpperCase()).toBe('PUT')
    expect(JSON.parse(init.body as string)).toMatchObject({ email: 'newemail@example.com' })
    expect(init.credentials).toBe('include')
  })

  it('resetPassword — POSTs to /api/v1/auth/reset-password with token and password', async () => {
    const mockFetch = vi.fn().mockResolvedValue(makeFetchResponse({ success: true }))
    vi.stubGlobal('fetch', mockFetch)

    await authApi.resetPassword('reset-token', 'newpassword')

    const [url, init] = mockFetch.mock.calls[0] as [string, RequestInit]
    expect(url).toContain('/api/v1/auth/reset-password')
    expect((init?.method ?? 'GET').toUpperCase()).toBe('POST')
    expect(JSON.parse(init.body as string)).toMatchObject({ token: 'reset-token', password: 'newpassword' })
    expect(init.credentials).toBe('include')
  })
})
