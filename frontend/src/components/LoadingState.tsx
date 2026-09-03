/** A small centered spinner, replacing plain "Loading…" text wherever a page/table is waiting on data. */
export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="empty-state loading-state">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}
