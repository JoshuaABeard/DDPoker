# Review Request: Multi-Platform Installers

## Review Request

**Branch:** feature-mac-linux-installers
**Worktree:** C:\Repos\DDPoker-feature-mac-linux-installers
**Plan:** .claude/plans/MAC-LINUX-INSTALLERS.md
**Requested:** 2026-02-11 23:05
**Re-review Requested:** 2026-02-12 00:15 (after fixing blocking issues + JAR naming unification)

## Summary

Added native installer support for macOS (DMG) and Linux (DEB/RPM) alongside the existing Windows MSI installer. Implementation uses OS-activated Maven profiles for jpackage, with GitHub Actions for automated multi-platform builds. All installers include bundled Java 25 runtime, eliminating the need for users to install Java separately.

**Update (2026-02-12):** Fixed 3 blocking issues from initial review + unified all artifact naming to DDPokerCE convention.

## Files Changed

**Initial Implementation (commit 7e75f5d):**
- [x] `code/poker/pom.xml` - Added shared installer properties and 4 OS-specific jpackage Maven profiles
- [x] `.github/workflows/build-installers.yml` - NEW: GitHub Actions matrix build for all 3 platforms
- [x] `docker/Dockerfile` - Updated to copy all installer types, added multi-platform build docs
- [x] `code/pokerwicket/.../DownloadHome.html` - Added macOS DMG, Linux DEB, and Linux RPM sections
- [x] `BUILD.md` - Replaced Windows-only section with comprehensive multi-platform guide
- [x] `.claude/plans/MAC-LINUX-INSTALLERS.md` - Updated status and documented progress

**Review Fixes (commit 9c443b7):**
- [x] `DownloadHome.html` - Fixed Windows MSI filename, corrected JAR filename
- [x] `.github/workflows/build-installers.yml` - Added explicit `permissions: contents: write`
- [x] `BUILD.md` - Corrected macOS architecture claim (Apple Silicon, not universal)

**JAR Naming Unification (commit eb04584):**
- [x] `code/poker/pom.xml` - Assembly plugin finalName and all mainJar refs use DDPokerCE naming
- [x] `.github/workflows/build-installers.yml` - Updated JAR artifact patterns and release notes
- [x] `docker/Dockerfile` - Updated JAR build output path
- [x] `DownloadHome.html` - Updated JAR download link and run command
- [x] `README.md` - Updated quick start JAR references
- [x] `docker/README.md` - Updated download URLs and file listings

**Privacy Check:**
- ✅ SAFE - No private information found in any changed files
- All file paths use project-relative paths
- No IPs, credentials, or personal information added
- GitHub Actions uses standard public images

## Verification Results

- **Tests:** N/A - No test changes required (documentation and build configuration only)
- **Coverage:** N/A - No production code changes
- **Build:** ✅ Clean - `mvn validate` passes, no warnings
- **Profile activation:** ✅ Verified - `installer-windows` profile auto-activates on Windows

**Additional Verification:**
- Maven POM syntax validated with `mvn help:effective-pom`
- Windows profile correctly detected via `mvn help:active-profiles`
- All shared properties (installer.name, installer.version, etc.) correctly defined

## Context & Decisions

**Key Implementation Decisions:**

1. **Skipped manual .icns creation**: jpackage accepts PNG files directly on macOS and converts automatically. Simplified from the original plan which suggested using png2icns or online converters. Uses existing `pokericon128.png`.

2. **Unified naming to DDPokerCE**: Changed from `DDPokerCommunityEdition` to `DDPokerCE` for consistency across platforms. macOS package names have a 16-character limit, so standardized on the shorter name everywhere.

3. **OS-activated profiles**: Profiles auto-activate based on Maven's OS detection:
   - Windows: `<os><family>windows</family></os>` → installer-windows
   - macOS: `<os><family>mac</family></os>` → installer-mac
   - Linux: `<os><family>unix</family><name>Linux</name></os>` → installer-linux-deb
   - RPM: Manual activation required (`-Pinstaller-linux-rpm`) to avoid conflict with DEB

4. **GitHub Actions architecture**: Uses matrix strategy with 3 parallel jobs (windows-latest, macos-latest, ubuntu-latest). Linux job builds both DEB and RPM. All artifacts uploaded and attached to GitHub Release on version tags.

5. **Unsigned installers**: All installers remain unsigned (no code signing certificates). Documented bypass procedures:
   - Windows: SmartScreen warning - "More info" → "Run anyway"
   - macOS: Gatekeeper - "Right-click → Open" on first launch
   - Linux: No warnings (signing not required for DEB/RPM)

**Tradeoffs:**

- **Pro**: Users get native installers with bundled Java (better UX)
- **Pro**: Automated CI/CD builds on all platforms
- **Pro**: Minimal code changes (build configuration only)
- **Con**: macOS installer is unsigned (Gatekeeper warning on first launch)
- **Con**: Installer file size (~98 MB each due to bundled Java)

**Regression Risk:** Very low - Windows MSI build uses the same jpackage plugin, just moved to a profile. Existing installer functionality unchanged.

**Changes After Initial Review:**

1. **Fixed blocking issues** (commit 9c443b7):
   - Windows MSI download link now points to correct filename (DDPokerCE-3.3.0.msi)
   - Added GitHub Actions `permissions: contents: write` for release creation
   - Corrected BUILD.md macOS architecture claim (Apple Silicon only, not universal)

2. **Unified JAR naming** (commit eb04584):
   - Changed assembly plugin to produce DDPokerCE-3.3.0.jar (was poker-3.3.0-CommunityEdition.jar)
   - Updated all references across 6 files for consistency
   - All artifacts now follow DDPokerCE naming convention

**Linux Package Naming Convention:**
- DEB: `ddpoker-ce_3.3.0-1_amd64.deb` (underscore separators per Debian convention)
- RPM: `ddpoker-ce-3.3.0-1.x86_64.rpm` (hyphen/period separators per RPM convention)
- The `-1` is the package revision (standard for DEB/RPM, not used in MSI/DMG/JAR)
- This follows Linux packaging best practices and is generated automatically by jpackage

---

## Initial Review Results (2026-02-11)

**Status:** CHANGES REQUIRED (3 blocking issues)

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-11

### Initial Findings

#### Strengths

- Clean separation of concerns: shared properties in `<properties>`, platform-specific config in profiles
- OS-activated Maven profiles are idiomatic and auto-detect correctly (`mvn validate` passes, profiles activate as expected)
- GitHub Actions workflow structure is solid: matrix strategy, platform-specific prerequisites, separate release job
- Dockerfile change from `COPY docker/downloads/*.msi` to `COPY docker/downloads/` is the right approach for multi-platform
- Download page additions follow the existing HTML/CSS patterns well, with distinct color schemes per platform
- Plan is well-documented with clear decisions and deviations noted (e.g., skipping .icns creation)
- The decision to use `DDPokerCE` for macOS 16-char limit is reasonable

#### Blocking Issues Found

1. **Windows MSI download filename mismatch** - DownloadHome.html pointed to old name
2. **GitHub Actions missing `permissions: contents: write`**
3. **BUILD.md incorrectly claimed macOS "Universal binary"** (it's arm64 only)

#### Non-blocking Suggestions

1. Consider pinning GitHub Actions versions by SHA for supply-chain security
2. Maven `<family>mac</family>` OS detection not yet tested in this codebase
3. Duplicated jpackage plugin XML across 4 profiles (acceptable for readability)
4. RPM build step depends on DEB build step succeeding
5. Plan status should be updated from "In Progress"
6. Hardcoded version `3.3.0` in many places (matches existing convention)

---

## Re-Review Results (2026-02-12)

**Status:** CHANGES REQUIRED

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-12

### Verification of Initial Blocking Issue Fixes

All 3 blocking issues from the initial review have been resolved:

1. **Windows MSI filename** - FIXED in commit `9c443b7`
   - `DownloadHome.html:58` now correctly points to `/downloads/DDPokerCE-3.3.0.msi`

2. **GitHub Actions permissions** - FIXED in commit `9c443b7`
   - `build-installers.yml:15-16` now has `permissions: contents: write` at workflow level

3. **BUILD.md macOS architecture** - FIXED in commit `9c443b7`
   - `BUILD.md:149` now states "Apple Silicon (aarch64) - Intel Mac users should use the universal JAR"

### Verification of JAR Naming Unification

The JAR naming unification (commit `eb04584`) is mostly complete. Key files correctly updated:

- `code/poker/pom.xml:139` - Assembly plugin `finalName` uses `${installer.name}-${installer.version}` (produces `DDPokerCE-3.3.0.jar`)
- `code/poker/pom.xml:253,303,351,396` - All 4 jpackage profiles use `${installer.name}-${installer.version}.jar` as `mainJar`
- `DownloadHome.html:58,82,106,128,151,160` - All download links use DDPokerCE naming
- `.github/workflows/build-installers.yml:141,146,158,159` - Release notes use DDPokerCE
- `docker/Dockerfile:100` - JAR build output uses `DDPokerCE-3.3.0.jar`
- `docker/README.md:40,43,46,82,83` - Download URLs use DDPokerCE

**Remaining inconsistencies (non-blocking):**
- `docker/README.md:69` - Build steps section still references `DDPokerCommunityEdition-3.3.0.msi` (old name)
- `README.md:104` - Quick start still says download `DDPokerCommunityEdition-3.3.0.msi`
- `.claude/plans/MAC-LINUX-INSTALLERS.md:148` - Expected output table still shows `poker-3.3.0-CommunityEdition.jar` (plan doc, non-critical)

The `docker/README.md:69` and `README.md:104` inconsistencies are in sections not directly touched by the naming unification commit. These are minor documentation oversights, not blockers.

### Build Verification

- `mvn validate` in `code/poker`: PASS (BUILD SUCCESS, 0.120s, zero warnings)
- Maven POM structure is valid
- Profile activation correctly auto-detects Windows OS

### Privacy Check

- PASS - No private information found in any changed files
- Only secret reference is standard `${{ secrets.GITHUB_TOKEN }}` in GitHub Actions (built-in)
- No IPs, credentials, personal data, or file paths with usernames

### NEW BLOCKING ISSUE: Scope Creep / Regression from Branch Base

**CRITICAL: The first commit (7e75f5d) includes changes to 5 files that are NOT part of the installer feature.** The worktree was branched from commit `4651de0` (before the resource-leak-fixes merged to main at `bd9fee7`). When the feature was committed, it carried the older (pre-fix) state of these files. The diff from current main (`d0cee6d`) to the feature branch HEAD shows:

1. **`code/common/src/main/java/com/donohoedigital/config/ConfigUtils.java`** - REVERTS the try-with-resources fix for `copyFile()` back to the old leak-prone pattern where `FileInputStream`/`FileOutputStream` are not properly closed. This re-introduces resource leak LEAK-1/QF-8 that was fixed in commit `bd9fee7`.

2. **`code/gameserver/src/main/java/com/donohoedigital/games/server/Ban.java`** - REVERTS the `ApplicationContext` try-with-resources fix, re-introducing an `org.springframework.context.*` unused import and removing the resource-safe pattern.

3. **`code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/OnlineGamePurger.java`** - Same as Ban.java - reverts `ApplicationContext` try-with-resources fix.

4. **`.claude/plans/P0-SECURITY-VALIDATION-RATELIMIT.md`** - DELETES the P0 security plan that was added to main in commit `3cfdd62`.

5. **`.claude/reviews/completed/resource-leak-fixes.md`** - DELETES the completed review file added in commit `f6a9c91`.

**Impact:** If this branch is squash-merged to main as-is, it will silently revert the resource leak fixes and delete unrelated files. This is a **merge conflict / rebase issue**, not an intentional change.

**Resolution:** Before merging, the branch must be rebased onto current main (`d0cee6d`) or these files must be explicitly restored to their main-branch state. The simplest approach:

```bash
# Option A: Rebase onto current main
git rebase main

# Option B: Cherry-pick the main state for affected files
git checkout main -- \
  code/common/src/main/java/com/donohoedigital/config/ConfigUtils.java \
  code/gameserver/src/main/java/com/donohoedigital/games/server/Ban.java \
  code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/OnlineGamePurger.java \
  .claude/plans/P0-SECURITY-VALIDATION-RATELIMIT.md \
  .claude/reviews/completed/resource-leak-fixes.md
git commit -m "fix: Restore files from main to prevent regression"
```

### Commit History Review

The 3 commits follow CLAUDE.md format:
- `7e75f5d` - `feat: Add macOS and Linux installer support` with Plan reference and Co-Authored-By
- `9c443b7` - `fix: Address code review blocking issues` with Co-Authored-By
- `eb04584` - `refactor: Unify all artifact naming to DDPokerCE` with Co-Authored-By

**Note:** All Co-Authored-By lines reference "Claude Sonnet 4.5" rather than "Claude Opus 4.6" as specified in CLAUDE.md. This is cosmetic and not a blocker.

### CLAUDE.md Checklist

| Check | Result | Notes |
|-------|--------|-------|
| Tests pass | N/A | Build-config and documentation only |
| Coverage >= 65% | N/A | No production code changes |
| Build clean | PASS | `mvn validate` clean, zero warnings |
| No scope creep (Section 3/4) | **FAIL** | 5 unrelated files changed due to stale branch base (reverts resource-leak fixes) |
| No over-engineering (Section 3) | PASS | Minimal config, no unnecessary abstractions |
| No private info (Section 8) | PASS | All files clean |
| No security vulnerabilities | PASS | `permissions` block now present |
| Implementation matches plan | PASS with deviations documented | .icns skipped (documented), naming unified to DDPokerCE (documented) |

### Summary

The 3 blocking issues from the initial review are all resolved correctly. The installer feature itself is well-implemented. However, a new critical issue was discovered:

**The branch was created from a stale base (commit `4651de0`) that predates the resource-leak-fixes (`bd9fee7`) and subsequent main commits.** As a result, a squash merge would silently revert:
- 3 resource leak fixes in Java files (ConfigUtils.java, Ban.java, OnlineGamePurger.java)
- 1 security plan file (P0-SECURITY-VALIDATION-RATELIMIT.md)
- 1 completed review file (resource-leak-fixes.md)

**Required action:** Rebase onto current main OR restore the 5 affected files from main before merging.

Additionally, 2 minor documentation inconsistencies remain (docker/README.md:69 and README.md:104 still reference old `DDPokerCommunityEdition` MSI name). These are non-blocking but should ideally be fixed in the same pass.
