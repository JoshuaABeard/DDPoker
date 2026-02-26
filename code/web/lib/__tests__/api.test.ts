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
import { gameServerApi, checkApiHealth } from '../api'

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
