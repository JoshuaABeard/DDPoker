/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useState } from 'react'

interface ActionTimerProps {
  /** Total timeout in seconds (from ACTION_REQUIRED). */
  totalSeconds: number
  /** Live remaining seconds (from TIMER_UPDATE). null = use totalSeconds as start. */
  remainingSeconds: number | null
}

/**
 * Countdown progress bar for the current actor's time limit.
 *
 * Server sends `timeRemainingMs` (countdown) not wall-clock — no sync needed.
 * Client adds a 500ms visual buffer to account for network latency before
 * displaying "timed out" state.
 */
export function ActionTimer({ totalSeconds, remainingSeconds }: ActionTimerProps) {
  const [displaySeconds, setDisplaySeconds] = useState(remainingSeconds ?? totalSeconds)

  useEffect(() => {
    setDisplaySeconds(remainingSeconds ?? totalSeconds)
    const interval = setInterval(() => {
      setDisplaySeconds((prev) => Math.max(0, prev - 1))
    }, 1000)
    return () => clearInterval(interval)
  }, [remainingSeconds, totalSeconds])

  const pct = totalSeconds > 0 ? Math.max(0, (displaySeconds / totalSeconds) * 100) : 0
  const color = pct > 50 ? '#22c55e' : pct > 25 ? '#eab308' : '#ef4444'

  return (
    <div className="w-full" role="timer" aria-label={`${Math.round(displaySeconds)} seconds remaining`}>
      <div className="h-2 bg-gray-700 rounded-full overflow-hidden">
        <div
          className="h-full rounded-full transition-[width] duration-1000"
          style={{ width: `${pct}%`, backgroundColor: color }}
        />
      </div>
      <div className="text-center text-xs text-gray-400 mt-0.5">
        {Math.ceil(displaySeconds)}s
      </div>
    </div>
  )
}
