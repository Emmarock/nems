import { useEffect, useState } from 'react'
import { gatesApi } from '../../api/endpoints'
import type { Gate, PageResponse } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'

const EMPTY: PageResponse<Gate> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  { name: 'name', label: 'Gate name', required: true },
  { name: 'code', label: 'Code', required: true },
  { name: 'location', label: 'Location', full: true },
  {
    name: 'type',
    label: 'Type',
    type: 'select',
    required: true,
    options: [
      { value: 'VEHICLE', label: 'Vehicle' },
      { value: 'PEDESTRIAN', label: 'Pedestrian' },
    ],
  },
]

export function GatesPage() {
  const [result, setResult] = useState<PageResponse<Gate>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const { openGate } = useEntityDetail()

  async function load() {
    setLoading(true)
    setResult(await gatesApi.list({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function handleSubmit(values: Record<string, unknown>) {
    await gatesApi.create({
      name: values.name as string,
      code: values.code as string,
      location: (values.location as string) || null,
      type: values.type as Gate['type'],
    })
    setModalOpen(false)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Gates</h1>
          <p className="page-subtitle">Register the estate's physical gates (spec Phase 3 §2).</p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + New gate
        </button>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(g) => g.id}
        emptyMessage="No gates registered yet."
        columns={[
          {
            key: 'name',
            label: 'Name',
            render: (g) => (
              <button type="button" className="link-button" onClick={() => openGate(g.id)}>
                {g.name}
              </button>
            ),
          },
          { key: 'code', label: 'Code' },
          { key: 'location', label: 'Location' },
          { key: 'type', label: 'Type', render: (g) => <StatusBadge value={g.type} /> },
          { key: 'status', label: 'Status', render: (g) => <StatusBadge value={g.status} /> },
        ]}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal title="New gate" fields={FIELDS} onSubmit={handleSubmit} onClose={() => setModalOpen(false)} />
      )}
    </div>
  )
}
