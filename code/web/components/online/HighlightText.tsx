/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Highlight Text Component (for search results)
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

interface HighlightTextProps {
  text: string
  searchTerm: string
}

export function HighlightText({ text, searchTerm }: HighlightTextProps) {
  if (!searchTerm) {
    return <span>{text}</span>
  }

  const parts = text.split(new RegExp(`(${searchTerm})`, 'gi'))

  return (
    <span>
      {parts.map((part, index) =>
        part.toLowerCase() === searchTerm.toLowerCase() ? (
          <mark key={index} className="bg-yellow-300 font-bold">
            {part}
          </mark>
        ) : (
          <span key={index}>{part}</span>
        )
      )}
    </span>
  )
}
