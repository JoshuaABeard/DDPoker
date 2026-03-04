# Web Client Unit Test Coverage Design

**Date:** 2026-03-01
**Status:** COMPLETED (2026-03-04)
**Goal:** Comprehensive unit test coverage for all untested web client source files, with 80% coverage threshold enforced

## Overview

Bottom-up test coverage sweep for the Next.js/React/TypeScript web client (`code/web/`).
Tests 27 untested source files across 6 sections, ordered by dependency layer so each
layer validates mocking assumptions for the next. Enforces 80% line/branch/function/statement
thresholds in `vitest.config.ts` at completion.

**Testing stack:** Vitest 3 + React Testing Library + jsdom (already configured)

## Current State

| Area | Files Tested | Files Untested | Notes |
|---|---|---|---|
| `lib/game/` | 6/8 | GameContext, hooks | Best covered |
| `lib/audio/` | 4/4 | — | Complete |
| `lib/theme/` | 3/3 | — | Complete |
| `lib/auth/` | 0/4 | All | Zero coverage |
| `lib/` root | 3/8 | config, navData, pagination, sidebarData, types | |
| `components/game/` | 19/30 | 11 files | Core gameplay UI gaps |
| `components/auth/` | 0/2 | All | Login/profile |
| `components/layout/` | 0/4 | All | Nav/sidebar/footer |
| `components/data/` | 0/2 | All | DataTable/Pagination |
| `components/profile/` | 1/3 | 2 files | AliasManagement, PasswordChangeForm |
| `middleware.ts` | 1/1 | — | Complete |

## Design Decisions

- **Bottom-up ordering.** Test pure utilities and storage first, then hooks/context,
  then consumer components. Each layer's tests validate mock patterns for the next.
- **Match existing conventions.** Follow established patterns: explicit Vitest imports,
  `vi.mock()` for modules, `vi.fn()` for callbacks, builder functions with `Partial<T>`
  overrides for test data, `getByRole` with regex names preferred.
- **Mock child components in integration-heavy tests.** PokerTable mocks all children
  to `() => <div data-testid="..." />` to isolate its orchestration logic.
- **Fake timers for time-dependent components.** ActionTimer, GameOverlay countdown
  variants use `vi.useFakeTimers()` / `vi.advanceTimersByTime()`.

## Section 1: Auth Module (`lib/auth/`, 4 test files)

### `storage.test.ts`
- `getAuthUser`: localStorage hit, sessionStorage hit, both empty → null, corrupt JSON self-heals
- `setAuthUser`: `rememberMe=true` → localStorage + clear sessionStorage; `false` → opposite
- `clearAuthUser`: removes key from both storages
- SSR guard: returns null when `window` is undefined

### `useAuth.test.ts`
- Throws when used outside `AuthProvider`
- Returns full context value when inside `AuthProvider`

### `AuthContext.test.tsx` (mocks `authApi` + `./storage`)
- Mount with stored user → calls `getCurrentUser()`, sets authenticated + admin status
- Mount with no stored user → stays unauthenticated
- Mount with failed API (expired session) → clears storage, unauthenticated
- `login()` success: calls API, stores user, updates state, returns true
- `login()` failure: sets error, returns false
- `logout()`: calls API, clears storage, resets state
- `clearError()`: nulls error

### `useRequireAuth.test.ts` (mocks `useAuth` + `next/navigation`)
- Loading → no redirect
- Not authenticated → redirects to `/login?returnUrl=<current path>`
- Authenticated, no admin required → no redirect
- Authenticated, admin required but not admin → redirects to `/`
- Authenticated, admin required and is admin → no redirect

## Section 2: Trivial/Presentational Components (6 test files)

No hooks or context — pure prop-driven rendering.

### `DealerButton.test.tsx`
- Renders "D"/"SB"/"BB" labels per `type` prop
- Correct aria-labels for accessibility

### `Footer.test.tsx`
- Copyright text present
- External link renders with correct href

### `DashboardWidget.test.tsx`
- Children visible when expanded (default)
- Toggle button hides/shows children
- `defaultExpanded={false}` starts collapsed
- `aria-expanded` attribute correct

### `TableFelt.test.tsx` (mocks CommunityCards + PotDisplay)
- Renders child components
- Applies theme colors

### `TournamentInfoBar.test.tsx`
- Renders level/blinds/player count
- Ante shown only when non-zero
- `nextLevelIn=null` hides timer
- Break hint computed from `blindStructure`
- Player rank shown only when provided

### `DataTable.test.tsx`
- Renders all rows/columns from data + column config
- `emptyMessage` when `data=[]`
- `currentUser` row highlighted via `highlightField`
- Custom `render` function invoked per cell
- `keyField` used as row key

## Section 3: Medium Components with Timers/Events (5 test files)

### `ActionTimer.test.tsx` (fake timers)
- Correct initial seconds display
- Progress bar width = `remainingSeconds/totalSeconds`
- Color: green >50%, yellow >25%, red otherwise
- Counts down each second via `vi.advanceTimersByTime(1000)`
- Server `remainingSeconds` update resets display

### `BetSlider.test.tsx`
- Range input with correct min/max/value
- Min/All-In presets always shown
- Half-pot/pot presets only when strictly between min and max
- Preset clicks fire `onChange` with correct amount
- Range input change fires `onChange`

### `PasswordDialog.test.tsx` (window keydown events)
- Game name in heading
- Submit disabled when empty, enabled when filled
- `onSubmit` called with password value
- Escape key fires `onCancel`
- `error` prop shows alert message
- Auto-focus on mount

### `GameOverlay.test.tsx` (fake timers for countdown variants)
- `paused`: renders reason + pausedBy
- `eliminated`: shows finish position, `onClose` fires
- `tab-replaced`: renders replacement message
- `continueRunout`: `onContinue` fires on button click
- `rebuy`/`addon`: countdown ticks, accept/decline call `onDecision(true/false)`
- `neverBroke`: countdown + decision callbacks

### `Pagination.test.tsx` (mock `next/navigation`)
- Returns null when ≤1 page
- Previous disabled on page 1, Next disabled on last page
- Page click calls `router.push` with correct URL
- Ellipsis for large page counts
- Existing query params preserved

## Section 4: Auth/Profile UI Components (4 test files)

### `LoginForm.test.tsx` (mock `useAuth` + `next/navigation`)
- Renders username/password/remember-me fields
- Submit disabled while loading
- Successful login redirects to `returnUrl` or `/online`
- Failed login shows error message
- `clearError` called on input change

### `CurrentProfile.test.tsx` (mock `useAuth` + `next/navigation`)
- Returns null when not authenticated
- Renders username when authenticated
- Admin badge only when `isAdmin=true`
- Logout calls `logout()` then navigates to `/`

### `AliasManagement.test.tsx`
- Active/retired aliases in separate sections
- Retire button opens Dialog confirmation
- Confirm shows success message
- Cancel closes dialog

### `PasswordChangeForm.test.tsx` (mock `playerApi.changePassword`)
- Validation: passwords must match, min 8 chars
- Validation failure: error shown, no API call
- Valid submit: API called with correct args
- Success: message shown, fields reset
- API failure: error displayed

## Section 5: Layout Components (3 test files)

### `Sidebar.test.tsx` (mock `next/navigation`)
- Renders section titles and item links
- Active item highlighted on pathname match
- Exact-match items only active on exact path
- Mobile: hamburger opens, Escape/overlay closes

### `Navigation.test.tsx` (mock `useAuth` + `usePathname`, mock `CurrentProfile`)
- Nav items from `navData` rendered
- Admin items hidden when `isAdmin=false`
- Login link when unauthenticated; CurrentProfile when authenticated
- Dropdown open/close on click, outside click, Escape
- Mobile hamburger toggles menu

### `SidebarLayout.test.tsx`
- Renders Sidebar with sections prop
- Renders children in content area

## Section 6: PokerTable + Coverage Threshold (1 test file + config)

### `PokerTable.test.tsx` (mocks 8 hooks + all child components)
All children mocked to `() => <div data-testid="X" />`. Hooks return realistic stubs.

- Renders player seats for each player
- Action panel shown when `isHumanTurn=true`
- Action timer shown when `actionTimer` non-null
- Tournament info bar shown when `isTournament=true`
- Chat panel renders
- Toolbar keyboard shortcuts: H (hand rankings), D (dashboard), V (volume)
- Observer panel shown when player is observer
- Overlay prop rendered when provided

### `vitest.config.ts` — coverage threshold
```ts
thresholds: {
  lines: 80,
  branches: 80,
  functions: 80,
  statements: 80,
}
```

## Test Conventions

- **Framework:** Vitest 3 + React Testing Library (match existing setup)
- **File location:** `__tests__/` directory adjacent to source files
- **Naming:** `ComponentName.test.tsx` / `moduleName.test.ts`
- **Imports:** Explicit named imports from `vitest` (`describe, it, expect, vi, beforeEach`)
- **Queries:** `getByRole` with regex names preferred; `getByTestId` as fallback
- **Absence:** `queryBy*` + `.toBeNull()`
- **Presence:** `getBy*` + `.toBeTruthy()`
- **Test data:** Builder functions with `Partial<T>` overrides
- **Mocks:** `vi.mock()` at module scope; `vi.fn()` for callbacks
- **Timers:** `vi.useFakeTimers()` in `beforeEach`, `vi.useRealTimers()` in `afterEach`
- **State mutations:** Wrapped in `act()`
- **Organization:** Comment banners (`// ---`) within `describe` blocks

## Success Criteria

- All 23 new test files pass via `npm test` from `code/web/`
- Coverage thresholds of 80% line/branch/function/statement enforced
- `npm run test:coverage` passes with thresholds
- No flaky tests (timer-dependent tests use fake timers)
- Existing 38 test files continue to pass
