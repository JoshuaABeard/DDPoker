'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Forgot Password Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useState } from 'react'
import Link from 'next/link'
import { authApi } from '@/lib/api'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [result, setResult] = useState<{ success: boolean; message: string } | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!email.trim()) {
      setResult({ success: false, message: 'Please enter your email address' })
      return
    }

    setIsSubmitting(true)
    setResult(null)

    try {
      const response = await authApi.forgotPassword(email.trim())
      setResult(response)

      // Clear form on success
      if (response.success) {
        setEmail('')
      }
    } catch (error) {
      setResult({
        success: false,
        message: 'An error occurred. Please try again later.',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl">
      <h1 className="text-3xl font-bold mb-6">Forgot Password</h1>

      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <p className="mb-4 text-gray-700">
          Enter your email address below and we&apos;ll send a password reset link to your inbox.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
              Email address
            </label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-[var(--color-poker-green)]"
              placeholder="Enter your email address"
              disabled={isSubmitting}
              autoFocus
            />
          </div>

          {result && (
            <div
              className={`p-4 rounded-md ${
                result.success
                  ? 'bg-green-50 border border-green-400 text-green-800'
                  : 'bg-red-50 border border-red-400 text-red-800'
              }`}
              role={result.success ? 'status' : 'alert'}
            >
              {result.message}
            </div>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-[var(--color-poker-green)] text-white py-2 px-4 rounded-md hover:bg-[var(--color-dark-green)] disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {isSubmitting ? 'Sending...' : 'Send Reset Link'}
          </button>
        </form>
      </div>

      <div className="text-center space-y-2">
        <div>
          <Link href="/login" className="text-[var(--color-poker-green)] hover:underline">
            ← Back to Login
          </Link>
        </div>
        <div>
          <Link href="/support/passwords" className="text-[var(--color-poker-green)] hover:underline">
            Password Help & FAQ
          </Link>
        </div>
        <div>
          <Link href="/support" className="text-[var(--color-poker-green)] hover:underline">
            Contact Support
          </Link>
        </div>
      </div>
    </div>
  )
}
