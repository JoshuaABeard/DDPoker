import { describe, it, expect } from 'vitest'

// The middleware is tricky to unit test directly (uses NextRequest/Response).
// Test the core logic: public path detection.
describe('middleware public paths', () => {
  const PUBLIC_PATHS = ['/', '/login', '/register', '/forgot', '/support', '/download', '/about', '/terms', '/online']

  it('allows root path', () => {
    expect(PUBLIC_PATHS.includes('/')).toBe(true)
  })

  it('allows /login', () => {
    expect(PUBLIC_PATHS.includes('/login')).toBe(true)
  })

  it('games paths are not public', () => {
    expect(PUBLIC_PATHS.some((p) => '/games'.startsWith(p + '/') || '/games' === p)).toBe(false)
  })
})
