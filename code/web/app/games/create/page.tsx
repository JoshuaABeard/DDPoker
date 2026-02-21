/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useSearchParams, useRouter } from 'next/navigation'
import { Suspense, useState } from 'react'
import { gameServerApi } from '@/lib/api'
import type { GameConfigDto } from '@/lib/game/types'

const DEFAULT_BLIND_STRUCTURE: GameConfigDto['blindStructure'] = [
  { level: 1,  small: 25,   big: 50,    ante: 0,   durationMinutes: 15 },
  { level: 2,  small: 50,   big: 100,   ante: 0,   durationMinutes: 15 },
  { level: 3,  small: 75,   big: 150,   ante: 25,  durationMinutes: 15 },
  { level: 4,  small: 100,  big: 200,   ante: 25,  durationMinutes: 15 },
  { level: 5,  small: 150,  big: 300,   ante: 50,  durationMinutes: 15 },
  { level: 6,  small: 200,  big: 400,   ante: 50,  durationMinutes: 15 },
  { level: 7,  small: 300,  big: 600,   ante: 75,  durationMinutes: 15 },
  { level: 8,  small: 400,  big: 800,   ante: 100, durationMinutes: 15 },
  { level: 9,  small: 600,  big: 1200,  ante: 200, durationMinutes: 15 },
  { level: 10, small: 800,  big: 1600,  ante: 200, durationMinutes: 15 },
]

function CreateGameForm() {
  const searchParams = useSearchParams()
  const isPractice = searchParams.get('practice') === 'true'
  const router = useRouter()

  const [name, setName] = useState('')
  const [maxPlayers, setMaxPlayers] = useState(9)
  const [buyIn, setBuyIn] = useState(0)
  const [startingChips, setStartingChips] = useState(10_000)
  const [password, setPassword] = useState('')
  const [fillWithAI, setFillWithAI] = useState(isPractice)
  const [aiCount, setAiCount] = useState(8)
  const [allowRebuys, setAllowRebuys] = useState(false)
  const [rebuyCost, setRebuyCost] = useState(0)
  const [rebuyChips, setRebuyChips] = useState(10_000)
  const [rebuyLimit, setRebuyLimit] = useState(2)
  const [allowAddon, setAllowAddon] = useState(false)
  const [addonCost, setAddonCost] = useState(0)
  const [addonChips, setAddonChips] = useState(5_000)
  const [actionTimeoutSeconds, setActionTimeoutSeconds] = useState(30)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handlePractice() {
    setError(null)
    setLoading(true)
    try {
      const { gameId } = await gameServerApi.createPracticeGame({
        name: name || 'Quick Practice',
        startingChips,
        blindStructure: DEFAULT_BLIND_STRUCTURE,
        actionTimeoutSeconds,
        fillWithAI: true,
        aiCount: 8,
        maxPlayers: 9,
        buyIn: 0,
        allowRebuys: false,
        rebuyLimit: 0,
        rebuyCost: 0,
        rebuyChips: 0,
        allowAddon: false,
        addonCost: 0,
        addonChips: 0,
      })
      router.push(`/games/${gameId}/play`)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to create practice game.')
    } finally {
      setLoading(false)
    }
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) {
      setError('Game name is required.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      const config: GameConfigDto = {
        name: name.trim(),
        maxPlayers,
        buyIn,
        startingChips,
        blindStructure: DEFAULT_BLIND_STRUCTURE,
        fillWithAI,
        aiCount: fillWithAI ? aiCount : 0,
        password: password || undefined,
        allowRebuys,
        rebuyLimit,
        rebuyCost,
        rebuyChips,
        allowAddon,
        addonCost,
        addonChips,
        actionTimeoutSeconds,
      }
      const game = await gameServerApi.createGame(config)
      router.push(`/games/${game.gameId}/lobby`)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to create game.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">
        {isPractice ? 'Quick Practice Game' : 'Create Game'}
      </h1>

      {isPractice && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6 text-sm text-blue-800">
          Quick Practice fills all seats with AI opponents and starts immediately.
        </div>
      )}

      <form onSubmit={handleCreate} className="space-y-6">
        {/* Basic settings */}
        <section className="space-y-4">
          <h2 className="text-lg font-semibold border-b border-gray-200 pb-1">Basic Settings</h2>

          <div>
            <label htmlFor="name" className="block text-sm font-medium mb-1">
              Game Name {!isPractice && <span className="text-red-500">*</span>}
            </label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required={!isPractice}
              maxLength={60}
              placeholder={isPractice ? 'Quick Practice' : 'Enter game name'}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="maxPlayers" className="block text-sm font-medium mb-1">
                Max Players
              </label>
              <select
                id="maxPlayers"
                value={maxPlayers}
                onChange={(e) => setMaxPlayers(Number(e.target.value))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {[2, 3, 4, 5, 6, 7, 8, 9, 10].map((n) => (
                  <option key={n} value={n}>
                    {n} players
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="startingChips" className="block text-sm font-medium mb-1">
                Starting Chips
              </label>
              <input
                id="startingChips"
                type="number"
                value={startingChips}
                onChange={(e) => setStartingChips(Math.max(1, Number(e.target.value)))}
                min={100}
                max={1_000_000}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
        </section>

        {/* AI settings */}
        <section className="space-y-3">
          <h2 className="text-lg font-semibold border-b border-gray-200 pb-1">AI Opponents</h2>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              checked={fillWithAI}
              onChange={(e) => setFillWithAI(e.target.checked)}
              className="rounded"
            />
            Fill empty seats with AI players
          </label>
          {fillWithAI && (
            <div>
              <label htmlFor="aiCount" className="block text-sm font-medium mb-1">
                AI Player Count
              </label>
              <input
                id="aiCount"
                type="number"
                value={aiCount}
                onChange={(e) => setAiCount(Math.max(1, Math.min(maxPlayers - 1, Number(e.target.value))))}
                min={1}
                max={maxPlayers - 1}
                className="w-32 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          )}
        </section>

        {/* Advanced (collapsible) */}
        <details className="border border-gray-200 rounded-lg">
          <summary className="px-4 py-3 text-sm font-semibold cursor-pointer hover:bg-gray-50">
            Advanced Settings
          </summary>
          <div className="px-4 pb-4 pt-2 space-y-4">
            <div>
              <label htmlFor="password" className="block text-sm font-medium mb-1">
                Password (leave blank for public)
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                maxLength={100}
                autoComplete="off"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="actionTimeout" className="block text-sm font-medium mb-1">
                Action Timeout (seconds)
              </label>
              <input
                id="actionTimeout"
                type="number"
                value={actionTimeoutSeconds}
                onChange={(e) => setActionTimeoutSeconds(Math.max(10, Number(e.target.value)))}
                min={10}
                max={300}
                className="w-32 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="space-y-2">
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={allowRebuys}
                  onChange={(e) => setAllowRebuys(e.target.checked)}
                  className="rounded"
                />
                Allow rebuys
              </label>
              {allowRebuys && (
                <div className="grid grid-cols-3 gap-2 ml-6">
                  <div>
                    <label className="block text-xs font-medium mb-1">Cost</label>
                    <input type="number" value={rebuyCost} min={0}
                      onChange={(e) => setRebuyCost(Number(e.target.value))}
                      className="w-full border border-gray-300 rounded px-2 py-1 text-xs" />
                  </div>
                  <div>
                    <label className="block text-xs font-medium mb-1">Chips</label>
                    <input type="number" value={rebuyChips} min={1}
                      onChange={(e) => setRebuyChips(Number(e.target.value))}
                      className="w-full border border-gray-300 rounded px-2 py-1 text-xs" />
                  </div>
                  <div>
                    <label className="block text-xs font-medium mb-1">Limit</label>
                    <input type="number" value={rebuyLimit} min={1}
                      onChange={(e) => setRebuyLimit(Number(e.target.value))}
                      className="w-full border border-gray-300 rounded px-2 py-1 text-xs" />
                  </div>
                </div>
              )}
            </div>

            <div className="space-y-2">
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={allowAddon}
                  onChange={(e) => setAllowAddon(e.target.checked)}
                  className="rounded"
                />
                Allow add-on
              </label>
              {allowAddon && (
                <div className="grid grid-cols-2 gap-2 ml-6">
                  <div>
                    <label className="block text-xs font-medium mb-1">Cost</label>
                    <input type="number" value={addonCost} min={0}
                      onChange={(e) => setAddonCost(Number(e.target.value))}
                      className="w-full border border-gray-300 rounded px-2 py-1 text-xs" />
                  </div>
                  <div>
                    <label className="block text-xs font-medium mb-1">Chips</label>
                    <input type="number" value={addonChips} min={1}
                      onChange={(e) => setAddonChips(Number(e.target.value))}
                      className="w-full border border-gray-300 rounded px-2 py-1 text-xs" />
                  </div>
                </div>
              )}
            </div>
          </div>
        </details>

        {error && (
          <p className="text-red-600 text-sm" role="alert">
            {error}
          </p>
        )}

        <div className="flex gap-3">
          {isPractice ? (
            <button
              type="button"
              onClick={handlePractice}
              disabled={loading}
              className="flex-1 py-2 bg-green-700 hover:bg-green-600 disabled:opacity-50 text-white font-bold rounded-lg transition-colors"
            >
              {loading ? 'Starting…' : 'Start Practice Game'}
            </button>
          ) : (
            <>
              <button
                type="submit"
                disabled={loading}
                className="flex-1 py-2 bg-green-700 hover:bg-green-600 disabled:opacity-50 text-white font-bold rounded-lg transition-colors"
              >
                {loading ? 'Creating…' : 'Create Game'}
              </button>
              <button
                type="button"
                disabled={loading}
                onClick={handlePractice}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 disabled:opacity-50 text-gray-800 font-semibold rounded-lg transition-colors text-sm"
              >
                Quick Practice
              </button>
            </>
          )}
        </div>
      </form>
    </div>
  )
}

export default function CreateGamePage() {
  return (
    <Suspense fallback={<div className="text-gray-500 text-sm">Loading…</div>}>
      <CreateGameForm />
    </Suspense>
  )
}
