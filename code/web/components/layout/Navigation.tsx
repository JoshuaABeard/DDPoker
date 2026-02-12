'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 *
 * Navigation component with responsive hamburger menu
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useState, useEffect, useRef } from 'react'
import { navData } from '@/lib/navData'
import { useAuth } from '@/lib/auth/useAuth'
import { CurrentProfile } from '@/components/auth/CurrentProfile'

export default function Navigation() {
  const pathname = usePathname()
  const { user, isAuthenticated } = useAuth()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [openDropdown, setOpenDropdown] = useState<string | null>(null)
  const dropdownRefs = useRef<Record<string, HTMLLIElement | null>>({})

  // Close mobile menu when route changes
  useEffect(() => {
    setMobileMenuOpen(false)
    setOpenDropdown(null)
  }, [pathname])

  // Close dropdowns when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement
      if (openDropdown && dropdownRefs.current[openDropdown]) {
        if (!dropdownRefs.current[openDropdown]?.contains(target)) {
          setOpenDropdown(null)
        }
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [openDropdown])

  const isActive = (link: string) => {
    if (link === '/') return pathname === '/'
    return pathname.startsWith(link)
  }

  const toggleDropdown = (key: string) => {
    setOpenDropdown(openDropdown === key ? null : key)
  }

  const renderNavItem = (key: string, mobile = false) => {
    const item = navData[key]

    // Skip admin menu if not admin
    if (item.admin && !user?.isAdmin) {
      return null
    }

    const hasDropdown = item.subPages && item.subPages.length > 0
    const active = isActive(item.link)

    if (mobile) {
      return (
        <li key={key} className="mobile-nav-item">
          <Link
            href={item.link}
            className={`mobile-nav-link ${active ? 'active' : ''}`}
          >
            {item.title}
          </Link>
          {hasDropdown && (
            <ul className="mobile-submenu">
              {item.subPages?.map((subPage) => (
                <li key={subPage.link}>
                  <Link
                    href={subPage.link}
                    className={`mobile-submenu-link ${
                      pathname === subPage.link ? 'active' : ''
                    }`}
                  >
                    {subPage.title}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </li>
      )
    }

    // Desktop navigation
    return (
      <li
        key={key}
        className="nav-item"
        ref={(el) => {
          if (el) dropdownRefs.current[key] = el
        }}
      >
        <Link
          href={item.link}
          className={`nav-link ${active ? 'active' : ''}`}
          onClick={(e) => {
            if (hasDropdown) {
              e.preventDefault()
              toggleDropdown(key)
            }
          }}
        >
          {item.title}
          {hasDropdown && <span className="dropdown-arrow"> ▼</span>}
        </Link>
        {hasDropdown && openDropdown === key && (
          <ul className="dropdown-menu">
            {item.subPages?.map((subPage) => (
              <li key={subPage.link}>
                <Link
                  href={subPage.link}
                  className={`dropdown-link ${
                    pathname === subPage.link ? 'active' : ''
                  }`}
                >
                  {subPage.title}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </li>
    )
  }

  return (
    <header className="header">
      <div className="header-top">
        <div className="header-wrapper">
          {/* Desktop Navigation */}
          <nav className="desktop-nav">
            <ul className="main-nav">
              {Object.keys(navData).map((key) => renderNavItem(key, false))}
            </ul>
            <div className="auth-section">
              {isAuthenticated ? (
                <CurrentProfile />
              ) : (
                <Link href="/login" className="login-link">
                  Log In
                </Link>
              )}
            </div>
          </nav>

          {/* Mobile Menu Toggle */}
          <div className="mobile-menu-title">
            <Link href="/" className="mobile-logo">
              DD Poker
            </Link>
            <button
              className="mobile-menu-toggle"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-label="Toggle menu"
              aria-expanded={mobileMenuOpen}
            >
              ☰
            </button>
          </div>

          {/* Mobile Navigation */}
          {mobileMenuOpen && (
            <nav className="mobile-nav">
              <ul className="mobile-nav-list">
                {Object.keys(navData).map((key) => renderNavItem(key, true))}
              </ul>
            </nav>
          )}
        </div>
      </div>

      <style jsx>{`
        /* Desktop Navigation */
        .header {
          background: linear-gradient(180deg, #3a3a3a 0%, #2a2a2a 50%, #3a3a3a 100%);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
          position: sticky;
          top: 0;
          z-index: 1000;
        }

        .header-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 1rem;
          min-height: 60px;
        }

        .header-wrapper {
          width: 100%;
        }

        .desktop-nav {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .main-nav {
          display: flex;
          gap: 0;
          list-style: none;
          margin: 0;
          padding: 0;
        }

        .auth-section {
          margin-left: auto;
          padding: 0 1.25rem;
        }

        .login-link {
          display: block;
          padding: 0.5rem 1rem;
          color: white;
          text-decoration: none;
          font-family: "Delius", cursive;
          font-variant: small-caps;
          font-size: 1.1rem;
          font-weight: 500;
          background-color: rgba(255, 255, 255, 0.1);
          border-radius: 4px;
          transition: background-color 0.2s;
        }

        .login-link:hover {
          background-color: rgba(255, 255, 255, 0.2);
        }

        .nav-item {
          position: relative;
        }

        .nav-link {
          display: block;
          padding: 0.75rem 1.25rem;
          color: white;
          text-decoration: none;
          font-family: "Delius", cursive;
          font-variant: small-caps;
          font-size: 1.2rem;
          font-weight: 500;
          transition: background-color 0.2s;
          cursor: pointer;
        }

        .nav-link:hover,
        .nav-link.active {
          background-color: rgba(255, 255, 255, 0.1);
        }

        .dropdown-arrow {
          font-size: 0.8rem;
          margin-left: 0.25rem;
        }

        .dropdown-menu {
          position: absolute;
          top: 100%;
          left: 0;
          background: #2a2a2a;
          min-width: 200px;
          list-style: none;
          margin: 0;
          padding: 0.5rem 0;
          box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
          border-radius: 0 0 4px 4px;
        }

        .dropdown-link {
          display: block;
          padding: 0.5rem 1.25rem;
          color: white;
          text-decoration: none;
          font-size: 1rem;
          transition: background-color 0.2s;
        }

        .dropdown-link:hover,
        .dropdown-link.active {
          background-color: rgba(255, 255, 255, 0.1);
        }

        /* Mobile Navigation */
        .mobile-menu-title {
          display: none;
        }

        .mobile-nav {
          display: none;
        }

        @media (max-width: 768px) {
          .desktop-nav {
            display: none;
          }

          .mobile-menu-title {
            display: flex;
            justify-content: space-between;
            align-items: center;
            width: 100%;
          }

          .mobile-logo {
            color: white;
            text-decoration: none;
            font-family: "Delius", cursive;
            font-variant: small-caps;
            font-size: 1.5rem;
            font-weight: 700;
          }

          .mobile-menu-toggle {
            background: none;
            border: none;
            color: white;
            font-size: 2rem;
            cursor: pointer;
            padding: 0.25rem 0.5rem;
          }

          .mobile-nav {
            display: block;
            position: absolute;
            top: 100%;
            left: 0;
            right: 0;
            background: #2a2a2a;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
          }

          .mobile-nav-list {
            list-style: none;
            margin: 0;
            padding: 0;
          }

          .mobile-nav-item {
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
          }

          .mobile-nav-link {
            display: block;
            padding: 1rem 1.25rem;
            color: white;
            text-decoration: none;
            font-size: 1.1rem;
          }

          .mobile-nav-link.active {
            background-color: rgba(255, 255, 255, 0.1);
          }

          .mobile-submenu {
            list-style: none;
            margin: 0;
            padding: 0;
            background: rgba(0, 0, 0, 0.2);
          }

          .mobile-submenu-link {
            display: block;
            padding: 0.75rem 1.25rem 0.75rem 2.5rem;
            color: rgba(255, 255, 255, 0.9);
            text-decoration: none;
            font-size: 0.95rem;
          }

          .mobile-submenu-link.active {
            background-color: rgba(255, 255, 255, 0.1);
          }
        }
      `}</style>
    </header>
  )
}
