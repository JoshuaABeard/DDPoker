/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Current Games Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import { Suspense } from 'react'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { PlayerLink } from '@/components/online/PlayerLink'
import { PlayerList } from '@/components/online/PlayerList'
import { gamesApi } from '@/lib/api'
import { mapCurrentGame } from '@/lib/mappers'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'

export const metadata: Metadata = {
  title: 'Current Games - DD Poker',
  description: 'View games currently in progress',
}

export const dynamic = 'force-dynamic'

interface CurrentGame {
  id: number
  name: string
  host: string
  mode: string
  players: string[]
  blindLevel: number
  started: string
}

async function getCurrentGames(page: number): Promise<{
  games: CurrentGame[]
  totalPages: number
  totalItems: number
}> {
  try {
    const backendPage = toBackendPage(page)
    const { games, total } = await gamesApi.getRunning(backendPage, 20)
    const mapped = games.map(mapCurrentGame)
    const result = buildPaginationResult(mapped, total, page, 20)
    return {
      games: result.data,
      totalPages: result.totalPages,
      totalItems: result.totalItems,
    }
  } catch (error) {
    console.error('Failed to fetch current games:', error)
    return {
      games: [],
      totalPages: 0,
      totalItems: 0,
    }
  }
}

export default async function CurrentGamesPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>
}) {
  const params = await searchParams
  const currentPage = parseInt(params.page || '1', 10) || 1
  const { games, totalPages, totalItems } = await getCurrentGames(currentPage)

  const columns = [
    {
      key: 'name',
      header: 'Game Name',
      render: (game: CurrentGame) => game.name,
    },
    {
      key: 'host',
      header: 'Host',
      render: (game: CurrentGame) => <PlayerLink playerName={game.host} />,
    },
    {
      key: 'mode',
      header: 'Mode',
      render: (game: CurrentGame) => game.mode,
    },
    {
      key: 'players',
      header: 'Players',
      render: (game: CurrentGame) => <PlayerList players={game.players} />,
    },
    {
      key: 'blindLevel',
      header: 'Blind Level',
      render: (game: CurrentGame) => game.blindLevel,
      align: 'center' as const,
    },
    {
      key: 'started',
      header: 'Started',
      render: (game: CurrentGame) => {
        const date = new Date(game.started)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleString()
      },
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Current Games</h1>

      <p className="mb-6 text-gray-700">
        Games currently in progress. Click on player names to view their tournament history.
      </p>

      <DataTable
        data={games}
        columns={columns}
        emptyMessage="No games currently in progress"
      />

      {totalPages > 1 && (
        <Suspense fallback={<div className="text-center text-gray-500">Loading...</div>}>
          <Pagination
            currentPage={currentPage}
            totalItems={totalItems}
            itemsPerPage={20}
          />
        </Suspense>
      )}
    </div>
  )
}
