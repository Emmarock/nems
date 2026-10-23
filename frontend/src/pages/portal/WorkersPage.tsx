import { useEffect, useState } from 'react'
import { rfidRequestsApi, workersApi } from '../../api/endpoints'
import type { PageResponse, RfidRequest, Worker } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { QrCodeModal } from '../../components/QrCodeModal'
import { RfidRequestAction } from '../../components/RfidRequestAction'
import { buildScanUrl } from '../../utils/scanUrl'

const EMPTY: PageResponse<Worker> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

const FIELDS: FieldConfig[] = [
  { name: 'photo', label: 'Worker photo', type: 'image', full: true },
  { name: 'fullName', label: 'Worker full name', required: true, full: true },
  { name: 'phone', label: 'Phone', required: true },
  { name: 'nationalId', label: 'National / Work ID' },
  { name: 'contractorName', label: 'Contractor / company', required: true, full: true },
  { name: 'workType', label: 'Type of work', required: true, full: true },
  {
    name: 'workerCategory',
    label: 'Worker category',
    type: 'select',
    required: true,
    options: [
      { value: 'DOMESTIC_STAFF', label: 'Domestic staff' },
      { value: 'NON_DOMESTIC_STAFF', label: 'Non-domestic staff' },
    ],
  },
  { name: 'startDate', label: 'Start date', type: 'date', required: true },
  { name: 'expectedEndDate', label: 'Expected end date', type: 'date', required: true },
]

export function WorkersPage() {
  const [result, setResult] = useState<PageResponse<Worker>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Worker | null>(null)
  const [qrWorker, setQrWorker] = useState<Worker | null>(null)
  const [rfidRequests, setRfidRequests] = useState<RfidRequest[]>([])

  async function load() {
    setLoading(true)
    const [workers, requests] = await Promise.all([
      workersApi.mine({ page, size: 20 }),
      rfidRequestsApi.mine({ size: 100 }),
    ])
    setResult(workers)
    setRfidRequests(requests.content)
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function handleSubmit(values: Record<string, unknown>) {
    await workersApi.request({
      fullName: values.fullName as string,
      phone: values.phone as string,
      nationalId: (values.nationalId as string) || undefined,
      contractorName: values.contractorName as string,
      workType: values.workType as string,
      workerCategory: values.workerCategory as Worker['workerCategory'],
      startDate: values.startDate as string,
      expectedEndDate: values.expectedEndDate as string,
      photo: (values.photo as string) || undefined,
    })
    setModalOpen(false)
    await load()
  }

  async function handleEditSubmit(values: Record<string, unknown>) {
    if (!editing) return
    await workersApi.update(editing.id, {
      fullName: values.fullName as string,
      phone: values.phone as string,
      nationalId: (values.nationalId as string) || undefined,
      contractorName: values.contractorName as string,
      workType: values.workType as string,
      workerCategory: values.workerCategory as Worker['workerCategory'],
      startDate: values.startDate as string,
      expectedEndDate: values.expectedEndDate as string,
      photo: (values.photo as string) || undefined,
    })
    setEditing(null)
    await load()
  }

  return (
    <div>
      <div className="page-header" data-tour="/portal/workers">
        <div>
          <h1>My Workers</h1>
          <p className="page-subtitle">
            Request access for contractors/labourers while the estate is under development.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Request worker access
        </button>
      </div>

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(w) => w.id}
        emptyMessage="No worker access requests yet."
        columns={[
          {
            key: 'photo',
            label: '',
            render: (w) =>
              w.photo ? (
                <img src={w.photo} alt="" className="avatar-thumb" />
              ) : (
                <div className="avatar-thumb avatar-thumb-empty" />
              ),
          },
          { key: 'fullName', label: 'Worker' },
          { key: 'contractorName', label: 'Contractor' },
          { key: 'workType', label: 'Work type' },
          { key: 'workerCategory', label: 'Category', render: (w) => <StatusBadge value={w.workerCategory} /> },
          { key: 'startDate', label: 'Start' },
          { key: 'expectedEndDate', label: 'Expected end' },
          { key: 'status', label: 'Status', render: (w) => <StatusBadge value={w.status} /> },
        ]}
        actions={(w) => (
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            <button className="btn btn-sm" onClick={() => setEditing(w)}>
              Edit
            </button>
            {w.qrToken ? (
              <>
                <button className="btn btn-sm" onClick={() => setQrWorker(w)}>
                  View QR pass
                </button>
                <RfidRequestAction
                  subjectType="WORKER"
                  subjectId={w.id}
                  subjectName={w.fullName}
                  requests={rfidRequests}
                  onChanged={load}
                />
              </>
            ) : (
              <span className="muted">Pending approval</span>
            )}
          </div>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && (
        <FormModal
          title="Request worker access"
          fields={FIELDS}
          onSubmit={handleSubmit}
          onClose={() => setModalOpen(false)}
        />
      )}

      {editing && (
        <FormModal
          title={`Edit worker — ${editing.fullName}`}
          fields={FIELDS}
          initial={editing}
          submitLabel="Save changes"
          onSubmit={handleEditSubmit}
          onClose={() => setEditing(null)}
        />
      )}

      {qrWorker && qrWorker.qrToken && (
        <QrCodeModal
          title={`Worker pass — ${qrWorker.fullName}`}
          subtitle={`${qrWorker.contractorName} · valid through ${qrWorker.expectedEndDate}`}
          value={buildScanUrl('worker', qrWorker.qrToken)}
          fileName={`worker-pass-${qrWorker.fullName.replace(/\s+/g, '-').toLowerCase()}`}
          onClose={() => setQrWorker(null)}
        />
      )}
    </div>
  )
}
