# DD Poker - Universal Installer Deployment Summary

## What We Built

A complete deployment system for DD Poker with:
1. ✅ **Universal installers** - built at Docker image creation time
2. ✅ **First-run dialog** - automatically prompts users to configure server
3. ✅ **User-friendly setup** - perfect for non-technical users
4. ✅ **No pre-configuration needed** - one image works everywhere

## Files Created/Modified

### Client Code (New)
- **`ServerConfigDialog.java`** - Simple first-run configuration dialog
  - Uses standard Swing components
  - Validates format (hostname:port)
  - Saves to Java Preferences
  - Auto-enables online features

### Client Code (Modified)
- **`PokerMain.java`** - Added first-run detection
  - Checks if server is still default `your-server.com`
  - Shows dialog before main menu if unconfigured
  - Integrated into existing startup flow

- **`client.properties`** - Changed default server values
  - `option.onlineserver.default = your-server.com:8877`
  - `option.onlinechat.default = your-server.com:11886`
  - Makes it obvious users need to configure

### Docker & Build
- **`Dockerfile`** - Build-time universal installers
  - Install4j for Windows/Mac/Linux native installers
  - Universal JAR (~20 MB)
  - No runtime configuration
  - No SERVER_HOST environment variable

- **`docker-compose.yml`** - Already set up for deployment

### Website
- **`DownloadHome.html`** - Updated download page
  - Lists all installer types
  - Clear setup instructions
  - Mentions first-run configuration

## How It Works

### Build Time (Once - 5-10 minutes)
```bash
# Compile Java
mvn clean install -DskipTests=true

# Build Docker image
docker build -t ddpoker:latest .
```

**What happens:**
1. Maven compiles all code including ServerConfigDialog
2. Docker copies compiled classes
3. Install4j creates Windows .exe, macOS .dmg, Linux installers
4. JAR created with all dependencies
5. All installers saved to image at `/app/downloads/`

### Runtime (Seconds)
```bash
docker run -p 8080:8080 -p 8877:8877 ddpoker:latest
```

**What happens:**
1. Server processes start (pokerserver + pokerweb)
2. Pre-built installers served via `/downloads/` endpoint
3. No installer building - instant startup!

### User Experience
1. User visits `http://yourserver.com:8080`
2. Downloads installer (Windows .exe, Mac .dmg, or JAR)
3. Installs and launches DD Poker
4. **First-run dialog appears:** "Welcome to DD Poker!"
5. User enters: `poker.yourserver.com:8877` and `poker.yourserver.com:11886`
6. Clicks OK
7. Settings saved - dialog never shows again!
8. Main menu appears - ready to play

## Deployment Steps

### 1. Build the Image
```bash
# From repo root
mvn clean install -DskipTests=true
docker build -t ddpoker:latest .
```

### 2. Run the Container
```bash
# Simple
docker run -p 8080:8080 -p 8877:8877 -p 11886:11886/udp ddpoker:latest

# Or with docker-compose
docker-compose up
```

### 3. Share with Friends
Tell your friends:
> "Go to http://poker.yourserver.com:8080
>
> Download the installer for your computer
>
> When you first run it, enter:
> - Game Server: **poker.yourserver.com:8877**
> - Chat Server: **poker.yourserver.com:11886**
>
> Click OK and you're ready to play!"

## Testing the First-Run Dialog

### To Test Fresh Install
Need to clear Java preferences to simulate first run:

**Windows:**
```cmd
reg delete "HKCU\Software\JavaSoft\Prefs\com\donohoedigital\games\ddpoker" /f
```

**macOS:**
```bash
rm ~/Library/Preferences/com.donohoedigital.games.ddpoker.plist
```

**Linux:**
```bash
rm -rf ~/.java/.userPrefs/com/donohoedigital/games/ddpoker/
```

Then run the client - dialog should appear!

## What's Different from Before

### Before (Runtime Configuration)
- ❌ Installers built at container startup (slow)
- ❌ Runtime configuration changes
- ❌ Complex entrypoint scripts
- ❌ Users had to manually configure in Options

### After (Universal Installers)
- ✅ Installers built at image build time (fast startup)
- ✅ No runtime configuration
- ✅ Simple entrypoint
- ✅ First-run dialog guides users automatically

## Benefits

### For You (Server Operator)
- ✅ Build once, deploy anywhere
- ✅ No rebuild when server changes
- ✅ Fast container startup
- ✅ Less user support needed (dialog guides them)
- ✅ One image for multiple servers

### For Your Friends (Users)
- ✅ Can't miss configuration (dialog appears automatically)
- ✅ Simple two-field form
- ✅ Format validation prevents mistakes
- ✅ One-time setup
- ✅ Works just like Options dialog
- ✅ Native installers or cross-platform JAR

## Architecture

```
Build Time:
┌─────────────┐
│ Maven Build │ → Compile all Java (including ServerConfigDialog)
└──────┬──────┘
       ↓
┌──────────────┐
│ Docker Build │ → Install4j creates Windows/Mac/Linux installers
│              │ → JAR created
│              │ → Saved to /app/downloads/
└──────────────┘

Runtime:
┌────────────┐
│ Docker Run │ → Start server processes
│            │ → Serve pre-built installers
└────────────┘

First Launch:
┌─────────────┐
│ User Launch │ → PokerMain.initialStart() checks prefs
│             │ → If "your-server.com" → Show dialog
│             │ → User enters server address
│             │ → Save to Java Preferences
│             │ → Continue to main menu
└─────────────┘

Subsequent Launches:
┌─────────────┐
│ User Launch │ → PokerMain.initialStart() checks prefs
│             │ → Server configured → Skip dialog
│             │ → Go straight to main menu
└─────────────┘
```

## Files in Container

```
/app/
├── classes/          # All compiled server classes
├── lib/              # All JAR dependencies
├── webapp/           # Web application
├── downloads/        # Pre-built installers
│   ├── DDPoker.jar   # Cross-platform (21 MB)
│   ├── DDPoker-windows.exe (if Install4j succeeds)
│   ├── DDPoker-macos.dmg (if Install4j succeeds)
│   └── DDPoker-linux.sh (if Install4j succeeds)
├── entrypoint.sh     # Start script
└── runtime/
    └── messages/     # Runtime message files

/data/                # Persistent volume
├── poker.*           # H2 database files
└── work/             # Runtime work directory
```

## Ports

- **8877** - Game server (TCP)
- **8080** - Web portal (HTTP)
- **11886** - Chat server (UDP)
- **11889** - Connection test (UDP)

## Environment Variables

```dockerfile
# Database (H2 by default)
DB_DRIVER=org.h2.Driver
DB_URL=jdbc:h2:file:/data/poker;MODE=MySQL;AUTO_SERVER=TRUE
DB_USER=sa
DB_PASSWORD=

# Ports (no SERVER_HOST needed!)
SERVER_PORT=8877
CHAT_PORT=11886
WEB_PORT=8080
```

## Next Steps

### For Deployment
1. Build the Docker image
2. Run the container
3. Test the download page
4. Download client and test first-run dialog
5. Share with friends!

### For Windows/Mac Native Installers
See `.claude/todo-windows-mac-installers.md` for:
- GitHub Actions approach (recommended)
- jpackage on each platform
- Install4j improvements

### For Enhanced First-Run
Future enhancements could include:
- Actual connection test (ping server)
- Auto-fill chat server from game server
- QR code scanning
- Server list/directory
- Remember recent servers

## Success Criteria

✅ Maven build succeeds
✅ Docker image builds successfully
✅ Container starts in < 10 seconds
✅ Downloads page shows installers
✅ JAR download works
✅ JAR launches successfully
✅ First-run dialog appears
✅ Server address can be entered
✅ Settings save correctly
✅ Dialog doesn't show again
✅ Can connect to server

## Documentation

- **This file** - Deployment overview
- `.claude/universal-installers-approach.md` - Technical details
- `.claude/first-run-server-config.md` - Dialog implementation
- `.claude/todo-windows-mac-installers.md` - Future enhancements

---

**Ready to deploy!** 🎉
