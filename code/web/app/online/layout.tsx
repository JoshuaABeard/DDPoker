/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Online Portal Layout
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import SidebarLayout from '@/components/layout/SidebarLayout'
import { onlineSidebarData } from '@/lib/sidebarData'

export default function OnlineLayout({ children }: { children: React.ReactNode }) {
  return (
    <SidebarLayout sections={onlineSidebarData}>
      {children}
    </SidebarLayout>
  )
}
