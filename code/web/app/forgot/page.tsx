/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Forgot Password Page (Placeholder)
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Forgot Password - DD Poker Community Edition',
  description: 'Reset your DD Poker password',
}

export default function ForgotPassword() {
  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-4">Forgot Password</h1>

      <div className="p-8 bg-blue-50 border-2 border-blue-500 rounded-lg text-center">
        <h2 className="text-2xl font-bold mb-4 text-blue-800">Coming Soon in Phase 3</h2>
        <p className="leading-relaxed mb-4">
          This page will allow you to reset your password by entering your online profile name.
          We'll send your password to the email address associated with your account.
          This feature will be implemented in Phase 3 of the website modernization project.
        </p>
        <Link href="/support/passwords" className="text-[var(--color-poker-green)] hover:underline font-bold">
          View Password Help →
        </Link>
      </div>
    </div>
  )
}
