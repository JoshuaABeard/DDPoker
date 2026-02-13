/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { Metadata } from 'next'
import { Delius } from 'next/font/google'
import Navigation from '@/components/layout/Navigation'
import Footer from '@/components/layout/Footer'
import { AuthProvider } from '@/lib/auth/AuthContext'
import './globals.css'

const delius = Delius({ weight: '400', subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'DD Poker Community Edition - Free Texas Hold\'em Simulator',
  description:
    'Free, open-source Texas Hold\'em poker game featuring no-limit, pot-limit, and limit variants. Practice against advanced AI opponents, host online games, and master tournament play.',
  keywords: [
    'poker',
    'texas holdem',
    'tournament',
    'AI',
    'poker simulator',
    'free poker game',
    'poker training',
  ],
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" className={delius.className}>
      <body>
        <AuthProvider>
          <Navigation />
          <main id="content">{children}</main>
          <Footer />
        </AuthProvider>
      </body>
    </html>
  )
}
