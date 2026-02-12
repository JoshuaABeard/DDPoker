/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 *
 * Copyright footer component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

export default function Footer() {
  return (
    <footer className="mt-12 py-8 px-4 border-t border-[var(--color-gray-light)] bg-[var(--bg-light-gray)]">
      <div className="text-center text-[var(--color-gray-dark)] text-sm leading-relaxed">
        <p className="my-1">DD Poker Community Edition</p>
        <p className="my-1">
          Originally created by{' '}
          <a
            href="https://www.ddpoker.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[var(--color-poker-green)] no-underline hover:underline"
          >
            Doug Donohoe
          </a>
        </p>
        <p className="my-1">DD Poker™ is a trademark of Donohoe Digital LLC</p>
      </div>
    </footer>
  )
}
