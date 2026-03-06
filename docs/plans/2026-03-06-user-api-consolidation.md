# User API Consolidation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix broken user API endpoints, eliminate sensitive data leakage, consolidate redundant frontend clients, and add server-side tournament stats.

**Architecture:** Backend-first approach — fix DTOs and endpoints, then update frontend to match. Each task is independently testable and committable. Backend changes in pokergameprotocol (DTOs) and pokergameserver (controllers/services). Frontend changes in code/web/.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, JUnit 5/MockMvc, TypeScript, React/Next.js, Vitest

**Design doc:** `docs/plans/2026-03-06-user-api-consolidation-design.md`

---

## Task 1: Expand ProfileResponse DTO

Add `emailVerified`, `admin`, `createDate` fields to the canonical "current user" response.

**Files:**
- Modify: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/ProfileResponse.java`

**Step 1: Update the record**

```java
public record ProfileResponse(Long id, String username, String email,
        boolean emailVerified, boolean admin, boolean retired,
        String createDate) {
}
```

**Step 2: Fix all compilation errors**

The old signature was `(Long id, String username, String email, boolean retired)`. Every call site constructing a `ProfileResponse` must be updated. Key locations:
- `AuthService.getCurrentUser()` — `code/pokergameserver/src/main/java/.../service/AuthService.java:279`
- Any tests that construct `ProfileResponse` directly

**Step 3: Run tests**

```bash
cd code && mvn test -pl pokergameprotocol,pokergameserver -Dtest="AuthServiceTest,AuthControllerTest,AuthControllerProfileTest" -P fast
```

**Step 4: Commit**

```
feat(protocol): expand ProfileResponse with emailVerified, admin, createDate
```

---

## Task 2: Restructure LoginResponse to wrap ProfileResponse

Change `LoginResponse` from flat fields to wrapping a `ProfileResponse` object.

**Files:**
- Modify: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/LoginResponse.java`

**Step 1: Update the record**

```java
public record LoginResponse(boolean success, ProfileResponse profile,
        String token, String message, Long retryAfterSeconds) {
}
```

**Step 2: Fix all call sites in AuthService**

Every `new LoginResponse(...)` in `AuthService.java` must be updated. There are ~10 call sites across `register()` and `login()`. Pattern:

Failure responses (no profile):
```java
return new LoginResponse(false, null, null, "Error message", null);
```

Success responses (with profile):
```java
ProfileResponse profile = new ProfileResponse(p.getId(), p.getName(), p.getEmail(),
        p.isEmailVerified(), false, p.isRetired(), p.getCreateDate().toString());
String token = tokenProvider.generateToken(username, p.getId(), rememberMe, emailVerified);
return new LoginResponse(true, profile, token, null, null);
```

Locked responses (retryAfterSeconds):
```java
return new LoginResponse(false, null, null, "Account is locked", retryAfterSeconds);
```

**Step 3: Fix AuthController**

Update `register()` and `login()` — the cookie-setting logic reads `result.token()` which still works. The JSON assertions in tests change because the response shape changes from flat to nested.

**Step 4: Fix all tests**

Update `AuthControllerTest.java` — assertions change from `$.username` to `$.profile.username`, `$.profileId` to `$.profile.id`, etc. Update any test that constructs `LoginResponse` directly.

**Step 5: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest="AuthServiceTest,AuthControllerTest" -P fast
```

**Step 6: Commit**

```
feat(protocol): restructure LoginResponse to wrap ProfileResponse
```

---

## Task 3: Create PublicProfileResponse DTO

New DTO for unauthenticated profile lookups — prevents leaking sensitive entity fields.

**Files:**
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/PublicProfileResponse.java`

**Step 1: Create the record**

```java
package com.donohoedigital.games.poker.protocol.dto;

public record PublicProfileResponse(Long id, String name, String createDate) {
}
```

**Step 2: Commit**

```
feat(protocol): add PublicProfileResponse DTO for public profile lookups
```

---

## Task 4: Update ProfileController — return PublicProfileResponse, remove dead endpoints

Remove `PUT /{id}`, `DELETE /{id}`, `PUT /{id}/password`. Update remaining endpoints to return DTOs instead of raw entities.

**Files:**
- Modify: `code/pokergameserver/src/main/java/.../controller/ProfileController.java`
- Modify: `code/pokergameserver/src/test/java/.../controller/ProfileControllerTest.java`
- Delete or gut: `code/pokergameserver/src/test/java/.../controller/ProfileControllerPasswordTest.java`

**Step 1: Remove dead endpoints from ProfileController**

Delete these methods entirely:
- `updateProfile(Long id, UpdateProfileRequest request)` — email changes go through `/auth/email`
- `deleteProfile(Long id)` — retire via `POST /{id}/retire` only
- `changePassword(Long id, ChangePasswordRequest request)` — moves to AuthController

Remove imports for `UpdateProfileRequest`, `ChangePasswordRequest`, `DeleteMapping`, `ProfileService.InvalidPasswordException`. Remove the `ProfileService` dependency entirely (no remaining methods need it). The controller only needs `OnlineProfileRepository` and `ProfileService` (for `getProfile` used in aliases and retire).

Actually, keep `ProfileService` — `retireProfile` calls `profileService.deleteProfile(id)` and aliases uses `profileService.getProfile(profileId)`.

**Step 2: Update getProfile to return PublicProfileResponse**

```java
@GetMapping("/{id}")
public ResponseEntity<PublicProfileResponse> getProfile(@PathVariable("id") Long id) {
    OnlineProfile profile = profileService.getProfile(id);
    if (profile == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(toPublicProfile(profile));
}
```

**Step 3: Update getProfileByName to return PublicProfileResponse**

```java
@GetMapping("/name/{name}")
public ResponseEntity<PublicProfileResponse> getProfileByName(@PathVariable("name") String name) {
    OnlineProfile profile = profileRepository.findByName(name).orElse(null);
    if (profile == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(toPublicProfile(profile));
}
```

**Step 4: Update getAliases to return List<PublicProfileResponse>**

```java
@GetMapping("/aliases")
public ResponseEntity<List<PublicProfileResponse>> getAliases() {
    Long profileId = getAuthenticatedProfileId();
    OnlineProfile profile = profileService.getProfile(profileId);
    if (profile == null) {
        return ResponseEntity.notFound().build();
    }
    List<PublicProfileResponse> aliases = profileRepository
            .findByEmailExcludingName(profile.getEmail(), profile.getName())
            .stream().map(this::toPublicProfile).toList();
    return ResponseEntity.ok(aliases);
}
```

**Step 5: Add helper method**

```java
private PublicProfileResponse toPublicProfile(OnlineProfile p) {
    return new PublicProfileResponse(p.getId(), p.getName(),
            p.getCreateDate() != null ? p.getCreateDate().toString() : null);
}
```

**Step 6: Update ProfileControllerTest**

- Remove `testUpdateProfile`, `testUpdateProfileForbidden`, `testDeleteProfile`, `testDeleteProfileForbidden`
- Update `testGetProfile` assertions: `$.name` instead of `$.email`, no email in response
- Delete `ProfileControllerPasswordTest.java` entirely (endpoint moved)

**Step 7: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest="ProfileControllerTest,ProfileControllerNewEndpointsTest" -P fast
```

**Step 8: Commit**

```
refactor(server): return PublicProfileResponse from profile endpoints, remove dead endpoints
```

---

## Task 5: Expand retire alias ownership check

Allow retiring aliases that share the authenticated user's email, not just same profile ID.

**Files:**
- Modify: `code/pokergameserver/src/main/java/.../controller/ProfileController.java`
- Modify: `code/pokergameserver/src/test/java/.../controller/ProfileControllerNewEndpointsTest.java`

**Step 1: Update retireProfile ownership check**

```java
@PostMapping("/{id}/retire")
public ResponseEntity<Map<String, Object>> retireProfile(@PathVariable("id") Long id) {
    Long authenticatedProfileId = getAuthenticatedProfileId();
    OnlineProfile authenticatedProfile = profileService.getProfile(authenticatedProfileId);
    OnlineProfile targetProfile = profileService.getProfile(id);

    if (authenticatedProfile == null || targetProfile == null) {
        return ResponseEntity.notFound().build();
    }

    // Allow if same profile OR same email (alias)
    boolean isOwner = authenticatedProfileId.equals(id);
    boolean isSameEmailAlias = authenticatedProfile.getEmail().equalsIgnoreCase(targetProfile.getEmail());
    if (!isOwner && !isSameEmailAlias) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    boolean success = profileService.deleteProfile(id);
    if (!success) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(Map.of("success", true, "message", "Profile retired successfully"));
}
```

**Step 2: Add test for alias retire**

In `ProfileControllerNewEndpointsTest.java`, add a test that verifies retiring an alias with the same email succeeds.

**Step 3: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest="ProfileControllerNewEndpointsTest" -P fast
```

**Step 4: Commit**

```
feat(server): allow retiring aliases with same email
```

---

## Task 6: Move change-password to AuthController

Add `PUT /auth/password` endpoint, extracting profile ID from JWT.

**Files:**
- Modify: `code/pokergameserver/src/main/java/.../controller/AuthController.java`
- Modify: `code/pokergameserver/src/test/java/.../controller/AuthControllerTest.java`

**Step 1: Add ProfileService dependency to AuthController**

```java
private final ProfileService profileService;

public AuthController(AuthService authService, ProfileService profileService,
        JwtProperties jwtProperties, Environment environment) {
    this.authService = authService;
    this.profileService = profileService;
    this.cookieName = jwtProperties.getCookieName();
    this.environment = environment;
}
```

**Step 2: Add change-password endpoint**

```java
@PutMapping("/password")
public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
    if (isBlank(request.oldPassword())) {
        return ResponseEntity.badRequest().build();
    }
    if (isBlank(request.newPassword()) || request.newPassword().length() < 8
            || request.newPassword().length() > 128) {
        return ResponseEntity.badRequest().build();
    }
    Long profileId = getAuthenticatedProfileId();
    try {
        profileService.changePassword(profileId, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    } catch (ProfileService.InvalidPasswordException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
```

Add import for `ChangePasswordRequest` and `ProfileService`.

**Step 3: Add tests**

Add to `AuthControllerTest.java` (or a new `AuthControllerPasswordTest.java`):

```java
@Test
void changePassword_success_returns200() throws Exception {
    doNothing().when(profileService).changePassword(eq(1L), eq("oldpass"), eq("newpass123"));

    mockMvc.perform(put("/api/v1/auth/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"oldPassword\":\"oldpass\",\"newPassword\":\"newpass123\"}"))
            .andExpect(status().isOk());
}

@Test
void changePassword_wrongOldPassword_returns403() throws Exception {
    doThrow(new ProfileService.InvalidPasswordException())
            .when(profileService).changePassword(eq(1L), eq("wrongpass"), eq("newpass123"));

    mockMvc.perform(put("/api/v1/auth/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"oldPassword\":\"wrongpass\",\"newPassword\":\"newpass123\"}"))
            .andExpect(status().isForbidden());
}

@Test
void changePassword_shortPassword_returns400() throws Exception {
    mockMvc.perform(put("/api/v1/auth/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"oldPassword\":\"oldpass\",\"newPassword\":\"short\"}"))
            .andExpect(status().isBadRequest());
}
```

Mock `ProfileService` in the test class: `@MockitoBean private ProfileService profileService;`

**Step 4: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest="AuthControllerTest" -P fast
```

**Step 5: Commit**

```
feat(server): add PUT /auth/password endpoint
```

---

## Task 7: Create CreateBanRequest DTO and update AdminController

Replace raw `BanEntity` acceptance with a proper request DTO.

**Files:**
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/CreateBanRequest.java`
- Modify: `code/pokergameserver/src/main/java/.../controller/AdminController.java`
- Modify: `code/pokergameserver/src/test/java/.../controller/AdminControllerTest.java`

**Step 1: Create the DTO**

```java
package com.donohoedigital.games.poker.protocol.dto;

import java.time.LocalDate;
import com.donohoedigital.games.poker.gameserver.persistence.entity.BanEntity;

public record CreateBanRequest(BanEntity.BanType banType, Long profileId, String email,
        String reason, LocalDate until) {
}
```

Note: `BanType` is defined inside `BanEntity`. If that creates a circular module dependency (protocol -> gameserver), move the `BanType` enum to the protocol module as a standalone enum. Check module boundaries.

Actually, `pokergameprotocol` cannot depend on `pokergameserver` (entity module). Move `BanType` to its own enum in the protocol module:

```java
package com.donohoedigital.games.poker.protocol.dto;

public enum BanType {
    PROFILE, EMAIL
}
```

Then update `BanEntity` to use `com.donohoedigital.games.poker.protocol.dto.BanType` (or keep a separate copy — check if pokergameserver depends on pokergameprotocol). If so, `BanEntity.BanType` can be replaced by the protocol one. If not, use a String in the DTO and convert in the controller.

Simplest approach: use a String field in the DTO and validate/convert in the controller:

```java
package com.donohoedigital.games.poker.protocol.dto;

import java.time.LocalDate;

public record CreateBanRequest(String banType, Long profileId, String email,
        String reason, LocalDate until) {
}
```

**Step 2: Update AdminController.addBan()**

```java
@PostMapping("/bans")
public ResponseEntity<BanEntity> addBan(@RequestBody CreateBanRequest request) {
    BanEntity.BanType banType;
    try {
        banType = BanEntity.BanType.valueOf(request.banType());
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
    }

    if (banType == BanEntity.BanType.PROFILE && request.profileId() == null) {
        return ResponseEntity.badRequest().build();
    }
    if (banType == BanEntity.BanType.EMAIL && (request.email() == null || request.email().isBlank())) {
        return ResponseEntity.badRequest().build();
    }

    BanEntity ban = new BanEntity();
    ban.setBanType(banType);
    ban.setProfileId(request.profileId());
    ban.setEmail(request.email());
    ban.setReason(request.reason());
    ban.setUntil(request.until());

    BanEntity saved = banRepository.save(ban);
    return ResponseEntity.ok(saved);
}
```

Add import for `CreateBanRequest`.

**Step 3: Update AdminControllerTest**

Update `addBan_returnsCreated()` test to send the new DTO shape:
```java
.content("{\"banType\":\"PROFILE\",\"profileId\":1,\"reason\":\"test\",\"until\":\"2099-12-31\"}")
```

**Step 4: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest="AdminControllerTest" -P fast
```

**Step 5: Commit**

```
feat(server): add CreateBanRequest DTO, validate ban creation input
```

---

## Task 8: Add server-side tournament aggregate stats

Add `TournamentStatsDto` and a repository aggregate query. Update `HistoryController` to return stats.

**Files:**
- Create: `code/pokergameprotocol/src/main/java/com/donohoedigital/games/poker/protocol/dto/TournamentStatsDto.java`
- Modify: `code/pokergameserver/src/main/java/.../persistence/repository/TournamentHistoryRepository.java`
- Modify: `code/pokergameserver/src/main/java/.../controller/HistoryController.java`
- Modify: `code/pokergameserver/src/test/java/.../controller/HistoryControllerTest.java`

**Step 1: Create the DTO**

```java
package com.donohoedigital.games.poker.protocol.dto;

public record TournamentStatsDto(int totalGames, int totalWins, int totalPrize,
        int totalBuyIn, int profitLoss, int bestFinish,
        double avgPlacement, double winRate) {
}
```

**Step 2: Add aggregate query to TournamentHistoryRepository**

The `TournamentHistory` entity has `getPlace()`, `getPrize()`, `getBuyin()`. Add a query that returns raw aggregate values. Since JPQL doesn't easily map to a record, use `Object[]`:

```java
@Query("SELECT COUNT(t), "
        + "SUM(CASE WHEN t.place = 1 THEN 1 ELSE 0 END), "
        + "COALESCE(SUM(t.prize), 0), "
        + "COALESCE(SUM(t.buyin), 0), "
        + "COALESCE(MIN(t.place), 0), "
        + "COALESCE(AVG(t.place), 0) "
        + "FROM TournamentHistory t WHERE t.profile.id = :profileId"
        + " AND (:from IS NULL OR t.endDate >= :from)"
        + " AND (:to IS NULL OR t.endDate <= :to)")
Object[] aggregateStats(@Param("profileId") Long profileId,
        @Param("from") Date from, @Param("to") Date to);
```

Note: Verify exact field names by checking `TournamentHistory` entity (the column getters are `getPlace()`, `getPrize()`, `getBuyin()`). The JPQL field names should match the Java property names: `place`, `prize`, `buyin`.

**Step 3: Create a wrapper response record for history endpoint**

```java
package com.donohoedigital.games.poker.protocol.dto;

import java.util.List;

public record TournamentHistoryResponse(List<?> content, long totalElements,
        int totalPages, int number, int size, TournamentStatsDto stats) {
}
```

Or simpler: just build a `Map` in the controller with `content`, `totalElements`, `totalPages`, `stats` keys to match the existing Page JSON shape plus `stats`.

**Step 4: Update HistoryController.getHistory()**

```java
@GetMapping("/history")
public ResponseEntity<Map<String, Object>> getHistory(@RequestParam("name") String name,
        @RequestParam(name = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
        @RequestParam(name = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "pageSize", defaultValue = "50") int pageSize) {

    OnlineProfile profile = profileRepository.findByName(name).orElse(null);
    if (profile == null) {
        return ResponseEntity.notFound().build();
    }

    Page<TournamentHistory> history = historyRepository.findByProfileId(profile.getId(), from, to,
            PageRequest.of(page, pageSize));

    Object[] agg = historyRepository.aggregateStats(profile.getId(), from, to);
    long totalGames = ((Number) agg[0]).longValue();
    long totalWins = ((Number) agg[1]).longValue();
    int totalPrize = ((Number) agg[2]).intValue();
    int totalBuyIn = ((Number) agg[3]).intValue();
    int bestFinish = ((Number) agg[4]).intValue();
    double avgPlacement = ((Number) agg[5]).doubleValue();
    double winRate = totalGames > 0 ? (totalWins * 100.0 / totalGames) : 0;

    TournamentStatsDto stats = new TournamentStatsDto(
            (int) totalGames, (int) totalWins, totalPrize, totalBuyIn,
            totalPrize - totalBuyIn, bestFinish, avgPlacement, winRate);

    Map<String, Object> response = new java.util.LinkedHashMap<>();
    response.put("content", history.getContent());
    response.put("totalElements", history.getTotalElements());
    response.put("totalPages", history.getTotalPages());
    response.put("number", history.getNumber());
    response.put("size", history.getSize());
    response.put("stats", stats);

    return ResponseEntity.ok(response);
}
```

Add import for `TournamentStatsDto` and `Map`.

**Step 5: Update HistoryControllerTest**

Update `getHistory_playerFound_returnsPage` to also mock `aggregateStats` and assert `$.stats`:

```java
when(historyRepository.aggregateStats(eq(1L), isNull(), isNull()))
        .thenReturn(new Object[]{1L, 1L, 1000, 100, 1, 1.0});

mockMvc.perform(get("/api/v1/history").param("name", "player1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(10))
        .andExpect(jsonPath("$.stats.totalGames").value(1))
        .andExpect(jsonPath("$.stats.totalWins").value(1));
```

**Step 6: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest="HistoryControllerTest" -P fast
```

**Step 7: Commit**

```
feat(server): add server-side tournament aggregate stats to history endpoint
```

---

## Task 9: Remove ProfileService.updateProfile (dead code)

Now that `PUT /profiles/{id}` is removed, the `updateProfile` method in `ProfileService` is unused.

**Files:**
- Modify: `code/pokergameserver/src/main/java/.../service/ProfileService.java`
- Modify: `code/pokergameserver/src/test/java/.../service/ProfileServiceTest.java`

**Step 1: Remove `updateProfile` method from ProfileService**

Delete the method and its test (`testUpdateProfileEmail`, `testUpdateProfileNotFound`).

**Step 2: Run tests**

```bash
cd code && mvn test -pl pokergameserver -P fast
```

**Step 3: Commit**

```
refactor(server): remove unused ProfileService.updateProfile
```

---

## Task 10: Run full backend test suite

Verify everything compiles and passes before moving to frontend.

**Step 1: Run full suite**

```bash
cd code && mvn test -pl pokergameprotocol,pokergameserver -P fast
```

**Step 2: Fix any failures, commit if needed**

---

## Task 11: Update frontend types

Replace `AuthResponse`, `BannedKeyDto` with new types matching backend DTOs.

**Files:**
- Modify: `code/web/lib/types.ts`

**Step 1: Replace types**

Delete `AuthResponse`. Add/replace:

```typescript
export interface ProfileResponse {
  id: number
  username: string
  email: string
  emailVerified: boolean
  admin: boolean
  retired: boolean
  createDate: string
}

export interface LoginResponse {
  success: boolean
  profile: ProfileResponse | null
  token: string | null
  message: string | null
  retryAfterSeconds: number | null
}

export interface PublicProfileResponse {
  id: number
  name: string
  createDate: string
}

export interface BanDto {
  id: number
  banType: 'PROFILE' | 'EMAIL'
  profileId: number | null
  email: string | null
  reason: string | null
  until: string | null
  createdAt: string
}

export interface TournamentStatsDto {
  totalGames: number
  totalWins: number
  totalPrize: number
  totalBuyIn: number
  profitLoss: number
  bestFinish: number
  avgPlacement: number
  winRate: number
}
```

Remove `AuthResponse` and `BannedKeyDto` interfaces. Keep `PlayerProfile` if still used elsewhere, but update references.

**Step 2: Commit**

```
feat(web): update frontend types to match new backend DTOs
```

---

## Task 12: Consolidate frontend API clients

Rewrite `authApi` and `profileApi`, delete `playerApi`, clean `gameServerApi`.

**Files:**
- Modify: `code/web/lib/api.ts`

**Step 1: Update authApi**

Update method signatures and request bodies:

```typescript
export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiFetch<LoginResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    })
    return response.data
  },

  register: async (userData: RegisterRequest): Promise<LoginResponse> => {
    const response = await apiFetch<LoginResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    })
    return response.data
  },

  logout: async (): Promise<void> => {
    await apiFetch<void>('/api/v1/auth/logout', { method: 'POST' })
  },

  getCurrentUser: async (): Promise<ProfileResponse | null> => {
    try {
      const response = await apiFetch<ProfileResponse>('/api/v1/auth/me')
      return response.data
    } catch {
      return null
    }
  },

  changePassword: (oldPassword: string, newPassword: string): Promise<Response> =>
    apiFetchRaw('/api/v1/auth/password', {
      method: 'PUT',
      body: JSON.stringify({ oldPassword, newPassword }),
    }),

  changeEmail: (email: string): Promise<Response> =>
    apiFetchRaw('/api/v1/auth/email', {
      method: 'PUT',
      body: JSON.stringify({ email }),
    }),

  forgotPassword: async (email: string): Promise<{ success: boolean; message: string }> => {
    const response = await apiFetch<{ success: boolean; message: string }>('/api/v1/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    })
    return response.data
  },

  resetPassword: (token: string, newPassword: string): Promise<Response> =>
    apiFetchRaw('/api/v1/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ token, newPassword }),
    }),

  verifyEmail: (token: string): Promise<Response> =>
    apiFetchRaw(`/api/v1/auth/verify-email?token=${encodeURIComponent(token)}`),

  resendVerification: (): Promise<Response> =>
    apiFetchRaw('/api/v1/auth/resend-verification', { method: 'POST' }),

  checkUsername: (username: string): Promise<Response> =>
    apiFetchRaw(`/api/v1/auth/check-username?username=${encodeURIComponent(username)}`),

  getWsToken: async (): Promise<WsTokenResponseDto> => {
    const response = await apiFetch<WsTokenResponseDto>('/api/v1/auth/ws-token')
    return response.data
  },
}
```

**Step 2: Update profileApi**

```typescript
export const profileApi = {
  getProfile: async (id: number): Promise<PublicProfileResponse> => {
    const response = await apiFetch<PublicProfileResponse>(`/api/v1/profiles/${id}`)
    return response.data
  },

  getProfileByName: async (name: string): Promise<PublicProfileResponse> => {
    const response = await apiFetch<PublicProfileResponse>(`/api/v1/profiles/name/${name}`)
    return response.data
  },

  getAliases: async (): Promise<PublicProfileResponse[]> => {
    const response = await apiFetch<PublicProfileResponse[]>('/api/v1/profiles/aliases')
    return response.data
  },

  retireProfile: async (id: number): Promise<void> => {
    await apiFetch<void>(`/api/v1/profiles/${id}/retire`, { method: 'POST' })
  },
}
```

**Step 3: Delete playerApi entirely**

Remove the entire `playerApi` object.

**Step 4: Clean gameServerApi**

Remove `register` and `getWsToken` methods from `gameServerApi`. Keep all game methods.

**Step 5: Update adminApi**

Update `getBans` and `addBan` to use new types:

```typescript
getBans: async (): Promise<{ bans: BanDto[]; total: number }> => {
  const response = await apiFetch<BanDto[]>('/api/v1/admin/bans')
  return { bans: response.data, total: response.data.length }
},

addBan: async (banData: {
  banType: 'PROFILE' | 'EMAIL'
  profileId?: number
  email?: string
  reason?: string
  until?: string
}): Promise<BanDto> => {
  const response = await apiFetch<BanDto>('/api/v1/admin/bans', {
    method: 'POST',
    body: JSON.stringify(banData),
  })
  return response.data
},

removeBan: async (id: number): Promise<void> => {
  await apiFetch<void>(`/api/v1/admin/bans/${id}`, { method: 'DELETE' })
},
```

**Step 6: Update imports in types**

Add `ProfileResponse`, `LoginResponse`, `PublicProfileResponse`, `BanDto` to the import block at the top of `api.ts`. Remove `AuthResponse`, `PlayerProfile`, `BannedKeyDto`.

**Step 7: Commit**

```
refactor(web): consolidate API clients — delete playerApi, fix endpoints and field names
```

---

## Task 13: Update AuthContext to use new types

**Files:**
- Modify: `code/web/lib/auth/AuthContext.tsx`
- Modify: `code/web/lib/auth/__tests__/AuthContext.test.tsx`

**Step 1: Update AuthContext**

Replace `AuthResponse` import with `LoginResponse`, `ProfileResponse`. Update `checkAuthStatus`:

```typescript
const checkAuthStatus = useCallback(async () => {
  setState((prev) => ({ ...prev, isLoading: true, error: null }))
  try {
    const storedUser = getAuthUser()
    if (storedUser) {
      const profile = await authApi.getCurrentUser()
      if (profile) {
        setState({
          user: {
            username: profile.username,
            email: profile.email ?? '',
            isAdmin: profile.admin || false,
            emailVerified: profile.emailVerified ?? false,
          },
          isAuthenticated: true,
          isLoading: false,
          error: null,
        })
        return
      }
    }
    clearAuthUser()
    setState({ user: null, isAuthenticated: false, isLoading: false, error: null })
  } catch {
    clearAuthUser()
    setState({ user: null, isAuthenticated: false, isLoading: false, error: null })
  }
}, [])
```

Update `login` to unwrap `LoginResponse.profile`:

```typescript
const response = await authApi.login({ username, password, rememberMe })
if (response.success && response.profile) {
  const p = response.profile
  setAuthUser({ username: p.username }, rememberMe)
  const user: AuthUser = {
    username: p.username,
    email: p.email ?? '',
    isAdmin: p.admin || false,
    emailVerified: p.emailVerified ?? false,
  }
  setState({ user, isAuthenticated: true, isLoading: false, error: null })
  return { success: true, emailVerified: p.emailVerified }
} else {
  setState((prev) => ({ ...prev, isLoading: false, error: response.message || 'Login failed' }))
  return { success: false }
}
```

**Step 2: Update AuthContext tests**

Update mock returns to use new `LoginResponse` shape with nested `profile`:

```typescript
vi.mocked(authApi.login).mockResolvedValue({
  success: true,
  profile: { id: 1, username: 'testuser', email: 'test@test.com', emailVerified: true, admin: false, retired: false, createDate: '2026-01-01' },
  token: 'jwt',
  message: null,
  retryAfterSeconds: null,
})
```

Update `getCurrentUser` mock to return `ProfileResponse` directly (no `success` field):

```typescript
vi.mocked(authApi.getCurrentUser).mockResolvedValue({
  id: 1, username: 'testuser', email: 'test@test.com',
  emailVerified: true, admin: false, retired: false, createDate: '2026-01-01',
})
```

**Step 3: Run tests**

```bash
cd code/web && npx vitest run lib/auth/
```

**Step 4: Commit**

```
refactor(web): update AuthContext to use LoginResponse/ProfileResponse types
```

---

## Task 14: Update page components

Fix all pages that consume the changed APIs.

**Files:**
- Modify: `code/web/app/account/page.tsx`
- Modify: `code/web/app/reset-password/page.tsx`
- Modify: `code/web/app/register/page.tsx`
- Modify: `code/web/components/profile/PasswordChangeForm.tsx`
- Modify: `code/web/app/online/myprofile/page.tsx`
- Modify: `code/web/components/profile/AliasManagement.tsx`

**Step 1: Fix /account page**

`authApi.changePassword` already calls the correct method — just ensure the method in `authApi` now hits `PUT /auth/password` with `{ oldPassword, newPassword }`. The `account/page.tsx` sends `(currentPassword, newPassword)` — update the call:

```typescript
const res = await authApi.changePassword(currentPassword, newPassword);
```

This already matches the new `authApi.changePassword(oldPassword, newPassword)` signature since the param names in the call site are just local variable names.

**Step 2: Fix /reset-password page**

Find where it calls `authApi.resetPassword(token, password)` and change to:

```typescript
const res = await authApi.resetPassword(token, newPassword)
```

Ensure the variable name is `newPassword` (or rename the local).

**Step 3: Fix PasswordChangeForm**

Change from `playerApi.changePassword` to `authApi.changePassword`:

```typescript
import { authApi } from '@/lib/api'
// ...
const res = await authApi.changePassword(currentPassword, newPassword)
```

Remove `playerApi` import.

**Step 4: Fix /online/myprofile page**

Update alias mapping to use `PublicProfileResponse` shape:

```typescript
const data = await profileApi.getAliases()
const aliases = data.map((p) => ({
  id: p.id,
  name: p.name,
  createdDate: p.createDate,
}))
```

**Step 5: Fix AliasManagement component**

Update `Alias` interface to include `id`:

```typescript
interface Alias {
  id: number
  name: string
  createdDate: string
  retiredDate?: string
}
```

Wire the retire function:

```typescript
import { profileApi } from '@/lib/api'

const confirmRetire = async () => {
  const alias = aliases.find(a => a.name === dialogState.aliasName)
  if (!alias) return

  setIsLoading(true)
  setMessage(null)
  try {
    await profileApi.retireProfile(alias.id)
    setMessage({ type: 'success', text: `Alias "${alias.name}" retired successfully` })
  } catch (error) {
    setMessage({
      type: 'error',
      text: error instanceof Error ? error.message : 'Failed to retire alias',
    })
  } finally {
    setIsLoading(false)
  }
}
```

**Step 6: Fix /register page**

Update to handle nested `LoginResponse.profile`:

```typescript
const response = await authApi.register({ username, email, password })
if (response.success) {
  router.push('/verify-email-pending')
} else {
  setError(response.message || 'Registration failed')
}
```

**Step 7: Commit**

```
fix(web): update all pages to use consolidated API clients and correct field names
```

---

## Task 15: Update tournament history page — use server-side stats

**Files:**
- Modify: `code/web/app/online/history/page.tsx`
- Modify: `code/web/lib/mappers.ts`
- Modify: `code/web/lib/api.ts` (tournamentApi return type)

**Step 1: Update tournamentApi.getHistory return type**

```typescript
getHistory: async (
  playerName: string, page = 0, pageSize = 50, from?: string, to?: string
): Promise<{ content: TournamentHistoryDto[]; totalElements: number; stats: TournamentStatsDto }> => {
  const params = new URLSearchParams({
    name: playerName, page: page.toString(), pageSize: pageSize.toString(),
  })
  if (from) params.append('from', from)
  if (to) params.append('to', to)
  const response = await apiFetch<{ content: TournamentHistoryDto[]; totalElements: number; stats: TournamentStatsDto }>(
    `/api/v1/history?${params}`
  )
  return response.data
},
```

Note the response shape changed from Spring's `Page` JSON (`content`, `totalElements`) — verify this matches the new `Map` response from Task 8.

**Step 2: Update history page**

Replace `calculateTournamentStats` usage:

```typescript
const { content, totalElements, stats } = await tournamentApi.getHistory(
  playerName, backendPage, 50, filters.begin, filters.end
)
const mapped = content.map(mapTournamentEntry)
const result = buildPaginationResult(mapped, totalElements, page, 50)
return {
  entries: result.data,
  stats,
  totalPages: result.totalPages,
  totalItems: result.totalItems,
}
```

Remove `calculateTournamentStats` import.

**Step 3: Clean up mappers.ts**

Remove `calculateTournamentStats` function from `mappers.ts` (now dead code). Keep `mapLeaderboardEntry` and `mapTournamentEntry`.

**Step 4: Run tests**

```bash
cd code/web && npx vitest run
```

**Step 5: Commit**

```
feat(web): use server-side tournament stats, remove client-side calculation
```

---

## Task 16: Update ban list page

**Files:**
- Modify: `code/web/app/admin/ban-list/page.tsx`

**Step 1: Update types and form**

Replace `BannedKeyDto` / `BannedKey` with `BanDto`. Update the add-ban form to send `banType`, `profileId`/`email`, `reason`, `until` instead of `key`, `comment`, `until`.

Update the form state:
```typescript
const [banType, setBanType] = useState<'PROFILE' | 'EMAIL'>('EMAIL')
const [banTarget, setBanTarget] = useState('')  // profileId or email
const [banReason, setBanReason] = useState('')
const [banUntil, setBanUntil] = useState('')
```

Update the submit handler:
```typescript
await adminApi.addBan({
  banType,
  ...(banType === 'PROFILE' ? { profileId: parseInt(banTarget) } : { email: banTarget }),
  reason: banReason || undefined,
  until: banUntil || undefined,
})
```

Update the table columns to display `banType`, `email`/`profileId`, `reason` instead of `key`, `comment`.

Update `removeBan` to use numeric ID: `adminApi.removeBan(ban.id)`.

**Step 2: Run tests**

```bash
cd code/web && npx vitest run app/admin/ban-list/
```

**Step 3: Commit**

```
fix(web): align ban list page with CreateBanRequest DTO
```

---

## Task 17: Update and fix all frontend tests

**Files:**
- Modify: `code/web/lib/__tests__/api.test.ts`
- Modify: `code/web/lib/__tests__/api-clients.test.ts`
- Modify: `code/web/app/account/__tests__/page.test.tsx`
- Modify: `code/web/app/reset-password/__tests__/page.test.tsx`
- Modify: `code/web/components/profile/__tests__/PasswordChangeForm.test.tsx`
- Modify: `code/web/components/profile/__tests__/AliasManagement.test.tsx`
- Modify: `code/web/lib/__tests__/mappers.test.ts`

**Step 1: Fix api.test.ts**

Remove tests for `playerApi` methods. Update `authApi.login` and `authApi.register` assertions to expect new `LoginResponse` shape. Update `authApi.changePassword` test to hit `/api/v1/auth/password`. Remove `gameServerApi.register` and `gameServerApi.getWsToken` tests.

**Step 2: Fix account page test**

Update mock for `authApi.changePassword` — it now returns a `Response` object from `apiFetchRaw`.

**Step 3: Fix reset-password page test**

Update to send `{ token, newPassword }` instead of `{ token, password }`.

**Step 4: Fix PasswordChangeForm test**

Change mock from `playerApi.changePassword` to `authApi.changePassword`.

**Step 5: Fix AliasManagement test**

Add mock for `profileApi.retireProfile` now that the retire button makes a real API call.

**Step 6: Fix mappers test**

Remove tests for `calculateTournamentStats` if they exist.

**Step 7: Run all frontend tests**

```bash
cd code/web && npx vitest run
```

**Step 8: Commit**

```
test(web): update all frontend tests for API consolidation
```

---

## Task 18: Final verification and cleanup

**Step 1: Search for dead references**

Search the codebase for any remaining references to:
- `playerApi`
- `gameServerApi.register`
- `gameServerApi.getWsToken`
- `AuthResponse` (the old type)
- `BannedKeyDto`
- `calculateTournamentStats`

Fix any found.

**Step 2: Run full test suites**

```bash
cd code && mvn test -P fast
cd code/web && npx vitest run
```

**Step 3: Commit any cleanup**

```
chore: remove dead references from API consolidation
```

**Step 4: Update design doc status**

Change status in `docs/plans/2026-03-06-user-api-consolidation-design.md` to `COMPLETED (YYYY-MM-DD)`.
