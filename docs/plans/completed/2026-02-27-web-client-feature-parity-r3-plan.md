# Web Client Feature Parity Round 3 — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add table felt themes, card animations (Framer Motion), card back designs, player avatars, blind presets, scheduled breaks, hand rankings reference, and visual hand replay to the web client.

**Architecture:** Client-side preferences (theme, card back, avatar) stored in localStorage via React hooks. Framer Motion powers card animations and hand replay. Blind presets and scheduled breaks enhance the existing game creation form. Hand replay reuses existing table components with a state machine for playback.

**Tech Stack:** React 19, TypeScript 5, Framer Motion, Vitest + React Testing Library, Tailwind CSS 4

**Design doc:** `docs/plans/2026-02-27-web-client-feature-parity-r3-design.md`

---

## Phase 1: Theme Infrastructure

### Task 1: Theme Definitions + useTheme Hook

**Files:**
- Create: `code/web/lib/theme/themes.ts`
- Create: `code/web/lib/theme/useTheme.ts`
- Test: `code/web/lib/theme/__tests__/useTheme.test.ts`

**Step 1: Write the test**

Create `code/web/lib/theme/__tests__/useTheme.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useTheme } from '../useTheme'
import { THEMES } from '../themes'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

describe('useTheme', () => {
  it('defaults to classic-green', () => {
    const { result } = renderHook(() => useTheme())
    expect(result.current.themeId).toBe('classic-green')
    expect(result.current.colors.center).toBe('#2d5a1b')
  })

  it('switches theme and persists', () => {
    const { result } = renderHook(() => useTheme())
    act(() => result.current.setTheme('royal-blue'))
    expect(result.current.themeId).toBe('royal-blue')
    expect(store['ddpoker-theme']).toBe('royal-blue')
  })

  it('loads saved theme from localStorage', () => {
    store['ddpoker-theme'] = 'casino-red'
    const { result } = renderHook(() => useTheme())
    expect(result.current.themeId).toBe('casino-red')
  })

  it('falls back to default for invalid saved theme', () => {
    store['ddpoker-theme'] = 'nonexistent'
    const { result } = renderHook(() => useTheme())
    expect(result.current.themeId).toBe('classic-green')
  })

  it('exports all available themes', () => {
    expect(Object.keys(THEMES).length).toBe(5)
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/theme/__tests__/useTheme.test.ts`

**Step 3: Write the implementations**

Create `code/web/lib/theme/themes.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

export interface ThemeColors {
  center: string
  mid: string
  edge: string
  border: string
}

export interface Theme {
  id: string
  name: string
  colors: ThemeColors
}

export const THEMES: Record<string, Theme> = {
  'classic-green': {
    id: 'classic-green',
    name: 'Classic Green',
    colors: { center: '#2d5a1b', mid: '#1e3d12', edge: '#152d0d', border: '#1a2e0f' },
  },
  'royal-blue': {
    id: 'royal-blue',
    name: 'Royal Blue',
    colors: { center: '#1b3d5a', mid: '#122d3e', edge: '#0d1f2d', border: '#0f1e2e' },
  },
  'casino-red': {
    id: 'casino-red',
    name: 'Casino Red',
    colors: { center: '#5a1b1b', mid: '#3d1212', edge: '#2d0d0d', border: '#2e0f0f' },
  },
  'dark-night': {
    id: 'dark-night',
    name: 'Dark Night',
    colors: { center: '#2a2a3a', mid: '#1e1e2e', edge: '#141420', border: '#1a1a28' },
  },
  'wooden': {
    id: 'wooden',
    name: 'Wooden',
    colors: { center: '#5a3d1b', mid: '#3d2a12', edge: '#2d1f0d', border: '#2e1a0f' },
  },
}

export const DEFAULT_THEME_ID = 'classic-green'
```

Create `code/web/lib/theme/useTheme.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'
import { THEMES, DEFAULT_THEME_ID } from './themes'
import type { ThemeColors } from './themes'

const STORAGE_KEY = 'ddpoker-theme'

function loadThemeId(): string {
  try {
    const id = localStorage.getItem(STORAGE_KEY)
    if (id && THEMES[id]) return id
  } catch { /* ignore */ }
  return DEFAULT_THEME_ID
}

export function useTheme(): {
  themeId: string
  colors: ThemeColors
  setTheme: (id: string) => void
} {
  const [themeId, setThemeId] = useState(loadThemeId)

  const setTheme = useCallback((id: string) => {
    if (!THEMES[id]) return
    setThemeId(id)
    try { localStorage.setItem(STORAGE_KEY, id) } catch { /* ignore */ }
  }, [])

  return { themeId, colors: THEMES[themeId]?.colors ?? THEMES[DEFAULT_THEME_ID].colors, setTheme }
}
```

**Step 4: Run tests, verify pass**

Run: `cd code/web && npx vitest run lib/theme/__tests__/useTheme.test.ts`

**Step 5: Commit**

```bash
git add code/web/lib/theme/
git commit -m "feat(web): add table felt theme definitions and useTheme hook"
```

---

### Task 2: TableFelt Theme Integration

**Files:**
- Modify: `code/web/components/game/TableFelt.tsx`

**Context:** Currently `TableFelt.tsx` has hardcoded green hex values. Change it to accept theme colors as a prop (passed from PokerTable which uses `useTheme()`).

**Step 1: Add `colors` prop to TableFelt**

Modify `code/web/components/game/TableFelt.tsx` — add a `colors` prop with the same shape as `ThemeColors`, defaulting to classic green for backwards compat:

```typescript
import type { ThemeColors } from '@/lib/theme/themes'

interface TableFeltProps {
  table: TableData
  colors?: ThemeColors
}

export function TableFelt({ table, colors }: TableFeltProps) {
  const c = colors ?? { center: '#2d5a1b', mid: '#1e3d12', edge: '#152d0d', border: '#1a2e0f' }
  return (
    <div
      className="absolute inset-x-[8%] inset-y-[15%] rounded-[50%] flex flex-col items-center justify-center gap-4"
      style={{
        background: `radial-gradient(ellipse at center, ${c.center} 0%, ${c.mid} 60%, ${c.edge} 100%)`,
        boxShadow: 'inset 0 0 80px rgba(0,0,0,0.5), 0 0 40px rgba(0,0,0,0.4)',
        border: `4px solid ${c.border}`,
      }}
      role="region"
      aria-label="Poker table felt"
    >
      <CommunityCards cards={table.communityCards} />
      <PotDisplay pots={table.pots} />
    </div>
  )
}
```

**Step 2: Wire theme in PokerTable**

Modify `code/web/components/game/PokerTable.tsx`:
- Add import: `import { useTheme } from '@/lib/theme/useTheme'`
- Inside component, add: `const { colors: feltColors } = useTheme()`
- Pass to TableFelt: `<TableFelt table={currentTable} colors={feltColors} />`

**Step 3: Run tests, verify nothing broke**

Run: `cd code/web && npx vitest run`

**Step 4: Commit**

```bash
git add code/web/components/game/TableFelt.tsx code/web/components/game/PokerTable.tsx
git commit -m "feat(web): apply theme colors to table felt"
```

---

### Task 3: Card Back Designs + useCardBack Hook

**Files:**
- Create: `code/web/components/game/cardBacks.tsx` — 4 SVG card back components
- Create: `code/web/lib/theme/useCardBack.ts`
- Test: `code/web/lib/theme/__tests__/useCardBack.test.ts`
- Modify: `code/web/components/game/Card.tsx` — use selected card back

**Step 1: Write useCardBack test**

Create `code/web/lib/theme/__tests__/useCardBack.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useCardBack, CARD_BACK_IDS } from '../useCardBack'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

describe('useCardBack', () => {
  it('defaults to classic-red', () => {
    const { result } = renderHook(() => useCardBack())
    expect(result.current.cardBackId).toBe('classic-red')
  })

  it('switches card back and persists', () => {
    const { result } = renderHook(() => useCardBack())
    act(() => result.current.setCardBack('blue-diamond'))
    expect(result.current.cardBackId).toBe('blue-diamond')
    expect(store['ddpoker-card-back']).toBe('blue-diamond')
  })

  it('has 4 card back options', () => {
    expect(CARD_BACK_IDS.length).toBe(4)
  })
})
```

**Step 2: Write useCardBack hook**

Create `code/web/lib/theme/useCardBack.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'

export const CARD_BACK_IDS = ['classic-red', 'blue-diamond', 'green-celtic', 'gold-royal'] as const
export type CardBackId = (typeof CARD_BACK_IDS)[number]

const STORAGE_KEY = 'ddpoker-card-back'
const DEFAULT_ID: CardBackId = 'classic-red'

function load(): CardBackId {
  try {
    const id = localStorage.getItem(STORAGE_KEY) as CardBackId
    if (CARD_BACK_IDS.includes(id)) return id
  } catch { /* ignore */ }
  return DEFAULT_ID
}

export function useCardBack() {
  const [cardBackId, setId] = useState<CardBackId>(load)

  const setCardBack = useCallback((id: CardBackId) => {
    if (!CARD_BACK_IDS.includes(id)) return
    setId(id)
    try { localStorage.setItem(STORAGE_KEY, id) } catch { /* ignore */ }
  }, [])

  return { cardBackId, setCardBack }
}
```

**Step 3: Create card back SVG components**

Create `code/web/components/game/cardBacks.tsx` — 4 inline SVG card back designs. Each is a function component accepting `width` and `height` props. Use simple geometric patterns (diamond grid, celtic knot, crown) rendered as SVG paths. The designs should match the 200x280 aspect ratio of the cards.

**Step 4: Modify Card.tsx to render SVG card backs**

In `Card.tsx`, when card is face-down, render the selected card back SVG component instead of `card_blank.png`. Accept an optional `cardBackId` prop (default: `'classic-red'`). PokerTable passes the cardBackId from `useCardBack()` down.

**Step 5: Run tests, commit**

```bash
git add code/web/lib/theme/useCardBack.ts code/web/lib/theme/__tests__/useCardBack.test.ts code/web/components/game/cardBacks.tsx code/web/components/game/Card.tsx
git commit -m "feat(web): add card back designs with 4 built-in options"
```

---

### Task 4: Avatar Icons + useAvatar Hook

**Files:**
- Create: `code/web/components/game/avatarIcons.tsx` — 12 SVG avatar icons
- Create: `code/web/lib/theme/useAvatar.ts`
- Test: `code/web/lib/theme/__tests__/useAvatar.test.ts`
- Modify: `code/web/components/game/PlayerSeat.tsx` — show avatar circle

**Step 1: Write useAvatar test**

Create `code/web/lib/theme/__tests__/useAvatar.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAvatar, AVATAR_IDS } from '../useAvatar'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

describe('useAvatar', () => {
  it('defaults to spade', () => {
    const { result } = renderHook(() => useAvatar())
    expect(result.current.avatarId).toBe('spade')
  })

  it('switches avatar and persists', () => {
    const { result } = renderHook(() => useAvatar())
    act(() => result.current.setAvatar('crown'))
    expect(result.current.avatarId).toBe('crown')
    expect(store['ddpoker-avatar']).toBe('crown')
  })

  it('has 12 avatar options', () => {
    expect(AVATAR_IDS.length).toBe(12)
  })
})
```

**Step 2: Write useAvatar hook**

Create `code/web/lib/theme/useAvatar.ts` — same localStorage pattern as useCardBack but with 12 avatar IDs: `bear`, `eagle`, `fox`, `wolf`, `shark`, `owl`, `crown`, `diamond`, `spade`, `star`, `flame`, `lightning`. Default: `spade`.

**Step 3: Create avatar SVG icons**

Create `code/web/components/game/avatarIcons.tsx` — 12 simple SVG icons, each 28x28px. Export a `AVATARS` map from ID to component, plus an `AvatarIcon` component that takes `id` and `size` props and renders the right SVG.

**Step 4: Add avatar to PlayerSeat**

Modify `code/web/components/game/PlayerSeat.tsx`:
- Add optional `avatarId` prop (string)
- Before the player name, render a 28px circular div with gray-600 bg containing the AvatarIcon
- For the local player's seat, PokerTable passes the selected avatarId
- For other players, show a default silhouette icon

**Step 5: Run tests, commit**

```bash
git add code/web/lib/theme/useAvatar.ts code/web/lib/theme/__tests__/useAvatar.test.ts code/web/components/game/avatarIcons.tsx code/web/components/game/PlayerSeat.tsx
git commit -m "feat(web): add player avatars with 12 built-in icons"
```

---

### Task 5: ThemePicker Component

**Files:**
- Create: `code/web/components/game/ThemePicker.tsx`
- Test: `code/web/components/game/__tests__/ThemePicker.test.tsx`

**Context:** Unified settings popover with 3 tabs: Table Theme (5 colored circles), Card Back (4 mini cards), Avatar (4x3 grid of icons). Positioned near VolumeControl in bottom-left.

**Step 1: Write ThemePicker test**

Create `code/web/components/game/__tests__/ThemePicker.test.tsx`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ThemePicker } from '../ThemePicker'

vi.mock('@/lib/theme/useTheme', () => ({
  useTheme: () => ({ themeId: 'classic-green', colors: { center: '#2d5a1b', mid: '#1e3d12', edge: '#152d0d', border: '#1a2e0f' }, setTheme: vi.fn() }),
}))
vi.mock('@/lib/theme/useCardBack', () => ({
  useCardBack: () => ({ cardBackId: 'classic-red', setCardBack: vi.fn() }),
  CARD_BACK_IDS: ['classic-red', 'blue-diamond', 'green-celtic', 'gold-royal'],
}))
vi.mock('@/lib/theme/useAvatar', () => ({
  useAvatar: () => ({ avatarId: 'spade', setAvatar: vi.fn() }),
  AVATAR_IDS: ['bear', 'eagle', 'fox', 'wolf', 'shark', 'owl', 'crown', 'diamond', 'spade', 'star', 'flame', 'lightning'],
}))

describe('ThemePicker', () => {
  it('renders a settings button', () => {
    render(<ThemePicker />)
    expect(screen.getByRole('button', { name: /settings/i })).toBeDefined()
  })

  it('opens popover on click', () => {
    render(<ThemePicker />)
    fireEvent.click(screen.getByRole('button', { name: /settings/i }))
    expect(screen.getByText('Table')).toBeDefined()
  })

  it('shows theme options when Table tab active', () => {
    render(<ThemePicker />)
    fireEvent.click(screen.getByRole('button', { name: /settings/i }))
    expect(screen.getByLabelText(/classic green/i)).toBeDefined()
  })
})
```

**Step 2: Implement ThemePicker**

Create `code/web/components/game/ThemePicker.tsx` — a button (gear/palette icon) that toggles a popover. The popover has 3 tabs: "Table", "Cards", "Avatar". Each tab shows the relevant options. Click-away closes the popover.

**Step 3: Integrate into PokerTable**

Add ThemePicker to the bottom-left toolbar alongside VolumeControl and sit-out button.

**Step 4: Run tests, commit**

```bash
git add code/web/components/game/ThemePicker.tsx code/web/components/game/__tests__/ThemePicker.test.tsx code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add ThemePicker for table, card back, and avatar settings"
```

---

## Phase 2: Card Animations

### Task 6: Install Framer Motion + Animate CommunityCards

**Files:**
- Modify: `code/web/package.json` — add `framer-motion`
- Modify: `code/web/components/game/CommunityCards.tsx` — AnimatePresence + stagger

**Step 1: Install framer-motion**

Run: `cd code/web && npm install framer-motion`

**Step 2: Animate CommunityCards**

Modify `code/web/components/game/CommunityCards.tsx`:

```tsx
'use client'

import { motion, AnimatePresence } from 'framer-motion'
import { Card } from './Card'

interface CommunityCardsProps {
  cards: string[]
  cardWidth?: number
  cardBackId?: string
}

export function CommunityCards({ cards, cardWidth = 65, cardBackId }: CommunityCardsProps) {
  if (cards.length === 0) return null

  return (
    <div className="flex gap-1 items-center justify-center" role="region" aria-label="Community cards">
      <AnimatePresence>
        {cards.map((card, i) => (
          <motion.div
            key={card}
            initial={{ opacity: 0, y: -20, scale: 0.8 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.3, delay: i < 3 ? i * 0.15 : 0 }}
          >
            <Card card={card} width={cardWidth} cardBackId={cardBackId} />
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  )
}
```

**Step 3: Run tests, commit**

```bash
git add code/web/package.json code/web/package-lock.json code/web/components/game/CommunityCards.tsx
git commit -m "feat(web): add community card deal animations with Framer Motion"
```

---

### Task 7: PlayerSeat Animations

**Files:**
- Modify: `code/web/components/game/PlayerSeat.tsx`

**Context:** Add subtle animations to PlayerSeat:
- Hole cards: fade in with slight y-offset (200ms staggered)
- Winning hand: pulse glow (yellow ring, 2 cycles)
- Use `motion.div` from framer-motion

The seat already has `animate-pulse` on `isCurrentActor`. Add a `isWinner` prop that triggers a yellow glow animation when the player wins a hand.

**Step 1: Modify PlayerSeat**

- Add `'use client'` directive and import `{ motion }` from `framer-motion`
- Wrap hole card rendering in `motion.div` with fade-in animation
- Add optional `isWinner` prop that triggers a ring glow animation

**Step 2: Run tests, commit**

```bash
git add code/web/components/game/PlayerSeat.tsx
git commit -m "feat(web): add hole card deal and winner glow animations"
```

---

## Phase 3: Game Creation

### Task 8: Blind Structure Presets

**Files:**
- Create: `code/web/lib/game/blindPresets.ts`
- Test: `code/web/lib/game/__tests__/blindPresets.test.ts`
- Modify: `code/web/app/games/create/page.tsx`

**Step 1: Write the test**

Create `code/web/lib/game/__tests__/blindPresets.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { BLIND_PRESETS } from '../blindPresets'

describe('blindPresets', () => {
  it('has 3 presets: turbo, standard, deep-stack', () => {
    expect(BLIND_PRESETS).toHaveLength(3)
    expect(BLIND_PRESETS.map((p) => p.id)).toEqual(['turbo', 'standard', 'deep-stack'])
  })

  it('turbo has 8 levels with 3 min each', () => {
    const turbo = BLIND_PRESETS.find((p) => p.id === 'turbo')!
    expect(turbo.levels).toHaveLength(8)
    expect(turbo.levels[0].minutes).toBe(3)
  })

  it('standard has 10 levels with 5 min each', () => {
    const standard = BLIND_PRESETS.find((p) => p.id === 'standard')!
    expect(standard.levels).toHaveLength(10)
    expect(standard.levels[0].minutes).toBe(5)
  })

  it('deep-stack has 12 levels with 8 min each', () => {
    const deep = BLIND_PRESETS.find((p) => p.id === 'deep-stack')!
    expect(deep.levels).toHaveLength(12)
    expect(deep.levels[0].minutes).toBe(8)
  })

  it('all presets have increasing blinds', () => {
    for (const preset of BLIND_PRESETS) {
      for (let i = 1; i < preset.levels.length; i++) {
        expect(preset.levels[i].bigBlind).toBeGreaterThanOrEqual(preset.levels[i - 1].bigBlind)
      }
    }
  })
})
```

**Step 2: Write the implementation**

Create `code/web/lib/game/blindPresets.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { GameConfigDto } from './types'

type BlindLevel = GameConfigDto['blindStructure'][number]

export interface BlindPreset {
  id: string
  name: string
  description: string
  levels: BlindLevel[]
}

function level(sb: number, bb: number, ante: number, minutes: number): BlindLevel {
  return { smallBlind: sb, bigBlind: bb, ante, minutes, isBreak: false, gameType: 'NOLIMIT_HOLDEM' }
}

export const BLIND_PRESETS: BlindPreset[] = [
  {
    id: 'turbo',
    name: 'Turbo',
    description: '8 levels, 3 min each — fast-paced games',
    levels: [
      level(25, 50, 0, 3), level(50, 100, 0, 3), level(100, 200, 25, 3), level(150, 300, 50, 3),
      level(200, 400, 50, 3), level(300, 600, 75, 3), level(500, 1000, 100, 3), level(1000, 2000, 200, 3),
    ],
  },
  {
    id: 'standard',
    name: 'Standard',
    description: '10 levels, 5 min each — balanced play',
    levels: [
      level(25, 50, 0, 5), level(50, 100, 0, 5), level(75, 150, 25, 5), level(100, 200, 25, 5),
      level(150, 300, 50, 5), level(200, 400, 50, 5), level(300, 600, 75, 5), level(400, 800, 100, 5),
      level(600, 1200, 200, 5), level(800, 1600, 200, 5),
    ],
  },
  {
    id: 'deep-stack',
    name: 'Deep Stack',
    description: '12 levels, 8 min each — slower, deeper strategy',
    levels: [
      level(10, 20, 0, 8), level(15, 30, 0, 8), level(25, 50, 0, 8), level(50, 100, 10, 8),
      level(75, 150, 20, 8), level(100, 200, 25, 8), level(150, 300, 40, 8), level(200, 400, 50, 8),
      level(250, 500, 75, 8), level(300, 600, 100, 8), level(400, 800, 100, 8), level(500, 1000, 150, 8),
    ],
  },
]
```

**Step 3: Add preset dropdown to game creation form**

Modify `code/web/app/games/create/page.tsx`:
- Import `BLIND_PRESETS` from `@/lib/game/blindPresets`
- Add a `<select>` dropdown above the blind structure table with options: "Turbo", "Standard", "Deep Stack", "Custom"
- On preset select, replace `blindStructure` state with the preset's levels
- Default to "Standard" (which matches the current `DEFAULT_BLIND_STRUCTURE`)

**Step 4: Run tests, commit**

```bash
git add code/web/lib/game/blindPresets.ts code/web/lib/game/__tests__/blindPresets.test.ts code/web/app/games/create/page.tsx
git commit -m "feat(web): add blind structure presets (Turbo, Standard, Deep Stack)"
```

---

### Task 9: Scheduled Breaks UI

**Files:**
- Modify: `code/web/app/games/create/page.tsx`
- Modify: `code/web/components/game/TournamentInfoBar.tsx`

**Context:** The blind structure type already has `isBreak: boolean`. We just need UI to add break levels and display them differently.

**Step 1: Add "Add Break" button to game creation**

In `code/web/app/games/create/page.tsx`:
- Add an `addBreakLevel()` function that appends `{ smallBlind: 0, bigBlind: 0, ante: 0, minutes: 5, isBreak: true, gameType: 'NOLIMIT_HOLDEM' }` to the structure
- Render break rows with blue background (`bg-blue-900`) and only show the "Duration" field (not blinds/ante)
- Show "BREAK" label in the first column

**Step 2: Enhance TournamentInfoBar**

In `code/web/components/game/TournamentInfoBar.tsx`:
- Accept optional `blindStructure` prop (the full structure array) and current `level` index
- Scan ahead from current level to find the next break
- If a break is coming, show "Break in N levels" in the info bar

**Step 3: Run tests, commit**

```bash
git add code/web/app/games/create/page.tsx code/web/components/game/TournamentInfoBar.tsx
git commit -m "feat(web): add scheduled breaks UI in blind structure editor"
```

---

## Phase 4: Reference & Replay

### Task 10: Hand Rankings Reference

**Files:**
- Create: `code/web/components/game/HandRankings.tsx`
- Test: `code/web/components/game/__tests__/HandRankings.test.tsx`
- Modify: `code/web/components/game/PokerTable.tsx` — H key handler

**Step 1: Write test**

Create `code/web/components/game/__tests__/HandRankings.test.tsx`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { HandRankings } from '../HandRankings'

describe('HandRankings', () => {
  it('renders all 10 poker hand rankings', () => {
    render(<HandRankings onClose={() => {}} />)
    expect(screen.getByText('Royal Flush')).toBeDefined()
    expect(screen.getByText('Straight Flush')).toBeDefined()
    expect(screen.getByText('Four of a Kind')).toBeDefined()
    expect(screen.getByText('Full House')).toBeDefined()
    expect(screen.getByText('Flush')).toBeDefined()
    expect(screen.getByText('Straight')).toBeDefined()
    expect(screen.getByText('Three of a Kind')).toBeDefined()
    expect(screen.getByText('Two Pair')).toBeDefined()
    expect(screen.getByText('One Pair')).toBeDefined()
    expect(screen.getByText('High Card')).toBeDefined()
  })

  it('displays example cards for each ranking', () => {
    render(<HandRankings onClose={() => {}} />)
    // Each ranking should show card images
    const images = screen.getAllByRole('img')
    expect(images.length).toBeGreaterThanOrEqual(50) // 10 rankings × 5 cards
  })
})
```

**Step 2: Implement HandRankings**

Create `code/web/components/game/HandRankings.tsx`:

A modal overlay component that shows all 10 poker hand rankings from strongest to weakest. Each ranking includes:
- Rank number (1-10)
- Name (e.g., "Royal Flush")
- Brief description (e.g., "A, K, Q, J, 10 of the same suit")
- 5 example cards rendered using the `Card` component at small size (width=35)

The rankings data is a static array with the hand name, description, and 5 card codes for the example.

Props: `onClose: () => void` — called when clicking backdrop or pressing Escape.

**Step 3: Wire into PokerTable**

In `PokerTable.tsx`:
- Add `[showHandRankings, setShowHandRankings]` state
- Add keyboard handler for 'h' key to toggle the panel
- Render `{showHandRankings && <HandRankings onClose={() => setShowHandRankings(false)} />}`

**Step 4: Run tests, commit**

```bash
git add code/web/components/game/HandRankings.tsx code/web/components/game/__tests__/HandRankings.test.tsx code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add hand rankings reference panel (H key)"
```

---

### Task 11: Hand Replay State Hook

**Files:**
- Create: `code/web/lib/game/useHandReplayState.ts`
- Test: `code/web/lib/game/__tests__/useHandReplayState.test.ts`

**Context:** This hook takes a hand's history entries and builds a step-by-step timeline. Each step represents a visual state: cards visible, actions shown, pot amounts. The hook manages playback (current step, play/pause, speed).

**Step 1: Write tests**

Create `code/web/lib/game/__tests__/useHandReplayState.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useHandReplayState } from '../useHandReplayState'
import type { HandHistoryEntry } from '../gameReducer'

function e(overrides: Partial<HandHistoryEntry>): HandHistoryEntry {
  return { id: String(Math.random()), handNumber: 1, type: 'action', timestamp: Date.now(), ...overrides }
}

const sampleEntries: HandHistoryEntry[] = [
  e({ type: 'hand_start', handNumber: 1 }),
  e({ type: 'action', playerName: 'Alice', action: 'CALL', amount: 50 }),
  e({ type: 'action', playerName: 'Bob', action: 'RAISE', amount: 150 }),
  e({ type: 'community', round: 'Flop', cards: ['Ah', 'Kd', '3c'] }),
  e({ type: 'action', playerName: 'Alice', action: 'CHECK' }),
  e({ type: 'action', playerName: 'Bob', action: 'BET', amount: 200 }),
  e({ type: 'community', round: 'Turn', cards: ['Ah', 'Kd', '3c', '7s'] }),
  e({ type: 'result', winners: [{ playerName: 'Bob', amount: 600, hand: 'Pair of Kings' }] }),
]

describe('useHandReplayState', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('initializes at step 0', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    expect(result.current.currentStep).toBe(0)
    expect(result.current.totalSteps).toBe(sampleEntries.length)
  })

  it('advances to next step', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.next())
    expect(result.current.currentStep).toBe(1)
  })

  it('goes back to previous step', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.next())
    act(() => result.current.next())
    act(() => result.current.prev())
    expect(result.current.currentStep).toBe(1)
  })

  it('does not go below step 0', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.prev())
    expect(result.current.currentStep).toBe(0)
  })

  it('does not go past last step', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    for (let i = 0; i < 20; i++) act(() => result.current.next())
    expect(result.current.currentStep).toBe(sampleEntries.length - 1)
  })

  it('returns visible entries up to current step', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.next()) // step 1
    act(() => result.current.next()) // step 2
    expect(result.current.visibleEntries).toHaveLength(3) // steps 0,1,2
  })

  it('derives community cards from visible entries', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    // Advance to the flop (step 3)
    act(() => { result.current.next(); result.current.next(); result.current.next(); result.current.next() })
    expect(result.current.communityCards).toEqual(['Ah', 'Kd', '3c'])
  })

  it('auto-plays at set speed', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.play())
    expect(result.current.isPlaying).toBe(true)
    act(() => { vi.advanceTimersByTime(1000) })
    expect(result.current.currentStep).toBe(1)
  })

  it('pauses auto-play', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.play())
    act(() => result.current.pause())
    expect(result.current.isPlaying).toBe(false)
  })
})
```

**Step 2: Implement the hook**

Create `code/web/lib/game/useHandReplayState.ts`:

The hook manages:
- `currentStep` (0 to entries.length - 1)
- `next()`, `prev()`, `play()`, `pause()`, `setSpeed(multiplier)`
- `isPlaying` flag, `speed` (1, 2, or 4)
- `visibleEntries` — `entries.slice(0, currentStep + 1)`
- `communityCards` — derived from the latest `type: 'community'` entry in visibleEntries
- Auto-play uses `setInterval` at `1000/speed` ms, advancing one step each tick
- Auto-play stops at the end

**Step 3: Run tests, commit**

```bash
git add code/web/lib/game/useHandReplayState.ts code/web/lib/game/__tests__/useHandReplayState.test.ts
git commit -m "feat(web): add useHandReplayState hook for hand replay playback"
```

---

### Task 12: Hand Replay Component

**Files:**
- Create: `code/web/components/game/HandReplay.tsx`
- Test: `code/web/components/game/__tests__/HandReplay.test.tsx`
- Modify: `code/web/components/game/HandHistory.tsx` — clickable hand numbers

**Step 1: Write test**

Create `code/web/components/game/__tests__/HandReplay.test.tsx`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { HandReplay } from '../HandReplay'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

function e(overrides: Partial<HandHistoryEntry>): HandHistoryEntry {
  return { id: String(Math.random()), handNumber: 1, type: 'action', timestamp: Date.now(), ...overrides }
}

const entries: HandHistoryEntry[] = [
  e({ type: 'hand_start', handNumber: 1 }),
  e({ type: 'action', playerName: 'Alice', action: 'CALL', amount: 50 }),
  e({ type: 'community', round: 'Flop', cards: ['Ah', 'Kd', '3c'] }),
  e({ type: 'result', winners: [{ playerName: 'Alice', amount: 100, hand: 'Pair' }] }),
]

describe('HandReplay', () => {
  it('renders playback controls', () => {
    render(<HandReplay entries={entries} onClose={() => {}} />)
    expect(screen.getByLabelText(/next/i)).toBeDefined()
    expect(screen.getByLabelText(/previous/i)).toBeDefined()
  })

  it('shows hand number in title', () => {
    render(<HandReplay entries={entries} onClose={() => {}} />)
    expect(screen.getByText(/hand #1/i)).toBeDefined()
  })

  it('advances step on next click', () => {
    render(<HandReplay entries={entries} onClose={() => {}} />)
    fireEvent.click(screen.getByLabelText(/next/i))
    // After advancing, should show the action text
    expect(screen.getByText(/alice/i)).toBeDefined()
  })

  it('calls onClose when close button clicked', () => {
    const onClose = vi.fn()
    render(<HandReplay entries={entries} onClose={onClose} />)
    fireEvent.click(screen.getByLabelText(/close/i))
    expect(onClose).toHaveBeenCalledOnce()
  })
})
```

**Step 2: Implement HandReplay**

Create `code/web/components/game/HandReplay.tsx`:

A modal overlay component that provides visual hand replay:
- Uses `useHandReplayState(entries)` for playback state
- Renders a compact table area (300x200px) with:
  - Community cards from `state.communityCards` using `CommunityCards` component
  - Action log showing `visibleEntries` as text lines
  - Winner highlight when a `type: 'result'` entry is visible
- Playback controls bar: Previous | Play/Pause | Next | Speed (1x/2x/4x)
- Progress bar: thin horizontal bar showing `currentStep / totalSteps`
- Close button (X) in top-right corner

Props: `entries: HandHistoryEntry[]`, `onClose: () => void`

**Step 3: Make hand numbers clickable in HandHistory**

Modify `code/web/components/game/HandHistory.tsx`:
- Add optional `onReplayHand?: (handNumber: number) => void` prop
- For `hand_start` entries, wrap the text in a clickable button that calls `onReplayHand(entry.handNumber)`
- Style as subtle link (underline on hover)

**Step 4: Wire into PokerTable**

In `PokerTable.tsx`:
- Add `[replayHand, setReplayHand]` state (number | null)
- Pass `onReplayHand={setReplayHand}` to HandHistory
- When `replayHand` is set, filter `state.handHistory` for that handNumber and render `<HandReplay entries={filtered} onClose={() => setReplayHand(null)} />`

**Step 5: Run all tests, commit**

```bash
git add code/web/components/game/HandReplay.tsx code/web/components/game/__tests__/HandReplay.test.tsx code/web/components/game/HandHistory.tsx code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add visual hand replay with step-through playback"
```

---

## Summary

| Task | Phase | Description | New Files | Tests |
|------|-------|-------------|-----------|-------|
| 1 | Theme | Theme definitions + useTheme | 3 | 5 |
| 2 | Theme | TableFelt theme integration | — | — |
| 3 | Theme | Card backs + useCardBack | 3 | 3 |
| 4 | Theme | Avatars + useAvatar | 3 | 3 |
| 5 | Theme | ThemePicker component | 2 | 3 |
| 6 | Anim | Framer Motion + CommunityCards | — | — |
| 7 | Anim | PlayerSeat animations | — | — |
| 8 | Create | Blind presets | 2 | 5 |
| 9 | Create | Scheduled breaks | — | — |
| 10 | Ref | Hand rankings | 2 | 2 |
| 11 | Replay | useHandReplayState | 2 | 9 |
| 12 | Replay | HandReplay component | 2 | 4 |
| **Total** | | | **19 new** | **34** |
