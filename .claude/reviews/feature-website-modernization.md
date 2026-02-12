# Review Request: Phase 1 - Spring Boot REST API

## Review Request

**Branch:** feature-website-modernization
**Worktree:** ../DDPoker-feature-website-modernization
**Plan:** .claude/plans/WEBSITE-MODERNIZATION-PLAN.md (Phase 1)
**Requested:** 2026-02-12 03:30

## Summary

Built complete Spring Boot REST API to replace Apache Wicket web frontend. Created 9 REST controllers with JWT authentication, wrapping all existing services (OnlineProfileService, OnlineGameService, TournamentHistoryService, BannedKeyService). Zero code duplication - reuses entire service layer via Spring XML context imports. Provides full API coverage for games, leaderboard, profiles, history, search, admin, downloads, and RSS feeds.

## Commits

- `8bc6211` - feat: Add Spring Boot REST API foundation (JWT security, auth controller)
- `27585cb` - feat: Complete Spring Boot REST API core (game, profile controllers, fixed versions)
- `a8b4e81` - docs: Update Phase 1 status - core complete
- `a750ef1` - feat: Complete all Phase 1 REST API controllers (leaderboard, history, search, admin, downloads, RSS)
- `1e113c7` - docs: Update Phase 1 complete status

## Files Changed

### New Module Structure
- [x] code/pom.xml - Added `api` module to parent POM
- [x] code/api/pom.xml - Spring Boot 3.2.2, JWT dependencies, version 3.3.0-CommunityEdition

### Core Application (3 files)
- [x] code/api/src/main/java/com/donohoedigital/poker/api/ApiApplication.java - Spring Boot main, imports app-context-pokerserver.xml
- [x] code/api/src/main/resources/application.properties - JWT config, logging

### Security Infrastructure (3 files)
- [x] code/api/src/main/java/com/donohoedigital/poker/api/security/JwtTokenProvider.java - Token generation/validation, HMAC SHA-256
- [x] code/api/src/main/java/com/donohoedigital/poker/api/security/JwtAuthFilter.java - Extract JWT from cookies, set Spring Security context
- [x] code/api/src/main/java/com/donohoedigital/poker/api/config/SecurityConfig.java - Endpoint protection, role-based access, CORS

### Controllers (9 files)
- [x] code/api/src/main/java/com/donohoedigital/poker/api/controller/AuthController.java - POST /api/auth/login, /logout, GET /me
- [x] code/api/src/main/java/com/donohoedigital/poker/api/controller/GameController.java - GET /api/games, /games/{id}, /games/hosts
- [x] code/api/src/main/java/com/donohoedigital/poker/api/controller/ProfileController.java - GET /api/profile, PUT /password, GET /aliases, POST /retire
- [x] code/api/src/main/java/com/donohoedigital/poker/api/controller/LeaderboardController.java - GET /api/leaderboard (DDR1/ROI modes)
- [x] code/api/src/main/java/com/donohoedigital/poker/api/controller/HistoryController.java - GET /api/history (player tournament history)
- [x] code/api/src/main/java/com/donohoedigital/poker/api/controller/SearchController.java - GET /api/search (player name search)
- [x] code/api/src/main/java/com/donohoedigital/poker/api/controller/AdminController.java - GET/POST/DELETE /api/admin/bans, GET /admin/profiles
- [x] code/api/src/main/java/com/donohoedigital/poker/api/controller/DownloadController.java - GET /api/downloads/{filename}
- [x] code/api/src/main/java/com/donohoedigital/poker/api/controller/RssController.java - GET /api/rss/{mode}

### DTOs (3 files)
- [x] code/api/src/main/java/com/donohoedigital/poker/api/dto/LoginRequest.java - Username, password, rememberMe
- [x] code/api/src/main/java/com/donohoedigital/poker/api/dto/AuthResponse.java - Success, message, username, admin flag
- [x] code/api/src/main/java/com/donohoedigital/poker/api/dto/GameListResponse.java - Games list with pagination

### Documentation (1 file)
- [x] .claude/plans/WEBSITE-MODERNIZATION-STATUS.md - Full status tracking document

**Total:** 20 new files, ~2,100 lines

**Privacy Check:**
- ✅ SAFE - No private information. All code is configuration and API wrappers.

## Verification Results

- **Tests:** Skipped per user directive (focus on implementation speed)
- **Coverage:** N/A (no tests written)
- **Build:** ✅ BUILD SUCCESS - compiles cleanly with all dependencies
- **Compilation:** ✅ `mvn clean compile -pl api -am` passes

## Context & Decisions

### Key Architectural Decisions

1. **JWT in HttpOnly Cookies**
   - More secure than localStorage (XSS-resistant)
   - Automatic transmission with requests
   - Matches existing "remember me" pattern from Wicket

2. **Reuse ALL Existing Services**
   - ApiApplication imports `app-context-pokerserver.xml`
   - Gets all services via component scanning (`@Service` annotations)
   - Zero database/service logic duplication
   - OnlineProfileService, OnlineGameService, TournamentHistoryService, BannedKeyService all autowired

3. **Version Compatibility**
   - API module version: 3.3.0-CommunityEdition (matches parent)
   - Initially used version 3.0 which caused dependency resolution failures
   - Fixed by matching parent version exactly

4. **Admin Detection**
   - Uses existing `settings.admin.user` property from PropertyConfig
   - Matches Wicket implementation (PokerUser.isAdmin())
   - JWT contains "ROLE_ADMIN" claim for admin users

5. **Spring Boot 3.2.2**
   - Latest stable Spring Boot
   - Uses Jakarta EE 10 (jakarta.* packages match existing code)
   - Compatible with existing Spring XML config via @ImportResource

6. **Security Configuration**
   - Public endpoints: /api/auth/**, /api/games/**, /api/leaderboard/**, etc.
   - Protected: /api/profile/** (requires authentication)
   - Admin-only: /api/admin/** (requires ROLE_ADMIN)
   - CORS: Allows all origins for development (TODO: restrict in production)

7. **Service Method Discovery**
   - BannedKey uses `key` field (String), not separate name/email/licenseKey
   - BannedKey.deleteBannedKey() takes String key, not Long id
   - TournamentHistoryService uses LeaderboardType enum (ddr1, roi)
   - OnlineGameList extends PagedList<OnlineGame> which extends ArrayList
   - OnlineGame has getHostPlayer(), getUrl() (not getName(), getHostName())

8. **Download Security**
   - Path traversal protection in DownloadController
   - Normalizes paths and checks they start with DOWNLOADS_DIR
   - Serves files as application/octet-stream with Content-Disposition header

9. **RSS Feed Generation**
   - Simple string concatenation (no XML library)
   - Escapes XML entities manually
   - Uses SimpleDateFormat for RFC 822 date format

### Trade-offs Made

**Pros:**
- Fast implementation (reuses everything)
- Minimal code to maintain
- Type-safe (uses existing JPA entities and services)
- Stateless (JWT enables horizontal scaling)

**Cons:**
- No integration tests (deferred per user)
- Exposes JPA entities directly in responses (should use DTOs, but acceptable for MVP)
- CORS allows all origins (needs production config)
- RSS generation is manual (no XML validation)
- Download path configured via system property (could use Spring property)

### Research Conducted

- Read OnlineProfile, OnlineGame model classes to understand data structure
- Read service interfaces (OnlineProfileService, OnlineGameService, etc.) to understand method signatures
- Checked BannedKey model (uses `key` field, not separate fields)
- Verified Spring XML context loading chain (app-context-pokerserver.xml → app-context-gameserver.xml)
- Confirmed component scanning discovers @Service beans in com.donohoedigital package

---

## Review Results

**Status:** ❌ BLOCKING ISSUES FOUND - Security fixes required before Phase 2

**Reviewed by:** Claude Sonnet 4.5
**Date:** 2026-02-12

### Findings

#### ✅ Strengths

1. **Excellent Architecture** - Service layer reuse is clean and effective. Zero duplication, all existing business logic preserved.

2. **Proper JWT Cookie Handling** - HttpOnly cookies provide good XSS protection. Automatic transmission simplifies client implementation.

3. **Good Logging** - Auth events properly logged with context (username, admin status, ban reasons).

4. **Comprehensive Coverage** - All Wicket functionality covered by REST endpoints. API provides full feature parity.

5. **Clean Code Structure** - Controllers are focused, security infrastructure is modular, DTOs are appropriately used.

#### ⚠️ Suggestions (Non-blocking)

1. **Add Input Validation** - Most endpoints lack validation annotations. Consider adding @Valid, @Size, @Pattern where appropriate.
   - Affects: All controllers except AuthController

2. **Consider Transaction Management** - Multi-step operations (password update, profile retirement) should use @Transactional.
   - Affects: ProfileController.java:75-100, 115-123

3. **Add DTO Layer** - Currently returning JPA entities directly. Consider DTOs for cleaner API contracts.
   - Affects: All controllers
   - Note: Already causing password hash exposure (see blocking issue #1)

4. **Improve Error Responses** - Use proper HTTP status codes (400, 401, 403) instead of 200 with error messages in body.
   - Affects: AuthController, ProfileController

5. **Add Rate Limiting** - Login endpoint vulnerable to brute force attacks without rate limiting.
   - Affects: AuthController.java:78

#### ❌ Required Changes (Blocking)

1. **CRITICAL: Password Hash Exposure**
   - File: ProfileController.java:69, OnlineProfile.java:148
   - Issue: GET /api/profile returns entire OnlineProfile entity including password hash
   - Impact: Password hashes exposed to authenticated users (and potentially attackers)
   - Fix: Add `@JsonIgnore` to OnlineProfile.getPasswordHash() OR use ProfileDTO without password field

2. **CRITICAL: Weak JWT Secret Default**
   - Files: application.properties:8, JwtTokenProvider.java:50
   - Issue: Default JWT secret is weak and predictable
   - Impact: If JWT_SECRET env var not set, all tokens can be forged
   - Fix: Remove default value, add startup validation to fail-fast if not configured

3. **CRITICAL: CSRF Protection Disabled**
   - File: SecurityConfig.java:64
   - Issue: `.csrf(AbstractHttpConfigurer::disable)` with cookie-based auth
   - Impact: Even with stateless JWT, cookies are automatically sent with requests, enabling CSRF attacks
   - Fix: Either enable CSRF protection OR add SameSite=Strict to cookies + document risk

4. **CRITICAL: Broken /api/auth/me Endpoint**
   - File: AuthController.java:145
   - Issue: Uses `@RequestAttribute("username")` instead of `@AuthenticationPrincipal`
   - Impact: Endpoint always returns "Not authenticated" even with valid JWT
   - Fix: Change to `@AuthenticationPrincipal String username`

5. **CRITICAL: CORS Allows All Origins with Credentials**
   - File: SecurityConfig.java:86-89
   - Issue: `allowedOriginPatterns("*")` + `allowCredentials(true)`
   - Impact: Any website can make authenticated requests to API. Major security vulnerability.
   - Fix: Restrict to specific allowed origins (e.g., https://www.ddpoker.com)

6. **HIGH: Path Traversal Vulnerability**
   - File: DownloadController.java:63
   - Issue: String comparison `!filePath.startsWith(DOWNLOADS_DIR)` instead of path comparison
   - Impact: Can potentially be bypassed with ../ sequences depending on OS path handling
   - Fix: Use `!filePath.startsWith(Paths.get(DOWNLOADS_DIR))` for proper path comparison

### Verification

- **Tests:** ⚠️ None written (deferred per user directive for speed)
- **Coverage:** N/A
- **Build:** ✅ Compiles cleanly, all dependencies resolved
- **Privacy:** ✅ No private data in code itself
- **Security:** ❌ 6 blocking security issues identified above

### Summary

Phase 1 implementation demonstrates excellent architectural decisions (service reuse, clean separation, JWT in HttpOnly cookies) but has critical security vulnerabilities that MUST be addressed before proceeding to Phase 2. The issues are concentrated in:

1. Data exposure (password hashes)
2. Configuration weaknesses (JWT secret, CORS, CSRF)
3. Implementation bugs (path traversal, broken auth endpoint)

All blocking issues have straightforward fixes. Once resolved, the foundation will be solid for Phase 2 (Next.js frontend).
