import { useEffect, useState } from 'react'
import { faceSearchApi } from '../api/endpoints'

/**
 * Whether CCTV face search is switched on for this estate (server setting
 * NITEL_FACE_MATCH_ENABLED). While it's off, the nav item, the page and every mention of CCTV
 * matching stay hidden. Fetched once per page load and shared; null until known (treat as off).
 */
let cached: Promise<boolean> | null = null

function load(): Promise<boolean> {
  if (!cached) {
    cached = faceSearchApi
      .status()
      .then((s) => s.enabled)
      .catch(() => {
        cached = null // e.g. not signed in yet - ask again next time
        return false
      })
  }
  return cached
}

export function useFaceSearchEnabled(): boolean | null {
  const [enabled, setEnabled] = useState<boolean | null>(null)
  useEffect(() => {
    let live = true
    load().then((v) => {
      if (live) setEnabled(v)
    })
    return () => {
      live = false
    }
  }, [])
  return enabled
}
