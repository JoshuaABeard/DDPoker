'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Online Portal Layout
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Sidebar from '@/components/layout/Sidebar'
import Footer from '@/components/layout/Footer'
import { onlineSidebarData } from '@/lib/sidebarData'

export default function OnlineLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="portal-layout">
      <Sidebar sections={onlineSidebarData} title="Online Portal" variant="default" />
      <div className="portal-content-wrapper">
        <main className="portal-content">{children}</main>
        <Footer />
      </div>

      <style jsx>{`
        .portal-layout {
          display: flex;
          min-height: calc(100vh - 60px);
        }

        .portal-content-wrapper {
          flex: 1;
          display: flex;
          flex-direction: column;
          background: #f5f5f5;
        }

        .portal-content {
          flex: 1;
          padding: 2rem;
          max-width: 1400px;
          margin: 0 auto;
          width: 100%;
        }

        @media (max-width: 768px) {
          .portal-content {
            padding: 1rem;
          }
        }
      `}</style>
    </div>
  )
}
