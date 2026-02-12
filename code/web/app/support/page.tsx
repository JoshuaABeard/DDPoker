/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Support Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Support - DD Poker Community Edition',
  description: 'Get help and support for DD Poker Community Edition',
}

export default function Support() {
  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-4">DD Poker Community Edition Support</h1>

      <p className="mb-6 leading-relaxed">
        We want your experience with DD Poker Community Edition to be excellent. If you encounter any issues
        or have questions, there are several resources available to help:
      </p>

      <ul className="list-disc list-inside space-y-4 mb-6 ml-4">
        <li>
          <strong>Common Solutions:</strong> Check out our{' '}
          <Link href="/support/selfhelp" className="text-[var(--color-poker-green)] hover:underline">
            self-help page
          </Link>{' '}
          which addresses frequently encountered problems and their solutions.
        </li>
        <li>
          <strong>Login & Password Issues:</strong> Visit our{' '}
          <Link href="/support/passwords" className="text-[var(--color-poker-green)] hover:underline">
            password help page
          </Link>{' '}
          for assistance with account access.
        </li>

        <li>
          <strong>Online Game Troubleshooting:</strong> Review the{' '}
          <Link href="/support/selfhelp" className="text-[var(--color-poker-green)] hover:underline">
            self-help page
          </Link>{' '}
          for help with networking and multiplayer connectivity.
        </li>

        <li>
          <strong>Self-Hosting with Docker:</strong> Pre-built images are available on{' '}
          <a
            href="https://hub.docker.com/r/joshuaabeard/ddpoker"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[var(--color-poker-green)] hover:underline"
          >
            Docker Hub
          </a>
          . For detailed setup instructions, see our{' '}
          <a
            href="https://github.com/JoshuaABeard/DDPoker/blob/main/docker/DEPLOYMENT.md"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[var(--color-poker-green)] hover:underline"
          >
            deployment guide
          </a>{' '}
          on GitHub.
        </li>

        <li>
          <strong>Community Support:</strong> Join the discussion and get help from the community on{' '}
          <a
            href="https://github.com/JoshuaABeard/DDPoker/discussions"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[var(--color-poker-green)] hover:underline"
          >
            GitHub Discussions
          </a>
          .
        </li>

        <li>
          <strong>Bug Reports:</strong> Found a bug? Please report it on our{' '}
          <a
            href="https://github.com/JoshuaABeard/DDPoker/issues"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[var(--color-poker-green)] hover:underline"
          >
            GitHub Issues
          </a>{' '}
          page.
        </li>
      </ul>
    </div>
  )
}
