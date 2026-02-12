'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Leaderboard Filter Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useRouter, useSearchParams } from 'next/navigation'
import { FilterForm } from '@/components/filters/FilterForm'

export function LeaderboardFilter() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const currentType = searchParams.get('type') || 'ddr1'

  const handleTypeChange = (type: string) => {
    const params = new URLSearchParams(searchParams.toString())
    params.set('type', type)
    params.set('page', '1') // Reset to page 1 when changing type
    router.push(`/online/leaderboard?${params.toString()}`)
  }

  return (
    <div className="space-y-4 mb-6">
      <div className="flex gap-2">
        <button
          onClick={() => handleTypeChange('ddr1')}
          className={`px-4 py-2 rounded transition-colors ${
            currentType === 'ddr1'
              ? 'bg-green-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          DDR1 Mode
        </button>
        <button
          onClick={() => handleTypeChange('roi')}
          className={`px-4 py-2 rounded transition-colors ${
            currentType === 'roi'
              ? 'bg-green-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          ROI Mode
        </button>
      </div>

      <FilterForm showDateRange showNameSearch showGamesFilter />
    </div>
  )
}
