/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Available Games Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { PlayerLink } from '@/components/online/PlayerLink'

export const metadata: Metadata = {
  title: 'Available Games - DD Poker',
  description: 'View available games waiting for players',
}

interface AvailableGame {
  id: number
  name: string
  host: string
  mode: string
  buyIn: number
  players: number
  maxPlayers: number
  created: string
}

async function getAvailableGames(page: number): Promise<{
  games: AvailableGame[]
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

export default async function AvailableGamesPage({
  searchParams,
}: {
  searchParams: { page?: string }
}) {
  const currentPage = parseInt(searchParams.page || '1')
  const { games, totalPages, totalItems } = await getAvailableGames(currentPage)

  const columns = [
    {
      key: 'name',
      header: 'Game Name',
      render: (game: AvailableGame) => game.name,
    },
    {
      key: 'host',
      header: 'Host',
      render: (game: AvailableGame) => <PlayerLink playerName={game.host} />,
    },
    {
      key: 'mode',
      header: 'Mode',
      render: (game: AvailableGame) => game.mode,
    },
    {
      key: 'buyIn',
      header: 'Buy-In',
      render: (game: AvailableGame) => `$${game.buyIn.toLocaleString()}`,
      align: 'right' as const,
    },
    {
      key: 'players',
      header: 'Players',
      render: (game: AvailableGame) => `${game.players}/${game.maxPlayers}`,
      align: 'center' as const,
    },
    {
      key: 'created',
      header: 'Created',
      render: (game: AvailableGame) => new Date(game.created).toLocaleDateString(),
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Available Games</h1>

      <p className="mb-6 text-gray-700">
        Games waiting for players to join. Click on a host name to view their tournament history.
      </p>

      <DataTable
        data={games}
        columns={columns}
        emptyMessage="No available games at this time"
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
