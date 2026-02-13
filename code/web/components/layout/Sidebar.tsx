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
import { useState } from 'react'
import { SidebarSection } from '@/lib/sidebarData'

interface SidebarProps {
  sections: SidebarSection[]
  title?: string
  variant?: 'default' | 'admin'
}

export default function Sidebar({ sections, title = 'Navigation', variant = 'default' }: SidebarProps) {
  const pathname = usePathname()
  const [mobileOpen, setMobileOpen] = useState(false)

  const isActive = (link: string) => {
    // Normalize paths by removing trailing slashes for comparison
    const normalizedPathname = pathname.replace(/\/$/, '') || '/'
    const normalizedLink = link.replace(/\/$/, '') || '/'

    if (normalizedLink === '/online' || normalizedLink === '/admin' || normalizedLink === '/' || normalizedLink === '/about' || normalizedLink === '/support') {
      return normalizedPathname === normalizedLink
    }
    return normalizedPathname.startsWith(normalizedLink)
  }

  // Wood and leather brown gradients for all sidebars
  const bgGradient = variant === 'admin'
    ? 'linear-gradient(180deg, #57534e 0%, #292524 100%)'
    : 'linear-gradient(180deg, #57534e 0%, #292524 100%)'
  const accentColor = variant === 'admin' ? '#d97706' : '#d97706'
  const borderColor = 'rgba(217, 119, 6, 0.4)'

  return (
    <>
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
                      className={`sidebar-link ${isActive(item.link) ? 'active' : ''}`}
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

          /* Mobile Styles */
          @media (max-width: 768px) {
            .mobile-sidebar-toggle {
              display: block;
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
