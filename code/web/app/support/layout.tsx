/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Support Section Layout
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import SidebarLayout from '@/components/layout/SidebarLayout'
import { supportSidebarData } from '@/lib/sidebarData'

export default function SupportLayout({ children }: { children: React.ReactNode }) {
  return (
    <SidebarLayout sections={supportSidebarData} title="Support">
      {children}
    </SidebarLayout>
  )
}
