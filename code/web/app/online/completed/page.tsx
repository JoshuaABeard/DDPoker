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
  // TODO: Replace with actual API call
  // For now, return empty data
  return {
    games: [],
    totalPages: 0,
    totalItems: 0,
  }
}

export default async function CompletedGamesPage({
  searchParams,
}: {
  searchParams: { page?: string; begin?: string; end?: string }
}) {
  const currentPage = parseInt(searchParams.page || '1')
  const filters = {
    begin: searchParams.begin,
    end: searchParams.end,
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
      render: (game: CompletedGame) => new Date(game.completed).toLocaleDateString(),
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
