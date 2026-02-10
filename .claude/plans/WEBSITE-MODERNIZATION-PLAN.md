# Modernize DD Poker Website: Wicket → React/Next.js

## Context

The DD Poker website is built on Apache Wicket 10.8.0, a Java component-based web framework. While functional, Wicket is a niche framework that makes the codebase harder to maintain and less approachable for contributors. The goal is to replace the web UI with React/Next.js for better developer experience and maintainability, while keeping the same features and visual design. The game server (port 8877, UDP chat) and database layer remain unchanged.

## Architecture Decision

**Two-layer approach:**
1. **Spring Boot REST API** (Java) — thin API layer that reuses existing JPA entities and Spring services
2. **Next.js frontend** (TypeScript/React) — server-side rendered pages that call the REST API

This avoids reimplementing any data access logic. The existing `OnlineProfileService`, `OnlineGameService`, `TournamentHistoryService`, and `BannedKeyService` are wired directly into REST controllers.

### Why not Next.js API routes with direct DB access?
- Would require reimplementing all JPA entities, queries, and business logic in Node.js
- The existing Java services are battle-tested and well-integrated with the game server

### Docker Deployment
- The container currently runs Java only (eclipse-temurin:25-jdk)
- **Build Next.js as a static export** (`next export`) served by the Spring Boot embedded server
- This keeps the container Java-only — no Node.js runtime needed at deploy time
- Node.js is only needed at build time (multi-stage Docker build)
- Alternative: Run Next.js in SSR mode with a Node.js process alongside Java processes. Only needed if we require server-side rendering (unlikely given the content is mostly data tables)

### Authentication
- JWT tokens issued by the Spring Boot API on login
- Stored in HttpOnly cookies for security
- Admin routes protected by JWT role claims
- "Remember me" maps to longer-lived JWT expiry

### CSS/Styling
- Use Tailwind CSS for utility-first styling
- Port the existing color scheme (poker greens, CSS custom properties) as Tailwind theme config
- Keeps the same visual design while being easier to maintain

## Directory Structure

```
code/
├── api/                          # NEW: Spring Boot REST API module
│   ├── pom.xml
│   └── src/main/java/.../api/
│       ├── ApiApplication.java   # Spring Boot main class
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   ├── CorsConfig.java
│       │   └── JwtConfig.java
│       ├── controller/
│       │   ├── AuthController.java       # login, logout, forgot-password
│       │   ├── GameController.java       # game lists, detail, export
│       │   ├── LeaderboardController.java
│       │   ├── ProfileController.java    # my-profile, aliases
│       │   ├── SearchController.java     # player/host search
│       │   ├── HistoryController.java    # tournament history
│       │   ├── AdminController.java      # admin endpoints
│       │   ├── DownloadController.java   # file downloads
│       │   └── RssController.java        # RSS feeds
│       ├── dto/                          # Response DTOs
│       ├── security/
│       │   ├── JwtTokenProvider.java
│       │   └── JwtAuthFilter.java
│       └── resources/
│           ├── application.properties
│           └── app-context-api.xml       # imports existing Spring contexts
│
├── web/                          # NEW: Next.js frontend
│   ├── package.json
│   ├── next.config.js
│   ├── tailwind.config.js
│   ├── tsconfig.json
│   ├── src/
│   │   ├── app/                  # Next.js App Router
│   │   │   ├── layout.tsx        # Root layout (nav, footer)
│   │   │   ├── page.tsx          # Home page
│   │   │   ├── about/
│   │   │   │   ├── page.tsx
│   │   │   │   ├── practice/page.tsx
│   │   │   │   ├── online/page.tsx
│   │   │   │   ├── analysis/page.tsx
│   │   │   │   ├── pokerclock/page.tsx
│   │   │   │   ├── screenshots/page.tsx
│   │   │   │   └── faq/page.tsx
│   │   │   ├── download/page.tsx
│   │   │   ├── support/
│   │   │   │   ├── page.tsx
│   │   │   │   ├── selfhelp/page.tsx
│   │   │   │   └── passwords/page.tsx
│   │   │   ├── online/page.tsx
│   │   │   ├── leaderboard/page.tsx
│   │   │   ├── current/page.tsx
│   │   │   ├── completed/page.tsx
│   │   │   ├── in-progress/page.tsx
│   │   │   ├── available/page.tsx
│   │   │   ├── game/page.tsx
│   │   │   ├── history/page.tsx
│   │   │   ├── hosts/page.tsx
│   │   │   ├── search/page.tsx
│   │   │   ├── myprofile/page.tsx
│   │   │   ├── forgot/page.tsx
│   │   │   ├── terms/page.tsx
│   │   │   └── admin/
│   │   │       ├── page.tsx
│   │   │       ├── online-profile-search/page.tsx
│   │   │       ├── reg-search/page.tsx
│   │   │       └── ban-list/page.tsx
│   │   ├── components/
│   │   │   ├── layout/
│   │   │   │   ├── Navigation.tsx
│   │   │   │   ├── TopNavigation.tsx
│   │   │   │   └── CopyrightFooter.tsx
│   │   │   ├── auth/
│   │   │   │   ├── LoginForm.tsx
│   │   │   │   └── CurrentProfile.tsx
│   │   │   ├── games/
│   │   │   │   ├── GamesList.tsx
│   │   │   │   ├── GameDetail.tsx
│   │   │   │   ├── PlayerList.tsx
│   │   │   │   └── GameUrl.tsx
│   │   │   ├── leaderboard/
│   │   │   │   └── LeaderboardTable.tsx
│   │   │   ├── search/
│   │   │   │   ├── NameRangeSearchForm.tsx
│   │   │   │   └── HighlightedAliases.tsx
│   │   │   ├── profile/
│   │   │   │   └── Aliases.tsx
│   │   │   └── ui/
│   │   │       ├── Pagination.tsx
│   │   │       ├── DateRangeFilter.tsx
│   │   │       └── FormFeedback.tsx
│   │   ├── lib/
│   │   │   ├── api.ts              # API client (fetch wrapper)
│   │   │   ├── auth.ts             # Auth context/hooks
│   │   │   └── types.ts            # TypeScript types matching API DTOs
│   │   └── styles/
│   │       └── globals.css         # Tailwind imports + custom properties
│   └── public/
│       ├── images/                 # Copied from current webapp/images
│       ├── favicon.ico
│       └── robots.txt
│
├── pokerwicket/                   # EXISTING: kept during migration, removed after
├── pokerserver/                   # EXISTING: unchanged
└── pom.xml                        # Add api module
```

## Implementation Phases

### Phase 1: Spring Boot REST API

Create the `code/api` module with REST endpoints that wrap existing services.

**Steps:**
1. Create `code/api/pom.xml` with Spring Boot starter dependencies, referencing existing modules (`pokerserver`, `db`, `wicket`) for service access
2. Create `ApiApplication.java` Spring Boot app that imports existing Spring XML contexts
3. Implement JWT authentication (`JwtTokenProvider`, `JwtAuthFilter`, `SecurityConfig`)
4. Implement controllers in order of dependency:
   - `AuthController` — POST `/api/auth/login`, `/api/auth/logout`, `/api/auth/forgot-password`
   - `GameController` — GET `/api/games?mode=available|current|running|ended`, GET `/api/games/{id}`, GET `/api/games/export?mode=...&format=csv`
   - `LeaderboardController` — GET `/api/leaderboard?mode=ddr1|roi&from=&to=&name=&page=`
   - `ProfileController` — GET `/api/profile`, PUT `/api/profile/password`, POST `/api/profile/retire`
   - `HistoryController` — GET `/api/history?name=&from=&to=&page=`
   - `SearchController` — GET `/api/search?name=&page=`
   - `AdminController` — GET/POST/DELETE `/api/admin/bans`, GET `/api/admin/profiles`, GET `/api/admin/registrations`
   - `DownloadController` — GET `/api/downloads/{filename}` (serves files from /app/downloads)
   - `RssController` — GET `/api/rss/{feed}` (generates RSS XML)
5. Create DTO classes for API responses (don't expose JPA entities directly)
6. Write integration tests for each controller

**Key existing files to reuse:**
- `code/pokerserver/src/main/java/.../service/` — All service interfaces and implementations
- `code/pokerwicket/src/main/java/.../wicket/util/LoginUtils.java` — Authentication logic
- `code/pokerwicket/src/main/java/.../wicket/pages/online/Leaderboard.java` — Leaderboard query logic
- `code/pokerwicket/src/main/java/.../wicket/rss/GamesListFeed.java` — RSS feed generation

**Verify:** Run API integration tests. Manually test endpoints with curl/Postman.

### Phase 2: Next.js Project Setup & Static Pages

Create the `code/web` module with Next.js, Tailwind, and port all static/content pages.

**Steps:**
1. Initialize Next.js project with TypeScript in `code/web/`
2. Configure Tailwind CSS with the existing color scheme from `styles.css`
3. Create the root layout with Navigation and CopyrightFooter components
4. Port the navigation structure from `navData.js` to the React Navigation component
5. Implement mobile responsive hamburger menu (matching current behavior)
6. Port static pages (content from existing HTML templates):
   - Home, About (all 7 sub-pages), Download, Support (all 3 sub-pages), Terms
7. Copy all static images from `code/pokerwicket/src/main/webapp/images/` to `code/web/public/images/`
8. Set up the API client library (`lib/api.ts`) with JWT token handling

**Verify:** `npm run dev` — all static pages render. Navigation works. Mobile menu works. Visual comparison with existing site.

### Phase 3: Authentication & Online Pages

Wire up login/logout and build the dynamic online portal pages.

**Steps:**
1. Implement auth context/hooks (`lib/auth.ts`) — login, logout, token refresh, "remember me"
2. Build LoginForm and CurrentProfile components
3. Implement Online Portal home page with RSS feed links
4. Build the shared GamesList component with pagination and date-range filtering
5. Implement game list pages: Available, Current, Running, Completed
6. Implement GameDetail page with tournament profile and finish table
7. Implement CSV export functionality
8. Build Leaderboard page with DDR1/ROI modes, filtering, pagination
9. Implement Search page with highlighted results
10. Implement History page with player stats
11. Implement HostList page
12. Implement MyProfile page (change password, view/retire aliases)
13. Implement ForgotPassword page

**Verify:** Login/logout works. All game lists paginate correctly. Leaderboard modes switch properly. Profile management works. Compare data output with existing Wicket pages.

### Phase 4: Admin Section & RSS

**Steps:**
1. Implement admin route protection (redirect to login if not admin)
2. Build AdminHome dashboard
3. Implement OnlineProfileSearch page
4. Implement RegistrationSearch page
5. Implement BanList management (add/remove bans with date ranges)
6. Verify RSS feeds work via the API

**Verify:** Admin pages only accessible when logged in as admin. Ban management CRUD works. RSS feeds validate.

### Phase 5: Docker Integration & Deployment

Update the Docker build to include the new frontend.

**Steps:**
1. Add Node.js build stage to Dockerfile for `next build && next export`
2. Copy static export output to `/app/webapp/` (replacing old Wicket webapp)
3. Update Spring Boot API to serve the static files (or use a simple static file handler)
4. Update `entrypoint.sh` to start the API instead of PokerJetty
5. Update environment variable handling for the new API
6. Update `docker-compose.yml` if needed
7. Test full Docker build and deployment

**Verify:** `docker compose up` — full site works. Game server still functions. All pages load. Login works. Admin works.

### Phase 6: Cleanup

1. Remove `code/pokerwicket/` module (after verifying everything works)
2. Remove `code/wicket/` module if no longer needed
3. Update `code/pom.xml` module list
4. Update documentation (README, DEPLOYMENT.md)

## Key Files to Modify

| File | Change |
|------|--------|
| `code/pom.xml` | Add `api` module, eventually remove `pokerwicket` and `wicket` |
| `docker/Dockerfile` | Add Node.js build stage, change web process to API |
| `docker/entrypoint.sh` | Replace PokerJetty startup with Spring Boot API startup |
| `docker/docker-compose.yml` | Update if port mappings change |

## Key Files to Create

| File | Purpose |
|------|---------|
| `code/api/pom.xml` | Spring Boot REST API Maven module |
| `code/api/src/main/java/.../ApiApplication.java` | Spring Boot entry point |
| `code/api/src/main/java/.../controller/*.java` | REST controllers (~9 files) |
| `code/api/src/main/java/.../security/*.java` | JWT auth (~3 files) |
| `code/web/package.json` | Next.js project |
| `code/web/src/app/**/*.tsx` | All page components (~25 files) |
| `code/web/src/components/**/*.tsx` | Shared UI components (~15 files) |
| `code/web/src/lib/*.ts` | API client, auth, types (~3 files) |

## Testing Strategy

- **API:** JUnit integration tests for each controller using MockMvc
- **Frontend:** Jest + React Testing Library for component tests
- **E2E:** Manual testing against Docker deployment (consider Playwright later)
- **Regression:** Compare API responses with data from existing Wicket pages

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Static export limits dynamic features | Start with static export; switch to Node.js SSR in Docker if needed |
| JWT adds complexity vs. session auth | Use well-tested library (jjwt); keep token logic simple |
| Large image directory (~200+ files) | Simple copy; no changes to image content |
| Database access from API vs. Wicket | API reuses exact same Spring services; no data access changes |
