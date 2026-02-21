/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

interface BetSliderProps {
  min: number
  max: number
  value: number
  potSize: number
  onChange: (amount: number) => void
}

function formatChips(n: number): string {
  return new Intl.NumberFormat('en-US').format(n)
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

  const presets = [
    { label: 'Min', amount: min },
    { label: '½ Pot', amount: halfPot },
    { label: 'Pot', amount: pot },
    { label: 'All-In', amount: max },
  ]

  return (
    <div className="flex flex-col gap-2 w-full">
      {/* Preset buttons */}
      <div className="flex gap-1 justify-center">
        {presets.map(({ label, amount }) => (
          <button
            key={label}
            type="button"
            onClick={() => onChange(amount)}
            className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 text-white rounded transition-colors"
          >
            {label}
          </button>
        ))}
      </div>

      {/* Range input */}
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
        <div className="text-yellow-300 text-sm font-bold min-w-[70px] text-right">
          {formatChips(value)}
        </div>
      </div>
    </div>
  )
}
