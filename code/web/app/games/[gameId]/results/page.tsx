/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import type { StandingData } from '@/lib/game/types'

function formatChips(n: number): string {
  return new Intl.NumberFormat('en-US').format(n)
}

function ordinal(n: number): string {
  const s = ['th', 'st', 'nd', 'rd']
  const v = n % 100
  return n + (s[(v - 20) % 10] || s[v] || s[0])
}

interface ResultsViewProps {
  standings: StandingData[]
  myPlayerId: number | null
}

function ResultsView({ standings, myPlayerId }: ResultsViewProps) {
  const router = useRouter()

  const myStanding = myPlayerId != null
    ? standings.find((s) => s.playerId === myPlayerId)
    : undefined

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-2">Game Results</h1>

      {myStanding && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6 text-center">
          <p className="text-lg font-semibold text-blue-800">
            You finished {ordinal(myStanding.position)}
          </p>
          {myStanding.prize > 0 && (
            <p className="text-blue-700 text-sm mt-1">
              Prize: {formatChips(myStanding.prize)}
            </p>
          )}
        </div>
      )}

      <div className="rounded-lg border border-gray-200 shadow-sm overflow-hidden mb-6">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="text-center px-4 py-3 font-semibold text-gray-700 w-16">Place</th>
              <th className="text-left px-4 py-3 font-semibold text-gray-700">Player</th>
              <th className="text-right px-4 py-3 font-semibold text-gray-700">Prize</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {standings.map((standing) => (
              <tr
                key={standing.playerId}
                className={[
                  'transition-colors',
                  standing.playerId === myPlayerId ? 'bg-blue-50' : 'hover:bg-gray-50',
                ].join(' ')}
              >
                <td className="px-4 py-3 text-center font-bold text-gray-700">
                  {ordinal(standing.position)}
                </td>
                {/* Player name — text node only (XSS safe) */}
                <td className="px-4 py-3 font-medium text-gray-900">
                  {standing.playerName}
                  {standing.playerId === myPlayerId && (
                    <span className="ml-2 text-xs text-blue-600 font-normal">(you)</span>
                  )}
                </td>
                <td className="px-4 py-3 text-right text-gray-700">
                  {standing.prize > 0 ? formatChips(standing.prize) : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex gap-3">
        <Link
          href="/games"
          className="px-5 py-2 bg-green-700 hover:bg-green-600 text-white font-bold rounded-lg transition-colors text-sm"
        >
          Game Lobby
        </Link>
        <button
          type="button"
          onClick={() => router.push('/games/create?practice=true')}
          className="px-5 py-2 bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold rounded-lg transition-colors text-sm"
        >
          Play Again (Practice)
        </button>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page — reads standings from sessionStorage (written by the play page just
// before navigating here). Falls back to a lobby link if accessed directly.
// ---------------------------------------------------------------------------

export default function ResultsPage() {
  const params = useParams()
  // gameId available if a future REST fallback is needed
  void params.gameId

  const [standings, setStandings] = useState<StandingData[] | null>(null)
  const [myPlayerId, setMyPlayerId] = useState<number | null>(null)

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem('ddpoker_results')
      if (raw) {
        const parsed = JSON.parse(raw) as { standings: StandingData[]; myPlayerId: number | null }
        setStandings(parsed.standings)
        setMyPlayerId(parsed.myPlayerId)
        sessionStorage.removeItem('ddpoker_results')
      }
    } catch {
      // Ignore parse errors or unavailable sessionStorage
    }
  }, [])

  if (standings && standings.length > 0) {
    return <ResultsView standings={standings} myPlayerId={myPlayerId} />
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-4">Game Results</h1>
      <p className="text-gray-600 mb-6">
        The game has ended. Results are shown here after the game completes.
        Return to the lobby to find or create another game.
      </p>
      <Link
        href="/games"
        className="px-5 py-2 bg-green-700 hover:bg-green-600 text-white font-bold rounded-lg transition-colors text-sm"
      >
        Game Lobby
      </Link>
    </div>
  )
}
