'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useState } from 'react'
import { adminApi } from '@/lib/api'

interface ProfileActionsCellProps {
  id: number
  emailVerified: boolean
  lockedUntil: number | null | undefined
}

export function ProfileActionsCell({ id, emailVerified, lockedUntil }: ProfileActionsCellProps) {
  const [message, setMessage] = useState<{ text: string; isError: boolean } | null>(null)
  const [loading, setLoading] = useState<string | null>(null)

  const isLocked = lockedUntil != null && lockedUntil > Date.now()

  async function handleAction(action: string, fn: () => Promise<void>, successMsg: string) {
    setLoading(action)
    setMessage(null)
    try {
      await fn()
      setMessage({ text: successMsg, isError: false })
    } catch {
      setMessage({ text: 'Action failed. Please try again.', isError: true })
    } finally {
      setLoading(null)
    }
  }

  return (
    <div className="flex flex-col gap-1 items-start">
      {message && (
        <p
          className={`text-xs ${message.isError ? 'text-red-600' : 'text-green-700'}`}
          role="status"
        >
          {message.text}
        </p>
      )}
      <div className="flex flex-wrap gap-1">
        {!emailVerified && (
          <button
            onClick={() =>
              handleAction('verify', () => adminApi.verifyProfile(id), 'Verified successfully.')
            }
            disabled={loading !== null}
            className="px-2 py-1 text-xs bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 transition-colors"
          >
            {loading === 'verify' ? 'Verifying...' : 'Verify'}
          </button>
        )}
        {isLocked && (
          <button
            onClick={() =>
              handleAction('unlock', () => adminApi.unlockProfile(id), 'Account unlocked.')
            }
            disabled={loading !== null}
            className="px-2 py-1 text-xs bg-yellow-600 text-white rounded hover:bg-yellow-700 disabled:opacity-50 transition-colors"
          >
            {loading === 'unlock' ? 'Unlocking...' : 'Unlock'}
          </button>
        )}
        {!emailVerified && (
          <button
            onClick={() =>
              handleAction(
                'resend',
                () => adminApi.resendVerification(id),
                'Verification email sent.'
              )
            }
            disabled={loading !== null}
            className="px-2 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            {loading === 'resend' ? 'Sending...' : 'Resend Verification'}
          </button>
        )}
      </div>
    </div>
  )
}
