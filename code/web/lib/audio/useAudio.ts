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
