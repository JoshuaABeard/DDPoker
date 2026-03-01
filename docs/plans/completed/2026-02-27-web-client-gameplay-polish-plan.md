# Web Client Core Gameplay Polish — Implementation Plan

**Status:** COMPLETED (2026-02-28)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add 6 missing gameplay features to the web client: admin controls UI, expanded observer list, chat mute, persistent chip standings, betting hotkey improvements, and hand history export.

**Architecture:** Each feature is independent and testable in isolation. Features build on existing game state, actions, and WebSocket messages that are already wired up. No new server-side changes needed — all work is frontend-only (TypeScript/React). Chat mute uses localStorage for persistence.

**Tech Stack:** TypeScript, React 19, Next.js 16, Vitest, React Testing Library, Tailwind CSS 4.

**Design doc:** `docs/plans/2026-02-27-web-client-gameplay-polish-design.md`

---

## Phase 1: Admin Controls UI

### Task 1.1: Add `isOwner` to Game State

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts`
- Test: `code/web/lib/game/__tests__/gameReducer.test.ts`

**Step 1: Write the failing test**

Add to the test file:

```typescript
describe('isOwner', () => {
  it('sets isOwner true when myPlayerId matches lobby ownerProfileId', () => {
    let state = gameReducer(initialGameState, { type: 'SERVER_MESSAGE', message: connected(42) })
    state = gameReducer(state, {
      type: 'SERVER_MESSAGE',
      message: msg('LOBBY_STATE', {
        gameId: 'game-1',
        name: 'Test Game',
        hostingType: 'PUBLIC',
        ownerName: 'Alice',
        ownerProfileId: 42,
        maxPlayers: 10,
        isPrivate: false,
        players: [],
        blinds: { smallBlind: 50, bigBlind: 100, ante: 0 },
      }),
    })
    expect(state.isOwner).toBe(true)
  })

  it('sets isOwner false when myPlayerId does not match ownerProfileId', () => {
    let state = gameReducer(initialGameState, { type: 'SERVER_MESSAGE', message: connected(42) })
    state = gameReducer(state, {
      type: 'SERVER_MESSAGE',
      message: msg('LOBBY_STATE', {
        gameId: 'game-1',
        name: 'Test Game',
        hostingType: 'PUBLIC',
        ownerName: 'Bob',
        ownerProfileId: 99,
        maxPlayers: 10,
        isPrivate: false,
        players: [],
        blinds: { smallBlind: 50, bigBlind: 100, ante: 0 },
      }),
    })
    expect(state.isOwner).toBe(false)
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/game/__tests__/gameReducer.test.ts`
Expected: FAIL — `isOwner` does not exist on `GameState`

**Step 3: Implement — add `isOwner` to GameState and update handlers**

In `code/web/lib/game/gameReducer.ts`:

1. Add `isOwner: boolean` to the `GameState` interface (after `error` field, around line 127):
```typescript
  /** True when this client is the game owner/host (can kick/pause/resume) */
  isOwner: boolean
```

2. Add `isOwner: false` to `initialGameState` (around line 171):
```typescript
  isOwner: false,
```

3. Update `handleLobbyState` (around line 464) to compute `isOwner`:
```typescript
function handleLobbyState(state: GameState, data: LobbyStateData): GameState {
  return {
    ...state,
    gamePhase: 'lobby',
    lobbyState: data,
    isOwner: state.myPlayerId != null && state.myPlayerId === data.ownerProfileId,
    error: null,
  }
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/game/__tests__/gameReducer.test.ts`
Expected: PASS

**Step 5: Commit**

```bash
git add code/web/lib/game/gameReducer.ts code/web/lib/game/__tests__/gameReducer.test.ts
git commit -m "feat(web): add isOwner to game state from lobby ownerProfileId"
```

---

### Task 1.2: Add Admin Pause/Resume Buttons to PokerTable

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx`

**Step 1: Add admin toolbar**

In `PokerTable.tsx`, after the observer count block (line 131) and before the TableFelt, add:

```tsx
{/* Admin controls — visible to game owner only */}
{state.isOwner && (
  <div className="absolute top-12 left-1/2 -translate-x-1/2 z-10 flex gap-2">
    {gameState.status === 'PAUSED' ? (
      <button
        type="button"
        onClick={actions.sendAdminResume}
        className="px-3 py-1 text-xs font-semibold rounded-lg bg-green-700 hover:bg-green-600 text-white"
      >
        Resume Game
      </button>
    ) : (
      <button
        type="button"
        onClick={actions.sendAdminPause}
        className="px-3 py-1 text-xs font-semibold rounded-lg bg-yellow-700 hover:bg-yellow-600 text-white"
      >
        Pause Game
      </button>
    )}
  </div>
)}
```

**Step 2: Run existing tests to verify no regressions**

Run: `cd code/web && npx vitest run`
Expected: All existing tests PASS

**Step 3: Commit**

```bash
git add code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add admin pause/resume buttons to poker table"
```

---

### Task 1.3: Add Admin Kick Button to PlayerSeat

**Files:**
- Modify: `code/web/components/game/PlayerSeat.tsx`
- Test: `code/web/components/game/__tests__/PlayerSeat.test.tsx`

**Step 1: Write the failing test**

Add to `PlayerSeat.test.tsx`:

```typescript
it('shows kick button when isAdmin is true and seat is not me', () => {
  const seat = makeSeat(0, 1, 'Alice', 1000)
  render(
    <PlayerSeat seat={seat} isMe={false} positionStyle={{ top: '50%', left: '50%' }} isAdmin={true} onKick={vi.fn()} />
  )
  expect(screen.getByRole('button', { name: /kick/i })).toBeTruthy()
})

it('does not show kick button when isAdmin is false', () => {
  const seat = makeSeat(0, 1, 'Alice', 1000)
  render(
    <PlayerSeat seat={seat} isMe={false} positionStyle={{ top: '50%', left: '50%' }} isAdmin={false} />
  )
  expect(screen.queryByRole('button', { name: /kick/i })).toBeNull()
})

it('does not show kick button on own seat even when admin', () => {
  const seat = makeSeat(0, 1, 'Alice', 1000)
  render(
    <PlayerSeat seat={seat} isMe={true} positionStyle={{ top: '50%', left: '50%' }} isAdmin={true} onKick={vi.fn()} />
  )
  expect(screen.queryByRole('button', { name: /kick/i })).toBeNull()
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run components/game/__tests__/PlayerSeat.test.ts`
Expected: FAIL — `isAdmin` prop does not exist

**Step 3: Implement — add kick button to PlayerSeat**

In `code/web/components/game/PlayerSeat.tsx`:

1. Add to `PlayerSeatProps` interface:
```typescript
  /** Whether the local player is the game owner/admin */
  isAdmin?: boolean
  /** Called when admin clicks kick on this seat */
  onKick?: (playerId: number) => void
```

2. Destructure new props in the component function signature:
```typescript
export function PlayerSeat({ seat, isMe, positionStyle, isAdmin, onKick }: PlayerSeatProps) {
```

3. Add kick button inside the player info box div (after the status badges div, before the closing `</div>` of the info box):
```tsx
{isAdmin && !isMe && onKick && (
  <button
    type="button"
    onClick={(e) => { e.stopPropagation(); onKick(seat.playerId) }}
    aria-label={`Kick ${playerName}`}
    className="text-[9px] text-red-400 hover:text-red-300 mt-0.5"
  >
    Kick
  </button>
)}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run components/game/__tests__/PlayerSeat.test.ts`
Expected: PASS

**Step 5: Commit**

```bash
git add code/web/components/game/PlayerSeat.tsx code/web/components/game/__tests__/PlayerSeat.test.tsx
git commit -m "feat(web): add admin kick button to PlayerSeat component"
```

---

### Task 1.4: Wire Kick to PokerTable with Confirmation Dialog

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx`

**Step 1: Add kick state and Dialog import**

In `PokerTable.tsx`:

1. Add import for Dialog:
```typescript
import { Dialog } from '@/components/ui/Dialog'
```

2. Add state for kick confirmation (inside the component, after `setChatOpen`):
```typescript
const [kickTarget, setKickTarget] = useState<{ playerId: number; playerName: string } | null>(null)
```

3. Pass `isAdmin` and `onKick` to each `PlayerSeat`:
```tsx
<PlayerSeat
  key={seat.playerId ?? arrayIndex}
  seat={{
    ...seat,
    holeCards: seat.playerId === myPlayerId ? holeCards : [],
  }}
  isMe={seat.playerId === myPlayerId}
  positionStyle={SEAT_POSITIONS[visualPosition(arrayIndex)]}
  isAdmin={state.isOwner}
  onKick={(playerId) => {
    const name = currentTable.seats.find((s) => s.playerId === playerId)?.playerName ?? 'Player'
    setKickTarget({ playerId, playerName: name })
  }}
/>
```

4. Add confirmation dialog at the end of the component (before the closing `</div>`):
```tsx
{/* Kick confirmation dialog */}
{kickTarget && (
  <Dialog
    isOpen={true}
    onClose={() => setKickTarget(null)}
    onConfirm={() => {
      actions.sendAdminKick(kickTarget.playerId)
      setKickTarget(null)
    }}
    type="confirm"
    title="Kick Player"
    message={`Remove ${kickTarget.playerName} from the game?`}
    confirmText="Kick"
    cancelText="Cancel"
  />
)}
```

**Step 2: Run existing tests to verify no regressions**

Run: `cd code/web && npx vitest run`
Expected: All existing tests PASS

**Step 3: Commit**

```bash
git add code/web/components/game/PokerTable.tsx
git commit -m "feat(web): wire admin kick with confirmation dialog in PokerTable"
```

---

## Phase 2: Observer List

### Task 2.1: Create ObserverPanel Component

**Files:**
- Create: `code/web/components/game/ObserverPanel.tsx`
- Create: `code/web/components/game/__tests__/ObserverPanel.test.tsx`

**Step 1: Write the failing test**

Create `code/web/components/game/__tests__/ObserverPanel.test.tsx`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ObserverPanel } from '../ObserverPanel'

describe('ObserverPanel', () => {
  it('renders nothing when no observers', () => {
    const { container } = render(<ObserverPanel observers={[]} />)
    expect(container.textContent).toBe('')
  })

  it('shows observer count', () => {
    render(
      <ObserverPanel observers={[
        { observerId: 1, observerName: 'Alice' },
        { observerId: 2, observerName: 'Bob' },
      ]} />
    )
    expect(screen.getByText('2 watching')).toBeTruthy()
  })

  it('expands to show observer names on click', () => {
    render(
      <ObserverPanel observers={[
        { observerId: 1, observerName: 'Alice' },
        { observerId: 2, observerName: 'Bob' },
      ]} />
    )
    fireEvent.click(screen.getByText('2 watching'))
    expect(screen.getByText('Alice')).toBeTruthy()
    expect(screen.getByText('Bob')).toBeTruthy()
  })

  it('collapses on second click', () => {
    render(
      <ObserverPanel observers={[
        { observerId: 1, observerName: 'Alice' },
      ]} />
    )
    fireEvent.click(screen.getByText('1 watching'))
    expect(screen.getByText('Alice')).toBeTruthy()
    fireEvent.click(screen.getByText('1 watching'))
    expect(screen.queryByText('Alice')).toBeNull()
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run components/game/__tests__/ObserverPanel.test.tsx`
Expected: FAIL — module not found

**Step 3: Implement ObserverPanel**

Create `code/web/components/game/ObserverPanel.tsx`:

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useState } from 'react'

interface Observer {
  observerId: number
  observerName: string
}

interface ObserverPanelProps {
  observers: Observer[]
}

/**
 * Expandable observer list.
 *
 * Default: shows "{N} watching" counter.
 * On click: expands to show the list of observer names.
 * XSS safety: observer names rendered as text nodes only.
 */
export function ObserverPanel({ observers }: ObserverPanelProps) {
  const [expanded, setExpanded] = useState(false)

  if (observers.length === 0) return null

  return (
    <div className="bg-gray-900 bg-opacity-80 rounded-lg shadow-md">
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        className="px-2 py-1 text-gray-400 text-xs hover:text-gray-200 w-full text-left"
      >
        {observers.length} watching
      </button>
      {expanded && (
        <div className="px-2 pb-2 space-y-0.5">
          {observers.map((o) => (
            <div key={o.observerId} className="text-xs text-gray-300 pl-1">
              {o.observerName}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run components/game/__tests__/ObserverPanel.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add code/web/components/game/ObserverPanel.tsx code/web/components/game/__tests__/ObserverPanel.test.tsx
git commit -m "feat(web): add expandable ObserverPanel component"
```

---

### Task 2.2: Integrate ObserverPanel into PokerTable

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx`

**Step 1: Replace inline observer count with ObserverPanel**

In `PokerTable.tsx`:

1. Add import:
```typescript
import { ObserverPanel } from './ObserverPanel'
```

2. Replace the observer count block (lines 124-131):

From:
```tsx
{/* Task 6.8: observer count — shown when watchers are present */}
{state.observers.length > 0 && (
  <div className="absolute top-12 left-3 z-10">
    <span className="text-gray-400 text-xs">
      {state.observers.length} watching
    </span>
  </div>
)}
```

To:
```tsx
{/* Observer panel — shown when watchers are present */}
<div className="absolute top-12 left-3 z-10">
  <ObserverPanel observers={state.observers} />
</div>
```

**Step 2: Run tests**

Run: `cd code/web && npx vitest run`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add code/web/components/game/PokerTable.tsx
git commit -m "feat(web): integrate ObserverPanel into PokerTable"
```

---

## Phase 3: Chat Mute

### Task 3.1: Create useMutedPlayers Hook

**Files:**
- Create: `code/web/lib/game/useMutedPlayers.ts`
- Create: `code/web/lib/game/__tests__/useMutedPlayers.test.ts`

**Step 1: Write the failing test**

Create `code/web/lib/game/__tests__/useMutedPlayers.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useMutedPlayers } from '../useMutedPlayers'

// Mock localStorage
const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => { store[key] = value })
})

describe('useMutedPlayers', () => {
  it('starts with empty set', () => {
    const { result } = renderHook(() => useMutedPlayers())
    expect(result.current.isMuted(1)).toBe(false)
  })

  it('mutes a player', () => {
    const { result } = renderHook(() => useMutedPlayers())
    act(() => result.current.mute(42))
    expect(result.current.isMuted(42)).toBe(true)
  })

  it('unmutes a player', () => {
    const { result } = renderHook(() => useMutedPlayers())
    act(() => result.current.mute(42))
    act(() => result.current.unmute(42))
    expect(result.current.isMuted(42)).toBe(false)
  })

  it('persists to localStorage', () => {
    const { result } = renderHook(() => useMutedPlayers())
    act(() => result.current.mute(1))
    act(() => result.current.mute(2))
    expect(store['ddpoker-muted-players']).toBe('[1,2]')
  })

  it('loads from localStorage', () => {
    store['ddpoker-muted-players'] = '[10,20]'
    const { result } = renderHook(() => useMutedPlayers())
    expect(result.current.isMuted(10)).toBe(true)
    expect(result.current.isMuted(20)).toBe(true)
    expect(result.current.isMuted(30)).toBe(false)
  })

  it('returns mutedIds set', () => {
    const { result } = renderHook(() => useMutedPlayers())
    act(() => result.current.mute(5))
    expect(result.current.mutedIds.has(5)).toBe(true)
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/game/__tests__/useMutedPlayers.test.ts`
Expected: FAIL — module not found

**Step 3: Implement useMutedPlayers**

Create `code/web/lib/game/useMutedPlayers.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'

const STORAGE_KEY = 'ddpoker-muted-players'

function loadMuted(): Set<number> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return new Set(JSON.parse(raw) as number[])
  } catch { /* ignore corrupt data */ }
  return new Set()
}

function saveMuted(ids: Set<number>): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...ids]))
  } catch { /* localStorage unavailable */ }
}

export function useMutedPlayers() {
  const [mutedIds, setMutedIds] = useState<Set<number>>(() => loadMuted())

  const mute = useCallback((playerId: number) => {
    setMutedIds((prev) => {
      const next = new Set(prev)
      next.add(playerId)
      saveMuted(next)
      return next
    })
  }, [])

  const unmute = useCallback((playerId: number) => {
    setMutedIds((prev) => {
      const next = new Set(prev)
      next.delete(playerId)
      saveMuted(next)
      return next
    })
  }, [])

  const isMuted = useCallback((playerId: number) => mutedIds.has(playerId), [mutedIds])

  return { mutedIds, mute, unmute, isMuted }
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/game/__tests__/useMutedPlayers.test.ts`
Expected: PASS

**Step 5: Commit**

```bash
git add code/web/lib/game/useMutedPlayers.ts code/web/lib/game/__tests__/useMutedPlayers.test.ts
git commit -m "feat(web): add useMutedPlayers hook with localStorage persistence"
```

---

### Task 3.2: Add Mute Functionality to ChatPanel

**Files:**
- Modify: `code/web/components/game/ChatPanel.tsx`

**Step 1: Update ChatPanel to accept mute props and filter messages**

In `code/web/components/game/ChatPanel.tsx`:

1. Update the `ChatPanelProps` interface:
```typescript
interface ChatPanelProps {
  messages: ChatEntry[]
  onSend: (message: string) => void
  open: boolean
  onToggle: () => void
  /** Set of muted player IDs — messages from these players are hidden */
  mutedIds?: Set<number>
  /** Called when user clicks mute on a message */
  onMute?: (playerId: number) => void
  /** Called when user clicks unmute */
  onUnmute?: (playerId: number) => void
}
```

2. Update destructuring:
```typescript
export function ChatPanel({ messages, onSend, open, onToggle, mutedIds, onMute, onUnmute }: ChatPanelProps) {
```

3. Add filtering logic (after `listRef`):
```typescript
const visibleMessages = mutedIds && mutedIds.size > 0
  ? messages.filter((m) => !mutedIds.has(m.playerId))
  : messages
const hiddenCount = messages.length - visibleMessages.length
```

4. Replace `messages` with `visibleMessages` in the render (both the empty check and the `.map()` call).

5. Add mute icon to each message row (wrap the existing message content):
```tsx
{visibleMessages.map((entry) => (
  <div key={entry.id} className="text-xs group flex items-start gap-1">
    <div className="flex-1">
      <span className={`font-semibold ${entry.optimistic ? 'text-blue-300' : 'text-yellow-300'}`}>
        {entry.playerName}:
      </span>{' '}
      <span className="text-gray-200">{entry.message}</span>
    </div>
    {onMute && !entry.optimistic && (
      <button
        type="button"
        onClick={() => onMute(entry.playerId)}
        aria-label={`Mute ${entry.playerName}`}
        className="text-gray-600 hover:text-red-400 opacity-0 group-hover:opacity-100 text-[10px] flex-shrink-0"
      >
        Mute
      </button>
    )}
  </div>
))}
```

6. Add hidden messages indicator (after the message list, before the form):
```tsx
{hiddenCount > 0 && onUnmute && (
  <div className="px-2 py-1 text-[10px] text-gray-500 border-t border-gray-700">
    {hiddenCount} message{hiddenCount > 1 ? 's' : ''} from muted players hidden
  </div>
)}
```

**Step 2: Run tests**

Run: `cd code/web && npx vitest run`
Expected: All existing tests PASS (new props are optional, backward compatible)

**Step 3: Commit**

```bash
git add code/web/components/game/ChatPanel.tsx
git commit -m "feat(web): add mute/unmute support to ChatPanel"
```

---

### Task 3.3: Wire Mute to PokerTable

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx`

**Step 1: Integrate useMutedPlayers into PokerTable**

In `PokerTable.tsx`:

1. Add import:
```typescript
import { useMutedPlayers } from '@/lib/game/useMutedPlayers'
```

2. Add hook call inside the component (after the `chatOpen` state):
```typescript
const { mutedIds, mute, unmute } = useMutedPlayers()
```

3. Pass mute props to ChatPanel:
```tsx
<ChatPanel
  messages={chatMessages}
  onSend={actions.sendChat}
  open={chatOpen}
  onToggle={() => setChatOpen((v) => !v)}
  mutedIds={mutedIds}
  onMute={mute}
  onUnmute={unmute}
/>
```

**Step 2: Run tests**

Run: `cd code/web && npx vitest run`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add code/web/components/game/PokerTable.tsx
git commit -m "feat(web): wire chat mute into PokerTable via useMutedPlayers"
```

---

## Phase 4: Persistent Mini Chip Standings

### Task 4.1: Create ChipLeaderMini Component

**Files:**
- Create: `code/web/components/game/ChipLeaderMini.tsx`
- Create: `code/web/components/game/__tests__/ChipLeaderMini.test.tsx`

**Step 1: Write the failing test**

Create `code/web/components/game/__tests__/ChipLeaderMini.test.tsx`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { ChipLeaderMini } from '../ChipLeaderMini'

describe('ChipLeaderMini', () => {
  it('renders nothing when no players', () => {
    const { container } = render(<ChipLeaderMini players={[]} myPlayerId={1} />)
    expect(container.textContent).toBe('')
  })

  it('shows top 3 players sorted by chips', () => {
    const players = [
      { playerId: 1, name: 'Alice', chipCount: 500 },
      { playerId: 2, name: 'Bob', chipCount: 1500 },
      { playerId: 3, name: 'Charlie', chipCount: 1000 },
      { playerId: 4, name: 'Dave', chipCount: 200 },
    ]
    render(<ChipLeaderMini players={players} myPlayerId={4} />)
    expect(screen.getByText('Bob')).toBeTruthy()
    expect(screen.getByText('Charlie')).toBeTruthy()
    expect(screen.getByText('Alice')).toBeTruthy()
    // Dave (4th) should not be in top 3
    expect(screen.queryByText(/^Dave$/)).toBeNull()
  })

  it('shows my position when not in top 3', () => {
    const players = [
      { playerId: 1, name: 'Alice', chipCount: 5000 },
      { playerId: 2, name: 'Bob', chipCount: 4000 },
      { playerId: 3, name: 'Charlie', chipCount: 3000 },
      { playerId: 4, name: 'Me', chipCount: 100 },
    ]
    render(<ChipLeaderMini players={players} myPlayerId={4} />)
    expect(screen.getByText(/4th/)).toBeTruthy()
  })

  it('shows all players when 5 or fewer', () => {
    const players = [
      { playerId: 1, name: 'Alice', chipCount: 500 },
      { playerId: 2, name: 'Bob', chipCount: 300 },
    ]
    render(<ChipLeaderMini players={players} myPlayerId={1} />)
    expect(screen.getByText('Alice')).toBeTruthy()
    expect(screen.getByText('Bob')).toBeTruthy()
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run components/game/__tests__/ChipLeaderMini.test.tsx`
Expected: FAIL — module not found

**Step 3: Implement ChipLeaderMini**

Create `code/web/components/game/ChipLeaderMini.tsx`:

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { formatChips } from '@/lib/utils'

interface Player {
  playerId: number
  name: string
  chipCount: number
}

interface ChipLeaderMiniProps {
  players: Player[]
  myPlayerId: number | null
}

function ordinal(n: number): string {
  const s = ['th', 'st', 'nd', 'rd']
  const v = n % 100
  return n + (s[(v - 20) % 10] || s[v] || s[0])
}

/**
 * Compact always-visible chip standings.
 *
 * Shows top 3 players (or all if <=5 remain).
 * If the local player is not in the displayed list, shows their rank below.
 * XSS safety: all names rendered as text nodes only.
 */
export function ChipLeaderMini({ players, myPlayerId }: ChipLeaderMiniProps) {
  if (players.length === 0) return null

  const sorted = [...players].sort((a, b) => b.chipCount - a.chipCount)
  const showAll = sorted.length <= 5
  const displayed = showAll ? sorted : sorted.slice(0, 3)
  const myRank = sorted.findIndex((p) => p.playerId === myPlayerId) + 1
  const myInDisplayed = displayed.some((p) => p.playerId === myPlayerId)

  return (
    <div className="bg-gray-900 bg-opacity-80 rounded-lg px-2 py-1.5 text-xs shadow-md min-w-[120px]">
      {displayed.map((p, i) => (
        <div
          key={p.playerId}
          className={`flex items-center gap-1 ${p.playerId === myPlayerId ? 'text-blue-300' : 'text-gray-300'}`}
        >
          <span className="text-gray-500 w-4">{i + 1}.</span>
          <span className="flex-1 truncate max-w-[70px]">{p.name}</span>
          <span className="text-yellow-300 font-semibold">{formatChips(p.chipCount)}</span>
        </div>
      ))}
      {!myInDisplayed && myRank > 0 && (
        <div className="text-blue-300 border-t border-gray-700 mt-1 pt-1 flex items-center gap-1">
          <span className="text-gray-500 w-4">{ordinal(myRank)}</span>
          <span className="flex-1 truncate max-w-[70px]">You</span>
          <span className="text-yellow-300 font-semibold">
            {formatChips(sorted[myRank - 1].chipCount)}
          </span>
        </div>
      )}
    </div>
  )
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run components/game/__tests__/ChipLeaderMini.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add code/web/components/game/ChipLeaderMini.tsx code/web/components/game/__tests__/ChipLeaderMini.test.tsx
git commit -m "feat(web): add ChipLeaderMini component for persistent standings"
```

---

### Task 4.2: Integrate ChipLeaderMini into PokerTable

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx`

**Step 1: Add ChipLeaderMini to the table**

In `PokerTable.tsx`:

1. Add import:
```typescript
import { ChipLeaderMini } from './ChipLeaderMini'
```

2. Add ChipLeaderMini after the TournamentInfoBar block and observer panel (around the right side of the table). Place it below the hand history panel:
```tsx
{/* Mini chip standings — right side, below hand history */}
{currentTable.seats.length > 0 && (
  <div className="absolute top-72 right-3 z-10">
    <ChipLeaderMini
      players={currentTable.seats.map((s) => ({
        playerId: s.playerId,
        name: s.playerName,
        chipCount: s.chipCount,
      }))}
      myPlayerId={myPlayerId}
    />
  </div>
)}
```

**Step 2: Run tests**

Run: `cd code/web && npx vitest run`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add code/web/components/game/PokerTable.tsx
git commit -m "feat(web): add persistent mini chip standings to poker table"
```

---

## Phase 5: Betting Hotkey Improvements

### Task 5.1: Add Enter, A, Escape Hotkeys

**Files:**
- Modify: `code/web/components/game/ActionPanel.tsx`
- Test: `code/web/components/game/__tests__/ActionPanel.test.tsx`

**Step 1: Write the failing tests**

Add to `ActionPanel.test.tsx`:

```typescript
describe('keyboard shortcuts - extended', () => {
  it('Enter key confirms pending raise', () => {
    const onAction = vi.fn()
    render(
      <ActionPanel
        options={makeOptions({ canRaise: true, minRaise: 200, maxRaise: 2000 })}
        potSize={500}
        onAction={onAction}
      />,
    )
    // Press R to enter raise mode
    fireEvent.keyDown(window, { key: 'r' })
    // Press Enter to confirm
    fireEvent.keyDown(window, { key: 'Enter' })
    expect(onAction).toHaveBeenCalledWith({ action: 'RAISE', amount: 200 })
  })

  it('Escape cancels pending raise', () => {
    render(
      <ActionPanel
        options={makeOptions({ canRaise: true, minRaise: 200, maxRaise: 2000 })}
        potSize={500}
        onAction={vi.fn()}
      />,
    )
    fireEvent.keyDown(window, { key: 'r' })
    expect(screen.getByTestId('bet-slider')).toBeTruthy()
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(screen.queryByTestId('bet-slider')).toBeNull()
  })

  it('A key triggers all-in when available', () => {
    const onAction = vi.fn()
    render(
      <ActionPanel
        options={makeOptions({ canAllIn: true, allInAmount: 5000 })}
        potSize={500}
        onAction={onAction}
      />,
    )
    fireEvent.keyDown(window, { key: 'a' })
    expect(onAction).toHaveBeenCalledWith({ action: 'ALL_IN', amount: 5000 })
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run components/game/__tests__/ActionPanel.test.tsx`
Expected: FAIL — Enter and A keys not handled

**Step 3: Implement — extend keyboard handler**

In `code/web/components/game/ActionPanel.tsx`, extend the `switch` statement in the keyboard handler (lines 47-58). The handler needs access to `pendingAction`, `betAmount`, and the functions. Since the keyboard handler currently only reads `options` from the closure, we need to restructure slightly.

Replace the keyboard handler `useEffect` (lines 42-63) with:

```typescript
  // Keyboard shortcuts — suppressed when a text input has focus
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return

      switch (e.key.toLowerCase()) {
        case 'f':
          if (options.canFold) onActionRef.current({ action: 'FOLD', amount: 0 })
          break
        case 'c':
          if (options.canCheck) onActionRef.current({ action: 'CHECK', amount: 0 })
          else if (options.canCall) onActionRef.current({ action: 'CALL', amount: options.callAmount })
          break
        case 'r':
          if (options.canRaise) setPendingAction('raise')
          else if (options.canBet) setPendingAction('bet')
          break
        case 'a':
          if (options.canAllIn) onActionRef.current({ action: 'ALL_IN', amount: options.allInAmount })
          break
        case 'enter':
          if (pendingAction) {
            const action = pendingAction === 'bet' ? 'BET' : 'RAISE'
            onActionRef.current({ action, amount: betAmount })
            setPendingAction(null)
          }
          break
        case 'escape':
          if (pendingAction) setPendingAction(null)
          break
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [options, pendingAction, betAmount])
```

Note: the dependency array now includes `pendingAction` and `betAmount` so the handler sees the current values.

Also update the component docstring to document the new keys:
```typescript
/**
 * Action panel shown when it's the current player's turn.
 *
 * Keyboard shortcuts (suppressed when any text input is focused):
 * - F: Fold
 * - C: Check / Call
 * - R: Raise / Bet (focuses bet slider)
 * - A: All-in
 * - Enter: Confirm pending bet/raise
 * - Escape: Cancel pending bet/raise
 */
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run components/game/__tests__/ActionPanel.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add code/web/components/game/ActionPanel.tsx code/web/components/game/__tests__/ActionPanel.test.tsx
git commit -m "feat(web): add Enter/Escape/A keyboard shortcuts to ActionPanel"
```

---

## Phase 6: Hand History Export

### Task 6.1: Add formatHandHistoryForExport Utility

**Files:**
- Modify: `code/web/lib/utils.ts`
- Modify: `code/web/lib/__tests__/utils.test.ts`

**Step 1: Write the failing test**

Add to `code/web/lib/__tests__/utils.test.ts`:

```typescript
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

describe('formatHandHistoryForExport', () => {
  it('formats a complete hand', () => {
    const entries: HandHistoryEntry[] = [
      { id: '1', handNumber: 1, type: 'hand_start', timestamp: 1000 },
      { id: '2', handNumber: 1, type: 'action', playerName: 'Alice', action: 'CALL', amount: 100, timestamp: 1001 },
      { id: '3', handNumber: 1, type: 'action', playerName: 'Bob', action: 'FOLD', amount: 0, timestamp: 1002 },
      { id: '4', handNumber: 1, type: 'community', round: 'FLOP', cards: ['As', 'Kd', 'Qc'], timestamp: 1003 },
      { id: '5', handNumber: 1, type: 'result', handNumber: 1, winners: [{ playerName: 'Alice', amount: 200, hand: 'Pair of Aces' }], timestamp: 1004 },
    ]
    const output = formatHandHistoryForExport(entries)
    expect(output).toContain('Hand #1')
    expect(output).toContain('Alice: CALL 100')
    expect(output).toContain('Bob: FOLD')
    expect(output).toContain('FLOP: As Kd Qc')
    expect(output).toContain('Alice wins 200 with Pair of Aces')
  })

  it('returns empty string for empty entries', () => {
    const output = formatHandHistoryForExport([])
    expect(output).toBe('')
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/__tests__/utils.test.ts`
Expected: FAIL — `formatHandHistoryForExport` is not exported

**Step 3: Implement formatHandHistoryForExport**

Add to `code/web/lib/utils.ts`:

```typescript
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

export function formatHandHistoryForExport(entries: HandHistoryEntry[]): string {
  if (entries.length === 0) return ''

  const lines: string[] = []
  for (const entry of entries) {
    switch (entry.type) {
      case 'hand_start':
        if (lines.length > 0) lines.push('') // blank line between hands
        lines.push(`--- Hand #${entry.handNumber} ---`)
        break
      case 'action': {
        const amt = entry.amount && entry.amount > 0 ? ` ${formatChips(entry.amount)}` : ''
        lines.push(`${entry.playerName ?? 'Player'}: ${entry.action ?? ''}${amt}`)
        break
      }
      case 'community':
        lines.push(`${entry.round ?? 'Dealt'}: ${(entry.cards ?? []).join(' ')}`)
        break
      case 'result': {
        const winner = entry.winners?.[0]
        if (winner) {
          lines.push(`${winner.playerName} wins ${formatChips(winner.amount)} with ${winner.hand}`)
        } else {
          lines.push('Hand complete')
        }
        break
      }
    }
  }
  return lines.join('\n')
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/__tests__/utils.test.ts`
Expected: PASS

**Step 5: Commit**

```bash
git add code/web/lib/utils.ts code/web/lib/__tests__/utils.test.ts
git commit -m "feat(web): add formatHandHistoryForExport utility"
```

---

### Task 6.2: Add Export Button to HandHistory

**Files:**
- Modify: `code/web/components/game/HandHistory.tsx`

**Step 1: Add export button**

In `code/web/components/game/HandHistory.tsx`:

1. Add import:
```typescript
import { formatHandHistoryForExport } from '@/lib/utils'
```

2. Add export handler inside the component:
```typescript
function handleExport() {
  const text = formatHandHistoryForExport(entries)
  if (!text) return
  const blob = new Blob([text], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `hand-history-${Date.now()}.txt`
  a.click()
  URL.revokeObjectURL(url)
}
```

3. Add export button to the header bar (next to the toggle button). Replace the header button with a div containing both buttons:

```tsx
<div className="flex items-center justify-between w-full px-3 py-2">
  <button
    type="button"
    onClick={() => setOpen((v) => !v)}
    aria-expanded={open}
    aria-controls="hand-history-log"
    className="flex items-center gap-1 text-sm font-semibold text-gray-300 hover:text-white"
  >
    <span>Hand History</span>
    <span>{open ? '▾' : '▸'}</span>
  </button>
  {entries.length > 0 && (
    <button
      type="button"
      onClick={handleExport}
      aria-label="Export hand history"
      className="text-gray-500 hover:text-gray-300 text-xs"
      title="Export hand history"
    >
      Export
    </button>
  )}
</div>
```

**Step 2: Run tests**

Run: `cd code/web && npx vitest run`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add code/web/components/game/HandHistory.tsx
git commit -m "feat(web): add hand history export button"
```

---

## Final: Run All Tests

Run: `cd code/web && npx vitest run`
Expected: All tests PASS

---

## Summary

| Phase | Feature | New Files | Modified Files | Tests |
|-------|---------|-----------|----------------|-------|
| 1 | Admin controls | - | gameReducer.ts, PlayerSeat.tsx, PokerTable.tsx | gameReducer.test.ts, PlayerSeat.test.tsx |
| 2 | Observer list | ObserverPanel.tsx | PokerTable.tsx | ObserverPanel.test.tsx |
| 3 | Chat mute | useMutedPlayers.ts | ChatPanel.tsx, PokerTable.tsx | useMutedPlayers.test.ts |
| 4 | Chip standings | ChipLeaderMini.tsx | PokerTable.tsx | ChipLeaderMini.test.tsx |
| 5 | Betting hotkeys | - | ActionPanel.tsx | ActionPanel.test.tsx |
| 6 | Hand history export | - | utils.ts, HandHistory.tsx | utils.test.ts |
