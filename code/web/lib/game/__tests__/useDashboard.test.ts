/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useDashboard, DEFAULT_WIDGETS } from '../useDashboard'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

describe('useDashboard', () => {
  it('returns default widgets on first load', () => {
    const { result } = renderHook(() => useDashboard())
    expect(result.current.widgets).toEqual(DEFAULT_WIDGETS)
  })

  it('toggles widget visibility', () => {
    const { result } = renderHook(() => useDashboard())
    act(() => result.current.toggleWidget('pot-odds'))
    expect(result.current.widgets.find((w) => w.id === 'pot-odds')?.visible).toBe(false)
    act(() => result.current.toggleWidget('pot-odds'))
    expect(result.current.widgets.find((w) => w.id === 'pot-odds')?.visible).toBe(true)
  })

  it('moves widget up', () => {
    const { result } = renderHook(() => useDashboard())
    // 'pot-odds' is at index 1 — move it up to index 0
    act(() => result.current.moveWidget('pot-odds', 'up'))
    expect(result.current.widgets[0].id).toBe('pot-odds')
    expect(result.current.widgets[1].id).toBe('hand-strength')
  })

  it('moves widget down', () => {
    const { result } = renderHook(() => useDashboard())
    // 'hand-strength' is at index 0 — move it down to index 1
    act(() => result.current.moveWidget('hand-strength', 'down'))
    expect(result.current.widgets[0].id).toBe('pot-odds')
    expect(result.current.widgets[1].id).toBe('hand-strength')
  })

  it('does not move first widget up', () => {
    const { result } = renderHook(() => useDashboard())
    act(() => result.current.moveWidget('hand-strength', 'up'))
    expect(result.current.widgets[0].id).toBe('hand-strength')
  })

  it('does not move last widget down', () => {
    const { result } = renderHook(() => useDashboard())
    const lastId = DEFAULT_WIDGETS[DEFAULT_WIDGETS.length - 1].id
    act(() => result.current.moveWidget(lastId, 'down'))
    expect(result.current.widgets[result.current.widgets.length - 1].id).toBe(lastId)
  })

  it('persists to localStorage', () => {
    const { result } = renderHook(() => useDashboard())
    act(() => result.current.toggleWidget('rank'))
    const stored = JSON.parse(store['ddpoker-dashboard'])
    expect(stored.find((w: { id: string }) => w.id === 'rank').visible).toBe(false)
  })

  it('resets to defaults', () => {
    const { result } = renderHook(() => useDashboard())
    act(() => result.current.toggleWidget('rank'))
    act(() => result.current.moveWidget('pot-odds', 'up'))
    act(() => result.current.resetToDefaults())
    expect(result.current.widgets).toEqual(DEFAULT_WIDGETS)
  })

  it('loads stored config', () => {
    const custom = [
      { id: 'pot-odds', label: 'Pot Odds', visible: false },
      { id: 'hand-strength', label: 'Hand Strength', visible: true },
      { id: 'tournament-info', label: 'Tournament Info', visible: true },
      { id: 'rank', label: 'Rank', visible: true },
      { id: 'starting-hand', label: 'Starting Hand', visible: true },
    ]
    store['ddpoker-dashboard'] = JSON.stringify(custom)
    const { result } = renderHook(() => useDashboard())
    expect(result.current.widgets[0].id).toBe('pot-odds')
    expect(result.current.widgets[0].visible).toBe(false)
  })

  it('ignores invalid stored data', () => {
    store['ddpoker-dashboard'] = 'not-json'
    const { result } = renderHook(() => useDashboard())
    expect(result.current.widgets).toEqual(DEFAULT_WIDGETS)
  })
})
