/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 *
 * Home page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'
import Footer from '@/components/layout/Footer'

export default function Home() {
  return (
    <>
      <div className="container mx-auto px-4 py-8 max-w-4xl">
      <div id="homepage">
        <h1 className="text-4xl font-bold text-center mb-8 text-[var(--color-poker-green)]">
          DD Poker Community Edition
        </h1>

        <div className="space-y-6">
          <div className="text-lg leading-relaxed">
            DD Poker Community Edition is a free, open-source Texas Hold'em simulator featuring no-limit,
            pot-limit, and limit variants. Practice against advanced AI opponents, host online games with
            friends, and master tournament play with comprehensive analysis tools.
          </div>

          <div className="leading-relaxed">
            <strong className="text-xl block mb-2">Core Features</strong>
            <ul className="list-disc list-inside space-y-1 ml-4">
              <li>Advanced AI opponents with configurable difficulty levels</li>
              <li>Online multiplayer support for private games</li>
              <li>Hand analysis calculator with odds computation</li>
              <li>Tournament clock for managing live home games</li>
              <li>Custom tournament designer with flexible blind structures</li>
              <li>Comprehensive player statistics and hand history</li>
              <li>
                <Link href="/about" className="text-[var(--color-poker-green)] hover:underline">
                  Learn more about features
                </Link>
              </li>
            </ul>
          </div>

          <div className="leading-relaxed">
            <strong className="text-xl block mb-2">What's New in Community Edition</strong>
            <ul className="list-disc list-inside space-y-1 ml-4">
              <li>License-free operation - no registration required</li>
              <li>File-based configuration for easy customization</li>
              <li>First-time user experience wizard for streamlined setup</li>
              <li>
                Docker deployment support for self-hosted online portals (
                <Link href="/support" className="text-[var(--color-poker-green)] hover:underline">
                  setup guide
                </Link>
                )
              </li>
              <li>Modern Java 25+ compatibility with ongoing updates</li>
            </ul>
          </div>

          <div className="leading-relaxed">
            Originally created by{' '}
            <a
              href="https://www.ddpoker.com"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[var(--color-poker-green)] hover:underline"
            >
              Doug Donohoe
            </a>{' '}
            and released as open source in 2024, DD Poker has been maintained and enhanced by the community.
          </div>

          <div className="text-center mt-8">
            <Link href="/download">
              <button className="text-xl px-6 py-3 bg-[var(--color-poker-green)] text-white border-none rounded cursor-pointer font-bold transition-colors hover:bg-[var(--color-poker-green-dark)]">
                Download DD Poker Community Edition
              </button>
            </Link>
          </div>
        </div>
      </div>
      </div>
      <Footer />
    </>
  )
}
