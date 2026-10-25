import type { Role } from '../api/types'

export const ROLE_LABELS: Record<Role, string> = {
  SUPER_ADMIN: 'Super Admin',
  CDA_ADMIN: 'CDA Administrator',
  TREASURER: 'Treasurer',
  FINANCIAL_SECRETARY: 'Financial Secretary',
  SECRETARY: 'Secretary',
  PRO: 'PRO',
  SECURITY: 'Security',
  MAINTENANCE: 'Maintenance',
  RESIDENT: 'Resident',
}

/** Staff roles a complaint can be routed to - RESIDENT excluded, since complaints are filed BY residents, not assigned to them. */
export const ASSIGNABLE_COMPLAINT_ROLES: Role[] = [
  'SUPER_ADMIN',
  'CDA_ADMIN',
  'TREASURER',
  'FINANCIAL_SECRETARY',
  'SECRETARY',
  'PRO',
  'SECURITY',
  'MAINTENANCE',
]
