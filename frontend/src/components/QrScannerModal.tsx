import { useEffect, useRef, useState } from 'react'
import jsQR from 'jsqr'

interface QrScannerModalProps {
  title?: string
  onDetected: (text: string) => void
  onClose: () => void
}

/**
 * Opens the device camera and scans for a QR code (worker/visitor/resident pass) using jsQR
 * against video frames drawn to a hidden canvas — no server round-trip, decoding happens
 * entirely in the browser. Calls onDetected once with the raw decoded text and stops the
 * camera; the caller decides what to do with it (parse a scan URL, treat as a raw token, etc).
 */
export function QrScannerModal({ title = 'Scan QR pass', onDetected, onClose }: QrScannerModalProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const frameRef = useRef<number | null>(null)
  const detectedRef = useRef(false)
  const [error, setError] = useState<string | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let cancelled = false

    async function start() {
      if (!navigator.mediaDevices?.getUserMedia) {
        setError('Camera access is not supported in this browser.')
        return
      }
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
        })
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop())
          return
        }
        streamRef.current = stream
        if (videoRef.current) {
          videoRef.current.srcObject = stream
          await videoRef.current.play()
        }
        setReady(true)
        scanLoop()
      } catch {
        if (!cancelled) setError('Could not access the camera. Check permissions and try again.')
      }
    }

    function scanLoop() {
      const video = videoRef.current
      const canvas = canvasRef.current
      if (!video || !canvas || detectedRef.current) return

      if (video.readyState === video.HAVE_ENOUGH_DATA) {
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        const ctx = canvas.getContext('2d', { willReadFrequently: true })
        if (ctx) {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
          const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
          const code = jsQR(imageData.data, imageData.width, imageData.height, {
            inversionAttempts: 'dontInvert',
          })
          if (code && code.data) {
            detectedRef.current = true
            stopCamera()
            onDetected(code.data)
            return
          }
        }
      }
      frameRef.current = requestAnimationFrame(scanLoop)
    }

    function stopCamera() {
      if (frameRef.current) cancelAnimationFrame(frameRef.current)
      streamRef.current?.getTracks().forEach((t) => t.stop())
      streamRef.current = null
    }

    start()

    return () => {
      cancelled = true
      stopCamera()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 420 }}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button type="button" className="btn btn-sm" onClick={onClose}>
            ✕
          </button>
        </div>
        <div className="modal-body" style={{ textAlign: 'center' }}>
          {error ? (
            <p className="error-text">{error}</p>
          ) : (
            <>
              <div className="qr-scanner-viewport">
                <video ref={videoRef} playsInline muted className="qr-scanner-video" />
                <div className="qr-scanner-frame" />
              </div>
              <p className="muted" style={{ fontSize: 12, marginTop: 10 }}>
                {ready ? 'Point the camera at a worker, visitor, or resident QR pass.' : 'Starting camera…'}
              </p>
            </>
          )}
          <canvas ref={canvasRef} style={{ display: 'none' }} />
        </div>
      </div>
    </div>
  )
}
