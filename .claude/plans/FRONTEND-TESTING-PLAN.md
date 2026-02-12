# Frontend Testing Improvement Plan

## Context

DD Poker has two UI surfaces: a **Wicket web portal** (server-rendered HTML) and a **Swing desktop client** (custom Java UI). The web portal has a minimal Playwright setup (12 navigation tests) that needs full functional + visual regression coverage. The Swing desktop had AssertJ Swing tests that proved unreliable due to EDT timing issues, custom component rendering, headless incompatibility, and slow initialization. This plan expands web testing comprehensively and proposes a practical alternative for desktop visual validation.

## Part 1: Playwright Web Testing (Full Functional + Visual Regression)

### Test Organization

```
code/pokerwicket/tests/
  fixtures/
    base.ts                    # Shared Playwright fixtures (common locators, helpers)
    test-data.sql              # H2 seed data for auth and data-dependent tests
  pages/                       # Page Object Model
    base.page.ts               # Header, footer, nav locators, assertNoConsoleErrors()
    about.page.ts              # About section pages
    online.page.ts             # Online portal pages (extends base with login panel)
    leaderboard.page.ts        # Leaderboard with pagination, type switching
    admin.page.ts              # Admin pages
  helpers/
    auth.helper.ts             # Login/logout via Login panel form
  navigation.spec.ts           # (existing 12 tests, keep as-is)
  static-pages.spec.ts         # Phase 1: all static page rendering
  about-section.spec.ts        # Phase 1: about sub-pages
  support-section.spec.ts      # Phase 1: support sub-pages
  error-pages.spec.ts          # Phase 1: error, expired, 404
  responsive.spec.ts           # Phase 1: mobile vs desktop layouts
  visual-regression.spec.ts    # Phase 1: full-page visual baselines
  auth.spec.ts                 # Phase 2: login/logout, remember-me, invalid creds
  my-profile.spec.ts           # Phase 2: profile page after login
  online-games.spec.ts         # Phase 3: available/current/running/recent games
  leaderboard.spec.ts          # Phase 3: leaderboard pagination + type switching
  search.spec.ts               # Phase 3: player search
  admin-pages.spec.ts          # Phase 4: admin home, ban list, searches
```

### Phased Rollout

**Phase 1 - Static Pages & Visual Regression:**
- Every static page loads (HTTP 200, no console errors): `/`, `/about/*`, `/download`, `/support/*`, `/terms`
- Error pages: `/notfound` (404), `/expired`, `/error`
- Header/footer/nav render on every page
- Secondary nav on section pages (about, support, online)
- Active nav state is correct per page
- Mobile responsive layout (hamburger menu, stacking)
- Visual regression baselines for every static page at desktop (1280x720) + mobile (375x667)

**Phase 2 - Authentication:**
- Requires: test seed SQL with a regular user and an admin user
- Login form renders with name/password/remember-me fields
- Successful login shows username, invalid creds show error
- Cookie-based "remember me" persists across reloads
- Logout clears session
- `/myprofile` redirects unauthenticated users
- `/admin` requires admin auth

**Phase 3 - Data-Dependent Pages:**
- Requires: test seed SQL with games in various states + tournament history
- Game listing tables render correct columns
- "No results" state when empty
- Pagination (next/previous/page numbers)
- Leaderboard type switching (DDR1 vs ROI)
- Player search form submission + results
- Game detail page with valid game parameters

**Phase 4 - Admin & Edge Cases:**
- Admin pages after admin login
- Ban list, profile search, registration search
- RSS feed endpoints return valid XML

### Config Changes to `playwright.config.ts`

- Add `expect.toHaveScreenshot` defaults: `maxDiffPixels: 100`, `animations: 'disabled'`
- Update `webServer` to auto-start in CI: `mvn -pl pokerwicket exec:java` with `PokerJetty` daemon mode
- Keep `reuseExistingServer: !process.env.CI` for local dev flexibility
- Consider CI-only single-browser for speed (Chromium), full matrix on main branch

### Database Seeding Strategy

- Create `tests/fixtures/test-data.sql` with:
  - 2-3 test player profiles in `wan_profile` (one matching `settings.admin.user`)
  - 5-10 games across states (available, in-progress, completed) in `wan_game`
  - Tournament history records in `wan_history` for leaderboard data
  - At least one banned key for ban list tests
- Server loads seed data at startup via H2 `INIT=RUNSCRIPT` or application init hook

### Key Files to Modify
- `code/pokerwicket/playwright.config.ts` - webServer auto-start, visual regression defaults
- `code/pokerwicket/package.json` - potentially add seed/setup scripts

### Key Files to Reference
- `code/pokerwicket/tests/navigation.spec.ts` - existing patterns to follow
- `code/pokerwicket/src/main/java/.../panels/Login.java` - login form fields
- `code/pokerwicket/src/main/java/.../util/LoginUtils.java` - auth flow
- `code/gameserver/src/main/resources/h2-init.sql` - database schema for seed data
- `code/pokerwicket/src/main/java/.../pages/online/Leaderboard.java` - example data-dependent page

---

## Part 2: Swing Desktop Testing (Alternative to AssertJ Swing)

### Why AssertJ Swing Failed
- ~30s+ startup per test (full `PokerMain` initialization)
- EDT thread timing caused flakiness
- Cannot run headless in CI without Xvfb
- 70+ custom `DD*` components with custom painting bypass standard component lookup
- `InternalDialog` (JInternalFrame) instead of standard JDialog required custom matchers

### Recommended: Headless Screenshot Comparison Tests

Render the Swing app, capture screenshots at key states, compare against baselines with image diff. This validates visual correctness without fragile interaction testing.

**Approach:**
1. Launch app on EDT, navigate programmatically via game engine phase system (not GUI clicks)
2. Capture window as `BufferedImage` via `Robot.createScreenCapture()` or component `paint()`
3. Compare against baseline images with configurable pixel tolerance
4. Store baselines in `src/test/resources/screenshots/`

**Proposed Tests** (in `code/poker/src/test/java/.../ui/visual/`):
- `VisualMainMenuTest` - Main menu after launch
- `VisualPracticeSetupTest` - Practice game setup dialog
- `VisualGameTableTest` - Poker table rendering
- `VisualPokerClockTest` - Poker clock display

**Infrastructure:**
- Extend existing `PokerUITestBase` (has screenshot capture + `@EnabledIfDisplay`)
- Add `assertScreenshotMatches(baselineName, tolerance)` utility
- Use simple pixel-diff comparison (no external library needed for basic comparison)
- Tag as `@Tag("visual")` for selective execution

**Tradeoffs:**
- Validates rendering correctness (works with custom DD components)
- Platform-dependent (font rendering differs across OS) - baselines per OS
- Requires display (Xvfb in Linux CI) but more reliable than interaction testing
- Does not test click/type interactions, only appearance

### Alternatives Considered (Not Recommended)
- **SikuliX**: Image recognition per action - extremely slow and brittle
- **Playwright for Swing**: Not possible - Playwright only works with browsers
- **Accessibility API testing**: DD Poker's custom components likely lack proper AccessibleContext
- **Commercial tools** (Squish, TestComplete): Cost prohibitive for community project

---

## Part 3: CI Integration (GitHub Actions)

### New Workflow: `.github/workflows/e2e.yml`

Separate from existing `ci.yml` (Java unit/integration tests) because:
- Different dependencies (Node.js, Playwright browsers)
- Longer runtime (server startup + browser tests)
- Can fail independently

**Steps:**
1. Checkout code
2. Setup JDK 25 (Temurin) with Maven cache
3. Build project: `mvn clean package -DskipTests -pl pokerwicket -am`
4. Setup Node.js 20 with npm cache
5. Install Playwright: `npm ci && npx playwright install --with-deps`
6. Seed test database (place seed data for H2)
7. Start Wicket server in background (PokerJetty daemon mode)
8. Wait for server ready (poll localhost:8080)
9. Run Playwright tests: `npx playwright test`
10. Upload artifacts: `playwright-report/` and `test-results/` (14-day retention)

**Triggers:** Push to main, PRs to main, manual dispatch

**Browser strategy:**
- PRs: Chromium only (fast feedback)
- Main branch: Full matrix (Chromium, Firefox, WebKit, Mobile Chrome)

### Visual Regression Baselines in CI
- Baselines committed to git in `tests/*.spec.ts-snapshots/`
- Platform-specific filenames auto-generated by Playwright
- Generate initial baselines in CI environment (Linux) to match CI rendering
- Update with `npx playwright test --update-snapshots`

---

## Implementation Sequence

| Phase | Scope | Depends On |
|-------|-------|------------|
| 1 | Static pages + visual regression + page objects | Nothing |
| CI | GitHub Actions e2e workflow | Phase 1 |
| 2 | Auth flow tests + test database seed | Phase 1 |
| 3 | Data-dependent page tests | Phase 2 (seed data) |
| 4 | Admin pages + edge cases | Phase 2 |
| Swing | Headless screenshot comparison tests | Independent |

## Verification

- **Phase 1:** `npm test` from `code/pokerwicket/` passes all static page + visual regression tests locally
- **CI:** GitHub Actions e2e workflow runs green on a PR
- **Phase 2-4:** Tests pass with seeded database, both locally and in CI
- **Swing:** `mvn test -P dev -Dgroups=visual` runs screenshot comparison tests (on machines with display)
- **Visual regression:** `npx playwright test --update-snapshots` regenerates baselines without failures
