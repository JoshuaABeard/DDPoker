/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useState } from 'react'
import { CardPicker } from './CardPicker'
import { Card } from './Card'
import { calculateEquity } from '@/lib/poker/equityCalculator'
import type { EquityResult } from '@/lib/poker/types'

interface SimulatorProps {
  /** Current hole cards from the game (for "Load from Game" button) */
  currentHoleCards?: string[]
  /** Current community cards from the game */
  currentCommunityCards?: string[]
  onClose: () => void
}

type SlotType = { kind: 'hole'; index: number } | { kind: 'community'; index: number }

export function Simulator({ currentHoleCards, currentCommunityCards, onClose }: SimulatorProps) {
  const [holeCards, setHoleCards] = useState<(string | null)[]>([null, null])
  const [communityCards, setCommunityCards] = useState<(string | null)[]>([null, null, null, null, null])
  const [opponents, setOpponents] = useState(1)
  const [results, setResults] = useState<EquityResult | null>(null)
  const [isCalculating, setIsCalculating] = useState(false)
  const [activeSlot, setActiveSlot] = useState<SlotType | null>(null)

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        if (activeSlot) {
          setActiveSlot(null)
        } else {
          onClose()
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, activeSlot])

  const allSelected = [
    ...holeCards.filter((c): c is string => c !== null),
    ...communityCards.filter((c): c is string => c !== null),
  ]

  const holeCardCount = holeCards.filter((c) => c !== null).length
  const canCalculate = holeCardCount === 2 && !isCalculating

  function handleSlotClick(slot: SlotType) {
    const card = slot.kind === 'hole' ? holeCards[slot.index] : communityCards[slot.index]
    if (card) return // slot already filled — use clear button instead
    setActiveSlot(slot)
  }

  function handleCardSelect(card: string) {
    if (!activeSlot) return
    if (activeSlot.kind === 'hole') {
      setHoleCards((prev) => {
        const next = [...prev]
        next[activeSlot.index] = card
        return next
      })
    } else {
      setCommunityCards((prev) => {
        const next = [...prev]
        next[activeSlot.index] = card
        return next
      })
    }
    setActiveSlot(null)
    setResults(null)
  }

  function handleClearSlot(slot: SlotType) {
    if (slot.kind === 'hole') {
      setHoleCards((prev) => {
        const next = [...prev]
        next[slot.index] = null
        return next
      })
    } else {
      setCommunityCards((prev) => {
        const next = [...prev]
        next[slot.index] = null
        return next
      })
    }
    setResults(null)
  }

  function handleClearAll() {
    setHoleCards([null, null])
    setCommunityCards([null, null, null, null, null])
    setResults(null)
    setActiveSlot(null)
  }

  function handleLoadFromGame() {
    if (currentHoleCards && currentHoleCards.length >= 2) {
      setHoleCards([currentHoleCards[0], currentHoleCards[1]])
    }
    if (currentCommunityCards) {
      const next: (string | null)[] = [null, null, null, null, null]
      for (let i = 0; i < Math.min(currentCommunityCards.length, 5); i++) {
        next[i] = currentCommunityCards[i]
      }
      setCommunityCards(next)
    }
    setResults(null)
    setActiveSlot(null)
  }

  function handleCalculate() {
    const hole = holeCards.filter((c): c is string => c !== null)
    if (hole.length < 2) return
    const community = communityCards.filter((c): c is string => c !== null)

    setIsCalculating(true)
    // Run synchronously (Web Worker can be added later)
    const result = calculateEquity(hole, community, opponents, 10000)
    setResults(result)
    setIsCalculating(false)
  }

  const hasGameCards =
    (currentHoleCards && currentHoleCards.length >= 2) ||
    (currentCommunityCards && currentCommunityCards.length > 0)

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-60"
      onClick={onClose}
      role="dialog"
      aria-label="Poker Simulator"
    >
      <div
        className="bg-gray-900 border border-gray-700 rounded-xl shadow-2xl p-6 max-h-[90vh] overflow-y-auto w-full max-w-lg mx-4"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-white">Poker Simulator</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-white text-xl leading-none"
            aria-label="Close simulator"
          >
            &times;
          </button>
        </div>

        {/* Hole cards */}
        <div className="mb-4">
          <div className="text-sm text-gray-400 mb-2">Hole Cards</div>
          <div className="flex gap-2">
            {holeCards.map((card, i) => (
              <CardSlot
                key={`hole-${i}`}
                card={card}
                label={`Hole card ${i + 1}`}
                isActive={activeSlot?.kind === 'hole' && activeSlot.index === i}
                onClick={() => handleSlotClick({ kind: 'hole', index: i })}
                onClear={() => handleClearSlot({ kind: 'hole', index: i })}
              />
            ))}
          </div>
        </div>

        {/* Community cards */}
        <div className="mb-4">
          <div className="text-sm text-gray-400 mb-2">Community Cards</div>
          <div className="flex gap-2">
            {communityCards.map((card, i) => (
              <CardSlot
                key={`community-${i}`}
                card={card}
                label={`Community card ${i + 1}`}
                isActive={activeSlot?.kind === 'community' && activeSlot.index === i}
                onClick={() => handleSlotClick({ kind: 'community', index: i })}
                onClear={() => handleClearSlot({ kind: 'community', index: i })}
              />
            ))}
          </div>
        </div>

        {/* Card picker */}
        {activeSlot && (
          <div className="mb-4">
            <CardPicker
              usedCards={allSelected}
              onSelect={handleCardSelect}
              onClose={() => setActiveSlot(null)}
            />
          </div>
        )}

        {/* Controls */}
        <div className="flex items-center gap-4 mb-4">
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-400">Opponents:</span>
            <div className="flex gap-1">
              <button
                type="button"
                onClick={() => setOpponents((v) => Math.max(1, v - 1))}
                className="w-7 h-7 rounded bg-gray-700 hover:bg-gray-600 text-white text-sm font-semibold"
                aria-label="Decrease opponents"
              >
                -
              </button>
              <span className="w-7 h-7 flex items-center justify-center text-white text-sm font-semibold" data-testid="opponent-count">
                {opponents}
              </span>
              <button
                type="button"
                onClick={() => setOpponents((v) => Math.min(9, v + 1))}
                className="w-7 h-7 rounded bg-gray-700 hover:bg-gray-600 text-white text-sm font-semibold"
                aria-label="Increase opponents"
              >
                +
              </button>
            </div>
          </div>

          <div className="flex gap-2 ml-auto">
            {hasGameCards && (
              <button
                type="button"
                onClick={handleLoadFromGame}
                className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-gray-700 hover:bg-gray-600 text-gray-200"
              >
                Load from Game
              </button>
            )}
            <button
              type="button"
              onClick={handleClearAll}
              className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-gray-700 hover:bg-gray-600 text-gray-200"
            >
              Clear All
            </button>
          </div>
        </div>

        {/* Calculate button */}
        <button
          type="button"
          onClick={handleCalculate}
          disabled={!canCalculate}
          className={`w-full py-2 rounded-lg font-semibold text-sm mb-4 ${
            canCalculate
              ? 'bg-green-700 hover:bg-green-600 text-white'
              : 'bg-gray-700 text-gray-500 cursor-not-allowed'
          }`}
        >
          {isCalculating ? 'Calculating...' : 'Calculate'}
        </button>

        {/* Results */}
        {results && (
          <div className="bg-gray-800 rounded-lg p-4" data-testid="results">
            <div className="grid grid-cols-3 gap-4 text-center mb-3">
              <div>
                <div className="text-green-400 text-2xl font-bold">{results.win.toFixed(1)}%</div>
                <div className="text-gray-400 text-xs">Win</div>
              </div>
              <div>
                <div className="text-yellow-400 text-2xl font-bold">{results.tie.toFixed(1)}%</div>
                <div className="text-gray-400 text-xs">Tie</div>
              </div>
              <div>
                <div className="text-red-400 text-2xl font-bold">{results.loss.toFixed(1)}%</div>
                <div className="text-gray-400 text-xs">Loss</div>
              </div>
            </div>
            <div className="text-center text-gray-500 text-xs">
              Simulations: {results.iterations.toLocaleString()}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

function CardSlot({
  card,
  label,
  isActive,
  onClick,
  onClear,
}: {
  card: string | null
  label: string
  isActive: boolean
  onClick: () => void
  onClear: () => void
}) {
  return (
    <div className="relative" data-testid="card-slot">
      <button
        type="button"
        onClick={onClick}
        className={`w-12 h-[67px] rounded-md border-2 flex items-center justify-center ${
          isActive
            ? 'border-blue-500 bg-gray-700'
            : card
              ? 'border-gray-600 bg-transparent'
              : 'border-dashed border-gray-600 bg-gray-800 hover:border-gray-400'
        }`}
        aria-label={label}
      >
        {card ? (
          <Card card={card} width={40} />
        ) : (
          <span className="text-gray-500 text-lg">+</span>
        )}
      </button>
      {card && (
        <button
          type="button"
          onClick={onClear}
          className="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-gray-600 hover:bg-red-600 text-white text-[10px] leading-none flex items-center justify-center"
          aria-label={`Clear ${label}`}
        >
          &times;
        </button>
      )}
    </div>
  )
}
