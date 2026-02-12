/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Player Search Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { FilterForm } from '@/components/filters/FilterForm'
import { HighlightText } from '@/components/online/HighlightText'
import { PlayerLink } from '@/components/online/PlayerLink'

export const metadata: Metadata = {
  title: 'Player Search - DD Poker',
  description: 'Search for players and view their profiles',
}

interface PlayerSearchResult {
  playerName: string
  gamesPlayed: number
  lastPlayed: string
  rank?: number
}

async function searchPlayers(
  name: string,
  page: number
): Promise<{
  results: PlayerSearchResult[]
  totalPages: number
  totalItems: number
}> {
  // TODO: Replace with actual API call
  // For now, return empty data
  return {
    results: [],
    totalPages: 0,
    totalItems: 0,
  }
}

export default async function SearchPage({
  searchParams,
}: {
  searchParams: { name?: string; page?: string }
}) {
  const searchTerm = searchParams.name || ''
  const currentPage = parseInt(searchParams.page || '1')

  const { results, totalPages, totalItems } = searchTerm
    ? await searchPlayers(searchTerm, currentPage)
    : { results: [], totalPages: 0, totalItems: 0 }

  const columns = [
    {
      key: 'playerName',
      header: 'Player Name',
      render: (player: PlayerSearchResult) => (
        <span>
          <HighlightText text={player.playerName} searchTerm={searchTerm} />
          {' ('}
          <PlayerLink playerName={player.playerName} />
          {')'}
        </span>
      ),
    },
    {
      key: 'gamesPlayed',
      header: 'Games Played',
      render: (player: PlayerSearchResult) => player.gamesPlayed.toLocaleString(),
      align: 'right' as const,
    },
    {
      key: 'lastPlayed',
      header: 'Last Played',
      render: (player: PlayerSearchResult) => new Date(player.lastPlayed).toLocaleDateString(),
    },
    {
      key: 'rank',
      header: 'Rank',
      render: (player: PlayerSearchResult) => player.rank || '-',
      align: 'center' as const,
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Player Search</h1>

      <p className="mb-6 text-gray-700">
        Search for players by name. Use the = prefix for exact matches (e.g., =JohnDoe).
      </p>

      <FilterForm showNameSearch />

      {searchTerm ? (
        <>
          <div className="mb-4 text-sm text-gray-600">
            Search results for: <strong>{searchTerm}</strong>
          </div>

          <DataTable
            data={results}
            columns={columns}
            emptyMessage={`No players found matching "${searchTerm}"`}
          />

          {totalPages > 1 && (
            <Pagination
              currentPage={currentPage}
              totalItems={totalItems}
              itemsPerPage={20}
            />
          )}
        </>
      ) : (
        <div className="p-6 bg-gray-50 border border-gray-300 rounded-lg text-center">
          <p className="text-gray-700">Enter a player name to search</p>
        </div>
      )}
    </div>
  )
}
