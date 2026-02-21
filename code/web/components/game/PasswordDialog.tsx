/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useRef, useState } from 'react'

interface PasswordDialogProps {
  gameName: string
  onSubmit: (password: string) => void
  onCancel: () => void
  error?: string
}

/**
 * Modal dialog for entering a private game password.
 *
 * Security notes:
 * - Input state is cleared on unmount (useEffect cleanup).
 * - Password is submitted in the POST body only — never put it in a URL or query param.
 * - XSS safe: gameName rendered as a text node.
 */
export function PasswordDialog({ gameName, onSubmit, onCancel, error }: PasswordDialogProps) {
  const [password, setPassword] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  // Auto-focus input when dialog opens
  useEffect(() => {
    inputRef.current?.focus()
  }, [])

  // Clear password from memory on unmount
  useEffect(() => {
    return () => setPassword('')
  }, [])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!password) return
    onSubmit(password)
  }

  // Close on Escape key
  useEffect(() => {
    function handler(e: KeyboardEvent) {
      if (e.key === 'Escape') onCancel()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onCancel])

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-60"
      role="dialog"
      aria-modal="true"
      aria-label="Enter game password"
    >
      <div className="bg-white rounded-xl shadow-2xl p-6 max-w-sm w-full mx-4">
        <h2 className="text-xl font-bold mb-1">Private Game</h2>
        {/* gameName is a text node — XSS safe */}
        <p className="text-gray-600 text-sm mb-4">
          Enter the password to join <span className="font-semibold">{gameName}</span>.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="game-password" className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            <input
              ref={inputRef}
              id="game-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              autoComplete="off"
              maxLength={100}
            />
          </div>

          {error && (
            <p className="text-red-600 text-sm" role="alert">
              {error}
            </p>
          )}

          <div className="flex gap-3 justify-end">
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 text-sm text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!password}
              className="px-4 py-2 text-sm text-white bg-blue-600 hover:bg-blue-500 disabled:opacity-50 rounded-lg font-semibold transition-colors"
            >
              Join
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
