/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect } from 'react'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'
import { useHandReplayState } from '@/lib/game/useHandReplayState'
import { CommunityCards } from './CommunityCards'
import { formatChips } from '@/lib/utils'

interface HandReplayProps {
  entries: HandHistoryEntry[]
  onClose: () => void
}

function formatReplayEntry(entry: HandHistoryEntry): { text: string; highlight: boolean } {
  switch (entry.type) {
    case 'hand_start':
      return { text: `Hand #${entry.handNumber} started`, highlight: false }
    case 'action': {
      const amountStr = entry.amount && entry.amount > 0 ? ` ${formatChips(entry.amount)}` : ''
      return { text: `${entry.playerName ?? 'Player'}: ${entry.action ?? ''}${amountStr}`, highlight: false }
    }
    case 'community':
      return { text: `${entry.round ?? 'Dealt'}: ${(entry.cards ?? []).join(' ')}`, highlight: false }
    case 'result': {
      const winner = entry.winners?.[0]
      if (winner) {
        return {
          text: `${winner.playerName} wins ${formatChips(winner.amount)} (${winner.hand})`,
          highlight: true,
        }
      }
      return { text: 'Hand complete', highlight: false }
    }
    default:
      return { text: '', highlight: false }
  }
}

export function HandReplay({ entries, onClose }: HandReplayProps) {
  const {
    currentStep,
    totalSteps,
    isPlaying,
    speed,
    visibleEntries,
    communityCards,
    next,
    prev,
    play,
    pause,
    setSpeed,
  } = useHandReplayState(entries)

  const handNumber = entries[0]?.handNumber ?? 0
  const progressPercent = totalSteps > 1 ? (currentStep / (totalSteps - 1)) * 100 : 0

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-60"
      onClick={onClose}
      role="dialog"
      aria-label="Hand Replay"
    >
      <div
        className="bg-gray-900 border border-gray-700 rounded-lg p-4 w-[500px] max-h-[80vh] flex flex-col relative"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close button */}
        <button
          type="button"
          onClick={onClose}
          aria-label="Close"
          className="absolute top-2 right-2 text-gray-400 hover:text-white text-lg leading-none"
        >
          x
        </button>

        {/* Title */}
        <h2 className="text-lg font-bold text-white mb-3">Hand #{handNumber} Replay</h2>

        {/* Community cards area */}
        <div className="flex justify-center mb-3 min-h-[50px]">
          <CommunityCards cards={communityCards} cardWidth={50} />
        </div>

        {/* Action log */}
        <div className="overflow-y-auto max-h-48 mb-3 space-y-0.5">
          {visibleEntries.map((entry) => {
            const { text, highlight } = formatReplayEntry(entry)
            return (
              <div
                key={entry.id}
                className={`text-xs ${highlight ? 'text-yellow-400 font-semibold' : 'text-gray-300'}`}
              >
                {text}
              </div>
            )
          })}
        </div>

        {/* Progress bar */}
        <div className="h-1 bg-gray-700 rounded-full mb-3">
          <div
            className="h-full bg-blue-500 rounded-full transition-all"
            style={{ width: `${progressPercent}%` }}
          />
        </div>

        {/* Playback controls */}
        <div className="flex items-center justify-center gap-3">
          <button
            type="button"
            onClick={prev}
            aria-label="Previous step"
            className="px-2 py-1 text-sm text-gray-300 hover:text-white bg-gray-800 rounded"
          >
            Prev
          </button>
          <button
            type="button"
            onClick={isPlaying ? pause : play}
            aria-label={isPlaying ? 'Pause' : 'Play'}
            className="px-3 py-1 text-sm text-white bg-blue-600 hover:bg-blue-500 rounded"
          >
            {isPlaying ? 'Pause' : 'Play'}
          </button>
          <button
            type="button"
            onClick={next}
            aria-label="Next step"
            className="px-2 py-1 text-sm text-gray-300 hover:text-white bg-gray-800 rounded"
          >
            Next
          </button>
          <div className="flex gap-1 ml-3">
            {[1, 2, 4].map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => setSpeed(s)}
                className={`px-2 py-0.5 text-xs rounded ${speed === s ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-400 hover:text-white'}`}
              >
                {s}x
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
