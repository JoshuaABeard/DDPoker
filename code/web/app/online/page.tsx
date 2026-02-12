/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Online Portal Page (Placeholder)
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Online Games Portal - DD Poker Community Edition',
  description: 'View leaderboards, current games, and tournament history',
}

export default function OnlinePortal() {
  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-4">Online Games Portal</h1>

      <div className="p-8 bg-blue-50 border-2 border-blue-500 rounded-lg text-center">
        <h2 className="text-2xl font-bold mb-4 text-blue-800">Coming Soon in Phase 3</h2>
        <p className="leading-relaxed">
          The Online Games Portal will provide access to leaderboards, current games, completed games,
          tournament history, and player profiles. This feature requires authentication and will be
          implemented in Phase 3 of the website modernization project.
        </p>
      </div>

      <div className="mt-8 space-y-4">
        <h2 className="text-2xl font-bold mb-4">Planned Features:</h2>
        <ul className="list-disc list-inside space-y-2 ml-4">
          <li>Leaderboard - View top players and rankings</li>
          <li>Current Games - See games currently in progress</li>
          <li>Completed Games - Browse recently finished tournaments</li>
          <li>Tournament History - Review past tournament results</li>
          <li>Search - Find specific players or games</li>
          <li>Host Information - View game host details</li>
          <li>My Profile - Manage your player profile and settings</li>
        </ul>
      </div>
    </div>
  )
}
