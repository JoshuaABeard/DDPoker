'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 *
 * Reusable Sidebar Navigation Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useState, useEffect } from 'react'
import { SidebarSection, SidebarItem } from '@/lib/sidebarData'

interface SidebarProps {
  sections: SidebarSection[]
  title?: string
  variant?: 'default' | 'admin'
}

export default function Sidebar({ sections, title = 'Navigation', variant = 'default' }: SidebarProps) {
  const pathname = usePathname()
  const [mobileOpen, setMobileOpen] = useState(false)

  const isActive = (item: SidebarItem) => {
    // Normalize paths by removing trailing slashes for comparison
    const normalizedPathname = pathname.replace(/\/$/, '') || '/'
    const normalizedLink = item.link.replace(/\/$/, '') || '/'

    // Use exactMatch property if specified, otherwise default to startsWith for sub-pages
    if (item.exactMatch) {
      return normalizedPathname === normalizedLink
    }
    return normalizedPathname.startsWith(normalizedLink)
  }

  // Wood and leather brown gradients for all sidebars
  const bgGradient = 'linear-gradient(180deg, #57534e 0%, #292524 100%)'
  const accentColor = '#d97706'
  const borderColor = 'rgba(217, 119, 6, 0.4)'

  // Handle Escape key to close sidebar on mobile
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && mobileOpen) {
        setMobileOpen(false)
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [mobileOpen])

  return (
    <>
      {/* Mobile Hamburger Button */}
      <button
        className="mobile-hamburger"
        onClick={() => setMobileOpen(!mobileOpen)}
        aria-label="Toggle sidebar menu"
        aria-expanded={mobileOpen}
      >
        <span></span>
        <span></span>
        <span></span>
      </button>

      {/* Mobile Backdrop/Overlay */}
      {mobileOpen && (
        <div
          className="mobile-backdrop"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`sidebar ${mobileOpen ? 'mobile-open' : ''}`}>
        <nav className="sidebar-nav">
          {sections.map((section, sectionIndex) => (
            <div key={sectionIndex} className="sidebar-section">
              {section.title && (
                <div className="section-title">
                  {section.title}
                </div>
              )}
              <ul className="section-items">
                {section.items.map((item) => (
                  <li key={item.link}>
                    <Link
                      href={item.link}
                      className={`sidebar-link ${isActive(item) ? 'active' : ''}`}
                      onClick={() => setMobileOpen(false)}
                    >
                      {item.icon && <span className="icon">{item.icon}</span>}
                      <span className="label">{item.title}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>

        <style jsx>{`
          .sidebar {
            width: 200px;
            background: ${bgGradient};
            color: white;
            height: calc(100vh - 60px);
            position: sticky;
            top: 60px;
            overflow-y: auto;
            border-right: 2px solid ${borderColor};
            flex-shrink: 0;
            box-shadow: 2px 0 8px rgba(0, 0, 0, 0.2);
          }

          .sidebar-nav {
            padding: 1.5rem 0 1.5rem 1.5rem;
          }

          .sidebar-section {
            margin-bottom: 0.5rem;
          }

          .section-title {
            color: rgba(255, 255, 255, 0.5);
            padding: 0.5rem 1.25rem 0.5rem 2rem;
            font-family: 'Delius', cursive;
            font-variant: small-caps;
            font-size: 0.75rem;
            font-weight: 500;
            letter-spacing: 0.1em;
            text-transform: uppercase;
          }

          .section-items {
            list-style: none;
            margin: 0;
            padding: 0;
          }

          .sidebar-link {
            display: flex;
            align-items: center;
            gap: 1rem;
            padding: 0.5rem 1.25rem 0.5rem 2rem;
            color: rgba(255, 255, 255, 0.85);
            text-decoration: none;
            transition: color 0.2s;
            border-left: 2px solid transparent;
            font-size: 0.95rem;
          }

          .sidebar-link:hover {
            color: ${accentColor};
            border-left-color: ${accentColor};
          }

          .sidebar-link.active {
            color: ${accentColor};
            border-left-color: ${accentColor};
            font-weight: 500;
          }

          .sidebar-link .icon {
            font-size: 1.25rem;
            width: 1.5rem;
            text-align: center;
            flex-shrink: 0;
          }

          .sidebar-link .label {
            flex: 1;
          }

          /* Mobile Hamburger Button */
          .mobile-hamburger {
            display: none;
            position: fixed;
            top: 70px;
            left: 10px;
            z-index: 1000;
            background: ${accentColor};
            border: none;
            border-radius: 4px;
            width: 40px;
            height: 40px;
            padding: 8px;
            cursor: pointer;
            flex-direction: column;
            justify-content: space-around;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
          }

          .mobile-hamburger span {
            display: block;
            width: 100%;
            height: 3px;
            background: white;
            border-radius: 2px;
            transition: all 0.3s;
          }

          .mobile-hamburger:hover {
            background: #b45309;
          }

          /* Mobile Backdrop */
          .mobile-backdrop {
            display: none;
          }

          /* Mobile Styles */
          @media (max-width: 768px) {
            .mobile-hamburger {
              display: flex;
            }

            .mobile-backdrop {
              display: block;
              position: fixed;
              top: 60px;
              left: 0;
              right: 0;
              bottom: 0;
              background: rgba(0, 0, 0, 0.5);
              z-index: 997;
            }

            .sidebar {
              position: fixed;
              top: 60px;
              left: 0;
              z-index: 998;
              transform: translateX(-100%);
              transition: transform 0.3s ease-in-out;
              box-shadow: 2px 0 8px rgba(0, 0, 0, 0.3);
            }

            .sidebar.mobile-open {
              transform: translateX(0);
            }

          }

          /* Scrollbar Styling */
          .sidebar::-webkit-scrollbar {
            width: 8px;
          }

          .sidebar::-webkit-scrollbar-track {
            background: rgba(0, 0, 0, 0.2);
          }

          .sidebar::-webkit-scrollbar-thumb {
            background: rgba(255, 255, 255, 0.3);
            border-radius: 4px;
          }

          .sidebar::-webkit-scrollbar-thumb:hover {
            background: rgba(255, 255, 255, 0.5);
          }
        `}</style>
      </aside>
    </>
  )
}
