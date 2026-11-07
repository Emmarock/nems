import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { faceSearchApi } from '../../api/endpoints'
import type { FaceCandidate, FaceSearchResult } from '../../api/types'
import { StatusBadge } from '../../components/StatusBadge'
import { apiErrorMessage } from '../../api/client'
import { useFaceSearchEnabled } from '../../hooks/useFaceSearchEnabled'
import { LoadingState } from '../../components/LoadingState'

/** Rekognition's inline image limit. */
const MAX_BYTES = 5 * 1024 * 1024

function readAsDataUri(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

/**
 * Admin-only: upload a still from CCTV footage and see which registered workers or household
 * members each face in it may belong to. The upload isn't stored; every search is audit-logged
 * (FaceSearchService). Results are leads to verify, never proof on their own.
 */
export function FaceSearchPage() {
  const enabled = useFaceSearchEnabled()
  const [image, setImage] = useState<string | null>(null)
  const [note, setNote] = useState('')
  const [searching, setSearching] = useState(false)
  const [result, setResult] = useState<FaceSearchResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleFile(file: File | undefined) {
    setError(null)
    setResult(null)
    if (!file) return
    if (!file.type.startsWith('image/')) {
      setError('Please choose an image (JPEG or PNG). For video, pause on a clear frame and take a screenshot.')
      return
    }
    if (file.size > MAX_BYTES) {
      setError('That image is over 5MB. Take a screenshot of the frame instead, or crop it.')
      return
    }
    setImage(await readAsDataUri(file))
  }

  async function handleSearch() {
    if (!image) return
    setSearching(true)
    setError(null)
    setResult(null)
    try {
      setResult(await faceSearchApi.search({ image, note: note.trim() || undefined }))
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSearching(false)
    }
  }

  // Switched off for this estate: the page doesn't exist as far as the UI is concerned.
  if (enabled === null) return <LoadingState />
  if (!enabled) return <Navigate to="/" replace />

  return (
    <div>
      <div className="page-header" data-tour="/face-search">
        <div>
          <h1>CCTV Face Search</h1>
          <p className="page-subtitle">
            Upload a still from CCTV footage to check it against registered workers and household members.
          </p>
        </div>
      </div>


      <div className="card" style={{ marginBottom: 16, borderLeft: '4px solid var(--color-warning, #d97706)' }}>
        <strong>A match is a lead, not proof.</strong>
        <p className="muted" style={{ marginBottom: 0 }}>
          Face matching makes mistakes, especially on blurry, dark or angled footage. Always compare the photos yourself
          and verify through other evidence before acting, and hand serious matters to the police. Searches are logged
          with your name and note; the uploaded image is not stored.
        </p>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <label>
            <div style={{ fontWeight: 600, marginBottom: 4 }}>CCTV still</div>
            <input type="file" accept="image/*" onChange={(e) => handleFile(e.target.files?.[0])} />
          </label>
          {image && (
            <img src={image} alt="Uploaded CCTV still" style={{ maxWidth: '100%', maxHeight: 320, objectFit: 'contain', borderRadius: 8 }} />
          )}
          <label>
            <div style={{ fontWeight: 600, marginBottom: 4 }}>Incident note (recommended)</div>
            <input
              type="text"
              value={note}
              maxLength={500}
              placeholder="e.g. Break-in at No. 12, 2 Nov, ~11pm"
              onChange={(e) => setNote(e.target.value)}
              style={{ width: '100%' }}
            />
          </label>
          <div>
            <button className="btn btn-primary" disabled={!image || searching} onClick={handleSearch}>
              {searching ? 'Searching…' : 'Search faces'}
            </button>
          </div>
        </div>
      </div>

      {error && <p className="error-text">{error}</p>}

      {result && result.facesFound === 0 && (
        <div className="empty-state">No faces could be found in this image. Try a clearer frame or a closer crop.</div>
      )}

      {result?.faces.map((face, i) => (
        <section key={i} className="card" style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
            <div style={{ textAlign: 'center' }}>
              <img src={face.faceImage} alt={`Face ${i + 1}`} style={{ width: 120, height: 120, objectFit: 'cover', borderRadius: 8 }} />
              <div className="muted" style={{ fontSize: 12 }}>Face {i + 1} from footage</div>
            </div>
            <div style={{ flex: '1 1 300px' }}>
              {face.candidates.length === 0 ? (
                <p className="muted">No registered worker or household member matched this face.</p>
              ) : (
                face.candidates.map((c) => <CandidateRow key={`${c.subjectType}-${c.subjectId}`} c={c} />)
              )}
            </div>
          </div>
        </section>
      ))}
    </div>
  )
}

function CandidateRow({ c }: { c: FaceCandidate }) {
  const kind = c.subjectType === 'WORKER' ? 'Worker' : 'Household member'
  return (
    <div style={{ display: 'flex', gap: 12, padding: '8px 0', borderBottom: '1px solid var(--color-border)' }}>
      {c.photo ? (
        <img src={c.photo} alt="" style={{ width: 72, height: 72, objectFit: 'cover', borderRadius: 8 }} />
      ) : (
        <div className="avatar-thumb avatar-thumb-empty" style={{ width: 72, height: 72 }} />
      )}
      <div style={{ flex: 1 }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <strong>{c.fullName}</strong>
          <span className="badge">{c.similarity.toFixed(1)}% similar</span>
          <StatusBadge value={c.status} />
        </div>
        <div className="muted" style={{ fontSize: 13 }}>
          {[kind, c.role, c.phone].filter(Boolean).join(' · ')}
        </div>
        {(c.sponsorName || c.propertyHouseNumber) && (
          <div className="muted" style={{ fontSize: 13 }}>
            Registered by {c.sponsorName ?? 'unknown resident'}
            {c.sponsorPhone ? ` (${c.sponsorPhone})` : ''}
            {c.propertyHouseNumber ? ` · House ${c.propertyHouseNumber}` : ''}
          </div>
        )}
      </div>
    </div>
  )
}
