# Backend Code Review & Improvement Plan

## Context

Comprehensive code review of DDPoker's poker-specific backend (`code/pokerserver/`). Focuses on the servlet layer, service layer, DAO implementations, and TCP chat server.

**Scope:** `code/pokerserver/` - PokerServlet, TcpChatServer, service layer, DAO layer, tests
**Complements:** `CODE-REVIEW.md` (covers general infrastructure: gameserver, common, db, udp, mail)
**Review Date:** February 2026

---

## Architecture Overview

Standard 3-tier Spring application:

```
PokerServlet (HTTP) ──> Services ──> DAOs ──> JPA/Hibernate
TcpChatServer (TCP) ──> Services ──> DAOs ──> JPA/Hibernate
```

**Strengths:**
- Clean interface/implementation separation
- Proper use of `@Transactional` with `readOnly` hints
- bcrypt password hashing with proper salt management
- Rate limiting on critical endpoints (profile creation, game creation)
- Input validation via `InputValidator` utility class
- Good test infrastructure with Spring test context

**Weaknesses:**
- Servlet is a monolith (988 lines) dispatching 16+ message types
- No REST API / no separation of transport from business logic
- Authentication inconsistent across endpoints
- No authorization framework (host vs. non-host not enforced)
- TCP chat authentication uses different mechanism than HTTP
- Major test coverage gaps (servlet untested, chat server tests disabled)

---

## P0: Critical — Security

### SEC-BACKEND-1: Information Disclosure via Chat Debug Commands
**File:** `TcpChatServer.java:488-493`
**Issue:** Any authenticated chat user can execute `./dump` to receive a full JVM stack dump, and `./stats` to see IP addresses and ports of all connected players.

```java
if (chatText.startsWith("./stats")) {
    sendMessage(channel, chatServer_.getStatusHTML());
    return;
} else if (chatText.startsWith("./dump")) {
    sendMessage(channel, "<PRE>" + Utils.getAllStacktraces() + "</PRE>");
    return;
}
```

The `./dump` command exposes internal thread states, class names, and memory layout. The `./stats` command exposes private IP addresses of all connected users.

**Fix:** Remove these commands entirely, or gate them behind an admin role check.
**Priority:** Critical - Information disclosure vulnerability

---

### SEC-BACKEND-3: Password Reset Without Authentication
**File:** `PokerServlet.java:923-951`
**Issue:** Anyone who knows a profile name can trigger a password reset (`sendOnlineProfilePassword`).

```java
profile = onlineProfileService.getOnlineProfileByName(profile.getName());
if (profile != null) {
    String newPassword = onlineProfileService.generatePassword();
    onlineProfileService.hashAndSetPassword(profile, newPassword);
    onlineProfileService.updateOnlineProfile(profile);
    sendProfileEmail(..., newPassword, null);
```

no email verification step, no confirmation token, and no rate limiting on this specific endpoint. Attacker can enumerate profile names and reset arbitrary passwords.

**Fix:**
- Add rate limiting to this endpoint
- Require email verification before resetting
- Generate reset token instead of new password

**Priority:** Critical - Account takeover vulnerability

---

## P1: High — Security & Authorization

### SEC-BACKEND-4: Chat Authentication Uses Plaintext Password Comparison
**File:** `TcpChatServer.java:408`
**Issue:** Chat HELLO handler compares passwords using plaintext field instead of bcrypt.

```java
if (user == null || !user.getPassword().equals(auth.getPassword())) {
```

This either (a) always fails if `getPassword()` returns null (since it's transient/not persisted), or (b) relies on old plaintext password field instead of bcrypt hash.

**Fix:** Replace with `passwordHashingService.checkPassword(auth.getPassword(), user.getPasswordHash())` or reuse `onlineProfileService.authenticateOnlineProfile()`.

**Priority:** High - Authentication bypass risk

---

### AUTH-1: Missing Authorization on Game Mutations
**File:** `PokerServlet.java`
**Issue:** No authentication on game mutation operations.

- `updateWanGame()` (line 492): Any client can update any game
- `deleteWanGame()` (line 548): Any client can delete any game
- `endWanGame()` (line 518): Any client can end any game

The `addWanGame()` method authenticates for v3+ clients, but update/delete/end accept requests purely based on matching game URL, with no verification that requester is the game host.

**Fix:** Require authentication for all game mutations and verify authenticated user is the game host.

**Priority:** High - Authorization bypass

---

### DOS-1: Unbounded Write Queue in Chat Server
**File:** `TcpChatServer.java:558`
**Issue:** `LinkedBlockingQueue<Peer2PeerMessage>` has no capacity bound.

```java
final LinkedBlockingQueue<Peer2PeerMessage> writeQueue;
```

A slow consumer that never reads causes messages to accumulate without limit, eventually exhausting server memory.

**Fix:** Set capacity limit (e.g., 100 messages) and disconnect clients whose queues are full.

**Priority:** High - DoS vulnerability

---

### CONCURRENCY-1: DisallowedManager Uses Static Mutable Collections
**File:** `DisallowedManager.java:50-51`
**Issue:** Static mutable lists with no synchronization.

```java
private static final List<String> disallowedContains = new ArrayList<String>();
private static final List<Pattern> disallowedPatterns = new ArrayList<Pattern>();
```

Lists are `static` but populated in constructor. Multiple instances accumulate duplicate entries. `isNameValid()` iterates lists without synchronization while constructor may be adding to them concurrently.

**Fix:** Make these instance fields (not static), or load once in static initializer, or use `Collections.unmodifiableList()` after population.

**Priority:** High - Thread safety issue

---

## P2: Medium — Security & Performance

### SEC-BACKEND-5: Hardcoded Email Domain Bypass
**File:** `PokerServlet.java:686, 766`
**Issue:** `donohoe.info` domain hardcoded as exempt from `MAX_PROFILES_PER_EMAIL` limit.

```java
if (count >= PokerConstants.MAX_PROFILES_PER_EMAIL && !(profile.getEmail().endsWith("donohoe.info"))) {
```

Leftover from original developer. Should be configurable or removed for community edition.

**Fix:** Remove hardcoded bypass or make it a configurable property.

**Priority:** Medium - Technical debt

---

### BUG-1: addWanGame() Ban Check on Null Profile
**File:** `PokerServlet.java:439, 457`
**Issue:** `banCheck()` called before profile is resolved.

```java
OnlineProfile profile = null;
...
resMsg = banCheck(profile);  // profile is still null here!
```

The `banCheck(OnlineProfile)` method returns null for null profiles, silently skipping the ban check. Should occur after profile is resolved (lines 465 or 470).

**Fix:** Move ban check to after profile resolution.

**Priority:** Medium - Logic error (ban check bypassed)

---

### PERF-1: N+1 Query in getOnlineGamesAndHistoriesForDay()
**File:** `OnlineGameServiceImpl.java:92-101`
**Issue:** Separate SQL query for each game's histories.

```java
for (OnlineGame game : list) {
    game.getHistories().size();  // forces lazy-load per game
}
```

Classic N+1 problem. With large number of games, causes severe performance degradation.

**Fix:** Use JPQL query with `JOIN FETCH` to eagerly load histories in a single query.

**Priority:** Medium - Performance issue

---

### LEAK-BACKEND-1: OnlineProfilePurger ApplicationContext Resource Leak
**File:** `OnlineProfilePurger.java:77`
**Issue:** `ApplicationContext` never closed.

```java
ApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-pokertools.xml");
```

Unlike `OnlineGamePurger` which uses try-with-resources, this leaks database connections and Spring resources.

**Fix:** Use try-with-resources like `OnlineGamePurger` does.

**Priority:** Medium - Resource leak

---

### CONCURRENCY-2: SimpleDateFormat Thread Safety
**File:** `OnlineGamePurger.java:52`
**Issue:** Static `SimpleDateFormat` is not thread-safe.

```java
private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
```

While `OnlineGamePurger` is a command-line tool (single-threaded), this is a latent defect if class is ever used in concurrent context.

**Fix:** Use `DateTimeFormatter` (thread-safe) or `ThreadLocal<SimpleDateFormat>`.

**Priority:** Medium - Latent concurrency issue

---

### SEC-BACKEND-6: Chat Messages Not Validated for Content
**File:** `TcpChatServer.java:475-498`
**Issue:** `handleChat()` broadcasts messages without content validation.

`InputValidator.isValidChatMessage()` exists (checks 1-500 characters) but is never called. No HTML sanitization before broadcast. Malicious HTML/script content could be injected.

**Fix:** Validate chat message length and sanitize HTML content before broadcasting.

**Priority:** Medium - XSS risk

---

### VALIDATION-1: Missing Input Validation in Multiple Servlet Endpoints
**File:** `PokerServlet.java`
**Issue:** Input validation applied to `addOnlineProfile()` and `addWanGame()`, but absent from:

- `resetOnlineProfile()` (line 735): No email validation
- `linkOnlineProfile()` (line 800): No input validation
- `changeOnlineProfilePassword()` (line 884): No password length/complexity validation
- `validateProfile()` (line 616): No input validation

**Fix:** Apply consistent input validation across all endpoints.

**Priority:** Medium - Input validation gaps

---

## P3: Low — Code Quality & Technical Debt

### NAMING-1: Confusing Setter Method Name (Misleading Autowiring)
**File:** `OnlineGameServiceImpl.java:64-71`
**Issue:** Two methods have same name `setOnlineGameDao` with different parameter types.

```java
@Autowired
public void setOnlineGameDao(OnlineGameDao dao) { gameDao = dao; }

@Autowired
public void setOnlineGameDao(OnlineProfileDao dao) { profileDao = dao; }
```

Confusing and violates naming conventions. Second should be `setOnlineProfileDao`.

**Fix:** Rename to `setOnlineProfileDao`.

**Priority:** Low - Naming convention

---

### DEBT-BACKEND-1: Dummy Profile Passwords in Plaintext
**File:** `OnlineProfileImplJpa.java:150-151`
**Issue:** Dummy profile sets both transient `password` field and hash.

```java
profile.setPassword("!!DUMMY!!");
profile.setPasswordHash(BCrypt.hashpw("!!DUMMY!!", BCrypt.gensalt()));
```

Setting transient `password` field is unnecessary and leaves value in memory.

**Fix:** Only set `passwordHash`, not `password`.

**Priority:** Low - Unnecessary plaintext storage

---

### CLEANUP-BACKEND-1: Duplicate Switch Cases
**File:** `PokerServlet.java:257-260`
**Issue:** Duplicate cases calling same method.

```java
case OnlineMessage.CAT_WAN_GAME_STOP :
    return endWanGame(ddreceived);
case OnlineMessage.CAT_WAN_GAME_END :
    return endWanGame(ddreceived);
```

Could be combined with fall-through.

**Priority:** Low - Code clarity

---

### CLEANUP-BACKEND-2: Commented-Out Code and Stale Comments
**File:** `PokerServlet.java`
**Issue:** Multiple stale design notes referencing "Doug" (lines 339-342, 419-426, 723-730) and commented-out logger lines.

Comment at line 534: "FIX: make this end-game stuff more robust" indicates acknowledged tech debt.

**Fix:** Remove stale comments or convert to actionable TODOs.

**Priority:** Low - Code clarity

---

### BUG-2: DisallowedManager Comment-Detection Bug
**File:** `DisallowedManager.java:67`
**Issue:** Comment detection checks for literal string instead of using regex.

```java
if (line.startsWith("\\s*#"))
    continue; // comment
```

Checks for literal `\s*#` (metacharacters not interpreted in `startsWith`). Lines beginning with whitespace followed by `#` won't be treated as comments.

**Fix:** Change to `line.startsWith("#")` or use `line.matches("\\s*#.*")`.

**Priority:** Low - Edge case bug

---

### CLEANUP-BACKEND-3: TcpChatServer Sets Global System Property
**File:** `TcpChatServer.java:87`
**Issue:** Setting global system property has side effects.

```java
System.setProperty("settings.server.thread.class", ChatSocketThread.class.getName());
```

Affects all threads in JVM and makes testing fragile. Can interfere with other server instances.

**Fix:** Pass configuration through object hierarchy instead of system properties.

**Priority:** Low - Testing fragility

---

### CLEANUP-BACKEND-4: Hotmail-Specific Logic in Email Sending
**File:** `PokerServlet.java:980-981`
**Issue:** Obsolete workaround for Hotmail.

```java
boolean isHotmail = sTo.toLowerCase().endsWith("@hotmail.com");
String sPlainText = (isHotmail) ? null : email.getPlain();
```

Special-cases Hotmail (now Outlook) to avoid multipart emails. Likely obsolete as Hotmail has handled multipart correctly for many years.

**Fix:** Remove special case or verify if still needed.

**Priority:** Low - Obsolete workaround

---

### LEAK-BACKEND-2: BufferedReader Not Closed in DisallowedManager
**File:** `DisallowedManager.java:60-84`
**Issue:** `BufferedReader` wrapping `StringReader` never closed.

While `StringReader` doesn't hold external resources, it's a best practice to close readers.

**Fix:** Use try-with-resources.

**Priority:** Low - Best practice

---

## Testing Coverage Issues

### TEST-1: PokerServlet Has Zero Unit Tests
**File:** `PokerServlet.java`
**Issue:** No unit tests for the entire servlet.

Servlet dispatch, authentication, rate limiting, ban checking, game management, and profile management endpoints have no direct tests.

**Fix:** Create comprehensive servlet tests covering:
- Message dispatch for all 16+ message types
- Authentication flows
- Rate limiting behavior
- Ban checking
- Error handling

**Priority:** High - Critical coverage gap

---

### TEST-2: TcpChatServer Tests Entirely Disabled
**File:** `TcpChatServerTest.java:62`
**Issue:** All 13 chat server tests disabled.

```java
@Disabled("Java 25 incompatibility with Mockito/ByteBuddy")
```

Entire TcpChatServer has zero test coverage. No tests for HELLO/chat handling, broadcast logic, disconnect detection, rate limiting, or debug commands.

**Fix:**
- Upgrade Mockito/ByteBuddy for Java 25 compatibility
- Re-enable tests
- Add coverage for security-critical features (authentication, debug commands)

**Priority:** High - Critical coverage gap

---

### TEST-3: DisallowedManagerTest Uses JUnit 3 API
**File:** `DisallowedManagerTest.java`
**Issue:** Uses JUnit 3 (`extends TestCase`) instead of JUnit 5.

The `@Tag("slow")` JUnit 5 annotation is present but has no effect on JUnit 3 test class.

**Fix:** Migrate to JUnit 5 with `@Test` annotations.

**Priority:** Low - Test modernization

---

### TEST-4: Empty Test Method
**File:** `OnlineProfileServiceDummyTest.java`
**Issue:** Single test has entire body commented out (lines 56-68).

Contributes no coverage.

**Fix:** Either implement the test or remove the file.

**Priority:** Low - Dead code

---

## Big Effort Items (Need Separate Plans)

### BE-BACKEND-1: PokerServlet Refactoring
**File:** `PokerServlet.java`
**Issue:** Monolithic servlet with 988 lines, 16+ message types in giant switch statement.

**Fix:**
- Extract message handlers into separate classes using `MessageHandler` interface
- Separate authentication/authorization concerns
- Improve error handling consistency

**Why separate plan:** Major refactoring affecting all servlet endpoints. Requires comprehensive regression testing.

---

### BE-BACKEND-2: REST API Migration
**Scope:** `PokerServlet.java`, all service layer
**Issue:** No REST API. Custom binary protocol over HTTP with no separation of transport from business logic.

**Fix:**
- Design RESTful API with proper endpoints
- Implement JSON request/response format
- Maintain backward compatibility with existing binary protocol
- Add OpenAPI/Swagger documentation

**Why separate plan:** Significant architectural change. Requires API design, client migration strategy, versioning strategy.

---

### BE-BACKEND-3: Unified Authentication & Authorization Framework
**Scope:** `PokerServlet.java`, `TcpChatServer.java`, service layer
**Issue:** Authentication inconsistent across HTTP/TCP. No authorization framework.

**Fix:**
- Implement consistent authentication across HTTP and TCP
- Add role-based authorization (admin, host, user)
- Enforce authorization checks on all endpoints
- Add audit logging for authentication events

**Why separate plan:** Cross-cutting concern affecting all layers. Requires security design review.

---

## Summary

| Priority | Category | Count |
|----------|----------|-------|
| P0 | Critical security | 3 |
| P1 | High (security, auth, concurrency, DoS) | 4 |
| P2 | Medium (security, performance, bugs, leaks) | 7 |
| P3 | Low (code quality, tech debt) | 9 |
| TEST | Testing gaps | 4 |
| BE | Big effort (separate plans) | 3 |
| | **Total** | **30** |

## Testing Coverage Summary

**Well-tested areas:**
- `OnlineProfileService`: 30+ tests (CRUD, authentication, search, pagination, retirement)
- `OnlineGameService`: 20+ tests (save/update/delete, pagination, host summary, purge)
- `TournamentHistoryService`: 18+ tests (histories, leaderboard, upgrades, mixed player types)
- `PasswordHashingService`: 8 tests (hashing, verification, null safety, edge cases)
- `PokerServer`: 5 tests (admin initialization scenarios)
- DAO layer: Direct tests for `OnlineGame` and `OnlineProfile`

**Critical coverage gaps:**
- **PokerServlet**: Zero unit tests (988 lines untested)
- **TcpChatServer**: All 13 tests disabled (entire server untested)
- **DisallowedManager**: Only 2 JUnit 3 tests (comment regex bug not caught)
- **Command-line tools**: No tests for purger utilities
- **Error paths**: No tests for ban checks, malformed messages, concurrent access, email failures

## Verification

After implementing P0, P1, and P2 fixes:
1. `mvn test -pl pokerserver` — all tests pass
2. `mvn package` — build succeeds with zero new warnings
3. Manual: start server, create profile, login, join chat, send message, create/update/delete game
4. Security: verify debug commands removed/protected, password reset requires verification
5. Performance: measure query count for game list with histories

## Related Plans

- `CODE-REVIEW.md` — General infrastructure review (common, db, gameserver, udp, mail)
- `BCRYPT-PASSWORD-HASHING.md` — Password hashing migration (completed)
- `UNIT-TESTING-PLAN.md` — General testing strategy
