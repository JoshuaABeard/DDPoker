/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useEffect } from 'react'
import { Card } from './Card'

interface HandRankingsProps {
  onClose: () => void
}

const RANKINGS = [
  { rank: 1, name: 'Royal Flush', description: 'A, K, Q, J, 10 of the same suit', cards: ['As', 'Ks', 'Qs', 'Js', 'Ts'] },
  { rank: 2, name: 'Straight Flush', description: 'Five sequential cards of the same suit', cards: ['9h', '8h', '7h', '6h', '5h'] },
  { rank: 3, name: 'Four of a Kind', description: 'Four cards of the same rank', cards: ['Kd', 'Kc', 'Kh', 'Ks', '2d'] },
  { rank: 4, name: 'Full House', description: 'Three of a kind plus a pair', cards: ['Jd', 'Jc', 'Jh', '8s', '8d'] },
  { rank: 5, name: 'Flush', description: 'Five cards of the same suit, not sequential', cards: ['Ad', 'Jd', '8d', '5d', '2d'] },
  { rank: 6, name: 'Straight', description: 'Five sequential cards, mixed suits', cards: ['Tc', '9d', '8h', '7s', '6c'] },
  { rank: 7, name: 'Three of a Kind', description: 'Three cards of the same rank', cards: ['7d', '7c', '7h', 'Ks', '3d'] },
  { rank: 8, name: 'Two Pair', description: 'Two different pairs', cards: ['Qs', 'Qd', '5c', '5h', 'Ac'] },
  { rank: 9, name: 'One Pair', description: 'Two cards of the same rank', cards: ['9s', '9d', 'Ah', '7c', '4d'] },
  { rank: 10, name: 'High Card', description: 'No matching cards', cards: ['Ad', 'Jc', '8h', '5s', '2c'] },
] as const

export function HandRankings({ onClose }: HandRankingsProps) {
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
      aria-label="Hand Rankings"
    >
      <div
        className="bg-gray-900 border border-gray-700 rounded-lg p-6 max-h-[90vh] overflow-y-auto w-full max-w-2xl mx-4"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-xl font-bold text-white mb-4">Hand Rankings</h2>
        <div className="space-y-3">
          {RANKINGS.map((ranking) => (
            <div key={ranking.rank} className="flex items-center gap-3">
              <span className="text-gray-400 font-mono w-6 text-right shrink-0">{ranking.rank}.</span>
              <div className="min-w-[130px] shrink-0">
                <div className="text-white font-semibold text-sm">{ranking.name}</div>
                <div className="text-gray-400 text-xs">{ranking.description}</div>
              </div>
              <div className="flex gap-0.5 ml-auto shrink-0">
                {ranking.cards.map((card) => (
                  <Card key={card} card={card} width={35} />
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
