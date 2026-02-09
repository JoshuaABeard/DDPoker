# DDPoker Enhancement Backlog

**Last Updated:** February 9, 2026
**Project:** DDPoker GPL-3.0 Community Edition

---

## Overview

This document tracks enhancement ideas, future features, and non-critical improvements identified during code review and maintenance. Items are prioritized based on value to the Community Edition and implementation complexity.

---

## Priority Levels

- **P0 (Critical):** Blocking issues, security vulnerabilities
- **P1 (High):** Significant user impact, high value
- **P2 (Medium):** Moderate impact, nice-to-have features
- **P3 (Low):** Minor improvements, future considerations

---

## Active Backlog

### ENHANCE-5: Multi-JVM Horizontal Scaling

**Priority:** P3 (Low)
**Effort:** Large (3-4 weeks)
**Value:** Low for Community Edition, High for commercial deployment

**Description:**
Implement FileLock-based synchronization to support multiple JVM instances running in parallel, enabling horizontal scaling for high-traffic deployments.

**Current State:**
- Single JVM deployment using synchronized blocks
- Works perfectly for Community Edition use cases
- No evidence of performance bottlenecks requiring scaling

**Technical Details:**
- Files affected: `ServerSideGame.java:219`, `EngineServlet.java:1099`
- Implementation: Replace synchronized(SAVE_DIR) with FileLock
- Additional work: Load balancer configuration, session management
- Testing: Multi-server deployment testing required

**Acceptance Criteria:**
- [ ] Multiple JVM instances can run concurrently
- [ ] Game state synchronization works across instances
- [ ] No race conditions in file access
- [ ] Load balancer can distribute requests
- [ ] Performance tests show linear scaling

**Dependencies:**
- ENHANCE-8: Load Balancing Implementation
- Infrastructure: Multiple servers or containers

**References:**
- TODO: ServerSideGame.java:219
- TODO: EngineServlet.java:1099
- Analysis: TODO-INCOMPLETE-ANALYSIS.md

---

### ENHANCE-6: Email Bounce Handling

**Priority:** P2 (Medium)
**Effort:** Medium (1-2 weeks)
**Value:** Medium - improves email deliverability

**Description:**
Implement server-side email bounce detection and handling to improve reliability of password resets, game notifications, and tournament emails.

**Current State:**
- Emails sent via postalService without bounce handling
- No tracking of failed deliveries
- Users may not receive critical emails (password resets)
- No feedback when email addresses are invalid

**Features to Implement:**
1. Bounce detection (hard bounces, soft bounces)
2. Email address validation and blacklisting
3. Retry logic for soft bounces
4. User notification when email fails
5. Admin dashboard for bounce monitoring

**Technical Details:**
- File affected: `ServerSideGame.java:860`
- Integration: DDPostalService interface
- Storage: Database table for bounce tracking
- Email verification: Check MX records before sending

**Active Email Usage:**
- Password reset emails (ForgotPassword.java)
- Game notification emails (EngineServlet.java)
- Tournament emails (PokerServlet.java)
- Player-to-player emails (ServerSideGame.java)

**Acceptance Criteria:**
- [ ] Hard bounces detected and logged
- [ ] Invalid email addresses marked in database
- [ ] Soft bounces retried with exponential backoff
- [ ] Users notified when email delivery fails
- [ ] Admin can view bounce statistics

**Dependencies:**
- Email service provider with bounce webhooks (optional)
- Database schema changes for bounce tracking

**References:**
- TODO: ServerSideGame.java:860
- Analysis: TODO-INCOMPLETE-ANALYSIS.md

---

### ENHANCE-7: Player Statistics Tracking

**Priority:** P3 (Low)
**Effort:** Medium (1-2 weeks)
**Value:** Medium - enhances online multiplayer experience

**Description:**
Track and display player statistics including games played, tournaments won, win rate, ranking, and performance over time.

**Current State:**
- Basic player profiles exist (name, alias, create date)
- No game statistics tracked
- No leaderboards or rankings
- No historical performance data

**Features to Implement:**
1. Statistics tracking:
   - Games played (total, by type)
   - Tournaments entered / won / placed
   - Win rate (cash games, tournaments)
   - Chips won/lost
   - Biggest wins/losses
   - Playing style metrics (aggression, tightness)

2. Leaderboards:
   - Overall ranking
   - Tournament champions
   - Cash game winners
   - Weekly/monthly/all-time

3. Player profiles:
   - Public statistics page
   - Performance graphs
   - Achievement badges
   - Playing history

**Technical Details:**
- File affected: `ChatServer.java:228`
- Database: New tables for statistics, rankings
- Performance: Incremental updates, denormalized data
- Privacy: Players can opt-out of public leaderboards

**Acceptance Criteria:**
- [ ] Statistics collected during games
- [ ] Player profile shows statistics
- [ ] Leaderboard displays top players
- [ ] Statistics updated in real-time
- [ ] Historical data preserved
- [ ] Performance impact < 5% on game server

**Dependencies:**
- Database schema changes
- UI for displaying statistics
- Privacy policy updates

**References:**
- TODO: ChatServer.java:228
- Analysis: TODO-INCOMPLETE-ANALYSIS.md

---

### ENHANCE-8: Load Balancing Implementation

**Priority:** P3 (Low)
**Effort:** Large (2-3 weeks)
**Value:** Low for Community Edition

**Description:**
Implement dynamic server URL assignment to distribute client load across multiple game servers.

**Current State:**
- Single hardcoded server URL
- All clients connect to same server
- No load distribution mechanism

**Related To:** ENHANCE-5 (Multi-JVM Scaling)

**Technical Details:**
- File affected: `EngineServlet.java:1099`
- Implementation options:
  1. DNS round-robin (simple)
  2. HTTP redirect based on load
  3. Central dispatcher service
  4. Client-side server selection

**Deferred:** Not needed until ENHANCE-5 is implemented

**References:**
- TODO: EngineServlet.java:1099

---

## Deferred Items (Low Value)

### DEFER-1: ServletDebug Config File Loading

**Priority:** P3 (Low)
**Effort:** Small (2-3 hours)
**Value:** Low - debugging tool only

**Description:**
Load ServletDebug settings from config file instead of hardcoded values.

**Current State:**
- Debug settings hardcoded in ServletDebug.java
- Works fine for current use (load testing with LoadGen)
- Only used by developers, not production

**Rationale for Deferral:**
- ServletDebug is a development/testing tool
- Current hardcoded approach works fine
- Low ROI for configuration effort

**References:**
- TODO: ServletDebug.java:49

---

### DEFER-2: Multi-Line URL File Support

**Priority:** P3 (Low)
**Effort:** Small (1-2 hours)
**Value:** Low - testing tool only

**Description:**
Allow ServletDebug URL files to contain multiple lines for load testing scenarios.

**Current State:**
- Single-line URL files only
- Newlines transformed to spaces
- Sufficient for current XML POST testing

**Rationale for Deferral:**
- Testing tool enhancement
- Current approach works for XML payloads
- Not blocking any use cases

**References:**
- TODO: ServletDebug.java:174

---

### DEFER-3: FileChooserDialog Improvements

**Priority:** P3 (Low)
**Effort:** Medium (3-5 days)
**Value:** Low - minor UX improvement

**Description:**
Improve FileChooserDialog to support "pick file" mode in addition to current "save as" mode.

**Current State:**
- Designed primarily for "save as" operations
- "Pick file" mode needs work
- Workaround exists (see DeckProfile)

**References:**
- TODO: FileChooserDialog.java:52

---

### DEFER-4: Tab Focus in Help Table

**Priority:** P3 (Low)
**Effort:** Small (2-3 hours)
**Value:** Low - accessibility improvement

**Description:**
Fix help table so Tab key causes focus change between UI elements.

**Current State:**
- Tab key doesn't change focus in help table
- Minor accessibility issue
- Users can still navigate with mouse

**References:**
- TODO: Help.java:153

---

## Design Notes (Keep in Code)

These TODO comments explain design decisions and should remain in the codebase:

### Valid Design Documentation

1. **Bet Validation Context** (PlayerAction.java:63)
   - "identify from context what's legal (esp bet vs raise)"
   - Explains complexity of bet validation logic

2. **Window Resize Timing** (GameContext.java:322)
   - "option to resize window to component"
   - Documents timing constraint in UI initialization

3. **Online Player Limit** (TournamentProfile.java:76)
   - "MAX_ONLINE_PLAYERS = 30; is this too few?"
   - Documents capacity planning decision

4. **ZIP Precision Loss** (ZipUtil.java:85,101,123)
   - "what to do about loss of precision?"
   - Documents limitation in timestamp handling

5. **XML Escaping** (XMLWriter.java:248,273)
   - "escapes?"
   - Notes potential issue with special characters

**Action:** ✅ KEEP - These provide valuable context

---

## Code Cleanup Queue

Items identified for immediate cleanup:

### CLEANUP-1: BaseServlet Comment ✅ DONE
- **File:** BaseServlet.java:146
- **Issue:** Unclear comment "needed for email"
- **Action:** Delete comment (feature already implemented)
- **Status:** Completed February 9, 2026

### CLEANUP-2: Unprofessional Comment
- **File:** Utils.java:89
- **Issue:** Comment says "GAH! this code is crap"
- **Action:** Replace with specific issue description or delete
- **Status:** Pending review

### CLEANUP-3: CSV Parser Error Handling
- **File:** CSVParser.java:110
- **Issue:** TODO says "ERROR CONDITION - what to do?"
- **Action:** Implement proper error handling (throw exception or log)
- **Status:** Pending review

---

## Completed Enhancements

### Phase 1: Dead Code Removal ✅ COMPLETE
- Removed performance profiling stubs (5 instances)
- Removed licensing validation dead code
- Removed debug "XXXX" logging
- Cleaned up obsolete comments in BaseFrame.java

**Completed:** February 8, 2026

---

## Process Notes

### Adding New Items

When adding backlog items:
1. Assign priority (P0-P3)
2. Estimate effort (Small/Medium/Large)
3. Describe current state and desired state
4. Define acceptance criteria
5. Note dependencies
6. Reference source TODO comments

### Prioritization Criteria

**P1 (High):**
- Security vulnerabilities
- Data loss prevention
- Critical user experience issues

**P2 (Medium):**
- Feature improvements with clear ROI
- Moderate user experience improvements
- Performance optimizations

**P3 (Low):**
- Nice-to-have features
- Developer convenience
- Future scalability (not currently needed)

---

## Related Documents

- **TODO-INCOMPLETE-ANALYSIS.md** - Detailed TODO analysis
- **FIXME-ANALYSIS.md** - Critical bug analysis (none found)
- **TESTING.md** - Testing strategy and coverage
- **SERVER-CODE-REVIEW.md** - Server-side architectural notes

---

**Backlog Maintained By:** Claude Code (automated analysis)
**Review Schedule:** Quarterly or as needed
**Next Review:** May 2026
