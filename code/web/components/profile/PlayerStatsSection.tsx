'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useEffect, useState, useMemo } from 'react'
import { tournamentApi } from '@/lib/api'
import { calculateTournamentStats, mapTournamentEntry } from '@/lib/mappers'
import type { TournamentHistoryDto } from '@/lib/types'

interface PlayerStatsSectionProps {
  username: string
}

type SortKey = 'placement' | 'gameName' | 'date' | 'buyIn' | 'prize' | 'profit'
type SortDir = 'asc' | 'desc'

const ROWS_PER_PAGE = 10

export function PlayerStatsSection({ username }: PlayerStatsSectionProps) {
  const [history, setHistory] = useState<TournamentHistoryDto[]>([])
  const [loading, setLoading] = useState(true)
  const [sortKey, setSortKey] = useState<SortKey>('date')
  const [sortDir, setSortDir] = useState<SortDir>('desc')
  const [page, setPage] = useState(0)

  useEffect(() => {
    let cancelled = false
    async function fetchHistory() {
      setLoading(true)
      try {
        const data = await tournamentApi.getHistory(username, 0, 1000)
        if (!cancelled) {
          setHistory(data.content)
        }
      } catch (error) {
        console.error('Failed to fetch tournament history:', error)
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    fetchHistory()
    return () => {
      cancelled = true
    }
  }, [username])

  const stats = useMemo(() => calculateTournamentStats(history), [history])

  const entries = useMemo(() => history.map(mapTournamentEntry), [history])

  // Cumulative P/L data points (chronological order)
  const plData = useMemo(() => {
    const sorted = [...entries].sort(
      (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
    )
    let cumulative = 0
    return sorted.map((e) => {
      cumulative += e.prize - e.buyIn
      return { date: e.date, value: cumulative }
    })
  }, [entries])

  // Sorted and paginated entries for the table
  const sortedEntries = useMemo(() => {
    const sorted = [...entries].sort((a, b) => {
      let cmp = 0
      switch (sortKey) {
        case 'placement':
          cmp = a.placement - b.placement
          break
        case 'gameName':
          cmp = a.gameName.localeCompare(b.gameName)
          break
        case 'date':
          cmp = new Date(a.date).getTime() - new Date(b.date).getTime()
          break
        case 'buyIn':
          cmp = a.buyIn - b.buyIn
          break
        case 'prize':
          cmp = a.prize - b.prize
          break
        case 'profit':
          cmp = a.prize - a.buyIn - (b.prize - b.buyIn)
          break
      }
      return sortDir === 'asc' ? cmp : -cmp
    })
    return sorted
  }, [entries, sortKey, sortDir])

  const totalPages = Math.max(1, Math.ceil(sortedEntries.length / ROWS_PER_PAGE))
  const paginatedEntries = sortedEntries.slice(
    page * ROWS_PER_PAGE,
    (page + 1) * ROWS_PER_PAGE
  )

  function handleSort(key: SortKey) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(key)
      setSortDir('asc')
    }
    setPage(0)
  }

  function sortIndicator(key: SortKey) {
    if (sortKey !== key) return ''
    return sortDir === 'asc' ? ' \u25B2' : ' \u25BC'
  }

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-2xl font-bold mb-4">Tournament Statistics</h2>
        <div className="text-center py-8 text-gray-500">Loading...</div>
      </div>
    )
  }

  if (history.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-2xl font-bold mb-4">Tournament Statistics</h2>
        <div className="text-center py-8 text-gray-500">No tournament history available.</div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-2xl font-bold mb-4">Tournament Statistics</h2>

      {/* Summary Stats Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 mb-6">
        <StatCard label="Games Played" value={stats.totalGames.toLocaleString()} />
        <StatCard
          label="Win Rate"
          value={`${stats.winRate.toFixed(1)}%`}
        />
        <StatCard label="Best Finish" value={ordinal(stats.bestFinish)} />
        <StatCard
          label="Avg Placement"
          value={stats.avgPlacement.toFixed(1)}
        />
        <StatCard
          label="Profit/Loss"
          value={formatCurrency(stats.profitLoss)}
          valueClass={stats.profitLoss >= 0 ? 'text-green-600' : 'text-red-600'}
        />
      </div>

      {/* P/L Chart */}
      <PLChart data={plData} />

      {/* History Table */}
      <div className="mt-6">
        <h3 className="text-lg font-semibold mb-3">Tournament History</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b-2 border-gray-200">
                <SortableHeader label="Finish" sortKey="placement" currentKey={sortKey} onClick={handleSort} indicator={sortIndicator} />
                <SortableHeader label="Tournament" sortKey="gameName" currentKey={sortKey} onClick={handleSort} indicator={sortIndicator} />
                <SortableHeader label="Date" sortKey="date" currentKey={sortKey} onClick={handleSort} indicator={sortIndicator} />
                <SortableHeader label="Buy-in" sortKey="buyIn" currentKey={sortKey} onClick={handleSort} indicator={sortIndicator} />
                <SortableHeader label="Prize" sortKey="prize" currentKey={sortKey} onClick={handleSort} indicator={sortIndicator} />
                <SortableHeader label="Profit/Loss" sortKey="profit" currentKey={sortKey} onClick={handleSort} indicator={sortIndicator} />
              </tr>
            </thead>
            <tbody>
              {paginatedEntries.map((entry) => {
                const profit = entry.prize - entry.buyIn
                return (
                  <tr key={entry.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-2 px-3 text-center">{ordinal(entry.placement)}</td>
                    <td className="py-2 px-3">{entry.gameName}</td>
                    <td className="py-2 px-3">{formatDate(entry.date)}</td>
                    <td className="py-2 px-3 text-right">{formatCurrency(entry.buyIn)}</td>
                    <td className="py-2 px-3 text-right">{formatCurrency(entry.prize)}</td>
                    <td className={`py-2 px-3 text-right ${profit >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {formatCurrency(profit)}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between mt-4">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1 border rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              Previous
            </button>
            <span className="text-sm text-gray-600">
              Page {page + 1} of {totalPages}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 border rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

/** Single stat card */
function StatCard({
  label,
  value,
  valueClass = '',
}: {
  label: string
  value: string
  valueClass?: string
}) {
  return (
    <div className="bg-gray-50 rounded-lg p-4 text-center">
      <div className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</div>
      <div className={`text-2xl font-bold mt-1 ${valueClass}`}>{value}</div>
    </div>
  )
}

/** Sortable table header */
function SortableHeader({
  label,
  sortKey,
  currentKey,
  onClick,
  indicator,
}: {
  label: string
  sortKey: SortKey
  currentKey: SortKey
  onClick: (key: SortKey) => void
  indicator: (key: SortKey) => string
}) {
  return (
    <th
      className={`py-2 px-3 text-left cursor-pointer select-none hover:text-blue-600 ${
        currentKey === sortKey ? 'text-blue-600' : 'text-gray-600'
      }`}
      onClick={() => onClick(sortKey)}
    >
      {label}{indicator(sortKey)}
    </th>
  )
}

/** Cumulative P/L line chart rendered as SVG */
function PLChart({ data }: { data: Array<{ date: string; value: number }> }) {
  if (data.length === 0) return null

  const width = 600
  const height = 200
  const padLeft = 60
  const padRight = 20
  const padTop = 20
  const padBottom = 30
  const chartW = width - padLeft - padRight
  const chartH = height - padTop - padBottom

  const values = data.map((d) => d.value)
  const minVal = Math.min(0, ...values)
  const maxVal = Math.max(0, ...values)
  const range = maxVal - minVal || 1

  function x(i: number) {
    return padLeft + (data.length === 1 ? chartW / 2 : (i / (data.length - 1)) * chartW)
  }

  function y(val: number) {
    return padTop + chartH - ((val - minVal) / range) * chartH
  }

  const points = data.map((d, i) => `${x(i)},${y(d.value)}`).join(' ')
  const zeroY = y(0)

  // Date labels: show first, middle, and last
  const dateLabels: Array<{ index: number; label: string }> = []
  if (data.length >= 1) dateLabels.push({ index: 0, label: shortDate(data[0].date) })
  if (data.length >= 3) dateLabels.push({ index: Math.floor(data.length / 2), label: shortDate(data[Math.floor(data.length / 2)].date) })
  if (data.length >= 2) dateLabels.push({ index: data.length - 1, label: shortDate(data[data.length - 1].date) })

  // Y-axis labels
  const yLabels = [minVal, 0, maxVal].filter((v, i, arr) => arr.indexOf(v) === i)

  return (
    <div data-testid="pl-chart" className="mb-4">
      <h3 className="text-lg font-semibold mb-2">Cumulative Profit/Loss</h3>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full max-w-2xl"
        role="img"
        aria-label="Cumulative profit and loss chart"
      >
        {/* Zero line */}
        <line
          x1={padLeft}
          y1={zeroY}
          x2={width - padRight}
          y2={zeroY}
          stroke="#9CA3AF"
          strokeWidth="1"
          strokeDasharray="4 4"
        />

        {/* Y-axis labels */}
        {yLabels.map((val) => (
          <text
            key={val}
            x={padLeft - 8}
            y={y(val) + 4}
            textAnchor="end"
            className="text-[10px] fill-gray-500"
          >
            {formatCurrency(val)}
          </text>
        ))}

        {/* Data line */}
        <polyline
          points={points}
          fill="none"
          stroke={data[data.length - 1].value >= 0 ? '#16A34A' : '#DC2626'}
          strokeWidth="2"
          strokeLinejoin="round"
          strokeLinecap="round"
        />

        {/* Data points */}
        {data.map((d, i) => (
          <circle
            key={i}
            cx={x(i)}
            cy={y(d.value)}
            r="3"
            fill={d.value >= 0 ? '#16A34A' : '#DC2626'}
          />
        ))}

        {/* X-axis date labels */}
        {dateLabels.map(({ index, label }) => (
          <text
            key={index}
            x={x(index)}
            y={height - 5}
            textAnchor="middle"
            className="text-[10px] fill-gray-500"
          >
            {label}
          </text>
        ))}
      </svg>
    </div>
  )
}

/** Format number as currency */
function formatCurrency(value: number): string {
  const abs = Math.abs(value)
  const formatted = abs.toLocaleString()
  if (value < 0) return `-$${formatted}`
  return `$${formatted}`
}

/** Format date string for display */
function formatDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString()
  } catch {
    return dateStr
  }
}

/** Short date for chart axis */
function shortDate(dateStr: string): string {
  try {
    const d = new Date(dateStr)
    return `${d.getMonth() + 1}/${d.getDate()}`
  } catch {
    return dateStr
  }
}

/** Convert number to ordinal string (1st, 2nd, 3rd, etc.) */
function ordinal(n: number): string {
  if (n === 0) return '-'
  const s = ['th', 'st', 'nd', 'rd']
  const v = n % 100
  return n + (s[(v - 20) % 10] || s[v] || s[0])
}
