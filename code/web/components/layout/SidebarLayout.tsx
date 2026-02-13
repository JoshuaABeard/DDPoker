'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Shared Sidebar Layout Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Sidebar from '@/components/layout/Sidebar'
import Footer from '@/components/layout/Footer'
import { SidebarSection } from '@/lib/sidebarData'

interface SidebarLayoutProps {
  sections: SidebarSection[]
  title: string
  variant?: 'default' | 'admin'
  children: React.ReactNode
}

export default function SidebarLayout({
  sections,
  title,
  variant = 'default',
  children,
}: SidebarLayoutProps) {
  return (
    <div className="sidebar-layout">
      <Sidebar sections={sections} title={title} variant={variant} />
      <div className="sidebar-content-wrapper">
        <main className="sidebar-content">{children}</main>
        <Footer />
      </div>

      <style jsx>{`
        .sidebar-layout {
          display: flex;
          min-height: calc(100vh - 60px);
        }

        .sidebar-content-wrapper {
          flex: 1;
          display: flex;
          flex-direction: column;
          background: #f5f5f5;
        }

        .sidebar-content {
          flex: 1;
          padding: 2rem;
          max-width: 1400px;
          margin: 0 auto;
          width: 100%;
        }

        @media (max-width: 768px) {
          .sidebar-content {
            padding: 1rem;
          }
        }
      `}</style>
    </div>
  )
}
