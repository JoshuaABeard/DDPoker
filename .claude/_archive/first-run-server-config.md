# First-Run Server Configuration

## Overview

Added a friendly first-run dialog that automatically detects if the user needs to configure their server and guides them through the setup process.

## How It Works

### Detection Logic
On application startup, checks if the online server preference is still set to the default `your-server.com:8877`. If yes, shows the welcome dialog.

### User Experience
1. User launches DD Poker for the first time
2. **Welcome dialog appears** with friendly message
3. Two simple text fields:
   - Game Server (e.g., `localhost:8877`)
   - Chat Server (e.g., `localhost:11886`)
4. Three buttons:
   - **"Configure Later"** - Skip for now, configure in Options later
   - **"Test Connection"** - Validate format and test (simplified for now)
   - **"Save & Continue"** - Save settings and proceed to main menu

### Settings Persistence
- Saves to Java Preferences (same as Options dialog)
- Sets `online.enabled` to true automatically
- Settings persist across launches
- Won't show dialog again once configured

## Files Modified

### 1. PokerMain.java
**Location:** `code/poker/src/main/java/com/donohoedigital/games/poker/PokerMain.java`

**Changes:**
- Added `initialStart()` override to check for first-run
- Added `showFirstRunServerConfig()` method
- Detects if server is still default value

**Code:**
```java
@Override
protected void initialStart()
{
    // Check if online server is still the default value
    String serverAddress = Prefs.getUserPrefs(getPrefsNodeName()).get(
            EngineConstants.OPTION_ONLINE_SERVER,
            PropertyConfig.getStringProperty("option.onlineserver.default", ""));

    // If server is unconfigured (still default), show welcome dialog
    if (serverAddress.contains("your-server.com"))
    {
        showFirstRunServerConfig();
    }

    // Continue with normal startup
    super.initialStart();
}
```

### 2. ServerConfigDialog.java (NEW)
**Location:** `code/poker/src/main/java/com/donohoedigital/games/poker/ServerConfigDialog.java`

**Purpose:** Modal dialog for first-run server configuration

**Features:**
- Friendly welcome message
- Simple two-field form (game server, chat server)
- Format validation (hostname:port)
- Test connection button (simplified)
- Save settings button
- "Configure Later" option

**UI Layout:**
```
┌─────────────────────────────────────────┐
│ Welcome to DD Poker!              [X]   │
├─────────────────────────────────────────┤
│ Welcome!                                 │
│ To play online poker, you need to       │
│ connect to a poker server.              │
│ Please enter the server address:        │
│                                          │
│ ┌─ Server Settings ──────────────────┐  │
│ │ Game Server: [localhost:8877    ]  │  │
│ │ Chat Server: [localhost:11886   ]  │  │
│ └─────────────────────────────────────┘  │
│                                          │
│ Status message here                      │
│                                          │
│    [Configure Later] [Test] [Save & Continue]│
└─────────────────────────────────────────┘
```

## Benefits

### For Your Friends (Non-Technical Users)
- ✅ **Automatic detection** - No need to remember to configure
- ✅ **Can't be missed** - Shows on first launch
- ✅ **Simple interface** - Just two fields
- ✅ **Clear instructions** - Friendly welcome message
- ✅ **Validation** - Format checking prevents mistakes
- ✅ **Escape hatch** - "Configure Later" for power users

### For You (Server Operator)
- ✅ **Less support** - Users guided through setup
- ✅ **Clear messaging** - Can provide exact server address
- ✅ **One-time setup** - Never shows again once configured
- ✅ **Fallback option** - Still have Options → Online for changes

## User Flow

### First Launch (Unconfigured)
```
1. User double-clicks DDPoker.jar (or runs installer)
2. [Splash screen]
3. **DIALOG APPEARS:** "Welcome to DD Poker!"
4. User enters: poker.yourserver.com:8877
5. User enters: poker.yourserver.com:11886
6. User clicks "Save & Continue"
7. Main menu appears
8. User can now play online!
```

### Subsequent Launches
```
1. User double-clicks DDPoker.jar
2. [Splash screen]
3. Main menu appears (no dialog)
4. Settings remembered!
```

### Manual Configuration
If user clicks "Configure Later":
```
1. Main menu appears
2. User goes to Options → Online tab
3. Enters server addresses there
4. Saves
```

## Default Values

The dialog pre-fills with sensible defaults:
- **Game Server:** `localhost:8877` (for testing)
- **Chat Server:** `localhost:11886` (for testing)

Users just need to change `localhost` to their actual server hostname.

## Future Enhancements

### Possible Improvements
1. **Actual connection test** - Could ping the server to verify
2. **Auto-fill chat from game server** - If they're on same host
3. **QR code support** - Scan server config from website
4. **Server list** - Dropdown of known servers
5. **Remember last N servers** - Quick switch between servers

### Current Simplification
The "Test Connection" button currently just validates the format. A full implementation could:
```java
private void testConnection()
{
    // Parse server address
    String[] parts = serverField.getText().split(":");
    String host = parts[0];
    int port = Integer.parseInt(parts[1]);

    // Try to connect
    try {
        Socket socket = new Socket(host, port);
        socket.close();
        statusLabel.setText("✓ Connection successful!");
    } catch (IOException e) {
        statusLabel.setText("⚠️ Could not connect to server");
    }
}
```

But for your use case (20-50 friends, single server), format validation is probably sufficient.

## Testing Checklist

- [ ] Clean Java preferences (to simulate first run)
- [ ] Launch DD Poker
- [ ] Verify welcome dialog appears
- [ ] Enter server address
- [ ] Click "Save & Continue"
- [ ] Verify main menu appears
- [ ] Restart DD Poker
- [ ] Verify welcome dialog does NOT appear
- [ ] Check Options → Online shows saved values
- [ ] Test "Configure Later" button
- [ ] Test format validation

### Clean Preferences Commands

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

## Configuration Example

For your deployment, you'd tell your friends:

> "Download DD Poker from http://yourserver.com:8080
>
> When you first run it, enter:
> - Game Server: **poker.yourserver.com:8877**
> - Chat Server: **poker.yourserver.com:11886**
>
> Click 'Save & Continue' and you're ready to play!"

Simple and clear! 🎉

## Notes

- Dialog is **modal** - must be addressed before continuing
- Uses existing DD Poker UI components (DDDialog, GlassButton, etc.)
- Consistent with existing app styling
- Saves to same preferences as Options dialog
- No database or external dependencies
- ~200 lines of simple, straightforward code
