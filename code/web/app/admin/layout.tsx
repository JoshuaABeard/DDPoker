'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Admin Section Layout
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useRequireAuth } from '@/lib/auth/useRequireAuth'
import { useRouter } from 'next/navigation'
import { useEffect } from 'react'
import Link from 'next/link'

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
    <div className="flex min-h-screen">
      {/* Admin Navigation Sidebar */}
      <aside className="w-64 bg-gray-800 text-white p-6">
        <h2 className="text-2xl font-bold mb-6">Admin</h2>
        <nav className="space-y-2">
          <Link
            href="/admin"
            className="block px-4 py-2 rounded hover:bg-gray-700 transition-colors"
          >
            Dashboard
          </Link>
          <Link
            href="/admin/online-profile-search"
            className="block px-4 py-2 rounded hover:bg-gray-700 transition-colors"
          >
            Profile Search
          </Link>
          <Link
            href="/admin/reg-search"
            className="block px-4 py-2 rounded hover:bg-gray-700 transition-colors"
          >
            Registration Search
          </Link>
          <Link
            href="/admin/ban-list"
            className="block px-4 py-2 rounded hover:bg-gray-700 transition-colors"
          >
            Ban List
          </Link>
          <hr className="my-4 border-gray-600" />
          <Link
            href="/online"
            className="block px-4 py-2 rounded hover:bg-gray-700 transition-colors"
          >
            ← Back to Portal
          </Link>
        </nav>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 bg-gray-50">{children}</main>
    </div>
  )
}
