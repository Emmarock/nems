import { useEffect, useState, type FormEvent } from 'react'
import { qrTagsApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { QrTagBatch, QrTagKind } from '../../api/types'
import { DataTable } from '../../components/DataTable'
import { downloadBlob } from '../../utils/downloadBlob'

/**
 * Print QR tag stickers in bulk ahead of time, then link each sticker to a vehicle or property from
 * the Vehicles / Properties pages ("Link QR tag") by typing its printed code or scanning it.
 */
export function QrTagsPage() {
  const [batches, setBatches] = useState<QrTagBatch[]>([])
  const [loading, setLoading] = useState(true)
  const [kind, setKind] = useState<QrTagKind>('VEHICLE')
  const [count, setCount] = useState('40')
  const [generating, setGenerating] = useState(false)
  const [reprinting, setReprinting] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    try {
      setBatches(await qrTagsApi.batches())
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function handleGenerate(e: FormEvent) {
    e.preventDefault()
    setGenerating(true)
    setError(null)
    try {
      const { blob, batchNo } = await qrTagsApi.generate(kind, Number(count))
      downloadBlob(blob, `qr-tags-batch-${batchNo}.pdf`)
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setGenerating(false)
    }
  }

  async function handleReprint(batchNo: number) {
    setReprinting(batchNo)
    setError(null)
    try {
      downloadBlob(await qrTagsApi.batchPdf(batchNo), `qr-tags-batch-${batchNo}.pdf`)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setReprinting(null)
    }
  }

  const countValid = Number(count) >= 1 && Number(count) <= 1000

  return (
    <div>
      <div className="page-header" data-tour="/qr-tags">
        <div>
          <h1>QR Tags</h1>
          <p className="page-subtitle">
            Print QR stickers in bulk, then link each one to a vehicle or property with “Link QR tag” on the Vehicles or
            Properties page.
          </p>
        </div>
      </div>

      <form className="card" style={{ marginBottom: 16 }} onSubmit={handleGenerate}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div className="form-field">
            <label htmlFor="qrTagKind">Tags for</label>
            <select id="qrTagKind" value={kind} onChange={(e) => setKind(e.target.value as QrTagKind)}>
              <option value="VEHICLE">Vehicles (V-…)</option>
              <option value="PROPERTY">Properties (P-…)</option>
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="qrTagCount">How many (20 per page)</label>
            <input id="qrTagCount" type="number" min={1} max={1000} value={count} onChange={(e) => setCount(e.target.value)} style={{ width: 120 }} />
          </div>
          <button type="submit" className="btn btn-primary" disabled={generating || !countValid}>
            {generating ? 'Generating…' : 'Generate & download PDF'}
          </button>
        </div>
        {error && <p className="error-text" style={{ marginBottom: 0 }}>{error}</p>}
      </form>

      <DataTable
        loading={loading}
        rows={batches}
        rowKey={(b) => b.batchNo}
        emptyMessage="No QR tags generated yet."
        columns={[
          { key: 'batchNo', label: 'Batch' },
          { key: 'kind', label: 'For', render: (b) => (b.kind === 'VEHICLE' ? 'Vehicles' : 'Properties') },
          { key: 'firstCode', label: 'Codes', render: (b) => `${b.firstCode} – ${b.lastCode}` },
          { key: 'total', label: 'Tags' },
          { key: 'linked', label: 'Linked', render: (b) => `${b.linked} of ${b.total}` },
          { key: 'createdAt', label: 'Generated', render: (b) => new Date(b.createdAt).toLocaleString() },
        ]}
        actions={(b) => (
          <button className="btn btn-sm" disabled={reprinting === b.batchNo} onClick={() => handleReprint(b.batchNo)}>
            {reprinting === b.batchNo ? 'Preparing…' : 'Reprint PDF'}
          </button>
        )}
      />
    </div>
  )
}
