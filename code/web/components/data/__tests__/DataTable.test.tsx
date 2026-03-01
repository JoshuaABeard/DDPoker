/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { DataTable } from '../DataTable'

interface Row {
  id: number
  name: string
  score: number
}

const rows: Row[] = [
  { id: 1, name: 'Alice', score: 100 },
  { id: 2, name: 'Bob', score: 200 },
  { id: 3, name: 'Charlie', score: 300 },
]

const columns = [
  { key: 'name', header: 'Name', render: (item: Row) => item.name },
  { key: 'score', header: 'Score', render: (item: Row) => String(item.score) },
]

describe('DataTable', () => {
  it('renders all rows', () => {
    render(<DataTable data={rows} columns={columns} />)
    expect(screen.getByText('Alice')).toBeTruthy()
    expect(screen.getByText('Bob')).toBeTruthy()
    expect(screen.getByText('Charlie')).toBeTruthy()
  })

  it('renders all column headers', () => {
    render(<DataTable data={rows} columns={columns} />)
    expect(screen.getByText('Name')).toBeTruthy()
    expect(screen.getByText('Score')).toBeTruthy()
  })

  it('renders all cell values', () => {
    render(<DataTable data={rows} columns={columns} />)
    expect(screen.getByText('100')).toBeTruthy()
    expect(screen.getByText('200')).toBeTruthy()
    expect(screen.getByText('300')).toBeTruthy()
  })

  it('shows default empty message when data is empty', () => {
    render(<DataTable data={[]} columns={columns} />)
    expect(screen.getByText('No data available')).toBeTruthy()
  })

  it('shows custom emptyMessage when data is empty', () => {
    render(<DataTable data={[]} columns={columns} emptyMessage="Nothing to show" />)
    expect(screen.getByText('Nothing to show')).toBeTruthy()
  })

  it('does not render a table when data is empty', () => {
    render(<DataTable data={[]} columns={columns} />)
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('highlights the currentUser row when highlightField matches', () => {
    render(
      <DataTable data={rows} columns={columns} currentUser="Alice" highlightField="name" />,
    )
    const allRows = screen.getAllByRole('row').filter((r) => r.closest('tbody'))
    // Alice is row 0 — should have aria-current="row"
    expect(allRows[0].getAttribute('aria-current')).toBe('row')
    // Bob is not the current user
    expect(allRows[1].getAttribute('aria-current')).toBeNull()
  })

  it('does not highlight any row when currentUser does not match', () => {
    render(
      <DataTable data={rows} columns={columns} currentUser="Zara" highlightField="name" />,
    )
    screen
      .getAllByRole('row')
      .filter((r) => r.closest('tbody'))
      .forEach((tr) => {
        expect(tr.getAttribute('aria-current')).toBeNull()
      })
  })

  it('uses keyField for row keys (renders without errors)', () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    render(<DataTable data={rows} columns={columns} keyField="id" />)
    const reactKeyWarning = errorSpy.mock.calls.find(
      (args) => typeof args[0] === 'string' && args[0].includes('key'),
    )
    expect(reactKeyWarning).toBeUndefined()
    expect(screen.getByText('Alice')).toBeTruthy()
    errorSpy.mockRestore()
  })

  it('calls custom render function for each row', () => {
    const renderFn = vi.fn((item: Row) => <span>{item.name.toUpperCase()}</span>)
    const customColumns = [{ key: 'name', header: 'Name', render: renderFn }]
    render(<DataTable data={rows} columns={customColumns} />)
    expect(renderFn).toHaveBeenCalledTimes(3)
    expect(screen.getByText('ALICE')).toBeTruthy()
    expect(screen.getByText('BOB')).toBeTruthy()
    expect(screen.getByText('CHARLIE')).toBeTruthy()
  })
})
