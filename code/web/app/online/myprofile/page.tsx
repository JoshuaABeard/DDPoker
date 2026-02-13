'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - My Profile Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { useAuth } from '@/lib/auth/useAuth'
import { LoginForm } from '@/components/auth/LoginForm'
import { PasswordChangeForm } from '@/components/profile/PasswordChangeForm'
import { AliasManagement } from '@/components/profile/AliasManagement'
import { profileApi } from '@/lib/api'

export default function MyProfilePage() {
  const { user, isLoading, isAuthenticated } = useAuth()
  const [aliases, setAliases] = useState<
    Array<{ name: string; createdDate: string; retiredDate?: string }>
  >([])

  useEffect(() => {
    async function fetchAliases() {
      if (user) {
        try {
          const data = await profileApi.getAliases()
          setAliases(
            data.map((a) => ({
              name: a.name,
              createdDate: a.createdDate,
              retiredDate: a.retiredDate,
            }))
          )
        } catch (error) {
          console.error('Failed to fetch aliases:', error)
        }
      }
    }
    fetchAliases()
  }, [user])

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8 text-center">
        <div className="text-xl">Loading...</div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-2xl">
        <h1 className="text-3xl font-bold mb-6">My Profile</h1>
        <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 mb-6">
          <p className="text-yellow-800">
            Please log in to view your profile and manage your account.
          </p>
        </div>
        <LoginForm />
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-6">My Profile</h1>

      <div className="mb-6 bg-white rounded-lg shadow-md p-6">
        <h2 className="text-2xl font-bold mb-4">Profile Information</h2>
        <div className="space-y-2">
          <div>
            <span className="font-medium">Username:</span>{' '}
            <span className="text-gray-700">{user.username}</span>
          </div>
          <div>
            <span className="font-medium">Status:</span>{' '}
            <span className="text-green-600">Active</span>
          </div>
          {user.isAdmin && (
            <div>
              <span className="inline-block px-3 py-1 bg-yellow-400 text-black text-sm rounded">
                Administrator
              </span>
            </div>
          )}
        </div>
      </div>

      <div className="space-y-6">
        <PasswordChangeForm />

        <AliasManagement aliases={aliases} />

        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-2xl font-bold mb-4">Quick Links</h2>
          <div className="space-y-2">
            <div>
              <Link
                href={`/online/history?name=${encodeURIComponent(user.username)}`}
                className="text-blue-600 hover:underline"
              >
                View My Tournament History
              </Link>
            </div>
            <div>
              <Link href="/online/leaderboard" className="text-blue-600 hover:underline">
                View Leaderboard
              </Link>
            </div>
            <div>
              <Link href="/online" className="text-blue-600 hover:underline">
                Back to Online Portal
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
