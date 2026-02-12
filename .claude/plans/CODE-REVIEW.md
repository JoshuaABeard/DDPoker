# Code Review & Backlog

## Context

Comprehensive code review of DDPoker's server-side and shared infrastructure modules. Covers code quality, security, performance, concurrency, resource management, and technical debt.

**Scope:** `code/common/`, `code/db/`, `code/mail/`, `code/udp/`, `code/server/`, `code/gameserver/`, `code/pokerserver/`, `code/poker/`
**Excludes:** Password plaintext/hashing issues (covered in `BCRYPT-PASSWORD-HASHING.md`)
**Review Date:** February 2026

---

## P0: Critical — Security & Stability

### ✅ SEC-2: Add Input Validation (COMPLETED)
**Files:** `EngineServlet.java`, `PokerServlet.java`
**Issue:** No validation of email format, parameter bounds, string lengths.
**Locations:**
- `joinOnlineGame()` — `EngineServlet.java` line 1015 (email validation missing)
- `addOnlineProfile()` — no bounds checking
- `addWanGame()` — no parameter validation

**Fix:** Add email format validation, string length limits, and validate all user inputs before processing.
**Completed:** Feb 12, 2026 (commit 3eca2de) - Created InputValidator utility class

---

### ✅ SEC-3: Implement Rate Limiting (COMPLETED)
**Files:** `PokerServlet.java`, `ChatServer.java`
**Issue:** No rate limiting on profile operations or chat messages. DoS vulnerability.
**Fix:** Add rate limiting for profile creation/updates and chat message frequency per user.
**Completed:** Feb 12, 2026 (commit 3eca2de) - Created RateLimiter utility class

---

### ✅ LEAK-1: Fix ApplicationContext Resource Leak (COMPLETED)
**Files:**
- `OnlineGamePurger.java` (line 128)
- `Ban.java` (line 111)

**Issue:** `ApplicationContext` created but never closed.
**Fix:** Use try-with-resources or explicit `close()`.
**Completed:** Feb 11, 2026 - Wrapped in try-with-resources

---

### ✅ DEAD-1: Remove Dead Code — `if (false)` Block (COMPLETED)
**File:** `EngineServlet.java` (lines 405-416)
**Issue:** License validation disabled with `if (false)` — dead code that confuses maintainers.
**Fix:** Delete entire block.
**Completed:** Feb 9, 2026 (commit 3e2ac3c)

---

## P1: Quick Fixes — Concurrency (small effort, high value)

### ✅ QF-1: Add `volatile` to cross-thread boolean flags (COMPLETED)

**Issue:** Multiple classes use `boolean bDone_` flags to signal thread shutdown. Without `volatile`, the JVM may cache the value in a CPU register, so one thread's write is never visible to another. Threads may never terminate on shutdown.

**Files:**
- `code/server/.../GameServer.java` line 67: `private boolean bDone_` ✅ Already had volatile
- `code/mail/.../DDPostalServiceImpl.java` line 68: `private boolean bDone_`, line 74: `private boolean bSleeping_` ✅ Already had volatile
- `code/udp/.../UDPServer.java` line 103: `private boolean bDone_` ✅ Already had volatile
- `code/udp/.../UDPManager.java` line 64: `boolean bDone_` ✅ Already had volatile
- `code/udp/.../UDPLink.java` lines 91-95: `bHelloReceived_`, `bHelloSent_`, `bGoodbyeInProgress_`, `bDone_` ✅ Already had volatile
- `code/udp/.../DispatchQueue.java` line 53: `private boolean bDone_` ✅ Fixed
- `code/udp/.../OutgoingQueue.java` line 54: `private boolean bDone_` ✅ Fixed

**Completed:** Feb 12, 2026 (commit 813e406) - Added volatile to remaining classes
**Verify:** `mvn test -pl server,mail,udp`

---

### ✅ QF-2: Add `volatile` to `EngineServlet` double-checked locking fields (COMPLETED)
**File:** `EngineServlet.java` (lines 1265, 1267)
**Issue:** Double-checked locking without volatile
**Fix:** Add `volatile` keyword.
**Completed:** Previously completed - both fields already have volatile

---

### ✅ QF-3: Use `ConcurrentHashMap` for shared static maps (COMPLETED)

**Issue:** Unsynchronized `HashMap` used for static fields accessed from multiple threads. Can cause infinite loops, lost entries, or `ConcurrentModificationException`.

**Files:**
- `code/db/.../DatabaseManager.java` line 66: ✅ Already uses `ConcurrentHashMap`
- `code/common/.../DDHttpClient.java` lines 87-94: ✅ Already uses synchronized `LinkedHashMap` with size cap of 256

**Completed:** Previously completed
**Verify:** `mvn test -pl db,common`

---

### ✅ QF-4: Fix `PropertyConfig.formats_` thread safety (COMPLETED)
**File:** `code/common/.../PropertyConfig.java` line 384
**Issue:** `HashMap<String, MessageFormat>` used inside a `synchronized` block. Works, but `ConcurrentHashMap` with `computeIfAbsent()` is cleaner and more performant.
**Fix:** Replace with `ConcurrentHashMap`, use `computeIfAbsent()`, remove the synchronized block.
**Completed:** Feb 12, 2026 (commit 813e406) - Converted to ConcurrentHashMap with computeIfAbsent
**Verify:** `mvn test -pl common`

---

### ✅ QF-5: Remove unnecessary nested `synchronized` on `GameServer.getRegisterQueue()` (COMPLETED)
**File:** `code/server/.../GameServer.java` line 747
**Issue:** Method had unnecessary outer synchronized
**Fix:** Remove `synchronized` from the method signature. Inner `synchronized(registerQ_)` is sufficient.
**Completed:** Previously completed - method signature no longer has synchronized
**Verify:** `mvn test -pl server`

---

### ✅ QF-6: Replace Static SEQ Counter with AtomicInteger (COMPLETED)
**File:** `EngineServlet.java` (line 163)
**Issue:** Synchronized int counter for sequence numbers
**Fix:** Use `AtomicInteger.incrementAndGet()`.
**Completed:** Previously completed - already using AtomicInteger

---

### ✅ QF-7: Make Mutable Static Fields Final (COMPLETED)
**File:** `LanManager.java` (lines 57-58)
**Issue:** `private static int ALIVE_SECONDS = 5;` and `ALIVE_REFRESH_CNT = 10` are mutable statics that should be final.
**Fix:** Add `final` modifier.
**Completed:** Previously completed - both fields already have final

---

## P1: Quick Fixes — Resource Leaks

### ✅ QF-8: Fix resource leaks in `ConfigUtils.copyFile()` (COMPLETED)
**File:** `code/common/.../ConfigUtils.java` lines 461-464
**Issue:** Creates `FileInputStream`/`FileOutputStream`, gets channels, transfers data, closes only the channels. The underlying streams are never closed, leaking file descriptors.
**Fix:** Use try-with-resources wrapping both streams and channels.
**Completed:** Previously completed - already using try-with-resources for both streams and channels
**Verify:** `mvn test -pl common`

---

## P1: Quick Fixes — Code Clarity

### ✅ QF-9: Fix `DDPostalServiceImpl` misleading mail send loop (COMPLETED)
**File:** `code/mail/.../DDPostalServiceImpl.java` line 337
**Issue:** Loop iterates `nNum - 1` down to `0`, but each iteration calls `list.get(0)` / `list.remove(0)`. Works correctly but the loop variable `i` is unused and misleading.
**Fix:** Replace with `while (!list.isEmpty())`.
**Completed:** Previously completed - already using while loop
**Verify:** `mvn test -pl mail`

---

## P2: Medium Fixes (moderate effort)

### ✅ MF-1: Fix resource leaks in `DatabaseQuery` (COMPLETED)
**File:** `code/db/.../DatabaseQuery.java`, `ResultMap.java`
**Issue:** Class-level `@SuppressWarnings("JDBCResourceOpenedButNotSafelyClosed")` at line 46. `close()` method (line 588) closes `PreparedStatement` and `Connection` but not `ResultSet`. Relies on implicit close which isn't guaranteed by all JDBC drivers.
**Fix:** Track `ResultSet` as instance field, close explicitly in `close()` before closing the statement.
**Completed:** Feb 12, 2026 (commit 343a9ea) - Added ResultSet cleanup in ResultMap.close()
**Verify:** `mvn test -pl db`

---

### ✅ MF-2: Fix resource leaks in `PokerDatabase` (COMPLETED)
**File:** `code/poker/.../PokerDatabase.java`
**Issue:** Multiple methods close `ResultSet` and `PreparedStatement` in the try block body (not in `finally`). If an exception occurs between creating the resource and the close call, the resource leaks. Two critical leaks found where Statement objects were never closed.
**Fix:** Convert to try-with-resources for `Connection`, `PreparedStatement`, and `ResultSet`. All 28 methods modernized.
**Completed:** Feb 12, 2026 (commits 66cab52, f37ed19, 4d939c9) - Modernized all database resource management
**Verify:** `mvn test -pl poker`

---

### MF-3: Replace custom `Base64` with `java.util.Base64`
**File:** `code/common/.../Base64.java`
**Issue:** Custom third-party implementation (iharder.net) with exception-swallowing (empty catch blocks, `e.printStackTrace()` then return null). Java has included `java.util.Base64` since Java 8.
**Fix:** Find all usages of `com.donohoedigital.base.Base64`, replace with `java.util.Base64.getEncoder()` / `getDecoder()`. API differs, so each call site needs attention.
**Verify:** `mvn test` (full build)

---

### MF-4: Reduce Lock Duration in EngineServlet
**File:** `EngineServlet.java` (lines 500-558)
**Issue:** Game lock held during serialization. Poor scalability with concurrent games.
**Fix:** Serialize response outside synchronized block. Already partially implemented with `retdata` pattern — complete the optimization.

---

## P2: Technical Debt

### DEBT-1: Replace System.out/err with Logging
**Files:** `Ban.java`, `RegAnalyzer.java`, `OnlineGamePurger.java`, `OnlineProfilePurger.java`
**Issue:** Direct `System.out/err` in command-line tools instead of log4j2.
**Fix:** Use logging framework.

---

### DEBT-2: Externalize Hardcoded Values
**Files:** `EngineServlet.java` (lines 1121-1125), `LanManager.java` (lines 55-59)
**Issue:** Timing constants and poll settings hardcoded.
**Fix:** Move to properties files.

---

### DEBT-3: Remove or Undeprecate Registration Class
**File:** `Registration.java` (lines 16-25)
**Issue:** Class marked `@Deprecated` but still used throughout codebase — causes compiler warnings.
**Fix:** Either remove entirely or undeprecate with documentation.

---

### DEBT-4: Add Query Result Caching
**File:** `RegistrationServiceImpl.java` (lines 128-155)
**Issue:** Fetches ALL banned keys into memory each time.
**Fix:** Add pagination or cache banned key list with TTL.

---

### DEBT-5: Optimize Database Queries
**File:** `RegistrationImplJpa.java` (line 235)
**Issue:** Complex GROUP BY + HAVING queries.
**Fix:** Add query optimization, consider indexed views.

---

## P2: TODO Items (from code comments)

### TODO-1: Implement Load Balancing
**File:** `EngineServlet.java` (line 1112)
**TODO:** `// TODO: should we need to do load balancing, new server URLs`
**Priority:** Low unless scaling needed.

---

### TODO-2: Implement FileLock for Multi-JVM
**File:** `ServerSideGame.java` (line 219)
**TODO:** `// TODO: if we move to multiple JVM instances, we can synchronize`
**Fix:** Use `java.nio.channels.FileLock` or distributed locking if multi-JVM is needed.

---

### TODO-3: Implement Email Bounce Handling
**File:** `ServerSideGame.java` (line 860)
**TODO:** `// TODO: send from server w/ bounce handling?`

---

### TODO-4: Add Player Statistics
**File:** `ChatServer.java` (line 228)
**TODO:** `// TODO: stats?`

---

### TODO-5: Add Configuration File Loading
**File:** `ServletDebug.java` (lines 49, 174)
**Issue:** Configuration file reading marked as TODO.

---

## Big Effort Items (need separate plans)

### BE-1: EngineServlet Monster Method Refactor
**File:** `EngineServlet.java`
**Issue:** `_processMessage()` is 476 lines (lines 254-730). `processExistingGameMessageLocked()` is 167 lines.
**Fix:** Extract each message category into separate handler classes using a `MessageHandler` interface.
**Why separate plan:** 5-8 hours per handler class, many handler classes needed, requires comprehensive regression testing.

---

### BE-2: Chat Message Handling Refactor
**File:** `ChatServer.java` (lines 144-269)
**Issue:** 125-line switch statement in `processMessage()`.
**Fix:** Strategy or command pattern with per-message-type handlers.

---

### BE-3: Authentication System Redesign
**File:** `PokerServlet.java` (line 359)
**Issue:** Design note in code: "Our auth logic is kind of a pain and needs a redesign. We should always be sending down the current player."
**Fix:** Implement consistent player identity validation, document auth flow, add integration tests.

---

### BE-4: UDP Networking Concurrency Overhaul
**Scope:** `code/udp/` — `UDPLink.java`, `UDPServer.java`, `UDPManager.java`, `Peer2PeerMulticast.java`

**Issues:**
- `LinkedList sendQueue_` in `UDPLink` (line 80) with inconsistent synchronization
- Message ID rollover at `UDPLink` line 560 with no handling (just a log warning)
- Race conditions between `isHelloReceived()` check and subsequent operations
- Mixed synchronization patterns across the module

**Why separate plan:** Deeply intertwined concurrency issues. Fixing volatile (QF-1) addresses visibility but not atomicity. Proper fix requires `java.util.concurrent` primitives and concurrent testing.

**Note:** If UDP is being replaced with TCP (per `UDP-TO-TCP-CONVERSION.md`), this effort may not be worthwhile.

---

### BE-5: Database Resource Management Modernization
**Scope:** `code/db/`, `code/poker/` — `DatabaseQuery.java`, `PokerDatabase.java`, and all DAO classes

**Issues:**
- Widespread manual resource management instead of try-with-resources
- `DatabaseQuery` suppresses JDBC resource warnings at class level
- `PokerDatabase` has 10+ methods with potential leak paths
- No consistent Connection lifecycle pattern

**Why separate plan:** Database layer is used extensively. Requires touching many methods with careful attention to C3P0 connection pooling behavior. MF-1 and MF-2 are the highest-priority individual fixes from this category.

---

## P3: Minor Cleanup

### CLEANUP-1: Replace Unprofessional Comment
**File:** `Utils.java` (line 89)
**Issue:** Comment says "GAH! this code is crap".
**Fix:** Replace with a specific issue description or delete.

---

### CLEANUP-2: CSV Parser Error Handling
**File:** `CSVParser.java` (line 110)
**Issue:** TODO says "ERROR CONDITION - what to do?"
**Fix:** Implement proper error handling (throw exception or log).

---

### CLEANUP-3: FileChooserDialog "Pick File" Mode
**File:** `FileChooserDialog.java` (line 52)
**Issue:** Designed primarily for "save as" — "pick file" mode needs work. Workaround exists in DeckProfile.
**Fix:** Implement proper "pick file" mode or document the workaround.

---

### CLEANUP-4: Tab Focus in Help Table
**File:** `Help.java` (line 153)
**Issue:** Tab key doesn't change focus between UI elements in help table. Minor accessibility issue.
**Fix:** Implement proper focus traversal.

---

## P3: Enhancements (nice-to-have)

### ENHANCE-1: Add Metrics/Monitoring
Server health endpoints, JVM metrics, message processing times, active game/player counts.

### ENHANCE-2: Improve Error Messages
Add request IDs, diagnostic info, error code taxonomy.

### ENHANCE-3: Add OpenAPI/Swagger Documentation
Document all servlet endpoints with request/response examples.

### ENHANCE-4: Implement Graceful Shutdown
ServletContextListener, flush queues, close resources, save in-progress games.

---

## Design Notes (keep in code, do not remove)

These TODO comments document design decisions and should remain in the codebase:

1. **Bet Validation Context** (`PlayerAction.java:63`) — explains complexity of bet validation logic
2. **Window Resize Timing** (`GameContext.java:322`) — documents timing constraint in UI initialization
3. **Online Player Limit** (`TournamentProfile.java:76`) — documents capacity planning decision (`MAX_ONLINE_PLAYERS = 30`)
4. **ZIP Precision Loss** (`ZipUtil.java:85,101,123`) — documents limitation in timestamp handling
5. **XML Escaping** (`XMLWriter.java:248,273`) — notes potential issue with special characters

---

## Summary

| Priority | Category | Count |
|----------|----------|-------|
| P0 | Critical (security, leaks, dead code) | 4 |
| P1 | Quick fixes (concurrency, resources, clarity) | 9 |
| P2 | Medium fixes | 4 |
| P2 | Technical debt | 5 |
| P2 | TODO items | 5 |
| BE | Big effort (separate plans) | 5 |
| P3 | Minor cleanup | 4 |
| P3 | Enhancements | 4 |
| | **Total** | **40** |

## Verification

After all P0, P1, and P2 fixes:
1. `mvn test` — full test suite passes
2. `mvn package` — build succeeds with zero new warnings
3. Manual: start Docker container, verify server + web start cleanly, create profile, login
