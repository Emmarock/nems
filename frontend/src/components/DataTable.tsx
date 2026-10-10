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

  function cellValue(col: Column<T>, row: T): ReactNode {
    return col.render ? col.render(row) : String((row as Record<string, unknown>)[col.key] ?? '—')
  }

  // Every DataTable across the app (admin and portal alike) follows the same rough shape: an
  // optional leading thumbnail column (label '' — a worker/visitor/household-member photo), a
  // name-ish column, a status column, a handful of other metadata columns, then actions. Below
  // the mobile breakpoint that shape renders as a proper contact-card list instead of the desktop
  // table (see .mobile-card-list in index.css) — picked out here once so no page has to restate
  // "this is my title column" / "this is my badge column" itself.
  const avatarColumn = columns.find((c) => c.label === '')
  const withoutAvatar = columns.filter((c) => c !== avatarColumn)
  const badgeColumn = withoutAvatar.find((c) => c.key === 'status')
  const withoutBadge = withoutAvatar.filter((c) => c !== badgeColumn)
  const [titleColumn, ...factColumns] = withoutBadge

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
                  {cellValue(col, row)}
                </td>
              ))}
              {actions && <td data-label="Actions">{actions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>

      <ul className="mobile-card-list">
        {rows.map((row) => (
          <li className="mobile-card" key={rowKey(row)}>
            <div className="mobile-card-header">
              {avatarColumn && <div className="mobile-card-avatar">{cellValue(avatarColumn, row)}</div>}
              {titleColumn && <div className="mobile-card-title">{cellValue(titleColumn, row)}</div>}
              {badgeColumn && <div className="mobile-card-badge">{cellValue(badgeColumn, row)}</div>}
            </div>
            {factColumns.length > 0 && (
              <dl className="mobile-card-facts">
                {factColumns.map((col) => (
                  <div className="mobile-card-fact" key={col.key}>
                    <dt>{col.label}</dt>
                    <dd>{cellValue(col, row)}</dd>
                  </div>
                ))}
              </dl>
            )}
            {actions && <div className="mobile-card-actions">{actions(row)}</div>}
          </li>
        ))}
      </ul>
    </div>
  )
}
