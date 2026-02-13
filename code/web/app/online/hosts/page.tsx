/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Game Hosts Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
export const dynamic = 'force-static'

import { Metadata } from 'next'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { FilterForm } from '@/components/filters/FilterForm'
import { PlayerLink } from '@/components/online/PlayerLink'
import { hostApi } from '@/lib/api'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'

export const metadata: Metadata = {
  title: 'Game Hosts - DD Poker',
  description: 'View game host information',
}

interface HostInfo {
  name: string
  lastHosted: string
  totalGamesHosted: number
}

async function getHosts(
  page: number,
  filters: { name?: string; begin?: string; end?: string }
): Promise<{
  hosts: HostInfo[]
  totalPages: number
  totalItems: number
}> {
  try {
    const backendPage = toBackendPage(page)
    const { hosts, total } = await hostApi.getHosts(
      filters.name,
      filters.begin,
      filters.end,
      backendPage,
      50
    )
    const mapped = hosts.map((h) => ({
      name: h.playerName,
      lastHosted: h.lastHosted || 'Unknown',
      totalGamesHosted: h.gamesHosted,
    }))
    const result = buildPaginationResult(mapped, total, page, 50)
    return {
      hosts: result.data,
      totalPages: result.totalPages,
      totalItems: result.totalItems,
    }
  } catch (error) {
    console.error('Failed to fetch hosts:', error)
    return {
      hosts: [],
      totalPages: 0,
      totalItems: 0,
    }
  }
}

export default async function HostsPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string; name?: string; begin?: string; end?: string }>
}) {
  const params = await searchParams
  const currentPage = parseInt(params.page || '1', 10) || 1
  const filters = {
    name: params.name,
    begin: params.begin,
    end: params.end,
  }

  const { hosts, totalPages, totalItems } = await getHosts(currentPage, filters)

  const columns = [
    {
      key: 'name',
      header: 'Host Name',
      render: (host: HostInfo) => <PlayerLink playerName={host.name} />,
    },
    {
      key: 'lastHosted',
      header: 'Last Hosted',
      render: (host: HostInfo) =>
        host.lastHosted === 'Unknown' || !host.lastHosted
          ? 'Unknown'
          : new Date(host.lastHosted).toLocaleDateString(),
    },
    {
      key: 'totalGamesHosted',
      header: 'Total Games Hosted',
      render: (host: HostInfo) => host.totalGamesHosted.toLocaleString(),
      align: 'right' as const,
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Game Hosts</h1>

      <p className="mb-6 text-gray-700">
        Information about players who have hosted online games. Click on a host name to view their
        tournament history.
      </p>

      <FilterForm showDateRange showNameSearch />

      <DataTable
        data={hosts}
        columns={columns}
        emptyMessage="No hosts found matching the criteria"
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
