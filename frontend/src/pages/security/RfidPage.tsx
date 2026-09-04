import { useEffect, useState } from 'react'
import { rfidApi } from '../../api/endpoints'
import type { PageResponse, RfidTag } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { IssueRfidModal } from '../../components/IssueRfidModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'

const EMPTY: PageResponse<RfidTag> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function RfidPage() {
  const { openResident, openWorker } = useEntityDetail()
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
          {
            key: 'assignedResidentId',
            label: 'Resident',
            render: (t) =>
              t.assignedResidentId ? (
                <button type="button" className="link-button" onClick={() => openResident(t.assignedResidentId!)}>
                  {t.assignedResidentName ?? `Resident #${t.assignedResidentId}`}
                </button>
              ) : (
                <span className="muted">—</span>
              ),
          },
          {
            key: 'assignedWorkerId',
            label: 'Worker',
            render: (t) =>
              t.assignedWorkerId ? (
                <button type="button" className="link-button" onClick={() => openWorker(t.assignedWorkerId!)}>
                  {t.assignedWorkerName ?? `Worker #${t.assignedWorkerId}`}
                </button>
              ) : (
                <span className="muted">—</span>
              ),
          },
          {
            key: 'vehicleId',
            label: 'Vehicle',
            render: (t) => t.vehiclePlateNumber ?? (t.vehicleId ? `#${t.vehicleId}` : <span className="muted">—</span>),
          },
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

      {modalOpen && <IssueRfidModal onClose={() => setModalOpen(false)} onCreated={load} />}
    </div>
  )
}
