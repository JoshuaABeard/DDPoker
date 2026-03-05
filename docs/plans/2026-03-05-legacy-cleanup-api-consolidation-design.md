# Legacy Cleanup + API Consolidation Design

**Status:** APPROVED (2026-03-05)

## Goal

Remove dead legacy code, modernize the server to Spring Boot, and consolidate the separate `api` module into the unified server. Update all clients (desktop, web, test scripts) for the new API paths.

## Workstream A: Dead Code Cleanup

### A1. Delete orphaned artifacts

Delete `code/gameserver/target/` — stale compiled artifacts from the deleted `gameserver` module.

### A2. Update DDPOKER-OVERVIEW.md

- Fix module list to match the current 15 modules in parent POM
- Add `pokergamecore` description
- Remove deleted modules (`gameserver` and any others no longer in the POM)

### A3. Verify and remove dead ChainPhases

- `CheckEndHand.java` — verify unreachable now that practice games use the embedded server via `PracticeGameLauncher` + `WebSocketTournamentDirector`. Delete class + `gamedef.xml` registration.
- Audit remaining `TD.*` phases in `gamedef.xml` for any others that are dead.

### A4. Audit dead game-driving methods

- `PokerTable.startNewHand()` — verify no gameplay callers, document as server-only / simulation-only
- `HoldemHand.deal()` — only called by `PokerStats`, keep for now (addressed in A5)
- `HoldemHand.bet/call/raise/fold/check()` — identify and remove any that are truly dead (not called from display or simulation code)
- `Pot` winner/chip methods — determine display-only vs dead, remove dead ones

### A5. Extract PokerStats simulation

- Move Monte Carlo simulation logic out of the client-side `HoldemHand.deal()` path
- Target module: `pokergamecore` (pure game logic, no Spring/Swing)
- After extraction, remove any game-driving methods from `HoldemHand`/`PokerTable` that become orphaned

## Workstream B: Spring Boot Modernization of `pokerserver`

### B1. Replace XML config with Spring Boot

- Convert `PokerServerMain` to `@SpringBootApplication`
- Convert `app-context-pokerserver.xml` and `app-context-gameserver.xml` bean definitions to `@Configuration` classes
- Migrate `pokerserver.properties` to Spring Boot `application.properties` / `application-{profile}.properties`
- Convert `PokerServer.init()` to `ApplicationRunner` or `@PostConstruct`
- Keep all service/DAO implementations unchanged (already `@Service`/`@Repository` annotated)

### B2. Unify Spring contexts

- `pokergameserver`'s `@AutoConfiguration` wires in naturally with Spring Boot
- Single unified application context instead of separate XML contexts
- `persistence-pokerserver.xml` migrates to Spring Boot JPA auto-configuration

### B3. Simplify Docker

- `entrypoint.sh` becomes single `java -jar` invocation
- `Dockerfile` builds one JAR instead of two
- Remove `api` module references from Docker build

## Workstream C: API Consolidation

### C1. Merge controllers into `pokergameserver`

All endpoints move to `/api/v1/` prefix to match existing `pokergameserver` convention.

| `api` Endpoint | Target Controller | Action |
|---|---|---|
| `/api/players/*` | Existing `ProfileController` | Add GET by name, PUT update, PUT password |
| `/api/profile/aliases` | Existing `ProfileController` | Add GET aliases |
| `/api/profile/templates/*` | New `TemplateController` | Create at `/api/v1/profiles/templates` |
| `/api/leaderboard/*` | New `LeaderboardController` | Create at `/api/v1/leaderboard` |
| `/api/history`, `/api/tournaments/*` | New `HistoryController` | Create at `/api/v1/history` |
| `/api/search` | New `SearchController` | Create at `/api/v1/search` |
| `/api/admin/*` | New `AdminController` | Create at `/api/v1/admin` (merge `DevController` verify-user) |
| `/api/downloads/*` | New `DownloadController` | Create at `/api/v1/downloads` |
| `/api/rss/*` | New `RssController` | Create at `/api/v1/rss` |
| `/api/health` | Spring Boot Actuator or `HealthController` | Create at `/api/v1/health` |
| `/` (IndexController) | Drop or simple redirect | Decide during implementation |

When merging into existing controllers, do not duplicate features — if the endpoint already exists in `pokergameserver`, compare feature sets and keep the more complete implementation.

### C2. Move DTOs and service wiring

- Move `api` module DTOs to `pokergameserver`
- Controllers depend on services in `pokerserver` (injected via Spring Boot auto-wiring)
- Any `api`-specific mappers/helpers move with their controllers

### C3. Update all three clients

- **Desktop** (`code/poker/`): Update `RestGameClient`, `GameServerRestClient`, any other HTTP callers
- **Web** (`code/web/`): Update `lib/api.ts` — change all `/api/{resource}` paths to `/api/v1/{resource}`
- **Web tests** (`code/web/lib/__tests__/`): Update `api-clients.test.ts` and `api.test.ts` path assertions
- **Scenario tests** (`tests/scenarios/`): Update curl URLs

### C4. Delete `api` module

- Remove `code/api/` directory entirely
- Remove `<module>api</module>` from parent POM
- Remove `api` references from Docker files

## Execution Order

```
A1-A3 (quick wins, independent)
  -> A4 (audit, informs A5)
    -> A5 (extraction)

B1-B2 (Spring Boot modernization)
  -> C1-C2 (merge controllers, requires B)
    -> C3 (update clients)
      -> C4 (delete api module)
        -> B3 (Docker simplification, requires C4)
```

## Acceptable Legacy (Not Touched)

These are required for the desktop thin client to function and are not in scope:

- `gameengine` (68 files) — Swing UI framework
- `gamecommon` — config/state infrastructure, required by gameengine
- `gametools` — game infrastructure, required by gameengine
- `PokerGame extends Game` — central to desktop app lifecycle
- `PokerPlayer`, `Pot` display-only methods — used by UI rendering

## Deployment Model

Single process: one Spring Boot JAR serves REST API + game hosting (WebSocket + game engine). One port, one process. Docker simplifies to `java -jar`.
