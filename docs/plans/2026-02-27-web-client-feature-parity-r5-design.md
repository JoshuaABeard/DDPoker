# Web Client Feature Parity Round 5 — Design

**Status:** APPROVED (2026-02-27)
**Depends on:** None (all features are client-only)
**Scope:** 8 components across game preferences, shortcuts, AI config, poker math, starting hands, simulator, advisor, and dashboard

---

## Context

Rounds 1-4 covered core gameplay, audio, theming, animations, game creation, overlays, and display polish. This round adds the remaining client-only desktop features: game preferences, enhanced keyboard shortcuts, AI player configuration, a TypeScript poker math library, starting hands chart, poker simulator, AI advisor panel, and a configurable dashboard widget system.

## Feature 1: Game Preferences

### Problem
The desktop client has extensive preference toggles (four-color deck, check-fold, disable shortcuts, dealer chat filtering). The web client only has theme/card back/avatar preferences.

### Design

Add a `useGamePrefs` hook following the existing localStorage pattern (same as useTheme/useCardBack). Store all gameplay preferences in a single localStorage key.

**Preferences:**

| Pref | Key | Default | Effect |
|------|-----|---------|--------|
| Four-color deck | `fourColorDeck` | false | Use blue diamonds, green clubs (separate PNG set) |
| Check-fold | `checkFold` | false | Show pre-action check/fold button |
| Disable shortcuts | `disableShortcuts` | false | Disable all keyboard shortcuts |
| Dealer chat | `dealerChat` | `'all'` | `'all'` / `'actions'` / `'none'` |

**Four-color deck implementation:** Generate a second set of 26 card PNGs (13 diamonds recolored blue, 13 clubs recolored green) using an image processing script. Card.tsx selects the appropriate image set based on the `fourColorDeck` preference.

Add a "Gameplay" tab to ThemePicker for these toggles.

### Files
- New: `code/web/lib/game/useGamePrefs.ts`
- New: `code/web/public/images/cards/4color/` (26 PNGs)
- Modify: `code/web/components/game/ThemePicker.tsx` — add Gameplay tab
- Modify: `code/web/components/game/Card.tsx` — four-color card selection
- Modify: `code/web/components/game/ChatPanel.tsx` — dealer chat filtering

---

## Feature 2: Keyboard Shortcuts Enhancement

### Problem
The web client has basic shortcuts (F/C/R/A/Enter/Escape/H/I). The desktop adds check-fold pre-action queuing, an N key for advancing, and a disable-shortcuts toggle.

### Design

**New shortcuts:**
- **N key** — When in DEAL or CONTINUE input mode, advance to next hand (calls `sendAction('DEAL')` or `sendAction('CONTINUE')`)
- **Check-fold queuing** — When `checkFold` pref is on and it's not your turn, pressing F queues a fold for when your turn comes. Visual indicator shows queued action.

**Disable shortcuts:** When `disableShortcuts` pref is true, all keyboard handlers in ActionPanel and PokerTable skip processing.

### Files
- Modify: `code/web/components/game/ActionPanel.tsx` — add N key, check-fold queue, respect disableShortcuts
- Modify: `code/web/components/game/PokerTable.tsx` — respect disableShortcuts for H/I keys

---

## Feature 3: AI Player Type Configuration

### Problem
The game creation form shows AI players as bare name + skill level (1-10 number). The desktop has named skill presets and play style options.

### Design

Replace the numeric skill slider with named presets and add a play style dropdown:

**Skill presets:**
- Novice (skillLevel: 1-2)
- Beginner (skillLevel: 3-4)
- Intermediate (skillLevel: 5-6)
- Advanced (skillLevel: 7-8)
- Expert (skillLevel: 9-10)

**Play styles:**
- Tight-Passive, Tight-Aggressive, Loose-Passive, Loose-Aggressive

Each AI player row shows: Name (editable) | Skill dropdown | Style dropdown

The play style maps to a `playStyle` string on `AIPlayerConfigDto`. The server may ignore it for now, but the field is sent.

### Files
- Modify: `code/web/app/games/create/page.tsx` — enhanced AI player config UI
- Modify: `code/web/lib/game/types.ts` — add `playStyle` to AIPlayerConfigDto if not present

---

## Feature 4: Poker Math Library

### Problem
Features 5-7 (starting hands, simulator, advisor) all require poker hand evaluation and equity calculation. No poker math exists in the web client.

### Design

Pure TypeScript library in `code/web/lib/poker/`:

**Hand Evaluator (`handEvaluator.ts`):**
- `evaluateHand(cards: string[]): HandResult` — Evaluate 5-7 cards, return hand rank + kickers
- `compareHands(a: HandResult, b: HandResult): number` — Compare two evaluated hands
- Hand ranks: Royal Flush (9) → High Card (0)
- Cards represented as 2-char strings ("Ah", "Kd", etc.)
- Uses rank/suit decomposition with optimized lookup tables

**Equity Calculator (`equityCalculator.ts`):**
- `calculateEquity(holeCards: string[], communityCards: string[], numOpponents: number, iterations?: number): EquityResult`
- Monte Carlo simulation: deal random remaining cards, evaluate all hands, track wins/ties/losses
- Default 10,000 iterations (configurable)
- Returns `{ win: number, tie: number, loss: number }` as percentages
- Supports Web Worker execution for non-blocking computation

**Deck utilities (`deck.ts`):**
- Standard 52-card deck constant
- `shuffle(deck: string[]): string[]` — Fisher-Yates shuffle
- `removeCards(deck: string[], cards: string[]): string[]` — Remove known cards

**Types (`types.ts`):**
- `HandRank` enum (ROYAL_FLUSH through HIGH_CARD)
- `HandResult` interface (rank, kickers, description)
- `EquityResult` interface (win, tie, loss percentages)

### Files
- New: `code/web/lib/poker/types.ts`
- New: `code/web/lib/poker/deck.ts`
- New: `code/web/lib/poker/handEvaluator.ts`
- New: `code/web/lib/poker/equityCalculator.ts`
- New: `code/web/lib/poker/__tests__/` (comprehensive test suite)

---

## Feature 5: Starting Hands Chart

### Problem
The desktop has a hand selection matrix showing all 169 preflop combinations. No equivalent exists in the web client.

### Design

**StartingHandsChart component:**
- 13×13 grid: rows and columns labeled A, K, Q, J, T, 9, 8, 7, 6, 5, 4, 3, 2
- Upper-right triangle = suited hands (e.g., "AKs"), lower-left = offsuit ("AKo"), diagonal = pairs ("AA")
- Color-coded by standard preflop categories:
  - Green: Premium (AA, KK, QQ, AKs, etc.)
  - Yellow: Strong (JJ, TT, AQs, KQs, etc.)
  - Orange: Playable (suited connectors, medium pairs, etc.)
  - Gray: Marginal/fold
- Click a cell to see hand name and category
- Current hand highlighted when `currentHand` prop is provided

**Data file (`startingHands.ts`):**
- Map of all 169 hand combinations to categories
- Based on standard 9-player full ring preflop charts

**Access:** Toolbar button on poker table (grid icon), also embedded in advisor panel.

### Files
- New: `code/web/components/game/StartingHandsChart.tsx`
- New: `code/web/lib/poker/startingHands.ts`
- New: `code/web/components/game/__tests__/StartingHandsChart.test.tsx`

---

## Feature 6: Poker Simulator

### Problem
The desktop has a full poker simulator dialog for calculating hand equity. The web client has no equivalent.

### Design

**Simulator modal** accessible from poker table toolbar (calculator icon):

**Card Selection Area:**
- 2 hole card slots + 5 community card slots
- Click a slot to open CardPicker (grid of 52 cards organized by suit)
- Selected cards shown in slot, removed from picker
- "Clear" button per slot, "Clear All" button
- "Load from Game" button fills current hole cards + community cards

**Controls:**
- Opponent count slider: 1-9
- "Calculate" button starts simulation
- Progress bar during calculation
- "Cancel" button to stop

**Results:**
- Win: XX.X%
- Tie: XX.X%
- Loss: XX.X%
- Simulations run: N

**CardPicker component:** 4×13 grid (suits × ranks), unavailable cards grayed out, click to select.

**Computation:** Runs Monte Carlo via `equityCalculator`. For >10,000 iterations, uses a Web Worker to avoid blocking the UI thread. Shows progress updates.

### Files
- New: `code/web/components/game/Simulator.tsx`
- New: `code/web/components/game/CardPicker.tsx`
- New: `code/web/lib/poker/equityWorker.ts` (Web Worker)
- New: `code/web/components/game/__tests__/Simulator.test.tsx`
- Modify: `code/web/components/game/PokerTable.tsx` — add toolbar button

---

## Feature 7: AI Advisor Panel

### Problem
The desktop has a detailed AI advisor showing hand strength, decision factors, and a 13×13 hand matrix. The web client has nothing.

### Design

**AdvisorPanel** — Side panel (same position as GameInfoPanel, toggled with **V** key or toolbar button):

**Sections:**
1. **Hand Strength** — Current hand description + strength category
   - Uses `handEvaluator.evaluateHand()` with current hole cards + community cards
   - Shows: "Two Pair, Aces and Kings" + strength bar

2. **Equity** — Win probability against N opponents
   - Uses `equityCalculator.calculateEquity()`
   - Shows: "Equity: 72.3% (vs 3 opponents)"
   - Auto-recalculates when cards change

3. **Pot Odds** — Whether calling is mathematically justified
   - Pot odds = call amount / (pot + call amount)
   - Compare to equity: "Pot odds: 25% — Equity: 72% — +EV Call"
   - Green text for +EV, red for -EV

4. **Starting Hand** — Preflop hand category (when on preflop)
   - Shows category name and the 13×13 chart with current hand highlighted

5. **Recommendation** — Simple heuristic action suggestion
   - Based on equity vs pot odds comparison
   - Not a full AI engine — just basic math guidance

### Files
- New: `code/web/components/game/AdvisorPanel.tsx`
- Modify: `code/web/components/game/PokerTable.tsx` — add V key handler + toolbar button

---

## Feature 8: Dashboard Widget System

### Problem
The desktop has a customizable dashboard with 15 collapsible widgets. The web client has no dashboard.

### Design

**Dashboard component** — Right-side panel toggled with **D** key or toolbar button:

**Layout:**
- Vertical stack of collapsible widget sections
- Each widget has a header (click to collapse/expand) and a body
- Settings button opens a configuration dialog to show/hide and reorder widgets
- Widget visibility and order persisted in localStorage via `useDashboard` hook

**Core Widgets (6):**

1. **Hand Strength** — Current hand name + equity bar (compact version of advisor's hand section)
2. **Pot Odds** — Pot size, call cost, odds ratio (compact)
3. **Tournament Info** — Level, blinds, time to next level, players remaining
4. **Rank** — "Position: 3rd of 8 players"
5. **Quick Actions** — Compact fold/check/call buttons for fast play
6. **Starting Hand** — Small 13×13 grid with current hand highlighted (preflop only)

**DashboardSettings dialog:**
- List of all widgets with checkboxes (show/hide)
- Drag handles or up/down buttons to reorder
- "Reset to defaults" button

### Files
- New: `code/web/components/game/Dashboard.tsx`
- New: `code/web/components/game/DashboardWidget.tsx`
- New: `code/web/components/game/DashboardSettings.tsx`
- New: `code/web/lib/game/useDashboard.ts`
- Modify: `code/web/components/game/PokerTable.tsx` — add D key handler + toolbar button

---

## Architecture Notes

- **Poker math library** is the foundational dependency for features 5-7
- **No server changes needed** — all features are client-side
- **No new npm dependencies** — Web Workers are native, all UI is custom React + Tailwind
- **Four-color deck** requires generating 26 new PNG card images
- **localStorage keys** — new: `ddpoker-game-prefs`, `ddpoker-dashboard`
- **Testing** — Poker math library gets comprehensive unit tests (hand evaluation correctness is critical). UI components get standard Vitest + React Testing Library tests.
- **Performance** — Equity calculations >10k iterations use Web Workers. Dashboard widgets update only on relevant game state changes.
