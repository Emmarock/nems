import { useEffect, useState } from 'react'
import { rfidApi } from '../../api/endpoints'
import type { PageResponse, RfidTag } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'

const EMPTY: PageResponse<RfidTag> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  { name: 'tagId', label: 'Tag ID', required: true, full: true },
  { name: 'assignedResidentId', label: 'Resident ID', type: 'number' },
  { name: 'assignedWorkerId', label: 'Worker ID', type: 'number' },
  { name: 'vehicleId', label: 'Vehicle ID', type: 'number' },
]

export function RfidPage() {
  const [result, setResult] = useState<PageResponse<RfidTag>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)

  async function load() {
    setLoading(true)
    setResult(await rfidApi.list({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function handleSubmit(values: Record<string, unknown>) {
    await rfidApi.issue({
      tagId: values.tagId as string,
      assignedResidentId: values.assignedResidentId ? Number(values.assignedResidentId) : undefined,
      assignedWorkerId: values.assignedWorkerId ? Number(values.assignedWorkerId) : undefined,
      vehicleId: values.vehicleId ? Number(values.vehicleId) : undefined,
    })
    setModalOpen(false)
    await load()
  }

  async function revoke(id: number) {
    await rfidApi.revoke(id)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>RFID Tags</h1>
          <p className="page-subtitle">Physical credentials for residents/long-term workers (spec Phase 3 §6).</p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Issue tag
        </button>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(t) => t.id}
        emptyMessage="No RFID tags issued yet."
        columns={[
          { key: 'tagId', label: 'Tag ID' },
          { key: 'assignedResidentId', label: 'Resident ID' },
          { key: 'assignedWorkerId', label: 'Worker ID' },
          { key: 'vehicleId', label: 'Vehicle ID' },
          { key: 'status', label: 'Status', render: (t) => <StatusBadge value={t.status} /> },
        ]}
        actions={(t) =>
          t.status === 'ACTIVE' ? (
            <button className="btn btn-sm" onClick={() => revoke(t.id)}>
              Revoke
            </button>
          ) : null
        }
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal title="Issue RFID tag" fields={FIELDS} onSubmit={handleSubmit} onClose={() => setModalOpen(false)} />
      )}
    </div>
  )
}
