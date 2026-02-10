# Fix Public IP Detection - Return Public IP Instead of Local Address

## Context

The DDPoker application has a "Test Online" feature in the game preferences that allows users to verify their network connectivity for hosting games. When users click this button, it should return their **public IP address** so others know how to connect to their hosted game.

**Current Problem:** The feature returns the local/private IP address in both test scenarios instead of the public IP address. This makes it impossible for users to determine their actual public-facing IP address for game hosting.

**Root Cause:** The server-side implementation in `EngineServlet.getPublicIP()` uses `request.getRemoteAddr()`, which returns the immediate client's IP address from the HTTP request. This works correctly when the client connects directly, but fails to return the public IP in common deployment scenarios:
- When behind a proxy or load balancer
- When behind NAT/router (returns private IP like 192.168.x.x or 10.x.x.x)
- When using reverse proxy configurations

**User Impact:** Users cannot determine their public IP address for hosting games, making it difficult for other players to connect to their hosted tournaments.

---

## Current Implementation Analysis

### Key Files Involved

1. **`code/gameserver/src/main/java/com/donohoedigital/games/server/EngineServlet.java`** (lines 1272-1279)
   - Contains `getPublicIP()` method - **THIS IS WHERE THE BUG IS**
   - Currently uses `request.getRemoteAddr()`

2. **`code/poker/src/main/java/com/donohoedigital/games/poker/online/GetPublicIP.java`**
   - Client-side dialog that sends `CAT_PUBLIC_IP` request to server
   - Receives IP address from server response

3. **`code/poker/src/main/java/com/donohoedigital/games/poker/GamePrefsPanel.java`** (lines 641-649)
   - `testConnection()` method invoked by "Test Online" button
   - Calls GetPublicIP dialog and retrieves IP from response

4. **`code/poker/src/main/java/com/donohoedigital/games/poker/online/TestPublicConnect.java`**
   - Tests public connectivity and displays status
   - Part of the connection testing flow

5. **`code/gametools/src/main/java/com/donohoedigital/games/tools/WhatIsMyIp.java`**
   - Command-line utility for IP detection
   - Uses same server endpoint

### Current Data Flow

```
User clicks "Test Online"
    ↓
GamePrefsPanel.testConnection()
    ↓
GetPublicIP dialog sends CAT_PUBLIC_IP message to server
    ↓
EngineServlet.processMessage() routes to getPublicIP()
    ↓
EngineServlet.getPublicIP() calls request.getRemoteAddr() ← BUG IS HERE
    ↓
Returns private/local IP instead of public IP
    ↓
Client receives and displays incorrect IP
```

---

## Proposed Solution

**Have Client Query External IP Detection Service Directly**

The DDPoker architecture is peer-to-peer: when a user hosts a game, the game server runs **on their local machine**, and other players connect directly to **that user's public IP address**. Therefore, the "Test Online" feature needs to return the **client's own public IP**, not the game server's IP.

The current server-side approach (`request.getRemoteAddr()`) fails because:
1. The game server servlet and client often run on the same machine (returns 127.0.0.1)
2. When behind NAT, the servlet sees the client's private IP (192.168.x.x)
3. The servlet has no way to determine the client's public-facing IP

The solution is to have the **client directly** query an external "What is my IP" service to discover its own public IP address.

### Why This Approach?

1. **Correct for P2P architecture**: The client discovers its own public IP, which is what other players need to connect
2. **Works with NAT**: External services see the client's public IP after NAT translation
3. **Works for all scenarios**: Whether client is localhost, behind NAT, or has direct public IP
4. **Simple & reliable**: Uses existing `DDHttpClient` infrastructure in the codebase
5. **No server changes needed**: Pure client-side solution

### Trade-offs
- ✅ Architecturally correct for P2P game hosting
- ✅ Works for all NAT scenarios (most users)
- ✅ Simple implementation using existing HTTP client code
- ✅ Can cache results to minimize external calls
- ✅ No server-side changes required (simpler deployment)
- ⚠️ Adds external dependency (mitigated by fallback to multiple services)
- ⚠️ Slightly slower first call (mitigated by caching)

---

## Recommended Implementation Plan (TDD Approach)

Following Test-Driven Development principles, we'll write tests FIRST, then implement the code to make them pass.

**CRITICAL: Do NOT write any production code until tests are written and failing (RED phase).**

The TDD cycle:
1. **RED** → Write a failing test for desired behavior
2. **GREEN** → Write minimal code to make the test pass
3. **REFACTOR** → Clean up code while keeping tests green
4. **REPEAT** → Next test, back to RED

This ensures:
- ✅ All code is testable by design
- ✅ High test coverage (every line has a test)
- ✅ Confidence in refactoring (tests catch regressions)
- ✅ Better design (tests reveal tight coupling)

### Step 0: Write Tests FIRST (RED Phase)

Create test file: `code/poker/src/test/java/com/donohoedigital/games/poker/online/PublicIPDetectorTest.java`

**Test cases to write before any implementation:**

1. **Test: Successful IP fetch from ipify.org**
   - Mock HTTP response with valid IP (e.g., "203.0.113.42")
   - Assert: Returns the public IP string

2. **Test: Fallback to icanhazip.com when ipify fails**
   - Mock ipify.org to throw IOException
   - Mock icanhazip.com to return valid IP
   - Assert: Returns the public IP from backup service

3. **Test: Fallback to checkip.amazonaws.com when first two fail**
   - Mock ipify.org and icanhazip.com to fail
   - Mock checkip.amazonaws.com to return valid IP
   - Assert: Returns the public IP from third service

4. **Test: Reject private IP addresses (192.168.x.x)**
   - Mock service to return "192.168.1.100"
   - Assert: Returns null (invalid, should not use private IPs)

5. **Test: Reject private IP addresses (10.x.x.x)**
   - Mock service to return "10.0.0.5"
   - Assert: Returns null

6. **Test: Reject localhost (127.0.0.1)**
   - Mock service to return "127.0.0.1"
   - Assert: Returns null

7. **Test: Cache returns same IP within TTL**
   - Fetch IP first time (mock service called)
   - Fetch IP second time within 5 minutes (mock service NOT called, uses cache)
   - Assert: Both return same IP, service only called once

8. **Test: Cache expires after TTL**
   - Fetch IP first time (mock service called)
   - Advance time by 6 minutes (beyond TTL)
   - Fetch IP second time (mock service called again)
   - Assert: Service called twice

9. **Test: Returns null when all services fail**
   - Mock all three services to throw IOExceptions
   - Assert: Returns null (will fallback to server method in caller)

10. **Test: Validate IPv4 format**
    - Mock service to return invalid format "abc.def.ghi.jkl"
    - Assert: Returns null

**Run tests - they should all FAIL** (RED phase) because no implementation exists yet.

---

### Step 1: Create PublicIPDetector Utility Class (GREEN Phase)

Create new utility class: `code/poker/src/main/java/com/donohoedigital/games/poker/online/PublicIPDetector.java`

This class will contain:
```java
public class PublicIPDetector {
    // Caching fields
    private static String cachedPublicIP = null;
    private static long cachedPublicIPTimestamp = 0;
    private static final long CACHE_TTL_MILLIS;

    // Service URLs
    private static final String[] IP_SERVICES;

    static {
        CACHE_TTL_MILLIS = PropertyConfig.getIntegerProperty("settings.publicip.cache.ttl", 300000);
        String primaryUrl = PropertyConfig.getProperty("settings.publicip.service.url", "https://api.ipify.org");
        IP_SERVICES = new String[] { primaryUrl, "https://icanhazip.com", "https://checkip.amazonaws.com" };
    }

    /**
     * Fetch public IP from external service with caching
     */
    public static String fetchPublicIP() {
        // Check cache first
        if (cachedPublicIP != null && (System.currentTimeMillis() - cachedPublicIPTimestamp) < CACHE_TTL_MILLIS) {
            return cachedPublicIP;
        }

        // Try each service in order
        for (String serviceUrl : IP_SERVICES) {
            String ip = tryFetchFromService(serviceUrl);
            if (ip != null && isValidPublicIP(ip)) {
                // Cache and return
                cachedPublicIP = ip;
                cachedPublicIPTimestamp = System.currentTimeMillis();
                return ip;
            }
        }

        return null; // All services failed
    }

    private static String tryFetchFromService(String url) { /* Use DDHttpClient */ }
    private static boolean isValidPublicIP(String ip) { /* Validate format & not private */ }
    private static boolean isPrivateIP(String ip) { /* Check RFC 1918 ranges */ }
}
```

**Write minimal code to make tests pass** (GREEN phase).

---

### Step 2: Integrate PublicIPDetector into GetPublicIP (GREEN Phase)

Modify `GetPublicIP.java` to use the new utility:

Override the `start()` method to fetch IP directly from external service:
```java
@Override
public void start() {
    if (!isFaceless()) {
        // Show dialog UI (if not faceless mode)
        super.start();
    }

    // Update status to show we're fetching IP
    updateStep(DDMessageListener.STEP_CONNECTING);

    // Fetch public IP from external service directly (don't contact game server)
    String publicIP = PublicIPDetector.fetchPublicIP();

    if (publicIP != null) {
        // Success - create message with result
        updateStep(DDMessageListener.STEP_DONE);
        EngineMessage result = new EngineMessage(EngineMessage.GAME_NOTDEFINED,
                                                  EngineMessage.PLAYER_SERVER,
                                                  EngineMessage.CAT_PUBLIC_IP);
        result.setString(EngineMessage.PARAM_IP, publicIP);
        messageReceived(result); // Simulate successful server response
    } else {
        // Fallback: contact server with original behavior
        // (This maintains backward compatibility)
        logger.warn("Failed to fetch public IP from external services, falling back to server query");
        sendMessage(null);
    }
}
```

**Run tests - they should all PASS** (GREEN phase).

---

### Step 3: Refactor (REFACTOR Phase)

Review the code for:
- Code duplication
- Magic numbers (replace with constants)
- Logging improvements
- Error message clarity
- Thread safety for caching (consider using AtomicReference)

**Run tests again - they should still PASS** after refactoring.

### Step 4: Add Configuration Properties (Support for Testing & Customization)
Add new properties in `code/common/src/main/resources/config/common/common.properties`:
```properties
# External service URL for public IP detection (can be overridden)
settings.publicip.service.url=https://api.ipify.org
# Cache TTL in milliseconds (default 5 minutes)
settings.publicip.cache.ttl=300000
```

This allows:
- Users to configure their own IP detection service if needed
- Adjustable cache TTL
- Easy testing/mocking

### Step 5: Integration Test with GetPublicIP

Write integration test: `code/poker/src/test/java/com/donohoedigital/games/poker/online/GetPublicIPIntegrationTest.java`

Test that `GetPublicIP` dialog:
- Correctly calls `PublicIPDetector.fetchPublicIP()`
- Populates result message with IP address
- Falls back to server query when external services fail
- Shows appropriate UI status messages

### Step 6: Manual End-to-End Testing

After all unit/integration tests pass:
1. Run the poker client application
2. Go to Preferences → Online tab
3. Click "Test Online" button
4. Verify it returns your actual public IP (not 192.168.x.x or 127.0.0.1)
5. Compare with whatismyip.com to confirm accuracy

### Step 7: No Server Changes Needed

Keep `EngineServlet.getPublicIP()` unchanged for backward compatibility:
- Serves as fallback if external services fail
- Maintains compatibility with older clients
- No deployment dependencies

---

## Critical Files to Modify

1. **`code/poker/src/main/java/com/donohoedigital/games/poker/online/GetPublicIP.java`**
   - Add `fetchPublicIPFromExternalService()` method
   - Add caching fields (cachedPublicIP, cachedPublicIPTimestamp, CACHE_TTL_MILLIS)
   - Add `isPrivateIP(String)` validation method
   - Modify `finish()` or appropriate lifecycle method to query external service first
   - Fall back to server query if external services fail

2. **`code/common/src/main/resources/config/common/common.properties`**
   - Add `settings.publicip.service.url` property (default: https://api.ipify.org)
   - Add `settings.publicip.cache.ttl` property (default: 300000 = 5 minutes)

3. **`code/gameserver/src/main/java/com/donohoedigital/games/server/EngineServlet.java`** (lines 1272-1279)
   - NO CHANGES (keep as fallback for backward compatibility)
   - Or optionally add deprecation comment

## Existing Utilities to Reuse

1. **`code/common/src/main/java/com/donohoedigital/comms/DDHttpClient.java`**
   - Use for making HTTP GET requests to external IP services
   - Already handles timeouts, DNS resolution, connection management
   - Example usage pattern available in codebase

---

## Verification Plan (TDD Red-Green-Refactor Cycle)

### Phase 1: RED - Write Tests First
1. **Write all unit tests** in `PublicIPDetectorTest.java` (10 test cases listed above)
2. **Run tests** → All should FAIL (expected, no implementation yet)
3. **Confirm test failures** are due to missing implementation, not test bugs

### Phase 2: GREEN - Implement to Pass Tests
1. **Implement `PublicIPDetector` class** with minimal code
2. **Run tests after each method** implemented
3. **Fix implementation** until all tests PASS
4. **Do NOT refactor yet** - just make tests pass

### Phase 3: REFACTOR - Clean Up Code
1. **Refactor for clarity** (remove duplication, extract methods, improve names)
2. **Run tests after each refactor** → Must stay GREEN
3. **Add logging, error handling** without breaking tests
4. **Improve thread safety** (use synchronized or AtomicReference for cache)

### Phase 4: Integration Testing
1. **Write integration test** for `GetPublicIP` dialog
2. **Run integration test** → Should pass if unit tests pass
3. **Test with real external services** (not mocked) - use `@Tag("integration")` to separate from unit tests

### Phase 5: Manual End-to-End Testing
1. **Start poker client application**
2. **Click "Test Online" button** in Preferences → Online tab
3. **Verify returned IP:**
   - Should show public IP (check against whatismyip.com)
   - Should NOT show 192.168.x.x, 10.x.x.x, or 127.0.0.1
4. **Test caching:**
   - Click "Test Online" twice quickly → Second call should be instant (cached)
   - Wait 6 minutes, click again → Should query service again
5. **Test fallback:**
   - Temporarily block external services (firewall/hosts file)
   - Click "Test Online" → Should fall back to server method

### Success Criteria (in TDD Order)
✅ All 10 unit tests pass (RED → GREEN)
✅ Code coverage >80% for `PublicIPDetector` class
✅ Integration tests pass
✅ Public IP returned when behind NAT (192.168.x.x users get real public IP)
✅ Works when client and server on same machine (127.0.0.1 scenario)
✅ External IP service successfully queried and cached
✅ Cache expiration works correctly (5-minute TTL)
✅ Private IPs filtered out from responses
✅ Fallback to server method works when all external services fail
✅ Existing tests still pass (no regressions)
✅ Manual E2E test confirms correct behavior

---

## Implementation Details

### External IP Service Selection

**Recommended: `https://api.ipify.org`**
- Free, simple, reliable service
- Returns plain text IP address (e.g., "203.0.113.42")
- HTTPS for security
- No rate limits for reasonable usage
- Used by millions of applications
- Fallback options if primary fails:
  - `https://icanhazip.com` (plain text)
  - `https://checkip.amazonaws.com` (AWS-backed)

### Response Parsing

All three services return plain text IP address with newline:
```
203.0.113.42\n
```

Simple parsing:
```java
String response = readResponseAsString(inputStream);
String ip = response.trim();
// Validate IP format before returning
```

### IP Validation

Validate that returned IP is:
1. **Valid IPv4 format**: `xxx.xxx.xxx.xxx` where xxx is 0-255
2. **Not private/local**:
   - 10.0.0.0/8 (10.x.x.x)
   - 172.16.0.0/12 (172.16-31.x.x)
   - 192.168.0.0/16 (192.168.x.x)
   - 127.0.0.0/8 (127.x.x.x - loopback)
   - 169.254.0.0/16 (169.254.x.x - link-local)

### Error Handling Strategy

1. **Try primary service** (ipify.org)
2. **If fails, try backup services** (icanhazip.com, checkip.amazonaws.com)
3. **If all external services fail**, fall back to `request.getRemoteAddr()`
4. **Log all failures** for troubleshooting
5. **Return result to client** with error indicator if needed

### Caching Strategy

- **Cache duration**: 5 minutes (configurable via `settings.publicip.cache.ttl`)
- **Cache invalidation**: Time-based (check timestamp on each request)
- **Thread safety**: Use synchronized block or AtomicReference for cache updates
- **Cache key**: None needed (single global value - server's public IP)

## Open Questions

None - the approach is straightforward with well-defined implementation steps.
