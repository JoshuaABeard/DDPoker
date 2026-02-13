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
  const [focusedIndex, setFocusedIndex] = useState<number>(-1)
  const dropdownRefs = useRef<Record<string, HTMLLIElement | null>>({})
  const dropdownLinkRefs = useRef<Record<string, HTMLAnchorElement[]>>({})

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
          setFocusedIndex(-1)
        }
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [openDropdown])

  // Handle Escape key to close dropdowns
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && openDropdown) {
        setOpenDropdown(null)
        setFocusedIndex(-1)
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [openDropdown])

  const isActive = (link: string) => {
    if (link === '/') return pathname === '/'
    return pathname.startsWith(link)
  }

  const toggleDropdown = (key: string) => {
    setOpenDropdown(openDropdown === key ? null : key)
    setFocusedIndex(-1)
  }

  const handleDropdownKeyDown = (e: React.KeyboardEvent, key: string, hasDropdown: boolean) => {
    if (!hasDropdown) return

    const item = navData[key]
    const itemCount = item.subPages?.length || 0

    switch (e.key) {
      case 'Enter':
      case ' ':
        e.preventDefault()
        toggleDropdown(key)
        break
      case 'ArrowDown':
        e.preventDefault()
        if (openDropdown === key) {
          // Navigate within open dropdown
          const nextIndex = focusedIndex < itemCount - 1 ? focusedIndex + 1 : 0
          setFocusedIndex(nextIndex)
          dropdownLinkRefs.current[key]?.[nextIndex]?.focus()
        } else {
          // Open dropdown and focus first item
          setOpenDropdown(key)
          setFocusedIndex(0)
          setTimeout(() => {
            dropdownLinkRefs.current[key]?.[0]?.focus()
          }, 0)
        }
        break
      case 'ArrowUp':
        e.preventDefault()
        if (openDropdown === key) {
          const prevIndex = focusedIndex > 0 ? focusedIndex - 1 : itemCount - 1
          setFocusedIndex(prevIndex)
          dropdownLinkRefs.current[key]?.[prevIndex]?.focus()
        }
        break
    }
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
    // Initialize dropdown link refs array
    if (!dropdownLinkRefs.current[key]) {
      dropdownLinkRefs.current[key] = []
    }

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
          onKeyDown={(e) => handleDropdownKeyDown(e, key, !!hasDropdown)}
          role={hasDropdown ? 'button' : undefined}
          aria-haspopup={hasDropdown ? 'true' : undefined}
          aria-expanded={hasDropdown ? openDropdown === key : undefined}
        >
          {item.title}
          {hasDropdown && <span className="dropdown-arrow"> ▼</span>}
        </Link>
        {hasDropdown && openDropdown === key && (
          <ul className="dropdown-menu" role="menu">
            {item.subPages?.map((subPage, index) => (
              <li key={subPage.link} role="none">
                <Link
                  href={subPage.link}
                  className={`dropdown-link ${
                    pathname === subPage.link ? 'active' : ''
                  }`}
                  role="menuitem"
                  ref={(el) => {
                    if (el) dropdownLinkRefs.current[key][index] = el
                  }}
                  tabIndex={focusedIndex === index ? 0 : -1}
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
          background: linear-gradient(180deg, #451a03 0%, #292524 100%);
          box-shadow: 0 2px 12px rgba(0, 0, 0, 0.5);
          border-bottom: 1px solid rgba(217, 119, 6, 0.3);
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
          gap: 1rem;
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
          padding: 0.75rem 1.5rem;
          color: #ffffff !important;
          text-decoration: none;
          font-family: "Delius", cursive;
          font-variant: small-caps;
          font-size: 1.1rem;
          font-weight: 500;
          background: rgba(0, 0, 0, 0.2) !important;
          border: 1px solid rgba(217, 119, 6, 0.3) !important;
          border-radius: 4px !important;
          transition: all 0.2s;
        }

        .login-link:hover {
          background: rgba(217, 119, 6, 0.3);
          border-color: rgba(217, 119, 6, 0.6);
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
        }

        .nav-item {
          position: relative;
        }

        .nav-link {
          display: block;
          padding: 0.75rem 1.5rem;
          color: #ffffff !important;
          text-decoration: none;
          font-family: "Delius", cursive;
          font-variant: small-caps;
          font-size: 1.2rem;
          font-weight: 500;
          background: rgba(0, 0, 0, 0.2) !important;
          border: 1px solid rgba(217, 119, 6, 0.3) !important;
          border-radius: 4px !important;
          transition: all 0.2s;
          cursor: pointer;
          margin: 0 0.25rem;
        }

        .nav-link:hover {
          background: rgba(217, 119, 6, 0.3);
          border-color: rgba(217, 119, 6, 0.6);
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
        }

        .nav-link.active {
          background: rgba(217, 119, 6, 0.4);
          border-color: rgba(217, 119, 6, 0.7);
        }

        .dropdown-arrow {
          font-size: 0.8rem;
          margin-left: 0.25rem;
        }

        .dropdown-menu {
          position: absolute;
          top: 100%;
          left: 0;
          background: linear-gradient(180deg, #292524 0%, #1c1917 100%);
          min-width: 200px;
          list-style: none;
          margin: 0;
          padding: 0.5rem 0;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.6);
          border-radius: 0 0 4px 4px;
          border: 1px solid rgba(217, 119, 6, 0.4);
        }

        .dropdown-link {
          display: block;
          padding: 0.5rem 1.25rem;
          color: #ffffff !important;
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
            background: linear-gradient(180deg, #292524 0%, #1c1917 100%);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.6);
            border: 1px solid rgba(217, 119, 6, 0.3);
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
