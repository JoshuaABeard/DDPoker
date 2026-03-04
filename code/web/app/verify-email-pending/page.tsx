'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useState } from 'react'
import { useAuth } from '@/lib/auth/useAuth'
import { authApi } from '@/lib/api'

export default function VerifyEmailPendingPage() {
  const { user } = useAuth()
  const [status, setStatus] = useState<'idle' | 'sent' | 'error'>('idle')

  const resend = async () => {
    try {
      const res = await authApi.resendVerification()
      if (res.ok) {
        setStatus('sent')
      } else {
        setStatus('error')
      }
    } catch {
      setStatus('error')
    }
  }

  return (
    <main>
      <h1>Check your email</h1>
      <p>
        We sent a verification link to <strong>{user?.email}</strong>.
      </p>
      <p>Click the link in that email to activate your account. It expires in 7 days.</p>
      {status === 'sent' && <p>Email resent — check your inbox.</p>}
      {status === 'error' && <p>Could not resend — please try again in a few minutes.</p>}
      <button onClick={resend} disabled={status === 'sent'}>
        Resend verification email
      </button>
    </main>
  )
}
