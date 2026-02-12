'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Admin Ban List Management
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { adminApi } from '@/lib/api'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'

interface BannedKey {
  id: number
  key: string
  reason?: string
  bannedAt: string
  expiresAt?: string
}

export default function BanListPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const currentPage = parseInt(searchParams.get('page') || '1', 10) || 1

  const [bans, setBans] = useState<BannedKey[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalItems, setTotalItems] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Add ban form state
  const [newBanKey, setNewBanKey] = useState('')
  const [newBanReason, setNewBanReason] = useState('')
  const [newBanExpires, setNewBanExpires] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    loadBans()
  }, [currentPage])

  async function loadBans() {
    try {
      setIsLoading(true)
      setError(null)
      const backendPage = toBackendPage(currentPage)
      const { bans: bansData, total } = await adminApi.getBans(backendPage, 50)
      const mapped = bansData.map((b: any) => ({
        id: b.id,
        key: b.key || b.keyHash || 'Unknown',
        reason: b.reason,
        bannedAt: b.bannedAt || b.createDate || new Date().toISOString(),
        expiresAt: b.expiresAt || b.expireDate,
      }))
      const result = buildPaginationResult(mapped, total, currentPage, 50)
      setBans(result.data)
      setTotalPages(result.totalPages)
      setTotalItems(result.totalItems)
    } catch (err) {
      console.error('Failed to load bans:', err)
      setError('Failed to load ban list. Please try again.')
      setBans([])
      setTotalPages(0)
      setTotalItems(0)
    } finally {
      setIsLoading(false)
    }
  }

  async function handleAddBan(e: React.FormEvent) {
    e.preventDefault()
    if (!newBanKey.trim()) {
      alert('Key is required')
      return
    }

    try {
      setIsSubmitting(true)
      await adminApi.addBan({
        key: newBanKey.trim(),
        reason: newBanReason.trim() || undefined,
        expiresAt: newBanExpires || undefined,
      })
      setNewBanKey('')
      setNewBanReason('')
      setNewBanExpires('')
      await loadBans()
    } catch (err) {
      console.error('Failed to add ban:', err)
      alert('Failed to add ban. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleRemoveBan(banId: number) {
    if (!confirm('Are you sure you want to remove this ban?')) {
      return
    }

    try {
      await adminApi.removeBan(banId)
      await loadBans()
    } catch (err) {
      console.error('Failed to remove ban:', err)
      alert('Failed to remove ban. Please try again.')
    }
  }

  const columns = [
    {
      key: 'id',
      header: 'ID',
      render: (ban: BannedKey) => ban.id,
      align: 'center' as const,
    },
    {
      key: 'key',
      header: 'Key Hash',
      render: (ban: BannedKey) => (
        <code className="text-xs bg-gray-100 px-2 py-1 rounded">
          {ban.key.length > 24 ? `${ban.key.substring(0, 24)}...` : ban.key}
        </code>
      ),
    },
    {
      key: 'reason',
      header: 'Reason',
      render: (ban: BannedKey) => ban.reason || '-',
    },
    {
      key: 'bannedAt',
      header: 'Banned On',
      render: (ban: BannedKey) => {
        const date = new Date(ban.bannedAt)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleDateString()
      },
    },
    {
      key: 'expiresAt',
      header: 'Expires',
      render: (ban: BannedKey) => {
        if (!ban.expiresAt) return 'Never'
        const date = new Date(ban.expiresAt)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleDateString()
      },
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (ban: BannedKey) => (
        <button
          onClick={() => handleRemoveBan(ban.id)}
          className="px-3 py-1 bg-red-600 text-white text-sm rounded hover:bg-red-700 transition-colors"
        >
          Remove
        </button>
      ),
      align: 'center' as const,
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Ban List Management</h1>

      <p className="mb-6 text-gray-700">
        View and manage banned keys. Add new bans or remove existing ones.
      </p>

      {/* Add Ban Form */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
        <h2 className="text-xl font-bold mb-4">Add New Ban</h2>
        <form onSubmit={handleAddBan}>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label htmlFor="banKey" className="block text-sm font-medium mb-1">
                Key Hash <span className="text-red-600">*</span>
              </label>
              <input
                type="text"
                id="banKey"
                value={newBanKey}
                onChange={(e) => setNewBanKey(e.target.value)}
                placeholder="Enter key hash to ban"
                required
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="banReason" className="block text-sm font-medium mb-1">
                Reason (optional)
              </label>
              <input
                type="text"
                id="banReason"
                value={newBanReason}
                onChange={(e) => setNewBanReason(e.target.value)}
                placeholder="e.g., Cheating, spam, etc."
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="banExpires" className="block text-sm font-medium mb-1">
                Expiration Date (optional)
              </label>
              <input
                type="date"
                id="banExpires"
                value={newBanExpires}
                onChange={(e) => setNewBanExpires(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <div className="text-xs text-gray-500 mt-1">Leave empty for permanent ban</div>
            </div>
          </div>

          <div className="mt-4">
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors disabled:bg-gray-400"
            >
              {isSubmitting ? 'Adding...' : 'Add Ban'}
            </button>
          </div>
        </form>
      </div>

      {/* Ban List */}
      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="text-center py-8 text-gray-600">Loading ban list...</div>
      ) : (
        <>
          <DataTable data={bans} columns={columns} emptyMessage="No bans found" />

          {totalPages > 1 && (
            <Pagination currentPage={currentPage} totalItems={totalItems} itemsPerPage={50} />
          )}
        </>
      )}
    </div>
  )
}
