/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import SidebarLayout from '@/components/layout/SidebarLayout'
import { gamesSidebarData } from '@/lib/sidebarData'

export default function GamesLayout({ children }: { children: React.ReactNode }) {
  return (
    <SidebarLayout sections={gamesSidebarData}>
      {children}
    </SidebarLayout>
  )
}
