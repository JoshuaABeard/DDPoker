# Web Client Audio & Immersion — Design

**Status:** APPROVED (2026-02-27)
**Depends on:** None (can be implemented independently)
**Scope:** Gameplay sound effects for the web client using Web Audio API

---

## Context

The desktop Java client has a full audio system with 30 gameplay sounds and 4 music tracks. The web client currently has no audio at all. This design adds gameplay sound effects to close the audio gap. Background music is deferred to a future pass.

## Audio File Conversion

**Source:** 30 gameplay files in `code/poker/src/main/resources/config/poker/audio/` (WAV/AIF format, ~3 MB total)

**Target:** Dual-format MP3 + OGG in `code/web/public/audio/` for cross-browser support (MP3 for Safari, OGG for Firefox/Chrome)

**Files to convert (excluding 4 music tracks):**

| Category | Files |
|----------|-------|
| Betting | `bet1`-`bet10` (10 randomized variants) |
| Cards | `shuffle1`-`shuffle3`, `shuffle1_short`-`shuffle3_short` (6 files) |
| Actions | `check`, `raise2` |
| UI | `button-click`, `bell`, `bell2`, `attention` |
| Events | `cheers1`-`cheers4`, `camera` |

**Skipped (music, deferred):** `music2a`, `music2b`, `music3a`, `music3b`

**Conversion:** One-time ffmpeg script, targeting ~128kbps MP3 and ~96kbps OGG.

## AudioManager (Singleton)

Core audio engine wrapping Web Audio API.

**Lifecycle:**
- `AudioContext` created lazily on first user gesture (required by browser autoplay policy)
- Audio buffers pre-loaded after context creation
- `GainNode` controls master volume

**API:**
- `play(soundName: string)` — plays the sound; for randomized sounds (bet, shuffle, cheers), picks a random variant
- `setVolume(value: number)` — sets master volume (0.0–1.0)
- `setMuted(muted: boolean)` — mutes/unmutes all audio
- `getVolume(): number` / `isMuted(): boolean` — read current state

**Persistence:** Volume and mute state stored in localStorage key `ddpoker-audio` as `{ volume: number, muted: boolean }`.

## Sound Map

Maps logical sound names to audio file arrays. Similar to desktop's `audio.xml`.

```typescript
const soundMap: Record<string, string[]> = {
  bet: ['bet1', 'bet2', ..., 'bet10'],
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
```

When a name maps to multiple files, `play()` picks one at random.

## useAudio Hook

React wrapper around AudioManager singleton.

```typescript
function useAudio(): {
  playSound: (name: string) => void
  volume: number
  setVolume: (v: number) => void
  isMuted: boolean
  toggleMute: () => void
}
```

Triggers AudioContext initialization on mount (deferred until user gesture).

## useSoundEffects Hook

Watches game state changes and plays appropriate sounds automatically.

**Event-to-sound mapping:**

| Game Event | Sound | Detection |
|------------|-------|-----------|
| Player bets/calls | `bet` | New action in hand history with type BET/CALL |
| Player checks | `check` | New action with type CHECK |
| Player raises | `raise` | New action with type RAISE |
| Cards dealt (new round) | `shuffle` | Community cards change or new hand starts |
| Player wins pot | `cheers` | New winner entry in hand history |
| Your turn to act | `bell` | `isMyTurn` transitions from false to true |
| Player folds | `click` | New action with type FOLD |

**Implementation:** Uses `useRef` to track previous state, compares on each render to detect new events. Only plays sounds for events that happened since last check.

## VolumeControl Component

Compact volume UI for the poker table.

**Default state:** Speaker icon button (🔊 when unmuted, 🔇 when muted)
**Click:** Toggles mute/unmute
**Hover/focus:** Shows a small vertical volume slider popover
**Position:** In PokerTable toolbar area, near existing toggle buttons

~40 lines, uses useAudio hook.

## Files

| File | Action | Lines |
|------|--------|-------|
| `code/web/public/audio/*.mp3, *.ogg` | Create | 52 files (26 sounds × 2 formats) |
| `code/web/lib/audio/AudioManager.ts` | Create | ~100 |
| `code/web/lib/audio/soundMap.ts` | Create | ~40 |
| `code/web/lib/audio/useAudio.ts` | Create | ~30 |
| `code/web/lib/audio/useSoundEffects.ts` | Create | ~60 |
| `code/web/components/game/VolumeControl.tsx` | Create | ~40 |
| `code/web/components/game/PokerTable.tsx` | Modify | +5 (add VolumeControl + useSoundEffects) |

## Architecture Notes

- All new code, no existing functionality changed
- No new npm dependencies (Web Audio API is native)
- AudioManager is a singleton — only one AudioContext per page
- Sound effects are purely client-side; no server interaction needed
- Audio files served as static assets from `public/audio/`
- Browser autoplay policy handled by deferring AudioContext creation to first user gesture
