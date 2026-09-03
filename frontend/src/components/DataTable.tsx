import type { ReactNode } from 'react'
import { LoadingState } from './LoadingState'

export interface Column<T> {
  key: string
  label: string
  render?: (row: T) => ReactNode
}

interface DataTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  actions?: (row: T) => ReactNode
  emptyMessage?: string
  loading?: boolean
}

export function DataTable<T extends object>({
  columns,
  rows,
  rowKey,
  actions,
  emptyMessage = 'Nothing here yet.',
  loading = false,
}: DataTableProps<T>) {
  if (loading) {
    return <LoadingState />
  }
  if (rows.length === 0) {
    return <div className="empty-state">{emptyMessage}</div>
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key}>{col.label}</th>
            ))}
            {actions && <th></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((col) => (
                <td key={col.key} data-label={col.label}>
                  {col.render ? col.render(row) : String((row as Record<string, unknown>)[col.key] ?? '—')}
                </td>
              ))}
              {actions && <td data-label="Actions">{actions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
