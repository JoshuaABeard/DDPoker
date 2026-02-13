/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Leaderboard Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import { Suspense } from 'react'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { PlayerLink } from '@/components/online/PlayerLink'
import { LeaderboardFilter } from './LeaderboardFilter'
import { leaderboardApi } from '@/lib/api'
import { mapLeaderboardEntry } from '@/lib/mappers'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'

export const metadata: Metadata = {
  title: 'Leaderboard - DD Poker',
  description: 'View player rankings and statistics',
}

export const dynamic = 'force-dynamic'

interface LeaderboardEntry {
  rank: number
  playerName: string
  score: number
  gamesPlayed: number
  wins: number
  winRate: number
}

async function getLeaderboard(
  type: string,
  page: number,
  filters: { name?: string; begin?: string; end?: string; games?: string }
): Promise<{
  entries: LeaderboardEntry[]
  totalPages: number
  totalItems: number
}> {
  try {
    const mode = (type === 'roi' ? 'roi' : 'ddr1') as 'ddr1' | 'roi'
    const backendPage = toBackendPage(page)
    const apiFilters = {
      name: filters.name,
      from: filters.begin,
      to: filters.end,
      games: filters.games ? parseInt(filters.games) : undefined,
    }
    const { entries, total } = await leaderboardApi.getLeaderboard(mode, backendPage, 50, apiFilters)
    const mapped = entries.map((e) => mapLeaderboardEntry(e, mode))
    const result = buildPaginationResult(mapped, total, page, 50)
    return {
      entries: result.data,
      totalPages: result.totalPages,
      totalItems: result.totalItems,
    }
  } catch (error) {
    console.error('Failed to fetch leaderboard:', error)
    return {
      entries: [],
      totalPages: 0,
      totalItems: 0,
    }
  }
}

export default async function LeaderboardPage({
  searchParams,
}: {
  searchParams: Promise<{
    type?: string
    page?: string
    name?: string
    begin?: string
    end?: string
    games?: string
  }>
}) {
  const params = await searchParams
  const type = params.type || 'ddr1'
  const currentPage = parseInt(params.page || '1', 10) || 1
  const filters = {
    name: params.name,
    begin: params.begin,
    end: params.end,
    games: params.games,
  }

  const { entries, totalPages, totalItems } = await getLeaderboard(type, currentPage, filters)

  const columns = [
    {
      key: 'rank',
      header: 'Rank',
      render: (entry: LeaderboardEntry) => entry.rank,
      align: 'center' as const,
    },
    {
      key: 'playerName',
      header: 'Player',
      render: (entry: LeaderboardEntry) => <PlayerLink playerName={entry.playerName} />,
    },
    {
      key: 'score',
      header: type === 'ddr1' ? 'DDR1 Score' : 'ROI %',
      render: (entry: LeaderboardEntry) =>
        type === 'roi' ? `${entry.score.toFixed(2)}%` : entry.score.toFixed(2),
      align: 'right' as const,
    },
    {
      key: 'gamesPlayed',
      header: 'Games',
      render: (entry: LeaderboardEntry) => entry.gamesPlayed.toLocaleString(),
      align: 'right' as const,
    },
    {
      key: 'wins',
      header: 'Wins',
      render: (entry: LeaderboardEntry) => entry.wins.toLocaleString(),
      align: 'right' as const,
    },
    {
      key: 'winRate',
      header: 'Win Rate',
      render: (entry: LeaderboardEntry) => `${entry.winRate.toFixed(1)}%`,
      align: 'right' as const,
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Leaderboard</h1>

      <div className="mb-6 p-4 bg-blue-50 rounded-lg">
        <h2 className="font-bold mb-2">About Rankings</h2>
        <p className="text-sm text-gray-700">
          <strong>DDR1 Mode:</strong> Doug Donohoe Rating v1 - A proprietary rating system that
          considers tournament placement, field size, and buy-in level.
        </p>
        <p className="text-sm text-gray-700 mt-2">
          <strong>ROI Mode:</strong> Return on Investment - Percentage return based on buy-ins and
          winnings.
        </p>
      </div>

      <LeaderboardFilter />

      <DataTable
        data={entries}
        columns={columns}
        emptyMessage="No leaderboard entries found matching the criteria"
      />

      {totalPages > 1 && (
        <Suspense fallback={<div className="text-center text-gray-500">Loading...</div>}>
          <Pagination
            currentPage={currentPage}
            totalItems={totalItems}
            itemsPerPage={50}
          />
        </Suspense>
      )}
    </div>
  )
}
