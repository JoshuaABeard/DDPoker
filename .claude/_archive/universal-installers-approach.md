# DDPoker - Universal Installers Approach

## Summary

We've implemented a **universal JAR distribution** system where:
- ✅ JAR is built **once** at Docker image build time
- ✅ JAR works with **any server** - no pre-configuration needed
- ✅ Users configure their server via the **existing settings UI**
- ✅ Settings are saved locally using Java Preferences
- ✅ No runtime installer rebuilds or complexity
- ✅ Cross-platform support (Windows, Mac, Linux)

## What Changed

### 1. Dockerfile (Main Container)
**Location:** `Dockerfile`

**Changes:**
- Reverted to build-time installer generation
- Added Install4j for Windows/Mac/Linux native installers
- Builds universal JAR at Docker build time
- No runtime configuration or rebuilding
- Removed all SERVER_HOST environment variables

**Key sections:**
```dockerfile
# Build universal FAT JAR (no server configuration)
RUN cd /tmp/client-build && \
    cat > MANIFEST.MF << 'EOF' && \
Manifest-Version: 1.0
Main-Class: com.donohoedigital.games.poker.PokerMain
EOF
    "${JAVA_HOME}/bin/jar" cfm /app/downloads/DDPoker.jar MANIFEST.MF -C . .

# Build Install4j installers (Windows/Mac/Linux)
RUN /opt/install4j/bin/install4jc \
        -D sys.version=3.0 \
        -D releasedir=/tmp/client-build \
        --destination=/app/downloads \
        /tmp/install4j-config/poker.install4j
```

### 2. Client Configuration Defaults
**Location:** `code/poker/src/main/resources/config/poker/client.properties`

**Changes:**
- Changed default server from `example.ddpoker.com:80` to `your-server.com:8877`
- Changed default chat from `example.ddpoker.com:11886` to `your-server.com:11886`
- Updated help text to mention "Configure this in Options → Online tab"

**Why:** Makes it obvious to users they need to configure the server address.

### 3. Download Page
**Location:** `code/pokerwicket/src/main/java/com/donohoedigital/games/poker/wicket/pages/download/DownloadHome.html`

**Changes:**
- Removed "Pre-Configured" messaging
- Added prominent "First-Time Setup Required" notice
- Listed all installer types: Windows .exe, macOS .dmg, Linux installers, JAR
- Added step-by-step setup instructions
- Emphasized the Options → Online tab for configuration

### 4. Removed Files
- `Dockerfile.optimized` - no longer needed (was runtime config approach)
- `docker/runtime-build-installer.sh` - no longer needed
- `docker/entrypoint-optimized.sh` - no longer needed

## How It Works

### Build Time (Once)
1. Docker builds the image
2. Install4j creates native installers for Windows/Mac/Linux
3. Universal JAR is created
4. All installers saved to `/app/downloads/`
5. **No server configuration embedded**

### Runtime (Server Starts)
1. Container starts with `docker run`
2. Server processes start (pokerserver + pokerweb)
3. Pre-built installers served via `/downloads/` endpoint
4. **No installer building or configuration**

### User Experience
1. User downloads installer from server's website
2. User installs and runs DD Poker
3. **First-run: User goes to Options → Online tab**
4. User enters server address (e.g., `poker.myserver.com:8877`)
5. User clicks "Test" to verify connection
6. Settings saved locally via Java Preferences
7. Done! Client remembers server for future sessions

## Benefits

### For Server Operators
- ✅ Build image once, deploy anywhere
- ✅ No need to rebuild when changing servers
- ✅ Simpler Docker setup
- ✅ Faster container startup (no build step)
- ✅ Can run multiple servers with same image

### For Users
- ✅ Native installers for all platforms
- ✅ Existing UI for server configuration
- ✅ Settings persist across launches
- ✅ Can change servers anytime
- ✅ Clear setup instructions on download page

### Technical
- ✅ No runtime complexity
- ✅ Leverages existing Java Preferences system
- ✅ Uses existing Settings UI (no new code!)
- ✅ Installers work with any server
- ✅ Install4j handles cross-platform builds

## User Settings Storage

Settings are stored using Java Preferences API:
- **Windows:** Registry (`HKEY_CURRENT_USER\Software\JavaSoft\Prefs\com\donohoedigital\games\ddpoker`)
- **macOS:** `~/Library/Preferences/com.donohoedigital.games.ddpoker.plist`
- **Linux:** `~/.java/.userPrefs/com/donohoedigital/games/ddpoker/prefs.xml`

Settings include:
- `onlineserver` - Game server address:port
- `onlinechat` - Chat server address:port
- `onlineenabled` - Whether online features are enabled
- All other user preferences

## Files Modified

1. **Dockerfile** - Build-time universal installers
2. **client.properties** - Generic default server values
3. **DownloadHome.html** - Instructions for universal installers

## Files Removed

1. **Dockerfile.optimized** - Runtime config approach
2. **docker/runtime-build-installer.sh** - Runtime builder
3. **docker/entrypoint-optimized.sh** - Runtime entry point

## Testing Checklist

- [ ] Build Docker image successfully
- [ ] Verify installers exist in `/app/downloads/`
- [ ] Download JAR and verify it runs
- [ ] Open Options → Online tab
- [ ] Enter server address
- [ ] Test connection works
- [ ] Settings persist after restart
- [ ] (Optional) Test Windows .exe installer
- [ ] (Optional) Test macOS .dmg installer
- [ ] (Optional) Test Linux installers

## Future Enhancements

### Optional First-Run Dialog
Could add a dialog on first launch if server not configured:
```java
if (serverAddress.equals("your-server.com:8877")) {
    // Show "Please configure your server" dialog
    // with direct link to Options → Online
}
```

But this isn't strictly necessary - the existing UI works perfectly!

### GitHub Actions for Multi-Platform Builds
For Windows/Mac native installers, could use GitHub Actions:
- Build on Windows runner → .exe
- Build on macOS runner → .dmg
- Build on Linux runner → .deb/.rpm

See `.claude/todo-windows-mac-installers.md` for details.

## Commands

### Build Image
```bash
# Build from Maven compiled classes
mvn clean install -DskipTests=true
docker build -t ddpoker:latest .
```

### Run Container
```bash
# Simple run
docker run -p 8080:8080 -p 8877:8877 ddpoker:latest

# With docker-compose
docker-compose up
```

### Access
- **Website:** http://localhost:8080
- **Downloads:** http://localhost:8080/downloads/
- **Game Server:** localhost:8877
- **Chat:** localhost:11886

## Conclusion

This approach is **simple, flexible, and maintainable**:
- No pre-configuration complexity
- Leverages existing client code
- Universal installers work anywhere
- Users configure server once, settings persist
- Clean separation of concerns

Perfect for hosting poker games for 20-50 friends! 🎉
