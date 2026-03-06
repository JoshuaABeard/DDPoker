'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Alias Management Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useState } from 'react'
import { Dialog } from '@/components/ui/Dialog'
import { profileApi } from '@/lib/api'

interface Alias {
  id: number
  name: string
  createdDate: string
}

interface AliasManagementProps {
  aliases: Alias[]
}

export function AliasManagement({ aliases }: AliasManagementProps) {
  const [isLoading, setIsLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [dialogState, setDialogState] = useState<{
    isOpen: boolean
    aliasName: string
  }>({ isOpen: false, aliasName: '' })

  const handleRetire = async (aliasName: string) => {
    setDialogState({ isOpen: true, aliasName })
  }

  const confirmRetire = async () => {
    const alias = aliases.find((a) => a.name === dialogState.aliasName)
    if (!alias) return

    setIsLoading(true)
    setMessage(null)

    try {
      await profileApi.retireProfile(alias.id)
      setMessage({ type: 'success', text: `Alias "${alias.name}" retired successfully` })
    } catch (error) {
      setMessage({
        type: 'error',
        text: error instanceof Error ? error.message : 'Failed to retire alias',
      })
    } finally {
      setIsLoading(false)
    }
  }

  const activeAliases = aliases

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-2xl font-bold mb-4">Alias Management</h2>

      {message && (
        <div
          className={`mb-4 p-3 rounded ${
            message.type === 'success'
              ? 'bg-green-100 border border-green-400 text-green-700'
              : 'bg-red-100 border border-red-400 text-red-700'
          }`}
          role={message.type === 'success' ? 'status' : 'alert'}
        >
          {message.text}
        </div>
      )}

      <div className="mb-6">
        <h3 className="text-lg font-semibold mb-2">Active Aliases</h3>
        {activeAliases.length === 0 ? (
          <p className="text-gray-500">No active aliases</p>
        ) : (
          <div className="space-y-2">
            {activeAliases.map((alias) => (
              <div
                key={alias.name}
                className="flex justify-between items-center p-3 border border-gray-300 rounded"
              >
                <div>
                  <div className="font-medium">{alias.name}</div>
                  <div className="text-xs text-gray-500">
                    Created: {new Date(alias.createdDate).toLocaleDateString()}
                  </div>
                </div>
                <button
                  onClick={() => handleRetire(alias.name)}
                  disabled={isLoading}
                  className="px-3 py-1 bg-red-600 text-white rounded hover:bg-red-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors text-sm"
                >
                  Retire
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <Dialog
        isOpen={dialogState.isOpen}
        onClose={() => setDialogState({ isOpen: false, aliasName: '' })}
        onConfirm={confirmRetire}
        title="Retire Alias"
        message={`Are you sure you want to retire the alias "${dialogState.aliasName}"? This cannot be undone.`}
        type="confirm"
        confirmText="Retire"
        cancelText="Cancel"
      />
    </div>
  )
}
