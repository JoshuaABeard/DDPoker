/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { gameServerApi } from '@/lib/api'
import type { GameSummaryDto } from '@/lib/game/types'
import { PasswordDialog } from '@/components/game/PasswordDialog'

type TabFilter = 'WAITING_FOR_PLAYERS' | 'IN_PROGRESS' | 'COMPLETED'

function formatChips(n: number): string {
  return new Intl.NumberFormat('en-US').format(n)
}

export default function GamesPage() {
  const router = useRouter()
  const [games, setGames] = useState<GameSummaryDto[]>([])
  const [tab, setTab] = useState<TabFilter>('WAITING_FOR_PLAYERS')
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [passwordTarget, setPasswordTarget] = useState<GameSummaryDto | null>(null)
  const [joinError, setJoinError] = useState<string | null>(null)

  const fetchGames = useCallback(async () => {
    try {
      const { games: list } = await gameServerApi.listGames({ status: tab })
      setGames(list)
      setError(null)
    } catch {
      setError('Failed to load games. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [tab])

  useEffect(() => {
    setLoading(true)
    fetchGames()
    const interval = setInterval(fetchGames, 10_000)
    return () => clearInterval(interval)
  }, [fetchGames])

  async function handleJoin(game: GameSummaryDto, password?: string) {
    setJoinError(null)
    try {
      await gameServerApi.joinGame(game.gameId, password)
      router.push(`/games/${game.gameId}/lobby`)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to join game.'
      if (game.isPrivate && !password) {
        setPasswordTarget(game)
      } else {
        setJoinError(msg)
      }
    }
  }

  async function handlePasswordSubmit(password: string) {
    if (!passwordTarget) return
    setJoinError(null)
    try {
      await gameServerApi.joinGame(passwordTarget.gameId, password)
      router.push(`/games/${passwordTarget.gameId}/lobby`)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Incorrect password.'
      setJoinError(msg)
    }
  }

  const filtered = games.filter((g) => {
    if (!search) return true
    const q = search.toLowerCase()
    return g.name.toLowerCase().includes(q) || g.ownerName.toLowerCase().includes(q)
  })

  const tabs: Array<{ label: string; value: TabFilter }> = [
    { label: 'Open', value: 'WAITING_FOR_PLAYERS' },
    { label: 'In Progress', value: 'IN_PROGRESS' },
    { label: 'Completed', value: 'COMPLETED' },
  ]

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Game Lobby</h1>
        <Link
          href="/games/create"
          className="px-4 py-2 bg-green-700 hover:bg-green-600 text-white font-semibold rounded-lg transition-colors text-sm"
        >
          Create Game
        </Link>
      </div>

      {/* Tab filter */}
      <div className="flex gap-1 mb-4 border-b border-gray-300">
        {tabs.map((t) => (
          <button
            key={t.value}
            type="button"
            onClick={() => setTab(t.value)}
            className={[
              'px-4 py-2 text-sm font-medium transition-colors border-b-2 -mb-px',
              tab === t.value
                ? 'border-green-700 text-green-800'
                : 'border-transparent text-gray-600 hover:text-gray-900',
            ].join(' ')}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Search */}
      <div className="mb-4">
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by name or host…"
          className="w-full max-w-xs border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          aria-label="Search games"
        />
      </div>

      {error && (
        <p className="text-red-600 text-sm mb-4" role="alert">
          {error}
        </p>
      )}
      {joinError && !passwordTarget && (
        <p className="text-red-600 text-sm mb-4" role="alert">
          {joinError}
        </p>
      )}

      {loading ? (
        <p className="text-gray-500">Loading games…</p>
      ) : filtered.length === 0 ? (
        <p className="text-gray-500">No games found.</p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-gray-200 shadow-sm">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 font-semibold text-gray-700">Name</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-700">Host</th>
                <th className="text-center px-3 py-3 font-semibold text-gray-700">Players</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-700">Blinds</th>
                <th className="text-center px-3 py-3 font-semibold text-gray-700">Private</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-700">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map((game) => (
                <tr key={game.gameId} className="hover:bg-gray-50 transition-colors">
                  {/* Game name — text node only (XSS safe) */}
                  <td className="px-4 py-3 font-medium text-gray-900">{game.name}</td>
                  {/* Owner name — text node only */}
                  <td className="px-4 py-3 text-gray-600">{game.ownerName}</td>
                  <td className="px-3 py-3 text-center text-gray-600">
                    {game.playerCount}/{game.maxPlayers}
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    {formatChips(game.blinds.small)}/{formatChips(game.blinds.big)}
                  </td>
                  <td className="px-3 py-3 text-center">
                    {game.isPrivate ? (
                      <span title="Password protected" aria-label="Private game">🔒</span>
                    ) : null}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {tab === 'COMPLETED' ? (
                      <Link
                        href={`/games/${game.gameId}/results`}
                        className="text-blue-600 hover:underline text-sm"
                      >
                        Results
                      </Link>
                    ) : tab === 'IN_PROGRESS' ? (
                      <Link
                        href={`/games/${game.gameId}/play`}
                        className="text-blue-600 hover:underline text-sm"
                      >
                        Watch
                      </Link>
                    ) : (
                      <button
                        type="button"
                        onClick={() =>
                          game.isPrivate ? setPasswordTarget(game) : handleJoin(game)
                        }
                        className="px-3 py-1 bg-green-700 hover:bg-green-600 text-white text-xs font-semibold rounded-lg transition-colors"
                      >
                        Join
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Password dialog for private games */}
      {passwordTarget && (
        <PasswordDialog
          gameName={passwordTarget.name}
          onSubmit={handlePasswordSubmit}
          onCancel={() => {
            setPasswordTarget(null)
            setJoinError(null)
          }}
          error={joinError ?? undefined}
        />
      )}
    </div>
  )
}
