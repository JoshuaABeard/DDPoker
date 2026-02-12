/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Game Hosts Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { FilterForm } from '@/components/filters/FilterForm'
import { PlayerLink } from '@/components/online/PlayerLink'

export const metadata: Metadata = {
  title: 'Game Hosts - DD Poker',
  description: 'View game host information',
}

interface HostInfo {
  name: string
  lastHosted: string
  totalGamesHosted: number
  ipAddress: string
}

async function getHosts(
  page: number,
  filters: { name?: string; begin?: string; end?: string }
): Promise<{
  hosts: HostInfo[]
  totalPages: number
  totalItems: number
}> {
  // TODO: Replace with actual API call
  // For now, return empty data
  return {
    hosts: [],
    totalPages: 0,
    totalItems: 0,
  }
}

export default async function HostsPage({
  searchParams,
}: {
  searchParams: { page?: string; name?: string; begin?: string; end?: string }
}) {
  const currentPage = parseInt(searchParams.page || '1')
  const filters = {
    name: searchParams.name,
    begin: searchParams.begin,
    end: searchParams.end,
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
      render: (host: HostInfo) => new Date(host.lastHosted).toLocaleDateString(),
    },
    {
      key: 'totalGamesHosted',
      header: 'Total Games Hosted',
      render: (host: HostInfo) => host.totalGamesHosted.toLocaleString(),
      align: 'right' as const,
    },
    {
      key: 'ipAddress',
      header: 'IP Address',
      render: (host: HostInfo) => host.ipAddress,
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
          itemsPerPage={20}
        />
      )}
    </div>
  )
}
