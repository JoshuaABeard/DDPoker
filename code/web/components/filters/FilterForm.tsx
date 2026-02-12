'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Generic Filter Form Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useRouter, useSearchParams } from 'next/navigation'
import { FormEvent, useState } from 'react'

interface FilterFormProps {
  showDateRange?: boolean
  showNameSearch?: boolean
  showGamesFilter?: boolean
  baseUrl?: string
  onSubmit?: (filters: Record<string, string>) => void
}

export function FilterForm({
  showDateRange = false,
  showNameSearch = false,
  showGamesFilter = false,
  baseUrl,
  onSubmit,
}: FilterFormProps) {
  const router = useRouter()
  const searchParams = useSearchParams()

  const [beginDate, setBeginDate] = useState(searchParams.get('begin') || '')
  const [endDate, setEndDate] = useState(searchParams.get('end') || '')
  const [name, setName] = useState(searchParams.get('name') || '')
  const [minGames, setMinGames] = useState(searchParams.get('games') || '')

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()

    const params = new URLSearchParams()

    if (beginDate) params.set('begin', beginDate)
    if (endDate) params.set('end', endDate)
    if (name) params.set('name', name)
    if (minGames) params.set('games', minGames)

    // Reset to page 1 when filtering
    params.set('page', '1')

    // Preserve other search params
    searchParams.forEach((value, key) => {
      if (!['begin', 'end', 'name', 'games', 'page'].includes(key)) {
        params.set(key, value)
      }
    })

    if (onSubmit) {
      const filters: Record<string, string> = {}
      params.forEach((value, key) => {
        filters[key] = value
      })
      onSubmit(filters)
    } else {
      const url = baseUrl || window.location.pathname
      router.push(`${url}?${params.toString()}`)
    }
  }

  const handleReset = () => {
    setBeginDate('')
    setEndDate('')
    setName('')
    setMinGames('')

    const url = baseUrl || window.location.pathname
    router.push(url)
  }

  return (
    <form onSubmit={handleSubmit} className="bg-gray-100 p-4 rounded-lg mb-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {showDateRange && (
          <>
            <div>
              <label htmlFor="beginDate" className="block text-sm font-medium mb-1">
                Start Date
              </label>
              <input
                type="date"
                id="beginDate"
                value={beginDate}
                onChange={(e) => setBeginDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>

            <div>
              <label htmlFor="endDate" className="block text-sm font-medium mb-1">
                End Date
              </label>
              <input
                type="date"
                id="endDate"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-green-500"
              />
            </div>
          </>
        )}

        {showNameSearch && (
          <div>
            <label htmlFor="name" className="block text-sm font-medium mb-1">
              Player Name
            </label>
            <input
              type="text"
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Search (use = for exact)"
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-green-500"
            />
            <div className="text-xs text-gray-500 mt-1">
              Prefix with = for exact match (e.g., =John)
            </div>
          </div>
        )}

        {showGamesFilter && (
          <div>
            <label htmlFor="minGames" className="block text-sm font-medium mb-1">
              Minimum Games
            </label>
            <input
              type="number"
              id="minGames"
              value={minGames}
              onChange={(e) => setMinGames(e.target.value)}
              min="1"
              placeholder="e.g., 10"
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
        )}

        <div className="flex items-end gap-2">
          <button
            type="submit"
            className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition-colors"
          >
            Apply Filters
          </button>
          <button
            type="button"
            onClick={handleReset}
            className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 transition-colors"
          >
            Reset
          </button>
        </div>
      </div>
    </form>
  )
}
