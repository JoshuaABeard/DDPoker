# Code Review Handoff: resource-leak-fixes

## Summary

Fixed 2 critical resource leaks (LEAK-1 P0 and QF-8 P1) from CODE-REVIEW.md by adding proper try-with-resources blocks. These leaks could cause file descriptor exhaustion and memory leaks over time in command-line tools.

## Files Changed

### Privacy Check Status: ✅ ALL SAFE

All 3 files contain only source code changes - no configuration, no private information.

1. ✅ `code/common/src/main/java/com/donohoedigital/config/ConfigUtils.java` - Fixed stream leak in copyFile()
2. ✅ `code/gameserver/src/main/java/com/donohoedigital/games/server/Ban.java` - Fixed ApplicationContext leak
3. ✅ `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/OnlineGamePurger.java` - Fixed ApplicationContext leak

## Verification

### Test Results: ✅ PASS
```
mvn test --projects common,gameserver,pokerserver

Reactor Summary:
- common: SUCCESS [34.580s] - 236 tests (1 skipped)
- gameserver: SUCCESS [9.916s] - 49 tests
- pokerserver: SUCCESS [37.191s] - 119 tests

Total: 404 tests (1 skipped), 0 failures, 0 errors
BUILD SUCCESS
```

### Coverage: ✅ MEETS THRESHOLD
All modules maintain existing coverage (≥65%). No new code added, only modifications to existing exception handling.

### Build Status: ✅ CLEAN
Compile succeeds with zero warnings.

## Changes Detail

### LEAK-1: Fix ApplicationContext resource leaks (2 files)

**Issue:** Spring `ApplicationContext` instances created but never closed, causing resource leaks in command-line tools.

**OnlineGamePurger.java (line 128):**
```java
// Before: ApplicationContext created but never closed
ApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-pokertools.xml");
service = (OnlineGameService) ctx.getBean("onlineGameService");
doPurge();

// After: Wrapped in try-with-resources
try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-pokertools.xml"))
{
    service = (OnlineGameService) ctx.getBean("onlineGameService");
    doPurge();
}
```

**Ban.java (line 111):**
```java
// Before: ApplicationContext created but never closed
ApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-gameserver.xml");
Ban app = (Ban) ctx.getBean("banApp");
app.initAndRun(info.getCommandLineOptions());

// After: Wrapped in try-with-resources
try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("app-context-gameserver.xml"))
{
    Ban app = (Ban) ctx.getBean("banApp");
    app.initAndRun(info.getCommandLineOptions());
}
```

### QF-8: Fix resource leak in ConfigUtils.copyFile() (1 file)

**Issue:** `FileInputStream` and `FileOutputStream` created but only their channels are closed, not the underlying streams. This leaks file descriptors.

**ConfigUtils.java (lines 459-473):**
```java
// Before: Only channels closed, streams leaked
FileChannel src = new FileInputStream(from).getChannel();
FileChannel dst = new FileOutputStream(to).getChannel();
src.transferTo(0, src.size(), dst);
src.close();
dst.close();

// After: All resources in try-with-resources
try (FileInputStream fis = new FileInputStream(from);
     FileOutputStream fos = new FileOutputStream(to);
     FileChannel src = fis.getChannel();
     FileChannel dst = fos.getChannel())
{
    src.transferTo(0, src.size(), dst);
}
```

## Context & Decisions

### Why try-with-resources:
- Automatically closes resources in reverse order of declaration
- Handles exceptions during close() properly (suppressed exceptions)
- More concise and less error-prone than manual close() in finally blocks

### Impact:
- **OnlineGamePurger & Ban**: Command-line tools that run once and exit, so leak impact is minimal in production but still important to fix
- **ConfigUtils.copyFile()**: Called during file operations throughout the codebase, so leak could accumulate over time

## Worktree Path

`C:\Repos\DDPoker-resource-leak-fixes`

## Review Status

**STATUS:** APPROVED

**Reviewer:** Claude Opus 4.6 (review agent)

**Findings:**

### Verification Results

- **Tests:** ALL PASS -- 404 tests run (1 pre-existing skip), 0 failures, 0 errors. BUILD SUCCESS.
- **Coverage:** Maintained. No new code branches introduced; only existing code paths restructured for proper resource management.
- **Build:** Clean. No compile warnings. The only `[WARNING]` lines are standard Maven markers for the pre-existing skipped test.

### CLAUDE.md Checklist

| Check | Result | Notes |
|-------|--------|-------|
| Tests pass | PASS | 404 tests, 0 failures |
| Coverage >= 65% | PASS | No coverage reduction; modifications only |
| Build clean | PASS | Zero compile warnings |
| No scope creep (Section 4) | PASS | 3 files, +16/-14 lines, tightly focused |
| No over-engineering (Section 3) | PASS | Standard try-with-resources, idiomatic Java |
| No private info (Section 10) | PASS | Pure source code changes only |
| No security vulnerabilities | PASS | Improves resource safety |
| Implementation matches plan | PASS | No plan file; fixes match CODE-REVIEW.md items LEAK-1 and QF-8 exactly |

### Detailed Findings

All three changes are correct and well-scoped:

1. **ConfigUtils.java:459-467** -- `copyFile()` fix is correct. The original code created `FileInputStream`/`FileOutputStream` inline in `.getChannel()` calls, so the streams themselves were never closed (only channels). The new code properly declares all four resources in the try-with-resources, which closes them in reverse order. This is the most impactful fix since `copyFile()` can be called repeatedly.

2. **Ban.java:111-116** -- `ApplicationContext` leak fix is correct. The `initAndRun()` call happens inside the try block, so all Spring-managed resources (including `@Autowired` `BannedKeyService`) remain available during execution. The context closes after `initAndRun()` returns, just before `System.exit(0)`.

3. **OnlineGamePurger.java:128-134** -- `ApplicationContext` leak fix is correct. `doPurge()` is called inside the try block (line 133), so the `service` bean and its underlying database connections are available throughout. The context closes after `doPurge()` returns.

### Minor Note (non-blocking)

Both `Ban.java` (line 47) and `OnlineGamePurger.java` (line 40) retain the wildcard import `org.springframework.context.*`. After the changes, `ApplicationContext` (the only type used from that package) is no longer referenced -- the code now uses `ClassPathXmlApplicationContext` from `org.springframework.context.support.*` directly. The `org.springframework.context.*` import is technically unused. This is cosmetic and does not affect compilation or runtime behavior, so it is not a blocker.

### Blockers

None.

### Post-Review Action

**Minor Note ADDRESSED:** Removed unused `org.springframework.context.*` imports from both Ban.java and OnlineGamePurger.java. Compilation verified successful.
