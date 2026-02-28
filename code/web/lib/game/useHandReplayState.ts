/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { HandHistoryEntry } from './gameReducer'

export interface HandReplayState {
  currentStep: number
  totalSteps: number
  isPlaying: boolean
  speed: number
  visibleEntries: HandHistoryEntry[]
  communityCards: string[]
  next: () => void
  prev: () => void
  play: () => void
  pause: () => void
  setSpeed: (multiplier: number) => void
}

export function useHandReplayState(entries: HandHistoryEntry[]): HandReplayState {
  const [currentStep, setCurrentStep] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [speed, setSpeed] = useState(1)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const totalSteps = entries.length

  const next = useCallback(() => {
    setCurrentStep((prev) => Math.min(prev + 1, totalSteps - 1))
  }, [totalSteps])

  const prev = useCallback(() => {
    setCurrentStep((prev) => Math.max(prev - 1, 0))
  }, [])

  const pause = useCallback(() => {
    setIsPlaying(false)
  }, [])

  const play = useCallback(() => {
    setIsPlaying(true)
  }, [])

  // Auto-play effect
  useEffect(() => {
    if (!isPlaying) {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
      return
    }

    intervalRef.current = setInterval(() => {
      setCurrentStep((prev) => {
        const nextStep = prev + 1
        if (nextStep >= totalSteps - 1) {
          setIsPlaying(false)
        }
        return Math.min(nextStep, totalSteps - 1)
      })
    }, 1000 / speed)

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
    }
  }, [isPlaying, speed, totalSteps])

  const visibleEntries = useMemo(
    () => entries.slice(0, currentStep + 1),
    [entries, currentStep],
  )

  const communityCards = useMemo(() => {
    for (let i = visibleEntries.length - 1; i >= 0; i--) {
      if (visibleEntries[i].type === 'community' && visibleEntries[i].cards) {
        return visibleEntries[i].cards!
      }
    }
    return []
  }, [visibleEntries])

  return {
    currentStep,
    totalSteps,
    isPlaying,
    speed,
    visibleEntries,
    communityCards,
    next,
    prev,
    play,
    pause,
    setSpeed,
  }
}
