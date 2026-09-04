import { useEffect, useState } from 'react'
import { vehiclesApi } from '../../api/endpoints'
import type { PageResponse, Vehicle } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { RegisterVehicleModal } from '../../components/RegisterVehicleModal'
import { StatusBadge } from '../../components/StatusBadge'
import { SearchInput } from '../../components/SearchInput'
import { Pagination } from '../../components/Pagination'
import { QrCodeModal } from '../../components/QrCodeModal'
import { useEntityDetail } from '../../entityDetail/EntityDetailContext'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { buildScanUrl } from '../../utils/scanUrl'

const EMPTY: PageResponse<Vehicle> = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

export function VehiclesPage() {
  const [result, setResult] = useState<PageResponse<Vehicle>>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query)
  const { openResident } = useEntityDetail()
  const [qrVehicle, setQrVehicle] = useState<Vehicle | null>(null)
  const [qrToken, setQrToken] = useState<string | null>(null)

  async function openAccessPass(v: Vehicle) {
    setQrVehicle(v)
    setQrToken(await vehiclesApi.accessPass(v.id))
  }

  async function load() {
    setLoading(true)
    setResult(await vehiclesApi.list({ q: debouncedQuery || undefined, page, size: 20 }))
    setLoading(false)
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, page])

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Vehicles</h1>
          <p className="page-subtitle">Registered ahead of ANPR (spec §3).</p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + Register vehicle
        </button>
      </div>

      <SearchInput value={query} onChange={setQuery} placeholder="Search vehicles by plate, make, model, colour…" />

      <DataTable
        loading={loading}
        rows={result.content}
        rowKey={(v) => v.id}
        emptyMessage={query ? 'No vehicles match your search.' : 'No vehicles registered yet.'}
        columns={[
          { key: 'plateNumber', label: 'Plate' },
          { key: 'make', label: 'Make' },
          { key: 'model', label: 'Model' },
          { key: 'colour', label: 'Colour' },
          {
            key: 'residentId',
            label: 'Resident',
            render: (v) => (
              <button type="button" className="link-button" onClick={() => openResident(v.residentId)}>
                {v.residentName ?? `Resident #${v.residentId}`}
              </button>
            ),
          },
          { key: 'status', label: 'Status', render: (v) => <StatusBadge value={v.status} /> },
        ]}
        actions={(v) => (
          <button className="btn btn-sm" onClick={() => openAccessPass(v)}>
            View QR pass
          </button>
        )}
      />

      <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onPageChange={setPage} />

      {modalOpen && <RegisterVehicleModal onClose={() => setModalOpen(false)} onCreated={load} />}

      {qrVehicle && qrToken && (
        <QrCodeModal
          title={`Vehicle pass — ${qrVehicle.plateNumber}`}
          subtitle={qrVehicle.residentName ?? `Resident #${qrVehicle.residentId}`}
          value={buildScanUrl('vehicle', qrToken)}
          fileName={`vehicle-pass-${qrVehicle.plateNumber.toLowerCase()}`}
          helpText="Scan this at the gate to identify the vehicle and its owner before granting access."
          onClose={() => {
            setQrVehicle(null)
            setQrToken(null)
          }}
        />
      )}
    </div>
  )
}
