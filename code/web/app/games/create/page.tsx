/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useSearchParams, useRouter } from 'next/navigation'
import { Suspense, useEffect, useState } from 'react'
import { gameServerApi, templateApi } from '@/lib/api'
import type { TemplateDto } from '@/lib/types'
import type { GameConfigDto } from '@/lib/game/types'
import { BLIND_PRESETS } from '@/lib/game/blindPresets'

const SKILL_PRESETS = [
  { label: 'Novice', value: 2 },
  { label: 'Beginner', value: 4 },
  { label: 'Intermediate', value: 6 },
  { label: 'Advanced', value: 8 },
  { label: 'Expert', value: 10 },
] as const

const PLAY_STYLES = ['Tight-Passive', 'Tight-Aggressive', 'Loose-Passive', 'Loose-Aggressive'] as const

const DEFAULT_BLIND_STRUCTURE: GameConfigDto['blindStructure'] = [
  { smallBlind: 25,   bigBlind: 50,    ante: 0,   minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
  { smallBlind: 50,   bigBlind: 100,   ante: 0,   minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
  { smallBlind: 75,   bigBlind: 150,   ante: 25,  minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
  { smallBlind: 100,  bigBlind: 200,   ante: 25,  minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
  { smallBlind: 150,  bigBlind: 300,   ante: 50,  minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
  { smallBlind: 200,  bigBlind: 400,   ante: 50,  minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
  { smallBlind: 300,  bigBlind: 600,   ante: 75,  minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
  { smallBlind: 400,  bigBlind: 800,   ante: 100, minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
  { smallBlind: 600,  bigBlind: 1200,  ante: 200, minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
  { smallBlind: 800,  bigBlind: 1600,  ante: 200, minutes: 15, isBreak: false, gameType: 'NOLIMIT_HOLDEM' },
]

const DEFAULT_AI_PLAYERS: Array<{ name: string; skillLevel: number; playStyle: string }> = [
  { name: 'AI 1', skillLevel: 6, playStyle: 'Tight-Aggressive' },
  { name: 'AI 2', skillLevel: 6, playStyle: 'Tight-Aggressive' },
  { name: 'AI 3', skillLevel: 6, playStyle: 'Tight-Aggressive' },
  { name: 'AI 4', skillLevel: 6, playStyle: 'Tight-Aggressive' },
  { name: 'AI 5', skillLevel: 6, playStyle: 'Tight-Aggressive' },
  { name: 'AI 6', skillLevel: 6, playStyle: 'Tight-Aggressive' },
  { name: 'AI 7', skillLevel: 6, playStyle: 'Tight-Aggressive' },
  { name: 'AI 8', skillLevel: 6, playStyle: 'Tight-Aggressive' },
]

function CreateGameForm() {
  const searchParams = useSearchParams()
  const isPractice = searchParams.get('practice') === 'true'
  const router = useRouter()

  // Basic settings
  const [name, setName] = useState('')
  const [maxPlayers, setMaxPlayers] = useState(9)
  const [buyIn, setBuyIn] = useState(0)
  const [startingChips, setStartingChips] = useState(10_000)
  const [password, setPassword] = useState('')

  // Task 6.1 step 1 — game type
  const [defaultGameType, setDefaultGameType] = useState<'NOLIMIT_HOLDEM' | 'POTLIMIT_HOLDEM' | 'LIMIT_HOLDEM'>('NOLIMIT_HOLDEM')

  // Task 6.1 step 2 — blind structure
  const [blindStructure, setBlindStructure] = useState([...DEFAULT_BLIND_STRUCTURE])

  // Task 8 — blind preset selector
  const [selectedPreset, setSelectedPreset] = useState<string>('custom')

  // Task 6.1 step 3 — level advance mode
  const [levelAdvanceMode, setLevelAdvanceMode] = useState<'TIME' | 'HANDS'>('TIME')
  const [handsPerLevel, setHandsPerLevel] = useState(20)

  // Task 6.4 step 2 — AI player list (replaces aiCount)
  const [fillComputer, setFillComputer] = useState(isPractice)
  const [aiPlayerList, setAiPlayerList] = useState<Array<{ name: string; skillLevel: number; playStyle: string }>>([
    ...DEFAULT_AI_PLAYERS,
  ])

  // Practice options
  const [aiFaceUp, setAiFaceUp] = useState(false)
  const [pauseAllin, setPauseAllin] = useState(false)
  const [autoDeal, setAutoDeal] = useState(true)
  const [zipMode, setZipMode] = useState(false)

  // Rebuys / addons
  const [allowRebuys, setAllowRebuys] = useState(false)
  const [rebuyCost, setRebuyCost] = useState(0)
  const [rebuyChips, setRebuyChips] = useState(10_000)
  const [rebuyLimit, setRebuyLimit] = useState(2)
  const [allowAddon, setAllowAddon] = useState(false)
  const [addonCost, setAddonCost] = useState(0)
  const [addonChips, setAddonChips] = useState(5_000)

  // Timeouts
  const [actionTimeoutSeconds, setActionTimeoutSeconds] = useState(30)

  // Task 6.2 step 1 — payout
  const [payoutSpots, setPayoutSpots] = useState(3)

  // Task 6.2 step 2 — bounty
  const [bountyEnabled, setBountyEnabled] = useState(false)
  const [bountyAmount, setBountyAmount] = useState(0)

  // Task 6.3 step 2 — boot settings
  const [bootSitout, setBootSitout] = useState(false)
  const [bootDisconnect, setBootDisconnect] = useState(false)
  const [bootAfterHands, setBootAfterHands] = useState(3)

  // Task 6.3 step 3 — late registration
  const [lateRegistration, setLateRegistration] = useState(false)
  const [lateRegUntilLevel, setLateRegUntilLevel] = useState(3)

  // UI state
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  // Template management
  const [templates, setTemplates] = useState<TemplateDto[]>([])
  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('')
  const [templateName, setTemplateName] = useState('')
  const [showSaveDialog, setShowSaveDialog] = useState(false)
  const [templateLoading, setTemplateLoading] = useState(false)

  useEffect(() => {
    templateApi.list().then(setTemplates).catch(() => {})
  }, [])

  // Blind structure helpers
  function updateBlindLevel(
    index: number,
    field: keyof GameConfigDto['blindStructure'][number],
    value: number | boolean | string,
  ) {
    setSelectedPreset('custom')
    setBlindStructure((prev) =>
      prev.map((level, i) => (i === index ? { ...level, [field]: value } : level)),
    )
  }

  function addBlindLevel() {
    setSelectedPreset('custom')
    setBlindStructure((prev) => {
      const last = prev[prev.length - 1] ?? DEFAULT_BLIND_STRUCTURE[0]
      return [...prev, { ...last }]
    })
  }

  function addBreakLevel() {
    setSelectedPreset('custom')
    setBlindStructure((prev) => [
      ...prev,
      { smallBlind: 0, bigBlind: 0, ante: 0, minutes: 5, isBreak: true, gameType: 'NOLIMIT_HOLDEM' },
    ])
  }

  function removeBlindLevel(index: number) {
    setSelectedPreset('custom')
    setBlindStructure((prev) => prev.filter((_, i) => i !== index))
  }

  // AI player list helpers
  function updateAiPlayer(index: number, field: 'name' | 'skillLevel' | 'playStyle', value: string | number) {
    setAiPlayerList((prev) =>
      prev.map((p, i) => (i === index ? { ...p, [field]: value } : p)),
    )
  }

  function addAiPlayer() {
    setAiPlayerList((prev) => [
      ...prev,
      { name: `AI ${prev.length + 1}`, skillLevel: 6, playStyle: 'Tight-Aggressive' },
    ])
  }

  function removeAiPlayer(index: number) {
    setAiPlayerList((prev) => prev.filter((_, i) => i !== index))
  }

  function gatherConfig() {
    return {
      maxPlayers, startingChips, defaultGameType, blindStructure, levelAdvanceMode,
      handsPerLevel, fillComputer, aiPlayerList, payoutSpots, bountyEnabled, bountyAmount,
      allowRebuys, rebuyCost, rebuyChips, rebuyLimit, allowAddon, addonCost, addonChips,
      actionTimeoutSeconds, bootSitout, bootDisconnect, bootAfterHands,
      lateRegistration, lateRegUntilLevel, aiFaceUp, pauseAllin, autoDeal, zipMode,
    }
  }

  function loadTemplate(id: string) {
    const template = templates.find(t => t.id === Number(id))
    if (!template) return
    const cfg = JSON.parse(template.config)
    setMaxPlayers(cfg.maxPlayers ?? 9)
    setBuyIn(cfg.buyIn ?? 0)
    setStartingChips(cfg.startingChips ?? 10000)
    setDefaultGameType(cfg.defaultGameType ?? 'NOLIMIT_HOLDEM')
    setBlindStructure(cfg.blindStructure ?? [...DEFAULT_BLIND_STRUCTURE])
    setSelectedPreset('custom')
    setLevelAdvanceMode(cfg.levelAdvanceMode ?? 'TIME')
    setHandsPerLevel(cfg.handsPerLevel ?? 20)
    setFillComputer(cfg.fillComputer ?? isPractice)
    setAiPlayerList(cfg.aiPlayerList ?? [...DEFAULT_AI_PLAYERS])
    setPayoutSpots(cfg.payoutSpots ?? 3)
    setBountyEnabled(cfg.bountyEnabled ?? false)
    setBountyAmount(cfg.bountyAmount ?? 0)
    setAllowRebuys(cfg.allowRebuys ?? false)
    setRebuyCost(cfg.rebuyCost ?? 0)
    setRebuyChips(cfg.rebuyChips ?? 10000)
    setRebuyLimit(cfg.rebuyLimit ?? 2)
    setAllowAddon(cfg.allowAddon ?? false)
    setAddonCost(cfg.addonCost ?? 0)
    setAddonChips(cfg.addonChips ?? 5000)
    setActionTimeoutSeconds(cfg.actionTimeoutSeconds ?? 30)
    setBootSitout(cfg.bootSitout ?? false)
    setBootDisconnect(cfg.bootDisconnect ?? false)
    setBootAfterHands(cfg.bootAfterHands ?? 3)
    setLateRegistration(cfg.lateRegistration ?? false)
    setLateRegUntilLevel(cfg.lateRegUntilLevel ?? 3)
    setAiFaceUp(cfg.aiFaceUp ?? false)
    setPauseAllin(cfg.pauseAllin ?? false)
    setAutoDeal(cfg.autoDeal ?? true)
    setZipMode(cfg.zipMode ?? false)
  }

  async function saveAsTemplate() {
    if (!templateName.trim()) return
    setTemplateLoading(true)
    try {
      const config = gatherConfig()
      const created = await templateApi.create(templateName.trim(), config)
      setTemplates(prev => [created, ...prev])
      setTemplateName('')
      setShowSaveDialog(false)
      setSelectedTemplateId(String(created.id))
    } catch (e) {
      console.error('Failed to save template:', e)
    } finally {
      setTemplateLoading(false)
    }
  }

  async function deleteTemplate() {
    const id = Number(selectedTemplateId)
    if (!id) return
    if (!confirm('Delete this template?')) return
    setTemplateLoading(true)
    try {
      await templateApi.delete(id)
      setTemplates(prev => prev.filter(t => t.id !== id))
      setSelectedTemplateId('')
    } catch (e) {
      console.error('Failed to delete template:', e)
    } finally {
      setTemplateLoading(false)
    }
  }

  function buildConfig(overrides: Partial<{
    name: string
    fillComputer: boolean
    aiPlayerList: Array<{ name: string; skillLevel: number; playStyle: string }>
  }> = {}): GameConfigDto {
    const effectiveName = overrides.name ?? name
    const effectiveFill = overrides.fillComputer ?? fillComputer
    const effectiveAiPlayerList = overrides.aiPlayerList ?? aiPlayerList

    return {
      name: effectiveName,
      maxPlayers,
      fillComputer: effectiveFill,
      buyIn,
      startingChips,
      blindStructure,
      defaultGameType,
      levelAdvanceMode,
      handsPerLevel: levelAdvanceMode === 'HANDS' ? handsPerLevel : undefined,
      rebuys: {
        enabled: allowRebuys,
        cost: rebuyCost,
        chips: rebuyChips,
        maxRebuys: rebuyLimit,
      },
      addons: {
        enabled: allowAddon,
        cost: addonCost,
        chips: addonChips,
      },
      timeouts: {
        defaultSeconds: actionTimeoutSeconds,
      },
      aiPlayers: effectiveFill ? effectiveAiPlayerList : [],
      // percent and prizePool are computed server-side for STANDARD type; send 0 as placeholders
      payout: { type: 'STANDARD', spots: payoutSpots, percent: 0, prizePool: 0, allocationType: 'PERCENT' },
      bounty: bountyEnabled ? { enabled: true, amount: bountyAmount } : undefined,
      boot: {
        bootSitout,
        bootSitoutCount: bootAfterHands, // shared counter — sits-out and disconnected players use same threshold
        bootDisconnect,
        bootDisconnectCount: bootAfterHands,
      },
      lateRegistration: {
        enabled: lateRegistration,
        untilLevel: lateRegUntilLevel,
        chipMode: 'STARTING',
      },
      practiceConfig: isPractice ? {
        aiFaceUp,
        pauseAllinInteractive: pauseAllin,
        autoDeal,
        zipModeEnabled: zipMode,
      } : undefined,
      password: password || undefined,
    }
  }

  async function handlePractice() {
    setError(null)
    setLoading(true)
    try {
      const { gameId } = await gameServerApi.createPracticeGame(
        buildConfig({
          name: name || 'Quick Practice',
          fillComputer: true,
          aiPlayerList: DEFAULT_AI_PLAYERS,
        })
      )
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
      const game = await gameServerApi.createGame(buildConfig({ name: name.trim() }))
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
        {/* Template Management */}
        <div className="mb-6 p-4 bg-gray-50 rounded-lg border">
          <h3 className="text-sm font-semibold text-gray-700 mb-3">Templates</h3>
          <div className="flex flex-wrap items-center gap-2">
            <select
              value={selectedTemplateId}
              onChange={(e) => setSelectedTemplateId(e.target.value)}
              className="flex-1 min-w-[200px] rounded border-gray-300 text-sm"
            >
              <option value="">-- Select Template --</option>
              {templates.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
            <button
              type="button"
              onClick={() => loadTemplate(selectedTemplateId)}
              disabled={!selectedTemplateId || templateLoading}
              className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
            >
              Load
            </button>
            <button
              type="button"
              onClick={() => setShowSaveDialog(true)}
              disabled={templateLoading}
              className="px-3 py-1.5 text-sm bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
            >
              Save Current Settings
            </button>
            <button
              type="button"
              onClick={deleteTemplate}
              disabled={!selectedTemplateId || templateLoading}
              className="px-3 py-1.5 text-sm bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50"
            >
              Delete
            </button>
          </div>
          {showSaveDialog && (
            <div className="mt-3 flex items-center gap-2">
              <input
                type="text"
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
                placeholder="Template name"
                className="flex-1 rounded border-gray-300 text-sm"
              />
              <button
                type="button"
                onClick={saveAsTemplate}
                disabled={!templateName.trim() || templateLoading}
                className="px-3 py-1.5 text-sm bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
              >
                Save
              </button>
              <button
                type="button"
                onClick={() => { setShowSaveDialog(false); setTemplateName('') }}
                className="px-3 py-1.5 text-sm bg-gray-400 text-white rounded hover:bg-gray-500"
              >
                Cancel
              </button>
            </div>
          )}
        </div>

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
                onChange={(e) => setStartingChips(Math.max(100, Number(e.target.value)))}
                min={100}
                max={1_000_000}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Task 6.1 step 1 — Game type selector */}
          <div>
            <label htmlFor="defaultGameType" className="block text-sm font-medium mb-1">
              Game Type
            </label>
            <select
              id="defaultGameType"
              value={defaultGameType}
              onChange={(e) => setDefaultGameType(e.target.value as typeof defaultGameType)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="NOLIMIT_HOLDEM">No Limit</option>
              <option value="POTLIMIT_HOLDEM">Pot Limit</option>
              <option value="LIMIT_HOLDEM">Limit</option>
            </select>
          </div>
        </section>

        {/* Task 6.1 step 2 — Blind Structure Editor */}
        <section className="space-y-3">
          <h2 className="text-lg font-semibold border-b border-gray-200 pb-1">Blind Structure</h2>
          <div>
            <label htmlFor="blindPreset" className="block text-sm font-medium mb-1">
              Preset
            </label>
            <select
              id="blindPreset"
              value={selectedPreset}
              onChange={(e) => {
                const presetId = e.target.value
                setSelectedPreset(presetId)
                const preset = BLIND_PRESETS.find((p) => p.id === presetId)
                if (preset) {
                  setBlindStructure(preset.levels.map((l) => ({ ...l })))
                }
              }}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {BLIND_PRESETS.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} — {p.description}
                </option>
              ))}
              <option value="custom">Custom</option>
            </select>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-xs border-collapse">
              <thead>
                <tr className="text-left text-gray-500">
                  <th className="pr-2 py-1 font-medium">Level</th>
                  <th className="pr-2 py-1 font-medium">Small</th>
                  <th className="pr-2 py-1 font-medium">Big</th>
                  <th className="pr-2 py-1 font-medium">Ante</th>
                  <th className="pr-2 py-1 font-medium">Min</th>
                  <th className="py-1"></th>
                </tr>
              </thead>
              <tbody>
                {blindStructure.map((level, i) => (
                  <tr key={i} className={`border-t border-gray-100 ${level.isBreak ? 'bg-blue-900/50' : ''}`}>
                    <td className="pr-2 py-1 text-gray-500">
                      {level.isBreak ? (
                        <span className="font-semibold text-blue-400">BREAK</span>
                      ) : (
                        i + 1
                      )}
                    </td>
                    <td className="pr-2 py-1">
                      {!level.isBreak && (
                        <input
                          type="number"
                          value={level.smallBlind}
                          min={1}
                          onChange={(e) => updateBlindLevel(i, 'smallBlind', Number(e.target.value))}
                          className="w-20 border border-gray-300 rounded px-1 py-0.5"
                        />
                      )}
                    </td>
                    <td className="pr-2 py-1">
                      {!level.isBreak && (
                        <input
                          type="number"
                          value={level.bigBlind}
                          min={1}
                          onChange={(e) => updateBlindLevel(i, 'bigBlind', Number(e.target.value))}
                          className="w-20 border border-gray-300 rounded px-1 py-0.5"
                        />
                      )}
                    </td>
                    <td className="pr-2 py-1">
                      {!level.isBreak && (
                        <input
                          type="number"
                          value={level.ante}
                          min={0}
                          onChange={(e) => updateBlindLevel(i, 'ante', Number(e.target.value))}
                          className="w-16 border border-gray-300 rounded px-1 py-0.5"
                        />
                      )}
                    </td>
                    <td className="pr-2 py-1">
                      <input
                        type="number"
                        value={level.minutes}
                        min={1}
                        onChange={(e) => updateBlindLevel(i, 'minutes', Number(e.target.value))}
                        className="w-14 border border-gray-300 rounded px-1 py-0.5"
                      />
                    </td>
                    <td className="py-1">
                      <button
                        type="button"
                        onClick={() => removeBlindLevel(i)}
                        disabled={blindStructure.length <= 1}
                        className="text-red-500 hover:text-red-700 disabled:opacity-30 text-xs px-1"
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex gap-3">
            <button
              type="button"
              onClick={addBlindLevel}
              className="text-sm text-blue-600 hover:text-blue-800 font-medium"
            >
              + Add Level
            </button>
            <button
              type="button"
              onClick={addBreakLevel}
              className="text-sm text-blue-600 hover:text-blue-800 font-medium"
            >
              + Add Break
            </button>
          </div>
        </section>

        {/* Task 6.1 step 3 — Level Advance Mode */}
        <section className="space-y-3">
          <h2 className="text-lg font-semibold border-b border-gray-200 pb-1">Level Timing</h2>
          <div className="flex items-center gap-4">
            <label htmlFor="levelAdvanceMode" className="block text-sm font-medium">
              Advance levels by
            </label>
            <select
              id="levelAdvanceMode"
              value={levelAdvanceMode}
              onChange={(e) => setLevelAdvanceMode(e.target.value as 'TIME' | 'HANDS')}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="TIME">Time</option>
              <option value="HANDS">Hands</option>
            </select>
          </div>
          {levelAdvanceMode === 'HANDS' && (
            <div>
              <label htmlFor="handsPerLevel" className="block text-sm font-medium mb-1">
                Hands per Level
              </label>
              <input
                id="handsPerLevel"
                type="number"
                value={handsPerLevel}
                min={1}
                onChange={(e) => setHandsPerLevel(Math.max(1, Number(e.target.value)))}
                className="w-24 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          )}
        </section>

        {/* Task 6.2 — Payout & Bounty */}
        <section className="space-y-3">
          <h2 className="text-lg font-semibold border-b border-gray-200 pb-1">Payouts</h2>
          <div>
            <label htmlFor="payoutSpots" className="block text-sm font-medium mb-1">
              Pay top N spots
            </label>
            <input
              id="payoutSpots"
              type="number"
              value={payoutSpots}
              min={1}
              onChange={(e) => setPayoutSpots(Math.max(1, Number(e.target.value)))}
              className="w-24 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="space-y-2">
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={bountyEnabled}
                onChange={(e) => setBountyEnabled(e.target.checked)}
                className="rounded"
              />
              Enable bounties
            </label>
            {bountyEnabled && (
              <div className="ml-6">
                <label className="block text-xs font-medium mb-1">Bounty Amount</label>
                <input
                  type="number"
                  value={bountyAmount}
                  min={0}
                  onChange={(e) => setBountyAmount(Number(e.target.value))}
                  className="w-32 border border-gray-300 rounded px-2 py-1 text-xs"
                />
              </div>
            )}
          </div>
        </section>

        {/* AI settings */}
        <section className="space-y-3">
          <h2 className="text-lg font-semibold border-b border-gray-200 pb-1">AI Opponents</h2>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              checked={fillComputer}
              onChange={(e) => setFillComputer(e.target.checked)}
              className="rounded"
            />
            Fill empty seats with AI players
          </label>
          {fillComputer && (
            <div className="space-y-2">
              {aiPlayerList.map((ai, i) => {
                const currentPreset = SKILL_PRESETS.find((p) => p.value >= ai.skillLevel)?.label ?? 'Intermediate'
                return (
                  <div key={i} className="flex items-center gap-2">
                    <input
                      type="text"
                      value={ai.name}
                      onChange={(e) => updateAiPlayer(i, 'name', e.target.value)}
                      maxLength={40}
                      placeholder={`AI ${i + 1}`}
                      className="flex-1 border border-gray-300 rounded px-2 py-1 text-sm"
                    />
                    <label className="text-xs text-gray-500 whitespace-nowrap">Skill</label>
                    <select
                      value={currentPreset}
                      onChange={(e) => {
                        const preset = SKILL_PRESETS.find((p) => p.label === e.target.value)
                        if (preset) updateAiPlayer(i, 'skillLevel', preset.value)
                      }}
                      className="w-32 border border-gray-300 rounded px-2 py-1 text-sm"
                    >
                      {SKILL_PRESETS.map((p) => (
                        <option key={p.label} value={p.label}>
                          {p.label}
                        </option>
                      ))}
                    </select>
                    <label className="text-xs text-gray-500 whitespace-nowrap">Style</label>
                    <select
                      value={ai.playStyle}
                      onChange={(e) => updateAiPlayer(i, 'playStyle', e.target.value)}
                      className="w-40 border border-gray-300 rounded px-2 py-1 text-sm"
                    >
                      {PLAY_STYLES.map((style) => (
                        <option key={style} value={style}>
                          {style}
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      onClick={() => removeAiPlayer(i)}
                      disabled={aiPlayerList.length <= 1}
                      className="text-red-500 hover:text-red-700 disabled:opacity-30 text-xs"
                    >
                      Remove
                    </button>
                  </div>
                )
              })}
              <button
                type="button"
                onClick={addAiPlayer}
                className="text-sm text-blue-600 hover:text-blue-800 font-medium"
              >
                + Add AI Player
              </button>
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

            {/* Task 6.3 step 2 — Boot Settings */}
            <div className="space-y-2">
              <p className="text-sm font-medium">Boot Settings</p>
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={bootSitout}
                  onChange={(e) => setBootSitout(e.target.checked)}
                  className="rounded"
                />
                Boot players who sit out
              </label>
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={bootDisconnect}
                  onChange={(e) => setBootDisconnect(e.target.checked)}
                  className="rounded"
                />
                Boot players who disconnect
              </label>
              {(bootSitout || bootDisconnect) && (
                <div className="ml-6">
                  <label className="block text-xs font-medium mb-1">After how many hands</label>
                  <input
                    type="number"
                    value={bootAfterHands}
                    min={1}
                    onChange={(e) => setBootAfterHands(Math.max(1, Number(e.target.value)))}
                    className="w-20 border border-gray-300 rounded px-2 py-1 text-xs"
                  />
                </div>
              )}
            </div>

            {/* Task 6.3 step 3 — Late Registration */}
            <div className="space-y-2">
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={lateRegistration}
                  onChange={(e) => setLateRegistration(e.target.checked)}
                  className="rounded"
                />
                Allow late registration
              </label>
              {lateRegistration && (
                <div className="ml-6">
                  <label className="block text-xs font-medium mb-1">Until level</label>
                  <input
                    type="number"
                    value={lateRegUntilLevel}
                    min={1}
                    onChange={(e) => setLateRegUntilLevel(Math.max(1, Number(e.target.value)))}
                    className="w-20 border border-gray-300 rounded px-2 py-1 text-xs"
                  />
                </div>
              )}
            </div>
          </div>
        </details>

        {/* Practice Options — visible only in practice mode */}
        {isPractice && (
          <section className="space-y-3">
            <h2 className="text-lg font-semibold border-b border-gray-200 pb-1">Practice Options</h2>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={aiFaceUp}
                onChange={(e) => setAiFaceUp(e.target.checked)}
                className="rounded"
              />
              <span>Show AI Cards <span className="text-gray-500">— Reveal AI hole cards face-up</span></span>
            </label>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={pauseAllin}
                onChange={(e) => setPauseAllin(e.target.checked)}
                className="rounded"
              />
              <span>Pause on All-In <span className="text-gray-500">— Pause before dealing all-in runout</span></span>
            </label>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={autoDeal}
                onChange={(e) => setAutoDeal(e.target.checked)}
                className="rounded"
              />
              <span>Auto Deal <span className="text-gray-500">— Automatically start next hand</span></span>
            </label>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={zipMode}
                onChange={(e) => setZipMode(e.target.checked)}
                className="rounded"
              />
              <span>Fast Mode <span className="text-gray-500">— Skip animations and delays</span></span>
            </label>
          </section>
        )}

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
              {loading ? 'Starting\u2026' : 'Start Practice Game'}
            </button>
          ) : (
            <>
              <button
                type="submit"
                disabled={loading}
                className="flex-1 py-2 bg-green-700 hover:bg-green-600 disabled:opacity-50 text-white font-bold rounded-lg transition-colors"
              >
                {loading ? 'Creating\u2026' : 'Create Game'}
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
    <Suspense fallback={<div className="text-gray-500 text-sm">Loading\u2026</div>}>
      <CreateGameForm />
    </Suspense>
  )
}
