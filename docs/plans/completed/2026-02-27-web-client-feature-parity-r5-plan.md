# Web Client Feature Parity Round 5 — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add game preferences, enhanced keyboard shortcuts, AI player config, a TypeScript poker math library, starting hands chart, poker simulator, AI advisor panel, and a configurable dashboard to the web client.

**Architecture:** All features are client-side only. A new `lib/poker/` module provides hand evaluation and Monte Carlo equity calculation. UI features use existing React + Tailwind patterns. Preferences stored in localStorage. The poker math library is the foundation for the simulator, advisor, and dashboard hand-strength widgets.

**Tech Stack:** React 19, TypeScript 5, Vitest + React Testing Library, Tailwind CSS 4, Web Workers (native)

**Design doc:** `docs/plans/2026-02-27-web-client-feature-parity-r5-design.md`

---

## Task 1: Game Preferences Hook (useGamePrefs)

**Files:**
- Create: `code/web/lib/game/useGamePrefs.ts`
- Test: `code/web/lib/game/__tests__/useGamePrefs.test.ts`

**Context:** Follow the exact same pattern as `useTheme.ts` and `useCardBack.ts` — `useState` with lazy localStorage init, `useCallback` setter that writes to localStorage, try-catch safety.

**Step 1: Write test**

Create `code/web/lib/game/__tests__/useGamePrefs.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach } from 'vitest'
import { useGamePrefs, DEFAULT_GAME_PREFS } from '../useGamePrefs'

beforeEach(() => {
  localStorage.clear()
})

describe('useGamePrefs', () => {
  it('returns defaults when nothing stored', () => {
    const { result } = renderHook(() => useGamePrefs())
    expect(result.current.prefs).toEqual(DEFAULT_GAME_PREFS)
  })

  it('persists changes to localStorage', () => {
    const { result } = renderHook(() => useGamePrefs())
    act(() => result.current.setPref('fourColorDeck', true))
    expect(result.current.prefs.fourColorDeck).toBe(true)
    expect(JSON.parse(localStorage.getItem('ddpoker-game-prefs')!).fourColorDeck).toBe(true)
  })

  it('loads stored prefs on mount', () => {
    localStorage.setItem('ddpoker-game-prefs', JSON.stringify({ ...DEFAULT_GAME_PREFS, checkFold: true }))
    const { result } = renderHook(() => useGamePrefs())
    expect(result.current.prefs.checkFold).toBe(true)
  })

  it('ignores invalid stored data', () => {
    localStorage.setItem('ddpoker-game-prefs', 'not-json')
    const { result } = renderHook(() => useGamePrefs())
    expect(result.current.prefs).toEqual(DEFAULT_GAME_PREFS)
  })
})
```

**Step 2: Implement hook**

Create `code/web/lib/game/useGamePrefs.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'

export interface GamePrefs {
  fourColorDeck: boolean
  checkFold: boolean
  disableShortcuts: boolean
  dealerChat: 'all' | 'actions' | 'none'
}

export const DEFAULT_GAME_PREFS: GamePrefs = {
  fourColorDeck: false,
  checkFold: false,
  disableShortcuts: false,
  dealerChat: 'all',
}

const STORAGE_KEY = 'ddpoker-game-prefs'

function load(): GamePrefs {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      return { ...DEFAULT_GAME_PREFS, ...parsed }
    }
  } catch { /* ignore */ }
  return { ...DEFAULT_GAME_PREFS }
}

export function useGamePrefs() {
  const [prefs, setPrefs] = useState<GamePrefs>(load)

  const setPref = useCallback(<K extends keyof GamePrefs>(key: K, value: GamePrefs[K]) => {
    setPrefs((prev) => {
      const next = { ...prev, [key]: value }
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)) } catch { /* ignore */ }
      return next
    })
  }, [])

  return { prefs, setPref }
}
```

**Step 3: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/lib/game/useGamePrefs.ts code/web/lib/game/__tests__/useGamePrefs.test.ts
git commit -m "feat(web): add game preferences hook (four-color, check-fold, shortcuts, dealer chat)"
```

---

## Task 2: ThemePicker Gameplay Tab

**Files:**
- Modify: `code/web/components/game/ThemePicker.tsx`

**Context:** ThemePicker has 3 tabs (Table/Cards/Avatar) defined at line 20-21. Add a 4th "Gameplay" tab with toggle switches for the 4 game preferences.

**Step 1: Add Gameplay tab and useGamePrefs import**

In `ThemePicker.tsx`:
- Import `useGamePrefs` from `@/lib/game/useGamePrefs`
- Change Tab type to: `type Tab = 'Table' | 'Cards' | 'Avatar' | 'Gameplay'`
- Add `'Gameplay'` to TABS array
- Call `const { prefs, setPref } = useGamePrefs()` inside the component
- After the Avatar tab content section, add:

```tsx
{tab === 'Gameplay' && (
  <div className="space-y-3">
    <label className="flex items-center justify-between cursor-pointer">
      <span className="text-sm">Four-Color Deck</span>
      <input type="checkbox" checked={prefs.fourColorDeck} onChange={(e) => setPref('fourColorDeck', e.target.checked)} className="w-4 h-4 rounded" />
    </label>
    <label className="flex items-center justify-between cursor-pointer">
      <span className="text-sm">Check-Fold</span>
      <input type="checkbox" checked={prefs.checkFold} onChange={(e) => setPref('checkFold', e.target.checked)} className="w-4 h-4 rounded" />
    </label>
    <label className="flex items-center justify-between cursor-pointer">
      <span className="text-sm">Disable Shortcuts</span>
      <input type="checkbox" checked={prefs.disableShortcuts} onChange={(e) => setPref('disableShortcuts', e.target.checked)} className="w-4 h-4 rounded" />
    </label>
    <div className="flex items-center justify-between">
      <span className="text-sm">Dealer Chat</span>
      <select value={prefs.dealerChat} onChange={(e) => setPref('dealerChat', e.target.value as 'all' | 'actions' | 'none')} className="bg-gray-700 text-white text-xs rounded px-2 py-1">
        <option value="all">All</option>
        <option value="actions">Actions Only</option>
        <option value="none">None</option>
      </select>
    </div>
  </div>
)}
```

**Step 2: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/components/game/ThemePicker.tsx
git commit -m "feat(web): add Gameplay tab to ThemePicker settings"
```

---

## Task 3: Four-Color Deck Card Images

**Files:**
- Create: `code/web/scripts/generate-four-color-cards.sh` (utility script)
- Create: `code/web/public/images/cards/4color/` (26 PNG files)
- Modify: `code/web/components/game/Card.tsx`

**Context:** Card.tsx renders PNGs from `/images/cards/card_{code}.png`. For four-color mode, diamonds should be blue and clubs should be green. We generate recolored PNGs and Card.tsx picks the right path based on the `fourColorDeck` prop.

**Step 1: Generate four-color card images**

Create a script that uses ImageMagick to recolor diamond cards (red→blue) and club cards (black→green). If ImageMagick isn't available, create the PNGs manually or use a CSS filter fallback.

For each diamond card (d suit): apply hue-rotate to shift red→blue
For each club card (c suit): apply hue-rotate to shift black→green

Place output in `code/web/public/images/cards/4color/card_{code}.png` for the 26 recolored cards.

**Fallback approach if image generation isn't practical:** Use CSS `filter: hue-rotate()` on the `<img>` element:
- Diamonds: `filter: hue-rotate(240deg) saturate(1.5)` (red → blue)
- Clubs: `filter: hue-rotate(120deg) saturate(2) brightness(1.3)` (black → green)

**Step 2: Modify Card.tsx**

Add optional `fourColorDeck?: boolean` prop. When true and the card suit is 'd' or 'c', either:
- Use the alternate image path: `/images/cards/4color/card_{code}.png`
- Or apply CSS filter to the existing image

```typescript
// In Card component, determine image source:
const suit = validCode ? validCode[1].toLowerCase() : ''
const useFourColor = fourColorDeck && (suit === 'd' || suit === 'c')
const imagePath = useFourColor
  ? `/images/cards/4color/card_${validCode}.png`
  : `/images/cards/card_${validCode}.png`
```

If using CSS filter fallback instead of new PNGs:
```typescript
const fourColorFilter = fourColorDeck
  ? suit === 'd' ? 'hue-rotate(240deg) saturate(1.5)'
  : suit === 'c' ? 'hue-rotate(120deg) saturate(2) brightness(1.3)'
  : undefined
  : undefined
```

**Step 3: Pass fourColorDeck through component tree**

PokerTable.tsx needs to read `prefs.fourColorDeck` from `useGamePrefs()` and pass it to components that render Card: PlayerSeat, CommunityCards, HandReplay, ColorUpOverlay. The simplest approach is passing it as a prop through the component tree.

**Step 4: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/components/game/Card.tsx code/web/public/images/cards/4color/ code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add four-color deck support (blue diamonds, green clubs)"
```

---

## Task 4: Keyboard Shortcuts Enhancement

**Files:**
- Modify: `code/web/components/game/ActionPanel.tsx`
- Modify: `code/web/components/game/PokerTable.tsx`

**Context:** ActionPanel.tsx has keyboard handling in a useEffect (lines 44-78). PokerTable.tsx has H/I key handlers. Both need to respect `disableShortcuts` pref.

**Step 1: Add N key to ActionPanel**

In the keyboard handler (ActionPanel.tsx ~line 49), add a case for the N key. This requires knowing the current input mode. Add an optional `inputMode?: string` prop to ActionPanel and check for DEAL/CONTINUE modes:

```typescript
if (e.key === 'n' || e.key === 'N') {
  if (inputMode === 'DEAL' || inputMode === 'CONTINUE') {
    onAction({ action: inputMode === 'DEAL' ? 'DEAL' : 'CONTINUE', amount: 0 } as PlayerActionData)
  }
}
```

Note: Check if the action types include DEAL/CONTINUE. If not, use `sendAction` directly from the play page or PokerTable instead.

**Step 2: Add disableShortcuts support**

Both ActionPanel and PokerTable keyboard handlers should check `disableShortcuts`. Pass it as a prop:

In ActionPanel's keyboard handler, at the top:
```typescript
if (disableShortcuts) return
```

In PokerTable's keyboard handler, same guard.

**Step 3: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/components/game/ActionPanel.tsx code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add N key shortcut and disable-shortcuts support"
```

---

## Task 5: Dealer Chat Filtering

**Files:**
- Modify: `code/web/components/game/ChatPanel.tsx`

**Context:** ChatPanel filters messages by muted player IDs (line 85). Add dealer chat filtering based on the `dealerChat` preference. Dealer messages have `tableChat: true`.

**Step 1: Add dealerChat prop and filter**

Add `dealerChat?: 'all' | 'actions' | 'none'` to ChatPanelProps.

Update the `visibleMessages` filter (line 85):

```typescript
const visibleMessages = messages.filter((m) => {
  if (mutedIds && mutedIds.has(m.playerId)) return false
  if (m.tableChat && dealerChat === 'none') return false
  return true
})
```

Note: The `'actions'` mode would filter non-action dealer messages, but since we can't distinguish dealer message subtypes from the ChatEntry data, `'actions'` behaves the same as `'all'` for now. The UI shows three options but only `'none'` actively filters.

**Step 2: Pass dealerChat from PokerTable**

In PokerTable.tsx, read `prefs.dealerChat` from `useGamePrefs()` and pass to ChatPanel.

**Step 3: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/components/game/ChatPanel.tsx code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add dealer chat filtering preference"
```

---

## Task 6: AI Player Type Configuration

**Files:**
- Modify: `code/web/app/games/create/page.tsx`

**Context:** AI player config (lines 29-38, 541-594) currently shows name + numeric skill level. Replace with named presets and add play style dropdown.

**Step 1: Define skill presets and play styles**

Add constants at the top of the file:

```typescript
const SKILL_PRESETS = [
  { label: 'Novice', value: 2 },
  { label: 'Beginner', value: 4 },
  { label: 'Intermediate', value: 6 },
  { label: 'Advanced', value: 8 },
  { label: 'Expert', value: 10 },
] as const

const PLAY_STYLES = ['Tight-Passive', 'Tight-Aggressive', 'Loose-Passive', 'Loose-Aggressive'] as const
```

**Step 2: Update AI player state to include playStyle**

Change the AI player array type to include `playStyle`:

```typescript
const DEFAULT_AI_PLAYERS: Array<{ name: string; skillLevel: number; playStyle: string }> = [
  { name: 'AI 1', skillLevel: 6, playStyle: 'Tight-Aggressive' },
  // ... repeat for AI 2-8
]
```

**Step 3: Update AI player UI**

Replace the numeric skill input with a select dropdown:

```tsx
<select value={SKILL_PRESETS.find((p) => p.value >= ai.skillLevel)?.label ?? 'Intermediate'}
  onChange={(e) => {
    const preset = SKILL_PRESETS.find((p) => p.label === e.target.value)
    if (preset) updateAiPlayer(idx, 'skillLevel', preset.value)
  }}
  className="bg-gray-700 text-white text-xs rounded px-2 py-1">
  {SKILL_PRESETS.map((p) => (
    <option key={p.label} value={p.label}>{p.label}</option>
  ))}
</select>

<select value={ai.playStyle}
  onChange={(e) => updateAiPlayer(idx, 'playStyle', e.target.value)}
  className="bg-gray-700 text-white text-xs rounded px-2 py-1">
  {PLAY_STYLES.map((s) => (
    <option key={s} value={s}>{s}</option>
  ))}
</select>
```

**Step 4: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/app/games/create/page.tsx
git commit -m "feat(web): add skill presets and play style for AI players"
```

---

## Task 7: Poker Math — Types & Deck

**Files:**
- Create: `code/web/lib/poker/types.ts`
- Create: `code/web/lib/poker/deck.ts`
- Test: `code/web/lib/poker/__tests__/deck.test.ts`

**Step 1: Create types**

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

export enum HandRank {
  HIGH_CARD = 0,
  ONE_PAIR = 1,
  TWO_PAIR = 2,
  THREE_OF_A_KIND = 3,
  STRAIGHT = 4,
  FLUSH = 5,
  FULL_HOUSE = 6,
  FOUR_OF_A_KIND = 7,
  STRAIGHT_FLUSH = 8,
  ROYAL_FLUSH = 9,
}

export const HAND_RANK_NAMES: Record<HandRank, string> = {
  [HandRank.HIGH_CARD]: 'High Card',
  [HandRank.ONE_PAIR]: 'One Pair',
  [HandRank.TWO_PAIR]: 'Two Pair',
  [HandRank.THREE_OF_A_KIND]: 'Three of a Kind',
  [HandRank.STRAIGHT]: 'Straight',
  [HandRank.FLUSH]: 'Flush',
  [HandRank.FULL_HOUSE]: 'Full House',
  [HandRank.FOUR_OF_A_KIND]: 'Four of a Kind',
  [HandRank.STRAIGHT_FLUSH]: 'Straight Flush',
  [HandRank.ROYAL_FLUSH]: 'Royal Flush',
}

export interface HandResult {
  rank: HandRank
  /** Kicker values for tiebreaking, highest first */
  kickers: number[]
  /** Human-readable description, e.g. "Two Pair, Aces and Kings" */
  description: string
}

export interface EquityResult {
  win: number   // percentage 0-100
  tie: number
  loss: number
  iterations: number
}

/** Rank values: 2=2, 3=3, ..., T=10, J=11, Q=12, K=13, A=14 */
export const RANK_VALUES: Record<string, number> = {
  '2': 2, '3': 3, '4': 4, '5': 5, '6': 6, '7': 7, '8': 8,
  '9': 9, 'T': 10, 'J': 11, 'Q': 12, 'K': 13, 'A': 14,
}

export const RANKS = ['2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A'] as const
export const SUITS = ['h', 'd', 'c', 's'] as const
```

**Step 2: Create deck utilities**

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { RANKS, SUITS } from './types'

/** Full 52-card deck as 2-char card codes */
export const FULL_DECK: string[] = RANKS.flatMap((r) => SUITS.map((s) => `${r}${s}`))

/** Fisher-Yates shuffle (mutates array) */
export function shuffle(deck: string[]): string[] {
  for (let i = deck.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[deck[i], deck[j]] = [deck[j], deck[i]]
  }
  return deck
}

/** Return a new deck with the given cards removed */
export function removeCards(deck: string[], cards: string[]): string[] {
  const set = new Set(cards.map((c) => c.toLowerCase()))
  return deck.filter((c) => !set.has(c.toLowerCase()))
}
```

**Step 3: Write deck tests**

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { FULL_DECK, shuffle, removeCards } from '../deck'

describe('deck', () => {
  it('has 52 cards', () => {
    expect(FULL_DECK.length).toBe(52)
  })

  it('has no duplicates', () => {
    expect(new Set(FULL_DECK).size).toBe(52)
  })

  it('shuffle returns same length', () => {
    const deck = [...FULL_DECK]
    shuffle(deck)
    expect(deck.length).toBe(52)
    expect(new Set(deck).size).toBe(52)
  })

  it('removeCards removes specified cards', () => {
    const result = removeCards(FULL_DECK, ['Ah', 'Kd'])
    expect(result.length).toBe(50)
    expect(result).not.toContain('Ah')
    expect(result).not.toContain('Kd')
  })

  it('removeCards is case-insensitive', () => {
    const result = removeCards(FULL_DECK, ['ah', 'KD'])
    expect(result.length).toBe(50)
  })
})
```

**Step 4: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/lib/poker/
git commit -m "feat(web): add poker math types and deck utilities"
```

---

## Task 8: Poker Math — Hand Evaluator

**Files:**
- Create: `code/web/lib/poker/handEvaluator.ts`
- Test: `code/web/lib/poker/__tests__/handEvaluator.test.ts`

**Step 1: Write comprehensive tests**

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { evaluateHand, compareHands } from '../handEvaluator'
import { HandRank } from '../types'

describe('evaluateHand', () => {
  it('detects royal flush', () => {
    const result = evaluateHand(['Ah', 'Kh', 'Qh', 'Jh', 'Th'])
    expect(result.rank).toBe(HandRank.ROYAL_FLUSH)
  })

  it('detects straight flush', () => {
    const result = evaluateHand(['9c', '8c', '7c', '6c', '5c'])
    expect(result.rank).toBe(HandRank.STRAIGHT_FLUSH)
  })

  it('detects four of a kind', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Ac', 'As', 'Kh'])
    expect(result.rank).toBe(HandRank.FOUR_OF_A_KIND)
  })

  it('detects full house', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Ac', 'Kh', 'Kd'])
    expect(result.rank).toBe(HandRank.FULL_HOUSE)
  })

  it('detects flush', () => {
    const result = evaluateHand(['Ah', 'Jh', '9h', '5h', '3h'])
    expect(result.rank).toBe(HandRank.FLUSH)
  })

  it('detects straight', () => {
    const result = evaluateHand(['Ah', 'Kd', 'Qc', 'Js', 'Th'])
    expect(result.rank).toBe(HandRank.STRAIGHT)
  })

  it('detects ace-low straight (wheel)', () => {
    const result = evaluateHand(['Ah', '2d', '3c', '4s', '5h'])
    expect(result.rank).toBe(HandRank.STRAIGHT)
  })

  it('detects three of a kind', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Ac', 'Ks', '9h'])
    expect(result.rank).toBe(HandRank.THREE_OF_A_KIND)
  })

  it('detects two pair', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Kc', 'Ks', '9h'])
    expect(result.rank).toBe(HandRank.TWO_PAIR)
  })

  it('detects one pair', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Kc', 'Qs', '9h'])
    expect(result.rank).toBe(HandRank.ONE_PAIR)
  })

  it('detects high card', () => {
    const result = evaluateHand(['Ah', 'Jd', '9c', '5s', '3h'])
    expect(result.rank).toBe(HandRank.HIGH_CARD)
  })

  it('evaluates 7-card hand (picks best 5)', () => {
    const result = evaluateHand(['Ah', 'Kh', 'Qh', 'Jh', 'Th', '3c', '2d'])
    expect(result.rank).toBe(HandRank.ROYAL_FLUSH)
  })

  it('evaluates 6-card hand', () => {
    const result = evaluateHand(['Ah', 'Ad', 'Ac', 'Kh', 'Kd', '2c'])
    expect(result.rank).toBe(HandRank.FULL_HOUSE)
  })
})

describe('compareHands', () => {
  it('higher rank wins', () => {
    const flush = evaluateHand(['Ah', 'Jh', '9h', '5h', '3h'])
    const straight = evaluateHand(['Ah', 'Kd', 'Qc', 'Js', 'Th'])
    expect(compareHands(flush, straight)).toBeGreaterThan(0)
  })

  it('same rank uses kickers', () => {
    const aceHigh = evaluateHand(['Ah', 'Kd', '9c', '5s', '3h'])
    const kingHigh = evaluateHand(['Kh', 'Qd', '9c', '5s', '3h'])
    expect(compareHands(aceHigh, kingHigh)).toBeGreaterThan(0)
  })

  it('identical hands return 0', () => {
    const hand1 = evaluateHand(['Ah', 'Kd', 'Qc', 'Js', '9h'])
    const hand2 = evaluateHand(['Ac', 'Ks', 'Qh', 'Jd', '9c'])
    expect(compareHands(hand1, hand2)).toBe(0)
  })

  it('higher pair beats lower pair', () => {
    const aces = evaluateHand(['Ah', 'Ad', 'Kc', 'Qs', '9h'])
    const kings = evaluateHand(['Kh', 'Kd', 'Ac', 'Qs', '9h'])
    expect(compareHands(aces, kings)).toBeGreaterThan(0)
  })
})
```

**Step 2: Implement hand evaluator**

Create `code/web/lib/poker/handEvaluator.ts`. The evaluator:

1. Parses cards into rank/suit arrays
2. For 6-7 card hands, generates all C(n,5) combinations and evaluates each, keeping the best
3. For exactly 5 cards, checks for flush, straight, then counts ranks for pairs/trips/quads
4. Returns `HandResult` with rank, kickers, and description

Key implementation details:
- Ace-low straight (A-2-3-4-5): When checking straights, if A-K-Q-J-T not present but A-5-4-3-2 is, it's a wheel with kicker value 5 (not 14)
- Royal flush: Straight flush with high card Ace
- Kickers sorted descending for tiebreaking
- `compareHands`: Compare rank first, then kicker-by-kicker

```typescript
export function evaluateHand(cards: string[]): HandResult
export function compareHands(a: HandResult, b: HandResult): number
```

**Step 3: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/lib/poker/handEvaluator.ts code/web/lib/poker/__tests__/handEvaluator.test.ts
git commit -m "feat(web): add poker hand evaluator with full hand ranking support"
```

---

## Task 9: Poker Math — Equity Calculator

**Files:**
- Create: `code/web/lib/poker/equityCalculator.ts`
- Test: `code/web/lib/poker/__tests__/equityCalculator.test.ts`

**Step 1: Write tests**

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { calculateEquity } from '../equityCalculator'

describe('calculateEquity', () => {
  it('returns percentages that sum to ~100', () => {
    const result = calculateEquity(['Ah', 'Kh'], [], 1, 1000)
    const total = result.win + result.tie + result.loss
    expect(total).toBeGreaterThan(99)
    expect(total).toBeLessThan(101)
  })

  it('AA vs 1 opponent has ~80%+ equity', () => {
    const result = calculateEquity(['Ah', 'Ad'], [], 1, 5000)
    expect(result.win).toBeGreaterThan(75)
  })

  it('known board gives deterministic results with enough iterations', () => {
    // AA with board AAKK2 — quads, nearly unbeatable
    const result = calculateEquity(['Ah', 'Ad'], ['Ac', 'As', 'Kh', 'Kd', '2c'], 1, 1000)
    expect(result.win).toBeGreaterThan(95)
  })

  it('respects iteration count', () => {
    const result = calculateEquity(['Ah', 'Kh'], [], 1, 100)
    expect(result.iterations).toBe(100)
  })

  it('handles multiple opponents', () => {
    const result = calculateEquity(['Ah', 'Ad'], [], 3, 1000)
    expect(result.win).toBeGreaterThan(50)
    expect(result.loss).toBeGreaterThan(0)
  })
})
```

**Step 2: Implement equity calculator**

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { FULL_DECK, shuffle, removeCards } from './deck'
import { evaluateHand, compareHands } from './handEvaluator'
import type { EquityResult } from './types'

/**
 * Monte Carlo equity calculator.
 * Deals random remaining cards to complete the board and opponents' hands,
 * then evaluates all hands to determine win/tie/loss rates.
 */
export function calculateEquity(
  holeCards: string[],
  communityCards: string[],
  numOpponents: number,
  iterations: number = 10000,
): EquityResult {
  const knownCards = [...holeCards, ...communityCards]
  const remainingDeck = removeCards(FULL_DECK, knownCards)
  const communityNeeded = 5 - communityCards.length

  let wins = 0
  let ties = 0
  let losses = 0

  for (let i = 0; i < iterations; i++) {
    const shuffled = shuffle([...remainingDeck])
    let dealIndex = 0

    // Deal remaining community cards
    const board = [...communityCards]
    for (let j = 0; j < communityNeeded; j++) {
      board.push(shuffled[dealIndex++])
    }

    // Deal opponent hands
    const opponentHands: string[][] = []
    for (let o = 0; o < numOpponents; o++) {
      opponentHands.push([shuffled[dealIndex++], shuffled[dealIndex++]])
    }

    // Evaluate player hand
    const playerHand = evaluateHand([...holeCards, ...board])

    // Compare against all opponents
    let playerWins = true
    let playerTies = false
    for (const oppHole of opponentHands) {
      const oppHand = evaluateHand([...oppHole, ...board])
      const cmp = compareHands(playerHand, oppHand)
      if (cmp < 0) { playerWins = false; break }
      if (cmp === 0) playerTies = true
    }

    if (!playerWins) losses++
    else if (playerTies) ties++
    else wins++
  }

  return {
    win: (wins / iterations) * 100,
    tie: (ties / iterations) * 100,
    loss: (losses / iterations) * 100,
    iterations,
  }
}
```

**Step 3: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/lib/poker/equityCalculator.ts code/web/lib/poker/__tests__/equityCalculator.test.ts
git commit -m "feat(web): add Monte Carlo equity calculator"
```

---

## Task 10: Starting Hands Data & Chart Component

**Files:**
- Create: `code/web/lib/poker/startingHands.ts`
- Create: `code/web/components/game/StartingHandsChart.tsx`
- Test: `code/web/components/game/__tests__/StartingHandsChart.test.tsx`

**Step 1: Create starting hands data**

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

export type HandCategory = 'premium' | 'strong' | 'playable' | 'marginal' | 'fold'

/** Labels for display row/column headers (A high to 2 low) */
export const HAND_LABELS = ['A', 'K', 'Q', 'J', 'T', '9', '8', '7', '6', '5', '4', '3', '2'] as const

/**
 * 13x13 grid of preflop hand categories.
 * Row = first card rank (A=0, K=1, ..., 2=12)
 * Col = second card rank
 * Upper-right triangle (col > row) = suited hands
 * Lower-left triangle (row > col) = offsuit hands
 * Diagonal (row === col) = pocket pairs
 */
export const STARTING_HAND_CATEGORIES: HandCategory[][] = [
  // Populate full 13x13 grid based on standard preflop charts
  // Example: row 0 (Ace), col 0-12
]

/** Get the hand notation for a grid cell */
export function getHandNotation(row: number, col: number): string {
  const r1 = HAND_LABELS[row]
  const r2 = HAND_LABELS[col]
  if (row === col) return `${r1}${r2}`     // pair
  if (col < row) return `${r2}${r1}s`      // suited (upper-right)
  return `${r1}${r2}o`                       // offsuit (lower-left)
}

/** Convert hole cards to grid position */
export function holeCardsToGrid(cards: string[]): { row: number; col: number } | null {
  if (cards.length !== 2) return null
  const rankOrder = 'AKQJT98765432'
  const r1 = rankOrder.indexOf(cards[0][0].toUpperCase())
  const r2 = rankOrder.indexOf(cards[1][0].toUpperCase())
  if (r1 < 0 || r2 < 0) return null
  const suited = cards[0][1].toLowerCase() === cards[1][1].toLowerCase()
  if (r1 === r2) return { row: r1, col: r1 }         // pair
  if (suited) return { row: Math.min(r1, r2), col: Math.max(r1, r2) }  // suited in upper-right
  return { row: Math.max(r1, r2), col: Math.min(r1, r2) }              // offsuit in lower-left
}
```

Populate `STARTING_HAND_CATEGORIES` with a standard 9-player full ring chart. Premium: AA-QQ, AKs. Strong: JJ-TT, AQs-AJs, KQs, AKo. Playable: 99-77, suited connectors, etc. Fill the full 13×13 grid.

**Step 2: Create StartingHandsChart component**

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { HAND_LABELS, STARTING_HAND_CATEGORIES, getHandNotation, holeCardsToGrid } from '@/lib/poker/startingHands'
import type { HandCategory } from '@/lib/poker/startingHands'

interface StartingHandsChartProps {
  /** Current hole cards to highlight */
  currentHand?: string[]
  /** Compact mode for dashboard embedding */
  compact?: boolean
  onClose?: () => void
}

const CATEGORY_COLORS: Record<HandCategory, string> = {
  premium: 'bg-green-600',
  strong: 'bg-yellow-600',
  playable: 'bg-orange-600',
  marginal: 'bg-orange-900',
  fold: 'bg-gray-700',
}

export function StartingHandsChart({ currentHand, compact, onClose }: StartingHandsChartProps) {
  const highlight = currentHand ? holeCardsToGrid(currentHand) : null
  const cellSize = compact ? 'w-5 h-5 text-[7px]' : 'w-8 h-8 text-[10px]'

  return (
    <div className={compact ? '' : 'bg-gray-900 rounded-xl shadow-2xl p-4'}>
      {!compact && (
        <div className="flex justify-between items-center mb-3">
          <h2 className="text-white font-bold text-sm">Starting Hands</h2>
          {onClose && <button onClick={onClose} className="text-gray-400 hover:text-white">×</button>}
        </div>
      )}
      <div className="grid gap-px" style={{ gridTemplateColumns: `repeat(14, auto)` }}>
        {/* Header row */}
        <div className={cellSize} />
        {HAND_LABELS.map((label) => (
          <div key={label} className={`${cellSize} flex items-center justify-center text-gray-400 font-bold`}>
            {label}
          </div>
        ))}
        {/* Grid rows */}
        {HAND_LABELS.map((rowLabel, row) => (
          <>
            <div key={`label-${row}`} className={`${cellSize} flex items-center justify-center text-gray-400 font-bold`}>
              {rowLabel}
            </div>
            {HAND_LABELS.map((_, col) => {
              const category = STARTING_HAND_CATEGORIES[row][col]
              const isHighlighted = highlight && highlight.row === row && highlight.col === col
              return (
                <div
                  key={`${row}-${col}`}
                  className={`${cellSize} flex items-center justify-center rounded-sm ${CATEGORY_COLORS[category]} ${isHighlighted ? 'ring-2 ring-white' : ''}`}
                  title={getHandNotation(row, col)}
                >
                  {!compact && <span className="text-white">{getHandNotation(row, col)}</span>}
                </div>
              )
            })}
          </>
        ))}
      </div>
      {!compact && (
        <div className="flex gap-3 mt-3 text-[10px] text-gray-300">
          <span className="flex items-center gap-1"><span className="w-3 h-3 bg-green-600 rounded-sm" /> Premium</span>
          <span className="flex items-center gap-1"><span className="w-3 h-3 bg-yellow-600 rounded-sm" /> Strong</span>
          <span className="flex items-center gap-1"><span className="w-3 h-3 bg-orange-600 rounded-sm" /> Playable</span>
          <span className="flex items-center gap-1"><span className="w-3 h-3 bg-gray-700 rounded-sm" /> Fold</span>
        </div>
      )}
    </div>
  )
}
```

**Step 3: Write tests, run, commit**

Test: renders 169 grid cells, highlights current hand, shows legend in non-compact mode.

```bash
cd code/web && npx vitest run
git add code/web/lib/poker/startingHands.ts code/web/components/game/StartingHandsChart.tsx code/web/components/game/__tests__/StartingHandsChart.test.tsx
git commit -m "feat(web): add starting hands chart with 13x13 preflop grid"
```

---

## Task 11: Card Picker Component

**Files:**
- Create: `code/web/components/game/CardPicker.tsx`
- Test: `code/web/components/game/__tests__/CardPicker.test.tsx`

**Context:** CardPicker is used by the Simulator to select specific cards. Shows all 52 cards organized by suit. Already-selected cards are disabled.

**Step 1: Create CardPicker**

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { RANKS, SUITS } from '@/lib/poker/types'
import { Card } from './Card'

interface CardPickerProps {
  /** Cards that are already in use (disabled in picker) */
  usedCards: string[]
  /** Called when a card is selected */
  onSelect: (card: string) => void
  onClose: () => void
}

export function CardPicker({ usedCards, onSelect, onClose }: CardPickerProps) {
  const usedSet = new Set(usedCards.map((c) => c.toLowerCase()))

  return (
    <div className="bg-gray-800 rounded-lg shadow-xl p-3" role="dialog" aria-label="Card picker">
      <div className="flex justify-between items-center mb-2">
        <span className="text-white text-sm font-semibold">Select Card</span>
        <button onClick={onClose} className="text-gray-400 hover:text-white text-lg">×</button>
      </div>
      <div className="space-y-1">
        {SUITS.map((suit) => (
          <div key={suit} className="flex gap-0.5">
            {[...RANKS].reverse().map((rank) => {
              const code = `${rank}${suit}`
              const isUsed = usedSet.has(code.toLowerCase())
              return (
                <button
                  key={code}
                  type="button"
                  disabled={isUsed}
                  onClick={() => { onSelect(code); onClose() }}
                  className={`${isUsed ? 'opacity-25 cursor-not-allowed' : 'hover:ring-2 hover:ring-blue-400 cursor-pointer'} rounded`}
                  aria-label={`Select ${code}`}
                >
                  <Card card={code} width={28} />
                </button>
              )
            })}
          </div>
        ))}
      </div>
    </div>
  )
}
```

**Step 2: Write tests, run, commit**

Test: renders 52 card buttons, disables used cards, calls onSelect when clicked.

```bash
cd code/web && npx vitest run
git add code/web/components/game/CardPicker.tsx code/web/components/game/__tests__/CardPicker.test.tsx
git commit -m "feat(web): add card picker component for simulator"
```

---

## Task 12: Poker Simulator

**Files:**
- Create: `code/web/components/game/Simulator.tsx`
- Test: `code/web/components/game/__tests__/Simulator.test.tsx`
- Modify: `code/web/components/game/PokerTable.tsx` — add toolbar button

**Context:** Modal dialog with card selection slots, opponent count, and equity calculation results.

**Step 1: Create Simulator component**

The simulator has:
- 2 hole card slots + 5 community card slots (click to open CardPicker)
- Opponent count selector (1-9)
- "Calculate" button that runs `calculateEquity()`
- Results display: win/tie/loss percentages
- "Load from Game" button fills current game cards
- "Clear All" button

State management: `selectedHoleCards: (string | null)[]`, `selectedCommunity: (string | null)[]`, `opponents: number`, `result: EquityResult | null`, `calculating: boolean`, `activeSlot: { type: 'hole' | 'community', index: number } | null`

Props: `holeCards?: string[]`, `communityCards?: string[]`, `onClose: () => void`

**Step 2: Wire into PokerTable toolbar**

Add a calculator icon button to the bottom-left toolbar (alongside VolumeControl and ThemePicker). State: `showSimulator: boolean`. Key binding: **S** key.

**Step 3: Write tests, run, commit**

Test: renders card slots, shows results after calculate, load-from-game fills cards.

```bash
cd code/web && npx vitest run
git add code/web/components/game/Simulator.tsx code/web/components/game/__tests__/Simulator.test.tsx code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add poker simulator with Monte Carlo equity calculator"
```

---

## Task 13: AI Advisor Panel

**Files:**
- Create: `code/web/components/game/AdvisorPanel.tsx`
- Test: `code/web/components/game/__tests__/AdvisorPanel.test.tsx`
- Modify: `code/web/components/game/PokerTable.tsx` — add V key + toolbar button

**Context:** Side panel showing hand strength, equity, pot odds, starting hand position, and a simple action recommendation.

**Step 1: Create AdvisorPanel**

Sections:
1. **Hand Strength** — Calls `evaluateHand()` with hole cards + community cards. Shows hand name and rank.
2. **Equity** — Calls `calculateEquity()` with current cards and `playersRemaining - 1` opponents. Shows win percentage.
3. **Pot Odds** — Computes `callAmount / (potSize + callAmount)`. Compares to equity.
4. **Starting Hand** — Shows embedded `StartingHandsChart` with `compact={true}` and `currentHand` highlighted.
5. **Recommendation** — Simple heuristic: if equity > pot_odds_needed → "Call/Raise +EV", else "Fold -EV"

Props:
```typescript
interface AdvisorPanelProps {
  holeCards: string[]
  communityCards: string[]
  potSize: number
  callAmount: number
  numPlayers: number
  onClose: () => void
}
```

Auto-recalculate equity when cards or player count change (use `useEffect` with debounce to avoid spamming calculations).

**Step 2: Wire into PokerTable**

Add **V** key handler and toolbar button. Show panel on the right side (same z-index area as GameInfoPanel).

**Step 3: Write tests, run, commit**

Test: renders hand strength section, shows equity, shows pot odds, recommendation text.

```bash
cd code/web && npx vitest run
git add code/web/components/game/AdvisorPanel.tsx code/web/components/game/__tests__/AdvisorPanel.test.tsx code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add AI advisor panel with hand strength, equity, and pot odds"
```

---

## Task 14: Dashboard Hook & Widget Base

**Files:**
- Create: `code/web/lib/game/useDashboard.ts`
- Create: `code/web/components/game/DashboardWidget.tsx`
- Test: `code/web/lib/game/__tests__/useDashboard.test.ts`

**Step 1: Create useDashboard hook**

Manages widget visibility and order, persisted in localStorage:

```typescript
export interface WidgetConfig {
  id: string
  label: string
  visible: boolean
  collapsed: boolean
}

export const DEFAULT_WIDGETS: WidgetConfig[] = [
  { id: 'hand-strength', label: 'Hand Strength', visible: true, collapsed: false },
  { id: 'pot-odds', label: 'Pot Odds', visible: true, collapsed: false },
  { id: 'tournament', label: 'Tournament', visible: true, collapsed: false },
  { id: 'rank', label: 'Rank', visible: true, collapsed: false },
  { id: 'starting-hand', label: 'Starting Hand', visible: true, collapsed: false },
  { id: 'quick-actions', label: 'Quick Actions', visible: false, collapsed: false },
]

export function useDashboard() {
  // localStorage key: 'ddpoker-dashboard'
  // Returns: { widgets, toggleVisible, toggleCollapsed, moveUp, moveDown, resetDefaults }
}
```

**Step 2: Create DashboardWidget wrapper**

Collapsible section with header click to expand/collapse:

```typescript
interface DashboardWidgetProps {
  title: string
  collapsed: boolean
  onToggleCollapse: () => void
  children: React.ReactNode
}
```

**Step 3: Write tests, run, commit**

```bash
cd code/web && npx vitest run
git add code/web/lib/game/useDashboard.ts code/web/lib/game/__tests__/useDashboard.test.ts code/web/components/game/DashboardWidget.tsx
git commit -m "feat(web): add dashboard hook and collapsible widget component"
```

---

## Task 15: Dashboard Panel & Settings

**Files:**
- Create: `code/web/components/game/Dashboard.tsx`
- Create: `code/web/components/game/DashboardSettings.tsx`
- Modify: `code/web/components/game/PokerTable.tsx` — add D key + toolbar button

**Step 1: Create Dashboard component**

Right-side panel containing all visible widgets:

- Hand Strength widget: calls `evaluateHand()`, shows hand name
- Pot Odds widget: shows pot/call/odds calculation
- Tournament widget: shows level, blinds, players remaining
- Rank widget: shows player rank
- Starting Hand widget: embedded compact `StartingHandsChart`
- Quick Actions widget: compact fold/check/call buttons

Props come from game state (passed through PokerTable).

**Step 2: Create DashboardSettings dialog**

Modal showing all widgets with:
- Checkbox to toggle visible
- Up/Down buttons to reorder
- "Reset Defaults" button

**Step 3: Wire into PokerTable**

Add **D** key handler and toolbar button. Dashboard panel on the right side.

**Step 4: Write tests, run, commit**

```bash
cd code/web && npx vitest run
git add code/web/components/game/Dashboard.tsx code/web/components/game/DashboardSettings.tsx code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add configurable dashboard with 6 widgets"
```

---

## Summary

| Task | Description | New Files | Tests |
|------|-------------|-----------|-------|
| 1 | Game preferences hook | 2 | 4 |
| 2 | ThemePicker Gameplay tab | — | — |
| 3 | Four-color deck cards | 1+ images | — |
| 4 | Keyboard shortcuts enhancement | — | — |
| 5 | Dealer chat filtering | — | — |
| 6 | AI player type configuration | — | — |
| 7 | Poker math types & deck | 3 | 5 |
| 8 | Hand evaluator | 2 | 16 |
| 9 | Equity calculator | 2 | 5 |
| 10 | Starting hands chart | 3 | ~4 |
| 11 | Card picker component | 2 | ~3 |
| 12 | Poker simulator | 2 | ~3 |
| 13 | AI advisor panel | 2 | ~3 |
| 14 | Dashboard hook & widget base | 3 | ~4 |
| 15 | Dashboard panel & settings | 2 | ~3 |
| **Total** | | **~24 new** | **~50** |
