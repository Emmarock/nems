const POSITIVE = new Set(['ACTIVE', 'SUCCESS', 'RESOLVED', 'CLOSED', 'APPROVED', 'OCCUPIED', 'COMPLETED', 'ISSUED', 'READ'])
const NEGATIVE = new Set(['SUSPENDED', 'FAILED', 'CANCELLED', 'EXPIRED', 'DISABLED', 'REVOKED', 'LOST'])
const WARNING = new Set(['PENDING', 'OPEN', 'ASSIGNED', 'IN_PROGRESS', 'VACANT', 'UNDER_CONSTRUCTION', 'UNREAD'])

export function StatusBadge({ value }: { value: string | null | undefined }) {
  if (!value) return <span className="badge">—</span>
  let cls = 'badge'
  if (POSITIVE.has(value)) cls = 'badge badge-success'
  else if (NEGATIVE.has(value)) cls = 'badge badge-danger'
  else if (WARNING.has(value)) cls = 'badge badge-warning'
  else cls = 'badge badge-primary'
  return <span className={cls}>{value.replace(/_/g, ' ')}</span>
}
