import { useEffect, useState } from 'react'
import { meApi } from '../../api/endpoints'
import type { MeDashboard } from '../../api/types'
import { StatCard } from '../../components/StatCard'
import { LoadingState } from '../../components/LoadingState'
import { QrCodeModal } from '../../components/QrCodeModal'
import { CarIcon, CoinsIcon, HouseIcon, UserBadgeIcon } from '../../components/icons'
import { apiErrorMessage } from '../../api/client'
import { buildScanUrl } from '../../utils/scanUrl'

export function DashboardPage() {
  const [dashboard, setDashboard] = useState<MeDashboard | null>(null)
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState<string | null>(null)
  const [qrOpen, setQrOpen] = useState(false)
  const [accessPassToken, setAccessPassToken] = useState<string | null>(null)
  const [vehicleQr, setVehicleQr] = useState<{ plate: string; token: string } | null>(null)

  async function load() {
    setDashboard(await meApi.dashboard())
  }

  useEffect(() => {
    load()
  }, [])

  async function openAccessPass() {
    if (!accessPassToken) {
      setAccessPassToken(await meApi.accessPass())
    }
    setQrOpen(true)
  }

  async function viewVehicleQr(id: number, plate: string) {
    setError(null)
    try {
      const token = await meApi.vehicleAccessPass(id)
      setVehicleQr({ plate, token })
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function payNow() {
    if (!dashboard) return
    setPaying(true)
    setError(null)
    try {
      const result = await meApi.payOutstanding(dashboard.account.outstanding)
      window.location.href = result.redirectUrl
    } catch (err) {
      setError(apiErrorMessage(err))
      setPaying(false)
    }
  }

  if (!dashboard) {
    return <LoadingState />
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Welcome, {dashboard.resident.fullName}</h1>
          <p className="page-subtitle">Resident portal (spec §6).</p>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}

      <div className="stat-grid">
        <StatCard
          label="Outstanding"
          value={`₦${dashboard.account.outstanding.toLocaleString()}`}
          icon={<CoinsIcon />}
          tone="danger"
        />
        <StatCard
          label="Total due"
          value={`₦${dashboard.account.totalDue.toLocaleString()}`}
          icon={<CoinsIcon />}
          tone="primary"
        />
        <StatCard
          label="Total paid"
          value={`₦${dashboard.account.totalPaid.toLocaleString()}`}
          icon={<CoinsIcon />}
          tone="success"
        />
        <StatCard label="Vehicles" value={dashboard.vehicles.length} icon={<CarIcon />} tone="teal" />
      </div>

      {dashboard.account.outstanding > 0 && (
        <div className="card" style={{ marginBottom: 20 }}>
          <p style={{ margin: '0 0 10px' }}>
            You have an outstanding balance of <strong>₦{dashboard.account.outstanding.toLocaleString()}</strong>.
          </p>
          <button className="btn btn-primary" onClick={payNow} disabled={paying}>
            {paying ? 'Redirecting…' : 'Pay now'}
          </button>
        </div>
      )}

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="media-row">
          <div className="media-icon media-icon-magenta">
            <UserBadgeIcon />
          </div>
          <div style={{ flex: 1 }}>
            <div className="media-row-title">My Access Pass</div>
            <div className="muted">Present this QR at the gate to be scanned as yourself.</div>
          </div>
          <button className="btn btn-primary" onClick={openAccessPass}>
            View QR pass
          </button>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="section-title" style={{ marginTop: 0 }}>Property</div>
        {dashboard.property ? (
          <div className="media-row">
            <div className="media-icon media-icon-violet">
              <HouseIcon />
            </div>
            <div>
              <div className="media-row-title">{dashboard.property.houseNumber}</div>
              <div className="muted">
                {dashboard.property.block}, {dashboard.property.plot}
              </div>
            </div>
          </div>
        ) : (
          <p className="muted">No property linked yet.</p>
        )}
      </div>

      <div className="card">
        <div className="section-title" style={{ marginTop: 0 }}>Vehicles</div>
        {dashboard.vehicles.length === 0 ? (
          <p className="muted">No vehicles registered yet.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {dashboard.vehicles.map((v) => (
              <div className="media-row" key={v.id}>
                <div className="media-icon media-icon-teal">
                  <CarIcon />
                </div>
                <div style={{ flex: 1 }}>
                  <div className="media-row-title">{v.plateNumber}</div>
                  <div className="muted">
                    {v.make} {v.model} {v.colour && `· ${v.colour}`}
                  </div>
                </div>
                <button className="btn btn-sm" onClick={() => viewVehicleQr(v.id, v.plateNumber)}>
                  View QR
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {qrOpen && accessPassToken && (
        <QrCodeModal
          title="My Access Pass"
          subtitle={dashboard.resident.fullName}
          value={buildScanUrl('resident', accessPassToken)}
          fileName="my-access-pass"
          onClose={() => setQrOpen(false)}
        />
      )}

      {vehicleQr && (
        <QrCodeModal
          title="Vehicle QR code"
          subtitle={vehicleQr.plate}
          value={buildScanUrl('vehicle', vehicleQr.token)}
          fileName={`vehicle-${vehicleQr.plate}`}
          helpText="Present this QR code at the gate. Security will scan it to identify your vehicle before granting access."
          onClose={() => setVehicleQr(null)}
        />
      )}
    </div>
  )
}
