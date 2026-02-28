/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

/**
 * Maps logical sound event names to audio file base names.
 * When a name maps to multiple files, playback picks one at random.
 */
export const soundMap: Record<string, string[]> = {
  bet: ['bet1', 'bet2', 'bet3', 'bet4', 'bet5', 'bet6', 'bet7', 'bet8', 'bet9', 'bet10'],
  check: ['check'],
  raise: ['raise2'],
  shuffle: ['shuffle1', 'shuffle2', 'shuffle3'],
  shuffleShort: ['shuffle1_short', 'shuffle2_short', 'shuffle3_short'],
  cheers: ['cheers1', 'cheers2', 'cheers3', 'cheers4'],
  bell: ['bell', 'bell2'],
  attention: ['attention'],
  click: ['button-click'],
  camera: ['camera'],
}

/** Flat array of all unique audio file names (used for pre-loading). */
export const ALL_SOUND_FILES: string[] = [...new Set(Object.values(soundMap).flat())]
