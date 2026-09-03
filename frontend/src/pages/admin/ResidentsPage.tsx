import { useEffect, useState } from 'react'
import { residentsApi } from '../../api/endpoints'
import type { PageResponse, Resident } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { SearchInput } from '../../components/SearchInput'
import { Pagination } from '../../components/Pagination'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'

const EMPTY: PageResponse<Resident> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  { name: 'fullName', label: 'Full name', required: true, full: true },
  { name: 'phone', label: 'Phone', required: true },
  { name: 'email', label: 'Email' },
  { name: 'propertyId', label: 'Property ID', type: 'number' },
  {
    name: 'residentType',
    label: 'Resident type',
    type: 'select',
    required: true,
    options: [
      { value: 'OWNER', label: 'Owner' },
      { value: 'TENANT', label: 'Tenant' },
      { value: 'LANDLORD', label: 'Landlord' },
    ],
  },
  { name: 'emergencyContact', label: 'Emergency contact', full: true },
]

export function ResidentsPage() {
  const [result, setResult] = useState<PageResponse<Resident>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Resident | null>(null)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)
  const { openProperty } = useEntityDetail()

  async function load() {
    setLoading(true)
    setResult(await residentsApi.list({ q: debouncedQuery || undefined, page, size: 20 }))
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
    const body = {
      fullName: values.fullName as string,
      phone: values.phone as string,
      email: (values.email as string) || null,
      propertyId: values.propertyId ? Number(values.propertyId) : null,
      residentType: values.residentType as Resident['residentType'],
      emergencyContact: (values.emergencyContact as string) || null,
    }
    if (editing) {
      await residentsApi.update(editing.id, body)
    } else {
      await residentsApi.create(body)
    }
    setModalOpen(false)
    setEditing(null)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Residents</h1>
          <p className="page-subtitle">Owner/tenant profiles linked to properties (spec §1).</p>
        </div>
        <button className="btn btn-primary" onClick={() => { setEditing(null); setModalOpen(true) }}>
          + New resident
        </button>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search residents by name, phone, email, type…" />

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(r) => r.id}
        emptyMessage={query ? 'No residents match your search.' : 'No residents yet.'}
        columns={[
          { key: 'fullName', label: 'Name' },
          { key: 'phone', label: 'Phone' },
          { key: 'email', label: 'Email' },
          { key: 'residentType', label: 'Type', render: (r) => <StatusBadge value={r.residentType} /> },
          {
            key: 'propertyId',
            label: 'Property',
            render: (r) =>
              r.propertyId ? (
                <button type="button" className="link-button" onClick={() => openProperty(r.propertyId!)}>
                  {r.propertyHouseNumber ?? `Property #${r.propertyId}`}
                </button>
              ) : (
                <span className="muted">—</span>
              ),
          },
          { key: 'status', label: 'Status', render: (r) => <StatusBadge value={r.status} /> },
        ]}
        actions={(r) => (
          <button className="btn btn-sm" onClick={() => { setEditing(r); setModalOpen(true) }}>
            Edit
          </button>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal
          title={editing ? 'Edit resident' : 'New resident'}
          fields={FIELDS}
          initial={editing ?? {}}
          onSubmit={handleSubmit}
          onClose={() => { setModalOpen(false); setEditing(null) }}
        />
      )}
    </div>
  )
}
