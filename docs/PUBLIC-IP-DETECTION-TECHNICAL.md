# Public IP Detection - Technical Documentation

**Version:** 3.3.0-community
**Date:** 2026-02-09
**Status:** Implemented

## Overview

DDPoker's "Test Online" feature allows users to verify their network connectivity and determine their public IP address for peer-to-peer (P2P) game hosting. This document describes the technical implementation of the public IP detection system, which was redesigned to correctly handle NAT/router scenarios.

## Problem Statement

### Original Issue

The original implementation used server-side IP detection via `HttpServletRequest.getRemoteAddr()`. This approach had critical flaws:

1. **NAT/Router Scenarios**: When users are behind a NAT router, the server sees the client's private IP address (e.g., 192.168.1.100) rather than their public IP
2. **Localhost Testing**: When client and server run on the same machine, the server returns 127.0.0.1
3. **P2P Architecture Mismatch**: DDPoker uses P2P architecture where users host games on their local machines, requiring their actual public IP for other players to connect

### User Impact

Users behind NAT routers (the vast majority of home networks) would see incorrect IP addresses like:
- `192.168.x.x` (private network)
- `10.x.x.x` (private network)
- `127.0.0.1` (localhost)

This made it impossible for users to determine their actual public-facing IP address for game hosting.

## Solution Architecture

### Client-Side Detection Approach

The solution implements **client-side public IP detection** by querying external "What is my IP" services directly from the client application.

**Why Client-Side?**
- Correctly reflects the P2P architecture (client needs its own public IP)
- Works with NAT/router scenarios (external services see the post-NAT public IP)
- No proxy/load balancer complications
- Simpler deployment (no server-side changes required)

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│  User's Network (Behind NAT Router)                         │
│                                                              │
│  ┌──────────────────┐                                       │
│  │  DDPoker Client  │                                       │
│  │                  │                                       │
│  │  1. User clicks  │                                       │
│  │     "Test Online"│                                       │
│  │                  │                                       │
│  │  2. PublicIP     │───────────────┐                      │
│  │     Detector     │               │                      │
│  └──────────────────┘               │                      │
│         Private IP:                 │                      │
│         192.168.1.100               │                      │
└─────────────────────────────────────┼──────────────────────┘
                                      │
                    NAT Router        │
                    Translates to ────┤
                    Public IP:        │
                    203.0.113.42      │
                                      │
                                      ▼
            ┌──────────────────────────────────────────┐
            │  Internet                                 │
            │                                           │
            │  ┌─────────────────────┐                │
            │  │  api.ipify.org      │◄───── Primary  │
            │  │  Returns: 203.0.113.42                │
            │  └─────────────────────┘                │
            │                                           │
            │  ┌─────────────────────┐                │
            │  │  icanhazip.com      │◄───── Fallback 1│
            │  └─────────────────────┘                │
            │                                           │
            │  ┌─────────────────────┐                │
            │  │  checkip.amazonaws  │◄───── Fallback 2│
            │  └─────────────────────┘                │
            └──────────────────────────────────────────┘
```

## Implementation Details

### Core Classes

#### 1. PublicIPDetector

**Location:** `code/poker/src/main/java/com/donohoedigital/games/poker/online/PublicIPDetector.java`

**Responsibilities:**
- Query external IP detection services
- Implement fallback across multiple services
- Validate IP addresses (reject private/special ranges)
- Cache results to minimize external calls
- Thread-safe concurrent access

**Key Features:**

```java
public class PublicIPDetector {
    // Caching with configurable TTL (default 5 minutes)
    private String cachedPublicIP;
    private long cachedPublicIPTimestamp;

    // Multiple services for reliability
    private final String[] ipServices = {
        "https://api.ipify.org",           // Primary
        "https://icanhazip.com",           // Fallback 1
        "https://checkip.amazonaws.com"    // Fallback 2
    };

    public synchronized String fetchPublicIP() {
        // Check cache first
        // Try each service in order
        // Validate and filter results
        // Cache successful result
    }
}
```

#### 2. GetPublicIP (Modified)

**Location:** `code/poker/src/main/java/com/donohoedigital/games/poker/online/GetPublicIP.java`

**Changes:**
- Overrides `start()` to use `PublicIPDetector` instead of server query
- Falls back to original server query if all external services fail
- Maintains backward compatibility

### IP Validation Rules

The implementation validates and filters IP addresses to ensure only valid public IPs are returned:

#### Accepted IPs
- Valid IPv4 addresses in public ranges
- Examples: `8.8.8.8`, `1.1.1.1`, `203.0.113.42`

#### Rejected IPs

| Range | CIDR | Reason |
|-------|------|--------|
| 0.0.0.0 | 0.0.0.0/32 | Invalid/unspecified address |
| 255.255.255.255 | 255.255.255.255/32 | Broadcast address |
| 10.0.0.0 - 10.255.255.255 | 10.0.0.0/8 | RFC 1918 private network |
| 172.16.0.0 - 172.31.255.255 | 172.16.0.0/12 | RFC 1918 private network |
| 192.168.0.0 - 192.168.255.255 | 192.168.0.0/16 | RFC 1918 private network |
| 127.0.0.0 - 127.255.255.255 | 127.0.0.0/8 | Loopback addresses |
| 169.254.0.0 - 169.254.255.255 | 169.254.0.0/16 | Link-local addresses |

#### Additional Validation
- IPv6 addresses are rejected (only IPv4 supported)
- Malformed IPs: partial (3 octets), extra octets (5+), invalid characters
- Octets must be in range 0-255
- Leading zeros rejected: `192.168.001.001`
- Special formats rejected: IPs with ports (`8.8.8.8:80`), CIDR notation (`8.8.8.8/24`)

### External Service Selection

#### Primary Service: api.ipify.org

**Why ipify?**
- Free, open-source service
- Simple API (returns plain text IP)
- HTTPS for security
- High reliability and uptime
- No rate limits for reasonable usage
- Used by millions of applications

**Example Response:**
```
203.0.113.42
```

#### Fallback Services

1. **icanhazip.com** - Cloudflare-backed, highly reliable
2. **checkip.amazonaws.com** - AWS-backed, enterprise-grade reliability

**Fallback Strategy:**
- Try primary service first
- If primary fails (network error, timeout, invalid response), try fallback 1
- If fallback 1 fails, try fallback 2
- If all fail, fall back to original server-side detection

### Caching Strategy

**Why Caching?**
- Reduces load on external services
- Improves performance (instant response from cache)
- Minimizes network traffic
- Public IPs rarely change (most ISPs use semi-static IPs)

**Cache Configuration:**
- **Default TTL:** 5 minutes (300,000 milliseconds)
- **Configurable:** `settings.publicip.cache.ttl` property
- **Thread-safe:** `synchronized` methods prevent race conditions
- **Cache invalidation:** Time-based expiration

**Cache Behavior:**
```
First call:  Query external service → Cache result → Return IP
Second call (within TTL): Return cached IP immediately (no network call)
After TTL expires: Query external service again → Update cache
```

### Thread Safety

The implementation is thread-safe for concurrent access scenarios:

**Mechanisms:**
- `synchronized` keyword on `fetchPublicIP()` method
- `synchronized` keyword on `clearCache()` method
- Instance-level locking ensures only one thread fetches at a time
- Other threads wait for the first thread to complete and use cached result

**Tested Scenarios:**
- 10 concurrent threads calling `fetchPublicIP()`
- Only 1 HTTP request made (others use cache)
- No race conditions or cache corruption

### Resource Management

**HTTP Connection Handling:**
- Proper cleanup in `finally` blocks
- Reader and client closed even on exceptions
- No resource leaks

```java
finally {
    try {
        if (reader != null) reader.close();
    } catch (Exception e) {
        logger.warn("Error closing reader: {}", e.getMessage());
    }
    try {
        if (client != null) client.close();
    } catch (Exception e) {
        logger.warn("Error closing HTTP client: {}", e.getMessage());
    }
}
```

## Configuration

### Properties

**Location:** `code/poker/src/main/resources/config/poker/client.properties`

```properties
# Public IP detection (for P2P game hosting)
# Primary external service URL for detecting public IP address
settings.publicip.service.url=https://api.ipify.org

# Cache TTL in milliseconds (default 5 minutes = 300000ms)
settings.publicip.cache.ttl=300000
```

### Customization Options

Users can customize the behavior by modifying these properties:

**Change Primary Service:**
```properties
settings.publicip.service.url=https://icanhazip.com
```

**Disable Caching (always fetch):**
```properties
settings.publicip.cache.ttl=0
```

**Extend Cache Duration (30 minutes):**
```properties
settings.publicip.cache.ttl=1800000
```

## Error Handling

### Failure Scenarios

1. **Primary service down:** Automatically tries fallback services
2. **All services down:** Falls back to original server-side detection
3. **Invalid IP returned:** Rejected by validation, tries next service
4. **Network timeout:** Caught and logged, tries next service
5. **Malformed response:** Validation catches, tries next service

### Logging

The implementation provides comprehensive logging:

```java
// Success
logger.info("Successfully fetched public IP: {} from {}", ip, serviceUrl);

// Validation failure
logger.warn("Service {} returned invalid or private IP: {}", serviceUrl, ip);

// Network failure
logger.warn("Failed to fetch IP from {}: {}", serviceUrl, e.getMessage());

// Complete failure
logger.error("All IP detection services failed");
```

## Testing

### Unit Test Coverage

**Total Tests:** 40 comprehensive unit tests

**Test Categories:**

1. **Basic Functionality (3 tests)**
   - Successful IP fetch from primary service
   - Fallback to second service when first fails
   - Fallback to third service when first two fail

2. **IP Validation (10 tests)**
   - Reject private IPs: 10.x.x.x, 172.16-31.x.x, 192.168.x.x
   - Reject loopback: 127.0.0.1
   - Reject link-local: 169.254.x.x
   - Reject broadcast: 255.255.255.255
   - Reject zero address: 0.0.0.0
   - Accept valid public IPs

3. **IPv6 Handling (2 tests)**
   - Reject full IPv6 addresses
   - Reject compressed IPv6 addresses

4. **Malformed Input (10 tests)**
   - Partial IPs (3 octets)
   - Extra octets (5+ octets)
   - Leading zeros
   - Octets > 255
   - Negative octets
   - HTML error responses
   - Multiple IPs in response
   - IPs with ports
   - IPs with CIDR notation
   - Invalid format strings

5. **Caching (4 tests)**
   - Cache hit within TTL
   - Cache expiration after TTL
   - Zero TTL (no caching)
   - Explicit cache clear

6. **Thread Safety (1 test)**
   - 10 concurrent threads
   - Only 1 HTTP call made
   - All threads get same result

7. **Edge Cases (10 tests)**
   - Empty string response
   - Null response
   - Whitespace in IP
   - Extra whitespace between octets
   - Boundary values (0.0.0.1, 254.254.254.254)
   - All services fail

### Test Methodology

**Test-Driven Development (TDD) Approach:**
1. **RED Phase:** Wrote all 40 tests first (all failed - no implementation)
2. **GREEN Phase:** Implemented code to pass all tests
3. **REFACTOR Phase:** Cleaned up code while keeping tests green

**Test Implementation:**
- Manual test doubles (no Mockito complexity)
- Simple `TestHttpFetcher` class with configurable responses
- Clear arrange-act-assert structure
- Descriptive test names following convention

## Performance Characteristics

### Metrics

| Metric | Value | Notes |
|--------|-------|-------|
| First call (uncached) | 100-500ms | Depends on network latency to external service |
| Cached call | < 1ms | Instant return from memory |
| Cache memory | ~50 bytes | Single String + timestamp |
| HTTP request size | < 1 KB | Simple GET request |
| HTTP response size | < 50 bytes | Plain text IP address |

### Network Traffic

**Per Request (uncached):**
- 1 HTTP GET request to external service
- ~1 KB request + ~50 bytes response
- HTTPS overhead

**Per Request (cached):**
- 0 network traffic
- Memory access only

**Daily Usage (typical user):**
- User clicks "Test Online": 2-3 times per session
- With 5-minute cache: ~1-2 external calls per hour of gameplay
- Total daily traffic: < 10 KB

## Security Considerations

### Privacy

- **No tracking:** External services see only the IP address (which they must know to respond)
- **No user data:** No personal information sent to external services
- **No cookies:** Simple HTTP GET requests, no session tracking
- **Configurable:** Users can change services or disable feature

### Service Trust

**Why trust ipify.org, icanhazip.com, checkip.amazonaws.com?**
- Open source services with transparent operation
- No logging or tracking claims
- High reputation in developer community
- Used by major applications and companies
- HTTPS encryption for all requests

### Fallback Safety

If user doesn't trust external services:
- Configure alternative service via properties
- Set cache TTL to very high value (minimize calls)
- Falls back to server-side detection if services unavailable

### IP Address Exposure

**What information does public IP reveal?**
- Approximate geographic location (city/region level)
- ISP (Internet Service Provider)
- Does NOT reveal: specific address, name, personal data

**Is this a security concern?**
- Public IP is necessary for P2P gaming (others need to connect)
- IP is already visible to game server and other players
- Same as any P2P application (BitTorrent, Skype, etc.)

## Deployment Considerations

### Server-Side Changes

**None required!** The implementation is entirely client-side.

**Advantages:**
- No server deployment needed
- No server downtime
- No coordination between client and server versions
- Old and new clients can coexist

### Backward Compatibility

**Old Clients:**
- Continue to use server-side detection
- No breaking changes

**New Clients:**
- Use external service detection
- Fall back to server-side if services unavailable
- Transparent to users

### External Service Dependencies

**Dependency:** Client requires internet access to external IP services

**Risk Mitigation:**
- Multiple fallback services
- Falls back to server-side detection
- Configurable service URLs
- Graceful degradation

## Troubleshooting

### Issue: Returns null

**Possible Causes:**
1. All external services are down or unreachable
2. Firewall blocking HTTPS requests to external services
3. Network connectivity issues

**Solutions:**
- Check firewall settings
- Verify internet connectivity
- Check logs for specific error messages
- Try configuring alternative service URL

### Issue: Returns private IP (192.168.x.x)

**Possible Causes:**
1. External services failed and fell back to server-side detection
2. Server and client on same private network

**Solutions:**
- Check why external services are failing (firewall, network)
- Verify `settings.publicip.service.url` is accessible

### Issue: Slow response

**Possible Causes:**
1. Network latency to external services
2. External service overloaded

**Solutions:**
- Configure alternative service (icanhazip.com, checkip.amazonaws.com)
- Increase cache TTL to reduce frequency of calls
- Check network connection quality

## Future Enhancements

### Potential Improvements

1. **IPv6 Support**
   - Add IPv6 address detection
   - Dual-stack support (IPv4 + IPv6)

2. **Additional Validation**
   - Reject multicast ranges (224.0.0.0/4)
   - Reject reserved ranges (documentation IPs)

3. **Service Health Monitoring**
   - Track service reliability
   - Automatically prefer reliable services
   - Adaptive fallback strategy

4. **User Notification**
   - Inform user when using cached IP
   - Show which service provided the IP
   - Warning if IP might be incorrect

5. **Local Network Detection**
   - Auto-detect when all clients on same LAN
   - Optimize for LAN-only scenarios

## References

### External Services

- **ipify API:** https://www.ipify.org/
- **icanhazip:** https://icanhazip.com/
- **AWS checkip:** https://checkip.amazonaws.com/

### RFCs and Standards

- **RFC 1918:** Address Allocation for Private Internets
- **RFC 4122:** A Universally Unique IDentifier (UUID) URN Namespace
- **RFC 5735:** Special Use IPv4 Addresses

### Related Documentation

- `docs/LICENSE-REMOVAL-TECHNICAL.md` - Player identity system
- `docs/FILE-BASED-CONFIGURATION.md` - Configuration system
- `CHANGELOG.md` - Release notes

## Conclusion

The public IP detection implementation successfully solves the NAT/router scenario problem by moving detection to the client side. The solution is:

- ✅ **Architecturally correct** for P2P game hosting
- ✅ **Reliable** with multiple fallback services
- ✅ **Performant** with caching and thread-safety
- ✅ **Well-tested** with 40 comprehensive unit tests
- ✅ **Configurable** for user customization
- ✅ **Secure** with proper validation and privacy considerations
- ✅ **Backward compatible** with graceful degradation

Users can now reliably determine their public IP address for hosting P2P poker games, regardless of their network configuration.
