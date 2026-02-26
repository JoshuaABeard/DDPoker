# Configurable Online Player Limit (Max 120)

## Context

Currently, the maximum online player limit is hardcoded at 90 players (9 tables of 10). This was recently increased from 30 in the `GAME-HOSTING-CONFIG-IMPROVEMENTS` plan. However, it remains a global constant that applies to all tournaments.

**Why this change is needed:**
- Different hosts have different network capacity and use cases
- Some hosts want larger tournaments (100+ players for big family/friend events)
- Others may prefer smaller limits for stability
- Making it configurable allows hosts to choose based on their specific needs

**Target**: Make the online player limit configurable per tournament profile, with a maximum of 120 players.

## Current Architecture

**TournamentProfile.java** (data model):
- Line 75: `MAX_ONLINE_PLAYERS = 90` - hard limit constant
- Line 312-314: `getMaxOnlinePlayers()` - returns `Math.min(getNumPlayers(), MAX_ONLINE_PLAYERS)`
- Line 1191: Used as max for `PARAM_MIN_PLAYERS_START` validation
- Parameters stored in `DMTypedHashMap` with string keys (e.g., `PARAM_NUM_PLAYERS`, `PARAM_MAX_OBSERVERS`)

**TournamentProfileDialog.java** (UI):
- Line 333-338: Observers UI control (pattern to follow)
- Line 451-453: Min players spinner (uses `MAX_ONLINE_PLAYERS` as max)
- Line 558-565: Num players spinner (uses `MAX_ONLINE_PLAYERS` as max for online games)
- Uses `OptionInteger` for numeric spinners with min/max/default/width parameters

**ParameterConstraints.java** (validation):
- Line 128-130: `getMaxOnlinePlayers()` - enforces the cap

**client.properties** (UI labels):
- Line 2619-2624: Pattern for `defplayers` (num players) parameter
- Line 2932-2934: Pattern for `maxobservers` parameter
- Format: `option.<paramname>.label`, `.default`, `.help`, `.left`

## Implementation Plan

### 1. Update TournamentProfile.java

**Add new parameter constant** (after line 106):
```java
public static final String PARAM_MAX_ONLINE_PLAYERS = "maxonlineplayers";
```

**Raise the absolute maximum** (line 75):
```java
public static final int MAX_ONLINE_PLAYERS = 120; // Absolute maximum
```

**Add getter** (after line 314):
```java
/**
 * Get configured maximum online players for this tournament
 */
public int getConfiguredMaxOnlinePlayers() {
    return map_.getInteger(PARAM_MAX_ONLINE_PLAYERS, 90, 2, MAX_ONLINE_PLAYERS);
}
```

**Add setter** (after getter):
```java
/**
 * Set maximum online players for this tournament
 */
public void setMaxOnlinePlayers(int max) {
    map_.setInteger(PARAM_MAX_ONLINE_PLAYERS, max);
}
```

**Update getMaxOnlinePlayers()** (line 312-314):
```java
public int getMaxOnlinePlayers() {
    int configuredMax = getConfiguredMaxOnlinePlayers();
    return Math.min(getNumPlayers(), configuredMax);
}
```

**Update constructor defaults** (around line 210):
Add to initialization in `TournamentProfile(String sName)`:
```java
setMaxOnlinePlayers(60); // Conservative default for local hosting
```

### 2. Update TournamentProfileDialog.java

**Add spinner field and warning label** (after line 83):
```java
private DDNumberSpinner maxOnlinePlayers_;
private DDLabel maxOnlinePlayersWarning_;
```

**Add UI control in OnlineTab.createUILocal()** (after line 331, before observers section):
```java
// Maximum online players
DDLabelBorder maxPlayers = new DDLabelBorder("maxonlineplayers", STYLE);
maxPlayers.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 2, VerticalFlowLayout.LEFT));
left.add(maxPlayers);

int nMin = 2;
int nMax = TournamentProfile.MAX_ONLINE_PLAYERS; // 120
OptionInteger maxOnlineOpt = OptionMenu.add(
    new OptionInteger(null, TournamentProfile.PARAM_MAX_ONLINE_PLAYERS, STYLE,
                      dummy_, null, nMin, nMax, 50, true),
    maxPlayers);
maxOnlineOpt.getSpinner().setUseBigStep(true); // Enable big step (10s)
maxOnlinePlayers_ = maxOnlineOpt.getSpinner();

// Warning label for high player counts (>= 90)
maxOnlinePlayersWarning_ = new DDLabel(GuiManager.DEFAULT, STYLE);
maxOnlinePlayersWarning_.setText(PropertyConfig.getMessage("msg.maxonlineplayers.warning"));
maxOnlinePlayersWarning_.setForeground(new Color(180, 100, 0)); // Orange
maxOnlinePlayersWarning_.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 5));
maxOnlinePlayersWarning_.setVisible(false);
maxPlayers.add(maxOnlinePlayersWarning_);

// Add listener to show/hide warning based on value
maxOnlinePlayers_.addChangeListener(e -> {
    int value = ((Number) maxOnlinePlayers_.getValue()).intValue();
    maxOnlinePlayersWarning_.setVisible(value >= 90);
});
```

**Update minPlayers max constraint** (line 452):
Change from:
```java
null, 2, TournamentProfile.MAX_ONLINE_PLAYERS, 60);
```
To:
```java
null, 2, 120, 60); // Use absolute max instead of constant
```

**Update numPlayers max constraint** (line 558-560):
Change from:
```java
int nMax = (game_ != null && game_.isOnlineGame())
        ? TournamentProfile.MAX_ONLINE_PLAYERS
        : TournamentProfile.MAX_PLAYERS;
```
To:
```java
int nMax = (game_ != null && game_.isOnlineGame())
        ? 120 // Use absolute max
        : TournamentProfile.MAX_PLAYERS;
```

### 3. Add UI Labels to client.properties

**Add after observers section** (after line 2934):
```properties
option.maxonlineplayers.label=   Maximum Online Players
option.maxonlineplayers.default= 60
option.maxonlineplayers.help=    The maximum number of players allowed in this online tournament (2-120). Higher values require better network bandwidth. Default: 60.
msg.maxonlineplayers.warning=    <HTML><B>Warning:</B> High player counts (90+) may require significant network bandwidth when hosting locally. Recommended for dedicated servers.
```

### 4. Update Tests

**TournamentProfileTest.java**:
- Update assertion on line 38 from `assertEquals(90, ...)` to `assertEquals(120, ...)`
- Add test for configurable max online players:
  ```java
  @Test
  public void should_UseConfiguredMaxOnlinePlayers() {
      TournamentProfile profile = new TournamentProfile("Test");
      profile.setMaxOnlinePlayers(100);
      profile.setNumPlayers(150);

      assertEquals(100, profile.getMaxOnlinePlayers());
  }

  @Test
  public void should_DefaultTo60Players() {
      TournamentProfile profile = new TournamentProfile("Test");
      assertEquals(60, profile.getConfiguredMaxOnlinePlayers());
  }

  @Test
  public void should_CapAtConfiguredMax() {
      TournamentProfile profile = new TournamentProfile("Test");
      profile.setMaxOnlinePlayers(50);
      profile.setNumPlayers(100);

      assertEquals(50, profile.getMaxOnlinePlayers());
  }
  ```

**ParameterConstraintsTest.java**:
- Update test on line 146 to expect 120 instead of 90
- Update test on line 135 description to reflect new behavior

### 5. Update Documentation

**CHANGELOG.md** - Add entry:
```markdown
### Changed
- Made online player limit configurable per tournament (2-120 players, default 60)
- Raised absolute maximum from 90 to 120 players to support larger tournaments
- Added warning for high player counts (90+) when configuring tournaments locally
```

## Critical Files

- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileDialog.java`
- `code/poker/src/main/resources/config/poker/client.properties`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/TournamentProfileTest.java`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/ParameterConstraintsTest.java`
- `CHANGELOG.md`

## Verification

1. **Build**: `mvn test` - all tests pass
2. **Manual UI test**:
   - Launch poker client
   - Create new tournament
   - Go to Online tab
   - Verify "Maximum Online Players" control appears with default 60
   - Set to 89, verify no warning shows
   - Set to 90, verify orange warning appears about network bandwidth
   - Set to 120, verify warning still shows
   - Set back to 60, verify warning disappears
   - Set to 50, set num players to 100, verify max is capped at 50
3. **Profile persistence**:
   - Create profile with max=100
   - Save profile
   - Exit and reload
   - Verify max is still 100
4. **Online game**:
   - Create tournament with max=120
   - Start hosting (with bots if needed for testing)
   - Verify game allows up to 120 players (or test max in join validation)

## Notes

- Default of 60 is conservative for typical local P2P hosting
- Warning shown when value >= 90 to alert about network requirements
- Warning only shown in GUI client (when configuring); dedicated servers won't see it
- Hosts can increase to 120 for large events on dedicated servers
- The UI help text mentions network requirements
- ParameterConstraints automatically enforces the configured limit
