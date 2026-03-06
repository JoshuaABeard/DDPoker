# Web Client E2E Test Results — Initial Run

**Date:** 2026-03-06
**Branch:** `feature/web-e2e-playwright`
**Result:** 27 passed, 47 failed (74 total)

---

## Infrastructure Status: Working

The E2E infrastructure is fully operational:
- pokergameserver starts as standalone JAR with H2 in-memory DB
- Next.js production build + server starts on port 3000
- Playwright runs Chromium against the production build
- DB reset between test suites works
- User registration/verification via API works
- Login via UI with cross-origin cookie propagation works

## Infrastructure Fixes Applied

| Fix | Issue |
|-----|-------|
| `aria-current="true"` in DataTable | React TS types don't include `"row"` value |
| Exclude e2e/playwright from tsconfig | Next.js was type-checking Playwright files |
| CORS config in standalone server | Browser fetch to port 8877 was blocked |
| Public endpoint permissions | Leaderboard, search, history returned 403 |
| API base URL double prefix | `/api/v1/api/v1/...` caused all API calls to 403 |
| Cookie propagation in test helper | JWT cookie set on port 8877 not visible to port 3000 |

## Passing Tests (27)

### Static Pages (8/10)
- Home page loads with branding and download link
- Navigate to About via nav link
- Download page shows platform installers
- Support page loads via nav
- Terms page loads at /terms
- Leaderboard loads without login
- Footer is visible on every page
- Navigation highlights active page

### Registration (4/10)
- Navigate from login to register
- Short username shows no availability feedback
- Mismatched passwords shows error
- Unverified user cannot access /games

### Login & Recovery (4/10)
- Navigate to login via nav
- Successful login redirects to /online
- Login with unverified account redirects to /verify-email-pending
- Logged-in user sees username in navigation

### Account Management (3/8)
- Change password with mismatched new passwords shows error
- Change password with short new password shows error
- Unverified user sees email verification section

### Online Portal (3/10)
- Leaderboard loads in default mode
- Hosts page loads
- My profile page requires login

### Admin (1/8)
- Non-admin user cannot see admin nav link

### Game Creation (0/8)
All fail — tests expect specific UI elements not yet matching

### Gameplay (0/13)
All fail — depends on game creation working first

## Failure Categories

### 1. Login cookie not propagating to Next.js middleware (many tests)
The `ui.login()` helper copies the JWT cookie from port 8877 to localhost, but some tests still get redirected to `/login`. The cookie propagation timing or the middleware cookie check needs investigation.

### 2. UI text/element mismatches (many tests)
Tests expect specific text like "at least 8 characters", "Username available", "no games found", etc. The actual UI uses different text or different element structures. These are the tests doing their job — revealing what the UI actually shows vs. what users should see.

### 3. Admin features not wired (6 tests)
Admin nav link not visible even for admin users. The `ProfileResponse.admin` field was added in this branch but the web client may need updates to read it properly.

### 4. Game creation/lobby UI structure (8 tests)
Tests expect tabs (Open/In Progress/Completed), search input, Quick Practice button, etc. The actual game creation UI may use different component names or structures.

### 5. Online portal page structures (6 tests)
Search, history, and profile pages have different element structures than expected. Some use emoji prefixes in link names (e.g., "Leaderboard" link text is actually "Leaderboard" with emoji).

## Next Steps

1. **Fix remaining infrastructure issues** — investigate cookie propagation reliability
2. **Update tests to match actual UI** — adapt selectors/text to what the app actually renders, while keeping the user-perspective intent
3. **Fix real bugs found** — cases where the UI genuinely doesn't work as expected (not just selector mismatches)
4. **Squash fix commits** — consolidate the infrastructure fix commits before merging
