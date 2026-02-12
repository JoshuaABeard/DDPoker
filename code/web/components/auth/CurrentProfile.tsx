'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Current Profile Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useRouter } from 'next/navigation'
import { useAuth } from '@/lib/auth/useAuth'

export function CurrentProfile() {
  const { user, isAuthenticated, logout } = useAuth()
  const router = useRouter()

  if (!isAuthenticated || !user) {
    return null
  }

  const handleLogout = async () => {
    await logout()
    router.push('/')
  }

  return (
    <div className="flex items-center gap-4">
      <div className="text-right">
        <div className="font-medium">{user.username}</div>
        {user.isAdmin && (
          <div className="text-xs bg-yellow-400 text-black px-2 py-0.5 rounded inline-block">
            Admin
          </div>
        )}
      </div>
      <button
        onClick={handleLogout}
        className="px-3 py-1 bg-red-600 text-white rounded hover:bg-red-700 transition-colors text-sm"
      >
        Logout
      </button>
    </div>
  )
}
