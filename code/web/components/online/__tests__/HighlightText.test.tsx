/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { HighlightText } from '../HighlightText'

describe('HighlightText', () => {
  it('renders plain text when no search term', () => {
    render(<HighlightText text="hello world" searchTerm="" />)
    expect(screen.getByText('hello world')).toBeTruthy()
    expect(document.querySelector('mark')).toBeNull()
  })

  it('wraps matching text in a mark element', () => {
    render(<HighlightText text="hello world" searchTerm="world" />)
    expect(screen.getByText('world').tagName).toBe('MARK')
  })

  it('is case-insensitive when highlighting', () => {
    render(<HighlightText text="Hello World" searchTerm="hello" />)
    expect(screen.getByText('Hello').tagName).toBe('MARK')
  })

  it('highlights multiple occurrences', () => {
    render(<HighlightText text="ab ab ab" searchTerm="ab" />)
    const marks = document.querySelectorAll('mark')
    expect(marks.length).toBe(3)
  })

  it('escapes special regex characters in search term', () => {
    render(<HighlightText text="price: $5.00" searchTerm="$5.00" />)
    expect(screen.getByText('$5.00').tagName).toBe('MARK')
  })
})
