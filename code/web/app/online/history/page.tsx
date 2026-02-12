/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Tournament History Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import Link from 'next/link'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { FilterForm } from '@/components/filters/FilterForm'
import { tournamentApi } from '@/lib/api'
import { mapTournamentEntry, calculateTournamentStats } from '@/lib/mappers'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'

export const metadata: Metadata = {
  title: 'Tournament History - DD Poker',
  description: 'View tournament history and statistics',
}

interface TournamentEntry {
  id: number
  gameName: string
  placement: number
  totalPlayers: number
  buyIn: number
  prize: number
  date: string
  mode: string
}

interface PlayerStats {
  totalGames: number
  totalWins: number
  totalPrize: number
  avgPlacement: number
  winRate: number
}

async function getTournamentHistory(
  playerName: string,
  page: number,
  filters: { begin?: string; end?: string }
): Promise<{
  entries: TournamentEntry[]
  stats: PlayerStats
  totalPages: number
  totalItems: number
}> {
  try {
    const backendPage = toBackendPage(page)
    const { history, total } = await tournamentApi.getHistory(
      playerName,
      backendPage,
      50,
      filters.begin,
      filters.end
    )
    const mapped = history.map(mapTournamentEntry)
    // LIMITATION: Stats calculated from current page only, not full history
    // TODO: Backend should return aggregate stats, or fetch all history (page=0, pageSize=total)
    const stats = calculateTournamentStats(mapped)
    const result = buildPaginationResult(mapped, total, page, 50)
    return {
      entries: result.data,
      stats,
      totalPages: result.totalPages,
      totalItems: result.totalItems,
    }
  } catch (error) {
    console.error('Failed to fetch tournament history:', error)
    return {
      entries: [],
      stats: {
        totalGames: 0,
        totalWins: 0,
        totalPrize: 0,
        avgPlacement: 0,
        winRate: 0,
      },
      totalPages: 0,
      totalItems: 0,
    }
  }
}

export default async function TournamentHistoryPage({
  searchParams,
}: {
  searchParams: Promise<{ name?: string; page?: string; begin?: string; end?: string }>
}) {
  const params = await searchParams
  const playerName = params.name || ''
  const currentPage = parseInt(params.page || '1')
  const filters = {
    begin: params.begin,
    end: params.end,
  }

  if (!playerName) {
    return (
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold mb-6">Tournament History</h1>
        <div className="p-6 bg-yellow-50 border border-yellow-400 rounded-lg">
          <p className="text-gray-700">
            Please specify a player name in the URL query parameter: ?name=PlayerName
          </p>
          <p className="text-gray-700 mt-2">
            Or use the{' '}
            <Link href="/online/search" className="text-blue-600 hover:underline">
              Player Search
            </Link>{' '}
            to find a player.
          </p>
        </div>
      </div>
    )
  }

  const { entries, stats, totalPages, totalItems } = await getTournamentHistory(
    playerName,
    currentPage,
    filters
  )

  const columns = [
    {
      key: 'gameName',
      header: 'Tournament',
      render: (entry: TournamentEntry) => (
        <Link href={`/online/game?id=${entry.id}`} className="text-blue-600 hover:underline">
          {entry.gameName}
        </Link>
      ),
    },
    {
      key: 'placement',
      header: 'Place',
      render: (entry: TournamentEntry) => `${entry.placement} / ${entry.totalPlayers}`,
      align: 'center' as const,
    },
    {
      key: 'buyIn',
      header: 'Buy-In',
      render: (entry: TournamentEntry) => `$${entry.buyIn.toLocaleString()}`,
      align: 'right' as const,
    },
    {
      key: 'prize',
      header: 'Prize',
      render: (entry: TournamentEntry) =>
        entry.prize > 0 ? `$${entry.prize.toLocaleString()}` : '-',
      align: 'right' as const,
    },
    {
      key: 'mode',
      header: 'Mode',
      render: (entry: TournamentEntry) => entry.mode,
    },
    {
      key: 'date',
      header: 'Date',
      render: (entry: TournamentEntry) => new Date(entry.date).toLocaleDateString(),
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">
        Tournament History: <span className="text-green-600">{playerName}</span>
      </h1>

      {stats.totalGames > 0 && (
        <div className="mb-6 grid grid-cols-2 md:grid-cols-5 gap-4">
          <div className="bg-white p-4 rounded-lg shadow">
            <div className="text-sm text-gray-600">Total Games</div>
            <div className="text-2xl font-bold">{stats.totalGames}</div>
          </div>
          <div className="bg-white p-4 rounded-lg shadow">
            <div className="text-sm text-gray-600">Wins</div>
            <div className="text-2xl font-bold text-green-600">{stats.totalWins}</div>
          </div>
          <div className="bg-white p-4 rounded-lg shadow">
            <div className="text-sm text-gray-600">Win Rate</div>
            <div className="text-2xl font-bold">{stats.winRate.toFixed(1)}%</div>
          </div>
          <div className="bg-white p-4 rounded-lg shadow">
            <div className="text-sm text-gray-600">Avg Place</div>
            <div className="text-2xl font-bold">{stats.avgPlacement.toFixed(1)}</div>
          </div>
          <div className="bg-white p-4 rounded-lg shadow">
            <div className="text-sm text-gray-600">Total Prize</div>
            <div className="text-2xl font-bold text-green-600">
              ${stats.totalPrize.toLocaleString()}
            </div>
          </div>
        </div>
      )}

      <FilterForm showDateRange />

      <DataTable
        data={entries}
        columns={columns}
        emptyMessage={`No tournament history found for ${playerName}`}
      />

      {totalPages > 1 && (
        <Pagination
          currentPage={currentPage}
          totalItems={totalItems}
          itemsPerPage={50}
        />
      )}
    </div>
  )
}
