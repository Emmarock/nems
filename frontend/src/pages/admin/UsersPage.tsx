import { useEffect, useState } from 'react'
import { usersApi } from '../../api/endpoints'
import type { PageResponse, User } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { SearchInput } from '../../components/SearchInput'
import { Pagination } from '../../components/Pagination'
import { ResetPasswordModal } from '../../components/ResetPasswordModal'
import { BulkCreateResidentAccountsModal } from '../../components/BulkCreateResidentAccountsModal'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'

const EMPTY: PageResponse<User> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  { name: 'fullName', label: 'Full name', required: true, full: true },
  { name: 'email', label: 'Email', required: true },
  { name: 'phone', label: 'Phone (optional, alternative login)' },
  { name: 'password', label: 'Password', required: true },
  {
    name: 'role',
    label: 'Role',
    type: 'select',
    required: true,
    options: [
      { value: 'SUPER_ADMIN', label: 'Super Admin' },
      { value: 'CDA_ADMIN', label: 'CDA Administrator' },
      { value: 'TREASURER', label: 'Treasurer' },
      { value: 'SECRETARY', label: 'Secretary' },
      { value: 'SECURITY', label: 'Security' },
      { value: 'MAINTENANCE', label: 'Maintenance' },
      { value: 'RESIDENT', label: 'Resident' },
    ],
  },
  { name: 'residentId', label: 'Resident ID (if role = Resident)', type: 'number' },
]

export function UsersPage() {
  const [result, setResult] = useState<PageResponse<User>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)
  const [resetTarget, setResetTarget] = useState<User | null>(null)
  const [bulkCreateOpen, setBulkCreateOpen] = useState(false)

  async function load() {
    setLoading(true)
    setResult(await usersApi.list({ q: debouncedQuery || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, page])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery])

  async function handleSubmit(values: Record<string, unknown>) {
    await usersApi.create({
      email: values.email as string,
      phone: (values.phone as string) || undefined,
      password: values.password as string,
      fullName: values.fullName as string,
      role: values.role as string,
      residentId: values.residentId ? Number(values.residentId) : undefined,
    })
    setModalOpen(false)
    await load()
  }

  async function toggleStatus(user: User) {
    await usersApi.setStatus(user.id, user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Users</h1>
          <p className="page-subtitle">Staff and resident login accounts (spec §10, Super Admin only).</p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn" onClick={() => setBulkCreateOpen(true)}>
            Bulk-create resident accounts
          </button>
          <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
            + New user
          </button>
        </div>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search users by name, email, role…" />

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(u) => u.id}
        emptyMessage={query ? 'No users match your search.' : 'No users yet.'}
        columns={[
          { key: 'fullName', label: 'Name' },
          { key: 'email', label: 'Email' },
          { key: 'phone', label: 'Phone', render: (u) => u.phone ?? <span className="muted">—</span> },
          { key: 'role', label: 'Role', render: (u) => <StatusBadge value={u.role} /> },
          { key: 'status', label: 'Status', render: (u) => <StatusBadge value={u.status} /> },
        ]}
        actions={(u) => (
          <div style={{ display: 'flex', gap: 6 }}>
            <button className="btn btn-sm" onClick={() => setResetTarget(u)}>
              Reset password
            </button>
            <button className="btn btn-sm" onClick={() => toggleStatus(u)}>
              {u.status === 'ACTIVE' ? 'Disable' : 'Enable'}
            </button>
          </div>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal title="New user" fields={FIELDS} onSubmit={handleSubmit} onClose={() => setModalOpen(false)} />
      )}

      {resetTarget && (
        <ResetPasswordModal userId={resetTarget.id} userName={resetTarget.fullName} onClose={() => setResetTarget(null)} />
      )}

      {bulkCreateOpen && (
        <BulkCreateResidentAccountsModal onClose={() => setBulkCreateOpen(false)} onDone={load} />
      )}
    </div>
  )
}
