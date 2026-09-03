interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

/** Prev/Next pager for a 0-based backend page — renders nothing for a single page of results. */
export function Pagination({ page, totalPages, totalElements, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null

  return (
    <div className="pagination">
      <button className="btn btn-sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
        ← Prev
      </button>
      <span className="muted">
        Page {page + 1} of {totalPages} · {totalElements.toLocaleString()} total
      </span>
      <button className="btn btn-sm" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>
        Next →
      </button>
    </div>
  )
}
