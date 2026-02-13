'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Admin Section Layout
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 *
 * SECURITY NOTE: This layout provides client-side admin protection only.
 *
 * Due to localStorage-based authentication, this cannot be a server component
 * with true server-side protection. The admin check happens AFTER page HTML
 * is delivered to the client.
 *
 * CRITICAL: Backend Java servlets MUST independently verify admin authorization.
 * This frontend check is for UX only - NOT a security boundary.
 *
 * See: .claude/docs/ADMIN-AUTH-ARCHITECTURE.md for details and recommendations.
 */

import { useRequireAuth } from '@/lib/auth/useRequireAuth'
import { useRouter } from 'next/navigation'
import { useEffect } from 'react'
import Sidebar from '@/components/layout/Sidebar'
import { adminSidebarData } from '@/lib/sidebarData'

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useRequireAuth()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading && (!user || !user.isAdmin)) {
      router.push('/login')
    }
  }, [user, isLoading, router])

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8 text-center">
        <div className="text-xl">Loading...</div>
      </div>
    )
  }

  if (!user || !user.isAdmin) {
    return null // Will redirect via useEffect
  }

  return (
    <div className="admin-layout">
      <Sidebar sections={adminSidebarData} />
      <div className="admin-content">{children}</div>

      <style jsx>{`
        .admin-layout {
          display: flex;
          min-height: calc(100vh - 60px);
          background: #f5f5f5;
        }

        .admin-content {
          flex: 1;
          padding: 2rem;
          max-width: 1400px;
          margin: 0 auto;
          width: 100%;
        }

        @media (max-width: 768px) {
          .admin-content {
            padding: 1rem;
          }
        }
      `}</style>
    </div>
  )
}
