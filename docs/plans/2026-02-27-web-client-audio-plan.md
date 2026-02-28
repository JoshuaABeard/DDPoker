# Web Client Audio Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add gameplay sound effects to the web client using Web Audio API with dual-format MP3/OGG audio files.

**Architecture:** AudioManager singleton wraps Web Audio API (AudioContext + GainNode). React hooks (`useAudio`, `useSoundEffects`) bridge the manager to components. A sound map translates logical event names (bet, check, shuffle) to randomized file arrays. Volume/mute state persists in localStorage.

**Tech Stack:** Web Audio API (native), React 19 hooks, Vitest + React Testing Library, ffmpeg (one-time conversion)

**Design doc:** `docs/plans/2026-02-27-web-client-audio-design.md`

---

## Phase 1: Audio Assets

### Task 1: Convert Audio Files

**Files:**
- Source: `code/poker/src/main/resources/config/poker/audio/*.wav, *.aif`
- Create: `code/web/public/audio/*.mp3` (26 files)
- Create: `code/web/public/audio/*.ogg` (26 files)

**Step 1: Create conversion script**

Create `code/web/scripts/convert-audio.sh`:

```bash
#!/usr/bin/env bash
# Convert desktop audio files to web-optimized MP3 + OGG
set -euo pipefail

SRC="../../poker/src/main/resources/config/poker/audio"
DST="../public/audio"
mkdir -p "$DST"

# Skip music files (music2a, music2b, music3a, music3b)
SKIP="music2a|music2b|music3a|music3b"

for f in "$SRC"/*.wav "$SRC"/*.aif; do
  [ -f "$f" ] || continue
  base=$(basename "${f%.*}")
  echo "$base" | grep -qE "^($SKIP)$" && continue

  echo "Converting $base..."
  ffmpeg -y -i "$f" -codec:a libmp3lame -b:a 128k "$DST/$base.mp3" 2>/dev/null
  ffmpeg -y -i "$f" -codec:a libvorbis -qscale:a 3 "$DST/$base.ogg" 2>/dev/null
done

echo "Done. $(ls "$DST"/*.mp3 2>/dev/null | wc -l) MP3 + $(ls "$DST"/*.ogg 2>/dev/null | wc -l) OGG files created."
```

**Step 2: Run the conversion**

Run: `cd code/web && bash scripts/convert-audio.sh`
Expected: 26 MP3 + 26 OGG files in `code/web/public/audio/`

Verify: `ls code/web/public/audio/ | wc -l` → 52 files

**Step 3: Add audio directory to .gitignore (if files are too large)**

Check total size: `du -sh code/web/public/audio/`
If under 2 MB, commit the audio files. If over, add `public/audio/` to `code/web/.gitignore` and document the conversion step.

**Step 4: Commit**

```bash
git add code/web/public/audio/ code/web/scripts/convert-audio.sh
git commit -m "feat(web): add converted audio files for gameplay sounds"
```

---

## Phase 2: Audio Engine

### Task 2: Sound Map

**Files:**
- Create: `code/web/lib/audio/soundMap.ts`
- Test: `code/web/lib/audio/__tests__/soundMap.test.ts`

**Step 1: Write the test**

Create `code/web/lib/audio/__tests__/soundMap.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { soundMap, ALL_SOUND_FILES } from '../soundMap'

describe('soundMap', () => {
  it('maps all expected sound names', () => {
    const expected = ['bet', 'check', 'raise', 'shuffle', 'shuffleShort', 'cheers', 'bell', 'attention', 'click', 'camera']
    for (const name of expected) {
      expect(soundMap[name]).toBeDefined()
      expect(soundMap[name].length).toBeGreaterThan(0)
    }
  })

  it('has 10 bet variants', () => {
    expect(soundMap.bet).toHaveLength(10)
  })

  it('exports a flat list of all unique file names', () => {
    expect(ALL_SOUND_FILES.length).toBeGreaterThan(0)
    // No duplicates
    expect(new Set(ALL_SOUND_FILES).size).toBe(ALL_SOUND_FILES.length)
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/audio/__tests__/soundMap.test.ts`
Expected: FAIL — module not found

**Step 3: Write the implementation**

Create `code/web/lib/audio/soundMap.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

/**
 * Maps logical sound event names to audio file base names.
 * When a name maps to multiple files, playback picks one at random.
 */
export const soundMap: Record<string, string[]> = {
  bet: ['bet1', 'bet2', 'bet3', 'bet4', 'bet5', 'bet6', 'bet7', 'bet8', 'bet9', 'bet10'],
  check: ['check'],
  raise: ['raise2'],
  shuffle: ['shuffle1', 'shuffle2', 'shuffle3'],
  shuffleShort: ['shuffle1_short', 'shuffle2_short', 'shuffle3_short'],
  cheers: ['cheers1', 'cheers2', 'cheers3', 'cheers4'],
  bell: ['bell', 'bell2'],
  attention: ['attention'],
  click: ['button-click'],
  camera: ['camera'],
}

/** Flat array of all unique audio file names (used for pre-loading). */
export const ALL_SOUND_FILES: string[] = [...new Set(Object.values(soundMap).flat())]
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/audio/__tests__/soundMap.test.ts`
Expected: 3 tests PASS

**Step 5: Commit**

```bash
git add code/web/lib/audio/soundMap.ts code/web/lib/audio/__tests__/soundMap.test.ts
git commit -m "feat(web): add sound map for audio event routing"
```

---

### Task 3: AudioManager Singleton

**Files:**
- Create: `code/web/lib/audio/AudioManager.ts`
- Test: `code/web/lib/audio/__tests__/AudioManager.test.ts`

**Context:** AudioManager wraps Web Audio API. Tests mock `AudioContext`, `GainNode`, and `fetch`. The manager lazy-initializes on first `init()` call (triggered by user gesture). It reads/writes volume and mute settings from localStorage.

**Step 1: Write the tests**

Create `code/web/lib/audio/__tests__/AudioManager.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { AudioManager } from '../AudioManager'

// Mock localStorage
const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

// Mock Web Audio API
const mockStart = vi.fn()
const mockSourceConnect = vi.fn()
const mockGainConnect = vi.fn()
const mockGain = { value: 0 }
const mockDecodeAudioData = vi.fn().mockResolvedValue({ duration: 1 })

beforeEach(() => {
  mockStart.mockClear()
  mockSourceConnect.mockClear()
  mockGainConnect.mockClear()
  mockDecodeAudioData.mockClear()
  mockGain.value = 0

  vi.stubGlobal('AudioContext', vi.fn(() => ({
    createGain: () => ({ gain: mockGain, connect: mockGainConnect }),
    createBufferSource: () => ({ buffer: null, connect: mockSourceConnect, start: mockStart }),
    decodeAudioData: mockDecodeAudioData,
    destination: {},
  })))

  // Mock fetch for audio loading
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    arrayBuffer: () => Promise.resolve(new ArrayBuffer(8)),
  }))
})

describe('AudioManager', () => {
  it('starts with default volume 0.7 and unmuted', () => {
    const mgr = new AudioManager()
    expect(mgr.getVolume()).toBe(0.7)
    expect(mgr.isMuted()).toBe(false)
  })

  it('loads saved settings from localStorage', () => {
    store['ddpoker-audio'] = JSON.stringify({ volume: 0.3, muted: true })
    const mgr = new AudioManager()
    expect(mgr.getVolume()).toBe(0.3)
    expect(mgr.isMuted()).toBe(true)
  })

  it('persists volume changes to localStorage', () => {
    const mgr = new AudioManager()
    mgr.setVolume(0.5)
    expect(store['ddpoker-audio']).toContain('"volume":0.5')
  })

  it('persists mute changes to localStorage', () => {
    const mgr = new AudioManager()
    mgr.setMuted(true)
    expect(store['ddpoker-audio']).toContain('"muted":true')
  })

  it('clamps volume to 0-1 range', () => {
    const mgr = new AudioManager()
    mgr.setVolume(1.5)
    expect(mgr.getVolume()).toBe(1)
    mgr.setVolume(-0.5)
    expect(mgr.getVolume()).toBe(0)
  })

  it('sets gain to 0 when muted after init', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    mgr.setMuted(true)
    expect(mockGain.value).toBe(0)
  })

  it('restores gain when unmuted after init', async () => {
    const mgr = new AudioManager()
    mgr.setVolume(0.6)
    await mgr.init()
    mgr.setMuted(true)
    mgr.setMuted(false)
    expect(mockGain.value).toBe(0.6)
  })

  it('plays a sound by creating a buffer source', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    mgr.play('check')
    expect(mockSourceConnect).toHaveBeenCalled()
    expect(mockStart).toHaveBeenCalled()
  })

  it('does not play when muted', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    mgr.setMuted(true)
    mgr.play('check')
    expect(mockStart).not.toHaveBeenCalled()
  })

  it('does not play unknown sound names', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    mgr.play('nonexistent')
    expect(mockStart).not.toHaveBeenCalled()
  })

  it('init is idempotent', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    await mgr.init()
    // AudioContext constructor called only once
    expect(AudioContext).toHaveBeenCalledTimes(1)
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/audio/__tests__/AudioManager.test.ts`
Expected: FAIL — module not found

**Step 3: Write the implementation**

Create `code/web/lib/audio/AudioManager.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { soundMap, ALL_SOUND_FILES } from './soundMap'

const STORAGE_KEY = 'ddpoker-audio'

interface AudioSettings {
  volume: number
  muted: boolean
}

/**
 * Singleton audio engine wrapping Web Audio API.
 *
 * Call `init()` on first user gesture to create the AudioContext
 * (required by browser autoplay policy). After init, `play(name)`
 * plays the named sound using the sound map for randomization.
 */
export class AudioManager {
  private context: AudioContext | null = null
  private gainNode: GainNode | null = null
  private buffers = new Map<string, AudioBuffer>()
  private settings: AudioSettings
  private initialized = false

  constructor() {
    this.settings = this.loadSettings()
  }

  private loadSettings(): AudioSettings {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) return JSON.parse(raw) as AudioSettings
    } catch { /* ignore corrupt data */ }
    return { volume: 0.7, muted: false }
  }

  private saveSettings(): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.settings))
    } catch { /* localStorage unavailable */ }
  }

  /** Initialize AudioContext and pre-load all sounds. Idempotent. */
  async init(): Promise<void> {
    if (this.initialized) return
    this.context = new AudioContext()
    this.gainNode = this.context.createGain()
    this.gainNode.connect(this.context.destination)
    this.gainNode.gain.value = this.settings.muted ? 0 : this.settings.volume
    this.initialized = true
    await this.loadAllSounds()
  }

  private async loadAllSounds(): Promise<void> {
    await Promise.allSettled(ALL_SOUND_FILES.map((f) => this.loadSound(f)))
  }

  private async loadSound(name: string): Promise<void> {
    if (!this.context) return
    for (const ext of ['ogg', 'mp3']) {
      try {
        const res = await fetch(`/audio/${name}.${ext}`)
        if (!res.ok) continue
        const buf = await res.arrayBuffer()
        this.buffers.set(name, await this.context.decodeAudioData(buf))
        return
      } catch { /* try next format */ }
    }
  }

  /** Play a named sound. For multi-file names (bet, shuffle), picks a random variant. */
  play(name: string): void {
    if (!this.context || !this.gainNode || this.settings.muted) return
    const files = soundMap[name]
    if (!files?.length) return
    const file = files[Math.floor(Math.random() * files.length)]
    const buffer = this.buffers.get(file)
    if (!buffer) return
    const source = this.context.createBufferSource()
    source.buffer = buffer
    source.connect(this.gainNode)
    source.start()
  }

  setVolume(value: number): void {
    this.settings.volume = Math.max(0, Math.min(1, value))
    if (this.gainNode && !this.settings.muted) {
      this.gainNode.gain.value = this.settings.volume
    }
    this.saveSettings()
  }

  setMuted(muted: boolean): void {
    this.settings.muted = muted
    if (this.gainNode) {
      this.gainNode.gain.value = muted ? 0 : this.settings.volume
    }
    this.saveSettings()
  }

  getVolume(): number {
    return this.settings.volume
  }

  isMuted(): boolean {
    return this.settings.muted
  }
}

/** Module-level singleton. */
let instance: AudioManager | null = null

export function getAudioManager(): AudioManager {
  if (!instance) instance = new AudioManager()
  return instance
}

/** Reset singleton (for testing). */
export function resetAudioManager(): void {
  instance = null
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/audio/__tests__/AudioManager.test.ts`
Expected: 11 tests PASS

**Step 5: Commit**

```bash
git add code/web/lib/audio/AudioManager.ts code/web/lib/audio/__tests__/AudioManager.test.ts
git commit -m "feat(web): add AudioManager singleton wrapping Web Audio API"
```

---

## Phase 3: React Integration

### Task 4: useAudio Hook

**Files:**
- Create: `code/web/lib/audio/useAudio.ts`
- Test: `code/web/lib/audio/__tests__/useAudio.test.ts`

**Context:** This hook wraps the AudioManager singleton for React components. It provides `playSound()`, `volume`, `setVolume`, `isMuted`, and `toggleMute`. It calls `manager.init()` on first playSound call.

**Step 1: Write the tests**

Create `code/web/lib/audio/__tests__/useAudio.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAudio } from '../useAudio'
import { resetAudioManager } from '../AudioManager'

// Mock localStorage
const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
  resetAudioManager()
})

// Mock Web Audio API (minimal — AudioManager handles the details)
beforeEach(() => {
  vi.stubGlobal('AudioContext', vi.fn(() => ({
    createGain: () => ({ gain: { value: 0 }, connect: vi.fn() }),
    createBufferSource: () => ({ buffer: null, connect: vi.fn(), start: vi.fn() }),
    decodeAudioData: vi.fn().mockResolvedValue({ duration: 1 }),
    destination: {},
  })))
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    arrayBuffer: () => Promise.resolve(new ArrayBuffer(8)),
  }))
})

describe('useAudio', () => {
  it('returns default volume and unmuted state', () => {
    const { result } = renderHook(() => useAudio())
    expect(result.current.volume).toBe(0.7)
    expect(result.current.isMuted).toBe(false)
  })

  it('toggleMute switches muted state', () => {
    const { result } = renderHook(() => useAudio())
    act(() => result.current.toggleMute())
    expect(result.current.isMuted).toBe(true)
    act(() => result.current.toggleMute())
    expect(result.current.isMuted).toBe(false)
  })

  it('setVolume updates volume', () => {
    const { result } = renderHook(() => useAudio())
    act(() => result.current.setVolume(0.4))
    expect(result.current.volume).toBe(0.4)
  })

  it('playSound is callable', () => {
    const { result } = renderHook(() => useAudio())
    // Should not throw
    expect(() => result.current.playSound('check')).not.toThrow()
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/audio/__tests__/useAudio.test.ts`
Expected: FAIL — module not found

**Step 3: Write the implementation**

Create `code/web/lib/audio/useAudio.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'
import { getAudioManager } from './AudioManager'

/**
 * React hook wrapping the AudioManager singleton.
 *
 * Provides playSound(), volume control, and mute toggle.
 * Calls AudioManager.init() on first playSound (user-gesture requirement).
 */
export function useAudio() {
  const manager = getAudioManager()
  const [volume, setVolumeState] = useState(manager.getVolume())
  const [isMuted, setMutedState] = useState(manager.isMuted())

  const playSound = useCallback((name: string) => {
    // init() is idempotent — safe to call on every play
    manager.init()
    manager.play(name)
  }, [manager])

  const setVolume = useCallback((v: number) => {
    manager.setVolume(v)
    setVolumeState(manager.getVolume())
  }, [manager])

  const toggleMute = useCallback(() => {
    const newMuted = !manager.isMuted()
    manager.setMuted(newMuted)
    setMutedState(newMuted)
  }, [manager])

  return { playSound, volume, setVolume, isMuted, toggleMute }
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/audio/__tests__/useAudio.test.ts`
Expected: 4 tests PASS

**Step 5: Commit**

```bash
git add code/web/lib/audio/useAudio.ts code/web/lib/audio/__tests__/useAudio.test.ts
git commit -m "feat(web): add useAudio React hook for sound playback"
```

---

### Task 5: useSoundEffects Hook

**Files:**
- Create: `code/web/lib/audio/useSoundEffects.ts`
- Test: `code/web/lib/audio/__tests__/useSoundEffects.test.ts`

**Context:** This hook watches `useGameState()` for changes and plays corresponding sounds. It tracks previous state via `useRef` to detect new events. It does NOT call `useGameState` directly — instead it accepts state as a parameter so tests can control it.

**Important types from `code/web/lib/game/gameReducer.ts`:**
```typescript
interface HandHistoryEntry {
  id: string; handNumber: number;
  type: 'action' | 'community' | 'result' | 'hand_start';
  playerName?: string; action?: string; amount?: number;
  round?: string; cards?: string[];
  winners?: { playerName: string; amount: number; hand: string }[];
  timestamp: number;
}
```

**Sound-to-event mapping:**
| Game Event | Sound | Detection |
|------------|-------|-----------|
| Hand starts | `shuffle` | New entry with `type: 'hand_start'` |
| Player bets/calls | `bet` | `type: 'action'`, `action` is `BET` or `CALL` |
| Player checks | `check` | `type: 'action'`, `action` is `CHECK` |
| Player raises | `raise` | `type: 'action'`, `action` is `RAISE` |
| Player folds | `click` | `type: 'action'`, `action` is `FOLD` |
| Community cards | `shuffleShort` | `type: 'community'` |
| Hand result | `cheers` | `type: 'result'` |
| Your turn | `bell` | `actionRequired` changes from null to non-null |

**Step 1: Write the tests**

Create `code/web/lib/audio/__tests__/useSoundEffects.test.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useSoundEffects } from '../useSoundEffects'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

// Mock useAudio
const mockPlaySound = vi.fn()
vi.mock('../useAudio', () => ({
  useAudio: () => ({
    playSound: mockPlaySound,
    volume: 0.7,
    setVolume: vi.fn(),
    isMuted: false,
    toggleMute: vi.fn(),
  }),
}))

function entry(overrides: Partial<HandHistoryEntry>): HandHistoryEntry {
  return {
    id: String(Math.random()),
    handNumber: 1,
    type: 'action',
    timestamp: Date.now(),
    ...overrides,
  }
}

describe('useSoundEffects', () => {
  beforeEach(() => {
    mockPlaySound.mockClear()
  })

  it('plays shuffle on hand_start', () => {
    const entries = [entry({ type: 'hand_start' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('shuffle')
  })

  it('plays bet on BET action', () => {
    const entries = [entry({ type: 'action', action: 'BET' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('bet')
  })

  it('plays check on CHECK action', () => {
    const entries = [entry({ type: 'action', action: 'CHECK' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('check')
  })

  it('plays raise on RAISE action', () => {
    const entries = [entry({ type: 'action', action: 'RAISE' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('raise')
  })

  it('plays click on FOLD action', () => {
    const entries = [entry({ type: 'action', action: 'FOLD' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('click')
  })

  it('plays shuffleShort on community cards', () => {
    const entries = [entry({ type: 'community', round: 'Flop', cards: ['Ah', 'Kd', '3c'] })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('shuffleShort')
  })

  it('plays cheers on result', () => {
    const entries = [entry({ type: 'result', winners: [{ playerName: 'P1', amount: 100, hand: 'Pair' }] })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('cheers')
  })

  it('plays bell when it becomes your turn', () => {
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: [], a: true })
    expect(mockPlaySound).toHaveBeenCalledWith('bell')
  })

  it('does not play bell if already your turn', () => {
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: true } },
    )
    rerender({ h: [], a: true })
    expect(mockPlaySound).not.toHaveBeenCalledWith('bell')
  })

  it('does not replay sounds for old entries', () => {
    const entries = [entry({ type: 'action', action: 'CHECK' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    mockPlaySound.mockClear()
    // Re-render with same entries — should NOT replay
    rerender({ h: entries, a: false })
    expect(mockPlaySound).not.toHaveBeenCalled()
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/audio/__tests__/useSoundEffects.test.ts`
Expected: FAIL — module not found

**Step 3: Write the implementation**

Create `code/web/lib/audio/useSoundEffects.ts`:

```typescript
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useRef } from 'react'
import { useAudio } from './useAudio'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

/**
 * Watches game state changes and plays appropriate sound effects.
 *
 * @param handHistory - Current hand history entries array
 * @param isMyTurn - Whether the local player must act now
 */
export function useSoundEffects(
  handHistory: HandHistoryEntry[],
  isMyTurn: boolean,
): void {
  const { playSound } = useAudio()
  const prevRef = useRef({ historyLength: handHistory.length, isMyTurn })

  useEffect(() => {
    const prev = prevRef.current

    // Process new hand history entries
    if (handHistory.length > prev.historyLength) {
      const newEntries = handHistory.slice(prev.historyLength)
      for (const e of newEntries) {
        switch (e.type) {
          case 'hand_start':
            playSound('shuffle')
            break
          case 'action':
            if (e.action === 'CHECK') playSound('check')
            else if (e.action === 'RAISE') playSound('raise')
            else if (e.action === 'FOLD') playSound('click')
            else playSound('bet') // BET, CALL, ALL_IN
            break
          case 'community':
            playSound('shuffleShort')
            break
          case 'result':
            playSound('cheers')
            break
        }
      }
    }

    // Your turn notification
    if (isMyTurn && !prev.isMyTurn) {
      playSound('bell')
    }

    prevRef.current = { historyLength: handHistory.length, isMyTurn }
  }, [handHistory, isMyTurn, playSound])
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/audio/__tests__/useSoundEffects.test.ts`
Expected: 10 tests PASS

**Step 5: Commit**

```bash
git add code/web/lib/audio/useSoundEffects.ts code/web/lib/audio/__tests__/useSoundEffects.test.ts
git commit -m "feat(web): add useSoundEffects hook for game event audio"
```

---

## Phase 4: UI & Integration

### Task 6: VolumeControl Component

**Files:**
- Create: `code/web/components/game/VolumeControl.tsx`
- Test: `code/web/components/game/__tests__/VolumeControl.test.tsx`

**Context:** Compact mute toggle + volume slider. Click toggles mute, hover reveals slider. Follow existing component patterns (see `code/web/components/game/ObserverPanel.tsx` for a similar compact toggle-to-expand pattern).

**Step 1: Write the tests**

Create `code/web/components/game/__tests__/VolumeControl.test.tsx`:

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { VolumeControl } from '../VolumeControl'

const mockToggleMute = vi.fn()
const mockSetVolume = vi.fn()
let mockIsMuted = false
let mockVolume = 0.7

vi.mock('@/lib/audio/useAudio', () => ({
  useAudio: () => ({
    playSound: vi.fn(),
    volume: mockVolume,
    setVolume: mockSetVolume,
    isMuted: mockIsMuted,
    toggleMute: mockToggleMute,
  }),
}))

describe('VolumeControl', () => {
  beforeEach(() => {
    mockToggleMute.mockClear()
    mockSetVolume.mockClear()
    mockIsMuted = false
    mockVolume = 0.7
  })

  it('renders a mute toggle button', () => {
    render(<VolumeControl />)
    expect(screen.getByRole('button', { name: /mute/i })).toBeDefined()
  })

  it('calls toggleMute on click', () => {
    render(<VolumeControl />)
    fireEvent.click(screen.getByRole('button', { name: /mute/i }))
    expect(mockToggleMute).toHaveBeenCalledOnce()
  })

  it('shows muted icon when muted', () => {
    mockIsMuted = true
    render(<VolumeControl />)
    expect(screen.getByRole('button', { name: /unmute/i })).toBeDefined()
  })

  it('shows volume slider on hover', () => {
    render(<VolumeControl />)
    const container = screen.getByTestId('volume-control')
    fireEvent.mouseEnter(container)
    expect(screen.getByRole('slider')).toBeDefined()
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run components/game/__tests__/VolumeControl.test.tsx`
Expected: FAIL — module not found

**Step 3: Write the implementation**

Create `code/web/components/game/VolumeControl.tsx`:

```tsx
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useState } from 'react'
import { useAudio } from '@/lib/audio/useAudio'

/**
 * Compact volume control: mute toggle button with hover-reveal slider.
 */
export function VolumeControl() {
  const { volume, setVolume, isMuted, toggleMute } = useAudio()
  const [showSlider, setShowSlider] = useState(false)

  return (
    <div
      className="relative inline-flex items-center gap-1"
      data-testid="volume-control"
      onMouseEnter={() => setShowSlider(true)}
      onMouseLeave={() => setShowSlider(false)}
    >
      <button
        type="button"
        onClick={toggleMute}
        className="px-2 py-1.5 text-sm rounded-lg transition-colors bg-gray-700 hover:bg-gray-600 text-gray-200"
        aria-label={isMuted ? 'Unmute sound' : 'Mute sound'}
      >
        {isMuted ? '\u{1F507}' : volume > 0.5 ? '\u{1F50A}' : '\u{1F509}'}
      </button>
      {showSlider && !isMuted && (
        <div className="absolute bottom-full left-0 mb-2 p-2 bg-gray-800 rounded-lg shadow-lg">
          <input
            type="range"
            min="0"
            max="100"
            value={Math.round(volume * 100)}
            onChange={(e) => setVolume(Number(e.target.value) / 100)}
            className="w-24 h-1 accent-green-500 cursor-pointer"
            aria-label="Volume"
          />
        </div>
      )}
    </div>
  )
}
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run components/game/__tests__/VolumeControl.test.tsx`
Expected: 4 tests PASS

**Step 5: Commit**

```bash
git add code/web/components/game/VolumeControl.tsx code/web/components/game/__tests__/VolumeControl.test.tsx
git commit -m "feat(web): add VolumeControl component with mute toggle and slider"
```

---

### Task 7: PokerTable Integration

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx`

**Context:** Wire `useSoundEffects` and `VolumeControl` into the main table component. The `useSoundEffects` hook needs `handHistory` and `actionRequired !== null` from game state. The `VolumeControl` goes near the sit-out button (bottom-left area).

**Step 1: Add imports**

Add to the import block at the top of `PokerTable.tsx` (after the existing imports around lines 10-22):

```typescript
import { useSoundEffects } from '@/lib/audio/useSoundEffects'
import { VolumeControl } from './VolumeControl'
```

**Step 2: Add the useSoundEffects call**

Inside the `PokerTable` function body, after the existing hook calls (after line 68: `const [kickTarget, setKickTarget] = ...`), add:

```typescript
  useSoundEffects(state.handHistory, actionRequired != null)
```

**Step 3: Add VolumeControl to the layout**

Find the sit-out button block (lines 201-212). Add the `VolumeControl` inside the same container, changing:

```tsx
      {/* Task 6.7: sit-out / come-back toggle — shown when player has a seat */}
      {mySeat != null && (
        <div className="absolute bottom-3 left-3 z-20">
          <button
```

To:

```tsx
      {/* Sit-out toggle + volume control — bottom-left */}
      {mySeat != null && (
        <div className="absolute bottom-3 left-3 z-20 flex items-center gap-2">
          <button
```

Then after the closing `</button>` tag and before the closing `</div>` (around line 211), add:

```tsx
          <VolumeControl />
```

If the player has no seat (observer), still show volume control:

After the closing `)}` of the sit-out block, add:

```tsx
      {/* Volume control for observers (no seat) */}
      {mySeat == null && (
        <div className="absolute bottom-3 left-3 z-20">
          <VolumeControl />
        </div>
      )}
```

**Step 4: Run all tests to verify nothing broke**

Run: `cd code/web && npx vitest run`
Expected: All tests PASS

**Step 5: Commit**

```bash
git add code/web/components/game/PokerTable.tsx
git commit -m "feat(web): integrate sound effects and volume control into poker table"
```

---

## Summary

| Task | Description | New Files | Tests |
|------|-------------|-----------|-------|
| 1 | Convert audio files | 52 audio files + script | — |
| 2 | Sound map | `lib/audio/soundMap.ts` | 3 |
| 3 | AudioManager | `lib/audio/AudioManager.ts` | 11 |
| 4 | useAudio hook | `lib/audio/useAudio.ts` | 4 |
| 5 | useSoundEffects hook | `lib/audio/useSoundEffects.ts` | 10 |
| 6 | VolumeControl | `components/game/VolumeControl.tsx` | 4 |
| 7 | PokerTable integration | (modify existing) | — |
| **Total** | | **6 new + 1 modified** | **32** |
