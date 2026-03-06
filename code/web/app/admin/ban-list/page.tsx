'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Admin Ban List Management
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useEffect, useState, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { DataTable } from '@/components/data/DataTable'
import { Pagination } from '@/components/data/Pagination'
import { Dialog } from '@/components/ui/Dialog'
import { adminApi } from '@/lib/api'
import { toBackendPage, buildPaginationResult } from '@/lib/pagination'
import type { BanDto } from '@/lib/types'

export default function BanListPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const currentPage = parseInt(searchParams.get('page') || '1', 10) || 1

  const [bans, setBans] = useState<BanDto[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalItems, setTotalItems] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Add ban form state
  const [banType, setBanType] = useState<'PROFILE' | 'EMAIL'>('EMAIL')
  const [banTarget, setBanTarget] = useState('')
  const [banReason, setBanReason] = useState('')
  const [banUntil, setBanUntil] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Dialog state
  const [dialogState, setDialogState] = useState<{
    isOpen: boolean
    type: 'alert' | 'confirm'
    title: string
    message: string
    onConfirm?: () => void
  }>({ isOpen: false, type: 'alert', title: '', message: '' })

  useEffect(() => {
    loadBans()
  }, [currentPage])

  async function loadBans() {
    try {
      setIsLoading(true)
      setError(null)
      const { bans: bansData, total } = await adminApi.getBans()
      // Client-side pagination since backend returns all bans
      const startIndex = (currentPage - 1) * 50
      const endIndex = startIndex + 50
      const paginatedBans = bansData.slice(startIndex, endIndex)
      setBans(paginatedBans)
      setTotalPages(Math.ceil(total / 50))
      setTotalItems(total)
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
    if (!banTarget.trim()) {
      setDialogState({
        isOpen: true,
        type: 'alert',
        title: 'Validation Error',
        message: banType === 'PROFILE' ? 'Profile ID is required' : 'Email is required',
      })
      return
    }

    try {
      setIsSubmitting(true)
      await adminApi.addBan({
        banType,
        ...(banType === 'PROFILE' ? { profileId: parseInt(banTarget) } : { email: banTarget }),
        reason: banReason || undefined,
        until: banUntil || undefined,
      })
      setBanTarget('')
      setBanReason('')
      setBanUntil('')
      await loadBans()
    } catch (err) {
      console.error('Failed to add ban:', err)
      setDialogState({
        isOpen: true,
        type: 'alert',
        title: 'Error',
        message: 'Failed to add ban. Please try again.',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleRemoveBan(ban: BanDto) {
    setDialogState({
      isOpen: true,
      type: 'confirm',
      title: 'Remove Ban',
      message: 'Are you sure you want to remove this ban?',
      onConfirm: () => confirmRemoveBan(ban.id),
    })
  }

  async function confirmRemoveBan(id: number) {
    try {
      await adminApi.removeBan(id)
      await loadBans()
    } catch (err) {
      console.error('Failed to remove ban:', err)
      setDialogState({
        isOpen: true,
        type: 'alert',
        title: 'Error',
        message: 'Failed to remove ban. Please try again.',
      })
    }
  }

  const columns = [
    {
      key: 'id',
      header: 'ID',
      render: (ban: BanDto) => ban.id,
      align: 'center' as const,
    },
    {
      key: 'banType',
      header: 'Type',
      render: (ban: BanDto) => ban.banType,
    },
    {
      key: 'target',
      header: 'Target',
      render: (ban: BanDto) =>
        ban.banType === 'PROFILE' ? (
          <span>Profile #{ban.profileId}</span>
        ) : (
          <code className="text-xs bg-gray-100 px-2 py-1 rounded">{ban.email}</code>
        ),
    },
    {
      key: 'reason',
      header: 'Reason',
      render: (ban: BanDto) => ban.reason || '-',
    },
    {
      key: 'createdAt',
      header: 'Banned On',
      render: (ban: BanDto) => {
        const date = new Date(ban.createdAt)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleDateString()
      },
    },
    {
      key: 'until',
      header: 'Expires',
      render: (ban: BanDto) => {
        if (!ban.until) return 'Never'
        const date = new Date(ban.until)
        return isNaN(date.getTime()) ? 'Unknown' : date.toLocaleDateString()
      },
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (ban: BanDto) => (
        <button
          onClick={() => handleRemoveBan(ban)}
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
        View and manage bans. Add new bans or remove existing ones.
      </p>

      {/* Add Ban Form */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
        <h2 className="text-xl font-bold mb-4">Add New Ban</h2>
        <form onSubmit={handleAddBan}>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label htmlFor="banType" className="block text-sm font-medium mb-1">
                Ban Type <span className="text-red-600">*</span>
              </label>
              <select
                id="banType"
                value={banType}
                onChange={(e) => {
                  setBanType(e.target.value as 'PROFILE' | 'EMAIL')
                  setBanTarget('')
                }}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="EMAIL">Email</option>
                <option value="PROFILE">Profile</option>
              </select>
            </div>

            <div>
              <label htmlFor="banTarget" className="block text-sm font-medium mb-1">
                {banType === 'PROFILE' ? 'Profile ID' : 'Email'}{' '}
                <span className="text-red-600">*</span>
              </label>
              <input
                type={banType === 'PROFILE' ? 'number' : 'email'}
                id="banTarget"
                value={banTarget}
                onChange={(e) => setBanTarget(e.target.value)}
                placeholder={banType === 'PROFILE' ? 'Enter profile ID' : 'Enter email address'}
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
                value={banReason}
                onChange={(e) => setBanReason(e.target.value)}
                placeholder="e.g., Cheating, spam, etc."
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="banUntil" className="block text-sm font-medium mb-1">
                Expires On (optional)
              </label>
              <input
                type="date"
                id="banUntil"
                value={banUntil}
                onChange={(e) => setBanUntil(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <div className="text-xs text-gray-500 mt-1">Leave empty for permanent ban (2099-12-31)</div>
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
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700" role="alert">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="text-center py-8 text-gray-600">Loading ban list...</div>
      ) : (
        <>
          <DataTable data={bans} columns={columns} emptyMessage="No bans found" />

          {totalPages > 1 && (
            <Suspense fallback={<div className="text-center text-gray-500">Loading...</div>}>
              <Pagination currentPage={currentPage} totalItems={totalItems} itemsPerPage={50} />
            </Suspense>
          )}
        </>
      )}

      <Dialog
        isOpen={dialogState.isOpen}
        onClose={() => setDialogState({ ...dialogState, isOpen: false })}
        onConfirm={dialogState.onConfirm}
        title={dialogState.title}
        message={dialogState.message}
        type={dialogState.type}
      />
    </div>
  )
}
