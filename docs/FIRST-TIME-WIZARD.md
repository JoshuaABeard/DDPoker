# First-Time User Experience Wizard

**Version**: 3.3.0-community
**Status**: Production Ready
**Test Coverage**: 139 comprehensive tests

---

## Overview

The First-Time User Experience (FTUE) Wizard is a guided setup flow that appears when you launch DD Poker for the first time. It helps you get started quickly by walking you through the initial configuration based on how you want to play.

### Key Benefits

✅ **Streamlined Setup** - Get playing faster with step-by-step guidance
✅ **No Confusion** - Only see options relevant to your chosen play mode
✅ **Smart Defaults** - Skip the wizard if you want; sensible defaults are created
✅ **Fixed UX Issue** - Server configuration now comes BEFORE profile creation (online play)
✅ **Clear Validation** - Real-time feedback on any input errors

---

## How It Works

### When Does the Wizard Appear?

The wizard automatically appears:
- On first launch (no existing player profile)
- When all player profiles have been deleted
- Unless you've previously selected "Don't show again"

### Can I Skip It?

Yes! You can skip the wizard at any time:
- Click the **"Skip Wizard"** button on any screen
- A default offline profile will be created automatically
- You can manually configure settings later

### Will It Appear Again?

- By default, the wizard appears each time until completed
- Check **"Don't show this wizard again"** to prevent future appearances
- You can always create profiles manually from the main menu

---

## Wizard Paths

The wizard offers three different paths based on how you want to play:

### 🎮 Path 1: Practice Offline (Recommended for New Users)

**Best for**: Learning the game, practicing strategies, playing against AI

**Steps**:
1. **Play Mode Selection** - Choose "Practice Offline"
2. **Create Profile** - Enter your player name
3. **Complete!** - Ready to play

**What You Get**:
- Local player profile (not synchronized)
- Full access to single-player features
- Tournament mode, practice mode, AI opponents
- No server connection required

---

### 🌐 Path 2: Play Online (New Account)

**Best for**: New users who want to play online multiplayer

**Steps**:
1. **Play Mode Selection** - Choose "Play Online (New Account)"
2. **Server Configuration** - Enter server address (e.g., `server.ddpoker.com:8877`)
3. **Test Connection** (optional) - Verify server is reachable
4. **Create Profile** - Enter your player name
5. **Email Address** - Provide email for account recovery
6. **Create Password** - Set your account password
7. **Complete!** - Ready for online play

**What You Get**:
- Online player profile synchronized with server
- Access to online multiplayer games
- Online tournaments and leaderboards
- Friend lists and chat features

**Important**: Server configuration happens FIRST, before profile creation. This ensures your profile can connect to the server properly.

---

### 🔗 Path 3: Link Existing Account

**Best for**: Users who already have an online account and are setting up on a new device

**Steps**:
1. **Play Mode Selection** - Choose "Link Existing Account"
2. **Server Configuration** - Enter server address (e.g., `server.ddpoker.com:8877`)
3. **Test Connection** (optional) - Verify server is reachable
4. **Login Credentials** - Enter existing username and password
5. **Complete!** - Profile linked and ready

**What You Get**:
- Access to your existing online profile
- All your stats, achievements, and settings
- Synchronized across devices

---

## Field Validation

The wizard validates your input in real-time to catch errors early:

### Player Name Validation
- ✅ **Required**: Cannot be empty or only whitespace
- ✅ **Flexible**: Any characters accepted (letters, numbers, special characters, emoji)
- ✅ **Trimmed**: Leading/trailing spaces automatically removed
- ❌ **Invalid**: Spaces-only, tabs-only, or newlines-only names rejected

### Email Validation
- ✅ **Format**: Must be valid email format (user@domain.com)
- ✅ **Case**: Accepts uppercase, lowercase, or mixed case
- ✅ **Plus Addressing**: Supports tags (user+tag@domain.com)
- ✅ **Subdomains**: Accepts subdomains (user@mail.example.com)
- ✅ **Trimmed**: Leading/trailing spaces automatically removed
- ❌ **Invalid**: Missing @, missing domain, missing TLD, spaces in middle

### Server Address Validation
- ✅ **Format**: Must be hostname:port or IP:port
- ✅ **Hostname**: Accepts domain names (server.example.com:8877)
- ✅ **IPv4**: Accepts IP addresses (192.168.1.1:8877)
- ✅ **Localhost**: Accepts localhost and 127.0.0.1
- ❌ **Invalid**: Missing port, spaces in address, special characters
- ⚠️ **Note**: IPv6 addresses not currently supported

### Password Validation
- ✅ **Required**: Cannot be empty
- ✅ **Flexible**: Any characters accepted (letters, numbers, special characters)
- ⚠️ **Note**: Whitespace-only passwords currently accepted (may change)

---

## Error Messages

The wizard provides clear, actionable error messages:

| Error Message | Meaning | Fix |
|---------------|---------|-----|
| "Please enter a player name" | Name is empty or whitespace-only | Enter a valid name |
| "Please enter an email address" | Email field is empty | Enter your email |
| "Invalid email format" | Email doesn't match required pattern | Check for typos, ensure user@domain.com format |
| "Please enter server address" | Server field is empty | Enter server address with port |
| "Invalid server address format" | Server doesn't match hostname:port format | Use format like server.com:8877 |
| "Please enter a password" | Password field is empty | Enter your password |

---

## Navigation

### Back Button
- Returns to the previous step
- Your entered data is preserved
- Available on all steps except first step (Play Mode)

### Next Button
- Advances to the next step
- Validates current step before proceeding
- Shows error message if validation fails
- Button is enabled/disabled based on step requirements

### Skip Button
- Available on all steps
- Creates default offline profile
- You can configure settings manually later

### Finish Button
- Appears on final step (Complete screen)
- Closes wizard and launches main application
- Your profile is created and activated

---

## Preferences

The wizard respects and saves several preferences:

### Wizard Completed Flag
- **Location**: `com.donohoedigital.poker.ftue.wizard_completed`
- **Purpose**: Tracks if wizard has been completed
- **Reset**: Delete preference to see wizard again

### Don't Show Again Flag
- **Location**: `com.donohoedigital.poker.ftue.dont_show_again`
- **Purpose**: Prevents wizard from appearing on future launches
- **Reset**: Delete preference or uncheck in settings

### Where Are Preferences Stored?

Preferences are stored in platform-specific JSON configuration files:
- **Windows**: `%APPDATA%\ddpoker\config.json`
- **macOS**: `~/Library/Application Support/ddpoker/config.json`
- **Linux**: `~/.ddpoker/config.json`

---

## Server Configuration

### Default Server
The default server address is configured in `client.properties`:
```
msg.ftue.server.default=poker.donohoedigital.com:8877
```

### Testing Server Connection
The "Test Connection" button (optional):
- Attempts to connect to the specified server
- Shows success/failure message
- Connection test failure doesn't block wizard progression
- You can proceed even if test fails (useful if server is temporarily down)

### Custom Server
You can use any server address:
- Official servers: `poker.donohoedigital.com:8877`
- LAN server: `192.168.1.100:8877`
- Localhost (development): `localhost:8877` or `127.0.0.1:8877`

---

## Troubleshooting

### Wizard Doesn't Appear

**Possible Causes**:
1. Player profile already exists
2. "Don't show again" was previously selected
3. Wizard is disabled in configuration

**Solutions**:
- Delete existing profiles to trigger wizard
- Edit config.json and remove `ftue.dont_show_again` preference
- Check `client.properties` for wizard disable flag

### Server Connection Fails

**Possible Causes**:
1. Server is offline or unreachable
2. Firewall blocking connection
3. Incorrect server address or port

**Solutions**:
- Verify server address is correct (hostname:port)
- Check your internet connection
- Try alternative server address
- You can proceed even if connection test fails

### Email Validation Fails

**Possible Causes**:
1. Email format is invalid
2. Typo in email address
3. Missing @ symbol, domain, or TLD

**Solutions**:
- Check email follows format: user@domain.com
- Remove any spaces in the email
- Ensure domain has valid TLD (.com, .org, etc.)

### Can't Click Next Button

**Possible Causes**:
1. Validation failed on current step
2. Required field is empty
3. Server configuration not complete

**Solutions**:
- Check for error message displayed in red
- Fill in all required fields
- For server step: Test connection or mark as complete

---

## Manual Profile Creation

If you skip the wizard or want to create additional profiles:

1. Launch DD Poker
2. Click **"Options"** from main menu
3. Select **"Player Profiles"**
4. Click **"New Profile"**
5. Follow profile creation dialog

---

## Technical Details

### Implementation
- **Class**: `com.donohoedigital.games.poker.FirstTimeWizard`
- **Type**: DialogPhase (modal dialog)
- **Framework**: DDPoker game engine dialog system
- **Validation**: Real-time regex-based validation
- **State Management**: Step-based state machine

### Test Coverage
- **Unit Tests**: 29 tests (core logic and validation)
- **Integration Tests**: 19 tests (end-to-end flows)
- **Edge Case Tests**: 49 tests (boundary conditions)
- **Advanced Edge Cases**: 42 tests (complex scenarios)
- **Total**: 139 tests with 100% pass rate

### Configuration Files
- **gamedef.xml**: Phase definition and dialog parameters
- **client.properties**: Localized strings (50+ messages)
- **Preferences**: Wizard state stored in JSON config

### Validation Patterns

**Email Regex**:
```
^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$
```

**Server Address Regex**:
```
^[a-zA-Z0-9.-]+:[0-9]{1,5}$
```

---

## Frequently Asked Questions

### Q: Do I have to complete the wizard?
**A**: No, you can skip it anytime. A default offline profile will be created.

### Q: Can I change my choice later?
**A**: Yes! You can create additional profiles or change settings from the Options menu.

### Q: What if I choose the wrong play mode?
**A**: You can go back using the Back button, or skip and create the correct profile type manually.

### Q: Does offline mode limit my features?
**A**: Offline mode has full access to single-player features. You can create an online profile later for multiplayer.

### Q: Can I have both offline and online profiles?
**A**: Yes! You can create multiple profiles of different types from the Options menu.

### Q: What if my server address changes?
**A**: Edit your profile settings to update the server address at any time.

### Q: Is my password stored securely?
**A**: Passwords are hashed before transmission to the server. Never stored in plain text.

### Q: Can I export my profile?
**A**: Yes! Profile data is stored in portable JSON format in your config directory.

---

## Related Documentation

- [File-Based Configuration](FILE-BASED-CONFIGURATION.md) - How settings are stored
- [License Removal Technical](LICENSE-REMOVAL-TECHNICAL.md) - Player identity system
- [CHANGELOG](../CHANGELOG.md) - Release notes and version history

---

## Support

For issues or questions about the First-Time Wizard:

- **GitHub Issues**: https://github.com/donohoedigital/DDPoker/issues
- **Documentation**: https://github.com/donohoedigital/DDPoker/tree/main/docs

---

**Last Updated**: 2026-02-09
**Version**: 3.3.0-community
**Status**: Production Ready
