import type { SellerStatus } from '../api/types'

const LABELS: Record<SellerStatus, string> = {
  APPROVED: 'Approved',
  PENDING: 'Pending review',
  REJECTED: 'Rejected',
}

export function StatusBadge({ status }: { status: SellerStatus }) {
  return <span className={`badge badge--${status.toLowerCase()}`}>{LABELS[status]}</span>
}
