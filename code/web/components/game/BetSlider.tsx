/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { formatChips } from '@/lib/utils'

interface BetSliderProps {
  min: number
  max: number
  value: number
  potSize: number
  onChange: (amount: number) => void
}

/**
 * Range slider for bet/raise amounts with preset buttons.
 *
 * Uses integer arithmetic only — no floating-point rounding errors.
 */
export function BetSlider({ min, max, value, potSize, onChange }: BetSliderProps) {
  const clamp = (v: number) => Math.min(max, Math.max(min, v))

  // Preset buttons: Min, ½ Pot, Pot, All-In
  const halfPot = clamp(Math.floor(potSize / 2))
  const pot = clamp(potSize)

  return (
    <div className="flex flex-col gap-2 w-full">
      {/* Preset buttons */}
      <div className="flex gap-1 justify-center">
        <button
          type="button"
          onClick={() => onChange(min)}
          className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-white rounded transition-colors"
        >
          Min
        </button>
        {halfPot > min && halfPot < max && (
          <button
            type="button"
            onClick={() => onChange(halfPot)}
            className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-white rounded transition-colors"
          >
            ½ Pot
          </button>
        )}
        {pot > min && pot < max && (
          <button
            type="button"
            onClick={() => onChange(pot)}
            className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-white rounded transition-colors"
          >
            Pot
          </button>
        )}
        <button
          type="button"
          onClick={() => onChange(max)}
          className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-white rounded transition-colors"
        >
          All-In
        </button>
      </div>

      {/* Range input + text input */}
      <div className="flex items-center gap-2">
        <input
          type="range"
          min={min}
          max={max}
          step={1}
          value={value}
          onChange={(e) => onChange(clamp(parseInt(e.target.value, 10)))}
          className="flex-1 accent-yellow-400"
          aria-label={`Bet amount: ${formatChips(value)}`}
        />
        <input
          type="number"
          min={min}
          max={max}
          value={value}
          onChange={(e) => {
            const parsed = parseInt(e.target.value, 10)
            if (!isNaN(parsed)) onChange(Math.min(max, Math.max(min, parsed)))
          }}
          className="w-20 text-center bg-gray-700 text-white rounded px-2 py-1 text-sm"
          aria-label="Enter bet amount"
        />
      </div>
    </div>
  )
}
