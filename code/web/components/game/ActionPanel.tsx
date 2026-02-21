/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useRef, useState } from 'react'
import { BetSlider } from './BetSlider'
import type { ActionOptionsData } from '@/lib/game/types'
import type { PlayerActionData } from '@/lib/game/types'

interface ActionPanelProps {
  options: ActionOptionsData
  potSize: number
  onAction: (action: PlayerActionData) => void
}

function formatChips(n: number): string {
  return new Intl.NumberFormat('en-US').format(n)
}

/**
 * Action panel shown when it's the current player's turn.
 *
 * Keyboard shortcuts (suppressed when any text input is focused):
 * - F: Fold
 * - C: Check / Call
 * - R: Raise (focuses bet slider)
 * - Enter: Confirm action
 */
export function ActionPanel({ options, potSize, onAction }: ActionPanelProps) {
  const [betAmount, setBetAmount] = useState(options.canRaise ? options.minRaise : options.minBet)
  const [pendingAction, setPendingAction] = useState<'bet' | 'raise' | null>(null)
  const panelRef = useRef<HTMLDivElement>(null)

  const showBetSlider = pendingAction === 'bet' || pendingAction === 'raise'
  const sliderMin = pendingAction === 'raise' ? options.minRaise : options.minBet
  const sliderMax = pendingAction === 'raise' ? options.maxRaise : options.maxBet

  // Keyboard shortcuts — suppressed when a text input has focus
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return

      switch (e.key.toLowerCase()) {
        case 'f':
          if (options.canFold) handleFold()
          break
        case 'c':
          if (options.canCheck) handleCheck()
          else if (options.canCall) handleCall()
          break
        case 'r':
          if (options.canRaise) setPendingAction('raise')
          else if (options.canBet) setPendingAction('bet')
          break
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [options])

  function handleFold() {
    onAction({ action: 'FOLD', amount: 0 })
  }
  function handleCheck() {
    onAction({ action: 'CHECK', amount: 0 })
  }
  function handleCall() {
    onAction({ action: 'CALL', amount: options.callAmount })
  }
  function handleBetOrRaise() {
    if (pendingAction === 'bet') {
      onAction({ action: 'BET', amount: betAmount })
    } else {
      onAction({ action: 'RAISE', amount: betAmount })
    }
    setPendingAction(null)
  }
  function handleAllIn() {
    onAction({ action: 'ALL_IN', amount: options.allInAmount })
  }

  return (
    <div
      ref={panelRef}
      className="flex flex-col gap-2 bg-gray-900 bg-opacity-90 rounded-xl p-3 shadow-xl"
      role="region"
      aria-label="Action panel"
    >
      {/* Bet slider — shown when Raise or Bet is pending */}
      {showBetSlider && (
        <div className="mb-1">
          <BetSlider
            min={sliderMin}
            max={sliderMax}
            value={betAmount}
            potSize={potSize}
            onChange={setBetAmount}
          />
        </div>
      )}

      {/* Action buttons */}
      <div className="flex gap-2 justify-center flex-wrap">
        {options.canFold && (
          <button
            type="button"
            onClick={handleFold}
            className="px-4 py-2 bg-red-700 hover:bg-red-600 text-white font-bold rounded-lg transition-colors"
            aria-keyshortcuts="f"
          >
            Fold
          </button>
        )}

        {options.canCheck && (
          <button
            type="button"
            onClick={handleCheck}
            className="px-4 py-2 bg-green-700 hover:bg-green-600 text-white font-bold rounded-lg transition-colors"
            aria-keyshortcuts="c"
          >
            Check
          </button>
        )}

        {options.canCall && (
          <button
            type="button"
            onClick={handleCall}
            className="px-4 py-2 bg-blue-700 hover:bg-blue-600 text-white font-bold rounded-lg transition-colors"
            aria-keyshortcuts="c"
          >
            Call {formatChips(options.callAmount)}
          </button>
        )}

        {options.canBet && !showBetSlider && (
          <button
            type="button"
            onClick={() => {
              setPendingAction('bet')
              setBetAmount(options.minBet)
            }}
            className="px-4 py-2 bg-yellow-700 hover:bg-yellow-600 text-white font-bold rounded-lg transition-colors"
          >
            Bet
          </button>
        )}

        {options.canRaise && !showBetSlider && (
          <button
            type="button"
            onClick={() => {
              setPendingAction('raise')
              setBetAmount(options.minRaise)
            }}
            className="px-4 py-2 bg-yellow-700 hover:bg-yellow-600 text-white font-bold rounded-lg transition-colors"
            aria-keyshortcuts="r"
          >
            Raise
          </button>
        )}

        {showBetSlider && (
          <>
            <button
              type="button"
              onClick={handleBetOrRaise}
              className="px-4 py-2 bg-yellow-500 hover:bg-yellow-400 text-black font-bold rounded-lg transition-colors"
            >
              Confirm {formatChips(betAmount)}
            </button>
            <button
              type="button"
              onClick={() => setPendingAction(null)}
              className="px-3 py-2 bg-gray-600 hover:bg-gray-500 text-white rounded-lg transition-colors text-sm"
            >
              Cancel
            </button>
          </>
        )}

        {options.canAllIn && !showBetSlider && (
          <button
            type="button"
            onClick={handleAllIn}
            className="px-4 py-2 bg-purple-700 hover:bg-purple-600 text-white font-bold rounded-lg transition-colors"
          >
            All-In {formatChips(options.allInAmount)}
          </button>
        )}
      </div>
    </div>
  )
}
