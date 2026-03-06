# Remove Legacy Server Module

**Status:** APPROVED (2026-03-05)

## Problem

The `code/server/` module is a legacy custom HTTP server framework (socket-based, pre-Servlet era) from the original DD Poker codebase (2003). It has been fully replaced by Spring Boot (`pokergameserver` module). The legacy socket server binds a port but has no servlet to handle requests — it's dead code consuming build time and causing confusion.

## Scope

### Delete entirely

- **`code/server/`** — entire module (10 classes, ~2,500 lines)
  - `GameServer`, `BaseServlet`, `SocketThread`, `ThreadPool`, `WorkerPool`, `WorkerThread`, `GameServletRequest`, `GameServletResponse`, `ServerSecurityProvider`, `ServletDebug`
  - `ThreadPoolTest`
- **3 gametools utilities** — `GenerateKey.java`, `EncryptString.java`, `EncryptKey.java` (used `ServerSecurityProvider`, confirmed unused)
- **`PokerServer.java`** — extends `GameServer`, only useful code is admin profile init

### Replace

- **`PokerServer`** → new `AdminProfileInitializer` Spring `@Component` in `com.donohoedigital.games.poker.server`
  - Contains `initializeAdminProfile()`, `writeAdminPasswordFile()`, `readAdminPasswordFile()` (moved from `PokerServer`)
  - `@PostConstruct` triggers on startup
  - Injects `OnlineProfileService` only (drops `DDPostalService` and `GameServer` lifecycle)
- **`PokerServerTest`** → renamed to `AdminProfileInitializerTest`, same 5 tests, no more reflection hacks
- **`TestConfig`** → update exclusion filter (replace `PokerServer.class` reference)

### POM changes

- **`code/pom.xml`** — remove `<module>server</module>`
- **`pokerserver/pom.xml`** — remove `server` dependency
- **`gamecommon/pom.xml`** — remove `server` dependency (unused transitive)

## Verification

- The legacy socket server handles zero traffic (confirmed: no `BaseServlet` implementation exists, all clients use Spring Boot REST/WebSocket)
- `pokergameserver` has zero dependency on the `server` module
- `ServerSecurityProvider` is explicitly deprecated; only consumers are the 3 gametools utilities being deleted
- `docs/memory.md` documents Spring Boot migration as complete

## Risk

**Low.** All code being removed is confirmed dead. The only behavioral change is admin profile initialization moving to a standalone component (same logic, cleaner wiring).

### DDPostalService lifecycle fix

`PokerServer.destroy()` currently calls `postalService.destroy()` to shut down the mail queue thread. `DDPostalServiceImpl` is created via `@Bean` in `PostalServiceConfig` but `destroy()` is not registered with Spring. Fix: add `destroyMethod = "destroy"` to the `@Bean` annotation so Spring handles shutdown automatically. This decouples mail lifecycle from the server component.
