import { useEffect, useState, type FormEvent } from 'react'
import { qrTagsApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { QrTag, QrTagKind } from '../api/types'
import { QrScannerModal } from './QrScannerModal'

interface Props {
  kind: QrTagKind
  subjectId: number
  /** e.g. the plate number or house number, for the title. */
  subjectLabel: string
  onClose: () => void
  onLinked?: (tag: QrTag) => void
}

/**
 * Links a pre-printed QR tag sticker to a vehicle or property: type the code printed under the QR
 * (V-000123) or scan the sticker with the camera. The vehicle/property's previous pass stops working.
 */
export function LinkQrTagModal({ kind, subjectId, subjectLabel, onClose, onLinked }: Props) {
  const [current, setCurrent] = useState<QrTag | null | undefined>(undefined)
  const [code, setCode] = useState('')
  const [scanning, setScanning] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [linked, setLinked] = useState<QrTag | null>(null)
  const prefix = kind === 'VEHICLE' ? 'V' : 'P'
  const noun = kind === 'VEHICLE' ? 'vehicle' : 'property'

  useEffect(() => {
    qrTagsApi.current(kind, subjectId).then(setCurrent).catch(() => setCurrent(null))
  }, [kind, subjectId])

  async function link(value: string) {
    if (!value.trim()) return
    setSubmitting(true)
    setError(null)
    try {
      const tag = await qrTagsApi.assign(kind, subjectId, value.trim())
      setLinked(tag)
      onLinked?.(tag)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    link(code)
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 440 }}>
        <div className="modal-header">
          <h2>Link QR tag — {subjectLabel}</h2>
          <button type="button" className="btn btn-sm" onClick={onClose}>
            ✕
          </button>
        </div>
        {linked ? (
          <>
            <div className="modal-body">
              <p style={{ color: 'var(--color-success)', margin: 0 }}>
                Tag <strong>{linked.code}</strong> is now this {noun}'s pass. Stick it on, and it works at the gate straight away.
              </p>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-primary" onClick={onClose}>
                Done
              </button>
            </div>
          </>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              <p className="muted" style={{ marginTop: 0 }}>
                {current === undefined
                  ? 'Checking the current pass…'
                  : current
                    ? <>Currently linked to tag <strong>{current.code}</strong>.</>
                    : 'No printed tag linked yet.'}
              </p>
              {error && <p className="error-text">{error}</p>}
              <div className="form-field">
                <label htmlFor="qrTagCode">Code printed under the QR</label>
                <div style={{ display: 'flex', gap: 6 }}>
                  <input
                    id="qrTagCode"
                    value={code}
                    placeholder={`e.g. ${prefix}-000123`}
                    autoFocus
                    autoComplete="off"
                    onChange={(e) => setCode(e.target.value)}
                    style={{ flex: 1, textTransform: 'uppercase' }}
                  />
                  <button type="button" className="btn btn-sm" onClick={() => setScanning(true)}>
                    Scan tag
                  </button>
                </div>
              </div>
              <p className="muted" style={{ fontSize: 12, margin: '8px 0 0' }}>
                Linking replaces this {noun}'s current pass{current ? ` (tag ${current.code})` : ''}: the old QR stops working at the gate.
                Each tag can only ever be linked once.
              </p>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn" onClick={onClose}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={submitting || !code.trim()}>
                {submitting ? 'Linking…' : 'Link tag'}
              </button>
            </div>
          </form>
        )}
      </div>
      {scanning && (
        <QrScannerModal
          title="Scan the QR tag"
          onClose={() => setScanning(false)}
          onDetected={(text) => {
            setScanning(false)
            link(text)
          }}
        />
      )}
    </div>
  )
}
