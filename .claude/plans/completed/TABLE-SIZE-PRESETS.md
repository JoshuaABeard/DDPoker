# Table Size Presets

**Status:** Not Started
**Priority:** P4 (Nice to Have)
**Effort:** Low
**Plan reference:** `GAME-HOSTING-CONFIG-IMPROVEMENTS.md` item #17

## Context

Tournament table size is currently configured via a numeric spinner (2-10) in the Details tab of the tournament profile dialog. This requires users to know the standard poker format seat counts. Adding presets (Full Ring / 6-Max / Heads-Up) makes format selection immediate and intuitive, matching how every modern poker platform presents this choice.

## Approach

Replace the `OptionInteger` spinner with 3 `OptionRadio` buttons writing directly to `PARAM_TABLE_SEATS`. This follows the exact same pattern used for late registration chip mode and payout allocation type.

**Presets:**
- Full Ring (10) — current default
- 6-Max (6)
- Heads-Up (2)

**Tradeoff:** Loses ability to set non-standard sizes (3, 4, 5, 7, 8, 9). These are uncommon formats. No new parameters needed — radios write directly to the existing `PARAM_TABLE_SEATS`.

## Changes

### 1. `PokerConstants.java` — Add format constants

Add 3 constants next to the existing `SEATS = 10`:

```java
public static final int SEATS_FULL_RING = 10;
public static final int SEATS_6MAX = 6;
public static final int SEATS_HEADS_UP = 2;
```

### 2. `TournamentProfileDialog.java` — Replace spinner with radios

In `DetailsTab.createUILocal()` (lines 459-461), replace:
```java
oi = OptionMenu.add(new OptionInteger(null, TournamentProfile.PARAM_TABLE_SEATS, STYLE, dummy_, null, 2,
        PokerConstants.SEATS, 40, true), quantity, BorderLayout.CENTER);
```

With a radio button group following the late-reg chip mode pattern (lines 350-362):
```java
ButtonGroup tableFormatGroup = new ButtonGroup();
DDPanel tableFormatPanel = new DDPanel();
tableFormatPanel.setLayout(new GridLayout(3, 1, 0, -4));

OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_TABLE_SEATS, STYLE, dummy_,
        "tableformat.fullring", tableFormatGroup, PokerConstants.SEATS_FULL_RING), tableFormatPanel);
OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_TABLE_SEATS, STYLE, dummy_,
        "tableformat.6max", tableFormatGroup, PokerConstants.SEATS_6MAX), tableFormatPanel);
OptionMenu.add(new OptionRadio(null, TournamentProfile.PARAM_TABLE_SEATS, STYLE, dummy_,
        "tableformat.headsup", tableFormatGroup, PokerConstants.SEATS_HEADS_UP), tableFormatPanel);
```

Add the panel to the quantity section using `BorderLayout.CENTER` (same slot the spinner used).

### 3. `client.properties` — Add radio labels and update help

Add near the existing `option.tableseats.*` block (line ~2619):
```properties
option.tableformat.fullring=        Full Ring (10)
option.tableformat.6max=            6-Max (6)
option.tableformat.headsup=         Heads-Up (2)
```

Update `option.tableseats.help` to reflect the new format selection.

### 4. `TournamentProfileHtml.java` — Show format name in summary

In `toHTMLSummary()` and `toHTML()`, enhance the seats display to show the format name (e.g., "6-Max" instead of just "6"). Add a helper method:
```java
private static String getTableFormatName(int seats) {
    return switch (seats) {
        case PokerConstants.SEATS_FULL_RING -> "Full Ring (10)";
        case PokerConstants.SEATS_6MAX -> "6-Max (6)";
        case PokerConstants.SEATS_HEADS_UP -> "Heads-Up (2)";
        default -> String.valueOf(seats);
    };
}
```

### 5. Update plan file

Mark item #17 as completed in `GAME-HOSTING-CONFIG-IMPROVEMENTS.md`.

## Files Modified

| File | Change |
|------|--------|
| `code/pokerengine/.../engine/PokerConstants.java` | Add 3 seat constants |
| `code/poker/.../TournamentProfileDialog.java` | Replace spinner with radios |
| `code/poker/src/main/resources/config/poker/client.properties` | Add radio labels |
| `code/pokerengine/.../engine/TournamentProfileHtml.java` | Show format name |
| `.claude/plans/GAME-HOSTING-CONFIG-IMPROVEMENTS.md` | Mark #17 done |

## Verification

1. `mvn test -pl pokerengine,poker -P dev` — all tests pass
2. Manual: Open tournament profile dialog → Details tab → verify 3 radio buttons appear
3. Manual: Select each format → save → reopen → verify selection persists
4. Manual: Check tournament summary HTML shows format name
5. Existing profiles with value=10 should auto-select "Full Ring"
