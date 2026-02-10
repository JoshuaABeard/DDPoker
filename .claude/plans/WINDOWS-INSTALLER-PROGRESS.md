# Windows Installer Implementation - Progress Summary

## Date: 2026-02-10

## What We've Accomplished

### 1. ✅ jpackage Plugin Configuration
- **File**: `code/poker/pom.xml`
- **Changes**:
  - Added `jpackage-maven-plugin` v1.6.5
  - Configured for Windows MSI installer with:
    - Application name: DDPoker
    - Bundled Java runtime (512MB max heap)
    - Start Menu integration
    - Desktop shortcut creation
    - Directory chooser in installer
    - Icon: Uses existing `pokericon32.ico`

### 2. ✅ Documentation Updates
- **File**: `BUILD.md`
  - Added "Build Windows Installer" section
  - Documented WiX Toolset prerequisite
  - Provided installation instructions
  - Explained SmartScreen warning (unsigned installer)
  - Added testing and uninstall procedures

- **File**: `README.md`
  - Updated "Download Desktop Client" section
  - Added two options: Windows Installer (recommended) and Universal JAR
  - Documented no-Java-required benefit of installer
  - Included SmartScreen bypass instructions

- **File**: `.claude/plans/WINDOWS-INSTALLER-PLAN.md`
  - Added progress tracking section
  - Documented WiX Toolset requirement
  - Marked completed tasks

### 3. ✅ Build Verification
- Successfully built fat JAR: `poker-3.3.0-community-jar-with-dependencies.jar` (21MB)
- Verified jpackage plugin downloads and Maven integration

## ✅ MILESTONE REACHED: Windows Installer Built Successfully!

### Build Results (2026-02-10)
- **File**: `code/poker/target/dist/DDPokerCommunityEdition-3.3.0.msi`
- **Size**: 98MB (includes bundled Java 25 runtime)
- **Build time**: ~38 seconds
- **JAR filename**: `poker-3.3.0-community.jar`
- **Package Name**: DDPokerCommunityEdition (no spaces)
- **Display Name**: DD Poker Community Edition
- **Vendor**: DD Poker Community
- **Copyright**: Copyright 2003-2026 Doug Donohoe
- **Start Menu**: DD Poker Community Edition

### Issues Resolved

**Issue #1: WiX Toolset Not Found**
- **Solution**: Installed WiX Toolset v3.14.1 via `winget install WiXToolset.WiXToolset`
- **Location**: `C:\Program Files (x86)\WiX Toolset v3.14\bin\`

**Issue #2: Invalid Version String**
- **Error**: `Version [3.3.0-community] contains invalid component [0-community]`
- **Root cause**: MSI installers require numeric-only versions (X.Y.Z format)
- **Solution**: Changed `appVersion` from `${project.version}` to hardcoded `3.3.0`

### Resolution Required

**Option 1: Install via winget (Recommended)**
```powershell
# Open PowerShell as Administrator
winget install WiXToolset.WiXToolset
```

**Option 2: Manual Download**
1. Visit: https://wixtoolset.org/releases/
2. Download WiX Toolset 3.14.1 (latest v3.x)
3. Run installer as Administrator
4. Restart terminal

**Verification**:
```powershell
# Should show WiX version
candle.exe -?
light.exe -?
```

## Next Steps

### After WiX Installation

1. **Build the Windows Installer**:
   ```powershell
   cd C:\Repos\DDPoker\code\poker
   mvn clean package jpackage:jpackage
   ```

2. **Expected Output**:
   - File: `target/dist/DDPoker-3.3.0-community.msi`
   - Size: ~130MB (includes Java runtime)
   - Build time: ~2-5 minutes

3. **Test the Installer**:
   - Copy MSI to test machine (or use current machine)
   - Double-click to install
   - Bypass SmartScreen warning ("More info" → "Run anyway")
   - Launch from Start Menu: DD Poker → DDPoker
   - Verify application runs without requiring Java

4. **Verify Installation Locations**:
   - **Program**: `C:\Program Files\DDPoker\`
   - **Database**: `%APPDATA%\ddpoker\poker.mv.db`
   - **Config**: `%APPDATA%\ddpoker\config.json`

5. **Update Plan Document**:
   - Mark "Install WiX Toolset" as completed
   - Mark "Build and test Windows installer" as completed
   - Document any issues encountered

## Remaining Work (From Original Plan)

### Phase 1: Core Installer (Current)
- [x] Add jpackage plugin to pom.xml
- [x] Configure Windows-specific settings
- [x] Use existing icon file
- [ ] **Install WiX Toolset** ← BLOCKER
- [ ] Build Windows installer
- [ ] Test on Windows machine
- [ ] Document SmartScreen behavior

### Phase 2: Docker Integration (Future)
- [ ] Create `docker/downloads/` directory
- [ ] Copy built MSI to docker downloads folder
- [ ] Mount downloads folder in docker-compose.yml
- [ ] Verify both JAR and MSI served by Docker

### Phase 3: Download Page (Future)
- [ ] Update `DownloadPage.html` with dual options
- [ ] Add CSS styling for download cards
- [ ] Mark Windows installer as "Recommended"
- [ ] Add system requirements section

### Phase 4: GitHub Actions (Optional)
- [ ] Create `.github/workflows/build-windows-installer.yml`
- [ ] Configure automated builds on tag push
- [ ] Upload artifacts to GitHub Releases

### Phase 5: Code Signing (Optional, Cost: $75-300/year)
- [ ] Research SSL.com eSigner EV certificate
- [ ] Apply for DigiCert OSS program (free option)
- [ ] Configure signing in jpackage plugin
- [ ] Eliminate SmartScreen warnings

## Files Modified

1. `code/poker/pom.xml` - Added jpackage plugin
2. `BUILD.md` - Added installer build instructions
3. `README.md` - Updated download options
4. `.claude/plans/WINDOWS-INSTALLER-PLAN.md` - Added progress tracking

## Files Created

1. `.claude/plans/WINDOWS-INSTALLER-PROGRESS.md` - This file

## Technical Notes

### Why MSI vs EXE?
- MSI provides better Windows integration (Add/Remove Programs)
- Both require WiX Toolset
- MSI is more "enterprise-friendly"
- Can switch to EXE by changing `<type>MSI</type>` to `<type>EXE</type>`

### Icon File
- Using existing: `src/main/resources/config/poker/images/pokericon32.ico`
- Works for both installer and application
- No additional assets needed

### SmartScreen Warning
- **Expected**: Windows will warn about unsigned installer
- **Bypass**: "More info" → "Run anyway"
- **To Remove**: Requires code signing certificate ($75-300/year)
- **Impact**: Does NOT prevent installation, just adds one extra click

### Installer Size
- **JAR alone**: 21MB
- **Installer with JRE**: ~130MB
- **Why larger**: Bundles entire Java 25 runtime
- **Trade-off**: Larger download, zero-dependency installation

## Success Criteria

When WiX is installed and build completes:

✅ MSI file created in `target/dist/`
✅ Installer runs without errors
✅ Application launches from Start Menu
✅ No Java installation required
✅ Database created in AppData
✅ Config file created in AppData
✅ Uninstall works cleanly
✅ User data preserved after uninstall

## Estimated Time Remaining

- **WiX Installation**: 5 minutes (manual, requires admin)
- **First Build**: 3-5 minutes (downloads dependencies, bundles JRE)
- **Testing**: 10-15 minutes
- **Docker Integration**: 15-20 minutes
- **Download Page Updates**: 20-30 minutes

**Total**: ~1-1.5 hours (after WiX installed)

## Questions for Consideration

1. **MSI vs EXE**: Prefer MSI (current) or switch to EXE?
2. **Code Signing**: Worth $75/year for SSL.com eSigner to remove SmartScreen warning?
3. **Multi-platform**: Add macOS (.dmg) and Linux (.deb) support later?
4. **GitHub Actions**: Automate builds on every release tag?
5. **Version in filename**: Use `DDPoker-3.3.0-community.msi` or just `DDPoker.msi`?

## Resources

- **WiX Toolset**: https://wixtoolset.org/
- **jpackage docs**: https://docs.oracle.com/en/java/javase/25/jpackage/
- **Plugin docs**: https://github.com/petr-panteleyev/jpackage-maven-plugin
- **SSL.com eSigner**: https://www.ssl.com/certificates/ev-code-signing/
- **DigiCert OSS Program**: https://www.digicert.com/signing/code-signing-certificates
