/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - About Section Layout
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import SidebarLayout from '@/components/layout/SidebarLayout'
import { aboutSidebarData } from '@/lib/sidebarData'

export default function AboutLayout({ children }: { children: React.ReactNode }) {
  return (
    <SidebarLayout sections={aboutSidebarData}>
      {children}
    </SidebarLayout>
  )
}
