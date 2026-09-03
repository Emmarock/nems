import { useState, type FormEvent } from 'react'
import { fileToCompressedDataUrl } from '../utils/imageResize'

export interface FieldOption {
  value: string
  label: string
}

export interface FieldConfig {
  name: string
  label: string
  type?: 'text' | 'number' | 'select' | 'date' | 'datetime-local' | 'textarea' | 'checkbox' | 'image'
  options?: FieldOption[]
  required?: boolean
  full?: boolean
  step?: string
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
