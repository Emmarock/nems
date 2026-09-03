import { useEffect, useState } from 'react'
import { visitorsApi } from '../../api/endpoints'
import type { PageResponse, Visitor } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { QrCodeModal } from '../../components/QrCodeModal'
import { buildScanUrl } from '../../utils/scanUrl'

const EMPTY: PageResponse<Visitor> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  { name: 'name', label: 'Visitor name', required: true, full: true },
  { name: 'phone', label: 'Phone', required: true },
  { name: 'vehiclePlate', label: 'Vehicle plate (optional)' },
  { name: 'validFrom', label: 'From', type: 'datetime-local', required: true },
  { name: 'validUntil', label: 'Until', type: 'datetime-local', required: true },
]

export function VisitorsPage() {
  const [result, setResult] = useState<PageResponse<Visitor>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [qrVisitor, setQrVisitor] = useState<Visitor | null>(null)

  async function load() {
    setLoading(true)
    setResult(await visitorsApi.mine({ page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function handleSubmit(values: Record<string, unknown>) {
    await visitorsApi.create({
      name: values.name as string,
      phone: values.phone as string,
      vehiclePlate: (values.vehiclePlate as string) || undefined,
      validFrom: new Date(values.validFrom as string).toISOString(),
      validUntil: new Date(values.validUntil as string).toISOString(),
    })
    setModalOpen(false)
    await load()
  }

  async function cancel(id: number) {
    await visitorsApi.cancel(id)
    await load()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>My Visitors</h1>
          <p className="page-subtitle">Create QR visitor passes for guests (spec §9).</p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Create visitor pass
        </button>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(v) => v.id}
        emptyMessage="No visitor passes yet."
        columns={[
          { key: 'name', label: 'Name' },
          { key: 'phone', label: 'Phone' },
          { key: 'vehiclePlate', label: 'Vehicle' },
          { key: 'validFrom', label: 'From', render: (v) => new Date(v.validFrom).toLocaleString() },
          { key: 'validUntil', label: 'Until', render: (v) => new Date(v.validUntil).toLocaleString() },
          { key: 'status', label: 'Status', render: (v) => <StatusBadge value={v.status} /> },
        ]}
        actions={(v) => (
          <div style={{ display: 'flex', gap: 6 }}>
            {v.status === 'ACTIVE' && (
              <>
                <button className="btn btn-sm" onClick={() => setQrVisitor(v)}>
                  View QR pass
                </button>
                <button className="btn btn-sm" onClick={() => cancel(v.id)}>
                  Cancel
                </button>
              </>
            )}
          </div>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal title="Create visitor pass" fields={FIELDS} onSubmit={handleSubmit} onClose={() => setModalOpen(false)} />
      )}

      {qrVisitor && (
        <QrCodeModal
          title={`Visitor pass — ${qrVisitor.name}`}
          subtitle={`Valid until ${new Date(qrVisitor.validUntil).toLocaleString()}`}
          value={buildScanUrl('visitor', qrVisitor.qrToken)}
          fileName={`visitor-pass-${qrVisitor.name.replace(/\s+/g, '-').toLowerCase()}`}
          onClose={() => setQrVisitor(null)}
        />
      )}
    </div>
  )
}
