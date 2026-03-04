# Account Management Design

**Status:** COMPLETED (2026-03-04)
**Date:** 2026-03-03

## Goals

1. Enforce email verification before online features are accessible
2. Restrict the embedded game server to localhost-only binding
3. Provide complete account lifecycle workflows in both the Java thin client and web client
4. Consolidate auth to a single API (game server) and remove the legacy portal auth layer

---

## 1. Data Model Changes

### `OnlineProfile` entity (`pokerengine` module, `wan_profile` table)

New columns:

| Column | Type | Default | Notes |
|---|---|---|---|
| `email_verified` | `BOOLEAN NOT NULL` | `false` | Set `true` after verification |
| `email_verification_token` | `VARCHAR(64) UNIQUE NULL` | `null` | Overwritten on resend |
| `email_verification_token_expiry` | `BIGINT NULL` | `null` | Epoch ms, 7-day TTL |
| `pending_email` | `VARCHAR(255) NULL` | `null` | New email awaiting confirmation |
| `failed_login_attempts` | `INT NOT NULL` | `0` | Reset to 0 on successful login |
| `locked_until` | `BIGINT NULL` | `null` | Epoch ms; null = not locked |
| `lockout_count` | `INT NOT NULL` | `0` | Rolling 24h lockout counter |

Existing rows on migration: `email_verified = false`. These users will be prompted to verify on next interaction with an online feature.

### `OnlineProfileDao` (`pokerserver` module)

New queries:
- `findByEmailVerificationToken(String token)`
- `findByPendingEmail(String email)` — for email change confirmation

---

## 2. Embedded Server Restriction

`EmbeddedGameServer` currently binds to `0.0.0.0` when started with a specific port, making it reachable from outside the local machine.

**Fix:** Set the Spring Boot `server.address` property to `127.0.0.1` when the `embedded` Spring profile is active. Enforced at the OS level before any request handler runs.

- The no-argument `start()` mode (random port) is unchanged
- Standalone `pokerserver` process is unaffected — it does not use the `embedded` profile
- Protects against accidental internet exposure and LAN misuse

---

## 3. Account Creation Workflows

### Web Client

- Landing page: **Register** is the primary CTA in the nav and hero section
- Public pages (marketing, features, downloads, docs) remain accessible without auth
- Game section pages require login + email verification
- Registration form: username, email, password, confirm password
- Username availability checked in real-time with debounce (`GET /api/v1/auth/check-username?username=...`)
- On submit: `POST /api/v1/auth/register` → verification email sent → redirect to `/verify-email-pending`

### Java Thin Client — First Run (No Profiles)

On first launch with no profiles defined, a **first-run wizard** replaces the empty profile picker:

1. Welcome screen: **Play Locally** or **Play Online**
2. **Play Locally** → enter display name → local profile created → main menu
3. **Play Online** → online profile creation wizard (see below)

### Java Thin Client — Online Profile Creation Wizard

Accessible from first-run wizard and from the **[+ New Profile]** button in the profile picker.

Steps:
1. **Server configuration** — Host field (placeholder: `poker.yourdomain.com`, blank by default) + Port field (default: `8877`)
2. **Register or Login** — two tabs:
   - Register: username, email, password
   - Login: username, password
3. On success: local profile created storing server URL, username, JWT (in memory)
4. If `emailVerified = false`: wizard closes, **Email Verification Required** dialog shown immediately (see §5)

### Java Thin Client — Local-to-Online Upgrade

When a user with a local profile attempts an online feature:
1. Dialog: *"This feature requires an online account. Create one now?"*
2. If yes: online profile creation wizard launches
3. Result: a **new** online profile alongside the existing local one — the local profile is not mutated
4. User selects which profile to use going forward from the profile picker

---

## 4. Startup & Profile Switching

### Startup — Online Profile as Default

The last-used profile is persisted as a simple preference (profile name only).

**If last profile is local:** Straight to main menu. No prompt.

**If last profile is online:**

A lightweight startup screen shows:
- "Welcome back, **[username]**" — `server.hostname`
- **[Practice]** — enters main menu immediately, no authentication. Online features will prompt for auth if attempted.
- **[Sign In]** — password prompt (username pre-filled) → authenticated → main menu with full access
- **"Switch / New Profile"** link → Profile Picker

### Profile Picker

- Lists all profiles: local (no icon) and online (lock icon if unauthenticated, server hostname shown)
- **[+ New Profile]** → creation wizard
- Selecting an authenticated online profile (JWT in memory): straight to main menu
- Selecting an unauthenticated online profile: Practice / Sign In choice
- Selecting a local profile: straight to main menu instantly

### Session Caching

- JWT stored **in memory only** per profile — never written to disk (unless Remember Me is checked, see §7)
- Lost on app exit: user re-authenticates once per session per online profile
- Passwords never stored anywhere

---

## 5. Email Verification Flow

### Registration

1. Profile created with `emailVerified = false`
2. 64-character Base64URL token generated, stored on profile with 7-day expiry
3. Verification email sent (see §10)
4. JWT issued with `emailVerified = false` claim
5. Web: redirect to `/verify-email-pending`. Desktop: Email Verification Required dialog.

### Verification

1. User clicks link in email: `GET /api/v1/auth/verify-email?token=...`
2. Token looked up, expiry checked
3. On success: `emailVerified = true`, token fields cleared, fresh JWT issued (`emailVerified = true`)
4. Web: redirect to game lobby. Desktop: gate lifts on next WAN request (JWT refreshed automatically)

### Resend

- `POST /api/v1/auth/resend-verification` — authenticated, rate-limited (1 per 5 minutes)
- New token generated, overwrites old one, new email sent
- Web: **Resend Email** button on `/verify-email-pending` wall and on account settings
- Desktop: **Resend Email** button in the Email Verification Required dialog

### Verification Wall Behaviour

**Web client (game section only):**
- All game section routes check `emailVerified`; redirect to `/verify-email-pending` if false
- Account settings page (change password, change email) accessible to unverified users
- Public pages unaffected

**Java thin client:**
- Practice mode (embedded server) always accessible regardless of verification status
- Any feature touching the hosted WAN server triggers the Email Verification Required dialog
- Dialog shows: email address, **Resend Email** button, **Close** button
- After verifying in browser, next WAN request gets a fresh JWT — gate lifts automatically

---

## 6. Server-side Enforcement

### JWT Claims

`emailVerified: boolean` baked into JWT at login and registration. No DB query on every request.

### `EmailVerificationFilter` (Spring Security, `pokergameserver`)

Runs after JWT authentication. If `emailVerified = false`, returns `403 EMAIL_NOT_VERIFIED` for all endpoints except:

| Exempt endpoint | Reason |
|---|---|
| `POST /api/v1/auth/register` | Account creation |
| `POST /api/v1/auth/login` | Getting the JWT |
| `POST /api/v1/auth/logout` | Always allowed |
| `GET /api/v1/auth/verify-email` | Verification link destination |
| `POST /api/v1/auth/resend-verification` | Request new token |
| `POST /api/v1/auth/forgot-password` | Password recovery |
| `POST /api/v1/auth/reset-password` | Password recovery |
| `GET /api/v1/auth/check-username` | Registration helper |

### JWT Refresh on Verification

`GET /api/v1/auth/verify-email` issues a fresh JWT with `emailVerified = true` in the same response. Web client receives the updated cookie in the verification redirect. Desktop client picks up the fresh JWT on the next authenticated WAN request.

---

## 7. Authentication Workflows

### Login

- `POST /api/v1/auth/login` — username + password
- Response includes: JWT, `emailVerified`, email address (for displaying in verification dialog/wall)
- Checks account lock status before validating password (returns `423 ACCOUNT_LOCKED` with `retryAfterSeconds` if locked)
- Resets `failedLoginAttempts` to 0 on success

### Logout

- `POST /api/v1/auth/logout` — clears JWT cookie
- Desktop: clears in-memory JWT for that profile (and Remember Me file if present)

### Remember Me

**Web:** "Remember me" checkbox on login. If checked: JWT cookie TTL extended to 30 days (vs 24h default).

**Desktop:** "Remember me" checkbox on the Sign In prompt. If checked: JWT written to an encrypted file in the profile directory on disk. On startup, if a valid unexpired JWT file exists for the active online profile, the password prompt is skipped and the user is silently re-authenticated. If expired or file missing, falls back to the normal Practice / Sign In prompt. Settings note: *"Stores a session token on this device."*

---

## 8. Password Workflows

### Password Strength (Server-enforced)

Applied at registration, change password, and reset password:
- Minimum 8 characters
- Maximum 128 characters
- No mandatory complexity rules (NIST SP 800-63B recommendation)

Both client UIs display the same constraints so server rejection is never a surprise.

### Change Password

- `PUT /api/v1/auth/change-password` — authenticated, requires current password in request body
- Returns fresh JWT on success (desktop: cached JWT updated in memory)
- Web: account settings page, accessible to unverified users
- Desktop: profile settings for online profiles only, requires Sign In (not Practice) mode

### Forgot Password

- `POST /api/v1/auth/forgot-password` — unauthenticated, takes email address
- Rate-limited: 1 request per email per hour
- Always returns generic confirmation (prevents email enumeration)
- Sends reset link to registered email: `https://{SERVER_HOST}/reset-password?token=...`
- Desktop: "Forgot password?" link on the Sign In prompt → small dialog, email entry, generic confirmation shown

### Reset Password

- `POST /api/v1/auth/reset-password` — unauthenticated, takes token + new password
- Token: 32-byte Base64URL, 1-hour expiry, single-use
- Web: `/reset-password?token=...` page — handles resets for all clients (desktop users click link in browser, complete reset there, return to desktop and sign in)
- Web reset page: accessible to unauthenticated and unverified users — it is a recovery entry point

---

## 9. Email Change Workflow

- `PUT /api/v1/auth/email` — authenticated, takes new email address
- Server stores new address as `pendingEmail` on the profile
- Sends verification link to the new address (7-day token, same token fields reused)
- Old email remains the active email until new address is confirmed
- On confirmation (`GET /api/v1/auth/verify-email?token=...`): `email` swapped to `pendingEmail`, `pendingEmail` cleared, fresh JWT issued
- User can abandon the change by ignoring the link — old email remains indefinitely
- Web: account settings page. Desktop: online profile settings

---

## 10. Email Infrastructure

### Library

`DDPostalService` from the existing `mail` module added as a dependency to `pokergameserver`. No new mail library introduced.

### Configuration

All existing Docker environment variables reused unchanged:

| Docker env var | Property | Default |
|---|---|---|
| `SMTP_HOST` | `settings.smtp.host` | `127.0.0.1` |
| `SMTP_PORT` | `settings.smtp.port` | `587` |
| `SMTP_USER` | `settings.smtp.user` | — |
| `SMTP_PASSWORD` | `settings.smtp.pass` | — |
| `SMTP_AUTH` | `settings.smtp.auth` | `false` |
| `SMTP_STARTTLS_ENABLE` | `settings.smtp.starttls.enable` | `true` |
| `SMTP_FROM` | `settings.server.profilefrom` | `noreply@ddpoker.com` |

`SERVER_HOST` (existing) used to construct verification and reset links in emails.

No new Docker environment variables required.

### Email Templates (`pokergameserver`)

1. **Verification email** — subject: *"Verify your DD Poker account"*
   Body: verification link (`https://{SERVER_HOST}/verify-email?token={token}`), expires in 7 days

2. **Password reset email** — subject: *"Reset your DD Poker password"*
   Body: reset link (`https://{SERVER_HOST}/reset-password?token={token}`), expires in 1 hour

3. **Email change confirmation** — subject: *"Confirm your new DD Poker email address"*
   Body: confirmation link (`https://{SERVER_HOST}/verify-email?token={token}`), expires in 7 days

### Dev/Embedded Fallback

When `SMTP_HOST` is not set, `EmailService` logs the full verification/reset link at INFO level. Local development works without an SMTP server.

---

## 11. API Consolidation

### Removed from `api` module (no backwards compatibility)

- `AuthController` — `/api/auth/login`, `/api/auth/logout`
- `ProfileController` auth endpoints — `/api/profile/forgot-password`, `/api/profile/password`
- `EmailService` — the DDPostalService wrapper for temp-password emails

The `api` module is retained for admin panel and non-auth web portal endpoints.

### Added to `pokergameserver`

| Endpoint | Purpose |
|---|---|
| `PUT /api/v1/auth/change-password` | Authenticated password change |
| `PUT /api/v1/auth/email` | Request email address change |
| `GET /api/v1/auth/check-username` | Username availability (rate-limited) |
| `POST /api/v1/auth/resend-verification` | Request new verification token |

CORS configuration on `pokergameserver` extended to allow browser requests from `SERVER_HOST`.

### Web Client (`code/web/lib/api.ts`)

All `authApi.*` calls to `/api/auth/*` replaced with `gameServerApi.*` calls to `/api/v1/auth/*`.

New pages:
- `/verify-email-pending` — verification wall (email shown, Resend button, instructions)
- `/verify-email` — verification link landing page; calls API, redirects to lobby on success or shows expired/invalid error with Resend option
- `/reset-password` — password reset form (token from query string, new password + confirm)

Existing pages updated:
- `/login` — add Remember Me checkbox, Forgot Password link
- `/register` — add username availability check, link to `/verify-email-pending` on success
- `/forgot` — updated to call game server endpoint

Account settings page (new):
- Change password form
- Change email form
- Resend verification button (if unverified)

---

## 12. Rate Limiting & Account Lockout

### Failed Login Lockout

- Tracked per account, persisted to DB (survives server restarts)
- After **5 failures within 15 minutes**: account locked
- Progressive lockout durations:
  - 1st lockout: 5 minutes
  - 2nd lockout: 15 minutes
  - 3rd lockout: 1 hour
  - 4th+ lockout within rolling 24h: requires admin manual unlock
- Login returns `423 ACCOUNT_LOCKED` with `retryAfterSeconds`
- Both clients display: *"Account locked, try again in X minutes"*
- Auto-unlock when duration expires (no user action needed for first three)
- `failedLoginAttempts` and `lockoutCount` reset to 0 on successful login

### Endpoint Rate Limits

| Endpoint | Limit |
|---|---|
| `POST /api/v1/auth/forgot-password` | 1 per email per hour |
| `POST /api/v1/auth/resend-verification` | 1 per account per 5 minutes |
| `GET /api/v1/auth/check-username` | 30 per IP per minute |
| `POST /api/v1/auth/register` | 10 per IP per hour |

---

## 13. Admin Account Management

Extensions to the existing admin panel:

- **Accounts list** — all accounts with verification status, lock status, lockout count, registration date
- **Manual verify** — mark account as verified without email confirmation
- **Resend verification** — trigger a new verification email for a user
- **Unlock account** — clear lock and reset lockout count
- **Disable/retire account** — sets existing `retired` flag (soft delete)
- **View login history** — recent failed attempts and lockout events per account

---

## 14. Username Availability Check

- `GET /api/v1/auth/check-username?username=...` — unauthenticated
- Returns `{ "available": true/false }` — no detail on why unavailable (prevents enumeration)
- Rate-limited: 30 per IP per minute
- Web: called with debounce during registration as user types
- Desktop: called on blur in the online profile creation wizard

---

## 15. Deferred / Out of Scope

| Feature | Reason deferred |
|---|---|
| **Username rename** | Username is a public identity tied to game history, leaderboards, and chat. A rename cascades broadly. Future direction: introduce a separate **display name** distinct from the login username — username becomes a permanent private credential, display name is what appears at the table and on leaderboards. |
| **Account deletion** | Cascading concerns across game history, leaderboards, tournament records. The existing `retired` flag provides soft-disable for now. GDPR-compliant deletion is a dedicated feature. |
| **Concurrent session management / sign out all devices** | JWT is stateless; multiple sessions work by default. Revocation requires a token blocklist. Separate feature. |

---

## 16. Modules Affected

| Module | Changes |
|---|---|
| `pokerengine` | `OnlineProfile` entity — 7 new columns |
| `pokerserver` | `OnlineProfileDao` — new queries; `PasswordResetTokenDao` unchanged |
| `pokergameserver` | `AuthService`, `AuthController`, `JwtService` — new endpoints and claims; new `EmailService`; `EmailVerificationFilter`; CORS update; `mail` module dependency added |
| `poker` (desktop) | `RestAuthClient` — new methods; first-run wizard; startup screen; profile picker; Email Verification Required dialog; Remember Me on sign-in prompt; online feature gates |
| `api` | Remove `AuthController`, `ProfileController` auth endpoints, `EmailService` |
| `code/web` | `api.ts` consolidation; new pages (`/verify-email-pending`, `/verify-email`, `/reset-password`); updated pages (`/login`, `/register`, `/forgot`); new account settings page |
| `docker` | No new env vars; `docker-compose.yml` comments updated to document `SERVER_HOST` role in email links |
