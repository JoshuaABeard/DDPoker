# Plan: Require Password Validation When Switching to Online Profile

## Context

Online profiles (those with a non-null email, `PlayerProfile.isOnline() == true`) are
intended exclusively for WAN/community servers. When a user switches to an online profile,
they must prove they own it by entering the profile password, validated against the
configured WAN server. Without a WAN server configured, online profiles cannot be used
OR created.

## Requirements

1. **Profile selection — online, no WAN server**: Show an error, keep the previous profile.
2. **Profile selection — online, WAN server**: Show password prompt → `RestAuthClient.login()` → if fails, show error and keep previous profile.
3. **Profile selection — offline**: No dialog; switch proceeds as today.
4. **Profile creation (dialog)**: Disable the "New Online" and "Link Existing" radio buttons in `PlayerProfileDialog` if no WAN server is configured.

## Architecture

### Part A — Profile Selection: Hook in `ProfileList`

`ProfileList.profileSelected()` is **private**, so it cannot be overridden. The cleanest
solution is to add a protected `onProfileSelecting()` hook called at the very start of
`profileSelected()` before any state changes. Returning `false` cancels the switch with
no side effects.

### Part B — Profile Creation: Disable Online Radios in `PlayerProfileDialog`

`PlayerProfileDialog` already has `getServerUrl()` to look up the WAN server. When a
new (non-online) profile is being created, simply disable `newRadio_` and `existRadio_`
(and add explanatory tooltip text) when no WAN server is configured.

### WAN Server Check Logic

A WAN server is "configured" when BOTH:
- `EngineConstants.OPTION_ONLINE_ENABLED` pref is `true` (global online toggle)
- `EngineConstants.OPTION_ONLINE_SERVER` pref is non-blank (server URL set)

Helper (same pattern used by `ChangePasswordDialog.getServerUrl()` in the same class):
```java
private String getWanServerUrl() {
    String node = Prefs.NODE_OPTIONS + engine_.getPrefsNodeName();
    boolean enabled = Prefs.getUserPrefs(node).getBoolean(
        EngineConstants.OPTION_ONLINE_ENABLED, false);
    if (!enabled) return null;
    String server = Prefs.getUserPrefs(node).get(
        EngineConstants.OPTION_ONLINE_SERVER, "");
    return server.isBlank() ? null : "http://" + server;
}
```

## Files to Modify

| File | Change |
|------|--------|
| `code/gameengine/src/main/java/com/donohoedigital/games/engine/ProfileList.java` | Add `protected boolean onProfileSelecting(BaseProfile newProfile, BaseProfile currentProfile)` hook; call at start of `profileSelected()` |
| `code/poker/src/main/java/com/donohoedigital/games/poker/PlayerProfileOptions.java` | Override `onProfileSelecting()` in `PlayerProfileList`; add `getWanServerUrl()` helper |
| `code/poker/src/main/java/com/donohoedigital/games/poker/PlayerProfileDialog.java` | Disable `newRadio_` and `existRadio_` when no WAN server configured |
| `code/poker/src/main/resources/config/poker/client.properties` | Add i18n strings |

## Implementation Details

### 1. `ProfileList.java` — add hook (line 411)

```java
private void profileSelected(ProfilePanel pp) {
    BaseProfile newProfile = pp != null ? pp.profile_ : null;
    if (!onProfileSelecting(newProfile, selected_)) {
        return; // cancelled by subclass
    }
    // ... existing code unchanged ...
}

/**
 * Called before switching to a new profile. Return {@code false} to abort.
 * Default implementation returns {@code true} (always allow).
 */
protected boolean onProfileSelecting(BaseProfile newProfile, BaseProfile currentProfile) {
    return true;
}
```

### 2. `PlayerProfileOptions.PlayerProfileList` — override hook

Add `onProfileSelecting()` and `getWanServerUrl()` to the existing
`private static class PlayerProfileList`:

```java
@Override
protected boolean onProfileSelecting(BaseProfile newProfile, BaseProfile currentProfile) {
    PlayerProfile pp = (PlayerProfile) newProfile;
    if (pp == null || !pp.isOnline()) {
        return true; // offline or clearing selection: always allow
    }

    // Online profile: require a configured WAN server
    String serverUrl = getWanServerUrl();
    if (serverUrl == null) {
        JOptionPane.showMessageDialog(
            SwingUtilities.getWindowAncestor(this),
            PropertyConfig.getMessage("msg.profile.online.nowanserver"),
            PropertyConfig.getMessage("msg.profile.online.title"),
            JOptionPane.WARNING_MESSAGE);
        return false;
    }

    // Show password prompt
    JPasswordField pwField = new JPasswordField(20);
    int choice = JOptionPane.showConfirmDialog(
        SwingUtilities.getWindowAncestor(this),
        new Object[] {
            PropertyConfig.getMessage("msg.profile.online.passwordprompt", pp.getName()),
            pwField
        },
        PropertyConfig.getMessage("msg.profile.online.title"),
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);

    if (choice != JOptionPane.OK_OPTION) {
        return false; // user cancelled
    }

    try {
        RestAuthClient.getInstance().login(
            serverUrl, pp.getName(), new String(pwField.getPassword()));
        return true;
    } catch (RestAuthClient.RestAuthException e) {
        JOptionPane.showMessageDialog(
            SwingUtilities.getWindowAncestor(this),
            e.getMessage(),
            PropertyConfig.getMessage("msg.profile.online.title"),
            JOptionPane.ERROR_MESSAGE);
        return false;
    }
}

private String getWanServerUrl() {
    String node = Prefs.NODE_OPTIONS + engine_.getPrefsNodeName();
    boolean enabled = Prefs.getUserPrefs(node).getBoolean(
        EngineConstants.OPTION_ONLINE_ENABLED, false);
    if (!enabled) return null;
    String server = Prefs.getUserPrefs(node).get(
        EngineConstants.OPTION_ONLINE_SERVER, "");
    return server.isBlank() ? null : "http://" + server;
}
```

New imports for `PlayerProfileOptions.java`:
- `javax.swing.JOptionPane`
- `javax.swing.JPasswordField`
- `javax.swing.SwingUtilities`
- `com.donohoedigital.games.poker.online.RestAuthClient`
- `com.donohoedigital.config.Prefs`
- `com.donohoedigital.games.engine.EngineConstants`

### 3. `PlayerProfileDialog.java` — disable online radios without WAN server

In the existing block that creates `noRadio_` / `newRadio_` / `existRadio_` for
non-online profiles, add a WAN server check:

```java
noRadio_ = onlinePanel_.addRadio("profilenoonline");
newRadio_ = onlinePanel_.addRadio("profilenewonline");
existRadio_ = onlinePanel_.addRadio("profileexistonline");
noRadio_.setSelected(true);

// Disable online options if no WAN server is configured
if (getServerUrl().isBlank() || isServerUrlEmpty()) {
    String tip = PropertyConfig.getMessage("msg.profile.online.nowanserver.tip");
    newRadio_.setEnabled(false);
    newRadio_.setToolTipText(tip);
    existRadio_.setEnabled(false);
    existRadio_.setToolTipText(tip);
}
```

`PlayerProfileDialog` already has a `getServerUrl()` method. Add a helper:
```java
private boolean hasWanServer() {
    String node = Prefs.NODE_OPTIONS + PokerMain.getPokerMain().getPrefsNodeName();
    boolean enabled = Prefs.getUserPrefs(node).getBoolean(
        EngineConstants.OPTION_ONLINE_ENABLED, false);
    if (!enabled) return false;
    String server = Prefs.getUserPrefs(node).get(
        EngineConstants.OPTION_ONLINE_SERVER, "");
    return !server.isBlank();
}
```

Then use `if (!hasWanServer())` for the disable block.

### 4. `client.properties` — new i18n strings

```
msg.profile.online.title=                       Online Profile
msg.profile.online.nowanserver=                 Online profiles require an online server.\nGo to Options > Online Settings to configure a server.
msg.profile.online.nowanserver.tip=             Configure an online server in Options to use online profiles
msg.profile.online.passwordprompt=              Enter password for {0}:
```

## Key Reused Code

- `PlayerProfile.isOnline()` — `getEmail() != null`
- `RestAuthClient.getInstance().login(url, username, password)` — throws `RestAuthException` with user-facing message on failure
- `getServerUrl()` already in `PlayerProfileDialog` — same pref-reading pattern to reuse in `hasWanServer()`
- `EngineConstants.OPTION_ONLINE_ENABLED` / `OPTION_ONLINE_SERVER` — existing pref keys
- `Prefs.NODE_OPTIONS + engine_.getPrefsNodeName()` — existing pattern for pref node lookup
- `SwingUtilities.getWindowAncestor(this)` — get parent window from DDPanel subclass

## Verification

1. **Offline profile switch**: Clicks offline profile → switches immediately, no dialog.
2. **Online profile, no WAN server**: Select online profile → warning shown → previous profile stays active (check `PlayerProfileOptions.getDefaultProfile()` returns previous).
3. **Online profile, WAN server, wrong password**: Enter wrong password → server error shown → previous profile stays.
4. **Online profile, WAN server, correct password**: Switch succeeds; `preAuthenticateProfile()` fires for embedded server.
5. **Cancel password dialog**: Previous profile stays.
6. **Profile creation dialog, no WAN server**: "New Online" and "Link Existing" radios are disabled with tooltip; "Keep Local" is the only enabled option.
7. **Profile creation dialog, WAN server configured**: All three radios enabled as today.
8. **Build**: `mvn test -P dev` passes (excluding known flaky `interHandPausePreventsRacing`).
