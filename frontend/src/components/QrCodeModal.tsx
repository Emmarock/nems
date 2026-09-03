import { useEffect, useState } from 'react'
import QRCode from 'qrcode'

interface QrCodeModalProps {
  title: string
  subtitle?: string
  value: string
  fileName: string
  helpText?: string
  onClose: () => void
}

/**
 * Shows a scannable QR pass a resident can download or share with their visitor/worker to
 * present at the gate. The QR encodes a URL into the security-only scan page, so any phone
 * camera opening it lands straight on the destination-confirmation screen (spec §9 / Phase 2 §4).
 */
export function QrCodeModal({ title, subtitle, value, fileName, helpText, onClose }: QrCodeModalProps) {
  const [dataUrl, setDataUrl] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [shareSupported, setShareSupported] = useState(false)

  useEffect(() => {
    let cancelled = false
    QRCode.toDataURL(value, { width: 320, margin: 2 })
      .then((url) => {
        if (!cancelled) setDataUrl(url)
      })
      .catch(() => {
        if (!cancelled) setError('Could not generate QR code')
      })
    return () => {
      cancelled = true
    }
  }, [value])

  useEffect(() => {
    setShareSupported(typeof navigator !== 'undefined' && typeof navigator.share === 'function')
  }, [])

  async function handleShare() {
    if (!dataUrl) return
    try {
      const blob = await (await fetch(dataUrl)).blob()
      const file = new File([blob], `${fileName}.png`, { type: 'image/png' })
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        await navigator.share({ files: [file], title, text: subtitle })
        return
      }
      await navigator.share({ title, text: subtitle, url: value })
    } catch {
      // user cancelled the share sheet — nothing to do
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 380 }}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button type="button" className="btn btn-sm" onClick={onClose}>
            ✕
          </button>
        </div>
        <div className="modal-body" style={{ textAlign: 'center' }}>
          {subtitle && <p className="muted" style={{ marginTop: 0 }}>{subtitle}</p>}
          {error && <p className="error-text">{error}</p>}
          {!error && !dataUrl && <p className="muted">Generating QR code…</p>}
          {dataUrl && (
            <img
              src={dataUrl}
              alt="Access pass QR code"
              width={220}
              height={220}
              style={{ borderRadius: 8, border: '1px solid var(--color-border)' }}
            />
          )}
          <p className="muted" style={{ fontSize: 12, marginTop: 12 }}>
            {helpText ??
              'Present this QR code at the gate. Security will scan it to confirm your destination before check-in.'}
          </p>
        </div>
        <div className="modal-footer">
          {dataUrl && (
            <a className="btn" href={dataUrl} download={`${fileName}.png`}>
              Download
            </a>
          )}
          {shareSupported && dataUrl && (
            <button type="button" className="btn btn-primary" onClick={handleShare}>
              Share
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
