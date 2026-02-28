# Web Client Feature Parity Round 4 — Design

**Status:** APPROVED (2026-02-27)
**Depends on:** None (all features are additive)
**Scope:** 6 features across practice game UX, tournament display, and gameplay polish

---

## Context

Rounds 1-3 covered core gameplay, audio, theming, animations, and game creation. This round closes remaining client-side gaps: practice game option controls, missing overlay wiring, color up display, side pot details, chip count animations, and game info enhancements.

## Feature 1: Practice Game Options UI

### Problem
`PracticeConfigDto` has fields (`aiFaceUp`, `pauseAllinInteractive`, `autoDeal`, `zipModeEnabled`) that are sent to the server but have no toggles in the game creation form for practice games.

### Design

Add a "Practice Options" section to `code/web/app/games/create/page.tsx`, visible only when `isPractice` is true. Four checkbox toggles:

| Option | Field | Default | Description |
|--------|-------|---------|-------------|
| Show AI Cards | `aiFaceUp` | false | Reveal AI hole cards face-up |
| Pause on All-In | `pauseAllinInteractive` | false | Pause before dealing all-in runout |
| Auto Deal | `autoDeal` | true | Auto-advance to next hand |
| Fast Mode | `zipModeEnabled` | false | Skip animations and delays |

These fields already exist in the practice config sent to the server — we just need UI controls.

### Files
- Modify: `code/web/app/games/create/page.tsx` — add practice options section

---

## Feature 2: Wire Never-Broke & Continue-Runout Overlays

### Problem
`GameOverlay.tsx` already has `neverBroke` and `continueRunout` overlay types. `GameContext.tsx` already has `sendNeverBrokeDecision` and `sendContinueRunout` actions. The game reducer already tracks `neverBrokeOffer` and `continueRunoutPending` state. But the play page (`app/games/[gameId]/play/page.tsx`) never renders these overlays.

### Design

In `code/web/app/games/[gameId]/play/page.tsx`, add two more overlay conditions to the existing priority chain (after addon, before eliminated):

```typescript
} else if (neverBrokeOffer) {
  overlay = (
    <GameOverlay
      type="neverBroke"
      timeoutSeconds={neverBrokeOffer.timeoutSeconds}
      onDecision={actions.sendNeverBrokeDecision}
    />
  )
} else if (continueRunoutPending) {
  overlay = (
    <GameOverlay
      type="continueRunout"
      onContinue={actions.sendContinueRunout}
    />
  )
}
```

Also need to destructure `neverBrokeOffer` and `continueRunoutPending` from game state.

### Files
- Modify: `code/web/app/games/[gameId]/play/page.tsx` — add overlay wiring (~10 lines)

---

## Feature 3: Color Up Display

### Problem
When a chip race (color up) occurs in a tournament, the server sends `COLOR_UP_STARTED` with detailed player data and `COLOR_UP_COMPLETED`. The reducer sets `colorUpInProgress: boolean` but no UI is shown. Players see nothing during the color up sequence.

### Design

**ColorUpOverlay component** shown when `colorUpInProgress` is true:

- Semi-transparent modal overlay (same style as GameOverlay)
- Title: "Chip Race"
- Subtitle: "Exchanging chips to new minimum: {formatChips(newMinChip)}"
- Table showing each player's outcome:
  - Player name
  - Cards drawn (rendered with Card component at small size)
  - Result: "Won" (green), "Broke" (red), or "—"
  - Final chip count
- Auto-dismiss when `COLOR_UP_COMPLETED` received (colorUpInProgress becomes false)

**State changes:** Expand the reducer to store color up data (not just boolean):

```typescript
colorUpData: ColorUpStartedData | null  // replaces colorUpInProgress: boolean
```

On `COLOR_UP_STARTED`: set `colorUpData` to the message data.
On `COLOR_UP_COMPLETED`: set `colorUpData` to null.

### Files
- New: `code/web/components/game/ColorUpOverlay.tsx` (~60 lines)
- Modify: `code/web/lib/game/gameReducer.ts` — store full ColorUpStartedData instead of boolean
- Modify: `code/web/app/games/[gameId]/play/page.tsx` — render ColorUpOverlay

---

## Feature 4: Enhanced Side Pot Display

### Problem
PotDisplay shows side pot amounts but doesn't indicate how many players are eligible for each pot. Players can't tell which pots they can win.

### Design

Enhance `PotDisplay.tsx`:
- Main pot: "Pot: 1,500 (4 players)"
- Side pots: "Side Pot 1: 800 (2 players)"
- On hover/click, show a tooltip listing eligible player names (requires passing seat data to PotDisplay so it can map player IDs to names)

**PotDisplay props change:**
```typescript
interface PotDisplayProps {
  pots: PotData[]
  seats?: SeatData[]  // optional, for player name lookup
}
```

When `seats` is provided and a pot has `eligiblePlayers`, show count in parentheses. On hover, show a small tooltip with player names.

### Files
- Modify: `code/web/components/game/PotDisplay.tsx` — add player count and tooltip
- Modify: `code/web/components/game/TableFelt.tsx` — pass seats to PotDisplay

---

## Feature 5: Chip Count Animations

### Problem
When a player wins a pot or chips are transferred, the chip count on PlayerSeat updates instantly. This makes it hard to notice chip changes.

### Design

Use Framer Motion's `animate` on the chip count display in PlayerSeat:

- When `chipCount` changes, animate the number with a brief color flash:
  - Increase: flash green for 1 second
  - Decrease: flash red for 1 second
- Use `useRef` to track previous chip count and detect direction of change
- The number itself updates immediately (no counting animation — just a color highlight)

This is a subtle visual cue, not a complex flying-chip animation. Keep it simple.

### Files
- Modify: `code/web/components/game/PlayerSeat.tsx` — add chip change highlight

---

## Feature 6: Enhanced Game Info Panel

### Problem
GameInfoPanel shows blinds, blind structure, and player chip counts. It doesn't show greeting message, rebuy/addon availability, or minimum chip value — information the desktop client displays.

### Design

Add three optional sections to GameInfoPanel:

1. **Greeting message**: If `greeting` prop is provided, show it below the header as italic gray text
2. **Tournament details row**: Show rebuy/addon status ("Rebuys: Allowed until Level 4", "Add-ons: Available at break") if those config fields are provided
3. **Minimum chip**: Show "Min Chip: {value}" below the current blinds when provided

**New optional props:**
```typescript
greeting?: string
minChip?: number
rebuyInfo?: string   // pre-formatted by parent: "Allowed until Level 4"
addonInfo?: string   // pre-formatted by parent: "Available at break"
```

### Files
- Modify: `code/web/components/game/GameInfoPanel.tsx` — add greeting, tournament details, min chip
- Modify: `code/web/components/game/PokerTable.tsx` — pass new props to GameInfoPanel

---

## Architecture Notes

- No new npm dependencies
- No server changes needed — all features use existing state fields and message types
- Color up is the only feature requiring a reducer state shape change (boolean → object)
- Practice options just wire existing DTO fields to UI controls
- Never-broke and continue-runout are literally 10 lines of wiring
- Side pot enhancement is purely visual (data already available)
- Chip animation uses Framer Motion (already installed)
- Testing: each feature gets unit tests following existing Vitest + React Testing Library patterns
