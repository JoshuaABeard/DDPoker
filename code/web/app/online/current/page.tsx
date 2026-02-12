/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Current Games Page (Placeholder)
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Current Games - DD Poker Community Edition',
  description: 'View games currently in progress',
}

export default function CurrentGames() {
  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-4">Current Games</h1>

      <div className="p-8 bg-blue-50 border-2 border-blue-500 rounded-lg text-center">
        <h2 className="text-2xl font-bold mb-4 text-blue-800">Coming Soon in Phase 3</h2>
        <p className="leading-relaxed mb-4">
          This page will display all poker games currently in progress, including player counts,
          blind levels, and game status. Authentication is required to access this feature.
        </p>
        <Link href="/online" className="text-[var(--color-poker-green)] hover:underline font-bold">
          ← Back to Online Portal
        </Link>
      </div>
    </div>
  )
}
