'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - About Section Layout
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Sidebar from '@/components/layout/Sidebar'
import Footer from '@/components/layout/Footer'
import { aboutSidebarData } from '@/lib/sidebarData'

export default function AboutLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="about-layout">
      <Sidebar sections={aboutSidebarData} title="About" variant="default" />
      <div className="about-content-wrapper">
        <main className="about-content">{children}</main>
        <Footer />
      </div>

      <style jsx>{`
        .about-layout {
          display: flex;
          min-height: calc(100vh - 60px);
        }

        .about-content-wrapper {
          flex: 1;
          display: flex;
          flex-direction: column;
          background: #f5f5f5;
        }

        .about-content {
          flex: 1;
          padding: 2rem;
          max-width: 1400px;
          margin: 0 auto;
          width: 100%;
        }

        @media (max-width: 768px) {
          .about-content {
            padding: 1rem;
          }
        }
      `}</style>
    </div>
  )
}
