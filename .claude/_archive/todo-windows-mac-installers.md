# TODO: Windows and Mac Native Installers

## Current Status

✅ **Working:**
- Cross-platform JAR (21 MB) - works on Windows/Mac/Linux
- Linux native app via jpackage in Docker
- 4-second build time
- No external dependencies

❌ **Missing:**
- Windows .exe installer
- macOS .dmg/.pkg installer

## The Problem

**jpackage only builds for the platform it runs on:**
- Linux Docker container → Can only build Linux apps
- Windows machine needed → For .exe
- macOS machine needed → For .dmg

## Solution Options

### Option 1: GitHub Actions (Recommended)
Build native installers in CI/CD on each platform.

**Pros:**
- Automated builds for all platforms
- Can trigger on git push or manually
- Free for public repos
- No local machine needed

**Cons:**
- Requires GitHub Actions setup
- Longer build times (but automated)
- Need to configure secrets for signing (optional)

**Implementation:**
```yaml
# .github/workflows/build-installers.yml
jobs:
  build-windows:
    runs-on: windows-latest
    # Build with jpackage on Windows

  build-mac:
    runs-on: macos-latest
    # Build with jpackage on macOS

  build-linux:
    runs-on: ubuntu-latest
    # Build with jpackage on Linux
```

### Option 2: Install4j (Cross-Platform Builds)
Re-add Install4j but make it optional and handle the URL concern.

**Pros:**
- Can build all platforms from Linux Docker
- Single build location
- Professional installers

**Cons:**
- Reintroduces URL/dependency concerns
- 150MB image size increase
- Licensing complexity

**Mitigation:**
- Bundle Install4j in the repo (if license allows)
- Or download and cache in Docker build layer
- Graceful fallback to JAR if Install4j unavailable

### Option 3: Pre-build and Mount
Build installers manually on local Windows/Mac, mount into container.

**Pros:**
- Simple
- No CI/CD setup
- Full control

**Cons:**
- Manual process
- Requires local Windows/Mac machines
- Tedious for frequent updates

### Option 4: Hybrid Approach
- JAR: Primary distribution (always available)
- Linux: Built in Docker with jpackage
- Windows/Mac: Built via GitHub Actions or Install4j

**Pros:**
- Best of both worlds
- JAR always works
- Native installers as bonus

**Cons:**
- More complex setup

## Recommendation

**Start with Option 1 (GitHub Actions):**

1. Create `.github/workflows/build-installers.yml`
2. Build on push to `main` or manual trigger
3. Upload artifacts to GitHub Releases
4. Update download page to link to releases

**Later, if needed:**
- Add Install4j back as optional enhancement
- Or keep it simple with JAR + Actions

## Files to Update When Implementing

### For GitHub Actions:
- `.github/workflows/build-installers.yml` (new)
- `docker/build-installers-local.sh` (helper script for jpackage)
- Download page: link to GitHub Releases

### For Install4j:
- `Dockerfile.optimized` (add Install4j back, make optional)
- `docker/runtime-build-installer.sh` (restore Install4j logic)
- Could bundle Install4j tar in repo to avoid URL concerns

## Build Command Examples

### Windows (on Windows machine):
```bash
jpackage --input dist/ --name DDPoker --main-jar DDPoker.jar \
  --type exe --win-menu --win-shortcut \
  --app-version 3.0 --vendor "DD Poker"
```

### macOS (on Mac machine):
```bash
jpackage --input dist/ --name DDPoker --main-jar DDPoker.jar \
  --type dmg --mac-package-name "DDPoker" \
  --app-version 3.0 --vendor "DD Poker"
```

### Linux (in Docker - already working):
```bash
jpackage --input dist/ --name DDPoker --main-jar DDPoker.jar \
  --type app-image \
  --app-version 3.0 --vendor "DD Poker"
```

## Current Workaround

For now, your 20-50 friends can use:
- **JAR file** - works on all platforms (requires Java 25)
- Most gamers have Java installed already (Minecraft, etc.)
- Simple: `java -jar DDPoker.jar`

## Timeline

**Phase 1 (Current):** ✅ JAR distribution - DONE
**Phase 2 (Future):** Add Windows/Mac native installers via GitHub Actions
**Phase 3 (Optional):** Code signing for installers (Windows/Mac require this for non-scary installs)

---

**Next Steps:**
1. Use JAR-only for initial deployment
2. Gather feedback from users
3. If native installers are really needed, implement GitHub Actions
4. Consider code signing if users report OS warnings

## Notes

- JAR works great for tech-savvy users (like poker-playing friends)
- Native installers nice-to-have but not essential
- Most concerns about "install Java" are overblown - it's one download
- Code signing costs money ($99/year for Mac, $75-500 for Windows)
