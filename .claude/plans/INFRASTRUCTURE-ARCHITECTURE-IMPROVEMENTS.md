# Infrastructure Architecture Improvements Plan

## Context

This plan covers major architectural improvements to DDPoker's core infrastructure that were identified during the general code review but require separate planning due to their scope and complexity.

**Source:** Extracted from `CODE-REVIEW.md` (now archived)
**Status:** draft
**Created:** 2026-02-13

---

## Overview

The general code review identified five major architectural improvements that require significant design work, implementation effort, and comprehensive testing:

1. **EngineServlet Refactoring** - Break up 476-line monster method into maintainable components
2. **Chat Message Handling Refactor** - Replace 125-line switch with strategy pattern
3. **Authentication System Redesign** - Implement consistent player identity validation
4. **UDP Networking Concurrency Overhaul** - Fix deep concurrency issues in UDP layer
5. **Database Resource Management Modernization** - Complete migration to try-with-resources pattern

These items were separated from the main code review because each represents a substantial architectural change requiring its own implementation plan.

---

## BE-1: EngineServlet Monster Method Refactor

### Current State

**File:** `code/gameserver/src/main/java/com/donohoedigital/games/server/EngineServlet.java`

**Problem:** Monolithic message processing with two massive methods:
- `_processMessage()`: 476 lines (lines 254-730)
- `processExistingGameMessageLocked()`: 167 lines

**Issues:**
- Single method handles 10+ different message categories
- All game state logic intermixed with message dispatch
- Complex nested conditionals and error handling
- Impossible to unit test individual message handlers
- Lock held during entire message processing (performance bottleneck)
- Very difficult to understand and modify

### Proposed Solution

**Extract message handlers using Command/Strategy pattern:**

```java
interface MessageHandler {
    OnlineMessage handle(OnlineMessage request, GameEngine engine);
}
```

**Handler examples:**
- `JoinGameHandler` - Handle player joining game
- `PlayerActionHandler` - Process player actions (bet, fold, etc.)
- `ChatMessageHandler` - Handle chat messages
- `GameStateRequestHandler` - Handle game state queries
- etc.

**Refactored servlet:**
```java
public class EngineServlet extends DDMessageServlet {
    private final Map<Integer, MessageHandler> handlers;

    @Override
    protected OnlineMessage _processMessage(OnlineMessage msg) {
        MessageHandler handler = handlers.get(msg.getCategory());
        if (handler == null) {
            return createErrorResponse("Unknown message type");
        }
        return handler.handle(msg, gameEngine);
    }
}
```

### Benefits

- **Separation of Concerns:** Each handler responsible for one message type
- **Testability:** Individual handlers can be unit tested in isolation
- **Performance:** Lock granularity can be optimized per handler
- **Maintainability:** Easier to find and modify specific functionality
- **Extensibility:** New message types just add new handler classes

### Implementation Steps

1. **Design Phase:**
   - Define `MessageHandler` interface
   - Identify all message categories and their responsibilities
   - Design handler lifecycle and dependencies
   - Plan lock/synchronization strategy per handler

2. **Incremental Migration:**
   - Start with simplest handlers (error messages, pings)
   - Extract one handler at a time
   - Maintain backward compatibility
   - Test each handler thoroughly before moving to next

3. **Lock Optimization:**
   - Move expensive operations (serialization, I/O) outside locks
   - Use read/write locks where appropriate
   - Reduce lock contention for concurrent games

4. **Testing:**
   - Unit tests for each handler
   - Integration tests for servlet dispatch
   - Load testing to verify performance improvements
   - Regression tests to ensure no behavior changes

### Risks & Mitigations

**Risk:** Breaking existing game clients
**Mitigation:** Maintain exact same request/response protocol, comprehensive regression testing

**Risk:** Performance degradation from handler dispatch overhead
**Mitigation:** Benchmark before/after, handler map lookup is O(1), lock optimization should improve overall performance

**Risk:** Incomplete extraction leaving hybrid implementation
**Mitigation:** Complete one message type at a time, each commit leaves system functional

---

## BE-2: Chat Message Handling Refactor

### Current State

**File:** `code/gameserver/src/main/java/com/donohoedigital/games/server/ChatServer.java`

**Problem:** 125-line switch statement in `processMessage()` (lines 144-269) handling multiple message types with mixed concerns.

**Issues:**
- Single method handles all chat message types
- Message validation mixed with business logic
- Error handling inconsistent across message types
- Difficult to test individual message handlers
- Adding new message types requires modifying large switch statement

### Proposed Solution

**Strategy pattern with message-type handlers:**

```java
interface ChatMessageHandler {
    void handle(Peer2PeerMessage message, ChatSession session);
}
```

**Handler examples:**
- `HelloMessageHandler` - Client authentication
- `ChatTextHandler` - Chat message broadcast
- `PrivateMessageHandler` - Direct messages
- `TypingIndicatorHandler` - Typing notifications
- `GoodbyeMessageHandler` - Graceful disconnect

**Refactored chat server:**
```java
public class ChatServer {
    private final Map<Integer, ChatMessageHandler> handlers;

    private void processMessage(Peer2PeerMessage msg, ChatSession session) {
        ChatMessageHandler handler = handlers.get(msg.getType());
        if (handler == null) {
            logger.warn("Unknown message type: {}", msg.getType());
            return;
        }
        handler.handle(msg, session);
    }
}
```

### Benefits

- **Cleaner Code:** Each handler focuses on single message type
- **Easier Testing:** Mock ChatSession, test each handler independently
- **Better Error Handling:** Consistent error handling per handler
- **Extensibility:** Add new message types without modifying switch

### Implementation Steps

1. Define `ChatMessageHandler` interface
2. Extract handlers one message type at a time
3. Add comprehensive unit tests for each handler
4. Maintain backward compatibility with existing protocol

---

## BE-3: Authentication System Redesign

### Current State

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServlet.java` (line 359)

**Problem:** Design note in code states: *"Our auth logic is kind of a pain and needs a redesign. We should always be sending down the current player."*

**Issues:**
- Inconsistent player identity validation across endpoints
- Sometimes profile sent in request, sometimes looked up by session
- No clear documentation of auth flow
- Integration testing gaps for authentication scenarios
- Confusion about when/how player identity is validated

### Proposed Solution

**Clarify and document authentication flow:**

1. **Session Management:**
   - Define clear session lifecycle (creation, validation, expiration)
   - Document when sessions are created vs validated
   - Implement consistent session token handling

2. **Player Identity:**
   - Always include authenticated player in response when applicable
   - Validate player identity at servlet layer before business logic
   - Clear separation between "authenticated" and "anonymous" requests

3. **Documentation:**
   - Document complete auth flow with sequence diagrams
   - Add JavaDoc explaining authentication requirements per endpoint
   - Create integration tests covering all auth scenarios

### Implementation Strategy

**Phase 1: Audit & Document**
- Map all endpoints and their authentication requirements
- Document current authentication flow (even if inconsistent)
- Identify gaps and inconsistencies

**Phase 2: Design Improvement**
- Design consistent authentication pattern
- Choose between: always require auth, or explicit "anonymous allowed" marking
- Plan backward compatibility strategy

**Phase 3: Implementation**
- Implement authentication interceptor/filter
- Update endpoints to use consistent pattern
- Add comprehensive integration tests

**Phase 4: Cleanup**
- Remove obsolete authentication code
- Update documentation
- Remove design note comment

### Benefits

- **Clarity:** Clear understanding of authentication requirements
- **Security:** Consistent validation reduces auth bypass risks
- **Maintainability:** New endpoints follow established pattern
- **Testing:** Comprehensive integration tests catch auth issues

---

## BE-4: UDP Networking Concurrency Overhaul

### Current State

**Scope:** `code/udp/` — `UDPLink.java`, `UDPServer.java`, `UDPManager.java`, `Peer2PeerMulticast.java`

**Problems:**

1. **UDPLink sendQueue_ (line 80):** `LinkedList` with inconsistent synchronization
   - Some methods synchronize on `sendQueue_`, others don't
   - Race conditions between add and remove operations
   - Potential for lost messages or duplicates

2. **Message ID Rollover (line 560):** No handling when message ID counter wraps
   - Just logs warning when rollover occurs
   - Could cause message ordering issues
   - No clear plan for rollover recovery

3. **Hello Handshake Race Conditions:**
   - `isHelloReceived()` check followed by operations assuming hello completed
   - Time-of-check vs time-of-use (TOCTOU) race
   - Could lead to messages sent before connection fully established

4. **Mixed Synchronization Patterns:**
   - Some use `synchronized` methods
   - Some use `synchronized` blocks on different objects
   - Some use volatile flags
   - No consistent concurrency strategy

### Proposed Solution

**Modernize with java.util.concurrent:**

```java
// Replace LinkedList with concurrent queue
private final BlockingQueue<UDPMessage> sendQueue =
    new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

// Replace synchronized collections
private final ConcurrentHashMap<Integer, UDPLink> activeLinks =
    new ConcurrentHashMap<>();

// Use AtomicInteger for message IDs with rollover handling
private final AtomicInteger messageIdGenerator = new AtomicInteger(0);

private int nextMessageId() {
    return messageIdGenerator.updateAndGet(id ->
        (id >= Integer.MAX_VALUE - 1000) ? 0 : id + 1
    );
}
```

**State machine for connection lifecycle:**
```java
enum ConnectionState {
    CONNECTING,      // Initial state
    HELLO_SENT,      // Sent hello, waiting for response
    HELLO_RECEIVED,  // Received hello, connection active
    CLOSING,         // Goodbye sent, waiting for close
    CLOSED           // Connection terminated
}

private final AtomicReference<ConnectionState> state =
    new AtomicReference<>(ConnectionState.CONNECTING);
```

### Implementation Steps

1. **Replace Synchronized Collections:**
   - `LinkedList sendQueue_` → `BlockingQueue`
   - `HashMap` → `ConcurrentHashMap`
   - Test thoroughly with concurrent load

2. **Fix Message ID Rollover:**
   - Use `AtomicInteger` with wrap-around logic
   - Add tests for rollover scenarios

3. **Implement State Machine:**
   - Define clear connection states
   - Use atomic state transitions
   - Eliminate TOCTOU races

4. **Comprehensive Concurrent Testing:**
   - Multi-threaded stress tests
   - Race condition detection tools
   - Network partition simulation

### Important Note

**UDP Replacement:** If UDP is being replaced with TCP (per `UDP-TO-TCP-CONVERSION.md` plan), this effort may not be worthwhile. Check UDP deprecation timeline before starting this work.

**If UDP is deprecated:** Focus on TCP implementation and plan UDP removal rather than fixing these issues.

---

## BE-5: Database Resource Management Modernization

### Current State

**Scope:** `code/db/`, `code/poker/` — `DatabaseQuery.java`, `PokerDatabase.java`, and all DAO classes

**Problems:**

1. **DatabaseQuery.java:**
   - Class-level `@SuppressWarnings("JDBCResourceOpenedButNotSafelyClosed")` at line 46
   - Suppressing warnings instead of fixing resource leaks
   - `close()` method at line 588 closes `PreparedStatement` and `Connection` but not `ResultSet`
   - Relies on implicit close which isn't guaranteed by all JDBC drivers

2. **PokerDatabase.java:**
   - 10+ methods with potential resource leak paths
   - Resources closed in try block body instead of finally/try-with-resources
   - If exception occurs before close(), resource leaks

3. **Manual Resource Management:**
   - Widespread manual try/catch/finally instead of try-with-resources
   - Inconsistent patterns across DAO classes
   - Error-prone and verbose

### Proposed Solution

**Standardize on try-with-resources pattern:**

```java
// BEFORE (error-prone):
Connection conn = null;
PreparedStatement stmt = null;
ResultSet rs = null;
try {
    conn = getConnection();
    stmt = conn.prepareStatement(sql);
    rs = stmt.executeQuery();
    // process results
} catch (SQLException e) {
    logger.error("Error", e);
} finally {
    if (rs != null) try { rs.close(); } catch (SQLException e) {}
    if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
    if (conn != null) try { conn.close(); } catch (SQLException e) {}
}

// AFTER (clean and safe):
try (Connection conn = getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {
    // process results
} catch (SQLException e) {
    logger.error("Error", e);
}
```

### Implementation Steps

1. **Audit All Database Code:**
   - Identify all manual resource management
   - Prioritize based on leak risk (high-frequency queries first)
   - Document C3P0 connection pool behavior

2. **Incremental Migration:**
   - Start with highest-risk methods
   - Convert one class/method at a time
   - Test thoroughly after each change
   - **Note:** MF-1 and MF-2 already completed highest-priority fixes

3. **Remove Suppression Warnings:**
   - After fixing resource leaks, remove `@SuppressWarnings` annotations
   - Enable JDBC resource warnings in IDE/SpotBugs

4. **Testing:**
   - Unit tests verifying resources closed on success
   - Unit tests verifying resources closed on exception
   - Connection pool monitoring to detect leaks
   - Load testing to verify no connection pool exhaustion

### Progress So Far

✅ **MF-1 (Completed):** Fixed `DatabaseQuery` and `ResultMap` resource leaks (Feb 12, 2026)
✅ **MF-2 (Completed):** Modernized all 28 methods in `PokerDatabase.java` (Feb 12, 2026)

**Remaining Work:**
- Audit other DAO classes for similar patterns
- Remove class-level suppression warnings
- Standardize connection lifecycle across all database code

### Benefits

- **Correctness:** Guaranteed resource cleanup, no leaks
- **Simplicity:** Try-with-resources is more concise and readable
- **Safety:** Compiler enforces AutoCloseable interface
- **Performance:** No connection pool exhaustion from leaked connections

---

## Recommended Implementation Order

**Priority 1: BE-3 (Authentication System Redesign)**
- Clarifies confusing area of codebase
- Improves security posture
- Foundation for other improvements
- Mostly documentation and testing (lower risk)

**Priority 2: BE-5 (Database Resource Management)**
- Highest-priority fixes already complete (MF-1, MF-2)
- Remaining work is cleanup and standardization
- Low risk, high value

**Priority 3: BE-1 (EngineServlet Refactoring)**
- Significant maintainability improvement
- Performance benefits from lock optimization
- Makes testing easier
- High effort but clear benefits

**Priority 4: BE-2 (Chat Message Handling)**
- Similar to BE-1 but smaller scope
- Can learn from EngineServlet refactoring

**Priority 5: BE-4 (UDP Networking) - CONDITIONAL**
- **Only if UDP is not being deprecated**
- Check `UDP-TO-TCP-CONVERSION.md` plan status first
- If UDP is being replaced, skip this entirely

---

## Related Plans

- `CODE-REVIEW.md` (archived) - Original code review that identified these items
- `BACKEND-ARCHITECTURE-IMPROVEMENTS.md` - Backend-specific architectural improvements
- `UDP-TO-TCP-CONVERSION.md` - UDP replacement plan (affects BE-4 priority)

---

## Next Steps

1. Review and approve this plan
2. **Check UDP deprecation status** before planning BE-4
3. Create detailed implementation plan for BE-3 (Authentication System Redesign)
4. Begin implementation in priority order
5. Archive CODE-REVIEW.md plan as complete
