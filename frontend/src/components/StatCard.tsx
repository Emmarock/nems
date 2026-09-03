import type { ReactNode } from 'react'

export type StatTone = 'primary' | 'success' | 'danger' | 'warning' | 'violet' | 'teal' | 'magenta'

interface StatCardProps {
  label: string
  value: string | number
  icon?: ReactNode
  tone?: StatTone
  onClick?: () => void
}

export function StatCard({ label, value, icon, tone = 'primary', onClick }: StatCardProps) {
  const body = (
    <>
      {icon && <div className={`stat-icon stat-icon-${tone}`}>{icon}</div>}
      <div className="stat-card-body">
        <div className="label">{label}</div>
        <div className="value">{value}</div>
      </div>
    </>
  )

  if (onClick) {
    return (
      <button type="button" className="stat-card stat-card-clickable" onClick={onClick}>
        {body}
      </button>
    )
  }

  return <div className="stat-card">{body}</div>
}
