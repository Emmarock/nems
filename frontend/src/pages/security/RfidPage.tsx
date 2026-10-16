import { useEffect, useState } from 'react'
import { rfidApi, rfidRequestsApi } from '../../api/endpoints'
import type { PageResponse, RfidRequest, RfidTag } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { IssueRfidModal } from '../../components/IssueRfidModal'
import { IssueRfidRequestModal } from '../../components/IssueRfidRequestModal'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'

const EMPTY: PageResponse<RfidTag> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
const EMPTY_REQUESTS: PageResponse<RfidRequest> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function RfidPage() {
  const { openResident, openWorker, openVisitor } = useEntityDetail()
  const [result, setResult] = useState<PageResponse<RfidTag>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [pending, setPending] = useState<PageResponse<RfidRequest>>(EMPTY_REQUESTS)
  const [pendingLoading, setPendingLoading] = useState(true)
  const [issuingRequest, setIssuingRequest] = useState<RfidRequest | null>(null)

  async function load() {
    setLoading(true)
    setResult(await rfidApi.list({ page, size: 20 }))
    setLoading(false)
  }

  async function loadPending() {
    setPendingLoading(true)
    setPending(await rfidRequestsApi.pending({ size: 50 }))
    setPendingLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  useEffect(() => {
    loadPending()
  }, [])

  async function revoke(id: number) {
    await rfidApi.revoke(id)
    await load()
  }

  async function afterIssueFromRequest() {
    await Promise.all([loadPending(), load()])
  }

  return (
    <div>
      <div className="page-header" data-tour="/security/rfid">
        <div>
          <h1>RFID Tags</h1>
          <p className="page-subtitle">Physical credentials for residents/long-term workers.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Issue tag
        </button>
      </div>

      <div className="section-title">Pending RFID requests</div>
      <p className="page-subtitle" style={{ marginTop: -8 }}>
        Visitors or workers without a smartphone — issue a physical tag when they arrive at the gate.
      </p>
      <DataTable
        loading={pendingLoading}
        rows={pending.content}
        rowKey={(r) => r.id}
        emptyMessage="No pending RFID requests."
        columns={[
          { key: 'subjectType', label: 'Type', render: (r) => <StatusBadge value={r.subjectType} /> },
          { key: 'subjectName', label: 'Name', render: (r) => r.subjectName ?? <span className="muted">—</span> },
          { key: 'subjectPhone', label: 'Phone', render: (r) => r.subjectPhone ?? <span className="muted">—</span> },
          {
            key: 'requestedByResidentName',
            label: 'Requested by',
            render: (r) => r.requestedByResidentName ?? <span className="muted">—</span>,
          },
          { key: 'createdAt', label: 'Requested', render: (r) => new Date(r.createdAt).toLocaleString() },
        ]}
        actions={(r) => (
          <button className="btn btn-sm btn-primary" onClick={() => setIssuingRequest(r)}>
            Issue tag
          </button>
        )}
      />

      <div className="section-title" style={{ marginTop: 32 }}>
        RFID Tags
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
            key: 'assignedVisitorId',
            label: 'Visitor',
            render: (t) =>
              t.assignedVisitorId ? (
                <button type="button" className="link-button" onClick={() => openVisitor(t.assignedVisitorId!)}>
                  {t.assignedVisitorName ?? `Visitor #${t.assignedVisitorId}`}
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

      {issuingRequest && (
        <IssueRfidRequestModal
          request={issuingRequest}
          onClose={() => setIssuingRequest(null)}
          onIssued={afterIssueFromRequest}
        />
      )}
    </div>
  )
}
