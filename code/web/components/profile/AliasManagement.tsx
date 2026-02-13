'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Alias Management Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useState } from 'react'
import { Dialog } from '@/components/ui/Dialog'

interface Alias {
  name: string
  createdDate: string
  retiredDate?: string
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
    const aliasName = dialogState.aliasName

    setIsLoading(true)
    setMessage(null)

    try {
      // TODO: Replace with actual API call
      // await profileApi.retireAlias(aliasName)

      setMessage({ type: 'success', text: `Alias "${aliasName}" retired successfully` })
      // In real implementation, would refresh the alias list
    } catch (error) {
      setMessage({
        type: 'error',
        text: error instanceof Error ? error.message : 'Failed to retire alias',
      })
    } finally {
      setIsLoading(false)
    }
  }

  const activeAliases = aliases.filter((a) => !a.retiredDate)
  const retiredAliases = aliases.filter((a) => a.retiredDate)

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

      {retiredAliases.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold mb-2">Retired Aliases</h3>
          <div className="space-y-2">
            {retiredAliases.map((alias) => (
              <div key={alias.name} className="p-3 border border-gray-300 rounded bg-gray-50">
                <div className="font-medium text-gray-600">{alias.name}</div>
                <div className="text-xs text-gray-500">
                  Retired: {alias.retiredDate && new Date(alias.retiredDate).toLocaleDateString()}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

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
