# Web Client Feature Parity Round 4 — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add practice game options, wire missing overlays, color up display, side pot details, chip count animations, and enhanced game info to the web client.

**Architecture:** All features are client-side only — no server changes. Practice options add UI controls for existing DTO fields. Overlay wiring connects existing components. Color up expands reducer state from boolean to object. Side pots and chip animations enhance existing display components.

**Tech Stack:** React 19, TypeScript 5, Framer Motion (already installed), Vitest + React Testing Library, Tailwind CSS 4

**Design doc:** `docs/plans/2026-02-27-web-client-feature-parity-r4-design.md`

---

## Task 1: Practice Game Options UI

**Files:**
- Modify: `code/web/app/games/create/page.tsx`

**Context:** The `PracticeConfigDto` type (in `types.ts:688-696`) has `aiFaceUp`, `pauseAllinInteractive`, `autoDeal`, and `zipModeEnabled` fields. The `buildConfig()` function (line 154) doesn't include `practiceConfig`. The `handlePractice()` function (line 206) calls `createPracticeGame(buildConfig(...))`. We need to add state variables for these options, show toggles when `isPractice` is true, and include them in the config.

**Step 1: Add practice option state variables**

In `code/web/app/games/create/page.tsx`, after the existing state variables (around line 68), add:

```typescript
// Practice game options
const [aiFaceUp, setAiFaceUp] = useState(false)
const [pauseAllin, setPauseAllin] = useState(false)
const [autoDeal, setAutoDeal] = useState(true)
const [zipMode, setZipMode] = useState(false)
```

**Step 2: Include practiceConfig in buildConfig**

In the `buildConfig()` function (line 163), add `practiceConfig` to the returned object:

```typescript
return {
  ...existingFields,
  practiceConfig: {
    aiFaceUp,
    pauseAllinInteractive: pauseAllin,
    autoDeal,
    zipModeEnabled: zipMode,
  },
}
```

**Step 3: Add practice options UI section**

After the AI players section and before the submit buttons, add a "Practice Options" section visible only when `isPractice`:

```tsx
{isPractice && (
  <div className="space-y-3">
    <h2 className="text-lg font-semibold">Practice Options</h2>
    <label className="flex items-center gap-3 cursor-pointer">
      <input type="checkbox" checked={aiFaceUp} onChange={(e) => setAiFaceUp(e.target.checked)}
        className="w-4 h-4 rounded" />
      <span className="text-sm">Show AI Cards</span>
      <span className="text-xs text-gray-500">Reveal AI hole cards face-up</span>
    </label>
    <label className="flex items-center gap-3 cursor-pointer">
      <input type="checkbox" checked={pauseAllin} onChange={(e) => setPauseAllin(e.target.checked)}
        className="w-4 h-4 rounded" />
      <span className="text-sm">Pause on All-In</span>
      <span className="text-xs text-gray-500">Pause before dealing all-in runout</span>
    </label>
    <label className="flex items-center gap-3 cursor-pointer">
      <input type="checkbox" checked={autoDeal} onChange={(e) => setAutoDeal(e.target.checked)}
        className="w-4 h-4 rounded" />
      <span className="text-sm">Auto Deal</span>
      <span className="text-xs text-gray-500">Automatically start next hand</span>
    </label>
    <label className="flex items-center gap-3 cursor-pointer">
      <input type="checkbox" checked={zipMode} onChange={(e) => setZipMode(e.target.checked)}
        className="w-4 h-4 rounded" />
      <span className="text-sm">Fast Mode</span>
      <span className="text-xs text-gray-500">Skip animations and delays</span>
    </label>
  </div>
)}
```

**Step 4: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/app/games/create/page.tsx
git commit -m "feat(web): add practice game options (AI face-up, pause all-in, auto deal, fast mode)"
```

---

## Task 2: Wire Never-Broke & Continue-Runout Overlays

**Files:**
- Modify: `code/web/app/games/[gameId]/play/page.tsx`

**Context:** `GameOverlay` already supports `neverBroke` and `continueRunout` types (lines 185-216 of GameOverlay.tsx). `GameContext` already provides `sendNeverBrokeDecision(accept: boolean)` and `sendContinueRunout()` actions. The reducer tracks `neverBrokeOffer` and `continueRunoutPending`. The play page just needs to render these overlays.

**Step 1: Destructure missing state fields**

On line 31 of `play/page.tsx`, add `neverBrokeOffer` and `continueRunoutPending` to the destructured state:

```typescript
const { gamePhase, lobbyState, gameState, rebuyOffer, addonOffer, eliminatedPosition, error, neverBrokeOffer, continueRunoutPending } = state
```

**Step 2: Add overlay conditions**

After the `addonOffer` check (line 98) and before the `eliminatedPosition` check (line 99), add:

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

**Step 3: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/app/games/\\[gameId\\]/play/page.tsx
git commit -m "feat(web): wire never-broke and continue-runout overlays"
```

---

## Task 3: Color Up Display

**Files:**
- Create: `code/web/components/game/ColorUpOverlay.tsx`
- Test: `code/web/components/game/__tests__/ColorUpOverlay.test.tsx`
- Modify: `code/web/lib/game/gameReducer.ts` — store full data instead of boolean
- Modify: `code/web/app/games/[gameId]/play/page.tsx` — render overlay

**Step 1: Write test**

Create `code/web/components/game/__tests__/ColorUpOverlay.test.tsx`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { ColorUpOverlay } from '../ColorUpOverlay'
import type { ColorUpStartedData } from '@/lib/game/types'

const data: ColorUpStartedData = {
  players: [
    { playerId: 1, cards: ['Ah', 'Kd'], won: true, broke: false, finalChips: 5000 },
    { playerId: 2, cards: ['3c', '2s'], won: false, broke: false, finalChips: 3000 },
    { playerId: 3, cards: ['7h', '5d'], won: false, broke: true, finalChips: 0 },
  ],
  newMinChip: 100,
  tableId: 1,
}

const seatNames: Record<number, string> = { 1: 'Alice', 2: 'Bob', 3: 'Charlie' }

describe('ColorUpOverlay', () => {
  it('shows chip race title and new minimum chip', () => {
    render(<ColorUpOverlay data={data} seatNames={seatNames} />)
    expect(screen.getByText(/chip race/i)).toBeDefined()
    expect(screen.getByText(/100/)).toBeDefined()
  })

  it('shows player results', () => {
    render(<ColorUpOverlay data={data} seatNames={seatNames} />)
    expect(screen.getByText('Alice')).toBeDefined()
    expect(screen.getByText('Bob')).toBeDefined()
    expect(screen.getByText('Charlie')).toBeDefined()
  })

  it('marks winners and broke players', () => {
    render(<ColorUpOverlay data={data} seatNames={seatNames} />)
    expect(screen.getByText('Won')).toBeDefined()
    expect(screen.getByText('Broke')).toBeDefined()
  })
})
```

**Step 2: Implement ColorUpOverlay**

Create `code/web/components/game/ColorUpOverlay.tsx`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { ColorUpStartedData } from '@/lib/game/types'
import { Card } from './Card'
import { formatChips } from '@/lib/utils'

interface ColorUpOverlayProps {
  data: ColorUpStartedData
  seatNames: Record<number, string>
}

export function ColorUpOverlay({ data, seatNames }: ColorUpOverlayProps) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-60"
      role="dialog"
      aria-modal="true"
      aria-label="Chip Race"
    >
      <div className="bg-gray-800 rounded-2xl shadow-2xl p-6 max-w-md w-full mx-4 text-white">
        <h2 className="text-xl font-bold mb-1 text-center">Chip Race</h2>
        <p className="text-gray-400 text-sm text-center mb-4">
          New minimum chip: {formatChips(data.newMinChip)}
        </p>
        <div className="space-y-2">
          {data.players.map((p) => (
            <div key={p.playerId} className="flex items-center justify-between text-sm bg-gray-700 rounded-lg px-3 py-2">
              <span className="font-medium w-24 truncate">{seatNames[p.playerId] ?? `Player ${p.playerId}`}</span>
              <div className="flex gap-0.5">
                {p.cards.map((c) => (
                  <Card key={c} card={c} width={28} />
                ))}
              </div>
              <span className={p.won ? 'text-green-400 font-semibold' : p.broke ? 'text-red-400 font-semibold' : 'text-gray-400'}>
                {p.won ? 'Won' : p.broke ? 'Broke' : '—'}
              </span>
              <span className="text-yellow-300 font-semibold w-16 text-right">{formatChips(p.finalChips)}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
```

**Step 3: Update reducer — store full ColorUpStartedData**

In `code/web/lib/game/gameReducer.ts`:

1. Change the state field from `colorUpInProgress: boolean` to `colorUpData: ColorUpStartedData | null` (in the GameState interface and initial state).

2. Update the `COLOR_UP_STARTED` handler to store the full data:
```typescript
case 'COLOR_UP_STARTED':
  return { ...state, colorUpData: message.data as ColorUpStartedData, ...seqState }
```

3. Update the `COLOR_UP_COMPLETED` handler:
```typescript
case 'COLOR_UP_COMPLETED':
  return { ...state, colorUpData: null, ...seqState }
```

4. Update initial state: `colorUpData: null`

5. Update existing tests that reference `colorUpInProgress` to use `colorUpData` instead.

**Step 4: Wire overlay in play page**

In `code/web/app/games/[gameId]/play/page.tsx`:
- Import `ColorUpOverlay`
- Destructure `colorUpData` from state
- Add to the overlay chain (after continueRunout, before eliminated):
```typescript
} else if (colorUpData) {
  const seatNames: Record<number, string> = {}
  if (gameState) {
    gameState.players.forEach((p) => { seatNames[p.playerId] = p.name })
  }
  overlay = <ColorUpOverlay data={colorUpData} seatNames={seatNames} />
}
```

**Step 5: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/components/game/ColorUpOverlay.tsx code/web/components/game/__tests__/ColorUpOverlay.test.tsx code/web/lib/game/gameReducer.ts code/web/app/games/\\[gameId\\]/play/page.tsx
git commit -m "feat(web): add color up (chip race) display overlay"
```

---

## Task 4: Enhanced Side Pot Display

**Files:**
- Modify: `code/web/components/game/PotDisplay.tsx`
- Modify: `code/web/components/game/TableFelt.tsx` — pass seats to PotDisplay
- Test: `code/web/components/game/__tests__/PotDisplay.test.tsx`

**Step 1: Write test**

Create `code/web/components/game/__tests__/PotDisplay.test.tsx`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { PotDisplay } from '../PotDisplay'
import type { PotData, SeatData } from '@/lib/game/types'

const seats: SeatData[] = [
  { seatIndex: 0, playerId: 1, playerName: 'Alice', chipCount: 5000, status: 'ACTIVE', isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
  { seatIndex: 1, playerId: 2, playerName: 'Bob', chipCount: 3000, status: 'ALL_IN', isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
  { seatIndex: 2, playerId: 3, playerName: 'Charlie', chipCount: 8000, status: 'ACTIVE', isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
]

describe('PotDisplay', () => {
  it('shows nothing when no pots', () => {
    const { container } = render(<PotDisplay pots={[]} />)
    expect(container.innerHTML).toBe('')
  })

  it('shows main pot with player count', () => {
    const pots: PotData[] = [{ amount: 1500, eligiblePlayers: [1, 2, 3] }]
    render(<PotDisplay pots={pots} seats={seats} />)
    expect(screen.getByText(/1,500/)).toBeDefined()
    expect(screen.getByText(/3 players/)).toBeDefined()
  })

  it('shows side pots with player count', () => {
    const pots: PotData[] = [
      { amount: 1500, eligiblePlayers: [1, 2, 3] },
      { amount: 800, eligiblePlayers: [1, 3] },
    ]
    render(<PotDisplay pots={pots} seats={seats} />)
    expect(screen.getByText(/Side Pot 1/)).toBeDefined()
    expect(screen.getByText(/2 players/)).toBeDefined()
  })

  it('works without seats prop', () => {
    const pots: PotData[] = [{ amount: 1000, eligiblePlayers: [1, 2] }]
    render(<PotDisplay pots={pots} />)
    expect(screen.getByText(/1,000/)).toBeDefined()
  })
})
```

**Step 2: Update PotDisplay**

Modify `code/web/components/game/PotDisplay.tsx`:

```typescript
import type { PotData, SeatData } from '@/lib/game/types'
import { formatChips } from '@/lib/utils'

interface PotDisplayProps {
  pots: PotData[]
  seats?: SeatData[]
}

export function PotDisplay({ pots, seats }: PotDisplayProps) {
  if (pots.length === 0) return null

  const mainPot = pots[0]
  const sidePots = pots.slice(1)

  function playerCount(pot: PotData): string {
    if (!seats || pot.eligiblePlayers.length === 0) return ''
    return ` (${pot.eligiblePlayers.length} player${pot.eligiblePlayers.length !== 1 ? 's' : ''})`
  }

  return (
    <div className="flex flex-col items-center gap-1">
      <div className="text-yellow-300 font-bold text-sm">
        Pot: {formatChips(mainPot.amount)}{playerCount(mainPot)}
      </div>
      {sidePots.map((pot, i) => (
        <div key={i} className="text-yellow-200 text-xs">
          Side Pot {i + 1}: {formatChips(pot.amount)}{playerCount(pot)}
        </div>
      ))}
    </div>
  )
}
```

**Step 3: Pass seats from TableFelt to PotDisplay**

In `code/web/components/game/TableFelt.tsx`, add `seats` to the props interface (from `table.seats`) and pass to PotDisplay:

```typescript
<PotDisplay pots={table.pots} seats={table.seats} />
```

**Step 4: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/components/game/PotDisplay.tsx code/web/components/game/__tests__/PotDisplay.test.tsx code/web/components/game/TableFelt.tsx
git commit -m "feat(web): show player count on side pots"
```

---

## Task 5: Chip Count Change Highlight

**Files:**
- Modify: `code/web/components/game/PlayerSeat.tsx`

**Context:** PlayerSeat already uses Framer Motion (`motion.div`). When `chipCount` changes, we want a brief color flash — green for increase, red for decrease. Use `useRef` to track the previous chip count and `useEffect` to detect changes.

**Step 1: Add chip change detection**

In `code/web/components/game/PlayerSeat.tsx`:

```typescript
import { useRef, useState, useEffect } from 'react'

// Inside the component:
const prevChipCount = useRef(seat.chipCount)
const [chipFlash, setChipFlash] = useState<'up' | 'down' | null>(null)

useEffect(() => {
  if (prevChipCount.current !== seat.chipCount && prevChipCount.current !== 0) {
    setChipFlash(seat.chipCount > prevChipCount.current ? 'up' : 'down')
    const timer = setTimeout(() => setChipFlash(null), 1000)
    prevChipCount.current = seat.chipCount
    return () => clearTimeout(timer)
  }
  prevChipCount.current = seat.chipCount
}, [seat.chipCount])
```

**Step 2: Apply flash styling to chip count display**

Find the chip count display element and add conditional classes:

```typescript
const chipFlashClass = chipFlash === 'up' ? 'text-green-400' : chipFlash === 'down' ? 'text-red-400' : 'text-yellow-300'
```

Apply `chipFlashClass` to the chip count text element, adding `transition-colors duration-300` for a smooth fade.

**Step 3: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/components/game/PlayerSeat.tsx
git commit -m "feat(web): highlight chip count changes (green up, red down)"
```

---

## Task 6: Enhanced Game Info Panel

**Files:**
- Modify: `code/web/components/game/GameInfoPanel.tsx`
- Modify: `code/web/app/games/[gameId]/play/page.tsx` — pass new props

**Step 1: Add new optional props to GameInfoPanel**

In `code/web/components/game/GameInfoPanel.tsx`, add to `GameInfoPanelProps`:

```typescript
greeting?: string
minChip?: number
rebuyInfo?: string
addonInfo?: string
```

**Step 2: Render greeting below header**

After the header section, if `greeting` is provided:

```tsx
{greeting && (
  <div className="px-4 py-2 border-b border-gray-700">
    <p className="text-sm text-gray-400 italic">{greeting}</p>
  </div>
)}
```

**Step 3: Add min chip to blinds section**

In the current blinds section, after the ante display:

```tsx
{minChip != null && minChip > 0 && (
  <div className="text-xs text-gray-400 mt-1">Min chip: {formatChips(minChip)}</div>
)}
```

**Step 4: Add tournament details row**

After the blinds section, if rebuyInfo or addonInfo are provided:

```tsx
{(rebuyInfo || addonInfo) && (
  <div className="px-4 py-2 border-b border-gray-700 space-y-0.5">
    {rebuyInfo && <div className="text-xs text-gray-400">{rebuyInfo}</div>}
    {addonInfo && <div className="text-xs text-gray-400">{addonInfo}</div>}
  </div>
)}
```

**Step 5: Pass props from play page**

In `code/web/app/games/[gameId]/play/page.tsx`, where GameInfoPanel is rendered (line 143):

```tsx
<GameInfoPanel
  gameName={gameName}
  ownerName={lobbyState?.ownerName ?? ''}
  blinds={gameState.blinds}
  currentLevel={gameState.level}
  greeting={lobbyState?.greeting}
  players={...}
  onClose={() => setInfoOpen(false)}
/>
```

Note: `greeting` comes from `lobbyState` which contains the game config. Check if `lobbyState` has a `greeting` field. If not, this prop will just be undefined and the section won't render — which is fine.

**Step 6: Run tests, commit**

```bash
cd code/web && npx vitest run
git add code/web/components/game/GameInfoPanel.tsx code/web/app/games/\\[gameId\\]/play/page.tsx
git commit -m "feat(web): enhance game info with greeting, min chip, and rebuy/addon status"
```

---

## Summary

| Task | Description | New Files | Tests |
|------|-------------|-----------|-------|
| 1 | Practice game options UI | — | — |
| 2 | Wire never-broke & continue-runout overlays | — | — |
| 3 | Color up display overlay | 2 | 3 |
| 4 | Enhanced side pot display | 1 (test) | 4 |
| 5 | Chip count change highlight | — | — |
| 6 | Enhanced game info panel | — | — |
| **Total** | | **3 new** | **7** |
