/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { NextRequest } from 'next/server'
import { middleware } from '../middleware'

function makeRequest(pathname: string, cookies: Record<string, string> = {}): NextRequest {
  const url = `http://localhost${pathname}`
  const req = new NextRequest(url)
  for (const [name, value] of Object.entries(cookies)) {
    req.cookies.set(name, value)
  }
  return req
}

describe('middleware', () => {
  describe('public paths — always allowed regardless of auth', () => {
    const publicPaths = ['/', '/login', '/register', '/forgot', '/support', '/download', '/about', '/terms', '/online']

    for (const path of publicPaths) {
      it(`allows ${path} without auth cookie`, () => {
        const res = middleware(makeRequest(path))
        // NextResponse.next() has no Location header (not a redirect)
        expect(res.headers.get('location')).toBeNull()
      })
    }

    it('allows /_next/* paths', () => {
      const res = middleware(makeRequest('/_next/static/chunk.js'))
      expect(res.headers.get('location')).toBeNull()
    })

    it('allows /api/* paths', () => {
      const res = middleware(makeRequest('/api/health'))
      expect(res.headers.get('location')).toBeNull()
    })
  })

  describe('protected paths — require DDPoker-JWT cookie', () => {
    it('redirects unauthenticated /games to /login with returnUrl', () => {
      const res = middleware(makeRequest('/games'))
      const location = res.headers.get('location')
      expect(location).not.toBeNull()
      expect(location).toContain('/login')
      expect(location).toContain('returnUrl=%2Fgames')
    })

    it('redirects unauthenticated /games/abc/play to /login', () => {
      const res = middleware(makeRequest('/games/abc/play'))
      const location = res.headers.get('location')
      expect(location).toContain('/login')
      expect(location).toContain('returnUrl')
    })

    it('allows authenticated request with DDPoker-JWT cookie', () => {
      const res = middleware(makeRequest('/games', { 'DDPoker-JWT': 'valid-token' }))
      expect(res.headers.get('location')).toBeNull()
    })

    it('does NOT accept auth_token as a substitute', () => {
      const res = middleware(makeRequest('/games', { 'auth_token': 'valid-token' }))
      // Should still redirect since wrong cookie name
      expect(res.headers.get('location')).toContain('/login')
    })
  })
})
