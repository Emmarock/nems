import type { LevyBalance } from '../api/types'

/** Per-levy due/paid/outstanding table plus a total-outstanding line, for a resident's account. */
export function BalanceBreakdown({ items }: { items: LevyBalance[] }) {
  if (items.length === 0) {
    return <span className="muted">No levies invoiced yet</span>
  }
  const totalOutstanding = items.reduce((sum, i) => sum + i.outstanding, 0)
  return (
    <div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Levy</th>
              <th>Due</th>
              <th>Paid</th>
              <th>Outstanding</th>
            </tr>
          </thead>
          <tbody>
            {items.map((i) => (
              <tr key={i.levyId}>
                <td>{i.levyName}</td>
                <td>₦{i.totalDue.toLocaleString()}</td>
                <td>₦{i.totalPaid.toLocaleString()}</td>
                <td style={i.outstanding > 0 ? { color: 'var(--color-danger)', fontWeight: 600 } : undefined}>
                  ₦{i.outstanding.toLocaleString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div style={{ marginTop: 6, fontSize: 13 }}>
        <strong>Total outstanding: </strong>
        <span style={totalOutstanding > 0 ? { color: 'var(--color-danger)', fontWeight: 700 } : undefined}>
          ₦{totalOutstanding.toLocaleString()}
        </span>
      </div>
    </div>
  )
}
