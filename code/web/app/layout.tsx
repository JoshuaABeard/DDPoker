/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { Metadata } from 'next'
import Navigation from '@/components/layout/Navigation'
import Footer from '@/components/layout/Footer'
import { AuthProvider } from '@/lib/auth/AuthContext'
import './globals.css'

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
    <html lang="en">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Delius&display=swap"
          rel="stylesheet"
        />
      </head>
      <body>
        <AuthProvider>
          <Navigation />
          <main id="content">{children}</main>
        </AuthProvider>
      </body>
    </html>
  )
}
