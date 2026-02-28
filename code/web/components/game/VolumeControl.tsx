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
