/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Generic Data Table Component
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

const ALIGN_CLASSES = {
  left: 'text-left',
  center: 'text-center',
  right: 'text-right',
} as const

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
  keyField?: keyof T
}

export function DataTable<T>({
  data,
  columns,
  currentUser,
  highlightField,
  emptyMessage = 'No data available',
  keyField,
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
                className={`px-4 py-2 border border-white ${ALIGN_CLASSES[column.align || 'left']}`}
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

            const rowKey = keyField ? String(item[keyField]) : index
            return (
              <tr key={rowKey} className={`${rowClass} border border-white`}>
                {columns.map((column) => (
                  <td
                    key={column.key}
                    className={`px-4 py-2 border border-white ${ALIGN_CLASSES[column.align || 'left']}`}
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
