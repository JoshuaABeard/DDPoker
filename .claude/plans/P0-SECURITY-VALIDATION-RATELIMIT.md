# P0 Security Fixes: Input Validation & Rate Limiting

**Status:** Planning
**Priority:** P0 (Critical - Security & Stability)
**Effort:** Medium (3-5 hours)
**Items:** SEC-2, SEC-3 from CODE-REVIEW.md

## Summary

Implement input validation and rate limiting for servlet endpoints to prevent DoS attacks and invalid data injection.

## Current Issues

### SEC-2: Missing Input Validation
- **Email validation**: No format checking in `joinOnlineGame()` (EngineServlet.java:1015)
- **String length limits**: No bounds on user-provided strings (profile names, game names, etc.)
- **Parameter validation**: No validation in `addOnlineProfile()`, `addWanGame()`

**Impact:** Malformed data can be stored in database, potential for XSS or injection attacks

### SEC-3: No Rate Limiting
- **Profile operations**: Users can spam profile creation/updates (PokerServlet.java)
- **Chat messages**: No frequency limits on chat (ChatServer.java)

**Impact:** DoS vulnerability - attackers can exhaust server resources

## Approach

### SEC-2: Input Validation Strategy

**Option 1: Use Jakarta Bean Validation (JSR-380)**
- Pros: Standard, declarative, well-tested
- Cons: Adds dependency, requires annotations on DTOs
- Verdict: ❌ Over-engineering for servlet code

**Option 2: Custom validation utility class**
- Pros: Simple, focused, no new dependencies
- Cons: Need to write validators
- Verdict: ✅ **CHOSEN** - Matches existing codebase style

**Implementation:**
1. Create `InputValidator` utility class in `common` module
2. Add methods: `isValidEmail()`, `isValidStringLength()`, `isValidInt()`
3. Use in servlets before processing requests
4. Return clear error messages for invalid input

### SEC-3: Rate Limiting Strategy

**Option 1: Use Guava RateLimiter**
- Pros: Battle-tested, simple API
- Cons: Adds Guava dependency (already in use?)
- Verdict: Check if Guava is available

**Option 2: Custom token bucket implementation**
- Pros: No dependencies, full control
- Cons: More code to maintain
- Verdict: Fallback if Guava not available

**Option 3: Simple time-window counting**
- Pros: Very simple, good enough for MVP
- Cons: Less sophisticated than token bucket
- Verdict: ✅ **CHOSEN** - Start simple, can enhance later

**Implementation:**
1. Create `RateLimiter` class with ConcurrentHashMap tracking
2. Key by IP address or user ID
3. Track: requests per time window (e.g., 10 requests/minute)
4. Return 429 (Too Many Requests) when exceeded
5. Auto-cleanup old entries to prevent memory leak

## Files to Modify

### New Files
- `code/common/src/main/java/com/donohoedigital/base/InputValidator.java`
- `code/common/src/main/java/com/donohoedigital/base/RateLimiter.java`

### Modified Files
- `code/gameserver/src/main/java/com/donohoedigital/games/server/EngineServlet.java`
- `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServlet.java`
- `code/udp/src/main/java/com/donohoedigital/udp/ChatServer.java`

### Test Files (new)
- `code/common/src/test/java/com/donohoedigital/base/InputValidatorTest.java`
- `code/common/src/test/java/com/donohoedigital/base/RateLimiterTest.java`

## Implementation Plan

### Phase 1: Input Validation (TDD)

1. **Write InputValidator tests**
   - Email validation (RFC 5322 subset)
   - String length limits (min/max)
   - Integer bounds checking
   - Null/empty handling

2. **Implement InputValidator**
   - Use regex for email (simple pattern, not full RFC)
   - String length: configurable min/max
   - Int bounds: configurable range

3. **Add validation to servlets**
   - EngineServlet.joinOnlineGame() - email validation
   - PokerServlet.addOnlineProfile() - name length, email format
   - PokerServlet.addWanGame() - game name length
   - Return clear error messages (not stack traces)

4. **Test**
   - Unit tests for InputValidator
   - Integration tests for servlet endpoints
   - Verify error responses

### Phase 2: Rate Limiting (TDD)

1. **Write RateLimiter tests**
   - Basic rate limiting (requests per window)
   - Auto-cleanup of old entries
   - Thread safety
   - Different keys (IP, user ID)

2. **Implement RateLimiter**
   - ConcurrentHashMap<String, RateLimitEntry>
   - RateLimitEntry: { count, windowStart }
   - Method: `boolean allowRequest(String key, int maxRequests, long windowMs)`
   - Scheduled cleanup task for old entries

3. **Add rate limiting to servlets**
   - PokerServlet.addOnlineProfile() - 5 requests/minute per IP
   - PokerServlet.updateOnlineProfile() - 10 requests/minute per user
   - ChatServer.processMessage() - 30 messages/minute per user
   - Return HTTP 429 when exceeded

4. **Test**
   - Unit tests for RateLimiter
   - Load tests to verify limits work
   - Verify cleanup doesn't leak memory

### Phase 3: Integration & Documentation

1. **Integration testing**
   - Test validation + rate limiting together
   - Verify no performance degradation
   - Check error messages are user-friendly

2. **Documentation**
   - Add javadoc to new classes
   - Document rate limit values (where configurable)
   - Update CODE-REVIEW.md to mark items complete

## Validation Rules

### Email Format
- Regex: `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$`
- Max length: 254 characters (RFC 5321)

### String Lengths
- Profile name: 1-50 characters
- Game name: 1-100 characters
- Chat message: 1-500 characters

### Rate Limits
- Profile creation: 5/minute per IP
- Profile update: 10/minute per user
- Chat messages: 30/minute per user

## Success Criteria

- [ ] All validation rules implemented and tested
- [ ] Rate limiting functional for all identified endpoints
- [ ] All existing tests still pass
- [ ] New tests achieve >80% coverage on new code
- [ ] No performance degradation (< 1ms overhead per request)
- [ ] Error messages are clear and don't expose internals
- [ ] Memory doesn't leak (cleanup task works)

## Risks & Mitigations

**Risk:** Rate limiting by IP can block legitimate users behind NAT
**Mitigation:** Use generous limits, add user-level limits where possible

**Risk:** Regex validation might be too strict/loose
**Mitigation:** Start conservative, can adjust based on real-world usage

**Risk:** Memory leak from rate limiter map
**Mitigation:** Implement cleanup task, add max entries cap

## Testing Strategy

1. **Unit tests** - InputValidator, RateLimiter classes
2. **Integration tests** - Servlet endpoints with validation
3. **Load tests** - Verify rate limiting works under load
4. **Security tests** - Try to bypass validation/rate limiting

## Notes

- Keep validation simple - don't over-engineer
- Rate limits should be configurable (future enhancement)
- Consider logging validation failures for security monitoring
- Don't expose sensitive info in error messages
