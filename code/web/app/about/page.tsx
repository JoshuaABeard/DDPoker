/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - About Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'About - DD Poker Community Edition',
  description: 'Learn about DD Poker Community Edition - origins, features, and community contributions',
}

export default function About() {
  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-4">About DD Poker Community Edition</h1>

      <p className="mb-6 leading-relaxed">
        DD Poker Community Edition is a free, open-source Texas Hold'em simulator maintained by the community.
        This page explains the project's origins, what makes the community edition special, and how you can
        get involved.
      </p>

      <h2 className="text-2xl font-bold mt-8 mb-4">Community Edition</h2>

      <p className="mb-4 leading-relaxed">
        The Community Edition builds upon the original DD Poker foundation with modern enhancements:
      </p>

      <ul className="list-disc list-inside space-y-2 mb-6 ml-4">
        <li>Removed licensing restrictions - fully free to use and modify</li>
        <li>File-based configuration system for easier customization</li>
        <li>First-time user experience wizard to streamline initial setup</li>
        <li>Docker deployment support for self-hosted online game portals</li>
        <li>Compatibility with modern Java versions (Java 25+)</li>
        <li>Ongoing bug fixes and improvements from community contributors</li>
        <li>Active development with transparent issue tracking and roadmap</li>
      </ul>

      <h2 className="text-2xl font-bold mt-8 mb-4">Key Features</h2>

      <div className="space-y-6 mt-6">
        <div>
          <h3 className="text-xl font-bold mb-2">
            <Link href="/about/practice" className="text-[var(--color-poker-green)] hover:underline">
              Practice Mode
            </Link>
          </h3>
          <p className="leading-relaxed">
            Hone your skills across limit, pot-limit, and no-limit Texas Hold'em formats. Face AI opponents
            modeled after realistic playing styles including aggressive, conservative, and unpredictable
            patterns. Build custom opponent profiles and design your ideal practice environment with
            configurable tournament structures.
          </p>
        </div>

        <div>
          <h3 className="text-xl font-bold mb-2">
            <Link href="/about/online" className="text-[var(--color-poker-green)] hover:underline">
              Online Play
            </Link>
          </h3>
          <p className="leading-relaxed">
            Host private poker games with friends using peer-to-peer connections. No central server required,
            making it perfect for LAN play or Internet games. Supports self-hosted portals via Docker for
            managing multiple games, player profiles, and tournament histories. Play real poker without rake
            or fees.
          </p>
        </div>

        <div>
          <h3 className="text-xl font-bold mb-2">
            <Link href="/about/analysis" className="text-[var(--color-poker-green)] hover:underline">
              Hand Analysis
            </Link>
          </h3>
          <p className="leading-relaxed">
            Track detailed statistics for every hand played in practice or online modes. The built-in calculator
            evaluates hand strength, computes showdown probabilities, and identifies your outs in any situation.
            Review hand histories to understand your playing patterns and identify areas for improvement.
          </p>
        </div>

        <div>
          <h3 className="text-xl font-bold mb-2">
            <Link href="/about/pokerclock" className="text-[var(--color-poker-green)] hover:underline">
              Tournament Clock
            </Link>
          </h3>
          <p className="leading-relaxed">
            Organize and manage live home tournaments with a professional-grade poker clock. Configure blind
            levels, break schedules, buy-ins, rebuys, and add-ons. The clock provides visual and audio alerts
            for blind increases and break periods, keeping your tournament running smoothly just like the pros.
          </p>
        </div>

        <div>
          <h3 className="text-xl font-bold mb-2">Documentation</h3>
          <p className="leading-relaxed">
            Comprehensive guides and documentation are available covering installation, configuration,
            hosting online games, and advanced features. Visit the{' '}
            <Link href="/support" className="text-[var(--color-poker-green)] hover:underline">
              Support
            </Link>{' '}
            section for troubleshooting guides and help resources.
          </p>
        </div>
      </div>

      <h2 className="text-2xl font-bold mt-8 mb-4">Origins</h2>

      <p className="mb-4 leading-relaxed">
        DD Poker was originally created by <strong>Doug Donohoe</strong> and first released in June 2004 as one
        of the pioneering poker software products on the market. Over the years, DD Poker evolved through multiple
        major versions:
      </p>

      <ul className="list-disc list-inside space-y-2 mb-6 ml-4">
        <li><strong>DD Poker 1.0</strong> (2004) - Initial release with practice play and AI opponents</li>
        <li><strong>DD Poker 2.0</strong> (2005) - Added online play and sophisticated odds calculator</li>
        <li><strong>DD Poker 2.5</strong> (2006) - Major improvements and refinements</li>
        <li><strong>DD Poker 3.0</strong> (2009) - Released as donation-ware with online portal</li>
        <li>
          <strong>Open Source Release</strong> (August 2024) - Doug generously open-sourced the{' '}
          <a
            href="https://github.com/dougdonohoe/ddpoker"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[var(--color-poker-green)] hover:underline"
          >
            entire codebase on GitHub
          </a>
        </li>
      </ul>

      <p className="mb-6 leading-relaxed">
        The trademark and branding remain with Donohoe Digital LLC. For more about the original project,
        visit{' '}
        <a
          href="https://www.ddpoker.com"
          target="_blank"
          rel="noopener noreferrer"
          className="text-[var(--color-poker-green)] hover:underline"
        >
          Doug Donohoe's website
        </a>.
      </p>

      <h2 className="text-2xl font-bold mt-8 mb-4">Special Thanks</h2>

      <p className="mb-6 leading-relaxed">
        We extend our deepest gratitude to <strong>Doug Donohoe</strong> for creating DD Poker and for making the
        decision to open-source the project in 2024. This generous act has enabled the community to preserve,
        maintain, and enhance this excellent poker software for years to come. Doug's vision and craftsmanship
        built the foundation upon which this community edition stands.
      </p>

      <p className="mt-8 leading-relaxed">
        Explore the feature links above for detailed information, browse the{' '}
        <Link href="/about/faq" className="text-[var(--color-poker-green)] hover:underline">
          FAQ
        </Link>, view{' '}
        <Link href="/about/screenshots" className="text-[var(--color-poker-green)] hover:underline">
          screenshots
        </Link>, or{' '}
        <Link href="/download" className="text-[var(--color-poker-green)] hover:underline">
          download DD Poker Community Edition
        </Link>{' '}
        to get started.
      </p>
    </div>
  )
}
