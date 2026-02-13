'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Forgot Password Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useState } from 'react'
import Link from 'next/link'
import { authApi } from '@/lib/api'
import Footer from '@/components/layout/Footer'

export default function ForgotPassword() {
  const [username, setUsername] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [result, setResult] = useState<{ success: boolean; message: string } | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!username.trim()) {
      setResult({ success: false, message: 'Please enter your username' })
      return
    }

    setIsSubmitting(true)
    setResult(null)

    try {
      const response = await authApi.forgotPassword(username.trim())
      setResult(response)

      // Clear form on success
      if (response.success) {
        setUsername('')
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
          Enter your online profile name below and we'll send your password to the email address
          associated with your account.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-2">
              Username
            </label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-[var(--color-poker-green)]"
              placeholder="Enter your username"
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
            >
              {result.message}
            </div>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-[var(--color-poker-green)] text-white py-2 px-4 rounded-md hover:bg-[var(--color-dark-green)] disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {isSubmitting ? 'Sending...' : 'Send Password'}
          </button>
        </form>
      </div>

      <div className="bg-blue-50 border-l-4 border-blue-400 p-4 mb-6">
        <p className="text-blue-800 text-sm">
          <strong>Note:</strong> For security reasons, we can only send your password if you have an
          email address registered with your account.
        </p>
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
      <Footer />
    </div>
  )
}
