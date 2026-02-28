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
