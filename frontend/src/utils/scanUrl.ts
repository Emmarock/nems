export type ScanKind = 'visitor' | 'worker' | 'resident' | 'property' | 'vehicle'

/** The URL a QR pass encodes — opens straight into the security scan/destination-confirmation page. */
export function buildScanUrl(kind: ScanKind, qrToken: string): string {
  return `${window.location.origin}/scan/${kind}/${qrToken}`
}

/**
 * Parses whatever the camera (or a paste) hands back. A pass QR encodes a full scan URL, so a
 * successful decode gives us both the kind and the token; anything else (a bare token, a QR
 * from an older/different source) is treated as a raw token with an unknown kind, and the
 * officer picks the kind manually.
 */
export function parseScanValue(raw: string): { kind: ScanKind | null; qrToken: string } {
  const text = raw.trim()
  try {
    const url = new URL(text)
    const match = url.pathname.match(/\/scan\/(visitor|worker|resident|property|vehicle)\/([^/]+)/)
    if (match) {
      return { kind: match[1] as ScanKind, qrToken: decodeURIComponent(match[2]) }
    }
  } catch {
    // not a URL — fall through to raw-token handling
  }
  return { kind: null, qrToken: text }
}
