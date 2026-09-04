import { useEffect, useState } from 'react'
import { residentsApi } from '../api/endpoints'
import type { Resident } from '../api/types'
import { useDebouncedValue } from '../hooks/useDebouncedValue'

interface ResidentPickerProps {
  value: Resident | null
  onChange: (resident: Resident | null) => void
  required?: boolean
}

/** Searchable resident lookup - lets an admin pick a resident by name instead of typing a raw
 * numeric id (the source of a real bug: a blank/wrong id silently created a RESIDENT-role
 * account with nothing to link to, which then 400'd the moment that resident tried to log in). */
export function ResidentPicker({ value, onChange, required }: ResidentPickerProps) {
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebouncedValue(query)
  const [results, setResults] = useState<Resident[]>([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (debouncedQuery.trim().length < 2) {
      setResults([])
      return
    }
    let cancelled = false
    setLoading(true)
    residentsApi.list({ q: debouncedQuery, size: 8 }).then((r) => {
      if (!cancelled) {
        setResults(r.content)
        setLoading(false)
      }
    })
    return () => {
      cancelled = true
    }
  }, [debouncedQuery])

  if (value) {
    return (
      <div className="resident-picker-selected">
        <div>
          <strong>{value.fullName}</strong>
          {value.propertyHouseNumber && <span className="muted"> · {value.propertyHouseNumber}</span>}
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
        placeholder="Search residents by name…"
        value={query}
        required={required}
        onChange={(e) => {
          setQuery(e.target.value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
      />
      {open && debouncedQuery.trim().length >= 2 && (
        <div className="resident-picker-dropdown">
          {loading && <div className="resident-picker-empty">Searching…</div>}
          {!loading && results.length === 0 && <div className="resident-picker-empty">No residents match.</div>}
          {!loading &&
            results.map((r) => (
              <button
                type="button"
                key={r.id}
                className="resident-picker-option"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onChange(r)
                  setQuery('')
                  setOpen(false)
                }}
              >
                <span>{r.fullName}</span>
                {r.propertyHouseNumber && <span className="muted">{r.propertyHouseNumber}</span>}
              </button>
            ))}
        </div>
      )}
    </div>
  )
}
