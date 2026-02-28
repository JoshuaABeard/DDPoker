# Web Client Feature Parity Round 3 — Design

**Status:** APPROVED (2026-02-27)
**Depends on:** None (all features are additive)
**Scope:** 8 features across visual polish, game creation, analysis, and player customization

---

## Context

Rounds 1 (core gameplay polish) and 2 (audio & immersion) are complete. This round addresses the remaining feature gaps between the web client and desktop Java client: visual theming, card animations, player avatars, blind presets, scheduled breaks, hand rankings reference, and visual hand replay.

## Feature 1: Table Felt Themes

### Problem
TableFelt.tsx has a hardcoded green radial gradient. The desktop client allows customizable table colors.

### Design

**5 built-in themes** stored in localStorage key `ddpoker-theme`:

| Theme | Center | Mid | Edge | Border |
|-------|--------|-----|------|--------|
| Classic Green (default) | `#2d5a1b` | `#1e3d12` | `#152d0d` | `#1a2e0f` |
| Royal Blue | `#1b3d5a` | `#122d3e` | `#0d1f2d` | `#0f1e2e` |
| Casino Red | `#5a1b1b` | `#3d1212` | `#2d0d0d` | `#2e0f0f` |
| Dark Night | `#2a2a3a` | `#1e1e2e` | `#141420` | `#1a1a28` |
| Wooden | `#5a3d1b` | `#3d2a12` | `#2d1f0d` | `#2e1a0f` |

**useTheme hook:** Reads/writes localStorage, returns `{ theme, setTheme, colors }`. Colors object contains `center`, `mid`, `edge`, `border` hex values.

**ThemePicker component:** Small popover button (paint palette icon) near VolumeControl in the bottom-left toolbar. Shows 5 colored circles, click to select. Also serves as container for card back and avatar selection (tabs within the popover).

**TableFelt.tsx changes:** Read theme colors from `useTheme()` instead of hardcoded hex values. Apply as inline styles on the radial gradient.

### Files
- New: `code/web/lib/theme/useTheme.ts` (~40 lines)
- New: `code/web/lib/theme/themes.ts` (~30 lines, theme definitions)
- New: `code/web/components/game/ThemePicker.tsx` (~80 lines)
- Modify: `code/web/components/game/TableFelt.tsx` — use theme colors
- Modify: `code/web/components/game/PokerTable.tsx` — add ThemePicker

---

## Feature 2: Card Animations (Framer Motion)

### Problem
Cards appear instantly with no dealing, flipping, or transition animation. The desktop client has smooth card dealing animations.

### Design

**Install `framer-motion`** as a dependency.

**Card.tsx enhancements:**
- Wrap card image in `motion.div`
- Face-down to face-up: Y-axis 3D rotation (180° flip, 300ms)
- Appearance: fade in + slight scale from 0.8 to 1.0 (200ms)
- Use `layoutId` on card wrapper for smooth position transitions

**CommunityCards.tsx enhancements:**
- Wrap in `AnimatePresence`
- Flop: 3 cards stagger in with 150ms delay between each
- Turn/River: single card slides in from right with fade
- Cards animate from slightly above their final position

**PlayerSeat.tsx enhancements:**
- Hole cards: slide in from table center toward seat position (200ms, staggered 100ms)
- Fold: cards fade out and shrink (150ms)
- Winning hand: pulse glow ring animation (yellow, 2 cycles)

### Files
- Modify: `code/web/package.json` — add `framer-motion`
- Modify: `code/web/components/game/Card.tsx` — motion wrapper, flip animation
- Modify: `code/web/components/game/CommunityCards.tsx` — AnimatePresence, stagger
- Modify: `code/web/components/game/PlayerSeat.tsx` — deal, fold, win animations

---

## Feature 3: Card Back Designs

### Problem
Face-down cards always show `card_blank.png`. The desktop client has multiple card back designs.

### Design

**4 built-in card back designs** as inline SVG components:
- **Classic Red** (default) — red diamond pattern with white border
- **Blue Diamond** — blue with interlocking diamond motif
- **Green Celtic** — green with celtic knot pattern
- **Gold Royal** — gold with crown/shield emblem

Stored in localStorage key `ddpoker-card-back`. When a card is face-down, `Card.tsx` renders the selected SVG instead of `card_blank.png`.

Selection UI integrated as a tab in the ThemePicker popover.

### Files
- New: `code/web/components/game/cardBacks.tsx` (~120 lines, 4 SVG components)
- New: `code/web/lib/theme/useCardBack.ts` (~25 lines)
- Modify: `code/web/components/game/Card.tsx` — render SVG card back when face-down

---

## Feature 4: Player Avatars

### Problem
Player seats show only name and chip count. The desktop client has player avatars.

### Design

**12 built-in SVG avatar icons** (poker-themed):
- Animals: Bear, Eagle, Fox, Wolf, Shark, Owl
- Objects: Crown, Diamond, Spade, Star, Flame, Lightning

Stored in localStorage key `ddpoker-avatar`. Each avatar is a simple 28x28px SVG icon rendered in a colored circle.

**PlayerSeat.tsx changes:**
- Add a 28px circular avatar above the player name
- Local player shows their selected avatar
- Other players show a default silhouette icon
- Avatar circle uses a consistent color (gray-600 bg)

**Selection UI:** Tab in ThemePicker popover showing a 4x3 grid of avatar icons.

### Files
- New: `code/web/components/game/avatarIcons.tsx` (~150 lines, 12 SVG components)
- New: `code/web/lib/theme/useAvatar.ts` (~25 lines)
- Modify: `code/web/components/game/PlayerSeat.tsx` — add avatar circle
- Modify: `code/web/components/game/ThemePicker.tsx` — add avatar tab

---

## Feature 5: Blind Structure Presets

### Problem
Game creators must manually configure blind levels. The desktop client offers preset structures.

### Design

**3 presets** as a dropdown above the blind structure editor:

| Preset | Levels | Starting | Final | Duration |
|--------|--------|----------|-------|----------|
| Turbo | 8 | 25/50 | 1000/2000 | 3 min |
| Standard | 10 | 25/50 | 800/1600 | 5 min |
| Deep Stack | 12 | 10/20 | 500/1000 | 8 min |
| Custom | — | — | — | — |

Selecting a preset replaces the blind table with the preset values. "Custom" preserves current manual entries. The dropdown defaults to "Standard" (matching the existing default).

### Files
- New: `code/web/lib/game/blindPresets.ts` (~50 lines)
- Modify: `code/web/app/games/create/page.tsx` — add preset dropdown

---

## Feature 6: Scheduled Breaks

### Problem
The blind structure type already has `isBreak: boolean` but there is no UI to add break levels.

### Design

**"Add Break" button** next to the existing "Add Level" button in the blind structure editor.

Break rows:
- Displayed with a blue highlight background (distinct from yellow current-level)
- Only show a "Duration" field (no blinds/ante)
- Labeled "BREAK" in the level column

**TournamentInfoBar enhancement:**
- When a break level is approaching, show "Break in N levels" hint

### Files
- Modify: `code/web/app/games/create/page.tsx` — add break button, break row styling
- Modify: `code/web/components/game/TournamentInfoBar.tsx` — break countdown hint

---

## Feature 7: Hand Rankings Reference

### Problem
No in-game poker hand rankings reference. Desktop client has an accessible hand chart.

### Design

**HandRankings panel** toggled with **H** key (like **I** for GameInfoPanel):
- Centered modal overlay showing all 10 poker hand rankings
- Each ranking shows: name, description, and a 5-card visual example using the Card component
- Rankings listed from strongest (Royal Flush) to weakest (High Card)
- Click outside or press H/Escape to close
- Styled as a semi-transparent dark overlay with white panel

### Files
- New: `code/web/components/game/HandRankings.tsx` (~100 lines)
- Modify: `code/web/components/game/PokerTable.tsx` — add H key handler, render HandRankings

---

## Feature 8: Visual Hand Replay

### Problem
Hand history is text-only. The desktop client allows replaying hands visually.

### Design

**HandReplay modal** launched by clicking a hand number in HandHistory:
- Renders a compact version of the poker table using existing components (TableFelt, CommunityCards, Card)
- Shows player positions with names and chip counts at hand start

**Playback state machine:**
1. Hand start (show player names, chip counts)
2. Hole cards dealt (face-down for others, face-up for local player)
3. Preflop actions (step through each action, highlight acting player)
4. Flop (3 cards appear with animation)
5. Flop actions
6. Turn (1 card appears)
7. Turn actions
8. River (1 card appears)
9. River actions
10. Showdown (reveal remaining cards)
11. Winner (highlight winner, show pot awarded)

**Controls:**
- Previous Step / Next Step buttons
- Play/Pause auto-advance (1 second per step)
- Speed selector: 1x, 2x, 4x
- Progress bar showing position in timeline

**Data source:** Groups `HandHistoryEntry[]` by `handNumber` and builds a step array from the entries. Each step contains the state delta to apply.

**useHandReplayState hook:** Manages the step index, play/pause state, speed, and derives the current table state (community cards visible, player actions, pot) from accumulated steps.

### Files
- New: `code/web/components/game/HandReplay.tsx` (~150 lines)
- New: `code/web/lib/game/useHandReplayState.ts` (~100 lines)
- Modify: `code/web/components/game/HandHistory.tsx` — clickable hand numbers

---

## Architecture Notes

- All new features are additive — no existing functionality is changed
- Only new dependency: `framer-motion` for card animations and hand replay
- All user preferences (theme, card back, avatar) stored client-side in localStorage
- Blind presets and scheduled breaks are game creation enhancements (no server changes)
- Hand replay uses existing HandHistoryEntry data (no new server messages)
- ThemePicker serves as a unified settings popover for theme, card back, and avatar
- Testing: each feature gets unit tests following existing Vitest + React Testing Library patterns
