/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useMemo, useState } from 'react'
import { useDashboard } from '@/lib/game/useDashboard'
import { evaluateHand } from '@/lib/poker/handEvaluator'
import { calculateEquity } from '@/lib/poker/equityCalculator'
import { HAND_RANK_NAMES, HandRank } from '@/lib/poker/types'
import { DashboardWidget } from './DashboardWidget'
import { StartingHandsChart } from './StartingHandsChart'

interface DashboardProps {
  holeCards: string[]
  communityCards: string[]
  potSize: number
  callAmount: number
  numOpponents: number
  level?: number
  blinds?: string
  nextLevelIn?: string
  playersRemaining?: number
  totalPlayers?: number
  playerRank?: number
  onClose: () => void
}

export function Dashboard({
  holeCards,
  communityCards,
  potSize,
  callAmount,
  numOpponents,
  level,
  blinds,
  nextLevelIn,
  playersRemaining,
  totalPlayers,
  playerRank,
  onClose,
}: DashboardProps) {
  const { widgets, toggleWidget, moveWidget, resetToDefaults } = useDashboard()
  const [showSettings, setShowSettings] = useState(false)

  const allCards = useMemo(() => [...holeCards, ...communityCards], [holeCards, communityCards])

  const handResult = useMemo(() => {
    if (allCards.length >= 5) {
      return evaluateHand(allCards)
    }
    return null
  }, [allCards])

  const equity = useMemo(() => {
    if (holeCards.length === 2 && numOpponents > 0) {
      return calculateEquity(holeCards, communityCards, numOpponents, 1000)
    }
    return null
  }, [holeCards, communityCards, numOpponents])

  const equityPct = equity ? equity.win + equity.tie : 0
  const potOdds = callAmount > 0 ? (callAmount / (potSize + callAmount)) * 100 : 0

  function isVisible(id: string): boolean {
    return widgets.find((w) => w.id === id)?.visible ?? false
  }

  const widgetRenderers: Record<string, () => React.ReactNode> = {
    'hand-strength': () => (
      <DashboardWidget title="Hand Strength" key="hand-strength">
        {handResult ? (
          <div className="flex items-baseline gap-2">
            <span className="text-sm text-white">{HAND_RANK_NAMES[handResult.rank as HandRank]}</span>
            <span className="text-xs text-gray-400">{equityPct.toFixed(1)}% equity</span>
          </div>
        ) : equity ? (
          <div className="text-sm text-white">{equityPct.toFixed(1)}% equity</div>
        ) : (
          <p className="text-xs text-gray-500 italic">Waiting for cards...</p>
        )}
      </DashboardWidget>
    ),
    'pot-odds': () => (
      <DashboardWidget title="Pot Odds" key="pot-odds">
        {callAmount > 0 ? (
          <p className="text-sm text-white">
            Pot: {potSize} | Call: {callAmount} | Odds: {potOdds.toFixed(1)}%
          </p>
        ) : (
          <p className="text-sm text-green-400">Free check</p>
        )}
      </DashboardWidget>
    ),
    'tournament-info': () => (
      <DashboardWidget title="Tournament Info" key="tournament-info">
        <div className="text-sm text-white space-y-0.5">
          {level != null && <p>Level: {level}</p>}
          {blinds && <p>Blinds: {blinds}</p>}
          {nextLevelIn && <p>Next level: {nextLevelIn}</p>}
          {playersRemaining != null && (
            <p>Players: {playersRemaining}{totalPlayers != null ? ` / ${totalPlayers}` : ''}</p>
          )}
        </div>
      </DashboardWidget>
    ),
    'rank': () => (
      <DashboardWidget title="Rank" key="rank">
        {playerRank != null && totalPlayers != null ? (
          <p className="text-sm text-white">
            Position: {playerRank}{ordinalSuffix(playerRank)} of {totalPlayers} players
          </p>
        ) : (
          <p className="text-xs text-gray-500 italic">Not available</p>
        )}
      </DashboardWidget>
    ),
    'starting-hand': () => (
      <DashboardWidget title="Starting Hand" key="starting-hand">
        {communityCards.length === 0 && holeCards.length === 2 ? (
          <StartingHandsChart currentHand={holeCards} compact />
        ) : (
          <p className="text-xs text-gray-500 italic">Preflop only</p>
        )}
      </DashboardWidget>
    ),
  }

  return (
    <div
      className="w-[260px] h-full flex flex-col rounded-xl bg-black/85 border border-gray-700 shadow-2xl"
      role="complementary"
      aria-label="Dashboard"
    >
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 border-b border-gray-700 shrink-0">
        <h2 className="text-sm font-bold text-white">Dashboard</h2>
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => setShowSettings((v) => !v)}
            className="text-gray-400 hover:text-white text-sm leading-none"
            aria-label="Dashboard settings"
          >
            &#9881;
          </button>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-white text-lg leading-none"
            aria-label="Close dashboard"
          >
            &times;
          </button>
        </div>
      </div>

      {/* Settings panel */}
      {showSettings && (
        <div className="px-3 py-2 border-b border-gray-700 bg-gray-900/50 shrink-0">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-gray-400 uppercase">Settings</span>
            <button
              type="button"
              onClick={resetToDefaults}
              className="text-[10px] text-gray-500 hover:text-white"
            >
              Reset
            </button>
          </div>
          <ul className="space-y-1">
            {widgets.map((w, idx) => (
              <li key={w.id} className="flex items-center gap-1.5 text-xs text-gray-300">
                <input
                  type="checkbox"
                  checked={w.visible}
                  onChange={() => toggleWidget(w.id)}
                  className="w-3 h-3"
                  aria-label={`Toggle ${w.label}`}
                />
                <span className="flex-1">{w.label}</span>
                <button
                  type="button"
                  onClick={() => moveWidget(w.id, 'up')}
                  disabled={idx === 0}
                  className="text-gray-500 hover:text-white disabled:opacity-30 text-[10px]"
                  aria-label={`Move ${w.label} up`}
                >
                  &#9650;
                </button>
                <button
                  type="button"
                  onClick={() => moveWidget(w.id, 'down')}
                  disabled={idx === widgets.length - 1}
                  className="text-gray-500 hover:text-white disabled:opacity-30 text-[10px]"
                  aria-label={`Move ${w.label} down`}
                >
                  &#9660;
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Scrollable widget area */}
      <div className="flex-1 overflow-y-auto">
        {widgets.filter((w) => isVisible(w.id)).map((w) => widgetRenderers[w.id]?.())}
      </div>
    </div>
  )
}

function ordinalSuffix(n: number): string {
  const mod100 = n % 100
  if (mod100 >= 11 && mod100 <= 13) return 'th'
  switch (n % 10) {
    case 1: return 'st'
    case 2: return 'nd'
    case 3: return 'rd'
    default: return 'th'
  }
}
