const POSITIVE = new Set(['ACTIVE', 'SUCCESS', 'RESOLVED', 'CLOSED', 'APPROVED', 'OCCUPIED', 'COMPLETED', 'ISSUED', 'READ'])
const NEGATIVE = new Set([
  'SUSPENDED', 'FAILED', 'CANCELLED', 'EXPIRED', 'DISABLED', 'REVOKED', 'LOST', 'INACTIVE',
  // Gate-scan flagReason values (ResidentService/HouseholdMemberService) - these ride in the same
  // StatusBadge as everything else, so without this they'd render as a neutral blue badge right
  // next to a red one, undercutting the "stop" signal security is supposed to see at a glance.
  'RESIDENT_INACTIVE', 'RESIDENT_SUSPENDED', 'HOUSEHOLD_MEMBER_REVOKED', 'VEHICLE_INACTIVE', 'ACCOUNT_IN_ARREARS',
])
const WARNING = new Set(['PENDING', 'PENDING_APPROVAL', 'OPEN', 'ASSIGNED', 'IN_PROGRESS', 'VACANT', 'UNDER_CONSTRUCTION', 'UNREAD'])

export function StatusBadge({ value }: { value: string | null | undefined }) {
  if (!value) return <span className="badge">—</span>
  let cls = 'badge'
  if (POSITIVE.has(value)) cls = 'badge badge-success'
  else if (NEGATIVE.has(value)) cls = 'badge badge-danger'
  else if (WARNING.has(value)) cls = 'badge badge-warning'
  else cls = 'badge badge-primary'
  return <span className={cls}>{value.replace(/_/g, ' ')}</span>
}
