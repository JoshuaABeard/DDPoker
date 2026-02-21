/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Online Portal Home Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'
import Link from 'next/link'

export const metadata: Metadata = {
  title: 'Online Games Portal - DD Poker Community Edition',
  description: 'Browse games, view leaderboards, and tournament history',
}

export default function OnlinePortal() {
  const sections = [
    {
      title: 'Play',
      links: [
        { href: '/games', label: 'Game Lobby' },
      ],
    },
    {
      title: 'Rankings & Stats',
      links: [
        { href: '/online/leaderboard', label: 'Leaderboard', rss: '/api/rss/leaderboard' },
        { href: '/online/search', label: 'Player Search' },
      ],
    },
    {
      title: 'Information',
      links: [
        { href: '/online/hosts', label: 'Host List' },
      ],
    },
  ]

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-6">Online Games Portal</h1>

      <div className="space-y-6">
        {sections.map((section) => (
          <div key={section.title} className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-2xl font-bold mb-4">{section.title}</h2>
            <ul className="space-y-2">
              {section.links.map((link) => (
                <li key={link.href} className="flex items-center justify-between">
                  <Link
                    href={link.href}
                    className="text-blue-600 hover:underline text-lg"
                  >
                    {link.label}
                  </Link>
                  {link.rss && (
                    <a
                      href={link.rss}
                      className="text-orange-600 hover:underline text-sm"
                      title="RSS Feed"
                    >
                      RSS
                    </a>
                  )}
                </li>
              ))}
            </ul>
          </div>
        ))}

        <div className="bg-gray-100 rounded-lg p-6">
          <h2 className="text-xl font-bold mb-3">Need Help?</h2>
          <p className="text-gray-700">
            Visit our{' '}
            <Link href="/support" className="text-blue-600 hover:underline">
              Support
            </Link>{' '}
            page for assistance with online games, or check out the{' '}
            <Link href="/support/selfhelp" className="text-blue-600 hover:underline">
              Self-Help Guide
            </Link>
            .
          </p>
        </div>
      </div>
    </div>
  )
}
