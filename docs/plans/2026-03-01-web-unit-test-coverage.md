# Web Client Unit Test Coverage Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Write 23 new test files covering all untested web client source files, then enforce 80% coverage thresholds.

**Architecture:** Bottom-up by dependency layer — pure storage utilities first, then auth hooks/context, then presentational components, then interactive components with timers/events, then auth/profile/layout UI, and finally PokerTable (the integration root). Each layer's tests validate mocking patterns for the next.

**Tech Stack:** Vitest 3, React Testing Library 16, jsdom 26, `@testing-library/user-event` 14

**Design doc:** `docs/plans/2026-03-01-web-unit-test-coverage-design.md`

---

### Task 1: Auth Storage Tests

**Files:**
- Create: `code/web/lib/auth/__tests__/storage.test.ts`
- Source: `code/web/lib/auth/storage.ts`

**Step 1: Write tests**

```ts
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { getAuthUser, setAuthUser, clearAuthUser } from '../storage'

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
})

describe('getAuthUser', () => {
  it('returns null when both storages are empty', () => {
    expect(getAuthUser()).toBeNull()
  })

  it('returns user from localStorage (rememberMe)', () => {
    localStorage.setItem('ddpoker_auth_user', JSON.stringify({ username: 'alice' }))
    expect(getAuthUser()).toEqual({ username: 'alice' })
  })

  it('returns user from sessionStorage when localStorage is empty', () => {
    sessionStorage.setItem('ddpoker_auth_user', JSON.stringify({ username: 'bob' }))
    expect(getAuthUser()).toEqual({ username: 'bob' })
  })

  it('prefers localStorage over sessionStorage', () => {
    localStorage.setItem('ddpoker_auth_user', JSON.stringify({ username: 'alice' }))
    sessionStorage.setItem('ddpoker_auth_user', JSON.stringify({ username: 'bob' }))
    expect(getAuthUser()).toEqual({ username: 'alice' })
  })

  it('self-heals corrupt localStorage JSON', () => {
    localStorage.setItem('ddpoker_auth_user', 'not-json')
    expect(getAuthUser()).toBeNull()
    expect(localStorage.getItem('ddpoker_auth_user')).toBeNull()
  })

  it('self-heals corrupt sessionStorage JSON', () => {
    sessionStorage.setItem('ddpoker_auth_user', '{bad')
    expect(getAuthUser()).toBeNull()
    expect(sessionStorage.getItem('ddpoker_auth_user')).toBeNull()
  })
})

describe('setAuthUser', () => {
  it('stores in localStorage and clears sessionStorage when rememberMe is true', () => {
    sessionStorage.setItem('ddpoker_auth_user', 'old')
    setAuthUser({ username: 'alice' }, true)
    expect(JSON.parse(localStorage.getItem('ddpoker_auth_user')!)).toEqual({ username: 'alice' })
    expect(sessionStorage.getItem('ddpoker_auth_user')).toBeNull()
  })

  it('stores in sessionStorage and clears localStorage when rememberMe is false', () => {
    localStorage.setItem('ddpoker_auth_user', 'old')
    setAuthUser({ username: 'bob' }, false)
    expect(JSON.parse(sessionStorage.getItem('ddpoker_auth_user')!)).toEqual({ username: 'bob' })
    expect(localStorage.getItem('ddpoker_auth_user')).toBeNull()
  })
})

describe('clearAuthUser', () => {
  it('removes from both storages', () => {
    localStorage.setItem('ddpoker_auth_user', JSON.stringify({ username: 'alice' }))
    sessionStorage.setItem('ddpoker_auth_user', JSON.stringify({ username: 'bob' }))
    clearAuthUser()
    expect(localStorage.getItem('ddpoker_auth_user')).toBeNull()
    expect(sessionStorage.getItem('ddpoker_auth_user')).toBeNull()
  })
})
```

**Step 2: Run tests**

```bash
cd code/web && npx vitest run lib/auth/__tests__/storage.test.ts
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/lib/auth/__tests__/storage.test.ts
git commit -m "test: add storage.ts unit tests (lib/auth)"
```

---

### Task 2: useAuth Hook Tests

**Files:**
- Create: `code/web/lib/auth/__tests__/useAuth.test.tsx`
- Source: `code/web/lib/auth/useAuth.ts`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { useAuth } from '../useAuth'
import { AuthContext } from '../AuthContext'

describe('useAuth', () => {
  it('throws when used outside AuthProvider', () => {
    expect(() => renderHook(() => useAuth())).toThrow(
      'useAuth must be used within an AuthProvider'
    )
  })

  it('returns context value when inside AuthProvider', () => {
    const mockValue = {
      user: { username: 'alice', isAdmin: false },
      isAuthenticated: true,
      isLoading: false,
      error: null,
      login: async () => true,
      logout: async () => {},
      checkAuthStatus: async () => {},
      clearError: () => {},
    }

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <AuthContext.Provider value={mockValue}>{children}</AuthContext.Provider>
    )

    const { result } = renderHook(() => useAuth(), { wrapper })
    expect(result.current.user?.username).toBe('alice')
    expect(result.current.isAuthenticated).toBe(true)
  })
})
```

**Step 2: Run tests**

```bash
cd code/web && npx vitest run lib/auth/__tests__/useAuth.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/lib/auth/__tests__/useAuth.test.tsx
git commit -m "test: add useAuth hook tests (lib/auth)"
```

---

### Task 3: AuthContext / AuthProvider Tests

**Files:**
- Create: `code/web/lib/auth/__tests__/AuthContext.test.tsx`
- Source: `code/web/lib/auth/AuthContext.tsx`

**Step 1: Write tests**

Mock `authApi` from `@/lib/api` and `./storage` functions. Render a consumer component inside `<AuthProvider>` to read state and call methods.

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, act, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AuthProvider } from '../AuthContext'
import { useAuth } from '../useAuth'

// --- Mocks ---

vi.mock('@/lib/api', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    getCurrentUser: vi.fn(),
  },
}))

vi.mock('../storage', () => ({
  getAuthUser: vi.fn(),
  setAuthUser: vi.fn(),
  clearAuthUser: vi.fn(),
}))

import { authApi } from '@/lib/api'
import { getAuthUser, setAuthUser, clearAuthUser } from '../storage'

const mockGetAuthUser = vi.mocked(getAuthUser)
const mockSetAuthUser = vi.mocked(setAuthUser)
const mockClearAuthUser = vi.mocked(clearAuthUser)
const mockLogin = vi.mocked(authApi.login)
const mockLogout = vi.mocked(authApi.logout)
const mockGetCurrentUser = vi.mocked(authApi.getCurrentUser)

// Consumer component that exposes auth state for assertions
function AuthConsumer() {
  const auth = useAuth()
  return (
    <div>
      <span data-testid="loading">{String(auth.isLoading)}</span>
      <span data-testid="authenticated">{String(auth.isAuthenticated)}</span>
      <span data-testid="username">{auth.user?.username ?? ''}</span>
      <span data-testid="admin">{String(auth.user?.isAdmin ?? false)}</span>
      <span data-testid="error">{auth.error ?? ''}</span>
      <button onClick={() => auth.login('user1', 'pass1', true)}>login</button>
      <button onClick={() => auth.logout()}>logout</button>
      <button onClick={() => auth.clearError()}>clearError</button>
    </div>
  )
}

beforeEach(() => {
  vi.resetAllMocks()
})

describe('AuthProvider', () => {
  // --- Mount behavior ---

  it('checks stored user on mount and sets authenticated when session is valid', async () => {
    mockGetAuthUser.mockReturnValue({ username: 'alice' })
    mockGetCurrentUser.mockResolvedValue({ success: true, username: 'alice', admin: true })

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })
    expect(screen.getByTestId('authenticated').textContent).toBe('true')
    expect(screen.getByTestId('username').textContent).toBe('alice')
    expect(screen.getByTestId('admin').textContent).toBe('true')
  })

  it('stays unauthenticated when no stored user exists', async () => {
    mockGetAuthUser.mockReturnValue(null)

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
    expect(mockGetCurrentUser).not.toHaveBeenCalled()
  })

  it('clears stored user and stays unauthenticated when API rejects session', async () => {
    mockGetAuthUser.mockReturnValue({ username: 'alice' })
    mockGetCurrentUser.mockRejectedValue(new Error('expired'))

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
    expect(mockClearAuthUser).toHaveBeenCalled()
  })

  it('clears stored user when API returns unsuccessful response', async () => {
    mockGetAuthUser.mockReturnValue({ username: 'alice' })
    mockGetCurrentUser.mockResolvedValue({ success: false, username: '', admin: false })

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('authenticated').textContent).toBe('false')
    })
    expect(mockClearAuthUser).toHaveBeenCalled()
  })

  // --- Login ---

  it('login success stores user and updates state', async () => {
    mockGetAuthUser.mockReturnValue(null)
    mockLogin.mockResolvedValue({ success: true, username: 'user1', admin: false })

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })

    await act(async () => {
      screen.getByText('login').click()
    })

    await waitFor(() => {
      expect(screen.getByTestId('authenticated').textContent).toBe('true')
    })
    expect(screen.getByTestId('username').textContent).toBe('user1')
    expect(mockSetAuthUser).toHaveBeenCalledWith({ username: 'user1' }, true)
  })

  it('login failure sets error', async () => {
    mockGetAuthUser.mockReturnValue(null)
    mockLogin.mockResolvedValue({ success: false, username: '', message: 'Bad credentials' })

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })

    await act(async () => {
      screen.getByText('login').click()
    })

    await waitFor(() => {
      expect(screen.getByTestId('error').textContent).toBe('Bad credentials')
    })
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
  })

  it('login exception sets error message', async () => {
    mockGetAuthUser.mockReturnValue(null)
    mockLogin.mockRejectedValue(new Error('Network failure'))

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })

    await act(async () => {
      screen.getByText('login').click()
    })

    await waitFor(() => {
      expect(screen.getByTestId('error').textContent).toBe('Network failure')
    })
  })

  // --- Logout ---

  it('logout clears storage and resets state', async () => {
    mockGetAuthUser.mockReturnValue({ username: 'alice' })
    mockGetCurrentUser.mockResolvedValue({ success: true, username: 'alice', admin: false })
    mockLogout.mockResolvedValue(undefined)

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('authenticated').textContent).toBe('true')
    })

    await act(async () => {
      screen.getByText('logout').click()
    })

    await waitFor(() => {
      expect(screen.getByTestId('authenticated').textContent).toBe('false')
    })
    expect(mockClearAuthUser).toHaveBeenCalled()
  })

  it('logout continues even when API call fails', async () => {
    mockGetAuthUser.mockReturnValue({ username: 'alice' })
    mockGetCurrentUser.mockResolvedValue({ success: true, username: 'alice', admin: false })
    mockLogout.mockRejectedValue(new Error('Server down'))

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('authenticated').textContent).toBe('true')
    })

    await act(async () => {
      screen.getByText('logout').click()
    })

    await waitFor(() => {
      expect(screen.getByTestId('authenticated').textContent).toBe('false')
    })
    expect(mockClearAuthUser).toHaveBeenCalled()
  })

  // --- clearError ---

  it('clearError nulls error', async () => {
    mockGetAuthUser.mockReturnValue(null)
    mockLogin.mockResolvedValue({ success: false, username: '', message: 'Wrong' })

    render(<AuthProvider><AuthConsumer /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })

    await act(async () => {
      screen.getByText('login').click()
    })

    await waitFor(() => {
      expect(screen.getByTestId('error').textContent).toBe('Wrong')
    })

    await act(async () => {
      screen.getByText('clearError').click()
    })

    expect(screen.getByTestId('error').textContent).toBe('')
  })
})
```

**Step 2: Run tests**

```bash
cd code/web && npx vitest run lib/auth/__tests__/AuthContext.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/lib/auth/__tests__/AuthContext.test.tsx
git commit -m "test: add AuthProvider integration tests (lib/auth)"
```

---

### Task 4: useRequireAuth Hook Tests

**Files:**
- Create: `code/web/lib/auth/__tests__/useRequireAuth.test.ts`
- Source: `code/web/lib/auth/useRequireAuth.ts`

**Step 1: Write tests**

```ts
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useRequireAuth } from '../useRequireAuth'

// --- Mocks ---

const mockPush = vi.fn()

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => '/admin/dashboard',
}))

vi.mock('../useAuth', () => ({
  useAuth: vi.fn(),
}))

import { useAuth } from '../useAuth'
const mockUseAuth = vi.mocked(useAuth)

beforeEach(() => {
  vi.clearAllMocks()
})

describe('useRequireAuth', () => {
  it('does not redirect while loading', () => {
    mockUseAuth.mockReturnValue({
      user: null, isAuthenticated: false, isLoading: true, error: null,
      login: vi.fn(), logout: vi.fn(), checkAuthStatus: vi.fn(), clearError: vi.fn(),
    })

    renderHook(() => useRequireAuth())
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('redirects to /login with returnUrl when not authenticated', () => {
    mockUseAuth.mockReturnValue({
      user: null, isAuthenticated: false, isLoading: false, error: null,
      login: vi.fn(), logout: vi.fn(), checkAuthStatus: vi.fn(), clearError: vi.fn(),
    })

    renderHook(() => useRequireAuth())
    expect(mockPush).toHaveBeenCalledWith(
      `/login?returnUrl=${encodeURIComponent('/admin/dashboard')}`
    )
  })

  it('does not redirect when authenticated and admin not required', () => {
    mockUseAuth.mockReturnValue({
      user: { username: 'alice', isAdmin: false },
      isAuthenticated: true, isLoading: false, error: null,
      login: vi.fn(), logout: vi.fn(), checkAuthStatus: vi.fn(), clearError: vi.fn(),
    })

    renderHook(() => useRequireAuth())
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('redirects non-admin to / when admin is required', () => {
    mockUseAuth.mockReturnValue({
      user: { username: 'alice', isAdmin: false },
      isAuthenticated: true, isLoading: false, error: null,
      login: vi.fn(), logout: vi.fn(), checkAuthStatus: vi.fn(), clearError: vi.fn(),
    })

    renderHook(() => useRequireAuth({ requireAdmin: true }))
    expect(mockPush).toHaveBeenCalledWith('/')
  })

  it('does not redirect admin when admin is required', () => {
    mockUseAuth.mockReturnValue({
      user: { username: 'admin', isAdmin: true },
      isAuthenticated: true, isLoading: false, error: null,
      login: vi.fn(), logout: vi.fn(), checkAuthStatus: vi.fn(), clearError: vi.fn(),
    })

    renderHook(() => useRequireAuth({ requireAdmin: true }))
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('returns user, isLoading, isAuthenticated', () => {
    mockUseAuth.mockReturnValue({
      user: { username: 'alice', isAdmin: false },
      isAuthenticated: true, isLoading: false, error: null,
      login: vi.fn(), logout: vi.fn(), checkAuthStatus: vi.fn(), clearError: vi.fn(),
    })

    const { result } = renderHook(() => useRequireAuth())
    expect(result.current.user?.username).toBe('alice')
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.isLoading).toBe(false)
  })
})
```

**Step 2: Run all auth tests**

```bash
cd code/web && npx vitest run lib/auth/__tests__/
```
Expected: all 4 files PASS

**Step 3: Commit**

```bash
git add code/web/lib/auth/__tests__/useRequireAuth.test.ts
git commit -m "test: add useRequireAuth route guard tests (lib/auth)"
```

---

### Task 5: DealerButton Tests

**Files:**
- Create: `code/web/components/game/__tests__/DealerButton.test.tsx`
- Source: `code/web/components/game/DealerButton.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { DealerButton } from '../DealerButton'

describe('DealerButton', () => {
  it('renders D for dealer', () => {
    render(<DealerButton type="dealer" />)
    expect(screen.getByText('D')).toBeTruthy()
    expect(screen.getByLabelText('dealer marker')).toBeTruthy()
  })

  it('renders SB for small blind', () => {
    render(<DealerButton type="small-blind" />)
    expect(screen.getByText('SB')).toBeTruthy()
    expect(screen.getByLabelText('small-blind marker')).toBeTruthy()
  })

  it('renders BB for big blind', () => {
    render(<DealerButton type="big-blind" />)
    expect(screen.getByText('BB')).toBeTruthy()
    expect(screen.getByLabelText('big-blind marker')).toBeTruthy()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/game/__tests__/DealerButton.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/__tests__/DealerButton.test.tsx
git commit -m "test: add DealerButton component tests"
```

---

### Task 6: Footer Tests

**Files:**
- Create: `code/web/components/layout/__tests__/Footer.test.tsx`
- Source: `code/web/components/layout/Footer.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import Footer from '../Footer'

describe('Footer', () => {
  it('renders community edition text', () => {
    render(<Footer />)
    expect(screen.getByText('DD Poker Community Edition')).toBeTruthy()
  })

  it('renders ddpoker.com link with correct href', () => {
    render(<Footer />)
    const link = screen.getByRole('link', { name: /Doug Donohoe/i })
    expect(link).toBeTruthy()
    expect(link.getAttribute('href')).toBe('https://www.ddpoker.com')
    expect(link.getAttribute('target')).toBe('_blank')
    expect(link.getAttribute('rel')).toContain('noopener')
  })

  it('renders trademark notice', () => {
    render(<Footer />)
    expect(screen.getByText(/DD Poker™ is a trademark/)).toBeTruthy()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/layout/__tests__/Footer.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/layout/__tests__/Footer.test.tsx
git commit -m "test: add Footer component tests"
```

---

### Task 7: DashboardWidget Tests

**Files:**
- Create: `code/web/components/game/__tests__/DashboardWidget.test.tsx`
- Source: `code/web/components/game/DashboardWidget.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { DashboardWidget } from '../DashboardWidget'

describe('DashboardWidget', () => {
  it('shows children when expanded by default', () => {
    render(<DashboardWidget title="Stats"><p>content</p></DashboardWidget>)
    expect(screen.getByText('content')).toBeTruthy()
    expect(screen.getByRole('button', { name: /stats/i })).toBeTruthy()
  })

  it('has aria-expanded=true when expanded', () => {
    render(<DashboardWidget title="Stats"><p>content</p></DashboardWidget>)
    expect(screen.getByRole('button').getAttribute('aria-expanded')).toBe('true')
  })

  it('hides children when defaultExpanded is false', () => {
    render(<DashboardWidget title="Stats" defaultExpanded={false}><p>content</p></DashboardWidget>)
    expect(screen.queryByText('content')).toBeNull()
    expect(screen.getByRole('button').getAttribute('aria-expanded')).toBe('false')
  })

  it('toggles children on click', () => {
    render(<DashboardWidget title="Stats"><p>content</p></DashboardWidget>)
    const btn = screen.getByRole('button')

    fireEvent.click(btn)
    expect(screen.queryByText('content')).toBeNull()
    expect(btn.getAttribute('aria-expanded')).toBe('false')

    fireEvent.click(btn)
    expect(screen.getByText('content')).toBeTruthy()
    expect(btn.getAttribute('aria-expanded')).toBe('true')
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/game/__tests__/DashboardWidget.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/__tests__/DashboardWidget.test.tsx
git commit -m "test: add DashboardWidget component tests"
```

---

### Task 8: TableFelt Tests

**Files:**
- Create: `code/web/components/game/__tests__/TableFelt.test.tsx`
- Source: `code/web/components/game/TableFelt.tsx`

**Step 1: Write tests**

Mock `CommunityCards` and `PotDisplay` child components.

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { TableFelt } from '../TableFelt'

vi.mock('../CommunityCards', () => ({
  CommunityCards: (props: { cards: string[]; fourColorDeck?: boolean }) => (
    <div data-testid="community-cards" data-four-color={props.fourColorDeck} />
  ),
}))

vi.mock('../PotDisplay', () => ({
  PotDisplay: () => <div data-testid="pot-display" />,
}))

describe('TableFelt', () => {
  const minimalTable = { tableId: 1, seats: [], communityCards: ['Ah', 'Kd'], pots: [{ amount: 100, eligiblePlayers: [1] }], currentRound: 'FLOP', handNumber: 1 }

  it('renders community cards and pot display', () => {
    render(<TableFelt table={minimalTable} />)
    expect(screen.getByTestId('community-cards')).toBeTruthy()
    expect(screen.getByTestId('pot-display')).toBeTruthy()
  })

  it('has role=region with aria-label', () => {
    render(<TableFelt table={minimalTable} />)
    expect(screen.getByRole('region', { name: 'Poker table felt' })).toBeTruthy()
  })

  it('applies custom theme colors to style', () => {
    const colors = { center: '#ff0000', mid: '#00ff00', edge: '#0000ff', border: '#111111' }
    const { container } = render(<TableFelt table={minimalTable} colors={colors} />)
    const felt = container.querySelector('[role="region"]') as HTMLElement
    expect(felt.style.border).toContain('#111111')
  })

  it('passes fourColorDeck to CommunityCards', () => {
    render(<TableFelt table={minimalTable} fourColorDeck={true} />)
    expect(screen.getByTestId('community-cards').getAttribute('data-four-color')).toBe('true')
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/game/__tests__/TableFelt.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/__tests__/TableFelt.test.tsx
git commit -m "test: add TableFelt component tests"
```

---

### Task 9: TournamentInfoBar Tests

**Files:**
- Create: `code/web/components/game/__tests__/TournamentInfoBar.test.tsx`
- Source: `code/web/components/game/TournamentInfoBar.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { TournamentInfoBar } from '../TournamentInfoBar'

function makeProps(overrides = {}) {
  return {
    level: 3,
    blinds: { small: 50, big: 100, ante: 0 },
    nextLevelIn: 180,
    playerCount: 6,
    gameName: 'Test Tournament',
    ...overrides,
  }
}

describe('TournamentInfoBar', () => {
  it('renders game name, level, and blinds', () => {
    render(<TournamentInfoBar {...makeProps()} />)
    expect(screen.getByText('Test Tournament')).toBeTruthy()
    expect(screen.getByText('3')).toBeTruthy()
    expect(screen.getByText(/50\/100/)).toBeTruthy()
  })

  it('shows ante when non-zero', () => {
    render(<TournamentInfoBar {...makeProps({ blinds: { small: 50, big: 100, ante: 10 } })} />)
    expect(screen.getByText(/\(10\)/)).toBeTruthy()
  })

  it('does not show ante when zero', () => {
    const { container } = render(<TournamentInfoBar {...makeProps()} />)
    expect(container.textContent).not.toContain('(0)')
  })

  it('shows timer when nextLevelIn is set', () => {
    render(<TournamentInfoBar {...makeProps({ nextLevelIn: 90 })} />)
    expect(screen.getByText('1:30')).toBeTruthy()
  })

  it('hides timer when nextLevelIn is null', () => {
    render(<TournamentInfoBar {...makeProps({ nextLevelIn: null })} />)
    expect(screen.queryByText(/Next/)).toBeNull()
  })

  it('renders playerCount/totalPlayers when totalPlayers provided', () => {
    render(<TournamentInfoBar {...makeProps({ playerCount: 6, totalPlayers: 9 })} />)
    expect(screen.getByText('6/9')).toBeTruthy()
  })

  it('renders playerCount alone when totalPlayers not provided', () => {
    render(<TournamentInfoBar {...makeProps({ playerCount: 6 })} />)
    expect(screen.getByText('6')).toBeTruthy()
  })

  it('shows player rank when provided', () => {
    render(<TournamentInfoBar {...makeProps({ playerRank: 2 })} />)
    expect(screen.getByText('2')).toBeTruthy()
  })

  it('hides rank when not provided', () => {
    const { container } = render(<TournamentInfoBar {...makeProps()} />)
    expect(container.textContent).not.toContain('Rank')
  })

  it('shows break hint when blindStructure has a break ahead', () => {
    const blindStructure = [
      { smallBlind: 25, bigBlind: 50, ante: 0, minutes: 15 },
      { smallBlind: 50, bigBlind: 100, ante: 0, minutes: 15 },
      { smallBlind: 50, bigBlind: 100, ante: 0, minutes: 0, isBreak: true },
    ]
    render(<TournamentInfoBar {...makeProps({ blindStructure, currentLevel: 0 })} />)
    expect(screen.getByText('Break in 2 levels')).toBeTruthy()
  })

  it('shows singular level for break 1 level away', () => {
    const blindStructure = [
      { smallBlind: 25, bigBlind: 50, ante: 0, minutes: 15 },
      { smallBlind: 50, bigBlind: 100, ante: 0, minutes: 0, isBreak: true },
    ]
    render(<TournamentInfoBar {...makeProps({ blindStructure, currentLevel: 0 })} />)
    expect(screen.getByText('Break in 1 level')).toBeTruthy()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/game/__tests__/TournamentInfoBar.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/__tests__/TournamentInfoBar.test.tsx
git commit -m "test: add TournamentInfoBar component tests"
```

---

### Task 10: DataTable Tests

**Files:**
- Create: `code/web/components/data/__tests__/DataTable.test.tsx`
- Source: `code/web/components/data/DataTable.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { DataTable } from '../DataTable'

interface TestRow {
  id: number
  name: string
  score: number
}

const columns = [
  { key: 'name', header: 'Name', render: (item: TestRow) => item.name },
  { key: 'score', header: 'Score', render: (item: TestRow) => item.score, align: 'right' as const },
]

const data: TestRow[] = [
  { id: 1, name: 'Alice', score: 100 },
  { id: 2, name: 'Bob', score: 200 },
]

describe('DataTable', () => {
  it('renders all rows and columns', () => {
    render(<DataTable data={data} columns={columns} />)
    expect(screen.getByText('Name')).toBeTruthy()
    expect(screen.getByText('Score')).toBeTruthy()
    expect(screen.getByText('Alice')).toBeTruthy()
    expect(screen.getByText('Bob')).toBeTruthy()
    expect(screen.getByText('100')).toBeTruthy()
    expect(screen.getByText('200')).toBeTruthy()
  })

  it('shows emptyMessage when data is empty', () => {
    render(<DataTable data={[]} columns={columns} />)
    expect(screen.getByText('No data available')).toBeTruthy()
  })

  it('shows custom emptyMessage', () => {
    render(<DataTable data={[]} columns={columns} emptyMessage="Nothing here" />)
    expect(screen.getByText('Nothing here')).toBeTruthy()
  })

  it('highlights current user row', () => {
    const { container } = render(
      <DataTable data={data} columns={columns} currentUser="Alice" highlightField="name" />
    )
    const rows = container.querySelectorAll('tbody tr')
    expect(rows[0].className).toContain('bg-[var(--bg-khaki)]')
    expect(rows[1].className).not.toContain('bg-[var(--bg-khaki)]')
  })

  it('uses keyField for row keys', () => {
    const { container } = render(
      <DataTable data={data} columns={columns} keyField="id" />
    )
    const rows = container.querySelectorAll('tbody tr')
    expect(rows).toHaveLength(2)
  })

  it('calls custom render function per cell', () => {
    const customColumns = [
      { key: 'name', header: 'Name', render: (item: TestRow) => <strong>{item.name}</strong> },
    ]
    render(<DataTable data={data} columns={customColumns} />)
    expect(screen.getByText('Alice').tagName).toBe('STRONG')
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/data/__tests__/DataTable.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/data/__tests__/DataTable.test.tsx
git commit -m "test: add DataTable component tests"
```

---

### Task 11: ActionTimer Tests

**Files:**
- Create: `code/web/components/game/__tests__/ActionTimer.test.tsx`
- Source: `code/web/components/game/ActionTimer.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ActionTimer } from '../ActionTimer'

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('ActionTimer', () => {
  it('renders initial seconds from totalSeconds when remainingSeconds is null', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={null} />)
    expect(screen.getByRole('timer')).toBeTruthy()
    expect(screen.getByText('30s')).toBeTruthy()
  })

  it('renders remainingSeconds when provided', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={20} />)
    expect(screen.getByText('20s')).toBeTruthy()
  })

  it('counts down each second', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={10} />)
    expect(screen.getByText('10s')).toBeTruthy()

    act(() => { vi.advanceTimersByTime(1000) })
    expect(screen.getByText('9s')).toBeTruthy()

    act(() => { vi.advanceTimersByTime(1000) })
    expect(screen.getByText('8s')).toBeTruthy()
  })

  it('does not go below 0', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={1} />)

    act(() => { vi.advanceTimersByTime(2000) })
    expect(screen.getByText('0s')).toBeTruthy()

    act(() => { vi.advanceTimersByTime(1000) })
    expect(screen.getByText('0s')).toBeTruthy()
  })

  it('resyncs when remainingSeconds prop changes', () => {
    const { rerender } = render(<ActionTimer totalSeconds={30} remainingSeconds={20} />)
    act(() => { vi.advanceTimersByTime(3000) })
    expect(screen.getByText('17s')).toBeTruthy()

    rerender(<ActionTimer totalSeconds={30} remainingSeconds={25} />)
    expect(screen.getByText('25s')).toBeTruthy()
  })

  it('sets aria-label with remaining seconds', () => {
    render(<ActionTimer totalSeconds={30} remainingSeconds={15} />)
    expect(screen.getByLabelText('15 seconds remaining')).toBeTruthy()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/game/__tests__/ActionTimer.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/__tests__/ActionTimer.test.tsx
git commit -m "test: add ActionTimer component tests"
```

---

### Task 12: BetSlider Tests

**Files:**
- Create: `code/web/components/game/__tests__/BetSlider.test.tsx`
- Source: `code/web/components/game/BetSlider.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { BetSlider } from '../BetSlider'

describe('BetSlider', () => {
  it('renders Min and All-In preset buttons always', () => {
    const onChange = vi.fn()
    render(<BetSlider min={100} max={1000} value={500} potSize={200} onChange={onChange} />)
    expect(screen.getByRole('button', { name: /min/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /all-in/i })).toBeTruthy()
  })

  it('shows half-pot button when halfPot is strictly between min and max', () => {
    const onChange = vi.fn()
    // potSize=400, halfPot=200, min=100, max=1000 → 200 > 100 && 200 < 1000 → show
    render(<BetSlider min={100} max={1000} value={200} potSize={400} onChange={onChange} />)
    expect(screen.getByRole('button', { name: /½ pot/i })).toBeTruthy()
  })

  it('hides half-pot button when halfPot equals min', () => {
    const onChange = vi.fn()
    // potSize=200, halfPot=100, min=100 → 100 > 100 is false → hide
    render(<BetSlider min={100} max={1000} value={100} potSize={200} onChange={onChange} />)
    expect(screen.queryByRole('button', { name: /½ pot/i })).toBeNull()
  })

  it('shows pot button when pot is strictly between min and max', () => {
    const onChange = vi.fn()
    render(<BetSlider min={100} max={1000} value={200} potSize={500} onChange={onChange} />)
    expect(screen.getByRole('button', { name: /^pot$/i })).toBeTruthy()
  })

  it('hides pot button when pot equals max', () => {
    const onChange = vi.fn()
    render(<BetSlider min={100} max={500} value={200} potSize={500} onChange={onChange} />)
    expect(screen.queryByRole('button', { name: /^pot$/i })).toBeNull()
  })

  it('clicking Min fires onChange with min value', () => {
    const onChange = vi.fn()
    render(<BetSlider min={100} max={1000} value={500} potSize={400} onChange={onChange} />)
    fireEvent.click(screen.getByRole('button', { name: /min/i }))
    expect(onChange).toHaveBeenCalledWith(100)
  })

  it('clicking All-In fires onChange with max value', () => {
    const onChange = vi.fn()
    render(<BetSlider min={100} max={1000} value={500} potSize={400} onChange={onChange} />)
    fireEvent.click(screen.getByRole('button', { name: /all-in/i }))
    expect(onChange).toHaveBeenCalledWith(1000)
  })

  it('range input has correct min/max/value', () => {
    const onChange = vi.fn()
    render(<BetSlider min={100} max={1000} value={500} potSize={400} onChange={onChange} />)
    const slider = screen.getByLabelText(/bet amount/i)
    expect(slider.getAttribute('min')).toBe('100')
    expect(slider.getAttribute('max')).toBe('1000')
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/game/__tests__/BetSlider.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/__tests__/BetSlider.test.tsx
git commit -m "test: add BetSlider component tests"
```

---

### Task 13: PasswordDialog Tests

**Files:**
- Create: `code/web/components/game/__tests__/PasswordDialog.test.tsx`
- Source: `code/web/components/game/PasswordDialog.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { PasswordDialog } from '../PasswordDialog'

describe('PasswordDialog', () => {
  it('renders game name in the dialog', () => {
    render(<PasswordDialog gameName="My Game" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    expect(screen.getByText(/My Game/)).toBeTruthy()
  })

  it('has role=dialog', () => {
    render(<PasswordDialog gameName="My Game" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    expect(screen.getByRole('dialog')).toBeTruthy()
  })

  it('submit button is disabled when password is empty', () => {
    render(<PasswordDialog gameName="My Game" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    const submitBtn = screen.getByRole('button', { name: /join/i })
    expect(submitBtn).toBeTruthy()
    expect((submitBtn as HTMLButtonElement).disabled).toBe(true)
  })

  it('submit button is enabled when password is entered', () => {
    render(<PasswordDialog gameName="My Game" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    const input = screen.getByLabelText(/password/i)
    fireEvent.change(input, { target: { value: 'secret123' } })
    expect((screen.getByRole('button', { name: /join/i }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('calls onSubmit with password on form submit', () => {
    const onSubmit = vi.fn()
    render(<PasswordDialog gameName="My Game" onSubmit={onSubmit} onCancel={vi.fn()} />)
    const input = screen.getByLabelText(/password/i)
    fireEvent.change(input, { target: { value: 'secret123' } })
    fireEvent.submit(input.closest('form')!)
    expect(onSubmit).toHaveBeenCalledWith('secret123')
  })

  it('calls onCancel on Escape key', () => {
    const onCancel = vi.fn()
    render(<PasswordDialog gameName="My Game" onSubmit={vi.fn()} onCancel={onCancel} />)
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(onCancel).toHaveBeenCalled()
  })

  it('calls onCancel on Cancel button click', () => {
    const onCancel = vi.fn()
    render(<PasswordDialog gameName="My Game" onSubmit={vi.fn()} onCancel={onCancel} />)
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }))
    expect(onCancel).toHaveBeenCalled()
  })

  it('shows error when error prop is provided', () => {
    render(<PasswordDialog gameName="My Game" onSubmit={vi.fn()} onCancel={vi.fn()} error="Wrong password" />)
    expect(screen.getByRole('alert').textContent).toBe('Wrong password')
  })

  it('does not show error when error prop is absent', () => {
    render(<PasswordDialog gameName="My Game" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/game/__tests__/PasswordDialog.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/__tests__/PasswordDialog.test.tsx
git commit -m "test: add PasswordDialog component tests"
```

---

### Task 14: GameOverlay Tests

**Files:**
- Create: `code/web/components/game/__tests__/GameOverlay.test.tsx`
- Source: `code/web/components/game/GameOverlay.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { GameOverlay } from '../GameOverlay'

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('GameOverlay', () => {
  // --- Paused ---

  it('paused: renders reason and pausedBy', () => {
    render(<GameOverlay type="paused" reason="Dinner break" pausedBy="HostPlayer" />)
    expect(screen.getByText('Dinner break')).toBeTruthy()
    expect(screen.getByText(/HostPlayer/)).toBeTruthy()
    expect(screen.getByRole('dialog', { name: 'Game Paused' })).toBeTruthy()
  })

  // --- Eliminated ---

  it('eliminated: renders finish position and onClose fires', () => {
    const onClose = vi.fn()
    render(<GameOverlay type="eliminated" finishPosition={3} onClose={onClose} />)
    expect(screen.getByText(/position 3/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /continue watching/i }))
    expect(onClose).toHaveBeenCalled()
  })

  // --- Tab replaced ---

  it('tab-replaced: renders message', () => {
    render(<GameOverlay type="tab-replaced" />)
    expect(screen.getByRole('dialog', { name: /another tab/i })).toBeTruthy()
  })

  // --- Continue runout ---

  it('continueRunout: onContinue fires on button click', () => {
    const onContinue = vi.fn()
    render(<GameOverlay type="continueRunout" onContinue={onContinue} />)
    fireEvent.click(screen.getByRole('button', { name: /continue/i }))
    expect(onContinue).toHaveBeenCalled()
  })

  // --- Rebuy ---

  it('rebuy: renders chip/cost info and countdown ticks', () => {
    const onDecision = vi.fn()
    render(<GameOverlay type="rebuy" cost={500} chips={1000} timeoutSeconds={10} onDecision={onDecision} />)
    expect(screen.getByText(/1,000/)).toBeTruthy()
    expect(screen.getByText(/500/)).toBeTruthy()
    expect(screen.getByText(/10s/)).toBeTruthy()

    act(() => { vi.advanceTimersByTime(3000) })
    expect(screen.getByText(/7s/)).toBeTruthy()
  })

  it('rebuy: accept calls onDecision(true)', () => {
    const onDecision = vi.fn()
    render(<GameOverlay type="rebuy" cost={500} chips={1000} timeoutSeconds={10} onDecision={onDecision} />)
    fireEvent.click(screen.getByRole('button', { name: /accept/i }))
    expect(onDecision).toHaveBeenCalledWith(true)
  })

  it('rebuy: decline calls onDecision(false)', () => {
    const onDecision = vi.fn()
    render(<GameOverlay type="rebuy" cost={500} chips={1000} timeoutSeconds={10} onDecision={onDecision} />)
    fireEvent.click(screen.getByRole('button', { name: /decline/i }))
    expect(onDecision).toHaveBeenCalledWith(false)
  })

  // --- Addon ---

  it('addon: renders and accept/decline work', () => {
    const onDecision = vi.fn()
    render(<GameOverlay type="addon" cost={300} chips={2000} timeoutSeconds={15} onDecision={onDecision} />)
    expect(screen.getByRole('dialog', { name: /add-on available/i })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /accept/i }))
    expect(onDecision).toHaveBeenCalledWith(true)
  })

  // --- NeverBroke ---

  it('neverBroke: countdown and decision callbacks', () => {
    const onDecision = vi.fn()
    render(<GameOverlay type="neverBroke" timeoutSeconds={20} onDecision={onDecision} />)
    expect(screen.getByText(/20s/)).toBeTruthy()

    act(() => { vi.advanceTimersByTime(5000) })
    expect(screen.getByText(/15s/)).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: /decline/i }))
    expect(onDecision).toHaveBeenCalledWith(false)
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/game/__tests__/GameOverlay.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/__tests__/GameOverlay.test.tsx
git commit -m "test: add GameOverlay component tests"
```

---

### Task 15: Pagination Tests

**Files:**
- Create: `code/web/components/data/__tests__/Pagination.test.tsx`
- Source: `code/web/components/data/Pagination.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Pagination } from '../Pagination'

// --- Mocks ---

const mockPush = vi.fn()
const mockSearchParams = new URLSearchParams()

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useSearchParams: () => mockSearchParams,
}))

describe('Pagination', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('returns null when total pages is 1 or less', () => {
    const { container } = render(
      <Pagination currentPage={1} totalItems={5} itemsPerPage={10} baseUrl="/test" />
    )
    expect(container.innerHTML).toBe('')
  })

  it('renders Previous and Next buttons', () => {
    render(<Pagination currentPage={1} totalItems={30} itemsPerPage={10} baseUrl="/test" />)
    expect(screen.getByRole('button', { name: /previous/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /next/i })).toBeTruthy()
  })

  it('Previous is disabled on page 1', () => {
    render(<Pagination currentPage={1} totalItems={30} itemsPerPage={10} baseUrl="/test" />)
    expect((screen.getByRole('button', { name: /previous/i }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('Next is disabled on last page', () => {
    render(<Pagination currentPage={3} totalItems={30} itemsPerPage={10} baseUrl="/test" />)
    expect((screen.getByRole('button', { name: /next/i }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('clicking a page number calls router.push with correct URL', () => {
    render(<Pagination currentPage={1} totalItems={30} itemsPerPage={10} baseUrl="/test" />)
    fireEvent.click(screen.getByRole('button', { name: '2' }))
    expect(mockPush).toHaveBeenCalledWith('/test?page=2')
  })

  it('clicking Next advances to the next page', () => {
    render(<Pagination currentPage={1} totalItems={30} itemsPerPage={10} baseUrl="/test" />)
    fireEvent.click(screen.getByRole('button', { name: /next/i }))
    expect(mockPush).toHaveBeenCalledWith('/test?page=2')
  })

  it('renders ellipsis for large page counts', () => {
    render(<Pagination currentPage={5} totalItems={200} itemsPerPage={10} baseUrl="/test" />)
    const ellipses = screen.getAllByText('...')
    expect(ellipses.length).toBeGreaterThan(0)
  })

  it('shows page info text', () => {
    render(<Pagination currentPage={2} totalItems={30} itemsPerPage={10} baseUrl="/test" />)
    expect(screen.getByText(/Page 2 of 3/)).toBeTruthy()
    expect(screen.getByText(/30 total items/)).toBeTruthy()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/data/__tests__/Pagination.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/data/__tests__/Pagination.test.tsx
git commit -m "test: add Pagination component tests"
```

---

### Task 16: LoginForm Tests

**Files:**
- Create: `code/web/components/auth/__tests__/LoginForm.test.tsx`
- Source: `code/web/components/auth/LoginForm.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent, act, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { LoginForm } from '../LoginForm'

// --- Mocks ---

const mockPush = vi.fn()
const mockSearchParams = new URLSearchParams()

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useSearchParams: () => mockSearchParams,
}))

vi.mock('next/link', () => ({
  default: ({ children, href }: { children: React.ReactNode; href: string }) => (
    <a href={href}>{children}</a>
  ),
}))

const mockLogin = vi.fn()
const mockClearError = vi.fn()

vi.mock('@/lib/auth/useAuth', () => ({
  useAuth: () => ({
    login: mockLogin,
    error: mockError,
    isLoading: mockIsLoading,
    clearError: mockClearError,
  }),
}))

let mockError: string | null = null
let mockIsLoading = false

beforeEach(() => {
  vi.clearAllMocks()
  mockError = null
  mockIsLoading = false
})

describe('LoginForm', () => {
  it('renders username, password, and remember me fields', () => {
    render(<LoginForm />)
    expect(screen.getByLabelText(/username/i)).toBeTruthy()
    expect(screen.getByLabelText(/password/i)).toBeTruthy()
    expect(screen.getByLabelText(/remember me/i)).toBeTruthy()
  })

  it('renders submit button', () => {
    render(<LoginForm />)
    expect(screen.getByRole('button', { name: /log in/i })).toBeTruthy()
  })

  it('disables inputs when isLoading is true', () => {
    mockIsLoading = true
    render(<LoginForm />)
    expect((screen.getByLabelText(/username/i) as HTMLInputElement).disabled).toBe(true)
    expect((screen.getByLabelText(/password/i) as HTMLInputElement).disabled).toBe(true)
    expect((screen.getByRole('button', { name: /logging in/i }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('shows error when error is set', () => {
    mockError = 'Invalid credentials'
    render(<LoginForm />)
    expect(screen.getByRole('alert').textContent).toBe('Invalid credentials')
  })

  it('redirects to /online on successful login without returnUrl', async () => {
    mockLogin.mockResolvedValue(true)
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'alice' } })
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'pass123' } })

    await act(async () => {
      fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)
    })

    expect(mockPush).toHaveBeenCalledWith('/online')
  })

  it('redirects to returnUrl on successful login', async () => {
    mockLogin.mockResolvedValue(true)
    mockSearchParams.set('returnUrl', '/games/123')
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'alice' } })
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'pass123' } })

    await act(async () => {
      fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)
    })

    expect(mockPush).toHaveBeenCalledWith('/games/123')
    mockSearchParams.delete('returnUrl')
  })

  it('does not redirect on failed login', async () => {
    mockLogin.mockResolvedValue(false)
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'alice' } })
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'wrong' } })

    await act(async () => {
      fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)
    })

    expect(mockPush).not.toHaveBeenCalled()
  })

  it('rejects open redirect attempts', async () => {
    mockLogin.mockResolvedValue(true)
    mockSearchParams.set('returnUrl', '//evil.com')
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'alice' } })
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'pass123' } })

    await act(async () => {
      fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)
    })

    expect(mockPush).toHaveBeenCalledWith('/online')
    mockSearchParams.delete('returnUrl')
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/auth/__tests__/LoginForm.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/auth/__tests__/LoginForm.test.tsx
git commit -m "test: add LoginForm component tests"
```

---

### Task 17: CurrentProfile Tests

**Files:**
- Create: `code/web/components/auth/__tests__/CurrentProfile.test.tsx`
- Source: `code/web/components/auth/CurrentProfile.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { CurrentProfile } from '../CurrentProfile'

// --- Mocks ---

const mockPush = vi.fn()
const mockLogout = vi.fn().mockResolvedValue(undefined)

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

let mockUser: { username: string; isAdmin: boolean } | null = null
let mockIsAuthenticated = false

vi.mock('@/lib/auth/useAuth', () => ({
  useAuth: () => ({
    user: mockUser,
    isAuthenticated: mockIsAuthenticated,
    logout: mockLogout,
  }),
}))

beforeEach(() => {
  vi.clearAllMocks()
  mockUser = null
  mockIsAuthenticated = false
})

describe('CurrentProfile', () => {
  it('returns null when not authenticated', () => {
    const { container } = render(<CurrentProfile />)
    expect(container.innerHTML).toBe('')
  })

  it('renders username when authenticated', () => {
    mockUser = { username: 'alice', isAdmin: false }
    mockIsAuthenticated = true
    render(<CurrentProfile />)
    expect(screen.getByText('alice')).toBeTruthy()
  })

  it('shows Admin badge when user is admin', () => {
    mockUser = { username: 'admin', isAdmin: true }
    mockIsAuthenticated = true
    render(<CurrentProfile />)
    expect(screen.getByText('Admin')).toBeTruthy()
  })

  it('does not show Admin badge when user is not admin', () => {
    mockUser = { username: 'alice', isAdmin: false }
    mockIsAuthenticated = true
    render(<CurrentProfile />)
    expect(screen.queryByText('Admin')).toBeNull()
  })

  it('calls logout and navigates to / on logout click', async () => {
    mockUser = { username: 'alice', isAdmin: false }
    mockIsAuthenticated = true
    render(<CurrentProfile />)

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /logout/i }))
    })

    expect(mockLogout).toHaveBeenCalled()
    expect(mockPush).toHaveBeenCalledWith('/')
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/auth/__tests__/CurrentProfile.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/auth/__tests__/CurrentProfile.test.tsx
git commit -m "test: add CurrentProfile component tests"
```

---

### Task 18: AliasManagement Tests

**Files:**
- Create: `code/web/components/profile/__tests__/AliasManagement.test.tsx`
- Source: `code/web/components/profile/AliasManagement.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AliasManagement } from '../AliasManagement'

// Mock Dialog to simplify testing the confirmation flow
vi.mock('@/components/ui/Dialog', () => ({
  Dialog: ({ isOpen, onClose, onConfirm, title, message }: {
    isOpen: boolean; onClose: () => void; onConfirm: () => void; title: string; message: string
  }) => isOpen ? (
    <div data-testid="dialog" role="dialog">
      <p>{title}</p>
      <p>{message}</p>
      <button onClick={onConfirm}>Confirm</button>
      <button onClick={onClose}>Cancel</button>
    </div>
  ) : null,
}))

const activeAliases = [
  { name: 'Player1', createdDate: '2025-01-15' },
  { name: 'Player2', createdDate: '2025-03-20' },
]

const retiredAliases = [
  { name: 'OldName', createdDate: '2024-01-01', retiredDate: '2025-06-01' },
]

describe('AliasManagement', () => {
  it('renders active aliases', () => {
    render(<AliasManagement aliases={activeAliases} />)
    expect(screen.getByText('Player1')).toBeTruthy()
    expect(screen.getByText('Player2')).toBeTruthy()
  })

  it('renders retired aliases in separate section', () => {
    render(<AliasManagement aliases={[...activeAliases, ...retiredAliases]} />)
    expect(screen.getByText('Active Aliases')).toBeTruthy()
    expect(screen.getByText('Retired Aliases')).toBeTruthy()
    expect(screen.getByText('OldName')).toBeTruthy()
  })

  it('shows "No active aliases" when none exist', () => {
    render(<AliasManagement aliases={retiredAliases} />)
    expect(screen.getByText('No active aliases')).toBeTruthy()
  })

  it('opens dialog when Retire is clicked', () => {
    render(<AliasManagement aliases={activeAliases} />)
    const retireButtons = screen.getAllByRole('button', { name: /retire/i })
    fireEvent.click(retireButtons[0])
    expect(screen.getByTestId('dialog')).toBeTruthy()
    expect(screen.getByText(/Player1/)).toBeTruthy()
  })

  it('closes dialog on Cancel', () => {
    render(<AliasManagement aliases={activeAliases} />)
    fireEvent.click(screen.getAllByRole('button', { name: /retire/i })[0])
    expect(screen.getByTestId('dialog')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }))
    expect(screen.queryByTestId('dialog')).toBeNull()
  })

  it('shows success message on confirm', async () => {
    render(<AliasManagement aliases={activeAliases} />)
    fireEvent.click(screen.getAllByRole('button', { name: /retire/i })[0])
    fireEvent.click(screen.getByRole('button', { name: /confirm/i }))
    expect(await screen.findByText(/retired successfully/)).toBeTruthy()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/profile/__tests__/AliasManagement.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/profile/__tests__/AliasManagement.test.tsx
git commit -m "test: add AliasManagement component tests"
```

---

### Task 19: PasswordChangeForm Tests

**Files:**
- Create: `code/web/components/profile/__tests__/PasswordChangeForm.test.tsx`
- Source: `code/web/components/profile/PasswordChangeForm.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { PasswordChangeForm } from '../PasswordChangeForm'

// --- Mocks ---

vi.mock('@/lib/api', () => ({
  playerApi: {
    changePassword: vi.fn(),
  },
}))

import { playerApi } from '@/lib/api'
const mockChangePassword = vi.mocked(playerApi.changePassword)

beforeEach(() => {
  vi.clearAllMocks()
})

function fillForm(current: string, newPw: string, confirm: string) {
  fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: current } })
  fireEvent.change(screen.getByLabelText(/new password/i), { target: { value: newPw } })
  fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: confirm } })
}

describe('PasswordChangeForm', () => {
  it('renders three password fields', () => {
    render(<PasswordChangeForm />)
    expect(screen.getByLabelText(/current password/i)).toBeTruthy()
    expect(screen.getByLabelText(/new password/i)).toBeTruthy()
    expect(screen.getByLabelText(/confirm new password/i)).toBeTruthy()
  })

  it('shows error when passwords do not match', async () => {
    render(<PasswordChangeForm />)
    fillForm('old123', 'newpass88', 'different')

    await act(async () => {
      fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)
    })

    expect(screen.getByRole('alert').textContent).toBe('New passwords do not match')
    expect(mockChangePassword).not.toHaveBeenCalled()
  })

  it('shows error when password is too short', async () => {
    render(<PasswordChangeForm />)
    fillForm('old123', 'short', 'short')

    await act(async () => {
      fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)
    })

    expect(screen.getByRole('alert').textContent).toBe('Password must be at least 8 characters')
    expect(mockChangePassword).not.toHaveBeenCalled()
  })

  it('calls API and shows success on valid submit', async () => {
    mockChangePassword.mockResolvedValue(undefined)
    render(<PasswordChangeForm />)
    fillForm('old123', 'newpass88', 'newpass88')

    await act(async () => {
      fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)
    })

    expect(mockChangePassword).toHaveBeenCalledWith('old123', 'newpass88')
    expect(screen.getByRole('status').textContent).toBe('Password changed successfully')
    // Fields should be cleared
    expect((screen.getByLabelText(/current password/i) as HTMLInputElement).value).toBe('')
  })

  it('shows API error on failure', async () => {
    mockChangePassword.mockRejectedValue(new Error('Invalid current password'))
    render(<PasswordChangeForm />)
    fillForm('wrong', 'newpass88', 'newpass88')

    await act(async () => {
      fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)
    })

    expect(screen.getByRole('alert').textContent).toBe('Invalid current password')
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/profile/__tests__/PasswordChangeForm.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/profile/__tests__/PasswordChangeForm.test.tsx
git commit -m "test: add PasswordChangeForm component tests"
```

---

### Task 20: Sidebar Tests

**Files:**
- Create: `code/web/components/layout/__tests__/Sidebar.test.tsx`
- Source: `code/web/components/layout/Sidebar.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import Sidebar from '../Sidebar'

// --- Mocks ---

let mockPathname = '/online'

vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
}))

vi.mock('next/link', () => ({
  default: ({ children, href, onClick, className }: {
    children: React.ReactNode; href: string; onClick?: () => void; className?: string
  }) => (
    <a href={href} onClick={onClick} className={className}>{children}</a>
  ),
}))

beforeEach(() => {
  mockPathname = '/online'
})

const sections = [
  {
    title: 'Games',
    items: [
      { title: 'Game Lobby', link: '/games', icon: 'X', exactMatch: true },
      { title: 'Hosts', link: '/online/hosts', icon: 'Y' },
    ],
  },
  {
    items: [
      { title: 'Leaderboard', link: '/online/leaderboard' },
    ],
  },
]

describe('Sidebar', () => {
  it('renders section titles and item links', () => {
    render(<Sidebar sections={sections} />)
    expect(screen.getByText('Games')).toBeTruthy()
    expect(screen.getByText('Game Lobby')).toBeTruthy()
    expect(screen.getByText('Hosts')).toBeTruthy()
    expect(screen.getByText('Leaderboard')).toBeTruthy()
  })

  it('marks items as active when pathname starts with link', () => {
    mockPathname = '/online/hosts/123'
    render(<Sidebar sections={sections} />)
    const hostsLink = screen.getByText('Hosts').closest('a')!
    expect(hostsLink.className).toContain('active')
  })

  it('exactMatch items only active on exact path', () => {
    mockPathname = '/games/123'
    render(<Sidebar sections={sections} />)
    const lobbyLink = screen.getByText('Game Lobby').closest('a')!
    expect(lobbyLink.className).not.toContain('active')
  })

  it('exactMatch item is active on exact match', () => {
    mockPathname = '/games'
    render(<Sidebar sections={sections} />)
    const lobbyLink = screen.getByText('Game Lobby').closest('a')!
    expect(lobbyLink.className).toContain('active')
  })

  it('hamburger toggles mobile sidebar open/close', () => {
    render(<Sidebar sections={sections} />)
    const hamburger = screen.getByLabelText('Toggle sidebar menu')
    expect(hamburger.getAttribute('aria-expanded')).toBe('false')

    fireEvent.click(hamburger)
    expect(hamburger.getAttribute('aria-expanded')).toBe('true')

    fireEvent.click(hamburger)
    expect(hamburger.getAttribute('aria-expanded')).toBe('false')
  })

  it('Escape key closes mobile sidebar', () => {
    render(<Sidebar sections={sections} />)
    const hamburger = screen.getByLabelText('Toggle sidebar menu')
    fireEvent.click(hamburger)
    expect(hamburger.getAttribute('aria-expanded')).toBe('true')

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(hamburger.getAttribute('aria-expanded')).toBe('false')
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/layout/__tests__/Sidebar.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/layout/__tests__/Sidebar.test.tsx
git commit -m "test: add Sidebar component tests"
```

---

### Task 21: Navigation Tests

**Files:**
- Create: `code/web/components/layout/__tests__/Navigation.test.tsx`
- Source: `code/web/components/layout/Navigation.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import Navigation from '../Navigation'

// --- Mocks ---

let mockPathname = '/'
let mockUser: { username: string; isAdmin: boolean } | null = null
let mockIsAuthenticated = false

vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
}))

vi.mock('next/link', () => ({
  default: ({ children, href, className }: {
    children: React.ReactNode; href: string; className?: string
  }) => (
    <a href={href} className={className}>{children}</a>
  ),
}))

vi.mock('@/lib/auth/useAuth', () => ({
  useAuth: () => ({
    user: mockUser,
    isAuthenticated: mockIsAuthenticated,
  }),
}))

vi.mock('@/components/auth/CurrentProfile', () => ({
  CurrentProfile: () => <div data-testid="current-profile">Profile</div>,
}))

beforeEach(() => {
  mockPathname = '/'
  mockUser = null
  mockIsAuthenticated = false
})

describe('Navigation', () => {
  it('renders nav items', () => {
    render(<Navigation />)
    expect(screen.getByText('Home')).toBeTruthy()
    expect(screen.getByText('About')).toBeTruthy()
    expect(screen.getByText('Download')).toBeTruthy()
  })

  it('hides admin nav items for non-admin users', () => {
    mockUser = { username: 'alice', isAdmin: false }
    mockIsAuthenticated = true
    render(<Navigation />)
    // Admin item should not be visible in desktop nav
    const adminLinks = screen.queryAllByText('Admin')
    // May be zero (desktop hidden) or some that are in mobile menu but still hidden
    const visibleAdmin = adminLinks.filter(el => {
      // Check if the element or its parents have admin-related content
      return el.closest('[class*="admin"]') || el.textContent === 'Admin'
    })
    // The admin dropdown trigger should not exist since user is not admin
    // Using a broader check: admin items should be filtered out
    expect(screen.queryByText('Ban List')).toBeNull()
  })

  it('shows admin nav items for admin users', () => {
    mockUser = { username: 'admin', isAdmin: true }
    mockIsAuthenticated = true
    render(<Navigation />)
    // Admin item should be in the nav
    expect(screen.getAllByText('Admin').length).toBeGreaterThan(0)
  })

  it('shows login link when not authenticated', () => {
    render(<Navigation />)
    expect(screen.getByText(/log in/i)).toBeTruthy()
  })

  it('shows CurrentProfile when authenticated', () => {
    mockUser = { username: 'alice', isAdmin: false }
    mockIsAuthenticated = true
    render(<Navigation />)
    expect(screen.getByTestId('current-profile')).toBeTruthy()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/layout/__tests__/Navigation.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/layout/__tests__/Navigation.test.tsx
git commit -m "test: add Navigation component tests"
```

---

### Task 22: SidebarLayout Tests

**Files:**
- Create: `code/web/components/layout/__tests__/SidebarLayout.test.tsx`
- Source: `code/web/components/layout/SidebarLayout.tsx`

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import SidebarLayout from '../SidebarLayout'

vi.mock('@/components/layout/Sidebar', () => ({
  default: ({ sections }: { sections: unknown[] }) => (
    <div data-testid="sidebar">Sidebar ({sections.length} sections)</div>
  ),
}))

describe('SidebarLayout', () => {
  const sections = [
    { title: 'Test', items: [{ title: 'Item', link: '/test' }] },
  ]

  it('renders Sidebar with sections', () => {
    render(<SidebarLayout sections={sections}><p>Content</p></SidebarLayout>)
    expect(screen.getByTestId('sidebar')).toBeTruthy()
    expect(screen.getByText(/1 sections/)).toBeTruthy()
  })

  it('renders children', () => {
    render(<SidebarLayout sections={sections}><p>Page Content</p></SidebarLayout>)
    expect(screen.getByText('Page Content')).toBeTruthy()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/layout/__tests__/SidebarLayout.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/layout/__tests__/SidebarLayout.test.tsx
git commit -m "test: add SidebarLayout component tests"
```

---

### Task 23: PokerTable Tests

**Files:**
- Create: `code/web/components/game/__tests__/PokerTable.test.tsx`
- Source: `code/web/components/game/PokerTable.tsx`

This is the most complex test file — mocks all 8 hooks and all child components.

**Step 1: Write tests**

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { PokerTable } from '../PokerTable'

// --- Mock all child components ---

vi.mock('../TournamentInfoBar', () => ({
  TournamentInfoBar: () => <div data-testid="tournament-info-bar" />,
}))
vi.mock('../TableFelt', () => ({
  TableFelt: () => <div data-testid="table-felt" />,
}))
vi.mock('../PlayerSeat', () => ({
  PlayerSeat: ({ seat }: { seat: { playerName: string } }) => (
    <div data-testid="player-seat">{seat.playerName}</div>
  ),
}))
vi.mock('../ActionPanel', () => ({
  ActionPanel: () => <div data-testid="action-panel" />,
}))
vi.mock('../ActionTimer', () => ({
  ActionTimer: () => <div data-testid="action-timer" />,
}))
vi.mock('../HandHistory', () => ({
  HandHistory: () => <div data-testid="hand-history" />,
}))
vi.mock('../ChatPanel', () => ({
  ChatPanel: () => <div data-testid="chat-panel" />,
}))
vi.mock('../ObserverPanel', () => ({
  ObserverPanel: () => <div data-testid="observer-panel" />,
}))
vi.mock('../ChipLeaderMini', () => ({
  ChipLeaderMini: () => <div data-testid="chip-leader" />,
}))
vi.mock('../VolumeControl', () => ({
  VolumeControl: () => <div data-testid="volume-control" />,
}))
vi.mock('../ThemePicker', () => ({
  ThemePicker: () => <div data-testid="theme-picker" />,
}))
vi.mock('@/components/ui/Dialog', () => ({
  Dialog: () => <div data-testid="dialog" />,
}))
vi.mock('../HandRankings', () => ({
  HandRankings: () => <div data-testid="hand-rankings" />,
}))
vi.mock('../HandReplay', () => ({
  HandReplay: () => <div data-testid="hand-replay" />,
}))
vi.mock('../Simulator', () => ({
  Simulator: () => <div data-testid="simulator" />,
}))
vi.mock('../AdvisorPanel', () => ({
  AdvisorPanel: () => <div data-testid="advisor-panel" />,
}))
vi.mock('../Dashboard', () => ({
  Dashboard: () => <div data-testid="dashboard" />,
}))

// --- Mock all hooks ---

const mockSendAction = vi.fn()
const mockSendChat = vi.fn()
const mockSendContinueRunout = vi.fn()
const mockSendAdminResume = vi.fn()
const mockSendAdminPause = vi.fn()
const mockSendAdminKick = vi.fn()
const mockSendSitOut = vi.fn()
const mockSendComeBack = vi.fn()

const defaultActions = {
  sendAction: mockSendAction,
  sendChat: mockSendChat,
  sendContinueRunout: mockSendContinueRunout,
  sendAdminResume: mockSendAdminResume,
  sendAdminPause: mockSendAdminPause,
  sendAdminKick: mockSendAdminKick,
  sendSitOut: mockSendSitOut,
  sendComeBack: mockSendComeBack,
}

function makeSeat(overrides = {}) {
  return {
    seatIndex: 0, playerId: 1, playerName: 'Alice', chipCount: 5000,
    status: 'ACTIVE', isDealer: true, isSmallBlind: false, isBigBlind: false,
    currentBet: 0, holeCards: [], isCurrentActor: false,
    ...overrides,
  }
}

function makeState(overrides = {}) {
  return {
    currentTable: {
      tableId: 1,
      seats: [makeSeat(), makeSeat({ seatIndex: 1, playerId: 2, playerName: 'Bob' })],
      communityCards: [],
      pots: [{ amount: 100, eligiblePlayers: [1, 2] }],
      currentRound: 'PREFLOP',
      handNumber: 1,
    },
    holeCards: ['Ah', 'Kd'],
    actionRequired: null,
    actionTimeoutSeconds: null,
    actionTimer: null,
    myPlayerId: 1,
    gameState: {
      status: 'RUNNING',
      level: 1,
      blinds: { small: 25, big: 50, ante: 0 },
      nextLevelIn: 300,
      players: [],
      totalPlayers: 9,
      playersRemaining: 6,
      numTables: 1,
      playerRank: 3,
    },
    chatMessages: [],
    continueRunoutPending: false,
    handHistory: [],
    observers: [],
    isOwner: false,
    playersRemaining: 6,
    totalPlayers: 9,
    playerRank: 3,
    advisorData: null,
    ...overrides,
  }
}

let mockState = makeState()

vi.mock('@/lib/game/hooks', () => ({
  useGameState: () => mockState,
  useGameActions: () => defaultActions,
}))

vi.mock('@/lib/game/useMutedPlayers', () => ({
  useMutedPlayers: () => ({ mutedIds: new Set(), mute: vi.fn(), unmute: vi.fn() }),
}))

vi.mock('@/lib/theme/useTheme', () => ({
  useTheme: () => ({ colors: { center: '#000', mid: '#111', edge: '#222', border: '#333' } }),
}))

vi.mock('@/lib/theme/useCardBack', () => ({
  useCardBack: () => ({ cardBackId: 'classic' }),
}))

vi.mock('@/lib/theme/useAvatar', () => ({
  useAvatar: () => ({ avatarId: 'default' }),
}))

vi.mock('@/lib/game/useGamePrefs', () => ({
  useGamePrefs: () => ({
    prefs: { fourColorDeck: false, disableShortcuts: false, checkFold: false, dealerChat: true },
  }),
}))

vi.mock('@/lib/audio/useSoundEffects', () => ({
  useSoundEffects: vi.fn(),
}))

beforeEach(() => {
  vi.clearAllMocks()
  mockState = makeState()
})

describe('PokerTable', () => {
  // --- Loading state ---

  it('shows connecting message when currentTable is null', () => {
    mockState = makeState({ currentTable: null })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByText('Connecting to game...')).toBeTruthy()
    expect(screen.getByRole('status', { name: 'Connecting to game' })).toBeTruthy()
  })

  it('shows connecting message when gameState is null', () => {
    mockState = makeState({ gameState: null })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByText('Connecting to game...')).toBeTruthy()
  })

  // --- Core rendering ---

  it('renders player seats for each player', () => {
    render(<PokerTable gameName="Test Game" />)
    const seats = screen.getAllByTestId('player-seat')
    expect(seats).toHaveLength(2)
    expect(screen.getByText('Alice')).toBeTruthy()
    expect(screen.getByText('Bob')).toBeTruthy()
  })

  it('renders table felt', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('table-felt')).toBeTruthy()
  })

  it('renders tournament info bar', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('tournament-info-bar')).toBeTruthy()
  })

  it('renders chat panel', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('chat-panel')).toBeTruthy()
  })

  it('renders hand history', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('hand-history')).toBeTruthy()
  })

  // --- Action panel ---

  it('shows action panel when actionRequired is set', () => {
    mockState = makeState({
      actionRequired: {
        canFold: true, canCheck: false, canCall: true, callAmount: 50,
        canBet: false, minBet: 0, maxBet: 0,
        canRaise: false, minRaise: 0, maxRaise: 0,
        canAllIn: true, allInAmount: 5000,
      },
    })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('action-panel')).toBeTruthy()
  })

  it('hides action panel when actionRequired is null', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('action-panel')).toBeNull()
  })

  // --- Action timer ---

  it('shows action timer when actionTimeoutSeconds is set', () => {
    mockState = makeState({ actionTimeoutSeconds: 30 })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByTestId('action-timer')).toBeTruthy()
  })

  it('hides action timer when actionTimeoutSeconds is null', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('action-timer')).toBeNull()
  })

  // --- Admin controls ---

  it('shows Pause button for owner when game is running', () => {
    mockState = makeState({ isOwner: true })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByRole('button', { name: /pause game/i })).toBeTruthy()
  })

  it('shows Resume button for owner when game is paused', () => {
    mockState = makeState({
      isOwner: true,
      gameState: { ...makeState().gameState, status: 'PAUSED' },
    })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByRole('button', { name: /resume game/i })).toBeTruthy()
  })

  it('hides admin controls for non-owner', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByRole('button', { name: /pause game/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /resume game/i })).toBeNull()
  })

  // --- Paused banner ---

  it('shows paused banner when game status is PAUSED', () => {
    mockState = makeState({
      gameState: { ...makeState().gameState, status: 'PAUSED' },
    })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByText('Game Paused')).toBeTruthy()
  })

  // --- Keyboard shortcuts ---

  it('H key toggles hand rankings', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('hand-rankings')).toBeNull()

    fireEvent.keyDown(window, { key: 'h' })
    expect(screen.getByTestId('hand-rankings')).toBeTruthy()

    fireEvent.keyDown(window, { key: 'h' })
    expect(screen.queryByTestId('hand-rankings')).toBeNull()
  })

  it('D key toggles dashboard', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.queryByTestId('dashboard')).toBeNull()

    fireEvent.keyDown(window, { key: 'd' })
    expect(screen.getByTestId('dashboard')).toBeTruthy()
  })

  // --- Overlay ---

  it('renders overlay prop when provided', () => {
    render(<PokerTable gameName="Test Game" overlay={<div data-testid="test-overlay">Overlay</div>} />)
    expect(screen.getByTestId('test-overlay')).toBeTruthy()
  })

  // --- Sit out ---

  it('shows Sit Out button when player has a seat', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByRole('button', { name: /sit out/i })).toBeTruthy()
  })

  it('shows I\'m Back button when player is sat out', () => {
    mockState = makeState({
      currentTable: {
        ...makeState().currentTable,
        seats: [makeSeat({ status: 'SAT_OUT' }), makeSeat({ seatIndex: 1, playerId: 2, playerName: 'Bob' })],
      },
    })
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByRole('button', { name: /i'm back/i })).toBeTruthy()
  })

  // --- Toolbar buttons ---

  it('has simulator, advisor, and dashboard toolbar buttons', () => {
    render(<PokerTable gameName="Test Game" />)
    expect(screen.getByLabelText('Open simulator')).toBeTruthy()
    expect(screen.getByLabelText('Toggle advisor')).toBeTruthy()
    expect(screen.getByLabelText('Toggle dashboard')).toBeTruthy()
  })
})
```

**Step 2: Run test**

```bash
cd code/web && npx vitest run components/game/__tests__/PokerTable.test.tsx
```
Expected: PASS

**Step 3: Commit**

```bash
git add code/web/components/game/__tests__/PokerTable.test.tsx
git commit -m "test: add PokerTable component tests"
```

---

### Task 24: Coverage Threshold + Final Verification

**Files:**
- Modify: `code/web/vitest.config.ts`

**Step 1: Run full test suite to verify all pass**

```bash
cd code/web && npx vitest run
```
Expected: all tests PASS (existing 38 + new 23 = 61 test files)

**Step 2: Run coverage and check current numbers**

```bash
cd code/web && npx vitest run --coverage
```
Expected: review coverage output to confirm 80%+ is achievable

**Step 3: Add coverage thresholds to vitest.config.ts**

In `code/web/vitest.config.ts`, add `thresholds` to the existing `coverage` section:

```ts
coverage: {
  provider: 'v8',
  reporter: ['text', 'json', 'html'],
  exclude: ['node_modules/', '.next/', 'coverage/', '*.config.ts'],
  thresholds: {
    lines: 80,
    branches: 80,
    functions: 80,
    statements: 80,
  },
},
```

**Step 4: Run coverage with thresholds to verify**

```bash
cd code/web && npx vitest run --coverage
```
Expected: PASS — thresholds met

**Step 5: Commit**

```bash
git add code/web/vitest.config.ts
git commit -m "chore: enforce 80% coverage thresholds in vitest.config.ts"
```

**Step 6: Run full suite one final time**

```bash
cd code/web && npm test
```
Expected: all PASS
