import { useEffect, useState } from 'react'
import { workersApi } from '../api/endpoints'
import type { Worker } from '../api/types'
import { useDebouncedValue } from '../hooks/useDebouncedValue'

interface WorkerPickerProps {
  sponsorResidentId: number
  value: Worker | null
  onChange: (worker: Worker | null) => void
}

/** Searchable worker lookup, scoped to one resident's own workers - mirrors ResidentPicker, but
 * a worker only makes sense in the context of the resident sponsoring them (see RfidService.issue,
 * which enforces the same worker-belongs-to-resident relationship server-side). */
export function WorkerPicker({ sponsorResidentId, value, onChange }: WorkerPickerProps) {
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebouncedValue(query)
  const [results, setResults] = useState<Worker[]>([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    workersApi.listBySponsor(sponsorResidentId, { q: debouncedQuery || undefined, size: 8 }).then((r) => {
      if (!cancelled) {
        setResults(r.content)
        setLoading(false)
      }
    })
    return () => {
      cancelled = true
    }
  }, [sponsorResidentId, debouncedQuery])

  if (value) {
    return (
      <div className="resident-picker-selected">
        <div>
          <strong>{value.fullName}</strong>
          <span className="muted"> · {value.contractorName}</span>
        </div>
        <button type="button" className="btn btn-sm" onClick={() => onChange(null)}>
          Change
        </button>
      </div>
    )
  }

  return (
    <div className="resident-picker">
      <input
        type="text"
        placeholder="Search this resident's workers…"
        value={query}
        onChange={(e) => {
          setQuery(e.target.value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
      />
      {open && (
        <div className="resident-picker-dropdown">
          {loading && <div className="resident-picker-empty">Searching…</div>}
          {!loading && results.length === 0 && (
            <div className="resident-picker-empty">This resident has no workers on record.</div>
          )}
          {!loading &&
            results.map((w) => (
              <button
                type="button"
                key={w.id}
                className="resident-picker-option"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onChange(w)
                  setQuery('')
                  setOpen(false)
                }}
              >
                <span>{w.fullName}</span>
                <span className="muted">{w.contractorName}</span>
              </button>
            ))}
        </div>
      )}
    </div>
  )
}
