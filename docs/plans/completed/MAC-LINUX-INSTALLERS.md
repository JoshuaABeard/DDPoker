# Plan: macOS and Linux Installers via jpackage

**Status:** In Progress
**Date:** 2026-02-11
**Branch:** feature-mac-linux-installers

## Context

DDPoker currently has a Windows MSI installer built with `jpackage-maven-plugin` (hardcoded in `code/poker/pom.xml`). Mac and Linux users can only use the universal JAR, which requires Java 25 pre-installed. This plan adds macOS DMG and Linux DEB/RPM installers using the same jpackage tool, with GitHub Actions for automated builds on all 3 platforms.

## Decisions

- **App name:** `DDPokerCE` on all platforms (macOS requires <16 char package name, so standardize everywhere)
- **Linux:** Both DEB (Debian/Ubuntu) and RPM (Fedora/RHEL)
- **macOS icon:** Use `png2icns` (Linux cross-platform tool) or online converter since no Mac is available
- **Code signing:** Skipped (unsigned, users bypass OS warnings)
- **Approach:** OS-activated Maven profiles replace the current hardcoded Windows config

## Files to Change

| File | Action |
|------|--------|
| `code/poker/pom.xml` | Remove hardcoded jpackage config, add 4 OS profiles (Windows/Mac/Linux-DEB/Linux-RPM) with shared properties |
| `code/poker/src/main/resources/config/poker/images/pokericon.icns` | NEW — macOS icon created from existing PNGs |
| `.github/workflows/build-installers.yml` | NEW — GitHub Actions matrix build for all 3 platforms |
| `docker/Dockerfile` | Update COPY to include *.dmg, *.deb, *.rpm alongside *.msi |
| `code/pokerwicket/.../pages/download/DownloadHome.html` | Add macOS and Linux download sections |
| `BUILD.md` | Add multi-platform build instructions |

## Step-by-Step Implementation

### Step 1: Create macOS .icns icon

Generate `pokericon.icns` from existing PNGs using a cross-platform approach. Options:
- `png2icns` tool on Linux/WSL: `png2icns pokericon.icns pokericon128.png pokericon48.png pokericon32.png pokericon16.png`
- Online converter (e.g., cloudconvert.com PNG→ICNS)
- On WSL/Linux: install `icnsutils` package (`sudo apt install icnsutils`)

Place at: `code/poker/src/main/resources/config/poker/images/pokericon.icns`

### Step 2: Update `code/poker/pom.xml`

**2a.** Add installer properties after `<name>poker</name>` (line 49):

```xml
<properties>
  <installer.name>DDPokerCE</installer.name>
  <installer.version>3.3.0</installer.version>
  <installer.vendor>DD Poker Community</installer.vendor>
  <installer.description>DD Poker Community Edition - Texas Hold'em Tournament Poker</installer.description>
  <installer.copyright>Copyright 2003-2026 Doug Donohoe</installer.copyright>
</properties>
```

**2b.** Remove the existing jpackage plugin block (lines 147-183).

**2c.** Add `<profiles>` section before `</project>` with 4 profiles:

- **`installer-windows`** — activated by `<os><family>windows</family></os>`
  - `<type>MSI</type>`, icon `.ico`, `winMenu/winShortcut/winDirChooser/winMenuGroup`

- **`installer-mac`** — activated by `<os><family>mac</family></os>`
  - `<type>DMG</type>`, icon `.icns`, `<macPackageName>DD Poker CE</macPackageName>`, `<macPackageIdentifier>com.donohoedigital.games.poker</macPackageIdentifier>`

- **`installer-linux-deb`** — activated by `<os><family>unix</family><name>Linux</name></os>`
  - `<type>DEB</type>`, icon `.png` (128px), `linuxShortcut=true`, `linuxMenuGroup=Games`, `linuxAppCategory=Game`, `linuxPackageName=ddpoker-ce`

- **`installer-linux-rpm`** — manually activated (`-Pinstaller-linux-rpm`), NOT auto-activated
  - `<type>RPM</type>`, same Linux settings as DEB plus `<linuxRpmLicenseType>GPLv3</linuxRpmLicenseType>`
  - Not auto-activated because DEB and RPM can't both auto-activate on Linux (same OS detection)

All profiles share the same common config via properties:
```xml
<name>${installer.name}</name>
<appVersion>${installer.version}</appVersion>
<vendor>${installer.vendor}</vendor>
<description>${installer.description}</description>
<copyright>${installer.copyright}</copyright>
<input>target</input>
<destination>target/dist</destination>
<mainJar>poker-${project.version}.jar</mainJar>
<mainClass>com.donohoedigital.games.poker.PokerMain</mainClass>
<javaOptions>
  <option>-Dfile.encoding=UTF-8</option>
  <option>-Xmx512m</option>
</javaOptions>
```

### Step 3: Create GitHub Actions workflow

**File:** `.github/workflows/build-installers.yml`

- **Trigger:** Push to tags matching `v*.*.*`, plus `workflow_dispatch` for manual runs
- **Matrix:** `windows-latest`, `macos-latest`, `ubuntu-latest`
- **Steps per job:**
  1. Checkout
  2. Setup JDK 25 (Temurin)
  3. Windows-only: Install WiX Toolset via `choco install wixtoolset`
  4. Linux-only: Install `fakeroot` and `rpm` packages
  5. Build: `cd code/poker && mvn clean package assembly:single jpackage:jpackage -DskipTests`
  6. Linux-only: Also run `mvn jpackage:jpackage -Pinstaller-linux-rpm` for RPM
  7. Upload artifacts
- **Release job:** Downloads all artifacts, creates GitHub Release with all installers attached

### Step 4: Update Dockerfile

**File:** `docker/Dockerfile` (around line 120)

Change:
```dockerfile
COPY docker/downloads/*.msi /app/downloads/
```
To:
```dockerfile
# Copy all pre-built platform installers (build on each platform or download from GitHub Releases)
COPY docker/downloads/ /app/downloads/
```

Update the surrounding comments to document all platform build commands.

### Step 5: Update download page

**File:** `code/pokerwicket/.../pages/download/DownloadHome.html`

Insert after the Windows installer section (line 75), before the JAR section:

- **macOS section** — green-blue border, DMG download link, drag-to-Applications instructions, Gatekeeper bypass note
- **Linux DEB section** — orange border, DEB download link, `dpkg -i` install instructions
- **Linux RPM section** — similar to DEB but with `dnf install` / `rpm -i` instructions

### Step 6: Update BUILD.md

Add "Multi-Platform Installers" section documenting:
- Prerequisites per platform (WiX for Windows, fakeroot/rpm for Linux, nothing for macOS)
- Unified build command: `mvn clean package assembly:single jpackage:jpackage -DskipTests`
- RPM build command: `mvn jpackage:jpackage -Pinstaller-linux-rpm`
- Output file locations
- CI/CD tag-based release process

## Expected Output Files

| Platform | Filename | Size (est.) |
|----------|----------|-------------|
| Windows | `DDPokerCE-3.3.0.msi` | ~98 MB |
| macOS | `DDPokerCE-3.3.0.dmg` | ~98 MB |
| Linux (DEB) | `ddpoker-ce_3.3.0-1_amd64.deb` | ~98 MB |
| Linux (RPM) | `ddpoker-ce-3.3.0-1.x86_64.rpm` | ~98 MB |
| Universal | `poker-3.3.0-CommunityEdition.jar` | ~21 MB |

## Verification

1. **Windows (local):** Run `mvn clean package assembly:single jpackage:jpackage -DskipTests` in `code/poker` — verify MSI appears in `target/dist/` with new name `DDPokerCE-3.3.0.msi`
2. **GitHub Actions:** Push a test tag, verify all 3 matrix jobs pass, download artifacts
3. **macOS:** Install DMG on a Mac (or in CI), verify app launches
4. **Linux:** Install DEB on Ubuntu (or in CI), verify app launches; install RPM on Fedora
5. **Docker:** Copy all installers to `docker/downloads/`, rebuild Docker image, verify all download links work on the download page
6. **Regression:** Existing Windows MSI build still works identically (same command, just different output filename)

## Risk Notes

- **macOS unsigned DMG:** Users must right-click > Open to bypass Gatekeeper. Same trade-off as Windows SmartScreen.
- **RPM naming:** jpackage generates RPM filenames automatically; exact name may differ slightly from table above.
- **GitHub Actions cost:** macOS runners are 10x cost of Linux for private repos. For a public repo, all are free.
- **macOS arch:** `macos-latest` is Apple Silicon (arm64). Intel Mac users would need a separate x86_64 build or use the universal JAR. This can be addressed later if needed.

---

## Implementation Progress

### Completed Steps (2026-02-11)

#### ✅ Step 1: macOS Icon (Simplified)
- **Decision**: Skipped manual .icns creation - jpackage accepts PNG files directly on macOS and converts automatically
- Using existing `pokericon128.png` for all platforms except Windows (which uses `.ico`)

#### ✅ Step 2: Updated pom.xml
- Added shared installer properties:
  - `installer.name`: DDPokerCE
  - `installer.version`: 3.3.0
  - `installer.vendor`, `installer.description`, `installer.copyright`
- Removed hardcoded jpackage plugin configuration
- Added 4 Maven profiles:
  - **installer-windows**: Auto-activated on Windows, builds MSI
  - **installer-mac**: Auto-activated on macOS, builds DMG
  - **installer-linux-deb**: Auto-activated on Linux, builds DEB
  - **installer-linux-rpm**: Manual activation (`-Pinstaller-linux-rpm`), builds RPM
- All profiles share common config via properties for consistency

#### ✅ Step 3: GitHub Actions Workflow
- Created `.github/workflows/build-installers.yml`
- Matrix build strategy: windows-latest, macos-latest, ubuntu-latest
- Prerequisites installed per platform:
  - Windows: WiX Toolset via choco
  - Linux: fakeroot + rpm packages
  - macOS: No additional prereqs needed
- Linux job builds both DEB and RPM
- Automatic GitHub Release creation on version tags with all installers attached
- Manual trigger via workflow_dispatch for testing

#### ✅ Step 4: Updated Dockerfile
- Changed `COPY docker/downloads/*.msi` to `COPY docker/downloads/` to include all installer types
- Updated comments with build instructions for all 3 platforms
- Added alternative option to download pre-built installers from GitHub Releases

#### ✅ Step 5: Updated Download Page
- Added 3 new platform sections to `DownloadHome.html`:
  - **macOS DMG**: Blue theme, drag-to-Applications instructions, Gatekeeper bypass note
  - **Linux DEB**: Orange theme, dpkg install command
  - **Linux RPM**: Pink theme, dnf/rpm install commands
- Each section includes file size (~98 MB), platform requirements, and installation steps
- Preserved existing Windows MSI and Cross-Platform JAR sections

#### ✅ Step 6: Updated BUILD.md
- Replaced "Build Windows Installer" section with comprehensive "Multi-Platform Installers" section
- Documented unified build command that works on all platforms
- Added platform-specific subsections:
  - Windows: WiX Toolset installation and verification
  - macOS: Gatekeeper bypass instructions
  - Linux DEB: fakeroot prerequisite
  - Linux RPM: Explicit profile activation required
- Added CI/CD section documenting GitHub Actions automated builds
- Added note on code signing costs and unsigned installer workarounds

### Next Steps

1. **Verification**:
   - Test pom.xml syntax: `mvn help:effective-pom` ✅ (Already validated)
   - Local build test on Windows: `mvn clean package assembly:single jpackage:jpackage -DskipTests`
   - Commit changes to worktree
   - Test GitHub Actions workflow (push to feature branch or use workflow_dispatch)

2. **Regression Testing**:
   - Verify Windows MSI still builds identically with new profile system
   - Check that profile auto-activation works correctly
   - Ensure installer naming is consistent (DDPokerCE vs old DDPokerCommunityEdition)

3. **Code Review**:
   - Request review per CLAUDE.md Section 9
   - Create `.claude/reviews/feature-mac-linux-installers.md` handoff file

4. **Completion**:
   - After approval: squash merge to main
   - Move plan to `.claude/plans/completed/`
   - Clean up worktree
