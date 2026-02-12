/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Generic Data Table Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

interface Column<T> {
  key: string
  header: string
  render: (item: T) => React.ReactNode
  align?: 'left' | 'center' | 'right'
}

interface DataTableProps<T> {
  data: T[]
  columns: Column<T>[]
  currentUser?: string | null
  highlightField?: keyof T
  emptyMessage?: string
}

export function DataTable<T>({
  data,
  columns,
  currentUser,
  highlightField,
  emptyMessage = 'No data available',
}: DataTableProps<T>) {
  if (data.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        {emptyMessage}
      </div>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr className="bg-gray-700 text-white">
            {columns.map((column) => (
              <th
                key={column.key}
                className={`px-4 py-2 border border-white text-${column.align || 'left'}`}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((item, index) => {
            const isCurrentUser =
              currentUser && highlightField && item[highlightField] === currentUser
            const rowClass = isCurrentUser
              ? 'bg-[var(--bg-khaki)]'
              : index % 2 === 0
                ? 'bg-[var(--color-gray-light)]'
                : 'bg-[var(--color-gray-medium)]'

            return (
              <tr key={index} className={`${rowClass} border border-white`}>
                {columns.map((column) => (
                  <td
                    key={column.key}
                    className={`px-4 py-2 border border-white text-${column.align || 'left'}`}
                  >
                    {column.render(item)}
                  </td>
                ))}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
