/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import SidebarLayout from '../SidebarLayout'

vi.mock('@/components/layout/Sidebar', () => ({
  default: ({ sections }: { sections: unknown[] }) => (
    <div data-testid="sidebar">Sidebar ({sections.length} sections)</div>
  ),
}))

const sections = [
  { title: 'Section One', items: [{ title: 'Item A', link: '/a' }] },
  { items: [{ title: 'Item B', link: '/b' }] },
]

describe('SidebarLayout', () => {
  it('renders Sidebar with sections prop', () => {
    render(<SidebarLayout sections={sections}>Content</SidebarLayout>)
    expect(screen.getByTestId('sidebar')).toBeTruthy()
    expect(screen.getByText('Sidebar (2 sections)')).toBeTruthy()
  })

  it('renders children', () => {
    render(
      <SidebarLayout sections={sections}>
        <p>Child content here</p>
      </SidebarLayout>
    )
    expect(screen.getByText('Child content here')).toBeTruthy()
  })
})
