'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Reusable Dialog/Modal Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { useEffect, useId, useRef } from 'react'

interface DialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm?: () => void
  title: string
  message: string
  type?: 'alert' | 'confirm'
  confirmText?: string
  cancelText?: string
}

export function Dialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  type = 'alert',
  confirmText = 'OK',
  cancelText = 'Cancel',
}: DialogProps) {
  // Generate unique IDs for ARIA attributes (prevents collisions if multiple Dialogs exist)
  const titleId = useId()
  const messageId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)

  // Handle Escape key (S8: only register listener when dialog is open)
  useEffect(() => {
    if (!isOpen) return

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [isOpen, onClose])

  // S2: Focus trap - trap focus within dialog using ref
  useEffect(() => {
    if (!isOpen || !dialogRef.current) return

    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab' || !dialogRef.current) return

      const focusableElements = dialogRef.current.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      )
      const firstElement = focusableElements[0] as HTMLElement
      const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          lastElement?.focus()
          e.preventDefault()
        }
      } else {
        if (document.activeElement === lastElement) {
          firstElement?.focus()
          e.preventDefault()
        }
      }
    }

    document.addEventListener('keydown', handleTabKey)
    return () => document.removeEventListener('keydown', handleTabKey)
  }, [isOpen])

  if (!isOpen) return null

  const handleConfirm = () => {
    onConfirm?.()
    onClose()
  }

  return (
    <div
      className="dialog-overlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={messageId}
    >
      <div className="dialog-content" onClick={(e) => e.stopPropagation()} ref={dialogRef}>
        <div className="dialog-header">
          <h3 id={titleId} className="dialog-title">
            {title}
          </h3>
        </div>

        <div className="dialog-body">
          <p id={messageId}>{message}</p>
        </div>

        <div className="dialog-actions">
          {type === 'confirm' && (
            <button
              onClick={onClose}
              className="dialog-button dialog-button-cancel"
            >
              {cancelText}
            </button>
          )}
          <button
            onClick={handleConfirm}
            className="dialog-button dialog-button-confirm"
            autoFocus
          >
            {confirmText}
          </button>
        </div>
      </div>

      <style jsx>{`
        .dialog-overlay {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 9999;
        }

        .dialog-content {
          background: white;
          border-radius: 8px;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
          max-width: 500px;
          width: 90%;
          max-height: 80vh;
          overflow: auto;
        }

        .dialog-header {
          padding: 1.5rem 1.5rem 1rem;
          border-bottom: 1px solid #e5e7eb;
        }

        .dialog-title {
          font-size: 1.25rem;
          font-weight: 600;
          color: #111827;
          margin: 0;
        }

        .dialog-body {
          padding: 1.5rem;
          color: #374151;
          line-height: 1.6;
        }

        .dialog-body p {
          margin: 0;
        }

        .dialog-actions {
          padding: 1rem 1.5rem;
          border-top: 1px solid #e5e7eb;
          display: flex;
          gap: 0.75rem;
          justify-content: flex-end;
        }

        .dialog-button {
          padding: 0.5rem 1.25rem;
          border-radius: 4px;
          font-size: 0.875rem;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;
          border: 1px solid transparent;
        }

        .dialog-button-cancel {
          background: white;
          color: #374151;
          border-color: #d1d5db;
        }

        .dialog-button-cancel:hover {
          background: #f9fafb;
          border-color: #9ca3af;
        }

        .dialog-button-confirm {
          background: #2563eb;
          color: white;
        }

        .dialog-button-confirm:hover {
          background: #1d4ed8;
        }
      `}</style>
    </div>
  )
}
