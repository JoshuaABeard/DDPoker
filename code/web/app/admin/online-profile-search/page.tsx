/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Admin Online Profile Search
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
export const dynamic = 'force-static'

import { Metadata } from 'next'
import { Suspense } from 'react'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { AdminSearchForm } from './AdminSearchForm'
import { adminApi } from '@/lib/api'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'

export const metadata: Metadata = {
  title: 'Online Profile Search - Admin - DD Poker',
  description: 'Search online player profiles',
}

interface OnlineProfile {
  id: number
  name: string
  email: string
  createdAt: string
  lastLogin?: string
  isActive: boolean
}

async function searchProfiles(
  page: number,
  filters: { name?: string; email?: string }
): Promise<{
  profiles: OnlineProfile[]
  totalPages: number
  totalItems: number
}> {
  try {
    const backendPage = toBackendPage(page)
    const { profiles, total } = await adminApi.searchProfiles({
      name: filters.name,
      email: filters.email,
      page: backendPage,
      pageSize: 50,
    })
    const mapped = profiles.map((p: any) => ({
      id: p.id,
      name: p.name || p.playerName || 'Unknown',
      email: p.email || 'N/A',
      createdAt: p.createdAt || p.createDate || new Date().toISOString(),
      lastLogin: p.lastLogin,
      isActive: p.isActive !== false,
    }))
    const result = buildPaginationResult(mapped, total, page, 50)
    return {
      profiles: result.data,
      totalPages: result.totalPages,
      totalItems: result.totalItems,
    }
  } catch (error) {
    console.error('Failed to search profiles:', error)
    return {
      profiles: [],
      totalPages: 0,
      totalItems: 0,
    }
  }
}

export default async function OnlineProfileSearchPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string; name?: string; email?: string }>
}) {
  const params = await searchParams
  const currentPage = parseInt(params.page || '1', 10) || 1
  const filters = {
    name: params.name,
    email: params.email,
  }

  const hasFilters = filters.name || filters.email
  const { profiles, totalPages, totalItems } = hasFilters
    ? await searchProfiles(currentPage, filters)
    : { profiles: [], totalPages: 0, totalItems: 0 }

  const columns = [
    {
      key: 'id',
      header: 'ID',
      render: (profile: OnlineProfile) => profile.id,
      align: 'center' as const,
    },
    {
      key: 'name',
      header: 'Player Name',
      render: (profile: OnlineProfile) => profile.name,
    },
    {
      key: 'email',
      header: 'Email',
      render: (profile: OnlineProfile) => profile.email,
    },
    {
      key: 'createdAt',
      header: 'Created',
      render: (profile: OnlineProfile) => {
        const date = new Date(profile.createdAt)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleDateString()
      },
    },
    {
      key: 'lastLogin',
      header: 'Last Login',
      render: (profile: OnlineProfile) => {
        if (!profile.lastLogin) return 'Never'
        const date = new Date(profile.lastLogin)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleDateString()
      },
    },
    {
      key: 'isActive',
      header: 'Status',
      render: (profile: OnlineProfile) => (
        <span
          className={`px-2 py-1 rounded text-xs font-semibold ${
            profile.isActive
              ? 'bg-green-100 text-green-800'
              : 'bg-gray-100 text-gray-800'
          }`}
        >
          {profile.isActive ? 'Active' : 'Inactive'}
        </span>
      ),
      align: 'center' as const,
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Online Profile Search</h1>

      <p className="mb-6 text-gray-700">
        Search for online player profiles by name or email address. At least one search
        criterion is required.
      </p>

      <AdminSearchForm />

      {hasFilters ? (
        <>
          <div className="mb-4 text-sm text-gray-600">
            {filters.name && (
              <span>
                Name: <strong>{filters.name}</strong>
                {filters.email && ' | '}
              </span>
            )}
            {filters.email && (
              <span>
                Email: <strong>{filters.email}</strong>
              </span>
            )}
          </div>

          <DataTable
            data={profiles}
            columns={columns}
            emptyMessage="No profiles found matching the search criteria"
          />

          {totalPages > 1 && (
            <Suspense fallback={<div className="text-center text-gray-500">Loading...</div>}>
              <Pagination currentPage={currentPage} totalItems={totalItems} itemsPerPage={50} />
            </Suspense>
          )}
        </>
      ) : (
        <div className="p-6 bg-gray-50 border border-gray-300 rounded-lg text-center">
          <p className="text-gray-700">Enter a player name or email address to search</p>
        </div>
      )}
    </div>
  )
}
