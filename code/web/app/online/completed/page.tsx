/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Completed Games Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import Link from 'next/link'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { FilterForm } from '@/components/filters/FilterForm'
import { PlayerLink } from '@/components/online/PlayerLink'
import { gamesApi } from '@/lib/api'
import { mapCompletedGame } from '@/lib/mappers'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'

export const metadata: Metadata = {
  title: 'Completed Games - DD Poker',
  description: 'View recently completed games',
}

interface CompletedGame {
  id: number
  name: string
  host: string
  mode: string
  winner: string
  players: number
  buyIn: number
  completed: string
}

async function getCompletedGames(
  page: number,
  filters: { begin?: string; end?: string }
): Promise<{
  games: CompletedGame[]
  totalPages: number
  totalItems: number
}> {
  try {
    const backendPage = toBackendPage(page)
    const { games, total } = await gamesApi.getCompleted(
      backendPage,
      20,
      filters.begin,
      filters.end
    )
    const mapped = games.map(mapCompletedGame)
    const result = buildPaginationResult(mapped, total, page, 20)
    return {
      games: result.data,
      totalPages: result.totalPages,
      totalItems: result.totalItems,
    }
  } catch (error) {
    console.error('Failed to fetch completed games:', error)
    return {
      games: [],
      totalPages: 0,
      totalItems: 0,
    }
  }
}

export default async function CompletedGamesPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string; begin?: string; end?: string }>
}) {
  const params = await searchParams
  const currentPage = parseInt(params.page || '1', 10) || 1
  const filters = {
    begin: params.begin,
    end: params.end,
  }

  const { games, totalPages, totalItems } = await getCompletedGames(currentPage, filters)

  const columns = [
    {
      key: 'name',
      header: 'Game Name',
      render: (game: CompletedGame) => (
        <Link href={`/online/game?id=${game.id}`} className="text-blue-600 hover:underline">
          {game.name}
        </Link>
      ),
    },
    {
      key: 'host',
      header: 'Host',
      render: (game: CompletedGame) => <PlayerLink playerName={game.host} />,
    },
    {
      key: 'mode',
      header: 'Mode',
      render: (game: CompletedGame) => game.mode,
    },
    {
      key: 'winner',
      header: 'Winner',
      render: (game: CompletedGame) => <PlayerLink playerName={game.winner} />,
    },
    {
      key: 'players',
      header: 'Players',
      render: (game: CompletedGame) => game.players,
      align: 'center' as const,
    },
    {
      key: 'buyIn',
      header: 'Buy-In',
      render: (game: CompletedGame) => `$${game.buyIn.toLocaleString()}`,
      align: 'right' as const,
    },
    {
      key: 'completed',
      header: 'Completed',
      render: (game: CompletedGame) => {
        const date = new Date(game.completed)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleDateString()
      },
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Completed Games</h1>

      <p className="mb-6 text-gray-700">
        Recently finished tournaments. Click on a game name to view full details and final results.
      </p>

      <FilterForm showDateRange />

      <DataTable
        data={games}
        columns={columns}
        emptyMessage="No completed games found matching the criteria"
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
