/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 *
 * Sidebar navigation data for Online Portal and Admin sections
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

export interface SidebarItem {
  title: string
  link: string
  icon?: string
}

export interface SidebarSection {
  title?: string // Optional section title
  items: SidebarItem[]
}

export const onlineSidebarData: SidebarSection[] = [
  {
    title: 'Games',
    items: [
      { title: 'Available Games', link: '/online/available', icon: '🎲' },
      { title: 'Current Games', link: '/online/current', icon: '▶️' },
      { title: 'Hosts', link: '/online/hosts', icon: '🎯' },
    ],
  },
  {
    title: 'Statistics',
    items: [
      { title: 'Leaderboard', link: '/online/leaderboard', icon: '🏆' },
      { title: 'Completed Games', link: '/online/completed', icon: '✅' },
      { title: 'History', link: '/online/history', icon: '📜' },
    ],
  },
  {
    title: 'Profile',
    items: [
      { title: 'My Profile', link: '/online/myprofile', icon: '🙋' },
      { title: 'Search Players', link: '/online/search', icon: '🔍' },
    ],
  },
]

export const adminSidebarData: SidebarSection[] = [
  {
    title: 'Administration',
    items: [
      { title: 'Dashboard', link: '/admin', icon: '⚙️' },
      { title: 'Profile Search', link: '/admin/online-profile-search', icon: '👥' },
      { title: 'Registration Search', link: '/admin/reg-search', icon: '📝' },
      { title: 'Ban List', link: '/admin/ban-list', icon: '🚫' },
    ],
  },
]

export const aboutSidebarData: SidebarSection[] = [
  {
    title: 'About',
    items: [
      { title: 'Overview', link: '/about', icon: '📖' },
      { title: 'Practice', link: '/about/practice', icon: '🎯' },
      { title: 'Online', link: '/about/online', icon: '🌐' },
      { title: 'Analysis', link: '/about/analysis', icon: '📊' },
      { title: 'Poker Clock', link: '/about/pokerclock', icon: '⏱️' },
      { title: 'Screenshots', link: '/about/screenshots', icon: '📸' },
      { title: 'FAQ', link: '/about/faq', icon: '❓' },
    ],
  },
]

export const supportSidebarData: SidebarSection[] = [
  {
    title: 'Support',
    items: [
      { title: 'Overview', link: '/support', icon: '🆘' },
      { title: 'Self Help', link: '/support/selfhelp', icon: '🔧' },
      { title: 'Password Help', link: '/support/passwords', icon: '🔑' },
    ],
  },
]
