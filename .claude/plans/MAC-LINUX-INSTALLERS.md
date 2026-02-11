# Mac and Linux Installer Plan

**Status:** Planned
**Date:** 2026-02-10
**Prerequisite:** Windows MSI installer (complete - see `completed/WINDOWS-INSTALLER-COMPLETE.md`)

---

## Goal

Build macOS `.dmg` and Linux `.deb`/`.rpm` installers using the same jpackage tool already used for the Windows MSI. All three platforms should produce zero-dependency installers that bundle the Java 25 runtime.

---

## Current State

- **Windows MSI:** Working. Built via `jpackage-maven-plugin` in `code/poker/pom.xml`.
- **macOS / Linux:** Only the universal JAR is available (requires user to install Java 25).
- **Icon files available:**
  - Windows: `pokericon32.ico`
  - PNG (usable for Linux): `pokericon128.png`, `pokericon48.png`, `pokericon32.png`, `pokericon16.png`
  - macOS `.icns`: **Does not exist yet** (needs to be created from PNGs)

---

## Key Constraint: jpackage is Platform-Specific

jpackage **must run on the target OS** — you cannot cross-compile:

| Installer | Must Build On | External Tool Required |
|-----------|--------------|----------------------|
| `.msi` | Windows | WiX Toolset |
| `.dmg` | macOS | None (built-in) |
| `.deb` | Linux | `fakeroot` (usually pre-installed) |
| `.rpm` | Linux | `rpm-build` package |

This means we need a CI/CD pipeline (GitHub Actions) with runners for each platform, or manual builds on each OS.

---

## Phase 1: Maven Profile Setup

**Objective:** Add Maven profiles so the same `pom.xml` can build platform-specific installers.

### 1.1 Create OS-detection profiles in `code/poker/pom.xml`

Replace the current hardcoded Windows jpackage config with three OS-activated profiles:

```xml
<profiles>
  <!-- Windows MSI -->
  <profile>
    <id>installer-windows</id>
    <activation>
      <os><family>windows</family></os>
    </activation>
    <build>
      <plugins>
        <plugin>
          <groupId>org.panteleyev</groupId>
          <artifactId>jpackage-maven-plugin</artifactId>
          <version>1.6.5</version>
          <configuration>
            <!-- shared config (see 1.2) -->
            <type>MSI</type>
            <icon>src/main/resources/config/poker/images/pokericon32.ico</icon>
            <winMenu>true</winMenu>
            <winDirChooser>true</winDirChooser>
            <winShortcut>true</winShortcut>
            <winMenuGroup>DD Poker Community Edition</winMenuGroup>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>

  <!-- macOS DMG -->
  <profile>
    <id>installer-mac</id>
    <activation>
      <os><family>mac</family></os>
    </activation>
    <build>
      <plugins>
        <plugin>
          <groupId>org.panteleyev</groupId>
          <artifactId>jpackage-maven-plugin</artifactId>
          <version>1.6.5</version>
          <configuration>
            <!-- shared config (see 1.2) -->
            <type>DMG</type>
            <icon>src/main/resources/config/poker/images/pokericon.icns</icon>
            <macPackageName>DDPokerCommunityEdition</macPackageName>
            <macPackageIdentifier>com.donohoedigital.games.poker</macPackageIdentifier>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>

  <!-- Linux DEB -->
  <profile>
    <id>installer-linux-deb</id>
    <activation>
      <os><family>unix</family><name>Linux</name></os>
    </activation>
    <build>
      <plugins>
        <plugin>
          <groupId>org.panteleyev</groupId>
          <artifactId>jpackage-maven-plugin</artifactId>
          <version>1.6.5</version>
          <configuration>
            <!-- shared config (see 1.2) -->
            <type>DEB</type>
            <icon>src/main/resources/config/poker/images/pokericon128.png</icon>
            <linuxShortcut>true</linuxShortcut>
            <linuxMenuGroup>Game</linuxMenuGroup>
            <linuxPackageName>ddpoker-community</linuxPackageName>
            <linuxDebMaintainer>ddpoker@example.com</linuxDebMaintainer>
            <linuxAppCategory>games</linuxAppCategory>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>

  <!-- Linux RPM (optional, can be added alongside DEB) -->
  <profile>
    <id>installer-linux-rpm</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.panteleyev</groupId>
          <artifactId>jpackage-maven-plugin</artifactId>
          <version>1.6.5</version>
          <configuration>
            <!-- shared config (see 1.2) -->
            <type>RPM</type>
            <icon>src/main/resources/config/poker/images/pokericon128.png</icon>
            <linuxShortcut>true</linuxShortcut>
            <linuxMenuGroup>Game</linuxMenuGroup>
            <linuxPackageName>ddpoker-community</linuxPackageName>
            <linuxAppCategory>games</linuxAppCategory>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

### 1.2 Shared jpackage config

All profiles share these values (extract to properties or repeat in each profile):

```xml
<name>DDPokerCommunityEdition</name>
<appVersion>3.3.0</appVersion>
<vendor>DD Poker Community</vendor>
<description>DD Poker Community Edition - Texas Hold'em Tournament Poker</description>
<copyright>Copyright 2003-2026 Doug Donohoe</copyright>
<input>target</input>
<destination>target/dist</destination>
<mainJar>poker-${project.version}.jar</mainJar>
<mainClass>com.donohoedigital.games.poker.PokerMain</mainClass>
<javaOptions>
    <option>-Dfile.encoding=UTF-8</option>
    <option>-Xmx512m</option>
</javaOptions>
```

### 1.3 Build command (same on all platforms)

```bash
cd code/poker
mvn clean package assembly:single jpackage:jpackage -DskipTests
```

Maven auto-activates the correct profile based on the OS.

---

## Phase 2: macOS Icon

**Objective:** Create a `.icns` icon file required by macOS DMG installers.

### 2.1 Generate `pokericon.icns` from existing PNGs

On a Mac, use `iconutil`:

```bash
mkdir pokericon.iconset
cp pokericon16.png  pokericon.iconset/icon_16x16.png
cp pokericon32.png  pokericon.iconset/icon_16x16@2x.png
cp pokericon32.png  pokericon.iconset/icon_32x32.png
cp pokericon128.png pokericon.iconset/icon_128x128.png
iconutil -c icns pokericon.iconset -o pokericon.icns
```

Alternatively, use an online PNG-to-ICNS converter or the `png2icns` tool on Linux.

### 2.2 Place icon file

```
code/poker/src/main/resources/config/poker/images/pokericon.icns
```

Commit this to the repo so CI can use it.

---

## Phase 3: GitHub Actions CI/CD

**Objective:** Automate building installers for all three platforms on push/tag.

### 3.1 Create `.github/workflows/build-installers.yml`

```yaml
name: Build Installers

on:
  push:
    tags: ['v*']
  workflow_dispatch:  # manual trigger

jobs:
  build-windows:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
      - name: Install WiX Toolset
        run: choco install wixtoolset -y
      - name: Build MSI
        run: |
          cd code/poker
          mvn clean package assembly:single jpackage:jpackage -DskipTests
      - uses: actions/upload-artifact@v4
        with:
          name: windows-installer
          path: code/poker/target/dist/*.msi

  build-macos:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
      - name: Build DMG
        run: |
          cd code/poker
          mvn clean package assembly:single jpackage:jpackage -DskipTests
      - uses: actions/upload-artifact@v4
        with:
          name: macos-installer
          path: code/poker/target/dist/*.dmg

  build-linux:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'
      - name: Install packaging tools
        run: sudo apt-get install -y fakeroot rpm
      - name: Build DEB
        run: |
          cd code/poker
          mvn clean package assembly:single jpackage:jpackage -DskipTests
      - name: Build RPM (optional)
        run: |
          cd code/poker
          mvn jpackage:jpackage -Dinstaller-linux-rpm -DskipTests
      - uses: actions/upload-artifact@v4
        with:
          name: linux-installers
          path: |
            code/poker/target/dist/*.deb
            code/poker/target/dist/*.rpm
```

### 3.2 Considerations

- **macOS runners on GitHub Actions** are free for public repos, paid for private.
- The macOS build produces an **unsigned** DMG. macOS Gatekeeper will warn users ("unidentified developer"). Users right-click > Open to bypass. This is the same UX trade-off as the Windows SmartScreen warning.
- If the repo is private, you may prefer building Mac/Linux locally or on a self-hosted runner.

---

## Phase 4: Docker Integration

**Objective:** Serve all platform installers from the Docker container's `/downloads` page.

### 4.1 Update Dockerfile

Change the copy line from MSI-only to all installers:

```dockerfile
# Copy all pre-built platform installers
COPY docker/downloads/*.msi /app/downloads/
COPY docker/downloads/*.dmg /app/downloads/
COPY docker/downloads/*.deb /app/downloads/
```

Use wildcards so the build doesn't fail if an installer is missing (or use `COPY --chown` with a directory).

### 4.2 Update download page (`DownloadHome.html`)

Add macOS and Linux download options alongside the existing Windows/JAR options:

| Option | File | Size (est.) | Notes |
|--------|------|-------------|-------|
| Windows Installer | `.msi` | ~98 MB | Existing |
| macOS Installer | `.dmg` | ~120 MB | New |
| Linux (Debian/Ubuntu) | `.deb` | ~110 MB | New |
| Linux (Fedora/RHEL) | `.rpm` | ~110 MB | New (optional) |
| Universal JAR | `.jar` | ~23 MB | Existing, requires Java 25 |

### 4.3 Update `docker/README.md`

Document the build steps for each platform's installer.

---

## Phase 5: Testing

### 5.1 macOS Testing

- [ ] DMG opens and shows app + Applications folder alias
- [ ] Drag-to-install works
- [ ] App launches from Applications
- [ ] Gatekeeper bypass works (right-click > Open)
- [ ] App data stored in `~/Library/Application Support/DDPokerCommunityEdition/` or `~/.ddpoker/`

### 5.2 Linux Testing

- [ ] DEB installs via `sudo dpkg -i` or `sudo apt install ./`
- [ ] RPM installs via `sudo rpm -i` or `sudo dnf install ./` (if building RPM)
- [ ] Desktop shortcut appears in application menu
- [ ] App launches from menu and command line
- [ ] App data stored in `~/.ddpoker/` or `~/.local/share/DDPokerCommunityEdition/`
- [ ] Uninstall removes application but preserves user data

---

## Task Summary

| # | Task | Effort | Platform Needed |
|---|------|--------|----------------|
| 1 | Add Maven profiles for Mac/Linux to `pom.xml` | Small | Any (edit only) |
| 2 | Create `.icns` icon for macOS | Small | Mac (or converter) |
| 3 | Create GitHub Actions workflow | Medium | Any (edit only) |
| 4 | Build and test macOS DMG | Medium | Mac |
| 5 | Build and test Linux DEB | Medium | Linux |
| 6 | Build RPM (optional) | Small | Linux |
| 7 | Update Dockerfile for multi-platform downloads | Small | Any |
| 8 | Update download page HTML | Small | Any |
| 9 | Update documentation (BUILD.md, README.md, docker/README.md) | Small | Any |

---

## Open Questions

1. **RPM support** — Do we need RPM (Fedora/RHEL) in addition to DEB (Debian/Ubuntu), or is DEB sufficient for now?
2. **macOS architectures** — GitHub Actions `macos-latest` runs on Apple Silicon (arm64). Do we need an x86_64 DMG as well for older Macs? (Temurin JDK 25 supports both, but the bundled runtime would be arch-specific.)
3. **Linux maintainer email** — The DEB `linuxDebMaintainer` field needs a real email address. What should it be?
4. **App data location** — jpackage defaults vary by platform. Should we verify/override the data directory to always use `~/.ddpoker/`?

---

## What We Can Do Now (on Windows)

Tasks 1, 3, 7, 8, and 9 can all be done on this Windows machine — they're just file edits. Tasks 2, 4, 5, and 6 require Mac/Linux access (or CI).
