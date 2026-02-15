# Plan: Improve pokernetwork Module Coverage (46% → 65%+)

**Status:** Completed ✅
**Branch:** test-improvement
**Target:** Increase pokernetwork module coverage from 46% to 65%+ threshold

## Current State

**Coverage Analysis:**
- Current: 46% instruction coverage
- Target: 65%+ (meets JaCoCo threshold)
- Gap: ~20% improvement needed

**Existing Tests (2):**
- `PokerConnectionTcpTest.java` - Tests `PokerConnection` wrapper class
- `TcpChatClientTest.java` - Comprehensive tests for `TcpChatClient`

**Untested Classes (3):**
1. **OnlineMessage.java** (657 lines) - Large wrapper for network messages
2. **OnlinePlayerInfo.java** (189 lines) - Player info data class
3. **PokerURL.java** (74 lines) - URL parser for game connections

**Skipped:**
- `PokerConnectionServer.java` - Interface only (no implementation)

## Implementation Strategy

### Phase 1: OnlineMessage Tests (Priority: HIGH)
**Rationale:** Largest class, likely where most missing coverage is

**Test Coverage:**
- Constructor variants (with DDMessage, with category)
- Category constants and toStringCategory() for all message types
- Getter/setter pairs for all fields:
  - Player info (name, profile, demo, connected status)
  - Game data (gameID, password, GUID)
  - Chat (chat, chatType, playerList)
  - Game state (phase, hand action, table events)
  - WAN operations (auth, profile, games)
  - Connection (URL, message IDs, in-reply-to)
- Edge cases:
  - Null DDMessage (should throw ApplicationError)
  - Default values when fields not set
  - Player list conversion (DMArrayList ↔ List<OnlinePlayerInfo>)

**Test file:** `OnlineMessageTest.java`

### Phase 2: OnlinePlayerInfo Tests (Priority: MEDIUM)
**Rationale:** Data class with equals/hashCode/compareTo that needs validation

**Test Coverage:**
- Constructors (empty, with DMTypedHashMap)
- Getter/setter pairs (name, playerId, createDate, aliases)
- Name case-insensitivity (getNameLower())
- equals() contract:
  - Reflexive, symmetric, transitive
  - Based on lowercase name + playerId
  - Null and different type handling
- hashCode() consistency with equals()
- compareTo() for sorting (case-insensitive name comparison)
- Aliases list handling

**Test file:** `OnlinePlayerInfoTest.java`

### Phase 3: PokerURL Tests (Priority: LOW)
**Rationale:** Simple parser, but critical for connection establishment

**Test Coverage:**
- Valid URL parsing (gameID and password extraction)
- URI format with delimiter handling
- isTCP() always returns true (post-UDP removal)
- Getter methods (getGameID, getPassword)
- Edge cases:
  - Invalid format (missing delimiter)
  - Empty gameID or password
  - Special characters in ID/password

**Test file:** `PokerURLTest.java`

## Success Criteria

1. All new tests pass
2. Build succeeds with zero warnings
3. Coverage report shows pokernetwork at 65%+ instruction coverage
4. No existing tests broken
5. Tests follow patterns from existing well-tested modules (pokerengine, pokergamecore)

## Verification Steps

```bash
# Run tests for pokernetwork module only
cd /c/Repos/DDPoker-test-improvement/code
mvn test -pl pokernetwork

# Generate coverage report
mvn verify -P coverage -pl pokernetwork

# Check coverage HTML report
# Open: code/pokernetwork/target/site/jacoco/index.html
```

## Dependencies

- No new dependencies required
- Uses existing test frameworks: JUnit 5, AssertJ, Mockito
- Test patterns from TcpChatClientTest provide good examples

## Risks & Mitigations

**Risk:** OnlineMessage has dependencies on PokerConstants and other poker module classes
**Mitigation:** Use test stubs/mocks where needed (pattern from TcpChatClientTest)

**Risk:** Some methods may be hard to test (e.g., message serialization)
**Mitigation:** Focus on behavior testing, not implementation details

## Timeline Estimate

- Phase 1 (OnlineMessage): 1-2 hours (~60-80 test cases)
- Phase 2 (OnlinePlayerInfo): 30-45 min (~20-25 test cases)
- Phase 3 (PokerURL): 15-30 min (~10-12 test cases)
- Verification: 15 min

**Total:** 2.5-3.5 hours

## Completion Summary

**Completed:** 2026-02-15
**Commit:** 0eb03e3c

**Final Results:**
- ✅ All 151 tests passing (118 new + 33 existing)
- ✅ Coverage: 87% instruction (target was 65%+)
- ✅ Build succeeds with zero warnings
- ✅ No existing tests broken

**Tests Added:**
- `OnlineMessageTest.java`: 64 tests covering all message operations
- `OnlinePlayerInfoTest.java`: 34 tests for player data class
- `PokerURLTest.java`: 20 tests for URL parsing

**Key Learning:**
- PokerURL format is `poker://host:port/gameID/password` (delimiter is `/` not `~`)
- P2PURL parent class requires all three delimiters: `://`, `:`, `/`

## Notes

- Following test-first principles: write tests, verify they fail, implement/verify, verify they pass
- Match existing test style from TcpChatClientTest (clear sections, descriptive names)
- Use AssertJ fluent assertions for readability
- Group tests by functionality using comments (pattern from existing tests)
