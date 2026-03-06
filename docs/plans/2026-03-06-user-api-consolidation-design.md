# User API Consolidation & Fixes — Design

**Status:** COMPLETED (2026-03-06)

## Problem

The user management APIs have accumulated several issues:

1. **Broken endpoints** — Frontend calls routes that don't exist on the backend (e.g., `/api/v1/auth/change-password`, `/api/v1/profiles/me`)
2. **Request body mismatches** — Field names differ between frontend and backend DTOs (`password` vs `newPassword`, `currentPassword` vs `oldPassword`)
3. **Sensitive data leakage** — Public profile endpoints return raw `OnlineProfile` JPA entities, exposing `emailVerificationToken`, `failedLoginAttempts`, `lockedUntil`, `lockoutCount`, and `pendingEmail`
4. **Stubbed code** — `AliasManagement.tsx` retire function is a no-op with a TODO
5. **Redundant API clients** — Four frontend objects (`authApi`, `playerApi`, `profileApi`, `gameServerApi`) with overlapping and duplicated user methods
6. **Inconsistent response shapes** — `LoginResponse` and `ProfileResponse` carry overlapping user data in different shapes; frontend has a separate `AuthResponse` type that doesn't match either
7. **Tournament stats bug** — Stats calculated client-side from current page only, producing wrong results for paginated data
8. **Ban API mismatch** — Frontend sends `{ key, comment }` but backend entity expects `{ banType, profileId/email, reason }`

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Email change flow | Verification-first only (`PUT /auth/email`) | Secure; no direct update without email ownership proof |
| Frontend client structure | Two clients: `authApi` + `profileApi` | Maps to backend controller boundary; clear ownership |
| Profile response shapes | Two DTOs: `ProfileResponse` (private) + `PublicProfileResponse` (public) | Public lookups shouldn't expose email or verification status |
| `PUT /profiles/{id}` | Remove entirely | Only field was email, now handled by `/auth/email` |
| Change-password endpoint | `PUT /auth/password` (ID from JWT) | Password is an auth credential; no `/me` pattern needed |
| Ban request | New `CreateBanRequest` DTO | Keep entity internal; clean API contract |
| `ProfileResponse` as canonical shape | Expanded with `emailVerified`, `admin`, `createDate` | One type for "current user" everywhere |
| `LoginResponse` structure | Wraps `ProfileResponse` with auth metadata | Eliminates duplication between login and me responses |
| `GET /auth/me` | Keep | Validates session + refreshes stale cached data on page load |
| Alias retire ownership | Expand to allow same-email profiles | Otherwise the retire button can never work for aliases |
| Tournament stats | Server-side aggregation | Client-side page-only stats are incorrect |

## Backend Changes

### Endpoint Changes

| Change | Before | After |
|--------|--------|-------|
| Move change-password | `PUT /profiles/{id}/password` | `PUT /auth/password` (ID from JWT) |
| Remove direct profile update | `PUT /profiles/{id}` | Deleted |
| Remove profile delete | `DELETE /profiles/{id}` | Deleted (retire via `POST /profiles/{id}/retire` only) |

**AuthController gains:** `PUT /auth/password`

**ProfileController loses:** `PUT /{id}`, `DELETE /{id}`, `PUT /{id}/password`

**ProfileController keeps:** `GET /{id}`, `GET /name/{name}`, `GET /aliases`, `POST /{id}/retire`

### DTO Changes

**`ProfileResponse`** — expanded to canonical "current user" shape:

```java
public record ProfileResponse(
    Long id, String username, String email,
    boolean emailVerified, boolean admin, boolean retired,
    String createDate
) {}
```

**`LoginResponse`** — wraps `ProfileResponse` with auth metadata:

```java
public record LoginResponse(
    boolean success, ProfileResponse profile,
    String token, String message, Long retryAfterSeconds
) {}
```

**New `PublicProfileResponse`** — for unauthenticated profile lookups:

```java
public record PublicProfileResponse(Long id, String name, String createDate) {}
```

**New `CreateBanRequest`** — replaces raw entity acceptance:

```java
public record CreateBanRequest(
    BanType banType, Long profileId, String email,
    String reason, LocalDate until
) {}
```

**New `TournamentStatsDto`** — server-side aggregate stats:

```java
public record TournamentStatsDto(
    int totalGames, int totalWins, int totalPrize,
    int totalBuyIn, int profitLoss, int bestFinish,
    double avgPlacement, double winRate
) {}
```

### Response Type Changes

| Endpoint | Before | After |
|----------|--------|-------|
| `GET /profiles/{id}` | Raw `OnlineProfile` entity | `PublicProfileResponse` |
| `GET /profiles/name/{name}` | Raw `OnlineProfile` entity | `PublicProfileResponse` |
| `GET /profiles/aliases` | `List<OnlineProfile>` | `List<PublicProfileResponse>` |
| `GET /auth/me` | `ProfileResponse` (old, sparse) | `ProfileResponse` (expanded) |
| `POST /auth/login` | `LoginResponse` (flat) | `LoginResponse` (wrapping `ProfileResponse`) |
| `POST /auth/register` | `LoginResponse` (flat) | `LoginResponse` (wrapping `ProfileResponse`) |
| `POST /admin/bans` | Raw entity fields | `CreateBanRequest` DTO |
| `GET /api/v1/history` | `{ history, total }` | `{ history, total, stats }` |

### Retire Alias Ownership

`ProfileController.retireProfile()` ownership check expanded: allow if the authenticated user's email matches the target profile's email (same person, different alias), not just same profile ID.

### Tournament History Stats

Extend `GET /api/v1/history` response to include a `stats` field computed from all matching records (not just the current page). Single aggregate query with the same WHERE clause as the paginated query:

```sql
SELECT COUNT(*), SUM(prize), SUM(buyin), MIN(placement),
       AVG(placement), COUNT(CASE WHEN placement = 1 THEN 1 END)
FROM ... WHERE <same filters>
```

## Frontend Changes

### API Client Consolidation

**`authApi`** — session lifecycle and authentication credentials:

- `login(credentials)` -> `POST /auth/login`
- `register(userData)` -> `POST /auth/register`
- `logout()` -> `POST /auth/logout`
- `getCurrentUser()` -> `GET /auth/me`
- `changePassword(oldPassword, newPassword)` -> `PUT /auth/password`
- `changeEmail(email)` -> `PUT /auth/email`
- `forgotPassword(email)` -> `POST /auth/forgot-password`
- `resetPassword(token, newPassword)` -> `POST /auth/reset-password`
- `verifyEmail(token)` -> `GET /auth/verify-email`
- `resendVerification()` -> `POST /auth/resend-verification`
- `checkUsername(username)` -> `GET /auth/check-username`
- `getWsToken()` -> `GET /auth/ws-token`

**`profileApi`** — user profile data (read + manage):

- `getProfile(id)` -> `GET /profiles/{id}`
- `getProfileByName(name)` -> `GET /profiles/name/{name}`
- `getAliases()` -> `GET /profiles/aliases`
- `retireProfile(id)` -> `POST /profiles/{id}/retire`

### Removed

- **`playerApi`** — deleted entirely. Methods move to `profileApi` or `authApi`.
- **`gameServerApi.register`** — deleted (duplicate of `authApi.register`).
- **`gameServerApi.getWsToken`** — moved to `authApi.getWsToken`.
- **`AuthResponse` type** — deleted, replaced by `LoginResponse` + `ProfileResponse`.
- **`BannedKeyDto` type** — replaced by `BanDto`.
- **`calculateTournamentStats()`** — deleted, replaced by server response.

`gameServerApi` retains all game-related methods (listGames, createGame, joinGame, etc.).

### Type Changes

```typescript
// Canonical "current user" -- matches backend ProfileResponse
interface ProfileResponse {
  id: number
  username: string
  email: string
  emailVerified: boolean
  admin: boolean
  retired: boolean
  createDate: string
}

// Login/register response -- wraps ProfileResponse
interface LoginResponse {
  success: boolean
  profile: ProfileResponse | null
  token: string | null
  message: string | null
  retryAfterSeconds: number | null
}

// Public profile -- for unauthenticated lookups
interface PublicProfileResponse {
  id: number
  name: string
  createDate: string
}

// Ban DTO -- replaces BannedKeyDto
interface BanDto {
  id: number
  banType: 'PROFILE' | 'EMAIL'
  profileId: number | null
  email: string | null
  reason: string | null
  until: string | null
  createdAt: string
}
```

### Request Body Fixes

| Call | Before (broken) | After (correct) |
|------|-----------------|-----------------|
| `resetPassword` | `{ token, password }` | `{ token, newPassword }` |
| `changePassword` | `{ currentPassword, newPassword }` | `{ oldPassword, newPassword }` |

### Page Updates

- **`/account`** — Use `authApi.changePassword` (`PUT /auth/password`), fix field names.
- **`/reset-password`** — Fix request body field: `password` -> `newPassword`.
- **`/online/myprofile`** — Use `authApi.changePassword`, update alias rendering to `PublicProfileResponse`.
- **`AliasManagement.tsx`** — Wire retire button to `profileApi.retireProfile(id)`, update `Alias` interface to include `id`.
- **`AuthContext`** — Use `ProfileResponse`/`LoginResponse` types. `checkAuthStatus` returns `ProfileResponse`. `login` unwraps `LoginResponse.profile`.
- **`/online/history`** — Drop `calculateTournamentStats()`, use `stats` from API response.
- **`/admin/ban-list`** — Update form to send `CreateBanRequest` fields. Update display to use `BanDto`.
- **`/admin/online-profile-search`** — No changes (admin views use separate admin endpoint shapes).
