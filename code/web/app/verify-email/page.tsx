'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { authApi } from '@/lib/api'

export default function VerifyEmailPage() {
  const params = useSearchParams()
  const router = useRouter()
  const token = params.get('token')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) {
      setError('Missing token.')
      return
    }
    authApi
      .verifyEmail(token)
      .then(async (res) => {
        if (res.ok) {
          router.replace('/games')
        } else {
          const body = await res.json().catch(() => ({}))
          setError(body.message ?? 'Invalid or expired link.')
        }
      })
      .catch(() => {
        setError('Verification failed. Please try again.')
      })
  }, [token, router])

  if (error) {
    return (
      <main>
        <h1>Verification failed</h1>
        <p>{error}</p>
        <a href="/verify-email-pending">Request a new link</a>
      </main>
    )
  }

  return (
    <main>
      <p>Verifying…</p>
    </main>
  )
}
