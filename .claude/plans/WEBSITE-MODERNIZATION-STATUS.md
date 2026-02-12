# Website Modernization - Implementation Status

## Phase 1: Spring Boot REST API ✅ IN PROGRESS

### Completed
- ✅ Created `code/api` module structure
- ✅ Created `pom.xml` with Spring Boot 3.2.2, JWT dependencies
- ✅ Created `ApiApplication.java` - Spring Boot main class with XML context imports
- ✅ Created JWT security infrastructure:
  - `JwtTokenProvider.java` - Token generation and validation
  - `JwtAuthFilter.java` - JWT authentication filter (extracts from cookies)
  - `SecurityConfig.java` - Security configuration with endpoint protection
- ✅ Created DTOs:
  - `LoginRequest.java`
  - `AuthResponse.java`
- ✅ Created `AuthController.java` - Login/logout with cookie-based JWT
- ✅ Created `application.properties` - App configuration

### Remaining for Phase 1
- ⏳ Create remaining controllers:
  - `GameController.java` - Game lists, detail, export (wraps OnlineGameService)
  - `LeaderboardController.java` - DDR1/ROI leaderboards
  - `ProfileController.java` - My profile, password change, retire
  - `HistoryController.java` - Tournament history (wraps TournamentHistoryService)
  - `SearchController.java` - Player/host search
  - `AdminController.java` - Ban management, profile search
  - `DownloadController.java` - File downloads
  - `RssController.java` - RSS feed generation
- ⏳ Create corresponding DTOs for each controller
- ⏳ Write integration tests for controllers
- ⏳ Add to parent `pom.xml` modules list
- ⏳ Test compilation and Spring Boot startup

## Phase 2: Next.js Project Setup (NOT STARTED)
- Create `code/web` directory
- Initialize Next.js with TypeScript
- Configure Tailwind CSS
- Create layout components
- Port static pages
- Copy images

## Phase 3: Authentication & Online Pages (NOT STARTED)
- Auth context/hooks
- Login form component
- Game list components
- Leaderboard page
- Profile pages

## Phase 4: Admin Section (NOT STARTED)
- Admin routing
- Admin pages
- RSS feeds

## Phase 5: Docker Integration (NOT STARTED)
- Update Dockerfile
- Update entrypoint.sh
- Test deployment

## Phase 6: Cleanup (NOT STARTED)
- Remove Wicket modules
- Update docs

---

## Key Architectural Decisions Made

### 1. JWT in HttpOnly Cookies
- More secure than localStorage (XSS-resistant)
- Automatic transmission with requests
- Matches existing "remember me" cookie pattern

### 2. Reuse Existing Services
- `OnlineProfileService`, `OnlineGameService`, `TournamentHistoryService` unchanged
- Import existing Spring XML contexts (`app-context-poker-server.xml`, `app-context-db.xml`)
- No database migration needed

### 3. Admin Check via PropertyConfig
- Matches existing Wicket implementation
- Admin user defined in `settings.admin.user` property
- JWT contains "ROLE_ADMIN" claim for admin users

### 4. Spring Boot 3.2.2
- Latest stable Spring Boot
- Uses Jakarta EE 10 (jakarta.* packages match existing code)
- Compatible with existing Spring XML config via @ImportResource

## Next Steps

To continue this work:
1. Complete remaining Phase 1 controllers (see list above)
2. Add `<module>api</module>` to `code/pom.xml`
3. Test API module builds: `cd code/api && mvn clean install`
4. Test Spring Boot starts: `mvn spring-boot:run`
5. Test login endpoint with curl
6. Move to Phase 2: Next.js setup
