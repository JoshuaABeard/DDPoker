/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Admin Dashboard
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import Link from 'next/link'

export const metadata: Metadata = {
  title: 'Admin - DD Poker',
  description: 'Administration tools',
}

export default function AdminPage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Administration</h1>

      <p className="mb-8 text-gray-700">
        Welcome to the DD Poker administration panel. Select a tool below to manage the system.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <Link href="/admin/online-profile-search">
          <div className="p-6 bg-white rounded-lg shadow hover:shadow-lg transition-shadow cursor-pointer">
            <h2 className="text-xl font-bold mb-2">Online Profile Search</h2>
            <p className="text-gray-600">Search and view online player profiles by name or email.</p>
          </div>
        </Link>

        <Link href="/admin/reg-search">
          <div className="p-6 bg-white rounded-lg shadow hover:shadow-lg transition-shadow cursor-pointer">
            <h2 className="text-xl font-bold mb-2">Registration Search</h2>
            <p className="text-gray-600">Search player registrations with date range filters.</p>
          </div>
        </Link>

        <Link href="/admin/ban-list">
          <div className="p-6 bg-white rounded-lg shadow hover:shadow-lg transition-shadow cursor-pointer">
            <h2 className="text-xl font-bold mb-2">Ban List</h2>
            <p className="text-gray-600">View and manage banned keys and players.</p>
          </div>
        </Link>
      </div>
    </div>
  )
}
