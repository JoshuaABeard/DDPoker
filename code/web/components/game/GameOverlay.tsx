/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

interface RebuyProps {
  type: 'rebuy'
  cost: number
  chips: number
  timeoutSeconds: number
  onDecision: (accept: boolean) => void
}

interface AddonProps {
  type: 'addon'
  cost: number
  chips: number
  timeoutSeconds: number
  onDecision: (accept: boolean) => void
}

interface PausedProps {
  type: 'paused'
  reason: string
  pausedBy: string
}

interface EliminatedProps {
  type: 'eliminated'
  finishPosition: number
  onClose: () => void
}

interface TabReplaced {
  type: 'tab-replaced'
}

type OverlayProps = RebuyProps | AddonProps | PausedProps | EliminatedProps | TabReplaced

function formatChips(n: number): string {
  return new Intl.NumberFormat('en-US').format(n)
}

/**
 * Modal overlays for game events: rebuy, addon, pause, elimination, tab-replaced.
 *
 * XSS safety: all user strings rendered as text nodes.
 */
export function GameOverlay(props: OverlayProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-60">
      <div className="bg-gray-800 rounded-2xl shadow-2xl p-6 max-w-sm w-full mx-4 text-white text-center">
        {props.type === 'rebuy' && (
          <>
            <h2 className="text-xl font-bold mb-2">Rebuy Available</h2>
            <p className="text-gray-300 mb-4">
              Add {formatChips(props.chips)} chips for {formatChips(props.cost)}?
            </p>
            <div className="flex gap-3 justify-center">
              <button
                type="button"
                onClick={() => props.onDecision(true)}
                className="px-5 py-2 bg-green-600 hover:bg-green-500 rounded-lg font-bold transition-colors"
              >
                Accept
              </button>
              <button
                type="button"
                onClick={() => props.onDecision(false)}
                className="px-5 py-2 bg-gray-600 hover:bg-gray-500 rounded-lg transition-colors"
              >
                Decline
              </button>
            </div>
          </>
        )}

        {props.type === 'addon' && (
          <>
            <h2 className="text-xl font-bold mb-2">Add-On Available</h2>
            <p className="text-gray-300 mb-4">
              Add {formatChips(props.chips)} chips for {formatChips(props.cost)}?
            </p>
            <div className="flex gap-3 justify-center">
              <button
                type="button"
                onClick={() => props.onDecision(true)}
                className="px-5 py-2 bg-green-600 hover:bg-green-500 rounded-lg font-bold transition-colors"
              >
                Accept
              </button>
              <button
                type="button"
                onClick={() => props.onDecision(false)}
                className="px-5 py-2 bg-gray-600 hover:bg-gray-500 rounded-lg transition-colors"
              >
                Decline
              </button>
            </div>
          </>
        )}

        {props.type === 'paused' && (
          <>
            <h2 className="text-xl font-bold mb-2">Game Paused</h2>
            <p className="text-gray-300 mb-1">{props.reason}</p>
            <p className="text-gray-400 text-sm">Paused by {props.pausedBy}</p>
          </>
        )}

        {props.type === 'eliminated' && (
          <>
            <h2 className="text-xl font-bold mb-2">You Have Been Eliminated</h2>
            <p className="text-gray-300 mb-4">
              You finished in position {props.finishPosition}.
            </p>
            <button
              type="button"
              onClick={props.onClose}
              className="px-5 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg font-bold transition-colors"
            >
              Continue Watching
            </button>
          </>
        )}

        {props.type === 'tab-replaced' && (
          <>
            <h2 className="text-xl font-bold mb-2">Game Opened in Another Tab</h2>
            <p className="text-gray-300">
              This game session was taken over by another browser tab. Close this tab and continue
              there, or reload to rejoin.
            </p>
          </>
        )}
      </div>
    </div>
  )
}
