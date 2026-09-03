import { useEffect, useState } from 'react'
import { complaintsApi } from '../../api/endpoints'
import type { Complaint, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'

const EMPTY: PageResponse<Complaint> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  {
    name: 'category',
    label: 'Category',
    type: 'select',
    required: true,
    full: true,
    options: [
      { value: 'ELECTRICITY', label: 'Electricity' },
      { value: 'WATER', label: 'Water' },
      { value: 'SECURITY', label: 'Security' },
      { value: 'WASTE_DISPOSAL', label: 'Waste disposal' },
      { value: 'ROAD', label: 'Road' },
      { value: 'DRAINAGE', label: 'Drainage' },
      { value: 'STREETLIGHT', label: 'Streetlight' },
      { value: 'GENERAL', label: 'General' },
    ],
  },
  { name: 'description', label: 'Description', type: 'textarea', required: true, full: true },
  {
    name: 'priority',
    label: 'Priority',
    type: 'select',
    options: [
      { value: 'LOW', label: 'Low' },
      { value: 'MEDIUM', label: 'Medium' },
      { value: 'HIGH', label: 'High' },
    ],
  },
]

export function ComplaintsPage() {
  const [result, setResult] = useState<PageResponse<Complaint>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)

  async function load() {
    setLoading(true)
    setResult(await complaintsApi.mine({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function handleSubmit(values: Record<string, unknown>) {
    await complaintsApi.create({
      category: values.category as string,
      description: values.description as string,
      priority: (values.priority as string) || undefined,
    })
    setModalOpen(false)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>My Complaints</h1>
          <p className="page-subtitle">Report electricity, water, security and other issues (spec §7).</p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + File complaint
        </button>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(c) => c.id}
        emptyMessage="You haven't filed any complaints."
        columns={[
          { key: 'category', label: 'Category', render: (c) => <StatusBadge value={c.category} /> },
          { key: 'description', label: 'Description' },
          { key: 'priority', label: 'Priority', render: (c) => <StatusBadge value={c.priority} /> },
          { key: 'status', label: 'Status', render: (c) => <StatusBadge value={c.status} /> },
          { key: 'createdAt', label: 'Filed', render: (c) => new Date(c.createdAt).toLocaleString() },
        ]}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal title="File a complaint" fields={FIELDS} onSubmit={handleSubmit} onClose={() => setModalOpen(false)} />
      )}
    </div>
  )
}
