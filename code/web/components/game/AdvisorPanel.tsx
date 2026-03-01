/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { HAND_RANK_NAMES, HandRank } from '@/lib/poker/types'
import { StartingHandsChart } from './StartingHandsChart'
import type { AdvisorData } from '@/lib/game/types'

interface AdvisorPanelProps {
  /** Server-computed advisor data (null before first update) */
  advisorData: AdvisorData | null
  /** Current hole cards (for starting hands chart) */
  holeCards: string[]
  onClose: () => void
}

const STRENGTH_COLORS: Record<number, string> = {
  0: 'bg-red-600',
  1: 'bg-red-500',
  2: 'bg-orange-500',
  3: 'bg-orange-400',
  4: 'bg-yellow-500',
  5: 'bg-yellow-400',
  6: 'bg-green-500',
  7: 'bg-green-400',
  8: 'bg-emerald-400',
  9: 'bg-emerald-300',
}

const RECOMMENDATION_COLORS: Record<string, string> = {
  'Raise or Call': 'text-green-400',
  'Consider calling': 'text-yellow-400',
  'Consider folding': 'text-red-400',
}

export function AdvisorPanel({
  advisorData,
  holeCards,
  onClose,
}: AdvisorPanelProps) {
  if (!advisorData) {
    return (
      <div
        className="absolute top-12 right-64 z-30 w-[280px] max-h-[calc(100%-4rem)] overflow-y-auto rounded-xl bg-black/85 border border-gray-700 shadow-2xl"
        role="complementary"
        aria-label="AI Advisor"
      >
        <div className="flex items-center justify-between px-4 py-2 border-b border-gray-700">
          <h2 className="text-sm font-bold text-white">AI Advisor</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-white text-lg leading-none"
            aria-label="Close advisor"
          >
            &times;
          </button>
        </div>
        <div className="p-4">
          <p className="text-sm text-gray-500 italic">Waiting for data...</p>
        </div>
      </div>
    )
  }

  const { handRank, handDescription, equity, potOdds, recommendation, startingHandCategory, startingHandNotation } = advisorData

  // Determine recommendation color (default to green for free-check type messages)
  const recommendationColor = RECOMMENDATION_COLORS[recommendation] ?? 'text-green-400'

  return (
    <div
      className="absolute top-12 right-64 z-30 w-[280px] max-h-[calc(100%-4rem)] overflow-y-auto rounded-xl bg-black/85 border border-gray-700 shadow-2xl"
      role="complementary"
      aria-label="AI Advisor"
    >
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-2 border-b border-gray-700">
        <h2 className="text-sm font-bold text-white">AI Advisor</h2>
        <button
          type="button"
          onClick={onClose}
          className="text-gray-400 hover:text-white text-lg leading-none"
          aria-label="Close advisor"
        >
          &times;
        </button>
      </div>

      <div className="p-4 flex flex-col gap-4">
        {/* Section 1: Hand Strength */}
        <section>
          <h3 className="text-xs font-semibold text-gray-400 uppercase mb-1">Hand Strength</h3>
          {handRank != null && handDescription ? (
            <div>
              <p className="text-sm text-white mb-1">{handDescription}</p>
              <div className="h-2 bg-gray-700 rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full ${STRENGTH_COLORS[handRank] ?? 'bg-gray-500'}`}
                  style={{ width: `${((handRank + 1) / 10) * 100}%` }}
                />
              </div>
              <p className="text-[10px] text-gray-500 mt-0.5">{HAND_RANK_NAMES[handRank as HandRank]}</p>
            </div>
          ) : (
            <p className="text-sm text-gray-500 italic">Waiting for flop...</p>
          )}
        </section>

        {/* Section 2: Equity */}
        <section>
          <h3 className="text-xs font-semibold text-gray-400 uppercase mb-1">Equity</h3>
          {equity > 0 ? (
            <p className="text-sm text-white">
              Equity: {equity.toFixed(1)}%
            </p>
          ) : (
            <p className="text-sm text-gray-500 italic">No equity data</p>
          )}
        </section>

        {/* Section 3: Pot Odds */}
        <section>
          <h3 className="text-xs font-semibold text-gray-400 uppercase mb-1">Pot Odds</h3>
          {potOdds === 0 ? (
            <p className="text-sm text-green-400">Free check</p>
          ) : equity > 0 ? (
            <div>
              <p className="text-sm text-white">
                Pot Odds: {potOdds.toFixed(1)}% — Equity: {equity.toFixed(1)}% —{' '}
                <span className={equity > potOdds ? 'text-green-400' : 'text-red-400'}>
                  {equity > potOdds ? '+EV' : '-EV'} Call
                </span>
              </p>
            </div>
          ) : (
            <p className="text-sm text-gray-500">Pot Odds: {potOdds.toFixed(1)}%</p>
          )}
        </section>

        {/* Section 4: Starting Hand (preflop only) */}
        {startingHandNotation && startingHandCategory && (
          <section>
            <h3 className="text-xs font-semibold text-gray-400 uppercase mb-1">Starting Hand</h3>
            <p className="text-sm text-white mb-2">
              {startingHandNotation} — <span className="capitalize">{startingHandCategory}</span>
            </p>
            <StartingHandsChart currentHand={holeCards} compact />
          </section>
        )}

        {/* Section 5: Recommendation */}
        {recommendation && (
          <section>
            <h3 className="text-xs font-semibold text-gray-400 uppercase mb-1">Recommendation</h3>
            <p className={`text-sm font-semibold ${recommendationColor}`}>{recommendation}</p>
          </section>
        )}
      </div>
    </div>
  )
}
