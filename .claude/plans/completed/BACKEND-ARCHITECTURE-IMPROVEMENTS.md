# Backend Architecture Improvements Plan

## Context

This plan covers major architectural improvements to the DDPoker backend that were identified during the backend code review but require separate planning due to their scope and complexity.

**Source:** Extracted from `BACKEND-CODE-REVIEW.md` (now archived)
**Status:** SUPERSEDED by `SERVER-HOSTED-GAME-ENGINE.md` (2026-02-15)

> **Note:** All three items superseded — PokerServlet gets gutted by legacy P2P removal (Milestone 7),
> REST API migration IS Milestone 2 of the server hosting plan, and unified auth IS Phase 2.1 (JWT).
**Created:** 2026-02-13

---

## Overview

The backend code review identified three major architectural improvements that require significant design work, implementation effort, and comprehensive testing:

1. **PokerServlet Refactoring** - Break up 988-line monolith into maintainable components
2. **REST API Migration** - Replace custom binary protocol with modern REST API
3. **Unified Auth Framework** - Consistent authentication/authorization across all entry points

These items were separated from the main code review because each represents a substantial architectural change requiring its own implementation plan.

---

## BE-BACKEND-1: PokerServlet Refactoring

### Current State

**File:** `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServlet.java`

**Problem:** Monolithic servlet with 988 lines handling 16+ message types in a giant switch statement.

```java
switch (nCategory) {
    case OnlineMessage.CAT_APPL_ERROR:
        return processError(ddreceived);
    case OnlineMessage.CAT_WAN_PROFILE_ADD:
        return addOnlineProfile(ddreceived);
    case OnlineMessage.CAT_WAN_PROFILE_UPDATE:
        return updateOnlineProfile(ddreceived);
    // ... 13+ more cases
}
```

**Issues:**
- All message handling logic in single class violates Single Responsibility Principle
- Authentication/authorization mixed with business logic
- Error handling inconsistent across message types
- Difficult to test individual message handlers
- Hard to maintain and extend

### Proposed Solution

**Extract message handlers into separate classes using a `MessageHandler` interface:**

```java
interface MessageHandler {
    OnlineMessage handle(OnlineMessage request, HttpServletRequest httpRequest);
}
```

**Handler examples:**
- `ProfileAddHandler` - Handle profile creation
- `ProfileUpdateHandler` - Handle profile updates
- `GameAddHandler` - Handle game creation
- `GameUpdateHandler` - Handle game updates
- etc.

**Refactored servlet:**
```java
public class PokerServlet extends DDMessageServlet {
    private final Map<Integer, MessageHandler> handlers;

    @Override
    protected OnlineMessage processDD(OnlineMessage msg, HttpServletRequest req) {
        MessageHandler handler = handlers.get(msg.getCategory());
        if (handler == null) {
            return createErrorResponse("Unknown message type");
        }
        return handler.handle(msg, req);
    }
}
```

### Benefits

- **Separation of Concerns:** Each handler responsible for one message type
- **Testability:** Individual handlers can be unit tested in isolation
- **Maintainability:** Easier to find and modify specific functionality
- **Extensibility:** New message types just add new handler classes
- **Consistency:** Enforce consistent error handling and auth patterns

### Implementation Steps

1. **Design Phase:**
   - Define `MessageHandler` interface
   - Define handler lifecycle (constructor injection, initialization)
   - Design authentication/authorization extraction
   - Plan error handling strategy

2. **Incremental Migration:**
   - Start with simple handlers (e.g., `processError`)
   - Extract one handler at a time
   - Maintain backward compatibility
   - Test each handler thoroughly

3. **Authentication/Authorization Extraction:**
   - Create `AuthenticationService` for consistent auth checks
   - Create `AuthorizationService` for permission checks
   - Apply to all handlers consistently

4. **Testing:**
   - Unit tests for each handler
   - Integration tests for servlet dispatch
   - Regression tests to ensure no behavior changes

### Risks & Mitigations

**Risk:** Breaking existing clients during refactoring
**Mitigation:** Maintain exact same request/response format, comprehensive regression testing

**Risk:** Performance degradation from additional abstraction
**Mitigation:** Benchmark before/after, handler map lookup is O(1)

**Risk:** Incomplete extraction leaving hybrid state
**Mitigation:** Complete one message type at a time, each commit leaves system in working state

---

## BE-BACKEND-2: REST API Migration

### Current State

**Scope:** `PokerServlet.java`, all service layer

**Problem:** Custom binary protocol over HTTP with no separation of transport from business logic.

**Current protocol:**
- Binary `OnlineMessage` objects serialized over HTTP
- Tight coupling between transport and business logic
- No API documentation
- Difficult for third-party integration
- Can't use standard HTTP tools (curl, Postman, etc.)

### Proposed Solution

**Design RESTful API with proper endpoints:**

```
POST   /api/v1/profiles              - Create profile
GET    /api/v1/profiles/{id}         - Get profile
PUT    /api/v1/profiles/{id}         - Update profile
DELETE /api/v1/profiles/{id}         - Delete profile
POST   /api/v1/profiles/{id}/password/reset - Reset password

POST   /api/v1/games                 - Create game
GET    /api/v1/games/{id}            - Get game
PUT    /api/v1/games/{id}            - Update game
DELETE /api/v1/games/{id}            - Delete game
GET    /api/v1/games                 - List games (with filters)

POST   /api/v1/auth/login            - Authenticate
POST   /api/v1/auth/logout           - Logout
```

**JSON request/response format:**
```json
POST /api/v1/profiles
{
  "name": "PlayerName",
  "email": "player@example.com",
  "password": "securepassword"
}

Response 201 Created:
{
  "id": "uuid",
  "name": "PlayerName",
  "email": "player@example.com",
  "createdAt": "2026-02-13T12:00:00Z"
}
```

### Implementation Strategy

**Phase 1: Design**
- Define complete API specification
- Choose REST framework (Spring MVC vs JAX-RS)
- Design versioning strategy (URL path vs header)
- Design error response format
- Create OpenAPI/Swagger specification

**Phase 2: Parallel Implementation**
- Implement REST controllers alongside existing servlet
- Reuse existing service layer
- Both APIs available simultaneously
- Test REST API thoroughly

**Phase 3: Client Migration**
- Update desktop client to support both protocols
- Add configuration flag to choose protocol
- Default to legacy protocol initially
- Gradual migration to REST

**Phase 4: Deprecation (Future)**
- Announce legacy protocol deprecation timeline
- Monitor usage metrics
- Eventually remove legacy servlet

### Benefits

- **Standards-based:** Use HTTP methods and status codes correctly
- **Documentation:** OpenAPI spec provides interactive docs
- **Tooling:** Works with standard HTTP tools
- **Third-party Integration:** External tools can integrate easily
- **Testability:** Easier to write API tests

### Risks & Mitigations

**Risk:** Breaking existing clients
**Mitigation:** Maintain both APIs in parallel, gradual migration

**Risk:** Performance concerns with JSON vs binary
**Mitigation:** Benchmark and optimize, modern JSON parsers are fast

**Risk:** Increased complexity maintaining two APIs
**Mitigation:** Share service layer, only transport differs

---

## BE-BACKEND-3: Unified Authentication & Authorization Framework

### Current State

**Scope:** `PokerServlet.java`, `TcpChatServer.java`, service layer

**Problem:** Authentication is inconsistent across HTTP and TCP entry points.

**Current HTTP Authentication:**
```java
OnlineProfile profile = onlineProfileService.authenticateOnlineProfile(
    profileName, password
);
```

**Current TCP Authentication:**
```java
if (user == null || !user.getPassword().equals(auth.getPassword())) {
    // This was already fixed, but shows the inconsistency
}
```

**Authorization Issues:**
- No role-based access control
- No distinction between admin/host/user
- Game mutations don't verify requester is game host
- No audit logging of auth events

### Proposed Solution

**Implement unified authentication framework:**

```java
interface AuthenticationService {
    AuthToken authenticate(String username, String password);
    AuthToken authenticate(String token);
    void logout(AuthToken token);
    boolean isValid(AuthToken token);
}

interface AuthorizationService {
    boolean hasRole(AuthToken token, Role role);
    boolean canModifyGame(AuthToken token, String gameId);
    boolean canModifyProfile(AuthToken token, String profileId);
}

enum Role {
    ADMIN,      // Full system access
    HOST,       // Can create/manage own games
    USER,       // Can join games
    ANONYMOUS   // Limited read-only access
}
```

**Session Management:**
- Token-based authentication (JWT or similar)
- Secure token storage
- Token expiration and refresh
- Revocation support

**Audit Logging:**
```java
interface AuditLogger {
    void logLogin(String username, String ipAddress, boolean success);
    void logLogout(String username);
    void logAuthorizationFailure(String username, String resource, String action);
}
```

### Implementation Steps

1. **Design Phase:**
   - Choose auth mechanism (JWT, session tokens, etc.)
   - Define role hierarchy and permissions
   - Design audit log format and storage
   - Plan migration strategy for existing sessions

2. **Core Implementation:**
   - Implement `AuthenticationService`
   - Implement `AuthorizationService`
   - Implement `AuditLogger`
   - Add database tables for tokens/sessions if needed

3. **Integration:**
   - Update PokerServlet to use new auth framework
   - Update TcpChatServer to use same framework
   - Add authorization checks to all endpoints
   - Add role checks where needed

4. **Testing:**
   - Unit tests for auth services
   - Integration tests for auth flows
   - Security testing (penetration testing)
   - Performance testing (auth overhead)

### Benefits

- **Consistency:** Same auth mechanism everywhere
- **Security:** Role-based access control
- **Auditability:** Track all auth events
- **Flexibility:** Easy to add new roles/permissions
- **Compliance:** Better meets security best practices

### Risks & Mitigations

**Risk:** Breaking existing client sessions
**Mitigation:** Gradual migration, support legacy auth temporarily

**Risk:** Performance overhead from auth checks
**Mitigation:** Cache authorization decisions, benchmark and optimize

**Risk:** Audit log growth
**Mitigation:** Log rotation, retention policies, summary reports

---

## Recommended Implementation Order

**Priority 1: BE-BACKEND-3 (Unified Auth Framework)**
- Addresses security concerns
- Required foundation for other improvements
- Can be implemented independently
- Immediate security benefits

**Priority 2: BE-BACKEND-1 (PokerServlet Refactoring)**
- Makes code more maintainable
- Easier to implement REST API after refactoring
- Improves testability
- Reduces technical debt

**Priority 3: BE-BACKEND-2 (REST API Migration)**
- Builds on refactored servlet
- Benefits from unified auth
- Long-term improvement
- Can be done in phases

---

## Related Plans

- `BACKEND-CODE-REVIEW.md` (archived) - Original code review that identified these items
- `CODE-REVIEW.md` - General infrastructure review

---

## Next Steps

1. Review and approve this plan
2. Create detailed implementation plan for BE-BACKEND-3 (Unified Auth Framework)
3. Begin implementation in priority order
4. Archive BACKEND-CODE-REVIEW.md plan as complete
