/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Leaderboard Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { PlayerLink } from '@/components/online/PlayerLink'
import { LeaderboardFilter } from './LeaderboardFilter'

export const metadata: Metadata = {
  title: 'Leaderboard - DD Poker',
  description: 'View player rankings and statistics',
}

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
  // TODO: Replace with actual API call
  // For now, return empty data
  return {
    entries: [],
    totalPages: 0,
    totalItems: 0,
  }
}

export default async function LeaderboardPage({
  searchParams,
}: {
  searchParams: {
    type?: string
    page?: string
    name?: string
    begin?: string
    end?: string
    games?: string
  }
}) {
  const type = searchParams.type || 'ddr1'
  const currentPage = parseInt(searchParams.page || '1')
  const filters = {
    name: searchParams.name,
    begin: searchParams.begin,
    end: searchParams.end,
    games: searchParams.games,
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
        <Pagination
          currentPage={currentPage}
          totalItems={totalItems}
          itemsPerPage={20}
        />
      )}
    </div>
  )
}
