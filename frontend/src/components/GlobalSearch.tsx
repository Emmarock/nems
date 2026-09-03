import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { propertiesApi, residentsApi, usersApi, vehiclesApi, workersApi } from '../api/endpoints'
import type { Property, Resident, User, Vehicle, Worker } from '../api/types'
import { useEntityDetail } from '../entityDetail/EntityDetailContext'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { LoadingState } from './LoadingState'
import { CarIcon, HouseIcon, SearchIcon, UserBadgeIcon, UsersIcon, WrenchIcon } from './icons'

const GROUP_SIZE = 5

interface Loaded {
  residents: Resident[]
  properties: Property[]
  vehicles: Vehicle[]
  workers: Worker[]
  users: User[]
}

/**
 * Server-backed search across residents/properties/vehicles/workers/users for the admin
 * dashboard. Each keystroke (debounced) fires one small page-size-5 request per entity type —
 * a list that 403s for the current role (e.g. Treasurer has no Users access) is simply omitted
 * from results rather than surfacing an error.
 */
export function GlobalSearch() {
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [results, setResults] = useState<Loaded | null>(null)
  const [loading, setLoading] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const { openResident, openProperty } = useEntityDetail()
  const debouncedQuery = useDebouncedValue(query, 250)

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  useEffect(() => {
    const q = debouncedQuery.trim()
    if (q.length < 1) {
      setResults(null)
      return
    }
    let cancelled = false
    setLoading(true)
    Promise.all([
      residentsApi.list({ q, size: GROUP_SIZE }).catch(() => null),
      propertiesApi.list({ q, size: GROUP_SIZE }).catch(() => null),
      vehiclesApi.list({ q, size: GROUP_SIZE }).catch(() => null),
      workersApi.listAll({ q, size: GROUP_SIZE }).catch(() => null),
      usersApi.list({ q, size: GROUP_SIZE }).catch(() => null),
    ]).then(([residents, properties, vehicles, workers, users]) => {
      if (cancelled) return
      setResults({
        residents: residents?.content ?? [],
        properties: properties?.content ?? [],
        vehicles: vehicles?.content ?? [],
        workers: workers?.content ?? [],
        users: users?.content ?? [],
      })
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [debouncedQuery])

  const totalResults = results
    ? results.residents.length +
      results.properties.length +
      results.vehicles.length +
      results.workers.length +
      results.users.length
    : 0

  function select(action: () => void) {
    action()
    setOpen(false)
    setQuery('')
  }

  return (
    <div className="global-search" ref={containerRef}>
      <div className="global-search-input">
        <SearchIcon />
        <input
          type="text"
          placeholder="Search residents, properties, vehicles, workers, users…"
          value={query}
          onFocus={() => setOpen(true)}
          onChange={(e) => {
            setQuery(e.target.value)
            setOpen(true)
          }}
        />
      </div>

      {open && query.trim().length > 0 && (
        <div className="global-search-dropdown">
          {loading && !results ? (
            <LoadingState />
          ) : totalResults === 0 ? (
            <div className="bell-empty">No matches for "{query}"</div>
          ) : (
            <>
              {results!.residents.length > 0 && (
                <SearchGroup label="Residents" icon={<UsersIcon />}>
                  {results!.residents.map((r) => (
                    <SearchResult
                      key={`r-${r.id}`}
                      title={r.fullName}
                      subtitle={r.phone}
                      onClick={() => select(() => openResident(r.id))}
                    />
                  ))}
                </SearchGroup>
              )}
              {results!.properties.length > 0 && (
                <SearchGroup label="Properties" icon={<HouseIcon />}>
                  {results!.properties.map((p) => (
                    <SearchResult
                      key={`p-${p.id}`}
                      title={p.houseNumber}
                      subtitle={`${p.block}, ${p.plot}`}
                      onClick={() => select(() => openProperty(p.id))}
                    />
                  ))}
                </SearchGroup>
              )}
              {results!.vehicles.length > 0 && (
                <SearchGroup label="Vehicles" icon={<CarIcon />}>
                  {results!.vehicles.map((v) => (
                    <SearchResult
                      key={`v-${v.id}`}
                      title={v.plateNumber}
                      subtitle={[v.make, v.model].filter(Boolean).join(' ') || 'View owner'}
                      onClick={() => select(() => openResident(v.residentId))}
                    />
                  ))}
                </SearchGroup>
              )}
              {results!.workers.length > 0 && (
                <SearchGroup label="Workers" icon={<WrenchIcon />}>
                  {results!.workers.map((w) => (
                    <SearchResult
                      key={`w-${w.id}`}
                      title={w.fullName}
                      subtitle={w.contractorName}
                      onClick={() => select(() => openResident(w.sponsorResidentId))}
                    />
                  ))}
                </SearchGroup>
              )}
              {results!.users.length > 0 && (
                <SearchGroup label="Users" icon={<UserBadgeIcon />}>
                  {results!.users.map((u) => (
                    <SearchResult
                      key={`u-${u.id}`}
                      title={u.fullName}
                      subtitle={u.email}
                      onClick={() => select(() => navigate('/users'))}
                    />
                  ))}
                </SearchGroup>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}

function SearchGroup({ label, icon, children }: { label: string; icon: ReactNode; children: ReactNode }) {
  return (
    <div className="search-group">
      <div className="search-group-label">
        {icon}
        {label}
      </div>
      {children}
    </div>
  )
}

function SearchResult({ title, subtitle, onClick }: { title: string; subtitle?: string; onClick: () => void }) {
  return (
    <button type="button" className="search-result" onClick={onClick}>
      <span className="search-result-title">{title}</span>
      {subtitle && <span className="search-result-subtitle">{subtitle}</span>}
    </button>
  )
}
