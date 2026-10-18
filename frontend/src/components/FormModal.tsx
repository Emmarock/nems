import { useEffect, useRef, useState, type FormEvent } from 'react'
import { fileToCompressedDataUrl } from '../utils/imageResize'
import { useDebouncedValue } from '../hooks/useDebouncedValue'

export interface FieldOption {
  value: string
  label: string
}

export interface FieldConfig {
  name: string
  label: string
  type?: 'text' | 'number' | 'select' | 'date' | 'datetime-local' | 'textarea' | 'checkbox' | 'image' | 'search'
  options?: FieldOption[]
  required?: boolean
  full?: boolean
  step?: string
  /** type: 'search' only - looks up matches as the user types; the selected option's `value`
   * is stored in form state under `name`, same as any other field. */
  searchFn?: (query: string) => Promise<FieldOption[]>
  searchPlaceholder?: string
  /** type: 'search' only - key in `values` holding the already-selected option's display label
   * (e.g. editing a property whose owner is already set - `values.ownerName`), so the field
   * shows a name instead of a bare id when the modal opens. */
  labelField?: string
}

/** type: 'search' field - debounced lookup via field.searchFn, picks one result into form state. */
function SearchSelectField({
  field,
  value,
  initialLabel,
  onSelect,
}: {
  field: FieldConfig
  value: unknown
  initialLabel: string
  onSelect: (value: string | null) => void
}) {
  const [query, setQuery] = useState(initialLabel)
  const [results, setResults] = useState<FieldOption[]>([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const debouncedQuery = useDebouncedValue(query)
  const containerRef = useRef<HTMLDivElement>(null)
  // Once a selection is made (or the field starts pre-filled), the query text IS the selection's
  // label - re-searching on every keystroke of an unchanged label would be wasted calls and a
  // flickering dropdown the user never asked to reopen.
  const selectedLabelRef = useRef(initialLabel)

  useEffect(() => {
    if (!open || debouncedQuery.trim().length < 2 || debouncedQuery === selectedLabelRef.current) {
      setResults([])
      return
    }
    let cancelled = false
    setLoading(true)
    field
      .searchFn!(debouncedQuery.trim())
      .then((found) => {
        if (!cancelled) setResults(found)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, open])

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  function pick(option: FieldOption) {
    selectedLabelRef.current = option.label
    setQuery(option.label)
    setOpen(false)
    onSelect(option.value)
  }

  function clear() {
    selectedLabelRef.current = ''
    setQuery('')
    setResults([])
    onSelect(null)
  }

  return (
    <div ref={containerRef} style={{ position: 'relative' }}>
      <div style={{ display: 'flex', gap: 6 }}>
        <input
          id={field.name}
          type="text"
          placeholder={field.searchPlaceholder ?? 'Type to search…'}
          required={field.required && !value}
          value={query}
          onChange={(e) => {
            setQuery(e.target.value)
            setOpen(true)
            if (value) onSelect(null) // typing again invalidates the previous selection
          }}
          onFocus={() => setOpen(true)}
        />
        {value ? (
          <button type="button" className="btn btn-sm" onClick={clear}>
            Clear
          </button>
        ) : null}
      </div>
      {open && (loading || results.length > 0) && (
        <div
          className="search-select-dropdown"
          style={{
            position: 'absolute',
            top: '100%',
            left: 0,
            right: 0,
            zIndex: 20,
            background: 'var(--color-surface, #fff)',
            border: '1px solid var(--color-border)',
            borderRadius: 8,
            marginTop: 4,
            maxHeight: 220,
            overflowY: 'auto',
            boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
          }}
        >
          {loading ? (
            <div style={{ padding: '8px 12px', fontSize: 13 }} className="muted">
              Searching…
            </div>
          ) : (
            results.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() => pick(opt)}
                style={{
                  display: 'block',
                  width: '100%',
                  textAlign: 'left',
                  padding: '8px 12px',
                  border: 'none',
                  background: 'none',
                  cursor: 'pointer',
                  fontSize: 13,
                }}
              >
                {opt.label}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  )
}

interface FormModalProps {
  title: string
  fields: FieldConfig[]
  initial?: object
  submitLabel?: string
  onSubmit: (values: Record<string, unknown>) => Promise<void>
  onClose: () => void
}

export function FormModal({ title, fields, initial = {}, submitLabel = 'Save', onSubmit, onClose }: FormModalProps) {
  const [values, setValues] = useState<Record<string, unknown>>(initial as Record<string, unknown>)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [imageBusy, setImageBusy] = useState<string | null>(null)

  function setField(name: string, value: unknown) {
    setValues((prev) => ({ ...prev, [name]: value }))
  }

  async function handleImagePick(name: string, file: File | undefined) {
    if (!file) return
    setImageBusy(name)
    setError(null)
    try {
      const dataUrl = await fileToCompressedDataUrl(file)
      setField(name, dataUrl)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not process the selected image')
    } finally {
      setImageBusy(null)
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await onSubmit(values)
    } catch (err) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        (err instanceof Error ? err.message : 'Something went wrong')
      setError(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <h2>{title}</h2>
            <button type="button" className="btn btn-sm" onClick={onClose}>
              ✕
            </button>
          </div>
          <div className="modal-body">
            {error && <p className="error-text">{error}</p>}
            <div className="form-grid">
              {fields.map((field) => (
                <div key={field.name} className={`form-field ${field.full ? 'full' : ''}`}>
                  {field.type === 'checkbox' ? (
                    <label className="checkbox-field">
                      <input
                        type="checkbox"
                        checked={Boolean(values[field.name] ?? false)}
                        onChange={(e) => setField(field.name, e.target.checked)}
                      />
                      {field.label}
                    </label>
                  ) : field.type === 'image' ? (
                    <>
                      <label htmlFor={field.name}>{field.label}</label>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        {typeof values[field.name] === 'string' && (
                          <img
                            src={values[field.name] as string}
                            alt=""
                            style={{ width: 48, height: 48, borderRadius: 8, objectFit: 'cover', border: '1px solid var(--color-border)' }}
                          />
                        )}
                        <input
                          id={field.name}
                          type="file"
                          accept="image/*"
                          required={field.required && !values[field.name]}
                          onChange={(e) => handleImagePick(field.name, e.target.files?.[0])}
                        />
                      </div>
                      {imageBusy === field.name && <span className="muted" style={{ fontSize: 12 }}>Processing image…</span>}
                    </>
                  ) : field.type === 'search' ? (
                    <>
                      <label htmlFor={field.name}>{field.label}</label>
                      <SearchSelectField
                        field={field}
                        value={values[field.name]}
                        initialLabel={
                          (field.labelField ? (initial as Record<string, unknown>)[field.labelField] : undefined) as
                            | string
                            | undefined ?? ''
                        }
                        onSelect={(selected) => setField(field.name, selected)}
                      />
                    </>
                  ) : (
                    <>
                      <label htmlFor={field.name}>{field.label}</label>
                      {field.type === 'select' ? (
                        <select
                          id={field.name}
                          required={field.required}
                          value={String(values[field.name] ?? '')}
                          onChange={(e) => setField(field.name, e.target.value)}
                        >
                          <option value="" disabled>
                            Select…
                          </option>
                          {field.options?.map((opt) => (
                            <option key={opt.value} value={opt.value}>
                              {opt.label}
                            </option>
                          ))}
                        </select>
                      ) : field.type === 'textarea' ? (
                        <textarea
                          id={field.name}
                          required={field.required}
                          value={String(values[field.name] ?? '')}
                          onChange={(e) => setField(field.name, e.target.value)}
                        />
                      ) : (
                        <input
                          id={field.name}
                          type={field.type ?? 'text'}
                          step={field.step}
                          required={field.required}
                          value={String(values[field.name] ?? '')}
                          onChange={(e) =>
                            setField(
                              field.name,
                              field.type === 'number' ? e.target.valueAsNumber : e.target.value,
                            )
                          }
                        />
                      )}
                    </>
                  )}
                </div>
              ))}
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting || imageBusy !== null}>
              {submitting ? 'Saving…' : submitLabel}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
