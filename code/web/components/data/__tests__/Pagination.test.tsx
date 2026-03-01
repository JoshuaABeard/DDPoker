/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { Pagination } from '../Pagination'

const mockPush = vi.fn()
let mockSearchParams: URLSearchParams

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useSearchParams: () => mockSearchParams,
}))

describe('Pagination', () => {
  beforeEach(() => {
    mockPush.mockClear()
    mockSearchParams = new URLSearchParams()
  })

  it('returns null when totalPages <= 1', () => {
    // 5 items, 10 per page = 1 page
    const { container } = render(
      <Pagination currentPage={1} totalItems={5} itemsPerPage={10} />,
    )
    expect(container.firstChild).toBeNull()
  })

  it('renders Previous and Next buttons', () => {
    render(
      <Pagination currentPage={2} totalItems={50} itemsPerPage={10} />,
    )
    expect(screen.getByRole('button', { name: /^previous$/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /^next$/i })).toBeTruthy()
  })

  it('Previous button is disabled on page 1', () => {
    render(
      <Pagination currentPage={1} totalItems={50} itemsPerPage={10} />,
    )
    const prev = screen.getByRole('button', { name: /^previous$/i })
    expect((prev as HTMLButtonElement).disabled).toBe(true)
  })

  it('Next button is disabled on last page', () => {
    // 50 items / 10 per page = 5 pages, on page 5
    render(
      <Pagination currentPage={5} totalItems={50} itemsPerPage={10} />,
    )
    const next = screen.getByRole('button', { name: /^next$/i })
    expect((next as HTMLButtonElement).disabled).toBe(true)
  })

  it('clicking a page number calls router.push with correct URL', () => {
    render(
      <Pagination currentPage={1} totalItems={50} itemsPerPage={10} baseUrl="/games" />,
    )
    // Page 2 button should be visible (totalPages=5, all shown)
    fireEvent.click(screen.getByRole('button', { name: '2' }))
    expect(mockPush).toHaveBeenCalledWith('/games?page=2')
  })

  it('clicking Next advances to next page', () => {
    render(
      <Pagination currentPage={2} totalItems={50} itemsPerPage={10} baseUrl="/games" />,
    )
    fireEvent.click(screen.getByRole('button', { name: /^next$/i }))
    expect(mockPush).toHaveBeenCalledWith('/games?page=3')
  })

  it('shows ellipsis for large page counts', () => {
    // 100 items / 10 per page = 10 pages; on page 1, currentPage <= 3
    // no leading ellipsis, but trailing ellipsis before last page
    render(
      <Pagination currentPage={1} totalItems={100} itemsPerPage={10} />,
    )
    // At least one ellipsis should be shown
    const ellipses = screen.getAllByText('...')
    expect(ellipses.length).toBeGreaterThan(0)
  })

  it('shows page info text', () => {
    render(
      <Pagination currentPage={2} totalItems={50} itemsPerPage={10} />,
    )
    expect(screen.getByText(/page 2 of 5/i)).toBeTruthy()
    expect(screen.getByText(/50 total items/i)).toBeTruthy()
  })
})
