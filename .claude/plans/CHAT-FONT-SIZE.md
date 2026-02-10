# Plan: Chat Font Size Setting

## Context

Chat text in DD Poker uses a fixed font size (12pt) defined in the style system. Users have no way to adjust this, which can be problematic for readability on different screen sizes or for accessibility. This plan adds a user-configurable chat font size setting that applies to all chat contexts (in-game and lobby).

## How Chat Font Rendering Works

1. `ChatItemPanel` (inner class of `ChatListPanel`) creates a `DDHtmlArea` for each message
2. The first `DDHtmlArea` created becomes the "style prototype" - its `StyleSheet` is shared with all subsequent instances
3. `DDHtmlArea.setStyles()` bakes the component's `getFont().getSize()` into a CSS `body` rule
4. `setStyles()` only runs on the prototype instance (when `proto == null`); subsequent instances share the stylesheet

This means we need to override the font on the prototype `DDHtmlArea` and refresh its CSS for the size to take effect.

## Implementation

### 1. Add preference constant

**File**: [PokerConstants.java](code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/PokerConstants.java) (~line 169)

Add after existing `OPTION_CHAT_*` constants:
```java
public static final String OPTION_CHAT_FONT_SIZE = "chatfontsize";
public static final int DEFAULT_CHAT_FONT_SIZE = 12;
```

### 2. Add option properties

**File**: [client.properties](code/poker/src/main/resources/config/poker/client.properties) (near other `chatXxx` option entries)

```properties
option.chatfontsize.label=          Chat Font Size
option.chatfontsize.default=        12
option.chatfontsize.help=           Font size for chat messages (8-24)
```

### 3. Expose `setStyles()` on `DDHtmlArea`

**File**: [DDHtmlArea.java](code/gui/src/main/java/com/donohoedigital/gui/DDHtmlArea.java) (~line 169)

Add a public method that delegates to the existing private `setStyles()`:
```java
public void refreshStyles() {
    setStyles();
}
```

### 4. Apply font size in `ChatItemPanel` constructor

**File**: [ChatListPanel.java](code/poker/src/main/java/com/donohoedigital/games/poker/online/ChatListPanel.java) (~line 585)

After `DDHtmlArea` is created, derive a font with the user's preferred size. When this is the prototype instance, call `refreshStyles()` to re-bake the CSS with the new size:

```java
html_ = new DDHtmlArea(GuiManager.DEFAULT, sStyle, null, panel.styleproto_);
// ... existing setup ...

// Apply user-configured chat font size
int fontSize = PokerUtils.getIntPref(PokerConstants.OPTION_CHAT_FONT_SIZE,
                                      PokerConstants.DEFAULT_CHAT_FONT_SIZE);
Font currentFont = html_.getFont();
if (currentFont.getSize() != fontSize) {
    html_.setFont(currentFont.deriveFont((float) fontSize));
}

if (panel.styleproto_ == null) {
    if (currentFont.getSize() != fontSize) {
        html_.refreshStyles(); // Re-bake CSS with new font size
    }
    panel.styleproto_ = html_;
}
```

### 5. Add `resetStyleProto()` to `ChatListPanel`

**File**: [ChatListPanel.java](code/poker/src/main/java/com/donohoedigital/games/poker/online/ChatListPanel.java)

```java
void resetStyleProto() {
    styleproto_ = null;
}
```

### 6. Update `ChatPanel.updatePrefs()` to reset font prototype

**File**: [ChatPanel.java](code/poker/src/main/java/com/donohoedigital/games/poker/online/ChatPanel.java) (~line 366)

Reset style prototypes so new messages pick up the updated font:
```java
public void updatePrefs() {
    for (ChatListPanel chatList : chatList_) {
        if (chatList != null) chatList.resetStyleProto();
    }
    createDisplay(true);
}
```

### 7. Add UI control to preferences dialog

**File**: [GamePrefsPanel.java](code/poker/src/main/java/com/donohoedigital/games/poker/GamePrefsPanel.java) (~line 575)

Add an `OptionInteger` spinner to the "Chat Options" section (`detailbase`) in the Online tab, after the existing chat checkboxes:

```java
OptionMenu.add(new OptionInteger(NODE, PokerConstants.OPTION_CHAT_FONT_SIZE,
    OSTYLE, map_, null, 8, 24, 55), detailbase);
```

## Files Modified (6 files)

| File | Change |
|------|--------|
| `PokerConstants.java` | Add `OPTION_CHAT_FONT_SIZE` + `DEFAULT_CHAT_FONT_SIZE` constants |
| `client.properties` | Add label, default, help for `chatfontsize` option |
| `DDHtmlArea.java` | Add `refreshStyles()` public method (3 lines) |
| `ChatListPanel.java` | Override font in `ChatItemPanel` constructor; add `resetStyleProto()` |
| `ChatPanel.java` | Reset style prototypes in `updatePrefs()` |
| `GamePrefsPanel.java` | Add `OptionInteger` spinner in Online tab's Chat Options section |

## Behavior Notes

- New messages immediately use the configured font size
- Setting persists across restarts (stored in `config.json` via `FilePrefs`)
- Lobby chat picks up the setting when the lobby is opened (panel is created fresh each time)
- Default is 12pt (matches current behavior), range 8-24

## Verification

1. Build: `mvn compile` from `code/` directory
2. Open Options > Online tab - verify "Chat Font Size" spinner appears in Chat Options with default 12
3. Change to 18, click OK - verify new chat messages render larger
4. Change to 10 - verify messages render smaller
5. Restart application - verify setting persists
6. Test in lobby chat - verify font size applies there too
