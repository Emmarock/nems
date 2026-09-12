import { useEffect, useState } from 'react'
import { meApi, stickerRequestsApi } from '../../api/endpoints'
import type { PageResponse, StickerRequest, Vehicle } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { FormModal, type FieldConfig } from '../../components/FormModal'
import { StatusBadge } from '../../components/StatusBadge'
import { LoadingState } from '../../components/LoadingState'
import { QrCodeModal } from '../../components/QrCodeModal'
import { apiErrorMessage } from '../../api/client'
import { buildScanUrl } from '../../utils/scanUrl'

const VEHICLE_FIELDS: FieldConfig[] = [
  { name: 'plateNumber', label: 'Plate number', required: true, full: true },
  { name: 'vehicleType', label: 'Vehicle type' },
  { name: 'make', label: 'Make' },
  { name: 'model', label: 'Model' },
  { name: 'colour', label: 'Colour' },
]

const EMPTY_VEHICLES: PageResponse<Vehicle> = { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }

/** A resident's own vehicles, plus requesting a window sticker per vehicle. A sticker request is
 * only ever created here (no admin equivalent) — once requested it's pay-and-wait (see My
 * Payments): the sticker is issued automatically the moment a treasurer/financial secretary
 * approves the matching payment (see StickerRequestService.onPaymentSucceeded on the backend). */
export function VehiclesPage() {
  const [vehicles, setVehicles] = useState<PageResponse<Vehicle>>(EMPTY_VEHICLES)
  const [stickers, setStickers] = useState<StickerRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [vehicleQr, setVehicleQr] = useState<{ plate: string; token: string } | null>(null)

  async function load() {
    setLoading(true)
    const [v, s] = await Promise.all([meApi.vehicles({ size: 100 }), stickerRequestsApi.mine({ size: 100 })])
    setVehicles(v)
    setStickers(s.content)
    setLoading(false)
  }

  useEffect(() => {
    load()
  }, [])

  async function handleSubmit(values: Record<string, unknown>) {
    await meApi.registerVehicle({
      plateNumber: (values.plateNumber as string).toUpperCase(),
      vehicleType: (values.vehicleType as string) || null,
      make: (values.make as string) || null,
      model: (values.model as string) || null,
      colour: (values.colour as string) || null,
    })
    setModalOpen(false)
    await load()
  }

  async function requestSticker(vehicleId: number) {
    setBusyId(vehicleId)
    setError(null)
    try {
      await stickerRequestsApi.request(vehicleId)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusyId(null)
    }
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

  function stickerFor(vehicleId: number) {
    return stickers.find((s) => s.vehicleId === vehicleId) ?? null
  }

  if (loading && vehicles.content.length === 0) {
    return <LoadingState />
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>My Vehicles</h1>
          <p className="page-subtitle">Register your vehicles and request window stickers for them.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Register vehicle
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}

      <DataTable
        loading={loading}
        rows={vehicles.content}
        rowKey={(v) => v.id}
        emptyMessage="No vehicles registered yet."
        columns={[
          { key: 'plateNumber', label: 'Plate' },
          { key: 'make', label: 'Make' },
          { key: 'model', label: 'Model' },
          { key: 'colour', label: 'Colour' },
          { key: 'status', label: 'Status', render: (v) => <StatusBadge value={v.status} /> },
          {
            key: 'sticker',
            label: 'Sticker',
            render: (v) => {
              const sr = stickerFor(v.id)
              return sr ? <StatusBadge value={sr.status} /> : <span className="muted">Not requested</span>
            },
          },
        ]}
        actions={(v) => {
          const sr = stickerFor(v.id)
          return (
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              <button className="btn btn-sm" onClick={() => viewVehicleQr(v.id, v.plateNumber)}>
                View QR
              </button>
              {!sr && (
                <button
                  className="btn btn-sm btn-primary"
                  disabled={busyId === v.id}
                  onClick={() => requestSticker(v.id)}
                >
                  {busyId === v.id ? 'Requesting…' : 'Request sticker'}
                </button>
              )}
              {sr && sr.status === 'PENDING_PAYMENT' && (
                <span className="muted" style={{ fontSize: 13 }}>
                  Pay {sr.invoiceAmount != null ? `₦${sr.invoiceAmount.toLocaleString()}` : 'the sticker fee'} via My
                  Payments to get this issued
                </span>
              )}
            </div>
          )
        }}
      />

      {modalOpen && (
        <FormModal
          title="Register vehicle"
          fields={VEHICLE_FIELDS}
          onSubmit={handleSubmit}
          onClose={() => setModalOpen(false)}
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
