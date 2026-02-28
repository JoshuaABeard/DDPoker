/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useMemo } from 'react'
import { evaluateHand } from '@/lib/poker/handEvaluator'
import { calculateEquity } from '@/lib/poker/equityCalculator'
import { holeCardsToGrid, STARTING_HAND_CATEGORIES, HAND_LABELS } from '@/lib/poker/startingHands'
import { HAND_RANK_NAMES, HandRank } from '@/lib/poker/types'
import { StartingHandsChart } from './StartingHandsChart'

interface AdvisorPanelProps {
  /** Current hole cards */
  holeCards: string[]
  /** Current community cards */
  communityCards: string[]
  /** Current pot size */
  potSize: number
  /** Amount needed to call (0 if can check) */
  callAmount: number
  /** Number of active opponents */
  numOpponents: number
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

export function AdvisorPanel({
  holeCards,
  communityCards,
  potSize,
  callAmount,
  numOpponents,
  onClose,
}: AdvisorPanelProps) {
  const allCards = useMemo(() => [...holeCards, ...communityCards], [holeCards, communityCards])

  // Hand evaluation (only meaningful with 5+ cards)
  const handResult = useMemo(() => {
    if (allCards.length >= 5) {
      return evaluateHand(allCards)
    }
    return null
  }, [allCards])

  // Equity calculation (2000 iterations for responsiveness)
  const equity = useMemo(() => {
    if (holeCards.length === 2 && numOpponents > 0) {
      return calculateEquity(holeCards, communityCards, numOpponents, 2000)
    }
    return null
  }, [holeCards, communityCards, numOpponents])

  const equityPct = equity ? equity.win + equity.tie : 0

  // Pot odds
  const potOdds = callAmount > 0 ? (callAmount / (potSize + callAmount)) * 100 : 0

  // Starting hand info (preflop only)
  const startingHand = useMemo(() => {
    if (communityCards.length === 0 && holeCards.length === 2) {
      const { row, col } = holeCardsToGrid(holeCards)
      const category = STARTING_HAND_CATEGORIES[row][col]
      const r1 = HAND_LABELS[row]
      const r2 = HAND_LABELS[col]
      let notation: string
      if (row === col) notation = `${r1}${r2}`
      else if (col > row) notation = `${r1}${r2}s`
      else notation = `${r2}${r1}o`
      return { notation, category }
    }
    return null
  }, [holeCards, communityCards])

  // Recommendation
  const recommendation = useMemo(() => {
    if (callAmount === 0) {
      return { text: 'Check — no cost to see more cards', color: 'text-green-400' }
    }
    if (!equity) return null
    const diff = equityPct - potOdds
    if (diff > 10) return { text: 'Raise or Call', color: 'text-green-400' }
    if (diff > 0) return { text: 'Consider calling', color: 'text-yellow-400' }
    return { text: 'Consider folding', color: 'text-red-400' }
  }, [callAmount, equity, equityPct, potOdds])

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
          {handResult ? (
            <div>
              <p className="text-sm text-white mb-1">{handResult.description}</p>
              <div className="h-2 bg-gray-700 rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full ${STRENGTH_COLORS[handResult.rank] ?? 'bg-gray-500'}`}
                  style={{ width: `${((handResult.rank + 1) / 10) * 100}%` }}
                />
              </div>
              <p className="text-[10px] text-gray-500 mt-0.5">{HAND_RANK_NAMES[handResult.rank as HandRank]}</p>
            </div>
          ) : (
            <p className="text-sm text-gray-500 italic">Waiting for flop...</p>
          )}
        </section>

        {/* Section 2: Equity */}
        <section>
          <h3 className="text-xs font-semibold text-gray-400 uppercase mb-1">Equity</h3>
          {equity ? (
            <p className="text-sm text-white">
              Equity: {equityPct.toFixed(1)}%{' '}
              <span className="text-gray-400">(vs {numOpponents} opponent{numOpponents !== 1 ? 's' : ''})</span>
            </p>
          ) : (
            <p className="text-sm text-gray-500 italic">Need hole cards and opponents</p>
          )}
        </section>

        {/* Section 3: Pot Odds */}
        <section>
          <h3 className="text-xs font-semibold text-gray-400 uppercase mb-1">Pot Odds</h3>
          {callAmount === 0 ? (
            <p className="text-sm text-green-400">Free check</p>
          ) : equity ? (
            <div>
              <p className="text-sm text-white">
                Pot Odds: {potOdds.toFixed(1)}% — Equity: {equityPct.toFixed(1)}% —{' '}
                <span className={equityPct > potOdds ? 'text-green-400' : 'text-red-400'}>
                  {equityPct > potOdds ? '+EV' : '-EV'} Call
                </span>
              </p>
            </div>
          ) : (
            <p className="text-sm text-gray-500">Call: {callAmount} into {potSize} pot</p>
          )}
        </section>

        {/* Section 4: Starting Hand (preflop only) */}
        {startingHand && (
          <section>
            <h3 className="text-xs font-semibold text-gray-400 uppercase mb-1">Starting Hand</h3>
            <p className="text-sm text-white mb-2">
              {startingHand.notation} — <span className="capitalize">{startingHand.category}</span>
            </p>
            <StartingHandsChart currentHand={holeCards} compact />
          </section>
        )}

        {/* Section 5: Recommendation */}
        {recommendation && (
          <section>
            <h3 className="text-xs font-semibold text-gray-400 uppercase mb-1">Recommendation</h3>
            <p className={`text-sm font-semibold ${recommendation.color}`}>{recommendation.text}</p>
          </section>
        )}
      </div>
    </div>
  )
}
