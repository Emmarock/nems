import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { accessEventsApi, residentsApi, securityDashboardApi, vehiclesApi, visitorsApi, workersApi } from '../../api/endpoints'
import type { AccessEvent, PageResponse, SecurityDashboard } from '../../api/types'
import { StatCard } from '../../components/StatCard'
import { AlertIcon, CameraIcon, CarIcon, PersonBadgeIcon, WrenchIcon } from '../../components/icons'
import { DataTable } from '../../components/DataTable'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { LoadingState } from '../../components/LoadingState'
import { QrScannerModal } from '../../components/QrScannerModal'
import { apiErrorMessage } from '../../api/client'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { parseScanValue, type ScanKind } from '../../utils/scanUrl'

const EMPTY_EVENTS: PageResponse<AccessEvent> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function SecurityDashboardPage() {
  const [dashboard, setDashboard] = useState<SecurityDashboard | null>(null)
  const [events, setEvents] = useState<PageResponse<AccessEvent>>(EMPTY_EVENTS)
  const [eventsLoading, setEventsLoading] = useState(true)
  const [eventsPage, setEventsPage] = useState(0)
  const { openGate, openResident, openVisitor, openWorker, openVehicle } = useEntityDetail()
  const navigate = useNavigate()

  function openSubject(e: AccessEvent) {
    if (e.subjectType === 'RESIDENT') openResident(e.subjectId)
    else if (e.subjectType === 'VISITOR') openVisitor(e.subjectId)
    else if (e.subjectType === 'WORKER') openWorker(e.subjectId)
    else openVehicle(e.subjectId)
  }

  async function loadDashboard() {
    setDashboard(await securityDashboardApi.get())
  }

  async function loadEvents() {
    setEventsLoading(true)
    setEvents(await accessEventsApi.list({ page: eventsPage, size: 20 }))
    setEventsLoading(false)
  }

  useEffect(() => {
    loadDashboard()
  }, [])

  useEffect(() => {
    loadEvents()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventsPage])

  async function refreshAfterCheckIn() {
    await Promise.all([loadDashboard(), loadEvents()])
  }

  if (!dashboard) {
    return <LoadingState />
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Security Dashboard</h1>
          <p className="page-subtitle">Live operational view for the gate team (spec Phase 3 §1/§4/§5).</p>
        </div>
      </div>

      <div className="stat-grid">
        <StatCard label="Visitors active" value={dashboard.visitorsActive} icon={<PersonBadgeIcon />} tone="magenta" />
        <StatCard label="Workers on site" value={dashboard.workersOnSite} icon={<WrenchIcon />} tone="warning" />
        <StatCard
          label="Registered vehicles"
          value={dashboard.registeredVehicles}
          icon={<CarIcon />}
          tone="teal"
          onClick={() => navigate('/vehicles')}
        />
        <StatCard
          label="Accounts in arrears"
          value={dashboard.accountsInArrears}
          icon={<AlertIcon />}
          tone="danger"
          onClick={() => navigate('/security/arrears')}
        />
      </div>

      <GateCheckIn onDone={refreshAfterCheckIn} />

      <div className="section-title">Recent access events</div>
      <DataTable
        loading={eventsLoading}
        rows={events.content}
        rowKey={(e) => e.id}
        emptyMessage="No access events logged yet."
        columns={[
          { key: 'subjectType', label: 'Subject', render: (e) => <StatusBadge value={e.subjectType} /> },
          {
            key: 'subjectId',
            label: 'Name',
            render: (e) => (
              <button type="button" className="link-button" onClick={() => openSubject(e)}>
                {e.subjectName ?? `#${e.subjectId}`}
              </button>
            ),
          },
          {
            key: 'subjectPhone',
            label: 'Phone',
            render: (e) => e.subjectPhone ?? <span className="muted">—</span>,
          },
          {
            key: 'vehicle',
            label: 'Vehicle',
            render: (e) =>
              e.vehiclePlateNumber ? (
                <span>
                  {e.vehiclePlateNumber}
                  {(e.vehicleMake || e.vehicleModel) && ` — ${[e.vehicleMake, e.vehicleModel].filter(Boolean).join(' ')}`}
                  {e.vehicleColour && ` (${e.vehicleColour})`}
                </span>
              ) : (
                <span className="muted">—</span>
              ),
          },
          {
            key: 'vehicleResidentName',
            label: 'Owner',
            render: (e) => (e.subjectType === 'VEHICLE' ? e.vehicleResidentName ?? <span className="muted">—</span> : null),
          },
          { key: 'direction', label: 'Direction', render: (e) => <StatusBadge value={e.direction} /> },
          {
            key: 'gateId',
            label: 'Gate',
            render: (e) =>
              e.gateId ? (
                <button type="button" className="link-button" onClick={() => openGate(e.gateId!)}>
                  Gate #{e.gateId}
                </button>
              ) : (
                <span className="muted">—</span>
              ),
          },
          { key: 'occurredAt', label: 'When', render: (e) => new Date(e.occurredAt).toLocaleString() },
          {
            key: 'expectedCheckoutAt',
            label: 'Expected checkout',
            render: (e) => (e.expectedCheckoutAt ? new Date(e.expectedCheckoutAt).toLocaleString() : <span className="muted">—</span>),
          },
          { key: 'flagReason', label: 'Flag', render: (e) => (e.flagReason ? <StatusBadge value={e.flagReason} /> : '—') },
        ]}
      />

      <Pagination page={events.page} totalPages={events.totalPages} totalElements={events.totalElements} onPageChange={setEventsPage} />
    </div>
  )
}

function GateCheckIn({ onDone }: { onDone: () => Promise<void> }) {
  const [kind, setKind] = useState<ScanKind>('visitor')
  const [qrToken, setQrToken] = useState('')
  const [gateId, setGateId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [scannerOpen, setScannerOpen] = useState(false)
  const { openVisitor, openWorker } = useEntityDetail()

  function apiFor(k: ScanKind) {
    return k === 'visitor' ? visitorsApi : k === 'worker' ? workersApi : k === 'vehicle' ? vehiclesApi : residentsApi
  }

  function nameOf(result: { name?: string; fullName?: string; plateNumber?: string }) {
    return result.name ?? result.fullName ?? result.plateNumber ?? ''
  }

  async function act(action: 'checkin' | 'checkout', e?: FormEvent) {
    e?.preventDefault()
    if (!qrToken) {
      setError('Scan or paste a QR token first')
      return
    }
    setSubmitting(true)
    setError(null)
    setMessage(null)
    try {
      const gate = gateId ? Number(gateId) : undefined
      const api = apiFor(kind)
      const result = action === 'checkin' ? await api.checkIn(qrToken, gate) : await api.checkOut(qrToken, gate)
      setMessage(
        `${action === 'checkin' ? 'Checked in' : 'Checked out'} ${kind}: ${nameOf(result)} (status ${result.status})`,
      )
      // On check-in, pull up the full profile (photo included, for a worker) rather than leaving
      // the officer with just this one-line summary to go on.
      if (action === 'checkin' && kind === 'visitor') openVisitor(result.id)
      if (action === 'checkin' && kind === 'worker') openWorker(result.id)
      setQrToken('')
      await onDone()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  function onScanned(raw: string) {
    const parsed = parseScanValue(raw)
    if (parsed.kind) setKind(parsed.kind)
    setQrToken(parsed.qrToken)
    setScannerOpen(false)
  }

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <div className="section-title" style={{ marginTop: 0 }}>Gate check-in</div>
      <form onSubmit={(e) => act('checkin', e)} className="form-grid">
        <div className="form-field">
          <label>Pass type</label>
          <select value={kind} onChange={(e) => setKind(e.target.value as ScanKind)}>
            <option value="visitor">Visitor</option>
            <option value="worker">Worker</option>
            <option value="resident">Resident</option>
            <option value="vehicle">Vehicle</option>
          </select>
        </div>
        <div className="form-field">
          <label>Gate ID (optional)</label>
          <input value={gateId} onChange={(e) => setGateId(e.target.value)} />
        </div>
        <div className="form-field full">
          <label>QR token</label>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              required
              value={qrToken}
              onChange={(e) => setQrToken(e.target.value)}
              placeholder="Scan or paste QR token"
              style={{ flex: 1 }}
            />
            <button
              type="button"
              className="icon-btn"
              title="Scan with camera"
              aria-label="Scan with camera"
              onClick={() => setScannerOpen(true)}
            >
              <CameraIcon />
            </button>
          </div>
        </div>
        {error && <p className="error-text" style={{ gridColumn: '1 / -1' }}>{error}</p>}
        {message && <p style={{ gridColumn: '1 / -1', color: 'var(--color-success)' }}>{message}</p>}
        <div className="form-field full" style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-primary" type="submit" disabled={submitting}>
            {submitting ? 'Checking in…' : 'Check in'}
          </button>
          <button className="btn" type="button" disabled={submitting} onClick={() => act('checkout')}>
            {submitting ? 'Checking out…' : 'Check out'}
          </button>
        </div>
      </form>

      {scannerOpen && (
        <QrScannerModal
          title="Scan visitor, worker, resident, or vehicle pass"
          onDetected={onScanned}
          onClose={() => setScannerOpen(false)}
        />
      )}
    </div>
  )
}
