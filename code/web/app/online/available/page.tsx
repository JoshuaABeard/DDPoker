/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Available Games Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
export const dynamic = 'force-static'

import { Metadata } from 'next'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { PlayerLink } from '@/components/online/PlayerLink'
import { gamesApi } from '@/lib/api'
import { mapAvailableGame } from '@/lib/mappers'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'

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
  try {
    const backendPage = toBackendPage(page)
    const { games, total } = await gamesApi.getAvailable(backendPage, 20)
    const mapped = games.map(mapAvailableGame)
    const result = buildPaginationResult(mapped, total, page, 20)
    return {
      games: result.data,
      totalPages: result.totalPages,
      totalItems: result.totalItems,
    }
  } catch (error) {
    console.error('Failed to fetch available games:', error)
    return {
      games: [],
      totalPages: 0,
      totalItems: 0,
    }
  }
}

export default async function AvailableGamesPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>
}) {
  const params = await searchParams
  const currentPage = parseInt(params.page || '1', 10) || 1
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
      render: (game: AvailableGame) => {
        const date = new Date(game.created)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleDateString()
      },
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
