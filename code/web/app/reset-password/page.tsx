'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { authApi } from '@/lib/api'

export default function ResetPasswordPage() {
  const params = useSearchParams()
  const router = useRouter()
  const token = params.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (password !== confirm) {
      setError('Passwords do not match.')
      return
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }
    try {
      const res = await authApi.resetPassword(token, password)
      if (res.ok) {
        setDone(true)
        setTimeout(() => router.replace('/login'), 2000)
      } else {
        const b = await res.json().catch(() => ({}))
        setError(b.message ?? 'Reset failed.')
      }
    } catch {
      setError('Reset failed. Please try again.')
    }
  }

  if (done) {
    return (
      <main>
        <p>Password reset. Redirecting to login…</p>
      </main>
    )
  }

  return (
    <main>
      <h1>Reset your password</h1>
      <form onSubmit={submit}>
        <input
          type="password"
          placeholder="New password (min 8 chars)"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Confirm password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          required
        />
        {error && <p>{error}</p>}
        <button type="submit">Reset password</button>
      </form>
    </main>
  )
}
