# Fix Chat/Keyboard Shortcut Focus Conflict

**Created:** 2026-02-15
**Status:** implemented
**Completed:** 2026-02-15
**Branch:** fix-chat-keyboard-focus-conflict
**Commit:** b8263efa

## Problem Statement

Players typing in chat when it becomes their turn can accidentally trigger game actions (fold, raise, all-in) because:

1. The game steals focus from chat input when transitioning to the player's action
2. Keystrokes intended for the chat message get interpreted as keyboard shortcuts
3. Example: Typing "Red Sox game" → hitting 'R' triggers a Raise action

This is a **serious UX bug** that can ruin games and cause significant frustration.

## Current Behavior Analysis

### Root Causes

1. **Global keyboard listener** - `ShowPokerTable` uses `AWTEventListener` that captures all keyboard events (ShowPokerTable.java:504)

2. **Focus stealing** - `setInputMode()` calls `board_.requestFocus()` in multiple places:
   - Line 1035: `MODE_DEAL` (but only if `!isFocusInChat()`)
   - Line 1140: When hiding amount panel in limit games
   - Line 1202: When disabling amount panel

3. **Inconsistent protection** - `isFocusInChat()` check exists (line 1532-1534) but is only used in `MODE_DEAL`, NOT in betting modes:
   - `MODE_CHECK_BET` (line 1038)
   - `MODE_CHECK_RAISE` (line 1053)
   - `MODE_CALL_RAISE` (line 1068)

4. **Race condition** - Keystrokes in flight when focus transfers are processed as coming from the new focus target (board) instead of the original (chat)

### Affected Keyboard Shortcuts

From ShowTournamentTable.java:1445-1512:
- **D** - Deal
- **F** - Fold
- **C** - Check/Call
- **B** - Bet
- **R** - Raise
- **A** - All-in
- **P** - Pot bet
- **E** - Rebuy

All controlled by `OPTION_DISABLE_SHORTCUTS` preference.

## Proposed Solution

### Two-Phase Approach

**Phase 1: Conservative Focus Management**
- Add `isFocusInChat()` check to all input mode transitions
- Prevent automatic focus stealing when player is typing in chat
- Minimal risk, preserves existing behavior when chat not in use

**Phase 2: Enhanced Visual Feedback**
- Add visual indicator when it's player's turn but board doesn't have focus
- Require explicit click on game area to activate keyboard shortcuts
- Provide clear "waiting for your attention" feedback

### Design Decisions

1. **Sound notification** - Keep existing turn notification sound (if present)

2. **Focus guard** - Don't auto-grab focus from chat during input mode transitions

3. **Activation requirement** - When chat has focus:
   - Show visual indicator (glowing border, overlay message, or status icon)
   - Keyboard shortcuts remain disabled
   - Click anywhere on game board to activate
   - Then keyboard shortcuts work normally

4. **Focus logic** - Smart focus management:
   ```
   IF chat has focus THEN
     - Don't steal focus
     - Show "Click to activate" visual indicator
     - Disable keyboard shortcuts until board gets focus
   ELSE
     - Existing behavior (auto-focus board)
   END IF
   ```

## Implementation Plan

### Phase 1: Conservative Focus Management

#### Step 1.1: Add Focus Guard to Betting Input Modes
**File:** `ShowTournamentTable.java`

- [ ] Modify `setInputMode()` at line 1038 (`MODE_CHECK_BET`)
  - Add focus check before enabling betting controls
  - Pattern: `if (!isFocusInChat()) board_.requestFocus();`

- [ ] Modify `setInputMode()` at line 1053 (`MODE_CHECK_RAISE`)
  - Add same focus guard

- [ ] Modify `setInputMode()` at line 1068 (`MODE_CALL_RAISE`)
  - Add same focus guard

- [ ] Review other `board_.requestFocus()` calls:
  - Line 1140: Already has `amount_.hasFocus()` check, consider adding chat check
  - Line 1202: Already has `amount_.hasFocus()` check, consider adding chat check

**Expected outcome:** Focus never stolen from chat when player is typing

#### Step 1.2: Test Focus Management
- [ ] Manual test: Type in chat when turn begins
  - Verify focus stays in chat
  - Verify keyboard shortcuts don't trigger
  - Verify can click board to activate

- [ ] Test in different game modes:
  - Practice game
  - Online game
  - Tournament
  - With/without keyboard shortcuts enabled

- [ ] Edge cases:
  - Rapid typing during transition
  - Multiple rapid mode changes
  - Chat closed/disabled

**Commit:** "fix: Prevent focus stealing from chat during player's turn"

### Phase 2: Visual Feedback (Optional Enhancement)

#### Step 2.1: Add "Waiting for Focus" Visual State

**Option A: Gameboard Border Highlight**
- [ ] Add boolean field `awaitingPlayerFocus_` to `ShowTournamentTable`
- [ ] Create method `setAwaitingPlayerFocus(boolean)`
- [ ] In `setInputMode()`, set flag when betting mode but chat has focus
- [ ] Override `PokerGameboard.paintComponent()` or use overlay panel
- [ ] Draw glowing yellow/orange border when `awaitingPlayerFocus_ == true`
- [ ] Clear flag when board gains focus

**Option B: Status Message Overlay**
- [ ] Create semi-transparent overlay panel over game board
- [ ] Show "Click here when ready" or "Your turn - click to activate" message
- [ ] Position in center or top of board
- [ ] Remove on mouse click or board focus

**Option C: Keyboard Icon Indicator**
- [ ] Already exists: keyboard icon in client.properties (msg.keyboardfocus)
- [ ] Extend to show red/disabled state when awaiting focus
- [ ] Change to yellow/active when board has focus
- [ ] Add tooltip: "Click game board to enable keyboard shortcuts"

**Decision point:** User preference? Test with Option A first (simplest).

#### Step 2.2: Add Mouse Click Handler
- [ ] Add `MouseListener` to board or overlay
- [ ] On click when `awaitingPlayerFocus_ == true`:
  - Call `board_.requestFocus()`
  - Clear visual indicator
  - Enable keyboard shortcuts

#### Step 2.3: Test Visual Feedback
- [ ] Verify indicator appears when chat has focus and it's player's turn
- [ ] Verify indicator disappears on click
- [ ] Verify indicator disappears if user presses F1 to switch focus
- [ ] Test visibility against different table designs/colors
- [ ] Verify no performance impact (overlay repaints)

**Commit:** "feat: Add visual feedback when awaiting player focus"

### Phase 3: Configuration & Documentation

#### Step 3.1: Add User Preference (Optional)
- [ ] Add option in `PokerConstants`: `OPTION_REQUIRE_FOCUS_CLICK`
- [ ] Add UI checkbox in Options dialog
- [ ] Default: ON (safe behavior)
- [ ] Allow power users to disable if desired

#### Step 3.2: Update Help Text
- [ ] Update keyboard focus help message (msg.keyboardfocus in client.properties)
- [ ] Document the click-to-activate behavior
- [ ] Add to FAQ or tips

**Commit:** "docs: Document click-to-activate keyboard shortcut behavior"

## Files to Modify

### Primary Changes
- `code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java`
  - Modify `setInputMode()` (lines 972-1215)
  - Add visual feedback logic (Phase 2)
  - Add mouse handler (Phase 2)

### Supporting Changes (Phase 2)
- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGameboard.java`
  - Possibly override painting for border highlight
  - Or add overlay component

- `code/poker/src/main/resources/config/poker/client.properties`
  - Update keyboard focus help message
  - Add new option label/help (if adding preference)

- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/PokerConstants.java`
  - Add `OPTION_REQUIRE_FOCUS_CLICK` constant (if adding preference)

### Test Files
- `code/poker/src/test/java/com/donohoedigital/games/poker/ShowTournamentTableFocusTest.java` (new)
  - Test focus management logic
  - Test isFocusInChat() behavior
  - Mock chat panel and board components

## Testing Strategy

### Unit Tests
- [ ] Test `isFocusInChat()` returns correct value
- [ ] Test `setInputMode()` doesn't call `requestFocus()` when chat has focus
- [ ] Test visual indicator state management

### Manual Testing Scenarios

**Scenario 1: Normal Typing in Chat**
1. Start online game with chat visible
2. Begin typing a message in chat
3. Wait for turn to arrive mid-typing
4. Continue typing (should NOT trigger shortcuts)
5. Click game board
6. Type keyboard shortcut (should trigger now)

**Scenario 2: Rapid Mode Transitions**
1. Type in chat
2. Quickly change game modes (deal, bet, raise)
3. Verify focus never stolen
4. Verify no visual glitches

**Scenario 3: F1 Focus Toggle**
1. Press F1 to move focus to board
2. Type 'C' to check/call (should work)
3. Press F1 to move focus to chat
4. Type 'C' in chat message (should NOT call)
5. Press F1 to return to board
6. Type 'R' to raise (should work)

**Scenario 4: Shortcuts Disabled**
1. Disable keyboard shortcuts in options
2. Verify focus behavior still works
3. Verify no shortcuts trigger regardless of focus

**Scenario 5: Chat Closed/Hidden**
1. Close or hide chat panel
2. Verify normal focus behavior (board gets focus)
3. Verify shortcuts work immediately

### Regression Testing
- [ ] Test with keyboard shortcuts enabled
- [ ] Test with keyboard shortcuts disabled
- [ ] Test in practice mode (no chat)
- [ ] Test in online mode (with chat)
- [ ] Test with different game types (limit, no-limit, pot-limit)
- [ ] Test rebuy scenarios

## Risk Assessment

### Low Risk
- **Phase 1 changes**: Simply extends existing `isFocusInChat()` pattern to more modes
- **Existing precedent**: `MODE_DEAL` already uses this pattern (line 1034)
- **Backward compatible**: No behavior change when chat not in use

### Medium Risk
- **Phase 2 visual feedback**: New UI elements could have rendering issues
- **Mitigation**: Make it optional, test against different table designs
- **Fallback**: Phase 1 alone is still a significant improvement

### Testing Coverage Needed
- Multiple platforms (Windows, Mac, Linux)
- Different screen resolutions
- Different Java versions
- Different table designs/colors

## Success Criteria

### Must Have (Phase 1)
- ✅ No keyboard shortcuts trigger when typing in chat
- ✅ Focus remains in chat when turn begins
- ✅ Can manually switch focus with F1 or mouse click
- ✅ No regression in existing keyboard shortcut functionality
- ✅ No regression when chat not in use

### Nice to Have (Phase 2)
- ✅ Clear visual indicator when awaiting focus
- ✅ Indicator visible against all table designs
- ✅ Smooth UX transition on click
- ✅ No performance impact

### Optional (Phase 3)
- ✅ User preference to disable click-to-activate
- ✅ Updated documentation
- ✅ Help text improvements

## Rollback Plan

If issues arise:
1. **Phase 2 problems**: Remove visual feedback, keep Phase 1 fixes
2. **Phase 1 problems**: Revert to original behavior, add option to enable new behavior
3. **Complete rollback**: Revert entire change, file as known issue

## Future Enhancements

Potential improvements for later:
1. **Smart timeout**: After N seconds with no chat activity, auto-activate board focus
2. **Confirmation dialog**: For high-risk actions (all-in), require confirmation even with shortcuts
3. **Keystroke buffer**: Buffer keystrokes during focus transition, replay to correct target
4. **Chat activity detection**: Only guard focus if chat has been used recently
5. **Audio cue**: Different sound for "your turn, waiting for focus" vs "your turn, ready"

## Dependencies

None - this is a standalone UX fix.

## Notes

- User reported this as happening "currently, and it sucks ass" - high priority UX issue
- Affects online multiplayer games most (where chat is heavily used)
- Can cause game-ruining mistakes (accidental all-in)
- Conservative fix (Phase 1) has minimal risk
- Enhanced UX (Phase 2) provides better player experience

## References

- Bug report: User message 2026-02-15
- Related code:
  - `ShowPokerTable.java`: lines 547-615 (event handling)
  - `ShowTournamentTable.java`: lines 972-1215 (setInputMode)
  - `ShowTournamentTable.java`: lines 1417-1530 (keyboard shortcuts)
  - `ChatPanel.java`: lines 437-454 (focus management)
