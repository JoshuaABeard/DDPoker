/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Login Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Suspense } from 'react'
import { LoginForm } from '@/components/auth/LoginForm'
import Footer from '@/components/layout/Footer'

export const metadata = {
  title: 'Login - DD Poker',
  description: 'Log in to your DD Poker account',
}

function LoginContent() {
  return (
    <div className="container mx-auto px-4 py-12">
      <LoginForm />

      <div className="max-w-md mx-auto mt-8 text-center">
        <h3 className="text-lg font-semibold mb-3">Need Help?</h3>
        <ul className="space-y-2 text-sm">
          <li>
            <a href="/forgot" className="text-green-600 hover:underline">
              Forgot your password?
            </a>
          </li>
          <li>
            <a href="/support" className="text-green-600 hover:underline">
              Contact Support
            </a>
          </li>
          <li>
            <a href="/support/selfhelp" className="text-green-600 hover:underline">
              Self-Help Guide
            </a>
          </li>
        </ul>
      </div>
    </div>
  )
}

export default function LoginPage() {
  return (
    <>
      <Suspense fallback={<div className="container mx-auto px-4 py-12 text-center">Loading...</div>}>
        <LoginContent />
      </Suspense>
      <Footer />
    </>
  )
}
