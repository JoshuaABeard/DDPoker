'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - useRequireAuth Hook
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useRouter, usePathname } from 'next/navigation'
import { useEffect } from 'react'
import { useAuth } from './useAuth'

interface UseRequireAuthOptions {
  requireAdmin?: boolean
}

/**
 * Hook for protected routes - redirects unauthenticated users to /login
 */
export function useRequireAuth(options: UseRequireAuthOptions = {}) {
  const { requireAdmin = false } = options
  const { user, isAuthenticated, isLoading } = useAuth()
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    if (isLoading) return

    if (!isAuthenticated) {
      // Redirect to login with return URL
      const returnUrl = encodeURIComponent(pathname)
      router.push(`/login?returnUrl=${returnUrl}`)
      return
    }

    if (requireAdmin && !user?.isAdmin) {
      // Redirect non-admin users to home
      router.push('/')
    }
  }, [isAuthenticated, isLoading, user, requireAdmin, router, pathname])

  return { user, isLoading, isAuthenticated }
}
