import { createContext, useContext, useState, type ReactNode } from 'react'

export type EntityRef =
  | { type: 'resident'; id: number }
  | { type: 'property'; id: number }
  | { type: 'gate'; id: number }
  | { type: 'visitor'; id: number }
  | { type: 'worker'; id: number }
  | { type: 'vehicle'; id: number }

interface EntityDetailContextValue {
  stack: EntityRef[]
  openResident: (id: number) => void
  openProperty: (id: number) => void
  openGate: (id: number) => void
  openVisitor: (id: number) => void
  openWorker: (id: number) => void
  openVehicle: (id: number) => void
  back: () => void
  close: () => void
}

const EntityDetailContext = createContext<EntityDetailContextValue | undefined>(undefined)

export function EntityDetailProvider({ children }: { children: ReactNode }) {
  const [stack, setStack] = useState<EntityRef[]>([])

  function push(ref: EntityRef) {
    setStack((prev) => [...prev, ref])
  }

  function back() {
    setStack((prev) => prev.slice(0, -1))
  }

  function close() {
    setStack([])
  }

  return (
    <EntityDetailContext.Provider
      value={{
        stack,
        openResident: (id) => push({ type: 'resident', id }),
        openProperty: (id) => push({ type: 'property', id }),
        openGate: (id) => push({ type: 'gate', id }),
        openVisitor: (id) => push({ type: 'visitor', id }),
        openWorker: (id) => push({ type: 'worker', id }),
        openVehicle: (id) => push({ type: 'vehicle', id }),
        back,
        close,
      }}
    >
      {children}
    </EntityDetailContext.Provider>
  )
}

export function useEntityDetail(): EntityDetailContextValue {
  const ctx = useContext(EntityDetailContext)
  if (!ctx) throw new Error('useEntityDetail must be used within EntityDetailProvider')
  return ctx
}
