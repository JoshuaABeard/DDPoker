/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Admin Registration Search
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { RegistrationSearchForm } from './RegistrationSearchForm'
import { adminApi } from '@/lib/api'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'

export const metadata: Metadata = {
  title: 'Registration Search - Admin - DD Poker',
  description: 'Search player registrations',
}

interface Registration {
  id: number
  playerName: string
  email: string
  registrationDate: string
  keyHash: string
}

async function searchRegistrations(
  page: number,
  filters: { name?: string; email?: string; from?: string; to?: string }
): Promise<{
  registrations: Registration[]
  totalPages: number
  totalItems: number
}> {
  try {
    const backendPage = toBackendPage(page)
    const { registrations, total } = await adminApi.searchRegistrations({
      name: filters.name,
      email: filters.email,
      from: filters.from,
      to: filters.to,
      page: backendPage,
      pageSize: 50,
    })
    const mapped = registrations.map((r: any) => ({
      id: r.id,
      playerName: r.playerName || r.name || 'Unknown',
      email: r.email || 'N/A',
      registrationDate: r.registrationDate || r.createDate || new Date().toISOString(),
      keyHash: r.keyHash || r.key || 'Unknown',
    }))
    const result = buildPaginationResult(mapped, total, page, 50)
    return {
      registrations: result.data,
      totalPages: result.totalPages,
      totalItems: result.totalItems,
    }
  } catch (error) {
    console.error('Failed to search registrations:', error)
    return {
      registrations: [],
      totalPages: 0,
      totalItems: 0,
    }
  }
}

export default async function RegistrationSearchPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string; name?: string; email?: string; from?: string; to?: string }>
}) {
  const params = await searchParams
  const currentPage = parseInt(params.page || '1', 10) || 1
  const filters = {
    name: params.name,
    email: params.email,
    from: params.from,
    to: params.to,
  }

  const hasFilters = filters.name || filters.email || filters.from || filters.to
  const { registrations, totalPages, totalItems } = hasFilters
    ? await searchRegistrations(currentPage, filters)
    : { registrations: [], totalPages: 0, totalItems: 0 }

  const columns = [
    {
      key: 'id',
      header: 'ID',
      render: (reg: Registration) => reg.id,
      align: 'center' as const,
    },
    {
      key: 'playerName',
      header: 'Player Name',
      render: (reg: Registration) => reg.playerName,
    },
    {
      key: 'email',
      header: 'Email',
      render: (reg: Registration) => reg.email,
    },
    {
      key: 'registrationDate',
      header: 'Registration Date',
      render: (reg: Registration) => {
        const date = new Date(reg.registrationDate)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleDateString()
      },
    },
    {
      key: 'keyHash',
      header: 'Key Hash',
      render: (reg: Registration) => (
        <code className="text-xs bg-gray-100 px-2 py-1 rounded">
          {reg.keyHash.substring(0, 16)}...
        </code>
      ),
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Registration Search</h1>

      <p className="mb-6 text-gray-700">
        Search for player registrations by name, email, or registration date range. At least one
        search criterion is required.
      </p>

      <RegistrationSearchForm />

      {hasFilters ? (
        <>
          <div className="mb-4 text-sm text-gray-600">
            {filters.name && (
              <span>
                Name: <strong>{filters.name}</strong>
                {' | '}
              </span>
            )}
            {filters.email && (
              <span>
                Email: <strong>{filters.email}</strong>
                {' | '}
              </span>
            )}
            {filters.from && (
              <span>
                From: <strong>{filters.from}</strong>
                {' | '}
              </span>
            )}
            {filters.to && (
              <span>
                To: <strong>{filters.to}</strong>
              </span>
            )}
          </div>

          <DataTable
            data={registrations}
            columns={columns}
            emptyMessage="No registrations found matching the search criteria"
          />

          {totalPages > 1 && (
            <Pagination currentPage={currentPage} totalItems={totalItems} itemsPerPage={50} />
          )}
        </>
      ) : (
        <div className="p-6 bg-gray-50 border border-gray-300 rounded-lg text-center">
          <p className="text-gray-700">
            Enter search criteria (name, email, or date range) to find registrations
          </p>
        </div>
      )}
    </div>
  )
}
