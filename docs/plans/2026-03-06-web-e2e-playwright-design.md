# Web Client E2E Testing with Playwright — Design

**Status:** APPROVED (2026-03-06)

**Goal:** Comprehensive browser-based end-to-end testing of the Next.js web client using Playwright, covering all user-facing workflows against a real pokergameserver backend. Tests are written from the user's perspective — they validate what users should experience, not what the code currently does. This makes them effective at catching existing bugs and preventing regressions.

---

## Architecture

```
+-------------------------------------------+
| pokergameserver (child process)            |
| Spring Boot, embedded profile, H2 in-mem  |
| DevController: /api/v1/dev/verify-user     |
|               /api/v1/dev/reset            |
+-------------------------------------------+
         | REST + WebSocket (port 8877)
+-------------------------------------------+
| Next.js Production Server                 |
| next build + next start (port 3000)       |
| NEXT_PUBLIC_GAME_SERVER_URL=localhost:8877 |
+-------------------------------------------+
         | HTTP (localhost:3000)
+-------------------------------------------+
| Playwright Test Runner                     |
| Chromium browser                          |
| globalSetup: start servers                |
| globalTeardown: stop servers              |
+-------------------------------------------+
```

### Key Design Decisions

- **Real backend** — tests run against a real pokergameserver with H2 in-memory DB, not mocked APIs. This catches integration bugs between frontend and backend.
- **Production build** — `next build` + `next start` for the web client. Catches SSR/build errors and is faster per-test than dev mode.
- **Chromium only** — single browser for speed. More browsers can be added later via config.
- **Fresh DB per suite** — a new `POST /api/v1/dev/reset` endpoint truncates all tables between test suites. Gated by `@Profile("embedded")` so it cannot exist in production.
- **Serial execution** — 1 Playwright worker since all suites share a single DB.
- **User-perspective tests** — tests navigate via clicks, verify what the user sees, and test error states. Not coded around current implementation quirks.

---

## New Backend Endpoint

One new endpoint in existing `DevController.java` (already gated by `@Profile("embedded")`):

```java
@PostMapping("/reset")
public Map<String, Object> resetDatabase() {
    // Truncate all tables via JdbcTemplate
    // Reset auto-increment sequences
    return Map.of("success", true);
}
```

**Security:** The `DevController` class is annotated `@Profile("embedded")`. The bean is not created in production deployments — the endpoint simply does not exist. No API key or auth needed since the entire controller is profile-gated.

---

## Server Lifecycle

- **`global-setup.ts`** — Builds Next.js app (`next build`), starts pokergameserver JAR as child process, starts Next.js production server (`next start`). Waits for health checks on both (`/api/v1/health` and the Next.js port). Exports process handles for teardown.
- **`global-teardown.ts`** — Kills both child processes.
- **Test helper `resetDatabase()`** — Called in `beforeAll` of each suite that needs a clean DB. Sends `POST /api/v1/dev/reset` directly to the game server.

---

## Playwright Configuration

- **Browser:** Chromium only
- **Base URL:** `http://localhost:3000`
- **Timeouts:** 30s default action timeout, 120s for `@slow` tagged tests
- **Retries:** 1 on CI, 0 locally
- **Workers:** 1 (serial — shared DB)
- **Screenshots:** On failure only
- **Video:** Off (enable per-test for debugging)
- **Test directory:** `code/web/e2e/`

---

## File Structure

```
code/web/
  e2e/
    fixtures/
      test-helper.ts          # TestHelper class, auth utilities, DB reset
    static-pages.spec.ts
    registration.spec.ts
    login-and-recovery.spec.ts
    account-management.spec.ts
    online-portal.spec.ts
    game-creation.spec.ts
    gameplay.spec.ts
    admin.spec.ts
  playwright.config.ts
  global-setup.ts
  global-teardown.ts
```

---

## Test Helper (`test-helper.ts`)

Wraps common operations so tests stay focused on user behavior:

- `resetDatabase()` — `POST /api/v1/dev/reset` to game server
- `registerUser(username, password, email)` — `POST /api/v1/auth/register` directly (API shortcut for setup)
- `verifyUser(username)` — `POST /api/v1/dev/verify-user?username=...` (bypass email)
- `loginViaUI(page, username, password)` — fills login form, clicks submit (used when testing the login flow itself)
- `loginViaAPI(context, username, password)` — `POST /api/v1/auth/login` and captures the cookie (fast setup for tests that need auth but aren't testing login)
- `createAndVerifyUser(username, password, email)` — register + verify in one call (common setup)
- `getForgotPasswordToken(email)` — `POST /api/v1/auth/forgot-password` (embedded mode returns token in response)

---

## Test Suites

### Suite 1: Static Pages & Navigation (~10 tests)

A first-time visitor exploring the site.

| Test | User Action | Expected Outcome |
|------|-------------|------------------|
| Home page loads | Visit site | See DD Poker branding, feature descriptions, download link |
| Navigate to About | Click "About" in nav | About page loads, nav highlights About as active |
| About sub-pages | Click sidebar links (FAQ, Screenshots, etc.) | Each sub-page loads with content |
| Download page | Click "Download" in nav | Platform-specific installers listed with version info |
| Support page | Click "Support" in nav | Help options displayed |
| Terms page | Navigate to terms | Legal content visible |
| Online portal | Click "Online" | Hub page with links to leaderboard, search, etc. |
| Footer links | Click footer links | Navigate correctly |
| Nav active state | Navigate to different pages | Active page highlighted in nav |
| Leaderboard without login | Visit leaderboard directly | Renders, no login required |

**DB Reset:** No (read-only pages)

---

### Suite 2: New User Registration (~10 tests)

A brand new user signing up.

| Test | User Action | Expected Outcome |
|------|-------------|------------------|
| Navigate to register | Home → click "Create Account" link | Register form loads |
| Short username | Type < 3 chars | No availability feedback shown |
| Available username | Type valid unused name, wait for debounce | "Username available" shown in green |
| Taken username | Type existing username, wait for debounce | "Username not available" shown in red |
| Mismatched passwords | Fill form, passwords don't match, submit | "Passwords do not match" error |
| Short password | Fill form, password < 8 chars, submit | "Password must be at least 8 characters" error |
| Successful registration | Complete form correctly, submit | Redirected to "Check your email" page showing the user's email |
| Unverified gate | Try to navigate to /games | Redirected to verify-email-pending, not games |
| Email verification | Visit /verify-email?token=... (from dev endpoint) | Redirected to games page |
| Post-verification access | Navigate to /games after verifying | Game Lobby loads successfully |

**DB Reset:** Yes (beforeAll)

---

### Suite 3: Login, Logout & Password Recovery (~10 tests)

Returning user and recovery flows.

| Test | User Action | Expected Outcome |
|------|-------------|------------------|
| Navigate to login | Click "Log In" in nav | Login form loads |
| Wrong password | Enter valid username, wrong password, submit | Clear error message displayed |
| Successful login | Enter correct credentials, submit | Redirected to online portal (default destination) |
| Login with returnUrl | Visit /games while logged out → redirected to login → login | After login, taken back to /games |
| Login when unverified | Login with unverified account | Redirected to verify-email-pending, not to games |
| Profile indicator | Log in successfully | Username/profile visible in nav |
| Logout | Click logout | Redirected away; /games redirects to login |
| Forgot password link | Login page → click "Forgot your password?" | Forgot password form loads |
| Forgot password flow | Enter email, submit → visit reset link with token → enter new password | "Password reset" confirmation → redirected to login → login with new password works |
| Reset without token | Visit /reset-password with no token | "Invalid link" message with link to request new one |

**DB Reset:** Yes (beforeAll)

---

### Suite 4: Account Management (~8 tests)

Logged-in user managing their account.

| Test | User Action | Expected Outcome |
|------|-------------|------------------|
| Account page loads | Navigate to account | See current email, change password form, change email form |
| Wrong current password | Enter wrong current password in change form | Error message displayed |
| Mismatched new passwords | New password and confirm don't match | "Passwords do not match" error |
| Short new password | New password < 8 chars | Validation error |
| Successful password change | Change password correctly | Success message → logout → login with new password works |
| Change email | Enter new email, submit | "Confirmation email sent" message |
| Verification section (unverified) | Unverified user visits account | Sees email verification section with resend button |
| Resend verification | Click resend button | "Email resent" confirmation |

**DB Reset:** Yes (beforeAll)

---

### Suite 5: Online Portal (Public Data) (~10 tests)

Any visitor browsing online stats.

| Test | User Action | Expected Outcome |
|------|-------------|------------------|
| Leaderboard default | Visit leaderboard | DDR1 mode, ranked players with scores displayed |
| Switch to ROI mode | Click ROI mode toggle | Scores change to percentages |
| Filter by name | Enter player name in filter | Results narrow to matching players |
| Leaderboard pagination | Navigate to page 2 (if data exists) | Different results shown |
| Player search | Enter name → see results | Results shown with highlighted search term |
| Click through to history | Click a player name in search results | Player's tournament history page loads |
| History stats cards | View player history | Stats cards show games, wins, win rate, avg placement |
| History date filter | Apply date filter | Results narrow |
| Hosts page | Visit hosts page | Host names and game counts displayed |
| Empty search | Search for nonexistent player | "No results" message, not blank page or error |

**DB Reset:** Yes (beforeAll, seeds test data via API — creates users and completes games)

---

### Suite 6: Game Creation & Lobby (~10 tests)

User creating and managing games.

| Test | User Action | Expected Outcome |
|------|-------------|------------------|
| Navigate to create | Games page → click "Create Game" | Create form loads |
| Quick Practice | Click Quick Practice button | Immediately land on play page with poker table |
| Custom practice game | Set name, 3 AI opponents, fast blinds, submit | Land on play page with 4 players at table |
| Create online game | Fill form for online game, submit | Redirected to lobby with game name, self listed as owner |
| Owner controls | View lobby as game owner | Start Game and Cancel Game buttons visible |
| Cancel game | Click Cancel Game | Redirected to game list; game no longer in "Open" tab |
| Game visible in lobby | Create game, check games list in new context | Game appears in "Open" tab with correct info (name, host, player count) |
| Search for game | Type game name in search box | Game found in filtered results |
| Tab switching | Click Open → In Progress → Completed tabs | Different game lists shown per tab |
| Password-protected game | Create private game, try to join from second context | Password dialog appears; correct password lets you in |

**DB Reset:** Yes (beforeAll)

---

### Suite 7: Gameplay (Practice) (~12 tests)

User playing a poker game.

| Test | User Action | Expected Outcome |
|------|-------------|------------------|
| Table renders | Start practice game | Poker table with correct number of player seats |
| Player seat position | Look at the table | Current player's seat at bottom center with hole cards face-up |
| AI cards hidden | Look at AI seats | AI players show face-down cards |
| Tournament info | Look at top bar | Current blinds, level number, player count shown |
| Action prompt | Wait for turn | Available actions displayed (fold, check/call, etc.) |
| Fold action | Click Fold button | Hand continues, hand history shows fold entry |
| Check/Call action | Click Check or Call | Pot amount changes, action recorded in history |
| Keyboard fold | Press F key when action available | Same as clicking Fold |
| Theme picker | Open theme picker, change table color | Table felt color updates visually |
| Hand history toggle | Click hand history panel | Opens to show entries; click again to close |
| Chat panel | Type message in chat, send | Message appears in chat history |
| **@slow Full game** | 2 AI, 500 chips, 100/200 blinds, always call | Game eventually ends → results page shows placements (1st, 2nd, 3rd) with player names |
| Results page links | View results after game | Link to create another game works |

**DB Reset:** Yes (beforeAll)

---

### Suite 8: Admin (~8 tests)

Admin user managing the platform.

| Test | User Action | Expected Outcome |
|------|-------------|------------------|
| Non-admin blocked | Regular user navigates to /admin | Cannot access admin features (redirect or no admin nav link) |
| Admin nav | Login as admin | "Admin" link visible in navigation |
| Admin dashboard | Click Admin in nav | Dashboard with links to Profile Search and Ban List |
| Profile search | Search by username | Results with profile details (name, email, status) |
| Verify email action | Click verify on unverified profile | Profile marked as verified |
| Unlock account | Click unlock on locked account | Account unlocked |
| Add ban | Fill ban form, submit | Ban appears in ban list |
| Remove ban | Click remove on existing ban | Ban disappears from list |

**DB Reset:** Yes (beforeAll)

**Note:** Requires a way to create an admin user. The test helper will need to either:
- Use a dev endpoint to grant admin role, or
- Pre-seed an admin user via the reset endpoint, or
- Use a known admin credential from the embedded profile config

---

## Implementation Order

| Phase | Work |
|-------|------|
| 1 | Add `POST /api/v1/dev/reset` endpoint to `DevController` |
| 2 | Install Playwright, create config, global setup/teardown |
| 3 | Build test helper with auth and DB utilities |
| 4 | Static pages suite (simplest, validates infrastructure) |
| 5 | Registration suite |
| 6 | Login & recovery suite |
| 7 | Account management suite |
| 8 | Online portal suite (needs data seeding) |
| 9 | Game creation & lobby suite |
| 10 | Gameplay suite (most complex, WebSocket-dependent) |
| 11 | Admin suite (needs admin user creation) |

---

## Open Questions for Implementation

1. **Admin user creation** — How does the embedded profile create admin users? Need to check if there's a dev endpoint or seed data mechanism.
2. **Forgot password token** — The embedded profile returns the reset token in the API response (not sent via email). Need to verify this works for the E2E flow.
3. **WebSocket stability** — Practice game WebSocket connection through the Next.js server. Need to verify the proxy/connection path works in production build mode.
4. **pokergameserver JAR path** — Need to determine the built JAR location and any required JVM flags for the child process.
5. **Port conflicts** — Both servers need stable ports (8877 and 3000). May need configurable ports or port-finding logic.
