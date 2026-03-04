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
import {
  authApi,
  playerApi,
  profileApi,
  gameServerApi,
  leaderboardApi,
  tournamentApi,
  templateApi,
  searchApi,
  hostApi,
  adminApi,
} from '../api'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function mockFetch(body: unknown, ok = true, status = 200) {
  const fn = vi.fn().mockResolvedValue({
    ok,
    status,
    statusText: ok ? 'OK' : 'Error',
    json: () => Promise.resolve(body),
  })
  vi.stubGlobal('fetch', fn)
  return fn
}

function capturedUrl(fn: ReturnType<typeof vi.fn>): string {
  return fn.mock.calls[0][0] as string
}

function capturedBody(fn: ReturnType<typeof vi.fn>): unknown {
  const init = fn.mock.calls[0][1] as RequestInit
  return init.body ? JSON.parse(init.body as string) : undefined
}

function capturedMethod(fn: ReturnType<typeof vi.fn>): string {
  const init = fn.mock.calls[0][1] as RequestInit
  return (init?.method ?? 'GET').toUpperCase()
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
// authApi
// ---------------------------------------------------------------------------

describe('authApi', () => {
  it('login — POSTs to /api/v1/auth/login with credentials', async () => {
    const fn = mockFetch({ success: true, message: 'ok', username: 'alice', email: 'alice@example.com', emailVerified: false })
    const result = await authApi.login({ username: 'alice', password: 'pw', rememberMe: false })
    expect(capturedUrl(fn)).toContain('/api/v1/auth/login')
    expect(capturedMethod(fn)).toBe('POST')
    expect(capturedBody(fn)).toMatchObject({ username: 'alice', password: 'pw' })
    expect(result.username).toBe('alice')
  })

  it('register — POSTs to /api/v1/auth/register', async () => {
    const fn = mockFetch({ success: true, message: null, username: 'alice', email: 'a@b.com', emailVerified: false })
    await authApi.register({ username: 'alice', email: 'a@b.com', password: 'pw' })
    expect(capturedUrl(fn)).toContain('/api/v1/auth/register')
    expect(capturedMethod(fn)).toBe('POST')
  })

  it('logout — POSTs to /api/v1/auth/logout', async () => {
    const fn = mockFetch({})
    await authApi.logout()
    expect(capturedUrl(fn)).toContain('/api/v1/auth/logout')
    expect(capturedMethod(fn)).toBe('POST')
  })

  it('getCurrentUser — GETs /api/v1/auth/me and returns user', async () => {
    const fn = mockFetch({ success: true, message: '', username: 'bob', email: 'bob@example.com', emailVerified: true })
    const result = await authApi.getCurrentUser()
    expect(capturedUrl(fn)).toContain('/api/v1/auth/me')
    expect(result?.username).toBe('bob')
  })

  it('getCurrentUser — returns null when request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network fail')))
    const result = await authApi.getCurrentUser()
    expect(result).toBeNull()
  })

  it('forgotPassword — POSTs to /api/v1/auth/forgot-password', async () => {
    const fn = mockFetch({ success: true, message: 'sent' })
    const result = await authApi.forgotPassword('alice')
    expect(capturedUrl(fn)).toContain('/api/v1/auth/forgot-password')
    expect(capturedMethod(fn)).toBe('POST')
    expect(capturedBody(fn)).toMatchObject({ username: 'alice' })
    expect(result.success).toBe(true)
  })
})

// ---------------------------------------------------------------------------
// playerApi
// ---------------------------------------------------------------------------

describe('playerApi', () => {
  it('getProfile — GETs /api/players/:id', async () => {
    const fn = mockFetch({ id: 42, name: 'alice', isActive: true, createdAt: '2026-01-01' })
    const result = await playerApi.getProfile(42)
    expect(capturedUrl(fn)).toContain('/api/players/42')
    expect(result.id).toBe(42)
  })

  it('getProfileByName — GETs /api/players/name/:name', async () => {
    const fn = mockFetch({ id: 99, name: 'bob', isActive: true, createdAt: '2026-01-01' })
    await playerApi.getProfileByName('bob')
    expect(capturedUrl(fn)).toContain('/api/players/name/bob')
  })

  it('updateProfile — PUTs to /api/players/me', async () => {
    const fn = mockFetch({ id: 1, name: 'alice updated', isActive: true, createdAt: '2026-01-01' })
    await playerApi.updateProfile({ name: 'alice updated' })
    expect(capturedUrl(fn)).toContain('/api/players/me')
    expect(capturedMethod(fn)).toBe('PUT')
  })

  it('changePassword — PUTs to /api/players/me/password', async () => {
    const fn = mockFetch({})
    await playerApi.changePassword('old', 'new')
    expect(capturedUrl(fn)).toContain('/api/players/me/password')
    expect(capturedMethod(fn)).toBe('PUT')
    expect(capturedBody(fn)).toMatchObject({ currentPassword: 'old', newPassword: 'new' })
  })
})

// ---------------------------------------------------------------------------
// profileApi
// ---------------------------------------------------------------------------

describe('profileApi', () => {
  it('getAliases — GETs /api/profile/aliases', async () => {
    const fn = mockFetch([{ name: 'al', createdDate: '2026-01-01' }])
    const result = await profileApi.getAliases()
    expect(capturedUrl(fn)).toContain('/api/profile/aliases')
    expect(result).toHaveLength(1)
  })
})

// ---------------------------------------------------------------------------
// gameServerApi — untested methods
// ---------------------------------------------------------------------------

describe('gameServerApi — remaining methods', () => {
  it('register — POSTs to /api/v1/auth/register', async () => {
    const fn = mockFetch({ success: true })
    await gameServerApi.register('alice', 'pw', 'a@b.com')
    expect(capturedUrl(fn)).toContain('/api/v1/auth/register')
    expect(capturedMethod(fn)).toBe('POST')
  })

  it('listGames — GETs /api/v1/games with no params', async () => {
    const fn = mockFetch({ games: [], total: 0 })
    await gameServerApi.listGames()
    expect(capturedUrl(fn)).toContain('/api/v1/games')
  })

  it('listGames — appends query params', async () => {
    const fn = mockFetch({ games: [], total: 0 })
    await gameServerApi.listGames({ status: 'IN_PROGRESS', page: 1 })
    expect(capturedUrl(fn)).toContain('status=IN_PROGRESS')
    expect(capturedUrl(fn)).toContain('page=1')
  })

  it('getGame — GETs /api/v1/games/:id', async () => {
    const fn = mockFetch({ gameId: 'g1', name: 'Test' })
    await gameServerApi.getGame('g1')
    expect(capturedUrl(fn)).toContain('/api/v1/games/g1')
  })

  it('createPracticeGame — POSTs to /api/v1/games/practice', async () => {
    const fn = mockFetch({ gameId: 'g-practice' })
    const result = await gameServerApi.createPracticeGame()
    expect(capturedUrl(fn)).toContain('/api/v1/games/practice')
    expect(capturedMethod(fn)).toBe('POST')
    expect(result.gameId).toBe('g-practice')
  })

  it('joinGame — POSTs to /api/v1/games/:id/join', async () => {
    const fn = mockFetch({ wsUrl: 'ws://localhost/ws', gameId: 'g1' })
    await gameServerApi.joinGame('g1')
    expect(capturedUrl(fn)).toContain('/api/v1/games/g1/join')
    expect(capturedMethod(fn)).toBe('POST')
  })

  it('joinGame — sends password in body when provided', async () => {
    const fn = mockFetch({ wsUrl: 'ws://localhost/ws', gameId: 'g1' })
    await gameServerApi.joinGame('g1', 'secret')
    expect(capturedBody(fn)).toMatchObject({ password: 'secret' })
  })

  it('startGame — POSTs to /api/v1/games/:id/start', async () => {
    const fn = mockFetch({})
    await gameServerApi.startGame('g1')
    expect(capturedUrl(fn)).toContain('/api/v1/games/g1/start')
    expect(capturedMethod(fn)).toBe('POST')
  })

  it('updateSettings — PUTs to /api/v1/games/:id/settings', async () => {
    const fn = mockFetch({ gameId: 'g1', name: 'Updated' })
    await gameServerApi.updateSettings('g1', { name: 'Updated' } as Parameters<typeof gameServerApi.updateSettings>[1])
    expect(capturedUrl(fn)).toContain('/api/v1/games/g1/settings')
    expect(capturedMethod(fn)).toBe('PUT')
  })

  it('kickPlayer — POSTs to /api/v1/games/:id/kick', async () => {
    const fn = mockFetch({})
    await gameServerApi.kickPlayer('g1', 42)
    expect(capturedUrl(fn)).toContain('/api/v1/games/g1/kick')
    expect(capturedMethod(fn)).toBe('POST')
    expect(capturedBody(fn)).toMatchObject({ profileId: 42 })
  })

  it('cancelGame — DELETEs /api/v1/games/:id', async () => {
    const fn = mockFetch({})
    await gameServerApi.cancelGame('g1')
    expect(capturedUrl(fn)).toContain('/api/v1/games/g1')
    expect(capturedMethod(fn)).toBe('DELETE')
  })

  it('observeGame — POSTs to /api/v1/games/:id/observe', async () => {
    const fn = mockFetch({ wsUrl: 'ws://localhost/observe', gameId: 'g1' })
    await gameServerApi.observeGame('g1')
    expect(capturedUrl(fn)).toContain('/api/v1/games/g1/observe')
    expect(capturedMethod(fn)).toBe('POST')
  })

  it('simulate — POSTs to /api/v1/poker/simulate', async () => {
    const fn = mockFetch({ win: 0.6, tie: 0.1, loss: 0.3 })
    await gameServerApi.simulate({
      holeCards: ['Ah', 'Kh'],
      communityCards: [],
      numOpponents: 1,
      iterations: 1000,
    })
    expect(capturedUrl(fn)).toContain('/api/v1/poker/simulate')
    expect(capturedMethod(fn)).toBe('POST')
  })
})

// ---------------------------------------------------------------------------
// leaderboardApi
// ---------------------------------------------------------------------------

describe('leaderboardApi', () => {
  it('getLeaderboard — GETs /api/leaderboard with mode param', async () => {
    const fn = mockFetch({ entries: [], total: 0 })
    await leaderboardApi.getLeaderboard('ddr1')
    expect(capturedUrl(fn)).toContain('/api/leaderboard')
    expect(capturedUrl(fn)).toContain('mode=ddr1')
  })

  it('getLeaderboard — appends optional filter params', async () => {
    const fn = mockFetch({ entries: [], total: 0 })
    await leaderboardApi.getLeaderboard('roi', 0, 50, { name: 'alice', from: '2026-01-01' })
    expect(capturedUrl(fn)).toContain('name=alice')
    expect(capturedUrl(fn)).toContain('from=2026-01-01')
  })

  it('getPlayerRank — GETs /api/leaderboard/player/:name', async () => {
    const fn = mockFetch({ rank: 1, playerName: 'alice', points: 100, gamesPlayed: 10, wins: 5, winRate: 0.5 })
    await leaderboardApi.getPlayerRank('alice')
    expect(capturedUrl(fn)).toContain('/api/leaderboard/player/alice')
  })
})

// ---------------------------------------------------------------------------
// tournamentApi
// ---------------------------------------------------------------------------

describe('tournamentApi', () => {
  it('getHistory — GETs /api/history with name param', async () => {
    const fn = mockFetch({ history: [], total: 0 })
    await tournamentApi.getHistory('alice')
    expect(capturedUrl(fn)).toContain('/api/history')
    expect(capturedUrl(fn)).toContain('name=alice')
  })

  it('getHistory — appends from/to date filters', async () => {
    const fn = mockFetch({ history: [], total: 0 })
    await tournamentApi.getHistory('alice', 0, 50, '2026-01-01', '2026-12-31')
    expect(capturedUrl(fn)).toContain('from=2026-01-01')
    expect(capturedUrl(fn)).toContain('to=2026-12-31')
  })

  it('getDetails — GETs /api/tournaments/:id', async () => {
    const fn = mockFetch({ id: 5, gameName: 'Test', placement: 1, totalPlayers: 8, prizeWon: 100, date: '2026-01-01' })
    await tournamentApi.getDetails(5)
    expect(capturedUrl(fn)).toContain('/api/tournaments/5')
  })
})

// ---------------------------------------------------------------------------
// templateApi
// ---------------------------------------------------------------------------

describe('templateApi', () => {
  it('list — GETs /api/profile/templates', async () => {
    const fn = mockFetch([])
    await templateApi.list()
    expect(capturedUrl(fn)).toContain('/api/profile/templates')
    expect(capturedMethod(fn)).toBe('GET')
  })

  it('create — POSTs to /api/profile/templates', async () => {
    const fn = mockFetch({ id: 1, name: 'T1', config: '{}', createdDate: '', modifiedDate: '' })
    await templateApi.create('T1', { maxPlayers: 6 })
    expect(capturedUrl(fn)).toContain('/api/profile/templates')
    expect(capturedMethod(fn)).toBe('POST')
    expect(capturedBody(fn)).toMatchObject({ name: 'T1' })
  })

  it('update — PUTs to /api/profile/templates/:id', async () => {
    const fn = mockFetch({ id: 1, name: 'T1 Updated', config: '{}', createdDate: '', modifiedDate: '' })
    await templateApi.update(1, 'T1 Updated', { maxPlayers: 9 })
    expect(capturedUrl(fn)).toContain('/api/profile/templates/1')
    expect(capturedMethod(fn)).toBe('PUT')
  })

  it('delete — DELETEs /api/profile/templates/:id', async () => {
    const fn = mockFetch({})
    await templateApi.delete(1)
    expect(capturedUrl(fn)).toContain('/api/profile/templates/1')
    expect(capturedMethod(fn)).toBe('DELETE')
  })
})

// ---------------------------------------------------------------------------
// searchApi
// ---------------------------------------------------------------------------

describe('searchApi', () => {
  it('searchPlayers — GETs /api/search with name param', async () => {
    const fn = mockFetch([])
    await searchApi.searchPlayers('alice')
    expect(capturedUrl(fn)).toContain('/api/search')
    expect(capturedUrl(fn)).toContain('name=alice')
  })
})

// ---------------------------------------------------------------------------
// hostApi
// ---------------------------------------------------------------------------

describe('hostApi', () => {
  it('getHosts — GETs /api/games/hosts', async () => {
    const fn = mockFetch({ hosts: [], total: 0 })
    await hostApi.getHosts()
    expect(capturedUrl(fn)).toContain('/api/games/hosts')
  })

  it('getHosts — appends optional filter params', async () => {
    const fn = mockFetch({ hosts: [], total: 0 })
    await hostApi.getHosts('alice', '2026-01-01', '2026-12-31')
    expect(capturedUrl(fn)).toContain('search=alice')
    expect(capturedUrl(fn)).toContain('from=2026-01-01')
  })
})

// ---------------------------------------------------------------------------
// adminApi
// ---------------------------------------------------------------------------

describe('adminApi', () => {
  it('searchProfiles — GETs /api/admin/profiles', async () => {
    const fn = mockFetch({ profiles: [], total: 0 })
    await adminApi.searchProfiles({ name: 'alice', page: 0, pageSize: 20 })
    expect(capturedUrl(fn)).toContain('/api/admin/profiles')
    expect(capturedUrl(fn)).toContain('name=alice')
  })

  it('searchProfiles — works with no filters', async () => {
    const fn = mockFetch({ profiles: [], total: 0 })
    await adminApi.searchProfiles()
    expect(capturedUrl(fn)).toContain('/api/admin/profiles')
  })

  it('getBans — GETs /api/admin/bans and wraps in { bans, total }', async () => {
    const fn = mockFetch([{ id: 1, key: 'abc', createDate: '2026-01-01' }])
    const result = await adminApi.getBans()
    expect(capturedUrl(fn)).toContain('/api/admin/bans')
    expect(result.bans).toHaveLength(1)
    expect(result.total).toBe(1)
  })

  it('addBan — POSTs to /api/admin/bans', async () => {
    const fn = mockFetch({ id: 2, key: 'xyz', createDate: '2026-01-01' })
    await adminApi.addBan({ key: 'xyz', comment: 'spammer' })
    expect(capturedUrl(fn)).toContain('/api/admin/bans')
    expect(capturedMethod(fn)).toBe('POST')
    expect(capturedBody(fn)).toMatchObject({ key: 'xyz', comment: 'spammer' })
  })

  it('removeBan — DELETEs /api/admin/bans/:key (URL-encoded)', async () => {
    const fn = mockFetch({})
    await adminApi.removeBan('abc def')
    expect(capturedUrl(fn)).toContain('/api/admin/bans/abc%20def')
    expect(capturedMethod(fn)).toBe('DELETE')
  })
})
