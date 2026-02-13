'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Support Section Layout
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Sidebar from '@/components/layout/Sidebar'
import Footer from '@/components/layout/Footer'
import { supportSidebarData } from '@/lib/sidebarData'

export default function SupportLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="support-layout">
      <Sidebar sections={supportSidebarData} title="Support" variant="default" />
      <div className="support-content-wrapper">
        <main className="support-content">{children}</main>
        <Footer />
      </div>

      <style jsx>{`
        .support-layout {
          display: flex;
          min-height: calc(100vh - 60px);
        }

        .support-content-wrapper {
          flex: 1;
          display: flex;
          flex-direction: column;
          background: #f5f5f5;
        }

        .support-content {
          flex: 1;
          padding: 2rem;
          max-width: 1400px;
          margin: 0 auto;
          width: 100%;
        }

        @media (max-width: 768px) {
          .support-content {
            padding: 1rem;
          }
        }
      `}</style>
    </div>
  )
}
